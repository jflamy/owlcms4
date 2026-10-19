package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.components.elements.DecisionElementState;
import app.owlcms.components.elements.DecisionElementState.DisplayMode;
import app.owlcms.components.elements.DecisionElementState.IDecisionRenderer;
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
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;

public class DecisionElementStateTest {
    private static final Logger logger = LoggerFactory.getLogger(DecisionElementStateTest.class);
    private static final long DECISION_INPUT_IGNORE_WINDOW_MS = 100;
    private static final long DECISION_VISIBLE_DURATION_MS = 1_000;
    private static final long MINIMUM_DOWN_SIGNAL_VISIBLE_MS = 900;
    private static final long RECORD_NOTIFICATION_DELAY_MS = 0;
    private static final int REVERSAL_DELAY_MS = 1_200;
    private static final long TIMING_TOLERANCE_MS = 350;

    private static Group groupA;
    private static final List<String> timelineSummaries = new CopyOnWriteArrayList<>();
    private List<Athlete> athletes;
    private final Object[] juryOrigins = { new Object(), new Object(), new Object() };

    @BeforeClass
    public static void setupTests() {
        timelineSummaries.clear();
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
    }

    @AfterClass
    public static void tearDownTests() {
        logTimelineSummary();
        JPAService.close();
    }

    private static void logTimelineSummary() {
        logger.info("DecisionElementStateTest TIMELINE SUMMARY START");
        for (String summary : timelineSummaries) {
            logger.info(summary);
        }
        logger.info("DecisionElementStateTest TIMELINE SUMMARY END");
    }

    @Before
    public void setupTest() {
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
        this.athletes = AthleteRepository.findAll();
        setJuryDecisionsOnAttemptBoard(false);
        setSevenPersonAdjudication(false);
        Competition.getCurrent().setVideoPlaybackTechnology(false);
    }

    @Test
    public void delayedGoodLift() throws Exception {
        // @formatter:off
		// Expected timeline:
		// stop clock
		// ref1 white
		// ref2 white renders DOWN
		// ref3 white emits InitialDecision but renders nothing
		// reversal delay commits good lift and renders Decision
		// decision window elapses
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(false);
        enterInitialGoodDecision(scenario);

        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());
        Thread.sleep(250);
        assertStillDownAndUncommitted(scenario, "provisional decision must not commit the lift");

        assertFinalDecision(scenario, true);
        assertReset(scenario);
        assertDelayedTiming(scenario);
        scenario.timeline.log("delayedGoodLift");
    }

    @Test
    public void delayedReversalBeforeCommit() throws Exception {
        // @formatter:off
		// Expected timeline:
		// stop clock
		// initial two whites render DOWN
		// third white emits InitialDecision but renders nothing
		// two refs reverse to red before Decision is visible
		// reversal delay commits bad lift and renders Decision
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(false);
        enterInitialGoodDecision(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());
        Thread.sleep(250);
        assertStillDownAndUncommitted(scenario, "provisional decision must not commit the lift");

        reverseToBadBeforeDecisionVisible(scenario);

        assertFinalDecision(scenario, false);
        assertReset(scenario);
        assertDelayedTiming(scenario);
        scenario.timeline.log("delayedReversalBeforeCommit");
    }

    @Test
    public void immediateGoodLift() throws Exception {
        // @formatter:off
		// Expected timeline:
		// stop clock
		// ref1 white
		// ref2 white renders DOWN
		// ref3 white emits InitialDecision but renders nothing
		// remaining minimum DOWN time elapses and renders Decision
		// reversal delay commits good lift
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(true);
        enterInitialGoodDecision(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        assertFalse("final decision rendered before minimum down-signal time elapsed",
                scenario.renderer.awaitFinalDecision(500));
        assertStillDownAndUncommitted(scenario, "early immediate decision must not commit the lift");

        assertFinalDecisionVisibleButUncommitted(scenario, true);
        assertCommitted(scenario, true);
        assertReset(scenario);
        assertImmediateTiming(scenario);
        scenario.timeline.log("immediateGoodLift");
    }

    @Test
    public void immediateReversalWhileDownSignalVisible()
            throws Exception {
        // @formatter:off
		// Expected timeline:
		// stop clock
		// initial two whites render DOWN
		// third white emits InitialDecision but renders nothing
		// two refs reverse to red while DOWN is still visible
		// remaining minimum DOWN time renders bad Decision
		// reversal delay commits bad lift
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(true);
        enterInitialGoodDecision(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        reverseToBadBeforeDecisionVisible(scenario);
        assertFalse("final decision rendered before minimum down-signal time elapsed",
                scenario.renderer.awaitFinalDecision(250));
        assertStillDownAndUncommitted(scenario, "visible-DOWN reversal must not commit the lift");

        assertReversedDecisionVisibleButUncommitted(scenario);
        assertCommitted(scenario, false);
        assertReset(scenario);
        assertImmediateTiming(scenario);
        scenario.timeline.log("immediateReversalWhileDownSignalVisible");
    }

    @Test
    public void immediateReversalWhileVisible()
            throws Exception {
        // @formatter:off
		// Expected timeline:
		// stop clock
		// initial two whites render DOWN
		// third white renders Decision after minimum DOWN
		// two refs reverse to red while Decision is visible
		// rendered Decision updates to bad
		// reversal delay commits bad lift
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(true);
        enterInitialGoodDecision(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        assertFalse("final decision rendered before minimum down-signal time elapsed",
                scenario.renderer.awaitFinalDecision(500));
        assertStillDownAndUncommitted(scenario, "early immediate decision must not commit the lift");

        assertFinalDecisionVisibleButUncommitted(scenario, true);
        reverseToBadAfterDecisionVisible(scenario);
        assertReversedDecisionWasEmittedAndRendered(scenario);
        assertEquals("visible reversal must not commit until reversal timer ends", 0,
                scenario.curLifter.getAttemptsDone().intValue());

        assertCommitted(scenario, false);
        assertReset(scenario);
        assertImmediateTiming(scenario);
        scenario.timeline.log("immediateReversalWhileVisible");
    }

    @Test
    public void delayedClockNotStarted() throws Exception {
        // @formatter:off
		// Expected timeline:
		// no clock start
		// ref1 white forces decision setup
		// ref2 white renders DOWN
		// ref3 white emits InitialDecision but renders nothing
		// reversal delay commits good lift and renders Decision
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(false);
        enterInitialGoodDecisionWithoutClock(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        Thread.sleep(250);
        assertStillDownAndUncommitted(scenario, "clock-not-started provisional decision must not commit the lift");

        assertFinalDecision(scenario, true);
        assertReset(scenario);
        assertDelayedTiming(scenario);
        scenario.timeline.log("delayedClockNotStarted");
    }

    @Test
    public void delayedClockNotStartedReversalBeforeCommit() throws Exception {
        // @formatter:off
		// Expected timeline:
		// no clock start
		// ref1 white forces decision setup
		// ref2 white renders DOWN
		// ref3 white emits InitialDecision but renders nothing
		// two refs reverse to red before Decision is visible
		// reversal delay commits bad lift and renders Decision
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(false);
        enterInitialGoodDecisionWithoutClock(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        Thread.sleep(250);
        assertStillDownAndUncommitted(scenario, "clock-not-started provisional decision must not commit the lift");

        reverseToBadBeforeDecisionVisible(scenario);

        assertFinalDecision(scenario, false);
        assertReset(scenario);
        assertDelayedTiming(scenario);
        scenario.timeline.log("delayedClockNotStartedReversalBeforeCommit");
    }

    @Test
    public void immediateClockNotStarted() throws Exception {
        // @formatter:off
		// Expected timeline:
		// no clock start
		// ref1 white forces decision setup
		// ref2 white renders DOWN
		// ref3 white emits InitialDecision but renders nothing
		// remaining minimum DOWN time renders Decision
		// reversal delay commits good lift
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(true);
        enterInitialGoodDecisionWithoutClock(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        assertFalse("final decision rendered before minimum down-signal time elapsed",
                scenario.renderer.awaitFinalDecision(250));
        assertStillDownAndUncommitted(scenario, "clock-not-started immediate decision must not commit early");

        assertFinalDecisionVisibleButUncommitted(scenario, true);
        assertCommitted(scenario, true);
        assertReset(scenario);
        assertImmediateTiming(scenario);
        scenario.timeline.log("immediateClockNotStarted");
    }

    @Test
    public void immediateClockNotStartedReversalWhileDownSignalVisible() throws Exception {
        // @formatter:off
		// Expected timeline:
		// no clock start
		// ref1 white forces decision setup
		// ref2 white renders DOWN
		// ref3 white emits InitialDecision but renders nothing
		// two refs reverse to red while DOWN is still visible
		// remaining minimum DOWN time renders bad Decision
		// reversal delay commits bad lift
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(true);
        enterInitialGoodDecisionWithoutClock(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        reverseToBadBeforeDecisionVisible(scenario);
        assertFalse("final decision rendered before minimum down-signal time elapsed",
                scenario.renderer.awaitFinalDecision(250));
        assertStillDownAndUncommitted(scenario, "clock-not-started visible-DOWN reversal must not commit early");

        assertReversedDecisionVisibleButUncommitted(scenario);
        assertCommitted(scenario, false);
        assertReset(scenario);
        assertImmediateTiming(scenario);
        scenario.timeline.log("immediateClockNotStartedReversalWhileDownSignalVisible");
    }

    @Test
    public void immediateClockNotStartedReversalWhileVisible() throws Exception {
        // @formatter:off
		// Expected timeline:
		// no clock start
		// ref1 white forces decision setup
		// ref2 white renders DOWN
		// ref3 white emits InitialDecision but renders nothing
		// remaining minimum DOWN time renders Decision
		// two refs reverse to red while Decision is visible
		// rendered Decision updates to bad
		// reversal delay commits bad lift
		// reset renders
		// @formatter:on
        DecisionScenario scenario = startScenario(true);
        enterInitialGoodDecisionWithoutClock(scenario);
        assertTrue("initial decision UI event was not fired", scenario.timeline.awaitInitialDecision());

        assertFalse("final decision rendered before minimum down-signal time elapsed",
                scenario.renderer.awaitFinalDecision(250));
        assertStillDownAndUncommitted(scenario, "clock-not-started immediate decision must not commit early");

        assertFinalDecisionVisibleButUncommitted(scenario, true);
        reverseToBadAfterDecisionVisible(scenario);
        assertReversedDecisionWasEmittedAndRendered(scenario);
        assertEquals("visible reversal must not commit until reversal timer ends", 0,
                scenario.curLifter.getAttemptsDone().intValue());

        assertCommitted(scenario, false);
        assertReset(scenario);
        assertImmediateTiming(scenario);
        scenario.timeline.log("immediateClockNotStartedReversalWhileVisible");
    }

    @Test
    public void juryRenderingIsOptInAndSupportsTwoThroughFiveMembers() {
        MockDecisionRenderer renderer = new MockDecisionRenderer(new EventTimeline());
        DecisionElementState state = new DecisionElementState(renderer);
        UIEvent.JuryUpdate update = new UIEvent.JuryUpdate(this, 0,
                new Boolean[] { true, false, null, null, null }, 3, null);
        state.slaveJuryUpdate(update);
        assertEquals(0, renderer.juryRenderCount);

        state.setShowJuryDecisions(true);
        state.slaveJuryUpdate(update);
        assertEquals(List.of("voted", "voted", "empty"), renderer.juryLights);
        long previousGeneration = renderer.juryGeneration;
        for (int size = 2; size <= 5; size++) {
            Boolean[] votes = new Boolean[5];
            for (int index = 0; index < size; index++) {
                votes[index] = index % 2 == 0;
            }
            state.slaveJuryUpdate(new UIEvent.JuryUpdate(this, false, votes, size, null));
            assertEquals(size, renderer.juryLights.size());
            for (int index = 0; index < size; index++) {
                assertEquals(index % 2 == 0 ? "white" : "red", renderer.juryLights.get(index));
            }
        }
        assertFalse(state.isCurrentJuryGeneration(previousGeneration));
        state.setShowJuryDecisions(false);
        assertEquals(List.of(), renderer.juryLights);
        int renderCount = renderer.juryRenderCount;
        state.slaveJuryUpdate(update);
        state.slaveResetOnNewClock(new UIEvent.ResetOnNewClock(null, this, null));
        assertEquals(renderCount, renderer.juryRenderCount);
    }

    @Test
    public void startTimeReplacesPendingNewClockReset() {
        MockDecisionRenderer renderer = new MockDecisionRenderer(new EventTimeline());
        DecisionElementState state = new DecisionElementState(renderer);
        state.setResetOnClockStart(true);
        state.slaveResetOnNewClock(new UIEvent.ResetOnNewClock(null, this, null));
        long pendingReset = renderer.resetGeneration;
        assertTrue(pendingReset >= 0);
        state.slaveStartTimer(new UIEvent.StartTime(60000, this, false, null));
        long clockStartReset = renderer.resetGeneration;
        assertFalse("clock start must supersede the pending new-clock reset",
                state.isCurrentGeneration(pendingReset));
        assertTrue("clock start must issue a replacement reset", clockStartReset > pendingReset);
        assertTrue("the replacement reset must remain current", state.isCurrentGeneration(clockStartReset));
    }

    @Test
    public void juryLightsPersistThroughResetUntilNextReveal() {
        MockDecisionRenderer renderer = new MockDecisionRenderer(new EventTimeline());
        DecisionElementState state = new DecisionElementState(renderer);
        state.setShowJuryDecisions(true);
        UIEvent.JuryUpdate update = new UIEvent.JuryUpdate(this, false,
                new Boolean[] { true, false }, 2, null);
        state.slaveJuryUpdate(update);
        assertEquals(List.of("white", "red"), renderer.juryLights);
        int renderCount = renderer.juryRenderCount;
        state.slaveDecisionReset(new UIEvent.DecisionReset(null, this, null));
        state.slaveResetOnNewClock(new UIEvent.ResetOnNewClock(null, this, null));
        assertEquals(renderCount, renderer.juryRenderCount);
        assertEquals(List.of("white", "red"), renderer.juryLights);
        state.slaveJuryUpdate(update);
        assertEquals(List.of("white", "red"), renderer.juryLights);
        // reveal re-syncs from the FOP; with no FOP there is nothing to show
        state.slaveShowDecision(new UIEvent.Decision(null, true, true, true, true, this, null, false));
        assertEquals(List.of(), renderer.juryLights);
    }

    @Test
    public void incompleteJuryWaitsForSafeDeadline() throws Exception {
        DecisionScenario scenario = startScenario(false);
        configureJuryTiming(scenario);
        scenario.decisionState.setShowJuryDecisions(true);
        enterInitialGoodDecision(scenario);
        assertFinalDecision(scenario, true);
        assertEquals(List.of("empty", "empty", "empty"), scenario.renderer.juryLights);
        assertReset(scenario);
        scenario.timeline.assertElapsedAtLeast("UI Decision", "UI DecisionReset", 2_000,
                "unfinished jury must use the safe deadline");
        scenario.timeline.assertElapsedAtMost("UI Decision", "UI DecisionReset", 2_000,
                "unfinished jury must not block indefinitely");
    }

    @Test
    public void juryWaitRequiresAttemptBoardFeature() throws Exception {
        DecisionScenario scenario = startScenario(false);
        scenario.fopState.setJurySize(3);
        scenario.fopState.setJuryDecisionTimingForTests(2_000, 800);
        enterInitialGoodDecision(scenario);
        assertFinalDecision(scenario, true);
        assertReset(scenario);
        scenario.timeline.assertElapsedAtMost("UI Decision", "UI DecisionReset", DECISION_VISIBLE_DURATION_MS,
                "jury size alone must not extend the decision display");
    }

    @Test
    public void juryAlreadyCompleteUsesNormalDuration() throws Exception {
        DecisionScenario scenario = startScenario(false);
        configureJuryTiming(scenario);
        enterInitialGoodDecision(scenario);
        completeJury(scenario);
        assertFinalDecision(scenario, true);
        assertReset(scenario);
        scenario.timeline.assertElapsedAtMost("UI Decision", "UI DecisionReset", DECISION_VISIBLE_DURATION_MS,
                "jury completed before referee publication needs no extension");
    }

    @Test
    public void earlyJuryShortensDeadlineWithoutRestartingForRepeatedVotes() throws Exception {
        DecisionScenario scenario = startScenario(false);
        configureJuryTiming(scenario);
        enterInitialGoodDecision(scenario);
        assertFinalDecision(scenario, true);
        completeJury(scenario);
        Thread.sleep(400);
        completeJury(scenario);
        assertReset(scenario);
        scenario.timeline.assertElapsedAtLeast("UI Decision", "UI DecisionReset", DECISION_VISIBLE_DURATION_MS,
                "early jury must preserve the referee display minimum");
        scenario.timeline.assertElapsedAtMost("UI Decision", "UI DecisionReset", DECISION_VISIBLE_DURATION_MS,
                "repeated votes must not restart the hold");
    }

    @Test
        public void lateJuryGetsFullHoldBeyondWaitingDeadline() throws Exception {
        DecisionScenario scenario = startScenario(false);
        configureJuryTiming(scenario);
        enterInitialGoodDecision(scenario);
        assertFinalDecision(scenario, true);
        Thread.sleep(1_500);
        completeJury(scenario);
        assertReset(scenario);
        scenario.timeline.assertElapsedAtLeast("UI Decision", "UI DecisionReset", 2_300,
            "completion before the waiting deadline must receive the full jury hold");
        scenario.timeline.assertElapsedAtMost("UI Decision", "UI DecisionReset", 2_300,
            "reset must follow the jury hold without further extension");
    }

    @Test
    public void immediateRefereeModeKeepsCommitTimingWithJury() throws Exception {
        DecisionScenario scenario = startScenario(true);
        configureJuryTiming(scenario);
        enterInitialGoodDecision(scenario);
        assertFinalDecisionVisibleButUncommitted(scenario, true);
        completeJury(scenario);
        assertCommitted(scenario, true);
        assertReset(scenario);
        assertImmediateTiming(scenario);
    }

    @Test
    public void startLiftingInvalidatesPendingJuryReset() throws Exception {
        DecisionScenario scenario = startScenario(false);
        configureJuryTiming(scenario);
        enterInitialGoodDecision(scenario);
        assertFinalDecision(scenario, true);
        scenario.fopState.fopEventPost(new FOPEvent.StartLifting(this));
        assertFalse("old reset must not run after leaving its decision phase",
                scenario.timeline.decisionReset.await(2_400, TimeUnit.MILLISECONDS));
    }

    @Test
    public void sevenPersonAdjudicationDerivesJurySizeFour() {
        DecisionScenario scenario = startScenario(false);
        scenario.fopState.setJurySize(3);
        assertEquals(3, scenario.fopState.getJurySize());
        assertFalse(scenario.fopState.isSevenPersonAdjudication());

        setSevenPersonAdjudication(true);
        try {
            assertTrue(scenario.fopState.isSevenPersonAdjudication());
            assertEquals("switch forces a four-member jury", 4, scenario.fopState.getJurySize());
            scenario.fopState.setJurySize(5);
            assertEquals("stored size is ignored while the switch is on", 4, scenario.fopState.getJurySize());
        } finally {
            setSevenPersonAdjudication(false);
        }
        assertEquals("stored size is used again when the switch is off", 5, scenario.fopState.getJurySize());
    }

    @Test
    public void juryVotesBeyondJurySizeAreDropped() {
        DecisionScenario scenario = startScenario(false);
        scenario.fopState.setJurySize(3);
        // events with the same origin in the same millisecond are discarded as duplicates, use one origin per vote
        for (int index : new int[] { -1, 3, 4 }) {
            scenario.fopState.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(new Object(), index, false));
        }
        Boolean[] votes = scenario.fopState.getJuryMemberDecision();
        assertEquals("juror 4 does not exist in a 3-member jury", null, votes[3]);
        assertEquals("juror 5 does not exist in a 3-member jury", null, votes[4]);

        scenario.fopState.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(new Object(), 2, false));
        assertEquals(Boolean.FALSE, scenario.fopState.getJuryMemberDecision()[2]);
    }

    @Test
    public void fourthJurorVoteIsAcceptedWithSevenPersonAdjudication() {
        DecisionScenario scenario = startScenario(false);
        setSevenPersonAdjudication(true);
        try {
            scenario.fopState.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(new Object(), 3, false));
            scenario.fopState.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(new Object(), 4, false));
            assertEquals(Boolean.FALSE, scenario.fopState.getJuryMemberDecision()[3]);
            assertEquals("a fifth juror does not exist with the seven-person panel", null,
                    scenario.fopState.getJuryMemberDecision()[4]);
        } finally {
            setSevenPersonAdjudication(false);
        }
    }

    @Test
    public void panelAgreementDoesNotStopCompetition() throws Exception {
        DecisionScenario scenario = startPanelScenario(false);
        try {
            enterInitialGoodDecision(scenario);
            assertFinalDecision(scenario, true);
            assertReset(scenario);
            assertTrue("jury votes are awaited after the lift", scenario.fopState.isPanelVotePending());
            // 3 referee whites + 2 jury whites: a 2-2 jury split leaves the referee decision
            voteJury(scenario, true, true, false, false);
            assertFalse(scenario.fopState.isPanelVotePending());
            assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, scenario.fopState.getState());
            assertFalse(scenario.fopState.isPanelReview());
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void panelMajorityOppositeStartsReviewAtOnce() throws Exception {
        DecisionScenario scenario = startPanelScenario(false);
        try {
            enterInitialGoodDecision(scenario);
            assertFinalDecision(scenario, true);
            assertReset(scenario);
            assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, scenario.fopState.getState());
            // 3 referee whites, 4 jury reds: the panel majority is No Lift
            voteJury(scenario, false, false, false, false);
            assertEquals(FOPState.BREAK, scenario.fopState.getState());
            assertEquals(BreakType.JURY, scenario.fopState.getBreakType());
            assertTrue(scenario.fopState.isPanelReview());
            assertEquals("the jury votes that caused the review are kept", Boolean.FALSE,
                    scenario.fopState.getJuryMemberDecision()[3]);
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void panelReviewWaitsForTheHeldDecisionToClear() throws Exception {
        DecisionScenario scenario = startPanelScenario(true);
        try {
            enterInitialGoodDecision(scenario);
            assertFinalDecision(scenario, true);
            // longer than the legacy jury cap (2 s in these tests): no time limit with the panel
            Thread.sleep(2_500);
            assertEquals("decision stays visible until the jury has voted", FOPState.DECISION_VISIBLE,
                    scenario.fopState.getState());
            voteJury(scenario, false, false, false, false);
            assertEquals("the break waits for the decision to clear", FOPState.DECISION_VISIBLE,
                    scenario.fopState.getState());
            assertTrue("decision reset UI event was not fired", scenario.timeline.awaitDecisionReset());
            awaitState(scenario, FOPState.BREAK);
            assertEquals(BreakType.JURY, scenario.fopState.getBreakType());
            assertTrue(scenario.fopState.isPanelReview());
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void clockStartRefusedUntilOverrideWhileJuryVotesPending() throws Exception {
        DecisionScenario scenario = startPanelScenario(false);
        PendingListener pending = new PendingListener();
        scenario.fopState.getUiEventBus().register(pending);
        try {
            enterInitialGoodDecision(scenario);
            assertFinalDecision(scenario, true);
            assertReset(scenario);
            voteJury(scenario, true, true, true);

            scenario.fopState.fopEventPost(new FOPEvent.TimeStarted(new Object()));
            assertEquals("clock start refused while a jury vote is missing", FOPState.CURRENT_ATHLETE_DISPLAYED,
                    scenario.fopState.getState());
            assertTrue("deciding console is asked to confirm", pending.awaitPending(true));

            scenario.fopState.fopEventPost(new FOPEvent.TimeStarted(new Object(), true));
            assertEquals(FOPState.TIME_RUNNING, scenario.fopState.getState());
            assertFalse(scenario.fopState.isPanelVotePending());
            assertEquals("incomplete jury votes are discarded", null, scenario.fopState.getJuryMemberDecision()[0]);
            assertTrue("consoles are told the wait is over", pending.awaitPending(false));
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void clockStartOverrideClearsHeldDecision() throws Exception {
        DecisionScenario scenario = startPanelScenario(true);
        try {
            enterInitialGoodDecision(scenario);
            assertFinalDecision(scenario, true);
            scenario.fopState.fopEventPost(new FOPEvent.TimeStarted(new Object()));
            assertEquals(FOPState.DECISION_VISIBLE, scenario.fopState.getState());
            scenario.fopState.fopEventPost(new FOPEvent.TimeStarted(new Object(), true));
            assertEquals(FOPState.TIME_RUNNING, scenario.fopState.getState());
            assertFalse(scenario.fopState.isPanelVotePending());
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void clockStartNotGuardedWithoutVideoPlayback() throws Exception {
        DecisionScenario scenario = startPanelScenario(false);
        Competition.getCurrent().setVideoPlaybackTechnology(false);
        try {
            enterInitialGoodDecision(scenario);
            assertFinalDecision(scenario, true);
            assertReset(scenario);
            assertFalse(scenario.fopState.isPanelVotePending());
            scenario.fopState.fopEventPost(new FOPEvent.TimeStarted(new Object()));
            assertEquals(FOPState.TIME_RUNNING, scenario.fopState.getState());
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void reviewVoteGoodLiftIsAppliedAutomatically() throws Exception {
        DecisionScenario scenario = startPanelReview();
        PanelListener listener = new PanelListener();
        scenario.fopState.getUiEventBus().register(listener);
        try {
            scenario.fopState.fopEventPost(new FOPEvent.PanelVoteCalled(new Object()));
            assertTrue(scenario.fopState.isPanelVoteOpen());
            assertEquals("jury votes are cleared for the review vote", null,
                    scenario.fopState.getJuryMemberDecision()[0]);
            voteReviewReferees(scenario, true, true, true);
            voteJury(scenario, true, true, true);
            assertEquals("six votes: nothing decided", FOPState.BREAK, scenario.fopState.getState());
            assertTrue("votes stay hidden until all seven are in", listener.awaitUpdate(u -> u.getVoted()[5]));
            assertFalse(listener.updates.stream().anyMatch(u -> u.isComplete()));

            voteJury(scenario, false, false, false, false);
            // 3 referees white + 4 jury red: No Lift is the majority, but the last juror changes to white
            voteJury(scenario, true, true, true, false);
            assertTrue("all seven votes revealed together", listener.awaitUpdate(UIEvent.PanelVoteUpdate::isComplete));
            awaitState(scenario, FOPState.DECISION_VISIBLE);
            assertFalse(scenario.fopState.isPanelVoteOpen());
            assertTrue(listener.awaitVerdict(true));
            assertFalse("panel decision is not marked as entered by the Chair", listener.verdicts.get(0).isChairEntered());
            assertFalse("the panel decision is shown without waiting for the Speaker",
                    listener.verdicts.get(0).isWaitForAnnouncer());
            assertTrue(scenario.fopState.getAthleteUnderReview().getActualLift(1) > 0);
            // then the break ends by itself
            awaitState(scenario, FOPState.CURRENT_ATHLETE_DISPLAYED);
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void reviewVoteNoLiftWaitsForTheChairRule() throws Exception {
        DecisionScenario scenario = startPanelReview();
        PanelListener listener = new PanelListener();
        scenario.fopState.getUiEventBus().register(listener);
        try {
            // the Referee call button of the jury requests the new decision
            scenario.fopState.fopEventPost(new FOPEvent.SummonReferee(new Object(), 0));
            assertTrue(scenario.fopState.isPanelVoteOpen());
            voteReviewReferees(scenario, false, false, true);
            voteJury(scenario, false, false, false, false);
            Thread.sleep(2_000);
            assertEquals("a No Lift needs the rule from the Chair", FOPState.BREAK, scenario.fopState.getState());

            Athlete athlete = scenario.fopState.getAthleteUnderReview();
            scenario.fopState.fopEventPost(new FOPEvent.JuryDecision(athlete, new Object(), false, true, 1));
            assertEquals(FOPState.DECISION_VISIBLE, scenario.fopState.getState());
            assertTrue(athlete.getActualLift(1) < 0);
            assertTrue(listener.awaitVerdict(false));
            assertFalse(listener.verdicts.get(0).isChairEntered());
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void chairOverrideIsMarked() throws Exception {
        DecisionScenario scenario = startPanelReview();
        PanelListener listener = new PanelListener();
        scenario.fopState.getUiEventBus().register(listener);
        try {
            scenario.fopState.fopEventPost(new FOPEvent.PanelVoteCalled(new Object()));
            voteReviewReferees(scenario, false);
            Athlete athlete = scenario.fopState.getAthleteUnderReview();
            scenario.fopState.fopEventPost(new FOPEvent.JuryDecision(athlete, new Object(), false, true, 2));
            assertFalse("the vote is closed", scenario.fopState.isPanelVoteOpen());
            assertTrue(listener.awaitVerdict(false));
            assertTrue("Jury Chair Entered Decision", listener.verdicts.get(0).isChairEntered());
            assertTrue("the Speaker must announce a decision entered by the Chair",
                    listener.verdicts.get(0).isWaitForAnnouncer());
            assertEquals(FOPState.BREAK, scenario.fopState.getState());

            // the Speaker announces it
            scenario.fopState.fopEventPost(new FOPEvent.JuryDecision(athlete, new Object(), false, false));
            assertEquals(FOPState.DECISION_VISIBLE, scenario.fopState.getState());
            assertTrue(athlete.getActualLift(1) < 0);
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void endingTheBreakDiscardsAnIncompleteReviewVote() throws Exception {
        DecisionScenario scenario = startPanelReview();
        try {
            scenario.fopState.fopEventPost(new FOPEvent.PanelVoteCalled(new Object()));
            voteReviewReferees(scenario, false, false, false);
            scenario.fopState.fopEventPost(new FOPEvent.StartLifting(new Object()));
            assertFalse(scenario.fopState.isPanelVoteOpen());
            assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, scenario.fopState.getState());
            assertTrue("the lift keeps the referees' decision",
                    scenario.fopState.getAthleteUnderReview().getActualLift(1) > 0);
        } finally {
            endPanelScenario();
        }
    }

    @Test
    public void refereeVotesInBreakAreRefusedWithoutAReviewVote() throws Exception {
        DecisionScenario scenario = startPanelReview();
        try {
            voteReviewReferees(scenario, false);
            assertEquals(null, scenario.fopState.getReviewRefereeDecision()[0]);
            assertEquals(FOPState.BREAK, scenario.fopState.getState());
        } finally {
            endPanelScenario();
        }
    }

    /** a good lift by the referees, then four red jury votes: automatic Adjudication Panel Review */
    private DecisionScenario startPanelReview() throws Exception {
        DecisionScenario scenario = startPanelScenario(false);
        enterInitialGoodDecision(scenario);
        assertFinalDecision(scenario, true);
        assertReset(scenario);
        voteJury(scenario, false, false, false, false);
        assertEquals(FOPState.BREAK, scenario.fopState.getState());
        assertTrue(scenario.fopState.isPanelReview());
        return scenario;
    }

    private void voteReviewReferees(DecisionScenario scenario, Boolean... votes) {
        for (int index = 0; index < votes.length; index++) {
            scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(new Object(), index, votes[index]));
        }
    }

    public static final class PanelListener {
        final List<UIEvent.PanelVoteUpdate> updates = new CopyOnWriteArrayList<>();
        final List<UIEvent.JuryNotification> verdicts = new CopyOnWriteArrayList<>();

        @Subscribe
        public void panelVoteUpdate(UIEvent.PanelVoteUpdate e) {
            this.updates.add(e);
        }

        @Subscribe
        public void juryNotification(UIEvent.JuryNotification e) {
            if (e.getDeliberationEventType() == JuryDeliberationEventType.GOOD_LIFT
                    || e.getDeliberationEventType() == JuryDeliberationEventType.BAD_LIFT) {
                this.verdicts.add(e);
            }
        }

        boolean awaitUpdate(Predicate<UIEvent.PanelVoteUpdate> condition) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 2_000;
            while (System.currentTimeMillis() < deadline) {
                if (this.updates.stream().anyMatch(condition)) {
                    return true;
                }
                Thread.sleep(20);
            }
            return false;
        }

        boolean awaitVerdict(boolean goodLift) throws InterruptedException {
            JuryDeliberationEventType expected = goodLift ? JuryDeliberationEventType.GOOD_LIFT
                    : JuryDeliberationEventType.BAD_LIFT;
            long deadline = System.currentTimeMillis() + 3_000;
            while (System.currentTimeMillis() < deadline) {
                if (!this.verdicts.isEmpty()) {
                    return this.verdicts.get(0).getDeliberationEventType() == expected;
                }
                Thread.sleep(20);
            }
            return false;
        }
    }

    private DecisionScenario startPanelScenario(boolean juryDecisionsOnAttemptBoard) {
        DecisionScenario scenario = startScenario(false);
        setSevenPersonAdjudication(true);
        Competition.getCurrent().setVideoPlaybackTechnology(true);
        // default: jury decisions entered by the Chair must be announced by the Speaker
        Competition.getCurrent().setAnnouncerControlledJuryDecision(true);
        setJuryDecisionsOnAttemptBoard(juryDecisionsOnAttemptBoard);
        scenario.fopState.setJuryDecisionTimingForTests(2_000, 800);
        return scenario;
    }

    private void endPanelScenario() {
        setSevenPersonAdjudication(false);
        setJuryDecisionsOnAttemptBoard(false);
        Competition.getCurrent().setVideoPlaybackTechnology(false);
        Competition.getCurrent().setAnnouncerControlledJuryDecision(true);
    }

    private void voteJury(DecisionScenario scenario, Boolean... votes) {
        for (int index = 0; index < votes.length; index++) {
            scenario.fopState.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(new Object(), index, votes[index]));
        }
    }

    private void awaitState(DecisionScenario scenario, FOPState expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (scenario.fopState.getState() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(expected, scenario.fopState.getState());
    }

    public static final class PendingListener {
        private final CountDownLatch pending = new CountDownLatch(1);
        private final CountDownLatch done = new CountDownLatch(1);

        @Subscribe
        public void juryVotesPending(UIEvent.JuryVotesPending e) {
            (e.isPending() ? this.pending : this.done).countDown();
        }

        boolean awaitPending(boolean isPending) throws InterruptedException {
            return (isPending ? this.pending : this.done).await(2, TimeUnit.SECONDS);
        }
    }

    private void configureJuryTiming(DecisionScenario scenario) {
        setJuryDecisionsOnAttemptBoard(true);
        scenario.fopState.setJurySize(3);
        scenario.fopState.setJuryDecisionTimingForTests(2_000, 800);
    }

    private void completeJury(DecisionScenario scenario) {
        for (int index = 0; index < 3; index++) {
            scenario.fopState.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(this.juryOrigins[index], index, true));
        }
        for (int index = 0; index < 3; index++) {
            assertEquals("juror vote was not received", Boolean.TRUE, scenario.fopState.getJuryMemberDecision()[index]);
        }
    }

    private DecisionScenario startScenario(boolean showDecisionsImmediately) {
        setShowDecisionsImmediately(showDecisionsImmediately);
        FieldOfPlay fopState = MockFieldOfPlay.create(this.athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        fopState.setDecisionTimingForTests(MINIMUM_DOWN_SIGNAL_VISIBLE_MS, REVERSAL_DELAY_MS,
                DECISION_VISIBLE_DURATION_MS, DECISION_INPUT_IGNORE_WINDOW_MS, RECORD_NOTIFICATION_DELAY_MS);
        OwlcmsSession.setFop(fopState);
        prepLiftingState(fopState);
        // jury size is stored on the platform; this one is never persisted
        fopState.setPlatform(new Platform());
        fopState.setJurySize(0);
        fopState.setTestingMode(false);

        EventTimeline timeline = new EventTimeline();
        MockDecisionRenderer renderer = new MockDecisionRenderer(timeline);
        DecisionElementState decisionState = new DecisionElementState(renderer);
        decisionState.setFop(fopState);
        fopState.getUiEventBus().register(timeline);
        fopState.getUiEventBus().register(decisionState);

        Athlete curLifter = fopState.getCurAthlete();
        assertEquals(0, curLifter.getAttemptsDone().intValue());
        return new DecisionScenario(fopState, decisionState, renderer, timeline, curLifter);
    }

    private void enterInitialGoodDecision(DecisionScenario scenario) throws Exception {
        scenario.fopState.fopEventPost(new FOPEvent.TimeStarted(this));
        assertEquals(FOPState.TIME_RUNNING, scenario.fopState.getState());
        Thread.sleep(100);
        scenario.fopState.fopEventPost(new FOPEvent.TimeStopped(this));
        scenario.timeline.resetBase("FOP TIME_STOPPED");
        assertEquals(FOPState.TIME_STOPPED, scenario.fopState.getState());

        Thread.sleep(100);
        scenario.timeline.mark("FOP ref1 white");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, true));
        Thread.sleep(100);
        scenario.timeline.mark("FOP ref2 white");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, true));
        assertTrue("down signal was not rendered", scenario.renderer.awaitDown());
        assertEquals(DisplayMode.DOWN, scenario.decisionState.snapshot().mode());

        Thread.sleep(300);
        scenario.timeline.mark("FOP ref3 white");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, true));
    }

    private void enterInitialGoodDecisionWithoutClock(DecisionScenario scenario) throws Exception {
        scenario.timeline.resetBase("FOP CLOCK_NOT_STARTED");
        assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, scenario.fopState.getState());

        Thread.sleep(100);
        scenario.timeline.mark("FOP ref1 white");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, true));
        assertEquals(FOPState.TIME_STOPPED, scenario.fopState.getState());

        Thread.sleep(100);
        scenario.timeline.mark("FOP ref2 white");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, true));
        assertTrue("down signal was not rendered", scenario.renderer.awaitDown());
        assertEquals(DisplayMode.DOWN, scenario.decisionState.snapshot().mode());

        Thread.sleep(300);
        scenario.timeline.mark("FOP ref3 white");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, true));
    }

    private void assertStillDownAndUncommitted(DecisionScenario scenario, String message) {
        assertFalse("decision element must not render InitialDecision",
                scenario.renderer.eventNames().contains("InitialDecision"));
        assertEquals(DisplayMode.DOWN, scenario.decisionState.snapshot().mode());
        assertEquals(message, 0, scenario.curLifter.getAttemptsDone().intValue());
    }

    private void assertFinalDecision(DecisionScenario scenario, boolean goodLift) throws Exception {
        assertTrue("final decision was not rendered", scenario.renderer.awaitFinalDecision());
        assertTrue("final decision UI event was not fired", scenario.timeline.awaitDecision());
        assertEquals(DisplayMode.DECISION, scenario.decisionState.snapshot().mode());
        assertEquals(Boolean.valueOf(goodLift), scenario.decisionState.snapshot().decision());
        assertEquals(FOPState.DECISION_VISIBLE, scenario.fopState.getState());
        assertCommitted(scenario, goodLift);
    }

    private void assertFinalDecisionVisibleButUncommitted(DecisionScenario scenario, boolean goodLift)
            throws Exception {
        assertTrue("final decision was not rendered after minimum down-signal time",
                scenario.renderer.awaitFinalDecision());
        assertTrue("final decision UI event was not fired", scenario.timeline.awaitDecision());
        assertEquals(DisplayMode.DECISION, scenario.decisionState.snapshot().mode());
        assertEquals(Boolean.valueOf(goodLift), scenario.decisionState.snapshot().decision());
        assertEquals(FOPState.DECISION_VISIBLE, scenario.fopState.getState());
        assertEquals("visible immediate decision must not commit before reversal timer", 0,
                scenario.curLifter.getAttemptsDone().intValue());
    }

    private void assertReversedDecisionVisibleButUncommitted(DecisionScenario scenario) throws Exception {
        assertTrue("final decision was not rendered after minimum down-signal time",
                scenario.renderer.awaitFinalDecision());
        assertReversedDecisionWasEmittedAndRendered(scenario);
        assertEquals(FOPState.DECISION_VISIBLE, scenario.fopState.getState());
        assertEquals("visible immediate decision must not commit before reversal timer", 0,
                scenario.curLifter.getAttemptsDone().intValue());
    }

    private void assertReversedDecisionWasEmittedAndRendered(DecisionScenario scenario) throws Exception {
        assertTrue("FOP did not emit reversed Decision with ref1=false, ref2=false, ref3=true",
                scenario.timeline.awaitDecision(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE));
        assertTrue("decision element did not render reversed Decision with ref1=false, ref2=false, ref3=true",
                scenario.renderer.awaitDecision(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE));
        assertEquals(Boolean.FALSE, scenario.decisionState.snapshot().decision());
        assertEquals(Boolean.FALSE, scenario.decisionState.snapshot().ref1());
        assertEquals(Boolean.FALSE, scenario.decisionState.snapshot().ref2());
        assertEquals(Boolean.TRUE, scenario.decisionState.snapshot().ref3());
    }

    private void assertCommitted(DecisionScenario scenario, boolean goodLift) throws Exception {
        assertTrue("reversal commit did not complete", scenario.awaitCommit());
        scenario.timeline.mark("FOP commit observed");
        assertEquals("final decision should commit the lift", 1,
                scenario.fopState.getAthleteUnderReview().getAttemptsDone().intValue());
        assertEquals(Boolean.valueOf(goodLift), scenario.fopState.getGoodLift());
    }

    private void assertReset(DecisionScenario scenario) throws Exception {
        assertTrue("decision reset was not rendered", scenario.renderer.awaitReset());
        assertTrue("decision reset UI event was not fired", scenario.timeline.awaitDecisionReset());
        assertTrue("FOP did not finish decision reset", scenario.awaitState(FOPState.CURRENT_ATHLETE_DISPLAYED));
        assertEquals(DisplayMode.RESET, scenario.decisionState.snapshot().mode());
        assertTrue(scenario.renderer.eventNames().contains("DownSignal"));
        assertFalse(scenario.renderer.eventNames().contains("InitialDecision"));
        assertTrue(scenario.renderer.eventNames().contains("Decision"));
        assertTrue(scenario.renderer.eventNames().contains("DecisionReset"));
    }

    private void reverseToBadBeforeDecisionVisible(DecisionScenario scenario) {
        scenario.timeline.mark("FOP reversal ref1 red");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, false));
        scenario.timeline.mark("FOP reversal ref2 red");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, false));
    }

    private void reverseToBadAfterDecisionVisible(DecisionScenario scenario) {
        scenario.timeline.mark("FOP reversal ref1 red");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, false));
        scenario.timeline.mark("FOP reversal ref2 red");
        scenario.fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, false));
    }

    private void assertDelayedTiming(DecisionScenario scenario) {
        scenario.timeline.assertElapsedAtLeast("UI InitialDecision", "UI Decision", REVERSAL_DELAY_MS,
                "delayed Decision must wait for reversal delay after InitialDecision");
        scenario.timeline.assertElapsedAtLeast("UI Decision", "UI DecisionReset", DECISION_VISIBLE_DURATION_MS,
                "DecisionReset must wait for decision visible duration");
    }

    private void assertImmediateTiming(DecisionScenario scenario) {
        scenario.timeline.assertElapsedAtLeast("UI DownSignal", "UI Decision", MINIMUM_DOWN_SIGNAL_VISIBLE_MS,
                "immediate Decision must wait for minimum down-signal duration");
        scenario.timeline.assertElapsedAtMost("UI InitialDecision", "UI Decision",
                MINIMUM_DOWN_SIGNAL_VISIBLE_MS,
                "immediate Decision must be based on remaining DOWN time, not full reversal delay");
        scenario.timeline.assertElapsedAtLeast("UI InitialDecision", "FOP commit observed", REVERSAL_DELAY_MS,
                "immediate commit must wait for reversal delay after InitialDecision");
        scenario.timeline.assertElapsedAtLeast("UI Decision", "UI DecisionReset", DECISION_VISIBLE_DURATION_MS,
                "DecisionReset must wait for decision visible duration");
    }

    private void prepLiftingState(FieldOfPlay fopState) {
        fopState.testBefore();
        fopState.loadGroup(groupA, this, true);
        fopState.testStartLifting(groupA, fopState);
        this.athletes = fopState.getDisplayOrder();
        Athlete first = this.athletes.get(0);
        Athlete second = this.athletes.get(1);

        JPAService.runInTransaction(em -> {
            AthleteSorter.testAssignStartNumbers(this.athletes);
            first.setSnatch1Declaration(Integer.toString(60));
            second.setSnatch1Declaration(Integer.toString(60));
            first.setCleanJerk1Declaration(Integer.toString(80));
            second.setCleanJerk1Declaration(Integer.toString(82));
            em.merge(first);
            em.merge(second);

            AthleteSorter.liftingOrder(this.athletes);
            for (int i = 2; i < this.athletes.size(); i++) {
                Athlete athlete = this.athletes.get(i);
                athlete.setGroup(null);
                em.merge(athlete);
            }
            em.flush();
            return null;
        });
        fopState.loadGroup(groupA, this, true);
        fopState.fopEventPost(new FOPEvent.SwitchGroup(groupA, this));
        fopState.fopEventPost(new FOPEvent.StartLifting(this));
    }

    private void setShowDecisionsImmediately(boolean enabled) {
        Config config = Config.getCurrent();
        config.setFeatureSwitchValue(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, enabled);
        Config.setCurrent(config);
    }

    private void setSevenPersonAdjudication(boolean enabled) {
        Config config = Config.getCurrent();
        config.setFeatureSwitchValue(FeatureSwitch.SEVEN_PERSON_ADJUDICATION, enabled);
        Config.setCurrent(config);
    }

    private void setJuryDecisionsOnAttemptBoard(boolean enabled) {
        Config config = Config.getCurrent();
        config.setFeatureSwitchValue(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, enabled);
        Config.setCurrent(config);
    }

    private static final class EventTimeline {
        private final CountDownLatch downSignal = new CountDownLatch(1);
        private final CountDownLatch initialDecision = new CountDownLatch(1);
        private final CountDownLatch decision = new CountDownLatch(1);
        private final CountDownLatch decisionReset = new CountDownLatch(1);
        private final List<DecisionSample> decisions = new CopyOnWriteArrayList<>();
        private final List<String> entries = new CopyOnWriteArrayList<>();
        private final List<String> names = new CopyOnWriteArrayList<>();
        private final Map<String, Long> timesByName = new ConcurrentHashMap<>();
        private volatile long baseNanos = System.nanoTime();

        @Subscribe
        public void slaveDownSignal(UIEvent.DownSignal e) {
            mark("UI DownSignal");
            this.downSignal.countDown();
        }

        @Subscribe
        public void slaveInitialDecision(UIEvent.InitialDecision e) {
            mark("UI InitialDecision");
            this.initialDecision.countDown();
        }

        @Subscribe
        public void slaveDecision(UIEvent.Decision e) {
            mark("UI Decision");
            this.decisions.add(new DecisionSample(e.decision, e.ref1, e.ref2, e.ref3));
            this.decision.countDown();
        }

        @Subscribe
        public void slaveDecisionReset(UIEvent.DecisionReset e) {
            mark("UI DecisionReset");
            this.decisionReset.countDown();
        }

        void assertElapsedAtLeast(String start, String end, long expectedMs, String message) {
            long actualMs = elapsed(start, end);
            assertTrue(message + ": expected >= " + expectedMs + "ms, actual=" + actualMs + "ms",
                    actualMs + TIMING_TOLERANCE_MS >= expectedMs);
        }

        void assertElapsedAtMost(String start, String end, long maximumMs, String message) {
            long actualMs = elapsed(start, end);
            assertTrue(message + ": expected <= " + maximumMs + "ms, actual=" + actualMs + "ms",
                    actualMs <= maximumMs + TIMING_TOLERANCE_MS);
        }

        boolean awaitDecision() throws InterruptedException {
            return this.decision.await(5, TimeUnit.SECONDS);
        }

        boolean awaitDecision(Boolean expectedDecision, Boolean expectedRef1, Boolean expectedRef2,
                Boolean expectedRef3) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 1_000;
            while (System.currentTimeMillis() < deadline) {
                for (DecisionSample decisionSample : this.decisions) {
                    if (decisionSample.matches(expectedDecision, expectedRef1, expectedRef2, expectedRef3)) {
                        return true;
                    }
                }
                Thread.sleep(25);
            }
            return false;
        }

        boolean awaitDecisionReset() throws InterruptedException {
            return this.decisionReset.await(5, TimeUnit.SECONDS);
        }

        boolean awaitInitialDecision() throws InterruptedException {
            return this.initialDecision.await(1, TimeUnit.SECONDS);
        }

        void log(String label) {
            String summary = "DecisionElementStateTest timeline [" + label + "]: " + String.join(" | ", this.entries);
            timelineSummaries.add(summary);
            logger.info(summary);
        }

        void mark(String name) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.baseNanos);
            this.timesByName.putIfAbsent(name, elapsedMs);
            this.names.add(name);
            this.entries.add(elapsedMs + "ms " + name);
        }

        void resetBase(String name) {
            this.baseNanos = System.nanoTime();
            this.decisions.clear();
            this.timesByName.clear();
            this.entries.clear();
            this.names.clear();
            mark(name);
        }

        private long elapsed(String start, String end) {
            Long startMs = this.timesByName.get(start);
            Long endMs = this.timesByName.get(end);
            assertTrue("missing timeline entry " + start, startMs != null);
            assertTrue("missing timeline entry " + end, endMs != null);
            return endMs.longValue() - startMs.longValue();
        }
    }

    private record DecisionSample(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
        private boolean matches(Boolean expectedDecision, Boolean expectedRef1, Boolean expectedRef2,
                Boolean expectedRef3) {
            return expectedDecision.equals(this.decision)
                    && expectedRef1.equals(this.ref1)
                    && expectedRef2.equals(this.ref2)
                    && expectedRef3.equals(this.ref3);
        }
    }

    private static final class DecisionScenario {
        private final FieldOfPlay fopState;
        private final DecisionElementState decisionState;
        private final MockDecisionRenderer renderer;
        private final EventTimeline timeline;
        private final Athlete curLifter;

        private DecisionScenario(FieldOfPlay fopState, DecisionElementState decisionState,
                MockDecisionRenderer renderer,
                EventTimeline timeline, Athlete curLifter) {
            this.fopState = fopState;
            this.decisionState = decisionState;
            this.renderer = renderer;
            this.timeline = timeline;
            this.curLifter = curLifter;
        }

        private boolean awaitCommit() throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5_000;
            while (System.currentTimeMillis() < deadline) {
                Athlete athleteUnderReview = this.fopState.getAthleteUnderReview();
                if (athleteUnderReview != null && athleteUnderReview.getAttemptsDone() != null
                        && athleteUnderReview.getAttemptsDone().intValue() > 0) {
                    return true;
                }
                Thread.sleep(25);
            }
            return false;
        }

        private boolean awaitState(FOPState expectedState) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5_000;
            while (System.currentTimeMillis() < deadline) {
                if (expectedState == this.fopState.getState()) {
                    return true;
                }
                Thread.sleep(25);
            }
            return false;
        }
    }

    private static final class MockDecisionRenderer implements IDecisionRenderer {
        private final EventTimeline timeline;
        private final CountDownLatch down = new CountDownLatch(1);
        private final CountDownLatch finalDecision = new CountDownLatch(1);
        private final CountDownLatch resetAfterFinalDecision = new CountDownLatch(1);
        private final List<DecisionSample> decisionValues = new CopyOnWriteArrayList<>();
        private final List<String> eventNames = new CopyOnWriteArrayList<>();
        private volatile boolean finalDecisionSeen;
        private List<String> juryLights = List.of();
        private int juryRenderCount;
        private long juryGeneration;
        private long resetGeneration = -1;

        private MockDecisionRenderer(EventTimeline timeline) {
            this.timeline = timeline;
        }

        @Override
        public void showJuryLights(UIEvent event, List<String> lights, long generation) {
            this.juryLights = lights;
            this.juryGeneration = generation;
            this.juryRenderCount++;
        }

        @Override
        public void resetDecisionDisplay(UIEvent event, long generation) {
            this.resetGeneration = generation;
            this.eventNames.add(event.getClass().getSimpleName());
            this.timeline.mark("render reset");
            if (this.finalDecisionSeen) {
                this.resetAfterFinalDecision.countDown();
            }
        }

        @Override
        public void setEnabled(UIEvent event, boolean enabled) {
            this.eventNames.add(event.getClass().getSimpleName());
        }

        @Override
        public void showDecisionLights(UIEvent event, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
                boolean singleLight, boolean announcerForced) {
            this.eventNames.add(event.getClass().getSimpleName());
            if (event instanceof UIEvent.Decision) {
                this.decisionValues.add(new DecisionSample(decision, ref1, ref2, ref3));
                this.timeline.mark("render decision " + decision);
                this.finalDecisionSeen = true;
                this.finalDecision.countDown();
            }
        }

        @Override
        public void showDownSignal(UIEvent.DownSignal event, boolean silent) {
            this.eventNames.add(event.getClass().getSimpleName());
            this.timeline.mark("render DOWN");
            this.down.countDown();
        }

        boolean awaitDown() throws InterruptedException {
            return this.down.await(1, TimeUnit.SECONDS);
        }

        boolean awaitFinalDecision() throws InterruptedException {
            return awaitFinalDecision(5_000);
        }

        boolean awaitFinalDecision(long timeoutMs) throws InterruptedException {
            return this.finalDecision.await(timeoutMs, TimeUnit.MILLISECONDS);
        }

        boolean awaitDecision(Boolean expectedDecision, Boolean expectedRef1, Boolean expectedRef2,
                Boolean expectedRef3) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 1_000;
            while (System.currentTimeMillis() < deadline) {
                for (DecisionSample decisionSample : this.decisionValues) {
                    if (decisionSample.matches(expectedDecision, expectedRef1, expectedRef2, expectedRef3)) {
                        return true;
                    }
                }
                Thread.sleep(25);
            }
            return false;
        }

        boolean awaitReset() throws InterruptedException {
            return this.resetAfterFinalDecision.await(5, TimeUnit.SECONDS);
        }

        List<String> eventNames() {
            return this.eventNames;
        }
    }
}