package com.github.mvysny.vaadinboot;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.open.Open;

/**
 * Modified to always open a browser and not to write on stdout.
 * Also changed the termination message since Enter does not work for our use case.
 * 
 * JFLamy
 */
public class VaadinBoot {
    /**
     * The default port where Jetty will listen for http:// traffic.
     */
    private static final int DEFAULT_PORT = 8080;

    /**
     * The port where Jetty will listen for http:// traffic. Defaults to {@value #DEFAULT_PORT}.
     * <br/>
     * Can be configured via the <code>SERVER_PORT</code> environment variable, or <code>-Dserver.port=</code> Java system property.
     */
    int port = Integer.parseInt(Env.getProperty("SERVER_PORT", "server.port", "" + DEFAULT_PORT));

    /**
     * Listen on interface handling given host name. Defaults to <code>null</code> which causes Jetty
     * to listen on all interfaces.
     * <br/>
     * Can be configured via the <code>SERVER_ADDRESS</code> environment variable, or <code>-Dserver.address=</code> Java system property.
     */


    String hostName = Env.getProperty("SERVER_ADDRESS", "server.address");

    /**
     * The context root to run under. Defaults to `/`.
     * Change this to e.g. /foo to host your app on a different context root
     * <br/>
     * Can be configured via the <code>SERVER_SERVLET_CONTEXT-PATH</code> environment variable, or <code>-Dserver.servlet.context-path=</code> Java system property.
     */


    String contextRoot = Env.getProperty("SERVER_SERVLET_CONTEXT-PATH", "server.servlet.context-path", "/");

    /**
     * When the app launches, open the browser automatically when in dev mode.
     */
    private boolean openBrowserInDevMode = true;

    /**
     * If true, no classpath scanning is performed - no servlets nor weblisteners are detected.
     * <br/>
     * This will most probably cause Vaadin to not work and throw NullPointerException at <code>VaadinServlet.serveStaticOrWebJarRequest</code>.
     * However, it's a good thing to disable this when starting your app with a QuickStart configuration.
     */
    private boolean disableClasspathScanning = false;

    /**
     * If true, the test classpath will also be scanned for annotations. Defaults to false.
     * <br/>
     * Only set to true if you have Vaadin routes in <code>src/test/java/</code> - it's
     * a bit of an antipattern but quite common with Vaadin addons. See
     * <a href="https://github.com/mvysny/vaadin-boot/issues/15">Issue #15</a> for more details.
     * <br/>
     * Ignored if {@link #disableClasspathScanning} is true.
     */
    private boolean isScanTestClasspath = false;

    /**
     * If true and we're running on JDK 21+, we'll configure Jetty to take advantage
     * of virtual threads.
     * <br/>
     * Defaults to true.
     */
    private boolean useVirtualThreadsIfAvailable = true;

    /**
     * Sets the port to listen on. Listens on {@value #DEFAULT_PORT} by default.
     * @param port the new port, 1..65535
     * @return this
     */

    public VaadinBoot setPort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Parameter port: invalid value " + port + ": must be 1..65535");
        }
        this.port = port;
        return this;
    }

    /**
     * Listen on network interface handling given host name. Pass in null to listen on all interfaces;
     * pass in `127.0.0.1` or `localhost` to listen on localhost only (or call {@link #localhostOnly()}).
     * @param hostName the interface to listen on.
     * @return this
     */

    public VaadinBoot listenOn(String hostName) {
        this.hostName = hostName;
        return this;
    }

    /**
     * Listen on the <code>localhost</code> network interface only.
     * @return this
     */

    public VaadinBoot localhostOnly() {
        return listenOn("localhost");
    }

    /**
     * Change this to e.g. /foo to host your app on a different context root
     * @param contextRoot the new context root, e.g. `/foo`.
     * @return this
     */

    public VaadinBoot withContextRoot(String contextRoot) {
        this.contextRoot = Objects.requireNonNull(contextRoot);
        return this;
    }

    /**
     * When the app launches, open the browser automatically when in dev mode.
     * @param openBrowserInDevMode defaults to true.
     * @return this
     */

    public VaadinBoot openBrowserInDevMode(boolean openBrowserInDevMode) {
        this.openBrowserInDevMode = openBrowserInDevMode;
        return this;
    }

    /**
     * Returns the URL where the app is running, for example <code>http://localhost:8080/app</code>.
     * @return the server URL, not null.
     */

    public String getServerURL() {
        return "http://" + (hostName != null ? hostName : "localhost") + ":" + port + contextRoot;
    }

    /**
     * If true, no classpath scanning is performed - no servlets nor weblisteners are detected.
     * <br/>
     * This will most probably cause Vaadin to not work and throw NullPointerException at <code>VaadinServlet.serveStaticOrWebJarRequest</code>.
     * However, it's a good thing to disable this when starting your app with a QuickStart configuration.
     * @return this
     */

    public VaadinBoot disableClasspathScanning() {
        return disableClasspathScanning(true);
    }

    /**
     * If true, no classpath scanning is performed - no servlets nor weblisteners are detected.
     * <br/>
     * This will most probably cause Vaadin to not work and throw NullPointerException at <code>VaadinServlet.serveStaticOrWebJarRequest</code>.
     * However, it's a good thing to disable this when starting your app with a QuickStart configuration.
     * @param disableClasspathScanning If true, no classpath scanning is performed. Defaults to false.
     * @return this
     */

    public VaadinBoot disableClasspathScanning(boolean disableClasspathScanning) {
        this.disableClasspathScanning = disableClasspathScanning;
        return this;
    }

    /**
     * When called, the test classpath will also be scanned for annotations. Defaults to false.
     * <br/>
     * Use only in case when you have Vaadin routes in <code>src/test/java/</code> - it's
     * a bit of an antipattern but quite common with Vaadin addons. See
     * <a href="https://github.com/mvysny/vaadin-boot/issues/15">Issue #15</a> for more details.
     * <br/>
     * Ignored if {@link #disableClasspathScanning} is true.
     * @return this
     */

    public VaadinBoot scanTestClasspath() {
        isScanTestClasspath = true;
        return this;
    }

    /**
     * If true and we're running on JDK 21+, we'll configure Jetty to take advantage
     * of virtual threads.
     * <br/>
     * Defaults to true.
     * @param useVirtualThreadsIfAvailable if true (default), use virtual threads to
     *                                     handle http requests if running on JDK21+
     * @return this
     */

    public VaadinBoot useVirtualThreadsIfAvailable(boolean useVirtualThreadsIfAvailable) {
        this.useVirtualThreadsIfAvailable = useVirtualThreadsIfAvailable;
        return this;
    }

    // mark volatile: might be accessed by the shutdown hook from a different thread.
    private volatile Server server;

    /**
     * Runs your app. Blocks until the user presses Enter or CTRL+C.
     * <br/>
     * WARNING: this function may never terminate when the entire JVM may be killed on CTRL+C.
     * @throws Exception when the webapp fails to start.
     */
    public void run() throws Exception {
        start();

        // We want to shut down the app cleanly by calling stop().
        // Unfortunately, that's not easy. When running from:
        // * Intellij as a Java app: CTRL+C doesn't work but Enter does.
        // * ./gradlew run: Enter doesn't work (no stdin); CTRL+C kills the app forcibly.
        // * ./mvnw exec:java: both CTRL+C and Enter works properly.
        // * cmdline as an unzipped app (production): both CTRL+C and Enter works properly.
        // Therefore, we'll use a combination of the two.

        // this gets called both when CTRL+C is pressed, and when main() terminates.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop("Shutdown hook called, shutting down")));
        System.out.println("Press CTRL+C to shutdown");

        if (openBrowserInDevMode && !Env.isVaadinProductionMode) {
            Open.open(getServerURL());
        }

        // Await for Enter.
        if (System.in.read() == -1) {
            // "./gradlew" run offers no stdin and read() will return immediately with -1
            // This happens when we're running from Gradle; but also when running from Docker with no tty
            System.out.println("No stdin available. press CTRL+C to shutdown");
            server.join(); // blocks endlessly
        } else {
            stop("Main: Shutting down");
        }
    }

    /**
     * Starts the Jetty server and your app. Blocks until the app is fully started, then
     * resumes execution. Mostly used for testing.
     * @throws Exception when the webapp fails to start.
     */
    public void start() throws Exception {
        @SuppressWarnings("unused")
		final long startupMeasurementSince = System.currentTimeMillis();
        log.info("Starting App");

        // detect&enable production mode, but only if it hasn't been specified by the user already
        if (System.getProperty("vaadin.productionMode") == null && Env.isVaadinProductionMode) {
            // fixes https://github.com/mvysny/vaadin14-embedded-jetty/issues/1
            System.setProperty("vaadin.productionMode", "true");
        }

        fixClasspath();
        log.debug("Classpath fixed");

        final WebAppContext context = createWebAppContext();
        log.debug("Jetty WebAppContext created");

        server = new Server(newThreadPool());
        final ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(port);
        if (hostName != null) {
            serverConnector.setHost(hostName);
        }
        server.addConnector(serverConnector);
        server.setHandler(context);
        log.debug("Jetty Server configured");
        try {
            server.start();
            log.debug("Jetty Server started");

            onStarted(context);

//            final Duration startupDuration = Duration.ofMillis(System.currentTimeMillis() - startupMeasurementSince);
//            System.out.println("\n\n=================================================\n" +
//                    "Started in " + startupDuration + ". Running on " + Env.dumpHost() + "\n" +
//                    "Please open " + getServerURL() + " in your browser.");
//            if (!Env.isVaadinProductionMode) {
//                System.out.println("If you see the 'Unable to determine mode of operation' exception, just kill me and run `./gradlew vaadinPrepareFrontend` or `./mvnw vaadin:prepare-frontend`");
//            }
//            System.out.println("=================================================\n");
        } catch (Exception e) {
            stop("Failed to start");
            throw e;
        }
    }

    /**
     * Invoked when the Jetty server has been started. By default, does nothing. You can
     * for example dump the quickstart configuration here.
     * @param context the web app context.
     * @throws IOException on i/o exception
     */
    public void onStarted(WebAppContext context) throws IOException {
    }

    /**
     * Creates the Jetty {@link WebAppContext}.
     * @return the {@link WebAppContext}
     * @throws IOException on i/o exception
     */

    protected WebAppContext createWebAppContext() throws IOException {
        final WebAppContext context = new WebAppContext();
        final Resource webRoot = Env.findWebRoot(context.getResourceFactory());
        context.setBaseResource(webRoot);
        context.setContextPath(contextRoot);

        // don't add the servlet this way - the @WebServlet annotation is ignored!
        // https://github.com/mvysny/vaadin-boot/issues/22
//        context.addServlet(servlet, "/*");

        // when the webapp fails to initialize, make sure that start() throws.
        context.setThrowUnavailableOnStartupException(true);
        if (!disableClasspathScanning) {
            // this will properly scan the classpath for all @WebListeners, including the most important
            // com.vaadin.flow.server.startup.ServletContextListeners.
            // See also https://mvysny.github.io/vaadin-lookup-vs-instantiator/
            // Jetty documentation: https://www.eclipse.org/jetty/documentation/jetty-12/operations-guide/index.html#og-annotations-scanning
            String pattern = ".*\\.jar|.*/classes/.*";
            if (isScanTestClasspath) {
                pattern += "|.*/test-classes/.*";
            }
            context.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN, pattern);
            // must be set to true, to enable classpath scanning:
            // https://eclipse.dev/jetty/documentation/jetty-12/operations-guide/index.html#og-annotations-scanning
            context.setConfigurationDiscovered(true);
        }
        return context;
    }

    /**
     * See {@link Env#fixClasspath()}.
     */
    protected void fixClasspath() {
        Env.fixClasspath();
    }

    /**
     * Stops your app. Blocks until the webapp is fully stopped. Mostly used for tests.
     * Never throws an exception.
     * @param reason why we're shutting down. Logged as info.
     */
    public void stop(String reason) {
        try {
            if (server != null) {
                log.info(reason);
                server.stop(); // blocks until the webapp stops fully
                log.info("Stopped");
                server = null;
            }
        } catch (Throwable t) {
            log.error("stop() failed: " + t, t);
        }
    }


    private static final Logger log = LoggerFactory.getLogger(VaadinBoot.class);

    /**
     * Creates a thread pool for Jetty to serve http requests.
     * @return the thread pool, may be null if the default one is to be used.
     */

    protected ThreadPool newThreadPool() {
        if (useVirtualThreadsIfAvailable && Env.getJavaVersion() >= 21) {
            log.info("Configuring Jetty to use JVM 21+ virtual threads");
            // see https://eclipse.dev/jetty/documentation/jetty-12/programming-guide/index.html#pg-arch-threads-thread-pool-virtual-threads
            final QueuedThreadPool threadPool = new QueuedThreadPool();
            try {
                final Method m = Executors.class.getDeclaredMethod("newVirtualThreadPerTaskExecutor");
                threadPool.setVirtualThreadsExecutor(((Executor) m.invoke(null)));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return threadPool;
        } else {
            log.info("Configuring Jetty to use regular JVM threads");
            return null;
        }
    }

	public int getPort() {
		return port;
	}
}