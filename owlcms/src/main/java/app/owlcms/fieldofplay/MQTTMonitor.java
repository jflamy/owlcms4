package app.owlcms.fieldofplay;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

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
import app.owlcms.data.config.Config;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class receives and emits MQTT events.
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
    private class MQTTCallback implements MqttCallback {
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
            connectionLoop(client);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // required by abstract class
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            new Thread(() -> {
                String messageStr = new String(message.getPayload(), StandardCharsets.UTF_8);
                logger.info("{}{} : {}", fop.getLoggingName(), topic, messageStr.trim());

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
         * 
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
                logger.error("{}Malformed MQTT jury decision message topic='{}' message='{}'",
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
                logger.error("{}Malformed MQTT jury member decision message topic='{}' message='{}'",
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
                logger.error("{}Malformed MQTT referee decision message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void postFopEventSummonReferee(String topic, String messageStr) {
            messageStr = messageStr.trim();
            try {
                String[] parts = messageStr.split(" ");
                int refIndex = 0;
                if (parts[0].contentEquals("all")) {
                    refIndex = 0;
                } else if (parts[0].contentEquals("controller")) {
                    refIndex = 4;
                } else {
                    refIndex = Integer.parseInt(parts[0]);
                }
                // do the actual summoning
                if (fop != null) {
                    if (fop.getState() != FOPState.BREAK) {
                        fop.fopEventPost(
                                new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
                    }
                    fop.fopEventPost(new FOPEvent.SummonReferee(this, refIndex));
                }
            } catch (NumberFormatException e) {
                logger.error("{}Malformed MQTT referee summon message topic='{}' message='{}'",
                        fop.getLoggingName(), topic, messageStr);
            }
        }

        private void postFopJuryBreakEvents(String topic, String messageStr) {
            messageStr = messageStr.trim();
            if (messageStr.equalsIgnoreCase("technical")) {
                fop.fopEventPost(
                        new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, this));
            } else if (messageStr.equalsIgnoreCase("deliberation")) {
                fop.fopEventPost(
                        new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
            } else if (messageStr.equalsIgnoreCase("stop")) {
                fop.fopEventPost(
                        new FOPEvent.StartLifting(this));
            } else {
                logger.error("{}Malformed MQTT jury break message topic='{}' message='{}'",
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

    private static Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);
    public static MqttAsyncClient createMQTTClient(FieldOfPlay fop) throws MqttException {
        String server = Config.getCurrent().getParamMqttServer();
        server = (server != null ? server : "127.0.0.1");
        String port = Config.getCurrent().getParamMqttPort();
        port = (port != null ? port : "1883");
        String string = port.startsWith("8") ? "ssl://" : "tcp://";
        Main.getStartupLogger().info("connecting to MQTT {}{}:{}", string, server, port);

        MqttAsyncClient client = new MqttAsyncClient(
                string + server + ":" + port                        ,
                fop.getName()+"_"+MqttClient.generateClientId(), // ClientId
                new MemoryPersistence()); // Persistence
        return client;
    }
    private MqttAsyncClient client;
    private FieldOfPlay fop;

    private String password;
    private String userName;
    private MQTTCallback callback;

    private Long prevRefereeTimeStamp = 0L;

    MQTTMonitor(FieldOfPlay fop) {
        logger.setLevel(Level.DEBUG);
        this.setFop(fop);
        fop.getUiEventBus().register(this);
        fop.getFopEventBus().register(this);

        try {
            if (Config.getCurrent().getParamMqttInternal() || Config.getCurrent().getParamMqttServer() != null) {
                client = createMQTTClient(fop);
                connectionLoop(client);
            } else {
                logger.info("no MQTT server configured, skipping");
            }
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
        // Event Ignored. We reset all devices on the clock start for next attempt (resetDecisions MQTT)
    }

    /**
     * A display or console has triggered the down signal (e.g. keypad connected to a laptop) and down signal post
     * connected via MQTT.
     * 
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
    public void slaveJuryUpdate(UIEvent.JuryUpdate e) {
        if (e.getCollective() == null) {
            // individual decision hidden
            publishMqttJuryMemberDecision(e.getJuryMemberUpdated());
        } else {
            Boolean[] decisions = e.getJuryMemberDecision();
            int nbDecisions = 0;
            for (int i = 0; i < e.getJurySize(); i++) {
                nbDecisions += (decisions[i] != null ? 1 : 0);
            }
            if (nbDecisions == e.getJurySize()) {
                publishMqttJuryReveal(nbDecisions, decisions);
            }
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
        publishMqttRefereeUpdates(e.ref1, e.ref2, e.ref3, e.ref1Time, e.ref2Time, e.ref3Time);
    }

    @Subscribe
    public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
        // we switched lifter, or we switched attempt. reset the decisions.
        prevRefereeTimeStamp = 0L;
        publishMqttResetAllDecisions();
    }

    @Subscribe
    public void slaveSummonRef(UIEvent.SummonRef e) {
        // e.ref is 0..2
        // 3 is all
        // 4 is controller.
        int ref = e.ref;

        publishMqttSummonRef(ref);
    }

    @Subscribe
    public void slaveTimeStarted(UIEvent.StartTime e) {
    }

    @Subscribe
    public void slaveWakeUpRef(UIEvent.WakeUpRef e) {
        // e.ref is 1..3
        // logger.debug("slaveWakeUp {}", e.on);
        int ref = e.ref;
        publishMqttWakeUpRef(ref, e.on);
    }

    private void connectionLoop(MqttAsyncClient mqttAsyncClient) {
        while (!mqttAsyncClient.isConnected()) {
            try {
                // doConnect will generate a new client Id, and wait for completion
                // client.reconnect() and automaticReconnection do not work as I expect.
                doConnect();
            } catch (Exception e1) {
                Main.getStartupLogger().error("{}MQTT refereeing device server: {}", fop.getLoggingName(),
                        e1.getCause() != null ? e1.getCause().getMessage() : e1);
                logger.error("{}MQTT refereeing device server: {}", fop.getLoggingName(),
                        e1.getCause() != null ? e1.getCause().getMessage() : e1);
            }
            sleep(1000);
        }
    }

    private void doConnect() throws MqttSecurityException, MqttException {
        if (Config.getCurrent().getParamMqttInternal() && Config.getCurrent().getParamMqttServer() == null) {
            userName = Config.getCurrent().getMqttUserName();
            password = Main.mqttStartup;
        } else {
            userName = Config.getCurrent().getParamMqttUserName();
            password = Config.getCurrent().getParamMqttPassword();
        }
        MqttConnectOptions connOpts = setupMQTTClient(userName, password);
        client.connect(connOpts).waitForCompletion();

        publishMqttLedOnOff();

        client.subscribe(callback.deprecatedDecisionTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.deprecatedDecisionTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.decisionTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.decisionTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.downEmittedTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.downEmittedTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.juryBreakTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.juryBreakTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.juryMemberDecisionTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.juryMemberDecisionTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.juryDecisionTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.juryDecisionTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.jurySummonTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.jurySummonTopicName,
                client.getCurrentServerURI());
        client.subscribe(callback.clockTopicName, 0);
        logger.info("{}MQTT subscribe {} {}", fop.getLoggingName(), callback.clockTopicName,
                client.getCurrentServerURI());
    }

    private void doPublishMQTTSummon(int ref) throws MqttException, MqttPersistenceException {
        String topic = "owlcms/fop/summon/" + fop.getName();
        client.publish(topic, new MqttMessage(Integer.toString(ref).getBytes(StandardCharsets.UTF_8)));
        String deprecatedTopic = "owlcms/summon/" + fop.getName() + "/" + ref;
        client.publish(deprecatedTopic, new MqttMessage(("on").getBytes(StandardCharsets.UTF_8)));
    }

    private void publishMqttDownSignal() throws MqttException, MqttPersistenceException {
        String topic = "owlcms/fop/down/" + fop.getName();
        client.publish(topic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
    }

    private void publishMqttJuryMemberDecision(Integer juryMemberUpdated) {
        String topic = "owlcms/fop/juryMemberDecision/" + fop.getName();
        try {
            String message = Integer.toString(juryMemberUpdated+1) + " hidden";
            logger.warn("posting {} {}", topic, message);
            client.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e) {
        }
    }

    private void publishMqttJuryReveal(int jurySize, Boolean[] juryMemberDecision) {
        String topic = "owlcms/fop/juryMemberDecision/" + fop.getName();
        for (int i = 0 ; i < jurySize ; i++) {
            try {
                String message = Integer.toString(i+1) + (juryMemberDecision[i] ? " good" : " bad");
                logger.warn("posting {} {}", topic, message);
                client.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
            } catch (MqttException e) {
            }
        }
    }

    private void publishMqttLedOnOff() throws MqttException, MqttPersistenceException {
        //logger.debug("{}MQTT LedOnOff", fop.getLoggingName());
        String topic = "owlcms/fop/startup/" + fop.getName();
        String deprecatedTopic = "owlcms/led/" + fop.getName();
        client.publish(topic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
        client.publish(deprecatedTopic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
        sleep(1000);
        client.publish(topic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
        client.publish(deprecatedTopic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
    }

    private void publishMqttRefereeUpdates(Boolean ref1, Boolean ref2, Boolean ref3, Long ref1Time, Long ref2Time,
            Long ref3Time) {
        Optional<Long> curRefereeUpdateTimeStamp = Arrays.asList(ref1Time, ref2Time, ref3Time)
                .stream()
                .filter(ts -> {
                    return ts != null;
                })
                .max(Long::compare);
        if (curRefereeUpdateTimeStamp.isPresent()
                && curRefereeUpdateTimeStamp.get() >= prevRefereeTimeStamp) {
            logger.debug("{}MQTT publishMqttRefereeUpdates {}({}) {}({}) {}({})", fop.getLoggingName(), ref1, ref1Time,
                    ref2, ref2Time, ref3, ref3Time);
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
        } else {
            logger.debug("{}MQTT skipping out-of-date publishMqttRefereeUpdates {}({}) {}({}) {}({})",
                    fop.getLoggingName(), ref1, ref1Time,
                    ref2, ref2Time, ref3, ref3Time);
        }
        prevRefereeTimeStamp = curRefereeUpdateTimeStamp.isPresent() ? curRefereeUpdateTimeStamp.get() : 0L;
    }

    private void publishMqttResetAllDecisions() {
        logger.debug("{}MQTT resetDecisions", fop.getLoggingName());
        try {
            client.publish("owlcms/fop/resetDecisions/" + fop.getName(),
                    new MqttMessage("reset".getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException e1) {

        }
    }

    private void publishMqttSummonRef(int ref) {
        logger.debug("{}MQTT summon {}", fop.getLoggingName(), ref);
        try {
            if (ref > 0 && ref <= 4) {
                doPublishMQTTSummon(ref);
            } else if (ref == 0) {
                // 0 = all referees
                for (int i = 1; i <= 3; i++) {
                    doPublishMQTTSummon(i);
                }
            }
        } catch (MqttException e1) {
            logger.error("could not publish summon {}", e1.getCause());
        }
    }

    private void publishMqttWakeUpRef(int ref, boolean on) {
        logger.debug("{}MQTT decisionRequest {} {}", fop.getLoggingName(), ref, on);
        try {
            FOPState state = fop.getState();
            if (state != FOPState.DOWN_SIGNAL_VISIBLE
                    && state != FOPState.TIME_RUNNING
                    && state != FOPState.TIME_STOPPED) {
                // boundary condition where the wait thread to remind referee is not cancelled
                // in time; should not happen, this is defensive.
                return;
            }
            String topic = "owlcms/fop/decisionRequest/" + fop.getName();
            if (on) {
                client.publish(topic, new MqttMessage(Integer.toString(ref).getBytes(StandardCharsets.UTF_8)));
            } else {
                // off is not sent in modern mode.
            }

            // Legacy : specific referee is added at the end of the topic.
            String deprecatedTopic = "owlcms/decisionRequest/" + fop.getName() + "/" + ref;
            if (on) {
                client.publish(deprecatedTopic,
                        new MqttMessage(("on").getBytes(StandardCharsets.UTF_8)));
            } else {
                client.publish(deprecatedTopic,
                        new MqttMessage(("off").getBytes(StandardCharsets.UTF_8)));
            }
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

    private MqttConnectOptions setupMQTTClient(String userName, String password) {
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
