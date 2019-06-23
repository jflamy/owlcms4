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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Scanner;

import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.vaadin.flow.i18n.I18NProvider;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TranslationProvider implements I18NProvider {

    private static final String CSV_DELIMITER = "\t";

    private final static TranslationProvider helper = new TranslationProvider();

    private static final String BUNDLEBASE = "translation4";
    private static final String BUNDLE_PACKAGE_DOTS = "i18n.";
    private static final String BUNDLE_PACKAGE_SLASH = "/i18n/";
    public static final String BUNDLE_DOTTED_NAME = BUNDLE_PACKAGE_DOTS+BUNDLEBASE;

    public static final Locale LOCALE_EN = new Locale("en");

    public static final Locale LOCALE_FR = new Locale("fr");
    public static final Locale LOCALE_DA = new Locale("da");
    public static final Locale LOCALE_ES = new Locale("es");
    private static List<Locale> locales = Collections
            .unmodifiableList(Arrays.asList(LOCALE_EN, LOCALE_FR, LOCALE_DA, LOCALE_ES));

    private static Locale forcedLocale;

    private static HashMap<String, ClassLoader> processed = new HashMap<>();

    public static List<Locale> getAvailableLocales() {
        return helper.getProvidedLocales();
    }

//    public static void main(String[] args) {
//        try {
////            Writer out = createCSV();
////            out.flush();
//            
////            getBundle(new File("src/main/resources/i18n/translation4.csv"),new Locale("fr"));
//            PropertyResourceBundle prb = (PropertyResourceBundle) getBundleFromCSV("translation4", LOCALE_FR);
//            
//        } catch (Throwable e1) {
//            e1.printStackTrace();
//        }
//    }

    public static void setForcedLocale(Locale locale) {
        if (locales.contains(locale)) {
            TranslationProvider.forcedLocale = locale;
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

    public static String translate(String string, Locale locale, Object... params) {
        return helper.getTranslation(string, OwlcmsSession.getLocale(), params);
    }

//    @SuppressWarnings("unused")
//    private static Writer createCSV() throws FileNotFoundException, IOException {
//        Writer out = new PrintWriter("translation.csv");// new OutputStreamWriter(System.out);
//
//        ResourceBundle masterBundle = ResourceBundle.getBundle(BUNDLE_DOTTED_NAME, Locale.ENGLISH);
//        for (Enumeration<String> masterKeys = masterBundle.getKeys(); masterKeys.hasMoreElements();) {
//            String key = masterKeys.nextElement();
//            escape(out, key);
//            for (Locale locale : helper.getProvidedLocales()) {
//                String translation = null;
//                try {
//                    if (locale.getLanguage().contentEquals("en")) {
//                        translation = helper.getTranslation(key, locale);
//                    } else {
//                        translation = helper.getTranslationOrNull(key, locale);
//                    }
//                } catch (Exception e) {
//                }
//                out.write("\t");
//                escape(out, translation);
//            }
//            out.write("\n");
//        }
//        return out;
//    }
    
//    private static void escape(Writer out, String string) throws IOException {
//        out.write('"');
//        // csv requires doubling double quotes inside strings
//        if (string != null)
//            out.write(string.replace("\"", "\"\""));
//        out.write('"');
//    }

//    @SuppressWarnings("unused")
//    private synchronized static ResourceBundle getBundle(final File csv, final Locale local) {
//        final String csvname = csv.getName().replace(".csv", "");
//        ClassLoader i18nloader = TranslationProvider.processed.get(csv.getName());
//        if (i18nloader == null) {
//            try (final Scanner in = new Scanner(csv)) {
//                // process header
//                final String[] header = in.nextLine().split(";");
//                final File[] outFiles = new File[header.length];
//                final Properties[] languageProperties = new Properties[header.length];
//                System.err.println("header.length"+header.length);
//                for (int i = 1; i < header.length; i++) {
//                    String language = header[i];
//                    System.err.println(""+i+" "+language);
//                    if (!language.isEmpty()) {
//                        language = "_" + language;
//                    }
//                    final File outfile = new File(csv.getParentFile(), csvname + language + ".properties");
//                    outFiles[i] = outfile;
//                    languageProperties[i] = new Properties();
//                }
//
//                // reading to properties
//                while (in.hasNextLine()) {
//                    String nextLine = in.nextLine();
//                    final String[] line = nextLine.split(";",header.length);
//                    System.err.println(nextLine);
//                    final String key = line[0];
//                    for (int i = 1; i < languageProperties.length; i++) {
//                        languageProperties[i].setProperty(key, line[i]);
//                    }
//                }
//
//                // writing
//                for (int i = 1; i < languageProperties.length; i++) {
//                    languageProperties[i].store(new FileOutputStream(outFiles[i]), "generated from " + csv.getName());
//                }
//                final URL[] urls = { csv.getParentFile().toURI().toURL() };
//                i18nloader = new URLClassLoader(urls);
//                TranslationProvider.processed.put(csv.getName(), i18nloader);
//            } catch (final IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return ResourceBundle.getBundle(csvname, local, i18nloader);
//    }
    
    private synchronized static ResourceBundle getBundleFromCSV(String baseName, final Locale locale) {
        String csvName = BUNDLE_PACKAGE_SLASH+baseName+".csv";
        InputStream csvStream = helper.getClass().getResourceAsStream(csvName);
        ClassLoader i18nloader = TranslationProvider.processed.get(baseName);
        File bundleDir = Files.createTempDir();
        if (i18nloader == null) {
//            System.out.println("creating "+baseName+" from "+csvName+" "+csvStream);
            try (final Scanner in = new Scanner(csvStream)) {
                // process header
                final String[] header = in.nextLine().split(CSV_DELIMITER);
                final File[] outFiles = new File[header.length];
//                System.out.println("header.length"+header.length);
                final Properties[] languageProperties = new Properties[header.length];
                for (int i = 1; i < header.length; i++) {
                    String language = header[i];
                    if (!language.isEmpty()) {
                        language = "_" + language;
                    }
                    final File outfile = new File(bundleDir, baseName + language + ".properties");
                    outFiles[i] = outfile;
                    languageProperties[i] = new Properties();
                }

                // reading to properties
                while (in.hasNextLine()) {
                    String nextLine = in.nextLine();
                    final String[] line = nextLine.split(CSV_DELIMITER,header.length);
                    final String key = line[0];
                    for (int i = 1; i < languageProperties.length; i++) {
                        languageProperties[i].setProperty(key, line[i]);
                    }
                }

                // writing
                for (int i = 1; i < languageProperties.length; i++) {
//                    System.out.println("writing to "+outFiles[i].getAbsolutePath());
                    languageProperties[i].store(new FileOutputStream(outFiles[i]), "generated from " + csvName);
                }
                final URL[] urls = { bundleDir.toURI().toURL() };
                i18nloader = new URLClassLoader(urls);
                TranslationProvider.processed.put(baseName, i18nloader);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ResourceBundle.getBundle(baseName, locale, i18nloader);
    }

    Logger logger = (Logger) LoggerFactory.getLogger(TranslationProvider.class.getName());

    @Override
    public List<Locale> getProvidedLocales() {
        if (forcedLocale != null) {
            return Arrays.asList(forcedLocale);
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

        //final ResourceBundle bundle = PropertyResourceBundle.getBundle(BUNDLE_DOTTED_NAME, locale);
        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(BUNDLEBASE, locale);

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
        forcedLocale = LOCALE_FR;
        if (forcedLocale != null) {
            locale = forcedLocale;
        }
        return locale;
    }

    public String getTranslationOrNull(String key, Locale locale, Object... params) {
        locale = overrideLocale(locale);
        
        if (key == null) {
            logger.warn("null translation key");
            return "";
        }

//        final PropertyResourceBundle bundle = (PropertyResourceBundle) PropertyResourceBundle.getBundle(BUNDLE_DOTTED_NAME, locale);
        final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(BUNDLEBASE, locale);

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