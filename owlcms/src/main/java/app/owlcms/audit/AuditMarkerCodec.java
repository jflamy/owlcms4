package app.owlcms.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class AuditMarkerCodec {
	private static final String PURPOSE = "OWLCMS-AUDIT-SEAL:1\n";
	private static final char[] HEX = "0123456789abcdef".toCharArray();
	private static final JsonMapper MAPPER = JsonMapper.builder()
			.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
			.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

	private AuditMarkerCodec() {
	}

	public record Signed(AuditMarker marker, String signature) {
		public Signed {
			if (marker == null || signature == null || Base64.getDecoder().decode(signature).length != 64) {
				throw new IllegalArgumentException("An audit marker requires a 64-byte Ed25519 signature");
			}
		}
	}

	public static Signed sign(AuditMarker marker, AuditSigningKey key) throws GeneralSecurityException {
		if (!key.fingerprint().equals(marker.identity().keyFingerprint())) {
			throw new IllegalArgumentException("Marker fingerprint does not match the signing key");
		}
		return new Signed(marker, Base64.getEncoder().encodeToString(key.sign(signedBytes(marker))));
	}

	public static boolean verify(Signed signed, AuditPublicKey key) throws GeneralSecurityException {
		return key.fingerprint().equals(signed.marker().identity().keyFingerprint())
				&& key.verify(signedBytes(signed.marker()), Base64.getDecoder().decode(signed.signature()));
	}

	public static String render(Signed signed) {
		Map<String, Object> fields = new TreeMap<>(signed.marker().payload());
		fields.put("signature", signed.signature());
		return signed.marker().type().name() + " " + canonicalJson(fields);
	}

	public static byte[] signedBytes(AuditMarker marker) {
		return (PURPOSE + canonicalJson(marker.payload())).getBytes(StandardCharsets.UTF_8);
	}

	// Marker schemas contain only strings and safe integers; no floating-point canonicalization is needed.
	static String canonicalJson(Map<String, Object> fields) {
		StringBuilder json = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<String, Object> entry : new TreeMap<>(fields).entrySet()) {
			if (!first) {
				json.append(',');
			}
			first = false;
			appendString(json, entry.getKey());
			json.append(':');
			if (entry.getValue() instanceof String text) {
				appendString(json, text);
			} else if (entry.getValue() instanceof Long number) {
				appendInteger(json, number);
			} else if (entry.getValue() instanceof Integer number) {
				appendInteger(json, number.longValue());
			} else {
				throw new IllegalArgumentException("Unsupported audit canonical JSON field type");
			}
		}
		return json.append('}').toString();
	}

	public static Signed parse(String line) throws IOException {
		try {
			int separator = line.indexOf(' ');
			if (separator <= 0) {
				throw new IOException("Audit marker prefix is missing");
			}
			AuditMarker.Type type = AuditMarker.Type.valueOf(line.substring(0, separator));
			JsonNode root = MAPPER.readTree(line.substring(separator + 1));
			if (!root.isObject() || !type.name().equals(text(root, "marker"))
					|| number(root, "version") != AuditMarker.VERSION) {
				throw new IOException("Audit marker prefix, type or version is invalid");
			}
			AuditMarker.Identity identity = new AuditMarker.Identity(text(root, "stream"), text(root, "runId"),
					text(root, "artifactName"), Instant.parse(text(root, "sealedAt")),
					text(root, "applicationVersion"), text(root, "keyFingerprint"));
			if (!identity.sealedAt().toString().equals(text(root, "sealedAt"))) {
				throw new IOException("Audit marker sealedAt must use its canonical UTC representation");
			}
			AuditMarker marker = switch (type) {
				case FIRST -> new AuditMarker.First(identity, block(root));
				case CONTINUE -> new AuditMarker.Continue(identity, block(root), text(root, "previousFile"),
						text(root, "previousBlockSha256"));
				case SEAL -> new AuditMarker.Seal(identity, block(root), text(root, "previousBlockSha256"));
				case FINAL -> new AuditMarker.Final(identity, number(root, "recordCount"),
						text(root, "lastBlockSha256"));
			};
			Map<String, Object> expected = new TreeMap<>(marker.payload());
			expected.put("signature", text(root, "signature"));
			Set<String> actual = new HashSet<>();
			root.properties().forEach(entry -> actual.add(entry.getKey()));
			if (!actual.equals(expected.keySet())) {
				throw new IOException("Audit marker has missing or unexpected fields");
			}
			canonicalJson(expected);
			return new Signed(marker, text(root, "signature"));
		} catch (RuntimeException e) {
			throw new IOException("Invalid audit marker", e);
		}
	}

	private static AuditMarker.Block block(JsonNode root) throws IOException {
		return new AuditMarker.Block(number(root, "firstLine"), number(root, "lastLine"),
				number(root, "byteCount"), text(root, "sha256"));
	}

	private static String text(JsonNode root, String field) throws IOException {
		JsonNode value = root.get(field);
		if (value == null || !value.isString()) {
			throw new IOException("Audit marker string field is missing: " + field);
		}
		return value.asString();
	}

	private static long number(JsonNode root, String field) throws IOException {
		JsonNode value = root.get(field);
		if (value == null || !value.isIntegralNumber()) {
			throw new IOException("Audit marker integer field is missing: " + field);
		}
		String encoded = value.toString();
		long number;
		try {
			number = Long.parseLong(encoded);
		} catch (NumberFormatException e) {
			throw new IOException("Audit marker integer is out of range: " + field, e);
		}
		if (number < 0 || number > AuditMarker.MAX_SAFE_INTEGER) {
			throw new IOException("Audit marker integer is not JSON-safe: " + field);
		}
		return number;
	}

	private static void appendInteger(StringBuilder json, long number) {
		if (number < -AuditMarker.MAX_SAFE_INTEGER || number > AuditMarker.MAX_SAFE_INTEGER) {
			throw new IllegalArgumentException("Audit canonical JSON integer is out of range");
		}
		json.append(number);
	}

	private static void appendString(StringBuilder json, String text) {
		json.append('"');
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			switch (ch) {
				case '"' -> json.append("\\\"");
				case '\\' -> json.append("\\\\");
				case '\b' -> json.append("\\b");
				case '\t' -> json.append("\\t");
				case '\n' -> json.append("\\n");
				case '\f' -> json.append("\\f");
				case '\r' -> json.append("\\r");
				default -> {
					if (ch < 0x20) {
						json.append("\\u00").append(HEX[ch >> 4]).append(HEX[ch & 15]);
					} else if (Character.isHighSurrogate(ch)) {
						if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
							throw new IllegalArgumentException("Audit canonical JSON contains an unpaired surrogate");
						}
						json.append(ch).append(text.charAt(++i));
					} else if (Character.isLowSurrogate(ch)) {
						throw new IllegalArgumentException("Audit canonical JSON contains an unpaired surrogate");
					} else {
						json.append(ch);
					}
				}
			}
		}
		json.append('"');
	}
}
