package app.owlcms.audit;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class AuditPublicKey {
	private final PublicKey key;
	private final String fingerprint;

	private AuditPublicKey(PublicKey key) {
		this.key = key;
		this.fingerprint = AuditBlockHash.sha256(key.getEncoded());
	}

	public static AuditPublicKey fromBase64(String encoded) throws IOException {
		try {
			KeyFactory factory = KeyFactory.getInstance("Ed25519");
			return new AuditPublicKey(factory.generatePublic(
					new X509EncodedKeySpec(Base64.getDecoder().decode(encoded))));
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new IOException("Invalid Ed25519 audit public key", e);
		}
	}

	public String fingerprint() {
		return this.fingerprint;
	}

	public boolean isBuiltin() {
		return AuditSigningKey.BUILTIN_FINGERPRINT.equals(this.fingerprint);
	}

	public boolean verify(byte[] bytes, byte[] signatureBytes) throws GeneralSecurityException {
		Signature signature = Signature.getInstance("Ed25519");
		signature.initVerify(this.key);
		signature.update(bytes);
		return signature.verify(signatureBytes);
	}
}
