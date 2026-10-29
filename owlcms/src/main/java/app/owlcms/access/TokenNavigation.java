package app.owlcms.access;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import app.owlcms.data.account.AccountToken;
import app.owlcms.data.account.UserAccount;

/**
 * Decides whether a navigation that carries a {@code token} parameter, or that comes from a tab already opened with a
 * token, may proceed. Pure logic: no Vaadin types, no session, and no I/O except through the parameters. The access
 * listener acts on the decision.
 * <p>
 * The token is the account token in accounts mode and the display PIN in PIN mode.
 */
public final class TokenNavigation {

	public static final int UNAUTHORIZED = 401;
	public static final int FORBIDDEN = 403;
	public static final int TOO_MANY_REQUESTS = 429;

	static final String TOKEN = "token";
	static final String FOP = "fop";
	private static final String DISPLAY_PIN_KEY_PREFIX = "#displaypin@";

	private TokenNavigation() {
	}

	/** What the URL says, before any authentication. */
	public sealed interface Inputs {
		/** No token; the platform is null unless the page is platform-bound. */
		record NoToken(String platform) implements Inputs {
		}

		record Present(String token, String platform) implements Inputs {
		}

		/** Two or more token values. */
		record Malformed() implements Inputs {
		}

		/** The platform is missing when several exist, repeated, or unknown. */
		record BadPlatform() implements Inputs {
		}
	}

	public sealed interface Decision {
		/** Not a token navigation: continue with the existing logic. */
		record NotToken() implements Decision {
		}

		/** @param username the token owner when known, else null; @param tokenId the token id when known, else null */
		record Refuse(int status, String reason, String username, String tokenId) implements Decision {
		}

		/** @param newlyBound the tab was just opened with the token in its URL */
		record Allow(TokenTab tab, boolean newlyBound) implements Decision {
		}
	}

	public sealed interface PinDecision {
		/** Not applicable: the token is ignored and the existing PIN flow applies. */
		record NotApplicable() implements PinDecision {
		}

		record Refuse(int status, String reason) implements PinDecision {
		}

		record Accept() implements PinDecision {
		}
	}

	/**
	 * Reads the token and the platform from the URL, without side effects.
	 *
	 * @param platformBound whether the page operates on one platform
	 * @param platformNames the existing platforms
	 */
	public static Inputs readInputs(Map<String, List<String>> query, boolean platformBound,
	        List<String> platformNames) {
		List<String> tokens = nonBlank(query.get(TOKEN));
		if (tokens.size() > 1) {
			return new Inputs.Malformed();
		}
		String platform = null;
		if (platformBound) {
			List<String> fops = query.get(FOP);
			if (fops != null && !fops.isEmpty()) {
				if (fops.size() != 1) {
					return new Inputs.BadPlatform();
				}
				platform = URLDecoder.decode(fops.get(0), StandardCharsets.UTF_8);
				if (!platformNames.contains(platform)) {
					return new Inputs.BadPlatform();
				}
			} else if (platformNames.size() == 1) {
				platform = platformNames.get(0);
			} else {
				return new Inputs.BadPlatform();
			}
		}
		return tokens.isEmpty() ? new Inputs.NoToken(platform) : new Inputs.Present(tokens.get(0).strip(), platform);
	}

	/**
	 * Accounts mode.
	 *
	 * @param existing the token identity already bound to this tab, or null
	 * @param findById finds an account by id, or null
	 */
	public static Decision decideAccounts(PageRule rule, Map<String, List<String>> query, List<String> platformNames,
	        TokenTab existing, String clientIp, TokenAuthenticator authenticator, Function<Long, UserAccount> findById) {
		boolean tokenInUrl = !nonBlank(query.get(TOKEN)).isEmpty();
		if (!tokenInUrl && existing == null) {
			return new Decision.NotToken();
		}
		String boundUser = existing != null ? existing.principal().username() : null;
		String boundTokenId = existing != null ? existing.tokenId() : null;
		if (!rule.tokenAccessible()) {
			return tokenInUrl ? new Decision.Refuse(FORBIDDEN, "not-token-page", null, null)
			        : new Decision.Refuse(FORBIDDEN, "not-token-page", boundUser, boundTokenId);
		}
		Inputs inputs = readInputs(query, rule.platformBound(), platformNames);
		switch (inputs) {
			case Inputs.Malformed _ -> {
				return new Decision.Refuse(UNAUTHORIZED, "malformed", boundUser, boundTokenId);
			}
			case Inputs.BadPlatform _ -> {
				return new Decision.Refuse(FORBIDDEN, "platform", boundUser, boundTokenId);
			}
			default -> {
			}
		}
		String platform = inputs instanceof Inputs.Present present ? present.platform()
		        : ((Inputs.NoToken) inputs).platform();

		UserAccount account;
		String tokenId;
		boolean newlyBound;
		if (inputs instanceof Inputs.Present present) {
			switch (authenticator.authenticate(present.token(), clientIp)) {
				case TokenAuthenticator.Result.Throttled _ -> {
					return new Decision.Refuse(TOO_MANY_REQUESTS, "throttled", null, null);
				}
				case TokenAuthenticator.Result.Authenticated authenticated -> {
					account = authenticated.account();
					tokenId = authenticated.token().getTokenId();
					newlyBound = true;
				}
				default -> {
					return new Decision.Refuse(UNAUTHORIZED, "invalid", null, null);
				}
			}
		} else {
			account = findById.apply(existing.accountId());
			tokenId = existing.tokenId();
			if (account == null || !account.isEnabled() || !holdsToken(account, tokenId)) {
				return new Decision.Refuse(UNAUTHORIZED, "revoked", boundUser, boundTokenId);
			}
			newlyBound = false;
		}

		Principal principal = Principal.ofToken(account);
		if (!AccessPolicy.canOpen(principal, rule, platform)) {
			return new Decision.Refuse(FORBIDDEN, "forbidden", account.getUsername(), tokenId);
		}
		return new Decision.Allow(new TokenTab(account.getId(), tokenId, principal), newlyBound);
	}

	/**
	 * PIN mode: the display PIN may be passed as {@code token}, as a weak shared secret.
	 *
	 * @param displayPinExpected whether a display PIN is configured
	 * @param ipAllowedForDisplay whether the display address list, if any, accepts the address
	 * @param displayPinMatches compares a value with the display PIN, in constant time
	 */
	public static PinDecision decidePin(PageRule rule, Map<String, List<String>> query, boolean displayPinExpected,
	        boolean ipAllowedForDisplay, String clientIp, Predicate<String> displayPinMatches,
	        LoginThrottle throttle) {
		List<String> values = nonBlank(query.get(TOKEN));
		if (values.isEmpty() || !rule.tokenAccessible() || !displayPinExpected) {
			return new PinDecision.NotApplicable();
		}
		if (values.size() > 1) {
			return new PinDecision.Refuse(UNAUTHORIZED, "malformed");
		}
		if (!ipAllowedForDisplay) {
			return new PinDecision.Refuse(FORBIDDEN, "display-ip");
		}
		String key = DISPLAY_PIN_KEY_PREFIX + clientIp;
		if (throttle.blockedSeconds(key, clientIp) > 0) {
			return new PinDecision.Refuse(TOO_MANY_REQUESTS, "throttled");
		}
		if (displayPinMatches.test(values.get(0).strip())) {
			throttle.success(key, clientIp);
			return new PinDecision.Accept();
		}
		throttle.fail(key, clientIp);
		return new PinDecision.Refuse(UNAUTHORIZED, "invalid");
	}

	private static boolean holdsToken(UserAccount account, String tokenId) {
		for (AccountToken token : account.getTokens()) {
			if (tokenId.equals(token.getTokenId())) {
				return true;
			}
		}
		return false;
	}

	private static List<String> nonBlank(List<String> values) {
		return values == null ? List.of() : values.stream().filter(v -> v != null && !v.isBlank()).toList();
	}
}
