/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

/**
 * Drops UI events that arrive after a newer event has already been applied.
 *
 * The FOP UI bus is asynchronous; two events posted in order can reach a display's
 * {@code ui.access} queue inverted. The FOP stamps every event with a monotonic per-FOP
 * sequence; this class keeps the high-water mark of what was applied. Not thread-safe by
 * design: every call must be made while holding the same UI lock.
 */
public final class UIEventSequenceGuard {

	private long lastApplied;

	/**
	 * Record the event as applied unless it is older than one already applied.
	 *
	 * @param sequence the FOP sequence of the event; 0 means unstamped and is never stale
	 * @return true if the event is stale and must be ignored
	 */
	public boolean isStale(long sequence) {
		if (sequence > 0 && sequence < this.lastApplied) {
			return true;
		}
		this.lastApplied = Math.max(this.lastApplied, sequence);
		return false;
	}

	/** Raise the high-water mark (e.g. on a group switch) without applying an event. Never lowers it. */
	public void advanceTo(long sequence) {
		this.lastApplied = Math.max(this.lastApplied, sequence);
	}

	public long getLastApplied() {
		return this.lastApplied;
	}
}
