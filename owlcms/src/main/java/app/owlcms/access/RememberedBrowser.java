package app.owlcms.access;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;

/**
 * "Remember this browser" for the officials PIN and the display PIN (PIN mode). The browser keeps an HttpOnly cookie
 * holding an expiry and an HMAC of the current PIN, keyed with a secret stored in the settings and never exported.
 * Changing the PIN, or forgetting all browsers (a new secret), invalidates every cookie. A login with the control panel
 * token is never remembered. Accounts mode uses the same secret and tickets for {@link RememberedAccount}.
 * <p>
 * The login page sends a one-time ticket to {@code /remember}, whose HTTP response sets the cookie: the PIN itself is
 * entered over the push connection, which has no response to carry a cookie.
 */
public final class RememberedBrowser {

	public enum Kind {
		OFFICIALS("owlcms-officials"),
		DISPLAY("owlcms-display"),
		/** Accounts mode: keeps one account logged in until midnight; see {@link RememberedAccount}. */
		ACCOUNT("owlcms-account");

		private final String cookieName;

		Kind(String cookieName) {
			this.cookieName = cookieName;
		}

		public String getCookieName() {
			return this.cookieName;
		}
	}

	public static final Duration LIFETIME = Duration.ofDays(30);
	static final Duration TICKET_LIFETIME = Duration.ofMinutes(1);
	private static final String VERSION = "v1";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Map<String, Redeemed> tickets = new ConcurrentHashMap<>();

	/**
	 * What a ticket was issued for.
	 *
	 * @param accountId     the account logged in, for {@link Kind#ACCOUNT}; null for a PIN
	 * @param platform      the platform chosen at that login, or null
	 * @param expiresMillis when the ticket can no longer be exchanged
	 */
	public record Redeemed(Kind kind, Long accountId, String platform, long expiresMillis) {
	}

	private RememberedBrowser() {
	}

	/** @return a new random secret, for the settings */
	public static String newSecret() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * @param pinBasis what the cookie is tied to: the PIN override, or the stored PIN hash
	 * @return the cookie value
	 */
	public static String cookieValue(Kind kind, String pinBasis, String secret, long expiresEpochSeconds) {
		return VERSION + "." + expiresEpochSeconds + "." + mac(kind, pinBasis, secret, expiresEpochSeconds);
	}

	/** @return true if the cookie was issued for this kind, PIN and secret, and has not expired */
	static boolean isValid(String cookie, Kind kind, String pinBasis, String secret, long nowEpochSeconds) {
		if (cookie == null || pinBasis == null || secret == null) {
			return false;
		}
		String[] parts = cookie.split("\\.");
		if (parts.length != 3 || !VERSION.equals(parts[0])) {
			return false;
		}
		long expires;
		try {
			expires = Long.parseLong(parts[1]);
		} catch (NumberFormatException e) {
			return false;
		}
		if (expires <= nowEpochSeconds) {
			return false;
		}
		return AccessUtils.constantTimeEquals(mac(kind, pinBasis, secret, expires), parts[2]);
	}

	/** @return the value the cookie is tied to, or null when no PIN is required (nothing to remember) */
	public static String pinBasis(Kind kind, Config config) {
		if (kind == Kind.ACCOUNT) {
			return null;
		}
		String override = kind == Kind.OFFICIALS ? config.getParamPin() : config.getParamDisplayPin();
		if (override != null) {
			return override.isBlank() ? null : "override:" + override;
		}
		String stored = kind == Kind.OFFICIALS ? config.getPin() : config.getDisplayPin();
		return stored == null || stored.isBlank() ? null : "stored:" + stored;
	}

	/** @return true if one of the cookies remembers this browser for the current PIN */
	public static boolean remembers(Kind kind, Map<String, String> cookies, Config config, long nowEpochSeconds) {
		String secret = config.getRememberSecret();
		return isValid(cookies.get(kind.getCookieName()), kind, pinBasis(kind, config), secret, nowEpochSeconds);
	}

	/** @return a ticket that {@code /remember} exchanges once for the cookie, within a minute */
	public static String issueTicket(Kind kind, long nowMillis) {
		return issue(new Redeemed(kind, null, null, nowMillis + TICKET_LIFETIME.toMillis()), nowMillis);
	}

	/**
	 * After a password login in accounts mode.
	 *
	 * @param platform the platform chosen at login, or null
	 * @return a ticket that {@code /remember} exchanges once for the account cookie, within a minute
	 */
	public static String issueAccountTicket(long accountId, String platform, long nowMillis) {
		return issue(new Redeemed(Kind.ACCOUNT, accountId, platform, nowMillis + TICKET_LIFETIME.toMillis()),
		        nowMillis);
	}

	private static String issue(Redeemed redeemed, long nowMillis) {
		tickets.values().removeIf(t -> t.expiresMillis() < nowMillis);
		byte[] bytes = new byte[24];
		RANDOM.nextBytes(bytes);
		String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		tickets.put(ticket, redeemed);
		return ticket;
	}

	/** @return the kind the ticket was issued for, or null if unknown, used or expired */
	public static Kind redeemTicket(String ticket, long nowMillis) {
		Redeemed redeemed = redeem(ticket, nowMillis);
		return redeemed != null ? redeemed.kind() : null;
	}

	/** @return what the ticket was issued for, or null if unknown, used or expired */
	public static Redeemed redeem(String ticket, long nowMillis) {
		if (ticket == null) {
			return null;
		}
		Redeemed found = tickets.remove(ticket.strip());
		return found != null && found.expiresMillis() >= nowMillis ? found : null;
	}

	private static String mac(Kind kind, String pinBasis, String secret, long expires) {
		return hmac(secret, kind.name() + "\n" + pinBasis + "\n" + expires);
	}

	/** @return the URL-safe HMAC-SHA256 of the payload, keyed with the remember secret */
	static String hmac(String secret, String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException(e);
		}
	}
}
