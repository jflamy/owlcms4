package app.owlcms.displays.options;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import app.owlcms.utils.SoundUtils;
import app.owlcms.utils.queryparameters.DisplayParameters;
import ch.qos.logback.classic.Logger;

public class DisplayOptions {
    final static Logger logger = (Logger) LoggerFactory.getLogger(DisplayOptions.class);

    public static void addLightingEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        boolean darkMode = dp.isDarkMode();
        Button darkButton = new Button(layout.getTranslation(DisplayParameters.DARK));
        darkButton.getStyle().set("color", "white");
        darkButton.getStyle().set("background-color", "black");

        Button lightButton = new Button(layout.getTranslation(DisplayParameters.LIGHT));
        lightButton.getStyle().set("color", "black");
        lightButton.getStyle().set("background-color", "white");

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        // rbgroup.setLabel("Title of radiobuttongroup");
        rbgroup.setItems(Boolean.TRUE, Boolean.FALSE);
        rbgroup.setValue(Boolean.valueOf(darkMode));
        rbgroup.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? darkButton : lightButton));
        rbgroup.addValueChangeListener(e -> {
            dp.switchLightingMode(target, e.getValue(), true);
        });

        layout.add(rbgroup);
    }

    public static void addSoundEntries(VerticalLayout layout, Component target, DisplayParameters dp) {

        boolean silentMode = dp.isSilenced();
        Button silentButton = new Button(layout.getTranslation(DisplayParameters.SILENT));
        silentButton.getStyle().set("color", "white");
        silentButton.getStyle().set("background-color", "blue");

        Button soundButton = new Button(layout.getTranslation(DisplayParameters.SOUND));
        soundButton.getStyle().set("color", "black");
        soundButton.getStyle().set("background-color", "yellow");

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        // rbgroup.setLabel("Title of radiobuttongroup");
        rbgroup.setItems(Boolean.TRUE, Boolean.FALSE);
        rbgroup.setValue(Boolean.valueOf(silentMode));
        rbgroup.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? silentButton : soundButton));
        rbgroup.addValueChangeListener(e -> {
            Boolean silenced = e.getValue();
            dp.switchSoundMode(target, silenced, true);
            if (!silenced) {
                SoundUtils.doEnableAudioContext(target.getElement());
            }
        });

        layout.add(rbgroup);
    }
}
