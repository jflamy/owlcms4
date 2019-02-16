package org.ledocte.owlcms.state;

import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.AthleteRepository;
import org.ledocte.owlcms.data.athleteSort.AthleteSorter;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.platform.Platform;
import org.ledocte.owlcms.utils.LoggerUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ch.qos.logback.classic.Logger;

/**
 * This class describes one field of play at runtime It encapsulates the
 * in-memory data structures used to compute the state of the competition and
 * links them to the database descriptions of the group and platform.
 * 
 * @author owlcms
 */
public class FieldOfPlayState {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(FieldOfPlayState.class);

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	public enum State {
		ANNOUNCER_WAITING_FOR_TIMEKEEPER,
		CURRENT_ATHLETE_DISPLAYED,
		DECISION_VISIBLE,
		DOWN_SIGNAL_VISIBLE,
		INTERMISSION,
		TIME_RUNNING,
		TIME_STOPPED,
		TIMEKEEPER_WAITING_FOR_ANNOUNCER,
	}

	private Athlete clockOwner;
	private Athlete curAthlete;
	private Integer curWeight;
	private EventBus eventBus = null;
	private Group group = null;
	private List<Athlete> liftingOrder;
	private String name;
	private Platform platform = null;
	private Athlete previousAthlete;
	private boolean startTimeAutomatically;
	private State state;
	private ICountdownTimer timer;

	public FieldOfPlayState(Group group, Platform platform, ICountdownTimer timer) {
		this.group = group;
		this.platform = platform;
		this.name = platform.getName();
		this.setTimer(timer);
		init(AthleteRepository.findAllByGroupAndWeighIn(group, true));
	}

	public FieldOfPlayState(List<Athlete> athletes, ICountdownTimer timer) {
		this.setTimer(timer);
		init(athletes);
	}

	public void setStartTimeAutomatically(boolean startTime) {
		this.startTimeAutomatically = startTime;
	}

	public Athlete getCurAthlete() {
		return curAthlete;
	}

	/**
	 * @return the eventBus
	 */
	public EventBus getEventBus() {
		if (eventBus == null) {
			eventBus = new EventBus();
		}
		return eventBus;
	}

	/**
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}

	public List<Athlete> getLifters() {
		return liftingOrder;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the platform
	 */
	public Platform getPlatform() {
		return platform;
	}

	public Athlete getPreviousAthlete() {
		return previousAthlete;
	}

	public ICountdownTimer getTimer() {
		return this.timer;
	}


	@Subscribe
	public void handleFOPEvent(FOPEvent e) {
		logger.debug("event received {}", e);
		// in all cases we can interrupt competition (real intermission, technical
		// incident, etc.)
		if (e instanceof FOPEvent.IntermissionStarted) {
			transitionToIntermission();
		}

		switch (this.getState()) {

		case INTERMISSION:
			if (e instanceof FOPEvent.IntermissionDone) {
				recomputeLiftingOrder();
				displayCurrentAthlete();
				setState(State.CURRENT_ATHLETE_DISPLAYED);
			} else if (e instanceof FOPEvent.AthleteAnnounced) {
				announce();
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				// display new current weight, stay in current state
				recomputeLiftingOrder();
				displayCurrentWeight();
				setState(State.INTERMISSION);
			} else {
				unexpectedEventInState(e, State.INTERMISSION);
			}
			break;

		case CURRENT_ATHLETE_DISPLAYED:
			if (e instanceof FOPEvent.AthleteAnnounced) {
				announce();
			} else if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// time was started prematurely before announcer hit "announce" button
				warnTimekeeperPrematureStart();
				remindAnnouncerToAnnounce();
				setState(State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChange(curAthlete, State.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, State.CURRENT_ATHLETE_DISPLAYED);
			}
			break;

		case ANNOUNCER_WAITING_FOR_TIMEKEEPER:
			if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// time was started manually
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChange(curAthlete, State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
			} else {
				unexpectedEventInState(e, State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
			}
			break;

		case TIMEKEEPER_WAITING_FOR_ANNOUNCER:
			if (e instanceof FOPEvent.AthleteAnnounced) {
				// remove the warnings on announcer and timekeeper
				clearTimekeeperWarnings();
				clearAnnouncerWarnings();
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// we are already in this state, do nothing (we could escalate)
				setState(State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChange(curAthlete, State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else {
				unexpectedEventInState(e, State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			}
			break;

		case TIME_RUNNING:
			if (e instanceof FOPEvent.DownSignal) {
				// 2 referees have given same decision
				getTimer().stop();
				showDownSignalOnSlaveDisplays();
				stopTimeOnSlaveDisplays();
				setState(State.DOWN_SIGNAL_VISIBLE);
			} else if (e instanceof FOPEvent.TimeStoppedByTimeKeeper) {
				// athlete lifted the bar
				getTimer().stop();
				stopTimeOnSlaveDisplays();
				setState(State.TIME_STOPPED);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				// coach is requesting change
				weightChange(curAthlete, State.TIME_RUNNING);
			} else {
				unexpectedEventInState(e, State.TIME_RUNNING);
			}
			break;

		case TIME_STOPPED:
			if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// timekeeper mistake
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChangeLiftInProgress(curAthlete, State.TIME_STOPPED);
			} else {
				unexpectedEventInState(e, State.TIME_STOPPED);
			}
			break;

		case DOWN_SIGNAL_VISIBLE:
			this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
			this.setClockOwner(null);
			if (e instanceof FOPEvent.RefereeDecision) {
				getTimer().stop();
				decision(e);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChangeLiftInProgress(curAthlete, State.DOWN_SIGNAL_VISIBLE);
			} else {
				unexpectedEventInState(e, State.DOWN_SIGNAL_VISIBLE);
			}
			break;

		case DECISION_VISIBLE:
			if (e instanceof FOPEvent.RefereeDecision) {
				// decision reversal
				decision(e);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChangeLiftInProgress(curAthlete, State.DECISION_VISIBLE);
			} else if (e instanceof FOPEvent.DecisionReset) {
				recomputeLiftingOrder();
				displayCurrentAthlete();
				setState(State.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, State.DECISION_VISIBLE);
			}
			break;
		}
	}

	private void decision(FOPEvent e) {
		FOPEvent.RefereeDecision decision = (FOPEvent.RefereeDecision) e;
		if (decision.success)
			curAthlete.successfulLift();
		else
			curAthlete.failedLift();
		showRefereeDecisionOnSlaveDisplays(decision);
		setState(State.DECISION_VISIBLE);
	}

	private void setPreviousAthlete(Athlete athlete) {
		logger.debug("setting previousAthlete to {}", curAthlete);
		this.previousAthlete = athlete;
	}

	public void pause() {
		this.eventBus.post(new FOPEvent.TimeStoppedByTimeKeeper());
	}

	/**
	 * @param eventBus the eventBus to set
	 */
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	/**
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param platform the platform to set
	 */
	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public int timeAllowed() {
		Athlete a = getCurAthlete();
		int timeAllowed;
		if (getClockOwner() == a) {
			// the clock was started for us. we own the clock, clock keeps running
			timeAllowed = getTimer().getTimeRemaining();
			logger.debug("timeAllowed = timeRemaining = {}, clock owner = {}", timeAllowed, a);
		} else if (previousAthlete == a) {
			if (getClockOwner() != null) {
				// clock has started for someone else
				timeAllowed = 60000;
			} else {
				timeAllowed = 120000;
			}
		} else {
			timeAllowed = 60000;
		}
		return timeAllowed;
	}

	private void announce() {
		if (this.startTimeAutomatically) {
			// time is already running
			transitionToTimeRunning();
		} else {
			remindTimekeeperToStartTime();
			setState(State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
		}
	}

	private void weightChange(Athlete curLifter, State state) {
		AthleteSorter.liftingOrder(this.liftingOrder);
		// TODO change is checked as legit at marshall/announcer wrt to time left
		Athlete recomputedCurLifter = getLifters().get(0);
		Integer nextAttemptRequestedWeight = recomputedCurLifter.getNextAttemptRequestedWeight();
		logger.debug("weight change current={} {}, new={} {}",
			curLifter,
			curWeight,
			recomputedCurLifter,
			nextAttemptRequestedWeight);
		if (nextAttemptRequestedWeight > curWeight || recomputedCurLifter != curLifter) {
			// stop time while loaders adjust
			getTimer().stop();
			stopTimeOnAllDisplays();
			this.setCurAthlete(recomputedCurLifter);
			displayCurrentAthlete();
			// we need to re-announce
			setState(State.CURRENT_ATHLETE_DISPLAYED);
		}
	}

	/**
	 * weight change while a lift is being performed (bar lifted above knees)
	 * Lifting order is recomputed, so the displays can get it, but not the attempt
	 * board state.
	 * 
	 * @param curAthlete
	 * @param state
	 */
	private void weightChangeLiftInProgress(Athlete curLifter, State state) {
		AthleteSorter.liftingOrder(this.liftingOrder);
	}

	/* ============================================================= */

	private void clearAnnouncerWarnings() {
		// TODO Auto-generated method stub
	}

	private void clearTimekeeperWarnings() {
		// TODO Auto-generated method stub
	}

	private void displayCurrentAthlete() {
		Integer nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
		logger.info("current athlete = {} attempt {}, requested = {}, timer={}",
			getCurAthlete(),
			curAthlete.getAttemptedLifts() + 1,
			nextAttemptRequestedWeight,
			timeAllowed());
		curWeight = nextAttemptRequestedWeight;
	}

	private void displayCurrentWeight() {
		Integer nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
		logger.info("requested weight: {} (from curAthlete {})",
			nextAttemptRequestedWeight,
			getCurAthlete());
		curWeight = nextAttemptRequestedWeight;
	}

	private void init(List<Athlete> athletes) {
		this.eventBus = getEventBus();
		this.eventBus.register(this);
		this.curAthlete = null;
		this.setClockOwner(null);
		this.previousAthlete = null;
		this.liftingOrder = athletes;
		recomputeLiftingOrder();
		this.setState(State.INTERMISSION);
	}

	private void recomputeLiftingOrder() {
		AthleteSorter.liftingOrder(this.liftingOrder);
		this.setCurAthlete(this.liftingOrder.get(0));
		getTimer().setTimeRemaining(timeAllowed());
		logger.debug("recomputed lifting order curAthlete={} prevlifter={}", curAthlete, previousAthlete);
	}

	private void setCurAthlete(Athlete athlete) {
		logger.debug("changing curAthlete to {} [{}]", athlete, LoggerUtils.whereFrom());
		this.curAthlete = athlete;
	}

	private void remindAnnouncerToAnnounce() {
		// TODO Auto-generated method stub

	}

	private void remindTimekeeperToStartTime() {
		// TODO Auto-generated method stub

	}

	private void setClockOwner(Athlete athlete) {
		logger.debug("setting clock owner to {} [{}]", athlete, LoggerUtils.whereFrom());
		this.clockOwner = athlete;
	}

	private void showDownSignalOnSlaveDisplays() {
		// TODO Auto-generated method stub
	}

	private void showRefereeDecisionOnSlaveDisplays(FOPEvent.RefereeDecision e) {
		// TODO Auto-generated method stub

	}

	private void startTimeOnSlaveDisplays() {
		// TODO Auto-generated method stub

	}

	private void stopTimeOnAllDisplays() {
		// TODO Auto-generated method stub

	}

	private void stopTimeOnSlaveDisplays() {
		// TODO Auto-generated method stub

	}

	private void transitionToIntermission() {
		recomputeLiftingOrder();
		displayCurrentWeight();
		setState(State.INTERMISSION);
	}

	private void transitionToTimeRunning() {
		// start time on the various displays
		getTimer().start();
		startTimeOnSlaveDisplays();
		setClockOwner(getCurAthlete());
		// enable master to listening for decision
		unlockReferees();
		setState(State.TIME_RUNNING);
	}

	private void unexpectedEventInState(FOPEvent e, State announced) {
		// TODO Auto-generated method stub

	}

	private void unlockReferees() {
		// TODO Auto-generated method stub

	}

	private void warnTimekeeperPrematureStart() {
		// TODO Auto-generated method stub

	}

	public State getState() {
		return state;
	}

	void setState(State state) {
		logger.debug("entering {}", state);
		this.state = state;
	}

	private Athlete getClockOwner() {
		return clockOwner;
	}

	public void setTimer(ICountdownTimer timer) {
		this.timer = timer;
	}

}
