package app.owlcms.uievents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UIEventSequenceGuardTest {

	@Test
	public void inOrderEventsAreApplied() {
		UIEventSequenceGuard guard = new UIEventSequenceGuard();
		assertFalse(guard.isStale(101));
		assertFalse(guard.isStale(102));
		assertEquals(102, guard.getLastApplied());
	}

	@Test
	public void olderEventArrivingAfterNewerIsDropped() {
		UIEventSequenceGuard guard = new UIEventSequenceGuard();
		assertFalse(guard.isStale(102));
		assertTrue(guard.isStale(101));
		assertEquals(102, guard.getLastApplied());
	}

	@Test
	public void equalSequenceIsNotDropped() {
		UIEventSequenceGuard guard = new UIEventSequenceGuard();
		assertFalse(guard.isStale(5));
		assertFalse(guard.isStale(5));
	}

	@Test
	public void unstampedEventIsNeverDroppedAndDoesNotLowerWatermark() {
		UIEventSequenceGuard guard = new UIEventSequenceGuard();
		assertFalse(guard.isStale(50));
		assertFalse(guard.isStale(0));
		assertEquals(50, guard.getLastApplied());
	}

	@Test
	public void groupSwitchDropsOrderUpdatesPostedBeforeIt() {
		UIEventSequenceGuard guard = new UIEventSequenceGuard();
		assertFalse(guard.isStale(10));
		// SwitchGroup stamped 20 is applied; a LiftingOrderUpdated stamped 15 arrives late
		assertFalse(guard.isStale(20));
		assertTrue(guard.isStale(15));
		// an update posted after the switch is still applied
		assertFalse(guard.isStale(21));
	}

	@Test
	public void delayedGroupSwitchIsRejectedAfterNewerOrderUpdate() {
		UIEventSequenceGuard guard = new UIEventSequenceGuard();
		assertFalse(guard.isStale(21));
		assertTrue(guard.isStale(20));
		assertEquals(21, guard.getLastApplied());
		assertFalse(guard.isStale(22));
	}

	@Test
	public void advanceNeverLowersWatermark() {
		UIEventSequenceGuard guard = new UIEventSequenceGuard();
		assertFalse(guard.isStale(30));
		guard.advanceTo(3);
		assertEquals(30, guard.getLastApplied());
		assertTrue(guard.isStale(29));
	}
}
