package app.owlcms.fieldofplay;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class MQTTMonitor {

    // TODO for testing until we add the selection of field play on the devices
    private static boolean alreadyOne = false;

    private MqttClient client;
    private FieldOfPlay fop;
    private Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);

    MQTTMonitor(FieldOfPlay fop) {
        if (alreadyOne) {
            return;
        }
        logger.setLevel(Level.DEBUG);
        alreadyOne = true;
        this.setFop(fop);
        try {
            client = new MqttClient(
                    "tcp://test.mosquitto.org:1883", // URI
                    MqttClient.generateClientId(), // ClientId
                    new MemoryPersistence()); // Persistence
            doConnect();
            client.publish("owlcms/decisionRequest", new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
            sleep(1000);
            client.publish("owlcms/decisionRequest", new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e) {
            logger.error("cannot initialize MQTT: {}", LoggerUtils.stackTrace(e));
        }

    }

    public FieldOfPlay getFop() {
        return fop;
    }

    public void setFop(FieldOfPlay fop) {
        this.fop = fop;
    }

    private void doConnect() throws MqttSecurityException, MqttException {
        client.connect();
        client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable cause) {
                // Called when the client lost the connection to the broker
                while (!client.isConnected()) {
                    try {
                        client.connect();
                    } catch (Exception e1) {
                        logger.error("cannot reconnect MQTT: {}", LoggerUtils.stackTrace(e1));
                    }
                    sleep(1000);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {// Called when a outgoing publish is complete
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                logger.debug("{} : {}", topic, new String(message.getPayload(),StandardCharsets.UTF_8));
            }
        });
        // TODO need to specify field of play
        client.subscribe("owlcms/decision");
        logger.info("{}connected to MQTT server {}",fop.getLoggingName(),client.getCurrentServerURI());
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
