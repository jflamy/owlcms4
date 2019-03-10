package app.owlcms.ui.home;

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
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FieldOfPlayState;
import ch.qos.logback.classic.Logger;

public interface QueryParameterReader extends HasUrlParameter<String>{

	final static Logger logger = (Logger)LoggerFactory.getLogger(QueryParameterReader.class);
	
	/*
	 * Process query parameters
	 * @see app.owlcms.ui.lifting.URLParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		
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
		Group group;
		if (groupNames != null  && groupNames.get(0) != null) {
			group = GroupRepository.findByName(groupNames.get(0));
			fop.setGroup(group);
		} else {
			group = fop.getGroup();
		}
		if (group != null) params.put("group",Arrays.asList(group.getName()));

		OwlcmsSession.setAttribute("fop", fop);
		logger.debug("setting fop in session: {} group={}",(fop != null ? fop.getName() : null),(group != null ? group.getName() : null));
		
		// change the URL to reflect fop and group
		event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}

}