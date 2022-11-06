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

public class MQTTMonitor {

    public class MQTTCallback implements MqttCallback, JuryEvents {
        private Athlete athleteUnderReview;
        private String juryBreakTopicName;
        private String juryMemberDecisionTopicName;
        private String juryDecisionTopicName;
        private String downEmittedTopicName;
        private String decisionTopicName;
        private String jurySummonTopicName;
        private String oldDecisionTopicName;
        private String clockTopicName;

        MQTTCallback() {
            this.oldDecisionTopicName = "owlcms/decision/" + fop.getName();
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
        public void deliveryComplete(IMqttDeliveryToken token) {// Called when a outgoing publish is complete
        }

        /**
         * @return the athleteUnderReview
         */
        public Athlete getAthleteUnderReview() {
            return athleteUnderReview;
        }

        /**
         * @return the clockTopicName
         */
        public String getClockTopicName() {
            return clockTopicName;
        }

        /**
         * @return the decisionTopicName
         */
        public String getDecisionTopicName() {
            return decisionTopicName;
        }

        /**
         * @return the downEmittedTopicName
         */
        public String getDownEmittedTopicName() {
            return downEmittedTopicName;
        }

        /**
         * @return the juryBreakTopicName
         */
        public String getJuryBreakTopicName() {
            return juryBreakTopicName;
        }

        /**
         * @return the juryDecisionTopicName
         */
        public String getJuryDecisionTopicName() {
            return juryDecisionTopicName;
        }

        /**
         * @return the juryMemberDecisionTopicName
         */
        public String getJuryMemberDecisionTopicName() {
            return juryMemberDecisionTopicName;
        }

        /**
         * @return the jurySummonTopicName
         */
        public String getJurySummonTopicName() {
            return jurySummonTopicName;
        }

        /**
         * @return the oldDecisionTopicName
         */
        public String getOldDecisionTopicName() {
            return oldDecisionTopicName;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            new Thread(() -> {
                String messageStr = new String(message.getPayload(), StandardCharsets.UTF_8);
                logger.info("{}{} : {}", fop.getLoggingName(), topic, messageStr);

                if (topic.endsWith(decisionTopicName) || topic.endsWith(oldDecisionTopicName)) {
                    fopEventRefereeDecisionUpdate(topic, messageStr);
                } else if (topic.endsWith(downEmittedTopicName)) {
                    fopEventDownEmitted(topic, messageStr);
                } else if (topic.endsWith(clockTopicName)) {
                    fopTimeEvents(topic, messageStr);
                } else if (topic.endsWith(getJuryBreakTopicName())) {
                    fopJuryBreakEvents(topic, messageStr);
                } else if (topic.endsWith(getJuryMemberDecisionTopicName())) {
                    fopEventJuryMemberDecisionUpdate(topic, messageStr);
                } else if (topic.endsWith(juryDecisionTopicName)) {
                    fopEventJuryDecision(topic, messageStr);
                } else if (topic.endsWith(jurySummonTopicName)) {
                    fopEventSummonReferee(topic, messageStr);
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

        private void fopEventDownEmitted(String topic, String messageStr) {
            messageStr = messageStr.trim();
            fop.fopEventPost(new FOPEvent.DownSignal(this));
        }

        private void fopEventRefereeDecisionUpdate(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                String[] parts = messageStr.split(" ");
                int refIndex = Integer.parseInt(parts[0]) - 1;
                fop.fopEventPost(new FOPEvent.DecisionUpdate(this, refIndex,
                        parts[parts.length - 1].contentEquals("good")));
                macAddress[refIndex] = parts[1];
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void fopEventSummonReferee(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                String[] parts = messageStr.split(" ");
                int refIndex = Integer.parseInt(parts[0]) - 1;
                fop.fopEventPost(new FOPEvent.DecisionUpdate(this, refIndex,
                        parts[parts.length - 1].contentEquals("good")));
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void fopEventJuryMemberDecisionUpdate(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                String[] parts = messageStr.split(" ");
                int refIndex = Integer.parseInt(parts[0]) - 1;
                fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(MQTTMonitor.this, refIndex,
                        parts[parts.length - 1].contentEquals("good")));
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void fopEventJuryDecision(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                fop.fopEventPost(
                        new FOPEvent.JuryDecision(getAthleteUnderReview(), this, messageStr.contentEquals("good")));
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void fopTimeEvents(String topic, String messageStr) {
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

        private void fopJuryBreakEvents(String topic, String messageStr) {
            messageStr = messageStr.trim();
            if (messageStr.equalsIgnoreCase("technical")) {
                postJuryTechnicalPause(fop, this);
            } else if (messageStr.equalsIgnoreCase("deliberation")) {
                postJuryDeliberation(OwlcmsSession.getFop(), this, getAthleteUnderReview());
            } else if (messageStr.equalsIgnoreCase("stop")) {
                // TODO resume competition
            } else {
                logger.error("{}Malformed MQTT clock message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }
    }

    private MqttAsyncClient client;
    private FieldOfPlay fop;
    private Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);
    private String[] macAddress = new String[3];
    private String password;
    private String port;
    private String server;
    private String userName;
    private MQTTCallback callback;

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
        try {
            client.publish("owlcms/fop/" + fop.getName(),
                    new MqttMessage("decisionReset".getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {
        }
    }

    @Subscribe
    public void slaveRefereeDecision(UIEvent.Decision e) {
        callback.setAthleteUnderReview(e.getAthlete());
    }

    @Subscribe
    public void slaveJuryDecision(FOPEvent.JuryDecision jd) {
        logger.warn("MQTT monitor received FOPEvent {}", jd.getClass().getSimpleName());
    }

    @Subscribe
    public void slaveJuryNotification(UIEvent.JuryNotification jn) {
        // these events come from the Jury Console as UI Events
        switch (jn.getDeliberationEventType()) {
        case CALL_TECHNICAL_CONTROLLER:
            // if we ever build a TC device
            doSummonRef(4, true);
            break;
        default:
            break;
        }

    }

    @Subscribe
    public void slaveSummonRef(UIEvent.SummonRef e) {
        // e.ref is 1..3
        // e.ref 4 is technical controller.
        int ref = e.ref;
        boolean on = e.on;
        doSummonRef(ref, on);
    }

    @Subscribe
    public void slaveTimerStart(UIEvent.StartTime e) {
        try {
            client.publish("owlcms/fop/" + fop.getName(),
                    new MqttMessage("clockStart".getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {

        }
    }

    @Subscribe
    public void slaveWakeUpRef(UIEvent.WakeUpRef e) {
        // e.ref is 1..3
        // logger.debug("slaveWakeUp {}", e.on);
        try {
            String topic = "owlcms/fop/decisionRequest/" + fop.getName();
            String oldTopic = "owlcms/decisionRequest/" + fop.getName() + "/" + e.ref;
            // String refMacAddress = macAddress[e.ref];
            client.publish(topic, new MqttMessage(
                    (e.ref + " " + (e.on ? "on" : "off")
                    /* + (refMacAddress != null ? " " + refMacAddress : "") */)
                            .getBytes(StandardCharsets.UTF_8)));
            client.publish(oldTopic, new MqttMessage(
                    ((e.on ? "on" : "off")
                    /* + (refMacAddress != null ? " " + refMacAddress : "") */)
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

        ledOnOff();

        client.subscribe(callback.getOldDecisionTopicName(), 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.getOldDecisionTopicName(),
                client.getCurrentServerURI());
        client.subscribe(callback.getDecisionTopicName(), 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.getDecisionTopicName(),
                client.getCurrentServerURI());
        client.subscribe(callback.getClockTopicName(), 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.getClockTopicName(),
                client.getCurrentServerURI());
    }

    private void doSummonRef(int ref, boolean on) {
        logger.debug("{}MQTT summon {} {}", fop.getLoggingName(), ref, on);
        try {
            String topic = "owlcms/fop/summon/" + fop.getName();
            String oldTopic = "owlcms/summon/" + fop.getName() + "/" + ref;
            // String refMacAddress = macAddress[e.ref-1];
            // insert target device mac address for cross-check
            client.publish(topic, new MqttMessage(
                    (ref + " " + (on ? "on" : "off")
                    // + (refMacAddress != null ? " " + refMacAddress : "")
                    )
                            .getBytes(StandardCharsets.UTF_8)));
            client.publish(oldTopic, new MqttMessage(
                    ((on ? "on" : "off")
                    // + (refMacAddress != null ? " " + refMacAddress : "")
                    )
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {
            logger.error("could not publish summon {}", e1.getCause());
        }
    }

    private void ledOnOff() throws MqttException, MqttPersistenceException {
        client.publish("owlcms/fop/startup/" + fop.getName(), new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
        client.publish("owlcms/led/" + fop.getName(), new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
        sleep(1000);
        client.publish("owlcms/fop/startup/" + fop.getName(), new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
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
