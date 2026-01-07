/*******************************************************************************
 * @author Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.data.group.Group;

/**
 * TechnicalOfficialsTimetable - Maps technical officials to sessions and roles.
 * 
 * Represents the assignment of technical officials (referees, jury members, etc.)
 * to specific sessions and roles. Used for generating IWF-compliant competition
 * schedules and managing official assignments during competition.
 */
@Entity
@Table(name = "technical_officials_timetable")
@Cacheable(value = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TechnicalOfficialsTimetable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Group.class)
    @JsonIdentityReference(alwaysAsId = true)
    private Group group;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OfficialRole roleCategory;

    @Column(nullable = false)
    private Integer teamNumber;

    // Constructors
    public TechnicalOfficialsTimetable() {
    }

    public TechnicalOfficialsTimetable(Group group, OfficialRole roleCategory, Integer teamNumber) {
        this.group = group;
        this.roleCategory = roleCategory;
        this.teamNumber = teamNumber;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public OfficialRole getRoleCategory() {
        return roleCategory;
    }

    public void setRoleCategory(OfficialRole roleCategory) {
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
        return "TechnicalOfficialsTimetable [id=" + id + ", group=" + (group != null ? group.getName() : "null")
                + ", roleCategory=" + roleCategory + ", teamNumber=" + teamNumber + "]";
    }

}
