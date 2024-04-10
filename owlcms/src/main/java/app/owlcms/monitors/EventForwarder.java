/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.HasBoardMode;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.DecisionEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakDone;
import app.owlcms.uievents.UIEvent.BreakPaused;
import app.owlcms.uievents.UIEvent.BreakSetTime;
import app.owlcms.uievents.UIEvent.BreakStarted;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.uievents.UIEvent.SetTime;
import app.owlcms.uievents.UIEvent.StartTime;
import app.owlcms.uievents.UIEvent.StopTime;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class EventForwarder implements BreakDisplay, HasBoardMode, IUnregister {

	// private static HashMap<String, EventForwarder> registeredFop = new HashMap<>();

	private static final int KEEPALIVE_INTERVAL = 15000;
	private static final boolean NO_KEEPALIVE = true;
	final private static Logger logger = (Logger) LoggerFactory.getLogger(EventForwarder.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	public static final Object singleThreadLock = new Object();
	private String attempt;
	private String categoryName;
	private JsonArray cattempts;
	@SuppressWarnings("unused")
	private Boolean debugMode;
	private Boolean decisionLight1 = null;
	private Boolean decisionLight2 = null;
	private Boolean decisionLight3 = null;
	private boolean decisionLightsVisible = false;
	private boolean down = false;
	// private EventBus fopEventBus;
	private FieldOfPlay fop;
	private String fullName;
	private JsonValue groupAthletes;
	private JsonValue liftingOrderAthletes;
	private List<Athlete> groupLeaders;
	private String groupName;
	private boolean hidden;
	private JsonValue leaders;
	private String liftsDone;
	private EventBus postBus;
	private int previousHashCode = 0;
	private long previousMillis = 0L;
	private JsonArray sattempts;
	private Integer startNumber;
	private String teamName;
	private Integer timeAllowed;
	private JsonObject translationMap;
	private long translatorResetTimeStamp;
	private Integer weight;
	private boolean wideTeamNames;
	private String noLiftRanks;
	private JsonValue records;
	private Boolean teamFlags;
	private String boardMode;
	private String groupInfo;
	private Map<String, String> lastTimerMap;
	private Map<String, String> lastDecisionMap;
	private Map<String, String> lastUpdate;

	public EventForwarder(FieldOfPlay emittingFop) {
		this.setFop(emittingFop);
		// logger.debug("|||| eventForwarder {} {} {}", System.identityHashCode(this),
		// emittingFop.getName(),System.identityHashCode(emittingFop));

		this.postBus = getFop().getEventForwardingBus();
		this.postBus.register(this);

		this.translatorResetTimeStamp = 0L;

		String updateKey = Config.getCurrent().getParamUpdateKey();
		String updateUrl = Config.getCurrent().getParamUpdateUrl();
		if (updateUrl == null || updateKey == null || updateUrl.trim().isEmpty()
		        || updateKey.trim().isEmpty()) {
			logger.info("{}Pushing results to remote site not enabled.", FieldOfPlay.getLoggingName(getFop()));
		} else {
			logger.info("{}Pushing to remote site {}", FieldOfPlay.getLoggingName(getFop()), updateUrl);
		}
		if (emittingFop.getState() != null) {
			pushUpdate();
		}
	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	@Override
	public void doBreak(UIEvent e) {
		logger.warn("============= doBreak {} {}",e.getClass().getSimpleName(), this.fop.getBreakType());
		BreakType breakType = this.fop.getBreakType();
		Group group = this.fop.getGroup();
		if (breakType == null) {
			breakType = BreakType.BEFORE_INTRODUCTION;
		}
		switch (breakType) {
			case GROUP_DONE:
				setFullName(groupResults(group));
				break;
			default:
				setFullName((group != null ? (Translator.translate("Group_number", group.getName()) + " &ndash; ") : "")
				        + inferMessage(this.fop.getBreakType(), this.fop.getCeremonyType(), true));
				break;
		}
		setTeamName("");
		setAttempt("");
		setHidden(false);
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		BreakType breakType = this.fop.getBreakType();
		Group group = this.fop.getGroup();
		if (breakType == null) {
			breakType = BreakType.BEFORE_INTRODUCTION;
		}
		switch (breakType) {
			case GROUP_DONE:
				setFullName(groupResults(group));
				break;
			default:
				setFullName((group != null ? (Translator.translate("Group_number", group.getName()) + " &ndash; ") : "")
				        + inferMessage(this.fop.getBreakType(), this.fop.getCeremonyType(), true));
				break;
		}
		setTeamName("");
		setAttempt("");
		setHidden(false);
	}

	public String getBoardMode() {
		return this.boardMode;
	}

	public Boolean getDecisionLight1() {
		return this.decisionLight1;
	}

	public Boolean getDecisionLight2() {
		return this.decisionLight2;
	}

	public Boolean getDecisionLight3() {
		return this.decisionLight3;
	}

	public String getGroupInfo() {
		return this.groupInfo;
	}

	public String getGroupName() {
		return this.groupName;
	}

	public String getLiftsDone() {
		return this.liftsDone;
	}

	public JsonValue getRecords() {
		return this.records;
	}

	public Boolean getTeamFlags() {
		return this.teamFlags;
	}

	public Integer getTimeAllowed() {
		return this.timeAllowed;
	}

	public JsonObject getTranslationMap() {
		return this.translationMap;
	}

	/**
	 * Change the messages because we are not showing live timers
	 *
	 * @see app.owlcms.uievents.BreakDisplay#inferMessage(app.owlcms.uievents.BreakType)
	 */
	@Override
	// public String inferMessage(BreakType breakType, CeremonyType ceremonyType, boolean publicDisplay) {
	// if (breakType == null) {
	// return Translator.translate("PublicMsg.CompetitionPaused");
	// }
	// if (ceremonyType != null) {
	// switch (ceremonyType) {
	// case INTRODUCTION:
	// return Translator.translate("BreakMgmt.IntroductionOfAthletes");
	// case MEDALS:
	// return Translator.translate("PublicMsg.Medals");
	// case OFFICIALS_INTRODUCTION:
	// return Translator.translate("BreakMgmt.IntroductionOfOfficials");
	// }
	// }
	// switch (breakType) {
	// case FIRST_CJ:
	// return Translator.translate("BreakType.FIRST_CJ");
	// case FIRST_SNATCH:
	// return Translator.translate("BreakType.FIRST_SNATCH");
	// case BEFORE_INTRODUCTION:
	// return Translator.translate("BreakType.BEFORE_INTRODUCTION");
	// case TECHNICAL:
	// return Translator.translate("PublicMsg.CompetitionPaused");
	// case JURY:
	// return Translator.translate("PublicMsg.JuryDeliberation");
	// case GROUP_DONE:
	// return Translator.translate("PublicMsg.GroupDone");
	// case MARSHAL:
	// return Translator.translate("PublicMsg.CompetitionPaused");
	// default:
	// break;
	// }
	// // can't happen
	// return "";
	// }

	public String inferMessage(BreakType breakType, CeremonyType ceremonyType, boolean publicDisplay) {
		if (breakType == null && ceremonyType == null) {
			return Translator.translate("PublicMsg.CompetitionPaused");
		}
		if (ceremonyType != null) {
			switch (ceremonyType) {
				case INTRODUCTION:
					return Translator.translate("BreakMgmt.IntroductionOfAthletes");
				case MEDALS:
					return Translator.translate("PublicMsg.Medals");
				case OFFICIALS_INTRODUCTION:
					return Translator.translate("BreakMgmt.IntroductionOfOfficials");
			}
		}
		if (ceremonyType != null && ceremonyType == CeremonyType.INTRODUCTION) {
			// we display the introduction title even in the warmup room because it
			// is the introduction of the group that is warming up.
			return Translator.translate("BreakMgmt.IntroductionOfAthletes");
		}
		if (breakType == null) {
			return "";
		}
		switch (breakType) {
			case FIRST_CJ:
				return Translator.translate("BreakType.FIRST_CJ");
			case FIRST_SNATCH:
				return Translator.translate("BreakType.FIRST_SNATCH");
			case BEFORE_INTRODUCTION:
				return Translator.translate("BreakType.BEFORE_INTRODUCTION");
			case TECHNICAL:
				return Translator.translate("PublicMsg.CompetitionPaused");
			case JURY:
				return Translator.translate("PublicMsg.JuryDeliberation");
			case CHALLENGE:
				return Translator.translate("PublicMsg.CHALLENGE");
			case GROUP_DONE:
				return Translator.translate("PublicMsg.GroupDone");
			case MARSHAL:
				return Translator.translate("PublicMsg.CompetitionPaused");
			default:
				break;
		}
		// can't happen
		return "";
	}

	public boolean isDecisionLightsVisible() {
		return this.decisionLightsVisible;
	}

	public boolean isDown() {
		return this.down;
	}

	public void setBoardMode(String boardMode) {
		this.boardMode = boardMode;
	}

	public void setDecisionLight1(Boolean decisionLight1) {
		this.decisionLight1 = decisionLight1;
	}

	public void setDecisionLight2(Boolean decisionLight2) {
		this.decisionLight2 = decisionLight2;
	}

	public void setDecisionLight3(Boolean decisionLight3) {
		this.decisionLight3 = decisionLight3;
	}

	public void setDecisionLightsVisible(boolean decisionLightsVisible) {
		this.decisionLightsVisible = decisionLightsVisible;
	}

	public void setDown(boolean down) {
		this.down = down;
	}

	/**
	 * @param a
	 * @param ja
	 */
	public void setTeamFlag(Athlete a, JsonObject ja) {
		String team = a.getTeam();
		String teamFileName = URLUtils.sanitizeFilename(team);
		String prop = null;
		if (this.teamFlags == null) {
			this.teamFlags = URLUtils.checkFlags();
		}

		if (this.teamFlags && !team.isBlank()) {
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

	public void setTeamFlags(Boolean teamFlags) {
		this.teamFlags = teamFlags;
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		Athlete a = e.getAthlete();
		setHidden(false);
		doBreak(e);
		doUpdate(a, e);
		pushUpdate();
	}

	@Subscribe
	public void slaveBreakPause(UIEvent.BreakPaused e) {
		uiLog(e);
		pushTimer(e);
	}

	@Subscribe
	public void slaveBreakSet(UIEvent.BreakSetTime e) {
		uiLog(e);
		pushTimer(e);
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		uiLog(e);
		setHidden(false);
		doBreak(e);
		pushUpdate();
		pushTimer(e);
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		setHidden(false);
		doBreak(e);
		pushUpdate();
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		uiLog(e);
		setHidden(false);
		doCeremony(e);
		pushUpdate();
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		uiLog(e);
		setDecisionLight1(e.ref1);
		setDecisionLight2(e.ref2);
		setDecisionLight3(e.ref3);
		setDecisionLightsVisible(true);
		setDown(false);
		pushDecision(DecisionEventType.FULL_DECISION);
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		setDecisionLight1(null);
		setDecisionLight2(null);
		setDecisionLight3(null);
		setDecisionLightsVisible(false);
		setDown(false);
		pushDecision(DecisionEventType.RESET);
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		setDecisionLightsVisible(false);
		setDown(true);
		pushDecision(DecisionEventType.DOWN_SIGNAL);
	}

	@Subscribe
	public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
		uiLog(e);
		computeCurrentGroup(getFop().getGroup());
		pushUpdate();
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		Group g = e.getGroup();
		if (isDown()) {
			// wait until next event.
			return;
		} else if (isDecisionLightsVisible()) {
			computeCurrentGroup(g);
			// wait until next event.
			return;
		} else {
			computeCurrentGroup(g);
		}
		if (g == null) {
			setHidden(true);
		} else {
			setHidden(false);
			// done is a special kind of break.
			// the done event can be triggered when the decision is being given
			// we need to wait until after the decision is shown and reset.
			doBreak(g);
		}
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		Athlete a = e.getAthlete();
		computeCurrentGroup(e.getAthlete() != null ? e.getAthlete().getGroup() : null);
		doUpdate(a, e);
		pushUpdate();
	}

	@Subscribe
	public void slaveSetTime(UIEvent.SetTime e) {
		uiLog(e);
		setHidden(false);
		pushTimer(e);
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		setHidden(false);
		pushUpdate();
	}

	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
		uiLog(e);
		setHidden(false);
		pushTimer(e);
	}

	@Subscribe
	public void slaveStopTime(UIEvent.StopTime e) {
		uiLog(e);
		setHidden(false);
		pushTimer(e);
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		computeCurrentGroup(e.getGroup());
		if (e.getState() == null) {
			setHidden(true);
			pushUpdate();
			return;
		}
		switch (e.getState()) {
			case INACTIVE:
				setHidden(true);
				break;
			case BREAK:
				if (e.getAthlete() == null) {
					setHidden(true);
				} else {
					doUpdate(e.getAthlete(), e);
					doBreak(e);
				}
				break;
			default:
				setHidden(false);
				doUpdate(e.getAthlete(), e);
		}
		pushUpdate();
	}

	@Override
	public void unregister() {
		this.postBus.unregister(this);
		this.setFop(null);
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
		setTranslationMap(translations);
	}

	void setAttempt(String formattedAttempt) {
		this.attempt = formattedAttempt;
	}

	void setFullName(String fullName) {
		this.fullName = fullName;
	}

	void setGroupName(String name) {
		this.groupName = name;
	}

	void setHidden(boolean b) {
		this.hidden = b;
	}

	void setLiftsDone(String formattedDone) {
		this.liftsDone = formattedDone;
	}

	void setStartNumber(Integer integer) {
		this.startNumber = integer;
	}

	void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	void setWeight(Integer weight) {
		this.weight = weight;
	}

	private void computeCurrentGroup(Group g) {
		//Group group = getFop().getGroup();
		List<Athlete> displayOrder = getFop().getDisplayOrder();
		//int liftsDone = AthleteSorter.countLiftsDone(displayOrder);

		// setGroupName(group != null ? group.getName() : "");
		// setGroupInfo(computeSecondLine(getFop().getCurAthlete(), group != null ? group.getName() : null));
		// setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));

		if (displayOrder != null && displayOrder.size() > 0) {
			updateGroupInfo(computeLiftType(displayOrder.get(0)));
			setGroupAthletes(getAthletesJson(displayOrder, getFop().getLiftingOrder(), true));
			setLiftingOrderAthletes(getAthletesJson(getFop().getLiftingOrder(), getFop().getLiftingOrder(), false));
		} else {
			updateGroupInfo(null);
			setGroupAthletes(null);
			setLiftingOrderAthletes(null);
		}
		if (Competition.getCurrent().isSinclair()) {
			setNoLiftRanks("noranks sinclair");
		} else if (!Competition.getCurrent().isSnatchCJTotalMedals()) {
			setNoLiftRanks("noranks");
		} else {
			setNoLiftRanks("");
		}
		computeLeaders();
		setRecords(this.fop.getRecordsJson());
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

	private void computeLeaders() {
		// logger.debug("|||| computeLeaders {} {} {} {} {} {}", System.identityHashCode(this), fop.getName(),
		// System.identityHashCode(fop), fop.getGroup(), fop.getCurAthlete(), LoggerUtils.stackTrace());
		Athlete curAthlete = this.fop.getCurAthlete();
		if (curAthlete != null && curAthlete.getGender() != null) {
			setCategoryName(curAthlete.getCategory().getTranslatedName());
			this.groupLeaders = this.fop.getLeaders();
			if (this.groupLeaders == null || this.groupLeaders.isEmpty()) {
				setLeaders(null);
				return;
			}
			int size = this.groupLeaders.size();
			if (size > 16) {
				setLeaders(null);
			} else if (this.groupLeaders.size() > 0) {
				// null as second argument because we do not highlight current athletes in the
				// leaderboard
				setLeaders(getAthletesJson(this.groupLeaders, null, true));
			} else {
				// no one has totaled, so we show the snatch leaders
				if (!this.fop.isCjStarted()) {
					if (this.groupLeaders.size() > 0) {
						setLeaders(getAthletesJson(this.groupLeaders, null, true));
					} else {
						// nothing to show
						setLeaders(null);
					}
				} else {
					// nothing to show
					setLeaders(null);
				}
			}
		}

	}

	private String computeSecondLine(Athlete a, String groupName) {
		if (a == null) {
			return ("");
		}
		return Translator.translate("Scoreboard.GroupLiftType", groupName,
		        (a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		                : Translator.translate("Snatch")));
	}

	private void updateGroupInfo(String pLiftType) {
		Group lCurGroup = fop.getGroup();
		int lNbLiftsDone = AthleteSorter.countLiftsDone(fop.getDisplayOrder());

		String lGroupDescription = lCurGroup != null ? lCurGroup.getDescription() : null;
		String lGroupName = "";
		String lLiftsDone = "";
		if (lCurGroup != null && lCurGroup.isDone()) {
			lGroupName = lGroupDescription != null ? lGroupDescription : "\u00a0";
			lLiftsDone = "";
		} else if (lCurGroup != null && pLiftType != null) {
			String name = lGroupDescription != null ? lGroupDescription : lCurGroup.getName();
			String value = lGroupDescription == null ? Translator.translate("Scoreboard.GroupLiftType", name, pLiftType)
			        : Translator.translate("Scoreboard.DescriptionLiftTypeFormat", lGroupDescription, pLiftType);
			lGroupName = value;
			lLiftsDone = Translator.translate("Scoreboard.AttemptsDone", lNbLiftsDone);
		} else {
			lGroupName = "";
		}
		setGroupName(lGroupName);
		setGroupInfo(lGroupDescription);
		setLiftsDone(lLiftsDone);
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private Map<String, String> createDecision(DecisionEventType det) {
		Map<String, String> sb = new HashMap<>();
		mapPut(sb, "decisionEventType", det.toString());
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		setMode(sb);

		// competition state
		mapPut(sb, "competitionName", Competition.getCurrent().getCompetitionName());
		mapPut(sb, "fop", getFop().getName());
		FOPState state = getFop().getState();
		mapPut(sb, "fopState", state != null ? state.toString() : FOPState.INACTIVE.name());
		mapPut(sb, "break", String.valueOf(isBreak()));

		// current athlete & attempt
		mapPut(sb, "d1", getDecisionLight1() != null ? getDecisionLight1().toString() : null);
		mapPut(sb, "d2", getDecisionLight2() != null ? getDecisionLight2().toString() : null);
		mapPut(sb, "d3", getDecisionLight3() != null ? getDecisionLight3().toString() : null);
		mapPut(sb, "decisionsVisible", Boolean.toString(isDecisionLightsVisible()));
		mapPut(sb, "down", Boolean.toString(isDown()));

		createRecord(sb);

		return sb;
	}

	private void setMode(Map<String, String> sb) {
		String boardMode2 = getBoardMode();
		mapPut(sb, "mode", boardMode2);
	}

	private void createRecord(Map<String, String> sb) {
		if (this.records != null) {
			if (this.fop.getNewRecords() != null && !this.fop.getNewRecords().isEmpty()) {
				mapPut(sb, "recordKind", "new");
				mapPut(sb, "recordMessage", Translator.translate("Scoreboard.NewRecord"));
			} else if (this.fop.getChallengedRecords() != null && !this.fop.getChallengedRecords().isEmpty()) {
				mapPut(sb, "recordKind", "attempt");
				mapPut(sb, "recordMessage",
				        Translator.translate("Scoreboard.RecordAttempt"));
			} else {
				mapPut(sb, "recordKind", "none");
			}
			mapPut(sb, "records", this.records.toJson());
		}
	}

	private Map<String, String> createTimer(UIEvent e) {
		Map<String, String> sb = new HashMap<>();
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "fopName", getFop().getName());

		Integer breakMillisRemaining = null;
		Integer athleteMillisRemaining = null;
		Long breakStartTimeMillis = null;
		Long athleteStartTimeMillis = null;
		Boolean indefiniteBreak = null;
		String timerEventType = e.getClass().getSimpleName();
		mapPut(sb, "timerEventType", timerEventType);
		logger.debug("****** {}", timerEventType);

		if (e instanceof SetTime) {
			SetTime st = (SetTime) e;
			athleteStartTimeMillis = null;
			athleteMillisRemaining = st.getTimeRemaining();
		} else if (e instanceof StartTime) {
			StartTime st = (StartTime) e;
			athleteStartTimeMillis = System.currentTimeMillis();
			athleteMillisRemaining = st.getTimeRemaining();
		} else if (e instanceof UIEvent.StopTime) {
			StopTime st = (StopTime) e;
			athleteStartTimeMillis = null;
			athleteMillisRemaining = st.getTimeRemaining();
		} else if (e instanceof BreakSetTime) {
			BreakSetTime bst = (BreakSetTime) e;
			indefiniteBreak = bst.isIndefinite();
			if (bst.getEnd() != null) {
				breakMillisRemaining = (int) LocalDateTime.now().until(bst.getEnd(), ChronoUnit.MILLIS);
			} else {
				breakMillisRemaining = bst.isIndefinite() ? null : bst.getTimeRemaining();
			}
		} else if (e instanceof BreakStarted) {
			BreakStarted bst = (BreakStarted) e;
			breakStartTimeMillis = System.currentTimeMillis();
			breakMillisRemaining = bst.isIndefinite() ? null : bst.getTimeRemaining();
			indefiniteBreak = bst.isIndefinite();
		} else if (e instanceof BreakPaused) {
			logger.trace("????? break paused {}", LoggerUtils.whereFrom());
			BreakPaused bst = (BreakPaused) e;
			breakMillisRemaining = bst.isIndefinite() ? null : bst.getTimeRemaining();
			indefiniteBreak = bst.isIndefinite();
		} else if (e instanceof BreakDone) {
			breakMillisRemaining = -1;
		}

		athleteMillisRemaining = athleteMillisRemaining != null ? athleteMillisRemaining : 0;
		mapPut(sb, "athleteStartTimeMillis",
		        athleteStartTimeMillis != null ? Long.toString(athleteStartTimeMillis) : null);
		mapPut(sb, "athleteMillisRemaining", athleteMillisRemaining != null ? athleteMillisRemaining.toString() : null);

		breakStartTimeMillis = breakStartTimeMillis != null ? breakStartTimeMillis : System.currentTimeMillis();
		breakMillisRemaining = breakMillisRemaining != null ? breakMillisRemaining : 0;
		mapPut(sb, "breakStartTimeMillis", Long.toString(breakStartTimeMillis));
		mapPut(sb, "breakMillisRemaining", breakMillisRemaining != null ? breakMillisRemaining.toString() : null);
		mapPut(sb, "break", String.valueOf(isBreak()));
		mapPut(sb, "breakType",
		        ((getFop().getState() == FOPState.BREAK) && (getFop().getBreakType() != null))
		                ? getFop().getBreakType().toString()
		                : null);
		mapPut(sb, "indefiniteBreak", indefiniteBreak != null ? Boolean.toString(indefiniteBreak) : null);

		logger.debug("timer {} {} {} end {}", sb.get("timerEventType"), sb.get("break"), sb.get("breakType"),
		        breakStartTimeMillis + breakMillisRemaining);

		return sb;
	}

	private Map<String, String> createUpdate() {
		Map<String, String> sb = new HashMap<>();
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		String paramStylesDir = Config.getCurrent().getParamStylesDir();
		mapPut(sb, "stylesDir", paramStylesDir);

		if (this.translatorResetTimeStamp != Translator.getResetTimeStamp()) {
			// translation map has been updated (reload or language change)
			setTranslationMap();
		}

		// competition state
		mapPut(sb, "competitionName", Competition.getCurrent().getCompetitionName());
		mapPut(sb, "fop", getFop().getName());
		FOPState state = getFop().getState();
		mapPut(sb, "fopState", state != null ? state.toString() : FOPState.INACTIVE.name());
		String isBreak = String.valueOf(isBreak());
		mapPut(sb, "break", isBreak);
		BreakType breakType = getFop().getBreakType();
		String bts = ((getFop().getState() == FOPState.BREAK) && (breakType != null))
		        ? getFop().getBreakType().toString()
		        : null;

		mapPut(sb, "breakType", bts);
		// logger.trace("***** break {} breakType {}", isBreak, bts);
		IBreakTimer breakTimer = getFop().getBreakTimer();
		mapPut(sb, "breakIsIndefinite", Boolean.toString(breakTimer != null ? breakTimer.isIndefinite() : false));

		// current athlete & attempt
		mapPut(sb, "startNumber", this.startNumber != null ? this.startNumber.toString() : null);
		mapPut(sb, "categoryName", this.categoryName);
		mapPut(sb, "fullName", this.fullName);
		mapPut(sb, "teamName", this.teamName);
		mapPut(sb, "attempt", this.attempt);
		mapPut(sb, "weight", this.weight != null ? this.weight.toString() : null);
		mapPut(sb, "timeAllowed", this.timeAllowed != null ? this.timeAllowed.toString() : null);

		// current group
		mapPut(sb, "groupName", getGroupName());
		mapPut(sb, "groupInfo", getGroupInfo());
		mapPut(sb, "liftsDone", getLiftsDone());

		// bottom tables
		mapPut(sb, "noLiftRanks", getNoLiftRanks());
		if (this.groupAthletes != null) {
			mapPut(sb, "groupAthletes", this.groupAthletes.toJson());
		}
		if (this.liftingOrderAthletes != null) {
			mapPut(sb, "liftingOrderAthletes", this.liftingOrderAthletes.toJson());
		}
		if (this.leaders != null) {
			mapPut(sb, "leaders", this.leaders.toJson());
		}
		createRecord(sb);

		// presentation information
		mapPut(sb, "translationMap", this.translationMap.toJson());
		mapPut(sb, "hidden", String.valueOf(this.hidden));
		mapPut(sb, "wideTeamNames", String.valueOf(this.wideTeamNames));
		mapPut(sb, "sinclairMeet", Boolean.toString(Competition.getCurrent().isSinclair()));

		// include timer and decision info for synchronization on restart/refresh
		if (getLastTimerMap() != null) {
			sb.putAll(getLastTimerMap());
		}
		if (getLastDecisionMap() != null) {
			sb.putAll(getLastDecisionMap());
		}

		setBoardMode(computeBoardModeName(this.fop.getState(), this.fop.getBreakType(), this.fop.getCeremonyType()));
		setMode(sb);

		var breakStartTimeMillis = Long.parseLong(sb.get("breakStartTimeMillis"));
		var breakMillisRemaining = Long.parseLong(sb.get("breakMillisRemaining"));
		logger.debug("update last timerEvent {} {} {} end {}", sb.get("timerEventType"), sb.get("break"),
		        sb.get("breakType"), breakStartTimeMillis + breakMillisRemaining);

		return sb;
	}

	private void doBreak(Group g) {
		OwlcmsSession.withFop(fop -> {
			createUpdate();
			if (fop.getState() != FOPState.BREAK) {
				logger.debug("### done not break");
			} else {
				logger.debug("### done but break");
				setFullName(groupResults(g));
				setTeamName("");
				setAttempt("");
				setHidden(false);
			}
		});
		pushUpdate();
	}

	private void doDone(Group g) {
		logger.debug("forwarding doDone {}", g == null ? null : g.getName());
		computeCurrentGroup(g);
		if (g == null) {
			setHidden(true);
		} else {
			setFullName(g.getName());
			setGroupName("");
			setGroupInfo("");
			setLiftsDone("");
		}
		pushUpdate();
	}

	private void doPost(String url, Map<String, String> parameters) {
		HttpPost post = new HttpPost(url);
		// add request parameters or form parameters
		List<NameValuePair> urlParameters = new ArrayList<>();
		parameters.entrySet().stream()
		        .forEach((e) -> urlParameters.add(new BasicNameValuePair(e.getKey(), e.getValue())));

		boolean done = false;
		int nbTries = 0;
		// send post. if the local configuration files are missing, we are sent back a
		// 412 code.
		// we send the configuration files as well.
		while (!done && nbTries <= 1) {
			try {
				post.setEntity(new UrlEncodedFormEntity(urlParameters, "UTF-8"));
				try (CloseableHttpClient httpClient = HttpClients.createDefault();
				        CloseableHttpResponse response = httpClient.execute(post)) {
					StatusLine statusLine = response.getStatusLine();
					Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
					if (statusCode != null && statusCode != 200) {
						synchronized (singleThreadLock) {
							if (nbTries == 0 && statusCode != null && statusCode == 412) {
								logger.error("{}missing remote configuration {} {} {}",
								        FieldOfPlay.getLoggingName(getFop()), url,
								        statusLine,
								        LoggerUtils.whereFrom(1));
								sendConfig(parameters.get("updateKey"));
								nbTries++;
							} else {
								logger.error("{}could not post to {} {} {}", FieldOfPlay.getLoggingName(getFop()), url,
								        statusLine,
								        LoggerUtils.whereFrom(1));
								done = true;
							}
						}
					} else {
						done = true;
					}
				} catch (Exception e1) {
					logger.error("{}could not post to {} {}", FieldOfPlay.getLoggingName(getFop()), url,
					        LoggerUtils.exceptionMessage(e1));
					done = true;
				}
			} catch (UnsupportedEncodingException e2) {
				// can't happen.
				logger.error("{}could not post to {} {}", FieldOfPlay.getLoggingName(getFop()), url,
				        LoggerUtils.exceptionMessage(e2));
				done = true;
			}
		}
	}

	private void doUpdate(Athlete a, UIEvent e) {
		logger.trace("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
		boolean leaveTopAlone = false;
		if (e instanceof UIEvent.LiftingOrderUpdated) {
			LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
			if (e2.isInBreak()) {
				leaveTopAlone = !e2.isDisplayToggle();
			} else {
				leaveTopAlone = !e2.isCurrentDisplayAffected();
			}
		}
		if (a != null && a.getAttemptsDone() < 6) {
			if (!leaveTopAlone) {
				logger.trace("ef updating top {}", a.getFullName());
				setFullName(a.getFullName());
				setTeamName(a.getTeam());
				setStartNumber(a.getStartNumber());
				String formattedAttempt = formatAttempt(a.getAttemptsDone());
				setAttempt(formattedAttempt);
				setWeight(a.getNextAttemptRequestedWeight());
				if (e instanceof UIEvent.LiftingOrderUpdated) {
					setTimeAllowed(((LiftingOrderUpdated) e).getTimeAllowed());
				}
				String groupName = getFop().getGroup() != null ? getFop().getGroup().getName() : null;
				String computedName = groupName != null
				        ? computeSecondLine(a, groupName)
				        : "";
				setGroupInfo(computedName);
			}
		} else {
			if (!leaveTopAlone) {
				logger.trace("ef doUpdate doDone");
				Group g = (a != null ? a.getGroup() : null);
				doDone(g);
			}
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

	private void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
		String category;
		category = curCat != null ? curCat.getTranslatedName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank);
		ja.put("sattempts", this.sattempts);
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", this.cattempts);
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));
		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			ja.put("snatchRank", formatInt(mainRankings.getSnatchRank()));
			ja.put("cleanJerkRank", formatInt(mainRankings.getCleanJerkRank()));
			ja.put("totalRank", formatInt(mainRankings.getTotalRank()));
		} else {
			logger.error("main rankings null for {}", a);
		}

		ja.put("sinclair", computedScore(a));
		ja.put("sinclairRank", computedScoreRank(a));

		ja.put("group", a.getGroup().getName());
		ja.put("subCategory", a.getSubCategory());
		boolean notDone = a.getAttemptsDone() < 6;
		String blink = (notDone ? " blink" : "");
		if (notDone) {
			ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
		}
		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");
		setTeamFlag(a, ja);
	}

	/**
	 * @param startOrder     use starting order or lifting order ?
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	private JsonValue getAthletesJson(List<Athlete> groupAthletes, List<Athlete> liftOrder, boolean startOrder) {
		JsonArray jath = Json.createArray();
		int athx = 0;
		Category prevCat = null;
		Athlete prevAth = null;
		long currentId = (liftOrder != null && liftOrder.size() > 0) ? liftOrder.get(0).getId() : -1L;
		long nextId = (liftOrder != null && liftOrder.size() > 1) ? liftOrder.get(1).getId() : -1L;
		List<Athlete> athletes = groupAthletes != null ? Collections.unmodifiableList(groupAthletes)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			JsonObject ja = Json.createObject();
			Category curCat = a.getCategory();
			if (startOrder) {
				if (curCat != null && !curCat.sameAs(prevCat)) {
					// changing categories, put spacer before athlete
					ja.put("isSpacer", true);
					jath.set(athx, ja);
					ja = Json.createObject();
					prevCat = curCat;
					athx++;
				}
			} else {
				if (prevAth == null ||
				        (a.getActuallyAttemptedLifts() >= 3
				                && prevAth.getActuallyAttemptedLifts() < 3)) {
					// lifting order, put spacer before snatch done
					ja.put("isSpacer", true);
					jath.set(athx, ja);
					ja = Json.createObject();
					athx++;
				}
				prevAth = a;
			}
			getAthleteJson(a, ja, curCat, (a.getId() == currentId)
			        ? 1
			        : ((a.getId() == nextId)
			                ? 2
			                : 0));
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
	private synchronized void getAttemptsJson(Athlete a, int liftOrderRank) {
		this.sattempts = Json.createArray();
		this.cattempts = Json.createArray();
		for (int i = 0; i < 3; i++) {
			this.sattempts.set(i, Json.createNull());
			this.cattempts.set(i, Json.createNull());
		}
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
							String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
							        : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
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

	/**
	 * @return the fop
	 */
	private FieldOfPlay getFop() {
		return this.fop;
	}

	private String getNoLiftRanks() {
		return this.noLiftRanks;
	}

	private String groupResults(Group g) {
		return Translator.translate("Group_number_results", g.toString());
	}

	private boolean isBreak() {
		return getFop().getState() == FOPState.BREAK;
	}

	private void mapPut(Map<String, String> wr, String key, String value) {
		if (value == null) {
			return;
		}
		wr.put(key, value);
	}

	private void pushDecision(DecisionEventType det) {
		setBoardMode(computeBoardModeName(this.fop.getState(), this.fop.getBreakType(), this.fop.getCeremonyType()));
		String decisionUrl = Config.getCurrent().getParamDecisionUrl();
		String videoUrl = Config.getCurrent().getParamVideoDataDecisionUrl();
		if (decisionUrl == null && videoUrl == null) {
			return;
		}
		logger.trace("pushing {}", det);
		setLastDecisionMap(createDecision(det));
		sendPost(videoUrl, getLastDecisionMap());
		sendPost(decisionUrl, getLastDecisionMap());
	}

	private void pushTimer(UIEvent e) {
		setBoardMode(computeBoardModeName(this.fop.getState(), this.fop.getBreakType(), this.fop.getCeremonyType()));
		String timerUrl = Config.getCurrent().getParamTimerUrl();
		String videoUrl = Config.getCurrent().getParamVideoDataTimerUrl();
		if (timerUrl == null && videoUrl == null) {
			return;
		}
		setLastTimerMap(createTimer(e));
		sendPost(videoUrl, getLastTimerMap());
		sendPost(timerUrl, getLastTimerMap());
	}

	Thread keepaliveThread;

	/**
	 * push updates every n seconds in case publicresults is restarted. The individual instances on the receiver side
	 * can debounce.
	 */
	private void pushUpdate() {
		if (NO_KEEPALIVE) {
			pushUpdateDoIt();
			return;
		}
		if (keepaliveThread != null) {
			keepaliveThread.interrupt();
		}
		keepaliveThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					pushUpdateDoIt();
					Thread.sleep(KEEPALIVE_INTERVAL);
				} catch (InterruptedException e) {
					logger.debug("thread {} interrupted", Thread.currentThread().getId());
					break;
				}
			}
		});
		keepaliveThread.start();
	}

	private void pushUpdateDoIt() {
		setBoardMode(computeBoardModeName(this.fop.getState(), this.fop.getBreakType(),
		        this.fop.getCeremonyType()));
		logger.debug("### pushing update from {}", Thread.currentThread().getId());
		String updateUrl = Config.getCurrent().getParamUpdateUrl();
		String videoUrl = Config.getCurrent().getParamVideoDataUpdateUrl();
		if (updateUrl == null && videoUrl == null) {
			return;
		}
		lastUpdate = createUpdate();
		sendPost(videoUrl, lastUpdate);
		sendPost(updateUrl, lastUpdate);
	}

	private void sendConfig(String updateKey) {
		String destination = Config.getCurrent().getParamPublicResultsURL() + "/config";
		// wait for previous send to finish.
		// no consequences sending it multiple times in a row -- we have no idea why it
		// is being requested again.
		synchronized (Config.getCurrent()) {
			try {
				logger.info("{}sending config", FieldOfPlay.getLoggingName(getFop()));
				HttpPost post = new HttpPost(destination);

				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				builder.addPart("updateKey", new StringBody(updateKey, ContentType.TEXT_PLAIN));

				try {
					PipedOutputStream out = new PipedOutputStream();
					PipedInputStream in = new PipedInputStream(out);
					new Thread(() -> {
						try {
							ResourceWalker.zipPublicResultsConfig(out);
							out.flush();
							out.close();
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}
					}).start();
					builder.addBinaryBody("local", in, ContentType.create("application/zip"), "local.zip");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				HttpEntity entity = builder.build();

				post.setEntity(entity);
				try (CloseableHttpClient httpClient = HttpClients.createDefault();
				        CloseableHttpResponse response = httpClient.execute(post)) {
					StatusLine statusLine = response.getStatusLine();
					Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
					if (statusCode != null && statusCode != 200) {
						logger.error("{}could not send config to {} {} {}", FieldOfPlay.getLoggingName(getFop()),
						        destination,
						        statusLine,
						        LoggerUtils.whereFrom(1));
					}
					EntityUtils.toString(response.getEntity());
				} catch (Exception e1) {
					logger.error("{}could not send config to {} {}", FieldOfPlay.getLoggingName(getFop()), destination,
					        LoggerUtils.exceptionMessage(e1));
				}
			} catch (Exception e2) {
				logger.error("{}could not send config to {} {}", FieldOfPlay.getLoggingName(getFop()), destination, e2);
			}
		}
	}

	private void sendPost(String url, Map<String, String> parameters) {
		if (url == null) {
			return;
		}
		// logger.debug("{}posting update {}", getFop().getLoggingName(),
		// LoggerUtils.whereFrom());
		long deltaMillis = System.currentTimeMillis() - this.previousMillis;
		int hashCode = parameters.hashCode();
		// debounce, sometimes several identical updates in a rapid succession
		// identical updates are ok after 1 sec.
		if (hashCode != this.previousHashCode || (deltaMillis > 1000)) {
			new Thread(() -> doPost(url, parameters)).start();

			this.previousHashCode = hashCode;
			this.previousMillis = System.currentTimeMillis();
		}

	}

	private void setCategoryName(String name) {
		this.categoryName = name;
	}

	/**
	 * @param fop the fop to set
	 */
	private void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	private void setGroupAthletes(JsonValue athletesJson) {
		this.groupAthletes = athletesJson;
	}

	private void setGroupInfo(String computeSecondLine) {
		this.groupInfo = computeSecondLine;
	}

	private void setLeaders(JsonValue athletesJson) {
		this.leaders = athletesJson;
	}

	private void setLiftingOrderAthletes(JsonValue athletesJson) {
		this.liftingOrderAthletes = athletesJson;
	}

	private void setNoLiftRanks(String string) {
		this.noLiftRanks = string;
	}

	private void setRecords(JsonValue recordsJson) {
		this.records = recordsJson;
	}

	private void setTimeAllowed(Integer timeAllowed) {
		this.timeAllowed = timeAllowed;
	}

	private void setTranslationMap(JsonObject translations) {
		this.translationMap = translations;
	}

	private void setWideTeamNames(boolean b) {
		this.wideTeamNames = b;
	}

	private void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        null, e.getOrigin(), LoggerUtils.whereFrom());
	}

	private Map<String, String> getLastTimerMap() {
		return lastTimerMap;
	}

	private void setLastTimerMap(Map<String, String> lastTimerMap) {
		this.lastTimerMap = lastTimerMap;
	}

	private Map<String, String> getLastDecisionMap() {
		return lastDecisionMap;
	}

	private void setLastDecisionMap(Map<String, String> lastDecisionMap) {
		this.lastDecisionMap = lastDecisionMap;
	}

}
