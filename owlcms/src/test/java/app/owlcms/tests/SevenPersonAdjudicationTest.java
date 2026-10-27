/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.components.elements.DecisionElementState;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;

/**
 * Seven-person Adjudication Panel (IWF TCRR 2026, 7.5.7): a one-athlete session driven by the same FOP events that
 * the referee and jury devices send. The FOP runs in testing mode, so its delays run at once and the scenarios are
 * deterministic; only the UI events (published asynchronously) are awaited.
 *
 * See compliance/SEVEN_PERSON_ADJUDICATION.md (design and remaining work).
 */
public class SevenPersonAdjudicationTest {

	private static final boolean W = true;
	private static final boolean R = false;

	private static Group groupA;

	private FieldOfPlay fop;
	private Recorder recorder;
	private Athlete athlete;

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Before
	public void setupSession() {
		TestData.insertInitialData(5, true);
		JPAService.runInTransaction((em) -> {
			groupA = GroupRepository.doFindByName("A", em);
			Group groupB = GroupRepository.doFindByName("B", em);
			Group groupC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 5, groupA, groupB, groupC);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, false);
		setSwitch(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, false);
		Competition.getCurrent().setVideoPlaybackTechnology(true);

		this.fop = MockFieldOfPlay.create(AthleteRepository.findAll(), new MockCountdownTimer(),
		        new MockCountdownTimer());
		OwlcmsSession.setFop(this.fop);
		this.fop.setPlatform(new Platform());
		this.fop.setJurySize(4);
		startSessionWithOneAthlete();
		this.recorder = new Recorder();
		this.fop.getUiEventBus().register(this.recorder);
	}

	@After
	public void restoreSettings() {
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, false);
		setSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT, false);
		Competition.getCurrent().setVideoPlaybackTechnology(false);
	}

	// ---------------------------------------------------------------- configuration

	@Test
	public void jurySizeFourWithVideoUsesThePanelProcedure() {
		assertEquals(4, this.fop.getJurySize());
		assertTrue(this.fop.isFourMemberJury());
		assertTrue(this.fop.isPanelProcedure());
		this.fop.setJurySize(3);
		assertFalse(this.fop.isFourMemberJury());
		assertFalse(this.fop.isPanelProcedure());
	}

	@Test
	public void jurySizeChangeIsAFopEventThatTheJuryPagesFollow() {
		Object page = new Object();
		post(new FOPEvent.JurySizeChange(page, 3));
		assertEquals(3, this.fop.getJurySize());
		assertFalse(this.fop.isPanelProcedure());
		assertTrue("the jury pages are told", this.recorder.await(this.recorder.jurySizeChanges,
		        e -> e.getOrigin() == page));
	}

	@Test
	public void competitionAndPlatformAcceptJurySizeFour() {
		Integer original = Competition.getCurrent().getJurySize();
		try {
			Competition.getCurrent().setJurySize(4);
			assertEquals(Integer.valueOf(4), Competition.getCurrent().getJurySize());
			Platform platform = new Platform();
			platform.setJurySize(4);
			assertEquals(Integer.valueOf(4), platform.getJurySize());
		} finally {
			Competition.getCurrent().setJurySize(original);
		}
	}

	@Test
	public void votesFromAFifthJuryStationAreIgnored() {
		post(new FOPEvent.JuryMemberDecisionUpdate(new Object(), 4, R));
		assertNull(this.fop.getJuryMemberDecision()[4]);
		post(new FOPEvent.JuryMemberDecisionUpdate(new Object(), 3, R));
		assertEquals(Boolean.FALSE, this.fop.getJuryMemberDecision()[3]);
	}

	// ---------------------------------------------------------------- phase A: the lift

	@Test
	public void panelAgreesWithTheReferees() {
		lift(W, W, W);
		assertTrue("jury votes are awaited", this.fop.isPanelVotePending());
		juryVotes(W, W, R, R); // 2-2 jury split: the referees' decision stands
		assertFalse(this.fop.isPanelVotePending());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void panelMajorityOppositeStopsTheCompetition() {
		lift(W, W, W);
		juryVotes(R, R, R, R);
		assertPanelReviewBreak();
		assertTrue("Adjudication Panel Review notification",
		        this.recorder.await(this.recorder.notifications, n -> n.isPanelReview()));
		assertTrue("the referees are called to the video review",
		        this.recorder.await(this.recorder.summons, s -> s.ref == 0));
		assertTrue("the referee call is announced as for the Referee call buttons",
		        this.recorder.await(this.recorder.notifications,
		                n -> n.getDeliberationEventType() == JuryDeliberationEventType.CALL_REFEREES));
		assertEquals("the jury votes that caused the review are kept", Boolean.FALSE,
		        this.fop.getJuryMemberDecision()[3]);
	}

	@Test
	public void decisionStaysVisibleUntilTheJuryHasVoted() {
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		lift(W, W, W);
		assertEquals("no time limit for the jury", FOPState.DECISION_VISIBLE, this.fop.getState());
		juryVotes(W, W, W, W);
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
	}

	@Test
	public void reviewStartsWhenTheHeldDecisionClears() {
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		lift(W, W, W);
		juryVotes(R, R, R, R);
		assertPanelReviewBreak();
	}

	@Test
	public void chairDeliberationEndsADecisionHeldForABrokenJuryBox() {
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		lift(W, W, R);
		// jury box 1 does not work
		juryVote(1, R);
		juryVote(2, R);
		juryVote(3, R);
		assertEquals("held for the missing jury vote", FOPState.DECISION_VISIBLE, this.fop.getState());
		post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertTrue("the Chair's deliberation starts", this.recorder.await(this.recorder.notifications,
		        n -> n.getDeliberationEventType() == JuryDeliberationEventType.START_DELIBERATION));
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertEquals(BreakType.JURY, this.fop.getBreakType());
		assertEquals("the votes on the lift are kept", Boolean.FALSE, this.fop.getJuryMemberDecision()[1]);

		// the vote never completes without jury box 1: the Chair gives the decision
		callForVotes();
		reviewVotes(R, R, R);
		juryVote(1, R);
		juryVote(2, R);
		juryVote(3, R);
		assertNull("no result: the vote is incomplete", this.fop.getPanelVoteMajority());
		post(new FOPEvent.JuryRuleRequest(new Object(), false));
		assertTrue(this.recorder.await(this.recorder.ruleRequests, r -> !r.isCancelled() && !r.isForVote()));
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 1, true));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.BAD_LIFT, verdict.getDeliberationEventType());
		assertTrue("not the result of a vote", verdict.isChairEntered());
		assertGoodLift(false);
	}

	@Test
	public void clockStartIsRefusedUntilTheSupervisorOverrides() {
		lift(W, W, W);
		juryVotes(W, W, W);
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertTrue("the deciding console is asked",
		        this.recorder.await(this.recorder.pending, p -> p.isPending()));

		post(new FOPEvent.TimeStarted(new Object(), true));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
		assertFalse(this.fop.isPanelVotePending());
		assertNull("incomplete jury votes are discarded", this.fop.getJuryMemberDecision()[0]);
		assertTrue("the consoles are told", this.recorder.await(this.recorder.pending, p -> !p.isPending()));
	}

	@Test
	public void clockStartOverrideClearsAHeldDecision() {
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		lift(W, W, W);
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.DECISION_VISIBLE, this.fop.getState());
		post(new FOPEvent.TimeStarted(new Object(), true));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
	}

	@Test
	public void threeMemberJuryWithVideoBlocksTheNextClockUntilAllHaveVoted() {
		this.fop.setJurySize(3);
		assertTrue(Competition.getCurrent().isVideoPlaybackTechnology());
		lift(W, W, R);
		juryVotes(W, W);
		assertTrue(this.fop.isJuryVotesAwaited());

		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals("the next clock is refused", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertTrue(this.recorder.await(this.recorder.pending, UIEvent.JuryVotesPending::isPending));

		juryVote(2, W);
		assertFalse(this.fop.isJuryVotesAwaited());
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
	}

	@Test
	public void fiveMemberJuryStartClockAnywayLeavesTheRefereesDecision() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		this.fop.setJurySize(5);
		lift(W, W, R);
		juryVotes(R, R, R, R);
		assertTrue(this.fop.isJuryVotesAwaited());

		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		post(new FOPEvent.TimeStarted(new Object(), true));

		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
		assertFalse(this.fop.isJuryVotesAwaited());
		assertNull("incomplete jury votes are discarded", this.fop.getJuryMemberDecision()[0]);
		assertGoodLift(true);
	}

	@Test
	public void fiveMemberJuryReleasesTheNextClockWhenTheLastJurorVotes() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		this.fop.setJurySize(5);
		lift(W, W, R);
		juryVotes(W, W, W, W);
		assertTrue(this.fop.isJuryVotesAwaited());

		juryVote(4, W);

		assertFalse(this.fop.isJuryVotesAwaited());
		assertEquals("a 5-member jury does not trigger an automatic review", FOPState.CURRENT_ATHLETE_DISPLAYED,
		        this.fop.getState());
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
	}

	@Test
	public void chairDecisionReleasesAnIncompleteThreeMemberJuryVote() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		this.fop.setJurySize(3);
		lift(W, W, R);
		juryVotes(R, R);
		assertTrue(this.fop.isJuryVotesAwaited());

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 1, true));

		assertFalse(this.fop.isJuryVotesAwaited());
		assertEquals(JuryDeliberationEventType.BAD_LIFT, awaitVerdict().getDeliberationEventType());
		assertGoodLift(false);
	}

	@Test
	public void threeMemberJuryDecisionStaysVisibleWhenJuryVotesAreShown() {
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		this.fop.setJurySize(3);
		lift(W, W, R);
		assertEquals("no time limit while the jury is voting", FOPState.DECISION_VISIBLE, this.fop.getState());

		juryVotes(W, W, W);

		assertFalse(this.fop.isJuryVotesAwaited());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
	}

	@Test
	public void juryResumeEndsANormalBreakLikeBreakManagement() {
		post(new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(FOPState.BREAK, this.fop.getState());

		post(new FOPEvent.JuryResume(new Object()));

		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertTrue("an ordinary break does not add a jury reset", this.recorder.displayResets.isEmpty());
	}

	@Test
	public void juryResumeDuringAnActiveLiftIsANoOp() {
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());

		post(new FOPEvent.JuryResume(new Object()));

		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
		assertTrue(this.recorder.displayResets.isEmpty());
	}

	@Test
	public void juryResumeAbandonsPendingJuryVotesAndClearsTheDisplays() {
		lift(W, W, W);
		assertTrue(this.fop.isJuryVotesAwaited());

		post(new FOPEvent.JuryResume(new Object()));

		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertFalse(this.fop.isJuryVotesAwaited());
		assertNull(this.fop.getJuryMemberDecision()[0]);
		assertTrue(this.recorder.await(this.recorder.displayResets, r -> true));
	}

	@Test
	public void withoutVideoPlaybackASplitJuryDoesNotStopTheCompetition() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(W, W, W);
		juryVotes(R, R, R, W);
		assertEquals("unanimity is needed without VPT", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertFalse(this.fop.isJuryReview());
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
	}

	@Test
	public void withoutVideoPlaybackTheHeldDecisionWaitsForTheJury() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		lift(W, W, W);
		assertEquals("no time limit for the four-member jury", FOPState.DECISION_VISIBLE, this.fop.getState());
		assertTrue(this.fop.isJuryVotesAwaited());
		juryVotes(W, W, W);
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals("the clock waits for the jury", FOPState.DECISION_VISIBLE, this.fop.getState());
		assertTrue("the deciding console is asked",
		        this.recorder.await(this.recorder.pending, p -> p.isPending()));

		juryVote(3, R);
		assertFalse(this.fop.isJuryVotesAwaited());
		assertEquals("no automatic review without VPT", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
	}

	@Test
	public void withoutVideoPlaybackTheOverrideClearsTheHeldDecision() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		lift(W, W, W);
		post(new FOPEvent.TimeStarted(new Object(), true));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
		assertFalse(this.fop.isJuryVotesAwaited());
	}

	@Test
	public void withoutVideoPlaybackTheClockWaitsWhenTheDecisionIsNotHeld() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(W, W, W);
		juryVotes(W, W, W);
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertTrue(this.recorder.await(this.recorder.pending, UIEvent.JuryVotesPending::isPending));

		post(new FOPEvent.TimeStarted(new Object(), true));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
		assertFalse(this.fop.isJuryVotesAwaited());
		assertGoodLift(true);
	}

	// ---------------------------------------------------------------- phase B: the review vote

	@Test
	public void reviewVoteConfirmsTheGoodLift() {
		reviewAfterLift(W, W, W);
		callForVotes();
		assertTrue(this.fop.isPanelVoteOpen());
		assertTrue("referee devices re-arm and buzz", this.recorder.await(this.recorder.voteCalls, c -> c.getRef() == 0));
		assertNull("jury votes are cleared for the review vote", this.fop.getJuryMemberDecision()[0]);

		reviewVotes(W, W, W);
		juryVotes(W, W, W);
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertTrue(this.recorder.await(this.recorder.updates, u -> u.getVoted()[5]));
		assertFalse("votes stay hidden until all seven are in",
		        this.recorder.updates.stream().anyMatch(UIEvent.PanelVoteUpdate::isComplete));

		juryVote(3, R);
		assertTrue("jury displays reveal when the seventh vote arrives",
		        this.recorder.await(this.recorder.updates,
		                u -> u.isComplete() && !u.isPublicReveal()));
		assertResultWaitsForTheChair();
		confirmGoodLift();
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertTrue("the decision is flagged as revealed when applied",
		        this.recorder.await(this.recorder.updates, UIEvent.PanelVoteUpdate::isPublicReveal));
		assertEquals(JuryDeliberationEventType.GOOD_LIFT, verdict.getDeliberationEventType());
		assertFalse("decided by the panel", verdict.isChairEntered());
		assertFalse("shown without waiting for the Speaker", verdict.isWaitForAnnouncer());
		assertEquals("the break ends by itself", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void clockStartIsRefusedWhileTheJuryDecisionCardIsShown() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(W, W, W);
		juryVotes(W, W, W, W);
		confirmGoodLift();
		awaitVerdict();
		// the test timers fire at once: the FOP has already resumed lifting, but the attempt boards still show
		// the jury decision card for 5 s
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		this.recorder.errors.clear();
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals("the clock does not start while the card is shown", FOPState.CURRENT_ATHLETE_DISPLAYED,
		        this.fop.getState());
		assertTrue("the timekeeper is told", this.recorder.await(this.recorder.errors, n -> true));

		// an explicit Resume ends the decision cycle
		post(new FOPEvent.JuryResume(new Object()));
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals("the clock starts after the Resume", FOPState.TIME_RUNNING, this.fop.getState());
	}

	@Test
	public void juryBoxResumeEndsAReviewWaitingForTheRule() {
		reviewAfterLift(W, W, R);
		callForVotes();
		reviewVotes(W, R, R);
		juryVotes(W, R, R, R);
		assertTrue("the No Lift result waits for the rule", this.fop.getPanelVoteUpdate(this).isComplete());

		// the jury box (MQTT) has no session: the StartLifting that follows Resume must still reach this platform
		OwlcmsSession.setFop(null);
		post(new FOPEvent.JuryResume(new Object()));
		assertEquals("the review ends", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertFalse("the review vote is closed", this.fop.isPanelVoteOpen());
		assertFalse("the result is cleared", this.fop.getPanelVoteUpdate(this).isComplete());
	}

	// ---------------------------------------------------------------- clock start in each phase of the review

	@Test
	public void clockStartIsRefusedDuringTheReviewBreakBeforeTheVote() {
		reviewAfterLift(W, W, W);
		assertClockStartRefused("Phase B, video review in progress", FOPState.BREAK);
		assertTrue("the vote can still be called", this.fop.isPanelVoteCallable());
	}

	@Test
	public void clockStartIsRefusedWhileTheReviewVoteIsOpen() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R);
		juryVotes(R, R);
		assertClockStartRefused("Phase B, vote open", FOPState.BREAK);
		assertTrue("the vote stays open", this.fop.isPanelVoteOpen());
		assertEquals("the votes received are kept", Boolean.FALSE, this.fop.getReviewRefereeDecision()[0]);
	}

	@Test
	public void clockStartIsRefusedWhileTheNoLiftRuleIsAwaited() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R, R);
		juryVotes(R, R, R, R);
		assertTrue(this.recorder.await(this.recorder.updates, u -> Boolean.FALSE.equals(u.getMajority())));
		assertClockStartRefused("Phase B, No Lift waiting for the rule", FOPState.BREAK);
		assertTrue("the vote is still open for the Chair's rule", this.fop.isPanelVoteOpen());
	}

	@Test
	public void clockStartIsRefusedWhileTheNoLiftCardIsShown() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R, R);
		juryVotes(R, R, R, R);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 5));
		awaitVerdict();
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertTrue(this.fop.isJuryDecisionCardShown());
		assertClockStartRefused("Phase C, No Lift card shown", FOPState.CURRENT_ATHLETE_DISPLAYED);

		post(new FOPEvent.JuryResume(new Object()));
		assertFalse("Resume ends the cycle", this.fop.isJuryDecisionCardShown());
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
	}

	@Test
	public void clockStartIsAcceptedOnceTheCardHasExpired() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(W, W, W);
		juryVotes(W, W, W, W);
		confirmGoodLift();
		awaitVerdict();
		assertTrue(this.fop.isJuryDecisionCardShown());
		this.fop.expireJuryDecisionCardForTests();
		this.recorder.errors.clear();
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals("the clock starts once the card is gone", FOPState.TIME_RUNNING, this.fop.getState());
		assertTrue("no refusal", this.recorder.errors.isEmpty());
	}

	/** a clock start is refused in the current state, the timekeeper is told, and the state does not change */
	private void assertClockStartRefused(String phase, FOPState expectedState) {
		this.recorder.errors.clear();
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(phase + ": the clock does not start", expectedState, this.fop.getState());
		assertTrue(phase + ": the timekeeper is told", this.recorder.await(this.recorder.errors, n -> true));
	}

	@Test
	public void resumeAfterASummonDoesNotLeaveTheJuryVotesAwaited() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(W, W, W);
		juryVotes(W, W, W, W);
		confirmGoodLift();
		awaitVerdict();
		assertEquals("the break ends by itself", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertFalse("the review is over", this.fop.isPanelReview());
		// the attempt boards show the jury decision card for 5 s; the Chair's Resume ends the cycle
		post(new FOPEvent.JuryResume(new Object()));

		// next lift, the jury does not vote; the Chair summons the referees, then resumes
		lift(W, W, W);
		post(new FOPEvent.SummonReferee(new Object(), 0));
		assertEquals(BreakType.JURY, this.fop.getBreakType());
		assertFalse("a summon is not a panel review", this.fop.isPanelReview());
		post(new FOPEvent.StartLifting(new Object()));
		assertFalse(this.fop.isJuryVotesAwaited());
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals("the clock starts after the resume", FOPState.TIME_RUNNING, this.fop.getState());
	}

	@Test
	public void reviewVoteReversesToNoLiftWithTheChairsRule() {
		reviewAfterLift(W, W, R);
		// the Referee call button of the jury box requests the new decision
		post(new FOPEvent.SummonReferee(new Object(), 0));
		assertTrue(this.fop.isPanelVoteOpen());
		reviewVotes(R, R, R);
		juryVotes(R, R, R, R);
		assertEquals("a No Lift waits for the technical rule", FOPState.BREAK, this.fop.getState());
		assertTrue("jury displays reveal when the last vote arrives",
		        this.recorder.await(this.recorder.updates,
		                u -> u.isComplete() && !u.isPublicReveal()));
		assertFalse("the decision is not flagged as revealed before the rule is given",
		        this.recorder.updates.stream().anyMatch(UIEvent.PanelVoteUpdate::isPublicReveal));

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 3));
		assertTrue("the decision is flagged as revealed with the rule",
		        this.recorder.await(this.recorder.updates, UIEvent.PanelVoteUpdate::isPublicReveal));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.BAD_LIFT, verdict.getDeliberationEventType());
		assertEquals(Integer.valueOf(3), verdict.getReasonCode());
		assertFalse(verdict.isChairEntered());
		assertFalse(verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(false);
	}

	@Test
	public void noLiftButtonAfterANoLiftVoteIsTheVotesDecision() {
		reviewAfterLift(W, W, R);
		callForVotes();
		reviewVotes(R, R, R);
		juryVotes(R, R, R, R);
		assertEquals("a No Lift waits for the technical rule", FOPState.BREAK, this.fop.getState());
		int resetsBefore = this.recorder.displayResets.size();

		// the Chair closed the rule dialog, then used the No Lift button to give the rule
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 3, true));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertFalse("the decision of the vote is not marked as entered by the Chair", verdict.isChairEntered());
		assertEquals("the votes are not cleared", resetsBefore, this.recorder.displayResets.size());
		assertTrue("the vote is revealed with the decision",
		        this.recorder.await(this.recorder.updates, UIEvent.PanelVoteUpdate::isPublicReveal));
		assertGoodLift(false);
	}

	@Test
	public void goodLiftButtonAgainstANoLiftVoteIsEnteredByTheChair() {
		reviewAfterLift(W, W, R);
		callForVotes();
		reviewVotes(R, R, R);
		juryVotes(R, R, R, R);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), W, true, null, true));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertTrue("Jury Chair Entered Decision", verdict.isChairEntered());
		assertFalse("the votes are cleared", this.recorder.displayResets.isEmpty());
		assertGoodLift(true);
	}

	@Test
	public void chairOverrideIsMarkedAndAppliedWithoutTheSpeaker() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R);
		Athlete underReview = this.fop.getAthleteUnderReview();
		// the Chair uses the No Lift button while the review vote is in progress
		post(new FOPEvent.JuryDecision(underReview, new Object(), R, true, 2, true));
		assertFalse("the vote is closed", this.fop.isPanelVoteOpen());
		assertTrue("the review vote is abandoned", this.recorder.updates.stream()
		        .anyMatch(u -> !u.isOpen() && !u.isComplete()));
		assertFalse("the jury displays are cleared", this.recorder.displayResets.isEmpty());
		for (Boolean vote : this.fop.getJuryMemberDecision()) {
			assertNull("the jury votes are cleared", vote);
		}
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertTrue("Jury Chair Entered Decision", verdict.isChairEntered());
		assertFalse("the Speaker is only informed", verdict.isWaitForAnnouncer());
		assertEquals(Integer.valueOf(2), verdict.getReasonCode());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(false);
	}

	@Test
	public void recallBuzzesOneRefereeWithoutClearingVotes() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R);
		post(new FOPEvent.SummonReferee(new Object(), 2));
		assertTrue(this.recorder.await(this.recorder.voteCalls, c -> c.getRef() == 2));
		assertEquals(Boolean.FALSE, this.fop.getReviewRefereeDecision()[0]);
		assertTrue(this.fop.isPanelVoteOpen());
	}

	@Test
	public void cancelledVoteKeepsTheBreak() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R);
		juryVotes(R);
		post(new FOPEvent.PanelVoteCancelled(new Object()));
		assertFalse(this.fop.isPanelVoteOpen());
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertNull(this.fop.getReviewRefereeDecision()[0]);
		assertNull(this.fop.getJuryMemberDecision()[0]);
		callForVotes();
		assertTrue("the vote can be called again", this.fop.isPanelVoteOpen());
	}

	@Test
	public void completedVoteIsLocked() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(W, W, W);
		juryVotes(W, W, W, W);
		assertResultWaitsForTheChair();
		juryVote(0, R);
		reviewVotes(R);
		assertEquals("a changed jury vote is ignored", Boolean.TRUE, this.fop.getJuryMemberDecision()[0]);
		assertEquals("a changed referee vote is ignored", Boolean.TRUE, this.fop.getReviewRefereeDecision()[0]);
		assertEquals(Boolean.TRUE, this.fop.getPanelVoteMajority());
	}

	@Test
	public void cancelVoteResultClearsEverythingAndAllowsANewVote() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R, R);
		juryVotes(R, R, R, R);
		assertTrue(this.recorder.await(this.recorder.ruleRequests, r -> r.isForVote() && !r.isCancelled()));
		int resetsBefore = this.recorder.displayResets.size();

		// the jury says a vote was entered by mistake
		post(new FOPEvent.PanelVoteCancelled(new Object()));
		assertFalse(this.fop.isPanelVoteOpen());
		assertNull(this.fop.getPanelVoteMajority());
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertTrue("the rule dialog is withdrawn", this.recorder.await(this.recorder.ruleRequests,
		        UIEvent.JuryRuleRequired::isCancelled));
		assertTrue("the displays and devices are cleared",
		        this.recorder.await(this.recorder.displayResets, r -> this.recorder.displayResets.size() > resetsBefore));
		assertNoDecision();

		// the new vote gives the other result
		callForVotes();
		reviewVotes(W, W, W);
		juryVotes(W, W, W, W);
		assertResultWaitsForTheChair();
		confirmGoodLift();
		assertFalse(awaitVerdict().isChairEntered());
		assertGoodLift(true);
	}

	@Test
	public void summonAllMeansSummonInAReviewStartedByTheChairWithoutVideo() {
		fourMemberDeliberationWithoutVideo(W, W, W);
		assertFalse(this.fop.isSummonAllVote());
		juryVotes(R, R);
		post(new FOPEvent.SummonReferee(new Object(), 0));
		assertTrue("the referees are summoned", this.recorder.await(this.recorder.summons, r -> r.ref == 0));
		assertEquals("the votes are not cleared", Boolean.FALSE, this.fop.getJuryMemberDecision()[0]);
	}

	@Test
	public void summonAllMeansVoteAfterAnAutomaticStopWithoutVideo() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		setSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT, true);
		lift(W, W, W);
		juryVotes(R, R, R, R);
		assertTrue(this.fop.isJuryReview());
		assertTrue(this.fop.isSummonAllVote());
		post(new FOPEvent.SummonReferee(new Object(), 0));
		assertTrue("the vote is called", this.fop.isPanelVoteOpen());
		assertNull("the jury votes are cleared", this.fop.getJuryMemberDecision()[0]);
	}

	@Test
	public void endingTheBreakDiscardsAnUnfinishedVote() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R, R);
		post(new FOPEvent.StartLifting(new Object()));
		assertFalse(this.fop.isPanelVoteOpen());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void refereeVotesInABreakAreRefusedWithoutAReviewVote() {
		reviewAfterLift(W, W, W);
		reviewVotes(R);
		assertNull(this.fop.getReviewRefereeDecision()[0]);
		assertEquals(FOPState.BREAK, this.fop.getState());
	}

	@Test
	public void challengeCallsTheRefereesAndAllowsTheVote() {
		lift(W, W, W);
		juryVotes(W, W, W, W);
		post(new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(BreakType.CHALLENGE, this.fop.getBreakType());
		assertTrue(this.recorder.await(this.recorder.summons, s -> s.ref == 0));
		assertTrue(this.fop.isPanelVoteCallable());
		callForVotes();
		assertTrue(this.fop.isPanelVoteOpen());
	}

	@Test
	public void challengeWithVideoIsTheAdjudicationPanelReview() {
		lift(W, W, W);
		juryVotes(W, W, W, W);
		assertFalse("the panel agrees with the referees", this.fop.isPanelReview());
		post(new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(BreakType.CHALLENGE, this.fop.getBreakType());
		assertTrue("labelled Challenge", this.recorder.await(this.recorder.notifications,
		        n -> n.getDeliberationEventType() == JuryDeliberationEventType.CHALLENGE));
		assertTrue("referees called to the video review",
		        this.recorder.await(this.recorder.summons, s -> s.ref == 0));
		assertEquals("the lift's jury votes are kept until the vote is called", Boolean.TRUE,
		        this.fop.getJuryMemberDecision()[0]);
		assertNotNull("the lift's votes are shown", this.fop.getPanelReviewInitialVotes());

		callForVotes();
		assertNull("jury votes cleared", this.fop.getJuryMemberDecision()[0]);
		reviewVotes(R, R, W);
		juryVotes(R, R, W, W);
		assertEquals("4 red, 3 white: the 7-person majority reverses, the rule is awaited", FOPState.BREAK,
		        this.fop.getState());
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 5));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.BAD_LIFT, verdict.getDeliberationEventType());
		assertFalse("the result of the vote", verdict.isChairEntered());
		assertGoodLift(false);
	}

	@Test
	public void challengeWithoutVideoIsAJuryDeliberation() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(W, W, W);
		juryVotes(R, R, R, W);
		post(new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(BreakType.CHALLENGE, this.fop.getBreakType());
		assertTrue("labelled Challenge", this.recorder.await(this.recorder.notifications,
		        n -> n.getDeliberationEventType() == JuryDeliberationEventType.CHALLENGE));
		assertFalse("summoning the referees is the Chair's decision",
		        this.recorder.summons.stream().anyMatch(s -> s.ref == 0));

		callForVotes();
		reviewVotes(R);
		assertNull("the referees do not vote again", this.fop.getReviewRefereeDecision()[0]);
		juryVotes(R, R, R, W);
		// not unanimous: the referees' decision stands, once the Chair confirms it
		assertResultWaitsForTheChair();
		confirmGoodLift();
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.GOOD_LIFT, verdict.getDeliberationEventType());
		assertFalse(verdict.isChairEntered());
		assertGoodLift(true);
	}

	@Test
	public void boardsShowTheSevenVotesTogether() {
		PanelRenderer boards = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(boards);
		boardState.setShowJuryDecisions(true);
		boardState.setFop(this.fop);
		this.fop.getUiEventBus().register(boardState);

		reviewAfterLift(W, W, W);
		callForVotes();
		assertTrue("cells cleared when the vote is called", boards.await(
		        p -> p.referees().equals(List.of("empty", "empty", "empty"))
		                && p.jury().equals(List.of("empty", "empty", "empty", "empty"))));
		reviewVotes(R, R, R);
		juryVotes(R, R, R);
		assertTrue("who has voted, values hidden", boards.await(
		        p -> p.referees().equals(List.of("voted", "voted", "voted"))
		                && p.jury().equals(List.of("voted", "voted", "voted", "empty"))));
		assertFalse(boards.panels.stream().anyMatch(p -> p.referees().contains("red")));

		juryVote(3, W);
		assertTrue("the result is known", this.recorder.await(this.recorder.updates,
		        u -> Boolean.FALSE.equals(u.getMajority())));
		assertTrue("the boards reveal the seven votes as soon as the last one is in, before the rule",
		        boards.await(p -> p.referees().equals(List.of("red", "red", "red"))
		                && p.jury().equals(List.of("red", "red", "red", "white"))));

		int shown = boards.panels.size();
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 17));
		awaitVerdict();
		settledEvents();
		assertTrue("the votes stay revealed under the decision", boards.panels.subList(shown, boards.panels.size())
		        .stream().allMatch(p -> p.referees().equals(List.of("red", "red", "red"))));
	}

	@Test
	public void boardsClearTheVotesWhenTheChairEntersTheDecision() {
		PanelRenderer boards = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(boards);
		boardState.setShowJuryDecisions(true);
		boardState.setFop(this.fop);
		this.fop.getUiEventBus().register(boardState);

		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R, R, R);
		juryVotes(W, W);
		assertTrue("who has voted", boards.await(
		        p -> p.referees().equals(List.of("voted", "voted", "voted"))
		                && p.jury().equals(List.of("voted", "voted", "empty", "empty"))));
		int shownBefore = boards.panels.size();
		int resetsBefore = boards.resets.get();

		// the Chair uses the No Lift button without waiting for the end of the vote
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 2, true));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertTrue(verdict.isChairEntered());
		assertTrue("the board is cleared", boards.resets.get() > resetsBefore);
		assertFalse("no vote is revealed",
		        boards.panels.subList(shownBefore, boards.panels.size()).stream()
		                .anyMatch(p -> p.referees().contains("red") || p.referees().contains("white")));
	}

	@Test
	public void boardsShowTheVotesThatLedToTheReview() {
		PanelRenderer boards = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(boards);
		boardState.setShowJuryDecisions(true);
		boardState.setFop(this.fop);
		this.fop.getUiEventBus().register(boardState);

		reviewAfterLift(W, R, W);
		assertTrue("the lift votes are shown during the review", boards.await(
		        p -> p.referees().equals(List.of("white", "red", "white"))
		                && p.jury().equals(List.of("red", "red", "red", "red"))));
		assertNotNull(this.fop.getPanelReviewInitialVotes());

		callForVotes();
		assertNull("the lift votes are no longer shown once the vote is called",
		        this.fop.getPanelReviewInitialVotes());
		assertTrue("cells cleared when the vote is called", boards.await(
		        p -> p.referees().equals(List.of("empty", "empty", "empty"))
		                && p.jury().equals(List.of("empty", "empty", "empty", "empty"))));
	}

	@Test
	public void challengeShowsTheVotesOfTheLift() {
		PanelRenderer boards = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(boards);
		boardState.setShowJuryDecisions(true);
		boardState.setFop(this.fop);
		this.fop.getUiEventBus().register(boardState);

		lift(W, W, R);
		juryVotes(W, W, W, R);
		post(new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertTrue("the lift votes are shown during the challenge", boards.await(
		        p -> p.referees().equals(List.of("white", "white", "red"))
		                && p.jury().equals(List.of("white", "white", "white", "red"))));

		post(new FOPEvent.StartLifting(new Object()));
		assertNull("nothing is shown after the break", this.fop.getPanelReviewInitialVotes());
	}

	// ---------------------------------------------------------------- referee input outside the review vote

	@Test
	public void refereeInputDuringAnOrdinaryBreakIsRefusedSilently() throws InterruptedException {
		post(new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(FOPState.BREAK, this.fop.getState());
		post(new FOPEvent.DecisionUpdate(new Object(), 0, W));
		post(new FOPEvent.DownSignal(new Object()));
		assertNull(this.fop.getRefereeDecision()[0]);
		Thread.sleep(300);
		assertFalse(this.recorder.errors.stream().anyMatch(n -> "DecisionUpdate".equals(n.getFopEventString())
		        || "DownSignal".equals(n.getFopEventString())));
	}

	@Test
	public void refereeInputDuringAJuryBreakBeforeTheVoteIsSignaled() {
		reviewAfterLift(W, W, W);
		assertFalse(this.fop.isPanelVoteOpen());
		post(new FOPEvent.DecisionUpdate(new Object(), 0, R));
		assertTrue(this.recorder.await(this.recorder.errors, n -> "DecisionUpdate".equals(n.getFopEventString())));
	}

	// ---------------------------------------------------------------- traditional 3 or 5 member jury

	@Test
	public void threeOrFiveMemberJuryVotesDoNotDecide() {
		juryDeliberation(3, W, W, W);
		juryVotes(R, R, R);
		assertResultWaitsForTheChair();
		assertTrue("the No Lift waits for the Chair's rule", this.recorder.await(this.recorder.ruleRequests,
		        r -> r.isForVote() && !r.isCancelled()));
	}

	@Test
	public void fiveMemberJuryVotesDoNotDecide() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		juryDeliberation(5, W, W, R);
		juryVotes(R, R, R, R, R);
		assertNoDecision();
	}

	@Test
	public void chairNoLiftAsksForTheRuleThenApplies() {
		juryDeliberation(3, W, W, W);
		// the jury vote is optional: No Lift on Jury Control or on the jury box, the same request
		post(new FOPEvent.JuryRuleRequest(new Object(), false));
		assertTrue("the Jury Control pages are asked for the rule", this.recorder.await(this.recorder.ruleRequests,
		        r -> !r.isCancelled() && !r.isForVote()));
		assertTrue(this.fop.isJuryRuleRequested());
		assertNoDecision();

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 1, true));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.BAD_LIFT, verdict.getDeliberationEventType());
		assertEquals(Integer.valueOf(1), verdict.getReasonCode());
		assertFalse("not marked without the seven-person panel", verdict.isChairEntered());
		assertFalse(verdict.isWaitForAnnouncer());
		assertFalse(this.fop.isJuryRuleRequested());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(false);
	}

	@Test
	public void cancelledRuleRequestIsWithdrawnEverywhere() {
		juryDeliberation(3, W, W, W);
		post(new FOPEvent.JuryRuleRequest(new Object(), false));
		post(new FOPEvent.JuryRuleRequest(new Object(), true));
		assertTrue(this.recorder.await(this.recorder.ruleRequests, UIEvent.JuryRuleRequired::isCancelled));
		assertFalse(this.fop.isJuryRuleRequested());
		assertEquals(FOPState.BREAK, this.fop.getState());
	}

	@Test
	public void speakerEnteredDecisionIsFlagged() {
		juryDeliberation(3, W, W, W);
		// relayed by the Technical Controller and entered in the Speaker's dialog
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, false));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertTrue(verdict.isSpeakerEntered());
		assertFalse(verdict.isChairEntered());
		assertGoodLift(false);
	}

	@Test
	public void threeMemberJuryWithVideoVotesAsWithout() {
		// VPT in use: a 3-member jury has no automatic trigger, the Chair starts the review as without VPT
		assertTrue(Competition.getCurrent().isVideoPlaybackTechnology());
		juryDeliberation(3, W, W, W);
		assertNull("the votes on the lift are not kept", this.fop.getPanelReviewInitialVotes());
		assertTrue("the jury vote is open at once", this.fop.isPanelVoteOpen());
		assertNull("the jury votes are cleared", this.fop.getJuryMemberDecision()[0]);
		assertEquals("the referees' decision stays", Boolean.TRUE, this.fop.getRefereeDecision()[0]);
		assertFalse("Summon All is not a vote", this.fop.isSummonAllVote());
		post(new FOPEvent.SummonReferee(new Object(), 0));
		assertTrue("Summon All summons the referees", this.recorder.await(this.recorder.summons, r -> r.ref == 0));
		reviewVotes(R);
		assertNull("the referees do not vote again", this.fop.getReviewRefereeDecision()[0]);
		juryVotes(R, R, R);
		assertTrue(this.recorder.await(this.recorder.updates,
		        u -> u.isComplete() && u.getVotes() != null && u.getVotes().length == 6));
		assertTrue("unanimous: the No Lift waits for the rule", this.recorder.await(this.recorder.ruleRequests,
		        r -> r.isForVote() && !r.isCancelled()));
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 4, true));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(Integer.valueOf(4), verdict.getReasonCode());
		assertFalse("the result of the vote", verdict.isChairEntered());
		assertGoodLift(false);
	}

	@Test
	public void summonCueEndsOnceTheJuryHasDecided() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		this.fop.setJurySize(3);
		lift(W, W, R);
		juryVotes(R, R, R);
		assertTrue("unanimous jury against the referees: Jury Control prompts the Chair",
		        this.fop.isJuryVoteAgainstReferees());
		post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		juryVotes(R, R, R);
		// the Speaker enters the decision relayed by the Technical Controller
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, false));
		awaitVerdict();
		assertFalse("the jury has decided: no more prompt", this.fop.isJuryVoteAgainstReferees());
	}

	@Test
	public void threeMemberJurySplitVoteLeavesTheRefereesDecision() {
		juryDeliberation(3, W, W, W);
		juryVotes(R, R, W);
		assertResultWaitsForTheChair();
		assertEquals("not unanimous: the referees' decision", Boolean.TRUE, this.fop.getPanelVoteMajority());
		confirmGoodLift();
		assertEquals(JuryDeliberationEventType.GOOD_LIFT, awaitVerdict().getDeliberationEventType());
	}

	@Test
	public void fiveMemberJuryWithVideoReversesByMajority() {
		juryDeliberation(5, W, W, R);
		juryVotes(R, R, R, W, W);
		assertResultWaitsForTheChair();
		assertEquals(Boolean.FALSE, this.fop.getPanelVoteMajority());
	}

	@Test
	public void fiveMemberJuryWithoutVideoNeedsUnanimity() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		juryDeliberation(5, W, W, R);
		assertTrue("without VPT the jurors may vote at once", this.fop.isPanelVoteOpen());
		assertFalse(this.fop.isSummonAllVote());
		juryVotes(R, R, R, R, W);
		assertResultWaitsForTheChair();
		assertEquals("not unanimous: the referees' decision", Boolean.TRUE, this.fop.getPanelVoteMajority());
	}

	@Test
	public void threeMemberJuryWithoutVideoSummonAllSummons() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		juryDeliberation(3, W, W, W);
		juryVotes(R);
		post(new FOPEvent.SummonReferee(new Object(), 0));
		assertTrue("the referees are summoned", this.recorder.await(this.recorder.summons, r -> r.ref == 0));
		assertEquals("the vote goes on", Boolean.FALSE, this.fop.getJuryMemberDecision()[0]);
	}

	@Test
	public void publicBoardsWithHiddenJuryVotesShowNothingOfTheReviewVote() {
		PanelRenderer boards = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(boards);
		boardState.setFop(this.fop);
		this.fop.getUiEventBus().register(boardState);
		reviewAfterLift(W, W, W);
		callForVotes();
		assertTrue("the board is cleared when the vote is called", awaitCondition(() -> boards.resets.get() > 0));
		reviewVotes(R, R, R);
		juryVotes(R, R, R, R);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 2));
		awaitVerdict();
		settledEvents();
		assertTrue("no jury circles, no review vote", boards.panels.stream()
		        .noneMatch(p -> !p.jury().isEmpty() || p.referees().contains("voted")
		                || p.referees().contains("red")));
	}

	@Test
	public void withoutVideoPlaybackAUnanimousJuryStopsAndRevotes() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		setSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT, true);
		lift(W, W, W);
		juryVotes(R, R, R, R);
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertEquals(BreakType.JURY, this.fop.getBreakType());
		assertTrue("automatic jury deliberation", this.fop.isJuryReview());
		assertFalse("not an Adjudication Panel Review", this.fop.isPanelReview());
		assertFalse("summoning the referees is the Chair's decision (7.5.8)",
		        this.recorder.summons.stream().anyMatch(s -> s.ref == 0));
		assertEquals("the jury votes are kept until the vote is called", Boolean.FALSE,
		        this.fop.getJuryMemberDecision()[0]);

		callForVotes();
		assertTrue(this.fop.isPanelVoteOpen());
		assertNull("jury votes cleared", this.fop.getJuryMemberDecision()[0]);
		reviewVotes(R);
		assertNull("the referees do not vote again", this.fop.getReviewRefereeDecision()[0]);
		juryVotes(R, R, R, R);
		assertEquals("a No Lift waits for the technical rule", FOPState.BREAK, this.fop.getState());

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 4));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.BAD_LIFT, verdict.getDeliberationEventType());
		assertEquals(Integer.valueOf(4), verdict.getReasonCode());
		assertFalse("the result of the vote", verdict.isChairEntered());
		assertFalse("the Speaker is only informed", verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(false);
	}

	@Test
	public void withoutVideoPlaybackAUnanimousJuryDoesNotStopByDefault() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(W, W, W);
		juryVotes(R, R, R, R);
		assertEquals("TCRR 7.5.8: the jury deliberates by hand", FOPState.CURRENT_ATHLETE_DISPLAYED,
		        this.fop.getState());
		assertFalse(this.fop.isJuryReview());
		assertFalse("the jury votes are no longer awaited", this.fop.isJuryVotesAwaited());

		// the Chair starts the deliberation and calls the vote by hand
		post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertTrue(this.fop.isPanelVoteCallable());
		callForVotes();
		juryVotes(R, R, R, R);
		assertEquals("a No Lift waits for the technical rule", FOPState.BREAK, this.fop.getState());
	}

	@Test
	public void withoutVideoPlaybackASplitSecondVoteLeavesTheRefereesDecision() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		setSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT, true);
		lift(W, W, W);
		juryVotes(R, R, R, R);
		callForVotes();
		juryVotes(R, R, R, W);
		assertResultWaitsForTheChair();
		confirmGoodLift();
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals("decision stands", JuryDeliberationEventType.GOOD_LIFT, verdict.getDeliberationEventType());
		assertFalse(verdict.isChairEntered());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void withoutVideoPlaybackAUnanimousSecondVoteReversesToGoodLift() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		setSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT, true);
		lift(R, R, R);
		juryVotes(W, W, W, W);
		assertTrue(this.fop.isJuryReview());
		callForVotes();
		juryVotes(W, W, W, W);
		confirmGoodLift();
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.GOOD_LIFT, verdict.getDeliberationEventType());
		assertFalse(verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void fourMemberChairDecisionWithoutVideoIsAppliedWithoutTheSpeaker() {
		fourMemberDeliberationWithoutVideo(W, W, W);
		assertTrue("the jurors may vote at once", this.fop.isPanelVoteOpen());
		juryVotes(R, R);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 2, true));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertFalse("without VPT the Chair's decision is the normal path, not marked", verdict.isChairEntered());
		assertFalse("the Speaker is only informed", verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(false);
	}

	@Test
	public void threeMemberChairDecisionIsAppliedWithoutTheSpeaker() {
		juryDeliberation(3, W, W, W);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 1));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertFalse("the Speaker announces after the decision is shown", verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
	}

	/** four-member jury without VPT: a lift followed by a manual jury deliberation */
	private void fourMemberDeliberationWithoutVideo(boolean... refereeVotes) {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		assertEquals(4, this.fop.getJurySize());
		lift(refereeVotes);
		post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(FOPState.BREAK, this.fop.getState());
	}

	/** seven-person procedure off, a lift followed by a manual jury deliberation */
	private void juryDeliberation(int jurySize, boolean... refereeVotes) {
		this.fop.setJurySize(jurySize);
		assertFalse(this.fop.isPanelProcedure());
		lift(refereeVotes);
		post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(FOPState.BREAK, this.fop.getState());
	}

	private void assertNoDecision() {
		assertEquals("the jury votes alone do not decide", FOPState.BREAK, this.fop.getState());
		assertFalse("no decision", this.recorder.notifications.stream()
		        .anyMatch(n -> n.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT
		                || n.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT));
	}

	/** the review vote is complete: its result is locked and waits for the Jury Chair */
	private void assertResultWaitsForTheChair() {
		assertTrue(this.recorder.await(this.recorder.updates, u -> u.isOpen() && u.isComplete()));
		assertNoDecision();
		assertTrue("the result stays open for the Chair", this.fop.isPanelVoteOpen());
	}

	/** the Jury Chair's Confirm Good Lift on the result screen (or Good Lift on the jury box) */
	private void confirmGoodLift() {
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), W, true, null, true));
	}

	// ---------------------------------------------------------------- full review, UI events in order

	/**
	 * One athlete, one lift judged good by the referees, four red jury votes, automatic Adjudication Panel Review,
	 * Call for Votes, seven votes, decision, automatic resume, Resume. Checks every UI event the displays, consoles
	 * and devices rely on, in order.
	 */
	@Test
	public void fullReviewEndingInNoLiftSendsTheExpectedUiEvents() {
		fullReview(new boolean[] { R, R, W }, new boolean[] { R, R, R, W }, false);
	}

	@Test
	public void fullReviewEndingInGoodLiftSendsTheExpectedUiEvents() {
		fullReview(new boolean[] { W, W, W }, new boolean[] { W, W, W, R }, true);
	}

	private void fullReview(boolean[] refereeReview, boolean[] juryReview, boolean good) {
		PanelRenderer board = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(board);
		boardState.setShowJuryDecisions(true);
		boardState.setFop(this.fop);
		this.fop.getUiEventBus().register(boardState);
		Boolean[] expectedVotes = new Boolean[7];
		for (int i = 0; i < 3; i++) {
			expectedVotes[i] = refereeReview[i];
		}
		for (int i = 0; i < 4; i++) {
			expectedVotes[3 + i] = juryReview[i];
		}
		List<String> expectedRefs = cells(refereeReview);
		List<String> expectedJury = cells(juryReview);

		// phase A: the lift, the jury opposes the referees
		lift(W, W, W);
		juryVotes(R, R, R, R);
		assertPanelReviewBreak();
		// phase B: the review vote
		callForVotes();
		assertTrue(board.await(p -> p.referees().equals(List.of("empty", "empty", "empty"))));
		reviewVotes(refereeReview);
		juryVotes(juryReview);
		assertResultWaitsForTheChair();
		assertTrue("the attempt board reveals the votes as soon as the last one is in, before the decision",
		        board.await(p -> p.referees().equals(expectedRefs) && p.jury().equals(expectedJury)));
		assertFalse("the decision is not flagged as applied before the Chair",
		        this.recorder.updates.stream().anyMatch(UIEvent.PanelVoteUpdate::isPublicReveal));
		if (!good) {
			assertTrue("the vote asks the Jury Control pages for the rule",
			        this.recorder.await(this.recorder.ruleRequests, r -> r.isForVote() && !r.isCancelled()));
			// the Chair gives the rule in the dialog opened by the vote
			post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 3));
		} else {
			confirmGoodLift();
		}
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertGoodLift(good);
		assertEquals("lifting resumes automatically", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		this.fop.expireJuryDecisionCardForTests();
		int boardResets = board.resets.get();
		post(new FOPEvent.JuryResume(new Object()));
		assertTrue("Resume clears the jury displays", this.recorder.await(this.recorder.displayResets, r -> true));
		assertTrue("Resume clears the attempt board", awaitCondition(() -> board.resets.get() > boardResets));
		for (Boolean vote : this.fop.getJuryMemberDecision()) {
			assertNull("Resume clears the jury votes", vote);
		}

		List<UIEvent> events = settledEvents();
		String trace = trace(events);
		assertInOrder(events, trace,
		        // phase A
		        step("referee decision WWW", UIEvent.Decision.class,
		                d -> Boolean.TRUE.equals(d.decision) && Boolean.TRUE.equals(d.ref1)
		                        && Boolean.TRUE.equals(d.ref2) && Boolean.TRUE.equals(d.ref3)),
		        step("jury vote 1", UIEvent.JuryUpdate.class, j -> juryIs(j, R, null, null, null)),
		        step("jury vote 2", UIEvent.JuryUpdate.class, j -> juryIs(j, R, R, null, null)),
		        step("jury vote 3", UIEvent.JuryUpdate.class, j -> juryIs(j, R, R, R, null)),
		        step("jury vote 4", UIEvent.JuryUpdate.class, j -> juryIs(j, R, R, R, R)),
		        step("automatic review break", UIEvent.JuryNotification.class,
		                n -> n.getDeliberationEventType() == JuryDeliberationEventType.START_DELIBERATION),
		        // phase B
		        step("Call for Votes notification", UIEvent.JuryNotification.class,
		                n -> n.getDeliberationEventType() == JuryDeliberationEventType.CALL_REFEREES),
		        step("referees summoned", UIEvent.SummonRef.class, r -> r.ref == 0),
		        step("vote called", UIEvent.PanelVoteCalled.class, c -> c.getRef() == 0),
		        step("seven empty cells", UIEvent.PanelVoteUpdate.class,
		                u -> u.isOpen() && votedCount(u) == 0 && u.getVotes() == null),
		        step("referee 1 voted", UIEvent.PanelVoteUpdate.class, u -> refereeVoted(u, 0, 1)),
		        step("referee 2 voted", UIEvent.PanelVoteUpdate.class, u -> refereeVoted(u, 1, 2)),
		        step("referee 3 voted", UIEvent.PanelVoteUpdate.class, u -> refereeVoted(u, 2, 3)),
		        step("juror 1 vote", UIEvent.JuryUpdate.class, j -> juryIs(j, juryReview[0], null, null, null)),
		        step("juror 1 voted", UIEvent.PanelVoteUpdate.class, u -> hidden(u, 4)),
		        step("juror 2 vote", UIEvent.JuryUpdate.class,
		                j -> juryIs(j, juryReview[0], juryReview[1], null, null)),
		        step("juror 2 voted", UIEvent.PanelVoteUpdate.class, u -> hidden(u, 5)),
		        step("juror 3 vote", UIEvent.JuryUpdate.class,
		                j -> juryIs(j, juryReview[0], juryReview[1], juryReview[2], null)),
		        step("juror 3 voted", UIEvent.PanelVoteUpdate.class, u -> hidden(u, 6)),
		        step("juror 4 vote", UIEvent.JuryUpdate.class,
		                j -> juryIs(j, juryReview[0], juryReview[1], juryReview[2], juryReview[3])),
		        step("private reveal at the last vote", UIEvent.PanelVoteUpdate.class,
		                u -> u.isOpen() && u.isComplete() && !u.isPublicReveal()
		                        && Boolean.valueOf(good).equals(u.getMajority())
		                        && Arrays.equals(expectedVotes, u.getVotes())),
		        step("decision flagged as revealed", UIEvent.PanelVoteUpdate.class,
		                u -> u.isPublicReveal() && Arrays.equals(expectedVotes, u.getVotes())),
		        step("vote closed", UIEvent.PanelVoteUpdate.class,
		                u -> !u.isOpen() && u.isComplete() && u.isPublicReveal()),
		        step("decision", UIEvent.JuryNotification.class,
		                n -> n.getDeliberationEventType() == (good ? JuryDeliberationEventType.GOOD_LIFT
		                        : JuryDeliberationEventType.BAD_LIFT)),
		        step("lifting resumes", UIEvent.StartLifting.class, x -> true),
		        step("Resume clears the jury displays", UIEvent.JuryDisplayReset.class, x -> true));

		// the decision
		assertEquals(good ? null : Integer.valueOf(3), verdict.getReasonCode());
		assertFalse("the result of the vote is not marked as entered by the Chair", verdict.isChairEntered());
		assertFalse("the Speaker is only informed", verdict.isWaitForAnnouncer());
		assertEquals("reversal", !good, Boolean.TRUE.equals(verdict.getReversal()));
		assertEquals(trace, 1, events.stream().filter(e -> e instanceof UIEvent.JuryNotification n
		        && (n.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT
		                || n.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT))
		        .count());
		// nothing disclosed early, nothing refused
		long completeIndex = events.indexOf(events.stream()
		        .filter(e -> e instanceof UIEvent.PanelVoteUpdate u && u.isComplete()).findFirst().get());
		assertFalse(trace, events.subList(0, (int) completeIndex).stream()
		        .anyMatch(e -> e instanceof UIEvent.PanelVoteUpdate u && u.getVotes() != null));
		assertTrue(trace, events.stream().noneMatch(e -> e instanceof UIEvent.Notification n
		        && n.getLevel() == UIEvent.Notification.Level.ERROR));
		assertEquals("the rule is asked only for a No Lift", good ? 0 : 1, this.recorder.ruleRequests.size());
		assertEquals("the referees are summoned once", 1, this.recorder.summons.size());

		// the attempt board: who has voted in gray, then the seven votes revealed together
		assertTrue(board.panels.stream().anyMatch(
		        p -> p.referees().equals(List.of("voted", "voted", "voted"))
		                && p.jury().equals(List.of("voted", "voted", "voted", "empty"))));
		assertTrue(board.panels.stream()
		        .anyMatch(p -> p.referees().equals(expectedRefs) && p.jury().equals(expectedJury)));
	}

	/** UI events are delivered asynchronously: wait up to 2 s for the condition */
	private static boolean awaitCondition(BooleanSupplier condition) {
		long deadline = System.currentTimeMillis() + 2000;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline) {
				return false;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return true;
	}

	private record Step(String label, Predicate<UIEvent> matcher) {
	}

	private static <T extends UIEvent> Step step(String label, Class<T> type, Predicate<T> condition) {
		return new Step(label, e -> type.isInstance(e) && condition.test(type.cast(e)));
	}

	/** the steps occur in this order (other events may come in between) */
	private static void assertInOrder(List<UIEvent> events, String trace, Step... steps) {
		int index = 0;
		for (Step step : steps) {
			while (index < events.size() && !step.matcher().test(events.get(index))) {
				index++;
			}
			assertTrue("missing or out of order: " + step.label() + "\n" + trace, index < events.size());
			index++;
		}
	}

	/** the recorded UI events in emission order, once delivery has settled */
	private List<UIEvent> settledEvents() {
		int size = -1;
		while (size != this.recorder.all.size()) {
			size = this.recorder.all.size();
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		List<UIEvent> events = new ArrayList<>(this.recorder.all);
		events.sort(Comparator.comparingLong(UIEvent::getSequence));
		return events;
	}

	private static String trace(List<UIEvent> events) {
		StringBuilder sb = new StringBuilder();
		for (UIEvent e : events) {
			sb.append(e.getSequence()).append(' ').append(e.getClass().getSimpleName()).append(' ')
			        .append(describe(e)).append('\n');
		}
		return sb.toString();
	}

	private static boolean juryIs(UIEvent.JuryUpdate j, Boolean... expected) {
		Boolean[] votes = j.getJuryMemberDecision();
		if (votes == null || votes.length < expected.length || j.getJurySize() != 4) {
			return false;
		}
		return Arrays.equals(expected, Arrays.copyOf(votes, expected.length));
	}

	private static int votedCount(UIEvent.PanelVoteUpdate u) {
		int n = 0;
		for (boolean v : u.getVoted()) {
			n += v ? 1 : 0;
		}
		return n;
	}

	private static boolean refereeVoted(UIEvent.PanelVoteUpdate u, int referee, int count) {
		return hidden(u, count) && Integer.valueOf(referee).equals(u.getRefereeUpdated()) && u.getVoted()[referee];
	}

	/** who has voted is known, the values are not */
	private static boolean hidden(UIEvent.PanelVoteUpdate u, int count) {
		return u.isOpen() && !u.isComplete() && u.getVotes() == null && votedCount(u) == count;
	}

	private static List<String> cells(boolean[] votes) {
		List<String> cells = new ArrayList<>();
		for (boolean v : votes) {
			cells.add(v ? "white" : "red");
		}
		return cells;
	}

	private static String describe(UIEvent e) {
		if (e instanceof UIEvent.PanelVoteUpdate u) {
			return "open=" + u.isOpen() + " complete=" + u.isComplete() + " public=" + u.isPublicReveal()
			        + " majority=" + u.getMajority() + " voted=" + Arrays.toString(u.getVoted())
			        + " votes=" + Arrays.toString(u.getVotes()) + " ref=" + u.getRefereeUpdated();
		}
		if (e instanceof UIEvent.JuryNotification n) {
			return n.getDeliberationEventType() + " reason=" + n.getReasonCode() + " chair=" + n.isChairEntered()
			        + " wait=" + n.isWaitForAnnouncer() + " reversal=" + n.getReversal();
		}
		if (e instanceof UIEvent.SummonRef r) {
			return "ref=" + r.ref;
		}
		if (e instanceof UIEvent.PanelVoteCalled c) {
			return "ref=" + c.getRef();
		}
		if (e instanceof UIEvent.JuryUpdate j) {
			return Arrays.toString(j.getJuryMemberDecision()) + " size=" + j.getJurySize();
		}
		if (e instanceof UIEvent.Decision d) {
			return "decision=" + d.decision + " " + d.ref1 + d.ref2 + d.ref3;
		}
		if (e instanceof UIEvent.BreakStarted b) {
			return "" + b.getBreakType();
		}
		if (e instanceof UIEvent.Notification n) {
			return n.getLevel() + " " + n.getFopEventString();
		}
		return "";
	}

	// ---------------------------------------------------------------- scenario helpers

	/** the referees' initial decision on the current athlete's attempt */
	private void lift(boolean... refereeVotes) {
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
		post(new FOPEvent.TimeStopped(new Object()));
		for (int i = 0; i < refereeVotes.length; i++) {
			post(new FOPEvent.DecisionUpdate(new Object(), i, refereeVotes[i]));
		}
		assertNotNull("the lift was recorded", this.fop.getAthleteUnderReview());
	}

	/** a lift followed by four red jury votes: automatic Adjudication Panel Review */
	private void reviewAfterLift(boolean... refereeVotes) {
		lift(refereeVotes);
		juryVotes(R, R, R, R);
		assertPanelReviewBreak();
	}

	private void juryVotes(boolean... votes) {
		for (int i = 0; i < votes.length; i++) {
			juryVote(i, votes[i]);
		}
	}

	private void juryVote(int juror, boolean vote) {
		post(new FOPEvent.JuryMemberDecisionUpdate(new Object(), juror, vote));
	}

	private void reviewVotes(boolean... votes) {
		for (int i = 0; i < votes.length; i++) {
			post(new FOPEvent.DecisionUpdate(new Object(), i, votes[i]));
		}
	}

	private void callForVotes() {
		post(new FOPEvent.PanelVoteCalled(new Object()));
	}

	private void post(FOPEvent e) {
		this.fop.fopEventPost(e);
	}

	private void assertPanelReviewBreak() {
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertEquals(BreakType.JURY, this.fop.getBreakType());
		assertTrue(this.fop.isPanelReview());
	}

	private void assertGoodLift(boolean good) {
		Integer lift = this.fop.getAthleteUnderReview().getActualLift(1);
		assertNotNull(lift);
		assertEquals(good, lift > 0);
	}

	private UIEvent.JuryNotification awaitVerdict() {
		assertTrue("jury decision notification",
		        this.recorder.await(this.recorder.notifications,
		                n -> n.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT
		                        || n.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT));
		return this.recorder.notifications.stream()
		        .filter(n -> n.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT
		                || n.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT)
		        .findFirst().get();
	}

	private void startSessionWithOneAthlete() {
		this.fop.testBefore();
		this.fop.loadGroup(groupA, this, true);
		this.fop.testStartLifting(groupA, this.fop);
		List<Athlete> athletes = this.fop.getDisplayOrder();
		Athlete only = athletes.get(0);
		JPAService.runInTransaction(em -> {
			AthleteSorter.testAssignStartNumbers(athletes);
			only.setSnatch1Declaration(Integer.toString(60));
			only.setCleanJerk1Declaration(Integer.toString(80));
			em.merge(only);
			for (int i = 1; i < athletes.size(); i++) {
				Athlete other = athletes.get(i);
				other.setGroup(null);
				em.merge(other);
			}
			em.flush();
			return null;
		});
		this.fop.loadGroup(groupA, this, true);
		this.fop.fopEventPost(new FOPEvent.SwitchGroup(groupA, this));
		this.fop.fopEventPost(new FOPEvent.StartLifting(this));
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		this.athlete = this.fop.getCurAthlete();
		assertEquals(0, this.athlete.getAttemptsDone().intValue());
	}

	private static void setSwitch(FeatureSwitch featureSwitch, boolean enabled) {
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(featureSwitch, enabled);
		Config.setCurrent(config);
	}

	/** records what the boards are asked to draw for the review vote */
	public static final class PanelRenderer implements DecisionElementState.IDecisionRenderer {
		record Panel(List<String> referees, List<String> jury) {
		}

		final List<Panel> panels = new CopyOnWriteArrayList<>();

		@Override
		public void showPanelVote(UIEvent event, List<String> refereeStates, List<String> juryStates,
		        long generation) {
			this.panels.add(new Panel(refereeStates, juryStates));
		}

		@Override
		public void setEnabled(UIEvent event, boolean enabled) {
		}

		final AtomicInteger resets = new AtomicInteger();

		@Override
		public void resetDecisionDisplay(UIEvent event, long generation) {
			this.resets.incrementAndGet();
		}

		@Override
		public void showDecisionLights(UIEvent event, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
		        boolean singleLight, boolean announcerForced) {
		}

		@Override
		public void showDownSignal(UIEvent.DownSignal event, boolean silent) {
		}

		boolean await(Predicate<Panel> condition) {
			long deadline = System.currentTimeMillis() + 2_000;
			while (System.currentTimeMillis() < deadline) {
				if (this.panels.stream().anyMatch(condition)) {
					return true;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			return false;
		}
	}

	/** records the UI events that devices, consoles and MQTT react to */
	public static final class Recorder {
		final List<UIEvent.JuryNotification> notifications = new CopyOnWriteArrayList<>();
		final List<UIEvent.SummonRef> summons = new CopyOnWriteArrayList<>();
		final List<UIEvent.PanelVoteCalled> voteCalls = new CopyOnWriteArrayList<>();
		final List<UIEvent.PanelVoteUpdate> updates = new CopyOnWriteArrayList<>();
		final List<UIEvent.JuryVotesPending> pending = new CopyOnWriteArrayList<>();
		final List<UIEvent.Notification> errors = new CopyOnWriteArrayList<>();
		final List<UIEvent.JuryRuleRequired> ruleRequests = new CopyOnWriteArrayList<>();
		final List<UIEvent.JuryDisplayReset> displayResets = new CopyOnWriteArrayList<>();
		final List<UIEvent.JurySizeChanged> jurySizeChanges = new CopyOnWriteArrayList<>();
		/** every UI event, in delivery order (sort by sequence for emission order) */
		final List<UIEvent> all = new CopyOnWriteArrayList<>();

		@Subscribe
		public void any(UIEvent e) {
			this.all.add(e);
		}

		@Subscribe
		public void juryDisplayReset(UIEvent.JuryDisplayReset e) {
			this.displayResets.add(e);
		}

		@Subscribe
		public void jurySizeChanged(UIEvent.JurySizeChanged e) {
			this.jurySizeChanges.add(e);
		}

		@Subscribe
		public void juryRuleRequired(UIEvent.JuryRuleRequired e) {
			this.ruleRequests.add(e);
		}

		@Subscribe
		public void notification(UIEvent.Notification e) {
			if (e.getLevel() == UIEvent.Notification.Level.ERROR) {
				this.errors.add(e);
			}
		}

		@Subscribe
		public void juryNotification(UIEvent.JuryNotification e) {
			this.notifications.add(e);
		}

		@Subscribe
		public void summonRef(UIEvent.SummonRef e) {
			this.summons.add(e);
		}

		@Subscribe
		public void panelVoteCalled(UIEvent.PanelVoteCalled e) {
			this.voteCalls.add(e);
		}

		@Subscribe
		public void panelVoteUpdate(UIEvent.PanelVoteUpdate e) {
			this.updates.add(e);
		}

		@Subscribe
		public void juryVotesPending(UIEvent.JuryVotesPending e) {
			this.pending.add(e);
		}

		<T> boolean await(List<T> events, Predicate<T> condition) {
			long deadline = System.currentTimeMillis() + 2_000;
			while (System.currentTimeMillis() < deadline) {
				if (events.stream().anyMatch(condition)) {
					return true;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			return false;
		}
	}
}
