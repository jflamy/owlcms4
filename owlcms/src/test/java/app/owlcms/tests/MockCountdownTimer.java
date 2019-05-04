/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.tests;

import org.slf4j.LoggerFactory;

import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class MockCountdownTimer implements IProxyTimer {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(MockCountdownTimer.class);

	private int timeRemaining;

	private int timeRemainingAtLastStop;

	public MockCountdownTimer() {
		logger.setLevel(Level.INFO);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.tests.ICountDownTimer#start()
	 */
	@Override
	public void start() {
		logger.debug("starting Time -- timeRemaining = {} \t[{}]",timeRemaining, LoggerUtils.whereFrom());
		timeRemaining = (getTimeRemaining() - 1000);
		timeRemainingAtLastStop = timeRemaining;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.tests.ICountDownTimer#stop()
	 */
	@Override
	public void stop() {
		logger.debug("stopping Time -- timeRemaining = {} \t[{}]",timeRemaining, LoggerUtils.whereFrom());
		timeRemaining = (getTimeRemaining());
		timeRemainingAtLastStop = timeRemaining;

	}

	/**
	 * @return the timeRemainingAtLastStop
	 */
	@Override
	public int getTimeRemainingAtLastStop() {
		return timeRemainingAtLastStop;
	}


	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemaining()
	 */
	@Override
	public int getTimeRemaining() {
		return timeRemaining;
	}


	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int)
	 */
	@Override
	public void setTimeRemaining(int timeRemaining) {
		logger.debug("setting Time -- timeRemaining = {}\t[{}]", timeRemaining, LoggerUtils.whereFrom());
		this.timeRemaining = timeRemaining;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object)
	 */
	@Override
	public void timeOut(Object origin) {
		stop();
		timeRemaining = 0;
	}


}
