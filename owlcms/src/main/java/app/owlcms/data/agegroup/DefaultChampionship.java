/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import app.owlcms.data.athleteSort.Ranking;

public class DefaultChampionship extends Championship {

	private static final long serialVersionUID = 1L;
	private static final DefaultChampionship INSTANCE = new DefaultChampionship();
	private static final Ranking DEFAULT_MEDAL_SCORING = Ranking.TOTAL;
	private static final Ranking DEFAULT_BEST_ATHLETE_SCORING = Ranking.BW_SINCLAIR;
	private static final int DEFAULT_TEAM_POINTS_1ST = 28;
	private static final int DEFAULT_TEAM_POINTS_2ND = 25;
	private static final int DEFAULT_TEAM_POINTS_3RD = 23;
	private static final int DEFAULT_MAX_TEAM_SIZE = 8;
	private static final int DEFAULT_MAX_PER_CATEGORY = 2;

	public static DefaultChampionship getInstance() {
		return INSTANCE;
	}

	private DefaultChampionship() {
		super("Competition Defaults", ChampionshipType.DEFAULT);
	}

	@Override
	public boolean computeUsesCompetitionDefaults() {
		return true;
	}

	@Override
	public void setUseCompetitionDefaults(boolean useCompetitionDefaults) {
		// Sentinel always delegates to competition defaults.
	}

	@Override
	public Ranking getScoringSystem() {
		Championship template = template();
		if (template != null) {
			return template.getScoringSystem();
		}
		return DEFAULT_MEDAL_SCORING;
	}

	@Override
	public Ranking getBestAthleteScoringSystem() {
		Championship template = template();
		if (template != null) {
			return template.getBestAthleteScoringSystem();
		}
		return DEFAULT_BEST_ATHLETE_SCORING;
	}

	@Override
	public Ranking getBestSnatchScoringSystem() {
		Championship template = template();
		if (template != null) {
			return template.getBestSnatchScoringSystem();
		}
		return null;
	}

	@Override
	public Ranking getBestCJScoringSystem() {
		Championship template = template();
		if (template != null) {
			return template.getBestCJScoringSystem();
		}
		return null;
	}

	@Override
	public boolean isSnatchCJTotalMedals() {
		Championship template = template();
		if (template != null) {
			return template.isSnatchCJTotalMedals();
		}
		return false;
	}

	@Override
	public Integer getTeamPoints1st() {
		Championship template = template();
		if (template != null) {
			return template.getTeamPoints1st();
		}
		return DEFAULT_TEAM_POINTS_1ST;
	}

	@Override
	public Integer getTeamPoints2nd() {
		Championship template = template();
		if (template != null) {
			return template.getTeamPoints2nd();
		}
		return DEFAULT_TEAM_POINTS_2ND;
	}

	@Override
	public Integer getTeamPoints3rd() {
		Championship template = template();
		if (template != null) {
			return template.getTeamPoints3rd();
		}
		return DEFAULT_TEAM_POINTS_3RD;
	}

	@Override
	public Integer getMensBestN() {
		Championship template = template();
		if (template != null) {
			return template.getMensBestN();
		}
		return null;
	}

	@Override
	public Integer getWomensBestN() {
		Championship template = template();
		if (template != null) {
			return template.getWomensBestN();
		}
		return null;
	}

	@Override
	public Integer getMixedBestN() {
		Championship template = template();
		if (template != null) {
			return template.getMixedBestN();
		}
		return null;
	}

	@Override
	public Integer getMixedMensBestN() {
		Championship template = template();
		return template != null ? template.getMixedMensBestN() : null;
	}

	@Override
	public Integer getMixedWomensBestN() {
		Championship template = template();
		return template != null ? template.getMixedWomensBestN() : null;
	}

	@Override
	public Integer getMaxTeamSize() {
		Championship template = template();
		if (template != null) {
			return template.getMaxTeamSize();
		}
		return DEFAULT_MAX_TEAM_SIZE;
	}

	@Override
	public Integer getMaxPerCategory() {
		Championship template = template();
		if (template != null) {
			return template.getMaxPerCategory();
		}
		return DEFAULT_MAX_PER_CATEGORY;
	}

	@Override
	public Integer getExplicitTeamSize() {
		Championship template = template();
		return template != null ? template.getExplicitTeamSize() : super.getExplicitTeamSize();
	}

	@Override
	public boolean isExplicitMixedTeamMembers() {
		Championship template = template();
		return template != null ? template.isExplicitMixedTeamMembers() : super.isExplicitMixedTeamMembers();
	}

	@Override
	public boolean isGenderedTeamsEnabled() {
		Championship template = template();
		return template != null ? template.isGenderedTeamsEnabled() : super.isGenderedTeamsEnabled();
	}

	@Override
	public boolean isMixedTeamEnabled() {
		Championship template = template();
		return template != null ? template.isMixedTeamEnabled() : super.isMixedTeamEnabled();
	}

	@Override
	public Ranking getTeamScoringSystem() {
		Championship template = template();
		return template != null ? template.getTeamScoringSystem() : super.getTeamScoringSystem();
	}

	@Override
	public Ranking getMixedTeamScoringSystem() {
		Championship template = template();
		return template != null ? template.getMixedTeamScoringSystem() : super.getMixedTeamScoringSystem();
	}

	@Override
	public boolean isScoreMedalChampionship() {
		Championship template = template();
		if (template != null) {
			return template.isScoreMedalChampionship();
		}
		return getScoringSystem() != null && getScoringSystem().isMedalScore();
	}

	private Championship template() {
		return ChampionshipRepository.findCompetitionTemplate();
	}
}