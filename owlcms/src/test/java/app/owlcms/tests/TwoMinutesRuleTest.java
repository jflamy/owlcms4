/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

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
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.DebugUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class TwoMinutesRuleTest {
    private static Group gA;
    private static Group gB;
    private static Group gC;

    @BeforeClass
    public static void setupTests() {
        JPAService.init(true, true);
        Config.initConfig();
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    final Logger logger = (Logger) LoggerFactory.getLogger(TwoMinutesRuleTest.class);

    private List<Athlete> athletes;

    @Test
    public void initialCheck() {
        final String resName = "/initialCheck.txt";
        AthleteSorter.displayOrder(athletes);
        AthleteSorter.assignStartNumbers(athletes);

        Collections.shuffle(athletes);

        List<Athlete> sorted = AthleteSorter.liftingOrderCopy(athletes);
        final String actual = DebugUtils.shortDump(sorted);
        assertEqualsToReferenceFile(resName, actual);
    }

    @Test
    public void liftSequence3() throws InterruptedException {
        FieldOfPlay fopState = OwlcmsSession.getFop();
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger(Athlete.class)).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger(MockCountdownTimer.class)).setLevel(Level.DEBUG);
        doSequence3(fopState, fopBus, logger);
    }

    void doSequence3(FieldOfPlay fopState, EventBus fopBus, Logger logger) {
        prepState3(fopState, fopBus, logger);
        athletes = fopState.getDisplayOrder();
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);

        // competition start
        assertEquals(60000, fopState.getTimeAllowed());
        logger.debug("(1)\n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));

        // schneiderF is called with initial weight
        Athlete curLifter = fopState.getCurAthlete();
        Athlete previousLifter = fopState.getPreviousAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(null, previousLifter);
        successfulLift(fopBus, curLifter, fopState);

        logger.debug("(2)\n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));
        // first is now simpsonR ; he has declared 60kg
        curLifter = fopState.getCurAthlete();
        previousLifter = fopState.getPreviousAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(schneiderF, previousLifter);
        assertEquals(60000, fopState.getTimeAllowed());

        // ... but simpsonR changes to 62 before being called by announcer (time not
        // restarted)
        declaration(curLifter, "62", fopBus);
        logger.debug("(3)simpson declared 62, schneider first at 61\n{}",
                DebugUtils.shortDump(fopState.getLiftingOrder()));
        // so now schneider should be back on top at 61, with two minutes because
        // there was no time started.
        curLifter = fopState.getCurAthlete();
        logger.info("curLifter = {}", curLifter);
        previousLifter = fopState.getPreviousAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(schneiderF, previousLifter);
        assertEquals(120000, fopState.getTimeAllowed());

        // schneider lifts 62
        successfulLift(fopBus, curLifter, fopState);
        logger.debug("(4) simpson now first at 62 after schneider +62\n{}",
                DebugUtils.shortDump(fopState.getLiftingOrder()));
        // is now simpson's turn, he should NOT have 2 minutes
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(60000, fopState.getTimeAllowed());

        // simpson fails
        failedLift(fopBus, curLifter, fopState);
        logger.debug("(5) simpson failed \n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));
        // still simpson because 2nd try and schneider is at 3rd.
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        // simpson is called again with two minutes
        logger.info("calling lifter: {}", curLifter);
        fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time
        assertEquals(FOPState.TIME_RUNNING, fopState.getState());

        // but simpson now asks for more; weight change should stop clock.
        declaration(curLifter, "67", fopBus);
        logger.debug("(6) simpson declared 67\n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));
        assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, fopState.getState());
        logger.info("declaration by {}: {}", curLifter, "67");
        // schneider does not get 2 minutes.
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(60000, fopState.getTimeAllowed());

        // schneider is called
        logger.debug("(6) calling lifter: {}", curLifter);
        fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time
        assertEquals(FOPState.TIME_RUNNING, fopState.getState());

        // but asks for more weight -- the following stops time.
        declaration(curLifter, "65", fopBus);
        logger.debug("(7) scheneider changes to 65 still first\n{}", DebugUtils.shortDump(fopState.getLiftingOrder()));
        assertEquals(FOPState.TIME_STOPPED, fopState.getState());
        int remainingTime = fopState.getAthleteTimer()
                .getTimeRemaining();

        // at this point, if schneider is called again, he should get the remaining
        // time.
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(remainingTime, fopState.getTimeAllowed());
    }

    public void prepState3(FieldOfPlay fopState, EventBus fopBus, Logger logger2) {
        fopState.beforeTest();
        fopState.loadGroup(gA, this, true);
        athletes = fopState.getDisplayOrder();
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);

        JPAService.runInTransaction(em -> {
            AthleteSorter.assignStartNumbers(athletes);
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
                logger.info("athlete {}, group {}",athlete,athlete.getGroup());
                em.merge(athlete);
            }
            em.flush();
            return null;
        });        
        fopState.loadGroup(gA, this, true);
    }
    
    public void prepState4(FieldOfPlay fopState, EventBus fopBus, Logger logger2) {
        fopState.beforeTest();
        fopState.loadGroup(gA, this, true);
        athletes = fopState.getDisplayOrder();
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);

        JPAService.runInTransaction(em -> {
            AthleteSorter.assignStartNumbers(athletes);
            // simulate initial declaration at weigh-in
            schneiderF.setSnatch1Declaration(Integer.toString(60));
            simpsonR.setSnatch1Declaration(Integer.toString(65));
            schneiderF.setCleanJerk1Declaration(Integer.toString(80));
            simpsonR.setCleanJerk1Declaration(Integer.toString(85));
            em.merge(schneiderF);
            em.merge(simpsonR);

            // hide non-athletes from Group
            AthleteSorter.liftingOrder(athletes);
            final int size = athletes.size();
            for (int i = 2; i < size; i++) {
                Athlete athlete = athletes.get(i);
                athlete.setGroup(null);
                logger.info("athlete {}, group {}",athlete,athlete.getGroup());
                em.merge(athlete);
            }
            em.flush();
            return null;
        });        
        fopState.loadGroup(gA, this, true);
    }

    @Test
    public void liftSequence4() throws InterruptedException {
        FieldOfPlay fopState = OwlcmsSession.getFop();
        EventBus fopBus = fopState.getFopEventBus();

        doLiftSequence4(fopState, fopBus, logger);
    }

    void doLiftSequence4(FieldOfPlay fopState, EventBus fopBus, Logger logger) {
        prepState4(fopState, fopBus, logger);
        athletes = fopState.getDisplayOrder();
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);

        // competition start
        assertEquals(60000, fopState.getTimeAllowed());

        // schneiderF snatch1
        Athlete curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        successfulLift(fopBus, curLifter, fopState);

        // schneiderF snatch2
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // schneiderF snatch3
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // simpsonR snatch1
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(60000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // simpsonR snatch2
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // simpsonR snatch3
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // schneiderF cj1
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(60000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // schneiderF cj2
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(schneiderF, fopState.getPreviousAthlete());
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // schneiderF cj3
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // simpsonR cj1
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(60000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // simpsonR cj2
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);

        // simpsonR cj3
        curLifter = fopState.getCurAthlete();
        assertEquals(simpsonR, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);
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
        AthleteRepository.resetParticipations();
        athletes = AthleteRepository.findAll();
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(Level.INFO);
        // EventBus fopBus = fopState.getFopEventBus();
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
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
            };
            em.merge(lifter);
            return null;
        });
        eventBus.post(new FOPEvent.WeightChange(this, lifter));
    }

    private void failedLift(EventBus fopBus, Athlete curLifter, FieldOfPlay fopState) {
        logger.debug("calling lifter: {}", curLifter);
        fopBus.post(new FOPEvent.TimeStarted(null));
        fopBus.post(new FOPEvent.DownSignal(null));
        fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, false, false, false, 0, 0, 0));
        logger.debug("failed lift for {}", curLifter);
//        fopState.finalDecision(null);
        fopBus.post(new FOPEvent.DecisionReset(null));

    }

    private void successfulLift(EventBus fopBus, Athlete curLifter, FieldOfPlay fopState) {
        logger.debug("calling lifter: {}", curLifter);
        fopBus.post(new FOPEvent.TimeStarted(null));
        fopBus.post(new FOPEvent.DownSignal(null));
        fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, true, true, true, 0, 0, 0));
        logger.debug("successful lift for {}", curLifter);
//        fopState.finalDecision(null);
        fopBus.post(new FOPEvent.DecisionReset(null));
    }

    public List<Athlete> getAthletes() {
        return athletes;
    }

}
