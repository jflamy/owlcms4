/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import com.google.common.eventbus.Subscribe;

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
	public default void start(FOPEvent e) {
		start();
	};

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
	void timeOut(Object origin);

	

}