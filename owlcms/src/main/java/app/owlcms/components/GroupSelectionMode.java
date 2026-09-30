package app.owlcms.components;

import java.util.Comparator;
import java.util.List;

import app.owlcms.data.group.Group;

public enum GroupSelectionMode {
	DONE_ONLY,
	NOT_DONE_ONLY,
	DONE_FIRST,
	NOT_DONE_FIRST;

	public List<Group> apply(List<Group> groups) {
		return switch (this) {
			case DONE_ONLY -> groups.stream().filter(group -> Boolean.TRUE.equals(group.isDone())).toList();
			case NOT_DONE_ONLY -> groups.stream().filter(group -> !Boolean.TRUE.equals(group.isDone())).toList();
			case DONE_FIRST -> groups.stream().sorted(doneComparator(true)).toList();
			case NOT_DONE_FIRST -> groups.stream().sorted(doneComparator(false)).toList();
		};
	}

	public boolean hasSeparator() {
		return this == DONE_FIRST || this == NOT_DONE_FIRST;
	}

	private static Comparator<Group> doneComparator(boolean doneFirst) {
		return Comparator.comparingInt(group -> Boolean.TRUE.equals(group.isDone()) == doneFirst ? 0 : 1);
	}
}