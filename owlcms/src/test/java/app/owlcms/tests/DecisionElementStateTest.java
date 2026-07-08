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
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
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

    private DecisionScenario startScenario(boolean showDecisionsImmediately) {
        setShowDecisionsImmediately(showDecisionsImmediately);
        FieldOfPlay fopState = MockFieldOfPlay.create(this.athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        fopState.setDecisionTimingForTests(MINIMUM_DOWN_SIGNAL_VISIBLE_MS, REVERSAL_DELAY_MS,
                DECISION_VISIBLE_DURATION_MS, DECISION_INPUT_IGNORE_WINDOW_MS, RECORD_NOTIFICATION_DELAY_MS);
        OwlcmsSession.setFop(fopState);
        prepLiftingState(fopState);
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

        private MockDecisionRenderer(EventTimeline timeline) {
            this.timeline = timeline;
        }

        @Override
        public void resetDecisionDisplay(UIEvent event, long generation) {
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