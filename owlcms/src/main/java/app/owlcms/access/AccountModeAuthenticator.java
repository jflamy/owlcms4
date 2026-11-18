package app.owlcms.access;

import java.time.Instant;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinRequest;

import app.owlcms.access.Principal.AuthSource;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.Cookie;

/**
 * Resolves the principal when named accounts are in use: a login or a token is always required, no backdoor. A login
 * kept until midnight ({@link RememberedAccount}) logs a new session in again.
 */
public final class AccountModeAuthenticator {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountModeAuthenticator.class);

	private AccountModeAuthenticator() {
	}

	/** @return the principal, or null when the login page must be shown */
	public static Principal resolve(String path, QueryParameters queryParameters) {
		Principal existing = OwlcmsSession.getPrincipal();
		if (existing != null && existing.source() == AuthSource.ACCOUNT) {
			UserAccount account = UserAccountRepository.findById(existing.accountId());
			if (account != null && account.isEnabled() && account.hasPassword()) {
				return Principal.of(account, existing.loginPlatform());
			}
			logout(existing, "account disabled, deleted or password cleared");
		}

		Principal remembered = rememberedLogin();
		if (remembered != null) {
			return remembered;
		}

		OwlcmsSession.setRequestedUrl(path);
		OwlcmsSession.setRequestedQueryParameters(queryParameters);
		return null;
	}

	/**
	 * A session without a login, from a browser that kept the login until midnight: the session is logged in again.
	 *
	 * @return the principal, or null if the request has no usable account cookie
	 */
	private static Principal rememberedLogin() {
		String cookie = cookie(RememberedBrowser.Kind.ACCOUNT.getCookieName());
		if (cookie == null) {
			return null;
		}
		Config config = Config.getCurrent();
		List<String> platforms = OwlcmsFactory.getFOPs().stream().map(FieldOfPlay::getName).toList();
		Principal principal = RememberedAccount.restore(cookie, config.allowsRememberedAccountLogins(),
		        config.getRememberSecret(), UserAccountRepository::findById, platforms,
		        Instant.now().getEpochSecond());
		if (principal == null) {
			return null;
		}
		OwlcmsSession.setPrincipal(principal);
		if (principal.loginPlatform() != null) {
			OwlcmsSession.setFop(OwlcmsFactory.getFOPByName(principal.loginPlatform()));
		}
		logger.info("login user={} ip={} how=remembered-browser", principal.username(), AccessUtils.getClientIp());
		AuthAudit.loginSucceeded("ACCOUNTS", principal.username(), "remembered-browser",
		        AccessPolicy.platformsWithAccess(principal, platforms));
		return principal;
	}

	private static String cookie(String name) {
		VaadinRequest request = VaadinRequest.getCurrent();
		Cookie[] cookies = request != null ? request.getCookies() : null;
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	/** Forgets the identity of this session; the caller decides where to send the browser. */
	public static void logout(Principal principal, String reason) {
		logger.info("logout user={} reason={}", principal.username(), reason);
		AuthAudit.loggedOut("ACCOUNTS", principal.username(), reason, principal.loginPlatform());
		OwlcmsSession.setPrincipal(null);
	}
}
