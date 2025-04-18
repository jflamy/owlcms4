/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import java.net.URL;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class LogbackConfigReloader {
	private static final String[] CONFIG_FILES = {
	        "logback.xml",
	        "logback-test.xml"
	};

	public static void reloadLogbackConfiguration() {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			URL configUrl = findLogbackConfigurationUrl();

			if (configUrl == null) {
				System.err./**/println("No Logback configuration file found");
				return;
			}

			// Reset the current configuration
			loggerContext.reset();

			// Create a new configurator
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(loggerContext);

			// Reconfigure using the original configuration URL
			configurator.doConfigure(configUrl);

			System.out./**/println("Logback configuration reloaded from: " + configUrl);

		} catch (JoranException e) {
			System.err./**/println("Error reloading Logback configuration: " + e.getMessage());
		}
	}

	private static URL findLogbackConfigurationUrl() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		// Check system property first
		String explicitConfigFile = System.getProperty("logback.configurationFile");
		if (explicitConfigFile != null) {
			return classLoader.getResource(explicitConfigFile);
		}

		// Try predefined list of configuration files
		for (String configFile : CONFIG_FILES) {
			URL url = classLoader.getResource(configFile);
			if (url != null) {
				return url;
			}
		}
		return null;
	}
}