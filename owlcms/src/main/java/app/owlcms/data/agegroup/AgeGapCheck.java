/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import app.owlcms.data.athlete.Gender;

/**
 * The age-gap check (spec 3.4 step 1): per gender, the proposed active age groups of a championship must cover a
 * continuous range of ages, from the lowest minimum to the highest maximum. Overlaps are allowed; ages below the
 * lowest minimum or above the highest maximum are not gaps. Pure function, no database access.
 *
 * Overlaps are not gaps and are never refused: {@link #overlaps} reports all of them, next to all the gaps, always,
 * because nothing tells a championship where they are a mistake from one where they are intended. The person
 * making the change sees everything there is to fix, and may continue.
 */
public final class AgeGapCheck {

	/** A range of ages that no proposed active age group covers, both ends inclusive. */
	public record Gap(Gender gender, int fromAge, int toAge) {
	}

	/** Ages that two active age groups of the same gender both cover, both ends inclusive. */
	public record Overlap(Gender gender, String firstCode, String secondCode, int fromAge, int toAge) {
	}

	private AgeGapCheck() {
	}

	/** Every pair of age groups of the same gender that share ages, with the ages they share. */
	public static List<Overlap> overlaps(List<AgeGroup> proposedActiveAgeGroups) {
		List<Overlap> overlaps = new ArrayList<>();
		Map<Gender, List<AgeGroup>> byGender = proposedActiveAgeGroups.stream()
		        .filter(ag -> ag.getGender() != null && ag.getMinAge() != null && ag.getMaxAge() != null)
		        .collect(Collectors.groupingBy(AgeGroup::getGender));
		for (Map.Entry<Gender, List<AgeGroup>> entry : byGender.entrySet()) {
			List<AgeGroup> sorted = entry.getValue().stream()
			        .sorted(Comparator.comparing(AgeGroup::getMinAge).thenComparing(AgeGroup::getMaxAge))
			        .toList();
			for (int i = 0; i < sorted.size(); i++) {
				for (int j = i + 1; j < sorted.size(); j++) {
					AgeGroup first = sorted.get(i);
					AgeGroup second = sorted.get(j);
					int from = Math.max(first.getMinAge(), second.getMinAge());
					int to = Math.min(first.getMaxAge(), second.getMaxAge());
					if (from <= to) {
						overlaps.add(new Overlap(entry.getKey(), first.getCode(), second.getCode(), from, to));
					}
				}
			}
		}
		overlaps.sort(Comparator.comparing(Overlap::gender).thenComparing(Overlap::fromAge)
		        .thenComparing(Overlap::firstCode, Comparator.nullsFirst(Comparator.naturalOrder())));
		return overlaps;
	}

	public static List<Gap> gaps(List<AgeGroup> proposedActiveAgeGroups) {
		List<Gap> gaps = new ArrayList<>();
		Map<Gender, List<AgeGroup>> byGender = proposedActiveAgeGroups.stream()
		        .filter(ag -> ag.getGender() != null && ag.getMinAge() != null && ag.getMaxAge() != null)
		        .collect(Collectors.groupingBy(AgeGroup::getGender));
		for (Map.Entry<Gender, List<AgeGroup>> entry : byGender.entrySet()) {
			List<AgeGroup> intervals = entry.getValue().stream()
			        .sorted(Comparator.comparing(AgeGroup::getMinAge))
			        .toList();
			int coveredUpTo = intervals.get(0).getMaxAge();
			for (int i = 1; i < intervals.size(); i++) {
				AgeGroup next = intervals.get(i);
				if (next.getMinAge() > coveredUpTo + 1) {
					gaps.add(new Gap(entry.getKey(), coveredUpTo + 1, next.getMinAge() - 1));
				}
				coveredUpTo = Math.max(coveredUpTo, next.getMaxAge());
			}
		}
		gaps.sort(Comparator.comparing(Gap::gender).thenComparing(Gap::fromAge));
		return gaps;
	}

	/**
	 * The holes that removing one age group from the active list opens. Holes that were already there are not
	 * repeated, and removing a group that is not in the list opens nothing.
	 */
	public static List<Gap> gapsOpenedByRemoving(List<AgeGroup> activeAgeGroups, AgeGroup removed) {
		List<Gap> before = gaps(activeAgeGroups);
		List<AgeGroup> remaining = activeAgeGroups.stream().filter(ag -> !isSame(ag, removed)).toList();
		return gaps(remaining).stream().filter(g -> !before.contains(g)).toList();
	}

	private static boolean isSame(AgeGroup a, AgeGroup b) {
		return a == b || (a.getId() != null && a.getId().equals(b.getId()));
	}
}
