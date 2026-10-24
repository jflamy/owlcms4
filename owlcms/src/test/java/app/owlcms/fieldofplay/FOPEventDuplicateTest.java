/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.MockCountdownTimer;
import app.owlcms.tests.TestData;

/**
 * The field of play ignores an event that duplicates the previous one. Distinct events whose hash codes collide must
 * not be ignored.
 */
public class FOPEventDuplicateTest {

	/**
	 * Even millisecond while the upper 32 bits of the clock are odd (October-November 2026): an unselect at this time
	 * and a selection one millisecond later have the same hash code.
	 */
	private static final long COLLIDING_TIME = (417L << 32) + 1000L;

	private Group gA;

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Before
	public void setupData() {
		TestData.insertInitialData(5, true);
		JPAService.runInTransaction(em -> {
			this.gA = GroupRepository.doFindByName("A", em);
			Group gB = GroupRepository.doFindByName("B", em);
			Group gC = GroupRepository.doFindByName("C", em);
			TestData.deleteAllLifters(em);
			TestData.insertSampleLifters(em, 5, this.gA, gB, gC);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);
	}

	@Test
	public void selectionOneMillisecondAfterUnselectIsNotADuplicate() {
		FOPEvent.SwitchGroup unselect = switchGroup(null, COLLIDING_TIME);
		FOPEvent.SwitchGroup select = switchGroup(this.gA, COLLIDING_TIME + 1);
		assertEquals("precondition: hash codes collide", unselect.hashCode(), select.hashCode());

		assertFalse(select.isDuplicateOf(unselect));
	}

	@Test
	public void selectionOneMillisecondAfterUnselectLoadsTheSession() {
		FieldOfPlay fop = MockFieldOfPlay.create(AthleteRepository.findAll(), new MockCountdownTimer(),
		        new MockCountdownTimer());

		fop.fopEventPost(switchGroup(null, COLLIDING_TIME));
		fop.fopEventPost(switchGroup(this.gA, COLLIDING_TIME + 1));

		assertNotNull(fop.getGroup());
		assertEquals(this.gA.getName(), fop.getGroup().getName());
	}

	@Test
	public void sameEventTwiceIsADuplicate() {
		FOPEvent.TimeStarted started = new FOPEvent.TimeStarted(this);
		assertTrue(started.isDuplicateOf(started));
	}

	@Test
	public void equalEventsAreDuplicatesOnlyWithinTheSameMillisecond() {
		FOPEvent.TimeStarted first = new FOPEvent.TimeStarted(this);
		first.setTimestamp(COLLIDING_TIME);
		FOPEvent.TimeStarted sameTime = new FOPEvent.TimeStarted(this);
		sameTime.setTimestamp(COLLIDING_TIME);
		FOPEvent.TimeStarted later = new FOPEvent.TimeStarted(this);
		later.setTimestamp(COLLIDING_TIME + 1);

		assertTrue(sameTime.isDuplicateOf(first));
		assertFalse(later.isDuplicateOf(first));
		assertFalse(first.isDuplicateOf(null));
	}

	private FOPEvent.SwitchGroup switchGroup(Group group, long timestamp) {
		FOPEvent.SwitchGroup event = new FOPEvent.SwitchGroup(group, this);
		event.setTimestamp(timestamp);
		return event;
	}
}
