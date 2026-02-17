/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
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
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.group.Group;
import app.owlcms.data.team.Team;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.HasBoardMode;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.DecisionEventType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakDone;
import app.owlcms.uievents.UIEvent.BreakPaused;
import app.owlcms.uievents.UIEvent.BreakSetTime;
import app.owlcms.uievents.UIEvent.BreakStarted;
import app.owlcms.uievents.UIEvent.CeremonyDone;
import app.owlcms.uievents.UIEvent.IniitialDecision;
import app.owlcms.uievents.UIEvent.JuryNotification;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.uievents.UIEvent.SetTime;
import app.owlcms.uievents.UIEvent.StartTime;
import app.owlcms.uievents.UIEvent.StopTime;
import app.owlcms.monitors.websocket.AthleteExporter;
import app.owlcms.monitors.websocket.ForwarderPayloadBuilder;
import app.owlcms.monitors.websocket.ForwarderPayloadBuilder.CompetitionDataExport;
import app.owlcms.monitors.websocket.WebSocketSender;
import app.owlcms.monitors.websocket.WebSocketEventSender;
import app.owlcms.utils.DatabaseZipHelper;
import app.owlcms.utils.FlagsZipHelper;
import app.owlcms.utils.TranslationsZipHelper;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class WebSocketEventForwarder implements BreakDisplay, HasBoardMode, IUnregister {

	private static final int KEEPALIVE_INTERVAL = 15000;
	final private static Logger logger = (Logger) LoggerFactory.getLogger(WebSocketEventForwarder.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	public static final Object singleThreadLock = new Object();
	private static Map<String, WebSocketEventForwarder> eventForwarderByName = new HashMap<>();

	synchronized public static WebSocketEventForwarder initEventForwarderByName(String name, FieldOfPlay fieldOfPlay) {
		// Check if there are any WebSocket URLs to forward to
		String updateUrl = Config.getCurrent().getParamPublicResultsURL();
		String updateUrlV = Config.getCurrent().getParamVideoDataURL();
		boolean hasWebSocketUrl = false;
		
		if (updateUrl != null && !updateUrl.trim().isEmpty() && 
		    (updateUrl.startsWith("ws://") || updateUrl.startsWith("wss://"))) {
			hasWebSocketUrl = true;
		}
		if (updateUrlV != null && !updateUrlV.trim().isEmpty() && 
		    (updateUrlV.startsWith("ws://") || updateUrlV.startsWith("wss://"))) {
			hasWebSocketUrl = true;
		}
		
		if (!hasWebSocketUrl) {
			logger.info("{}no WebSocket URLs configured, skipping WebSocketEventForwarder creation", FieldOfPlay.getLoggingName(fieldOfPlay));
			return null;
		}
		
		WebSocketEventForwarder eventForwarder = eventForwarderByName.get(name);
		if (eventForwarder == null) {
			logger.info("{}creating websocket event forwarder", FieldOfPlay.getLoggingName(fieldOfPlay));
			WebSocketEventForwarder newForwarder = new WebSocketEventForwarder(name, fieldOfPlay);
			eventForwarderByName.put(name, newForwarder);
			return newForwarder;
		} else {
			// reusing the found forwarder, forcing the values
			logger.info("{}reusing websocket event forwarder", FieldOfPlay.getLoggingName(fieldOfPlay));
			eventForwarder.setFop(fieldOfPlay);
			return eventForwarder;
		}
	}

	synchronized public static WebSocketEventForwarder getEventForwarderByName(String name) {
		return eventForwarderByName.get(name);
	}

	/**
	 * Reinitialize WebSocket event forwarders for all FOPs.
	 * Call this when WebSocket URL configuration changes to create forwarders
	 * that weren't created at startup (because no URL was configured then),
	 * or to close connections when URLs are removed.
	 */
	synchronized public static void reinitializeForAllFOPs() {
		logger.info("reinitializing WebSocket event forwarders for all FOPs after config change");
		
		// Get current URL configuration
		Config current = Config.getCurrent();
		String publicResultsUrl = current.getParamPublicResultsURL();
		String videoDataUrl = current.getParamVideoDataURL();
		
		boolean hasPublicResultsWs = publicResultsUrl != null && !publicResultsUrl.trim().isEmpty()
			&& (publicResultsUrl.startsWith("ws://") || publicResultsUrl.startsWith("wss://"));
		boolean hasVideoDataWs = videoDataUrl != null && !videoDataUrl.trim().isEmpty()
			&& (videoDataUrl.startsWith("ws://") || videoDataUrl.startsWith("wss://"));
		
		for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
			WebSocketEventForwarder existing = fop.getWebSocketEventForwarder();
			if (existing == null) {
				// No forwarder exists - try to create one if URLs are now configured
				WebSocketEventForwarder newForwarder = initEventForwarderByName(fop.getName(), fop);
				if (newForwarder != null) {
					fop.setWebSocketEventForwarder(newForwarder);
					logger.info("{}created WebSocket event forwarder after config change", FieldOfPlay.getLoggingName(fop));
					// Trigger an initial update to establish connection
					newForwarder.pushUpdate(null);
				}
			} else {
				// Forwarder exists - check for URL removal and close stale connections
				existing.closeStaleConnections(hasPublicResultsWs, hasVideoDataWs);
				
				// Trigger an update which will detect URL changes and open new connections
				if (hasPublicResultsWs || hasVideoDataWs) {
					logger.info("{}triggering update on existing WebSocket event forwarder", FieldOfPlay.getLoggingName(fop));
					existing.pushUpdate(null);
				}
			}
		}
	}

	private static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

	private boolean NO_KEEPALIVE = false;
	private String attempt;
	private Integer attemptNumber;
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

	private List<Athlete> groupLeaders;
	private String sessionDescription;
	private String sessionName;
	private boolean hidden;
	private JsonValue leaders;
	private List<Map<String, Object>> leadersSessionData;
	private String liftsDone;
	private EventBus postBus;
	private JsonArray sattempts;
	private Integer startNumber;
	private String teamName;
	private Integer timeAllowed;
	private JsonObject translationMap;
	private long translatorResetTimeStamp;
	private Integer weight;
	private boolean wideTeamNames;
	private JsonValue records;
	private Boolean teamFlags;
	private String boardMode;
	private String sessionInfo;
	private Map<String, String> lastTimerMap;
	private Map<String, String> lastDecisionMap;
	private Map<String, Object> lastUpdate;
	Thread keepaliveThread;
	private boolean showLiftRanks;
	private boolean showSinclair;
	private boolean showSinclairRank;
	private boolean showTotalRank;
	private CeremonyType ceremonyType;
	private BreakType breakType;
	private FOPState fopState;
	private Group ceremonySession;
	private Category ceremonyCategory;
	private AgeGroup ceremonyAgeGroup;
	private Championship ceremonyChampionship;
	private String ceremonyEventType;
	private String forwardedFopName;
	private String liftType;
	private String liftTypeKey;
	Map<String, Integer> debouncingHash = new HashMap<>();
	Map<String, Long> debouncingMillis = new HashMap<>();
	// Track current WebSocket URLs to detect changes
	private String currentPublicResultsUrl = null;
	private String currentVideoDataUrl = null;

	/**
	 * Check if this forwarder has any active WebSocket URLs to send to.
	 * Event handlers should call this first and return immediately if false.
	 * 
	 * @return true if there are ws:// or wss:// URLs configured, false otherwise
	 */
	public boolean isActive() {
		String publicUrl = Config.getCurrent().getParamPublicResultsURL();
		String videoUrl = Config.getCurrent().getParamVideoDataURL();
		
		boolean hasPublicUrl = publicUrl != null && !publicUrl.trim().isEmpty() 
			&& (publicUrl.startsWith("ws://") || publicUrl.startsWith("wss://"));
		boolean hasVideoUrl = videoUrl != null && !videoUrl.trim().isEmpty()
			&& (videoUrl.startsWith("ws://") || videoUrl.startsWith("wss://"));
		
		return hasPublicUrl || hasVideoUrl;
	}

	/**
	 * Close WebSocket connections for URLs that have been removed from config.
	 * Called when config is saved to clean up stale connections.
	 * 
	 * @param hasPublicResultsWs true if a valid publicResults WebSocket URL is configured
	 * @param hasVideoDataWs true if a valid videoData WebSocket URL is configured
	 */
	public void closeStaleConnections(boolean hasPublicResultsWs, boolean hasVideoDataWs) {
		// Close publicResults connection if URL was removed
		if (!hasPublicResultsWs && this.currentPublicResultsUrl != null) {
			logger.info("{}PublicResults WebSocket URL removed, closing connection to {}",
			        FieldOfPlay.getLoggingName(getFop()), this.currentPublicResultsUrl);
			WebSocketEventSender.closeSender(this.currentPublicResultsUrl);
			this.currentPublicResultsUrl = null;
		}
		
		// Close videoData connection if URL was removed
		if (!hasVideoDataWs && this.currentVideoDataUrl != null) {
			logger.info("{}VideoData WebSocket URL removed, closing connection to {}",
			        FieldOfPlay.getLoggingName(getFop()), this.currentVideoDataUrl);
			WebSocketEventSender.closeSender(this.currentVideoDataUrl);
			this.currentVideoDataUrl = null;
		}
	}

	private static final ObjectMapper JSON_MAPPER = createObjectMapper();

	private WebSocketEventForwarder(String name, FieldOfPlay emittingFop) {
		this.setForwardedFopName(name);
		this.setFop(emittingFop);
		// logger.debug("|||| eventForwarder {} {} {}", System.identityHashCode(this),
		// emittingFop.getName(),System.identityHashCode(emittingFop));

		this.postBus = getFop().getEventForwardingBus();
		this.postBus.register(this);

		this.translatorResetTimeStamp = 0L;

		// update key is actually not mandatory
		// String updateKey = Config.getCurrent().getParamUpdateKey();
		String updateUrl = Config.getCurrent().getParamPublicResultsURL();
		boolean publicResultsEnabled = false;
		boolean publicResultsIsWebSocket = false;
		if (updateUrl == null || updateUrl.trim().isEmpty()) {
			logger.info("{}publicresults not enabled.", FieldOfPlay.getLoggingName(getFop()));
		} else if (updateUrl.startsWith("http://") || updateUrl.startsWith("https://")) {
			logger.info("{}ignoring HTTP publicresults URL (handled by EventForwarder): {}", FieldOfPlay.getLoggingName(getFop()), updateUrl);
		} else {
			publicResultsEnabled = true;
			publicResultsIsWebSocket = updateUrl.startsWith("ws://") || updateUrl.startsWith("wss://");
			logger.info("{}publicresults enabled, pushing to {}", FieldOfPlay.getLoggingName(getFop()), updateUrl);
		}
		if (emittingFop.getState() != null) {
			pushUpdate(null);
		}

		// update key is actually not mandatory
		// String updateKeyV = Config.getCurrent().getParamVideoDataKey();
		String updateUrlV = Config.getCurrent().getParamVideoDataURL();
		boolean videoResultsEnabled = false;
		boolean videoResultsIsWebSocket = false;
		if (updateUrlV == null || updateUrlV.trim().isEmpty()) {
			logger.info("{}video data not enabled.", FieldOfPlay.getLoggingName(getFop()));
		} else if (updateUrlV.startsWith("http://") || updateUrlV.startsWith("https://")) {
			logger.info("{}ignoring HTTP video data URL (handled by EventForwarder): {}", FieldOfPlay.getLoggingName(getFop()), updateUrlV);
		} else {
			videoResultsEnabled = true;
			videoResultsIsWebSocket = updateUrlV.startsWith("ws://") || updateUrlV.startsWith("wss://");
			logger.info("{}video data enabled, pushing to {}", FieldOfPlay.getLoggingName(getFop()), updateUrlV);
		}
		if (emittingFop.getState() != null) {
			pushUpdate(null);
		}

		this.NO_KEEPALIVE = Config.getCurrent().featureSwitch("noForwarderKeepAlive");
		// Disable keepalive if using WebSocket (persistent connection doesn't need keepalive)
		if (publicResultsIsWebSocket || videoResultsIsWebSocket) {
			this.NO_KEEPALIVE = true;
			logger.info("{}event forwarding keepalive disabled (WebSocket protocol)", FieldOfPlay.getLoggingName(getFop()));
		}
		if (!publicResultsEnabled && !videoResultsEnabled) {
			this.NO_KEEPALIVE = true;
			logger.info("{}event forwading keepalive disabled", FieldOfPlay.getLoggingName(getFop()), updateUrlV);
		}
	}

	/**
	 * @see app.owlcms.uievents.BreakDisplay#doBreak(app.owlcms.uievents.UIEvent)
	 */
	@Override
	public void doBreak(UIEvent e) {
		if (!isBreak()) {
			return;
		}
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
		if (!isBreak()) {
			return;
		}
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
		setCeremonyEventType("ceremonyStarted");
		setCeremonyType(this.ceremonyType);
		setCeremonySession(e.getCeremonySession() != null ? e.getCeremonySession() : null);
		setCeremonyCategory(e.getCeremonyCategory() != null ? e.getCeremonyCategory() : null);
		setCeremonyAgeGroup(e.getAgeGroup() != null ? e.getAgeGroup() : null);
		setCeremonyChampionship(e.getChampionship() != null ? e.getChampionship() : null);
	}

	public String getBoardMode() {
		return this.boardMode;
	}

	public BreakType getBreakType() {
		return this.breakType;
	}

	public CeremonyType getCeremonyType() {
		return this.ceremonyType;
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

	public FOPState getFopState() {
		return this.fopState;
	}

	public String getSessionDescription() {
		return this.sessionDescription;
	}

	public String getSessionInfo() {
		return this.sessionInfo;
	}

	public String getSessionName() {
		return this.sessionName;
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

	public boolean isShowLiftRanks() {
		return this.showLiftRanks;
	}

	public boolean isShowSinclair() {
		return this.showSinclair;
	}

	public boolean isShowSinclairRank() {
		return this.showSinclairRank;
	}

	public boolean isShowTotalRank() {
		return this.showTotalRank;
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
		String prop = null;
		if (this.teamFlags == null) {
			this.teamFlags = URLUtils.checkFlags();
		}

		if (this.teamFlags && !team.isBlank()) {
			prop = Team.getImgTag(team, "");
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
		if (!isActive()) return;
		uiLog(e);
		Athlete a = e.getAthlete();
		setHidden(false);
		// doBreak(e);
		doUpdate(a, e);
		pushUpdate(e);
	}

	@Subscribe
	public void slaveBreakPause(UIEvent.BreakPaused e) {
		if (!isActive()) return;
		uiLog(e);
		pushTimer(e);
	}

	@Subscribe
	public void slaveBreakSet(UIEvent.BreakSetTime e) {
		if (!isActive()) return;
		uiLog(e);
		pushTimer(e);
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		if (!isActive()) return;
		uiLog(e);
		setHidden(false);
		doBreak(e);
		pushUpdate(e);
		pushTimer(e);
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		if (!isActive()) return;
		uiLog(e);
		setHidden(false);
		doCeremony(e);
		doBreak(e);
		pushUpdate(e);
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		if (!isActive()) return;
		uiLog(e);
		setHidden(false);
		doCeremony(e);
		pushUpdate(e);
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		if (!isActive()) return;
		uiLog(e);
		setDecisionLight1(e.ref1);
		setDecisionLight2(e.ref2);
		setDecisionLight3(e.ref3);
		setDecisionLightsVisible(true);
		setDown(false);
		pushDecision(DecisionEventType.FULL_DECISION, e);
	}

	@Subscribe
	public void slaveIniitialDecision(IniitialDecision e) {
		if (!isActive()) return;
		uiLog(e);
		setDecisionLight1(e.ref1);
		setDecisionLight2(e.ref2);
		setDecisionLight3(e.ref3);
		setDecisionLightsVisible(false);
		setDown(false);
		pushDecision(DecisionEventType.INIITIAL_DECISION, e);
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		if (!isActive()) return;
		uiLog(e);
		setDecisionLight1(null);
		setDecisionLight2(null);
		setDecisionLight3(null);
		setDecisionLightsVisible(false);
		setDown(false);
		pushDecision(DecisionEventType.RESET, e);
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		if (!isActive()) return;
		uiLog(e);
		setDecisionLightsVisible(false);
		setDown(true);
		pushDecision(DecisionEventType.DOWN_SIGNAL, e);
	}

	@Subscribe
	public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
		if (!isActive()) return;
		uiLog(e);
		computeCurrentGroup(getFop().getGroup());
		pushUpdate(e);
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		if (!isActive()) return;
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
		doBreak(e, g);
	}
	
	// Database is now embedded in the update message for both HTTP and WebSocket
}	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		if (!isActive()) return;
		uiLog(e);
		Athlete a = e.getAthlete();
		computeCurrentGroup(e.getAthlete() != null ? e.getAthlete().getGroup() : null);
		doUpdate(a, e);
		pushUpdate(e);
	}

	@Subscribe
	public void slaveSetTime(UIEvent.SetTime e) {
		if (!isActive()) return;
		uiLog(e);
		setHidden(false);
		pushTimer(e);
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		if (!isActive()) return;
		uiLog(e);
		setHidden(false);
		pushUpdate(e);
	}

	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
		if (!isActive()) return;
		uiLog(e);
		setHidden(false);
		pushTimer(e);
	}

	@Subscribe
	public void slaveStopTime(UIEvent.StopTime e) {
		if (!isActive()) return;
		uiLog(e);
		setHidden(false);
		pushTimer(e);
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		if (!isActive()) return;
		logger.debug("slaveSwitchGroup: switching to group {} state {} {}",
		        e.getGroup() != null ? e.getGroup().getName() : "null",
		        e.getState(),
		        LoggerUtils.whereFrom());
		computeCurrentGroup(e.getGroup());
		if (e.getState() == null) {
			setHidden(true);
			pushUpdate(e);
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
	pushUpdate(e);
	
	// Database is now embedded in the update message for both HTTP and WebSocket
}	@Override
	public void unregister() {
		// we do nothing. We now have exactly one EventForwarder per name
		// and we reuse it if we ever recreate the field of play

		// logger.info("unregistering event forwarder for platform {}",getForwardedFopName());
		// this.postBus.unregister(this);
		// this.setFop(null);
		// OwlcmsFactory.getFOPByName(getForwardedFopName()).setEventForwarder(null);
		// eventForwarderByName.remove(getForwardedFopName());
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

	void setAttemptNumber(Integer attemptNumber) {
		this.attemptNumber = attemptNumber;
	}

	void setFullName(String fullName) {
		this.fullName = fullName;
	}

	void setSessionDescription(String description) {
		this.sessionDescription = description;
	}

	void setSessionName(String name) {
		this.sessionName = name;
	}

	void setHidden(boolean b) {
		this.hidden = b;
	}

	void setLiftsDone(String formattedDone) {
		this.liftsDone = formattedDone;
	}

	void setLiftType(String liftType) {
		this.liftType = liftType;
	}

	void setLiftTypeKey(String liftTypeKey) {
		this.liftTypeKey = liftTypeKey;
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

	@Subscribe
	void slaveJuryNotification(UIEvent.JuryNotification e) {
		// logger.debug("===== slaveJuryNotification {} new record = {} waitForAnnouncer = {} trace\n{}",
		// e.getDeliberationEventType(),
		// e.getNewRecord(),
		// e.isWaitForAnnouncer(),
		// e.getTrace());
		uiLog(e);
		pushDecision(e);
	}

	private void computeCurrentGroup(Group g) {
		// Group group = getFop().getGroup();
		List<Athlete> displayOrder = getFop().getDisplayOrder();
		// int liftsDone = AthleteSorter.countLiftsDone(displayOrder);

		// setSessionName(group != null ? group.getName() : "");
		// setSessionInfo(computeSecondLine(getFop().getCurAthlete(), group != null ? group.getName() : null));
		// setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));

		if (displayOrder != null && displayOrder.size() > 0) {
			List<Athlete> liftingOrder = getFop().getLiftingOrder();
			if (liftingOrder != null && liftingOrder.size() > 0) {
				Athlete currentAthlete = liftingOrder.get(0);
				updateSessionInfo(computeLiftType(currentAthlete));
				setLiftTypeKey(computeLiftTypeKey(currentAthlete));
				setLiftType(computeLiftType(currentAthlete));

			}
		} else {
			updateSessionInfo(null);

		}

		// String sinclair = Competition.getCurrent().isSinclair() ? "sinclair" : "nosinclair";
		// String ranks = Competition.getCurrent().isSnatchCJTotalMedals() ? "ranks" : "noranks";
		// setNoLiftRanks(sinclair + " " + ranks);

		// getElement().setProperty("showTotal", true);
		// getElement().setProperty("showBest", true);
		setShowLiftRanks(Competition.getCurrent().isSnatchCJTotalMedals() && !Competition.getCurrent().isSinclair());
		setShowTotalRank(!Competition.getCurrent().isSinclair());
		setShowSinclair(Competition.getCurrent().isSinclair() || Competition.getCurrent().isDisplayScores());
		setShowSinclairRank(Competition.getCurrent().isSinclair() || Competition.getCurrent().isDisplayScoreRanks());

		computeLeaders();
		JsonValue recordsJson = this.fop.getRecordsJson();
		// logger.debug("setting records {}",recordsJson.toJson());
		setRecords(recordsJson);
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
			setCategoryName(curAthlete.getCategory().getDisplayName());
			this.groupLeaders = this.fop.getLeaders();
			if (this.groupLeaders == null || this.groupLeaders.isEmpty()) {
				setLeadersV2(null);
				return;
			}
			int size = this.groupLeaders.size();
			if (size > 16) {
				setLeadersV2(null);
			} else if (this.groupLeaders.size() > 0) {
				setLeadersV2(AthleteExporter.exportLeaderEntries(this.groupLeaders));
			} else {
				// no one has totaled, so we show the snatch leaders
				if (!this.fop.isCjStarted()) {
					if (this.groupLeaders.size() > 0) {
						setLeadersV2(AthleteExporter.exportLeaderEntries(this.groupLeaders));
					} else {
						// nothing to show
						setLeadersV2(null);
					}
				} else {
					// nothing to show
					setLeadersV2(null);
				}
			}
		}

	}

	private String computeLiftType(Athlete a) {
		String liftTypeKey = computeLiftTypeKey(a);
		return liftTypeKey == null ? null : Translator.translate(liftTypeKey);
	}

	private String computeLiftTypeKey(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		return a.getAttemptsDone() >= 3 ? "Clean_and_Jerk" : "Snatch";
	}

	private String computeSecondLine(Athlete a, String groupName) {
		if (a == null) {
			return ("");
		}
		return Translator.translate("Scoreboard.GroupLiftType", groupName,
		        (a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		                : Translator.translate("Snatch")));
	}

	private synchronized Map<String, String> createDecision(UIEvent event, DecisionEventType det) {
		updateState();
		Map<String, String> sb = new LinkedHashMap<>();
		mapPut(sb, "decisionEventType", det == DecisionEventType.INIITIAL_DECISION ? "iniitialDecision" : det.toString());
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "mode", getBoardMode());

		// competition state
		mapPut(sb, "competitionName", Competition.getCurrent().getCompetitionName());
		mapPut(sb, "fop", getFop().getName());
		setMapFopState(sb);
		mapPut(sb, "break", String.valueOf(isBreak())); // current athlete & attempt
		mapPut(sb, "fullName", this.fullName);
		mapPut(sb, "attemptNumber", this.attemptNumber != null ? this.attemptNumber.toString() : null); // 1..3
		mapPut(sb, "liftTypeKey", this.liftTypeKey);
		mapPut(sb, "d1", getDecisionLight1() != null ? getDecisionLight1().toString() : null);
		mapPut(sb, "d2", getDecisionLight2() != null ? getDecisionLight2().toString() : null);
		mapPut(sb, "d3", getDecisionLight3() != null ? getDecisionLight3().toString() : null);
		if (event instanceof UIEvent.Decision) {
			UIEvent.Decision decisionEvent = (UIEvent.Decision) event;
			mapPut(sb, "decision", decisionEvent.decision != null ? decisionEvent.decision.toString() : null);
		}
		mapPut(sb, "decisionsVisible", Boolean.toString(isDecisionLightsVisible()));
		mapPut(sb, "down", Boolean.toString(isDown()));
		
		// Add singleReferee flag if this is a Decision event
		if (event instanceof UIEvent.Decision) {
			UIEvent.Decision decisionEvent = (UIEvent.Decision) event;
			mapPut(sb, "singleReferee", Boolean.toString(decisionEvent.isSingleReferee()));
		}

		populateRecordInfoStrings(sb);
		// dumpMap("createDecision", event.getTrace(), sb);
		return sb;
	}

	private synchronized Map<String, String> createJuryEvent(UIEvent.JuryNotification e) {
		updateState();
		Map<String, String> sb = new LinkedHashMap<>();

		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "mode", getBoardMode());

		// competition state
		mapPut(sb, "fop", getFop().getName());
		setMapFopState(sb);
		mapPut(sb, "break", String.valueOf(isBreak()));

		// deliberation on a lift
		JuryDeliberationEventType det = e.getDeliberationEventType();
		if (det == JuryDeliberationEventType.BAD_LIFT
		        || det == JuryDeliberationEventType.GOOD_LIFT) {
			mapPut(sb, "decisionEventType", "JURY_DECISION");
			mapPut(sb, "juryDecision", det.name());
			mapPut(sb, "juryReversal", e.getReversal().toString());
			mapPut(sb, "athleteFull", e.getAthlete().getFullName());
			mapPut(sb, "athleteAbbreviated", e.getAthlete().getAbbreviatedName());
			mapPut(sb, "waitForAnnouncer", Boolean.toString(e.isWaitForAnnouncer()));
			mapPut(sb, "recordKind", getFop().getLastChallengedRecords().isEmpty() ? "none" : (e.getNewRecord() ? "new" : "denied"));
			if (e.getActualLift() != null) {
				mapPut(sb, "actualLift", Integer.toString(e.getActualLift()));
			}
		} else if (det == JuryDeliberationEventType.START_DELIBERATION
		        || det == JuryDeliberationEventType.END_DELIBERATION
		        || det == JuryDeliberationEventType.CHALLENGE
		        || det == JuryDeliberationEventType.END_CHALLENGE) {
			mapPut(sb, "decisionEventType", det.name());
		}

		dumpMap("*** createJuryDecision", e.getTrace(), sb);
		return sb;
	}

	private void populateRecordInfo(Map<String, Object> sb) {
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
			Object convertedRecords = convertJsonValue(this.records);
			if (convertedRecords != null) {
				sb.put("records", convertedRecords);
			} else {
				mapPut(sb, "records", null);
			}
		} else {
			mapPut(sb, "records", null);
		}
	}

	@SuppressWarnings("unchecked")
	private void populateRecordInfoStrings(Map<String, String> sb) {
		if (this.records != null) {
			if (this.fop.getNewRecords() != null && !this.fop.getNewRecords().isEmpty()) {
				sb.put("recordKind", "new");
				sb.put("recordMessage", Translator.translate("Scoreboard.NewRecord"));
			} else if (this.fop.getChallengedRecords() != null && !this.fop.getChallengedRecords().isEmpty()) {
				sb.put("recordKind", "attempt");
				sb.put("recordMessage", Translator.translate("Scoreboard.RecordAttempt"));
			} else {
				sb.put("recordKind", "none");
			}
			sb.put("records", this.records.toJson());
		} else {
			sb.remove("recordKind");
			sb.remove("recordMessage");
			sb.remove("records");
		}
	}

	private synchronized Map<String, String> createTimer(UIEvent e) {
		updateState();
		Map<String, String> sb = new LinkedHashMap<>();
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "fopName", getFop().getName());
		setMapFopState(sb);
		mapPut(sb, "mode", getBoardMode());

		// current athlete info
		mapPut(sb, "fullName", this.fullName);
		mapPut(sb, "attemptNumber", this.attemptNumber != null ? this.attemptNumber.toString() : null); // 1..3
		mapPut(sb, "liftTypeKey", this.liftTypeKey);
		mapPut(sb, "serverLocalTime", LocalTime.now().toString());

		Integer breakMillisRemaining = null;
		Integer athleteMillisRemaining = null;
		Long breakStartTimeMillis = null;
		Long athleteStartTimeMillis = null;
		Boolean indefiniteBreak = null;
		String timerEventType = e.getClass().getSimpleName();

		String breakTimerEventType = "";
		String athleteTimerEventType = "";
		if (e instanceof SetTime) {
			SetTime st = (SetTime) e;
			// relative time to be displayed. start time will send the absolute value
			athleteTimerEventType = timerEventType;
			athleteStartTimeMillis = null;
			athleteMillisRemaining = st.getTimeRemaining();
		} else if (e instanceof StartTime) {
			athleteTimerEventType = timerEventType;
			StartTime st = (StartTime) e;
			athleteStartTimeMillis = System.currentTimeMillis();
			athleteMillisRemaining = st.getTimeRemaining();
		} else if (e instanceof StopTime) {
			athleteTimerEventType = timerEventType;
			StopTime st = (StopTime) e;
			athleteStartTimeMillis = System.currentTimeMillis();
			athleteMillisRemaining = st.getTimeRemaining();
		} else if (e instanceof BreakSetTime) {
			breakTimerEventType = timerEventType;
			BreakSetTime bst = (BreakSetTime) e;
			indefiniteBreak = bst.isIndefinite();
			if (bst.getEnd() != null) {
				breakMillisRemaining = (int) LocalDateTime.now().until(bst.getEnd(), ChronoUnit.MILLIS);
			} else {
				breakMillisRemaining = bst.isIndefinite() ? null : bst.getTimeRemaining();
			}
		} else if (e instanceof BreakStarted) {
			breakTimerEventType = timerEventType;
			BreakStarted bst = (BreakStarted) e;
			breakStartTimeMillis = System.currentTimeMillis();
			breakMillisRemaining = bst.isIndefinite() ? null : bst.getTimeRemaining();
			indefiniteBreak = bst.isIndefinite();
		} else if (e instanceof BreakPaused) {
			breakTimerEventType = timerEventType;
			BreakPaused bst = (BreakPaused) e;
			breakMillisRemaining = bst.isIndefinite() ? null : bst.getTimeRemaining();
			indefiniteBreak = bst.isIndefinite();
		} else if (e instanceof BreakDone) {
			breakMillisRemaining = -1;
		}

		if (e instanceof StartTime || e instanceof SetTime || e instanceof StopTime) {
			mapPut(sb, "athleteTimerEventType", athleteTimerEventType);
			athleteMillisRemaining = athleteMillisRemaining != null ? athleteMillisRemaining : 0;
			mapPut(sb, "athleteStartTimeMillis",
			        athleteStartTimeMillis != null ? Long.toString(athleteStartTimeMillis) : null);
			mapPut(sb, "athleteMillisRemaining",
			        athleteMillisRemaining != null ? athleteMillisRemaining.toString() : null);
		} else {
			mapPut(sb, "breakTimerEventType", breakTimerEventType);
			mapPut(sb, "break", String.valueOf(isBreak()));
			mapPut(sb, "breakType",
			        ((getFopState() == FOPState.BREAK) && (getFop().getBreakType() != null))
			                ? getFop().getBreakType().toString()
			                : null);
			mapPut(sb, "ceremonyType",
			        getCeremonyType() != null ? getCeremonyType().name() : null);
			if (e instanceof BreakStarted || e instanceof BreakSetTime) {
				mapPut(sb, "indefiniteBreak", indefiniteBreak != null ? Boolean.toString(indefiniteBreak) : null);
			}
			if (e instanceof BreakStarted) {
				breakStartTimeMillis = breakStartTimeMillis != null ? breakStartTimeMillis : System.currentTimeMillis();
				breakMillisRemaining = breakMillisRemaining != null ? breakMillisRemaining : 0;
				mapPut(sb, "breakStartTimeMillis", Long.toString(breakStartTimeMillis));
				mapPut(sb, "breakMillisRemaining",
				        breakMillisRemaining != null ? breakMillisRemaining.toString() : null);
				// logger.debug("breaktimer {} {} {} end {} indefinite {}", sb.get("timerEventType"), sb.get("break"),
				// sb.get("breakType"),
				// breakStartTimeMillis + breakMillisRemaining, sb.get("indefiniteBreak"));
			}
		}
		// dumpMap("createTimer", e.getTrace(), sb);
		return sb;
	}

	private synchronized Map<String, Object> createUpdate(UIEvent event) {
		updateState();
		Map<String, Object> sb = new LinkedHashMap<>();

		// include timer info for synchronization on restart/refresh
		// the update will override common fields
		// Decision info is NOT included - decisions are sent via separate DECISION messages
		// and should not be mixed with lifting order updates
		if (getLastTimerMap() != null) {
			sb.putAll(getLastTimerMap());
		}
		recomputeRemainingTimes(sb);

		mapPut(sb, "uiEvent", event.getClass().getSimpleName());
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
		setMapFopStateObject(sb);

		String isBreak = String.valueOf(isBreak());
		mapPut(sb, "break", isBreak);
		BreakType breakType = getFop().getBreakType();
		String bts = ((getFop().getState() == FOPState.BREAK) && (breakType != null))
		        ? getFop().getBreakType().toString()
		        : null;
		mapPut(sb, "breakType", bts);
		IBreakTimer breakTimer = getFop().getBreakTimer();
		mapPut(sb, "breakIsIndefinite", Boolean.toString(breakTimer != null ? breakTimer.isIndefinite() : false));

		CeremonyType ceremonyType = getFop().getCeremonyType();
		String cts = ceremonyType != null ? ceremonyType.name() : null;
		mapPut(sb, "ceremonyEventType", this.ceremonyEventType);
		mapPut(sb, "ceremonyType", cts);
		mapPut(sb, "ceremonySession", this.ceremonySession != null ? this.ceremonySession.getName() : null);
		mapPut(sb, "ceremonyCategory", this.ceremonyCategory != null ? this.ceremonyCategory.getDisplayName() : null);
		mapPut(sb, "ceremonyType", ceremonyType != null ? ceremonyType.name() : null);
		mapPut(sb, "ceremonyCategory", this.ceremonyCategory != null ? this.ceremonyCategory.getDisplayName() : null);
		mapPut(sb, "ceremonyAgeGroup", this.ceremonyAgeGroup != null ? this.ceremonyAgeGroup.getName() : null);
		mapPut(sb, "ceremonyChampionship", this.ceremonyChampionship != null ? this.ceremonyChampionship.getName() : null);

		// current athlete & attempt
		mapPut(sb, "startNumber", this.startNumber != null ? this.startNumber.toString() : null);
		mapPut(sb, "categoryName", this.categoryName);
		mapPut(sb, "fullName", this.fullName);
		mapPut(sb, "teamName", this.teamName);
		mapPut(sb, "attempt", this.attempt);
		mapPut(sb, "attemptNumber", this.attemptNumber != null ? this.attemptNumber.toString() : null); // 1..3
		mapPut(sb, "weight", this.weight != null ? this.weight.toString() : null);
		mapPut(sb, "timeAllowed", this.timeAllowed != null ? this.timeAllowed.toString() : null);

		// current session
		mapPut(sb, "sessionName", getSessionName());
		mapPut(sb, "sessionDescription", getSessionDescription());
		mapPut(sb, "sessionInfo", getSessionInfo());
		mapPut(sb, "liftTypeKey", this.liftTypeKey);
		mapPut(sb, "liftsDone", getLiftsDone());

		// bottom tables
		mapPut(sb, "showLiftRanks", Boolean.toString(isShowLiftRanks()));
		mapPut(sb, "showTotalRank", Boolean.toString(isShowTotalRank()));
		mapPut(sb, "showSinclair", Boolean.toString(isShowSinclair()));
		mapPut(sb, "showSinclairRank", Boolean.toString(isShowSinclairRank()));

		// Always use V2 format: send athlete order (with spacers) plus full session athlete data
		List<Athlete> displayOrder = getFop().getDisplayOrder();
		List<Athlete> liftingOrder = getFop().getLiftingOrder();
		// Do not emit legacy groupAthletesV2 / liftingOrderAthletesV2 payloads
		
		if (displayOrder != null && !displayOrder.isEmpty()) {
			sb.put("startOrderKeys", getAthleteKeyEntries(displayOrder, true));
			
			// Export enriched session athlete data (athlete DTO + displayInfo)
			// Pass liftingOrder to compute classname ("current blink", "next", "")
			List<Map<String, Object>> sessionAthletes = AthleteExporter.exportSessionAthletes(displayOrder, liftingOrder);
			sb.put("sessionAthletes", sessionAthletes);
		}
		if (liftingOrder != null && !liftingOrder.isEmpty()) {
			sb.put("liftingOrderKeys", getAthleteKeyEntries(liftingOrder, false));
			
			// Add current, next, previous athlete keys
			if (liftingOrder.size() > 0) {
				Athlete current = liftingOrder.get(0);
				mapPut(sb, "currentAthleteKey", current.getKey());
			}
			if (liftingOrder.size() > 1) {
				Athlete next = liftingOrder.get(1);
				mapPut(sb, "nextAthleteKey", next.getKey());
			}
			// Previous athlete is the one who just lifted (if available)
			if (liftingOrder.size() > 0) {
				Athlete current = liftingOrder.get(0);
				// Check if there's a recently completed athlete before current
				int currentIndex = displayOrder.indexOf(current);
				if (currentIndex > 0) {
					Athlete previous = displayOrder.get(currentIndex - 1);
					// Only include if they've actually lifted
					if (previous.getAttemptsDone() > 0) {
						mapPut(sb, "previousAthleteKey", previous.getKey());
					}
				}
			}
		}
		
		if (this.leadersSessionData != null) {
			sb.put("leaders", this.leadersSessionData);
		} else if (this.leaders != null) {
			Object convertedLeaders = convertJsonValue(this.leaders);
			if (convertedLeaders != null) {
				// Ensure each leader entry has a top-level athleteKey for readability
				if (convertedLeaders instanceof List) {
					List<?> rawList = (List<?>) convertedLeaders;
					List<Object> out = new ArrayList<>();
					for (Object item : rawList) {
						if (item instanceof Map) {
							@SuppressWarnings("unchecked")
							Map<String, Object> m = (Map<String, Object>) item;
							// Determine a sensible athlete key: prefer 'athleteKey', then 'id', then nested athlete.key/id
							String keyVal = null;
							if (m.containsKey("athleteKey") && m.get("athleteKey") != null) {
								keyVal = String.valueOf(m.get("athleteKey"));
							} else if (m.containsKey("id") && m.get("id") != null) {
								keyVal = String.valueOf(m.get("id"));
							} else if (m.containsKey("athlete") && m.get("athlete") instanceof Map) {
								@SuppressWarnings("unchecked")
								Map<String, Object> nested = (Map<String, Object>) m.get("athlete");
								if (nested.containsKey("key") && nested.get("key") != null) {
									keyVal = String.valueOf(nested.get("key"));
								} else if (nested.containsKey("id") && nested.get("id") != null) {
									keyVal = String.valueOf(nested.get("id"));
								}
							}
							if (keyVal != null) {
								Map<String, Object> newMap = new LinkedHashMap<>();
								newMap.put("athleteKey", keyVal);
								newMap.putAll(m);
								out.add(newMap);
								continue;
							}
						}
						// Fallback: pass item through unchanged
						out.add(item);
					}
					sb.put("leaders", out);
				} else {
					sb.put("leaders", convertedLeaders);
				}
			}
		}
		populateRecordInfo(sb);

		// Translations are now sent separately via 428 callback (sendTranslations)
		// No longer included in regular updates for efficiency
		
		mapPut(sb, "hidden", String.valueOf(this.hidden));
		mapPut(sb, "wideTeamNames", String.valueOf(this.wideTeamNames));
		mapPut(sb, "sinclairMeet", Boolean.toString(Competition.getCurrent().isSinclair()));

		setBoardMode(computeBoardModeName(this.fop.getState(), this.fop.getBreakType(), this.fop.getCeremonyType()));
		mapPut(sb, "mode", getBoardMode());

		// Database is now sent proactively on connection open and after platform updates.
		// No longer embed empty database structure in update messages - it confuses the tracker
		// into thinking a binary database_zip will follow.
		if (event instanceof UIEvent.SwitchGroup || event instanceof UIEvent.GroupDone) {
			CompetitionDataExport export = ForwarderPayloadBuilder.exportCompetitionData(getFop());
			if (export != null) {
				// Only send checksum so tracker can verify it has current data
				mapPut(sb, "databaseChecksum", export.checksum());
			}
		}

		// dumpMap("createUpdate " + System.identityHashCode(sb), event.getTrace(), sb);

		return sb;
	}
	
	/**
	 * Build an order list consisting of athlete keys with spacer entries inserted
	 * whenever category boundaries (start order) or lift-phase transitions
	 * (lifting order) occur. This mirrors the EventForwarder behavior so
	 * downstream consumers can rely on identical spacing.
	 */
	private List<Object> getAthleteKeyEntries(List<Athlete> athletes, boolean startOrder) {
		if (athletes == null || athletes.isEmpty()) {
			return Collections.emptyList();
		}

		List<Object> entries = new ArrayList<>();
		Category prevCat = null;
		Athlete prevAth = null;

		for (Athlete athlete : athletes) {
			if (startOrder) {
				Category curCat = athlete.getCategory();
				if (curCat != null && !curCat.sameAs(prevCat)) {
					entries.add(createSpacerEntry());
					prevCat = curCat;
				}
			} else {
				if (prevAth == null ||
				        (athlete.getActuallyAttemptedLifts() >= 3 && prevAth.getActuallyAttemptedLifts() < 3)) {
					entries.add(createSpacerEntry());
				}
				prevAth = athlete;
			}

			Integer key = athlete.getKey();
			if (key != null) {
				entries.add(key);
			}
		}

		return entries;
	}
	
	// Session athlete export and attempt info building delegated to AthleteExporter

	private void doBreak(UIEvent e, Group g) {
		OwlcmsSession.withFop(fop -> {
			createUpdate(e);
			if (getFopState() != FOPState.BREAK) {
				logger.debug("### done not break");
			} else {
				logger.debug("### done but break");
				setFullName(groupResults(g));
				setTeamName("");
				setAttempt("");
				setHidden(false);
			}
			pushUpdate(e);
		});

	}

	private void doCeremony(CeremonyDone e) {
		setCeremonyEventType("ceremonyDone");
		setCeremonyType(null);
		setCeremonySession(null);
		setCeremonyCategory(null);
		setCeremonyAgeGroup(null);
		setCeremonyChampionship(null);
	}

	private void doDone(UIEvent e, Group g) {
		logger.debug("forwarding doDone {}", g == null ? null : g.getName());
		computeCurrentGroup(g);
		if (g == null) {
			setHidden(true);
		} else {
			setFullName(g.getName());
			setSessionName("");
			setSessionDescription("");
			setSessionInfo("");
			setLiftsDone("");
		}
		pushUpdate(e);
	}

	private void doPost(String url, String updateKey, Map<String, ?> parameters) {
		HttpPost post = new HttpPost(url);
		// add request parameters or form parameters
		List<NameValuePair> urlParameters = new ArrayList<>();
		parameters.entrySet().stream()
		        .forEach((e) -> {
			        String value = convertParameterValue(e.getValue());
			        if (value != null) {
				        urlParameters.add(new BasicNameValuePair(e.getKey(), value));
			        }
		        });

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
								sendConfig(url, updateKey);
								nbTries++;
							} else if (nbTries == 0 && statusCode != null && statusCode == 428) {
								logger.debug("{}hub returned 428 - sending database {} {} {}",
								        FieldOfPlay.getLoggingName(getFop()), url,
								        statusLine,
								        LoggerUtils.whereFrom(1));
								sendDatabase(url, updateKey);
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
				setAttemptNumber(a.getAttemptNumber());
				setWeight(a.getNextAttemptRequestedWeight());
				if (e instanceof UIEvent.LiftingOrderUpdated) {
					setTimeAllowed(((LiftingOrderUpdated) e).getTimeAllowed());
				}
				String sessionName = getFop().getGroup() != null ? getFop().getGroup().getName() : null;
				String computedName = sessionName != null
				        ? computeSecondLine(a, sessionName)
				        : "";
				setSessionInfo(computedName);
			}
		} else {
			if (!leaveTopAlone) {
				logger.trace("ef doUpdate doDone");
				Group g = (a != null ? a.getGroup() : null);
				doDone(e, g);
			}
		}
		// override top scoreboard line when we are in a break
		doBreak(e);
	}

	@SuppressWarnings("unused")
	private void dumpMap(String string, String string2, Map<String, String> map) {
		// if (StartupUtils.isDebugSetting()) {
		Level level = logger.getLevel();
		try {
			logger.setLevel(Level.TRACE);
			logger.trace("=== {}\n{}", string, string2);
			for (Entry<String, String> m : map.entrySet()) {
				if (m.getKey() == "updateKey") {
					logger.trace(" {} = {}", m.getKey(), m.getValue() != null ? "masked " + m.getValue().length() : "masked null value");
				} else {
					logger.trace(" {} = {}", m.getKey(), m.getValue());
				}
			}
		} finally {
			logger.setLevel(level);
		}
		// }
	}

	private String formatAttempt(Integer attemptNo) {
		String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
		return translate;
	}

	private String formatInt(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			// invited lifter, not eligible.
			return Translator.translate("Results.Extra/Invited");
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
		category = curCat != null ? curCat.getNameWithAgeGroup() : "";
		ja.put("id", a.getId() != null ? a.getId().toString() : "");
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		ja.put("gender", a.getGender() != null ? a.getGender().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		Integer lotNumber = a.getLotNumber();
		ja.put("lotNumber", (lotNumber != null ? lotNumber.toString() : ""));
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

		if (a.getGroup() != null) {
			ja.put("group", a.getGroup().getName());
		}
		ja.put("subCategory", a.getSubCategory());
		boolean notDone = a.getAttemptsDone() < 6;
		String blink = (notDone ? " blink" : "");
		if (notDone) {
			ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
		}
		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");
		ja.put("membership", a.getMembership() != null ? a.getMembership() : "");
		setTeamFlag(a, ja);
	}

	/**
	 * @param startOrder     use starting order or lifting order ?
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	@SuppressWarnings("unused")
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

	// Removed unused getAthletesV2Json — session athlete export now handled by exportSessionAthletes

	private Map<String, Object> createSpacerEntry() {
		Map<String, Object> spacer = new LinkedHashMap<>();
		spacer.put("isSpacer", true);
		return spacer;
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
	@Override
	public FieldOfPlay getFop() {
		return this.fop;
	}

	@SuppressWarnings("unused")
	private String getForwardedFopName() {
		return this.forwardedFopName;
	}

	private Map<String, String> getLastDecisionMap() {
		return this.lastDecisionMap;
	}

	private Map<String, String> getLastTimerMap() {
		return this.lastTimerMap;
	}

	private String groupResults(Group g) {
		return Translator.translate("Group_number_results", g.toString());
	}

	private boolean isBreak() {
		return getFop().getState() == FOPState.BREAK;
	}

	private void mapPut(Map<String, Object> wr, String key, Object value) {
		if (value == null) {
			return;
		}
		wr.put(key, value);
	}

	private void mapPut(Map<String, String> wr, String key, String value) {
		if (value == null) {
			return;
		}
		wr.put(key, value);
	}

	private String convertParameterValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof CompetitionData) {
			return ((CompetitionData) value).exportDataAsString();
		}
		if (value instanceof JsonValue) {
			return convertParameterValue(convertJsonValue((JsonValue) value));
		}
		if (value instanceof Map || value instanceof Iterable || value.getClass().isArray()) {
			try {
				return JSON_MAPPER.writeValueAsString(value);
			} catch (JsonProcessingException e) {
				logger.debug("{}could not serialize parameter value {}", FieldOfPlay.getLoggingName(getFop()),
				        LoggerUtils.exceptionMessage(e));
			}
		}
		return value.toString();
	}

	// Competition data export and hash computation delegated to ForwarderPayloadBuilder

	private Object convertJsonValue(JsonValue value) {
		if (value == null || value.getType() == JsonType.NULL) {
			return null;
		}
		switch (value.getType()) {
			case STRING:
				return value.asString();
			case NUMBER:
				double number = value.asNumber();
				if (Math.rint(number) == number) {
					long longVal = (long) number;
					if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
						return (int) longVal;
					}
					return longVal;
				}
				return number;
			case BOOLEAN:
				return value.asBoolean();
			case ARRAY:
				JsonArray array = (JsonArray) value;
				List<Object> list = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					list.add(convertJsonValue(array.get(i)));
				}
				return list;
			case OBJECT:
				JsonObject object = (JsonObject) value;
				Map<String, Object> map = new LinkedHashMap<>();
				for (String key : object.keys()) {
					map.put(key, convertJsonValue(object.get(key)));
				}
				return map;
			default:
				return null;
		}
	}

	private synchronized void pushDecision(DecisionEventType det, UIEvent e) {
		Config current = Config.getCurrent();
		String decisionUrl = current.getParamDecisionUrl();
		String videoUrl = current.getParamVideoDataDecisionUrl();

		setLastDecisionMap(createDecision(e, det));
		if (decisionUrl == null && videoUrl == null) {
			return;
		}
		sendPost(videoUrl, current.getParamVideoDataKey(), getLastDecisionMap(), "decision");
		sendPost(decisionUrl, current.getUpdatekey(), getLastDecisionMap(), "decision");
	}

	private void pushDecision(JuryNotification e) {
		Config current = Config.getCurrent();
		String decisionUrl = current.getParamDecisionUrl();
		String videoUrl = current.getParamVideoDataDecisionUrl();
		setLastDecisionMap(createJuryEvent(e));

		if (decisionUrl == null && videoUrl == null) {
			return;
		}

		sendPost(videoUrl, current.getParamVideoDataKey(), getLastDecisionMap(), "decision");
		sendPost(decisionUrl, current.getUpdatekey(), getLastDecisionMap(), "decision");
	}

	private synchronized void pushTimer(UIEvent e) {
		Config current = Config.getCurrent();
		String timerUrl = current.getParamTimerUrl();
		String videoUrl = current.getParamVideoDataTimerUrl();

		setLastTimerMap(createTimer(e));
		if (timerUrl == null && videoUrl == null) {
			return;
		}

		sendPost(videoUrl, current.getParamVideoDataKey(), getLastTimerMap(), "timer");
		sendPost(timerUrl, current.getUpdatekey(), getLastTimerMap(), "timer");
	}

	/**
	 * push updates every n seconds in case publicresults is restarted. The individual instances for each viewer need to debounce because they will get
	 * duplicate events.
	 *
	 */
	@SuppressWarnings("deprecation")
	private synchronized void pushUpdate(UIEvent e2) {
		if (this.NO_KEEPALIVE) {
			pushUpdateDoIt(e2);
			return;
		}
		if (this.keepaliveThread != null) {
			this.keepaliveThread.interrupt();
		}
		this.keepaliveThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					pushUpdateDoIt(e2);
					Thread.sleep(KEEPALIVE_INTERVAL);
				} catch (InterruptedException e) {
					logger.debug("thread {} interrupted", Thread.currentThread().getId());
					break;
				}
			}
		});
		this.keepaliveThread.start();
	}

	private void pushUpdateDoIt(UIEvent e2) {
		setBoardMode(computeBoardModeName(this.fop.getState(), this.fop.getBreakType(),
		        this.fop.getCeremonyType()));
		this.lastUpdate = createUpdate(e2);

		Config current = Config.getCurrent();
		String updateUrl = current.getParamUpdateUrl();
		String videoUrl = Config.getCurrent().getParamVideoDataUpdateUrl();
		logger.debug("pushUpdateDoIt: updateUrl={} videoUrl={} {}", updateUrl, videoUrl, LoggerUtils.whereFrom());
		if (updateUrl == null && videoUrl == null) {
			return;
		}

		sendPost(videoUrl, current.getParamVideoDataKey(), this.lastUpdate, "update");
		sendPost(updateUrl, current.getParamUpdateKey(), this.lastUpdate, "update");
	}

	private void recomputeRemainingTimes(Map<String, Object> sb) {
		try {
			if (sb == null) return;
			if (getFop() == null) return;

			long now = System.currentTimeMillis();

			// Helper: parse long/int/boolean from payload values (strings or numbers)
			java.util.function.Function<Object, Long> toLong = (o) -> {
				if (o == null) return null;
				if (o instanceof Number) return ((Number) o).longValue();
				try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return null; }
			};
			java.util.function.Function<Object, Integer> toInt = (o) -> {
				if (o == null) return null;
				if (o instanceof Number) return ((Number) o).intValue();
				try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
			};
			java.util.function.Function<Object, Boolean> toBool = (o) -> {
				if (o == null) return Boolean.FALSE;
				if (o instanceof Boolean) return (Boolean) o;
				String s = String.valueOf(o);
				return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "1".equals(s);
			};

			// ATHLETE: If the update payload already contains athlete start+remaining, recompute remaining from that, else fallback to live timer
			try {
				Long existingAthStart = toLong.apply(sb.get("athleteStartTimeMillis"));
				Integer existingAthRemaining = toInt.apply(sb.get("athleteMillisRemaining"));
				String existingAthEvent = sb.get("athleteTimerEventType") != null ? sb.get("athleteTimerEventType").toString() : null;
				if (existingAthStart != null && existingAthRemaining != null && existingAthStart > 0
						&& "StartTime".equals(existingAthEvent)) {
					long elapsed = now - existingAthStart;
					int recomputed = Math.max(0, existingAthRemaining - (int) elapsed);
					sb.put("athleteMillisRemaining", recomputed);
					// keep event type as StartTime and preserve start timestamp
				} else {
					// fallback to live timer
					try {
						IProxyTimer athleteTimer = getFop().getAthleteTimer();
						if (athleteTimer != null) {
							int athleteRemaining = athleteTimer.liveTimeRemaining();
							sb.put("athleteMillisRemaining", athleteRemaining);
							sb.put("athleteTimerEventType", athleteTimer.isRunning() ? "StartTime" : "SetTime");
							if (athleteTimer.isRunning()) {
								sb.put("athleteStartTimeMillis", now);
							}
						}
					} catch (Throwable t) {
						logger.debug("{}could not recompute athlete timer: {}", FieldOfPlay.getLoggingName(getFop()), LoggerUtils.exceptionMessage(t));
					}
				}
			} catch (Throwable t) {
				logger.debug("{}athlete timer recomputation failed: {}", FieldOfPlay.getLoggingName(getFop()), LoggerUtils.exceptionMessage(t));
			}

			// BREAK: Prefer values present in the update payload (e.g., from lastTimerMap) and recompute remaining based on stored start time; otherwise fall back to live timer
			try {
				Long existingBreakStart = toLong.apply(sb.get("breakStartTimeMillis"));
				Integer existingBreakRemaining = toInt.apply(sb.get("breakMillisRemaining"));
				Boolean existingIndefinite = toBool.apply(sb.get("indefiniteBreak"));
				String existingBreakEvent = sb.get("breakTimerEventType") != null ? sb.get("breakTimerEventType").toString() : null;

				if (existingBreakStart != null && existingBreakStart > 0 && existingBreakRemaining != null
						&& "BreakStarted".equals(existingBreakEvent)) {
					long elapsed = now - existingBreakStart;
					int recomputed = Math.max(0, existingBreakRemaining - (int) elapsed);
					if (existingIndefinite != null && existingIndefinite) {
						sb.put("indefiniteBreak", Boolean.TRUE);
						sb.put("breakMillisRemaining", null);
						sb.put("breakIsIndefinite", Boolean.toString(true));
					} else {
						sb.put("indefiniteBreak", Boolean.FALSE);
						sb.put("breakMillisRemaining", recomputed);
						sb.put("breakIsIndefinite", Boolean.toString(false));
					}
					// preserve breakStartTimeMillis as provided
				} else if (existingIndefinite != null && existingIndefinite) {
					// explicit indefinite marker in payload
					sb.put("indefiniteBreak", Boolean.TRUE);
					sb.put("breakMillisRemaining", null);
					sb.put("breakIsIndefinite", Boolean.toString(true));
				} else {
					// fallback to live break timer
					try {
						IBreakTimer breakTimer = getFop().getBreakTimer();
						if (breakTimer != null) {
							boolean indefinite = breakTimer.isIndefinite();
							int breakRemaining = breakTimer.liveTimeRemaining();
							if (indefinite) {
								sb.put("indefiniteBreak", Boolean.TRUE);
								sb.put("breakMillisRemaining", null);
							} else {
								sb.put("indefiniteBreak", Boolean.FALSE);
								sb.put("breakMillisRemaining", breakRemaining);
							}
							sb.put("breakIsIndefinite", Boolean.toString(indefinite));
							if (breakTimer.isRunning()) {
								sb.put("breakStartTimeMillis", now);
								sb.put("breakTimerEventType", "BreakStarted");
							}
						}
					} catch (Throwable t) {
						logger.debug("{}could not recompute break timer: {}", FieldOfPlay.getLoggingName(getFop()), LoggerUtils.exceptionMessage(t));
					}
				}
			} catch (Throwable t) {
				logger.debug("{}break timer recomputation failed: {}", FieldOfPlay.getLoggingName(getFop()), LoggerUtils.exceptionMessage(t));
			}

		} catch (Throwable ex) {
			logger.debug("{}recomputeRemainingTimes failed: {}", FieldOfPlay.getLoggingName(getFop()), LoggerUtils.exceptionMessage(ex));
		}

	}

	private void sendConfig(String url, String updateKey) {
		if (url == null || updateKey == null) {
			logger.error("cannot send config info, url or updateKey is null");
			return;
		}
		Config current = Config.getCurrent();
		String destination = url.replaceAll("/update", "") + "/config";
		// wait for previous send to finish.
		// no consequences sending it multiple times in a row -- we have no idea why it
		// is being requested again.
		synchronized (current) {
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
	/**
	 * Send competition database via WebSocket using compressed binary ZIP format.
	 * SINGLE DATABASE SENDING ROUTINE - always uses binary ZIP for efficiency (70-80% reduction).
	 * 
	 * Called in these scenarios:
	 * 1. Initial connection (response to 428 Precondition Required)
	 * 2. Platform configuration changes (plate/weight updates)
	 * 3. Session switches (group selection)
	 * 
	 * ZIP format handles large competition databases efficiently:
	 * - Typical database: ~700KB JSON → ~200KB ZIP
	 * - Scales better than JSON for competitions with 100+ athletes
	 * 
	 * @param url the WebSocket URL to send the database to
	 * @param updateKey the update key for validation (optional, not used for WebSocket)
	 */
	public void sendDatabase(String url, String updateKey) {
		logger.info("{}sendDatabase called for url: {}", FieldOfPlay.getLoggingName(getFop()), url);

		if (url == null) {
			logger.error("cannot send database, url is null");
			return;
		}

		CompetitionDataExport export = ForwarderPayloadBuilder.exportCompetitionData(getFop());
		if (export == null) {
			logger.warn("{}unable to build competition data for database", FieldOfPlay.getLoggingName(getFop()));
			return;
		}

		// Only support WebSocket for database transmission
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				// Compress database using binary ZIP format
				// Use export.structure() (parsed object) not export.json() (string)
				// to avoid double-encoding issues
				byte[] databaseZipBytes = DatabaseZipHelper.createDatabaseZipBytes(
					export.structure()
				);

				if (databaseZipBytes.length > 0) {
					boolean sent = sender.sendBinary("database_zip", databaseZipBytes);
					if (sent) {
						// Log compression ratio for reference
						String jsonDatabase = export.json();
						double ratio = 100.0 * (1.0 - (double) databaseZipBytes.length / jsonDatabase.getBytes().length);
						logger.info(
							"{}sent database ZIP via WebSocket to {} ({} bytes, from {}, {:.1f}% reduction)",
							FieldOfPlay.getLoggingName(getFop()), url, databaseZipBytes.length,
							jsonDatabase.getBytes().length, ratio
						);
					} else {
						logger.debug(
							"{}could not send database ZIP via WebSocket to {} (socket not ready)",
							FieldOfPlay.getLoggingName(getFop()), url
						);
					}
				} else {
					logger.error("{}failed to create database ZIP for {}",
						FieldOfPlay.getLoggingName(getFop()), url);
				}
			}
			return;
		}

		logger.debug("{}database transmission requires WebSocket ({})",
			FieldOfPlay.getLoggingName(getFop()), url);
	}

	/**
	 * Send competition database via WebSocket using compressed binary ZIP format.
	 * Convenience method without update key parameter.
	 * 
	 * @param url the WebSocket URL to send the database to
	 */
	public void sendDatabase(String url) {
		sendDatabase(url, null);
	}

	/**
	 * Send competition database via WebSocket using compressed binary ZIP format.
	 * PRIMARY static method to broadcast database to all remote trackers/scoreboards.
	 * 
	 * @param updateKey the update key for validation (optional, not used for WebSocket binary format)
	 */
	public static void sendDatabaseToAll(String updateKey) {
		String[] urls = new String[2];
		urls[0] = Config.getCurrent().getParamPublicResultsURL();
		urls[1] = Config.getCurrent().getParamVideoDataURL();

		for (String url : urls) {
			if (url != null && !url.trim().isEmpty() &&
				(url.startsWith("ws://") || url.startsWith("wss://"))) {
				for (WebSocketEventForwarder forwarder : eventForwarderByName.values()) {
					forwarder.sendDatabase(url, updateKey);
				}
			}
		}
	}

	/**
	 * Send competition database via WebSocket using compressed binary ZIP format.
	 * Static convenience method without update key.
	 */
	public static void sendDatabaseToAll() {
		sendDatabaseToAll(null);
	}

	/**
	 * Send flags directory as a zipped archive via WebSocket.
	 * Called when the remote system requests flags (via 428 response with "flags" in missing array).
	 * Uses binary transmission for maximum efficiency.
	 * 
	 * @param url the WebSocket URL to send flags to
	 */
	private void sendFlags(String url) {
		logger.debug("{}sendFlags called for url: {}", FieldOfPlay.getLoggingName(getFop()), url);

		if (url == null) {
			logger.error("cannot send flags, url is null");
			return;
		}

		if (!FlagsZipHelper.hasFlagsAvailable()) {
			logger.debug("{}flags not available, cannot send", FieldOfPlay.getLoggingName(getFop()));
			return;
		}

		// Check if URL is WebSocket
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			// Send via WebSocket as binary data (most efficient)
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				byte[] flagsZipBytes = FlagsZipHelper.createFlagsZipBytes();
				if (flagsZipBytes.length > 0) {
					boolean sent = sender.sendBinary("flags_zip", flagsZipBytes);
					if (sent) {
						logger.debug("{}sent flags_zip ZIP via WebSocket binary to {} ({} bytes)",
						        FieldOfPlay.getLoggingName(getFop()), url, flagsZipBytes.length);
					} else {
						logger.debug("{}could not send flags_zip ZIP via WebSocket to {} (socket not ready)",
						        FieldOfPlay.getLoggingName(getFop()), url);
					}
				} else {
					logger.debug("{}failed to create flags ZIP for {}", FieldOfPlay.getLoggingName(getFop()), url);
				}
			}
			return;
		}

		// HTTP endpoints for flags are not typically used, but log a warning
		logger.debug("{}HTTP endpoint for flags not implemented ({})", FieldOfPlay.getLoggingName(getFop()), url);
	}

	/**
	 * Send all translations for all 26 locales as a zipped JSON archive via WebSocket.
	 * Called when the remote system requests translations (via 428 response with "translations" in missing array).
	 * Sends complete translation maps with regional variant merging (e.g., fr-CA gets all fr keys + 10 overrides).
	 * Uses binary transmission for maximum efficiency.
	 * 
	 * @param url the WebSocket URL to send translations to
	 */
	private void sendTranslations(String url) {
		logger.debug("{}sendTranslations called for url: {}", FieldOfPlay.getLoggingName(getFop()), url);

		if (url == null) {
			logger.error("cannot send translations, url is null");
			return;
		}

		if (!TranslationsZipHelper.hasTranslationsAvailable()) {
			logger.debug("{}translations not available, cannot send", FieldOfPlay.getLoggingName(getFop()));
			return;
		}

		// Check if URL is WebSocket
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			// Send via WebSocket as binary data (most efficient)
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				byte[] translationsZipBytes = TranslationsZipHelper.createTranslationsZipBytes();
				if (translationsZipBytes.length > 0) {
					boolean sent = sender.sendBinary("translations_zip", translationsZipBytes);
					if (sent) {
						logger.debug("{}sent translations ZIP via WebSocket binary to {} ({} bytes with all 26 locales)",
						        FieldOfPlay.getLoggingName(getFop()), url, translationsZipBytes.length);
					} else {
						logger.debug("{}could not send translations ZIP via WebSocket to {} (socket not ready)",
						        FieldOfPlay.getLoggingName(getFop()), url);
					}
				} else {
					logger.debug("{}failed to create translations ZIP for {}", FieldOfPlay.getLoggingName(getFop()), url);
				}
			}
			return;
		}

		// HTTP endpoints for translations are not typically used, but log a warning
		logger.debug("{}HTTP endpoint for translations not implemented ({})", FieldOfPlay.getLoggingName(getFop()), url);
	}

	private void sendPost(String url, String updateKey, Map<String, ?> parameters, String messageType) {
		if (url == null) {
			return;
		}

		// Check if URL is WebSocket (ws:// or wss://)
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			sendWebSocket(url, messageType, parameters);
			return;
		}

		Integer previousDebounceHash = this.debouncingHash.get(url);
		Long previousDebounceMillis = this.debouncingMillis.get(url);
		long deltaMillis = System.currentTimeMillis() - (previousDebounceMillis != null ? previousDebounceMillis : 0);
		Integer hashCode = ForwarderPayloadBuilder.computeParametersHash(parameters);

		// debounce, sometimes several identical updates in a rapid succession
		// identical updates are ok after 1 sec.
		if (hashCode != previousDebounceHash || (deltaMillis > 1000)) {
			new Thread(() -> doPost(url, updateKey, parameters)).start();

			this.debouncingHash.put(url, hashCode);
			this.debouncingMillis.put(url, System.currentTimeMillis());
		}

	}

	/**
	 * Send data via WebSocket connection with message type
	 */
	private void sendWebSocket(String url, String messageType, Map<String, ?> parameters) {
		// Determine which URL this is (publicResults or videoData)
		Config current = Config.getCurrent();
		String publicResultsUrl = current.getParamUpdateUrl();
		String videoDataUrl = current.getParamVideoDataUpdateUrl();
		
		boolean isPublicResults = url != null && url.equals(publicResultsUrl);
		boolean isVideoData = url != null && url.equals(videoDataUrl);
		
		// Check if URL has changed and close old connection if needed
		if (isPublicResults && this.currentPublicResultsUrl != null && !this.currentPublicResultsUrl.equals(url)) {
			logger.info("{}PublicResults URL changed from {} to {}, closing old connection",
			        FieldOfPlay.getLoggingName(getFop()), this.currentPublicResultsUrl, url);
			WebSocketEventSender.closeSender(this.currentPublicResultsUrl);
			this.currentPublicResultsUrl = url;
		} else if (isPublicResults && this.currentPublicResultsUrl == null) {
			this.currentPublicResultsUrl = url;
		}
		
		if (isVideoData && this.currentVideoDataUrl != null && !this.currentVideoDataUrl.equals(url)) {
			logger.info("{}VideoData URL changed from {} to {}, closing old connection",
			        FieldOfPlay.getLoggingName(getFop()), this.currentVideoDataUrl, url);
			WebSocketEventSender.closeSender(this.currentVideoDataUrl);
			this.currentVideoDataUrl = url;
		} else if (isVideoData && this.currentVideoDataUrl == null) {
			this.currentVideoDataUrl = url;
		}
		
		Integer previousDebounceHash = this.debouncingHash.get(url);
		Long previousDebounceMillis = this.debouncingMillis.get(url);
		long deltaMillis = System.currentTimeMillis() - (previousDebounceMillis != null ? previousDebounceMillis : 0);
		Integer hashCode = ForwarderPayloadBuilder.computeParametersHash(parameters);

		// debounce, sometimes several identical updates in a rapid succession
		// identical updates are ok after 1 sec.
		if (hashCode != previousDebounceHash || (deltaMillis > 1000)) {
			// Create sender with onOpen callback to send database/translations/flags
			// Callback fires on EVERY connection (including reconnects) because tracker may have restarted
			Runnable onOpenCallback = () -> {
				Config currentCallback = Config.getCurrent();
				String updateKey = currentCallback.getParamUpdateKey();
				if (updateKey == null) {
					updateKey = currentCallback.getParamVideoDataKey();
				}
				logger.info("{}WebSocket connection established to {}, proactively sending database and translations",
				        FieldOfPlay.getLoggingName(getFop()), url);
				
				// Send database (binary ZIP format)
				sendDatabase(url, updateKey);
				
				// Send translations
				sendTranslations(url);
				
				// Send flags if available
				sendFlags(url);
			};
			
			// Pass URL supplier and callback so they're set BEFORE connection starts
			WebSocketEventSender sender;
			if (isPublicResults) {
				sender = WebSocketEventSender.getOrCreate(url, () -> Config.getCurrent().getParamUpdateUrl(), onOpenCallback);
			} else if (isVideoData) {
				sender = WebSocketEventSender.getOrCreate(url, () -> Config.getCurrent().getParamVideoDataUpdateUrl(), onOpenCallback);
			} else {
				sender = WebSocketEventSender.getOrCreate(url, () -> url, onOpenCallback);
			}
			
			if (sender != null) {
				
				// Set up callback for 428 status response (database requested) - fallback for reconnection
				sender.setMissingDataCallback("database", () -> {
					Config currentCallback = Config.getCurrent();
					String updateKey = currentCallback.getParamUpdateKey();
					if (updateKey == null) {
						updateKey = currentCallback.getParamVideoDataKey();
					}
				// Send database (binary ZIP format)
				sendDatabase(url, updateKey);
			});
				// Set up callback for 428 status response (translations requested) - fallback for reconnection
				sender.setMissingDataCallback("translations", () -> {
					sendTranslations(url);
				});

				sender.send(messageType, parameters);
			}

			this.debouncingHash.put(url, hashCode);
			this.debouncingMillis.put(url, System.currentTimeMillis());
		}
	}

	private void setBreakType(BreakType breakType) {
		this.breakType = breakType;
	}

	private void setCategoryName(String name) {
		this.categoryName = name;
	}

	private void setCeremonyAgeGroup(AgeGroup ceremonyAgeGroup) {
		this.ceremonyAgeGroup = ceremonyAgeGroup;
	}

	private void setCeremonyCategory(Category ceremonyCategory) {
		this.ceremonyCategory = ceremonyCategory;
	}

	private void setCeremonyChampionship(Championship ceremonyChampionship) {
		this.ceremonyChampionship = ceremonyChampionship;
	}

	private void setCeremonyEventType(String ceremonyEventType) {
		this.ceremonyEventType = ceremonyEventType;
	}

	private void setCeremonySession(Group ceremonySession) {
		this.ceremonySession = ceremonySession;
	}

	private void setCeremonyType(CeremonyType ceremonyType) {
		this.ceremonyType = ceremonyType;
	}

	/**
	 * @param fop the fop to set
	 */
	private void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	private void setFopState(FOPState state) {
		this.fopState = state;
	}

	private void setForwardedFopName(String name) {
		this.forwardedFopName = name;
	}

	private void setSessionInfo(String computeSecondLine) {
		this.sessionInfo = computeSecondLine;
	}

	private void setLastDecisionMap(Map<String, String> lastDecisionMap) {
		this.lastDecisionMap = lastDecisionMap;
	}

	private void setLastTimerMap(Map<String, String> lastTimerMap) {
		this.lastTimerMap = lastTimerMap;
	}

	@SuppressWarnings("unused")
	private void setLeaders(JsonValue athletesJson) {
		this.leaders = athletesJson;
		this.leadersSessionData = null;
	}

	private void setLeadersV2(List<Map<String, Object>> leaders) {
		if (leaders != null && leaders.isEmpty()) {
			leaders = null;
		}
		this.leadersSessionData = leaders;
		// Always null legacy payload when using the V2 structure
		this.leaders = null;
	}



	private void setMapFopState(Map<String, String> sb) {
		FOPState state = getFopState();
		String value = state != null ? state.toString() : FOPState.INACTIVE.name();
		mapPut(sb, "fopState", value);
	}

	private void setMapFopStateObject(Map<String, Object> sb) {
		Map<String, String> temp = new LinkedHashMap<>();
		setMapFopState(temp);
		temp.forEach((k, v) -> sb.put(k, v));
	}

	private void setRecords(JsonValue recordsJson) {
		this.records = recordsJson;
	}

	private void setShowLiftRanks(boolean b) {
		this.showLiftRanks = b;
	}

	private void setShowSinclair(boolean b) {
		this.showSinclair = b;
	}

	private void setShowSinclairRank(boolean b) {
		this.showSinclairRank = b;
	}

	private void setShowTotalRank(boolean b) {
		this.showTotalRank = b;
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

	private void updateSessionInfo(String pLiftType) {
		Group lCurGroup = this.fop.getGroup();
		int lNbLiftsDone = AthleteSorter.countLiftsDone(this.fop.getDisplayOrder());

		String lGroupDescription = lCurGroup != null ? lCurGroup.getDescription() : null;
		String lSessionInfo = lGroupDescription;
		String lLiftsDone = "";
		if (lCurGroup != null && lCurGroup.isDone()) {
			lLiftsDone = "";
		} else if (lCurGroup != null && pLiftType != null) {
			String name = lGroupDescription != null ? lGroupDescription : lCurGroup.getName();
			lSessionInfo = lGroupDescription == null ? Translator.translate("Scoreboard.GroupLiftType", name, pLiftType)
			        : Translator.translate("Scoreboard.DescriptionLiftTypeFormat", lGroupDescription, pLiftType);
			lLiftsDone = Translator.translate("Scoreboard.AttemptsDone", lNbLiftsDone);
		}
		setSessionName(lCurGroup != null ? lCurGroup.getName() : "");
		setSessionDescription(lGroupDescription != null ? lGroupDescription : "");
		setSessionInfo(lSessionInfo);
		setLiftsDone(lLiftsDone);
	}

	private synchronized void updateState() {
		FOPState state = this.fop.getState();
		BreakType breakType = this.fop.getBreakType();
		CeremonyType ceremonyType = this.fop.getCeremonyType();
		setBoardMode(computeBoardModeName(state, breakType, ceremonyType));
		setFopState(state);
		setBreakType(breakType);
		setCeremonyType(ceremonyType);
	}

	public String getLiftType() {
		return liftType;
	}

	/**
	 * Register startup data callbacks for WebSocket connections.
	 * Delegates to WebSocketSender.registerStartupDataCallbacks().
	 * 
	 * @param videoUrl the video data WebSocket URL (if configured)
	 * @param updateUrl the public results WebSocket URL (if configured)
	 */
	public static void registerStartupDataCallbacks(String videoUrl, String updateUrl) {
		WebSocketSender.registerStartupDataCallbacks(videoUrl, updateUrl);
	}
}
