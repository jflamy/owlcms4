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
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public Ranking getBestAthleteScoringSystem() {
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public Ranking getBestSnatchScoringSystem() {
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public Ranking getBestCJScoringSystem() {
		return Competition.getCurrent().getScoringSystem();
	}

	@Override
	public boolean isSnatchCJTotalMedals() {
		return Competition.getCurrent().isSnatchCJTotalMedals();
	}

	@Override
	public Integer getTeamPoints1st() {
		return Competition.getCurrent().getTeamPoints1st();
	}

	@Override
	public Integer getTeamPoints2nd() {
		return Competition.getCurrent().getTeamPoints2nd();
	}

	@Override
	public Integer getTeamPoints3rd() {
		return Competition.getCurrent().getTeamPoints3rd();
	}

	@Override
	public Integer getMensBestN() {
		return Competition.getCurrent().getMensBestN();
	}

	@Override
	public Integer getWomensBestN() {
		return Competition.getCurrent().getWomensBestN();
	}

	@Override
	public Integer getMixedBestN() {
		return Competition.getCurrent().getMixedBestN();
	}

	@Override
	public Integer getMaxTeamSize() {
		Integer competitionMaxTeamSize = Competition.getCurrent().getMaxTeamSize();
		return competitionMaxTeamSize != null ? competitionMaxTeamSize : 8;
	}

	@Override
	public Integer getMaxPerCategory() {
		return Competition.getCurrent().getMaxPerCategory();
	}

	@Override
	public boolean isScoreMedalChampionship() {
		return Competition.getCurrent().isScoreMedalChampionship();
	}
}