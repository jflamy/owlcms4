/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ProxyBreakTimer. Relay timer instructions from {@link FieldOfPlay} to the actual timers associated with each
 * screen. Memorize the elapsed time and timer state.
 *
 * @author Jean-François Lamy
 */
public class ProxyAthleteTimer implements IProxyTimer {

	private FieldOfPlay fop;
	final private Logger logger = (Logger) LoggerFactory.getLogger(ProxyAthleteTimer.class);
	private boolean running = false;
	private long startMillis;
	private long stopMillis;
	private int timeRemaining;
	private int timeRemainingAtLastStop;
	{
		this.logger.setLevel(Level.INFO);
	}

	/**
	 * Instantiates a new countdown timer.
	 *
	 * @param fop
	 */
	public ProxyAthleteTimer(FieldOfPlay fop) {
		this.setFop(fop);
	}

	@Override
	public void finalWarning(Object origin) {
		getFop().emitFinalWarning();
	}

	/**
	 * @return the fop
	 */
	public FieldOfPlay getFop() {
		return this.fop;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemaining()
	 */
	@Override
	public int getTimeRemaining() {
		return this.timeRemaining;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemainingAtLastStop()
	 */
	@Override
	public int getTimeRemainingAtLastStop() {
		return this.timeRemainingAtLastStop;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#initialWarning(java.lang.Object)
	 */
	@Override
	public void initialWarning(Object origin) {
		getFop().emitInitialWarning();
	}

	@Override
	public boolean isIndefinite() {
		return false;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#isRunning()
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Compute time elapsed since start.
	 */
	@Override
	public int liveTimeRemaining() {
		if (this.running) {
			this.stopMillis = System.currentTimeMillis();
			long elapsed = this.stopMillis - this.startMillis;
			int tr = (int) (getTimeRemaining() - elapsed);
			// logger.debug("liveTimeRemaining running {} {}", formattedDuration(tr),
			// LoggerUtils.whereFrom());
			return tr;
		} else {
			int tr = getTimeRemaining();
			// logger.debug("liveTimeRemaining stopped {} {}", formattedDuration(tr),
			// LoggerUtils.whereFrom());
			return tr;
		}
	}

	/**
	 * @param fop the fop to set
	 */
	@Override
	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int, boolean)
	 */
	@Override
	public void setTimeRemaining(int timeRemaining, boolean indefinite) {
		if (this.running) {
			computeTimeRemaining();
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("{}==== setting Time -- timeRemaining = {} ({})", FieldOfPlay.getLoggingName(getFop()),
			        timeRemaining,
			        LoggerUtils.whereFrom());
		}
		this.timeRemaining = timeRemaining;
		if (timeRemaining < 1) {
			this.logger./**/warn("setting with no time {}", LoggerUtils.whereFrom());
		}
		getFop().pushOutUIEvent(new UIEvent.SetTime(timeRemaining, null, LoggerUtils.stackTrace()));
		this.running = false;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#start()
	 */
	@Override
	public void start() {
		if (!this.running) {
			this.startMillis = System.currentTimeMillis();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("{}starting Time -- timeRemaining = {} ({})", FieldOfPlay.getLoggingName(getFop()),
				        this.timeRemaining,
				        LoggerUtils.whereFrom());
			}
			this.timeRemainingAtLastStop = this.timeRemaining;
		}
		if (this.timeRemaining < 1) {
			this.logger./**/warn("starting with no time {}", LoggerUtils.whereFrom());
		}
		getFop().pushOutUIEvent(
		        new UIEvent.StartTime(this.timeRemaining, null, getFop().isEmitSoundsOnServer(),
		                LoggerUtils.stackTrace()));
		this.running = true;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#stop()
	 */
	@Override
	public void stop() {
		if (this.running) {
			computeTimeRemaining();
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("{}stopping Time -- timeRemaining = {} ({})", FieldOfPlay.getLoggingName(getFop()),
			        this.timeRemaining,
			        LoggerUtils.whereFrom());
		}
		this.timeRemainingAtLastStop = this.timeRemaining;
		getFop().pushOutUIEvent(new UIEvent.StopTime(this.timeRemaining, null));
		this.running = false;
	}

	@Override
	public void timeOver(Object origin) {
		// avoid sending multiple events to FOP
		boolean needToSendEvent = !getFop().isTimeoutEmitted();
		if (needToSendEvent) {
			getFop().emitTimeOver();
			getFop().fopEventPost(new FOPEvent.TimeOver(origin));
		}
		// leave enough time for buzzer event to propagate allowing for some clock drift
		if (this.running) {
			try {
				// timers that are more than 1 sec. late will now stop silently.
				Thread.sleep(1000);
				this.stop();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Compute time elapsed since start and adjust time remaining accordingly.
	 */
	private void computeTimeRemaining() {
		this.stopMillis = System.currentTimeMillis();
		long elapsed = this.stopMillis - this.startMillis;
		this.timeRemaining = (int) (this.timeRemaining - elapsed);
	}

	@SuppressWarnings("unused")
	private String formattedDuration(Integer milliseconds) {
		return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
		        : (milliseconds != null ? milliseconds.toString() : "-");
	}
}
