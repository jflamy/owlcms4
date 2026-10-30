package app.owlcms.access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;

/**
 * The officials PIN, the display PIN and the MQTT password are hashed with PBKDF2, like account passwords. The salted
 * SHA-256 of earlier versions is still accepted, and replaced at the next correct entry where the caller can save it.
 */
public final class PinHashes {

	/** Stored hash to the SHA-256 of a value accepted for it: a display visiting with ?token= is not slowed down. */
	private static final Map<String, String> verified = new ConcurrentHashMap<>();

	private PinHashes() {
	}

	public static String hash(String pin) {
		return PasswordHasher.hash(pin);
	}

	/** Compares in constant time; false if either value is missing. */
	public static boolean matches(String candidate, String stored) {
		if (candidate == null || stored == null || stored.isBlank()) {
			return false;
		}
		String digest = sha256(candidate);
		String known = verified.get(stored);
		if (known != null && AccessUtils.constantTimeEquals(known, digest)) {
			return true;
		}
		boolean ok = PasswordHasher.isHash(stored) ? PasswordHasher.verify(candidate, stored) : legacyMatches(candidate,
		        stored);
		if (ok) {
			verified.put(stored, digest);
		}
		return ok;
	}

	/** @return true if the stored value should be replaced by a new hash at the next correct entry */
	public static boolean isOutdated(String stored) {
		return stored != null && !stored.isBlank() && PasswordHasher.needsRehash(stored);
	}

	private static boolean legacyMatches(String candidate, String stored) {
		try {
			return AccessUtils.constantTimeEquals(stored, Config.getCurrent().encodeUserPassword(candidate, stored));
		} catch (RuntimeException e) {
			// an unreadable older value never matches
			return false;
		}
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of()
			        .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
