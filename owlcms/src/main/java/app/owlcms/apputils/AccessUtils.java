/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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

import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class AccessUtils {
    static Logger logger = (Logger) LoggerFactory.getLogger(AccessUtils.class);

    public static boolean checkAuthenticated(String password) {
        boolean isAuthenticated = OwlcmsSession.isAuthenticated();

        if (!isAuthenticated) {
            boolean whiteListed = AccessUtils.checkWhitelist(getClientIp());

            // check for PIN if one is specified
            String expectedPin = Config.getCurrent().getParamPin();
            String hashedPassword = Config.getCurrent().encodeUserPassword(password);
            logger.debug("checking whiteListed={} pin={} password={} hashedPassword={}", whiteListed,
                    expectedPin, password, hashedPassword);
            if (whiteListed && (expectedPin == null || expectedPin.isBlank())) {
                // there is no password provided in the environmet, or it is empty. Check that there is no password in
                // the database.
                OwlcmsSession.setAuthenticated(true);
                return true;
            } else if (whiteListed && (expectedPin.contentEquals(hashedPassword))) {
                OwlcmsSession.setAuthenticated(true);
                return true;
            } else {
                OwlcmsSession.setAuthenticated(false);
                return false;
            }
        }
        return true;
    }

    public static boolean checkBackdoor(String clientIp) {
        String backdoorList = Config.getCurrent().getParamBackdoorList();
        return checkListMembership(clientIp, backdoorList, false);
    }

    public static boolean checkWhitelist(String clientIp) {
        String whiteList = Config.getCurrent().getParamAccessList();
        return checkListMembership(clientIp, whiteList, true);
    }

    public static String encodePin(String pin, boolean checkingPassword) {
        if (pin == null) {
            return null;
        }

        Config config = Config.getCurrent();
        String salt = config.getSalt();

        String doSHA = doSHA(pin, salt);
        if (checkingPassword) {
            String storedPin = config.getPin();
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
                    return doSHA+"_"+salt;
                } else {
                    return storedPin; // no salt, should never happen
                }
            } else {
                // old technique - salt is saved in the database
                logger.debug("[checking] given={} length={} encoded={} expected={} storedSalt={} (from {})",
                        pin,
                        pin != null ? pin.length() : 0,
                        storedPin,
                        doSHA, salt, LoggerUtils.whereFrom());
                return doSHA+"_"+salt;
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
            // no white list, allow all IP addresses
            whiteListed = true;
        }
        return whiteListed;
    }

    private static String doSHA(String pin, String salt) {
        String sha256hex = Hashing.sha256()
                .hashString(pin + salt, StandardCharsets.UTF_8)
                .toString();
        return sha256hex;
    }

}
