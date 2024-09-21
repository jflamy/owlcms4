/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.publicresults;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.util.concurrent.Runnables;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.i18n.Translator;
import app.owlcms.servlet.EmbeddedJetty;
import app.owlcms.servlet.ExitException;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Main.
 */
public class Main {

    public final static Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

    public static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    public static String productionMode;

    private static Integer serverPort;

    public static void logSessionMemUsage(String message, VaadinSession session) {
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        int megaB = 1024 * 1024;
        message = message != null && !message.isBlank() ? message + " " : "";
        long committed = heapMemoryUsage.getCommitted() / megaB;
        long used = heapMemoryUsage.getUsed() / megaB;
        float usageRatio = ((float)used/(float)committed);
//        if (committed > 300 && usageRatio > 0.75) {
//            new Thread(() -> {
//                logger.warn("restarting because usage ratio = {}",committed);
//                throw new ExitException("over " + committed);
//            }).start();
//        }
        LoggerFactory.getLogger(Main.class).info("{}sessions: {}, heap {}/{} nonHeap {}/{} {}",
                message,
                AppShell.getActiveSessions().get(),
                used,
                committed,
                nonHeapMemoryUsage.getUsed() / megaB,
                nonHeapMemoryUsage.getCommitted() / megaB,
                session != null ? System.identityHashCode(session) : "");
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
            doStart();
        } catch (Exception e) {
            LoggerUtils.logError(logger, e);
        } finally {
        }
    }

    private static void doStart() throws Exception {
        periodicTasks();
        new EmbeddedJetty(new CountDownLatch(0), "publicresults")
                .setStartLogger(logger)
                .setInitConfig(Runnables::doNothing)
                .setInitData(Runnables::doNothing)
                .run(serverPort, "/");
    }

    /**
     * Prepare owlcms
     *
     * Reads configuration options, injects data, initializes singletons and
     * configurations. The embedded web server can
     * then be started.
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
        threadExceptionHandling();

        // Configure logging -- must take place before anything else
        // Redirect java.util.logging logs to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // read command-line and environment variable parameters
        parseConfig();
        StartupUtils.logStart("publish", serverPort);

        // Vaadin configs
        System.setProperty("vaadin.i18n.provider", Translator.class.getName());
        System.setProperty("vaadin.closeIdleSessions", "true");
        System.setProperty("heartbeatInterval", "30");

        // app config injection
        Translator.setLocaleSupplier(Main::computeLocale);
        ResourceWalker.setLocaleSupplier(Translator.getLocaleSupplier());
        ResourceWalker.setLocalZipBlobSupplier(() -> {
            // no database, so no override in database.
            return null;
        });

        // technical initializations
        // System.setProperty("java.net.preferIPv4Stack", "true");

    }

    private static void threadExceptionHandling() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof ExitException) {
                    try {
                        logger.error("********** Stopping server.");
                        EmbeddedJetty.getJettyServer().stop();
                        System.exit(1); // trigger restart on-fail
//                        logger.error("Restarting server.");
//                        System.gc();
//                        System.gc();
//                        doStart();
                    } catch (Exception e2) {
                        LoggerUtils.logError(logger, e2);
                    }
                } else {
                    System.out.println("Caught " + e);
                }
            }
        });
    }

    private static Locale computeLocale() {
        String stringParam = StartupUtils.getStringParam("locale");
        if (stringParam == null) {
            return Locale.getDefault();
        } else {
            return Translator.createLocale(stringParam);
        }
    }

    private static Locale overrideDisplayLanguage() {
        // read override value from database
        Locale l = null;

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
     * get configuration from environment variables and if not found, from system
     * properties.
     */
    private static void parseConfig() {
        // read server.port parameter from -D"server.port"=9999 on java command line
        // this is required for running on Heroku which assigns us the port at run time.
        // default is 8080
        serverPort = StartupUtils.getIntegerParam("port", 8080);
        StartupUtils.setServerPort(serverPort);

        overrideDisplayLanguage();
    }

    private static void periodicTasks() {
        new Thread(() -> {
            while (true) {
                String message = "";
                try {
                    logSessionMemUsage(message, null);
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                }
            }
        }).start();
    }

}