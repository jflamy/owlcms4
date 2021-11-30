/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
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
import com.google.common.io.BaseEncoding;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

public class AccessUtils {
    static Logger logger = (Logger) LoggerFactory.getLogger(AccessUtils.class);

    public static boolean checkBackdoor() {
        String whiteList = Config.getCurrent().getParamBackdoorList();
        return checkListMembership(whiteList, false);
    }

    public static boolean checkWhitelist() {
        String whiteList = Config.getCurrent().getParamAccessList();
        return checkListMembership(whiteList, true);
    }

    public static String getClientIp() {
        HttpServletRequest request;
        request = VaadinServletRequest.getCurrent().getHttpServletRequest();

        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }

    private static boolean checkListMembership(String whiteList, boolean whiteListCheck) {
        boolean whiteListed;
        if (whiteList != null && !whiteList.trim().isEmpty()) {
            String clientIp = getClientIp();
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
    
    public static boolean checkAuthenticated(String password) {
        boolean isAuthenticated = OwlcmsSession.isAuthenticated();

        if (!isAuthenticated) {
            boolean whiteListed = AccessUtils.checkWhitelist();

            // check for PIN if one is specified
            String expectedPin = Config.getCurrent().getParamPin();
            String hashedPassword = Config.getCurrent().encodeUserPassword(password);
            logger.debug("about to check PIN whiteListed={} pin={} password={} hashedPassword={}", whiteListed, expectedPin, password, hashedPassword);
            if (whiteListed && (expectedPin == null || expectedPin.isBlank())) {
                // there is no password provided in the environmet, or it is empty.  Check that there is no password in the database.
                OwlcmsSession.setAuthenticated(true);
                return true;
            } else if (whiteListed && ( expectedPin.contentEquals(hashedPassword))) {
                OwlcmsSession.setAuthenticated(true);
                return true;
            } else {
                OwlcmsSession.setAuthenticated(false);
                return false;
            }
        }
        return true;
    }
    
    public static String encodePin(String pin, boolean password) {
        boolean parsed;
        
        if (pin == null) {
            return null;
        }

        Config config = Config.getCurrent();
        String salt = config.getSalt();
        if (salt == null || salt.isBlank()) {
            salt = config.defineSalt();
        }
        // SHA256 is 64 hex characters by definition (256 / 4)
        String doSHA = doSHA(pin, salt);
        if (pin != null && pin.length() != 64) {
            // not encrypted.
            logger.debug("[not crypted] {}={} length={} encoded={} salt={}", password ? "given" : "expected", pin, pin != null ? pin.length() : 0, doSHA, salt);
            return doSHA;
        } 
        
        try {
            // check that the 64 characters are valid hexa
            BaseEncoding.base16().lowerCase().decode(pin);
            logger.debug("hexa ok");
            parsed = true;
        } catch (IllegalArgumentException e) {
            logger.debug("not hexa");
            parsed = false;
        }
        
        if (parsed) {
            // 64 characters valid hexa assume already crypted
            logger.debug("[crypted] {}={} length={} encoded={} salt={}", password ? "given" : "expected", pin, pin != null ? pin.length() : 0, pin, salt);
            return pin; 
        } else {
            // 64 characters pass phrase
            logger.debug("[not crypted 64char] {}={} length={} encoded={} salt={}", password ? "given" : "expected", pin, pin != null ? pin.length() : 0, doSHA, salt);
            return doSHA;
        }
    }

    private static String doSHA(String pin, String salt) {
        String sha256hex = Hashing.sha256()
                .hashString(pin + salt, StandardCharsets.UTF_8)
                .toString();
        return sha256hex;
    }

}
