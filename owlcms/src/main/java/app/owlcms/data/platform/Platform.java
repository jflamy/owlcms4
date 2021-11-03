/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.platform;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.sound.sampled.Mixer;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.sound.Speakers;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Data on a lifting site.
 * <p>
 * Groups are associated with a lifting platformName.
 * </p>
 * <p>
 * Projectors and officials are associated with a lifting platformName so there is no need to refresh their setup during
 * a competition. The name of the platformName is used as a key in the ServletContext so other sessions and other kinds
 * of pages (such as JSP) can locate the information about that platformName. See in particular the
 * {@link LiftList#updateTable()} method
 * </p>
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name", scope = Platform.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "logger"})
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

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    Long id;

    /** The name. */
    String name;

    /**
     * true if the referee use this application to give decisions, and decision lights need to be shown on the attempt
     * and result boards.
     */
    private Boolean showDecisionLights = false;
    /**
     * true if the time should be displayed
     */
    private Boolean showTimer = false;
    /**
     * If mixer is not null, emit sound on the associated device
     */
    @Transient
    @JsonIgnore
    private Mixer mixer = null;

    private String soundMixerName;

    @Transient
    @JsonIgnore
    private boolean mixerChecked;
    // collar
    private Integer nbC_2_5 = 0;
    // small plates
    private Integer nbS_0_5 = 0;
    private Integer nbS_1 = 0;
    private Integer nbS_1_5 = 0;
    private Integer nbS_2 = 0;

    private Integer nbS_2_5 = 0;
    private Integer nbS_5 = 0;
    // large plates
    private Integer nbL_2_5 = 0;
    private Integer nbL_5 = 0;
    private Integer nbL_10 = 0;
    private Integer nbL_15 = 0;

    private Integer nbL_20 = 0;
    private Integer nbL_25 = 0;
    // bar
    private Integer officialBar = 0;

    private Integer lightBar = 0;

    private boolean nonStandardBar;

    /**
     * Instantiates a new platform.
     */
    public Platform() {
    }

    /**
     * Instantiates a new platform.
     *
     * @param name the name
     */
    public Platform(String name) {
        this();
        this.setName(name);
    }

    @Override
    public int compareTo(Platform o) {
        return ObjectUtils.compare(this.getName(), o.getName(), true);
    }

    @Override
    public boolean equals(Object obj) {
        // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Platform other = (Platform) obj;
        return id != null && id.equals(other.getId());

        // @Override
        // public boolean equals(Object obj) {
        // if (this == obj) {
        // return true;
        // }
        // if (obj == null) {
        // return false;
        // }
        // if (getClass() != obj.getClass()) {
        // return false;
        // }
        // Platform other = (Platform) obj;
        // return Objects.equals(id, other.id) && Objects.equals(lightBar, other.lightBar)
        // && mixerChecked == other.mixerChecked && Objects.equals(name, other.name)
        // && Objects.equals(nbC_2_5, other.nbC_2_5) && Objects.equals(nbL_10, other.nbL_10)
        // && Objects.equals(nbL_15, other.nbL_15) && Objects.equals(nbL_20, other.nbL_20)
        // && Objects.equals(nbL_25, other.nbL_25) && Objects.equals(nbL_2_5, other.nbL_2_5)
        // && Objects.equals(nbL_5, other.nbL_5) && Objects.equals(nbS_0_5, other.nbS_0_5)
        // && Objects.equals(nbS_1, other.nbS_1) && Objects.equals(nbS_1_5, other.nbS_1_5)
        // && Objects.equals(nbS_2, other.nbS_2) && Objects.equals(nbS_2_5, other.nbS_2_5)
        // && Objects.equals(nbS_5, other.nbS_5) && nonStandardBar == other.nonStandardBar
        // && Objects.equals(officialBar, other.officialBar)
        // && Objects.equals(showDecisionLights, other.showDecisionLights)
        // && Objects.equals(showTimer, other.showTimer) && Objects.equals(soundMixerName, other.soundMixerName);
        // }
        //
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the light bar.
     *
     * @return the light bar
     */
    public Integer getLightBar() {
        if (lightBar == null) {
            return 0;
        }
        return lightBar;
    }

    public Mixer getMixer() {
        if (!mixerChecked && mixer == null) {
            setSoundMixerName(this.getSoundMixerName());
        }
        return mixer;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the nb C 2 5.
     *
     * @return the nb C 2 5
     */
    public Integer getNbC_2_5() {
        if (nbC_2_5 == null) {
            return 0;
        }
        return nbC_2_5;
    }

    /**
     * Gets the nb L 10.
     *
     * @return the nb L 10
     */
    public Integer getNbL_10() {
        if (nbL_10 == null) {
            return 0;
        }
        return nbL_10;
    }

    /**
     * Gets the nb L 15.
     *
     * @return the nb L 15
     */
    public Integer getNbL_15() {
        if (nbL_15 == null) {
            return 0;
        }
        return nbL_15;
    }

    /**
     * Gets the nb L 2 5.
     *
     * @return the nb L 2 5
     */
    public Integer getNbL_2_5() {
        if (nbL_2_5 == null) {
            return 0;
        }
        return nbL_2_5;
    }

    /**
     * Gets the nb L 20.
     *
     * @return the nb L 20
     */
    public Integer getNbL_20() {
        if (nbL_20 == null) {
            return 0;
        }
        return nbL_20;
    }

    /**
     * Gets the nb L 25.
     *
     * @return the nb L 25
     */
    public Integer getNbL_25() {
        if (nbL_25 == null) {
            return 0;
        }
        return nbL_25;
    }

    /**
     * Gets the nb L 5.
     *
     * @return the nb L 5
     */
    public Integer getNbL_5() {
        if (nbL_5 == null) {
            return 0;
        }
        return nbL_5;
    }

    /**
     * Gets the nb S 0 5.
     *
     * @return the nb S 0 5
     */
    public Integer getNbS_0_5() {
        if (nbS_0_5 == null) {
            return 0;
        }
        return nbS_0_5;
    }

    /**
     * Gets the nb S 1.
     *
     * @return the nb S 1
     */
    public Integer getNbS_1() {
        if (nbS_1 == null) {
            return 0;
        }
        return nbS_1;
    }

    /**
     * Gets the nb S 1 5.
     *
     * @return the nb S 1 5
     */
    public Integer getNbS_1_5() {
        if (nbS_1_5 == null) {
            return 0;
        }
        return nbS_1_5;
    }

    /**
     * Gets the nb S 2.
     *
     * @return the nb S 2
     */
    public Integer getNbS_2() {
        if (nbS_2 == null) {
            return 0;
        }
        return nbS_2;
    }

    /**
     * Gets the nb S 2 5.
     *
     * @return the nb S 2 5
     */
    public Integer getNbS_2_5() {
        if (nbS_2_5 == null) {
            return 0;
        }
        return nbS_2_5;
    }

    /**
     * Gets the nb S 5.
     *
     * @return the nb S 5
     */
    public Integer getNbS_5() {
        if (nbS_5 == null) {
            return 0;
        }
        return nbS_5;
    }

    /**
     * Gets the official bar.
     *
     * @return the official bar
     */
    public Integer getOfficialBar() {
        if (lightBar == null) {
            return 0;
        }
        return officialBar;
    }

    /**
     * Gets the show decision lights.
     *
     * @return the show decision lights
     */
    public Boolean getShowDecisionLights() {
        return showDecisionLights == null ? false : showDecisionLights;
    }

    /**
     * Gets the show timer.
     *
     * @return the show timer
     */
    public Boolean getShowTimer() {
        boolean b = showTimer == null ? false : showTimer;
        return b;
    }

    /**
     * @return the soundMixerName
     */
    public String getSoundMixerName() {
        logger.debug("getSoundMixerName {} {}", System.identityHashCode(this), soundMixerName);
        if (soundMixerName == null) {
            return Translator.translate("UseBrowserSound");
        }
        return soundMixerName;
    }

    @Override
    public int hashCode() {
        // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return 31;
    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(id, lightBar, mixerChecked, name, nbC_2_5, nbL_10, nbL_15, nbL_20, nbL_25, nbL_2_5, nbL_5,
//                nbS_0_5, nbS_1, nbS_1_5, nbS_2, nbS_2_5, nbS_5, nonStandardBar, officialBar, showDecisionLights,
//                showTimer, soundMixerName);
//    }

    public boolean isNonStandardBar() {
        return nonStandardBar;
    }

    /**
     * Sets the light bar.
     *
     * @param lightBar the new light bar
     */
    public void setLightBar(Integer lightBar) {
        this.lightBar = lightBar;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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

    public void setNonStandardBar(boolean nonStandardBar) {
        this.nonStandardBar = nonStandardBar;
    }

    /**
     * Sets the official bar.
     *
     * @param officialBar the new official bar
     */
    public void setOfficialBar(Integer officialBar) {
        this.officialBar = officialBar;
    }

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
            mixerChecked = true;
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
        if (mixer == null) {
            logger.debug("Platform: {}: changing mixer to {}", this.name, null);
        }
        mixerChecked = true;
    }

    @Override
    public String toString() {
        return name; // $NON-NLS-1$
    }

    private void setMixer(Mixer soundMixer) {
        logger.debug("SETTING platform {}: soundMixer={}", System.identityHashCode(this),
                soundMixer == null ? null : soundMixer.getLineInfo());
        this.mixer = soundMixer;
    }

}
