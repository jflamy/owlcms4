/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils.queryparameters;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface FOPParameters extends HasUrlParameter<String> {

    final String FOP = "fop";
    final String GROUP = "group";
    final Logger logger = (Logger) LoggerFactory.getLogger(FOPParameters.class);

    public abstract String getFopName();

    public Location getLocation();

    public UI getLocationUI();

    public default boolean isIgnoreFopFromURL() {
        return false;
    }

    public default boolean isIgnoreGroupFromURL() {
        return true;
    }

    public default boolean isShowInitialDialog() {
        return false;
    }

    public default HashMap<String, List<String>> readParams(Location location,
            Map<String, List<String>> parametersMap) {
//        logger.debug("location {} getLocation {}", location.getPathWithQueryParameters(),
//                getLocation().getPathWithQueryParameters());

        HashMap<String, List<String>> newParameterMap = new HashMap<>(parametersMap);

        List<String> fopNames = parametersMap.get(FOP);
        boolean fopFound = fopNames != null && fopNames.get(0) != null;
        if (!fopFound) {
            setShowInitialDialog(true);
        }

        if (!isIgnoreFopFromURL()) {
            if (fopFound) {
                // logger.trace("fopNames {}", fopNames);
                String decoded = URLDecoder.decode(fopNames.get(0), StandardCharsets.UTF_8);
                // logger.trace("URL fop = {} decoded = {}",fopNames.get(0), decoded);
                // fopName = OwlcmsFactory.getFOPByName(decoded);
                setFopName(decoded);
            } else {
                if (OwlcmsSession.getFopName() != null) {
                    // logger.trace("OwlcmsSession.getFop() {}", OwlcmsSession.getFop());
                    setFopName(OwlcmsSession.getFopName());
                }
            }
            if (getFopName() == null) {
                // logger.trace("OwlcmsFactory.getDefaultFOP() {}",
                // OwlcmsFactory.getDefaultFOP());
                Set<String> knownNames = UpdateReceiverServlet.getUpdateCache().keySet();
                if (knownNames.size() >= 1) {
                    setFopName(knownNames.stream().findFirst().get());
                } else {
                    setFopName("A"); // default config.
                }
            }
            newParameterMap.put(FOP, Arrays.asList(URLUtils.urlEncode(getFopName())));
            OwlcmsSession.getCurrent().setFopName(getFopName());
        } else {
            newParameterMap.remove(FOP);
        }

//        // get the group from query parameters
//        Group group = null;
//        if (!isIgnoreGroupFromURL()) {
//            List<String> groupNames = parametersMap.get(GROUP);
//            if (groupNames != null && groupNames.get(0) != null) {
//                String decoded = URLDecoder.decode(groupNames.get(0), StandardCharsets.UTF_8);
//                // logger.trace("URL group = {} decoded = {}",groupNames.get(0), decoded);
//                group = GroupRepository.findByName(decoded);
//                fop.loadGroup(group, this, true);
//            } else {
//                group = (fop != null ? fop.getGroup() : null);
//            }
//            if (group != null) {
//                newParameterMap.put(GROUP, Arrays.asList(URLUtils.urlEncode(group.getName())));
//            }
//        } else {
//            newParameterMap.remove(GROUP);
//        }

//        logger.debug("URL parsing: {} OwlcmsSession: fop={} group={}", LoggerUtils.whereFrom(),
//                (fop != null ? fop.getName() : null), (group != null ? group.getName() : null));
        return newParameterMap;
    }

    public abstract void setFopName(String decoded);

    public void setLocation(Location location);

    public void setLocationUI(UI locationUI);

    /*
     * Retrieve parameter(s) from URL and update according to current settings.
     *
     * The values are stored in the URL in order to allow bookmarking and easy
     * reloading.
     *
     * Note: what Vaadin calls a parameter is in the REST style, actually part of
     * the URL path. We use the old-style
     * Query parameters for our purposes.
     *
     * @see
     * com.vaadin.flow.router.HasUrlParameter#setParameter(com.vaadin.flow.router.
     * BeforeEvent, java.lang.Object)
     */
    /**
     * @see com.vaadin.flow.router.HasUrlParameter#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.Object)
     */
    @Override
    public default void setParameter(BeforeEvent event, @OptionalParameter String unused) {
        logger.setLevel(Level.INFO);
        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> params = readParams(location, parametersMap);

        // change the URL to reflect the updated parameters
        Location location2 = new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params)));
        //logger.debug("setParameter {}", location2);
        event.getUI().getPage().getHistory().replaceState(null, location2);
    }

    /**
     * By default, there is no initial dialog. Classes that need one must override.
     *
     * @param b
     */
    public default void setShowInitialDialog(boolean b) {
    }

    public default void storeInSessionStorage(String key, String value) {
        UI.getCurrent().getElement().executeJs("window.sessionStorage.setItem($0, $1);", key, value);
    }

    public default void storeReturnURL() {
    }

    public default void updateParam(Map<String, List<String>> cleanParams, String parameter, String value) {
        if (value != null) {
            cleanParams.put(parameter, Arrays.asList(value));
        } else {
            cleanParams.remove(parameter);
        }
    }

    public default void updateURLLocation(UI ui, Location location, String parameter, String value) {
        TreeMap<String, List<String>> parametersMap = new TreeMap<>(location.getQueryParameters().getParameters());
        // get current values
        if (!this.isIgnoreFopFromURL()) {
            String fopName = OwlcmsSession.getFopName();
            updateParam(parametersMap, FOP, fopName);
        } else {
            updateParam(parametersMap, FOP, null);
        }

        // override with the update
        updateParam(parametersMap, parameter, value);

        Location location2 = new Location(location.getPath(), new QueryParameters(parametersMap));
        ui.getPage().getHistory().replaceState(null, location2);
        //logger.debug("location2 = {}", location2.getPathWithQueryParameters());
        setLocation(location2);
        storeReturnURL();
    }

}