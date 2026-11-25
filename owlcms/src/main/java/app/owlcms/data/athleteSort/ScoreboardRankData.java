package app.owlcms.data.athleteSort;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.scoring.ScoreTarget;
import app.owlcms.i18n.Translator;

/** Shared medal and best-athlete values for displays and exported displayInfo. */
public final class ScoreboardRankData {
	public enum DeltaMode {
		NONE,
		PREVIOUS_RANK,
		LEADER
	}

	public record Columns(boolean medalScore, boolean liftRanks, boolean legacyTotalRank,
	        boolean bestScore, boolean bestRank, Set<Ranking> medalSystems, Set<Ranking> bestSystems) {}

	private ScoreboardRankData() {}

	public static DeltaMode deltaMode(boolean showKgDelta, boolean showKgLeader) {
		if (showKgLeader) {
			return DeltaMode.LEADER;
		}
		return showKgDelta ? DeltaMode.PREVIOUS_RANK : DeltaMode.NONE;
	}

	public static Double leaderScore(List<Athlete> categoryAthletes) {
		if (categoryAthletes == null || categoryAthletes.isEmpty()) {
			return null;
		}
		for (Athlete candidate : categoryAthletes) {
			if (candidate == null) {
				continue;
			}
			Participation participation = candidate.getMainRankings();
			if (participation != null && participation.getCategoryScoreRank() == 1) {
				Double score = participation.getCategoryScore();
				if (score != null && score > 0.001) {
					return score;
				}
			}
		}
		return null;
	}

	public static Double leaderScore(Athlete athlete, Map<String, List<Athlete>> medalsByCategory,
	        List<Athlete> fallbackAthletes) {
		Category category = athlete != null ? athlete.getCategory() : null;
		if (category == null) {
			return null;
		}
		if (medalsByCategory != null) {
			List<Athlete> categoryAthletes = medalsByCategory.get(category.getCode());
			if (categoryAthletes == null) {
				categoryAthletes = medalsByCategory.get(category.getComputedCode());
			}
			Double score = leaderScore(categoryAthletes);
			if (score != null) {
				return score;
			}
		}
		if (fallbackAthletes != null) {
			for (Athlete candidate : fallbackAthletes) {
				if (candidate == null || candidate.getCategory() == null
				        || !candidate.getCategory().sameAs(category)) {
					continue;
				}
				Participation participation = candidate.getMainRankings();
				if (participation != null && participation.getCategoryScoreRank() == 1) {
					Double score = participation.getCategoryScore();
					if (score != null && score > 0.001) {
						return score;
					}
				}
			}
		}
		return null;
	}

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
		return title(systems, locale, false);
	}

	public static String title(Set<Ranking> systems, Locale locale, boolean showKgDelta) {
		String base = systems.size() == 1 ? Translator.translate("Ranking." + systems.iterator().next().name(), locale)
		        : Translator.translate("Score", locale);
		return showKgDelta && !systems.isEmpty()
		        ? Translator.translate("Scoreboard.ScoreWithDeltaFormat", locale, base)
		        : base;
	}

	/** Body weight for the optional scoreboard column (feature switch displayBodyWeight). */
	public static String bodyWeight(Athlete athlete, Locale locale) {
		Double bodyWeight = athlete.getBodyWeight();
		return bodyWeight != null ? String.format(locale, "%.2f", bodyWeight) : "";
	}

	public static Map<String, String> fields(Athlete athlete, Locale locale, boolean highlightMedals) {
		return fields(athlete, locale, highlightMedals, DeltaMode.NONE, null);
	}

	public static Map<String, String> fields(Athlete athlete, Locale locale, boolean highlightMedals,
	        boolean showKgDelta) {
		return fields(athlete, locale, highlightMedals,
		        showKgDelta ? DeltaMode.PREVIOUS_RANK : DeltaMode.NONE, null);
	}

	public static Map<String, String> fields(Athlete athlete, Locale locale, boolean highlightMedals,
	        DeltaMode deltaMode, Double leaderScore) {
		athlete.getCategory();
		Participation participation = Objects.requireNonNull(athlete.getMainRankings(),
		        "Scoreboard ranks require a participation");
		Map<String, String> fields = new LinkedHashMap<>();
		int medalRank = participation.getCategoryScoreRank();
		Ranking medalSystem = participation.getCategory() != null
		        && participation.getCategory().getAgeGroup() != null
		        ? participation.getCategory().getAgeGroup().getComputedScoringSystem()
		        : athlete.getComputedScoringSystem();
		boolean weightMedals = medalSystem == Ranking.TOTAL;
		fields.put("snatchRank", rank(participation.getSnatchRank(), locale));
		fields.put("cleanJerkRank", rank(participation.getCleanJerkRank(), locale));
		fields.put("totalRank", rank(participation.getTotalRank(), locale));
		fields.put("medalRank", rank(medalRank, locale));
		fields.put("age", athlete.getAge() != null ? athlete.getAge().toString() : "");
		Double rawMedalScore = participation.getCategoryScore();
		double medalScoreValue = rawMedalScore != null ? rawMedalScore : 0.0D;
		String medalScore = weightMedals ? "" : score(medalScoreValue, locale);
		if (deltaMode != null && deltaMode != DeltaMode.NONE && !weightMedals && medalRank > 1
		        && medalScoreValue > 0.001) {
			int kgDelta = 0;
			if (deltaMode == DeltaMode.LEADER) {
				if (leaderScore != null && leaderScore > 0.001) {
					kgDelta = ScoreTarget.kgDifference(athlete, leaderScore, medalSystem);
				}
			} else {
				kgDelta = participation.getCategoryScoreKgDelta();
			}
			if (kgDelta > 0) {
				medalScore = medalScore + " (+" + kgDelta + ")";
			}
		}
		fields.put("medalScore", medalScore);
		fields.put("medalHighlight", highlightMedals && athlete.getMedalPolicy().includesTotal()
		        && AthleteSorter.isMedalist(athlete, Ranking.CATEGORY_SCORE) ? "medal" + medalRank : "");
		Ranking bestSystem = participation.getCategory().getAgeGroup().getChampionship()
		        .getEffectiveBestAthleteScoringSystem();
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
