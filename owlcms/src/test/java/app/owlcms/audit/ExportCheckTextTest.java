package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

import org.junit.Test;

import app.owlcms.audit.ExportSignature.KeyMatch;
import app.owlcms.audit.ExportSignature.Result;

public class ExportCheckTextTest {

	private static final String BODY = "{\"formatVersion\":\"2.0\",\"name\":\"Fixture\"}";
	private static final Function<String, String> LABEL = key -> "[" + key + "]";

	private static byte[] signed(String body, AuditSigningKey key) throws Exception {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		byte[] trailer = ExportSignature.trailer(AuditBlockHash.sha256(bytes), key);
		byte[] file = Arrays.copyOf(bytes, bytes.length - 1 + trailer.length);
		System.arraycopy(trailer, 0, file, bytes.length - 1, trailer.length);
		return file;
	}

	@Test
	public void unsignedFileShowsItsChecksumOnly() {
		Result result = ExportSignature.verify(BODY.getBytes(StandardCharsets.UTF_8));
		assertEquals("ExportCheck.Unsigned", ExportCheckText.verdictKey(result, KeyMatch.NONE));
		assertFalse(ExportCheckText.compareFingerprint(result, KeyMatch.NONE));
		String details = ExportCheckText.details("export.json", result, LABEL);
		assertTrue(details.contains("SHA-256: " + result.sha256()));
		assertFalse(details.contains("[AuditCheck.Fingerprint]"));
	}

	@Test
	public void ownKeySignatureAsksForTheFingerprintComparison() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		Result result = ExportSignature.verify(signed(BODY, key));
		KeyMatch match = ExportSignature.match(result, key);
		assertEquals("ExportCheck.Signed.OWN", ExportCheckText.verdictKey(result, match));
		assertTrue(ExportCheckText.compareFingerprint(result, match));
		assertTrue(ExportCheckText.details("export.json", result, LABEL)
				.contains("[AuditCheck.Fingerprint]: " + key.displayFingerprint()));
	}

	@Test
	public void builtinKeySignatureIsNotCompared() throws Exception {
		AuditSigningKey builtin = AuditSigningKey.builtin();
		Result result = ExportSignature.verify(signed(BODY, builtin));
		KeyMatch match = ExportSignature.match(result, null);
		assertEquals("ExportCheck.Signed.BUILTIN", ExportCheckText.verdictKey(result, match));
		assertFalse(ExportCheckText.compareFingerprint(result, match));
	}

	@Test
	public void tamperedFileIsInvalid() throws Exception {
		byte[] file = signed(BODY, AuditSigningKey.generate());
		file[5] = 'X';
		Result result = ExportSignature.verify(file);
		assertEquals("ExportCheck.Invalid", ExportCheckText.verdictKey(result, ExportSignature.match(result, null)));
		assertFalse(ExportCheckText.compareFingerprint(result, KeyMatch.NONE));
	}
}
