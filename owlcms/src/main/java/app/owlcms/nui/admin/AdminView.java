package app.owlcms.nui.admin;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.Main;
import app.owlcms.access.AccessPolicy;
import app.owlcms.access.Principal;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.websocket.WebSocketEventSender;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.RestartUtils;

/** Administration: stopping and restarting OWLCMS, translations, backup, audit trail and simulation. */
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
		checkLocalAccess();
	}

	/** PIN mode only, until the admin pages ask for the admin password: localhost or the backdoor list. */
	private static void checkLocalAccess() {
		Principal principal = OwlcmsSession.getPrincipal();
		boolean accountSession = principal != null && principal.source() == Principal.AuthSource.ACCOUNT;
		if (!OwlcmsSession.isAuthenticated() && !accountSession) {
			return;
		}
		String clientIp = AccessUtils.getClientIp();
		boolean backdoor = !Config.getCurrent().isAccountsMode() && AccessUtils.checkBackdoor(clientIp);
		if (!AccessUtils.isLocalhost(clientIp) && !backdoor) {
			throw new AccessDeniedException();
		}
	}

	/**
	 * The side-menu entry is offered only to signed-in administrator accounts, which pass
	 * {@link #checkAdminAccess()}; PIN-mode sessions reach the page by its URL.
	 */
	public static boolean isMenuEntryAvailable() {
		Principal principal = OwlcmsSession.getPrincipal();
		return principal != null && principal.source() == Principal.AuthSource.ACCOUNT
		        && AccessPolicy.isAdmin(principal);
	}

	public AdminView() {
		Button stop = new Button(Translator.translate("Admin.Stop"), VaadinIcon.STOP.create(),
		        event -> confirmStop());
		Button restart = new Button(Translator.translate("Admin.Restart"), VaadinIcon.REFRESH.create(),
		        event -> confirmRestart());
		Button backup = openInNewTabNoParam(BackupView.class, Translator.translate("Backup.Title"),
		        VaadinIcon.DOWNLOAD.create());

		Button reloadTranslations = new Button(Translator.translate("reloadTranslation"), event -> {
			Translator.resetFromLocal();
			WebSocketEventSender.sendTranslationsToAll();
		});
		String sheetsKey = System.getenv("GOOGLE_SHEETS_API_KEY");
		Button reloadFromGoogleSheets = new Button(Translator.translate("reloadTranslationFromGoogleSheets"),
		        event -> {
			        Translator.resetFromGoogleSheets();
			        WebSocketEventSender.sendTranslationsToAll();
		        });
		reloadFromGoogleSheets.setEnabled(sheetsKey != null && !sheetsKey.isBlank());

		Button auditKey = new Button(Translator.translate("AuditIntegrity.PrepareTitle"), VaadinIcon.KEY.create(),
		        event -> AuditKeyDialog.open(UI.getCurrent(),
		                AuditActor.capture(this).atStation("ADMIN", null, false)));
		Button auditCheck = openInNewTabNoParam(AuditIntegrityCheckView.class,
		        Translator.translate("AuditIntegrity.CheckTitle"), VaadinIcon.CHECK_SQUARE_O.create());

		Button simulation = openInNewTabNoParam(SimulationView.class, Translator.translate("Simulation.Title"));

		FlexibleGridLayout system = HomeNavigationContent.navigationGrid(stop, restart, backup);
		FlexibleGridLayout translations = HomeNavigationContent.navigationGrid(reloadTranslations,
		        reloadFromGoogleSheets);
		FlexibleGridLayout audit = HomeNavigationContent.navigationGrid(auditKey, auditCheck);
		FlexibleGridLayout testing = HomeNavigationContent.navigationGrid(simulation);

		doGroup(Translator.translate("Admin.System"), system, this);
		VerticalLayout translationsIntro = new VerticalLayout(new Paragraph(Translator.translate("reloadTranslationInfo")));
		doGroup(Translator.translate("Admin.Translations"), translationsIntro, translations, this);
		doGroup(Translator.translate("Admin.AuditTrail"), audit, this);
		doGroup(Translator.translate("Admin.Testing"), testing, this);
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
