/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.relay;

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
import org.slf4j.bridge.SLF4JBridgeHandler;

import app.owlcms.init.EmbeddedJetty;
import ch.qos.logback.classic.Logger;
import javassist.Translator;

/**
 * Main.
 */
public class Main {

    public final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

    private static Integer serverPort;

    public static String productionMode;

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String... args) throws Exception {

        try {
        	System.err.println("start");
            init();
            new EmbeddedJetty().run(serverPort, "/");
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            tearDown();
        }
    }

    /**
     * Prepare owlcms
     * 
     * Reads configuration options, injects data, initializes singletons and
     * configurations. The embedded web server can then be started.
     * 
     * Sample command line to run on port 80 and in demo mode (automatically
     * generated fake data, in-memory database)
     * 
     * <code><pre>java -D"server.port"=80 -DdemoMode=true -jar owlcms-4.0.1-SNAPSHOT.jar app.owlcms.Main</pre></code>
     * 
     * @return the server port on which we want to run
     * @throws IOException
     * @throws ParseException
     */
    protected static void init() throws IOException, ParseException {
        // Configure logging -- must take place before anything else
        // Redirect java.util.logging logs to SLF4J
    	System.err.println("start init");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // read command-line and environment variable parameters
        parseConfig();
        logStart();

        // Vaadin configs
        System.setProperty("vaadin.i18n.provider", Translator.class.getName());
        
        // technical initializations
        System.setProperty("java.net.preferIPv4Stack", "true");
    	System.err.println("end init "+serverPort);
        return;
    }

    /**
     * get configuration from environment variables and if not found, from system properties.
     */
    private static void parseConfig() {
        // read server.port parameter from -D"server.port"=9999 on java command line
        // this is required for running on Heroku which assigns us the port at run time.
        // default is 8080
        serverPort = getIntegerParam("port", 8080);
    }

    /**
     * return true if OWLCMS_KEY = true as an environment variable,
     * and if not, if -Dkey=true as a system property.
     * 
     * Environment variables are upperCased, system properties are case-sensitive.
     * <ul>
     * <li>OWMCMS_PORT=80 is the same as -Dport=80
     * </ul>
     * @param key
     * @return true if value is found and exactly "true"
     */
    public static boolean getBooleanParam(String key) {
        String envVar = "OWLCMS_"+key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return val.equals("true");
        } else {
            return Boolean.getBoolean(key);
        }
    }
    
    public static String getStringParam(String key) {
        String envVar = "OWLCMS_"+key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return val;
        } else {
            return System.getProperty(key);
        }
    }

    public static Integer getIntegerParam(String key, Integer defaultValue) {
        String envVar = "OWLCMS_"+key.toUpperCase();
        String val = System.getenv(envVar);
        if (val != null) {
            return Integer.parseInt(val);
        } else {
            return Integer.getInteger(key,defaultValue);
        }
    }

    protected static void logStart() throws IOException, ParseException {
        InputStream in = Main.class.getResourceAsStream("/build.properties");
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("version");
        String buildTimestamp = props.getProperty("buildTimestamp");
        String buildZone = props.getProperty("buildZone");
        logger.warn("relay {} built {} ({})", version, buildTimestamp, buildZone);
    }

    protected static void tearDown() {
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
                e.printStackTrace();
            }
        } else {
            logger.debug("no browser support");
        }
    }

    public static boolean openBrowser(Desktop desktop, String hostName)
            throws MalformedURLException, IOException, ProtocolException, URISyntaxException {
        if (hostName == null) return false;
        
        int response;
        URL testingURL = new URL("http", hostName, serverPort, "/sounds/timeOver.mp3");
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

}