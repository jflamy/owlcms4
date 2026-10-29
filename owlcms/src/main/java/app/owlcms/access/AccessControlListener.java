package app.owlcms.access;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.DisplayLoginView;
import app.owlcms.nui.home.LoginView;
import ch.qos.logback.classic.Logger;

/** The single, deny-by-default access check run on every navigation. */
public class AccessControlListener implements BeforeEnterListener {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccessControlListener.class);

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		OwlcmsFactory.waitDBInitialized();

		Class<?> target = event.getNavigationTarget();
		if (target == null || !target.isAnnotationPresent(Route.class)) {
			// error views
			return;
		}

		PageRule rule = PageRule.of(target);
		if (rule.feature() != null && !rule.feature().isEffective()) {
			event.rerouteToError(NotFoundException.class, "");
			return;
		}

		String path = event.getLocation().getPath();
		QueryParameters queryParameters = event.getLocation().getQueryParameters();

		if (Config.getCurrent().isRecordRepository()) {
			handleRecordsOnly(event, rule, path, queryParameters);
			return;
		}

		if (rule.kind() == PageRule.Kind.PUBLIC) {
			if (!Config.getCurrent().isAccountsMode() && target == LoginView.class) {
				Principal principal = PinModeAuthenticator.resolveOfficials(path, queryParameters);
				if (principal != null) {
					OwlcmsSession.setPrincipal(principal);
					event.forwardTo("", withoutToken(queryParameters));
				}
			}
			return;
		}

		if (handleToken(event, rule, path, queryParameters)) {
			return;
		}
		// nothing below may save a token for the login redirect or carry it to another page: a forward keeps the
		// original query parameters unless they are given explicitly
		queryParameters = withoutToken(queryParameters);

		Principal principal = resolvePrincipal(rule, path, queryParameters);
		if (principal == null) {
			boolean displayLogin = rule.isDisplayPage() && !Config.getCurrent().isAccountsMode();
			event.forwardTo(displayLogin ? DisplayLoginView.LOGIN : LoginView.LOGIN, queryParameters);
			return;
		}
		OwlcmsSession.setPrincipal(principal);

		if (AccountPasswords.mustChangePassword(principal) && !AccountPasswords.ACCOUNT_ROUTE.equals(path)) {
			event.forwardTo(AccountPasswords.ACCOUNT_ROUTE, queryParameters);
			return;
		}

		String platform = rule.platformBound() ? targetPlatform(queryParameters) : null;
		if (!AccessPolicy.canOpen(principal, rule, platform)) {
			logger.info("access denied user={} path={} platform={}", principal.username(), path, platform);
			event.forwardTo(AccessDeniedView.class, queryParameters);
		}
	}

	/**
	 * A navigation carrying {@code token}, or made in a tab opened with a token. Accounts mode: the account token
	 * decides, and a tab bound to a token never falls back to a password login. PIN mode: a correct display PIN in
	 * {@code token} authenticates the session for displays, then the normal flow continues.
	 *
	 * @return true if the navigation was decided here (allowed or refused)
	 */
	private boolean handleToken(BeforeEnterEvent event, PageRule rule, String path, QueryParameters queryParameters) {
		UI ui = event.getUI();
		String clientIp = AccessUtils.getClientIp();
		Map<String, List<String>> query = queryParameters.getParameters();
		if (Config.getCurrent().isAccountsMode()) {
			List<String> platforms = OwlcmsFactory.getFOPs().stream().map(FieldOfPlay::getName).toList();
			TokenAuthenticator authenticator = new TokenAuthenticator(UserAccountRepository::findByTokenHash,
			        LoginThrottle.shared());
			switch (TokenNavigation.decideAccounts(rule, query, platforms, TokenTab.of(ui), clientIp, authenticator,
			        UserAccountRepository::findById)) {
				case TokenNavigation.Decision.NotToken _ -> {
					return false;
				}
				case TokenNavigation.Decision.Refuse refuse -> {
					logger.info("token navigation refused path={} status={} reason={}", path, refuse.status(),
					        refuse.reason());
					AuthAudit.tokenRefused(clientIp, path, refuse.reason(), refuse.username(), refuse.tokenId());
					event.rerouteToError(new TokenRefusedException(refuse.status()), "");
					return true;
				}
				case TokenNavigation.Decision.Allow allow -> {
					TokenTab.bind(ui, allow.tab());
					if (allow.newlyBound()) {
						logger.info("token navigation accepted path={} user={} token id={}", path,
						        allow.tab().principal().username(), allow.tab().tokenId());
						AuthAudit.tokenAccepted(clientIp, path, allow.tab().principal().username(),
						        allow.tab().tokenId());
					}
					return true;
				}
			}
		}
		String displayList = Config.getCurrent().getParamDisplayList();
		boolean ipAllowedForDisplay = displayList == null || displayList.isBlank()
		        || AccessUtils.isIpAllowedForDisplay(clientIp);
		switch (TokenNavigation.decidePin(rule, query, PinModeAuthenticator.displayPinExpected(), ipAllowedForDisplay,
		        clientIp, AccessUtils::displayPinMatches, LoginThrottle.shared())) {
			case TokenNavigation.PinDecision.Refuse refuse -> {
				logger.info("display PIN in URL refused path={} status={} reason={}", path, refuse.status(),
				        refuse.reason());
				AuthAudit.tokenRefused(clientIp, path, refuse.reason(), null, null);
				event.rerouteToError(new TokenRefusedException(refuse.status()), "");
				return true;
			}
			case TokenNavigation.PinDecision.Accept _ -> OwlcmsSession.setDisplayAuthenticated(true);
			case TokenNavigation.PinDecision.NotApplicable _ -> {
			}
		}
		return false;
	}

	private static QueryParameters withoutToken(QueryParameters queryParameters) {
		Map<String, List<String>> parameters = queryParameters.getParameters();
		if (!parameters.containsKey(TokenNavigation.TOKEN)) {
			return queryParameters;
		}
		Map<String, List<String>> kept = new HashMap<>(parameters);
		kept.remove(TokenNavigation.TOKEN);
		return new QueryParameters(kept);
	}

	private Principal resolvePrincipal(PageRule rule, String path, QueryParameters queryParameters) {
		if (Config.getCurrent().isAccountsMode()) {
			return AccountModeAuthenticator.resolve(path, queryParameters);
		}
		return rule.isDisplayPage()
		        ? PinModeAuthenticator.resolveDisplay(path, queryParameters)
		        : PinModeAuthenticator.resolveOfficials(path, queryParameters);
	}

	/** Same fallback as FOPParametersReader: query parameter, then session platform, then default platform. */
	private String targetPlatform(QueryParameters queryParameters) {
		Optional<String> fopName = queryParameters.getSingleParameter("fop");
		if (fopName.isPresent()) {
			return URLDecoder.decode(fopName.get(), StandardCharsets.UTF_8);
		}
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop == null) {
			fop = OwlcmsFactory.getDefaultFOP();
		}
		return fop != null ? fop.getName() : null;
	}

	/** Records-only mode: moved unchanged from AuthorizationDispatch. */
	private void handleRecordsOnly(BeforeEnterEvent event, PageRule rule, String path,
	        QueryParameters queryParameters) {
		if (path.isEmpty()) {
			event.forwardTo("publicRecords");
			return;
		}
		if (rule.isDisplayPage()) {
			RecordsOnlyPolicy.DisplayDestination destination = RecordsOnlyPolicy
			        .displayDestination(OwlcmsSession.isAuthenticated());
			event.forwardTo(destination == RecordsOnlyPolicy.DisplayDestination.EDIT_RECORDS
			        ? "preparation/records"
			        : "publicRecords");
			return;
		}
		if (isRecordsOnlyPublicPath(path)) {
			return;
		}
		if (isRecordsOnlyAdminPath(path)) {
			if (AccessUtils.isBackdoorAccess()) {
				return;
			}
			event.forwardTo("recordsPreparation");
			return;
		}
		if (!isRecordsOnlySecretaryPath(path)) {
			event.forwardTo("publicRecords");
			return;
		}
		Principal principal = PinModeAuthenticator.resolveOfficials(path, queryParameters);
		if (principal == null) {
			event.forwardTo(LoginView.LOGIN);
		} else {
			OwlcmsSession.setPrincipal(principal);
		}
	}

	private static boolean isRecordsOnlyPublicPath(String path) {
		return path.equals(LoginView.LOGIN)
		        || path.equals("publicRecords")
		        || path.equals("recordsPreparation");
	}

	private static boolean isRecordsOnlySecretaryPath(String path) {
		return List.of("preparation/recordsConfig", "preparation/config", "preparation/records").contains(path);
	}

	private static boolean isRecordsOnlyAdminPath(String path) {
		return false;
	}
}
