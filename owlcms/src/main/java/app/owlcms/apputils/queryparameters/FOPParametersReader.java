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
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

public interface FOPParametersReader extends ParameterReader, FOPParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(FOPParametersReader.class);

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#doUpdateUrlLocation(com.vaadin.flow.component.UI,
	 *      com.vaadin.flow.router.Location, java.util.Map)
	 */
	@Override
	public default void doUpdateUrlLocation(UI ui, Location location, Map<String, List<String>> queryParameterMap) {

		Map<String, List<String>> nq = removeDefaultValues(queryParameterMap);

		setUrlParameterMap(nq);
		Location location2 = new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(nq)));
		ui.getPage().getHistory().replaceState(null, location2.getPathWithQueryParameters());
		setLocation(location2);
		if (logger.isDebugEnabled()) {
			logger.debug("**** updatingLocation {} {}", location2.getPathWithQueryParameters(),
			        LoggerUtils.whereFrom());
		}
		storeReturnURL(location2);
	}

	public default boolean isIgnoreFopFromURL() {
		return false;
	}

	public default boolean isIgnoreGroupFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#readParams(com.vaadin.flow.router.Location,
	 *      java.util.Map)
	 */
	@Override
	@SuppressWarnings("null")
	public default Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		logger.warn("FopParameter readParams");
		HashMap<String, List<String>> newParameterMap = new HashMap<>(parametersMap);

		// get the fop from the query parameters, set to the default FOP if not provided
		FieldOfPlay fop = null;

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
				fop = OwlcmsFactory.getFOPByName(decoded);
			} else if (OwlcmsSession.getFop() != null) {
				// logger.trace("OwlcmsSession.getFop() {}", OwlcmsSession.getFop());
				fop = OwlcmsSession.getFop();
			}
			if (fop == null) {
				fop = OwlcmsFactory.getDefaultFOP();
			}
			newParameterMap.put(FOP, Arrays.asList(URLUtils.urlEncode(fop.getName())));
			OwlcmsSession.setFop(fop);
		} else {
			newParameterMap.remove(FOP);
		}

		// get the group from query parameters
		Group group = null;
		if (!isIgnoreGroupFromURL()) {
			List<String> groupNames = parametersMap.get(GROUP);
			if (groupNames != null && groupNames.get(0) != null) {
				String decoded = URLDecoder.decode(groupNames.get(0), StandardCharsets.UTF_8);
				// logger.trace("URL group = {} decoded = {}",groupNames.get(0), decoded);
				group = GroupRepository.findByName(decoded);
				fop.loadGroup(group, this, true);
			} else {
				group = (fop != null ? fop.getGroup() : null);
			}
			if (group != null) {
				newParameterMap.put(GROUP, Arrays.asList(URLUtils.urlEncode(group.getName())));
			}
		} else {
			newParameterMap.remove(GROUP);
		}

		logger.debug("URL parsing: {} OwlcmsSession: fop={} group={}", LoggerUtils.whereFrom(),
		        (fop != null ? fop.getName() : null), (group != null ? group.getName() : null));

		setUrlParameterMap(removeDefaultValues(newParameterMap));
		return getUrlParameterMap();
	}

	/*
	 * Retrieve parameter(s) from URL and update according to current settings.
	 *
	 * The values are stored in the URL in order to allow bookmarking and easy reloading.
	 *
	 * Note: what Vaadin calls a parameter is in the REST style, actually part of the URL path. We use the old-style
	 * Query parameters for our purposes.
	 *
	 * @see com.vaadin.flow.router.HasUrlParameter#setParameter(com.vaadin.flow.router. BeforeEvent, java.lang.Object)
	 */
	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent,
	 *      java.lang.String)
	 */
	@Override
	public default void setParameter(BeforeEvent event, @OptionalParameter String routeParameter) {
		logger.warn("FOPParameter setParameter");
		ParameterReader.super.setParameter(event, routeParameter);
		if (routeParameter != null) {
			setRouteParameter(routeParameter);
		}
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#setShowInitialDialog(boolean)
	 */
	@Override
	public default void setShowInitialDialog(boolean b) {
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#storeInSessionStorage(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public default void storeInSessionStorage(String key, String value) {
		UI.getCurrent().getElement().executeJs("window.sessionStorage.setItem($0, $1);", key, value);
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#storeReturnURL(com.vaadin.flow.router.Location)
	 */
	@Override
	public default void storeReturnURL(Location location2) {
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#updateParam(java.util.Map, java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public default void updateParam(Map<String, List<String>> parameters, String parameter, String value) {
		if (value != null) {
			parameters.put(parameter, Arrays.asList(value));
		} else {
			parameters.remove(parameter);
		}
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#updateURLLocation(com.vaadin.flow.component.UI,
	 *      com.vaadin.flow.router.Location, java.lang.String, java.lang.String)
	 */
	@Override
	public default void updateURLLocation(UI ui, Location location, String parameter, String value) {
		if (logger.isDebugEnabled()) {
			logger.debug("**** updating {} to {} from {}", parameter, value, LoggerUtils.whereFrom());
		}
		Map<String, List<String>> parametersMap = new TreeMap<>(location.getQueryParameters().getParameters());

		// get current values
		if (!this.isIgnoreFopFromURL()) {
			FieldOfPlay fop = OwlcmsSession.getFop();
			updateParam(parametersMap, FOP, fop != null ? fop.getName() : null);
		} else {
			updateParam(parametersMap, FOP, null);
		}

		// override with the update
		updateParam(parametersMap, parameter, value);
		doUpdateUrlLocation(ui, location, parametersMap);
	}

}