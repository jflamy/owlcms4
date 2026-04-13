/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordDefinitionReader;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RecordsTest {
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

    final Logger logger = (Logger) LoggerFactory.getLogger(RecordsTest.class);

    private List<Athlete> athletes;

    public List<Athlete> getAthletes() {
        return athletes;
    }

//    @Test
//    public void initialCheck() {
//        final String resName = "/initialCheck.txt";
//        AthleteSorter.displayOrder(athletes);
//        AthleteSorter.testAssignStartNumbers(athletes);
//
//        Collections.shuffle(athletes);
//
//        List<Athlete> sorted = AthleteSorter.liftingOrderCopy(athletes);
//        final String actual = DebugUtils.shortDump(sorted);
//        assertEqualsToReferenceFile(resName, actual);
//    }

    @Test
    public void liftSequence4() throws InterruptedException {
        FieldOfPlay fopState = OwlcmsSession.getFop();
        EventBus fopBus = fopState.getFopEventBus();

        doLiftSequence4(fopState, fopBus, logger);
    }

    @Test
    public void provisionalCurrentFilterKeepsHighestProvisionalOnly() {
        RecordRepository.save(createRecord(100.0D, "A"));
        RecordRepository.save(createRecord(101.0D, "A"));

        List<RecordEvent> currentProvisionals = RecordRepository.findWithFilters(
                null,
                null,
                null,
                null,
                null,
                "PROVISIONAL",
                "CURRENT",
                null);

        assertEquals(1, currentProvisionals.size());
        assertEquals(101.0D, currentProvisionals.get(0).getRecordValue(), 0.001D);

        List<RecordEvent> provisionalHistory = RecordRepository.findWithFilters(
                null,
                null,
                null,
                null,
                null,
                "PROVISIONAL",
                "HISTORY",
                null);

        assertEquals(2, provisionalHistory.size());
    }

    @Test
    public void exactDuplicateProvisionalsAreSuppressed() {
        RecordEvent duplicate = createRecord(102.0D, "A");
        RecordRepository.save(duplicate);
        RecordRepository.save(createRecord(102.0D, "A"));

        List<RecordEvent> provisionalRecords = RecordRepository.findWithFilters(
                null,
                null,
                null,
                null,
                null,
                "PROVISIONAL",
                "HISTORY",
                null);

        assertEquals(1, provisionalRecords.size());
        assertNotNull(provisionalRecords.get(0));
    }

    @Test
    public void duplicateProvisionalsIgnoreFilename() {
        RecordEvent first = createRecord(103.0D, "A");
        first.setFileName("meet_a");
        RecordEvent second = createRecord(103.0D, "A");
        second.setFileName("meet_b");

        RecordRepository.save(first);
        RecordRepository.save(second);

        List<RecordEvent> provisionalRecords = RecordRepository.findWithFilters(
                null,
                null,
                null,
                null,
                null,
                "PROVISIONAL",
                "HISTORY",
                null);

        assertEquals(1, provisionalRecords.size());
    }

    @Test
    public void keepLatestOfficialRecordsPrunesOnlyOfficialHistory() throws Exception {
        RecordEvent lowerOfficial = createRecord(100.0D, null);
        RecordEvent bestOfficial = createRecord(105.0D, null);
        RecordEvent provisional = createRecord(104.0D, "A");

        RecordRepository.save(lowerOfficial);
        RecordRepository.save(bestOfficial);
        RecordRepository.save(provisional);

        RecordRepository.keepLatestOfficialRecordsWithFilters(null, null, null, null, null);

        List<RecordEvent> history = RecordRepository.findWithFilters(null, null, null, null, null, "ALL", "HISTORY", null);
        assertEquals(2, history.size());
        assertEquals(1, history.stream().filter(rec -> rec.getGroupNameString() == null || rec.getGroupNameString().isBlank()).count());
        assertEquals(1, history.stream().filter(rec -> rec.getGroupNameString() != null && !rec.getGroupNameString().isBlank()).count());
        assertEquals(105.0D,
                history.stream()
                        .filter(rec -> rec.getGroupNameString() == null || rec.getGroupNameString().isBlank())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("missing official record"))
                        .getRecordValue(),
                0.001D);
    }

    @Test
    public void bodyWeightCategoryCodeTracksUpdatedBounds() {
        RecordEvent record = createRecord(100.0D, null);

        assertEquals("76", RecordEvent.computeBodyWeightCategoryCode(record.getBwCatLower(), record.getBwCatUpper()));

        record.setBwCatUpper(81);
        assertEquals("81", RecordEvent.computeBodyWeightCategoryCode(record.getBwCatLower(), record.getBwCatUpper()));
        assertEquals("81", record.getBwCatString());
    }

    @Test
    public void updatingOfficialLogicalKeyMovesOfficialHistory() {
        RecordRepository.save(createRecord(100.0D, null));
        RecordEvent current = RecordRepository.save(createRecord(105.0D, null));

        RecordEvent originalDefinition = copyRecordDefinition(current);
        current.setBwCatUpper(81);

        RecordRepository.save(current, originalDefinition);

        List<RecordEvent> currentRecords = RecordRepository.findWithFilters(null, null, null, null, null, "ALL", "CURRENT", null);
        assertEquals(1, currentRecords.size());
        assertEquals(Integer.valueOf(81), currentRecords.get(0).getBwCatUpper());
        assertEquals(105.0D, currentRecords.get(0).getRecordValue(), 0.001D);

        List<RecordEvent> historyRecords = RecordRepository.findWithFilters(null, null, null, null, null, "ALL", "HISTORY", null);
        assertEquals(2, historyRecords.size());
        assertEquals(0, historyRecords.stream().filter(rec -> Integer.valueOf(76).equals(rec.getBwCatUpper())).count());
        assertEquals(2, historyRecords.stream().filter(rec -> Integer.valueOf(81).equals(rec.getBwCatUpper())).count());
    }

    @Test
    public void currentRecordsStaySequentiallyOrderedAfterCategoryEdit() {
        RecordRepository.save(createRecord(51.0D, null, Ranking.SNATCH, 45));
        RecordRepository.save(createRecord(61.0D, null, Ranking.CLEANJERK, 45));
        RecordRepository.save(createRecord(112.0D, null, Ranking.TOTAL, 45));
        RecordEvent edited = RecordRepository.save(createRecord(63.0D, null, Ranking.SNATCH, 48));
        RecordRepository.save(createRecord(76.0D, null, Ranking.CLEANJERK, 49));
        RecordRepository.save(createRecord(139.0D, null, Ranking.TOTAL, 49));

        RecordEvent originalDefinition = copyRecordDefinition(edited);
        edited.setBwCatLower(44);
        edited.setBwCatUpper(49);

        RecordRepository.save(edited, originalDefinition);

        List<RecordEvent> currentRecords = RecordRepository.findWithFilters(null, null, null, null, null, "ALL", "CURRENT", null);
        assertEquals(Arrays.asList(45, 45, 45, 49, 49, 49),
                currentRecords.stream().map(RecordEvent::getBwCatUpper).toList());
        assertEquals(Arrays.asList(Ranking.SNATCH, Ranking.CLEANJERK, Ranking.TOTAL, Ranking.SNATCH, Ranking.CLEANJERK, Ranking.TOTAL),
                currentRecords.stream().map(RecordEvent::getRecordLift).toList());
    }

    @Test
    public void historyRecordsSortByNormalizedBodyweightCategory() {
        RecordRepository.save(createLegacyCategoryRecord(51.0D, 45));
        RecordRepository.save(createLegacyCategoryRecord(58.0D, 48));
        RecordRepository.save(createLegacyCategoryRecord(63.0D, 49));

        List<RecordEvent> historyRecords = RecordRepository.findWithFilters(null, null, null, null, null, "ALL", "HISTORY", null);

        assertEquals(Arrays.asList("45", "48", "49"),
                historyRecords.stream().map(RecordEvent::getBwCatString).toList());
    }

    @Test
    public void visibleAgeGroupSortTakesPrecedenceOverHiddenAgeBounds() {
        RecordEvent category49 = createRecord(63.0D, null, Ranking.SNATCH, 49);
        category49.setAgeGrp("LC");
        category49.setAgeGrpLower(9);
        category49.setAgeGrpUpper(12);
        RecordRepository.save(category49);

        RecordEvent category45Snatch = createRecord(51.0D, null, Ranking.SNATCH, 45);
        category45Snatch.setAgeGrp("LC");
        category45Snatch.setAgeGrpLower(15);
        category45Snatch.setAgeGrpUpper(999);
        RecordRepository.save(category45Snatch);

        RecordEvent category45Cj = createRecord(61.0D, null, Ranking.CLEANJERK, 45);
        category45Cj.setAgeGrp("LC");
        category45Cj.setAgeGrpLower(15);
        category45Cj.setAgeGrpUpper(999);
        RecordRepository.save(category45Cj);

        List<RecordEvent> historyRecords = RecordRepository.findWithFilters(null, null, null, null, null, "ALL", "HISTORY", null);

        assertEquals(Arrays.asList(45, 45, 49),
                historyRecords.stream().map(RecordEvent::getBwCatUpperForSort).toList());
        assertEquals(Arrays.asList(Ranking.SNATCH, Ranking.CLEANJERK, Ranking.SNATCH),
                historyRecords.stream().map(RecordEvent::getRecordLift).toList());
    }

    @Test
    public void redefiningCurrentOfficialRecordTriggersWarningRule() {
        RecordRepository.save(createRecord(100.0D, null));
        RecordEvent current = RecordRepository.save(createRecord(105.0D, null));

        RecordEvent updated = copyRecordDefinition(current);
        updated.setBwCatUpper(81);

        assertEquals(true, RecordRepository.wouldRedefineCurrentOfficialRecord(current, updated));
    }

    @Test
    public void redefiningHistoricalOfficialRecordDoesNotTriggerWarningRule() {
        RecordEvent historical = RecordRepository.save(createRecord(100.0D, null));
        RecordRepository.save(createRecord(105.0D, null));

        RecordEvent updated = copyRecordDefinition(historical);
        updated.setBwCatUpper(81);

        assertEquals(false, RecordRepository.wouldRedefineCurrentOfficialRecord(historical, updated));
    }

    @Test
    public void heavyweightNumericCategoriesNormalizeTo999ForSort() {
        assertEquals(Integer.valueOf(999), RecordEvent.computeBodyWeightCategorySortValue(999, null));
        assertEquals(Integer.valueOf(999), RecordEvent.computeBodyWeightCategorySortValue(199, null));
    }

    @Test
    public void heavyweightImportMarkersNormalizeTo999() {
        assertEquals(Integer.valueOf(999), RecordEvent.normalizeImportedBodyWeightCategoryUpper(">87"));
        assertEquals(Integer.valueOf(999), RecordEvent.normalizeImportedBodyWeightCategoryUpper("+87"));
        assertEquals(Integer.valueOf(999), RecordEvent.normalizeImportedBodyWeightCategoryUpper("87+"));
    }

    @Before
    public void setupTest() {
        TestData.insertInitialData(1, true);
		try {
			RecordRepository.clearLoadedRecords();
			RecordRepository.clearNewRecords();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        JPAService.runInTransaction((em) -> {
            gA = GroupRepository.doFindByName("A", em);
            gB = GroupRepository.doFindByName("B", em);
            gC = GroupRepository.doFindByName("C", em);
            TestData.deleteAllLifters(em);
            TestData.insertSampleLifters(em, 1, gA, gB, gC);
            return null;
        });
        AthleteRepository.resetParticipations(false, true);
        athletes = AthleteRepository.findAll();
        FieldOfPlay fopState = FieldOfPlay.mockFieldOfPlay(athletes, new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(Level.INFO);
    }

    public void testPrepState4(FieldOfPlay fopState, EventBus fopBus, Logger logger2) {
        var streamURI = this.getClass().getResourceAsStream("/records/20_CA_QC_Provincial_Records_2022-11-24.xlsx");
        new RecordDefinitionReader().readInputStream(streamURI, "test");
        
        fopState.testBefore();
        fopState.loadGroup(gA, this, true);
        fopState.testStartLifting(gA, fopState);
        athletes = fopState.getDisplayOrder();
        final Athlete schneiderF = athletes.get(0);

        JPAService.runInTransaction(em -> {
            AthleteSorter.testAssignStartNumbers(athletes);
            // simulate initial declaration at weigh-in
            schneiderF.setSnatch1Declaration(Integer.toString(60));
            schneiderF.setCleanJerk1Declaration(Integer.toString(80));
            em.merge(schneiderF);

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
        

    }

    void doLiftSequence4(FieldOfPlay fopState, EventBus fopBus, Logger logger) {
        testPrepState4(fopState, fopBus, logger);
        Group group = fopState.getGroup();
        fopBus.post(new FOPEvent.SwitchGroup(group, this));
        fopBus.post(new FOPEvent.StartLifting(this));

        athletes = fopState.getDisplayOrder();
        final Athlete schneiderF = athletes.get(0);

        // competition start
        assertEquals(60000, fopState.getTimeAllowed());

        // schneiderF snatch1
        Athlete curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        successfulLift(fopBus, curLifter, fopState);
        
        // Check that record has been set and assigned

        // schneiderF snatch2
        curLifter = fopState.getCurAthlete();
        assertEquals(schneiderF, curLifter);
        assertEquals(120000, fopState.getTimeAllowed());
        successfulLift(fopBus, curLifter, fopState);
        
        // Check that record has been improved
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
//        fopState.finalDecision(null);
        fopBus.post(new FOPEvent.DecisionReset(null));

    }

    private void successfulLift(EventBus fopBus, Athlete curLifter, FieldOfPlay fopState) {
        logger.debug("calling lifter: {}", curLifter);
        fopBus.post(new FOPEvent.TimeStarted(null));
        fopBus.post(new FOPEvent.DownSignal(null));
        fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, true, true, true, 0L, 0L, 0L, false));
        logger.debug("successful lift for {}", curLifter);
//        fopState.finalDecision(null);
        fopBus.post(new FOPEvent.DecisionReset(null));
    }

    private RecordEvent createRecord(double value, String groupName) {
        RecordEvent record = new RecordEvent();
        record.setRecordFederation("QC");
        record.setRecordName("Provincial");
        record.setGender(Gender.F);
        record.setAgeGrp("SR");
        record.setAgeGrpLower(15);
        record.setAgeGrpUpper(999);
        record.setBwCatLower(71);
        record.setBwCatUpper(76);
        record.setRecordLift(Ranking.SNATCH);
        record.setRecordValue(value);
        record.setAthleteName("Athlete One");
        record.setEvent("Test Event");
        record.setEventLocation("Test City");
        record.setGroupNameString(groupName);
        record.setFileName("generated");
        return record;
    }

    private RecordEvent copyRecordDefinition(RecordEvent source) {
        RecordEvent copy = new RecordEvent();
        copy.setId(source.getId());
        copy.setRecordFederation(source.getRecordFederation());
        copy.setRecordName(source.getRecordName());
        copy.setAgeGrp(source.getAgeGrp());
        copy.setGender(source.getGender());
        copy.setRecordLift(source.getRecordLift());
        copy.setAgeGrpLower(source.getAgeGrpLower());
        copy.setAgeGrpUpper(source.getAgeGrpUpper());
        copy.setBwCatLower(source.getBwCatLower());
        copy.setBwCatUpper(source.getBwCatUpper());
        copy.setGroupNameString(source.getGroupNameString());
        return copy;
    }

    private RecordEvent createRecord(double recordValue, String groupNameString, Ranking recordLift, int bwCatUpper) {
        RecordEvent record = createRecord(recordValue, groupNameString);
        record.setRecordLift(recordLift);
        record.setBwCatUpper(bwCatUpper);
        record.setBwCatLower(Math.max(0, bwCatUpper - 5));
        record.syncBodyWeightCategoryString();
        return record;
    }

	private RecordEvent createLegacyCategoryRecord(double recordValue, int bwCatUpper) {
		RecordEvent record = createRecord(recordValue, null);
		record.setBwCatLower(Math.max(0, bwCatUpper - 5));
		record.setBwCatUpper(null);
		record.setBwCatString(Integer.toString(bwCatUpper));
		return record;
	}

}
