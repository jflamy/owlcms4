/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.i18n;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.vaadin.flow.i18n.I18NProvider;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TranslationProvider implements I18NProvider {

	Logger logger = (Logger) LoggerFactory.getLogger(TranslationProvider.class.getName());

	public static final String BUNDLE_PREFIX = "translate";

	public final Locale LOCALE_FR = new Locale("fr");
	public final Locale LOCALE_EN = new Locale("en");
	public final Locale LOCALE_FR_CA = new Locale("fr", "CA");

	private List<Locale> locales = Collections.unmodifiableList(Arrays.asList(LOCALE_EN, LOCALE_FR, LOCALE_FR_CA));

	private static final LoadingCache<Locale, ResourceBundle> bundleCache = CacheBuilder.newBuilder()
		.expireAfterWrite(1, TimeUnit.DAYS).build(new CacheLoader<Locale, ResourceBundle>() {

			@Override
			public ResourceBundle load(final Locale key) throws Exception {
				return initializeBundle(key);
			}
		});

	@Override
	public List<Locale> getProvidedLocales() {
		return locales;
	}

	/**
	 * @see com.vaadin.flow.i18n.I18NProvider#getTranslation(java.lang.String, java.util.Locale, java.lang.Object[])
	 */
	@Override
	public String getTranslation(String key, Locale locale, Object... params) {

		if (key == null) {
			logger.warn("Got lang request for key with null value!");
			return "";
		}

		final ResourceBundle bundle = bundleCache.getUnchecked(locale);

		String value;
		try {
			value = bundle.getString(key);
		} catch (final MissingResourceException e) {
			logger.warn("Missing resource", e);
			return "!" + locale.getLanguage() + ": " + key;
		}
		if (params.length > 0) {
			value = MessageFormat.format(value, params);
		}
		return value;
	}

	private static ResourceBundle initializeBundle(final Locale locale) {
		return readProperties(locale);
	}

	protected static ResourceBundle readProperties(final Locale locale) {
		final ClassLoader cl = TranslationProvider.class.getClassLoader();

		ResourceBundle propertiesBundle = null;
		try {
			propertiesBundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale, cl);
		} catch (final MissingResourceException e) {
			LoggerFactory.getLogger(TranslationProvider.class.getName()).warn("Missing resource", e);
		}
		return propertiesBundle;
	}

    public static String getString(String string) {
        // FIXME: use TransationProvider
        return Messages.getString(string, Locale.ENGLISH);
    }

    public static String getTranslation(String string) {
     // FIXME: use TransationProvider
        return Messages.getString(string, Locale.ENGLISH);
    }
}