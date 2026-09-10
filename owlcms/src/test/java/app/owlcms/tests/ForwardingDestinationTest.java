package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
	public void captureForwardingProperties() {
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
	public void controlPanelConnectionsAreRememberedAndFollowCurrentVideoDataValue() {
		Config config = configWithConnections();
		System.setProperty("controlpanel", "test");
		System.setProperty("videodata", "ws://127.0.0.1:8095/ws");

		List<ForwardingConnection> firstConnections = config.getForwardingDestinations();

		assertEquals(1, firstConnections.size());
		assertTrue(firstConnections.get(0).isActive());
		assertTrue(firstConnections.get(0).isControlPanelManaged());

		System.setProperty("videodata", "ws://localhost:8096/ws");
		List<ForwardingConnection> changedConnections = config.getForwardingDestinations();

		assertEquals(2, changedConnections.size());
		assertFalse(changedConnections.get(0).isActive());
		assertTrue(changedConnections.get(1).isActive());

		System.setProperty("videodata", "");
		List<ForwardingConnection> disabledConnections = config.getForwardingDestinations();

		assertEquals(2, disabledConnections.size());
		assertFalse(disabledConnections.get(0).isActive());
		assertFalse(disabledConnections.get(1).isActive());
		assertEquals(0, ForwardingDestination.fromConfig(config).size());
	}

	@Test
	public void managedConnectionCanBePausedWithoutPersistingActivity() {
		System.setProperty("controlpanel", "test");
		System.setProperty("videodata", "ws://localhost:8096/ws");
		Config config = configWithConnections();
		List<ForwardingConnection> connections = config.getEffectiveForwardingDestinations();
		connections.get(0).setActive(false);
		config.setForwardingDestinations(connections);

		assertFalse(config.getEffectiveForwardingDestinations().get(0).isActive());
		assertTrue(config.getForwardingDestinations().get(0).isActive());
		assertEquals(0, ForwardingDestination.fromConfig(config).size());

		Config reloaded = configWithConnections(config.getForwardingDestinations().toArray(ForwardingConnection[]::new));
		assertEquals(1, ForwardingDestination.fromConfig(reloaded).size());

		connections.get(0).setActive(true);
		config.setForwardingDestinations(connections);
		assertEquals(1, ForwardingDestination.fromConfig(config).size());
	}

	@Test
	public void managedConnectionsCannotBeRemovedFromEditedList() {
		System.setProperty("controlpanel", "test");
		System.setProperty("videodata", "ws://localhost:8096/ws");
		Config config = configWithConnections();
		System.setProperty("videodata", "");

		config.setForwardingDestinations(List.of());

		assertEquals(1, config.getForwardingDestinations().size());
		assertTrue(config.getForwardingDestinations().get(0).isControlPanelManaged());
	}

	@Test
	public void launcherFallbackMarksLocalVideoDataAsControlPanelManaged() {
		Config config = configWithConnections();
		System.setProperty("launcher", "fake-version");
		System.setProperty("videodata", "ws://localhost:8096/ws");

		List<ForwardingConnection> connections = config.getForwardingDestinations();

		assertEquals(1, connections.size());
		assertTrue(connections.get(0).isControlPanelManaged());
		assertTrue(connections.get(0).isActive());
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