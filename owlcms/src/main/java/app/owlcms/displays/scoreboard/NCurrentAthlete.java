/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.GroupDone;
import app.owlcms.uievents.UIEventSequenceGuard;
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * NCurrentAthlete: feeds the NCurrentAthlete.js web component.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("ncurrentathlete-template")
@JsModule("./components/NCurrentAthlete.js")
public class NCurrentAthlete extends Results {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(NCurrentAthlete.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private EventBus uiEventBus;
	private int decisionStickMillis;
	private Timer decisionTimer;
	private volatile long decisionStickyUntil;
	private long boardStateSequence;
	// guarded by ui.access; see UIEventSequenceGuard
	private final UIEventSequenceGuard orderGuard = new UIEventSequenceGuard();
	private UI ui;

	public NCurrentAthlete(AbstractDisplayPage page) {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	private static boolean isSingleLight(FieldOfPlay fop) {
		InputKind inputKind = fop != null ? fop.getCurrentInputKind() : null;
		return inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT;
	}

	private static Boolean refereeDecision(FieldOfPlay fop, int index) {
		Boolean[] decisions = fop != null ? fop.getRefereeDecision() : null;
		return decisions != null && decisions.length > index ? decisions[index] : null;
	}

	@Override
	public void doBreak(UIEvent e) {
		uiEventLogger.debug("$$$ currentAthlete calling doBreak()");
		publishState(null, null, null, false);
	}

	@Override
	protected void doBreakLocked(UIEvent e) {
		doBreak(e);
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		uiEventLogger.debug("$$$ currentAthlete calling doCeremony()");
		publishState(null, null, null, false);
	}

	@Override
	public void reset() {
	}
	
	@Override
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> doUpdate(getFop().getCurAthlete(), e));
	}

	@Override
	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		// logger.trace"------- slaveCeremonyDone {}", e.getCeremonyType());
		uiLog(e);
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			// revert to current break
			doBreak(null);
		});
	}
	
	@Override
	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.trace"------- slaveCeremonyStarted {}", e.getCeremonyType());
		uiLog(e);
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			doCeremony(e);
		});
	}

	@Override
	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		if (e.decision == null && isDecisionStickyActive(e)) {
			return;
		}
		if (e.decision == null) {
			if (logger.isDebugEnabled()) logger.debug("waiting for decision");
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () ->
			        publishState(e.getAthlete(), null, null, false, BoardMode.CURRENT_ATHLETE, null, true));
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			try {
				if (logger.isDebugEnabled()) logger.debug("showing decision");
				cancelDecisionTimer();

				Athlete athlete = e.getAthlete() != null ? e.getAthlete() : getFop().getCurAthlete();
				showDecision(athlete, e.getFop(), e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight());

				scheduleDecisionStick();
			} catch (Exception e1) {
				logger.error("exception while showing decision\n{}", LoggerUtils.stackTrace(e1));
			}
		});
	}

	private void showDecision(Athlete athlete, FieldOfPlay fop, Boolean decision, Boolean ref1, Boolean ref2,
	        Boolean ref3, boolean singleLight) {
		JsonArray decisions = Json.createArray();
		if (singleLight) {
			decisions.set(0, decisionColor(ref2 != null ? ref2 : decision));
		} else {
			decisions.set(0, decisionColor(ref1));
			decisions.set(1, decisionColor(ref2));
			decisions.set(2, decisionColor(ref3));
		}
		publishState(athlete, decision, decisions, true, BoardMode.CURRENT_ATHLETE, null, true);
	}

	private String decisionColor(Boolean decision) {
		return decision == null ? "empty" : decision ? "white" : "red";
	}

	private boolean recoverDecisionVisibleStick(UIEvent.SwitchGroup e) {
		if (this.decisionStickMillis <= 0 || e.getState() != FOPState.DECISION_VISIBLE) {
			return false;
		}
		FieldOfPlay fop = e.getFop();
		if (fop == null) {
			return false;
		}
		Boolean decision = fop.getGoodLift();
		if (decision == null) {
			return false;
		}

		Athlete athlete = fop.getAthleteUnderReview();
		if (athlete == null) {
			athlete = fop.getPreviousAthlete();
		}
		if (athlete == null) {
			athlete = e.getAthlete();
		}
		Boolean ref1 = refereeDecision(fop, 0);
		Boolean ref2 = refereeDecision(fop, 1);
		Boolean ref3 = refereeDecision(fop, 2);
		boolean singleLight = isSingleLight(fop);
		cancelDecisionTimer();
		showDecision(athlete, fop, decision, ref1, ref2, ref3, singleLight);
		scheduleDecisionStick();
		return true;
	}
	
	private void cancelDecisionTimer() {
		this.decisionStickyUntil = 0;
		if (this.decisionTimer != null) {
			this.decisionTimer.cancel();
			this.decisionTimer = null;
		}
	}

	private boolean isDecisionStickyActive(UIEvent e) {
		boolean active = this.decisionStickyUntil > System.currentTimeMillis();
		return active;
	}

	private void scheduleDecisionStick() {
		if (this.decisionStickMillis <= 0) {
			return;
		}
		UI capturedUi = this.ui;
		FieldOfPlay fop = getFop();
		if (capturedUi == null || fop == null) {
			return;
		}
		long stickyUntil = System.currentTimeMillis() + this.decisionStickMillis;
		this.decisionStickyUntil = stickyUntil;
		Timer timer = new Timer("decision-stick", true);
		this.decisionTimer = timer;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					capturedUi.access(() -> {
						if (NCurrentAthlete.this.decisionStickyUntil != stickyUntil) {
							timer.cancel();
							return;
						}
						clearDecisionStick(timer, stickyUntil);
						syncWithFOP(
						        new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), NCurrentAthlete.this, fop),
						        false);
					});
					timer.cancel();
				} catch (UIDetachedException ignored) {
					clearDecisionStick(timer, stickyUntil);
				}
			}
		}, this.decisionStickMillis);
	}

	private void clearDecisionStick(Timer timer, long stickyUntil) {
		if (this.decisionStickyUntil == stickyUntil) {
			this.decisionStickyUntil = 0;
		}
		if (this.decisionTimer == timer) {
			this.decisionTimer = null;
		}
		timer.cancel();
	}

	@Override
	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
//		uiLog(e);
//		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
//			setDisplay();
//			this.getElement().setProperty("decisionVisible", false);
//			if (isDone()) {
//				doDone(e.getAthlete().getGroup());
//			} else {
//				OwlcmsSession.withFop(fop -> doUpdate(fop.getCurAthlete(), e));
//			}
//		});
	}

	@Override
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			publishState(e.getFop().getCurAthlete(), null, null, false,
			        BoardMode.CURRENT_ATHLETE, null, true);
		});
	}

	@Override
	@Subscribe
	public void slaveGroupDone(GroupDone e) {
		uiLog(e);
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			publishState(getFop().getPreviousAthlete(), null, null, false);
		});
	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		if (logger.isDebugEnabled()) logger.debug("slaveOrderUpdated called with event: {}", e);
		if (isDecisionStickyActive(e)) {
			return;
		}
		FieldOfPlay fop = e.getFop();
		FOPState state = fop.getState();
		if (state == FOPState.DOWN_SIGNAL_VISIBLE || state == FOPState.DECISION_VISIBLE) {
			return;
		}
		if (state == FOPState.BREAK && fop.getBreakType() != null && fop.getBreakType().isInterruption()) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				if (isStaleOrderEvent(e)) {
					return;
				}
				showInterruption(fop.getBreakType());
			});
			return;
		}
		uiEventLogger.debug("### {} isDisplayToggle={}", this.getClass().getSimpleName(), e.isDisplayToggle());
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (isStaleOrderEvent(e)) {
				return;
			}
			doUpdate(e.getAthlete(), e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			doBreak(e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		if (isDecisionStickyActive(e)) {
			return;
		}
		cancelDecisionTimer();
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
		});
	}

	@Override
	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			Athlete a = e.getAthlete();
			doUpdate(a, e);
		});
	}

	@Override
	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (this.orderGuard.isStale(e.getSequence())) {
				return;
			}
			syncWithFOP(e);
		});
	}

	private boolean isStaleOrderEvent(UIEvent.LiftingOrderUpdated e) {
		long lastApplied = this.orderGuard.getLastApplied();
		boolean stale = this.orderGuard.isStale(e.getSequence());
		if (stale) {
			logger.debug("dropping out-of-order LiftingOrderUpdated seq={} lastApplied={}", e.getSequence(), lastApplied);
		}
		return stale;
	}

	@Override
	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		BreakType breakType = breakTypeFromJuryNotification(e);
		if (breakType != null) {
			uiLog(e);
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> showInterruption(breakType));
			return;
		}
		super.slaveJuryNotification(e);
	}

	private BreakType breakTypeFromJuryNotification(UIEvent.JuryNotification e) {
		JuryDeliberationEventType eventType = e.getDeliberationEventType();
		if (eventType == null) {
			return null;
		}
		switch (eventType) {
			case START_DELIBERATION:
				return BreakType.JURY;
			case CHALLENGE:
				return BreakType.CHALLENGE;
			case MARSHALL:
				return BreakType.MARSHAL;
			case TECHNICAL_PAUSE:
				return BreakType.TECHNICAL;
			default:
				return null;
		}
	}

	private void showInterruption(BreakType breakType) {
		publishState(null, null, null, false, BoardMode.INTERRUPTION, breakType, false);
	}

	public void setDecisionStickMillis(int decisionStickMillis) {
		this.decisionStickMillis = Math.max(0, decisionStickMillis);
		if (this.decisionStickMillis == 0) {
			cancelDecisionTimer();
		}
	}
	
	@Override
	protected void doEmpty() {
		//super.doEmpty();
		if (logger.isDebugEnabled()) logger.debug("doEmpty() {}", LoggerUtils.whereFrom());
		setDisplay();
	}

	@Override
	protected void doUpdate(Athlete a, UIEvent e) {
		if (logger.isDebugEnabled()) logger.debug("doUpdate called with athlete: {} {}", a, LoggerUtils.whereFrom());
		publishState(a, null, null, false);
	}

	public String formatTeam(Athlete a) {
		String team = a.getTeam();
		if (team != null && !team.isBlank()) {
			return team;
		} else {
			return "";
		}
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		FieldOfPlay fop = getFop();
		// Timer elements are injected by Vaadin @Id after setFop() was called.
		// Re-propagate FOP to timer elements now that they're available.
		propagateFopToTimerElements(fop);
		setId("ncurrentathlete-" + fop.getName());
		init();
		computeStylesDir(this);

		this.ui = UI.getCurrent();
		syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this, fop));
		// we listen on uiEventBus.
		this.uiEventBus = uiEventBusRegister(this, fop);
		this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
		this.getElement().setProperty("logoSrc", getLogoSrc());
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		cancelDecisionTimer();
		this.ui = null;
		super.onDetach(detachEvent);
	}

	@Override
	protected void updateDisplay(String liftType, FieldOfPlay fop) {
		updateDisplay(liftType, fop, null);
	}

	protected void updateDisplay(String liftType, FieldOfPlay fop, Athlete a) {
		if (!isDecisionStickyActive(null)) {
			publishState(a, null, null, false);
		}
	}


	private String formatAttempt(Integer attemptNo) {
		String liftType = attemptNo >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
		return liftType + "<br>" + translate;
	}
	
	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}
	
	private IndicatorState buildIndicators(Athlete a, FieldOfPlay fop, Boolean decisionOverride) {
		XAthlete x = new XAthlete(a);
		Integer curLift = decisionOverride != null ? fop.getLiftsDoneAtLastStart() : x.getAttemptsDone();
		JsonArray snIndicators = Json.createArray();
		JsonArray snIndicatorClasses = Json.createArray();
		JsonArray cjIndicators = Json.createArray();
		JsonArray cjIndicatorClasses = Json.createArray();

		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			String stringValue = i.getStringValue();
			String trim = stringValue != null ? stringValue.trim() : "";
			String className = "empty";
			String value = "";

			if (i.getChangeNo() >= 0) {
				switch (Changes.values()[i.getChangeNo()]) {
					case ACTUAL:
						if (!trim.isEmpty()) {
							if (trim.contentEquals("-") || trim.contentEquals("0")) {
								className = "red";
								value = "-";
							} else {
								boolean failed = stringValue != null && stringValue.startsWith("-");
								className = failed ? "red" : "white";
								value = formatKg(stringValue);
							}
						}
						break;
					default:
						if (stringValue != null && !trim.isEmpty()) {
								if (i.getLiftNo() == curLift) {
									if (decisionOverride != null) {
										className = decisionOverride ? "white" : "red";
									} else if (fop.getState() != FOPState.DECISION_VISIBLE) {
										className = "current";
									} else {
										className = "empty";
									}
							} else {
								className = "empty";
							}
							value = stringValue;
						}
						break;
				}
			}

			if (ix < 3) {
				snIndicators.set(ix, value);
				snIndicatorClasses.set(ix, className);
			} else {
				cjIndicators.set(ix % 3, value);
				cjIndicatorClasses.set(ix % 3, className);
			}
			ix++;
		}
		return new IndicatorState(snIndicators, snIndicatorClasses, cjIndicators, cjIndicatorClasses);
	}

	private static class IndicatorState {

		private final JsonArray cjIndicatorClasses;
		private final JsonArray cjIndicators;
		private final JsonArray snIndicatorClasses;
		private final JsonArray snIndicators;

		private IndicatorState(JsonArray snIndicators, JsonArray snIndicatorClasses, JsonArray cjIndicators,
		        JsonArray cjIndicatorClasses) {
			this.snIndicators = snIndicators;
			this.snIndicatorClasses = snIndicatorClasses;
			this.cjIndicators = cjIndicators;
			this.cjIndicatorClasses = cjIndicatorClasses;
		}
	}
	
	protected void init() {
		FieldOfPlay fop = getFop();
		logger.trace("{}Starting result board", FieldOfPlay.getLoggingName(fop));
		setId("scoreboard-" + fop.getName());
		setWideTeamNames(false);
		this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		setTranslationMap();
	}
	
	private void setDisplay() {
		publishState(null, null, null, false);
	}

	private void publishState(Athlete eventAthlete, Boolean decisionOverride, JsonArray decisions,
	        boolean decisionVisible) {
		FieldOfPlay fop = getFop();
		publishState(eventAthlete, decisionOverride, decisions, decisionVisible,
		        computeBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType()),
		        fop.getBreakType(), false);
	}

	private void publishState(Athlete eventAthlete, Boolean decisionOverride, JsonArray decisions,
	        boolean decisionVisible, BoardMode mode, BreakType breakType, boolean attemptedLift) {
		FieldOfPlay fop = getFop();
		FOPState fopState = fop.getState();
		if (logger.isDebugEnabled()) logger.debug("setting board mode {} {}", mode.name(), LoggerUtils.whereFrom());
		Group group = fop.getGroup();
		String description = null;
		if (group != null) {
			description = group.getDescription();
			if (description == null) {
				description = Translator.translate("Group_number", group.getName());
			}
		}
		Athlete athlete = mode == BoardMode.SESSION_DONE ? fop.getPreviousAthlete()
		        : eventAthlete != null ? eventAthlete : fop.getCurAthlete();
		CurrentAthleteState.Builder builder = CurrentAthleteState.builder(++this.boardStateSequence, mode.name())
		        .groupDescription(description)
		        .showAthleteClock((fopState == FOPState.CURRENT_ATHLETE_DISPLAYED
		                || fopState == FOPState.TIME_RUNNING || fopState == FOPState.TIME_STOPPED
		                || mode == BoardMode.LIFT_COUNTDOWN_CEREMONY)
		                && (mode == BoardMode.CURRENT_ATHLETE || mode == BoardMode.LIFT_COUNTDOWN_CEREMONY)
		                && !attemptedLift && !decisionVisible)
		        .showAttemptResults(mode == BoardMode.CURRENT_ATHLETE || mode == BoardMode.SESSION_DONE)
		        .showBreakClock(mode == BoardMode.INTRO_COUNTDOWN || mode == BoardMode.LIFT_COUNTDOWN
		                || mode == BoardMode.CEREMONY)
		        .showDecisions(decisionVisible)
		        .showDetails(mode == BoardMode.CURRENT_ATHLETE)
		        .decisions(decisions);

		if (athlete != null && (mode == BoardMode.CURRENT_ATHLETE || mode == BoardMode.SESSION_DONE)) {
			IndicatorState indicators = buildIndicators(athlete, fop, decisionOverride);
			builder.snIndicators(indicators.snIndicators)
			        .snIndicatorClasses(indicators.snIndicatorClasses)
			        .cjIndicators(indicators.cjIndicators)
			        .cjIndicatorClasses(indicators.cjIndicatorClasses);
		}

		if (mode == BoardMode.SESSION_DONE) {
			builder.fullName(group != null ? Translator.translate("Group_number_done", group.toString()) : "");
		} else if (mode == BoardMode.CURRENT_ATHLETE && athlete != null) {
			builder.fullName(athlete.getFullName())
			        .team(formatTeam(athlete))
			        .lift(formatAttempt(attemptedLift ? fop.getLiftsDoneAtLastStart() : athlete.getAttemptsDone()));
		} else if (mode == BoardMode.WAIT) {
			builder.fullName(Translator.translate("Scoreboard.WaitingNextGroup"));
		} else {
			builder.fullName(inferMessage(breakType,
			        mode == BoardMode.INTERRUPTION ? null : fop.getCeremonyType(), true));
		}

		this.getElement().setPropertyJson("boardState", builder.build().toJson());
		this.getUI().ifPresent(UI::push);
	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
		syncWithFOP(e, true);
	}

	private void syncWithFOP(UIEvent.SwitchGroup e, boolean recoverDecisionVisible) {
		if (recoverDecisionVisible && recoverDecisionVisibleStick(e)) {
			return;
		}
		switch (e.getState()) {
			case INACTIVE:
				doEmpty();
				break;
			case BREAK:
				if (e.getGroup() == null) {
					doEmpty();
				} else {
					doBreak(e);
				}
				break;
			default:
				doUpdate(e.getAthlete(), e);
		}
	}
}
