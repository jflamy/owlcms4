package app.owlcms.monitors;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import ch.qos.logback.classic.Logger;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;

/**
 * Encapsulates MQTT intercept handlers previously declared in Main.java.
 */
public class MQTTInterceptHandlers {

    // Centralized static storage for connection description tracking
    private static final java.util.Map<String, Long> globalActiveClientIds = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, String> connectionDescriptors = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Long> connectionLastSeen = new java.util.concurrent.ConcurrentHashMap<>();
    // best-effort transport mapping (clientId -> transport string, e.g. "tcp" or "ws")
    private static final java.util.Map<String, String> connectionTransport = new java.util.concurrent.ConcurrentHashMap<>();
    // (remote address capture removed) - broker does not expose originating IP reliably

    public static void notifyGlobalClientConnected(String clientId) {
        if (clientId == null || clientId.isBlank()) return;
        globalActiveClientIds.put(clientId, System.currentTimeMillis());
        try {
            // Ignore descriptor updates for server-originated connections (contain "_owlcms_")
            if (isServerClientId(clientId) || isConfigClientId(clientId)) {
                // don't add server/system or raw config ids
            } else if (isGenericClientId(clientId)) {
                // Generic mqtt clients (e.g. mqttjs_*) get a generic 'mqtt' descriptor
                connectionDescriptors.put(clientId, "mqtt");
                connectionLastSeen.put(clientId, System.currentTimeMillis());
            } else {
                // Descriptive client IDs: use clientId itself as descriptor
                connectionDescriptors.put(clientId, clientId);
                connectionLastSeen.put(clientId, System.currentTimeMillis());
            }
        } catch (Throwable t) {}
    try { LoggerFactory.getLogger(MQTTInterceptHandlers.class).debug("MQTT client connected: clientId={} globalActiveCount={}", clientId, globalActiveClientIds.size()); } catch (Throwable t) {}
    }

    // All attempts to extract a remote IP have been removed — broker does not reliably provide it

    public static void notifyGlobalClientDisconnected(String clientId) {
        if (clientId == null || clientId.isBlank()) return;
        globalActiveClientIds.remove(clientId);
        connectionDescriptors.remove(clientId);
        connectionLastSeen.remove(clientId);
    try { LoggerFactory.getLogger(MQTTInterceptHandlers.class).info("MQTT client disconnected: clientId={} globalActiveCount={}", clientId, globalActiveClientIds.size()); } catch (Throwable t) {}
    }

    public static java.util.Set<String> getGlobalActiveClientIds() {
        return new java.util.HashSet<>(new java.util.TreeSet<>(globalActiveClientIds.keySet()));
    }

    public static java.util.Map<String, String> getConnectionDescriptorsSnapshot() {
        return new java.util.HashMap<>(connectionDescriptors);
    }

    public static java.util.Map<String, Long> getConnectionLastSeenSnapshot() {
        return new java.util.HashMap<>(connectionLastSeen);
    }

    // remote-address API removed

    public static void putDescriptor(String clientId, String desc) {
        if (clientId == null) return;
        if (isConfigClientId(clientId)) return;
        // Ignore descriptor updates for server-originated connections (contain "_owlcms_")
        if (isServerClientId(clientId)) return;
        connectionDescriptors.put(clientId, desc);
    }

    public static void putLastSeen(String clientId, long ts) {
        if (clientId == null) return;
        if (isConfigClientId(clientId)) return;
        connectionLastSeen.put(clientId, ts);
    }

    public static void removeDescriptor(String clientId) {
        if (clientId == null) return;
        connectionDescriptors.remove(clientId);
    }

    public static void removeLastSeen(String clientId) {
        if (clientId == null) return;
        connectionLastSeen.remove(clientId);
    }

    public static void removeGlobalClient(String clientId) {
        if (clientId == null) return;
        globalActiveClientIds.remove(clientId);
    }

    public static void resetGlobalActiveClients() {
        globalActiveClientIds.clear();
    }

    public static boolean isGenericClientId(String clientId) {
        if (clientId == null) return false;
        String s = clientId.toLowerCase();
        return s.startsWith("mqtt") || s.startsWith("a_f_");
    }

    public static boolean isConfigClientId(String clientId) {
        if (clientId == null) return false;
        return clientId.toLowerCase().startsWith("config") && !clientId.toLowerCase().startsWith("config_f");
    }

    public static boolean isServerClientId(String clientId) {
        if (clientId == null) return false;
        return clientId.contains("_owlcms_");
    }

    public static class PublisherListener extends AbstractInterceptHandler {

        private static final Logger logger = (Logger) LoggerFactory.getLogger(MQTTInterceptHandlers.class);

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            final String decodedPayload = msg.getPayload().toString(UTF_8);
            String clientId = null;
            try {
                java.lang.reflect.Method m = msg.getClass().getMethod("getClientId");
                Object o = m.invoke(msg);
                if (o != null) clientId = o.toString();
            } catch (Throwable t1) {
                try {
                    java.lang.reflect.Method m2 = msg.getClass().getMethod("getClientID");
                    Object o2 = m2.invoke(msg);
                    if (o2 != null) clientId = o2.toString();
                } catch (Throwable t2) {
                    // no client id available
                }
            }

            String transport = clientId != null ? connectionTransport.getOrDefault(clientId, "unknown") : "server";
            logger.debug("Received on topic: {} from clientId={} transport={} content: {}", msg.getTopicName(), clientId, transport, decodedPayload);

            // Log potential recipients and their transport (snapshot)
            try {
                java.util.Set<String> recipients = getGlobalActiveClientIds();
                if (!recipients.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (String rid : recipients) {
                        String t = connectionTransport.getOrDefault(rid, "unknown");
                        if (count++ > 0) sb.append(',');
                        sb.append(rid).append('(').append(t).append(')');
                    }
                    logger.debug("Dispatching publish on topic {} to {} clients: {}", msg.getTopicName(), recipients.size(), sb.toString());
                }
            } catch (Throwable t) {
                // ignore diagnostics
            }
            try {
                clientId = null;
                try {
                    java.lang.reflect.Method m = msg.getClass().getMethod("getClientId");
                    Object o = m.invoke(msg);
                    if (o != null) clientId = o.toString();
                } catch (Throwable t1) {
                    try {
                        java.lang.reflect.Method m2 = msg.getClass().getMethod("getClientID");
                        Object o2 = m2.invoke(msg);
                        if (o2 != null) clientId = o2.toString();
                    } catch (Throwable t2) {
                        // no client id available
                    }
                }
                // remote-address probing removed; no originating IP available from broker
                MQTTMonitor.assignDescriptorForPublish(msg.getTopicName(), clientId);
            } catch (Throwable t) {
                // ignore diagnostic failures
            }
        }

        @Override
        public void onSessionLoopError(Throwable error) {
            logger.error("mqtt onSessionLoopError: " + error);
        }
    }

    public static class SubscribeListener extends AbstractInterceptHandler {

        private static final Logger logger = (Logger) LoggerFactory.getLogger(MQTTInterceptHandlers.class);

        @Override
        public String getID() {
            return "EmbeddedLauncherSubscribeListener";
        }

        @Override
        public void onSubscribe(InterceptSubscribeMessage msg) {
            try {
                String clientId = msg.getClientID();
                String topic = msg.getTopicFilter();
                String transport = clientId != null ? connectionTransport.getOrDefault(clientId, "unknown") : "unknown";
                logger.info("MQTT subscribe: clientId={} topic={} transport={}", clientId, topic, transport);
            } catch (Throwable t) {
                // ignore
            }
        }

        @Override
        public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
            try {
                String clientId = msg.getClientID();
                String topic = msg.getTopicFilter();
                String transport = clientId != null ? connectionTransport.getOrDefault(clientId, "unknown") : "unknown";
                logger.info("MQTT unsubscribe: clientId={} topic={} transport={}", clientId, topic, transport);
            } catch (Throwable t) {
                // ignore
            }
        }

        @Override
        public void onSessionLoopError(Throwable error) {
            logger.error("mqtt subscribe listener session error", error);
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
                try {
                    logger.debug("MQTT client connected: clientId={} username={} msg={}", clientId, msg.getUsername(), msg.toString());
                } catch (Throwable t) {
                    logger.debug("MQTT client connected: clientId={} username={}", clientId, msg.getUsername());
                }

                // best-effort: detect transport/protocol for this connect message via reflection
                try {
                    String transport = "unknown";
                    try {
                        java.lang.reflect.Method m = msg.getClass().getMethod("getClientInetAddress");
                        Object o = m.invoke(msg);
                        if (o != null) {
                            String s = o.toString();
                            if (s.toLowerCase().contains("websocket") || s.contains( Config.getCurrent().getParamMqttWsPort() )) transport = "ws";
                            else transport = "tcp";
                        }
                    } catch (Throwable tt) {
                        // try other methods
                        try {
                            java.lang.reflect.Method m2 = msg.getClass().getMethod("getRemoteAddress");
                            Object o2 = m2.invoke(msg);
                            if (o2 != null) {
                                String s2 = o2.toString();
                                if (s2.toLowerCase().contains("websocket") || s2.contains(Config.getCurrent().getParamMqttWsPort())) transport = "ws";
                                else transport = "tcp";
                            }
                        } catch (Throwable t2) {
                            // give up
                        }
                    }
                    if (clientId != null) connectionTransport.put(clientId, transport);
                    try { MQTTMonitor.notifyGlobalClientConnected(clientId); } catch (Throwable t) {}
                } catch (Throwable t) {
                    try { MQTTMonitor.notifyGlobalClientConnected(clientId); } catch (Throwable t2) {}
                }
            } catch (Throwable t) {
                logger.debug("Error handling onConnect message", t);
            }
        }

        @Override
        public void onDisconnect(InterceptDisconnectMessage msg) {
            try {
                String clientId = msg.getClientID();
                try {
                    logger.debug("MQTT client disconnected: clientId={} msg={}", clientId, msg.toString());
                } catch (Throwable t) {
                    logger.debug("MQTT client disconnected: clientId={}", clientId);
                }

                // Additional diagnostic logging: timestamp, transport hint, lastSeen
                try {
                    long ts = System.currentTimeMillis();
                    String transport = clientId != null ? connectionTransport.getOrDefault(clientId, "unknown") : "server";
                    Long lastSeen = connectionLastSeen.get(clientId);
                    logger.info("MQTT disconnect diagnostic: clientId={} transport={} lastSeen={} now={} wsHint={}",
                            clientId, transport, lastSeen == null ? "<null>" : lastSeen, ts,
                            "ws".equals(transport));
                } catch (Throwable t) {
                    // ignore diagnostic failures
                }

                try { MQTTMonitor.notifyGlobalClientDisconnected(clientId); } catch (Throwable t) {}
            } catch (Throwable t) {
                logger.debug("Error handling onDisconnect message", t);
            }
        }

        @Override
        public void onSessionLoopError(Throwable error) {
            logger.error("mqtt connection listener session error", error);
        }
    }
}
