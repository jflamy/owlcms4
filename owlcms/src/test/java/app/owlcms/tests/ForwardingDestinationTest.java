package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.ForwardingConnection;
import app.owlcms.monitors.ForwardingDestination;

public class ForwardingDestinationTest {

	private String originalEnableEventForwardingProperty;
	private String originalControlPanelProperty;
	private String originalLauncherProperty;
	private String originalRemoteProperty;
	private String originalUpdateKeyProperty;
	private String originalVideoDataKeyProperty;
	private String originalVideoDataProperty;

	@Before
	public void captureForwardingProperties() throws Exception {
		this.originalEnableEventForwardingProperty = System.getProperty("enableEventForwarding");
		this.originalControlPanelProperty = System.getProperty("controlpanel");
		this.originalLauncherProperty = System.getProperty("launcher");
		this.originalRemoteProperty = System.getProperty("remote");
		this.originalUpdateKeyProperty = System.getProperty("updateKey");
		this.originalVideoDataProperty = System.getProperty("videodata");
		this.originalVideoDataKeyProperty = System.getProperty("videoDataKey");
		System.clearProperty("enableEventForwarding");
		System.clearProperty("controlpanel");
		System.clearProperty("launcher");
		System.clearProperty("remote");
		System.clearProperty("updateKey");
		System.clearProperty("videodata");
		System.clearProperty("videoDataKey");
	}

	@After
	public void restoreForwardingProperties() {
		restoreProperty("enableEventForwarding", this.originalEnableEventForwardingProperty);
		restoreProperty("controlpanel", this.originalControlPanelProperty);
		restoreProperty("launcher", this.originalLauncherProperty);
		restoreProperty("remote", this.originalRemoteProperty);
		restoreProperty("updateKey", this.originalUpdateKeyProperty);
		restoreProperty("videodata", this.originalVideoDataProperty);
		restoreProperty("videoDataKey", this.originalVideoDataKeyProperty);
	}

	@Test
	public void migratesLegacyPairsIntoDestinations() {
		Config config = new Config();
		config.setPublicResultsURL("https://results.example/");
		config.setUpdatekey("results-key");
		config.setVideoDataURL("wss://tracker.example/ws");
		config.setVideoDataKey("tracker-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(2, destinations.size());
		assertDestination(destinations.get(0), "https://results.example", "results-key");
		assertDestination(destinations.get(1), "wss://tracker.example/ws", "tracker-key");
	}

	@Test
	public void supportsArbitraryPersistedDestinationCount() {
		Config config = configWithConnections(
		        new ForwardingConnection("https://one.example", "one"),
		        new ForwardingConnection("wss://two.example/ws", "two"),
		        new ForwardingConnection("https://three.example", "three"));

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(3, destinations.size());
		assertDestination(destinations.get(2), "https://three.example", "three");
	}

	@Test
	public void environmentDestinationIsAddedByDefault() {
		Config config = configWithConnections(new ForwardingConnection("wss://database.example/ws", "database-key"));
		System.setProperty("videodata", "wss://tracker.example/ws");
		System.setProperty("videoDataKey", "tracker-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(2, destinations.size());
		assertDestination(destinations.get(0), "wss://database.example/ws", "database-key");
		assertDestination(destinations.get(1), "wss://tracker.example/ws", "tracker-key");
	}

	@Test
	public void environmentKeyWinsWhenUrlDuplicatesPersistedConnection() {
		Config config = configWithConnections(new ForwardingConnection("https://results.example/", "database-key"));
		System.setProperty("remote", "https://results.example/update");
		System.setProperty("updateKey", "environment-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "https://results.example", "environment-key");
	}

	@Test
	public void authoritativeEmptyListDoesNotRestoreLegacyValues() {
		Config config = configWithConnections();
		config.setPublicResultsURL("https://legacy.example");
		config.setUpdatekey("legacy-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(0, destinations.size());
	}

	@Test
	public void disabledForwardingReturnsNoDestinations() {
		Config config = configWithConnections(new ForwardingConnection("https://results.example", "key"));
		System.setProperty("enableEventForwarding", "false");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(0, destinations.size());
	}

	@Test
	public void inactivePersistedConnectionIsNotForwarded() {
		ForwardingConnection connection = new ForwardingConnection("https://results.example", "key");
		connection.setActive(false);
		Config config = configWithConnections(connection);

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(0, destinations.size());
	}

	@Test
	public void controlPanelConnectionIsRuntimeOnlyAndFollowsCurrentVideoDataValue() {
		Config config = configWithConnections();
		System.setProperty("launcher", "3.10.0");
		System.setProperty("videodata", "ws://127.0.0.1:8095/ws");

		assertEquals(0, config.getForwardingDestinations().size());
		List<ForwardingConnection> firstConnections = config.getEffectiveForwardingDestinations();
		assertEquals(1, firstConnections.size());
		assertEquals("ws://127.0.0.1:8095/ws", firstConnections.get(0).getUrl());
		assertTrue(firstConnections.get(0).isActive());
		assertTrue(firstConnections.get(0).isControlPanelManaged());

		System.setProperty("videodata", "ws://localhost:8096/ws");
		assertEquals(0, config.getForwardingDestinations().size());
		List<ForwardingConnection> changedConnections = config.getEffectiveForwardingDestinations();
		assertEquals(1, changedConnections.size());
		assertEquals("ws://localhost:8096/ws", changedConnections.get(0).getUrl());
		assertTrue(changedConnections.get(0).isActive());

		System.setProperty("videodata", "");
		assertEquals(0, config.getForwardingDestinations().size());
		assertEquals(0, config.getEffectiveForwardingDestinations().size());
		assertEquals(0, ForwardingDestination.fromConfig(config).size());
	}

	@Test
	public void managedConnectionCanBePausedWithoutPersisting() {
		System.setProperty("launcher", "3.10.0");
		System.setProperty("videodata", "ws://localhost:8096/ws");
		Config config = configWithConnections();
		List<ForwardingConnection> connections = config.getEffectiveForwardingDestinations();
		connections.get(0).setActive(false);
		config.setForwardingDestinations(connections);

		assertEquals(0, config.getForwardingDestinations().size());
		assertFalse(config.getEffectiveForwardingDestinations().get(0).isActive());
		assertEquals(0, ForwardingDestination.fromConfig(config).size());

		Config reloaded = configWithConnections(config.getForwardingDestinations().toArray(ForwardingConnection[]::new));
		assertEquals(1, ForwardingDestination.fromConfig(reloaded).size());

		connections.get(0).setActive(true);
		config.setForwardingDestinations(connections);
		assertEquals(1, ForwardingDestination.fromConfig(config).size());
	}

	@Test
	public void persistedControlPanelManagedConnectionsAreStrippedOnLoadAndImport() throws Exception {
		// Simulate an older H2 database row containing controlPanelManaged entries
		Config fromDb = new Config();
		Field field = Config.class.getDeclaredField("forwardingDestinationsJson");
		field.setAccessible(true);
		field.set(fromDb,
		        "[{\"url\":\"ws://localhost:8096/ws\",\"updateKey\":null,\"active\":false,\"controlPanelManaged\":true},"
		                + "{\"url\":\"ws://127.0.0.1:8096/ws\",\"updateKey\":null,\"active\":true,\"controlPanelManaged\":true},"
		                + "{\"url\":\"wss://cloud.example/ws\",\"updateKey\":\"k\",\"active\":true,\"controlPanelManaged\":false}]");

		List<ForwardingConnection> persistedFromDb = fromDb.getForwardingDestinations();
		assertEquals(1, persistedFromDb.size());
		assertEquals("wss://cloud.example/ws", persistedFromDb.get(0).getUrl());
		assertFalse(persistedFromDb.get(0).isControlPanelManaged());
		assertEquals(1, fromDb.getEffectiveForwardingDestinations().size());

		// Simulate JSON import calling setForwardingDestinations with controlPanelManaged entries
		Config imported = configWithConnections(
		        new ForwardingConnection("ws://localhost:8096/ws", null, true, true),
		        new ForwardingConnection("wss://cloud.example/ws", "k", true, false));
		List<ForwardingConnection> persistedFromImport = imported.getForwardingDestinations();
		assertEquals(1, persistedFromImport.size());
		assertEquals("wss://cloud.example/ws", persistedFromImport.get(0).getUrl());
	}

	@Test
	public void onlyLauncherWithLoopbackMarksVideoDataAsControlPanelManaged() {
		Config config = configWithConnections();
		// Standalone MainWrapper sets OWLCMS_CONTROLPANEL without OWLCMS_LAUNCHER: treat as environment variable
		System.setProperty("controlpanel", "3.10.0");
		System.setProperty("videodata", "ws://localhost:8096/ws");

		List<ForwardingConnection> standalone = config.getEffectiveForwardingDestinations();
		assertEquals(1, standalone.size());
		assertFalse(standalone.get(0).isControlPanelManaged());
		assertTrue(standalone.get(0).isEnvironmentManaged());

		// Real Control Panel launch sets OWLCMS_LAUNCHER
		System.setProperty("launcher", "3.10.0");
		List<ForwardingConnection> controlPanelLocal = config.getEffectiveForwardingDestinations();
		assertEquals(1, controlPanelLocal.size());
		assertTrue(controlPanelLocal.get(0).isControlPanelManaged());
		assertTrue(controlPanelLocal.get(0).isActive());

		// Non-loopback VIDEODATA stays environment-managed even under Control Panel
		System.setProperty("videodata", "wss://tracker.example/ws");
		List<ForwardingConnection> controlPanelRemote = config.getEffectiveForwardingDestinations();
		assertEquals(1, controlPanelRemote.size());
		assertFalse(controlPanelRemote.get(0).isControlPanelManaged());
		assertTrue(controlPanelRemote.get(0).isEnvironmentManaged());
	}

	@Test
	public void environmentVariablesAppearAsManagedRuntimeOnlyAndCanBePaused() {
		Config config = configWithConnections(new ForwardingConnection("https://persisted.example", "db-key"));
		System.setProperty("remote", "https://remote.example/update");
		System.setProperty("updateKey", "remote-key");
		System.setProperty("videodata", "ws://127.0.0.1:8096/ws");
		System.setProperty("videoDataKey", "video-key");

		assertEquals(1, config.getForwardingDestinations().size());
		List<ForwardingConnection> effective = config.getEffectiveForwardingDestinations();
		assertEquals(3, effective.size());
		assertFalse(effective.get(0).isManaged());
		assertTrue(effective.get(1).isEnvironmentManaged());
		assertFalse(effective.get(1).isControlPanelManaged());
		assertEquals("https://remote.example", effective.get(1).getUrl());
		assertTrue(effective.get(2).isEnvironmentManaged());
		assertFalse(effective.get(2).isControlPanelManaged());
		assertEquals("ws://127.0.0.1:8096/ws", effective.get(2).getUrl());

		// Pausing an environment connection suppresses forwarding without persisting it
		effective.get(2).setActive(false);
		config.setForwardingDestinations(effective);
		assertEquals(1, config.getForwardingDestinations().size());
		assertFalse(config.getEffectiveForwardingDestinations().get(2).isActive());
		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);
		assertEquals(2, destinations.size());
		assertDestination(destinations.get(0), "https://persisted.example", "db-key");
		assertDestination(destinations.get(1), "https://remote.example", "remote-key");
	}

	@Test
	public void savingWhileEnvVarShadowsPersistedConnectionKeepsPersistedConnection() {
		Config config = configWithConnections(new ForwardingConnection("https://results.example", "db-key"));
		System.setProperty("remote", "https://results.example/update");
		System.setProperty("updateKey", "env-key");

		List<ForwardingConnection> effective = config.getEffectiveForwardingDestinations();
		assertEquals(1, effective.size());
		assertTrue(effective.get(0).isManaged());
		assertEquals("env-key", effective.get(0).getUpdateKey());

		config.setForwardingDestinations(effective);

		List<ForwardingConnection> persisted = config.getForwardingDestinations();
		assertEquals(1, persisted.size());
		assertEquals("https://results.example", persisted.get(0).getUrl());
		assertEquals("db-key", persisted.get(0).getUpdateKey());
	}

	@Test
	public void persistedConnectionsAreNormalizedAndDeduplicated() {
		Config config = configWithConnections(
		        new ForwardingConnection(" https://results.example/ ", "old-key"),
		        new ForwardingConnection("https://results.example", "new-key"));

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "https://results.example", "new-key");
	}

	private static Config configWithConnections(ForwardingConnection... connections) {
		Config config = new Config();
		config.setForwardingDestinations(List.of(connections));
		return config;
	}

	private static void assertDestination(ForwardingDestination destination, String expectedUrl, String expectedKey) {
		assertEquals(expectedUrl, destination.getBaseUrl());
		assertEquals(expectedKey, destination.getUpdateKey());
	}

	private static void restoreProperty(String name, String originalValue) {
		if (originalValue == null) {
			System.clearProperty(name);
		} else {
			System.setProperty(name, originalValue);
		}
	}
}