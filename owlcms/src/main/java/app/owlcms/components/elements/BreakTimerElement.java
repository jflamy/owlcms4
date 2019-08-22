/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;

import app.owlcms.fieldofplay.ProxyBreakTimer;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
//@Tag("break-timer-element")
//@HtmlImport("frontend://components/TimerElement.html")
public class BreakTimerElement extends TimerElement {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(BreakTimerElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.TRACE);
	}

	/**
	 * Instantiates a new timer element.
	 */
	public BreakTimerElement() {
	}
	
	@Override
    public void clientFinalWarning() {
        // ignored 
    }

	@Override
    public void clientInitialWarning() {
        // ignored
    }

	/**
	 * Set the remaining time when the timer element has been hidden for a long time.
	 */
	@Override
	@ClientCallable
	public void clientSyncTime() {
		logger.info("timer element fetching time");
		OwlcmsSession.withFop(fop -> {
			ProxyBreakTimer breakTimer = (ProxyBreakTimer) fop.getBreakTimer();
            doSetTimer(breakTimer.isIndefinite() ? null : breakTimer.getTimeRemaining());
		});
		return;
	}

	/**
	 * Timer stopped
	 * 
	 * @param remaining Time the remaining time
	 */
	@Override
	@ClientCallable
	public void clientTimeOver() {
		logger.info("break time over");
		OwlcmsSession.withFop(fop -> {
			fop.getBreakTimer().timeOver(this);
		});
	}

	/**
	 * Timer stopped
	 *
	 * @param remaining Time the remaining time
	 */
	@Override
	@ClientCallable
	public void clientTimerStopped(double remainingTime) {
		logger.trace("timer stopped from client" + remainingTime);
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiEventLogger.debug("&&& break {} {}", e.getClass().getSimpleName(),	e.getOrigin());
		doStopTimer();
	}

	@Subscribe
	public void slaveBreakPause(UIEvent.BreakPaused e) {
		uiEventLogger.debug("&&& break {} {}",  e.getClass().getSimpleName(), e.getOrigin());
		doStopTimer();
	}

	@Subscribe
	public void slaveBreakSet(UIEvent.BreakSetTime e) {
		Integer milliseconds = e.isIndefinite() ? null : e.getTimeRemaining();
		uiEventLogger.debug("&&& break {} {} {} {}", e.getClass().getSimpleName(), milliseconds, e.isIndefinite(), e.getOrigin());
		doSetTimer(milliseconds);
	}

    @Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		uiEventLogger.debug("&&& break {} {} {}", e.getClass().getSimpleName(), null, e.getOrigin());
		doStartTimer(null);
	}

    /* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		init();
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			doSetTimer(fop.getBreakTimer().getTimeRemaining());
			// we listen on uiEventBus; this method ensures we stop when detached.
			uiEventBusRegister(this, fop);
		});
	}

}
