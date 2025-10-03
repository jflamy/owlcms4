/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.coach;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.utils.IdUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames() if you want persistence
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Coach.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Coach implements Serializable, Comparable<Coach> {

    @Transient
    @JsonIgnore
    private static final Logger logger = (Logger) LoggerFactory.getLogger(Coach.class);

    @Id
    private Long id;
    private String lastName;
    private String firstName;
    private String membershipId;
    private String team;

    public void setId(Long id) {
        this.id = id;
    }

    public Coach() {
        setId(IdUtils.getTimeBasedId());
    }

    public Coach(String lastName, String firstName, String membershipId, String team) {
        setId(IdUtils.getTimeBasedId());
        this.lastName = lastName;
        this.firstName = firstName;
        this.membershipId = membershipId;
        this.team = team;
    }

    @Override
    public int compareTo(Coach o) {
        return ObjectUtils.compare(this.getFullName(), o.getFullName(), true);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Coach other = (Coach) obj;
        return getId() != null && getId().equals(other.getId());
    }

    public Long getId() {
        return this.id;
    }

    public String getFullName() {
        return this.lastName + (this.firstName != null ? (", " + this.firstName) : "");
    }

    @Override
    public String toString() {
        return getFullName();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getTeamFlagPath() {
        logger.debug("Coach {} team {}", this, this.team);
		// use the same approach as URLUtils to find the flag
		return URLUtils.getFlagResourcePath(this.team, new String[] {".png"});
	}

    public void setTeamFlagPath(String path) {
        // no-op, just to please some serializers
    }
}
