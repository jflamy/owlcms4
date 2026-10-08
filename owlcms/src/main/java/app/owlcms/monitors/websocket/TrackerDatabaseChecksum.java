package app.owlcms.monitors.websocket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Cache-busting token sent to the tracker with a database payload. Ignores {@code exportDate} and the encrypted
 * {@code updateKey} values (re-encrypted with a fresh nonce on every serialization) so that it only changes when
 * the competition data does. Distinct from the audited SHA-256 of the exact exported bytes.
 */
public final class TrackerDatabaseChecksum {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TrackerDatabaseChecksum() {
	}

	public static String compute(byte[] jsonBytes) {
		try {
			JsonNode root = MAPPER.readTree(jsonBytes);
			if (root instanceof ObjectNode object) {
				object.remove("exportDate");
			}
			removeUpdateKeys(root);
			return HexFormat.of().formatHex(sha256().digest(MAPPER.writeValueAsBytes(root)));
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to compute tracker database checksum", e);
		}
	}

	private static void removeUpdateKeys(JsonNode node) {
		if (node instanceof ObjectNode object) {
			object.remove("updateKey");
			object.properties().forEach(property -> removeUpdateKeys(property.getValue()));
		} else if (node != null && node.isArray()) {
			node.forEach(TrackerDatabaseChecksum::removeUpdateKeys);
		}
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
