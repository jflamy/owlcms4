package app.owlcms.monitors;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;

/**
 * Encapsulates MQTT intercept handlers previously declared in Main.java.
 */
public class MQTTInterceptHandlers {

    public static class PublisherListener extends AbstractInterceptHandler {

        private static final Logger logger = (Logger) LoggerFactory.getLogger(MQTTInterceptHandlers.class);

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            final String decodedPayload = msg.getPayload().toString(UTF_8);
            logger.debug("Received on topic: " + msg.getTopicName() + " content: " + decodedPayload);
        }

        @Override
        public void onSessionLoopError(Throwable error) {
            logger.error("mqtt onSessionLoopError: " + error);
        }
    }

    public static class ConnectionListener extends AbstractInterceptHandler {

        private static final Logger logger = (Logger) LoggerFactory.getLogger(MQTTInterceptHandlers.class);

        @Override
        public String getID() {
            return "EmbeddedLauncherConnectionListener";
        }

        @Override
        public void onConnect(InterceptConnectMessage msg) {
            try {
                String clientId = msg.getClientID();
                String remote = "";
                try {
                    java.lang.reflect.Method m = msg.getClass().getMethod("getClientAddress");
                    Object o = m.invoke(msg);
                    if (o != null) remote = o.toString();
                } catch (Throwable ignore) {
                }
                try {
                    logger.info("MQTT client connected: clientId={} username={} remote={} msg={}", clientId, msg.getUsername(), remote, msg.toString());
                } catch (Throwable t) {
                    logger.info("MQTT client connected: clientId={} username={} remote={}", clientId, msg.getUsername(), remote);
                }
                try { MQTTMonitor.notifyGlobalClientConnected(clientId); } catch (Throwable t) {}
            } catch (Throwable t) {
                logger.warn("Error handling onConnect message", t);
            }
        }

        @Override
        public void onDisconnect(InterceptDisconnectMessage msg) {
            try {
                String clientId = msg.getClientID();
                String remote = "";
                try {
                    java.lang.reflect.Method m = msg.getClass().getMethod("getClientAddress");
                    Object o = m.invoke(msg);
                    if (o != null) remote = o.toString();
                } catch (Throwable ignore) {
                }
                try {
                    logger.info("MQTT client disconnected: clientId={} remote={} msg={}", clientId, remote, msg.toString());
                } catch (Throwable t) {
                    logger.info("MQTT client disconnected: clientId={} remote={}", clientId, remote);
                }
                try { MQTTMonitor.notifyGlobalClientDisconnected(clientId); } catch (Throwable t) {}
            } catch (Throwable t) {
                logger.warn("Error handling onDisconnect message", t);
            }
        }

        @Override
        public void onSessionLoopError(Throwable error) {
            logger.error("mqtt connection listener session error", error);
        }
    }
}
