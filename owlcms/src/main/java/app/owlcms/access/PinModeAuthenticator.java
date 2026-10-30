package app.owlcms.access;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinRequest;

import app.owlcms.access.Principal.AuthSource;
import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.config.Config;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.LoginView;
import jakarta.servlet.http.Cookie;

/**
 * Resolves the principal when the officials PIN / display PIN scheme is in use. This is the logic that used to live in
 * {@code AuthorizationDispatch} and {@code RequireDisplayLogin}; the session flags are kept in step so code that
 * still reads them keeps working.
 */
public final class PinModeAuthenticator {

	private PinModeAuthenticator() {
	}

	/** @return the principal, or null when the login page must be shown */
	public static Principal resolveOfficials(String path, QueryParameters queryParameters) {
		return resolveOfficials(path, queryParameters, true);
	}

	/**
	 * @param acceptRemembered false on the login page itself: after a logout it shows the PIN form, even to a
	 *                         remembered browser, which any other page lets in
	 * @return the principal, or null when the login page must be shown
	 */
	public static Principal resolveOfficials(String path, QueryParameters queryParameters, boolean acceptRemembered) {
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
		String clientIp = AccessUtils.getClientIp();
		boolean pinExpected = pinExpected(paramPin, dbPin);
		if (!pinExpected && AccessUtils.ipIsAllowedForOfficials(clientIp)) {
			OwlcmsSession.setAuthenticated(true);
			return Principal.admin(AuthSource.NO_PIN);
		}
		if (acceptRemembered && pinExpected && AccessUtils.ipIsAllowedForOfficials(clientIp)
		        && remembered(Kind.OFFICIALS, config)) {
			OwlcmsSession.setAuthenticated(true);
			Principal principal = Principal.admin(AuthSource.OFFICIALS_PIN);
			auditRememberedLogin(principal, "officials pin");
			return principal;
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

		boolean noDisplayPin = !pinExpected(displayPinOverride, dbDisplayPin);
		boolean noDisplayList = displayList == null || displayList.isBlank();
		if (noDisplayPin && noDisplayList) {
			OwlcmsSession.setDisplayAuthenticated(true);
			return Principal.displays();
		}
		if (AccessUtils.ipIsAllowedForOfficials(AccessUtils.getClientIp()) && remembered(Kind.OFFICIALS, config)) {
			OwlcmsSession.setAuthenticated(true);
			OwlcmsSession.setDisplayAuthenticated(true);
			Principal principal = Principal.admin(AuthSource.OFFICIALS_PIN);
			auditRememberedLogin(principal, "officials pin");
			return principal;
		}
		boolean displayAddressAllowed = noDisplayList || AccessUtils.isIpAllowedForDisplay(AccessUtils.getClientIp());
		if (!noDisplayPin && displayAddressAllowed && remembered(Kind.DISPLAY, config)) {
			OwlcmsSession.setDisplayAuthenticated(true);
			Principal principal = Principal.displays();
			auditRememberedLogin(principal, "display pin");
			return principal;
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

	/** @return true if a display PIN is configured (an empty override disables the stored one) */
	public static boolean displayPinExpected() {
		Config config = Config.getCurrent();
		return pinExpected(config.getParamDisplayPin(), config.getDisplayPin());
	}

	/** @return true if the current request comes from a browser remembered for this PIN */
	public static boolean isRemembered(Kind kind) {
		return remembered(kind, Config.getCurrent());
	}

	/**
	 * The login page's Login button with no PIN, from a browser the page found remembered when it opened.
	 *
	 * @return the principal, or null if the address is not allowed
	 */
	public static Principal loginRemembered(Kind kind) {
		String clientIp = AccessUtils.getClientIp();
		Principal principal;
		if (kind == Kind.OFFICIALS) {
			if (!AccessUtils.ipIsAllowedForOfficials(clientIp)) {
				return null;
			}
			OwlcmsSession.setAuthenticated(true);
			principal = Principal.admin(AuthSource.OFFICIALS_PIN);
		} else {
			String displayList = Config.getCurrent().getParamDisplayList();
			if (displayList != null && !displayList.isBlank() && !AccessUtils.isIpAllowedForDisplay(clientIp)) {
				return null;
			}
			OwlcmsSession.setDisplayAuthenticated(true);
			principal = Principal.displays();
		}
		auditRememberedLogin(principal, kind == Kind.OFFICIALS ? "officials pin" : "display pin");
		return principal;
	}

	/** @return true if the request carries a valid "remember this browser" cookie for the current PIN */
	private static boolean remembered(Kind kind, Config config) {
		VaadinRequest request = VaadinRequest.getCurrent();
		Cookie[] cookies = request != null ? request.getCookies() : null;
		if (cookies == null) {
			return false;
		}
		Map<String, String> values = new HashMap<>();
		for (Cookie cookie : cookies) {
			values.put(cookie.getName(), cookie.getValue());
		}
		return RememberedBrowser.remembers(kind, values, config, Instant.now().getEpochSecond());
	}

	private static void auditRememberedLogin(Principal principal, String pin) {
		List<String> platforms = PlatformRepository.findAll().stream().map(Platform::getName).toList();
		AuthAudit.loginSucceeded("PIN", "-", "remembered-browser " + pin,
		        AccessPolicy.platformsWithAccess(principal, platforms));
	}

	static boolean pinExpected(String override, String storedPin) {
		return override != null ? !override.isBlank() : storedPin != null && !storedPin.isBlank();
	}
}
