package org.ledocte.owlcms.state;

import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.AthleteRepository;
import org.ledocte.owlcms.data.athleteSort.AthleteSorter;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.platform.Platform;
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
	private Athlete curLifter;
	private EventBus eventBus = null;
	private Group group = null;
	private List<Athlete> liftingOrder;
	private String name;
	private Platform platform = null;
	private Athlete previousLifter;
	private boolean startTimeAutomatically;
	private State state;
	private int timeRemaining;

	public FieldOfPlayState(Group group, Platform platform) {
		this.group = group;
		this.platform = platform;
		this.name = platform.getName();
		init(AthleteRepository.findAllByGroupAndWeighIn(group, true));
	}

	public FieldOfPlayState(List<Athlete> athletes) {
		init(athletes);
	}

	public void setStartTimeAutomatically(boolean startTime) {
		this.startTimeAutomatically = startTime;
	}

	public Athlete getCurLifter() {
		return curLifter;
	}

	/**
	 * @return the eventBus
	 */
	public EventBus getEventBus() {
		if (eventBus == null) {
			eventBus = new EventBus();
			logger.debug("{}", eventBus.identifier());
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

	public Athlete getPreviousLifter() {
		return previousLifter;
	}

	public CountdownTimer getTimer() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTimeRemaining() {
		return timeRemaining;
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
				weightChange(curLifter, State.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, State.CURRENT_ATHLETE_DISPLAYED);
			}
			break;

		case ANNOUNCER_WAITING_FOR_TIMEKEEPER:
			if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// time was started manually
				setTimeRunning();
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChange(curLifter, State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
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
				weightChange(curLifter, State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else {
				unexpectedEventInState(e, State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			}
			break;

		case TIME_RUNNING:
			if (e instanceof FOPEvent.DownSignal) {
				// 2 referees have given same decision
				showDownSignalOnSlaveDisplays();
				setState(State.DOWN_SIGNAL_VISIBLE);
			} else if (e instanceof FOPEvent.TimeStoppedByTimeKeeper) {
				// athlete lifted the bar
				stopTimeOnSlaveDisplays();
				setState(State.TIME_STOPPED);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				// coach is requesting change
				weightChange(curLifter, State.TIME_RUNNING);
			} else {
				unexpectedEventInState(e, State.TIME_RUNNING);
			}
			break;

		case TIME_STOPPED:
			if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// timekeeper mistake
				startTimeOnSlaveDisplays();
				setState(State.TIME_RUNNING);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChangeLiftInProgress(curLifter, State.TIME_STOPPED);
			} else {
				unexpectedEventInState(e, State.TIME_STOPPED);
			}
			break;

		case DOWN_SIGNAL_VISIBLE:
			if (e instanceof FOPEvent.RefereeDecision) {
				showRefereeDecisionOnSlaveDisplays((FOPEvent.RefereeDecision) e);
				setState(State.DECISION_VISIBLE);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChangeLiftInProgress(curLifter, State.DOWN_SIGNAL_VISIBLE);
			} else {
				unexpectedEventInState(e, State.DOWN_SIGNAL_VISIBLE);
			}
			break;

		case DECISION_VISIBLE:
			if (e instanceof FOPEvent.RefereeDecision) {
				showRefereeDecisionOnSlaveDisplays((FOPEvent.RefereeDecision) e);
				setState(State.DECISION_VISIBLE);
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				weightChangeLiftInProgress(curLifter, State.DECISION_VISIBLE);
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
		Athlete a = getCurLifter();
		int timeAllowed;
		if (previousLifter == a) {
			if (clockOwner == a) {
				// we own the clock, clock keeps running
				timeAllowed = timeRemaining;
			} else if (timeRemaining != 60000 && timeRemaining != 120000) {
				// clock has started for someone else
				timeAllowed = 60000;
			} else {
				timeAllowed = 120000;
			}
		} else {
			if (clockOwner == a) {
				timeAllowed = timeRemaining;
			} else {
				timeAllowed = 60000;
			}
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
		// stop time while loaders adjust
		stopTimeOnAllDisplays();
		this.setCurLifter(recomputedCurLifter);
		displayCurrentAthlete();
		// we need to re-announce
		state = State.CURRENT_ATHLETE_DISPLAYED;
	}

	/**
	 * weight change while a lift is being performed (bar lifted above knees)
	 * 
	 * @param curLifter
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
		// FIXME: set the clocks to timeAllowed
		// FIXME: BUT do no lose remaining -- only change remaining when the clock
		// starts to run
	}

	private void displayCurrentWeight() {
		// TODO Auto-generated method stub

	}

	private void init(List<Athlete> athletes) {
		this.eventBus = getEventBus();
		this.eventBus.register(this);
		this.curLifter = null;
		this.clockOwner = null;
		this.previousLifter = null;
		this.liftingOrder = AthleteSorter.liftingOrderCopy(athletes);
		this.setState(State.INTERMISSION);
	}

	private void recomputeLiftingOrder() {
		previousLifter = this.getCurLifter();
		AthleteSorter.liftingOrder(this.liftingOrder);
		this.setCurLifter(this.liftingOrder.get(0));
		logger.debug("recomputed lifting order curLifter={} prevlifter={}", curLifter, previousLifter);
	}

	private void setCurLifter(Athlete athlete) {
		logger.debug("changing curLifter to {}", athlete);
		this.curLifter = athlete;
	}

	private void remindAnnouncerToAnnounce() {
		// TODO Auto-generated method stub

	}

	private void remindTimekeeperToStartTime() {
		// TODO Auto-generated method stub

	}

	private void setClockOwner(Athlete curLifter) {
		// TODO Auto-generated method stub

	}

	private void setTimeRunning() {
		clockOwner = curLifter;
		setState(State.TIME_RUNNING);
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
		// time has started; current lifter will go on with remaining time
		// if there are weight changes, and the two minutes privilege will be lost
		setClockOwner(getCurLifter());
		// start time on the various displays (other than the master)
		startTimeOnSlaveDisplays();
		// enable master to listening for decision
		unlockReferees();
		setTimeRunning();
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

}
