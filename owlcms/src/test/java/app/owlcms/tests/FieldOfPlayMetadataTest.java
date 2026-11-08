/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tools.jackson.databind.JsonNode;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteMetadata;
import app.owlcms.data.athlete.AthleteMetadataEdit;
import app.owlcms.data.athlete.AthleteMetadataEdit.Opening;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.AthleteRepository.MetadataSaveResult;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.init.TestPlatforms;
import app.owlcms.uievents.BreakType;

/**
 * Metadata corrections saved through the platform leave the lift, clock and decision state alone, and are picked up
 * by the next normal recomputation.
 */
public class FieldOfPlayMetadataTest {

    private static Group groupA;
    private List<Athlete> athletes;
    private final MockCountdownTimer athleteTimer = new MockCountdownTimer();

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
    public void setupTest() throws IOException {
        TestData.insertInitialData(5, true);
        RecordRepository.clearLoadedRecords();
        RecordRepository.clearNewRecords();
        RecordConfig.apply(config -> {
            config.setRecordOrder(List.of());
            config.setShowAllFederations(false);
            config.setShowAllCategoryRecords(false);
        }, AuditOrigin.system("metadata test setup"));
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
    }

    @Test
    public void metadataSavedThroughThePlatformLeavesLiftingStateUntouched() {
        FieldOfPlay fop = startFop();
        Athlete lifter = fop.getCurAthlete();
        FOPState stateBefore = fop.getState();
        List<Long> orderBefore = ids(fop.getLiftingOrder());
        int timeBefore = fop.getAthleteTimer().getTimeRemaining();
        Athlete stored = AthleteRepository.findById(lifter.getId());
        String declarationBefore = stored.getSnatch1Declaration();

        MetadataSaveResult result = fop.updateAthlete(lifter.getId(), corrected(stored, "IWF,EWF"),
                stored.getMetadataUpdatedAt(), AuditOrigin.system("metadata test"));

        assertTrue(result.saved());
        assertEquals(stateBefore, fop.getState());
        assertEquals(lifter, fop.getCurAthlete());
        assertEquals(orderBefore, ids(fop.getLiftingOrder()));
        assertEquals(timeBefore, fop.getAthleteTimer().getTimeRemaining());
        Athlete reloaded = AthleteRepository.findById(lifter.getId());
        assertEquals("Fixed", reloaded.getFirstName());
        assertEquals(declarationBefore, reloaded.getSnatch1Declaration());
        assertEquals(0, reloaded.getAttemptsDone().intValue());
    }

    @Test
    public void metadataEditorSaveWaitsForThePlatformAndInvalidatesRecordsWithoutDisturbingTheLift() throws Exception {
        FieldOfPlay fop = startRecordScenario(false);
        Map<String, FieldOfPlay> previous = TestPlatforms.replace(new HashMap<>(Map.of("test", fop)));
        Thread worker = null;
        try {
            Athlete lifter = fop.getCurAthlete();
            Athlete other = fop.getLiftingOrder().get(1);
            fop.fopEventPost(new FOPEvent.TimeStarted(this));
            Opening opening = AthleteMetadataEdit.open(lifter.getId());
            AthleteMetadata values = AthleteMetadataEdit.fromForm("Fixed", "Corrected", "Other team", "EWF",
                    EligibleForIndividualRankingStatus.ELIGIBLE);
            long sequence = fop.getUiEventSequence();
            int remaining = this.athleteTimer.getTimeRemaining();
            FutureTask<MetadataSaveResult> save = new FutureTask<>(
                    () -> AthleteMetadataEdit.save(opening, values, AuditOrigin.system("form test")));
            worker = new Thread(save, "metadata-save-readiness");
            synchronized (fop) {
                worker.start();
                assertTrue("the save must block on this platform, not merely be waiting to start",
                        awaitBlockedOnPlatform(worker, fop));
                assertFalse(save.isDone());
                assertEquals(opening.updatedAt(),
                        AthleteRepository.findById(lifter.getId()).getMetadataUpdatedAt());
            }

            assertTrue(save.get(5, TimeUnit.SECONDS).saved());
            assertEquals(values, AthleteMetadata.from(AthleteRepository.findById(lifter.getId())));
            assertEquals(FOPState.TIME_RUNNING, fop.getState());
            assertTrue(this.athleteTimer.isRunning());
            assertEquals(remaining, this.athleteTimer.getTimeRemaining());
            assertSame(lifter, fop.getCurAthlete());
            assertEquals(sequence, fop.getUiEventSequence());
            assertRecordDisplay(fop.getRecordsJson(), false, "IWF");

            changeOpeningWeight(fop, other, "70");
            assertRecordDisplay(fop.getRecordsJson(), false, "EWF");
        } finally {
            try {
                if (worker != null) {
                    worker.join(5000);
                    assertFalse("metadata save worker did not finish", worker.isAlive());
                }
            } finally {
                TestPlatforms.replace(previous);
            }
        }
    }

    private boolean awaitBlockedOnPlatform(Thread worker, FieldOfPlay fop) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        var threads = ManagementFactory.getThreadMXBean();
        while (System.nanoTime() < deadline) {
            ThreadInfo info = threads.getThreadInfo(worker.threadId());
            if (info == null) {
                return false;
            }
            if (info.getThreadState() == Thread.State.BLOCKED && info.getLockInfo() != null
                    && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(fop)
                    && info.getLockOwnerId() == Thread.currentThread().threadId()) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    @Test
    public void metadataSavePreservesRunningClockAndPartialRefereeAndJuryVotes() {
        FieldOfPlay fop = startFop();
        fop.setJurySize(3);
        Athlete lifter = fop.getCurAthlete();
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, true));
        fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(this, 0, false));

        assertEquals(FOPState.TIME_RUNNING, fop.getState());
        assertTrue(this.athleteTimer.isRunning());
        assertArrayEquals(new Boolean[] { true, null, null }, fop.getRefereeDecision());
        assertArrayEquals(new Boolean[] { false, null, null, null, null }, fop.getJuryMemberDecision());
        Long[] refereeTimes = fop.getRefereeTime().clone();
        assertNotNull(refereeTimes[0]);
        int remaining = this.athleteTimer.getTimeRemaining();
        int stoppedAt = this.athleteTimer.getTimeRemainingAtLastStop();
        int allowed = fop.getClockOwnerInitialTimeAllowed();
        long sequence = fop.getUiEventSequence();
        List<Long> order = ids(fop.getLiftingOrder());

        MetadataSaveResult saved = saveCorrection(fop, lifter, "EWF");

        assertTrue(saved.saved());
        assertEquals(FOPState.TIME_RUNNING, fop.getState());
        assertTrue(this.athleteTimer.isRunning());
        assertEquals(remaining, this.athleteTimer.getTimeRemaining());
        assertEquals(stoppedAt, this.athleteTimer.getTimeRemainingAtLastStop());
        assertEquals(allowed, fop.getClockOwnerInitialTimeAllowed());
        assertSame(lifter, fop.getClockOwner());
        assertSame(lifter, fop.getCurAthlete());
        assertEquals(order, ids(fop.getLiftingOrder()));
        assertArrayEquals(new Boolean[] { true, null, null }, fop.getRefereeDecision());
        assertArrayEquals(refereeTimes, fop.getRefereeTime());
        assertArrayEquals(new Boolean[] { false, null, null, null, null }, fop.getJuryMemberDecision());
        assertEquals("metadata saves emit no FOP display/timer events", sequence, fop.getUiEventSequence());
        assertEquals(0, AthleteRepository.findById(lifter.getId()).getAttemptsDone().intValue());
        assertEquals("EWF", AthleteRepository.findById(lifter.getId()).getFederationCodes());
    }

    @Test
    public void staleTimestampIsRefusedAndNothingIsWritten() {
        FieldOfPlay fop = startFop();
        Athlete lifter = fop.getCurAthlete();
        Athlete stored = AthleteRepository.findById(lifter.getId());
        Long openedWith = stored.getMetadataUpdatedAt();
        AuditOrigin origin = AuditOrigin.system("metadata test");

        MetadataSaveResult first = fop.updateAthlete(lifter.getId(), corrected(stored, "IWF"), openedWith, origin);
        MetadataSaveResult second = fop.updateAthlete(lifter.getId(),
                new AthleteMetadata("Other", "Person", "Elsewhere", "IWF", true, null), openedWith, origin);

        assertTrue(first.saved());
        assertTrue(second.stale());
        assertEquals("Fixed", AthleteRepository.findById(lifter.getId()).getFirstName());
    }

    @Test
    public void decisionAfterTheCorrectionKeepsItThroughThePlatform() throws InterruptedException {
        FieldOfPlay fop = startFop();
        Athlete lifter = fop.getCurAthlete();
        Athlete stored = AthleteRepository.findById(lifter.getId());
        AthleteMetadata corrected = corrected(stored, "IWF,EWF");
        MetadataSaveResult result = fop.updateAthlete(lifter.getId(), corrected, stored.getMetadataUpdatedAt(),
                AuditOrigin.system("metadata test"));

        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, true, true, true));

        Athlete reloaded = AthleteRepository.findById(lifter.getId());
        assertEquals(1, reloaded.getAttemptsDone().intValue());
        assertEquals(corrected, AthleteMetadata.from(reloaded));
        assertEquals(result.athlete().getMetadataUpdatedAt(), reloaded.getMetadataUpdatedAt());
    }

    @Test
    public void recordEligibilityFollowsTheCorrectedFederationAfterTheNextRecomputation()
            throws InterruptedException {
        createRecord("EWF", "Continental", 1.0D);
        for (Athlete a : this.athletes) {
            Athlete stored = AthleteRepository.findById(a.getId());
            AthleteRepository.saveMetadata(a.getId(), corrected(stored, "IWF", stored.getFirstName()),
                    stored.getMetadataUpdatedAt(), AuditOrigin.system("metadata test"));
        }
        this.athletes = AthleteRepository.findAll();

        FieldOfPlay fop = startFop();
        Athlete lifter = fop.getCurAthlete();
        Athlete next = fop.getLiftingOrder().get(1);
        assertEquals(Gender.M, next.getGender());
        assertTrue("IWF athlete is not eligible for an EWF record",
                MockFieldOfPlay.eligibleRecords(fop, next).isEmpty());

        Athlete stored = AthleteRepository.findById(next.getId());
        assertTrue(fop.updateAthlete(next.getId(), corrected(stored, "EWF", stored.getFirstName()),
                stored.getMetadataUpdatedAt(), AuditOrigin.system("metadata test")).saved());
        assertTrue("the record maps are not rebuilt by the save itself",
                MockFieldOfPlay.eligibleRecords(fop, next).isEmpty());

        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, true, true, true));

        List<RecordEvent> eligible = MockFieldOfPlay.eligibleRecords(fop, next);
        assertFalse("the corrected federation is used after the recomputation", eligible.isEmpty());
        assertEquals("EWF", eligible.get(0).getRecordFederation());
    }

    @Test
    public void federationCorrectionWithoutARecomputeKeepsTheCurrentAttemptsRecordEvidence() {
        FieldOfPlay fop = startRecordScenario(false);
        Athlete lifter = fop.getCurAthlete();
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        List<RecordEvent> evidence = List.copyOf(fop.getChallengedRecords());
        JsonNode display = fop.getRecordsJson().deepCopy();
        long sequence = fop.getUiEventSequence();

        assertTrue(saveCorrection(fop, lifter, "EWF").saved());

        assertEquals(sequence, fop.getUiEventSequence());
        assertEquals(display, fop.getRecordsJson());
        assertEquals(evidence, fop.getChallengedRecords());
        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, true, true, true));
        assertEquals(evidence, fop.getLastChallengedRecords());
        assertOriginalFederationRecordAwarded();
    }

    @Test
    public void metadataCorrectionPreservesAwardedRecordsAndPartialJuryReviewUntilReversal() {
        FieldOfPlay fop = startRecordScenario(false);
        fop.setJurySize(3);
        Athlete lifter = fop.getCurAthlete();
        Athlete other = fop.getLiftingOrder().get(1);
        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, true, true, true));
        RecordEvent awarded = assertOriginalFederationRecordAwarded();
        List<RecordEvent> evidence = List.copyOf(fop.getLastChallengedRecords());
        fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE,
                0, null, true, this));
        fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(this, 0, false));
        assertEquals(FOPState.BREAK, fop.getState());
        assertTrue(fop.isPanelVoteOpen());
        assertArrayEquals(new Boolean[] { true, true, true }, fop.getRefereeDecision());
        assertArrayEquals(new Boolean[] { false, null, null, null, null }, fop.getJuryMemberDecision());
        Boolean[] referees = fop.getRefereeDecision().clone();
        Boolean[] jurors = fop.getJuryMemberDecision().clone();
        long sequence = fop.getUiEventSequence();

        MetadataSaveResult saved = saveCorrection(fop, lifter, "EWF");

        assertTrue(saved.saved());
        assertEquals(sequence, fop.getUiEventSequence());
        assertEquals(FOPState.BREAK, fop.getState());
        assertEquals(BreakType.JURY, fop.getBreakType());
        assertTrue(fop.isPanelVoteOpen());
        assertSame(lifter, fop.getAthleteUnderReview());
        assertArrayEquals(referees, fop.getRefereeDecision());
        assertArrayEquals(jurors, fop.getJuryMemberDecision());
        assertEquals(evidence, fop.getLastChallengedRecords());
        RecordEvent unchanged = assertOriginalFederationRecordAwarded();
        assertEquals(awarded.getId(), unchanged.getId());
        assertEquals(awarded.getAthleteName(), unchanged.getAthleteName());
        assertEquals(awarded.getNation(), unchanged.getNation());

        changeOpeningWeight(fop, other, "70");
        assertEquals("the refreshed athlete uses the new federation", "EWF", fop.getCurAthlete().getFederationCodes());
        assertEquals(evidence, fop.getLastChallengedRecords());
        fop.fopEventPost(new FOPEvent.JuryDecision(fop.getAthleteUnderReview(), this, false, true));

        assertTrue("the jury cancels the original award", provisionalRecords().isEmpty());
        assertEquals("both original record definitions remain", 2, RecordRepository.findAll().size());
        Athlete reloaded = AthleteRepository.findById(lifter.getId());
        assertEquals(Integer.valueOf(-60), reloaded.getActualLift(1));
        assertEquals(AthleteMetadata.from(saved.athlete()), AthleteMetadata.from(reloaded));
        assertEquals(saved.athlete().getMetadataUpdatedAt(), reloaded.getMetadataUpdatedAt());
    }

    @Test
    public void juryReversalToGoodUsesOriginalEvidenceAfterFederationCorrectionAndRefresh() {
        FieldOfPlay fop = startRecordScenario(false);
        fop.setJurySize(3);
        Athlete lifter = fop.getCurAthlete();
        Athlete other = fop.getLiftingOrder().get(1);
        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, false, false, false));
        List<RecordEvent> evidence = List.copyOf(fop.getLastChallengedRecords());
        assertEquals(List.of("IWF"), evidence.stream().map(RecordEvent::getRecordFederation).toList());
        assertTrue(provisionalRecords().isEmpty());
        MetadataSaveResult saved = saveCorrection(fop, lifter, "EWF");
        assertTrue(saved.saved());

        changeOpeningWeight(fop, other, "70");
        assertEquals(List.of("EWF"),
                fop.getChallengedRecords().stream().map(RecordEvent::getRecordFederation).toList());
        assertEquals(evidence, fop.getLastChallengedRecords());
        fop.fopEventPost(new FOPEvent.JuryDecision(fop.getAthleteUnderReview(), this, true, true));

        assertOriginalFederationRecordAwarded();
        Athlete reloaded = AthleteRepository.findById(lifter.getId());
        assertEquals(Integer.valueOf(60), reloaded.getActualLift(1));
        assertEquals(AthleteMetadata.from(saved.athlete()), AthleteMetadata.from(reloaded));
        assertEquals(saved.athlete().getMetadataUpdatedAt(), reloaded.getMetadataUpdatedAt());
    }

    @Test
    public void eligibleOnlyRecordDisplayRefreshesWithoutChangingTheOtherAthlete() {
        checkRecordDisplayRefresh(false);
    }

    @Test
    public void allFederationsRecordDisplayChangesHighlightsWithoutChangingTheOtherAthlete() {
        checkRecordDisplayRefresh(true);
    }

    private void checkRecordDisplayRefresh(boolean showAllFederations) {
        FieldOfPlay fop = startRecordScenario(showAllFederations);
        Athlete lifter = fop.getCurAthlete();
        Athlete other = fop.getLiftingOrder().get(1);
        List<RecordEvent> otherEligibility = List.copyOf(MockFieldOfPlay.eligibleRecords(fop, other));
        assertEquals(List.of("IWF"), otherEligibility.stream().map(RecordEvent::getRecordFederation).toList());
        assertRecordDisplay(fop.getRecordsJson(), showAllFederations, "IWF");
        JsonNode originalDisplay = fop.getRecordsJson().deepCopy();
        List<RecordEvent> challenges = List.copyOf(fop.getChallengedRecords());
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        int remaining = this.athleteTimer.getTimeRemaining();
        long sequence = fop.getUiEventSequence();

        assertTrue(saveCorrection(fop, lifter, "EWF").saved());

        assertEquals("saving metadata does not refresh the display", sequence, fop.getUiEventSequence());
        assertEquals(originalDisplay, fop.getRecordsJson());
        assertEquals(challenges, fop.getChallengedRecords());
        changeOpeningWeight(fop, other, "70");

        assertEquals(lifter, fop.getCurAthlete());
        assertEquals(FOPState.TIME_RUNNING, fop.getState());
        assertTrue(this.athleteTimer.isRunning());
        assertEquals(remaining, this.athleteTimer.getTimeRemaining());
        assertEquals(lifter, fop.getClockOwner());
        assertEquals("EWF", fop.getCurAthlete().getFederationCodes());
        assertEquals(List.of("EWF"),
                fop.getChallengedRecords().stream().map(RecordEvent::getRecordFederation).toList());
        assertRecordDisplay(fop.getRecordsJson(), showAllFederations, "EWF");
        assertEquals(otherEligibility, MockFieldOfPlay.eligibleRecords(fop, other));

        // A's failed lift leaves record values unchanged. B's display must still use B's own federation.
        changeOpeningWeight(fop, other, "60");
        fop.fopEventPost(new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this, true, false, false, false));
        assertEquals(other, fop.getCurAthlete());
        assertRecordDisplay(fop.getRecordsJson(), showAllFederations, "IWF");
        assertEquals(originalDisplay, fop.getRecordsJson());
    }

    private void assertRecordDisplay(JsonNode display, boolean showAllFederations, String federation) {
        assertTrue("record display must be populated", display.isObject());
        List<String> names = new ArrayList<>();
        display.path("recordNames").forEach(name -> names.add(name.asString()));
        assertEquals(showAllFederations ? List.of("World", "Continental")
                : List.of("IWF".equals(federation) ? "World" : "Continental"), names);
        assertEquals(1, display.path("recordCategories").size());
        assertEquals(1, display.path("recordTable").size());
        JsonNode column = display.path("recordTable").get(0);
        assertEquals("recordBox", column.path("recordClass").asString());
        assertEquals(names.size(), column.path("records").size());
        for (int i = 0; i < names.size(); i++) {
            boolean world = "World".equals(names.get(i));
            JsonNode cell = column.path("records").get(i);
            assertEquals(world ? 50.0D : 55.0D, cell.path("SNATCH").asDouble(), 0.0D);
            boolean eligible = (world ? "IWF" : "EWF").equals(federation);
            assertEquals(eligible ? "highlight" : "", cell.path("snatchHighlight").asString());
            assertTrue(cell.has("CLEANJERK"));
            assertTrue(cell.has("TOTAL"));
            assertFalse(cell.has("cjHighlight"));
            assertFalse(cell.has("totalHighlight"));
        }
    }

    private FieldOfPlay startRecordScenario(boolean showAllFederations) {
        createRecord("IWF", "World", 50.0D);
        createRecord("EWF", "Continental", 55.0D);
        RecordConfig.apply(config -> {
            config.setRecordOrder(List.of("World", "Continental"));
            config.setShowAllFederations(showAllFederations);
        }, AuditOrigin.system("metadata record display test"));
        for (Athlete athlete : this.athletes) {
            athlete.setFederationCodes("IWF");
            AthleteRepository.save(athlete);
        }
        this.athletes = AthleteRepository.findAll();
        return startFop();
    }

    private MetadataSaveResult saveCorrection(FieldOfPlay fop, Athlete athlete, String federations) {
        Athlete stored = AthleteRepository.findById(athlete.getId());
        return fop.updateAthlete(athlete.getId(), corrected(stored, federations), stored.getMetadataUpdatedAt(),
                AuditOrigin.system("metadata test"));
    }

    private void changeOpeningWeight(FieldOfPlay fop, Athlete athlete, String weight) {
        synchronized (fop) {
            Athlete stored = AthleteRepository.findById(athlete.getId());
            stored.setSnatch1Declaration(weight);
            Athlete saved = AthleteRepository.saveNoMetadata(stored);
            fop.fopEventPost(new FOPEvent.WeightChange(this, saved, false));
        }
    }

    private List<RecordEvent> provisionalRecords() {
        return RecordRepository.findAll().stream()
                .filter(record -> groupA.getName().equals(record.getGroupNameString())).toList();
    }

    private RecordEvent assertOriginalFederationRecordAwarded() {
        List<RecordEvent> awarded = provisionalRecords();
        assertEquals(1, awarded.size());
        assertEquals("IWF", awarded.get(0).getRecordFederation());
        assertEquals("World", awarded.get(0).getRecordName());
        assertEquals(60.0D, awarded.get(0).getRecordValue(), 0.0D);
        return awarded.get(0);
    }

    private void createRecord(String federation, String name, double value) {
        RecordEvent record = new RecordEvent();
        record.setRecordFederation(federation);
        record.setRecordName(name);
        record.setGender(Gender.M);
        record.setAgeGrp("SR");
        record.setAgeGrpLower(0);
        record.setAgeGrpUpper(999);
        record.setBwCatLower(0);
        record.setBwCatUpper(500);
        record.setRecordLift(Ranking.SNATCH);
        record.setRecordValue(value);
        record.setAthleteName("Record Holder");
        record.setEvent("Test Event");
        record.setEventLocation("Test City");
        record.setFileName("generated");
        assertNotNull(RecordRepository.save(record));
    }

    private static AthleteMetadata corrected(Athlete stored, String federations) {
        return corrected(stored, federations, "Fixed");
    }

    private static AthleteMetadata corrected(Athlete stored, String federations, String firstName) {
        AthleteMetadata original = AthleteMetadata.from(stored);
        return new AthleteMetadata(firstName, "Corrected", "Other team", federations,
                original.eligibleForIndividualRanking(), original.individualEligibilityStatus());
    }

    private static List<Long> ids(List<Athlete> list) {
        return list.stream().map(Athlete::getId).collect(Collectors.toList());
    }

    /** Two athletes in group A, both opening at 60. */
    private FieldOfPlay startFop() {
        Config config = Config.getCurrent();
        config.setFeatureSwitchValue(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, false);
        Config.setCurrentTesting(config);
        FieldOfPlay fop = MockFieldOfPlay.create(this.athletes, this.athleteTimer, new MockCountdownTimer());
        fop.setDecisionTimingForTests(0, 0, 0, 0, 0);
        OwlcmsSession.setFop(fop);
        fop.testBefore();
        fop.loadGroup(groupA, this, true);
        fop.testStartLifting(groupA, fop);
        this.athletes = fop.getDisplayOrder();
        Athlete first = this.athletes.get(0);
        Athlete second = this.athletes.get(1);
        JPAService.runInTransaction(em -> {
            AthleteSorter.testAssignStartNumbers(this.athletes);
            first.setSnatch1Declaration("60");
            second.setSnatch1Declaration("60");
            em.merge(first);
            em.merge(second);
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
        fop.setPlatform(new Platform());
        fop.setJurySize(0);
        fop.setTestingMode(true);
        return fop;
    }
}
