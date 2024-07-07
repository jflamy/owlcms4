/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ProxyBreakTimer. Relay serverTimer instructions from {@link FieldOfPlay} to the actual timers associated with each
 * screen. Memorize the elapsed time and serverTimer state.
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
	private Timer serverTimer;
	{
		this.logger.setLevel(Level.INFO);
	}

	/**
	 * Instantiates a new countdown serverTimer.
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
		getFop().pushOutUIEvent(new UIEvent.SetTime(timeRemaining, null, LoggerUtils.stackTrace(), getFop()));
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
		                LoggerUtils.stackTrace(), getFop()));
		this.running = true;

		if (!Config.getCurrent().featureSwitch("oldTimers")) {
			this.serverTimer = new Timer();
			serverTimer.schedule(computeTask(timeRemaining), timeRemaining % 30000);
		}
	}

	private TimerTask computeTask(int timeRemaining2) {
		final int timeRemaining = timeRemaining2;
		int nbStops = (timeRemaining) / 30000;
		switch (nbStops) {
			case 0 -> {
				logger.debug("{}+++++ scheduling serverTimer timeOver {}", FieldOfPlay.getLoggingName(fop), timeRemaining);
				return new TimerTask() {
					@Override
					public void run() {
						logger.info("{}+++++ running time over", FieldOfPlay.getLoggingName(fop));
						timeOver(this);
					}
				};
			}
			case 1 -> {
				logger.debug("{}+++++ scheduling serverTimer finalWarning {}", FieldOfPlay.getLoggingName(fop), timeRemaining);
				return new TimerTask() {
					@Override
					public void run() {
						logger.info("{}+++++ running final warning", FieldOfPlay.getLoggingName(fop));
						finalWarning(this);
						// next task is time over, in 30sec.
						serverTimer.schedule(computeTask(0), 30000);
					}
				};
			}
			case 2 -> {
				logger.debug("{}+++++ scheduling serverTimer 1:00 {}", FieldOfPlay.getLoggingName(fop), timeRemaining);
				return new TimerTask() {
					@Override
					public void run() {
						logger.info("{}running 1:00", FieldOfPlay.getLoggingName(fop));
						// nothing to do, next task is final warning, in 30s.
						serverTimer.schedule(computeTask(30000), 30000);
					}
				};
			}
			case 3 -> {
				logger.debug("{}+++++ scheduling server serverTimer initialWarning {}", FieldOfPlay.getLoggingName(fop), timeRemaining);
				return new TimerTask() {
					@Override
					public void run() {
						logger.info("{}+++++ running initial warning", FieldOfPlay.getLoggingName(fop));
						initialWarning(this);
						// next task is final warning, in 60 seconds.
						serverTimer.schedule(computeTask(30000), 60000);
					}
				};
			}
			case 4 -> {
				logger.debug("{}+++++ scheduling server serverTimer 2:00 {}", FieldOfPlay.getLoggingName(fop), timeRemaining);
				return new TimerTask() {
					@Override
					public void run() {
						logger.info("{}+++++ running 2:00", FieldOfPlay.getLoggingName(fop));
						// next task is initial warning, in 30s.
						serverTimer.schedule(computeTask(90000), 30000);
					}
				};
			}
			default -> {
				throw new RuntimeException("timeRemaining " + timeRemaining + " nbStops " + nbStops);
			}
		}
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
		if (this.serverTimer != null) {
			logger.info("{}+++++ stopping serverTimer", FieldOfPlay.getLoggingName(fop));
			this.serverTimer.cancel();
		}
		getFop().pushOutUIEvent(new UIEvent.StopTime(this.timeRemaining, null, getFop()));
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
