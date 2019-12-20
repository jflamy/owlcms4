/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays;

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

import app.owlcms.ui.shared.QueryParameterReader;

/**
 * @author owlcms
 *
 */
public interface DarkModeParameters extends QueryParameterReader {

    public static final String DARK = "dark";

    public default void buildContextMenu(Component target) {
        ContextMenu oldContextMenu = getContextMenu();
        if (oldContextMenu != null) {
            oldContextMenu.setTarget(null);
        }

        ContextMenu contextMenu = new ContextMenu();

        if (isDarkMode()) {
            Button darkButton = new Button(contextMenu.getTranslation(DARK), e -> setDarkMode(target, true, false));
            darkButton.setThemeName("secondary contrast");
            contextMenu.addItem(darkButton);
            Button lightButton = new Button(contextMenu.getTranslation("light"),
                    e -> setDarkMode(target, false, false));
            lightButton.setThemeName("primary contrast");
            contextMenu.addItem(lightButton);
        } else {
            Button lightButton = new Button(contextMenu.getTranslation("light"),
                    e -> setDarkMode(target, false, false));
            lightButton.setThemeName("primary contrast");
            contextMenu.addItem(lightButton);
            Button darkButton = new Button(contextMenu.getTranslation(DARK), e -> setDarkMode(target, true, false));
            darkButton.setThemeName("secondary contrast");
            contextMenu.addItem(darkButton);
        }
        contextMenu.setOpenOnClick(true);
        contextMenu.setTarget(target);
        setContextMenu(contextMenu);
    }

    public default void doNotification(boolean dark) {
        Notification n = new Notification();
        H2 h2 = new H2();
        h2.setText(h2.getTranslation("darkMode." + (dark ? DARK : "light")));
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
        target.getElement().getClassList().set("light", !dark);
        setDarkMode(dark);
        updateURLLocation(getLocationUI(), getLocation(), dark ? null : "false");

        if (notify) {
            doNotification(dark);
        }
    }

    /*
     * Process query parameters
     *
     * @see app.owlcms.ui.group.URLParameter#setParameter(com.vaadin.flow.router.
     * BeforeEvent, java.lang.String)
     *
     * @see
     * app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router
     * .BeforeEvent, java.lang.String)
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
        event.getUI().getPage().getHistory().replaceState(null,
                new Location(location.getPath(), new QueryParameters(cleanParams)));
    }

    public default void updateURLLocation(UI ui, Location location, String mode) {
        // change the URL to reflect fop group
        HashMap<String, List<String>> params = new HashMap<>(
                location.getQueryParameters().getParameters());
        if (mode != null) {
            params.put(DARK, Arrays.asList(mode));
        } else {
            params.remove(DARK);
        }
        ui.getPage().getHistory().replaceState(null, new Location(location.getPath(), new QueryParameters(params)));
    }

}
