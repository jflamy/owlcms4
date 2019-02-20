package org.ledocte.owlcms.ui.home;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ledocte.owlcms.OwlcmsFactory;
import org.ledocte.owlcms.OwlcmsSession;
import org.ledocte.owlcms.state.FieldOfPlayState;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.page.History;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import ch.qos.logback.classic.Logger;

public interface QueryParameterReader extends HasUrlParameter<String>{

	final static Logger logger = (Logger)LoggerFactory.getLogger(QueryParameterReader.class);
	
	/*
	 * Process query parameters
	 * 
	 * @see
	 * com.vaadin.flow.router.HasUrlParameter#setParameter(com.vaadin.flow.router.
	 * BeforeEvent, java.lang.Object)
	 */
	/* (non-Javadoc)
	 * @see org.ledocte.owlcms.ui.lifting.URLParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		logger.warn("entering setParameter");
		Location location = event.getLocation();
		QueryParameters queryParameters = location.getQueryParameters();

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
		OwlcmsSession.setAttribute("fop", fop);
		
		History history = event.getUI().getPage().getHistory();
		history.replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}

}