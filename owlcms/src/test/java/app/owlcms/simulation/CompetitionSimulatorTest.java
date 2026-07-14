package app.owlcms.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class CompetitionSimulatorTest {

	@Test
	public void skipBeforeUsesTheRequestedGroupPositionInComputedOrder() {
		List<String> computedOrder = List.of("M1", "M2", "F1", "Y1");

		assertEquals(2, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "F1"));
		assertEquals(2, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "f1"));
		assertEquals(0, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "M1"));
		assertEquals(3, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "Y1"));
	}

	@Test
	public void skipBeforeStartsAtTheRequestedSessionOrSessionBlock() {
		List<String> computedOrder = List.of("1", "3", "2", "47A", "47B", "4", "48");

		int sessionIndex = CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "3");
		int sessionBlockIndex = CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "47");

		assertEquals(List.of("3", "2", "47A", "47B", "4", "48"), computedOrder.subList(sessionIndex, computedOrder.size()));
		assertEquals(List.of("47A", "47B", "4", "48"), computedOrder.subList(sessionBlockIndex, computedOrder.size()));
	}

	@Test
	public void skipBeforeNumericSessionBlockDoesNotMatchAnotherNumericGroup() {
		List<String> computedOrder = List.of("3", "30", "31A");

		assertEquals(0, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "3"));
		assertEquals(1, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "30"));
	}

	@Test
	public void missingSkipBeforeBoundaryHasNoPosition() {
		List<String> computedOrder = List.of("M1", "M2", "F1", "Y1");

		assertEquals(-1, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "X1"));
		assertEquals(-1, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, null));
		assertEquals(-1, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, ""));
		assertEquals(-1, CompetitionSimulator.findSkipBoundaryIndex(computedOrder, "   "));
	}

	@Test
	public void platformFilterParsesCommaDelimitedNamesCaseInsensitively() {
		Set<String> platforms = CompetitionSimulator.parsePlatformFilter(" blue, RED ,, White ");

		assertEquals(3, platforms.size());
		assertTrue(platforms.contains("blue"));
		assertTrue(platforms.contains("red"));
		assertTrue(platforms.contains("white"));
	}

	@Test
	public void platformFilterMatchesPlatformNamesCaseInsensitively() {
		Set<String> platforms = CompetitionSimulator.parsePlatformFilter("blue, red");

		assertTrue(CompetitionSimulator.platformMatchesFilter("BLUE", platforms));
		assertTrue(CompetitionSimulator.platformMatchesFilter("Red", platforms));
		assertFalse(CompetitionSimulator.platformMatchesFilter("WHITE", platforms));
		assertFalse(CompetitionSimulator.platformMatchesFilter(null, platforms));
	}

	@Test
	public void blankPlatformFilterAllowsAnyPlatform() {
		Set<String> platforms = CompetitionSimulator.parsePlatformFilter("   ");

		assertTrue(CompetitionSimulator.platformMatchesFilter("BLUE", platforms));
	}

}