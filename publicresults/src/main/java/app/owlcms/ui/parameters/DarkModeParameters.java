/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.parameters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.utils.URLUtils;

/**
 * @author owlcms
 *
 */
public interface DarkModeParameters extends QueryParameterReader {

    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    public default void buildContextMenu(Component target) {
        ContextMenu oldContextMenu = getContextMenu();
        if (oldContextMenu != null) {
            oldContextMenu.setTarget(null);
        }
        setContextMenu(null);

        ContextMenu contextMenu = new ContextMenu();

        boolean darkMode = isDarkMode();
        Button darkButton = new Button(contextMenu.getTranslation(DARK),
                e -> setDarkMode(target, true, false));
        darkButton.getStyle().set("color", "white");
        darkButton.getStyle().set("background-color", "black");

        Button lightButton = new Button(contextMenu.getTranslation(LIGHT),
                e -> setDarkMode(target, false, false));
        lightButton.getStyle().set("color", "black");
        lightButton.getStyle().set("background-color", "white");

        if (darkMode) {
            contextMenu.addItem(darkButton);
            contextMenu.addItem(lightButton);
        } else {
            contextMenu.addItem(lightButton);
            contextMenu.addItem(darkButton);
        }
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

    public boolean isDarkMode();

    public void setContextMenu(ContextMenu contextMenu);

    public void setDarkMode(boolean dark);

    public default void setDarkMode(Component target, boolean dark, boolean notify) {
        target.getElement().getClassList().set(DARK, dark);
        target.getElement().getClassList().set(LIGHT, !dark);
        setDarkMode(dark);
        buildContextMenu(target);
        updateURLLocation(getLocationUI(), getLocation(), dark ? null : "false");

//        if (notify) {
//            doNotification(dark);
//        }
    }

    /*
     * Process query parameters
     *
     * @see app.owlcms.ui.group.URLParameter#setParameter(com.vaadin.flow.router. BeforeEvent, java.lang.String)
     *
     * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router .BeforeEvent,
     * java.lang.String)
     */
    @Override
    public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        Location location = event.getLocation();
        setLocation(location);
        setLocationUI(event.getUI());
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> cleanParams = computeParams(location, parametersMap);

        List<String> darkParams = cleanParams.get(DARK);
        // dark is the default. dark=false or dark=no or ... will turn off dark mode.
        boolean darkMode = darkParams == null || darkParams.isEmpty() || darkParams.get(0).toLowerCase().equals("true");
        setDarkMode(darkMode);

        // change the URL to reflect retrieved parameters
        Location newLocation = new Location(location.getPath(), new QueryParameters(cleanParams));
        event.getUI().getPage().getHistory().replaceState(null, newLocation);
    }

    public default void updateURLLocation(UI ui, Location location, String mode) {
        // change the URL to reflect fop group
        HashMap<String, List<String>> parametersMap = new HashMap<>(
                location.getQueryParameters().getParameters());
        HashMap<String, List<String>> params = computeParams(location, parametersMap);
        if (mode != null) {
            params.put(DARK, Arrays.asList(mode));
        } else {
            params.remove(DARK);
        }
        ui.getPage().getHistory().replaceState(null, new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
    }

}
