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
        
        String expectedUserName = Config.getCurrent().getMqttUserName();
        if (expectedUserName == null || expectedUserName.isBlank()) {
            // no check, anonymous allowed
            logger.warn("no user name configured, anonymous MQTT access allowed");
            return true;
        }
        if (!expectedUserName.contentEquals(username)) {
            // wrong user name provided
            logger./**/warn("wrong MQTT user is denied: {}",username);
            return false;
        }

        String expectedClearTextPassword = StartupUtils.getStringParam("mqttPassword");
        logger.warn("client password string : {}", clientPasswordString);
        if (expectedClearTextPassword != null) {
            // clear text comparison
            boolean plainTextMatch = expectedClearTextPassword.contentEquals(clientPasswordString);
            logger.debug("clear text match {}",plainTextMatch);
            return plainTextMatch;
        } else if (clientPasswordString.contentEquals(Main.mqttStartup)) {
            // special case -- owlcms is calling it's own moquette locally
            logger.warn("owlcms MQTT connection {}",clientPasswordString);
            return true;
        } else {
            String dbHashedPassword = Config.getCurrent().getMqttPassword();
            String hashedPassword = Config.getCurrent().encodeUserPassword(clientPasswordString, dbHashedPassword);
            if (dbHashedPassword == null || dbHashedPassword.isBlank()) {
                logger.warn("no password configured, MQTT access allowed to {}",username);
                return true;
            }  else  {
                boolean shaMatch = dbHashedPassword.contentEquals(hashedPassword);
                logger.warn("correct password provided, MQTT access allowed to {}", username);
                return shaMatch;
            }
        }
    }

}
