/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypts {@code enc:v1:} values produced by the control panel's {@code secret encrypt} command (see controlpanel shared/secret.go).
 */
public final class InstallationSecret {

	private static final String ENCRYPTED_PREFIX = "enc:v1:";
	private static final int NONCE_LENGTH = 12;
	private static final int TAG_BITS = 128;

	private InstallationSecret() {
	}

	/**
	 * @return the value unchanged unless it starts with {@code enc:v1:}, in which case it is decrypted with this computer's installation key
	 * @throws IOException if the value cannot be decrypted on this computer
	 */
	public static String decrypt(String value) throws IOException {
		if (value == null || !value.strip().startsWith(ENCRYPTED_PREFIX)) {
			return value;
		}
		String payload = value.strip().substring(ENCRYPTED_PREFIX.length());
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

		byte[] key = loadInstallationKey();
		if (!fingerprint.equals(fingerprint(key))) {
			throw new IOException("the value was encrypted on another computer; encrypt it again on this one with the control panel");
		}
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
			        new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(sealed, 0, NONCE_LENGTH)));
			byte[] plain = cipher.doFinal(sealed, NONCE_LENGTH, sealed.length - NONCE_LENGTH);
			return new String(plain, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IOException("the encrypted value is invalid");
		}
	}

	// Control panel 3.8.0 stored the key as the file ~/.owlcms; later versions use ~/.owlcms/key.
	private static byte[] loadInstallationKey() throws IOException {
		Path legacyPath = Path.of(System.getProperty("user.home"), ".owlcms");
		Path keyPath = Files.isRegularFile(legacyPath) ? legacyPath : legacyPath.resolve("key");
		String encoded;
		try {
			encoded = Files.readString(keyPath, StandardCharsets.US_ASCII).strip();
		} catch (NoSuchFileException e) {
			throw new IOException("no installation key in " + legacyPath + "; encrypt the value on this computer with the control panel");
		}
		byte[] key;
		try {
			key = Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException e) {
			throw new IOException("invalid installation key in " + keyPath);
		}
		if (key.length != 32) {
			throw new IOException("invalid installation key in " + keyPath);
		}
		return key;
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
