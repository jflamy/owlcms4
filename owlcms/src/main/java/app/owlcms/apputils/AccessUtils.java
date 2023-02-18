/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class AccessUtils {
	static Logger logger = (Logger) LoggerFactory.getLogger(AccessUtils.class);

	public static boolean checkAuthenticated(String password) {
		boolean isAuthenticated = OwlcmsSession.isAuthenticated();

		if (!isAuthenticated) {
			boolean ipIsAllowed = AccessUtils.ipIsAllowedForOfficials(getClientIp());
			if (!(ipIsAllowed)) {
				OwlcmsSession.setAuthenticated(false);
				return false;
			}

			String pinOverride = Config.getCurrent().getParamPin();
			String dbPin = Config.getCurrent().getPin();
			return checkPassword(password, pinOverride, dbPin, "TO");
		}
		return true;
	}

	public static boolean checkBackdoor(String clientIp) {
		String backdoorList = Config.getCurrent().getParamBackdoorList();
		return checkListMembership(clientIp, backdoorList, false);
	}

	public static boolean checkDisplayAuthenticated(String password) {
		boolean isAuthenticated = OwlcmsSession.isDisplayAuthenticated();

		if (!isAuthenticated) {
			boolean ipIsAllowed = AccessUtils.isIpAllowedForDisplay(getClientIp());
			if (!(ipIsAllowed)) {
				OwlcmsSession.setAuthenticated(false);
				return false;
			}

			// check for PIN if one is specified
			String pinOverride = Config.getCurrent().getParamDisplayPin();
			String dbPin = Config.getCurrent().getDisplayPin();
			return checkPassword(password, pinOverride, dbPin, "Display");
		}
		return true;
	}

	public static String encodePin(String pin, String storedPin, boolean checkingPassword) {
		logger.debug("encodePin {} {} {}", pin, storedPin, checkingPassword);
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
					String storedSHA = storedPin.substring(0, 64);
					salt = storedPin.substring(65);
					logger.debug("[checking] given={} length={} encoded={} expected={} appendedSalt={} (from {})",
					        pin,
					        pin != null ? pin.length() : 0,
					        storedSHA,
					        doSHA, salt, LoggerUtils.whereFrom());
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
				logger.debug("[checking] given={} length={} encoded={} expected={} storedSalt={} (from {})",
				        pin,
				        pin != null ? pin.length() : 0,
				        storedPin,
				        doSHA, salt, LoggerUtils.whereFrom());
				return doSHA + "_" + salt;
			}
		} else {
			// encoding the password
			if (salt == null || salt.isBlank()) {
				salt = config.computeSalt();
			}
			logger.debug("[encoding] encoding:{} length={} encoded={} salt={} (from {})", pin,
			        pin.length(), doSHA, salt, LoggerUtils.whereFrom());
			return doSHA + "_" + salt;
		}
	}

	public static String getClientIp() {
		HttpServletRequest request;
		VaadinServletRequest current = VaadinServletRequest.getCurrent();
		request = current.getHttpServletRequest();

		String remoteAddr = "";

		if (request != null) {
			remoteAddr = request.getHeader("X-FORWARDED-FOR");
			if (remoteAddr == null || "".equals(remoteAddr)) {
				remoteAddr = request.getRemoteAddr();
			}
		}
		return remoteAddr;
	}

	public static boolean ipIsAllowedForOfficials(String clientIp) {
		String whiteList = Config.getCurrent().getParamAccessList();
		return (whiteList != null && !whiteList.isEmpty()) ? checkListMembership(clientIp, whiteList, true) : true;
	}

	public static boolean isIpAllowedForDisplay(String clientIp) {
		String displayList = Config.getCurrent().getParamDisplayList();
		return checkListMembership(clientIp, displayList, true);
	}

	private static boolean checkListMembership(String clientIp, String whiteList, boolean whiteListCheck) {
		boolean whiteListed;
		if (whiteList != null && !whiteList.trim().isEmpty()) {
			if ("0:0:0:0:0:0:0:1".equals(clientIp) || clientIp.startsWith("169.254")) {
				// compensate for IPv6 returned and other windows networking oddities
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

	private static boolean checkPassword(String password, String pinOverride, String dbPin, String loggingContext) {

		logger.debug("{} override {} provided {} dbPin {}", loggingContext, pinOverride, password, dbPin);
		String hashedPassword = "";
		if (dbPin != null) {
			hashedPassword = Config.getCurrent().encodeUserPassword(password, dbPin);
		}
		logger.debug("checking override={} password={} storedHashedPassword={} hashedUserPassword={}", pinOverride,
		        password, dbPin, hashedPassword);

		if (pinOverride != null && pinOverride.isBlank()) {
			// no check
			logger.info("empty {} password override", loggingContext);
			OwlcmsSession.setAuthenticated(true);
			return true;
		} else if (pinOverride != null && !pinOverride.isBlank()) {
			if (pinOverride.contentEquals(password)) {
				logger.info("{} password override successful", loggingContext);
				OwlcmsSession.setAuthenticated(true);
				return true;
			} else {
				logger.error("{} password override unsuccessful", loggingContext);
				OwlcmsSession.setAuthenticated(false);
				return false;
			}
		} else if (dbPin == null || hashedPassword.contentEquals(dbPin)) {
			OwlcmsSession.setAuthenticated(true);
			return true;
		} else {
			OwlcmsSession.setAuthenticated(false);
			return false;
		}
	}

	private static String doSHA(String pin, String salt) {
		String sha256hex = Hashing.sha256()
		        .hashString(pin + salt, StandardCharsets.UTF_8)
		        .toString();
		return sha256hex;
	}

}
