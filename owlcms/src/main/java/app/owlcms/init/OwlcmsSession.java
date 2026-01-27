/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
 

/**
 * Store the current user's settings and choices, across the multiple pages that may be opened.
 *
 * This class is either stored in a the Vaadin session shared between pages, or used as a singleton for testing.
 *
 * @author Jean-François Lamy
 */
public class OwlcmsSession {

	private static final String REQUESTED_URL = "requestedURL";
	private static final String QUERY_PARAMETERS = "queryParameters";
	private static final String DISPLAY_AUTHENTICATED = "displayAuthenticated";
	private static final String AUTHENTICATED = "authenticated";
	private static final String FOP = "fop";
	public static final String LOCALE = "locale";
	private final static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsSession.class);
	private static OwlcmsSession owlcmsSessionSingleton = null;

	static {
		logger.setLevel(Level.INFO);
	}

	@SuppressWarnings("deprecation")
	public static Locale computeLocale() {
		Locale locale = (Locale) getAttribute(LOCALE);

		if (locale != null) {
			return locale;
		}

		String country = null;
		Locale forcedLocale = Translator.getForcedLocale();
		if (forcedLocale != null) {
			country = forcedLocale.getCountry();
			if (country.isBlank()) {
				UI currentUi = UI.getCurrent();
				if (currentUi != null) {
					locale = currentUi.getLocale();
					if (locale != null) {
						country = locale.getCountry();
						@SuppressWarnings("deprecation")
						Locale combinedLocale = new Locale(forcedLocale.getLanguage(), country);
						locale = combinedLocale;
						//logger.debug("adding country from browser {}", locale);
						currentUi.setLocale(locale);
						setAttribute(LOCALE, locale);
						return locale;
					} else {
						// forced locale not improved with a country
						//logger.debug("cannot add country from browser {}", forcedLocale, country);
						return forcedLocale;
					}
				} else {
					// forced locale not improved with a country
					//logger.debug("no UI, cannot add country from browser {} {}", forcedLocale, LoggerUtils.stackTrace());
					return forcedLocale;
				}
			} else {
				// forced locale already had country
				//logger.debug("already have country from forced locale {}:", forcedLocale, country);
				return forcedLocale;
			}
		}

		UI currentUi = UI.getCurrent();
		if (currentUi != null) {
			// VaadinService.getCurrentRequest() may be null in some contexts (e.g. certain upload handlers).
			// Defensively handle that case and fall back to the UI locale or system default.
			com.vaadin.flow.server.VaadinRequest currentRequest = VaadinService.getCurrentRequest();
			List<Locale> acceptableLocales;
			if (currentRequest != null) {
				acceptableLocales = Collections.list(currentRequest.getLocales());
			} else {
				Locale uiLocale = currentUi.getLocale();
				if (uiLocale != null) {
					acceptableLocales = List.of(uiLocale);
				} else {
					acceptableLocales = List.of(Locale.getDefault());
				}
			}
			//logger.debug("acceptable locales: {}", acceptableLocales);
			List<Locale> availableLocales = Translator.getAvailableLocales();
			Locale match = null;
			Optional<Locale> found = findMatchingLocale(acceptableLocales, availableLocales);
			if (found.isEmpty()) {
				// Firefox does not add es if user has es_AR, so we check for es only
				match = findLanguageOnlyMatch(acceptableLocales, availableLocales);
			} else {
				match = found.get();
			}
			if (match.getCountry().isBlank()) {
				country = getFirstCountryFromLocales(acceptableLocales);
				if (country == null) {
					country = Locale.getDefault().getCountry();
				}
				String language = match.getLanguage();
				locale = new Locale(language, country);
				//logger.debug("adding country '{}' {}: {}", Locale.getDefault(), country, locale);
			} else {
				locale = match;
			}
			//logger.debug("match = {}", match);
			//logger.debug("setting session locale: {}",locale);
			currentUi.setLocale(locale);
			setAttribute(LOCALE, locale);
			return locale;
		} else {
			//logger.debug("Locale.ENGLISH as fallback");
			return Locale.ENGLISH;
		}
	}
	
    private static String getFirstCountryFromLocales(List<Locale> locales) {
        if (locales == null) {
            return null;
        }

        Optional<String> country = locales.stream()
                                          .map(Locale::getCountry) // Get the country code for each locale
                                          .filter(c -> c != null && !c.isEmpty()) // Filter out null or empty country codes
                                          .findFirst(); // Find the first one

        return country.orElse(null); // Return the country code or null if not found
    }
	
    private static Optional<Locale> findMatchingLocale(
            List<Locale> acceptableLocales,
            List<Locale> supportedLocales) {

        Set<Locale> supportedLocaleSet = new HashSet<>(supportedLocales);

        return acceptableLocales.stream()
                .filter(supportedLocaleSet::contains) // Filter for locales present in the supported set
                .findFirst();                         // Get the first one, if any
    }
    
    /**
     * Finds the first Locale from the acceptableLocales list whose language part
     * matches a language supported by the application, using Java Streams.
     * This method assumes all Locale objects have a non-empty language component.
     *
     * When a language match is found, it returns the *first* corresponding
     * Locale object from the 'supportedLocales' list that matches that language.
     * If no language match is found, it defaults to {@code Locale.ENGLISH}.
     *
     * @param acceptableLocales A list of Locale objects the user prefers, in order of preference.
     * @param supportedLocales  A list of Locale objects that the application supports.
     * @return The first supported Locale object whose language matches an acceptable language,
     * or {@code Locale.ENGLISH} if no match is found.
     */
    public static Locale findLanguageOnlyMatch(
            List<Locale> acceptableLocales,
            List<Locale> supportedLocales) {

        // 1. Create a Set of supported language strings for efficient lookup.
        //    Example: From [en-US, fr-FR, de] -> { "en", "fr", "de" }
        Set<String> supportedLanguages = supportedLocales.stream()
                .map(Locale::getLanguage)
                .collect(Collectors.toSet());

        // 2. Stream through acceptable locales to find the first language match.
        //    Then, find the first full Locale object from supported locales that corresponds to it.
        return acceptableLocales.stream()
                .map(Locale::getLanguage) // Extract language string (e.g., "en" from "en-CA")
                .filter(supportedLanguages::contains) // Keep only languages that are in our supported set
                .findFirst() // Get the first *language string* that matches (e.g., "en")
                .flatMap(matchedLanguage -> // Once a matching language string is found...
                    // ...find the first actual Locale object from the 'supportedLocales' list
                    // that has this matching language.
                    supportedLocales.stream()
                        .filter(supported -> supported.getLanguage().equals(matchedLanguage))
                        .findFirst()
                )
                .orElse(Locale.ENGLISH); // If no match is found anywhere in the pipeline, return Locale.ENGLISH
    }

	/**
	 * Gets the attribute.
	 *
	 * @param s the s
	 * @return the attribute
	 */
	public static Object getAttribute(String s) {
		return getCurrent().getAttributes().get(s);
	}

	public static OwlcmsSession getCurrent() {
		VaadinRequest request = VaadinRequest.getCurrent();

		if (request == null) {
			// Called from a background thread (no VaadinRequest). Prefer the
			// OwlcmsSession stored in the InheritableThreadLocal so background
			// work inherits the user's session.
			OwlcmsSession tl = OwlcmsSessionThreadLocal.get();
			if (tl != null) {
				return tl;
			}
			// No OwlcmsSession found in thread-local storage, create a default singleton
			// This can happen in background threads not spawned from an HTTP request context.
			// logger.debug("Background thread {} has no ThreadLocal OwlcmsSession, using singleton", 
			// 	Thread.currentThread().getName());
			if (owlcmsSessionSingleton == null) {
				owlcmsSessionSingleton = new OwlcmsSession();
			}
			return owlcmsSessionSingleton;
		}

		var httpSession = request.getWrappedSession();
		if (httpSession != null) {
			OwlcmsSession owlcmsSession = (OwlcmsSession) httpSession.getAttribute("owlcmsSession");
			if (owlcmsSession == null) {
				owlcmsSession = new OwlcmsSession();
				httpSession.setAttribute("owlcmsSession", owlcmsSession);
			}

			// create / refresh an InheritableThreadLocal OwlcmsSession for use in background threads
			// Only set if different from what's already stored
			OwlcmsSession tlValue = OwlcmsSessionThreadLocal.get();
			if (tlValue != owlcmsSession) {
				OwlcmsSessionThreadLocal.set(owlcmsSession);
			}

			return owlcmsSession;
		} else {
			throw new RuntimeException("no Vaadin session");
		}
	}

	public static FieldOfPlay getFop() {
		FieldOfPlay fop = (FieldOfPlay) getAttribute(FOP);
		if (fop == null) {
			logger.debug("getFop() returned null for thread: {}", Thread.currentThread().getName());
		}
		return fop;
	}

	public static String getFopLoggingName() {
		FieldOfPlay fop = getFop();
		if (fop != null) {
			return FieldOfPlay.getLoggingName(fop);
		} else {
			return "-";
		}
	}

	public static String getFopNameIfMultiple() {
		if (OwlcmsFactory.getFOPs().size() > 1) {
			FieldOfPlay fop;
			if ((fop = getFop()) != null) {
				return " " + fop.getName();
			} else {
				return null;
			}
		} else {
			return "";
		}
	}

	public static Locale getLocale() {
		return computeLocale();
	}

	public static QueryParameters getRequestedQueryParameters() {
		return (QueryParameters) getAttribute(QUERY_PARAMETERS);
	}

	public static String getRequestedUrl() {
		return (String) getAttribute(REQUESTED_URL);
	}

	public static void invalidate() {
		VaadinSession currentVaadinSession = VaadinSession.getCurrent();
		currentVaadinSession.getSession().invalidate();
	}

	public static boolean isAuthenticated() {
		return Boolean.TRUE.equals(getAttribute(AUTHENTICATED));
	}

	public static boolean isDisplayAuthenticated() {
		return isAuthenticated() || Boolean.TRUE.equals(getAttribute(DISPLAY_AUTHENTICATED));
	}

	/**
	 * Sets the attribute.
	 *
	 * @param s the s
	 * @param o the o
	 */
	public static void setAttribute(String s, Object o) {
		// Always apply to the canonical current OwlcmsSession
		OwlcmsSession current = getCurrent();
		if (o == null) {
			current.getAttributes().remove(s);
		} else {
			current.getAttributes().put(s, o);
		}

		// Also mirror into the thread-local OwlcmsSession (useful for background threads)
		try {
			OwlcmsSession tl = OwlcmsSessionThreadLocal.get();
			if (tl != null && tl != current) {
				if (o == null) {
					tl.getAttributes().remove(s);
				} else {
					tl.getAttributes().put(s, o);
				}
			}
		} catch (Throwable ignore) {
			// be defensive — don't fail when threadlocal isn't available
		}
	}

	public static void setAuthenticated(boolean isAuthenticated) {
		setAttribute(AUTHENTICATED, isAuthenticated);
	}

	public static void setDisplayAuthenticated(boolean b) {
		setAttribute(DISPLAY_AUTHENTICATED, b);
	}

	public static void setFop(FieldOfPlay fop) {
		// logger.debug("setFop {} from {}", (fop != null ? fop.getName() : null), LoggerUtils.whereFrom());
		setAttribute(FOP, fop);
	}

	public static void setRequestedQueryParameters(QueryParameters queryParameters) {
		setAttribute(QUERY_PARAMETERS, queryParameters);
	}

	public static void setRequestedUrl(String url) {
		setAttribute(REQUESTED_URL, url);
	}

	public static void withFop(Consumer<FieldOfPlay> command) {
		FieldOfPlay fop = getFop();
		if (fop == null) {
			fop = OwlcmsFactory.getDefaultFOP();
		}
		if (fop != null) {
			command.accept(fop);
		}
	}

	private Properties attributes = new Properties();

	public OwlcmsSession() {
	}

	private Properties getAttributes() {
		return this.attributes;
	}

	public void setLocale(Locale locale) {
		//logger.debug("setLocale {}\n{}", locale, LoggerUtils.stackTrace());
		setAttribute(LOCALE, locale);
	}

}
