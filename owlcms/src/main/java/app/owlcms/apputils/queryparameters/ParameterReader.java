package app.owlcms.apputils.queryparameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

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
	
	public default void processBooleanParam(HashMap<String, List<String>> params, String paramName,
	        Consumer<Boolean> doer) {
		List<String> paramValues = params.get(paramName);
		logger.warn("param {} values={}", paramName, paramValues);
		boolean value = false;
		if (paramValues == null || paramValues.isEmpty()) {
			value = getDefaultParamValue(paramName);
		} else {
			value = paramValues.get(0).toLowerCase().equals("true");
		}
		doer.accept(value);
		updateParam(params, paramName, value ? "true" : "false");
		logger.warn("updated values for {} {}", paramName, params.get(paramName));
	}

	public default boolean getDefaultParamValue(String paramName) {
		boolean value = false;
		QueryParameters dp = getDefaultParameters();
		if (dp != null) {
			List<String> defaultValues = dp.getParameters().get(paramName);
			if (defaultValues != null) {
				logger.warn("param {} DEFAULT values={}", paramName, defaultValues);
				String defaultVal = defaultValues.get(0);
				value = defaultVal.toLowerCase().equals("true");
			}
		}
		return value;
	}
	
	public default QueryParameters getDefaultParameters() {
		return null;
	}

}