package app.owlcms.nui.results;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import app.owlcms.data.group.Group;

public class SessionResultsGroupOrderTest {

	@Test
	public void completedSessionsComeFirstWithMostRecentCompletionFirst() {
		LocalDateTime now = LocalDateTime.of(2026, 11, 7, 20, 0);
		Group olderCompleted = group("older", true, now.minusHours(2), now.minusDays(1));
		Group newerCompleted = group("newer", true, now.minusHours(1), now.minusDays(2));
		Group scheduled = group("scheduled", false, null, now.plusDays(1));
		Group unscheduled = group("unscheduled", false, null, null);
		List<Group> groups = new ArrayList<>(
		        List.of(scheduled, olderCompleted, unscheduled, newerCompleted));

		List<Group> sorted = SessionResultsGroupOrder.sortedGroups(groups, List.of());

		assertEquals(List.of("newer", "older", "scheduled", "unscheduled"),
		        sorted.stream().map(Group::getName).toList());
	}

	private static Group group(String name, boolean done, LocalDateTime completed, LocalDateTime scheduled) {
		Group group = new Group(name);
		group.setDone(done);
		group.setLastCJDecisionTime(completed);
		group.setCompetitionTime(scheduled);
		return group;
	}
}
