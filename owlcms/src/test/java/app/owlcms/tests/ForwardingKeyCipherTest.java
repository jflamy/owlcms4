package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import app.owlcms.audit.AuditSigningKey;
import app.owlcms.data.config.ForwardingKeyCipher;

public class ForwardingKeyCipherTest {

	@Test
	public void pinModeStoresPlainText() throws Exception {
		AuditSigningKey pem = AuditSigningKey.generate();

		assertEquals("tracker-key", ForwardingKeyCipher.forStorage("tracker-key", false, pem));
		assertEquals("tracker-key", ForwardingKeyCipher.reveal("tracker-key", pem));
		assertNull(ForwardingKeyCipher.forStorage(null, false, pem));
		assertNull(ForwardingKeyCipher.forStorage(" ", true, pem));
	}

	@Test
	public void accountsModeEncryptsWithTheConfiguredKey() throws Exception {
		AuditSigningKey pem = AuditSigningKey.generate();

		String stored = ForwardingKeyCipher.forStorage("tracker-key", true, pem);

		assertTrue(stored.startsWith("enc:v1:"));
		assertNotEquals(stored, ForwardingKeyCipher.forStorage("tracker-key", true, pem));
		assertEquals(stored, ForwardingKeyCipher.forStorage(stored, true, pem));
		assertEquals("tracker-key", ForwardingKeyCipher.reveal(stored, pem));
		assertThrows(IOException.class, () -> ForwardingKeyCipher.reveal(stored, AuditSigningKey.generate()));
		assertThrows(IOException.class, () -> ForwardingKeyCipher.reveal(stored, null));
	}

	@Test
	public void accountsModeWithoutPemHidesTheValueWithTheBuiltinKey() throws Exception {
		AuditSigningKey builtin = AuditSigningKey.builtin();

		String stored = ForwardingKeyCipher.forStorage("tracker-key", true, builtin);

		assertTrue(stored.startsWith("enc:v1:"));
		assertEquals("tracker-key", ForwardingKeyCipher.reveal(stored, null));
		assertEquals("tracker-key", ForwardingKeyCipher.reveal(stored, AuditSigningKey.generate()));
	}

	@Test
	public void switchingModesKeepsTheKey() throws Exception {
		AuditSigningKey pem = AuditSigningKey.generate();
		String encrypted = ForwardingKeyCipher.forStorage("tracker-key", true, pem);
		String hidden = ForwardingKeyCipher.forStorage("tracker-key", true, AuditSigningKey.builtin());

		assertEquals("tracker-key", ForwardingKeyCipher.forStorage(encrypted, false, pem));
		String reencrypted = ForwardingKeyCipher.forStorage(hidden, true, pem);
		assertNotEquals(hidden, reencrypted);
		assertEquals("tracker-key", ForwardingKeyCipher.reveal(reencrypted, pem));
	}

	@Test
	public void reencryptHandlesEveryChangeOfProtection() throws Exception {
		AuditSigningKey oldKey = AuditSigningKey.generate();
		AuditSigningKey newKey = AuditSigningKey.generate();

		String encrypted = ForwardingKeyCipher.reencrypt("tracker-key", null, oldKey);
		assertTrue("PIN to accounts encrypts", encrypted.startsWith("enc:v1:"));
		assertEquals("tracker-key", ForwardingKeyCipher.reveal(encrypted, oldKey));
		assertEquals("already protected: unchanged", encrypted, ForwardingKeyCipher.reencrypt(encrypted, oldKey, oldKey));

		assertEquals("accounts to PIN decrypts", "tracker-key", ForwardingKeyCipher.reencrypt(encrypted, oldKey, null));

		String rotated = ForwardingKeyCipher.reencrypt(encrypted, oldKey, newKey);
		assertEquals("a new audit key re-encrypts", "tracker-key", ForwardingKeyCipher.reveal(rotated, newKey));
		assertThrows(IOException.class, () -> ForwardingKeyCipher.reveal(rotated, oldKey));

		assertEquals("plain stays plain", "tracker-key", ForwardingKeyCipher.reencrypt("tracker-key", null, null));
		assertNull(ForwardingKeyCipher.reencrypt(null, oldKey, newKey));
		assertThrows("a foreign key cannot be re-encrypted", IOException.class,
		        () -> ForwardingKeyCipher.reencrypt(encrypted, AuditSigningKey.generate(), newKey));
	}

	@Test
	public void rejectsMalformedUnsupportedAndTamperedValues() throws Exception {
		AuditSigningKey pem = AuditSigningKey.generate();
		String stored = ForwardingKeyCipher.forStorage("tracker-key", true, pem);
		char last = stored.charAt(stored.length() - 3);
		String tampered = stored.substring(0, stored.length() - 3) + (last == 'A' ? 'B' : 'A')
		        + stored.substring(stored.length() - 2);

		assertThrows(IOException.class, () -> ForwardingKeyCipher.reveal("enc:v1:broken", pem));
		assertThrows(IOException.class, () -> ForwardingKeyCipher.reveal("enc:v2:abcd:AAAA", pem));
		assertThrows(IOException.class, () -> ForwardingKeyCipher.reveal(tampered, pem));
	}
}
