/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private static final String NATIVE_SERVICE_TYPE = "_owlcms._tcp";
	private static final long VERIFICATION_DELAY_MILLIS = 3000;
	private final static Logger logger = (Logger) LoggerFactory.getLogger(MdnsResponder.class);
	private static final List<JmDNS> instances = new ArrayList<>();
	private static Process nativeResponder;
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
			Process process = nativeResponder;
			nativeResponder = null;
			if (process != null) {
				process.destroy();
			}
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
		Map<String, List<InetAddress>> announcedAddresses = new LinkedHashMap<>();
		try {
			IfaceAddress candidate = selectCandidateAddress(IPInterfaceUtils.getCandidateAddresses());
			if (candidate == null) {
				logger./**/warn("could not announce {}: no eligible network address", requestedName);
				return;
			}
			InetAddress address = candidate.address();
			if (IPInterfaceUtils.isMacOs()) {
				announceWithNativeResponder(requestedName, port, candidate, announcedAddresses);
				verifyAnnouncements(announcedAddresses);
				return;
			}
			try {
				JmDNS jmdns = JmDNS.create(address, requestedName);
				jmdns.registerService(ServiceInfo.create(SERVICE_TYPE, requestedName, port, ""));
				synchronized (instances) {
					instances.add(jmdns);
				}
				String hostName = stripTrailingDot(jmdns.getHostName());
				announcedAddresses.computeIfAbsent(hostName, ignored -> new ArrayList<>()).add(address);
				logger.info("announcing {} on {} ({})", hostName, address.getHostAddress(), candidate.iface().getName());
			} catch (IOException e) {
				logger.warn("could not announce {} on {}: {}", requestedName, address.getHostAddress(), e.getMessage());
			}
			verifyAnnouncements(announcedAddresses);
		} catch (Exception e) {
			logger.warn("could not announce {}: {}", requestedName, e.getMessage());
		}
	}

	private static void announceWithNativeResponder(String requestedName, int port, IfaceAddress candidate,
	        Map<String, List<InetAddress>> announcedAddresses) throws IOException {
		InetAddress address = candidate.address();
		String hostName = requestedName + ".local";
		Process process = new ProcessBuilder("/usr/bin/dns-sd", "-P", requestedName, NATIVE_SERVICE_TYPE, "local.",
		        Integer.toString(port), hostName + ".", address.getHostAddress())
		        .redirectErrorStream(true)
		        .start();
		synchronized (instances) {
			nativeResponder = process;
			registeredHostName = hostName;
		}
		announcedAddresses.computeIfAbsent(hostName, ignored -> new ArrayList<>()).add(address);
		logger.info("announcing {} on {} ({}) using macOS mDNSResponder", hostName, address.getHostAddress(),
		        candidate.iface().getName());

		Thread outputReader = new Thread(() -> monitorNativeResponder(process), "mdns-native-responder");
		outputReader.setDaemon(true);
		outputReader.start();
	}

	private static void monitorNativeResponder(Process process) {
		try (BufferedReader reader = process.inputReader()) {
			String line;
			while ((line = reader.readLine()) != null) {
				logger.debug("dns-sd: {}", line);
			}
			int exitCode = process.waitFor();
			synchronized (instances) {
				if (nativeResponder == process) {
					nativeResponder = null;
					registeredHostName = null;
					logger./**/warn("macOS mDNS responder exited with code {}", exitCode);
				}
			}
		} catch (IOException e) {
			logger.warn("could not read macOS mDNS responder output: {}", e.getMessage());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static IfaceAddress selectCandidateAddress(List<IfaceAddress> candidates) {
		IfaceAddress firstEligible = null;
		for (IfaceAddress candidate : candidates) {
			InetAddress address = candidate.address();
			// link-local covers the no-DHCP switch case (169.254.x.x) where mDNS matters most
			if (address.isLoopbackAddress()
			        || !(address.isSiteLocalAddress() || address.isLinkLocalAddress())) {
				continue;
			}
			if (firstEligible == null) {
				firstEligible = candidate;
			}
			if (IPInterfaceUtils.isWiredInterface(candidate.iface().getName())) {
				return candidate;
			}
		}
		return firstEligible;
	}

	private static void verifyAnnouncements(Map<String, List<InetAddress>> announcedAddresses) {
		try {
			Thread.sleep(VERIFICATION_DELAY_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}

		for (Map.Entry<String, List<InetAddress>> announcement : announcedAddresses.entrySet()) {
			String hostName = announcement.getKey();
			try {
				InetAddress[] resolvedAddresses = InetAddress.getAllByName(hostName);
				boolean resolvesToAnnouncedAddress = Arrays.stream(resolvedAddresses)
				        .anyMatch(announcement.getValue()::contains);
				if (resolvesToAnnouncedAddress) {
					synchronized (instances) {
						if (instances.isEmpty() && (nativeResponder == null || !nativeResponder.isAlive())) {
							return;
						}
						registeredHostName = hostName;
					}
					logger.info("verified {} resolves to an announced address", hostName);
					return;
				}
				logger./**/warn("not publishing {} URL: resolved addresses {} do not include announced addresses {}",
				        hostName, Arrays.toString(resolvedAddresses), announcement.getValue());
			} catch (UnknownHostException e) {
				logger./**/warn("not publishing {} URL: name does not resolve on this machine", hostName);
			}
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
