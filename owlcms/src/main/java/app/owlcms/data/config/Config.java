/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.config;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class Config.
 */
@Cacheable
@Entity
public class Config {

    public static final int SHORT_TEAM_LENGTH = 6;

    @SuppressWarnings("unused")
    final static private Logger logger = (Logger) LoggerFactory.getLogger(Config.class);

    private static Config current;

    /**
     * Gets the current.
     *
     * @return the current
     */
    public static Config getCurrent() {
        // if (current == null) {
        current = ConfigRepository.findAll().get(0);
        // }
        return current;
    }

    public static Config setCurrent(Config config) {
        current = ConfigRepository.save(config);
        return current;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    private String ipAccessList;

    private String pin;

    private String publicResultsURL;

    private String updatekey;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public String getIpAccessList() {
        return ipAccessList;
    }

    /**
     * @return the current password.
     */
    public String getParamAccessList() {
        String uAccessList = StartupUtils.getStringParam("ip");
        if (uAccessList != null) {
            return uAccessList;
        } else {
            uAccessList = ipAccessList;
            if (uAccessList == null || uAccessList.trim().isEmpty()) {
                return null;
            } else {
                return uAccessList;
            }
        }
    }

    public String getParamDecisionUrl() {
        String paramPublicResultsURL = getParamPublicResultsURL();
        return paramPublicResultsURL != null ? paramPublicResultsURL + "/decision" : null;
    }

    /**
     * @return the current password.
     */
    public String getParamPin() {
        String uPin = StartupUtils.getStringParam("pin");
        if (uPin != null) {
            return uPin;
        } else {
            uPin = pin;
            if (uPin == null || uPin.trim().isEmpty()) {
                return null;
            } else {
                return uPin;
            }
        }
    }

    public String getParamTimerUrl() {
        String paramPublicResultsURL = getParamPublicResultsURL();
        return paramPublicResultsURL != null ? paramPublicResultsURL + "/timer" : null;
    }

    /**
     * @return the updateKey stored in the database, except if overridden by system property or envariable.
     */
    public String getParamUpdateKey() {
        String uKey = StartupUtils.getStringParam("updateKey");
        if (uKey != null) {
            return uKey;
        } else {
            uKey = updatekey;
            if (uKey == null || uKey.trim().isEmpty()) {
                return null;
            } else {
                return uKey;
            }
        }
    }

    public String getParamUpdateUrl() {
        String publicResultsURLParam = getParamPublicResultsURL();
        return publicResultsURLParam != null ? publicResultsURLParam + "/update" : null;
    }

    public String getPin() {
        return pin;
    }

    public String getPublicResultsURL() {
        return publicResultsURL;
    }

    public String getUpdatekey() {
        return updatekey;
    }

    public void setIpAccessList(String ipAccessList) {
        this.ipAccessList = ipAccessList;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public void setPublicResultsURL(String publicResultsURL) {
        this.publicResultsURL = publicResultsURL;
    }

    public void setUpdatekey(String updatekey) {
        this.updatekey = updatekey;
    }

    /**
     * @return the public results url stored in the database, except if overridden by system property or envariable.
     */
    private String getParamPublicResultsURL() {
        String uURL = StartupUtils.getStringParam("remote");
        if (uURL != null) {
            // old configs with environment variable may still have a trailing /update.
            uURL = uURL.replaceFirst("/update$", "");
            return uURL;
        } else {
            uURL = publicResultsURL;
            if (uURL == null || uURL.trim().isEmpty()) {
                return null;
            } else {
                // user may have copied URL with trailing /
                uURL = uURL.replaceFirst("/$", "");
                return uURL;
            }
        }
    }

}
