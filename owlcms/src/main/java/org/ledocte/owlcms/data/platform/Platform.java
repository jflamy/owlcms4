/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.platform;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinSession;

import ch.qos.logback.classic.Logger;

/**
 * Data on a lifting site.
 * <p>
 * Groups are associated with a lifting platformName.
 * </p>
 * <p>
 * Projectors and officials are associated with a lifting platformName so there is no need to refresh their setup during a competition. The
 * name of the platformName is used as a key in the ServletContext so other sessions and other kinds of pages (such as JSP) can locate the
 * information about that platformName. See in particular the {@link LiftList#updateTable()} method
 * </p>
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
@Entity
@Cacheable
public class Platform implements Serializable {
	
    @SuppressWarnings("unused")
	private static final Logger logger = (Logger) LoggerFactory.getLogger(Platform.class);

	/**
	 * Only used for unit testing when there is no session
	 */
	private static Platform testingPlatform;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    String name;

    /**
     * true if the referee use this application to give decisions, and decision lights need to be shown on the attempt and result boards.
     */
    private Boolean showDecisionLights = false;

    /**
     * true if the time should be displayed
     */
    private Boolean showTimer = false;
    

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


//    String mixerName = "";
//    transient private Mixer mixer;

    public Platform() {
    }

    public Platform(String name) {
        this.setName(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Platform other = (Platform) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public Integer getLightBar() {
        if (lightBar == null)
            return 0;
        return lightBar;
    }



    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public Integer getNbC_2_5() {
        if (nbC_2_5 == null)
            return 0;
        return nbC_2_5;
    }

    public Integer getNbL_10() {
        if (nbL_10 == null)
            return 0;
        return nbL_10;
    }

    public Integer getNbL_15() {
        if (nbL_15 == null)
            return 0;
        return nbL_15;
    }

    public Integer getNbL_2_5() {
        if (nbL_2_5 == null)
            return 0;
        return nbL_2_5;
    }

    public Integer getNbL_20() {
        if (nbL_20 == null)
            return 0;
        return nbL_20;
    }

    public Integer getNbL_25() {
        if (nbL_25 == null)
            return 0;
        return nbL_25;
    }

    public Integer getNbL_5() {
        if (nbL_5 == null)
            return 0;
        return nbL_5;
    }

    public Integer getNbS_0_5() {
        if (nbS_0_5 == null)
            return 0;
        return nbS_0_5;
    }

    public Integer getNbS_1() {
        if (nbS_1 == null)
            return 0;
        return nbS_1;
    }

    public Integer getNbS_1_5() {
        if (nbS_1_5 == null)
            return 0;
        return nbS_1_5;
    }

    public Integer getNbS_2() {
        if (nbS_2 == null)
            return 0;
        return nbS_2;
    }

    public Integer getNbS_2_5() {
        if (nbS_2_5 == null)
            return 0;
        return nbS_2_5;
    }

    public Integer getNbS_5() {
        if (nbS_5 == null)
            return 0;
        return nbS_5;
    }

    public Integer getOfficialBar() {
        if (lightBar == null)
            return 0;
        return officialBar;
    }

    public Boolean getShowDecisionLights() {
        return showDecisionLights == null ? false : showDecisionLights;
    }

    public Boolean getShowTimer() {
        boolean b = showTimer == null ? false : showTimer;
        return b;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public void setLightBar(Integer lightBar) {
        this.lightBar = lightBar;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setNbC_2_5(Integer nbC_2_5) {
        this.nbC_2_5 = nbC_2_5;
    }

    public void setNbL_10(Integer nbL_10) {
        this.nbL_10 = nbL_10;
    }

    public void setNbL_15(Integer nbL_15) {
        this.nbL_15 = nbL_15;
    }

    public void setNbL_2_5(Integer nbL_2_5) {
        this.nbL_2_5 = nbL_2_5;
    }

    public void setNbL_20(Integer nbL_20) {
        this.nbL_20 = nbL_20;
    }

    public void setNbL_25(Integer nbL_25) {
        this.nbL_25 = nbL_25;
    }

    public void setNbL_5(Integer nbL_5) {
        this.nbL_5 = nbL_5;
    }

    public void setNbS_0_5(Integer nbS_0_5) {
        this.nbS_0_5 = nbS_0_5;
    }

    public void setNbS_1(Integer nbS_1) {
        this.nbS_1 = nbS_1;
    }

    public void setNbS_1_5(Integer nbS_1_5) {
        this.nbS_1_5 = nbS_1_5;
    }

    public void setNbS_2(Integer nbS_2) {
        this.nbS_2 = nbS_2;
    }

    public void setNbS_2_5(Integer nbS_2_5) {
        this.nbS_2_5 = nbS_2_5;
    }

    public void setNbS_5(Integer nbS_5) {
        this.nbS_5 = nbS_5;
    }

    public void setOfficialBar(Integer officialBar) {
        this.officialBar = officialBar;
    }

    public void setShowDecisionLights(Boolean showDecisionLights) {
        this.showDecisionLights = showDecisionLights;
    }

//    public Mixer getMixer() {
//        if (mixer == null) {
//            setMixerName(this.getMixerName());
//        }
//        return mixer;
//    }

//    public void setMixer(Mixer mixer) {
//        this.mixer = mixer;
//    }

//    /**
//     * @return the mixerName
//     */
//    public String getMixerName() {
//        return mixerName;
//    }
//
//    /**
//     * @param mixerName
//     *            the mixerName to set
//     */
//    public void setMixerName(String mixerName) {
//        this.mixerName = mixerName;
//        setMixer(null);
//        List<Mixer> mixers = Speakers.getOutputs();
//        for (Mixer curMixer : mixers) {
//            if (curMixer.getMixerInfo().getName().equals(mixerName)) {
//                setMixer(curMixer);
//                logger.info("Platform: {}: changing mixer to {}", this.name, curMixer.getMixerInfo().getName());
//                //LoggerUtils.traceBack(logger);
//                break;
//            }
//        }
//        if (mixer == null) {
//            logger.info("Platform: {}: changing mixer to {}", this.name, mixerName);
//        }
//    }

    public void setShowTimer(Boolean showTime) {
        this.showTimer = showTime;
    }

    @Override
    public String toString() {
        return name + "_" + System.identityHashCode(this); //$NON-NLS-1$
    }

	public static Platform getCurrent() {
		//FIXME: need to inject something that hides the implementation and can be mocked
		//should not have anything Vaadin in the data layer.
		VaadinSession current = VaadinSession.getCurrent();
		if (current != null) {
			return (Platform) current.getAttribute("platform");
		} else {
			return testingPlatform;
		}
	}
	
	public static void setCurrent(Platform p) {
		VaadinSession current = VaadinSession.getCurrent();
		if (current != null) {
			current.setAttribute("platform", p);
		} else {
			testingPlatform = p;
		}
	}


}
