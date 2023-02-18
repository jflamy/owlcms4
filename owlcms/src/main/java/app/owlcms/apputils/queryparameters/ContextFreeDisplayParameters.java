package app.owlcms.apputils.queryparameters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Location;

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
	
	@Override
	public default HashMap<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		FOPParameters r = (this);
		HashMap<String, List<String>> newParameterMap = new HashMap<>(parametersMap);

		// get the fop from the query parameters, set to the default FOP if not provided
		FieldOfPlay fop = null;

		@Nonnull
		List<String> fopNames = parametersMap.get("fop");
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
			newParameterMap.put("fop", Arrays.asList(URLUtils.urlEncode(fop.getName())));
			this.setFop(fop);
		} else {
			newParameterMap.remove("fop");
		}

		// get the group from query parameters
		Group group = null;
		if (!r.isIgnoreGroupFromURL()) {
			List<String> groupNames = parametersMap.get("group");
			if (groupNames != null && groupNames.get(0) != null) {
				group = GroupRepository.findByName(groupNames.get(0));
			} else {
				// medals are for a past group, not the current group
				// group = (fop != null ? fop.getGroup() : null);
			}
			if (group != null) {
				newParameterMap.put("group", Arrays.asList(URLUtils.urlEncode(group.getName())));
			}
			this.setGroup(group);
		} else {
			newParameterMap.remove("group");
		}

		// get the category from query parameters
		Category cat = null;
		if (!r.isIgnoreGroupFromURL()) {
			List<String> catCodes = parametersMap.get("cat");
			if (catCodes != null && catCodes.get(0) != null) {
				cat = CategoryRepository.findByCode(catCodes.get(0));
			}
			// logger.trace("cat = {}", cat);
			if (cat != null) {
				newParameterMap.put("cat", Arrays.asList(URLUtils.urlEncode(cat.getName())));
			}
			this.setCategory(cat);
		} else {
			newParameterMap.remove("cat");
		}

		FOPParameters.logger.warn("URL parsing: {} OwlcmsSession: fop={} cat={}", LoggerUtils.whereFrom(),
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
		setUrlParameterMap(params);
		return params;
	}

	public void setGroup(Group group);

	public void setCategory(Category cat);

	public void setFop(FieldOfPlay fop);

}

