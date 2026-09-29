package app.owlcms.access;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Account password hashing: {@code pbkdf2-sha256$<iterations>$<b64salt>$<b64hash>}. */
public final class PasswordHasher {

	public static final int MIN_LENGTH = 8;

	// stored in each hash, so it can be raised later without invalidating existing accounts
	private static final int ITERATIONS = 310_000;
	private static final int SALT_BYTES = 16;
	private static final int KEY_BYTES = 32;
	private static final String PREFIX = "pbkdf2-sha256";
	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordHasher() {
	}

	public static boolean isAcceptable(String password) {
		return password != null && password.length() >= MIN_LENGTH;
	}

	public static String hash(String password) {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		byte[] key = derive(password, salt, ITERATIONS);
		Base64.Encoder encoder = Base64.getEncoder();
		return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(key);
	}

	/** @return false for a wrong password, a missing hash or a malformed hash */
	public static boolean verify(String password, String stored) {
		if (password == null || stored == null) {
			return false;
		}
		String[] parts = stored.split("\\$");
		if (parts.length != 4 || !PREFIX.equals(parts[0])) {
			return false;
		}
		try {
			int iterations = Integer.parseInt(parts[1]);
			if (iterations < 1) {
				return false;
			}
			byte[] salt = Base64.getDecoder().decode(parts[2]);
			byte[] expected = Base64.getDecoder().decode(parts[3]);
			return MessageDigest.isEqual(expected, derive(password, salt, iterations, expected.length));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	static int iterationsOf(String stored) {
		return Integer.parseInt(stored.split("\\$")[1]);
	}

	private static byte[] derive(String password, byte[] salt, int iterations) {
		return derive(password, salt, iterations, KEY_BYTES);
	}

	private static byte[] derive(String password, byte[] salt, int iterations, int keyBytes) {
		String normalized = Normalizer.normalize(password, Normalizer.Form.NFC);
		PBEKeySpec spec = new PBEKeySpec(normalized.toCharArray(), salt, iterations, keyBytes * 8);
		try {
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("PBKDF2 unavailable", e);
		} finally {
			spec.clearPassword();
		}
	}

}
