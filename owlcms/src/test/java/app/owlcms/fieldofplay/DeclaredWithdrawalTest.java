/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.MockCountdownTimer;
import app.owlcms.tests.TestData;

/** A first attempt requested at 0 or 1 kg withdraws the athlete from that lift when the session is loaded. */
public class DeclaredWithdrawalTest {

	private Group gA;

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
	public void setupData() {
		TestData.insertInitialData(5, true);
		JPAService.runInTransaction(em -> {
			this.gA = GroupRepository.doFindByName("A", em);
			Group gB = GroupRepository.doFindByName("B", em);
			Group gC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 5, this.gA, gB, gC);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);
	}

	@Test
	public void onlyAnExplicitRequestBelowTwoIsAWithdrawal() {
		assertTrue(declared("0", "120").declaresSnatchWithdrawal());
		assertTrue(declared("1", "120").declaresSnatchWithdrawal());
		assertFalse(declared("2", "120").declaresSnatchWithdrawal());
		assertFalse("blank is not a withdrawal", declared(null, "120").declaresSnatchWithdrawal());
		assertFalse(declared("100", "120").declaresCleanJerkWithdrawal());
		assertTrue(declared("100", "1").declaresCleanJerkWithdrawal());

		Athlete raised = declared("0", "120");
		raised.setSnatch1Change1("95");
		assertFalse("a later change replaces the declaration", raised.declaresSnatchWithdrawal());

		Athlete started = declared("100", "0");
		started.setCleanJerk1ActualLift("-120");
		assertFalse("a clean and jerk was already taken", started.declaresCleanJerkWithdrawal());
	}

	@Test
	public void withdrawingFromOneLiftLeavesTheOtherAndTakenAttempts() {
		Athlete athlete = declared("100", "120");
		athlete.setCleanJerk1ActualLift("120");
		athlete.withdrawFromCleanJerk();
		assertEquals("120", athlete.getCleanJerk1ActualLift());
		assertEquals("0", athlete.getCleanJerk2ActualLift());
		assertEquals("0", athlete.getCleanJerk3ActualLift());
		assertEquals(0, athlete.getSnatchAttemptsDone().intValue());

		Athlete fresh = declared("100", "120");
		fresh.withdrawFromSnatch();
		assertEquals(3, fresh.getSnatchAttemptsDone().intValue());
		assertEquals(0, fresh.getCleanJerkAttemptsDone().intValue());
	}

	@Test
	public void loadingTheSessionWithdrawsAndSkipsTheAthlete() {
		List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(this.gA, true);
		Athlete both = athletes.get(0);
		Athlete snatchOnly = athletes.get(1);
		save(both, "0", "1");
		save(snatchOnly, "0", "120");
		for (Athlete other : athletes.subList(2, athletes.size())) {
			save(other, "100", "120");
		}

		FieldOfPlay fop = MockFieldOfPlay.create(AthleteRepository.findAll(), new MockCountdownTimer(),
		        new MockCountdownTimer());
		fop.loadGroup(this.gA, this, true);

		Athlete storedBoth = AthleteRepository.findById(both.getId());
		assertEquals(6, storedBoth.getAttemptsDone().intValue());
		Athlete storedSnatchOnly = AthleteRepository.findById(snatchOnly.getId());
		assertEquals(3, storedSnatchOnly.getSnatchAttemptsDone().intValue());
		assertEquals(0, storedSnatchOnly.getCleanJerkAttemptsDone().intValue());
		assertNotEquals(both.getId(), fop.getCurAthlete().getId());
		assertNotEquals(snatchOnly.getId(), fop.getCurAthlete().getId());
		assertTrue(fop.getCurAthlete().getNextAttemptRequestedWeight() >= 2);
		assertFalse("a declared withdrawal does not start the session", fop.isSessionInProgress());
	}

	private static Athlete declared(String snatch, String cleanJerk) {
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		athlete.setSnatch1Declaration(snatch);
		athlete.setCleanJerk1Declaration(cleanJerk);
		return athlete;
	}

	private static void save(Athlete athlete, String snatch, String cleanJerk) {
		athlete.setValidation(false);
		athlete.setSnatch1Declaration(snatch);
		athlete.setCleanJerk1Declaration(cleanJerk);
		AthleteRepository.save(athlete);
	}
}
