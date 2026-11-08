/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.config.Config;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;

public class RegistrationEditCheckTest {
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

	private static Athlete athlete() {
		Athlete athlete = new Athlete();
		athlete.setLastName("Registration");
		athlete.setSnatch1Declaration("90");
		return athlete;
	}

	@Test
	public void unchangedStoredDataIsNotStale() {
		Athlete opened = athlete();
		assertFalse(RegistrationEditCheck.open(opened).isStale(athlete()));
	}

	@Test
	public void ownUnsavedEditsDoNotMakeStoredDataStale() {
		Athlete form = athlete();
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		form.setCoach("My edit");
		form.setSnatch1Declaration("95");
		assertFalse(check.isStale(athlete()));
	}

	@Test
	public void evenMatchingEditsDoNotPermitAStaleRegistrationSave() {
		Athlete form = athlete();
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		form.setCoach("Same coach");
		Athlete stored = athlete();
		stored.setCoach("Same coach");
		assertTrue(check.isStale(stored));
	}

	@Test
	public void cardResultsDoNotMakeRegistrationStale() {
		Athlete form = athlete();
		AthleteRepository.save(form);
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		Athlete lifted = AthleteRepository.findById(form.getId());
		lifted.setValidation(false);
		lifted.setSnatch1ActualLift("90");
		AthleteRepository.save(lifted);
		// Selection belongs to the FOP, not the athlete. Unselecting cannot change this database comparison.
		assertFalse(check.isStale(check.loadCurrent()));
		assertFalse(RegistrationEditCheck.open(check.loadCurrent()).isStale(check.loadCurrent()));
	}

	@Test
	public void registrationAndHiddenMetadataChangesAreDetected() {
		List<Consumer<Athlete>> changes = List.of(
		        a -> a.setTeam("New team"),
		        a -> a.setFirstName("Other first name"),
		        a -> a.setLastName("Other last name"),
		        a -> a.setFederationCodes("IWF,EWF"),
		        a -> a.setMetadataUpdatedAt(7L),
		        a -> a.setMembership("New membership"),
		        a -> a.setScaleWeight(80.1),
		        a -> a.setQualifyingTotal(200),
		        a -> a.setSubCategory("Sub"),
		        a -> a.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.values()[0]),
		        a -> a.setPersonalBestSnatch(100),
		        a -> a.setPersonalBestCleanJerk(120),
		        a -> a.setPersonalBestTotal(220));
		for (Consumer<Athlete> change : changes) {
			Athlete stored = athlete();
			stored.setValidation(false);
			RegistrationEditCheck check = RegistrationEditCheck.open(stored);
			change.accept(stored);
			assertTrue(check.isStale(stored));
		}
	}

	@Test
	public void cardAndAdjudicationChangesAreOutsideRegistration() {
		List<Consumer<Athlete>> changes = List.of(
		        a -> a.setCustomScore(4.0),
		        a -> a.setSnatch2Change1("95"),
		        a -> a.setCleanJerk3ActualLift("-120"),
		        a -> a.setSnatch1Decisions(new ArrayList<>(Arrays.asList(true, null, false))),
		        a -> a.setSnatch1Challenge(true),
		        a -> a.setSnatch1JuryDeliberation(true),
		        a -> a.setSnatch1LiftTime(LocalDateTime.of(2026, 10, 6, 15, 0)));
		for (Consumer<Athlete> change : changes) {
			Athlete stored = athlete();
			stored.setValidation(false);
			RegistrationEditCheck check = RegistrationEditCheck.open(stored);
			change.accept(stored);
			assertFalse(check.isStale(stored));
		}
	}

	@Test
	public void decisionListsAreNotRegistrationInputs() {
		Athlete stored = athlete();
		stored.setSnatch1Decisions(new ArrayList<>(Arrays.asList(true, null, false)));
		RegistrationEditCheck check = RegistrationEditCheck.open(stored);
		stored.getSnatch1Decisions().set(1, true);
		assertFalse(check.isStale(stored));
	}

	@Test
	public void teamMembershipChangesAreDetectedButRankChangesAreIgnored() {
		Athlete stored = athlete();
		Category category = new Category();
		category.setId(123L);
		category.setCode("TEST");
		Participation participation = new Participation(stored, category);
		stored.setParticipations(new ArrayList<>(List.of(participation)));
		RegistrationEditCheck check = RegistrationEditCheck.open(stored);
		participation.setBestAthleteRank(2);
		participation.setSnatchRank(1);
		stored.setTeamSinclairRank(3);
		assertFalse(check.isStale(stored));
		participation.setTeamMember(false);
		assertTrue(check.isStale(stored));
	}

	@Test
	public void changingTheSessionIdIsDetectedEvenWithTheSameName() {
		Athlete stored = athlete();
		Group group = new Group("A");
		group.setId(10L);
		stored.setGroup(group);
		RegistrationEditCheck check = RegistrationEditCheck.open(stored);
		Group other = new Group("A");
		other.setId(11L);
		stored.setGroup(other);
		assertTrue(check.isStale(stored));
	}

	@Test
	public void staleSaveDoesNotExecuteTheRegistrationWrite() {
		Athlete form = athlete();
		AthleteRepository.save(form);
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		Athlete current = check.loadCurrent();
		current.setCoach("Director");
		AthleteRepository.save(current);
		AtomicInteger writes = new AtomicInteger();
		RegistrationEditCheck.SaveResult result = check.trySave(form, () -> true, candidate -> {
			writes.incrementAndGet();
			return candidate;
		});
		assertTrue(result.stale());
		assertFalse(result.saved());
		assertEquals(0, writes.get());
	}

	@Test
	public void freshFormSavesItsOwnEdits() {
		Athlete form = athlete();
		AthleteRepository.save(form);
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		form.setCoach("My new coach");
		RegistrationEditCheck.SaveResult result = check.trySave(form, () -> true,
		        AthleteRepository::save);
		assertTrue(result.saved());
		assertFalse(result.stale());
		assertEquals("My new coach", check.loadCurrent().getCoach());
	}

	@Test
	public void closedRegistrationDoesNotReadOrWrite() {
		Athlete form = athlete();
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		RegistrationEditCheck.SaveResult result = check.trySave(form, () -> false,
		        candidate -> { throw new AssertionError("Closed form must not save"); });
		assertFalse(result.saved());
	}

	@Test
	public void independentDraftsSaveInEitherOrderWithoutReopening() {
		for (boolean cardFirst : List.of(true, false)) {
			Athlete opened = AthleteRepository.save(athlete());
			Athlete registration = AthleteRepository.findById(opened.getId());
			Athlete card = AthleteRepository.findById(opened.getId());
			RegistrationEditCheck registrationCheck = RegistrationEditCheck.open(registration);
			ConcurrentEditCheck cardCheck = ConcurrentEditCheck.open(card);
			registration.setLastName("Corrected registration");
			registration.setCoach("New coach");
			card.setSnatch1ActualLift("90");
			Runnable saveRegistration = () -> assertTrue(registrationCheck.trySave(registration, () -> true,
			        AthleteRepository::save).saved());
			Runnable saveCard = () -> assertTrue(cardCheck.trySave(card, null, () -> true, fop -> {
				Athlete current = AthleteRepository.findById(opened.getId());
				AthleteCardValues.copyTo(card, current);
				AthleteRepository.saveNoMetadata(current);
			}).saved());
			if (cardFirst) {
				saveCard.run();
				assertEquals("Registration", AthleteRepository.findById(opened.getId()).getLastName());
				saveRegistration.run();
			} else {
				saveRegistration.run();
				assertEquals("", AthleteRepository.findById(opened.getId()).getSnatch1ActualLift());
				saveCard.run();
			}
			Athlete stored = AthleteRepository.findById(opened.getId());
			assertEquals("Corrected registration", stored.getLastName());
			assertEquals("New coach", stored.getCoach());
			assertEquals("90", stored.getSnatch1ActualLift());
			registration.setMetadataUpdatedAt(stored.getMetadataUpdatedAt());
			registration.setCoach("Second save");
			assertTrue(registrationCheck.trySave(registration, () -> true, AthleteRepository::save).saved());
			assertEquals("90", AthleteRepository.findById(opened.getId()).getSnatch1ActualLift());
		}
	}

	@Test
	public void registrationPreservesNewDecisionsAndUntouchedSharedDeclaration() {
		Athlete opened = AthleteRepository.save(athlete());
		RegistrationEditCheck check = RegistrationEditCheck.open(opened);
		Athlete current = check.loadCurrent();
		current.setSnatch1Declaration("95");
		current.setSnatch1ActualLift("95");
		current.setSnatch1Decisions(new ArrayList<>(Arrays.asList(true, false, true)));
		current.setSnatch1LiftTime(LocalDateTime.of(2026, 11, 7, 12, 0));
		AthleteRepository.saveNoMetadata(current);
		opened.setCoach("Registration only");
		assertTrue(check.trySave(opened, () -> true, AthleteRepository::save).saved());
		Athlete saved = check.loadCurrent();
		assertEquals("95", saved.getSnatch1Declaration());
		assertEquals("95", saved.getSnatch1ActualLift());
		assertEquals(current.getSnatch1Decisions(), saved.getSnatch1Decisions());
		assertEquals(current.getSnatch1LiftTime(), saved.getSnatch1LiftTime());
	}

	@Test
	public void competingEditsToSharedDeclarationStillConflict() {
		Athlete form = AthleteRepository.save(athlete());
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		Athlete stored = check.loadCurrent();
		stored.setSnatch1Declaration("95");
		AthleteRepository.saveNoMetadata(stored);
		form.setSnatch1Declaration("97");
		assertTrue(check.trySave(form, () -> true, candidate -> {
			throw new AssertionError("Conflicting declaration must not save");
		}).stale());
	}
}
