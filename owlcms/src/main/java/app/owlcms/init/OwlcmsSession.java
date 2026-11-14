/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

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

	/** Language and country pairs for which Java has date and number formats. */
	private static final Set<Locale> FORMATTED_LOCALES = Set.of(DateFormat.getAvailableLocales());

	/**
	 * The language of a page, decided on the UI thread when navigating (see AppShell). In order: the language chosen
	 * with this session's menu, the database default (both used as chosen), the first translated language of the
	 * browser in the browser's order, English. The browser's language keeps its country when Java has formats for that
	 * pair (e.g. en_GB), even without a translation for that country. Translations read the result from the UI,
	 * through {@link Translator#getLocale()}.
	 */
	public static Locale resolvePageLocale() {
		Locale chosen = (Locale) getAttribute(LOCALE);
		if (chosen != null) {
			return chosen;
		}

		List<Locale> browserLocales = browserLocales();
		Locale forcedLocale = Translator.getForcedLocale();
		if (forcedLocale != null) {
			return forcedLocale;
		}

		if (browserLocales.isEmpty()) {
			return Locale.ENGLISH;
		}
		return browserPreference(browserLocales, Translator.getAvailableLocales());
	}

	/**
	 * The first browser language, in the browser's order, that has translations: the exact translated locale when
	 * there is one (es_EC), else the browser's own pair when Java has formats for it (es_AR), else the generic
	 * translated language. English when no browser language is translated.
	 */
	static Locale browserPreference(List<Locale> browserLocales, List<Locale> translated) {
		for (Locale browser : browserLocales) {
			Locale pair = languageAndCountry(browser);
			if (translated.contains(pair)) {
				return pair;
			}
			Optional<Locale> sameLanguage = translated.stream()
			        .filter(t -> t.getLanguage().equals(pair.getLanguage()))
			        .min(Comparator.comparing(t -> !t.getCountry().isEmpty()));
			if (sameLanguage.isEmpty()) {
				continue;
			}
			if (!pair.getCountry().isEmpty() && FORMATTED_LOCALES.contains(pair)) {
				return pair;
			}
			return withBrowserCountry(sameLanguage.get(), browserLocales);
		}
		return Locale.ENGLISH;
	}

	/**
	 * The entries of the language menu, grouped by language in the order of the translations. In each group: the
	 * generic language, then the browser's own variants of that language (in the browser's order), then the other
	 * variants. A browser variant is offered when Java has formats for it, even without a translation for that
	 * country; it is never listed twice. Called on the UI thread.
	 */
	public static List<Locale> languageMenuChoices(List<Locale> translated) {
		return languageMenuChoices(translated, browserLocales());
	}

	static List<Locale> languageMenuChoices(List<Locale> translated, List<Locale> browserLocales) {
		Map<String, List<Locale>> byLanguage = new LinkedHashMap<>();
		for (Locale l : translated) {
			byLanguage.computeIfAbsent(l.getLanguage(), k -> new ArrayList<>()).add(l);
		}
		List<Locale> browserPairs = new ArrayList<>();
		for (Locale l : browserLocales) {
			Locale pair = languageAndCountry(l);
			if (!pair.getCountry().isEmpty() && byLanguage.containsKey(pair.getLanguage())
			        && (translated.contains(pair) || FORMATTED_LOCALES.contains(pair))
			        && !browserPairs.contains(pair)) {
				browserPairs.add(pair);
			}
		}
		List<Locale> choices = new ArrayList<>();
		for (Map.Entry<String, List<Locale>> group : byLanguage.entrySet()) {
			group.getValue().stream().filter(l -> l.getCountry().isEmpty()).forEach(choices::add);
			browserPairs.stream().filter(l -> l.getLanguage().equals(group.getKey())).forEach(choices::add);
			group.getValue().stream().filter(l -> !l.getCountry().isEmpty() && !browserPairs.contains(l))
			        .forEach(choices::add);
		}
		return choices;
	}

	/** A match without a country takes the country of the browser's own entry for that language, if Java formats it. */
	private static Locale withBrowserCountry(Locale locale, List<Locale> browserLocales) {
		if (!locale.getCountry().isBlank()) {
			return locale;
		}
		for (Locale browser : browserLocales) {
			Locale pair = languageAndCountry(browser);
			if (pair.getLanguage().equals(locale.getLanguage()) && !pair.getCountry().isEmpty()
			        && FORMATTED_LOCALES.contains(pair)) {
				return pair;
			}
		}
		return locale;
	}

	@SuppressWarnings("deprecation")
	private static Locale languageAndCountry(Locale l) {
		return new Locale(l.getLanguage(), l.getCountry());
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
