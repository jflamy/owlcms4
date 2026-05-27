/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;

public class DefaultChampionship extends Championship {

	private static final long serialVersionUID = 1L;
	private static final DefaultChampionship INSTANCE = new DefaultChampionship();

	public static DefaultChampionship getInstance() {
		return INSTANCE;
	}

	private DefaultChampionship() {
		super("Competition Defaults", ChampionshipType.DEFAULT);
		super.setUseCompetitionDefaults(true);
	}

	@Override
	public boolean usesCompetitionDefaults() {
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
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public Ranking getBestAthleteScoringSystem() {
		Championship template = template();
		if (template != null) {
			return template.getBestAthleteScoringSystem();
		}
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public Ranking getBestSnatchScoringSystem() {
		Championship template = template();
		if (template != null) {
			return template.getBestSnatchScoringSystem();
		}
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public Ranking getBestCJScoringSystem() {
		Championship template = template();
		if (template != null) {
			return template.getBestCJScoringSystem();
		}
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public boolean isSnatchCJTotalMedals() {
		Championship template = template();
		if (template != null) {
			return template.isSnatchCJTotalMedals();
		}
		return Competition.getCurrent().isSnatchCJTotalMedals();
	}

	@Override
	public Integer getTeamPoints1st() {
		Championship template = template();
		if (template != null) {
			return template.getTeamPoints1st();
		}
		return Competition.getCurrent().getTeamPoints1st();
	}

	@Override
	public Integer getTeamPoints2nd() {
		Championship template = template();
		if (template != null) {
			return template.getTeamPoints2nd();
		}
		return Competition.getCurrent().getTeamPoints2nd();
	}

	@Override
	public Integer getTeamPoints3rd() {
		Championship template = template();
		if (template != null) {
			return template.getTeamPoints3rd();
		}
		return Competition.getCurrent().getTeamPoints3rd();
	}

	@Override
	public Integer getMensBestN() {
		Championship template = template();
		if (template != null) {
			return template.getMensBestN();
		}
		return Competition.getCurrent().getMensBestN();
	}

	@Override
	public Integer getWomensBestN() {
		Championship template = template();
		if (template != null) {
			return template.getWomensBestN();
		}
		return Competition.getCurrent().getWomensBestN();
	}

	@Override
	public Integer getMixedBestN() {
		Championship template = template();
		if (template != null) {
			return template.getMixedBestN();
		}
		return Competition.getCurrent().getMixedBestN();
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
		Integer competitionMaxTeamSize = Competition.getCurrent().getMaxTeamSize();
		return competitionMaxTeamSize != null ? competitionMaxTeamSize : 8;
	}

	@Override
	public Integer getMaxPerCategory() {
		Championship template = template();
		if (template != null) {
			return template.getMaxPerCategory();
		}
		return Competition.getCurrent().getMaxPerCategory();
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
		return Competition.getCurrent().isScoreMedalChampionship();
	}

	private Championship template() {
		return ChampionshipRepository.findCompetitionTemplate();
	}
}