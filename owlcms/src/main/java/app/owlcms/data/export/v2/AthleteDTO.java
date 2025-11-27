/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.EntityManager;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;

/**
 * DTO for Athlete in V2 export format.
 * Uses group name instead of ID, category code instead of ID,
 * and numeric values for lifts instead of strings (null for empty).
 */
public class AthleteDTO {
	
	// Stable athlete identifier (precomputed hash)
	private Integer key;  // Hash of lastName|firstName|teamName|lotNumber
	
	// Basic info
	private String lastName;
	private String firstName;
	private LocalDate fullBirthDate;
	private Gender gender;
	private Double bodyWeight;
	private Double presumedBodyWeight;
	
	// Competition info
	private String groupName;  // Instead of group ID
	private String categoryCode;  // Instead of category ID
	private Integer team;  // Team ID (hashcode of team name)
	private String membership;
	private String federationCodes;
	private Integer startNumber;
	private Integer lotNumber;
	private Integer entryTotal;
	private Integer qualifyingTotal;
	
	// Lifts - using Integer (null for empty) instead of String
	private Integer snatch1Declaration;
	private Integer snatch1Change1;
	private Integer snatch1Change2;
	private Integer snatch1ActualLift;
	private LocalDateTime snatch1LiftTime;
	
	private String snatch2AutomaticProgression;
	private Integer snatch2Declaration;
	private Integer snatch2Change1;
	private Integer snatch2Change2;
	private Integer snatch2ActualLift;
	private LocalDateTime snatch2LiftTime;
	
	private String snatch3AutomaticProgression;
	private Integer snatch3Declaration;
	private Integer snatch3Change1;
	private Integer snatch3Change2;
	private Integer snatch3ActualLift;
	private LocalDateTime snatch3LiftTime;
	
	private Integer cleanJerk1Declaration;
	private Integer cleanJerk1Change1;
	private Integer cleanJerk1Change2;
	private Integer cleanJerk1ActualLift;
	private LocalDateTime cleanJerk1LiftTime;
	
	private String cleanJerk2AutomaticProgression;
	private Integer cleanJerk2Declaration;
	private Integer cleanJerk2Change1;
	private Integer cleanJerk2Change2;
	private Integer cleanJerk2ActualLift;
	private LocalDateTime cleanJerk2LiftTime;
	
	private String cleanJerk3AutomaticProgression;
	private Integer cleanJerk3Declaration;
	private Integer cleanJerk3Change1;
	private Integer cleanJerk3Change2;
	private Integer cleanJerk3ActualLift;
	private LocalDateTime cleanJerk3LiftTime;
	
	// Personal bests
	private Integer personalBestSnatch;
	private Integer personalBestCleanJerk;
	private Integer personalBestTotal;
	
	// Rankings
	private Integer sinclairRank;
	private Integer qPointsRank;
	private Integer qAgeRank;
	private Integer smhfRank;
	private Integer teamSinclairRank;
	private Integer catSinclairRank;
	private Integer catQPointsRank;
	private Integer gamxRank;
	private Integer robiRank;
	private Integer ageAdjustedTotalRank;
	private Integer combinedRank;
	private Integer teamCleanJerkRank;
	private Integer teamCombinedRank;
	private Integer teamCustomRank;
	private Integer teamRobiRank;
	private Integer teamSnatchRank;
	private Integer teamTotalRank;
	
	// Other fields
	private String coach;
	private String custom1;
	private String custom2;
	private Double customScore;
	private Boolean eligibleForIndividualRanking;
	private Boolean eligibleForTeamRanking;
	private Boolean forcedAsCurrent;
	private String subCategory;
	
	// Participations
	private List<ParticipationDTO> participations;

	public AthleteDTO() {
	}

	/**
	 * Convert from domain Athlete object to DTO (legacy method without team map)
	 */
	public static AthleteDTO fromAthlete(Athlete athlete) {
		return fromAthlete(athlete, null);
	}
	
	/**
	 * Convert from domain Athlete object to DTO with team reference mapping
	 * @param athlete The athlete to convert
	 * @param teamMap Map of team names to TeamDTO objects (for ID lookup), or null for backward compatibility
	 */
	public static AthleteDTO fromAthlete(Athlete athlete, Map<String, TeamDTO> teamMap) {
		AthleteDTO dto = new AthleteDTO();
		
		// Stable athlete key (precomputed hash)
		dto.setKey(athlete.getKey());
		
		// Basic info
		dto.setLastName(athlete.getLastName());
		dto.setFirstName(athlete.getFirstName());
		dto.setFullBirthDate(athlete.getFullBirthDate());
		dto.setGender(athlete.getGender());
		dto.setBodyWeight(athlete.getBodyWeight());
		dto.setPresumedBodyWeight(athlete.getPresumedBodyWeight());
		
		// Competition info
		dto.setGroupName(athlete.getGroup() != null ? athlete.getGroup().getName() : null);
		dto.setCategoryCode(athlete.getCategory() != null ? athlete.getCategory().getCode() : null);
		
		// Team reference: use team ID from map if available
		String teamName = athlete.getTeam();
		if (teamMap != null && teamName != null && !teamName.trim().isEmpty()) {
			TeamDTO teamDto = teamMap.get(teamName);
			dto.setTeam(teamDto != null ? teamDto.getId() : null);
		} else {
			// Fallback for backward compatibility or null teams
			dto.setTeam(teamName != null && !teamName.trim().isEmpty() ? teamName.hashCode() : null);
		}
		
		dto.setMembership(athlete.getMembership());
		dto.setFederationCodes(athlete.getFederationCodes());
		dto.setStartNumber(athlete.getStartNumber());
		dto.setLotNumber(athlete.getLotNumber());
		dto.setEntryTotal(athlete.getEntryTotal());
		dto.setQualifyingTotal(athlete.getQualifyingTotal());
		
		// Lifts - convert from String to Integer
		dto.setSnatch1Declaration(parseWeight(athlete.getSnatch1Declaration()));
		dto.setSnatch1Change1(parseWeight(athlete.getSnatch1Change1()));
		dto.setSnatch1Change2(parseWeight(athlete.getSnatch1Change2()));
		dto.setSnatch1ActualLift(parseWeight(athlete.getSnatch1ActualLift()));
		dto.setSnatch1LiftTime(athlete.getSnatch1LiftTime());
		
		dto.setSnatch2AutomaticProgression(athlete.getSnatch2AutomaticProgression());
		dto.setSnatch2Declaration(parseWeight(athlete.getSnatch2Declaration()));
		dto.setSnatch2Change1(parseWeight(athlete.getSnatch2Change1()));
		dto.setSnatch2Change2(parseWeight(athlete.getSnatch2Change2()));
		dto.setSnatch2ActualLift(parseWeight(athlete.getSnatch2ActualLift()));
		dto.setSnatch2LiftTime(athlete.getSnatch2LiftTime());
		
		dto.setSnatch3AutomaticProgression(athlete.getSnatch3AutomaticProgression());
		dto.setSnatch3Declaration(parseWeight(athlete.getSnatch3Declaration()));
		dto.setSnatch3Change1(parseWeight(athlete.getSnatch3Change1()));
		dto.setSnatch3Change2(parseWeight(athlete.getSnatch3Change2()));
		dto.setSnatch3ActualLift(parseWeight(athlete.getSnatch3ActualLift()));
		dto.setSnatch3LiftTime(athlete.getSnatch3LiftTime());
		
		dto.setCleanJerk1Declaration(parseWeight(athlete.getCleanJerk1Declaration()));
		dto.setCleanJerk1Change1(parseWeight(athlete.getCleanJerk1Change1()));
		dto.setCleanJerk1Change2(parseWeight(athlete.getCleanJerk1Change2()));
		dto.setCleanJerk1ActualLift(parseWeight(athlete.getCleanJerk1ActualLift()));
		dto.setCleanJerk1LiftTime(athlete.getCleanJerk1LiftTime());
		
		dto.setCleanJerk2AutomaticProgression(athlete.getCleanJerk2AutomaticProgression());
		dto.setCleanJerk2Declaration(parseWeight(athlete.getCleanJerk2Declaration()));
		dto.setCleanJerk2Change1(parseWeight(athlete.getCleanJerk2Change1()));
		dto.setCleanJerk2Change2(parseWeight(athlete.getCleanJerk2Change2()));
		dto.setCleanJerk2ActualLift(parseWeight(athlete.getCleanJerk2ActualLift()));
		dto.setCleanJerk2LiftTime(athlete.getCleanJerk2LiftTime());
		
		dto.setCleanJerk3AutomaticProgression(athlete.getCleanJerk3AutomaticProgression());
		dto.setCleanJerk3Declaration(parseWeight(athlete.getCleanJerk3Declaration()));
		dto.setCleanJerk3Change1(parseWeight(athlete.getCleanJerk3Change1()));
		dto.setCleanJerk3Change2(parseWeight(athlete.getCleanJerk3Change2()));
		dto.setCleanJerk3ActualLift(parseWeight(athlete.getCleanJerk3ActualLift()));
		dto.setCleanJerk3LiftTime(athlete.getCleanJerk3LiftTime());
		
		// Personal bests
		dto.setPersonalBestSnatch(athlete.getPersonalBestSnatch());
		dto.setPersonalBestCleanJerk(athlete.getPersonalBestCleanJerk());
		dto.setPersonalBestTotal(athlete.getPersonalBestTotal());
		
		// Rankings
		dto.setSinclairRank(athlete.getSinclairRank());
		dto.setqPointsRank(athlete.getqPointsRank());
		dto.setqAgeRank(athlete.getQMastersRank());
		dto.setSmhfRank(athlete.getSmhfRank());
		dto.setTeamSinclairRank(athlete.getTeamSinclairRank());
		dto.setCatSinclairRank(athlete.getCatSinclairRank());
		dto.setCatQPointsRank(athlete.getCatQPointsRank());
		dto.setGamxRank(athlete.getGamxRank());
		dto.setRobiRank(athlete.getRobiRank());
		dto.setAgeAdjustedTotalRank(athlete.getQYouthRank());
		dto.setCombinedRank(athlete.getCombinedRank());
		dto.setTeamCleanJerkRank(athlete.getTeamCleanJerkRank());
		dto.setTeamCombinedRank(athlete.getTeamCombinedRank());
		dto.setTeamCustomRank(athlete.getTeamCustomRank());
		dto.setTeamRobiRank(athlete.getTeamRobiRank());
		dto.setTeamSnatchRank(athlete.getTeamSnatchRank());
		dto.setTeamTotalRank(athlete.getTeamTotalRank());
		
		// Other fields
		dto.setCoach(athlete.getCoach());
		dto.setCustom1(athlete.getCustom1());
		dto.setCustom2(athlete.getCustom2());
		dto.setCustomScore(athlete.getCustomScore());
		dto.setEligibleForIndividualRanking(athlete.isEligibleForIndividualRanking());
		dto.setEligibleForTeamRanking(athlete.isEligibleForTeamRanking());
		dto.setForcedAsCurrent(athlete.isForcedAsCurrent());
		dto.setSubCategory(athlete.getSubCategory());
		
		// Participations - convert to DTOs
		List<Participation> athleteParticipations = athlete.getParticipations();
		if (athleteParticipations != null) {
			List<ParticipationDTO> participationDTOs = athleteParticipations.stream()
				.map(ParticipationDTO::fromParticipation)
				.collect(java.util.stream.Collectors.toList());
			dto.setParticipations(participationDTOs);
		}
		
		return dto;
	}

	/**
	 * Convert from DTO back to domain Athlete object (legacy method without team map)
	 */
	public Athlete toAthlete(EntityManager em) {
		return toAthlete(em, null);
	}
	
	/**
	 * Convert from DTO back to domain Athlete object with team ID to name resolution
	 * @param em EntityManager for database operations
	 * @param teamIdToNameMap Map of team IDs to team names (for resolving team references), or null for backward compatibility
	 */
	public Athlete toAthlete(EntityManager em, Map<Integer, String> teamIdToNameMap) {
		Athlete athlete = new Athlete();
		
		// Basic info
		athlete.setLastName(this.lastName);
		athlete.setFirstName(this.firstName);
		athlete.setFullBirthDate(this.fullBirthDate);
		athlete.setGender(this.gender);
		athlete.setBodyWeight(this.bodyWeight);
		athlete.setPresumedBodyWeight(this.presumedBodyWeight);
		
		// Competition info - resolve by name/code
		if (this.groupName != null) {
			// Use the provided EntityManager when available so lookups occur
			// within the same transaction as the import (avoids visibility issues).
			Group group = null;
			if (em != null) {
				group = GroupRepository.doFindByName(this.groupName, em);
			} else {
				group = GroupRepository.findByName(this.groupName);
			}
			athlete.setGroup(group);
		}
		
		if (this.categoryCode != null) {
			// Use doFindByCode with the current EntityManager to see uncommitted changes
			Category category = CategoryRepository.doFindByCode(this.categoryCode, em);
			athlete.setCategory(category);
		}
		
		// Team: resolve team ID to name using provided map
		if (this.team != null && teamIdToNameMap != null) {
			String teamName = teamIdToNameMap.get(this.team);
			athlete.setTeam(teamName != null ? teamName : "");
		} else if (this.team != null) {
			// Fallback: if no map provided, convert ID to string (backward compatibility)
			athlete.setTeam(this.team.toString());
		} else {
			athlete.setTeam("");
		}
		
		athlete.setMembership(this.membership);
		athlete.setFederationCodes(this.federationCodes);
		athlete.setStartNumber(this.startNumber);
		athlete.setLotNumber(this.lotNumber);
		athlete.setEntryTotal(this.entryTotal);
		athlete.setQualifyingTotal(this.qualifyingTotal);
		
		// Lifts - convert from Integer to String
		athlete.setSnatch1Declaration(formatWeight(this.snatch1Declaration));
		athlete.setSnatch1Change1(formatWeight(this.snatch1Change1));
		athlete.setSnatch1Change2(formatWeight(this.snatch1Change2));
		athlete.setSnatch1ActualLift(formatWeight(this.snatch1ActualLift));
		athlete.setSnatch1LiftTime(this.snatch1LiftTime);
		
		athlete.setSnatch2AutomaticProgression(this.snatch2AutomaticProgression);
		athlete.setSnatch2Declaration(formatWeight(this.snatch2Declaration));
		athlete.setSnatch2Change1(formatWeight(this.snatch2Change1));
		athlete.setSnatch2Change2(formatWeight(this.snatch2Change2));
		athlete.setSnatch2ActualLift(formatWeight(this.snatch2ActualLift));
		athlete.setSnatch2LiftTime(this.snatch2LiftTime);
		
		athlete.setSnatch3AutomaticProgression(this.snatch3AutomaticProgression);
		athlete.setSnatch3Declaration(formatWeight(this.snatch3Declaration));
		athlete.setSnatch3Change1(formatWeight(this.snatch3Change1));
		athlete.setSnatch3Change2(formatWeight(this.snatch3Change2));
		athlete.setSnatch3ActualLift(formatWeight(this.snatch3ActualLift));
		athlete.setSnatch3LiftTime(this.snatch3LiftTime);
		
		athlete.setCleanJerk1Declaration(formatWeight(this.cleanJerk1Declaration));
		athlete.setCleanJerk1Change1(formatWeight(this.cleanJerk1Change1));
		athlete.setCleanJerk1Change2(formatWeight(this.cleanJerk1Change2));
		athlete.setCleanJerk1ActualLift(formatWeight(this.cleanJerk1ActualLift));
		athlete.setCleanJerk1LiftTime(this.cleanJerk1LiftTime);
		
		athlete.setCleanJerk2AutomaticProgression(this.cleanJerk2AutomaticProgression);
		athlete.setCleanJerk2Declaration(formatWeight(this.cleanJerk2Declaration));
		athlete.setCleanJerk2Change1(formatWeight(this.cleanJerk2Change1));
		athlete.setCleanJerk2Change2(formatWeight(this.cleanJerk2Change2));
		athlete.setCleanJerk2ActualLift(formatWeight(this.cleanJerk2ActualLift));
		athlete.setCleanJerk2LiftTime(this.cleanJerk2LiftTime);
		
		athlete.setCleanJerk3AutomaticProgression(this.cleanJerk3AutomaticProgression);
		athlete.setCleanJerk3Declaration(formatWeight(this.cleanJerk3Declaration));
		athlete.setCleanJerk3Change1(formatWeight(this.cleanJerk3Change1));
		athlete.setCleanJerk3Change2(formatWeight(this.cleanJerk3Change2));
		athlete.setCleanJerk3ActualLift(formatWeight(this.cleanJerk3ActualLift));
		athlete.setCleanJerk3LiftTime(this.cleanJerk3LiftTime);
		
		// Personal bests
		athlete.setPersonalBestSnatch(this.personalBestSnatch);
		athlete.setPersonalBestCleanJerk(this.personalBestCleanJerk);
		athlete.setPersonalBestTotal(this.personalBestTotal);
		
		// Rankings
		athlete.setSinclairRank(this.sinclairRank);
		athlete.setqPointsRank(this.qPointsRank);
		athlete.setQMastersRank(this.qAgeRank);
		athlete.setSmhfRank(this.smhfRank);
		athlete.setTeamSinclairRank(this.teamSinclairRank);
		athlete.setCatSinclairRank(this.catSinclairRank);
		athlete.setCatQPointsRank(this.catQPointsRank);
		athlete.setGamxRank(this.gamxRank);
		athlete.setRobiRank(this.robiRank);
		athlete.setQYouthRank(this.ageAdjustedTotalRank);
		athlete.setCombinedRank(this.combinedRank);
		athlete.setTeamCleanJerkRank(this.teamCleanJerkRank);
		athlete.setTeamCombinedRank(this.teamCombinedRank);
		athlete.setTeamCustomRank(this.teamCustomRank);
		athlete.setTeamRobiRank(this.teamRobiRank);
		athlete.setTeamSnatchRank(this.teamSnatchRank);
		athlete.setTeamTotalRank(this.teamTotalRank);
		
		// Other fields
		athlete.setCoach(this.coach);
		athlete.setCustom1(this.custom1);
		athlete.setCustom2(this.custom2);
		athlete.setCustomScore(this.customScore);
		athlete.setEligibleForIndividualRanking(this.eligibleForIndividualRanking != null ? this.eligibleForIndividualRanking : true);
		athlete.setEligibleForTeamRanking(this.eligibleForTeamRanking != null ? this.eligibleForTeamRanking : true);
		athlete.setForcedAsCurrent(this.forcedAsCurrent != null ? this.forcedAsCurrent : false);
		athlete.setSubCategory(this.subCategory);
		
		// Participations - convert from DTOs, filtering out nulls (categories not found)
		if (this.participations != null) {
			List<Participation> participationList = this.participations.stream()
				.map(dto -> dto.toParticipation(em, athlete))
				.filter(p -> p != null)  // Filter out null participations
				.collect(java.util.stream.Collectors.toList());
			athlete.setParticipations(participationList);
		}
		
		return athlete;
	}

	/**
	 * Parse weight string to integer. Empty or null string becomes null.
	 */
	private static Integer parseWeight(String weight) {
		if (weight == null || weight.trim().isEmpty()) {
			return null;
		}
		try {
			// Handle negative values (failed lifts)
			return Integer.parseInt(weight.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Format weight integer to string. Null becomes empty string.
	 */
	private static String formatWeight(Integer weight) {
		return weight != null ? weight.toString() : "";
	}

	// Getters and setters for all fields
	
	public Integer getKey() {
		return key;
	}

	public void setKey(Integer key) {
		this.key = key;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public LocalDate getFullBirthDate() {
		return fullBirthDate;
	}

	public void setFullBirthDate(LocalDate fullBirthDate) {
		this.fullBirthDate = fullBirthDate;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Double getBodyWeight() {
		return bodyWeight;
	}

	public void setBodyWeight(Double bodyWeight) {
		this.bodyWeight = bodyWeight;
	}

	public Double getPresumedBodyWeight() {
		return presumedBodyWeight;
	}

	public void setPresumedBodyWeight(Double presumedBodyWeight) {
		this.presumedBodyWeight = presumedBodyWeight;
	}

	@JsonProperty("sessionName")
	public String getGroupName() {
		return groupName;
	}

	@JsonProperty("sessionName")
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public Integer getTeam() {
		return team;
	}

	public void setTeam(Integer team) {
		this.team = team;
	}

	public String getMembership() {
		return membership;
	}

	public void setMembership(String membership) {
		this.membership = membership;
	}

	public String getFederationCodes() {
		return federationCodes;
	}

	public void setFederationCodes(String federationCodes) {
		this.federationCodes = federationCodes;
	}

	public Integer getStartNumber() {
		return startNumber;
	}

	public void setStartNumber(Integer startNumber) {
		this.startNumber = startNumber;
	}

	public Integer getLotNumber() {
		return lotNumber;
	}

	public void setLotNumber(Integer lotNumber) {
		this.lotNumber = lotNumber;
	}

	public Integer getEntryTotal() {
		return entryTotal;
	}

	public void setEntryTotal(Integer entryTotal) {
		this.entryTotal = entryTotal;
	}

	public Integer getQualifyingTotal() {
		return qualifyingTotal;
	}

	public void setQualifyingTotal(Integer qualifyingTotal) {
		this.qualifyingTotal = qualifyingTotal;
	}

	public Integer getSnatch1Declaration() {
		return snatch1Declaration;
	}

	public void setSnatch1Declaration(Integer snatch1Declaration) {
		this.snatch1Declaration = snatch1Declaration;
	}

	public Integer getSnatch1Change1() {
		return snatch1Change1;
	}

	public void setSnatch1Change1(Integer snatch1Change1) {
		this.snatch1Change1 = snatch1Change1;
	}

	public Integer getSnatch1Change2() {
		return snatch1Change2;
	}

	public void setSnatch1Change2(Integer snatch1Change2) {
		this.snatch1Change2 = snatch1Change2;
	}

	public Integer getSnatch1ActualLift() {
		return snatch1ActualLift;
	}

	public void setSnatch1ActualLift(Integer snatch1ActualLift) {
		this.snatch1ActualLift = snatch1ActualLift;
	}

	public LocalDateTime getSnatch1LiftTime() {
		return snatch1LiftTime;
	}

	public void setSnatch1LiftTime(LocalDateTime snatch1LiftTime) {
		this.snatch1LiftTime = snatch1LiftTime;
	}

	public String getSnatch2AutomaticProgression() {
		return snatch2AutomaticProgression;
	}

	public void setSnatch2AutomaticProgression(String snatch2AutomaticProgression) {
		this.snatch2AutomaticProgression = snatch2AutomaticProgression;
	}

	public Integer getSnatch2Declaration() {
		return snatch2Declaration;
	}

	public void setSnatch2Declaration(Integer snatch2Declaration) {
		this.snatch2Declaration = snatch2Declaration;
	}

	public Integer getSnatch2Change1() {
		return snatch2Change1;
	}

	public void setSnatch2Change1(Integer snatch2Change1) {
		this.snatch2Change1 = snatch2Change1;
	}

	public Integer getSnatch2Change2() {
		return snatch2Change2;
	}

	public void setSnatch2Change2(Integer snatch2Change2) {
		this.snatch2Change2 = snatch2Change2;
	}

	public Integer getSnatch2ActualLift() {
		return snatch2ActualLift;
	}

	public void setSnatch2ActualLift(Integer snatch2ActualLift) {
		this.snatch2ActualLift = snatch2ActualLift;
	}

	public LocalDateTime getSnatch2LiftTime() {
		return snatch2LiftTime;
	}

	public void setSnatch2LiftTime(LocalDateTime snatch2LiftTime) {
		this.snatch2LiftTime = snatch2LiftTime;
	}

	public String getSnatch3AutomaticProgression() {
		return snatch3AutomaticProgression;
	}

	public void setSnatch3AutomaticProgression(String snatch3AutomaticProgression) {
		this.snatch3AutomaticProgression = snatch3AutomaticProgression;
	}

	public Integer getSnatch3Declaration() {
		return snatch3Declaration;
	}

	public void setSnatch3Declaration(Integer snatch3Declaration) {
		this.snatch3Declaration = snatch3Declaration;
	}

	public Integer getSnatch3Change1() {
		return snatch3Change1;
	}

	public void setSnatch3Change1(Integer snatch3Change1) {
		this.snatch3Change1 = snatch3Change1;
	}

	public Integer getSnatch3Change2() {
		return snatch3Change2;
	}

	public void setSnatch3Change2(Integer snatch3Change2) {
		this.snatch3Change2 = snatch3Change2;
	}

	public Integer getSnatch3ActualLift() {
		return snatch3ActualLift;
	}

	public void setSnatch3ActualLift(Integer snatch3ActualLift) {
		this.snatch3ActualLift = snatch3ActualLift;
	}

	public LocalDateTime getSnatch3LiftTime() {
		return snatch3LiftTime;
	}

	public void setSnatch3LiftTime(LocalDateTime snatch3LiftTime) {
		this.snatch3LiftTime = snatch3LiftTime;
	}

	public Integer getCleanJerk1Declaration() {
		return cleanJerk1Declaration;
	}

	public void setCleanJerk1Declaration(Integer cleanJerk1Declaration) {
		this.cleanJerk1Declaration = cleanJerk1Declaration;
	}

	public Integer getCleanJerk1Change1() {
		return cleanJerk1Change1;
	}

	public void setCleanJerk1Change1(Integer cleanJerk1Change1) {
		this.cleanJerk1Change1 = cleanJerk1Change1;
	}

	public Integer getCleanJerk1Change2() {
		return cleanJerk1Change2;
	}

	public void setCleanJerk1Change2(Integer cleanJerk1Change2) {
		this.cleanJerk1Change2 = cleanJerk1Change2;
	}

	public Integer getCleanJerk1ActualLift() {
		return cleanJerk1ActualLift;
	}

	public void setCleanJerk1ActualLift(Integer cleanJerk1ActualLift) {
		this.cleanJerk1ActualLift = cleanJerk1ActualLift;
	}

	public LocalDateTime getCleanJerk1LiftTime() {
		return cleanJerk1LiftTime;
	}

	public void setCleanJerk1LiftTime(LocalDateTime cleanJerk1LiftTime) {
		this.cleanJerk1LiftTime = cleanJerk1LiftTime;
	}

	public String getCleanJerk2AutomaticProgression() {
		return cleanJerk2AutomaticProgression;
	}

	public void setCleanJerk2AutomaticProgression(String cleanJerk2AutomaticProgression) {
		this.cleanJerk2AutomaticProgression = cleanJerk2AutomaticProgression;
	}

	public Integer getCleanJerk2Declaration() {
		return cleanJerk2Declaration;
	}

	public void setCleanJerk2Declaration(Integer cleanJerk2Declaration) {
		this.cleanJerk2Declaration = cleanJerk2Declaration;
	}

	public Integer getCleanJerk2Change1() {
		return cleanJerk2Change1;
	}

	public void setCleanJerk2Change1(Integer cleanJerk2Change1) {
		this.cleanJerk2Change1 = cleanJerk2Change1;
	}

	public Integer getCleanJerk2Change2() {
		return cleanJerk2Change2;
	}

	public void setCleanJerk2Change2(Integer cleanJerk2Change2) {
		this.cleanJerk2Change2 = cleanJerk2Change2;
	}

	public Integer getCleanJerk2ActualLift() {
		return cleanJerk2ActualLift;
	}

	public void setCleanJerk2ActualLift(Integer cleanJerk2ActualLift) {
		this.cleanJerk2ActualLift = cleanJerk2ActualLift;
	}

	public LocalDateTime getCleanJerk2LiftTime() {
		return cleanJerk2LiftTime;
	}

	public void setCleanJerk2LiftTime(LocalDateTime cleanJerk2LiftTime) {
		this.cleanJerk2LiftTime = cleanJerk2LiftTime;
	}

	public String getCleanJerk3AutomaticProgression() {
		return cleanJerk3AutomaticProgression;
	}

	public void setCleanJerk3AutomaticProgression(String cleanJerk3AutomaticProgression) {
		this.cleanJerk3AutomaticProgression = cleanJerk3AutomaticProgression;
	}

	public Integer getCleanJerk3Declaration() {
		return cleanJerk3Declaration;
	}

	public void setCleanJerk3Declaration(Integer cleanJerk3Declaration) {
		this.cleanJerk3Declaration = cleanJerk3Declaration;
	}

	public Integer getCleanJerk3Change1() {
		return cleanJerk3Change1;
	}

	public void setCleanJerk3Change1(Integer cleanJerk3Change1) {
		this.cleanJerk3Change1 = cleanJerk3Change1;
	}

	public Integer getCleanJerk3Change2() {
		return cleanJerk3Change2;
	}

	public void setCleanJerk3Change2(Integer cleanJerk3Change2) {
		this.cleanJerk3Change2 = cleanJerk3Change2;
	}

	public Integer getCleanJerk3ActualLift() {
		return cleanJerk3ActualLift;
	}

	public void setCleanJerk3ActualLift(Integer cleanJerk3ActualLift) {
		this.cleanJerk3ActualLift = cleanJerk3ActualLift;
	}

	public LocalDateTime getCleanJerk3LiftTime() {
		return cleanJerk3LiftTime;
	}

	public void setCleanJerk3LiftTime(LocalDateTime cleanJerk3LiftTime) {
		this.cleanJerk3LiftTime = cleanJerk3LiftTime;
	}

	public Integer getPersonalBestSnatch() {
		return personalBestSnatch;
	}

	public void setPersonalBestSnatch(Integer personalBestSnatch) {
		this.personalBestSnatch = personalBestSnatch;
	}

	public Integer getPersonalBestCleanJerk() {
		return personalBestCleanJerk;
	}

	public void setPersonalBestCleanJerk(Integer personalBestCleanJerk) {
		this.personalBestCleanJerk = personalBestCleanJerk;
	}

	public Integer getPersonalBestTotal() {
		return personalBestTotal;
	}

	public void setPersonalBestTotal(Integer personalBestTotal) {
		this.personalBestTotal = personalBestTotal;
	}

	public Integer getSinclairRank() {
		return sinclairRank;
	}

	public void setSinclairRank(Integer sinclairRank) {
		this.sinclairRank = sinclairRank;
	}

	public Integer getqPointsRank() {
		return qPointsRank;
	}

	public void setqPointsRank(Integer qPointsRank) {
		this.qPointsRank = qPointsRank;
	}

	public Integer getqAgeRank() {
		return qAgeRank;
	}

	public void setqAgeRank(Integer qAgeRank) {
		this.qAgeRank = qAgeRank;
	}

	public Integer getSmhfRank() {
		return smhfRank;
	}

	public void setSmhfRank(Integer smhfRank) {
		this.smhfRank = smhfRank;
	}

	public Integer getTeamSinclairRank() {
		return teamSinclairRank;
	}

	public void setTeamSinclairRank(Integer teamSinclairRank) {
		this.teamSinclairRank = teamSinclairRank;
	}

	public Integer getCatSinclairRank() {
		return catSinclairRank;
	}

	public void setCatSinclairRank(Integer catSinclairRank) {
		this.catSinclairRank = catSinclairRank;
	}

	public Integer getCatQPointsRank() {
		return catQPointsRank;
	}

	public void setCatQPointsRank(Integer catQPointsRank) {
		this.catQPointsRank = catQPointsRank;
	}

	public Integer getGamxRank() {
		return gamxRank;
	}

	public void setGamxRank(Integer gamxRank) {
		this.gamxRank = gamxRank;
	}

	public Integer getRobiRank() {
		return robiRank;
	}

	public void setRobiRank(Integer robiRank) {
		this.robiRank = robiRank;
	}

	public Integer getAgeAdjustedTotalRank() {
		return ageAdjustedTotalRank;
	}

	public void setAgeAdjustedTotalRank(Integer ageAdjustedTotalRank) {
		this.ageAdjustedTotalRank = ageAdjustedTotalRank;
	}

	public Integer getCombinedRank() {
		return combinedRank;
	}

	public void setCombinedRank(Integer combinedRank) {
		this.combinedRank = combinedRank;
	}

	public Integer getTeamCleanJerkRank() {
		return teamCleanJerkRank;
	}

	public void setTeamCleanJerkRank(Integer teamCleanJerkRank) {
		this.teamCleanJerkRank = teamCleanJerkRank;
	}

	public Integer getTeamCombinedRank() {
		return teamCombinedRank;
	}

	public void setTeamCombinedRank(Integer teamCombinedRank) {
		this.teamCombinedRank = teamCombinedRank;
	}

	public Integer getTeamCustomRank() {
		return teamCustomRank;
	}

	public void setTeamCustomRank(Integer teamCustomRank) {
		this.teamCustomRank = teamCustomRank;
	}

	public Integer getTeamRobiRank() {
		return teamRobiRank;
	}

	public void setTeamRobiRank(Integer teamRobiRank) {
		this.teamRobiRank = teamRobiRank;
	}

	public Integer getTeamSnatchRank() {
		return teamSnatchRank;
	}

	public void setTeamSnatchRank(Integer teamSnatchRank) {
		this.teamSnatchRank = teamSnatchRank;
	}

	public Integer getTeamTotalRank() {
		return teamTotalRank;
	}

	public void setTeamTotalRank(Integer teamTotalRank) {
		this.teamTotalRank = teamTotalRank;
	}

	public String getCoach() {
		return coach;
	}

	public void setCoach(String coach) {
		this.coach = coach;
	}

	public String getCustom1() {
		return custom1;
	}

	public void setCustom1(String custom1) {
		this.custom1 = custom1;
	}

	public String getCustom2() {
		return custom2;
	}

	public void setCustom2(String custom2) {
		this.custom2 = custom2;
	}

	public Double getCustomScore() {
		return customScore;
	}

	public void setCustomScore(Double customScore) {
		this.customScore = customScore;
	}

	public Boolean getEligibleForIndividualRanking() {
		return eligibleForIndividualRanking;
	}

	public void setEligibleForIndividualRanking(Boolean eligibleForIndividualRanking) {
		this.eligibleForIndividualRanking = eligibleForIndividualRanking;
	}

	public Boolean getEligibleForTeamRanking() {
		return eligibleForTeamRanking;
	}

	public void setEligibleForTeamRanking(Boolean eligibleForTeamRanking) {
		this.eligibleForTeamRanking = eligibleForTeamRanking;
	}

	public Boolean getForcedAsCurrent() {
		return forcedAsCurrent;
	}

	public void setForcedAsCurrent(Boolean forcedAsCurrent) {
		this.forcedAsCurrent = forcedAsCurrent;
	}

	public String getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}

	public List<ParticipationDTO> getParticipations() {
		return participations;
	}

	public void setParticipations(List<ParticipationDTO> participations) {
		this.participations = participations;
	}
}
