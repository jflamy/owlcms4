package app.owlcms.init;

import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import io.moquette.broker.security.IAuthenticator;

public class MoquetteAuthenticator implements IAuthenticator {
    Logger logger = (Logger) LoggerFactory.getLogger(MoquetteAuthenticator.class);

    @Override
    public boolean checkValid(String clientId, String username, byte[] password) {
        String clientPasswordString = new String(password, StandardCharsets.UTF_8);
        
        if (clientPasswordString.contentEquals(Main.mqttStartup)) {
            // special case -- owlcms is calling it's own moquette locally
            // the shared secret is the milliseconds at which the server started.
            logger.debug("owlcms MQTT connection {}",clientPasswordString);
            return true;
        } 
        
        String expectedUserName = Config.getCurrent().getMqttUserName();
        if (expectedUserName == null || expectedUserName.isBlank()) {
            // no check, anonymous allowed
            logger.debug("no user name configured, anonymous MQTT access allowed");
            return true;
        }
        if (!expectedUserName.contentEquals(username)) {
            // wrong user name provided
            logger./**/warn("wrong MQTT username, {} is denied: {}",clientId, username);
            return false;
        }

        String expectedClearTextPassword = StartupUtils.getStringParam("mqttPassword");
        logger.trace("client password string : {}", clientPasswordString);
        if (expectedClearTextPassword != null) {
            // clear text comparison
            boolean plainTextMatch = expectedClearTextPassword.contentEquals(clientPasswordString);
            logger.debug("clear text match {}",plainTextMatch);
            return plainTextMatch;
        } else {
            String dbHashedPassword = Config.getCurrent().getMqttPassword();
            if (dbHashedPassword == null || dbHashedPassword.isBlank()) {
                logger.debug("no password configured, MQTT access allowed to {}",username);
                return true;
            }  else  {
                String hashedPassword = Config.getCurrent().encodeUserPassword(clientPasswordString, dbHashedPassword);
                boolean shaMatch = dbHashedPassword.contentEquals(hashedPassword);
                if (shaMatch) {
                    logger.debug("correct password provided, MQTT access allowed to {}", username);
                } else {
                    logger./**/warn("wrong MQTT password, incorrect password from {}", clientId);
                }
                return shaMatch;
            }
        }
    }

}
