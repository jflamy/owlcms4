package app.owlcms.apputils.queryparameters;

import com.vaadin.flow.component.Component;

public interface ContentParameters extends FOPParameters {

    public static final String SILENT = "silent";

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

}
