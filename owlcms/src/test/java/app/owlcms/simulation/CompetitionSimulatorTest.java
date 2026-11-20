package app.owlcms.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.tests.TestStartup;

public class CompetitionSimulatorTest {

	@BeforeClass
	public static void setupTests() {
		TestStartup.init();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Test
	public void announcerStartsClockOnlyForAnAnnouncedOrStoppedAttempt() {
		for (FOPState state : FOPState.values()) {
			boolean allowed = state == FOPState.CURRENT_ATHLETE_DISPLAYED || state == FOPState.TIME_STOPPED;
			assertEquals(state.name(), allowed, FOPSimulator.canStartClock(state));
		}
		assertFalse(FOPSimulator.canStartClock(null));
	}

	@Test
	public void marshalChangesExcludeAttemptedAthleteOnlyDuringDecision() {
		assertTrue(FOPSimulator.allowsAttemptedAthleteChange(FOPSimulator.MarshalChangePhase.BEFORE_CLOCK));
		assertTrue(FOPSimulator.allowsAttemptedAthleteChange(FOPSimulator.MarshalChangePhase.CLOCK_RUNNING));
		assertFalse(FOPSimulator.allowsAttemptedAthleteChange(FOPSimulator.MarshalChangePhase.DURING_DECISION));
	}

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

	@Test
	public void simulationUsesNoJuryForResetPlatforms() {
		assertEquals(0, CompetitionSimulator.effectiveJurySize(4, 0, true));
		assertEquals(4, CompetitionSimulator.effectiveJurySize(4, 0, false));
		assertEquals(4, CompetitionSimulator.effectiveJurySize(4, 3, true));
		assertEquals(4, CompetitionSimulator.effectiveJurySize(4, null, true));
	}

	@Test
	public void missingStartingWeightUsesEntryTotalWithoutReplacingEnteredOpener() {
		CompetitionSimulator simulator = new CompetitionSimulator();
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setSnatch1Declaration("43");

		simulator.setMissingStartingWeights(athlete, 80, 100);

		assertEquals("43", athlete.getSnatch1Declaration());
		assertEquals("56", athlete.getCleanJerk1Declaration());
	}

	@Test
	public void zeroStartingWeightsAreDerivedFromEntryTotal() {
		CompetitionSimulator simulator = new CompetitionSimulator();
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setSnatch1Declaration("0");
		athlete.setCleanJerk1Declaration("0");

		simulator.setMissingStartingWeights(athlete, 80, 100);

		assertEquals("44", athlete.getSnatch1Declaration());
		assertEquals("56", athlete.getCleanJerk1Declaration());
	}

}