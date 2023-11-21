/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.options;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.component.fields.LocalizedDecimalField;
import app.owlcms.i18n.Translator;
import app.owlcms.prutils.SoundUtils;
import ch.qos.logback.classic.Logger;

public class DisplayOptions {
    final static Logger logger = (Logger) LoggerFactory.getLogger(DisplayOptions.class);

    public static void addLightingEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.VisualSettings"));
        
        boolean darkMode = dp.isDarkMode();
        Button darkButton = new Button(Translator.translate(DisplayParameters.DARK));
        darkButton.getStyle().set("color", "white");
        darkButton.getStyle().set("background-color", "black");

        Button lightButton = new Button(Translator.translate(DisplayParameters.LIGHT));
        lightButton.getStyle().set("color", "black");
        lightButton.getStyle().set("background-color", "white");

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        rbgroup.setLabel(null);
        rbgroup.setItems(Boolean.TRUE, Boolean.FALSE);
        rbgroup.setValue(Boolean.valueOf(darkMode));
        rbgroup.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? darkButton : lightButton));
        rbgroup.addValueChangeListener(e -> {
            dp.switchLightingMode(target, e.getValue(), true);
        });
        rbgroup.getStyle().set("margin-top", "0px");

        layout.add(label);
        layout.add(rbgroup);
    }

    public static void addSoundEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.SoundSettings"));
        boolean silentMode = dp.isSilenced();
        Button silentButton = new Button(Translator.translate("DisplayParameters.Silent"));//AvIcons.VOLUME_OFF.create()));
        Button soundButton = new Button(Translator.translate("DisplayParameters.SoundOn"));//, AvIcons.VOLUME_UP.create()));

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        rbgroup.setLabel(null);
        rbgroup.setHelperText(Translator.translate("DisplayParameters.SoundHelper"));
        rbgroup.setItems(Boolean.TRUE, Boolean.FALSE);
        rbgroup.setValue(silentMode);
        rbgroup.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? silentButton : soundButton));
        rbgroup.addValueChangeListener(e -> {
            Boolean silenced = e.getValue();
            dp.switchSoundMode(silenced, true);
            if (!silenced) {
                SoundUtils.doEnableAudioContext(target.getElement());
            }
        });

        layout.add(label);
        layout.add(rbgroup);
    }

    public static void addSwitchableEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.SwitchableSettings"));

        boolean switchable = dp.isSwitchableDisplay();
        Button publicDisplay = new Button(Translator.translate("DisplayParameters.PublicDisplay"));
        Button warmupDisplay = new Button(Translator.translate("DisplayParameters.WarmupDisplay"));

        RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
        rbgroup.setRequired(true);
        rbgroup.setLabel(null);
        rbgroup.setHelperText(Translator.translate("DisplayParameters.SwitchableHelper"));
        rbgroup.setItems(Boolean.TRUE, Boolean.FALSE);
        rbgroup.setValue(switchable);
        rbgroup.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? publicDisplay : warmupDisplay));
        rbgroup.addValueChangeListener(e -> {
            Boolean silenced = e.getValue();
            dp.switchSwitchable(target, silenced, true);
        });

        layout.add(label);
        layout.add(rbgroup);
    }

    public static void addSizingEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.FontSizeLabel"));
        
        LocalizedDecimalField fontSizeField = new LocalizedDecimalField();
        TextField wrappedTextField = fontSizeField.getWrappedTextField();
        wrappedTextField.setLabel(null);
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
            // UI.getCurrent().getPage().reload();
        });
        
        layout.add(label);
        layout.add(fontSizeField);
    }

    public static void addSectionEntries(VerticalLayout layout, Component target, DisplayParameters dp) {
        NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.Content"));
        
        boolean showRecords = dp.isRecordsDisplay();
        Checkbox recordsDisplayCheckbox = new Checkbox(Translator.translate("DisplayParameters.ShowRecords"));//
        recordsDisplayCheckbox.setValue(showRecords);
        recordsDisplayCheckbox.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                dp.switchRecords(target, e.getValue(), true);
            }
            // UI.getCurrent().getPage().reload();
        });

        boolean showLeaders = dp.isShowLeaders();
        Checkbox leadersDisplayCheckbox = new Checkbox(Translator.translate("DisplayParameters.ShowLeaders"));//
        leadersDisplayCheckbox.setValue(showLeaders);
        leadersDisplayCheckbox.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getSource() == leadersDisplayCheckbox) {
                dp.switchLeaders(target, e.getValue(), true);
            }
            //UI.getCurrent().getPage().reload();
        });
        
        boolean liftingOrder = dp.isLiftingOrder();
        Checkbox liftingOrderCheckbox = new Checkbox(Translator.translate("DisplayParameters.LiftingOrder"));//
        liftingOrderCheckbox.setValue(liftingOrder);
        liftingOrderCheckbox.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getSource() == liftingOrderCheckbox) {
                dp.switchLiftingOrder(target, e.getValue(), true);
            }
            UI.getCurrent().getPage().open(dp.getLocation().getPathWithQueryParameters());
        });    

        HorizontalLayout horizontalLayout = new HorizontalLayout(liftingOrderCheckbox, recordsDisplayCheckbox, leadersDisplayCheckbox);
        layout.add(label);
        layout.add(horizontalLayout);
        
    }

    
    public static void addRule(VerticalLayout vl) {
        Hr hr = new Hr();
        hr.getStyle().set("border-top", "1px solid");
        hr.getStyle().set("margin-top", "1em");
        vl.add(hr);
    }

}
