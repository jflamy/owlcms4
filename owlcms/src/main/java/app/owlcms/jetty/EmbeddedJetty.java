/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.jetty;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import jakarta.websocket.ContainerProvider;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.apputils.LogbackConfigReloader;
import app.owlcms.endpoints.ControlPanelServlet;
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
		context.addServlet(ControlPanelServlet.class, "/controlpanel/stop");
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

		// this gets called both when CTRL+C is pressed, and when main() terminates.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			stop("Shutdown hook called, shutting down");
			if (Main.isIntentionalSignalReceived()) {
				// Force exit 0 so Docker on-failure and other supervisors treat
				// SIGTERM/SIGINT as an intentional stop rather than a crash.
				startLogger.info("Intentional signal received — exiting with code 0");
				Runtime.getRuntime().halt(0);
			}
		}));
		//startLogger.info("Press CTRL+C to shutdown");

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
