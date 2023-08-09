package app.owlcms.apputils.queryparameters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Location;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;

public interface ContextFreeDisplayParameters extends DisplayParameters {

	final static String AGEGROUP = "ag";

	@Override
	public default HashMap<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		FOPParameters r = (this);
		HashMap<String, List<String>> newParameterMap = new HashMap<>(parametersMap);

		// get the fop from the query parameters, set to the default FOP if not provided
		FieldOfPlay fop = null;

		@Nonnull
		List<String> fopNames = parametersMap.get(FOP);
		boolean fopFound = fopNames != null && fopNames.get(0) != null;
		if (!fopFound) {
			r.setShowInitialDialog(true);
		}

		if (!r.isIgnoreFopFromURL()) {
			if (fopFound) {
				FOPParameters.logger.trace("fopNames {}", fopNames);
				fop = OwlcmsFactory.getFOPByName(fopNames.get(0));
			} else if (OwlcmsSession.getFop() != null) {
				FOPParameters.logger.trace("OwlcmsSession.getFop() {}", OwlcmsSession.getFop());
				fop = OwlcmsSession.getFop();
			}
			if (fop == null) {
				FOPParameters.logger.trace("OwlcmsFactory.getDefaultFOP() {}", OwlcmsFactory.getDefaultFOP());
				fop = OwlcmsFactory.getDefaultFOP();
			}
			newParameterMap.put(FOP, Arrays.asList(URLUtils.urlEncode(fop.getName())));
			this.setFop(fop);
		} else {
			newParameterMap.remove(FOP);
		}

		// get the group from query parameters
		Group group = null;
		if (!r.isIgnoreGroupFromURL()) {
			List<String> groupNames = parametersMap.get(GROUP);
			if (groupNames != null && groupNames.get(0) != null) {
				group = GroupRepository.findByName(groupNames.get(0));
			} else if (fop != null && isVideo(location) && fop.getVideoGroup() != null) {
				group = fop.getVideoGroup();
			}
			if (group != null) {
				newParameterMap.put(GROUP, Arrays.asList(URLUtils.urlEncode(group.getName())));
			}
			this.setGroup(group);
		} else if (fop != null && isVideo(location) && fop.getVideoGroup() != null) {
			group = fop.getVideoGroup();
			newParameterMap.put(GROUP, Arrays.asList(URLUtils.urlEncode(group.getName())));
		} else {
			newParameterMap.remove(GROUP);
		}

		// get the category from query parameters
		Category cat = null;
		if (!r.isIgnoreGroupFromURL()) {
			List<String> catCodes = parametersMap.get(CATEGORY);
			if (catCodes != null && catCodes.get(0) != null) {
				cat = CategoryRepository.findByCode(catCodes.get(0));
			} else if (fop != null && isVideo(location) && fop.getVideoCategory() != null) {
				cat = fop.getVideoCategory();
			}
			// logger.trace("cat = {}", cat);
			if (cat != null) {
				newParameterMap.put(CATEGORY, Arrays.asList(URLUtils.urlEncode(cat.getName())));
			}
			this.setCategory(cat);
		} else if (fop != null && isVideo(location) && fop.getVideoCategory() != null) {
			cat = fop.getVideoCategory();
			newParameterMap.put(CATEGORY, Arrays.asList(URLUtils.urlEncode(cat.getName())));
		} else {
			newParameterMap.remove(CATEGORY);
		}
		
		// get the age group from query parameters
		AgeGroup ageGroup = null;
		if (!r.isIgnoreGroupFromURL()) {
			List<String> ageGroupNames = parametersMap.get(AGEGROUP);
			if (ageGroupNames != null && ageGroupNames.get(0) != null) {
				//CHECK urldecode the agegroup ?
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

		logger.debug("URL parsing - {} OwlcmsSession: fop={} cat={}", LoggerUtils.whereFrom(),
		        (fop != null ? fop.getName() : null), (cat != null ? cat.getName() : null));

		HashMap<String, List<String>> params = newParameterMap;
		List<String> darkParams = params.get(DARK);
		// dark is the default. dark=false or dark=no or ... will turn off dark mode.
		boolean darkMode = darkParams == null || darkParams.isEmpty() || darkParams.get(0).toLowerCase().equals("true");
		setDarkMode(darkMode);
		switchLightingMode((Component) this, darkMode, false);
		updateParam(params, DARK, !isDarkMode() ? "false" : null);

		List<String> silentParams = params.get(SILENT);
		// silent is the default. silent=false will cause sound
		boolean silentMode = silentParams == null || silentParams.isEmpty()
		        || silentParams.get(0).toLowerCase().equals("true");
		if (!isSilencedByDefault()) {
			// for referee board, default is noise
			silentMode = silentParams != null && !silentParams.isEmpty()
			        && silentParams.get(0).toLowerCase().equals("true");
		}
		switchSoundMode((Component) this, silentMode, false);
		updateParam(params, SILENT, !isSilenced() ? "false" : "true");
		
		List<String> downSilentParams = params.get(DOWNSILENT);
		// silent is the default. silent=false will cause sound
		boolean downSilentMode = downSilentParams == null || downSilentParams.isEmpty()
		        || downSilentParams.get(0).toLowerCase().equals("true");
		if (!isSilencedByDefault()) {
			// for referee board, default is noise
			downSilentMode = downSilentParams != null && !downSilentParams.isEmpty()
			        && downSilentParams.get(0).toLowerCase().equals("true");
		}
		switchDownMode((Component) this, downSilentMode, false);
		updateParam(params, DOWNSILENT, !isDownSilenced() ? "false" : "true");
		

		List<String> sizeParams = params.get(FONTSIZE);
		Double emSize;
		try {
			emSize = (sizeParams != null && !sizeParams.isEmpty() ? Double.parseDouble(sizeParams.get(0)) : 0.0D);
			if (emSize > 0.0D) {
				setEmFontSize(emSize);
				updateParam(params, FONTSIZE, emSize.toString());
			} else {
				setEmFontSize(null);
				updateParam(params, FONTSIZE, null);
			}
			buildDialog((Component) this);
		} catch (NumberFormatException e) {
			emSize = 0.0D;
			setEmFontSize(null);
			updateParam(params, FONTSIZE, null);
		}
		
		List<String> twParams = params.get(TEAMWIDTH);
		Double tWidth;
		try {
			tWidth = (twParams != null && !twParams.isEmpty() ? Double.parseDouble(twParams.get(0)) : 0.0D);
			if (tWidth > 0.0D) {
				setTeamWidth(tWidth);
				updateParam(params, FONTSIZE, tWidth.toString());
			} else {
				setTeamWidth(null);
				updateParam(params, FONTSIZE, null);
			}
			buildDialog((Component) this);
		} catch (NumberFormatException e) {
			tWidth = 10.0D;
			setEmFontSize(null);
			updateParam(params, FONTSIZE, null);
		}

		List<String> abbParams = params.get(ABBREVIATED);
		boolean abb;
		abb = (abbParams != null && !abbParams.isEmpty() ? Boolean.valueOf(abbParams.get(0)) : false);
		setAbbreviatedName(abb);
		updateParam(params, ABBREVIATED, abb ? "true" : null);
		buildDialog((Component) this);
		
		setUrlParameterMap(params);
		return params;
	}

	public void setAgeGroup(AgeGroup ag);

	public default boolean isVideo(Location location) {
		return location.getPath().endsWith("video");
	}

	public void setGroup(Group group);

	public void setCategory(Category cat);

	public void setFop(FieldOfPlay fop);

}
