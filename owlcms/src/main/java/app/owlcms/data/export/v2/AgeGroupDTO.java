/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;

/**
 * DTO for AgeGroup in V2 export format.
 * Uses code as the primary identifier instead of ID.
 * Contains CategoryDTO objects with codes instead of Category entities with IDs.
 */
public class AgeGroupDTO {
	
	private String code;                    // Primary identifier
	private String key;
	private boolean active;
	private Integer minAge;
	private Integer maxAge;
	private Gender gender;
	private String ageDivision;
	private String championshipName;
	private ChampionshipType championshipType;
	private Integer qualificationTotal;
	private Boolean alreadyGendered;
	private Ranking scoringSystem;
	private Ranking bestAthleteScoringSystem;
	private Boolean medals;
	private List<CategoryDTO> categories = new ArrayList<>();
	
	public AgeGroupDTO() {
	}
	
	/**
	 * Convert from domain AgeGroup object to DTO
	 */
	public static AgeGroupDTO fromAgeGroup(AgeGroup ageGroup) {
		if (ageGroup == null) {
			return null;
		}
		AgeGroupDTO dto = new AgeGroupDTO();
		dto.setCode(ageGroup.getCode());
		dto.setKey(ageGroup.getKey());
		dto.setActive(ageGroup.isActive());
		dto.setMinAge(ageGroup.getMinAge());
		dto.setMaxAge(ageGroup.getMaxAge());
		dto.setGender(ageGroup.getGender());
		dto.setAgeDivision(ageGroup.getAgeDivision());
		dto.setChampionshipName(ageGroup.getChampionshipName());
		dto.setChampionshipType(ageGroup.getChampionshipType());
		dto.setQualificationTotal(ageGroup.getQualificationTotal());
		dto.setAlreadyGendered(ageGroup.isAlreadyGendered());
		dto.setScoringSystem(ageGroup.getScoringSystem());
		dto.setBestAthleteScoringSystem(ageGroup.getBestAthleteScoringSystem());
		dto.setMedals(ageGroup.getMedals());
		
		// Convert categories to DTOs with codes
		if (ageGroup.getCategories() != null) {
			dto.setCategories(
				ageGroup.getCategories().stream()
					.map(CategoryDTO::fromCategory)
					.collect(Collectors.toList())
			);
		}
		
		return dto;
	}
	
	/**
	 * Convert from DTO back to domain AgeGroup object (for import)
	 */
	public AgeGroup toAgeGroup() {
		AgeGroup ag = new AgeGroup();
		ag.setCode(this.code);
		ag.setKey(this.key);
		ag.setActive(this.active);
		ag.setMinAge(this.minAge);
		ag.setMaxAge(this.maxAge);
		ag.setGender(this.gender);
		ag.setAgeDivision(this.ageDivision);
		ag.setChampionshipName(this.championshipName);
		ag.setChampionshipType(this.championshipType);
		ag.setQualificationTotal(this.qualificationTotal);
		ag.setAlreadyGendered(this.alreadyGendered != null ? this.alreadyGendered : false);
		ag.setScoringSystem(this.scoringSystem);
		ag.setBestAthleteScoringSystem(this.bestAthleteScoringSystem);
		ag.setMedals(this.medals);
		
		// Convert CategoryDTOs back to Category entities
		if (this.categories != null) {
			for (CategoryDTO catDto : this.categories) {
				Category cat = catDto.toCategory();
				ag.addCategory(cat);  // This sets the bidirectional relationship
			}
		}
		
		return ag;
	}
	
	// Getters and setters
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public Integer getMinAge() {
		return minAge;
	}
	
	public void setMinAge(Integer minAge) {
		this.minAge = minAge;
	}
	
	public Integer getMaxAge() {
		return maxAge;
	}
	
	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}
	
	public Gender getGender() {
		return gender;
	}
	
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	
	public String getAgeDivision() {
		return ageDivision;
	}
	
	public void setAgeDivision(String ageDivision) {
		this.ageDivision = ageDivision;
	}
	
	public String getChampionshipName() {
		return championshipName;
	}
	
	public void setChampionshipName(String championshipName) {
		this.championshipName = championshipName;
	}
	
	public ChampionshipType getChampionshipType() {
		return championshipType;
	}
	
	public void setChampionshipType(ChampionshipType championshipType) {
		this.championshipType = championshipType;
	}
	
	public Integer getQualificationTotal() {
		return qualificationTotal;
	}
	
	public void setQualificationTotal(Integer qualificationTotal) {
		this.qualificationTotal = qualificationTotal;
	}
	
	public Boolean getAlreadyGendered() {
		return alreadyGendered;
	}
	
	public void setAlreadyGendered(Boolean alreadyGendered) {
		this.alreadyGendered = alreadyGendered;
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
	
	public Boolean getMedals() {
		return medals;
	}
	
	public void setMedals(Boolean medals) {
		this.medals = medals;
	}
	
	public List<CategoryDTO> getCategories() {
		return categories;
	}
	
	public void setCategories(List<CategoryDTO> categories) {
		this.categories = categories;
	}
}
