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
import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

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

/**
 * Main.
 */
public class Main {

    public final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);
    
    private static Integer serverPort;
    private static boolean demoMode;
    private static boolean memoryMode;
    private static boolean resetMode;
    private static boolean devMode;
    private static boolean testMode;
    private static boolean masters;

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String... args) throws Exception {

        try {
            init();
            new EmbeddedJetty().run(serverPort, "/"); //$NON-NLS-1$
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
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        
        // read command-line and environment variable parameters
        parseConfig();
        logStart();
        
        // open resource subdirectories as filesystems
        ResourceWalker.openTemplatesFileSystem("/templates");

        // translation
        System.setProperty("vaadin.i18n.provider", Translator.class.getName());

        // technical initializations
        System.setProperty("java.net.preferIPv4Stack", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        ConvertUtils.register(new DateConverter(null), java.util.Date.class);
        ConvertUtils.register(new DateConverter(null), java.sql.Date.class);

        // setup database
        JPAService.init(demoMode || memoryMode, demoMode || resetMode);
        injectData(demoMode, devMode, testMode, masters);

        // read application parameters from database
        overrideDisplayLanguage();
        OwlcmsFactory.getDefaultFOP();

        return;
    }

    private static void parseConfig() {
        // read server.port parameter from -D"server.port"=9999 on java command line
        // this is required for running on Heroku which assigns us the port at run time.
        // default is 8080
        serverPort = Integer.getInteger("port", 8080); //$NON-NLS-1$

        // reads system properties (-D on command line)
        demoMode = Boolean.getBoolean("demoMode"); // same as devMode + resetMode + memoryMode //$NON-NLS-1$
        memoryMode = Boolean.getBoolean("memoryMode"); // run in memory //$NON-NLS-1$
        resetMode = Boolean.getBoolean("resetMode"); // drop the schema first //$NON-NLS-1$
        devMode = Boolean.getBoolean("devMode"); // load large demo data if empty, do not reset //$NON-NLS-1$
                                                         // unless resetMode, persistent unless memoryMode also
        testMode = Boolean.getBoolean("testMode"); // load small dummy data if empty, do not reset //$NON-NLS-1$
                                                           // unless resetMode, persistent unless memoryMode
        masters = Boolean.getBoolean("masters"); //$NON-NLS-1$
    }


    private static void overrideDisplayLanguage() {
        // read override value from database
        Locale l = Competition.getCurrent().getDefaultLocale();
        
        // if LOCALE defined, ignore previous values and use LOCALE
        String localeEnvStr = System.getenv("LOCALE");
        if (localeEnvStr != null) l = Translator.createLocale(localeEnvStr);
        
        // Obey -Dlocale
        String localeProperty = System.getProperty("locale");
        if (localeProperty != null) l = Locale.getDefault();
        
        if (l != null) {
            Translator.setForcedLocale(l);
            logger.info("forcing display language to {}",l);
        }
    }

    private static void injectData(boolean demoMode, boolean devMode, boolean testMode, boolean masters) {
        if (demoMode) {
            // demoMode forces JPAService to reset.
            DemoData.insertInitialData(20, masters);
        } else {
            // the other modes require explicit resetMode. We don't want multiple inserts.
            List<Competition> allCompetitions = CompetitionRepository.findAll();
            if (allCompetitions.isEmpty()) {
                if (testMode) {
                    DemoData.insertInitialData(1, masters);
                } else if (devMode) {
                    DemoData.insertInitialData(20, masters);
                } else {
                    ProdData.insertInitialData(0);
                }
            } else {
                logger.info("database not empty: {}", allCompetitions.get(0).getCompetitionName()); //$NON-NLS-1$
            }
        }
    }

    protected static void logStart() throws IOException, ParseException {
        InputStream in = Main.class.getResourceAsStream("/build.properties"); //$NON-NLS-1$
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("version"); //$NON-NLS-1$
        OwlcmsFactory.setVersion(version);
        String buildTimestamp = props.getProperty("buildTimestamp"); //$NON-NLS-1$
        OwlcmsFactory.setBuildTimestamp(buildTimestamp);
        String buildZone = props.getProperty("buildZone"); //$NON-NLS-1$
        logger.info("owlcms {} built {} ({})", version, buildTimestamp, buildZone); //$NON-NLS-1$
    }

    protected static void tearDown() {
        JPAService.close();
    }

    public static void startBrowser() {
        if (Desktop.isDesktopSupported()) {
            logger.debug("starting browser");
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI("http://127.0.0.1"+(serverPort == 80 ? "" : ":"+serverPort.toString()+"/")));
            } catch (Exception e) {
                logger.error(LoggerUtils.stackTrace(e));
            }
        } else {
            logger.debug("no browser support");
        }
    }


}