package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import app.owlcms.access.AccessMode;
import app.owlcms.access.AccessStartup;
import app.owlcms.access.AccountModeAuthenticator;
import app.owlcms.access.Principal;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.access.SessionLogout;
import app.owlcms.audit.AuditOrigin;
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
@Route(value = AccountsContent.ROUTE, layout = OwlcmsLayout.class)
public class AccountsContent extends BaseContent implements OwlcmsContent {

	static final String ROUTE = "preparation/accounts";
	// Zero-based tab indices. The URL uses these values directly, so "/0" is equivalent to "/".
	public static final int GENERIC_TAB = 0;
	public static final int USERS_TAB = 1;

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountsContent.class);
	private OwlcmsLayout routerLayout;
	private GridCrud<UserAccount> genericCrud;
	private GridCrud<UserAccount> usersCrud;
	private TabSheet tabSheet;

	public AccountsContent() {
		this.genericCrud = createGrid(true);
		this.usersCrud = createGrid(false);
		this.tabSheet = new TabSheet();
		this.tabSheet.add(Translator.translate("Access.Accounts.Generic"), this.genericCrud);
		this.tabSheet.add(Translator.translate("Access.Accounts.Users"), this.usersCrud);
		this.tabSheet.setSizeFull();
		this.tabSheet.setMinHeight("0");
		// keep the selection in the URL, so that reloading or a link opens the same tab
		this.tabSheet.addSelectedChangeListener(event -> UI.getCurrent().getPage().getHistory()
		        .replaceState(null, new Location(ROUTE + "/" + this.tabSheet.getSelectedIndex())));
		fillHW(this.tabSheet, this);
	}

	/** Opens directly on a tab when the URL ends with its index, as in {@code preparation/accounts/1}. */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		super.setParameter(event, parameter);
		if (parameter == null || parameter.isBlank()) {
			return;
		}
		try {
			int index = Integer.parseInt(parameter);
			if (index >= 0 && index < this.tabSheet.getTabCount()) {
				this.tabSheet.setSelectedIndex(index);
			}
		} catch (NumberFormatException e) {
			// ignore invalid tab parameter
		}
	}

	/** Lists one group of accounts; an edit can move an account to the other group, so both grids are refreshed. */
	private class GroupListener implements CrudListener<UserAccount> {
		private final OwlcmsCrudFormFactory<UserAccount> factory;
		private final boolean generic;

		GroupListener(OwlcmsCrudFormFactory<UserAccount> factory, boolean generic) {
			this.factory = factory;
			this.generic = generic;
		}

		@Override
		public Collection<UserAccount> findAll() {
			return UserAccountRepository.findAll().stream().filter(a -> a.isGeneric() == this.generic).toList();
		}

		@Override
		public UserAccount add(UserAccount account) {
			UserAccount added = this.factory.add(account);
			refreshOther();
			return added;
		}

		@Override
		public UserAccount update(UserAccount account) {
			UserAccount updated = this.factory.update(account);
			refreshOther();
			return updated;
		}

		@Override
		public void delete(UserAccount account) {
			this.factory.delete(account);
			refreshOther();
		}

		private void refreshOther() {
			GridCrud<UserAccount> other = this.generic ? AccountsContent.this.usersCrud
			        : AccountsContent.this.genericCrud;
			if (other != null) {
				other.refreshGrid();
			}
		}
	}

	private Checkbox createAccessModeSwitch() {
		Checkbox accountsMode = new Checkbox(Translator.translate("Access.Mode.ACCOUNTS"));
		accountsMode.setValue(Config.getCurrent().isAccountsMode());
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
			config.chooseAccessMode(event.getValue() ? AccessMode.ACCOUNTS : AccessMode.PIN);
			Config.setCurrent(config, AuditOrigin.fromUi("User Accounts access-mode switch"));
			Principal principal = OwlcmsSession.getPrincipal();
			if (principal != null && principal.source() == Principal.AuthSource.ACCOUNT) {
				AccountModeAuthenticator.logout(principal, "access mode changed");
			}
			SessionLogout.redirectAllAndInvalidate(UI.getCurrent().getSession());
		});
		return accountsMode;
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

	private GridCrud<UserAccount> createGrid(boolean generic) {
		OwlcmsCrudFormFactory<UserAccount> crudFormFactory = new AccountEditingFormFactory(UserAccount.class, generic);
		Grid<UserAccount> grid = new Grid<>(UserAccount.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addComponentColumn(this::enabledToggle).setHeader(Translator.translate("Active")).setAutoWidth(true).setFlexGrow(0);
		Grid.Column<UserAccount> platformColumn = grid.addColumn(AccountsContent::platforms)
		        .setHeader(Translator.translate("Platform")).setAutoWidth(true).setFlexGrow(0)
		        .setComparator(Comparator.comparing(AccountsContent::platformSortKey)
		                .thenComparing(a -> a.getUsername().toLowerCase(Locale.ROOT)));
		Grid.Column<UserAccount> usernameColumn = grid.addColumn(UserAccount::getUsername)
		        .setHeader(Translator.translate("Access.Username"))
		        .setComparator(Comparator.comparing(a -> a.getUsername().toLowerCase(Locale.ROOT)));
		grid.addComponentColumn(this::passwordRequiredIndicator)
		        .setHeader(Translator.translate("Access.Account.PasswordRequired")).setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(a -> summary(a)).setHeader(Translator.translate("Access.Account.Grants"))
		        .setComparator(Comparator.comparing(AccountsContent::summary));
		grid.sort(List.of(new GridSortOrder<>(generic ? platformColumn : usernameColumn, SortDirection.ASCENDING)));

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(UserAccount.class);
		GridCrud<UserAccount> gridCrud = new OwlcmsCrudGrid<>(UserAccount.class, gridLayout, crudFormFactory, grid);
		gridCrud.setCrudListener(new GroupListener(crudFormFactory, generic));
		gridCrud.setClickRowToUpdate(true);
		if (generic) {
			gridCrud.getCrudLayout().addToolbarComponent(createAccessModeSwitch());
			gridCrud.getCrudLayout().addToolbarComponent(createRecomputeGenericAccountsButton());
		}
		gridCrud.getCrudLayout().addToolbarComponent(createClearPasswordsButton(generic));
		((HorizontalLayout) gridLayout.getToolbarLayout())
		        .setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		return gridCrud;
	}

	private void setGridsEnabled(boolean enabled) {
		this.genericCrud.getElement().setEnabled(enabled);
		this.usersCrud.getElement().setEnabled(enabled);
	}

	private void refreshGrids() {
		this.genericCrud.getGrid().asSingleSelect().clear();
		this.usersCrud.getGrid().asSingleSelect().clear();
		this.genericCrud.refreshGrid();
		this.usersCrud.refreshGrid();
	}

	private Button createRecomputeGenericAccountsButton() {
		String label = Translator.translate("Access.Account.RecomputeGeneric");
		Button recompute = new Button(label, event -> new ConfirmationDialog(
		        label, Translator.translate("Access.Account.RecomputeGenericConfirm"),
		        label, null, this::recomputeGenericAccounts).open());
		return recompute;
	}

	private void recomputeGenericAccounts() {
		UI ui = UI.getCurrent();
		String success = Translator.translate("Access.Account.RecomputeGenericDone");
		String failureMessage = Translator.translate("Access.Account.RecomputeGenericFailed");
		setGridsEnabled(false);
		AccessStartup.recomputeGenericAccountsInBackground(AuditOrigin.fromUi("Recompute Generic Accounts"))
		        .whenComplete((accounts, failure) -> ui.accessLater(() -> {
			setGridsEnabled(true);
			if (failure != null) {
				Notification.show(failureMessage, 5000, Notification.Position.MIDDLE);
				return;
			}
			refreshGrids();
			Notification.show(success);
		}, () -> logger.info("generic account recompute notification skipped because account page detached")).run());
	}

	private Button createClearPasswordsButton(boolean generic) {
		String label = Translator.translate("Access.Account.ClearPasswords");
		Button clear = new Button(label, event -> new ConfirmationDialog(
		        label, Translator.translate("Access.Account.ClearPasswordsConfirm"),
		        label, null, () -> clearPasswords(generic)).open());
		clear.addThemeVariants(ButtonVariant.LUMO_ERROR);
		return clear;
	}

	private void clearPasswords(boolean generic) {
		UI ui = UI.getCurrent();
		Principal principal = OwlcmsSession.getPrincipal();
		Long actingAccountId = principal != null && principal.source() == Principal.AuthSource.ACCOUNT
		        ? principal.accountId()
		        : null;
		String actingUser = principal != null ? principal.username() : "-";
		String failureMessage = Translator.translate("Access.Account.ClearPasswordsFailed");
		setGridsEnabled(false);
		AccessStartup.clearPasswordsInBackground(generic, actingAccountId, actingUser,
		        AuditOrigin.fromUi("Clear All Passwords"))
		        .whenComplete((count, failure) -> ui.accessLater(() -> {
			        setGridsEnabled(true);
			        if (failure != null) {
				        Notification.show(failureMessage, 5000, Notification.Position.MIDDLE);
				        return;
			        }
			        refreshGrids();
			        Notification.show(Translator.translate("Access.Account.ClearPasswordsDone", count));
		        }, () -> logger.info("password clearing notification skipped because account page detached")).run());
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
				UserAccountRepository.save(updated, AuditOrigin.fromUi("User Accounts active checkbox"));
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

	/** The platforms named in the grants; blank for accounts that are not tied to a platform. */
	private static String platforms(UserAccount account) {
		return account.getGrants().stream().map(RoleGrant::getPlatformName).filter(Objects::nonNull).distinct()
		        .sorted().collect(Collectors.joining(", "));
	}

	/** Accounts without a platform sort first, so the global accounts stay together at the top. */
	private static String platformSortKey(UserAccount account) {
		return platforms(account).toLowerCase(Locale.ROOT);
	}

	private static String describe(RoleGrant grant) {
		String role = Translator.translate("Access.Role." + grant.getRole().name());
		return grant.getPlatformName() == null ? role : role + " (" + grant.getPlatformName() + ")";
	}
}
