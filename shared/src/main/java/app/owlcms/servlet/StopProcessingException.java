package app.owlcms.servlet;

/**
 * This exception is used when an error message has been given by the UI and
 * no further processing is necessary.  It can percolate all the way up
 * to the web server, where it will be ignored instead of polluting the logs.
 *
 */
@SuppressWarnings("serial")
public class StopProcessingException extends RuntimeException {

	/**
	 * @param message
	 * @param e
	 */
	public StopProcessingException(String message, Throwable e) {
		super(message, e);
	}

}
