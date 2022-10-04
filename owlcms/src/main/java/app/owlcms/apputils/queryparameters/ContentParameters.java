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

    public boolean isSilenced();

    public default boolean isSilencedByDefault() {
        return true;
    }

    public void setSilenced(boolean silent);

    public default void switchSoundMode(Component target, boolean silent, boolean updateURL) {
        setSilenced(silent);
        // logger.debug("switching sound");

        if (updateURL) {
            updateURLLocation(getLocationUI(), getLocation(), SILENT, silent ? "true" : "false");
        }
        buildDialog(target);
    }

    public default void buildDialog(Component target) {
    }

    public default boolean isSingleReferee() {
        return false;
    }

    public default void switchSingleRefereeMode(Component component, boolean b, boolean updateURL) {
        setSingleReferee(b);
        if (updateURL) {
            updateURLLocation(getLocationUI(), getLocation(), SINGLEREF, b ? "true" : "false");
        }
    }    
    
    public default void switchImmediateDecisionMode(Component component, boolean b, boolean updateURL) {
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop == null) {
            return;
        }
        fop.setAnnouncerDecisionImmediate(b);
        if (updateURL) {
            updateURLLocation(getLocationUI(), getLocation(), IMMEDIATE, b ? "true" : "false");
        }
    }

    public default void setSingleReferee(boolean b) {
    };
    
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
        
        List<String> immParams = params.get(IMMEDIATE);
        boolean imm = immParams != null && !immParams.isEmpty()
                && immParams.get(0).toLowerCase().equals("true");
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop != null) {
            fop.setAnnouncerDecisionImmediate(imm);
            switchImmediateDecisionMode((Component) this, imm, false);
            updateParam(params, IMMEDIATE, imm ? "true" : null);
        }
        return params;
    }
}
