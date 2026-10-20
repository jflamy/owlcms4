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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * See compliance/seven-person-adjudication.md.
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
		setSwitch(FeatureSwitch.SEVEN_PERSON_ADJUDICATION, true);
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, false);
		setSwitch(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, false);
		Competition.getCurrent().setVideoPlaybackTechnology(true);
		// default: a decision entered by the Jury Chair must be announced by the Speaker
		Competition.getCurrent().setAnnouncerControlledJuryDecision(true);

		this.fop = MockFieldOfPlay.create(AthleteRepository.findAll(), new MockCountdownTimer(),
		        new MockCountdownTimer());
		OwlcmsSession.setFop(this.fop);
		this.fop.setPlatform(new Platform());
		startSessionWithOneAthlete();
		this.recorder = new Recorder();
		this.fop.getUiEventBus().register(this.recorder);
	}

	@After
	public void restoreSettings() {
		setSwitch(FeatureSwitch.SEVEN_PERSON_ADJUDICATION, false);
		setSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, false);
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		Competition.getCurrent().setAnnouncerControlledJuryDecision(true);
	}

	// ---------------------------------------------------------------- configuration

	@Test
	public void switchGivesAFourMemberJury() {
		assertEquals(4, this.fop.getJurySize());
		assertTrue(this.fop.isPanelProcedure());
		setSwitch(FeatureSwitch.SEVEN_PERSON_ADJUDICATION, false);
		assertFalse(this.fop.isPanelProcedure());
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
	public void withoutVideoPlaybackTheClockIsFreeWhenTheDecisionIsNotHeld() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(W, W, W);
		juryVotes(W, W, W);
		post(new FOPEvent.TimeStarted(new Object()));
		assertEquals(FOPState.TIME_RUNNING, this.fop.getState());
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
		assertTrue("seven votes revealed together",
		        this.recorder.await(this.recorder.updates, UIEvent.PanelVoteUpdate::isComplete));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.GOOD_LIFT, verdict.getDeliberationEventType());
		assertFalse("decided by the panel", verdict.isChairEntered());
		assertFalse("shown without waiting for the Speaker", verdict.isWaitForAnnouncer());
		assertEquals("the break ends by itself", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void resumeAfterASummonDoesNotLeaveTheJuryVotesAwaited() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(W, W, W);
		juryVotes(W, W, W, W);
		awaitVerdict();
		assertEquals("the break ends by itself", FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertFalse("the review is over", this.fop.isPanelReview());

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
		assertFalse("not revealed before the rule is given",
		        this.recorder.updates.stream().anyMatch(UIEvent.PanelVoteUpdate::isComplete));

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 3));
		assertTrue("revealed with the decision",
		        this.recorder.await(this.recorder.updates, UIEvent.PanelVoteUpdate::isComplete));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.BAD_LIFT, verdict.getDeliberationEventType());
		assertEquals(Integer.valueOf(3), verdict.getReasonCode());
		assertFalse(verdict.isChairEntered());
		assertFalse(verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(false);
	}

	@Test
	public void chairOverrideIsMarkedAndAppliedWithoutTheSpeaker() {
		reviewAfterLift(W, W, W);
		callForVotes();
		reviewVotes(R);
		Athlete underReview = this.fop.getAthleteUnderReview();
		post(new FOPEvent.JuryDecision(underReview, new Object(), R, true, 2));
		assertFalse("the vote is closed", this.fop.isPanelVoteOpen());
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
	public void boardsShowTheSevenVotesTogether() {
		PanelRenderer boards = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(boards);
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
		assertFalse("a No Lift is not revealed before the rule is given",
		        boards.panels.stream().anyMatch(p -> p.referees().contains("red")));

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 17));
		assertTrue("the seven votes revealed together with the decision", boards.await(
		        p -> p.referees().equals(List.of("red", "red", "red"))
		                && p.jury().equals(List.of("red", "red", "red", "white"))));
	}

	@Test
	public void boardsShowTheVotesThatLedToTheReview() {
		PanelRenderer boards = new PanelRenderer();
		DecisionElementState boardState = new DecisionElementState(boards);
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
	public void unanimousRedJuryReversalWaitsForTheRule() {
		juryDeliberation(3, W, W, W);
		juryVotes(R, R, R);
		assertRuleRequested();

		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), false, true, 1));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.BAD_LIFT, verdict.getDeliberationEventType());
		assertEquals(Integer.valueOf(1), verdict.getReasonCode());
	}

	@Test
	public void unanimousRedJuryConfirmationWaitsForTheRule() {
		juryDeliberation(3, R, R, W);
		juryVotes(R, R, R);
		assertRuleRequested();
	}

	@Test
	public void unanimousWhiteJuryIsSentAutomatically() {
		juryDeliberation(3, R, R, R);
		juryVotes(W, W, W);
		assertSentWithoutRule(JuryDeliberationEventType.GOOD_LIFT);
	}

	@Test
	public void splitJuryConfirmingRedRefereesWaitsForTheRule() {
		juryDeliberation(3, R, R, W);
		juryVotes(R, W, W);
		assertRuleRequested();
	}

	@Test
	public void splitJuryConfirmingWhiteRefereesIsSentAutomatically() {
		juryDeliberation(3, W, W, R);
		juryVotes(R, R, W);
		assertSentWithoutRule(JuryDeliberationEventType.GOOD_LIFT);
	}

	@Test
	public void fiveMemberMajorityReversalWithVideoWaitsForTheRule() {
		juryDeliberation(5, W, W, R);
		juryVotes(R, R, R, W, W);
		assertRuleRequested();
	}

	@Test
	public void fiveMemberMajorityConfirmationWithVideoWaitsForTheRule() {
		juryDeliberation(5, R, R, W);
		juryVotes(R, R, R, W, W);
		assertRuleRequested();
	}

	@Test
	public void fiveMemberMajorityWithoutVideoLeavesTheRefereesDecision() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		juryDeliberation(5, W, W, R);
		juryVotes(R, R, R, W, W);
		assertSentWithoutRule(JuryDeliberationEventType.GOOD_LIFT);
	}

	@Test
	public void fiveMemberUnanimousReversalWithoutVideoWaitsForTheRule() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		juryDeliberation(5, W, W, R);
		juryVotes(R, R, R, R, R);
		assertRuleRequested();
	}

	@Test
	public void withoutVideoPlaybackAUnanimousJuryStopsAndRevotes() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(W, W, W);
		juryVotes(R, R, R, R);
		assertEquals(FOPState.BREAK, this.fop.getState());
		assertEquals(BreakType.JURY, this.fop.getBreakType());
		assertTrue("automatic jury deliberation", this.fop.isJuryReview());
		assertFalse("not an Adjudication Panel Review", this.fop.isPanelReview());
		assertFalse("the referees are not summoned", this.recorder.summons.stream().anyMatch(s -> s.ref == 0));
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
	public void withoutVideoPlaybackASplitSecondVoteLeavesTheRefereesDecision() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(W, W, W);
		juryVotes(R, R, R, R);
		callForVotes();
		juryVotes(R, R, R, W);
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals("decision stands", JuryDeliberationEventType.GOOD_LIFT, verdict.getDeliberationEventType());
		assertFalse(verdict.isChairEntered());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void withoutVideoPlaybackAUnanimousSecondVoteReversesToGoodLift() {
		Competition.getCurrent().setVideoPlaybackTechnology(false);
		lift(R, R, R);
		juryVotes(W, W, W, W);
		assertTrue(this.fop.isJuryReview());
		callForVotes();
		juryVotes(W, W, W, W);
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertEquals(JuryDeliberationEventType.GOOD_LIFT, verdict.getDeliberationEventType());
		assertFalse(verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(true);
	}

	@Test
	public void fourMemberChairDecisionWithoutVideoIsAppliedWithoutTheSpeaker() {
		fourMemberDeliberationWithoutVideo(W, W, W);
		juryVotes(R, R);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 2));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertTrue("Jury Chair Entered Decision", verdict.isChairEntered());
		assertFalse("the Speaker is only informed", verdict.isWaitForAnnouncer());
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, this.fop.getState());
		assertGoodLift(false);
	}

	@Test
	public void threeMemberChairDecisionStillWaitsForTheSpeaker() {
		juryDeliberation(3, W, W, W);
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), new Object(), R, true, 1));
		UIEvent.JuryNotification verdict = awaitVerdict();
		assertTrue("announcer controlled jury decision", verdict.isWaitForAnnouncer());
		assertEquals(FOPState.BREAK, this.fop.getState());
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
		setSwitch(FeatureSwitch.SEVEN_PERSON_ADJUDICATION, false);
		this.fop.setJurySize(jurySize);
		assertFalse(this.fop.isPanelProcedure());
		lift(refereeVotes);
		post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		assertEquals(FOPState.BREAK, this.fop.getState());
	}

	private void assertRuleRequested() {
		assertTrue("the Jury Control pages are asked for the rule",
		        this.recorder.await(this.recorder.ruleRequests, r -> true));
		assertFalse("no decision without the rule", this.recorder.notifications.stream()
		        .anyMatch(n -> n.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT
		                || n.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT));
	}

	private void assertSentWithoutRule(JuryDeliberationEventType expected) {
		assertEquals(expected, awaitVerdict().getDeliberationEventType());
		assertTrue("no rule is asked for", this.recorder.ruleRequests.isEmpty());
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

		@Override
		public void resetDecisionDisplay(UIEvent event, long generation) {
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

		@Subscribe
		public void juryDisplayReset(UIEvent.JuryDisplayReset e) {
			this.displayResets.add(e);
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
