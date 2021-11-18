package app.owlcms.displays.options;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;
import app.owlcms.i18n.Translator;

public class DisplayOptions {
    final static Logger logger = (Logger) LoggerFactory.getLogger(DisplayOptions.class);

    public static void addLightingEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        boolean darkMode = dp.isDarkMode();
        Button darkButton = new Button(Translator.translate(DisplayParameters.DARK));
        darkButton.getStyle().set("color", "white");
        darkButton.getStyle().set("background-color", "black");

        Button lightButton = new Button(Translator.translate(DisplayParameters.LIGHT));
        lightButton.getStyle().set("color", "black");
        lightButton.getStyle().set("background-color", "white");

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        rbgroup.setLabel(Translator.translate("DisplayParameters.VisualSettings"));
        rbgroup.setItems(Boolean.TRUE, Boolean.FALSE);
        rbgroup.setValue(Boolean.valueOf(darkMode));
        rbgroup.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? darkButton : lightButton));
        rbgroup.addValueChangeListener(e -> {
            dp.switchLightingMode(target, e.getValue(), true);
        });

        layout.add(rbgroup);
    }

    public static void addSoundEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop != null) {
            if (fop.isEmitSoundsOnServer()) {
                Label label = new Label(Translator.translate("DisplayParameters.SoundsOnServer"));
                label.setWidth("25em");
                layout.add(label);
                return;
            }
        }

        boolean silentMode = dp.isSilenced();
        Button silentButton = new Button(Translator.translate("DisplayParameters.Silent", AvIcons.VOLUME_OFF.create()));
        Button soundButton = new Button(Translator.translate("DisplayParameters.SoundOn", AvIcons.VOLUME_UP.create()));

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        rbgroup.setLabel(Translator.translate("DisplayParameters.SoundSettings"));
        rbgroup.setHelperText(Translator.translate("DisplayParameters.SoundHelper"));
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
