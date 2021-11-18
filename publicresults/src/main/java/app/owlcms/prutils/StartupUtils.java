/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
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
import java.text.ParseException;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class StartupUtils {

    static Logger logger = (Logger) LoggerFactory.getLogger(StartupUtils.class);
    static Logger mainLogger = (Logger) LoggerFactory.getLogger("app.owlcms.Main");

    static Integer serverPort = null;

    public static void disableWarning() {
//        // https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
//        try {
//            Field theUnsafe = UnsafeAPI.class.getDeclaredField("theUnsafe");
//            theUnsafe.setAccessible(true);
//            u = theUnsafe.get(null);
//
//            Class<?> cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
//            Field logger = cls.getDeclaredField("logger");
//            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
//        } catch (Exception e) {
//            // ignore
//        }
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

    public static Integer getIntegerParam(String key, Integer defaultValue) {
        String envVar = "OWLCMS_" + key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return Integer.parseInt(val);
        } else {
            return Integer.getInteger(key, defaultValue);
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

    public static void logStart(String appName, Integer serverPort) throws IOException, ParseException {
        InputStream in = StartupUtils.class.getResourceAsStream("/build.properties");
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("version");
//        OwlcmsFactory.setVersion(version);
        String buildTimestamp = props.getProperty("buildTimestamp");
//        OwlcmsFactory.setBuildTimestamp(buildTimestamp);
        String buildZone = props.getProperty("buildZone");
        mainLogger.info("{} {} built {} ({})", appName, version, buildTimestamp, buildZone);
    }

    public static boolean openBrowser(Desktop desktop, String hostName)
            throws MalformedURLException, IOException, ProtocolException, URISyntaxException {
        if (hostName == null) {
            return false;
        }

        int response;
        URL testingURL = new URL("http", hostName, serverPort, "/frontend/images/owlcms.ico");
        HttpURLConnection huc = (HttpURLConnection) testingURL.openConnection();
        logger.debug("checking for {}", testingURL.toExternalForm());
        huc.setRequestMethod("GET");
        huc.connect();
        int response1 = huc.getResponseCode();
        response = response1;
        if (response == 200) {
            URL appURL = new URL("http", hostName, serverPort, "");
            desktop.browse(appURL.toURI());
            return true;
        }
        return false;
    }

    public static void setServerPort(Integer serverPort) {
        StartupUtils.serverPort = serverPort;
    }

    public static void startBrowser() {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                InetAddress localMachine = InetAddress.getLocalHost();
                String hostName = localMachine.getHostName();

                boolean opened = openBrowser(desktop, hostName);
                if (!opened) {
                    openBrowser(desktop, "127.0.0.1");
                }
            } catch (Exception e) {
                mainLogger.error(LoggerUtils.stackTrace(e));
            }
        } else {
            logger./**/warn("no browser support");
        }
    }
    
    public static boolean isDebugSetting() {
        String param = StartupUtils.getStringParam("DEBUG");
        return "true".equalsIgnoreCase(param) || "debug".equalsIgnoreCase(param) || "trace".equalsIgnoreCase(param);
    }
    
    public static boolean isTraceSetting() {
        String param = StartupUtils.getStringParam("DEBUG");
        return "trace".equalsIgnoreCase(param);
    }

}
