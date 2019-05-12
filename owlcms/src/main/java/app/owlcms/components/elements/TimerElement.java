/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
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
public abstract class TimerElement extends PolymerTemplate<TimerElement.TimerModel>
		implements SafeEventBusRegistration {

	/**
	 * TimerModel Vaadin Flow propagates these variables to the corresponding Polymer template
	 * JavaScript properties. When the JS properties are changed, a "propname-changed" event is
	 * triggered. {@link Element.#addPropertyChangeListener(String, String,
	 * com.vaadin.flow.dom.PropertyChangeListener)}
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
		 * Checks if timer is running.
		 *
		 * @return true, if is running
		 */
		boolean isRunning();
		
		/**
		 * Checks if timer is silent.
		 *
		 * @return true, if sounds are to be emitted.
		 */
		boolean isSilent();

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
		 * Sets the timer running with true, stops if false.
		 *
		 * @param running setting to true starts the timer, false stops it.
		 */
		void setRunning(boolean running);

		/**
		 * Sets the start time (the time to which the timer will reset)
		 *
		 * @param seconds the new start time
		 */
		void setStartTime(double seconds);

		/**
		 * Determine whether sounds are emitted at 90, 30 and 0 seconds
		 * 
		 * @param quiet true indicates no sound
		 */
		void setSilent(boolean quiet);
	}

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TimerElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private EventBus uiEventBus;
	private Element timerElement;

	/**
	 * Instantiates a new timer element.
	 */
	public TimerElement() {
	}

	/**
	 * Client requests that the server send back the remaining time. Intended to be used after client
	 * has been hidden and is made visible again.
	 */
	@ClientCallable
	abstract public void clientSyncTime();

	/**
	 * Timer ran down to zero.
	 */
	@ClientCallable
	abstract public void clientTimeOver();

	/**
	 * Timer has been stopped on the client side.
	 * 
	 * @param remainingTime
	 */
	@ClientCallable
	abstract public void clientTimerStopped(double remainingTime);

	protected final void doSetTimer(Integer milliseconds) {
		if (milliseconds == null) {
			logger.error(LoggerUtils.stackTrace());
		}
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			stop();
			setTimeRemaining(milliseconds);
		});
	}

	protected void doStartTimer(Integer milliseconds) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (milliseconds != null) {
				setTimeRemaining(milliseconds);
			}
			start();
		});
	}

	protected void doStopTimer() {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			stop();
		});
	}

	protected Element getTimerElement() {
		return timerElement;
	}

	protected void init() {
		double seconds = 0.00D;
		TimerModel model = getModel();
		model.setStartTime(0.0D);
		model.setCurrentTime(seconds);
		model.setCountUp(false);
		model.setRunning(false);
		model.setSilent(true);

		setTimerElement(this.getElement());
	}

	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		init();
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			doSetTimer(fop.getAthleteTimer().getTimeRemaining());
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	protected void setTimerElement(Element timerElement) {
		this.timerElement = timerElement;
	}

	private void setTimeRemaining(int milliseconds) {
		logger.debug("=== time remaining = {} from {} ", milliseconds, LoggerUtils.whereFrom());
		double seconds = milliseconds / 1000.0D;
		TimerModel model = getModel();
		model.setCurrentTime(seconds);
		model.setStartTime(seconds);
		// should not be necessary
		getTimerElement().callFunction("reset");
	}

	private void start() {
		getTimerElement().callFunction("start");
	}

	private void stop() {
		getTimerElement().callFunction("pause");
	}

}
