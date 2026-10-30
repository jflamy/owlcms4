package app.owlcms.data.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import app.owlcms.audit.AuditSigningKey;

public class ForwardingKeyProtectionTest {

	private static Config configWithKeys() {
		Config config = new Config();
		config.setForwardingDestinations(List.of(new ForwardingConnection("wss://tracker.example/ws", "tracker-secret"),
		        new ForwardingConnection("https://results.example/", "results-secret")));
		return config;
	}

	private static List<String> stored(Config config) {
		return config.getForwardingDestinations().stream().map(ForwardingConnection::storedUpdateKey).toList();
	}

	private static void assertProtectedBy(Config config, AuditSigningKey key) throws Exception {
		List<String> values = stored(config);
		assertTrue(values.stream().allMatch(v -> v.startsWith("enc:v1:")));
		assertEquals("tracker-secret", ForwardingKeyCipher.reveal(values.get(0), key));
		assertEquals("results-secret", ForwardingKeyCipher.reveal(values.get(1), key));
	}

	@Test
	public void switchingToAccountsModeEncryptsAndBackToPinDecrypts() throws Exception {
		Config config = configWithKeys();
		AuditSigningKey key = AuditSigningKey.generate();

		assertTrue(config.reencryptForwardingKeys(null, key));
		assertProtectedBy(config, key);
		assertFalse("values already protected are not rewritten", config.reencryptForwardingKeys(key, key));

		assertTrue(config.reencryptForwardingKeys(key, null));
		assertEquals(List.of("tracker-secret", "results-secret"), stored(config));
	}

	@Test
	public void aNewAuditKeyReencryptsTheValues() throws Exception {
		Config config = configWithKeys();
		AuditSigningKey replaced = AuditSigningKey.generate();
		AuditSigningKey key = AuditSigningKey.generate();
		config.reencryptForwardingKeys(null, replaced);

		assertTrue(config.reencryptForwardingKeys(replaced, key));
		assertProtectedBy(config, key);
	}

	@Test
	public void valuesFromAnotherInstallationAreLeftAsTheyAre() throws Exception {
		Config config = configWithKeys();
		AuditSigningKey foreign = AuditSigningKey.generate();
		config.reencryptForwardingKeys(null, foreign);
		List<String> before = stored(config);

		assertFalse(config.reencryptForwardingKeys(AuditSigningKey.generate(), AuditSigningKey.generate()));
		assertEquals(before, stored(config));
	}
}
