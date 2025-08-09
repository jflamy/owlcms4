/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.io.FileNotFoundException;
import java.nio.file.Path;
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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Location;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.ResultsParameters;
import app.owlcms.data.agegroup.AgeGroup;
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
import app.owlcms.data.config.Config;
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
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class BaseResults
 *
 * Show results scoreboard for a session, including records and leaders
 *
 */
@SuppressWarnings({ "serial", "deprecation" })

public class BaseResults extends LitTemplate
        implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay,
        RequireDisplayLogin, HasBoardMode, StylesDirSelection {

	protected Group curGroup;
	protected List<Athlete> displayOrder;
	protected EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private boolean abbreviatedName;
	private HashMap<Athlete, String> athleteToFlag = new HashMap<>();
	private JsonArray cattempts;
	private boolean darkMode = true;
	private boolean downSilenced;
	private Double emFontSize;
	private FieldOfPlay fop;
	private Group group;
	private boolean leadersDisplay;
	private int liftsDone;
	private Location location;
	private UI locationUI;
	protected final Logger logger = (Logger) LoggerFactory.getLogger(BaseResults.class);
	private boolean publicDisplay;
	private boolean recordsDisplay;
	private String routeParameter;
	private JsonArray sattempts;
	private boolean silenced;
	private boolean teamFlags;
	private Double teamWidth;
	private final Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	private boolean video;

	public BaseResults() {
		this.uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
		this.getElement().setProperty("scoreboardType", this.getClass().getSimpleName());

		overrideColors(this.getElement());
	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	@Override
	public void doBreak(UIEvent event) {
		// this.logger.debug("BaseResults doBreak {}", LoggerUtils.stackTrace());
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

	public JsonArray getCattempts() {
		return this.cattempts;
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

	public boolean isJury() {
		return false;
	}

	@Override
	public final boolean isLeadersDisplay() {
		return this.leadersDisplay;
	}

	@Override
	public boolean isPublicDisplay() {
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
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#pushEmSize(Element)
	 */
	@Override
	public void pushEmSize(Element element) {
		String formattedEm = null;
		if (this.emFontSize != null) {
			formattedEm = ResultsParameters.formatEN_US.format(this.emFontSize);
			// logger.debug("B pushing em {} {}\n{}",element.getTag(), emFontSize, LoggerUtils.stackTrace());
			element.setProperty("sizeOverride", " --tableFontSize:" + formattedEm + "em;");
		}
	}

	@Override
	public void pushTeamWidth(Element element) {
		String formattedTW = null;
		if (this.teamWidth != null) {
			formattedTW = ResultsParameters.formatEN_US.format(this.teamWidth);
			element.setProperty("twOverride", "--clubWidth:" + formattedTW + "em;");
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

	@Override
	public void setDownSilenced(boolean silent) {
		this.downSilenced = silent;
	}

	@Override
	public final void setEmFontSize(Double emFontSize) {
		logger.debug("%%%%% setEmFontSize {}", emFontSize);
		this.emFontSize = emFontSize;
		pushEmSize(this.getElement());
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	final public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setLeadersDisplay(boolean)
	 */
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
		pushTeamWidth(this.getElement());
	}

	final public void setUrlParameterMap(Map<String, List<String>> parametersMap) {
		this.urlParameterMap = parametersMap;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setVideo(boolean)
	 */
	@Override
	public void setVideo(boolean b) {
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
			updateDisplay(computeLiftType(a.getAttemptsDone() - 1), e.getFop());
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			this.getElement().setProperty("decisionVisible", false);
			Athlete a = e.getAthlete();
			updateDisplay(computeLiftType(a.getAttemptsDone() - 1), e.getFop());
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
			this.displayOrder = getOrder(e.getFop());
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

	protected String computedScore(Athlete a) {
		AgeGroup ageGroup = a.getAgeGroup();
		Ranking ageGroupScoringSystem = ageGroup != null ? ageGroup.getComputedScoringSystem() : null;

		Competition current = Competition.getCurrent();
		boolean sinclair = current.isSinclair();
		Competition current2 = Competition.getCurrent();
		boolean displayGlobal = current2.isDisplayScores();
		Competition current3 = Competition.getCurrent();
		Ranking scoringSystem = current3.getScoringSystem();

		if (ageGroupScoringSystem != null && !sinclair && !displayGlobal) {
			double value = Ranking.getRankingValue(a, Ranking.CATEGORY_SCORE);
			String score;
			if (ageGroupScoringSystem == Ranking.TOTAL) {
				score = value > 0.001 ? String.format("%.0f", value) : "\u2013";
			} else {
				score = value > 0.001 ? String.format("%.3f", value) : "\u2013";
			}
			return score;
		} else {
			double value = Ranking.getRankingValue(a, scoringSystem);
			String score = value > 0.001 ? String.format("%.3f", value) : "\u2013";
			return score;
		}
	}

	protected String computedScoreRank(Athlete a) {
		Ranking ageGroupScoringSystem = a.getAgeGroup().getComputedScoringSystem();

		Competition current = Competition.getCurrent();
		boolean sinclair = current.isSinclair();
		Competition current2 = Competition.getCurrent();
		boolean displayGlobal = current2.isDisplayScoreRanks();
		Competition current3 = Competition.getCurrent();
		Ranking bestLifterScoringSystem = current3.getScoringSystem();

		if (a.isEligibleForIndividualRanking()) {
			if (ageGroupScoringSystem != null && !sinclair && !displayGlobal) {
				Integer value = Ranking.getRanking(a, Ranking.CATEGORY_SCORE);
				return value != null && value > 0 ? "" + value : "-";
			} else {
				Integer value = Ranking.getRanking(a, bestLifterScoringSystem);
				return value != null && value > 0 ? "" + value : "-";
			}
		} else {
			return Translator.translate("Results.Extra/Invited");
		}

	}

	protected void computeLeaders(boolean done) {
		OwlcmsSession.withFop(fop -> {
			Athlete curAthlete = fop.getCurAthlete();
			if (curAthlete == null) {
				this.getElement().setPropertyJson("leaders", Json.createNull());
				setBottomSize(1);
				return;
			}
			if (curAthlete.getGender() != null) {
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
					setBottomSize(this.displayOrder.size() + 2); // spacer + title
				} else {
					// nothing to show
					this.getElement().setPropertyJson("leaders", Json.createNull());
					setBottomSize(1);
				}
			}
		});
	}

	/**
	 * For TV and Public scoreboards, the leaderboard is not pushed down to the bottom. The last line of the grid is not present because this leads to too much
	 * space
	 * 
	 * @param normal
	 */
	public void setBottomSize(int normal) {
		// we stretch video and TV ONLY when using the old standard nogrid or grid styles, or if the corresponding "stretch" feature toggle is present
		if (this.isPublicDisplay() || this.isVideo()) {
			// logger.debug("isPublicDisplay {} {} {} {}",
			// isPublicDisplay(),
			// Config.getCurrent().getParamPublicStylesDir().endsWith("grid"),
			// Config.getCurrent().featureSwitch("stretchPublic"),
			// (isPublicDisplay() && !(Config.getCurrent().getParamPublicStylesDir().endsWith("grid") || Config.getCurrent().featureSwitch("stretchPublic")))
			// );
			boolean noStretch = (isPublicDisplay()
			        && !(Config.getCurrent().getParamPublicStylesDir().endsWith("grid") || Config.getCurrent().featureSwitch("stretchPublic")))
			        || (isVideo() && !(Config.getCurrent().getParamVideoStylesDir().endsWith("grid") || Config.getCurrent().featureSwitch("stretchVideo")));
			// noStretch = no filler line to push the leaderboard down
			this.getElement().setProperty("leaderLines", normal - (noStretch ? 1 : 0));
		} else {
			// filler line to push the leaderboard down
			this.getElement().setProperty("leaderLines", normal);
		}
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
		String prevBWCat = null;
		Category prevCat = null;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			Category curCat = a.getCategory();
			String curBWCat = a.getBWCategory();
			if (isAllBWCategory(a)) {
				if (curCat != null && !curCat.sameAs(prevCat)) {
					// changing categories, put marker before athlete
					nbCats++;
				}
			} else {
				if (curBWCat != null && (prevBWCat == null || !prevBWCat.contentEquals(curBWCat))) {
					// changing categories, put marker before athlete
					nbCats++;
				}
			}
			prevBWCat = curBWCat;
			prevCat = curCat;
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
				nbCats++;
			}

			prevCat = curCat;
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

	boolean iwfLook = Config.getCurrent().featureSwitch("iwfLook");

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

		FieldOfPlay fop = e.getFop();
		if (!leaveTopAlone) {
			if (a != null) {
				Group group = fop != null ? fop.getGroup() : null;
				if (group != null && !group.isDone()) {
					if (isAbbreviatedName() || (a.getFullName().length() >= 45)) {
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
		if (fop != null) {
			updateDisplay(computeLiftType(fop.getCurAthlete()), fop);
		} else {
			updateDisplay(computeLiftType((Integer)null), fop);
		}
	}

	protected String formatInt(Integer value) {
		if (value == null || value == 0) {
			return "-";
		} else if (value < 0) {
			return "(" + Math.abs(value) + ")";
		} else {
			return value.toString();
		}
	}

	protected String formatRank(Integer total) {
		if (total == null || total == 0) {
			return "&nbsp;";
		} else if (total == -1) {
			// invited lifter, not eligible.
			return Translator.translate("Results.Extra/Invited");
		} else {
			return total.toString();
		}
	}

	protected JsonArray getAgeGroupNamesJson(LinkedHashMap<String, Participation> currentAthleteParticipations) {
		JsonArray ageGroups = Json.createArray();
		return ageGroups;
	}

	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		boolean bestScore = Config.getCurrent().featureSwitch("displayBestScore");
		boolean bestScoreRank = Config.getCurrent().featureSwitch("displayBestScoreRank");

		String category;
		category = curCat != null ? curCat.getDisplayName() : "";
		String fullName;
		if (isAbbreviatedName()) {
			fullName = a.getAbbreviatedName() != null ? a.getAbbreviatedName() : "";
		} else {
			fullName = a.getFullName() != null ? a.getFullName() : "";
		}
		if (!a.isEligibleForIndividualRanking() && !fullName.isBlank()) {
			fullName = Translator.translate("Scoreboard.Extra/Invited", fullName);
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
		ja.put("attemptNumber", formatInt(a.getAttemptsDone() + 1));
		ja.put("group", a.getGroup().getName());
		ja.put("subCategory", a.getSubCategory());

		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");

		if (a.getComputedScoringSystem() != Ranking.TOTAL || bestScore || bestScoreRank) {
			ja.put("sinclair", computedScore(a));
			if (bestScoreRank) {
				ja.put("sinclairRank", computedScoreRank(a));
			} else {
				ja.put("sinclairRank", a.getBestLifterRank());
			}
		}

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
		ja.put("entryTotal", formatInt(a.getEntryTotal()));

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
		boolean bwClassThenAgeGroup = Config.getCurrent().featureSwitch("bwClassThenAgeGroup");
		// logger.debug("displayByAgeGroup {} bwClassThenAgeGroup {}", displayByAgeGroup, bwClassThenAgeGroup);
		BiPredicate<Athlete, Athlete> separator = (cur, prev) -> {
			if (prev == null) {
				return true;
			} else if (displayByAgeGroup || isAllBWCategory(cur)) {
				// score-based all-bodyweight categories need separator in spite of same bounds
				return (cur.getCategory() != null
				        && !cur.getCategory().sameAs(prev.getCategory()));
			} else if (!displayByAgeGroup && !bwClassThenAgeGroup) {
				// no separator unless switch in body weight max
				return (cur.getCategory() != null
				        && cur.getCategory().getMaximumWeight() > (prev.getCategory().getMaximumWeight()));
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
			computeStylesDir(this);
			this.teamFlags = URLUtils.checkFlags();

			// get the global category rankings (attached to each athlete)
			this.displayOrder = getOrder(fop);

			this.liftsDone = AthleteSorter.countLiftsDone(this.displayOrder);
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this, fop));
			// we listen on uiEventBus.
			this.uiEventBus = uiEventBusRegister(this, fop);

			this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
			this.getElement().setProperty("logoSrc", getLogoSrc());

		});

		getElement().setProperty("showTotal", true);
		getElement().setProperty("showBest", true); // overridden by media queries, not a variable
		getElement().setProperty("showLiftRanks",
		        Competition.getCurrent().isSnatchCJTotalMedals() && !Competition.getCurrent().isSinclair());
		getElement().setProperty("showTotalRank", !Competition.getCurrent().isSinclair());

		if (!isSilenced() || !isDownSilenced()) {
			SoundUtils.enableAudioContextNotification(this.getElement());
		}
	}

	protected void resultsInit() {
		boolean scoring[] = { false };
		OwlcmsSession.withFop(fop -> {
			setId("scoreboard-" + fop.getName());
			this.curGroup = fop.getGroup();
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());

			List<Athlete> athletes = fop.getDisplayOrder();
			if (athletes != null && athletes.size() > 0) {
				boolean any = athletes.stream()
				        .map(a -> a.getAgeGroup())
				        .filter(ageGroup -> ageGroup != null)
				        .map(agegroup -> agegroup.getComputedScoringSystem())
				        .anyMatch(s -> s != Ranking.TOTAL);
				scoring[0] = any;
			}
		});
		setTranslationMap();

		boolean showScore = scoring[0] || Competition.getCurrent().isDisplayScores() || Competition.getCurrent().isSinclair();
		this.getElement().setProperty("showSinclair", showScore);

		boolean showScoreRank = scoring[0] || Competition.getCurrent().isDisplayScoreRanks() || Competition.getCurrent().isSinclair();
		if (Config.getCurrent().featureSwitch("noSinclairRank")) {
			showScoreRank = false;
		} else 	if (Config.getCurrent().featureSwitch("displayBestScoreRank")) {
			showScoreRank = true;
		}
		this.getElement().setProperty("showSinclairRank", showScoreRank);

		this.displayOrder = ImmutableList.of();
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
		translations.put("ScoringTitle", Translator.translate("Score"));
		if (!Config.getCurrent().featureSwitch("medalistsAsLeaders")) {
			translations.put("Leaders", Translator.translate("Leaders.PreviousGroups"));
		}
		this.getElement().setPropertyJson("t", translations);
	}

	protected void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
	}

	protected void uiLog(UIEvent e) {
		if (this.uiEventLogger.isDebugEnabled()) {
			this.uiEventLogger.debug("### {} {} {} {}",
			        this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getOrigin(),
			        LoggerUtils.whereFrom());
		}
	}

	protected void updateDisplay(String liftType, FieldOfPlay fop) {
		if (fop == null || liftType == null) {
			this.logger.error("updateDisplay: fop is null or liftType is null");
			doEmpty();
			return;
		}
		this.curGroup = fop.getGroup();
		this.displayOrder = getOrder(fop);
		spotlightRecords(fop);
		if (liftType != null && this.curGroup != null && !this.curGroup.isDone()) {
			setDisplayTypeProperty(getDisplayType());
		}

		updateGroupInfo(liftType);
		// getAgeGroupNamesJson must be called before getAthletesJson
		if (Config.getCurrent().featureSwitch("displayBestScore")) {
			this.getElement().setProperty("scoringName", Translator.translate("Scoreboard."+Competition.getCurrent().getScoringSystem().name()));
		} else {
			this.getElement().setProperty("scoringName", Translator.translate("Score"));
		}
		if (fop.getGroup() != null) {
			this.getElement().setPropertyJson("ageGroups", getAgeGroupNamesJson(fop.getAgeGroupMap()));
		}
		this.getElement().setPropertyJson("athletes",
		        getAthletesJson(this.displayOrder, fop.getLiftingOrder(), fop));

		List<Athlete> order = getOrder(fop);
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
		if (g == null) {
			doEmpty();
		} else {
			OwlcmsSession.withFop(fop -> {
				computeLeaders(true);
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

	private boolean isAllBWCategory(Athlete cur) {
		// score-based all-bodyweight categories need to be identified
		var cat = cur.getCategory();
		var min = cat.getMinimumWeight();
		var max = cat.getMaximumWeight();
		return (min < 10 && max > 900);
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
			syncWithFOP(new UIEvent.SwitchGroup(fop.getGroup(), fop.getState(), fop.getCurAthlete(), this, fop));
		});
	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
		var fop = e.getFop();
		setFop(fop);
		setGroup(fop != null ? fop.getGroup() : null);
		switch (e.getFop().getState()) {
			case INACTIVE:
				doEmpty();
				break;
			case BREAK:
				if (e.getGroup() == null) {
					doEmpty();
				} else {
					resultsInit();
					doUpdate(e.getAthlete(), e);
					doBreak(e);
				}
				break;
			default:
				resultsInit();
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

	protected String getLogoSrc() {
		try {
			Path loc = ResourceWalker.getFileOrResourcePath("logos/left.svg");
			return loc.toString();
		} catch (FileNotFoundException e) {
			try {
				Path loc = ResourceWalker.getFileOrResourcePath("logos/left.png");
				return loc.toString();
			} catch (FileNotFoundException e2) {
				return "../../../../local/logos/left.png";
			}
		}

	}
}