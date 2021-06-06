package app.owlcms.displays.menu;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;

import app.owlcms.utils.queryparameters.DisplayParameters;

public class DisplayContextMenu {

    public static void addSoundEntries(ContextMenu contextMenu, Component target, DisplayParameters dp) {

        boolean silentMode = dp.isSilenced();
        Button silentButton = new Button(contextMenu.getTranslation(DisplayParameters.SILENT),
                e -> dp.switchSoundMode(target, true, true));
        silentButton.getStyle().set("color", "white");
        silentButton.getStyle().set("background-color", "blue");

        Button soundButton = new Button(contextMenu.getTranslation(DisplayParameters.SOUND),
                e -> dp.switchSoundMode(target, false, true));
        soundButton.getStyle().set("color", "black");
        soundButton.getStyle().set("background-color", "yellow");

        if (silentMode) {
            contextMenu.addItem(silentButton);
            contextMenu.addItem(soundButton);
        } else {
            contextMenu.addItem(soundButton);
            contextMenu.addItem(silentButton);
        }
    }

    public static void addLightingEntries(ContextMenu contextMenu, Component target, DisplayParameters dp) {
        boolean darkMode = dp.isDarkMode();
        Button darkButton = new Button(contextMenu.getTranslation(DisplayParameters.DARK),
                e -> dp.switchLightingMode(target, true, true));
        darkButton.getStyle().set("color", "white");
        darkButton.getStyle().set("background-color", "black");

        Button lightButton = new Button(contextMenu.getTranslation(DisplayParameters.LIGHT),
                e -> dp.switchLightingMode(target, false, true));
        lightButton.getStyle().set("color", "black");
        lightButton.getStyle().set("background-color", "white");

        if (darkMode) {
            contextMenu.addItem(darkButton);
            contextMenu.addItem(lightButton);
        } else {
            contextMenu.addItem(lightButton);
            contextMenu.addItem(darkButton);
        }
    }
}
