package app.owlcms.endpoints;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.slf4j.LoggerFactory;

import app.owlcms.access.RememberedBrowser;
import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.config.Config;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exchanges a one-time ticket, issued by the PIN login page after a correct PIN, for the "remember this browser"
 * cookie. The cookie is set here because the login itself happens over the push connection.
 */
@SuppressWarnings("serial")
public class RememberServlet extends HttpServlet {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(RememberServlet.class);
	private static final int MAX_TICKET_LENGTH = 200;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		byte[] body = request.getInputStream().readNBytes(MAX_TICKET_LENGTH);
		Kind kind = RememberedBrowser.redeemTicket(new String(body, StandardCharsets.UTF_8), System.currentTimeMillis());
		String host = ProxyUtils.getClientIp(request);
		if (kind == null) {
			logger./**/warn("{} remember-browser ticket refused", host);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		Config config = Config.getCurrent();
		String pinBasis = RememberedBrowser.pinBasis(kind, config);
		if (pinBasis == null) {
			// no PIN: nothing to remember
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}
		String secret = rememberSecret(config);
		long expires = Instant.now().plus(RememberedBrowser.LIFETIME).getEpochSecond();
		Cookie cookie = new Cookie(kind.getCookieName(),
		        RememberedBrowser.cookieValue(kind, pinBasis, secret, expires));
		cookie.setHttpOnly(true);
		cookie.setSecure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")));
		cookie.setPath(cookiePath(request));
		cookie.setMaxAge((int) RememberedBrowser.LIFETIME.toSeconds());
		cookie.setAttribute("SameSite", "Lax");
		response.addCookie(cookie);
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		logger.info("{} browser remembered for the {} PIN", host, kind);
		AuthAudit.browserRemembered(host, kind.name());
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
				logger.info("{} browser forgotten for the {} PIN", ProxyUtils.getClientIp(request), kind);
			}
		}
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	private static String cookiePath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		return contextPath == null || contextPath.isEmpty() ? "/" : contextPath;
	}

	private static synchronized String rememberSecret(Config config) {
		String secret = config.getRememberSecret();
		if (secret == null || secret.isBlank()) {
			secret = RememberedBrowser.newSecret();
			config.setRememberSecret(secret);
			Config.setCurrent(config, AuditOrigin.system("remember browser secret created"));
		}
		return secret;
	}
}
