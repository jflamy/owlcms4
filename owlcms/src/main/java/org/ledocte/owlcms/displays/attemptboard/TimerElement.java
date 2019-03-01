/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.displays.attemptboard;

import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.ICountdownTimer;
import org.ledocte.owlcms.state.UIEvent;
import org.ledocte.owlcms.ui.home.SafeEventBusRegistration;
import org.ledocte.owlcms.ui.lifting.UIEventProcessor;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
@Tag("timer-element")
@HtmlImport("frontend://components/TimerElement.html")
public class TimerElement extends PolymerTemplate<TimerElement.TimerModel> implements ICountdownTimer, SafeEventBusRegistration {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TimerElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("owlcms.uiEventLogger");

	/**
	 * TimerModel
	 * 
	 * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties.
	 * When the JS properties are changed, a "propname-changed" event is triggered.
	 * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
	 * 
	 */
	public interface TimerModel extends TemplateModel {

		/**
		 * Gets the current time.
		 *
		 * @return the current time
		 */
		double getCurrentTime();

		/**
		 * Sets the current time.
		 *
		 * @param seconds the new current time
		 */
		void setCurrentTime(double seconds);

		/**
		 * Gets the start time (the time to which the timer will reset)
		 *
		 * @return the start time
		 */
		double getStartTime();

		/**
		 * Sets the start time (the time to which the timer will reset)
		 *
		 * @param seconds the new start time
		 */
		void setStartTime(double seconds);

		/**
		 * Checks if timer is running.
		 *
		 * @return true, if is running
		 */
		boolean isRunning();

		/**
		 * Sets the timer running with true, stops if false.
		 *
		 * @param 
		 */
		void setRunning(boolean running);

		/**
		 * Checks if is counting up.
		 *
		 * @return true, if is counting up
		 */
		boolean isCountUp();

		/**
		 * Sets the count direction.
		 *
		 * @param countUp counts up if true, down if false.
		 */
		void setCountUp(boolean countUp);

		/**
		 * Checks if is interactive.
		 *
		 * @return true, if is interactive
		 */
		boolean isInteractive();

		/**
		 * Sets whether debugging controls are shown.
		 *
		 * @param interactive if true.
		 */
		void setInteractive(boolean interactive);
	}

	private EventBus uiEventBus;
	private Element timerElement;

	/**
	 * Instantiates a new timer element.
	 */
	public TimerElement() {
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.INFO);
	}

	protected void init() {
		double seconds = 0.00D;
		TimerModel model = getModel();
		model.setStartTime(0.0D);
		model.setCurrentTime(seconds);
		model.setCountUp(false);
		model.setRunning(false);
		model.setInteractive(true);
		
		timerElement = this.getElement();

//		timerElement.addPropertyChangeListener("running", "running-changed", (e) -> {
//			logger.info(
//				e.getPropertyName() + " changed to " + e.getValue() + " isRunning()=" + this.getModel().isRunning());
//		});
	}

	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		init();
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			setTimer(fop.getTimer().getTimeRemaining());
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	/* (non-Javadoc)
	 * @see org.ledocte.owlcms.state.ICountdownTimer#startTimer(org.ledocte.owlcms.state.UIEvent.StartTime)
	 */
	@Override
	@Subscribe
	public void startTimer(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Integer milliseconds = e.getTimeRemaining();
			uiEventLogger.debug(">>> start received {} {}", e, milliseconds);
			if (milliseconds != null)
				setTimeRemaining(milliseconds);
			start();
		});
	}
	
	/* (non-Javadoc)
	 * @see org.ledocte.owlcms.state.ICountdownTimer#startTimer(org.ledocte.owlcms.state.UIEvent.StartTime)
	 */
	@Override
	@Subscribe
	public void stopTimer(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			uiEventLogger.debug("<<< stop received {}", e);
			stop();
		});
	}

	/* (non-Javadoc)
	 * @see org.ledocte.owlcms.state.ICountdownTimer#setTimer(org.ledocte.owlcms.state.UIEvent.SetTime)
	 */
	@Override
	@Subscribe
	public void setTimer(UIEvent.SetTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Integer milliseconds = e.getTimeRemaining();
			uiEventLogger.debug("=== set received {}", milliseconds);
			setTimeRemaining(milliseconds);
		});
	}
	

	protected void setTimer(Integer milliseconds) {
		UIEventProcessor.uiAccess(this, uiEventBus, null, () -> {
			setTimeRemaining(milliseconds);
		});
	}

	/* @see org.ledocte.owlcms.state.ICountdownTimer#start() */
	@Override
	public void start() {
		timerElement.callFunction("start");
	}

	/* @see org.ledocte.owlcms.state.ICountdownTimer#stop() */
	@Override
	public void stop() {
		timerElement.callFunction("pause");
	}	

	/* @see org.ledocte.owlcms.state.ICountdownTimer#getTimeRemaining() */
	@Override
	public int getTimeRemaining() {
		return (int) (getModel().getCurrentTime()/1000.0D) ;
	}
	
	/* @see org.ledocte.owlcms.state.ICountdownTimer#setTimeRemaining(int) */
	@Override
	public void setTimeRemaining(int milliseconds) {
		double seconds = milliseconds/1000.0D;
		TimerModel model = getModel();
		model.setCurrentTime(seconds);
		model.setStartTime(seconds);
	}
	
	/*** 
	 * Client-callable functions
	 */
	
	/**
	 * Timer stopped
	 *
	 * @param remaining Time the remaining time
	 */
	@ClientCallable
	public void timerStopped(double remainingTime) {
		logger.info("timer stopped " + remainingTime);
	}
	
	//FIXME: add functions for 90 seconds, 30 seconds, 0 seconds
}
