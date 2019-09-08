/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
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

import org.slf4j.LoggerFactory;

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
 * Projectors and officials are associated with a lifting platformName so there
 * is no need to refresh their setup during a competition. The name of the
 * platformName is used as a key in the ServletContext so other sessions and
 * other kinds of pages (such as JSP) can locate the information about that
 * platformName. See in particular the {@link LiftList#updateTable()} method
 * </p>
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
@Entity
@Cacheable
public class Platform implements Serializable {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Platform.class);

    /**
     * Only used for unit testing when there is no session
     */
    private static Platform testingPlatform;

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    /** The name. */
    String name;

    /**
     * true if the referee use this application to give decisions, and decision
     * lights need to be shown on the attempt and result boards.
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
    private Mixer mixer = null;
    private String soundMixerName;
    @Transient
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
        if (lightBar == null)
            return 0;
        return lightBar;
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
        if (nbC_2_5 == null)
            return 0;
        return nbC_2_5;
    }

    /**
     * Gets the nb L 10.
     *
     * @return the nb L 10
     */
    public Integer getNbL_10() {
        if (nbL_10 == null)
            return 0;
        return nbL_10;
    }

    /**
     * Gets the nb L 15.
     *
     * @return the nb L 15
     */
    public Integer getNbL_15() {
        if (nbL_15 == null)
            return 0;
        return nbL_15;
    }

    /**
     * Gets the nb L 2 5.
     *
     * @return the nb L 2 5
     */
    public Integer getNbL_2_5() {
        if (nbL_2_5 == null)
            return 0;
        return nbL_2_5;
    }

    /**
     * Gets the nb L 20.
     *
     * @return the nb L 20
     */
    public Integer getNbL_20() {
        if (nbL_20 == null)
            return 0;
        return nbL_20;
    }

    /**
     * Gets the nb L 25.
     *
     * @return the nb L 25
     */
    public Integer getNbL_25() {
        if (nbL_25 == null)
            return 0;
        return nbL_25;
    }

    /**
     * Gets the nb L 5.
     *
     * @return the nb L 5
     */
    public Integer getNbL_5() {
        if (nbL_5 == null)
            return 0;
        return nbL_5;
    }

    /**
     * Gets the nb S 0 5.
     *
     * @return the nb S 0 5
     */
    public Integer getNbS_0_5() {
        if (nbS_0_5 == null)
            return 0;
        return nbS_0_5;
    }

    /**
     * Gets the nb S 1.
     *
     * @return the nb S 1
     */
    public Integer getNbS_1() {
        if (nbS_1 == null)
            return 0;
        return nbS_1;
    }

    /**
     * Gets the nb S 1 5.
     *
     * @return the nb S 1 5
     */
    public Integer getNbS_1_5() {
        if (nbS_1_5 == null)
            return 0;
        return nbS_1_5;
    }

    /**
     * Gets the nb S 2.
     *
     * @return the nb S 2
     */
    public Integer getNbS_2() {
        if (nbS_2 == null)
            return 0;
        return nbS_2;
    }

    /**
     * Gets the nb S 2 5.
     *
     * @return the nb S 2 5
     */
    public Integer getNbS_2_5() {
        if (nbS_2_5 == null)
            return 0;
        return nbS_2_5;
    }

    /**
     * Gets the nb S 5.
     *
     * @return the nb S 5
     */
    public Integer getNbS_5() {
        if (nbS_5 == null)
            return 0;
        return nbS_5;
    }

    /**
     * Gets the official bar.
     *
     * @return the official bar
     */
    public Integer getOfficialBar() {
        if (lightBar == null)
            return 0;
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


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((lightBar == null) ? 0 : lightBar.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nbC_2_5 == null) ? 0 : nbC_2_5.hashCode());
        result = prime * result + ((nbL_10 == null) ? 0 : nbL_10.hashCode());
        result = prime * result + ((nbL_15 == null) ? 0 : nbL_15.hashCode());
        result = prime * result + ((nbL_20 == null) ? 0 : nbL_20.hashCode());
        result = prime * result + ((nbL_25 == null) ? 0 : nbL_25.hashCode());
        result = prime * result + ((nbL_2_5 == null) ? 0 : nbL_2_5.hashCode());
        result = prime * result + ((nbL_5 == null) ? 0 : nbL_5.hashCode());
        result = prime * result + ((nbS_0_5 == null) ? 0 : nbS_0_5.hashCode());
        result = prime * result + ((nbS_1 == null) ? 0 : nbS_1.hashCode());
        result = prime * result + ((nbS_1_5 == null) ? 0 : nbS_1_5.hashCode());
        result = prime * result + ((nbS_2 == null) ? 0 : nbS_2.hashCode());
        result = prime * result + ((nbS_2_5 == null) ? 0 : nbS_2_5.hashCode());
        result = prime * result + ((nbS_5 == null) ? 0 : nbS_5.hashCode());
        result = prime * result + ((officialBar == null) ? 0 : officialBar.hashCode());
        result = prime * result + ((showDecisionLights == null) ? 0 : showDecisionLights.hashCode());
        result = prime * result + ((showTimer == null) ? 0 : showTimer.hashCode());
        result = prime * result + ((soundMixerName == null) ? 0 : soundMixerName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Platform other = (Platform) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (lightBar == null) {
            if (other.lightBar != null)
                return false;
        } else if (!lightBar.equals(other.lightBar))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nbC_2_5 == null) {
            if (other.nbC_2_5 != null)
                return false;
        } else if (!nbC_2_5.equals(other.nbC_2_5))
            return false;
        if (nbL_10 == null) {
            if (other.nbL_10 != null)
                return false;
        } else if (!nbL_10.equals(other.nbL_10))
            return false;
        if (nbL_15 == null) {
            if (other.nbL_15 != null)
                return false;
        } else if (!nbL_15.equals(other.nbL_15))
            return false;
        if (nbL_20 == null) {
            if (other.nbL_20 != null)
                return false;
        } else if (!nbL_20.equals(other.nbL_20))
            return false;
        if (nbL_25 == null) {
            if (other.nbL_25 != null)
                return false;
        } else if (!nbL_25.equals(other.nbL_25))
            return false;
        if (nbL_2_5 == null) {
            if (other.nbL_2_5 != null)
                return false;
        } else if (!nbL_2_5.equals(other.nbL_2_5))
            return false;
        if (nbL_5 == null) {
            if (other.nbL_5 != null)
                return false;
        } else if (!nbL_5.equals(other.nbL_5))
            return false;
        if (nbS_0_5 == null) {
            if (other.nbS_0_5 != null)
                return false;
        } else if (!nbS_0_5.equals(other.nbS_0_5))
            return false;
        if (nbS_1 == null) {
            if (other.nbS_1 != null)
                return false;
        } else if (!nbS_1.equals(other.nbS_1))
            return false;
        if (nbS_1_5 == null) {
            if (other.nbS_1_5 != null)
                return false;
        } else if (!nbS_1_5.equals(other.nbS_1_5))
            return false;
        if (nbS_2 == null) {
            if (other.nbS_2 != null)
                return false;
        } else if (!nbS_2.equals(other.nbS_2))
            return false;
        if (nbS_2_5 == null) {
            if (other.nbS_2_5 != null)
                return false;
        } else if (!nbS_2_5.equals(other.nbS_2_5))
            return false;
        if (nbS_5 == null) {
            if (other.nbS_5 != null)
                return false;
        } else if (!nbS_5.equals(other.nbS_5))
            return false;
        if (officialBar == null) {
            if (other.officialBar != null)
                return false;
        } else if (!officialBar.equals(other.officialBar))
            return false;
        if (showDecisionLights == null) {
            if (other.showDecisionLights != null)
                return false;
        } else if (!showDecisionLights.equals(other.showDecisionLights))
            return false;
        if (showTimer == null) {
            if (other.showTimer != null)
                return false;
        } else if (!showTimer.equals(other.showTimer))
            return false;
        if (soundMixerName == null) {
            if (other.soundMixerName != null)
                return false;
        } else if (!soundMixerName.equals(other.soundMixerName))
            return false;
        return true;
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

    public Mixer getMixer() {
        if (!mixerChecked && mixer == null) {
            setSoundMixerName(this.getSoundMixerName());
        }
        return mixer;
    }

    private void setMixer(Mixer soundMixer) {
        logger.debug("SETTING platform {}: soundMixer={}", System.identityHashCode(this),soundMixer == null ? null : soundMixer.getLineInfo());
        this.mixer = soundMixer;
    }

    /**
     * @return the soundMixerName
     */
    public String getSoundMixerName() {
        logger.debug("getSoundMixerName {} {}", System.identityHashCode(this),soundMixerName);
        if (soundMixerName == null) {
            return Translator.translate("UseBrowserSound");
        }
        return soundMixerName;
    }

    /**
     * @param soundMixerName
     *            the soundMixerName to set
     */
    public void setSoundMixerName(String soundMixerName) {
        logger.debug("setSoundMixerName {} {} {}", System.identityHashCode(this),soundMixerName,  LoggerUtils.whereFrom());
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
                logger.debug("Platform {}: changing mixer to {}", this.name, curMixer.getMixerInfo().getName());
                break;
            }
        }
        if (mixer == null) {
            logger.debug("Platform: {}: changing mixer to {}", this.name, soundMixerName);
        }
        mixerChecked = true;
    }

    /**
     * Sets the show timer.
     *
     * @param showTime the new show timer
     */
    public void setShowTimer(Boolean showTime) {
        this.showTimer = showTime;
    }

    @Override
    public String toString() {
        return name; // $NON-NLS-1$
    }

    /**
     * Gets the current platform
     *
     * @return the current
     */
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
    public static void setCurrent(Platform p) {
        VaadinSession current = VaadinSession.getCurrent();
        if (current != null) {
            current.setAttribute("platform", p);
        } else {
            testingPlatform = p;
        }
    }
    
    
    public boolean isNonStandardBar() {
        return nonStandardBar;
    }

    public void setNonStandardBar(boolean nonStandardBar) {
        this.nonStandardBar = nonStandardBar;
    }


}
