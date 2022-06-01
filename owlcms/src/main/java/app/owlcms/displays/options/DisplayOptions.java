/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.options;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.components.fields.LocalizedDecimalField;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

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
        // logger.debug("addSoundEntries {}",LoggerUtils.stackTrace());

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
        rbgroup.setValue(silentMode);
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

    public static void addSwitchableEntries(VerticalLayout layout, Component target, DisplayParameters dp) {

        boolean switchable = dp.isSwitchableDisplay();
        Button publicDisplay = new Button(Translator.translate("DisplayParameters.PublicDisplay"));
        Button warmupDisplay = new Button(Translator.translate("DisplayParameters.WarmupDisplay"));

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        rbgroup.setLabel(Translator.translate("DisplayParameters.SwitchableSettings"));
        rbgroup.setHelperText(Translator.translate("DisplayParameters.SwitchableHelper"));
        rbgroup.setItems(Boolean.TRUE, Boolean.FALSE);
        rbgroup.setValue(switchable);
        rbgroup.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? publicDisplay : warmupDisplay));
        rbgroup.addValueChangeListener(e -> {
            Boolean silenced = e.getValue();
            dp.switchSwitchable(target, silenced, true);
        });

        layout.add(rbgroup);
    }

    public static void addSizingEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        LocalizedDecimalField fontSizeField = new LocalizedDecimalField();
        TextField wrappedTextField = fontSizeField.getWrappedTextField();
        wrappedTextField.setLabel(Translator.translate("DisplayParameters.FontSizeLabel"));
        wrappedTextField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        wrappedTextField.addFocusListener(f -> {
            dp.getDialogTimer().cancel();
            dp.getDialogTimer().purge();
        });
        fontSizeField.setValue(dp.getEmFontSize());
        fontSizeField.addValueChangeListener(e -> {
            dp.getDialogTimer().cancel();

            Double emSize = e.getValue();
            dp.switchEmFontSize(target, emSize, true);
            UI.getCurrent().getPage().reload();
        });

        layout.add(fontSizeField);
    }

}
