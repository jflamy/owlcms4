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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface QueryParameterReader extends HasUrlParameter<String>{

	final Logger logger = (Logger)LoggerFactory.getLogger(QueryParameterReader.class);
	
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
		HashMap<String, List<String>> params = new HashMap<String, List<String>>(parametersMap);

		// get the fop from the query parameters, set as default if not provided
		FieldOfPlay fop = null;
		if (!isIgnoreFopFromURL()) {
			List<String> fopNames = parametersMap.get("fop"); //$NON-NLS-1$
			if (fopNames != null && fopNames.get(0) == null) {
				fop = OwlcmsFactory.getFOPByName(fopNames.get(0));
			} else if (OwlcmsSession.getFop() != null) {
				fop = OwlcmsSession.getFop();
			} else {
				fop = OwlcmsFactory.getDefaultFOP();
			}
			params.put("fop",Arrays.asList(fop.getName())); //$NON-NLS-1$
			OwlcmsSession.setFop(fop);
		} else {
			params.remove("fop"); //$NON-NLS-1$
		}
	
		// get the group from query parameters, do not add value if group is not defined
		Group group = null;
		if (!isIgnoreGroupFromURL()) {
			List<String> groupNames = parametersMap.get("group"); //$NON-NLS-1$
			if (groupNames != null  && groupNames.get(0) != null) {
				group = GroupRepository.findByName(groupNames.get(0));
				fop.setGroup(group);
			} else {
				group = (fop != null ? fop.getGroup() : null);
			}
			if (group != null) params.put("group",Arrays.asList(group.getName())); //$NON-NLS-1$
		} else {
			params.remove("group"); //$NON-NLS-1$
		}
		
		logger.debug("URL parsing: OwlcmsSession: fop={} group={}",(fop != null ? fop.getName() : null),(group != null ? group.getName() : null)); //$NON-NLS-1$
		// change the URL to reflect fop and group
		event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}
	
	public default boolean isIgnoreGroupFromURL() {
		return true;
	}
	
	public default boolean isIgnoreFopFromURL() {
		return false;
	}

}