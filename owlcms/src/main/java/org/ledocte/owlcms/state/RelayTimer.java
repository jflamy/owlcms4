/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.state;

import org.ledocte.owlcms.utils.LoggerUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class RelayTimer.
 * Relays start-stop instructions to the browser-based client displays.
 * Keeps server-side information on elapsed time.
 */
public class RelayTimer implements ICountdownTimer {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(RelayTimer.class);

	private int timeRemaining;
	private FieldOfPlayState fop;

	private long startMillis;

	private long stopMillis;
	
	/**
	 * Instantiates a new countdown timer.
	 * @param fop 
	 */
	public RelayTimer(FieldOfPlayState fop) {
		logger.setLevel(Level.DEBUG);
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
	 * @see org.ledocte.owlcms.tests.ICountDownTimer#start()
	 */
	@Override
	public void start() {
		startMillis = System.currentTimeMillis();
		logger.debug("starting Time -- timeRemaining = {}", timeRemaining);
		fop.getUiEventBus().post(new UIEvent.StartTime(timeRemaining));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ledocte.owlcms.tests.ICountDownTimer#stop()
	 */
	@Override
	public void stop() {
		stopMillis =  System.currentTimeMillis();
		long elapsed = stopMillis-startMillis;
		timeRemaining = (int) (timeRemaining - elapsed);
		logger.debug("stopping Time -- timeRemaining = {}", timeRemaining);
		fop.getUiEventBus().post(new UIEvent.StartTime(timeRemaining));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ledocte.owlcms.tests.ICountDownTimer#getTimeRemaining()
	 */
	@Override
	public int getTimeRemaining() {
		return timeRemaining;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ledocte.owlcms.tests.ICountDownTimer#setTimeRemaining(int)
	 */
	@Override
	public void setTimeRemaining(int timeRemaining) {
		logger.debug("setting Time -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		this.timeRemaining = timeRemaining;
		fop.getUiEventBus().post(new UIEvent.SetTime(timeRemaining));
	}

}
