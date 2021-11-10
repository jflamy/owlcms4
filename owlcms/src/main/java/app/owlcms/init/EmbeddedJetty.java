/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.net.BindException;
import java.net.URI;
import java.net.URL;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.startup.ServletContextListeners;

import app.owlcms.Main;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * jetty web server
 */
public class EmbeddedJetty {
    private final static Logger logger = (Logger) LoggerFactory.getLogger(EmbeddedJetty.class);
    private final static Logger startLogger = (Logger) LoggerFactory.getLogger(Main.class);

    private CountDownLatch latch;

    public EmbeddedJetty(CountDownLatch latch) {
        this.setLatch(latch);
    }

    /**
     * Run.
     *
     * @param port        the port
     * @param contextPath the context path
     * @throws Exception the exception
     */
    public void run(int port, String contextPath) throws Exception {
        startLogger.info("starting web server");
        URL webRootLocation = this.getClass().getResource("/META-INF/resources/");
        URI webRootUri = webRootLocation.toURI();

        WebAppContext context = new WebAppContext();
        context.setBaseResource(Resource.newResource(webRootUri));
        context.setContextPath(contextPath);
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*");
        context.setConfigurationDiscovered(true);
        context.setConfigurations(new Configuration[] {
                new AnnotationConfiguration(),
                new WebInfConfiguration(),
                new WebXmlConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new PlusConfiguration(),
                new JettyWebXmlConfiguration()
        });
        Context servletContext = context.getServletContext();
        servletContext.setExtendedListenerTypes(true);
        context.addEventListener(new ServletContextListeners());

        Server server = new Server(port);
        server.setHandler(context);
        ServletContextHandler scHandler = (ServletContextHandler) server.getHandler();
        scHandler.getServletHandler().addFilterWithMapping(HttpsEnforcer.class, "/*",
                EnumSet.of(DispatcherType.REQUEST));

        Main.initConfig();
        try {
            // start the server so that kubernetes ingress does not complain due to long initialization.
            server.start();
            startLogger.info("started on port {}", port);

            // start JPA+Hibernate, initialize database if needed, etc.
            Main.initData();

            // server threads blocking on latch will now go ahead.
            startLogger.info("initialization done, allowing requests.");
            getLatch().countDown();

            StartupUtils.startBrowser();
            server.join();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof BindException) {
                logger.error("another server is already running on port {}", port);
                System.err.println("another program is already using port " + port
                        + "; set the environment variable OWLCMS_PORT to use another port number");
            } else {
                logger.error(LoggerUtils./**/stackTrace());
                System.err.println("server could not be started");
                e.printStackTrace();
            }
        }
    }

    private CountDownLatch getLatch() {
        return latch;
    }

    private void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

}