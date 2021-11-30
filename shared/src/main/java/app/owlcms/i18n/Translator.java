/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.i18n;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.vaadin.flow.i18n.I18NProvider;

import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * This class creates a resource bundle from a CSV file containing the various translations, and provides translations
 * for Components according to the Vaadin translation spec.
 *
 * Static variations of the translation routines are also provided for translations that do not take place inside Vaadin
 * components (e.g. spreadsheets).
 *
 */
@SuppressWarnings("serial")
public class Translator implements I18NProvider {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Translator.class);
    private static Translator helper = new Translator();
    private static final String BUNDLE_BASE = "translation4";
    private static final String BUNDLE_PACKAGE_SLASH = "/i18n/";

    private static List<Locale> locales = null;
    private static Locale forcedLocale = null;
    private static ClassLoader i18nloader = null;
    private static int line;
    private static long resetTimeStamp = System.currentTimeMillis();

    private static Supplier<Locale> localeSupplier;

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
        if (locales == null) {
            Translator.getBundleFromCSV(Locale.ENGLISH);
        }
        return locales;
    }

    public static List<Locale> getAvailableLocales() {
        return helper.getProvidedLocales();
    }

    public static Locale getForcedLocale() {
        return forcedLocale;
    }

    public static Enumeration<String> getKeys() {
        return Translator.getBundleFromCSV(Locale.ENGLISH).getKeys();
    }

    /**
     * @return the localeSupplier
     */
    public static Supplier<Locale> getLocaleSupplier() {
        return localeSupplier;
    }

    public static Map<String, String> getMap() {
        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(getLocaleSupplier().get());
        Map<String, String> translations = new HashMap<>();
        Enumeration<String> keys = bundle.getKeys();
        String key;
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            translations.put(key, bundle.getString(key));
        }
        return translations;
    }

    public static long getResetTimeStamp() {
        return resetTimeStamp;
    }

    public static List<String> readLine(ICsvListReader listReader) throws IOException {
        line++;
        return listReader.read();
    }

    /**
     * Force a reload of the translation files
     */
    public static void reset() {
        resetTimeStamp = System.currentTimeMillis();
        locales = null;
        i18nloader = null;
        helper = new Translator();
        logger.debug("cleared translation class loader");
    }

    public static void setForcedLocale(Locale locale) {
        if (locale != null) {
            locales = getAllAvailableLocales();

            for (Locale l : getAllAvailableLocales()) {
                if (l.getLanguage() == locale.getLanguage()) {
                    // thing will work no matter what the country and variant
                    Translator.forcedLocale = locale;
                    break;
                }
            }
        } else {
            Translator.forcedLocale = null; // use browser-provided locale
        }
    }

    /**
     * @param localeSupplier the localeSupplier to set
     */
    public static void setLocaleSupplier(Supplier<Locale> localeSupplier) {
        Translator.localeSupplier = localeSupplier;
    }

    public static String translate(String string) {
        return helper.getTranslation(string, getLocaleSupplier().get());
    }

    public static String translate(String string, Locale locale) {
        return helper.getTranslation(string, locale);
    }

    public static String translate(String string, Locale locale, Object... params) {
        return helper.getTranslation(string, locale, params);
    }

    public static String translate(String string, Object... params) {
        return helper.getTranslation(string, getLocaleSupplier().get(), params);
    }

    public static String translateNoOverrideOrElseNull(String string, Locale locale) {
        return helper.getTranslationNoOverrideOrElseNull(string, locale);
    }

    public static String translateOrElseEn(String string, Locale locale) {
        return helper.getTranslationOrElseEn(string, locale);
    }

    public static String translateOrElseNull(String string, Locale locale) {
        return helper.getTranslationOrElseNull(string, locale);
    }

    /**
     * Return a resource bundle created by reading a CSV files. This creates properties files, and uses the standard
     * caching implementation and bundle hierarchy as defined by Java.
     *
     * Resource bundles are cached by the Java implementation, so this method can be called repeatedly.
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
        Path bundleDir = null;
        try {
            bundleDir = Files.createTempDirectory("bundles");
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        line = 0;

        if (i18nloader == null) {
            logger.debug("reloading translation bundles");
            InputStream csvStream = ResourceWalker.getResourceAsStream(csvName);
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
                        csvStream = ResourceWalker.getResourceAsStream(csvName);
                    } else {
                        logger.debug(stringList.toString());
                        break;
                    }
                }

                final File[] outFiles = new File[stringList.size()];
                final Properties[] languageProperties = new Properties[outFiles.length];
                locales = new ArrayList<>();

                int nbLanguages = 0;
                for (int i = 1; i < outFiles.length; i++) {
                    String language = stringList.get(i);
                    logger.trace("language={} {}", language, i);
                    if (language == null || language.isBlank()) {
                        nbLanguages = i - 1;
                        break;
                    }
                    locales.add(createLocale(language));
                    if (language != null && !language.isEmpty()) {
                        language = "_" + language;
                    }
                    final File outfile = new File(bundleDir.toFile(), baseName + language + ".properties");
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
                    for (int i = 1; i < nbLanguages + 1; i++) {
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
                for (int i = 1; i < nbLanguages + 1; i++) {
                    logger.debug("writing to " + outFiles[i].getAbsolutePath());
                    languageProperties[i].store(new FileOutputStream(outFiles[i]), "generated from " + csvName);
                }
                final URL[] urls = { bundleDir.toUri().toURL() };
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

    private static void throwInvalidLocale(String localeString) {
        String message = MessageFormat.format("invalid locale: {0}", localeString);
        logger.error(message);
        throw new RuntimeException(message);
    }

    @Override
    public List<Locale> getProvidedLocales() {
        if (getForcedLocale() != null) {
            return Arrays.asList(getForcedLocale());
        } else {
            return getAllAvailableLocales();
        }
    }

    /**
     * @see com.vaadin.flow.i18n.I18NProvider#getTranslation(java.lang.String, java.util.Locale, java.lang.Object[])
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
        if (params.length > 0) {
            value = format(value, params);
        }
        return value;
    }

    public String getTranslationNoOverrideOrElseNull(String key, Locale locale, Object... params) {
        if (key == null) {
            nullTranslationKey();
            return "";
        }
        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);

        String value;
        value = (String) bundle.handleGetObject(key);
        if (params.length > 0) {
            value = format(value, params);
        }
        return value;
    }

    public String getTranslationOrElseEn(String key, Locale locale, Object... params) {
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
            PropertyResourceBundle enBundle = (PropertyResourceBundle) getBundleFromCSV(Locale.ENGLISH);
            value = (String) enBundle.handleGetObject(key);
        }
        if (params.length > 0 && value != null) {
            value = format(value, params);
        }
        return value;
    }

    public String getTranslationOrElseNull(String key, Locale locale, Object... params) {
        locale = overrideLocale(locale);
        return getTranslationNoOverrideOrElseNull(key, locale, params);
    }

    public void nullTranslationKey() {
        logger./**/warn("null translation key");
    }

    private String format(String pattern, Object... params) {
        String value = pattern;
        if (params.length > 0) {
            // single quotes must be doubled. If already doubled in the input, fix back.
            pattern = pattern.replaceAll("'", "''");
            pattern = pattern.replaceAll("''''", "''");
            value = MessageFormat.format(pattern, params);
            // logger.trace("format {} input={} params={} \\n result={}", params.getClass(), pattern, params, value);
        }
        return value;
    }

    private Locale overrideLocale(Locale locale) {
        if (getForcedLocale() != null) {
            locale = getForcedLocale();
        }
        return locale;
    }

}