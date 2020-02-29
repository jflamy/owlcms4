/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms;

import java.io.IOException;
import java.text.ParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.jpa.DemoData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.i18n.Translator;
import app.owlcms.init.EmbeddedJetty;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Main class for launching owlcms through an embedded jetty server.
 * 
 * @author Jean-François Lamy
 */
public class Main {

    public final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

    protected static Integer serverPort;
    protected static boolean demoMode;
    protected static boolean memoryMode;
    protected static boolean resetMode;
    protected static boolean devMode;
    protected static boolean smallMode;
    protected static boolean masters;
    protected static String productionMode;

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

    /**
     * Prepare owlcms
     *
     * Reads configuration options, injects data, initializes singletons and configurations. The embedded web server can
     * then be started.
     *
     * Sample command line to run on port 80 and in demo mode (automatically generated fake data, in-memory database)
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
        StartupUtils.disableWarning();

        // read command-line and environment variable parameters
        parseConfig();
        StartupUtils.setServerPort(serverPort);
        StartupUtils.logStart("owlcms", serverPort);

        // open jar as filesystem; cannot use /; any resource inside the jar will do
        // cannot open the same jar twice.
        ResourceWalker.openFileSystem("/templates");

        // Vaadin configs
        System.setProperty("vaadin.i18n.provider", Translator.class.getName());

        // technical initializations
        System.setProperty("java.net.preferIPv4Stack", "true");
        ConvertUtils.register(new DateConverter(null), java.util.Date.class);
        ConvertUtils.register(new DateConverter(null), java.sql.Date.class);

        // setup database
        boolean inMemory = demoMode || memoryMode;
        boolean reset = demoMode || resetMode;
        JPAService.init(inMemory, reset);

        // read locale from database and overrrde if needed
        Locale l = overrideDisplayLanguage();
        injectData(demoMode, devMode, smallMode, masters, l);

        OwlcmsFactory.getDefaultFOP();
        return;
    }

    protected static void tearDown() {
        JPAService.close();
    }

    private static void injectData(boolean demoMode, boolean devMode, boolean smallMode, boolean masters,
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
                    if (smallMode) {
                        DemoData.insertInitialData(1, ageDivisions);
                    } else if (devMode) {
                        DemoData.insertInitialData(20, ageDivisions);
                    } else {
                        ProdData.insertInitialData(0);
                    }
                } else {
                    logger.info("database not empty: {}", allCompetitions.get(0).getCompetitionName());
                    List<AgeGroup> ags = AgeGroupRepository.findAll();
                    if (ags.isEmpty()) {
                        logger.info("updating age groups and categories");
                        JPAService.runInTransaction(em -> {
                            AgeGroupRepository.insertAgeGroups(em, null);
                            return null;
                        });
                    }
                }
            }
        } finally {
            Translator.setForcedLocale(locale);
        }
    }

    private static Locale overrideDisplayLanguage() {
        // read override value from database
        Locale l = null;
        try {
            l = Competition.getCurrent().getDefaultLocale();
        } catch (Exception e) {
        }

        // check OWLCMS_LOCALE, then -Dlocale, then LOCALE
        String localeEnvStr = StartupUtils.getStringParam("locale");
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
     * get configuration from environment variables and if not found, from system properties.
     */
    private static void parseConfig() {
        // read server.port parameter from -D"server.port"=9999 on java command line
        // this is required for running on Heroku which assigns us the port at run time.
        // default is 8080
        serverPort = StartupUtils.getIntegerParam("port", 8080);
        StartupUtils.setServerPort(serverPort);

        // same as devMode + resetMode + memoryMode
        demoMode = StartupUtils.getBooleanParam("demoMode");

        // run in memory
        memoryMode = StartupUtils.getBooleanParam("memoryMode");

        // drop the schema first
        resetMode = StartupUtils.getBooleanParam("resetMode");

        // load large demo data if empty
        devMode = StartupUtils.getBooleanParam("devMode");

        // load small dummy data if empty
        smallMode = StartupUtils.getBooleanParam("smallMode");

        // productionMode required to tell vaadin to skip npm
        boolean npmMode = StartupUtils.getBooleanParam("npmMode");
        productionMode = npmMode ? "false" : "true";

        masters = StartupUtils.getBooleanParam("masters");
    }

}