/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.fieldofplay;

import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class RelayTimer.
 * Relays start-stop instructions to the browser-based client app.owlcms.ui.displayselection.
 * Keeps server-side information on elapsed time.
 */
public class RelayTimer implements ICountdownTimer {

	final private Logger logger = (Logger) LoggerFactory.getLogger(RelayTimer.class);
	{ logger.setLevel(Level.DEBUG); }

	protected int timeRemaining;
	protected FieldOfPlay fop;
	protected long startMillis;
	protected long stopMillis;
	
	/**
	 * Instantiates a new countdown timer.
	 * @param fop 
	 */
	public RelayTimer(FieldOfPlay fop) {
		this.fop = fop;
	}
	
	/***
	 * Remote control commands, via events
	 * We ignore these since we are not actually listening.
	 */
	
	@Override
	@Subscribe
	public void startTimer(UIEvent.StartTime e) {
	}
	
	@Override
	@Subscribe
	public void stopTimer(UIEvent.StopTime e) {
	}
	
	@Override
	@Subscribe
	public void setTimer(UIEvent.SetTime e) {
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see app.owlcms.tests.ICountDownTimer#start()
	 */
	@Override
	public void start() {
		startMillis = System.currentTimeMillis();
		logger.debug("starting Time -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		fop.getUiEventBus().post(new UIEvent.StartTime(timeRemaining, null));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see app.owlcms.tests.ICountDownTimer#stop()
	 */
	@Override
	public void stop() {
		stopMillis =  System.currentTimeMillis();
		long elapsed = stopMillis-startMillis;
		timeRemaining = (int) (timeRemaining - elapsed);
		logger.debug("stopping Time -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		fop.getUiEventBus().post(new UIEvent.StopTime(timeRemaining, null));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see app.owlcms.tests.ICountDownTimer#getTimeRemaining()
	 */
	@Override
	public int getTimeRemaining() {
		return timeRemaining;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.ICountdownTimer#setTimeRemaining(int)
	 */
	@Override
	public void setTimeRemaining(int timeRemaining) {
		logger.debug("setting Time -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		this.timeRemaining = timeRemaining;
		fop.getUiEventBus().post(new UIEvent.SetTime(timeRemaining, null));
	}
	
	@SuppressWarnings("unused")
	private String msToString(Integer millis) {
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		long fullHoursInMinutes = TimeUnit.HOURS.toMinutes(hours);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		long fullMinutesInSeconds = TimeUnit.MINUTES.toSeconds(minutes);
		if (hours > 0) {
			return String.format("%02d:%02d:%02d", hours,
		      minutes - fullHoursInMinutes,
		      seconds - fullMinutesInSeconds);
		} else {
			return String.format("%02d:%02d",
			      minutes,
			      seconds - fullMinutesInSeconds);
		}
	}

	@Override
	public void timeOut(Object origin) {
		this.stop();
		fop.getFopEventBus().post(new FOPEvent.TimeOver(origin));
	}


}
