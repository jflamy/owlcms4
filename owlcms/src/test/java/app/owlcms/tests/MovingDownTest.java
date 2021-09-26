/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

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
import app.owlcms.data.config.Config;
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
    @FunctionalInterface
    public interface WeightChange {
        void doChange() throws RuleViolationException;
    }

    private static Level LoggerLevel = Level.INFO;
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

    final Logger logger = (Logger) LoggerFactory.getLogger(MovingDownTest.class);

    private List<Athlete> athletes;

    @Test
    public void cleanJerkCheckAttemptNumber() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        doWeighIn(fopState, schneiderF, simpsonR, allisonR);

        // dummy snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR);

        // clean&jerk start
        change1(schneiderF, "70", fopBus);
        change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, simpsonR, fopState);
        // simpsonR declares 63 for second attempt
        declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
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
        testChange(() -> change1(schneiderF, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        testChange(() -> change1(allisonR, "63", fopBus), logger, null);
    }

    @Test
    public void cleanJerkCheckAttemptNumberWithClock() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        doWeighIn(fopState, schneiderF, simpsonR, allisonR);

        // dummy snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR);

        // clean&jerk start
        change1(schneiderF, "70", fopBus);
        change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, simpsonR, fopState);
        // simpsonR declares 63 for second attempt
        declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
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
        testChange(() -> change1(schneiderF, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        testChange(() -> change1(allisonR, "63", fopBus), logger, null);
    }

    @Test
    public void cleanJerkCheckProgression() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        doWeighIn(fopState, schneiderF, simpsonR, allisonR);

        change1(simpsonR, "62", fopBus);
        change1(allisonR, "62", fopBus);

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
        // simpsonR declares 65
        declaration(simpsonR, "65", fopBus);

        // allisonR succeeds at 62 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // allisonR declares 66
        declaration(allisonR, "66", fopBus);

        // back to simpsonR for 65 as second lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        // simpsonR attempts 65
        successfulLift(fopBus, curAthlete, fopState);

        // schneiderF wants to move down. cannot take the same weight
        // he lifted first, cannot manipulate the lifting order to end up lifting the same weight after
        testChange(() -> change1(schneiderF, "65", fopBus), logger, RuleViolationException.LiftedEarlier.class);

        // but allisonR can move down because he lifted after
        testChange(() -> change1(allisonR, "65", fopBus), logger, null);
    }

    /*
     * ActualLiftInfo [athlete=Allison, weight=75, attemptNo=1, progression=75, startNumber=3, lotNumber=3]
     * ActualLiftInfo [athlete=Allison, weight=77, attemptNo=2, progression=2, startNumber=3, lotNumber=3]
     * ActualLiftInfo [athlete=Simpson, weight=80, attemptNo=1, progression=80, startNumber=2, lotNumber=2] Simpson
     * requests 83 ActualLiftInfo [athlete=Allison, weight=80, attemptNo=3, progression=3, startNumber=3, lotNumber=3]
     * ActualLiftInfo [athlete=Verne, weight=81, attemptNo=1, progression=81, startNumber=4, lotNumber=4] Simpson
     * requests 81 -> should be denied.
     */
    @Test
    public void cleanJerkCheckProgression2() {
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
        final Athlete verneU = athletes.get(3);
        keepOnly(athletes, 4, null);

        // weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(85));
        simpsonR.setSnatch1Declaration(Integer.toString(80));
        allisonR.setSnatch1Declaration(Integer.toString(75));
        verneU.setSnatch1Declaration(Integer.toString(81));
        schneiderF.setCleanJerk1Declaration(Integer.toString(85));
        simpsonR.setCleanJerk1Declaration(Integer.toString(80));
        allisonR.setCleanJerk1Declaration(Integer.toString(75));
        verneU.setCleanJerk1Declaration(Integer.toString(81));
        fopState.recomputeLiftingOrder();

        // fill up snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR, verneU);

        // allisonR successful at 75
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, allisonR, fopState);
        // allisonR declares 77 for second attempt
        declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        failedLift(fopBus, allisonR, fopState);
        // allisonR declares 80 for third attempt
        declaration(allisonR, "80", fopBus);

        // simpsonR succeeds at 80 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // simpsonR declares 83
        declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);

        // verneU fails first at 81
        curAthlete = fopState.getCurAthlete();
        assertEquals(verneU, curAthlete);
        // fails and keeps same
        failedLift(fopBus, curAthlete, fopState);

        // verneU is on second lift, but has not started clock.
        // simpsonR wants to move down to 81. He is on his second lift
        // Change is acceptable, and simpsonR even becomes next lifter because he lifted first
        testChange(() -> change1(simpsonR, "81", fopBus), logger, null);

        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
    }

    @Test
    public void cleanJerkCheckProgression3() {
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
        final Athlete verneU = athletes.get(3);
        keepOnly(athletes, 4, null);

        // weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(85));
        simpsonR.setSnatch1Declaration(Integer.toString(80));
        allisonR.setSnatch1Declaration(Integer.toString(75));
        verneU.setSnatch1Declaration(Integer.toString(81));
        schneiderF.setCleanJerk1Declaration(Integer.toString(85));
        simpsonR.setCleanJerk1Declaration(Integer.toString(80));
        allisonR.setCleanJerk1Declaration(Integer.toString(75));
        verneU.setCleanJerk1Declaration(Integer.toString(81));
        fopState.recomputeLiftingOrder();

        // fill up snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR, verneU);

        // start clean&jerk

        // allisonR successful at 75
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, allisonR, fopState);
        // allisonR declares 77 for second attempt
        declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        failedLift(fopBus, allisonR, fopState);
        // allisonR declares 80 for third attempt
        declaration(allisonR, "80", fopBus);

        // simpsonR fails at 80 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        failedLift(fopBus, curAthlete, fopState);
        // simpsonR declares 83
        declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);

        // verneU succeeds first at 81
        curAthlete = fopState.getCurAthlete();
        assertEquals(verneU, curAthlete);
        // fails and keeps automatic 82
        successfulLift(fopBus, curAthlete, fopState);

        // simpsonR wants to move down. ok because he ends up lifting first, before Verne
        // since he lifted earlier
        testChange(() -> change1(simpsonR, "81", fopBus), logger, null);
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
    }

    @Test
    public void snatchCheckProgression4() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(85));
        simpsonR.setSnatch1Declaration(Integer.toString(90));
        allisonR.setSnatch1Declaration(Integer.toString(80));
        fopState.recomputeLiftingOrder();

        // allisonR successful at 80
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, allisonR, fopState);
        // allisonR declares 90 for second attempt
        declaration(allisonR, "90", fopBus);

        // schneiderF succeeds 85 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(schneiderF, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // simpsonR declares 89
        declaration(schneiderF, "89", fopBus);

        // schneiderF succeeds 89 second lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(schneiderF, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // simpsonR declares 95
        declaration(schneiderF, "95", fopBus);

        // simpsonR succeeds at 90 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, curAthlete, fopState);
        // simpsonR declares 91
        declaration(simpsonR, "91", fopBus);

        // allisonR successful at 90 second lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        successfulLift(fopBus, allisonR, fopState);
        // allisonR declares 90 for third attempt
        declaration(schneiderF, "91", fopBus);

        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time

        // schneiderF wants to move down. cannot take the same weight
        // should be allowed because does not affect the lifting order
        testChange(() -> change1(schneiderF, "91", fopBus), logger, null);
    }

    @Test
    public void cleanJerkCheckStartNumber() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        doWeighIn(fopState, schneiderF, simpsonR, allisonR);

        // competition start
        change1(schneiderF, "64", fopBus);
        change1(allisonR, "64", fopBus);

        // simpsonR successful at 60
        successfulLift(fopBus, simpsonR, fopState);

        // schneiderF cannot move back to 60 because of his start number
        testChange(() -> change2(schneiderF, "60", fopBus), logger, RuleViolationException.StartNumberTooHigh.class);

        // but allison can move back to 60
        testChange(() -> change2(allisonR, "60", fopBus), logger, null);
    }

    public List<Athlete> getAthletes() {
        return athletes;
    }

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
        AthleteRepository.resetCategories();
        athletes = AthleteRepository.findAll();
        logger.warn("athletes {}", athletes);
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(Level.INFO);
        // EventBus fopBus = fopState.getFopEventBus();
    }

    @Test
    public void snatchCheckAttemptNumber() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        doWeighIn(fopState, schneiderF, simpsonR, allisonR);

        // competition start
        change1(schneiderF, "70", fopBus);
        change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, simpsonR, fopState);
        // simpsonR declares 63 for second attempt
        declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
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
        testChange(() -> change1(schneiderF, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        testChange(() -> change1(allisonR, "63", fopBus), logger, null);
    }

    @Test
    public void snatchCheckAttemptNumberWithClock() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        doWeighIn(fopState, schneiderF, simpsonR, allisonR);

        // competition start
        change1(schneiderF, "70", fopBus);
        change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        successfulLift(fopBus, simpsonR, fopState);
        // simpsonR declares 63 for second attempt
        declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
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
        testChange(() -> change1(schneiderF, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        testChange(() -> change1(allisonR, "63", fopBus), logger, null);
    }

    @Test
    public void snatchCheckProgression() {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        EventBus fopBus = prepAllSame(fopState, 3);
        
        athletes = fopState.getDisplayOrder();
        Athlete schneiderF = athletes.get(0);
        Athlete simpsonR = athletes.get(1);
        Athlete allisonR = athletes.get(2);
        // dummy snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR);
        
        // get updated athletes as they are in database
        athletes = fopState.getDisplayOrder();
        schneiderF = athletes.get(0);
        simpsonR = athletes.get(1);
        allisonR = athletes.get(2);

        // clean&jerk start
        simpsonR = change1(simpsonR, "62", fopBus);
        allisonR = change1(allisonR, "62", fopBus);

        // schneiderF successful at 60
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(schneiderF, curAthlete);
        schneiderF = successfulLift(fopBus, curAthlete, fopState);
        // schneiderF declares 70 for second attempt
        schneiderF = declaration(schneiderF, "70", fopBus);

        // simpsonR succeeds at 62 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        simpsonR = successfulLift(fopBus, curAthlete, fopState);        
        // simpsonR declares 65
        simpsonR = declaration(simpsonR, "65", fopBus);

        // allisonR succeeds at 62 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);       
        allisonR = successfulLift(fopBus, curAthlete, fopState);        
        // allisonR declares 66
        allisonR = declaration(allisonR, "66", fopBus);

        // back to simpsonR for 65 as second lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        // simpsonR attempts 65
        simpsonR = successfulLift(fopBus, curAthlete, fopState);

        // schneiderF wants to move down. cannot take the same weight
        // he lifted first, cannot manipulate the lifting order to end up lifting the same weight after
        final Athlete s = schneiderF;
        testChange(() -> change1(s, "65", fopBus), logger, RuleViolationException.LiftedEarlier.class);

        // but allisonR can move down because he lifted after
        final Athlete a = allisonR;
        testChange(() -> change1(a, "65", fopBus), logger, null);
    }

    @Test
    public void snatchCheckProgression2() {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        EventBus fopBus = prepSnatchCheckProgression(fopState, 4);

        athletes = fopState.getDisplayOrder();
        // final Athlete schneiderF = athletes.get(0);
        Athlete simpsonR = athletes.get(1);
        Athlete allisonR = athletes.get(2);
        Athlete verneU = athletes.get(3);

        // allisonR successful at 75
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        allisonR = successfulLift(fopBus, allisonR, fopState);
        // allisonR declares 77 for second attempt
        allisonR = declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        allisonR = failedLift(fopBus, allisonR, fopState);
        // allisonR declares 80 for third attempt
        allisonR = declaration(allisonR, "80", fopBus);

        // simpsonR succeeds at 80 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        simpsonR = successfulLift(fopBus, curAthlete, fopState);
        // simpsonR declares 83
        simpsonR = declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        allisonR = successfulLift(fopBus, curAthlete, fopState);

        // verneU fails first at 81
        curAthlete = fopState.getCurAthlete();
        assertEquals(verneU, curAthlete);
        // fails and keeps same
        verneU = failedLift(fopBus, curAthlete, fopState);

        // verneU is on second lift, but has not started clock.
        // simpsonR wants to move down to 81. He is on his second lift
        // Change is acceptable, and simpsonR even becomes next lifter because he lifted first
        final Athlete s = simpsonR;
        testChange(() -> change1(s, "81", fopBus), logger, null);

        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
    }

    @Test
    public void snatchCheckProgression3() {
        FieldOfPlay fopState = new FieldOfPlay(athletes, new MockCountdownTimer(), new MockCountdownTimer(), true);
        EventBus fopBus = prepSnatchCheckProgression(fopState, 4);

        athletes = fopState.getDisplayOrder();
        // final Athlete schneiderF = athletes.get(0);
        Athlete simpsonR = athletes.get(1);
        Athlete allisonR = athletes.get(2);
        Athlete verneU = athletes.get(3);

        // allisonR successful at 75
        Athlete curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        allisonR = successfulLift(fopBus, allisonR, fopState);
        // allisonR declares 77 for second attempt
        allisonR = declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        allisonR = failedLift(fopBus, allisonR, fopState);
        // allisonR declares 80 for third attempt
        allisonR = declaration(allisonR, "80", fopBus);

        // simpsonR fails at 80 first lift
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
        simpsonR = failedLift(fopBus, curAthlete, fopState);
        // simpsonR declares 83
        simpsonR = declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        curAthlete = fopState.getCurAthlete();
        assertEquals(allisonR, curAthlete);
        allisonR = successfulLift(fopBus, curAthlete, fopState);

        // verneU succeeds first attempt at 81
        curAthlete = fopState.getCurAthlete();
        assertEquals(verneU, curAthlete);
        // fails and keeps automatic 82
        verneU = successfulLift(fopBus, curAthlete, fopState);

        // simpsonR wants to move down. ok because he is still before Verne since he lifted earlier
        final Athlete s = simpsonR;
        testChange(() -> change1(s, "81", fopBus), logger, null);
        curAthlete = fopState.getCurAthlete();
        assertEquals(simpsonR, curAthlete);
    }

    private EventBus prepAllSame(FieldOfPlay fopState, int nbAthletes) {
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);

        fopState.beforeTest();
        fopState.loadGroup(gA, this, true);
        athletes = fopState.getDisplayOrder();

        // weigh-in
        JPAService.runInTransaction(em -> {
            AthleteSorter.assignStartNumbers(athletes);
            final Athlete schneiderF = athletes.get(0);
            final Athlete simpsonR = athletes.get(1);
            final Athlete allisonR = athletes.get(2);
            final Athlete verneU = athletes.get(3);
            doIdenticalWeighIn(fopState, em, nbAthletes, schneiderF, simpsonR, allisonR, verneU);
            keepOnly(athletes, nbAthletes, em);

            em.flush();
            return null;
        });
        fopState.loadGroup(gA, this, true);
        return fopBus;
    }

    private EventBus prepSnatchCheckProgression(FieldOfPlay fopState, int nbAthletes) {
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);

        fopState.beforeTest();
        fopState.loadGroup(gA, this, true);
        athletes = fopState.getDisplayOrder();

        // weigh-in
        JPAService.runInTransaction(em -> {
            AthleteSorter.assignStartNumbers(athletes);
            final Athlete schneiderF = athletes.get(0);
            final Athlete simpsonR = athletes.get(1);
            final Athlete allisonR = athletes.get(2);
            final Athlete verneU = athletes.get(3);
            schneiderF.setSnatch1Declaration(Integer.toString(85));
            simpsonR.setSnatch1Declaration(Integer.toString(80));
            allisonR.setSnatch1Declaration(Integer.toString(75));
            verneU.setSnatch1Declaration(Integer.toString(81));
            schneiderF.setCleanJerk1Declaration(Integer.toString(85));
            simpsonR.setCleanJerk1Declaration(Integer.toString(80));
            allisonR.setCleanJerk1Declaration(Integer.toString(75));
            verneU.setCleanJerk1Declaration(Integer.toString(81));
            em.merge(schneiderF);
            em.merge(simpsonR);
            em.merge(allisonR);
            em.merge(verneU);

            keepOnly(athletes, nbAthletes, em);

            em.flush();
            return null;
        });
        fopState.loadGroup(gA, this, true);
        return fopBus;
    }

    @Test
    public void snatchCheckStartNumber() {
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
        keepOnly(athletes, 3, null);

        // weigh-in
        doWeighIn(fopState, schneiderF, simpsonR, allisonR);

        // competition start
        change1(schneiderF, "64", fopBus);
        change1(allisonR, "64", fopBus);

        // simpsonR successful at 60
        successfulLift(fopBus, simpsonR, fopState);

        // schneiderF cannot move back to 60 because of his start number
        testChange(() -> change2(schneiderF, "60", fopBus), logger, RuleViolationException.StartNumberTooHigh.class);

        // but allison can move back to 60
        testChange(() -> change2(allisonR, "60", fopBus), logger, null);
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
    private Athlete change1(final Athlete lifter, final String weight, EventBus eventBus) {
        Athlete updated = JPAService.runInTransaction(em -> {
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
            return em.merge(lifter);
        });

        eventBus.post(new FOPEvent.WeightChange(this, updated));
        return updated;
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
    private Athlete change2(final Athlete lifter, final String weight, EventBus eventBus) {
        Athlete updated = JPAService.runInTransaction(em -> {
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
            return em.merge(lifter);
        });
        eventBus.post(new FOPEvent.WeightChange(this, updated));
        return updated;
    }

    /**
     * @param lifter
     * @param weight
     * @param eventBus
     */
    private Athlete declaration(final Athlete lifter, final String weight, EventBus eventBus) {
        Athlete updated = JPAService.runInTransaction(em -> {
            int attempt = lifter.getAttemptsDone() + 1;
            logger.warn("*** attempt {} declaration for athlete {}: {}", attempt, lifter, weight);
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
            return em.merge(lifter);
        });
        eventBus.post(new FOPEvent.WeightChange(this, updated));
        return updated;
    }

    /**
     * Fill the snatch section of the scoreboard. All successful lifts
     * 
     * @param fopState
     * @param fopBus
     * @param athletes
     */
    private void doSnatch(FieldOfPlay fopState, EventBus fopBus, final Athlete... athletes) {
        for (int i = 0; i < (athletes.length * 3); i++) {
            Athlete curAthlete = fopState.getCurAthlete();
            successfulLift(fopBus, curAthlete, fopState);
        }
    }

    private void doWeighIn(FieldOfPlay fopState, final Athlete... athletes) {
        JPAService.runInTransaction((em) -> {
            // weigh-in
            for (Athlete a : athletes) {
                a.setSnatch1Declaration(Integer.toString(60));
                a.setCleanJerk1Declaration(Integer.toString(60));
                em.merge(a);
            }
            return null;
        });
        fopState.recomputeLiftingOrder();
    }

    private void doIdenticalWeighIn(FieldOfPlay fopState, EntityManager em, int nbAthletes, final Athlete... athletes) {
        // weigh-in
        int i = 0;
        for (Athlete a : athletes) {
            if (i >= athletes.length-1) return;
            a.setSnatch1Declaration(Integer.toString(60));
            a.setCleanJerk1Declaration(Integer.toString(60));
            em.merge(a);
            i++;
        }
    }

    private Athlete failedLift(EventBus fopBus, Athlete curLifter, FieldOfPlay fopState) {
        return JPAService.runInTransaction((em) -> {
            logger.debug("calling lifter: {}", curLifter);
            fopBus.post(new FOPEvent.TimeStarted(null));
            fopBus.post(new FOPEvent.DownSignal(null));
            fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, false, false, false, 0, 0, 0));
            logger.debug("failed lift for {}", curLifter);
            fopBus.post(new FOPEvent.DecisionReset(null));
            return em.merge(curLifter);
        });
    }

    private void keepOnly(List<Athlete> athletes, int endIndex, EntityManager em) {
        // hide non-athletes
        AthleteSorter.liftingOrder(athletes);
        Group group = athletes.get(0).getGroup();
        final int size = athletes.size();
        for (int i = 0; i < size; i++) {
            Athlete a = athletes.get(i);
            if (i < endIndex) {
                a.setGroup(group);
            } else {
                a.setGroup(null);
            }
            em.merge(a);
        }
    }

    private Athlete successfulLift(EventBus fopBus, Athlete curLifter, FieldOfPlay fopState) {
        return JPAService.runInTransaction((em) -> {
            logger.debug("calling lifter: {}", curLifter);
            fopBus.post(new FOPEvent.TimeStarted(null));
            fopBus.post(new FOPEvent.DownSignal(null));
            fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, true, true, true, 0, 0, 0));
            logger.debug("successful lift for {}", curLifter);
            fopBus.post(new FOPEvent.DecisionReset(null));
            return em.merge(curLifter);
        });
    }

    private void testChange(WeightChange w, Logger logger, Class<? extends Exception> expectedException) {
        // schneider wants to come down
        String message = null;
        boolean thrown = false;

        try {
            w.doChange();
        } catch (RuleViolationException e) {
            thrown = true;
            message = e.getLocalizedMessage();
            logger.warn("{}{}", OwlcmsSession.getFopLoggingName(), message);
            assertEquals(expectedException, e.getClass());
        } finally {
            if (expectedException != null && !thrown) {
                fail("no exception was thrown, expected " + expectedException.getSimpleName());
            }
        }
    }

}
