/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import app.owlcms.audit.AuditIntegrity;
import app.owlcms.audit.AuditSigningKey;

/**
 * Protects the forwarding update keys written to the database and to exports.
 *
 * In PIN mode they are written in plain text. In accounts mode they are encrypted with AES-256-GCM, using a key derived
 * from the audit signing key: the configured PEM key, or else the built-in key. The built-in private key is public, so
 * that only hides the value. Values are read whatever the current mode, so changing the mode loses nothing.
 * <p>
 * A change of protection (switching to or from PIN mode, or a new audit key) re-stores every value with
 * {@link #reencrypt(String, AuditSigningKey, AuditSigningKey)}.
 */
public final class ForwardingKeyCipher {

	private static final String ENCRYPTED_PREFIX = "enc:v1:";
	private static final String DERIVATION_LABEL = "OWLCMS forwarding update key encryption v1";
	private static final int NONCE_LENGTH = 12;
	private static final int TAG_BITS = 128;
	private static final Map<String, byte[]> keysBySigningKey = new ConcurrentHashMap<>();
	private static volatile AuditSigningKey builtinKey;

	private ForwardingKeyCipher() {
	}

	/**
	 * @return the value to write; the stored value unchanged while the audit key is not yet loaded at startup
	 * @throws IOException if the stored value cannot be decrypted with the current or the built-in audit key
	 */
	static String forStorage(String stored) throws IOException {
		if (!AuditIntegrity.isInitialized()) {
			return stored;
		}
		return forStorage(stored, Config.getCurrent().isAccountsMode(), currentSigningKey());
	}

	/**
	 * @return the plain value, null if none
	 * @throws IOException if the stored value cannot be decrypted with the current, replaced or built-in audit key
	 */
	static String reveal(String stored) throws IOException {
		if (!AuditIntegrity.isInitialized()) {
			return revealWith(stored);
		}
		return revealWith(stored, currentSigningKey(), AuditIntegrity.current().replacedKey());
	}

	public static String forStorage(String stored, boolean accountsMode, AuditSigningKey signingKey)
	        throws IOException {
		return reencrypt(stored, signingKey, accountsMode ? signingKey : null);
	}

	/**
	 * Re-stores a value for a change of protection.
	 *
	 * @param oldKey the key that protected the value, null if it was plain text
	 * @param newKey the key that must protect it, null for plain text
	 * @return the value to store: plain text when newKey is null, else encrypted with newKey (a value already encrypted
	 *         with newKey is returned unchanged); null if none
	 * @throws IOException if the value is encrypted with neither key nor the built-in key
	 */
	public static String reencrypt(String stored, AuditSigningKey oldKey, AuditSigningKey newKey) throws IOException {
		String plain = revealWith(stored, oldKey, newKey);
		if (plain == null || newKey == null) {
			return plain;
		}
		byte[] key = keyFor(newKey);
		String current = stored.strip();
		if (current.startsWith(ENCRYPTED_PREFIX + fingerprint(key) + ":")) {
			return current;
		}
		return seal(key, plain);
	}

	/**
	 * @param signingKey the audit key in force, null to try only the built-in key
	 */
	public static String reveal(String stored, AuditSigningKey signingKey) throws IOException {
		return revealWith(stored, signingKey);
	}

	/** Tries each non-null key, then the built-in key. */
	private static String revealWith(String stored, AuditSigningKey... keys) throws IOException {
		if (stored == null || stored.isBlank()) {
			return null;
		}
		String value = stored.strip();
		if (!value.startsWith("enc:")) {
			return stored;
		}
		if (!value.startsWith(ENCRYPTED_PREFIX)) {
			throw new IOException("the encrypted value version is unsupported");
		}
		String payload = value.substring(ENCRYPTED_PREFIX.length());
		int separator = payload.indexOf(':');
		if (separator < 0) {
			throw new IOException("the encrypted value is invalid");
		}
		String fingerprint = payload.substring(0, separator);
		byte[] sealed;
		try {
			sealed = Base64.getDecoder().decode(payload.substring(separator + 1));
		} catch (IllegalArgumentException e) {
			throw new IOException("the encrypted value is invalid");
		}
		if (sealed.length <= NONCE_LENGTH) {
			throw new IOException("the encrypted value is invalid");
		}
		List<AuditSigningKey> candidates = new ArrayList<>();
		for (AuditSigningKey key : keys) {
			if (key != null) {
				candidates.add(key);
			}
		}
		candidates.add(builtin());
		for (AuditSigningKey candidate : candidates) {
			byte[] key = keyFor(candidate);
			if (fingerprint.equals(fingerprint(key))) {
				return open(key, sealed);
			}
		}
		throw new IOException("the value was encrypted with another audit key");
	}

	private static AuditSigningKey currentSigningKey() throws IOException {
		AuditSigningKey signingKey = AuditIntegrity.current().signingKey();
		return signingKey != null ? signingKey : builtin();
	}

	private static AuditSigningKey builtin() throws IOException {
		AuditSigningKey key = builtinKey;
		if (key == null) {
			key = AuditSigningKey.builtin();
			builtinKey = key;
		}
		return key;
	}

	private static byte[] keyFor(AuditSigningKey signingKey) throws IOException {
		byte[] key = keysBySigningKey.get(signingKey.fingerprint());
		if (key == null) {
			try {
				key = signingKey.deriveSymmetricKey(DERIVATION_LABEL);
			} catch (GeneralSecurityException e) {
				throw new IOException("the encryption key could not be derived", e);
			}
			keysBySigningKey.put(signingKey.fingerprint(), key);
		}
		return key;
	}

	private static String seal(byte[] key, String plain) throws IOException {
		byte[] nonce = new byte[NONCE_LENGTH];
		new SecureRandom().nextBytes(nonce);
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
			byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] sealed = ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
			return ENCRYPTED_PREFIX + fingerprint(key) + ":" + Base64.getEncoder().encodeToString(sealed);
		} catch (GeneralSecurityException e) {
			throw new IOException("the value could not be encrypted", e);
		}
	}

	private static String open(byte[] key, byte[] sealed) throws IOException {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
			        new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(sealed, 0, NONCE_LENGTH)));
			return new String(cipher.doFinal(sealed, NONCE_LENGTH, sealed.length - NONCE_LENGTH),
			        StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IOException("the encrypted value is invalid");
		}
	}

	private static String fingerprint(byte[] key) throws IOException {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(key);
			return HexFormat.of().formatHex(digest, 0, 4);
		} catch (GeneralSecurityException e) {
			throw new IOException(e);
		}
	}
}
