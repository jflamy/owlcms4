/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ProxyBreakTimer. Relay timer instructions from {@link FieldOfPlay} to the actual timers
 * associated with each screen. Memorize the elapsed time and timer state.
 *
 * @author Jean-François Lamy
 */
public class ProxyBreakTimer implements IProxyTimer {

	final private Logger breakLogger = (Logger) LoggerFactory.getLogger(ProxyBreakTimer.class);
	{
		breakLogger.setLevel(Level.INFO);
	}

	private int timeRemaining;
	private FieldOfPlay fop;
	private long startMillis;
	private long stopMillis;
	private int timeRemainingAtLastStop;

	/**
	 * Instantiates a new break timer proxy.
	 *
	 * @param fop the fop
	 */
	public ProxyBreakTimer(FieldOfPlay fop) {
		this.fop = fop;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemaining() */
	@Override
	public int getTimeRemaining() {
		return timeRemaining;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int) */
	@Override
	public void setTimeRemaining(int timeRemaining) {
		breakLogger.debug("break timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		this.timeRemaining = timeRemaining;
		fop.getUiEventBus().post(new UIEvent.BreakSetTime(timeRemaining, this));
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#start() */
	@Override
	public void start() {
		startMillis = System.currentTimeMillis();
		breakLogger.debug("break start = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		timeRemainingAtLastStop = timeRemaining;
		fop.getUiEventBus().post(new UIEvent.BreakStarted(timeRemaining, this));
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#stop() */
	@Override
	public void stop() {
		stopMillis = System.currentTimeMillis();
		long elapsed = stopMillis - startMillis;
		timeRemaining = (int) (timeRemaining - elapsed);
		timeRemainingAtLastStop = timeRemaining;
		breakLogger.debug("break stop = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		fop.getUiEventBus().post(new UIEvent.BreakPaused(this));
	}

	/**
	 * @return the timeRemainingAtLastStop
	 */
	@Override
	public int getTimeRemainingAtLastStop() {
		return timeRemainingAtLastStop;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object) */
	@Override
	public void timeOut(Object origin) {
		breakLogger.debug("break stop = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
		fop.getUiEventBus().post(new UIEvent.BreakDone(origin));
		fop.getFopEventBus().post(new FOPEvent.StartLifting(origin));

	}

}
