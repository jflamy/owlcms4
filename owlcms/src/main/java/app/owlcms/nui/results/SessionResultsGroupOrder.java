/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.utils.NaturalOrderComparator;

final class SessionResultsGroupOrder {

	private SessionResultsGroupOrder() {
	}

	static List<Group> sortedGroups(List<Group> groups, Collection<FieldOfPlay> fops) {
		Set<Long> currentGroupIds = new HashSet<>();
		for (FieldOfPlay fop : fops) {
			Group group = fop.getGroup();
			if (group != null && group.getId() != null) {
				currentGroupIds.add(group.getId());
			}
		}

		groups.sort(resultsGroupComparator(currentGroupIds));
		return groups;
	}

	private static Comparator<Group> resultsGroupComparator(Set<Long> currentGroupIds) {
		NaturalOrderComparator<Group> naturalOrder = new NaturalOrderComparator<>();
		Comparator<LocalDateTime> latestFirst = Comparator.nullsLast(Comparator.reverseOrder());

		return (group1, group2) -> {
			boolean group1Done = group1.isDone();
			boolean group2Done = group2.isDone();
			int doneComparison = Boolean.compare(group2Done, group1Done);
			if (doneComparison != 0) {
				return doneComparison;
			}

			if (group1Done) {
				int finishComparison = latestFirst.compare(group1.getLastCJDecisionTime(),
				        group2.getLastCJDecisionTime());
				if (finishComparison != 0) {
					return finishComparison;
				}
			} else {
				boolean group1Current = currentGroupIds.contains(group1.getId());
				boolean group2Current = currentGroupIds.contains(group2.getId());
				int currentComparison = Boolean.compare(group2Current, group1Current);
				if (currentComparison != 0) {
					return currentComparison;
				}
			}

			int scheduledComparison = latestFirst.compare(group1.getCompetitionTime(), group2.getCompetitionTime());
			if (scheduledComparison != 0) {
				return scheduledComparison;
			}
			return naturalOrder.compare(group1, group2);
		};
	}
}
