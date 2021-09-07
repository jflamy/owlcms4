/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.StartupUtils;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class Config.
 */
@Cacheable

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
public class Config {

    public static final int SHORT_TEAM_LENGTH = 6;

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

    @Lob
    @Column(name = "localcontent", columnDefinition = "BLOB", nullable = true)
    private byte[] localContent;

    @Transient
    private boolean initializedLocalDir = false;

    @Transient
    private Path localDirPath = null;

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

    public byte[] getLocalContent() {
        return localContent;
    }

    public Path getLocalDirPath() {
        return localDirPath;
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

    public void initLocalDir() {
        byte[] localContent2 = this.getLocalContent();
        if (localContent2 != null && localContent2.length > 0) {
            logger.debug("override blob found");
            try {
                unzipBlobToTemp(localContent2);
            } catch (Exception e) {
                checkForLocalOverrideDirectory();
            }
        } else {
            checkForLocalOverrideDirectory();
        }
        setInitializedLocalDir(true);
    }

    public boolean isInitializedLocalDir() {
        return initializedLocalDir;
    }

    public void setIpAccessList(String ipAccessList) {
        this.ipAccessList = ipAccessList;
    }

    public void setIpBackdoorList(String ipBackdoorList) {
        this.ipBackdoorList = ipBackdoorList;
    }

    public void setLocalContent(byte[] localContent) {
        this.localContent = localContent;
    }

    public void setLocalDirPath(Path curDir) {
        this.localDirPath = curDir;
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

    private void checkForLocalOverrideDirectory() {
        Path curDir = Paths.get(".", "local");
        curDir = curDir.normalize();
        if (Files.exists(curDir)) {
            logger.debug("local override directory = {}", curDir.toAbsolutePath());
            setLocalDirPath(curDir);
        } else {
            logger.debug("no override directory {}", curDir.toAbsolutePath());
        }
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

    private void setInitializedLocalDir(boolean checkedLocalDir) {
        this.initializedLocalDir = checkedLocalDir;
    }

    private void unzipBlobToTemp(byte[] localContent2) throws Exception {
        Path f = null;
        try {
            f = Files.createTempDirectory("owlcms");
            logger.debug("created temp directory " + f);
        } catch (IOException e) {
            throw new Exception("cannot create directory ", e);
        }
        try {
            ZipUtils.unzip(new ByteArrayInputStream(localContent2), f.toFile());
            setLocalDirPath(f);
        } catch (IOException e) {
            throw new Exception("cannot unzip", e);
        }
    }

}
