/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.net.URL;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
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
import app.owlcms.apputils.StartupUtils;
import app.owlcms.utils.LoggerUtils;
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
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        context.setErrorHandler(new ErrorHandler());
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
            disableServerVersionHeader(server);
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

    /**
     * Don't reveal version number in headers.
     * @param server
     */
    private void disableServerVersionHeader(Server server) {
        for (Connector y : server.getConnectors()) {
            y.getConnectionFactories().stream()
                    .filter(cf -> cf instanceof HttpConnectionFactory)
                    .forEach(cf -> {
                        HttpConfiguration httpConfiguration = ((HttpConnectionFactory) cf)
                                .getHttpConfiguration();
                        httpConfiguration.setSendServerVersion(false);
                    });
        }
    }
    
    /**
     * Dummy error handler that disables any error pages or jetty related messages and returns our
     * ERROR status JSON with plain HTTP status instead. All original error messages (from our code) are preserved
     * as they are not handled by this code.
     */
    static class ErrorHandler extends ErrorPageErrorHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.getWriter()
                .append("{\"status\":\"ERROR\",\"message\":\"HTTP ")
                .append(String.valueOf(response.getStatus()))
                .append("\"}");
        }
    }
    

    private CountDownLatch getLatch() {
        return latch;
    }

    private void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

}