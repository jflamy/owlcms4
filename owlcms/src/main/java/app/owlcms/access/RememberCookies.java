/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

import java.time.Instant;
import java.time.ZoneId;

import org.slf4j.LoggerFactory;

import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sets and deletes the "remember" cookies on the HTTP response of the request that did the login. The login page's
 * button click reaches the server as an HTTP request (the push transport is WEBSOCKET_XHR: only server-to-client
 * messages use the WebSocket), so its response carries the cookie; there is no separate endpoint to call.
 */
public final class RememberCookies {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(RememberCookies.class);

	private RememberCookies() {
	}

	/** After a correct officials or display PIN, when "remember this browser" is checked. */
	public static void rememberPin(Kind kind, HttpServletRequest request, HttpServletResponse response, String host) {
		Config config = Config.getCurrent();
		String pinBasis = RememberedBrowser.pinBasis(kind, config);
		if (pinBasis == null) {
			// no PIN: nothing to remember
			return;
		}
		long expires = Instant.now().plus(RememberedBrowser.LIFETIME).getEpochSecond();
		response.addCookie(cookie(request, kind,
		        RememberedBrowser.cookieValue(kind, pinBasis, rememberSecret(config), expires),
		        (int) RememberedBrowser.LIFETIME.toSeconds()));
		logger.info("{} browser remembered for the {} PIN", host, kind);
		AuthAudit.browserRemembered(host, kind.name());
	}

	/**
	 * After a password login in accounts mode: until midnight, with the account's current password hash, if the
	 * setting and the account allow it.
	 *
	 * @param platform the platform chosen at login, or null
	 */
	public static void rememberAccount(UserAccount account, String platform, HttpServletRequest request,
	        HttpServletResponse response, String host) {
		Config config = Config.getCurrent();
		if (!config.isAccountsMode() || !config.allowsRememberedAccountLogins()
		        || !RememberedAccount.isRememberable(account)) {
			return;
		}
		Instant now = Instant.now();
		ZoneId zone = RememberedAccount.zone(config);
		long expires = RememberedAccount.nextMidnight(now, zone);
		response.addCookie(cookie(request, Kind.ACCOUNT, RememberedAccount.cookieValue(account.getId(), platform,
		        account.getPasswordHash(), rememberSecret(config), expires), (int) (expires - now.getEpochSecond())));
		String until = Instant.ofEpochSecond(expires).atZone(zone).toOffsetDateTime().toString();
		logger.info("{} login of {} remembered until {}", host, account.getUsername(), until);
		AuthAudit.accountLoginRemembered(host, account.getUsername(), until);
	}

	/** The browser forgets this kind of login. */
	public static void forget(Kind kind, HttpServletRequest request, HttpServletResponse response, String host) {
		response.addCookie(cookie(request, kind, "", 0));
		logger.info("{} browser forgotten for the {} cookie", host, kind);
	}

	/** At logout: the browser forgets every remembered login. */
	public static void forgetAll(HttpServletRequest request, HttpServletResponse response) {
		for (Kind kind : Kind.values()) {
			response.addCookie(cookie(request, kind, "", 0));
		}
	}

	private static Cookie cookie(HttpServletRequest request, Kind kind, String value, int maxAgeSeconds) {
		Cookie cookie = new Cookie(kind.getCookieName(), value);
		cookie.setHttpOnly(true);
		cookie.setSecure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")));
		String contextPath = request.getContextPath();
		cookie.setPath(contextPath == null || contextPath.isEmpty() ? "/" : contextPath);
		cookie.setMaxAge(maxAgeSeconds);
		cookie.setAttribute("SameSite", "Lax");
		return cookie;
	}

	private static String rememberSecret(Config config) {
		String secret = config.getRememberSecret();
		if (secret == null || secret.isBlank()) {
			// created at most once: the row is checked again under the save lock
			Config saved = ConfigRepository.apply(c -> {
				String stored = c.getRememberSecret();
				if (stored == null || stored.isBlank()) {
					c.setRememberSecret(RememberedBrowser.newSecret());
				}
			}, AuditOrigin.system("remember browser secret created"));
			secret = saved.getRememberSecret();
		}
		return secret;
	}
}
