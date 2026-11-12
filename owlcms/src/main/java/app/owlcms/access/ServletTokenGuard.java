package app.owlcms.access;

import org.slf4j.LoggerFactory;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.apputils.AccessUtils.PinCheck;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authorization of plain servlets that extract competition data. Simple Access (PIN) mode: the officials PIN, from any
 * address the officials access list allows; with no officials PIN, the local network. Accounts mode: an account token
 * whose owner has the required permission, whatever the address. The secret is given as {@code ?token=} or
 * Authorization Bearer.
 */
public final class ServletTokenGuard {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ServletTokenGuard.class);

	private ServletTokenGuard() {
	}

	/**
	 * Sets the refusal status and headers itself.
	 *
	 * @param host     the client address, for the audit and PIN mode
	 * @param required the permission the token's owner must have, through any of its roles
	 * @param what     what is requested, for the log
	 * @return the audit actor of the request, or null when refused
	 */
	public static AuditActor authorize(HttpServletRequest request, HttpServletResponse response, String host,
	        Permission required, String what) {
		String path = request.getServletPath();
		String token = TokenAuthenticator.presentedToken(request.getParameter("token"),
		        request.getHeader("Authorization"));
		if (!required.isAvailable()) {
			// refused in both access modes: Simple Access does not check permissions
			logger./**/warn("{} {} refused: the {} permission is not available in this configuration", host, what,
			        required);
			AuthAudit.tokenRefused(host, path, "unavailable", null, null);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		if (!Config.getCurrent().isAccountsMode()) {
			return authorizeSimpleAccess(response, host, path, token, what);
		}
		TokenAuthenticator authenticator = new TokenAuthenticator(UserAccountRepository::findByTokenHash,
		        LoginThrottle.shared());
		switch (authenticator.authenticate(token, host)) {
			case TokenAuthenticator.Result.Authenticated authenticated -> {
				String username = authenticated.account().getUsername();
				String tokenId = authenticated.token().getTokenId();
				if (TokenAuthenticator.holds(authenticated.account(), required)) {
					logger.info("{} {} authorized by token id={} of account {}", host, what, tokenId, username);
					return AuditActor.token(username, tokenId, host);
				}
				logger./**/warn("{} {} refused: token id={} of account {} does not have the {} permission", host,
				        what, tokenId, username, required);
				AuthAudit.tokenRefused(host, path, "forbidden", username, tokenId);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
			case TokenAuthenticator.Result.Missing _ -> {
				logger.info("{} {} refused: no token", host, what);
				response.setHeader("WWW-Authenticate", "Bearer");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
			case TokenAuthenticator.Result.Invalid _ -> {
				AuthAudit.tokenRefused(host, path, "invalid", null, null);
				response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
			case TokenAuthenticator.Result.Throttled throttled -> {
				AuthAudit.tokenRefused(host, path, "throttled", null, null);
				response.setHeader("Retry-After", Long.toString(throttled.seconds()));
				response.setStatus(429);
			}
		}
		return null;
	}

	/** Simple Access mode: the officials PIN when one is set, else the local network. */
	private static AuditActor authorizeSimpleAccess(HttpServletResponse response, String host, String path,
	        String pin, String what) {
		if (!PinModeAuthenticator.officialsPinExpected()) {
			if (AccessUtils.isLocalNetwork(host)) {
				logger.info("{} authorized {}", host, what);
				return AuditActor.http(host);
			}
			logger.error("{} not on the local network, denied {}", host, what);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		if (!AccessUtils.ipIsAllowedForOfficials(host)) {
			logger.error("{} not in the officials access list, denied {}", host, what);
			AuthAudit.tokenRefused(host, path, "address", null, null);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		if (pin == null) {
			logger.info("{} {} refused: no officials PIN", host, what);
			response.setHeader("WWW-Authenticate", "Bearer");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}
		PinCheck check = AccessUtils.checkOfficialsPinForServlet(pin, host);
		if (check.accepted()) {
			logger.info("{} {} authorized by the officials PIN", host, what);
			return AuditActor.http(host);
		}
		if (check.blockedSeconds() > 0) {
			AuthAudit.tokenRefused(host, path, "throttled", null, null);
			response.setHeader("Retry-After", Long.toString(check.blockedSeconds()));
			response.setStatus(429);
			return null;
		}
		logger./**/warn("{} {} refused: wrong officials PIN", host, what);
		AuthAudit.tokenRefused(host, path, "invalid", null, null);
		response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		return null;
	}
}
