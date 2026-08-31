/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.components.elements.PlatesElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.team.Team;
import app.owlcms.displays.video.StylesDirSelection;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.i18n.Translator;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.HasBoardMode;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Attempt board.
 */

@SuppressWarnings({ "serial", "deprecation" })

public abstract class AbstractAttemptBoard extends LitTemplate implements
        DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle,
        RequireDisplayLogin,
        StylesDirSelection, HasBoardMode {

	protected final static Logger logger = (Logger) LoggerFactory.getLogger(AbstractAttemptBoard.class);
	protected final static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	private static final int JURY_NOTIFICATION_DIALOG_DURATION_MS = 5000;
	private static final DateTimeFormatter CLIENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
			.withZone(ZoneId.systemDefault());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	/*
	 * The following 3 items need to be injected in the LitTemplate. Vaadin will create the slots and perform the injection based on the @Id annotation.
	 */
	@Id("athleteTimer")
	protected AthleteTimerElement athleteTimer; // created by Flow during template instantiation
	@Id("breakTimer")
	protected BreakTimerElement breakTimer; // created by Flow during template instantiation
	@Id("decisions")
	protected DecisionElement decisions; // created by Flow during template instantiation
	private boolean athletePictures;
	protected String routeParameter;
	protected boolean teamFlags;
	protected EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private boolean silenced;
	private boolean downSilenced;
	private boolean groupDone;
	private PlatesElement plates;
	private boolean publicFacing;
	private boolean showBarbell;
	private boolean video;
	private boolean publicDisplay;
	private FieldOfPlay fop;
	private Group group;
	private boolean abbreviatedName;
	private UI ui;
	private Dialog juryNotificationDialog;
	private Timer juryNotificationTimer;
	private long boardStateSequence;
	private boolean decisionLightsVisible;
	private AttemptBoardState lastBoardState;

	/**
	 * Instantiates a new attempt board.
	 */
	public AbstractAttemptBoard() {
		OwlcmsFactory.waitDBInitialized();
		// logger.debug("AttemptBoard new {}", LoggerUtils.whereFrom());
		// athleteTimer.setOrigin(this);
		this.getElement().setProperty("kgSymbol", Translator.translate("KgSymbol"));
		this.getElement().setProperty("STOP", Translator.translate("STOP"));
		// breakTimer.setParent("attemptBoard");
		checkImages();
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());

		overrideColors(this.getElement());
	}

	@Override
	public void doBreak(UIEvent e) {
		FieldOfPlay fop = getFop();
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			try {
				publish(buildBreakState(fop));
				updatePlates(fop.getCurAthlete() != null && fop.getBreakType() != BreakType.GROUP_DONE);
				uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
			} catch (Throwable e1) {
				LoggerUtils.logError(logger, e1);
			}
		});
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		FieldOfPlay fop = getFop();
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			publish(buildBreakState(fop));
		});
	}

	public DecisionElement getDecisions() {
		return this.decisions;
	}

	@Override
	final public FieldOfPlay getFop() {
		return this.fop;
	}

	@Override
	final public Group getGroup() {
		return this.group;
	}

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("Attempt") + suffix;
	}

	@Override
	final public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public final boolean isAbbreviatedName() {
		return this.abbreviatedName;
	}

	@Override
	public boolean isDarkMode() {
		return true;
	}

	@Override
	public boolean isDownSilenced() {
		return this.downSilenced;
	}

	/**
	 * @return the publicFacing
	 */
	public boolean isPublicFacing() {
		return this.publicFacing;
	}

	/**
	 * @return the showBarbell
	 */
	public boolean isShowBarbell() {
		return this.showBarbell;
	}

	@Override
	public boolean isSilenced() {
		return this.silenced;
	}

	@Override
	public boolean isVideo() {
		return this.video;
	}

	@Override
	final public void setAbbreviatedName(boolean b) {
		this.abbreviatedName = b;
	}

	@Override
	final public void setDarkMode(boolean dark) {
		// always dark, see #isDarkMode
	}

	@Override
	public void setDownSilenced(boolean downSilenced) {
		this.decisions.setSilenced(downSilenced);
		this.downSilenced = downSilenced;
	}

	@Override
	public void setEmFontSize(Double emFontSize) {
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
		// Propagate FOP to timer elements so they can register on the correct event bus
		propagateFopToTimerElements(fop);
	}

	/**
	 * Propagate FOP to timer elements (BreakTimerElement, AthleteTimerElement, DecisionElement).
	 * Timer elements must have their FOP set before they attach to register on the correct event bus.
	 * @param fop the field of play to propagate
	 */
	protected void propagateFopToTimerElements(FieldOfPlay fop) {
		if (this.breakTimer != null) {
			this.breakTimer.setFop(fop);
		}
		if (this.athleteTimer != null) {
			this.athleteTimer.setOrigin(this);
			this.athleteTimer.setFop(fop);
		}
		if (this.decisions != null) {
			this.decisions.setFop(fop);
			// Keep the down signal visible for 1.5 s before showing an IMMEDIATE decision
			// so athletes and spectators can see the down signal.
			this.decisions.setDownSignalHoldMs(DecisionElement.MINIMUM_DOWN_SIGNAL_VISIBLE_MS);
		}
	}

	@Override
	public void setGroup(Group group) {
		this.group = group;

	}

	@Override
	public void setLeadersDisplay(boolean showLeaders) {
	}

	@Override
	public void setPublicDisplay(boolean publicDisplay) {
		this.publicDisplay = publicDisplay;
	}

	/**
	 * @param publicFacing the publicFacing to set
	 */
	public void setPublicFacing(boolean publicFacing) {
		this.getElement().setProperty("publicFacing", true);
		this.decisions.setPublicFacing(publicFacing);
		this.publicFacing = publicFacing;
	}

	@Override
	public void setRecordsDisplay(boolean showRecords) {
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
		if (routeParameter != null && routeParameter.contentEquals("video")) {
			setVideo(true);
		}
	}

	/**
	 * @param showBarbell the showBarbell to set
	 */
	public void setShowBarbell(boolean showBarbell) {
		this.getElement().setProperty("showBarbell", true);
		this.showBarbell = showBarbell;
	}

	@Override
	public void setSilenced(boolean silenced) {
		this.athleteTimer.setSilenced(silenced);
		this.breakTimer.setSilenced(silenced);
		this.silenced = silenced;
	}

	@Override
	public void setTeamWidth(Double tw) {
	}

	@Override
	public void setVideo(boolean b) {
		this.video = b;
	}

	@Subscribe
	public void slaveBarbellOrPlatesChanged(UIEvent.BarbellOrPlatesChanged e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> showPlates());
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			syncWithFOP(e.getFop());
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			syncWithFOP(e.getFop());
		});
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			// records only: athlete fields stay frozen so the board matches the decision being shown
			publish(buildRecordUpdate(e.getFop()));
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to show down on the slave devices -- the master device is the one where
	 * refereeing buttons are attached.
	 *
	 * @param e
	 */
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		if (Config.getCurrent().featureSwitch(FeatureSwitch.PLAYWRIGHT)) {
			logger./*playwright*/warn("{}attemptBoard slaveDownSignal received origin={} self={}", FieldOfPlay.getLoggingName(getFop()),
			        e.getOrigin(), this.getOrigin());
		}
		// Make the decision element container visible on the UI thread, in order with
		// the subsequent Decision/Reset events. (A previous version used a detached
		// thread here, which queued this property change out of order.)
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDecisionLightsVisible(true);
		});
	}

	@Subscribe
	public void slaveInitialDecision(UIEvent.InitialDecision e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		if (Config.getCurrent().featureSwitch(FeatureSwitch.PLAYWRIGHT)) {
			logger./*playwright*/warn("{}attemptBoard slaveInitialDecision received origin={} self={} goodLift={} timingPolicy={}",
			        FieldOfPlay.getLoggingName(getFop()), e.getOrigin(), this.getOrigin(), e.decision, e.getTimingPolicy());
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDecisionLightsVisible(true);
		});
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			Group g = e.getGroup();
			publish(buildDoneState(g));
			hidePlates();
			setDone(true);
		});
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		if (e.isWaitForAnnouncer()) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			String reversalText = "";
			if (e.getReversal() != null) {
				reversalText = e.getReversal() ? Translator.translate("JuryNotification.Reversal")
				        : Translator.translate("JuryNotification.Confirmed");
			}
			int previousAttemptNo;
			switch (e.getDeliberationEventType()) {
				case BAD_LIFT:
					previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
					showJuryNotification(reversalText, Translator.translate("JuryDialog.BadLiftLabel"), e.getAthlete(),
					        formatAttemptByIndex(previousAttemptNo), false, "failNotification",
						        JURY_NOTIFICATION_DIALOG_DURATION_MS);
					break;
				case GOOD_LIFT:
					previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
					showJuryNotification(reversalText, Translator.translate("JuryDialog.GoodLiftLabel"), e.getAthlete(),
					        formatAttemptByIndex(previousAttemptNo), e.getNewRecord(), "successNotification",
						        JURY_NOTIFICATION_DIALOG_DURATION_MS);
					break;
				default:
					break;
			}

		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		if (Config.getCurrent().featureSwitch(FeatureSwitch.ATTEMPT_TRACES)) {
			logger.warn("{}attemptBoard order received seq={} athlete={} startNumber={} weight={} requested={} changedWeight={} attached={}",
					FieldOfPlay.getLoggingName(getFop()), e.getSequence(), e.getAthlete(),
					e.getAthlete() != null ? e.getAthlete().getStartNumber() : null,
					e.getAthlete() != null ? e.getAthlete().getNextAttemptRequestedWeight() : null,
					e.getAthlete() != null ? e.getAthlete().getNextAttemptRequestedWeight() : null,
					e.getNewWeight(), getUI().isPresent());
		}
		FieldOfPlay fop = e.getFop();
		FOPState state = fop.getState();
		BreakType breakType = state == FOPState.BREAK ? fop.getBreakType() : null;
		CeremonyType ceremonyType = fop.getCeremonyType();
		Group group = fop.getGroup();
		Athlete athlete = e.isDisplayToggle() ? e.getAthlete() : fop.getCurAthlete();
		Integer requestedWeight = athlete != null ? athlete.getNextAttemptRequestedWeight() : null;
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			boolean attemptTraces = Config.getCurrent().featureSwitch(FeatureSwitch.ATTEMPT_TRACES);
			this.getElement().setProperty("attemptTraces", attemptTraces);
			if (attemptTraces) {
				logger.debug("{}attemptBoard order applying seq={} state={} athlete={} requested={}",
						FieldOfPlay.getLoggingName(fop), e.getSequence(), state, fop.getCurAthlete(),
						fop.getCurAthlete() != null ? fop.getCurAthlete().getNextAttemptRequestedWeight() : null);
			}
			uiEventLogger.debug("### {} {} isDisplayToggle={}", state, this.getClass().getSimpleName(),
			        e.isDisplayToggle());
			if (state == FOPState.DECISION_VISIBLE) {
				// ignore -- decision reset will resync.
			} else if (state == FOPState.BREAK) {
				if (e.isDisplayToggle()) {
					setDecisionLightsVisible(false);
					publish(buildAthleteState(athlete, fop, state, breakType, requestedWeight));
					updatePlates(athlete != null);
				} else {
					publish(buildBreakState(fop, state, breakType, ceremonyType, athlete, group, requestedWeight));
					updatePlates(athlete != null && breakType != BreakType.GROUP_DONE);
				}
			} else if (state == FOPState.INACTIVE) {
				publish(buildWaitState(fop));
				hidePlates();
			} else if (!e.isCurrentDisplayAffected()) {
				// same as next case
				// logging to see if this ever occurs
				logger.info(">>>>> isCurrentDisplayAffected false");
				setDecisionLightsVisible(false);
				publish(buildAthleteState(athlete, fop, state, breakType, requestedWeight));
				updatePlates(athlete != null);
			} else {
				setDecisionLightsVisible(false);
				publish(buildAthleteState(athlete, fop, state, breakType, requestedWeight));
				updatePlates(athlete != null);
			}
		});
	}

	@ClientCallable
	public void attemptBoardWeightRendered(String sequence, String weightUsedForRendering, Double clientEpochMillis,
			String renderedStartNumber, String renderedWeight, String mode, boolean weightVisible) {
		if (!Config.getCurrent().featureSwitch(FeatureSwitch.ATTEMPT_TRACES)) {
			return;
		}
		String clientTime = clientEpochMillis == null ? "" : CLIENT_TIME_FORMATTER
				.format(Instant.ofEpochMilli(clientEpochMillis.longValue()));
		logger.warn("{}attemptBoard weight rendered seq={} startNumber={} weight={} rendered={} mode={} weightVisible={} clientTime={}",
				FieldOfPlay.getLoggingName(getFop()), sequence, renderedStartNumber, weightUsedForRendering, renderedWeight,
				mode, weightVisible, clientTime);
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to show decisions on the slave devices -- the master device is the one where
	 * refereeing buttons are attached.
	 *
	 * @param e
	 */
	@Subscribe
	public void slaveRefereeDecision(UIEvent.Decision e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		if (Config.getCurrent().featureSwitch(FeatureSwitch.PLAYWRIGHT)) {
			logger./*playwright*/warn("{}attemptBoard slaveRefereeDecision received origin={} self={} goodLift={}",
			        FieldOfPlay.getLoggingName(getFop()), e.getOrigin(), this.getOrigin(), e.decision);
		}
		// hide the athleteTimer except if the decision came from this ui.
		// this does not actually display the down signal, it makes it so the decision
		// element can show the down or decision.
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this.getOrigin(), () -> {
			setDecisionLightsVisible(true);
		});
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		doBreak(e);
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDecisionLightsVisible(false);
			FieldOfPlay fop = e.getFop();
			if (e.getGroup() == null) {
				publish(buildWaitState(fop));
				hidePlates();
				return;
			}
			Athlete curAthlete = fop.getCurAthlete();
			if (curAthlete != null) {
				Athlete refreshed = AthleteRepository.findById(curAthlete.getId());
				publish(buildAthleteState(refreshed, fop));
				updatePlates(true);
				this.athleteTimer.syncWithFop(fop);
			} else {
				publish(buildWaitState(fop));
				hidePlates();
			}
		});
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			syncWithFOP(e.getFop());
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			FieldOfPlay fop = getFop();
			switch (fop.getState()) {
				case INACTIVE:
					publish(buildWaitState(fop));
					hidePlates();
					break;
				case BREAK:
					if (e.getGroup() == null) {
						publish(buildWaitState(fop));
						hidePlates();
					} else {
						publish(buildBreakState(fop));
						updatePlates(fop.getCurAthlete() != null && fop.getBreakType() != BreakType.GROUP_DONE);
					}
					break;
				default:
					Athlete athlete = fop.getCurAthlete();
					setDecisionLightsVisible(false);
					publish(buildAthleteState(athlete, e.getFop()));
					updatePlates(athlete != null);
			}
		});
	}

	protected void checkImages() {
		this.teamFlags = URLUtils.checkFlags();
		setAthletePictures(URLUtils.checkPictures());
	}

	public String computeTeamName(Athlete a) {
		String team = a.getTeam();
		if (team == null) {
			team = "";
		}

		if (Config.getCurrent().featureSwitch(FeatureSwitch.CUSTOM_TEAM_NAME)) {
			var customTeamFormatString = Translator.translateOrElseNull("AttemptBoard.TeamFormat");
			if (customTeamFormatString != null) {
				String custom1 = a.getCustom1();
				String custom2 = a.getCustom2();
				boolean custom1Present = custom1 != null && !custom1.isBlank();
				boolean custom2Present = custom2 != null && !custom2.isBlank();
				int count = custom1Present && custom2Present ? 3 : (custom2Present ? 2 : (custom1Present ? 1 : 0));

				// The message format is expected to be something similar to
				// {0, choice, 0#{1}|1#{1}, {2}|2#{1}, {3}|3#{1}, {2}, {3}}
				// a "binary" encoding is used to control the format
				// count = 0 show only team (00)
				// count = 1 show team and custom1 (01)
				// count = 2 show team and custom2 (10)
				// count = 3 show team, custom1 and custom 2 (11)
				
				team = MessageFormat.format(customTeamFormatString, count, team, custom1 != null ? custom1 : "", custom2 != null ? custom2 : "");
			}
		}
		return team;
	}

	private void addRecordState(AttemptBoardState.Builder builder, FieldOfPlay fop) {
		if (Config.getCurrent().featureSwitch(FeatureSwitch.DISABLE_RECORD_HIGHLIGHT)
		        || fop.getState() == FOPState.INACTIVE || fop.getState() == FOPState.BREAK) {
			return;
		}
		List<RecordEvent> newRecords = fop.getNewRecords();
		if (newRecords != null && !newRecords.isEmpty()) {
			addRecordState(builder, newRecords, Translator.translate("Scoreboard.NewRecord(s)", newRecords.size()), false,
			        true);
			return;
		}
		List<RecordEvent> challengedRecords = fop.getChallengedRecords();
		if (challengedRecords != null && !challengedRecords.isEmpty()) {
			addRecordState(builder, challengedRecords,
			        Translator.translate("Scoreboard.RecordAttempt(s)", challengedRecords.size()), true, false);
		}
	}

	private void addRecordState(AttemptBoardState.Builder builder, List<RecordEvent> records, String prefix,
	        boolean recordAttempt, boolean recordBroken) {
		String recordsList = records.stream().map(RecordEvent::prettyPrint).collect(Collectors.joining(", "));
		builder.recordAttempt(recordAttempt)
				.recordBroken(recordBroken)
				.recordMessage(prefix + " \u2013 " + recordsList)
				.recordMessageSpeed(5 + records.size() * 5);
	}

	private AttemptBoardState buildAthleteState(Athlete athlete, FieldOfPlay fop) {
		FOPState state = fop.getState();
		BreakType breakType = state == FOPState.BREAK ? fop.getBreakType() : null;
		Integer requestedWeight = athlete != null ? athlete.getNextAttemptRequestedWeight() : null;
		return buildAthleteState(athlete, fop, state, breakType, requestedWeight);
	}

	private AttemptBoardState buildAthleteState(Athlete athlete, FieldOfPlay fop, FOPState state,
	        BreakType breakType, Integer requestedWeight) {
		if (state == FOPState.INACTIVE || (state == FOPState.BREAK && breakType == BreakType.GROUP_DONE)) {
			return buildWaitState(fop);
		}
		if (athlete == null) {
			logger.error("{}attemptBoard cannot publish current athlete: athlete is null {}",
			        FieldOfPlay.getLoggingName(fop), LoggerUtils.whereFrom());
			return buildWaitState(fop);
		}
		if (athlete.getAttemptsDone() >= 6) {
			setDone(true);
			return buildDoneState(fop.getGroup());
		}

		if (requestedWeight == null || requestedWeight <= 0) {
			logger.error("{}attemptBoard cannot publish current athlete startNumber={}: requested weight is {} {}",
			        FieldOfPlay.getLoggingName(fop), athlete.getStartNumber(), requestedWeight, LoggerUtils.whereFrom());
			return buildWaitState(fop);
		}

		AttemptBoardState.Builder builder = AttemptBoardState.builder(nextBoardStateSequence(), BoardMode.CURRENT_ATHLETE.name())
				.decisionVisible(this.decisionLightsVisible);
		String lastName = athlete.getLastName();
		builder.lastName(lastName != null ? lastName.toUpperCase() : "");
		if (lastName != null && lastName.length() > 18) {
			builder.nameSizeOverride("font-size: 8vh; line-height: 8vh; text-wrap: balance; text-overflow: hidden")
					.firstNameSizeOverride("font-size: 8vh; line-height: 12vh; text-wrap: wrap; text-overflow: hidden");
		}

		String firstName = athlete.getFirstName();
		if (!athlete.isEligibleForIndividualRanking() && firstName != null && !firstName.isBlank()) {
			firstName = Translator.translate("Attempt.Extra/Invited", firstName);
		}
		Category category = athlete.getCategory();
		builder.firstName(firstName)
				.weight(requestedWeight.toString())
				.category(category != null ? category.getDisplayName() : "")
				.teamName(computeTeamName(athlete))
				.startNumber(athlete.getStartNumber())
				.attempt(formatAttempt(athlete));

		String team = athlete.getTeam();
		if (this.teamFlags && team != null && !team.isBlank()) {
			builder.teamFlagImg(Team.getImgTag(team, ""));
		}
		String membership = athlete.getMembership();
		if (isAthletePictures() && membership != null) {
			String athleteImg = URLUtils.getImgTag("pictures/", membership, ".jpg", "");
			builder.athleteImg(athleteImg != null ? athleteImg : URLUtils.getImgTag("pictures/", membership, ".jpeg", ""));
		}
		addRecordState(builder, fop);
		setDone(false);
		return builder.build();
	}

	private AttemptBoardState buildDoneState(Group group) {
		String groupDone = group != null ? Translator.translate("Group_number_done", group.toString()) : "";
		return AttemptBoardState.builder(nextBoardStateSequence(), BoardMode.SESSION_DONE.name())
				.breakType(BreakType.GROUP_DONE.name())
				.lastName(groupDone)
				.build();
	}

	private AttemptBoardState buildWaitState(FieldOfPlay fop) {
		return AttemptBoardState.builder(nextBoardStateSequence(), BoardMode.WAIT.name())
				.competitionName(Competition.getCurrent().getCompetitionName())
				.build();
	}

	private AttemptBoardState buildBreakState(FieldOfPlay fop) {
		FOPState fopState = fop.getState();
		BreakType breakType = fopState == FOPState.BREAK ? fop.getBreakType() : null;
		CeremonyType ceremonyType = fop.getCeremonyType();
		Athlete athlete = fop.getCurAthlete();
		Integer requestedWeight = athlete != null ? athlete.getNextAttemptRequestedWeight() : null;
		return buildBreakState(fop, fopState, breakType, ceremonyType, athlete, fop.getGroup(), requestedWeight);
	}

	private AttemptBoardState buildBreakState(FieldOfPlay fop, FOPState fopState, BreakType breakType,
	        CeremonyType ceremonyType, Athlete athlete, Group group, Integer requestedWeight) {
		BoardMode boardMode = computeBoardMode(fopState, breakType, ceremonyType);
		boolean weightVisible = boardMode == BoardMode.LIFT_COUNTDOWN
				|| (boardMode == BoardMode.INTERRUPTION && breakType == BreakType.TECHNICAL);
		if (weightVisible && (requestedWeight == null || requestedWeight <= 0)) {
			logger.error("{}attemptBoard cannot publish {}: athlete={} requested weight={} {}",
			        FieldOfPlay.getLoggingName(fop), boardMode, athlete, requestedWeight, LoggerUtils.whereFrom());
			return buildWaitState(fop);
		}
		AttemptBoardState.Builder builder = AttemptBoardState
				.builder(nextBoardStateSequence(), boardMode.name())
				.decisionVisible(this.decisionLightsVisible)
				.breakType(breakType != null ? breakType.name() : "");

		if (breakType == BreakType.GROUP_DONE) {
			if (athlete != null && athlete.getAttemptsDone() < 6) {
				return builder.lastName(inferGroupName(group, ceremonyType))
						.firstName(inferMessage(breakType, ceremonyType, true))
						.build();
			}
			return builder.lastName(group != null
					? Translator.translate("Group_number_done", group.toString())
					: "").build();
		}

		if (breakType == BreakType.JURY || breakType == BreakType.CHALLENGE) {
			return builder.lastName(inferGroupName(group, null))
					.firstName(inferMessage(breakType, ceremonyType, true))
					.build();
		}

		builder.lastName(inferGroupName(group, breakType == BreakType.CEREMONY ? ceremonyType : null))
				.firstName(inferMessage(breakType, ceremonyType, true));
		if (athlete != null) {
			Category category = athlete.getCategory();
			builder.category(category != null ? category.getDisplayName() : "")
					.attempt(formatAttempt(athlete))
					.weight(requestedWeight != null && requestedWeight > 0 ? requestedWeight.toString() : "");
		}
		return builder.build();
	}

	private String inferGroupName(Group group, CeremonyType ceremonyType) {
		if (ceremonyType == CeremonyType.MEDALS || group == null || group.getName().isBlank()) {
			return "";
		}
		return Translator.translate("Group_number", group.getName());
	}

	private AttemptBoardState buildRecordUpdate(FieldOfPlay fop) {
		if (this.lastBoardState == null) {
			return buildAthleteState(fop.getCurAthlete(), fop);
		}
		AttemptBoardState.Builder builder = this.lastBoardState.copy(nextBoardStateSequence())
				.decisionVisible(this.decisionLightsVisible)
				.recordAttempt(false)
				.recordBroken(false)
				.recordMessage("")
				.recordMessageSpeed(0);
		addRecordState(builder, fop);
		return builder.build();
	}

	private long nextBoardStateSequence() {
		return ++this.boardStateSequence;
	}

	private void setDecisionLightsVisible(boolean visible) {
		if (Config.getCurrent().featureSwitch(FeatureSwitch.ATTEMPT_TRACES)) {
			FieldOfPlay fop = getFop();
			FOPState fopState = fop != null ? fop.getState() : null;
			// decision phases where visible lights are coherent with the FOP
			boolean coherent = visible == (fopState == FOPState.DOWN_SIGNAL_VISIBLE || fopState == FOPState.DECISION_VISIBLE);
			logger.warn("{}attemptBoard decisionVisible={} fopState={}{} {}", FieldOfPlay.getLoggingName(fop),
			        visible, fopState, coherent ? "" : " MISMATCH", LoggerUtils.whereFrom());
		}
		boolean changed = this.decisionLightsVisible != visible;
		this.decisionLightsVisible = visible;
		this.getElement().setProperty("decisionVisible", visible);
		// keep the dormant snapshot field in sync, but only on real transitions
		if (changed && this.lastBoardState != null) {
			publish(this.lastBoardState.copy(nextBoardStateSequence()).decisionVisible(visible).build());
		}
	}

	private void publish(AttemptBoardState state) {
		this.lastBoardState = state;
		this.getElement().setPropertyJson("boardState", state.toJson());
		if (Config.getCurrent().featureSwitch(FeatureSwitch.ATTEMPT_TRACES)) {
			logger.warn("{}attemptBoard state published seq={}", FieldOfPlay.getLoggingName(getFop()), state.getSequence());
		}
	}

	private void updatePlates(boolean visible) {
		if (visible) {
			showPlates();
		} else {
			hidePlates();
		}
	}

	protected Object getOrigin() {
		return this;
	}

	protected boolean isAthletePictures() {
		return this.athletePictures;
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		ui = UI.getCurrent();
		FieldOfPlay fop = getFop();
		if (fop == null) {
			logger.error("No FOP available in onAttach; FOP must be provided via route parameters");
			return;
		}
		// Timer elements are injected by Vaadin @Id after setFop() was called.
		// Re-propagate FOP to timer elements now that they're available.
		propagateFopToTimerElements(fop);
		logger.debug("{}onAttach {}", FieldOfPlay.getLoggingName(fop), fop.getState());
		init();
		computeStylesDir(this);
		ThemeList themeList = UI.getCurrent().getElement().getThemeList();
		themeList.remove(Lumo.LIGHT);
		themeList.add(Lumo.DARK);

		if (!isSilenced() || !isDownSilenced()) {
			SoundUtils.enableAudioContextNotification(this.getElement());
		}

		syncWithFOP(fop);
		this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
		// we send on fopEventBus, listen on uiEventBus.
		this.uiEventBus = uiEventBusRegister(this, fop);
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		clearJuryNotification();
		super.onDetach(detachEvent);
	}

	protected void setAthletePictures(boolean athletePictures) {
		this.athletePictures = athletePictures;
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		this.getElement().setPropertyJson("t", translations);
	}

	protected void syncWithFOP(FieldOfPlay fop) {
		if (fop.getState() == FOPState.INACTIVE && fop.getCeremonyType() == null) {
			publish(buildWaitState(fop));
			hidePlates();
			return;
		}

		Athlete currentAthlete = fop.getCurAthlete();
		if (fop.getState() == FOPState.BREAK || fop.getState() == FOPState.INACTIVE) {
			publish(buildBreakState(fop));
			updatePlates(currentAthlete != null && fop.getBreakType() != BreakType.GROUP_DONE);
			return;
		}

		if (currentAthlete == null) {
			publish(buildWaitState(fop));
			hidePlates();
			return;
		}
		Athlete refreshedAthlete = AthleteRepository.findById(currentAthlete.getId());
		setDecisionLightsVisible(false);
		publish(buildAthleteState(refreshedAthlete, fop));
		updatePlates(refreshedAthlete != null);
		this.athleteTimer.syncWithFop(fop);
	}

	private String formatAttempt(Athlete a) {
		return formatAttemptByIndex(a.getAttemptsDone());
	}

	private String formatAttemptByIndex(int attemptIndex) {
		int attemptNo = attemptIndex % 3 + 1;
		String translation = Translator.translateOrElseNull("AttemptBoard_lift_attempt_number", getLocale());
		if (translation != null) {
			String liftKey = attemptIndex < 3 ? "AttemptBoard_lift.SNATCH" : "AttemptBoard_lift.CLEANJERK";
			translation = Translator.translate("AttemptBoard_lift_attempt_number", attemptNo,
			        Translator.translate(liftKey));
		} else {
			translation = Translator.translate("AttemptBoard_attempt_number", attemptNo);
		}
		return translation;
	}

	private void hidePlates() {
		if (this.plates != null) {
			try {
				this.getElement().removeChild(this.plates.getElement());
			} catch (IllegalArgumentException e) {
				// ignore
			}
		}
		this.plates = null;
	}

	private void init() {
		FieldOfPlay fop = getFop();
		logger.trace("{}Starting attempt board", FieldOfPlay.getLoggingName(fop));
		setTranslationMap();
	}

	@SuppressWarnings("unused")
	private boolean isDone() {
		return this.groupDone;
	}

	private void setDone(boolean b) {
		this.groupDone = b;
	}

	private void showPlates() {
		FieldOfPlay fop = getFop();
		try {
			if (this.plates != null) {
				this.getElement().removeChild(this.plates.getElement());
			}
			this.plates = new PlatesElement();
			this.plates.computeImageArea(fop, false);
			Element platesElement = this.plates.getElement();
			// tell polymer that the plates belong in the slot named barbell of the template
			platesElement.setAttribute("slot", "barbell");
			platesElement.getStyle().set("font-size", "3.3vh");
			platesElement.getClassList().set("dark", true);
			this.getElement().appendChild(platesElement);
		} catch (Throwable t) {
			LoggerUtils.logError(logger, t);
		}
	}

	private void clearJuryNotification() {
		cancelJuryNotificationTimer();
		closeJuryNotificationDialog();
	}

	private void showJuryNotification(String status, String decision, Athlete athlete, String attempt, boolean newRecord,
	        String notificationClass, int duration) {
		clearJuryNotification();
		Dialog dialog = ensureJuryNotificationDialog();
		dialog.removeAll();
		dialog.add(new JuryNotificationCard(status, decision, athlete, attempt, newRecord, notificationClass));
		dialog.open();
		scheduleJuryNotificationClose(duration);
	}

	private void cancelJuryNotificationTimer() {
		if (this.juryNotificationTimer != null) {
			this.juryNotificationTimer.cancel();
			this.juryNotificationTimer.purge();
			this.juryNotificationTimer = null;
		}
	}

	private void closeJuryNotificationDialog() {
		if (this.juryNotificationDialog != null) {
			this.juryNotificationDialog.close();
			this.juryNotificationDialog.removeAll();
		}
	}

	private Dialog ensureJuryNotificationDialog() {
		if (this.juryNotificationDialog == null) {
			this.juryNotificationDialog = new Dialog();
			this.juryNotificationDialog.addThemeName("jury-notification-dialog");
			this.juryNotificationDialog.setCloseOnEsc(false);
			this.juryNotificationDialog.setCloseOnOutsideClick(false);
		}
		return this.juryNotificationDialog;
	}

	private void scheduleJuryNotificationClose(int duration) {
		if (duration <= 0 || this.ui == null) {
			return;
		}
		cancelJuryNotificationTimer();
		this.juryNotificationTimer = new Timer("jury-notification-dialog", true);
		this.juryNotificationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				UI currentUi = AbstractAttemptBoard.this.ui;
				if (currentUi != null) {
					currentUi.access(() -> clearJuryNotification());
				}
			}
		}, duration);
	}

	@Override
	public boolean isPublicDisplay() {
		return publicDisplay;
	}

}
