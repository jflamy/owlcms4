/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import javax.persistence.EntityManager;

import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;

/**
 * DTO for Participation in V2 export format.
 * Uses category code instead of ID references.
 */
public class ParticipationDTO {
	
	private String categoryCode;  // Instead of category ID
	private Integer cleanJerkRank;
	private Integer totalRank;
	private Integer combinedRank;
	private Integer customRank;
	private Integer snatchRank;
	private Integer teamCJRank;
	private Integer teamCombinedRank;
	private Boolean teamMember;
	private Integer teamRobiRank;
	private Integer teamSinclairRank;
	private Integer teamSnatchRank;
	private Integer teamTotalRank;
	private String championshipType;
	
	public ParticipationDTO() {
	}
	
	/**
	 * Convert from domain Participation object to DTO
	 */
	public static ParticipationDTO fromParticipation(Participation participation) {
		ParticipationDTO dto = new ParticipationDTO();
		
		// Use getComputedCode() which correctly formats the category code
		dto.setCategoryCode(participation.getCategory() != null ? participation.getCategory().getComputedCode() : null);
		dto.setCleanJerkRank(participation.getCleanJerkRank());
		dto.setTotalRank(participation.getTotalRank());
		dto.setCombinedRank(participation.getCombinedRank());
		dto.setCustomRank(participation.getCustomRank());
		dto.setSnatchRank(participation.getSnatchRank());
		dto.setTeamCJRank(participation.getTeamCJRank());
		dto.setTeamCombinedRank(participation.getTeamCombinedRank());
		dto.setTeamMember(participation.getTeamMember());
		dto.setTeamRobiRank(participation.getTeamRobiRank());
		dto.setTeamSinclairRank(participation.getTeamSinclairRank());
		dto.setTeamSnatchRank(participation.getTeamSnatchRank());
		dto.setTeamTotalRank(participation.getTeamTotalRank());
		ChampionshipType ct = participation.getChampionshipType();
		dto.setChampionshipType(ct != null ? ct.name() : null);
		
		return dto;
	}
	
	/**
	 * Convert from DTO back to domain Participation object
	 * Returns null if category cannot be found (to handle gracefully)
	 */
	public Participation toParticipation(EntityManager em, Athlete athlete) {
		// Look up category by code using the current EntityManager to see uncommitted changes
		Category category = null;
		if (categoryCode != null) {
			category = CategoryRepository.doFindByCode(categoryCode, em);
		}
		
		// If category not found, return null (will be filtered out)
		if (category == null) {
			return null;
		}
		
		// Use public constructor
		Participation participation = new Participation(athlete, category);
		
		participation.setCleanJerkRank(cleanJerkRank != null ? cleanJerkRank : 0);
		participation.setTotalRank(totalRank != null ? totalRank : 0);
		participation.setCombinedRank(combinedRank != null ? combinedRank : 0);
		participation.setCustomRank(customRank != null ? customRank : 0);
		participation.setSnatchRank(snatchRank != null ? snatchRank : 0);
		participation.setTeamCJRank(teamCJRank != null ? teamCJRank : 0);
		participation.setTeamCombinedRank(teamCombinedRank != null ? teamCombinedRank : 0);
		participation.setTeamMember(teamMember != null ? teamMember : false);
		participation.setTeamRobiRank(teamRobiRank != null ? teamRobiRank : 0);
		participation.setTeamSinclairRank(teamSinclairRank != null ? teamSinclairRank : 0);
		participation.setTeamSnatchRank(teamSnatchRank != null ? teamSnatchRank : 0);
		participation.setTeamTotalRank(teamTotalRank != null ? teamTotalRank : 0);
		// Note: championshipType is computed from category's age group, not stored
		
		return participation;
	}
	
	// Getters and setters
	public String getCategoryCode() {
		return categoryCode;
	}
	
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	
	public Integer getCleanJerkRank() {
		return cleanJerkRank;
	}
	
	public void setCleanJerkRank(Integer cleanJerkRank) {
		this.cleanJerkRank = cleanJerkRank;
	}
	
	public Integer getTotalRank() {
		return totalRank;
	}
	
	public void setTotalRank(Integer totalRank) {
		this.totalRank = totalRank;
	}
	
	public Integer getCombinedRank() {
		return combinedRank;
	}
	
	public void setCombinedRank(Integer combinedRank) {
		this.combinedRank = combinedRank;
	}
	
	public Integer getCustomRank() {
		return customRank;
	}
	
	public void setCustomRank(Integer customRank) {
		this.customRank = customRank;
	}
	
	public Integer getSnatchRank() {
		return snatchRank;
	}
	
	public void setSnatchRank(Integer snatchRank) {
		this.snatchRank = snatchRank;
	}
	
	public Integer getTeamCJRank() {
		return teamCJRank;
	}
	
	public void setTeamCJRank(Integer teamCJRank) {
		this.teamCJRank = teamCJRank;
	}
	
	public Integer getTeamCombinedRank() {
		return teamCombinedRank;
	}
	
	public void setTeamCombinedRank(Integer teamCombinedRank) {
		this.teamCombinedRank = teamCombinedRank;
	}
	
	public Boolean getTeamMember() {
		return teamMember;
	}
	
	public void setTeamMember(Boolean teamMember) {
		this.teamMember = teamMember;
	}
	
	public Integer getTeamRobiRank() {
		return teamRobiRank;
	}
	
	public void setTeamRobiRank(Integer teamRobiRank) {
		this.teamRobiRank = teamRobiRank;
	}
	
	public Integer getTeamSinclairRank() {
		return teamSinclairRank;
	}
	
	public void setTeamSinclairRank(Integer teamSinclairRank) {
		this.teamSinclairRank = teamSinclairRank;
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
	
	public String getChampionshipType() {
		return championshipType;
	}
	
	public void setChampionshipType(String championshipType) {
		this.championshipType = championshipType;
	}
}
