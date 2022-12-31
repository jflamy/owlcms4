/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class StartupUtils {

    static Logger logger = (Logger) LoggerFactory.getLogger(StartupUtils.class);
    static Logger mainLogger = (Logger) LoggerFactory.getLogger("app.owlcms.Main");

    static Integer serverPort = null;
    private static String buildTimestamp;
    private static String version;

    public static void disableWarning() {
    }

    /**
     * return true if OWLCMS_KEY = true as an environment variable, and if not, if -Dkey=true as a system property.
     *
     * Environment variables are upperCased, system properties are case-sensitive.
     * <ul>
     * <li>OWMCMS_PORT=80 is the same as -Dport=80
     * </ul>
     *
     * @param key
     * @return true if value is found and exactly "true"
     */
    public static boolean getBooleanParam(String key) {
        String envVar = "OWLCMS_" + key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return val.equals("true");
        } else {
            return Boolean.getBoolean(key);
        }
    }

    /**
     * @return the buildTimestamp
     */
    public static String getBuildTimestamp() {
        return buildTimestamp;
    }

    public static Integer getIntegerParam(String key, Integer defaultValue) {
        String envVar = "OWLCMS_" + key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return Integer.parseInt(val);
        } else {
            return Integer.getInteger(key, defaultValue);
        }
    }

    public static Logger getMainLogger() {
        return mainLogger;
    }

    public static String getRawStringParam(String key) {
        String envVar = key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return val;
        } else {
            return System.getProperty(key);
        }
    }

    public static Integer getServerPort() {
        return serverPort;
    }

    public static String getStringParam(String key) {
        String envVar = "OWLCMS_" + key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return val;
        } else {
            return System.getProperty(key);
        }
    }

    /**
     * @return the version
     */
    public static String getVersion() {
        return version;
    }

    public static boolean isDebugSetting() {
        String param = StartupUtils.getStringParam("DEBUG");
        return "true".equalsIgnoreCase(param) || "debug".equalsIgnoreCase(param) || "trace".equalsIgnoreCase(param);
    }

    public static boolean isTraceSetting() {
        String param = StartupUtils.getStringParam("DEBUG");
        return "trace".equalsIgnoreCase(param);
    }

    public static void logStart(String appName, Integer serverPort) throws IOException, ParseException {
        InputStream in = StartupUtils.class.getResourceAsStream("/build.properties");
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("version");
        setVersion(version);
        setBuildTimestamp(props.getProperty("buildTimestamp"));
        String buildZone = props.getProperty("buildZone");
        mainLogger.info("{} {} built {} ({})", appName, version, getBuildTimestamp(), buildZone);
    }

    public static boolean openBrowser(Desktop desktop, String hostName)
            throws MalformedURLException, IOException, ProtocolException, URISyntaxException,
            UnsupportedOperationException {
        if (hostName == null) {
            return false;
        }

        int response;

        URL testingURL = new URL("http", hostName, serverPort, "/local/sounds/timeOver.mp3");
        HttpURLConnection huc = (HttpURLConnection) testingURL.openConnection();
        logger.debug("checking for {}", testingURL.toExternalForm());
        huc.setRequestMethod("GET");
        huc.connect();
        int response1 = huc.getResponseCode();
        response = response1;
        if (response == 200) {
            URL appURL = new URL("http", hostName, serverPort, "");
            String os = System.getProperty("os.name").toLowerCase();
            if (desktop != null) {
                desktop.browse(appURL.toURI());
            } else if (os.contains("win")) {
                Runtime rt = Runtime.getRuntime();
                rt.exec("rundll32 url.dll,FileProtocolHandler " + appURL.toURI());
            } else {
                return false;
            }
            return true;
        } else {
            logger.error("cannot open expected URL {}", testingURL.toExternalForm());
            return false;
        }
    }

    /**
     * @param buildTimestamp the buildTimestamp to set
     */
    public static void setBuildTimestamp(String buildTimestamp) {
        StartupUtils.buildTimestamp = buildTimestamp;
    }

    public static void setMainLogger(Logger mainLogger) {
        StartupUtils.mainLogger = mainLogger;
    }

    public static void setServerPort(Integer serverPort) {
        StartupUtils.serverPort = serverPort;
    }

    public static void startBrowser() {
        try {
            if (getBooleanParam("publicDemo")) {
                logger./**/warn("public demo, not starting browser");
                return;
            }
            String hostName = fixBrowserHostname(); 
            Desktop desktop = null;
            if (Desktop.isDesktopSupported()) {
                desktop = Desktop.getDesktop();
            }
            // if no desktop, will attempt Windows-specific technique
            boolean ok = openBrowser(desktop, hostName);
            if (!ok) {
                logger./**/warn("Cannot start browser on {}", System.getProperty("os.name"));
            }
        } catch (Throwable t) {
            logger./**/warn("Cannot start browser: {}", t.getCause() != null ? t.getCause() : t.getMessage());
        }
    }

    /**
     * In development mode when the browser is opened on the local machine, it appears that
     * the Vite server cannot be reached unless the address is given as "localhost".
     * (Observed on Windows 11 with firewalls disabled, with Vaadin 23.3.2).
     * 
     * If the IP address returned for the current machine name is one of the current machine's interfaces, we
     * force "localhost" as the name to be used in the browser.
     * 
     * @return "localhost" if the address is local to the dev machine
     * @throws UnknownHostException
     */
    private static String fixBrowserHostname() throws UnknownHostException {
        InetAddress localMachine = InetAddress.getLocalHost();
        String ipAddress = localMachine.getHostAddress();
        String hostName;
        List<String> localAdresses = new IPInterfaceUtils().getLocalAdresses();
        //logger.debug("addresses {} {}", ipAddress, localAdresses);
        if (localAdresses.contains(ipAddress)) {
            hostName = "localhost";
        } else {
            hostName = localMachine.getHostName();
        }
        return hostName;
    }

    private static void setVersion(String v) {
        version = v;
    }

}
