/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import static app.owlcms.fieldofplay.FOPState.BREAK;
import static app.owlcms.fieldofplay.FOPState.CURRENT_ATHLETE_DISPLAYED;
import static app.owlcms.fieldofplay.FOPState.DECISION_VISIBLE;
import static app.owlcms.fieldofplay.FOPState.DOWN_SIGNAL_VISIBLE;
import static app.owlcms.fieldofplay.FOPState.INACTIVE;
import static app.owlcms.fieldofplay.FOPState.TIME_RUNNING;
import static app.owlcms.fieldofplay.FOPState.TIME_STOPPED;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.FOPEvent.BreakPaused;
import app.owlcms.fieldofplay.FOPEvent.BreakStarted;
import app.owlcms.fieldofplay.FOPEvent.DecisionReset;
import app.owlcms.fieldofplay.FOPEvent.DownSignal;
import app.owlcms.fieldofplay.FOPEvent.ForceTime;
import app.owlcms.fieldofplay.FOPEvent.Decision;
import app.owlcms.fieldofplay.FOPEvent.RefereeUpdate;
import app.owlcms.fieldofplay.FOPEvent.StartLifting;
import app.owlcms.fieldofplay.FOPEvent.TimeOver;
import app.owlcms.fieldofplay.FOPEvent.TimeStarted;
import app.owlcms.fieldofplay.FOPEvent.TimeStopped;
import app.owlcms.fieldofplay.FOPEvent.WeightChange;
import app.owlcms.i18n.TranslationProvider;
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
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName()); //$NON-NLS-1$
	{
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.INFO);
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
	private FOPState state;
	private IProxyTimer athleteTimer;
	private IProxyTimer breakTimer;
	private BreakType breakType;
	private List<Athlete> liftingOrder;
	
	private List<Athlete> displayOrder;
	private int curWeight;
	/**
	 * Instantiates a new field of play state. When using this constructor
	 * {@link #init(List, IProxyTimer)} must be used to provide the athletes and set the athleteTimer
	 *
	 * @param group     the group (to get details such as name, and to reload athletes)
	 * @param platform2 the platform (to get details such as name)
	 */
	public FieldOfPlay(Group group, Platform platform2) {
		this.name = platform2.getName();
		this.fopEventBus = new EventBus("FOP-"+name); //$NON-NLS-1$
		this.uiEventBus = new EventBus("UI-"+name); //$NON-NLS-1$
		this.athleteTimer = null;
		this.breakTimer = new ProxyBreakTimer(this);
		this.platform = platform2;
	}

	/**
	 * Instantiates a new field of play state.
	 * This constructor is only used for testing using mock timers.
	 *
	 * @param athletes the athletes
	 * @param timer1    the athleteTimer
	 */
	public FieldOfPlay(List<Athlete> athletes, IProxyTimer timer1, IProxyTimer breakTimer1) {
		this.name = "test"; //$NON-NLS-1$
		this.fopEventBus = new EventBus("FOP-"+this.name); //$NON-NLS-1$
		this.uiEventBus = new EventBus("UI-"+this.name); //$NON-NLS-1$
		init(athletes, timer1, breakTimer1);
	}
	
	/**
	 * @return how many lifts done so far in the group.
	 */
	public int countLiftsDone() {
		int liftsDone = AthleteSorter.countLiftsDone(displayOrder);
		return liftsDone;
	}

	/**
	 * @return the server-side athleteTimer that tracks the time used
	 */
	public IProxyTimer getAthleteTimer() {
		return this.athleteTimer;
	}

	public IProxyTimer getBreakTimer() {
//		if (!(this.breakTimer.getClass().isAssignableFrom(ProxyBreakTimer.class))) throw new RuntimeException("wrong athleteTimer setup");
		return this.breakTimer;
	}

	public BreakType getBreakType() {
		return breakType;
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
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
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
		
		Athlete owner = getClockOwner();
		if (owner != null && owner.equals(a)) {
			// the clock was started for us. we own the clock, clock is set to what time was left
			timeAllowed = getAthleteTimer().getTimeRemainingAtLastStop();
			logger.trace("timeAllowed = timeRemaining = {}, clock owner = {}", timeAllowed, a); //$NON-NLS-1$
		} else if (previousAthlete != null && previousAthlete.equals(a)) { 
			if (owner != null || a.getAttemptNumber() == 1) {
				// clock has started for someone else, one minute
				// first C&J, one minute (doesn't matter who lifted last during snatch)
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
	 * @return the bus on which we post commands for the listening browser pages.
	 */
	public EventBus getUiEventBus() {
		return uiEventBus;
	}

	/**
	 * Handle field of play events.
	 * 
	 * FOP (Field of Play) events inform us of what is happening (e.g. athleteTimer
	 * started by timekeeper, decision given by official, etc.) The current state
	 * determines what we do with the event. Typically, we update the state of the
	 * field of play (e.g. time is now running) and we issue commands to the
	 * listening user interfaces (e.g. start or stop time being displayed, show the
	 * decision, etc.)
	 * 
	 * There is a fopEventBus for each active field of play. A given user interface
	 * will issue a FOP event on our fopEventBus, this method reacts to the event by
	 * updating state, and we issue the resulting user interface commands on the @link uiEventBus.
	 * 
	 * One exception is timers: the task to send UI events to start
	 * stop/start/manage timers is delegated to implementers of IProxyTimer; these
	 * classes remember the time and broadcast to all listening timers.
	 *
	 * @param e the event
	 */
	@Subscribe
	public void handleFOPEvent(FOPEvent e) {
		logger.debug("state {}, event received {}", this.getState(), e.getClass().getSimpleName()); //$NON-NLS-1$
		// it is always possible to explicitly interrupt competition (break between the two lifts, technical
		// incident, etc.)
		if (e instanceof BreakStarted) {
			transitionToBreak((BreakStarted) e);
			return;
		} else if (e instanceof StartLifting) {
			transitionToLifting(e, true);
		}

		switch (this.getState()) {
		
		case INACTIVE:
			if (e instanceof BreakStarted) {
				transitionToBreak((BreakStarted) e);
			} else if (e instanceof TimeStarted) {
				getAthleteTimer().start();
				transitionToTimeRunning();
			} else if (e instanceof WeightChange) {
				doWeightChangeDisturbIfNeeded((WeightChange) e);
				// leave us in INACTIVE state
				setState(INACTIVE);
			} else {
				unexpectedEventInState(e, INACTIVE);
			}
			break;

		case BREAK:
			if (e instanceof StartLifting) {
				transitionToLifting(e, true);
			} else if (e instanceof BreakPaused) {
				getBreakTimer().stop();
				getUiEventBus().post(new UIEvent.BreakPaused(e.getOrigin()));
			} else if (e instanceof BreakStarted) {
				transitionToBreak((BreakStarted) e);
			} else if (e instanceof WeightChange) {
				doWeightChangeDisturbIfNeeded((WeightChange) e);
//				if (curAthlete.getAttemptsDone() == 0) {
//					// the group has not started lifting, override the change to
//					// lifting state from weightChange and stay in BREAK mode
//					setState(BREAK);
//				}	
			} else {
				unexpectedEventInState(e, BREAK);
			}
			break;

		case CURRENT_ATHLETE_DISPLAYED:
			if (e instanceof TimeStarted) {
				getAthleteTimer().start();
				transitionToTimeRunning();		
			} else if (e instanceof WeightChange) {
				doWeightChangeDisturbIfNeeded((WeightChange) e);
				setState(CURRENT_ATHLETE_DISPLAYED);
			} else if (e instanceof ForceTime) {
				// need to set time
				getAthleteTimer().setTimeRemaining(((ForceTime) e).timeAllowed);
				setState(CURRENT_ATHLETE_DISPLAYED);
			} else if (e instanceof StartLifting) {
				// announcer can set break manually
				setState(CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, CURRENT_ATHLETE_DISPLAYED);
			}
			break;

		case TIME_RUNNING:
			if (e instanceof DownSignal) {
				// 2 referees have given same decision
				getAthleteTimer().stop();
				uiShowDownSignalOnSlaveDisplays((DownSignal) e);
				setState(DOWN_SIGNAL_VISIBLE);
			} else if (e instanceof TimeStopped) {
				// athlete lifted the bar
				getAthleteTimer().stop();
				setState(TIME_STOPPED);
			} else if (e instanceof WeightChange) {
				doWeightChangeDisturbIfNeeded((WeightChange) e);
			} else if (e instanceof Decision) {
				// in theory not possible, we would get down signal before.
				getAthleteTimer().stop();
				this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
				this.setClockOwner(null);
				decision(e);
			} else if (e instanceof RefereeUpdate) {
				// a referee entered a decision
				// for now, this is only used for jury (in the future, may accomodate multiple
				// devices such as phones.
				uiShowUpdateOnJuryScreen((RefereeUpdate) e);
			} else if (e instanceof TimeOver) {
				// athleteTimer got down to 0
				// getTimer() signals this, nothing else required for athleteTimer
				// rule says referees must give reds
				setState(TIME_STOPPED);
			} else {
				unexpectedEventInState(e, TIME_RUNNING);
			}
			break;

		case TIME_STOPPED:
			if (e instanceof DownSignal) {
				// 2 referees have given same decision
				getAthleteTimer().stop(); // paranoia
				uiShowDownSignalOnSlaveDisplays((DownSignal) e);
				setState(DOWN_SIGNAL_VISIBLE);
			} else if (e instanceof TimeStarted) {
				getAthleteTimer().start();
				transitionToTimeRunning();
			} else if (e instanceof WeightChange) {
				doWeightChangeDisturbIfNeeded((WeightChange) e);
			} else if (e instanceof Decision) {
				getAthleteTimer().stop();
				this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
				this.setClockOwner(null);
				decision(e);
			} else if (e instanceof RefereeUpdate) {
				// a referee entered a decision
				// for now, this is only used for jury (in the future, may accomodate multiple
				// devices such as phones.
				uiShowUpdateOnJuryScreen((RefereeUpdate) e);
			} else if (e instanceof ForceTime) {
				// need to set time
				getAthleteTimer().setTimeRemaining(((ForceTime) e).timeAllowed);
				setState(CURRENT_ATHLETE_DISPLAYED);
			} else {
				unexpectedEventInState(e, TIME_STOPPED);
			}
			break;

		case DOWN_SIGNAL_VISIBLE:
			this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
			this.setClockOwner(null);
			if (e instanceof Decision) {
				getAthleteTimer().stop();
				decision(e);
			} else if (e instanceof RefereeUpdate) {
				// a referee entered a decision.
				uiShowUpdateOnJuryScreen((RefereeUpdate) e);
			} else if (e instanceof WeightChange) {
				weightChangeDoNotDisturb((WeightChange) e);
				setState(DOWN_SIGNAL_VISIBLE);
			} else {
				unexpectedEventInState(e, DOWN_SIGNAL_VISIBLE);
			}
			break;

		case DECISION_VISIBLE:
			if (e instanceof Decision) {
				// decision reversal
				decision(e);
			} else if (e instanceof RefereeUpdate) {
				// a referee entered a decision
				uiShowUpdateOnJuryScreen((RefereeUpdate) e);
			} else if (e instanceof WeightChange) {
				weightChangeDoNotDisturb((WeightChange) e);
				setState(DECISION_VISIBLE);
			} else if (e instanceof DecisionReset) {
				uiEventBus.post(new UIEvent.DecisionReset(e.origin));
				setClockOwner(null);
//				recomputeLiftingOrder(); // now done as part of decision(e)
				displayOrBreak(e);
			} else {
				unexpectedEventInState(e, DECISION_VISIBLE);
			}
			break;
		}
	}

	public void init(List<Athlete> athletes, IProxyTimer timer, IProxyTimer breakTimer) {
		this.athleteTimer = timer;
		this.breakTimer = breakTimer;
		this.fopEventBus = getFopEventBus();
		this.fopEventBus.register(this);
		this.curAthlete = null;
		this.setClockOwner(null);
		this.previousAthlete = null;
		this.liftingOrder = athletes;
		if (athletes != null && athletes.size() > 0) {
			recomputeLiftingOrder();
		}
		if (state == null) {
			this.setState(INACTIVE);
		}
	}

	public void initGroup(Group group, Object origin) {
		this.group = group;
		if (group != null) {
			logger.debug("{} loading data for group {} [{}]", this.getName(), (group != null ? group.getName() : group), LoggerUtils.whereFrom()); //$NON-NLS-1$
			List<Athlete> findAllByGroupAndWeighIn = AthleteRepository.findAllByGroupAndWeighIn(group, true);
			init(findAllByGroupAndWeighIn, athleteTimer, breakTimer);
		} else {
			init(new ArrayList<Athlete>(), athleteTimer, breakTimer);
		}
	}

	/**
	 * Sets the athleteTimer.
	 *
	 * @param athleteTimer the new athleteTimer
	 */
	public void setAthleteTimer(IProxyTimer timer) {
		this.athleteTimer = timer;
	}

	public void setBreakType(BreakType breakType) {
		this.breakType = breakType;
	}

	public void setDisplayOrder(List<Athlete> displayOrder) {
		this.displayOrder = displayOrder;
		AthleteSorter.assignCategoryRanks(displayOrder);
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
	 * Switch group.
	 *
	 * @param group the group
	 */
	public void switchGroup(Group group, Object origin) {
		logger.trace("switchGroup {}",LoggerUtils.stackTrace()); //$NON-NLS-1$
		initGroup(group, origin);
		logger.trace("{} start lifting for group {} origin={}", this.getName(), (group != null ? group.getName() : group),origin); //$NON-NLS-1$
		getFopEventBus().post(new StartLifting(origin));
	}


	/**
	 * Sets the state.
	 *
	 * @param state the new state
	 */
	void setState(FOPState state) {
		logger.trace("entering {} {}", state, LoggerUtils.whereFrom()); //$NON-NLS-1$
		this.state = state;
	}

	private void decision(FOPEvent e) {
		Decision decision = (Decision) e;
		if (decision.success) {
			curAthlete.successfulLift();
		} else {
			curAthlete.failedLift();
		}
		AthleteRepository.save(curAthlete);
		recomputeLiftingOrder();
		uiShowRefereeDecisionOnSlaveDisplays(decision);
		setState(DECISION_VISIBLE);
	}

	private void displayOrBreak(FOPEvent e) {
		if (curAthlete != null && curAthlete.getAttemptsDone() < 6) {
			uiDisplayCurrentAthleteAndTime(true, e);
			setState(CURRENT_ATHLETE_DISPLAYED);
		} else {
			UIEvent.GroupDone event = new UIEvent.GroupDone(this.getGroup(), null);
			uiEventBus.post(event);
			setBreakType(BreakType.GROUP_DONE);
			setState(BREAK);
		}
	}

	private void doWeightChangeDisturbIfNeeded(WeightChange wc) {
		Athlete changingAthlete = wc.getAthlete();
		Integer newWeight = changingAthlete.getNextAttemptRequestedWeight();
		logger.trace("&&1 cur={} curWeight={} new={} newWeight={}", curAthlete, curWeight, changingAthlete, newWeight); //$NON-NLS-1$
		logger.trace("&&2 clockOwner={} clockLastStopped={} state={}", clockOwner, getAthleteTimer().getTimeRemainingAtLastStop(), state); //$NON-NLS-1$
		
		
		boolean stopAthleteTimer = false;
		if (clockOwner != null) {
			// time has started
			if (changingAthlete.equals(clockOwner)) {
				logger.trace("&&3.A clock IS running for changing athlete"); //$NON-NLS-1$
				// X is the current lifter
				// if a real change (and not simply a declaration that does not change weight), make sure clock is stopped.
				if (curWeight != newWeight) {
					logger.trace("&&3.A.A weight change for clock owner: stop clock"); //$NON-NLS-1$
					getAthleteTimer().stop(); // memorize time
					stopAthleteTimer = true; // make sure we broacast to clients
					logger.trace("&&4.1 stop, recompute, state"); //$NON-NLS-1$
					recomputeLiftingOrder();
					// set the state now, otherwise attempt board will ignore request to display if in a break
					setState(CURRENT_ATHLETE_DISPLAYED);
					// if in a break, we don't stop break timer on a weight change.
					// unless we are at the end of a group (a loading error may have occurred)
					boolean stopBreakTimer = (state == BREAK && getBreakType() == BreakType.GROUP_DONE);
					if (stopBreakTimer) {
						getBreakTimer().stop();
					}
					uiDisplayCurrentAthleteAndTime(stopAthleteTimer, wc);
				} else {
					logger.trace("&&3.A.B declaration for clock owner: leave clock running"); //$NON-NLS-1$
					// no weight change.  this is most likely a declaration.
					//TODO: post uiEvent to signal declaration
					if (Athlete.zeroIfInvalid(changingAthlete.getCurrentDeclaration()) == newWeight) {
						Notification.show(MessageFormat.format(TranslationProvider.translate("FieldOfPlay.0"), changingAthlete, newWeight),5000,Position.TOP_START); //$NON-NLS-1$
					}
					return;
				}
			} else {
				logger.trace("&&3.B clock running, but NOT for changing athlete"); //$NON-NLS-1$
				weightChangeDoNotDisturb(wc);
				return;
			}
		} else {
			logger.trace("&&4 recompute + NOT changing state"); //$NON-NLS-1$
			// time is not running
			// changing athlete is not current athlete
			recomputeLiftingOrder();
			uiDisplayCurrentAthleteAndTime(true, wc);
		}
	}

	private Athlete getClockOwner() {
		return clockOwner;
	}

	private synchronized void recomputeLiftingOrder() {
		AthleteSorter.liftingOrder(this.liftingOrder);
		setDisplayOrder(AthleteSorter.displayOrderCopy(this.liftingOrder));
		this.setCurAthlete(this.liftingOrder.isEmpty() ? null : this.liftingOrder.get(0));
		int timeAllowed = getTimeAllowed();
		logger.debug("recomputed lifting order curAthlete={} prevlifter={} time={} [{}]", //$NON-NLS-1$
			curAthlete != null ? curAthlete.getFullName() : "", //$NON-NLS-1$
			previousAthlete != null ? previousAthlete.getFullName() : "", timeAllowed, LoggerUtils.whereFrom()); //$NON-NLS-1$

		getAthleteTimer().setTimeRemaining(timeAllowed);
	}

	private void setClockOwner(Athlete athlete) {
		logger.trace("***setting clock owner to {} [{}]", athlete, LoggerUtils.whereFrom()); //$NON-NLS-1$
		this.clockOwner = athlete;
	}

	private void setCurAthlete(Athlete athlete) {
		logger.trace("changing curAthlete to {} [{}]", athlete, LoggerUtils.whereFrom()); //$NON-NLS-1$
		this.curAthlete = athlete;
	}

	private void setPreviousAthlete(Athlete athlete) {
		logger.trace("setting previousAthlete to {}", curAthlete); //$NON-NLS-1$
		this.previousAthlete = athlete;
	}

	private void transitionToBreak(BreakStarted e) {
		this.setBreakType(e.getBreakType());
		getBreakTimer().start();
		setState(BREAK);
	}

	private void transitionToLifting(FOPEvent e, boolean stopBreakTimer) {
		logger.trace("transitionToLifting {} {} {}",e.getAthlete(), stopBreakTimer, LoggerUtils.whereFrom()); //$NON-NLS-1$
		recomputeLiftingOrder();
		// set the state now, otherwise attempt board will ignore request to display
		setState(CURRENT_ATHLETE_DISPLAYED);
		if (stopBreakTimer) {
			getBreakTimer().stop();
		}
		uiDisplayCurrentAthleteAndTime(true, e);
	}

	private void transitionToTimeRunning() {
		setClockOwner(getCurAthlete());
		// enable master to listening for decision
		unlockReferees();
		setState(TIME_RUNNING);
	}

	private void uiDisplayCurrentAthleteAndTime(boolean stopTimer, FOPEvent e) {
		Integer clock = getAthleteTimer().getTimeRemaining();

		curWeight = 0;
		if (curAthlete != null) {
			curWeight = curAthlete.getNextAttemptRequestedWeight();
		}
		// if only one athlete, no next athlete
		Athlete nextAthlete = liftingOrder.size() > 1 ? liftingOrder.get(1) : null;

		Athlete changingAthlete = null ;
		if (e instanceof WeightChange) changingAthlete= e.getAthlete();
		uiEventBus.post(new UIEvent.LiftingOrderUpdated(curAthlete, nextAthlete, previousAthlete, changingAthlete, liftingOrder, getDisplayOrder(), clock, stopTimer, e.getOrigin()));
	
		logger.info("current athlete = {} attempt {}, requested = {}, timeAllowed={} timeRemainingAtLastStop={}", //$NON-NLS-1$
			curAthlete,
			curAthlete != null ? curAthlete.getAttemptedLifts() + 1 : 0,
			curWeight,
			clock,
			getAthleteTimer().getTimeRemainingAtLastStop());
	}

	@SuppressWarnings("unused")
	private void uiDisplayCurrentWeight() {
		Integer nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
		uiEventLogger.info("requested weight: {} (from curAthlete {})", //$NON-NLS-1$
			nextAttemptRequestedWeight,
			getCurAthlete());
	}

	private void uiShowDownSignalOnSlaveDisplays(DownSignal e) {
		uiEventLogger.trace("showDownSignalOnSlaveDisplays"); //$NON-NLS-1$
		uiEventBus.post(new UIEvent.DownSignal(e.origin));
	}

	private void uiShowRefereeDecisionOnSlaveDisplays(Decision e) {
		uiEventLogger.trace("showRefereeDecisionOnSlaveDisplays"); //$NON-NLS-1$
		uiEventBus.post(new UIEvent.Decision(e.getAthlete(), e.success,e.ref1,e.ref2,e.ref3,e.origin));
	}

	private void uiShowUpdateOnJuryScreen(RefereeUpdate e) {
		uiEventLogger.trace("uiShowUpdateOnJuryScreen"); //$NON-NLS-1$
		uiEventBus.post(new UIEvent.RefereeUpdate(e.getAthlete(),e.ref1,e.ref2,e.ref3,e.ref1Time,e.ref2Time,e.ref3Time,e.origin));
	}

	private void unexpectedEventInState(FOPEvent e, FOPState state) {
		// events not worth signaling
		if (e instanceof DecisionReset || e instanceof RefereeUpdate) {
			// ignore
			return;
		}
		String text = MessageFormat.format(TranslationProvider.translate("FieldOfPlay.1"),e.getClass().getSimpleName(),state); //$NON-NLS-1$
		logger.warn(TranslationProvider.translate("FieldOfPlay.2"),e.getClass().getSimpleName(),state); //$NON-NLS-1$
		Notification.show(text,5000,Position.BOTTOM_END);
	}

	private void unlockReferees() {
		//FUTURE: when we implement phones as ref devices.
		uiEventLogger.trace("unlockReferees"); //$NON-NLS-1$
	}

	/**
	 * weight change while a lift is being performed (bar lifted above knees)
	 * Lifting order is recomputed, so the app.owlcms.ui.displayselection can get it, but not the attempt
	 * board state.
	 * @param e 
	 * @param curAthlete
	 */
	private void weightChangeDoNotDisturb(WeightChange e) {
		AthleteSorter.liftingOrder(this.liftingOrder);
		this.setDisplayOrder(AthleteSorter.displayOrderCopy(this.liftingOrder));
		uiDisplayCurrentAthleteAndTime(false, e);
	}

}
