package app.owlcms.servlet;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.open.Open;

import jakarta.servlet.Servlet;

/**
 * Code originally from https://github.com/mvysny/vaadin-boot-example-maven.git
 * Modified to remove stdout and always open browser.
 *
 *
 *
 * Bootstraps your Vaadin application from your main() function. Simply call
 * <code><pre>
 * new VaadinBoot().withArgs(args).run();
 * </pre></code> from your <code>main()</code> method.
 * <p>
 * </p>
 * By default, listens on all interfaces; call {@link #localhostOnly()} to only
 * listen on localhost.
 */
public class VaadinBoot {

	/**
	 * The default port where Jetty will listen for http:// traffic.
	 */
	private static final int DEFAULT_PORT = 8080;

	private static final Logger log = LoggerFactory.getLogger(VaadinBoot.class);

	private static Resource findWebRoot() throws MalformedURLException {
		// don't look up directory as a resource, it's unreliable:
		// https://github.com/eclipse/jetty.project/issues/4173#issuecomment-539769734
		// instead we'll look up the /webapp/ROOT and retrieve the parent folder from
		// that.
		final URL f = VaadinBoot.class.getResource("/webapp/ROOT");
		if (f == null) {
			throw new IllegalStateException(
					"Invalid state: the resource /webapp/ROOT doesn't exist, has webapp been packaged in as a resource?");
		}
		final String url = f.toString();
		if (!url.endsWith("/ROOT")) {
			throw new RuntimeException("Parameter url: invalid value " + url + ": doesn't end with /ROOT");
		}
		log.debug("/webapp/ROOT is " + f);

		// Resolve file to directory
		URL webRoot = new URL(url.substring(0, url.length() - 5));
		log.debug("WebRoot is " + webRoot);
		return Resource.newResource(webRoot);
	}

	private static boolean isProductionMode() {
		// try checking for flow-server-production-mode.jar on classpath
		final String probe = "META-INF/maven/com.vaadin/flow-server-production-mode/pom.xml";
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader.getResource(probe) != null) {
			return true;
		}

		// Gradle plugin doesn't add flow-server-production-mode.jar to production
		// build. Try loading flow-build-info.json instead.
		final URL flowBuildInfoJson = classLoader.getResource("META-INF/VAADIN/config/flow-build-info.json");
		if (flowBuildInfoJson != null) {
			try {
				final String json = IOUtils.toString(flowBuildInfoJson, StandardCharsets.UTF_8);
				if (json.contains("\"productionMode\": true")) {
					return true;
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return false;
	}

	Logger logger = LoggerFactory.getLogger(VaadinBoot.class);

	/**
	 * The port where Jetty will listen for http:// traffic.
	 */
	protected int port = DEFAULT_PORT;

	/**
	 * When the app launches, open the browser automatically when in dev mode.
	 */
	//    private boolean openBrowserInDevMode = true;

	/**
	 * The VaadinServlet.
	 */

	Class<? extends Servlet> servlet;

	/**
	 * Listen on interface handling given host name. Defaults to null which causes
	 * Jetty to listen on all interfaces.
	 */
	@Nullable
	private String hostName = null;

	/**
	 * The context root to run under. Defaults to `/`. Change this to e.g. /foo to
	 * host your app on a different context root
	 */

	private String contextRoot = "/";

	// mark volatile: might be accessed by the shutdown hook from a different
	// thread.
	private volatile Server server;

	private long startupMeasurementSince;

	private String appName;

	/**
	 * Creates the new instance of the Boot launcher.
	 */
	public VaadinBoot() {
		try {
			servlet = Class.forName("com.vaadin.flow.server.VaadinServlet").asSubclass(Servlet.class);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the URL where the app is running, for example
	 * <code>http://localhost:8080/app</code>.
	 *
	 * @return the server URL, not null.
	 */

	public String getServerURL() {
		// // Vaadin is confused by multiple applications running on same host, different ports.
		// // Fake a host number if running locally
		// int hostNum = (port >= 8080 ? port+16 : port) % 253 + 1;
		// return "http://127.0.0." + hostNum + ":" + port + contextRoot;

		return "http://localhost:" + port + contextRoot;
	}

	/**
	 * Listen on network interface handling given host name. Pass in null to listen
	 * on all interfaces; pass in `127.0.0.1` or `localhost` to listen on localhost
	 * only (or call {@link #localhostOnly()}).
	 *
	 * @param hostName the interface to listen on.
	 * @return this
	 */

	public VaadinBoot listenOn(@Nullable String hostName) {
		this.hostName = hostName;
		return this;
	}

	/**
	 * Listen on the <code>localhost</code> network interface only.
	 *
	 * @return this
	 */

	public VaadinBoot localhostOnly() {
		return listenOn("localhost");
	}

	/**
	 * When the app launches, open the browser automatically when in dev mode.
	 *
	 * @param openBrowserInDevMode defaults to true.
	 * @return this
	 */

	public VaadinBoot openBrowserInDevMode(boolean openBrowserInDevMode) {
		//        this.openBrowserInDevMode = openBrowserInDevMode;
		return this;
	}

	/**
	 * Runs your app. Blocks until the user presses Enter or CTRL+C.
	 * <p>
	 * </p>
	 * WARNING: this function may never terminate since the entire JVM may be killed
	 * on CTRL+C.
	 */
	public void run() throws Exception {
		start();

		// We want to shut down the app cleanly by calling stop().
		// Unfortunately, that's not easy. When running from:
		// * Intellij as a Java app: CTRL+C doesn't work but Enter does.
		// * ./gradlew run: Enter doesn't work (no stdin); CTRL+C kills the app
		// forcibly.
		// * ./mvnw exec:java: both CTRL+C and Enter works properly.
		// * cmdline as an unzipped app (production): both CTRL+C and Enter works
		// properly.
		// Therefore, we'll use a combination of the two.

		// this gets called both when CTRL+C is pressed, and when main() terminates.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stop("Shutdown hook called, shutting down")));
		System.out.println("Press CTRL+C to shutdown");

		//        if (openBrowserInDevMode && !isProductionMode()) {
		Open.open(getServerURL());
		//        }

		// Await for Enter. ./gradlew run offers no stdin and read() will return
		// immediately with -1
		if (System.in.read() == -1) {
			// running from Gradle
			System.out.println("Press CTRL+C to shutdown");
			server.join(); // blocks endlessly
		} else {
			stop("Shutting down");
		}
	}

	/**
	 * Sets the port to listen on. Listens on {@value #DEFAULT_PORT} by default.
	 *
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
	 * Starts the Jetty server and your app. Blocks until the app is fully started,
	 * then resumes execution. Mostly used for testing.
	 */
	public void start() throws Exception {
		startupMeasurementSince = System.currentTimeMillis();

		// detect&enable production mode
		if (isProductionMode()) {
			// fixes https://github.com/mvysny/vaadin14-embedded-jetty/issues/1
			System.setProperty("vaadin.productionMode", "true");
		}

		fixClasspath();

		final WebAppContext context = createWebAppContext();
		context.getSessionHandler().setSessionCookie(getAppName() != null ? getAppName() : "V"+System.currentTimeMillis());

		if (hostName != null) {
			server = new Server(new InetSocketAddress(hostName, port));
		} else {
			server = new Server(port);
		}
		server.setHandler(context);
		server.start();
		onStarted();
	}

	/**
	 * Stops your app. Blocks until the webapp is fully stopped. Mostly used for
	 * tests.
	 *
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

	public VaadinBoot withAppName(String arg) {
		setAppName(arg);
		return this;
	}

	/**
	 * Parses given command-line parameters. At the moment only the port number is
	 * parsed out if the array is non-empty.
	 *
	 * @param args the command-line parameters, not null.
	 * @return this
	 */

	public VaadinBoot withArgs(String[] args) {
		if (args.length >= 1) {
			setPort(Integer.parseInt(args[0]));
		}
		return this;
	}

	/**
	 * Change this to e.g. /foo to host your app on a different context root
	 *
	 * @param contextRoot the new context root, e.g. `/foo`.
	 * @return this
	 */

	public VaadinBoot withContextRoot(String contextRoot) {
		this.contextRoot = Objects.requireNonNull(contextRoot);
		return this;
	}

	/**
	 * Bootstraps custom servlet instead of the default
	 * <code>com.vaadin.flow.server.VaadinServlet</code>.
	 *
	 * @param vaadinServlet the custom servlet, not null.
	 * @return this
	 */

	public VaadinBoot withServlet(Class<? extends Servlet> vaadinServlet) {
		this.servlet = Objects.requireNonNull(vaadinServlet);
		return this;
	}

	/**
	 * Creates the Jetty {@link WebAppContext}.
	 *
	 * @return the {@link WebAppContext}
	 */

	protected WebAppContext createWebAppContext() throws MalformedURLException {
		final WebAppContext context = new WebAppContext();
		context.setBaseResource(findWebRoot());
		context.setContextPath(contextRoot);
		context.addServlet(servlet, "/*");
		// this will properly scan the classpath for all @WebListeners, including the
		// most important
		// com.vaadin.flow.server.startup.ServletContextListeners.
		// See also https://mvysny.github.io/vaadin-lookup-vs-instantiator/
		context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*\\.jar|.*/classes/.*");
		context.setConfigurationDiscovered(true);
		context.getServletContext().setExtendedListenerTypes(true);
		return context;
	}

	protected void fixClasspath() {
		// see https://github.com/mvysny/vaadin-boot/issues/1
		final String classpath = System.getProperty("java.class.path");
		if (classpath != null) {
			final String[] entries = classpath.split("[" + File.pathSeparator + "]");
			final String filteredClasspath = Arrays.stream(entries)
					.filter(it -> !it.isBlank() && new File(it).exists())
					.collect(Collectors.joining(File.pathSeparator));
			System.setProperty("java.class.path", filteredClasspath);
		}
	}

	protected void onStarted() {
		final Duration startupDuration = Duration.ofMillis(System.currentTimeMillis() - startupMeasurementSince);
		logger.info("Started in {} ms", startupDuration);
	}

	public void setAppName(String arg) {
		this.appName = arg;
	}

	public String getAppName() {
		return appName;
	}
}
