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
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.google.common.io.Files;
import com.vaadin.flow.i18n.I18NProvider;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * This class creates a resource bundle from a CSV file containing the various
 * translations, and provides translations for Components according to the
 * Vaadin translation spec.
 *
 * Static variations of the translation routines are also provided for
 * translations that do not take place inside Vaadin components (e.g.
 * spreadsheets).
 *
 */
@SuppressWarnings("serial")
public class Translator implements I18NProvider {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Translator.class);
    private static final Translator helper = new Translator();
    private static final String BUNDLE_BASE = "translation4";
    private static final String BUNDLE_PACKAGE_SLASH = "/i18n/";

    private static List<Locale> locales = null;
    private static Locale forcedLocale = null;
    private static ClassLoader i18nloader = null;
    private static int line;

    public static Locale createLocale(String localeString) {
        if (localeString == null) {
            throwInvalidLocale(localeString);
            return null; // unreacheable
        } else {
            String[] parts = localeString.split("_");
            if (parts.length == 1) {
                return new Locale(parts[0]);
            } else if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            } else if (parts.length >= 3) {
                return new Locale(parts[0], parts[1], parts[2]);
            } else {
                throwInvalidLocale(localeString);
                return null; // unreacheable
            }
        }
    }

    public static List<Locale> getAllAvailableLocales() {
        return locales;
    }

    public static List<Locale> getAvailableLocales() {
        return helper.getProvidedLocales();
    }

    /**
     * Return a resource bundle created by reading a CSV files. This creates
     * properties files, and uses the standard caching implementation and bundle
     * hierarchy as defined by Java.
     *
     * Resource bundles are cached by the Java implementation, so this method can be
     * called repeatedly.
     *
     * Adapted from https://hub.jmonkeyengine.org/t/i18n-from-csv-calc/31492
     *
     * @param locale
     *
     * @return
     */
    private static ResourceBundle getBundleFromCSV(Locale locale) {
        String baseName = BUNDLE_BASE;
        String csvName = BUNDLE_PACKAGE_SLASH + baseName + ".csv";
        File bundleDir = Files.createTempDir();
        line = 0;

        if (i18nloader == null) {
            logger.debug("reloading translation bundles");

            InputStream csvStream = helper.getClass().getResourceAsStream(csvName);
            ICsvListReader listReader = null;
            try {
                CsvPreference[] preferences = new CsvPreference[] { CsvPreference.STANDARD_PREFERENCE,
                        CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE, CsvPreference.TAB_PREFERENCE };

                List<String> stringList = new ArrayList<>();
                for (CsvPreference preference : preferences) {
                    listReader = new CsvListReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8),
                            preference);

                    if ((stringList = readLine(listReader)) == null) {
                        throw new RuntimeException(csvName + " file is empty");
                    } else if (stringList.size() <= 2) {
                        // reset stream
                        csvStream = helper.getClass().getResourceAsStream(csvName);
                    } else {
                        logger.debug(stringList.toString());
                        break;
                    }
                }

                final File[] outFiles = new File[stringList.size()];
                final Properties[] languageProperties = new Properties[outFiles.length];
                locales = new ArrayList<>();
                for (int i = 1; i < outFiles.length; i++) {
                    String language = stringList.get(i);
                    locales.add(createLocale(language));
                    if (language != null && !language.isEmpty()) {
                        language = "_" + language;
                    }
                    final File outfile = new File(bundleDir, baseName + language + ".properties");
                    outFiles[i] = outfile;
                    languageProperties[i] = new Properties();
                }

                // reading to properties
                while ((stringList = readLine(listReader)) != null) {
                    final String key = stringList.get(0);
                    if (key == null) {
                        String message = MessageFormat.format("{0} line {1}: key is null", csvName, line);
                        logger.error(message);
                        throw new RuntimeException(message);
                    }
                    logger.debug(stringList.toString());
                    for (int i = 1; i < languageProperties.length; i++) {
                        // treat the CSV strings using same rules as Properties files.
                        // u0000 escapes are translated to Java characters
                        String input = stringList.get(i);
                        if (input != null) {
                            // "\ " is not valid, \u0020 is needed.
                            String unescapeJava = StringEscapeUtils.unescapeJava(input.trim());
                            if (!unescapeJava.isEmpty()) {
                                Properties properties = languageProperties[i];
                                if (properties == null) {
                                    String message = MessageFormat
                                            .format("{0} line {1}: languageProperties[{2}] is null", csvName, line, i);
                                    logger.error(message);
                                    throw new RuntimeException(message);
                                }
                                properties.setProperty(key, unescapeJava);
                            }
                        }
                    }
                }

                // writing
                for (int i = 1; i < languageProperties.length; i++) {
                    logger.debug("writing to " + outFiles[i].getAbsolutePath());
                    languageProperties[i].store(new FileOutputStream(outFiles[i]), "generated from " + csvName);
                }
                final URL[] urls = { bundleDir.toURI().toURL() };
                i18nloader = new URLClassLoader(urls);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (listReader != null) {
                    try {
                        listReader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        // reload the files
        ResourceBundle.clearCache();
        return ResourceBundle.getBundle(baseName, locale, i18nloader);
    }

    public static Enumeration<String> getKeys() {
        return Translator.getBundleFromCSV(Locale.ENGLISH).getKeys();
    }

    public static List<String> readLine(ICsvListReader listReader) throws IOException {
        line++;
        return listReader.read();
    }

    /**
     * Force a reload of the translation files
     */
    public static void reset() {
        locales = null;
        i18nloader = null;
        logger.debug("cleared translation class loader");
    }

    public static void setForcedLocale(Locale locale) {
        if (locale != null && getAvailableLocales().contains(locale)) {
            Translator.forcedLocale = locale;
        } else {
            Translator.forcedLocale = null; // default behaviour, first forcedLocale in list will be used
        }
    }

    private static void throwInvalidLocale(String localeString) {
        String message = MessageFormat.format("invalid locale: {0}", localeString);
        logger.error(message);
        throw new RuntimeException(message);
    }

    public static String translate(String string) {
        return helper.getTranslation(string, OwlcmsSession.getLocale());
    }

    public static String translate(String string, Locale locale) {
        return helper.getTranslation(string, locale);
    }

    public static String translate(String string, Locale locale, Object... params) {
        return helper.getTranslation(string, OwlcmsSession.getLocale(), params);
    }

    public static String translate(String string, Object... params) {
        return helper.getTranslation(string, OwlcmsSession.getLocale(), params);
    }

    public static String translateOrElseNull(String string, Locale locale) {
        return helper.getTranslationOrElseNull(string, locale);
    }

    private String format(String key, Locale locale, String value, Object... params) {
        if (params.length > 0) {
            try {
                value = MessageFormat.format(value, params);
            } catch (Exception e) {
                value = "!" + locale.getLanguage() + ": " + e.getLocalizedMessage() + ": " + key + " " + params;
            }
        }
        return value;
    }

    @Override
    public List<Locale> getProvidedLocales() {
        if (forcedLocale != null) {
            return Arrays.asList(forcedLocale);
        } else if (locales == null) {
            // sets the available locales
            getBundleFromCSV(Locale.ENGLISH);
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
            nullTranslationKey();
            return "";
        }

        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);

        String value;
        try {
            value = bundle.getString(key);
        } catch (final MissingResourceException e) {
            return "!" + locale.getLanguage() + ": " + key;
        }
        value = format(key, locale, value, params);
        return value;
    }

    public String getTranslationOrElseNull(String key, Locale locale, Object... params) {
        locale = overrideLocale(locale);

        if (key == null) {
            nullTranslationKey();
            return "";
        }
        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);

        String value;
        try {
            value = (String) bundle.handleGetObject(key);
        } catch (final MissingResourceException e) {
            return null;
        }
        value = format(key, locale, value, params);
        return value;
    }

    public void nullTranslationKey() {
        logger/**/.warn("null translation key");
    }

    private Locale overrideLocale(Locale locale) {
        if (forcedLocale != null) {
            locale = forcedLocale;
        }
        return locale;
    }
}