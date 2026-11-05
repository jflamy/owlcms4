package app.owlcms.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Signature trailer of a V2 export: exact byte format, generation and byte-level verification. The document is never
 * parsed; see compliance/145-IMPLEMENTATION_PLAN.md for the wire format.
 */
public final class ExportSignature {
	private static final String ALG = "Ed25519";
	private static final String PURPOSE = "OWLCMS-EXPORT-SIG:1\n";

	private static final int PUBLIC_KEY_LENGTH = 60;
	private static final int HEX_LENGTH = 64;
	private static final int SIGNATURE_LENGTH = 88;

	private static final String PREFIX = ",\"signature\":{\"alg\":\"" + ALG + "\",\"publicKey\":\"";
	private static final String FINGERPRINT_LITERAL = "\",\"keyFingerprint\":\"";
	private static final String SHA256_LITERAL = "\",\"sha256\":\"";
	private static final String VALUE_LITERAL = "\",\"value\":\"";
	private static final String SUFFIX = "\"}}";

	/** Length in bytes of every trailer, computed from the format. */
	public static final int TRAILER_LENGTH = PREFIX.length() + PUBLIC_KEY_LENGTH + FINGERPRINT_LITERAL.length()
			+ HEX_LENGTH + SHA256_LITERAL.length() + HEX_LENGTH + VALUE_LITERAL.length() + SIGNATURE_LENGTH
			+ SUFFIX.length();

	private static final Pattern TRAILER = Pattern.compile(
			",\"signature\":\\{\"alg\":\"Ed25519\",\"publicKey\":\"([A-Za-z0-9+/]{59}=)\","
					+ "\"keyFingerprint\":\"([0-9a-f]{64})\",\"sha256\":\"([0-9a-f]{64})\","
					+ "\"value\":\"([A-Za-z0-9+/]{86}==)\"\\}\\}");

	public enum Status {
		ABSENT, INVALID, VALID
	}

	public enum Reason {
		NONE, CHECKSUM, KEY, SIGNATURE
	}

	public enum KeyMatch {
		NONE, OWN, FOREIGN, BUILTIN
	}

	/** @param sha256 locally computed: of the body if a trailer was recognized, else of the whole file */
	public record Result(Status status, Reason reason, String sha256, long bytes, String fingerprint) {
	}

	private ExportSignature() {
	}

	/**
	 * Like the audit trail, exports are signed with whatever key the server holds. A built-in key signature only shows
	 * that the file is intact: its private part is public, so {@link #match} reports it as {@link KeyMatch#BUILTIN},
	 * never as the server's own.
	 *
	 * @param bodySha256 lowercase hexadecimal SHA-256 of the export body, including its closing brace
	 * @return the trailer, which replaces the closing brace of the body
	 */
	public static byte[] trailer(String bodySha256, AuditSigningKey key) throws GeneralSecurityException {
		AuditBlockHash.requireHash(bodySha256);
		String signature = Base64.getEncoder().encodeToString(key.sign(signedBytes(key.fingerprint(), bodySha256)));
		byte[] trailer = (PREFIX + key.publicKeyBase64() + FINGERPRINT_LITERAL + key.fingerprint() + SHA256_LITERAL
				+ bodySha256 + VALUE_LITERAL + signature + SUFFIX).getBytes(StandardCharsets.US_ASCII);
		if (trailer.length != TRAILER_LENGTH) {
			throw new IllegalStateException("Unexpected export signature trailer length " + trailer.length);
		}
		return trailer;
	}

	/** Never throws: malformed input is reported as absent or invalid. */
	public static Result verify(byte[] file) {
		if (file == null) {
			file = new byte[0];
		}
		long length = file.length;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			Matcher trailer = file.length < TRAILER_LENGTH ? null : TRAILER.matcher(
					new String(file, file.length - TRAILER_LENGTH, TRAILER_LENGTH, StandardCharsets.ISO_8859_1));
			if (trailer == null || !trailer.matches()) {
				digest.update(file);
				return new Result(Status.ABSENT, Reason.NONE, hex(digest), length, null);
			}
			String publicKey = trailer.group(1);
			String fingerprint = trailer.group(2);
			String claimedSha256 = trailer.group(3);
			String value = trailer.group(4);

			digest.update(file, 0, file.length - TRAILER_LENGTH);
			digest.update((byte) '}');
			String sha256 = hex(digest);
			if (!sha256.equals(claimedSha256)) {
				return new Result(Status.INVALID, Reason.CHECKSUM, sha256, length, fingerprint);
			}

			byte[] keyBytes = canonicalBase64(publicKey);
			if (keyBytes == null || !AuditBlockHash.sha256(keyBytes).equals(fingerprint)) {
				return new Result(Status.INVALID, Reason.KEY, sha256, length, fingerprint);
			}
			byte[] signature = canonicalBase64(value);
			if (signature == null || !verifySignature(publicKey, signature, fingerprint, sha256)) {
				return new Result(Status.INVALID, Reason.SIGNATURE, sha256, length, fingerprint);
			}
			return new Result(Status.VALID, Reason.NONE, sha256, length, fingerprint);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	/** @param serverKey the signing key held by AuditIntegrity, or null if it is not initialized */
	public static KeyMatch match(Result result, AuditSigningKey serverKey) {
		if (result == null || result.status() != Status.VALID) {
			return KeyMatch.NONE;
		}
		if (AuditSigningKey.BUILTIN_FINGERPRINT.equals(result.fingerprint())) {
			return KeyMatch.BUILTIN;
		}
		if (serverKey != null && !serverKey.isBuiltin() && serverKey.fingerprint().equals(result.fingerprint())) {
			return KeyMatch.OWN;
		}
		return KeyMatch.FOREIGN;
	}

	private static byte[] signedBytes(String fingerprint, String sha256) {
		String canonical = AuditMarkerCodec.canonicalJson(Map.of("alg", ALG, "keyFingerprint", fingerprint,
				"sha256", sha256));
		return (PURPOSE + canonical).getBytes(StandardCharsets.UTF_8);
	}

	private static boolean verifySignature(String publicKey, byte[] signature, String fingerprint, String sha256) {
		try {
			return AuditPublicKey.fromBase64(publicKey).verify(signedBytes(fingerprint, sha256), signature);
		} catch (IOException | GeneralSecurityException | RuntimeException e) {
			return false;
		}
	}

	/** @return the decoded bytes, or null unless the text is the canonical Base64 encoding of them */
	private static byte[] canonicalBase64(String text) {
		try {
			byte[] decoded = Base64.getDecoder().decode(text);
			return Base64.getEncoder().encodeToString(decoded).equals(text) ? decoded : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String hex(MessageDigest digest) {
		return HexFormat.of().formatHex(digest.digest());
	}
}
