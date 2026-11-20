/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

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
	private static final int MAX_NAME_COLLISIONS = 10;
	/** How long stop() lets the responders withdraw their announcements before returning. */
	private static final long STOP_WAIT_MILLIS = 500;
	/** How long to wait for another host to answer for a name before claiming it, split over two queries. */
	private static final long PROBE_TIMEOUT_MILLIS = 1000;
	private static final int PROBE_QUERIES = 2;
	private static final int MDNS_PORT = 5353;
	private static final int TYPE_A = 1;
	private static final int TYPE_AAAA = 28;
	private static final int CLASS_IN = 1;
	private final static Logger logger = (Logger) LoggerFactory.getLogger(MdnsResponder.class);
	private static final List<JmDNS> instances = new ArrayList<>();
	private static Process nativeResponder;
	private static volatile String registeredHostName;
	/** Completed with the registered name, or null, when the current search ends. */
	private static volatile CompletableFuture<String> registration = CompletableFuture.completedFuture(null);

	private enum NativeRegistration {
		REGISTERED, CONFLICT, FAILED
	}

	/**
	 * @return the name actually registered (e.g. "owlcms.local"), or null if not announcing. The name is renamed on
	 *         collision (e.g. "owlcms-2.local"), so this is not necessarily the requested name.
	 */
	public static String getRegisteredHostName() {
		return registeredHostName;
	}

	/**
	 * @return completes with the registered name, or with null when no name could be registered, the server stops, or
	 *         no name is being announced. Already complete when the search has ended.
	 */
	public static CompletableFuture<String> registration() {
		return registration;
	}

	/** Announces the name in the background; probing and collision resolution take a few seconds. */
	public static void start(String requestedName, int port) {
		if (hasRequestedHostName(requestedName)) {
			logger.info("not aliasing {}.local because it is already the server host name", requestedName);
			return;
		}
		CompletableFuture<String> result = new CompletableFuture<>();
		registration = result;
		// JmDNS default is 3600 s: a host that leaves without a goodbye stays cached on clients for up to an hour.
		// Read once when JmDNS is first used; a value given on the command line wins.
		if (System.getProperty("net.dns.ttl") == null) {
			System.setProperty("net.dns.ttl", "120");
		}
		Thread announcer = new Thread(() -> {
			try {
				announce(requestedName, port);
			} finally {
				result.complete(registeredHostName);
			}
		}, "mdns-responder");
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
			registration.complete(null);
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
	 * JmDNS does not always detect that another host already answers for the name, so the network is asked first and
	 * a name that another host answers for is skipped. The operating system resolver is not used to check the claimed
	 * name: it answers from a cache that can hold a host that has since left, and Windows may not resolve the name.
	 */
	private static void announceWithJmDNS(String requestedName, int port, IfaceAddress candidate)
	        throws IOException {
		InetAddress address = candidate.address();
		for (int attempt = 1; attempt <= MAX_NAME_COLLISIONS; attempt++) {
			String name = candidateName(requestedName, attempt);
			InetAddress owner = findOtherResponder(name + ".local", candidate);
			if (owner != null) {
				logger.info("{}.local is already answered by {}, trying another name", name, owner.getHostAddress());
				continue;
			}
			JmDNS jmdns = JmDNS.create(address, name);
			jmdns.registerService(ServiceInfo.create(SERVICE_TYPE, name, port, ""));
			String hostName = stripTrailingDot(jmdns.getHostName());
			synchronized (instances) {
				if (registration.isDone()) {
					// stop() was called while claiming the name
					closeInBackground(List.of(jmdns));
					return;
				}
				instances.add(jmdns);
				registeredHostName = hostName;
			}
			logger.info("announcing {} on {} ({})", hostName, address.getHostAddress(), candidate.iface().getName());
			return;
		}
		logger./**/warn("could not announce {}: no free name after {} attempts", requestedName, MAX_NAME_COLLISIONS);
	}

	/**
	 * Asks the network whether a host already answers for hostName. The query is sent from an ephemeral port, so
	 * responders answer this socket directly (RFC 6762 one-shot query) and the operating system resolver is not
	 * involved: Windows does not resolve a .local name reliably while this machine is announcing it.
	 *
	 * @return the address of the host that answered, or null if none did or the network could not be asked
	 */
	private static InetAddress findOtherResponder(String hostName, IfaceAddress candidate) {
		InetAddress address = candidate.address();
		boolean ipv4 = address instanceof Inet4Address;
		byte[] query = mdnsQuery(hostName, ipv4 ? TYPE_A : TYPE_AAAA);
		InetSocketAddress group = new InetSocketAddress(ipv4 ? "224.0.0.251" : "ff02::fb", MDNS_PORT);
		try (MulticastSocket socket = new MulticastSocket(new InetSocketAddress(address, 0))) {
			socket.setNetworkInterface(candidate.iface());
			socket.setTimeToLive(255);
			byte[] buffer = new byte[9000];
			for (int sent = 0; sent < PROBE_QUERIES; sent++) {
				socket.send(new DatagramPacket(query, query.length, group));
				long deadline = System.currentTimeMillis() + PROBE_TIMEOUT_MILLIS / PROBE_QUERIES;
				long remaining;
				while ((remaining = deadline - System.currentTimeMillis()) > 0) {
					socket.setSoTimeout((int) remaining);
					DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
					try {
						socket.receive(reply);
					} catch (SocketTimeoutException e) {
						break;
					}
					if (answers(reply, query)) {
						return reply.getAddress();
					}
				}
			}
		} catch (IOException e) {
			logger.info("could not ask the network whether {} is in use: {}", hostName, e.getMessage());
		}
		return null;
	}

	private static byte[] mdnsQuery(String hostName, int type) {
		ByteBuffer query = ByteBuffer.allocate(512);
		query.putShort((short) (1 + ThreadLocalRandom.current().nextInt(0xFFFF)));
		query.putShort((short) 0); // flags: standard query
		query.putShort((short) 1); // one question
		query.putShort((short) 0).putShort((short) 0).putShort((short) 0);
		for (String label : hostName.split("\\.")) {
			byte[] bytes = label.getBytes(StandardCharsets.UTF_8);
			query.put((byte) bytes.length).put(bytes);
		}
		query.put((byte) 0);
		query.putShort((short) type).putShort((short) CLASS_IN);
		return Arrays.copyOf(query.array(), query.position());
	}

	/**
	 * Only the owner of the name answers, so any successful response to our query with an answer means the name is
	 * taken; the answer may also be a record telling that the owner has no address of the requested type.
	 */
	private static boolean answers(DatagramPacket reply, byte[] query) {
		byte[] data = reply.getData();
		int offset = reply.getOffset();
		if (reply.getLength() < 12 || data[offset] != query[0] || data[offset + 1] != query[1]) {
			return false;
		}
		boolean isResponse = (data[offset + 2] & 0x80) != 0;
		boolean ok = (data[offset + 3] & 0x0F) == 0;
		int answerCount = ((data[offset + 6] & 0xFF) << 8) | (data[offset + 7] & 0xFF);
		return isResponse && ok && answerCount > 0;
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
