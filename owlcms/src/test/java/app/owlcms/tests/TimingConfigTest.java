/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import app.owlcms.data.competition.Competition;

/**
 * Tests that the decision and jury display durations are read from the timing properties.
 */
public class TimingConfigTest {

	@After
	public void restoreDefaults() {
		Competition.decisionVisibleDuration = 3500;
		Competition.juryDecisionHold = 10000;
		Competition.juryOutcomeCardDuration = 5000;
		Competition.decisionBlockHold = 20000;
	}

	@Test
	public void durationsAreReadFromProperties() {
		Properties props = new Properties();
		props.setProperty("decisionVisibleDuration", "1111");
		props.setProperty("juryDecisionHold", "2222");
		props.setProperty("juryOutcomeCardDuration", "3333");
		props.setProperty("decisionBlockHold", "4444");

		Competition.applyTimingConfig(props);

		assertEquals(1111, Competition.decisionVisibleDuration);
		assertEquals(2222, Competition.juryDecisionHold);
		assertEquals(3333, Competition.juryOutcomeCardDuration);
		assertEquals(4444, Competition.decisionBlockHold);
	}

	@Test
	public void absentKeysKeepDefaults() {
		Properties props = new Properties();
		props.setProperty("juryDecisionHold", "2222");

		Competition.applyTimingConfig(props);

		assertEquals(3500, Competition.decisionVisibleDuration);
		assertEquals(2222, Competition.juryDecisionHold);
		assertEquals(5000, Competition.juryOutcomeCardDuration);
		assertEquals(20000, Competition.decisionBlockHold);
	}
}
