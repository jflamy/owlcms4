package org.ledocte.owlcms.state;

import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Keep the lifting order correct based on competition events
 * 
 * @author owlcms
 */
public class LiftingOrderState {

	private enum State {
		ANNOUNCER_WAITING_FOR_TIMEKEEPER,
		DECISION_SHOWN,
		DOWN_SIGNAL_SHOWN,
		CURRENT_ATHLETE_DISPLAYED,
		PREMATURE_TIMEKEEPER,
		TIMEKEEPER_WAITING_FOR_ANNOUNCER,
		TIME_RUNNING,
		WEIGHT_CHANGE
	}

	private EventBus eventBus;
	@SuppressWarnings("unused")
	private FieldOfPlayState fieldOfPlayState;
	private boolean startTimeAutomatically;
	private State state;

	public LiftingOrderState(FieldOfPlayState fieldOfPlayState) {
		this.fieldOfPlayState = fieldOfPlayState;
		this.eventBus = fieldOfPlayState.getEventBus();
		this.eventBus.register(this);
	}

	public LiftingOrderState(List<Athlete> athletes) {
		// TODO Auto-generated constructor stub
	}

	private void clearAnnouncerWarnings() {
		// TODO Auto-generated method stub
	}

	private void clearTimekeeperWarnings() {
		// TODO Auto-generated method stub
	}

	private void displayCurrentAthlete() {
		// TODO Auto-generated method stub
	}

	/*=============================================================*/
	
	public Athlete getCurLifter() {
		return getLifters().get(0);
	}

	public List<Athlete> getLifters() {
		// TODO Auto-generated method stub
		return null;
	}

	public Athlete getPreviousLifter() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getTimer() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTimeRemaining() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Subscribe
	public void handleLiftingOrderEvent(FOPEvent e) {
		switch (this.state) {

		case CURRENT_ATHLETE_DISPLAYED:
			if (e instanceof FOPEvent.AthleteAnnounced) {
				if (this.startTimeAutomatically) {
					// time is already running
					transitionToTimeRunning();
				} else {
					remindTimekeeperToStartTime();
					state = State.ANNOUNCER_WAITING_FOR_TIMEKEEPER;
				}
			} else if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// time was started prematurely before announcer hit "announce" button
				warnTimekeeperPrematureStart();
				remindAnnouncerToAnnounce();
				state = State.TIMEKEEPER_WAITING_FOR_ANNOUNCER;
			} else {
				unexpectedState(e, State.CURRENT_ATHLETE_DISPLAYED);
			}
			break;

		case ANNOUNCER_WAITING_FOR_TIMEKEEPER:
			if (e instanceof FOPEvent.TimeStartedByTimeKeeper) {
				// time was started manually
				state = State.TIME_RUNNING;
			} else {
				unexpectedState(e, State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
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
			} else {
				unexpectedState(e, State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			}
			break;

		case TIME_RUNNING:
			if (e instanceof FOPEvent.DownSignal) {
				showDownSignalOnSlaveDisplays();
				state = State.DOWN_SIGNAL_SHOWN;
			} else {
				unexpectedState(e, State.TIME_RUNNING);
			}
			break;

		case DOWN_SIGNAL_SHOWN:
			if (e instanceof FOPEvent.RefereeDecision) {
				showRefereeDecisionOnSlaveDisplays();
				state = State.DECISION_SHOWN;
			} else {
				unexpectedState(e, State.DOWN_SIGNAL_SHOWN);
			}
			break;

		case WEIGHT_CHANGE:
		default:
			if (e instanceof FOPEvent.LiftingOrderUpdated) {
				showDownSignalOnSlaveDisplays();
				displayCurrentAthlete();
				state = State.CURRENT_ATHLETE_DISPLAYED;
			} else {
				unexpectedEvent(e);
			}
			break;

		}

	}

	public void liftDone(Athlete lifter, boolean b) {
		// TODO Auto-generated method stub
	}

	public void pause() {
		// TODO Auto-generated method stub

	}

	private void remindAnnouncerToAnnounce() {
		// TODO Auto-generated method stub

	}

	private void remindTimekeeperToStartTime() {
		// TODO Auto-generated method stub

	}

	private void showDownSignalOnSlaveDisplays() {
		// TODO Auto-generated method stub

	}

	private void showRefereeDecisionOnSlaveDisplays() {
		// TODO Auto-generated method stub

	}

	public int timeAllowed(Athlete a) {
		// TODO Auto-generated method stub
		return 0;
	}

	private void transitionToTimeRunning() {
		// time has started; current lifter will go on with remaining time
		// if there are weight changes, and the two minutes privilege will be lost
		setClockOwner(getCurLifter());
		// start time on the various displays (other than the master)
		startTimeOnSlaveDisplays();
		// enable master to listening for decision
		unlockReferees();
		state = State.TIME_RUNNING;
	}

	private void setClockOwner(Athlete curLifter) {
		// TODO Auto-generated method stub
		
	}

	private void unlockReferees() {
		// TODO Auto-generated method stub

	}

	private void startTimeOnSlaveDisplays() {
		// TODO Auto-generated method stub

	}

	private void unexpectedEvent(FOPEvent e) {
		// TODO Auto-generated method stub

	}

	private void unexpectedState(FOPEvent e, State announced) {
		// TODO Auto-generated method stub

	}

	public void updateListsForLiftingOrderChange(Athlete lifter, boolean b, boolean c) {
		// TODO Auto-generated method stub

	}

	private void warnTimekeeperPrematureStart() {
		// TODO Auto-generated method stub

	}

	public void callCurLifter() {
		// TODO Auto-generated method stub
		
	}

}
