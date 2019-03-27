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

	final static Logger logger = (Logger)LoggerFactory.getLogger(QueryParameterReader.class);
	static final boolean ignoreGroup = false;
	
	/*
	 * Process query parameters
	 * @see app.owlcms.ui.group.URLParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		logger.setLevel(Level.DEBUG);
		logger.debug("setParameter parameter={}",parameter);
		
		Location location = event.getLocation();
		QueryParameters queryParameters = location.getQueryParameters();

		// get the fop from the query parameters, set as default if not provided
		Map<String, List<String>> parametersMap = queryParameters.getParameters();
		List<String> fopNames = parametersMap.get("fop");
		FieldOfPlayState fop;
		HashMap<String, List<String>> params = new HashMap<String, List<String>>(parametersMap);
		if (fopNames != null && fopNames.get(0) == null) {
			fop = OwlcmsFactory.getFOPByName(fopNames.get(0));
		} else if (OwlcmsSession.getFop() != null) {
			fop = OwlcmsSession.getFop();
		} else {
			fop = OwlcmsFactory.getDefaultFOP();
		}
		params.put("fop",Arrays.asList(fop.getName()));
		OwlcmsSession.setFop(fop);
		
		// get the group from query parameters, do not add value if group is not defined
		Group group = null;
		if (!isIgnoreGroup()) {
			List<String> groupNames = parametersMap.get("group");
			if (groupNames != null  && groupNames.get(0) != null) {
				group = GroupRepository.findByName(groupNames.get(0));
				fop.setGroup(group);
			} else {
				group = fop.getGroup();
			}
			if (group != null) params.put("group",Arrays.asList(group.getName()));
		} else {
			params.remove("group");
		}
		
		logger.debug("setting group in session: {} group={}",(fop != null ? fop.getName() : null),(group != null ? group.getName() : null));
		// change the URL to reflect fop and group
		event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}
	
	public boolean isIgnoreGroup();

}