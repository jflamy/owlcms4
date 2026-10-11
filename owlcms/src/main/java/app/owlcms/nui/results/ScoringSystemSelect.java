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

import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.select.Select;

import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;

/**
 * Best-athlete scoring system selector for the results pages.
 *
 * Lists the systems required by the active championships first, then every scoring family
 * (GAMX, QPoints, Robi, Sinclair), each group separated by a rule. A ranking may appear in more
 * than one group, so items are wrapped with their section index to keep them distinct.
 */
@SuppressWarnings("serial")
public class ScoringSystemSelect extends Select<ScoringSystemSelect.Choice> {

	public record Choice(Ranking ranking, int section) {
	}

	private final List<Choice> choices = new ArrayList<>();

	public ScoringSystemSelect(String label) {
		setLabel(label);
		setItemLabelGenerator(c -> Ranking.getScoringExplanation(c.ranking()));
		setOverlayWidth("30ch");
		setWidth("30ch");
		setEmptySelectionAllowed(false);
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
			family.stream().filter(required::contains).forEach(requiredInFamilyOrder::add);
		}
		sections.add(requiredInFamilyOrder);
		families.forEach(f -> sections.add(new ArrayList<>(f)));

		this.choices.clear();
		List<Choice> lastOfSection = new ArrayList<>();
		for (int i = 0; i < sections.size(); i++) {
			List<Ranking> section = sections.get(i);
			if (section.isEmpty()) {
				continue;
			}
			for (Ranking r : section) {
				this.choices.add(new Choice(r, i));
			}
			lastOfSection.add(this.choices.get(this.choices.size() - 1));
		}
		setItems(this.choices);

		// a separator after each group except the last one
		for (int i = 0; i < lastOfSection.size() - 1; i++) {
			addComponents(lastOfSection.get(i), new Hr());
		}
	}
}
