/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import ch.qos.logback.classic.Logger;

public final class RestartUtils {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(RestartUtils.class);
	private static final ComparableVersion MIN_CONTROL_PANEL_RESTART_VERSION = new ComparableVersion("3.1.0-alpha00");
	private static final long EXIT_FLUSH_DELAY_MS = 500L;

	private RestartUtils() {
	}

	public static boolean isRestartScenario() {
		String controlPanelVersion = System.getenv("OWLCMS_CONTROLPANEL");
		if (controlPanelVersion == null || controlPanelVersion.trim().isEmpty()) {
			return false;
		}

		try {
			ComparableVersion currentVersion = new ComparableVersion(controlPanelVersion);
			return currentVersion.compareTo(MIN_CONTROL_PANEL_RESTART_VERSION) >= 0;
		} catch (Exception e) {
			logger.error("Error checking control panel version '{}': {}", controlPanelVersion, e.getMessage());
			return false;
		}
	}

	public static void triggerRestart(String reason) {
		if (reason == null || reason.isBlank()) {
			logger.info("Triggering restart via System.exit(1)");
			System.err.println("OWLCMS: Triggering restart via System.exit(1)");
		} else {
			logger.info("Triggering restart via System.exit(1): {}", reason);
			System.err.println("OWLCMS: Triggering restart via System.exit(1) - " + reason);
		}
		Main.prepareForExit();
		try {
			Thread.sleep(EXIT_FLUSH_DELAY_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		System.exit(1);
	}

	public static void triggerRestartIfNeeded(String reason) {
		if (!isRestartScenario()) {
			return;
		}
		triggerRestart(reason);
	}
}