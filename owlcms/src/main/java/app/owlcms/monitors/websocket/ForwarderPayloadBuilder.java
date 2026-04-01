/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors.websocket;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.DecisionEventType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakDone;
import app.owlcms.uievents.UIEvent.BreakPaused;
import app.owlcms.uievents.UIEvent.BreakSetTime;
import app.owlcms.uievents.UIEvent.BreakStarted;
import app.owlcms.uievents.UIEvent.JuryNotification;
import app.owlcms.uievents.UIEvent.SetTime;
import app.owlcms.uievents.UIEvent.StartTime;
import app.owlcms.uievents.UIEvent.StopTime;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Builds payload maps for WebSocket messages (update, timer, decision).
 * Responsible for creating the data structures that will be serialized to JSON.
 */
public class ForwarderPayloadBuilder {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ForwarderPayloadBuilder.class);
	private static final ObjectMapper JSON_MAPPER = createObjectMapper();

	private static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

	/**
	 * Competition data export result holder.
	 */
	public static final class CompetitionDataExport {
		private final Object structure;
		private final String json;
		private final String checksum;

		public CompetitionDataExport(Object structure, String json, String checksum) {
			this.structure = structure;
			this.json = json;
			this.checksum = checksum;
		}

		public Object structure() {
			return this.structure;
		}

		public String json() {
			return this.json;
		}

		public String checksum() {
			return this.checksum;
		}
	}

	/**
	 * Create timer message payload.
	 */
	public static Map<String, String> createTimer(UIEvent e, FieldOfPlay fop, String boardMode,
			String fullName, Integer attemptNumber, String liftTypeKey, FOPState fopState,
			BreakType breakType, CeremonyType ceremonyType) {
		
		Map<String, String> sb = new LinkedHashMap<>();
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "fopName", fop.getName());
		mapPut(sb, "fopState", fopState != null ? fopState.toString() : FOPState.INACTIVE.name());
		mapPut(sb, "mode", boardMode);

		// current athlete info
		mapPut(sb, "fullName", fullName);
		mapPut(sb, "attemptNumber", attemptNumber != null ? attemptNumber.toString() : null);
		mapPut(sb, "liftTypeKey", liftTypeKey);
		mapPut(sb, "serverLocalTime", LocalTime.now().toString());

		Integer breakMillisRemaining = null;
		Integer athleteMillisRemaining = null;
		Integer athleteTimeAllowed = null;
		Integer athleteInitialWarningMillis = null;
		Integer athleteFinalWarningMillis = null;
		Long breakStartTimeMillis = null;
		Long athleteStartTimeMillis = null;
		Boolean indefiniteBreak = null;
		String timerEventType = e.getClass().getSimpleName();

		String breakTimerEventType = "";
		String athleteTimerEventType = "";
		
		boolean isBreak = fop.getState() == FOPState.BREAK;
		
		if (e instanceof SetTime) {
			SetTime st = (SetTime) e;
			athleteTimerEventType = timerEventType;
			athleteStartTimeMillis = null;
			athleteMillisRemaining = st.getTimeRemaining();
			athleteTimeAllowed = resolveAthleteTimeAllowed(fop, athleteMillisRemaining);
		} else if (e instanceof StartTime) {
			athleteTimerEventType = timerEventType;
			StartTime st = (StartTime) e;
			athleteStartTimeMillis = System.currentTimeMillis();
			athleteMillisRemaining = st.getTimeRemaining();
			athleteTimeAllowed = resolveAthleteTimeAllowed(fop, athleteMillisRemaining);
		} else if (e instanceof StopTime) {
			athleteTimerEventType = timerEventType;
			StopTime st = (StopTime) e;
			athleteStartTimeMillis = System.currentTimeMillis();
			athleteMillisRemaining = st.getTimeRemaining();
			athleteTimeAllowed = resolveAthleteTimeAllowed(fop, athleteMillisRemaining);
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
			athleteInitialWarningMillis = resolveAthleteInitialWarningMillis(athleteTimeAllowed);
			athleteFinalWarningMillis = resolveAthleteFinalWarningMillis(athleteTimeAllowed);
			mapPut(sb, "athleteTimerEventType", athleteTimerEventType);
			athleteMillisRemaining = athleteMillisRemaining != null ? athleteMillisRemaining : 0;
			mapPut(sb, "timeAllowed", athleteTimeAllowed != null ? athleteTimeAllowed.toString() : null);
			mapPut(sb, "athleteStartTimeMillis",
			        athleteStartTimeMillis != null ? Long.toString(athleteStartTimeMillis) : null);
			mapPut(sb, "athleteMillisRemaining",
			        athleteMillisRemaining != null ? athleteMillisRemaining.toString() : null);
			mapPut(sb, "athleteInitialWarningMillis",
			        athleteInitialWarningMillis != null ? athleteInitialWarningMillis.toString() : null);
			mapPut(sb, "athleteFinalWarningMillis",
			        athleteFinalWarningMillis != null ? athleteFinalWarningMillis.toString() : null);
		} else {
			mapPut(sb, "breakTimerEventType", breakTimerEventType);
			mapPut(sb, "break", String.valueOf(isBreak));
			mapPut(sb, "breakType",
			        ((fopState == FOPState.BREAK) && (breakType != null))
			                ? breakType.toString()
			                : null);
			mapPut(sb, "ceremonyType", ceremonyType != null ? ceremonyType.name() : null);
			if (e instanceof BreakStarted || e instanceof BreakSetTime) {
				mapPut(sb, "indefiniteBreak", indefiniteBreak != null ? Boolean.toString(indefiniteBreak) : null);
			}
			if (e instanceof BreakStarted) {
				breakStartTimeMillis = breakStartTimeMillis != null ? breakStartTimeMillis : System.currentTimeMillis();
				breakMillisRemaining = breakMillisRemaining != null ? breakMillisRemaining : 0;
				mapPut(sb, "breakStartTimeMillis", Long.toString(breakStartTimeMillis));
				mapPut(sb, "breakMillisRemaining",
				        breakMillisRemaining != null ? breakMillisRemaining.toString() : null);
			}
		}
		return sb;
	}

	private static Integer resolveAthleteTimeAllowed(FieldOfPlay fop, Integer athleteMillisRemaining) {
		int currentAllowed = fop.getClockOwnerInitialTimeAllowed();
		if (currentAllowed > 0) {
			return currentAllowed;
		}
		return athleteMillisRemaining;
	}

	private static Integer resolveAthleteInitialWarningMillis(Integer athleteTimeAllowed) {
		if (athleteTimeAllowed == null || athleteTimeAllowed < 1) {
			return null;
		}
		if (athleteTimeAllowed == Competition.athleteTimerOneMinute) {
			return -1;
		}
		return Competition.athleteTimerInitialWarning <= athleteTimeAllowed ? Competition.athleteTimerInitialWarning : -1;
	}

	private static Integer resolveAthleteFinalWarningMillis(Integer athleteTimeAllowed) {
		if (athleteTimeAllowed == null || athleteTimeAllowed < 1) {
			return null;
		}
		return Competition.athleteTimerFinalWarning <= athleteTimeAllowed ? Competition.athleteTimerFinalWarning : -1;
	}

	/**
	 * Create decision message payload.
	 */
	public static Map<String, String> createDecision(UIEvent event, DecisionEventType det, FieldOfPlay fop,
			String boardMode, String fullName, Integer attemptNumber, String liftTypeKey,
			Boolean decisionLight1, Boolean decisionLight2, Boolean decisionLight3,
			boolean decisionLightsVisible, boolean down, FOPState fopState, JsonValue records) {
		
		Map<String, String> sb = new LinkedHashMap<>();
		mapPut(sb, "decisionEventType", det.toString());
		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "mode", boardMode);

		// competition state
		mapPut(sb, "competitionName", Competition.getCurrent().getCompetitionName());
		mapPut(sb, "fop", fop.getName());
		mapPut(sb, "fopState", fopState != null ? fopState.toString() : FOPState.INACTIVE.name());
		
		boolean isBreak = fop.getState() == FOPState.BREAK;
		mapPut(sb, "break", String.valueOf(isBreak));
		
		// current athlete & attempt
		mapPut(sb, "fullName", fullName);
		mapPut(sb, "attemptNumber", attemptNumber != null ? attemptNumber.toString() : null);
		mapPut(sb, "liftTypeKey", liftTypeKey);
		mapPut(sb, "d1", decisionLight1 != null ? decisionLight1.toString() : null);
		mapPut(sb, "d2", decisionLight2 != null ? decisionLight2.toString() : null);
		mapPut(sb, "d3", decisionLight3 != null ? decisionLight3.toString() : null);
		mapPut(sb, "decisionsVisible", Boolean.toString(decisionLightsVisible));
		mapPut(sb, "down", Boolean.toString(down));
		
		// Add decision metadata flags
		if (event instanceof UIEvent.Decision) {
			UIEvent.Decision decisionEvent = (UIEvent.Decision) event;
			mapPut(sb, "singleReferee", Boolean.toString(decisionEvent.isSingleLight())); // backward compat
			mapPut(sb, "singleLight", Boolean.toString(decisionEvent.isSingleLight()));
			if (decisionEvent.getTimingPolicy() != null) {
				mapPut(sb, "timingPolicy", decisionEvent.getTimingPolicy().name());
			}
			if (decisionEvent.getInputKind() != null) {
				mapPut(sb, "inputKind", decisionEvent.getInputKind().name());
			}
		} else if (event instanceof UIEvent.InitialDecision) {
			UIEvent.InitialDecision initialDecisionEvent = (UIEvent.InitialDecision) event;
			mapPut(sb, "singleReferee", Boolean.toString(initialDecisionEvent.isSingleLight())); // backward compat
			mapPut(sb, "singleLight", Boolean.toString(initialDecisionEvent.isSingleLight()));
			if (initialDecisionEvent.getTimingPolicy() != null) {
				mapPut(sb, "timingPolicy", initialDecisionEvent.getTimingPolicy().name());
			}
			if (initialDecisionEvent.getInputKind() != null) {
				mapPut(sb, "inputKind", initialDecisionEvent.getInputKind().name());
			}
		}

		populateRecordInfoStrings(sb, records, fop);
		return sb;
	}

	/**
	 * Create jury event payload.
	 */
	public static Map<String, String> createJuryEvent(JuryNotification e, FieldOfPlay fop,
			String boardMode, FOPState fopState) {
		
		Map<String, String> sb = new LinkedHashMap<>();

		mapPut(sb, "updateKey", Config.getCurrent().getParamUpdateKey());
		mapPut(sb, "mode", boardMode);

		// competition state
		mapPut(sb, "fop", fop.getName());
		mapPut(sb, "fopState", fopState != null ? fopState.toString() : FOPState.INACTIVE.name());
		
		boolean isBreak = fop.getState() == FOPState.BREAK;
		mapPut(sb, "break", String.valueOf(isBreak));

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
			mapPut(sb, "recordKind", fop.getLastChallengedRecords().isEmpty() ? "none" : (e.getNewRecord() ? "new" : "denied"));
			if (e.getActualLift() != null) {
				mapPut(sb, "actualLift", Integer.toString(e.getActualLift()));
			}
		} else if (det == JuryDeliberationEventType.START_DELIBERATION
		        || det == JuryDeliberationEventType.END_DELIBERATION
		        || det == JuryDeliberationEventType.CHALLENGE
		        || det == JuryDeliberationEventType.END_CHALLENGE) {
			mapPut(sb, "decisionEventType", det.name());
		}

		return sb;
	}

	/**
	 * Export competition data as a structured object with checksum.
	 */
	public static CompetitionDataExport exportCompetitionData(FieldOfPlay fop) {
		try {
			CompetitionDataV2 competitionData = new CompetitionDataV2();
			competitionData.fromDatabase();
			InputStream inputStream = competitionData.exportData();
			
			try (inputStream) {
				byte[] dataBytes = inputStream.readAllBytes();
				Object structure = JSON_MAPPER.readValue(dataBytes, Object.class);
				String json = new String(dataBytes, StandardCharsets.UTF_8);
				String checksum = computeChecksum(dataBytes, fop);
				return new CompetitionDataExport(structure, json, checksum);
			}
		} catch (Exception e) {
			if (fop != null) {
				logger.error("{}failed to export competition data: {}", FieldOfPlay.getLoggingName(fop),
				        LoggerUtils.exceptionMessage(e));
			} else {
				logger.error("failed to export competition data: {}", LoggerUtils.exceptionMessage(e));
			}
			return null;
		}
	}

	/**
	 * Export competition data without FOP context (for startup).
	 */
	public static CompetitionDataExport exportCompetitionDataStatic() {
		return exportCompetitionData(null);
	}

	/**
	 * Convert JsonValue to standard Java types for serialization.
	 */
	public static Object convertJsonValue(JsonValue value) {
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

	/**
	 * Convert parameter value to string for HTTP transmission.
	 */
	public static String convertParameterValue(Object value, FieldOfPlay fop) {
		if (value == null) {
			return null;
		}
		if (value instanceof JsonValue) {
			return convertParameterValue(convertJsonValue((JsonValue) value), fop);
		}
		if (value instanceof Map || value instanceof Iterable || value.getClass().isArray()) {
			try {
				return JSON_MAPPER.writeValueAsString(value);
			} catch (JsonProcessingException e) {
				logger.debug("{}could not serialize parameter value {}", 
				        fop != null ? FieldOfPlay.getLoggingName(fop) : "",
				        LoggerUtils.exceptionMessage(e));
			}
		}
		return value.toString();
	}

	/**
	 * Compute hash of parameters for debouncing.
	 */
	public static int computeParametersHash(Map<String, ?> parameters) {
		if (parameters == null) {
			return 0;
		}
		Map<String, Object> sanitized = new LinkedHashMap<>();
		parameters.forEach((key, value) -> {
			if ("database".equals(key)) {
				return;
			}
			sanitized.put(key, value);
		});
		return sanitized.hashCode();
	}

	/**
	 * Build translation map from Translator.
	 */
	public static JsonObject buildTranslationMap() {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		return translations;
	}

	/**
	 * Populate record info in a Map<String, Object> payload.
	 */
	public static void populateRecordInfo(Map<String, Object> sb, JsonValue records, FieldOfPlay fop) {
		if (records != null) {
			if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
				sb.put("recordKind", "new");
				sb.put("recordMessage", Translator.translate("Scoreboard.NewRecord"));
			} else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
				sb.put("recordKind", "attempt");
				sb.put("recordMessage", Translator.translate("Scoreboard.RecordAttempt"));
			} else {
				sb.put("recordKind", "none");
			}
			Object convertedRecords = convertJsonValue(records);
			if (convertedRecords != null) {
				sb.put("records", convertedRecords);
			} else {
				sb.put("records", null);
			}
		} else {
			sb.put("records", null);
		}
	}

	// Helper methods

	private static void mapPut(Map<String, String> wr, String key, String value) {
		if (value == null) {
			return;
		}
		wr.put(key, value);
	}

	private static void populateRecordInfoStrings(Map<String, String> sb, JsonValue records, FieldOfPlay fop) {
		if (records != null) {
			if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
				sb.put("recordKind", "new");
				sb.put("recordMessage", Translator.translate("Scoreboard.NewRecord"));
			} else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
				sb.put("recordKind", "attempt");
				sb.put("recordMessage", Translator.translate("Scoreboard.RecordAttempt"));
			} else {
				sb.put("recordKind", "none");
			}
			sb.put("records", records.toJson());
		} else {
			sb.remove("recordKind");
			sb.remove("recordMessage");
			sb.remove("records");
		}
	}

	private static String computeChecksum(byte[] dataBytes, FieldOfPlay fop) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(dataBytes);
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			if (fop != null) {
				logger.debug("{}failed to compute competition data checksum: {}", FieldOfPlay.getLoggingName(fop),
				        LoggerUtils.exceptionMessage(e));
			} else {
				logger.debug("failed to compute competition data checksum: {}", LoggerUtils.exceptionMessage(e));
			}
			return null;
		}
	}

	/**
	 * Get the shared ObjectMapper instance.
	 */
	public static ObjectMapper getObjectMapper() {
		return JSON_MAPPER;
	}
}
