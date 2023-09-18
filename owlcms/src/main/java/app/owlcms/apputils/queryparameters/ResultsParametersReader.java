package app.owlcms.apputils.queryparameters;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.Location;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

public interface ResultsParametersReader extends ResultsParameters, FOPParametersReader {
	
	public static final String CATEGORY = "cat";
	public static final String AGEGROUP_PREFIX = "agp";
	public static final String AGEDIVISION = "ad";
	public static final String AGEGROUP = "ag";

	@Override
	public default Map<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		Logger logger = (Logger) LoggerFactory.getLogger(ResultsParametersReader.class);
		var fop = getFop();

		logger.warn("ContextFreeParametersReader readParam");
		// handle previous parameters by calling superclass
		Map<String, List<String>> newParameterMap = FOPParametersReader.super.readParams(location, parametersMap);
		
		setVideo(isVideo(location));

		// get the age group from query parameters
		AgeGroup ageGroup = null;
		if (!this.isIgnoreGroupFromURL()) {
			List<String> ageGroupNames = parametersMap.get(AGEGROUP);
			if (ageGroupNames != null && ageGroupNames.get(0) != null) {
				ageGroup = AgeGroupRepository.findByName(ageGroupNames.get(0));
			} else if (fop != null && isVideo(location) && fop.getVideoAgeGroup() != null) {
				ageGroup = fop.getVideoAgeGroup();
			}
			// logger.trace("ageGroup = {}", ageGroup);
			if (ageGroup != null) {
				newParameterMap.put(AGEGROUP, Arrays.asList(URLUtils.urlEncode(ageGroup.getName())));
			}
			this.setAgeGroup(ageGroup);
		} else if (fop != null && isVideo(location) && fop.getVideoAgeGroup() != null) {
			ageGroup = fop.getVideoAgeGroup();
			newParameterMap.put(AGEGROUP, Arrays.asList(URLUtils.urlEncode(ageGroup.getName())));
		} else {
			newParameterMap.remove(AGEGROUP);
		}
		
		List<String> ageDivisionParams = newParameterMap.get(AGEDIVISION);
		try {
			String ageDivisionName = (ageDivisionParams != null
			        && !ageDivisionParams.isEmpty() ? ageDivisionParams.get(0) : null);
			AgeDivision valueOf = AgeDivision.valueOf(ageDivisionName);
			setAgeDivision(valueOf);
		} catch (Exception e) {
			setAgeDivision(null);
		}
		// remove if now null
		String value = getAgeDivision() != null ? getAgeDivision().name() : null;
		updateParam(newParameterMap, AGEDIVISION, value);

		List<String> ageGroupParams = newParameterMap.get(AGEGROUP_PREFIX);
		// no age group is the default
		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
		setAgeGroupPrefix(ageGroupPrefix);
		String value2 = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateParam(newParameterMap, AGEGROUP_PREFIX, value2);

		List<String> catParams = newParameterMap.get(CATEGORY);
		String catParam = (catParams != null && !catParams.isEmpty() ? catParams.get(0) : null);
		catParam = catParam != null ? URLDecoder.decode(catParam, StandardCharsets.UTF_8) : null;
		setCategory(CategoryRepository.findByCode(catParam));
		String catValue = getCategory() != null ? getCategory().toString() : null;
		updateParam(newParameterMap, CATEGORY, catValue);

		setUrlParameterMap(removeDefaultValues(newParameterMap));
		return getUrlParameterMap();
	}

	private boolean isVideo(Location location) {
		return location.getPath().endsWith(DisplayParameters.VIDEO);
	}
}
