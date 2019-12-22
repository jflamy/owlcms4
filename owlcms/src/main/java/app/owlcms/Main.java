/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.jpa.DemoData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.i18n.Translator;
import app.owlcms.init.EmbeddedJetty;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;
import sun.misc.Unsafe;

/**
 * Main.
 */
@SuppressWarnings("restriction")
public class Main {

    public final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

    private static Integer serverPort;
    private static boolean demoMode;
    private static boolean memoryMode;
    private static boolean resetMode;
    private static boolean devMode;
    private static boolean testMode;
    private static boolean masters;

    public static String productionMode;

    /**
     * return true if OWLCMS_KEY = true as an environment variable, and if not, if
     * -Dkey=true as a system property.
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
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        // disable poixml warning
        disableWarning();

        // read command-line and environment variable parameters
        parseConfig();
        logStart();

        // open resource subdirectories as filesystems
        ResourceWalker.openTemplatesFileSystem("/templates");

        // Vaadin configs
        System.setProperty("vaadin.i18n.provider", Translator.class.getName());

        // technical initializations
        System.setProperty("java.net.preferIPv4Stack", "true");
        ConvertUtils.register(new DateConverter(null), java.util.Date.class);
        ConvertUtils.register(new DateConverter(null), java.sql.Date.class);


        // setup database
        JPAService.init(demoMode || memoryMode, demoMode || resetMode);

        // read locale from database and overrrde if needed
        Locale l = overrideDisplayLanguage();
        injectData(demoMode, devMode, testMode, masters, l);

        OwlcmsFactory.getDefaultFOP();
        return;
    }

    private static void injectData(boolean demoMode, boolean devMode, boolean testMode, boolean masters,
            Locale locale) {
        Locale l = (locale == null ? Locale.ENGLISH : locale);
        EnumSet<AgeDivision> ageDivisions = masters ? EnumSet.of(AgeDivision.MASTERS, AgeDivision.U) : null;
        try {
            Translator.setForcedLocale(l);
            if (demoMode) {
                // demoMode forces JPAService to reset.
                DemoData.insertInitialData(20, ageDivisions);
            } else {
                // the other modes require explicit resetMode. We don't want multiple inserts.
                List<Competition> allCompetitions = CompetitionRepository.findAll();
                if (allCompetitions.isEmpty()) {
                    if (testMode) {
                        DemoData.insertInitialData(1, ageDivisions);
                    } else if (devMode) {
                        DemoData.insertInitialData(20, ageDivisions);
                    } else {
                        ProdData.insertInitialData(0);
                    }
                } else {
                    logger.info("database not empty: {}", allCompetitions.get(0).getCompetitionName());
                }
            }
        } finally {
            Translator.setForcedLocale(locale);
        }
    }

    protected static void logStart() throws IOException, ParseException {
        InputStream in = Main.class.getResourceAsStream("/build.properties");
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("version");
        OwlcmsFactory.setVersion(version);
        String buildTimestamp = props.getProperty("buildTimestamp");
        OwlcmsFactory.setBuildTimestamp(buildTimestamp);
        String buildZone = props.getProperty("buildZone");
        logger.info("owlcms {} built {} ({})", version, buildTimestamp, buildZone);
    }

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String... args) throws Exception {

        try {
            init();
            new EmbeddedJetty().run(serverPort, "/");
        } finally {
            tearDown();
        }
    }

    public static boolean openBrowser(Desktop desktop, String hostName)
            throws MalformedURLException, IOException, ProtocolException, URISyntaxException {
        if (hostName == null) {
            return false;
        }

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

    private static Locale overrideDisplayLanguage() {
        // read override value from database
        Locale l = null;
        try {
            l = Competition.getCurrent().getDefaultLocale();
        } catch (Exception e) {
        }

        // check OWLCMS_LOCALE, then -Dlocale, then LOCALE
        String localeEnvStr = getStringParam("locale");
        if (localeEnvStr != null) {
            l = Translator.createLocale(localeEnvStr);
        } else {
            localeEnvStr = System.getenv("LOCALE");
            if (localeEnvStr != null) {
                l = Translator.createLocale(localeEnvStr);
            }
        }

        if (l != null) {
            Translator.setForcedLocale(l);
            logger.info("forcing display language to {}", l);
        }
        return l;
    }

    /**
     * get configuration from environment variables and if not found, from system
     * properties.
     */
    private static void parseConfig() {
        // read server.port parameter from -D"server.port"=9999 on java command line
        // this is required for running on Heroku which assigns us the port at run time.
        // default is 8080
        serverPort = getIntegerParam("port", 8080);

        // same as devMode + resetMode + memoryMode
        demoMode = getBooleanParam("demoMode");

        // run in memory
        memoryMode = getBooleanParam("memoryMode");

        // drop the schema first
        resetMode = getBooleanParam("resetMode");

        // load large demo data if empty
        devMode = getBooleanParam("devMode");

        // load small dummy data if empty
        testMode = getBooleanParam("testMode");

        // productionMode required to tell vaadin to skip npm
        boolean npmMode = getBooleanParam("npmMode");
        productionMode = npmMode ? "false" : "true";

        masters = getBooleanParam("masters");
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
                logger.error(LoggerUtils.stackTrace(e));
            }
        } else {
            logger.debug("no browser support");
        }
    }

    protected static void tearDown() {
        JPAService.close();
    }
    
    public static void disableWarning() {
        // https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class<?> cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

}