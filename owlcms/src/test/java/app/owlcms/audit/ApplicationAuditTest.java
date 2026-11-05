package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

import app.owlcms.audit.ExportSignature.Result;

public class ApplicationAuditTest {

	private static final String BODY = "{\"formatVersion\":\"2.0\",\"name\":\"Fixture\"}";

	private static byte[] signed(AuditSigningKey key) throws Exception {
		byte[] bytes = BODY.getBytes(StandardCharsets.UTF_8);
		byte[] trailer = ExportSignature.trailer(AuditBlockHash.sha256(bytes), key);
		byte[] file = Arrays.copyOf(bytes, bytes.length - 1 + trailer.length);
		System.arraycopy(trailer, 0, file, bytes.length - 1, trailer.length);
		return file;
	}

	private static String detail(byte[] file, AuditSigningKey serverKey) {
		Result result = ExportSignature.verify(file);
		return ApplicationAudit.importDetail("export.json", result, ExportSignature.match(result, serverKey));
	}

	@Test
	public void unsignedImport() {
		byte[] file = BODY.getBytes(StandardCharsets.UTF_8);
		assertEquals("source=export.json,bytes=" + file.length + ",sha256=" + AuditBlockHash.sha256(file)
				+ ",signature=absent,reason=-,key=-,fingerprint=-", detail(file, null));
	}

	@Test
	public void signedWithTheServerKey() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		byte[] file = signed(key);
		assertEquals("source=export.json,bytes=" + file.length + ",sha256="
				+ AuditBlockHash.sha256(BODY.getBytes(StandardCharsets.UTF_8))
				+ ",signature=valid,reason=-,key=own,fingerprint=" + key.fingerprint(), detail(file, key));
	}

	@Test
	public void signedWithAnotherKey() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		assertTrue(detail(signed(key), AuditSigningKey.generate())
				.endsWith(",signature=valid,reason=-,key=foreign,fingerprint=" + key.fingerprint()));
	}

	@Test
	public void signedWithTheBuiltinKey() throws Exception {
		assertTrue(detail(signed(AuditSigningKey.builtin()), null)
				.endsWith(",signature=valid,reason=-,key=builtin,fingerprint=" + AuditSigningKey.BUILTIN_FINGERPRINT));
	}

	@Test
	public void tamperedImportIsRecordedAsInvalid() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		byte[] file = signed(key);
		file[5] = 'X';
		assertTrue(detail(file, key)
				.endsWith(",signature=invalid,reason=checksum,key=-,fingerprint=" + key.fingerprint()));
	}
}
