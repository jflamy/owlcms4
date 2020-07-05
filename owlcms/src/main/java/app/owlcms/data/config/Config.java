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
    private static Config config;

    @SuppressWarnings("unused")
    final static private Logger logger = (Logger) LoggerFactory.getLogger(Config.class);

    /**
     * Gets the current.
     *
     * @return the current
     */
    public static Config getCurrent() {
        if (config == null) {
            config = ConfigRepository.findAll().get(0);
        }
        return config;
    }

    public static void setCurrent(Config c) {
        config = c;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    private String ipAccessList;
    private String pin;
    private String publicResultsURL;

    public String getIpAccessList() {
        return ipAccessList;
    }

    public void setIpAccessList(String ipAccessList) {
        this.ipAccessList = ipAccessList;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getPublicResultsURL() {
        return publicResultsURL;
    }

    public void setPublicResultsURL(String publicResultsURL) {
        this.publicResultsURL = publicResultsURL;
    }

    public String getUpdatekey() {
        return updatekey;
    }

    public void setUpdatekey(String updatekey) {
        this.updatekey = updatekey;
    }

    private String updatekey;

    /**
     * @return the updateKey stored in the database, except if overridden by system property or envariable.
     */
    public static String getUpdateKeyParam() {
        String uKey = StartupUtils.getStringParam("updateKey");
        if (uKey != null) {
            return uKey;
        } else {
            uKey = getCurrent().getUpdatekey();
            if (uKey == null || uKey.trim().isEmpty()) {
                return null;
            } else {
                return uKey;
            }
        }
    }

    /**
     * @return the public results url stored in the database, except if overridden by system property or envariable.
     */
    public static String getUpdateURLParam() {
        String uURL = StartupUtils.getStringParam("remote");
        if (uURL != null) {
            return uURL;
        } else {
            uURL = getCurrent().getPublicResultsURL();
            if (uURL == null || uURL.trim().isEmpty()) {
                return null;
            } else {
                return uURL;
            }
        }
    }
}
