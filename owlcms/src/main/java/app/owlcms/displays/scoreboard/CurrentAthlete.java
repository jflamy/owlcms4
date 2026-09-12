/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.template.Id;

import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEventSequenceGuard;
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("currentathlete-template")
@JsModule("./components/CurrentAthlete.js")

public class CurrentAthlete extends Results {

	/**
	 * ScoreboardModel
	 *
	 */

	final private static Logger logger = (Logger) LoggerFactory.getLogger(CurrentAthlete.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	JsonArray cattempts;
	JsonArray sattempts;
	@Id("breakTimer")
	private BreakTimerElement breakTimer; // Flow creates it
	@Id("decisions")
	private DecisionElement decisions; // Flow creates it
	private boolean groupDone;
	private List<Athlete> order;
	@Id("timer")
	private AthleteTimerElement timer; // Flow creates it
	private EventBus uiEventBus;
	private long boardStateSequence;
	private CurrentAthleteState lastBoardState;
	// guarded by ui.access; see UIEventSequenceGuard
	private final UIEventSequenceGuard orderGuard = new UIEventSequenceGuard();
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public CurrentAthlete(AbstractDisplayPage page) {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	@Override
	protected void propagateFopToTimerElements(FieldOfPlay fop) {
		if (this.breakTimer != null) {
			this.breakTimer.setFop(fop);
		}
		if (this.timer != null) {
			this.timer.setFop(fop);
		}
		if (this.decisions != null) {
			this.decisions.setFop(fop);
		}
	}

	@Override
	public void doBreak(UIEvent e) {
		uiEventLogger.debug("$$$ currentAthlete calling doBreak()");
		publishState(null);
	}

	@Override
	protected void doBreakLocked(UIEvent e) {
		doBreak(e);
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		uiEventLogger.debug("$$$ currentAthlete calling doCeremony()");
		publishState(null);
	}

	/**
	 * Reset.
	 */
	@Override
	public void reset() {
		this.order = ImmutableList.of();
	}

	@Override
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			FieldOfPlay fop = getFop();
			Athlete a = e.getAthlete();
			if (a == null) {
				this.order = fop.getLiftingOrder();
				a = this.order.size() > 0 ? this.order.get(0) : null;
				// liftsDone = AthleteSorter.countLiftsDone(order);
				doUpdate(a, e);
			} else {
				// liftsDone = AthleteSorter.countLiftsDone(order);
				doUpdate(a, e);
			}
		});
	}

	@Override
	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		// logger.trace"------- slaveCeremonyDone {}", e.getCeremonyType());
		uiLog(e);
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
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			doCeremony(e);
		});
	}

	@Override
	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			publishDecisionState(e.getAthlete());
		});
	}

	@Override
	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		if (e.getAthlete() == null) {
			FieldOfPlay fop = e.getFop();
			logger.warn("{}TEMP null-athlete DecisionReset seq={} fopState={} curAthlete={} groupDone={} created at:\n{}",
			        FieldOfPlay.getLoggingName(fop), e.getSequence(), fop != null ? fop.getState() : null,
			        fop != null ? fop.getCurAthlete() : null, isDone(), e.getTrace());
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (isDone()) {
				// no current athlete once the session is complete
				doDone(getFop().getGroup());
			} else {
				doUpdate(getFop().getCurAthlete(), e);
			}
		});
	}

	@Override
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			publishDecisionState(null);
		});
	}

	@Subscribe
	public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
		uiLog(e);
	}

	@Override
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		logger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDone(true);
			publishState(getFop().getPreviousAthlete());
		});
	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		// uiLog(e);
		FieldOfPlay fop = e.getFop();
		FOPState state = fop.getState();
		if (state == FOPState.DOWN_SIGNAL_VISIBLE || state == FOPState.DECISION_VISIBLE) {
			return;
		}
		uiEventLogger.debug("### {} isDisplayToggle={}", this.getClass().getSimpleName(), e.isDisplayToggle());
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (isStaleOrderEvent(e)) {
				return;
			}
			this.order = e.getDisplayOrder();
			doUpdate(e.getAthlete(), e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		// logger.debug("### {} {} {} {}", this.getClass().getSimpleName(),
		// e.getClass().getSimpleName(),
		// this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			doBreak(e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
		});
	}

	@Override
	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			Athlete a = e.getAthlete();
			doUpdate(a, e);
		});
	}

	@Override
	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (this.orderGuard.isStale(e.getSequence())) {
				return;
			}
			syncWithFOP(e);
		});
	}

	@Override
	public void uiLog(UIEvent e) {
		if (uiEventLogger.isDebugEnabled()) {
			uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			        this.getOrigin(), e.getOrigin(), LoggerUtils.whereFrom());
		}
	}

	@Override
	protected void doEmpty() {
		this.setDisplay();
	}

	@Override
	protected void doUpdate(Athlete a, UIEvent e) {
		FieldOfPlay fop = e.getFop();
		if (fop == null) {
			doEmpty();
			return;
		}
		if (fop.getState() == FOPState.DECISION_VISIBLE || fop.getState() == FOPState.DOWN_SIGNAL_VISIBLE) {
			return;
		}
		publishState(a);
	}

	@Override
	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		String category;
		category = curCat != null ? curCat.getDisplayName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank, fop);
		ja.put("sattempts", this.sattempts);
		ja.put("cattempts", this.cattempts);
		ja.put("total", formatInt(a.getTotal()));
		ja.put("snatchRank", formatInt(a.getMainRankings().getSnatchRank()));
		ja.put("cleanJerkRank", formatInt(a.getMainRankings().getCleanJerkRank()));
		ja.put("totalRank", formatInt(a.getMainRankings().getTotalRank()));
		ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
		// boolean notDone = a.getAttemptsDone() < 6;
		// String blink = (notDone ? " blink" : "");
		// if (notDone) {
		// ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
		// }
		ja.put("className", "");
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	@Override
	protected JsonValue getAthletesJson(List<Athlete> groupAthletes, List<Athlete> liftOrder, FieldOfPlay fop) {
		JsonArray jath = Json.createArray();
		int athx = 0;

		long currentId = (liftOrder != null && liftOrder.size() > 0) ? liftOrder.get(0).getId() : -1L;
		long nextId = (liftOrder != null && liftOrder.size() > 1) ? liftOrder.get(1).getId() : -1L;
		List<Athlete> athletes = groupAthletes != null ? Collections.unmodifiableList(groupAthletes)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			if (a.getId() != currentId) {
				continue;
			}

			JsonObject ja = Json.createObject();
			Category curCat = a.getCategory();
			// compute the blinking rank (1 = current, 2 = next)
			getAthleteJson(a, ja, curCat, (a.getId() == currentId)
			        ? 1
			        : ((a.getId() == nextId)
			                ? 2
			                : 0),
			        fop);
			String team = a.getTeam();
			if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
				logger.trace("long team {}", team);
				setWideTeamNames(true);
			}
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}

	/**
	 * Compute Json string ready to be used by web component template
	 *
	 * CSS classes are pre-computed and passed along with the values; weights are formatted.
	 *
	 * @param a
	 * @param liftOrderRank2
	 * @return json string with nested attempts values
	 */
	@Override
	protected void getAttemptsJson(Athlete a, int liftOrderRank, FieldOfPlay fop) {
		this.sattempts = Json.createArray();
		this.cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		Integer curLift = x.getAttemptsDone();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;
			String blink = "";// (notDone ? " blink" : "");

			jri.put("liftStatus", "empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
					case ACTUAL:
						if (!trim.isEmpty()) {
							if (trim.contentEquals("-") || trim.contentEquals("0")) {
								jri.put("liftStatus", "fail");
								jri.put("stringValue", "-");
							} else {
								boolean failed = stringValue != null && stringValue.startsWith("-");
								jri.put("liftStatus", failed ? "fail" : "good");
								jri.put("stringValue", formatKg(stringValue));
							}
						}
						break;
					default:
						if (stringValue != null && !trim.isEmpty()) {
							// logger.debug("{} {} {}", fop.getState(), x.getShortName(), curLift);

							String highlight = "";
							// don't blink while decision is visible. wait until lifting order has been
							// recomputed and we get DECISION_RESET

							if (i.getLiftNo() == curLift && (fop.getState() != FOPState.DECISION_VISIBLE)) {
								switch (liftOrderRank) {
									case 1:
										highlight = (" current" + blink);
										break;
									case 2:
										highlight = " next";
										break;
									default:
										highlight = "";
								}
							}
							jri.put("liftStatus", "request");
							if (notDone) {
								jri.put("className", highlight);
							}
							jri.put("stringValue", stringValue);
						}
						break;
				}
			}

			if (ix < 3) {
				this.sattempts.set(ix, jri);
			} else {
				this.cattempts.set(ix % 3, jri);
			}
			ix++;
		}
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		FieldOfPlay fop = getFop();
		// Timer elements are injected by Vaadin @Id after setFop() was called.
		// Re-propagate FOP to timer elements now that they're available.
		propagateFopToTimerElements(fop);
		init();
		computeStylesDir(this);

		// get the global category rankings attached to each athlete
		this.order = fop.getDisplayOrder();

		// liftsDone = AthleteSorter.countLiftsDone(order);
		syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this, fop));
		// we listen on uiEventBus.
		this.uiEventBus = uiEventBusRegister(this, fop);
		this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
	}

	@Override
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

	@Override
	protected void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
	}

	@Override
	protected void updateDisplay(String liftType, FieldOfPlay fop) {
		if (fop.getState() != FOPState.DECISION_VISIBLE && fop.getState() != FOPState.DOWN_SIGNAL_VISIBLE) {
			publishState(null);
		}
	}

	private void doDone(Group g) {
		logger.debug("doDone {}", g == null ? null : g.getName());
		if (g == null) {
			doEmpty();
		} else {
			publishState(null);
		}
	}

	private String formatAttempt(Integer attemptNo) {
		String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
		return translate;
	}

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	private Object getOrigin() {
		return this;
	}

	private void init() {
		FieldOfPlay fop = getFop();
		logger.trace("{}Starting result board", FieldOfPlay.getLoggingName(fop));
		setId("scoreboard-" + fop.getName());
		setWideTeamNames(false);
		this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		setTranslationMap();
		this.order = ImmutableList.of();
	}

	private boolean isDone() {
		return this.groupDone;
	}

	private void setDisplay() {
		publishState(null);
	}

	private void publishState(Athlete eventAthlete) {
		FieldOfPlay fop = getFop();
		boolean attemptedLift = fop.getState() == FOPState.DECISION_VISIBLE
		        || fop.getState() == FOPState.DOWN_SIGNAL_VISIBLE;
		BoardMode mode = computeBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType());
		Group group = fop.getGroup();
		String description = null;
		if (group != null) {
			description = group.getDescription();
			if (description == null) {
				description = Translator.translate("Group_number", group.getName());
			}
		}

		Athlete athlete = mode == BoardMode.SESSION_DONE ? fop.getPreviousAthlete()
		        : eventAthlete != null ? eventAthlete
		        : fop.getState() == FOPState.DECISION_VISIBLE && fop.getAthleteUnderReview() != null
		                ? fop.getAthleteUnderReview() : fop.getCurAthlete();
		CurrentAthleteState.Builder builder = CurrentAthleteState.builder(++this.boardStateSequence, mode.name())
		        .groupDescription(description)
		        .showDecisions(attemptedLift)
		        .athletes(singleAthleteJson(athlete, fop));

		if (mode == BoardMode.SESSION_DONE) {
			builder.fullName(group != null ? Translator.translate("Group_number_done", group.toString()) : "");
		} else if (mode == BoardMode.CURRENT_ATHLETE && athlete != null) {
			builder.fullName(athlete.getFullName())
			        .team(athlete.getTeam())
			        .lift(formatAttempt(attemptedLift ? fop.getLiftsDoneAtLastStart() : athlete.getAttemptsDone()))
			        .startNumber(athlete.getStartNumber())
			        .weight(attemptedLift ? fop.getWeightAtLastStart() : athlete.getNextAttemptRequestedWeight());
		} else if (mode != BoardMode.WAIT) {
			builder.fullName(inferGroupName() + " &ndash; "
			        + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			if (mode == BoardMode.LIFT_COUNTDOWN || mode == BoardMode.LIFT_COUNTDOWN_CEREMONY) {
				builder.weight(athlete != null ? athlete.getNextAttemptRequestedWeight() : null);
			}
		}

		publish(builder.build());
	}

	private void publishDecisionState(Athlete athlete) {
		if (this.lastBoardState != null && this.lastBoardState.isCurrentAthlete()) {
			publish(this.lastBoardState.withDecision(++this.boardStateSequence));
		} else {
			// no retained snapshot (display just attached): the attempted lift belongs to the current athlete
			publishState(athlete != null ? athlete : getFop().getCurAthlete());
		}
	}

	private void publish(CurrentAthleteState state) {
		this.lastBoardState = state;
		this.getElement().setPropertyJson("boardState", state.toJson());
		this.getUI().ifPresent(UI::push);
	}

	private boolean isStaleOrderEvent(UIEvent.LiftingOrderUpdated e) {
		long lastApplied = this.orderGuard.getLastApplied();
		boolean stale = this.orderGuard.isStale(e.getSequence());
		if (stale) {
			logger.debug("dropping out-of-order LiftingOrderUpdated seq={} lastApplied={}", e.getSequence(), lastApplied);
		}
		return stale;
	}

	private JsonArray singleAthleteJson(Athlete athlete, FieldOfPlay fop) {
		JsonArray athletes = Json.createArray();
		if (athlete == null) {
			return athletes;
		}
		JsonObject athleteJson = Json.createObject();
		getAthleteJson(athlete, athleteJson, athlete.getCategory(), 1, fop);
		athletes.set(0, athleteJson);
		return athletes;
	}

	private void setDone(boolean b) {
		this.groupDone = b;
	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
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