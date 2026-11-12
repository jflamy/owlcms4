package app.owlcms.nui.admin;

import java.util.concurrent.atomic.AtomicReference;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import app.owlcms.Main;
import app.owlcms.access.AccessMode;
import app.owlcms.access.AccessUi;
import app.owlcms.access.AccountModeAuthenticator;
import app.owlcms.access.Permission;
import app.owlcms.access.ImportGate;
import app.owlcms.access.Principal;
import app.owlcms.access.Access;
import app.owlcms.access.RequiresAccess;
import app.owlcms.access.SameMachineProof;
import app.owlcms.access.SecuritySettings;
import app.owlcms.access.SecuritySettings.Outcome;
import app.owlcms.access.SessionLogout;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuditSnapshot;
import app.owlcms.audit.AuditSnapshot.Scope;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.components.elements.LazyDownloadButton;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.preparation.AccountsContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.IwfComplianceDisplay;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.RestartUtils;

/**
 * Administration: system (stop, restart, backup, imports allowed, simulation), security settings, audit trail. The
 * Competition Supervisor sees only the System tab.
 */
@SuppressWarnings("serial")
@RequiresAccess({ Access.ADMINISTRATOR, Access.SUPERVISOR })
@Route(value = AdminView.ROUTE, layout = OwlcmsLayout.class)
public class AdminView extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	public static final String ROUTE = "admin";
	// Zero-based tab indices. The URL uses these values directly, so "/0" is equivalent to "/".
	public static final int SYSTEM_TAB = 0;
	public static final int SECURITY_TAB = 1;

	private final TabSheet tabs;

	/** Shown to the sessions that can open the page. */
	public static boolean isMenuEntryAvailable() {
		return AccessUi.canOpen(AdminView.class, null);
	}

	/**
	 * Two tabs: System (with testing), Security and Audit. Without a control panel token (a cloud app without the
	 * secret), the actions that need it are disabled and say why; the others work.
	 */
	public AdminView() {
		boolean token = SameMachineProof.configuredToken() != null;
		Button stop = new Button(Translator.translate("Admin.Stop"), VaadinIcon.STOP.create(),
		        event -> confirmStop());
		Button restart = new Button(Translator.translate("Admin.Restart"), VaadinIcon.REFRESH.create(),
		        event -> confirmRestart());
		Button backup = openInNewTabNoParam(BackupView.class, Translator.translate("Backup.Title"),
		        VaadinIcon.DOWNLOAD.create());
		Button simulation = openInNewTabNoParam(SimulationView.class, Translator.translate("Simulation.Title"));

		Button auditKey = new Button(Translator.translate("AuditIntegrity.PrepareTitle"), VaadinIcon.KEY.create(),
		        event -> AuditKeyDialog.open(UI.getCurrent(),
		                AuditActor.capture(this).atStation("ADMIN", null, false)));
		Button auditCheck = openInNewTabNoParam(AuditIntegrityCheckView.class,
		        Translator.translate("AuditIntegrity.CheckTitle"), VaadinIcon.CHECK_SQUARE_O.create());
		auditKey.setEnabled(token);
		auditCheck.setEnabled(token);

		Button captureAnomaly = new Button(Translator.translate("Admin.CaptureAnomaly"), VaadinIcon.WARNING.create(),
		        event -> openAnomalyDialog());
		captureAnomaly.addThemeVariants(ButtonVariant.LUMO_ERROR);

		VerticalLayout systemTab = tab(heading("Admin.Server"),
		        HomeNavigationContent.navigationGrid(stop, restart, backup, captureAnomaly),
		        heading("Admin.Imports"), importsAllowedSwitch(),
		        heading("Admin.Testing"), HomeNavigationContent.navigationGrid(simulation));

		TabSheet tabs = new TabSheet();
		this.tabs = tabs;
		tabs.add(Translator.translate("Admin.System"), systemTab);
		if (AccessUi.hasPermission(Permission.SECURITY_SETTINGS, null)) {
			AccessControlForm accessControlForm = new AccessControlForm();
			Button accounts = openInNewTabNoParam(AccountsContent.class,
			        Translator.translate("Access.Accounts.Title"), VaadinIcon.KEY.create());
			VerticalLayout securityTab = tab(heading("Config.AccessControlTitle"), securityStatus(),
			        securityControls(accounts, token),
			        accessControlForm);
			FlexibleGridLayout auditTrail = AccessUi.hasPermission(Permission.AUDIT_KEYS, null)
			        ? HomeNavigationContent.navigationGrid(auditKey, auditCheck,
			                iwfComplianceCheck(accessControlForm::refreshIfClean))
			        : HomeNavigationContent.navigationGrid(iwfComplianceCheck(accessControlForm::refreshIfClean));
			securityTab.add(heading("Admin.AuditTrail"), auditTrail);
			if (!token) {
				securityTab.add(helperText("Access.ControlPanelTokenNotSet"));
			}
			tabs.add(Translator.translate("Admin.SecurityAndAudit"), securityTab);
		}
		tabs.setWidthFull();
		// keep the selection in the URL, so that reloading or a link opens the same tab
		tabs.addSelectedChangeListener(event -> UI.getCurrent().getPage().getHistory()
		        .replaceState(null, new Location(ROUTE + "/" + tabs.getSelectedIndex())));
		add(tabs);
	}

	/** Opens directly on a tab when the URL ends with its index, as in {@code admin/1}. */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		super.setParameter(event, parameter);
		if (parameter == null || parameter.isBlank()) {
			return;
		}
		try {
			int index = Integer.parseInt(parameter);
			if (index >= 0 && index < this.tabs.getTabCount()) {
				this.tabs.setSelectedIndex(index);
			}
		} catch (NumberFormatException e) {
			// ignore invalid tab parameter
		}
	}

	private static VerticalLayout tab(Component... content) {
		VerticalLayout layout = new VerticalLayout(content);
		layout.setPadding(false);
		return layout;
	}

	private static H4 heading(String key) {
		H4 heading = new H4(Translator.translate(key));
		heading.getStyle().set("margin", "var(--lumo-space-m) 0 0 0");
		return heading;
	}

	/** The access mode in force. */
	private static VerticalLayout securityStatus() {
		Config config = Config.getCurrent();
		VerticalLayout status = new VerticalLayout();
		status.setSpacing(false);
		status.add(new Paragraph(Translator.translate("Access.Mode") + ": "
		        + Translator.translate("Access.Mode." + config.getParamAccessMode().name())));
		status.add(helperText(config.getParamAccessMode() == AccessMode.ACCOUNTS
		        ? "Admin.AccountsModeHelp"
		        : "Admin.PinModeHelp"));
		return status;
	}

	/**
	 * @param accounts opens the user accounts page, shown first
	 * @param token    false when OWLCMS has no control panel token: the access mode cannot be switched
	 */
	private FlexibleGridLayout securityControls(Button accounts, boolean token) {
		Config config = Config.getCurrent();
		AccessMode target = config.getParamAccessMode() == AccessMode.ACCOUNTS ? AccessMode.PIN : AccessMode.ACCOUNTS;
		Button accessMode = new Button(Translator.translate(accessModeLabel(target)), VaadinIcon.USERS.create(),
		        event -> confirmAccessMode(target));
		accessMode.setEnabled(token);
		return HomeNavigationContent.navigationGrid(accounts,
		        securityAction(accessMode, token ? accessModeLabel(target) + "Help" : "Access.ControlPanelTokenNotSet"));
	}

	/**
	 * Downloads the database export and the logs, with the operator's description recorded in the audit trail before
	 * the logs are sealed, so that the zip holds it.
	 */
	private void openAnomalyDialog() {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(Translator.translate("Admin.CaptureAnomaly"));
		dialog.setWidth("40em");
		TextArea description = new TextArea(Translator.translate("Admin.AnomalyDescription"));
		description.setWidthFull();
		description.setMaxLength(500);

		// the zip is built on a request thread with no current UI; capture the actor and the text on click
		AtomicReference<AuditActor> actor = new AtomicReference<>();
		AtomicReference<String> text = new AtomicReference<>("");
		Div download = DownloadButtonFactory.createDynamicZipDownloadButton("owlcms_anomaly",
		        Translator.translate("Admin.AnomalyDownload"),
		        () -> AuditSnapshot.stream(Scope.LOGS_AND_EXPORT, actor.get(), text.get()));
		download.getChildren().filter(LazyDownloadButton.class::isInstance).findFirst()
		        .ifPresent(button -> ((LazyDownloadButton) button).addClickListener(event -> {
			        actor.set(AuditActor.capture(this).atStation("ADMIN", null, false));
			        text.set(description.getValue());
		        }));

		dialog.add(new Paragraph(Translator.translate("Admin.AnomalyExplanation")), description, download);
		dialog.getFooter().add(new Button(Translator.translate("Close"), event -> dialog.close()));
		dialog.open();
	}

	/** Registration, SBDE and database imports on the Preparation page. */
	private static Checkbox importsAllowedSwitch() {
		Checkbox allowed = new Checkbox(Translator.translate("Admin.ImportsAllowed"), ImportGate.isAllowed());
		allowed.setHelperText(Translator.translate("Admin.ImportsAllowedHelp"));
		allowed.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				ImportGate.setAllowed(event.getValue(), AuditOrigin.fromUi("Administration imports allowed"));
			}
		});
		return allowed;
	}

	/** The checkbox, and the list of requirements while it is checked. */
	private static VerticalLayout iwfComplianceCheck(Runnable settingsSaved) {
		VerticalLayout block = new VerticalLayout();
		block.setPadding(false);
		block.setSpacing(false);
		Div checklist = new Div();
		Checkbox check = new Checkbox(Translator.translate("Admin.IwfCheck"),
		        Config.getCurrent().isIwfComplianceChecked());
		check.setHelperText(Translator.translate("Admin.IwfCheckHelp"));
		check.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}
			// captured at the click, on the UI thread: while the page is being built it is not yet the active
			// route, and the actor would resolve to SYSTEM
			AuditOrigin origin = AuditOrigin.fromUi("Administration security settings");
			Outcome outcome = SecuritySettings.setIwfComplianceCheck(event.getValue(), origin);
			if (outcome == Outcome.CHANGED) {
				settingsSaved.run();
			} else if (outcome != Outcome.UNCHANGED) {
				showOutcome(outcome);
			}
			showChecklist(checklist);
		});
		showChecklist(checklist);
		block.add(check, checklist);
		return block;
	}

	private static void showChecklist(Div checklist) {
		checklist.removeAll();
		if (Config.getCurrent().isIwfComplianceChecked()) {
			checklist.add(IwfComplianceDisplay.checklist());
		}
	}

	private static String accessModeLabel(AccessMode target) {
		return target == AccessMode.ACCOUNTS ? "Admin.SwitchToAccounts" : "Admin.SwitchToPin";
	}

	private static VerticalLayout securityAction(Button button, String helperKey) {
		button.setWidthFull();
		VerticalLayout action = new VerticalLayout(button, helperText(helperKey));
		action.setPadding(false);
		action.setSpacing(false);
		return action;
	}

	private static Paragraph helperText(String key) {
		Paragraph helper = new Paragraph(Translator.translate(key));
		helper.getStyle()
		        .set("color", "var(--lumo-secondary-text-color)")
		        .set("font-size", "var(--lumo-font-size-s)")
		        .set("margin", "var(--lumo-space-xs) 0 0");
		return helper;
	}

	/** Changing the access mode needs the cp challenge. */
	private static void confirmSecurityChange(String labelKey, String explanationKey, Runnable change) {
		ControlPanelTokenField token = new ControlPanelTokenField();
		String label = Translator.translate(labelKey);
		ConfirmationDialog dialog = new ConfirmationDialog(label, null, label, null, null, change);
		Markdown explanation = new Markdown(Translator.translate(explanationKey));
		explanation.setWidth("550px");
		dialog.addBody(explanation);
		dialog.addBody(token);
		dialog.setConfirmCheck(() -> token.passes("security settings: " + label));
		dialog.open();
	}

	private void confirmAccessMode(AccessMode target) {
		UI ui = UI.getCurrent();
		AuditOrigin origin = AuditOrigin.fromUi("Administration security settings");
		confirmSecurityChange(accessModeLabel(target), accessModeLabel(target) + "Confirm", () -> {
			Outcome outcome = SecuritySettings.switchAccessMode(target, origin);
			if (outcome != Outcome.CHANGED) {
				showOutcome(outcome);
				return;
			}
			// the new mode applies to every session, including this one
			Principal principal = OwlcmsSession.getPrincipal();
			if (principal != null && principal.source() == Principal.AuthSource.ACCOUNT) {
				AccountModeAuthenticator.logout(principal, "access mode changed");
			}
			OwlcmsSession.setPrincipal(null);
			OwlcmsSession.setAuthenticated(false);
			OwlcmsSession.setDisplayAuthenticated(false);
			SessionLogout.redirectAllAndInvalidate(ui.getSession());
		});
	}

	private static void showOutcome(Outcome outcome) {
		String key = switch (outcome) {
			case ADMIN_PASSWORD_REQUIRED -> "Access.Error.AdminPasswordRequired";
			case CHANGED, UNCHANGED -> null;
		};
		if (key != null) {
			NotificationUtils.errorNotification(Translator.translate(key));
		}
	}

	private void confirmStop() {
		AuditActor actor = AuditActor.capture(this).atStation("ADMIN", null, false);
		new ConfirmationDialog(Translator.translate("Admin.Stop"), Translator.translate("Admin.StopConfirm"),
		        Translator.translate("Admin.Stop"), null, null, () -> {
			        AuditContext.run(actor, "Admin stop requested", () -> Main.prepareForExit("Admin stop requested"));
			        System.exit(0);
		        }).open();
	}

	private void confirmRestart() {
		AuditActor actor = AuditActor.capture(this).atStation("ADMIN", null, false);
		UI ui = UI.getCurrent();
		new ConfirmationDialog(Translator.translate("Admin.Restart"), Translator.translate("Admin.RestartConfirm"),
		        Translator.translate("Admin.Restart"), null, null, () -> {
			        NotificationUtils.restarting(ui);
			        AuditContext.run(actor, "Admin restart requested",
			                () -> RestartUtils.triggerRestart("Admin restart requested"));
		        }).open();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("Admin.Title");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Admin.Title");
	}

	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		return null;
	}
}
