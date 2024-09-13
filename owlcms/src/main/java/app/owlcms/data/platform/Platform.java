/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.platform;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.sound.sampled.Mixer;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.sound.Speakers;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Data on a lifting site.
 * <p>
 * Groups are associated with a lifting platformName.
 * </p>
 * <p>
 * Projectors and officials are associated with a lifting platformName so there is no need to refresh their setup during a competition. The name of the
 * platformName is used as a key in the ServletContext so other sessions and other kinds of pages (such as JSP) can locate the information about that
 * platformName. See in particular the {@link LiftList#updateTable()} method
 * </p>
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Platform.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Platform implements Serializable, Comparable<Platform> {

	@Transient
	@JsonIgnore
	private static final Logger logger = (Logger) LoggerFactory.getLogger(Platform.class);
	/**
	 * Only used for unit testing when there is no session
	 */
	@Transient
	@JsonIgnore
	private static Platform testingPlatform;

	/**
	 * Gets the current platform
	 *
	 * @return the current
	 */
	@Transient
	@JsonIgnore
	public static Platform getCurrent() {
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null) {
			Platform platform = fop.getPlatform();
			return (platform != null ? platform : testingPlatform);
		} else {
			return testingPlatform;
		}
	}

	/**
	 * Sets the current platform
	 *
	 * @param p the new current
	 */
	@Transient
	@JsonIgnore
	public static void setCurrent(Platform p) {
		VaadinSession current = VaadinSession.getCurrent();
		if (current != null) {
			current.setAttribute("platform", p);
		} else {
			testingPlatform = p;
		}
	}

	/** The name. */
	String name;
	/** The id. */
	@Id
	private
	// @GeneratedValue(strategy = GenerationType.AUTO)
	// @JsonIgnore
	Long id;
	/**
	 * If mixer is not null, emit sound on the associated device
	 */
	@Transient
	@JsonIgnore
	private Mixer mixer = null;
	@Transient
	@JsonIgnore
	private boolean mixerChecked;

	// collar
	private Integer nbC_2_5 = 1;

	// large plates
	private Integer nbL_10 = 1;
	private Integer nbL_15 = 1;
	private Integer nbL_20 = 1;
	private Integer nbL_25 = 3;

	// kid plates
	private Integer nbL_2_5 = 0;
	private Integer nbL_5 = 0;

	// small plates
	private Integer nbS_0_5 = 1;
	private Integer nbS_1 = 1;
	private Integer nbS_1_5 = 1;
	private Integer nbS_2 = 1;
	private Integer nbS_2_5 = 1;
	private Integer nbS_5 = 1;

	// bars
	private Integer nonStandardBarWeight = 0;
	private Integer nbB_5 = 0;
	private Integer nbB_10 = 0;
	private Integer nbB_15 = 1;
	private Integer nbB_20 = 1;
	/**
	 * true if the referee use this application to give decisions, and decision lights need to be shown on the attempt and result boards.
	 */
	private Boolean showDecisionLights = false;
	/**
	 * true if the time should be displayed
	 */
	private Boolean showTimer = false;
	private String soundMixerName;
	
	// nonStandardBar needed for backward compatibility (old column is present in imports)
	@SuppressWarnings("unused")
	@JsonIgnore
	private Boolean nonStandardBar = false;
	
	// unfortunate choice of name in old code base, needed for imports to work.
	@Column(name="nonStandardBarAvailable")
	@JsonProperty("nonStandardBarAvailable")
	private Boolean useNonStandardBar = false;

	/**
	 * Instantiates a new platform. Used for import, no default values.
	 */
	public Platform() {
		setId(IdUtils.getTimeBasedId());
		//logger.debug"new Platform 1 {} {}",this.getNbB_5(), LoggerUtils.whereFrom());
	}

	/**
	 * Instantiates a new platform.
	 *
	 * @param name the name
	 */
	public Platform(String name) {
		setId(IdUtils.getTimeBasedId());
		this.setName(name);
		//logger.debug"new Platform 2",this.getNbB_5());
		//this.defaultPlates();
	}

	@Override
	public int compareTo(Platform o) {
		return ObjectUtils.compare(this.getName(), o.getName(), true);
	}

	public void defaultPlates() {
		// setDefaultMixerName(platform1);
		this.setShowDecisionLights(true);
		this.setShowTimer(true);

		// collar
		this.setNbC_2_5(1);

		// small plates
		this.setNbS_0_5(1);
		this.setNbS_1(1);
		this.setNbS_1_5(1);
		this.setNbS_2(1);
		this.setNbS_2_5(1);
		this.setNbS_5(1);

		// large plates, regulation set-up
		this.setNbL_10(1);
		this.setNbL_15(1);
		this.setNbL_20(1);
		this.setNbL_25(3);

		// large plates, kid competitions
		this.setNbL_2_5(0);
		this.setNbL_5(0);

		// available bars
		this.setNbB_5(0);
		this.setNbB_10(0);
		this.setNbB_15(1);
		this.setNbB_20(1);
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
		Platform other = (Platform) obj;
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

	public Mixer getMixer() {
		if (!this.mixerChecked && this.mixer == null) {
			setSoundMixerName(this.getSoundMixerName());
		}
		return this.mixer;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	public Integer getNbB_10() {
		return this.nbB_10 != null ? this.nbB_10 : 0;
	}

	public Integer getNbB_15() {
		return this.nbB_15 != null ? this.nbB_15 : 1;
	}

	public Integer getNbB_20() {
		return this.nbB_20 != null ? this.nbB_20 : 1;
	}

	public Integer getNbB_5() {
		return this.nbB_5 != null ? this.nbB_5 : 0;
	}

	/**
	 * Gets the nb C 2 5.
	 *
	 * @return the nb C 2 5
	 */
	public Integer getNbC_2_5() {
		if (this.nbC_2_5 == null) {
			return 0;
		}
		return this.nbC_2_5;
	}

	/**
	 * Gets the nb L 10.
	 *
	 * @return the nb L 10
	 */
	public Integer getNbL_10() {
		if (this.nbL_10 == null) {
			return 0;
		}
		return this.nbL_10;
	}

	/**
	 * Gets the nb L 15.
	 *
	 * @return the nb L 15
	 */
	public Integer getNbL_15() {
		if (this.nbL_15 == null) {
			return 0;
		}
		return this.nbL_15;
	}

	/**
	 * Gets the nb L 2 5.
	 *
	 * @return the nb L 2 5
	 */
	public Integer getNbL_2_5() {
		if (this.nbL_2_5 == null) {
			return 0;
		}
		return this.nbL_2_5;
	}

	/**
	 * Gets the nb L 20.
	 *
	 * @return the nb L 20
	 */
	public Integer getNbL_20() {
		if (this.nbL_20 == null) {
			return 0;
		}
		return this.nbL_20;
	}

	/**
	 * Gets the nb L 25.
	 *
	 * @return the nb L 25
	 */
	public Integer getNbL_25() {
		if (this.nbL_25 == null) {
			return 0;
		}
		return this.nbL_25;
	}

	/**
	 * Gets the nb L 5.
	 *
	 * @return the nb L 5
	 */
	public Integer getNbL_5() {
		if (this.nbL_5 == null) {
			return 0;
		}
		return this.nbL_5;
	}

	/**
	 * Gets the nb S 0 5.
	 *
	 * @return the nb S 0 5
	 */
	public Integer getNbS_0_5() {
		if (this.nbS_0_5 == null) {
			return 0;
		}
		return this.nbS_0_5;
	}

	/**
	 * Gets the nb S 1.
	 *
	 * @return the nb S 1
	 */
	public Integer getNbS_1() {
		if (this.nbS_1 == null) {
			return 0;
		}
		return this.nbS_1;
	}

	/**
	 * Gets the nb S 1 5.
	 *
	 * @return the nb S 1 5
	 */
	public Integer getNbS_1_5() {
		if (this.nbS_1_5 == null) {
			return 0;
		}
		return this.nbS_1_5;
	}

	/**
	 * Gets the nb S 2.
	 *
	 * @return the nb S 2
	 */
	public Integer getNbS_2() {
		if (this.nbS_2 == null) {
			return 0;
		}
		return this.nbS_2;
	}

	/**
	 * Gets the nb S 2 5.
	 *
	 * @return the nb S 2 5
	 */
	public Integer getNbS_2_5() {
		if (this.nbS_2_5 == null) {
			return 0;
		}
		return this.nbS_2_5;
	}

	/**
	 * Gets the nb S 5.
	 *
	 * @return the nb S 5
	 */
	public Integer getNbS_5() {
		if (this.nbS_5 == null) {
			return 0;
		}
		return this.nbS_5;
	}

	/**
	 * Gets the light bar.
	 *
	 * @return the light bar
	 */
	public Integer getNonStandardBarWeight() {
		if (this.nonStandardBarWeight == null) {
			return 0;
		}
		return this.nonStandardBarWeight;
	}

//	/**
//	 * Gets the official bar.
//	 *
//	 * @return the official bar
//	 */
//	public Integer getOfficialBar() {
//		if (this.isNonStandardBar()) {
//			return 0;
//		}
//		return this.officialBar;
//	}

	/**
	 * Gets the show decision lights.
	 *
	 * @return the show decision lights
	 */
	public Boolean getShowDecisionLights() {
		return this.showDecisionLights == null ? false : this.showDecisionLights;
	}

	/**
	 * Gets the show timer.
	 *
	 * @return the show timer
	 */
	public Boolean getShowTimer() {
		boolean b = this.showTimer == null ? false : this.showTimer;
		return b;
	}

	/**
	 * @return the soundMixerName
	 */
	public String getSoundMixerName() {
		logger.debug("getSoundMixerName {} {}", System.identityHashCode(this), this.soundMixerName);
		if (this.soundMixerName == null) {
			return Translator.translate("UseBrowserSound");
		}
		return this.soundMixerName;
	}

	@Override
	public int hashCode() {
		// https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
		return 31;
	}

	public Boolean isUseNonStandardBar() {
		return getUseNonStandardBar();
	}
	
	public Boolean getUseNonStandardBar() {
		return Boolean.TRUE.equals(useNonStandardBar);
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void setNbB_10(Integer nbB_10) {
		this.nbB_10 = nbB_10;
	}

	public void setNbB_15(Integer nbB_15) {
		this.nbB_15 = nbB_15;
	}

	public void setNbB_20(Integer nbB_20) {
		//logger.debug("nbB_20 = {} {}",nbB_20, LoggerUtils.whereFrom());
		this.nbB_20 = nbB_20;
	}

	public void setNbB_5(Integer nbB_5) {
		//logger.debug"setting 5kg bumper {}",nbB_5);
		this.nbB_5 = nbB_5;
	}

	/**
	 * Sets the nb C 2 5.
	 *
	 * @param nbC_2_5 the new nb C 2 5
	 */
	public void setNbC_2_5(Integer nbC_2_5) {
		this.nbC_2_5 = nbC_2_5;
	}

	/**
	 * Sets the nb L 10.
	 *
	 * @param nbL_10 the new nb L 10
	 */
	public void setNbL_10(Integer nbL_10) {
		this.nbL_10 = nbL_10;
	}

	/**
	 * Sets the nb L 15.
	 *
	 * @param nbL_15 the new nb L 15
	 */
	public void setNbL_15(Integer nbL_15) {
		this.nbL_15 = nbL_15;
	}

	/**
	 * Sets the nb L 2 5.
	 *
	 * @param nbL_2_5 the new nb L 2 5
	 */
	public void setNbL_2_5(Integer nbL_2_5) {
		this.nbL_2_5 = nbL_2_5;
	}

	/**
	 * Sets the nb L 20.
	 *
	 * @param nbL_20 the new nb L 20
	 */
	public void setNbL_20(Integer nbL_20) {
		this.nbL_20 = nbL_20;
	}

	/**
	 * Sets the nb L 25.
	 *
	 * @param nbL_25 the new nb L 25
	 */
	public void setNbL_25(Integer nbL_25) {
		this.nbL_25 = nbL_25;
	}

	/**
	 * Sets the nb L 5.
	 *
	 * @param nbL_5 the new nb L 5
	 */
	public void setNbL_5(Integer nbL_5) {
		this.nbL_5 = nbL_5;
	}

	/**
	 * Sets the nb S 0 5.
	 *
	 * @param nbS_0_5 the new nb S 0 5
	 */
	public void setNbS_0_5(Integer nbS_0_5) {
		this.nbS_0_5 = nbS_0_5;
	}

	/**
	 * Sets the nb S 1.
	 *
	 * @param nbS_1 the new nb S 1
	 */
	public void setNbS_1(Integer nbS_1) {
		this.nbS_1 = nbS_1;
	}

	/**
	 * Sets the nb S 1 5.
	 *
	 * @param nbS_1_5 the new nb S 1 5
	 */
	public void setNbS_1_5(Integer nbS_1_5) {
		this.nbS_1_5 = nbS_1_5;
	}

	/**
	 * Sets the nb S 2.
	 *
	 * @param nbS_2 the new nb S 2
	 */
	public void setNbS_2(Integer nbS_2) {
		this.nbS_2 = nbS_2;
	}

	/**
	 * Sets the nb S 2 5.
	 *
	 * @param nbS_2_5 the new nb S 2 5
	 */
	public void setNbS_2_5(Integer nbS_2_5) {
		this.nbS_2_5 = nbS_2_5;
	}

	/**
	 * Sets the nb S 5.
	 *
	 * @param nbS_5 the new nb S 5
	 */
	public void setNbS_5(Integer nbS_5) {
		this.nbS_5 = nbS_5;
	}

	public void setUseNonStandardBar(Boolean nonStandardBarAvailable) {
		logger.debug("nsba {} ({})",true, System.identityHashCode(this));
		this.useNonStandardBar = nonStandardBarAvailable;
	}

	/**
	 * Sets the light bar.
	 *
	 * @param lightBar the new light bar
	 */
	public void setNonStandardBarWeight(Integer lightBar) {
		this.nonStandardBarWeight = lightBar;
	}

//	/**
//	 * Sets the official bar.
//	 *
//	 * @param officialBar the new official bar
//	 */
//	public void setOfficialBar(Integer officialBar) {
//		this.officialBar = officialBar;
//	}

	/**
	 * Sets the show decision lights.
	 *
	 * @param showDecisionLights the new show decision lights
	 */
	public void setShowDecisionLights(Boolean showDecisionLights) {
		this.showDecisionLights = showDecisionLights;
	}

	/**
	 * Sets the show timer.
	 *
	 * @param showTime the new show timer
	 */
	public void setShowTimer(Boolean showTime) {
		this.showTimer = showTime;
	}

	/**
	 * @param soundMixerName the soundMixerName to set
	 */
	public void setSoundMixerName(String soundMixerName) {
		logger.debug("setSoundMixerName {} {} {}", System.identityHashCode(this), soundMixerName,
		        LoggerUtils.whereFrom());
		this.soundMixerName = soundMixerName;
		if (soundMixerName == null) {
			this.mixerChecked = true;
			setMixer(null);
			return;
		}

		setMixer(null);

		List<Mixer> soundMixers = Speakers.getOutputs();
		for (Mixer curMixer : soundMixers) {
			if (curMixer.getMixerInfo().getName().equals(soundMixerName)) {
				setMixer(curMixer);
				logger.info("Platform {}: changing mixer to {}", this.name, curMixer.getMixerInfo().getName());
				break;
			}
		}
		if (this.mixer == null) {
			logger.debug("Platform: {}: changing mixer to {}", this.name, null);
		}
		this.mixerChecked = true;
	}

	@Override
	public String toString() {
		return this.name; // $NON-NLS-1$
	}

	private void setMixer(Mixer soundMixer) {
		logger.debug("SETTING platform {}: soundMixer={}", System.identityHashCode(this),
		        soundMixer == null ? null : soundMixer.getLineInfo());
		this.mixer = soundMixer;
	}

}
