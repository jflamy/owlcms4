/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.hibernate.engine.jdbc.BlobProxy;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import app.owlcms.Main;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.LocaleAttributeConverter;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.servlet.FileServlet;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import io.moquette.broker.config.IConfig;

/**
 * Class Config.
 */
@Cacheable

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
@JsonInclude(Include.NON_NULL)
public class Config {

	public static final String FAKE_PIN = "\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF";
	public static final int SHORT_TEAM_LENGTH = 6;
	private static Config current;
	@Transient
	final static private Logger logger = (Logger) LoggerFactory.getLogger(Config.class);

	/**
	 * Gets the current.
	 *
	 * @return the current
	 */
	public static synchronized Config getCurrent() {
		if (current == null) {
			List<Config> all = ConfigRepository.findAll();
			if (all.size() > 1) {
				logger.error("found {} Config", all.size());
			}
			current = all.get(0);
		}
		current.setSkipReading(false);
		return current;
	}

	public static void initConfig() {
		JPAService.runInTransaction(em -> {
			if (ConfigRepository.findAll().isEmpty()) {
				Config config = new Config();
				config.setMqttInternal(true);
				Config.setCurrent(config);
			}
			return null;
		});

		JPAService.runInTransaction(em -> {
			RecordConfig f = RecordConfig.getCurrent();
			if (f == null) {
				RecordConfig rc = new RecordConfig();
				RecordConfig.setCurrent(rc);
			}
			return null;
		});
	}

	public static Config setCurrent(Config config) {
		current = ConfigRepository.save(config);
		return current;
	}

	@Id
	Long id = 1L; // is a singleton. if we ever create a new one it should merge.
	@Column(columnDefinition = "boolean default false")
	private boolean clearZip;
	@Convert(converter = LocaleAttributeConverter.class)
	private Locale defaultLocale = null;
	private String pin;
	private String displayPin;
	private String ipAccessList;
	private String ipDisplayList;
	private String ipBackdoorList;
	private String mqttPort;
	private String mqttUserName;
	private String mqttPassword;
	/**
	 * Local Override: a zip file that is used to override resources, stored as a blob
	 */
	@Lob
	@Column(name = "localcontent", nullable = true)
	private Blob localOverride;
	private String publicResultsURL;
	private String updatekey;
	private String videoDataURL;
	private String videoDataKey;
	private String salt;
	private String timeZoneId;
	private Boolean traceMemory;
	@Transient
	@JsonIgnore
	private boolean skipReading;
	private String featureSwitches;
	@Column(columnDefinition = "boolean default false")
	private boolean localTemplatesOnly;
	@Transient
	@JsonIgnore
	private Boolean useCompetitionDate;
	@Column(columnDefinition = "boolean default true")
	private Boolean mqttInternal = true;
	@Column(columnDefinition = "varchar(255) default 'css/nogrid'")
	private String stylesDirectory;
	@Column(name = "videoStylesDirectory", columnDefinition = "varchar(255) default 'css/nogrid'")
	private String videoStylesDirectory;
	@Transient
	@JsonIgnore
	private IConfig mqttConfig;

	public String computeSalt() {
		this.setSalt(null);
		return Integer.toHexString(new Random(System.currentTimeMillis()).nextInt());
	}

	public String encodeUserPassword(String password, String storedPassword) {
		String encodedPassword = AccessUtils.encodePin(password, storedPassword, true);
		return encodedPassword;
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
		return this.id != null && this.id.equals(other.getId());
	}

	public boolean featureSwitch(String string) {
		return featureSwitch(string, true);
	}

	public boolean featureSwitch(String string, boolean trueIfPresent) {
		String paramFeatureSwitches = getParamFeatureSwitches();
		if (paramFeatureSwitches == null) {
			return !trueIfPresent;
		}
		String[] switches = paramFeatureSwitches.toLowerCase().split("[,; ]");
		//logger.debug("featureSwitches {}",Arrays.asList(switches));
		boolean present = Arrays.asList(switches).contains(string.toLowerCase());
		return trueIfPresent ? present : !present;
	}

	/**
	 * Gets the default locale.
	 *
	 * @return the default locale
	 */
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	public String getDisplayPin() {
		return this.displayPin;
	}

	@Transient
	@JsonIgnore
	public String getDisplayPinForField() {
		if (getDisplayPin() == null) {
			return "";
		} else {
			return FAKE_PIN;
		}
	}

	public String getFeatureSwitches() {
		return this.featureSwitches;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return this.id;
	}

	public String getIpAccessList() {
		return this.ipAccessList;
	}

	public String getIpBackdoorList() {
		return this.ipBackdoorList;
	}

	public String getIpDisplayList() {
		return this.ipDisplayList;
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
		logger.debug("getLocalZipBlob skip={} localOverride={}", this.skipReading, this.localOverride);
		if (this.localOverride == null || this.skipReading) {
			return null;
		}

		return JPAService.runInTransaction(em -> {
			try {
				Config thisConfig = em.find(Config.class, this.id);
				byte[] res = thisConfig.localOverride.getBytes(1, (int) this.localOverride.length());
				logger.debug("getLocalZipBlob read {} bytes", res.length);
				return res;
			} catch (SQLException e) {
				em.getTransaction().rollback();
				em.close();
				throw new RuntimeException(e);
			}
		});

	}

	public IConfig getMqttConfig() {
		return this.mqttConfig;
	}

	public Boolean getMqttInternal() {
		return this.mqttInternal;
	}

	public String getMqttPassword() {
		return this.mqttPassword;
	}

	@Transient
	@JsonIgnore
	public String getMqttPasswordForField() {
		if (getMqttPassword() == null) {
			return "";
		} else {
			return FAKE_PIN;
		}
	}

	public String getMqttPort() {
		return this.mqttPort;
	}

	public String getMqttUserName() {
		return this.mqttUserName;
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

	@Transient
	@JsonIgnore
	public String getParamDisplayList() {
		String uAccessList = StartupUtils.getStringParam("displayList");
		if (uAccessList == null) {
			// use access list from database
			uAccessList = Config.getCurrent().getIpDisplayList();
			if (uAccessList == null || uAccessList.isBlank()) {
				uAccessList = null;
			}
		}
		return uAccessList;
	}

	/**
	 * @return the current password.
	 */
	@Transient
	@JsonIgnore
	public String getParamDisplayPin() {
		String uPin = StartupUtils.getStringParam("displayPin");
		if (uPin == null) {
			// // not defined in environment
			// // use pin from database, which is either empty (no password required)
			// // or current.
			// uPin = Config.getCurrent().getDisplayPin();
			// // logger.debug("pin = {}", uPin);
			// if (uPin == null || uPin.isBlank()) {
			// return null;
			// } else {
			// return uPin; // what is in the database is already encrypted
			// }
			return null;
		} else if (uPin.isBlank()) {
			// no password will be expected
			return null;
		} else {
			return uPin;
		}
	}

	/**
	 * @return the current list of feature switches.
	 */
	@Transient
	@JsonIgnore
	public String getParamFeatureSwitches() {
		String uAccessList = StartupUtils.getStringParam("featureSwitches");
		if (uAccessList == null) {
			// use access list from database
			uAccessList = Config.getCurrent().getFeatureSwitches();
			if (uAccessList == null || uAccessList.isBlank()) {
				uAccessList = null;
			}
		}
		return uAccessList;
	}

	@Transient
	@JsonIgnore
	public boolean getParamMqttInternal() {
		Boolean enableInternal = StartupUtils.getBooleanParamOrElseNull("enableEmbeddedMqtt");
		if (enableInternal != null) {
			return enableInternal;
		} else {
			return isMqttInternal();
		}
	}

	/**
	 * @return the current mqtt server.
	 */
	@Transient
	@JsonIgnore
	public String getParamMqttPassword() {
		// get non-encrypted password
		String param = StartupUtils.getStringParam("mqttPassword");
		// don't get from the database - useless because encrypted
		// if (param == null) {
		// // get from database
		// param = Config.getCurrent().getMqttPassword();
		// if (param == null || param.isBlank()) {
		// param = null;
		// }
		// }
		return param;
	}

	/**
	 * @return the current mqtt port.
	 */
	@Transient
	@JsonIgnore
	public String getParamMqttPort() {
		String param = StartupUtils.getStringParam("mqttPort");
		if (param == null) {
			// get from database
			param = Config.getCurrent().getMqttPort();
			if (param == null || param.isBlank()) {
				param = "1883";
			}
		}
		return param;
	}

	/**
	 * @return the current mqtt server.
	 */
	@Transient
	@JsonIgnore
	public String getParamMqttServer() {
		String param = StartupUtils.getStringParam("mqttServer");
		return param;
	}

	/**
	 * @return the current mqtt server.
	 */
	@Transient
	@JsonIgnore
	public String getParamMqttUserName() {
		String param = StartupUtils.getStringParam("mqttUserName");
		if (param == null) {
			// get from database
			param = Config.getCurrent().getMqttUserName();
			if (param == null || param.isBlank()) {
				param = null;
			}
		}
		return param;
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
			// uPin = Config.getCurrent().getPin();
			// logger.debug("getParamPin uPin = {}", uPin);
			// if (uPin == null || uPin.isBlank() || uPin.trim().contentEquals(FAKE_PIN)) {
			// logger.debug("no uPin");
			// return null;
			// } else if (uPin.length() < 64) {
			// // assume legacy
			// logger.debug("legacy uPin");
			// String encodedPin = AccessUtils.encodePin(uPin, Config.getCurrent().getPin(), false);
			// return encodedPin;
			// } else {
			// logger.debug("encrypted uPin");
			// return uPin; // what is in the database is already encrypted
			// }
			return null;
		} else if (uPin.isBlank()) {
			// no password will be expected
			return null;
		} else {
			logger.debug("param uPin {}", uPin);
			return uPin;
		}
	}

	/**
	 * @return the public results url stored in the database, except if overridden by system property or envariable.
	 */
	@Transient
	@JsonIgnore
	public String getParamPublicResultsURL() {
		String uURL = StartupUtils.getStringParam("remote");
		if (uURL != null) {
			// old configs with environment variable may still have a trailing /update.
			uURL = uURL.replaceFirst("/update$", "");
			return uURL;
		} else {
			uURL = this.getPublicResultsURL();
			if (uURL == null || uURL.isBlank()) {
				return null;
			} else {
				// user may have copied URL with trailing /
				uURL = uURL.replaceFirst("/$", "");
				return uURL;
			}
		}
	}

	@Transient
	@JsonIgnore
	public String getParamStylesDir() {
		String param = StartupUtils.getStringParam("stylesDir");
		if (param == null || param.isBlank()) {
			// get from database
			param = Config.getCurrent().getStylesDirectory();
			if (param == null || param.isBlank()) {
				param = "css/nogrid";
			}
		}
		// to keep they old styles, users must move them to css/ and update the stylesDir setting.
		Path ldpd = ResourceWalker.getLocalDirPath();
		boolean legacyStyles = false;
		if (ldpd != null) {
			Path ldp = ldpd.resolve("styles");
			legacyStyles = Files.exists(ldp);
		}

		if (param.contentEquals("styles") && legacyStyles) {
			// if using the legacy styles, we want to force the conversion
			Main.getStartupLogger().error("Legacy styles found, forcing css/nogrid; must move folder to css folder");
			logger.error("Legacy styles found, forcing css/nogrid; must move folder to css folder");
			param = "css/nogrid";
		} else {
			if (param.startsWith("css/")) {
				param = param.substring("css/".length());
			}
			if (ldpd != null) {
				Path ldp = ldpd.resolve("css/" + param);
				boolean predefinedStyleName = isPredefinedStyle(param);
				if (!Files.exists(ldp) && !predefinedStyleName) {
					Main.getStartupLogger().error("{} does not exist, using default css/nogrid as default",
					        ldp.toAbsolutePath());
					logger./**/error("{} does not exist, using default css/nogrid as default", ldp.toAbsolutePath());
					param = "css/nogrid";
				}
			}
		}
		if (!param.startsWith("css/")) {
			param = "css/" + param;
		}
		return param;
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

	@Transient
	@JsonIgnore
	public String getParamVideoDataDecisionUrl() {
		String paramVideoDataURL = getParamVideoDataURL();
		return paramVideoDataURL != null ? paramVideoDataURL + "/decision" : null;
	}

	/**
	 * @return the updateKey stored in the database, except if overridden by system property or envariable.
	 */
	@Transient
	@JsonIgnore
	public String getParamVideoDataKey() {
		String uKey = StartupUtils.getStringParam("videoDataKey");
		if (uKey == null) {
			// use pin from database
			uKey = Config.getCurrent().getVideoDataKey();
			if (uKey == null || uKey.isBlank()) {
				uKey = null;
			}
		}
		return uKey;
	}

	@Transient
	@JsonIgnore
	public String getParamVideoDataTimerUrl() {
		String paramVideoDataURL = getParamVideoDataURL();
		return paramVideoDataURL != null ? paramVideoDataURL + "/timer" : null;
	}

	@Transient
	@JsonIgnore
	public String getParamVideoDataUpdateUrl() {
		String paramVideoDataURL = getParamVideoDataURL();
		return paramVideoDataURL != null ? paramVideoDataURL + "/update" : null;
	}

	/**
	 * @return the public results url stored in the database, except if overridden by system property or envariable.
	 */
	public String getParamVideoDataURL() {
		String uURL = StartupUtils.getStringParam("videodata");
		if (uURL != null) {
			return uURL;
		} else {
			uURL = this.getVideoDataURL();
			if (uURL == null || uURL.isBlank()) {
				return null;
			} else {
				// user may have copied URL with trailing /
				uURL = uURL.replaceFirst("/$", "");
				return uURL;
			}
		}
	}

	@Transient
	@JsonIgnore
	public String getParamVideoStylesDir() {
		String param = StartupUtils.getStringParam("videoStylesDir");
		if (param == null || param.isBlank()) {
			// get from database
			param = Config.getCurrent().getVideoStylesDirectory();
			if (param == null || param.isBlank()) {
				param = "css/nogrid";
			}
		}
		Path ldpd = ResourceWalker.getLocalDirPath();
		// accept and normalize old naming convention.
		if (param.startsWith("css/")) {
			param = param.substring("css/".length());
		}
		if (ldpd != null) {
			Path ldp = ldpd.resolve("css/" + param);
			boolean predefinedStyleName = isPredefinedStyle(param);
			if (!Files.exists(ldp) && !predefinedStyleName) {
				param = "css/nogrid";
				String message = "{} does not exist, using default css/nogrid as default video styles";
				Main.getStartupLogger().error(message, ldp.toAbsolutePath());
				logger./**/error(message, ldp.toAbsolutePath());
			}
		}
		if (!param.startsWith("css/")) {
			param = "css/" + param;
		}
		return param;
	}

	public String getPin() {
		return this.pin;
	}

	@Transient
	@JsonIgnore
	public String getPinForField() {
		if (getPin() == null) {
			return "";
		} else {
			return FAKE_PIN;
		}
	}

	public String getPublicResultsURL() {
		return this.publicResultsURL;
	}

	public String getSalt() {
		return this.salt;
	}

	@Transient
	@JsonIgnore
	public String getStylesDirBase() {
		String bd = getParamStylesDir();
		if (bd.startsWith("css/")) {
			return bd.substring("css/".length());
		}
		return bd;
	}

	public String getStylesDirectory() {
		return this.stylesDirectory;
	}

	public TimeZone getTimeZone() {
		if (this.timeZoneId == null) {
			return null;
		} else {
			return TimeZone.getTimeZone(this.timeZoneId);
		}
	}

	public String getUpdatekey() {
		return this.updatekey;
	}

	public String getVideoDataKey() {
		return this.videoDataKey;
	}

	public String getVideoDataURL() {
		return this.videoDataURL;
	}

	@Transient
	@JsonIgnore
	public String getVideoStylesDirBase() {
		String bd = getParamVideoStylesDir();
		if (bd.startsWith("css/")) {
			return bd.substring("css/".length());
		}
		return bd;
	}

	public String getVideoStylesDirectory() {
		return this.videoStylesDirectory;
	}

	@Override
	public int hashCode() {
		// https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
		return 31;
	}

	@Transient
	@JsonIgnore
	public boolean isClearZip() {
		if (this.localOverride == null) {
			this.clearZip = false;
		}
		return this.clearZip;
	}

	@Transient
	@JsonIgnore
	public boolean isIgnoreCaching() {
		return FileServlet.isIgnoreCaching();
	}

	public boolean isLocalTemplatesOnly() {
		return this.localTemplatesOnly || featureSwitch("localTemplatesOnly");
	}

	public boolean isMqttInternal() {
		return this.mqttInternal;
	}

	@Transient
	@JsonIgnore
	public boolean isTraceMemory() {
		if (this.traceMemory == null) {
			this.traceMemory = StartupUtils.getBooleanParam("traceMemory");
		}
		return Boolean.TRUE.equals(this.traceMemory);
	}

	@Transient
	@JsonIgnore
	public boolean isUseCompetitionDate() {
		if (this.useCompetitionDate == null) {
			this.useCompetitionDate = StartupUtils.getBooleanParam("useCompetitionDate");
		}
		return this.useCompetitionDate;
	}

	public void setClearZip(boolean clearZipRequested) {
		this.clearZip = clearZipRequested;
	}

	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	public void setDisplayPin(String displayPin) {
		// logger.debug("setting displayPin {}",displayPin);
		this.displayPin = displayPin;
	}

	public void setDisplayPinForField(String displayPin) {
		// logger.debug("setDisplayPinForField with {}", displayPin);
		if (displayPin != null && displayPin.length() != 64 && displayPin != FAKE_PIN) {
			String encodedPin = AccessUtils.encodePin(displayPin, Config.getCurrent().getPin(), false);
			logger.debug("encoded displayPin {}", encodedPin);
			this.setDisplayPin(encodedPin);
		} else if (displayPin == null || displayPin.isBlank()) {
			logger.debug("empty {}", displayPin);
			this.setDisplayPin(null);
		}
	}

	public void setFeatureSwitches(String featureSwitches) {
		this.featureSwitches = featureSwitches;
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

	public void setIpDisplayList(String ipDisplayList) {
		this.ipDisplayList = ipDisplayList;
	}

	public void setLocalTemplatesOnly(boolean localTemplatesOnly) {
		this.localTemplatesOnly = localTemplatesOnly;
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

	public void setMqttConfig(IConfig mqttConfig) {
		this.mqttConfig = mqttConfig;
	}

	public void setMqttInternal(boolean mqttInternal) {
		this.mqttInternal = mqttInternal;
	}

	public void setMqttInternal(Boolean mqttInternal) {
		this.mqttInternal = mqttInternal;
	}

	public void setMqttPassword(String mqttPassword) {
		this.mqttPassword = mqttPassword;
	}

	public void setMqttPasswordForField(String mqttPassword) {
		// logger.debug("setMqttPasswordForField with {}", mqttPassword);
		if (mqttPassword != null && mqttPassword.length() != 64 && mqttPassword != FAKE_PIN) {
			String encodedPin = AccessUtils.encodePin(mqttPassword, Config.getCurrent().getPin(), false);
			logger.debug("encoded mqttPassword {}", encodedPin);
			this.setMqttPassword(encodedPin);
		} else if (mqttPassword == null || mqttPassword.isBlank()) {
			logger.debug("empty mqttPassword {}", mqttPassword);
			this.setMqttPassword(null);
		}
	}

	public void setMqttPort(String mqttPort) {
		this.mqttPort = mqttPort;
	}

	public void setMqttUserName(String mqttUserName) {
		// anonymous allowed iff mqttUserName is empty or null.
		// we cannot override Moquette login to directly invoke our authenticator...
		if (getMqttConfig() != null) {
			getMqttConfig().setProperty(IConfig.ALLOW_ANONYMOUS_PROPERTY_NAME,
			        Boolean.toString(mqttUserName == null || mqttUserName.isBlank()));
		}
		this.mqttUserName = mqttUserName;
	}

	public void setPin(String pin) {
		// logger.debug("setting pin {}",pin);
		this.pin = pin;
	}

	public void setPinForField(String pin) {
		// logger.debug("displayPin setter called with {}", displayPin);
		if (pin != null && pin.length() != 64 && pin != FAKE_PIN) {
			this.setPin(AccessUtils.encodePin(pin, Config.getCurrent().getPin(), false));
		} else if (pin == null || pin.isBlank()) {
			this.setPin(null);
		}
	}

	public void setPublicResultsURL(String publicResultsURL) {
		this.publicResultsURL = publicResultsURL;
	}

	public void setSkipReading(boolean b) {
		this.skipReading = b;
	}

	public void setStylesDirectory(String sd) {
		// styles directory is stored with a leading "css/"
		if (sd == null) {
			this.stylesDirectory = "css/nogrid";
		} else if (!sd.startsWith("css/")) {
			this.stylesDirectory = "css/" + sd;
		} else {
			this.stylesDirectory = sd;
		}
		// this routine is called when reloading an export, during
		// the process of recreating the config. Therefore we don't
		// know where the override directory is (it can come from the
		// database).
	}

	public void setTimeZone(TimeZone timeZone) {
		if (timeZone == null) {
			this.timeZoneId = null;
		} else {
			this.timeZoneId = timeZone.getID();
		}
	}

	public void setUpdatekey(String updatekey) {
		this.updatekey = updatekey;
	}

	public void setVideoDataKey(String videoDataKey) {
		this.videoDataKey = videoDataKey;
	}

	public void setVideoDataURL(String videoDataURL) {
		this.videoDataURL = videoDataURL;
	}

	public void setVideoStylesDirectory(String videoStylesDirectory) {
		this.videoStylesDirectory = videoStylesDirectory;
	}

	private boolean isPredefinedStyle(String param) {
		return param.contentEquals("grid") || param.contentEquals("nogrid") || param.contentEquals("transparent");
	}

	/**
	 * @param salt the salt to set
	 */
	private void setSalt(String salt) {
		this.salt = salt;
		logger.debug("setting salt to {}", this.salt);
	}

}
