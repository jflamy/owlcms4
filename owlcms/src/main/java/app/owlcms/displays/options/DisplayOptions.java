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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Location;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.components.fields.LocalizedDecimalField;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

public class DisplayOptions {
	final static Logger logger = (Logger) LoggerFactory.getLogger(DisplayOptions.class);

	public static void addLightingEntries(VerticalLayout layout, Component target, DisplayParametersReader dp) {
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
			dp.switchLightingMode(e.getValue(), true);
		});
		rbgroup.getStyle().set("margin-top", "0px");

		layout.add(label);
		layout.add(rbgroup);
	}

	public static void addRule(VerticalLayout vl) {
		Hr hr = new Hr();
		hr.getStyle().set("border-top", "1px solid");
		hr.getStyle().set("margin-top", "1em");
		vl.add(hr);
	}

	public static void addSectionEntries(VerticalLayout layout, Component target, DisplayParametersReader dp) {
		NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.Content"));

		Checkbox recordsDisplayCheckbox = null;
		//if (!RecordRepository.findAll().isEmpty()) {
			boolean showRecords = dp.isRecordsDisplay();
			recordsDisplayCheckbox = new Checkbox(Translator.translate("DisplayParameters.ShowRecords"));//
			recordsDisplayCheckbox.setValue(showRecords);
			recordsDisplayCheckbox.addValueChangeListener(e -> {
				if (e.isFromClient()) {
					dp.switchRecords(e.getValue(), true);
				}
			});
		//}

		boolean showLeaders = dp.isLeadersDisplay();
		Checkbox leadersDisplayCheckbox = new Checkbox(Translator.translate("DisplayParameters.ShowLeaders"));//
		leadersDisplayCheckbox.setValue(showLeaders);
		leadersDisplayCheckbox.addValueChangeListener(e -> {
			if (e.isFromClient() && e.getSource() == leadersDisplayCheckbox) {
				dp.switchLeaders(e.getValue(), true);
			}
		});
		
		boolean abbreviated = dp.isAbbreviatedName();
		Checkbox abbreviatedCheckbox = new Checkbox(Translator.translate("DisplayParameters.Abbreviated"));//
		abbreviatedCheckbox.setValue(abbreviated);
		abbreviatedCheckbox.addValueChangeListener(e -> {
			if (e.isFromClient() && e.getSource() == abbreviatedCheckbox) {
				dp.switchAbbreviated(e.getValue(), true);
			}
			Location location = dp.getLocation();
			UI.getCurrent().getPage().setLocation(location.getPathWithQueryParameters());
		});

		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.add(leadersDisplayCheckbox);
		if (recordsDisplayCheckbox != null) {
			horizontalLayout.add(recordsDisplayCheckbox);
		}
		horizontalLayout.add(abbreviatedCheckbox);

		layout.add(label);
		layout.add(horizontalLayout);

	}

	public static void addSizingEntries(VerticalLayout layout, Component target, DisplayParametersReader dp) {

		LocalizedDecimalField fontSizeField = new LocalizedDecimalField(3);
		TextField wrappedTextField = fontSizeField.getWrappedTextField();

		wrappedTextField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		wrappedTextField.addFocusListener(f -> {
			dp.getDialogTimer().cancel();
			dp.getDialogTimer().purge();
		});
		fontSizeField.setValue(dp.getEmFontSize());
		fontSizeField.addValueChangeListener(e -> {
			dp.getDialogTimer().cancel();
			Double emSize = e.getValue();
			dp.switchEmFontSize(emSize, true);
		});
		
		LocalizedDecimalField twField = new LocalizedDecimalField(3);
		TextField twTextField = fontSizeField.getWrappedTextField();
		twTextField.setLabel(null);
		twTextField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		twTextField.addFocusListener(f -> {
			dp.getDialogTimer().cancel();
			dp.getDialogTimer().purge();
		});
		twField.setValue(dp.getTeamWidth());
		twField.addValueChangeListener(e -> {
			dp.getDialogTimer().cancel();
			Double emSize = e.getValue();
			dp.switchTeamWidth(emSize, true);
		});

		HorizontalLayout fx = new HorizontalLayout();
		fx.setSizeFull();
		HorizontalLayout p1 = new HorizontalLayout(new NativeLabel(Translator.translate("DisplayParameters.FontSizeLabel")), wrappedTextField);
		fx.add(p1);
		p1.setSizeFull();
		HorizontalLayout p2 = new HorizontalLayout(new NativeLabel(Translator.translate("DisplayParameters.TeamSizeLabel")),twField);
		fx.add(p2);
		p2.setSizeFull();
		layout.add(fx);
	}

	public static void addSoundEntries(VerticalLayout layout, Component target, DisplayParametersReader dp) {
		NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.SoundSettings"));
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null) {
			if (fop.isEmitSoundsOnServer()) {
				label = new NativeLabel(Translator.translate("DisplayParameters.SoundsOnServer"));
				label.setWidth("25em");
				layout.add(label);
				return;
			}
		}

		boolean silentMode = dp.isSilenced();
		Button silentButton = new Button(Translator.translate("DisplayParameters.ClockSoundOff"), new Icon(VaadinIcon.BELL_SLASH));
		Button soundButton = new Button(Translator.translate("DisplayParameters.ClockSoundOn"), new Icon(VaadinIcon.BELL));

		RadioButtonGroup<Boolean> rbgroup = new RadioButtonGroup<>();
		rbgroup.setRequired(true);
		rbgroup.setLabel(null);
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
		
		boolean downSilentMode = dp.isDownSilenced();
		Button downSilencedButton = new Button(Translator.translate("DisplayParameters.DownSoundOff"), new Icon(VaadinIcon.BELL_SLASH));
		Button downSoundButton = new Button(Translator.translate("DisplayParameters.DownSoundOn"), new Icon(VaadinIcon.BELL));

		RadioButtonGroup<Boolean> rb2group = new RadioButtonGroup<>();
		rb2group.setRequired(true);
		rb2group.setLabel(null);
		rb2group.setItems(Boolean.TRUE, Boolean.FALSE);
		rb2group.setValue(downSilentMode);
		rb2group.setRenderer(new ComponentRenderer<Button, Boolean>((mn) -> mn ? downSilencedButton : downSoundButton));
		rb2group.addValueChangeListener(e -> {
			Boolean downSilenced = e.getValue();
			dp.switchDownMode(downSilenced, true);
			if (!downSilenced) {
				SoundUtils.doEnableAudioContext(target.getElement());
			}
		});
		rb2group.setHelperText(Translator.translate("DisplayParameters.SoundHelper"));

		layout.add(label);
		layout.add(rbgroup, rb2group);
	}

	public static void addSwitchableEntries(VerticalLayout layout, Component target, DisplayParametersReader dp) {
		NativeLabel label = new NativeLabel(Translator.translate("DisplayParameters.SwitchableSettings"));

		boolean switchable = dp.isPublicDisplay();
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
			dp.switchSwitchable(silenced, true);
		});

		layout.add(label);
		layout.add(rbgroup);
	}

}
