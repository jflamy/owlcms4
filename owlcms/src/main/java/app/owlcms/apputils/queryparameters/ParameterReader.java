package app.owlcms.apputils.queryparameters;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import ch.qos.logback.classic.Logger;

public interface ParameterReader extends HasUrlParameter<String> {

	final Logger logger = (Logger) LoggerFactory.getLogger(ParameterReader.class);

	void doUpdateUrlLocation(UI ui, Location location, Map<String, List<String>> queryParameterMap);

	Location getLocation();

	UI getLocationUI();

	Map<String, List<String>> getUrlParameterMap();

	boolean isShowInitialDialog();

	Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap);

	void setLocation(Location location);

	void setLocationUI(UI locationUI);

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

	public default void processBooleanParam(Map<String, List<String>> params, String paramName,
	        Consumer<Boolean> doer) {
		List<String> paramValues = params.get(paramName);
		boolean value = false;
		if (paramValues == null || paramValues.isEmpty()) {
			value = getDefaultParamValue(paramName);
		} else {
			value = paramValues.get(0).toLowerCase().equals("true");
		}
		doer.accept(value);
		updateParam(params, paramName, value ? "true" : "false");
	}

	public default boolean getDefaultParamValue(String paramName) {
		boolean value = false;
		QueryParameters dp = getDefaultParameters();
		if (dp != null) {
			List<String> defaultValues = dp.getParameters().get(paramName);
			if (defaultValues != null) {
				String defaultVal = defaultValues.get(0);
				value = defaultVal.toLowerCase().equals("true");
			}
		}
		return value;
	}

	public void setDefaultParameters(QueryParameters qp);

	public QueryParameters getDefaultParameters();

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
		//logger.debug("ParameterReader setParameter");
		Location location = event.getLocation();
		setLocation(location);
		setLocationUI(event.getUI());

		QueryParameters queryParameters = location.getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters();
		Map<String, List<String>> params = readParams(location, parametersMap);
		setUrlParameterMap(parametersMap);
		doUpdateUrlLocation(getLocationUI(), location, params);
	}

	public default Map<String, List<String>> removeDefaultValues(Map<String, List<String>> parametersMap) {
		QueryParameters defaultParameters = getDefaultParameters();
		if (defaultParameters == null) {
			return parametersMap;
		}
		Map<String, List<String>> defaults = defaultParameters.getParameters();

		Iterator<Entry<String, List<String>>> paramsIterator = parametersMap.entrySet().iterator();
		var newParams = new TreeMap<String, List<String>>();
		while (paramsIterator.hasNext()) {
			var entry = paramsIterator.next();
			var defaultVal = defaults.get(entry.getKey());
			if (defaultVal == null || (defaultVal.get(0).compareTo(entry.getValue().get(0)) != 0)) {
				newParams.put(entry.getKey(), entry.getValue());
			}
		}
		return newParams;
	}

}