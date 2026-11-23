/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.monitors.MQTTMonitor;

/**
 * Test-only factory for {@link FieldOfPlay} instances. Lives in the test source tree so that the
 * production class is free of test scaffolding.
 */
public final class MockFieldOfPlay {

	private MockFieldOfPlay() {
	}

	/**
	 * Build a {@link FieldOfPlay} suitable for unit tests, wired with mock timers and the test
	 * group "A".
	 */
	public static FieldOfPlay create(List<Athlete> athletes, IProxyTimer timer, IProxyTimer breakTimer) {
		FieldOfPlay m = new FieldOfPlay();
		m.setName("test");
		m.initEventBuses();
		m.setTestingMode(true);
		Group group = GroupRepository.findByName("A");
		m.setGroup(group);
		m.init(athletes, timer, breakTimer, true);
		m.getFopEventBus().register(m);
		return m;
	}

	/**
	 * Build a named {@link FieldOfPlay} with the production timers, which publish the clock and break events the
	 * displays rely on. For tests that run several platforms and observe their UI events.
	 */
	public static FieldOfPlay createWithProductionTimers(String name, Group group, List<Athlete> athletes) {
		FieldOfPlay m = new FieldOfPlay();
		m.setName(name);
		m.initEventBuses();
		m.setTestingMode(true);
		m.setGroup(group);
		m.init(athletes, new ProxyAthleteTimer(m), new ProxyBreakTimer(m), true);
		m.getFopEventBus().register(m);
		return m;
	}

	/** The records the platform currently considers eligible for the athlete. */
	public static List<RecordEvent> eligibleRecords(FieldOfPlay fop, Athlete athlete) {
		return fop.eligibleRecordsByAthlete.getOrDefault(athlete, List.of());
	}

	/**
	 * Build a {@link FieldOfPlay} for a persisted platform with a custom {@link MQTTMonitor},
	 * so tests can observe published MQTT payloads without connecting to a broker.
	 */
	public static FieldOfPlay createWithMqttMonitor(Platform platform,
	        Function<FieldOfPlay, MQTTMonitor> monitorFactory) {
		AtomicReference<MQTTMonitor> monitorRef = new AtomicReference<>();
		FieldOfPlay m = new FieldOfPlay() {
			@Override
			public MQTTMonitor getMqttMonitor() {
				return monitorRef.get();
			}
		};
		m.setName(platform.getName());
		m.setPlatform(platform);
		m.initEventBuses();
		m.setTestingMode(true);
		m.getFopEventBus().register(m);
		monitorRef.set(monitorFactory.apply(m));
		return m;
	}
}
