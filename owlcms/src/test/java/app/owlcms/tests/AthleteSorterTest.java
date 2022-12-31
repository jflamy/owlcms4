/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.WinningOrderComparator;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;

public class AthleteSorterTest {

    private static final Level LOGGER_LEVEL = Level.OFF;

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        TestData.insertInitialData(5, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    List<Athlete> athletes = null;

    @Test
    public void initialCheck() {
        final String resName = "/initialCheck.txt";
        AthleteSorter.displayOrder(athletes);
        AthleteSorter.assignStartNumbers(athletes);

        // Collections.shuffle(athletes);

        List<Athlete> sorted = AthleteSorter.liftingOrderCopy(athletes);
        final String actual = DebugUtils.shortDump(sorted);
        assertEqualsToReferenceFile(resName, actual);
    }

    @Test
    public void liftSequence2() {
        FieldOfPlay fopState = FieldOfPlay.mockFieldOfPlay(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LOGGER_LEVEL);
        // EventBus fopBus = fopState.getFopEventBus();

        AthleteSorter.displayOrder(athletes);
        AthleteSorter.assignStartNumbers(athletes);

        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);

        // hide non-athletes
        final int size = athletes.size();
        for (int i = 2; i < size; i++) {
            athletes.remove(2);
        }

        // simulate weigh-in
        schneiderF.setBodyWeight(68.0);
        simpsonR.setBodyWeight(67.9);
        schneiderF.setSnatch1Declaration(Integer.toString(70));
        simpsonR.setSnatch1Declaration(Integer.toString(60));
        schneiderF.setCleanJerk1Declaration(Integer.toString(80));
        simpsonR.setCleanJerk1Declaration(Integer.toString(80));
        AthleteSorter.liftingOrder(athletes);

        // simpson will do all his lifts first and finish first
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        // but schneider should still start first CJ (does not matter who lifted
        // first)
        assertEquals(schneiderF, athletes.get(0));
    }

    @Test
    public void medalsBodyWeight() {
        FieldOfPlay fopState = FieldOfPlay.mockFieldOfPlay(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LOGGER_LEVEL);
        // EventBus fopBus = fopState.getFopEventBus();

        AthleteSorter.displayOrder(athletes);
        AthleteSorter.assignStartNumbers(athletes);

        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);
        final Athlete allisonA = athletes.get(2);
        final Athlete verneU = athletes.get(3);

        doLifts(schneiderF, simpsonR, allisonA, verneU);

        // all athletes are done, check medals

        // all athletes have body weight = 0
        // we have two athletes at same total and same bodyweight.
        // The one who reached total *first* should win.
        // in this test sequence, the winner has bigger lot number, but still
        // wins because of earlier lift.
        Collections.sort(athletes, new WinningOrderComparator(Ranking.TOTAL, false));
        AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
        assertEqualsToReferenceFile("/seq1_medals_timeStamp.txt", DebugUtils.shortDump(athletes));

        // now we give the first two athletes different body weights (second is
        // lighter)
        athletes.get(0).setBodyWeight(68.0);
        athletes.get(1).setBodyWeight(67.9);
        athletes.get(2).setBodyWeight(68.5);
        athletes.get(3).setBodyWeight(68.4);
        // we give the lighter lifter a higher lot number, which should make him lose (there is no
        // bodyweight advantage anymore)
        athletes.get(1).setLotNumber(99);
        // and we sort again for medals.
        Collections.sort(athletes, new WinningOrderComparator(Ranking.TOTAL, false));
        AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
        assertEqualsToReferenceFile("/seq1_medals_bodyWeight.txt", DebugUtils.shortDump(athletes));
    }

    @Test
    public void medalsEarlierTotals() {
        FieldOfPlay fopState = FieldOfPlay.mockFieldOfPlay(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LOGGER_LEVEL);
        // EventBus fopBus = fopState.getFopEventBus();

        AthleteSorter.displayOrder(athletes);
        AthleteSorter.assignStartNumbers(athletes);

        final Athlete schneiderF = athletes.get(0);
        final Athlete simpsonR = athletes.get(1);
        final Athlete allisonA = athletes.get(2);
        final Athlete verneU = athletes.get(3);

        // weigh-in
        simpsonR.setBodyWeight(68.0);
        schneiderF.setBodyWeight(67.9);
        verneU.setBodyWeight(68.5);
        allisonA.setBodyWeight(68.4);

        doLifts(schneiderF, simpsonR, allisonA, verneU);

        // for same total a smaller cj breaks tie (since reached earlier)
        try {
            // the changes are illegal with respect to lifting order, we don't care
            simpsonR.setValidation(false);
            schneiderF.setValidation(false);
            schneiderF.setLotNumber(99); // legacy

            // improve snatch
            simpsonR.setSnatch3Declaration(Integer.toString(62));
            simpsonR.setSnatch3ActualLift(Integer.toString(62));
            // reduce cj by 1kg; make sure things work even if reached on 2nd attempt
            // (which is still earlier than the better first attempt of lifter finishing second.
            simpsonR.setCleanJerk1Declaration(Integer.toString(84));
            simpsonR.setCleanJerk1ActualLift(Integer.toString(-84));
            simpsonR.setCleanJerk2ActualLift(Integer.toString(84));
            simpsonR.setCleanJerk3ActualLift(Integer.toString(0));

            Collections.sort(athletes, new WinningOrderComparator(Ranking.TOTAL, false));
            AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
            assertEqualsToReferenceFile("/seq1_medals_earlierTotal.txt", DebugUtils.longDump(athletes));
        } finally {
            simpsonR.setValidation(true);
            schneiderF.setValidation(true);
            schneiderF.setLotNumber(1);
        }

        // second lifter reaches total on his first attempt, but this is still later in the lifting order
        try {
            // the changes are illegal with respect to lifting order, we don't care
            simpsonR.setValidation(false);
            schneiderF.setValidation(false);
            schneiderF.setLotNumber(99); // legacy

            // improve snatch
            simpsonR.setSnatch3Declaration(Integer.toString(62));
            simpsonR.setSnatch3ActualLift(Integer.toString(62));
            // reduce cj by 1kg; make sure things work even if reached on 2nd attempt
            // (which is still earlier than the better first attempt of lifter finishing second.
            simpsonR.setCleanJerk1Declaration(Integer.toString(84));
            simpsonR.setCleanJerk1ActualLift(Integer.toString(-84));
            simpsonR.setCleanJerk2ActualLift(Integer.toString(84));
            simpsonR.setCleanJerk3ActualLift(Integer.toString(0));

            // second lifter reaches total on his first attempt, but this is still later in the lifting order
            // than simpson.
            schneiderF.setCleanJerk1Declaration(Integer.toString(85));
            schneiderF.setCleanJerk1ActualLift(Integer.toString(85));
            schneiderF.setCleanJerk2ActualLift(Integer.toString(-86));
            schneiderF.setCleanJerk3ActualLift(Integer.toString(-86));

            Collections.sort(athletes, new WinningOrderComparator(Ranking.TOTAL, false));
            AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
            assertEqualsToReferenceFile("/seq1_medals_earlierTotal2.txt", DebugUtils.longDump(athletes));
        } finally {
            simpsonR.setValidation(true);
            schneiderF.setValidation(true);
            schneiderF.setLotNumber(1);
        }

        // now we test that for same total a smaller cj breaks tie (since reached earlier)
        try {
            // the changes are illegal with respect to lifting order, we don't care
            simpsonR.setValidation(false);
            schneiderF.setValidation(false);

            // replicate canadian masters bug
            allisonA.setEligibleForTeamRanking(false);
            verneU.setEligibleForTeamRanking(false);
            simpsonR.setLastName("Thorne");
            simpsonR.setFirstName("");
            simpsonR.setLotNumber(453);
            simpsonR.setBodyWeight(85.50D);
            schneiderF.setLastName("Campbell");
            schneiderF.setFirstName("");
            schneiderF.setLotNumber(503);
            schneiderF.setBodyWeight(81.80D);

            simpsonR.setSnatch1Declaration(Integer.toString(53));
            simpsonR.setSnatch1ActualLift(Integer.toString(53));
            simpsonR.setSnatch2Declaration(Integer.toString(55));
            simpsonR.setSnatch2ActualLift(Integer.toString(-55));
            simpsonR.setSnatch3Declaration(Integer.toString(55));
            simpsonR.setSnatch3ActualLift(Integer.toString(55));

            simpsonR.setCleanJerk1Declaration(Integer.toString(60));
            simpsonR.setCleanJerk1ActualLift(Integer.toString(60));
            simpsonR.setCleanJerk2Declaration(Integer.toString(64));
            simpsonR.setCleanJerk2ActualLift(Integer.toString(-64));
            simpsonR.setCleanJerk3Declaration(Integer.toString(64));
            simpsonR.setCleanJerk3ActualLift(Integer.toString(-64));

            // second lifter reaches total on his first attempt, but this is still later in the lifting order
            // than simpson.
            schneiderF.setSnatch1Declaration(Integer.toString(48));
            schneiderF.setSnatch1ActualLift(Integer.toString(48));
            schneiderF.setSnatch2Declaration(Integer.toString(51));
            schneiderF.setSnatch2ActualLift(Integer.toString(51));
            schneiderF.setSnatch3Declaration(Integer.toString(53));
            schneiderF.setSnatch3ActualLift(Integer.toString(-53));

            schneiderF.setCleanJerk1Declaration(Integer.toString(61));
            schneiderF.setCleanJerk1ActualLift(Integer.toString(61));
            schneiderF.setCleanJerk2Declaration(Integer.toString(64));
            schneiderF.setCleanJerk2ActualLift(Integer.toString(-64));
            schneiderF.setCleanJerk3Declaration(Integer.toString(64));
            schneiderF.setCleanJerk3ActualLift(Integer.toString(64));

            Collections.sort(athletes, new WinningOrderComparator(Ranking.TOTAL, false));
            AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
            assertEqualsToReferenceFile("/seq1_medals_earlierTotal3.txt", DebugUtils.longDump(athletes));
        } finally {
            simpsonR.setValidation(true);
            schneiderF.setValidation(true);
        }
    }

    @Before
    public void setupTest() {
        // for this test, the initial data does not include body weights, so we use false
        // on the constructor to disable exclusion of incomplete data.
        athletes = AthleteRepository.findAll();
        OwlcmsSession.withFop(fop -> fop.testBefore());
    }

    /**
     * @param lifter
     * @param lifters1
     * @param weight
     */
    private void change1(final Athlete lifter, List<Athlete> lifters1, final String weight) {
        // sleep for a while to ensure that we get different time stamps on the
        // lifts.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        switch (lifter.getAttemptsDone() + 1) {
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
        AthleteSorter.liftingOrder(lifters1);
    }

    /*************************************************************************************
     * Utility routines
     */

    /**
     * @param lifter
     * @param lifters1
     * @param weight
     */
    private void change2(final Athlete lifter, List<Athlete> lifters1, final String weight) {
        // sleep for a while to ensure that we get different time stamps on the
        // lifts.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        switch (lifter.getAttemptsDone() + 1) {
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
        AthleteSorter.liftingOrder(lifters1);
    }

    /**
     * @param lifter
     * @param lifters1
     * @param weight
     */
    private void declaration(final Athlete lifter, List<Athlete> lifters1, final String weight) {
        // sleep for a while to ensure that we get different time stamps on the
        // lifts.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
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
        AthleteSorter.liftingOrder(lifters1);
    }

    /**
     * @param lifter
     * @param lifters1
     * @param weight
     */
    private void doLift(final Athlete lifter, List<Athlete> lifters1, final String weight) {
        // sleep for a while to ensure that we get different time stamps on the
        // lifts.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        lifter.doLift(weight);
        AthleteSorter.liftingOrder(lifters1);
    }

    private void doLifts(final Athlete schneiderF, final Athlete simpsonR, final Athlete allisonA,
            final Athlete verneU) {
        // all males
        schneiderF.setGender(Gender.M);
        simpsonR.setGender(Gender.M);
        allisonA.setGender(Gender.M);
        verneU.setGender(Gender.M);

        // simulate initial declaration at weigh-in
        schneiderF.setSnatch1Declaration(Integer.toString(60));
        simpsonR.setSnatch1Declaration(Integer.toString(60));
        allisonA.setSnatch1Declaration(Integer.toString(55));
        verneU.setSnatch1Declaration(Integer.toString(55));
        schneiderF.setCleanJerk1Declaration(Integer.toString(80));
        simpsonR.setCleanJerk1Declaration(Integer.toString(82));
        allisonA.setCleanJerk1Declaration(Integer.toString(61));
        verneU.setCleanJerk1Declaration(Integer.toString(68));

        // check initial lift order -- this checks the "lot number" rule
        AthleteSorter.liftingOrder(athletes);
        assertEqualsToReferenceFile("/seq1_lift0.txt", DebugUtils.shortDump(athletes));
        // hide non-athletes
        final int size = athletes.size();
        for (int i = 4; i < size; i++) {
            athletes.remove(4);
        }

        // competition start
        successfulLift(athletes);
        successfulLift(athletes);

        // change weights to have all athletes are the same at 60
        declaration(verneU, athletes, "58");
        declaration(allisonA, athletes, "60");
        change1(verneU, athletes, "59");
        change2(verneU, athletes, "60");

        // failure so we can test "earlier lifter"
        failedLift(athletes);

        // one more failure -- we now have 3 athletes at second try, 60kg.
        failedLift(athletes);

        // get second try done
        failedLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);

        // get third try done
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        // end of snatch

        // mixed-up sequence of pass/fail/go-up
        Random rnd = new Random(0); // so the sequence is repeatable from test
                                    // to test.
        for (int i = 0; i < 16; i++) { // 16 is purely empirical, observing the
                                       // sequence of events generated
            switch (rnd.nextInt(3)) {
            case 0:
                successfulLift(athletes);
                break;
            case 1:
                failedLift(athletes);
                break;
            case 2:
                final String change = Integer.toString(2 + athletes.get(0).getNextAttemptRequestedWeight());
                // in practice, declarations can't be redone, but for this test all we care about is that
                // nextAttemptRequestedWeight has changed.
                declaration(athletes.get(0), athletes, change);
                break;
            }
        }
        // in this sequence, one lifter is already done, check that others are
        // listed below

        // proceed with competition
        successfulLift(athletes);
        successfulLift(athletes);
        successfulLift(athletes);
        failedLift(athletes);
        // two athletes are now done
        successfulLift(athletes);
        successfulLift(athletes);

    }

    /**
     * Current lifter fails.
     *
     * @param lifter
     * @param lifters1
     */
    private void failedLift(List<Athlete> lifters1) {
        final Athlete lifter = lifters1.get(0);
        final Integer nextAttemptRequestedWeight = lifter.getNextAttemptRequestedWeight();
        final String weight = Integer.toString(-nextAttemptRequestedWeight);
        doLift(lifter, lifters1, weight);
        if (lifter.getAttemptsDone() < 5) {
            assertEquals(
                    "next requested weight should be equal after failed lift", nextAttemptRequestedWeight,
                    lifter.getNextAttemptRequestedWeight());
        }
    }

    /**
     * Current lifter has successul lift
     *
     * @param lifter
     */
    private void successfulLift(List<Athlete> lifters1) {
        final Athlete lifter = lifters1.get(0);
        final String weight = Integer.toString(lifter.getNextAttemptRequestedWeight());
        doLift(lifter, lifters1, weight);
    }

}
