package app.owlcms.monitors;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public final class ForwarderSetup {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(ForwarderSetup.class);
	private static final ExecutorService FORWARDER_REINITIALIZE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "Config-Forwarder-Reinitialize");
		thread.setDaemon(true);
		return thread;
	});

	private ForwarderSetup() {
	}

	public static void initializeForFop(String name, FieldOfPlay fieldOfPlay) {
		fieldOfPlay.setEventForwarder(EventForwarder.initEventForwarderByName(name, fieldOfPlay));
		fieldOfPlay.setWebSocketEventForwarder(WebSocketEventForwarder.initEventForwarderByName(name, fieldOfPlay));
	}

	public static void registerStartupDataCallbacks() {
		ForwardingDestination.logConfiguration(Config.getCurrent());
		WebSocketEventForwarder.registerStartupDataCallbacks();
	}

	public static void reinitializeIfDestinationsChanged(Config oldConfig, Config newConfig) {
		// For now, always log the resolved forwarder configuration on every config update so the
		// active URLs and where each value came from (environment variable vs database) are visible.
		ForwardingDestination.logConfiguration(newConfig);
		if (!destinationsChanged(oldConfig, newConfig)) {
			logger.debug("forwarding destinations unchanged after config update; skipping forwarder reinitialization");
			return;
		}
		FORWARDER_REINITIALIZE_EXECUTOR.submit(() -> {
			try {
				EventForwarder.reinitializeForAllFOPs();
				WebSocketEventForwarder.reinitializeForAllFOPs();
			} catch (Throwable t) {
				logger.warn("forwarder reinitialization failed after config update: {}",
				        LoggerUtils.exceptionMessage(t));
			}
		});
	}

	static boolean destinationsChanged(Config oldConfig, Config newConfig) {
		if (oldConfig == null) {
			return true;
		}
		List<ForwardingDestination> oldDestinations = ForwardingDestination.fromConfig(oldConfig);
		List<ForwardingDestination> newDestinations = ForwardingDestination.fromConfig(newConfig);
		return !oldDestinations.equals(newDestinations);
	}
}