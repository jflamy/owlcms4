/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import org.slf4j.LoggerFactory;

import app.owlcms.monitors.MQTTInterceptHandlers;
import app.owlcms.simulation.CompetitionSimulator;
import ch.qos.logback.classic.Logger;
import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;

public class MoquetteAuthorizatorPolicy implements IAuthorizatorPolicy {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(MoquetteAuthorizatorPolicy.class);

	@Override
	public boolean canWrite(Topic topic, String user, String client) {
		String topicName = topic != null ? topic.toString() : null;
		boolean simulationRunning = CompetitionSimulator.isRunning();
		if (simulationRunning
		        && isInboundFopControlTopic(topicName)
		        && !MQTTInterceptHandlers.isServerClientId(client)) {
			logger./**/warn("MQTT external inbound control message denied during simulation: clientId={} topic={}", client, topicName);
			return false;
		}
		return true;
	}

	@Override
	public boolean canRead(Topic topic, String user, String client) {
		return true;
	}

	private boolean isInboundFopControlTopic(String topic) {
		return topic != null && (topic.startsWith("owlcms/refbox/decision/")
		        || topic.startsWith("owlcms/decision/")
		        || topic.startsWith("owlcms/refbox/downEmitted/")
		        || topic.startsWith("owlcms/clock/")
		        || topic.startsWith("owlcms/jurybox/break/")
		        || topic.startsWith("owlcms/jurybox/juryMember/decision/")
		        || topic.startsWith("owlcms/jurybox/decision/")
		        || topic.startsWith("owlcms/jurybox/summon/"));
	}
}
