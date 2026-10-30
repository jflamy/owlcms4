package app.owlcms.nui.admin;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.Main;
import app.owlcms.access.AccessMode;
import app.owlcms.access.AccessUi;
import app.owlcms.access.AccountModeAuthenticator;
import app.owlcms.access.Principal;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.access.SecuritySettings;
import app.owlcms.access.SecuritySettings.Outcome;
import app.owlcms.access.SessionLogout;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.IwfComplianceDisplay;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.RestartUtils;

/** Administration: stopping and restarting OWLCMS, backup, security settings, audit trail and simulation. */
@SuppressWarnings("serial")
@RequiresRole(Role.ADMIN_PAGES)
@Route(value = "admin", layout = OwlcmsLayout.class)
public class AdminView extends BaseNavigationContent implements NavigationPage, HasDynamicTitle, BeforeEnterObserver {

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		checkAdminAccess();
	}

	/**
	 * Admin and backup pages. In accounts mode the ADMIN_PAGES login is enough, so cloud installations can use them; in
	 * PIN mode they need localhost or the backdoor list.
	 */
	public static void checkAdminAccess() {
		if (Config.getCurrent().isAccountsMode()) {
			return;
		}
		if (!hasLocalAccess()) {
			throw new AccessDeniedException();
		}
	}

	/** PIN mode only, until the admin pages ask for the admin password: localhost or the backdoor list. */
	private static boolean hasLocalAccess() {
		Principal principal = OwlcmsSession.getPrincipal();
		boolean accountSession = principal != null && principal.source() == Principal.AuthSource.ACCOUNT;
		if (!OwlcmsSession.isAuthenticated() && !accountSession) {
			return true;
		}
		String clientIp = AccessUtils.getClientIp();
		boolean backdoor = !Config.getCurrent().isAccountsMode() && AccessUtils.checkBackdoor(clientIp);
		return AccessUtils.isLocalhost(clientIp) || backdoor;
	}

	/** Shown to the sessions that can open the page: the route roles, and in PIN mode localhost or the backdoor. */
	public static boolean isMenuEntryAvailable() {
		return AccessUi.canOpen(AdminView.class, null)
		        && (Config.getCurrent().isAccountsMode() || hasLocalAccess());
	}

	public AdminView() {
		Button stop = new Button(Translator.translate("Admin.Stop"), VaadinIcon.STOP.create(),
		        event -> confirmStop());
		Button restart = new Button(Translator.translate("Admin.Restart"), VaadinIcon.REFRESH.create(),
		        event -> confirmRestart());
		Button backup = openInNewTabNoParam(BackupView.class, Translator.translate("Backup.Title"),
		        VaadinIcon.DOWNLOAD.create());

		Button auditKey = new Button(Translator.translate("AuditIntegrity.PrepareTitle"), VaadinIcon.KEY.create(),
		        event -> AuditKeyDialog.open(UI.getCurrent(),
		                AuditActor.capture(this).atStation("ADMIN", null, false)));
		Button auditCheck = openInNewTabNoParam(AuditIntegrityCheckView.class,
		        Translator.translate("AuditIntegrity.CheckTitle"), VaadinIcon.CHECK_SQUARE_O.create());

		Button simulation = openInNewTabNoParam(SimulationView.class, Translator.translate("Simulation.Title"));

		FlexibleGridLayout system = HomeNavigationContent.navigationGrid(stop, restart, backup);
		FlexibleGridLayout audit = HomeNavigationContent.navigationGrid(auditKey, auditCheck);
		FlexibleGridLayout testing = HomeNavigationContent.navigationGrid(simulation);

		doGroup(Translator.translate("Admin.System"), system, this);
		doGroup(Translator.translate("Admin.Security"), securityStatus(), securityControls(), this);
		doGroup(Translator.translate("Admin.AuditTrail"), audit, this);
		doGroup(Translator.translate("Admin.Testing"), testing, this);
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

	private FlexibleGridLayout securityControls() {
		Config config = Config.getCurrent();
		AccessMode target = config.getParamAccessMode() == AccessMode.ACCOUNTS ? AccessMode.PIN : AccessMode.ACCOUNTS;
		Button accessMode = new Button(Translator.translate(accessModeLabel(target)), VaadinIcon.USERS.create(),
		        event -> confirmAccessMode(target));
		return HomeNavigationContent.navigationGrid(securityAction(accessMode, accessModeLabel(target) + "Help"),
		        iwfComplianceCheck());
	}

	/** The checkbox, and the list of requirements while it is checked. */
	private static VerticalLayout iwfComplianceCheck() {
		VerticalLayout block = new VerticalLayout();
		block.setPadding(false);
		block.setSpacing(false);
		Div checklist = new Div();
		Checkbox check = new Checkbox(Translator.translate("Admin.IwfCheck"),
		        Config.getCurrent().isIwfComplianceChecked());
		check.setHelperText(Translator.translate(Config.isIwfComplianceSetAtStartup()
		        ? "Admin.SetAtStartup"
		        : "Admin.IwfCheckHelp"));
		check.setReadOnly(Config.isIwfComplianceSetAtStartup());
		AuditOrigin origin = AuditOrigin.fromUi("Administration security settings");
		check.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}
			Outcome outcome = SecuritySettings.setIwfComplianceCheck(event.getValue(), origin);
			if (outcome != Outcome.CHANGED && outcome != Outcome.UNCHANGED) {
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
			case SET_AT_STARTUP -> "Admin.SetAtStartup";
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
