/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.LiftDefinition.Stage;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.tests.SlowCategory;
import app.owlcms.tests.TestData;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;

/**
 * Two platforms lift at the same time, each with its own OBS monitor. Each monitor must report the states of its own
 * platform only, with the lift type of the lift each state is about. The platforms run as in a competition:
 * asynchronous event buses, production clocks, real (shortened) decision timers; no Vaadin objects.
 * <p>
 * Each test adds a section to {@code target/obs-monitor-report.html}: what was done on each platform, the platform's
 * state after each action, and the titles the monitor published, compared with those expected.
 */
@Category(SlowCategory.class)
public class OBSMonitorStatusTest {

	private static final boolean W = true;
	private static final boolean R = false;
	private static final long DOWN_SIGNAL_VISIBLE_MS = 300;
	private static final int REVERSAL_DELAY_MS = 400;
	private static final long DECISION_VISIBLE_MS = 800;
	private static final long TITLE_WAIT_MS = 5_000;

	/** The four situations a monitor has to report during the snatch. */
	private enum Lift {
		/** 3 referees and 4 jurors (Adjudication Panel), the jury agrees with the referees */
		PANEL_NO_REVIEW,
		/** 3 referees and 4 jurors, the jury opposes the referees: automatic review, the good lift becomes a no lift */
		PANEL_REVERSAL,
		/** 3 referees decide, the 3-member jury agrees */
		REFEREES_NO_REVIEW,
		/** 3 referees decide, the Jury Chair starts a deliberation: the no lift becomes a good lift */
		JURY_REVIEW
	}

	/** The juries for the last snatch and the last clean and jerk. */
	private enum Jury {
		/** jury size 0: the referees' decision is final */
		NONE,
		/** 3-member jury, the Chair starts a deliberation during the break: the no lift becomes a good lift */
		THREE_MEMBER_REVIEW,
		/** 3 referees and 4 jurors, the jury votes during the break: automatic review, the good lift becomes a no lift */
		PANEL_REVIEW;

		Jury next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private static final Path REPORT = Path.of("target", "obs-monitor-report.html");
	private static final List<String> reportSections = new ArrayList<>();

	private static Group groupA;
	private static Group groupB;

	private Station a;
	private Station b;
	private String situation;

	@Rule
	public final TestWatcher report = new TestWatcher() {
		@Override
		protected void succeeded(Description description) {
			writeReport(description, null);
		}

		@Override
		protected void failed(Throwable e, Description description) {
			writeReport(description, e);
		}
	};

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
			groupA = GroupRepository.doFindByName("A", em);
			groupB = GroupRepository.doFindByName("B", em);
			Group groupC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 5, groupA, groupB, groupC);
			// the sample athletes are all in the same category; two platforms never lift the same category at the
			// same time, so session B gets its own
			var m73 = CategoryRepository.doFindByCode("Open_M73", em);
			for (Athlete athlete : AthleteRepository.doFindAllByGroupAndWeighIn(em, groupB, null, (Gender) null)) {
				athlete.setBodyWeight(72.0);
				athlete.computeCategory(m73);
				em.merge(athlete);
			}
			return null;
		});
		AthleteRepository.resetParticipations(false, true);
		setSwitch(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, false);
		setSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT, false);
		Competition.getCurrent().setVideoPlaybackTechnology(true);
	}

	@After
	public void stopPlatforms() {
		for (Station station : new Station[] { this.a, this.b }) {
			if (station != null) {
				// stop recording first: what the platform does while it is shut down is not part of the test
				station.fop.getUiEventBus().unregister(station);
				station.fop.setTestingMode(true);
				station.fop.unregister();
			}
		}
		Competition.getCurrent().setVideoPlaybackTechnology(false);
	}

	@Test
	public void eachMonitorReportsItsOwnPlatformWhileBothAreLifting() throws Exception {
		startPlatforms("Each platform: a session of 5 athletes, all at 60 kg for their first snatch.",
		        OBSMonitorStatusTest::firstSnatch, Stage.SNATCH);

		// the same four situations in opposite orders, so that the platforms are never in the same one
		runConcurrently(
		        () -> run(this.a, Lift.PANEL_NO_REVIEW, Lift.PANEL_REVERSAL, Lift.REFEREES_NO_REVIEW, Lift.JURY_REVIEW),
		        () -> run(this.b, Lift.JURY_REVIEW, Lift.REFEREES_NO_REVIEW, Lift.PANEL_REVERSAL, Lift.PANEL_NO_REVIEW));
	}

	// the three juries on the last snatch; each test runs a different one on the other platform at the same time

	@Test
	public void lastSnatchWithoutJury() throws Exception {
		lastLiftOnBothPlatforms(OBSMonitorStatusTest::lastSnatch, Stage.SNATCH, BreakType.FIRST_CJ, Jury.NONE);
	}

	@Test
	public void lastSnatchWithThreeMemberJuryReview() throws Exception {
		lastLiftOnBothPlatforms(OBSMonitorStatusTest::lastSnatch, Stage.SNATCH, BreakType.FIRST_CJ,
		        Jury.THREE_MEMBER_REVIEW);
	}

	@Test
	public void lastSnatchWithAdjudicationPanelReview() throws Exception {
		lastLiftOnBothPlatforms(OBSMonitorStatusTest::lastSnatch, Stage.SNATCH, BreakType.FIRST_CJ,
		        Jury.PANEL_REVIEW);
	}

	// the three juries on the last clean and jerk

	@Test
	public void lastCleanJerkWithoutJury() throws Exception {
		lastLiftOnBothPlatforms(OBSMonitorStatusTest::lastCleanJerk, Stage.CLEANJERK, BreakType.GROUP_DONE,
		        Jury.NONE);
	}

	@Test
	public void lastCleanJerkWithThreeMemberJuryReview() throws Exception {
		lastLiftOnBothPlatforms(OBSMonitorStatusTest::lastCleanJerk, Stage.CLEANJERK, BreakType.GROUP_DONE,
		        Jury.THREE_MEMBER_REVIEW);
	}

	@Test
	public void lastCleanJerkWithAdjudicationPanelReview() throws Exception {
		lastLiftOnBothPlatforms(OBSMonitorStatusTest::lastCleanJerk, Stage.CLEANJERK, BreakType.GROUP_DONE,
		        Jury.PANEL_REVIEW);
	}

	/**
	 * Decisions shown as soon as they are given, before the lift is recorded. The decision on the first clean and jerk
	 * is then shown while the athlete under review is still the last snatch lifter.
	 */
	@Test
	public void liftTypeWhenDecisionsAreShownBeforeBeingRecorded() throws Exception {
		setSwitch(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY, true);
		startPlatforms("Each platform: a session with a single athlete, 2 snatches done. Decisions are shown at once,"
		        + " before the lift is recorded. Last snatch, break, then the first clean and jerk.",
		        OBSMonitorStatusTest::lastSnatch, Stage.SNATCH);
		runConcurrently(() -> lastSnatchThenFirstCleanJerk(this.a, W), () -> lastSnatchThenFirstCleanJerk(this.b, R));
	}

	private void lastSnatchThenFirstCleanJerk(Station station, boolean good) {
		station.juryCase("No jury. Last snatch: " + (good ? "good lift" : "no lift")
		        + ". The Speaker ends the break. First clean and jerk: " + (good ? "good lift." : "no lift."));
		station.fop.setJurySize(0);
		lift(station, Stage.SNATCH, good, good, good);
		station.expect("break=BREAK.FIRST_CJ;previous=DECISION_VISIBLE." + decision(good), null);
		station.post(new FOPEvent.StartLifting(new Object()));
		station.expect("state=CURRENT_ATHLETE_DISPLAYED;previous=BREAK.FIRST_CJ", Stage.CLEANJERK);
		lift(station, Stage.CLEANJERK, good, good, good);
		station.expect("state=CURRENT_ATHLETE_DISPLAYED;previous=DECISION_VISIBLE." + decision(good),
		        Stage.CLEANJERK);
	}

	@Test
	public void theMonitoredPlatformIsTheOneNamedInTheUrl() {
		FieldOfPlay fopA = MockFieldOfPlay.createWithProductionTimers("A", groupA, List.of());
		FieldOfPlay fopB = MockFieldOfPlay.createWithProductionTimers("Platform B", groupB, List.of());
		List<FieldOfPlay> both = List.of(fopA, fopB);

		assertSame(fopA, OBSMonitorStatus.fopToMonitor(Map.of("fop", List.of("A")), both));
		assertSame(fopB, OBSMonitorStatus.fopToMonitor(Map.of("fop", List.of("Platform B")), both));
		assertSame("name still URL-encoded", fopB,
		        OBSMonitorStatus.fopToMonitor(Map.of("fop", List.of("Platform%20B")), both));
		assertNull("several platforms: the URL must name one", OBSMonitorStatus.fopToMonitor(Map.of(), both));
		assertNull("unknown platform", OBSMonitorStatus.fopToMonitor(Map.of("fop", List.of("C")), both));

		List<FieldOfPlay> single = List.of(fopA);
		assertSame("a single platform needs no name", fopA, OBSMonitorStatus.fopToMonitor(Map.of(), single));
		assertSame("a single platform is used whatever the name", fopA,
		        OBSMonitorStatus.fopToMonitor(Map.of("fop", List.of("C")), single));
	}

	/**
	 * The last lift of the snatch or of the session, then the break that follows. The jury review interrupts that
	 * break, and the monitor goes back to it once the jury has decided. The snatches are not over until the last
	 * snatch is decided: the break before clean and jerk then starts again for its full duration, each time.
	 */
	private void lastLiftOnBothPlatforms(Consumer<List<Athlete>> session, Stage stage, BreakType breakAfter,
	        Jury onA) throws Exception {
		startPlatforms("Each platform: a session with a single athlete, whose next lift is the last "
		        + (stage == Stage.SNATCH ? "snatch (2 snatches done)." : "clean and jerk (5 lifts done).")
		        + " The other platform runs a different jury case at the same time.", session, stage);
		runConcurrently(
		        () -> lastLift(this.a, onA, stage, breakAfter),
		        () -> lastLift(this.b, onA.next(), stage, breakAfter));
		if (breakAfter == BreakType.FIRST_CJ) {
			assertFullCleanJerkBreaks(this.a, onA);
			assertFullCleanJerkBreaks(this.b, onA.next());
		}
	}

	/** Every start of the break before clean and jerk is for the full stated duration, also after a review. */
	private static void assertFullCleanJerkBreaks(Station station, Jury jury) {
		int stated = station.fop.getGroup().cjBreakDuration();
		assertTrue(stated > 0);
		int breaks = switch (jury) {
			case NONE -> 1;
			case PANEL_REVIEW -> 2;
			case THREE_MEMBER_REVIEW -> 3;
		};
		List<Integer> starts = List.copyOf(station.cleanJerkBreakStarts);
		assertTrue("platform " + station.fop.getName() + ": " + breaks + " clean and jerk breaks, got " + starts,
		        starts.size() >= breaks);
		for (Integer start : starts) {
			assertEquals("platform " + station.fop.getName() + ": clean and jerk break duration " + starts, stated,
			        start.intValue());
		}
	}

	/** The break runs for a moment, so that a break that would only resume is told from one that starts again. */
	private static void breakRuns() {
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void lastLift(Station station, Jury jury, Stage stage, BreakType breakAfter) {
		String inBreak = "BREAK." + breakAfter.name();
		switch (jury) {
			case NONE -> {
				station.juryCase("No jury (jury size 0). The referees give a good lift; the " + breakAfter
				        + " break follows.");
				station.fop.setJurySize(0);
				lift(station, stage, W, W, W);
				station.expect("break=" + inBreak + ";previous=DECISION_VISIBLE.GOOD_LIFT", null);
			}
			case THREE_MEMBER_REVIEW -> {
				station.juryCase("3-member jury. The referees give a no lift; during the " + breakAfter
				        + " break the Jury Chair starts a Deliberation and the jury reverses to a good lift."
				        + (breakAfter == BreakType.FIRST_CJ
				                ? " The jury then reviews the same lift again and goes back to a no lift."
				                : ""));
				station.fop.setJurySize(3);
				assertFalse(station.fop.isPanelProcedure());
				lift(station, stage, R, R, W);
				station.expect("break=" + inBreak + ";previous=DECISION_VISIBLE.BAD_LIFT", null);

				breakRuns();
				station.post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true,
				        new Object()));
				station.expect("break=BREAK.JURY;previous=" + inBreak, stage);
				juryVotes(station, W, W, W);
				station.post(new FOPEvent.JuryDecision(station.fop.getAthleteUnderReview(), new Object(), W, true,
				        null, true));
				station.expect("state=DECISION_VISIBLE.GOOD_LIFT;previous=BREAK.JURY", stage);
				station.expect("break=" + inBreak + ";previous=DECISION_VISIBLE.GOOD_LIFT", null);

				if (breakAfter == BreakType.FIRST_CJ) {
					station.note("Second review of the same lift.");
					breakRuns();
					station.post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true,
					        new Object()));
					station.expect("break=BREAK.JURY;previous=" + inBreak, stage);
					juryVotes(station, R, R, R);
					station.post(new FOPEvent.JuryDecision(station.fop.getAthleteUnderReview(), new Object(), R, true,
					        1, true));
					station.expect("state=DECISION_VISIBLE.BAD_LIFT;previous=BREAK.JURY", stage);
					station.expect("break=" + inBreak + ";previous=DECISION_VISIBLE.BAD_LIFT", null);
				}
			}
			case PANEL_REVIEW -> {
				station.juryCase("Adjudication Panel (3 referees, 4 jurors, video). The referees give a good lift; during the "
				        + breakAfter + " break the jurors vote no lift: automatic review, the panel reverses to a no lift.");
				station.fop.setJurySize(4);
				assertTrue(station.fop.isPanelProcedure());
				lift(station, stage, W, W, W);
				station.expect("break=" + inBreak + ";previous=DECISION_VISIBLE.GOOD_LIFT", null);

				breakRuns();
				juryVotes(station, R, R, R, R);
				station.expect("break=BREAK.JURY;previous=" + inBreak, stage);
				assertTrue("automatic Adjudication Panel Review", station.fop.isPanelReview());
				panelVoteReversingToNoLift(station);
				station.expect("state=DECISION_VISIBLE.BAD_LIFT;previous=BREAK.JURY", stage);
				station.expect("break=" + inBreak + ";previous=DECISION_VISIBLE.BAD_LIFT", null);
			}
		}
		assertEquals("platform " + station.fop.getName() + " back in the break", breakAfter,
		        station.fop.getBreakType());
	}

	private void run(Station station, Lift... lifts) {
		for (Lift lift : lifts) {
			switch (lift) {
				case PANEL_NO_REVIEW -> panelLiftWithoutReview(station);
				case PANEL_REVERSAL -> panelReversal(station);
				case REFEREES_NO_REVIEW -> refereesLiftWithoutReview(station);
				case JURY_REVIEW -> juryReview(station);
			}
		}
	}

	private void panelLiftWithoutReview(Station station) {
		station.juryCase("Adjudication Panel (3 referees, 4 jurors, video). Good lift; the jury agrees: no review.");
		station.fop.setJurySize(4);
		assertTrue(station.fop.isPanelProcedure());
		liftToNextAthlete(station, W, W, W);
		juryVotes(station, W, W, W, W);
		assertFalse("the panel agrees with the referees", station.fop.isPanelReview());
	}

	private void panelReversal(Station station) {
		station.juryCase("Adjudication Panel. Good lift; the jurors vote no lift: automatic review, the panel reverses "
		        + "to a no lift.");
		station.fop.setJurySize(4);
		assertTrue(station.fop.isPanelProcedure());
		liftToNextAthlete(station, W, W, W);
		juryVotes(station, R, R, R, R);
		station.expect("break=BREAK.JURY;previous=CURRENT_ATHLETE_DISPLAYED", Stage.SNATCH);
		assertTrue("automatic Adjudication Panel Review", station.fop.isPanelReview());
		panelVoteReversingToNoLift(station);
		station.expect("state=DECISION_VISIBLE.BAD_LIFT;previous=BREAK.JURY", Stage.SNATCH);
		station.expect("state=CURRENT_ATHLETE_DISPLAYED;previous=DECISION_VISIBLE.BAD_LIFT", Stage.SNATCH);
		// the next clock is refused while the attempt boards show the jury decision card
		station.fop.expireJuryDecisionCardForTests();
	}

	private void refereesLiftWithoutReview(Station station) {
		station.juryCase("3-member jury. No lift; the jury agrees: no review.");
		station.fop.setJurySize(3);
		assertFalse(station.fop.isPanelProcedure());
		liftToNextAthlete(station, R, R, W);
		juryVotes(station, R, R, R);
	}

	private void juryReview(Station station) {
		station.juryCase("3-member jury. No lift; the Jury Chair starts a Deliberation and the jury reverses to a good "
		        + "lift.");
		station.fop.setJurySize(3);
		assertFalse(station.fop.isPanelProcedure());
		liftToNextAthlete(station, R, R, W);
		station.post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, new Object()));
		station.expect("break=BREAK.JURY;previous=CURRENT_ATHLETE_DISPLAYED", Stage.SNATCH);

		juryVotes(station, W, W, W);
		station.post(new FOPEvent.JuryDecision(station.fop.getAthleteUnderReview(), new Object(), W, true, null, true));
		station.expect("state=DECISION_VISIBLE.GOOD_LIFT;previous=BREAK.JURY", Stage.SNATCH);
		station.expect("state=CURRENT_ATHLETE_DISPLAYED;previous=DECISION_VISIBLE.GOOD_LIFT", Stage.SNATCH);
	}

	/** Call for Votes, the seven votes for a no lift, and the Chair's rule. */
	private void panelVoteReversingToNoLift(Station station) {
		station.post(new FOPEvent.PanelVoteCalled(new Object()));
		for (int referee = 0; referee < 3; referee++) {
			station.post(new FOPEvent.DecisionUpdate(new Object(), referee, R));
		}
		juryVotes(station, R, R, R, R);
		station.post(new FOPEvent.JuryDecision(station.fop.getAthleteUnderReview(), new Object(), R, true, 3));
	}

	/** One attempt, then the next athlete of the snatch. */
	private void liftToNextAthlete(Station station, boolean... referees) {
		lift(station, Stage.SNATCH, referees);
		station.expect("state=CURRENT_ATHLETE_DISPLAYED;previous=DECISION_VISIBLE." + decision(referees),
		        Stage.SNATCH);
	}

	/** One attempt, from the clock start to the decision; the first two referees agree and give the down signal. */
	private void lift(Station station, Stage stage, boolean... referees) {
		station.post(new FOPEvent.TimeStarted(new Object()));
		station.expect("state=TIME_RUNNING;previous=CURRENT_ATHLETE_DISPLAYED", stage);
		station.post(new FOPEvent.TimeStopped(new Object()));
		station.expect("state=TIME_STOPPED;previous=TIME_RUNNING", stage);
		station.post(new FOPEvent.DecisionUpdate(new Object(), 0, referees[0]));
		station.post(new FOPEvent.DecisionUpdate(new Object(), 1, referees[1]));
		station.expect("state=DOWN_SIGNAL_VISIBLE;previous=TIME_STOPPED", stage);
		station.post(new FOPEvent.DecisionUpdate(new Object(), 2, referees[2]));
		station.expect("state=DECISION_VISIBLE." + decision(referees) + ";previous=DOWN_SIGNAL_VISIBLE", stage);
	}

	private static String decision(boolean... referees) {
		return referees[0] ? "GOOD_LIFT" : "BAD_LIFT";
	}

	private void juryVotes(Station station, boolean... votes) {
		for (int juror = 0; juror < votes.length; juror++) {
			station.post(new FOPEvent.JuryMemberDecisionUpdate(new Object(), juror, votes[juror]));
		}
	}

	/** Run the officials of both platforms at the same time, then check that nothing else shows up. */
	private void runConcurrently(Runnable onA, Runnable onB) throws Exception {
		ExecutorService officials = Executors.newFixedThreadPool(2);
		try {
			Future<?> a = officials.submit(onA);
			Future<?> b = officials.submit(onB);
			join(a);
			join(b);
		} finally {
			officials.shutdownNow();
		}
		Thread.sleep(DECISION_VISIBLE_MS + REVERSAL_DELAY_MS);
		assertEquals("platform A", this.a.expected, List.copyOf(this.a.titles));
		assertEquals("platform B", this.b.expected, List.copyOf(this.b.titles));
	}

	private static void join(Future<?> officials) throws Exception {
		try {
			officials.get(2, TimeUnit.MINUTES);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof AssertionError failure) {
				throw failure;
			}
			throw e;
		}
	}

	private void startPlatforms(String situation, Consumer<List<Athlete>> session, Stage stage) {
		this.situation = situation;
		List<Athlete> athletes = AthleteRepository.findAll();
		List<Athlete> rankedWithA = AthleteRepository.findAthletesForGlobalRanking(groupA, false);
		List<Athlete> rankedWithB = AthleteRepository.findAthletesForGlobalRanking(groupB, false);
		assertEquals("athletes ranked with session A", 5, rankedWithA.size());
		assertEquals("athletes ranked with session B", 5, rankedWithB.size());
		assertTrue("the platforms have no athlete in common", Collections.disjoint(rankedWithA, rankedWithB));
		long start = System.currentTimeMillis();
		this.a = new Station(startPlatform("A", groupA, athletes, session), start);
		this.b = new Station(startPlatform("Platform B", groupB, athletes, session), start);
		this.a.expect("state=CURRENT_ATHLETE_DISPLAYED;previous=INACTIVE", stage);
		this.b.expect("state=CURRENT_ATHLETE_DISPLAYED;previous=INACTIVE", stage);
	}

	/** All athletes start at the same weight, so each lift is a different athlete's first snatch. */
	private static void firstSnatch(List<Athlete> athletes) {
		for (Athlete athlete : athletes) {
			athlete.setSnatch1Declaration(Integer.toString(60));
			athlete.setCleanJerk1Declaration(Integer.toString(80));
		}
	}

	/** A single athlete, who has done two snatches. */
	private static void lastSnatch(List<Athlete> athletes) {
		onlyFirst(athletes);
		Athlete athlete = athletes.get(0);
		athlete.setSnatch1Declaration(Integer.toString(60));
		athlete.setSnatch1ActualLift("60");
		athlete.setSnatch2ActualLift("61");
		athlete.setCleanJerk1Declaration(Integer.toString(80));
	}

	/** A single athlete, who has done three snatches and two clean and jerks. */
	private static void lastCleanJerk(List<Athlete> athletes) {
		onlyFirst(athletes);
		Athlete athlete = athletes.get(0);
		athlete.setSnatch1Declaration(Integer.toString(60));
		athlete.setSnatch1ActualLift("60");
		athlete.setSnatch2ActualLift("61");
		athlete.setSnatch3ActualLift("62");
		athlete.setCleanJerk1Declaration(Integer.toString(80));
		athlete.setCleanJerk1ActualLift("80");
		athlete.setCleanJerk2ActualLift("81");
	}

	private static void onlyFirst(List<Athlete> athletes) {
		for (int i = 1; i < athletes.size(); i++) {
			athletes.get(i).setGroup(null);
		}
	}

	/** A platform on its own session, with the athletes prepared by {@code session}, lifting. */
	private FieldOfPlay startPlatform(String name, Group group, List<Athlete> allAthletes,
	        Consumer<List<Athlete>> session) {
		FieldOfPlay fop = MockFieldOfPlay.createWithProductionTimers(name, group, allAthletes);
		fop.setDecisionTimingForTests(DOWN_SIGNAL_VISIBLE_MS, REVERSAL_DELAY_MS, DECISION_VISIBLE_MS, 0, 0);
		// jury size is stored on the platform; this one is never persisted
		fop.setPlatform(new Platform());
		fop.loadGroup(group, this, true);
		List<Athlete> athletes = fop.getDisplayOrder();
		assertEquals("athletes in session " + group.getName(), 5, athletes.size());
		JPAService.runInTransaction(em -> {
			AthleteSorter.testAssignStartNumbers(athletes);
			session.accept(athletes);
			for (Athlete athlete : athletes) {
				em.merge(athlete);
			}
			em.flush();
			return null;
		});
		fop.loadGroup(group, this, true);
		fop.fopEventPost(new FOPEvent.SwitchGroup(group, this));
		fop.fopEventPost(new FOPEvent.StartLifting(this));
		assertEquals(FOPState.CURRENT_ATHLETE_DISPLAYED, fop.getState());
		// real timers from now on: the states last long enough to be seen, as in a competition
		fop.setTestingMode(false);
		return fop;
	}

	private static void setSwitch(FeatureSwitch featureSwitch, boolean enabled) {
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(featureSwitch, enabled);
		Config.setCurrentTesting(config);
	}

	/** Add this test's section to the report and rewrite the file, so that it is complete even if a test hangs. */
	private void writeReport(Description description, Throwable failure) {
		if (this.a == null) {
			return;
		}
		StringBuilder html = new StringBuilder();
		html.append("<section><h2 class='").append(failure == null ? "pass" : "fail").append("'>")
		        .append(failure == null ? "PASS " : "FAIL ").append(escape(description.getMethodName()))
		        .append("</h2>\n<p>").append(escape(this.situation)).append("</p>\n");
		if (failure != null) {
			html.append("<pre class='failure'>").append(escape(String.valueOf(failure.getMessage()))).append("</pre>\n");
		}
		html.append("<div class='platforms'>\n");
		for (Station station : new Station[] { this.a, this.b }) {
			appendStation(html, station);
		}
		html.append("</div></section>\n");
		synchronized (reportSections) {
			reportSections.add(html.toString());
			try {
				Files.createDirectories(REPORT.getParent());
				Files.writeString(REPORT, """
				        <!DOCTYPE html><html><head><meta charset='utf-8'><title>OBS monitor, two platforms</title>
				        <style>
				        body { font-family: sans-serif; font-size: 13px; margin: 1em; }
				        h2.pass { color: #1a7f37; } h2.fail { color: #c62828; }
				        .platforms { display: flex; gap: 1.5em; align-items: flex-start; }
				        .platform { flex: 1 1 0; min-width: 0; }
				        table { border-collapse: collapse; width: 100%; }
				        th, td { border: 1px solid #ccc; padding: 2px 6px; text-align: left; vertical-align: top; }
				        td.t { text-align: right; color: #666; white-space: nowrap; }
				        tr.note td { background: #eef3fb; font-style: italic; }
				        div.cases { background: #eef3fb; border: 1px solid #c5d3ea; padding: 4px 8px; margin-bottom: 8px; }
				        div.cases ol { margin: 4px 0 0 0; padding-left: 1.5em; }
				        tr.ok td.title { background: #e8f5e9; }
				        tr.bad td.title { background: #ffebee; }
				        .expected { color: #c62828; }
				        pre.failure { background: #ffebee; white-space: pre-wrap; }
				        code { font-size: 12px; overflow-wrap: anywhere; }
				        </style></head><body>
				        <h1>OBS monitor: two platforms lifting at the same time</h1>
				        <p>Generated by OBSMonitorStatusTest. For each platform: the actions of the officials, the
				        platform state right after each action, and each title the monitor published (green: as
				        expected, red: different from the expected title shown under it). Times are in ms from the
				        start of the test, common to both platforms.</p>
				        """ + String.join("", reportSections) + "</body></html>\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void appendStation(StringBuilder html, Station station) {
		// headed as in the monitor titles
		html.append("<div class='platform'><h3>fop=").append(escape(station.fop.getName())).append("</h3>\n");
		html.append("<div class='cases'><b>Jury cases, in order</b><ol>");
		for (String juryCase : station.juryCases) {
			html.append("<li>").append(escape(juryCase)).append("</li>");
		}
		html.append("</ol></div>\n")
		        .append("<table><tr><th>ms</th><th>Action</th><th>Platform state after</th>"
		                + "<th>Monitor title</th></tr>\n");
		List<Station.Entry> entries = new ArrayList<>(station.timeline);
		entries.sort((x, y) -> Long.compare(x.atMs(), y.atMs()));
		int titleIndex = 0;
		for (Station.Entry entry : entries) {
			switch (entry.kind()) {
				case NOTE -> html.append("<tr class='note'><td class='t'>").append(entry.atMs())
				        .append("</td><td colspan='3'>").append(escape(entry.text())).append("</td></tr>\n");
				case ACTION -> html.append("<tr><td class='t'>").append(entry.atMs()).append("</td><td>")
				        .append(escape(entry.text())).append("</td><td>").append(escape(entry.fopState()))
				        .append("</td><td></td></tr>\n");
				case TITLE -> {
					String expected = titleIndex < station.expected.size() ? station.expected.get(titleIndex) : null;
					boolean ok = entry.text().equals(expected);
					html.append("<tr class='").append(ok ? "ok" : "bad").append("'><td class='t'>")
					        .append(entry.atMs()).append("</td><td></td><td></td><td class='title'><code>")
					        .append(escape(entry.text())).append("</code>");
					if (!ok) {
						html.append("<br><span class='expected'>expected: <code>")
						        .append(expected != null ? escape(expected) : "nothing more").append("</code></span>");
					}
					html.append("</td></tr>\n");
					titleIndex++;
				}
			}
		}
		for (int i = titleIndex; i < station.expected.size(); i++) {
			html.append("<tr class='bad'><td></td><td></td><td></td><td class='title'><span class='expected'>"
			        + "never published: <code>").append(escape(station.expected.get(i))).append("</code></span></td></tr>\n");
		}
		html.append("</table></div>\n");
	}

	private static String escape(String text) {
		return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * One platform and its monitor. The monitor is fed from the platform's UI event bus, as OBSMonitor is, and the
	 * titles it would publish are kept in order.
	 */
	public static final class Station {
		enum Kind {
			NOTE, ACTION, TITLE
		}

		/** One line of the report: an explanation, an action and the platform state after it, or a title. */
		record Entry(long atMs, Kind kind, String text, String fopState) {
		}

		final FieldOfPlay fop;
		final OBSMonitorStatus status;
		final List<String> titles = new CopyOnWriteArrayList<>();
		final List<String> expected = new CopyOnWriteArrayList<>();
		final List<Entry> timeline = new CopyOnWriteArrayList<>();
		/** the jury cases run on this platform, in order */
		final List<String> juryCases = new CopyOnWriteArrayList<>();
		/** milliseconds on the clock at each start of the break before clean and jerk */
		final List<Integer> cleanJerkBreakStarts = new CopyOnWriteArrayList<>();
		private final long start;

		Station(FieldOfPlay fop, long start) {
			this.fop = fop;
			this.start = start;
			this.status = new OBSMonitorStatus(fop);
			this.timeline.add(new Entry(elapsed(), Kind.ACTION, "Session started, monitor opened", fopState()));
			publish(this.status.attach());
			fop.getUiEventBus().register(this);
		}

		void note(String text) {
			this.timeline.add(new Entry(elapsed(), Kind.NOTE, text, null));
		}

		/** The start of a jury case: listed at the top of the platform's column, and marked in its timeline. */
		void juryCase(String text) {
			this.juryCases.add(text);
			note(text);
		}

		private long elapsed() {
			return System.currentTimeMillis() - this.start;
		}

		private String fopState() {
			FOPState state = this.fop.getState();
			return state == FOPState.BREAK ? "BREAK." + this.fop.getBreakType() : String.valueOf(state);
		}

		@Subscribe
		public synchronized void slaveUIEvent(UIEvent e) {
			if (e instanceof UIEvent.BreakStarted b && b.getBreakType() == BreakType.FIRST_CJ
			        && b.getTimeRemaining() != null) {
				this.cleanJerkBreakStarts.add(b.getTimeRemaining());
				this.timeline.add(new Entry(elapsed(), Kind.ACTION, "",
				        "clean and jerk break clock set to " + b.getTimeRemaining() / 1000 + " s"));
			}
			publish(this.status.onEvent(e));
		}

		private synchronized void publish(String title) {
			if (title != null) {
				this.titles.add(title);
				this.timeline.add(new Entry(elapsed(), Kind.TITLE, title, null));
			}
		}

		void post(FOPEvent e) {
			long at = elapsed();
			this.fop.fopEventPost(e);
			this.timeline.add(new Entry(at, Kind.ACTION, describe(e), fopState()));
		}

		private static String describe(FOPEvent e) {
			if (e instanceof FOPEvent.TimeStarted) {
				return "Clock started";
			} else if (e instanceof FOPEvent.TimeStopped) {
				return "Clock stopped";
			} else if (e instanceof FOPEvent.DecisionUpdate d) {
				return "Referee " + (d.getRefIndex() + 1) + ": " + lift(d.isDecision());
			} else if (e instanceof FOPEvent.JuryMemberDecisionUpdate j) {
				return "Juror " + (j.refIndex + 1) + ": " + lift(j.decision);
			} else if (e instanceof FOPEvent.BreakStarted b) {
				return b.getBreakType() == BreakType.JURY ? "Jury Chair: Deliberation" : "Break " + b.getBreakType();
			} else if (e instanceof FOPEvent.PanelVoteCalled) {
				return "Jury Chair: Call for Votes";
			} else if (e instanceof FOPEvent.StartLifting) {
				return "Speaker: end of break";
			} else if (e instanceof FOPEvent.JuryDecision j) {
				return "Jury decision: " + lift(Boolean.TRUE.equals(j.success))
				        + (j.getReasonCode() != null ? " (rule " + j.getReasonCode() + ")" : "");
			}
			return e.getClass().getSimpleName();
		}

		private static String lift(boolean good) {
			return good ? "good lift" : "no lift";
		}

		/**
		 * Wait for the monitor to publish its next title, which must be the given state on this platform.
		 *
		 * @param stage the lift type in the title, null when the title has none
		 */
		void expect(String stateAndPrevious, Stage stage) {
			this.expected.add(stateAndPrevious + (stage != null ? ";liftType=" + stage : "") + ";fop="
			        + this.fop.getName());
			long deadline = System.currentTimeMillis() + TITLE_WAIT_MS;
			while (this.titles.size() < this.expected.size() && System.currentTimeMillis() < deadline) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			List<String> published = List.copyOf(this.titles);
			if (published.size() < this.expected.size()) {
				fail("platform " + this.fop.getName() + ": no title for " + stateAndPrevious + " after " + published);
			}
			assertEquals("platform " + this.fop.getName(), this.expected,
			        published.subList(0, this.expected.size()));
		}
	}
}
