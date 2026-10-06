/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AthleteDiff.Snapshot;
import app.owlcms.data.athlete.ConcurrentEditCheck.Conflict;
import app.owlcms.data.athlete.ConcurrentEditCheck.SaveAttempt;
import app.owlcms.data.config.Config;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;

public class ConcurrentEditCheckTest {
	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Competition.setCurrent(new Competition());
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	private static Athlete athlete(String coach, Double bodyWeight, String snatch1Declaration) {
		Athlete athlete = new Athlete();
		athlete.setLastName("Lifter");
		athlete.setCoach(coach);
		athlete.setBodyWeight(bodyWeight);
		athlete.setSnatch1Declaration(snatch1Declaration);
		return athlete;
	}

	private static List<Conflict> conflicts(Athlete opened, Athlete stored, Athlete form) {
		return ConcurrentEditCheck.conflicts(ConcurrentEditCheck.snapshot(opened),
		        ConcurrentEditCheck.snapshot(stored), ConcurrentEditCheck.snapshot(form));
	}

	@Test
	public void noChangeElsewhereIsNoConflict() {
		Athlete opened = athlete("Coach", 80.0, "90");
		Athlete form = athlete("Other coach", 81.0, "92");
		assertEquals(List.of(), conflicts(opened, opened, form));
	}

	@Test
	public void changeElsewhereOverwrittenByFormIsConflict() {
		Athlete opened = athlete("Coach", 80.0, "90");
		Athlete stored = athlete("Coach", 80.0, "95");
		Athlete form = athlete("Coach", 80.5, "90");

		List<Conflict> conflicts = conflicts(opened, stored, form);

		assertEquals(1, conflicts.size());
		Conflict conflict = conflicts.get(0);
		assertEquals("snatch1.declaration", conflict.field());
		assertEquals("90", conflict.whenOpened());
		assertEquals("95", conflict.current());
		assertEquals("90", conflict.inForm());
	}

	@Test
	public void reenteringStoredValuesClearsConflicts() {
		// marshal changes body weight while the director saves a new coach and declaration; marshal re-enters them
		Athlete opened = athlete("Coach", 80.0, "90");
		Athlete stored = athlete("Director coach", 80.0, "95");
		Athlete form = athlete("Director coach", 80.5, "95");
		assertEquals(List.of(), conflicts(opened, stored, form));
	}

	@Test
	public void keepingOwnValueReportsOnlyThatField() {
		Athlete opened = athlete("Coach", 80.0, "90");
		Athlete stored = athlete("Director coach", 80.0, "95");
		Athlete form = athlete("Director coach", 80.5, "93");

		List<Conflict> conflicts = conflicts(opened, stored, form);

		assertEquals(1, conflicts.size());
		assertEquals("snatch1.declaration", conflicts.get(0).field());
	}

	@Test
	public void sameChangeOnBothSidesIsNoConflict() {
		Athlete opened = athlete("Coach", 80.0, "90");
		Athlete changed = athlete("Coach", 80.0, "95");
		assertEquals(List.of(), conflicts(opened, changed, changed));
	}

	@Test
	public void fieldsNotWrittenByCardAreIgnored() {
		Athlete opened = athlete("Coach", 80.0, "90");
		opened.setTeam("Team A");
		Athlete stored = athlete("Coach", 80.0, "90");
		stored.setTeam("Team B");
		Athlete form = athlete("Coach", 80.0, "90");
		form.setTeam("Team A");

		assertEquals(List.of(), conflicts(opened, stored, form));
	}

	@Test
	public void liftDecisionReportsOnlyTheLiftNotTheProgression() {
		Athlete opened = athlete("Coach", 80.0, "90");
		Athlete stored = athlete("Coach", 80.0, "90");
		stored.setSnatch1ActualLift("90");
		Athlete form = athlete("Coach", 80.0, "90");

		List<Conflict> conflicts = conflicts(opened, stored, form);
		assertEquals(1, conflicts.size());
		assertEquals("snatch1.actualLift", conflicts.get(0).field());
	}

	@Test
	public void derivedValuesAreNotCompared() {
		Snapshot snapshot = ConcurrentEditCheck.snapshot(new Athlete());
		assertFalse(snapshot.values().containsKey("eligibleForIndividualRanking"));
		assertFalse(snapshot.values().containsKey("withdrawnFromSnatch"));
		assertFalse(snapshot.values().containsKey("snatch2.automatic"));
		assertTrue(snapshot.values().containsKey("coach"));
		assertEquals(ConcurrentEditCheck.FIELDS, snapshot.values().keySet());
	}

	@Test
	public void customScoreChangedElsewhereIsAConflict() {
		Athlete opened = athlete("Coach", 80.0, "90");
		opened.setCustomScore(10.0);
		Athlete stored = athlete("Coach", 80.0, "90");
		stored.setCustomScore(12.5);
		List<Conflict> result = conflicts(opened, stored, opened);
		assertEquals(1, result.size());
		assertEquals("customScore", result.get(0).field());
		assertEquals(12.5, result.get(0).current());
	}

	@Test
	public void savesOnTheSameFopCannotBothPassTheOldReference() throws Exception {
		FieldOfPlay fop = new FieldOfPlay();
		Snapshot opened = snapshot("90");
		AtomicReference<Snapshot> stored = new AtomicReference<>(opened);
		CountDownLatch firstSaving = new CountDownLatch(1);
		CountDownLatch allowCommit = new CountDownLatch(1);
		CountDownLatch secondStarted = new CountDownLatch(1);
		AtomicInteger writes = new AtomicInteger();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> ConcurrentEditCheck.trySave(fop, opened, snapshot("95"), () -> {
				assertTrue(Thread.holdsLock(fop));
				return stored.get();
			}, () -> true, () -> {
				assertTrue(Thread.holdsLock(fop));
				firstSaving.countDown();
				await(allowCommit);
				stored.set(snapshot("95"));
				writes.incrementAndGet();
			}));
			try {
				assertTrue(firstSaving.await(5, TimeUnit.SECONDS));
				var second = executor.submit(() -> {
					secondStarted.countDown();
					return ConcurrentEditCheck.trySave(fop, opened, snapshot("97"), () -> {
						assertTrue(Thread.holdsLock(fop));
						return stored.get();
					}, () -> true, () -> {
						stored.set(snapshot("97"));
						writes.incrementAndGet();
					});
				});
				assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
				allowCommit.countDown();
				assertTrue(first.get(5, TimeUnit.SECONDS).saved());
				SaveAttempt refused = second.get(5, TimeUnit.SECONDS);
				assertFalse(refused.saved());
				assertEquals("95", refused.conflicts().get(0).current());
				assertEquals(1, writes.get());
				assertEquals(snapshot("95"), stored.get());
			} finally {
				allowCommit.countDown();
			}
		}
	}

	@Test
	public void saveAnywayAcceptsOnlyTheSnapshotShown() {
		Object monitor = new FieldOfPlay();
		AtomicReference<Snapshot> stored = new AtomicReference<>(snapshot("95"));
		Snapshot form = snapshot("93");
		AtomicInteger writes = new AtomicInteger();
		SaveAttempt first = ConcurrentEditCheck.trySave(monitor, snapshot("90"), form, stored::get,
		        () -> true, writes::incrementAndGet);
		assertFalse(first.saved());
		stored.set(snapshot("97"));
		SaveAttempt changedAgain = ConcurrentEditCheck.trySave(monitor, first.current(), form, stored::get,
		        () -> true, writes::incrementAndGet);
		assertFalse(changedAgain.saved());
		assertEquals("97", changedAgain.conflicts().get(0).current());
		assertEquals(0, writes.get());
		SaveAttempt accepted = ConcurrentEditCheck.trySave(monitor, changedAgain.current(), form, stored::get,
		        () -> true, writes::incrementAndGet);
		assertTrue(accepted.saved());
		assertEquals(1, writes.get());
	}

	@Test
	public void saveAnywayAlsoDetectsANewChangeToAnotherField() {
		Athlete opened = athlete("Coach", 80.0, "90");
		Athlete stored = athlete("Coach", 80.0, "95");
		Snapshot shown = ConcurrentEditCheck.snapshot(stored);
		stored.setCoach("Director");
		SaveAttempt result = ConcurrentEditCheck.trySave(new FieldOfPlay(), shown,
		        ConcurrentEditCheck.snapshot(opened), () -> ConcurrentEditCheck.snapshot(stored),
		        () -> true, () -> { throw new AssertionError("Unexpected save"); });
		assertFalse(result.saved());
		assertEquals(1, result.conflicts().size());
		assertEquals("coach", result.conflicts().get(0).field());
	}

	@Test
	public void furtherChangeMatchingTheFormNeedsNoNewConfirmation() {
		SaveAttempt result = ConcurrentEditCheck.trySave(new FieldOfPlay(), snapshot("95"), snapshot("97"),
		        () -> snapshot("97"), () -> true, () -> {});
		assertTrue(result.saved());
	}

	@Test
	public void closedCardDoesNotReadOrSave() {
		SaveAttempt result = ConcurrentEditCheck.trySave(new FieldOfPlay(), snapshot("90"), snapshot("95"),
		        () -> { throw new AssertionError("Closed card must not read"); }, () -> false,
		        () -> { throw new AssertionError("Closed card must not save"); });
		assertFalse(result.saved());
		assertTrue(result.conflicts().isEmpty());
	}

	@Test
	public void closingDuringTheReadCancelsTheSave() {
		AtomicInteger checks = new AtomicInteger();
		SaveAttempt result = ConcurrentEditCheck.trySave(new FieldOfPlay(), snapshot("90"), snapshot("95"),
		        () -> snapshot("90"), () -> checks.incrementAndGet() == 1,
		        () -> { throw new AssertionError("Closed card must not save"); });
		assertFalse(result.saved());
	}

	@Test
	public void bothWithdrawalsAreRefusedWhenTheCardIsStale() {
		for (boolean snatchOnly : List.of(true, false)) {
			Athlete form = athlete("Coach", 80.0, "90");
			SaveAttempt result = ConcurrentEditCheck.trySave(new FieldOfPlay(), snapshot("90"),
			        ConcurrentEditCheck.snapshot(form), () -> snapshot("95"), () -> true, () -> {
				        if (snatchOnly) {
					        form.withdrawFromSnatch();
				        } else {
					        form.withdraw();
				        }
			        });
			assertFalse(result.saved());
			assertEquals(0, form.getAttemptsDone().intValue());
		}
	}

	@Test
	public void bothWithdrawalsRunInsideTheGuardWhenFresh() {
		for (boolean snatchOnly : List.of(true, false)) {
			FieldOfPlay fop = new FieldOfPlay();
			Athlete form = athlete("Coach", 80.0, "90");
			Athlete target = new Athlete();
			Snapshot opened = ConcurrentEditCheck.snapshot(form);
			SaveAttempt result = ConcurrentEditCheck.trySave(fop, opened, opened, () -> opened, () -> true, () -> {
				assertTrue(Thread.holdsLock(fop));
				Athlete.conditionalCopy(target, form, true, true, true);
				if (snatchOnly) {
					target.withdrawFromSnatch();
				} else {
					target.withdraw();
				}
			});
			assertTrue(result.saved());
			assertEquals(snatchOnly ? 3 : 6, target.getAttemptsDone().intValue());
		}
	}

	@Test
	public void repositorySaveCommitsBeforeTheNextCardChecks() {
		Athlete opened = athlete("Coach", 80.0, "90");
		AthleteRepository.save(opened);
		ConcurrentEditCheck marshal = ConcurrentEditCheck.open(opened);
		ConcurrentEditCheck director = ConcurrentEditCheck.open(opened);
		Athlete marshalForm = athlete("Coach", 80.0, "95");
		marshalForm.setId(opened.getId());
		Athlete directorForm = athlete("Coach", 80.0, "97");
		directorForm.setId(opened.getId());
		FieldOfPlay fop = new FieldOfPlay();
		assertTrue(marshal.trySave(fop, marshalForm, null, () -> true,
		        () -> AthleteRepository.save(marshalForm)).saved());
		SaveAttempt result = director.trySave(fop, directorForm, null, () -> true,
		        () -> AthleteRepository.save(directorForm));
		assertFalse(result.saved());
		assertEquals("95", result.conflicts().get(0).current());
		assertTrue(director.trySave(fop, directorForm, result.current(), () -> true,
		        () -> AthleteRepository.save(directorForm)).saved());
		assertEquals("97", AthleteRepository.findById(opened.getId()).getSnatch1Declaration());
		assertTrue(director.trySave(fop, directorForm, null, () -> true,
		        () -> AthleteRepository.save(directorForm)).saved());
	}

	@Test
	public void recheckShowsAllPendingOverwritesNotOnlyTheNewestChange() {
		Athlete opened = athlete("Coach", 80.0, "62");
		AthleteRepository.save(opened);
		ConcurrentEditCheck check = ConcurrentEditCheck.open(opened);
		Athlete form = AthleteRepository.findById(opened.getId());
		form.setSnatch1Change1("63");
		Athlete director = AthleteRepository.findById(opened.getId());
		director.setSnatch1Change1("65");
		AthleteRepository.save(director);
		FieldOfPlay fop = new FieldOfPlay();
		SaveAttempt first = check.trySave(fop, form, null, () -> true,
		        () -> { throw new AssertionError("First conflict must not save"); });
		assertEquals(1, first.conflicts().size());
		director.setSnatch1Change2("67");
		AthleteRepository.save(director);
		SaveAttempt recheck = check.trySave(fop, form, first.current(), () -> true,
		        () -> { throw new AssertionError("New conflict must not save"); });
		assertFalse(recheck.saved());
		assertEquals(2, recheck.conflicts().size());
		Conflict change1 = recheck.conflicts().get(0);
		assertEquals("snatch1.change1", change1.field());
		assertEquals("65", change1.current());
		assertEquals("63", change1.inForm());
		Conflict change2 = recheck.conflicts().get(1);
		assertEquals("snatch1.change2", change2.field());
		assertEquals("67", change2.current());
		assertTrue(Athlete.isEmpty((String) change2.inForm()));
		assertTrue(check.trySave(fop, form, recheck.current(), () -> true,
		        () -> AthleteRepository.save(form)).saved());
		Athlete saved = AthleteRepository.findById(opened.getId());
		assertEquals("63", saved.getSnatch1Change1());
		assertTrue(Athlete.isEmpty(saved.getSnatch1Change2()));
	}

	private static Snapshot snapshot(String declaration) {
		return ConcurrentEditCheck.snapshot(athlete("Coach", 80.0, declaration));
	}

	private static void await(CountDownLatch latch) {
		try {
			assertTrue(latch.await(5, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}
}
