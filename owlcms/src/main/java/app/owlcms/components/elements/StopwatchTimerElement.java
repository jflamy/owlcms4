/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import com.vaadin.flow.component.AttachEvent;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;

/**
 * Count-up (stopwatch) timer element.
 *
 * <p>
 * Unlike the other timer elements, this one is <em>not</em> driven by the FOP
 * timer. During a jury deliberation or a challenge the break timer is indefinite
 * and is not running, so there is no elapsed time to read from it. Instead this
 * element is started and cleared explicitly from the page that owns it (see
 * {@code Results}) on the same jury-deliberation / challenge events that show the
 * jury message. When started it counts up from zero.
 */
@SuppressWarnings("serial")
public class StopwatchTimerElement extends TimerElement {

	private boolean counting = false;

	public StopwatchTimerElement() {
		super();
	}

	@Override
	protected boolean isCountUpTimer() {
		return true;
	}

	@Override
	public void syncWithFopTimer(FieldOfPlay fop) {
		// Not driven by the FOP timer; just make sure the client element is bound.
		if (fop != null) {
			init(fop.getName());
		}
	}

	@Override
	protected IProxyTimer getFopTimer(FieldOfPlay fop) {
		return fop != null ? fop.getBreakTimer() : null;
	}

	@Override
	protected void onFopAssignedWhileAttached() {
		if (this.fop != null) {
			init(this.fop.getName());
		}
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		if (this.fop != null) {
			init(this.fop.getName());
			// Re-assert the count-up if the page attaches while a deliberation is ongoing.
			if (this.counting) {
				doStart();
			}
		}
	}

	/** Start counting up from zero (jury deliberation / challenge started). */
	public void startCountUp() {
		this.counting = true;
		doStart();
	}

	/** Clear the stopwatch (deliberation ended / lifting resumed). */
	public void clearCountUp() {
		this.counting = false;
		setDisplay(0, false, true);
	}

	private void doStart() {
		if (getTimerElement() == null && this.fop != null) {
			init(this.fop.getName());
		}
		start(0, false, true, "stopwatch");
	}
}
