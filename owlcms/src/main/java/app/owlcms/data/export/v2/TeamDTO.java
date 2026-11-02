/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An entry of the V2 {@code teams} table. There is no team entity: an athlete's team is its team name. V2 writes
 * the athlete's team as a numeric id, the hash of the name, and lists the names in this table; external readers rely
 * on that form. Athletes also carry the name itself ({@code teamName}).
 */
public class TeamDTO {

	private Integer id;
	private String name;

	public TeamDTO() {
	}

	public TeamDTO(String name) {
		this.name = name;
		this.id = idOf(name);
	}

	/** @return the id of a team name, null for no team */
	public static Integer idOf(String name) {
		return name == null || name.isBlank() ? null : name.hashCode();
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
		this.id = idOf(name);
	}

	/**
	 * Two different names can hash to the same id; readers that use the {@code teams} table would then give their
	 * athletes the same team.
	 *
	 * @return the names sharing an id, by id; empty when every id is unique
	 */
	public static Map<Integer, List<String>> idCollisions(Collection<String> teamNames) {
		Map<Integer, List<String>> byId = new TreeMap<>();
		for (String name : new TreeSet<>(teamNames)) {
			byId.computeIfAbsent(idOf(name), id -> new ArrayList<>()).add(name);
		}
		byId.values().removeIf(names -> names.size() < 2);
		return byId;
	}
}
