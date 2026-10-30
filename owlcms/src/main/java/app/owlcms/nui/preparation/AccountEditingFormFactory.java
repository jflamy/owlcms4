package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder.Binding;

import app.owlcms.access.AccessStartup;
import app.owlcms.access.AccountModeAuthenticator;
import app.owlcms.access.PasswordHasher;
import app.owlcms.access.Principal;
import app.owlcms.access.Role;
import app.owlcms.access.SessionLogout;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.components.fields.OwlcmsPasswordField;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.AccountTokenEditor;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
class AccountEditingFormFactory extends OwlcmsCrudFormFactory<UserAccount> {

	private final boolean generic;
	// the token section of the form being shown; its pending change is applied on Add or Update
	private AccountTokenEditor tokenEditor;

	/** @param generic whether accounts added through this form are generic (role) accounts */
	AccountEditingFormFactory(Class<UserAccount> domainType, boolean generic) {
		super(domainType);
		this.generic = generic;
	}

	@Override
	public UserAccount add(UserAccount account) {
		account.setGeneric(this.generic);
		return saveAndApplyModeChange(account, AuditOrigin.fromUi("User Accounts form"));
	}

	@Override
	public UserAccount update(UserAccount account) {
		return saveAndApplyModeChange(account, AuditOrigin.fromUi("User Accounts form"));
	}

	/**
	 * Setting the admin password can bring accounts mode into force (audit key present); the role accounts
	 * are then initialized and every session is sent to the login page. Called on the UI thread.
	 */
	private UserAccount saveAndApplyModeChange(UserAccount account, AuditOrigin origin) {
		if (this.tokenEditor != null) {
			this.tokenEditor.applyTo(account);
		}
		Config config = Config.getCurrent();
		boolean wasAccountsMode = config.isAccountsMode();
		UserAccount saved = UserAccountRepository.save(account, origin);
		boolean isAccountsMode = config.isAccountsMode();
		if (wasAccountsMode == isAccountsMode) {
			return saved;
		}
		if (isAccountsMode) {
			AccessStartup.initializeRoleAccountsInBackground(origin);
		}
		Principal principal = OwlcmsSession.getPrincipal();
		if (principal != null && principal.source() == Principal.AuthSource.ACCOUNT) {
			AccountModeAuthenticator.logout(principal, "access mode changed");
		}
		UI ui = UI.getCurrent();
		if (ui != null) {
			SessionLogout.redirectAllAndInvalidate(ui.getSession());
		}
		return saved;
	}

	@Override
	public void delete(UserAccount account) {
		if (account.isBuiltInAdmin()) {
			refuse("Access.Error.BuiltInAdmin");
		}
		Principal principal = OwlcmsSession.getPrincipal();
		if (principal != null && account.getId().equals(principal.accountId())) {
			refuse("Access.Error.OwnAccount");
		}
		UserAccountRepository.delete(account, AuditOrigin.fromUi("User Accounts form"));
	}

	@Override
	public Collection<UserAccount> findAll() {
		// implemented on grid
		return null;
	}

	@Override
	public String buildCaption(CrudOperation operation, UserAccount domainObject) {
		String what = Translator.translate("Access.Account");
		if (operation.equals(CrudOperation.ADD)) {
			return Translator.translate("Add") + " " + what;
		} else if (operation.equals(CrudOperation.UPDATE)) {
			return Translator.translate("Update") + " " + what;
		} else if (operation.equals(CrudOperation.DELETE)) {
			return Translator.translate("Delete") + " " + what;
		}
		return super.buildCaption(operation, domainObject);
	}

	@Override
	public Component buildNewForm(CrudOperation operation, UserAccount account, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {
		this.binder = buildBinder(operation, account);
		Component footer = this.buildFooter(operation, account, cancelButtonClickListener,
		        operationButtonClickListener, deleteButtonClickListener, true);
		Component form = accountLayout(account);
		VerticalLayout mainLayout = new VerticalLayout(form, footer);
		this.binder.readBean(account);
		return mainLayout;
	}

	private FormLayout accountLayout(UserAccount account) {
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField usernameField = new TextField(Translator.translate("Access.Username"));
		usernameField.setReadOnly(account.isBuiltInAdmin());
		layout.add(usernameField);
		this.binder.forField(usernameField)
		        .asRequired(Translator.translate("Access.Error.UsernameRequired"))
		        .withValidator(name -> isUnique(name, account), Translator.translate("Access.Error.DuplicateUsername"))
		        .bind(UserAccount::getUsername, UserAccount::setUsername);

		Checkbox enabledField = new Checkbox(Translator.translate("Access.Account.Enabled"));
		enabledField.setReadOnly(account.isBuiltInAdmin());
		layout.add(enabledField);
		this.binder.forField(enabledField).bind(UserAccount::isEnabled, UserAccount::setEnabled);

		PasswordField newPasswordField = new OwlcmsPasswordField(Translator.translate("Access.Account.NewPassword"));
		PasswordField confirmField = new OwlcmsPasswordField(Translator.translate("Access.Account.ConfirmPassword"));
		layout.add(newPasswordField, confirmField);
		// blank means unchanged; the hash is computed on save, and the typed value is never kept
		Binding<UserAccount, String> passwordBinding = this.binder.forField(newPasswordField)
		        .withValidator(v -> isBlank(v) || PasswordHasher.isAcceptable(v),
		                Translator.translate("Access.Error.PasswordTooShort"))
		        .withValidator(v -> isBlank(v) || v.equals(confirmField.getValue()),
		                Translator.translate("Access.Error.PasswordMismatch"))
		        .bind(a -> "", (a, v) -> {
			        if (!isBlank(v)) {
				        a.setPasswordHash(PasswordHasher.hash(v));
			        }
		        });
		this.binder.forField(confirmField).bind(a -> "", (a, v) -> {
		});
		confirmField.addValueChangeListener(e -> passwordBinding.validate());

		// the flag is only meaningful if the account has a password once saved: the existing one or a new one
		Checkbox mustChangeField = new Checkbox(Translator.translate("Access.Account.MustChangePassword"));
		layout.add(mustChangeField);
		Binding<UserAccount, Boolean> mustChangeBinding = this.binder.forField(mustChangeField)
		        .withValidator(checked -> !checked || account.hasPassword() || !isBlank(newPasswordField.getValue()),
		                Translator.translate("Access.Error.MustChangeNeedsPassword"))
		        .bind(UserAccount::isMustChangePassword, UserAccount::setMustChangePassword);
		newPasswordField.addValueChangeListener(e -> mustChangeBinding.validate());

		List<String> platformNames = PlatformRepository.findAll().stream().map(Platform::getName)
		        .collect(Collectors.toList());
		GrantsField grantsField = new GrantsField(platformNames);
		grantsField.setLabel(Translator.translate("Access.Account.Grants"));
		layout.add(grantsField);
		this.binder.forField(grantsField)
		        .withValidator(grants -> !account.isBuiltInAdmin() || holdsAdminEverywhere(grants),
		                Translator.translate("Access.Error.BuiltInAdmin"))
		        .withValidator(grants -> grants.stream().allMatch(
		                g -> g.getPlatformName() == null || platformNames.contains(g.getPlatformName())),
		                Translator.translate("Access.Error.UnknownPlatform"))
		        .bind(UserAccount::getGrants, UserAccount::setGrants);

		// the built-in admin is a person's login, not a device key; it gets no token
		this.tokenEditor = account.isBuiltInAdmin() ? null
		        : new AccountTokenEditor(account.getToken(), OwlcmsSession.getLocale());
		if (this.tokenEditor != null) {
			layout.add(this.tokenEditor);
		}

		return layout;
	}

	private static boolean isUnique(String name, UserAccount account) {
		UserAccount existing = UserAccountRepository.findByUsername(name);
		return existing == null || existing.getId().equals(account.getId());
	}

	private static boolean holdsAdminEverywhere(List<RoleGrant> grants) {
		return grants.stream().anyMatch(g -> g.getRole() == Role.ADMIN && g.getPlatformName() == null);
	}

	private static boolean isBlank(String s) {
		return s == null || s.isEmpty();
	}

	private static void refuse(String key) {
		String message = Translator.translate(key);
		Notification.show(message);
		throw new CrudOperationException(message);
	}
}
