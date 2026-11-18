/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Function;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.config.Config;

/**
 * Accounts mode: keeps an account logged in on one browser until midnight, when the security setting allows it. The
 * browser keeps an HttpOnly cookie holding the account, the platform chosen at login and the expiry, with an HMAC that
 * also covers the account's password hash, keyed with the remember secret of the settings (never exported).
 * <p>
 * The cookie stops working when the password changes, the account is disabled or deleted, the setting is off, or all
 * browsers are forgotten (a new secret). Accounts that hold the administrator role always log in with their password,
 * and a login with the control panel token is never remembered.
 */
public final class RememberedAccount {

	private static final String VERSION = "a1";
	/** Stands for "no platform": a Base64 encoding is never a single character. */
	private static final String NO_PLATFORM = "-";

	/** The content of a well-formed cookie, before its MAC is checked against the account. */
	record Claim(long accountId, String platform, long expires, String mac) {
	}

	private RememberedAccount() {
	}

	/** @return true if this account may stay logged in: enabled, with a password, and not an administrator */
	public static boolean isRememberable(UserAccount account) {
		return account != null && !account.isBuiltInAdmin() && account.isEnabled() && account.hasPassword()
		        && !account.isMustChangePassword()
		        && account.getGrants().stream().noneMatch(grant -> grant.getRole() == Role.ADMIN);
	}

	/**
	 * @param platform the platform chosen at login, or null
	 * @return the cookie value
	 */
	public static String cookieValue(long accountId, String platform, String passwordHash, String secret,
	        long expiresEpochSeconds) {
		return VERSION + "." + accountId + "." + encode(platform) + "." + expiresEpochSeconds + "."
		        + mac(accountId, platform, passwordHash, secret, expiresEpochSeconds);
	}

	/** @return the start of the day after {@code now} in the zone, in epoch seconds */
	public static long nextMidnight(Instant now, ZoneId zone) {
		return LocalDate.ofInstant(now, zone).plusDays(1).atStartOfDay(zone).toEpochSecond();
	}

	/** @return the competition time zone when one is set, else the server's */
	public static ZoneId zone(Config config) {
		TimeZone timeZone = config.getTimeZone();
		return timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
	}

	/**
	 * @param cookie    the value of the {@code owlcms-account} cookie, or null
	 * @param allowed   the Administration setting
	 * @param accounts  finds an account by id
	 * @param platforms the names of all platforms
	 * @return the principal of the remembered account, or null when the cookie does not log in
	 */
	public static Principal restore(String cookie, boolean allowed, String secret, Function<Long, UserAccount> accounts,
	        List<String> platforms, long nowEpochSeconds) {
		if (!allowed || secret == null) {
			return null;
		}
		Claim claim = parse(cookie);
		if (claim == null || claim.expires() <= nowEpochSeconds) {
			return null;
		}
		UserAccount account = accounts.apply(claim.accountId());
		if (!isRememberable(account) || !AccessUtils.constantTimeEquals(
		        mac(claim.accountId(), claim.platform(), account.getPasswordHash(), secret, claim.expires()),
		        claim.mac())) {
			return null;
		}
		Principal principal = Principal.of(account, null);
		if (!AccessPolicy.requiresPlatformChoice(principal)) {
			return principal;
		}
		// the platform chosen at login must still be one the account may choose
		String platform = claim.platform();
		return platform != null && AccessPolicy.loginPlatformChoices(principal, platforms).contains(platform)
		        ? principal.withLoginPlatform(platform)
		        : null;
	}

	/** @return the content of a well-formed cookie, or null */
	static Claim parse(String cookie) {
		if (cookie == null) {
			return null;
		}
		String[] parts = cookie.split("\\.", -1);
		if (parts.length != 5 || !VERSION.equals(parts[0])) {
			return null;
		}
		try {
			long accountId = Long.parseLong(parts[1]);
			String platform = NO_PLATFORM.equals(parts[2]) ? null
			        : new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);
			long expires = Long.parseLong(parts[3]);
			return new Claim(accountId, platform, expires, parts[4]);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String encode(String platform) {
		return platform == null || platform.isEmpty() ? NO_PLATFORM
		        : Base64.getUrlEncoder().withoutPadding().encodeToString(platform.getBytes(StandardCharsets.UTF_8));
	}

	private static String mac(long accountId, String platform, String passwordHash, String secret, long expires) {
		return RememberedBrowser.hmac(secret, RememberedBrowser.Kind.ACCOUNT.name() + "\n" + accountId + "\n"
		        + (platform != null ? platform : "") + "\n" + passwordHash + "\n" + expires);
	}
}
