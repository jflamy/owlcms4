package app.owlcms.endpoints;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;

import org.slf4j.LoggerFactory;

import app.owlcms.access.RememberedAccount;
import app.owlcms.access.RememberedBrowser;
import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.access.RememberedBrowser.Redeemed;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exchanges a one-time ticket, issued by the login page after a correct PIN or account password, for the "remember
 * this browser" cookie. The cookie is set here because the login itself happens over the push connection.
 */
@SuppressWarnings("serial")
public class RememberServlet extends HttpServlet {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(RememberServlet.class);
	private static final int MAX_TICKET_LENGTH = 200;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		byte[] body = request.getInputStream().readNBytes(MAX_TICKET_LENGTH);
		Redeemed redeemed = RememberedBrowser.redeem(new String(body, StandardCharsets.UTF_8),
		        System.currentTimeMillis());
		String host = ProxyUtils.getClientIp(request);
		if (redeemed == null) {
			logger./**/warn("{} remember-browser ticket refused", host);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		Kind kind = redeemed.kind();
		Config config = Config.getCurrent();
		if (kind == Kind.ACCOUNT) {
			rememberAccount(request, response, redeemed, config, host);
			return;
		}
		String pinBasis = RememberedBrowser.pinBasis(kind, config);
		if (pinBasis == null) {
			// no PIN: nothing to remember
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}
		String secret = rememberSecret(config);
		long expires = Instant.now().plus(RememberedBrowser.LIFETIME).getEpochSecond();
		response.addCookie(cookie(request, kind, RememberedBrowser.cookieValue(kind, pinBasis, secret, expires),
		        (int) RememberedBrowser.LIFETIME.toSeconds()));
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		logger.info("{} browser remembered for the {} PIN", host, kind);
		AuthAudit.browserRemembered(host, kind.name());
	}

	/** Until midnight, with the account's current password hash, if the setting and the account still allow it. */
	private static void rememberAccount(HttpServletRequest request, HttpServletResponse response, Redeemed redeemed,
	        Config config, String host) {
		UserAccount account = UserAccountRepository.findById(redeemed.accountId());
		if (!config.isAccountsMode() || !config.allowsRememberedAccountLogins()
		        || !RememberedAccount.isRememberable(account)) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}
		String secret = rememberSecret(config);
		Instant now = Instant.now();
		ZoneId zone = RememberedAccount.zone(config);
		long expires = RememberedAccount.nextMidnight(now, zone);
		response.addCookie(cookie(request, Kind.ACCOUNT, RememberedAccount.cookieValue(account.getId(),
		        redeemed.platform(), account.getPasswordHash(), secret, expires),
		        (int) (expires - now.getEpochSecond())));
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		String until = Instant.ofEpochSecond(expires).atZone(zone).toOffsetDateTime().toString();
		logger.info("{} login of {} remembered until {}", host, account.getUsername(), until);
		AuthAudit.accountLoginRemembered(host, account.getUsername(), until);
	}

	private static Cookie cookie(HttpServletRequest request, Kind kind, String value, int maxAgeSeconds) {
		Cookie cookie = new Cookie(kind.getCookieName(), value);
		cookie.setHttpOnly(true);
		cookie.setSecure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")));
		cookie.setPath(cookiePath(request));
		cookie.setMaxAge(maxAgeSeconds);
		cookie.setAttribute("SameSite", "Lax");
		return cookie;
	}

	/** The login page, when "remember this browser" was unchecked: the browser forgets this PIN. */
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		String body = new String(request.getInputStream().readNBytes(MAX_TICKET_LENGTH), StandardCharsets.UTF_8);
		for (Kind kind : Kind.values()) {
			if (kind.name().equals(body.strip())) {
				Cookie cookie = new Cookie(kind.getCookieName(), "");
				cookie.setHttpOnly(true);
				cookie.setPath(cookiePath(request));
				cookie.setMaxAge(0);
				response.addCookie(cookie);
				logger.info("{} browser forgotten for the {} cookie", ProxyUtils.getClientIp(request), kind);
			}
		}
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	static String cookiePath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		return contextPath == null || contextPath.isEmpty() ? "/" : contextPath;
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
