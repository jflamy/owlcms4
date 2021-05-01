/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
import app.owlcms.data.athlete.RuleViolationException;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.DebugUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class MovingDownTest {
    private static Level LoggerLevel = Level.INFO;
    private static Group gA;
    private static Group gB;
    private static Group gC;

    @BeforeClass
    public static void setupTests() {
        JPAService.init(true, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    final Logger logger = (Logger) LoggerFactory.getLogger(MovingDownTest.class);

    private List<Athlete> athletes;

    @Test
    public void initialCheck() {
        final String resName = "/initialCheck.txt";
        AthleteSorter.assignLotNumbers(athletes);
        AthleteSorter.assignStartNumbers(athletes);

        Collections.shuffle(athletes);

        List<Athlete> sorted = AthleteSorter.liftingOrderCopy(athletes);
        final String actual = DebugUtils.shortDump(sorted);
        assertEqualsToReferenceFile(resName, actual);
    }

    @Test
    public void checkStartNumber() {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);
        AthleteSorter.assignLotNumbers(athletes);
        AthleteSorter.assignStartNumbers(athletes);
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);
        final Athlete allisonR = athletes.get(2);
        keepOnly(athletes, 3);
        
        // weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(60));
        simpsonR.setSnatch1Declaration(Integer.toString(60));
        allisonR.setSnatch1Declaration(Integer.toString(60));
        schneiderF.setCleanJerk1Declaration(Integer.toString(82));
        simpsonR.setCleanJerk1Declaration(Integer.toString(82));
        allisonR.setCleanJerk1Declaration(Integer.toString(82));
        
        // competition start
        change1(schneiderF, "64", fopBus);
        change1(allisonR, "64", fopBus);
        
        // simpsonR successful at 60
        successfulLift(fopBus, simpsonR, fopState);
        
        // schneiderF cannot move back to 60 because of his start number
        testChange(() -> change2(schneiderF, "60", fopBus), logger, "RuleViolation.startNumberTooHigh");
        
        // but allison can.
        testChange(() -> change2(allisonR, "60", fopBus), logger, null);
    }
    
    
    @Test
    public void checkAttemptNumber() {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);
        AthleteSorter.assignLotNumbers(athletes);
        AthleteSorter.assignStartNumbers(athletes);
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);
        final Athlete allisonR = athletes.get(2);
        keepOnly(athletes, 3);
        
        // weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(60));
        simpsonR.setSnatch1Declaration(Integer.toString(60));
        allisonR.setSnatch1Declaration(Integer.toString(60));
        schneiderF.setCleanJerk1Declaration(Integer.toString(82));
        simpsonR.setCleanJerk1Declaration(Integer.toString(82));
        allisonR.setCleanJerk1Declaration(Integer.toString(82));
        
        // competition start
        change1(schneiderF, "70", fopBus);
        change1(allisonR, "62", fopBus);
        
        // simpsonR successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, simpsonR, fopState);
        // simpsonR declares 63 for second attempt
        declaration(simpsonR, "63", fopBus);
        
        // allisonR succeeds at 63 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // allisonR gets automatic progression 63 attemot 2
        declaration(allisonR, "65", fopBus);
        
        // back to Simpson for 63 as second lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        
        // schneiderF wants to move down. cannot because a 63 was done as second lift
        testChange(() ->  change1(schneiderF, "63", fopBus), logger, "RuleViolation.attemptNumberTooLow");
        
        // but allisonR can change his mind and go back to his automatic progression.
        testChange(() ->  change1(allisonR, "63", fopBus), logger, null);    
    }
    
    
    @Test
    public void checkAttemptNumberWithClock() {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);
        AthleteSorter.assignLotNumbers(athletes);
        AthleteSorter.assignStartNumbers(athletes);
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);
        final Athlete allisonR = athletes.get(2);
        keepOnly(athletes, 3);
        
        // weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(60));
        simpsonR.setSnatch1Declaration(Integer.toString(60));
        allisonR.setSnatch1Declaration(Integer.toString(60));
        schneiderF.setCleanJerk1Declaration(Integer.toString(82));
        simpsonR.setCleanJerk1Declaration(Integer.toString(82));
        allisonR.setCleanJerk1Declaration(Integer.toString(82));
        
        // competition start
        change1(schneiderF, "70", fopBus);
        change1(allisonR, "62", fopBus);
        
        // simpsonR successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, simpsonR, fopState);
        // simpsonR declares 63 for second attempt
        declaration(simpsonR, "63", fopBus);
        
        // allisonR succeeds at 63 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // allisonR gets automatic progression 63, moves up for attempt 2
        declaration(allisonR, "65", fopBus);
        
        // back to Simpson for 63 as second lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time

        // schneiderF wants to move down. cannot because clock owner is running with 63
        testChange(() ->  change1(schneiderF, "63", fopBus), logger, "RuleViolation.attemptNumberTooLow");
        
        // but allisonR can change his mind and go back to his automatic progression.
        testChange(() ->  change1(allisonR, "63", fopBus), logger, null);    
    }

    
    @Test
    public void checkProgresion() {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);
        AthleteSorter.assignLotNumbers(athletes);
        AthleteSorter.assignStartNumbers(athletes);
        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);
        keepOnly(athletes, 2);
        
        // weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(60));
        simpsonR.setSnatch1Declaration(Integer.toString(60));
        schneiderF.setCleanJerk1Declaration(Integer.toString(82));
        simpsonR.setCleanJerk1Declaration(Integer.toString(82));

        change1(simpsonR, "62", fopBus);
        
        // schneiderF successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(schneiderF, curAthlete);
        successfulLift(fopBus, schneiderF, fopState);
        // schneiderF declares 70 for second attempt
        declaration(schneiderF, "70", fopBus);
        
        // simpsonR succeeds at 62 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // allisonR declares 65
        declaration(simpsonR, "65", fopBus);
        
        // back to simpsonR for 65 as second lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        
        // schneiderF wants to move down. cannot take the same weight
        // he lifted first, cannot manipulate the lifting order to end up lifting the same weight after
        testChange(() ->  change1(schneiderF, "65", fopBus), logger, "RuleViolation.liftedEarlier");
    }

    private void keepOnly(List<Athlete> athletes, int endIndex) {
        // hide non-athletes
        AthleteSorter.liftingOrder(athletes);
        final int size = athletes.size();
        for (int i = endIndex; i < size; i++) {
            athletes.remove(endIndex); // always the same location is emptied
        }
    }

    @FunctionalInterface
    public interface WeightChange {
        void doChange() throws RuleViolationException;
    }
    
    private void testChange(WeightChange w, Logger logger, String expectedKey) {
        // schneider wants to come down
        String message = null;
        boolean thrown = false;
        try {
            w.doChange();
        } catch (RuleViolationException e) {
            thrown = true;
            message = e.getLocalizedMessage();
            logger.warn(message);
            assertEquals(expectedKey, expectedKey == null ? null : e.getMessageKey());
        } finally {
            if (expectedKey != null && !thrown) {
                fail("expected "+expectedKey);
            }
        }
    }

    @Before
    public void setupTest() {
        logger.setLevel(LoggerLevel);

        TestData.insertInitialData(5, true);
        JPAService.runInTransaction((em) -> {
            gA = GroupRepository.doFindByName("A", em);
            gB = GroupRepository.doFindByName("B", em);
            gC = GroupRepository.doFindByName("C", em);
            TestData.deleteAllLifters(em);
            TestData.insertSampleLifters(em, 5, gA, gB, gC);
            return null;
        });
        athletes = AthleteRepository.findAll();
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
    private void declaration(final Athlete lifter, final String weight, EventBus eventBus) {

        int attempt = lifter.getAttemptsDone() + 1;
        logger.debug("*** attempt {} declaration for athlete {}: {}", attempt, lifter, weight);
        switch (attempt) {
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
        eventBus.post(new FOPEvent.WeightChange(this, lifter));
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
    private void change1(final Athlete lifter, final String weight, EventBus eventBus) {
        int attempt = lifter.getAttemptsDone() + 1;
        logger.debug("***1 attempt {} change 1 for athlete {}: {}", attempt, lifter, weight);
        switch (attempt) {
        case 1:
            lifter.setSnatch1Change1(weight);
            break;
        case 2:
            lifter.setSnatch2Change1(weight);
            break;
        case 3:
            lifter.setSnatch3Change1(weight);
            break;
        case 4:
            lifter.setCleanJerk1Change1(weight);
            break;
        case 5:
            lifter.setCleanJerk2Change1(weight);
            break;
        case 6:
            lifter.setCleanJerk3Change1(weight);
            break;
        }
        logger.debug("***2 attempt {} change 1 for athlete {}: {} {}", attempt, lifter, weight,
                lifter.getNextAttemptRequestedWeight());
        eventBus.post(new FOPEvent.WeightChange(this, lifter));
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
    private void change2(final Athlete lifter, final String weight, EventBus eventBus) {
        int attempt = lifter.getAttemptsDone() + 1;
        logger.debug("***1 attempt {} change 2 for athlete {}: {}", attempt, lifter, weight);
        switch (attempt) {
        case 1:
            lifter.setSnatch1Change2(weight);
            break;
        case 2:
            lifter.setSnatch2Change2(weight);
            break;
        case 3:
            lifter.setSnatch3Change2(weight);
            break;
        case 4:
            lifter.setCleanJerk1Change2(weight);
            break;
        case 5:
            lifter.setCleanJerk2Change2(weight);
            break;
        case 6:
            lifter.setCleanJerk3Change2(weight);
            break;
        }
        logger.debug("***2 attempt {} change 2 for athlete {}: {} {}", attempt, lifter, weight,
                lifter.getNextAttemptRequestedWeight());
        eventBus.post(new FOPEvent.WeightChange(this, lifter));
    }

    @SuppressWarnings("unused")
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
