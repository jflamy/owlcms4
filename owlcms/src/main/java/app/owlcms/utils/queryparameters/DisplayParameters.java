/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils.queryparameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.displays.menu.DisplayContextMenu;

/**
 * @author owlcms
 *
 */
public interface DisplayParameters extends FOPParameters {

    public static final String LIGHT = "light";
    public static final String DARK = "dark";
    public static final String SILENT = "silent";
    public static final String SOUND = "sound";

    public default void buildContextMenu(Component target) {
        ContextMenu oldContextMenu = getContextMenu();
        if (oldContextMenu != null) {
            oldContextMenu.setTarget(null);
        }
        setContextMenu(null);

        ContextMenu contextMenu = new ContextMenu();
        DisplayContextMenu.addLightingEntries(contextMenu, target, this);
        DisplayContextMenu.addSoundEntries(contextMenu, target, this);

        contextMenu.setOpenOnClick(true);
        contextMenu.setTarget(target);
        setContextMenu(contextMenu);
    }

    public default void doNotification(boolean dark) {
        Notification n = new Notification();
        H2 h2 = new H2();
        h2.setText(h2.getTranslation("darkMode." + (dark ? DARK : LIGHT)));
        h2.getStyle().set("margin", "0");
        n.add(h2);
        n.setDuration(3000);
        n.setPosition(Position.MIDDLE);
        if (dark) {
            n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            h2.getStyle().set("color", "white");
        }
        n.getElement().getStyle().set("font-size", "x-large");
        n.open();
    }

    public ContextMenu getContextMenu();

    public default boolean isDarkMode() {
        return true;
    }

    /**
     * Displays have no sound-emitting elements by default.
     *
     * Those that can emit sound must override this.
     *
     * @return
     */
    public default boolean isSilenced() {
        return true;
    }

    @Override
    public default HashMap<String, List<String>> readParams(Location location,
            Map<String, List<String>> parametersMap) {
        // handle FOP and Group by calling superclass
        HashMap<String, List<String>> params = FOPParameters.super.readParams(location, parametersMap);

        List<String> darkParams = params.get(DARK);
        // dark is the default. dark=false or dark=no or ... will turn off dark mode.
        boolean darkMode = darkParams == null || darkParams.isEmpty() || darkParams.get(0).toLowerCase().equals("true");
        setDarkMode(darkMode);
        switchLightingMode((Component) this, darkMode, false);
        updateParam(params, DARK, !isDarkMode() ? "false" : null);

        List<String> silentParams = params.get(SILENT);
        // dark is the default. dark=false or dark=no or ... will turn off dark mode.
        boolean silentMode = silentParams == null || silentParams.isEmpty()
                || silentParams.get(0).toLowerCase().equals("true");
        switchSoundMode((Component) this, silentMode, false);
        updateParam(params, SILENT, !isSilenced() ? "false" : null);

        return params;
    }

    public void setContextMenu(ContextMenu contextMenu);

    public void setDarkMode(boolean dark);

    /*
     * Process query parameters
     *
     * Note: what Vaadin calls a parameter is in the REST style, actually part of the URL path. We use the old-style
     * Query parameters for our purposes.
     *
     * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
     */
    @Override
    public default void setParameter(BeforeEvent event, @OptionalParameter String unused) {
        Location location = event.getLocation();
        setLocation(location);
        setLocationUI(event.getUI());

        // the OptionalParameter string is the part of the URL path that can be interpreted as REST arguments
        // we use the ? query parameters instead.
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> params = readParams(location, parametersMap);

        event.getUI().getPage().getHistory().replaceState(null,
                new Location(location.getPath(), new QueryParameters(params)));
    }

    public default void setSilenced(boolean silent) {
        // silent by default
    }

    public default void switchLightingMode(Component target, boolean dark, boolean updateURL) {
        target.getElement().getClassList().set(DARK, dark);
        target.getElement().getClassList().set(LIGHT, !dark);
        setDarkMode(dark);
        buildContextMenu(target);
        if (updateURL) {
            updateURLLocation(getLocationUI(), getLocation(), DARK, dark ? null : "false");
        }
    }

    public default void switchSoundMode(Component target, boolean silent, boolean updateURL) {
        setSilenced(silent);
        buildContextMenu(target);
        if (updateURL) {
            updateURLLocation(getLocationUI(), getLocation(), SILENT, silent ? null : "false");
        }
    }

}
