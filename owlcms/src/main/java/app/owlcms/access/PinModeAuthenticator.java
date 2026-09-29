package app.owlcms.access;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;

import app.owlcms.access.Principal.AuthSource;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.LoginView;
import ch.qos.logback.classic.Logger;

/**
 * Resolves the principal when the officials PIN / display PIN scheme is in use. This is the logic that used to live in
 * {@code AuthorizationDispatch} and {@code RequireDisplayLogin}; the session flags are kept in step so code that
 * still reads them keeps working.
 */
public final class PinModeAuthenticator {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(PinModeAuthenticator.class);

	private PinModeAuthenticator() {
	}

	/** @return the principal, or null when the login page must be shown */
	public static Principal resolveOfficials(String path, QueryParameters queryParameters) {
		if (OwlcmsSession.isAuthenticated()) {
			return Principal.admin(AuthSource.OFFICIALS_PIN);
		}

		if (!path.equals(LoginView.LOGIN)) {
			OwlcmsSession.setRequestedUrl(path);
			OwlcmsSession.setRequestedQueryParameters(queryParameters);
		}

		Config config = Config.getCurrent();
		String paramPin = config.getParamPin();
		String dbPin = config.getPin();
		String backdoorList = config.getParamBackdoorList();

		boolean backdoor = backdoorList != null && !backdoorList.isBlank();
		String clientIp = AccessUtils.getClientIp();
		if (backdoor && AccessUtils.checkBackdoor(clientIp)) {
			// explicit backdoor access allowed (e.g. for video capture of browser screens)
			logger.info("Backdoor access from {}", clientIp);
			OwlcmsSession.setAuthenticated(true);
			return Principal.admin(AuthSource.BACKDOOR);
		}

		boolean pinExpected = pinExpected(paramPin, dbPin);
		if (!pinExpected && AccessUtils.ipIsAllowedForOfficials(clientIp)) {
			OwlcmsSession.setAuthenticated(true);
			return Principal.admin(AuthSource.NO_PIN);
		}
		return null;
	}

	/** @return the principal, or null when the display login page must be shown */
	public static Principal resolveDisplay(String path, QueryParameters queryParameters) {
		if (OwlcmsSession.computeDisplayAuthenticated()) {
			return OwlcmsSession.isAuthenticated() ? Principal.admin(AuthSource.OFFICIALS_PIN) : Principal.displays();
		}

		Config config = Config.getCurrent();
		String displayList = config.getParamDisplayList();
		// An explicit empty OWLCMS_DISPLAYPIN disables any database display PIN.
		String displayPinOverride = config.getParamDisplayPin();
		String dbDisplayPin = config.getDisplayPin();
		String backdoorList = config.getParamBackdoorList();

		boolean noDisplayPin = !pinExpected(displayPinOverride, dbDisplayPin);
		boolean noDisplayList = displayList == null || displayList.isBlank();
		boolean backdoor = backdoorList != null && !backdoorList.isBlank();
		if (noDisplayPin && noDisplayList) {
			OwlcmsSession.setDisplayAuthenticated(true);
			return Principal.displays();
		}
		if (backdoor && AccessUtils.checkBackdoor(AccessUtils.getClientIp())) {
			logger.info("allowing backdoor access from {}", AccessUtils.getClientIp());
			OwlcmsSession.setDisplayAuthenticated(true);
			return Principal.admin(AuthSource.BACKDOOR);
		}
		if (noDisplayPin && AccessUtils.isIpAllowedForDisplay(AccessUtils.getClientIp())) {
			OwlcmsSession.setDisplayAuthenticated(true);
			return Principal.displays();
		}
		// if whitelist membership is required, the user is prompted even if no PIN is required, so that an error
		// message is shown
		if (!path.equals(LoginView.LOGIN)) {
			OwlcmsSession.setRequestedUrl(path);
			OwlcmsSession.setRequestedQueryParameters(queryParameters);
		}
		return null;
	}

	static boolean pinExpected(String override, String storedPin) {
		return override != null ? !override.isBlank() : storedPin != null && !storedPin.isBlank();
	}
}
