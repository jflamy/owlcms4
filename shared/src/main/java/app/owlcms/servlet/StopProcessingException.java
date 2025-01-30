/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.servlet;

/**
 * This exception is used when an error message has been given by the UI and
 * no further processing is necessary.  It can percolate all the way up
 * to the web server, where it will be ignored instead of polluting the logs.
 *
 */
public class StopProcessingException extends RuntimeException {

	/**
	 * @param message
	 * @param e
	 */
	public StopProcessingException(String message, Throwable e) {
		super(message, e);
	}

}
