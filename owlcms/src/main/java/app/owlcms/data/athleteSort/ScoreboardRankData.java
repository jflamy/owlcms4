package app.owlcms.data.athleteSort;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Participation;
import app.owlcms.i18n.Translator;

/** Shared medal and best-athlete values for displays and exported displayInfo. */
public final class ScoreboardRankData {
	public record Columns(boolean medalScore, boolean liftRanks, boolean legacyTotalRank,
	        boolean bestScore, boolean bestRank, Set<Ranking> medalSystems, Set<Ranking> bestSystems) {}

	private ScoreboardRankData() {}

	public static Columns columns(Collection<Championship> championships, boolean bestScore,
	        boolean bestRank, boolean hideBestRank) {
		Set<Ranking> medals = championships.stream().filter(Objects::nonNull)
		        .map(Championship::getScoringSystem).filter(s -> s != Ranking.TOTAL).collect(Collectors.toSet());
		Set<Ranking> best = championships.stream().filter(Objects::nonNull)
		        .map(Championship::getBestAthleteScoringSystem).collect(Collectors.toSet());
		boolean total = championships.stream().filter(Objects::nonNull)
		        .anyMatch(c -> c.getScoringSystem() == Ranking.TOTAL);
		// Snatch/C&J rank columns follow the medal policy of the displayed championships: shown only when at
		// least one of them awards lift medals. A score medal system forces a Total-only policy, so
		// score-medal championships never show them.
		boolean liftRanks = championships.stream().filter(Objects::nonNull)
		        .anyMatch(Championship::isSnatchCJTotalMedals);
		return new Columns(!medals.isEmpty(), liftRanks, medals.isEmpty() || total,
		        bestScore, bestRank && !hideBestRank, medals, best);
	}

	public static String title(Set<Ranking> systems, Locale locale) {
		return systems.size() == 1 ? Translator.translate("Ranking." + systems.iterator().next().name(), locale)
		        : Translator.translate("Score", locale);
	}

	/** Body weight for the optional scoreboard column (feature switch displayBodyWeight). */
	public static String bodyWeight(Athlete athlete, Locale locale) {
		Double bodyWeight = athlete.getBodyWeight();
		return bodyWeight != null ? String.format(locale, "%.2f", bodyWeight) : "";
	}

	public static Map<String, String> fields(Athlete athlete, Locale locale, boolean highlightMedals) {
		athlete.getCategory();
		Participation participation = Objects.requireNonNull(athlete.getMainRankings(),
		        "Scoreboard ranks require a participation");
		Map<String, String> fields = new LinkedHashMap<>();
		int medalRank = participation.getCategoryScoreRank();
		boolean weightMedals = athlete.getComputedScoringSystem() == Ranking.TOTAL;
		fields.put("snatchRank", rank(participation.getSnatchRank(), locale));
		fields.put("cleanJerkRank", rank(participation.getCleanJerkRank(), locale));
		fields.put("totalRank", rank(participation.getTotalRank(), locale));
		fields.put("medalRank", rank(medalRank, locale));
		fields.put("medalScore", weightMedals ? ""
		        : score(participation.getCategoryScore(), locale));
		fields.put("medalHighlight", highlightMedals && athlete.getMedalPolicy().includesTotal()
		        && AthleteSorter.isMedalist(athlete, Ranking.CATEGORY_SCORE) ? "medal" + medalRank : "");
		Ranking bestSystem = participation.getCategory().getAgeGroup().getChampionship().getBestAthleteScoringSystem();
		fields.put("sinclair", score(Ranking.getRankingValue(athlete, bestSystem), locale));
		fields.put("sinclairRank", rank(athlete.getBestLifterRank(), locale));
		return fields;
	}

	private static String rank(int rank, Locale locale) {
		return rank == 0 ? "-" : rank < 0 ? Translator.translate("Results.Extra/Invited", locale)
		        : Integer.toString(rank);
	}

	private static String score(double score, Locale locale) {
		return score > 0.001 ? String.format(locale, "%.3f", score) : "-";
	}
}
