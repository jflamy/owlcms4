/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.i18n;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.i18n.I18NProvider;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TranslationProvider implements I18NProvider {


    Logger logger = (Logger) LoggerFactory.getLogger(TranslationProvider.class.getName());

    private final static TranslationProvider helper = new TranslationProvider();

    public static final String BUNDLE_PREFIX = "i18n.messages";

    public final Locale LOCALE_EN = new Locale("en");
    public final Locale LOCALE_FR = new Locale("fr");
    public final Locale LOCALE_DA = new Locale("da");
    public final Locale LOCALE_ES = new Locale("es");

    private List<Locale> locales = Collections
            .unmodifiableList(Arrays.asList(LOCALE_EN, LOCALE_FR, LOCALE_DA, LOCALE_ES));

    @Override
    public List<Locale> getProvidedLocales() {
        return locales;
    }

    /**
     * @see com.vaadin.flow.i18n.I18NProvider#getTranslation(java.lang.String,
     *      java.util.Locale, java.lang.Object[])
     */
    @Override
    public String getTranslation(String key, Locale locale, Object... params) {

        if (key == null) {
            logger.warn("null translation key");
            return "";
        }

        final ResourceBundle bundle = PropertyResourceBundle.getBundle(BUNDLE_PREFIX, locale);

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
    
    public String getTranslationOrNull(String key, Locale locale, Object... params) {

        if (key == null) {
            logger.warn("null translation key");
            return "";
        }

        final PropertyResourceBundle bundle = (PropertyResourceBundle) PropertyResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        
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

    public static String translate(String string) {
        return helper.getTranslation(string, OwlcmsSession.getLocale());
    }
    
    public static String translate(String string, Locale locale) {
        return helper.getTranslation(string, locale);
    }

    public static String translate(String string, Locale locale, Object... params) {
        return helper.getTranslation(string, OwlcmsSession.getLocale(), params);
    }
    
    public static void main(String[] args) {
        try {
            Writer out = new PrintWriter("translation.csv");//new OutputStreamWriter(System.out);

            ResourceBundle masterBundle = ResourceBundle.getBundle(BUNDLE_PREFIX,Locale.ENGLISH);
            for (Enumeration<String> masterKeys = masterBundle.getKeys(); masterKeys.hasMoreElements();) {
                String key = masterKeys.nextElement();
                escape(out, key);
                for (Locale locale : helper.getProvidedLocales()) {
                    String translation = null;
                    try {
                        if (locale.getLanguage().contentEquals("en")) {
                            translation = helper.getTranslation(key, locale);
                        } else {
                            translation = helper.getTranslationOrNull(key, locale);
                        }
                    } catch (Exception e) {
                    }
                    out.write("\t");
                    escape(out, translation);
                }
                out.write("\n");
            }
            out.flush();
        } catch (Throwable e1) {
            e1.printStackTrace();
        }
    }

    private static void escape(Writer out, String string) throws IOException {
        out.write('"');
        // csv requires doubling double quotes inside strings
       if (string != null) out.write(string.replace("\"", "\"\""));
        out.write('"');
    }
}