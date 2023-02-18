/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
import java.util.Timer;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.queryparameters.DisplayParameters;
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
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
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

@Route("displays/currentathlete")

public class CurrentAthlete extends PolymerTemplate<TemplateModel>
        implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle,
        RequireDisplayLogin {

	/**
	 * ScoreboardModel
	 *
	 * Vaadin Flow propagates these variables to the corresponding Polymer template
	 * JavaScript properties. When the JS properties are changed, a
	 * "propname-changed" event is triggered.
	 * {@link Element.#addPropertyChangeListener(String, String,
	 * com.vaadin.flow.dom.PropertyChangeListener)}
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

	private boolean darkMode;
	@Id("decisions")
	private DecisionElement decisions; // Flow creates it

	private Dialog dialog;
	private boolean groupDone;
	private boolean initializationNeeded;
	private Location location;
	private UI locationUI;
	private List<Athlete> order;
	@Id("timer")
	private AthleteTimerElement timer; // Flow creates it
	private EventBus uiEventBus;
	private Timer dialogTimer;

	private String routeParameter;

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	/**
	 * Instantiates a new results board.
	 */
	public CurrentAthlete() {
		OwlcmsFactory.waitDBInitialized();
		timer.setOrigin(this);
		setDarkMode(true);
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSoundEntries(vl, target, this);
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			getElement().setProperty("fullName",
			        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			getElement().setProperty("teamName", "");
			getElement().setProperty("attempt", "");
			setHidden(false);

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
			uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
			this.getElement().callJsFunction("doBreak");
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			getElement().setProperty("fullName",
			        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			getElement().setProperty("teamName", "");
			getElement().setProperty("attempt", "");
			setHidden(false);

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
			uiEventLogger.debug("$$$ attemptBoard calling doCeremony()");
			this.getElement().callJsFunction("doBreak");
		}));
	}

	/**
	 * return dialog, but only on first call.
	 *
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#getDialog()
	 */
	@Override
	public Dialog getDialog() {
		return dialog;
	}

	@Override
	public Timer getDialogTimer() {
		return this.dialogTimer;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getPageTitle() {
		return getTranslation("CurrentAthleteTitle") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public boolean isDarkMode() {
		return darkMode;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
	 */
	@Override
	public boolean isShowInitialDialog() {
		return this.initializationNeeded;
	}

	@Override
	public boolean isSilenced() {
		return true;
	}

	/**
	 * Reset.
	 */
	public void reset() {
		order = ImmutableList.of();
	}

	@Override
	public void setDarkMode(boolean dark) {
		this.darkMode = dark;
	}

	@Override
	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void setDialogTimer(Timer timer) {
		this.dialogTimer = timer;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setShowInitialDialog(boolean)
	 */
	@Override
	public void setShowInitialDialog(boolean b) {
		this.initializationNeeded = true;
	}

	@Override
	public void setSilenced(boolean silent) {
		// no op, silenced by definition
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			Athlete a = e.getAthlete();
			setHidden(false);
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

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setHidden(false);
			// do not update the bottom part until decision has been shown
			// doUpdateBottomPart(e);
			this.getElement().setProperty("hideBlock", "visibility:hidden");
			this.getElement().setProperty("noneBlock", "display:none");
			this.getElement().setProperty("hideInherited", "visibility:hidden");
			this.getElement().setProperty("hideTableCell", "visibility:hidden");
			this.getElement().callJsFunction("refereeDecision");
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setHidden(false);
			if (isDone()) {
				doDone(e.getAthlete().getGroup());
			} else {
				doUpdate(e.getAthlete(), e);
				this.getElement().setProperty("hideBlock", "visibility:visible");
				this.getElement().setProperty("noneBlock", "display:block");
				this.getElement().setProperty("hideInherited", "visibility:visible");
				this.getElement().setProperty("hideTableCell", "visibility:visible");
				this.getElement().callJsFunction("reset");
			}
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setHidden(false);
			this.getElement().callJsFunction("down");
		});
	}

	@Subscribe
	public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
		uiLog(e);
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setHidden(false);
//          Group g = e.getGroup();
			setDone(true);
		});
	}

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

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setHidden(false);
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setHidden(false);
			this.getElement().callJsFunction("reset");
		});
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setHidden(false);
			Athlete a = e.getAthlete();
			this.getElement().callJsFunction("reset");
			doUpdate(a, e);
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(e);
		});
	}

	public void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin(), LoggerUtils.whereFrom());
	}

//    private void doUpdateBottomPart(UIEvent e) {
//        Athlete a = e.getAthlete();
//        updateBottom(computeLiftType(a), OwlcmsSession.getFop());
//    }

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
				this.getElement().callJsFunction("groupDone");
			});
		}
	}

	private String formatAttempt(Integer attemptNo) {
		String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
		return translate;
	}

	private String formatInt(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else if (total < 0) {
			return "(" + Math.abs(total) + ")";
		} else {
			return total.toString();
		}
	}

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	private void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		String category;
		category = curCat != null ? curCat.getName() : "";
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
	private JsonValue getAthletesJson(List<Athlete> groupAthletes, List<Athlete> liftOrder, FieldOfPlay fop) {
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
	 * CSS classes are pre-computed and passed along with the values; weights are
	 * formatted.
	 *
	 * @param a
	 * @param liftOrderRank2
	 * @return json string with nested attempts values
	 */
	private void getAttemptsJson(Athlete a, int liftOrderRank, FieldOfPlay fop) {
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

	private void setDone(boolean b) {
		this.groupDone = b;
	}

	private void setHidden(boolean hidden) {
		this.getElement().setProperty("hiddenBlockStyle", (hidden ? "display:none" : "display:block"));
		this.getElement().setProperty("inactiveBlockStyle", (hidden ? "display:block" : "display:none"));
		this.getElement().setProperty("hiddenGridStyle", (hidden ? "display:none" : "display:grid"));
		this.getElement().setProperty("inactiveGridStyle", (hidden ? "display:grid" : "display:none"));
		this.getElement().setProperty("inactiveClass", (hidden ? "bigTitle" : ""));
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
			setHidden(false);
			doUpdate(e.getAthlete(), e);
		}
	}

	private void updateBottom(String liftType, FieldOfPlay fop) {
		// logger.debug("updateBottom {}",LoggerUtils.stackTrace());
		if (liftType != null) {
			getElement().setProperty("groupName", "");
			getElement().setProperty("liftsDone", "");
		} else {
			getElement().setProperty("groupName", "X");
			getElement().setProperty("liftsDone", "Y");
			this.getElement().callJsFunction("groupDone");
		}
		this.getElement().setPropertyJson("athletes",
		        getAthletesJson(order, fop.getLiftingOrder(), fop));
	}

	protected void doEmpty() {
		this.setHidden(true);
	}

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
			this.getElement().callJsFunction("reset");

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

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();

			// get the global category rankings attached to each athlete
			order = fop.getDisplayOrder();

			// liftsDone = AthleteSorter.countLiftsDone(order);
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
		switchLightingMode(this, isDarkMode(), true);
		this.getElement().setProperty("video", routeParameter != null ? routeParameter + "/" : "");
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
}