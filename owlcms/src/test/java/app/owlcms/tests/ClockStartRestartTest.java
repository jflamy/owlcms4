/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.Main;
import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class ClockStartRestartTest {
    private static Group gA;
    private static Group gB;
    private static Group gC;

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

    final Logger logger = (Logger) LoggerFactory.getLogger(ClockStartRestartTest.class);
    private List<Athlete> athletes;

    public List<Athlete> getAthletes() {
        return athletes;
    }

    @Test
    public void initialCheck() {
        final String resName = "/initialCheck.txt";
        AthleteSorter.displayOrder(athletes);
        AthleteSorter.testAssignStartNumbers(athletes);

        Collections.shuffle(athletes);

        List<Athlete> sorted = AthleteSorter.liftingOrderCopy(athletes);
        final String actual = DebugUtils.shortDump(sorted);
        assertEqualsToReferenceFile(resName, actual);
    }

    @Test
    public void liftSequence1() {
        FieldOfPlay fopState = OwlcmsSession.getFop();
        @SuppressWarnings("unused")
        EventBus fopBus = fopState.getFopEventBus();
        logger.setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger(Athlete.class)).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger(MockCountdownTimer.class)).setLevel(Level.DEBUG);
        testPrepState(fopState, fopState.getFopEventBus(), logger);
        Group group = fopState.getGroup();
        fopState.fopEventPost(new FOPEvent.SwitchGroup(group, this));
        fopState.fopEventPost(new FOPEvent.StartLifting(this));

        athletes = fopState.getDisplayOrder();
        @SuppressWarnings("unused")
        final Athlete schneiderF = athletes.get(0);
        @SuppressWarnings("unused")
        final Athlete simpsonR = athletes.get(1);

        fopState.fopEventPost(new FOPEvent.TimeStarted(null)); // this starts logical time
        assertEquals(FOPState.TIME_RUNNING, fopState.getState());
        fopState.fopEventPost(new FOPEvent.TimeStopped(null)); // this stops logical time
        assertEquals(FOPState.TIME_STOPPED, fopState.getState());
        // submit decision update for left referee (refIndex 0, decision true)
        fopState.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, true));
        fopState.fopEventPost(new FOPEvent.TimeStarted(null)); // this restarts logical time
        // assert that the decisions were reset (use getRefereeDecision())
        Boolean[] rd = fopState.getRefereeDecision();
        String rdS = rd == null ? "null" : Arrays.toString(rd);
        org.junit.Assert.assertTrue("refereeDecision[0] expected null but was: " + rdS, rd == null || rd[0] == null);
        org.junit.Assert.assertTrue("refereeDecision[1] expected null but was: " + rdS, rd == null || rd[1] == null);
        org.junit.Assert.assertTrue("refereeDecision[2] expected null but was: " + rdS, rd == null || rd[2] == null);
    }

    @Before
    public void setupTest() {
        TestData.insertInitialData(5, true);
        JPAService.runInTransaction((em) -> {
            gA = GroupRepository.doFindByName("A", em);
            gB = GroupRepository.doFindByName("B", em);
            gC = GroupRepository.doFindByName("C", em);
            TestData.deleteAllLifters(em);
            TestData.insertSampleLifters(em, 5, gA, gB, gC);
            return null;
        });
        AthleteRepository.resetParticipations(false, true);
        athletes = AthleteRepository.findAll();
        FieldOfPlay fopState = MockFieldOfPlay.create(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(Level.INFO);
        // EventBus fopBus = fopState.getFopEventBus();
    }

    private void testPrepState(FieldOfPlay fopState, EventBus fopBus, Logger logger2) {
        gA = GroupRepository.findByName("A");
        fopState.testBefore();
        fopState.loadGroup(gA, this, true);
        fopState.testStartLifting(gA, fopState);
        athletes = fopState.getDisplayOrder();
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);

        JPAService.runInTransaction(em -> {
            AthleteSorter.testAssignStartNumbers(athletes);
            // simulate initial declaration at weigh-in
            schneiderF.setSnatch1Declaration(Integer.toString(60));
            simpsonR.setSnatch1Declaration(Integer.toString(60));
            schneiderF.setCleanJerk1Declaration(Integer.toString(80));
            simpsonR.setCleanJerk1Declaration(Integer.toString(82));
            em.merge(schneiderF);
            em.merge(simpsonR);

            // hide non-athletes from Group
            AthleteSorter.liftingOrder(athletes);
            final int size = athletes.size();
            for (int i = 2; i < size; i++) {
                Athlete athlete = athletes.get(i);
                athlete.setGroup(null);
                logger.info("athlete {}, group {}", athlete, athlete.getGroup());
                em.merge(athlete);
            }
            em.flush();
            return null;
        });
        fopState.loadGroup(gA, this, true);
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
    @SuppressWarnings("unused")
    private void declaration(final Athlete lifter, final String weight, EventBus eventBus) {
        JPAService.runInTransaction(em -> {
            switch (lifter.getAttemptsDone() + 1) {
                case 1:
                    lifter.setSnatch1Declaration(weight);
                    break;
                case 2:
                    lifter.setSnatch2Declaration(weight);
                    break;
                case 3:
                    lifter.setSnatch3Declaration(weight);
                    break;
                case 4:
                    lifter.setCleanJerk1Declaration(weight);
                    break;
                case 5:
                    lifter.setCleanJerk2Declaration(weight);
                    break;
                case 6:
                    lifter.setCleanJerk3Declaration(weight);
                    break;
            }
            em.merge(lifter);
            return null;
        });
        eventBus.post(new FOPEvent.WeightChange(this, lifter, false));
    }

    @SuppressWarnings("unused")
    private void failedLift(EventBus fopBus, Athlete curLifter, FieldOfPlay fopState) {
        logger.debug("calling lifter: {}", curLifter);
        fopBus.post(new FOPEvent.TimeStarted(null));
        fopBus.post(new FOPEvent.DownSignal(null));
        fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, false, false, false, 0L, 0L, 0L, false));
        logger.debug("failed lift for {}", curLifter);
        // fopState.finalDecision(null);
        fopBus.post(new FOPEvent.DecisionReset(null));

    }

    @SuppressWarnings("unused")
    private void successfulLift(EventBus fopBus, Athlete curLifter, FieldOfPlay fopState) {
        logger.debug("calling lifter: {}", curLifter);
        fopBus.post(new FOPEvent.TimeStarted(null));
        fopBus.post(new FOPEvent.DownSignal(null));
        fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, true, true, true, 0L, 0L, 0L, false));
        logger.debug("successful lift for {}", curLifter);
        // fopState.finalDecision(null);
        fopBus.post(new FOPEvent.DecisionReset(null));
    }

}
