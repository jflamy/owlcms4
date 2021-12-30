/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
@SuppressWarnings("serial")
public class JettyErrorHandler extends DefaultErrorHandler {
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
        } else {
            logger.error("", t);
        }
    }

}
