/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import org.slf4j.LoggerFactory;

import com.vaadin.open.Open;
import com.vaadin.open.Options;

import ch.qos.logback.classic.Logger;

/**
 * Utility class for browser launching with headless environment detection.
 */
public class BrowserUtils {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(BrowserUtils.class);

	/**
	 * Start browser if not running in headless/containerized environment.
	 * Detects Docker, Kubernetes, CI environments, and headless Java.
	 * 
	 * @param serverURL the URL to open in the browser
	 */
	public static void startBrowserIfAppropriate(String serverURL) {
		if (isHeadlessEnvironment()) {
			StartupUtils.getStartupLogger().info("Headless environment detected - browser will not be started.");
			return;
		}

		new Thread(() -> {
			StartupUtils.getStartupLogger().info("Starting browser.");
			Options openOptions = new Options();
			openOptions.setNewInstance(true);
			openOptions.setBackground(true);
			openOptions.setWait(false);
			Open.open(serverURL, openOptions);
		}).start();
	}

	/**
	 * Detect if running in headless/containerized environment where browser should not be started.
	 * Checks for: Docker, Kubernetes, CI, headless Java, missing DISPLAY.
	 * 
	 * @return true if headless environment detected
	 */
	public static boolean isHeadlessEnvironment() {

		// removed checks for JVM headless mode and Desktop API support because
		// they were too aggressive and blocked browser launch if one was already running
		// on Windows.

		// 1. Check JVM AWT Headless mode (covers java.awt.headless property and OS capabilities)
		// if (java.awt.GraphicsEnvironment.isHeadless()) {
		// 	logger.warn("Detected JVM headless mode");
		// 	return true;
		// }

		// 2. Check Desktop API support
		// if (!java.awt.Desktop.isDesktopSupported() || !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
		// 	logger.warn("Java Desktop API or BROWSE action not supported");
		// 	return true;
		// }

		// 3. Check for specific container/CI markers that imply no browser usage
		// Check for Docker
		if (new java.io.File("/.dockerenv").exists()) {
			logger.warn("Detected Docker environment (/.dockerenv exists)");
			return true;
		}

		// Check for missing DISPLAY on Linux/Unix (Redundant with isHeadless usually, but explicit safety)
		// On Wayland, DISPLAY might not be set but WAYLAND_DISPLAY will be.
		String os = System.getProperty("os.name", "").toLowerCase();
		if ((os.contains("nix") || os.contains("nux") || os.contains("aix")) 
				&& System.getenv("DISPLAY") == null 
				&& System.getenv("WAYLAND_DISPLAY") == null) {
			logger.warn("Detected headless Linux/Unix (DISPLAY and WAYLAND_DISPLAY not set)");
			return true;
		}

		return false;
	}

	private BrowserUtils() {
		// Utility class - prevent instantiation
	}
}
