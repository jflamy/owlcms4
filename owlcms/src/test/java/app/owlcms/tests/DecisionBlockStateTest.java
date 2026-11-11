/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.displays.DecisionDisplayRules;
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
		String rule;
		Boolean[] juryVotes;
		int jurySize;

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
			this.juryVotes = votes;
			this.jurySize = jurySize;
		}

		@Override
		public void renderNoJuryCircles() {
			this.calls.add("noJuryCircles");
		}

		boolean[] votedRefs;
		boolean[] votedJury;

		@Override
		public void renderVotedRefereeLights(boolean[] voted) {
			this.calls.add("votedRefLights");
			this.votedRefs = voted;
		}

		@Override
		public void renderVotedJuryCircles(boolean[] voted) {
			this.calls.add("votedJuryCircles");
			this.votedJury = voted;
		}

		@Override
		public void renderJuryMessage(JuryDeliberationEventType type) {
			this.calls.add("juryMessage");
		}

		@Override
		public void renderJuryVerdict(boolean good, String rule) {
			this.calls.add(good ? "verdictGood" : "verdictBad");
			this.rule = rule;
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
	public void panelReviewVoteIsHiddenThenRevealedTogether() {
		this.state.setJurySize(4);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.renderer.clear();

		// vote called: the initial referee lights are cleared, nothing else is shown
		this.state.onPanelVote(null, null);
		assertTrue(this.renderer.calls.contains("emptyRefLights"));
		assertTrue(this.renderer.calls.contains("noJuryCircles"));
		assertFalse(this.renderer.calls.contains("refLights"));

		// jury votes arriving during the vote are not shown on their own
		this.renderer.clear();
		this.state.onJuryUpdate(new Boolean[] { false, false, false, false }, 4);
		assertFalse(this.renderer.calls.contains("juryCircles"));

		// all seven in: referee lights and jury circles together, even when the second vote is not public
		this.renderer.clear();
		this.state.onPanelVote(lights(false), new Boolean[] { false, false, false, true });
		assertTrue(this.renderer.calls.contains("refLights"));
		assertTrue(this.renderer.calls.contains("juryCircles"));
		assertEquals(State.DELIBERATION, this.state.getState());
	}

	@Test
	public void reviewBreakShowsTheJuryVotesThatLedToItUntilTheVoteIsCalled() {
		this.state.setJurySize(4);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true),
		        new Boolean[] { false, false, false, false });
		assertTrue("referee lights", this.renderer.calls.contains("refLights"));
		assertTrue("jury votes shown", this.renderer.calls.contains("juryCircles"));

		// Call for Votes: the cells are cleared
		this.renderer.clear();
		this.state.onPanelVote(null, null, new boolean[7]);
		assertTrue(this.renderer.calls.contains("votedRefLights"));
		assertTrue(this.renderer.calls.contains("votedJuryCircles"));
		assertFalse(this.renderer.calls.contains("juryCircles"));
		assertFalse(this.renderer.calls.contains("refLights"));
	}

	@Test
	public void hiddenJuryVotesShowNothingOfTheReview() {
		this.state.setShowJuryVotes(false);
		this.state.setJurySize(4);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true),
		        new Boolean[] { false, false, false, false });
		assertTrue(this.renderer.calls.contains("noJuryCircles"));
		assertFalse(this.renderer.calls.contains("juryCircles"));
		this.renderer.clear();
		this.state.onPanelVote(null, null, new boolean[7]);
		this.state.onPanelVote(lights(false), new Boolean[] { false, false, false, true });
		assertFalse("the review vote is not shown", this.renderer.calls.contains("votedJuryCircles")
		        || this.renderer.calls.contains("juryCircles"));
		this.state.onJuryVerdict(false, false, "rule");
		assertTrue("only the decision", this.renderer.calls.contains("verdictBad"));
	}

	@Test
	public void hiddenJuryVotesAreNotShownOnTheLift() {
		this.state.setShowJuryVotes(false);
		this.state.setJurySize(3);
		this.state.onRefereeDecision(null, lights(true));
		this.state.onJuryUpdate(votes(true, true, null), 3);
		assertFalse(this.renderer.calls.contains("juryCircles"));
		assertFalse("a jury is still voting: no review timeout", this.renderer.calls.contains("scheduleTimeout"));
		this.state.onJuryUpdate(votes(true, true, true), 3);
		assertFalse(this.renderer.calls.contains("juryCircles"));
		assertEquals("the outcome is known, also when the votes are hidden", 1, this.renderer.scheduleCount);
	}

	@Test
	public void panelReviewVoteShowsWhoHasVotedInGray() {
		this.state.setJurySize(4);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.renderer.clear();

		boolean[] voted = { true, false, true, true, true, true, true };
		this.state.onPanelVote(null, null, voted);
		assertTrue(this.renderer.calls.contains("votedRefLights"));
		assertTrue(this.renderer.calls.contains("votedJuryCircles"));
		assertFalse("never the values", this.renderer.calls.contains("refLights"));
		assertFalse("never the values", this.renderer.calls.contains("juryCircles"));
		assertTrue(Arrays.equals(new boolean[] { true, false, true }, this.renderer.votedRefs));
		assertTrue(Arrays.equals(new boolean[] { true, true, true, true }, this.renderer.votedJury));
		assertTrue("the review label stays", this.renderer.calls.contains("juryMessage"));

		// a jury update during the vote does not disclose anything
		this.renderer.clear();
		this.state.onJuryUpdate(new Boolean[] { false, false, false, false }, 4);
		assertFalse(this.renderer.calls.contains("juryCircles"));

		// the public reveal replaces the gray cells with the values
		this.renderer.clear();
		this.state.onPanelVote(lights(false), new Boolean[] { false, false, false, true }, null);
		assertTrue(this.renderer.calls.contains("refLights"));
		assertTrue(this.renderer.calls.contains("juryCircles"));
		assertFalse(this.renderer.calls.contains("votedRefLights"));
	}

	@Test
	public void juryVoteKeepsNonVotingRefereesEmpty() {
		this.state.setJurySize(3);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.renderer.clear();

		// a jury vote (no seven-person panel): the referees do not vote again
		boolean[] voted = { false, false, false, true, true, false };
		this.state.onPanelVote(null, null, voted, lights(true));
		assertEquals("referee lights while the jury votes",
		        !DecisionDisplayRules.HIDE_REFEREES_DISPLAYS, this.renderer.calls.contains("refLights"));
		assertEquals("cleared referee lights while the jury votes",
		        DecisionDisplayRules.HIDE_REFEREES_DISPLAYS, this.renderer.calls.contains("emptyRefLights"));
		assertFalse(this.renderer.calls.contains("votedRefLights"));
		assertTrue("who has voted in the jury", this.renderer.calls.contains("votedJuryCircles"));
		assertTrue(Arrays.equals(new boolean[] { true, true, false }, this.renderer.votedJury));
		assertFalse("never the jury values", this.renderer.calls.contains("juryCircles"));

		// Only the jurors voted: the last vote must not restore the original referee lights.
		this.renderer.clear();
		this.state.onPanelVote(lights(false), votes(true, true, true), null, lights(true));
		assertEquals(!DecisionDisplayRules.HIDE_REFEREES_DISPLAYS, this.renderer.calls.contains("refLights"));
		assertTrue(this.renderer.calls.contains("juryCircles"));
		assertEquals(DecisionDisplayRules.HIDE_REFEREES_DISPLAYS, this.renderer.calls.contains("emptyRefLights"));
	}

	@Test
	public void juryOnlyResultsShowOnlyTheJurorsForEveryJurySizeAndReviewType() {
		for (int size = 3; size <= 5; size++) {
			for (JuryDeliberationEventType type : List.of(JuryDeliberationEventType.START_DELIBERATION,
			        JuryDeliberationEventType.CHALLENGE)) {
				setup();
				this.state.setJurySize(size);
				this.state.onDeliberationStart(type, null, lights(false));
				this.state.onPanelVote(null, null, new boolean[3 + size], lights(false));
				Boolean[] jury = new Boolean[size];
				Arrays.fill(jury, true);
				jury[size - 1] = false;
				this.renderer.clear();
				this.state.onPanelVote(lights(false), jury, null, lights(false));
				assertTrue(this.renderer.calls.contains("emptyRefLights"));
				assertFalse(this.renderer.calls.contains("refLights"));
				assertFalse(this.renderer.calls.contains("votedRefLights"));
				assertEquals(size, this.renderer.jurySize);
				assertEquals(Arrays.asList(jury), Arrays.asList(this.renderer.juryVotes));

				this.renderer.clear();
				this.state.onJuryVerdict(false, false, "rule");
				assertTrue(this.renderer.calls.contains("emptyRefLights"));
				assertFalse(this.renderer.calls.contains("refLights"));
			}
		}
	}

	@Test
	public void panelVoteOutsideADeliberationIsIgnored() {
		this.state.onPanelVote(null, null);
		assertEquals(State.READY, this.state.getState());
		assertTrue(this.renderer.calls.isEmpty());
	}

	@Test
	public void refereeDecisionEntersDecisionState() {
		this.state.onRefereeDecision(null, lights(true));
		assertEquals(State.DECISION, this.state.getState());
		assertTrue(this.renderer.calls.contains("athlete"));
		assertTrue(this.renderer.calls.contains("refLights"));
		assertTrue(this.renderer.calls.contains("juryCircles"));
		assertTrue(this.renderer.calls.contains("noJuryMessage"));
		// A jury is present (size 3) and has not voted: we wait for them, so no review timeout is started.
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
	public void deliberationStartShowsMessageAndNoCirclesWhenJuryVotesHidden() {
		this.state.setShowJuryVotes(false);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.contains("athlete"));
		assertTrue(this.renderer.calls.contains("juryMessage"));
		assertTrue(this.renderer.calls.contains("noJuryCircles"));
		assertFalse(this.renderer.calls.contains("juryCircles"));
	}

	@Test
	public void deliberationShowsCirclesWhenJuryVotesShown() {
		this.state.setShowJuryVotes(true);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.contains("juryCircles"));
		this.renderer.clear();
		this.state.onJuryUpdate(votes(true, null, null), 3);
		assertTrue(this.renderer.calls.contains("juryCircles"));
	}

	@Test
	public void deliberationIgnoresJuryUpdateWhenJuryVotesHidden() {
		this.state.setShowJuryVotes(false);
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
	public void verdictStaysAfterAutomaticResumeUntilTimeout() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false, "3 - rule");
		int schedules = this.renderer.scheduleCount;
		this.renderer.clear();
		this.state.onStartLifting(true);
		assertEquals("the verdict stays after the automatic resume", State.DELIBERATION, this.state.getState());
		assertEquals("the review timeout is not started again", schedules, this.renderer.scheduleCount);
		assertFalse(this.renderer.calls.contains("readyClock"));
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void verdictSurvivesTheResyncsThatFollowTheResume() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false, "11 - rule");
		int schedules = this.renderer.scheduleCount;
		this.renderer.clear();
		// the scoreboard receives StartLifting then SwitchGroup; each re-syncs with the FOP before the handler runs
		this.state.onStartLifting(true);
		this.state.onStartLifting(true);
		this.state.onStartLifting(true);
		assertEquals("the verdict stays", State.DELIBERATION, this.state.getState());
		assertFalse(this.renderer.calls.contains("readyClock"));
		assertEquals("the timeout is not restarted", schedules, this.renderer.scheduleCount);
	}

	/** The 20 s are counted from the outcome: the verdict starts the timeout, the resume does not. */
	@Test
	public void verdictStartsTheReviewTimeoutOnceAndExpiresToReady() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(true, true);
		assertEquals("the announcer has not confirmed: nothing is shown", 0, this.renderer.scheduleCount);
		this.state.onJuryVerdict(true, false);
		assertEquals("the timeout starts at the verdict", 1, this.renderer.scheduleCount);
		this.state.onStartLifting(true);
		assertEquals("the resume does not start it again", 1, this.renderer.scheduleCount);
		assertEquals(State.DELIBERATION, this.state.getState());
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());

		// the resume never came
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false);
		assertEquals(2, this.renderer.scheduleCount);
		this.state.onReviewTimeout();
		assertEquals("the verdict ends without a start-lifting", State.READY, this.state.getState());
	}

	@Test
	public void chairDecisionDuringTheVoteShowsTheVerdict() {
		this.state.setJurySize(4);
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onPanelVote(null, null, new boolean[] { true, true, true, false, true, true, true });
		// the Jury Chair enters the decision: the votes are cleared, the review goes on
		this.state.onJuryDisplayReset();
		assertEquals(State.DELIBERATION, this.state.getState());
		this.renderer.clear();
		this.state.onJuryVerdict(false, false, "1 - Bending and extending of the arms");
		assertTrue("the verdict is shown", this.renderer.calls.contains("verdictBad"));
	}

	@Test
	public void verdictEndsOnClockStartOrResumeOrBreakOrNextDecision() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(true, false);
		this.state.onStartLifting(true);
		this.state.onStartTime();
		assertEquals("clock start", State.READY, this.state.getState());

		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(true, false);
		this.state.onStartLifting(true);
		this.state.onJuryDisplayReset();
		assertEquals("explicit Resume", State.READY, this.state.getState());

		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(true, false);
		this.state.onStartLifting(true);
		this.renderer.clear();
		this.state.onRefereeDecision(null, lights(false));
		assertEquals("next lift replaces it", State.DECISION, this.state.getState());
		assertTrue("its timeout is cancelled", this.renderer.calls.contains("cancelTimeout"));

		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(true, false);
		this.state.onStartLifting(true);
		this.state.onBreakStarted(false);
		assertEquals("break started by an official", State.READY, this.state.getState());
	}

	@Test
	public void resumeWithoutVerdictReturnsToReady() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onStartLifting(true);
		assertEquals("break ended before a verdict", State.READY, this.state.getState());
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
	 * The lift ended the snatches or the session, with a jury that has not finished voting: no clock will start and
	 * no timer starts with the break, the section waits for the missing votes like the attempt board. The last jury
	 * vote starts the review timeout, which returns the section to ready.
	 */
	@Test
	public void liftsDoneWithJuryWaitsForLastVoteThenReviewTimeout() {
		this.state.onRefereeDecision(null, lights(true));
		assertEquals("a jury is voting: no timeout", 0, this.renderer.scheduleCount);
		this.state.onLiftsDone();
		assertEquals(State.DECISION, this.state.getState());
		assertEquals("no timer at the break", 0, this.renderer.scheduleCount);
		this.renderer.clear();
		this.state.onJuryUpdate(votes(true, null, null), 3);
		this.state.onJuryUpdate(votes(true, false, null), 3);
		assertEquals("the section keeps waiting", 0, this.renderer.scheduleCount);
		this.state.onJuryUpdate(votes(true, false, true), 3);
		assertTrue("the jury votes are shown", this.renderer.calls.contains("juryCircles"));
		assertEquals("the last vote starts the timer", 1, this.renderer.scheduleCount);
		assertEquals(State.DECISION, this.state.getState());
		// the break is reported again (countdown changed)
		this.state.onLiftsDone();
		assertEquals("a running timeout is not restarted", 1, this.renderer.scheduleCount);
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
	}

	/** No jury: the timeout started with the decision goes on through the break. */
	@Test
	public void liftsDoneWithoutJuryKeepsDecisionAndRunningTimeout() {
		this.state.setJurySize(0);
		this.state.onRefereeDecision(null, lights(false));
		assertEquals(1, this.renderer.scheduleCount);
		this.state.onLiftsDone();
		assertEquals(State.DECISION, this.state.getState());
		assertEquals("the timeout started with the decision goes on", 1, this.renderer.scheduleCount);
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
	}

	/** A jury verdict on the last snatch or the last lift stays until the review timeout. */
	@Test
	public void liftsDoneKeepsJuryVerdictUntilReviewTimeout() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false);
		// lifting resumes by itself, then the break follows
		this.state.onStartLifting(true);
		this.state.onLiftsDone();
		assertEquals(State.DELIBERATION, this.state.getState());
		assertEquals(1, this.renderer.scheduleCount);
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());

		// the break follows without a resume
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false);
		this.state.onLiftsDone();
		assertEquals(State.DELIBERATION, this.state.getState());
		assertEquals(2, this.renderer.scheduleCount);
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
	}

	/** Without a decision or a verdict to keep, the break returns the section to ready; a reload always does. */
	@Test
	public void liftsDoneWithNothingToKeepReturnsToReady() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onLiftsDone();
		assertEquals(State.READY, this.state.getState());

		this.state.onRefereeDecision(null, lights(true));
		this.state.onLiftsDone();
		this.renderer.clear();
		// session reload
		this.state.onStartLifting(false);
		assertEquals(State.READY, this.state.getState());
		assertTrue("the reload cancels the timeout", this.renderer.calls.contains("cancelTimeout"));
		this.state.onRefereeDecision(null, lights(true));
		this.state.onLiftsDone();
		assertEquals("no timer while the jury votes are missing", 0, this.renderer.scheduleCount);
	}

	/**
	 * Happy path with a jury: three referees decide, three jurors vote. While votes are missing the section waits
	 * (no review timeout). The last vote makes the outcome known: the review timeout starts then, and the section
	 * holds the result and circles until it expires (the FOP's fast decision-reset does not cut it short).
	 */
	@Test
	public void withJuryLastVoteStartsTheReviewTimeout() {
		this.state.onRefereeDecision(null, lights(true));
		assertEquals(State.DECISION, this.state.getState());
		this.state.onJuryUpdate(votes(true, null, null), 3);
		this.state.onJuryUpdate(votes(true, true, null), 3);
		assertEquals("jurors are still voting", 0, this.renderer.scheduleCount);
		this.state.onJuryUpdate(votes(true, true, true), 3);
		assertEquals(State.DECISION, this.state.getState());
		assertEquals("the outcome is known", 1, this.renderer.scheduleCount);
		// the same votes reported again do not start the delay again
		this.state.onJuryUpdate(votes(true, true, true), 3);
		assertEquals(1, this.renderer.scheduleCount);
		// FOP clears its flash and declares the next clock meanwhile; neither closes the section.
		this.state.onDecisionReset();
		this.state.onResetOnNewClock();
		assertEquals(State.DECISION, this.state.getState());
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
	}

	/** Every jury size: the seven-person panel (4 jurors) confirming the referees, and the 5-member jury. */
	@Test
	public void lastVoteStartsTheReviewTimeoutForEveryJurySize() {
		for (int size : new int[] { 3, 4, 5 }) {
			setup();
			this.state.setJurySize(size);
			this.state.onRefereeDecision(null, lights(true));
			// the FOP always reports five slots; only the first ones are jurors
			Boolean[] votes = new Boolean[5];
			for (int i = 0; i < size - 1; i++) {
				votes[i] = true;
			}
			this.state.onJuryUpdate(votes, size);
			assertEquals("jury of " + size + " still voting", 0, this.renderer.scheduleCount);
			votes[size - 1] = true;
			this.state.onJuryUpdate(votes, size);
			assertEquals("jury of " + size + " has voted", 1, this.renderer.scheduleCount);
		}
	}

	/** The jurors may all vote before the referee decision is shown: the delay then starts with the decision. */
	@Test
	public void juryVotesBeforeTheDecisionStartTheReviewTimeoutWithIt() {
		this.state.onJuryUpdate(votes(true, true, true), 3);
		assertEquals(State.READY, this.state.getState());
		assertEquals(0, this.renderer.scheduleCount);
		this.state.onRefereeDecision(null, lights(true));
		assertEquals(1, this.renderer.scheduleCount);
		assertTrue(this.renderer.calls.contains("juryCircles"));
	}

	/** A review started during the delay, automatically or by hand, takes over at once. */
	@Test
	public void reviewDuringTheDelayTakesOver() {
		this.state.onRefereeDecision(null, lights(true));
		this.state.onJuryUpdate(votes(false, false, false), 3);
		assertEquals(1, this.renderer.scheduleCount);
		this.renderer.clear();
		this.state.onDeliberationStart(JuryDeliberationEventType.CHALLENGE, null, lights(true));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue("the delay is cancelled", this.renderer.calls.contains("cancelTimeout"));
		// a timeout that was already queued does not close the review
		this.state.onReviewTimeout();
		assertEquals(State.DELIBERATION, this.state.getState());
	}

	/** While votes are missing there is no delay: the announcer starting the next clock closes the section. */
	@Test
	public void withJuryMissingVotesWaitForTheClockStart() {
		this.state.onRefereeDecision(null, lights(true));
		this.state.onJuryUpdate(votes(true, true, null), 3);
		assertEquals(0, this.renderer.scheduleCount);
		this.state.onStartTime();
		assertEquals(State.READY, this.state.getState());
	}

	/** Votes that arrive after the section closed are not taken for votes on the next lift. */
	@Test
	public void lateVotesDoNotStartTheDelayForTheNextLift() {
		this.state.onRefereeDecision(null, lights(true));
		this.state.onJuryUpdate(votes(true, true, true), 3);
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
		// a juror changes a vote on the previous lift after the section closed, then the next clock starts
		this.state.onJuryUpdate(votes(true, true, false), 3);
		this.state.onStartTime();
		int schedules = this.renderer.scheduleCount;
		this.state.onRefereeDecision(null, lights(false));
		assertEquals("the jury has not voted on this lift", schedules, this.renderer.scheduleCount);
	}

	/**
	 * Second scenario: three referees decide, three jurors vote, the jury deliberates and votes
	 * again; the verdict is shown and control passes to the announcer, so the section stays put
	 * until whatever the announcer does to restart (start lifting).
	 */
	@Test
	public void deliberationRevoteWaitsForAnnouncerRestart() {
		this.state.setShowJuryVotes(true);
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
	public void noLiftVerdictShowsTheRule() {
		this.state.onRefereeDecision(null, lights(true));
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false, "3 - rule");
		assertTrue(this.renderer.calls.contains("verdictBad"));
		assertEquals("3 - rule", this.renderer.rule);
		// a Good Lift never carries a rule
		this.state.onJuryVerdict(true, false, "3 - rule");
		assertNull(this.renderer.rule);
	}

	@Test
	public void preliminaryVerdictIsIgnoredInEveryState() {
		// the first notification (waiting for the announcer) is never painted, whatever the state
		this.renderer.clear();
		this.state.onJuryVerdict(true, true);
		assertEquals(State.READY, this.state.getState());
		this.state.onRefereeDecision(null, lights(true));
		this.renderer.clear();
		this.state.onJuryVerdict(true, true);
		assertEquals(State.DECISION, this.state.getState());
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		int schedules = this.renderer.scheduleCount;
		this.renderer.clear();
		this.state.onJuryVerdict(false, true, "3 - rule");
		assertTrue(this.renderer.calls.isEmpty());
		assertEquals(schedules, this.renderer.scheduleCount);
	}

	@Test
	public void secondVerdictReplacesTheLabelAndRestartsTheTimeout() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false, "3 - rule");
		assertEquals(1, this.renderer.scheduleCount);
		this.renderer.clear();
		this.state.onJuryVerdict(true, false);
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue("the label is replaced", this.renderer.calls.contains("verdictGood"));
		assertNull(this.renderer.rule);
		assertEquals("the 20 s start again", 2, this.renderer.scheduleCount);
	}

	@Test
	public void verdictAfterReturnToReadyIsShownForTheTimeout() {
		this.state.onDeliberationStart(JuryDeliberationEventType.START_DELIBERATION, null, lights(true));
		this.state.onJuryVerdict(false, false, "3 - rule");
		this.state.onStartTime();
		assertEquals(State.READY, this.state.getState());
		int schedules = this.renderer.scheduleCount;
		this.renderer.clear();
		this.state.onJuryVerdict(true, false, null, null, lights(false));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.contains("athlete"));
		assertTrue("the reviewed lights", this.renderer.calls.contains("refLights"));
		assertTrue("no jury circles", this.renderer.calls.contains("noJuryCircles"));
		assertFalse(this.renderer.calls.contains("juryCircles"));
		assertTrue(this.renderer.calls.contains("verdictGood"));
		assertEquals("the 20 s start", schedules + 1, this.renderer.scheduleCount);
		this.state.onJuryUpdate(votes(true, null, null), 3);
		assertFalse("late votes do not draw circles", this.renderer.calls.contains("juryCircles"));
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
	}

	@Test
	public void verdictDuringADecisionIsShownForTheTimeout() {
		this.state.onRefereeDecision(null, lights(true));
		int schedules = this.renderer.scheduleCount;
		this.renderer.clear();
		this.state.onJuryVerdict(false, false, "3 - rule", null, lights(true));
		assertEquals(State.DELIBERATION, this.state.getState());
		assertTrue(this.renderer.calls.contains("athlete"));
		assertTrue(this.renderer.calls.contains("refLights"));
		assertTrue(this.renderer.calls.contains("noJuryCircles"));
		assertTrue(this.renderer.calls.contains("verdictBad"));
		assertEquals("3 - rule", this.renderer.rule);
		assertEquals("the 20 s start", schedules + 1, this.renderer.scheduleCount);
		this.state.onReviewTimeout();
		assertEquals(State.READY, this.state.getState());
	}
}
