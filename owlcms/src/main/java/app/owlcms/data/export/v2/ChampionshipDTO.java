/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.export.StoredChampionshipMixin;

/**
 * DTO for Championship in V2 export format.
 * Uses championship name as the symbolic key referenced by AgeGroupDTO.championshipName.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChampionshipDTO {

	/**
	 * Mapper that reads {@link Championship} via stored fields (using the shared
	 * mixin) so the V2 export is "dumb" and reflects the database, not values
	 * resolved through the competition-template defaults.
	 */
	private static final ObjectMapper STORED_FIELD_MAPPER = new ObjectMapper()
	        .addMixIn(Championship.class, StoredChampionshipMixin.class);

	private String name;
	private ChampionshipType type;
	private Ranking scoringSystem;
	private Ranking bestAthleteScoringSystem;
	private Ranking bestSnatchScoringSystem;
	private Ranking bestCJScoringSystem;
	private boolean snatchCJTotalMedals;
	private Integer teamPoints1st;
	private Integer teamPoints2nd;
	private Integer teamPoints3rd;
	private Integer mensBestN;
	private Integer womensBestN;
	private Integer mixedMensBestN;
	private Integer mixedWomensBestN;
	private Integer mixedBestN;
	private Integer explicitTeamSize;
	private Integer maxTeamSize;
	private Integer maxPerCategory;
	private boolean explicitMixedTeamMembers;
	private boolean mixedTeamEnabled = false;
	private boolean competitionTemplate = false;
	private boolean useCompetitionDefaults = false;
	private Ranking teamScoringSystem;
	private Ranking mixedTeamScoringSystem;

	public ChampionshipDTO() {
	}

	public static ChampionshipDTO fromChampionship(Championship championship) {
		if (championship == null) {
			return null;
		}
		// Copies stored fields (not smart getters) via the shared mixin.
		return STORED_FIELD_MAPPER.convertValue(championship, ChampionshipDTO.class);
	}

	public Championship toChampionship() {
		Championship championship = new Championship(this.competitionTemplate ? Championship.COMPETITION_TEMPLATE_NAME : this.name, this.type);
		championship.setCompetitionTemplate(this.competitionTemplate);
		championship.setScoringSystem(this.scoringSystem);
		championship.setBestAthleteScoringSystem(this.bestAthleteScoringSystem);
		championship.setBestSnatchScoringSystem(this.bestSnatchScoringSystem);
		championship.setBestCJScoringSystem(this.bestCJScoringSystem);
		championship.setSnatchCJTotalMedals(this.snatchCJTotalMedals);
		championship.setTeamPoints1st(this.teamPoints1st);
		championship.setTeamPoints2nd(this.teamPoints2nd);
		championship.setTeamPoints3rd(this.teamPoints3rd);
		championship.setMensBestN(this.mensBestN);
		championship.setWomensBestN(this.womensBestN);
		championship.setMixedMensBestN(this.mixedMensBestN);
		championship.setMixedWomensBestN(this.mixedWomensBestN);
		championship.setMixedBestN(this.mixedBestN);
		championship.setExplicitTeamSize(this.explicitTeamSize);
		championship.setMaxTeamSize(this.maxTeamSize);
		championship.setMaxPerCategory(this.maxPerCategory);
		championship.setExplicitMixedTeamMembers(this.explicitMixedTeamMembers);
		championship.setMixedTeamEnabled(this.mixedTeamEnabled);
		championship.setTeamScoringSystem(this.teamScoringSystem);
		championship.setMixedTeamScoringSystem(this.mixedTeamScoringSystem);
		championship.setUseCompetitionDefaults(this.useCompetitionDefaults);
		return championship;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ChampionshipType getType() {
		return type;
	}

	public void setType(ChampionshipType type) {
		this.type = type;
	}

	public Ranking getScoringSystem() {
		return scoringSystem;
	}

	public void setScoringSystem(Ranking scoringSystem) {
		this.scoringSystem = scoringSystem;
	}

	public Ranking getBestAthleteScoringSystem() {
		return bestAthleteScoringSystem;
	}

	public void setBestAthleteScoringSystem(Ranking bestAthleteScoringSystem) {
		this.bestAthleteScoringSystem = bestAthleteScoringSystem;
	}

	public Ranking getBestSnatchScoringSystem() {
		return bestSnatchScoringSystem;
	}

	public void setBestSnatchScoringSystem(Ranking bestSnatchScoringSystem) {
		this.bestSnatchScoringSystem = bestSnatchScoringSystem;
	}

	public Ranking getBestCJScoringSystem() {
		return bestCJScoringSystem;
	}

	public void setBestCJScoringSystem(Ranking bestCJScoringSystem) {
		this.bestCJScoringSystem = bestCJScoringSystem;
	}

	public boolean isSnatchCJTotalMedals() {
		return snatchCJTotalMedals;
	}

	public void setSnatchCJTotalMedals(boolean snatchCJTotalMedals) {
		this.snatchCJTotalMedals = snatchCJTotalMedals;
	}

	public Integer getTeamPoints1st() {
		return teamPoints1st;
	}

	public void setTeamPoints1st(Integer teamPoints1st) {
		this.teamPoints1st = teamPoints1st;
	}

	public Integer getTeamPoints2nd() {
		return teamPoints2nd;
	}

	public void setTeamPoints2nd(Integer teamPoints2nd) {
		this.teamPoints2nd = teamPoints2nd;
	}

	public Integer getTeamPoints3rd() {
		return teamPoints3rd;
	}

	public void setTeamPoints3rd(Integer teamPoints3rd) {
		this.teamPoints3rd = teamPoints3rd;
	}

	public Integer getMensBestN() {
		return mensBestN;
	}

	public void setMensBestN(Integer mensBestN) {
		this.mensBestN = mensBestN;
	}

	public Integer getWomensBestN() {
		return womensBestN;
	}

	public void setWomensBestN(Integer womensBestN) {
		this.womensBestN = womensBestN;
	}

	public Integer getMixedMensBestN() {
		return mixedMensBestN;
	}

	public void setMixedMensBestN(Integer mixedMensBestN) {
		this.mixedMensBestN = mixedMensBestN;
	}

	public Integer getMixedWomensBestN() {
		return mixedWomensBestN;
	}

	public void setMixedWomensBestN(Integer mixedWomensBestN) {
		this.mixedWomensBestN = mixedWomensBestN;
	}

	public Integer getMixedBestN() {
		return mixedBestN;
	}

	public void setMixedBestN(Integer mixedBestN) {
		this.mixedBestN = mixedBestN;
	}

	public Integer getExplicitTeamSize() {
		return explicitTeamSize;
	}

	public void setExplicitTeamSize(Integer explicitTeamSize) {
		this.explicitTeamSize = explicitTeamSize;
	}

	public Integer getMaxTeamSize() {
		return maxTeamSize;
	}

	public void setMaxTeamSize(Integer maxTeamSize) {
		this.maxTeamSize = maxTeamSize;
	}

	public Integer getMaxPerCategory() {
		return maxPerCategory;
	}

	public void setMaxPerCategory(Integer maxPerCategory) {
		this.maxPerCategory = maxPerCategory;
	}

	public boolean isExplicitMixedTeamMembers() {
		return explicitMixedTeamMembers;
	}

	public void setExplicitMixedTeamMembers(boolean explicitMixedTeamMembers) {
		this.explicitMixedTeamMembers = explicitMixedTeamMembers;
	}

	public boolean isMixedTeamEnabled() {
		return mixedTeamEnabled;
	}

	public void setMixedTeamEnabled(boolean mixedTeamEnabled) {
		this.mixedTeamEnabled = mixedTeamEnabled;
	}

	public boolean isCompetitionTemplate() {
		return competitionTemplate;
	}

	public void setCompetitionTemplate(boolean competitionTemplate) {
		this.competitionTemplate = competitionTemplate;
	}

	public Ranking getTeamScoringSystem() {
		return teamScoringSystem;
	}

	public void setTeamScoringSystem(Ranking teamScoringSystem) {
		this.teamScoringSystem = teamScoringSystem;
	}

	public Ranking getMixedTeamScoringSystem() {
		return mixedTeamScoringSystem;
	}

	public void setMixedTeamScoringSystem(Ranking mixedTeamScoringSystem) {
		this.mixedTeamScoringSystem = mixedTeamScoringSystem;
	}

	public boolean isUseCompetitionDefaults() {
		return useCompetitionDefaults;
	}

	public void setUseCompetitionDefaults(boolean useCompetitionDefaults) {
		this.useCompetitionDefaults = useCompetitionDefaults;
	}
}