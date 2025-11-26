/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

/**
 * DTO for Team in V2 export format.
 * Teams are synthetic objects created from the team name strings used by athletes.
 * The ID is the hashcode of the team name for referential integrity.
 */
public class TeamDTO {
	
	private Integer id;  // Hashcode of team name
	private String name;
	
	public TeamDTO() {
	}
	
	public TeamDTO(String name) {
		this.name = name;
		this.id = name != null ? name.hashCode() : null;
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		// Update id when name changes
		this.id = name != null ? name.hashCode() : null;
	}
}
