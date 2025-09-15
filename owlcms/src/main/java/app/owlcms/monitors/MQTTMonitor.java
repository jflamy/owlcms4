/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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
import app.owlcms.data.athlete.LiftDefinition;
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
 * Events initiated by the devices start with topics that names the device (owlcms/jurybox) Devices do not listen to other devices. They listen to MQTT events
 * that come from the field of play. These events are of the form (owlcms/fop). The field of play is always the last element in the topic.
 *
 * @author Jean-François Lamy
 */
public class MQTTMonitor extends Thread implements IUnregister {

	private boolean active;

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
			this.deprecatedDecisionTopicName = "owlcms/decision/" + MQTTMonitor.this.getFop().getName();
			this.decisionTopicName = "owlcms/refbox/decision/" + MQTTMonitor.this.getFop().getName();
			this.downEmittedTopicName = "owlcms/refbox/downEmitted/" + MQTTMonitor.this.getFop().getName();
			this.clockTopicName = "owlcms/clock/" + MQTTMonitor.this.getFop().getName();
			this.juryBreakTopicName = "owlcms/jurybox/break/" + MQTTMonitor.this.getFop().getName();
			this.juryMemberDecisionTopicName = "owlcms/jurybox/juryMember/decision/" + MQTTMonitor.this.getFop().getName();
			this.juryDecisionTopicName = "owlcms/jurybox/decision/" + MQTTMonitor.this.getFop().getName();
			this.jurySummonTopicName = "owlcms/jurybox/summon/" + MQTTMonitor.this.getFop().getName();
			this.testTopicName = "owlcms/test/" + MQTTMonitor.this.getFop().getName();
			// no FOP on this message, it is used for the device to query what FOPs are
			// present
			this.configTopicName = "owlcms/config";
			setMonitorActive(false);
		}

		@Override
		public void connectionLost(Throwable cause) {
			logger.debug("{}lost connection to MQTT: {}", FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()),
			        cause.getLocalizedMessage());
			// Called when the client lost the connection to the broker
			try {
				connectionLoop(MQTTMonitor.this.client);
			} catch (Throwable e) {
				logger.error("connectionLost {}", e);
			}
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			// required by abstract class
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			// Try to record a human-readable descriptor for any connection id embedded in the topic
			recordConnectionDescriptorFromTopic(topic);
			// record the publisher id derived from the topic for live connection listing
			recordPublisherFromTopic(topic);
			// record a signature based on topic+payload to help distinguish multiple clients publishing same topic
			try {
				recordPublisherSignature(topic, message != null ? message.getPayload() : null);
			} catch (Throwable t) {
				// ignore
			}
			new Thread(() -> {
				String messageStr = new String(message.getPayload(), StandardCharsets.UTF_8);
				logger.info("{}MQTT received {} : {}", FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), topic, messageStr.trim());

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
					this.setMonitorActive(true);
					publishMqttConfig("owlcms/fop/config");
				} else if (topic.endsWith(this.testTopicName)) {
					long before = Long.parseLong(messageStr);
					logger.info("{} timing = {}", getFop(), System.currentTimeMillis() - before);
				} else if (topic.startsWith("$SYS/")) {
					// broker system topic; try to parse connect/disconnect lines (Moquette may publish status here)
					try {
						parseSysTopic(topic, messageStr);
					} catch (Throwable t) {
						// ignore
					}
				} else {
					logger.error("{}Malformed MQTT unrecognized topic message topic='{}' message='{}'",
					        FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), topic, messageStr);
				}
			}).start();
				// Some broker runtime intercepts may expose the publisher client id or remote address
				// on a different object available to intercept handlers. Here we have only the topic
				// and message payload; no additional session object is available so skip this step.
				// Intercept handlers (embedded broker) populate remote addresses when available.
		}

		/**
		 * If the topic contains a token that looks like a connection id starting with 'mqtt',
		 * store a descriptor of the form "<platform> <topic-without-leading-owlcms/>" keyed by that id.
		 */
		private void recordConnectionDescriptorFromTopic(String topic) {
			if (topic == null || topic.isBlank()) return;
			String[] parts = topic.split("/");
			if (parts.length == 0) return;
			// find any token that looks like a client id starting with mqtt
			for (String token : parts) {
				if (token != null && token.startsWith("mqtt")) {
					String clientId = token;
					// build descriptor: platform (FOP name) followed by topic without leading 'owlcms/'
					String platform = (MQTTMonitor.this.getFop() != null ? MQTTMonitor.this.getFop().getName() : MQTTMonitor.this.monitoredFopName);
					String descriptor = topic;
					if (descriptor.startsWith("owlcms/")) descriptor = descriptor.substring("owlcms/".length());
					String finalDesc = (platform != null ? platform + " " + descriptor : descriptor);
					// Prefer assigning descriptor to any broker-reported client ids that start with 'mqtt'
					boolean assigned = false;
					long now = System.currentTimeMillis();
						for (String gid : MQTTInterceptHandlers.getGlobalActiveClientIds()) {
							if (gid == null) continue;
							if (MQTTInterceptHandlers.isConfigClientId(gid)) continue; // ignore config clients
							if (MQTTInterceptHandlers.isGenericClientId(gid)) {
								MQTTInterceptHandlers.putDescriptor(gid, finalDesc);
								MQTTInterceptHandlers.putLastSeen(gid, now);
								try {
									logger.debug("Assigned MQTT descriptor='{}' to broker clientId='{}' from topic='{}'", finalDesc, gid, topic);
									logger.debug("Updated connectionLastSeen: clientId='{}' ts={} (from topic)", gid, now);
								} catch (Throwable t) {
									// ignore logging failures
								}
								assigned = true;
							}
						}
					// Fallback: if no global mqtt ids found, store under the token extracted from topic
					if (!assigned) {
							if (!MQTTInterceptHandlers.isConfigClientId(clientId)) {
								MQTTInterceptHandlers.putDescriptor(clientId, finalDesc);
								MQTTInterceptHandlers.putLastSeen(clientId, now);
							}
						try {
							logger.debug("Assigned MQTT descriptor='{}' to inferred client token='{}' from topic='{}'", finalDesc, clientId, topic);
							logger.debug("Updated connectionLastSeen: clientId='{}' ts={} (inferred token)", clientId, now);
						} catch (Throwable t) {
							// ignore logging failures
						}
					}
					return;
				}
			}
			// diagnostic: if we didn't find an mqtt-like token, log parts to help debugging
			// Try fallback: use the second segment (e.g. 'jurybox' in 'owlcms/jurybox/...') as a candidate
			try {
				if (parts.length >= 2 && "owlcms".equals(parts[0])) {
					String candidate = parts[1];
					String platform = (MQTTMonitor.this.getFop() != null ? MQTTMonitor.this.getFop().getName() : MQTTMonitor.this.monitoredFopName);
					String descriptor = topic;
					if (descriptor.startsWith("owlcms/")) descriptor = descriptor.substring("owlcms/".length());
					String finalDesc = (platform != null ? platform + " " + descriptor : descriptor);
					boolean assigned2 = false;
					long now2 = System.currentTimeMillis();
						for (String gid : MQTTInterceptHandlers.getGlobalActiveClientIds()) {
							if (gid == null) continue;
							if (MQTTInterceptHandlers.isConfigClientId(gid)) continue; // ignore config clients
							if (gid.equals(candidate) || gid.startsWith(candidate) || candidate.startsWith(gid) || gid.contains(candidate) || candidate.contains(gid)) {
								MQTTInterceptHandlers.putDescriptor(gid, finalDesc);
								MQTTInterceptHandlers.putLastSeen(gid, now2);
								try {
									logger.debug("Assigned fallback descriptor='{}' to broker clientId='{}' from topic='{}' (candidate='{}')", finalDesc, gid, topic, candidate);
									logger.trace("Updated connectionLastSeen: clientId='{}' ts={} (fallback)", gid, now2);
								} catch (Throwable t) {
									// ignore logging failures
								}
								assigned2 = true;
							}
						}
					if (!assigned2) {
						// store under the candidate token so permissive UI lookup can find it
						if (!MQTTInterceptHandlers.isConfigClientId(candidate)) {
							MQTTInterceptHandlers.putDescriptor(candidate, finalDesc);
							MQTTInterceptHandlers.putLastSeen(candidate, now2);
						}
						try {
							logger.debug("Assigned fallback descriptor='{}' to inferred client token='{}' from topic='{}'", finalDesc, candidate, topic);
							logger.trace("Updated connectionLastSeen: clientId='{}' ts={} (fallback-inferred)", candidate, now2);
						} catch (Throwable t) {
							// ignore logging failures
						}
					}
					return;
				}
				String joined = String.join(",", parts);
				logger.info("No mqtt-like token found in topic='{}' parts=[{}]", topic, joined);
			} catch (Throwable t) {
				// ignore logging failures
			}
		}

		private void setMonitorActive(boolean b) {
			setActive(b);
		}

		private void parseSysTopic(String topic, String messageStr) {
			if (messageStr == null) return;
			String lower = messageStr.toLowerCase();
			boolean isConnect = lower.contains("connected");
			boolean isDisconnect = lower.contains("disconnected");
			if (!isConnect && !isDisconnect) return;
			String[] parts = messageStr.split("[ ,;:\\t\\n\\r]+");
			for (int i = 0; i < parts.length; i++) {
				String p = parts[i].trim();
				if (p.length() <= 1) continue;
				if (p.equalsIgnoreCase("client") && i + 1 < parts.length) {
					String cid = parts[i + 1].trim();
					if (isConnect) {
						MQTTMonitor.this.notifyClientConnected(cid);
					} else {
						MQTTMonitor.this.notifyClientDisconnected(cid);
					}
					return;
				}
			}
			// Fallback: pick first token that is not the keywords
			for (String p : parts) {
				String t = p.trim();
				if (t.length() <= 1) continue;
				if (t.equalsIgnoreCase("connected") || t.equalsIgnoreCase("disconnected") || t.equalsIgnoreCase("client")) continue;
				if (isConnect) {
					MQTTMonitor.this.notifyClientConnected(t);
				} else {
					MQTTMonitor.this.notifyClientDisconnected(t);
				}
				return;
			}
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
			MQTTMonitor.this.getFop().fopEventPost(new FOPEvent.DownSignal(this));
		}

		private void postFopEventJuryDecision(String topic, String messageStr) {
			messageStr = messageStr.trim();
			try {
				MQTTMonitor.this.getFop().fopEventPost(
				        new FOPEvent.JuryDecision(this.athleteUnderReview, this, messageStr.contentEquals("good"),
				                true));
			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT jury decision message topic='{}' message='{}'",
				        FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), topic, messageStr);
			}
		}

		private void postFopEventJuryMemberDecisionUpdate(String topic, String messageStr) {
			messageStr = messageStr.trim();
			try {
				String[] parts = messageStr.split(" ");
				int refIndex = Integer.parseInt(parts[0]) - 1;
				logger.debug("JuryMemberDecisionUpdate {} {}", parts, refIndex);
				MQTTMonitor.this.getFop().fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(MQTTMonitor.this, refIndex,
				        parts[parts.length - 1].contentEquals("good")));
			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT jury member decision message topic='{}' message='{}'",
				        FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), topic, messageStr);
			}
		}

		private void postFopEventRefereeDecisionUpdate(String topic, String messageStr) {
			messageStr = messageStr.trim();
			try {
				String[] parts = messageStr.split(" ");
				int refIndex = Integer.parseInt(parts[0]) - 1;
				MQTTMonitor.this.getFop().fopEventPost(new FOPEvent.DecisionUpdate(this, refIndex,
				        parts[parts.length - 1].contentEquals("good")));

			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT referee decision message topic='{}' message='{}'",
				        FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), topic, messageStr);
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
				if (MQTTMonitor.this.getFop() != null) {
					if (MQTTMonitor.this.getFop().getState() != FOPState.BREAK && refIndex != 4) {
						MQTTMonitor.this.getFop().fopEventPost(
						        new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true,
						                this));
					}
					MQTTMonitor.this.getFop().fopEventPost(new FOPEvent.SummonReferee(this, refIndex));
				}
			} catch (NumberFormatException e) {
				logger.error("{}Malformed MQTT referee summon message topic='{}' message='{}'",
				        FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), topic, messageStr);
			}
		}

		private void postFopJuryBreakEvents(String topic, String messageStr) {
			messageStr = messageStr.trim();
			if (messageStr.equalsIgnoreCase("technical")) {
				MQTTMonitor.this.getFop().fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, this));
			} else if (messageStr.equalsIgnoreCase("deliberation")) {
				MQTTMonitor.this.getFop().fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
			} else if (messageStr.equalsIgnoreCase("challenge")) {
				MQTTMonitor.this.getFop().fopEventPost(
				        new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, this));
			} else if (messageStr.equalsIgnoreCase("stop")) {
				var state = fop.getState();
				// green resume button used to clear the decision lights.
				if (state == FOPState.CURRENT_ATHLETE_DISPLAYED
				        || state == FOPState.INACTIVE
				        || (state == FOPState.BREAK && !fop.getBreakType().isInterruption())) {
					logger.info("{}MQTT jury resume received in state {}, sending ResetOnNewClock", FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), state);
					MQTTMonitor.this.getFop().getUiEventBus().post(new UIEvent.ResetOnNewClock(fop.getCurAthlete(), null, fop));
				} else {
					MQTTMonitor.this.getFop().fopEventPost(
					        new FOPEvent.StartLifting(this));
				}

			} else {
				logger.error("{}Malformed MQTT jury break message topic='{}' message='{}'",
				        FieldOfPlay.getLoggingName(MQTTMonitor.this.getFop()), topic, messageStr);
			}
		}

		private void postFopTimeEvents(String topic, String messageStr) {
			int index = messageStr.indexOf(' ');
			if (index > 0) {
				// ignore second part
				messageStr = messageStr.substring(0, index);
			}
			messageStr = messageStr.trim();
			FieldOfPlay fop2 = MQTTMonitor.this.getFop();
			if (messageStr.equalsIgnoreCase("start")) {
				fop2.fopEventPost(new FOPEvent.TimeStarted(this));
			} else if (messageStr.equalsIgnoreCase("stop")) {
				fop2.fopEventPost(new FOPEvent.TimeStopped(this));
			} else if (messageStr.equalsIgnoreCase("toggle")) {
				if (fop2.getAthleteTimer().isRunning()) {
					fop2.fopEventPost(new FOPEvent.TimeStopped(this));
				} else {
					fop2.fopEventPost(new FOPEvent.TimeStarted(this));
				}
			} else if (messageStr.equalsIgnoreCase("60")) {
				fop2.fopEventPost(new FOPEvent.ForceTime(60000, this));
			} else if (messageStr.equalsIgnoreCase("120")) {
				fop2.fopEventPost(new FOPEvent.ForceTime(120000, this));
			} else {
				logger.error("{}Malformed MQTT clock message topic='{}' message='{}'",
				        FieldOfPlay.getLoggingName(fop2), topic, messageStr);
			}
		}
	}

	private static Map<String, MQTTMonitor> mqttMonitorByName = new HashMap<>();
	private static Logger logger = (Logger) LoggerFactory.getLogger(MQTTMonitor.class);

	static {
		logger.setLevel(Level.DEBUG);
	}

	public static MqttAsyncClient createMQTTClient(FieldOfPlay fop) throws MqttException {
		String server = Config.getCurrent().getParamMqttServer();
		server = (server != null && !server.isBlank() ? server : "127.0.0.1");
		String port = Config.getCurrent().getParamMqttPort();
		port = (port != null ? port : "1883");
		String string = port.startsWith("8") ? "ssl://" : "tcp://";
		Main.getStartupLogger().info("connecting to MQTT {}{}:{}", string, server, port);

	// Use a stable client id indicating this server instance for the FOP: e.g. "A_owlcms_12345"
	// Append the global startup id when available so multiple server instances remain unique
	String startupId = (Main.mqttStartup != null && !Main.mqttStartup.isBlank()) ? Main.mqttStartup : Long.toString(System.currentTimeMillis());
	String clientId = fop.getName() + "_owlcms_" + startupId;
		MqttAsyncClient client = new MqttAsyncClient(
		        string + server + ":" + port,
		        clientId, // ClientId
		        new MemoryPersistence()); // Persistence
		return client;
	}

	public static MQTTMonitor getMqttMonitorByName(String name) {
		return mqttMonitorByName.get(name);
	}

	synchronized public static MQTTMonitor initMQTTMonitorByName(String monitorName, FieldOfPlay fieldOfPlay) {
		MQTTMonitor existingMonitor = mqttMonitorByName.get(monitorName);
		if (existingMonitor == null) {
			logger.info("{}creating MQTT monitor", FieldOfPlay.getLoggingName(fieldOfPlay));
			MQTTMonitor newForwarder = new MQTTMonitor(monitorName, fieldOfPlay);
			// fieldOfPlay.setMqttMonitor(newForwarder);
			mqttMonitorByName.put(monitorName, newForwarder);
			return newForwarder;
		} else {
			logger.info("{}reusing MQTT monitor", FieldOfPlay.getLoggingName(fieldOfPlay));
			// existingMonitor.getFop().setMqttMonitor(existingMonitor);
			existingMonitor.setFop(fieldOfPlay);
			return existingMonitor;
		}
	}

	public static void reset() {
		for (Entry<String, MQTTMonitor> e : mqttMonitorByName.entrySet()) {
			MQTTMonitor monitor = e.getValue();
			logger.info("unregistering MQTT monitor for platform {}", monitor.getMonitoredFopName());
			monitor.setFop(null);
			FieldOfPlay fop2 = OwlcmsFactory.getFOPByName(monitor.getMonitoredFopName());
			if (fop2 != null) {
				fop2.setEventForwarder(null);
			}
			try {
				monitor.client.disconnect();
				monitor.setActive(false);
			} catch (MqttException ex) {
				try {
					monitor.client.disconnectForcibly();
				} catch (MqttException e1) {
					LoggerUtils.logError(logger, e1);
				}
			}
		}
		mqttMonitorByName.clear();
	}

	private MqttAsyncClient client;
	private FieldOfPlay fop;
	private String password;
	private String userName;
	private MQTTCallback callback;
	private Long prevRefereeTimeStamp = 0L;
	private String monitoredFopName;

	// track recent publishers observed on topics for this monitor (publisher id -> lastSeen millis)
	private final Map<String, Long> lastSeenByPublisher = new ConcurrentHashMap<>();
	// track active client ids inferred from topic segments (clientId -> lastSeen millis)
	private final Map<String, Long> activeClientIds = new ConcurrentHashMap<>();
	// NOTE: global connection/descriptor state is owned by MQTTInterceptHandlers

	// Scheduled reconciliation executor for broker session checks
	private static final java.util.concurrent.ScheduledExecutorService reconciliationExecutor =
			java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "mqtt-reconciliation");
				t.setDaemon(true);
				return t;
			});
	// Start reconciliation when class is loaded
	static {
		// schedule reconciliation every 30 seconds
		reconciliationExecutor.scheduleAtFixedRate(() -> {
			try {
				reconcileWithBroker();
			} catch (Throwable t) {
				logger.debug("Error during MQTT broker reconciliation", t);
			}
		}, 30L, 30L, java.util.concurrent.TimeUnit.SECONDS);
	}

	/**
	 * Helper: return true for generic connection ids that should be treated like mqtt clients.
	 * We consider ids starting with 'mqtt' or 'a_f_' (case-insensitive) as generic.
	 */
	// delegated to MQTTInterceptHandlers.isGenericClientId

	/**
	 * Helper: return true for configuration-related client ids that should be ignored for descriptor tracking.
	 */
	// delegated to MQTTInterceptHandlers.isConfigClientId


	// track recent publisher signatures (topic+payload hash) to distinguish multiple clients publishing to same topic
	private final Map<String, Long> lastSeenByPublisherSignature = new ConcurrentHashMap<>();
	private final long PUBLISHER_TIMEOUT_MS = 30_000L;


	private MQTTMonitor(String monitorName, FieldOfPlay fop) {
		this.setMonitoredFopName(monitorName);
		this.setFop(fop);
	}

	public FieldOfPlay getFop() {
		return this.fop;
	}

	/**
	 * Return whether the underlying MQTT client is connected.
	 */
	public boolean isConnected() {
		return this.client != null && this.client.isConnected();
	}

	/**
	 * Return a short summary of the connection state for logging.
	 */
	public String getConnectionSummary() {
		String clientId = this.client != null ? this.client.getClientId() : "(no-client)";
		String server = "(no-server)";
		try {
			if (this.client != null && this.client.getCurrentServerURI() != null) {
				server = this.client.getCurrentServerURI();
			}
		} catch (Throwable t) {
			// defensive: some client implementations throw when not connected
		}
		return String.format("connected=%b clientId=%s server=%s", isConnected(), clientId, server);
	}

	/**
	 * Return summaries for all known MQTT monitors (monitorName -> summary).
	 */
	public static Map<String, String> getAllMonitorSummaries() {
		Map<String, String> summaries = new HashMap<>();
		synchronized (mqttMonitorByName) {
			for (Map.Entry<String, MQTTMonitor> e : mqttMonitorByName.entrySet()) {
				MQTTMonitor mm = e.getValue();
				String s = mm != null ? mm.getConnectionSummary() : "(null)";
				summaries.put(e.getKey(), s);
			}
		}
		return summaries;
	}

	/**
	 * Return safe snapshot of active publishers per monitor (monitorName -> list of publishers).
	 */
	public static Map<String, List<String>> getAllActivePublishers() {
		Map<String, List<String>> result = new HashMap<>();
		synchronized (mqttMonitorByName) {
			for (Map.Entry<String, MQTTMonitor> e : mqttMonitorByName.entrySet()) {
				MQTTMonitor mm = e.getValue();
				if (mm == null) {
					result.put(e.getKey(), List.of());
					continue;
				}
				try {
					var pubs = mm.getActivePublishers();
					result.put(e.getKey(), new ArrayList<>(pubs != null ? pubs : java.util.Set.of()));
				} catch (Throwable t) {
					result.put(e.getKey(), List.of());
				}
			}
		}
		return result;
	}

	private void recordPublisherFromTopic(String topic) {
		if (topic == null) {
			return;
		}
		String[] parts = topic.split("/");
		if (parts.length < 2) {
			return;
		}
		// Derive a stable publisher key: join the topic segments between 'owlcms' and the FOP name
		// Example: 'owlcms/refbox/decision/<fop>' -> 'refbox/decision'
		String publisherId = null;
		String fopName = (this.getFop() != null ? this.getFop().getName() : this.monitoredFopName);
		int fopIndex = -1;
		if (fopName != null) {
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].equals(fopName)) {
					fopIndex = i;
					break;
				}
			}
		}
		if (fopIndex > 1) {
			// join parts[1..fopIndex-1]
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < fopIndex; i++) {
				if (sb.length() > 0) sb.append('/');
				sb.append(parts[i]);
			}
			publisherId = sb.toString();
		} else if (parts.length >= 2) {
			// fallback to the second segment (device/type)
			publisherId = parts[1];
		} else {
			publisherId = topic;
		}

		long now = System.currentTimeMillis();
		lastSeenByPublisher.put(publisherId, now);
		// also mark inferred client id as active
		if (publisherId != null && !publisherId.isBlank()) {
			activeClientIds.put(publisherId, now);
			MQTTInterceptHandlers.putLastSeen(publisherId, now);
			try {
				logger.trace("Updated connectionLastSeen: inferredPublisher='{}' ts={}", publisherId, now);
			} catch (Throwable t) {
				// ignore logging failures
			}
			// if a global client id matches variants of publisherId, update its last seen too
			for (String gid : MQTTInterceptHandlers.getGlobalActiveClientIds()) {
				if (gid != null && (gid.equals(publisherId) || gid.startsWith(publisherId) || publisherId.startsWith(gid))) {
					MQTTInterceptHandlers.putLastSeen(gid, now);
					try {
						logger.trace("Updated connectionLastSeen: brokerClient='{}' ts={} (matched publisherId='{}')", gid, now, publisherId);
					} catch (Throwable t) {
						// ignore logging failures
					}
				}
			}
		}

		// expire old entries: collect expired keys then remove them to avoid iterator.remove() on ConcurrentHashMap
		List<String> expired = new ArrayList<>();
		for (java.util.Map.Entry<String, Long> entry : lastSeenByPublisher.entrySet()) {
			Long ts = entry.getValue();
			if (ts == null) continue;
			if (now - ts > PUBLISHER_TIMEOUT_MS) {
				expired.add(entry.getKey());
			}
		}
		for (String k : expired) {
			lastSeenByPublisher.remove(k);
			activeClientIds.remove(k);
		}
	}

	private void recordPublisherSignature(String topic, byte[] payload) {
		if (topic == null) return;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(topic.getBytes(StandardCharsets.UTF_8));
			if (payload != null) md.update(payload);
			byte[] digest = md.digest();
			String sig = bytesToHex(digest);
			long now = System.currentTimeMillis();
			lastSeenByPublisherSignature.put(sig, now);

			// expire old entries
			List<String> expired = new ArrayList<>();
			for (java.util.Map.Entry<String, Long> entry : lastSeenByPublisherSignature.entrySet()) {
				Long ts = entry.getValue();
				if (ts == null) continue;
				if (now - ts > PUBLISHER_TIMEOUT_MS) expired.add(entry.getKey());
			}
			for (String k : expired) lastSeenByPublisherSignature.remove(k);
		} catch (NoSuchAlgorithmException e) {
			// impossible for SHA-1 on standard JVMs, ignore
		}
	}

	private static String bytesToHex(byte[] bytes) {
		char[] hexArray = "0123456789abcdef".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public Set<String> getActivePublishers() {
		// return a sorted snapshot to avoid exposing the concurrent map's live view
		java.util.Set<String> s = new java.util.TreeSet<>(lastSeenByPublisher.keySet());
		return new java.util.HashSet<>(s);
	}

	/**
	 * Return a snapshot of currently active client ids inferred for this monitor.
	 */
	public Set<String> getActiveClientIds() {
		return new java.util.HashSet<>(new java.util.TreeSet<>(activeClientIds.keySet()));
	}

	/** Return snapshot of client id -> lastSeen for this monitor */
	public Map<String, Long> getActiveClientIdLastSeen() {
		return new HashMap<>(activeClientIds);
	}

	/**
	 * Return a snapshot of active publisher signatures (payload+topic hashes) observed by this monitor.
	 */
	public Set<String> getActivePublisherSignatures() {
		return new java.util.HashSet<>(new java.util.TreeSet<>(lastSeenByPublisherSignature.keySet()));
	}

	/**
	 * Return a snapshot of the raw last-seen timestamps for publishers observed by this monitor.
	 * Useful for debugging presence and timing (publisher -> lastSeenMillis).
	 */
	public Map<String, Long> getLastSeenSnapshot() {
		return new HashMap<>(lastSeenByPublisher);
	}

	/**
	 * Return a snapshot of last-seen maps for all known monitors (monitorName -> (publisher -> lastSeenMillis)).
	 */
	public static Map<String, Map<String, Long>> getAllLastSeenSnapshots() {
		Map<String, Map<String, Long>> result = new HashMap<>();
		synchronized (mqttMonitorByName) {
			for (Map.Entry<String, MQTTMonitor> e : mqttMonitorByName.entrySet()) {
				MQTTMonitor mm = e.getValue();
				if (mm == null) {
					result.put(e.getKey(), Map.of());
					continue;
				}

                
				try {
					result.put(e.getKey(), mm.getLastSeenSnapshot());
				} catch (Throwable t) {
					result.put(e.getKey(), Map.of());
				}
			}
		}
		return result;
	}

	/**
	 * Return snapshot of active publisher signatures across all monitors (monitorName -> set of signatures)
	 */
	public static Map<String, java.util.Set<String>> getAllActivePublisherSignatures() {
		Map<String, java.util.Set<String>> result = new HashMap<>();
		synchronized (mqttMonitorByName) {
			for (Map.Entry<String, MQTTMonitor> e : mqttMonitorByName.entrySet()) {
				MQTTMonitor mm = e.getValue();
				if (mm == null) {
					result.put(e.getKey(), java.util.Set.of());
					continue;
				}

                
				try {
					result.put(e.getKey(), mm.getActivePublisherSignatures());
				} catch (Throwable t) {
					result.put(e.getKey(), java.util.Set.of());
				}
			}
		}
		return result;
	}

	/**
	 * Return active client ids snapshot for all monitors (monitorName -> set of client ids)
	 */
	public static Map<String, java.util.Set<String>> getAllActiveClientIds() {
		Map<String, java.util.Set<String>> result = new HashMap<>();
		synchronized (mqttMonitorByName) {
			for (Map.Entry<String, MQTTMonitor> e : mqttMonitorByName.entrySet()) {
				MQTTMonitor mm = e.getValue();
				if (mm == null) {
					result.put(e.getKey(), java.util.Set.of());
					continue;
				}
				try {
					result.put(e.getKey(), mm.getActiveClientIds());
				} catch (Throwable t) {
					result.put(e.getKey(), java.util.Set.of());
				}
			}
		}
		return result;
	}

	public void publishMqttConfig() {
		PlatformRepository.syncFOPs();
		if (this.fop == null) {
			return;
		}
		publishMqttConfig("owlcms/fop/config");
	}

	public void publishRefDecision(int i, boolean goodLift) throws MqttPersistenceException, MqttException {
		// 0 is the announcer decision, bump by 1.
		String message = Integer.toString(i + 1) + " " + (goodLift ? "good" : "bad");
		this.client.publish("owlcms/refbox/decision/" + this.getFop().getName(),
		        new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
	}

	@SuppressWarnings("unused")
	/*
	 * used to republish a clock start event with information that the triggering device doesn't have.
	 */
	public void publishStartAthleteTimer(UIEvent.StartTime e) {
		try {
			Integer timeRemaining = e.getTimeRemaining();
			Athlete currentAthlete = getFop().getCurAthlete();
			int attemptNumber = currentAthlete.getAttemptNumber();
			LiftDefinition.Stage liftType = currentAthlete.getAttemptsDone() >= 3 ? LiftDefinition.Stage.CLEANJERK : LiftDefinition.Stage.SNATCH;

			if (currentAthlete != null) {
				Map<String, Object> payload = new TreeMap<>();
				payload.put("athleteName", currentAthlete.getFullName());
				payload.put("liftType", liftType.toString());
				payload.put("attemptNumber", attemptNumber);
				payload.put("session", getFop().getGroup().getName());

				String json;
				try {
					json = new ObjectMapper().writeValueAsString(payload);
				} catch (JsonProcessingException ex) {
					json = "";
				}
				this.client.publish("owlcms/fop/start/" + this.getFop().getName(),
				        new MqttMessage((json + " " + timeRemaining).getBytes(StandardCharsets.UTF_8)));
			} else {
				// can't happen. parsers should ignore if less than 2 parts
				this.client.publish("owlcms/fop/start/" + this.getFop().getName(),
				        new MqttMessage("{}".getBytes(StandardCharsets.UTF_8)));
			}
		} catch (MqttPersistenceException e1) {
			logger.error("cannot publish start athlete timer", e1);
		} catch (MqttException e1) {
			logger.error("cannot publish start athlete timer", e1);
		}
	}

	/**
	 * Assign a connection descriptor when a publish is observed at the broker.
	 * If the publishing connection id does NOT start with 'mqtt', the descriptor is the connection id as-is.
	 * If the publishing connection id starts with 'mqtt', override descriptor using the topic:
	 * descriptor = "<platform> <device>" where device is the second topic segment and platform is the last segment.
	 */
	public static void assignDescriptorForPublish(String topic, String publishingClientId) {
		// delegate to intercept handlers which own the global state
		if (publishingClientId == null || publishingClientId.isBlank()) return;
		// let the intercept handlers decide based on their own helpers
		try {
			MQTTInterceptHandlers.putDescriptor(publishingClientId, MQTTMonitor.buildDescriptorFromPublish(topic, publishingClientId));
			MQTTInterceptHandlers.putLastSeen(publishingClientId, System.currentTimeMillis());
		} catch (Throwable t) {
			// swallow to avoid affecting broker processing
		}
	}

	private static String buildDescriptorFromPublish(String topic, String publishingClientId) {
		try {
			if (publishingClientId == null || publishingClientId.isBlank()) return null;
			// Do not assign descriptors for server-originated client IDs (contain '_owlcms_')
			if (MQTTInterceptHandlers.isServerClientId(publishingClientId)) return null;
			// Keep config descriptors when the publishing connection is an MQTT-generated id
			// (i.e. starts with 'mqtt' or other generic prefixes). Only ignore config ids
			// when they are NOT generic (non-mqtt) connections.
			if (MQTTInterceptHandlers.isConfigClientId(publishingClientId) && !MQTTInterceptHandlers.isGenericClientId(publishingClientId)) return null;
			// If this is a generic MQTT client (e.g. mqttjs_* or a_f_*), default to a stable
			// generic descriptor 'mqtt'. Allow topic-derived descriptors to override this
			// except when the topic represents the special 'owlcms/config' channel.
			if (MQTTInterceptHandlers.isGenericClientId(publishingClientId)) {
				if (topic == null || topic.isBlank()) {
					logger.debug("Assigned descriptor='mqtt' to publishing clientId='{}' (no topic)", publishingClientId);
					return "mqtt";
				}
				String[] genericParts = topic.split("/");
				if (genericParts.length >= 2) {
					// If topic is just 'owlcms/config' do NOT let 'config' override the 'mqtt' descriptor
					if (genericParts.length == 2 && "owlcms".equals(genericParts[0]) && "config".equals(genericParts[1])) {
						logger.debug("Assigned descriptor='mqtt' to publishing clientId='{}' (topic='{}' - config suppressed)", publishingClientId, topic);
						return "mqtt";
					}
					// Otherwise attempt to derive a meaningful descriptor from topic and use it
					String device = genericParts.length >= 2 ? genericParts[1] : null;
					String platform = genericParts.length >= 1 ? genericParts[genericParts.length - 1] : null;
					if (device != null && !device.isBlank()) {
						String finalDesc = (platform != null && platform.equals(device)) ? device : (platform != null ? platform + " " + device : device);
						logger.debug("Assigned descriptor='{}' to publishing clientId='{}' from topic='{}' (overrode mqtt)", finalDesc, publishingClientId, topic);
						return finalDesc;
					}
				}
				// Fallback: keep the generic 'mqtt' descriptor
				logger.debug("Assigned descriptor='mqtt' to publishing clientId='{}' (topic='{}' - no better descriptor)", publishingClientId, topic);
				return "mqtt";
			}
			if (topic == null || topic.isBlank()) return null;
			// The special topic 'owlcms/config' must never override an existing descriptor
			if ("owlcms/config".equals(topic)) {
				logger.debug("Topic 'owlcms/config' detected - will not override descriptor for clientId='{}'", publishingClientId);
				return null;
			}
			String[] parts = topic.split("/");
			// Do not derive descriptors from 'owlcms/fop/...' or 'owlcms/led/...' topics
			if (parts.length >= 2 && ("fop".equals(parts[1]) || "led".equals(parts[1]))) {
				logger.debug("Topic '{}' is fop/led - will not derive descriptor for clientId='{}'", topic, publishingClientId);
				return null;
			}
			if (parts.length < 2) return null;
			// If topic is just 'owlcms/config' produce a clearer descriptor 'config'
			if (parts.length == 2 && "owlcms".equals(parts[0]) && "config".equals(parts[1])) {
				logger.debug("Assigned descriptor='config' to publishing clientId='{}' from topic='{}'", publishingClientId, topic);
				return "config";
			}
			String device = parts.length >= 2 ? parts[1] : null;
			String platform = parts.length >= 1 ? parts[parts.length - 1] : null;
			if (device == null || device.isBlank()) return null;
			// Avoid returning duplicate "config config" when device==platform=="config"
			String finalDesc;
			if (platform != null && platform.equals(device)) {
				finalDesc = device;
			} else {
				finalDesc = (platform != null ? platform + " " + device : device);
			}
			logger.debug("Assigned descriptor='{}' to publishing clientId='{}' from topic='{}'", finalDesc, publishingClientId, topic);
			return finalDesc;
		} catch (Throwable t) {
			return null;
		}
	}

	/*
	 * used to republish a clock stop event with information that the triggering device doesn't have.
	 */
	public void publishStopAthleteTimer(UIEvent.StopTime s) {
		Integer timeRemaining = s.getTimeRemaining();
		try {
			this.client.publish("owlcms/fop/stop/" + this.getFop().getName(),
			        new MqttMessage(("" + timeRemaining).getBytes(StandardCharsets.UTF_8)));
		} catch (MqttPersistenceException e1) {
			logger.error("cannot publish stop athlete timer", e1);
		} catch (MqttException e1) {
			logger.error("cannot publish stop athlete timer", e1);
		}
	}

	@SuppressWarnings("unused")
	public void simulateStartAthleteTimer() throws MqttPersistenceException, MqttException {
		Athlete currentAthlete = getFop().getCurAthlete();
		int attemptNumber = currentAthlete.getAttemptNumber();
		LiftDefinition.Stage liftType = currentAthlete.getAttemptsDone() >= 3 ? LiftDefinition.Stage.CLEANJERK : LiftDefinition.Stage.SNATCH;

		if (currentAthlete != null) {
			Map<String, Object> payload = new TreeMap<>();
			payload.put("athleteName", currentAthlete.getFullName());
			payload.put("liftType", liftType.toString());
			payload.put("attemptNumber", attemptNumber);
			payload.put("session", getFop().getGroup().getName());

			String json;
			try {
				json = new ObjectMapper().writeValueAsString(payload);
			} catch (JsonProcessingException e) {
				json = "";
			}
			this.client.publish("owlcms/clock/" + this.getFop().getName(),
			        new MqttMessage(("start " + json).getBytes(StandardCharsets.UTF_8)));
		} else {
			// can't happen
			this.client.publish("owlcms/clock/" + this.getFop().getName(),
			        new MqttMessage("start".getBytes(StandardCharsets.UTF_8)));
		}
	}

	public void simulateStopAthleteTimer() throws MqttPersistenceException, MqttException {
		this.client.publish("owlcms/clock/" + this.getFop().getName(),
		        new MqttMessage("stop".getBytes(StandardCharsets.UTF_8)));
	}

	public void testDownSignal() throws MqttPersistenceException, MqttException {
		try {
			this.publishMqttTimeRemaining(90);
			Thread.sleep(1000);
			this.publishMqttTimeRemaining(30);
			Thread.sleep(1000);
			this.publishMqttTimeRemaining(0);
			Thread.sleep(1000);
			this.publishMqttDownSignal();
		} catch (InterruptedException | MqttException e) {
			LoggerUtils.logError(logger, e);
		}

	}

	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
		// logger.debug("MQTTMonitor setFop {} {} {}\n{}", fop.getName(), System.identityHashCode(fop), System.identityHashCode(this),
		// LoggerUtils.stackTrace());
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
	 * A display or console has triggered the down signal (e.g. keypad connected to a laptop) and down signal post connected via MQTT.
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
		logger.trace("slaveRefereeDecision");
		// the deliberation is about the last athlete judged, not on the current
		// athlete.
		this.callback.setAthleteUnderReview(e.getAthlete());
		publishMqttRefereeDecision(e.ref1, e.ref2, e.ref3);
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		// the deliberation is about the last athlete judged, not on the current
		// athlete.
		this.callback.setAthleteUnderReview(e.getAthlete());
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
		publishStartAthleteTimer(e);
	}

	@Subscribe
	public void slaveTimeStopped(UIEvent.StopTime e) {
		publishStopAthleteTimer(e);
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
		// this.setFop(this.getFop());
		this.getFop().getUiEventBus().register(this);
		this.getFop().getFopEventBus().register(this);

		try {
			logger.info("{}starting MQTT monitoring for {}", FieldOfPlay.getLoggingName(this.getFop()), System.identityHashCode(this));
			String paramMqttServer = Config.getCurrent().getParamMqttServer();
			if (Config.getCurrent().getParamMqttInternal() || (paramMqttServer != null && !paramMqttServer.isBlank())) {
				this.client = createMQTTClient(this.getFop());
				connectionLoop(this.client);
			} else {
				logger.info("no MQTT server configured, skipping");
			}
		} catch (MqttException e) {
			logger.error("cannot initialize MQTT: {}", LoggerUtils.stackTrace(e));
		}
	}

	@Override
	public void unregister() {
		// we do nothing. We now have exactly one MQTTMonitor per platform name
		// and we reuse it if we ever recreate the field of play
	}

	private void connectionLoop(MqttAsyncClient mqttAsyncClient) {
		while (!mqttAsyncClient.isConnected()) {
			try {
				// doConnect will generate a new client Id, and wait for completion
				// client.reconnect() and automaticReconnection do not work as I expect.
				doConnect();
			} catch (MqttException me) {
				if (me.getReasonCode() == MqttException.REASON_CODE_CLIENT_CONNECTED) {
					try {
						doConnect();
					} catch (MqttException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e1) {
				Main.getStartupLogger().error("{}MQTT refereeing device server: {}", FieldOfPlay.getLoggingName(this.getFop()),
				        e1.getCause() != null ? e1.getCause().getMessage() : e1);
				logger.error("{}MQTT refereeing device server: {}", FieldOfPlay.getLoggingName(this.getFop()),
				        e1.getCause() != null ? e1.getCause().getMessage() : e1);
				break;
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
		logger.info("{}connected to {} MQTT broker {}",
		        FieldOfPlay.getLoggingName(this.fop),
		        (external ? "external" : "embedded"),
		        this.client.getCurrentServerURI());

		this.client.subscribe(this.callback.deprecatedDecisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.deprecatedDecisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.decisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.decisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.downEmittedTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.downEmittedTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.juryBreakTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.juryBreakTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.juryMemberDecisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.juryMemberDecisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.juryDecisionTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.juryDecisionTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.jurySummonTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.jurySummonTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.clockTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.clockTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.testTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.testTopicName,
		        this.client.getCurrentServerURI());
		this.client.subscribe(this.callback.configTopicName, 0);
		logger.trace("{}MQTT subscribe {} {}", FieldOfPlay.getLoggingName(this.getFop()), this.callback.configTopicName,
		        this.client.getCurrentServerURI());
		// subscribe to broker $SYS topics to detect client connections when available
		try {
			this.client.subscribe("$SYS/#", 0);
			logger.trace("{}MQTT subscribe $SYS/# {}", FieldOfPlay.getLoggingName(this.getFop()), this.client.getCurrentServerURI());
		} catch (MqttException me) {
			logger.debug("{}could not subscribe to $SYS topics: {}", FieldOfPlay.getLoggingName(this.getFop()), me.getMessage());
		}
	}

	private void doPublishMQTTSummon(int ref) throws MqttException, MqttPersistenceException {
		String topic = "owlcms/fop/summon/" + this.getFop().getName();
		this.client.publish(topic, new MqttMessage(Integer.toString(ref).getBytes(StandardCharsets.UTF_8)));
		String deprecatedTopic = "owlcms/summon/" + this.getFop().getName() + "/" + ref;
		this.client.publish(deprecatedTopic, new MqttMessage(("on").getBytes(StandardCharsets.UTF_8)));
	}

	@SuppressWarnings("unused")
	private String getMonitoredFopName() {
		return this.monitoredFopName;
	}

	private void publishMqttBreak(BreakStarted e) throws MqttPersistenceException, MqttException {
		try {
			this.client.publish("owlcms/fop/break/" + this.getFop().getName(),
			        new MqttMessage(e.getBreakType().name().getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e1) {
			logger.error("mqttBreak event error - {}", e.getTrace());
		}
	}

	private void publishMqttCeremony(UIEvent e, boolean b) throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/ceremony/" + this.getFop().getName();
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
		String topic = "owlcms/fop/challenge/" + this.getFop().getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttConfig(String topic) {
		Map<String, Object> payload = new TreeMap<>();
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		if (fops != null && fops.isEmpty()) {
			// there is always a default Fop unless we get the message prematurely
			// when are not fully initialized
			return;
		}
		List<String> platforms = fops != null ? fops.stream().map(p -> p.getPlatform().getName())
		        .collect(Collectors.toList()) : new ArrayList<>();
		payload.put("platforms", platforms);
		payload.put("version", StartupUtils.getVersion());
		payload.put("jurySize", Competition.getCurrent().getJurySize());
		try {
			String json = new ObjectMapper().writeValueAsString(payload);
			logger.debug("{}{} MQTT Config: {}", FieldOfPlay.getLoggingName(this.getFop()), System.identityHashCode(this), json);
			this.client.publish(topic, new MqttMessage(json.getBytes(StandardCharsets.UTF_8)));
		} catch (JsonProcessingException | MqttException e) {
		}
	}

	private void publishMqttDownSignal() throws MqttException, MqttPersistenceException {
		String topic = "owlcms/fop/down/" + this.getFop().getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttGroupDone(GroupDone e) throws MqttPersistenceException, MqttException {
		this.client.publish("owlcms/fop/break/" + this.getFop().getName(),
		        new MqttMessage(BreakType.GROUP_DONE.name().getBytes(StandardCharsets.UTF_8)));
	}

	private void publishMqttJuryDeliberation() throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/juryDeliberation/" + this.getFop().getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttJuryMemberDecision(Integer juryMemberUpdated) {
		String topic = "owlcms/fop/juryMemberDecision/" + this.getFop().getName();
		try {
			String message = Integer.toString(juryMemberUpdated + 1) + " hidden";
			this.client.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e) {
		}
	}

	private void publishMqttJuryReveal(int jurySize, Boolean[] juryMemberDecision) {
		String topic = "owlcms/fop/juryMemberDecision/" + this.getFop().getName();
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
		String topic = "owlcms/fop/startup/" + this.getFop().getName();
		String deprecatedTopic = "owlcms/led/" + this.getFop().getName();
		this.client.publish(topic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
		this.client.publish(deprecatedTopic, new MqttMessage("on".getBytes(StandardCharsets.UTF_8)));
		sleep(1000);
		this.client.publish(topic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
		this.client.publish(deprecatedTopic, new MqttMessage("off".getBytes(StandardCharsets.UTF_8)));
	}

	private void publishMqttLiftingOrderUpdated() throws MqttPersistenceException, MqttException {
		if (getFop() == null) {
			return;
		}
		String topic = "owlcms/fop/liftingOrderUpdated/" + this.getFop().getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttRefereeDecision(Boolean ref1, Boolean ref2, Boolean ref3) {
		boolean decision = false;
		if (ref1 == null || ref2 == null || ref3 == null) {
			if (ref1 != null) {
				decision = ref1;
			} else if (ref2 != null) {
				decision = ref2;
			} else if (ref3 != null) {
				decision = ref3;
			}
		} else {
			decision = (ref1 && ref2) || (ref1 && ref3) || (ref2 && ref3);
		}
		try {
			this.client.publish("owlcms/fop/refereesDecision/" + this.getFop().getName(),
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
			logger.debug("{}MQTT publishMqttRefereeUpdates {}({}) {}({}) {}({})", FieldOfPlay.getLoggingName(this.getFop()), ref1,
			        ref1Time,
			        ref2, ref2Time, ref3, ref3Time);
			try {
				if (ref1 != null) {
					this.client.publish("owlcms/fop/decision/" + this.getFop().getName(),
					        new MqttMessage((1 + " " + (ref1 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
				}
				if (ref2 != null) {
					this.client.publish("owlcms/fop/decision/" + this.getFop().getName(),
					        new MqttMessage((2 + " " + (ref2 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
				}
				if (ref3 != null) {
					this.client.publish("owlcms/fop/decision/" + this.getFop().getName(),
					        new MqttMessage((3 + " " + (ref3 ? "good" : "bad")).getBytes(StandardCharsets.UTF_8)));
				}
			} catch (MqttException e1) {
			}
		} else {
			logger.debug("{}MQTT skipping out-of-date publishMqttRefereeUpdates {}({}) {}({}) {}({})",
			        FieldOfPlay.getLoggingName(this.getFop()), ref1, ref1Time,
			        ref2, ref2Time, ref3, ref3Time);
		}
		this.prevRefereeTimeStamp = curRefereeUpdateTimeStamp.isPresent() ? curRefereeUpdateTimeStamp.get() : 0L;
	}

	private void publishMqttResetAllDecisions() {
		logger.debug("{}MQTT resetDecisions", FieldOfPlay.getLoggingName(this.getFop()));
		try {
			this.client.publish("owlcms/fop/resetDecisions/" + this.getFop().getName(),
			        new MqttMessage("reset".getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e1) {

		}
	}

	private void publishMqttStartLifting() throws MqttPersistenceException, MqttException {
		String topic = "owlcms/fop/startLifting/" + this.getFop().getName();
		this.client.publish(topic, new MqttMessage());
	}

	private void publishMqttSummonRef(int ref) {
		logger.debug("{}MQTT summon {}", FieldOfPlay.getLoggingName(this.getFop()), ref);
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
		logger.debug("{}MQTT timeRemaining {}", FieldOfPlay.getLoggingName(this.getFop()), tr);
		try {
			this.client.publish("owlcms/fop/timeRemaining/" + this.getFop().getName(),
			        new MqttMessage(Integer.toString(tr).getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e1) {
			logger.error("could not publish timeRemaining {}", e1.getCause());
		}
	}

	private void publishMqttWakeUpRef(int ref, boolean on) {
		logger.debug("{}MQTT decisionRequest {} {}", FieldOfPlay.getLoggingName(this.getFop()), ref, on);
		try {
			FOPState state = this.getFop().getState();
			if (state != FOPState.DOWN_SIGNAL_VISIBLE
			        && state != FOPState.TIME_RUNNING
			        && state != FOPState.TIME_STOPPED) {
				// boundary condition where the wait thread to remind referee is not cancelled
				// in time; should not happen, this is defensive.
				return;
			}
			String topic = "owlcms/fop/decisionRequest/" + this.getFop().getName();
			if (on) {
				this.client.publish(topic, new MqttMessage(Integer.toString(ref).getBytes(StandardCharsets.UTF_8)));
			} else {
				// off is not sent in modern mode.
			}

			// Legacy : specific referee is added at the end of the topic.
			String deprecatedTopic = "owlcms/decisionRequest/" + this.getFop().getName() + "/" + ref;
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

	private void setMonitoredFopName(String monitorName) {
		this.monitoredFopName = monitorName;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Called by broker integrations or $SYS parsing when a client connected.
	 */
	public void notifyClientConnected(String clientId) {
		if (clientId == null || clientId.isBlank()) return;
		activeClientIds.put(clientId, System.currentTimeMillis());
		try {
			String monitorName = this.getMonitoredFopName();
			logger.info("{} MQTT client connected: monitor={} clientId={}", FieldOfPlay.getLoggingName(this.getFop()), monitorName, clientId);
		} catch (Throwable t) {
			// defensive: logging must not throw
		}
	}

	/**
	 * Called by broker integrations or $SYS parsing when a client disconnected.
	 */
	public void notifyClientDisconnected(String clientId) {
		if (clientId == null || clientId.isBlank()) return;
		activeClientIds.remove(clientId);
		// cleanup any descriptor we kept for this connection
		MQTTInterceptHandlers.removeDescriptor(clientId);
		MQTTInterceptHandlers.removeLastSeen(clientId);
	}

	/**
	 * Broker-level notification: record a globally active client id across the application.
	 */
	public static void notifyGlobalClientConnected(String clientId) {
		MQTTInterceptHandlers.notifyGlobalClientConnected(clientId);
	}

	/**
	 * Broker-level notification: remove a globally active client id.
	 */
	public static void notifyGlobalClientDisconnected(String clientId) {
		MQTTInterceptHandlers.notifyGlobalClientDisconnected(clientId);
	}

    /**
     * Return a snapshot of connection descriptors (clientId -> descriptor).
     */
    public static Map<String, String> getConnectionDescriptorsSnapshot() {
		return MQTTInterceptHandlers.getConnectionDescriptorsSnapshot();
    }

	/** Return a snapshot of connection last-seen timestamps (clientId -> lastSeenMillis). */
	public static Map<String, Long> getConnectionLastSeenSnapshot() {
		return MQTTInterceptHandlers.getConnectionLastSeenSnapshot();
	}

	// Remote address snapshot API removed.

	/**
	 * Return a snapshot of global active client ids as reported by the broker.
	 */
	public static java.util.Set<String> getGlobalActiveClientIds() {
		return MQTTInterceptHandlers.getGlobalActiveClientIds();
	}

	/**
	 * Remove a specific global client id from the registry (manual cleanup API).
	 */
	public static void removeGlobalClient(String clientId) {
		MQTTInterceptHandlers.removeGlobalClient(clientId);
	}

	/**
	 * Clear all known global client ids. Useful for testing or manual reset.
	 */
	public static void resetGlobalActiveClients() {
		MQTTInterceptHandlers.resetGlobalActiveClients();
	}

	/**
	 * Reconcile the application registry with the broker's session list.
	 * This method uses reflection to attempt to extract a list of active client ids
	 * from the embedded Moquette broker instance (`Main.mqttBroker`). It will
	 * remove any global client id that is not present in the broker's authoritative list.
	 */
	private static void reconcileWithBroker() {
		try {
			// Main.mqttBroker is private; access it reflectively to avoid visibility issues
			Object broker = null;
			try {
				java.lang.reflect.Field f = Main.class.getDeclaredField("mqttBroker");
				f.setAccessible(true);
				broker = f.get(null);
			} catch (Throwable t) {
				// fallback: try public field access (unlikely)
				try { broker = Main.class.getField("mqttBroker").get(null); } catch (Throwable t2) {}
			}
			if (broker == null) return;

			java.util.Set<String> brokerClients = new java.util.HashSet<>();

			Class<?> cls = broker.getClass();
			// Try public methods first
			for (java.lang.reflect.Method m : cls.getMethods()) {
				String name = m.getName().toLowerCase();
				if (!(name.contains("session") || name.contains("sessions") || name.contains("client") || name.contains("clients") )) continue;
				try {
					Object res = m.invoke(broker);
					if (res == null) continue;
					collectClientIdsFromObject(res, brokerClients);
				} catch (Throwable t) {
					// ignore individual method failures
				}
			}

			// If nothing found, try declared fields
			if (brokerClients.isEmpty()) {
				for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
					String fname = f.getName().toLowerCase();
					if (!(fname.contains("session") || fname.contains("sessions") || fname.contains("client") || fname.contains("clients"))) continue;
					try {
						f.setAccessible(true);
						Object val = f.get(broker);
						if (val == null) continue;
						collectClientIdsFromObject(val, brokerClients);
					} catch (Throwable t) {
						// ignore
					}
				}
			}

			if (brokerClients.isEmpty()) {
				// nothing to reconcile
				return;
			}

			java.util.Set<String> known = new java.util.HashSet<>(MQTTInterceptHandlers.getGlobalActiveClientIds());
			for (String k : known) {
				if (!brokerClients.contains(k)) {
					// remove stale
					MQTTInterceptHandlers.removeGlobalClient(k);
					try {
						logger.info("Broker-level client reconciled and removed: clientId={} reason=missing_in_broker_sessions", k);
					} catch (Throwable t) {
					}
				}
			}
		} catch (Throwable t) {
			logger.debug("Unexpected error during broker reconciliation", t);
		}
	}

	private static void collectClientIdsFromObject(Object res, java.util.Set<String> out) {
		if (res == null) return;
		if (res instanceof java.util.Collection) {
			for (Object e : (java.util.Collection<?>) res) {
				if (e == null) continue;
				if (e instanceof String) {
					out.add(((String) e).trim());
				} else {
					// try common getter names
					try {
						java.lang.reflect.Method m = e.getClass().getMethod("getClientID");
						Object v = m.invoke(e);
						if (v != null) out.add(v.toString());
						continue;
					} catch (Throwable t) {
					}
					try {
						java.lang.reflect.Method m = e.getClass().getMethod("clientID");
						Object v = m.invoke(e);
						if (v != null) out.add(v.toString());
						continue;
					} catch (Throwable t) {
					}
					try {
						java.lang.reflect.Method m = e.getClass().getMethod("getClientId");
						Object v = m.invoke(e);
						if (v != null) out.add(v.toString());
						continue;
					} catch (Throwable t) {
					}
					// fallback to toString tokenizing
					String s = e.toString();
					if (s != null && s.length() > 0) {
						// split on non-word to pick client id-like tokens
						for (String token : s.split("[^A-Za-z0-9_\\-]+")) {
							if (token.length() > 1) out.add(token);
						}
					}
				}
			}
		} else if (res instanceof java.util.Map) {
			out.addAll(((java.util.Map<?, ?>) res).keySet().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()));
		} else {
			// try to inspect object for iterable-like methods
			try {
				java.lang.reflect.Method m = res.getClass().getMethod("getAllClientIds");
				Object v = m.invoke(res);
				collectClientIdsFromObject(v, out);
				return;
			} catch (Throwable t) {
			}
			try {
				java.lang.reflect.Method m = res.getClass().getMethod("connectedClients");
				Object v = m.invoke(res);
				collectClientIdsFromObject(v, out);
				return;
			} catch (Throwable t) {
			}
			// last resort: toString tokenization
			String s = res.toString();
			if (s != null && s.length() > 0) {
				for (String token : s.split("[^A-Za-z0-9_\\-]+")) {
					if (token.length() > 1) out.add(token);
				}
			}
		}
	}

}
