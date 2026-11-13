/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.combobox.ComboBox;

import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;

/**
 * Best-athlete scoring system selector for the results pages.
 *
 * Lists the systems required by the active championships first, then the remaining systems of
 * every scoring family (GAMX, QPoints, Robi, Sinclair). Each ranking appears only once.
 */
@SuppressWarnings("serial")
public class ScoringSystemSelect extends ComboBox<ScoringSystemSelect.Choice> {

	public record Choice(Ranking ranking, int section) {
	}

	private final List<Choice> choices = new ArrayList<>();

	public ScoringSystemSelect(String label) {
		setLabel(label);
		setItemLabelGenerator(c -> c == null ? "" : Ranking.getScoringExplanation(c.ranking()));
		setOverlayWidth("30ch");
		setWidth("30ch");
		setClearButtonVisible(true);
		buildItems();
	}

	public Ranking getRanking() {
		Choice choice = getValue();
		return choice != null ? choice.ranking() : null;
	}

	public void setRanking(Ranking ranking) {
		Choice match = null;
		if (ranking != null) {
			match = this.choices.stream().filter(c -> c.ranking() == ranking).findFirst().orElse(null);
		}
		setValue(match);
	}

	private void buildItems() {
		RankingConfig.updateMustCompute();
		Set<Ranking> required = RankingConfig.getMustCompute();

		List<Collection<Ranking>> families = List.of(
		        RankingConfig.getGamxRankings(),
		        RankingConfig.getQPointsRankings(),
		        RankingConfig.getRobiRankings(),
		        RankingConfig.getSinclairRankings());

		List<List<Ranking>> sections = new ArrayList<>();
		List<Ranking> requiredInFamilyOrder = new ArrayList<>();
		for (Collection<Ranking> family : families) {
			family.stream()
			        .filter(required::contains)
			        .forEach(requiredInFamilyOrder::add);
		}
		sections.add(requiredInFamilyOrder);
		// ComboBox cannot show separators, so required systems are not repeated in their family
		families.forEach(family -> sections.add(family.stream()
		        .filter(ranking -> !requiredInFamilyOrder.contains(ranking))
		        .toList()));

		this.choices.clear();
		for (int i = 0; i < sections.size(); i++) {
			List<Ranking> section = sections.get(i);
			if (section.isEmpty()) {
				continue;
			}
			for (Ranking r : section) {
				this.choices.add(new Choice(r, i));
			}
		}
		setItems(this.choices);
	}
}
