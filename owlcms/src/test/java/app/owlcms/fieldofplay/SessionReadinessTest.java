/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.audit.AuditActor;
import app.owlcms.fieldofplay.SessionReadiness.Evaluation;
import app.owlcms.fieldofplay.SessionReadiness.Status;
import app.owlcms.tests.TestData;
import app.owlcms.tests.TestStartup;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;

/** Tests for {@link SessionReadiness} and {@link FieldOfPlay#switchGroupAndApply} without Vaadin UI objects. */
public class SessionReadinessTest implements BreakController {

	@BeforeClass
	public static void setupTests() {
		TestStartup.init();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Test
	public void classifiesNoSessionDoneNotReadyAndReady() {
		Group session = group("F4", "A", LocalDateTime.of(2026, 11, 21, 17, 30));

		assertEquals(Status.NO_SESSION,
		        SessionReadiness.evaluate(null, List.of(), null).status());

		assertEquals(Status.NOT_READY,
		        SessionReadiness.evaluate(session, List.of(), null).status());

		Athlete unweighed = readyAthlete(session, "60", "80");
		unweighed.setBodyWeight(null);
		assertEquals(Status.NOT_READY,
		        SessionReadiness.evaluate(session, List.of(unweighed), null).status());

		Athlete missingWeights = readyAthlete(session, null, null);
		assertEquals(Status.NOT_READY,
		        SessionReadiness.evaluate(session, List.of(missingWeights), null).status());

		Athlete finished = finishedAthlete(session);
		assertEquals(Status.DONE,
		        SessionReadiness.evaluate(session, List.of(finished), finished).status());

		Athlete ready = readyAthlete(session, "60", "80");
		Evaluation loaded = SessionReadiness.evaluate(session, List.of(ready), ready);
		assertEquals(Status.READY, loaded.status());
		assertFalse(loaded.needsReload());

		Evaluation staleNull = SessionReadiness.evaluate(session, List.of(ready), null);
		assertEquals(Status.READY, staleNull.status());
		assertTrue("weigh-in added after load requires reload", staleNull.needsReload());

		Evaluation staleDone = SessionReadiness.evaluate(session, List.of(ready), finished);
		assertEquals(Status.READY, staleDone.status());
		assertTrue("stale finished curAthlete requires reload", staleDone.needsReload());

		Athlete blankBeforeStart = readyAthlete(session, null, null);
		Evaluation notStarted = SessionReadiness.evaluate(session, List.of(ready, blankBeforeStart),
		        blankBeforeStart);
		assertTrue("blank current athlete before lifting starts requires reload", notStarted.needsReload());

		Athlete snatchesDone = readyAthlete(session, "60", "80");
		snatchesDone.setSnatch1ActualLift("60");
		snatchesDone.setSnatch2ActualLift("61");
		snatchesDone.setSnatch3ActualLift("62");
		Athlete blankCj = readyAthlete(session, "60", null);
		blankCj.setSnatch1ActualLift("60");
		blankCj.setSnatch2ActualLift("61");
		blankCj.setSnatch3ActualLift("62");
		Evaluation midSession = SessionReadiness.evaluate(session, List.of(snatchesDone, blankCj), blankCj);
		assertEquals(Status.READY, midSession.status());
		assertFalse("blank C&J declaration after lifting started must not force a reload",
		        midSession.needsReload());

		Evaluation cjBreakAllBlank = SessionReadiness.evaluate(session, List.of(blankCj), blankCj);
		assertEquals("C&J weights still being entered must not block a session under way", Status.READY,
		        cjBreakAllBlank.status());
		assertFalse(cjBreakAllBlank.needsReload());

		Evaluation midSessionNotLoaded = SessionReadiness.evaluate(session, List.of(blankCj), null);
		assertTrue("session under way with no current athlete loaded requires reload",
		        midSessionNotLoaded.needsReload());
	}

	@Test
	public void handlesDeclaredWithdrawalsBeforeGroupLoad() {
		Group session = group("F4", "A", LocalDateTime.of(2026, 11, 21, 17, 30));

		Athlete snatchWithdrawnOnly = readyAthlete(session, "0", "80");
		assertTrue(SessionReadiness.hasReadyLift(snatchWithdrawnOnly));
		assertEquals(Status.READY,
		        SessionReadiness.evaluate(session, List.of(snatchWithdrawnOnly), null).status());

		Athlete bothWithdrawn = readyAthlete(session, "0", "1");
		assertFalse(SessionReadiness.hasReadyLift(bothWithdrawn));
		assertEquals(Status.NOT_READY,
		        SessionReadiness.evaluate(session, List.of(bothWithdrawn), null).status());
	}

	@Test
	public void ordersCurrentPlatformSessionsFirstThenOtherPlatformsBySchedule() {
		LocalDateTime t1 = LocalDateTime.of(2026, 11, 21, 10, 0);
		LocalDateTime t2 = LocalDateTime.of(2026, 11, 21, 12, 0);
		LocalDateTime t3 = LocalDateTime.of(2026, 11, 21, 14, 0);

		Group bEarly = group("B1", "B", t1);
		Group aLate = group("A2", "A", t3);
		Group unassigned = group("U1", null, t1);
		Group aEarly = group("A1", "A", t2);

		List<Group> ordered = SessionReadiness.orderCandidates(List.of(bEarly, aLate, unassigned, aEarly), "A");

		assertEquals(List.of("A1", "A2", "B1", "U1"), ordered.stream().map(Group::getName).toList());
	}

	@Test
	public void groupsSectionsByCurrentPlatformOtherPlatformsThenFinishedInStartingOrder() {
		LocalDateTime t07 = LocalDateTime.of(2026, 11, 21, 7, 0);
		LocalDateTime t08 = LocalDateTime.of(2026, 11, 21, 8, 0);
		LocalDateTime t09 = LocalDateTime.of(2026, 11, 21, 9, 0);
		LocalDateTime t10 = LocalDateTime.of(2026, 11, 21, 10, 0);
		LocalDateTime t11 = LocalDateTime.of(2026, 11, 21, 11, 0);
		LocalDateTime t13 = LocalDateTime.of(2026, 11, 21, 13, 0);
		LocalDateTime t14 = LocalDateTime.of(2026, 11, 21, 14, 0);

		Group b1 = scheduledGroup("B1", "B", t10, false, null);
		Group b2 = scheduledGroup("B2", "B", t14, false, "Men 88");
		Group a1 = scheduledGroup("A1", "A", t09, false, null);
		Group a2 = scheduledGroup("A2", "A", t13, false, null);
		Group c1 = scheduledGroup("C1", "C", t09, false, null);
		Group c2 = scheduledGroup("C2", "C", t11, false, null);
		Group u1 = scheduledGroup("U1", null, t08, false, null);
		Group finB = scheduledGroup("B0", "B", t08, true, null);
		Group finA = scheduledGroup("A0", "A", t08, true, null);
		Group finC = scheduledGroup("C0", "C", t07, true, null);

		List<List<Group>> sections = SessionSelectionOrder.groupSections(
		        List.of(c2, finB, a2, b2, u1, finA, c1, b1, finC, a1), "B", Group::isDone);

		List<List<String>> sectionNames = sections.stream()
		        .map(s -> s.stream().map(Group::getName).toList())
		        .toList();
		assertEquals(List.of(
		        List.of("B1", "B2"),
		        List.of("A1", "A2"),
		        List.of("C1", "C2"),
		        List.of("U1"),
		        List.of("C0", "A0", "B0")), sectionNames);

		// Ready-only predicate (no finished section) omits empty sections cleanly
		List<List<Group>> noCurrentPlatform = SessionSelectionOrder.groupSections(
		        List.of(c1, a1), "B", g -> false);
		assertEquals(List.of(List.of("A1"), List.of("C1")), noCurrentPlatform.stream()
		        .map(s -> s.stream().map(Group::getName).toList())
		        .toList());
	}

	@Test
	public void formatsSessionLabelsForSingleAndMultiplePlatforms() {
		LocalDateTime t10 = LocalDateTime.of(2026, 11, 21, 10, 0);
		Group plain = scheduledGroup("M1", "A", t10, false, null);
		Group withDesc = scheduledGroup("M2", "A", t10, false, "Men 79");
		Group unassigned = scheduledGroup("U1", null, t10, false, "Open");
		Group otherPlatform = scheduledGroup("F1", "B", t10, false, null);

		assertFalse(SessionSelectionOrder.hasMultiplePlatforms(List.of(plain, withDesc), 1));
		assertTrue(SessionSelectionOrder.hasMultiplePlatforms(List.of(plain, withDesc), 2));
		assertTrue(SessionSelectionOrder.hasMultiplePlatforms(List.of(plain, otherPlatform), 1));

		assertEquals("M1", SessionSelectionOrder.formatLabel(plain, false));
		assertEquals("M2 - Men 79", SessionSelectionOrder.formatLabel(withDesc, false));
		assertEquals("A - M1", SessionSelectionOrder.formatLabel(plain, true));
		assertEquals("A - M2 - Men 79", SessionSelectionOrder.formatLabel(withDesc, true));
		assertEquals("U1 - Open", SessionSelectionOrder.formatLabel(unassigned, true));
	}

	@Test
	public void clearingLastAthletesLastAttemptReopensCompletedSession() throws Exception {
		TestData.insertInitialData(2, true);
		Group session = JPAService.runInTransaction(em -> {
			Group a = GroupRepository.doFindByName("A", em);
			Group b = GroupRepository.doFindByName("B", em);
			Group c = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 2, a, b, c);
			return a;
		});
		AthleteRepository.resetParticipations(false, true);
		List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(session, true);
		assertEquals(2, athletes.size());
		for (Athlete athlete : athletes) {
			athlete.setValidation(false);
			athlete.setSnatch1ActualLift("60");
			athlete.setSnatch2ActualLift("61");
			athlete.setSnatch3ActualLift("62");
			athlete.setCleanJerk1ActualLift("80");
			athlete.setCleanJerk2ActualLift("81");
			athlete.setCleanJerk3ActualLift("82");
			AthleteRepository.save(athlete);
		}

		FieldOfPlay fop = MockFieldOfPlay.createWithProductionTimers("A", session,
		        AthleteRepository.findAllByGroupAndWeighIn(session, true));
		try {
			assertEquals(FOPState.BREAK, fop.getState());
			assertEquals(BreakType.GROUP_DONE, fop.getBreakType());
			assertTrue(GroupRepository.findByName("A").isDone());
			assertEquals(Status.DONE, SessionReadiness.evaluate(fop).status());
			assertFalse(SessionReadiness.findReadySessions(fop).contains(session));

			Athlete lastAthlete = athletes.get(athletes.size() - 1);
			lastAthlete.setCleanJerk3ActualLift("");
			AthleteRepository.save(lastAthlete);

			assertTrue("the persisted done marker is still set", GroupRepository.findByName("A").isDone());
			assertEquals("the platform still shows completion", BreakType.GROUP_DONE, fop.getBreakType());
			Evaluation reopened = SessionReadiness.evaluate(fop);
			assertEquals(Status.READY, reopened.status());
			assertTrue("the platform must reload the cleared attempt", reopened.needsReload());
			assertTrue(SessionReadiness.findReadySessions(fop).contains(session));

			fop.switchGroupAndApply(session, this, AuditActor.system(), reopened.needsReload(),
			        () -> fop.startCountdown(BreakType.FIRST_CJ, 600_000, this));

			assertEquals(BreakType.FIRST_CJ, fop.getBreakType());
			assertEquals(lastAthlete.getId(), fop.getCurAthlete().getId());
			assertEquals(Integer.valueOf(5), fop.getCurAthlete().getAttemptsDone());
			assertTrue(fop.getCurAthlete().getNextAttemptRequestedWeight() > 0);
			assertFalse(GroupRepository.findByName("A").isDone());
		} finally {
			fop.getAthleteTimer().stop();
			fop.getBreakTimer().stop();
		}
	}

	@Test
	public void switchGroupAndApplyFromFinishedSessionStartsCountdownWithoutInactiveEvent() throws Exception {
		TestData.insertInitialData(3, true);
		Group[] groups = new Group[2];
		JPAService.runInTransaction(em -> {
			groups[0] = GroupRepository.doFindByName("A", em);
			groups[1] = GroupRepository.doFindByName("B", em);
			Group gC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 3, groups[0], groups[1], gC);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);

		// Mark every athlete in session A finished so A is in GROUP_DONE.
		for (Athlete a : AthleteRepository.findAllByGroupAndWeighIn(groups[0], true)) {
			a.setValidation(false);
			a.setSnatch1ActualLift("60");
			a.setSnatch2ActualLift("61");
			a.setSnatch3ActualLift("62");
			a.setCleanJerk1ActualLift("80");
			a.setCleanJerk2ActualLift("81");
			a.setCleanJerk3ActualLift("82");
			AthleteRepository.save(a);
		}
		for (Athlete b : AthleteRepository.findAllByGroupAndWeighIn(groups[1], true)) {
			b.setValidation(false);
			b.setSnatch1Declaration("55");
			b.setCleanJerk1Declaration("70");
			AthleteRepository.save(b);
		}

		FieldOfPlay fop = MockFieldOfPlay.createWithProductionTimers("A", groups[0],
		        AthleteRepository.findAllByGroupAndWeighIn(groups[0], true));
		assertEquals(FOPState.BREAK, fop.getState());
		assertEquals(BreakType.GROUP_DONE, fop.getBreakType());
		assertEquals(Status.DONE, SessionReadiness.evaluate(fop).status());

		List<UIEvent.SwitchGroup> switches = new CopyOnWriteArrayList<>();
		List<UIEvent.LiftingOrderUpdated> orders = new CopyOnWriteArrayList<>();
		List<UIEvent.BreakStarted> breaks = new CopyOnWriteArrayList<>();
		CountDownLatch switchLatch = new CountDownLatch(1);
		CountDownLatch orderLatch = new CountDownLatch(1);
		CountDownLatch breakLatch = new CountDownLatch(1);

		Object recorder = new Object() {
			@Subscribe
			public void onSwitch(UIEvent.SwitchGroup e) {
				switches.add(e);
				switchLatch.countDown();
			}

			@Subscribe
			public void onOrder(UIEvent.LiftingOrderUpdated e) {
				orders.add(e);
				orderLatch.countDown();
			}

			@Subscribe
			public void onBreak(UIEvent.BreakStarted e) {
				breaks.add(e);
				breakLatch.countDown();
			}
		};
		fop.getUiEventBus().register(recorder);
		try {
			fop.switchGroupAndApply(groups[1], this, AuditActor.system(), false,
			        () -> fop.startCountdown(BreakType.FIRST_SNATCH, 600_000, this));

			assertTrue("SwitchGroup event delivered", switchLatch.await(3, TimeUnit.SECONDS));
			assertTrue("LiftingOrderUpdated event delivered", orderLatch.await(3, TimeUnit.SECONDS));
			assertTrue("BreakStarted event delivered", breakLatch.await(3, TimeUnit.SECONDS));

			assertEquals(FOPState.BREAK, fop.getState());
			assertEquals(BreakType.FIRST_SNATCH, fop.getBreakType());
			assertEquals(groups[1], fop.getGroup());
			assertNotNull(fop.getCurAthlete());
			assertTrue(fop.getCurAthlete().getNextAttemptRequestedWeight() > 0);

			for (UIEvent.SwitchGroup sg : switches) {
				assertEquals("SwitchGroup must carry final BREAK state, never INACTIVE", FOPState.BREAK, sg.getState());
				assertEquals(groups[1], sg.getGroup());
				assertNotNull(sg.getAthlete());
			}
			for (UIEvent.LiftingOrderUpdated lou : orders) {
				assertTrue("LiftingOrderUpdated must carry break state, never INACTIVE", lou.isInBreak());
				assertNotNull(lou.getAthlete());
				assertTrue(lou.getAthlete().getNextAttemptRequestedWeight() > 0);
			}
			assertTrue(breaks.stream().anyMatch(b -> b.getBreakType() == BreakType.FIRST_SNATCH));
		} finally {
			fop.getBreakTimer().stop();
			fop.getUiEventBus().unregister(recorder);
		}
	}

	@Test
	public void switchGroupAndApplyStartsLiftingWithoutInactiveEvent() throws Exception {
		TestData.insertInitialData(2, true);
		Group[] groups = new Group[1];
		JPAService.runInTransaction(em -> {
			Group gA = GroupRepository.doFindByName("A", em);
			groups[0] = GroupRepository.doFindByName("B", em);
			Group gC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 2, gA, groups[0], gC);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);
		for (Athlete b : AthleteRepository.findAllByGroupAndWeighIn(groups[0], true)) {
			b.setValidation(false);
			b.setSnatch1Declaration("50");
			b.setCleanJerk1Declaration("65");
			AthleteRepository.save(b);
		}

		FieldOfPlay fop = MockFieldOfPlay.createWithProductionTimers("A", null, List.of());
		assertEquals(Status.NO_SESSION, SessionReadiness.evaluate(fop).status());

		List<UIEvent.SwitchGroup> switches = new CopyOnWriteArrayList<>();
		CountDownLatch switchLatch = new CountDownLatch(1);
		CountDownLatch startLatch = new CountDownLatch(1);

		Object recorder = new Object() {
			@Subscribe
			public void onSwitch(UIEvent.SwitchGroup e) {
				switches.add(e);
				switchLatch.countDown();
			}

			@Subscribe
			public void onStart(UIEvent.StartLifting e) {
				startLatch.countDown();
			}
		};
		fop.getUiEventBus().register(recorder);
		try {
			fop.switchGroupAndApply(groups[0], this, AuditActor.system(), false,
			        () -> fop.fopEventPost(new FOPEvent.StartLifting(this)));

			assertTrue("SwitchGroup event delivered", switchLatch.await(3, TimeUnit.SECONDS));
			assertTrue("StartLifting event delivered", startLatch.await(3, TimeUnit.SECONDS));

			assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, fop.getState());
			assertEquals(groups[0], fop.getGroup());
			for (UIEvent.SwitchGroup sg : switches) {
				assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, sg.getState());
				assertEquals(groups[0], sg.getGroup());
			}
		} finally {
			fop.getUiEventBus().unregister(recorder);
		}
	}

	private static Group group(String name, String platformName, LocalDateTime weighInTime) {
		Group g = new Group();
		g.setName(name);
		g.setWeighInTime(weighInTime);
		if (platformName != null) {
			Platform p = new Platform();
			p.setName(platformName);
			g.setPlatform(p);
		}
		return g;
	}

	private static Group scheduledGroup(String name, String platformName, LocalDateTime competitionTime,
	        boolean done, String description) {
		Group g = group(name, platformName, competitionTime != null ? competitionTime.minusHours(2) : null);
		g.setCompetitionTime(competitionTime);
		g.setDone(done);
		g.setDescription(description);
		return g;
	}

	private static Athlete readyAthlete(Group session, String snatch1, String cj1) {
		Athlete a = new Athlete();
		a.setGroup(session);
		a.setBodyWeight(70.0);
		Category cat = new Category();
		cat.setCode("M71");
		a.setCategory(cat);
		a.setSnatch1Declaration(snatch1);
		a.setCleanJerk1Declaration(cj1);
		return a;
	}

	private static Athlete finishedAthlete(Group session) {
		Athlete a = readyAthlete(session, "60", "80");
		a.setSnatch1ActualLift("60");
		a.setSnatch2ActualLift("61");
		a.setSnatch3ActualLift("62");
		a.setCleanJerk1ActualLift("80");
		a.setCleanJerk2ActualLift("81");
		a.setCleanJerk3ActualLift("82");
		return a;
	}
}
