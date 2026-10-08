package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

public class AuditSigningKeyTest {
	@Test
	public void builtinFingerprintAndSignatureAreReproducible() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		assertEquals(AuditSigningKey.BUILTIN_FINGERPRINT, key.fingerprint());
		assertTrue(key.isBuiltin());
		assertEquals(64, key.displayFingerprint().replace(" ", "").length());
		byte[] message = "fixture audit block\n".getBytes(StandardCharsets.UTF_8);
		byte[] signature = key.sign(message);
		assertTrue(key.verify(message, signature));
		assertFalse(key.verify("altered\n".getBytes(StandardCharsets.UTF_8), signature));
	}

	@Test
	public void flattenedAndCrLfPemPreserveIdentity() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		assertFalse(key.isBuiltin());
		String pem = key.toPem().replace("\n", "\r\n");
		assertEquals(key.fingerprint(), AuditSigningKey.parsePem(pem).fingerprint());
		String flattened = Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
		assertEquals(key.fingerprint(), AuditSigningKey.fromFlattenedPem(flattened).fingerprint());
	}

	@Test
	public void rejectsMalformedIncompleteDuplicateAndMismatchedPairs() throws Exception {
		AuditSigningKey first = AuditSigningKey.generate();
		AuditSigningKey second = AuditSigningKey.generate();
		String firstPem = first.toPem();
		String mixed = firstPem.substring(0, firstPem.indexOf("-----BEGIN PUBLIC KEY-----"))
				+ second.toPem().substring(second.toPem().indexOf("-----BEGIN PUBLIC KEY-----"));
		assertThrows(IOException.class, () -> AuditSigningKey.parsePem(mixed));
		assertThrows(IOException.class, () -> AuditSigningKey.parsePem("invalid"));
		assertThrows(IOException.class, () -> AuditSigningKey.parsePem(firstPem + firstPem));
		assertThrows(IOException.class, () -> AuditSigningKey.parsePem(
				firstPem.substring(firstPem.indexOf("-----BEGIN PUBLIC KEY-----"))));
		assertThrows(IOException.class, () -> AuditSigningKey.fromFlattenedPem("invalid!"));
	}
}
