/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.state;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.state.FOPEvent.DownSignal;
import app.owlcms.state.FOPEvent.WeightChange;
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
public class FieldOfPlayState {
	
	final private static Logger logger = (Logger) LoggerFactory.getLogger(FieldOfPlayState.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("owlcms.uiEventLogger");
	protected void init_loggers() {
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.INFO);
	}
 
	/**
	 *  Current state of the competition field of play.
	 */
	public enum State {

		/** between sessions and during breaks. */
		INTERMISSION,

		/** current athlete displayed on attempt board. */
		CURRENT_ATHLETE_DISPLAYED,

		/**
		 * (only with manual time start) announcer has announced athlete and indicated
		 * so, waiting for timekeeper to start time.
		 */
		ANNOUNCER_WAITING_FOR_TIMEKEEPER,

		/**
		 * (only with manual time start) timekeeper waiting for announcer to confirm she
		 * has announced.
		 */
		TIMEKEEPER_WAITING_FOR_ANNOUNCER,

		/**
		 * time is running. Either automatically started on announce (if using the
		 * default "start on announce", or manually by timekeeper (in traditional mode)
		 */
		TIME_RUNNING,

		/** The time is stopped. */
		TIME_STOPPED,

		/** The down signal is visible. */
		DOWN_SIGNAL_VISIBLE,

		/** The decision is visible. */
		DECISION_VISIBLE,
	}


	/**
	 * Gets the logger.
	 *
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	private Athlete clockOwner;
	private Athlete curAthlete;
	private EventBus eventBus = null;
	private EventBus uiEventBus = null;
	private Group group = null;
	private String name;
	private Platform platform = null;
	private Athlete previousAthlete;
	private boolean startTimeAutomatically;
	private State state;
	private ICountdownTimer timer;
	
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
	public FieldOfPlayState(Group group, Platform platform2) {
		init_loggers(); 
		
		this.name = platform2.getName();
		this.eventBus = new EventBus("FOP-"+name);
		this.uiEventBus = new EventBus("UI-"+name);
		this.timer = null;
		this.platform = platform2;
	}


	
	/**
	 * Instantiates a new field of play state.
	 * This version is used for testing.
	 *
	 * @param athletes the athletes
	 * @param timer1    the timer
	 */
	public FieldOfPlayState(List<Athlete> athletes, ICountdownTimer timer1) {
		this.name = "test";
		this.eventBus = new EventBus("FOP-"+this.name);
		this.uiEventBus = new EventBus("UI-"+this.name);
		this.setTimer(timer1);
		init(athletes, timer);
	}

	/**
	 * Gets the cur athlete.
	 *
	 * @return the cur athlete
	 */
	public Athlete getCurAthlete() {
		return curAthlete;
	}

	public List<Athlete> getDisplayOrder() {
		return displayOrder;
	}

	/**
	 * Gets the event bus.
	 *
	 * @return the eventBus
	 */
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * Gets the group.
	 *
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}

	/**
	 * Gets the lifters.
	 *
	 * @return the lifters
	 */
	public List<Athlete> getLiftingOrder() {
		return liftingOrder;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the platform.
	 *
	 * @return the platform
	 */
	public Platform getPlatform() {
		return platform;
	}

	/**
	 * Gets the previous athlete.
	 *
	 * @return the previous athlete
	 */
	public Athlete getPreviousAthlete() {
		return previousAthlete;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public State getState() {
		return state;
	}

	/**
	 * Time allowed.
	 *
	 * @return the int
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
	 * Gets the timer.
	 *
	 * @return the timer
	 */
	public ICountdownTimer getTimer() {
		return this.timer;
	}


	public EventBus getUiEventBus() {
		return uiEventBus;
	}

	/**
	 * Handle FOP event.
	 *
	 * @param e the e
	 */
	@Subscribe
	public void handleFOPEvent(FOPEvent e) {
		logger.debug("state {}, event received {}", this.getState(), e.getClass().getSimpleName());
		// in all cases we can interrupt competition (real intermission, technical
		// incident, etc.)
		if (e instanceof FOPEvent.IntermissionStarted) {
			transitionToIntermission();
		}

		switch (this.getState()) {

		case INTERMISSION:
			if (e instanceof FOPEvent.IntermissionDone) {
				recomputeLiftingOrder();
				uiDisplayCurrentAthleteAndTime();

				setState(State.CURRENT_ATHLETE_DISPLAYED);
			} else if (e instanceof FOPEvent.AthleteAnnounced) {
				announce();
			} else if (e instanceof FOPEvent.WeightChange) {
				// display new current weight, stay in current state
				weightChange(curAthlete);
				setState(State.INTERMISSION);
			} else {
				unexpectedEventInState(e, State.INTERMISSION);
			}
			break;

		case CURRENT_ATHLETE_DISPLAYED:
			if (e instanceof FOPEvent.AthleteAnnounced) {
				announce(); // will set next state depending on automatic or not
			} else if (e instanceof FOPEvent.TimeStartedManually) {
				// time was started prematurely before announcer hit "announce" button
				warnTimekeeperPrematureStart();
				remindAnnouncerToAnnounce();
				setState(State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChange(curAthlete);
				setState(State.CURRENT_ATHLETE_DISPLAYED);
			} else if (e instanceof FOPEvent.ForceTime) {
				// need to set time
				getTimer().setTimeRemaining(((FOPEvent.ForceTime) e).timeAllowed);
				setState(State.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, State.CURRENT_ATHLETE_DISPLAYED);
			}
			break;

		case ANNOUNCER_WAITING_FOR_TIMEKEEPER:
			if (e instanceof FOPEvent.TimeStartedManually) {
				getTimer().start();
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChange(curAthlete);
				setState(State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
			} else {
				unexpectedEventInState(e, State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
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
				setState(State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChange(curAthlete);
				setState(State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			} else {
				unexpectedEventInState(e, State.TIMEKEEPER_WAITING_FOR_ANNOUNCER);
			}
			break;

		case TIME_RUNNING:
			if (e instanceof FOPEvent.DownSignal) {
				// 2 referees have given same decision
				getTimer().stop();
				uiShowDownSignalOnSlaveDisplays((DownSignal) e);
				getTimer().stop();
				setState(State.DOWN_SIGNAL_VISIBLE);
			} else if (e instanceof FOPEvent.TimeStoppedManually) {
				// athlete lifted the bar
				getTimer().stop();
				setState(State.TIME_STOPPED);
			} else if (e instanceof FOPEvent.WeightChange) {
				WeightChange wc = (FOPEvent.WeightChange)e;
				Athlete athlete = wc.getAthlete();
				if (athlete == curAthlete) {
					// coach is requesting change, stop clock
					getTimer().stop();
					weightChange(curAthlete);
					setState(State.CURRENT_ATHLETE_DISPLAYED);
				} else {
					// other athlete is changing, leave clock running
					weightChangeDoNotDisturb(curAthlete);
					setState(State.TIME_RUNNING);
				}
			} else if (e instanceof FOPEvent.TimeOver) {
				// timer got down to 0
				// getTimer() signals this, nothing else required for timer
				// rule says referees must give reds
				setState(State.TIME_STOPPED);
			} else {
				unexpectedEventInState(e, State.TIME_RUNNING);
			}
			break;

		case TIME_STOPPED:
			if (e instanceof FOPEvent.TimeStartedManually) {
				// timekeeper mistake, start time again
				getTimer().start();
				transitionToTimeRunning();
			} else if (e instanceof FOPEvent.WeightChange) {
				WeightChange wc = (FOPEvent.WeightChange)e;
				Athlete athlete = wc.getAthlete();
				if (athlete.equals(curAthlete)) {
					// clock is already stopped, coach requesting change
					weightChange(curAthlete);
					setState(State.CURRENT_ATHLETE_DISPLAYED);
				} else {
					// other athlete is changing, leave clock alone
					weightChangeDoNotDisturb(curAthlete);
					setState(State.TIME_STOPPED);
				}
			} else if (e instanceof FOPEvent.RefereeDecision) {
				getTimer().stop();
				this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
				this.setClockOwner(null);
				decision(e);
			} else if (e instanceof FOPEvent.ForceTime) {
				// need to set time
				getTimer().setTimeRemaining(((FOPEvent.ForceTime) e).timeAllowed);
				setState(State.CURRENT_ATHLETE_DISPLAYED);
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
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChangeDoNotDisturb(curAthlete);
				setState(State.DOWN_SIGNAL_VISIBLE);
			} else {
				unexpectedEventInState(e, State.DOWN_SIGNAL_VISIBLE);
			}
			break;

		case DECISION_VISIBLE:
			if (e instanceof FOPEvent.RefereeDecision) {
				// decision reversal
				decision(e);
			} else if (e instanceof FOPEvent.WeightChange) {
				weightChangeDoNotDisturb(curAthlete);
				setState(State.DECISION_VISIBLE);
			} else if (e instanceof FOPEvent.DecisionReset) {
				uiEventBus.post(new UIEvent.DecisionReset(e.originatingUI));
				clockOwner = null;
				recomputeLiftingOrder();
				uiDisplayCurrentAthleteAndTime();
				setState(State.CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, State.DECISION_VISIBLE);
			}
			break;
		}
	}

	public void init(List<Athlete> athletes, ICountdownTimer timer) {
		this.timer = timer;
		this.eventBus = getEventBus();
		this.eventBus.register(this);
		this.curAthlete = null;
		this.setClockOwner(null);
		this.previousAthlete = null;
		this.liftingOrder = athletes;
		if (athletes != null && athletes.size() > 0) {
			recomputeLiftingOrder();
		}
		this.setState(State.INTERMISSION);
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
	public void switchGroup(Group group) {
		this.group = group;
		logger.info("{} switching to group {}", this.getName(), (group != null ? group.getName() : group));
		if (group != null) {
			List<Athlete> findAllByGroupAndWeighIn = AthleteRepository.findAllByGroupAndWeighIn(group, true);
			init(findAllByGroupAndWeighIn, timer);
			getEventBus().post(new FOPEvent.IntermissionDone(null));
		} else {
			init(ImmutableList.of(), timer);
		}

	}

	private void announce() {
		if (this.startTimeAutomatically) {
			getTimer().start();
			// time is already running
			transitionToTimeRunning();
		} else {
			remindTimekeeperToStartTime();
			setState(State.ANNOUNCER_WAITING_FOR_TIMEKEEPER);
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

		setState(State.DECISION_VISIBLE);
	}

	private Athlete getClockOwner() {
		return clockOwner;
	}

	private void recomputeLiftingOrder() {
		AthleteSorter.liftingOrder(this.liftingOrder);
		setDisplayOrder(AthleteSorter.displayOrderCopy(this.liftingOrder));
		this.setCurAthlete(this.liftingOrder.isEmpty() ? null : this.liftingOrder.get(0));
		getTimer().setTimeRemaining(getTimeAllowed());
		logger.info("recomputed lifting order curAthlete={} prevlifter={}", curAthlete, previousAthlete);
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

	private void transitionToIntermission() {
		recomputeLiftingOrder();
		uiDisplayCurrentWeight();
		setState(State.INTERMISSION);
	}

	private void transitionToTimeRunning() {
		setClockOwner(getCurAthlete());
		// enable master to listening for decision
		unlockReferees();
		setState(State.TIME_RUNNING);
	}

	private void uiDisplayCurrentAthleteAndTime() {
		Integer clock = getTimer().getTimeRemaining();
		Integer nextAttemptRequestedWeight = 0;
		if (curAthlete != null) {
			nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
		}
		Athlete nextAthlete = liftingOrder.size() > 0 ? liftingOrder.get(1) : null;

		uiEventBus.post(new UIEvent.LiftingOrderUpdated(curAthlete, nextAthlete, previousAthlete, liftingOrder, displayOrder, clock, UI.getCurrent()));
	
		logger.info("current athlete = {} attempt {}, requested = {}, timeAllowed={}",
			curAthlete,
			curAthlete != null ? curAthlete.getAttemptedLifts() + 1 : 0,
			nextAttemptRequestedWeight,
			clock);
	}

	private void uiDisplayCurrentWeight() {
		Integer nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
		uiEventLogger.info("requested weight: {} (from curAthlete {})",
			nextAttemptRequestedWeight,
			getCurAthlete());
	}

	private void uiShowDownSignalOnSlaveDisplays(FOPEvent.DownSignal e) {
		uiEventLogger.debug("showDownSignalOnSlaveDisplays");
		uiEventBus.post(new UIEvent.DownSignal(e.originatingUI));
	}

	private void uiShowRefereeDecisionOnSlaveDisplays(FOPEvent.RefereeDecision e) {
		uiEventLogger.debug("showRefereeDecisionOnSlaveDisplays");
		uiEventBus.post(new UIEvent.RefereeDecision(e.success,e.ref1,e.ref2,e.ref3,e.originatingUI));
	}

	private void unexpectedEventInState(FOPEvent e, State state) {
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
	 * Lifting order is recomputed, so the app.owlcms.ui.displays can get it, but not the attempt
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
	void setState(State state) {
		logger.debug("entering {}", state);
		this.state = state;
	}

}
