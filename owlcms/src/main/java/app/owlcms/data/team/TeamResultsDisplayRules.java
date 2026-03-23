/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;

public final class TeamResultsDisplayRules {

	private TeamResultsDisplayRules() {
	}

	public static List<Ranking> getRequiredScoreRankings(Championship championship, Gender genderFilter,
			Ranking competitionScoring) {
		Set<Ranking> rankings = new LinkedHashSet<>();

		if (championship != null) {
			if (genderFilter == Gender.MF) {
				Ranking mixedScoring = championship.getMixedTeamScoringSystem();
				if (mixedScoring != null) {
					rankings.add(mixedScoring);
				}
			} else if (genderFilter == null) {
				Ranking teamScoring = championship.getTeamScoringSystem();
				if (teamScoring != null) {
					rankings.add(teamScoring);
				}
				Ranking mixedScoring = championship.getMixedTeamScoringSystem();
				if (mixedScoring != null) {
					rankings.add(mixedScoring);
				}
			} else {
				Ranking teamScoring = championship.getTeamScoringSystem();
				if (teamScoring != null) {
					rankings.add(teamScoring);
				}
			}

			if (competitionScoring != null) {
				rankings.add(competitionScoring);
			}

			if (championship.getType() != null && championship.getType().isMasters()) {
				rankings.add(Ranking.QAGE);
				rankings.add(Ranking.SMM);
			}
		} else {
			rankings.add(Ranking.QPOINTS);
			rankings.add(Ranking.BW_SINCLAIR);
		}

		return new ArrayList<>(rankings);
	}

	public static Ranking getChampionshipSelectedRanking(Championship championship, TeamTreeItem item) {
		if (championship == null || item == null || item.getAthlete() != null) {
			return null;
		}

		if (item.getGender() == Gender.MF) {
			return championship.getMixedTeamScoringSystem() != null ? championship.getMixedTeamScoringSystem() : Ranking.TOTAL;
		}

		return championship.getTeamScoringSystem() != null ? championship.getTeamScoringSystem() : Ranking.TOTAL;
	}

	public static boolean shouldShowTeamSummaryValue(Championship championship, TeamTreeItem item, Ranking ranking) {
		if (item == null || item.getAthlete() != null) {
			return true;
		}

		Ranking selectedRanking = getChampionshipSelectedRanking(championship, item);
		return ranking != null && ranking == selectedRanking;
	}

	public static boolean shouldShowTeamSummaryPoints(Championship championship, TeamTreeItem item) {
		if (item == null || item.getAthlete() != null) {
			return true;
		}

		return getChampionshipSelectedRanking(championship, item) == Ranking.TOTAL;
	}
}