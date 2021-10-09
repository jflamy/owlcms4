/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.LocaleAttributeConverter;
import app.owlcms.init.FileServlet;
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
    
    private String timeZoneId;

    @SuppressWarnings("unused")
    final static private Logger logger = (Logger) LoggerFactory.getLogger(Config.class);

    private static Config current;

    /**
     * Gets the current.
     *
     * @return the current
     */
    public static Config getCurrent() {
        current = ConfigRepository.findAll().get(0);
        return current;
    }

    public static Config setCurrent(Config config) {
        current = ConfigRepository.save(config);
        return current;
    }
    
    public static void initConfig() {
        JPAService.runInTransaction(em -> {
            if (ConfigRepository.findAll().isEmpty()) {
                Config config = new Config();
                Config.setCurrent(config);
            }
            return null;
        });
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    private String ipAccessList;

    private String pin;

    private String publicResultsURL;

    private String updatekey;

    private String ipBackdoorList;

    /**
     * Local Override: a zip file that is used to override resources, stored as a blob
     */
    @Lob
    @Column(name = "localcontent", nullable = true)
    private byte[] localOverride;

    @Column(columnDefinition = "boolean default false")
    private boolean clearZip;

    @Convert(converter = LocaleAttributeConverter.class)
    private Locale defaultLocale = null;
    
    public boolean isClearZip() {
        if (localOverride == null || localOverride.length == 0) {
            clearZip = false;
        }
        return clearZip;
    }

    /**
     * @return zip file containing a zipped ./local structure to override resources
     */
    public byte[] getLocalOverride() {
        return localOverride;
    }
    
    public void setClearZip(boolean clearZipRequested) {
        this.clearZip = clearZipRequested;
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

    public boolean isIgnoreCaching() {
        return FileServlet.isIgnoreCaching();
    }

    public void setIgnoreCaching(boolean ignoreCaching) {
        FileServlet.setIgnoreCaching(ignoreCaching);
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

    public void setLocalOverride(byte[] localContent) {
        if (this.clearZip) {
            this.localOverride = null;
            this.clearZip = false;
        } else {
            this.localOverride = localContent;
        }
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
            if (uURL == null || uURL.isBlank()) {
                return null;
            } else {
                // user may have copied URL with trailing /
                uURL = uURL.replaceFirst("/$", "");
                return uURL;
            }
        }
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * Gets the default locale.
     *
     * @return the default locale
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Gets the locale.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return getDefaultLocale();
    }
    
    public TimeZone getTimeZone() {
        if (timeZoneId == null) {
            return null;
        } else {
            return TimeZone.getTimeZone(timeZoneId);
        }
    }
    
    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null) {
            this.timeZoneId = null;
            return;
        } else {
            this.timeZoneId = timeZone.getID();
        }
    }

}
