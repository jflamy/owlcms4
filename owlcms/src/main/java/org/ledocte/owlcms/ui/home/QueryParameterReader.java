package org.ledocte.owlcms.ui.home;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.group.GroupRepository;
import org.ledocte.owlcms.init.OwlcmsFactory;
import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.FieldOfPlayState;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface QueryParameterReader extends HasUrlParameter<String>{

	final static Logger logger = (Logger)LoggerFactory.getLogger(QueryParameterReader.class);
	
	/*
	 * Process query parameters
	 * @see org.ledocte.owlcms.ui.lifting.URLParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		logger.setLevel(Level.DEBUG);
		
		Location location = event.getLocation();
		QueryParameters queryParameters = location.getQueryParameters();

		// get the fop from the query parameters, set as default if not provided
		Map<String, List<String>> parametersMap = queryParameters.getParameters();
		List<String> fopNames = parametersMap.get("fop");
		FieldOfPlayState fop;
		HashMap<String, List<String>> params = new HashMap<String, List<String>>(parametersMap);
		if (fopNames != null && fopNames.get(0) == null) {
			fop = OwlcmsFactory.getFOPByName(fopNames.get(0));
		} else {
			fop = OwlcmsFactory.getDefaultFOP();
			params.put("fop",Arrays.asList(fop.getName()));
		}
		
		// get the group from query parameters, leave as fop if group is absent
		List<String> groupNames = parametersMap.get("group");
		Group group = fop.getGroup();
		if (groupNames != null  && groupNames.get(0) != null) {
			group = GroupRepository.findByName(groupNames.get(0));
			fop.setGroup(group);
		}

		OwlcmsSession.setAttribute("fop", fop);
		logger.debug("setting fop in session: {} group={}",(fop != null ? fop.getName() : null),(group != null ? group.getName() : null));
		
		// change the URL to reflect fop and group
		event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}

}