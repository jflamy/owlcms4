/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import static app.owlcms.fieldofplay.FOPState.BREAK;
import static app.owlcms.fieldofplay.FOPState.CURRENT_ATHLETE_DISPLAYED;
import static app.owlcms.fieldofplay.FOPState.DECISION_VISIBLE;
import static app.owlcms.fieldofplay.FOPState.DOWN_SIGNAL_VISIBLE;
import static app.owlcms.fieldofplay.FOPState.INACTIVE;
import static app.owlcms.fieldofplay.FOPState.TIME_RUNNING;
import static app.owlcms.fieldofplay.FOPState.TIME_STOPPED;
import static app.owlcms.uievents.BreakType.BEFORE_INTRODUCTION;
import static app.owlcms.uievents.BreakType.FIRST_CJ;
import static app.owlcms.uievents.BreakType.FIRST_SNATCH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.LiftDefinition;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordFilter;
import app.owlcms.fieldofplay.FOPEvent.BarbellOrPlatesChanged;
import app.owlcms.fieldofplay.FOPEvent.CeremonyDone;
import app.owlcms.fieldofplay.FOPEvent.CeremonyStarted;
import app.owlcms.fieldofplay.FOPEvent.DecisionFullUpdate;
import app.owlcms.fieldofplay.FOPEvent.DecisionReset;
import app.owlcms.fieldofplay.FOPEvent.DecisionUpdate;
import app.owlcms.fieldofplay.FOPEvent.DownSignal;
import app.owlcms.fieldofplay.FOPEvent.ExplicitDecision;
import app.owlcms.fieldofplay.FOPEvent.ForceTime;
import app.owlcms.fieldofplay.FOPEvent.JuryDecision;
import app.owlcms.fieldofplay.FOPEvent.JuryMemberDecisionUpdate;
import app.owlcms.fieldofplay.FOPEvent.StartLifting;
import app.owlcms.fieldofplay.FOPEvent.SummonReferee;
import app.owlcms.fieldofplay.FOPEvent.SwitchGroup;
import app.owlcms.fieldofplay.FOPEvent.TimeOver;
import app.owlcms.fieldofplay.FOPEvent.TimeStarted;
import app.owlcms.fieldofplay.FOPEvent.TimeStopped;
import app.owlcms.fieldofplay.FOPEvent.WeightChange;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.EventForwarder;
import app.owlcms.monitors.IUnregister;
import app.owlcms.monitors.MQTTMonitor;
import app.owlcms.nui.lifting.AnnouncerContent;
import app.owlcms.nui.lifting.TimekeeperContent;
import app.owlcms.sound.Sound;
import app.owlcms.sound.Tone;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakStarted;
import app.owlcms.uievents.UIEvent.JuryNotification;
import app.owlcms.utils.DelayTimer;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * This class describes one field of play at runtime.
 *
 * It encapsulates the in-memory data structures used to describe the state of the competition and links them to the database descriptions of the group and
 * platform.
 *
 * The main method is {@link #handleFOPEvent(FOPEvent)} which implements a state automaton and processes events received on the event bus.
 *
 * @author owlcms
 */
public class FieldOfPlay implements IUnregister {

	public static final long DECISION_VISIBLE_DURATION = 3500;
	public static final int REVERSAL_DELAY = 3000;
	private static final int DEFAULT_BREAK_DURATION = 10 * 60 * 1000;
	private static final int WAKEUP_DURATION_MS = 20000;

	/**
	 * @param fieldOfPlay
	 * @return the name
	 */
	public static String getLoggingName(FieldOfPlay fieldOfPlay) {
		return "FOP "
		        + (fieldOfPlay != null ? fieldOfPlay.name
		                /* +System.identityHashCode(fieldOfPlay) */
		                : "-")
		        + "    ";
	}

	/**
	 * Instantiates a new field of play state. This constructor is only used for testing using mock timers.
	 *
	 * @param athletes the athletes
	 * @param timer1   the athleteTimer
	 */
	public static FieldOfPlay mockFieldOfPlay(List<Athlete> athletes, IProxyTimer timer1, IProxyTimer breakTimer1) {
		FieldOfPlay mFop = new FieldOfPlay();
		mFop.name = "test";
		mFop.fopEventBus = new EventBus("FOP-" + mFop.name);
		mFop.uiEventBus = new EventBus("UI-" + mFop.name);
		mFop.eventForwardingBus = new EventBus("POST-" + mFop.name);
		mFop.setTestingMode(true);
		mFop.setGroup(new Group());
		mFop.init(athletes, timer1, breakTimer1, true);

		mFop.fopEventBus.register(mFop);
		return mFop;
	}

	private LinkedHashMap<String, Participation> ageGroupMap = new LinkedHashMap<>();
	private IProxyTimer athleteTimer;
	private IProxyTimer breakTimer;
	private BreakType breakType;
	private CeremonyType ceremonyType;
	private boolean cjStarted;
	/**
	 * the clock owner is the last athlete for whom the clock has actually started.
	 */
	private Athlete clockOwner;
	private int clockOwnerInitialTimeAllowed;
	private CountdownType countdownType;
	private Athlete curAthlete;
	private int curWeight;
	private boolean decisionDisplayScheduled = false;
	private FOPEvent deferredBreak;
	private List<Athlete> displayOrder;
	private boolean downEmitted;
	@SuppressWarnings("unused")
	private Tone downSignal;
	private boolean finalWarningEmitted;
	private EventBus fopEventBus = null;
	private boolean forcedTime = false;
	private Boolean goodLift;
	private Group group = null;
	private boolean initialWarningEmitted;
	private long lastGroupLoaded;
	private List<Athlete> leaders;
	private List<Athlete> liftingOrder;
	private int liftsDoneAtLastStart;
	final private Logger logger = (Logger) LoggerFactory.getLogger(FieldOfPlay.class);
	private TreeMap<String, TreeSet<Athlete>> medals;
	private String name;
	private Platform platform = null;
	private EventBus eventForwardingBus = null;
	private Integer prevHash;
	private Athlete previousAthlete;
	private Boolean[] refereeDecision;
	private boolean refereeForcedDecision;
	private Long[] refereeTime;
	private FOPState state;
	private boolean testingMode;
	private boolean timeoutEmitted;
	final private Logger timingLogger = (Logger) LoggerFactory.getLogger(this.logger.getName() + "_Timing");
	private EventBus uiEventBus = null;
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger(this.logger.getName() + "_UI");
	private Thread wakeUpRef;
	private Integer weightAtLastStart;
	private int prevWeight;
	private JsonValue recordsJson;
	private List<RecordEvent> challengedRecords;
	private List<RecordEvent> newRecords;
	private List<RecordEvent> lastChallengedRecords;
	private List<RecordEvent> lastNewRecords;
	private boolean announcerDecisionImmediate = true;
	private Boolean[] juryMemberDecision;
	private Integer[] juryMemberTime;
	private Athlete athleteUnderReview;
	Map<Athlete, List<RecordEvent>> displayableRecordsByAthlete = new HashMap<>();
	Map<Athlete, List<RecordEvent>> eligibleRecordsByAthlete = new HashMap<>();
	Set<RecordEvent> groupRecords = new HashSet<>();
	private boolean clockStoppedDecisionsAllowed;
	private Group videoGroup;
	private Category videoCategory;
	private AgeGroup videoAgeGroup;
	private List<Athlete> resultsOrder;
	private boolean cjBreakDisplayed;
	private EventForwarder eventForwarder;
	private JuryDecision toBeAnnouncedJuryDecision;
	private FieldOfPlay existingFOP;
	private Queue<FOPEvent.WeightChange> deferredWeightChanges = new LinkedList<>();
	private Athlete nextAthlete;
	private TimerTask decisionDisplayTimer;
	private boolean singleReferee;
	private Sound finalWarningSound;
	private Sound initialWarningSound;
	private Sound timeOverSound;
	private boolean useCollarsIfAvailable;
	private int barWeight;
	private boolean lightBarInUse;

	public FieldOfPlay() {
	}

	/**
	 * Instantiates a new field of play state. When using this constructor {@link #init(List, IProxyTimer)} must later be used to provide the athletes and set
	 * the athleteTimer
	 *
	 * @param group     the group (to get details such as name, and to reload athletes)
	 * @param platform2 the platform (to get details such as name)
	 */
	public FieldOfPlay(Group group, Platform platform2) {
		this.name = platform2.getName();
		initEventBuses();

		this.existingFOP = OwlcmsFactory.getFOPByName(this.name);
		if (this.existingFOP != null) {
			RuntimeException exc = new RuntimeException("FOP " + this.name + " already exists");
			LoggerUtils.logError(this.logger, exc, true);
			throw exc;
		}

		// check if refereeing devices connected via MQTT are in use
		String paramMqttServer = Config.getCurrent().getParamMqttServer();
		boolean mqttInternal = Config.getCurrent().getParamMqttInternal();

		OwlcmsFactory.getFopByName().put(this.name, this);

		if (mqttInternal || paramMqttServer != null) {
			MQTTMonitor mm = MQTTMonitor.initMQTTMonitorByName(this.name, this);
			mm.start();
		}

		this.athleteTimer = null;
		this.breakTimer = null;
		this.setPlatform(platform2);

		this.fopEventBus.register(this);
		this.setEventForwarder(EventForwarder.initEventForwarderByName(this.name, this));
	}

	public void broadcast(String string) {
		pushOutUIEvent(new UIEvent.Broadcast(string, this, this));
	}

	public boolean computeShowAllGroupRecords() {
		boolean forced = Config.getCurrent().featureSwitch("forceAllGroupRecords");
		return forced || Boolean.TRUE.equals(RecordConfig.getCurrent().getShowAllCategoryRecords());
	}

	public boolean computeShowInformationalRecords() {
		boolean forced = Config.getCurrent().featureSwitch("forceAllFederationRecords");
		return forced || Boolean.TRUE.equals(RecordConfig.getCurrent().getShowAllFederations());
	}

	/**
	 * @return how many lifts done so far in the group.
	 */
	public int countLiftsDone() {
		int liftsDone = AthleteSorter.countLiftsDone(getDisplayOrder());
		return liftsDone;
	}

	public synchronized void fopEventPost(FOPEvent e) {
		e.setFop(this);
		handleFOPEvent(e);
	}

	public LinkedHashMap<String, Participation> getAgeGroupMap() {
		return this.ageGroupMap;
	}

	/**
	 * @return the server-side athleteTimer that tracks the time used
	 */
	public IProxyTimer getAthleteTimer() {
		return this.athleteTimer;
	}

	public Athlete getAthleteUnderReview() {
		return this.athleteUnderReview;
	}

	public IBreakTimer getBreakTimer() {
		// if (!(this.breakTimer.getClass().isAssignableFrom(ProxyBreakTimer.class)))
		// throw new RuntimeException("wrong athleteTimer setup");
		return (IBreakTimer) this.breakTimer;
	}

	public BreakType getBreakType() {
		if (this.state != BREAK) {
			return null;
		}
		return this.breakType;
	}

	public CeremonyType getCeremonyType() {
		return this.ceremonyType;
	}

	public List<RecordEvent> getChallengedRecords() {
		return this.challengedRecords;
	}

	public Athlete getClockOwner() {
		return this.clockOwner;
	}

	/**
	 * @return 0 if clock has not been started, 120000 or 60000 depending on time allowed when clock is started
	 */
	public int getClockOwnerInitialTimeAllowed() {
		return this.clockOwnerInitialTimeAllowed;
	}

	public CountdownType getCountdownType() {
		return this.countdownType;
	}

	/**
	 * @return the current athlete (to be called, or currently lifting)
	 */
	public Athlete getCurAthlete() {
		// the UI framework uses simple setters to set, for example, the next request.
		// but we need to validate in the context of the FOP (previous lifted values,
		// etc.).
		// So we set the FOP forcefully, to make sure all validations are in the correct
		// context.
		if (this.curAthlete != null) {
			this.curAthlete.setFop(this);
		}
		return this.curAthlete;
	}

	public LiftDefinition.Stage getCurrentStage() {
		if (this.state == INACTIVE) {
			return null;
		} else if (this.state == BREAK) {
			switch (this.breakType) {
				case BEFORE_INTRODUCTION:
				case FIRST_SNATCH:
				case FIRST_CJ:
				case GROUP_DONE:
					return null;
				default:
					break;
			}
		}

		LiftDefinition.Stage stage = null;
		if (getCurAthlete() != null) {
			return getCurAthlete().getAttemptsDone() < 3 ? LiftDefinition.Stage.SNATCH : LiftDefinition.Stage.CLEANJERK;
		}

		return stage;
	}

	public List<Athlete> getDisplayOrder() {
		return this.displayOrder;
	}

	public EventForwarder getEventForwarder() {
		return this.eventForwarder;
	}

	public EventBus getEventForwardingBus() {
		return this.eventForwardingBus;
	}

	/**
	 * @return the fopEventBus
	 */
	public EventBus getFopEventBus() {
		return this.fopEventBus;
	}

	/**
	 * @return the goodLift
	 */
	public Boolean getGoodLift() {
		return this.goodLift;
	}

	/**
	 * @return the group
	 */
	public Group getGroup() {
		return this.group;
	}

	public Boolean[] getJuryMemberDecision() {
		return this.juryMemberDecision;
	}

	public List<RecordEvent> getLastChallengedRecords() {
		return this.lastChallengedRecords;
	}

	/**
	 * @return the leaders
	 */
	public List<Athlete> getLeaders() {
		return this.leaders;
	}

	/**
	 * @return the lifters
	 */
	public List<Athlete> getLiftingOrder() {
		return this.liftingOrder;
	}

	/**
	 * @return the liftsDoneAtLastStart
	 */
	public int getLiftsDoneAtLastStart() {
		return this.liftsDoneAtLastStart;
	}

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return this.logger;
	}

	public TreeMap<String, TreeSet<Athlete>> getMedals() {
		return this.medals;
	}

	public MQTTMonitor getMqttMonitor() {
		return MQTTMonitor.getMqttMonitorByName(this.getName());
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	public List<RecordEvent> getNewRecords() {
		return this.newRecords;
	}

	/**
	 * @return the platform
	 */
	public Platform getPlatform() {
		return this.platform;
	}

	/**
	 * @return the previous athlete to have lifted (can be the same as current)
	 */
	public Athlete getPreviousAthlete() {
		return this.previousAthlete;
	}

	public JsonValue getRecordsJson() {
		if (this.recordsJson == null) {
			return Json.createNull();
		}
		return this.recordsJson;
	}

	public Boolean[] getRefereeDecision() {
		return this.refereeDecision;
	}

	public Long[] getRefereeTime() {
		return this.refereeTime;
	}

	public List<Athlete> getResultsOrder() {
		return this.resultsOrder;
	}

	/**
	 * @return the current state
	 */
	public FOPState getState() {
		return this.state;
	}

	/**
	 * @return the time allowed for the next athlete.
	 */
	public int getTimeAllowed() {
		if (this.isForcedTime()) {
			setForcedTime(false);
			int timeRemaining = getAthleteTimer().getTimeRemaining();
			setClockOwnerInitialTimeAllowed(timeRemaining);
			this.logger.debug("{}===== forced time {}", FieldOfPlay.getLoggingName(this), timeRemaining);
			return timeRemaining;
		}
		Athlete a = getCurAthlete();
		int timeAllowed;
		// int clockOwnerInitialTime;

		Athlete owner = getClockOwner();

		if (owner != null && owner.equals(a)) {
			// the clock was started for us. we own the clock, clock is already set to what
			// time was
			// left
			timeAllowed = getAthleteTimer().getTimeRemainingAtLastStop();
		} else if (getPreviousAthlete() != null && getPreviousAthlete().equals(a)) {
			// ** resetDecisions();
			if (owner != null || a.getAttemptNumber() == 1) {
				// clock has started for someone else, one minute
				// first C&J, one minute (doesn't matter who lifted last during snatch)
				timeAllowed = 60000;
			} else {
				timeAllowed = 120000;
			}
			if (owner == null) {
				setClockOwnerInitialTimeAllowed(timeAllowed);
			}
		} else {
			// ** resetDecisions();
			timeAllowed = 60000;
			if (owner == null) {
				setClockOwnerInitialTimeAllowed(timeAllowed);
			}
		}
		// logger.trace("{}==== timeAllowed={} owner={} prev={} cur={}",
		// getLoggingName(), timeAllowed, owner,
		// getPreviousAthlete(), a);
		return timeAllowed;
	}

	/**
	 * @return the bus on which we post commands for the listening browser pages.
	 */
	public EventBus getUiEventBus() {
		return this.uiEventBus;
	}

	public AgeGroup getVideoAgeGroup() {
		return this.videoAgeGroup;
	}

	public Category getVideoCategory() {
		return this.videoCategory;
	}

	public Group getVideoGroup() {
		return this.videoGroup;
	}

	public Integer getWeightAtLastStart() {
		return this.weightAtLastStart;
	}

	/**
	 * Handle field of play events.
	 *
	 * FOP (Field of Play) events inform us of what is happening (e.g. athleteTimer started by timekeeper, decision given by official, etc.) The current state
	 * determines what we do with the event. Typically, we update the state of the field of play (e.g. time is now running) and we issue commands to the
	 * listening user interfaces (e.g. start or stop time being displayed, show the decision, etc.)
	 *
	 * A given user interface will issue a FOP event. This method reacts to the event by updating state, and we issue the resulting user interface commands on
	 * the @link uiEventBus.
	 *
	 * All UIEvents are normally sent by this class, with two exceptions:
	 *
	 * - Timer events are sent by separate Timer objets that implement IProxyTimer. These classes remember the time and broadcast to all listening timers. -
	 * Some UI Classes send UIEvents to themselves, or sub-components, as a way to propagate UI State changes (for example, propagating the reset of decision
	 * lights).
	 *
	 * @param e the event
	 */
	@Subscribe
	public synchronized void handleFOPEvent(FOPEvent e) {
		String stackTrace = e.getStackTrace();
		if (e.getFop() != this) {
			this.logger./**/error("wrong event subscription {} {}\n{}", e, e.getFop(), this, stackTrace);
			return;
			// throw new RuntimeException("wrong event subscription");
		}
		int newHash = e.hashCode();
		if (this.prevHash != null && newHash == this.prevHash) {
			this.logger.debug("{}state {}, DUPLICATE event received {} {} {}", FieldOfPlay.getLoggingName(this),
			        stateName(this.getState()),
			        e, getWhereFrom(stackTrace));
			return;
		} else {
			this.logger.info("{}state {}, event received {} from {}", FieldOfPlay.getLoggingName(this),
			        stateName(this.getState()),
			        e, getWhereFrom(stackTrace));
			this.prevHash = newHash;
		}

		// ======= state-independent processing: the reaction does not depend on the
		// state.

		// it is always possible to explicitly interrupt competition (break between the
		// two lifts, technical incident, etc.). Even switching break type is allowed.

		if (e instanceof FOPEvent.BreakStarted) {
			Object origin = e.getOrigin();
			BreakType requestedBreak = ((FOPEvent.BreakStarted) e).getBreakType();
			boolean allAllowed = origin instanceof AnnouncerContent || origin instanceof TimekeeperContent;

			if (getState() == BREAK
			        && (requestedBreak == BreakType.JURY || requestedBreak == BreakType.CHALLENGE)
			        && (getBreakType() == BreakType.FIRST_CJ || getBreakType() == BreakType.GROUP_DONE)) {
				transitionToBreak((FOPEvent.BreakStarted) e);
				return;
			} else if (getState() == BREAK && getBreakType() != null && getBreakType().isCountdown() && !allAllowed) {
				pushOutUIEvent(new UIEvent.Notification(null, this,
				        UIEvent.Notification.Level.ERROR,
				        "BreakButton.cannotInterruptBreak",
				        3000, this));
				return;
			}
			// exception: wait until a decision has been registered to process jury
			// deliberation.
			if (this.state != DECISION_VISIBLE && this.state != DOWN_SIGNAL_VISIBLE) {
				transitionToBreak((FOPEvent.BreakStarted) e);
			} else {
				this.deferredBreak = e;
				this.logger.info("{}Deferred break", FieldOfPlay.getLoggingName(this));
			}
			return;
		} else if (e instanceof SummonReferee) {
			SummonReferee e2 = (SummonReferee) e;
			if (e2.getRefNumber() == 4) {
				// summoning TC does not trigger a break
				doSummonReferee(e2);
				return;
			} else if (this.state != DECISION_VISIBLE && this.state != DOWN_SIGNAL_VISIBLE) {
				// Summoning a referee must trigger a break if not already in a break
				if (getBreakType() != null) {
					doSummonReferee(e2);
				} else {
					transitionToBreak(
					        new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, null, null, true,
					                e.getOrigin()));
					doSummonReferee(e2);
				}
				return;
			}
			// do not return; error message will be shown if state does not allow summon.
		} else if (e instanceof StartLifting) {
			this.setCeremonyType(null);
			if (this.state == BREAK && (this.breakType == BreakType.JURY || this.breakType == BreakType.TECHNICAL
			        || this.breakType == BreakType.MARSHAL)) {
				if (getGroup() == null) {
					// break took place while inactive
					this.state = INACTIVE;
					pushOutSwitchGroup(this);
				} else {
					// if group under way, this will try to just keep going.
					resumeLifting(e);
				}
				return;
			} else if (this.state == BREAK && (this.breakType == BreakType.GROUP_DONE)) {
				// resume lifting only if current athlete has one more lift to do
				if (getCurAthlete() != null && getCurAthlete().getAttemptsDone() < 6) {
					transitionToLifting(e, getGroup(), true);
				}
				return;
			} else if (this.state == BREAK) {
				// group was not under way when break started, full start.
				transitionToLifting(e, getGroup(), true);
				return;
			} else {
				transitionToLifting(e, getGroup(), true);
				return;
			}
		} else if (e instanceof BarbellOrPlatesChanged) {
			uiShowPlates((BarbellOrPlatesChanged) e);
			return;
		} else if (e instanceof SwitchGroup) {
			this.logger.debug("{}*** switching group", FieldOfPlay.getLoggingName(this));
			Group oldGroup = this.getGroup();
			SwitchGroup switchGroup = (SwitchGroup) e;
			Group newGroup = switchGroup.getGroup();

			boolean inBreak = this.state == BREAK || this.state == INACTIVE;
			if (Objects.equals(oldGroup, newGroup)) {
				this.logger.debug("{}**** reloading", FieldOfPlay.getLoggingName(this));
				loadGroup(newGroup, this, true);
				pushOutSwitchGroup(e.getOrigin());
				uiDisplayCurrentAthleteAndTime(true, e, false);
			} else {
				if (!inBreak) {
					setState(INACTIVE);
					this.athleteTimer.stop();
				} else if (this.state == BREAK && this.breakType == BreakType.GROUP_DONE) {
					setState(INACTIVE);
				} else {
					setState(this.state);
				}
				loadGroup(newGroup, this, true);

				uiDisplayCurrentAthleteAndTime(true, e, false); // ****
			}
			return;
		} else if (e instanceof JuryMemberDecisionUpdate) {
			doJuryMemberDecisionUpdate((JuryMemberDecisionUpdate) e);
			return;
		}

		// ======= state-dependent processing. Depends on the current state.

		switch (this.getState()) {

			case INACTIVE:
				checkDeferredWeightChanges();
				// if (e instanceof TimeStarted) {
				// transitionToTimeRunning();
				// } else
				if (e instanceof WeightChange) {
					doWeightChange((WeightChange) e);
				} else if (e instanceof FOPEvent.CeremonyStarted) {
					getBreakTimer().setIndefinite();
					doStartCeremony((FOPEvent.CeremonyStarted) e);
				} else if (e instanceof FOPEvent.CeremonyDone) {
					doEndCeremony((FOPEvent.CeremonyDone) e);
				} else {
					unexpectedEventInState(e, INACTIVE);
				}
				break;

			case BREAK:
				if (e instanceof FOPEvent.BreakPaused) {
					FOPEvent.BreakPaused bpe = (FOPEvent.BreakPaused) e;
					getBreakTimer().stop();
					getBreakTimer().setTimeRemaining(bpe.getTimeRemaining(), false);
					pushOutUIEvent(new UIEvent.BreakPaused(
					        bpe.getTimeRemaining(),
					        e.getOrigin(),
					        false,
					        this.getBreakType(),
					        this.getCountdownType(), this));
				} else if (e instanceof FOPEvent.BreakDone) {
					pushOutUIEvent(new UIEvent.BreakDone(e.getOrigin(), getBreakType(), this));
					// logger.trace("break done {} {} \n{}", this.getName(), e.getFop().getName(),
					// e.getStackTrace());
					BreakType breakType = getBreakType();
					if (breakType == FIRST_SNATCH || breakType == FIRST_CJ) {
						transitionToLifting(e, getGroup(), false);
					} else if (breakType == BEFORE_INTRODUCTION) {
						transitionToBreak(
						        new FOPEvent.BreakStarted(FIRST_SNATCH, CountdownType.INDEFINITE, null,
						                null, true, this));
						doStartCeremony(
						        new FOPEvent.CeremonyStarted(CeremonyType.INTRODUCTION, getGroup(), null, this));
					} else {
						transitionToLifting(e, getGroup(), false);
					}
				} else if (e instanceof FOPEvent.BreakStarted) {
					transitionToBreak((FOPEvent.BreakStarted) e);
				} else if (e instanceof FOPEvent.CeremonyStarted) {
					doStartCeremony((FOPEvent.CeremonyStarted) e);
				} else if (e instanceof FOPEvent.CeremonyDone) {
					doEndCeremony((FOPEvent.CeremonyDone) e);
				} else if (e instanceof WeightChange) {
					doWeightChange((WeightChange) e);
				} else if (e instanceof JuryMemberDecisionUpdate) {
					doJuryMemberDecisionUpdate((JuryMemberDecisionUpdate) e);
				} else if (e instanceof JuryDecision) {
					doJuryDecision((JuryDecision) e);
				} else if (e instanceof SummonReferee) {
					doSummonReferee((SummonReferee) e);
				} else if (e instanceof DecisionReset) {
					doDecisionReset(e);
				} else {
					unexpectedEventInState(e, BREAK);
				}
				break;

			case CURRENT_ATHLETE_DISPLAYED:
				checkDeferredWeightChanges();
				if (e instanceof TimeStarted) {
					transitionToTimeRunning();
				} else if (e instanceof WeightChange) {
					doWeightChange((WeightChange) e);
				} else if (e instanceof ForceTime) {
					doForceTime((ForceTime) e);
				} else {
					pushOutUIEvent(new UIEvent.Notification(this.getCurAthlete(), e.getOrigin(), e, this.state,
					        UIEvent.Notification.Level.ERROR, this));
					// unexpectedEventInState(e, CURRENT_ATHLETE_DISPLAYED);
				}
				break;

			case TIME_RUNNING:
				checkDeferredWeightChanges();
				if (e instanceof DownSignal) {
					this.logger.debug("emitting down");
					emitDown(e);
				} else if (e instanceof TimeStopped) {
					// athlete lifted the bar
					setState(TIME_STOPPED);
					getAthleteTimer().stop();
					this.logger.info("{}time stopped for {} : {}", FieldOfPlay.getLoggingName(this),
					        getCurAthlete().getShortName(),
					        getAthleteTimer().getTimeRemainingAtLastStop());
				} else if (e instanceof DecisionFullUpdate) {
					// decision board/attempt board sends bulk update
					updateRefereeDecisions((DecisionFullUpdate) e);
					uiShowUpdateOnJuryScreen(e);
				} else if (e instanceof DecisionUpdate) {
					doPossiblySoloRefereeUpdate(e);
				} else if (e instanceof WeightChange) {
					doWeightChange((WeightChange) e);
				} else if (e instanceof ExplicitDecision) {
					simulateDecision((ExplicitDecision) e);
				} else if (e instanceof TimeOver) {
					// athleteTimer got down to 0
					// getTimer() signals this, nothing else required for athleteTimer
					// rule says referees must give reds
					setState(TIME_STOPPED);
				} else if (e instanceof ForceTime) {
					doForceTime((ForceTime) e);
				} else if (e instanceof TimeStarted) {
					if (!getAthleteTimer().isRunning()) {
						// don't start if already running
						getAthleteTimer().start();
					}
					return;
				} else {
					unexpectedEventInState(e, TIME_RUNNING);
				}
				break;

			case TIME_STOPPED:
				checkDeferredWeightChanges();
				if (e instanceof DownSignal) {
					// only occurs if solo referee
					emitDown(e);
				} else if (e instanceof DecisionFullUpdate) {
					// decision coming from decision display or attempt board
					updateRefereeDecisions((DecisionFullUpdate) e);
					uiShowUpdateOnJuryScreen(e);
				} else if (e instanceof DecisionUpdate) {
					doPossiblySoloRefereeUpdate(e);
				} else if (e instanceof TimeStarted) {
					if (!getCurAthlete().equals(getClockOwner())) {
						setClockOwner(getCurAthlete());
						// setClockOwnerInitialTimeAllowed(getTimeAllowed());
					}
					getTimeAllowed();
					prepareDownSignal();
					setWeightAtLastStart();

					// we do not reset decisions or "emitted" flags
					setState(TIME_RUNNING);
					getAthleteTimer().start();
				} else if (e instanceof WeightChange) {
					doWeightChange((WeightChange) e);
				} else if (e instanceof ExplicitDecision) {
					simulateDecision((ExplicitDecision) e);
				} else if (e instanceof ForceTime) {
					getAthleteTimer().setTimeRemaining(((ForceTime) e).timeAllowed, false);
					setState(CURRENT_ATHLETE_DISPLAYED);
				} else if (e instanceof TimeStopped) {
					// ignore duplicate time stopped
				} else if (e instanceof TimeOver) {
					// ignore, already dealt by timer
				} else if (e instanceof StartLifting) {
					// nothing to do, end of break when clock was already started
				} else if (e instanceof ForceTime) {
					doForceTime((ForceTime) e);
				} else {
					unexpectedEventInState(e, TIME_STOPPED);
				}
				break;

			case DOWN_SIGNAL_VISIBLE:
				if (e instanceof ExplicitDecision) {
					simulateDecision((ExplicitDecision) e);
				} else if (e instanceof DecisionFullUpdate) {
					// decision coming from decision display or attempt board
					updateRefereeDecisions((DecisionFullUpdate) e);
					uiShowUpdateOnJuryScreen(e);
				} else if (e instanceof DecisionUpdate) {
					doPossiblySoloRefereeUpdate(e);
				} else if (e instanceof WeightChange) {
					// we need to defer the weight change event until the decision has been shown
					this.logger.debug("{}weight change during down {} {} {}", FieldOfPlay.getLoggingName(this),
					        e.getAthlete(), this.getPreviousAthlete(),
					        this.getCurAthlete());
					this.deferredWeightChanges.add((WeightChange) e);
				} else if (e instanceof TimeStarted) {
					// needed if decision has been given too early (e.g. bar did not reach the knees but reds given)
					restartTimer(e);
				} else {
					unexpectedEventInState(e, DOWN_SIGNAL_VISIBLE);
				}
				break;

			case DECISION_VISIBLE:
				if (e instanceof ExplicitDecision) {
					simulateDecision((ExplicitDecision) e);
					// showExplicitDecision(((ExplicitDecision) e), e.origin);
				} else if (e instanceof DecisionFullUpdate) {
					// late update
					pushOutUIEvent(new UIEvent.Notification(this.getCurAthlete(), e.getOrigin(), e, this.state,
					        UIEvent.Notification.Level.ERROR, this));
				} else if (e instanceof DecisionUpdate) {
					// late update
					pushOutUIEvent(new UIEvent.Notification(this.getCurAthlete(), e.getOrigin(), e, this.state,
					        UIEvent.Notification.Level.ERROR, this));
				} else if (e instanceof WeightChange) {
					// we need to defer the weight change event until the decision has been shown
					this.logger.debug("{}weight change during decision visible {} {} {}",
					        FieldOfPlay.getLoggingName(this), e.getAthlete(),
					        this.getPreviousAthlete(),
					        this.getCurAthlete());
					this.deferredWeightChanges.add((WeightChange) e);
				} else if (e instanceof DecisionReset) {
					// process the deferred weight changes
					if (!this.deferredWeightChanges.isEmpty()) {
						this.logger.debug("{}processing deferred weight changes", FieldOfPlay.getLoggingName(this));
						boolean resultChange = false;
						for (WeightChange wc : this.deferredWeightChanges) {
							resultChange = resultChange || wc.isResultChange();
						}
						recomputeLiftingOrder(true, resultChange);
						this.deferredWeightChanges.clear();
					}
					doDecisionReset(e);
					if (this.deferredBreak != null) {
						transitionToBreak((FOPEvent.BreakStarted) this.deferredBreak);
						this.deferredBreak = null;
					}
				} else {
					unexpectedEventInState(e, DECISION_VISIBLE);
				}
				break;
		}
	}

	private void restartTimer(FOPEvent e) {
		cancelWakeUpRef();
		if (decisionDisplayTimer != null) {
			decisionDisplayTimer.cancel();
		}
		resetDecisions();
		pushOutUIEvent(new UIEvent.DecisionReset(getCurAthlete(), this, this));
		transitionToLifting(e, group, announcerDecisionImmediate);
		fopEventPost(new FOPEvent.TimeStarted(this));
	}

	public void init(List<Athlete> athletes, IProxyTimer timer, IProxyTimer breakTimer, boolean alreadyLoaded) {
		// logger.debug("start of init state={} \\n{}", state, LoggerUtils.
		// stackTrace());
		this.athleteTimer = timer;
		this.athleteTimer.setFop(this);
		this.breakTimer = breakTimer;
		this.breakTimer.setFop(this);
		this.setCurAthlete(null);
		this.setClockOwner(null);
		this.setClockOwnerInitialTimeAllowed(0);
		this.setLiftingOrder(athletes);
		List<AgeGroup> allAgeGroups = AgeGroupRepository.findAgeGroups(getGroup());
		this.ageGroupMap = new LinkedHashMap<>();
		for (AgeGroup ag : allAgeGroups) {
			this.ageGroupMap.put(ag.getCode(), null);
		}
		this.setMedals(new TreeMap<>());
		this.recomputeRecordsMap(athletes);

		boolean done = false;
		for (Athlete a : athletes) {
			a.setFop(this);
		}
		if (athletes != null && athletes.size() > 0) {
			done = recomputeLiftingOrder(true, true);
		}

		// get the correct previous athlete
		LiftOrderReconstruction lor = new LiftOrderReconstruction(this);
		LiftOrderInfo lastLift = lor.getLastLift();
		this.setPreviousAthlete(lastLift != null ? lastLift.getAthlete() : null);

		if (done) {
			pushOutDone();
		} else {
			this.recomputeLeadersAndRecords(athletes);
		}
		if (getGroup() != null) {
		}
		if (this.state == null) {
			this.setState(INACTIVE, LoggerUtils.whereFrom());
		}

		// force a wake up on user interfaces
		if (!alreadyLoaded) {
			this.logger.info("{}group {} athletes={}", FieldOfPlay.getLoggingName(this), getGroup(),
			        athletes != null ? athletes.size() : null);
			pushOutSwitchGroup(this);
		}
	}

	public void initEventBuses() {
		// we listen on this bus, and sometimes post to change our own state
		this.fopEventBus = new EventBus("FOP-" + this.name);

		// we post on these buses

		this.uiEventBus = new AsyncEventBus("UI-" + this.name, new ThreadPoolExecutor(8, Integer.MAX_VALUE,
		        60L, TimeUnit.SECONDS,
		        new SynchronousQueue<>()));
		this.eventForwardingBus = new AsyncEventBus("POST-" + this.name, new ThreadPoolExecutor(1, Integer.MAX_VALUE,
		        60L, TimeUnit.SECONDS,
		        new SynchronousQueue<>()));
	}

	public boolean isAnnouncerDecisionImmediate() {
		return this.announcerDecisionImmediate;
	}

	public boolean isCjStarted() {
		return this.cjStarted;
	}

	public boolean isClockStoppedDecisionsAllowed() {
		return this.clockStoppedDecisionsAllowed;
	}

	public boolean isEmitSoundsOnServer() {
		boolean b = getSoundMixer() != null;
		this.logger.trace("emit sound on server = {}", b);
		return b;
	}

	public boolean isRefereeForcedDecision() {
		return this.refereeForcedDecision;
	}

	public boolean isTestingMode() {
		return this.testingMode;
	}

	public synchronized boolean isTimeoutEmitted() {
		return this.timeoutEmitted;
	}

	/**
	 * all grids get their athletes from this method.
	 *
	 * @param group
	 * @param origin
	 * @param forceLoad reload from database even if current group
	 */
	public synchronized void loadGroup(Group group, Object origin, boolean forceLoad) {
		String thisGroupName = this.getGroup() != null ? this.getGroup().getName() : null;
		String loadGroupName = group != null ? group.getName() : null;

		boolean alreadyLoaded = thisGroupName == loadGroupName;
		this.setPrevWeight(0);
		if (loadGroupName != null && alreadyLoaded && !forceLoad) {
			// already loaded
			this.logger.debug("{}group {} already loaded", FieldOfPlay.getLoggingName(this), loadGroupName);
			return;
		}
		this.setGroup(group);
		this.setCjStarted(false);
		this.cjBreakDisplayed = false;
		resetDecisions();

		if (group != null) {
			// debounce spurious requests due to misconfigured client that would trigger
			// a loadGroup upon receiving a UIEvent.
			long now = System.currentTimeMillis();
			if (!this.testingMode && now - this.lastGroupLoaded < 300) {
				this.logger./**/warn("ignoring request to load group {}", group);
				return;
			}

			if (this.logger.isTraceEnabled()) {
				this.logger.trace("{}**** loading data for group {} [already={} forced={} from={}]",
				        FieldOfPlay.getLoggingName(this),
				        loadGroupName,
				        alreadyLoaded,
				        forceLoad,
				        LoggerUtils.whereFrom());
			}
			List<Athlete> groupAthletes = AthleteRepository.findAllByGroupAndWeighIn(group, true);
			if (groupAthletes.stream().map(Athlete::getStartNumber).anyMatch(sn -> sn == 0)) {
				this.logger./**/warn("start numbers were not assigned correctly");
				AthleteRepository.assignStartNumbers(group);
				groupAthletes = AthleteRepository.findAllByGroupAndWeighIn(group, true);
			}

			init(groupAthletes, this.athleteTimer, this.breakTimer, alreadyLoaded);
			this.lastGroupLoaded = now;
		} else {
			this.logger.debug("{}null group", FieldOfPlay.getLoggingName(this));
			init(new ArrayList<>(), this.athleteTimer, this.breakTimer, alreadyLoaded);
		}
	}

	public boolean recomputeLiftingOrder(boolean currentDisplayAffected, boolean resultChange) {
		Competition comp = Competition.getCurrent();
		// because of recalculation of ranks, if same category is lifting on two FOPs, strange things can happen
		// if done in parallel. Force sequence.
		synchronized (comp) {
			recomputeOrderAndRanks(resultChange);
			if (getCurAthlete() == null) {
				return true;
			}

			int timeAllowed = getTimeAllowed();
			Integer attemptsDone = getCurAthlete().getAttemptsDone();
			this.logger.debug("{}recomputed lifting order curAthlete={} prevlifter={} time={} attemptsDone={} [{}]",
			        FieldOfPlay.getLoggingName(this),
			        getCurAthlete() != null ? getCurAthlete().getFullName() : "",
			        getPreviousAthlete() != null ? getPreviousAthlete().getFullName() : "",
			        timeAllowed,
			        attemptsDone,
			        LoggerUtils.whereFrom());
			if (currentDisplayAffected) {
				getAthleteTimer().setTimeRemaining(timeAllowed, false);
			} else {
				// logger.debug("not affected {}", LoggerUtils.stackTrace());
			}
			// for the purpose of showing team scores, this is good enough.
			// if the current athlete has done all lifts, the group is marked as done.
			// if editing the athlete later gives back an attempt, then the state change
			// will take
			// place and subscribers will revert to current athlete display.
			boolean done = attemptsDone >= 6;
			getGroup().doDone(done);
			return done;
		}
	}

	public void recomputeRecords(Athlete curAthlete) {
		if (curAthlete == null) {
			setRecordsJson(Json.createNull());
			setChallengedRecords(List.of());
			setNewRecords(List.of());
			return;
		}

		Integer request = curAthlete.getNextAttemptRequestedWeight();
		int attemptsDone = curAthlete.getAttemptsDone();
		Integer snatchRequest = attemptsDone < 3 ? request : null;
		Integer cjRequest = attemptsDone >= 3 ? request : null;

		Integer bestSnatch = curAthlete.getBestSnatch();
		Integer totalRequest = attemptsDone >= 3 && bestSnatch != null && bestSnatch > 0 ? (bestSnatch + request)
		        : null;

		List<RecordEvent> eligibleRecords = this.eligibleRecordsByAthlete.get(curAthlete);
		List<RecordEvent> displayableRecords = this.displayableRecordsByAthlete.get(curAthlete);
		boolean showAllFederationRecords = computeShowInformationalRecords(eligibleRecords, displayableRecords);
		boolean showAllCategoryRecords = computeShowAllGroupRecords();
		List<RecordEvent> challengedRecords = RecordFilter.computeChallengedRecords(
		        eligibleRecords,
		        snatchRequest,
		        cjRequest,
		        totalRequest);

		for (RecordEvent gr : this.groupRecords) {
			this.logger.trace("gr: {} {} {}", gr.getAgeGrp(), gr.getRecordName(), gr.getRecordFederation());
		}
		List<RecordEvent> jsonRecords;
		if (showAllFederationRecords && showAllCategoryRecords) {
			jsonRecords = new ArrayList<>(this.groupRecords);
		} else if (showAllCategoryRecords) {
			jsonRecords = RecordFilter.filterEligibleRecordsForAthlete(curAthlete, this.groupRecords);
		} else if (showAllFederationRecords) {
			jsonRecords = displayableRecords;
		} else {
			jsonRecords = eligibleRecords;
		}

		JsonValue recordsJson = null;
		try {
			recordsJson = RecordFilter.buildRecordJson(
			        jsonRecords,
			        new HashSet<>(challengedRecords), snatchRequest, cjRequest,
			        totalRequest, curAthlete);
		} catch (Exception e) {
			// defensive, an error in records processing must not stop competition flow.
			recordsJson = null;
		}
		if (recordsJson == null) {
			setRecordsJson(Json.createNull());
			setChallengedRecords(List.of());
			setNewRecords(List.of());
		} else {
			setRecordsJson(recordsJson);
			setChallengedRecords(challengedRecords);
			for (RecordEvent re : challengedRecords) {
				this.logger.info("challenged record: {}", re);
			}
		}
	}

	public void resetJuryDecisions() {
		setJuryMemberDecision(new Boolean[5]);
		this.juryMemberTime = new Integer[5];
	}

	/**
	 * @param announcerDecisionImmediate the announcerDecisionImmediate to set
	 */
	public void setAnnouncerDecisionImmediate(boolean announcerDecisionImmediate) {
		this.announcerDecisionImmediate = announcerDecisionImmediate;
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
		// logger.debug("****** FOP {} setBreakType {} from {}", System.identityHashCode(this), breakType,
		// LoggerUtils.whereFrom());
		this.breakType = breakType;
	}

	public void setCeremonyType(CeremonyType ceremonyType) {
		this.ceremonyType = ceremonyType;
	}

	public void setChallengedRecords(List<RecordEvent> challengedRecords) {
		this.challengedRecords = challengedRecords;
	}

	public void setCjStarted(boolean cjStarted) {
		this.cjStarted = cjStarted;
	}

	public void setCountdownType(CountdownType countdownType) {
		this.countdownType = countdownType;
	}

	public void setEventForwarder(EventForwarder eventForwarder) {
		this.eventForwarder = eventForwarder;
	}

	/**
	 * Sets the group.
	 *
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	public void setJuryMemberDecision(Boolean[] juryMemberDecision) {
		this.juryMemberDecision = juryMemberDecision;
	}

	/**
	 * @param leaders the leaders to set
	 */
	public void setLeaders(List<Athlete> leaders) {
		this.leaders = leaders;
	}

	/**
	 * @param liftsDoneAtLastStart the liftsDoneAtLastStart to set
	 */
	public void setLiftsDoneAtLastStart(int liftsDoneAtLastStart) {
		this.liftsDoneAtLastStart = liftsDoneAtLastStart;
	}

	public void setMedals(TreeMap<String, TreeSet<Athlete>> medals) {
		this.medals = medals;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void setNewRecords(List<RecordEvent> newRecords) {
		if (newRecords == null || newRecords.isEmpty()) {
//			this.logger.debug("{} + clearing athlete records {}", FieldOfPlay.getLoggingName(this), LoggerUtils.whereFrom());
		}
		this.newRecords = newRecords;
	}

	public void setNextAthlete(Athlete a) {
		this.nextAthlete = a;
	}

	/**
	 * Sets the platform.
	 *
	 * @param platform the platform to set
	 */
	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public void setRecordsJson(JsonValue computedRecords) {
		this.recordsJson = computedRecords;
	}

	public void setRefereeDecision(Boolean[] refereeDecision) {
		this.refereeDecision = refereeDecision;
	}

	public void setRefereeForcedDecision(boolean refereeForcedDecision) {
		this.refereeForcedDecision = refereeForcedDecision;
	}

	public void setRefereeTime(Long[] refereeTime) {
		this.refereeTime = refereeTime;
	}

	/**
	 * @param testingMode true if we don't want wait delays during testing.
	 */
	public void setTestingMode(boolean testingMode) {
		this.testingMode = testingMode;
	}

	public void setVideoAgeGroup(AgeGroup videoAgeGroup) {
		this.videoAgeGroup = videoAgeGroup;
	}

	public void setVideoCategory(Category c) {
		this.videoCategory = c;
	}

	public void setVideoGroup(Group g) {
		this.videoGroup = g;
	}

	public void setWeightAtLastStart(Integer nextAttemptRequestedWeight) {
		this.weightAtLastStart = nextAttemptRequestedWeight;
		setLiftsDoneAtLastStart(((getCurAthlete() != null) ? getCurAthlete().getAttemptsDone() : 0));
	}

	/**
	 * Set up a group. Used for tests and simulation only.
	 *
	 */
	public void testBefore() {
		if (OwlcmsSession.getFop() == null) {
			OwlcmsSession.setFop(this);
		}
		setWeightAtLastStart(0);
		testStartLifting(null, null);
	}

	/**
	 * Start a group. Used for tests and simulation only.
	 *
	 * @param group the group
	 */
	public void testStartLifting(Group group, Object origin) {
		loadGroup(group, origin, true);
		this.logger.trace("{} start lifting for group {} origin={}", FieldOfPlay.getLoggingName(this),
		        (group != null ? group.getName() : group), origin);
		// intentionally posting an event for testing purposes
		fopEventPost(new StartLifting(origin));
	}

	@Override
	public void unregister() {
		MQTTMonitor mqttMonitor2 = this.getMqttMonitor();
		this.logger.debug("{}unregistering event forwarder and mqtt monitor {}", getLoggingName(this),
		        System.identityHashCode(mqttMonitor2));
		this.fopEventBus.unregister(this);
		if (this.getEventForwarder() != null) {
			this.getEventForwarder().unregister();
			this.setEventForwarder(null);
		}
		if (mqttMonitor2 != null) {
			mqttMonitor2.unregister();
		}
	}

	void emitFinalWarning() {
		if (!isFinalWarningEmitted()) {
			this.logger.info("{}Final Warning", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				finalWarningSound = new Sound(getSoundMixer(), "finalWarning.wav");
				finalWarningSound.emit();
			}
			pushOutUIEvent(new UIEvent.TimeRemaining(this, 30, this));
			setFinalWarningEmitted(true);
		}
	}

	void emitInitialWarning() {
		if (!isInitialWarningEmitted()) {
			this.logger.info("{}Initial Warning", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				initialWarningSound = new Sound(getSoundMixer(), "initialWarning.wav");
				initialWarningSound.emit();
			}
			pushOutUIEvent(new UIEvent.TimeRemaining(this, 90, this));
			setInitialWarningEmitted(true);
		}
	}

	void emitTimeOver() {
		if (!isTimeoutEmitted()) {
			this.logger.info("{}Time Over", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				timeOverSound = new Sound(getSoundMixer(), "timeOver.wav");
				timeOverSound.emit();

			}
			pushOutUIEvent(new UIEvent.TimeRemaining(this, 0, this));
			setTimeoutEmitted(true);
		}
	}

	public void pushOutUIEvent(UIEvent event) {
		// logger.debug("!!!! {}",event);
		getUiEventBus().post(event);
		getEventForwardingBus().post(event);
	}

	/**
	 * Sets the state.
	 *
	 * @param state the new state
	 */
	void setState(FOPState state) {
		this.logger.info("{}entering {} {}", FieldOfPlay.getLoggingName(this), stateName(state),
		        LoggerUtils.whereFrom());
		doSetState(state);
	}

	void setState(FOPState state, String whereFrom) {
		this.logger.info("{}entering {} {}", FieldOfPlay.getLoggingName(this), stateName(state), whereFrom);
		doSetState(state);
	}

	private boolean allFirstCJ() {
		if (this.liftingOrder == null || this.liftingOrder.isEmpty()) {
			return false;
		}
		// check that all athletes are at first CJ or have withdrawn
		boolean firstCJ = true;
		for (Athlete a : this.liftingOrder) {
			Integer firstCJValue = a.getCleanJerk1AsInteger();
			this.logger.trace("{} {} {}", a.getShortName(), a.getAttemptsDone(), a.getCleanJerk1AsInteger());
			if (a.getAttemptsDone() != 3 && (firstCJValue == null || (firstCJValue != null && firstCJValue != 0))) {
				// 0 means athlete withdrew
				this.logger.trace("no break because of {}", a.getShortName());
				firstCJ = false;
				break;
			}
		}
		return firstCJ;
	}

	private void cancelWakeUpRef() {
		if (this.wakeUpRef != null) {
			this.wakeUpRef.interrupt();
		}
		this.wakeUpRef = null;
	}

	private void checkDeferredWeightChanges() {
		if (!this.deferredWeightChanges.isEmpty()) {
			// decision reset did not happen, we have pending weight changes.
			this.logger.error("*** Can't happen: Weight changes during down/decision display, but no decision reset");
			recomputeLiftingOrder(true, true);
		}
	}

	private Ranking computeResultOrderRanking(boolean groupDone) {
		boolean _3medals = Competition.getCurrent().isSnatchCJTotalMedals();
		if (groupDone || (isCjStarted() && !_3medals)) {
			return Ranking.TOTAL;
		} else if (_3medals && isCjStarted()) {
			return Ranking.CLEANJERK;
		} else {
			return Ranking.SNATCH;
		}
	}

	private boolean computeShowInformationalRecords(List<RecordEvent> eligibleRecords,
	        List<RecordEvent> displayableRecords) {
		boolean computeShowInformationalRecords = computeShowInformationalRecords();
		// if (computeShowInformationalRecords) {
		// logger.debug(" ---- displayableRecords {} {}", computeShowInformationalRecords,
		// displayableRecords.size());
		// for (RecordEvent rec : displayableRecords) {
		// logger.debug("displayableRecord {}", rec);
		// }
		// } else {
		// logger.debug(" ---- eligibleRecords {} {}", computeShowInformationalRecords,
		// eligibleRecords.size());
		// for (RecordEvent rec : eligibleRecords) {
		// logger.debug("eligibleRecord {}", rec);
		// }
		// }
		return computeShowInformationalRecords;
	}

	private void createNewRecordEvent(Athlete a, List<RecordEvent> newRecords, RecordEvent rec, Double value) {
		RecordEvent newRecord = RecordEvent.newRecord(a, rec, value, this.getGroup());
		newRecords.add(newRecord);
	}

	private void doDecisionReset(FOPEvent e) {
		this.logger.debug("{}clearing decision lights", FieldOfPlay.getLoggingName(this));
		// the state will be rewritten in displayOrBreakIfDone
		// this is so the decision reset knows that the decision is no longer displayed.
		cancelWakeUpRef();
		pushOutUIEvent(new UIEvent.DecisionReset(getCurAthlete(), this, this));
		setClockOwner(null);
		// MUST NOT change setClockOwnerInitialTimeAllowed
		setNewRecords(List.of());

		if (getCurAthlete() != null && getCurAthlete().getAttemptsDone() < 6) {
			// Always change state first, then UI update.
			setState(CURRENT_ATHLETE_DISPLAYED);
			uiDisplayCurrentAthleteAndTime(true, e, false);
		} else {
			// special kind of break that allows moving back in case of jury reversal
			pushOutDone();
		}
	}

	private void doEndCeremony(CeremonyDone e) {
		setCeremonyType(null);
		pushOutUIEvent(new UIEvent.CeremonyDone(e.getCeremonyType(), e, this));
	}

	private void doForceTime(FOPEvent.ForceTime e) {
		// need to set time
		int ta = e.timeAllowed;
		this.logger.debug("{}forcing time to {}", FieldOfPlay.getLoggingName(this), ta);
		getAthleteTimer().stop();
		getAthleteTimer().setTimeRemaining(ta, false);
		getAthleteTimer().stop();
		setClockOwnerInitialTimeAllowed(ta);
		setForcedTime(true);
		setState(CURRENT_ATHLETE_DISPLAYED);
	}

	private void doJuryDecision(JuryDecision e) {
		Athlete a = e.getAthlete();
		Integer actualLift = a.getActualLift(a.getAttemptsDone());
		if (actualLift != null) {
			Integer curValue = Math.abs(actualLift);

			boolean reversalToGood = e.success && actualLift <= 0;
			boolean reversalToBad = !e.success && actualLift > 0;
			boolean newRecord = e.success && getLastChallengedRecords() != null
			        && !getLastChallengedRecords().isEmpty();

			boolean waitForAnnouncer = Competition.getCurrent().isAnnouncerControlledJuryDecision() && e.isJuryButton();
			JuryNotification juryNotificationEvent = new UIEvent.JuryNotification(a, e.getOrigin(),
			        e.success ? JuryDeliberationEventType.GOOD_LIFT : JuryDeliberationEventType.BAD_LIFT,
			        reversalToGood || reversalToBad, newRecord,
			        waitForAnnouncer, this);

			if (waitForAnnouncer) {
				// we will get a second JuryDecision event, coming this time from the announcer
				// we postpone processing until then
				this.toBeAnnouncedJuryDecision = e;
				pushOutUIEvent(juryNotificationEvent);
				return;
			}

			if (this.toBeAnnouncedJuryDecision != null) {
				// we are in announcer-controlled mode, and when the jury pressed the button,
				// we stored their decision.
				// now we have received a second jurydecision event, this time from the announcer
				// that indicates that the stored decision has been announced and must be processed.
				e = this.toBeAnnouncedJuryDecision;
				this.toBeAnnouncedJuryDecision = null;
			} else {
				// we are in immediate mode. e is the jury decision to be processed.
				// nothing to do.
			}

			// must set state before recomputing order so that scoreboards stop blinking the
			// current athlete
			// must also set state prior to sending event, so that state monitor shows new
			// state.
			setGoodLift(e.success);
			setState(DECISION_VISIBLE);
			pushOutUIEvent(juryNotificationEvent);
			a.doLift(a.getAttemptsDone(), e.success ? Integer.toString(curValue) : Integer.toString(-curValue));
			AthleteRepository.save(a);

			// reversal from bad to good should add records
			// reversal from good to bad must remove records
			setNewRecords(updateRecords(a, e.success, getLastChallengedRecords(), getLastNewRecords()));

			recomputeLiftingOrder(true, true);

			// tell ourself to reset after 3 secs.
			new DelayTimer(isTestingMode()).schedule(() -> {
				// fopEventPost(new DecisionReset(this));
				if (reversalToGood) {
					notifyRecords(this.newRecords, true);
					setLastNewRecords(getNewRecords());
				}
				fopEventPost(new StartLifting(this));
			}, DECISION_VISIBLE_DURATION);

		}
	}

	private void doJuryMemberDecisionUpdate(FOPEvent.JuryMemberDecisionUpdate e) {
		getJuryMemberDecision()[e.refIndex] = e.decision;
		this.juryMemberTime[e.refIndex] = 0;
		processJuryMemberDecisions(e.origin, e.refIndex);
	}

	private void doPossiblySoloRefereeUpdate(FOPEvent e) {
		// logger.debug("===== doPossiblySoloRefereeUpdate {}", isSingleReferee());
		if (isSingleReferee() || ((DecisionUpdate) e).getRefIndex() < 0) {
			boolean goodLift = ((DecisionUpdate) e).isDecision();
			simulateDecision(new ExplicitDecision(e.getAthlete(), e.getStackTrace(), isAnnouncerDecisionImmediate(),
			        goodLift, goodLift, goodLift));
		} else {
			updateRefereeDecisions((DecisionUpdate) e);
			uiShowUpdateOnJuryScreen(e);
		}
	}

	public boolean isSingleReferee() {
		return this.singleReferee;
	}

	public void setSingleReferee(boolean solo) {
		// logger.debug("===== set single referee {}",solo);
		this.singleReferee = solo;
	}

	private void doSetState(FOPState state) {
		if (state == CURRENT_ATHLETE_DISPLAYED) {
			Athlete a = getCurAthlete();
			if (getGroup() != null) {
				boolean lastLiftDone = a == null || a.getAttemptsDone() >= 6;
				getGroup().doDone(lastLiftDone);
				// special case for 0 on last lift, there willl be no decision, group is really
				// done
				if (lastLiftDone && a != null && a.getActualLift(6) == 0) {
					state = BREAK;
					this.breakType = BreakType.GROUP_DONE;
				}
			}
		} else if (state == BREAK && getGroup() != null) {
			getGroup().doDone(this.breakType == BreakType.GROUP_DONE);
		}
		this.state = state;
	}

	private void doStartCeremony(CeremonyStarted e) {
		setCeremonyType(e.getCeremony());
		setVideoGroup(e.getCeremonyGroup());
		setVideoCategory(e.getCeremonyCategory());
		pushOutUIEvent(new UIEvent.CeremonyStarted(e.getCeremony(), e.getCeremonyGroup(), e.getCeremonyCategory(),
		        e.getStackTrace(), e.getOrigin(), this));
	}

	private void doSummonReferee(SummonReferee e) {
		if (e.getRefNumber() >= 4) {
			JuryNotification event = new UIEvent.JuryNotification(null, e.getOrigin(),
			        JuryDeliberationEventType.CALL_TECHNICAL_CONTROLLER, null, null, false, this);
			pushOutUIEvent(event);
		} else {
			JuryNotification event = new UIEvent.JuryNotification(null, this, JuryDeliberationEventType.CALL_REFEREES,
			        null, null, false, this);
			pushOutUIEvent(event);
		}
		pushOutUIEvent(new UIEvent.SummonRef(e.getRefNumber(), true, this, this));
	}

	private void doTONotifications(BreakType newBreak) {
		// logger.debug("doTONotifications {}\n{}", newBreak, LoggerUtils.stackTrace());
		if (newBreak == null) {
			// resuming
			if (this.state == BREAK) {
				switch (this.breakType) {
					case JURY:
					case MARSHAL:
					case TECHNICAL:
						pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
						        JuryDeliberationEventType.END_JURY_BREAK, null, null, false, this));
						break;
					case CHALLENGE:
						pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
						        JuryDeliberationEventType.END_CHALLENGE, null, null, false, this));
					default:
						break;
				}
			}
		} else {
			switch (newBreak) {
				case JURY:
					resetJuryDecisions();
					pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
					        JuryDeliberationEventType.START_DELIBERATION, null, null, false, this));
					break;
				case MARSHAL:
					pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
					        JuryDeliberationEventType.MARSHALL, null, null, false, this));
					break;
				case TECHNICAL:
					pushOutUIEvent(new UIEvent.JuryNotification(null, this,
					        JuryDeliberationEventType.TECHNICAL_PAUSE, null, null, false, this));
					break;
				case CHALLENGE:
					resetJuryDecisions();
					pushOutUIEvent(new UIEvent.JuryNotification(null, this,
					        JuryDeliberationEventType.CHALLENGE, null, null, false, this));
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Perform weight change and adjust state.
	 *
	 * If the clock was started and we come back to the clock owner, we set the state to TIME_STARTED If in a break, we are careful not to update, unless the
	 * change causes an exit from the break (e.g. jury overrule on last lift) Otherwise we update the displays.
	 *
	 * @param wc
	 */
	private void doWeightChange(WeightChange wc) {
		long start = System.nanoTime();
		boolean resultChange = wc.isResultChange();
		String reason = "";
		Athlete changingAthlete = wc.getAthlete();

		Integer newWeight = changingAthlete.getNextAttemptRequestedWeight();
		// logger.debug("&&1 cur={} curWeight={} changing={} newWeight={}", getCurAthlete(), curWeight, changingAthlete,
		// newWeight);
		// logger.debug("&&2 clockOwner={} clockLastStopped={} state={}", getClockOwner(),
		// getAthleteTimer().getTimeRemainingAtLastStop(), state);

		boolean stopAthleteTimer = false;
		if (getClockOwner() != null && getAthleteTimer().isRunning()) {
			// time is running
			if (changingAthlete.equals(getClockOwner())) {
				reason = "1";
				// logger.trace("&&3.A clock IS running for changing athlete {}",
				// changingAthlete);
				// X is the current lifter
				// if a real change (and not simply a declaration that does not change weight),
				// make sure clock is stopped.
				if (this.curWeight != newWeight) {
					reason = "2";
					// logger.trace("&&3.A.A1 weight change for clock owner: clock running: stop
					// clock");
					getAthleteTimer().stop(); // memorize time
					stopAthleteTimer = true; // make sure we broacast to clients
					doWeightChange(wc, changingAthlete, getClockOwner(), stopAthleteTimer);
				} else {
					reason = "3";
					// logger.trace("&&3.A.B declaration at same weight for clock owner: leave clock
					// running");
					// no actual weight change. this is most likely a declaration.
					// we do the call to trigger a notification on official's screens, but request
					// that the clock keep running
					doWeightChange(wc, changingAthlete, getClockOwner(), false);
					// return;
				}
			} else {
				reason = "4";
				// logger.trace("&&3.B clock running, but NOT for changing athlete, do not
				// update attempt board");
				weightChangeDoNotDisturb(wc);
				// return;
			}
		} else if (getClockOwner() != null && !getAthleteTimer().isRunning()) {
			reason = "5";
			// logger.trace("&&3.B clock NOT running for changing athlete {}",
			// changingAthlete);
			// time was started (there is an owner) but is not currently running
			// time was likely stopped by timekeeper because coach signaled change of weight
			doWeightChange(wc, changingAthlete, getClockOwner(), true);
		} else {
			reason = "6";
			// logger.trace("&&3.C1 no clock owner, time is not running");
			// time is not running
			recomputeLiftingOrder(true, wc.isResultChange());

			setStateUnlessInBreak(CURRENT_ATHLETE_DISPLAYED);
			// logger.trace("&&3.C2 displaying, curAthlete={}, state={}", getCurAthlete(), state);

			// send an update even in a break (announcer/marshall need to refresh)
			uiDisplayCurrentAthleteAndTime(true, wc, false);
		}
		if (this.timingLogger.isDebugEnabled()) {
			this.timingLogger.debug("{}*** doWeightChange {} {} {}", FieldOfPlay.getLoggingName(this),
			        (System.nanoTime() - start) / 1000000.0, resultChange, reason);
		}
	}

	private void doWeightChange(WeightChange wc, Athlete changingAthlete, Athlete clockOwner,
	        boolean currentDisplayAffected) {
		setForcedTime(false);
		recomputeLiftingOrder(currentDisplayAffected, wc.isResultChange());
		// if the currentAthlete owns the clock, then the next ui update will show the
		// correct athlete and
		// the time needs to be restarted (state = TIME_STOPPED). Going to TIME_STOPPED
		// allows the decision to register if the announcer
		// forgets to start time.

		// otherwise we need to announce new athlete (state = CURRENT_ATHLETE_DISPLAYED)

		FOPState newState = this.state;
		if (clockOwner.equals(getCurAthlete()) && currentDisplayAffected) {
			newState = TIME_STOPPED;
		} else if (currentDisplayAffected) {
			newState = CURRENT_ATHLETE_DISPLAYED;
		}
		this.logger.trace("&&3.X change for {}, new cur = {}, displayAffected = {}, switching to {}", changingAthlete,
		        getCurAthlete(), currentDisplayAffected, newState);
		setStateUnlessInBreak(newState);
		uiDisplayCurrentAthleteAndTime(currentDisplayAffected, wc, false);
	}

	private void emitDown(FOPEvent e) {
		this.logger.debug("{}Emitting down {}", FieldOfPlay.getLoggingName(this), LoggerUtils.whereFrom(2));
		getAthleteTimer().stop(); // paranoia
		this.setPreviousAthlete(getCurAthlete()); // would be safer to use past lifting order
		setClockOwner(null); // athlete has lifted, time does not keep running for them
		setClockOwnerInitialTimeAllowed(0);
		uiShowDownSignalOnSlaveDisplays(e.origin);
		setState(DOWN_SIGNAL_VISIBLE);
	}

	private List<RecordEvent> getLastNewRecords() {
		return this.lastNewRecords;
	}

	public Athlete getNextAthlete() {
		return this.nextAthlete;
	}

	private int getPrevWeight() {
		return this.prevWeight;
	}

	private Mixer getSoundMixer() {
		Platform platform2 = getPlatform();
		return platform2 == null ? null : platform2.getMixer();
	}

	private String getWhereFrom(String stackTrace) {
		if (stackTrace != null) {
			try {
				String sep = System.lineSeparator();
				int start = stackTrace.indexOf(sep);
				start = stackTrace.indexOf(sep, start + 1);
				start = stackTrace.indexOf(sep, start + 1);
				return stackTrace.substring(start + 3, stackTrace.indexOf(sep, start + 1));
			} catch (Exception e) {
				return "?";
			}
		} else {
			return "?";
		}
	}

	private boolean isDecisionDisplayScheduled() {
		return this.decisionDisplayScheduled;
	}

	private synchronized boolean isDownEmitted() {
		return this.downEmitted;
	}

	private synchronized boolean isFinalWarningEmitted() {
		return this.finalWarningEmitted;
	}

	private boolean isForcedTime() {
		return this.forcedTime;
	}

	private synchronized boolean isInitialWarningEmitted() {
		return this.initialWarningEmitted;
	}

	private void notifyRecords(List<RecordEvent> newRecords, boolean newRecord) {
		if (newRecords == null) {
			return;
		}
		for (RecordEvent rec : newRecords) {
			pushOutUIEvent(
			        new UIEvent.Notification(
			                this.getCurAthlete(),
			                this,
			                newRecord ? UIEvent.Notification.Level.SUCCESS : UIEvent.Notification.Level.INFO,
			                newRecord ? "Record.NewNotification" : "Record.AttemptNotification",
			                3 * UIEvent.Notification.NORMAL_DURATION,
			                this,
			                rec.getRecordName(),
			                Translator.translate("Record." + rec.getRecordLift().name()),
			                rec.getAgeGrp(),
			                rec.getBwCatString(),
			                Long.toString(Math.round(rec.getRecordValue()))));
		}
	}

	private void prepareDownSignal() {
		if (isEmitSoundsOnServer()) {
			try {
				this.downSignal = new Tone(getSoundMixer(), 1100, 1200, 1.0);
			} catch (IllegalArgumentException | LineUnavailableException e) {
				this.logger.error("{}\n{}", e.getCause(), LoggerUtils./**/stackTrace(e));
				broadcast("SoundSystemProblem");
			}
		}
	}

	/**
	 * events resulting from decisions received so far (down signal, stopping timer, all decisions entered, etc.)
	 *
	 * @param refIndex
	 */
	private void processJuryMemberDecisions(Object origin, int refIndex) {
		this.logger.debug("*** process jury member decisions {} {} {} {}", Competition.getCurrent().getJurySize(),
		        getJuryMemberDecision()[0], getJuryMemberDecision()[1], getJuryMemberDecision()[2]);
		int jurySize = Competition.getCurrent().getJurySize();
		showJuryMemberDecisionReceived(this, refIndex, getJuryMemberDecision(), jurySize);
		int nbRed = 0;
		int nbWhite = 0;
		int nbDecisions = 0;

		for (int i = 0; i < jurySize; i++) {
			if (getJuryMemberDecision()[i] != null) {
				if (getJuryMemberDecision()[i]) {
					nbWhite++;
				} else {
					nbRed++;
				}
				nbDecisions++;
			}
		}
		final int reds = nbRed;
		final int whites = nbWhite;
		if (nbDecisions == jurySize) {
			new Thread(() -> {
				try {
					// make sure all greens are shown before showing decisions.
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
				showJuryMemberDecisionsNow(origin, (reds == jurySize || whites == jurySize), jurySize,
				        getJuryMemberDecision());
			}).start();
		}
	}

	/**
	 * events resulting from decisions received so far (down signal, stopping timer, all decisions entered, etc.)
	 */
	private void processRefereeDecisions(FOPEvent e) {
		// logger.debug("*** process referee decisions");
		int nbRed = 0;
		int nbWhite = 0;
		int nbDecisions = 0;
		for (int i = 0; i < 3; i++) {
			if (getRefereeDecision()[i] != null) {
				if (getRefereeDecision()[i]) {
					nbWhite++;
				} else {
					nbRed++;
				}
				nbDecisions++;
			}
		}
		setGoodLift(null);
		if (nbWhite >= 2 || nbRed >= 2) {
			if (!this.downEmitted) {
				emitDown(e);
				this.downEmitted = true;
			}
		}
		if (nbDecisions == 2) {
			// 2 decisions, reminder for last referee
			this.wakeUpRef = new Thread(() -> {
				int lastRef = -1;
				try {
					// wait a bit. If the decision comes in while waiting, this thread will be
					// cancelled anyway
					Thread.sleep(Competition.getCurrent().getRefereeWakeUpDelay());
					lastRef = ArrayUtils.indexOf(getRefereeDecision(), null);
					if (lastRef != -1 && !Thread.currentThread().isInterrupted()) {
						// logger.debug("posting");
						this.uiEventBus.post(new UIEvent.WakeUpRef(lastRef + 1, true, this, this));
					} else {
						// logger.debug("not posting");
					}
					Thread.sleep(WAKEUP_DURATION_MS);
				} catch (InterruptedException e1) {
					// ignore interruption, finally handles clean up
				} finally {
					// if we are here, either the last ref has entered a decision, or we've
					// exhausted the reminder
					// duration
					// in either case, we turn the reminder off.
					if (lastRef != -1) {
						this.uiEventBus.post(new UIEvent.WakeUpRef(lastRef + 1, false, this, this));
					}
				}
			});
			this.wakeUpRef.start();
		}
		if (nbDecisions == 3) {
			if (this.wakeUpRef != null) {
				cancelWakeUpRef();
			}
			setGoodLift(nbWhite >= 2);
			// logger.debug("*** 3 decisions");
			if (!isDecisionDisplayScheduled()) {
				// logger.debug("*** not scheduled");
				if (e instanceof FOPEvent.DecisionFullUpdate) {
					if (((FOPEvent.DecisionFullUpdate) e).isImmediate()) {
						// logger.debug("*** is Immediate, full update NOW");
						showDecisionNow(e.getOrigin());
					} else {
						// logger.debug("*** NOT immediate, full update scheduling");
						showDecisionAfterDelay(e.getOrigin(), REVERSAL_DELAY);
					}
				} else {
					// logger.debug("*** partial update scheduling");
					showDecisionAfterDelay(this, REVERSAL_DELAY);
				}
			} else {
				// logger.debug("*** already scheduled");
			}
		}
	}

	private void pushOutDone() {
		this.logger.debug("{} *** group {} done", FieldOfPlay.getLoggingName(this), getGroup());
		UIEvent.GroupDone event = new UIEvent.GroupDone(this.getGroup(), null, LoggerUtils.whereFrom(), this);
		// make sure the publicresults update carries the right state.
		this.setBreakType(BreakType.GROUP_DONE);
		this.getBreakTimer().setIndefinite();
		this.setState(BREAK);
		// for 3medals, make sure that the results show total and no longer cj ranking.
		setResultsOrder(AthleteSorter.resultsOrderCopy(getDisplayOrder(),
		        computeResultOrderRanking(true)));
		pushOutUIEvent(event);
	}

	private void pushOutSnatchDone() {
		Competition cCur = Competition.getCurrent();
		this.logger.debug("Snatch Done {}", cCur.isAutomaticCJBreak());
		if (!cCur.isAutomaticCJBreak()) {
			return;
		}

		int millisRemaining;
		if (Competition.getCurrent().isSimulation()) {
			millisRemaining = 10 * 1000;
		} else {
			millisRemaining = 10 * 60 * 1000;
			if (cCur.getShorterBreakMin() != null && this.liftingOrder.size() > cCur.getShorterBreakMin()) {
				millisRemaining = (cCur.getShorterBreakDuration() != null ? cCur.getShorterBreakDuration() : 10) * 60
				        * 1000;
			} else if (cCur.getLongerBreakMax() != null && this.liftingOrder.size() < cCur.getLongerBreakMax()) {
				millisRemaining = (cCur.getLongerBreakDuration() != null ? cCur.getLongerBreakDuration() : 10) * 60
				        * 1000;
			}
			if (millisRemaining <= 0) {
				return;
			}
		}

		if (this.state == BREAK && getBreakType() == FIRST_CJ) {
			// already in the break
			return;
		}

		this.logger.debug("{}group {} snatch done, break duration {}s", FieldOfPlay.getLoggingName(this), getGroup(),
		        millisRemaining / 1000);

		int timeRemaining = millisRemaining;
		this.setBreakType(BreakType.FIRST_CJ);
		this.getBreakTimer().setTimeRemaining(timeRemaining, false);
		this.getBreakTimer().setBreakDuration(timeRemaining);
		this.getBreakTimer().setEnd(null);

		// this actually starts the break.
		this.setState(BREAK); // break must be set before start
		this.getBreakTimer().start();

		// the event forces the other UIs to take notice.
		BreakStarted event = new UIEvent.BreakStarted(millisRemaining, this, false, BreakType.FIRST_CJ,
		        CountdownType.DURATION, LoggerUtils.stackTrace(), false, this);
		// logger.debug("BreakStarted UI {} ",event, event.getBreakType());
		pushOutUIEvent(event);
	}

	private void pushOutStartLifting(Group group2, Object origin) {
		pushOutUIEvent(new UIEvent.StartLifting(group2, origin, this));
	}

	private void pushOutSwitchGroup(Object origin) {
		pushOutUIEvent(new UIEvent.SwitchGroup(this.getGroup(), this.getState(), this.getCurAthlete(),
		        origin, this));
	}

	/**
	 * Compute the current leaders that match the Athlete's registration category.
	 *
	 * Assume 16-year old Youth Lifter Y is eligible for Youth, Junior, Senior
	 *
	 * If she is lifting, we show youth lifter rankings, and include her if in the top 3 youth. If a Junior is lifting, Y needs to be ranked as a junior, and
	 * include her if in top 3 juniors If a Senior is lifting, Y needs to be ranked as a senior, and include her if in top 3 seniors
	 *
	 * So we need to fetch the PAthlete that reflects each athlete's participation in the current lifter's registration category. Ouch.
	 *
	 * @param rankedAthletes
	 */
	private void recomputeCurrentLeaders(List<Athlete> rankedAthletes) {
		if (rankedAthletes == null || rankedAthletes.size() == 0) {
			setLeaders(null);
			return;
		}

		if (getCurAthlete() != null && getCurAthlete().getAgeGroup() != null && getCurAthlete().getAgeGroup().getComputedScoringSystem() != null) {
			// compute leaders according to score.
			Category category = getCurAthlete().getCategory();
			TreeSet<Athlete> medalists = getMedals().get(category.getCode());
			List<Athlete> scoreMedalists = medalists.stream().filter(a -> {
				int r = a.getCustomRank();
				return r <= 3 && r > 0;
			}).collect(Collectors.toList());
			// logger.debug("total medalists {}", totalMedalists);
			setLeaders(scoreMedalists);
		} else if (getCurAthlete() != null) {
			Category category = getCurAthlete().getCategory();

			TreeSet<Athlete> medalists = getMedals().get(category.getCode());
			// logger.debug("medals {}", medalists);
			List<Athlete> snatchMedalists = medalists.stream().filter(a -> {
				int r = a.getSnatchRank();
				return r <= 3 && r > 0;
			}).sorted((a, b) -> ObjectUtils.compare(a.getSnatchRank(), b.getSnatchRank())).collect(Collectors.toList());
			// logger.debug("snatch medalists {}", snatchMedalists);
			List<Athlete> totalMedalists = medalists.stream().filter(a -> {
				int r = a.getTotalRank();
				return r <= 3 && r > 0;
			}).collect(Collectors.toList());
			// logger.debug("total medalists {}", totalMedalists);

			if (!isCjStarted()) {
				setLeaders(snatchMedalists);
			} else {
				if (totalMedalists.size() > 0) {
					setLeaders(totalMedalists);
				} else {
					setLeaders(snatchMedalists);
				}

			}
		} else {
			setLeaders(null);
		}
	}

	private void recomputeLeadersAndRecords(List<Athlete> athletes) {
		recomputeCurrentLeaders(athletes);
		recomputeRecords(getCurAthlete());
	}

	/**
	 * Recompute lifting order, category ranks, and leaders for current category. Sets rankings including for previous lifters for all categories in the current
	 * group.
	 *
	 * @param recomputeCategoryRanks true if a result has changed and ranks need to be recomputed
	 */
	private void recomputeOrderAndRanks(boolean recomputeCategoryRanks) {
		Group g = getGroup();
		List<Athlete> athletes;

		long startAssignRanks = System.nanoTime();
		long endAssignRanks = 0;
		long endMedals = 0;
		long endDisplayOrder = 0;
		long endLeaders = 0;

		logger.debug("{}recompute ranks recomputeCategoryRanks={} [{}]", FieldOfPlay.getLoggingName(this),
		        recomputeCategoryRanks, LoggerUtils.whereFrom());
		if (recomputeCategoryRanks) {
			// we update the ranks all athletes in our category, as well as the current scoring system
			athletes = JPAService.runInTransaction(em -> {
				List<Athlete> l = AthleteSorter.assignCategoryRanks(em, g);
				List<Athlete> nl = updateScoringSystemRanking(em, l);
				return nl;
			});
		} else {
			// only recompute the current scoring system
			athletes = JPAService.runInTransaction(em -> {
				List<Athlete> l = AthleteRepository.findAthletesForGlobalRanking(em, g);
				List<Athlete> nl = updateScoringSystemRanking(em, l);
				return nl;
			});
		}
		endAssignRanks = System.nanoTime();

		if (athletes == null) {
			setDisplayOrder(null);
			setCurAthlete(null);
			setNextAthlete(null);
			recomputeRecords(null);
		} else {
			if (recomputeCategoryRanks) {
				setMedals(Competition.getCurrent().computeMedals(g, athletes));
			}
			endMedals = System.nanoTime();

			List<Athlete> currentGroupAthletes = AthleteSorter.displayOrderCopy(athletes).stream()
			        .filter(a -> a.getGroup() != null ? a.getGroup().equals(g) : false)
			        .peek(a -> {
				        if (a.getAttemptsDone() > 3 && !isCjStarted()) {
					        this.logger.trace("set cj started");
					        // known side-effect
					        setCjStarted(true);
				        }
			        })
			        .collect(Collectors.toList());

			setDisplayOrder(currentGroupAthletes);
			setLiftingOrder(AthleteSorter.liftingOrderCopy(currentGroupAthletes));
			boolean groupDone = this.curAthlete != null && this.curAthlete.getAttemptsDone() >= 6;
			setResultsOrder(AthleteSorter.resultsOrderCopy(currentGroupAthletes,
			        computeResultOrderRanking(groupDone)));
			endDisplayOrder = System.nanoTime();

			List<Athlete> liftingOrder2 = getLiftingOrder();
			setCurAthlete(liftingOrder2 != null && liftingOrder2.size() > 0 ? liftingOrder2.get(0) : null);
			setNextAthlete(liftingOrder2 != null && liftingOrder2.size() > 1 ? liftingOrder2.get(1) : null);
			// *** recomputeLeadersAndRecords(athletes);
			// for (Athlete a : liftingOrder2) {
			// logger.debug("sinclair {} {}",a.getShortName(), a.getSinclairRank());
			// }
			endLeaders = System.nanoTime();
		}

		if (this.timingLogger.isDebugEnabled()) {
			this.timingLogger.debug("{}*** {} total={}ms, fetch/assign={}ms medals={}ms liftingOrder={}ms leaders={}ms",
			        FieldOfPlay.getLoggingName(this),
			        recomputeCategoryRanks ? "recomputeOrderAndRanks" : "recompute order",
			        (endLeaders - startAssignRanks) / 1000000.0,
			        (endAssignRanks - startAssignRanks) / 1000000.0,
			        (endMedals - endAssignRanks) / 1000000.0,
			        (endDisplayOrder - endMedals) / 1000000.0,
			        (endLeaders - endDisplayOrder) / 1000000.0);
		}

	}

	private List<Athlete> updateScoringSystemRanking(EntityManager em, List<Athlete> l) {
		if (Competition.getCurrent().isDisplayScoreRanks()) {
			// long beforeRanks = System.currentTimeMillis();
			try {
				// this only computes the current scoring system
				Competition.getCurrent().scoringSystemRankings(em);
			} catch (Exception e) {
				this.logger.error("{} scoringSystemRankings exception {}\n ", FieldOfPlay.getLoggingName(this),
				        e,
				        LoggerUtils.stackTrace(e));
			}
			// long afterRanks = System.currentTimeMillis();
			// logger.debug("-------------------- scoringSystemRankings {}ms", afterRanks - beforeRanks);
		}

		List<Athlete> nl = new LinkedList<>();
		for (Athlete a : l) {
			nl.add(em.merge(a));
		}
		em.flush();
		return nl;
	}

	private void recomputeRecordsMap(List<Athlete> athletes) {
		// logger.debug("recompute record map");
		this.groupRecords.clear();
		for (Athlete a : athletes) {
			List<RecordEvent> displayableRecords = RecordFilter.computeDisplayableRecordsForAthlete(a);
			this.displayableRecordsByAthlete.put(a, displayableRecords);

			List<RecordEvent> eligibleRecords = RecordFilter.filterEligibleRecordsForAthlete(a, displayableRecords);
			// logger.debug("athlete {} {}",a, eligibleRecords);
			this.eligibleRecordsByAthlete.put(a, eligibleRecords);

			this.groupRecords.addAll(displayableRecords);
		}
	}

	/**
	 * Reset decisions. Invoked when a fresh clock is given.
	 */
	private void resetDecisions() {
		this.logger.debug("{}**** resetting all decisions on new clock", FieldOfPlay.getLoggingName(this));
		setRefereeDecision(new Boolean[3]);
		resetJuryDecisions();
		setRefereeTime(new Long[3]);
		setRefereeForcedDecision(false);
		pushOutUIEvent(new UIEvent.ResetOnNewClock(this.clockOwner, this, this));
	}

	private void resetEmittedFlags() {
		setInitialWarningEmitted(false);
		setFinalWarningEmitted(false);
		setTimeoutEmitted(false);
		setDownEmitted(false);
		setDecisionDisplayScheduled(false);
		setClockStoppedDecisionsAllowed(false);
	}

	private boolean resumeLifting(FOPEvent e) {
		// time will be restarted anyway
		setWeightAtLastStart(0);
		this.logger.trace("resumeLifting {} {} from:{}", e.getAthlete(),
		        LoggerUtils.whereFrom());

		boolean resumed = false;
		if (getCurAthlete() != null) {
			doTONotifications(null);
			Athlete clockOwner = getClockOwner();
			if (getCurAthlete().equals(clockOwner) && getState() == FOPState.BREAK) {
				this.logger.debug("resuming lifting from state {}", getState());
				// allows referees to enter decisions even if time is not restarted (which
				// sometimes happens).
				setClockStoppedDecisionsAllowed(true);
				setState(CURRENT_ATHLETE_DISPLAYED);
			} else {
				boolean done = recomputeLiftingOrder(true, true);
				if (done) {
					pushOutDone();
				} else {
					setState(CURRENT_ATHLETE_DISPLAYED);
				}
			}

			getBreakTimer().stop();
			setBreakType(null);
			pushOutStartLifting(getGroup(), e.getOrigin());
			uiDisplayCurrentAthleteAndTime(true, e, false);
			resumed = true;
		}
		return resumed;
	}

	private void setAthleteUnderReview(Athlete curAthlete2) {
		this.athleteUnderReview = curAthlete2;
	}

	private void setBreakParams(FOPEvent.BreakStarted e, IBreakTimer breakTimer2, BreakType breakType2,
	        CountdownType countdownType2) {
		this.setBreakType(breakType2);
		this.setCountdownType(countdownType2);
		this.setState(BREAK, LoggerUtils.whereFrom());
		getAthleteTimer().stop();

		if (e.isIndefinite() || countdownType2 == CountdownType.INDEFINITE) {
			breakTimer2.setIndefinite();
		} else if (countdownType2 == CountdownType.DURATION) {
			breakTimer2.setTimeRemaining(e.getTimeRemaining(), false);
			breakTimer2.setEnd(null);
		} else {
			breakTimer2.setTimeRemaining(0, false);
			breakTimer2.setEnd(e.getTargetTime());
		}
		// logger.debug("******* setBreakParams {} {} isIndefinite={}", breakType2, countdownType2,
		// breakTimer2.isIndefinite());
	}

	private void setClockOwner(Athlete athlete) {
		if (athlete == null) {
			this.logger.info("{}no clock owner [{}]", FieldOfPlay.getLoggingName(this), LoggerUtils.whereFrom());
		} else {
			this.logger.info("{}setting clock owner to {} [{}]", FieldOfPlay.getLoggingName(this), athlete,
			        LoggerUtils.whereFrom());
		}
		this.clockOwner = athlete;
	}

	private void setClockOwnerInitialTimeAllowed(int timeAllowed) {
		this.logger.debug("{}setClockOwnerInitialTimeAllowed timeAllowed={} {}", FieldOfPlay.getLoggingName(this),
		        timeAllowed, LoggerUtils.whereFrom());
		this.clockOwnerInitialTimeAllowed = timeAllowed;
	}

	private void setClockStoppedDecisionsAllowed(boolean b) {
		this.clockStoppedDecisionsAllowed = b;
	}

	private void setCurAthlete(Athlete athlete) {
		// logger.trace("setting curAthlete to {} [{}]", athlete,
		// LoggerUtils.whereFrom());
		this.curAthlete = athlete;
	}

	private void setDecisionDisplayScheduled(boolean decisionDisplayScheduled) {
		this.decisionDisplayScheduled = decisionDisplayScheduled;
	}

	/**
	 * @param displayOrder the displayOrder to set
	 */
	private void setDisplayOrder(List<Athlete> displayOrder) {
		this.displayOrder = displayOrder;
	}

	private synchronized void setDownEmitted(boolean downEmitted) {
		this.logger.trace("downEmitted {}", downEmitted);
		this.downEmitted = downEmitted;
	}

	private synchronized void setFinalWarningEmitted(boolean finalWarningEmitted) {
		this.logger.trace("finalWarningEmitted {}", finalWarningEmitted);
		this.finalWarningEmitted = finalWarningEmitted;
	}

	private void setForcedTime(boolean b) {
		this.forcedTime = b;
	}

	/**
	 * @param goodLift the goodLift to set
	 */
	private void setGoodLift(Boolean goodLift) {
		this.goodLift = goodLift;
	}

	private synchronized void setInitialWarningEmitted(boolean initialWarningEmitted) {
		this.logger.trace("initialWarningEmitted {}", initialWarningEmitted);
		this.initialWarningEmitted = initialWarningEmitted;
	}

	private void setLastChallengedRecords(List<RecordEvent> challengedRecords) {
		this.logger.debug("{} + lastChallengedRecords {}", FieldOfPlay.getLoggingName(this), challengedRecords);
		this.lastChallengedRecords = challengedRecords;
	}

	private void setLastNewRecords(List<RecordEvent> newRecords) {
		this.logger.debug("{} + lastNewRecords {}", FieldOfPlay.getLoggingName(this), newRecords);
		this.lastNewRecords = newRecords;
	}

	private void setLiftingOrder(List<Athlete> liftingOrder) {
		this.liftingOrder = liftingOrder;
	}

	private void setPreviousAthlete(Athlete athlete) {
		this.previousAthlete = athlete;
	}

	private void setPrevWeight(int prevWeight) {
		this.prevWeight = prevWeight;
	}

	private void setResultsOrder(List<Athlete> resultsOrderCopy) {
		this.resultsOrder = resultsOrderCopy;
	}

	/**
	 * Don't interrupt break if official-induced break. Interrupt break if it is simply "group done".
	 *
	 * @param newState the state we want to go to if there is no break
	 */
	private void setStateUnlessInBreak(FOPState newState) {
		if (this.state == INACTIVE) {
			// remain in INACTIVE state (do nothing)
		} else if (this.state == BREAK) {
			this.logger.debug("{}Break {} {} newState={}", FieldOfPlay.getLoggingName(this), this.state, getBreakType(),
			        newState);
			// if in a break, we don't stop break timer on a weight change.
			if (getBreakType() == BreakType.GROUP_DONE) {
				// weight change in state GROUP_DONE can happen if there is a loading error
				// and there is no jury deliberation break -- the weight change is entered
				// directly
				// in this case, we need to go back to lifting.
				// set the state now, otherwise attempt board will ignore request to display if
				// in a break

				setState(newState, LoggerUtils.whereFrom());
				if (newState == CURRENT_ATHLETE_DISPLAYED) {
					pushOutStartLifting(getGroup(), this);
				} else {
					uiShowUpdatedRankings();
				}
				getBreakTimer().stop();
			} else {
				this.logger.debug("remaining in state {} {}", this.state, getBreakType());
				// remain in break state
				this.setBreakType(getBreakType());
				this.setState(BREAK);
			}
		} else {
			setState(newState);
		}
	}

	private synchronized void setTimeoutEmitted(boolean timeoutEmitted) {
		this.logger.trace("timeoutEmitted {}", timeoutEmitted);
		this.timeoutEmitted = timeoutEmitted;
	}

	private void setWeightAtLastStart() {
		setWeightAtLastStart(getCurAthlete().getNextAttemptRequestedWeight());
	}

	private synchronized void showDecisionAfterDelay(Object origin2, int reversalDelay) {
		// logger.debug("{}scheduling decision display in {}ms", getLoggingName(),
		// reversalDelay);
		assert !isDecisionDisplayScheduled(); // caller checks.
		setDecisionDisplayScheduled(true); // so there are never two scheduled...
		decisionDisplayTimer = new DelayTimer(isTestingMode()).schedule(() -> showDecisionNow(origin2), reversalDelay);
	}

	/**
	 * The decision is confirmed as official after the 3 second delay following majority. After this delay, manual announcer intervention is required to change
	 * and announce.
	 */
	private void showDecisionNow(Object origin) {
		// logger.debug("*** Show decision now - enter");
		// we need to recompute majority, since they may have been reversal
		int nbWhite = 0;
		for (int i = 0; i < 3; i++) {
			nbWhite = nbWhite + (Boolean.TRUE.equals(getRefereeDecision()[i]) ? 1 : 0);
		}
		setAthleteUnderReview(getCurAthlete());
		setPreviousAthlete(this.athleteUnderReview);

		setLastChallengedRecords(this.challengedRecords);

		if (nbWhite >= 2) {
			setGoodLift(true);
			if (getCurAthlete() != null) {
				this.setCjStarted((getCurAthlete().getAttemptsDone() > 3));
				getCurAthlete().successfulLift();
			}
		} else {
			setGoodLift(false);
			if (getCurAthlete() != null) {
				this.setCjStarted((getCurAthlete().getAttemptsDone() > 3));
				getCurAthlete().failedLift();
			}
		}
		if (getCurAthlete() != null) {
			getCurAthlete().resetForcedAsCurrent();
		}
		setForcedTime(false);
		AthleteRepository.save(getCurAthlete());
		List<RecordEvent> newRecords = updateRecords(getCurAthlete(), getGoodLift(), getChallengedRecords(), List.of());
		setNewRecords(newRecords);
		setLastNewRecords(newRecords);

		// must set state before recomputing order so that scoreboards stop blinking the
		// current athlete
		// must also set state prior to sending event, so that state monitor shows new
		// state.
		setState(DECISION_VISIBLE);
		// logger.debug("*** Show decision now - doit");
		// use "this" because the origin must also show the decision.
		uiShowRefereeDecisionOnSlaveDisplays(getCurAthlete(), getGoodLift(), getRefereeDecision(), getRefereeTime(),
		        this);
		recomputeLiftingOrder(true, true);

		// control timing of notifications
		new DelayTimer(isTestingMode()).schedule(
		        () -> {
			        notifyRecords(getNewRecords(), true);
		        }, 500);
		// tell ourself to reset after 3 secs.
		// Decision reset will handle end of group.
		new DelayTimer(isTestingMode()).schedule(
		        () -> {
			        fopEventPost(new DecisionReset(this));
		        }, DECISION_VISIBLE_DURATION);
	}

	private void showJuryMemberDecisionReceived(Object origin, int i, Boolean[] juryMemberDecision2, int jurySize) {
		// show that one jury decision has been received (green LED)
		// logger.debug("{}updating jury member {}", getLoggingName(), i);
		pushOutUIEvent(new UIEvent.JuryUpdate(origin, i, juryMemberDecision2, jurySize, this));
	}

	private void showJuryMemberDecisionsNow(Object origin, boolean unanimous, int jurySize,
	        Boolean[] juryMemberDecision2) {
		// logger.debug("{}reveal jury member decisions {}", getLoggingName(),
		// juryMemberDecision2);
		pushOutUIEvent(new UIEvent.JuryUpdate(origin, unanimous, juryMemberDecision2, jurySize, this));
	}

	/**
	 * Create a fake unanimous decision when overridden.
	 *
	 * @param e
	 */
	private void simulateDecision(ExplicitDecision ed) {
		long now = System.currentTimeMillis();
		if (getAthleteTimer().isRunning()) {
			getAthleteTimer().stop();
		}

		this.setClockOwner(null);
		DecisionFullUpdate ne = new DecisionFullUpdate(ed.getOrigin(), ed.getAthlete(), ed.ref1, ed.ref2, ed.ref3, now,
		        now, now, isAnnouncerDecisionImmediate());
		setRefereeForcedDecision(true);
		updateRefereeDecisions(ne);
		uiShowUpdateOnJuryScreen(ed);
		// needed to make sure 2min rule is triggered. The athlete we have just decided
		// is the previous athlete.
		this.logger.debug("{}simulateDecision setting previousAthlete to {} -- {}", FieldOfPlay.getLoggingName(this),
		        ed.getAthlete());
		this.setPreviousAthlete(ed.getAthlete());
	}

	private String stateName(FOPState state2) {
		if (state2 == BREAK) {
			return state2.name() + "." + (this.breakType != null ? this.breakType.name() : BreakType.GROUP_DONE);
		} else if (state2 == null) {
			return INACTIVE.name();
		}
		{
			return state2.name();
		}
	}

	/**
	 * @param e
	 */
	/**
	 * @param e
	 */
	private void transitionToBreak(FOPEvent.BreakStarted e) {
		BreakType newBreak = e.getBreakType();
		CountdownType newCountdownType = e.getCountdownType();
		IBreakTimer breakTimer = getBreakTimer();
		boolean indefinite = breakTimer.isIndefinite();
		this.ceremonyType = null;

		// logger.debug("transitionToBreak {}", LoggerUtils.whereFrom());
		if (this.state == BREAK && (getBreakType() == FIRST_CJ)
		        && !(newBreak == BreakType.JURY || newBreak == BreakType.CHALLENGE)) {
			// no interruption other than jury during CJ countdown. Otherwise must stop
			// break.
			// logger.debug("CJ break, rejecting {}", newBreak);
			return;
		}

		if (this.state == BREAK
		        && (getBreakType() != null && getBreakType().isCountdown() && getBreakType() != BreakType.FIRST_CJ)
		        && (newBreak == BreakType.JURY || newBreak == BreakType.CHALLENGE)) {
			// logger.debug("ignoring jury break during {}", getBreakType());
			return;
		}

		doTONotifications(newBreak);

		if (this.state == BREAK) {
			if (getBreakType() == null) {
				// don't care about what was going on, force new break. Used by BreakManagement.
				// logger.debug("{}forced break from breakmgmt", getLoggingName());
				setBreakType(newBreak);
				getBreakTimer().start();

				pushOutUIEvent(new UIEvent.BreakStarted(breakTimer.liveTimeRemaining(), this, false, newBreak,
				        CountdownType.DURATION, e.getStackTrace(), getBreakTimer().isIndefinite(), this));
				return;
			} else if ((newBreak != getBreakType() || newCountdownType != getCountdownType())) {
				// changing the kind of break
				// logger.debug("{}switching break type while in break : current {} new {} remaining {}",
				// getLoggingName(), getBreakType(), newBreak, breakTimer.liveTimeRemaining());
				if (newBreak == BreakType.FIRST_SNATCH) {
					BreakType oldBreakType = getBreakType();
					setBreakType(newBreak);
					// logger.debug("switching oldbreaktype = {} indefinite = {}", oldBreakType, indefinite);
					if (oldBreakType == BEFORE_INTRODUCTION) {
						breakTimer.stop();
						breakTimer.setTimeRemaining(DEFAULT_BREAK_DURATION, true);
						breakTimer.setBreakDuration(DEFAULT_BREAK_DURATION);
						// do not start the break.
					} else {
						breakTimer.setTimeRemaining(breakTimer.liveTimeRemaining(), false);
						// break timer pushes out the BreakStarted event.
						breakTimer.start();
					}

					return;
				} else if (newBreak.isCountdown()) {
					this.logger.debug("{}switching to countdown {}", FieldOfPlay.getLoggingName(this), newBreak,
					        breakTimer.liveTimeRemaining());
					setBreakType(newBreak);
					getBreakTimer().start();
					pushOutUIEvent(new UIEvent.BreakStarted(breakTimer.liveTimeRemaining(), this, false, newBreak,
					        CountdownType.DURATION, LoggerUtils.stackTrace(), getBreakTimer().isIndefinite(), this));
					return;
				} else {
					// logger.debug("{}****** break switch: from {} to {} {}", getLoggingName(), getBreakType(),
					// newBreak, newCountdownType);
					breakTimer.stop();
					setBreakParams(e, breakTimer, newBreak, newCountdownType);
					breakTimer.setTimeRemaining(breakTimer.liveTimeRemaining(), newBreak.isInterruption());
					breakTimer.start(); // so we restart in the new type
					return;
				}
			} else {
				// we are in a break, resume if needed
				// logger.debug("{}******* resuming break : current {} new {}", getLoggingName(), getBreakType(),
				// e.getBreakType());
				if (!breakTimer.isIndefinite()) {
					breakTimer.setOrigin(e.getOrigin());
					breakTimer.setTimeRemaining(breakTimer.liveTimeRemaining(), false);
					breakTimer.start();
				}
				return;
			}
		} else if (this.state == INACTIVE) {
			setBreakType(newBreak);
			setState(BREAK);
			breakTimer.start();
			pushOutUIEvent(new UIEvent.BreakStarted(breakTimer.liveTimeRemaining(), this, false, newBreak,
			        CountdownType.DURATION, e.getStackTrace(), getBreakTimer().isIndefinite(), this));
			return;
		} else {
			setBreakParams(e, breakTimer, newBreak, newCountdownType);
			// logger.debug("stopping bt={} ct={} indefinite={}", newBreak, newCountdownType, newBreak.isInterruption()
			// ? true : e.isIndefinite());
			if (breakTimer.isRunning()) {
				breakTimer.stop(); // so we restart in the new type
			}
		}

		// this will broadcast to all slave break timers
		if (!breakTimer.isRunning()) {
			breakTimer.setOrigin(e.getOrigin());
			setBreakType(newBreak);
			if (indefinite) {
				breakTimer.setIndefinite();
			}
			breakTimer.start();
		}
	}

	private void transitionToLifting(FOPEvent e, Group group2, boolean stopBreakTimer) {
		Athlete clockOwner = getClockOwner();
		if (getState() == TIME_RUNNING || getState() == TIME_STOPPED || getState() == CURRENT_ATHLETE_DISPLAYED) {
			// we are already lifting
			return;
		}
		this.logger.debug("{}transition to lifting from curState = {}", FieldOfPlay.getLoggingName(this), getState());
		if (getCurAthlete() != null && getCurAthlete().equals(clockOwner)
		        && (getState() == FOPState.BREAK || getState() == FOPState.INACTIVE)) {
			setClockStoppedDecisionsAllowed(true);
			setState(CURRENT_ATHLETE_DISPLAYED);
		} else {
			if (getCurAthlete() != null) {
				// group already in progress, do not force loading from database
				loadGroup(group2, e.getOrigin(), false);
			} else {
				loadGroup(group2, e.getOrigin(), true);
			}
			setState(CURRENT_ATHLETE_DISPLAYED);
		}
		if (stopBreakTimer) {
			getBreakTimer().stop();
			setBreakType(null);
		}

		setWeightAtLastStart(0);
		setNewRecords(List.of());
		pushOutStartLifting(getGroup(), e.getOrigin());
		uiDisplayCurrentAthleteAndTime(true, e, false);
	}

	private void transitionToTimeRunning() {

		if (!getCurAthlete().equals(getClockOwner())) {
			setClockOwner(getCurAthlete());
			// setClockOwnerInitialTimeAllowed(getTimeAllowed());
		}
		resetEmittedFlags();
		prepareDownSignal();
		setWeightAtLastStart();
		setLastChallengedRecords(List.of());

		// make sure decisions have been reset before setting state to time running
		int time = getAthleteTimer().getTimeRemaining();
		if (isForcedTime() || this.clockOwner != this.previousAthlete || (time == 60000 || time == 120000)) {
			resetDecisions();
		}

		// enable master to listening for decision
		setState(TIME_RUNNING);
		setGoodLift(null);

		if (getCurAthlete().getAttemptsDone() >= 3) {
			setCjStarted(true);
		}
		getAthleteTimer().start();
	}

	private void uiDisplayCurrentAthleteAndTime(boolean currentDisplayAffected, FOPEvent e, boolean displayToggle) {
		Integer clock = getAthleteTimer().getTimeRemaining();

		this.curWeight = 0;
		Athlete curAthlete2 = getCurAthlete();
		if (curAthlete2 != null) {
			this.curWeight = curAthlete2.getNextAttemptRequestedWeight();
		}
		// if only one athlete, no next athlete
		Athlete nextAthlete = getLiftingOrder().size() > 1 ? getLiftingOrder().get(1) : null;

		Athlete changingAthlete = null;
		if (e instanceof WeightChange) {
			changingAthlete = e.getAthlete();
		}
		boolean inBreak = false;
		if (this.state == FOPState.BREAK) {
			inBreak = ((this.breakTimer != null && this.breakTimer.isRunning()));
		}
		// logger.trace("uiDisplayCurrentAthleteAndTime {} {} {} {} {}",
		// getCurAthlete(), inBreak, getPreviousAthlete(),
		// nextAthlete, currentDisplayAffected);
		Integer newWeight = getPrevWeight() != this.curWeight ? this.curWeight : null;

		if (curAthlete2 != null && curAthlete2.getActuallyAttemptedLifts() == 3) {
			// athlete has until before first CJ to comply with starting weights rule
			// if the snatch was lowered.
			warnMissingKg();
		}
		recomputeLeadersAndRecords(this.displayOrder);

		changePlatformEquipment(curAthlete2, this.curWeight);

		logger.debug("&&&& {} {} {} previous {} current {} change {} from[{}]", curAthlete2, nextAthlete, newWeight,
		        getPrevWeight(), curWeight, newWeight,
		        LoggerUtils.whereFrom());
		pushOutUIEvent(new UIEvent.LiftingOrderUpdated(curAthlete2, nextAthlete, getPreviousAthlete(),
		        changingAthlete,
		        getLiftingOrder(), getDisplayOrder(), clock, currentDisplayAffected, displayToggle, e.getOrigin(),
		        inBreak, newWeight, this));
		setPrevWeight(this.curWeight);

		// cur athlete can be null during some tests.
		int attempts = curAthlete2 == null ? 0 : curAthlete2.getAttemptsDone();

		String shortName = curAthlete2 == null ? "" : curAthlete2.getShortName();
		this.logger.info("{}current athlete = {} attempt = {}, requested = {}, clock={} initialTime={}",
		        FieldOfPlay.getLoggingName(this), shortName, attempts + 1, this.curWeight,
		        clock,
		        getClockOwnerInitialTimeAllowed());

		notifyRecords(getChallengedRecords(), false);

		if (attempts >= 6) {
			pushOutDone();
		}

		if (!this.cjBreakDisplayed && allFirstCJ()) {
			this.logger.debug("{}push out snatch done", FieldOfPlay.getLoggingName(this));
			pushOutSnatchDone();
			this.cjBreakDisplayed = true;
		}
	}

	private void changePlatformEquipment(Athlete a, Integer newWeight) {
		// skip during unit tests.
		if (getPlatform() == null) {
			return;
		}
		boolean use15Bar = false;
		if (Config.getCurrent().featureSwitch("childrenEquipment")) {
			getPlatform().setNbB_5(1);
			getPlatform().setNbB_10(1);
			getPlatform().setNbB_15(1);
			getPlatform().setNbB_20(1);
			getPlatform().setNbL_2_5(1);
			getPlatform().setNbL_5(1);
		}
		boolean federationRule = Config.getCurrent().featureSwitch("lightBarU13") && (a.getAgeGroup().getMinAge() <= 12 && a.getAgeGroup().getMaxAge() <= 20);
		use15Bar = getCurAthlete().getGender() != Gender.M || federationRule;

		if (getPlatform().isUseNonStandardBar()) {
			logger.warn("non standard bar: {}", getPlatform().getNonStandardBarWeight());
			this.setLightBarInUse(true);
			this.setBarWeight(getPlatform().getNonStandardBarWeight());
			this.setUseCollarsIfAvailable(this.curWeight >= 40);
		} else if (newWeight <= 14 && getPlatform().getNbB_5() > 0) {
			logger.warn("<= 14");
			this.setLightBarInUse(true);
			this.setBarWeight(5);
			this.setUseCollarsIfAvailable(false);
		} else if (newWeight <= 19 && getPlatform().getNbB_10() > 0) {
			logger.warn("<= 19");
			this.setLightBarInUse(true);
			this.setBarWeight(10);
			this.setUseCollarsIfAvailable(false);
		} else if ((newWeight <= 39 && (getPlatform().getNbB_20() == 0 || use15Bar) && (getPlatform().getNbB_15() > 0))) {
			logger.warn("<= 39 15");
			this.setLightBarInUse(true);
			this.setBarWeight(15);
			this.setUseCollarsIfAvailable(false);
		} else if ((newWeight >= 40 && (getPlatform().getNbB_20() == 0 || use15Bar) && (getPlatform().getNbB_15() > 0))) {
			logger.warn(">=40 15 collars");
			this.setLightBarInUse(true);
			this.setBarWeight(15);
			this.setUseCollarsIfAvailable(true);
		} else {
			logger.warn("standard");
			this.setLightBarInUse(false);
			Gender gender = curAthlete != null ? curAthlete.getGender() : null;
			this.setBarWeight((gender != null && gender == Gender.M) ? 20 : 15);
			this.setUseCollarsIfAvailable(true);
		}
		return;
	}

	private void setUseCollarsIfAvailable(boolean b) {
		this.useCollarsIfAvailable = b;
	}

	private void setBarWeight(int i) {
		this.barWeight = i;
	}

	private void setLightBarInUse(boolean b) {
		this.lightBarInUse = b;
	}

	private synchronized void uiShowDownSignalOnSlaveDisplays(Object origin2) {
		boolean announcerImmediate = origin2 instanceof AnnouncerContent && isAnnouncerDecisionImmediate();
		boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
		boolean downEmitted2 = isDownEmitted();
		this.uiEventLogger.debug("showDownSignalOnSlaveDisplays server={} emitted={}", emitSoundsOnServer2,
		        downEmitted2);
		if (emitSoundsOnServer2 && !downEmitted2 && !announcerImmediate) {
			// sound is synchronous, we don't want to wait.
			new Thread(() -> {
				try {
					new Sound(getSoundMixer(), "down.wav").emit();
					// downSignal.emit();
				} catch (IllegalArgumentException /* | LineUnavailableException */ e) {
					broadcast("SoundSystemProblem");
				}
			}).start();
			setDownEmitted(true);
		}
		pushOutUIEvent(new UIEvent.DownSignal(origin2, this));
	}

	private void uiShowPlates(BarbellOrPlatesChanged e) {
		if (e.getOrigin() != this) {
			changePlatformEquipment(curAthlete, curWeight);
		}
		pushOutUIEvent(new UIEvent.BarbellOrPlatesChanged(e.getOrigin(), this));
	}

	private void uiShowRefereeDecisionOnSlaveDisplays(Athlete athlete2, Boolean goodLift2, Boolean[] refereeDecision2,
	        Long[] longs, Object origin2) {
		this.uiEventLogger.debug("### showRefereeDecisionOnSlaveDisplays {}", athlete2);
		pushOutUIEvent(new UIEvent.Decision(athlete2, goodLift2, isRefereeForcedDecision() ? null : refereeDecision2[0],
		        refereeDecision2[1],
		        isRefereeForcedDecision() ? null : refereeDecision2[2], origin2, this));
	}

	private void uiShowUpdatedRankings() {
		pushOutUIEvent(new UIEvent.GlobalRankingUpdated(this, this));
	}

	private void uiShowUpdateOnJuryScreen(FOPEvent e) {
		this.uiEventLogger.debug("### uiShowUpdateOnJuryScreen {}", isRefereeForcedDecision());
		// logger.debug("uiShowUpdateOnJuryScreen {}", LoggerUtils.stackTrace());
		pushOutUIEvent(new UIEvent.RefereeUpdate(getCurAthlete(),
		        isRefereeForcedDecision() ? null : getRefereeDecision()[0],
		        getRefereeDecision()[1],
		        isRefereeForcedDecision() ? null : getRefereeDecision()[2], getRefereeTime()[0], getRefereeTime()[1],
		        getRefereeTime()[2],
		        e.getOrigin(), this));
	}

	private void unexpectedEventInState(FOPEvent e, FOPState state) {
		// events not worth signaling
		if (e instanceof DecisionReset || e instanceof DecisionFullUpdate) {
			// ignore
			return;
		}

		this.logger./**/warn("{}unexpected event {} in state {}", FieldOfPlay.getLoggingName(this),
		        e.getClass().getSimpleName(), state);

		pushOutUIEvent(new UIEvent.Notification(this.getCurAthlete(), e.getOrigin(), e, state,
		        UIEvent.Notification.Level.ERROR, this));
	}

	/**
	 * add new records if success, remove records if jury reversal.
	 *
	 * @param a
	 * @param success
	 * @return
	 */
	private List<RecordEvent> updateRecords(Athlete a, boolean success, List<RecordEvent> challengedRecords,
	        List<RecordEvent> voidableRecords) {
		ArrayList<RecordEvent> newRecords = new ArrayList<>();
		if (a == null) {
			return newRecords;
		}
		this.logger.debug("{}updateRecords {} {} {}", FieldOfPlay.getLoggingName(this), a.getShortName(), success,
		        LoggerUtils.whereFrom());
		if (success) {
			for (RecordEvent rec : challengedRecords) {
				Double value = rec.getRecordValue();
				switch (rec.getRecordLift()) {
					case SNATCH:
						Integer bestSnatch = a.getBestSnatch();
						if (bestSnatch > value) {
							createNewRecordEvent(a, newRecords, rec, bestSnatch + 0.0D);
						}
						break;
					case CLEANJERK:
						Integer bestCleanJerk = a.getBestCleanJerk();
						if (bestCleanJerk > value) {
							createNewRecordEvent(a, newRecords, rec, bestCleanJerk + 0.0D);
						}
						break;
					case TOTAL:
						Integer total = a.getTotal();
						if (total > value) {
							createNewRecordEvent(a, newRecords, rec, total + 0.0D);
						}
						break;
					default:
						break;
				}
			}
			JPAService.runInTransaction(em -> {
				// create the new records.
				// do not remove obsolete records, in case jury reverses new record
				// we always use the largest record, so no harm done by keeping the old ones.
				for (RecordEvent re : newRecords) {
					this.logger.info("new record: {}", re);
					em.persist(re);
				}
				return null;
			});
			recomputeRecordsMap(this.displayOrder);
			return newRecords;
		} else {
			// remove records just established as they are invalid.
			if (voidableRecords != null) {
				JPAService.runInTransaction(em -> {
					for (RecordEvent re : voidableRecords) {
						this.logger.info("cancelled record: {}", re);
						em.remove(em.merge(re));
					}
					return null;
				});
				recomputeRecordsMap(this.displayOrder);
			}
			return new ArrayList<>();
		}

	}

	private void updateRefereeDecisions(FOPEvent.DecisionFullUpdate e) {
		getRefereeDecision()[0] = e.ref1;
		getRefereeTime()[0] = e.ref1Time;
		getRefereeDecision()[1] = e.ref2;
		getRefereeTime()[1] = e.ref2Time;
		getRefereeDecision()[2] = e.ref3;
		getRefereeTime()[2] = e.ref3Time;
		processRefereeDecisions(e);
	}

	private void updateRefereeDecisions(FOPEvent.DecisionUpdate e) {
		getRefereeDecision()[e.getRefIndex()] = e.isDecision();
		getRefereeTime()[e.getRefIndex()] = System.currentTimeMillis();
		processRefereeDecisions(e);
	}

	private void warnMissingKg() {
		int missingKg = this.getCurAthlete().startingTotalDelta();
		if (missingKg > 0) {
			pushOutUIEvent(
			        new UIEvent.Notification(
			                this.getCurAthlete(),
			                this,
			                UIEvent.Notification.Level.ERROR,
			                "RuleViolation.StartingWeightCurrent",
			                0, // 3 * UIEvent.Notification.NORMAL_DURATION,
			                this,
			                Integer.toString(missingKg)));
		}
	}

	/**
	 * weight change while the clock is running.
	 *
	 * @param e
	 * @param curAthlete
	 */
	private void weightChangeDoNotDisturb(WeightChange e) {
		recomputeOrderAndRanks(e.isResultChange());
		uiDisplayCurrentAthleteAndTime(false, e, false);
	}

	public boolean isUseCollarsIfAvailable() {
		return useCollarsIfAvailable;
	}

	public int getBarWeight() {
		return barWeight;
	}

	public boolean isLightBarInUse() {
		return lightBarInUse;
	}

}
