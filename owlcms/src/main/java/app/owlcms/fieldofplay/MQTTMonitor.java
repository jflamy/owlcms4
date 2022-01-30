package app.owlcms.fieldofplay;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
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

import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class MQTTMonitor {

    private MqttClient client;
    private FieldOfPlay fop;
    private Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);
    private String decisionTopicName;
    private String userName;
    private String password;
    private String server;
    private String port;

    MQTTMonitor(FieldOfPlay fop) {
        logger.setLevel(Level.DEBUG);
        this.setFop(fop);
        fop.getUiEventBus().register(this);

        try {
            server = StartupUtils.getStringParam("mqttServer");
            port = StartupUtils.getStringParam("mqttPort");
            client = new MqttClient(
                    "tcp://" +
                    (server != null ? server : "test.mosquitto.org") +
                    ":" +
                    (port != null ? port : "1883"),
                    MqttClient.generateClientId(), // ClientId
                    new MemoryPersistence()); // Persistence
            doConnect();
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

    private void doConnect() throws MqttSecurityException, MqttException {
        userName = StartupUtils.getStringParam("mqttUserName");
        password = StartupUtils.getStringParam("mqttPassword");
        MqttConnectOptions connOpts = setUpConnectionOptions(userName != null ? userName : "", password != null ? password : "");
        client.connect(connOpts);
        
        ledOnOff();
        client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable cause) {
                logger.debug("{}lost connection to MQTT: {}", fop.getLoggingName(), cause.getLocalizedMessage());
                // Called when the client lost the connection to the broker
                while (!client.isConnected()) {
                    try {
                        client.connect();
                    } catch (Exception e1) {
                        logger.error("{}cannot reconnect MQTT: {}", fop.getLoggingName(), LoggerUtils.stackTrace(e1));
                    }
                    sleep(1000);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {// Called when a outgoing publish is complete
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                new Thread(() -> {
                    String messageStr = new String(message.getPayload(), StandardCharsets.UTF_8);
                    logger.debug("{}{} : {}", fop.getLoggingName(), topic, messageStr);
                    if (topic.endsWith(decisionTopicName)) {
                        String[] parts = messageStr.split(" ");
                        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, Integer.parseInt(parts[0])-1,
                                parts[1].contentEquals("good")));
                    }
                }).start();
            }
        });
        String topicFilter = "owlcms/decision/" + fop.getName();
        client.subscribe(topicFilter);
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
        return connOpts;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
    
    @Subscribe
    public void slaveWakeUpRef(UIEvent.WakeUpRef e) {
        logger.warn("slaveWakeUp {}",e.on);
        try {
            String topic = "owlcms/decisionRequest/" + fop.getName() + "/" + (e.ref+1);
            client.publish(topic, new MqttMessage((e.on ? "on" : "off").getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {
            logger.error("could not publish wakeup {}", e1.getCause());
        }
    }

}
