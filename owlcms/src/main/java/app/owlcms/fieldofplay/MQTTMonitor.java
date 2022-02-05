package app.owlcms.fieldofplay;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class MQTTMonitor {

    private MqttAsyncClient client;
    private String decisionTopicName;
    private FieldOfPlay fop;
    private Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);
    private String[] macAddress = new String[3];
    private String password;
    private String port;
    private String server;
    private String userName;

    MQTTMonitor(FieldOfPlay fop) {
        logger.setLevel(Level.DEBUG);
        this.setFop(fop);
        fop.getUiEventBus().register(this);

        try {
            server = StartupUtils.getStringParam("mqttServer");
            port = StartupUtils.getStringParam("mqttPort");
            client = new MqttAsyncClient(
                    "tcp://" +
                            (server != null ? server : "test.mosquitto.org") +
                            ":" +
                            (port != null ? port : "1883"),
                    MqttClient.generateClientId(), // ClientId
                    new MemoryPersistence()); // Persistence
            connectionLoop();
        } catch (MqttException e) {
            logger.error("cannot initialize MQTT: {}", LoggerUtils.stackTrace(e));
        }
    }

    public FieldOfPlay getFop() {
        return fop;
    }

    public void setFop(FieldOfPlay fop) {
        this.fop = fop;
        this.decisionTopicName = "/decision/" + fop.getName();
    }

    @Subscribe
    public void slaveSummonRef(UIEvent.SummonRef e) {
        logger.debug("{}MQTT summon {} {}", fop.getLoggingName(), e.ref, e.on);
        try {
            String topic = "owlcms/summon/" + fop.getName() + "/" + (e.ref);
            //String refMacAddress = macAddress[e.ref-1];
            // insert target device mac address for cross-check
            client.publish(topic, new MqttMessage(
                    ((e.on ? "on" : "off") 
                            //+ (refMacAddress != null ? " " + refMacAddress : "")
                            )
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {
            logger.error("could not publish summon {}", e1.getCause());
        }
    }

    @Subscribe
    public void slaveWakeUpRef(UIEvent.WakeUpRef e) {
        logger.warn("slaveWakeUp {}", e.on);
        try {
            String topic = "owlcms/decisionRequest/" + fop.getName() + "/" + (e.ref + 1);
            // String refMacAddress = macAddress[e.ref];
            client.publish(topic, new MqttMessage(
                    ((e.on ? "on" : "off")
                            /* + (refMacAddress != null ? " " + refMacAddress : "")*/) 
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {
            logger.error("could not publish wakeup {}", e1.getCause());
        }
    }

    private void connectionLoop() {
        while (!client.isConnected()) {
            try { 
                // doConnect will generate a new client Id, and wait for completion
                // client.reconnect() and automaticReconnection do not work as I expect.
                doConnect();
            } catch (Exception e1) {
                Main.logger.error("{}MQTT refereeing device server: {}", fop.getLoggingName(), e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage());
            }
            sleep(1000);
        }
    }

    private void doConnect() throws MqttSecurityException, MqttException {
        userName = StartupUtils.getStringParam("mqttUserName");
        password = StartupUtils.getStringParam("mqttPassword");
        MqttConnectOptions connOpts = setupMQTTClient();
        client.connect(connOpts).waitForCompletion();

        ledOnOff();

        String topicFilter = "owlcms/decision/" + fop.getName();
        client.subscribe(topicFilter, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), topicFilter, client.getCurrentServerURI());
    }

    private void ledOnOff() throws MqttException, MqttPersistenceException {
        client.publish("owlcms/led/" + fop.getName(), new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
        sleep(1000);
        client.publish("owlcms/led/" + fop.getName(), new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
    }

    private MqttConnectOptions setUpConnectionOptions(String username, String password) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        if (username != null) {
            connOpts.setUserName(username);
        }
        if (password != null) {
            connOpts.setPassword(password.toCharArray());
        }
        connOpts.setCleanSession(true);
        //connOpts.setAutomaticReconnect(true);
        return connOpts;
    }
    

    private MqttConnectOptions setupMQTTClient() {
        MqttConnectOptions connOpts = setUpConnectionOptions(userName != null ? userName : "",
                password != null ? password : "");
        client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable cause) {
                logger.debug("{}lost connection to MQTT: {}", fop.getLoggingName(), cause.getLocalizedMessage());
                // Called when the client lost the connection to the broker
                connectionLoop();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {// Called when a outgoing publish is complete
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                new Thread(() -> {
                    String messageStr = new String(message.getPayload(), StandardCharsets.UTF_8);
                    logger.warn("{}{} : {}", fop.getLoggingName(), topic, messageStr);
                    if (topic.endsWith(decisionTopicName)) {
                        messageStr = messageStr.trim();
                        try {
                            String[] parts = messageStr.split(" ");
                            int refIndex = Integer.parseInt(parts[0]) - 1;
                            fop.fopEventPost(new FOPEvent.DecisionUpdate(this, refIndex,
                                    parts[parts.length-1].contentEquals("good")));
                            macAddress[refIndex] = parts[1];
                        } catch (NumberFormatException e) {
                            logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                                    fop.getLoggingName(), topic, messageStr);
                        }
                    }
                }).start();
            }
        });
        return connOpts;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
    

}
