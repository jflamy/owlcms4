package app.owlcms.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.nio.ByteBuffer;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import jakarta.websocket.ClientEndpointConfig;

import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.StartupUtils;
import jakarta.websocket.Endpoint;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = MqttWebSocketProxyEndpoint.MQTT, subprotocols = { "mqtt" })
public class MqttWebSocketProxyEndpoint {

    public static final String MQTT = "/mqtt";
    private volatile Session targetSession;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(MqttWebSocketProxyEndpoint.class);

    @OnOpen
    public void onOpen(Session session) {
        try {
            // Ensure database and application initialization completed before handling incoming proxy opens
            OwlcmsFactory.waitDBInitialized();
        } catch (Throwable t) {
            logger.warn("Error while waiting for DB initialization: {}", t.getMessage());
        }
        try {
            logger.debug("Client negotiated subprotocol: {}", session.getNegotiatedSubprotocol());
            try {
                // Ensure the inbound client session uses the same conservative timeout as outbound
                session.setMaxIdleTimeout(app.owlcms.jetty.EmbeddedJetty.PROXY_TIMEOUT_DEFAULT_MS);
                logger.debug("Set client session max idle timeout to {} ms", app.owlcms.jetty.EmbeddedJetty.PROXY_TIMEOUT_DEFAULT_MS);
            } catch (Throwable t) {
                logger.warn("Unable to set inbound session idle timeout: {}", t.getMessage());
            }
            // WebSocket proxy uses default timeout - MQTT keepalive is handled by Moquette broker
            String target = StartupUtils.getStringParam("mqttWsTarget");
            if (target == null || target.isBlank()) {
                target = "ws://127.0.0.1:" + Config.getCurrent().getParamMqttWsPort() + MqttWebSocketProxyEndpoint.MQTT;
            }
            // Connect outbound to MQTT broker
            ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                    .preferredSubprotocols(Collections.singletonList("mqtt"))
                    .build();
            // pass the client session to the outbound endpoint via user properties

            // The WebSocketContainer default session timeout is configured during server startup (EmbeddedJetty).
            try {
                ContainerProvider.getWebSocketContainer().connectToServer(new TargetEndpoint(session), config, URI.create(target));
            } catch (Exception ex) {
                throw ex;
            }

            logger.debug("Proxy established: client connected, connecting to MQTT broker {}", target);
        } catch (Exception e) {
            logger.error("Failed to connect to MQTT broker: {}", e.getMessage(), e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Failed to connect to MQTT broker"));
            } catch (IOException ex) {
                logger.error("Error closing session after failed MQTT connect", ex);
            }
        }
    }
    @OnMessage
    public void onTextMessage(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace("PROXY → MQTT: Text message ({} chars): {}", message.length(),
                message.length() > 100 ? message.substring(0, 100) + "..." : message);
        }
        // Forward text frames unmodified to the broker
        if (targetSession != null && targetSession.isOpen()) {
            try {
                targetSession.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.error("Error forwarding text message to MQTT broker: {}", e.getMessage(), e);
            }
        } else {
            logger.error("Cannot forward text message: target session is null or closed");
        }
    }

    @OnMessage
    public void onBinaryMessage(Session session, ByteBuffer buffer) {
        if (logger.isTraceEnabled()) {
            logger.trace("PROXY → MQTT: Binary message ({} bytes)", buffer.remaining());
        }
        // Forward binary frames unmodified to the broker
        if (targetSession != null && targetSession.isOpen()) {
            try {
                targetSession.getBasicRemote().sendBinary(buffer);
            } catch (IOException e) {
                logger.error("Error forwarding binary message to MQTT broker: {}", e.getMessage(), e);
            }
        } else {
            logger.error("Cannot forward binary message: target session is null or closed");
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        logger.debug("Client connection closed: {}", reason);
        try {
            if (targetSession != null && targetSession.isOpen()) {
                int code = reason != null && reason.getCloseCode() != null ? reason.getCloseCode().getCode() : -1;
                // Treat these codes as non-transmittable and forward NORMAL_CLOSURE instead
                if (code == 1006 || code == 1003 || code == 1007) {
                    targetSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client disconnected"));
                } else {
                    targetSession.close(reason);
                }
            }
        } catch (IOException e) {
            logger.error("Error closing target session on client close", e);
        }
    }

    @OnError
    public void onError(Session session, Throwable cause) {
        // Ignore EOF/ClosedChannelException noise from Jetty's websocket internals
        Throwable root = cause;
        while (root.getCause() != null) root = root.getCause();
        String sid = session != null ? session.getId() : "<null>";
        if (root instanceof java.nio.channels.ClosedChannelException || root.getClass().getName().contains("WebSocketSessionState")) {
            if (logger.isDebugEnabled()) {
                logger.debug("WebSocket EOF/closed channel (sessionId={}), ignoring: {}", sid, root.toString());
            }
            return;
        }
        logger.error("WebSocket error (sessionId={})", sid, cause);
    }

    public class TargetEndpoint extends Endpoint {
        private final Session clientSession;

        public TargetEndpoint(Session clientSession) {
            this.clientSession = clientSession;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            targetSession = session;
            try {
                // WebSocket proxy uses default timeout - MQTT keepalive is handled by Moquette broker
                session.setMaxIdleTimeout(app.owlcms.jetty.EmbeddedJetty.PROXY_TIMEOUT_DEFAULT_MS);
                logger.debug("Connected to MQTT broker: {}", session.getRequestURI());
            } catch (Throwable t) {
                logger.error("Unable to set outbound session idle timeout: {}", t.getMessage());
            }
            logger.debug("Broker negotiated subprotocol: {}", session.getNegotiatedSubprotocol());

            // Text messages
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("MQTT → PROXY: Text message ({} chars): {}", message.length(),
                            message.length() > 100 ? message.substring(0, 100) + "..." : message);
                    }
                    if (clientSession != null && clientSession.isOpen()) {
                        try {
                            clientSession.getBasicRemote().sendText(message);
                        } catch (IOException e) {
                            logger.error("Error forwarding text message to client: {}", e.getMessage(), e);
                        }
                    } else {
                        logger.error("Cannot forward text message: client session is null or closed");
                    }
                }
            });

            // Binary messages
            session.addMessageHandler(ByteBuffer.class, new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer buffer) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("MQTT → PROXY: Binary message ({} bytes)", buffer.remaining());
                    }
                    if (clientSession != null && clientSession.isOpen()) {
                        try {
                            clientSession.getBasicRemote().sendBinary(buffer);
                        } catch (IOException e) {
                            logger.error("Error forwarding binary message to client: {}", e.getMessage(), e);
                        }
                    } else {
                        logger.error("Cannot forward binary message: client session is null or closed");
                    }
                }
            });
        }

        @Override
        public void onClose(Session session, CloseReason reason) {
            logger.info("MQTT broker connection closed: {}", reason);
            try {
                if (clientSession != null && clientSession.isOpen()) {
                    int code = reason != null && reason.getCloseCode() != null ? reason.getCloseCode().getCode() : -1;
                    if (code == 1006 || code == 1003 || code == 1007) {
                        clientSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "MQTT broker disconnected"));
                    } else {
                        clientSession.close(reason);
                    }
                }
            } catch (IOException e) {
                logger.error("Error closing client session on MQTT close", e);
            }
        }

        @Override
        public void onError(Session session, Throwable cause) {
            logger.error("MQTT WebSocket error (sessionId={})", session != null ? session.getId() : "<null>", cause);
        }
    }
}
