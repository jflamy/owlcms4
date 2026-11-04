/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup.reassign;

import java.util.Comparator;
import java.util.List;

/**
 * The immutable dry-run summary shown in the confirmation dialogs. Built from a plan inside its transaction;
 * contains only plain values, so it can safely outlive the rollback and be rendered by the UI. Apply never consumes
 * it: an accepted save recomputes from fresh data.
 */
public record ReassignmentPreview(List<Entry> entries) {

	public enum Change {
		LEAVES,
		JOINS,
		/** Stays in the championship with different categories. */
		MOVES
	}

	/** The categories the athlete is taken out of and put into, by display name. */
	public record Entry(Long athleteId, String athleteName, String championshipName, Change change,
	        LeaveReason reason, List<String> currentCategories, List<String> afterCategories) {
		public Entry {
			currentCategories = currentCategories != null ? List.copyOf(currentCategories) : List.of();
			afterCategories = afterCategories != null ? List.copyOf(afterCategories) : List.of();
		}
	}

	public ReassignmentPreview {
		entries = entries != null
		        ? List.copyOf(entries.stream()
		                .sorted(Comparator.comparing(Entry::championshipName, String.CASE_INSENSITIVE_ORDER)
		                        .thenComparing(Entry::athleteName, String.CASE_INSENSITIVE_ORDER))
		                .toList())
		        : List.of();
	}

	/** Athletes leaving the given championship, in display order. */
	public List<Entry> leavers(String championshipName) {
		return this.entries.stream()
		        .filter(e -> e.change() == Change.LEAVES
		                && e.championshipName().equalsIgnoreCase(championshipName))
		        .toList();
	}

	/** Athletes joining the given championship, in display order. */
	public List<Entry> joiners(String championshipName) {
		return this.entries.stream()
		        .filter(e -> e.change() == Change.JOINS
		                && e.championshipName().equalsIgnoreCase(championshipName))
		        .toList();
	}
}
