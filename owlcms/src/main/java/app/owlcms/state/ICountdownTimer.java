/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.state;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.UI;

/**
 * The Interface ICountdownTimer.
 */
public interface ICountdownTimer {
	
	@Subscribe
	public default void startTimer(UIEvent.StartTime e) {
		Integer milliseconds = e.getTimeRemaining();
		if (milliseconds != null) setTimeRemaining(milliseconds);
		start();
	}

	@Subscribe
	public default void stopTimer(UIEvent.StopTime e) {
		stop();
	}
	
	@Subscribe
	public default void setTimer(UIEvent.SetTime e) {
		Integer milliseconds = e.getTimeRemaining();
		setTimeRemaining(milliseconds);
	}
		
	/**
	 * Start.
	 */
	public void start();

	/**
	 * Stop.
	 */
	public void stop();

	/**
	 * Gets the time remaining.
	 *
	 * @return the time remaining
	 */
	public int getTimeRemaining();

	/**
	 * Sets the time remaining.
	 *
	 * @param timeRemaining the new time remaining
	 */
	public void setTimeRemaining(int timeRemaining);

	
	/**
	 * Stop with no time left.
	 */
	void timeOver(UI originatingUI);

}