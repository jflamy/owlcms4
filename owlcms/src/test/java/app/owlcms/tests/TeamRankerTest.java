package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.team.TeamRanker;
import app.owlcms.data.team.TeamTreeItem;

public class TeamRankerTest {
	private static final Championship TEST_CHAMPIONSHIP = new Championship() {
		@Override
		public Ranking getScoringSystem() {
			return Ranking.TOTAL;
		}

		@Override
		public boolean isSnatchCJTotalMedals() {
			return false;
		}
	};

	@Test
	public void sortAndRankUsesCompetitionRanksForTies() {
		TeamTreeItem fourth = teamWithPoints("Fourth", 4);
		TeamTreeItem tiedSecondA = teamWithPoints("Second A", 10);
		TeamTreeItem first = teamWithPoints("First", 12);
		TeamTreeItem tiedSecondB = teamWithPoints("Second B", 10);

		List<TeamTreeItem> ranked = TeamRanker.sortAndRank(
		        List.of(fourth, tiedSecondA, first, tiedSecondB),
		        TeamTreeItem.pointComparator);

		assertEquals(List.of("First", "Second A", "Second B", "Fourth"),
		        ranked.stream().map(TeamTreeItem::getName).toList());
		assertEquals(List.of(1, 2, 2, 4),
		        ranked.stream().map(TeamTreeItem::getRank).toList());
	}

	private TeamTreeItem teamWithPoints(String name, int points) {
		TeamTreeItem item = new TeamTreeItem(name, Gender.M, null, false, TEST_CHAMPIONSHIP);
		item.getTeam().setPoints(points);
		return item;
	}
}