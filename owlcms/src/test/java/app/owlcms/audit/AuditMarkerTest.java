package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AuditMarkerTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();
	private static final String FILE = "A_full_2026-10-08T11-12-00.000Z.log";
	private static final Instant TIME = Instant.parse("2026-10-08T11:12:00Z");

	@Test
	public void everyMarkerRoundTripsAndVerifiesWithPublicKeyOnly() throws Exception {
		AuditSigningKey signing = AuditSigningKey.builtin();
		AuditPublicKey publicKey = AuditPublicKey.fromBase64(signing.publicKeyBase64());
		AuditMarker.Identity identity = identity(signing, FILE);
		AuditMarker.Block header = AuditMarker.Block.of(1, 1, bytes("1 | audit.open\n"));
		AuditMarker.Block block = AuditMarker.Block.of(2, 3, bytes("2 | clock.start\n3 | referee.decision\n"));
		List<AuditMarker> markers = List.of(
				new AuditMarker.First(identity, header),
				new AuditMarker.Continue(identity, header, "A_full_2026-10-08T11-00-00.000Z.log", header.sha256()),
				new AuditMarker.Seal(identity, block, header.sha256()),
				new AuditMarker.Final(identity, 3, block.sha256()));
		for (AuditMarker marker : markers) {
			AuditMarkerCodec.Signed signed = AuditMarkerCodec.sign(marker, signing);
			String line = AuditMarkerCodec.render(signed);
			AuditMarkerCodec.Signed parsed = AuditMarkerCodec.parse(line);
			assertEquals(signed, parsed);
			assertTrue(AuditMarkerCodec.verify(parsed, publicKey));
			assertTrue(line.startsWith(marker.type().name() + " "));
		}
		assertTrue(publicKey.isBuiltin());
	}

	@Test
	public void canonicalEncodingSortsUtf16KeysAndUsesMinimalJsonEscapes() {
		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("z", 1L);
		fields.put("a", "é\n\t\u000f\"\\/");
		assertEquals("{\"a\":\"é\\n\\t\\u000f\\\"\\\\/\",\"z\":1}", AuditMarkerCodec.canonicalJson(fields));
		Map<String, Object> unicodeKeys = Map.of("\ue000", "second", "\ud83d\ude00", "first");
		assertEquals("{\"😀\":\"first\",\"\":\"second\"}", AuditMarkerCodec.canonicalJson(unicodeKeys));
		assertThrows(IllegalArgumentException.class,
				() -> AuditMarkerCodec.canonicalJson(Map.of("value", "\ud800")));
		assertThrows(IllegalArgumentException.class,
				() -> AuditMarkerCodec.canonicalJson(Map.of("value", AuditMarker.MAX_SAFE_INTEGER + 1)));
	}

	@Test
	public void builtinSignaturesAreDeterministicAndPurposeSeparated() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditMarker marker = new AuditMarker.First(identity(key, FILE),
				AuditMarker.Block.of(1, 1, bytes("header\n")));
		assertEquals(AuditMarkerCodec.sign(marker, key), AuditMarkerCodec.sign(marker, key));
		assertTrue(new String(AuditMarkerCodec.signedBytes(marker), StandardCharsets.UTF_8)
				.startsWith("OWLCMS-AUDIT-SEAL:1\n{\"applicationVersion\":"));
		byte[] rawSignature = Base64.getDecoder().decode(AuditMarkerCodec.sign(marker, key).signature());
		assertFalse(key.verify(AuditMarkerCodec.canonicalJson(marker.payload()).getBytes(StandardCharsets.UTF_8),
				rawSignature));
	}

	@Test
	public void openSslIndependentlyVerifiesTheSignedFixture() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditMarker marker = new AuditMarker.First(identity(key, FILE),
				AuditMarker.Block.of(1, 1, bytes("1 | audit.open\n")));
		AuditMarkerCodec.Signed signed = AuditMarkerCodec.sign(marker, key);
		Path root = temporary.getRoot().toPath();
		Path publicPem = root.resolve("public.pem");
		Path input = root.resolve("marker.bin");
		Path signature = root.resolve("marker.sig");
		Files.writeString(publicPem, "-----BEGIN PUBLIC KEY-----\n" + key.publicKeyBase64()
				+ "\n-----END PUBLIC KEY-----\n");
		Files.write(input, AuditMarkerCodec.signedBytes(marker));
		Files.write(signature, Base64.getDecoder().decode(signed.signature()));
		assertEquals(0, opensslVerify(publicPem, input, signature));
		byte[] changed = Files.readAllBytes(input);
		changed[0] ^= 1;
		Files.write(input, changed);
		assertTrue(opensslVerify(publicPem, input, signature) != 0);
	}

	@Test
	public void modifiedSignedFieldsAndWrongKeyFailVerification() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditPublicKey verifier = AuditPublicKey.fromBase64(key.publicKeyBase64());
		AuditMarker marker = new AuditMarker.First(identity(key, FILE),
				AuditMarker.Block.of(1, 1, bytes("header\n")));
		AuditMarkerCodec.Signed signed = AuditMarkerCodec.sign(marker, key);
		String line = AuditMarkerCodec.render(signed);
		for (String changed : List.of(
				line.replace("\"runId\":\"run-123\"", "\"runId\":\"run-456\""),
				line.replace(FILE, "A_full_other.log"),
				line.replace("\"stream\":\"A\"", "\"stream\":\"B\""),
				line.replace(TIME.toString(), "2026-10-08T11:12:01Z"),
				line.replace("\"byteCount\":7", "\"byteCount\":8"))) {
			assertFalse(AuditMarkerCodec.verify(AuditMarkerCodec.parse(changed), verifier));
		}
		AuditPublicKey wrong = AuditPublicKey.fromBase64(AuditSigningKey.generate().publicKeyBase64());
		assertFalse(AuditMarkerCodec.verify(signed, wrong));
	}

	@Test
	public void parserRejectsAmbiguousAndInvalidSchemas() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditMarkerCodec.Signed signed = AuditMarkerCodec.sign(new AuditMarker.First(identity(key, FILE),
				AuditMarker.Block.of(1, 1, bytes("header\n"))), key);
		String line = AuditMarkerCodec.render(signed);
		for (String invalid : List.of(
				line.replace("\"version\":1", "\"version\":2"),
				line.replace("\"version\":1", "\"version\":1,\"version\":1"),
				line.replace("\"version\":1", "\"version\":1,\"unexpected\":1"),
				line.replace("\"version\":1", "\"version\":1,\"previousBlockSha256\":\"" + "0".repeat(64) + "\""),
				line.replace("\"byteCount\":7,", ""),
				line.replace("\"byteCount\":7", "\"byteCount\":7.0"),
				line.replace("\"firstLine\":1", "\"firstLine\":2"),
				line.replace("\"stream\":\"A\"", "\"stream\":\"\\ud800\""),
				line.replace("FIRST {", "FINAL {"),
				line.replace(TIME.toString(), "2026-10-08T11:12:00.000Z"),
				line + " {}")) {
			assertThrows(invalid, IOException.class, () -> AuditMarkerCodec.parse(invalid));
		}
	}

	@Test
	public void sha256MatchesKnownVectorAndIncludesLineTerminators() {
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				AuditBlockHash.sha256(bytes("abc")));
		AuditMarker.Block block = AuditMarker.Block.of(1, 1, bytes("header\n"));
		assertTrue(block.matches(bytes("header\n")));
		assertFalse(block.matches(bytes("header\r\n")));
		assertFalse(block.matches(bytes("header")));
		assertFalse(block.matches(bytes("Header\n")));
	}

	@Test
	public void markerConstructorsRejectUnsafeRangesAndFilenames() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditMarker.Identity identity = identity(key, FILE);
		assertThrows(IllegalArgumentException.class,
				() -> new AuditMarker.Block(2, 1, 10, "0".repeat(64)));
		assertThrows(IllegalArgumentException.class,
				() -> new AuditMarker.Block(1, 1, AuditMarker.MAX_SAFE_INTEGER + 1, "0".repeat(64)));
		assertThrows(IllegalArgumentException.class, () -> identity(key, "../file.log"));
		assertThrows(IllegalArgumentException.class, () -> identity(key, "C:file.log"));
		assertThrows(IllegalArgumentException.class, () -> new AuditMarker.First(identity,
				AuditMarker.Block.of(1, 2, bytes("header\n"))));
		assertThrows(IllegalArgumentException.class, () -> new AuditMarker.Continue(identity,
				AuditMarker.Block.of(1, 1, bytes("header\n")), FILE, "0".repeat(64)));
	}

	private static AuditMarker.Identity identity(AuditSigningKey key, String file) {
		return new AuditMarker.Identity("A", "run-123", file, TIME, "69.0.0", key.fingerprint());
	}

	private static byte[] bytes(String value) {
		return value.getBytes(StandardCharsets.UTF_8);
	}

	private static int opensslVerify(Path publicPem, Path input, Path signature) throws Exception {
		Process process;
		try {
			process = new ProcessBuilder("openssl", "pkeyutl", "-verify", "-pubin",
					"-inkey", publicPem.toString(), "-rawin", "-in", input.toString(),
					"-sigfile", signature.toString()).redirectErrorStream(true).start();
		} catch (IOException e) {
			Assume.assumeNoException("OpenSSL is required for the external verification fixture", e);
			throw e;
		}
		if (!process.waitFor(10, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IOException("OpenSSL fixture verification timed out");
		}
		System.out.println(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
		return process.exitValue();
	}
}
