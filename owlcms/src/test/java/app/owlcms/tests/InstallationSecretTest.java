package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import app.owlcms.data.config.ForwardingConnection;
import app.owlcms.utils.InstallationSecret;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class InstallationSecretTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private String originalHome;

	@Before
	public void useTemporaryHome() {
		this.originalHome = System.getProperty("user.home");
		System.setProperty("user.home", this.temporaryFolder.getRoot().getAbsolutePath());
	}

	@After
	public void restoreHome() {
		System.setProperty("user.home", this.originalHome);
	}

	@Test
	public void encryptsWithFreshNonceAndPreservesExistingCiphertext() throws Exception {
		String first = InstallationSecret.encrypt("fixture-secret");
		String second = InstallationSecret.encrypt("fixture-secret");

		assertTrue(first.startsWith("enc:v1:"));
		assertFalse(first.contains("fixture-secret"));
		assertNotEquals(first, second);
		assertEquals("fixture-secret", InstallationSecret.decrypt(first));
		assertEquals("fixture-secret", InstallationSecret.decrypt(second));
		assertEquals(first, InstallationSecret.encrypt(first));
		assertTrue(Files.isRegularFile(this.temporaryFolder.getRoot().toPath().resolve(".owlcms/key")));
	}

	@Test
	public void acceptsPlaintextAndEmptyValuesWithoutCreatingAKey() throws Exception {
		assertEquals("plain-key", InstallationSecret.decrypt("plain-key"));
		assertEquals("", InstallationSecret.encrypt(""));
		assertNull(InstallationSecret.encrypt(null));
		assertFalse(Files.exists(this.temporaryFolder.getRoot().toPath().resolve(".owlcms")));
	}

	@Test
	public void usesTheLegacyInstallationKeyLocation() throws Exception {
		Path legacyKey = this.temporaryFolder.getRoot().toPath().resolve(".owlcms");
		String encodedKey = Base64.getEncoder().encodeToString(new byte[32]);
		Files.writeString(legacyKey, encodedKey, StandardCharsets.US_ASCII);

		String encrypted = InstallationSecret.encrypt("legacy-key");

		assertEquals("legacy-key", InstallationSecret.decrypt(encrypted));
		assertEquals(encodedKey, Files.readString(legacyKey, StandardCharsets.US_ASCII));
	}

	@Test
	public void rejectsMalformedUnsupportedForeignAndTamperedCiphertext() throws Exception {
		assertThrows(IOException.class, () -> InstallationSecret.decrypt("enc:v1:broken"));
		assertThrows(IOException.class, () -> InstallationSecret.encrypt("enc:v2:unsupported"));
		String encrypted = InstallationSecret.encrypt("fixture-secret");
		String[] parts = encrypted.split(":", 4);
		String foreign = "enc:v1:" + (parts[2].equals("00000000") ? "11111111" : "00000000") + ":" + parts[3];
		assertThrows(IOException.class, () -> InstallationSecret.decrypt(foreign));
		byte[] sealed = Base64.getDecoder().decode(parts[3]);
		sealed[sealed.length - 1] ^= 1;
		String tampered = "enc:v1:" + parts[2] + ":" + Base64.getEncoder().encodeToString(sealed);
		assertThrows(IOException.class, () -> InstallationSecret.decrypt(tampered));
	}

	@Test
	public void forwardingConnectionExportsOnlyItsKeyAsCiphertext() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String sourceJson = "{\"active\":false,\"controlPanelManaged\":true,"
		        + "\"url\":\"wss://tracker.example/ws\",\"updateKey\":\"clear-key\"}";
		ForwardingConnection connection = mapper.readValue(sourceJson, ForwardingConnection.class);

		assertEquals("clear-key", connection.getUpdateKey());
		JsonNode first = mapper.readTree(mapper.writeValueAsString(connection));
		JsonNode second = mapper.readTree(mapper.writeValueAsString(connection));

		assertEquals("wss://tracker.example/ws", first.get("url").asString());
		assertFalse(first.get("active").asBoolean());
		assertTrue(first.get("controlPanelManaged").asBoolean());
		assertTrue(first.get("updateKey").asString().startsWith("enc:v1:"));
		assertNotEquals(first.get("updateKey").asString(), second.get("updateKey").asString());
		assertEquals("clear-key", connection.getUpdateKey());
	}

	@Test
	public void forwardingConnectionPreservesImportedCiphertext() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String encrypted = InstallationSecret.encrypt("clear-key");
		ForwardingConnection connection = mapper.readValue(
		        "{\"url\":\"https://results.example\",\"updateKey\":\"" + encrypted + "\"}",
		        ForwardingConnection.class);

		JsonNode exported = mapper.readTree(mapper.writeValueAsString(connection));

		assertEquals(encrypted, exported.get("updateKey").asString());
		assertEquals("clear-key", connection.getUpdateKey());
	}
}