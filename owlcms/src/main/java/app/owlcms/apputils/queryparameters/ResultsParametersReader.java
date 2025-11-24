
/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
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
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

public interface ResultsParametersReader extends ResultsParameters, FOPParametersReader {
	// Only one GENDER constant should be declared; remove duplicates

	public static final String CATEGORY = "cat";
	public static final String AGEGROUP_PREFIX = "agp";
	public static final String AGEDIVISION = "ad";
	public static final String AGEGROUP = "ag";
	public static final String NB_ATHLETES = "nbAthletes";
	public static final String GENDER = "gender";
	public static final String CURRENT_ATTEMPT = "currentAttempt";

	@Override
	public default Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {

		@SuppressWarnings("unused")
		Logger logger = (Logger) LoggerFactory.getLogger(ResultsParametersReader.class);
		// logger.debug("ResultsParameterReader readParams");

		// var fop = getFop();

		// handle previous parameters by calling superclass
		Map<String, List<String>> newParameterMap = FOPParametersReader.super.readParams(location, parametersMap);

		// get the age group from query parameters
		AgeGroup ageGroup = null;
		if (!this.isIgnoreGroupFromURL()) {
			List<String> ageGroupNames = parametersMap.get(AGEGROUP);
			if (ageGroupNames != null && ageGroupNames.get(0) != null) {
				ageGroup = AgeGroupRepository.findByName(ageGroupNames.get(0));
			}
			// else if (fop != null && fop.getVideoAgeGroup() != null) {
			// ageGroup = fop.getVideoAgeGroup();
			// }
			if (ageGroup != null) {
				newParameterMap.put(AGEGROUP, Arrays.asList(URLUtils.urlEncode(ageGroup.getName())));
			}
			this.setAgeGroup(ageGroup);
		}
		// else if (fop != null && fop.getVideoAgeGroup() != null) {
		// ageGroup = fop.getVideoAgeGroup();
		// newParameterMap.put(AGEGROUP, Arrays.asList(URLUtils.urlEncode(ageGroup.getName())));
		// }
		else {
			newParameterMap.remove(AGEGROUP);
		}

		List<String> ageDivisionParams = newParameterMap.get(AGEDIVISION);
		String ageDivisionName = (ageDivisionParams != null && !ageDivisionParams.isEmpty() ? ageDivisionParams.get(0) : null);
		if (ageDivisionName == null || ageDivisionName.isEmpty()) {
			setChampionship(null);
			updateParam(newParameterMap, AGEDIVISION, null);
		} else {
			try {
				Championship valueOf = Championship.of(ageDivisionName);
				setChampionship(valueOf);
				String value = getChampionship() != null ? getChampionship().getName() : null;
				updateParam(newParameterMap, AGEDIVISION, value);
			} catch (Exception e) {
				setChampionship(null);
				updateParam(newParameterMap, AGEDIVISION, null);
			}
		}

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


	       // Parse nbAthletes parameter from URL

	       // Parse nbAthletes parameter from URL
	       List<String> nbAthletesParams = newParameterMap.get(NB_ATHLETES);
	       int nbAthletes = 10; // default value
	       if (nbAthletesParams != null && !nbAthletesParams.isEmpty()) {
		       try {
			       nbAthletes = Integer.parseInt(nbAthletesParams.get(0));
		       } catch (NumberFormatException e) {
			       nbAthletes = 10;
		       }
	       }
	       setNbAthletes(nbAthletes);

	       // Parse gender parameter from URL
	       List<String> genderParams = newParameterMap.get(GENDER);
	       app.owlcms.data.athlete.Gender gender = app.owlcms.data.athlete.Gender.MF; // default
	       if (genderParams != null && !genderParams.isEmpty()) {
		       String g = genderParams.get(0);
		       if (g != null) {
			       try {
				       gender = app.owlcms.data.athlete.Gender.valueOf(g.toUpperCase());
			       } catch (IllegalArgumentException e) {
				       gender = app.owlcms.data.athlete.Gender.MF;
			       }
		       }
	       }
	       setGender(gender);

		       setUrlParameterMap(removeDefaultValues(newParameterMap));

		       // Parse displayLifts parameter from URL
		       List<String> displayLiftsParams = newParameterMap.get("displayLifts");
		       boolean displayLifts = false;
		       if (displayLiftsParams != null && !displayLiftsParams.isEmpty()) {
			       String val = displayLiftsParams.get(0);
			       displayLifts = val != null && (val.equalsIgnoreCase("true") || val.equals("1"));
		       }
		       setDisplayLifts(displayLifts);

		       return getUrlParameterMap();
	       }


		// gender getter/setter
		default app.owlcms.data.athlete.Gender getGender() { return app.owlcms.data.athlete.Gender.MF; }
		default void setGender(app.owlcms.data.athlete.Gender gender) {}

	// nbAthletes getter/setter
	default int getNbAthletes() {
		return 10;
	}

	default void setNbAthletes(int nbAthletes) {
	}

}
