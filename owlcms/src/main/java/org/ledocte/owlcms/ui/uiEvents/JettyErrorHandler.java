package org.ledocte.owlcms.ui.uiEvents;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorEvent;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JettyErrorHandler extends DefaultErrorHandler {
private final static Logger logger = (Logger) LoggerFactory.getLogger(JettyErrorHandler.class);
	
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
