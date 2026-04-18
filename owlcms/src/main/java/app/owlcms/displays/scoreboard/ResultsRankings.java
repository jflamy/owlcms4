/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.fieldofplay.FieldOfPlay;
import ch.qos.logback.classic.Logger;
import elemental.json.JsonObject;

/**
 * Rankings display using the standard results template.
 *
 * This keeps the same public-simple layout while changing the athlete order to
 * results order, inserting category spacers, and adding medal highlight classes
 * on the rank cells.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class ResultsRankings extends Results {

	final private Logger logger = (Logger) LoggerFactory.getLogger(ResultsRankings.class);

	public ResultsRankings() {

	}

	@Override
	protected void getAthleteJson(Athlete athlete, JsonObject athleteJson, Category category, int liftOrderRank, FieldOfPlay fop) {
		super.getAthleteJson(athlete, athleteJson, category, liftOrderRank, fop);
		applyMedalClasses(athlete, athleteJson);
	}

	@Override
	protected List<Athlete> getOrder(FieldOfPlay fop) {
		List<Athlete> resultsOrder = fop != null ? fop.getResultsOrder() : null;
		if (resultsOrder == null) {
			return super.getOrder(fop);
		}

		List<Athlete> adjustedOrder = new ArrayList<>(resultsOrder);
		Ranking displayRanking = determineDisplayRanking(fop);
		Map<Athlete, Integer> originalPositions = new IdentityHashMap<>();
		Map<String, Integer> categoryPositions = new HashMap<>();

		for (int index = 0; index < resultsOrder.size(); index++) {
			Athlete athlete = resultsOrder.get(index);
			originalPositions.put(athlete, index);
			categoryPositions.putIfAbsent(getCategoryKey(athlete), index);
		}

		adjustedOrder.sort(Comparator
		        .comparingInt((Athlete athlete) -> categoryPositions.getOrDefault(getCategoryKey(athlete), Integer.MAX_VALUE))
		        .thenComparingInt(athlete -> getRankingSortKey(athlete, displayRanking))
		        .thenComparingInt(athlete -> originalPositions.getOrDefault(athlete, Integer.MAX_VALUE)));
		return adjustedOrder;
	}

	@Override
	protected BiPredicate<Athlete, Athlete> getSeparatorPredicate() {
		return (current, previous) -> {
			if (previous == null) {
				return true;
			}
			Category currentCategory = current != null ? current.getCategory() : null;
			Category previousCategory = previous.getCategory();
			return currentCategory != null && !currentCategory.sameAs(previousCategory);
		};
	}

	private void applyMedalClasses(Athlete athlete, JsonObject athleteJson) {
		athleteJson.put("snatchMedal", "");
		athleteJson.put("cleanJerkMedal", "");
		athleteJson.put("totalMedal", "");
		athleteJson.put("sinclairMedal", "");

		Participation mainRankings = athlete.getMainRankings();
		if (mainRankings == null) {
			this.logger.error("main rankings null for {}", athlete);
			return;
		}

		if (athlete.getComputedScoringSystem() == Ranking.TOTAL) {
			int snatchRank = mainRankings.getSnatchRank();
			athleteJson.put("snatchMedal", snatchRank >= 1 && snatchRank <= 3 ? "medal" + snatchRank : "");

			int cleanJerkRank = mainRankings.getCleanJerkRank();
			athleteJson.put("cleanJerkMedal", cleanJerkRank >= 1 && cleanJerkRank <= 3 ? "medal" + cleanJerkRank : "");

			int totalRank = mainRankings.getTotalRank();
			athleteJson.put("totalMedal", totalRank >= 1 && totalRank <= 3 ? "medal" + totalRank : "");
		} else {
			int scoreRank = mainRankings.getCategoryScoreRank();
			athleteJson.put("sinclairMedal", scoreRank >= 1 && scoreRank <= 3 ? "medal" + scoreRank : "");
		}
	}

	private Ranking determineDisplayRanking(FieldOfPlay fop) {
		if (fop == null) {
			return Ranking.CATEGORY_SCORE;
		}

		boolean anyMultiMedal = Championship.anyMultiMedal(fop.getActiveChampionships());
		Athlete currentAthlete = fop.getCurAthlete();
		boolean groupDone = currentAthlete != null && currentAthlete.getAttemptsDone() >= 6;
		if (groupDone || (fop.isCjStarted() && !anyMultiMedal)) {
			return Ranking.CATEGORY_SCORE;
		}
		return anyMultiMedal && fop.isCjStarted() ? Ranking.CLEANJERK : Ranking.SNATCH;
	}

	private String getCategoryKey(Athlete athlete) {
		Category category = athlete != null ? athlete.getCategory() : null;
		if (athlete == null || category == null) {
			return "";
		}
		return athlete.getAgeGroupDisplayName() + "|" + athlete.getGender() + "|" + category.getComputedCode();
	}

	private int getDisplayRank(Athlete athlete, Ranking displayRanking) {
		Participation mainRankings = athlete != null ? athlete.getMainRankings() : null;
		if (mainRankings == null) {
			return -1;
		}

		switch (displayRanking) {
			case SNATCH:
				return mainRankings.getSnatchRank();
			case CLEANJERK:
				return mainRankings.getCleanJerkRank();
			case CATEGORY_SCORE:
				return athlete.getComputedScoringSystem() == Ranking.TOTAL
				        ? mainRankings.getTotalRank()
				        : mainRankings.getCategoryScoreRank();
			default:
				return mainRankings.getTotalRank();
		}
	}

	private int getRankingSortKey(Athlete athlete, Ranking displayRanking) {
		int displayRank = getDisplayRank(athlete, displayRanking);
		if (displayRank > 0) {
			return displayRank;
		}
		if (displayRank == 0) {
			return Integer.MAX_VALUE - 1;
		}
		return Integer.MAX_VALUE;
	}

}
