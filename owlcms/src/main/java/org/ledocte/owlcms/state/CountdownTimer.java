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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class CountdownTimer.
 */
public class CountdownTimer implements ICountdownTimer {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(CountdownTimer.class);

	private int timeRemaining;
	
	/**
	 * Instantiates a new countdown timer.
	 */
	public CountdownTimer() {
		logger.setLevel(Level.DEBUG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ledocte.owlcms.tests.ICountDownTimer#start()
	 */
	//FIXME: this should interface with the real timer
	@Override
	public void start() {
		logger.debug("starting Time -- timeRemaining = {}" + timeRemaining);
		setTimeRemaining(getTimeRemaining() - 1000);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ledocte.owlcms.tests.ICountDownTimer#stop()
	 */
	@Override
	public void stop() {
		logger.debug("stopping Time -- timeRemaining = {}" + timeRemaining);
		setTimeRemaining(getTimeRemaining());
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
	}

}
