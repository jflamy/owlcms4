package app.owlcms.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class CompetitionSimulatorTest {

	@Test
	public void skipBeforeUsesNaturalComparisonWhenBoundaryStartsWithNumber() {
		assertTrue(CompetitionSimulator.isBeforeSkipBoundary("1 RED", "10 RED"));
		assertFalse(CompetitionSimulator.isBeforeSkipBoundary("10 RED", "1 RED"));
		assertFalse(CompetitionSimulator.isBeforeSkipBoundary("10 RED", "10 RED"));
		assertFalse(CompetitionSimulator.isBeforeSkipBoundary("11 RED", "10 RED"));
	}

	@Test
	public void skipBeforeFallsBackToCaseInsensitiveStringComparison() {
		assertTrue(CompetitionSimulator.isBeforeSkipBoundary("Alpha", "bravo"));
		assertFalse(CompetitionSimulator.isBeforeSkipBoundary("Bravo", "bravo"));
	}

	@Test
	public void blankSkipBeforeDoesNotSkipAnything() {
		assertFalse(CompetitionSimulator.isBeforeSkipBoundary("1 RED", null));
		assertFalse(CompetitionSimulator.isBeforeSkipBoundary("1 RED", ""));
		assertFalse(CompetitionSimulator.isBeforeSkipBoundary("1 RED", "   "));
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