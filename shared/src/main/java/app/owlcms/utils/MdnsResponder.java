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
	private static final String NATIVE_SERVICE_TYPE = "_owlcms._tcp";
	private static final long VERIFICATION_DELAY_MILLIS = 3000;
	private static final int MAX_NAME_COLLISIONS = 10;
	/** How long stop() lets the responders withdraw their announcements before returning. */
	private static final long STOP_WAIT_MILLIS = 500;
	private final static Logger logger = (Logger) LoggerFactory.getLogger(MdnsResponder.class);
	private static final List<JmDNS> instances = new ArrayList<>();
	private static Process nativeResponder;
	private static volatile String registeredHostName;

	private enum NativeRegistration {
		REGISTERED, CONFLICT, FAILED
	}

	private enum Verification {
		VERIFIED, CONFLICT, UNVERIFIED
	}

	/**
	 * @return the name actually registered (e.g. "owlcms.local"), or null if not announcing. The name is renamed on
	 *         collision (e.g. "owlcms-2.local"), so this is not necessarily the requested name.
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

	/**
	 * Stops announcing. Withdrawing a JmDNS announcement waits for its goodbye packets, up to 5 s per responder, so it
	 * runs in the background and the caller waits at most {@link #STOP_WAIT_MILLIS}: a stopping server must not wait
	 * for it.
	 */
	public static void stop() {
		List<JmDNS> responders;
		synchronized (instances) {
			Process process = nativeResponder;
			nativeResponder = null;
			if (process != null) {
				process.destroy();
			}
			responders = new ArrayList<>(instances);
			instances.clear();
			registeredHostName = null;
		}
		Thread closer = closeInBackground(responders);
		if (closer != null) {
			try {
				closer.join(STOP_WAIT_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/** @return the thread closing the responders, or null if there are none */
	private static Thread closeInBackground(List<JmDNS> responders) {
		if (responders.isEmpty()) {
			return null;
		}
		// close() also unregisters the services
		Thread closer = new Thread(() -> {
			for (JmDNS jmdns : responders) {
				try {
					jmdns.close();
				} catch (IOException e) {
					logger.debug("could not close mDNS responder: {}", e.getMessage());
				}
			}
		}, "mdns-close");
		closer.setDaemon(true);
		closer.start();
		return closer;
	}

	private static void announce(String requestedName, int port) {
		try {
			IfaceAddress candidate = selectCandidateAddress(IPInterfaceUtils.getCandidateAddresses());
			if (candidate == null) {
				logger./**/warn("could not announce {}: no eligible network address", requestedName);
				return;
			}
			if (IPInterfaceUtils.isMacOs()) {
				announceWithNativeResponder(requestedName, port, candidate);
				return;
			}
			announceWithJmDNS(requestedName, port, candidate);
		} catch (Exception e) {
			logger.warn("could not announce {}: {}", requestedName, e.getMessage());
		}
	}

	/** owlcms, owlcms-2, owlcms-3..., the suffix scheme of JmDNS and of the macOS responder. */
	private static String candidateName(String requestedName, int attempt) {
		return attempt == 1 ? requestedName : requestedName + "-" + attempt;
	}

	/**
	 * JmDNS does not always detect that another host already answers for the name, so each name is verified by
	 * resolving it; when it resolves elsewhere, the responder stops claiming it and the next name is tried.
	 */
	private static void announceWithJmDNS(String requestedName, int port, IfaceAddress candidate)
	        throws IOException {
		InetAddress address = candidate.address();
		for (int attempt = 1; attempt <= MAX_NAME_COLLISIONS; attempt++) {
			String name = candidateName(requestedName, attempt);
			JmDNS jmdns = JmDNS.create(address, name);
			synchronized (instances) {
				instances.add(jmdns);
			}
			jmdns.registerService(ServiceInfo.create(SERVICE_TYPE, name, port, ""));
			String hostName = stripTrailingDot(jmdns.getHostName());
			logger.info("announcing {} on {} ({})", hostName, address.getHostAddress(), candidate.iface().getName());

			if (verifyAnnouncement(jmdns, hostName, address) != Verification.CONFLICT) {
				return;
			}
			synchronized (instances) {
				if (!instances.remove(jmdns)) {
					// stop() was called while verifying; it closes the responder
					return;
				}
			}
			closeInBackground(List.of(jmdns));
			logger.info("{} is already in use on the network, trying another name", hostName);
		}
		logger./**/warn("could not announce {}: no free name after {} attempts", requestedName, MAX_NAME_COLLISIONS);
	}

	private static void announceWithNativeResponder(String requestedName, int port, IfaceAddress candidate)
	        throws IOException {
		InetAddress address = candidate.address();
		for (int attempt = 1; attempt <= MAX_NAME_COLLISIONS; attempt++) {
			String name = candidateName(requestedName, attempt);
			String hostName = name + ".local";
			Process process = new ProcessBuilder("/usr/bin/dns-sd", "-P", name, NATIVE_SERVICE_TYPE, "local.",
			        Integer.toString(port), hostName + ".", address.getHostAddress())
			        .redirectErrorStream(true)
			        .start();
			synchronized (instances) {
				nativeResponder = process;
			}
			BufferedReader reader = process.inputReader();
			NativeRegistration result = awaitNativeRegistration(reader, hostName);
			if (result == NativeRegistration.REGISTERED) {
				// mDNSResponder also serves local lookups, and a single lookup can transiently miss
				synchronized (instances) {
					if (nativeResponder != process) {
						return;
					}
					registeredHostName = hostName;
				}
				logger.info("announcing {} on {} ({}) using macOS mDNSResponder", hostName, address.getHostAddress(),
				        candidate.iface().getName());
				Thread outputReader = new Thread(() -> monitorNativeResponder(process, reader),
				        "mdns-native-responder");
				outputReader.setDaemon(true);
				outputReader.start();
				return;
			}
			synchronized (instances) {
				if (nativeResponder != process) {
					// stop() was called while probing
					return;
				}
				nativeResponder = null;
			}
			process.destroy();
			reader.close();
			if (result != NativeRegistration.CONFLICT) {
				logger./**/warn("could not announce {}: macOS mDNS responder exited", hostName);
				return;
			}
			logger.info("{} is already in use on the network, trying another name", hostName);
		}
		logger./**/warn("could not announce {}: no free name after {} attempts", requestedName,
		        MAX_NAME_COLLISIONS);
	}

	private static NativeRegistration awaitNativeRegistration(BufferedReader reader, String hostName)
	        throws IOException {
		String recordReply = "Got a reply for record " + hostName + ".";
		String line;
		while ((line = reader.readLine()) != null) {
			logger.debug("dns-sd: {}", line);
			if (line.contains(recordReply)) {
				if (line.contains("Name now registered and active")) {
					return NativeRegistration.REGISTERED;
				}
				if (line.contains("Name in use")) {
					return NativeRegistration.CONFLICT;
				}
			}
		}
		return NativeRegistration.FAILED;
	}

	private static void monitorNativeResponder(Process process, BufferedReader processOutput) {
		try (BufferedReader reader = processOutput) {
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

	/**
	 * Publishes hostName once it resolves to the announced address. A name that resolves elsewhere belongs to another
	 * host. A name that does not resolve on this machine is still announced, but not published.
	 */
	private static Verification verifyAnnouncement(JmDNS jmdns, String hostName, InetAddress address) {
		try {
			Thread.sleep(VERIFICATION_DELAY_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Verification.UNVERIFIED;
		}
		try {
			InetAddress[] resolvedAddresses = InetAddress.getAllByName(hostName);
			if (Arrays.asList(resolvedAddresses).contains(address)) {
				synchronized (instances) {
					if (!instances.contains(jmdns)) {
						return Verification.UNVERIFIED;
					}
					registeredHostName = hostName;
				}
				logger.info("verified {} resolves to an announced address", hostName);
				return Verification.VERIFIED;
			}
			logger.info("{} resolves to {}, not to the announced address {}", hostName,
			        Arrays.toString(resolvedAddresses), address.getHostAddress());
			return Verification.CONFLICT;
		} catch (UnknownHostException e) {
			logger./**/warn("not publishing {} URL: name does not resolve on this machine", hostName);
			return Verification.UNVERIFIED;
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
