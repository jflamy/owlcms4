package app.owlcms.access;

import org.slf4j.LoggerFactory;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authorization of plain servlets that extract competition data. PIN mode: local network.
 * Accounts mode: an account token ({@code ?token=} or Authorization Bearer) whose owner holds the required role,
 * whatever the address.
 */
public final class ServletTokenGuard {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ServletTokenGuard.class);

	private ServletTokenGuard() {
	}

	/**
	 * Sets the refusal status and headers itself.
	 *
	 * @param host     the client address, for the audit and PIN mode
	 * @param required the role the token's owner must hold, directly or through a composite role
	 * @param what     what is requested, for the log
	 * @return the audit actor of the request, or null when refused
	 */
	public static AuditActor authorize(HttpServletRequest request, HttpServletResponse response, String host,
	        Role required, String what) {
		if (!Config.getCurrent().isAccountsMode()) {
			if (AccessUtils.isLocalNetwork(host)) {
				logger.info("{} authorized {}", host, what);
				return AuditActor.http(host);
			}
			logger.error("{} not on the local network, denied {}", host, what);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		String path = request.getServletPath();
		String token = TokenAuthenticator.presentedToken(request.getParameter("token"),
		        request.getHeader("Authorization"));
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
				logger./**/warn("{} {} refused: token id={} of account {} does not hold the {} role", host, what,
				        tokenId, username, required);
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
}
