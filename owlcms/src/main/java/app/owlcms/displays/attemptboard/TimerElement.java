/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.displays.attemptboard;

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

import app.owlcms.fieldofplay.ICountdownTimer;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.group.UIEventProcessor;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.utils.LoggerUtils;
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
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
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
		 * Gets the start time (the time to which the timer will reset)
		 *
		 * @return the start time
		 */
		double getStartTime();

		/**
		 * Checks if is counting up.
		 *
		 * @return true, if is counting up
		 */
		boolean isCountUp();

		/**
		 * Checks if is interactive.
		 *
		 * @return true, if is interactive
		 */
		boolean isInteractive();

		/**
		 * Checks if timer is running.
		 *
		 * @return true, if is running
		 */
		boolean isRunning();

		/**
		 * Sets the count direction.
		 *
		 * @param countUp counts up if true, down if false.
		 */
		void setCountUp(boolean countUp);

		/**
		 * Sets the current time.
		 *
		 * @param seconds the new current time
		 */
		void setCurrentTime(double seconds);

		/**
		 * Sets whether debugging controls are shown.
		 *
		 * @param interactive if true.
		 */
		void setInteractive(boolean interactive);

		/**
		 * Sets the timer running with true, stops if false.
		 *
		 * @param 
		 */
		void setRunning(boolean running);

		/**
		 * Sets the start time (the time to which the timer will reset)
		 *
		 * @param seconds the new start time
		 */
		void setStartTime(double seconds);
	}


	private EventBus uiEventBus;
	private Element timerElement;

	/**
	 * Instantiates a new timer element.
	 */
	public TimerElement() {
	}

	public void doSetTimer(Integer milliseconds) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setTimeRemaining(milliseconds);
		});
	}

	public void doStartTimer(Integer milliseconds) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (milliseconds != null)
				setTimeRemaining(milliseconds);
			start();
		});
	}

	public void doStopTimer() {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			stop();
		});
	}


	/* @see app.owlcms.fieldofplay.ICountdownTimer#getTimeRemaining() */
	@Override
	public int getTimeRemaining() {
		return (int) (getModel().getCurrentTime()/1000.0D) ;
	}
	
	
	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.ICountdownTimer#setTimer(app.owlcms.fieldofplay.UIEvent.SetTime)
	 */
	@Override
	@Subscribe
	public void setTimer(UIEvent.SetTime e) {
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug("=== set received {}", milliseconds);
		doSetTimer(milliseconds);
	}


	/* @see app.owlcms.fieldofplay.ICountdownTimer#setTimeRemaining(int) */
	@Override
	public void setTimeRemaining(int milliseconds) {
		logger.debug("time remaining = {} from {} ",milliseconds,LoggerUtils.whereFrom());
		double seconds = milliseconds/1000.0D;
		TimerModel model = getModel();
		model.setCurrentTime(seconds);
		model.setStartTime(seconds);
	}

	/* @see app.owlcms.fieldofplay.ICountdownTimer#start() */
	@Override
	public void start() {
		timerElement.callFunction("start");
	}


	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.ICountdownTimer#startTimer(app.owlcms.fieldofplay.UIEvent.StartTime)
	 */
	@Override
	@Subscribe
	public void startTimer(UIEvent.StartTime e) {
		Integer milliseconds = e.getTimeRemaining();
		uiEventLogger.debug(">>> start received {} {}", e, milliseconds);
		doStartTimer(milliseconds);
	}
	

	/* @see app.owlcms.fieldofplay.ICountdownTimer#stop() */
	@Override
	public void stop() {
		timerElement.callFunction("pause");
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.ICountdownTimer#startTimer(app.owlcms.fieldofplay.UIEvent.StartTime)
	 */
	@Override
	@Subscribe
	public void stopTimer(UIEvent.StopTime e) {
		uiEventLogger.debug("<<< stop received {}", e);
		doStopTimer();
	}

	/**
	 * Set the remaining time when the timer element has been hidden for a long time.
	 */
	@ClientCallable
	public void syncRemainingTime() {
		logger.info("timer element fetching time");
		OwlcmsSession.withFop(fop -> {
			this.setTimeRemaining(fop.getTimer().getTimeRemaining());
		});
		return;
	}	

	@Override
	public void timeOut(Object origin) {
		stop();
		setTimeRemaining(0);
	}
	
	/**
	 * Timer stopped
	 * @param remaining Time the remaining time
	 */
	@ClientCallable
	public void timeOver() {
		logger.info("time over from client");
		OwlcmsSession.withFop(fop -> {
			fop.getTimer().timeOut(this);
		});
	}
	
	/**
	 * Timer stopped
	 *
	 * @param remaining Time the remaining time
	 */
	@ClientCallable
	public void timerStopped(double remainingTime) {
		logger.trace("timer stopped from client" + remainingTime);
	}
	
	/*** 
	 * Client-callable functions
	 */
	
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
		init();
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			setTimer(fop.getTimer().getTimeRemaining());
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}
	
	protected void setTimer(Integer milliseconds) {
		doSetTimer(milliseconds);
	}
	
}
