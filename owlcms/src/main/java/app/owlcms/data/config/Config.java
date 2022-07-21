/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.hibernate.engine.jdbc.BlobProxy;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.LocaleAttributeConverter;
import app.owlcms.servlet.FileServlet;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class Config.
 */
@Cacheable

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Config {

    public static final int SHORT_TEAM_LENGTH = 6;

    private static Config current;

    @Transient
    final static private Logger logger = (Logger) LoggerFactory.getLogger(Config.class);

    /**
     * Gets the current.
     *
     * @return the current
     */
    public static Config getCurrent() {
        // *******
        current = ConfigRepository.findAll().get(0);
        current.setSkipReading(false);
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

    public static Config setCurrent(Config config) {
        current = ConfigRepository.save(config);
        return current;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Column(columnDefinition = "boolean default false")
    private boolean clearZip;

    @Convert(converter = LocaleAttributeConverter.class)
    private Locale defaultLocale = null;

    private String ipAccessList;

    private String ipBackdoorList;

    /**
     * Local Override: a zip file that is used to override resources, stored as a blob
     */
    @Lob
    @Column(name = "localcontent", nullable = true)
    private Blob localOverride;

    private String pin;

    private String publicResultsURL;

    private String salt;

    private String timeZoneId;

    private Boolean traceMemory;

    private String updatekey;

    @Transient
    @JsonIgnore
    private boolean skipReading;

    private String featureSwitches;

    public String computeSalt() {
        this.setSalt(null);
        return Integer.toHexString(new Random(System.currentTimeMillis()).nextInt());
    }

    public String encodeUserPassword(String password) {
        String uPin = StartupUtils.getStringParam("pin");
        if (uPin == null) {
            String encodedPassword = AccessUtils.encodePin(password, true);
            return encodedPassword;
        } else {
            // we are comparing cleartext
            return password;
        }
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
     * Gets the default locale.
     *
     * @return the default locale
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
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
     * Gets the locale.
     *
     * @return the locale
     */
    @Transient
    @JsonIgnore
    public Locale getLocale() {
        return getDefaultLocale();
    }

    /**
     * @return zip file containing a zipped ./local structure to override resources
     * @throws SQLException
     */
    public byte[] getLocalZipBlob() {
        //logger.debug("getLocalZipBlob skip={}",skipReading);
        if (localOverride == null || skipReading) {
            return null;
        }

        return JPAService.runInTransaction(em -> {
            try {
                Config thisConfig = em.find(Config.class, this.id);
                byte[] res = thisConfig.localOverride.getBytes(1, (int) localOverride.length());
                logger.debug("getLocalZipBlob read {} bytes", res.length);
                return res;
            } catch (SQLException e) {
                em.getTransaction().rollback();
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * @return the current whitelist.
     */
    @Transient
    @JsonIgnore
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
    @Transient
    @JsonIgnore
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

    @Transient
    @JsonIgnore
    public String getParamDecisionUrl() {
        String paramPublicResultsURL = getParamPublicResultsURL();
        return paramPublicResultsURL != null ? paramPublicResultsURL + "/decision" : null;
    }

    /**
     * @return the current password.
     */
    @Transient
    @JsonIgnore
    public String getParamPin() {
        String uPin = StartupUtils.getStringParam("pin");
        if (uPin == null) {
            // not defined in environment
            // use pin from database, which is either empty (no password required)
            // or legacy (not crypted) or current.
            uPin = Config.getCurrent().getPin();
            // logger.debug("pin = {}", uPin);
            if (uPin == null || uPin.isBlank()) {
                return null;
            } else if (uPin.length() < 64) {
                // assume legacy
                String encodedPin = AccessUtils.encodePin(uPin, false);
                return encodedPin;
            } else {
                return uPin; // what is in the database is already
            }
        } else if (uPin.isBlank()) {
            // no password will be expected
            return null;
        } else {
            return uPin;
        }
    }

    @Transient
    @JsonIgnore
    public String getParamTimerUrl() {
        String paramPublicResultsURL = getParamPublicResultsURL();
        return paramPublicResultsURL != null ? paramPublicResultsURL + "/timer" : null;
    }

    /**
     * @return the updateKey stored in the database, except if overridden by system property or envariable.
     */
    @Transient
    @JsonIgnore
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

    @Transient
    @JsonIgnore
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

    public String getSalt() {
        return this.salt;
    }

    public TimeZone getTimeZone() {
        if (timeZoneId == null) {
            return null;
        } else {
            return TimeZone.getTimeZone(timeZoneId);
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

    @Transient
    @JsonIgnore
    public boolean isClearZip() {
        if (localOverride == null) {
            clearZip = false;
        }
        return clearZip;
    }

    @Transient
    @JsonIgnore
    public boolean isIgnoreCaching() {
        return FileServlet.isIgnoreCaching();
    }

    @Transient
    @JsonIgnore
    public boolean isTraceMemory() {
        if (traceMemory == null) {
            traceMemory = StartupUtils.getBooleanParam("traceMemory");
        }
        return Boolean.TRUE.equals(traceMemory);
    }

    public void setClearZip(boolean clearZipRequested) {
        this.clearZip = clearZipRequested;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public void setIgnoreCaching(boolean ignoreCaching) {
        FileServlet.setIgnoreCaching(ignoreCaching);
    }

    public void setIpAccessList(String ipAccessList) {
        this.ipAccessList = ipAccessList;
    }

    public void setIpBackdoorList(String ipBackdoorList) {
        this.ipBackdoorList = ipBackdoorList;
    }

    public void setLocalZipBlob(byte[] localContent) {
        if (this.clearZip) {
            this.localOverride = null;
            this.clearZip = false;
        } else if (localContent != null) {
            logger.debug("setting {}", localContent.length);
            this.localOverride = BlobProxy.generateProxy(localContent);
        } else {
            this.localOverride = null;
        }
    }

    public void setPin(String pin) {
        if (pin != null && pin.length() != 64) {
            this.pin = AccessUtils.encodePin(pin, false);
        } else {
            this.pin = pin;
        }
    }

    public void setPublicResultsURL(String publicResultsURL) {
        this.publicResultsURL = publicResultsURL;
    }

    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null) {
            this.timeZoneId = null;
            return;
        } else {
            this.timeZoneId = timeZone.getID();
        }
    }

    public void setUpdatekey(String updatekey) {
        this.updatekey = updatekey;
    }

    /**
     * @return the public results url stored in the database, except if overridden by system property or envariable.
     */
    public String getParamPublicResultsURL() {
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

    /**
     * @param salt the salt to set
     */
    private void setSalt(String salt) {
        this.salt = salt;
        logger.debug("setting salt to {}", this.salt);
    }

    public void setSkipReading(boolean b) {
        this.skipReading = b;   
    }

    public boolean featureSwitch(String string, boolean trueIfPresent) {
        if (getFeatureSwitches() == null) {
            return !trueIfPresent;
        }
        String[] switches = getFeatureSwitches().split("[,; ]");
        boolean present = Arrays.asList(switches).contains(string);
        return trueIfPresent ? present : !present;
    }

    public String getFeatureSwitches() {
        return featureSwitches;
    }

    public void setFeatureSwitches(String featureSwitches) {
        this.featureSwitches = featureSwitches;
    }

    public boolean isSizeOverride() {
        //return featureSwitch("sizeOverride", true);
        return true;
    }

    public boolean isOldScoreboards() {
        return featureSwitch("oldScoreboards", true);
    }

}
