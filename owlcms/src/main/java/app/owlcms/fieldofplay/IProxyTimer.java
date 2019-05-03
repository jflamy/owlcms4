/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

/**
 * The Interface IProxyTimer.
 */
public interface IProxyTimer {
		
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
	void timeOut(Object origin);

	/**
	 * @return time remaining when the clock owner last stopped it.
	 */
	public int getTimeRemainingAtLastStop();

	

}