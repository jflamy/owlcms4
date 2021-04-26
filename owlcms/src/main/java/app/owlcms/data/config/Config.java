/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.config;

import java.util.TimeZone;

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

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
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

    private String ipBackdoorList;

    private String timeZone;

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
        Config other = (Config) obj;
        return id != null && id.equals(other.getId());
    }

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

    public String getIpBackdoorList() {
        return ipBackdoorList;
    }

    /**
     * @return the current whitelist.
     */
    public String getParamAccessList() {
        String uAccessList = StartupUtils.getStringParam("ip");
        if (uAccessList == null) {
            // use access list from database
            uAccessList = Config.getCurrent().ipAccessList;
            if (uAccessList == null || uAccessList.isBlank()) {
                uAccessList = null;
            }
        }
        return uAccessList;
    }

    /**
     * @return the current whitelist.
     */
    public String getParamBackdoorList() {
        String uAccessList = StartupUtils.getStringParam("backdoor");
        if (uAccessList == null) {
            // use access list from database
            uAccessList = Config.getCurrent().ipBackdoorList;
            if (uAccessList == null || uAccessList.isBlank()) {
                uAccessList = null;
            }
        }
        return uAccessList;
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
        if (uPin == null) {
            // use pin from database
            uPin = Config.getCurrent().pin;
            // logger.debug("pin = {}", uPin);
            if (uPin == null || uPin.isBlank()) {
                uPin = null;
            }
        }
        return uPin;
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
        if (uKey == null) {
            // use pin from database
            uKey = Config.getCurrent().updatekey;
            if (uKey == null || uKey.isBlank()) {
                uKey = null;
            }
        }
        return uKey;
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

    public TimeZone getTimeZone() {
        if (timeZone == null) {
            return null;
        } else {
            return TimeZone.getTimeZone(timeZone);
        }
    }

    public String getUpdatekey() {
        return updatekey;
    }

    @Override
    public int hashCode() {
        // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return 31;
    }

    public void setIpAccessList(String ipAccessList) {
        this.ipAccessList = ipAccessList;
    }

    public void setIpBackdoorList(String ipBackdoorList) {
        this.ipBackdoorList = ipBackdoorList;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public void setPublicResultsURL(String publicResultsURL) {
        this.publicResultsURL = publicResultsURL;
    }

    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null) {
            this.timeZone = null;
            return;
        } else {
            this.timeZone = timeZone.getID();
        }
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
            if (uURL == null || uURL.isBlank()) {
                return null;
            } else {
                // user may have copied URL with trailing /
                uURL = uURL.replaceFirst("/$", "");
                return uURL;
            }
        }
    }

}
