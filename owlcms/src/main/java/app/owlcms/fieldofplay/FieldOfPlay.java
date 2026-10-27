/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import tools.jackson.databind.node.BaseJsonNode;

import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.audit.AuditFormat;
import app.owlcms.audit.AuditLog;
import app.owlcms.audit.FopAudit;
import app.owlcms.audit.RecordAudit;
import app.owlcms.audit.RecordChallengeTracker;
import app.owlcms.audit.SessionSummary;
import app.owlcms.audit.SettingsAudit;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.DefaultChampionship;
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
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordFilter;
import app.owlcms.displays.DecisionDisplayRules;
import app.owlcms.nui.shared.SafeEventBusRegistration;
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
import app.owlcms.fieldofplay.FOPEvent.JuryResume;
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
import app.owlcms.monitors.ForwarderSetup;
import app.owlcms.monitors.WebSocketEventForwarder;
import app.owlcms.monitors.IUnregister;
import app.owlcms.monitors.MQTTMonitor;
import app.owlcms.nui.lifting.AnnouncerContent;
import app.owlcms.simulation.CompetitionSimulator;
import app.owlcms.simulation.ProtocolScenario;
import app.owlcms.sound.Sound;
import app.owlcms.sound.Tone;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakStarted;
import app.owlcms.uievents.UIEvent.JuryNotification;
import app.owlcms.uievents.UIEvent.Notification;
import app.owlcms.utils.DelayTimer;
import app.owlcms.utils.JsonUtils;
import app.owlcms.utils.LoggerUtils;
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
public class FieldOfPlay implements IUnregister {

	public static final long DECISION_VISIBLE_DURATION = 3500;
	public static final long JURY_DECISION_VISIBLE_MAX_MS = 10000;
	public static final long JURY_DECISION_HOLD_MS = 3500;
	public static final long DECISION_INPUT_IGNORE_WINDOW_MS = 15000;
	public static final long MINIMUM_DOWN_SIGNAL_VISIBLE_MS = 1500L;
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
	 * Returns FOP name suffix for display, null-safe.
	 * If the FOP is null or there are multiple FOPs, returns empty string.
	 * Otherwise returns " " + fopName (with leading space).
	 *
	 * @param fop the field of play (may be null)
	 * @return suffix string: "" if null or multiple FOPs, " FopName" otherwise
	 */
	public static String getFopNameIfMultiple(FieldOfPlay fop) {
		if (OwlcmsFactory.getFOPs().size() > 1 && fop != null) {
			return " " + fop.getName();
		}
		return "";
	}

	private LinkedHashMap<String, Participation> ageGroupMap = new LinkedHashMap<>();
	private Set<Championship> activeChampionships = new LinkedHashSet<>();
	private Set<Championship> scoreboardChampionships = new LinkedHashSet<>();
	private boolean scoreboardChampionshipsComputed;
	private IProxyTimer athleteTimer;
	private IProxyTimer breakTimer;
	private BreakType breakType;
	private CeremonyType ceremonyType;
	private volatile CeremonyScope activeCeremony;
	private boolean cjStarted;
	private volatile boolean sessionStarted;
	/**
	 * the clock owner is the last athlete for whom the clock has actually started.
	 */
	private Athlete clockOwner;
	private int clockOwnerInitialTimeAllowed;
	private CountdownType countdownType;
	private Athlete curAthlete;
	private int curWeight;
	private long decisionInputIgnoreWindowMs = DECISION_INPUT_IGNORE_WINDOW_MS;
	private long decisionVisibleDuration = DECISION_VISIBLE_DURATION;
	private long juryDecisionVisibleMaxMs = JURY_DECISION_VISIBLE_MAX_MS;
	private long juryDecisionHoldMs = JURY_DECISION_HOLD_MS;
	private TimerTask decisionResetTimer;
	private long decisionResetGeneration;
	private long decisionVisibleSince;
	private long decisionResetDeadline;
	private int decisionJurySize;
	private boolean awaitingJuryCompletion;
	/**
	 * Seven-person Adjudication Panel, phase A: the referee votes of the last lift while its jury votes are still
	 * awaited. Null when no panel evaluation is pending.
	 */
	private Boolean[] panelPendingRefereeVotes;
	/** a clock start was refused while the jury votes were pending; consoles must be told when that ends */
	private boolean panelClockStartRefused;
	/** the current JURY break was started automatically because the panel majority differs from the referees */
	private boolean panelReview;
	/** without VPT: the current JURY break was started automatically by a unanimous jury opposite to the referees */
	private boolean juryReview;
	/** Phase B: the review vote of the seven officials is open (Call for Votes) */
	private boolean panelVoteOpen;
	/** Phase B: the referees' review votes, separate from the initial decision in refereeDecision */
	private Boolean[] reviewRefereeDecision = new Boolean[AdjudicationPanel.NB_REFEREES];
	/** Phase B: the panel majority once all seven have voted, null before */
	private Boolean panelVoteMajority;
	/** Phase B: the votes have been revealed to the jury displays/devices after the last vote */
	private boolean panelVoteRevealed;
	/** Phase B: the vote may be revealed on public boards (after the rule for a No Lift has been supplied) */
	private boolean panelVotePubliclyRevealed;
	/**
	 * Phase A votes (referees 1..3, jury 1..4) that led to the review, shown during the review break until the vote
	 * is called. Null when there is nothing to show.
	 */
	private Boolean[] panelReviewInitialVotes;
	/** a jury decision is being shown (DECISION_VISIBLE after doJuryDecision), until StartLifting */
	private boolean juryDecisionShown;
	/**
	 * Four-member jury: the attempt board shows the jury decision card until this time (ms); the clock cannot be
	 * started while the card is on, even after the FOP has resumed lifting.
	 */
	private long juryDecisionCardVisibleUntil;
	/** how long the attempt boards show the jury decision card after a four-member jury decision */
	public static final long JURY_DECISION_CARD_MS = 5000;
	private boolean decisionCommitScheduled;
	private boolean decisionDisplayScheduled = false;
	private FOPEvent deferredBreak;
	private List<Athlete> displayOrder;
	private boolean downEmitted;
	@SuppressWarnings("unused")
	private Tone downSignal;
	private boolean athleteFinalWarningEmitted;
	private boolean athleteInitialWarningEmitted;
	private boolean athleteTimeoutEmitted;
	private boolean breakFinalWarningEmitted;
	private boolean breakInitialWarningEmitted;
	private boolean breakTimeoutEmitted;
	private EventBus fopEventBus = null;
	private boolean forcedTime = false;
	private Boolean goodLift;
	private Group group = null;
	private long lastGroupLoaded;
	private List<Athlete> leaders;
	private List<Athlete> liftingOrder;
	private ProjectedRank projectedRank;
	private int liftsDoneAtLastStart;
	final private Logger logger = (Logger) LoggerFactory.getLogger(FieldOfPlay.class);
	private TreeMap<String, List<Athlete>> medals;
	private String name;
	private Platform platform = null;
	private EventBus eventForwardingBus = null;
	private FOPEvent prevEvent;
	private boolean auditEventRefused;
	private int auditEventDepth;
	private boolean auditLiftBoundary;
	private boolean auditEventSkipped;
	private FOPState auditRefusedState;
	private final RecordChallengeTracker recordChallengeTracker = new RecordChallengeTracker();
	private boolean sessionSummaryArmed = true;
	private Athlete previousAthlete;
	private Boolean[] refereeDecision;
	private Boolean[] lastShownRefereeDecision;
	private Boolean lastShownGoodLift;
	private Long[] refereeTime;
	private FOPState state;
	private boolean testingMode;
	final private Logger timingLogger = (Logger) LoggerFactory.getLogger(this.logger.getName() + "_Timing");
	private EventBus uiEventBus = null;
	// final private Logger uiEventLogger = (Logger)
	// LoggerFactory.getLogger(this.logger.getName() + "_UI");
	private Thread wakeUpRef;
	private Integer weightAtLastStart;
	private int prevWeight;
	private BaseJsonNode recordsJson;
	private List<RecordEvent> challengedRecords;
	private List<RecordEvent> newRecords;
	private List<RecordEvent> lastChallengedRecords;
	private List<RecordEvent> lastNewRecords;
	private Boolean[] juryMemberDecision;
	private Integer[] juryMemberTime;
	private Athlete athleteUnderReview;
	Map<Athlete, List<RecordEvent>> displayableRecordsByAthlete = new HashMap<>();
	Map<Athlete, List<RecordEvent>> eligibleRecordsByAthlete = new HashMap<>();
	Set<RecordEvent> groupRecords = new HashSet<>();
	private boolean clockStoppedDecisionsAllowed;
	private boolean decisionReceivedWithoutClock;
	private Group videoGroup;
	private Category videoCategory;
	private AgeGroup videoAgeGroup;
	private Championship videoChampionship;
	private List<Athlete> resultsOrder;
	private boolean cjBreakDisplayed;
	private Long missingKgWarnedAthleteId;
	private EventForwarder eventForwarder;
	private WebSocketEventForwarder webSocketEventForwarder;
	/** the Jury Chair pressed No Lift and the Jury Control pages are asked for the technical rule */
	private boolean juryRuleRequested;
	private FieldOfPlay existingFOP;
	private Queue<FOPEvent.WeightChange> deferredWeightChanges = new LinkedList<>();
	private Athlete nextAthlete;
	private TimerTask decisionDisplayTimer;
	private long decisionTaskGeneration;
	private volatile boolean singleReferee;
	private Sound finalWarningSound;
	private Sound initialWarningSound;
	private Sound timeOverSound;
	private Sound breakFinalWarningSound;
	private Sound breakInitialWarningSound;
	private Sound breakTimeOverSound;
	private boolean useCollarsIfAvailable;
	private int barWeight;
	private boolean lightBarInUse;
	private boolean showDecisionsImmediately;
	private InputKind currentInputKind;
	private long ignoreDecisionInputsUntil;
	private long minimumDownSignalVisibleMs = MINIMUM_DOWN_SIGNAL_VISIBLE_MS;
	private long recordNotificationDelayMs = 500;
	private int reversalDelay = REVERSAL_DELAY;
	private long downSignalVisibleSince;

	public FieldOfPlay() {
	}

	/**
	 * Instantiates a new field of play state. When using this constructor
	 * {@link #init(List, IProxyTimer)} must later be used to provide the athletes
	 * and set
	 * the athleteTimer
	 *
	 * @param group     the group (to get details such as name, and to reload
	 *                  athletes)
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
		ForwarderSetup.initializeForFop(this.name, this);
	}

	public void broadcast(String string) {
		pushOutUIEvent(new UIEvent.Broadcast(string, this, this));
	}

	public boolean computeShowAllGroupRecords() {
		boolean forced = Config.getCurrent().featureSwitch(FeatureSwitch.FORCE_ALL_GROUP_RECORDS);
		return forced || Boolean.TRUE.equals(RecordConfig.getCurrent().getShowAllCategoryRecords());
	}

	public boolean computeShowInformationalRecords() {
		boolean forced = Config.getCurrent().featureSwitch(FeatureSwitch.FORCE_ALL_FEDERATION_RECORDS);
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

	public Set<Championship> getActiveChampionships() {
		return this.activeChampionships.isEmpty()
				? Collections.singleton(DefaultChampionship.getInstance())
				: new LinkedHashSet<>(this.activeChampionships);
	}

	public Set<Championship> getScoreboardChampionships() {
		return this.scoreboardChampionshipsComputed
				? new LinkedHashSet<>(this.scoreboardChampionships)
				: getActiveChampionships();
	}

	private Set<String> deriveScoreboardChampionshipNames(List<Athlete> athletes) {
		if (athletes == null || athletes.isEmpty()) {
			return new LinkedHashSet<>();
		}
		return athletes.stream()
				.map(Athlete::getCategory)
				.filter(category -> category != null)
				.map(Category::getAgeGroup)
				.filter(ageGroup -> ageGroup != null)
				.map(AgeGroup::getChampionshipName)
				.filter(championshipName -> championshipName != null && !championshipName.isBlank())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private void refreshScoreboardChampionships(List<Athlete> athletes) {
		Set<String> derivedNames = deriveScoreboardChampionshipNames(athletes);
		if (derivedNames.isEmpty() || this.activeChampionships.isEmpty()) {
			this.scoreboardChampionships = new LinkedHashSet<>();
			this.scoreboardChampionshipsComputed = false;
			return;
		}
		this.scoreboardChampionships = this.activeChampionships.stream()
				.filter(championship -> championship != null && derivedNames.contains(championship.getName()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		this.scoreboardChampionshipsComputed = true;
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

	public int getBarWeight() {
		return this.barWeight;
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

	public CeremonyScope getActiveCeremony() {
		return this.activeCeremony;
	}

	public List<RecordEvent> getChallengedRecords() {
		return this.challengedRecords;
	}

	public Athlete getClockOwner() {
		return this.clockOwner;
	}

	/**
	 * @return 0 if clock has not been started, or the configured
	 *         one-minute/two-minute athlete clock value when started
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

	public WebSocketEventForwarder getWebSocketEventForwarder() {
		return this.webSocketEventForwarder;
	}

	public EventBus getEventForwardingBus() {
		return this.eventForwardingBus;
	}

	public void forceResendCurrentStateToLegacyDisplay() {
		if (this.eventForwarder != null) {
			this.eventForwarder.forceResendCurrentState();
		}
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
		if (curAthlete == null) {
			return List.of();
		}
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

	public TreeMap<String, List<Athlete>> getMedals() {
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

	public Athlete getNextAthlete() {
		return this.nextAthlete;
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

	public BaseJsonNode getRecordsJson() {
		if (this.recordsJson == null) {
			return JsonUtils.nullNode();
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
			this.logger.debug("{}forced time {}", FieldOfPlay.getLoggingName(this), timeRemaining);
			return timeRemaining;
		}
		Athlete a = getCurAthlete();
		int timeAllowed;
		// int clockOwnerInitialTime;

		Athlete owner = getClockOwner();

		if (owner != null && owner.equals(a)) {
			// the clock was started for us. we own the clock, clock is already set to what
			// time was left
			timeAllowed = getAthleteTimer().getTimeRemainingAtLastStop();
		} else if (getPreviousAthlete() != null && getPreviousAthlete().equals(a)) {
			if (owner != null || a.getAttemptNumber() == 1) {
				// clock has started for someone else, one minute
				// first C&J, one minute (doesn't matter who lifted last during snatch)
				timeAllowed = Competition.athleteTimerOneMinute;
			} else {
				timeAllowed = Competition.athleteTimerTwoMinutes;
			}
			if (owner == null) {
				setClockOwnerInitialTimeAllowed(timeAllowed);
			}
		} else {
			timeAllowed = Competition.athleteTimerOneMinute;
			if (owner == null) {
				setClockOwnerInitialTimeAllowed(timeAllowed);
			}
		}
		// logger.trace("{}timeAllowed={} owner={} prev={} cur={}",
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

	public Championship getVideoChampionship() {
		return this.videoChampionship;
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
	 * FOP (Field of Play) events inform us of what is happening (e.g. athleteTimer
	 * started by timekeeper, decision given by official, etc.) The current state
	 * determines what we do with the event. Typically, we update the state of the
	 * field of play (e.g. time is now running) and we issue commands to the
	 * listening user interfaces (e.g. start or stop time being displayed, show the
	 * decision, etc.)
	 *
	 * A given user interface will issue a FOP event. This method reacts to the
	 * event by updating state, and we issue the resulting user interface commands
	 * on
	 * the @link uiEventBus.
	 *
	 * All UIEvents are normally sent by this class, with two exceptions:
	 *
	 * - Timer events are sent by separate Timer objets that implement IProxyTimer.
	 * These classes remember the time and broadcast to all listening timers. -
	 * Some UI Classes send UIEvents to themselves, or sub-components, as a way to
	 * propagate UI State changes (for example, propagating the reset of decision
	 * lights).
	 *
	 * @param e the event
	 */
	@Subscribe
	public synchronized void handleFOPEvent(FOPEvent e) {
		this.auditEventRefused = false;
		this.auditEventSkipped = false;
		this.auditRefusedState = null;
		this.auditEventDepth++;
		try {
			AuditContext.run(e.getAuditActor(), e.getClass().getSimpleName(), () -> {
				String clockBefore = e instanceof FOPEvent.ForceTime ? FopAudit.clock(this) : null;
				doHandleFOPEvent(e);
				if (!this.auditEventSkipped) {
					if (!this.auditEventRefused && this.sessionSummaryArmed
							&& (e instanceof FOPEvent.StartLifting || e instanceof FOPEvent.TimeStarted)) {
						SessionSummary.write(this, e.getAuditActor());
						this.sessionSummaryArmed = false;
					}
					FopAudit.write(this, e, this.auditEventRefused,
							stateName(this.auditRefusedState != null ? this.auditRefusedState : getState()), clockBefore);
					if (!this.auditEventRefused && e instanceof FOPEvent.SwitchGroup) {
						this.sessionSummaryArmed = true;
					}
				}
			});
		} finally {
			this.auditEventDepth--;
			if (this.auditEventDepth == 0 && this.auditLiftBoundary) {
				this.auditLiftBoundary = false;
				AuditLog.closeBlock(getName());
			}
		}
	}

	private void doHandleFOPEvent(FOPEvent e) {
		String stackTrace = e.getStackTrace();
		if (e.getFop() != this) {
			this.auditEventSkipped = true;
			this.logger./**/error("wrong event subscription {} {}\n{}", e, e.getFop(), this, stackTrace);
			return;
			// throw new RuntimeException("wrong event subscription");
		}
		if (e.isDuplicateOf(this.prevEvent)) {
			this.auditEventSkipped = true;
			this.logger.debug("{}state {}, DUPLICATE event received {} {} {}", FieldOfPlay.getLoggingName(this),
					stateName(this.getState()),
					e, getWhereFrom(stackTrace));
			return;
		} else {
			this.logger.info("{}state {}, event received {} from {}", FieldOfPlay.getLoggingName(this),
					stateName(this.getState()),
				e.getClass().getSimpleName(), getWhereFrom(stackTrace));
			this.prevEvent = e;
		}

		if (getState() == BREAK && getBreakType() == BreakType.TEST_BUTTONS) {
			FOPEvent.showEvent(e, this);
			if (!(e instanceof FOPEvent.StartLifting || e instanceof FOPEvent.SwitchGroup)) {
				return;
			}
		}

		// ======= state-independent processing: the reaction does not depend on the
		// state.

		if (e instanceof FOPEvent.JurySizeChange) {
			setJurySize(((FOPEvent.JurySizeChange) e).getJurySize(), e.getOrigin());
			return;
		}

		// it is always possible to explicitly interrupt competition (break between the
		// two lifts, technical incident, etc.). Even switching break type is allowed.

		if (e instanceof FOPEvent.BreakStarted) {
			Object origin = e.getOrigin();
			BreakType requestedBreak = ((FOPEvent.BreakStarted) e).getBreakType();
			boolean allAllowed = origin instanceof BreakController;

			if (getState() == BREAK
					&& (requestedBreak == BreakType.JURY || requestedBreak == BreakType.CHALLENGE)
					&& (getBreakType() == BreakType.FIRST_CJ || getBreakType() == BreakType.GROUP_DONE)) {
				transitionToBreak((FOPEvent.BreakStarted) e);
				return;
			} else if (getState() == BREAK && getBreakType() != null && getBreakType().isCountdown() && !allAllowed) {
				this.auditEventRefused = true;
				this.auditRefusedState = getState();
				this.logger.debug("Break start rejected: origin={} breakType={} state={}",
						origin != null ? origin.getClass().getSimpleName() : "null",
						requestedBreak,
						this.getState());
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
				if (this.state == DECISION_VISIBLE && this.awaitingJuryCompletion
				        && (requestedBreak == BreakType.JURY || requestedBreak == BreakType.CHALLENGE)) {
					// the decision is held until the four jurors have voted on the lift, which may never happen (a
					// jury box that does not work): a review started by the Chair ends the hold, the display ends
					// after its usual minimum and the break starts
					this.logger.info("{}review started while the jury votes are incomplete, ending the decision display",
					        FieldOfPlay.getLoggingName(this));
					this.awaitingJuryCompletion = false;
					this.decisionResetDeadline = Math.max(this.decisionVisibleSince + this.decisionVisibleDuration,
					        System.nanoTime() / 1_000_000);
					armDecisionReset();
				}
			}
			return;
		} else if (e instanceof SummonReferee) {
			SummonReferee e2 = (SummonReferee) e;
			if (e2.getRefNumber() == 0 && isSummonAllVote()) {
				// TCRR 7.5.7: the Chair presses the Referee call buttons to request the new decision
				doPanelVoteCalled(e.getOrigin());
				return;
			} else if (this.panelVoteOpen && isPanelProcedure() && e2.getRefNumber() >= 1 && e2.getRefNumber() <= 3) {
				// re-call one referee who has not voted, nothing is cleared
				pushOutUIEvent(new UIEvent.PanelVoteCalled(e2.getRefNumber(), e.getOrigin(), this));
				return;
			}
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
		} else if (e instanceof FOPEvent.PanelVoteCalled) {
			if (isPanelVoteCallable()) {
				doPanelVoteCalled(e.getOrigin());
			} else {
				unexpectedEventInState(e, getState());
			}
			return;
		} else if (e instanceof FOPEvent.PanelVoteCancelled) {
			doPanelVoteCancelled(e.getOrigin());
			return;
		} else if (e instanceof FOPEvent.JuryRuleRequest) {
			doJuryRuleRequest((FOPEvent.JuryRuleRequest) e);
			return;
		} else if (e instanceof JuryResume) {
			doJuryResume(e);
			return;
		} else if (e instanceof StartLifting) {
			cancelDecisionReset();
			this.juryDecisionShown = false;
			// ending the break abandons an unfinished review vote; the lift keeps the referees' decision
			closePanelVote(e.getOrigin());
			this.panelReviewInitialVotes = null;
			this.panelReview = false;
			this.juryReview = false;
			this.juryRuleRequested = false;
			if (this.state == BREAK && (this.breakType == BreakType.JURY || this.breakType == BreakType.CHALLENGE)) {
				// the jury has dealt with the lift: its votes are no longer awaited
				endPanelVotePending(e.getOrigin());
			}
			endMedalCeremony(e.getOrigin());
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
			} else if (this.state == BREAK && getGroup() != null) {
				// group was not under way when break started, full start.
				transitionToLifting(e, getGroup(), true);
				return;
			} else if (this.state == CURRENT_ATHLETE_DISPLAYED && getGroup() != null) {
				// already lifting: resend so displays that missed the end of the break resync
				this.logger.info("{}StartLifting while already lifting, resyncing displays",
						FieldOfPlay.getLoggingName(this));
				pushOutStartLifting(getGroup(), e.getOrigin());
				uiDisplayCurrentAthleteAndTime(true, e, false);
				return;
			} else if (getGroup() != null) {
				transitionToLifting(e, getGroup(), true);
				return;
			} else {
				setState(INACTIVE);
				loadGroup(null, this, true);
				pushOutSwitchGroup(e.getOrigin());
				uiDisplayCurrentAthleteAndTime(true, e, false);
				return;
			}
		} else if (e instanceof BarbellOrPlatesChanged) {
			uiShowPlates((BarbellOrPlatesChanged) e);
			return;
		} else if (e instanceof SwitchGroup) {
			cancelDecisionReset();
			Group oldGroup = this.getGroup();
			SwitchGroup switchGroup = (SwitchGroup) e;
			Group newGroup = switchGroup.getGroup();
			this.logger.info("{}switching group to {}", FieldOfPlay.getLoggingName(this),
					newGroup != null ? newGroup.getName() : "null");
			var orig = e.getOrigin();
			boolean inBreak = this.state == BREAK || this.state == INACTIVE;
			if (Objects.equals(oldGroup, newGroup) && switchGroup.isForced()) {
				this.logger.info("{}forced reload of group {} from state {}", FieldOfPlay.getLoggingName(this),
						newGroup != null ? newGroup.getName() : "null", this.state);
				this.athleteTimer.stop();
				setState(INACTIVE);
				invalidatePendingDecisionTasks();
				loadGroup(newGroup, this, true);
				pushOutSwitchGroup(orig);
				uiDisplayCurrentAthleteAndTime(true, e, false);
			} else if (Objects.equals(oldGroup, newGroup)) {
				// this.logger.trace("{}reloading", FieldOfPlay.getLoggingName(this));
				loadGroup(newGroup, this, true);
				pushOutSwitchGroup(orig);
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

		if (shouldIgnoreDecisionInputDuringCooldown(e)) {
			return;
		}

		if (e instanceof TimeStarted && refuseClockStartWhileJuryVotesPending((TimeStarted) e)) {
			return;
		}

		if (e instanceof TimeStarted && isFourMemberJury() && isJuryDecisionCardShown()) {
			// four-member jury: the attempt boards still show the jury decision card
			this.logger.info("{}clock start refused, jury decision is being shown",
			        FieldOfPlay.getLoggingName(this));
			unexpectedEventInState(e, DECISION_VISIBLE);
			return;
		}

		// ======= state-dependent processing. Depends on the current state.

		switch (this.getState()) {

			case INACTIVE:
				checkDeferredWeightChanges();
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
						if (getActiveCeremony() == null || !getActiveCeremony().isMedals()) {
							doStartCeremony(
									new FOPEvent.CeremonyStarted(CeremonyType.INTRODUCTION, getGroup(), null, this));
						}
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
				} else if (e instanceof DecisionUpdate && this.panelVoteOpen && isPanelProcedure()) {
					doReviewRefereeVote((DecisionUpdate) e);
				} else if ((e instanceof DownSignal || e instanceof DecisionUpdate) && this.panelVoteOpen) {
					// refboxes may emit a down signal on the second matching vote; without VPT the referees do not
					// vote again
				} else if ((e instanceof DecisionUpdate || e instanceof DownSignal) && !juryDeliberationInProgress()) {
					// referee devices or board keys pressed during an ordinary break: refused without notification
					this.auditEventRefused = true;
					this.auditRefusedState = BREAK;
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
				} else if (e instanceof JuryDecision) {
					// a jury decision arriving after the break has already been exited (e.g., via jury keypad resume)
					doJuryDecision((JuryDecision) e);
				} else if (e instanceof ForceTime) {
					doForceTime((ForceTime) e);
				} else if (e instanceof CeremonyDone) {
					// ignore, already dealt by timer
				} else if (e instanceof DecisionFullUpdate) {
					// Accept decision even though clock was not started
					// Clear stale decisions and set up for decision processing
					setupForDecisionWithoutClock(e);
					updateRefereeDecisions((DecisionFullUpdate) e);
					uiShowUpdateOnJuryScreen(e);
				} else if (e instanceof DecisionUpdate) {
					// Accept decision even though clock was not started
					// Clear stale decisions and set up for decision processing
					setupForDecisionWithoutClock(e);
					doPossiblySoloRefereeUpdate(e);
				} else if (e instanceof ExplicitDecision) {
					// Accept announcer forced decision even though clock was not started
					// Clear stale decisions and set up for decision processing (same as MQTT flow)
					setupForDecisionWithoutClock(e);
					simulateDecision((ExplicitDecision) e);
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
						checkFirstClockForLift();
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
					// If no clock owner (e.g., after session reload), set up properly first
					if (getClockOwner() == null) {
						setupForDecisionWithoutClock(e);
					}
					updateRefereeDecisions((DecisionFullUpdate) e);
					uiShowUpdateOnJuryScreen(e);
				} else if (e instanceof DecisionUpdate) {
					// If no clock owner (e.g., after session reload), set up properly first
					if (getClockOwner() == null) {
						setupForDecisionWithoutClock(e);
					}
					// mqtt
					doPossiblySoloRefereeUpdate(e);
				} else if (e instanceof TimeStarted) {
					if (!getCurAthlete().equals(getClockOwner())) {
						setClockOwner(getCurAthlete());
						// setClockOwnerInitialTimeAllowed(getTimeAllowed());
					}
					getTimeAllowed();
					prepareDownSignal();
					setWeightAtLastStart();

					/*
					 * THIS IS WRONG, we need to reset decisions on clock restart.
					 * // we do not reset decisions or "emitted" flags
					 * setState(TIME_RUNNING);
					 * checkFirstClockForLift();
					 * getAthleteTimer().start();
					 */

					checkFirstClockForLift();
					restartTimer(e, false);
				} else if (e instanceof WeightChange) {
					doWeightChange((WeightChange) e);
				} else if (e instanceof ExplicitDecision) {
					simulateDecision((ExplicitDecision) e);
				}
				// else if (e instanceof ForceTime) {
				// getAthleteTimer().setTimeRemaining(((ForceTime) e).timeAllowed, false);
				// setState(CURRENT_ATHLETE_DISPLAYED);
				// }
				else if (e instanceof TimeStopped) {
					// ignore duplicate time stopped
				} else if (e instanceof TimeOver) {
					// ignore, already dealt by timer
				} else if (e instanceof StartLifting) {
					// nothing to do, end of break when clock was already started
				} else if (e instanceof ForceTime) {
					doForceTime((ForceTime) e);
				} else if (e instanceof CeremonyDone) {
					// ignore, already dealt by timer
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
					// needed if decision has been given too early (e.g. bar did not reach the knees
					// but reds given)
					restartTimer(e, true);
				} else {
					unexpectedEventInState(e, DOWN_SIGNAL_VISIBLE);
				}
				break;

			case DECISION_VISIBLE:
				if (e instanceof ExplicitDecision) {
					simulateDecision((ExplicitDecision) e);
					// showExplicitDecision(((ExplicitDecision) e), e.origin);
				} else if (e instanceof DecisionFullUpdate) {
					if (this.decisionCommitScheduled) {
						updateRefereeDecisions((DecisionFullUpdate) e);
						uiShowUpdateOnJuryScreen(e);
						setGoodLift(computeCurrentGoodLift());
						// only re-emit when a referee actually reversed; an identical
						// decision does not affect the outcome or the display.
						if (!refereeDecisionUnchangedSinceLastShown()) {
							uiShowCurrentRefereeDecision(e.getOrigin());
						}
					} else {
						// late update
						pushOutUIEvent(new UIEvent.Notification(this.getCurAthlete(), e.getOrigin(), e, this.state,
								UIEvent.Notification.Level.ERROR, this));
					}
				} else if (e instanceof DecisionUpdate) {
					if (this.decisionCommitScheduled) {
						updateRefereeDecisions((DecisionUpdate) e);
						uiShowUpdateOnJuryScreen(e);
						setGoodLift(computeCurrentGoodLift());
						// only re-emit when a referee actually reversed; an identical
						// decision does not affect the outcome or the display.
						if (!refereeDecisionUnchangedSinceLastShown()) {
							uiShowCurrentRefereeDecision(e.getOrigin());
						}
					} else {
						// late update
						pushOutUIEvent(new UIEvent.Notification(this.getCurAthlete(), e.getOrigin(), e, this.state,
								UIEvent.Notification.Level.ERROR, this));
					}
				} else if (e instanceof WeightChange) {
					// we need to defer the weight change event until the decision has been shown
					this.logger.debug("{}weight change during decision visible {} {} {}",
							FieldOfPlay.getLoggingName(this), e.getAthlete(),
							this.getPreviousAthlete(),
							this.getCurAthlete());
					this.deferredWeightChanges.add((WeightChange) e);
				} else if (e instanceof DecisionReset) {
					finishDecisionDisplay(e);
				} else {
					unexpectedEventInState(e, DECISION_VISIBLE);
				}
				break;
		}
	}

	/**
	 * Apply the common Resume-button behaviour for Jury Control and the physical jury box.
	 */
	private void doJuryResume(FOPEvent e) {
		boolean normalBreak = this.state == BREAK;
		boolean juryCycleActive = this.state == DECISION_VISIBLE || isJuryVotesAwaited()
		        || this.panelVoteOpen || juryDeliberationInProgress() || isJuryDecisionCardShown();
		if (juryCycleActive) {
			endPanelVotePending(e.getOrigin());
			resetJuryDecisions();
			// an explicit Resume ends the decision cycle: the clock may start at once
			this.juryDecisionCardVisibleUntil = 0;
			pushOutUIEvent(new UIEvent.JuryDisplayReset(e.getOrigin(), this));
			doHandleFOPEvent(new StartLifting(e.getOrigin()));
		} else if (normalBreak) {
			// Match BreakManagement.endBreak exactly: an ordinary break only needs StartLifting.
			doHandleFOPEvent(new StartLifting(e.getOrigin()));
		} else if (this.state == CURRENT_ATHLETE_DISPLAYED) {
			// Lifting has already resumed (automatically after the decision) and the clock has not started: the
			// jury displays and the physical jury box may still show the previous votes; Resume clears them.
			resetJuryDecisions();
			pushOutUIEvent(new UIEvent.JuryDisplayReset(e.getOrigin(), this));
		} else {
			// During an active lift, before referee decisions, Resume is deliberately a no-op.
			return;
		}
	}

	/**
	 * Clear the decision lights: process the weight changes and the break that were deferred while the decision was
	 * visible.
	 */
	private void finishDecisionDisplay(FOPEvent e) {
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
	}

	/**
	 * The clock is not started for the next attempt while the configured jury has not finished voting on the previous
	 * lift. The Competition Supervisor (Speaker in PIN mode) may start it anyway, which abandons the incomplete jury
	 * votes and leaves the referees' decision in place (technical mishap recovery).
	 *
	 * @return true if the event has been fully handled (refused)
	 */
	private boolean refuseClockStartWhileJuryVotesPending(TimeStarted e) {
		if (!isJuryVotesAwaited()) {
			return false;
		}
		boolean displayHeldForJury = this.state == DECISION_VISIBLE && this.awaitingJuryCompletion;
		if (this.state != CURRENT_ATHLETE_DISPLAYED && !displayHeldForJury) {
			// other states keep their usual handling (e.g. "wait until the decision has been shown")
			return false;
		}
		if (!e.isOverride()) {
			this.auditEventRefused = true;
			this.auditRefusedState = this.state;
			this.logger.info("{}clock start refused, jury has not finished voting on {}",
			        FieldOfPlay.getLoggingName(this), this.athleteUnderReview);
			this.panelClockStartRefused = true;
			pushOutUIEvent(new UIEvent.JuryVotesPending(this.athleteUnderReview, true, e.getOrigin(), this));
			return true;
		}
		this.logger.info("{}clock start override, abandoning incomplete jury votes on {}",
		        FieldOfPlay.getLoggingName(this), this.athleteUnderReview);
		endPanelVotePending(e.getOrigin());
		resetJuryDecisions();
		if (displayHeldForJury) {
			finishDecisionDisplay(e);
		}
		// continue with the normal clock start
		return false;
	}

	/**
	 * Once all configured jurors have voted, release the next-clock gate. For the four-member jury, also evaluate the
	 * automatic review rules.
	 */
	private void evaluatePanel() {
		if (!isJuryVotesAwaited() || juryDeliberationInProgress()) {
			return;
		}
		if (!allJurorsVoted(getJurySize())) {
			return;
		}
		Boolean[] refereeVotes = this.panelPendingRefereeVotes;
		endPanelVotePending(this);
		if (getJurySize() != AdjudicationPanel.NB_JURORS) {
			// 3- and 5-member juries do not trigger a review automatically.
			return;
		}
		Boolean refereeMajority = AdjudicationPanel.refereeMajority(refereeVotes);
		if (!isPanelProcedure()) {
			// without Video Playback (7.5.8) the jury deliberates by hand; the automatic stop on a unanimous jury
			// opposite to the referees is an option
			if (!Config.getCurrent().featureSwitch(FeatureSwitch.AUTOMATIC_BREAK_NO_VPT)) {
				return;
			}
			Boolean juryUnanimous = unanimousJury(getJuryMemberDecision());
			if (juryUnanimous == null || refereeMajority == null || juryUnanimous.equals(refereeMajority)) {
				return;
			}
			this.logger.info("{}unanimous jury against the referees, jury deliberation for {}",
			        FieldOfPlay.getLoggingName(this), this.athleteUnderReview);
			this.juryReview = true;
			fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, null, null, true, this));
			return;
		}
		AdjudicationPanel panel = new AdjudicationPanel(refereeVotes, getJuryMemberDecision());
		if (!panel.differsFrom(refereeMajority)) {
			this.logger.info("{}adjudication panel confirms the referee decision ({} white, {} red) for {}",
			        FieldOfPlay.getLoggingName(this), panel.whites(), panel.reds(), this.athleteUnderReview);
			return;
		}
		this.logger.info("{}adjudication panel review required ({} white, {} red) for {}",
		        FieldOfPlay.getLoggingName(this), panel.whites(), panel.reds(), this.athleteUnderReview);
		this.panelReview = true;
		fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, null, null, true, this));
	}

	/**
	 * @return the common vote of the four jury members, or null if one has not voted or they differ
	 */
	private static Boolean unanimousJury(Boolean[] jury) {
		if (jury == null || jury.length < AdjudicationPanel.NB_JURORS || jury[0] == null) {
			return null;
		}
		for (int i = 1; i < AdjudicationPanel.NB_JURORS; i++) {
			if (!jury[0].equals(jury[i])) {
				return null;
			}
		}
		return jury[0];
	}

	private void endPanelVotePending(Object origin) {
		this.panelPendingRefereeVotes = null;
		if (this.panelClockStartRefused) {
			this.panelClockStartRefused = false;
			pushOutUIEvent(new UIEvent.JuryVotesPending(this.athleteUnderReview, false, origin, this));
		}
	}

	/**
	 * Phase B step B2: the review vote is called. The votes are cleared; with the seven-person panel the referee
	 * devices are re-armed and buzzed; for a jury vote (any other jury) only the jurors vote again and the referees'
	 * decision stays. Calling again restarts the vote.
	 */
	private void doPanelVoteCalled(Object origin) {
		this.logger.info("{}{} review vote called for {}", FieldOfPlay.getLoggingName(this),
		        isPanelProcedure() ? "adjudication panel" : "jury", this.athleteUnderReview);
		this.panelVoteOpen = true;
		this.panelVoteMajority = null;
		this.juryRuleRequested = false;
		this.panelVoteRevealed = false;
		this.panelVotePubliclyRevealed = false;
		this.panelReviewInitialVotes = null;
		this.reviewRefereeDecision = new Boolean[AdjudicationPanel.NB_REFEREES];
		resetJuryDecisions();
		pushOutUIEvent(new UIEvent.PanelVoteCalled(0, origin, this));
		pushPanelVoteUpdate(origin);
	}

	/**
	 * Review started by the Chair (Deliberation or Challenge) without the seven-person panel (3- or 5-member jury with
	 * or without VPT, four-member jury without VPT): the jury lights have been cleared and the jurors may vote at once;
	 * the Chair asks for the vote verbally (Summon All summons the referees). The votes on the lift are kept, and the
	 * vote has to be called, only after an automatic trigger, or with the seven-person panel whose referees vote again.
	 */
	private void openJuryVoteAtReviewStart() {
		if (hasJuryReviewVote() && !isPanelProcedure() && !this.juryReview && this.athleteUnderReview != null) {
			doPanelVoteCalled(this);
		}
	}

	/**
	 * @return true when Summon All (keypad, jury box, `K`) means Call for Votes: during a review of the seven-person
	 *         panel (the referees vote again), or after the automatic stop of a four-member jury without VPT.
	 *         Otherwise it summons the referees (TCRR 7.5.7, "the Jury may call the Referees in question").
	 */
	public boolean isSummonAllVote() {
		return isPanelVoteCallable() && (isPanelProcedure() || this.juryReview);
	}

	/**
	 * Without the seven-person panel: the jury votes on the lift, counted with the jury's rule (unanimity, or majority
	 * for a 5-member jury with VPT), would reverse the current decision on the lift (the referees', or the jury's once
	 * it has decided, so the cue ends with the decision). Jury Control uses it to prompt the Chair.
	 */
	public boolean isJuryVoteAgainstReferees() {
		if (isPanelProcedure() || !hasJuryReviewVote()) {
			return false;
		}
		Boolean verdict = computeCurrentJuryVerdict(getJurySize());
		Boolean current = getGoodLift();
		return verdict != null && current != null && !verdict.equals(current);
	}

	private void doReviewRefereeVote(DecisionUpdate e) {
		int refIndex = e.getRefIndex();
		if (refIndex < 0 || refIndex >= AdjudicationPanel.NB_REFEREES) {
			this.logger.debug("{}ignoring review vote from referee index {}", FieldOfPlay.getLoggingName(this),
			        refIndex);
			return;
		}
		if (this.panelVoteMajority != null) {
			this.logger.info("{}review vote already complete, referee {} vote ignored",
			        FieldOfPlay.getLoggingName(this), refIndex + 1);
			return;
		}
		this.reviewRefereeDecision[refIndex] = e.isDecision();
		checkPanelVoteComplete(e.getOrigin(), refIndex);
	}

	/**
	 * Publish who has voted. When all have voted, reveal the votes to Jury Control and the physical jury box. Public
	 * boards remain hidden until the decision is applied (doJuryDecision), so a No Lift is shown publicly with its
	 * rule. With Video Playback the seven officials vote and the majority decides; without it the four jury members
	 * vote and must be unanimous to reverse (7.5.8), otherwise the referees' decision stands.
	 */
	private void checkPanelVoteComplete(Object origin, Integer refereeUpdated) {
		Boolean[] jury = getJuryMemberDecision();
		Boolean majority;
		String tally;
		if (isPanelProcedure()) {
			AdjudicationPanel panel = new AdjudicationPanel(this.reviewRefereeDecision, jury);
			majority = panel.majority();
			tally = panel.whites() + " white, " + panel.reds() + " red";
		} else {
			majority = computeCurrentJuryVerdict(getJurySize());
			tally = "jury " + Arrays.toString(jury);
		}
		this.panelVoteMajority = majority;
		if (majority != null && !this.panelVoteRevealed) {
			this.panelVoteRevealed = true;
			if (!isPanelProcedure()) {
				this.reviewRefereeDecision = getRefereeDecision().clone();
			}
			showJuryMemberDecisionsNow(origin, unanimousJury(jury) != null, getJurySize(), jury);
		}
		pushPanelVoteUpdate(origin, refereeUpdated);
		if (majority == null) {
			return;
		}
		this.logger.info("{}review vote complete: {} ({}) for {}",
		        FieldOfPlay.getLoggingName(this), majority ? "good lift" : "no lift", tally, this.athleteUnderReview);
		// the result waits for the Jury Chair: Confirm Good Lift, or the technical rule for a No Lift, or Cancel vote
		// result
		if (!majority) {
			this.logger.info("{}no lift on review, waiting for the Jury Chair to give the rule",
			        FieldOfPlay.getLoggingName(this));
			pushOutUIEvent(new UIEvent.JuryRuleRequired(this.athleteUnderReview, true, false, origin, this));
		}
	}

	/**
	 * Abandon the review vote (end of the break, or a decision has been applied).
	 */
	private void closePanelVote(Object origin) {
		if (!this.panelVoteOpen && this.panelVoteMajority == null) {
			return;
		}
		this.logger.info("{}adjudication panel review vote closed", FieldOfPlay.getLoggingName(this));
		this.panelVoteOpen = false;
		this.panelVoteMajority = null;
		this.panelVoteRevealed = false;
		this.panelVotePubliclyRevealed = false;
		this.reviewRefereeDecision = new Boolean[AdjudicationPanel.NB_REFEREES];
		pushPanelVoteUpdate(origin);
	}

	/**
	 * Cancel vote result: the votes are cleared on every display and device, and the break continues. The Chair calls
	 * a new vote; after a review started by the Chair without VPT, the jurors may vote again at once.
	 */
	private void doPanelVoteCancelled(Object origin) {
		boolean ruleRequested = this.juryRuleRequested || Boolean.FALSE.equals(this.panelVoteMajority);
		this.juryRuleRequested = false;
		resetJuryDecisions();
		this.panelReviewInitialVotes = null;
		closePanelVote(origin);
		if (ruleRequested) {
			pushOutUIEvent(new UIEvent.JuryRuleRequired(this.athleteUnderReview, true, true, origin, this));
		}
		pushOutUIEvent(new UIEvent.JuryDisplayReset(origin, this));
		if (juryDeliberationInProgress() && !this.juryReview) {
			openJuryVoteAtReviewStart();
		}
	}

	/**
	 * The Jury Chair pressed No Lift (Jury Control or jury box, same event): the Jury Control pages are asked for the
	 * technical rule; the decision is posted with it. If no Jury Control page answers, the Technical Controller relays
	 * the decision to the Speaker. A cancelled request is withdrawn on every page.
	 */
	private void doJuryRuleRequest(FOPEvent.JuryRuleRequest e) {
		if (e.isCancelled()) {
			this.juryRuleRequested = false;
			pushOutUIEvent(new UIEvent.JuryRuleRequired(this.athleteUnderReview, false, true, e.getOrigin(), this));
			return;
		}
		if (this.state != BREAK || !juryDeliberationInProgress() || this.athleteUnderReview == null) {
			this.logger.info("{}No Lift rule request ignored outside a jury review", FieldOfPlay.getLoggingName(this));
			this.auditEventRefused = true;
			this.auditRefusedState = this.state;
			return;
		}
		this.juryRuleRequested = true;
		boolean forVote = Boolean.FALSE.equals(this.panelVoteMajority);
		pushOutUIEvent(new UIEvent.JuryRuleRequired(this.athleteUnderReview, forVote, false, e.getOrigin(), this));
	}

	/**
	 * The review decision is being applied: public boards may now reveal the vote. The jury displays and physical jury
	 * box already revealed it when the last vote was cast.
	 */
	private void revealPanelVotePublicly(Object origin) {
		this.panelVotePubliclyRevealed = true;
		pushPanelVoteUpdate(origin);
	}

	/**
	 * Four-member jury: the Jury Chair entered the decision with the Good Lift / No Lift buttons. The jury votes
	 * (review vote, jury circles, Phase A pattern, physical jury box) are cleared so that nothing contradicting the
	 * Chair's decision stays on display; the decision card follows.
	 */
	private void clearJuryVotesForChairDecision(Object origin) {
		boolean voteInProgress = this.panelVoteOpen || this.panelVoteMajority != null;
		this.panelVoteOpen = false;
		this.panelVoteMajority = null;
		this.panelVoteRevealed = false;
		this.panelVotePubliclyRevealed = false;
		this.reviewRefereeDecision = new Boolean[AdjudicationPanel.NB_REFEREES];
		this.panelReviewInitialVotes = null;
		resetJuryDecisions();
		if (voteInProgress) {
			this.logger.info("{}adjudication panel review vote replaced by the Jury Chair's decision",
			        FieldOfPlay.getLoggingName(this));
			pushPanelVoteUpdate(origin);
		}
		pushOutUIEvent(new UIEvent.JuryDisplayReset(origin, this));
	}

	private void pushPanelVoteUpdate(Object origin) {
		pushPanelVoteUpdate(origin, null);
	}

	private void pushPanelVoteUpdate(Object origin, Integer refereeUpdated) {
		pushOutUIEvent(createPanelVoteUpdate(origin, refereeUpdated));
	}

	/** Current review vote, including its reveal state, for a display that attaches or re-syncs during the review. */
	public UIEvent.PanelVoteUpdate getPanelVoteUpdate(Object origin) {
		return createPanelVoteUpdate(origin, null);
	}

	private UIEvent.PanelVoteUpdate createPanelVoteUpdate(Object origin, Integer refereeUpdated) {
		Boolean[] jury = getJuryMemberDecision();
		int jurySize = getJurySize();
		boolean[] voted = new boolean[AdjudicationPanel.NB_REFEREES + jurySize];
		Boolean[] votes = new Boolean[AdjudicationPanel.NB_REFEREES + jurySize];
		for (int i = 0; i < AdjudicationPanel.NB_REFEREES; i++) {
			votes[i] = this.reviewRefereeDecision[i];
		}
		for (int i = 0; i < jurySize; i++) {
			votes[AdjudicationPanel.NB_REFEREES + i] = jury != null && jury.length > i ? jury[i] : null;
		}
		for (int i = 0; i < votes.length; i++) {
			voted[i] = votes[i] != null;
		}
		return new UIEvent.PanelVoteUpdate(this.panelVoteOpen, voted,
		        this.panelVoteRevealed ? votes : null, this.panelVoteMajority,
		        this.panelVotePubliclyRevealed, refereeUpdated, origin, this);
	}

	private void checkFirstClockForLift() {
		if (getGroup() != null) {
			this.sessionStarted = true;
		}
		if (getAthleteTimer().getTimeRemaining() == Competition.athleteTimerOneMinute) {
			if (this.isFirstSnatch()) {
				getGroup().setFirstSnatchTime(LocalDateTime.now(), this);
			} else if (this.isFirstCJ()) {
				getGroup().setFirstCJTime(LocalDateTime.now(), this);
			}
		}
	}

	public void checkLastDecision() {
		if (this.isLastSnatch()) {
			getGroup().setLastSnatchDecisionTime(LocalDateTime.now(), getGroup(), this);
		} else if (this.isLastCJ()) {
			getGroup().setLastCJDecisionTime(LocalDateTime.now(), getGroup(), this);
		}
	}

	public void init(List<Athlete> athletes, IProxyTimer timer, IProxyTimer breakTimer, boolean alreadyLoaded) {
		this.athleteTimer = timer;
		this.athleteTimer.setFop(this);
		this.breakTimer = breakTimer;
		this.breakTimer.setFop(this);
		this.showDecisionsImmediately = Config.getCurrent().featureSwitch(FeatureSwitch.SHOW_DECISIONS_IMMEDIATELY);
		this.setCurAthlete(null);
		this.setClockOwner(null);
		this.setClockOwnerInitialTimeAllowed(0);
		this.setLiftingOrder(athletes);
		List<AgeGroup> allAgeGroups = AgeGroupRepository.findAgeGroups(getGroup());
		this.ageGroupMap = new LinkedHashMap<>();
		this.activeChampionships = new LinkedHashSet<>();
		for (AgeGroup ag : allAgeGroups) {
			this.ageGroupMap.put(ag.getCode(), null);
			Championship championship = ag.getChampionship();
			if (championship != null) {
				this.activeChampionships.add(championship);
			}
		}
		refreshScoreboardChampionships(athletes);
		this.setMedals(new TreeMap<>());
		// if (this.getGroup() != null) {
		// Competition.getCurrent().computeMedals(this.getGroup());
		// }
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

	public boolean isCjStarted() {
		return this.cjStarted;
	}

	/**
	 * A session is in progress once the athlete clock has been started for it, or if it already had completed
	 * attempts when selected. Stops, resets and breaks do not change this; unselecting the session clears it.
	 *
	 * @return true if registration and results data for the selected session must not be edited.
	 */
	public boolean isSessionInProgress() {
		return this.group != null && this.sessionStarted;
	}

	public boolean isClockStoppedDecisionsAllowed() {
		return this.clockStoppedDecisionsAllowed;
	}

	public boolean isEmitSoundsOnServer() {
		boolean b = getSoundMixer() != null;
		this.logger.trace("emit sound on server = {}", b);
		return b;
	}

	public boolean isLightBarInUse() {
		return this.lightBarInUse;
	}

	public InputKind getCurrentInputKind() {
		return this.currentInputKind;
	}

	public boolean isSingleReferee() {
		return this.singleReferee;
	}

	public boolean isShowDecisionsImmediately() {
		return this.showDecisionsImmediately;
	}

	public boolean isTestingMode() {
		return this.testingMode;
	}

	public synchronized boolean isAthleteTimeoutEmitted() {
		return this.athleteTimeoutEmitted;
	}

	public boolean isUseCollarsIfAvailable() {
		return this.useCollarsIfAvailable;
	}

	/** @return true when the configured jury has four members */
	public boolean isFourMemberJury() {
		return getJurySize() == AdjudicationPanel.NB_JURORS;
	}

	/**
	 * @return true when the seven-person Adjudication Panel procedure applies (four-member jury and Video Playback
	 *         Technology in use). Without VPT, the four-member jury needs unanimity (7.5.8).
	 */
	public boolean isPanelProcedure() {
		return isFourMemberJury() && Competition.getCurrent().isVideoPlaybackTechnology();
	}

	/**
	 * @return true while the jury votes on the last lift are awaited by the Adjudication Panel procedure
	 */
	public boolean isPanelVotePending() {
		return this.panelPendingRefereeVotes != null && isPanelProcedure();
	}

	/** @return true while the configured jury's votes on the last lift are awaited */
	public boolean isJuryVotesAwaited() {
		return this.panelPendingRefereeVotes != null;
	}

	/**
	 * @return true while the current JURY break is an automatic Adjudication Panel Review
	 */
	public boolean isPanelReview() {
		return this.panelReview;
	}

	/**
	 * @return true while the current JURY break was started automatically (without VPT) by a unanimous four-member
	 *         jury opposite to the referees
	 */
	public boolean isJuryReview() {
		return this.juryReview;
	}

	/**
	 * @return true while the Jury Control pages are asked for the technical rule of a No Lift pressed by the Jury
	 *         Chair
	 */
	public boolean isJuryRuleRequested() {
		return this.juryRuleRequested;
	}

	/**
	 * @return true while the review vote of the seven officials is open
	 */
	public boolean isPanelVoteOpen() {
		return this.panelVoteOpen;
	}

	/**
	 * @return true once all seven officials have voted (the votes and the majority can be shown)
	 */
	public boolean isPanelVoteComplete() {
		return this.panelVoteMajority != null;
	}

	/**
	 * @return the result of the review vote (true = good lift) once all have voted, null before
	 */
	public Boolean getPanelVoteMajority() {
		return this.panelVoteMajority;
	}

	/**
	 * @return true when the Jury Chair can call the review vote: four-member jury (with or without VPT), jury or
	 *         challenge break, a lift under review
	 */
	public boolean isPanelVoteCallable() {
		return hasJuryReviewVote() && this.state == BREAK && juryDeliberationInProgress()
		        && this.athleteUnderReview != null;
	}

	/**
	 * @return true when a jury is present: a review is decided by a review vote (the seven-person panel, or the jury
	 *         alone with 3, 4 or 5 members) confirmed by the Jury Chair
	 */
	public boolean hasJuryReviewVote() {
		return getJurySize() > 0;
	}

	/**
	 * @return the number of votes in the review vote: 3 referees then the jury members (the referees only vote with
	 *         the seven-person panel)
	 */
	public int getReviewVoteSize() {
		return AdjudicationPanel.NB_REFEREES + getJurySize();
	}

	/**
	 * @return the referees' votes in the review vote (not the initial decision)
	 */
	public Boolean[] getReviewRefereeDecision() {
		return this.reviewRefereeDecision;
	}

	/**
	 * @return the votes on the lift (referees 1..3 then the jury members), kept during the review break until the Chair
	 *         calls for votes (seven-person panel, or after an automatic stop); null otherwise
	 */
	public Boolean[] getPanelReviewInitialVotes() {
		Boolean[] votes = this.panelReviewInitialVotes;
		return votes != null ? votes.clone() : null;
	}

	private void capturePanelReviewInitialVotes() {
		if (this.panelVoteOpen) {
			// the jury votes are now review votes, not the Phase A votes
			return;
		}
		Boolean[] referees = getRefereeDecision();
		Boolean[] jury = getJuryMemberDecision();
		int jurySize = getJurySize();
		Boolean[] votes = new Boolean[AdjudicationPanel.NB_REFEREES + jurySize];
		boolean any = false;
		for (int i = 0; i < AdjudicationPanel.NB_REFEREES; i++) {
			votes[i] = referees != null && referees.length > i ? referees[i] : null;
			any = any || votes[i] != null;
		}
		for (int i = 0; i < jurySize; i++) {
			Boolean vote = jury != null && jury.length > i ? jury[i] : null;
			votes[AdjudicationPanel.NB_REFEREES + i] = vote;
			any = any || vote != null;
		}
		this.panelReviewInitialVotes = any ? votes : null;
	}

	/** @return the configured number of jury members */
	public int getJurySize() {
		Platform p = getPlatform();
		return p != null ? p.getEffectiveJurySize() : Competition.getCurrent().getJurySize();
	}

	/** @param jurySize 0, 3, 4 or 5; null to use the competition default */
	public void setJurySize(Integer jurySize) {
		setJurySize(jurySize, this);
	}

	private void setJurySize(Integer jurySize, Object origin) {
		Platform p = getPlatform();
		if (p == null) {
			return;
		}
		p.setJurySize(jurySize);
		PlatformRepository.saveJurySize(p);
		MQTTMonitor mqttMonitor = getMqttMonitor();
		if (mqttMonitor != null) {
			mqttMonitor.publishMqttConfig();
		}
		pushOutUIEvent(new UIEvent.JurySizeChanged(origin, this));
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
			this.logger.info("{}group {} already loaded", FieldOfPlay.getLoggingName(this), loadGroupName);
			this.recomputeRecordsMap(liftingOrder);
			this.recomputeLeadersAndRecords(liftingOrder);
			return;
		}
		this.setGroup(group);
		this.setCjStarted(false);
		this.setCjBreakDisplayed(false);
		resetDecisions();

		// When the childrenEquipment feature switch is on, the platform must carry the
		// children's plates/bars (large 2.5/5, light 5/10 bars). Enforcement is done
		// eagerly
		// via ConfigEditingFormFactory when the toggle is added (remove+re-add trick),
		// using
		// Platform.applyChildrenEquipment(). No per-session enforcement is done here.

		if (group != null) {
			// debounce spurious requests due to misconfigured client that would trigger
			// a loadGroup upon receiving a UIEvent.
			long now = System.currentTimeMillis();
			if (!this.testingMode && now - this.lastGroupLoaded < 300) {
				this.logger./**/warn("ignoring request to load group {}", group);
				return;
			}

			if (this.logger.isTraceEnabled()) {
				this.logger.trace("{}loading data for group {} [already={} forced={} from={}]",
						FieldOfPlay.getLoggingName(this),
						loadGroupName,
						alreadyLoaded,
						forceLoad,
						LoggerUtils.whereFrom());
			}
			List<Athlete> groupAthletes = AthleteRepository.findAllByGroupAndWeighIn(group, true);

			// catastrophic if weighed in but no category
			List<Athlete> brokenAthletes = groupAthletes.stream().filter(a -> a.getCategory() == null).toList();
			if (!brokenAthletes.isEmpty()) {
				pushOutUIEvent(new UIEvent.Notification(
						null, this, Notification.Level.ERROR,
						"LoadingGroup.ErrorNoCategory", 0, this,
						brokenAthletes.stream().map(a -> a.getShortName()).collect(Collectors.joining(","))));
			}

			groupAthletes = groupAthletes.stream().filter(a -> a.getCategory() != null).toList();

			// skip if session is already in progress (forceLoad == false)
			if (forceLoad && AthleteRepository.needsStartNumbers(group)) {
				this.logger.info("{}assigning start numbers for group {}", FieldOfPlay.getLoggingName(this), group);
				AthleteRepository.assignStartNumbersUnlessManual(group);
				groupAthletes = AthleteRepository.findAllByGroupAndWeighIn(group, true).stream()
						.filter(a -> a.getCategory() != null).toList();
			}

			if (groupAthletes.stream().anyMatch(a -> a.getAttemptsDone() > 0)) {
				this.sessionStarted = true;
			}
			init(groupAthletes, this.athleteTimer, this.breakTimer, alreadyLoaded);
			this.lastGroupLoaded = now;
		} else {
			this.logger.info("{}null group", FieldOfPlay.getLoggingName(this));
			init(new ArrayList<>(), this.athleteTimer, this.breakTimer, alreadyLoaded);
		}
	}

	private final AtomicLong uiEventSequence = new AtomicLong(0);

	/**
	 * @return the current value of the per-FOP UI event sequence (peek, does not
	 *         increment)
	 */
	public long getUiEventSequence() {
		return this.uiEventSequence.get();
	}

	public void pushOutUIEvent(UIEvent event) {
		// stamp a per-FOP monotonic sequence so clients can drop out-of-order timer
		// events
		event.setSequence(this.uiEventSequence.incrementAndGet());
		// TEMPORARY (display-order) trace: decide whether missed StartTime/StopTime is a
		// registration gap.
		if ((event instanceof UIEvent.StartTime || event instanceof UIEvent.StopTime)
				&& Config.getCurrent().featureSwitch(FeatureSwitch.CLOCK_TRACES)) {
			int athleteTimerSubscribers = SafeEventBusRegistration.registeredSubscriberCount(getUiEventBus(),
					"AthleteTimerElement");
			int controlAthleteTimerSubscribers = SafeEventBusRegistration
					.registeredControlAthleteTimerCount(getUiEventBus());
			this.logger.warn(
					"{}uiBus pre-post {} seq={} athleteTimerSubscribers={} controlAthleteTimerSubscribers={} timers={}",
					FieldOfPlay.getLoggingName(this), event.getClass().getSimpleName(), event.getSequence(),
					athleteTimerSubscribers, controlAthleteTimerSubscribers,
					SafeEventBusRegistration.registeredTimerDetails(getUiEventBus()));
		}
		if (event instanceof UIEvent.LiftingOrderUpdated liftingOrderUpdated
				&& Config.getCurrent().featureSwitch(FeatureSwitch.ATTEMPT_TRACES)) {
			Athlete displayAthlete = liftingOrderUpdated.getAthlete();
			Integer displayWeight = displayAthlete != null ? displayAthlete.getNextAttemptRequestedWeight() : null;
			this.logger.warn(
					"{}displayOrder post seq={} athlete={} startNumber={} weight={} requested={} changedWeight={} displayAffected={} state={}",
					FieldOfPlay.getLoggingName(this), liftingOrderUpdated.getSequence(), liftingOrderUpdated.getAthlete(),
					displayAthlete != null ? displayAthlete.getStartNumber() : null, displayWeight, displayWeight,
					liftingOrderUpdated.getNewWeight(), liftingOrderUpdated.isCurrentDisplayAffected(), this.state);
		}
		if (Config.getCurrent().featureSwitch(FeatureSwitch.PLAYWRIGHT)) {
			if (event instanceof UIEvent.LiftingOrderUpdated liftingOrderUpdated) {
				MQTTMonitor mqttMonitor = getMqttMonitor();
				if (mqttMonitor != null) {
					mqttMonitor.publishMqttPlaywrightLiftingOrder(liftingOrderUpdated);
				}
			} else if (event instanceof UIEvent.BreakStarted || event instanceof UIEvent.GroupDone) {
				MQTTMonitor mqttMonitor = getMqttMonitor();
				if (mqttMonitor != null) {
					mqttMonitor.publishMqttPlaywrightPause(event, event.getClass().getSimpleName());
				}
			}
		}
		getUiEventBus().post(event);
		getEventForwardingBus().post(event);
	}

	public boolean recomputeLiftingOrder(boolean currentDisplayAffected, boolean resultChange) {
		Competition comp = Competition.getCurrent();
		// because of recalculation of ranks, if same category is lifting on two FOPs,
		// strange things can happen
		// if done in parallel. Force sequence.
		synchronized (comp) {
			recomputeOrderAndRanks(resultChange);
			if (getCurAthlete() == null) {
				return true;
			}

			int timeAllowed = getTimeAllowed();
			Integer attemptsDone = getCurAthlete().getAttemptsDone();
			this.logger.info("{}recomputed lifting order curAthlete={} prevlifter={} time={} attemptsDone={} [{}]",
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
			getGroup().doDone();
			return done;
		}
	}

	public void recomputeRecords(Athlete curAthlete) {
		if (curAthlete == null) {
			this.recordChallengeTracker.clear();
			setRecordsJson(JsonUtils.nullNode());
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

		List<RecordEvent> eligibleRecords = this.eligibleRecordsByAthlete.getOrDefault(curAthlete, List.of());
		List<RecordEvent> displayableRecords = this.displayableRecordsByAthlete.getOrDefault(curAthlete, List.of());
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

		BaseJsonNode recordsJson = null;
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
			setRecordsJson(JsonUtils.nullNode());
			setChallengedRecords(List.of());
			setNewRecords(List.of());
		} else {
			setRecordsJson(recordsJson);
			setChallengedRecords(challengedRecords.stream().sorted(RecordEvent.sequentialOrderComparator()).toList());
			this.recordChallengeTracker
					.newlyChallenged(curAthlete, attemptsDone + 1, request, getChallengedRecords())
					.forEach(record -> RecordAudit.challenge(getName(), curAthlete, attemptsDone + 1, request, record));
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
	 * Sets the athleteTimer.
	 *
	 * @param athleteTimer the new athleteTimer
	 */
	public void setAthleteTimer(IProxyTimer timer) {
		this.athleteTimer = timer;
	}

	public void setBreakType(BreakType breakType) {
		// logger.debug("FOP {} setBreakType {} from {}", System.identityHashCode(this),
		// breakType,
		// LoggerUtils.whereFrom());
		if (breakType == null && this.breakType != null
		        && Config.getCurrent().featureSwitch(FeatureSwitch.ATTEMPT_TRACES)) {
			this.logger.warn("{}attemptTrace clearing breakType previous={} state={} group={} athlete={} from={}",
					FieldOfPlay.getLoggingName(this), this.breakType, this.state, this.group, this.curAthlete,
					LoggerUtils.whereFrom());
		}
		this.breakType = breakType;
	}

	public void setCeremonyType(CeremonyType ceremonyType) {
		this.ceremonyType = ceremonyType;
		if (ceremonyType == null) {
			this.activeCeremony = null;
		}
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

	public void setWebSocketEventForwarder(WebSocketEventForwarder webSocketEventForwarder) {
		this.webSocketEventForwarder = webSocketEventForwarder;
	}

	/**
	 * Sets the group.
	 *
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		boolean sameGroup = group != null && this.group != null
				&& (group == this.group || (group.getId() != null && group.getId().equals(this.group.getId())));
		if (!sameGroup) {
			this.sessionStarted = false;
		}
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

	public void setMedals(TreeMap<String, List<Athlete>> treeMap) {
		this.medals = treeMap;
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
			// this.logger.debug("{} + clearing athlete records {}",
			// FieldOfPlay.getLoggingName(this), LoggerUtils.whereFrom());
		}
		this.newRecords = newRecords != null
				? newRecords.stream().sorted(RecordEvent.sequentialOrderComparator()).toList()
				: new ArrayList<>();
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

	public void setRecordsJson(BaseJsonNode computedRecords) {
		this.recordsJson = computedRecords;
	}

	public void setRefereeDecision(Boolean[] refereeDecision) {
		this.refereeDecision = refereeDecision;
	}

	public void setRefereeTime(Long[] refereeTime) {
		this.refereeTime = refereeTime;
	}

	public void setSingleReferee(boolean solo) {
		logger.trace("FieldOfPlay.setSingleReferee({}) on {} {}", solo, this.getName(), LoggerUtils.whereFrom());
		this.singleReferee = solo;
	}

	/**
	 * @param testingMode true if we don't want wait delays during testing.
	 */
	public void setTestingMode(boolean testingMode) {
		this.testingMode = testingMode;
	}

	public void setDecisionTimingForTests(long minimumDownSignalVisibleMs, int reversalDelay,
			long decisionVisibleDuration, long decisionInputIgnoreWindowMs, long recordNotificationDelayMs) {
		this.minimumDownSignalVisibleMs = minimumDownSignalVisibleMs;
		this.reversalDelay = reversalDelay;
		this.decisionVisibleDuration = decisionVisibleDuration;
		this.decisionInputIgnoreWindowMs = decisionInputIgnoreWindowMs;
		this.recordNotificationDelayMs = recordNotificationDelayMs;
	}

	public void setVideoAgeGroup(AgeGroup videoAgeGroup) {
		this.videoAgeGroup = videoAgeGroup;
	}

	public void setJuryDecisionTimingForTests(long maximumVisibleMs, long holdMs) {
		this.juryDecisionVisibleMaxMs = maximumVisibleMs;
		this.juryDecisionHoldMs = holdMs;
	}

	/** Tests: the attempt boards' jury decision card has gone away (the 5 s have elapsed). */
	public void expireJuryDecisionCardForTests() {
		this.juryDecisionCardVisibleUntil = 0;
	}

	/** @return true while the attempt boards show the jury decision card of a four-member jury */
	public boolean isJuryDecisionCardShown() {
		return System.currentTimeMillis() < this.juryDecisionCardVisibleUntil;
	}

	public void setVideoChampionship(Championship videoChampionship) {
		this.videoChampionship = videoChampionship;
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
		cancelDecisionReset();
		MQTTMonitor mqttMonitor2 = this.getMqttMonitor();
		this.logger.debug("{}unregistering event forwarders and mqtt monitor {}", getLoggingName(this),
				System.identityHashCode(mqttMonitor2));
		this.fopEventBus.unregister(this);
		if (this.getEventForwarder() != null) {
			this.getEventForwarder().unregister();
			this.setEventForwarder(null);
		}
		// Also unregister WebSocket forwarder
		if (this.getWebSocketEventForwarder() != null) {
			this.getWebSocketEventForwarder().unregister();
			this.setWebSocketEventForwarder(null);
		}
		if (mqttMonitor2 != null) {
			mqttMonitor2.unregister();
		}
	}

	void emitFinalWarning() {
		if (!isAthleteFinalWarningEmitted()) {
			this.logger.info("{}Final Warning", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				this.finalWarningSound = new Sound(getSoundMixer(), Competition.athleteTimerFinalWarningSound + ".wav");
				this.finalWarningSound.emit();
			}
			pushOutUIEvent(new UIEvent.TimeRemainingSeconds(this, Competition.athleteTimerFinalWarning / 1000, this));
			setAthleteFinalWarningEmitted(true);
		}
	}

	void emitBreakFinalWarning() {
		if (!isBreakTimerSoundsEnabled()) {
			return;
		}
		if (!isBreakFinalWarningEmitted()) {
			this.logger.info("{}Break Final Warning", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				this.breakFinalWarningSound = new Sound(getSoundMixer(), Competition.breakTimerFinalWarningSound + ".wav");
				this.breakFinalWarningSound.emit();
			}
			pushOutUIEvent(new UIEvent.BreakTimeRemainingSeconds(this, Competition.breakTimerFinalWarning / 1000, this));
			setBreakFinalWarningEmitted(true);
		}
	}

	void emitInitialWarning() {
		if (!isAthleteInitialWarningEmitted()) {
			this.logger.info("{}Initial Warning", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				this.initialWarningSound = new Sound(getSoundMixer(), Competition.athleteTimerInitialWarningSound + ".wav");
				this.initialWarningSound.emit();
			}
			pushOutUIEvent(new UIEvent.TimeRemainingSeconds(this, Competition.athleteTimerInitialWarning / 1000, this));
			setAthleteInitialWarningEmitted(true);
		}
	}

	void emitBreakInitialWarning() {
		if (!isBreakTimerSoundsEnabled()) {
			return;
		}
		if (!isBreakInitialWarningEmitted()) {
			this.logger.info("{}Break Initial Warning", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				this.breakInitialWarningSound = new Sound(getSoundMixer(), Competition.breakTimerInitialWarningSound + ".wav");
				this.breakInitialWarningSound.emit();
			}
			pushOutUIEvent(new UIEvent.BreakTimeRemainingSeconds(this, Competition.breakTimerInitialWarning / 1000, this));
			setBreakInitialWarningEmitted(true);
		}
	}

	void emitTimeOver() {
		if (!isAthleteTimeoutEmitted()) {
			this.logger.info("{}Time Over", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				this.timeOverSound = new Sound(getSoundMixer(), Competition.athleteTimerTimeOverSound + ".wav");
				this.timeOverSound.emit();

			}
			pushOutUIEvent(new UIEvent.TimeRemainingSeconds(this, 0, this));
			setAthleteTimeoutEmitted(true);
		}
	}

	void emitBreakTimeOver() {
		if (!isBreakTimerSoundsEnabled()) {
			return;
		}
		if (!isBreakTimeoutEmitted()) {
			this.logger.info("{}Break Time Over", FieldOfPlay.getLoggingName(this));
			if (isEmitSoundsOnServer()) {
				this.breakTimeOverSound = new Sound(getSoundMixer(), Competition.breakTimerTimeOverSound + ".wav");
				this.breakTimeOverSound.emit();

			}
			pushOutUIEvent(new UIEvent.BreakTimeRemainingSeconds(this, 0, this));
			setBreakTimeoutEmitted(true);
		}
	}

	private boolean isBreakTimerSoundsEnabled() {
		return Config.getCurrent().featureSwitch(FeatureSwitch.BREAK_TIMER_SOUNDS);
	}

	/**
	 * Sets the state.
	 *
	 * @param state the new state
	 */
	public void setState(FOPState state) {
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

	/**
	 * Force recalculation of bar weight and light bar status based on current
	 * athlete and platform settings.
	 * Call this after changing platform equipment to ensure the FOP state is
	 * updated before displaying.
	 */
	public void recomputeBarInUse() {
		changePlatformEquipment(this.curAthlete, this.curWeight);
	}

	private void changePlatformEquipment(Athlete a, Integer newWeight) {
		// skip during unit tests or results editing
		if (getPlatform() == null || a == null) {
			return;
		}
		// Note: childrenEquipment is enforced eagerly when the toggle is added (see
		// Platform.applyChildrenEquipment), not on session load.
		// TC screen can still override settings during the session.
		AgeGroup ageGroup = a.getAgeGroup();
		Integer age = a.getAge();
		int maximumAllowedBarWeight = PlatformEquipmentCalculator.maximumAllowedBarWeight(
				a.getGender(), ageGroup != null ? ageGroup.getMaxAge() : null,
				Config.getCurrent().featureSwitch(FeatureSwitch.LIGHT_BAR_U13),
				Config.getCurrent().featureSwitch(FeatureSwitch.LIGHT_BAR_U15));
		PlatformEquipmentCalculator.Inventory inventory = new PlatformEquipmentCalculator.Inventory(
				getPlatform().getNbB_20() > 0,
				getPlatform().getNbB_15() > 0,
				getPlatform().getNbB_10() > 0,
				getPlatform().getNbB_5() > 0,
				getPlatform().getNbC_2_5() > 0,
				smallestAvailableBumperPairWeight());
		PlatformEquipmentCalculator.Request request = new PlatformEquipmentCalculator.Request(
				newWeight,
				standardBarWeight(a.getGender()),
				maximumAllowedBarWeight,
				Config.getCurrent().featureSwitch(FeatureSwitch.USAW_COLLARS) && age != null && age > 13,
				Config.getCurrent().featureSwitch(FeatureSwitch.NO_COLLARS_5KG_BAR),
				getPlatform().isUseNonStandardBar(),
				getPlatform().getNonStandardBarWeight(),
				getPlatform().getCollarThreshold(),
				inventory);
		PlatformEquipmentCalculator.Selection selection = PlatformEquipmentCalculator.select(request);

		this.logger.trace("using {}kg bar{}", selection.barWeight(), selection.useCollars() ? " with collars" : "");
		this.setBarWeight(selection.barWeight());
		this.setUseCollarsIfAvailable(selection.useCollars());
		this.setLightBarInUse(selection.lightBarInUse());
	}

	private int standardBarWeight(Gender gender) {
		return gender == Gender.M ? 20 : 15;
	}

	/** weight of the lightest available large bumper pair (both sides), 0 if none */
	private int smallestAvailableBumperPairWeight() {
		Platform platform = getPlatform();
		if (platform.getNbL_2_5() > 0) {
			return 5;
		}
		if (platform.getNbL_5() > 0) {
			return 10;
		}
		if (platform.getNbL_10() > 0) {
			return 20;
		}
		if (platform.getNbL_15() > 0) {
			return 30;
		}
		if (platform.getNbL_20() > 0) {
			return 40;
		}
		if (platform.getNbL_25() > 0) {
			return 50;
		}
		return 0;
	}

	private void checkDeferredWeightChanges() {
		if (!this.deferredWeightChanges.isEmpty()) {
			// decision reset did not happen, we have pending weight changes.
			this.logger.error("Can't happen: Weight changes during down/decision display, but no decision reset");
			recomputeLiftingOrder(true, true);
		}
	}

	private Ranking computeResultOrderRanking(boolean groupDone) {
		boolean _3medals = Championship.anyMultiMedal(getActiveChampionships());
		if (groupDone || (isCjStarted() && !_3medals)) {
			// this falls back to TOTAL for categories that are not score-based
			return Ranking.CATEGORY_SCORE;
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
		// logger.debug(" ---- displayableRecords {} {}",
		// computeShowInformationalRecords,
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
		cancelDecisionReset();
		this.logger.info("{}clearing decision lights", FieldOfPlay.getLoggingName(this));
		// the state will be rewritten in displayOrBreakIfDone
		// this is so the decision reset knows that the decision is no longer displayed.
		cancelWakeUpRef();
		if (getCurAthlete() == null && CompetitionSimulator.isRunning()) {
			this.logger.warn("{}SIMULATION creating null-athlete DecisionReset state={} previousAthlete={} liftingOrderSize={} trigger={}\n{}",
			        FieldOfPlay.getLoggingName(this), getState(), getPreviousAthlete(),
			        this.liftingOrder != null ? this.liftingOrder.size() : null,
			        e != null ? e.getClass().getSimpleName() : null, LoggerUtils.stackTrace());
		}
		pushOutUIEvent(new UIEvent.DecisionReset(getCurAthlete(), this, this));
		setClockOwner(null);
		this.decisionCommitScheduled = false;
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
		if (e.getCeremonyType() != getCeremonyType()) {
			return;
		}
		setCeremonyType(null);
		pushOutUIEvent(new UIEvent.CeremonyDone(e.getCeremonyType(), e, this));
	}

	private void endMedalCeremony(Object origin) {
		CeremonyScope scope = this.activeCeremony;
		if (scope != null && scope.isMedals()) {
			setCeremonyType(null);
			pushOutUIEvent(new UIEvent.CeremonyDone(CeremonyType.MEDALS, origin, this));
		}
	}

	private void doForceTime(FOPEvent.ForceTime e) {
		// need to set time
		int ta = e.timeAllowed;
		this.logger.debug("{}forcing time to {}", FieldOfPlay.getLoggingName(this), ta);
		// setTimeRemaining already stops the timer (sets running=false) and emits
		// UIEvent.SetTime
		getAthleteTimer().setTimeRemaining(ta, false);
		setClockOwnerInitialTimeAllowed(ta);
		setForcedTime(true);
		setState(CURRENT_ATHLETE_DISPLAYED);
	}

	private void doJuryDecision(JuryDecision e) {
		Athlete a = e.getAthlete();
		Integer actualLift = a.getActualLift(a.getAttemptsDone());
		if (actualLift != null) {
			cancelDecisionReset();
			// The Chair or Speaker supplied the jury decision; individual jury votes are no longer required.
			endPanelVotePending(e.getOrigin());
			Integer curValue = Math.abs(actualLift);

			// The Chair used the Good Lift / No Lift buttons (Jury Control or jury box) instead of the result of a jury
			// vote. A button decision equal to the result of a completed vote (Confirm Good Lift, or the rule given
			// for a No Lift result) is the vote's decision.
			boolean matchesVote = this.panelVoteMajority != null && this.panelVoteMajority.equals(e.success);
			boolean overridesVote = hasJuryReviewVote() && e.isJuryButton() && e.isChairEntered()
			        && !matchesVote;
			// only with the seven-person panel must the decision come from the vote (TCRR 7.5.7): mark the override
			boolean chairEntered = overridesVote && isPanelProcedure();
			this.juryRuleRequested = false;
			if (overridesVote) {
				// the Chair's decision replaces whatever the jury had voted: clear the votes everywhere
				clearJuryVotesForChairDecision(e.getOrigin());
			} else if (this.panelVoteOpen) {
				if (this.panelVoteMajority != null && !this.panelVotePubliclyRevealed) {
					// public boards reveal the complete vote together with the decision (and rule for a No Lift)
					revealPanelVotePublicly(e.getOrigin());
				}
				// stop collecting votes; later votes are ignored
				this.panelVoteOpen = false;
				pushPanelVoteUpdate(e.getOrigin());
			}

			boolean reversalToGood = e.success && actualLift <= 0;
			boolean reversalToBad = !e.success && actualLift > 0;
			boolean newRecord = e.success && getLastChallengedRecords() != null
					&& !getLastChallengedRecords().isEmpty();

			// No jury decision waits for the Speaker: it is applied at once, and the Speaker announces it
			// (section 13 of the seven-person adjudication design).
			JuryNotification juryNotificationEvent = new UIEvent.JuryNotification(a, e.getOrigin(),
					e.success ? JuryDeliberationEventType.GOOD_LIFT : JuryDeliberationEventType.BAD_LIFT,
					reversalToGood || reversalToBad, newRecord,
					false, this, (e.success ? 1 : -1) * Math.abs(actualLift));
			juryNotificationEvent.setReasonCode(e.getReasonCode());
			juryNotificationEvent.setChairEntered(chairEntered);
			juryNotificationEvent.setSpeakerEntered(!e.isJuryButton());
			juryNotificationEvent.setPanelDecision(isPanelProcedure() && !chairEntered);
			// the votes as they stand now (cleared after a Chair override); listeners may run after they are reset
			juryNotificationEvent.setReviewVotes(this.reviewRefereeDecision, getJuryMemberDecision());

			// must set state before recomputing order so that scoreboards stop blinking the
			// current athlete
			// must also set state prior to sending event, so that state monitor shows new
			// state.
			setGoodLift(e.success);
			setState(DECISION_VISIBLE);
			this.juryDecisionShown = true;
			if (isFourMemberJury()) {
				this.juryDecisionCardVisibleUntil = System.currentTimeMillis() + JURY_DECISION_CARD_MS;
			}
			this.logger.info("{}jury decision {} for {}{}", FieldOfPlay.getLoggingName(this),
			        e.success ? "good lift" : "no lift", a, chairEntered ? " (entered by the Jury Chair)"
			                : (e.isJuryButton() ? "" : " (entered by the Speaker)"));
			this.panelVoteMajority = null;

			// Apply the reversal/confirmation to the athlete BEFORE sending the
			// notification
			// so attempt board shows updated result
			int attempt = a.getAttemptsDone();
			a.setActualLift(attempt, e.success ? Integer.toString(curValue) : Integer.toString(-curValue));
			if (getBreakType() == BreakType.CHALLENGE) {
				a.setChallenge(attempt, e.success);
			} else if (getBreakType() == BreakType.JURY) {
				a.setJuryDeliberation(attempt, e.success);
			}
			AthleteRepository.save(a);

			// reversal from bad to good should add records
			// reversal from good to bad must remove records
			setNewRecords(updateRecords(a, e.success, getLastChallengedRecords(), getLastNewRecords()));

			recomputeLiftingOrder(true, true);

			// Now send the notification with the updated data
			pushOutUIEvent(juryNotificationEvent);

			// tell ourself to reset after 3 secs.
			new DelayTimer(isTestingMode()).schedule(() -> {
				// fopEventPost(new DecisionReset(this));
				if (reversalToGood) {
					// the new record notification shows the records that were broken
					notifyRecords(getLastChallengedRecords(), true, curValue);
					setLastNewRecords(getNewRecords());
				}
				synchronized (FieldOfPlay.this) {
					// a clock start may already have ended the display
					if (this.juryDecisionShown && this.state == DECISION_VISIBLE) {
						fopEventPost(new StartLifting(this));
					}
				}
			}, this.decisionVisibleDuration);

		}
	}

	private void doJuryMemberDecisionUpdate(FOPEvent.JuryMemberDecisionUpdate e) {
		// devices may have more buttons than the current jury has members (e.g. station 5 on a 5-station box)
		if (e.refIndex < 0 || e.refIndex >= getJurySize()) {
			this.logger.debug("{}ignoring jury member decision, juror {} is beyond jury size {}",
			        FieldOfPlay.getLoggingName(this), e.refIndex + 1, getJurySize());
			return;
		}
		if (this.panelVoteOpen && this.panelVoteMajority != null) {
			// the result of the review vote is locked: only Cancel vote result or a new call reopens the voting
			this.logger.info("{}review vote already complete, juror {} vote ignored",
			        FieldOfPlay.getLoggingName(this), e.refIndex + 1);
			return;
		}
		getJuryMemberDecision()[e.refIndex] = e.decision;
		this.juryMemberTime[e.refIndex] = 0;
		processJuryMemberDecisions(e.origin, e.refIndex);
		rescheduleDecisionResetForJury();
		evaluatePanel();
		if (this.panelVoteOpen) {
			checkPanelVoteComplete(e.origin, null);
		}
	}

	private void doPossiblySoloRefereeUpdate(FOPEvent e) {
		DecisionUpdate du = (DecisionUpdate) e;
		if (du.getRefIndex() < 0) {
			this.currentInputKind = InputKind.ANNOUNCER_ENTRY;
			boolean goodLift = du.isDecision();
			simulateDecision(new ExplicitDecision(e.getAthlete(), e.getStackTrace(), true, // announcer decisions are
																							// always immediate
					goodLift, goodLift, goodLift));
		} else if (isSingleReferee()) {
			if (Config.getCurrent().featureSwitch(FeatureSwitch.PLAYWRIGHT)) {
				this.logger./*playwright*/warn("{}solo referee decision path refIndex={} decision={} state={} {}",
						FieldOfPlay.getLoggingName(this), du.getRefIndex(), du.isDecision(), this.state,
						LoggerUtils.whereFrom());
			}
			boolean goodLift = du.isDecision();
			long now = System.currentTimeMillis();
			notifyDecisionWithoutClock(e.getOrigin());
			setClockOwner(null);
			if (getAthleteTimer().isRunning()) {
				getAthleteTimer().stop();
			}
			setPreviousAthlete(e.getAthlete());
			normalizeSoloDecision(goodLift, now);
			processRefereeDecisions(e);
			uiShowUpdateOnJuryScreen(e);
		} else {
			if (this.currentInputKind == null) {
				this.currentInputKind = InputKind.THREE_REFEREE_INPUT;
			}
			updateRefereeDecisions(du);
			uiShowUpdateOnJuryScreen(e);
		}
	}

	private void doSetState(FOPState state) {
		if (state != DECISION_VISIBLE) {
			cancelDecisionReset();
		}
		if (state == CURRENT_ATHLETE_DISPLAYED) {
			Athlete a = getCurAthlete();
			if (getGroup() != null) {
				boolean lastLiftDone = a == null || a.getAttemptsDone() >= 6;
				getGroup().doDone();
				// special case for 0 on last lift, there willl be no decision, group is really
				// done
				if (lastLiftDone && a != null && a.getActualLift(6) == 0) {
					state = BREAK;
					this.breakType = BreakType.GROUP_DONE;
				}
			}
		} else if (state == BREAK && getGroup() != null) {
			getGroup().doDone();
		}
		this.state = state;
	}

	private void doStartCeremony(CeremonyStarted e) {
		if (e.getCeremony() != CeremonyType.MEDALS) {
			endMedalCeremony(e.getOrigin());
		}
		this.activeCeremony = new CeremonyScope(e.getCeremony(), e.getCeremonyGroup(), e.getCeremonyChampionship(),
		        e.getCeremonyAgeGroup(), e.getCeremonyCategory());
		setCeremonyType(e.getCeremony());
		setVideoGroup(e.getCeremonyGroup());
		setVideoCategory(e.getCeremonyCategory());
		setVideoAgeGroup(e.getCeremonyAgeGroup());
		setVideoChampionship(e.getCeremonyChampionship());
		UIEvent.CeremonyStarted event = new UIEvent.CeremonyStarted(e.getCeremony(), e.getCeremonyGroup(),
				e.getCeremonyCategory(), e.getStackTrace(), e.getOrigin(), this);
		event.setAgeGroup(e.getCeremonyAgeGroup());
		event.setChampionship(e.getCeremonyChampionship());
		pushOutUIEvent(event);
	}

	private void doSummonReferee(SummonReferee e) {
		if (e.getRefNumber() >= 4) {
			JuryNotification event = new UIEvent.JuryNotification(null, e.getOrigin(),
					JuryDeliberationEventType.CALL_TECHNICAL_CONTROLLER, null, null, false, this, null);
			pushOutUIEvent(event);
		} else {
			JuryNotification event = new UIEvent.JuryNotification(null, this, JuryDeliberationEventType.CALL_REFEREES,
					null, null, false, this, null);
			pushOutUIEvent(event);
		}
		pushOutUIEvent(new UIEvent.SummonRef(e.getRefNumber(), true, this, this));
	}

	private void doTONotifications(BreakType newBreak) {
		// logger.debug("doTONotifications {}\n{}", newBreak, LoggerUtils.stackTrace());
		this.panelReviewInitialVotes = null;
		if (newBreak == null) {
			// resuming
			this.panelReview = false;
			this.juryReview = false;
			if (this.state == BREAK) {
				switch (this.breakType) {
					case JURY:
					case MARSHAL:
					case TECHNICAL:
						pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
								JuryDeliberationEventType.END_JURY_BREAK, null, null, false, this, null));
						break;
					case CHALLENGE:
						pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
								JuryDeliberationEventType.END_CHALLENGE, null, null, false, this, null));
					default:
						break;
				}
			}
		} else {
			if (newBreak != BreakType.JURY) {
				this.panelReview = false;
				this.juryReview = false;
			}
			if (newBreak != BreakType.JURY && newBreak != BreakType.CHALLENGE) {
				closePanelVote(this);
			}
			switch (newBreak) {
				case JURY:
					logger.debug("{}Jury break notification", FieldOfPlay.getLoggingName(this));
					if (this.panelReview) {
						// Adjudication Panel Review: the jury votes are kept (they are cleared when the vote is
						// called) and all referees are called to the video review, as with the Referee call buttons.
						JuryNotification panelNotification = new UIEvent.JuryNotification(this.athleteUnderReview,
						        this, JuryDeliberationEventType.START_DELIBERATION, null, null, false, this, null);
						panelNotification.setPanelReview(true);
						capturePanelReviewInitialVotes();
						pushOutUIEvent(panelNotification);
						pushOutUIEvent(new UIEvent.JuryNotification(null, this, JuryDeliberationEventType.CALL_REFEREES,
						        null, null, false, this, null));
						pushOutUIEvent(new UIEvent.SummonRef(0, true, this, this));
						break;
					}
					if (this.juryReview) {
						// without VPT: a unanimous jury stopped the competition. The jury votes are kept (they are
						// cleared when the Chair calls for votes). Summoning the referees is the Chair's decision
						// (7.5.8), so no automatic SummonRef here; Jury Control prompts for it.
						capturePanelReviewInitialVotes();
						pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
						        JuryDeliberationEventType.START_DELIBERATION, null, null, false, this, null));
						break;
					}
					// deliberation started by the Chair: no automatic panel evaluation
					endPanelVotePending(this);
					if (isPanelProcedure()) {
						// seven-person panel: the votes on the lift stay displayed until the Chair calls the vote
						// (Summon All), which re-arms the referees
						capturePanelReviewInitialVotes();
					} else {
						resetJuryDecisions();
					}
					pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
							JuryDeliberationEventType.START_DELIBERATION, null, null, false, this, null));
					openJuryVoteAtReviewStart();
					break;
				case MARSHAL:
					pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
							JuryDeliberationEventType.MARSHALL, null, null, false, this, null));
					break;
				case TECHNICAL:
					pushOutUIEvent(new UIEvent.JuryNotification(null, this,
							JuryDeliberationEventType.TECHNICAL_PAUSE, null, null, false, this, null));
					break;
				case CHALLENGE:
					// a challenge follows the same procedure as the corresponding jury break; only the trigger and
					// the label differ
					endPanelVotePending(this);
					if (isPanelProcedure()) {
						// with VPT: same as the Adjudication Panel Review. The lift's votes are kept and shown until
						// the Chair calls for votes, and all referees are called to the video review.
						capturePanelReviewInitialVotes();
						pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
								JuryDeliberationEventType.CHALLENGE, null, null, false, this, null));
						pushOutUIEvent(new UIEvent.JuryNotification(null, this,
						        JuryDeliberationEventType.CALL_REFEREES, null, null, false, this, null));
						pushOutUIEvent(new UIEvent.SummonRef(0, true, this, this));
					} else {
						// without the seven-person panel (with or without VPT): same as a jury deliberation opened
						// by the Chair
						resetJuryDecisions();
						pushOutUIEvent(new UIEvent.JuryNotification(this.athleteUnderReview, this,
								JuryDeliberationEventType.CHALLENGE, null, null, false, this, null));
						openJuryVoteAtReviewStart();
					}
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Perform weight change and adjust state.
	 *
	 * If the clock was started and we come back to the clock owner, we set the
	 * state to TIME_STARTED If in a break, we are careful not to update, unless the
	 * change causes an exit from the break (e.g. jury overrule on last lift)
	 * Otherwise we update the displays.
	 *
	 * @param wc
	 */
	private void doWeightChange(WeightChange wc) {
		long start = System.nanoTime();
		boolean resultChange = wc.isResultChange();
		String reason = "";
		Athlete changingAthlete = wc.getAthlete();

		Integer newWeight = changingAthlete.getNextAttemptRequestedWeight();
		// logger.debug("1 cur={} curWeight={} changing={} newWeight={}",
		// getCurAthlete(), curWeight, changingAthlete,
		// newWeight);
		// logger.debug("2 clockOwner={} clockLastStopped={} state={}", getClockOwner(),
		// getAthleteTimer().getTimeRemainingAtLastStop(), state);

		boolean stopAthleteTimer = false;
		if (getClockOwner() != null && getAthleteTimer().isRunning()) {
			// time is running
			if (changingAthlete.equals(getClockOwner())) {
				reason = "1";
				// logger.trace("3.A clock IS running for changing athlete {}",
				// changingAthlete);
				// X is the current lifter
				// if a real change (and not simply a declaration that does not change weight),
				// make sure clock is stopped.
				if (this.curWeight != newWeight) {
					reason = "2";
					// logger.trace("3.A.A1 weight change for clock owner: clock running: stop
					// clock");
					getAthleteTimer().stop(); // memorize time
					stopAthleteTimer = true; // make sure we broacast to clients
					doWeightChange(wc, changingAthlete, getClockOwner(), stopAthleteTimer);
				} else {
					reason = "3";
					// logger.trace("3.A.B declaration at same weight for clock owner: leave clock
					// running");
					// no actual weight change. this is most likely a declaration.
					// we do the call to trigger a notification on official's screens, but request
					// that the clock keep running
					doWeightChange(wc, changingAthlete, getClockOwner(), false);
					// return;
				}
			} else {
				reason = "4";
				// logger.trace("3.B clock running, but NOT for changing athlete, do not
				// update attempt board");
				weightChangeDoNotDisturb(wc);
				// return;
			}
		} else if (getClockOwner() != null && !getAthleteTimer().isRunning()) {
			reason = "5";
			// logger.trace("3.B clock NOT running for changing athlete {}",
			// changingAthlete);
			// time was started (there is an owner) but is not currently running
			// time was likely stopped by timekeeper because coach signaled change of weight
			doWeightChange(wc, changingAthlete, getClockOwner(), true);
		} else {
			reason = "6";
			// logger.trace("3.C1 no clock owner, time is not running");
			// time is not running
			recomputeLiftingOrder(true, wc.isResultChange());

			setStateUnlessInBreak(CURRENT_ATHLETE_DISPLAYED);
			// logger.trace("3.C2 displaying, curAthlete={}, state={}", getCurAthlete(),
			// state);

			// send an update even in a break (announcer/marshall need to refresh)
			uiDisplayCurrentAthleteAndTime(true, wc, false);
		}
		if (this.timingLogger.isDebugEnabled()) {
			this.timingLogger.debug("{}doWeightChange {} {} {}", FieldOfPlay.getLoggingName(this),
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
		this.logger.trace("3.X change for {}, new cur = {}, displayAffected = {}, switching to {}", changingAthlete,
				getCurAthlete(), currentDisplayAffected, newState);
		setStateUnlessInBreak(newState);
		uiDisplayCurrentAthleteAndTime(currentDisplayAffected, wc, false);
	}

	private void emitDown(FOPEvent e) {
		// this.logger.debug("{}Emitting down {}", FieldOfPlay.getLoggingName(this),
		// LoggerUtils.whereFrom(2));
		getAthleteTimer().stop(); // paranoia
		this.setPreviousAthlete(getCurAthlete()); // would be safer to use past lifting order
		setClockOwner(null); // athlete has lifted, time does not keep running for them
		setClockOwnerInitialTimeAllowed(0);
		this.downSignalVisibleSince = System.currentTimeMillis();
		uiShowDownSignalOnSlaveDisplays(e.origin);
		setState(DOWN_SIGNAL_VISIBLE);
	}

	private List<RecordEvent> getLastNewRecords() {
		return this.lastNewRecords;
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

	private synchronized boolean isAthleteFinalWarningEmitted() {
		return this.athleteFinalWarningEmitted;
	}

	private synchronized boolean isBreakFinalWarningEmitted() {
		return this.breakFinalWarningEmitted;
	}

	private boolean isForcedTime() {
		return this.forcedTime;
	}

	private synchronized boolean isAthleteInitialWarningEmitted() {
		return this.athleteInitialWarningEmitted;
	}

	private synchronized boolean isBreakInitialWarningEmitted() {
		return this.breakInitialWarningEmitted;
	}

	private synchronized boolean isBreakTimeoutEmitted() {
		return this.breakTimeoutEmitted;
	}

	private boolean shouldIgnoreDecisionInputDuringCooldown(FOPEvent e) {
		if (isTestingMode() || CompetitionSimulator.isRunning()) {
			return false;
		}
		if (this.decisionCommitScheduled) {
			return false;
		}
		if (this.panelVoteOpen) {
			// the Jury Chair has called the review vote: the referees are asked for a new decision
			return false;
		}
		long now = System.currentTimeMillis();
		if (now >= this.ignoreDecisionInputsUntil) {
			return false;
		}
		if (!(e instanceof DecisionUpdate) && !(e instanceof DecisionFullUpdate) && !(e instanceof ExplicitDecision)) {
			return false;
		}
		this.logger.info("{}ignoring decision input during post-decision cooldown state={} remaining={}ms event={}",
				FieldOfPlay.getLoggingName(this), this.state, this.ignoreDecisionInputsUntil - now, e);
		return true;
	}

	private void notifyRecords(List<RecordEvent> records, boolean newRecord, int newRecordValue) {
		if (records == null || records.isEmpty()) {
			return;
		}
		String title = Translator.translate("Scoreboard." + (newRecord ? "NewRecord(s)" : "RecordAttempt(s)"),
				records.size());
		String name = newRecord ? getAthleteUnderReview().getFullName() : curAthlete.getFullName();
		title = Translator.translate("RecordNotification.Title", title, newRecordValue, name);
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (RecordEvent rec : records) {
			if (i > 0) {
				sb.append("<br>");
			}
			i++;
			StringBuilder recordName = new StringBuilder();
			recordName.append(rec.getRecordName());
			recordName.append("\u00A0");
			recordName.append(Translator.translate("Record." + rec.getRecordLift().name()));
			recordName.append("\u00A0");
			recordName.append(rec.getAgeGrp());
			recordName.append("\u00A0");
			recordName.append(rec.getBwCatString());
			recordName.append("\u00A0");

			String recordValue = Long.toString(Math.round(rec.getRecordValue()));
			sb.append(Translator.translate("RecordNotification." + (newRecord ? "New" : "Attempt"),
					recordName.toString(), recordValue));

			String date = rec.getRecordDateAsString();
			String holder = rec.getResAthleteName();
			boolean okDate = date != null && !date.isBlank();
			boolean okHolder = holder != null && !holder.isBlank();
			// always show the challenged/broken records; values go through translate so they are cleaned
			if (okDate && okHolder) {
				sb.append(" ").append(Translator.translate("RecordNotification.DateAndHolder", date, holder));
			} else if (okDate || okHolder) {
				sb.append(" ").append(Translator.translate("RecordNotification.DateOrHolder", okDate ? date : holder));
			}
		}
		pushOutUIEvent(
				new UIEvent.RecordNotification(
						this.getCurAthlete(),
						this,
						newRecord ? UIEvent.RecordNotification.Level.SUCCESS : UIEvent.RecordNotification.Level.INFO,
						title,
						sb.toString(),
						newRecord ? 10000 : 0,
						newRecord,
						this));
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
	 * events resulting from decisions received so far (down signal, stopping timer,
	 * all decisions entered, etc.)
	 *
	 * @param refIndex
	 */
	private void processJuryMemberDecisions(Object origin, int refIndex) {
		this.logger.debug("process jury member decisions {} {} {} {}", getJurySize(),
				getJuryMemberDecision()[0], getJuryMemberDecision()[1], getJuryMemberDecision()[2]);
		int jurySize = getJurySize();
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
		// during the review vote, the jury votes are revealed with the referees' when all seven are in
		if (nbDecisions == jurySize && !this.panelVoteOpen) {
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
	 * Verdict implied by the jury votes for a 5-person jury using simple majority.
	 */
	private Boolean juryVerdict(int reds, int whites, int jurySize) {
		if (whites > jurySize / 2) {
			return Boolean.TRUE;
		}
		if (reds > jurySize / 2) {
			return Boolean.FALSE;
		}
		return null;
	}

	/**
	 * Returns the majority referee decision, or null if the original referee decision
	 * is not available.
	 */
	private Boolean refereeMajority(Boolean[] refereeDecisions) {
		return AdjudicationPanel.refereeMajority(refereeDecisions);
	}

	private boolean juryDeliberationInProgress() {
		BreakType bt = getBreakType();
		return bt == BreakType.JURY || bt == BreakType.CHALLENGE;
	}

	/**
	 * Compute the effective auto-jury verdict given jury vote counts and the referee
	 * decision under review, used as fallback when the jury is not unanimous:
	 * <ul>
	 *   <li>3-person jury, unanimous white: good lift.</li>
	 *   <li>3-person jury, unanimous red: bad lift.</li>
	 *   <li>3-person jury, not unanimous: referee decision stands (referee majority).</li>
	 *   <li>5-person jury with VPT: simple majority wins.</li>
	 *   <li>5-person jury without VPT: unanimity required; a non-unanimous result
	 *       confirms the referee decision (same as 3-person non-unanimous rule).</li>
	 *   <li>4-person jury without VPT (7.5.8): unanimity required, same as above.</li>
	 *   <li>4-person jury with VPT: not used, the seven-person panel review vote decides.</li>
	 * </ul>
	 * Returns null if the fallback referee decision is unavailable.
	 */
	private Boolean effectiveJuryVerdict(int reds, int whites, int jurySize,
	        Boolean[] refereeDecisions) {
		if (jurySize == 3) {
			if (whites == jurySize) {
				return Boolean.TRUE;
			}
			if (reds == jurySize) {
				return Boolean.FALSE;
			}
			// Non-unanimous 3-person jury: original referee decision stands.
			return refereeMajority(refereeDecisions);
		}
		// 5-person jury (or 4-person jury without VPT, which always requires unanimity). A 4-person jury with VPT
		// is the seven-person Adjudication Panel, counted by AdjudicationPanel instead.
		// With Video Playback Technology, a majority suffices.
		// Without VPT, unanimity is required; a non-unanimous outcome confirms the
		// referee decision (same principle as the 3-person non-unanimous rule).
		if (Competition.getCurrent().isVideoPlaybackTechnology()) {
			return juryVerdict(reds, whites, jurySize);
		}
		// No VPT: require unanimity.
		if (whites == jurySize) {
			return Boolean.TRUE;
		}
		if (reds == jurySize) {
			return Boolean.FALSE;
		}
		// Non-unanimous without VPT: referee decision stands.
		return refereeMajority(refereeDecisions);
	}

	/**
	 * Tally the current jury votes and compute the effective verdict. Returns null if
	 * not all jurors have voted yet, or if the referee-decision fallback is unavailable.
	 */
	private Boolean computeCurrentJuryVerdict(int jurySize) {
		Boolean[] votes = getJuryMemberDecision();
		int reds = 0, whites = 0, decisions = 0;
		for (int i = 0; i < jurySize; i++) {
			if (votes != null && votes[i] != null) {
				if (votes[i]) {
					whites++;
				} else {
					reds++;
				}
				decisions++;
			}
		}
		if (decisions != jurySize) {
			return null; // not all voted yet
		}
		return effectiveJuryVerdict(reds, whites, jurySize, getRefereeDecision());
	}

	/**
	 * events resulting from decisions received so far (down signal, stopping timer,
	 * all decisions entered, etc.)
	 */
	private void processRefereeDecisions(FOPEvent e) {
		// logger.debug("process referee decisions");
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
		this.logger.info("{}referee decisions processed: event={} ref1={} ref2={} ref3={} red={} white={} total={}",
				FieldOfPlay.getLoggingName(this), e.getClass().getSimpleName(), getRefereeDecision()[0],
				getRefereeDecision()[1], getRefereeDecision()[2], nbRed, nbWhite, nbDecisions);
		setGoodLift(null);
		// normally announcer entry displays the decision immedeiately. 
		// It skips down and initial decision because it occurs when flags were used.
		// if ANNOUNCER_TRIGGERS_INITIAL_DECISION is set, we go on, so a video processor watching INITIAL_DECISION
		// events will show good lift/bad lift even on a flag decision
		if (this.currentInputKind == InputKind.ANNOUNCER_ENTRY
				&& !Config.getCurrent().featureSwitch(FeatureSwitch.ANNOUNCER_TRIGGERS_INITIAL_DECISION)) {
			setGoodLift(nbWhite >= 1);
			showDecisionNow(e.getOrigin());
			return;
		}
		// Note: ANNOUNCER_ENTRY and SOLO_INPUT both normalize their single decision to
		// 3 identical votes
		// (all white or all red), so nbDecisions == 3 and the majority threshold below
		// is always immediately met.
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
			if (!this.downEmitted) {
				emitDown(e);
				this.downEmitted = true;
			}
			// Notify announcer/timekeeper if decision was received without clock
			notifyDecisionWithoutClock(e.getOrigin());
			setGoodLift(nbWhite >= 2);
			// logger.debug("3 decisions");
			processDecisionDelay(e);
		}
	}

	public void processDecisionDelay(FOPEvent e) {
		if (!isDecisionDisplayScheduled()) {
			// logger.debug("not scheduled");
			if (e instanceof FOPEvent.DecisionFullUpdate) {
				if (((FOPEvent.DecisionFullUpdate) e).isImmediate()) {
					// logger.debug("is Immediate, full update NOW");
					showDecisionNow(e.getOrigin());
				} else {
					// Referee-originated decisions always keep the reversal window before
					// becoming official (DECISION_VISIBLE).
					emitInitialDecisionEvent(e.getOrigin());
					if (isShowDecisionsImmediately()) {
						showDecisionVisibleAfterDelay(e.getOrigin(), decisionDisplayDelayAfterInitialDecision());
						commitDecisionAfterDelay(e.getOrigin(), this.reversalDelay);
					} else {
						showDecisionAfterDelay(e.getOrigin(), this.reversalDelay);
					}
				}
			} else {
				// logger.debug("partial update scheduling");
				emitInitialDecisionEvent(this);
				if (isShowDecisionsImmediately()) {
					showDecisionVisibleAfterDelay(this, decisionDisplayDelayAfterInitialDecision());
					commitDecisionAfterDelay(this, this.reversalDelay);
				} else {
					showDecisionAfterDelay(this, this.reversalDelay);
				}
			}
		} else {
			// logger.debug("already scheduled");
		}
	}

	private int decisionDisplayDelayAfterInitialDecision() {
		if (!isShowDecisionsImmediately()) {
			return this.reversalDelay;
		}
		long visibleSince = this.downSignalVisibleSince;
		long visibleForMs = visibleSince > 0 ? System.currentTimeMillis() - visibleSince : 0;
		long remainingMs = Math.max(0L, this.minimumDownSignalVisibleMs - visibleForMs);
		return Math.toIntExact(remainingMs);
	}

	private void emitInitialDecisionEvent(Object origin) {
		Boolean[] refereeDecisions = getRefereeDecision();
		int nbWhite = 0;
		for (int i = 0; i < 3; i++) {
			nbWhite = nbWhite + (Boolean.TRUE.equals(refereeDecisions[i]) ? 1 : 0);
		}
		InputKind inputKind = this.currentInputKind;
		boolean singleRef = inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT;
		Boolean goodLift = singleRef ? (nbWhite >= 1) : (nbWhite >= 2);
		TimingPolicy timingPolicy = isShowDecisionsImmediately() ? TimingPolicy.IMMEDIATE : TimingPolicy.DELAYED;
		this.logger.info("{}INITIAL_DECISION inputKind={} goodLift={} singleRef={} timingPolicy={} refs=[{},{},{}]",
				FieldOfPlay.getLoggingName(this), inputKind, goodLift, singleRef, timingPolicy,
				refereeDecisions[0], refereeDecisions[1], refereeDecisions[2]);
		pushOutUIEvent(new UIEvent.InitialDecision(getCurAthlete(), goodLift,
				refereeDecisions[0], refereeDecisions[1], refereeDecisions[2],
				origin, this, singleRef, timingPolicy, inputKind));
	}

	private void pushOutDone() {
		this.logger.info("{}group {} done", FieldOfPlay.getLoggingName(this), getGroup());
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
		this.logger.trace("{}Snatch Done {}", FieldOfPlay.getLoggingName(this), cCur.isAutomaticCJBreak());
		if (!cCur.isAutomaticCJBreak()) {
			return;
		}

		int millisRemaining;
		if (CompetitionSimulator.isRunning() || ProtocolScenario.isRunning()) {
			// simulations do not wait the session's clean & jerk break minutes
			millisRemaining = 10 * 1000;
		} else {
			millisRemaining = group.cjBreakDuration();
			if (millisRemaining <= 0) {
				return;
			}
		}

		if (this.state == BREAK && getBreakType() == FIRST_CJ) {
			// already in the break
			return;
		}

		this.logger.info("{}group {} snatch done, break duration {}s", FieldOfPlay.getLoggingName(this), getGroup(),
				millisRemaining / 1000);

		int timeRemaining = millisRemaining;
		SettingsAudit.event(getName(), "break.start", AuditActor.system(), null, null, null, null,
				AuditFormat.kvs("type", BreakType.FIRST_CJ, "countdown", CountdownType.DURATION,
						"remaining", FopAudit.formatClock(timeRemaining)) + ",automatic");
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
	 * If she is lifting, we show youth lifter rankings, and include her if in the
	 * top 3 youth. If a Junior is lifting, Y needs to be ranked as a junior, and
	 * include her if in top 3 juniors If a Senior is lifting, Y needs to be ranked
	 * as a senior, and include her if in top 3 seniors
	 *
	 * So we need to fetch the PAthlete that reflects each athlete's participation
	 * in the current lifter's registration category. Ouch.
	 *
	 * @param rankedAthletes
	 */
	private void recomputeCurrentLeaders(List<Athlete> rankedAthletes) {
		if (rankedAthletes == null || rankedAthletes.size() == 0) {
			setLeaders(null);
			return;
		}

		if (getCurAthlete() != null && getCurAthlete().getAgeGroup() != null
				&& getCurAthlete().getAgeGroup().getComputedScoringSystem() != Ranking.TOTAL) {
			// compute leaders according to score.
			Category category = getCurAthlete().getCategory();
			List<Athlete> scoreMedalists = getMedals().get(category.getCode());

			if (!Config.getCurrent().featureSwitch(FeatureSwitch.MEDALISTS_AS_LEADERS)) {
				previousGroupScoreLeaders(scoreMedalists);
			} else {
				medalistScoreLeaders(scoreMedalists);
			}
		} else if (getCurAthlete() != null) {
			Category category = getCurAthlete().getCategory();
			List<Athlete> medalists = getMedals().get(category.getCode());
			logger.info("{}medalists for category {} : {}", FieldOfPlay.getLoggingName(this), category.getCode(),
			        getMedals().keySet());

			if (!Config.getCurrent().featureSwitch(FeatureSwitch.MEDALISTS_AS_LEADERS)) {
				previousGroupLeaders(medalists);
			} else {
				medalistLeaders(medalists);
			}
		} else {
			setLeaders(null);
		}
		if (getLeaders() == null) {
			return;
		}
		List<Athlete> nLeaders = new ArrayList<Athlete>(getLeaders().size());
		for (Athlete a : getLeaders()) {
			// the leaders come from the medals computation. This does not reflect the last
			// requested weight changes
			// fetch the most up-to-date info for athletes in the current group.
			Athlete found = null;
			for (Athlete curGroupAthlete : getLiftingOrder()) {
				if (curGroupAthlete.getId().equals(a.getId())) {
					found = curGroupAthlete;
					break;
				}
			}
			nLeaders.add(found != null ? found : a);
		}
		setLeaders(nLeaders);
	}

	public void medalistLeaders(List<Athlete> medalists) {
		if (medalists == null) {
			logger.error("medalistLeaders called with null medalists");
			medalists = Collections.emptyList();
		}
		if (isCurrentCategoryMultiMedal()) {
			// holding a medal on any lift is the sole inclusion criterion; zero-total
			// medalists sort into the lower tiers.
			setLeaders(multiMedalLeaders(medalists));
			return;
		}
		List<Athlete> leaders = medalists.stream()
				.filter(this::hasNonZeroTotal)
				.filter(a -> isRankInRange(a.getTotalRank(), 3))
				.sorted((a, b) -> Integer.compare(a.getTotalRank(), b.getTotalRank()))
				.collect(Collectors.toList());
		if (leaders.isEmpty()) {
			// nobody in the athlete's registration category has a total yet: the fake
			// snatch podium shows the current lead even though no snatch medal is awarded.
			leaders = snatchMedalLeaders(medalists);
		}
		setLeaders(leaders);
	}

	public void previousGroupLeaders(List<Athlete> medalists) {
		if (medalists == null) {
			medalists = Collections.emptyList();
		}
		medalists = medalists.stream().filter(m -> {
			Group group2 = this.getGroup();
			Group group3 = m.getGroup();
			return group2 != null && group3 != null && !group3.equals(group2);
		}).toList();
		var started = medalists.stream()
				.filter(this::hasStarted)
				.toList();
		if (isCurrentCategoryMultiMedal()) {
			// prior-session athletes are done lifting; the tiered order handles bombed
			// snatches and bombed CJs without any phase distinction.
			setLeaders(multiMedalLeaders(started));
			return;
		}
		var eligible = started.stream()
				.filter(this::hasNonZeroTotal)
				.toList();
		List<Athlete> leaders;
		if (Config.getCurrent().featureSwitch(FeatureSwitch.PREVIOUS_SESSION_MEDALS_ONLY)) {
			leaders = started.stream()
					.filter(a -> isRankInRange(a.getTotalRank(), 3))
					.sorted((a, b) -> Integer.compare(a.getTotalRank(), b.getTotalRank()))
					.collect(Collectors.toList());
		} else {
			leaders = eligible.stream()
					.sorted((a, b) -> Integer.compare(rankOrMax(a.getTotalRank()), rankOrMax(b.getTotalRank())))
					.limit(3)
					.collect(Collectors.toList());
		}
		setLeaders(leaders);
	}

	private boolean hasNonZeroTotal(Athlete a) {
		Integer total = a.getTotal();
		return total != null && total > 0;
	}

	/**
	 * Awarding of snatch/CJ/total medals is a property of the current athlete's registration category, not of the field of play as a whole.
	 */
	public boolean isCurrentCategoryMultiMedal() {
		AgeGroup ageGroup = getCurAthlete() != null ? getCurAthlete().getAgeGroup() : null;
		Championship championship = ageGroup != null ? ageGroup.getChampionship() : null;
		return championship != null && championship.isSnatchCJTotalMedals();
	}

	/**
	 * Snatch medallists according to the current standings. Total is unusable here: it stays 0 until a clean &amp; jerk is made.
	 */
	private List<Athlete> snatchMedalLeaders(List<Athlete> candidates) {
		return candidates.stream()
				.filter(a -> a.getSnatchTotal() > 0)
				.filter(a -> isRankInRange(a.getSnatchRank(), 3))
				.sorted((a, b) -> Integer.compare(rankOrMax(a.getSnatchRank()), rankOrMax(b.getSnatchRank())))
				.collect(Collectors.toList());
	}

	private List<Athlete> multiMedalLeaders(List<Athlete> candidates) {
		return candidates.stream()
				.filter(this::isMedalEligibleAnyLift)
				.sorted(Comparator.comparingInt(this::multiMedalLeaderGroup)
						.thenComparingInt(this::multiMedalLeaderRank))
				.collect(Collectors.toList());
	}

	private int multiMedalLeaderGroup(Athlete athlete) {
		if (hasNonZeroTotal(athlete)) {
			return 0;
		}
		return athlete.getSnatchTotal() == 0 ? 1 : 2;
	}

	private int multiMedalLeaderRank(Athlete athlete) {
		return switch (multiMedalLeaderGroup(athlete)) {
		case 0 -> rankOrMax(athlete.getTotalRank());
		case 1 -> rankOrMax(athlete.getCleanJerkRank());
		default -> rankOrMax(athlete.getSnatchRank());
		};
	}

	private boolean isMedalEligibleAnyLift(Athlete a) {
		return a.isMedalist();
	}

	private boolean isRankInRange(int rank, int maxRank) {
		return rank > 0 && rank <= maxRank;
	}

	private int rankOrMax(int rank) {
		return rank > 0 ? rank : Integer.MAX_VALUE;
	}

	private boolean hasStarted(Athlete a) {
		boolean weighedIn = a.getBodyWeight() != null && a.getBodyWeight() > 0.01;
		boolean started = a.getActualLiftOrNull(1) != null;
		return weighedIn && started;
	}

	public void medalistScoreLeaders(List<Athlete> medalists) {
		List<Athlete> scoreMedalists = medalists.stream().filter(a -> {
			int r = a.getCategoryScoreRank();
			return r <= 3 && r > 0;
		}).collect(Collectors.toList());
		setLeaders(scoreMedalists);
	}

	public void previousGroupScoreLeaders(List<Athlete> medalists) {
		if (medalists == null) {
			medalists = Collections.emptyList();
		}
		medalists = medalists.stream().filter(m -> {
			Group group2 = this.getGroup();
			Group group3 = m.getGroup();
			return group2 != null && group3 != null && !group3.equals(group2);
		}).toList();
		List<Athlete> scoreMedalists = medalists.stream()
				.filter(a -> hasStarted(a))
				.filter(a -> a.getCategoryScoreRank() > 0)
				.sorted((a, b) -> ObjectUtils.compare(a.getCategoryScoreRank(), b.getCategoryScoreRank()))
				.limit(3)
				.collect(Collectors.toList());
		setLeaders(scoreMedalists);
	}

	/**
	 * Projected rank for the current athlete if the next lift succeeds, compared against the current leaders list.
	 *
	 * <p><b>Display cases based on record contents:</b>
	 * <ul>
	 *   <li><b>(a) Snatch phase</b>: {@code totalRank = -1} — display "Athlete would be Nth in snatch"</li>
	 *   <li><b>(b) CJ phase, valid snatch</b>: {@code totalRank >= 1} — display "Athlete would be Nth in CJ, Xth in Total"</li>
	 *   <li><b>(c) CJ phase, bombed in snatch</b>: {@code totalRank = -1} — display "Athlete would be Nth in CJ" (no total possible)</li>
	 * </ul>
	 * Ranks {@code > 3} mean off the podium for that discipline.
	 * {@code computeProjectedRank()} returns {@code null} when there is no current athlete, no leaders list, or no next attempt.
	 * </p>
	 *
	 * @param liftRank   projected snatch rank (snatch phase) or CJ rank (CJ phase); 1 = best
	 * @param totalRank  projected total rank when a valid total is possible; -1 otherwise
	 */
	public record ProjectedRank(int liftRank, int totalRank) {}

	/**
	 * Computes the projected rank(s) for the current athlete assuming the next lift succeeds.
	 * <p>
	 * During snatch phase: returns the projected snatch rank vs. the leaders, totalRank = -1.
	 * During CJ phase: returns the projected CJ rank and either the projected total rank, or totalRank = -1 if the athlete bombed in snatch.
	 * </p>
	 * Uses the medals list for the current athlete's category directly so that the result is correct
	 * regardless of the MEDALISTS_AS_LEADERS setting and works in single-group competitions.
	 * Returns null if the computation is not meaningful (no current athlete, no medals, no next attempt, etc.).
	 */
	private ProjectedRank computeProjectedRank() {
		Athlete cur = getCurAthlete();
		if (cur == null) {
			return null;
		}
		// Use medals list directly instead of the display-filtered leaders list.
		// This works whether MEDALISTS_AS_LEADERS is on or off, and in single-group competitions.
		if (getMedals() == null) {
			return null;
		}
		Category medalCategory = findProjectedRankMedalCategory(cur);
		if (medalCategory == null) {
			return null;
		}
		List<Athlete> medalists = getMedals().get(medalCategory.getComputedCode());
		if (medalists == null || medalists.isEmpty()) {
			return null;
		}
		// Refresh current-group athletes with up-to-date lifting-order data (same as recomputeCurrentLeaders).
		List<Athlete> competitors = new ArrayList<>();
		for (Athlete a : medalists) {
			if (a.getId().equals(cur.getId())) {
				continue; // skip self
			}
			Athlete fresh = null;
			List<Athlete> lo = getLiftingOrder();
			if (lo != null) {
				for (Athlete g : lo) {
					if (g.getId().equals(a.getId())) {
						fresh = g;
						break;
					}
				}
			}
			competitors.add(fresh != null ? fresh : a);
		}
		if (competitors.isEmpty()) {
			return null;
		}
		Integer nextWeight = cur.getNextAttemptRequestedWeight();
		if (nextWeight == null || nextWeight <= 0) {
			return null;
		}

		if (!isCjStarted()) {
			// snatch phase: rank the projected snatch against leaders' best snatches
			int projectedSnatch = nextWeight;
			int snatchRank = 1;
			for (Athlete leader : competitors) {
				if (leader.getId().equals(cur.getId())) {
					continue;
				}
				int leaderSnatch = leader.getBestSnatch();
				boolean tiebreakerAhead = leaderSnatch == projectedSnatch && isTiebreakerAhead(leader, cur);
				boolean ahead = leaderSnatch > projectedSnatch || tiebreakerAhead;
				if (ahead) {
					snatchRank++;
				}
			}
			return new ProjectedRank(snatchRank, -1);
		} else {
			// CJ phase: always compute CJ rank; compute total rank only if snatch was not bombed
			int bestSnatch = cur.getBestSnatch();
			int projectedCJ = nextWeight;
			int projectedTotal = bestSnatch > 0 ? bestSnatch + nextWeight : -1;
			int cjRank = 1;
			int totalRank = bestSnatch > 0 ? 1 : -1; // -1 = case (c): bombed in snatch, no total possible
			for (Athlete leader : competitors) {
				if (leader.getId().equals(cur.getId())) {
					continue;
				}
				int leaderCJ = leader.getBestCleanJerk();
				boolean cjTiebreakerAhead = leaderCJ == projectedCJ && isTiebreakerAhead(leader, cur);
				boolean cjAhead = leaderCJ > projectedCJ || cjTiebreakerAhead;
				if (cjAhead) {
					cjRank++;
				}
				if (projectedTotal > 0) {
					int leaderTotal = leader.getTotal();
					boolean totalTiebreakerAhead = leaderTotal == projectedTotal && isTiebreakerAhead(leader, cur);
					boolean totalAhead = leaderTotal > projectedTotal || totalTiebreakerAhead;
					if (totalAhead) {
						totalRank++;
					}
				}
			}
			return new ProjectedRank(cjRank, totalRank);
		}
	}

	private Category findProjectedRankMedalCategory(Athlete cur) {
		Participation mainRankings = cur.getMainRankings();
		Category mainCategory = mainRankings != null ? mainRankings.getCategory() : null;
		if (mainCategory != null && getMedals().containsKey(mainCategory.getComputedCode())) {
			return mainCategory;
		}

		Category currentCategory = cur.getCategory();
		if (currentCategory != null && getMedals().containsKey(currentCategory.getComputedCode())) {
			return currentCategory;
		}
		return null;
	}

	/**
	 * Returns true if {@code leader} beats {@code cur} on tiebreakers (lighter bodyweight, then lower lot number).
	 */
	private boolean isTiebreakerAhead(Athlete leader, Athlete cur) {
		double leaderBW = leader.getBodyWeight() != null ? leader.getBodyWeight() : 0;
		double curBW = cur.getBodyWeight() != null ? cur.getBodyWeight() : 0;
		if (leaderBW < curBW) {
			return true;
		}
		if (leaderBW > curBW) {
			return false;
		}
		// same bodyweight: lower lot number lifts first and therefore wins the tiebreak
		int leaderLot = leader.getLotNumber() != null ? leader.getLotNumber() : Integer.MAX_VALUE;
		int curLot = cur.getLotNumber() != null ? cur.getLotNumber() : Integer.MAX_VALUE;
		return leaderLot < curLot;
	}

	private void recomputeLeadersAndRecords(List<Athlete> athletes) {
		recomputeCurrentLeaders(athletes);
		this.projectedRank = computeProjectedRank();
		recomputeRecords(getCurAthlete());
	}

	public ProjectedRank getProjectedRank() {
		return this.projectedRank;
	}

	/**
	 * Recompute lifting order, category ranks, and leaders for current category.
	 * Sets rankings including for previous lifters for all categories in the
	 * current
	 * group.
	 *
	 * @param recomputeCategoryRanks true if a result has changed and ranks need to
	 *                               be recomputed
	 */
	private void recomputeOrderAndRanks(boolean recomputeCategoryRanks) {
		Group g = getGroup();
		List<Athlete> athletes;

		long startAssignRanks = System.nanoTime();
		long endAssignRanks = 0;
		long endMedals = 0;
		long endDisplayOrder = 0;
		long endLeaders = 0;

		var initialList = getLiftingOrder();
		// this.logger.debug("{}recompute ranks recomputeCategoryRanks={} [{}]",
		// FieldOfPlay.getLoggingName(this),
		// recomputeCategoryRanks, LoggerUtils.whereFrom());
		if (recomputeCategoryRanks) {
			// we update the ranks all athletes in our category, as well as the current
			// scoring system
			athletes = JPAService.runInTransaction(em -> {
				return AthleteSorter.fetchForCategoryRanks(em, g);
			});
		} else {
			// only recompute the current scoring system
			athletes = JPAService.runInTransaction(em -> {
				return AthleteRepository.findAthletesForGlobalRanking(em, g);
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
				// logger.trace("athletes sent to computeMedals IDs: {}", athletes == null ?
				// null : athletes.stream().map(a ->
				// a.getAbbreviatedName()).collect(Collectors.toList()));
				setMedals(Competition.getCurrent().computeMedals(this, g, athletes));
			}
			endMedals = System.nanoTime();

			// athletes have been updated in database, refetch them. They are in 2nd-level
			// cache
			List<Athlete> currentGroupAthletes = JPAService.runInTransaction(em -> {
				ArrayList<Athlete> nAth = new ArrayList<>();
				for (Athlete a : initialList) {
					Athlete nA = em.find(Athlete.class, a.getId());
					if (nA != null) {
						nAth.add(nA);
					}
				}
				return nAth;
			});

			// set the FOP on refetched athletes so they can be used in background threads
			for (Athlete a : currentGroupAthletes) {
				a.setFop(this);
			}

			AthleteSorter.displayOrder(currentGroupAthletes);
			// Trace athlete IDs after fetching/refetching
			// logger.debug("currentGroupAthletes IDs: {}",
			// currentGroupAthletes.stream().map(a ->
			// a.getId()).collect(Collectors.toList()));
			// redundant filter appear to be an emergency kludge.
			currentGroupAthletes.stream()
					.filter(a -> a.getGroup() != null && a.getGroup().equals(g))
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
			Athlete nextCurrentAthlete = liftingOrder2 != null && liftingOrder2.size() > 0 ? liftingOrder2.get(0)
					: null;
			if (nextCurrentAthlete == null && CompetitionSimulator.isRunning()) {
				this.logger.warn("{}SIMULATION recompute produced no current athlete state={} group={} initialOrderSize={} refreshedGroupSize={} rankedAthleteSize={}\n{}",
				        FieldOfPlay.getLoggingName(this), getState(), g,
				        initialList != null ? initialList.size() : null, currentGroupAthletes.size(), athletes.size(),
				        LoggerUtils.stackTrace());
			}
			if (nextCurrentAthlete != null && nextCurrentAthlete.isForcedAsCurrent()) {
				nextCurrentAthlete.resetForcedAsCurrent();
				AthleteRepository.save(nextCurrentAthlete);
			}
			setCurAthlete(nextCurrentAthlete);
			setNextAthlete(liftingOrder2 != null && liftingOrder2.size() > 1 ? liftingOrder2.get(1) : null);
			// *** recomputeLeadersAndRecords(athletes);
			// for (Athlete a : liftingOrder2) {
			// logger.debug("sinclair {} {}",a.getShortName(), a.getSinclairRank());
			// }
			endLeaders = System.nanoTime();
		}

		if (this.timingLogger.isDebugEnabled()) {
			this.timingLogger.debug("{} {} total={}ms, fetch/assign={}ms medals={}ms liftingOrder={}ms leaders={}ms",
					FieldOfPlay.getLoggingName(this),
					recomputeCategoryRanks ? "recomputeOrderAndRanks" : "recompute order",
					(endLeaders - startAssignRanks) / 1000000.0,
					(endAssignRanks - startAssignRanks) / 1000000.0,
					(endMedals - endAssignRanks) / 1000000.0,
					(endDisplayOrder - endMedals) / 1000000.0,
					(endLeaders - endDisplayOrder) / 1000000.0);
		}

	}

	public boolean isFirstSnatch() {
		return getLiftingOrder().stream()
				.allMatch(a -> a.getAttemptsDone() == 0 || Integer.valueOf(0).equals(a.getActualLiftOrNull(1)));
	}

	public boolean isFirstCJ() {
		return getLiftingOrder().stream()
				.allMatch(a -> a.getAttemptsDone() == 3 || Integer.valueOf(0).equals(a.getActualLiftOrNull(4)));
	}

	public boolean isLastSnatch() {
		return getLiftingOrder().stream()
				.filter(a -> a.getAttemptsDone() == 2)
				.count() == 1;
	}

	public boolean isLastCJ() {
		return getLiftingOrder().stream()
				.filter(a -> a.getAttemptsDone() == 5)
				.count() == 1;
	}

	private void recomputeRecordsMap(List<Athlete> athletes) {
		// logger.debug("recompute record map");
		this.groupRecords.clear();
		for (Athlete a : athletes) {
			if (!hasCompleteRecordLookupProfile(a)) {
				this.displayableRecordsByAthlete.put(a, List.of());
				this.eligibleRecordsByAthlete.put(a, List.of());
				continue;
			}

			List<RecordEvent> displayableRecords = RecordFilter.computeDisplayableRecordsForAthlete(a);
			this.displayableRecordsByAthlete.put(a, displayableRecords);

			List<RecordEvent> eligibleRecords = RecordFilter.filterEligibleRecordsForAthlete(a, displayableRecords);
			// logger.debug("athlete {} {}",a, eligibleRecords);
			this.eligibleRecordsByAthlete.put(a, eligibleRecords);

			this.groupRecords.addAll(displayableRecords);
		}
	}

	private boolean hasCompleteRecordLookupProfile(Athlete athlete) {
		return athlete != null
				&& athlete.getFullBirthDate() != null
				&& athlete.getGender() != null
				&& athlete.getBodyWeight() != null;
	}

	/**
	 * Reset decisions. Invoked when a fresh clock is given.
	 */
	private void resetDecisions() {
		// this.logger.trace("{}resetting all decisions on new clock",
		// FieldOfPlay.getLoggingName(this));
		this.lastShownRefereeDecision = null;
		this.lastShownGoodLift = null;
		setRefereeDecision(new Boolean[3]);
		resetJuryDecisions();
		endPanelVotePending(this);
		setRefereeTime(new Long[3]);
		this.currentInputKind = null;
		pushOutUIEvent(new UIEvent.ResetOnNewClock(this.clockOwner, this, this));
	}

	private void resetEmittedFlags() {
		resetAthleteTimerWarningFlags();
		setDownEmitted(false);
		setDecisionDisplayScheduled(false);
		this.decisionCommitScheduled = false;
		setClockStoppedDecisionsAllowed(false);
		this.downSignalVisibleSince = 0;
		this.decisionReceivedWithoutClock = false;
	}

	void resetAthleteTimerWarningFlags() {
		setAthleteInitialWarningEmitted(false);
		setAthleteFinalWarningEmitted(false);
		setAthleteTimeoutEmitted(false);
	}

	void resetBreakTimerWarningFlags() {
		setBreakInitialWarningEmitted(false);
		setBreakFinalWarningEmitted(false);
		setBreakTimeoutEmitted(false);
	}

	/**
	 * Notify announcer/timekeeper if a decision was recorded without the clock
	 * being started.
	 * This is a safety warning - the lift result will still be processed, but
	 * officials should
	 * be alerted to the procedural irregularity.
	 * 
	 * @param origin the origin of the event for notification routing
	 */
	private void notifyDecisionWithoutClock(Object origin) {
		if (this.decisionReceivedWithoutClock) {
			this.logger./**/warn("{}Decision recorded but clock was never started for {}",
					FieldOfPlay.getLoggingName(this), getCurAthlete().getShortName());
			pushOutUIEvent(new UIEvent.Notification(
					getCurAthlete(), origin,
					UIEvent.Notification.Level.ERROR,
					"Decision.ClockNotStarted",
					5000, this));
			this.decisionReceivedWithoutClock = false;
		}
	}

	private void restartTimer(FOPEvent e, boolean useEvent) {
		// logger.trace("!!!!! restartTimer {} from:{}", this.getCurAthlete(),
		// LoggerUtils.whereFrom());
		cancelWakeUpRef();
		if (this.decisionDisplayTimer != null) {
			this.decisionDisplayTimer.cancel();
		}
		resetDecisions();
		if (getCurAthlete() == null && CompetitionSimulator.isRunning()) {
			this.logger.warn("{}SIMULATION restartTimer creating null-athlete DecisionReset state={} previousAthlete={} liftingOrderSize={} trigger={}\n{}",
			        FieldOfPlay.getLoggingName(this), getState(), getPreviousAthlete(),
			        this.liftingOrder != null ? this.liftingOrder.size() : null,
			        e != null ? e.getClass().getSimpleName() : null, LoggerUtils.stackTrace());
		}
		pushOutUIEvent(new UIEvent.DecisionReset(getCurAthlete(), this, this));
		transitionToLifting(e, this.group, true); // announcer decision is always immediate

		// if we are in TIME_STOPPED, this will create a loop as we will come back here.
		// in that case, useEvent should be false so we simply restart the timer.
		if (useEvent) {
			fopEventPost(new FOPEvent.TimeStarted(this));
		} else {
			setState(TIME_RUNNING);
			getAthleteTimer().start();
		}
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

	private void setBarWeight(int i) {
		this.barWeight = i;
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
		// logger.debug("setBreakParams {} {} isIndefinite={}", breakType2,
		// countdownType2,
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
		// this.logger.trace("{}setClockOwnerInitialTimeAllowed timeAllowed={} {}",
		// FieldOfPlay.getLoggingName(this),
		// timeAllowed, LoggerUtils.whereFrom());
		this.clockOwnerInitialTimeAllowed = timeAllowed;
	}

	/**
	 * Set up the FOP state to accept a referee decision even though the clock was
	 * never started.
	 * This ensures the decision can be processed as if the clock had been started.
	 * <p>
	 * This mirrors the essential setup from {@link #transitionToTimeRunning()} so
	 * that:
	 * <ul>
	 * <li>The down signal can be emitted when 2+ referees agree</li>
	 * <li>The lift outcome (good/bad) is properly registered</li>
	 * <li>The decision lights are shown after the appropriate delay</li>
	 * </ul>
	 *
	 * @param e the decision event
	 */
	private void setupForDecisionWithoutClock(FOPEvent e) {
		// Only do the full setup if we haven't already transitioned
		// (prevents re-setup and unwanted decision reset on 2nd and 3rd referee
		// decisions)
		if (getClockOwner() == null || !getClockOwner().equals(getCurAthlete())) {
			// Clear stale decisions from previous lift FIRST - before any other setup
			// This must happen only when starting a new decision phase, not on every
			// decision
			resetDecisions();

			// Set up clock ownership as if timekeeper had started it
			setClockOwner(getCurAthlete());

			// Reset emitted flags - crucial for down signal to work
			// This sets downEmitted=false so emitDown() will actually emit
			resetEmittedFlags();

			prepareDownSignal();
			setWeightAtLastStart();

			// Clear any previous lift result
			setGoodLift(null);

			// Transition to TIME_STOPPED (as if athlete already lifted bar)
			// Allow decisions to be processed in this state
			setState(TIME_STOPPED);
			setClockStoppedDecisionsAllowed(true);

			// Mark that decision came without clock - notification will be shown
			// only when all 3 referees have decided (in processRefereeDecisions)
			this.decisionReceivedWithoutClock = true;

			this.logger.debug("{}Decision received but clock was not started for {}", FieldOfPlay.getLoggingName(this),
					getCurAthlete().getShortName());
		}
	}

	private void setClockStoppedDecisionsAllowed(boolean b) {
		this.clockStoppedDecisionsAllowed = b;
	}

	private void setCurAthlete(Athlete athlete) {
		this.curAthlete = athlete;
		this.missingKgWarnedAthleteId = null;
	}

	private void setDecisionDisplayScheduled(boolean decisionDisplayScheduled) {
		this.decisionDisplayScheduled = decisionDisplayScheduled;
	}

	/**
	 * @param displayOrder the displayOrder to set
	 */
	private void setDisplayOrder(List<Athlete> displayOrder) {
		this.displayOrder = displayOrder;
		refreshScoreboardChampionships(displayOrder);
	}

	private synchronized void setDownEmitted(boolean downEmitted) {
		this.logger.trace("downEmitted {}", downEmitted);
		this.downEmitted = downEmitted;
	}

	private synchronized void setAthleteFinalWarningEmitted(boolean athleteFinalWarningEmitted) {
		this.logger.trace("athleteFinalWarningEmitted {}", athleteFinalWarningEmitted);
		this.athleteFinalWarningEmitted = athleteFinalWarningEmitted;
	}

	private synchronized void setBreakFinalWarningEmitted(boolean breakFinalWarningEmitted) {
		this.logger.trace("breakFinalWarningEmitted {}", breakFinalWarningEmitted);
		this.breakFinalWarningEmitted = breakFinalWarningEmitted;
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

	private synchronized void setAthleteInitialWarningEmitted(boolean athleteInitialWarningEmitted) {
		this.logger.trace("athleteInitialWarningEmitted {}", athleteInitialWarningEmitted);
		this.athleteInitialWarningEmitted = athleteInitialWarningEmitted;
	}

	private synchronized void setBreakInitialWarningEmitted(boolean breakInitialWarningEmitted) {
		this.logger.trace("breakInitialWarningEmitted {}", breakInitialWarningEmitted);
		this.breakInitialWarningEmitted = breakInitialWarningEmitted;
	}

	private void setLastChallengedRecords(List<RecordEvent> challengedRecords) {
		this.logger.info("{}lastChallengedRecords {}", FieldOfPlay.getLoggingName(this), challengedRecords);
		this.lastChallengedRecords = challengedRecords;
	}

	private void setLastNewRecords(List<RecordEvent> newRecords) {
		this.logger.info("{}lastNewRecords {}", FieldOfPlay.getLoggingName(this), newRecords);
		this.lastNewRecords = newRecords;
	}

	private void setLiftingOrder(List<Athlete> liftingOrder) {
		this.liftingOrder = liftingOrder;
		refreshScoreboardChampionships(liftingOrder);
	}

	private void setLightBarInUse(boolean b) {
		this.lightBarInUse = b;
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
	 * Don't interrupt break if official-induced break. Interrupt break if it is
	 * simply "group done".
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

	private synchronized void setAthleteTimeoutEmitted(boolean athleteTimeoutEmitted) {
		this.logger.trace("athleteTimeoutEmitted {}", athleteTimeoutEmitted);
		this.athleteTimeoutEmitted = athleteTimeoutEmitted;
	}

	private synchronized void setBreakTimeoutEmitted(boolean breakTimeoutEmitted) {
		this.logger.trace("breakTimeoutEmitted {}", breakTimeoutEmitted);
		this.breakTimeoutEmitted = breakTimeoutEmitted;
	}

	private void setUseCollarsIfAvailable(boolean b) {
		this.useCollarsIfAvailable = b;
	}

	private void setWeightAtLastStart() {
		setWeightAtLastStart(getCurAthlete().getNextAttemptRequestedWeight());
	}

	private synchronized void showDecisionAfterDelay(Object origin2, int reversalDelay) {
		// logger.debug("{}scheduling decision display in {}ms", getLoggingName(),
		// reversalDelay);
		assert !isDecisionDisplayScheduled(); // caller checks.
		setDecisionDisplayScheduled(true); // so there are never two scheduled...
		AuditActor actor = AuditContext.actor();
		String cause = AuditContext.cause();
		long generation = this.decisionTaskGeneration;
		this.decisionDisplayTimer = new DelayTimer(isTestingMode()).schedule(
				() -> AuditContext.run(actor, cause, () -> {
					synchronized (FieldOfPlay.this) {
						if (generation == this.decisionTaskGeneration) {
							showDecisionNow(origin2);
						}
					}
				}), reversalDelay);
	}

	private synchronized void showDecisionVisibleAfterDelay(Object origin2, int displayDelay) {
		assert !isDecisionDisplayScheduled();
		setDecisionDisplayScheduled(true);
		long generation = this.decisionTaskGeneration;
		this.decisionDisplayTimer = new DelayTimer(isTestingMode()).schedule(() -> {
			synchronized (FieldOfPlay.this) {
				if (generation == this.decisionTaskGeneration) {
					showDecisionVisibleNow(origin2);
				}
			}
		}, displayDelay);
	}

	private synchronized void commitDecisionAfterDelay(Object origin2, int reversalDelay) {
		this.decisionCommitScheduled = true;
		AuditActor actor = AuditContext.actor();
		String cause = AuditContext.cause();
		long generation = this.decisionTaskGeneration;
		new DelayTimer(isTestingMode()).schedule(
				() -> AuditContext.run(actor, cause, () -> {
					synchronized (FieldOfPlay.this) {
						if (generation == this.decisionTaskGeneration) {
							commitDecisionNow(origin2);
						}
					}
				}), reversalDelay);
	}

	/**
	 * Neutralize every scheduled decision-related task so that nothing scheduled before a forced reload can
	 * show, commit or reset a decision on the reloaded session. Results already saved are kept.
	 */
	private synchronized void invalidatePendingDecisionTasks() {
		this.decisionTaskGeneration++;
		cancelDecisionReset();
		if (this.decisionDisplayTimer != null) {
			this.decisionDisplayTimer.cancel();
			this.decisionDisplayTimer = null;
		}
		cancelWakeUpRef();
		resetEmittedFlags();
		this.ignoreDecisionInputsUntil = 0;
		this.deferredWeightChanges.clear();
		this.deferredBreak = null;
	}

	private synchronized void showDecisionVisibleNow(Object origin) {
		setGoodLift(computeCurrentGoodLift());
		setState(DECISION_VISIBLE);
		this.ignoreDecisionInputsUntil = System.currentTimeMillis() + this.decisionInputIgnoreWindowMs;
		uiShowCurrentRefereeDecision(origin);
		scheduleDecisionReset();
	}

	private synchronized void commitDecisionNow(Object origin) {
		this.decisionCommitScheduled = false;
		commitCurrentDecision();
		// The decision is already visible (immediate mode). Only re-broadcast if the
		// referee decisions changed since they were last shown (e.g. a reversal that
		// arrived after the decision became visible). Otherwise this would be a
		// redundant re-render of an identical decision.
		if (!refereeDecisionUnchangedSinceLastShown()) {
			uiShowCurrentRefereeDecision(origin);
		}
		recomputeLiftingOrder(true, true);
		if (this.state == DECISION_VISIBLE && this.awaitingJuryCompletion && isJuryVotesAwaited()) {
			// immediate mode: the display was scheduled before the commit made the panel vote pending
			holdDecisionForJury();
		}
		// the jury may have voted before the decision was committed
		evaluatePanel();
	}

	/**
	 * The decision is confirmed as official after the 3 second delay following
	 * majority. After this delay, manual announcer intervention is required to
	 * change
	 * and announce.
	 */
	private synchronized void showDecisionNow(Object origin) {
		commitCurrentDecision();
		setState(DECISION_VISIBLE);
		this.ignoreDecisionInputsUntil = System.currentTimeMillis() + this.decisionInputIgnoreWindowMs;
		uiShowCurrentRefereeDecision(this);
		recomputeLiftingOrder(true, true);
		scheduleDecisionReset();
		// the jury may have voted before the decision was committed
		evaluatePanel();
	}

	private void scheduleDecisionReset() {
		cancelDecisionReset();
		this.decisionVisibleSince = System.nanoTime() / 1_000_000;
		this.decisionJurySize = getJurySize();
		this.awaitingJuryCompletion = DecisionDisplayRules.showJuryDecisionsOnAttemptBoard(this.decisionJurySize,
		        Config.getCurrent().featureSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD))
		        && !allDecisionJurorsVoted();
		if (this.awaitingJuryCompletion && isJuryVotesAwaited()) {
			holdDecisionForJury();
			return;
		}
		long duration = this.awaitingJuryCompletion
				? Math.max(this.decisionVisibleDuration, this.juryDecisionVisibleMaxMs)
				: this.decisionVisibleDuration;
		this.decisionResetDeadline = this.decisionVisibleSince + duration;
		armDecisionReset();
	}

	/**
	 * Keep the decision visible with no time limit while the configured jury votes. The reset is armed by
	 * rescheduleDecisionResetForJury when the last jury vote arrives; the clock-start override also clears the display.
	 */
	private void holdDecisionForJury() {
		this.decisionResetGeneration++;
		if (this.decisionResetTimer != null) {
			this.decisionResetTimer.cancel();
			this.decisionResetTimer = null;
		}
		this.decisionResetDeadline = Long.MAX_VALUE;
	}

	private boolean allDecisionJurorsVoted() {
		return allJurorsVoted(this.decisionJurySize);
	}

	private boolean allJurorsVoted(int jurySize) {
		Boolean[] votes = getJuryMemberDecision();
		if (jurySize <= 0 || votes == null || votes.length < jurySize) {
			return false;
		}
		for (int index = 0; index < jurySize; index++) {
			if (votes[index] == null) {
				return false;
			}
		}
		return true;
	}

	private void rescheduleDecisionResetForJury() {
		if (this.state != DECISION_VISIBLE || !this.awaitingJuryCompletion || !allDecisionJurorsVoted()) {
			return;
		}
		long now = System.nanoTime() / 1_000_000;
		if (now >= this.decisionResetDeadline) {
			return;
		}
		this.awaitingJuryCompletion = false;
		this.decisionResetDeadline = Math.max(this.decisionVisibleSince + this.decisionVisibleDuration,
				now + this.juryDecisionHoldMs);
		armDecisionReset();
	}

	private void armDecisionReset() {
		if (this.decisionResetTimer != null) {
			this.decisionResetTimer.cancel();
		}
		long generation = ++this.decisionResetGeneration;
		long delay = Math.max(0, this.decisionResetDeadline - System.nanoTime() / 1_000_000);
		this.decisionResetTimer = new DelayTimer(isTestingMode()).schedule(() -> {
			synchronized (FieldOfPlay.this) {
				if (generation == this.decisionResetGeneration && this.state == DECISION_VISIBLE) {
					fopEventPost(new DecisionReset(this));
				}
			}
		}, delay);
	}

	private synchronized void cancelDecisionReset() {
		this.decisionResetGeneration++;
		this.awaitingJuryCompletion = false;
		if (this.decisionResetTimer != null) {
			this.decisionResetTimer.cancel();
			this.decisionResetTimer = null;
		}
	}

	private void commitCurrentDecision() {
		var attempted = getCurAthlete().getNextAttemptRequestedWeight();
		setAthleteUnderReview(getCurAthlete());
		setPreviousAthlete(this.athleteUnderReview);
		setLastChallengedRecords(this.challengedRecords);
		setGoodLift(computeCurrentGoodLift());
		if (getCurAthlete() != null) {
			this.setCjStarted((getCurAthlete().getAttemptsDone() > 3));
			int attempt = getCurAthlete().getAttemptsDone() + 1;
			boolean lastSnatch = attempt == 3 && isLastSnatch();
			boolean lastCleanJerk = attempt == 6 && isLastCJ();
			LocalDateTime decisionTime = LocalDateTime.now();
			String result = Integer.toString(Boolean.TRUE.equals(getGoodLift()) ? attempted : -attempted);
			getCurAthlete().recordLift(attempt, result, decisionTime);
			if (this.currentInputKind != InputKind.ANNOUNCER_ENTRY
					&& this.currentInputKind != InputKind.SOLO_INPUT) {
				getCurAthlete().setDecisions(attempt, Arrays.asList(getRefereeDecision().clone()));
			}
			// the configured jury votes on this lift; the next clock waits for all jurors, a jury decision, or an
			// explicit clock-start override
			this.panelPendingRefereeVotes = hasJuryReviewVote()
			        && AdjudicationPanel.refereeMajority(getRefereeDecision()) != null
			        && this.currentInputKind != InputKind.ANNOUNCER_ENTRY
			        && this.currentInputKind != InputKind.SOLO_INPUT
			                ? getRefereeDecision().clone()
			                : null;
			if (lastSnatch) {
				getGroup().setLastSnatchDecisionTime(decisionTime, getGroup(), this);
			} else if (lastCleanJerk) {
				getGroup().setLastCJDecisionTime(decisionTime, getGroup(), this);
			}
			getCurAthlete().resetForcedAsCurrent();
		}
		setForcedTime(false);
		String decisionCause = "decision " + Arrays.toString(getRefereeDecision());
		String decisionStation = this.currentInputKind == InputKind.ANNOUNCER_ENTRY ? "ANNOUNCER" : "REFEREES";
		AuditActor decisionActor = AuditContext.actor() != null ? AuditContext.actor() : AuditActor.system();
		List<RecordEvent> newRecords = AuditContext.call(
				decisionActor.atStation(decisionStation, null, false), decisionCause, () -> {
					AthleteRepository.save(getCurAthlete());
					return updateRecords(getCurAthlete(), getGoodLift(), getChallengedRecords(), List.of());
				});
		setNewRecords(newRecords);
		setLastNewRecords(newRecords);
		new DelayTimer(isTestingMode()).schedule(
				() -> {
					boolean recordsBroken = !newRecords.isEmpty();
					notifyRecords(recordsBroken ? getChallengedRecords() : List.of(),
							recordsBroken, attempted);
				}, this.recordNotificationDelayMs);
		this.auditLiftBoundary = true;
		if (this.auditEventDepth == 0) {
			this.auditLiftBoundary = false;
			AuditLog.closeBlock(getName());
		}
	}

	private Boolean computeCurrentGoodLift() {
		int nbWhite = 0;
		for (int i = 0; i < 3; i++) {
			nbWhite = nbWhite + (Boolean.TRUE.equals(getRefereeDecision()[i]) ? 1 : 0);
		}
		return nbWhite >= 2;
	}

	private void uiShowCurrentRefereeDecision(Object origin) {
		uiShowRefereeDecisionOnSlaveDisplays(getCurAthlete(), getGoodLift(), getRefereeDecision(), getRefereeTime(),
				origin);
		this.lastShownRefereeDecision = getRefereeDecision() == null ? null : getRefereeDecision().clone();
		this.lastShownGoodLift = getGoodLift();
	}

	private boolean refereeDecisionUnchangedSinceLastShown() {
		return this.lastShownRefereeDecision != null
				&& Arrays.equals(this.lastShownRefereeDecision, getRefereeDecision())
				&& Objects.equals(this.lastShownGoodLift, getGoodLift());
	}

	private void showJuryMemberDecisionReceived(Object origin, int i, Boolean[] juryMemberDecision2, int jurySize) {
		// show that one jury decision has been received (green LED)
		// logger.debug("{}updating jury member {}", getLoggingName(), i);
		UIEvent.JuryUpdate update = new UIEvent.JuryUpdate(origin, i, juryMemberDecision2, jurySize, this);
		update.setReviewVote(this.panelVoteOpen);
		pushOutUIEvent(update);
	}

	private void showJuryMemberDecisionsNow(Object origin, boolean unanimous, int jurySize,
			Boolean[] juryMemberDecision2) {
		// logger.debug("{}reveal jury member decisions {}", getLoggingName(),
		// juryMemberDecision2);
		UIEvent.JuryUpdate update = new UIEvent.JuryUpdate(origin, unanimous, juryMemberDecision2, jurySize, this);
		update.setReviewVote(this.panelVoteOpen);
		pushOutUIEvent(update);
	}

	/**
	 * Create a fake unanimous decision when overridden.
	 *
	 * @param e
	 */
	private void simulateDecision(ExplicitDecision ed) {
		this.currentInputKind = InputKind.ANNOUNCER_ENTRY;
		long now = System.currentTimeMillis();
		if (getAthleteTimer().isRunning()) {
			getAthleteTimer().stop();
		}

		// Check if this decision came without clock being started (e.g., MQTT solo
		// referee)
		notifyDecisionWithoutClock(ed.getOrigin());

		this.setClockOwner(null);
		DecisionFullUpdate ne = new DecisionFullUpdate(ed.getOrigin(), ed.getAthlete(), ed.ref1, ed.ref2, ed.ref3, now,
				now, now, true); // announcer decisions are always immediate
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
		if (this.activeCeremony == null || !this.activeCeremony.isMedals()) {
			setCeremonyType(null);
		}

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
				// logger.debug("{}switching break type while in break : current {} new {}
				// remaining {}",
				// getLoggingName(), getBreakType(), newBreak, breakTimer.liveTimeRemaining());
				if (newBreak == BreakType.FIRST_SNATCH) {
					BreakType oldBreakType = getBreakType();
					setBreakType(newBreak);
					// logger.debug("switching oldbreaktype = {} indefinite = {}", oldBreakType,
					// indefinite);
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
					// logger.debug("{}break switch: from {} to {} {}", getLoggingName(),
					// getBreakType(),
					// newBreak, newCountdownType);
					breakTimer.stop();
					setBreakParams(e, breakTimer, newBreak, newCountdownType);
					breakTimer.setTimeRemaining(breakTimer.liveTimeRemaining(), newBreak.isInterruption());
					breakTimer.start(); // so we restart in the new type
					return;
				}
			} else {
				// we are in a break, resume if needed
				// logger.debug("{}resuming break : current {} new {}", getLoggingName(),
				// getBreakType(),
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
			// logger.debug("stopping bt={} ct={} indefinite={}", newBreak,
			// newCountdownType, newBreak.isInterruption()
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
		this.logger.info("{}transition to lifting from curState = {}", FieldOfPlay.getLoggingName(this), getState());
		if (getCurAthlete() != null && getCurAthlete().equals(clockOwner)
				&& (getState() == FOPState.BREAK || getState() == FOPState.INACTIVE)) {
			setClockStoppedDecisionsAllowed(true);
			setState(CURRENT_ATHLETE_DISPLAYED);
		} else {
			if (getCurAthlete() != null) {
				// group already in progress, do not force loading from database
				// logger.debug("---------- already in progress");
				loadGroup(group2, e.getOrigin(), false);
			} else {
				// logger.debug("---------- NOT in progress");
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
		endMedalCeremony(this);

		if (!getCurAthlete().equals(getClockOwner())) {
			setClockOwner(getCurAthlete());
		}
		resetEmittedFlags();
		prepareDownSignal();
		setWeightAtLastStart();
		setLastChallengedRecords(List.of());

		// make sure decisions have been reset before setting state to time running
		int time = getAthleteTimer().getTimeRemaining();
		if (isForcedTime() || this.clockOwner != this.previousAthlete
				|| (time == Competition.athleteTimerOneMinute || time == Competition.athleteTimerTwoMinutes)) {
			resetDecisions();
			if (time == Competition.athleteTimerOneMinute || time == Competition.athleteTimerTwoMinutes) {
				// new clock, use this as reference for late declarations.
				setClockOwnerInitialTimeAllowed(time);
			}
		}

		// enable master to listening for decision
		setState(TIME_RUNNING);
		setGoodLift(null);
		// Clock start ends the post-decision cooldown so referees can enter a new decision.
		this.ignoreDecisionInputsUntil = 0;

		if (getCurAthlete().getAttemptsDone() >= 3) {
			setCjStarted(true);
		}
		checkFirstClockForLift();
		// Use start(time) to set and start atomically - avoids separate SetTime event
		// which can cause visual stutter on displays when StartTime follows immediately
		getAthleteTimer().start(time);
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

		if (curAthlete2 != null && curAthlete2.getSnatchAttemptsDone() == 3) {
			// athlete has until before first CJ tSo comply with starting weights rule
			// if the snatch was lowered.
			warnMissingKg();
		}
		recomputeLeadersAndRecords(this.liftingOrder);

		changePlatformEquipment(curAthlete2, this.curWeight);

		// logger.trace("uiDisplayCurrentAthleteAndTime {} {} {} previous {} current {}
		// change {} from[{}]", curAthlete2, nextAthlete, newWeight,
		// getPrevWeight(), curWeight, newWeight, LoggerUtils.whereFrom());
		UIEvent.LiftingOrderUpdated lou = new UIEvent.LiftingOrderUpdated(curAthlete2, nextAthlete,
				getPreviousAthlete(),
				changingAthlete,
				getLiftingOrder(), getDisplayOrder(), clock, currentDisplayAffected, displayToggle, e.getOrigin(),
				inBreak, newWeight, this);
		// carry the authoritative athlete-timer truth so clients can re-assert it if a
		// StartTime/StopTime
		// was reordered, dropped, or never emitted. Not meaningful during a break.
		boolean timerRunning = getAthleteTimer().isRunning();
		lou.setTimerShouldRun(timerRunning);
		lou.setTimerMillisRemaining(timerRunning ? getAthleteTimer().liveTimeRemaining()
				: getAthleteTimer().getTimeRemaining());
		lou.setTimerStateValid(!inBreak);
		pushOutUIEvent(lou);
		setPrevWeight(this.curWeight);

		// cur athlete can be null during some tests.
		int attempts = curAthlete2 == null ? 0 : curAthlete2.getAttemptsDone();

		String shortName = curAthlete2 == null ? "" : curAthlete2.getShortName();
		this.logger.info("{}current athlete = {} attempt = {}, requested = {}, clock={} initialTime={}",
				FieldOfPlay.getLoggingName(this), shortName, attempts + 1, this.curWeight,
				clock,
				getClockOwnerInitialTimeAllowed());

		notifyRecords(getChallengedRecords(), false, this.curWeight);

		if (attempts >= 6) {
			pushOutDone();
		}

		if (!this.isCjBreakDisplayed() && allFirstCJ()) {
			this.logger.trace("{}push out snatch done", FieldOfPlay.getLoggingName(this));
			pushOutSnatchDone();
			this.setCjBreakDisplayed(true);
		}
	}

	private synchronized void uiShowDownSignalOnSlaveDisplays(Object origin2) {
		boolean fromAnnouncer = origin2 instanceof AnnouncerContent;
		boolean announcerImmediate = fromAnnouncer; // announcer decisions are always immediate
		boolean emitSoundsOnServer2 = isEmitSoundsOnServer();
		boolean downEmitted2 = isDownEmitted();
		// this.logger.ddebug("showDownSignalOnSlaveDisplays fromAnnouncer {}
		// announcerImmediate {} emitted={}", fromAnnouncer,
		// announcerImmediate, downEmitted2);
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
		// logger.debug("pushing down");
		pushOutUIEvent(new UIEvent.DownSignal(origin2, this));
	}

	private void uiShowPlates(BarbellOrPlatesChanged e) {
		if (e.getOrigin() != this) {
			changePlatformEquipment(this.curAthlete, this.curWeight);
		}
		pushOutUIEvent(new UIEvent.BarbellOrPlatesChanged(e.getOrigin(), this));
	}

	private void uiShowRefereeDecisionOnSlaveDisplays(Athlete athlete2, Boolean goodLift2, Boolean[] refereeDecision2,
			Long[] longs, Object origin2) {
		InputKind inputKind = this.currentInputKind;
		boolean singleRef = inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT;
		Boolean ref1 = null;
		Boolean ref2 = null;
		Boolean ref3 = null;
		if (singleRef) {
			ref1 = null;
			ref2 = refereeDecision2[1];
			ref3 = null;
		} else {
			ref1 = refereeDecision2[0];
			ref2 = refereeDecision2[1];
			ref3 = refereeDecision2[2];
		}
		TimingPolicy timingPolicy = inputKind == InputKind.ANNOUNCER_ENTRY
				? TimingPolicy.IMMEDIATE
				: (isShowDecisionsImmediately() ? TimingPolicy.IMMEDIATE : TimingPolicy.DELAYED);
		pushOutUIEvent(new UIEvent.Decision(athlete2, goodLift2, ref1, ref2, ref3, origin2, this, singleRef,
				timingPolicy, inputKind));
	}

	private void uiShowUpdatedRankings() {
		pushOutUIEvent(new UIEvent.GlobalRankingUpdated(this, this));
	}

	private void uiShowUpdateOnJuryScreen(FOPEvent e) {
		InputKind inputKind = this.currentInputKind;
		boolean singleRef = inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT;
		// logger.debug("### uiShowUpdateOnJuryScreen inputKind={}", inputKind);
		pushOutUIEvent(new UIEvent.RefereeUpdate(getCurAthlete(),
				singleRef ? null : getRefereeDecision()[0],
				getRefereeDecision()[1],
				singleRef ? null : getRefereeDecision()[2],
				getRefereeTime()[0],
				getRefereeTime()[1],
				getRefereeTime()[2],
				e.getOrigin(),
				singleRef,
				this));
	}

	private void unexpectedEventInState(FOPEvent e, FOPState state) {
		this.auditEventRefused = true;
		this.auditRefusedState = state;

		// events not worth signaling
		if (e instanceof DecisionReset || e instanceof DecisionFullUpdate) {
			// ignore
			return;
		}

		// Log additional context for JuryDecision events
		if (e instanceof JuryDecision) {
			JuryDecision jd = (JuryDecision) e;
			this.logger./**/warn(
					"{}JuryDecision event refused in state {}: isJuryButton={}, athlete={}",
					FieldOfPlay.getLoggingName(this),
					state,
					jd.isJuryButton(),
					jd.getAthlete() != null ? jd.getAthlete().getShortName() : "null");
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
		this.logger.info("{}decision {} good lift={} {}", FieldOfPlay.getLoggingName(this), a.getShortName(), success,
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
			for (RecordEvent newRecord : newRecords) {
				challengedRecords.stream().filter(newRecord::sameAs).findFirst()
						.ifPresent(previous -> RecordAudit.improved(getName(), a, a.getAttemptsDone(), previous, newRecord));
			}
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
				voidableRecords.forEach(record -> RecordAudit.cancelled(getName(), a, a.getAttemptsDone(), record));
				recomputeRecordsMap(this.displayOrder);
			}
			return new ArrayList<>();
		}

	}

	private void updateRefereeDecisions(FOPEvent.DecisionFullUpdate e) {
		// logger.debug("referee decisions {} {} {}\n{}", e.ref1, e.ref2, e.ref3,
		// LoggerUtils.stackTrace());

		Boolean ref1 = e.ref1;
		Boolean ref2 = e.ref2;
		Boolean ref3 = e.ref3;
		Long ref1Time = e.ref1Time;
		Long ref2Time = e.ref2Time;
		Long ref3Time = e.ref3Time;

		if (isSingleReferee()) {
			Boolean firstDecision = ref2 != null ? ref2 : (ref1 != null ? ref1 : ref3);
			if (firstDecision != null) {
				Long firstDecisionTime = ref2 != null ? ref2Time : (ref1 != null ? ref1Time : ref3Time);
				if (Config.getCurrent().featureSwitch(FeatureSwitch.PLAYWRIGHT)) {
					this.logger./*playwright*/warn(
							"{}normalizing DecisionFullUpdate to solo input ref1={} ref2={} ref3={} state={} {}",
							FieldOfPlay.getLoggingName(this), e.ref1, e.ref2, e.ref3, this.state,
							LoggerUtils.whereFrom());
				}
				ref1 = firstDecision;
				ref2 = firstDecision;
				ref3 = firstDecision;
				Long normalizedTime = firstDecisionTime != null ? firstDecisionTime : System.currentTimeMillis();
				ref1Time = normalizedTime;
				ref2Time = normalizedTime;
				ref3Time = normalizedTime;
			}
		}

		// it is not possible to go from a non-null decision that was given back to null
		// this would indicate an event out of order.
		boolean outOfOrder = isOutOfOrder(ref1, ref2, ref3);
		if (!outOfOrder) {
			if (isSingleReferee() && ref1 != null) {
				normalizeSoloDecision(ref1, ref1Time);
			} else {
				setRefereeDecisions(ref1, ref2, ref3, ref1Time, ref2Time, ref3Time);
			}
			processRefereeDecisions(e);
		} else {
			// this should never happen, but the order of JavaScript callbacks
			// from the browser to the server is not guaranteed.
			this.logger.error("ignoring out of order DecisionFullUpdate event {}", e);
		}
	}

	private void updateRefereeDecisions(FOPEvent.DecisionUpdate e) {
		getRefereeDecision()[e.getRefIndex()] = e.isDecision();
		getRefereeTime()[e.getRefIndex()] = System.currentTimeMillis();
		processRefereeDecisions(e);
	}

	private boolean isOutOfOrder(Boolean ref1, Boolean ref2, Boolean ref3) {
		return (ref1 == null && getRefereeDecision()[0] != null)
				|| (ref2 == null && getRefereeDecision()[1] != null)
				|| (ref3 == null && getRefereeDecision()[2] != null);
	}

	private void normalizeSoloDecision(Boolean decision, Long decisionTime) {
		Long normalizedTime = decisionTime != null ? decisionTime : System.currentTimeMillis();
		this.currentInputKind = InputKind.SOLO_INPUT;
		setRefereeDecisions(decision, decision, decision, normalizedTime, normalizedTime, normalizedTime);
	}

	private void setRefereeDecisions(Boolean ref1, Boolean ref2, Boolean ref3, Long ref1Time, Long ref2Time,
			Long ref3Time) {
		getRefereeDecision()[0] = ref1;
		getRefereeTime()[0] = ref1Time;
		getRefereeDecision()[1] = ref2;
		getRefereeTime()[1] = ref2Time;
		getRefereeDecision()[2] = ref3;
		getRefereeTime()[2] = ref3Time;
	}


	private void warnMissingKg() {
		Athlete cur = this.getCurAthlete();
		int missingKg = cur.startingTotalDelta();
		if (missingKg > 0) {
			Long curId = cur.getId();
			if (curId != null && curId.equals(this.missingKgWarnedAthleteId)) {
				return; // already warned for this athlete
			}
			this.missingKgWarnedAthleteId = curId;
			pushOutUIEvent(
					new UIEvent.Notification(
							cur,
							this,
							UIEvent.Notification.Level.ERROR,
							"RuleViolation.StartingWeightCurrent",
							0,
							this,
							Integer.toString(missingKg)));
		} else {
			this.missingKgWarnedAthleteId = null;
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

	private boolean isCjBreakDisplayed() {
		return cjBreakDisplayed;
	}

	private void setCjBreakDisplayed(boolean cjBreakDisplayed) {
		this.cjBreakDisplayed = cjBreakDisplayed;
	}
}
