/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum FeatureSwitch {
	CENTER_ANNOUNCER_NOTIFICATIONS("centerAnnouncerNotifications", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	NO_LIVE_LIGHTS("noLiveLights", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	LOCAL_TEMPLATES_ONLY("localTemplatesOnly", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	SHORT_SCOREBOARD_NAMES("shortScoreboardNames", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	CUSTOM_TEAM_NAME("customTeamName", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	MEDALISTS_AS_LEADERS("medalistsAsLeaders", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	DONT_FIX_NAMES("dontFixNames", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	DISPLAY_BEST_SCORE("displayBestScore", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
	SHOW_DECLARATIONS_TO_ANNOUNCER("showDeclarationsToAnnouncer", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),

	ATHLETE_CARD_ENTRY_TOTAL("athleteCardEntryTotal", FeatureSwitchSection.GENERAL_OPTIONS, "AthleteCardEntryTotal"),
	EXPLICIT_TEAMS("explicitTeams", FeatureSwitchSection.GENERAL_OPTIONS),
	BEST_MATCH_CATEGORIES("bestMatchCategories", FeatureSwitchSection.GENERAL_OPTIONS),
	BW_CLASS_THEN_AGE_GROUP("bwClassThenAgeGroup", FeatureSwitchSection.GENERAL_OPTIONS),
	FORCE_ALL_GROUP_RECORDS("forceAllGroupRecords", FeatureSwitchSection.GENERAL_OPTIONS),
	FORCE_ALL_FEDERATION_RECORDS("forceAllFederationRecords", FeatureSwitchSection.GENERAL_OPTIONS),
	LIGHT_BAR_U13("lightBarU13", FeatureSwitchSection.GENERAL_OPTIONS),
	LIGHT_BAR_U15("lightBarU15", FeatureSwitchSection.GENERAL_OPTIONS),
	CHILDREN_EQUIPMENT("childrenEquipment", FeatureSwitchSection.GENERAL_OPTIONS),
	NO_INTERIM_SCORES_IN_RESULTS("noInterimScoresInResults", FeatureSwitchSection.GENERAL_OPTIONS),
	MASTERS_20KG("masters20kg", FeatureSwitchSection.GENERAL_OPTIONS),
	PREVIOUS_SESSION_MEDALS_ONLY("previousSessionMedalsOnly", FeatureSwitchSection.GENERAL_OPTIONS),

	DISABLE_RECORD_HIGHLIGHT("disableRecordHighlight", FeatureSwitchSection.SPECIALTY_FEATURES),
	BLACK_STOP_BUTTON("blackStopButton", FeatureSwitchSection.SPECIALTY_FEATURES),
	NO_FORWARDER_KEEP_ALIVE("noForwarderKeepAlive", FeatureSwitchSection.SPECIALTY_FEATURES),
	ENABLE_TIME_KEEPER_SESSION_SWITCH("enableTimeKeeperSessionSwitch", FeatureSwitchSection.SPECIALTY_FEATURES),
	USAW_SESSION_BLOCKS("usawSessionBlocks", FeatureSwitchSection.SPECIALTY_FEATURES),
	USAW_COLLARS("usawCollars", FeatureSwitchSection.SPECIALTY_FEATURES),
	MANUAL_START_NUMBERS("manualStartNumbers", FeatureSwitchSection.SPECIALTY_FEATURES),
	MEDALS_FOR_CATEGORY_ONLY("medalsForCategoryOnly", FeatureSwitchSection.SPECIALTY_FEATURES),
	MQTT_DOWN_SIGNAL("mqttDownSignal", FeatureSwitchSection.SPECIALTY_FEATURES),
	KEEP_SPANISH_HYPHEN_SHORTCUT("keepSpanishHyphenShortcut", FeatureSwitchSection.SPECIALTY_FEATURES),

	NO_ATHLETE_UPDATES("noAthleteUpdates", FeatureSwitchSection.OBSOLETE),
	OLD_TIMERS("oldTimers", FeatureSwitchSection.OBSOLETE),
	OLD_CAT_ORDER("oldCatOrder", FeatureSwitchSection.OBSOLETE),

	ANNOUNCER_TRIGGERS_INITIAL_DECISION("announcerTriggersInitialDecision", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	CHAMPIONSHIP_GROUPING("championshipGrouping", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	DISPLAY_BEST_SCORE_RANK("displayBestScoreRank", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	DISPLAY_BODY_WEIGHT("displayBodyWeight", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	GENDER_INCLUSIVE("genderInclusive", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	IWF_LOOK("iwfLook", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	NO_BEST_SCORE_RANK("noBestScoreRank", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	NO_SINCLAIR_RANK("noSinclairRank", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	RECORD_NAME_IS_CATEGORY("recordNameIsCategory", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	SHOW_DECISIONS_IMMEDIATELY("showDecisionsImmediately", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	SINCLAIR_MEET("SinclairMeet", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	STRETCH_PUBLIC("stretchPublic", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	STRETCH_VIDEO("stretchVideo", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	TEAM_POINTS_TOTAL_ONLY("teamPointsTotalOnly", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
	USE_CUSTOM2_AS_SUB_CATEGORY("UseCustom2AsSubCategory", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),

	GAMX("GAMX", FeatureSwitchSection.INTERNAL),
	PLAYWRIGHT("playwright", FeatureSwitchSection.INTERNAL),
	RECORD_REPOSITORY("recordRepository", FeatureSwitchSection.INTERNAL),
	RECORDS_ONLY("recordsOnly", FeatureSwitchSection.INTERNAL),
	RECORDS_PREPARATION("recordsPreparation", FeatureSwitchSection.INTERNAL),
	USAW("usaw", FeatureSwitchSection.INTERNAL),
	V2_EXPORT("v2Export", FeatureSwitchSection.INTERNAL);

	private static final Map<String, FeatureSwitch> BY_ID = buildLookup();
	private final String id;
	private final FeatureSwitchSection section;
	private final String[] aliases;

	FeatureSwitch(String id, FeatureSwitchSection section, String... aliases) {
		this.id = id;
		this.section = section;
		this.aliases = aliases;
	}

	public String getId() {
		return this.id;
	}

	public FeatureSwitchSection getSection() {
		return this.section;
	}

	public String getLabelKey() {
		return getTranslationBaseKey() + ".Label";
	}

	public String getDescriptionKey() {
		return getTranslationBaseKey() + ".Description";
	}

	public String getActivationKey() {
		return getTranslationBaseKey() + ".Activation";
	}

	public String getTranslationBaseKey() {
		return "FeatureSwitch." + this.id;
	}

	public static Optional<FeatureSwitch> fromId(String id) {
		if (id == null || id.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(BY_ID.get(normalize(id)));
	}

	public static Map<String, FeatureSwitch> getLookup() {
		return BY_ID;
	}

	private static Map<String, FeatureSwitch> buildLookup() {
		Map<String, FeatureSwitch> byId = new HashMap<>();
		for (FeatureSwitch featureSwitch : values()) {
			byId.put(normalize(featureSwitch.id), featureSwitch);
			for (String alias : featureSwitch.aliases) {
				byId.put(normalize(alias), featureSwitch);
			}
		}
		return Collections.unmodifiableMap(byId);
	}

	private static String normalize(String id) {
		return id.trim().toLowerCase(Locale.ROOT);
	}
}
