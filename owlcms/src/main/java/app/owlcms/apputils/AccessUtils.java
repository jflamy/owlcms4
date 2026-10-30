/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.Main;
import app.owlcms.access.LoginThrottle;
import app.owlcms.access.PinHashes;
import app.owlcms.access.SameMachineProof;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.HttpServletRequest;

public class AccessUtils {
	static Logger logger = (Logger) LoggerFactory.getLogger(AccessUtils.class);

	/**
	 * @param controlPanelToken accepted because the control panel token was given instead of the officials PIN
	 * @param blockedSeconds    when refused because of earlier failures, else 0
	 */
	public record PinCheck(boolean accepted, boolean controlPanelToken, long blockedSeconds) {
		static final PinCheck REFUSED = new PinCheck(false, false, 0);
		static final PinCheck ACCEPTED = new PinCheck(true, false, 0);
	}

	/** Throttling keys: PIN attempts are counted apart from account logins. */
	static final String OFFICIALS_PIN_THROTTLE = "officials-pin";
	static final String DISPLAY_PIN_THROTTLE = "display-pin";

	/**
	 * The officials PIN login. The control panel token is accepted too, so that a forgotten PIN can be replaced in the
	 * settings. Attempts are throttled.
	 */
	public static PinCheck checkOfficialsPin(String password) {
		if (OwlcmsSession.isAuthenticated()) {
			return PinCheck.ACCEPTED;
		}
		String clientIp = getClientIp();
		if (!ipIsAllowedForOfficials(clientIp)) {
			OwlcmsSession.setAuthenticated(false);
			return PinCheck.REFUSED;
		}
		LoginThrottle throttle = LoginThrottle.shared();
		long blocked = throttle.blockedSeconds(OFFICIALS_PIN_THROTTLE, clientIp);
		if (blocked > 0) {
			logger./**/warn("officials PIN login throttled ip={} seconds={}", clientIp, blocked);
			return new PinCheck(false, false, blocked);
		}
		if (SameMachineProof.matches(password, SameMachineProof.configuredToken())) {
			throttle.success(OFFICIALS_PIN_THROTTLE, clientIp);
			OwlcmsSession.setAuthenticated(true);
			logger./**/warn("officials login with the control panel token ip={}", clientIp);
			return new PinCheck(true, true, 0);
		}
		Config config = Config.getCurrent();
		boolean valid = checkPassword(password, config.getParamPin(), config.getPin(), "TO",
		        pin -> upgradePin("Officials", c -> c.setPin(PinHashes.hash(pin))));
		recordAttempt(throttle, OFFICIALS_PIN_THROTTLE, clientIp, valid);
		OwlcmsSession.setAuthenticated(valid);
		return valid ? PinCheck.ACCEPTED : PinCheck.REFUSED;
	}

	/** The display PIN login; attempts are throttled. */
	public static PinCheck checkDisplayPin(String password) {
		if (OwlcmsSession.computeDisplayAuthenticated()) {
			return PinCheck.ACCEPTED;
		}
		String displayList = Config.getCurrent().getParamDisplayList();
		boolean noDisplayList = displayList == null || displayList.isBlank();
		String clientIp = getClientIp();
		if (!noDisplayList && !isIpAllowedForDisplay(clientIp)) {
			OwlcmsSession.setDisplayAuthenticated(false);
			return PinCheck.REFUSED;
		}
		LoginThrottle throttle = LoginThrottle.shared();
		long blocked = throttle.blockedSeconds(DISPLAY_PIN_THROTTLE, clientIp);
		if (blocked > 0) {
			logger./**/warn("display PIN login throttled ip={} seconds={}", clientIp, blocked);
			return new PinCheck(false, false, blocked);
		}
		Config config = Config.getCurrent();
		boolean valid = checkPassword(password, config.getParamDisplayPin(), config.getDisplayPin(), "Display",
		        pin -> upgradePin("Display", c -> c.setDisplayPin(PinHashes.hash(pin))));
		recordAttempt(throttle, DISPLAY_PIN_THROTTLE, clientIp, valid);
		OwlcmsSession.setDisplayAuthenticated(valid);
		return valid ? PinCheck.ACCEPTED : PinCheck.REFUSED;
	}

	private static void recordAttempt(LoginThrottle throttle, String key, String clientIp, boolean valid) {
		if (valid) {
			throttle.success(key, clientIp);
		} else {
			throttle.fail(key, clientIp);
		}
	}

	public static String encodePin(String pin, String storedPin, boolean checkingPassword) {
		if (pin == null) {
			return null;
		}

		Config config = Config.getCurrent();
		String salt = config.getSalt();

		String doSHA = doSHA(pin, salt);
		if (checkingPassword) {
			if (salt == null || salt.isBlank()) {
				// use new technique - salt is after the encrypted password
				if (storedPin.length() > 64) {
					salt = storedPin.substring(65);
					return doSHA + "_" + salt;
				} else if (Config.FAKE_PIN.contentEquals(storedPin)) {
					// workaround for old bug.
					logger.error("Important: Obsolete password encoding detected; Please change your password.");
					Main.getStartupLogger()
					        .error("Important: Obsolete password encoding detected; Please change your password.");
					return storedPin;
				} else {
					throw new RuntimeException("can't happen, stored password with no salt");
				}
			} else {
				// old technique - salt is saved in the database
				return doSHA + "_" + salt;
			}
		} else {
			// encoding the password
			if (salt == null || salt.isBlank()) {
				salt = config.computeSalt();
			}
			return doSHA + "_" + salt;
		}
	}

	public static String getClientIp() {
		VaadinServletRequest current = VaadinServletRequest.getCurrent();
		if (current == null) {
			return "";
		}

		HttpServletRequest request = current.getHttpServletRequest();
		String remoteAddr = request.getHeader("X-FORWARDED-FOR");
		if (remoteAddr == null || remoteAddr.isEmpty()) {
			remoteAddr = request.getRemoteAddr();
		}
		return remoteAddr;
	}

	public static boolean ipIsAllowedForOfficials(String clientIp) {
		String whiteList = Config.getCurrent().getParamAccessList();
		return (whiteList != null && !whiteList.isEmpty()) ? checkListMembership(clientIp, whiteList, true) : true;
	}

	/**
	 * Compares a value with the display PIN, in constant time. Unlike the display login, it neither reads nor sets the
	 * session, and does not check the display address list.
	 */
	public static boolean displayPinMatches(String value) {
		String candidate = value == null ? "" : value;
		String pinOverride = Config.getCurrent().getParamDisplayPin();
		if (pinOverride != null) {
			return pinOverride.isBlank() || constantTimeEquals(pinOverride, candidate);
		}
		String dbPin = Config.getCurrent().getDisplayPin();
		if (dbPin == null) {
			return true;
		}
		return PinHashes.matches(candidate, dbPin);
	}

	/** Compares in constant time, to avoid leaking secrets through timing; false if either value is null. */
	public static boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
		        actual.getBytes(StandardCharsets.UTF_8));
	}

	public static boolean isIpAllowedForDisplay(String clientIp) {
		String displayList = Config.getCurrent().getParamDisplayList();
		return checkListMembership(clientIp, displayList, true);
	}

	private static boolean checkListMembership(String clientIp, String whiteList, boolean whiteListCheck) {
		boolean whiteListed;
		if (whiteList != null && !whiteList.trim().isEmpty()) {
			if (isLocalhost(clientIp)) {
				// compensate for IPv6 returned by some platforms
				clientIp = "127.0.0.1";
			}
			List<String> whiteListedList = Arrays.asList(whiteList.split(","));
			// must come from whitelisted address and have matching PIN
			whiteListed = whiteListedList.contains(clientIp);
			if (!whiteListed && whiteListCheck) {
				logger.error("login attempt from non-whitelisted host {} (whitelist={})", clientIp, whiteListedList);
			}
		} else {
			// no white list
			whiteListed = false;
		}
		return whiteListed;
	}

	/**
	 * Check whether an IP address represents localhost in any format.
	 * Uses {@link java.net.InetAddress#isLoopbackAddress()} which handles
	 * IPv4 (127.x.x.x), IPv6 (::1, 0:0:0:0:0:0:0:1), and bracketed forms.
	 */
	public static boolean isLocalhost(String ip) {
		if (ip == null || ip.isBlank()) {
			return false;
		}
		try {
			return java.net.InetAddress.getByName(ip).isLoopbackAddress();
		} catch (java.net.UnknownHostException e) {
			return false;
		}
	}

	/**
	 * Check whether an IP address belongs to the local network.
	 */
	public static boolean isLocalNetwork(String ip) {
		if (ip == null || ip.isBlank()) {
			return false;
		}
		try {
			java.net.InetAddress address = java.net.InetAddress.getByName(ip);
			if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
				return true;
			}
			byte[] bytes = address.getAddress();
			return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
		} catch (java.net.UnknownHostException e) {
			return false;
		}
	}

	/**
	 * @param upgrade saves a new hash of the PIN when the stored one is an older format
	 */
	private static boolean checkPassword(String password, String pinOverride, String dbPin, String loggingContext,
	        Consumer<String> upgrade) {
		String candidate = password == null ? "" : password;
		if (pinOverride != null && pinOverride.isBlank()) {
			// no check
			logger.info("empty {} password override", loggingContext);
			return true;
		} else if (pinOverride != null) {
			if (constantTimeEquals(pinOverride, candidate)) {
				logger.info("{} password override successful", loggingContext);
				return true;
			}
			logger.error("{} password override unsuccessful", loggingContext);
			return false;
		}
		if (dbPin == null || dbPin.isBlank()) {
			return true;
		}
		boolean valid = PinHashes.matches(candidate, dbPin);
		if (valid && PinHashes.isOutdated(dbPin)) {
			upgrade.accept(candidate);
		}
		return valid;
	}

	private static void upgradePin(String loggingContext, Consumer<Config> store) {
		Config config = Config.getCurrent();
		store.accept(config);
		Config.setCurrent(config, AuditOrigin.system(loggingContext + " PIN hash upgrade"));
		logger.info("{} PIN stored with the current hashing", loggingContext);
	}

	private static String doSHA(String pin, String salt) {
		String sha256hex = Hashing.sha256()
		        .hashString(pin + salt, StandardCharsets.UTF_8)
		        .toString();
		return sha256hex;
	}

}
