/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.init;

import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionInitEvent;
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
	
	/**
	 * Instantiates a new service listener.
	 */
	public ServiceListener() {}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.server.VaadinServiceInitListener#serviceInit(com.vaadin.flow.server.ServiceInitEvent)
	 */
	@Override
	public void serviceInit(ServiceInitEvent event) {	
		logger.info("Vaadin Service Startup Configuration.");
		event.getSource()
			.addSessionInitListener(sessionInitEvent -> {
				sessionInit(sessionInitEvent);
			});
	}

	// session init listener will be called whenever a VaadinSession is created
	// (which holds the http session and all the browser pages (UIs) under
	// the same session.
	private void sessionInit(SessionInitEvent sessionInitEvent) {
		VaadinSession session = sessionInitEvent.getSession();
		
		// override noisy Jetty error handler.
		session.setErrorHandler(new JettyErrorHandler());
		
		// ignore browser-specific settings based on configuration
		session.setLocale(Locale.ENGLISH);
		
	}

}
