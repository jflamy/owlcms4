package app.owlcms.monitors.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class TrackerDatabaseChecksumTest {

	@Test
	public void ignoresExportDateAndRandomizedForwardingKeyCiphertext() {
		String first = """
				{"formatVersion":"2.0","exportDate":"2026-10-07T20:00:00Z",
				"config":{"connections":[{"url":"wss://example","updateKey":"enc:v2:first"}]},
				"athletes":[{"id":1,"name":"Athlete"}]}
				""";
		String second = """
				{"formatVersion":"2.0","exportDate":"2026-10-07T20:01:00Z",
				"config":{"connections":[{"url":"wss://example","updateKey":"enc:v2:second"}]},
				"athletes":[{"id":1,"name":"Athlete"}]}
				""";

		assertEquals(checksum(first), checksum(second));
	}

	@Test
	public void changesWhenTrackerRelevantStateChanges() {
		String first = """
				{"formatVersion":"2.0","exportDate":"2026-10-07T20:00:00Z",
				"athletes":[{"id":1,"name":"Athlete","snatch1":100}]}
				""";
		String second = """
				{"formatVersion":"2.0","exportDate":"2026-10-07T20:01:00Z",
				"athletes":[{"id":1,"name":"Athlete","snatch1":101}]}
				""";

		assertNotEquals(checksum(first), checksum(second));
	}

	private static String checksum(String json) {
		return TrackerDatabaseChecksum.compute(json.getBytes(StandardCharsets.UTF_8));
	}
}
