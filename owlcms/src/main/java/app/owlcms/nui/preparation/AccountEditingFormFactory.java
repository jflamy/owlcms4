package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder.Binding;

import app.owlcms.access.PasswordHasher;
import app.owlcms.access.Principal;
import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
class AccountEditingFormFactory extends OwlcmsCrudFormFactory<UserAccount> {

	AccountEditingFormFactory(Class<UserAccount> domainType) {
		super(domainType);
	}

	@Override
	public UserAccount add(UserAccount account) {
		return UserAccountRepository.save(account);
	}

	@Override
	public UserAccount update(UserAccount account) {
		return UserAccountRepository.save(account);
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
		UserAccountRepository.delete(account);
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

		TextField displayNameField = new TextField(Translator.translate("Access.Account.DisplayName"));
		layout.add(displayNameField);
		this.binder.forField(displayNameField)
		        .withNullRepresentation("")
		        .bind(UserAccount::getDisplayName, UserAccount::setDisplayName);

		Checkbox enabledField = new Checkbox(Translator.translate("Access.Account.Enabled"));
		enabledField.setReadOnly(account.isBuiltInAdmin());
		layout.add(enabledField);
		this.binder.forField(enabledField).bind(UserAccount::isEnabled, UserAccount::setEnabled);

		PasswordField newPasswordField = new PasswordField(Translator.translate("Access.Account.NewPassword"));
		PasswordField confirmField = new PasswordField(Translator.translate("Access.Account.ConfirmPassword"));
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
