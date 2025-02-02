/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.technicalofficial;

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
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = TechnicalOfficial.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class TechnicalOfficial implements Serializable, Comparable<TechnicalOfficial> {

	@Transient
	@JsonIgnore
	private static final Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficial.class);
	/** The id. */
	@Id
	private Long id;
	private String lastName;
	private String firstName;
	private TOLevel level;
	private String iwfId;
	private String federation;
	private String federationId;

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Instantiates a new platform. Used for import, no default values.
	 */
	public TechnicalOfficial() {
		setId(IdUtils.getTimeBasedId());
		// logger.debug"new Platform 1 {} {}",this.getNbB_5(), LoggerUtils.whereFrom());
	}

	/**
	 * Instantiates a new platform.
	 *
	 * @param name the name
	 */
	public TechnicalOfficial(String lastName, String firstName, TOLevel level, String iwfId, String federation, String federationId) {
		setId(IdUtils.getTimeBasedId());
		this.lastName = lastName;
		this.firstName = firstName;
		this.level = level;
		this.iwfId = iwfId;
		this.federation = federation;
		this.federationId = federationId;
	}

	@Override
	public int compareTo(TechnicalOfficial o) {
		return ObjectUtils.compare(this.getFullName(), o.getFullName(), true);
	}

	@Override
	public boolean equals(Object obj) {
		// https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		TechnicalOfficial other = (TechnicalOfficial) obj;
		return getId() != null && getId().equals(other.getId());

	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Gets the full name.
	 *
	 * @return lastName + ", " + firstName
	 */
	public String getFullName() {
		return this.lastName + ", " + this.firstName;
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

	public TOLevel getLevel() {
		return level;
	}

	public void setLevel(TOLevel level) {
		this.level = level;
	}

	public String getIwfId() {
		return iwfId;
	}

	public void setIwfId(String iwfId) {
		this.iwfId = iwfId;
	}

	public String getFederation() {
		return federation;
	}

	public void setFederation(String federation) {
		this.federation = federation;
	}

	public String getFederationId() {
		return federationId;
	}

	public void setFederationId(String federationId) {
		this.federationId = federationId;
	}

}
