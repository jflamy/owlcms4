/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.servlet.JettyErrorHandler;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Automatic configuration at startup of the various listeners for sessions, etc.
 *
 * The fully qualified name of this class (app.owlcms.ui.uiEvents.ServiceListener) must appear on single line in file
 * src/main/resources/META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener
 *
 * @author owlcms
 *
 */
public class ServiceListener implements VaadinServiceInitListener {
    private static Logger logger = (Logger) LoggerFactory.getLogger(ServiceListener.class);

    /**
     * Instantiates a new service listener.
     */
    public ServiceListener() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.server.VaadinServiceInitListener#serviceInit(com.vaadin.flow.server.ServiceInitEvent)
     */
    @Override
    public void serviceInit(ServiceInitEvent event) {
        logger.debug("Vaadin Service Startup Configuration. {} {}", event.toString(), LoggerUtils.whereFrom());
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

//		// ignore browser-specific settings based on configuration
//		session.setLocale(Locale.ENGLISH);
//
    }

}
