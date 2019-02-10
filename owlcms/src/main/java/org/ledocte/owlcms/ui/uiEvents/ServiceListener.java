package org.ledocte.owlcms.ui.uiEvents;

import java.util.Locale;

import org.slf4j.LoggerFactory;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;

import ch.qos.logback.classic.Logger;

/**
 * Automatic configuration at startup of the various listeners for sessions, etc.
 * 
 * The fully qualified name of this class (org.ledocte.owlcms.ui.uiEvents.ServiceListener)
 * must appear on single line in file src/main/resources/META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener
 * 
 * @author owlcms
 *
 */
@SuppressWarnings("serial")
public class ServiceListener implements VaadinServiceInitListener {
	private static Logger logger = (Logger)LoggerFactory.getLogger(ServiceListener.class);
	
	public ServiceListener() {}

	@Override
	public void serviceInit(ServiceInitEvent event) {
		
		logger.info("Vaadin Service Startup Configuration.");
		// session init listener will be called whenever a VaadinSession is created
		// (which holds the http session and all the browser pages (UIs) under
		// the same session.
		event.getSource()
			.addSessionInitListener(sessionInitEvent -> {
				// override noisy Jetty error handler.
				VaadinSession session = sessionInitEvent.getSession();
				session.setErrorHandler(new JettyErrorHandler());
				// ignore browser-specific settings based on configuration
				session.setLocale(Locale.ENGLISH);
			});
	}

}
