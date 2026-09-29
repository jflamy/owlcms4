package app.owlcms.access;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.AccessUtils;
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
		if (rule.feature() != null && !Config.getCurrent().featureSwitch(rule.feature())) {
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
					event.forwardTo("");
				}
			}
			return;
		}

		Principal principal = resolvePrincipal(rule, path, queryParameters);
		if (principal == null) {
			boolean displayLogin = rule.isDisplayPage() && !Config.getCurrent().isAccountsMode();
			event.forwardTo(displayLogin ? DisplayLoginView.LOGIN : LoginView.LOGIN);
			return;
		}
		OwlcmsSession.setPrincipal(principal);

		String platform = rule.platformBound() ? targetPlatform(queryParameters) : null;
		if (!AccessPolicy.canOpen(principal, rule, platform)) {
			logger.info("access denied user={} path={} platform={}", principal.username(), path, platform);
			event.forwardTo(AccessDeniedView.class);
		}
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
