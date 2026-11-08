/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteMetadata;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.ConcurrentEditCheck;
import app.owlcms.data.athlete.RegistrationEditCheck;
import app.owlcms.data.athlete.RegistrationTabPolicy;
import app.owlcms.data.athlete.RegistrationTabPolicy.CardEntry;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.tests.MockCountdownTimer;
import app.owlcms.tests.TestData;
import app.owlcms.uievents.BreakType;

/**
 * A session is in progress from the first athlete clock start (or when selected with completed attempts) until it is
 * unselected. These backend checks support metadata-only registration while a session is in progress, independently
 * of the future tabbed editor. Standalone lifting cards retain their own save checks.
 */
public class LiftingSessionStatusTest {
	private Map<String, FieldOfPlay> previous;
	private Group gA;
	private Group gB;
	private FieldOfPlay fop;

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
	public void setupPlatform() {
		TestData.insertInitialData(5, true);
		JPAService.runInTransaction(em -> {
			this.gA = GroupRepository.doFindByName("A", em);
			this.gB = GroupRepository.doFindByName("B", em);
			Group gC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 5, this.gA, this.gB, gC);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);
		this.fop = MockFieldOfPlay.create(AthleteRepository.findAll(), new MockCountdownTimer(),
		        new MockCountdownTimer());
		this.previous = OwlcmsFactory.getFopByName();
		OwlcmsFactory.setFopByName(new HashMap<>());
		OwlcmsFactory.getFopByName().put("test", this.fop);
		post(new FOPEvent.SwitchGroup(null, this));
	}

	@After
	public void restorePlatforms() {
		OwlcmsFactory.setFopByName(this.previous);
	}

	@Test
	public void noSelectedSessionAllowsEditing() {
		assertFalse(this.fop.isSessionInProgress());
		assertNull(OwlcmsFactory.getFOPSessionInProgress(this.gA));
	}

	@Test
	public void owningPlatformIsResolvedByIdBeforeTheSessionStarts() {
		assertNull(OwlcmsFactory.getFOPWithGroup(this.gA));
		select(this.gA);
		assertFalse(this.fop.isSessionInProgress());
		Group renamedCopy = new Group("Renamed");
		renamedCopy.setId(this.gA.getId());
		assertSame(this.fop, OwlcmsFactory.getFOPWithGroup(renamedCopy));
		assertNull(OwlcmsFactory.getFOPWithGroup(this.gB));
		assertNull(OwlcmsFactory.getFOPWithGroup(null));
	}

	@Test
	public void cardWithoutOriginFopSavesUnderTheOwningPlatform() {
		// Assign start numbers before opening the draft; changing those would correctly make the card stale.
		select(this.gA);
		select(null);
		Athlete draft = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
		ConcurrentEditCheck check = ConcurrentEditCheck.open(draft);
		draft.setValidation(false);
		draft.setSnatch1Declaration("95");
		// The platform is selected after the card was opened. Resolve it on save, not from the UI's origin.
		select(this.gA);
		AthleteMetadata original = AthleteMetadata.from(draft);
		AthleteMetadata corrected = new AthleteMetadata(original.firstName(), "Corrected", original.team(),
		        original.federationCodes(), original.eligibleForIndividualRanking(), original.individualEligibilityStatus());
		Long metadataStamp;
		synchronized (this.fop) {
			metadataStamp = AthleteRepository.saveMetadata(draft.getId(), corrected, draft.getMetadataUpdatedAt(),
			        AuditOrigin.system("registration card routing test")).athlete().getMetadataUpdatedAt();
		}

		ConcurrentEditCheck.SaveAttempt attempt = check.trySave(draft, null, () -> true, owningFop -> {
			assertSame(this.fop, owningFop);
			assertTrue(Thread.holdsLock(owningFop));
			AthleteRepository.saveNoMetadata(draft);
			owningFop.fopEventPost(new FOPEvent.WeightChange(this, draft, false));
		});
		assertTrue(attempt.conflicts().toString(), attempt.saved());

		Athlete stored = AthleteRepository.findById(draft.getId());
		assertEquals("95", stored.getSnatch1Declaration());
		assertEquals(corrected, AthleteMetadata.from(stored));
		assertEquals(metadataStamp, stored.getMetadataUpdatedAt());
	}

	@Test
	public void cardWaitsForPlatformSaveBeforeCheckingConflicts() throws Exception {
		startClock(this.gA);
		Athlete draft = AthleteRepository.findById(this.fop.getCurAthlete().getId());
		ConcurrentEditCheck check = ConcurrentEditCheck.open(draft);
		draft.setValidation(false);
		draft.setSnatch1Change1("95");
		CountDownLatch attempted = new CountDownLatch(1);
		try (var executor = Executors.newSingleThreadExecutor()) {
			Future<ConcurrentEditCheck.SaveAttempt> pending;
			synchronized (this.fop) {
				pending = executor.submit(() -> {
					attempted.countDown();
					return check.trySave(draft, null, () -> true,
					        owningFop -> { throw new AssertionError("The stale card must not save"); });
				});
				assertTrue(attempted.await(5, TimeUnit.SECONDS));
				Athlete current = AthleteRepository.findById(draft.getId());
				current.setValidation(false);
				current.setSnatch1Change1("96");
				AthleteRepository.saveNoMetadata(current);
			}
			ConcurrentEditCheck.SaveAttempt refused = pending.get(5, TimeUnit.SECONDS);
			assertFalse(refused.saved());
			assertEquals("snatch1.change1", refused.conflicts().getFirst().field());
			assertEquals("96", refused.conflicts().getFirst().current());
			assertTrue(check.trySave(draft, refused.current(), () -> true, owningFop -> {
				assertSame(this.fop, owningFop);
				assertTrue(Thread.holdsLock(owningFop));
				AthleteRepository.saveNoMetadata(draft);
			}).saved());
			assertEquals("95", AthleteRepository.findById(draft.getId()).getSnatch1Change1());
		}
	}

	@Test
	public void selectedSessionAllowsEditingUntilTheFirstClockStart() {
		select(this.gA);
		assertNotInProgress(this.gA);
		post(new FOPEvent.StartLifting(this));
		assertNotInProgress(this.gA);
		post(new FOPEvent.TimeStarted(this));
		assertInProgress(this.gA);
	}

	@Test
	public void registrationFormChoiceUsesEachAthletesSessionInAMixedGrid() {
		startClock(this.gA);
		Athlete lifting = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
		Athlete waiting = AthleteRepository.findAllByGroupAndWeighIn(this.gB, true).getFirst();
		assertTrue(RegistrationEditCheck.requiresMetadataForm(lifting));
		assertFalse(RegistrationEditCheck.requiresMetadataForm(waiting));
		assertFalse(RegistrationEditCheck.requiresMetadataForm(new Athlete()));
		post(new FOPEvent.TimeStopped(this));
		assertTrue("a stopped clock does not reopen full registration",
		        RegistrationEditCheck.requiresMetadataForm(lifting));
	}

	@Test
	public void registrationFormChoiceChangesAtSessionStartAndUnselection() {
		Athlete athlete = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
		select(this.gA);
		post(new FOPEvent.StartLifting(this));
		assertFalse(RegistrationEditCheck.requiresMetadataForm(athlete));
		post(new FOPEvent.TimeStarted(this));
		assertTrue(RegistrationEditCheck.requiresMetadataForm(athlete));
		select(null);
		assertFalse(RegistrationEditCheck.requiresMetadataForm(athlete));
	}

	@Test
	public void completedAttemptsRequireMetadataOnlyWhenTheirSessionIsInProgress() {
		for (String result : List.of("60", "-60")) {
			select(null);
			Athlete athlete = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
			athlete.setValidation(false);
			athlete.setSnatch1Declaration("60");
			athlete.setSnatch1ActualLift(result);
			AthleteRepository.save(athlete);

			assertEquals(1, athlete.getAttemptsDone().intValue());
			assertFalse("past results alone do not make a session live",
			        RegistrationEditCheck.requiresMetadataForm(athlete));
			select(this.gA);
			assertTrue(RegistrationEditCheck.requiresMetadataForm(athlete));
		}
	}

	@Test
	public void tabPolicyKeepsTheOriginalFormBeforeLiftingStarts() {
		Athlete athlete = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
		athlete = AthleteRepository.findById(athlete.getId());

		assertEquals(new RegistrationTabPolicy.Layout(false, false, RegistrationTabPolicy.Tab.REGISTRATION),
		        RegistrationTabPolicy.forNewAthlete());
		assertEquals(new RegistrationTabPolicy.Layout(false, false, RegistrationTabPolicy.Tab.REGISTRATION),
		        RegistrationTabPolicy.choose(CardEntry.NONE, athlete));
		assertEquals(new RegistrationTabPolicy.Layout(false, false, RegistrationTabPolicy.Tab.REGISTRATION),
		        RegistrationTabPolicy.choose(CardEntry.AVAILABLE, athlete));
		assertEquals("no completed attempt: registration first",
		        new RegistrationTabPolicy.Layout(false, false, RegistrationTabPolicy.Tab.REGISTRATION),
		        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, athlete));

		athlete.setSnatch1Declaration("60");
		AthleteRepository.save(athlete);
		assertEquals("a declaration is not a result",
		        RegistrationTabPolicy.Tab.REGISTRATION,
		        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST,
		                AthleteRepository.findById(athlete.getId())).initialTab());
		assertFalse("a declaration does not add a Card tab",
		        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, athlete).cardTab());
	}

	@Test
	public void firstClockStartRestrictsRegistrationButAllowsTheSupervisorCard() {
		startClock(this.gA);
		Athlete stored = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
		assertEquals(0, stored.getAttemptsDone().intValue());
		assertEquals("results pages open on Card even when the athlete has no result",
		        new RegistrationTabPolicy.Layout(true, true, RegistrationTabPolicy.Tab.CARD),
		        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, stored));
		assertEquals("registration offers Card without switching the default tab",
		        new RegistrationTabPolicy.Layout(true, true, RegistrationTabPolicy.Tab.INFORMATION),
		        RegistrationTabPolicy.choose(CardEntry.AVAILABLE, stored));
		assertEquals(new RegistrationTabPolicy.Layout(true, false, RegistrationTabPolicy.Tab.INFORMATION),
		        RegistrationTabPolicy.choose(CardEntry.NONE, stored));
		Athlete otherSession = AthleteRepository.findAllByGroupAndWeighIn(this.gB, true).getFirst();
		assertEquals(new RegistrationTabPolicy.Layout(false, false, RegistrationTabPolicy.Tab.REGISTRATION),
		        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, otherSession));
	}

	@Test
	public void tabPolicyOpensOnTheCardWhenAnAttemptIsCompleted() {
		for (String result : List.of("60", "-60")) {
			select(null);
			Athlete athlete = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
			athlete = AthleteRepository.findById(athlete.getId());
			athlete.setValidation(false);
			athlete.setSnatch1Declaration("60");
			athlete.setSnatch1ActualLift(result);
			AthleteRepository.save(athlete);
			Athlete stored = AthleteRepository.findById(athlete.getId());

			assertEquals(new RegistrationTabPolicy.Layout(false, true, RegistrationTabPolicy.Tab.CARD),
			        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, stored));
			assertEquals("pages that do not ask for results first keep registration first",
			        RegistrationTabPolicy.Tab.REGISTRATION,
			        RegistrationTabPolicy.choose(CardEntry.AVAILABLE, stored).initialTab());
			assertFalse("weigh-in never gets the card",
			        RegistrationTabPolicy.choose(CardEntry.NONE, stored).cardTab());
		}
	}

	@Test
	public void partialSessionResultsOfferCardToAthletesWhoHaveNotLifted() {
		select(null);
		List<Athlete> groupAthletes = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true);
		Athlete lifted = AthleteRepository.findById(groupAthletes.getFirst().getId());
		Athlete waiting = AthleteRepository.findById(groupAthletes.getLast().getId());
		Athlete otherSession = AthleteRepository.findAllByGroupAndWeighIn(this.gB, true).getFirst();
		assertEquals(0, waiting.getAttemptsDone().intValue());
		assertFalse(RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, waiting).cardTab());
		for (String result : List.of("60", "-60")) {
			lifted.setValidation(false);
			lifted.setSnatch1Declaration("60");
			lifted.setSnatch1ActualLift(result);
			AthleteRepository.saveNoMetadata(lifted);

			assertEquals("unselected session with any recorded attempt offers Card to waiting athletes",
			        new RegistrationTabPolicy.Layout(false, true, RegistrationTabPolicy.Tab.CARD),
			        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, waiting));
			assertTrue(RegistrationTabPolicy.choose(CardEntry.AVAILABLE, waiting).cardTab());
			assertFalse("weigh-in still has no Card",
			        RegistrationTabPolicy.choose(CardEntry.NONE, waiting).cardTab());
			assertFalse("another session's results do not add Card",
			        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, otherSession).cardTab());
		}
	}

	@Test
	public void dangerModeKeepsInformationAndCardOnlyOnPagesThatOfferCard() {
		select(null);
		Athlete athlete = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).getFirst();
		athlete = AthleteRepository.findById(athlete.getId());
		athlete.setValidation(false);
		athlete.setSnatch1Declaration("60");
		athlete.setSnatch1ActualLift("60");
		AthleteRepository.save(athlete);
		select(this.gA);
		Athlete stored = AthleteRepository.findById(athlete.getId());

		assertEquals(new RegistrationTabPolicy.Layout(true, false, RegistrationTabPolicy.Tab.INFORMATION),
		        RegistrationTabPolicy.choose(CardEntry.NONE, stored));
		assertEquals(new RegistrationTabPolicy.Layout(true, true, RegistrationTabPolicy.Tab.INFORMATION),
		        RegistrationTabPolicy.choose(CardEntry.AVAILABLE, stored));
		assertEquals(new RegistrationTabPolicy.Layout(true, true, RegistrationTabPolicy.Tab.CARD),
		        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, stored));
		select(null);
		assertEquals("unselection restores the original registration tabs without removing Card",
		        new RegistrationTabPolicy.Layout(false, true, RegistrationTabPolicy.Tab.CARD),
		        RegistrationTabPolicy.choose(CardEntry.CARD_FIRST, AthleteRepository.findById(athlete.getId())));
	}

	@Test
	public void registrationFormChoiceUsesReloadedSessionRatherThanTheOldGridRow() {
		startClock(this.gA);
		Athlete row = AthleteRepository.findAllByGroupAndWeighIn(this.gB, true).getFirst();
		Athlete current = AthleteRepository.findById(row.getId());
		current.setGroup(this.gA);
		AthleteRepository.save(current);
		assertFalse(RegistrationEditCheck.requiresMetadataForm(row));
		assertTrue(RegistrationEditCheck.requiresMetadataForm(AthleteRepository.findById(row.getId())));
	}

	@Test
	public void stopsResetsBreaksAndReloadsDoNotEndTheSession() {
		startClock(this.gA);
		post(new FOPEvent.TimeStopped(this));
		assertInProgress(this.gA);
		post(new FOPEvent.ForceTime(Competition.athleteTimerTwoMinutes, this));
		assertInProgress(this.gA);
		post(new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, this));
		assertInProgress(this.gA);
		select(this.gA);
		assertInProgress(this.gA);
	}

	@Test
	public void unselectionAllowsEditingAndReselectionWithoutCompletedAttemptsKeepsItAllowed() {
		startClock(this.gA);
		select(null);
		assertNotInProgress(this.gA);
		select(this.gA);
		assertNotInProgress(this.gA);
	}

	@Test
	public void switchingToAnotherSessionStartsItAsNotInProgress() {
		startClock(this.gA);
		select(this.gB);
		assertNotInProgress(this.gA);
		assertNotInProgress(this.gB);
	}

	@Test
	public void selectingASessionWithACompletedAttemptMakesItInProgress() {
		for (String result : List.of("60", "-60")) {
			select(null);
			Athlete lifter = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true).get(0);
			lifter.setValidation(false);
			lifter.setSnatch1Declaration("60");
			lifter.setSnatch1ActualLift(result);
			AthleteRepository.save(lifter);
			select(this.gA);
			assertInProgress(this.gA);
		}
	}

	@Test
	public void storedAndProposedSessionAreBothCheckedById() {
		startClock(this.gA);
		Group sameId = new Group("Renamed");
		sameId.setId(this.gA.getId());
		assertSame(this.fop, OwlcmsFactory.getFOPSessionInProgress(sameId));
		assertNull(OwlcmsFactory.getFOPSessionInProgress(this.gB));
		assertSame(this.fop, OwlcmsFactory.getFOPSessionInProgress(this.gB, sameId));
		assertSame(this.fop, OwlcmsFactory.getFOPSessionInProgress(sameId, this.gB));
		assertNull(OwlcmsFactory.getFOPSessionInProgress(this.gB, null));
	}

	@Test
	public void registrationSaveIsRefusedForEitherStoredOrProposedSessionInProgress() {
		startClock(this.gA);
		for (boolean movingOut : List.of(true, false)) {
			Athlete form = new Athlete();
			form.setGroup(movingOut ? this.gA : this.gB);
			AthleteRepository.save(form);
			RegistrationEditCheck check = RegistrationEditCheck.open(form);
			form.setGroup(movingOut ? this.gB : this.gA);
			RegistrationEditCheck.SaveResult result = check.trySave(form, () -> true,
			        candidate -> { throw new AssertionError("Session in progress must not be saved"); });
			assertSame(this.fop, result.lifting());
			assertFalse(result.saved());
		}
	}

	@Test
	public void unselectedSessionAllowsRegistrationWithoutOverwritingCardData() {
		Athlete form = new Athlete();
		form.setGroup(this.gA);
		AthleteRepository.save(form);
		select(this.gA);
		form = AthleteRepository.findById(form.getId());
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		startClock(this.gA);
		assertSame(this.fop, check.trySave(form, () -> true,
		        candidate -> { throw new AssertionError("Session is in progress"); }).lifting());
		Athlete current = check.loadCurrent();
		current.setValidation(false);
		current.setSnatch1Declaration("90");
		current.setSnatch1ActualLift("90");
		AthleteRepository.save(current);
		select(null);
		RegistrationEditCheck.SaveResult saved = check.trySave(form, () -> true, AthleteRepository::save);
		assertFalse(saved.stale());
		assertTrue(saved.saved());
		assertEquals("90", check.loadCurrent().getSnatch1ActualLift());
		assertEquals("90", check.loadCurrent().getSnatch1Declaration());
		Athlete reopened = check.loadCurrent();
		assertTrue(RegistrationEditCheck.open(reopened).trySave(reopened, () -> true, candidate -> candidate).saved());
	}

	private void assertInProgress(Group group) {
		assertTrue(this.fop.isSessionInProgress());
		assertSame(this.fop, OwlcmsFactory.getFOPSessionInProgress(group));
	}

	private void assertNotInProgress(Group group) {
		assertNull(OwlcmsFactory.getFOPSessionInProgress(group));
	}

	private void post(FOPEvent event) {
		this.fop.fopEventPost(event);
	}

	private void select(Group group) {
		post(new FOPEvent.SwitchGroup(group, this));
	}

	private void startClock(Group group) {
		select(group);
		post(new FOPEvent.StartLifting(this));
		post(new FOPEvent.TimeStarted(this));
		assertInProgress(group);
	}
}
