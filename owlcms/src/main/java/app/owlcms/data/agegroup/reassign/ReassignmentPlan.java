/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup.reassign;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import app.owlcms.data.category.Category;

/**
 * The computed result of a reassignment, before anything is written. An executable plan is local to the transaction
 * that computed it: added categories are managed entities of that transaction and must not escape it.
 */
public final class ReassignmentPlan {

	public enum Mode {
		/** Only athletes already in the championship; nobody is added to a championship. */
		WITHIN,
		/** Every athlete against every active category; athletes join championships they fit. */
		ALL
	}

	/** The changes for one athlete within one championship. */
	public static final class ChampionshipChange {
		private final String championshipName;
		private final Set<Long> keptCategoryIds = new LinkedHashSet<>();
		private final Set<Long> removedCategoryIds = new LinkedHashSet<>();
		private final List<Category> addedCategories = new ArrayList<>();
		private final List<String> removedCategoryNames = new ArrayList<>();
		private final List<String> addedCategoryNames = new ArrayList<>();
		private boolean leaves;
		private boolean joins;
		private LeaveReason leaveReason;

		ChampionshipChange(String championshipName) {
			this.championshipName = championshipName;
		}

		public String championshipName() {
			return this.championshipName;
		}

		public Set<Long> keptCategoryIds() {
			return this.keptCategoryIds;
		}

		public Set<Long> removedCategoryIds() {
			return this.removedCategoryIds;
		}

		/** Managed entities of the plan's transaction. */
		public List<Category> addedCategories() {
			return this.addedCategories;
		}

		public boolean leaves() {
			return this.leaves;
		}

		/** Display names captured while the plan was computed, for the dry-run summary. */
		public List<String> removedCategoryNames() {
			return this.removedCategoryNames;
		}

		public List<String> addedCategoryNames() {
			return this.addedCategoryNames;
		}

		public boolean joins() {
			return this.joins;
		}

		public LeaveReason leaveReason() {
			return this.leaveReason;
		}

		void markLeaves(LeaveReason reason) {
			this.leaves = true;
			this.leaveReason = reason;
		}

		void markJoins() {
			this.joins = true;
		}
	}

	/** All the changes for one athlete, per championship (case-insensitive keys). */
	public static final class AthleteChange {
		private final Long athleteId;
		private final String athleteName;
		private final Map<String, ChampionshipChange> byChampionship = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		AthleteChange(Long athleteId, String athleteName) {
			this.athleteId = athleteId;
			this.athleteName = athleteName;
		}

		public Long athleteId() {
			return this.athleteId;
		}

		public String athleteName() {
			return this.athleteName;
		}

		public Map<String, ChampionshipChange> byChampionship() {
			return this.byChampionship;
		}
	}

	private final Mode mode;
	private final List<AthleteChange> changes = new ArrayList<>();
	private final Map<Long, AthleteChange> changesByAthleteId = new LinkedHashMap<>();
	private final Map<Long, Integer> finalCategoryCounts;
	private final Map<String, Boolean> mixedTeamsByChampionship;

	ReassignmentPlan(Mode mode, Map<Long, Integer> finalCategoryCounts, Map<String, Boolean> mixedTeamsByChampionship) {
		this.mode = mode;
		this.finalCategoryCounts = finalCategoryCounts != null ? Map.copyOf(finalCategoryCounts) : Map.of();
		this.mixedTeamsByChampionship = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (mixedTeamsByChampionship != null) {
			this.mixedTeamsByChampionship.putAll(mixedTeamsByChampionship);
		}
	}

	public Mode mode() {
		return this.mode;
	}

	/** Every athlete in scope, including those whose only change is the registration category recomputation. */
	public List<AthleteChange> changes() {
		return this.changes;
	}

	/** The change record for an athlete, created on first use. */
	AthleteChange changeFor(AthleteSnapshot snapshot) {
		return this.changesByAthleteId.computeIfAbsent(snapshot.id(), id -> {
			AthleteChange change = new AthleteChange(snapshot.id(), snapshot.fullName());
			this.changes.add(change);
			return change;
		});
	}

	/** Final category count per age group id, with the edited age group's kept-and-added count overriding. */
	public Map<Long, Integer> finalCategoryCounts() {
		return this.finalCategoryCounts;
	}

	/** Effective mixed-team policy per championship name (case-insensitive), resolved inside the transaction. */
	public Map<String, Boolean> mixedTeamsByChampionship() {
		return this.mixedTeamsByChampionship;
	}
}
