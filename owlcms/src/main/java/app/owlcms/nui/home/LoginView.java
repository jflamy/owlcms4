/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import app.owlcms.access.AccountAuthenticator;
import app.owlcms.access.AccountLandingPolicy;
import app.owlcms.access.AccessPolicy;
import app.owlcms.access.LoginThrottle;
import app.owlcms.access.PageRule;
import app.owlcms.access.Principal;
import app.owlcms.access.PublicPage;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.nui.preparation.RecordsNavigationContent;
import app.owlcms.nui.home.navigation.JuryNavigationContent;
import app.owlcms.nui.home.navigation.RefereeNavigationContent;
import app.owlcms.nui.lifting.PassiveAnnouncerContent;
import app.owlcms.nui.lifting.CompetitionDirectorContent;
import app.owlcms.nui.lifting.LiftingNavigationContent;
import app.owlcms.nui.lifting.WeighinContent;
import app.owlcms.nui.lifting.JuryContent;
import app.owlcms.nui.lifting.MarshallContent;
import app.owlcms.nui.lifting.TCContent;
import app.owlcms.nui.lifting.TimekeeperContent;
import app.owlcms.nui.preparation.PreparationNavigationContent;
import app.owlcms.nui.preparation.RegistrationContent;
import app.owlcms.nui.results.ResultsNavigationContent;
import app.owlcms.nui.displays.DisplayNavigationContent;
import app.owlcms.nui.shared.ContentWrapping;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.OwlcmsLayoutAware;
import ch.qos.logback.classic.Logger;

/**
 * Check for proper credentials.
 *
 * Scenarios:
 * <ul>
 * <li>If the IP environment variable is present, it is expected to be a commma-separated address list of IPv4 addresses. Browser must come from one of these
 * addresses The IP address(es) will normally be those for the local router or routers used at the competition site.
 * <li>if a PIN environment variable is present, the PIN will be required (even if no IP whitelist)
 * <li>if PIN enviroment variable is not present, all accesses from the whitelisted routers will be allowed. This can be sufficient if the router password is
 * well-protected (which is not likely). Users can type any NIP, including an empty value.
 * <li>if neither IP nor PIN is present, no check is done (the login view is not displayed).
 * </ul>
 */
@SuppressWarnings("serial")
@PublicPage
@Route(value = LoginView.LOGIN, layout = OwlcmsLayout.class)
public class LoginView extends Composite<VerticalLayout>
        implements OwlcmsLayoutAware, ContentWrapping, HasDynamicTitle {

	public static final String LOGIN = "login";
	static Logger logger = (Logger) LoggerFactory.getLogger(LoginView.class);
	private PasswordField pinField = new PasswordField();
	private TextField usernameField = new TextField();
	private PasswordField passwordField = new PasswordField();
	private Span errorMessage = new Span();
	private OwlcmsLayout routerLayout;

	public LoginView() {
		if (Config.getCurrent().isAccountsMode()) {
			buildAccountsForm();
		} else {
			buildPinForm();
		}
	}

	private H3 createTitle() {
		// brute-force the color because some display views use a white text color.
		H3 h3 = new H3(Translator.translate("Log_In"));
		h3.getStyle().set("color", "var(--lumo-header-text-color)");
		h3.getStyle().set("font-size", "var(--lumo-font-size-xl)");
		return h3;
	}

	private void buildPinForm() {
		this.pinField.setClearButtonVisible(true);
		this.pinField.setRevealButtonVisible(true);
		this.pinField.setAutofocus(true);
		this.pinField.setLabel(Translator.translate("EnterPin"));
		this.pinField.setWidthFull();
		this.pinField.addValueChangeListener(event -> {
			String value = event.getValue();
			if (checkAuthenticated(value)) {
				AuthAudit.loginFailed("PIN", "-", loginKind());
				this.pinField.setErrorMessage(Translator.translate("LoginDenied"));
				this.pinField.setInvalid(true);
			} else {
				Principal principal = authenticatedPrincipal();
				AuthAudit.loginSucceeded("PIN", "-", loginKind(), loginAuditPlatforms(principal));
				this.pinField.setInvalid(false);
				redirect();
			}
		});

		H3 h3 = createTitle();

		Button button = new Button(Translator.translate("Login"));
		button.addClickShortcut(Key.ENTER);
		button.setWidth("10em");
		button.getThemeNames().add("primary");
		button.getThemeNames().add("icon");

		VerticalLayout form = new VerticalLayout();
		form.add(h3, this.pinField, button);
		form.setWidth("20em");
		form.setAlignSelf(Alignment.CENTER, button);

		getContent().add(form);
	}

	private void buildAccountsForm() {
		this.usernameField.setLabel(Translator.translate("Access.Username"));
		this.usernameField.setAutocomplete(Autocomplete.USERNAME);
		this.usernameField.setAutofocus(true);
		this.usernameField.setWidthFull();

		this.passwordField.setLabel(Translator.translate("Access.Password"));
		this.passwordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
		this.passwordField.setRevealButtonVisible(true);
		this.passwordField.setWidthFull();

		this.errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
		this.errorMessage.setVisible(false);

		Button button = new Button(Translator.translate("Login"), e -> submitAccount());
		button.addClickShortcut(Key.ENTER);
		button.setWidth("10em");
		button.getThemeNames().add("primary");
		button.getThemeNames().add("icon");

		VerticalLayout form = new VerticalLayout();
		form.add(createTitle(), this.usernameField, this.passwordField, this.errorMessage, button);
		form.setWidth("20em");
		form.setAlignSelf(Alignment.CENTER, button);
		getContent().add(form);
	}

	// runs on the request thread: session rotation needs the current request. Deliberate, bounded CPU cost.
	private void submitAccount() {
		String username = this.usernameField.getValue();
		String clientIp = AccessUtils.getClientIp();
		AccountAuthenticator authenticator = new AccountAuthenticator(UserAccountRepository::findByUsername,
		        LoginThrottle.shared(), AccessUtils::ipIsAllowedForOfficials);
		AccountAuthenticator.Result result = authenticator.authenticate(username, this.passwordField.getValue(),
		        clientIp);
		this.passwordField.clear();

		switch (result) {
			case AccountAuthenticator.Result.Success success -> completeAccountAuthentication(success.account(), clientIp);
			case AccountAuthenticator.Result.Throttled throttled ->
			    showError(Translator.translate("Access.TooManyAttempts") + " (" + throttled.seconds() + " s)");
			case AccountAuthenticator.Result.IpRefused _ -> {
				AuthAudit.loginFailed("ACCOUNTS", username, "address not allowed");
				showError(Translator.translate("Access.LoginFailed"));
			}
			case AccountAuthenticator.Result.Failure _ -> {
				AuthAudit.loginFailed("ACCOUNTS", username, "invalid credentials");
				showError(Translator.translate("Access.LoginFailed"));
			}
		}
	}

	private void completeAccountAuthentication(UserAccount account, String clientIp) {
		VaadinRequest request = VaadinRequest.getCurrent();
		if (request != null) {
			VaadinService.reinitializeSession(request);
		}
		Principal principal = Principal.of(account, null);
		if (!AccessPolicy.requiresPlatformChoice(principal)) {
			completeAccountLogin(account, null, clientIp);
			return;
		}

		java.util.List<FieldOfPlay> choices = AccessPolicy.loginPlatformChoices(principal,
		        OwlcmsFactory.getFOPs().stream().map(FieldOfPlay::getName).toList()).stream()
		        .map(OwlcmsFactory::getFOPByName).filter(java.util.Objects::nonNull).toList();
		if (choices.isEmpty()) {
			showError(Translator.translate("Access.Denied.Message"));
			return;
		}
		FieldOfPlay requested = requestedLoginPlatform(choices);
		if (choices.size() == 1) {
			completeAccountLogin(account, choices.get(0), clientIp);
			return;
		}
		showPlatformChoice(account, clientIp, choices, requested);
	}

	private void showPlatformChoice(UserAccount account, String clientIp, java.util.List<FieldOfPlay> choices,
	        FieldOfPlay requested) {
		ComboBox<FieldOfPlay> platformField = new ComboBox<>(Translator.translate("CompetitionPlatform"));
		platformField.setItems(choices);
		platformField.setItemLabelGenerator(FieldOfPlay::getName);
		platformField.setPlaceholder(Translator.translate("SelectPlatform"));
		platformField.setWidthFull();
		platformField.setValue(requested);

		Button button = new Button(Translator.translate("Login"), event -> {
			FieldOfPlay selected = platformField.getValue();
			if (selected == null) {
				showError(Translator.translate("Access.Denied.Message"));
				return;
			}
			completeAccountLogin(account, selected, clientIp);
		});
		button.addClickShortcut(Key.ENTER);
		button.setWidth("10em");
		button.getThemeNames().add("primary");
		button.getThemeNames().add("icon");

		VerticalLayout form = new VerticalLayout();
		form.add(createTitle(), platformField, this.errorMessage, button);
		form.setWidth("20em");
		form.setAlignSelf(Alignment.CENTER, button);
		getContent().removeAll();
		getContent().add(form);
	}

	private FieldOfPlay requestedLoginPlatform(java.util.List<FieldOfPlay> choices) {
		com.vaadin.flow.router.QueryParameters queryParameters = OwlcmsSession.getRequestedQueryParameters();
		if (queryParameters == null) {
			return null;
		}
		return queryParameters.getSingleParameter("fop").map(OwlcmsFactory::getFOPByName)
		        .filter(choices::contains).orElse(null);
	}

	private void completeAccountLogin(UserAccount account, FieldOfPlay loginPlatform, String clientIp) {
		Principal principal = Principal.of(account, loginPlatform != null ? loginPlatform.getName() : null);
		OwlcmsSession.setPrincipal(principal);
		if (loginPlatform != null) {
			OwlcmsSession.setFop(loginPlatform);
		}
		logger.info("login user={} ip={}", account.getUsername(), clientIp);
		AuthAudit.loginSucceeded("ACCOUNTS", account.getUsername(), "password", loginAuditPlatforms(principal));
		redirect();
	}

	private static java.util.List<String> loginAuditPlatforms(Principal principal) {
		java.util.List<String> platforms = PlatformRepository.findAll().stream().map(p -> p.getName()).toList();
		return AccessPolicy.platformsWithAccess(principal, platforms);
	}

	protected Principal authenticatedPrincipal() {
		return Principal.admin(Principal.AuthSource.OFFICIALS_PIN);
	}

	private void showError(String message) {
		this.errorMessage.setText(message);
		this.errorMessage.setVisible(true);
	}

	/** What was checked, for the audit trail. */
	protected String loginKind() {
		return "officials pin";
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
		return Translator.translate("Login");
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

	protected boolean checkAuthenticated(String value) {
		return !AccessUtils.checkAuthenticated(value);
	}

	protected void redirect() {
		String requestedUrl = OwlcmsSession.getRequestedUrl();
		Principal principal = OwlcmsSession.getPrincipal();
		if (Config.getCurrent().isAccountsMode() && principal != null) {
			if (requestedUrl != null && requestedRouteAllowed(principal)) {
				UI.getCurrent().navigate(requestedUrl, OwlcmsSession.getRequestedQueryParameters());
				return;
			}
			redirectAccountLanding(principal);
			return;
		}
		if (requestedUrl != null) {
			UI.getCurrent().navigate(requestedUrl, OwlcmsSession.getRequestedQueryParameters());
		} else if (Config.getCurrent().isRecordRepository()) {
			UI.getCurrent().navigate(RecordsNavigationContent.class);
		} else {
			UI.getCurrent().navigate(HomeNavigationContent.class);
		}
	}

	private boolean requestedRouteAllowed(Principal principal) {
		String path = OwlcmsSession.getRequestedUrl();
		QueryParameters parameters = OwlcmsSession.getRequestedQueryParameters();
		if (path == null || parameters == null) {
			return false;
		}
		Class<?> target = VaadinService.getCurrent().getRouter().resolveNavigationTarget(new Location(path, parameters))
		        .map(state -> state.getNavigationTarget()).orElse(null);
		if (target == null) {
			return false;
		}
		String platform = parameters.getSingleParameter("fop").orElse(principal.loginPlatform());
		return AccessPolicy.canOpen(principal, PageRule.of(target), platform);
	}

	private void redirectAccountLanding(Principal principal) {
		AccountLandingPolicy.singleWorkPage(principal).ifPresentOrElse(page -> {
			QueryParameters parameters = principal.loginPlatform() != null
			        ? QueryParameters.simple(Map.of("fop", principal.loginPlatform())) : QueryParameters.empty();
			switch (page) {
				case HOME -> UI.getCurrent().navigate(HomeNavigationContent.class);
				case COMPETITION_DIRECTOR -> UI.getCurrent().navigate(CompetitionDirectorContent.class, parameters);
				case PREPARATION -> UI.getCurrent().navigate(PreparationNavigationContent.class, parameters);
				case REGISTRATION -> UI.getCurrent().navigate(RegistrationContent.class, parameters);
				case WEIGHIN -> UI.getCurrent().navigate(WeighinContent.class, parameters);
				case RESULTS -> UI.getCurrent().navigate(ResultsNavigationContent.class, parameters);
				case LIFTING -> UI.getCurrent().navigate(LiftingNavigationContent.class, parameters);
				case ANNOUNCER -> UI.getCurrent().navigate(PassiveAnnouncerContent.class, parameters);
				case MARSHAL -> UI.getCurrent().navigate(MarshallContent.class, parameters);
				case TIMEKEEPER -> UI.getCurrent().navigate(TimekeeperContent.class, parameters);
				case TC -> UI.getCurrent().navigate(TCContent.class, parameters);
				case JURY_CONSOLE -> UI.getCurrent().navigate(JuryContent.class, parameters);
				case REFEREE_NAVIGATION -> UI.getCurrent().navigate(RefereeNavigationContent.class, parameters);
				case JURY_NAVIGATION -> UI.getCurrent().navigate(JuryNavigationContent.class, parameters);
				case DISPLAY_NAVIGATION -> UI.getCurrent().navigate(DisplayNavigationContent.class, parameters);
			}
		}, () -> UI.getCurrent().navigate(HomeNavigationContent.class));
	}

}