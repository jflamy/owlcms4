/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import app.owlcms.data.category.Category;

/**
 * Classifies the difference between a stored age group and an editor scratchpad. Categories are compared by upper
 * limit, never by id or code; a duplicate upper limit in the scratchpad is an error (the UI validates first).
 */
public final class AgeGroupChange {

	public enum Kind {
		/** No pending change. */
		NONE,
		/** Code/name, already-gendered or medals only: saved directly, participations untouched. */
		DIRECT,
		/** Age range, gender, active, any QT, or an upper limit added or removed: the championship is reassigned. */
		REASSIGN,
		/** Only the championship association changes: confirmed and saved with no reassignment. */
		CHAMPIONSHIP_ONLY,
		/** Championship plus any other pending change: refused, nothing written. */
		CHAMPIONSHIP_MIXED
	}

	private AgeGroupChange() {
	}

	public static Kind classify(AgeGroup stored, AgeGroup scratch) {
		if (stored == null || scratch == null) {
			throw new IllegalArgumentException("classify needs both the stored age group and the scratchpad");
		}
		boolean championshipChanged = !Objects.equals(canonical(stored.getChampionshipName()),
		        canonical(scratch.getChampionshipName()));
		boolean directChanged = !Objects.equals(stored.getCode(), scratch.getCode())
		        || stored.isAlreadyGendered() != scratch.isAlreadyGendered()
		        || !Objects.equals(stored.getMedals(), scratch.getMedals());
		boolean reassignChanged = stored.isActive() != scratch.isActive()
		        || stored.getGender() != scratch.getGender()
		        || !Objects.equals(stored.getMinAge(), scratch.getMinAge())
		        || !Objects.equals(stored.getMaxAge(), scratch.getMaxAge())
		        || categoriesDiffer(stored, scratch);

		if (championshipChanged && (directChanged || reassignChanged)) {
			return Kind.CHAMPIONSHIP_MIXED;
		}
		if (championshipChanged) {
			return Kind.CHAMPIONSHIP_ONLY;
		}
		if (reassignChanged) {
			return Kind.REASSIGN;
		}
		if (directChanged) {
			return Kind.DIRECT;
		}
		return Kind.NONE;
	}

	/** Duplicate upper limits in the scratchpad's categories. */
	public static void validateNoDuplicateUpperLimits(AgeGroup scratch) {
		Map<Double, Integer> byUpperLimit = new LinkedHashMap<>();
		for (Category c : scratch.getCategories()) {
			Double max = c.getMaximumWeight();
			if (max == null) {
				continue;
			}
			if (byUpperLimit.put(max, c.getQualifyingTotal()) != null) {
				throw new IllegalArgumentException("duplicate upper limit " + max + " in " + scratch.getCode());
			}
		}
	}

	private static boolean categoriesDiffer(AgeGroup stored, AgeGroup scratch) {
		validateNoDuplicateUpperLimits(scratch);
		Map<Double, Integer> storedQt = new LinkedHashMap<>();
		for (Category c : stored.getCategories()) {
			storedQt.put(c.getMaximumWeight(), c.getQualifyingTotal());
		}
		Map<Double, Integer> scratchQt = new LinkedHashMap<>();
		for (Category c : scratch.getCategories()) {
			scratchQt.put(c.getMaximumWeight(), c.getQualifyingTotal());
		}
		if (!storedQt.keySet().equals(scratchQt.keySet())) {
			return true;
		}
		for (Map.Entry<Double, Integer> entry : storedQt.entrySet()) {
			if (!Objects.equals(entry.getValue(), scratchQt.get(entry.getKey()))) {
				return true;
			}
		}
		return false;
	}

	private static String canonical(String championshipName) {
		return Championship.canonicalizeChampionshipName(
		        championshipName != null && !championshipName.isBlank() ? championshipName.trim() : null);
	}
}
