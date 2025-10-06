/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.jetty;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import jakarta.websocket.ContainerProvider;
import org.slf4j.LoggerFactory;

import com.vaadin.open.Open;
import com.vaadin.open.Options;

import app.owlcms.apputils.LogbackConfigReloader;
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
		startLogger.info("started on port {}", this.getPort());
	}

	@Override
	public void run() throws Exception {
		server = this;
		start();

		// this gets called both when CTRL+C is pressed, and when main() terminates.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stop("Shutdown hook called, shutting down")));
		startLogger.info("Press CTRL+C to shutdown");

		// Open.open(getServerURL());

		new Thread(() -> {
			this.logger.info("Starting browser");
			Options openOptions = new Options();
			openOptions.setNewInstance(true);
			openOptions.setBackground(true);
			openOptions.setWait(false);
			Open.open(getServerURL(), openOptions);
			this.logger.info("Browser started");
		}).start();

	}

	public void run(Integer serverPort, String string) throws Exception {
		this.setPort(serverPort);
		this.run();
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
