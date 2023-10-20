/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
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
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
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
	 * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties. When the JS
	 * properties are changed, a "propname-changed" event is triggered.
	 * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
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
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public CurrentAthlete(AbstractDisplayPage page) {
		super();
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			uiEventLogger.debug("$$$ currentAthlete calling doBreak()");
			if (fop.getGroup() != null && fop.getGroup().isDone()) {
				setDisplay();
				getElement().setProperty("fullName", getTranslation("Group_number_done", fop.getGroup().toString()));
				getElement().setProperty("teamName", "");
				getElement().setProperty("attempt", "");
			} else {
				getElement().setProperty("fullName",
				        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
				getElement().setProperty("teamName", "");
				getElement().setProperty("attempt", "");
				setDisplay();

				updateBottom(computeLiftType(fop.getCurAthlete()), fop);
				uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
			}
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		uiEventLogger.debug("$$$ currentAthlete calling doCeremony()");
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			getElement().setProperty("fullName",
			        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			getElement().setProperty("teamName", "");
			getElement().setProperty("attempt", "");
			setDisplay();

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);

		}));
	}

	/**
	 * Reset.
	 */
	@Override
	public void reset() {
		order = ImmutableList.of();
	}

	@Override
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			Athlete a = e.getAthlete();
			setDisplay();
			if (a == null) {
				order = fop.getLiftingOrder();
				a = order.size() > 0 ? order.get(0) : null;
				// liftsDone = AthleteSorter.countLiftsDone(order);
				doUpdate(a, e);
			} else {
				// liftsDone = AthleteSorter.countLiftsDone(order);
				doUpdate(a, e);
			}
		}));
	}

	@Override
	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		// logger.trace"------- slaveCeremonyDone {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
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
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			doCeremony(e);
		});
	}

	@Override
	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", true);
		});
	}

	@Override
	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", false);
			if (isDone()) {
				doDone(e.getAthlete().getGroup());
			} else {
				doUpdate(e.getAthlete(), e);
			}
		});
	}

	@Override
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", true);
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
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			setDone(true);
			doBreak(e);
		});
	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		// uiLog(e);
		FieldOfPlay fop = OwlcmsSession.getFop();
		FOPState state = fop.getState();
		if (state == FOPState.DOWN_SIGNAL_VISIBLE || state == FOPState.DECISION_VISIBLE) {
			return;
		}
		uiEventLogger.debug("### {} isDisplayToggle={}", this.getClass().getSimpleName(), e.isDisplayToggle());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			order = e.getDisplayOrder();
			// liftsDone = AthleteSorter.countLiftsDone(order);
			doUpdate(a, e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		// logger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		// this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			doBreak(e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
		});
	}

	@Override
	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			Athlete a = e.getAthlete();
			doUpdate(a, e);
		});
	}

	@Override
	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
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
//        logger.debug("doUpdate {} {} {}", e != null ? e.getClass().getSimpleName() : "no event", a,
//                a != null ? a.getAttemptsDone() : null);
		boolean leaveTopAlone = false;
		if (e instanceof UIEvent.LiftingOrderUpdated) {
			LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
			if (e2.isInBreak()) {
				leaveTopAlone = !e2.isDisplayToggle();
			} else {
				leaveTopAlone = !e2.isCurrentDisplayAffected();
			}
		}

		FieldOfPlay fop = OwlcmsSession.getFop();
		if (!leaveTopAlone) {
			if (a != null) {
				Group group = fop.getGroup();
				if (!group.isDone()) {
					logger.debug("updating top {} {} {}", a.getFullName(), group, System.identityHashCode(group));
					getElement().setProperty("fullName", a.getFullName());
					getElement().setProperty("teamName", a.getTeam());
					getElement().setProperty("startNumber", a.getStartNumber());
					String formattedAttempt = formatAttempt(a.getAttemptsDone());
					getElement().setProperty("attempt", formattedAttempt);
					getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
				} else {
					logger.debug("group done {} {}", group, System.identityHashCode(group));
					doBreak(e);
				}
			}

			// current athlete bottom should only change when top does
			if (fop.getState() != FOPState.DECISION_VISIBLE) {
				// logger.debug("updating bottom {}", fop.getState());
				updateBottom(computeLiftType(a), fop);
			} else {
				// logger.debug("not updating bottom {}", fop.getState());
			}

		}
		// logger.debug("leave top alone {} {}", leaveTopAlone, fop.getState());
		if (leaveTopAlone && fop.getState() == FOPState.CURRENT_ATHLETE_DISPLAYED) {
			updateBottom(computeLiftType(a), fop);
		}

	}

	@Override
	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		String category;
		category = curCat != null ? curCat.getTranslatedName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank, fop);
		ja.put("sattempts", sattempts);
		ja.put("cattempts", cattempts);
		ja.put("total", formatInt(a.getTotal()));
		ja.put("snatchRank", formatInt(a.getMainRankings().getSnatchRank()));
		ja.put("cleanJerkRank", formatInt(a.getMainRankings().getCleanJerkRank()));
		ja.put("totalRank", formatInt(a.getMainRankings().getTotalRank()));
		ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
//        boolean notDone = a.getAttemptsDone() < 6;
//        String blink = (notDone ? " blink" : "");
//        if (notDone) {
//            ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
//        }
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
		sattempts = Json.createArray();
		cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		Integer curLift = x.getAttemptsDone();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;
			String blink = "";// (notDone ? " blink" : "");

			jri.put("goodBadClassName", "empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
				case ACTUAL:
					if (!trim.isEmpty()) {
						if (trim.contentEquals("-") || trim.contentEquals("0")) {
							jri.put("goodBadClassName", "fail");
							jri.put("stringValue", "-");
						} else {
							boolean failed = stringValue != null && stringValue.startsWith("-");
							jri.put("goodBadClassName", failed ? "fail" : "good");
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
						jri.put("goodBadClassName", "request");
						if (notDone) {
							jri.put("className", highlight);
						}
						jri.put("stringValue", stringValue);
					}
					break;
				}
			}

			if (ix < 3) {
				sattempts.set(ix, jri);
			} else {
				cattempts.set(ix % 3, jri);
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
		OwlcmsSession.withFop(fop -> {
			init();
			checkVideo(this);

			// get the global category rankings attached to each athlete
			order = fop.getDisplayOrder();

			// liftsDone = AthleteSorter.countLiftsDone(order);
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
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
	protected void updateBottom(String liftType, FieldOfPlay fop) {
		// logger.debug("updateBottom {}",LoggerUtils.stackTrace());
		if (liftType != null) {
			getElement().setProperty("groupName", "");
			getElement().setProperty("liftsDone", "");
		} else {
			getElement().setProperty("groupName", "X");
			getElement().setProperty("liftsDone", "Y");
		}
		this.getElement().setPropertyJson("athletes",
		        getAthletesJson(order, fop.getLiftingOrder(), fop));
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private void doDone(Group g) {
		logger.debug("doDone {}", g == null ? null : g.getName());
		if (g == null) {
			doEmpty();
		} else {
			OwlcmsSession.withFop(fop -> {
				updateBottom(null, fop);
				getElement().setProperty("fullName", getTranslation("Group_number_done", g.toString()));
			});
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
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting result board", fop.getLoggingName());
			setId("scoreboard-" + fop.getName());
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		});
		setTranslationMap();
		order = ImmutableList.of();
	}

	private boolean isDone() {
		return this.groupDone;
	}

	private void setDisplay() {
		OwlcmsSession.withFop(fop -> {
			setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), this.getElement());
			Group group = fop.getGroup();
			String description = null;
			if (group != null) {
				description = group.getDescription();
				if (description == null) {
					description = Translator.translate("Group_number", group.getName());
				}
			}
			this.getElement().setProperty("groupDescription", description != null ? description : "");
		});
	}

	private void setDone(boolean b) {
		this.groupDone = b;
	}

	private void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
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