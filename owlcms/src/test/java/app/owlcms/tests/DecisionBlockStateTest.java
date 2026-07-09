/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.displays.scoreboard.DecisionBlockState;
import app.owlcms.displays.scoreboard.DecisionBlockState.DecisionSectionRenderer;
import app.owlcms.displays.scoreboard.DecisionBlockState.RefLights;
import app.owlcms.displays.scoreboard.DecisionBlockState.State;
import app.owlcms.uievents.JuryDeliberationEventType;

/**
 * Unit tests for {@link DecisionBlockState}, the state machine that drives the bottom decision
 * section of the results scoreboard. These tests use a fake renderer that records the sequence of
 * paint calls, so no Vaadin UI or FOP/JPA objects are required. Scenarios follow the behavioral
 * specification in {@code DecisionBlock_SPEC.md}.
 */
public class DecisionBlockStateTest {

	/** Records every renderer call so tests can assert on the paint sequence. */
	private static class FakeRenderer implements DecisionSectionRenderer {
		final List<String> calls = new ArrayList<>();
		int scheduleCount;

		@Override
		public void renderReadyClock() {
			this.calls.add("readyClock");
		}

		@Override
		public void renderAthleteUnderReview(Athlete athlete) {
			this.calls.add("athlete");
		}

		@Override
		public void renderRefereeLights(RefLights lights) {
			this.calls.add("refLights");
		}

		@Override
		public void renderEmptyRefereeLights() {
			this.calls.add("emptyRefLights");
		}

		@Override
		public void renderJuryCircles(Boolean[] votes, int jurySize) {
			this.calls.add("juryCircles");
		}

		@Override
		public void renderNoJuryCircles() {
			this.calls.add("noJuryCircles");
		}

		@Override
		public void renderJuryMessage(JuryDeliberationEventType type) {
			this.calls.add("juryMessage");
		}

		@Override
		public void renderJuryVerdict(boolean good) {
			this.calls.add(good ? "verdictGood" : "verdictBad");
		}

		@Override
		public void renderNoJuryMessage() {
			this.calls.add("noJuryMessage");
		}

		@Override
		public void scheduleReviewTimeout() {
			this.calls.add("scheduleTimeout");
			this.scheduleCount++;
		}

		@Override
		public void cancelReviewTimeout() {
			this.calls.add("cancelTimeout");
		}

		void clear() {
			this.calls.clear();
		}
	}

	private FakeRenderer renderer;
	private DecisionBlockState state;

	private static RefLights lights(boolean good) {
		return new RefLights(good, good, good, good, false, false);
	}

	private static Boolean[] votes(Boolean a, Boolean b, Boolean c) {
		return new Boolean[] { a, b, c };
	}

	@Before
	public void setup() {
		this.renderer = new FakeRenderer();
		this.state = new DecisionBlockState(this.renderer);
		this.state.setJurySize(3);
	}

	@Test
	public void initialStateIsReady() {
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void refereeDecisionEntersDecisionState() {
		this.state.onRefereeDecision(null, lights(true));
		assertEquals(State.DECISION, this.state.getState());
		assertTrue(this.renderer.calls.contains("athlete"));
		assertTrue(this.renderer.calls.contains("refLights"));
		assertTrue(this.renderer.calls.contains("juryCircles"));
		assertTrue(this.renderer.calls.contains("noJuryMessage"));
		// A jury is present (size 3): we wait for them to vote, so no review timeout is started.
		assertFalse(this.renderer.calls.contains("scheduleTimeout"));
	}

	@Test
	public void readyStateShowsEmptyRefereeLightsNotStaleColors() {
		this.state.onRefereeDecision(null, lights(true));
		this.renderer.clear();
		// The announcer starting the next athlete's clock closes the section.
		this.state.onStartTime();
		assertEquals(State.READY, this.state.getState());
		assertTrue(this.renderer.calls.contains("readyClock"));
		assertTrue(this.renderer.calls.contains("emptyRefLights"));
		assertTrue(this.renderer.calls.contains("noJuryCircles"));
		// Referee lights must never be repainted with a color in the ready state.
		assertFalse(this.renderer.calls.contains("refLights"));
	}

	@Test
	public void noJuryStartsReviewTimeoutAndReturnsToReady() {
		// With no jury there is nobody to wait for, so the display is bounded by the review timeout.
		this.state.setJurySize(0);
		this.state.onRefereeDecision(null, lights(false));
		assertEquals(State.DECISION, this.state.getState());
		assertTrue(this.renderer.calls.contains("scheduleTimeout"));
		assertTrue(this.renderer.calls.contains("noJuryCircles"));
		this.renderer.clear();
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
		assertTrue(this.renderer.calls.contains("cancelTimeout"));
		assertTrue(this.renderer.calls.contains("readyClock"));
	}

	@Test
	public void announceOnlyEventsDoNotCloseTheSection() {
		// The FOP posts DecisionReset (~3s) and declares the next clock; neither closes the section.
		this.state.onRefereeDecision(null, lights(true));
		this.state.onDecisionReset();
		assertEquals(State.DECISION, this.state.getState());
		this.state.onResetOnNewClock();
		assertEquals(State.DECISION, this.state.getState());
	}

	@Test
	public void athleteClockStartClosesTheSection() {
		// Announcer announces the next athlete and starts time: that wins.
		this.state.onRefereeDecision(null, lights(true));
		this.state.onStartTime();
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void startLiftingReturnsToReady() {
		this.state.onRefereeDecision(null, lights(true));
		this.state.onStartLifting();
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void juryUpdateInDecisionUpdatesCirclesInPlaceWithoutRescheduling() {
		this.state.onRefereeDecision(null, lights(true));
		int schedulesAfterDecision = this.renderer.scheduleCount;
		this.renderer.clear();
		this.state.onJuryUpdate(votes(true, null, null), 3);
		assertEquals(State.DECISION, this.state.getState());
		assertTrue(this.renderer.calls.contains("juryCircles"));
		// The review timeout must not restart on a jury vote.
		assertEquals(schedulesAfterDecision, this.renderer.scheduleCount);
	}

	@Test
	public void juryUpdateIgnoredInReadyState() {
		this.state.onJuryUpdate(votes(true, true, true), 3);
		assertEquals(State.READY, this.state.getState());
		assertFalse(this.renderer.calls.contains("juryCircles"));
	}

	@Test
	public void deliberationStartShowsMessageAndNoCirclesByDefault() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.contains("athlete"));
		assertTrue(this.renderer.calls.contains("juryMessage"));
		assertTrue(this.renderer.calls.contains("noJuryCircles"));
		assertFalse(this.renderer.calls.contains("juryCircles"));
	}

	@Test
	public void deliberationShowsCirclesWhenSecondVotePublic() {
		this.state.setShowBothJuryVotes(true);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.contains("juryCircles"));
		this.renderer.clear();
		this.state.onJuryUpdate(votes(true, null, null), 3);
		assertTrue(this.renderer.calls.contains("juryCircles"));
	}

	@Test
	public void deliberationIgnoresJuryUpdateWhenSecondVoteHidden() {
		this.state.setShowBothJuryVotes(false);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.renderer.clear();
		this.state.onJuryUpdate(votes(true, true, true), 3);
		assertFalse(this.renderer.calls.contains("juryCircles"));
	}

	@Test
	public void refereeDecisionIgnoredDuringDeliberation() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.renderer.clear();
		this.state.onRefereeDecision(null, lights(false));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.isEmpty());
	}

	@Test
	public void juryVerdictStaysUntilStartLifting() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(true, false);
		assertEquals(State.DELIBERATION, this.state.getState());
		this.state.onStartLifting();
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void endBreakDoesNotReturnToReadyOnItsOwn() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onEndBreak();
		assertEquals(State.DELIBERATION, this.state.getState());
		this.state.onStartLifting();
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void breakStartedJuryIsIgnoredButOtherBreakReturnsToReady() {
		this.state.onRefereeDecision(null, lights(true));
		this.state.onBreakStarted(true);
		assertEquals(State.DECISION, this.state.getState());
		this.state.onBreakStarted(false);
		assertEquals(State.READY, this.state.getState());
	}

	/**
	 * Happy path with a jury: three referees decide, three jurors vote, no intervening timer. Because
	 * a jury is present the section waits (no review timeout) and holds the result and circles until
	 * the announcer starts the next athlete's clock (the FOP's fast decision-reset does not cut it
	 * short).
	 */
	@Test
	public void withJuryWaitsForVoteThenAnnouncerClockStart() {
		this.state.onRefereeDecision(null, lights(true));
		assertEquals(State.DECISION, this.state.getState());
		// Jurors vote one by one, no timer delay.
		this.state.onJuryUpdate(votes(true, null, null), 3);
		this.state.onJuryUpdate(votes(true, true, null), 3);
		this.state.onJuryUpdate(votes(true, true, true), 3);
		assertEquals(State.DECISION, this.state.getState());
		// A jury is present: no review timeout is ever started.
		assertEquals(0, this.renderer.scheduleCount);
		// FOP clears its flash and declares the next clock meanwhile; neither closes the section.
		this.state.onDecisionReset();
		this.state.onResetOnNewClock();
		assertEquals(State.DECISION, this.state.getState());
		// The announcer starts the next athlete's clock: that wins.
		this.state.onStartTime();
		assertEquals(State.READY, this.state.getState());
	}

	/**
	 * Second scenario: three referees decide, three jurors vote, the jury deliberates and votes
	 * again; the verdict is shown and control passes to the announcer, so the section stays put
	 * until whatever the announcer does to restart (start lifting).
	 */
	@Test
	public void deliberationRevoteWaitsForAnnouncerRestart() {
		this.state.setShowBothJuryVotes(true);
		this.state.onRefereeDecision(null, lights(true));
		this.state.onJuryUpdate(votes(true, true, true), 3);
		// Jury opens a deliberation.
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		assertEquals(State.DELIBERATION, this.state.getState());
		// Jury votes again.
		this.state.onJuryUpdate(votes(false, null, null), 3);
		this.state.onJuryUpdate(votes(false, false, null), 3);
		this.state.onJuryUpdate(votes(false, false, false), 3);
		// First notification awaits the announcer: the attempt board has not shown the verdict yet,
		// so neither do we — the deliberation label stays.
		this.renderer.clear();
		this.state.onJuryVerdict(false, true);
		assertEquals(State.DELIBERATION, this.state.getState());
		assertFalse(this.renderer.calls.contains("verdictBad"));
		// The announcer confirms (or the automatic outcome fires): now the verdict shows, matching
		// the attempt board, and stays on screen.
		this.renderer.clear();
		this.state.onJuryVerdict(false, false);
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.contains("verdictBad"));
		// A FOP decision-reset or end-of-break must not clear it either.
		this.state.onDecisionReset();
		this.state.onEndBreak();
		assertEquals(State.DELIBERATION, this.state.getState());
		// Whatever the announcer does to restart posts StartLifting.
		this.state.onStartLifting();
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void juryVerdictIgnoredOutsideDeliberation() {
		// A stray verdict with no active deliberation must not paint a label.
		this.state.onRefereeDecision(null, lights(true));
		this.renderer.clear();
		this.state.onJuryVerdict(true, false);
		assertEquals(State.DECISION, this.state.getState());
		assertFalse(this.renderer.calls.contains("verdictGood"));
	}
}
