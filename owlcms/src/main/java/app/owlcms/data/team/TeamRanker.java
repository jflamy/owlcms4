package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TeamRanker {

	private TeamRanker() {
	}

	public static List<TeamTreeItem> sortAndRank(List<TeamTreeItem> teams,
	        Comparator<TeamTreeItem> comparator) {
		if (teams == null || teams.isEmpty()) {
			return List.of();
		}

		List<TeamTreeItem> sorted = new ArrayList<>(teams);
		sorted.sort(comparator);

		int rank = 0;
		TeamTreeItem previous = null;
		for (int index = 0; index < sorted.size(); index++) {
			TeamTreeItem current = sorted.get(index);
			if (previous == null || comparator.compare(previous, current) != 0) {
				rank = index + 1;
			}
			current.setRank(rank);
			previous = current;
		}
		return sorted;
	}
}