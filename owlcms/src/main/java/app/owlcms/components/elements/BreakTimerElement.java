/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
public class BreakTimerElement extends TimerElement {

	public Long id;
	private String parentName = "";
	final private Logger logger = (Logger) LoggerFactory.getLogger(BreakTimerElement.class);
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

	{
		this.logger.setLevel(Level.INFO);
		this.uiEventLogger.setLevel(Level.INFO);
	}

	public BreakTimerElement() {
		this.id = IdUtils.getTimeBasedId();
		logger.trace("### BreakTimerElement created (no-arg constructor)\n{}", LoggerUtils.stackTrace());
	}

	/**
	 * Instantiates a new timer element.
	 */
	public BreakTimerElement(String parentName) {
		this.id = IdUtils.getTimeBasedId();
		logger.debug("### BreakTimerElement created with parentName={}\n{}", parentName, LoggerUtils.stackTrace());
	}

	public void setParent(String s) {
		this.parentName = s;
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		if (this.uiEventLogger.isDebugEnabled()) {
			this.uiEventLogger.debug("&&& break done {} {}", this.parentName, e.getOrigin());
		}
		doStopTimer(0);
	}

	@Subscribe
	public void slaveBreakPause(UIEvent.BreakPaused e) {
		if (this.uiEventLogger.isDebugEnabled()) {
			this.uiEventLogger.debug("&&& breakTimerElement pause {} {}", this.parentName, e.getMillis());
		}
		doStopTimer(e.getMillis());
	}

	@Subscribe
	public void slaveBreakSet(UIEvent.BreakSetTime e) {
		Integer milliseconds;
		if (e.getEnd() != null) {
			milliseconds = (int) LocalDateTime.now().until(e.getEnd(), ChronoUnit.MILLIS);
		} else {
			milliseconds = e.isIndefinite() ? null : e.getTimeRemaining();
			if (this.uiEventLogger.isDebugEnabled()) {
				this.uiEventLogger.debug("&&& breakTimerElement set {} {} {} {} {}", this.parentName,
				        formatDuration(milliseconds), e.isIndefinite(), this.id, LoggerUtils.stackTrace());
			}

		}
		doSetTimer(milliseconds);
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		if (e.isDisplayToggle()) {
			return;
		}
		Integer tr = e.isIndefinite() ? null : e.getMillis();
		if (this.uiEventLogger.isDebugEnabled()) {
			this.uiEventLogger.debug("&&& breakTimerElement start {} {} {} {}", this.parentName, tr, e.getOrigin(),
			        LoggerUtils.whereFrom());
		}
		if (Boolean.TRUE.equals(e.getPaused())) {
			doSetTimer(tr);
		} else {
			doStartTimer(tr, true); // true means "silent".
		}
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		syncWithFopTimer(e.getFop());
	}

	@Override
	public void syncWithFopTimer(FieldOfPlay fop) {
		// OwlcmsSession.withFop(fop -> {
		init(fop.getName());
		// sync with current status of FOP
		IProxyTimer breakTimer = getFopTimer(fop);
		if (breakTimer != null) {
			if (this.uiEventLogger.isDebugEnabled()) {
				this.uiEventLogger.debug("&&& breakTimerElement sync running {} indefinite {}",
				        breakTimer.isRunning(),
				        breakTimer.isIndefinite());
			}
			if (breakTimer.isRunning()) {
				if (breakTimer.isIndefinite()) {
					if (this.uiEventLogger.isDebugEnabled()) {
						this.uiEventLogger.debug("&&& indefinite {}", breakTimer.liveTimeRemaining());
					}
					doStartTimer(null, fop.isEmitSoundsOnServer());
				} else {
					if (this.uiEventLogger.isDebugEnabled()) {
						this.uiEventLogger.debug("&&& live {}", breakTimer.liveTimeRemaining());
					}
					doStartTimer(breakTimer.liveTimeRemaining(), isSilenced() || fop.isEmitSoundsOnServer());
				}
			} else {
				// If the break timer is not currently running, prefer showing the last-stopped
				// remaining time (when appropriate) instead of clearing to null. Clearing
				// causes clients to display 0:00 on session switches even when the server
				// has a remembered remaining time.
				if (breakTimer.isIndefinite()) {
					doSetTimer(null);
				} else {
					doSetTimer(breakTimer.getTimeRemainingAtLastStop());
				}
			}
		}
		// });
	}

	@Override
	protected IProxyTimer getFopTimer(FieldOfPlay fop) {
		return fop.getBreakTimer();
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		this.ui = UI.getCurrent();
		if (this.fop == null) {
			this.logger.error("BreakTimerElement requires explicit FOP before attach {}", LoggerUtils.whereFrom());
			return;
		}
		this.uiEventLogger.trace("&&& breakTimerElement register {} {}", this.parentName, LoggerUtils.whereFrom());
		uiEventBusRegister(this, this.fop);
		syncWithFopTimer(this.fop);
	}

	private String formatDuration(Integer milliseconds) {
		return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
		        : (milliseconds != null ? milliseconds.toString() : "-");
	}
}
