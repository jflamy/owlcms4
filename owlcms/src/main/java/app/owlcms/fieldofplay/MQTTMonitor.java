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
import app.owlcms.data.athlete.Athlete;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class listens and emits MQTT events.
 *
 * Events initiated by the devices start with topics that names the device (owlcms/jurybox) Devices do not listen to
 * other devices. They listen to MQTT events that come from the field of play. These events are of the form
 * (owlcms/fop). The field of play is always the last element in the topic.
 *
 * @author Jean-François Lamy
 */
public class MQTTMonitor {

    /**
     * This inner class contains the routines executed when an MQTT message is received.
     */
    private class MQTTCallback implements MqttCallback, JuryEvents {
        Athlete athleteUnderReview;
        String juryBreakTopicName;
        String juryMemberDecisionTopicName;
        String juryDecisionTopicName;
        String downEmittedTopicName;
        String decisionTopicName;
        String jurySummonTopicName;
        String deprecatedDecisionTopicName;
        String clockTopicName;

        MQTTCallback() {
            // these are the device-initiated events that the monitor tracks
            this.deprecatedDecisionTopicName = "owlcms/decision/" + fop.getName();
            this.decisionTopicName = "owlcms/refbox/decision/" + fop.getName();
            this.downEmittedTopicName = "owlcms/refbox/downEmitted/" + fop.getName();
            this.clockTopicName = "owlcms/clock/" + fop.getName();
            this.juryBreakTopicName = "owlcms/jurybox/break/" + fop.getName();
            this.juryMemberDecisionTopicName = "owlcms/jurybox/juryMember/decision/" + fop.getName();
            this.juryDecisionTopicName = "owlcms/jurybox/decision/" + fop.getName();
            this.jurySummonTopicName = "owlcms/jurybox/summon/" + fop.getName();
        }

        @Override
        public void connectionLost(Throwable cause) {
            logger.debug("{}lost connection to MQTT: {}", fop.getLoggingName(), cause.getLocalizedMessage());
            // Called when the client lost the connection to the broker
            connectionLoop();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // required by abstract class
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            new Thread(() -> {
                String messageStr = new String(message.getPayload(), StandardCharsets.UTF_8);
                logger.info("{}{} : {}", fop.getLoggingName(), topic, messageStr);

                if (topic.endsWith(decisionTopicName) || topic.endsWith(deprecatedDecisionTopicName)) {
                    postFopEventRefereeDecisionUpdate(topic, messageStr);
                } else if (topic.endsWith(downEmittedTopicName)) {
                    postFopEventDownEmitted(topic, messageStr);
                } else if (topic.endsWith(clockTopicName)) {
                    postFopTimeEvents(topic, messageStr);
                } else if (topic.endsWith(juryBreakTopicName)) {
                    postFopJuryBreakEvents(topic, messageStr);
                } else if (topic.endsWith(juryMemberDecisionTopicName)) {
                    postFopEventJuryMemberDecisionUpdate(topic, messageStr);
                } else if (topic.endsWith(juryDecisionTopicName)) {
                    postFopEventJuryDecision(topic, messageStr);
                } else if (topic.endsWith(jurySummonTopicName)) {
                    postFopEventSummonReferee(topic, messageStr);
                } else {
                    logger.error("{}Malformed MQTT unrecognized topic message topic='{}' message='{}'",
                            fop.getLoggingName(), topic, messageStr);
                }
            }).start();
        }

        /**
         * @param athleteUnderReview the athleteUnderReview to set
         */
        public void setAthleteUnderReview(Athlete athleteUnderReview) {
            this.athleteUnderReview = athleteUnderReview;
        }

        /**
         * Tell others that the refbox has given the down signal
         * @param topic
         * @param messageStr
         */
        private void postFopEventDownEmitted(String topic, String messageStr) {
            messageStr = messageStr.trim();
            fop.fopEventPost(new FOPEvent.DownSignal(this));
        }

        private void postFopEventJuryDecision(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                fop.fopEventPost(
                        new FOPEvent.JuryDecision(athleteUnderReview, this, messageStr.contentEquals("good")));
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void postFopEventJuryMemberDecisionUpdate(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                String[] parts = messageStr.split(" ");
                int refIndex = Integer.parseInt(parts[0]) - 1;
                logger.debug("JuryMemberDecisionUpdate {} {}", parts, refIndex);
                fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(MQTTMonitor.this, refIndex,
                        parts[parts.length - 1].contentEquals("good")));
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void postFopEventRefereeDecisionUpdate(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                String[] parts = messageStr.split(" ");
                int refIndex = Integer.parseInt(parts[0]) - 1;
                fop.fopEventPost(new FOPEvent.DecisionUpdate(this, refIndex,
                        parts[parts.length - 1].contentEquals("good")));
                ;
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void postFopEventSummonReferee(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                String[] parts = messageStr.split(" ");
                int refIndex = Integer.parseInt(parts[0]) - 1;
                // calling referee triggers a jury break
                postJurySummonNotification(fop, parts);
                // do the actual summoning
                fop.fopEventPost(new FOPEvent.SummonReferee(this, refIndex));
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void postFopJuryBreakEvents(String topic, String messageStr) {
            messageStr = messageStr.trim();
            if (messageStr.equalsIgnoreCase("technical")) {
                postJuryTechnicalPause(fop, this);
            } else if (messageStr.equalsIgnoreCase("deliberation")) {
                postJuryDeliberation(OwlcmsSession.getFop(), this, athleteUnderReview);
            } else if (messageStr.equalsIgnoreCase("stop")) {
                postJuryResumeCompetition(OwlcmsSession.getFop(), this, athleteUnderReview);
            } else {
                logger.error("{}Malformed MQTT clock message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void postFopTimeEvents(String topic, String messageStr) {
            messageStr = messageStr.trim();
            if (messageStr.equalsIgnoreCase("start")) {
                fop.fopEventPost(new FOPEvent.TimeStarted(this));
            } else if (messageStr.equalsIgnoreCase("stop")) {
                fop.fopEventPost(new FOPEvent.TimeStopped(this));
            } else if (messageStr.equalsIgnoreCase("60")) {
                fop.fopEventPost(new FOPEvent.ForceTime(60000, this));
            } else if (messageStr.equalsIgnoreCase("120")) {
                fop.fopEventPost(new FOPEvent.ForceTime(120000, this));
            } else {
                logger.error("{}Malformed MQTT clock message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }
    }

    private MqttAsyncClient client;
    private FieldOfPlay fop;
    private Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);
    private String password;
    private String port;
    private String server;
    private String userName;
    private MQTTCallback callback;
    private Athlete currentAthleteAtStart;
    private int currentAttemptNumber;
    private boolean newClock;
    private Athlete previousAthleteAtStart;
    private int previousAttemptNumber;

    MQTTMonitor(FieldOfPlay fop) {
        logger.setLevel(Level.DEBUG);
        this.setFop(fop);
        fop.getUiEventBus().register(this);
        fop.getFopEventBus().register(this);

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
    }

    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        // Ignored.  We reset all devices on the clock start for next attempt (resetDecisions MQTT)
//        try {
//            client.publish("owlcms/fop/" + fop.getName(),
//                    new MqttMessage("decisionReset".getBytes(StandardCharsets.UTF_8)));
//        } catch (MqttException e1) {
//        }
    }

    /**
     * A display or console has triggered the down signal (e.g. keypad connected to a laptop) and down signal post connected via MQTT.
     * @param d
     */
    @Subscribe
    public void slaveDownSignal(UIEvent.DownSignal d) {
        try {
            publishMqttDownSignal();
        } catch (MqttException e) {
        }
    }

    @Subscribe
    public void slaveJuryDecision(FOPEvent.JuryDecision jd) {
        logger.debug("MQTT monitor received FOPEvent {}", jd.getClass().getSimpleName());
    }
    
    @Subscribe
    public void slaveJuryNotification(UIEvent.JuryNotification jn) {
        // these events come from the Jury Console as UI Events
        switch (jn.getDeliberationEventType()) {
        case CALL_TECHNICAL_CONTROLLER:
            // if we ever build a TC device
            publishMqttSummonRef(4, true);
            break;
        default:
            break;
        }
    }

    @Subscribe
    public void slaveRefereeDecision(UIEvent.Decision e) {
        // the deliberation is about the last athlete judged, not on the current athlete.
        callback.setAthleteUnderReview(e.getAthlete());
    }

    @Subscribe
    public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
        // the deliberation is about the last athlete judged, not on the current athlete.
        publishMqttRefereeUpdates(e.ref1, e.ref2, e.ref3);
    }
    
    @Subscribe
    public void slaveSummonRef(UIEvent.SummonRef e) {
        // e.ref is 1..3
        // e.ref 4 is technical controller.
        int ref = e.ref;
        boolean on = e.on;
        publishMqttSummonRef(ref, on);
    }

    @Subscribe
    public void slaveTimeStarted(UIEvent.StartTime e) {
        OwlcmsSession.withFop(fop -> {
            currentAthleteAtStart = fop.getClockOwner();
            currentAttemptNumber = fop.getClockOwner().getActuallyAttemptedLifts();
            newClock = e.getTimeRemaining() == 60000 || e.getTimeRemaining() == 120000;
        });
        if ((currentAthleteAtStart != previousAthleteAtStart)
                || (currentAttemptNumber != previousAttemptNumber)
                || newClock) {
            // we switched lifter, or we switched attempt. reset the decisions.
            publishMqttResetAllDecisions();
        }
        previousAthleteAtStart = currentAthleteAtStart;
        previousAttemptNumber = currentAttemptNumber;
    }

    @Subscribe
    public void slaveWakeUpRef(UIEvent.WakeUpRef e) {
        // e.ref is 1..3
        // logger.debug("slaveWakeUp {}", e.on);
        int ref = e.ref;
        publishMqttWakeUpRef(ref, e.on);
    }

    private void connectionLoop() {
        while (!client.isConnected()) {
            try {
                // doConnect will generate a new client Id, and wait for completion
                // client.reconnect() and automaticReconnection do not work as I expect.
                doConnect();
            } catch (Exception e1) {
                Main.logger.error("{}MQTT refereeing device server: {}", fop.getLoggingName(),
                        e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage());
            }
            sleep(1000);
        }
    }
    

    private void doConnect() throws MqttSecurityException, MqttException {
        userName = StartupUtils.getStringParam("mqttUserName");
        password = StartupUtils.getStringParam("mqttPassword");
        MqttConnectOptions connOpts = setupMQTTClient();
        client.connect(connOpts).waitForCompletion();

        publishMqttLedOnOff();

        client.subscribe(callback.deprecatedDecisionTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.deprecatedDecisionTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.decisionTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.decisionTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.juryMemberDecisionTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.juryMemberDecisionTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.clockTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.clockTopicName,
                client.getCurrentServerURI());
    }

    private void publishMqttDownSignal() throws MqttException, MqttPersistenceException {
        String topic = "owlcms/fop/down/" + fop.getName();
        client.publish(topic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
    }

    private void publishMqttLedOnOff() throws MqttException, MqttPersistenceException {
        logger.debug("{}MQTT LedOnOff", fop.getLoggingName());
        String topic = "owlcms/fop/startup/" + fop.getName();
        String deprecatedTopic = "owlcms/led/" + fop.getName();
        client.publish(topic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
        client.publish(deprecatedTopic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
        sleep(1000);
        client.publish(topic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
        client.publish(deprecatedTopic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
    }

    private void publishMqttRefereeUpdates(Boolean ref1, Boolean ref2, Boolean ref3) {
        logger.debug("{}MQTT publishMqttRefereeUpdates {} {} {}", fop.getLoggingName(), ref1, ref2, ref3);
        try {
            if (ref1 != null) {
                client.publish("owlcms/fop/decision/" + fop.getName(),
                        new MqttMessage((1 + " " + (ref1 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
            }
            if (ref2 != null) {
                client.publish("owlcms/fop/decision/" + fop.getName(),
                        new MqttMessage((2 + " " + (ref2 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
            }
            if (ref3 != null) {
                client.publish("owlcms/fop/decision/" + fop.getName(),
                        new MqttMessage((3 + " " + (ref3 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
            }
        } catch (MqttException e1) {

        }
        
    }
    private void publishMqttResetAllDecisions() {
        logger.debug("{}MQTT resetDecisions", fop.getLoggingName());
        try {
            client.publish("owlcms/fop/resetDecisions/" + fop.getName(),
                    new MqttMessage("reset".getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {

        }
    }

    private void publishMqttSummonRef(int ref, boolean onOff) {
        logger.debug("{}MQTT summon {} {}", fop.getLoggingName(), ref, onOff);
        try {
            String topic = "owlcms/fop/summon/" + fop.getName();
            String deprecatedTopic = "owlcms/summon/" + fop.getName() + "/" + ref;
            // String refMacAddress = macAddress[e.ref-1];
            // insert target device mac address for cross-check
            client.publish(topic, new MqttMessage((onOff ? "on" : "off").getBytes(StandardCharsets.UTF_8)));
            client.publish(deprecatedTopic, new MqttMessage((onOff ? "on" : "off").getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {
            logger.error("could not publish summon {}", e1.getCause());
        }
    }

    private void publishMqttWakeUpRef(int ref, boolean onOff) {
        logger.debug("{}MQTT decisionRequest {} {}", fop.getLoggingName(), ref, onOff);
        try {
            // updated: no referee in the topic
            String topic = "owlcms/fop/decisionRequest/" + fop.getName();
            // Legacy : specific referee is added at the end of the topic.
            String deprecatedTopic = "owlcms/decisionRequest/" + fop.getName() + "/" + ref;
            client.publish(topic,
                    new MqttMessage((ref + " " + (onOff ? "on" : "off")).getBytes(StandardCharsets.UTF_8)));
            client.publish(deprecatedTopic,
                    new MqttMessage((ref + " " + (onOff ? "on" : "off")).getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {
            logger.error("could not publish decisionRequest {}", e1.getCause());
        }
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
        // connOpts.setAutomaticReconnect(true);
        return connOpts;
    }

    private MqttConnectOptions setupMQTTClient() {
        MqttConnectOptions connOpts = setUpConnectionOptions(userName != null ? userName : "",
                password != null ? password : "");
        callback = new MQTTCallback();
        client.setCallback(callback);
        return connOpts;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

}
