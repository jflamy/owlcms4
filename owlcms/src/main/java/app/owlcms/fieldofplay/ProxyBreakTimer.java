/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ProxyBreakTimer. Relay timer instructions from {@link FieldOfPlay} to the actual timers
 * associated with each screen. Memorize the elapsed time and timer state.
 *
 * @author Jean-François Lamy
 */
/**
 * @author owlcms
 *
 */
public class ProxyBreakTimer implements IProxyTimer, IBreakTimer {

	private Integer breakDuration;
	private LocalDateTime end;
	private FieldOfPlay fop;
	private boolean indefinite;
	private long lastStop;
	final private Logger logger = (Logger) LoggerFactory.getLogger(ProxyBreakTimer.class);
	private Object origin;
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
	 * Instantiates a new break timer proxy.
	 *
	 * @param fop the fop
	 */
	public ProxyBreakTimer(FieldOfPlay fop) {
		this.setFop(fop);
	}

	@Override
	public void finalWarning(Object origin) {
		// ignored
	}

	/**
	 * @see app.owlcms.fieldofplay.IBreakTimer#getBreakDuration()
	 */
	@Override
	public Integer getBreakDuration() {
		return this.breakDuration;
	}

	public LocalDateTime getEnd() {
		return this.end;
	}

	public Object getOrigin() {
		return this.origin;
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
		// ignored
	}

	/**
	 * @return the indefinite
	 */
	@Override
	public boolean isIndefinite() {
		return this.indefinite;
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
		if (getEnd() != null) {
			int until = (int) LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS);
			this.logger.debug("liveTimeRemaining target {} {}",
			        until >= 0 ? DurationFormatUtils.formatDurationHMS(until) : until,
			        LoggerUtils.whereFrom());
			return until;
		} else if (isRunning()) {
			this.stopMillis = System.currentTimeMillis();
			long elapsed = this.stopMillis - this.startMillis;
			int tr = (int) (getTimeRemaining() - elapsed);
			this.logger.debug("liveTimeRemaining running {} {}",
			        tr >= 0 ? DurationFormatUtils.formatDurationHMS(tr) : tr,
			        LoggerUtils.whereFrom());
			return tr;
		} else {
			int tr = getTimeRemaining();
			this.logger.debug("liveTimeRemaining stopped {} {}",
			        tr >= 0 ? DurationFormatUtils.formatDurationHMS(tr) : tr,
			        LoggerUtils.whereFrom());
			return tr;
		}
	}

	/**
	 * @see app.owlcms.fieldofplay.IBreakTimer#setBreakDuration(java.lang.Integer)
	 */
	@Override
	public void setBreakDuration(Integer breakDuration) {
		this.breakDuration = breakDuration;
	}

	/**
	 * @see app.owlcms.fieldofplay.IBreakTimer#setEnd(java.time.LocalDateTime)
	 */
	@Override
	public void setEnd(LocalDateTime targetTime) {
		this.setIndefinite(false);
		// end != null overrides duration computation
		// logger.trace("setting end time = {} \n{}", targetTime, LoggerUtils.stackTrace());
		this.end = targetTime;
	}

	/**
	 * @param fop the fop to set
	 */
	@Override
	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	/**
	 * @see app.owlcms.fieldofplay.IBreakTimer#setIndefinite()
	 */
	@Override
	public void setIndefinite() {
		CeremonyType ceremonyType = getFop().getCeremonyType();
		if (ceremonyType != null) {
			// we never start a timer for a ceremony
			return;
		}
		this.indefinite = true;
		// logger.debug("setting breaktimer indefinite = {} [{}]", indefinite, LoggerUtils.whereFrom());
		this.setTimeRemaining(0, false);
		this.setEnd(null);
		getFop().pushOutUIEvent(
		        new UIEvent.BreakSetTime(getFop().getBreakType(), getFop().getCountdownType(), getTimeRemaining(), null,
		                true, this, LoggerUtils.stackTrace(), getFop()));
		this.indefinite = true;
	}

	@Override
	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int, boolean)
	 *
	 */
	@Override
	public void setTimeRemaining(int timeRemaining2, boolean indefinite) {
		// logger.debug("--- ProxyBreakTimer setTimeRemaining={} indefinite={} from {}", timeRemaining2, indefinite,
		// LoggerUtils.whereFrom());
		this.setIndefinite(indefinite);
		this.timeRemaining = timeRemaining2;
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#start()
	 */
	@Override
	public void start() {
		BreakType breakType = getFop().getBreakType();
		//logger.debug("{}****** starting break with breakType = {} from={}", FieldOfPlay.getLoggingName(fop), breakType, LoggerUtils.whereFrom());
		if (breakType == null) {
			this.logger.error("null breaktype {}", LoggerUtils.stackTrace());
		}
		// CeremonyType ceremonyType = getFop().getCeremonyType();
		this.startMillis = System.currentTimeMillis();

		Integer millisRemaining = getMillis();

		UIEvent.BreakStarted event = new UIEvent.BreakStarted(
		        millisRemaining, getOrigin(), false,
		        breakType,
		        getFop().getCountdownType(), LoggerUtils.stackTrace(), this.isIndefinite(), getFop());
		// logger.debug("posting {}", event);
		getFop().pushOutUIEvent(event);
		setRunning(true);
		
		if (Config.getCurrent().featureSwitch("oldTimers")) {
			return;
		}
		
		// if a break is running, need to stop it before starting another.
		if (this.serverTimer != null) {
			//logger.debug("Cancelling running timer");
			serverTimer.cancel();
		}
		this.serverTimer = new Timer();
		TimerTask timerTask = computeTask(timeRemaining);
		serverTimer.schedule(timerTask, timeRemaining);

	}

	private TimerTask computeTask(int timeRemaining2) {
		logger.info("{}+++++ scheduling serverTimer break over {}", FieldOfPlay.getLoggingName(fop), timeRemaining);
		return new TimerTask() {
			@Override
			public void run() {
				logger.info("{}+++++ running break over", FieldOfPlay.getLoggingName(fop));
				timeOver(this);
			}
		};
	}

	/**
	 * @see app.owlcms.fieldofplay.IProxyTimer#stop()
	 */
	@Override
	public void stop() {
		if (isRunning()) {
			computeTimeRemaining();
		}
		setRunning(false);
		this.timeRemainingAtLastStop = this.timeRemaining;
		// logger.debug("*** stopping Break -- timeRemaining = {} [{}]", getTimeRemaining(), LoggerUtils.whereFrom());
		this.timeRemainingAtLastStop = getTimeRemaining();
		// logger.debug("break stop = {} [{}]", liveTimeRemaining(), LoggerUtils.whereFrom());
		if (this.serverTimer != null) {
			this.serverTimer.cancel();
		}
		UIEvent.BreakPaused event = new UIEvent.BreakPaused(isIndefinite() ? null : getTimeRemaining(), getOrigin(),
		        false,
		        getFop().getBreakType(), getFop().getCountdownType(), getFop());

		getFop().pushOutUIEvent(event);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object)
	 */
	@Override
	public void timeOver(Object origin) {
		// logger.debug("****** break {} {} timeover = {} [{}]", isRunning(), isIndefinite(), getTimeRemaining(),
		// LoggerUtils.whereFrom());
		if (isRunning() && !isIndefinite()) {
			long now = System.currentTimeMillis();
			if (now - this.lastStop > 1000) {
				// ignore rash of timers all signaling break over
				this.lastStop = System.currentTimeMillis();
				this.stop();
			} else {
				return;
			}
			// should emit sound at end of break
			// logger.debug("******* timeOver \n{}", LoggerUtils.whereFrom());
			getFop().pushOutUIEvent(new UIEvent.BreakDone(origin, getFop().getBreakType(), getFop()));
			getFop().fopEventPost(new FOPEvent.BreakDone(getFop().getBreakType(), origin));
		}
	}

	/**
	 * @return the fop
	 */
	FieldOfPlay getFop() {
		return this.fop;
	}

	/**
	 * Compute time elapsed since start and adjust time remaining accordingly.
	 */
	private int computeTimeRemaining() {
		if (getEnd() != null) {
			setTimeRemaining((int) LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS), false);
		} else {
			this.stopMillis = System.currentTimeMillis();
			long elapsed = this.stopMillis - this.startMillis;
			setTimeRemaining((int) (getTimeRemaining() - elapsed), false);
		}
		return getTimeRemaining();
	}

	private int getMillis() {
		return (int) (this.getEnd() != null ? LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS)
		        : getTimeRemaining());
	}

	private void setRunning(boolean running) {
		this.running = running;
	}

	private void setIndefinite(boolean indefinite) {
		// logger.debug("breakTimer setIndefinite {} {}",indefinite, LoggerUtils.whereFrom());
		this.indefinite = indefinite;
	}
}
