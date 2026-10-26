/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.simulation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.BreakController;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import ch.qos.logback.classic.Logger;

/**
 * A short competition that exercises the messages sent to the tracker (compliance/150-TRACKER_JURY_VOTES.md):
 * countdown to the introduction, introduction of athletes (no clock), countdown to the snatch with the introduction of
 * officials while it runs, a normal snatch (the jury agrees with the referees), a snatch reversed by the seven-person
 * Adjudication Panel, a good and a bad snatch for each
 * athlete with a weight change between two lifts, the second athlete withdrawing instead of taking a last snatch, the clean &amp; jerk break, a clean &amp;
 * jerk reversed after a challenge, and the end of the session.
 *
 * <p>
 * Runs on the {@code SINGLE_ATHLETE_GROUPS} demo data with the M2 athlete moved to M1 (two athletes), and the
 * competition set for the seven-person Adjudication Panel (four-member jury, Video Playback Technology), where an OVR
 * is used. The FOP must be a production one (server timers, real display delays): the countdowns are ended by its
 * break timer, and the scenario waits for its decision displays as the officials do. Every step posts the FOP
 * events that the screens and the referee and jury devices
 * would send; the FOP turns them into the same UI events, hence the same forwarded messages. Breaks are started as the
 * announcer's break management does ({@link BreakController}).
 * </p>
 */
public class ProtocolScenario implements BreakController {

	/** Waits between steps: real waits for a live run, short ones for a test. */
	public interface Pacer {
		void pause(long millis) throws InterruptedException;
	}

	/**
	 * A referee or jury device. Each official needs its own origin: the FOP drops an event equal to the previous one
	 * with the same timestamp, and event equality only compares the athlete and the origin.
	 */
	private record Device(String name) {
	}

	public static final String SESSION = "M1";
	public static final int BEFORE_INTRODUCTION_MILLIS = 10_000;
	public static final int BEFORE_SNATCH_MILLIS = 20_000;
	public static final int INTRODUCTION_STAGE_MILLIS = 5000;
	public static final int OVERTURN_RULE = 5;
	/** increase asked for by the coach in the weight change between two snatches */
	private static final int WEIGHT_CHANGE_KG = 2;
	/**
	 * time the clock runs before the referees' decisions give the down signal: two seconds go by on the clock (1:00,
	 * 0:59, 0:58), with a margin because the board shows whole seconds rounded up
	 */
	private static final long CLOCK_RUNNING_MILLIS = 2500;
	/** time between the referees' decision and the jury votes */
	private static final long VOTE_PAUSE_MILLIS = 1000;
	/** longest wait for the FOP's decision display, review break or jury card */
	private static final long DISPLAY_TIMEOUT_MILLIS = 60_000;
	/** time allowed beyond a countdown for the break timer to end it */
	private static final long TIMER_MARGIN_MILLIS = 5000;

	private static final boolean GOOD = true;
	private static final boolean BAD = false;
	private static final int JURY_SIZE = 4;

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ProtocolScenario.class);
	/** a clean &amp; jerk break started automatically by the FOP lasts 10 s during a simulation */
	private static final int AUTOMATIC_CJ_BREAK_MILLIS = 10_000;
	private static volatile boolean running;

	private final FieldOfPlay fop;
	private final Pacer pacer;
	private final long settleMillis;
	private final boolean includeIntroduction;
	private final Device[] referees = { new Device("referee 1"), new Device("referee 2"), new Device("referee 3") };
	private final Device[] jurors = { new Device("juror 1"), new Device("juror 2"), new Device("juror 3"),
	        new Device("juror 4") };

	/**
	 * @param fop          the field of play to drive, on the session returned by {@link #prepareSession()}
	 * @param pacer        waits between steps
	 * @param settleMillis pause after each step so the UI events are delivered before the next one
	 */
	public ProtocolScenario(FieldOfPlay fop, Pacer pacer, long settleMillis) {
		this(fop, pacer, settleMillis, true);
	}

	public ProtocolScenario(FieldOfPlay fop, Pacer pacer, long settleMillis, boolean includeIntroduction) {
		this.fop = fop;
		this.pacer = pacer;
		this.settleMillis = settleMillis;
		this.includeIntroduction = includeIntroduction;
	}

	/**
	 * Move the M2 athletes to M1 and give every M1 athlete the same declarations, so the session has two athletes
	 * lifting in a known order. Set the competition for the seven-person Adjudication Panel: four-member jury, Video
	 * Playback Technology (saved in the database, so the exported database shows it).
	 *
	 * @return the session
	 */
	public static Group prepareSession() {
		Group session = JPAService.runInTransaction(em -> {
			Group m1 = GroupRepository.doFindByName(SESSION, em);
			Group m2 = GroupRepository.doFindByName("M2", em);
			List<Athlete> athletes = AthleteRepository.doFindAll(em);
			int weight = 60;
			for (Athlete a : athletes) {
				Group g = a.getGroup();
				if (g == null || !(g.getName().equals(SESSION) || (m2 != null && g.getName().equals(m2.getName())))) {
					continue;
				}
				a.setGroup(m1);
				a.setSnatch1Declaration(Integer.toString(weight));
				a.setCleanJerk1Declaration(Integer.toString(weight + 20));
				weight++;
				em.merge(a);
			}
			em.flush();
			return m1;
		});
		// the situation where an OVR is used: seven-person Adjudication Panel (four-member jury with video playback)
		Competition competition = Competition.getCurrent();
		competition.setJurySize(JURY_SIZE);
		competition.setVideoPlaybackTechnology(true);
		CompetitionRepository.save(competition);
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD, true);
		Config.setCurrent(config);
		return session;
	}

	/** @return true while a scenario runs: the FOP shortens the automatic clean &amp; jerk break to 10 s */
	public static boolean isRunning() {
		return running;
	}

	public void run(Group session) throws InterruptedException {
		running = true;
		try {
			runSteps(session);
		} finally {
			running = false;
		}
	}

	private void runSteps(Group session) throws InterruptedException {
		step("switch to session " + session.getName());
		post(new FOPEvent.SwitchGroup(session, this));

		if (this.includeIntroduction) {
			step("countdown to the introduction");
			startCountdown(BreakType.BEFORE_INTRODUCTION, BEFORE_INTRODUCTION_MILLIS);
			// at the end of the countdown the FOP starts the introduction of athletes (before snatch break, no clock)
			awaitCeremony(CeremonyType.INTRODUCTION, BEFORE_INTRODUCTION_MILLIS + TIMER_MARGIN_MILLIS);

			logStep("introduction of athletes");
			this.pacer.pause(INTRODUCTION_STAGE_MILLIS);
			post(new FOPEvent.CeremonyDone(CeremonyType.INTRODUCTION, this));

			logStep("countdown to the snatch");
			startCountdown(BreakType.FIRST_SNATCH, BEFORE_SNATCH_MILLIS);
			this.pacer.pause(INTRODUCTION_STAGE_MILLIS);

			logStep("introduction of officials, countdown running");
			post(new FOPEvent.CeremonyStarted(CeremonyType.OFFICIALS_INTRODUCTION, this.fop.getGroup(), null, this));
			this.pacer.pause(INTRODUCTION_STAGE_MILLIS);
			post(new FOPEvent.CeremonyDone(CeremonyType.OFFICIALS_INTRODUCTION, this));

			logStep("end of the countdown to the snatch");
			// the end of the countdown starts lifting
			awaitLifting(BEFORE_SNATCH_MILLIS + TIMER_MARGIN_MILLIS);
		} else {
			step("start snatch without introductions");
			post(new FOPEvent.StartLifting(this));
			awaitLifting(TIMER_MARGIN_MILLIS);
		}

		step("a normal snatch: the jury agrees with the referees, no adjudication");
		Athlete normal = up();
		plainLift(GOOD);

		step("snatch reversed by the Adjudication Panel");
		Athlete reversed = up();
		lift(GOOD, GOOD, GOOD);
		juryVotes(BAD, BAD, BAD, BAD);
		// the jury against the referees: the FOP starts the Adjudication Panel Review once the decision display ends
		awaitBreak(BreakType.JURY);
		post(new FOPEvent.PanelVoteCalled(this));
		refereeReviewVotes(BAD, BAD, BAD);
		juryVotes(BAD, BAD, BAD, BAD);
		// No Lift result: the Jury Chair gives the rule; the FOP resumes lifting after the jury decision display
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), this, BAD, true, OVERTURN_RULE));

		step("a good and a bad snatch for each athlete");
		// the first lifter has a Good Lift and gets a No Lift; the reversed athlete gets a Good Lift; anyone else gets
		// both. Then the athlete who did not lift first withdraws.
		Map<Long, Deque<Boolean>> planned = new HashMap<>();
		Athlete second = null;
		for (Athlete a : this.fop.getDisplayOrder()) {
			if (normal != null && a.getId().equals(normal.getId())) {
				planned.put(a.getId(), new ArrayDeque<>(List.of(BAD)));
			} else {
				boolean wasReversed = reversed != null && a.getId().equals(reversed.getId());
				planned.put(a.getId(), new ArrayDeque<>(wasReversed ? List.of(GOOD) : List.of(GOOD, BAD)));
				second = second == null ? a : second;
			}
		}
		boolean weightChanged = false;
		for (Athlete a = up(); a != null && a.getAttemptsDone() < 3
		        && planned.values().stream().anyMatch(q -> !q.isEmpty()); a = up()) {
			Deque<Boolean> outcomes = planned.get(a.getId());
			plainLift(outcomes == null || outcomes.isEmpty() ? GOOD : outcomes.poll());
			Athlete next = up();
			if (!weightChanged && next != null && next.getAttemptsDone() < 3) {
				// between two lifts, the coach of the athlete up asks for more weight: the lifting order changes
				int newWeight = next.getNextAttemptRequestedWeight() + WEIGHT_CHANGE_KG;
				step("weight change: " + next.getShortName() + " asks for " + newWeight + " kg");
				changeWeight(next, newWeight);
				weightChanged = true;
			}
		}

		step("withdrawal instead of the last snatch");
		up();
		withdraw(second);

		step("rest of the snatch");
		for (Athlete a = up(); a != null && a.getAttemptsDone() < 3; a = up()) {
			plainLift(GOOD);
		}

		step("clean & jerk break");
		// started by the FOP after the last snatch when the competition has the automatic clean & jerk break
		if (this.fop.getState() != FOPState.BREAK || this.fop.getBreakType() != BreakType.FIRST_CJ) {
			startCountdown(BreakType.FIRST_CJ, AUTOMATIC_CJ_BREAK_MILLIS);
		}
		awaitLifting(AUTOMATIC_CJ_BREAK_MILLIS + TIMER_MARGIN_MILLIS);

		step("clean & jerk reversed after a challenge");
		lift(BAD, BAD, GOOD);
		juryVotes(BAD, BAD, BAD, BAD);
		// the coach challenges: the FOP starts the challenge break once the decision display ends
		post(new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, this));
		awaitBreak(BreakType.CHALLENGE);
		post(new FOPEvent.PanelVoteCalled(this));
		refereeReviewVotes(GOOD, GOOD, GOOD);
		juryVotes(GOOD, GOOD, GOOD, GOOD);
		// Good Lift result: the Jury Chair confirms it
		post(new FOPEvent.JuryDecision(this.fop.getAthleteUnderReview(), this, GOOD, true, null));

		step("rest of the clean & jerk");
		for (Athlete a = up(); a != null && a.getAttemptsDone() < 6; a = up()) {
			plainLift(GOOD);
		}
		up();
		step("session done");
	}

	/**
	 * Start a countdown with the simulation duration: the dialog sets the break timer
	 * and the countdown type before posting the event, because the FOP does not take them from the event when starting
	 * a break from no break, or when switching the before snatch break from no clock to a countdown.
	 */
	private void startCountdown(BreakType breakType, int durationMillis) {
		IBreakTimer breakTimer = this.fop.getBreakTimer();
		breakTimer.setTimeRemaining(durationMillis, false);
		breakTimer.setBreakDuration(durationMillis);
		breakTimer.setEnd(null);
		this.fop.setCountdownType(CountdownType.DURATION);
		post(new FOPEvent.BreakStarted(breakType, CountdownType.DURATION, durationMillis, null, false, this));
	}

	private void awaitCeremony(CeremonyType ceremony, long timeoutMillis) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (this.fop.getCeremonyType() != ceremony && System.currentTimeMillis() < deadline) {
			this.pacer.pause(100);
		}
		if (this.fop.getCeremonyType() != ceremony) {
			throw new IllegalStateException("ceremony " + ceremony + " did not start");
		}
	}

	private void awaitLifting(long timeoutMillis) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (this.fop.getState() == FOPState.BREAK && System.currentTimeMillis() < deadline) {
			this.pacer.pause(100);
		}
		if (this.fop.getState() == FOPState.BREAK) {
			throw new IllegalStateException("break did not end");
		}
	}

	private void step(String name) throws InterruptedException {
		this.pacer.pause(this.settleMillis);
		logStep(name);
	}

	private void logStep(String name) {
		logger.info("{}protocol scenario: {}", FieldOfPlay.getLoggingName(this.fop), name);
	}

	private void plainLift(boolean good) throws InterruptedException {
		lift(good, good, good);
		juryVotes(good, good, good, good);
	}

	private void lift(boolean... refereeVotes) throws InterruptedException {
		// the timekeeper starts the clock once the decision display and any jury decision card have ended
		awaitDisplayEnded();
		post(new FOPEvent.TimeStarted(this));
		// the athlete lifts while the clock runs down; the referees' decisions give the down signal, which stops it
		this.pacer.pause(CLOCK_RUNNING_MILLIS);
		for (int i = 0; i < refereeVotes.length; i++) {
			post(new FOPEvent.DecisionUpdate(this.referees[i], i, refereeVotes[i]));
		}
		// the jury votes shortly after the referees' decision is shown (the FOP reveals it after the down signal)
		awaitDecisionShown();
		this.pacer.pause(VOTE_PAUSE_MILLIS);
	}

	private void awaitDecisionShown() throws InterruptedException {
		long deadline = System.currentTimeMillis() + DISPLAY_TIMEOUT_MILLIS;
		while (this.fop.getState() != FOPState.DECISION_VISIBLE) {
			if (System.currentTimeMillis() > deadline) {
				throw new IllegalStateException("referee decision not shown, state " + this.fop.getState());
			}
			this.pacer.pause(50);
		}
	}

	private void juryVotes(boolean... votes) {
		for (int i = 0; i < votes.length; i++) {
			post(new FOPEvent.JuryMemberDecisionUpdate(this.jurors[i], i, votes[i]));
		}
	}

	private void refereeReviewVotes(boolean... votes) throws InterruptedException {
		for (int i = 0; i < votes.length; i++) {
			post(new FOPEvent.DecisionUpdate(this.referees[i], i, votes[i]));
		}
		this.pacer.pause(VOTE_PAUSE_MILLIS);
	}

	/**
	 * Wait as the officials do: until the decision display, any review break and any jury decision card have ended
	 * (the FOP times them).
	 */
	private void awaitDisplayEnded() throws InterruptedException {
		long deadline = System.currentTimeMillis() + DISPLAY_TIMEOUT_MILLIS;
		while (System.currentTimeMillis() < deadline) {
			FOPState state = this.fop.getState();
			BreakType breakType = this.fop.getBreakType();
			boolean review = state == FOPState.BREAK
			        && (breakType == BreakType.JURY || breakType == BreakType.CHALLENGE);
			boolean settled = state == FOPState.CURRENT_ATHLETE_DISPLAYED || (state == FOPState.BREAK && !review);
			if (settled && !this.fop.isJuryDecisionCardShown()) {
				return;
			}
			this.pacer.pause(100);
		}
		throw new IllegalStateException("decision display did not end, state " + this.fop.getState());
	}

	/** @return the athlete up once the display has ended, null in a break */
	private Athlete up() throws InterruptedException {
		awaitDisplayEnded();
		return this.fop.getState() == FOPState.CURRENT_ATHLETE_DISPLAYED ? current() : null;
	}

	private void awaitBreak(BreakType breakType) throws InterruptedException {
		long deadline = System.currentTimeMillis() + DISPLAY_TIMEOUT_MILLIS;
		while (!(this.fop.getState() == FOPState.BREAK && this.fop.getBreakType() == breakType)) {
			if (System.currentTimeMillis() > deadline) {
				throw new IllegalStateException(breakType + " break did not start, state " + this.fop.getState());
			}
			this.pacer.pause(100);
		}
	}

	/** A weight change entered on the athlete card (first change of the next attempt), as the marshal does. */
	private void changeWeight(Athlete athlete, int newWeight) throws InterruptedException {
		Athlete a = this.fop.getDisplayOrder().stream()
		        .filter(x -> x.getId().equals(athlete.getId()))
		        .findFirst().orElse(athlete);
		String weight = Integer.toString(newWeight);
		switch (a.getAttemptsDone() + 1) {
			case 1 -> a.setSnatch1Change1(weight);
			case 2 -> a.setSnatch2Change1(weight);
			case 3 -> a.setSnatch3Change1(weight);
			case 4 -> a.setCleanJerk1Change1(weight);
			case 5 -> a.setCleanJerk2Change1(weight);
			case 6 -> a.setCleanJerk3Change1(weight);
			default -> {
				return;
			}
		}
		AthleteRepository.save(a);
		post(new FOPEvent.WeightChange(this, a, false));
		this.pacer.pause(this.settleMillis);
	}

	/** The athlete withdraws from the competition, as with the athlete card's Withdraw button. */
	private void withdraw(Athlete athlete) throws InterruptedException {
		if (athlete == null) {
			return;
		}
		Athlete other = this.fop.getDisplayOrder().stream()
		        .filter(a -> a.getId().equals(athlete.getId()))
		        .findFirst().orElse(athlete);
		// Athlete.withdraw() only sets empty results; the athlete card's fields give "" where the database has null
		for (int liftNo = other.getAttemptsDone() + 1; liftNo <= 6; liftNo++) {
			if (other.getActualLiftOrNull(liftNo) == null) {
				other.setActualLift(liftNo, "");
			}
		}
		other.withdraw();
		AthleteRepository.save(other);
		post(new FOPEvent.WeightChange(this, other, true));
		this.pacer.pause(this.settleMillis);
	}

	private Athlete current() {
		return this.fop.getCurAthlete();
	}

	private void post(FOPEvent e) {
		this.fop.fopEventPost(e);
	}
}
