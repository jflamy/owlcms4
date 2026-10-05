/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import net.sf.jxls.transformer.XLSTransformer;

import app.owlcms.Main;
import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.data.athleteSort.WinningOrderComparator;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.spreadsheet.PAthlete;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("deprecation")
public class AthleteSorterTest {

    private static final Level LOGGER_LEVEL = Level.OFF;
    Logger logger = (Logger) LoggerFactory.getLogger(AthleteSorterTest.class);

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
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
        AthleteSorter.testAssignStartNumbers(athletes);

        // Collections.shuffle(athletes);

        List<Athlete> sorted = AthleteSorter.liftingOrderCopy(athletes);
        final String actual = DebugUtils.shortDump(sorted);
        assertEqualsToReferenceFile(resName, actual);
    }

    @Test
    public void liftSequence2() {
        FieldOfPlay fopState = MockFieldOfPlay.create(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LOGGER_LEVEL);
        // EventBus fopBus = fopState.getFopEventBus();

        AthleteSorter.displayOrder(athletes);
        AthleteSorter.testAssignStartNumbers(athletes);

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
        FieldOfPlay fopState = MockFieldOfPlay.create(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LOGGER_LEVEL);
        // EventBus fopBus = fopState.getFopEventBus();

        AthleteSorter.displayOrder(athletes);
        AthleteSorter.testAssignStartNumbers(athletes);

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
        FieldOfPlay fopState = MockFieldOfPlay.create(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LOGGER_LEVEL);
        // EventBus fopBus = fopState.getFopEventBus();

        AthleteSorter.displayOrder(athletes);
        AthleteSorter.testAssignStartNumbers(athletes);

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

    @Test
    public void oocAthleteKeepsExtraBestAthleteRanks() {
        Athlete ooc = athletes.stream()
                .filter(a -> a.getCategory() != null && !a.getParticipations().isEmpty())
                .findFirst()
                .orElseThrow();
        EligibleForIndividualRankingStatus previousEligibility = ooc.getIndividualEligibilityStatus();
        ooc.setGender(Gender.M);
        Participation mainRankings = ooc.getMainRankings();
        mainRankings.setSnatchRank(1);
        mainRankings.setCleanJerkRank(1);
        mainRankings.setTotalRank(1);
        mainRankings.setCustomRank(1);
        mainRankings.setCategoryScoreRank(1);
        ooc.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.OOC_INVITED);

        EnumMap<Ranking, Boolean> previousConfig = RankingConfig.getConfig();
        try {
            assertCategoryRanksAreExtra(ooc);
            assertCategoryRanksAreExtra(new PAthlete(mainRankings));

            AthleteSorter.assignCategoryRanks(new ArrayList<>(List.of(ooc)), Ranking.SNATCH);
            AthleteSorter.assignCategoryRanks(new ArrayList<>(List.of(ooc)), Ranking.CLEANJERK);
            AthleteSorter.assignCategoryRanks(new ArrayList<>(List.of(ooc)), Ranking.TOTAL);
            AthleteSorter.assignCategoryRanks(new ArrayList<>(List.of(ooc)), Ranking.CUSTOM);
            AthleteSorter.assignCategoryRanks(new ArrayList<>(List.of(ooc)), Ranking.CATEGORY_SCORE);

            assertCategoryRanksAreExtra(ooc);

            for (Ranking ranking : RankingConfig.getAllScoringRankings()) {
                RankingConfig.setUserEnabled(ranking, true);
                PAthlete ranked = new PAthlete(ooc.getMainRankings());
                AthleteSorter.assignBestAthleteRanks(new ArrayList<>(List.of(ranked)), ranking);
                ooc.setBestAthleteRank(ranked.getBestAthleteRank());
                JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(ranking);
                assertEquals(-1, ooc.getBestLifterRank());
            }

            assertEquals(Integer.valueOf(-1), ooc.getSinclairRank());
            assertEquals(-1, ooc.getCatSinclairRank());
            assertEquals(Integer.valueOf(-1), ooc.getCatQPointsRank());
            assertEquals(Integer.valueOf(-1), ooc.getCatGAMXRank());
            assertEquals(-1, ooc.getSmhfRank());
            assertEquals(Integer.valueOf(-1), ooc.getqPointsRank());
            assertEquals(-1, ooc.getQMastersRank());
            assertEquals(Integer.valueOf(-1), ooc.getGamxRank());
            assertEquals(Integer.valueOf(-1), ooc.getGamxMRank());
            assertEquals(Integer.valueOf(-1), ooc.getGamxURank());
            assertEquals(Integer.valueOf(-1), ooc.getGamxARank());
            assertEquals(Integer.valueOf(-1), ooc.getQYouthRank());
        } finally {
            for (Ranking ranking : RankingConfig.getAllScoringRankings()) {
                RankingConfig.setUserEnabled(ranking, previousConfig.getOrDefault(ranking, false));
            }
            ooc.setIndividualEligibilityStatus(previousEligibility);
            JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(null);
        }
    }

    @Test
    public void pAthleteBestLifterRankUsesReportRowRank() {
        Athlete athlete = athletes.stream()
                .filter(a -> a.getCategory() != null && !a.getParticipations().isEmpty())
                .findFirst()
                .orElseThrow();
        Participation participation = athlete.getMainRankings();
        PAthlete pAthlete = new PAthlete(participation);

        athlete.setBestAthleteRank(77);
        pAthlete.setBestAthleteRank(3);

        try {
            JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(Ranking.BW_SINCLAIR);
            assertEquals(3, pAthlete.getBestLifterRank());
            assertEquals(Integer.valueOf(77), athlete.getSinclairRank());
        } finally {
            JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(null);
        }
    }

    @Test
    public void pAthleteBestLifterRankUsesReportRowRankForAllScoringSystems() {
        Athlete athlete = athletes.stream()
                .filter(a -> a.getCategory() != null && !a.getParticipations().isEmpty())
                .findFirst()
                .orElseThrow();
        EligibleForIndividualRankingStatus previousEligibility = athlete.getIndividualEligibilityStatus();
        athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.ELIGIBLE);
        Participation participation = athlete.getMainRankings();
        EnumMap<Ranking, Boolean> previousConfig = RankingConfig.getConfig();
        Set<Ranking> rankingsWithoutStoredRank = EnumSet.of(Ranking.GAMX_MS, Ranking.GAMX_MC, Ranking.GAMX_S,
                Ranking.GAMX_C);
        try {
            for (Ranking ranking : RankingConfig.getAllScoringRankings()) {
                if (rankingsWithoutStoredRank.contains(ranking)) {
                    continue;
                }
                RankingConfig.setUserEnabled(ranking, true);
                JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(ranking);

                athlete.setBestAthleteRank(2);

                PAthlete reportRow = new PAthlete(participation);
                reportRow.setBestAthleteRank(1);

                assertEquals(ranking.name(), 1, reportRow.getBestLifterRank());
                assertEquals(ranking.name(), 2, athlete.getBestLifterRank());
                assertEquals(ranking.name(), 2, new PAthlete(participation).getBestLifterRank());
            }
        } finally {
            for (Ranking ranking : RankingConfig.getAllScoringRankings()) {
                RankingConfig.setUserEnabled(ranking, previousConfig.getOrDefault(ranking, false));
            }
            athlete.setIndividualEligibilityStatus(previousEligibility);
            JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(null);
        }
    }

    @Test
    public void bestAthleteAccessorsAndLegacyAliasesUseParticipation() {
        Athlete athlete = athletes.get(0);
        athlete.getBestAthleteRank();
        Participation original = athlete.getMainRankings();
        athlete.setBestAthleteRank(7);

        PAthlete reporting = PAthlete.copyForReporting(athlete);
        assertNotSame(original, reporting.getMainRankings());
        assertEquals(7, reporting.getBestAthleteRank());
        reporting.setBestAthleteRank(2);
        assertEquals(2, reporting.getMainRankings().getBestAthleteRank());
        assertEquals(7, athlete.getBestAthleteRank());
        assertEquals(Integer.valueOf(7), athlete.getSinclairRank());
        assertEquals(Integer.valueOf(2), reporting.getSinclairRank());
        assertEquals(Integer.valueOf(2), reporting.getGamxMRank());
        assertEquals(2, reporting.getQMastersRank());

        PAthlete anotherReport = PAthlete.copyForReporting(reporting);
        assertEquals(2, anotherReport.getBestAthleteRank());
        anotherReport.setBestAthleteRank(1);
        assertEquals(2, reporting.getBestAthleteRank());
        assertEquals(7, original.getBestAthleteRank());

        PAthlete legacyWrapper = new PAthlete(athlete);
        assertSame(original, legacyWrapper.getMainRankings());
        PAthlete safeReport = PAthlete.copyForReporting(legacyWrapper);
        safeReport.setBestAthleteRank(4);
        assertEquals(7, original.getBestAthleteRank());
    }

    @Test
    public void bestAthleteReportingDeduplicatesWithoutChangingSourceRanks() {
        Athlete first = prepareBestAthlete(0, Gender.M, 100, 120);
        Athlete tied = prepareBestAthlete(1, Gender.M, 100, 120);
        Athlete third = prepareBestAthlete(2, Gender.M, 80, 100);
        Participation original = first.getMainRankings();
        original.setBestAthleteRank(8);
        original.setTotalRank(5);
        original.setCategoryScoreRank(4);
        Participation secondary = new Participation(first, new Category(first.getCategory()));
        secondary.setBestAthleteRank(9);
        List<PAthlete> inputs = List.of(new PAthlete(original), new PAthlete(tied.getMainRankings()),
                new PAthlete(secondary), new PAthlete(third.getMainRankings()));

        List<PAthlete> report = AthleteSorter.bestAthleteOrderCopy(inputs, Ranking.BW_SINCLAIR);
        assertEquals(3, report.size());
        assertEquals(3, report.stream().map(Athlete::getId).distinct().count());
        assertEquals(List.of(1, 2, 3), report.stream().map(Athlete::getBestAthleteRank).toList());
        assertEquals(third.getId(), report.get(2).getId());
        assertTrue(report.get(0).getId() < report.get(1).getId());
        PAthlete firstInReport = report.stream().filter(a -> a.getId().equals(first.getId()))
                .findFirst().orElseThrow();
        assertEquals(5, firstInReport.getTotalRank());
        assertEquals(4, firstInReport.getCategoryScoreRank());
        assertEquals(8, original.getBestAthleteRank());
        assertEquals(9, secondary.getBestAthleteRank());
        assertEquals(8, inputs.get(0).getBestAthleteRank());
        assertEquals(9, inputs.get(2).getBestAthleteRank());

        List<PAthlete> assigned = new ArrayList<>(inputs);
        AthleteSorter.assignBestAthleteRanks(assigned, Ranking.BW_SINCLAIR);
        assertEquals(inputs.get(0).getBestAthleteRank(), inputs.get(2).getBestAthleteRank());
        assertEquals(3, inputs.get(3).getBestAthleteRank());
        assertEquals(8, original.getBestAthleteRank());
        assertEquals(9, secondary.getBestAthleteRank());
    }

    @Test
    public void bestAthleteReportsKeepPopulationAndScoringSelectionsIndependent() {
        Athlete stronger = prepareBestAthlete(0, Gender.M, 110, 130);
        Athlete athlete = prepareBestAthlete(1, Gender.M, 90, 110);
        athlete.getMainRankings().setBestAthleteRank(7);

        List<PAthlete> largerReport = AthleteSorter.bestAthleteOrderCopy(List.of(athlete, stronger),
                Ranking.BW_SINCLAIR);
        List<PAthlete> smallerReport = AthleteSorter.bestAthleteOrderCopy(List.of(athlete),
                Ranking.BW_SINCLAIR);
        assertEquals(2, largerReport.get(1).getBestAthleteRank());
        assertEquals(1, smallerReport.get(0).getBestAthleteRank());
        assertEquals(7, athlete.getBestAthleteRank());

        EnumMap<Ranking, Boolean> previousConfig = RankingConfig.getConfig();
        try {
            for (Ranking ranking : RankingConfig.getAllScoringRankings()) {
                RankingConfig.setUserEnabled(ranking, true);
                List<PAthlete> report = AthleteSorter.bestAthleteOrderCopy(List.of(athlete), ranking);
                int expectedRank = Ranking.getRankingValue(athlete, ranking) > 0 ? 1 : 0;
                assertEquals(ranking.name(), expectedRank, report.get(0).getBestAthleteRank());
                assertEquals(ranking.name(), 7, athlete.getBestAthleteRank());
                assertEquals(ranking.name(), 2, largerReport.get(1).getBestAthleteRank());
            }
        } finally {
            previousConfig.forEach(RankingConfig::setUserEnabled);
        }
    }

    @Test
    public void quebecTemplatesUseSelectedBestAthleteListsScoresAndRanks() throws Exception {
        Athlete man = prepareBestAthlete(0, Gender.M, 100, 120);
        Athlete secondMan = prepareBestAthlete(1, Gender.M, 80, 100);
        Athlete woman = prepareBestAthlete(2, Gender.F, 70, 90);
        man.getMainRankings().setBestAthleteRank(77);
        for (Ranking system : List.of(Ranking.CAT_SINCLAIR, Ranking.BW_SINCLAIR)) {
            List<PAthlete> ranked = AthleteSorter.bestAthleteOrderCopy(List.of(man, secondMan, woman), system);
            List<PAthlete> men = ranked.stream().filter(a -> a.getGender() == Gender.M).toList();
            List<PAthlete> women = ranked.stream().filter(a -> a.getGender() == Gender.F).toList();
            assertEquals(2, men.size());
            assertEquals(1, women.size());
            assertTrue(men.get(0).getBestAthleteRank() > 0);
            Ranking previousSystem = JXLSWorkbookStreamSource.getBestLifterRankingThreadLocal();
            try {
                JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(system);
                for (String template : List.of("Qc_JduQ_fr_CA.xlsx", "Qc_Juvenile_fr_CA.xlsx")) {
                    try (Workbook workbook = renderQuebecTemplate(template, system, men, women)) {
                        assertQuebecBestAthletes(workbook.getSheet("MS"), men, system);
                        assertQuebecBestAthletes(workbook.getSheet("WS"), women, system);
                    }
                    try (Workbook workbook = renderQuebecTemplate(template, system, List.of(), List.of())) {
                        assertQuebecBestAthletes(workbook.getSheet("MS"), List.of(), system);
                        assertQuebecBestAthletes(workbook.getSheet("WS"), List.of(), system);
                    }
                }
            } finally {
                JXLSWorkbookStreamSource.setBestLifterRankingThreadLocal(previousSystem);
            }
        }
        assertEquals(77, man.getBestAthleteRank());
    }

    private Workbook renderQuebecTemplate(String template, Ranking system, List<PAthlete> men,
            List<PAthlete> women) throws Exception {
        Map<String, Object> beans = new HashMap<>();
        beans.put("competition", Competition.getCurrent());
        beans.put("mBest", men);
        beans.put("wBest", women);
        beans.put("bestRankingTitle", Ranking.getScoringTitle(system));
        for (String name : List.of("mTot", "wTot", "clubs", "mwCombined", "mwTeam")) {
            beans.put(name, List.of());
        }
        beans.put("nbAthletes", men.size() + women.size());
        beans.put("nbClubs", 0);
        try (InputStream input = getClass().getResourceAsStream("/templates/competitionBook/qc/" + template)) {
            Workbook workbook = WorkbookFactory.create(input);
            try {
                new XLSTransformer().transformWorkbook(workbook, beans);
                return workbook;
            } catch (Exception e) {
                workbook.close();
                throw e;
            }
        }
    }

    private void assertQuebecBestAthletes(Sheet sheet, List<PAthlete> athletes, Ranking system) {
        assertEquals(Ranking.getScoringTitle(system), sheet.getRow(0).getCell(18).getStringCellValue());
        int found = 0;
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    assertTrue(cell.getStringCellValue(),
                            !cell.getStringCellValue().contains("${l.best"));
                }
            }
            for (PAthlete athlete : athletes) {
                boolean nameMatches = false;
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING
                            && athlete.getLastName().equals(cell.getStringCellValue())) {
                        nameMatches = true;
                    }
                }
                if (nameMatches) {
                    assertEquals(athlete.getBestAthleteRank(), row.getCell(20).getNumericCellValue(), 0);
                    assertEquals(Ranking.getRankingValue(athlete._getAthlete(), system),
                            row.getCell(19).getNumericCellValue(), 0.000001);
                    assertEquals(athlete.getSinclair(), row.getCell(18).getNumericCellValue(), 0.000001);
                    found++;
                }
            }
        }
        assertEquals(athletes.size(), found);
    }

    @Test
    public void bestAthleteRanksSeparateGendersAndSkipZeroAndInvitedAthletes() {
        Athlete man = prepareBestAthlete(0, Gender.M, 100, 120);
        Athlete secondMan = prepareBestAthlete(1, Gender.M, 80, 100);
        Athlete invited = prepareBestAthlete(2, Gender.M, 150, 180);
        invited.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.OOC_INVITED);
        Athlete zero = prepareBestAthlete(3, Gender.M, 0, 0);
        zero.getMainRankings().setBestAthleteRank(99);
        Athlete woman = prepareBestAthlete(4, Gender.F, 70, 90);

        List<PAthlete> report = AthleteSorter.bestAthleteOrderCopy(
                List.of(secondMan, zero, woman, invited, man), Ranking.BW_SINCLAIR);
        for (PAthlete row : report) {
            int expectedRank = row.getId().equals(invited.getId()) ? -1
                    : row.getId().equals(zero.getId()) ? 0
                    : row.getId().equals(secondMan.getId()) ? 2 : 1;
            assertEquals(row.getFullName(), expectedRank, row.getBestAthleteRank());
        }
        assertEquals(99, zero.getBestAthleteRank());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bestAthleteReportingRejectsMedalRankingTypes() {
        AthleteSorter.bestAthleteOrderCopy(List.of(athletes.get(0)), Ranking.TOTAL);
    }

    @Test(expected = NullPointerException.class)
    public void bestAthleteReportingRejectsMissingParticipation() {
        Athlete athlete = new Athlete();
        assertEquals(0, athlete.getBestAthleteRank());
        PAthlete.copyForReporting(athlete);
    }

    private Athlete prepareBestAthlete(int index, Gender gender, int snatch, int cleanJerk) {
        Athlete athlete = athletes.get(index);
        athlete.setValidation(false);
        athlete.setGender(gender);
        athlete.setBodyWeight(70.0);
        athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.ELIGIBLE);
        athlete.setSnatch1ActualLift(Integer.toString(snatch));
        athlete.setSnatch2ActualLift("0");
        athlete.setSnatch3ActualLift("0");
        athlete.setCleanJerk1ActualLift(Integer.toString(cleanJerk));
        athlete.setCleanJerk2ActualLift("0");
        athlete.setCleanJerk3ActualLift("0");
        athlete.getCategory();
        return athlete;
    }

    private void assertCategoryRanksAreExtra(Athlete athlete) {
        assertEquals(-1, athlete.getSnatchRank());
        assertEquals(-1, athlete.getCleanJerkRank());
        assertEquals(-1, athlete.getTotalRank());
        assertEquals(-1, athlete.getCustomRank());
        assertEquals(-1, athlete.getCategoryScoreRank());
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
