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

import app.owlcms.components.elements.TimerElement;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
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

	private Object origin;

	/**
	 * Instantiates a new timer element.
	 */
	public AthleteTimerElement() {
		this.setOrigin(null); // force exception
	}
	
	public AthleteTimerElement(Object origin) {
		this.setOrigin(origin);
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.TimerElement#clientSyncTime()
	 */
	@Override
	@ClientCallable
	public void clientSyncTime() {
		OwlcmsSession.withFop(fop -> {
			int timeRemaining = fop.getAthleteTimer().getTimeRemaining();		
			logger.debug("Fetched time = {} for {}",timeRemaining, fop.getCurAthlete());
			doSetTimer(timeRemaining);
		});
		return;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.TimerElement#clientTimeOver() */
	@Override
	@ClientCallable
	public void clientTimeOver() {
		logger.info("Received time over from client");
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

	/**
	 * @return the origin
	 */
	public Object getOrigin() {
		return origin;
	}

	@Subscribe
	public void slaveAthleteAnnounced(UIEvent.AthleteAnnounced e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		clientSyncTime();
	}
	
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		// TODO: must not stop timer if the lifting order change does not concern the clock owner.
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		if (e.isStopAthleteTimer()) {
			clientSyncTime();
		}
	}

	@Subscribe
	public void slaveSetTimer(UIEvent.SetTime e) {
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		doSetTimer(milliseconds);
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug(">>> start received {} {}", e, milliseconds);
		doStartTimer(milliseconds);
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		doStopTimer();
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.TimerElement#init()
	 */
	@Override
	protected void init() {
		super.init();
		getModel().setSilent(false); // emit sounds
	}

	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		logger.debug("attaching to {}",this.getOrigin());
		init();
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			doSetTimer(fop.getAthleteTimer().getTimeRemaining());
			// we listen on uiEventBus; this method ensures we stop when detached.
			uiEventBusRegister(this, fop);
		});
	}

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

}
