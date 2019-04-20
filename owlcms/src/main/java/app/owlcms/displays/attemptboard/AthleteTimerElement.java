/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.attemptboard;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;

import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
//@Tag("timer-element")
//@HtmlImport("frontend://components/TimerElement.html")
public class AthleteTimerElement extends TimerElement {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteTimerElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	/**
	 * Instantiates a new timer element.
	 */
	public AthleteTimerElement() {
	}

	@Subscribe
	public void slaveSetTimer(UIEvent.SetTime e) {
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug("=== set received {} from {} ", milliseconds, LoggerUtils.whereFrom());
		doSetTimer(milliseconds);
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.TimerElement#init()
	 */
	@Override
	protected void init() {
		super.init();
		getModel().setSilent(false); // emit sounds
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug(">>> start received {} {}", e, milliseconds);
		doStartTimer(milliseconds);
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		uiEventLogger.debug("<<< stop received {}", e);
		doStopTimer();
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.TimerElement#clientSyncTime()
	 */
	@Override
	@ClientCallable
	public void clientSyncTime() {
		logger.info("timer element fetching time");
		OwlcmsSession.withFop(fop -> {
			doSetTimer(fop.getAthleteTimer().getTimeRemaining());
		});
		return;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.TimerElement#clientTimeOver() */
	@Override
	@ClientCallable
	public void clientTimeOver() {
		logger.info("time over from client");
		OwlcmsSession.withFop(fop -> {
			fop.getAthleteTimer().timeOut(this);
		});
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double) */
	@Override
	@ClientCallable
	public void clientTimerStopped(double remainingTime) {
		logger.trace("timer stopped from client" + remainingTime);
		// do not stop the server-side timer, this is getting called as a result of the
		// server-side timer issuing a command.  Otherwise we create an infinite loop.
	}

	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		init();
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			doSetTimer(fop.getAthleteTimer().getTimeRemaining());
			// we listen on uiEventBus; this method ensures we stop when detached.
			uiEventBusRegister(this, fop);
		});
	}

}
