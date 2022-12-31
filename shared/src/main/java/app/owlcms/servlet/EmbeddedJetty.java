/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.servlet;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
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

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * jetty web server
 */
public class EmbeddedJetty {

    /**
     * Dummy error handler that disables any error pages or jetty related messages and returns our ERROR status JSON
     * with plain HTTP status instead. All original error messages (from our code) are preserved as they are not handled
     * by this code.
     */
    static class ErrorHandler extends ErrorPageErrorHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            response.getWriter()
                    .append("{\"status\":\"ERROR\",\"message\":\"HTTP ")
                    .append(String.valueOf(response.getStatus()))
                    .append("\"}");
        }
    }

    private final static Logger logger = (Logger) LoggerFactory.getLogger(EmbeddedJetty.class);

    private static Logger startLogger;

    public static Logger getStartLogger() {
        return startLogger;
    }

    private CountDownLatch latch;

    private Runnable initConfig;

    private Runnable initData;

    private Server server;

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
        getStartLogger().info("starting web server");
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

        server = new Server(port);
        server.setHandler(context);
        ServletContextHandler scHandler = (ServletContextHandler) server.getHandler();
        scHandler.getServletHandler().addFilterWithMapping(HttpsEnforcer.class, "/*",
                EnumSet.of(DispatcherType.REQUEST));

        initConfig.run();
        try {
            // start the server so that kubernetes ingress does not complain due to long initialization.

            server.start();
            disableServerVersionHeader(server);
            getStartLogger().info("started on port {}", port);

            // start JPA+Hibernate, initialize database if needed, etc.
            initData.run();

            // server threads blocking on latch will now go ahead.
            getStartLogger().info("initialization done, allowing requests.");
            getLatch().countDown();

            StartupUtils.startBrowser();
            server.join();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof BindException || cause instanceof SocketException) {
                logger.error("another server is already running on port {}", port);
                System.err.println("Another program is already using port " + port+" ; Please use another port number.");
                System.err.println("To change the port number, set the environment variable OWLCMS_PORT");
                System.err.println("or use the -Dport=number java option.");
            } else {
                logger.error(LoggerUtils./**/stackTrace());
                System.err.println("server could not be started");
                e.printStackTrace();
            }
        }
    }

    public EmbeddedJetty setInitConfig(Runnable initConfig) {
        this.initConfig = initConfig;
        return this;
    }

    public EmbeddedJetty setInitData(Runnable initData) {
        this.initData = initData;
        return this;
    }

    public EmbeddedJetty setStartLogger(Logger startLogger) {
        EmbeddedJetty.startLogger = startLogger;
        return this;
    }

    /**
     * Don't reveal version number in headers.
     *
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

    private CountDownLatch getLatch() {
        return latch;
    }

    private void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public void stop() {
        try {
            server.stop();
            server.destroy();
        } catch (Exception e) {
        }
    }

}