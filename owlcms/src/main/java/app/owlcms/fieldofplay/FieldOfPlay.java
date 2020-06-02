/***
 * Copyright (c) 2009-2020 Jean-François Lamy
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
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.UI;
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
import app.owlcms.forwarder.EventForwarder;
import app.owlcms.i18n.Translator;
import app.owlcms.sound.Sound;
import app.owlcms.sound.Tone;
import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class describes one field of play at runtime.
 *
 * It encapsulates the in-memory data structures used to describe the state of the competition and links them to the
 * database descriptions of the group and platform.
 *
 * The main method is {@link #handleFOPEvent(FOPEvent)} which implements a state automaton and processes events received
 * on the event bus.
 *
 * @author owlcms
 */
public class FieldOfPlay {

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

    private static final int REVERSAL_DELAY = 3000;

    private static final long DECISION_VISIBLE_DURATION = 3500;

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
    private EventBus postBus = null;
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

    private CountdownType countdownType;

    private boolean cjStarted;

    /**
     * Instantiates a new field of play state. When using this constructor {@link #init(List, IProxyTimer)} must later
     * be used to provide the athletes and set the athleteTimer
     *
     * @param group     the group (to get details such as name, and to reload athletes)
     * @param platform2 the platform (to get details such as name)
     */
    public FieldOfPlay(Group group, Platform platform2) {
        this.name = platform2.getName();
        this.fopEventBus = new EventBus("FOP-" + name);
        this.postBus = new EventBus("POST-" + name);

        // this.uiEventBus = new EventBus("UI-" + name);
        this.uiEventBus = new AsyncEventBus(Executors.newCachedThreadPool());

        this.athleteTimer = null;
        this.breakTimer = new ProxyBreakTimer(this);
        this.setPlatform(platform2);
    }

    /**
     * Instantiates a new field of play state. This constructor is only used for testing using mock timers.
     *
     * @param athletes the athletes
     * @param timer1   the athleteTimer
     */
    public FieldOfPlay(List<Athlete> athletes, IProxyTimer timer1, IProxyTimer breakTimer1, boolean testingMode) {
        this.name = "test";
        this.fopEventBus = new EventBus("FOP-" + this.name);
        this.uiEventBus = new EventBus("UI-" + this.name);
        this.postBus = new EventBus("POST-" + name);
        this.setTestingMode(testingMode);
        this.group = new Group();
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
        this.setPreviousAthlete(getCurAthlete()); // would be safer to use past lifting order
        setClockOwner(null); // athlete has lifted, time does not keep running for them
        uiShowDownSignalOnSlaveDisplays(e.origin);
        setState(DOWN_SIGNAL_VISIBLE);
    }

    public void emitFinalWarning() {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean emitted2 = isFinalWarningEmitted();
        // logger.trace("emitFinalWarning server={} emitted={}", emitSoundsOnServer2, emitted2);
        logger.info("{} Final Warning", getName());

        if (emitSoundsOnServer2 && !emitted2) {
            // instead of finalWarning2.wav sounds too much like down
            new Sound(getSoundMixer(), "initialWarning2.wav").emit();
            setFinalWarningEmitted(true);
        }
    }

    public void emitInitialWarning() {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean emitted2 = isInitialWarningEmitted();
        // logger.trace("emitInitialWarning server={} emitted={}", emitSoundsOnServer2, emitted2); // $NON-NLS-1

        if (emitSoundsOnServer2 && !emitted2) {
            new Sound(getSoundMixer(), "initialWarning2.wav").emit();
            setInitialWarningEmitted(true);
        }
    }

    public void emitTimeOver() {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean emitted2 = isTimeoutEmitted();
        // logger.trace("emitTimeout server={} emitted={}", emitSoundsOnServer2, emitted2);
        logger.info("{} Time Over", getName());

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

    public CountdownType getCountdownType() {
        return countdownType;
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

    public EventBus getPostEventBus() {
        return postBus;
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
            logger.debug("*** timeAllowed = timeRemaining = {}, clock owner = {}", timeAllowed, a);
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
     * FOP (Field of Play) events inform us of what is happening (e.g. athleteTimer started by timekeeper, decision
     * given by official, etc.) The current state determines what we do with the event. Typically, we update the state
     * of the field of play (e.g. time is now running) and we issue commands to the listening user interfaces (e.g.
     * start or stop time being displayed, show the decision, etc.)
     *
     * There is a fopEventBus for each active field of play. A given user interface will issue a FOP event on our
     * fopEventBus, this method reacts to the event by updating state, and we issue the resulting user interface
     * commands on the @link uiEventBus.
     *
     * One exception is timers: the task to send UI events to start stop/start/manage timers is delegated to
     * implementers of IProxyTimer; these classes remember the time and broadcast to all listening timers.
     *
     * @param e the event
     */
    @Subscribe
    public void handleFOPEvent(FOPEvent e) {
        logger.debug("{} state {}, event received {} {}", getName(), this.getState(), e.getClass().getSimpleName(), e);
        // it is always possible to explicitly interrupt competition (break between the
        // two lifts, technical incident, etc.)
        if (e instanceof BreakStarted) {
            transitionToBreak((BreakStarted) e);
            return;
        } else if (e instanceof BreakPaused) {
            // logger.debug("break paused {}", LoggerUtils.stackTrace());
        } else if (e instanceof StartLifting) {
            transitionToLifting(e, true);
        } else if (e instanceof BarbellOrPlatesChanged) {
            uiShowPlates((BarbellOrPlatesChanged) e);
            return;
        } else if (e instanceof SwitchGroup) {

            Group oldGroup = this.getGroup();
            SwitchGroup switchGroup = (SwitchGroup) e;
            Group newGroup = switchGroup.getGroup();

            if (ObjectUtils.equals(oldGroup, newGroup)) {
                loadGroup(newGroup, this, true);
                // SwitchGroup to self is used to refresh lists and should not cause end of a break.
                if (state == BREAK || state == INACTIVE) {
                    recomputeAndRefresh(e);
                } else {
                    // end break if needed and start lifting.
                    transitionToLifting(e, true);
                }
            } else {
                if (state != BREAK && state != INACTIVE) {
                    setState(INACTIVE);
                    athleteTimer.stop();
                } else if (state == BREAK && breakType == BreakType.GROUP_DONE) {
                    setState(INACTIVE);
                }
                loadGroup(newGroup, this, true);
                recomputeAndRefresh(e);
            }
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
            } else {
                unexpectedEventInState(e, INACTIVE);
            }
            break;

        case BREAK:
            if (e instanceof StartLifting) {
                transitionToLifting(e, true);
            } else if (e instanceof BreakPaused) {
                getBreakTimer().stop();
                pushOut(new UIEvent.BreakPaused(e.getOrigin()));
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
                this.setPreviousAthlete(getCurAthlete()); // would be safer to use past lifting order
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
                this.setPreviousAthlete(getCurAthlete()); // would be safer to use past lifting order
                this.setClockOwner(null);
                showExplicitDecision(((ExplicitDecision) e), e.origin);
            } else if (e instanceof ForceTime) {
                getAthleteTimer().setTimeRemaining(((ForceTime) e).timeAllowed);
                setState(CURRENT_ATHLETE_DISPLAYED);
            } else if (e instanceof TimeStopped) {
                // ignore duplicate time stopped
            } else if (e instanceof TimeOver) {
                // ignore, already dealt by timer
            } else if (e instanceof StartLifting) {
                // nothing to do, end of break when clock was already started
            } else {
                unexpectedEventInState(e, TIME_STOPPED);
            }
            break;

        case DOWN_SIGNAL_VISIBLE:
//            this.setPreviousAthlete(curAthlete); // would be safer to use past lifting order
//            this.setClockOwner(null);
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
                logger.debug("{} resetting decisions", getName());
                pushOut(new UIEvent.DecisionReset(getCurAthlete(), e.origin));
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
        this.fopEventBus.register(this);
        EventForwarder.listenToFOP(this);
        this.setCurAthlete(null);
        this.setClockOwner(null);
        this.previousAthlete = null;
        this.setLiftingOrder(athletes);
        if (athletes != null && athletes.size() > 0) {
            // SwitchGroup will also recompute if needed. Innocuous.
            recomputeLiftingOrder();
        }
        logger.debug("group {} athletes {}", getGroup(), athletes.size());
        if (state == null) {
            this.setState(INACTIVE);
        }

        // force a wake up on user interfaces
        pushOut(new UIEvent.SwitchGroup(getGroup(), getState(), getCurAthlete(), this));
        logger.trace("end of init state=" + state);
    }

    public boolean isCjStarted() {
        return cjStarted;
    }

    public boolean isEmitSoundsOnServer() {
        return getSoundMixer() != null;
    }

    public boolean isTestingMode() {
        return testingMode;
    }

    /**
     * all grids get their athletes from this method.
     *
     * @param group
     * @param origin
     * @param forceLoad reload from database even if current group
     */
    public void loadGroup(Group group, Object origin, boolean forceLoad) {
        if (Objects.equals(this.getGroup(), group) && !forceLoad) {
            // already loaded
            logger.trace("group {} already loaded", group != null ? group.getName() : null);
            return;
        }
        this.setGroup(group);
        if (group != null) {
            logger.debug("{} loading data for group {} [{} {} ]",
                    this.getName(),
                    (group != null ? group.getName() : group),
                    origin.getClass().getSimpleName(),
                    LoggerUtils.whereFrom());
            List<Athlete> findAllByGroupAndWeighIn = AthleteRepository.findAllByGroupAndWeighIn(group, true);
            init(findAllByGroupAndWeighIn, athleteTimer, breakTimer);
        } else {
            init(new ArrayList<Athlete>(), athleteTimer, breakTimer);
        }
    }

    public void pushOut(app.owlcms.fieldofplay.UIEvent event) {
        getUiEventBus().post(event);
        getPostEventBus().post(event);
    }

    public synchronized void recomputeLiftingOrder() {
        recomputeLiftingOrder(true);
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

    public void setCjStarted(boolean cjStarted) {
        this.cjStarted = cjStarted;
    }

    public void setCountdownType(CountdownType countdownType) {
        this.countdownType = countdownType;
    }

    public void setDisplayOrder(List<Athlete> displayOrder) {
        this.displayOrder = displayOrder;
        // this sets the order within the currenlty lifting group only
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
        loadGroup(group, origin, true);
        logger.trace("{} start lifting for group {} origin={}", this.getName(),
                (group != null ? group.getName() : group), origin);
        getFopEventBus().post(new StartLifting(origin));
    }

    public void uiDisplayCurrentAthleteAndTime(boolean currentDisplayAffected, FOPEvent e, boolean displayToggle) {
        Integer clock = getAthleteTimer().getTimeRemaining();

        curWeight = 0;
        if (getCurAthlete() != null) {
            curWeight = getCurAthlete().getNextAttemptRequestedWeight();
        }
        // if only one athlete, no next athlete
        Athlete nextAthlete = getLiftingOrder().size() > 1 ? getLiftingOrder().get(1) : null;

        Athlete changingAthlete = null;
        if (e instanceof WeightChange) {
            changingAthlete = e.getAthlete();
        }
        boolean inBreak = false;
        if (state == FOPState.BREAK) {
            inBreak = ((breakTimer != null && breakTimer.isRunning()));
        }
        pushOut(new UIEvent.LiftingOrderUpdated(getCurAthlete(), nextAthlete, previousAthlete, changingAthlete,
                getLiftingOrder(), getDisplayOrder(), clock, currentDisplayAffected, displayToggle, e.getOrigin(),
                inBreak));

        logger.info("current athlete = {} attempt {}, requested = {}, timeAllowed={} timeRemainingAtLastStop={}",
                getCurAthlete(), getCurAthlete() != null ? getCurAthlete().getAttemptedLifts() + 1 : 0, curWeight,
                clock,
                getAthleteTimer().getTimeRemainingAtLastStop());
    }

    /**
     * Sets the state.
     *
     * @param state the new state
     */
    void setState(FOPState state) {
        logger.debug("{} entering {} {}", getName(), state, LoggerUtils.whereFrom());
        // if (state == INACTIVE) {
        // logger.debug("entering inactive {}",LoggerUtils.stackTrace());
        // }
        if (state == CURRENT_ATHLETE_DISPLAYED) {
            group.setDone(getCurAthlete() == null || getCurAthlete().getAttemptsDone() >= 6);
        } else if (state == BREAK) {
            group.setDone(breakType == BreakType.GROUP_DONE);
        }
        this.state = state;
    }

    private void broadcast(String string) {
        getUiEventBus().post(new UIEvent.Broadcast(string, this));
    }

    private void displayOrBreakIfDone(FOPEvent e) {
        if (getCurAthlete() != null && getCurAthlete().getAttemptsDone() < 6) {
            uiDisplayCurrentAthleteAndTime(true, e, false);
            setState(CURRENT_ATHLETE_DISPLAYED);
            group.setDone(false);
        } else {
            pushOutDone();
            // special kind of break that allows moving back in case of jury reversal
            setBreakType(BreakType.GROUP_DONE);
            setState(BREAK);
            group.setDone(true);
        }
    }

    /**
     * Perform weight change and adjust state.
     *
     * If the clock was started and we come back to the clock owner, we set the state to TIME_STARTED If in a break, we
     * are careful not to update, unless the change causes an exit from the break (e.g. jury overrule on last lift)
     * Otherwise we update the displays.
     *
     * @param wc
     */
    private void doWeightChange(WeightChange wc) {
        Athlete changingAthlete = wc.getAthlete();
        Integer newWeight = changingAthlete.getNextAttemptRequestedWeight();
        logger.trace("&&1 cur={} curWeight={} changing={} newWeight={}", getCurAthlete(), curWeight, changingAthlete,
                newWeight);
        logger.trace("&&2 clockOwner={} clockLastStopped={} state={}", clockOwner,
                getAthleteTimer().getTimeRemainingAtLastStop(), state);

        boolean stopAthleteTimer = false;
        if (clockOwner != null && getAthleteTimer().isRunning()) {
            // time is running
            if (changingAthlete.equals(clockOwner)) {
                logger.trace("&&3.A clock IS running for changing athlete {}", changingAthlete);
                // X is the current lifter
                // if a real change (and not simply a declaration that does not change weight),
                // make sure clock is stopped.
                if (curWeight != newWeight) {
                    logger.trace("&&3.A.A1 weight change for clock owner: clock running: stop clock");
                    getAthleteTimer().stop(); // memorize time
                    stopAthleteTimer = true; // make sure we broacast to clients
                    doWeightChange(wc, changingAthlete, clockOwner, stopAthleteTimer);
                } else {
                    logger.trace("&&3.A.B declaration at same weight for clock owner: leave clock running");
                    // no actual weight change. this is most likely a declaration.
                    // we do the call to trigger a notification on official's screens, but request
                    // that the clock keep running
                    doWeightChange(wc, changingAthlete, clockOwner, false);
                    return;
                }
            } else {
                logger.trace("&&3.B clock running, but NOT for changing athlete, do not update attempt board");
                weightChangeDoNotDisturb(wc);
                return;
            }
        } else if (clockOwner != null && !getAthleteTimer().isRunning()) {
            logger.trace("&&3.B clock NOT running for changing athlete {}", changingAthlete);
            // time was started (there is an owner) but is not currently running
            // time was likely stopped by timekeeper because coach signaled change of weight
            doWeightChange(wc, changingAthlete, clockOwner, true);
        } else {
            logger.trace("&&3.C1 no clock owner, time is not running");
            // time is not running
            recomputeLiftingOrder();
            setStateUnlessInBreak(CURRENT_ATHLETE_DISPLAYED);
            logger.trace("&&3.C2 displaying, curAthlete={}, state={}", getCurAthlete(), state);
            uiDisplayCurrentAthleteAndTime(true, wc, false);
            updateGlobalRankings();
        }
    }

    private void doWeightChange(WeightChange wc, Athlete changingAthlete, Athlete clockOwner,
            boolean currentDisplayAffected) {
        recomputeLiftingOrder(currentDisplayAffected);
        // if the currentAthlete owns the clock, then the next ui update will show the
        // correct athlete and
        // the time needs to be restarted (state = TIME_STOPPED). Going to TIME_STOPPED
        // allows the decision to register if the announcer
        // forgets to start time.

        // otherwise we need to announce new athlete (state = CURRENT_ATHLETE_DISPLAYED)

        FOPState newState = state;
        if (clockOwner.equals(getCurAthlete()) && currentDisplayAffected) {
            newState = TIME_STOPPED;
        } else if (currentDisplayAffected) {
            newState = CURRENT_ATHLETE_DISPLAYED;
        }
        logger.trace("&&3.X change for {}, new cur = {}, displayAffected = {}, switching to {}", changingAthlete,
                getCurAthlete(), currentDisplayAffected, newState);
        setStateUnlessInBreak(newState);
        uiDisplayCurrentAthleteAndTime(currentDisplayAffected, wc, false);
        updateGlobalRankings();
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
            try {
                downSignal = new Tone(getSoundMixer(), 1100, 1200, 1.0);
            } catch (IllegalArgumentException | LineUnavailableException e) {
                logger.error("{}\n{}", e.getCause(), LoggerUtils.stackTrace(e));
                broadcast("SoundSystemProblem");
            }
        }
    }

    /**
     * Compute events resulting from decisions received so far (down signal, stopping timer, all decisions entered,
     * etc.)
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

    private void pushOutDone() {
        logger.debug("group {} done", group);
        UIEvent.GroupDone event = new UIEvent.GroupDone(this.getGroup(), null);
        pushOut(event);
    }

    private void recomputeAndRefresh(FOPEvent e) {
        recomputeLiftingOrder();
        updateGlobalRankings();
        pushOut(new UIEvent.SwitchGroup(this.getGroup(), this.getState(), this.getCurAthlete(),
                e.getOrigin()));
    }

    private void recomputeLiftingOrder(boolean currentDisplayAffected) {
        List<Athlete> liftingOrder2 = this.getLiftingOrder();
        AthleteSorter.liftingOrder(liftingOrder2);
        setDisplayOrder(AthleteSorter.displayOrderCopy(liftingOrder2));
        this.setCurAthlete(liftingOrder2.isEmpty() ? null : liftingOrder2.get(0));
        if (curAthlete == null) {
            pushOutDone();
            return;
        }

        int timeAllowed = getTimeAllowed();
        Integer attemptsDone = curAthlete.getAttemptsDone();
        logger.debug("{} recomputed lifting order curAthlete={} prevlifter={} time={} attemptsDone={} [{}]",
                getName(),
                getCurAthlete() != null ? getCurAthlete().getFullName() : "",
                previousAthlete != null ? previousAthlete.getFullName() : "",
                timeAllowed,
                attemptsDone,
                LoggerUtils.whereFrom());
        if (currentDisplayAffected) {
            getAthleteTimer().setTimeRemaining(timeAllowed);
        }
        // for the purpose of showing team scores, this is good enough.
        // if the current athlete has done all lifts, the group is marked as done.
        // if editing the athlete later gives back an attempt, then the state change will take
        // place and subscribers will revert to current athlete display.
        boolean done = attemptsDone >= 6;
        if (done) {
            pushOutDone();
        }
        group.setDone(done);
    }

    /**
     * Reset decisions. Invoked when recomputing lifting order when a fresh clock is given.
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
        logger.debug("***setting clock owner to {} [{}]", athlete, LoggerUtils.whereFrom());
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

    private void setLiftingOrder(List<Athlete> liftingOrder) {
        this.liftingOrder = liftingOrder;
    }

    private void setPreviousAthlete(Athlete athlete) {
        logger.trace("setting previousAthlete to {}", getCurAthlete());
        this.previousAthlete = athlete;
    }

    /**
     * Don't interrupt break if official-induced break. Interrupt break if it is simply "group done".
     *
     * @param newState the state we want to go to if there is no break
     */
    private void setStateUnlessInBreak(FOPState newState) {
        if (state == INACTIVE) {
            // remain in INACTIVE state (do nothing)
        } else if (state == BREAK) {
            logger.debug("{} Break {}", getName(), state, getBreakType());
            // if in a break, we don't stop break timer on a weight change.
            if (getBreakType() == BreakType.GROUP_DONE) {
                // weight change in state GROUP_DONE can happen if there is a loading error
                // and there is no jury deliberation break -- the weight change is entered
                // directly
                // in this case, we need to go back to lifting.
                // set the state now, otherwise attempt board will ignore request to display if
                // in a break
                setState(newState);
                Competition competition = Competition.getCurrent();
                competition.computeGlobalRankings(true);
                if (newState == CURRENT_ATHLETE_DISPLAYED) {
                    uiStartLifting(group, this);
                } else {
                    uiShowUpdatedRankings();
                }
                getBreakTimer().stop();
            } else {
                // remain in break state
                setState(BREAK);
            }
        } else {
            setState(newState);
        }
    }

    private synchronized void setTimeoutEmitted(boolean timeoutEmitted) {
        logger.trace("timeoutEmitted {}", timeoutEmitted);
        this.timeoutEmitted = timeoutEmitted;
    }

    synchronized private void showDecisionAfterDelay(Object origin2) {
        logger.trace("{} scheduling decision display", getName());
        assert !isDecisionDisplayScheduled(); // caller checks.
        setDecisionDisplayScheduled(true); // so there are never two scheduled...
        new DelayTimer().schedule(() -> showDecisionNow(origin2), REVERSAL_DELAY);

    }

    /**
     * The decision is confirmed as official after the 3 second delay following majority. After this delay, manual
     * announcer intervention is required to change and announce.
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
            this.setCjStarted((getCurAthlete().getAttemptsDone() > 3));
            getCurAthlete().successfulLift();
        } else {
            goodLift = false;
            this.setCjStarted((getCurAthlete().getAttemptsDone() > 3));
            getCurAthlete().failedLift();
        }
        getCurAthlete().resetForcedAsCurrent();
        AthleteRepository.save(getCurAthlete());
        uiShowRefereeDecisionOnSlaveDisplays(getCurAthlete(), goodLift, refereeDecision, refereeTime, origin);
        recomputeLiftingOrder();
        updateGlobalRankings();
        setState(DECISION_VISIBLE);
        // tell ourself to reset after 3 secs.
        new DelayTimer().schedule(() -> fopEventBus.post(new DecisionReset(origin)), DECISION_VISIBLE_DURATION);
    }

    /**
     * The decision is confirmed as official after the 3 second delay following majority. After this delay, manual
     * announcer intervention is required to change and announce.
     */
    private void showExplicitDecision(ExplicitDecision e, Object origin) {
        logger.trace("explicit decision display");
        refereeDecision[0] = null;
        refereeDecision[2] = null;
        if (e.success) {
            goodLift = true;
            refereeDecision[1] = true;
            getCurAthlete().successfulLift();
        } else {
            goodLift = false;
            refereeDecision[1] = false;
            getCurAthlete().failedLift();
        }
        getCurAthlete().resetForcedAsCurrent();
        AthleteRepository.save(getCurAthlete());
        uiShowRefereeDecisionOnSlaveDisplays(getCurAthlete(), goodLift, refereeDecision, refereeTime, origin);
        recomputeLiftingOrder();
        updateGlobalRankings();
        setState(DECISION_VISIBLE);
        // tell ourself to reset after 3 secs.
        new DelayTimer().schedule(() -> fopEventBus.post(new DecisionReset(origin)), DECISION_VISIBLE_DURATION);
    }

    private void transitionToBreak(BreakStarted e) {
        ProxyBreakTimer breakTimer2 = getBreakTimer();
        BreakType breakType2 = e.getBreakType();
        CountdownType countdownType2 = e.getCountdownType();
        if (state == BREAK && breakTimer2.isRunning()
                && (breakType2 != getBreakType() || countdownType2 != getCountdownType())) {
            // changing the kind of break
            logger.debug("{} switching break type while in break : current {} new {}", getName(), getBreakType(),
                    e.getBreakType());
            breakTimer2.stop();
        }

        logger.debug("transition to break {} {}", breakType2, countdownType2);
        setState(BREAK);
        this.setBreakType(breakType2);
        this.setCountdownType(countdownType2);
        getAthleteTimer().stop();

        if (e.isIndefinite()) {
            breakTimer2.setIndefinite();
        } else if (countdownType2 == CountdownType.DURATION) {
            breakTimer2.setTimeRemaining(e.getTimeRemaining());
            breakTimer2.setEnd(null);
        } else {
            breakTimer2.setTimeRemaining(0);
            breakTimer2.setEnd(e.getTargetTime());
        }
        // this will broadcast to all slave break timers
        if (!breakTimer2.isRunning()) {
            breakTimer2.setOrigin(e.getOrigin());
            breakTimer2.start();
        }
        logger.trace("started break timers {}", breakType2);
    }

    private void transitionToLifting(FOPEvent e, boolean stopBreakTimer) {
        logger.trace("transitionToLifting {} {} {}", e.getAthlete(), stopBreakTimer, LoggerUtils.whereFrom());
        recomputeLiftingOrder();
        updateGlobalRankings();
        Athlete clockOwner = getClockOwner();
        if (getCurAthlete() != null && getCurAthlete().equals(clockOwner)) {
            setState(TIME_STOPPED); // allows referees to enter decisions even if time is not restarted (which
                                    // sometimes happens).
        } else {
            setState(CURRENT_ATHLETE_DISPLAYED);
        }
        if (stopBreakTimer) {
            getBreakTimer().stop();
            setBreakType(null);
        }
        uiStartLifting(getGroup(), e.getOrigin());
        uiDisplayCurrentAthleteAndTime(true, e, false);
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
        Integer nextAttemptRequestedWeight = getCurAthlete().getNextAttemptRequestedWeight();
        uiEventLogger.info("requested weight: {} (from curAthlete {})", nextAttemptRequestedWeight, getCurAthlete());
    }

    private synchronized void uiShowDownSignalOnSlaveDisplays(Object origin2) {
        boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
        boolean downEmitted2 = isDownEmitted();
        uiEventLogger.debug("showDownSignalOnSlaveDisplays server={} emitted={}", emitSoundsOnServer2, downEmitted2);
        if (emitSoundsOnServer2 && !downEmitted2) {
            // sound is synchronous, we don't want to wait.
            new Thread(() -> {
                try {
                    downSignal.emit();
                } catch (IllegalArgumentException | LineUnavailableException e) {
                    broadcast("SoundSystemProblem");
                }
            }).start();
            setDownEmitted(true);
        }
        pushOut(new UIEvent.DownSignal(origin2));
    }

    private void uiShowPlates(BarbellOrPlatesChanged e) {
        pushOut(new UIEvent.BarbellOrPlatesChanged(e.getOrigin()));
    }

    private void uiShowRefereeDecisionOnSlaveDisplays(Athlete athlete2, Boolean goodLift2, Boolean[] refereeDecision2,
            Integer[] shownTimes, Object origin2) {
        uiEventLogger.trace("showRefereeDecisionOnSlaveDisplays");
        pushOut(new UIEvent.Decision(athlete2, goodLift2, refereeDecision2[0], refereeDecision2[1],
                refereeDecision2[2], origin2));
    }

    private void uiShowUpdatedRankings() {
        pushOut(new UIEvent.GlobalRankingUpdated(this));
    }

    private void uiShowUpdateOnJuryScreen() {
        uiEventLogger.trace("uiShowUpdateOnJuryScreen");
        pushOut(new UIEvent.RefereeUpdate(getCurAthlete(), refereeDecision[0], refereeDecision[1],
                refereeDecision[2], refereeTime[0], refereeTime[1], refereeTime[2], this));
    }

    private void uiStartLifting(Group group2, Object origin) {
        pushOut(new UIEvent.StartLifting(group2, origin));
    }

    private void unexpectedEventInState(FOPEvent e, FOPState state) {
        // events not worth signaling
        if (e instanceof DecisionReset || e instanceof DecisionFullUpdate) {
            // ignore
            return;
        }
        String text = Translator.translate("Unexpected_Notification", e.getClass().getSimpleName(), state);
        logger./**/warn(Translator.translate("Unexpected_Logging"), e.getClass().getSimpleName(), state);
        if (UI.getCurrent() != null) {
            Notification.show(text, 5000, Position.BOTTOM_END);
        }
    }

    private void updateGlobalRankings() {
        logger.debug("update rankings {}", LoggerUtils.whereFrom());
        Competition competition = Competition.getCurrent();
        competition.computeGlobalRankings(false);
        uiShowUpdatedRankings();
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
     * weight change while a lift is being performed (bar lifted above knees) Lifting order is recomputed, so the
     * app.owlcms.ui.displayselection can get it, but not the attempt board state.
     *
     * @param e
     * @param curAthlete
     */
    private void weightChangeDoNotDisturb(WeightChange e) {
        AthleteSorter.liftingOrder(this.getLiftingOrder());
        this.setDisplayOrder(AthleteSorter.displayOrderCopy(this.getLiftingOrder()));
        uiDisplayCurrentAthleteAndTime(false, e, false);
        updateGlobalRankings();
    }

}
