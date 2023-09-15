package app.owlcms.apputils.queryparameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;

import ch.qos.logback.classic.Logger;

public interface ParameterReader extends HasUrlParameter<String> {

	final Logger logger = (Logger) LoggerFactory.getLogger(ParameterReader.class);

	void doUpdateUrlLocation(UI ui, Location location, Map<String, List<String>> queryParameterMap);

	Location getLocation();

	UI getLocationUI();

	Map<String, List<String>> getUrlParameterMap();

	boolean isShowInitialDialog();

	HashMap<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap);

	void setLocation(Location location);

	void setLocationUI(UI locationUI);

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
	 * @see com.vaadin.flow.router.HasUrlParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.Object)
	 */
	@Override
	void setParameter(BeforeEvent event, String unused);

	/**
	 * By default, there is no initial dialog. Classes that need one must override.
	 *
	 * @param b
	 */
	void setShowInitialDialog(boolean b);

	void setUrlParameterMap(Map<String, List<String>> parametersMap);

	void storeInSessionStorage(String key, String value);

	void storeReturnURL(Location location2);

	void updateParam(Map<String, List<String>> cleanParams, String parameter, String value);

	void updateURLLocation(UI ui, Location location, String parameter, String value);

}