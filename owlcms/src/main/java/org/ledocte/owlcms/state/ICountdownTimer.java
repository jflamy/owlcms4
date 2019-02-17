/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.state;

/**
 * The Interface ICountdownTimer.
 */
public interface ICountdownTimer {

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

}