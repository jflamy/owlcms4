/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.internal.AllowInert;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.init.OwlcmsSession;
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
		super();
		this.id = IdUtils.getTimeBasedId();
	}

	/**
	 * Instantiates a new timer element.
	 */
	public BreakTimerElement(String parentName) {
		super();
		this.id = IdUtils.getTimeBasedId();
	}

	@Override
	public void clientFinalWarning(String fopName) {
		// ignored
	}

	@Override
	public void clientInitialWarning(String fopName) {
		// ignored
	}

	/**
	 * Set the remaining time when the timer element has been hidden for a long time.
	 */
	@Override
	@AllowInert
	@ClientCallable
	public void clientSyncTime(String fopName) {
		OwlcmsSession.withFop(fop -> {
			if (!fopName.contentEquals(fop.getName())) {
				return;
			}
			this.logger.debug("{}{} fetching time", getClass().getSimpleName(), fop.getLoggingName());
			IProxyTimer fopTimer = getFopTimer(fop);
			doSetTimer(fopTimer.isIndefinite() ? null : fopTimer.liveTimeRemaining());
		});
		return;
	}

	/**
	 * Timer stopped
	 *
	 * @param remaining Time the remaining time
	 */
	@Override
	@AllowInert
	@ClientCallable
	public void clientTimeOver(String fopName) {
		OwlcmsSession.withFop(fop -> {
			if (fopName != null && !fopName.contentEquals(fop.getName())) {
				return;
			}
			// logger.debug("{}Received time over.", fop.getLoggingName());
			IProxyTimer fopTimer = getFopTimer(fop);
			// logger.debug("{} ============= {} break time over {}", fopName, fop.getName(), fopTimer.isIndefinite());
			if (!fopTimer.isIndefinite()) {
				getFopTimer(fop).timeOver(this);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double)
	 */
	@Override
	@AllowInert
	@ClientCallable
	public void clientTimerStarting(String fopName, double remainingTime, double lateMillis, String from) {
		// logger.debug("timer {} starting on client: remaining = {}, late={}, roundtrip={}", from, remainingTime,
		// lateMillis, delta(lastStartMillis));
	}

	/**
	 * Timer stopped
	 *
	 * @param remaining Time the remaining time
	 */
	@Override
	@AllowInert
	@ClientCallable
	public void clientTimerStopped(String fopName, double remainingTime, String from) {
		// do not stop the server-side timer, otherwise we create an infinite loop.
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
		syncWithFopTimer();
	}

	@Override
	public void syncWithFopTimer() {
		OwlcmsSession.withFop(fop -> {
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
					doSetTimer(null);
					// if (breakTimer.isIndefinite()) {
					// doSetTimer(null);
					// } else {
					// doSetTimer(breakTimer.getTimeRemainingAtLastStop());
					// }
				}
			}
		});
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
		OwlcmsSession.withFop(fop -> {
			// we listen on uiEventBus; this method ensures we stop when detached.
			this.uiEventLogger.trace("&&& breakTimerElement register {} {}", this.parentName, LoggerUtils.whereFrom());
			uiEventBusRegister(this, fop);
		});
		syncWithFopTimer();
	}

	private String formatDuration(Integer milliseconds) {
		return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
		        : (milliseconds != null ? milliseconds.toString() : "-");
	}
}
