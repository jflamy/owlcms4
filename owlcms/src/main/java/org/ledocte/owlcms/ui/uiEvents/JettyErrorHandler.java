/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.uiEvents;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorEvent;

import ch.qos.logback.classic.Logger;

/**
 * The Class JettyErrorHandler.
 */
@SuppressWarnings("serial")
public class JettyErrorHandler extends DefaultErrorHandler {
private final static Logger logger = (Logger) LoggerFactory.getLogger(JettyErrorHandler.class);
	
	/* (non-Javadoc)
	 * @see com.vaadin.flow.server.DefaultErrorHandler#error(com.vaadin.flow.server.ErrorEvent)
	 */
	@Override
	public void error(ErrorEvent event) {
        Throwable t = findRelevantThrowable(event.getThrowable());

		if (event.getThrowable() instanceof org.eclipse.jetty.io.EofException) {
			logger.trace(t.getLocalizedMessage());
		} else {
			logger.error("",t);
		}
	}

}
