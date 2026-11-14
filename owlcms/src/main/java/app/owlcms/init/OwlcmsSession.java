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

import app.owlcms.access.AccessPolicy;
import app.owlcms.access.Principal;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
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
	private static final String PRINCIPAL = "principal";
	private static final String FOP = "fop";
	public static final String LOCALE = "locale";
	private final static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsSession.class);
	private static OwlcmsSession owlcmsSessionSingleton = null;

	static {
		logger.setLevel(Level.INFO);
	}

	/**
	 * The language of a page, decided on the UI thread when navigating (see AppShell). In order: the language chosen
	 * with this session's dropdown, the database default (completed with the browser's country when it has none), the
	 * browser's language, English. Translations read the result from the UI, through {@link Translator#getLocale()}.
	 */
	@SuppressWarnings("deprecation")
	public static Locale resolvePageLocale() {
		Locale chosen = (Locale) getAttribute(LOCALE);
		if (chosen != null) {
			return chosen;
		}

		List<Locale> browserLocales = browserLocales();
		Locale forcedLocale = Translator.getForcedLocale();
		if (forcedLocale != null) {
			if (!forcedLocale.getCountry().isBlank()) {
				return forcedLocale;
			}
			String country = getFirstCountryFromLocales(browserLocales);
			return country != null ? new Locale(forcedLocale.getLanguage(), country) : forcedLocale;
		}

		if (browserLocales.isEmpty()) {
			return Locale.ENGLISH;
		}
		List<Locale> availableLocales = Translator.getAvailableLocales();
		Locale match;
		Optional<Locale> found = findMatchingLocale(browserLocales, availableLocales);
		if (found.isEmpty()) {
			// Firefox does not add es if user has es_AR, so we check for es only
			match = findLanguageOnlyMatch(browserLocales, availableLocales);
		} else {
			match = found.get();
		}
		if (match.getCountry().isBlank()) {
			String country = getFirstCountryFromLocales(browserLocales);
			if (country == null) {
				country = Locale.getDefault().getCountry();
			}
			return new Locale(match.getLanguage(), country);
		}
		return match;
	}

	private static List<Locale> browserLocales() {
		// VaadinService.getCurrentRequest() may be null in some contexts; fall back to the UI locale.
		VaadinRequest currentRequest = VaadinService.getCurrentRequest();
		if (currentRequest != null) {
			return Collections.list(currentRequest.getLocales());
		}
		UI currentUi = UI.getCurrent();
		if (currentUi != null && currentUi.getLocale() != null) {
			return List.of(currentUi.getLocale());
		}
		return List.of();
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

	/** The language for translations in the current context; see {@link Translator#getLocale()}. */
	public static Locale getLocale() {
		return Translator.getLocale();
	}

	public static Principal getPrincipal() {
		return (Principal) getAttribute(PRINCIPAL);
	}

	public static void setPrincipal(Principal principal) {
		setAttribute(PRINCIPAL, principal);
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

	public static boolean computeDisplayAuthenticated() {
		return isAuthenticated() || isDisplayAuthenticated();
	}

	public static boolean isDisplayAuthenticated() {
		return Boolean.TRUE.equals(getAttribute(DISPLAY_AUTHENTICATED));
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
		Principal principal = VaadinSession.getCurrent() != null ? getPrincipal() : null;
		if (principal != null && fop != null) {
			List<String> platforms = OwlcmsFactory.getFOPs().stream().map(FieldOfPlay::getName).toList();
			if (!AccessPolicy.allowedSessionPlatforms(principal, platforms).contains(fop.getName())) {
				logger./**/warn("{}session FOP selection refused user={} requested={} {}",
				        FieldOfPlay.getLoggingName(fop), principal.username(), fop.getName(), LoggerUtils.whereFrom());
				return;
			}
		}
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

	/** Records the language chosen for this session; null returns to the default. Read by {@link #resolvePageLocale()}. */
	public void setLocale(Locale locale) {
		if (locale == null) {
			this.attributes.remove(LOCALE);
		} else {
			this.attributes.put(LOCALE, locale);
		}
	}

}
