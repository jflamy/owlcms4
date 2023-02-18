package app.owlcms.apputils.queryparameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Location;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;

public interface ContentParameters extends FOPParameters {

	public static final String SILENT = "silent";
	public static final String SINGLEREF = "singleRef";
	public static final String IMMEDIATE = "immediate";

	public default void buildDialog(Component target) {
	}

	public boolean isSilenced();

	public default boolean isSilencedByDefault() {
		return true;
	}

	public default boolean isSingleReferee() {
		return false;
	}

	@Override
	public default HashMap<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		HashMap<String, List<String>> params = FOPParameters.super.readParams(location, parametersMap);

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

		List<String> refParams = params.get(SINGLEREF);
		boolean sr = refParams != null && !refParams.isEmpty()
		        && refParams.get(0).toLowerCase().equals("true");
		setSingleReferee(sr);
		switchSingleRefereeMode((Component) this, sr, false);
		updateParam(params, SINGLEREF, isSingleReferee() ? "true" : null);

		// immediate is true by default, except if single ref.
		List<String> immParams = params.get(IMMEDIATE);
		boolean imm = true;
		if (immParams != null && !immParams.isEmpty()) {
			if (immParams.get(0).toLowerCase().equalsIgnoreCase("false")) {
				imm = false;
			} else if (immParams.get(0).toLowerCase().equalsIgnoreCase("true")) {
				imm = true;
			}
		} else if (sr) {
			imm = false;
		}
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null) {
			fop.setAnnouncerDecisionImmediate(imm);
			switchImmediateDecisionMode((Component) this, imm, false);
			updateParam(params, IMMEDIATE, imm ? null : "false");
		}
		return params;
	}

	public void setSilenced(boolean silent);

	public default void setSingleReferee(boolean b) {
	}

	public default void switchImmediateDecisionMode(Component component, boolean b, boolean updateURL) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop == null) {
			return;
		}
		fop.setAnnouncerDecisionImmediate(b);
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), IMMEDIATE, b ? null : "false");
		}
	}

	public default void switchSingleRefereeMode(Component component, boolean b, boolean updateURL) {
		setSingleReferee(b);
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SINGLEREF, b ? "true" : "false");
		}
	}

	public default void switchSoundMode(Component target, boolean silent, boolean updateURL) {
		setSilenced(silent);
		// logger.debug("switching sound");

		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SILENT, silent ? "true" : "false");
		}
		buildDialog(target);
	}
}
