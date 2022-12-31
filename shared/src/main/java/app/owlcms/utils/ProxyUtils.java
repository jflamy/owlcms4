/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Utilities to deal with reverse proxies/balancers/forwarders when reconstructing URLs
 *
 * This deals correctly with Heroku, mileage may vary.
 *
 * @author owlcms
 */

public class ProxyUtils {
    @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(ProxyUtils.class);

    public static String getClientIp(HttpServletRequest request) {
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
