/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinServletRequest;

import ch.qos.logback.classic.Logger;

public class IPInterfaceUtils {

    // a fully qualified domain name
    // reference: https://regex101.com/r/FLA9Bv/40
    private static final String FQDN_REGEX = "^(?!.*?_.*?)(?!(?:[\\w]+?\\.)?\\-[\\w\\.\\-]*?)(?![\\w]+?\\-\\.(?:[\\w\\.\\-]+?))(?=[\\w])(?=[\\w\\.\\-]*?\\.+[\\w\\.\\-]*?)(?![\\w\\.\\-]{254})(?!(?:\\.?[\\w\\-\\.]*?[\\w\\-]{64,}\\.)+?)[\\w\\.\\-]+?(?<![\\w\\-\\.]*?\\.[\\d]+?)(?<=[\\w\\-]{2,})(?<![\\w\\-]{25})$";

    private static String urlPrefix = "/local/";

    private static String targetFile = "sounds/timeOver.mp3";
    final private Logger logger = (Logger) LoggerFactory.getLogger(IPInterfaceUtils.class);
    ArrayList<String> wired = new ArrayList<>();
    ArrayList<String> recommended = new ArrayList<>();

    ArrayList<String> wireless = new ArrayList<>();
    ArrayList<String> loopback = new ArrayList<>();

    /**
     * Try to guess URLs that can reach the system.
     *
     * The browser on the master laptop most likely uses "localhost" in its URL. We can't know which of its available IP
     * addresses can actually reach the application. We scan the network addresses, and try the URLs one by one, listing
     * wired interfaces first, and wireless interfaces second (in as much as we can guess).
     *
     * We rely on the URL used to reach the "about" screen to know how the application is named, what port is used, and
     * which protocol works.
     *
     * @return HTML ("a" tags) for the various URLs that appear to work.
     */
    public IPInterfaceUtils() {
    }

    public void checkInterfaces(String protocol, int requestPort, boolean silent)
            throws SocketException {
        String ip;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            // filters out 127.0.0.1 and inactive interfaces
            if (// iface.isLoopback() ||
            !iface.isUp()) {
                continue;
            }

            String displayName = iface.getDisplayName();
            String ifaceDisplay = displayName.toLowerCase();
            String ifaceName = iface.getName().toLowerCase();

            // filter out interfaces to virtual machines
            if (!virtual(ifaceDisplay)) {
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    if (addr instanceof Inet4Address) {
                        logger.trace("address:{} loopback:{} local:{} ipv4:{} interface: {} ({})", ip,
                                addr.isLoopbackAddress(), addr.isSiteLocalAddress(),
                                addr.getHostAddress(), ifaceName, ifaceDisplay);
                        // try reaching the current IP address with the known protocol, port and site.
                        testIP(protocol, requestPort, "", urlPrefix + targetFile, ip, iface, addr, silent);
                    }
                }
            }
        }
    }
    
    public List<String> getLocalAdresses() {
        String ip;
        ArrayList<String> localAdresses = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (// iface.isLoopback() ||
                !iface.isUp()) {
                    continue;
                }

                String displayName = iface.getDisplayName();
                String ifaceDisplay = displayName.toLowerCase();

                // filter out interfaces to virtual machines
                if (!virtual(ifaceDisplay)) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        ip = addr.getHostAddress();
                        if (addr instanceof Inet4Address) {
                            localAdresses.add(ip);
                        }
                    }
                }
            }
        } catch (SocketException e) {
        }
        return localAdresses;
    }

    public void checkRequest() {
        try {

            HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
            Map<String, String> headerMap = getRequestHeadersInMap(request);

            checkTargetFileOk(targetFile);

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
            try {
                InetAddress serverAddr = Inet4Address.getByName(server);
                loopbackAddress = serverAddr.isLoopbackAddress();
                siteLocalAddress = serverAddr.isSiteLocalAddress();
                local = loopbackAddress || siteLocalAddress;
                logger.trace("request {} loopback:{} sitelocal: {}", requestURL, loopbackAddress, siteLocalAddress);
            } catch (UnknownHostException e) {
                // reverse name lookup not configured (e.g. when running on gitpod.io)
                local = false;
            }

            if (!local || server.matches(FQDN_REGEX)) {
                // an external name or address outside the local machine or local site (non-routable network).
                // or a
                if (absoluteURL.endsWith("/")) {
                    absoluteURL = requestURL.substring(0, requestURL.length() - 1);
                }
                recommended.add(absoluteURL);
                // if we are not on the cloud, we try to get a numerical address anyway.
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

    /**
     * @return the loopback
     */
    public ArrayList<String> getLocalUrl() {
        return loopback;
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

    private void checkTargetFileOk(String targetFile) {
        try {
            ResourceWalker.getResourceAsStream("/" + targetFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("test resource not found " + targetFile);
        }
    }

    private void testIP(String protocol, int requestPort, String uri, String targetFile, String ip,
            NetworkInterface iface, InetAddress addr, boolean silent) {
        try {
            URL siteURL = new URL(protocol, ip, requestPort, uri);
            String siteURLString = siteURL.toExternalForm();
            siteURLString = URLUtils.cleanURL(siteURL, siteURLString);

            // use a file inside the site to avoid triggering a loop if called on home page
            URL testingURL = new URL(protocol, ip, requestPort, uri + targetFile);
            String testingURLString = testingURL.toExternalForm();

            HttpURLConnection huc = (HttpURLConnection) testingURL.openConnection();
            huc.setRequestMethod("GET");
            huc.connect();
            int response = huc.getResponseCode();

            String ifaceName = iface.getName();
            String ifaceDisplay = iface.getDisplayName();
            if (response != 200) {
                logger./**/warn("{} not reachable: {} {} ({})", testingURLString, response, ifaceName, ifaceDisplay);
            } else {
                if (!silent) {
                    logger.info("networking check: {} OK {} ({}) {}", ip + ":" + requestPort, ifaceName, ifaceDisplay,
                            testingURL);
                }
                if (ifaceName.startsWith("lo") || ifaceDisplay.contains("loopback")) {
                    loopback.add(siteURLString);
                } else if (ifaceName.startsWith("wlan") || ifaceDisplay.contains("wireless")) {
                    wireless.add(siteURLString);
                } else if (ifaceName.startsWith("eth")) {
                    wired.add(siteURLString);
                } else {
                    logger./**/warn("inferface type not recognized: {} {}", ifaceName, ifaceDisplay);
                }
            }
        } catch (Exception e) {
            LoggerUtils.logError(logger, e);
        }
    }

    private boolean virtual(String displayName) {
        return displayName.contains("virtual");
    }

}
