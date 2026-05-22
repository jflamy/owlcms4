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
import com.vaadin.flow.dom.Element;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.category.Category;
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
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

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
		FieldOfPlay fop = getFop();
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			uiEventLogger.debug("$$$ currentAthlete calling doBreak()");
			if (fop.getGroup() != null && fop.getGroup().isDone()) {
				setDisplay();
				getElement().setProperty("fullName", Translator.translate("Group_number_done", fop.getGroup().toString()));
			} else {
				getElement().setProperty("fullName",
				        inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
				setDisplay();
				updateDisplay(computeLiftType(fop.getCurAthlete()), fop);
			}
		});
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		uiEventLogger.debug("$$$ currentAthlete calling doCeremony()");
		FieldOfPlay fop = getFop();
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			getElement().setProperty("fullName",
			        inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			getElement().setProperty("teamName", "");
			setDisplay();

			updateDisplay(computeLiftType(fop.getCurAthlete()), fop);

		});
	}

	@Override
	public void reset() {
	}
	
	public void setDetails(Element element, boolean b) {
		if (logger.isDebugEnabled()) logger.debug("setting details {} {}",b, LoggerUtils.whereFrom());
		element.setProperty("showDetails", b);
	}
	
	@Override
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		super.slaveBreakDone(e);
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
			setDisplay();
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
			setDisplay();
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
			setShowDecisions(this.getElement(), false);
			setShowAthleteClock(this.getElement(), false);
			setShowBreakClock(this.getElement(), false);
			this.getElement().setProperty("showDetails", true);
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
				logger.warn("exception while showing decision\n{}", LoggerUtils.stackTrace(e1));
			}
		});
	}

	private void showDecision(Athlete athlete, FieldOfPlay fop, Boolean decision, Boolean ref1, Boolean ref2,
	        Boolean ref3, boolean singleLight) {
		setDisplay();
		if (athlete != null) {
			setDecisionAthleteText(athlete, fop);
			computeIndicators(athlete, 1, fop, decision);
		}

		JsonArray decisions = Json.createArray();
		if (singleLight) {
			decisions.set(0, ref2 != null ? ref2 : decision);
		} else {
			decisions.set(0, ref1);
			decisions.set(1, ref2);
			decisions.set(2, ref3);
		}
		this.getElement().setPropertyJson("decisions", decisions);

		setShowDecisions(this.getElement(), true);
		setShowAthleteClock(this.getElement(), false);
		setShowBreakClock(this.getElement(), false);
		this.getElement().setProperty("showDetails", true);
	}

	private void setDecisionAthleteText(Athlete athlete, FieldOfPlay fop) {
		int attemptNumber = fop != null ? fop.getLiftsDoneAtLastStart() : athlete.getAttemptsDone();
		this.getElement().setProperty("fullName", athlete.getFullName() != null ? athlete.getFullName() : "");
		this.getElement().setProperty("team", athlete.getTeam() != null ? formatTeam(athlete) : "");
		this.getElement().setProperty("lift", formatAttempt(attemptNumber));
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
			setDisplay();
			setShowDecisions(this.getElement(), false);
			setShowAthleteClock(this.getElement(), false);
			setShowBreakClock(this.getElement(), false);
			setDetails(this.getElement(),true);
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
			setDisplay();
			setShowDecisions(this.getElement(), false);
			doBreak(e);
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
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> showInterruption(fop.getBreakType()));
			return;
		}
		uiEventLogger.debug("### {} isDisplayToggle={}", this.getClass().getSimpleName(), e.isDisplayToggle());
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			doUpdate(a, e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
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
			setShowDecisions(this.getElement(), false);
			setShowAthleteClock(this.getElement(), true);
			setShowBreakClock(this.getElement(), false);
			setDetails(this.getElement(),true);
		});
	}

	@Override
	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		if (isDecisionStickyActive(e)) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
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
		super.slaveSwitchGroup(e);
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
		Element element = this.getElement();
		element.setProperty("mode", "INTERRUPTION");
		element.setProperty("breakType", breakType.name());
		element.setProperty("fullName", inferMessage(breakType, null, true));
		element.setProperty("team", "");
		element.setProperty("lift", "");
		setShowDecisions(element, false);
		setShowAthleteClock(element, false);
		setShowBreakClock(element, false);
		setDetails(element, false);
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
		FieldOfPlay fop = e.getFop();
		if (a != null) {
			getElement().setProperty("fullName", a.getFullName());
			getElement().setProperty("team", formatTeam(a));
			getElement().setProperty("lift", formatAttempt(a.getAttemptsDone()));
		}
		updateDisplay(computeLiftType(a), fop, a);
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
	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		this.getElement().setProperty("fullName", a.getFullName() != null ? a.getFullName() : "");
		this.getElement().setProperty("team", a.getTeam() != null ? formatTeam(a) : "");
		this.getElement().setProperty("lift", formatAttempt(a.getAttemptsDone()));
		this.getElement().setProperty("logoSrc", getLogoSrc());
		computeIndicators(a, liftOrderRank, fop);
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
		JsonObject ja = Json.createObject();
		if (a != null) {
			getAthleteJson(a, ja, a.getCategory(), 1, fop);
			setDetails(this.getElement(),true);
			setShowAthleteClock(this.getElement(), true);
			setShowBreakClock(this.getElement(), false);
			setShowDecisions(this.getElement(), false);
		}
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
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
	
	private void computeIndicators(Athlete a, int liftOrderRank, FieldOfPlay fop) {
		computeIndicators(a, liftOrderRank, fop, null);
	}

	private void computeIndicators(Athlete a, int liftOrderRank, FieldOfPlay fop, Boolean decisionOverride) {
		XAthlete x = new XAthlete(a);
		Integer curLift = x.getAttemptsDone();
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
		this.getElement().setPropertyJson("snIndicators", snIndicators);
		this.getElement().setPropertyJson("snIndicatorClasses", snIndicatorClasses);
		this.getElement().setPropertyJson("cjIndicators", cjIndicators);
		this.getElement().setPropertyJson("cjIndicatorClasses", cjIndicatorClasses);
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
		FieldOfPlay fop = getFop();
		FOPState fopState = fop.getState();
		BreakType breakType = fop.getBreakType();
		Element element = this.getElement();
		BoardMode bm = computeBoardMode(fopState, breakType, fop.getCeremonyType());
		if (logger.isDebugEnabled()) logger.debug("setting board mode {} {}", bm.name(), LoggerUtils.whereFrom());
		switch (bm) {
			case WAIT:
				element.setProperty("mode", "WAIT");
				element.setProperty("fullName", Translator.translate("Scoreboard.WaitingNextGroup"));
				setShowDecisions(element,false);
				setShowAthleteClock(element, false);
				setShowBreakClock(element,false);
				setDetails(element, false);
				break;
			case INTRO_COUNTDOWN:
				element.setProperty("mode", "INTRO_COUNTDOWN");
				setShowDecisions(element,false);			
				setShowAthleteClock(element, false);
				setShowBreakClock(element,true);
				setDetails(element, false);
				break;
			case CEREMONY:
				element.setProperty("mode", "CEREMONY");
				setShowDecisions(element,false);
				setShowAthleteClock(element, false);
				setShowBreakClock(element,true);
				setDetails(element, false);
				break;
			case LIFT_COUNTDOWN:
				element.setProperty("mode", "LIFT_COUNTDOWN");
				setShowDecisions(element,false);
				setShowAthleteClock(element, false);
				setShowBreakClock(element,true);
				setDetails(element, false);
				break;
			case CURRENT_ATHLETE:
				element.setProperty("mode", "CURRENT_ATHLETE");
//				setShowDecisions(element,false);
//				setShowAthleteClock(element, true);
//				setShowBreakClock(element,false);
				setDetails(element,  true);
				break;
			case INTERRUPTION:
				element.setProperty("mode", "INTERRUPTION");
				setShowDecisions(element,false);
				setShowAthleteClock(element, false);
				setShowBreakClock(element,false);
				setDetails(element, false);
				break;
			case SESSION_DONE:
				element.setProperty("mode", "SESSION_DONE");
				setShowDecisions(element,false);
				setShowAthleteClock(element, false);
				setShowBreakClock(element,false);
				setDetails(element, false);
				break;
			case LIFT_COUNTDOWN_CEREMONY:
				element.setProperty("mode", "LIFT_COUNTDOWN_CEREMONY");
				setShowAthleteClock(element, true);
				setShowBreakClock(element,false);
				setDetails(element, false);
				break;
		}

		element.setProperty("breakType", fopState == FOPState.BREAK ? breakType.name() : null);
		Group group = fop.getGroup();
		String description = null;
		if (group != null) {
			description = group.getDescription();
			if (description == null) {
				description = Translator.translate("Group_number", group.getName());
			}
		}
		this.getElement().setProperty("groupDescription", description != null ? description : "");
	}

	private void setShowBreakClock(Element element, boolean b) {
		if (logger.isDebugEnabled()) logger.debug("showBreakClock({}) {}", b, LoggerUtils.whereFrom());
		element.setProperty("showBreakClock", b);
	}
	
	private void setShowAthleteClock(Element element, boolean b) {
		if (logger.isDebugEnabled()) logger.debug("showAthleteClock({}) {}", b, LoggerUtils.whereFrom());
		element.setProperty("showAthleteClock", b);;
	}
	
	private void setShowDecisions(Element element, boolean b) {
		if (logger.isDebugEnabled()) logger.debug("showDecisions({}) {}", b, LoggerUtils.whereFrom());
		element.setProperty("showDecisions",b);
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
					doUpdate(e.getAthlete(), e);
					doBreak(e);
				}
				break;
			default:
				setDisplay();
				doUpdate(e.getAthlete(), e);
		}
	}
}
