/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.jetty;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import jakarta.websocket.ContainerProvider;
import org.slf4j.LoggerFactory;

import app.owlcms.apputils.LogbackConfigReloader;
import app.owlcms.endpoints.ControlPanelServlet;
import app.owlcms.endpoints.RememberServlet;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

public class EmbeddedJetty extends com.github.mvysny.vaadinboot.VaadinBoot {

	// Conservative default timeout for WebSocket container (5 minutes)
	public static final long PROXY_TIMEOUT_DEFAULT_MS = 5 * 60 * 1000L;

	private static Logger startLogger = (Logger) LoggerFactory.getLogger(EmbeddedJetty.class);
	private static EmbeddedJetty server;

	public static void restart() {
		if (server != null) {
			server.stop("stopping for restart");
		}
		try {
			LogbackConfigReloader.reloadLogbackConfiguration();
			startLogger.info("restarting.");
			server.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stop(boolean restart) {
		if (server != null) {
			server.stop(restart ? "stopping prior to restart." : "intentional stop.");
		}
	}

	private Runnable initConfig;
	private Runnable initData;
	private CountDownLatch latch;
	Logger logger = (Logger) LoggerFactory.getLogger(EmbeddedJetty.class);

	public EmbeddedJetty(CountDownLatch countDownLatch, String appName) {
		this.setLatch(countDownLatch);
		this.setAppName(appName);
	}

	@Override
	protected WebAppContext createWebAppContext() throws IOException {
		WebAppContext context = super.createWebAppContext();
		// Jetty ee10's ASM-based annotation scanner cannot parse Jackson's Java 23
		// multi-release classes (META-INF/versions/23/...). Jackson jars contain no
		// web/servlet annotations, so exclude them from the annotation scan.
		Object pattern = context.getAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN);
		if (pattern instanceof String patternString) {
			context.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN,
			        patternString.replace(".*\\.jar", "(?!.*jackson).*\\.jar"));
		}
		context.addServlet(ControlPanelServlet.class, "/controlpanel/stop");
		context.addServlet(RememberServlet.class, "/remember");
		return context;
	}

	public CountDownLatch getLatch() {
		return this.latch;
	}

	@Override
	public void onStarted(WebAppContext c) {
		// Ensure the WebSocket container uses the conservative default timeout set by the proxy
		try {
			ContainerProvider.getWebSocketContainer().setDefaultMaxSessionIdleTimeout(EmbeddedJetty.PROXY_TIMEOUT_DEFAULT_MS);
			startLogger.info("Configured WebSocketContainer defaultMaxSessionIdleTimeout={} ms (conservative default)", EmbeddedJetty.PROXY_TIMEOUT_DEFAULT_MS);
		} catch (Throwable t) {
			startLogger.error("Unable to set WebSocketContainer default timeout at startup: {}", t.getMessage());
		}
		StartupUtils.getStartupLogger().info("Starting OWLCMS.");
	}

	@Override
	public void run() throws Exception {
		server = this;
		start();
		// stopped by the application's shutdown hook (Main), in order with the other services and the audit log
		//startLogger.info("Press CTRL+C to shutdown");

	}

	/** Stops the server as part of the application shutdown. */
	public static void stopForShutdown() {
		if (server != null) {
			server.stop("Shutdown hook called, shutting down");
		}
	}

	public void run(Integer serverPort, String string) throws Exception {
		this.setPort(serverPort);
		this.run();
		//StartupUtils.getStartupLogger().info("Initializing translations and system settings.");
		this.initConfig.run();
		this.initData.run();
	}

	public EmbeddedJetty setInitConfig(Runnable initConfig) {
		this.initConfig = initConfig;
		return this;
	}

	public EmbeddedJetty setInitData(Runnable initData) {
		this.initData = initData;
		return this;
	}

	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public EmbeddedJetty setStartLogger(Logger startLogger) {
		EmbeddedJetty.startLogger = startLogger;
		return this;
	}

}
