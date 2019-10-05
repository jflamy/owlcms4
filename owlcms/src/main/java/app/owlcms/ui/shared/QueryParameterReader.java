/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

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

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface QueryParameterReader extends HasUrlParameter<String>{

    final Logger logger = (Logger)LoggerFactory.getLogger(QueryParameterReader.class);

    public default HashMap<String, List<String>> computeParams(Location location, Map<String, List<String>> parametersMap) {

        HashMap<String, List<String>> params = new HashMap<>(parametersMap);

        // get the fop from the query parameters, set as default if not provided
        FieldOfPlay fop = null;
        if (!isIgnoreFopFromURL()) {
            List<String> fopNames = parametersMap.get("fop");
            if (fopNames != null && fopNames.get(0) == null) {
                fop = OwlcmsFactory.getFOPByName(fopNames.get(0));
            } else if (OwlcmsSession.getFop() != null) {
                fop = OwlcmsSession.getFop();
            } else {
                fop = OwlcmsFactory.getDefaultFOP();
            }
            params.put("fop",Arrays.asList(fop.getName()));
            OwlcmsSession.setFop(fop);
        } else {
            params.remove("fop");
        }

        // get the group from query parameters, do not add value if group is not defined
        Group group = null;
        if (!isIgnoreGroupFromURL()) {
            List<String> groupNames = parametersMap.get("group");
            if (groupNames != null  && groupNames.get(0) != null) {
                group = GroupRepository.findByName(groupNames.get(0));
                fop.setGroup(group);
            } else {
                group = (fop != null ? fop.getGroup() : null);
            }
            if (group != null) {
                params.put("group",Arrays.asList(group.getName()));
            }
        } else {
            params.remove("group");
        }

        logger.debug("URL parsing: {} OwlcmsSession: fop={} group={}",LoggerUtils.whereFrom(),(fop != null ? fop.getName() : null),(group != null ? group.getName() : null));
        return params;
    }

    public default boolean isIgnoreFopFromURL() {
        return false;
    }

    public default boolean isIgnoreGroupFromURL() {
        return true;
    }

    /*
     * Process query parameters
     * @see app.owlcms.ui.group.URLParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
     */
    @Override
    public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        logger.setLevel(Level.INFO);
        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> params = computeParams(location, parametersMap);
        // change the URL to reflect retrieved parameters
        event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
    }
    
    public Location getLocation();
    public void setLocation(Location location);
    public UI getLocationUI();
    public void setLocationUI(UI locationUI);

}