/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.BootstrapListener;
import com.vaadin.flow.server.BootstrapPageResponse;
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
@SuppressWarnings("serial")
public class ServiceListener implements VaadinServiceInitListener {
    private class CustomBootstrapListener implements BootstrapListener {

        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            Document document = response.getDocument();
            Element head = document.head();
            head.appendChild(createMeta(document, "robots", "noindex"));
        }

        private Element createMeta(Document document, String property,
                String content) {
            Element meta = document.createElement("meta");
            meta.attr("property", property);
            meta.attr("content", content);
            return meta;
        }

//        @Override
//        public void configurePage(InitialPageSettings settings) {
//            settings.addMetaTag("mobile-web-app-capable", "yes");
//            settings.addMetaTag("apple-mobile-web-app-capable", "yes");
//            settings.addLink("shortcut icon", "frontend/images/owlcms.ico");
//            settings.addFavIcon("icon", "frontend/images/logo.png", "96x96");
//            settings.setViewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes");
//        }

//        public void addLink(Position position, String href,
//                Map<String, String> attributes) {
//            Element link = new Element(Tag.valueOf("link"), "").attr("href", href);
//            attributes.forEach((key, value) -> link.attr(key, value));
//            getElement(position).add(link);
//        }

//        public void addFavIcon(Position position, String rel, String href,
//                String sizes) {
//            Element link = new Element(Tag.valueOf("link"), "").attr("href", href);
//            link.attr("rel", rel);
//            link.attr("sizes", sizes);
//            getElement(position).add(link);
//        }

    }

    private static Logger logger = (Logger) LoggerFactory.getLogger(ServiceListener.class);

    private CustomBootstrapListener bootStrapEventListener;

    /**
     * Instantiates a new service listener.
     */
    public ServiceListener() {
        bootStrapEventListener = new CustomBootstrapListener();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.server.VaadinServiceInitListener#serviceInit(com.vaadin.flow. server.ServiceInitEvent)
     */
    @Override
    public void serviceInit(ServiceInitEvent event) {
        logger.debug("Vaadin Service Startup Configuration. {} {}", event.toString(), LoggerUtils.whereFrom());
        event.getSource().addSessionInitListener(sessionInitEvent -> {
            sessionInit(sessionInitEvent);
        });

        event.addBootstrapListener(bootStrapEventListener);
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
