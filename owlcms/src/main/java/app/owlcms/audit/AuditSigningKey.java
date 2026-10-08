package app.owlcms.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AuditSigningKey {
	public static final String BUILTIN_FINGERPRINT =
			"d8a318725d89bf63d698a09cb3c10684ac6e76c05bda32318da8b8d71775dde6";
	private static final String BUILTIN_PRIVATE =
			"MC4CAQAwBQYDK2VwBCIEID8Dj3I0prjYBETJ2hZmUF6tYdOnBWTe7AHg5pKthe9T";
	private static final String BUILTIN_PUBLIC =
			"MCowBQYDK2VwAyEAOWuQImXESgrdLgyNNwOK5l8bzOA4YJKQvCkT1RKvvFE=";
	private static final Pattern PEM_BLOCK = Pattern.compile(
			"-----BEGIN (PRIVATE KEY|PUBLIC KEY)-----\\s*([A-Za-z0-9+/=\\s]+?)\\s*-----END \\1-----");
	private static final byte[] VALIDATION_MESSAGE = "OWLCMS audit key validation v1".getBytes(StandardCharsets.UTF_8);

	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	private final String fingerprint;

	private AuditSigningKey(PrivateKey privateKey, PublicKey publicKey) throws GeneralSecurityException {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.fingerprint = HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded()));
		if (!verify(VALIDATION_MESSAGE, sign(VALIDATION_MESSAGE))) {
			throw new GeneralSecurityException("Audit private and public keys do not match");
		}
	}

	public static AuditSigningKey parsePem(String pem) throws IOException {
		try {
			Matcher matcher = PEM_BLOCK.matcher(pem);
			byte[] privateDer = null;
			byte[] publicDer = null;
			int previousEnd = 0;
			while (matcher.find()) {
				if (!pem.substring(previousEnd, matcher.start()).isBlank()) {
					throw new IOException("Unexpected content in audit PEM");
				}
				byte[] der = Base64.getDecoder().decode(matcher.group(2).replaceAll("\\s", ""));
				if ("PRIVATE KEY".equals(matcher.group(1))) {
					if (privateDer != null) {
						throw new IOException("Duplicate private key in audit PEM");
					}
					privateDer = der;
				} else {
					if (publicDer != null) {
						throw new IOException("Duplicate public key in audit PEM");
					}
					publicDer = der;
				}
				previousEnd = matcher.end();
			}
			if (privateDer == null || publicDer == null || !pem.substring(previousEnd).isBlank()) {
				throw new IOException("Audit PEM must contain exactly one private and one public key block");
			}
			KeyFactory factory = KeyFactory.getInstance("Ed25519");
			return new AuditSigningKey(factory.generatePrivate(new PKCS8EncodedKeySpec(privateDer)),
					factory.generatePublic(new X509EncodedKeySpec(publicDer)));
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new IOException("Invalid Ed25519 audit key pair", e);
		}
	}

	public static AuditSigningKey fromFlattenedPem(String encoded) throws IOException {
		try {
			return parsePem(new String(Base64.getDecoder().decode(encoded.strip()), StandardCharsets.UTF_8));
		} catch (IllegalArgumentException e) {
			throw new IOException("Audit PEM environment value is not valid Base64", e);
		}
	}

	public static AuditSigningKey builtin() throws IOException {
		return parsePem(pemBlock("PRIVATE KEY", BUILTIN_PRIVATE) + pemBlock("PUBLIC KEY", BUILTIN_PUBLIC));
	}

	public static AuditSigningKey generate() throws IOException {
		try {
			KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
			return new AuditSigningKey(pair.getPrivate(), pair.getPublic());
		} catch (GeneralSecurityException e) {
			throw new IOException("Unable to generate Ed25519 audit key", e);
		}
	}

	public String fingerprint() {
		return this.fingerprint;
	}

	public String displayFingerprint() {
		return this.fingerprint.replaceAll("(.{4})(?!$)", "$1 ");
	}

	public boolean isBuiltin() {
		return BUILTIN_FINGERPRINT.equals(this.fingerprint);
	}

	public String publicKeyBase64() {
		return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
	}

	public byte[] sign(byte[] bytes) throws GeneralSecurityException {
		Signature signature = Signature.getInstance("Ed25519");
		signature.initSign(this.privateKey);
		signature.update(bytes);
		return signature.sign();
	}

	public boolean verify(byte[] bytes, byte[] signatureBytes) throws GeneralSecurityException {
		Signature signature = Signature.getInstance("Ed25519");
		signature.initVerify(this.publicKey);
		signature.update(bytes);
		return signature.verify(signatureBytes);
	}

	String toPem() {
		return pemBlock("PRIVATE KEY", Base64.getEncoder().encodeToString(this.privateKey.getEncoded()))
				+ pemBlock("PUBLIC KEY", publicKeyBase64());
	}

	private static String pemBlock(String kind, String encoded) {
		return "-----BEGIN " + kind + "-----\n" + encoded + "\n-----END " + kind + "-----\n";
	}
}
