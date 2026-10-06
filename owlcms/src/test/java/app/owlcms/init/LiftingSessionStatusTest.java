/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.RegistrationEditCheck;
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
 * unselected. Registration and results editing are refused while it is in progress.
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
	public void selectedSessionAllowsEditingUntilTheFirstClockStart() {
		select(this.gA);
		assertNotInProgress(this.gA);
		post(new FOPEvent.StartLifting(this));
		assertNotInProgress(this.gA);
		post(new FOPEvent.TimeStarted(this));
		assertInProgress(this.gA);
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
			        () -> { throw new AssertionError("Session in progress must not be saved"); });
			assertSame(this.fop, result.lifting());
			assertFalse(result.saved());
		}
	}

	@Test
	public void anOldRegistrationFormIsStillRefusedAfterThePlatformUnselects() {
		Athlete form = new Athlete();
		form.setGroup(this.gA);
		AthleteRepository.save(form);
		RegistrationEditCheck check = RegistrationEditCheck.open(form);
		startClock(this.gA);
		assertSame(this.fop, check.trySave(form, () -> true,
		        () -> { throw new AssertionError("Session is in progress"); }).lifting());
		Athlete current = check.loadCurrent();
		current.setValidation(false);
		current.setSnatch1Declaration("90");
		current.setSnatch1ActualLift("90");
		AthleteRepository.save(current);
		select(null);
		RegistrationEditCheck.SaveResult refused = check.trySave(form, () -> true,
		        () -> { throw new AssertionError("Unselection must not make old hidden results saveable"); });
		assertTrue(refused.stale());
		assertFalse(refused.saved());
		Athlete reopened = check.loadCurrent();
		assertTrue(RegistrationEditCheck.open(reopened).trySave(reopened, () -> true, () -> {}).saved());
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
