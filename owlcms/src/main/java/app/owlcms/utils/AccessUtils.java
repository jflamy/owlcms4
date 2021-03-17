/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.data.config.Config;
import ch.qos.logback.classic.Logger;

public class AccessUtils {
    static Logger logger = (Logger) LoggerFactory.getLogger(AccessUtils.class);

    public static boolean checkWhitelist() {
        String whiteList = Config.getCurrent().getParamAccessList();
        return checkListMembership(whiteList, true);
    }
    
    public static boolean checkBackdoor() {
        String whiteList = Config.getCurrent().getParamBackdoorList();
        return checkListMembership(whiteList, false);
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
            if (!whiteListCheck) {
                logger.info("checking client IP={} vs configured backdoor={}", clientIp, whiteList);
            }
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

}
