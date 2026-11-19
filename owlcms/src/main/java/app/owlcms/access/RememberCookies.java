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
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.access.RememberedLogin;
import app.owlcms.data.config.Config;
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
		if (!config.allowsRememberedLogins()) {
			return;
		}
		String pinBasis = RememberedBrowser.pinBasis(kind, config);
		if (pinBasis == null) {
			// no PIN: nothing to remember
			return;
		}
		Instant now = Instant.now();
		RememberedLogins.forget(value(request, kind), kind);
		RememberedLogins.Issued issued = RememberedLogins.issue(kind, null, null, pinBasis, now,
		        RememberedAccount.zone(config), host, request.getHeader("User-Agent"));
		response.addCookie(cookie(request, kind, issued.cookieValue(), maxAge(issued.login(), now)));
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
		if (!config.isAccountsMode() || !config.allowsRememberedLogins()
		        || !RememberedAccount.isRememberable(account)) {
			return;
		}
		Instant now = Instant.now();
		ZoneId zone = RememberedAccount.zone(config);
		RememberedLogins.forget(value(request, Kind.ACCOUNT), Kind.ACCOUNT);
		RememberedLogins.Issued issued = RememberedLogins.issue(Kind.ACCOUNT, account, platform,
		        account.getPasswordHash(), now, zone, host, request.getHeader("User-Agent"));
		response.addCookie(cookie(request, Kind.ACCOUNT, issued.cookieValue(), maxAge(issued.login(), now)));
		String until = issued.login().getExpiresAt().atZone(zone).toOffsetDateTime().toString();
		logger.info("{} login of {} remembered until {}", host, account.getUsername(), until);
		AuthAudit.accountLoginRemembered(host, account.getUsername(), until);
	}

	/** The browser forgets this kind of login. */
	public static void forget(Kind kind, HttpServletRequest request, HttpServletResponse response, String host) {
		RememberedLogins.forget(value(request, kind), kind);
		deleteCookie(kind, request, response);
		logger.info("{} browser forgotten for the {} cookie", host, kind);
	}

	/** At logout: the browser forgets every remembered login. */
	public static void forgetAll(HttpServletRequest request, HttpServletResponse response) {
		for (Kind kind : Kind.values()) {
			forget(kind, request, response, "logout");
		}
	}

	/** Delete a cookie without touching a row, for an expired or invalid token already handled by the store. */
	public static void deleteCookie(Kind kind, HttpServletRequest request, HttpServletResponse response) {
		response.addCookie(cookie(request, kind, "", 0));
	}

	/** @return the cookie value for this kind, or null */
	public static String value(HttpServletRequest request, Kind kind) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (kind.getCookieName().equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
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

	private static int maxAge(RememberedLogin login, Instant now) {
		return (int) Math.max(0, login.getExpiresAt().getEpochSecond() - now.getEpochSecond());
	}
}
