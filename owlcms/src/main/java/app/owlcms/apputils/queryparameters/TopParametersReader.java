package app.owlcms.apputils.queryparameters;

import java.util.List;
import java.util.Map;

import com.vaadin.flow.router.Location;

public interface TopParametersReader extends ResultsParametersReader, DisplayParametersReader {

	@Override
	default Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		var params = ResultsParametersReader.super.readParams(location, parametersMap);
		params = DisplayParametersReader.super.readParams(location, params);
		return params;
	}

}
