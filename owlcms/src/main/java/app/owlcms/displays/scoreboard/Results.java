/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Location;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.ResultsParameters;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AbstractLifterComparator;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.team.Team;
import app.owlcms.displays.video.StylesDirSelection;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.HasBoardMode;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Results
 *
 * Show results scoreboard for a session, including records and leaders
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class Results extends LitTemplate
        implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay,
        RequireDisplayLogin, HasBoardMode, StylesDirSelection {

	@Id("timer")
	private AthleteTimerElement timer; // WebComponent, injected by Vaadin
	@Id("breakTimer")
	private BreakTimerElement breakTimer; // WebComponent, injected by Vaadin
	@Id("decisions")
	private DecisionElement decisions; // WebComponent, injected by Vaadin
	private final Logger logger = (Logger) LoggerFactory.getLogger(Results.class);
	private final Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	private JsonArray cattempts;
	private Group curGroup;
	private JsonArray sattempts;
	private List<Athlete> displayOrder;
	private int liftsDone;
	private boolean darkMode = true;
	protected EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private boolean teamFlags;
	private FieldOfPlay fop;
	private Group group;
	private Location location;
	private UI locationUI;
	private String routeParameter;
	private boolean silenced;
	private boolean abbreviatedName;
	private Double emFontSize;
	private boolean publicDisplay;
	private Double teamWidth;
	private boolean leadersDisplay;
	private boolean recordsDisplay;
	private HashMap<Athlete, String> athleteToFlag = new HashMap<>();
	private boolean video;
	private boolean downSilenced;

	public Results() {
		this.uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
		this.getElement().setProperty("scoreboardType", this.getClass().getSimpleName());
	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	@Override
	public void doBreak(UIEvent event) {
		//this.logger.debug("Results doBreak {}", LoggerUtils.stackTrace());
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), this.getElement());

			String title = inferGroupName() + " &ndash; "
			        + inferMessage(fop.getBreakType(), fop.getCeremonyType(), isPublicDisplay());
			this.getElement().setProperty("fullName", title);
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("attempt", "");
			this.getElement().setProperty("kgSymbol", Translator.translate("kgSymbol"));
			Athlete a = fop.getCurAthlete();

			this.getElement().setProperty("weight", "");
			Integer nextAttemptRequestedWeight = null;
			if (a != null) {
				nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
			}
			if (fop.getCeremonyType() == null && a != null && nextAttemptRequestedWeight != null
			        && nextAttemptRequestedWeight > 0) {
				this.getElement().setProperty("weight", nextAttemptRequestedWeight);
			}
			setDisplay();
			updateDisplay(computeLiftType(a), fop);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
	}

	public BreakTimerElement getBreakTimer() {
		return this.breakTimer;
	}

	public JsonArray getCattempts() {
		return this.cattempts;
	}

	public DecisionElement getDecisions() {
		return this.decisions;
	}

	@Override
	public final Double getEmFontSize() {
		return this.emFontSize;
	}

	@Override
	final public FieldOfPlay getFop() {
		return this.fop;
	}

	@Override
	final public Group getGroup() {
		return this.group;
	}

	final public Location getLocation() {
		return this.location;
	}

	final public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public final String getRouteParameter() {
		return this.routeParameter;
	}

	final public JsonArray getSattempts() {
		return this.sattempts;
	}

	@Override
	public final Double getTeamWidth() {
		return this.teamWidth;
	}

	public AthleteTimerElement getTimer() {
		return this.timer;
	}

	final public Map<String, List<String>> getUrlParameterMap() {
		return this.urlParameterMap;
	}

	@Override
	public final boolean isAbbreviatedName() {
		return this.abbreviatedName;
	}

	@Override
	public final boolean isDarkMode() {
		return this.darkMode;
	}

	@Override
	public final boolean isDownSilenced() {
		return this.downSilenced;
	}

	@Override
	public final boolean isLeadersDisplay() {
		return this.leadersDisplay;
	}

	@Override
	public final boolean isPublicDisplay() {
		return this.publicDisplay;
	}

	@Override
	public final boolean isRecordsDisplay() {
		return this.recordsDisplay;
	}

	public boolean isShowInitialDialog() {
		return false;
	}

	@Override
	public final boolean isSilenced() {
		return this.silenced;
	}

	@Override
	public final boolean isVideo() {
		return this.video;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#pushEmSize()
	 */
	@Override
	public void pushEmSize() {
		String formattedEm = null;
		if (this.emFontSize != null) {
			formattedEm = ResultsParameters.formatEN_US.format(this.emFontSize);
			this.getElement().setProperty("sizeOverride", " --tableFontSize:" + formattedEm + "em;");
			// logger.trace("%%%%% board changing em size={} from
			// {}",emFontSize,LoggerUtils.whereFrom());
		}
	}

	@Override
	public void pushTeamWidth() {
		String formattedTW = null;
		if (this.teamWidth != null) {
			formattedTW = ResultsParameters.formatEN_US.format(this.teamWidth);
			this.getElement().setProperty("twOverride", "--nameWidth: 1fr; --clubWidth:" + formattedTW + "em;");
		}
	}

	/**
	 * Reset.
	 */
	public void reset() {
		this.displayOrder = ImmutableList.of();
	}

	@Override
	public final void setAbbreviatedName(boolean b) {
		this.abbreviatedName = b;
	}

	public void setBreakTimer(BreakTimerElement breakTimer) {
		this.breakTimer = breakTimer;
	}

	public void setCattempts(JsonArray cattempts) {
		this.cattempts = cattempts;
	}

	@Override
	public final void setDarkMode(boolean dark) {
		this.darkMode = dark;
		getElement().getClassList().set(DisplayParameters.DARK, dark);
		getElement().getClassList().set(DisplayParameters.LIGHT, !dark);

		String value = this.darkMode ? DisplayParameters.DARK : DisplayParameters.LIGHT;
		getElement().setProperty("darkMode", value);
	}

	public void setDecisions(DecisionElement decisions) {
		this.decisions = decisions;
	}

	@Override
	public void setDownSilenced(boolean silent) {
		this.downSilenced = silent;
		this.getDecisions().setSilenced(silent);
	}

	@Override
	public final void setEmFontSize(Double emFontSize) {
		// logger.trace("%%%%% setEmFontSize {}", emFontSize);
		this.emFontSize = emFontSize;
		pushEmSize();
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	final public void setGroup(Group group) {
		this.group = group;
	}

	@Override
	public void setLeadersDisplay(boolean b) {
		this.leadersDisplay = b;
		this.getElement().setProperty("showLeaders", b);
		FieldOfPlay fop = OwlcmsSession.getFop();
		boolean done = fop.getState() == FOPState.BREAK && fop.getBreakType() == BreakType.GROUP_DONE;
		if (!isLeadersDisplay() || done) {
			this.logger.debug("setLeadersDisplay 0px: isLeaders = {} done = {}", isLeadersDisplay(), done);
			this.getElement().setProperty("leaderFillerHeight", "--leaderFillerHeight: 0px");
		} else {
			this.logger.debug("setLeadersDisplay default: isLeaders = {} done = {}", isLeadersDisplay(), done);
			this.getElement().setProperty("leaderFillerHeight",
			        "--leaderFillerHeight: var(--defaultLeaderFillerHeight)");
		}
	}

	final public void setLocation(Location location) {
		this.location = location;

	}

	final public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setPublicDisplay(boolean publicDisplay) {
		this.publicDisplay = publicDisplay;
	}

	@Override
	public void setRecordsDisplay(boolean b) {
		this.recordsDisplay = b;
		this.getElement().setProperty("showRecords", b);
	}

	@Override
	public final void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
		if (routeParameter != null && routeParameter.contentEquals("video")) {
			setVideo(true);
		}
	}

	public void setSattempts(JsonArray sattempts) {
		this.sattempts = sattempts;
	}

	@Override
	public void setSilenced(boolean silent) {
		this.silenced = silent;
		this.getTimer().setSilenced(this.silenced);
		this.getBreakTimer().setSilenced(this.silenced);
	}

	/**
	 * @param a
	 * @param ja
	 */
	public void setTeamFlag(Athlete a, JsonObject ja) {
		String prop = null;
		if (this.athleteToFlag.containsKey(a)) {
			prop = this.athleteToFlag.get(a);
		} else {
			String team = a.getTeam();

			if (this.teamFlags && !team.isBlank()) {
				prop = Team.getImgTag(team, "");
			}

			// prop can be null, will be tested with ContainsKey
			this.athleteToFlag.put(a, prop);
			// ja.put("teamLength", team.isBlank() ? "" : (team.length()*1.2) + "ch");
		}
		ja.put("flagURL", prop != null ? prop : "");
		ja.put("flagClass", "flags");
	}

	@Override
	public void setTeamWidth(Double tw) {
		this.teamWidth = tw;
		pushTeamWidth();
	}

	public void setTimer(AthleteTimerElement timer) {
		this.timer = timer;
	}

	final public void setUrlParameterMap(Map<String, List<String>> parametersMap) {
		this.urlParameterMap = parametersMap;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setVideo(boolean)
	 */
	@Override
	public void setVideo(boolean b) {
		// this.logger.debug("{} setVideo {} from {}", this.getClass(), b,
		// LoggerUtils.whereFrom());
		this.video = b;
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			Athlete a = e.getAthlete();
			setDisplay();
			if (a == null) {
				this.displayOrder = fop.getLiftingOrder();
				a = this.displayOrder.size() > 0 ? this.displayOrder.get(0) : null;
				this.liftsDone = AthleteSorter.countLiftsDone(this.displayOrder);
				doUpdate(a, e);
			} else {
				this.liftsDone = AthleteSorter.countLiftsDone(this.displayOrder);
				doUpdate(a, e);
			}
		}));
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
			// revert to current break
			doBreak(null);
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.trace"------- slaveCeremonyStarted {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", true);
			Athlete a = e.getAthlete();
			// -1 because if decision in on snatch 3 we don't want to show CJ
			updateDisplay(computeLiftType(a.getAttemptsDone() - 1), OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", false);
			Athlete a = e.getAthlete();
			updateDisplay(computeLiftType(a.getAttemptsDone() - 1), OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			this.getElement().setProperty("decisionVisible", true);
			setDisplay();
		});
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
			doDone(e.getGroup());
		});
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
			if (e.getNewRecord()) {
				spotlightNewRecord();
			}
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			this.displayOrder = getOrder(OwlcmsSession.getFop());
			this.liftsDone = AthleteSorter.countLiftsDone(this.displayOrder);
			doUpdate(a, e);
		});
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", false);
			this.getElement().setProperty("recordName", "");
			syncWithFOP();
		});
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			syncWithFOP();
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			syncWithFOP(e);
		});
	}

	protected void computeLeaders(boolean done) {
		OwlcmsSession.withFop(fop -> {
			Athlete curAthlete = fop.getCurAthlete();
			if (curAthlete != null && curAthlete.getGender() != null) {
				this.getElement().setProperty("categoryName", curAthlete.getCategory().getDisplayName());
				if (Competition.getCurrent().isSinclair()) {
					Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
					List<Athlete> sortedAthletes = new ArrayList<>(
					        Competition.getCurrent().getGlobalScoreRanking(curAthlete.getGender()));
					this.displayOrder = AthleteSorter.topScore(sortedAthletes, 3).topAthletes;
					this.getElement().setProperty("categoryName", Ranking.getScoringTitle(scoringSystem));
				} else {
					this.displayOrder = fop.getLeaders();
				}
				if ((!done || Competition.getCurrent().isSinclair()) && this.displayOrder != null
				        && this.displayOrder.size() > 0) {
					// null as second argument because we do not highlight current athletes in the
					// leaderboard
					this.getElement().setPropertyJson("leaders", getAthletesJson(this.displayOrder, null, fop));
					this.getElement().setProperty("leaderLines", this.displayOrder.size() + 2); // spacer + title
				} else {
					// nothing to show
					this.getElement().setPropertyJson("leaders", Json.createNull());
					this.getElement().setProperty("leaderLines", 1); // must be > 0
				}
			}
		});
	}

	protected void computeRecords(boolean done) {
		// always compute
		// if (!this.isRecordsDisplay()) {
		// this.getElement().setPropertyJson("records", Json.createNull());
		// return;
		// }
		OwlcmsSession.withFop(fop -> {
			Athlete curAthlete = fop.getCurAthlete();
			if (curAthlete != null && curAthlete.getGender() != null) {
				if (!done && showCurrent(fop)) {
					this.getElement().setPropertyJson("records", fop.getRecordsJson());
				} else {
					// nothing to show
					this.getElement().setPropertyJson("records", Json.createNull());
				}
			}
		});
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	protected int countBWClasses(List<Athlete> displayOrder) {
		int nbCats = 0;
		String prevCat = null;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			String curCat = a.getBWCategory();
			if (curCat != null && (prevCat == null || !prevCat.contentEquals(curCat))) {
				// changing categories, put marker before athlete
				prevCat = curCat;
				nbCats++;
			}
		}
		return nbCats;
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	protected int countCategories(List<Athlete> displayOrder) {
		int nbCats = 0;
		Category prevCat = null;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			Category curCat = a.getCategory();
			if (curCat != null && !curCat.sameAs(prevCat)) {
				// changing categories, put marker before athlete
				prevCat = curCat;
				nbCats++;
			}
		}
		return nbCats;
	}

	protected int countSubsets(List<Athlete> order) {
		if (Competition.getCurrent().isByAgeGroup()) {
			return countCategories(order) + 1;
		} else {
			return (countBWClasses(order)) + 1;
		}
	}

	protected void doEmpty() {
		this.setDisplay();
	}

	protected void doUpdate(Athlete a, UIEvent e) {
		// logger.trace("doUpdate {} {} {}", e != null ? e.getClass().getSimpleName() :
		// "no event", a, a != null ?
		// a.getAttemptsDone() : null);
		boolean leaveTopAlone = false;
		if (e instanceof UIEvent.LiftingOrderUpdated) {
			LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
			if (e2.isInBreak()) {
				leaveTopAlone = !e2.isDisplayToggle();
				this.getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
				doBreak(e);
			} else {
				leaveTopAlone = !e2.isCurrentDisplayAffected();
			}
		}

		FieldOfPlay fop = OwlcmsSession.getFop();
		if (!leaveTopAlone) {
			if (a != null) {
				Group group = fop.getGroup();
				if (group != null && !group.isDone()) {
					if (isAbbreviatedName()) {
						this.getElement().setProperty("fullName",
						        a.getAbbreviatedName() != null ? a.getAbbreviatedName() : "");
					} else {
						this.getElement().setProperty("fullName", a.getFullName() != null ? a.getFullName() : "");
					}
					this.getElement().setProperty("teamName", a.getTeam());
					this.getElement().setProperty("startNumber", a.getStartNumber());
					String formattedAttempt = formatAttempt(a.getAttemptsDone());
					this.getElement().setProperty("attempt", formattedAttempt);
					this.getElement().setProperty("weight", a.getNextAttemptRequestedWeight());
				} else {
					// logger.debug("group done {} {}", group, System.identityHashCode(group));
					doBreak(e);
				}
			}
		}
		updateDisplay(computeLiftType(fop.getCurAthlete()), fop);
	}

	protected String formatInt(Integer total) {
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

	protected String formatRank(Integer total) {
		if (total == null || total == 0) {
			return "&nbsp;";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else {
			return total.toString();
		}
	}

	protected JsonArray getAgeGroupNamesJson(LinkedHashMap<String, Participation> currentAthleteParticipations) {
		JsonArray ageGroups = Json.createArray();
		return ageGroups;
	}


	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		String category;
		category = curCat != null ? curCat.getDisplayName() : "";
		String fullName;
		if (isAbbreviatedName()) {
			fullName = a.getAbbreviatedName() != null ? a.getAbbreviatedName() : "";
		} else {
			fullName = a.getFullName() != null ? a.getFullName() : "";
		}
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank, fop);
		ja.put("sattempts", getSattempts());
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", getCattempts());
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));
		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			ja.put("snatchRank", formatRank(mainRankings.getSnatchRank()));
			ja.put("cleanJerkRank", formatRank(mainRankings.getCleanJerkRank()));
			ja.put("totalRank", formatRank(mainRankings.getTotalRank()));
		} else {
			this.logger.error("main rankings null for {}", a);
		}
		ja.put("group", a.getGroup().getName());
		ja.put("subCategory", a.getSubCategory());

		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");

		ja.put("sinclair", computedScore(a));
		ja.put("sinclairRank", computedScoreRank(a));

		boolean notDone = a.getAttemptsDone() < 6;
		String blink = (notDone ? " blink" : "");
		String highlight = "";
		if (fop.getState() != FOPState.DECISION_VISIBLE && notDone && showCurrent(fop)) {
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
		Athlete previousAthlete = fop.getPreviousAthlete();
		// we use the start number because athlete equality is tricky due to participations.
		if (isJury() && previousAthlete != null && a.getStartNumber().equals(previousAthlete.getStartNumber())) {
			highlight = highlight + " previous";
			// add marker by using a unicode character defined in the translation file
			fullName = Translator.translate("PreviousAthleteOnJuryScoreboard", fullName);
		}
		if (!a.isEligibleForIndividualRanking()) {
			highlight = highlight + " outOfCompetition";
		}
		ja.put("fullName", fullName);

		// logger.debug("{} {} {}", a.getShortName(), fop.getState(), highlight);
		ja.put("classname", highlight);

		setTeamFlag(a, ja);
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	protected JsonValue getAthletesJson(List<Athlete> displayOrder, List<Athlete> liftOrder, FieldOfPlay fop) {
		JsonArray jath = Json.createArray();
		int athx = 0;

		Athlete prevAthlete = null;
		long currentId = (liftOrder != null && liftOrder.size() > 0) ? liftOrder.get(0).getId() : -1L;
		long nextId = (liftOrder != null && liftOrder.size() > 1) ? liftOrder.get(1).getId() : -1L;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			JsonObject ja = Json.createObject();
			if (getSeparatorPredicate().test(a, prevAthlete)) {
				// changing categories, put marker before athlete
				ja.put("isSpacer", true);
				jath.set(athx, ja);
				ja = Json.createObject();
				athx++;
			}
			// compute the blinking rank (1 = current, 2 = next)
			getAthleteJson(a, ja, a.getCategory(), (a.getId() == currentId)
			        ? 1
			        : ((a.getId() == nextId)
			                ? 2
			                : 0),
			        fop);
			String team = a.getTeam();
			if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
				setWideTeamNames(true);
			}
			jath.set(athx, ja);
			athx++;
			prevAthlete = a;
		}
		return jath;
	}

	/**
	 * Compute Json string ready to be used by web component template
	 *
	 * CSS classes are pre-computed and passed along with the values; weights are formatted.
	 *
	 * @param a
	 * @param fop
	 * @return json string with nested attempts values
	 */
	protected void getAttemptsJson(Athlete a, int liftOrderRank, FieldOfPlay fop) {
		setSattempts(Json.createArray());
		setCattempts(Json.createArray());
		XAthlete x = new XAthlete(a);
		Integer curLift = x.getAttemptsDone();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;
			String blink = (notDone ? " blink" : "");

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
							// don't blink while decision is visible. wait until lifting displayOrder has
							// been
							// recomputed and we get DECISION_RESET
							int liftBeingDisplayed = i.getLiftNo();
							if (liftBeingDisplayed == curLift && (fop.getState() != FOPState.DECISION_VISIBLE)
							        && showCurrent(fop)) {
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
							Athlete previousAthlete = fop.getPreviousAthlete();
							if (isJury() && previousAthlete != null && a.getShortName().equals(previousAthlete.getShortName())) {
								highlight = highlight + " previous";
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
				getSattempts().set(ix, jri);
			} else {
				getCattempts().set(ix % 3, jri);
			}
			ix++;
		}
	}

	protected String getDisplayType() {
		return "";
	}

	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getDisplayOrder();
	}

	/**
	 * @return the separator
	 */
	protected BiPredicate<Athlete, Athlete> getSeparatorPredicate() {
		boolean displayByAgeGroup = Competition.getCurrent().isByAgeGroup();
		BiPredicate<Athlete, Athlete> separator = (cur, prev) -> {
			if (prev == null) {
				return true;
			} else if (displayByAgeGroup) {
				return (cur.getCategory() != null
				        && !cur.getCategory().sameAs(prev.getCategory()));
			} else {
				int compare = AbstractLifterComparator.compareBWCategory(cur, prev);
				return compare != 0;
			}
		};
		return separator;
	}

	/**
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			// Page page = UI.getCurrent().getPage();
			// page.retrieveExtendedClientDetails(details -> {
			// logger.debug("{} device resolution : {}x{}",
			// details.isIPad()?"iPad":(details.isIOS()?"iPhone" :
			// details.toString()), details.getScreenWidth(), details.getScreenHeight());
			// });
			resultsInit();
			checkVideo(this);
			this.teamFlags = URLUtils.checkFlags();

			// get the global category rankings (attached to each athlete)
			this.displayOrder = getOrder(fop);

			this.liftsDone = AthleteSorter.countLiftsDone(this.displayOrder);
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
			// we listen on uiEventBus.
			this.uiEventBus = uiEventBusRegister(this, fop);

			this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
		});

		getElement().setProperty("showTotal", true);
		getElement().setProperty("showBest", true); // overridden by media queries, not a variable
		getElement().setProperty("showLiftRanks",
		        Competition.getCurrent().isSnatchCJTotalMedals() && !Competition.getCurrent().isSinclair());
		getElement().setProperty("showTotalRank", !Competition.getCurrent().isSinclair());
		getElement().setProperty("showSinclair",
		        Competition.getCurrent().isSinclair() || Competition.getCurrent().isDisplayScores());
		getElement().setProperty("showSinclairRank",
		        Competition.getCurrent().isSinclair() || Competition.getCurrent().isDisplayScoreRanks());

		if (!isSilenced() || !isDownSilenced()) {
			SoundUtils.enableAudioContextNotification(this.getElement());
		}
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

		String scoringTitle = Ranking.getScoringTitle(Competition.getCurrent().getScoringSystem());
		translations.put("ScoringTitle", scoringTitle != null ? scoringTitle : Translator.translate("Sinclair"));
		this.getElement().setPropertyJson("t", translations);
	}

	protected void uiLog(UIEvent e) {
		if (this.uiEventLogger.isDebugEnabled()) {
			this.uiEventLogger.debug("### {} {} {} {}",
			        this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getOrigin(),
			        LoggerUtils.whereFrom());
		}
	}

	protected void updateDisplay(String liftType, FieldOfPlay fop) {
		this.curGroup = fop.getGroup();
		this.displayOrder = getOrder(fop);
		spotlightRecords(fop);
		if (liftType != null && this.curGroup != null && !this.curGroup.isDone()) {
			setDisplayTypeProperty(getDisplayType());
		}

		updateGroupInfo(liftType);
		this.getElement().setPropertyJson("ageGroups", getAgeGroupNamesJson(fop.getAgeGroupMap()));
		this.getElement().setPropertyJson("athletes",
		        getAthletesJson(this.displayOrder, fop.getLiftingOrder(), fop));

		List<Athlete> order = getOrder(OwlcmsSession.getFop());
		int resultLines = (order != null ? order.size() : 0) + countSubsets(order);
		boolean done = fop.getState() == FOPState.BREAK && fop.getBreakType() == BreakType.GROUP_DONE;

		if (!isLeadersDisplay() || done) {
			this.logger.debug("0px: isLeaders = {} done = {}", isLeadersDisplay(), done);
			this.getElement().setProperty("leaderFillerHeight", "--leaderFillerHeight: 0px");
		} else {
			this.logger.debug("default: isLeaders = {} done = {}", isLeadersDisplay(), done);
			this.getElement().setProperty("leaderFillerHeight",
			        "--leaderFillerHeight: var(--defaultLeaderFillerHeight)");
		}
		this.getElement().setProperty("resultLines", resultLines);

		computeLeaders(done);
		computeRecords(done);
	}

	private String computedScore(Athlete a) {
		Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
		double value = Ranking.getRankingValue(a, scoringSystem);
		String score = value > 0.001 ? String.format("%.3f", value) : "-";
		return score;
	}

	private String computedScoreRank(Athlete a) {
		Integer value = Ranking.getRanking(a, Competition.getCurrent().getScoringSystem());
		return value != null && value > 0 ? "" + value : "-";
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private String computeLiftType(Integer curAttempt) {
		if (curAttempt == null || curAttempt > 6) {
			return null;
		}
		String liftType = curAttempt >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private void doDone(Group g) {
		this.logger.debug("doDone {}", g == null ? null : g.getName());
		if (g == null) {
			doEmpty();
		} else {
			OwlcmsSession.withFop(fop -> {
				this.getElement().setProperty("fullName", Translator.translate("Group_number_results", g.toString()));
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

	private void resultsInit() {
		OwlcmsSession.withFop(fop -> {
			this.logger.trace("{}Starting result board on FOP {}", FieldOfPlay.getLoggingName(fop));
			setId("scoreboard-" + fop.getName());
			this.curGroup = fop.getGroup();
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		});
		setTranslationMap();
		this.displayOrder = ImmutableList.of();
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

	private void setDisplayTypeProperty(String displayType) {
		this.getElement().setProperty("displayType", displayType);
	}

	private void setGroupDescriptionProperty(String groupDescription) {
		this.getElement().setProperty("groupDescription", groupDescription);
	}

	private void setGroupNameProperty(String value) {
		// logger.debug("setGroupNameProperty {} from {}",value,
		// LoggerUtils.whereFrom());
		this.getElement().setProperty("groupInfo", value);
	}

	private void setLiftsDoneProperty(String value) {
		this.getElement().setProperty("liftsDone", value);
	}

	private void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
	}

	private boolean showCurrent(FieldOfPlay fop) {
		if (isPublicDisplay() && fop.getState() == FOPState.BREAK && fop.getCeremonyType() != null) {
			return false;
		}
		return true;
	}

	private void spotlightNewRecord() {
		this.getElement().setProperty("recordKind", "new");
		this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.NewRecord"));
	}

	private void spotlightRecordAttempt() {
		this.getElement().setProperty("recordKind", "attempt");
		this.getElement().setProperty("recordMessage",
		        Translator.translate("Scoreboard.RecordAttempt"));
	}

	private void spotlightRecords(FieldOfPlay fop) {
		if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
			spotlightNewRecord();
		} else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
			spotlightRecordAttempt();
		} else {
			this.getElement().setProperty("recordKind", "none");
		}
	}

	private void syncWithFOP() {
		OwlcmsSession.withFop(fop -> {
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
		});
	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
		switch (OwlcmsSession.getFop().getState()) {
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

	private void updateGroupInfo(String liftType) {
		String groupDescription = this.curGroup != null ? this.curGroup.getDescription() : null;
		if (this.curGroup != null && this.curGroup.isDone()) {
			setGroupNameProperty(groupDescription != null ? groupDescription : "\u00a0");
			setLiftsDoneProperty("");
		} else if (this.curGroup != null && liftType != null) {
			String name = groupDescription != null ? groupDescription : this.curGroup.getName();
			String value = groupDescription == null ? Translator.translate("Scoreboard.GroupLiftType", name, liftType)
			        : Translator.translate("Scoreboard.DescriptionLiftTypeFormat", groupDescription, liftType);
			setGroupNameProperty(value);
			this.liftsDone = AthleteSorter.countLiftsDone(this.displayOrder);
			if ((isPublicDisplay() || isVideo())) {
				setLiftsDoneProperty("");
			} else {
				setLiftsDoneProperty(" \u2013 " + Translator.translate("Scoreboard.AttemptsDone", this.liftsDone));
			}
		} else {
			if ((isPublicDisplay() || isVideo()) && groupDescription != null) {
				setLiftsDoneProperty(groupDescription);
				setGroupDescriptionProperty("");
			}
			setGroupNameProperty("");
		}
	}

	public boolean isJury() {
		return false;
	}

}