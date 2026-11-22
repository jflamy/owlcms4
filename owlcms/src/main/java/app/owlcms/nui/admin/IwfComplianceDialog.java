package app.owlcms.nui.admin;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;

import app.owlcms.access.AccessMode;
import app.owlcms.access.IwfComplianceCheck.Item;
import app.owlcms.access.SameMachineProof;
import app.owlcms.access.SecuritySettings;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditIntegrity;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.IwfComplianceDisplay;

final class IwfComplianceDialog {

	private final Dialog dialog = new Dialog();
	private final Div content = new Div();
	private final AuditOrigin origin;
	private final Consumer<AccessMode> switchAccessMode;

	private IwfComplianceDialog(AuditOrigin origin, Consumer<AccessMode> switchAccessMode) {
		this.origin = origin;
		this.switchAccessMode = switchAccessMode;
		this.dialog.setHeaderTitle(Translator.translate("Admin.IwfCheck"));
		this.dialog.setWidth("850px");
		this.dialog.setMaxWidth("calc(100vw - 2rem)");
		this.dialog.add(this.content);
		this.dialog.getFooter().add(new Button(Translator.translate("Close"), event -> this.dialog.close()));
		refresh();
	}

	static void open(AuditOrigin origin, Consumer<AccessMode> switchAccessMode) {
		if (!AuditIntegrity.isInitialized() || AuditIntegrity.current().signingKey().isBuiltin()) {
			NotificationUtils.errorNotification(Translator.translate("IwfCheck.NotMet") + " "
			        + Translator.translate("AuditIntegrity.Builtin"));
			return;
		}
		new IwfComplianceDialog(origin, switchAccessMode).openDialog();
	}

	private void openDialog() {
		this.dialog.open();
	}

	private void refresh() {
		this.content.removeAll();
		Checkbox report = new Checkbox(null, Config.getCurrent().isIwfComplianceChecked());
		report.setAriaLabel(Translator.translate("Admin.IwfCheck"));
		report.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				SecuritySettings.setIwfComplianceCheck(event.getValue(), this.origin);
				refresh();
			}
		});
		this.content.add(IwfComplianceDisplay.checklistWithActions(Translator.translate("Admin.IwfCheck"), report,
		        this::action));
	}

	private Component action(Item item) {
		return switch (item.requirement()) {
			case USER_ACCOUNTS -> accountModeAction(item);
			case AUDIT_KEY, CONTROL_PANEL_TOKEN -> null;
			case TCRR_RULES -> featureSwitchAction(FeatureSwitch.TCRR_COMPLIANCE,
			        FeatureSwitch.TCRR_COMPLIANCE.getTranslationKey());
			case SIGNED_EXPORTS -> featureSwitchAction(FeatureSwitch.NO_V1_EXPORT,
			        FeatureSwitch.NO_V1_EXPORT.getTranslationKey());
			case NO_REMEMBERED_LOGINS -> rememberedLoginAction();
		};
	}

	private Component accountModeAction(Item item) {
		if (item.isMet()) {
			return null;
		}
		Button action = new Button(Translator.translate("Admin.SwitchToAccounts"), event -> {
			this.dialog.close();
			this.switchAccessMode.accept(AccessMode.ACCOUNTS);
		});
		action.setWidthFull();
		boolean token = SameMachineProof.configuredToken() != null;
		action.setEnabled(token);
		if (!token) {
			action.setTooltipText(Translator.translate("Access.ControlPanelTokenNotSet"));
		}
		return action;
	}

	private Component featureSwitchAction(FeatureSwitch featureSwitch, String labelKey) {
		Checkbox action = new Checkbox(Translator.translate(labelKey), featureSwitch.isEffective());
		action.setWidthFull();
		action.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				SecuritySettings.setIwfFeatureSwitch(featureSwitch, event.getValue(), this.origin);
				refresh();
			}
		});
		return action;
	}

	private Component rememberedLoginAction() {
		boolean disabled = !Config.getCurrent().allowsRememberedLogins();
		Checkbox action = new Checkbox(Translator.translate("IwfCheck.NoRememberedLogins"), disabled);
		action.setWidthFull();
		action.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				SecuritySettings.setRememberAccountLogins(!event.getValue(), this.origin);
				refresh();
			}
		});
		return action;
	}
}
