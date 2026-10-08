package app.owlcms.audit;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AuditBlockHash {
	private AuditBlockHash() {
	}

	public static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	static String requireHash(String value) {
		if (value == null || !value.matches("[0-9a-f]{64}")) {
			throw new IllegalArgumentException("Audit hashes must contain 64 lowercase hexadecimal characters");
		}
		return value;
	}
}
