/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.parameters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface QueryParameterReader extends HasUrlParameter<String> {

    final Logger logger = (Logger) LoggerFactory.getLogger(QueryParameterReader.class);

    public default HashMap<String, List<String>> computeParams(Location location,
            Map<String, List<String>> parametersMap) {

        HashMap<String, List<String>> params = new HashMap<>(parametersMap);
        String fopName = null;
        String groupName = null;
        // get the fop from the query parameters, set as default if not provided
        if (!isIgnoreFopFromURL()) {
            List<String> fopNames = parametersMap.get("fop");

            if (fopNames != null && fopNames.get(0) != null) {
                logger.trace("fopNames {}", fopNames);
                fopName = (fopNames.get(0));
            }
            if (fopName != null) {
                setFopName(fopName);
                try {
                    params.put("fop", Arrays.asList(URLEncoder.encode(fopName, StandardCharsets.UTF_8.name())));
                } catch (UnsupportedEncodingException e) {
                }
            }
        } else {
            params.remove("fop");
        }

        // get the group from query parameters, do not add value if group is not defined
        if (!isIgnoreGroupFromURL()) {
            List<String> groupNames = parametersMap.get("group");

            if (groupNames != null && groupNames.get(0) != null) {
                groupName = groupNames.get(0);

            }
            if (groupName != null) {
                try {
                    params.put("group", Arrays.asList(URLEncoder.encode(groupName, StandardCharsets.UTF_8.name())));
                } catch (UnsupportedEncodingException e) {
                }
            }
        } else {
            params.remove("group");
        }

//        logger.debug("URL parsing: {} OwlcmsSession: fop={} group={}", LoggerUtils.whereFrom(),
//                (fopName != null ? fopName : null), (groupName != null ? groupName : null));
        if (fopName != null) {
            OwlcmsSession.setAttribute("fopName", fopName);
        }
        return params;
    }

    public Location getLocation();

    public UI getLocationUI();

    public default boolean isIgnoreFopFromURL() {
        return false;
    }

    public default boolean isIgnoreGroupFromURL() {
        return true;
    }

    public void setFopName(String fopName);

    public void setLocation(Location location);

    public void setLocationUI(UI locationUI);

    /*
     * Process query parameters
     *
     * @see app.owlcms.ui.group.URLParameter#setParameter(com.vaadin.flow.router. BeforeEvent, java.lang.String)
     */
    @Override
    public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        logger.setLevel(Level.INFO);
        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> params = computeParams(location, parametersMap);
        // change the URL to reflect retrieved parameters
        event.getUI().getPage().getHistory().replaceState(null,
                new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
    }

}