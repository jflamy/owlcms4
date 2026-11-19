package app.owlcms.access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;

import org.slf4j.LoggerFactory;

import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.access.RememberedLogin;
import app.owlcms.data.access.RememberedLoginRepository;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import ch.qos.logback.classic.Logger;

/**
 * Server-side remembered logins. The cookie holds a selector and a validator; only hashes of the validator are in
 * the database.
 */
public final class RememberedLogins {

	public static final Integer PIN_DAYS = 1;
	public static final int ACCOUNT_DAYS = 1;
	public static final int MAX_PIN_BROWSERS = 200;
	public static final int MAX_ACCOUNT_BROWSERS = 20;
	static final String VERSION = "r1";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Logger logger = (Logger) LoggerFactory.getLogger(RememberedLogins.class);

	public record Issued(String cookieValue, RememberedLogin login) {
	}

	public record Found(RememberedLogin login, UserAccount account) {
	}

	public record Lookup(Found found, boolean deleteCookie) {
	}

	private record Presented(String selector, String validator) {
	}

	private RememberedLogins() {
	}

	/**
	 * @return the cookie and row created for this login
	 */
	public static Issued issue(Kind kind, UserAccount account, String platform, String credentialBasis, Instant now,
	        ZoneId zone, String clientIp, String userAgent) {
		RememberedLogin login = new RememberedLogin();
		String selector = random(16);
		String validator = random(32);
		int days = kind == Kind.ACCOUNT ? ACCOUNT_DAYS : PIN_DAYS;
		Instant expires = expiry(now, zone, days);
		login.setSelector(selector);
		login.setValidatorHash(sha256(validator));
		login.setCredentialCheck(sha256(validator + "\n" + credentialBasis));
		login.setKind(kind);
		login.setAccountId(account != null ? account.getId() : null);
		login.setLoginPlatform(platform);
		login.setIssuedAt(now);
		login.setExpiresAt(expires);
		login.setLastUsedAt(now);
		login.setClientIp(clientIp);
		login.setUserAgent(userAgent(userAgent));
		RememberedLoginRepository.deleteExpired(now);
		RememberedLoginRepository.save(login);
		RememberedLoginRepository.evictBeyond(kind, login.getAccountId(),
		        kind == Kind.ACCOUNT ? MAX_ACCOUNT_BROWSERS : MAX_PIN_BROWSERS, login.getSelector());
		return new Issued(VERSION + "." + selector + "." + validator, login);
	}

	/**
	 * @param credentialBasis current PIN basis, or null for an account whose password hash is read below
	 */
	public static Lookup lookup(String cookieValue, Kind kind, String credentialBasis, Config config, Instant now,
	        String clientIp, String userAgent) {
		if (!config.allowsRememberedLogins()) {
			return new Lookup(null, false);
		}
		Presented presented = parse(cookieValue);
		if (presented == null) {
			return new Lookup(null, cookieValue != null);
		}
		RememberedLogin login = RememberedLoginRepository.find(presented.selector());
		if (login == null) {
			return new Lookup(null, true);
		}
		if (!AccessUtils.constantTimeEquals(login.getValidatorHash(), sha256(presented.validator()))
		        || login.getKind() != kind) {
			RememberedLoginRepository.delete(login.getSelector());
			logger./**/warn("remembered {} login refused: validator mismatch", kind);
			AuthAudit.rememberMismatch(clientIp, kind.name());
			return new Lookup(null, true);
		}
		if (!login.getExpiresAt().isAfter(now)) {
			RememberedLoginRepository.delete(login.getSelector());
			return new Lookup(null, true);
		}

		UserAccount account = null;
		String currentBasis = credentialBasis;
		if (kind == Kind.ACCOUNT) {
			account = UserAccountRepository.findById(login.getAccountId());
			if (!RememberedAccount.isRememberable(account)) {
				RememberedLoginRepository.delete(login.getSelector());
				return new Lookup(null, true);
			}
			currentBasis = account.getPasswordHash();
		}
		if (currentBasis == null || !AccessUtils.constantTimeEquals(login.getCredentialCheck(),
		        sha256(presented.validator() + "\n" + currentBasis))) {
			RememberedLoginRepository.delete(login.getSelector());
			return new Lookup(null, true);
		}

		RememberedLogin current = RememberedLoginRepository.updateLastUse(login.getSelector(), now, clientIp,
		        userAgent(userAgent));
		return current == null ? new Lookup(null, true) : new Lookup(new Found(current, account), false);
	}

	/** @return true if the cookie matched and its row was deleted */
	public static boolean forget(String cookieValue, Kind kind) {
		Presented presented = parse(cookieValue);
		if (presented == null) {
			return false;
		}
		RememberedLogin login = RememberedLoginRepository.find(presented.selector());
		if (login == null || login.getKind() != kind
		        || !AccessUtils.constantTimeEquals(login.getValidatorHash(), sha256(presented.validator()))) {
			return false;
		}
		RememberedLoginRepository.delete(login.getSelector());
		return true;
	}

	public static int forgetAll(AuditOrigin origin) {
		int count = RememberedLoginRepository.deleteAll();
		AuthAudit.rememberedBrowsersForgotten(origin, count);
		return count;
	}

	static Instant expiry(Instant now, ZoneId zone, int days) {
		return LocalDate.ofInstant(now, zone).plusDays(days).atStartOfDay(zone).toInstant();
	}

	static String sha256(String value) {
		try {
			return Base64.getUrlEncoder().withoutPadding().encodeToString(
			        MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Presented parse(String cookieValue) {
		if (cookieValue == null) {
			return null;
		}
		String[] parts = cookieValue.split("\\.", -1);
		return parts.length == 3 && VERSION.equals(parts[0]) && !parts[1].isEmpty() && !parts[2].isEmpty()
		        ? new Presented(parts[1], parts[2])
		        : null;
	}

	private static String random(int bytes) {
		byte[] value = new byte[bytes];
		RANDOM.nextBytes(value);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private static String userAgent(String value) {
		return value == null ? null : value.substring(0, Math.min(value.length(), 512));
	}
}
