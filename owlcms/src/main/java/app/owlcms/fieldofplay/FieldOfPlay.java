/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.fieldofplay;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.FOPEvent.DownSignal;
import app.owlcms.fieldofplay.FOPEvent.WeightChange;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class describes one field of play at runtime.
 * 
 * It encapsulates the in-memory data structures used to describe the state of
 * the competition and links them to the database descriptions of the group and
 * platform.
 * 
 * The main method is {@link #handleFOPEvent(FOPEvent)} which implements a state
 * automaton and processes events received on the event bus.
 * 
 * @author owlcms
 */
public class FieldOfPlay {
	
	final private Logger logger = (Logger) LoggerFactory.getLogger(FieldOfPlay.class);
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	{
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
 
	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * the clock owner is the last athlete for whom the clock has actually started.
	 */
	private Athlete clockOwner;
	private Athlete curAthlete;
	private EventBus fopEventBus = null;
	private EventBus uiEventBus = null;
	private Group group = null;
	private String name;
	private Platform platform = null;
	private Athlete previousAthlete;
	private boolean startTimeAutomatically;
	private FOPState state;
	private ICountdownTimer timer;
	private ICountdownTimer breakTimer;
	
	
	private List<Athlete> liftingOrder;
	private List<Athlete> displayOrder;


	/**
	 * Instantiates a new field of play state.
	 * When using this constructor {@link #init(List, ICountdownTimer)} must be used to provide the athletes
	 * and set the timer
	 *
	 * @param group    the group (to get details such as name, and to reload
	 *                 athletes)
	 * @param platform2 the platform (to get details such as name)
	 */
	public FieldOfPlay(Group group, Platform platform2) {
		this.name = platform2.getName();
		this.fopEventBus = new EventBus("FOP-"+name);
		this.uiEventBus = new EventBus("UI-"+name);
		this.timer = null;
		this.breakTimer = new RelayTimer(this);
		this.platform = platform2;
	}
	
	/**
	 * Instantiates a new field of play state.
	 * This constructor is used for testing.
	 *
	 * @param athletes the athletes
	 * @param timer1    the timer
	 */
	public FieldOfPlay(List<Athlete> athletes, ICountdownTimer timer1, ICountdownTimer breakTimer1) {
		this.name = "test";
		this.fopEventBus = new EventBus("FOP-"+this.name);
		this.uiEventBus = new EventBus("UI-"+this.name);
		init(athletes, timer1, breakTimer1);
	}

	/**
	 * @return the current athlete (to be called, or currently lifting)
	 */
	public Athlete getCurAthlete() {
		return curAthlete;
	}

	public List<Athlete> getDisplayOrder() {
		return displayOrder;
	}

	/**
	 * @return the fopEventBus
	 */
	public EventBus getFopEventBus() {
		return fopEventBus;
	}

	/**
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}

	/**
	 * @return the lifters
	 */
	public List<Athlete> getLiftingOrder() {
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

	/**
	 * @return the previous athlete to have lifted (can be the same as current)
	 */
	public Athlete getPreviousAthlete() {
		return previousAthlete;
	}

	/**
	 * @return the current state
	 */
	public FOPState getState() {
		return state;
	}

	/**
	 * @return the time allowed for the next athlete.
	 */
	public int getTimeAllowed() {
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

	/**
	 * @return the server-side timer that tracks the time used
	 */
	public ICountdownTimer getTimer() {
		return this.timer;
	}


	/**
	 * @return the bus on which we post commands for the listening browser pages.
	 */
	public EventBus getUiEventBus() {
		return uiEventBus;
	}

	/**
	 * Handle FOP event.
	 * FOP (Field of Play) events inform us of what is happening (e.g. timer started by timekeeper, decision given by official, etc.)
	 * The current state determines what we do with the event.  Typically, we update the state of the field of play (e.g. time is now
	 * running) and we issue commands to the listening user interfaces (e.g. start or stop time being displayed, show the decision, etc.)
	 * 
	 * Normally, a given user interface will issue a FOP event on our fopEventBus, this method reacts to the event by updating state, and we issue the resulting
	 * commands on the @link uiEventBus.
	 *
	 * @param e the event
	 */
	@Subscribe
	public void handleFOPEvent(FOPEvent e) {
		logger.debug("state {}, event received {}", this.getState(), e.getClass().getSimpleName());
		// it is always possible to explicitly interrupt competition (break between the two lifts, technical
		// incident, etc.)
		if (e instanceof FOPEvent.BreakStarted) {
			transitionToBreak();
		}

		switch (this.getState()) {
		
		case INACTIVE:
			if (e instanceof FOPEvent.BreakStarted) {
				getTimer().start();
				setState(FOPState.BREAK);
			} else if (e instanceof FOPEvent.StartLifting) {
				recomputeLiftingOrder();
				uiDisplayCurrentAthleteAndTime();
				setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
			} else if (e instanceof FOPEvent.AthleteAnnounced) {
				announce();
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChange(((FOPEvent.WeightChange) e).getAthlete());
				setState(FOPState.INACTIVE);
			} else {
				unexpectedEventInState(e, FOPState.INACTIVE);
			}
			break;

		case BREAK:
			if (e instanceof FOPEvent.StartLifting) {
				getBreakTimer().stop();
				recomputeLiftingOrder();
				// we set the state before emitting the display order
				// beacuse attempt boards ignore updates while in BREAK state
				setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
				uiDisplayCurrentAthleteAndTime();
			} else if (e instanceof FOPEvent.BreakPaused) {
				getBreakTimer().stop();
				getUiEventBus().post(new UIEvent.BreakPaused(e.getOrigin()));
			} else if (e instanceof FOPEvent.BreakStarted) {
				getBreakTimer().start();
				getUiEventBus().post(new UIEvent.BreakStarted(e.getOrigin()));
			} else if (e instanceof FOPEvent.AthleteAnnounced) {
				getBreakTimer().stop();
				getTimer().stop();
				announce();
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChangeDoNotDisturb(curAthlete);
				setState(FOPState.BREAK);
			} else {
				unexpectedEventInState(e, FOPState.BREAK);
			}
			break;

		case CURRENT_ATHLETE_DISPLAYED:
			if (e instanceof FOPEvent.AthleteAnnounced) {
				announce(); // will set next state depending on automatic or not
			} else if (e instanceof FOPEvent.TimeStartedManually) {
				// time was started prematurely before announcer hit "announce" button
				warnTimekeeperPrematureStart();
				remindAnnouncerToAnnounce();
				setState(FOPState.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChange(curAthlete);
				setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
			} else if (e instanceof FOPEvent.ForceTime) {
				// need to set time
				getTimer().setTimeRemaining(((FOPEvent.ForceTime) e).timeAllowed);
				setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, FOPState.CURRENT_ATHLETE_DISPLAYED);
			}
			break;

		case ANNOUNCER_WAITING_FOR_TIMEKEEPER:
			if (e instanceof FOPEvent.TimeStartedManually) {
				getTimer().start();
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChange(curAthlete);
				setState(FOPState.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
			} else {
				unexpectedEventInState(e, FOPState.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
			}
			break;

		case TIMEKEEPER_WAITING_FOR_ANNOUNCER:
			if (e instanceof FOPEvent.AthleteAnnounced) {
				// remove the warnings on announcer and timekeeper
				clearTimekeeperWarnings();
				clearAnnouncerWarnings();
				getTimer().start();
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.TimeStartedManually) {
				// we are already in this state, do nothing (we could escalate)
				setState(FOPState.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChange(curAthlete);
				setState(FOPState.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else {
				unexpectedEventInState(e, FOPState.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			}
			break;

		case TIME_RUNNING:
			if (e instanceof FOPEvent.DownSignal) {
				// 2 referees have given same decision
				getTimer().stop();
				uiShowDownSignalOnSlaveDisplays((DownSignal) e);
				setState(FOPState.DOWN_SIGNAL_VISIBLE);
			} else if (e instanceof FOPEvent.TimeStoppedManually) {
				// athlete lifted the bar
				getTimer().stop();
				setState(FOPState.TIME_STOPPED);
			} else if (e instanceof FOPEvent.WeightChange) {
				WeightChange wc = (FOPEvent.WeightChange)e;
				Athlete athlete = wc.getAthlete();
				if (athlete == curAthlete) {
					// coach is requesting change, stop clock
					getTimer().stop();
					weightChange(curAthlete);
					setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
				} else {
					// other athlete is changing, leave clock running
					weightChangeDoNotDisturb(curAthlete);
					setState(FOPState.TIME_RUNNING);
				}
			} else if (e instanceof FOPEvent.RefereeDecision) {
				// in theory not possible, we would get down signal before.
				getTimer().stop();
				this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
				this.setClockOwner(null);
				decision(e);
			} else if (e instanceof FOPEvent.TimeOver) {
				// timer got down to 0
				// getTimer() signals this, nothing else required for timer
				// rule says referees must give reds
				setState(FOPState.TIME_STOPPED);
			} else {
				unexpectedEventInState(e, FOPState.TIME_RUNNING);
			}
			break;

		case TIME_STOPPED:
			if (e instanceof FOPEvent.DownSignal) {
				// 2 referees have given same decision
				getTimer().stop(); // paranoia
				uiShowDownSignalOnSlaveDisplays((DownSignal) e);
				setState(FOPState.DOWN_SIGNAL_VISIBLE);
			} else if (e instanceof FOPEvent.TimeStartedManually) {
				// timekeeper mistake, start time again
				getTimer().start();
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.WeightChange) {
				WeightChange wc = (FOPEvent.WeightChange)e;
				Athlete athlete = wc.getAthlete();
				if (athlete.equals(curAthlete)) {
					getTimer().stop();
					// clock is already stopped, coach requesting change
					weightChange(curAthlete);
					setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
				} else {
					// other athlete is changing, leave clock alone
					weightChangeDoNotDisturb(curAthlete);
					setState(FOPState.TIME_STOPPED);
				}
			} else if (e instanceof FOPEvent.RefereeDecision) {
				getTimer().stop();
				this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
				this.setClockOwner(null);
				decision(e);
			} else if (e instanceof FOPEvent.ForceTime) {
				// need to set time
				getTimer().setTimeRemaining(((FOPEvent.ForceTime) e).timeAllowed);
				setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, FOPState.TIME_STOPPED);
			}
			break;

		case DOWN_SIGNAL_VISIBLE:
			this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
			this.setClockOwner(null);
			if (e instanceof FOPEvent.RefereeDecision) {
				getTimer().stop();
				decision(e);
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChangeDoNotDisturb(curAthlete);
				setState(FOPState.DOWN_SIGNAL_VISIBLE);
			} else {
				unexpectedEventInState(e, FOPState.DOWN_SIGNAL_VISIBLE);
			}
			break;

		case DECISION_VISIBLE:
			if (e instanceof FOPEvent.RefereeDecision) {
				// decision reversal
				decision(e);
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChangeDoNotDisturb(curAthlete);
				setState(FOPState.DECISION_VISIBLE);
			} else if (e instanceof FOPEvent.DecisionReset) {
				uiEventBus.post(new UIEvent.DecisionReset(e.origin));
				clockOwner = null;
				recomputeLiftingOrder();
				uiDisplayCurrentAthleteAndTime();
				setState(FOPState.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, FOPState.DECISION_VISIBLE);
			}
			break;
		}
	}

	public void init(List<Athlete> athletes, ICountdownTimer timer, ICountdownTimer breakTimer) {
		this.timer = timer;
		this.fopEventBus = getFopEventBus();
		this.fopEventBus.register(this);
		this.curAthlete = null;
		this.setClockOwner(null);
		this.previousAthlete = null;
		this.liftingOrder = athletes;
		if (athletes != null && athletes.size() > 0) {
			recomputeLiftingOrder();
		}
		this.setState(FOPState.INACTIVE);
	}

	public void setDisplayOrder(List<Athlete> displayOrder) {
		this.displayOrder = displayOrder;
	}

	/**
	 * Sets the group.
	 *
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the platform.
	 *
	 * @param platform the platform to set
	 */
	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	/**
	 * Sets the start time automatically.
	 *
	 * @param startTime the new start time automatically
	 */
	public void setStartTimeAutomatically(boolean startTime) {
		this.startTimeAutomatically = startTime;
	}

	/**
	 * Sets the timer.
	 *
	 * @param timer the new timer
	 */
	public void setTimer(ICountdownTimer timer) {
		this.timer = timer;
	}


	/**
	 * Switch group.
	 *
	 * @param group the group
	 */
	public void switchGroup(Group group, Object origin) {
		initGroup(group, origin);
		logger.info("{} start lifting for group {}", this.getName(), (group != null ? group.getName() : group));
		getFopEventBus().post(new FOPEvent.StartLifting(origin));
	}


	public void initGroup(Group group, Object origin) {
		this.group = group;
		if (group != null) {
			logger.info("{} loading data for group {}", this.getName(), (group != null ? group.getName() : group));
			List<Athlete> findAllByGroupAndWeighIn = AthleteRepository.findAllByGroupAndWeighIn(group, true);
			init(findAllByGroupAndWeighIn, timer, breakTimer);
		} else {
			init(new ArrayList<Athlete>(), timer, breakTimer);
		}
	}

	private void announce() {
		if (this.startTimeAutomatically) {
			getTimer().start();
			// time is already running
			transitionToTimeRunning();
		} else {
			remindTimekeeperToStartTime();
			setState(FOPState.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
		}
	}

	private void clearAnnouncerWarnings() {
		// TODO clearAnnouncerWarnings
	}

	private void clearTimekeeperWarnings() {
		// TODO clearTimekeeperWarnings
	}

	private void decision(FOPEvent e) {
		FOPEvent.RefereeDecision decision = (FOPEvent.RefereeDecision) e;
		if (decision.success) {
			curAthlete.successfulLift();
		} else {
			curAthlete.failedLift();
		}
		AthleteRepository.save(curAthlete);
		uiShowRefereeDecisionOnSlaveDisplays(decision);
//		fopDecisionReset();

		setState(FOPState.DECISION_VISIBLE);
	}

	private Athlete getClockOwner() {
		return clockOwner;
	}

	private synchronized void recomputeLiftingOrder() {
		AthleteSorter.liftingOrder(this.liftingOrder);
		setDisplayOrder(AthleteSorter.displayOrderCopy(this.liftingOrder));
		this.setCurAthlete(this.liftingOrder.isEmpty() ? null : this.liftingOrder.get(0));
		getTimer().setTimeRemaining(getTimeAllowed());
		logger.debug("recomputed lifting order curAthlete={} prevlifter={}",
			curAthlete != null ? curAthlete.getFullName() : "",
			previousAthlete != null ? previousAthlete.getFullName() : "");
	}

	private void remindAnnouncerToAnnounce() {
		// TODO remindAnnouncerToAnnounce
	}

	private void remindTimekeeperToStartTime() {
		// TODO remindTimekeeperToStartTime
	}

	private void setClockOwner(Athlete athlete) {
		logger.debug("setting clock owner to {} [{}]", athlete, LoggerUtils.whereFrom());
		this.clockOwner = athlete;
	}

	private void setCurAthlete(Athlete athlete) {
		logger.debug("changing curAthlete to {} [{}]", athlete, LoggerUtils.whereFrom());
		this.curAthlete = athlete;
	}

	private void setPreviousAthlete(Athlete athlete) {
		logger.debug("setting previousAthlete to {}", curAthlete);
		this.previousAthlete = athlete;
	}

	private void transitionToBreak() {
		uiEventBus.post(new UIEvent.BreakStarted(this.getOrigin()));
		setState(FOPState.BREAK);
	}

	private void transitionToTimeRunning() {
		setClockOwner(getCurAthlete());
		// enable master to listening for decision
		unlockReferees();
		setState(FOPState.TIME_RUNNING);
	}

	private void uiDisplayCurrentAthleteAndTime() {
		Integer clock = getTimer().getTimeRemaining();
		Integer nextAttemptRequestedWeight = 0;
		if (curAthlete != null) {
			nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
		}
		Athlete nextAthlete = liftingOrder.size() > 0 ? liftingOrder.get(1) : null;

		uiEventBus.post(new UIEvent.LiftingOrderUpdated(curAthlete, nextAthlete, previousAthlete, liftingOrder, displayOrder, clock, this.getOrigin()));
	
		logger.info("current athlete = {} attempt {}, requested = {}, timeAllowed={}",
			curAthlete,
			curAthlete != null ? curAthlete.getAttemptedLifts() + 1 : 0,
			nextAttemptRequestedWeight,
			clock);
	}

	private Object getOrigin() {
		return this;
	}



	@SuppressWarnings("unused")
	private void uiDisplayCurrentWeight() {
		Integer nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
		uiEventLogger.info("requested weight: {} (from curAthlete {})",
			nextAttemptRequestedWeight,
			getCurAthlete());
	}

	private void uiShowDownSignalOnSlaveDisplays(FOPEvent.DownSignal e) {
		uiEventLogger.debug("showDownSignalOnSlaveDisplays");
		uiEventBus.post(new UIEvent.DownSignal(e.origin));
	}

	private void uiShowRefereeDecisionOnSlaveDisplays(FOPEvent.RefereeDecision e) {
		uiEventLogger.debug("showRefereeDecisionOnSlaveDisplays");
		uiEventBus.post(new UIEvent.RefereeDecision(e.success,e.ref1,e.ref2,e.ref3,e.origin));
	}

	private void unexpectedEventInState(FOPEvent e, FOPState state) {
		logger.warn("Unexpected event: {} in state {}",e,state);
	}

	private void unlockReferees() {
		// TODO unlockReferees
		uiEventLogger.debug("unlockReferees");
	}

	private void warnTimekeeperPrematureStart() {
		// TODO warnTimekeeperPrematureStart
		uiEventLogger.debug("warnTimekeeperPrematureStart");

	}

	private void weightChange(Athlete curLifter) {
		recomputeLiftingOrder();
		uiDisplayCurrentAthleteAndTime();
	}

	/**
	 * weight change while a lift is being performed (bar lifted above knees)
	 * Lifting order is recomputed, so the app.owlcms.ui.displayselection can get it, but not the attempt
	 * board state.
	 * 
	 * @param curAthlete
	 */
	private void weightChangeDoNotDisturb(Athlete curLifter) {
		AthleteSorter.liftingOrder(this.liftingOrder);
		this.setDisplayOrder(AthleteSorter.displayOrderCopy(this.liftingOrder));
		//TODO update lifting order boards but not attempt bar.
	}

	protected void fopDecisionReset() {
//		FOPEvent.DecisionReset event = new FOPEvent.DecisionReset(null);
//		fopPostAfterDelay(event, 3);
	}

	/**
	 * Sets the state.
	 *
	 * @param state the new state
	 */
	void setState(FOPState state) {
		logger.debug("entering {} {}", state, LoggerUtils.whereFrom());
		this.state = state;
	}

	public ICountdownTimer getBreakTimer() {
		return this.breakTimer;
	}

}
