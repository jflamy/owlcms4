/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.select.Select;

import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.i18n.Translator;

/**
 * Scoring system selector with a separator between scoring families.
 *
 * On the results pages, required scoring systems are listed first and "Per Athlete" is listed last when available.
 * Championship editing uses a fixed list without either.
 */
@SuppressWarnings("serial")
public class ScoringSystemSelect extends Select<ScoringSystemSelect.Choice> {

	/** A null ranking is the "Per Athlete" choice: each athlete uses its registration championship. */
	public record Choice(Ranking ranking, int section) {
	}

	private static final Choice PER_ATHLETE = new Choice(null, Integer.MAX_VALUE);

	private final List<Choice> choices = new ArrayList<>();
	private final List<Ranking> rankings;
	private boolean perAthleteAvailable;

	/** Results-page selector: required systems first, "Per Athlete" last. */
	public ScoringSystemSelect(String label) {
		this(label, null, true);
		setOverlayWidth("30ch");
		setWidth("30ch");
	}

	/**
	 * Selector over a fixed list of scoring systems, without "Per Athlete".
	 *
	 * @param rankings  the choices, in display order
	 * @param emptyCaption caption of an empty choice listed first, or null when no empty choice is offered
	 */
	public ScoringSystemSelect(List<Ranking> rankings, String emptyCaption) {
		this(null, rankings, false);
		if (emptyCaption != null) {
			setEmptySelectionAllowed(true);
			setEmptySelectionCaption(emptyCaption);
		}
	}

	private ScoringSystemSelect(String label, List<Ranking> rankings, boolean perAthleteAvailable) {
		this.rankings = rankings;
		this.perAthleteAvailable = perAthleteAvailable;
		setLabel(label);
		String perAthlete = Translator.translate("Ranking.PerAthlete");
		setItemLabelGenerator(c -> c == null ? "" : c.ranking() == null ? perAthlete : Ranking.getScoringExplanation(c.ranking()));
		buildItems();
	}

	public Ranking getRanking() {
		Choice choice = getValue();
		return choice != null ? choice.ranking() : null;
	}

	public void setPerAthleteAvailable(boolean available) {
		if (available == this.perAthleteAvailable) {
			return;
		}
		Ranking current = getRanking();
		this.perAthleteAvailable = available;
		buildItems();
		setRanking(current);
	}

	public void setRanking(Ranking ranking) {
		setValue(this.choices.stream().filter(c -> c.ranking() == ranking).findFirst().orElse(null));
	}

	private void buildItems() {
		this.choices.clear();
		if (this.rankings != null) {
			this.rankings.forEach(ranking -> this.choices.add(new Choice(ranking, familySection(ranking))));
		} else {
			RankingConfig.updateMustCompute();
			Set<Ranking> required = RankingConfig.getMustCompute();
			RankingConfig.comboOrder(required).forEach(ranking ->
			        this.choices.add(new Choice(ranking, required.contains(ranking) ? 0 : familySection(ranking))));
		}
		if (this.perAthleteAvailable) {
			this.choices.add(PER_ATHLETE);
		}
		removeAll();
		setItems(this.choices);
		for (int i = 1; i < this.choices.size(); i++) {
			Choice previous = this.choices.get(i - 1);
			if (previous.section() != this.choices.get(i).section()) {
				addComponents(previous, new Hr());
			}
		}
	}

	private static int familySection(Ranking ranking) {
		return switch (ranking) {
		case TOTAL -> 0;
		case GAMX, GAMX_M, GAMX_U, GAMX_A -> 1;
		case QPOINTS, QAGE, AGEFACTORS -> 2;
		case BW_SINCLAIR, SMM, ROBI -> 3;
		case CAT_GAMX, CAT_QPOINTS, CAT_SINCLAIR -> 4;
		case GAMX_S, GAMX_C, GAMX_MS, GAMX_MC -> 5;
		default -> throw new IllegalArgumentException("Unexpected scoring system: " + ranking);
		};
	}
}
