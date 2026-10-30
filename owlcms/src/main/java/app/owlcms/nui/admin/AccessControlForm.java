package app.owlcms.nui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.components.fields.OwlcmsPasswordField;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;

/**
 * The PIN-mode access settings, in the Security tab of the Administration page: the officials and display PINs and
 * address lists. In accounts mode they are kept but not used, and cannot be edited.
 */
@SuppressWarnings("serial")
class AccessControlForm extends VerticalLayout {

	private final Binder<Config> binder = new Binder<>(Config.class);

	AccessControlForm() {
		setPadding(false);
		boolean accounts = Config.getCurrent().isAccountsMode();

		Paragraph unused = new Paragraph(Translator.translate("Access.Mode.PinIgnored"));
		unused.getStyle().set("color", "var(--lumo-secondary-text-color)")
		        .set("margin", "var(--lumo-space-s) 0 var(--lumo-space-m) 0");
		unused.setVisible(accounts);

		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
		        new ResponsiveStep("800px", 2, LabelsPosition.TOP));

		PasswordField pin = new OwlcmsPasswordField();
		pin.setWidthFull();
		layout.addFormItem(pin, Translator.translate("Config.PasswordOrPIN"));
		this.binder.forField(pin).withNullRepresentation("").bind(Config::getPinForField, Config::setPinForField);

		TextField accessList = new TextField();
		accessList.setWidthFull();
		layout.addFormItem(accessList, Translator.translate("Config.AccessList"));
		this.binder.forField(accessList).withNullRepresentation("")
		        .bind(Config::getIpAccessList, Config::setIpAccessList);

		PasswordField displayPin = new OwlcmsPasswordField();
		displayPin.setWidthFull();
		layout.addFormItem(displayPin, Translator.translate("Config.DisplayPIN"));
		this.binder.forField(displayPin).withNullRepresentation("")
		        .bind(Config::getDisplayPinForField, Config::setDisplayPinForField);

		TextField displayList = new TextField();
		displayList.setWidthFull();
		layout.addFormItem(displayList, Translator.translate("Config.DisplayAccessList"));
		this.binder.forField(displayList).withNullRepresentation("")
		        .bind(Config::getIpDisplayList, Config::setIpDisplayList);

		Button save = new Button(Translator.translate("Save"), event -> save());
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		// the stored values are kept, so that switching back to PIN mode finds them again
		layout.setEnabled(!accounts);
		save.setEnabled(!accounts);

		this.binder.readBean(Config.getCurrent());
		add(unused, layout, save);
	}

	private void save() {
		// the settings are edited in place, as in System Settings
		Config config = Config.getCurrent();
		if (this.binder.writeBeanIfValid(config)) {
			Config.setCurrent(config, AuditOrigin.fromUi("Administration access control"));
			this.binder.readBean(Config.getCurrent());
			NotificationUtils.saved();
		}
	}
}
