/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
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
import ch.qos.logback.classic.Level;

/**
 * A forced same-group SwitchGroup must behave like an unload followed by a reload: whatever the state, the FOP ends
 * in INACTIVE with the group reloaded, and nothing scheduled before the reload may show, commit or reset a
 * decision afterwards.
 */
public class ForcedReloadTest {

    // loadGroup ignores a reload requested less than 300ms after the previous one (unless in testing mode)
    private static final long LOAD_DEBOUNCE_MS = 350;
    private static final long STATE_WAIT_MS = 3_000;

    private static Group groupA;
    private List<Athlete> athletes;

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
        setShowDecisionsImmediately(false);
    }

    @After
    public void restoreConfig() {
        setShowDecisionsImmediately(false);
    }

    @Test
    public void forcedReloadFromTimeRunning() {
        FieldOfPlay fop = startFop(true, false, 50, 200, 1_000);
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        assertEquals(FOPState.TIME_RUNNING, fop.getState());
        assertTrue(fop.getAthleteTimer().isRunning());

        postForcedReload(fop);

        assertReloadedAndInactive(fop);
    }

    @Test
    public void forcedReloadFromTimeStopped() {
        FieldOfPlay fop = startFop(true, false, 50, 200, 1_000);
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        fop.fopEventPost(new FOPEvent.TimeStopped(this));
        assertEquals(FOPState.TIME_STOPPED, fop.getState());

        postForcedReload(fop);

        assertReloadedAndInactive(fop);
    }

    @Test
    public void forcedReloadFromDownSignalVisible() {
        FieldOfPlay fop = startFop(true, false, 50, 200, 1_000);
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        fop.fopEventPost(new FOPEvent.TimeStopped(this));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, true));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, true));
        assertEquals(FOPState.DOWN_SIGNAL_VISIBLE, fop.getState());

        postForcedReload(fop);

        assertReloadedAndInactive(fop);
    }

    @Test
    public void forcedReloadFromDecisionVisible() throws Exception {
        FieldOfPlay fop = startFop(false, false, 50, 200, 5_000);
        enterThreeWhiteDecisions(fop);
        assertTrue("decision never became visible", awaitState(fop, FOPState.DECISION_VISIBLE));
        Thread.sleep(LOAD_DEBOUNCE_MS);

        postForcedReload(fop);

        assertReloadedAndInactive(fop);
    }

    @Test
    public void pendingDelayedDecisionDoesNotFireAfterForcedReload() throws Exception {
        // delayed mode: the decision is displayed and committed only after the reversal delay
        FieldOfPlay fop = startFop(false, false, 50, 1_500, 1_000);
        enterThreeWhiteDecisions(fop);
        assertEquals(FOPState.DOWN_SIGNAL_VISIBLE, fop.getState());
        Thread.sleep(LOAD_DEBOUNCE_MS);

        postForcedReload(fop);
        assertReloadedAndInactive(fop);

        Thread.sleep(2_200);
        assertReloadedAndInactive(fop);
        assertEquals("a stale decision recorded a lift", 0, totalAttemptsDone());
    }

    @Test
    public void pendingImmediateCommitDoesNotFireAfterForcedReload() throws Exception {
        // immediate mode: the decision becomes visible quickly, the commit is still pending
        FieldOfPlay fop = startFop(false, true, 50, 1_500, 1_000);
        enterThreeWhiteDecisions(fop);
        assertTrue("decision never became visible", awaitState(fop, FOPState.DECISION_VISIBLE));
        assertEquals("immediate decision must not be committed yet", 0, totalAttemptsDone());
        Thread.sleep(LOAD_DEBOUNCE_MS);

        postForcedReload(fop);
        assertReloadedAndInactive(fop);

        Thread.sleep(2_200);
        assertReloadedAndInactive(fop);
        assertEquals("a stale commit recorded a lift", 0, totalAttemptsDone());
    }

    @Test
    public void pendingImmediateDisplayAndCommitDoNotFireAfterForcedReload() throws Exception {
        // immediate mode, reload while the down signal is still within its minimum display time:
        // both the display and the commit are pending
        FieldOfPlay fop = startFop(false, true, 1_000, 1_500, 1_000);
        enterThreeWhiteDecisions(fop);
        assertEquals(FOPState.DOWN_SIGNAL_VISIBLE, fop.getState());
        Thread.sleep(LOAD_DEBOUNCE_MS);

        postForcedReload(fop);
        assertReloadedAndInactive(fop);

        Thread.sleep(2_200);
        assertReloadedAndInactive(fop);
        assertEquals("a stale commit recorded a lift", 0, totalAttemptsDone());
    }

    @Test
    public void ordinaryReloadInCurrentAthleteDisplayedIsUnchanged() {
        FieldOfPlay fop = startFop(true, false, 50, 200, 1_000);
        assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, fop.getState());

        fop.fopEventPost(new FOPEvent.SwitchGroup(groupA, this));

        assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, fop.getState());
        assertNotNull(fop.getGroup());
        assertEquals(groupA.getName(), fop.getGroup().getName());
        assertFalse(fop.getLiftingOrder().isEmpty());
    }

    @Test
    public void unloadThenReloadStillWorks() {
        FieldOfPlay fop = startFop(true, false, 50, 200, 1_000);

        fop.fopEventPost(new FOPEvent.SwitchGroup(null, this));
        assertEquals(FOPState.INACTIVE, fop.getState());
        assertNull(fop.getGroup());

        fop.fopEventPost(new FOPEvent.SwitchGroup(groupA, this));
        assertNotNull(fop.getGroup());
        assertEquals(groupA.getName(), fop.getGroup().getName());
        assertFalse(fop.getLiftingOrder().isEmpty());
    }

    @Test
    public void ordinaryThenForcedSwitchGroupAreNotDuplicates() {
        FOPEvent.SwitchGroup ordinary = new FOPEvent.SwitchGroup(groupA, this);
        FOPEvent.SwitchGroup forced = new FOPEvent.SwitchGroup(groupA, this, true);
        assertFalse(ordinary.isForced());
        assertTrue(forced.isForced());
        assertNotEquals(ordinary, forced);
        assertNotEquals(ordinary.hashCode(), forced.hashCode());

        FieldOfPlay fop = startFop(true, false, 50, 200, 1_000);
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        assertEquals(FOPState.TIME_RUNNING, fop.getState());

        fop.fopEventPost(new FOPEvent.SwitchGroup(groupA, this));
        postForcedReload(fop);

        assertReloadedAndInactive(fop);
    }

    private void assertReloadedAndInactive(FieldOfPlay fop) {
        assertEquals(FOPState.INACTIVE, fop.getState());
        assertNotNull(fop.getGroup());
        assertEquals(groupA.getName(), fop.getGroup().getName());
        assertFalse(fop.getLiftingOrder().isEmpty());
        assertFalse("athlete timer must be stopped", fop.getAthleteTimer().isRunning());
        for (Boolean decision : fop.getRefereeDecision()) {
            assertNull("referee decisions must be cleared", decision);
        }
    }

    private void postForcedReload(FieldOfPlay fop) {
        fop.fopEventPost(new FOPEvent.SwitchGroup(groupA, this, true));
    }

    private void enterThreeWhiteDecisions(FieldOfPlay fop) {
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        fop.fopEventPost(new FOPEvent.TimeStopped(this));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, true));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, true));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, true));
    }

    private boolean awaitState(FieldOfPlay fop, FOPState expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + STATE_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (fop.getState() == expected) {
                return true;
            }
            Thread.sleep(20);
        }
        return fop.getState() == expected;
    }

    private int totalAttemptsDone() {
        return AthleteRepository.findAll().stream().mapToInt(a -> a.getAttemptsDone()).sum();
    }

    /**
     * Creates a FOP in CURRENT_ATHLETE_DISPLAYED. In testing mode the delay timers run synchronously; otherwise
     * real timers are used so that pending tasks actually exist.
     */
    private FieldOfPlay startFop(boolean testingMode, boolean showDecisionsImmediately, long minimumDownSignalMs,
            int reversalDelayMs, long decisionVisibleMs) {
        setShowDecisionsImmediately(showDecisionsImmediately);
        FieldOfPlay fop = MockFieldOfPlay.create(this.athletes, new MockCountdownTimer(), new MockCountdownTimer());
        fop.setDecisionTimingForTests(minimumDownSignalMs, reversalDelayMs, decisionVisibleMs, 100, 0);
        OwlcmsSession.setFop(fop);
        fop.getLogger().setLevel(Level.INFO);
        prepLiftingState(fop);
        // jury size is stored on the platform; this one is never persisted
        fop.setPlatform(new Platform());
        fop.setJurySize(0);
        fop.setTestingMode(testingMode);
        return fop;
    }

    private void prepLiftingState(FieldOfPlay fop) {
        fop.testBefore();
        fop.loadGroup(groupA, this, true);
        fop.testStartLifting(groupA, fop);
        this.athletes = fop.getDisplayOrder();
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

            // hide non-athletes from Group
            AthleteSorter.liftingOrder(this.athletes);
            for (int i = 2; i < this.athletes.size(); i++) {
                Athlete athlete = this.athletes.get(i);
                athlete.setGroup(null);
                em.merge(athlete);
            }
            em.flush();
            return null;
        });
        fop.loadGroup(groupA, this, true);
        fop.fopEventPost(new FOPEvent.SwitchGroup(groupA, this));
        fop.fopEventPost(new FOPEvent.StartLifting(this));
    }

    private void setShowDecisionsImmediately(boolean enabled) {
        Config config = Config.getCurrent();
        config.setFeatureSwitchValue(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, enabled);
        Config.setCurrent(config);
    }
}
