/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;

/**
 * DTO for Category in V2 export format.
 * Uses code instead of ID for portability and readability.
 * The code is computed as: ageGroupCode_gender_weight (e.g., "SR_F48")
 */
public class CategoryDTO {
	
	private String code;           // Computed code like "SR_F48" - primary identifier
	private String categoryName;   // Display name like "SR F 48"
	private Double maximumWeight;
	private Double minimumWeight;
	private Gender gender;
	private boolean active;
	private int qualifyingTotal;
	private Integer wrSr;
	private Integer wrJr;
	private Integer wrYth;
	
	public CategoryDTO() {
	}
	
	/**
	 * Convert from domain Category object to DTO
	 */
	public static CategoryDTO fromCategory(Category category) {
		if (category == null) {
			return null;
		}
		CategoryDTO dto = new CategoryDTO();
		dto.setCode(category.getCode());  // This calls getComputedCode() internally
		dto.setCategoryName(category.getCategoryName());
		dto.setMaximumWeight(category.getMaximumWeight());
		dto.setMinimumWeight(category.getMinimumWeight());
		dto.setGender(category.getGender());
		dto.setActive(category.isActive());
		dto.setQualifyingTotal(category.getQualifyingTotal());
		dto.setWrSr(category.getWrSr());
		dto.setWrJr(category.getWrJr());
		dto.setWrYth(category.getWrYth());
		return dto;
	}
	
	/**
	 * Convert from DTO back to domain Category object (for import)
	 * Note: The code is a computed field and will be overwritten by Category.getComputedCode()
	 */
	public Category toCategory() {
		Category cat = new Category();
		// Note: Don't set code directly - it's computed from ageGroup + gender + weight
		// The code from DTO is used for lookup only
		cat.setMaximumWeight(this.maximumWeight);
		cat.setMinimumWeight(this.minimumWeight);
		cat.setGender(this.gender);
		cat.setActive(this.active);
		cat.setQualifyingTotal(this.qualifyingTotal);
		cat.setWrSr(this.wrSr);
		cat.setWrJr(this.wrJr);
		cat.setWrYth(this.wrYth);
		return cat;
	}
	
	// Getters and setters
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getCategoryName() {
		return categoryName;
	}
	
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}
	
	public Double getMaximumWeight() {
		return maximumWeight;
	}
	
	public void setMaximumWeight(Double maximumWeight) {
		this.maximumWeight = maximumWeight;
	}
	
	public Double getMinimumWeight() {
		return minimumWeight;
	}
	
	public void setMinimumWeight(Double minimumWeight) {
		this.minimumWeight = minimumWeight;
	}
	
	public Gender getGender() {
		return gender;
	}
	
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public int getQualifyingTotal() {
		return qualifyingTotal;
	}
	
	public void setQualifyingTotal(int qualifyingTotal) {
		this.qualifyingTotal = qualifyingTotal;
	}
	
	public Integer getWrSr() {
		return wrSr;
	}
	
	public void setWrSr(Integer wrSr) {
		this.wrSr = wrSr;
	}
	
	public Integer getWrJr() {
		return wrJr;
	}
	
	public void setWrJr(Integer wrJr) {
		this.wrJr = wrJr;
	}
	
	public Integer getWrYth() {
		return wrYth;
	}
	
	public void setWrYth(Integer wrYth) {
		this.wrYth = wrYth;
	}
}
