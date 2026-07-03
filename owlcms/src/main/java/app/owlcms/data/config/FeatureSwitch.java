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
    ATHLETE_CARD_ENTRY_TOTAL("athleteCardEntryTotal", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
    ENABLE_TIME_KEEPER_SESSION_SWITCH("enableTimeKeeperSessionSwitch", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),
    RECORD_REPOSITORY("recordRepository", FeatureSwitchSection.USER_INTERFACE_OVERRIDE),

    MEDALISTS_AS_LEADERS("medalistsAsLeaders", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    PREVIOUS_SESSION_MEDALS_ONLY("previousSessionMedalsOnly", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    BW_CLASS_THEN_AGE_GROUP("bwClassThenAgeGroup", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    CHAMPIONSHIP_GROUPING("championshipGrouping", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    DISPLAY_BEST_SCORE("displayBestScore", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    DISPLAY_BEST_SCORE_RANK("displayBestScoreRank", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    NO_BEST_SCORE_RANK("noBestScoreRank", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    NO_SINCLAIR_RANK("noSinclairRank", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    DISPLAY_BODY_WEIGHT("displayBodyWeight", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    SHORT_SCOREBOARD_NAMES("shortScoreboardNames", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    FORCE_ALL_GROUP_RECORDS("forceAllGroupRecords", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    FORCE_ALL_FEDERATION_RECORDS("forceAllFederationRecords", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    DISABLE_RECORD_HIGHLIGHT("disableRecordHighlight", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    MEDALS_FOR_CATEGORY_ONLY("medalFlagsForCategoryOnly", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    STRETCH_PUBLIC("stretchPublic", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    STRETCH_VIDEO("stretchVideo", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    DONT_FIX_NAMES("dontFixNames", FeatureSwitchSection.SCOREBOARD_OPTIONS),
    CUSTOM_TEAM_NAME("customTeamName", FeatureSwitchSection.SCOREBOARD_OPTIONS),

    MANUAL_START_NUMBERS("manualStartNumbers", FeatureSwitchSection.GENERAL_OPTIONS),
    EXPLICIT_TEAMS("explicitTeams", FeatureSwitchSection.GENERAL_OPTIONS),
    BEST_MATCH_CATEGORIES("bestMatchCategories", FeatureSwitchSection.GENERAL_OPTIONS),
    NO_INTERIM_SCORES_IN_RESULTS("noInterimScoresInResults", FeatureSwitchSection.GENERAL_OPTIONS),
    TEAM_POINTS_TOTAL_ONLY("teamPointsTotalOnly", FeatureSwitchSection.GENERAL_OPTIONS),
    LIGHT_BAR_U13("lightBarU13", FeatureSwitchSection.GENERAL_OPTIONS),
    LIGHT_BAR_U15("lightBarU15", FeatureSwitchSection.GENERAL_OPTIONS),
    CHILDREN_EQUIPMENT("childrenEquipment", FeatureSwitchSection.GENERAL_OPTIONS),

    USAW_SESSION_BLOCKS("usawSessionBlocks", FeatureSwitchSection.SPECIALTY_FEATURES),
    USAW_COLLARS("usawCollars", FeatureSwitchSection.SPECIALTY_FEATURES),
    MQTT_DOWN_SIGNAL("mqttDownSignal", FeatureSwitchSection.SPECIALTY_FEATURES),
    BLACK_STOP_BUTTON("blackStopButton", FeatureSwitchSection.SPECIALTY_FEATURES),
    KEEP_SPANISH_HYPHEN_SHORTCUT("keepSpanishHyphenShortcut", FeatureSwitchSection.SPECIALTY_FEATURES),
    ANNOUNCER_TRIGGERS_INITIAL_DECISION("announcerTriggersInitialDecision", FeatureSwitchSection.SPECIALTY_FEATURES),

    GENDER_INCLUSIVE("genderInclusive", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    RECORD_NAME_IS_CATEGORY("recordNameIsCategory", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    SHOW_DECISIONS_IMMEDIATELY("showDecisionsImmediately", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    SINCLAIR_MEET("SinclairMeet", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    PLAYWRIGHT("playwright", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    V2_EXPORT("v2Export", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    NO_FORWARDER_KEEP_ALIVE("noForwarderKeepAlive", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    DECISION_SECTION("decisionSection", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    DECISION_SECTION_REF_FINAL_ONLY("decisionSectionRefFinalOnly", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    DECISION_SECTION_SHOW_BOTH_JURY_VOTES("decisionSectionShowBothJuryVotes", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    DECISION_SECTION_STOPWATCH("decisionSectionStopwatch", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK),
    IWF_LOOK("iwfLook", FeatureSwitchSection.USE_AT_YOUR_OWN_RISK);

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

    public String getTranslationKey() {
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
