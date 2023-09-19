/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.displays.video.VideoCSSOverride;
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
        RequireDisplayLogin, HasBoardMode, VideoCSSOverride {
	
	@Id("timer")
	private AthleteTimerElement timer; // WebComponent, injected by Vaadin
	@Id("breakTimer")
	private BreakTimerElement breakTimer; // WebComponent, injected by Vaadin
	@Id("decisions")
	private DecisionElement decisions; // WebComponent, injected by Vaadin

	private final Logger logger = (Logger) LoggerFactory.getLogger(Results.class);
	private final Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
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
	private final DecimalFormat df = new DecimalFormat("0.000");
	private boolean video;
	private boolean downSilenced;

	public Results() {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	@Override
	public void doBreak(UIEvent event) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
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
			updateBottom(computeLiftType(a), fop);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
//		ceremonyGroup = e.getCeremonyGroup();
//		ceremonyCategory = e.getCeremonyCategory();
//		// logger.debug("------ ceremony event = {} {}", e, e.getTrace());
//		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
//			if (e.getCeremonyType() == CeremonyType.MEDALS && isPublicDisplay() && ceremonyGroup != null) {
//				Map<String, String> map = new HashMap<>(Map.of(
//				        FOPParameters.FOP, fop.getName(),
//				        FOPParameters.GROUP, ceremonyGroup.getName(),
//				        DisplayParameters.DARK, Boolean.toString(darkMode)));
//				if (ceremonyCategory != null) {
//					map.put(DisplayParameters.CATEGORY, ceremonyCategory.getCode());
//				} else {
//					// logger.trace("no ceremonyCategory");
//				}
//				QueryParameters simple = QueryParameters.simple(map);
//				// logger.debug("========== parameters {}",simple);
//				UI.getCurrent().navigate("displays/resultsMedals", simple);
//			} else {
//				// logger.debug("========== NOT {} {} {}",e.getCeremonyType(),
//				// this.isSwitchableDisplay(), ceremonyGroup);
//				String title = inferGroupName() + " &ndash; "
//				        + inferMessage(fop.getBreakType(), fop.getCeremonyType(), isPublicDisplay());
//				this.getElement().setProperty("fullName", title);
//				this.getElement().setProperty("teamName", "");
//				setGroupNameProperty("");
//				getBreakTimer().setVisible(!fop.getBreakTimer().isIndefinite());
//				setDisplay();
//
//				updateBottom(computeLiftType(fop.getCurAthlete()), fop);
//			}
//		}));
	}

	public BreakTimerElement getBreakTimer() {
		return breakTimer;
	}

	public JsonArray getCattempts() {
		return cattempts;
	}

	public DecisionElement getDecisions() {
		return decisions;
	}

	@Override
	public final Double getEmFontSize() {
		return emFontSize;
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
		return location;
	}

	final public UI getLocationUI() {
		return locationUI;
	}

	@Override
	public final String getRouteParameter() {
		return this.routeParameter;
	}

	final public JsonArray getSattempts() {
		return sattempts;
	}

	@Override
	public final Double getTeamWidth() {
		return teamWidth;
	}

	public AthleteTimerElement getTimer() {
		return timer;
	}

	final public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public final boolean isAbbreviatedName() {
		return abbreviatedName;
	}

	@Override
	public final boolean isDarkMode() {
		return this.darkMode;
	}

	@Override
	public final boolean isDownSilenced() {
		return downSilenced;
	}

	@Override
	public final boolean isLeadersDisplay() {
		return leadersDisplay;
	}

	@Override
	public final boolean isPublicDisplay() {
		return publicDisplay;
	}

	@Override
	public final boolean isRecordsDisplay() {
		return recordsDisplay;
	}

	public boolean isShowInitialDialog() {
		return false;
	}

	@Override
	public final boolean isSilenced() {
		return silenced;
	}

	@Override
	public final boolean isVideo() {
		return video;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#pushEmSize()
	 */
	@Override
	public void pushEmSize() {
		String formattedEm = null;
		if (emFontSize != null) {
			formattedEm = df.format(emFontSize);
			this.getElement().setProperty("sizeOverride", " --tableFontSize:" + formattedEm + "rem;");
			// logger.trace("%%%%% board changing em size={} from {}",emFontSize,LoggerUtils.whereFrom());
		}
	}

	@Override
	public void pushTeamWidth() {
		String formattedTW = null;
		if (teamWidth != null) {
			formattedTW = df.format(teamWidth);
			this.getElement().setProperty("twOverride", "--nameWidth: 1fr; --clubWidth:" + formattedTW + "em;");
		}
	}

	/**
	 * Reset.
	 */
	public void reset() {
		displayOrder = ImmutableList.of();
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
		getElement().setProperty("darkMode", darkMode ? DisplayParameters.DARK : DisplayParameters.LIGHT);
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
	}

	public void setSattempts(JsonArray sattempts) {
		this.sattempts = sattempts;
	}

	@Override
	public void setSilenced(boolean silent) {
		this.silenced = silent;
		this.getTimer().setSilenced(silenced);
		this.getBreakTimer().setSilenced(silenced);
	}

	/**
	 * @param a
	 * @param ja
	 */
	public void setTeamFlag(Athlete a, JsonObject ja) {
		String team = a.getTeam();
		String teamFileName = URLUtils.sanitizeFilename(team);
		String prop = null;
		if (teamFlags && !team.isBlank()) {
			prop = URLUtils.getImgTag("flags/", teamFileName, ".svg");
			if (prop == null) {
				prop = URLUtils.getImgTag("flags/", teamFileName, ".png");
				if (prop == null) {
					prop = URLUtils.getImgTag("flags/", teamFileName, ".jpg");
				}
			}
		}
		ja.put("teamLength", team.isBlank() ? "" : (team.length() + 2) + "ch");
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

	@Override
	public void setVideo(boolean b) {
		this.video = b;
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			Athlete a = e.getAthlete();
			setDisplay();
			if (a == null) {
				displayOrder = fop.getLiftingOrder();
				a = displayOrder.size() > 0 ? displayOrder.get(0) : null;
				liftsDone = AthleteSorter.countLiftsDone(displayOrder);
				doUpdate(a, e);
			} else {
				liftsDone = AthleteSorter.countLiftsDone(displayOrder);
				doUpdate(a, e);
			}
		}));
	}

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

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.trace"------- slaveCeremonyStarted {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", true);
			Athlete a = e.getAthlete();
			updateBottom(computeLiftType(a), OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", false);
			Athlete a = e.getAthlete();
			updateBottom(computeLiftType(a), OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			this.getElement().setProperty("decisionVisible", true);
			setDisplay();
		});
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			doDone(e.getGroup());
		});
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			if (e.getNewRecord()) {
				spotlightNewRecord();
			}
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			displayOrder = getOrder(OwlcmsSession.getFop());
			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			doUpdate(a, e);
		});
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", false);
			this.getElement().setProperty("recordName", "");
			syncWithFOP();
		});
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP();
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(e);
		});
	}

	protected void computeLeaders(boolean done) {
		OwlcmsSession.withFop(fop -> {
			Athlete curAthlete = fop.getCurAthlete();
			if (curAthlete != null && curAthlete.getGender() != null) {
				this.getElement().setProperty("categoryName", curAthlete.getCategory().getTranslatedName());

				if (Competition.getCurrent().isSinclair()) {
					List<Athlete> sortedAthletes = new ArrayList<>(
					        Competition.getCurrent().getGlobalSinclairRanking(curAthlete.getGender()));
					displayOrder = AthleteSorter.topSinclair(sortedAthletes, 3).topAthletes;
					this.getElement().setProperty("categoryName", Translator.translate("sinclair"));
				} else {
					displayOrder = fop.getLeaders();
				}
				if ((!done || Competition.getCurrent().isSinclair()) && displayOrder != null
				        && displayOrder.size() > 0) {
					// null as second argument because we do not highlight current athletes in the
					// leaderboard
					this.getElement().setPropertyJson("leaders", getAthletesJson(displayOrder, null, fop));
					this.getElement().setProperty("leaderLines", displayOrder.size() + 2); // spacer + title
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
//        if (!this.isRecordsDisplay()) {
//            this.getElement().setPropertyJson("records", Json.createNull());
//            return;
//        }
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
		return countCategories(order) + 1;
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
		// logger.debug("updating bottom");
		updateBottom(computeLiftType(a), fop);
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
		category = curCat != null ? curCat.getTranslatedName() : "";
		if (isAbbreviatedName()) {
			ja.put("fullName", a.getAbbreviatedName() != null ? a.getAbbreviatedName() : "");
		} else {
			ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
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
			logger.error("main rankings null for {}", a);
		}
		ja.put("group", a.getSubCategory());
		Double double1 = a.getAttemptsDone() <= 3 ? a.getSinclairForDelta()
		        : a.getSinclair();
		ja.put("sinclair", double1 > 0.001 ? String.format("%.3f", double1) : "-");
		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");
		ja.put("sinclairRank", a.getSinclairRank() != null && a.getSinclairRank() > 0 ? "" + a.getSinclairRank() : "-");

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

			jri.put("goodBadClassName", "narrow empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
				case ACTUAL:
					if (!trim.isEmpty()) {
						if (trim.contentEquals("-") || trim.contentEquals("0")) {
							jri.put("goodBadClassName", "narrow fail");
							jri.put("stringValue", "-");
						} else {
							boolean failed = stringValue != null && stringValue.startsWith("-");
							jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
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
						jri.put("goodBadClassName", "narrow request");
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
		BiPredicate<Athlete, Athlete> separator = (cur, prev) -> prev == null
		        || (cur.getCategory() != null
		                && !cur.getCategory().sameAs(prev.getCategory()));
		return separator;
	}


	/**
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();
			checkVideo(Config.getCurrent().getParamStylesDir() + "/video/attemptboard.css", getRouteParameter(),
			        this);
			teamFlags = URLUtils.checkFlags();

			// get the global category rankings (attached to each athlete)
			displayOrder = getOrder(fop);

			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this));
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});

		getElement().setProperty("showSinclair", Competition.getCurrent().isSinclair());
		getElement().setProperty("showLiftRanks",
		        Competition.getCurrent().isSnatchCJTotalMedals() && !Competition.getCurrent().isSinclair());
		SoundUtils.enableAudioContextNotification(this.getElement());
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

	protected void uiLog(UIEvent e) {
		if (uiEventLogger.isDebugEnabled()) {
			uiEventLogger.debug("### {} {} {} {}",
			        this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getOrigin(),
			        LoggerUtils.whereFrom());
		}
	}

	protected void updateBottom(String liftType, FieldOfPlay fop) {
		curGroup = fop.getGroup();
		String groupDescription = curGroup != null ? curGroup.getDescription() : null;
		displayOrder = getOrder(fop);
		spotlightRecords(fop);
		if (liftType != null && curGroup != null && !curGroup.isDone()) {
			this.getElement().setProperty("displayType", getDisplayType());
		}
		if (curGroup != null && curGroup.isDone()) {
			// logger.debug("case 2 {}", isSwitchableDisplay());
			setGroupNameProperty(groupDescription != null ? groupDescription : "\u00a0");
			setLiftsDoneProperty("");
		} else if (curGroup != null && liftType != null) {
			// logger.debug("case 3 {}", isSwitchableDisplay());
			String name = groupDescription != null ? groupDescription : curGroup.getName();
			String value = groupDescription == null ? Translator.translate("Scoreboard.GroupLiftType", name, liftType)
			        : Translator.translate("Scoreboard.DescriptionLiftTypeFormat", groupDescription, liftType);
			setGroupNameProperty(value);
			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			if ((isPublicDisplay() || isVideo())) {
				setLiftsDoneProperty("");
			} else {
				setLiftsDoneProperty(" \u2013 " + Translator.translate("Scoreboard.AttemptsDone", liftsDone));
			}
		} else {
			// logger.debug("case 4 {}", isSwitchableDisplay());
			if ((isPublicDisplay() || isVideo()) && groupDescription != null) {
				setLiftsDoneProperty(groupDescription);
				setGroupDescriptionProperty("");
			}
			setGroupNameProperty("");
		}
		this.getElement().setPropertyJson("ageGroups", getAgeGroupNamesJson(fop.getAgeGroupMap()));
		this.getElement().setPropertyJson("athletes",
		        getAthletesJson(displayOrder, fop.getLiftingOrder(), fop));

		List<Athlete> order = getOrder(OwlcmsSession.getFop());
		int resultLines = (order != null ? order.size() : 0) + countSubsets(order);
		this.getElement().setProperty("resultLines", resultLines);
		boolean done = fop.getState() == FOPState.BREAK && fop.getBreakType() == BreakType.GROUP_DONE;
		computeLeaders(done);
		computeRecords(done);
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
				this.getElement().setProperty("fullName", getTranslation("Group_number_results", g.toString()));
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

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting result board on FOP {}", fop.getLoggingName());
			setId("scoreboard-" + fop.getName());
			curGroup = fop.getGroup();
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		});
		setTranslationMap();
		displayOrder = ImmutableList.of();
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

	private void setGroupDescriptionProperty(String groupDescription) {
		this.getElement().setProperty("groupDescription", groupDescription);
	}

	private void setGroupNameProperty(String value) {
		this.getElement().setProperty("groupName", value);
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

}