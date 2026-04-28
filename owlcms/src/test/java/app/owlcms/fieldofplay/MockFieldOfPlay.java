/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.util.List;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;

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
}
