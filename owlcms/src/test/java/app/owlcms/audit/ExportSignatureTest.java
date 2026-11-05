package app.owlcms.audit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import app.owlcms.audit.ExportSignature.KeyMatch;
import app.owlcms.audit.ExportSignature.Reason;
import app.owlcms.audit.ExportSignature.Result;
import app.owlcms.audit.ExportSignature.Status;

public class ExportSignatureTest {
	private static final String BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private static final String BODY = "{\"name\":\"Fixture\",\"athletes\":[1,2,3]}";

	private static byte[] utf8(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}

	private static byte[] concat(byte[] first, byte[] second) {
		byte[] all = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, all, first.length, second.length);
		return all;
	}

	private static byte[] signed(String body, AuditSigningKey key) throws Exception {
		byte[] bytes = utf8(body);
		byte[] withoutBrace = Arrays.copyOf(bytes, bytes.length - 1);
		return concat(withoutBrace, ExportSignature.trailer(AuditBlockHash.sha256(bytes), key));
	}

	private static String text(byte[] file) {
		return new String(file, StandardCharsets.UTF_8);
	}

	private static String field(String file, String name) {
		String marker = "\"" + name + "\":\"";
		int start = file.lastIndexOf(marker) + marker.length();
		return file.substring(start, file.indexOf('"', start));
	}

	private static String replaceField(String file, String name, String oldValue, String newValue) {
		String marker = "\"" + name + "\":\"" + oldValue + "\"";
		int at = file.lastIndexOf(marker);
		return file.substring(0, at) + "\"" + name + "\":\"" + newValue + "\"" + file.substring(at + marker.length());
	}

	private static char flipLowBit(char base64Char) {
		return BASE64.charAt(BASE64.indexOf(base64Char) ^ 1);
	}

	private static void assertNeverValid(byte[] file) {
		assertNotEquals(Status.VALID, ExportSignature.verify(file).status());
	}

	@Test
	public void trailerLengthIs365() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		assertEquals(365, ExportSignature.TRAILER_LENGTH);
		assertEquals(365, ExportSignature.trailer(AuditBlockHash.sha256(utf8(BODY)), key).length);
	}

	@Test
	public void trailerFormatIsExact() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		String sha = AuditBlockHash.sha256(utf8(BODY));
		String trailer = new String(ExportSignature.trailer(sha, key), StandardCharsets.US_ASCII);
		assertEquals(",\"signature\":{\"alg\":\"Ed25519\",\"publicKey\":\"" + key.publicKeyBase64()
				+ "\",\"keyFingerprint\":\"" + key.fingerprint() + "\",\"sha256\":\"" + sha + "\",\"value\":\""
				+ field(trailer, "value") + "\"}}", trailer);
	}

	@Test
	public void roundTripIsValid() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		byte[] file = signed(BODY, key);
		Result result = ExportSignature.verify(file);
		assertEquals(Status.VALID, result.status());
		assertEquals(Reason.NONE, result.reason());
		assertEquals(AuditBlockHash.sha256(utf8(BODY)), result.sha256());
		assertEquals(file.length, result.bytes());
		assertEquals(key.fingerprint(), result.fingerprint());
	}

	@Test
	public void unsignedBodyIsAbsentWithWholeFileChecksum() {
		Result result = ExportSignature.verify(utf8(BODY));
		assertEquals(Status.ABSENT, result.status());
		assertEquals(Reason.NONE, result.reason());
		assertEquals(AuditBlockHash.sha256(utf8(BODY)), result.sha256());
		assertEquals(utf8(BODY).length, result.bytes());
		assertNull(result.fingerprint());
	}

	@Test
	public void editedBodyByteFailsChecksum() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		byte[] file = signed(BODY, key);
		file[BODY.indexOf("Fixture")] = 'X';
		Result result = ExportSignature.verify(file);
		assertEquals(Status.INVALID, result.status());
		assertEquals(Reason.CHECKSUM, result.reason());
		assertEquals(key.fingerprint(), result.fingerprint());
		assertEquals(AuditBlockHash.sha256(Arrays.copyOf(file, file.length - ExportSignature.TRAILER_LENGTH + 1)
				.length == 0 ? new byte[0] : concat(Arrays.copyOf(file, file.length - ExportSignature.TRAILER_LENGTH),
						utf8("}"))), result.sha256());
	}

	@Test
	public void editedBodyWithRecomputedChecksumFailsSignature() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		byte[] file = signed(BODY, key);
		String edited = BODY.replace("Fixture", "Fixtur2");
		String claimed = field(text(file), "sha256");
		String forged = text(Arrays.copyOf(file, file.length)).replace(BODY.substring(0, BODY.length() - 1),
				edited.substring(0, edited.length() - 1));
		forged = replaceField(forged, "sha256", claimed, AuditBlockHash.sha256(utf8(edited)));
		Result result = ExportSignature.verify(utf8(forged));
		assertEquals(Status.INVALID, result.status());
		assertEquals(Reason.SIGNATURE, result.reason());
		assertEquals(AuditBlockHash.sha256(utf8(edited)), result.sha256());
	}

	@Test
	public void resignedWithAnotherKeyIsValidButForeign() throws Exception {
		AuditSigningKey own = AuditSigningKey.generate();
		AuditSigningKey other = AuditSigningKey.generate();
		byte[] file = signed(BODY.replace("Fixture", "Fixtur2"), other);
		Result result = ExportSignature.verify(file);
		assertEquals(Status.VALID, result.status());
		assertEquals(other.fingerprint(), result.fingerprint());
		assertEquals(KeyMatch.FOREIGN, ExportSignature.match(result, own));
		assertEquals(KeyMatch.FOREIGN, ExportSignature.match(result, null));
	}

	@Test
	public void matchReportsOwnKey() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		Result result = ExportSignature.verify(signed(BODY, key));
		assertEquals(KeyMatch.OWN, ExportSignature.match(result, key));
	}

	@Test
	public void builtinKeySignatureVerifiesButNeverMatchesOwn() throws Exception {
		AuditSigningKey builtin = AuditSigningKey.builtin();
		Result result = ExportSignature.verify(signed(BODY, builtin));
		assertEquals(Status.VALID, result.status());
		assertEquals(KeyMatch.BUILTIN, ExportSignature.match(result, builtin));
		assertEquals(KeyMatch.BUILTIN, ExportSignature.match(result, AuditSigningKey.generate()));
		assertEquals(KeyMatch.BUILTIN, ExportSignature.match(result, null));
	}

	@Test
	public void matchIsNoneUnlessValid() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		assertEquals(KeyMatch.NONE, ExportSignature.match(ExportSignature.verify(utf8(BODY)), key));
		byte[] file = signed(BODY, key);
		file[1] = 'X';
		assertEquals(KeyMatch.NONE, ExportSignature.match(ExportSignature.verify(file), key));
		assertEquals(KeyMatch.NONE, ExportSignature.match(null, key));
	}

	@Test
	public void swappedPublicKeyFailsKey() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		AuditSigningKey other = AuditSigningKey.generate();
		String file = text(signed(BODY, key));
		String swapped = replaceField(file, "publicKey", key.publicKeyBase64(), other.publicKeyBase64());
		Result result = ExportSignature.verify(utf8(swapped));
		assertEquals(Status.INVALID, result.status());
		assertEquals(Reason.KEY, result.reason());
		assertEquals(key.fingerprint(), result.fingerprint());
	}

	@Test
	public void nonCanonicalBase64IsNotValid() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		String file = text(signed(BODY, key));

		String publicKey = key.publicKeyBase64();
		char last = publicKey.charAt(publicKey.length() - 2);
		String loosePublicKey = publicKey.substring(0, publicKey.length() - 2) + flipLowBit(last) + "=";
		Result keyResult = ExportSignature.verify(utf8(replaceField(file, "publicKey", publicKey, loosePublicKey)));
		assertEquals(Status.INVALID, keyResult.status());
		assertEquals(Reason.KEY, keyResult.reason());

		String value = field(file, "value");
		char lastValue = value.charAt(value.length() - 3);
		String looseValue = value.substring(0, value.length() - 3) + flipLowBit(lastValue) + "==";
		Result valueResult = ExportSignature.verify(utf8(replaceField(file, "value", value, looseValue)));
		assertEquals(Status.INVALID, valueResult.status());
		assertEquals(Reason.SIGNATURE, valueResult.reason());
	}

	@Test
	public void alteredTrailerShapeIsNeverValid() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		byte[] file = signed(BODY, key);
		String text = text(file);

		assertNeverValid(concat(file, utf8("\n")));
		assertNeverValid(concat(file, utf8("x")));
		assertNeverValid(concat(file, ExportSignature.trailer(AuditBlockHash.sha256(utf8(BODY)), key)));
		assertNeverValid(utf8(text.replace("\"alg\":\"Ed25519\",", "\"alg\":\"Ed25519\",\"extra\":\"1\",")));
		assertNeverValid(utf8(text.replace("\"alg\":\"Ed25519\"}}", "\"alg\":\"Ed25519\"}}")
				.replace(",\"signature\":{", ", \"signature\":{")));
		assertNeverValid(utf8(text.replace("\"alg\":", "\"alg\": ")));
		assertNeverValid(utf8(text.replace("\"sha256\":", "\"value\":\"x\",\"sha256\":")));
		assertNeverValid(Arrays.copyOf(file, file.length - 1));
		assertNeverValid(Arrays.copyOfRange(file, 1, file.length));
		assertNeverValid(utf8(text.substring(0, text.length() - 2) + "}"));
		assertNeverValid(utf8(text.replace("\"alg\":\"Ed25519\"", "\"alg\":\"Ed25518\"")));
	}

	@Test
	public void trailerLookalikeInsideBodyDoesNotConfuseVerification() throws Exception {
		AuditSigningKey key = AuditSigningKey.generate();
		AuditSigningKey other = AuditSigningKey.generate();
		String fake = new String(ExportSignature.trailer(AuditBlockHash.sha256(utf8("{}")), other),
				StandardCharsets.US_ASCII).replace("\"", "\\\"");
		String body = "{\"note\":\"" + fake + "\",\"a\":1}";
		byte[] file = signed(body, key);
		Result result = ExportSignature.verify(file);
		assertEquals(Status.VALID, result.status());
		assertEquals(key.fingerprint(), result.fingerprint());
		assertEquals(AuditBlockHash.sha256(utf8(body)), result.sha256());
		assertEquals(KeyMatch.OWN, ExportSignature.match(result, key));
	}

	@Test
	public void garbageAndShortInputsAreAbsentWithoutException() {
		byte[] random = new byte[1000];
		new Random(145).nextBytes(random);
		byte[] justShort = new byte[ExportSignature.TRAILER_LENGTH - 1];
		Arrays.fill(justShort, (byte) ',');
		byte[] justLong = new byte[ExportSignature.TRAILER_LENGTH];
		Arrays.fill(justLong, (byte) ',');
		for (byte[] input : new byte[][] { random, new byte[0], justShort, justLong, null }) {
			Result result = ExportSignature.verify(input);
			assertEquals(Status.ABSENT, result.status());
			assertEquals(Reason.NONE, result.reason());
			assertNull(result.fingerprint());
			assertEquals(input == null ? 0 : input.length, result.bytes());
		}
		assertEquals(AuditBlockHash.sha256(new byte[0]), ExportSignature.verify(new byte[0]).sha256());
		assertArrayEquals(new byte[0], new byte[0]);
	}
}
