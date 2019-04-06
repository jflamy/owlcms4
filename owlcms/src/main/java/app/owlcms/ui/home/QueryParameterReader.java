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
		FieldOfPlayState fop = null;
		if (!isIgnoreFop()) {
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
		if (!isIgnoreGroup()) {
			List<String> groupNames = parametersMap.get("group");
			if (groupNames != null  && groupNames.get(0) != null) {
				group = GroupRepository.findByName(groupNames.get(0));
				fop.setGroup(group);
			} else {
				group = (fop != null ? fop.getGroup() : null);
			}
			if (group != null) params.put("group",Arrays.asList(group.getName()));
		} else {
			params.remove("group");
		}
		
		logger.debug("URL parsing: OwlcmsSession: fop={} group={}",(fop != null ? fop.getName() : null),(group != null ? group.getName() : null));
		// change the URL to reflect fop and group
		event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}
	
	public default boolean isIgnoreGroup() {
		return true;
	}
	
	public default boolean isIgnoreFop() {
		return false;
	}

}