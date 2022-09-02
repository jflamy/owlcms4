/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.data.jpa.DemoData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.data.records.RecordDefinitionReader;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.InitialData;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.servlet.EmbeddedJetty;
import app.owlcms.uievents.AppEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Main class for launching owlcms through an embedded jetty server.
 *
 * @author Jean-François Lamy
 */
public class Main {

    private static final int WARNING_MINUTES = 5;

    public final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

    protected static boolean demoData;
    protected static boolean demoMode;
    protected static boolean masters;
    protected static boolean memoryMode;
    protected static String productionMode;
    protected static boolean resetMode;
    protected static Integer serverPort;
    protected static boolean smallData;

    private static InitialData initialData;

    public static Logger getStartupLogger() {
        String name = Main.class.getName() + ".startup";
        return (Logger) LoggerFactory.getLogger(name);
    }

    public static void initConfig() {
        // there is no config read so far.
        boolean publicDemo = StartupUtils.getBooleanParam("publicDemo");
        if (publicDemo) {
            JPAService.init(true, true);
        } else {
            // setup database
            JPAService.init(memoryMode, resetMode);
        }
        // check for database override of resource files
        Config.initConfig();
    }

    /**
     * This method is actually called from EmbeddedJetty immediately after starting the server
     */
    public static void initData() {
        // Vaadin configs
        System.setProperty("vaadin.i18n.provider", Translator.class.getName());

        initConfig();

        // read locale from database and overrrde if needed
        Locale l = overrideDisplayLanguage();
        injectData(initialData, l);
        overrideTimeZone();

        // initialization, don't push out to browsers
        OwlcmsFactory.initDefaultFOP();
    }

    public static void injectSuppliers() {
        // app config injection
        Translator.setLocaleSupplier(() -> OwlcmsSession.getLocale());
        ResourceWalker.setLocaleSupplier(Translator.getLocaleSupplier());
        ResourceWalker.setLocalZipBlobSupplier(() -> Config.getCurrent().getLocalZipBlob());
    }

    /**
     * The main method.
     *
     * Start a web server and do all the required initializations for the application If running normally, we run until
     * killed. If running as a public demo, we sleep for awhile, and then exit. Some external mechanism such as
     * Kubernetes will notice and restart another instance.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String... args) throws Exception {
        // there is no config read so far.
        Integer demoResetDelay = StartupUtils.getIntegerParam("publicDemo", null);
        logger.info("Public demo server, will reset after {} seconds", demoResetDelay);

        init();
        CountDownLatch latch = OwlcmsFactory.getInitializationLatch();

        // restart automatically forever if running as public demo
        while (true) {
            EmbeddedJetty embeddedJetty = new EmbeddedJetty(latch)
                    .setStartLogger(logger)
                    .setInitConfig(Main::initConfig)
                    .setInitData(Main::initData);
            Thread server = new Thread(() -> {
                try {
                    embeddedJetty.run(serverPort, "/");
                } catch (Exception e) {
                    logger.error("cannot start server {}\\n{}", e, LoggerUtils.stackTrace(e));
                }

            });
            server.start();
            if (demoResetDelay == null) {
                break;
            } else {
                warnAndExit(demoResetDelay, embeddedJetty);
            }
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

        // message about log locations.
        Path logPath = Path.of("logs", "owlcms.log");
        if (Files.exists(logPath)) {
            logger.info("Detailed log location: {}", logPath.toAbsolutePath());
        }

        // technical initializations
        ConvertUtils.register(new DateConverter(null), java.util.Date.class);
        ConvertUtils.register(new DateConverter(null), java.sql.Date.class);

        // dependency injection
        injectSuppliers();

        return;
    }

    protected static void tearDown() {
        JPAService.close();
    }

    private static void injectData(InitialData data,
            Locale locale) {
        Locale l = (locale == null ? Locale.ENGLISH : locale);
        EnumSet<AgeDivision> ageDivisions = masters ? EnumSet.of(AgeDivision.MASTERS, AgeDivision.U) : null;
        try {
            Translator.setForcedLocale(l);
            // if a reset was required (e.g. for demonstrations, or to reinitialize, this
            // has been handled beforehand by Hibernate when opening the database.
            List<Competition> allCompetitions = CompetitionRepository.findAll();
            if (data == InitialData.LEAVE_AS_IS && allCompetitions.isEmpty()) {
                // overide - we cannot leave the database empty
                data = InitialData.EMPTY_COMPETITION;
            }

            // there is no config read so far.
            boolean publicDemo = StartupUtils.getBooleanParam("publicDemo");
            if (allCompetitions.isEmpty() || publicDemo) {
                logger.info("injecting initial data {}", data);
                switch (data) {
                case EMPTY_COMPETITION:
                    ProdData.insertInitialData(0);
                    break;
                case LARGEGROUP_DEMO:
                    DemoData.insertInitialData(14, ageDivisions);
                    break;
                case LEAVE_AS_IS:
                    break;
                case SINGLE_ATHLETE_GROUPS:
                    DemoData.insertInitialData(1, ageDivisions);
                    break;
                }
            } else {
                // migrations and other changes
                logger.info("database not empty: {}", allCompetitions.get(0).getCompetitionName());
                List<AgeGroup> ags = AgeGroupRepository.findAll();
                if (ags.isEmpty()) {
                    logger.info("creating age groups and categories");
                    JPAService.runInTransaction(em -> {
                        AgeGroupRepository.insertAgeGroups(em, null);
                        return null;
                    });
                }
                List<Config> configs = ConfigRepository.findAll();
                if (configs.isEmpty()) {
                    logger.info("adding config object");
                    Config.setCurrent(new Config());
                }

                int nbParts = CategoryRepository.countParticipations();
                if (nbParts == 0 && AthleteRepository.countFiltered(null, null, null, null, null, null, null) > 0) {
                    // database has athletes, but no participations. 4.22 and earlier.
                    // need to create Participation entries for the Athletes.
                    logger.info("updating database: computing athlete eligibility to age groups and categories.");
                    AthleteRepository.resetParticipations();
                }

                List<Category> nullCodeCategories = CategoryRepository.findNullCodes();
                if (!nullCodeCategories.isEmpty()) {
                    logger.info("updating category codes", nullCodeCategories);
                    CategoryRepository.fixNullCodes(nullCodeCategories);
                }

                PlatformRepository.checkPlatforms();
            }
            resetRecords();
        } finally {
            Translator.setForcedLocale(locale);
        }
    }

    private static Locale overrideDisplayLanguage() {
        // read override value from database, if it was previously created.
        Locale l = null;
        try {
            l = Config.getCurrent().getDefaultLocale();
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

        Translator.setForcedLocale(l);
        if (l != null) {
            logger.info("forcing display language to {}", l);
        } else {
            logger.info("using per-session browser language", l);
        }
        return l;
    }

    private static void overrideTimeZone() {
        // read override value from database, if it was previously created.
        TimeZone tz = null;
        tz = Config.getCurrent().getTimeZone();
        if (tz != null) {
            TimeZone.setDefault(tz);
        }
    }

    /**
     * get configuration from environment variables and if not found, from system properties.
     */
    private static void parseConfig() {
        // under Kubernetes deployed under an owlcms service LoadBalancer
        String k8sServicePortString = StartupUtils.getStringParam("service_port");
        if (k8sServicePortString != null) {
            // we are running under a Kubernetes ingress or load balancer
            // which handles the mapping for us. We run on the default.
            serverPort = 8080;
        } else {
            // read port parameter from -Dport=9999 on java command line
            // this is required for running on Heroku which assigns us the port at run time.
            // default is 8080
            logger.trace("{}", "reading port from properties and environment");
            serverPort = StartupUtils.getIntegerParam("port", 8080);
        }

        StartupUtils.setServerPort(serverPort);

        // drop the schema first
        resetMode = StartupUtils.getBooleanParam("resetMode") || demoMode || memoryMode;

        String initialDataString = StartupUtils.getStringParam("initialData");
        try {
            initialData = InitialData.valueOf(initialDataString.toUpperCase());
        } catch (Exception e) {
            // no initial data setting, infer from legacy options
            if (!resetMode) {
                initialData = InitialData.LEAVE_AS_IS;
            } else if (demoMode || demoData) {
                initialData = InitialData.LARGEGROUP_DEMO;
            } else if (smallData) {
                initialData = InitialData.SINGLE_ATHLETE_GROUPS;
            } else {
                initialData = InitialData.EMPTY_COMPETITION;
                if (initialDataString != null) {
                    logger.error("unrecognized OWLCMS_INITIALDATA value: {}, defaulting to {}", initialDataString,
                            initialData);
                }
            }
        }

        masters = StartupUtils.getBooleanParam("masters");
    }

    private static void resetRecords() {
        Path recordsPath;
        try {
            recordsPath = ResourceWalker.getFileOrResourcePath("/records");
            try {
                RecordRepository.clearRecords();
                if (recordsPath != null && Files.exists(recordsPath)) {
                    RecordDefinitionReader.readFolder(recordsPath);
                } else {
                    logger.info("no record definition files in local/records");
                }
            } catch (IOException e) {
                logger.error("cannot process records {}");
            }
        } catch (FileNotFoundException e1) {
            logger.error("cannot find records {}", LoggerUtils.stackTrace(e1));
        }

    }

    private static void warnAndExit(Integer demoResetDelay, EmbeddedJetty server)
            throws InterruptedException {

        Thread.sleep(demoResetDelay * 1000);
        String warningText = Translator.translate("App.ResetWarning", Integer.toString(WARNING_MINUTES));
        AppEvent.AppNotification warning = new AppEvent.AppNotification(warningText);
        // server.start() hijacks stderr and stdout. Must use new thread to log.
        new Thread(() -> {
            logger.info(warningText);
        }).start();

        OwlcmsFactory.getAppUIBus().post(warning);
        Thread.sleep(WARNING_MINUTES * 60 * 1000);
        OwlcmsFactory.getAppUIBus().post(new AppEvent.CloseUI());
        Thread.sleep(5 * 1000);

        // public demo is run with a restart policy of "always", so k8s will restart everything
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("public demo server shut down");
        }));
        System.exit(0);
        return;
    }

}