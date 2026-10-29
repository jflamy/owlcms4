package app.owlcms.access;

import java.util.function.Function;

import org.slf4j.LoggerFactory;

import app.owlcms.data.account.AccountToken;
import app.owlcms.data.account.UserAccount;
import ch.qos.logback.classic.Logger;

/**
 * Authenticates a token presented on a URL or in an Authorization header, for non-interactive requests. The token
 * gives exactly its owner's current rights; it is never logged.
 */
public final class TokenAuthenticator {

	public sealed interface Result {
		/** No token was presented. */
		record Missing() implements Result {
		}

		/** Too many failures from this address. */
		record Throttled(long seconds) implements Result {
		}

		/** Unknown or revoked token, or disabled account. */
		record Invalid() implements Result {
		}

		/** A valid token; the caller decides whether its owner may perform the operation. */
		record Authenticated(UserAccount account, AccountToken token) implements Result {
		}
	}

	private static final Logger logger = (Logger) LoggerFactory.getLogger(TokenAuthenticator.class);
	// token failures are throttled per address only: a shared key would let one client block all token users
	private static final String TOKEN_KEY_PREFIX = "#token@";
	private static final String BEARER = "Bearer ";

	private final Function<String, UserAccount> byTokenHash;
	private final LoginThrottle throttle;

	/** @param byTokenHash finds the account holding a token with the given SHA-256 */
	public TokenAuthenticator(Function<String, UserAccount> byTokenHash, LoginThrottle throttle) {
		this.byTokenHash = byTokenHash;
		this.throttle = throttle;
	}

	/** @return the token from the {@code token} parameter, else from a Bearer Authorization header, else null */
	public static String presentedToken(String tokenParameter, String authorizationHeader) {
		if (tokenParameter != null && !tokenParameter.isBlank()) {
			return tokenParameter.strip();
		}
		if (authorizationHeader != null && authorizationHeader.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
			String value = authorizationHeader.substring(BEARER.length()).strip();
			return value.isEmpty() ? null : value;
		}
		return null;
	}

	/** Identifies the token's owner; authorization is up to the caller. */
	public Result authenticate(String token, String clientIp) {
		if (token == null) {
			return new Result.Missing();
		}
		long blocked = this.throttle.blockedSeconds(TOKEN_KEY_PREFIX + clientIp, clientIp);
		if (blocked > 0) {
			logger./**/warn("token request throttled ip={} seconds={}", clientIp, blocked);
			return new Result.Throttled(blocked);
		}
		String hash = AccountTokens.hash(token);
		UserAccount account = this.byTokenHash.apply(hash);
		AccountToken record = account == null ? null
		        : account.getTokens().stream().filter(t -> hash.equals(t.getTokenHash())).findFirst().orElse(null);
		if (account == null || record == null || !account.isEnabled()) {
			this.throttle.fail(TOKEN_KEY_PREFIX + clientIp, clientIp);
			logger./**/warn("token refused ip={}", clientIp);
			return new Result.Invalid();
		}
		this.throttle.success(TOKEN_KEY_PREFIX + clientIp, clientIp);
		return new Result.Authenticated(account, record);
	}

	/** True if the account holds the role, directly or through a composite role, on any platform. */
	public static boolean holds(UserAccount account, Role role) {
		return AccessPolicy.expandedRoles(Principal.of(account, null), null).contains(role);
	}
}
