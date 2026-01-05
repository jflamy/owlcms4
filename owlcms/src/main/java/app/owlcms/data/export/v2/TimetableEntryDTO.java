/*******************************************************************************
 * @author Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.owlcms.data.group.Group;
import app.owlcms.data.technicalofficial.OfficialRole;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;

/**
 * DTO for timetable entries in V2 export format.
 * 
 * Converts between TechnicalOfficialsTimetable entities (which reference Group objects)
 * and V2 export format (which uses session name strings for cleaner JSON).
 */
public class TimetableEntryDTO {

    @JsonProperty
    private String sessionName;

    @JsonProperty
    private String roleCategory;

    @JsonProperty
    private Integer teamNumber;

    // Constructors
    public TimetableEntryDTO() {
    }

    public TimetableEntryDTO(String sessionName, String roleCategory, Integer teamNumber) {
        this.sessionName = sessionName;
        this.roleCategory = roleCategory;
        this.teamNumber = teamNumber;
    }

    // Static factory methods
    public static TimetableEntryDTO fromEntity(TechnicalOfficialsTimetable entity) {
        if (entity == null) {
            return null;
        }
        Group group = entity.getGroup();
        String sessionName = group != null ? group.getName() : null;
        String roleCategory = entity.getRoleCategory() != null ? entity.getRoleCategory().name() : null;
        return new TimetableEntryDTO(sessionName, roleCategory, entity.getTeamNumber());
    }

    public TechnicalOfficialsTimetable toEntity(Group group) {
        TechnicalOfficialsTimetable entity = new TechnicalOfficialsTimetable();
        entity.setGroup(group);
        entity.setRoleCategory(roleCategory != null ? OfficialRole.valueOf(roleCategory) : null);
        entity.setTeamNumber(teamNumber);
        return entity;
    }

    // Getters and Setters
    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getRoleCategory() {
        return roleCategory;
    }

    public void setRoleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
    }

    public Integer getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(Integer teamNumber) {
        this.teamNumber = teamNumber;
    }

    @Override
    public String toString() {
        return "TimetableEntryDTO [sessionName=" + sessionName + ", roleCategory=" + roleCategory + ", teamNumber="
                + teamNumber + "]";
    }

}
