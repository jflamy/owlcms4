/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

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
import com.vaadin.flow.router.Location;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AbstractLifterComparator;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.HasBoardMode;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Class Results
 *
 * Show results scoreboard for a session, including records and leaders
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("records-template")
@JsModule("./components/Records.js")

public class Records extends LitTemplate
        implements SafeEventBusRegistration, UIEventProcessor,
        RequireDisplayLogin, HasBoardMode {

	protected Group curGroup;
	protected List<Athlete> displayOrder;
	protected EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private FieldOfPlay fop;
	private Group group;
	private Location location;
	private UI locationUI;
	protected final Logger logger = (Logger) LoggerFactory.getLogger(Records.class);

	private final Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

	public Records() {
		this.uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
		this.getElement().setProperty("scoreboardType", this.getClass().getSimpleName());
	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	public void doBreak(UIEvent event) {
		// this.logger.debug("Results doBreak {}", LoggerUtils.stackTrace());
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), this.getElement());

			Athlete a = fop.getCurAthlete();
			setDisplay();
			updateDisplay(computeLiftType(a), fop);
		}));
	}

	public void doCeremony(UIEvent.CeremonyStarted e) {
	}

	final public FieldOfPlay getFop() {
		return this.fop;
	}

	final public Group getGroup() {
		return this.group;
	}

	final public Location getLocation() {
		return this.location;
	}

	final public UI getLocationUI() {
		return this.locationUI;
	}

	final public Map<String, List<String>> getUrlParameterMap() {
		return this.urlParameterMap;
	}

	public boolean isJury() {
		return false;
	}

	public boolean isShowInitialDialog() {
		return false;
	}

	/**
	 * Reset.
	 */
	public void reset() {
		this.displayOrder = ImmutableList.of();
	}

	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	final public void setGroup(Group group) {
		this.group = group;
	}

	final public void setLocation(Location location) {
		this.location = location;

	}

	final public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	final public void setUrlParameterMap(Map<String, List<String>> parametersMap) {
		this.urlParameterMap = parametersMap;
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
				doUpdate(a, e);
			} else {
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
				score = value > 0.001 ? String.format("%.0f", value) : "-";
			} else {
				score = value > 0.001 ? String.format("%.3f", value) : "+";
			}
			return score;
		} else {
			double value = Ranking.getRankingValue(a, scoringSystem);
			String score = value > 0.001 ? String.format("%.3f", value) : "*";
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

		FieldOfPlay fop = e.getFop();
		if (!leaveTopAlone) {
			if (a != null) {
				Group group = fop.getGroup();
				if (group != null && !group.isDone()) {
				} else {
					// logger.debug("group done {} {}", group, System.identityHashCode(group));
					doBreak(e);
				}
			}
		}
		updateDisplay(computeLiftType(fop.getCurAthlete()), fop);
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
			} else if (displayByAgeGroup || isAllBWCategory(cur)) {
				// score-based all-bodyweight categories need separator in spite of same bounds
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
		});
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
				boolean any = athletes.stream().map(a -> a.getAgeGroup().getScoringSystem())
				        .anyMatch(s -> s != null && s != Ranking.TOTAL);
				scoring[0] = any;
			}
		});
		setTranslationMap();
		
		boolean showScore = scoring[0] || Competition.getCurrent().isDisplayScores() || Competition.getCurrent().isSinclair();
		this.getElement().setProperty("showSinclair", showScore);
		
		boolean showScoreRank = scoring[0] || Competition.getCurrent().isDisplayScoreRanks() || Competition.getCurrent().isSinclair();
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
		this.curGroup = fop.getGroup();
		this.displayOrder = getOrder(fop);
		spotlightRecords(fop);
		if (liftType != null && this.curGroup != null && !this.curGroup.isDone()) {
			setDisplayTypeProperty(getDisplayType());
		}
		boolean done = fop.getState() == FOPState.BREAK && fop.getBreakType() == BreakType.GROUP_DONE;
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
		}
		doEmpty();
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

	private boolean showCurrent(FieldOfPlay fop) {
		if (fop.getState() == FOPState.BREAK && fop.getCeremonyType() != null) {
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
}