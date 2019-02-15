package org.ledocte.owlcms.state;

import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.AthleteRepository;
import org.ledocte.owlcms.data.athleteSort.AthleteSorter;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Keep the lifting order correct based on competition events
 * 
 * @author owlcms
 */
public class LiftingOrderState {

	private enum State {
		INTERMISSION,
		CURRENT_ATHLETE_DISPLAYED,
		ANNOUNCER_WAITING_FOR_TIMEKEEPER,
		TIMEKEEPER_WAITING_FOR_ANNOUNCER,
		TIME_RUNNING,
		TIME_STOPPED,
		DOWN_SIGNAL_SHOWN,
		DECISION_SHOWN,
	}

	private EventBus eventBus;
	@SuppressWarnings("unused")
	private FieldOfPlayState fieldOfPlayState;
	private boolean startTimeAutomatically;
	private State state;
	private List<Athlete> liftingOrder;
	private Athlete curLifter;
	private Athlete previousLifter;
	private Athlete clockOwner;
	private int timeRemaining;

	public LiftingOrderState(FieldOfPlayState fieldOfPlayState) {
		this.fieldOfPlayState = fieldOfPlayState;
		init(fieldOfPlayState);
		liftingOrder = AthleteSorter
			.liftingOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(fieldOfPlayState.getGroup(), true));
	}

	public LiftingOrderState(List<Athlete> athletes) {
		liftingOrder = AthleteSorter.liftingOrderCopy(athletes);
		init(new FieldOfPlayState());
	}

	public void callCurLifter(boolean startTime) {
		this.startTimeAutomatically = startTime;
		eventBus.post(new FOPEvent.AthleteAnnounced());
	}

	public Athlete getCurLifter() {
		return getLifters().get(0);
	}

	public List<Athlete> getLifters() {
		return liftingOrder;
	}

	public Athlete getPreviousLifter() {
		return previousLifter;
	}

	public Object getTimer() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTimeRemaining() {
		return timeRemaining;
	}

	@Subscribe
	public void handleFOPEvent(FOPEvent e) {
		// in all cases we can interrupt competition (real intermission, technical incident, etc.)
		if (e instanceof FOPEvent.IntermissionStarted) {
			transitionToIntermission();
		}
		
		switch (this.state) {

		case INTERMISSION:
			if (e instanceof FOPEvent.IntermissionDone) {
				recomputeLiftingOrder();
				displayCurrentAthlete();
				state = State.CURRENT_ATHLETE_DISPLAYED;
			} else if (e instanceof FOPEvent.AthleteAnnounced) {
				announce();
			} else  if (e instanceof FOPEvent.LiftingOrderUpdated) {
				transitionToIntermission();
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
				state = State.TIMEKEEPER_WAITING_FOR_ANNOUNCER;
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				checkChangeForCurrentAthleteBeforeFinalCall(State.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, State.CURRENT_ATHLETE_DISPLAYED);
			}
			break;

		case ANNOUNCER_WAITING_FOR_TIMEKEEPER:
			if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// time was started manually
				setTimeRunning();
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				checkChangeForCurrentAthleteBeforeFinalCall(State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
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
				state = State.TIMEKEEPER_WAITING_FOR_ANNOUNCER;
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				checkChangeForCurrentAthleteBeforeFinalCall(State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else {
				unexpectedEventInState(e, State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			}
			break;

		case TIME_RUNNING:
			if (e instanceof FOPEvent.DownSignal) {
				// 2 referees have given same decision
				showDownSignalOnSlaveDisplays();
				state = State.DOWN_SIGNAL_SHOWN;
			} else if (e instanceof FOPEvent.TimeStoppedByTimeKeeper) {
				// athlete lifted the bar
				stopTimeOnSlaveDisplays();
				state = State.TIME_STOPPED;
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				// coach is requesting change
				checkChangeForCurrentAthleteBeforeFinalCall(State.TIME_RUNNING);
			} else {
				unexpectedEventInState(e, State.TIME_RUNNING);
			}
			break;

		case TIME_STOPPED:
			if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// athlete put down the bar
				startTimeOnSlaveDisplays();
				state = State.TIME_RUNNING;
			} else if (e instanceof FOPEvent.LiftingOrderUpdated) {
				checkChangeForCurrentAthleteBeforeFinalCall(State.TIME_STOPPED);
			} else {
				unexpectedEventInState(e, State.TIME_STOPPED);
			}
			break;

		case DOWN_SIGNAL_SHOWN:
			if (e instanceof FOPEvent.RefereeDecision) {
				liftDone();
				showRefereeDecisionOnSlaveDisplays((FOPEvent.RefereeDecision) e);
				state = State.DECISION_SHOWN;
			} else {
				unexpectedEventInState(e, State.DOWN_SIGNAL_SHOWN);
			}
			break;

		case DECISION_SHOWN:
			if (e instanceof FOPEvent.LiftingOrderUpdated) {
				recomputeLiftingOrder();
				displayCurrentAthlete();
				state = State.CURRENT_ATHLETE_DISPLAYED;
			} else {
				unexpectedEventInState(e, State.DECISION_SHOWN);
			}
			break;			
		}
	}

	public void liftDone(boolean success) {
		this.startTimeAutomatically = true;
		this.eventBus.post(new FOPEvent.AthleteAnnounced());
		// because of automatic start, we are in TIME_RUNNING state
		this.eventBus.post(new FOPEvent.DownSignal());
		this.eventBus.post(new FOPEvent.RefereeDecision(success));
	}

	public void pause() {
		this.eventBus.post(new FOPEvent.TimeStoppedByTimeKeeper());
	}

	public int timeAllowed(Athlete a) {
		int timeAllowed;
		if (previousLifter == a) {
			if (clockOwner == a) {
				// we own the clock, clock keeps running
				timeAllowed = timeRemaining;
			} else if (timeRemaining != 60 && timeRemaining != 120) {
				// clock has started for someone else
				timeAllowed = 60;
			} else {
				timeAllowed = 120;
			}
		} else {
			if (clockOwner == a) {
				timeAllowed = timeRemaining;
			} else {
				timeAllowed = 60;
			}
		}
		return timeAllowed;
	}

	public void updateListsForLiftingOrderChange(Athlete lifter, boolean b, boolean c) {
		this.eventBus.post(new FOPEvent.LiftingOrderUpdated());
	}

	private void announce() {
		if (this.startTimeAutomatically) {
			// time is already running
			transitionToTimeRunning();
		} else {
			remindTimekeeperToStartTime();
			state = State.ANNOUNCER_WAITING_FOR_TIMEKEEPER;
		}
	}

	private void checkChangeForCurrentAthleteBeforeFinalCall(State state) {
		AthleteSorter.liftingOrder(this.liftingOrder);
		// coach has requested change
		Athlete recomputedCurLifter = getCurLifter();
		if (curLifter == recomputedCurLifter) {
			// re-display the athlete under new weight,
			// stop time while loaders adjust
			stopTimeOnAllDisplays();
			displayCurrentAthlete();
			state = State.CURRENT_ATHLETE_DISPLAYED;
		} else {
			// the change is recorded in the lifting order, but is ignored
			// until the current lift is done.
		}
	}

	private void clearAnnouncerWarnings() {
		// TODO Auto-generated method stub
	}

	private void clearTimekeeperWarnings() {
		// TODO Auto-generated method stub
	}

	private void displayCurrentAthlete() {
		// FIXME: set the clocks to timeAllowed
		// FIXME: BUT do no lose remaining -- only change remaining when the clock starts to run 
	}

	private void displayCurrentWeight() {
		// TODO Auto-generated method stub
		
	}

	private void init(FieldOfPlayState fieldOfPlayState) {
		this.eventBus = fieldOfPlayState.getEventBus();
		this.eventBus.register(this);
		this.curLifter = null;
		this.clockOwner = null;
		this.previousLifter = null;
	}

	private void liftDone() {
		previousLifter = this.getCurLifter();
		AthleteSorter.liftingOrder(this.liftingOrder);
		curLifter = this.getCurLifter();
	}

	private void recomputeLiftingOrder() {
		AthleteSorter.liftingOrder(this.liftingOrder);
		curLifter = this.getCurLifter();
	}

	private void remindAnnouncerToAnnounce() {
		// TODO Auto-generated method stub

	}

	/* ============================================================= */

	private void remindTimekeeperToStartTime() {
		// TODO Auto-generated method stub

	}

	private void setClockOwner(Athlete curLifter) {
		// TODO Auto-generated method stub

	}

	private void setTimeRunning() {
		clockOwner = curLifter;
		state = State.TIME_RUNNING;
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
		state = State.INTERMISSION;
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

}
