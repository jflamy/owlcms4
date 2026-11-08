/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteMetadata;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.UIEvent;

/**
 * The lifting order moves to the next athlete as soon as a decision is committed, before the screens process the
 * Decision event. Screens that react to a decision (e.g. the athlete card, which closes) must use the athlete carried
 * by the event, not the current athlete.
 */
public class DecisionEventAthleteTest {

    private static Group groupA;
    private List<Athlete> athletes;

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
    public void setupTest() {
        TestData.insertInitialData(5, true);
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

    @After
    public void restoreConfig() {
        setShowDecisionsImmediately(false);
    }

    @Test
    public void announcerDecisionNamesTheDecidedAthlete() throws InterruptedException {
        FieldOfPlay fop = startFop(false);
        DecisionRecorder recorder = new DecisionRecorder(fop);
        Athlete lifter = fop.getCurAthlete();

        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, true, true, true));

        assertDecisionAbout(recorder, fop, lifter);
    }

    @Test
    public void delayedRefereeDecisionNamesTheDecidedAthlete() throws InterruptedException {
        FieldOfPlay fop = startFop(false);
        DecisionRecorder recorder = new DecisionRecorder(fop);
        Athlete lifter = fop.getCurAthlete();

        refereesGiveGoodLift(fop);

        assertDecisionAbout(recorder, fop, lifter);
    }

    @Test
    public void immediateRefereeDecisionNamesTheDecidedAthlete() throws InterruptedException {
        FieldOfPlay fop = startFop(true);
        DecisionRecorder recorder = new DecisionRecorder(fop);
        Athlete lifter = fop.getCurAthlete();

        refereesGiveGoodLift(fop);

        assertDecisionAbout(recorder, fop, lifter);
    }

    @Test
    public void announcerDecisionKeepsMetadataCorrectedWhileThePlatformHeldTheOldCopy() throws InterruptedException {
        FieldOfPlay fop = startFop(false);
        DecisionRecorder recorder = new DecisionRecorder(fop);
        Athlete lifter = fop.getCurAthlete();
        Corrected corrected = correctMetadata(lifter);

        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, true, true, true));

        assertTrue("no Decision event", recorder.await());
        assertCorrectedAndLifted(lifter, corrected);
    }

    @Test
    public void refereeDecisionKeepsMetadataCorrectedWhileThePlatformHeldTheOldCopy() throws InterruptedException {
        FieldOfPlay fop = startFop(false);
        DecisionRecorder recorder = new DecisionRecorder(fop);
        Athlete lifter = fop.getCurAthlete();
        Corrected corrected = correctMetadata(lifter);

        refereesGiveGoodLift(fop);

        assertTrue("no Decision event", recorder.await());
        assertCorrectedAndLifted(lifter, corrected);
    }

    @Test
    public void delayedRecordNotificationKeepsTheDecidedAthleteAndRecords() throws InterruptedException {
        checkDelayedRecordNotification(false);
    }

    @Test
    public void delayedRecordNotificationSurvivesTheNextDecisionClearingItsEvidence() throws InterruptedException {
        checkDelayedRecordNotification(true);
    }

    @Test
    public void delayedJuryRecordNotificationSurvivesTheNextClockStart() throws InterruptedException {
        FieldOfPlay fop = startFop(false);
        RecordNotificationRecorder recorder = new RecordNotificationRecorder(fop);
        Athlete lifter = fop.getCurAthlete();
        String nameAtDecision = lifter.getFullName();
        fop.setChallengedRecords(List.of(record("National")));
        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, false, false, false));
        assertEquals(lifter, fop.getAthleteUnderReview());
        assertEquals(1, fop.getLastChallengedRecords().size());

        fop.setDecisionTimingForTests(0, 0, 1000, 0, 0);
        fop.setTestingMode(false);
        fop.fopEventPost(new FOPEvent.JuryDecision(lifter, this, true, true));
        fop.setTestingMode(true);
        fop.fopEventPost(new FOPEvent.StartLifting(this));
        fop.fopEventPost(new FOPEvent.TimeStarted(this));

        assertNotEquals(lifter, fop.getCurAthlete());
        assertTrue("the next clock clears the jury record evidence", fop.getLastChallengedRecords().isEmpty());
        assertNull("the notification must still be pending", recorder.notification.get());
        assertRecordNotification(recorder, lifter, nameAtDecision);
    }

    private void checkDelayedRecordNotification(boolean nextDecision) throws InterruptedException {
        FieldOfPlay fop = startFop(false);
        DecisionRecorder decisions = new DecisionRecorder(fop);
        RecordNotificationRecorder recorder = new RecordNotificationRecorder(fop);
        Athlete lifter = fop.getCurAthlete();
        String nameAtDecision = lifter.getFullName();
        fop.setChallengedRecords(List.of(record("National")));
        fop.setDecisionTimingForTests(0, 0, 0, 0, 1000);
        fop.setTestingMode(false);
        fop.fopEventPost(new FOPEvent.ExplicitDecision(lifter, this, true, true, true, true));
        assertTrue("no Decision event", decisions.await());

        synchronized (fop) {
            fop.setTestingMode(true);
            if (fop.getState() == FOPState.DECISION_VISIBLE) {
                fop.fopEventPost(new FOPEvent.DecisionReset(this));
            }
            Athlete next = fop.getCurAthlete();
            assertNotEquals(lifter, next);
            if (nextDecision) {
                fop.setChallengedRecords(List.of());
                fop.fopEventPost(new FOPEvent.ExplicitDecision(next, this, true, false, false, false));
                assertEquals("the FOP now reviews a different athlete", next, fop.getAthleteUnderReview());
                assertTrue(fop.getLastChallengedRecords().isEmpty());
            } else {
                fop.setChallengedRecords(List.of(record("Regional")));
            }
        }

        assertNull("the notification must still be pending", recorder.notification.get());
        assertRecordNotification(recorder, lifter, nameAtDecision);
    }

    private void assertRecordNotification(RecordNotificationRecorder recorder, Athlete lifter, String name)
            throws InterruptedException {
        assertTrue("no new-record notification", recorder.received.await(3, TimeUnit.SECONDS));
        UIEvent.RecordNotification notification = recorder.notification.get();
        assertEquals(lifter, notification.getAthlete());
        assertTrue(notification.getTitle(), notification.getTitle().contains(name));
        assertTrue(notification.getTitle(), notification.getTitle().contains("60"));
        assertTrue(notification.getNotificationString(), notification.getNotificationString().contains("National"));
        assertTrue(notification.getNotificationString(), notification.getNotificationString().contains("50"));
        assertEquals(UIEvent.RecordNotification.Level.SUCCESS, notification.getLevel());
    }

    private RecordEvent record(String name) {
        RecordEvent record = new RecordEvent();
        record.setRecordFederation("TEST");
        record.setRecordName(name);
        record.setRecordLift(Ranking.SNATCH);
        record.setRecordValue(50.0D);
        record.setGender(this.athletes.get(0).getGender());
        record.setAgeGrp("SR");
        record.setAgeGrpLower(0);
        record.setAgeGrpUpper(999);
        record.setBwCatLower(0);
        record.setBwCatUpper(999);
        record.setFileName("generated");
        return record;
    }

    private record Corrected(AthleteMetadata metadata, Long stamp) {
    }

    /** The speaker's correction is saved after the platform loaded the athlete, so the platform's copy is stale. */
    private Corrected correctMetadata(Athlete lifter) {
        Athlete stored = AthleteRepository.findById(lifter.getId());
        AthleteMetadata original = AthleteMetadata.from(stored);
        AthleteMetadata corrected = new AthleteMetadata("Fixed", "Corrected", "Other team", "IWF,EWF",
                original.eligibleForIndividualRanking(), original.individualEligibilityStatus());
        AthleteRepository.MetadataSaveResult result = AthleteRepository.saveMetadata(lifter.getId(), corrected,
                stored.getMetadataUpdatedAt(), AuditOrigin.system("metadata test"));
        assertTrue(result.saved());
        return new Corrected(corrected, result.athlete().getMetadataUpdatedAt());
    }

    private void assertCorrectedAndLifted(Athlete lifter, Corrected corrected) {
        Athlete reloaded = AthleteRepository.findById(lifter.getId());
        assertEquals("the lift was committed", 1, reloaded.getAttemptsDone().intValue());
        assertEquals("the decision did not write back the platform's old metadata", corrected.metadata(),
                AthleteMetadata.from(reloaded));
        assertEquals("the decision did not touch the metadata stamp", corrected.stamp(),
                reloaded.getMetadataUpdatedAt());
    }

    private void assertDecisionAbout(DecisionRecorder recorder, FieldOfPlay fop, Athlete lifter)
            throws InterruptedException {
        assertTrue("no Decision event", recorder.await());
        assertEquals("the Decision event names the athlete who lifted", lifter, recorder.decision.get().getAthlete());
        assertEquals("the lift was committed", 1, lifter.getAttemptsDone().intValue());
        assertNotNull(fop.getCurAthlete());
        assertNotEquals("the lifting order has moved on", lifter, fop.getCurAthlete());
    }

    private void refereesGiveGoodLift(FieldOfPlay fop) {
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        fop.fopEventPost(new FOPEvent.TimeStopped(this));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, true));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, true));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, true));
    }

    /** Two athletes in group A, both opening at 60: after the first lift, the other one is up. */
    private FieldOfPlay startFop(boolean showDecisionsImmediately) {
        setShowDecisionsImmediately(showDecisionsImmediately);
        FieldOfPlay fop = MockFieldOfPlay.create(this.athletes, new MockCountdownTimer(), new MockCountdownTimer());
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
        // jury size is stored on the platform; this one is never persisted
        fop.setPlatform(new Platform());
        fop.setJurySize(0);
        fop.setTestingMode(true);
        return fop;
    }

    private void setShowDecisionsImmediately(boolean enabled) {
        Config config = Config.getCurrent();
        config.setFeatureSwitchValue(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, enabled);
        Config.setCurrentTesting(config);
    }

    public static class DecisionRecorder {
        private final AtomicReference<UIEvent.Decision> decision = new AtomicReference<>();
        private final CountDownLatch received = new CountDownLatch(1);

        DecisionRecorder(FieldOfPlay fop) {
            fop.getUiEventBus().register(this);
        }

        @Subscribe
        public void onDecision(UIEvent.Decision e) {
            if (this.decision.compareAndSet(null, e)) {
                this.received.countDown();
            }
        }

        boolean await() throws InterruptedException {
            return this.received.await(3, TimeUnit.SECONDS);
        }
    }

    public static class RecordNotificationRecorder {
        private final AtomicReference<UIEvent.RecordNotification> notification = new AtomicReference<>();
        private final CountDownLatch received = new CountDownLatch(1);

        RecordNotificationRecorder(FieldOfPlay fop) {
            fop.getUiEventBus().register(this);
        }

        @Subscribe
        public void onRecordNotification(UIEvent.RecordNotification e) {
            if (e.isNewRecord() && this.notification.compareAndSet(null, e)) {
                this.received.countDown();
            }
        }
    }
}
