/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

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
import app.owlcms.data.athlete.RuleViolationException;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
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
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    final Logger logger = (Logger) LoggerFactory.getLogger(MovingDownTest.class);

    @Test
    public void cleanJerkCheckAttemptNumber() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        // dummy snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR);

        // get updated allAthletes as they are in database
        groupAthletes = fopState.getDisplayOrder();
        schneiderF = groupAthletes.get(0);
        simpsonR = groupAthletes.get(1);
        allisonR = groupAthletes.get(2);

        // clean&jerk start
        schneiderF = change1(schneiderF, "70", fopBus);
        allisonR = change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 63 for second attempt
        simpsonR = declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR gets automatic progression 63 attemot 2
        allisonR = declaration(allisonR, "65", fopBus);

        // back to Simpson for 63 as second lift
        simpsonR = successfulLift(simpsonR, fopBus, fopState);

        // schneiderF wants to move down. cannot because a 63 was done as second lift
        final Athlete s = schneiderF;
        testChange(() -> change1(s, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        final Athlete a = allisonR;
        testChange(() -> change1(a, "63", fopBus), logger, null);
    }

    @Test
    public void cleanJerkCheckAttemptNumberWithClock() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        // dummy snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR);

        // get updated allAthletes as they are in database
        groupAthletes = fopState.getDisplayOrder();
        schneiderF = groupAthletes.get(0);
        simpsonR = groupAthletes.get(1);
        allisonR = groupAthletes.get(2);

        // clean&jerk start
        schneiderF = change1(schneiderF, "70", fopBus);
        change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 63 for second attempt
        simpsonR = declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR gets automatic progression 63, moves up for attempt 2
        allisonR = declaration(allisonR, "65", fopBus);

        // back to Simpson for 63 as second lift, starts time
        Athlete check = fopState.getCurAthlete();
        assertEquals(simpsonR, check);
        fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time

        // schneiderF wants to move down. cannot because clock owner is running with 63
        final Athlete s = schneiderF;
        testChange(() -> change1(s, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        final Athlete a = allisonR;
        testChange(() -> change1(a, "63", fopBus), logger, null);
    }

    @Test
    public void cleanJerkCheckProgression() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        // dummy snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR);

        // get updated allAthletes as they are in database
        groupAthletes = fopState.getDisplayOrder();
        schneiderF = groupAthletes.get(0);
        simpsonR = groupAthletes.get(1);
        allisonR = groupAthletes.get(2);

        // clean&jerk start
        simpsonR = change1(simpsonR, "62", fopBus);
        allisonR = change1(allisonR, "62", fopBus);

        // schneiderF successful at 60
        schneiderF = successfulLift(schneiderF, fopBus, fopState);
        // schneiderF declares 70 for second attempt
        schneiderF = declaration(schneiderF, "70", fopBus);

        // simpsonR succeeds at 62 first lift
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 65
        simpsonR = declaration(simpsonR, "65", fopBus);

        // allisonR succeeds at 62 first lift
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR declares 66
        allisonR = declaration(allisonR, "66", fopBus);

        // back to simpsonR for 65 as second lift
        // simpsonR attempts 65
        simpsonR = successfulLift(simpsonR, fopBus, fopState);

        // schneiderF wants to move down. cannot take the same weight
        // he lifted first, cannot manipulate the lifting order to end up lifting the same weight after
        final Athlete s = schneiderF;
        testChange(() -> change1(s, "65", fopBus), logger, RuleViolationException.LiftedEarlier.class);

        // but allisonR can move down because he lifted after
        final Athlete a = allisonR;
        testChange(() -> change1(a, "65", fopBus), logger, null);
    }

    @Test
    public void cleanJerkCheckProgression2() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepSnatchCheckProgression(fopState, 4);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        Athlete verneU = groupAthletes.get(3);

        // fill up snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR, verneU);

        // update from database
        groupAthletes = fopState.getDisplayOrder();
        schneiderF = groupAthletes.get(0);
        simpsonR = groupAthletes.get(1);
        allisonR = groupAthletes.get(2);
        verneU = groupAthletes.get(3);

        // start clean&Jerk
        // get schneider out of the way
        schneiderF = declaration(schneiderF, "100", fopBus);

        // allisonR successful at 75
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR declares 77 for second attempt
        allisonR = declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        allisonR = failedLift(allisonR, fopBus, fopState);
        // allisonR declares 80 for third attempt
        allisonR = declaration(allisonR, "80", fopBus);

        // simpsonR succeeds at 80 first lift
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 83
        simpsonR = declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        allisonR = successfulLift(allisonR, fopBus, fopState);

        // verneU fails first at 81
        // fails and keeps same
        verneU = failedLift(verneU, fopBus, fopState);

        // verneU is on second lift, but has not started clock.
        // simpsonR wants to move down to 81. He is on his second lift
        // Change is acceptable, and simpsonR even becomes next lifter because he lifted first
        final Athlete s = simpsonR;
        testChange(() -> change1(s, "81", fopBus), logger, null);

        Athlete check = fopState.getCurAthlete();
        assertEquals(simpsonR, check);
    }

    @Test
    public void cleanJerkCheckProgression3() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepSnatchCheckProgression(fopState, 4);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        Athlete verneU = groupAthletes.get(3);

        // fill up snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR, verneU);

        // update from database
        groupAthletes = fopState.getDisplayOrder();
        schneiderF = groupAthletes.get(0);
        simpsonR = groupAthletes.get(1);
        allisonR = groupAthletes.get(2);
        verneU = groupAthletes.get(3);

        // start clean&Jerk
        // get schneider out of the way
        schneiderF = declaration(schneiderF, "100", fopBus);

        // allisonR successful at 75
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR declares 77 for second attempt
        allisonR = declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        allisonR = failedLift(allisonR, fopBus, fopState);
        // allisonR declares 80 for third attempt
        allisonR = declaration(allisonR, "80", fopBus);

        // simpsonR fails at 80 first lift
        simpsonR = failedLift(simpsonR, fopBus, fopState);
        // simpsonR declares 83
        simpsonR = declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        allisonR = successfulLift(allisonR, fopBus, fopState);

        // verneU succeeds first at 81
        verneU = successfulLift(verneU, fopBus, fopState);

        // simpsonR wants to move down. ok because he ends up lifting first, before Verne
        // since he lifted earlier
        final Athlete s = simpsonR;
        testChange(() -> change1(s, "81", fopBus), logger, null);

        Athlete check = fopState.getCurAthlete();
        assertEquals(simpsonR, check);
    }

    @Test
    public void cleanJerkCheckStartNumber() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        // dummy snatch
        doSnatch(fopState, fopBus, schneiderF, simpsonR, allisonR);

        // get updated allAthletes as they are in database
        groupAthletes = fopState.getDisplayOrder();
        schneiderF = groupAthletes.get(0);
        simpsonR = groupAthletes.get(1);
        allisonR = groupAthletes.get(2);

        // clean&jerk startd
        schneiderF = change1(schneiderF, "64", fopBus);
        allisonR = change1(allisonR, "64", fopBus);

        // simpsonR successful at 60
        simpsonR = successfulLift(simpsonR, fopBus, fopState);

        // schneiderF cannot move back to 60 because of his start number
        final Athlete s = schneiderF;
        testChange(() -> change2(s, "60", fopBus), logger, RuleViolationException.StartNumberTooHigh.class);

        // but allison can move back to 60
        final Athlete a = allisonR;
        testChange(() -> change2(a, "60", fopBus), logger, null);
    }

    @Test
    public void initialCheck() {
        final String resName = "/initialCheck.txt";

        List<Athlete> allAthletes = AthleteRepository.findAll();
        FieldOfPlay fopState = FieldOfPlay.mockFieldOfPlay(allAthletes, new MockCountdownTimer(), new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        AthleteSorter.displayOrder(allAthletes);
        AthleteSorter.assignStartNumbers(allAthletes);

        Collections.shuffle(allAthletes);

        List<Athlete> sorted = AthleteSorter.liftingOrderCopy(allAthletes);
        final String actual = DebugUtils.shortDump(sorted);
        assertEqualsToReferenceFile(resName, actual);
    }

//    @Before
//    public void setupTest() {
//        JPAService.runInTransaction((em) -> {
//            TestData.deleteAllLifters(em);
//            return null;
//        });
//        TestData.insertInitialData(5, true);
//        AthleteRepository.resetCategories();
//    }

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
    }

    @Test
    public void snatchCheckAttemptNumber() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);

        // competition start
        schneiderF = change1(schneiderF, "70", fopBus);
        allisonR = change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 63 for second attempt
        simpsonR = declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR gets automatic progression 63 attemot 2
        allisonR = declaration(allisonR, "65", fopBus);

        // back to Simpson for 63 as second lift
        simpsonR = successfulLift(simpsonR, fopBus, fopState);

        // schneiderF wants to move down. cannot because a 63 was done as second lift
        final Athlete sc = schneiderF;
        testChange(() -> change1(sc, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        final Athlete al = allisonR;
        testChange(() -> change1(al, "63", fopBus), logger, null);
    }

    @Test
    public void snatchCheckAttemptNumberWithClock() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);
        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);

        // competition start
        schneiderF = change1(schneiderF, "70", fopBus);
        allisonR = change1(allisonR, "62", fopBus);

        // simpsonR successful at 60
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 63 for second attempt
        simpsonR = declaration(simpsonR, "63", fopBus);

        // allisonR succeeds at 62 first lift
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR gets automatic progression 63, moves up for attempt 2
        allisonR = declaration(allisonR, "65", fopBus);

        // back to Simpson for 63 as second lift
        Athlete check = fopState.getCurAthlete();
        assertEquals(simpsonR, check);
        fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time

        // schneiderF wants to move down. cannot because clock owner is running with 63
        final Athlete sc = schneiderF;
        testChange(() -> change1(sc, "63", fopBus), logger, RuleViolationException.AttemptNumberTooLow.class);

        // but allisonR can change his mind and go back to his automatic progression.
        final Athlete al = allisonR;
        testChange(() -> change1(al, "63", fopBus), logger, null);
    }

    @Test
    public void snatchCheckProgression() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);

        simpsonR = change1(simpsonR, "62", fopBus);
        allisonR = change1(allisonR, "62", fopBus);

        // schneiderF successful at 60
        schneiderF = successfulLift(schneiderF, fopBus, fopState);
        // schneiderF declares 70 for second attempt
        schneiderF = declaration(schneiderF, "70", fopBus);

        // simpsonR succeeds at 62 first lift
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 65
        simpsonR = declaration(simpsonR, "65", fopBus);

        // allisonR succeeds at 62 first lift
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR declares 66
        allisonR = declaration(allisonR, "66", fopBus);

        // back to simpsonR for 65 as second lift
        // simpsonR attempts 65
        simpsonR = successfulLift(simpsonR, fopBus, fopState);

        // schneiderF wants to move down. cannot take the same weight
        // he lifted first, cannot manipulate the lifting order to end up lifting the same weight after
        final Athlete sc = schneiderF;
        testChange(() -> change1(sc, "65", fopBus), logger, RuleViolationException.LiftedEarlier.class);

        // but allisonR can move down because he lifted after
        final Athlete al = allisonR;
        testChange(() -> change1(al, "65", fopBus), logger, null);
    }

    @Test
    public void snatchCheckProgression2() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepSnatchCheckProgression(fopState, 4);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        // final Athlete schneiderF = allAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        Athlete verneU = groupAthletes.get(3);

        // allisonR successful at 75
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR declares 77 for second attempt
        allisonR = declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        allisonR = failedLift(allisonR, fopBus, fopState);
        // allisonR declares 80 for third attempt
        allisonR = declaration(allisonR, "80", fopBus);

        // simpsonR succeeds at 80 first lift
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 83
        simpsonR = declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        allisonR = successfulLift(allisonR, fopBus, fopState);

        // verneU fails first at 81
        verneU = failedLift(verneU, fopBus, fopState);

        // verneU is on second lift, but has not started clock.
        // simpsonR wants to move down to 81. He is on his second lift
        // Change is acceptable, and simpsonR even becomes next lifter because he lifted first
        final Athlete s = simpsonR;
        testChange(() -> change1(s, "81", fopBus), logger, null);

        Athlete check = fopState.getCurAthlete();
        assertEquals(simpsonR, check);
    }

    @Test
    public void snatchCheckProgression3() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepSnatchCheckProgression(fopState, 4);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        // final Athlete schneiderF = allAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);
        Athlete verneU = groupAthletes.get(3);

        // allisonR successful at 75
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR declares 77 for second attempt
        allisonR = declaration(allisonR, "77", fopBus);

        // allisonR fails at 77
        allisonR = failedLift(allisonR, fopBus, fopState);
        // allisonR declares 80 for third attempt
        allisonR = declaration(allisonR, "80", fopBus);

        // simpsonR fails at 80 first lift
        simpsonR = failedLift(simpsonR, fopBus, fopState);
        // simpsonR declares 83
        simpsonR = declaration(simpsonR, "83", fopBus);

        // back to allisonR at 80 who succeeds third attempt
        allisonR = successfulLift(allisonR, fopBus, fopState);

        // verneU succeeds first attempt at 81
        verneU = successfulLift(verneU, fopBus, fopState);

        // simpsonR wants to move down. ok because he is still before Verne since he lifted earlier
        final Athlete s = simpsonR;
        testChange(() -> change1(s, "81", fopBus), logger, null);

        Athlete check = fopState.getCurAthlete();
        assertEquals(simpsonR, check);
    }

    @Test
    public void snatchCheckProgression4() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepSnatchCheckProgression(fopState, 3);

        List<Athlete> groupAthletes = fopState.getDisplayOrder();
        Athlete schneiderF = groupAthletes.get(0);
        Athlete simpsonR = groupAthletes.get(1);
        Athlete allisonR = groupAthletes.get(2);

        // changes
        schneiderF = declaration(schneiderF, "85", fopBus);
        simpsonR = declaration(simpsonR, "90", fopBus);
        allisonR = declaration(allisonR, "80", fopBus);

        // allisonR successful at 80
        allisonR = successfulLift(allisonR, fopBus, fopState);
        // allisonR declares 90 for second attempt
        allisonR = declaration(allisonR, "90", fopBus);

        // schneiderF succeeds 85 first lift
        schneiderF = successfulLift(schneiderF, fopBus, fopState);
        // simpsonR declares 89
        schneiderF = declaration(schneiderF, "89", fopBus);

        // schneiderF succeeds 89 second lift
        schneiderF = successfulLift(schneiderF, fopBus, fopState);
        // schneiderF declares 95
        schneiderF = declaration(schneiderF, "95", fopBus);

        // simpsonR succeeds at 90 first lift
        simpsonR = successfulLift(simpsonR, fopBus, fopState);
        // simpsonR declares 91
        simpsonR = declaration(simpsonR, "91", fopBus);

        // allisonR successful at 90 second lift
        allisonR = successfulLift(allisonR, fopBus, fopState);

        // schneiderF declares 91 for third attempt
        schneiderF = declaration(schneiderF, "91", fopBus);

        // back to simpson at 91
        Athlete check = fopState.getCurAthlete();
        assertEquals(simpsonR, check);
        fopBus.post(new FOPEvent.TimeStarted(null)); // this starts logical time

        // schneiderF wants to move down. cannot take the same weight
        // should be allowed because does not affect the lifting order
        final Athlete sc = schneiderF;
        testChange(() -> change1(sc, "91", fopBus), logger, null);
    }

    @Test
    public void snatchCheckStartNumber() {
        FieldOfPlay fopState = emptyFieldOfPlay();
        EventBus fopBus = testPrepAllSame(fopState, 3);

        List<Athlete> athletes = fopState.getDisplayOrder();
        Athlete schneiderF = athletes.get(0);
        Athlete simpsonR = athletes.get(1);
        Athlete allisonR = athletes.get(2);

        // competition start
        schneiderF = change1(schneiderF, "64", fopBus);
        allisonR = change1(allisonR, "64", fopBus);

        // simpsonR successful at 60
        simpsonR = successfulLift(simpsonR, fopBus, fopState);

        // schneiderF cannot move back to 60 because of his start number
        final Athlete sc = schneiderF;
        testChange(() -> change2(sc, "60", fopBus), logger, RuleViolationException.StartNumberTooHigh.class);

        // but allison can move back to 60
        final Athlete al = allisonR;
        testChange(() -> change2(al, "60", fopBus), logger, null);
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
            logger.info("*** attempt {} declaration for athlete {}: {}", attempt, lifter, weight);
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

    private void doIdenticalWeighIn(FieldOfPlay fopState, EntityManager em, int nbAthletes, final Athlete... athletes) {
        // weigh-in
        int i = 0;
        for (Athlete a : athletes) {
            if (i >= athletes.length - 1) {
                return;
            }
            a.setSnatch1Declaration(Integer.toString(60));
            a.setCleanJerk1Declaration(Integer.toString(60));
            em.merge(a);
            i++;
        }
    }

    /**
     * Fill the snatch section of the scoreboard. All successful lifts
     *
     * @param fopState
     * @param fopBus
     * @param allAthletes
     */
    private void doSnatch(FieldOfPlay fopState, EventBus fopBus, final Athlete... athletes) {
        for (int i = 0; i < (athletes.length * 3); i++) {
            Athlete curAthlete = fopState.getCurAthlete();
            // curAthlete is expected, by definition
            successfulLift(curAthlete, fopBus, fopState);
        }
    }

    private FieldOfPlay emptyFieldOfPlay() {
        return FieldOfPlay.mockFieldOfPlay(new ArrayList<Athlete>(), new MockCountdownTimer(), new MockCountdownTimer());
    }

    private Athlete failedLift(Athlete expected, EventBus fopBus, FieldOfPlay fopState) {
        Athlete curLifter = fopState.getCurAthlete();
        assertEquals(expected, curLifter);
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
        // hide non-allAthletes
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

    private EventBus testPrepAllSame(FieldOfPlay fopState, int nbAthletes) {
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);

        fopState.testBefore();
        fopState.loadGroup(gA, this, true);
        List<Athlete> groupAthletes = fopState.getDisplayOrder();

        // weigh-in
        JPAService.runInTransaction(em -> {
            AthleteSorter.assignStartNumbers(groupAthletes);
            final Athlete schneiderF = groupAthletes.get(0);
            final Athlete simpsonR = groupAthletes.get(1);
            final Athlete allisonR = groupAthletes.get(2);
            final Athlete verneU = groupAthletes.get(3);
            doIdenticalWeighIn(fopState, em, nbAthletes, schneiderF, simpsonR, allisonR, verneU);
            keepOnly(groupAthletes, nbAthletes, em);

            em.flush();
            return null;
        });
        fopState.loadGroup(gA, this, true);
        return fopBus;
    }

    private EventBus testPrepSnatchCheckProgression(FieldOfPlay fopState, int nbAthletes) {
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LoggerLevel);
        EventBus fopBus = fopState.getFopEventBus();

        logger.setLevel(Level.INFO);

        fopState.testBefore();
        fopState.loadGroup(gA, this, true);
        List<Athlete> groupAthletes = fopState.getDisplayOrder();

        // weigh-in
        JPAService.runInTransaction(em -> {
            AthleteSorter.assignStartNumbers(groupAthletes);
            final Athlete schneiderF = groupAthletes.get(0);
            final Athlete simpsonR = groupAthletes.get(1);
            final Athlete allisonR = groupAthletes.get(2);
            final Athlete verneU = groupAthletes.get(3);
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

            keepOnly(groupAthletes, nbAthletes, em);

            em.flush();
            return null;
        });
        fopState.loadGroup(gA, this, true);
        return fopBus;
    }

    private Athlete successfulLift(Athlete expected, EventBus fopBus, FieldOfPlay fopState) {
        Athlete curLifter = fopState.getCurAthlete();
        assertEquals(expected, curLifter);
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
            logger.info("{}{}", OwlcmsSession.getFopLoggingName(), message);
            assertEquals(expectedException, e.getClass());
        } finally {
            if (expectedException != null && !thrown) {
                fail("no exception was thrown, expected " + expectedException.getSimpleName());
            }
        }
    }

}
