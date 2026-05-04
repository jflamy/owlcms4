/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import java.util.EnumSet;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * TeamRole represents the generic roles used for:
 * - Import/Export columns
 * - Timetable-driven team assignment generation
 * - Dropdown selection in UI forms
 * 
 * Each TeamRole maps to a set of specific OfficialRole positions that can be
 * assigned to individuals for a specific session.
 */
public enum TeamRole {

    REFEREE("Referee", EnumSet.of(
            OfficialRole.REFEREE,
            OfficialRole.CENTER_REFEREE,
            OfficialRole.LEFT_REFEREE,
            OfficialRole.RIGHT_REFEREE,
            OfficialRole.REFEREE_RESERVE
    )),

    JURY_PRESIDENT("JuryPresident", EnumSet.of(
            OfficialRole.JURY_PRESIDENT
    )),

    JURY("Jury", EnumSet.of(
            OfficialRole.JURY,
            OfficialRole.JURY_MEMBER,
            OfficialRole.JURY_A,
            OfficialRole.JURY_B,
            OfficialRole.JURY_C,
            OfficialRole.JURY_D,
            OfficialRole.JURY_RESERVE
    )),

    MARSHALL("Marshall", EnumSet.of(
            OfficialRole.MARSHALL,
            OfficialRole.MARSHAL1,
            OfficialRole.MARSHAL2
    )),

    TIMEKEEPER("Timekeeper", EnumSet.of(
            OfficialRole.TIMEKEEPER
    )),

    TECHNICAL_CONTROLLER("TechnicalController", EnumSet.of(
            OfficialRole.TECHNICAL_CONTROLLER,
            OfficialRole.TECHNICAL_CONTROLLER1,
            OfficialRole.TECHNICAL_CONTROLLER2
    )),

    DOCTOR("Doctor", EnumSet.of(
            OfficialRole.DOCTOR,
            OfficialRole.DOCTOR2,
            OfficialRole.DOCTOR3
    )),

    COMPETITION_SECRETARY("CompetitionSecretary", EnumSet.of(
            OfficialRole.COMPETITION_SECRETARY,
            OfficialRole.COMPETITION_SECRETARY2
    )),

    ANNOUNCER("Announcer", EnumSet.of(
            OfficialRole.ANNOUNCER
    )),

    WEIGHIN("WeighIn", EnumSet.of(
            OfficialRole.WEIGHIN,
            OfficialRole.WEIGHIN1,
            OfficialRole.WEIGHIN2
    ));

    private final String translationKey;
    private final EnumSet<OfficialRole> specificRoles;

    TeamRole(String translationKey, EnumSet<OfficialRole> specificRoles) {
        this.translationKey = translationKey;
        this.specificRoles = specificRoles;
    }

    /**
     * Get the translation key for display.
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Get all specific OfficialRole positions that belong to this team role.
     */
    public EnumSet<OfficialRole> getSpecificRoles() {
        return specificRoles;
    }

    /**
     * Find the TeamRole that contains a given OfficialRole.
     * @param officialRole the specific role to look up
     * @return the TeamRole that contains it, or null if not found
     */
    public static TeamRole fromOfficialRole(OfficialRole officialRole) {
        if (officialRole == null) {
            return null;
        }
        for (TeamRole teamRole : values()) {
            if (teamRole.specificRoles.contains(officialRole)) {
                return teamRole;
            }
        }
        return null;
    }

    /**
     * Find a TeamRole by its translation key (used for import).
     * @param key the translation key to look up
     * @return the matching TeamRole, or null if not found
     */
    public static TeamRole fromTranslationKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (TeamRole teamRole : values()) {
            if (teamRole.translationKey.equalsIgnoreCase(key)) {
                return teamRole;
            }
        }
        return null;
    }

    /**
     * JSON deserializer that handles legacy value "MARSHAL" (single L)
     * which was used in earlier versions before standardizing to "MARSHALL".
     */
    @JsonCreator
    public static TeamRole fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Handle legacy spelling with single L
        if ("MARSHAL".equalsIgnoreCase(value)) {
            return MARSHALL;
        }
        // Standard enum lookup
        try {
            return TeamRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
