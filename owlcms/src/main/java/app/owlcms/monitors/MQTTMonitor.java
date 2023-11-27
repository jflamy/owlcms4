package app.owlcms.monitors;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.BreakStarted;
import app.owlcms.uievents.UIEvent.GroupDone;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
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
public class MQTTMonitor extends Thread {

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
		String testTopicName;
		String configTopicName;

		MQTTCallback() {
			// these are the device-initiated events that the monitor tracks
			this.deprecatedDecisionTopicName = "owlcms/decision/" + MQTTMonitor.this.fop.getName();
			this.decisionTopicName = "owlcms/refbox/decision/" + MQTTMonitor.this.fop.getName();
			this.downEmittedTopicName = "owlcms/refbox/downEmitted/" + MQTTMonitor.this.fop.getName();
			this.clockTopicName = "owlcms/clock/" + MQTTMonitor.this.fop.getName();
			this.juryBreakTopicName = "owlcms/jurybox/break/" + MQTTMonitor.this.fop.getName();
			this.juryMemberDecisionTopicName = "owlcms/jurybox/juryMember/decision/" + MQTTMonitor.this.fop.getName();
			this.juryDecisionTopicName = "owlcms/jurybox/decision/" + MQTTMonitor.this.fop.getName();
			this.jurySummonTopicName = "owlcms/jurybox/summon/" + MQTTMonitor.this.fop.getName();
			this.testTopicName = "owlcms/test/" + MQTTMonitor.this.fop.getName();
			// no FOP on this message, it is used for the device to query what FOPs are
			// present
			this.configTopicName = "owlcms/config";
		}

		@Override
		public void connectionLost(Throwable cause) {
			logger.debug("{}lost connection to MQTT: {}", MQTTMonitor.this.fop.getLoggingName(),
			        cause.getLocalizedMessage());
			// Called when the client lost the connection to the broker
			connectionLoop(MQTTMonitor.this.client);
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			// required by abstract class
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			new Thread(() -> {
				String messageStr = new String(message.getPayload(), StandardCharsets.UTF_8);
				logger.info("{}{} : {}", MQTTMonitor.this.fop.getLoggingName(), topic, messageStr.trim());

				if (topic.endsWith(this.decisionTopicName) || topic.endsWith(this.deprecatedDecisionTopicName)) {
					postFopEventRefereeDecisionUpdate(topic, messageStr);
				} else if (topic.endsWith(this.downEmittedTopicName)) {
					postFopEventDownEmitted(topic, messageStr);
				} else if (topic.endsWith(this.clockTopicName)) {
					postFopTimeEvents(topic, messageStr);
				} else if (topic.endsWith(this.juryBreakTopicName)) {
					postFopJuryBreakEvents(topic, messageStr);
				} else if (topic.endsWith(this.juryMemberDecisionTopicName)) {
					postFopEventJuryMemberDecisionUpdate(topic, messageStr);
				} else if (topic.endsWith(this.juryDecisionTopicName)) {
					postFopEventJuryDecision(topic, messageStr);
				} else if (topic.endsWith(this.jurySummonTopicName)) {
					postFopEventSummonReferee(topic, messageStr);
				} else if (topic.endsWith(this.configTopicName)) {
					publishMqttConfig("owlcms/fop/config");
				} else if (topic.endsWith(this.testTopicName)) {
					long before = Long.parseLong(messageStr);
					logger.info("{} timing = {}", getFop(), System.currentTimeMillis() - before);
				} else {
					logger.error("{}Malformed MQTT unrecognized topic message topic='{}' message='{}'",
					        MQTTMonitor.this.fop.getLoggingName(), topic, messageStr);
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
			MQTTMonitor.this.fop.fopEventPost(new FOPEvent.DownSignal(this));
		}

		private void postFopEventJuryDecision(String topic, String messageStr) {
			messageStr = messageStr.trim();
			try {
				MQTTMonitor.this.fop.fopEventPost(
				        new FOPEvent.JuryDecision(this.athleteUnderReview, this, messageStr.contentEquals("good")));
			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT jury decision message topic='{}' message='{}'",
				        MQTTMonitor.this.fop.getLoggingName(), topic, messageStr);
			}
		}

		private void postFopEventJuryMemberDecisionUpdate(String topic, String messageStr) {
			messageStr = messageStr.trim();
			try {
				String[] parts = messageStr.split(" ");
				int refIndex = Integer.parseInt(parts[0]) - 1;
				logger.debug("JuryMemberDecisionUpdate {} {}", parts, refIndex);
				MQTTMonitor.this.fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(MQTTMonitor.this, refIndex,
				        parts[parts.length - 1].contentEquals("good")));
			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT jury member decision message topic='{}' message='{}'",
				        MQTTMonitor.this.fop.getLoggingName(), topic, messageStr);
			}
		}

		private void postFopEventRefereeDecisionUpdate(String topic, String messageStr) {
			messageStr = messageStr.trim();
			try {
				String[] parts = messageStr.split(" ");
				int refIndex = Integer.parseInt(parts[0]) - 1;
				MQTTMonitor.this.fop.fopEventPost(new FOPEvent.DecisionUpdate(this, refIndex,
				        parts[parts.length - 1].contentEquals("good")));

			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT referee decision message topic='{}' message='{}'",
				        MQTTMonitor.this.fop.getLoggingName(), topic, messageStr);
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
				if (MQTTMonitor.this.fop != null) {
					if (MQTTMonitor.this.fop.getState() != FOPState.BREAK && refIndex != 4) {
						MQTTMonitor.this.fop.fopEventPost(
						        new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true,
						                this));
					}
					MQTTMonitor.this.fop.fopEventPost(new FOPEvent.SummonReferee(this, refIndex));
				}
			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT referee summon message topic='{}' message='{}'",
				        MQTTMonitor.this.fop.getLoggingName(), topic, messageStr);
			}
		}

		private void postFopJuryBreakEvents(String topic, String messageStr) {
			messageStr = messageStr.trim();
			if (messageStr.equalsIgnoreCase("technical")) {
				MQTTMonitor.this.fop.fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, this));
			} else if (messageStr.equalsIgnoreCase("deliberation")) {
				MQTTMonitor.this.fop.fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
			} else if (messageStr.equalsIgnoreCase("challenge")) {
				MQTTMonitor.this.fop.fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, this));
			} else if (messageStr.equalsIgnoreCase("stop")) {
				MQTTMonitor.this.fop.fopEventPost(
				        new FOPEvent.StartLifting(this));
			} else {
				logger.error("{}Malformed MQTT jury break message topic='{}' message='{}'",
				        MQTTMonitor.this.fop.getLoggingName(), topic, messageStr);
			}
		}

		private void postFopTimeEvents(String topic, String messageStr) {
			messageStr = messageStr.trim();
			if (messageStr.equalsIgnoreCase("start")) {
				MQTTMonitor.this.fop.fopEventPost(new FOPEvent.TimeStarted(this));
			} else if (messageStr.equalsIgnoreCase("stop")) {
				MQTTMonitor.this.fop.fopEventPost(new FOPEvent.TimeStopped(this));
			} else if (messageStr.equalsIgnoreCase("60")) {
				MQTTMonitor.this.fop.fopEventPost(new FOPEvent.ForceTime(60000, this));
			} else if (messageStr.equalsIgnoreCase("120")) {
				MQTTMonitor.this.fop.fopEventPost(new FOPEvent.ForceTime(120000, this));
			} else {
				logger.error("{}Malformed MQTT clock message topic='{}' message='{}'",
				        MQTTMonitor.this.fop.getLoggingName(), topic, messageStr);
			}
		}
	}

	private static Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);

	public static MqttAsyncClient createMQTTClient(FieldOfPlay fop) throws MqttException {
		String server = Config.getCurrent().getParamMqttServer();
		server = (server != null && !server.isBlank() ? server : "127.0.0.1");
		String port = Config.getCurrent().getParamMqttPort();
		port = (port != null ? port : "1883");
		String string = port.startsWith("8") ? "ssl://" : "tcp://";
		Main.getStartupLogger().info("connecting to MQTT {}{}:{}", string, server, port);

		MqttAsyncClient client = new MqttAsyncClient(
		        string + server + ":" + port,
		        fop.getName() + "_" + MqttClient.generateClientId(), // ClientId
		        new MemoryPersistence()); // Persistence
		return client;
	}

	private MqttAsyncClient client;
	private FieldOfPlay fop;
	private String password;
	private String userName;
	private MQTTCallback callback;
	private Long prevRefereeTimeStamp = 0L;

	public MQTTMonitor(FieldOfPlay fop) {
		this.fop = fop;
	}

	public FieldOfPlay getFop() {
		return this.fop;
	}

	public void publishMqttConfig() {
		PlatformRepository.syncFOPs();
		publishMqttConfig("owlcms/fop/config");
	}

	public void publishStartAthleteTimer() throws MqttPersistenceException, MqttException {
		this.client.publish("owlcms/clock/" + this.fop.getName(),
		        new MqttMessage("start".getBytes(StandardCharsets.UTF_8)));
	}

	public void publishStopAthleteTimer() throws MqttPersistenceException, MqttException {
		this.client.publish("owlcms/clock/" + this.fop.getName(),
		        new MqttMessage("stop".getBytes(StandardCharsets.UTF_8)));
	}

	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		// logger.debug("mqtt slaveBreakStart {} {}",e, e.getBreakType());
		if (e.getBreakType() == BreakType.JURY) {
			try {
				publishMqttJuryDeliberation();
			} catch (MqttException e1) {
			}
		} else if (e.getBreakType() == BreakType.CHALLENGE) {
			try {
				publishMqttChallenge();
			} catch (MqttException e1) {
			}
		} else {
			try {
				publishMqttBreak(e);
			} catch (MqttException e1) {
			}
		}
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		try {
			publishMqttCeremony(e, false);
		} catch (MqttException e1) {
			logger.error(e1.toString());
		}
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		try {
			publishMqttCeremony(e, true);
		} catch (MqttException e1) {
			logger.error(e1.toString());
		}
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
	public void slaveGroupDone(UIEvent.GroupDone e) {
		try {
			publishMqttGroupDone(e);
		} catch (MqttException e1) {
			logger.error(e1.toString());
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
	public void slaveLiftingOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		try {
			publishMqttLiftingOrderUpdated();
		} catch (MqttException e1) {
		}
	}

	@Subscribe
	public void slaveRefereeDecision(UIEvent.Decision e) {
		logger.warn("slaveRefereeDecision");
		// the deliberation is about the last athlete judged, not on the current
		// athlete.
		this.callback.setAthleteUnderReview(e.getAthlete());
		publishMqttRefereeDecision(e.ref1, e.ref2, e.ref3);
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		// the deliberation is about the last athlete judged, not on the current
		// athlete.
		publishMqttRefereeUpdates(e.ref1, e.ref2, e.ref3, e.ref1Time, e.ref2Time, e.ref3Time);
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		// we switched lifter, or we switched attempt. reset the decisions.
		this.prevRefereeTimeStamp = 0L;
		publishMqttResetAllDecisions();
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		try {
			publishMqttStartLifting();
		} catch (MqttException e1) {
		}
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
	public void slaveTimeRemaining(UIEvent.TimeRemaining e) {
		int tr = e.getTimeRemaining();
		publishMqttTimeRemaining(tr);
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

	@Override
	public void start() {
		logger.setLevel(Level.DEBUG);
		this.setFop(this.fop);
		this.fop.getUiEventBus().register(this);
		this.fop.getFopEventBus().register(this);

		try {
			String paramMqttServer = Config.getCurrent().getParamMqttServer();
			if (Config.getCurrent().getParamMqttInternal() || (paramMqttServer != null && !paramMqttServer.isBlank())) {
				this.client = createMQTTClient(this.fop);
				connectionLoop(this.client);
			} else {
				logger.info("no MQTT server configured, skipping");
			}
		} catch (MqttException e) {
			logger.error("cannot initialize MQTT: {}", LoggerUtils.stackTrace(e));
		}
	}

	private void connectionLoop(MqttAsyncClient mqttAsyncClient) {
		while (!mqttAsyncClient.isConnected()) {
			try {
				// doConnect will generate a new client Id, and wait for completion
				// client.reconnect() and automaticReconnection do not work as I expect.
				doConnect();
			} catch (Exception e1) {
				Main.getStartupLogger().error("{}MQTT refereeing device server: {}", this.fop.getLoggingName(),
				        e1.getCause() != null ? e1.getCause().getMessage() : e1);
				logger.error("{}MQTT refereeing device server: {}", this.fop.getLoggingName(),
				        e1.getCause() != null ? e1.getCause().getMessage() : e1);
			}
			sleep(1000);
		}
	}

	private void doConnect() throws MqttSecurityException, MqttException {
		Config curConfig = Config.getCurrent();
		boolean external = false;
		if (curConfig.getParamMqttServer() != null && !Config.getCurrent().getParamMqttServer().isBlank()) {
			external = true;
			Config current2 = Config.getCurrent();
			this.userName = current2.getParamMqttUserName();
			Config current3 = Config.getCurrent();
			this.password = current3.getParamMqttPassword();
		} else {
			Config current4 = Config.getCurrent();
			if (current4.getParamMqttInternal()) {
				Config current5 = Config.getCurrent();
				this.userName = current5.getMqttUserName();
				this.password = Main.mqttStartup;
			}
		}
		MqttConnectOptions connOpts = setupMQTTClient(this.userName, this.password);
		this.client.connect(connOpts).waitForCompletion();

		publishMqttLedOnOff();
		logger.info("connected to {} MQTT broker {}", (external ? "external" : "embedded"),
		        this.client.getCurrentServerURI());

		this.client.subscribe(this.callback.deprecatedDecisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.deprecatedDecisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.decisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.decisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.downEmittedTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.downEmittedTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.juryBreakTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.juryBreakTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.juryMemberDecisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.juryMemberDecisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.juryDecisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.juryDecisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.jurySummonTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.jurySummonTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.clockTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.clockTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.testTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.testTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.configTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", this.fop.getLoggingName(), this.callback.configTopicName,
		        this.client.getCurrentServerURI());
	}

	private void doPublishMQTTSummon(int ref) throws MqttException, MqttPersistenceException {
		String topic = "owlcms/fop/summon/" + this.fop.getName();
		this.client.publish(topic, new MqttMessage(Integer.toString(ref).getBytes(StandardCharsets.UTF_8)));
		String deprecatedTopic = "owlcms/summon/" + this.fop.getName() + "/" + ref;
		this.client.publish(deprecatedTopic, new MqttMessage(("on").getBytes(StandardCharsets.UTF_8)));
	}

	private void publishMqttBreak(BreakStarted e) throws MqttPersistenceException, MqttException {
		try {
			this.client.publish("owlcms/fop/break/" + this.fop.getName(),
			        new MqttMessage(e.getBreakType().name().getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e1) {
			logger.error("mqttBreak event error - {}", e.getTrace());
		}
	}

	private void publishMqttCeremony(UIEvent e, boolean b) throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/ceremony/" + this.fop.getName();
		try {
			CeremonyType ceremonyType;
			if (e instanceof UIEvent.CeremonyStarted) {
				ceremonyType = ((UIEvent.CeremonyStarted) e).getCeremonyType();
			} else {
				ceremonyType = ((UIEvent.CeremonyDone) e).getCeremonyType();
			}
			this.client.publish(
			        topic,
			        new MqttMessage(
			                (ceremonyType.name() + " " + (e instanceof UIEvent.CeremonyStarted ? "start" : "stop"))
			                        .getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e1) {
		}
	}

	private void publishMqttChallenge() throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/challenge/" + this.fop.getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttConfig(String topic) {
		Map<String, Object> payload = new TreeMap<>();
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		List<String> platforms = fops.stream().map(p -> p.getPlatform().getName())
		        .collect(Collectors.toList());
		payload.put("platforms", platforms);
		payload.put("version", StartupUtils.getVersion());
		payload.put("jurySize", Competition.getCurrent().getJurySize());
		try {
			String json = new ObjectMapper().writeValueAsString(payload);
			logger.info("{}MQTT Config: {}", this.fop.getLoggingName(), json);
			this.client.publish(topic, new MqttMessage(json.getBytes(StandardCharsets.UTF_8)));
		} catch (JsonProcessingException | MqttException e) {
		}
	}

	private void publishMqttDownSignal() throws MqttException, MqttPersistenceException {
		String topic = "owlcms/fop/down/" + this.fop.getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttGroupDone(GroupDone e) throws MqttPersistenceException, MqttException {
		this.client.publish("owlcms/fop/break/" + this.fop.getName(),
		        new MqttMessage(BreakType.GROUP_DONE.name().getBytes(StandardCharsets.UTF_8)));
	}

	private void publishMqttJuryDeliberation() throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/juryDeliberation/" + this.fop.getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttJuryMemberDecision(Integer juryMemberUpdated) {
		String topic = "owlcms/fop/juryMemberDecision/" + this.fop.getName();
		try {
			String message = Integer.toString(juryMemberUpdated + 1) + " hidden";
			this.client.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e) {
		}
	}

	private void publishMqttJuryReveal(int jurySize, Boolean[] juryMemberDecision) {
		String topic = "owlcms/fop/juryMemberDecision/" + this.fop.getName();
		for (int i = 0; i < jurySize; i++) {
			try {
				String message = Integer.toString(i + 1) + (juryMemberDecision[i] ? " good" : " bad");
				this.client.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
			} catch (MqttException e) {
			}
		}
	}

	private void publishMqttLedOnOff() throws MqttException, MqttPersistenceException {
		// logger.debug("{}MQTT LedOnOff", fop.getLoggingName());
		String topic = "owlcms/fop/startup/" + this.fop.getName();
		String deprecatedTopic = "owlcms/led/" + this.fop.getName();
		this.client.publish(topic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
		this.client.publish(deprecatedTopic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
		sleep(1000);
		this.client.publish(topic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
		this.client.publish(deprecatedTopic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
	}

	private void publishMqttLiftingOrderUpdated() throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/liftingOrderUpdated/" + this.fop.getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttRefereeDecision(Boolean ref1, Boolean ref2, Boolean ref3) {
		boolean decision;
		if (ref1 == null) {
			decision = ref2;
		} else {
			decision = (ref1 && ref2) || (ref1 && ref3) || (ref2 && ref3);
		}
		try {
			this.client.publish("owlcms/fop/refereesDecision/" + this.fop.getName(),
			        new MqttMessage((decision ? "good" : "bad").getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e) {
		}
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
		        && curRefereeUpdateTimeStamp.get() >= this.prevRefereeTimeStamp) {
			logger.debug("{}MQTT publishMqttRefereeUpdates {}({}) {}({}) {}({})", this.fop.getLoggingName(), ref1,
			        ref1Time,
			        ref2, ref2Time, ref3, ref3Time);
			try {
				if (ref1 != null) {
					this.client.publish("owlcms/fop/decision/" + this.fop.getName(),
					        new MqttMessage((1 + " " + (ref1 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
				}
				if (ref2 != null) {
					this.client.publish("owlcms/fop/decision/" + this.fop.getName(),
					        new MqttMessage((2 + " " + (ref2 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
				}
				if (ref3 != null) {
					this.client.publish("owlcms/fop/decision/" + this.fop.getName(),
					        new MqttMessage((3 + " " + (ref3 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
				}
			} catch (MqttException e1) {
			}
		} else {
			logger.debug("{}MQTT skipping out-of-date publishMqttRefereeUpdates {}({}) {}({}) {}({})",
			        this.fop.getLoggingName(), ref1, ref1Time,
			        ref2, ref2Time, ref3, ref3Time);
		}
		this.prevRefereeTimeStamp = curRefereeUpdateTimeStamp.isPresent() ? curRefereeUpdateTimeStamp.get() : 0L;
	}

	private void publishMqttResetAllDecisions() {
		logger.debug("{}MQTT resetDecisions", this.fop.getLoggingName());
		try {
			this.client.publish("owlcms/fop/resetDecisions/" + this.fop.getName(),
			        new MqttMessage("reset".getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e1) {

		}
	}

	private void publishMqttStartLifting() throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/startLifting/" + this.fop.getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttSummonRef(int ref) {
		logger.debug("{}MQTT summon {}", this.fop.getLoggingName(), ref);
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

	private void publishMqttTimeRemaining(int tr) {
		logger.debug("{}MQTT timeRemaining {}", this.fop.getLoggingName(), tr);
		try {
			this.client.publish("owlcms/fop/timeRemaining/" + this.fop.getName(),
			        new MqttMessage(Integer.toString(tr).getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e1) {
			logger.error("could not publish timeRemaining {}", e1.getCause());
		}
	}

	private void publishMqttWakeUpRef(int ref, boolean on) {
		logger.debug("{}MQTT decisionRequest {} {}", this.fop.getLoggingName(), ref, on);
		try {
			FOPState state = this.fop.getState();
			if (state != FOPState.DOWN_SIGNAL_VISIBLE
			        && state != FOPState.TIME_RUNNING
			        && state != FOPState.TIME_STOPPED) {
				// boundary condition where the wait thread to remind referee is not cancelled
				// in time; should not happen, this is defensive.
				return;
			}
			String topic = "owlcms/fop/decisionRequest/" + this.fop.getName();
			if (on) {
				this.client.publish(topic, new MqttMessage(Integer.toString(ref).getBytes(StandardCharsets.UTF_8)));
			} else {
				// off is not sent in modern mode.
			}

			// Legacy : specific referee is added at the end of the topic.
			String deprecatedTopic = "owlcms/decisionRequest/" + this.fop.getName() + "/" + ref;
			if (on) {
				this.client.publish(deprecatedTopic,
				        new MqttMessage(("on").getBytes(StandardCharsets.UTF_8)));
			} else {
				this.client.publish(deprecatedTopic,
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
		this.callback = new MQTTCallback();
		this.client.setCallback(this.callback);
		return connOpts;
	}

	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
}
