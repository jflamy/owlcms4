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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.Mixer;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.FOPEvent.BarbellOrPlatesChanged;
import app.owlcms.fieldofplay.FOPEvent.BreakPaused;
import app.owlcms.fieldofplay.FOPEvent.BreakStarted;
import app.owlcms.fieldofplay.FOPEvent.DecisionFullUpdate;
import app.owlcms.fieldofplay.FOPEvent.DecisionReset;
import app.owlcms.fieldofplay.FOPEvent.DecisionUpdate;
import app.owlcms.fieldofplay.FOPEvent.DownSignal;
import app.owlcms.fieldofplay.FOPEvent.ExplicitDecision;
import app.owlcms.fieldofplay.FOPEvent.ForceTime;
import app.owlcms.fieldofplay.FOPEvent.StartLifting;
import app.owlcms.fieldofplay.FOPEvent.SwitchGroup;
import app.owlcms.fieldofplay.FOPEvent.TimeOver;
import app.owlcms.fieldofplay.FOPEvent.TimeStarted;
import app.owlcms.fieldofplay.FOPEvent.TimeStopped;
import app.owlcms.fieldofplay.FOPEvent.WeightChange;
import app.owlcms.i18n.Translator;
import app.owlcms.sound.Sound;
import app.owlcms.sound.Tone;
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

    private static final int REVERSAL_DELAY = 3000;

    private static final long DECISION_VISIBLE_DURATION = 3500;

    private class DelayTimer {
        private final Timer t = new Timer();

        public TimerTask schedule(final Runnable r, long delay) {
            if (isTestingMode()) {
                r.run();
                return null;
            } else {
                final TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        r.run();
                    }
                };
                t.schedule(task, delay);
                return task;
            }
        }
    }

    final private Logger logger = (Logger) LoggerFactory.getLogger(FieldOfPlay.class);

    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    {
        logger.setLevel(Level./**/DEBUG);
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
    private Tone downSignal;
    private boolean initialWarningEmitted;
    private boolean finalWarningEmitted;
    private boolean timeoutEmitted;
    private boolean downEmitted;
    private Boolean[] refereeDecision;
    private boolean decisionDisplayScheduled = false;

    private Integer[] refereeTime;
    private Boolean goodLift;

    private boolean testingMode;

    /**
     * Instantiates a new field of play state. When using this constructor
     * {@link #init(List, IProxyTimer)} must later be used to provide the athletes
     * and set the athleteTimer
     *
     * @param group     the group (to get details such as name, and to reload
     *                  athletes)
     * @param platform2 the platform (to get details such as name)
     */
    public FieldOfPlay(Group group, Platform platform2) {
        this.name = platform2.getName();
        this.fopEventBus = new EventBus("FOP-" + name);
        this.uiEventBus = new EventBus("UI-" + name);
        this.athleteTimer = null;
        this.breakTimer = new ProxyBreakTimer(this);
        this.setPlatform(platform2);
    }

    /**
     * Instantiates a new field of play state. This constructor is only used for
     * testing using mock timers.
     *
     * @param athletes the athletes
     * @param timer1   the athleteTimer
     */
    public FieldOfPlay(List<Athlete> athletes, IProxyTimer timer1, IProxyTimer breakTimer1, boolean testingMode) {
        this.name = "test";
        this.fopEventBus = new EventBus("FOP-" + this.name);
        this.uiEventBus = new EventBus("UI-" + this.name);
        this.setTestingMode(testingMode);
        init(athletes, timer1, breakTimer1);
    }

    /**
     * @return how many lifts done so far in the group.
     */
    public int countLiftsDone() {
        int liftsDone = AthleteSorter.countLiftsDone(displayOrder);
        return liftsDone;
    }

    public void emitDown(FOPEvent e) {
        getAthleteTimer().stop(); // paranoia
        uiShowDownSignalOnSlaveDisplays(e.origin);
        setState(DOWN_SIGNAL_VISIBLE);
    }

    public void emitFinalWarning() {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean emitted2 = isFinalWarningEmitted();
        logger.debug("emitFinalWarning server={} emitted={}", emitSoundsOnServer2, emitted2); // $NON-NLS-1

        if (emitSoundsOnServer2 && !emitted2) {
            // instead of finalWarning2.wav sounds too much like down
            new Sound(getSoundMixer(), "initialWarning2.wav").emit();
            setFinalWarningEmitted(true);
        }
    }

    public void emitInitialWarning() {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean emitted2 = isInitialWarningEmitted();
        logger.debug("emitInitialWarning server={} emitted={}", emitSoundsOnServer2, emitted2); // $NON-NLS-1

        if (emitSoundsOnServer2 && !emitted2) {
            new Sound(getSoundMixer(), "initialWarning2.wav").emit();
            setInitialWarningEmitted(true);
        }
    }

    public void emitTimeOver() {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean emitted2 = isTimeoutEmitted();
        logger.debug("emitTimeout server={} emitted={}", emitSoundsOnServer2, emitted2); // $NON-NLS-1

        if (emitSoundsOnServer2 && !emitted2) {
            new Sound(getSoundMixer(), "timeOver2.wav").emit();
            setTimeoutEmitted(true);
        }
    }

    /**
     * @return the server-side athleteTimer that tracks the time used
     */
    public IProxyTimer getAthleteTimer() {
        return this.athleteTimer;
    }

    public ProxyBreakTimer getBreakTimer() {
        // if (!(this.breakTimer.getClass().isAssignableFrom(ProxyBreakTimer.class)))
        // throw new RuntimeException("wrong athleteTimer setup");
        return (ProxyBreakTimer) this.breakTimer;
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
            // the clock was started for us. we own the clock, clock is set to what time was
            // left
            timeAllowed = getAthleteTimer().getTimeRemainingAtLastStop();
            logger.trace("timeAllowed = timeRemaining = {}, clock owner = {}", timeAllowed, a);
        } else if (previousAthlete != null && previousAthlete.equals(a)) {
            resetDecisions();
            if (owner != null || a.getAttemptNumber() == 1) {
                // clock has started for someone else, one minute
                // first C&J, one minute (doesn't matter who lifted last during snatch)
                timeAllowed = 60000;
            } else {
                timeAllowed = 120000;
            }
        } else {
            resetDecisions();
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
     * updating state, and we issue the resulting user interface commands on
     * the @link uiEventBus.
     *
     * One exception is timers: the task to send UI events to start
     * stop/start/manage timers is delegated to implementers of IProxyTimer; these
     * classes remember the time and broadcast to all listening timers.
     *
     * @param e the event
     */
    @Subscribe
    public void handleFOPEvent(FOPEvent e) {
        logger.debug("state {}, event received {}", this.getState(), e.getClass().getSimpleName());
        // it is always possible to explicitly interrupt competition (break between the
        // two lifts, technical incident, etc.)
        if (e instanceof BreakStarted) {
            transitionToBreak((BreakStarted) e);
            return;
        } else if (e instanceof StartLifting) {
            transitionToLifting(e, true);
        } else if (e instanceof BarbellOrPlatesChanged) {
            uiShowPlates((BarbellOrPlatesChanged) e);
            return;
        } else if (e instanceof SwitchGroup) {
            if (state != BREAK && state != INACTIVE) {
                setState(INACTIVE);
                athleteTimer.stop();
            }
            loadGroup(((SwitchGroup) e).getGroup(), this);
            recomputeLiftingOrder();
            getUiEventBus().post(new UIEvent.SwitchGroup(((SwitchGroup) e).getGroup(), e.getOrigin()));
            return;
        }

        switch (this.getState()) {

        case INACTIVE:
            if (e instanceof BreakStarted) {
                transitionToBreak((BreakStarted) e);
            } else if (e instanceof TimeStarted) {
                getAthleteTimer().start();
                transitionToTimeRunning();
            } else if (e instanceof WeightChange) {
                doWeightChange((WeightChange) e);
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
                doWeightChange((WeightChange) e);
            } else {
                unexpectedEventInState(e, BREAK);
            }
            break;

        case CURRENT_ATHLETE_DISPLAYED:
            if (e instanceof TimeStarted) {
                getAthleteTimer().start();
                transitionToTimeRunning();
            } else if (e instanceof WeightChange) {
                doWeightChange((WeightChange) e);
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
                emitDown(e);
            } else if (e instanceof TimeStopped) {
                // athlete lifted the bar
                getAthleteTimer().stop();
                setState(TIME_STOPPED);
            } else if (e instanceof DecisionFullUpdate) {
                // decision board/attempt board sends bulk update
                updateRefereeDecisions((DecisionFullUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof DecisionUpdate) {
                updateRefereeDecisions((DecisionUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof WeightChange) {
                doWeightChange((WeightChange) e);
            } else if (e instanceof ExplicitDecision) {
                getAthleteTimer().stop();
                this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
                this.setClockOwner(null);
                showExplicitDecision((ExplicitDecision) e, e.origin);
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
                emitDown(e);
            } else if (e instanceof DecisionFullUpdate) {
                // decision coming from decision display or attempt board
                updateRefereeDecisions((DecisionFullUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof DecisionUpdate) {
                updateRefereeDecisions((DecisionUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof TimeStarted) {
                getAthleteTimer().start();
                setClockOwner(getCurAthlete());
                prepareDownSignal();
                // we do not reset decisions or "emitted" flags
                setState(TIME_RUNNING);
            } else if (e instanceof WeightChange) {
                doWeightChange((WeightChange) e);
            } else if (e instanceof ExplicitDecision) {
                getAthleteTimer().stop();
                this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
                this.setClockOwner(null);
                showExplicitDecision(((ExplicitDecision) e), e.origin);
            } else if (e instanceof ForceTime) {
                getAthleteTimer().setTimeRemaining(((ForceTime) e).timeAllowed);
                setState(CURRENT_ATHLETE_DISPLAYED);
            } else if (e instanceof TimeStopped) {
                // ignore duplicate time stopped
            } else if (e instanceof TimeOver) {
                // ignore, already dealt by timer
            } else {
                unexpectedEventInState(e, TIME_STOPPED);
            }
            break;

        case DOWN_SIGNAL_VISIBLE:
            this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
            this.setClockOwner(null);
            if (e instanceof ExplicitDecision) {
                getAthleteTimer().stop();
                showExplicitDecision(((ExplicitDecision) e), e.origin);
            } else if (e instanceof DecisionFullUpdate) {
                // decision coming from decision display or attempt board
                updateRefereeDecisions((DecisionFullUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof DecisionUpdate) {
                updateRefereeDecisions((DecisionUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof WeightChange) {
                weightChangeDoNotDisturb((WeightChange) e);
                setState(DOWN_SIGNAL_VISIBLE);
            } else {
                unexpectedEventInState(e, DOWN_SIGNAL_VISIBLE);
            }
            break;

        case DECISION_VISIBLE:
            if (e instanceof ExplicitDecision) {
                showExplicitDecision(((ExplicitDecision) e), e.origin);
            } else if (e instanceof DecisionFullUpdate) {
                // decision coming from decision display or attempt board
                updateRefereeDecisions((DecisionFullUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof DecisionUpdate) {
                updateRefereeDecisions((DecisionUpdate) e);
                uiShowUpdateOnJuryScreen();
            } else if (e instanceof WeightChange) {
                weightChangeDoNotDisturb((WeightChange) e);
                setState(DECISION_VISIBLE);
            } else if (e instanceof DecisionReset) {
                logger.debug("resetting decisions");
                uiEventBus.post(new UIEvent.DecisionReset(e.origin));
                setClockOwner(null);
                displayOrBreakIfDone(e);
            } else {
                unexpectedEventInState(e, DECISION_VISIBLE);
            }
            break;
        }
    }

    public void init(List<Athlete> athletes, IProxyTimer timer, IProxyTimer breakTimer) {
        logger.trace("start of init state=" + state);
        this.athleteTimer = timer;
        this.breakTimer = breakTimer;
        this.fopEventBus = getFopEventBus();
        this.fopEventBus.register(this);
        this.curAthlete = null;
        this.setClockOwner(null);
        this.previousAthlete = null;
        this.liftingOrder = athletes;
        if (athletes != null && athletes.size() > 0) {
            // we skip recomputation so that we can detect that we are on an empty group
            // and clear the displays. SwitchGroup processing will recompute.
            // recomputing twice is innocuous in this case and is simpler than keeping
            // state.
            recomputeLiftingOrder();
        }
        if (state == null) {
            this.setState(INACTIVE);
        }
        logger.trace("end of init state=" + state);
    }

    public boolean isEmitSoundsOnServer() {
        return getSoundMixer() != null;
    }

    public boolean isTestingMode() {
        return testingMode;
    }

    public void loadGroup(Group group, Object origin) {
        this.group = group;
        if (group != null) {
            logger.debug("{} loading data for group {} [{}]", this.getName(), (group != null ? group.getName() : group),
                    LoggerUtils.whereFrom());
            List<Athlete> findAllByGroupAndWeighIn = AthleteRepository.findAllByGroupAndWeighIn(group, true);
            init(findAllByGroupAndWeighIn, athleteTimer, breakTimer);
        } else {
            init(new ArrayList<Athlete>(), athleteTimer, breakTimer);
        }
    }

    public synchronized void recomputeLiftingOrder() {
        AthleteSorter.liftingOrder(this.liftingOrder);
        setDisplayOrder(AthleteSorter.displayOrderCopy(this.liftingOrder));
        this.setCurAthlete(this.liftingOrder.isEmpty() ? null : this.liftingOrder.get(0));
        int timeAllowed = getTimeAllowed();
        logger.debug("recomputed lifting order curAthlete={} prevlifter={} time={} [{}]",
                curAthlete != null ? curAthlete.getFullName() : "",
                        previousAthlete != null ? previousAthlete.getFullName() : "", timeAllowed, LoggerUtils.whereFrom());
        getAthleteTimer().setTimeRemaining(timeAllowed);
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
     * @param testingMode true if we don't want wait delays during testing.
     */
    public void setTestingMode(boolean testingMode) {
        this.testingMode = testingMode;
    }

    /**
     * Switch group.
     *
     * @param group the group
     */
    public void startLifting(Group group, Object origin) {
        logger.trace("startLifting {}", LoggerUtils.stackTrace());
        loadGroup(group, origin);
        logger.trace("{} start lifting for group {} origin={}", this.getName(),
                (group != null ? group.getName() : group), origin);
        getFopEventBus().post(new StartLifting(origin));
    }

    public void uiDisplayCurrentAthleteAndTime(boolean stopTimer, FOPEvent e, boolean displayToggle) {
        Integer clock = getAthleteTimer().getTimeRemaining();

        curWeight = 0;
        if (curAthlete != null) {
            curWeight = curAthlete.getNextAttemptRequestedWeight();
        }
        // if only one athlete, no next athlete
        Athlete nextAthlete = liftingOrder.size() > 1 ? liftingOrder.get(1) : null;

        Athlete changingAthlete = null;
        if (e instanceof WeightChange) {
            changingAthlete = e.getAthlete();
        }
        boolean inBreak = false;
        if (state == FOPState.BREAK) {
            inBreak = ((breakTimer != null && breakTimer.isRunning()));
        }
        uiEventBus.post(new UIEvent.LiftingOrderUpdated(curAthlete, nextAthlete, previousAthlete, changingAthlete,
                liftingOrder, getDisplayOrder(), clock, stopTimer, displayToggle, e.getOrigin(), inBreak));

        logger.info("current athlete = {} attempt {}, requested = {}, timeAllowed={} timeRemainingAtLastStop={}",
                curAthlete, curAthlete != null ? curAthlete.getAttemptedLifts() + 1 : 0, curWeight, clock,
                        getAthleteTimer().getTimeRemainingAtLastStop());
    }

    public void updateGlobalRankings() {
        logger.trace("update rankings {}", LoggerUtils.whereFrom());
        Competition competition = Competition.getCurrent();
        if (competition.isGlobalRankingRecompute()) {
            competition.computeGlobalRankings();
            uiShowUpdatedRankings();
        }

    }

    /**
     * Sets the state.
     *
     * @param state the new state
     */
    void setState(FOPState state) {
        logger.trace("entering {} {}", state, LoggerUtils.whereFrom());
        // if (state == INACTIVE) {
        // logger.debug("entering inactive {}",LoggerUtils.stackTrace());
        // }
        this.state = state;
    }

    /**
     * Don't interrupt break if official-induced break. Interrupt break if it is
     * simply "group done".
     */
    private void changeStateUnlessInBreak() {
        if (state == BREAK) {
            logger.debug("{} {}", state, getBreakType());
            // if in a break, we don't stop break timer on a weight change.
            if (getBreakType() == BreakType.GROUP_DONE) {
                // weight change in state GROUP_DONE can happen if there is a loading error
                // and there is no jury deliberation break -- the weight change is entered
                // directly
                // in this case, we need to go back to lifting.
                // set the state now, otherwise attempt board will ignore request to display if
                // in a break
                setState(CURRENT_ATHLETE_DISPLAYED);
                getBreakTimer().stop();
            } else {
                // remain in break state
                setState(BREAK);
            }
        } else {
            setState(CURRENT_ATHLETE_DISPLAYED);
        }
    }

    private void displayOrBreakIfDone(FOPEvent e) {
        if (curAthlete != null && curAthlete.getAttemptsDone() < 6) {
            uiDisplayCurrentAthleteAndTime(true, e, false);
            setState(CURRENT_ATHLETE_DISPLAYED);
        } else {
            UIEvent.GroupDone event = new UIEvent.GroupDone(this.getGroup(), null);
            uiEventBus.post(event);
//            setBreakType(BreakType.GROUP_DONE);
//            setState(BREAK);
            setState(INACTIVE);
        }
    }

    private void doWeightChange(WeightChange wc) {
        Athlete changingAthlete = wc.getAthlete();
        Integer newWeight = changingAthlete.getNextAttemptRequestedWeight();
        logger.trace("&&1 cur={} curWeight={} new={} newWeight={}", curAthlete, curWeight, changingAthlete, newWeight);
        logger.trace("&&2 clockOwner={} clockLastStopped={} state={}", clockOwner,
                getAthleteTimer().getTimeRemainingAtLastStop(), state);

        boolean stopAthleteTimer = false;
        if (clockOwner != null) {
            // time has started
            if (changingAthlete.equals(clockOwner)) {
                logger.trace("&&3.A clock IS running for changing athlete");
                // X is the current lifter
                // if a real change (and not simply a declaration that does not change weight),
                // make sure clock is stopped.
                if (curWeight != newWeight) {
                    logger.trace("&&3.A.A weight change for clock owner: stop clock");
                    getAthleteTimer().stop(); // memorize time
                    stopAthleteTimer = true; // make sure we broacast to clients
                    logger.trace("&&4.1 stop, recompute, state");
                    recomputeLiftingOrder();
                    changeStateUnlessInBreak();
                    uiDisplayCurrentAthleteAndTime(stopAthleteTimer, wc, false);
                    updateGlobalRankings();
                } else {
                    logger.trace("&&3.A.B declaration for clock owner: leave clock running");
                    // no weight change. this is most likely a declaration.
                    if (Athlete.zeroIfInvalid(changingAthlete.getCurrentDeclaration()) == newWeight) {
                        Notification.show(Translator.translate("Declaration_Notification", changingAthlete, newWeight),
                                5000, Position.TOP_START);
                    }
                    return;
                }
            } else {
                logger.trace("&&3.B clock running, but NOT for changing athlete");
                weightChangeDoNotDisturb(wc);
                return;
            }
        } else {
            logger.trace("&&4 recompute + NOT changing state");
            // time is not running
            // changing athlete is not current athlete
            recomputeLiftingOrder();
            changeStateUnlessInBreak();
            uiDisplayCurrentAthleteAndTime(true, wc, false);
            updateGlobalRankings();
        }
    }

    private Athlete getClockOwner() {
        return clockOwner;
    }

    private Mixer getSoundMixer() {
        Platform platform2 = getPlatform();
        return platform2 == null ? null : platform2.getMixer();
    }

    private boolean isDecisionDisplayScheduled() {
        return decisionDisplayScheduled;
    }

    private synchronized boolean isDownEmitted() {
        return downEmitted;
    }

    private synchronized boolean isFinalWarningEmitted() {
        return finalWarningEmitted;
    }

    private synchronized boolean isInitialWarningEmitted() {
        return initialWarningEmitted;
    }

    private synchronized boolean isTimeoutEmitted() {
        return timeoutEmitted;
    }

    private void prepareDownSignal() {
        if (isEmitSoundsOnServer()) {
            downSignal = new Tone(getSoundMixer(), 1100, 1200, 1.0);
        }
    }

    /**
     * Compute events resulting from decisions received so far (down signal,
     * stopping timer, all decisions entered, etc.)
     */
    private void processRefereeDecisions(FOPEvent e) {
        int nbRed = 0;
        int nbWhite = 0;
        int nbDecisions = 0;
        for (int i = 0; i < 3; i++) {
            if (refereeDecision[i] != null) {
                if (refereeDecision[i]) {
                    nbWhite++;
                } else {
                    nbRed++;
                }
                nbDecisions++;
            }
        }
        goodLift = null;
        if (nbWhite == 2 || nbRed == 2) {
            if (!downEmitted) {
                emitDown(e);
                downEmitted = true;
            }
        }
        if (nbDecisions == 3) {
            goodLift = nbWhite >= 2;
            if (!isDecisionDisplayScheduled()) {
                showDecisionAfterDelay(this);
            }
        }
    }

    /**
     * Reset decisions. Invoked when recomputing lifting order when a fresh clock is
     * given.
     */
    private void resetDecisions() {
        refereeDecision = new Boolean[3];
        refereeTime = new Integer[3];
    }

    private void resetEmittedFlags() {
        setInitialWarningEmitted(false);
        setFinalWarningEmitted(false);
        setTimeoutEmitted(false);
        setDownEmitted(false);
        setDecisionDisplayScheduled(false);
    }

    private void setClockOwner(Athlete athlete) {
        logger.trace("***setting clock owner to {} [{}]", athlete, LoggerUtils.whereFrom());
        this.clockOwner = athlete;
    }

    private void setCurAthlete(Athlete athlete) {
        logger.trace("changing curAthlete to {} [{}]", athlete, LoggerUtils.whereFrom());
        this.curAthlete = athlete;
    }

    private void setDecisionDisplayScheduled(boolean decisionDisplayScheduled) {
        this.decisionDisplayScheduled = decisionDisplayScheduled;
    }

    private synchronized void setDownEmitted(boolean downEmitted) {
        logger.trace("downEmitted {}", downEmitted);
        this.downEmitted = downEmitted;
    }

    private synchronized void setFinalWarningEmitted(boolean finalWarningEmitted) {
        logger.trace("finalWarningEmitted {}", finalWarningEmitted);
        this.finalWarningEmitted = finalWarningEmitted;
    }

    private synchronized void setInitialWarningEmitted(boolean initialWarningEmitted) {
        logger.trace("initialWarningEmitted {}", initialWarningEmitted);
        this.initialWarningEmitted = initialWarningEmitted;
    }

    private void setPreviousAthlete(Athlete athlete) {
        logger.trace("setting previousAthlete to {}", curAthlete);
        this.previousAthlete = athlete;
    }

    private synchronized void setTimeoutEmitted(boolean timeoutEmitted) {
        logger.trace("timeoutEmitted {}", timeoutEmitted);
        this.timeoutEmitted = timeoutEmitted;
    }

    synchronized private void showDecisionAfterDelay(Object origin2) {
        logger.debug("scheduling decision display");
        assert !isDecisionDisplayScheduled(); // caller checks.
        setDecisionDisplayScheduled(true); // so there are never two scheduled...
        new DelayTimer().schedule(() -> showDecisionNow(origin2), REVERSAL_DELAY);

    }

    /**
     * The decision is confirmed as official after the 3 second delay following
     * majority. After this delay, manual announcer intervention is required to
     * change and announce.
     */
    private void showDecisionNow(Object origin) {
        logger.trace("requesting decision display");
        // we need to recompute majority, since they may have been reversal
        int nbWhite = 0;
        for (int i = 0; i < 3; i++) {
            nbWhite = nbWhite + (Boolean.TRUE.equals(refereeDecision[i]) ? 1 : 0);
        }

        if (nbWhite >= 2) {
            goodLift = true;
            curAthlete.successfulLift();
        } else {
            goodLift = false;
            curAthlete.failedLift();
        }
        curAthlete.resetForcedAsCurrent();
        AthleteRepository.save(curAthlete);
        uiShowRefereeDecisionOnSlaveDisplays(curAthlete, goodLift, refereeDecision, refereeTime, origin);
        recomputeLiftingOrder();
        updateGlobalRankings();
        setState(DECISION_VISIBLE);
        // tell ourself to reset after 3 secs.
        new DelayTimer().schedule(() -> fopEventBus.post(new DecisionReset(origin)), DECISION_VISIBLE_DURATION);
    }

    /**
     * The decision is confirmed as official after the 3 second delay following
     * majority. After this delay, manual announcer intervention is required to
     * change and announce.
     */
    private void showExplicitDecision(ExplicitDecision e, Object origin) {
        logger.trace("explicit decision display");
        refereeDecision[0] = null;
        refereeDecision[2] = null;
        if (e.success) {
            goodLift = true;
            refereeDecision[1] = true;
            curAthlete.successfulLift();
        } else {
            goodLift = false;
            refereeDecision[1] = false;
            curAthlete.failedLift();
        }
        curAthlete.resetForcedAsCurrent();
        AthleteRepository.save(curAthlete);
        uiShowRefereeDecisionOnSlaveDisplays(curAthlete, goodLift, refereeDecision, refereeTime, origin);
        recomputeLiftingOrder();
        updateGlobalRankings();
        setState(DECISION_VISIBLE);
        // tell ourself to reset after 3 secs.
        new DelayTimer().schedule(() -> fopEventBus.post(new DecisionReset(origin)), DECISION_VISIBLE_DURATION);
    }

    private void transitionToBreak(BreakStarted e) {
        if (state == BREAK && (this.breakType != BreakType.GROUP_DONE && getBreakTimer().isRunning())) {
            logger.error("break already started {}", LoggerUtils.stackTrace());
        } else {
            BreakType breakType2 = e.getBreakType();
            logger.trace("transition to break {}", breakType2);
            setState(BREAK);
            this.setBreakType(breakType2);
            getAthleteTimer().stop();
            getBreakTimer().start();
        }
    }

    private void transitionToLifting(FOPEvent e, boolean stopBreakTimer) {
        logger.trace("transitionToLifting {} {} {}", e.getAthlete(), stopBreakTimer, LoggerUtils.whereFrom());
        recomputeLiftingOrder();
        // set the state now, otherwise attempt board will ignore request to display
        setState(CURRENT_ATHLETE_DISPLAYED);
        if (stopBreakTimer) {
            getBreakTimer().stop();
            setBreakType(null);
        }
        uiStartLifting(getGroup(), e.getOrigin());
        uiDisplayCurrentAthleteAndTime(true, e, false);
        updateGlobalRankings();
    }

    private void transitionToTimeRunning() {
        setClockOwner(getCurAthlete());
        resetEmittedFlags();
        prepareDownSignal();

        // enable master to listening for decision
        setState(TIME_RUNNING);
    }

    @SuppressWarnings("unused")
    private void uiDisplayCurrentWeight() {
        Integer nextAttemptRequestedWeight = curAthlete.getNextAttemptRequestedWeight();
        uiEventLogger.info("requested weight: {} (from curAthlete {})", nextAttemptRequestedWeight, getCurAthlete());
    }

    private void uiShowDownSignalOnSlaveDisplays(Object origin2) {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean downEmitted2 = isDownEmitted();
        uiEventLogger.debug("showDownSignalOnSlaveDisplays server={} emitted={}", emitSoundsOnServer2, downEmitted2);
        if (emitSoundsOnServer2 && !downEmitted2) {
            downSignal.emit();
            setDownEmitted(true);
        }
        uiEventBus.post(new UIEvent.DownSignal(origin2));
    }

    private void uiShowPlates(BarbellOrPlatesChanged e) {
        uiEventBus.post(new UIEvent.BarbellOrPlatesChanged(e.getOrigin()));
    }

    private void uiShowRefereeDecisionOnSlaveDisplays(Athlete athlete2, Boolean goodLift2, Boolean[] refereeDecision2,
            Integer[] shownTimes, Object origin2) {
        uiEventLogger.trace("showRefereeDecisionOnSlaveDisplays");
        uiEventBus.post(new UIEvent.Decision(athlete2, goodLift2, refereeDecision2[0], refereeDecision2[1],
                refereeDecision2[2], origin2));
    }

    private void uiShowUpdatedRankings() {
        uiEventBus.post(new UIEvent.GlobalRankingUpdated(this));
    }

    private void uiShowUpdateOnJuryScreen() {
        uiEventLogger.trace("uiShowUpdateOnJuryScreen");
        uiEventBus.post(new UIEvent.RefereeUpdate(curAthlete, refereeDecision[0], refereeDecision[1],
                refereeDecision[2], refereeTime[0], refereeTime[1], refereeTime[2], this));
    }

    private void uiStartLifting(Group group2, Object origin) {
        getUiEventBus().post(new UIEvent.StartLifting(group2, origin));
    }

    private void unexpectedEventInState(FOPEvent e, FOPState state) {
        // events not worth signaling
        if (e instanceof DecisionReset || e instanceof DecisionFullUpdate)
            // ignore
            return;
        String text = Translator.translate("Unexpected_Notification", e.getClass().getSimpleName(), state);
        logger/**/.warn(Translator.translate("Unexpected_Logging"), e.getClass().getSimpleName(), state);
        Notification.show(text, 5000, Position.BOTTOM_END);
    }

    private void updateRefereeDecisions(FOPEvent.DecisionFullUpdate e) {
        refereeDecision[0] = e.ref1;
        refereeTime[0] = e.ref1Time;
        refereeDecision[1] = e.ref2;
        refereeTime[1] = e.ref2Time;
        refereeDecision[2] = e.ref3;
        refereeTime[2] = e.ref3Time;
        processRefereeDecisions(e);
    }

    private void updateRefereeDecisions(FOPEvent.DecisionUpdate e) {
        refereeDecision[e.refIndex] = e.decision;
        refereeTime[e.refIndex] = 0;
        processRefereeDecisions(e);
    }

    /**
     * weight change while a lift is being performed (bar lifted above knees)
     * Lifting order is recomputed, so the app.owlcms.ui.displayselection can get
     * it, but not the attempt board state.
     *
     * @param e
     * @param curAthlete
     */
    private void weightChangeDoNotDisturb(WeightChange e) {
        AthleteSorter.liftingOrder(this.liftingOrder);
        this.setDisplayOrder(AthleteSorter.displayOrderCopy(this.liftingOrder));
        uiDisplayCurrentAthleteAndTime(false, e, false);
    }

}
