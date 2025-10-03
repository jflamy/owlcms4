/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.technicalofficial;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.i18n.Translator;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.URLUtils;
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
	private String affiliation;
	@Enumerated(EnumType.STRING)
	@Column(columnDefinition = "varchar(255) default 'TECHNICAL_OFFICIAL'")
	private Role role;
	@Column(columnDefinition = "boolean default false")
	private boolean active;

	public enum CredentialType {
		TECHNICAL_OFFICIAL,
		ORGANIZATION,
		INVITED,
		MEDIA,
		MEDICAL
	}

	public enum Role {
		TECHNICAL_OFFICIAL(CredentialType.TECHNICAL_OFFICIAL),
		COMPETITION_DIRECTOR(CredentialType.TECHNICAL_OFFICIAL),
		COMPETITION_SECRETARY(CredentialType.TECHNICAL_OFFICIAL),
		STAFF(CredentialType.ORGANIZATION),
		VOLUNTEER(CredentialType.ORGANIZATION),
		LOADER(CredentialType.ORGANIZATION),
		INFORMATION_TECHNOLOGY(CredentialType.ORGANIZATION),
		VIDEO(CredentialType.ORGANIZATION),
		MEDIA(CredentialType.MEDIA),
		VIP(CredentialType.INVITED),
		DOCTOR(CredentialType.MEDICAL),
		MEDICAL_STAFF(CredentialType.MEDICAL);

		private final CredentialType credentialType;

		Role(CredentialType credentialType) {
			this.credentialType = credentialType;
		}

		public CredentialType getCredentialType() {
			return credentialType;
		}
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Instantiates a new TO. Used for import, no default values.
	 */
	public TechnicalOfficial() {
		setId(IdUtils.getTimeBasedId());
		this.role = Role.TECHNICAL_OFFICIAL;
		this.active = false;
		// logger.debug"new Platform 1 {} {}",this.getNbB_5(), LoggerUtils.whereFrom());
	}

	/**
	 * Instantiates a new platform.
	 *
	 * @param name the name
	 */
	public TechnicalOfficial(String lastName, String firstName, TOLevel level, String iwfId, String federation,
			String federationId, String affiliation) {
		setId(IdUtils.getTimeBasedId());
		this.lastName = lastName;
		this.firstName = firstName;
		this.level = level;
		this.iwfId = iwfId;
		this.federation = federation;
		this.federationId = federationId;
		this.affiliation = affiliation;
		this.role = Role.TECHNICAL_OFFICIAL;
		this.active = false;
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

	@Override
	public int hashCode() {
		// Use id for hashCode to be consistent with equals
		// https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
		return (getId() != null) ? getId().hashCode() : 0;
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

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Transient
	@JsonIgnore
	public CredentialType getCredentialType() {
		return role != null ? role.getCredentialType() : null;
	}

	/**
	 * Get the translated role name for display.
	 * 
	 * @return translated role name, or empty string if role is null
	 */
	@Transient
	@JsonIgnore
	public String getTranslatedRole() {
		if (role == null) {
			return "";
		}
		return Translator.translate("TO.Role." + role.name());
	}

	/**
	 * Get the translated credential type name for display.
	 * 
	 * @return translated credential type name, or empty string if credential type is null
	 */
	@Transient
	@JsonIgnore
	public String getTranslatedCredentialType() {
		CredentialType credentialType = getCredentialType();
		if (credentialType == null) {
			return "";
		}
		return Translator.translate("TO.CredentialType." + credentialType.name());
	}

	@Transient
	@JsonIgnore
	public String getTeamFlagPath() {
		String fed = this.getFederation();
		logger.debug("TO {} federation {}", this, fed);
		// use the same approach as URLUtils to find the flag
		return URLUtils.getFlagResourcePath(fed, new String[] { ".png" });
	}

	public void setTeamFlagPath(String path) {
		// no-op, just to please some serializers
	}

}
