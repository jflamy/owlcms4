/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.publicresults;

import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import app.owlcms.i18n.Translator;
import app.owlcms.servlet.EmbeddedJetty;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

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
            init();
            EmbeddedJetty embeddedJetty = new EmbeddedJetty(new CountDownLatch(0));
            embeddedJetty.setStartLogger(logger);
            embeddedJetty.setInitConfig(() -> {});
            embeddedJetty.setInitData(() -> {});
            embeddedJetty.run(serverPort, "/");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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

        // read command-line and environment variable parameters
        parseConfig();
        StartupUtils.logStart("publish", serverPort);

        // Vaadin configs
        System.setProperty("vaadin.i18n.provider", Translator.class.getName());

        // app config injection
        Translator.setLocaleSupplier(() -> Locale.FRENCH);
        ResourceWalker.setLocaleSupplier(Translator.getLocaleSupplier());
        ResourceWalker.setLocalOverrideSupplier(() -> {
            ResourceWalker.checkForLocalOverrideDirectory();
            return null;
        });

        // technical initializations
        System.setProperty("java.net.preferIPv4Stack", "true");
        return;
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

        overrideDisplayLanguage();
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

}