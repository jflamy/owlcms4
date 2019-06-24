/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.i18n;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Scanner;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.vaadin.flow.i18n.I18NProvider;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * This class creates a resource bundle from a CSV file containing the various
 * translations, and provides translations for Components according to the Vaadin translation
 * spec. 
 * 
 * Static variations of the translation routines are also provided for
 * translations that do not take place inside Vaadin components (e.g.
 * spreadsheets).
 *
 */
@SuppressWarnings("serial")
public class Translator implements I18NProvider {

    final static Logger logger = (Logger) LoggerFactory.getLogger(Translator.class);

    private static final String CSV_DELIMITER = ";";

    private final static Translator helper = new Translator();

    private static final String BUNDLE_BASE = "translation4";
    private static final String BUNDLE_PACKAGE_SLASH = "/i18n/";

    private static List<Locale> locales = null;

    private static Locale forcedLocale;

    private static HashMap<String, ClassLoader> processed = new HashMap<>();

    public static List<Locale> getAvailableLocales() {
        return helper.getProvidedLocales();
    }

    public static void setForcedLocale(Locale locale) {
        if (getAvailableLocales().contains(locale)) {
            Translator.forcedLocale = locale;
        } else {
            locale = null; // default behaviour, first forcedLocale in list will be used
        }
    }

    public static String translate(String string) {
        return helper.getTranslation(string, OwlcmsSession.getLocale());
    }

    public static String translate(String string, Locale locale) {
        return helper.getTranslation(string, locale);
    }

    public static String translateOrElseNull(String string, Locale locale) {
        return helper.getTranslationOrElseNull(string, locale);
    }

    public static String translate(String string, Locale locale, Object... params) {
        return helper.getTranslation(string, OwlcmsSession.getLocale(), params);
    }

    /**
     * Adapted from https://hub.jmonkeyengine.org/t/i18n-from-csv-calc/31492
     * 
     * @param baseName
     * @param locale
     * @return
     */
    private synchronized static ResourceBundle getBundleFromCSV(String baseName, final Locale locale) {
        String csvName = BUNDLE_PACKAGE_SLASH + baseName + ".csv";
        InputStream csvStream = helper.getClass().getResourceAsStream(csvName);
        ClassLoader i18nloader = Translator.processed.get(baseName);
        File bundleDir = Files.createTempDir();
        if (i18nloader == null) {
            logger.trace("creating {} from {} : {}", baseName, csvName, csvStream);
            try (final Scanner in = new Scanner(csvStream, "ISO-8859-1")) {
                // process header
                final String[] header = in.nextLine().split(CSV_DELIMITER);
                final File[] outFiles = new File[header.length];
                final Properties[] languageProperties = new Properties[header.length];
                locales = new ArrayList<>();
                for (int i = 1; i < header.length; i++) {
                    String language = header[i];
                    locales.add(createLocale(language));
                    if (!language.isEmpty()) {
                        language = "_" + language;
                    }
                    final File outfile = new File(bundleDir, baseName + language + ".properties");
                    outFiles[i] = outfile;
                    languageProperties[i] = new Properties();
                }
                logger.debug("languages: {}",locales);

                // reading to properties
                while (in.hasNextLine()) {
                    String nextLine = in.nextLine();
                    final String[] line = nextLine.split(CSV_DELIMITER, header.length);
                    final String key = line[0];
                    logger.trace("{}", nextLine);
                    for (int i = 1; i < languageProperties.length; i++) {
                        // treat the CSV strings using same rules as Properties files.
                        // u0000 escapes are translated to Java characters
                        String input = line[i];
                        String unescapeJava = StringEscapeUtils.unescapeJava(input);
                        languageProperties[i].setProperty(key, unescapeJava);
                    }
                }

                // writing
                for (int i = 1; i < languageProperties.length; i++) {
                    logger.debug("writing to {}", outFiles[i].getAbsolutePath());
                    languageProperties[i].store(new FileOutputStream(outFiles[i]), "generated from " + csvName);
                }
                final URL[] urls = { bundleDir.toURI().toURL() };
                i18nloader = new URLClassLoader(urls);
                Translator.processed.put(baseName, i18nloader);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ResourceBundle.getBundle(baseName, locale, i18nloader);
    }

    private static Locale createLocale(String localeString) {
        if (localeString == null) {
            throwInvalidLocale(localeString);
            return null;  // unreacheable
        } else {
            String[] parts = localeString.split("_");
            if (parts.length == 1) {
                return new Locale(parts[0]);
            } else if (parts.length == 2) {
                return new Locale(parts[0],parts[1]);
            } else if (parts.length >= 3) {
                return new Locale(parts[0],parts[1], parts[2]);
            } else {
                throwInvalidLocale(localeString);
                return null;  // unreacheable
            }
        }
    }

    private static void throwInvalidLocale(String localeString) {
        String message = MessageFormat.format("invalid locale: {0}",localeString);
        logger.error(message);
        throw new RuntimeException(message);
    }

    @Override
    public List<Locale> getProvidedLocales() {
        if (forcedLocale != null) {
            return Arrays.asList(forcedLocale);
        } else if (locales == null) {
            // sets the available locales
            getBundleFromCSV(BUNDLE_BASE, Locale.ENGLISH);
        }
        return locales;
    }

    /**
     * @see com.vaadin.flow.i18n.I18NProvider#getTranslation(java.lang.String,
     *      java.util.Locale, java.lang.Object[])
     */
    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        locale = overrideLocale(locale);

        if (key == null) {
            logger.warn("null translation key");
            return "";
        }

        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(BUNDLE_BASE, locale);

        String value;
        try {
            value = bundle.getString(key);
        } catch (final MissingResourceException e) {
            return "!" + locale.getLanguage() + ": " + key;
        }
        if (params.length > 0) {
            value = MessageFormat.format(value, params);
        }
        return value;
    }

    private Locale overrideLocale(Locale locale) {
        if (forcedLocale != null) {
            locale = forcedLocale;
        }
        return locale;
    }

    public String getTranslationOrElseNull(String key, Locale locale, Object... params) {
        locale = overrideLocale(locale);

        if (key == null) {
            logger.warn("null translation key");
            return "";
        }
        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(BUNDLE_BASE, locale);

        String value;
        try {
            value = (String) bundle.handleGetObject(key);
        } catch (final MissingResourceException e) {
            return "!" + locale.getLanguage() + ": " + key;
        }
        if (params.length > 0) {
            value = MessageFormat.format(value, params);
        }
        return value;
    }
}