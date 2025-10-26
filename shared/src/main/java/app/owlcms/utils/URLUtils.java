/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletRequest;

import ch.qos.logback.classic.Logger;
import elemental.json.JsonValue;
import jakarta.servlet.http.HttpServletRequest;

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

    public static String buildAbsoluteURL(String string) {
        return URLUtils.buildAbsoluteURL(VaadinServletRequest.getCurrent().getHttpServletRequest(),
                string);
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

//    private static String getClientIp(HttpServletRequest request) {
//        String remoteAddr = "";
//
//        if (request != null) {
//            remoteAddr = request.getHeader("X-FORWARDED-FOR");
//            if (remoteAddr == null || "".equals(remoteAddr)) {
//                remoteAddr = request.getRemoteAddr();
//            }
//        }
//
//        return remoteAddr;
//    }

    public static <T extends Component> String getRelativeURLFromTargetClass(Class<T> class1) {
        RouteConfiguration routeResolver = RouteConfiguration.forApplicationScope();
        String relativeURL;
        relativeURL = routeResolver.getUrl(class1);
        return relativeURL;
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

    public static <T extends Component> String getUrlFromTargetClass(Class<T> class1) {
        String relativeURL = getRelativeURLFromTargetClass(class1);
        String absoluteURL = URLUtils.buildAbsoluteURL(VaadinServletRequest.getCurrent().getHttpServletRequest(),
                relativeURL);
        return absoluteURL;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends Component & HasUrlParameter<String>> String getUrlFromTargetClass(Class class1,
            String parameter, QueryParameters q) {
        RouteConfiguration routeResolver = RouteConfiguration.forApplicationScope();
        String relativeURL;
        if (parameter == null) {
            relativeURL = routeResolver.getUrl(class1);
        } else {
            relativeURL = routeResolver.<String, T>getUrl(class1, parameter);
        }
        String queryParameters = q != null ? "?" + q.getQueryString() : "";
        String absoluteURL = URLUtils.buildAbsoluteURL(VaadinServletRequest.getCurrent().getHttpServletRequest(),
                relativeURL)+queryParameters;
        return absoluteURL;
    }

    public static void logHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            logger.debug("{}: {}", headerName, request.getHeader(headerName));
        }
    }

    public static String urlEncode(String name) {
        try {
            name = URLEncoder.encode(name, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
        }
        return name;
    }

    public static Map<String, List<String>> cleanParams(Map<String, List<String>> params) {
        Set<Entry<String, List<String>>> entrySet = params.entrySet();
		Map<String, List<String>> collect = entrySet.stream().filter(e -> !e.getKey().isBlank()).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		return collect;
    }

    /**
     * replace illegal characters in a filename with "_" illegal characters : : \ /
     * * ? | < > ' "
     *
     * @param name
     * @return
     */
    public static String sanitizeFilename(String name) {
        String replaceAll = name.replaceAll("[:\\\\/*?|<>'\"]", "_");
		return CharMatcher.javaIsoControl().removeFrom(replaceAll);
    }

    public static boolean checkPictures() {
        boolean athletePictures;
        try {
            Path f = ResourceWalker.getFileOrResourcePath("pictures");
            athletePictures = Files.walk(f)
            		.anyMatch(x->(x.toString().toLowerCase().endsWith(".jpg")|| x.toString().toLowerCase().endsWith(".jpeg")));
        } catch (IOException e) {
            athletePictures = false;
        }
        return athletePictures;
    }

    public static boolean checkFlags() {
        boolean teamFlags;
        try {
            ResourceWalker.getFileOrResourcePath("flags");
            teamFlags = true;
        } catch (FileNotFoundException e) {
            teamFlags = false;
        }
        return teamFlags;
    }

    public static boolean setImgProp(String propertyName, String prefix, String name, String suffix, Component component) {
        boolean found;
        try {
            ResourceWalker.getFileOrResourcePath(prefix + name + suffix);
            found = true;
        } catch (FileNotFoundException e) {
            found = false;
        }
        if (found) {
            component.getElement().setProperty(propertyName, "<img src='local/" + prefix + name + suffix + "'></img>");
        } else {
            component.getElement().setProperty(propertyName, "");
        }
        return found;
    }

    public static String getImgTag(String prefix, String name, String suffix, String style) {
        boolean found;
        try {
            ResourceWalker.getFileOrResourcePath(prefix + name + suffix);
            found = true;
        } catch (FileNotFoundException e) {
            found = false;
        }
        if (found) {
            return "<img "+style+" src='local/" + prefix + name + suffix + "'></img>";
        } else {
            return null;
        }
    }

	/**
	 * @param history
	 * @param object
	 * @param location the new location to set
	 * @param originalLocation the original location before the change (for FOP validation)
	 */
	public static void replaceState(History history, JsonValue object, Location location, Location originalLocation) {
		//logger.debug("replaceState1 {} {}",location.getPathWithQueryParameters(), LoggerUtils.stackTrace());
		
		// Extract original FOP from the location passed in
		String originalFop = null;
		if (originalLocation != null) {
			QueryParameters origParams = originalLocation.getQueryParameters();
			if (origParams != null) {
				List<String> fopParams = origParams.getParameters().get("fop");
				if (fopParams != null && !fopParams.isEmpty()) {
					originalFop = fopParams.get(0);
				}
			}
		}
		
		// Safety check: ensure FOP parameter is never removed or changed
		// This is a critical parameter that should remain stable throughout URL updates
		Location finalLocation = location;
		
		QueryParameters newParams = location.getQueryParameters();
		if (newParams != null && !newParams.getParameters().isEmpty()) {
			// Check if FOP is being changed—this should not happen
			List<String> newFopParams = newParams.getParameters().get("fop");
			String newFop = (newFopParams != null && !newFopParams.isEmpty()) ? newFopParams.get(0) : null;
			
			if (originalFop != null && !originalFop.equals(newFop)) {
				// FOP value changed from original
				String errorMsg = "CRITICAL: replaceState would change FOP value!\n" +
					"Original: " + originalFop + "\nNew: " + newFop + "\nURL: " + location.getPathWithQueryParameters() + "\nStack: " + LoggerUtils.stackTrace();
				logger.error(errorMsg);
			}
		}
		
		// Only update URL if no FOP violations were detected
		history.replaceState(object, finalLocation);
	}

	public static String getFlagResourcePath(String team, String[] exts) {
        if (team == null || team.isBlank()) {
            return null;
        }
        String teamFileName = sanitizeFilename(team);
        for (String ext : exts) {
            try {
                ResourceWalker.getFileOrResourcePath("flags/" + teamFileName + ext);
                return "flags/" + teamFileName + ext;
            } catch (FileNotFoundException e) {
                // try next extension
            }
        }
        return null;
	}
}
