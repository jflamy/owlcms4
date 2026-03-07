/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    public void provisionalCurrentFilterUsesHistoryView() {
        RecordRepository.save(createRecord(100.0D, "A"));
        RecordRepository.save(createRecord(101.0D, "A"));

        List<RecordEvent> provisionalRecords = RecordRepository.findWithFilters(
                null,
                null,
                null,
                null,
                "PROVISIONAL",
                "CURRENT",
                null);

        assertEquals(2, provisionalRecords.size());
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

        RecordRepository.keepLatestOfficialRecordsWithFilters(null, null, null, null);

        List<RecordEvent> history = RecordRepository.findWithFilters(null, null, null, null, "ALL", "HISTORY", null);
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

}
