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

//    private static HashMap<String, EventForwarder> registeredFop = new HashMap<>();

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

	public EventForwarder(FieldOfPlay emittingFop) {
		this.setFop(emittingFop);
		// logger.debug("|||| eventForwarder {} {} {}", System.identityHashCode(this),
		// emittingFop.getName(),System.identityHashCode(emittingFop));

		postBus = getFop().getPostEventBus();
		postBus.register(this);

		translatorResetTimeStamp = 0L;

		String updateKey = Config.getCurrent().getParamUpdateKey();
		String updateUrl = Config.getCurrent().getParamUpdateUrl();
		if (updateUrl == null || updateKey == null || updateUrl.trim().isEmpty()
		        || updateKey.trim().isEmpty()) {
			logger.info("{}Pushing results to remote site not enabled.", FieldOfPlay.getLoggingName(getFop()));
		} else {
			logger.info("{}Pushing to remote site {}", FieldOfPlay.getLoggingName(getFop()), updateUrl);
		}
		pushUpdate();
	}
	
	@Override
	public void unregister() {
		postBus.unregister(this);
		this.setFop(null);
	}

	@Override
	public void doBreak(UIEvent e) {
		BreakType breakType = fop.getBreakType();
		Group group = fop.getGroup();
		if (breakType == null) {
			breakType = BreakType.BEFORE_INTRODUCTION;
		}
		switch (breakType) {
		case GROUP_DONE:
			setFullName(groupResults(group));
			break;
		default:
			setFullName((group != null ? (Translator.translate("Group_number", group.getName()) + " &ndash; ") : "")
			        + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			break;
		}
		setTeamName("");
		setAttempt("");
		setHidden(false);
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		BreakType breakType = fop.getBreakType();
		Group group = fop.getGroup();
		if (breakType == null) {
			breakType = BreakType.BEFORE_INTRODUCTION;
		}
		switch (breakType) {
		case GROUP_DONE:
			setFullName(groupResults(group));
			break;
		default:
			setFullName((group != null ? (Translator.translate("Group_number", group.getName()) + " &ndash; ") : "")
			        + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			break;
		}
		setTeamName("");
		setAttempt("");
		setHidden(false);
	}

	public Boolean getDecisionLight1() {
		return decisionLight1;
	}

	public Boolean getDecisionLight2() {
		return decisionLight2;
	}

	public Boolean getDecisionLight3() {
		return decisionLight3;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getLiftsDone() {
		return liftsDone;
	}

	public JsonValue getRecords() {
		return records;
	}

	public Integer getTimeAllowed() {
		return timeAllowed;
	}

	public JsonObject getTranslationMap() {
		return translationMap;
	}

	/**
	 * Change the messages because we are not showing live timers
	 *
	 * @see app.owlcms.uievents.BreakDisplay#inferMessage(app.owlcms.uievents.BreakType)
	 */
	@Override
	public String inferMessage(BreakType breakType, CeremonyType ceremonyType, boolean publicDisplay) {
		if (breakType == null) {
			return Translator.translate("PublicMsg.CompetitionPaused");
		}
		if (ceremonyType != null
				//&& publicDisplay
				) {
			switch (ceremonyType) {
			case INTRODUCTION:
				return Translator.translate("BreakMgmt.IntroductionOfAthletes");
			case MEDALS:
				return Translator.translate("PublicMsg.Medals");
			case OFFICIALS_INTRODUCTION:
				return Translator.translate("BreakMgmt.IntroductionOfOfficials");
			}
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
		return decisionLightsVisible;
	}

	public boolean isDown() {
		return down;
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

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		Athlete a = e.getAthlete();
		setHidden(false);
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
		Group group = getFop().getGroup();
		List<Athlete> displayOrder = getFop().getDisplayOrder();
		int liftsDone = AthleteSorter.countLiftsDone(displayOrder);
		setGroupName(computeSecondLine(getFop().getCurAthlete(), group != null ? group.getName() : null));
		setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));
		if (displayOrder != null && displayOrder.size() > 0) {
			setGroupAthletes(getAthletesJson(displayOrder, getFop().getLiftingOrder(), true));
			setLiftingOrderAthletes(getAthletesJson(getFop().getLiftingOrder(), getFop().getLiftingOrder(), false));
		} else {
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
		setRecords(fop.getRecordsJson());
	}

	private void computeLeaders() {
//        logger.debug("|||| computeLeaders {} {} {} {} {} {}", System.identityHashCode(this), fop.getName(),
//                System.identityHashCode(fop), fop.getGroup(), fop.getCurAthlete(), LoggerUtils.stackTrace());
		Athlete curAthlete = fop.getCurAthlete();
		if (curAthlete != null && curAthlete.getGender() != null) {
			setCategoryName(curAthlete.getCategory().getTranslatedName());
			groupLeaders = fop.getLeaders();
			if (groupLeaders == null || groupLeaders.isEmpty()) {
				setLeaders(null);
				return;
			}
			int size = groupLeaders.size();
			if (size > 16) {
				setLeaders(null);
			} else if (groupLeaders.size() > 0) {
				// null as second argument because we do not highlight current athletes in the
				// leaderboard
				setLeaders(getAthletesJson(groupLeaders, null, true));
			} else {
				// no one has totaled, so we show the snatch leaders
				if (!fop.isCjStarted()) {
					if (groupLeaders.size() > 0) {
						setLeaders(getAthletesJson(groupLeaders, null, true));
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

	private Map<String, String> createDecision(DecisionEventType det) {
		Map<String, String> sb = new HashMap<>();
		mapPut(sb, "eventType", det.toString());
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());

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
		mapPut(sb, "mode", getBoardMode());

		createRecord(sb);

		return sb;
	}

	private void createRecord(Map<String, String> sb) {
		if (records != null) {
			if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
				mapPut(sb, "recordKind", "new");
				mapPut(sb, "recordMessage", Translator.translate("Scoreboard.NewRecord"));
			} else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
				mapPut(sb, "recordKind", "attempt");
				mapPut(sb, "recordMessage",
				        Translator.translate("Scoreboard.RecordAttempt"));
			} else {
				mapPut(sb, "recordKind", "none");
			}
			mapPut(sb, "records", records.toJson());
		}
	}

	private Map<String, String> createTimer(UIEvent e) {
		Map<String, String> sb = new HashMap<>();
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "fopName", getFop().getName());

		Integer milliseconds = null;
		boolean indefiniteBreak = false;
		mapPut(sb, "eventType", e.getClass().getSimpleName());
		if (e instanceof SetTime) {
			SetTime st = (SetTime) e;
			milliseconds = st.getTimeRemaining();
		} else if (e instanceof StartTime) {
			StartTime st = (StartTime) e;
			milliseconds = st.getTimeRemaining();
		} else if (e instanceof UIEvent.StopTime) {
			StopTime st = (StopTime) e;
			milliseconds = st.getTimeRemaining();
		} else if (e instanceof BreakSetTime) {
			BreakSetTime bst = (BreakSetTime) e;
			if (bst.getEnd() != null) {
				milliseconds = (int) LocalDateTime.now().until(bst.getEnd(), ChronoUnit.MILLIS);
			} else {
				milliseconds = bst.isIndefinite() ? null : bst.getTimeRemaining();
			}
		} else if (e instanceof BreakStarted) {
			BreakStarted bst = (BreakStarted) e;
			milliseconds = bst.isIndefinite() ? null : bst.getTimeRemaining();
		} else if (e instanceof BreakPaused) {
			logger.trace("????? break paused {}", LoggerUtils.whereFrom());
			BreakPaused bst = (BreakPaused) e;
			milliseconds = bst.isIndefinite() ? null : bst.getTimeRemaining();
		} else if (e instanceof BreakDone) {
			milliseconds = -1;
		}

		mapPut(sb, "milliseconds", milliseconds != null ? milliseconds.toString() : null);
		mapPut(sb, "break", String.valueOf(isBreak()));
		mapPut(sb, "breakType",
		        ((getFop().getState() == FOPState.BREAK) && (getFop().getBreakType() != null))
		                ? getFop().getBreakType().toString()
		                : null);
		mapPut(sb, "indefiniteBreak", Boolean.toString(indefiniteBreak));
		mapPut(sb, "mode", getBoardMode());

		return sb;
	}

	private Map<String, String> createUpdate() {
		Map<String, String> sb = new HashMap<>();
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		String paramStylesDir = Config.getCurrent().getParamStylesDir();
		mapPut(sb, "stylesDir", paramStylesDir);

		if (translatorResetTimeStamp != Translator.getResetTimeStamp()) {
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
		int breakTimeRemaining = breakTimer != null ? breakTimer.liveTimeRemaining() : 0;
		mapPut(sb, "breakRemaining", Integer.toString(breakTimeRemaining));
		mapPut(sb, "breakIsIndefinite", Boolean.toString(breakTimer != null ? breakTimer.isIndefinite() : false));

		// current athlete & attempt
		mapPut(sb, "startNumber", startNumber != null ? startNumber.toString() : null);
		mapPut(sb, "categoryName", categoryName);
		mapPut(sb, "fullName", fullName);
		mapPut(sb, "teamName", teamName);
		mapPut(sb, "attempt", attempt);
		mapPut(sb, "weight", weight != null ? weight.toString() : null);
		mapPut(sb, "timeAllowed", timeAllowed != null ? timeAllowed.toString() : null);

		// current group
		mapPut(sb, "groupName", getGroupName());
		mapPut(sb, "liftsDone", getLiftsDone());

		// bottom tables
		mapPut(sb, "noLiftRanks", getNoLiftRanks());
		if (groupAthletes != null) {
			mapPut(sb, "groupAthletes", groupAthletes.toJson());
		}
		if (liftingOrderAthletes != null) {
			mapPut(sb, "liftingOrderAthletes", liftingOrderAthletes.toJson());
		}
		if (leaders != null) {
			mapPut(sb, "leaders", leaders.toJson());
		}
		createRecord(sb);

		// presentation information
		mapPut(sb, "translationMap", translationMap.toJson());
		mapPut(sb, "hidden", String.valueOf(hidden));
		mapPut(sb, "wideTeamNames", String.valueOf(wideTeamNames));
		mapPut(sb, "sinclairMeet", Boolean.toString(Competition.getCurrent().isSinclair()));
		mapPut(sb, "mode", getBoardMode());

		return sb;
	}

	private void doBreak(Group g) {
		OwlcmsSession.withFop(fop -> {
			createUpdate();
			if (fop.getState() != FOPState.BREAK) {
				logger.debug("### done not break");
				return;
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
								logger.error("{}missing remote configuration {} {} {}", FieldOfPlay.getLoggingName(getFop()), url,
								        statusLine,
								        LoggerUtils.whereFrom(1));
								sendConfig(parameters.get("updateKey"));
								nbTries++;
							} else {
								logger.error("{}could not post to {} {} {}", FieldOfPlay.getLoggingName(getFop()), url, statusLine,
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
				setGroupName(computedName);
			}
		} else {
			if (!leaveTopAlone) {
				logger.trace("ef doUpdate doDone");
				Group g = (a != null ? a.getGroup() : null);
				doDone(g);
			}
			return;
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
		ja.put("sattempts", sattempts);
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", cattempts);
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
	 * @param a
	 * @param ja
	 */
	public void setTeamFlag(Athlete a, JsonObject ja) {
		String team = a.getTeam();
		String teamFileName = URLUtils.sanitizeFilename(team);
		String prop = null;
		if (teamFlags == null) {
			teamFlags = URLUtils.checkFlags();
		}
		
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
	 * CSS classes are pre-computed and passed along with the values; weights are
	 * formatted.
	 *
	 * @param a
	 * @param liftOrderRank2
	 * @return json string with nested attempts values
	 */
	private synchronized void getAttemptsJson(Athlete a, int liftOrderRank) {
		sattempts = Json.createArray();
		cattempts = Json.createArray();
		for (int i = 0; i < 3; i++) {
			sattempts.set(i, Json.createNull());
			cattempts.set(i, Json.createNull());
		}
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
						String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
						        : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
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
				sattempts.set(ix, jri);
			} else {
				cattempts.set(ix % 3, jri);
			}
			ix++;
		}
	}

	/**
	 * @return the fop
	 */
	private FieldOfPlay getFop() {
		return fop;
	}

	private String getNoLiftRanks() {
		return noLiftRanks;
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
		setBoardMode(computeBoardModeName(fop.getState(), fop.getBreakType(), fop.getCeremonyType()));
		String decisionUrl = Config.getCurrent().getParamDecisionUrl();
		if (decisionUrl == null) {
			return;
		}
		logger.trace("pushing {}", det);
		sendPost(decisionUrl, createDecision(det));
	}

	private void pushTimer(UIEvent e) {
		setBoardMode(computeBoardModeName(fop.getState(), fop.getBreakType(), fop.getCeremonyType()));
		String timerUrl = Config.getCurrent().getParamTimerUrl();
		if (timerUrl == null) {
			return;
		}
		sendPost(timerUrl, createTimer(e));
	}

	private void pushUpdate() {
		setBoardMode(computeBoardModeName(fop.getState(), fop.getBreakType(), fop.getCeremonyType()));
		logger.debug("### pushing update");
		String updateUrl = Config.getCurrent().getParamUpdateUrl();
		if (updateUrl == null) {
			return;
		}
		sendPost(updateUrl, createUpdate());
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
						logger.error("{}could not send config to {} {} {}", FieldOfPlay.getLoggingName(getFop()), destination,
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
		// logger.debug("{}posting update {}", getFop().getLoggingName(),
		// LoggerUtils.whereFrom());
		long deltaMillis = System.currentTimeMillis() - previousMillis;
		int hashCode = parameters.hashCode();
		// debounce, sometimes several identical updates in a rapid succession
		// identical updates are ok after 1 sec.
		if (hashCode != previousHashCode || (deltaMillis > 1000)) {
			new Thread(() -> doPost(url, parameters)).start();

			previousHashCode = hashCode;
			previousMillis = System.currentTimeMillis();
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
		wideTeamNames = b;
	}

	private void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        null, e.getOrigin(), LoggerUtils.whereFrom());
	}

	public Boolean getTeamFlags() {
		return teamFlags;
	}

	public void setTeamFlags(Boolean teamFlags) {
		this.teamFlags = teamFlags;
	}

	public String getBoardMode() {
		return boardMode;
	}

	public void setBoardMode(String boardMode) {
		this.boardMode = boardMode;
	}
	
	private String computedScoreRank(Athlete a) {
		Integer value = Ranking.getRanking(a, Competition.getCurrent().getScoringSystem());
		return value != null && value > 0 ? "" + value : "-";
	}

	private String computedScore(Athlete a) {
		Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
		double value = Ranking.getRankingValue(a, scoringSystem);
		String score = value > 0.001 ? String.format("%.3f", value) : "-";
		return score;
	}

}
