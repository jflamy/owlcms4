/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.monitors.ForwardingDestination;

public class ForwardingDestinationTest {

	private String originalRemoteProperty;
	private String originalUpdateKeyProperty;
	private String originalVideoDataKeyProperty;
	private String originalVideoDataProperty;

	@Before
	public void captureForwardingProperties() {
		this.originalRemoteProperty = System.getProperty("remote");
		this.originalUpdateKeyProperty = System.getProperty("updateKey");
		this.originalVideoDataProperty = System.getProperty("videodata");
		this.originalVideoDataKeyProperty = System.getProperty("videoDataKey");
		System.clearProperty("remote");
		System.clearProperty("updateKey");
		System.clearProperty("videodata");
		System.clearProperty("videoDataKey");
	}

	@After
	public void restoreForwardingProperties() {
		restoreProperty("remote", this.originalRemoteProperty);
		restoreProperty("updateKey", this.originalUpdateKeyProperty);
		restoreProperty("videodata", this.originalVideoDataProperty);
		restoreProperty("videoDataKey", this.originalVideoDataKeyProperty);
	}

	@Test
	public void environmentVideoDataReplacesDatabaseDestinationByDefault() {
		Config config = videoDataConfig("wss://database.example/ws", "database-key");
		System.setProperty("videodata", "wss://tracker.example/ws");
		System.setProperty("videoDataKey", "tracker-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "wss://tracker.example/ws", "tracker-key");
	}

	@Test
	public void trackerExtraAddsDatabaseVideoDataDestination() {
		Config config = videoDataConfig("wss://database.example/ws", "database-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);
		System.setProperty("videodata", "wss://tracker.example/ws");
		System.setProperty("videoDataKey", "tracker-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(2, destinations.size());
		assertDestination(destinations.get(0), "wss://database.example/ws", "database-key");
		assertDestination(destinations.get(1), "wss://tracker.example/ws", "tracker-key");
	}

	@Test
	public void trackerExtraDedupsVideoUrlWithEnvironmentKey() {
		Config config = videoDataConfig("wss://tracker.example/ws/", "database-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);
		System.setProperty("videodata", "wss://tracker.example/ws");
		System.setProperty("videoDataKey", "tracker-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "wss://tracker.example/ws", "tracker-key");
	}

	@Test
	public void trackerExtraCollectsBothDatabaseAndEnvironmentPairs() {
		Config config = videoDataConfig("wss://database-video.example/ws", "database-video-key");
		config.setPublicResultsURL("https://database-results.example");
		config.setUpdatekey("database-results-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);
		System.setProperty("remote", "https://environment-results.example");
		System.setProperty("updateKey", "environment-results-key");
		System.setProperty("videodata", "wss://environment-video.example/ws");
		System.setProperty("videoDataKey", "environment-video-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(4, destinations.size());
		assertDestination(destinations.get(0), "https://database-results.example", "database-results-key");
		assertDestination(destinations.get(1), "wss://database-video.example/ws", "database-video-key");
		assertDestination(destinations.get(2), "https://environment-results.example", "environment-results-key");
		assertDestination(destinations.get(3), "wss://environment-video.example/ws", "environment-video-key");
	}

	@Test
	public void trackerExtraDedupsPublicResultsUrlWithEnvironmentKey() {
		Config config = new Config();
		config.setPublicResultsURL("https://results.example/");
		config.setUpdatekey("database-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);
		System.setProperty("remote", "https://results.example");
		System.setProperty("updateKey", "environment-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "https://results.example", "environment-key");
	}

	@Test
	public void trackerExtraKeepsDatabaseKeyWhenMatchingEnvironmentKeyIsUnset() {
		Config config = new Config();
		config.setPublicResultsURL("https://results.example");
		config.setUpdatekey("database-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);
		System.setProperty("remote", "https://results.example");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "https://results.example", "database-key");
	}

	@Test
	public void trackerExtraNormalizesLegacyEnvironmentRemoteUpdatePath() {
		Config config = new Config();
		config.setPublicResultsURL("https://results.example");
		config.setUpdatekey("database-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);
		System.setProperty("remote", "https://results.example/update");
		System.setProperty("updateKey", "environment-key");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "https://results.example", "environment-key");
	}

	@Test
	public void trackerExtraWithoutEnvironmentOverrideKeepsSingleDatabaseDestination() {
		Config config = videoDataConfig("wss://database.example/ws", "database-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "wss://database.example/ws", "database-key");
	}

	@Test
	public void blankEnvironmentVideoDataStillResurrectsDatabaseDestinationWithTrackerExtra() {
		Config config = videoDataConfig("wss://database.example/ws", "database-key");
		config.setFeatureSwitchValue(FeatureSwitch.TRACKER_EXTRA, true);
		System.setProperty("videodata", "");

		List<ForwardingDestination> destinations = ForwardingDestination.fromConfig(config);

		assertEquals(1, destinations.size());
		assertDestination(destinations.get(0), "wss://database.example/ws", "database-key");
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

	private static Config videoDataConfig(String videoDataUrl, String videoDataKey) {
		Config config = new Config();
		config.setVideoDataURL(videoDataUrl);
		config.setVideoDataKey(videoDataKey);
		return config;
	}
}