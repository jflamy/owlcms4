package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.stream.Collectors;

import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.access.AccessMode;
import app.owlcms.access.AccessStartup;
import app.owlcms.access.AccountModeAuthenticator;
import app.owlcms.access.Principal;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.access.SessionLogout;
import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Logger;

/** Named accounts and their role grants; usable in both access modes so accounts can be prepared beforehand. */
@SuppressWarnings("serial")
@RequiresRole(Role.ADMIN_PAGES)
@Route(value = "preparation/accounts", layout = OwlcmsLayout.class)
public class AccountsContent extends BaseContent implements CrudListener<UserAccount>, OwlcmsContent {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountsContent.class);
	private OwlcmsCrudFormFactory<UserAccount> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private GridCrud<UserAccount> crud;

	public AccountsContent() {
		this.editingFormFactory = new AccountEditingFormFactory(UserAccount.class);
		this.crud = createGrid(this.editingFormFactory);
		fillHW(this.crud, this);
	}

	private Checkbox createAccessModeSwitch() {
		Checkbox accountsMode = new Checkbox(Translator.translate("Access.Mode.ACCOUNTS"));
		accountsMode.setValue(Config.getCurrent().getAccessMode() == AccessMode.ACCOUNTS);
		accountsMode.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}
			if (event.getValue() && !UserAccountRepository.adminHasPassword()) {
				accountsMode.setValue(false);
				Notification.show(Translator.translate("Access.Error.AdminPasswordRequired"));
				return;
			}
			Config config = Config.getCurrent();
			config.setAccessMode(event.getValue() ? AccessMode.ACCOUNTS : AccessMode.PIN);
			Config.setCurrent(config);
			Principal principal = OwlcmsSession.getPrincipal();
			if (principal != null && principal.source() == Principal.AuthSource.ACCOUNT) {
				AccountModeAuthenticator.logout(principal, "access mode changed");
			}
			SessionLogout.redirectAllAndInvalidate(UI.getCurrent().getSession());
		});
		return accountsMode;
	}

	@Override
	public UserAccount add(UserAccount account) {
		return this.editingFormFactory.add(account);
	}

	@Override
	public UserAccount update(UserAccount account) {
		return this.editingFormFactory.update(account);
	}

	@Override
	public void delete(UserAccount account) {
		this.editingFormFactory.delete(account);
	}

	@Override
	public Collection<UserAccount> findAll() {
		return UserAccountRepository.findAll();
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("Access.Accounts.Title");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Access.Accounts.Title");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	private GridCrud<UserAccount> createGrid(OwlcmsCrudFormFactory<UserAccount> crudFormFactory) {
		Grid<UserAccount> grid = new Grid<>(UserAccount.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addComponentColumn(this::enabledToggle).setHeader(Translator.translate("Active")).setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(UserAccount::getUsername).setHeader(Translator.translate("Access.Username"));
		grid.addColumn(UserAccount::getDisplayName).setHeader(Translator.translate("Access.Account.DisplayName"));
		grid.addComponentColumn(this::passwordRequiredIndicator)
		        .setHeader(Translator.translate("Access.Account.PasswordRequired")).setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(a -> summary(a)).setHeader(Translator.translate("Access.Account.Grants"));

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(UserAccount.class);
		GridCrud<UserAccount> gridCrud = new OwlcmsCrudGrid<>(UserAccount.class, gridLayout, crudFormFactory, grid);
		gridCrud.setCrudListener(this);
		gridCrud.setClickRowToUpdate(true);
		gridCrud.getCrudLayout().addToolbarComponent(createAccessModeSwitch());
		gridCrud.getCrudLayout().addToolbarComponent(createResetPlatformAccountsButton(grid));
		((HorizontalLayout) gridLayout.getToolbarLayout())
		        .setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		return gridCrud;
	}

	private Button createResetPlatformAccountsButton(Grid<UserAccount> grid) {
		String label = Translator.translate("Access.Account.ResetPlatforms");
		Button reset = new Button(label, event -> new ConfirmationDialog(
		        label, Translator.translate("Access.Account.ResetPlatformsConfirm"),
		        label, null, () -> resetPlatformAccounts(grid)).open());
		reset.addThemeVariants(ButtonVariant.LUMO_ERROR);
		return reset;
	}

	private void resetPlatformAccounts(Grid<UserAccount> grid) {
		UI ui = UI.getCurrent();
		String success = Translator.translate("Access.Account.ResetPlatformsDone");
		String failureMessage = Translator.translate("Access.Account.ResetPlatformsFailed");
		this.crud.getElement().setEnabled(false);
		AccessStartup.resetPlatformAccountsInBackground().whenComplete((accounts, failure) -> ui.accessLater(() -> {
			this.crud.getElement().setEnabled(true);
			if (failure != null) {
				Notification.show(failureMessage, 5000, Notification.Position.MIDDLE);
				return;
			}
			grid.asSingleSelect().clear();
			grid.setItems(accounts);
			Notification.show(success);
		}, () -> logger.info("platform account reset notification skipped because account page detached")).run());
	}

	private Checkbox enabledToggle(UserAccount account) {
		Checkbox enabled = new Checkbox();
		enabled.setAriaLabel(Translator.translate("Active"));
		enabled.setValue(account.isEnabled());
		enabled.setEnabled(!account.isBuiltInAdmin());
		enabled.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				UserAccount updated = account.copy();
				updated.setEnabled(event.getValue());
				UserAccountRepository.save(updated);
			}
		});
		return enabled;
	}

	private Component passwordRequiredIndicator(UserAccount account) {
		if (!account.isPasswordChangeRequired()) {
			return new NativeLabel();
		}
		String label = Translator.translate("Access.Account.PasswordRequired");
		Icon warning = new Icon(VaadinIcon.WARNING);
		warning.getStyle().set("color", "var(--lumo-error-color)");
		warning.getStyle().set("width", "1.5em");
		warning.getStyle().set("height", "1.5em");
		warning.getElement().setAttribute("title", label);
		warning.getElement().setAttribute("aria-label", label);
		warning.getElement().setAttribute("role", "img");
		return warning;
	}

	private static String summary(UserAccount account) {
		return account.getGrants().stream().map(AccountsContent::describe).collect(Collectors.joining(", "));
	}

	private static String describe(RoleGrant grant) {
		String role = Translator.translate("Access.Role." + grant.getRole().name());
		return grant.getPlatformName() == null ? role : role + " (" + grant.getPlatformName() + ")";
	}
}
