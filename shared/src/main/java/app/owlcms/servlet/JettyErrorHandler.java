/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.servlet;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorEvent;

import ch.qos.logback.classic.Logger;

/**
 * The Class JettyErrorHandler.
 */

public class JettyErrorHandler extends DefaultErrorHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -3601423112228045354L;
    private final static Logger logger = (Logger) LoggerFactory.getLogger(JettyErrorHandler.class);

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.server.DefaultErrorHandler#error(com.vaadin.flow.server. ErrorEvent)
     */
    @Override
    public void error(ErrorEvent event) {
        Throwable t = findRelevantThrowable(event.getThrowable());

        if (event.getThrowable() instanceof org.eclipse.jetty.io.EofException) {
            logger.trace(t.getLocalizedMessage());
        } else if (event.getThrowable() instanceof StopProcessingException) {
            logger.trace(t.getLocalizedMessage());
        } else {
            logger.error("{} {}", t.getClass().getTypeName(), t);
        }
    }

}
