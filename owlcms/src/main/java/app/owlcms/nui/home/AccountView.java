package app.owlcms.nui.home;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.access.AccountPasswords;
import app.owlcms.access.AccountPasswords.ChangeResult;
import app.owlcms.access.AuthenticatedPage;
import app.owlcms.access.Principal;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.ContentWrapping;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.OwlcmsLayoutAware;

/**
 * Self-service page: the session's own account and its password. Also the page every other page redirects
 * to while the account must change its password. Nothing here takes an account from the request.
 */
@SuppressWarnings("serial")
@Route(value = AccountPasswords.ACCOUNT_ROUTE, layout = OwlcmsLayout.class)
@AuthenticatedPage
public class AccountView extends Composite<VerticalLayout>
        implements OwlcmsLayoutAware, ContentWrapping, HasDynamicTitle {

	private OwlcmsLayout routerLayout;
	private PasswordField currentPasswordField;
	private PasswordField newPasswordField;
	private PasswordField confirmField;

	public AccountView() {
		Principal principal = OwlcmsSession.getPrincipal();
		UserAccount account = principal != null && principal.source() == Principal.AuthSource.ACCOUNT
		        ? UserAccountRepository.findById(principal.accountId())
		        : null;

		VerticalLayout form = new VerticalLayout();
		form.add(createTitle());
		if (account == null) {
			form.add(new Paragraph(Translator.translate("Access.Account.NoPersonalAccount")));
		} else {
			buildForm(form, account, AccountPasswords.mustChangePassword(principal));
		}
		form.setWidth("24em");
		getContent().add(form);
	}

	private H3 createTitle() {
		H3 title = new H3(Translator.translate("Access.Account"));
		title.getStyle().set("color", "var(--lumo-header-text-color)");
		title.getStyle().set("font-size", "var(--lumo-font-size-xl)");
		return title;
	}

	private void buildForm(VerticalLayout form, UserAccount account, boolean mustChange) {
		if (mustChange) {
			Paragraph notice = new Paragraph(Translator.translate("Access.Account.MustChangeNotice"));
			notice.getStyle().set("font-weight", "bold");
			form.add(notice);
		}

		TextField usernameField = new TextField(Translator.translate("Access.Username"));
		usernameField.setValue(account.getUsername());
		usernameField.setReadOnly(true);
		usernameField.setWidthFull();

		this.currentPasswordField = new PasswordField(Translator.translate("Access.Account.CurrentPassword"));
		this.currentPasswordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
		this.newPasswordField = new PasswordField(Translator.translate("Access.Account.NewPassword"));
		this.newPasswordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
		this.confirmField = new PasswordField(Translator.translate("Access.Account.ConfirmPassword"));
		this.confirmField.setAutocomplete(Autocomplete.NEW_PASSWORD);
		for (PasswordField field : new PasswordField[] { this.currentPasswordField, this.newPasswordField,
		        this.confirmField }) {
			field.setRevealButtonVisible(true);
			field.setWidthFull();
		}

		Button change = new Button(Translator.translate("Access.Account.ChangePassword"), e -> changePassword());
		change.addClickShortcut(Key.ENTER);
		change.getThemeNames().add("primary");
		form.add(usernameField, this.currentPasswordField, this.newPasswordField,
		        this.confirmField, change);
		form.setAlignSelf(Alignment.CENTER, change);
	}

	// runs on the request thread like LoginView.submitAccount; the account is the session's, never a request value
	private void changePassword() {
		this.currentPasswordField.setInvalid(false);
		this.newPasswordField.setInvalid(false);
		this.confirmField.setInvalid(false);

		String newPassword = this.newPasswordField.getValue();
		if (!newPassword.equals(this.confirmField.getValue())) {
			showError(this.confirmField, "Access.Error.PasswordMismatch");
			return;
		}

		Principal principal = OwlcmsSession.getPrincipal();
		boolean forced = AccountPasswords.mustChangePassword(principal);
		ChangeResult result = AccountPasswords.changeOwnPassword(principal, this.currentPasswordField.getValue(),
		        newPassword, AuditOrigin.fromUi("Account page"));
		this.currentPasswordField.clear();
		switch (result) {
			case CHANGED -> {
				this.newPasswordField.clear();
				this.confirmField.clear();
				if (forced) {
					UI.getCurrent().navigate(HomeNavigationContent.class);
				}
				Notification.show(Translator.translate("Access.Account.PasswordChanged"));
			}
			case WRONG_CURRENT_PASSWORD -> showError(this.currentPasswordField, "Access.Error.CurrentPasswordWrong");
			case UNACCEPTABLE -> showError(this.newPasswordField, "Access.Error.PasswordTooShort");
			case SAME_AS_CURRENT -> showError(this.newPasswordField, "Access.Error.NewPasswordSame");
			case NO_ACCOUNT -> showError(this.currentPasswordField, "Access.Error.NoAccount");
		}
	}

	private static void showError(PasswordField field, String key) {
		field.setErrorMessage(Translator.translate(key));
		field.setInvalid(true);
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	public String getMenuTitle() {
		return Translator.translate("OWLCMS_Top");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Access.Account");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setHeaderContent() {
		NativeLabel label = new NativeLabel(getMenuTitle());
		label.getStyle().set("font-size", "var(--lumo-font-size-xl)");
		Image image = new Image("icons/owlcms.png", "owlcms icon");
		image.getStyle().set("height", "7ex");
		image.getStyle().set("width", "auto");
		HorizontalLayout topBarTitle = new HorizontalLayout(image, label);
		topBarTitle.setAlignSelf(Alignment.CENTER, label);
		this.routerLayout.setMenuTitle(topBarTitle);
		this.routerLayout.setMenuArea(createMenuArea());
		this.routerLayout.showLocaleDropdown(true);
		this.routerLayout.setDrawerOpened(true);
		this.routerLayout.updateHeader(true);
	}

	@Override
	public void setPadding(boolean b) {
		// not needed
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
}
