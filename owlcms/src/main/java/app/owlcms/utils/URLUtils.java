/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.net.URL;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletRequest;

import ch.qos.logback.classic.Logger;

/**
 * Utilities to deal with reverse proxies/balancers/forwarders when reconstructing URLs
 *
 * This deals correctly with Heroku, mileage may vary.
 *
 * @author owlcms
 */

public class URLUtils {
    final private static Logger logger = (Logger) LoggerFactory.getLogger(URLUtils.class);

    public static String buildAbsoluteURL(HttpServletRequest request, String resourcePath) {
        int port = URLUtils.getServerPort(request);
        String scheme = URLUtils.getScheme(request);
        StringBuilder result = new StringBuilder();
        result.append(scheme).append("://").append(URLUtils.getServerName(request));
        if ((scheme.equals("http") && port != 80) || (request.getScheme().equals("https") && port != 443)) {
            result.append(':').append(port);
        }
        result.append(request.getContextPath());
        if (resourcePath != null && resourcePath.length() > 0) {
            if (!resourcePath.startsWith("/")) {
                result.append("/");
            }
            result.append(resourcePath);
        }
        return result.toString();
    }

    public static String cleanURL(URL siteURL, String siteExternalForm) {
        if (siteURL.getProtocol().equals("http")) {
            siteExternalForm = siteExternalForm.replaceFirst(":80/", "");
            siteExternalForm = siteExternalForm.replaceFirst(":80$", "");
        } else if (siteURL.getProtocol().equals("https")) {
            siteExternalForm = siteExternalForm.replaceFirst(":443/", "");
            siteExternalForm = siteExternalForm.replaceFirst(":443$", "");
        }
        if (siteExternalForm.endsWith("/")) {
            siteExternalForm = siteExternalForm.substring(0, siteExternalForm.length() - 1);
        }
        return siteExternalForm;
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

    public static <T extends Component & HasUrlParameter<String>> String getUrlFromTargetClass(Class<T> class1,
            String parameter) {
        RouteConfiguration routeResolver = RouteConfiguration.forApplicationScope();
        String relativeURL;
        if (parameter == null) {
            relativeURL = routeResolver.getUrl(class1);
        } else {
            relativeURL = routeResolver.<String, T>getUrl(class1, parameter);
        }
        String absoluteURL = URLUtils.buildAbsoluteURL(VaadinServletRequest.getCurrent().getHttpServletRequest(),
                relativeURL);
        return absoluteURL;
    }

    public static void logHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            logger.debug("{}: {}", headerName, request.getHeader(headerName));
        }
    }

    public static <T extends Component> String getUrlFromTargetClass(Class<T> class1) {
        RouteConfiguration routeResolver = RouteConfiguration.forApplicationScope();
        String relativeURL;
        relativeURL = routeResolver.getUrl(class1);
        String absoluteURL = URLUtils.buildAbsoluteURL(VaadinServletRequest.getCurrent().getHttpServletRequest(),
                relativeURL);
        return absoluteURL;
    }

}
