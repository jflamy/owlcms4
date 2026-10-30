/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.access.PinHashes;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import io.moquette.broker.security.IAuthenticator;

public class MoquetteAuthenticator implements IAuthenticator {
	Logger logger = (Logger) LoggerFactory.getLogger(MoquetteAuthenticator.class);

	public MoquetteAuthenticator() {
		// logger.setLevel(Level.TRACE);
	}

	@Override
	public boolean checkValid(String clientId, String username, byte[] password) {
		// Moquette passes null when the client sends a user name without a password
		String clientPasswordString = password != null ? new String(password, StandardCharsets.UTF_8) : "";

		if (AccessUtils.constantTimeEquals(Main.mqttInternalSecret, password != null ? clientPasswordString : null)) {
			// special case -- owlcms is calling its own moquette locally with the secret generated at startup
			this.logger.debug("owlcms MQTT connection from {}", clientId);
			return true;
		}

		String expectedUserName = Config.getCurrent().getMqttUserName();
		if (expectedUserName == null || expectedUserName.isBlank()) {
			// no check, anonymous allowed
			this.logger.debug("no user name configured, anonymous MQTT access allowed");
			return true;
		}
		if (!expectedUserName.contentEquals(username != null ? username : "")) {
			// wrong user name provided
			this.logger./**/warn("wrong MQTT username, {} is denied: {}", clientId, username);
			return false;
		}

		String expectedClearTextPassword = StartupUtils.getStringParam("mqttPassword");
		if (expectedClearTextPassword != null) {
			// clear text comparison
			boolean plainTextMatch = AccessUtils.constantTimeEquals(expectedClearTextPassword, clientPasswordString);
			this.logger.debug("clear text match {}", plainTextMatch);
			return plainTextMatch;
		} else {
			String dbHashedPassword = Config.getCurrent().getMqttPassword();
			if (dbHashedPassword == null || dbHashedPassword.isBlank()) {
				this.logger.debug("no password configured, MQTT access allowed to {}", username);
				return true;
			} else {
				boolean shaMatch = PinHashes.matches(clientPasswordString, dbHashedPassword);
				if (shaMatch) {
					this.logger.debug("correct password provided, MQTT access allowed to {}", username);
				} else {
					this.logger./**/warn("wrong MQTT password, incorrect password from {}", clientId);
				}
				return shaMatch;
			}
		}
	}

}
