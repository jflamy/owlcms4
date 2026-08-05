/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.IPInterfaceUtils.IfaceAddress;
import ch.qos.logback.classic.Logger;

/**
 * Announces a stable "owlcms.local" name on the private LANs the machine is connected to.
 *
 * Creating a JmDNS instance with an explicit host name makes it answer mDNS queries for that name; one instance is
 * needed per interface. mDNS only makes sense on a private LAN, so only site-local addresses are used.
 */
public class MdnsResponder {

	private static final String SERVICE_TYPE = "_owlcms._tcp.local.";
	private final static Logger logger = (Logger) LoggerFactory.getLogger(MdnsResponder.class);
	private static final List<JmDNS> instances = new ArrayList<>();
	private static volatile String registeredHostName;

	/**
	 * @return the name actually registered (e.g. "owlcms.local"), or null if not announcing. JmDNS renames on
	 *         collision, so this is not necessarily derived from the requested name.
	 */
	public static String getRegisteredHostName() {
		return registeredHostName;
	}

	/** Announces the name in the background; probing and collision resolution take a few seconds. */
	public static void start(String requestedName, int port) {
		if (hasRequestedHostName(requestedName)) {
			logger.info("not aliasing {}.local because it is already the server host name", requestedName);
			return;
		}
		Thread announcer = new Thread(() -> announce(requestedName, port), "mdns-responder");
		announcer.setDaemon(true);
		announcer.start();
	}

	public static void stop() {
		synchronized (instances) {
			for (JmDNS jmdns : instances) {
				try {
					jmdns.unregisterAllServices();
					jmdns.close();
				} catch (IOException e) {
					logger.debug("could not close mDNS responder: {}", e.getMessage());
				}
			}
			instances.clear();
			registeredHostName = null;
		}
	}

	private static void announce(String requestedName, int port) {
		try {
			for (IfaceAddress candidate : IPInterfaceUtils.getCandidateAddresses()) {
				InetAddress address = candidate.address();
				// link-local covers the no-DHCP switch case (169.254.x.x) where mDNS matters most
				if (address.isLoopbackAddress()
				        || !(address.isSiteLocalAddress() || address.isLinkLocalAddress())) {
					continue;
				}
				try {
					JmDNS jmdns = JmDNS.create(address, requestedName);
					jmdns.registerService(ServiceInfo.create(SERVICE_TYPE, requestedName, port, ""));
					synchronized (instances) {
						instances.add(jmdns);
					}
					registeredHostName = stripTrailingDot(jmdns.getHostName());
					logger.info("announcing {} on {}", registeredHostName, address.getHostAddress());
				} catch (IOException e) {
					logger.warn("could not announce {} on {}: {}", requestedName, address.getHostAddress(),
					        e.getMessage());
				}
			}
		} catch (Exception e) {
			logger.warn("could not announce {}: {}", requestedName, e.getMessage());
		}
	}

	private static String stripTrailingDot(String hostName) {
		if (hostName != null && hostName.endsWith(".")) {
			return hostName.substring(0, hostName.length() - 1);
		}
		return hostName;
	}

	private static boolean hasRequestedHostName(String requestedName) {
		try {
			String hostName = InetAddress.getLocalHost().getHostName();
			return requestedName.equalsIgnoreCase(hostName)
			        || (requestedName + ".local").equalsIgnoreCase(hostName);
		} catch (IOException e) {
			logger.debug("could not determine server host name: {}", e.getMessage());
			return false;
		}
	}

}
