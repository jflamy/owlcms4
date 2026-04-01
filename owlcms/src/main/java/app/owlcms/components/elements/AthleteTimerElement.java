/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;

import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
public class AthleteTimerElement extends TimerElement {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteTimerElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.WARN);
		uiEventLogger.setLevel(Level.INFO);
	}

	/**
	 * Instantiates a new timer element.
	 */
	public AthleteTimerElement() {
		this.setOrigin(null); // force exception
		logger.trace("### AthleteTimerElement created (no-arg constructor)\n{}", LoggerUtils.stackTrace());
	}

	public AthleteTimerElement(Object origin) {
		this.setOrigin(origin);
		logger.debug("### AthleteTimerElement created with origin={}\n{}", origin, LoggerUtils.stackTrace());
	}

	public void detach() {
		if (this.fop != null) {
			try {
				this.fop.getFopEventBus().unregister(this);
			} catch (Exception e) {
				// ignored
			}
		}
	}

	@Subscribe
	public void slaveSetTimer(UIEvent.SetTime e) {
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), milliseconds,
		        e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		doSetTimer(milliseconds);
	}

	// @Subscribe
	// public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
	// uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
	// (e.isCurrentDisplayAffected() ? "stop_timer" : "leave_asis"), this.getOrigin(), e.getOrigin());
	// if (e.isCurrentDisplayAffected()) {
	// clientSyncTime(fopName);
	// }
	//// else {
	//// uiEventLogger.trace(LoggerUtils./**/stackTrace());
	//// }
	// }

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug(">>> start received {} {}", e, milliseconds);
		doStartTimer(milliseconds, e.isServerSound());
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		Integer milliseconds = e.getTimeRemaining();
		doStopTimer(milliseconds);
	}

	public void syncWithFop(FieldOfPlay fop) {
		init(fop.getName());
		// sync with current status of FOP
		IProxyTimer athleteTimer = getFopTimer(fop);
		if (athleteTimer != null) {
			if (athleteTimer.isRunning()) {
				doStartTimer(athleteTimer.liveTimeRemaining(), isSilenced() || fop.isEmitSoundsOnServer());
			} else {
				doSetTimer(athleteTimer.getTimeRemaining());
			}
		}
	}

	@Override
	public void syncWithFopTimer(FieldOfPlay fop) {
		// only used by break timer
	}

	@Override
	protected IProxyTimer getFopTimer(FieldOfPlay fop) {
		return fop.getAthleteTimer();
	}

	@Override
	protected double getInitialWarningThresholdSeconds() {
		return Competition.athleteTimerInitialWarning / 1000.0D;
	}

	@Override
	protected double getFinalWarningThresholdSeconds() {
		return Competition.athleteTimerFinalWarning / 1000.0D;
	}

	@Override
	protected boolean isIndefinite() {
		return false;
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent); // Guard and UI setup
		if (this.fop == null) {
			logger.error("AthleteTimerElement requires explicit FOP before attach {}", LoggerUtils.whereFrom());
			return;
		}
		init(this.fop.getName());
		// sync with current status of FOP
		IProxyTimer fopTimer = getFopTimer(this.fop);
		if (fopTimer != null) {
			if (fopTimer.isRunning()) {
				doStartTimer(fopTimer.liveTimeRemaining(), isSilenced() || this.fop.isEmitSoundsOnServer());
			} else {
				doSetTimer(fopTimer.getTimeRemaining());
			}
		}
		// we listen on uiEventBus.
		uiEventBusRegister(this, this.fop);
	}

}
