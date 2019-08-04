/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.util.Enumeration;

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

public class URLUtils {
    final private static Logger logger = (Logger)LoggerFactory.getLogger(URLUtils.class);
    
    public static void logHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
          String headerName = (String)headerNames.nextElement();
          logger.debug("{}: {}",headerName,request.getHeader(headerName));
        }
    }
    public static String getScheme(HttpServletRequest request) {
        logHeaders(request);
        String scheme = request.getHeader("X-Forwarded-Proto");
        return scheme != null ? scheme : request.getScheme();
    }

    public static String getServerName(HttpServletRequest request) {
        String host = request.getHeader("X-Forwarded-Host");
        return host != null ? host : request.getServerName();
    }

    public static int getServerPort(HttpServletRequest request) {
        String port = request.getHeader("X-Forwarded-Port");
        return port != null ? Integer.parseInt(port) : request.getServerPort();
    }


}
