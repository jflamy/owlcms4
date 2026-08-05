/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinServletRequest;

import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.HttpServletRequest;

public class IPInterfaceUtils {

	/** An IPv4 address together with the interface it was found on. */
	public record IfaceAddress(NetworkInterface iface, InetAddress address) {
	}

	/**
	 * Active, non-virtual interfaces and their IPv4 addresses. Loopback is included; callers that do not want it must
	 * filter it out themselves.
	 */
	public static List<IfaceAddress> getCandidateAddresses() throws SocketException {
		List<IfaceAddress> candidates = new ArrayList<>();
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface iface = interfaces.nextElement();
			if (!iface.isUp()) {
				continue;
			}
			String displayName = iface.getDisplayName();
			if (displayName != null && displayName.toLowerCase().contains("virtual")) {
				continue;
			}
			Enumeration<InetAddress> addresses = iface.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress addr = addresses.nextElement();
				if (addr instanceof Inet4Address) {
					candidates.add(new IfaceAddress(iface, addr));
				}
			}
		}
		return candidates;
	}

	// a fully qualified domain name
	// reference: https://regex101.com/r/FLA9Bv/40
	private static final String FQDN_REGEX = "^(?!.*?_.*?)(?!(?:[\\w]+?\\.)?\\-[\\w\\.\\-]*?)(?![\\w]+?\\-\\.(?:[\\w\\.\\-]+?))(?=[\\w])(?=[\\w\\.\\-]*?\\.+[\\w\\.\\-]*?)(?![\\w\\.\\-]{254})(?!(?:\\.?[\\w\\-\\.]*?[\\w\\-]{64,}\\.)+?)[\\w\\.\\-]+?(?<![\\w\\-\\.]*?\\.[\\d]+?)(?<=[\\w\\-]{2,})(?<![\\w\\-]{25})$";
	private static final String NUMERIC_HOST_REGEX = "^(https?:\\/\\/)?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(\\/[^\s]*)?$";
	final private Logger logger = (Logger) LoggerFactory.getLogger(IPInterfaceUtils.class);
	ArrayList<String> wired = new ArrayList<>();
	ArrayList<String> recommended = new ArrayList<>();
	ArrayList<String> wireless = new ArrayList<>();
	ArrayList<String> loopback = new ArrayList<>();
	ArrayList<String> networking = new ArrayList<>();

	/**
	 * Guess URLs that can reach the system.
	 *
	 * The browser on the master laptop most likely uses "localhost" in its URL. We can't know which of its available IP
	 * addresses can actually reach the application from other devices, so we list them all, wired interfaces first and
	 * wireless second (in as much as we can guess).
	 *
	 * We rely on the URL used to reach the "about" screen to know how the application is named, what port is used, and
	 * which protocol works.
	 */
	public IPInterfaceUtils() {
	}

	public void checkInterfaces(String protocol, int requestPort, boolean silent)
	        throws SocketException {
		for (IfaceAddress candidate : getCandidateAddresses()) {
			classify(protocol, requestPort, candidate.iface(), candidate.address(), silent);
		}
	}

	public void checkRequest() {
		try {

			HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
			Map<String, String> headerMap = getRequestHeadersInMap(request);

			String protocol = URLUtils.getScheme(request);
			int requestPort = URLUtils.getServerPort(request);
			String server = URLUtils.getServerName(request);
			String requestURL = request.getRequestURL().toString();
			String absoluteURL = URLUtils.buildAbsoluteURL(request, null);
			logger.trace("absolute URL {}", absoluteURL);

			boolean local = false;
			// local = isLocalAddress(server) || isLoopbackAddress(server);
			boolean loopbackAddress;
			boolean siteLocalAddress;
			if (server.endsWith(".local")) {
				// mDNS name: resolving it does a live multicast round-trip that can block for seconds
				local = true;
			} else {
				try {
					InetAddress serverAddr = InetAddress.getByName(server);
					loopbackAddress = serverAddr.isLoopbackAddress();
					siteLocalAddress = serverAddr.isSiteLocalAddress();
					local = loopbackAddress || siteLocalAddress;
					logger.trace("request {} loopback:{} sitelocal: {}", requestURL, loopbackAddress,
					        siteLocalAddress);
				} catch (UnknownHostException e) {
					// reverse name lookup not configured (e.g. when running in cloud environments)
					local = false;
				}
			}

			boolean numerical = server.matches(NUMERIC_HOST_REGEX);
			if (numerical && !local) {
				recommended.add(absoluteURL);
			} else if (!numerical && server.matches(FQDN_REGEX)) {
				// an external name or address outside the local machine or local site
				if (absoluteURL.endsWith("/")) {
					absoluteURL = requestURL.substring(0, requestURL.length() - 1);
				}
				recommended.add(absoluteURL);

				String forward = headerMap.get("x-forwarded-for");
				if (forward != null) {
					logger.trace("forwarding for {}, proxied, ip address would be meaningless", forward);
					return;
				} else {
					logger.trace("no x-forwarded-for, local machine with host name");
				}
			}

			checkInterfaces(protocol, requestPort, true);
		} catch (SocketException e) {
			LoggerUtils.logError(logger, e);
		}
		logger.trace("wired = {} {}", wired, wired.size());
		logger.trace("wireless = {} {}", wireless, wireless.size());
	}

	public List<String> getLocalAdresses() {
		ArrayList<String> localAdresses = new ArrayList<>();
		try {
			for (IfaceAddress candidate : getCandidateAddresses()) {
				localAdresses.add(candidate.address().getHostAddress());
			}
		} catch (SocketException e) {
		}
		return localAdresses;
	}

	/**
	 * @return the loopback
	 */
	public ArrayList<String> getLocalUrl() {
		return loopback;
	}

	public ArrayList<String> getNetworking() {
		return networking;
	}

	/**
	 * @return the external (non-local) url used to get to the site.
	 */
	public ArrayList<String> getRecommended() {
		return recommended;
	}

	public Map<String, String> getRequestHeadersInMap(HttpServletRequest request) {
		Map<String, String> result = new HashMap<>();
		String remoteAddr = request.getRemoteAddr();
		logger.trace("remoteAddr: {}", remoteAddr);
		result.put("remoteAddr", remoteAddr);
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = headerNames.nextElement().toLowerCase();
			if (key.equals("x-forwarded-for") || key.equals("host")) {
				String value = request.getHeader(key);
				result.put(key, value);
				logger.trace(key + ": " + value);
			}
		}
		return result;
	}

	/**
	 * @return the wired urls
	 */
	public ArrayList<String> getWired() {
		return wired;
	}

	/**
	 * @return the wireless urls
	 */
	public ArrayList<String> getWireless() {
		return wireless;
	}

	public void setNetworking(ArrayList<String> networking) {
		this.networking = networking;
	}

	/** Builds the URL for the address and files it by interface kind; no connection is attempted. */
	private void classify(String protocol, int requestPort, NetworkInterface iface, InetAddress addr,
	        boolean silent) {
		try {
			String ip = addr.getHostAddress();
			URL siteURL = URI.create(protocol + "://" + ip + ":" + requestPort).toURL();
			String siteURLString = URLUtils.cleanURL(siteURL, siteURL.toExternalForm());

			String ifaceName = iface.getName();
			String ifaceDisplay = iface.getDisplayName();
			if (!silent) {
				logger.debug("networking: {} {} ({})", siteURLString, ifaceName, ifaceDisplay);
			}
			if (addr.isLoopbackAddress()) {
				loopback.add(siteURLString);
			} else if (isWirelessInterface(ifaceName, ifaceDisplay)) {
				wireless.add(siteURLString);
			} else if (isWiredInterface(ifaceName)) {
				wired.add(siteURLString);
			} else {
				// on certain versions of macOS, wireless and wired interfaces are both "en"
				networking.add(siteURLString);
			}
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
	}

	private boolean isMacOs() {
		String osName = System.getProperty("os.name", "").toLowerCase();
		return osName.startsWith("mac");
	}

	private boolean isWirelessInterface(String ifaceName, String ifaceDisplay) {
		String normalizedName = ifaceName == null ? "" : ifaceName.toLowerCase();
		String normalizedDisplay = ifaceDisplay == null ? "" : ifaceDisplay.toLowerCase();

		if (normalizedName.startsWith("wlan") || normalizedName.startsWith("wlp")
		        || normalizedDisplay.contains("wireless")) {
			return true;
		}

		return isMacOs() && normalizedName.equals("en0");
	}

	private boolean isWiredInterface(String ifaceName) {
		String normalizedName = ifaceName == null ? "" : ifaceName.toLowerCase();

		if (normalizedName.startsWith("eth") || normalizedName.startsWith("enp")) {
			return true;
		}

		return isMacOs() && normalizedName.startsWith("en") && !normalizedName.equals("en0");
	}

}
