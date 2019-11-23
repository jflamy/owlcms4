/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.init;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Store the current user's settings and choices, across the multiple pages that
 * may be opened.
 *
 * This class is either stored in a the Vaadin session shared between pages, or
 * used as a singleton for testing.
 *
 * @author Jean-François Lamy
 */
public class OwlcmsSession {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsSession.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private static final String FOP = "fop";

    private static OwlcmsSession owlcmsSessionSingleton = null;

    /**
     * Gets the attribute.
     *
     * @param s the s
     * @return the attribute
     */
    public static Object getAttribute(String s) {
        return getCurrent().attributes.get(s);
    }

    public static OwlcmsSession getCurrent() {
        VaadinSession currentVaadinSession = VaadinSession.getCurrent();
        if (currentVaadinSession != null) {
            OwlcmsSession owlcmsSession = (OwlcmsSession) currentVaadinSession.getAttribute("owlcmsSession");
            if (owlcmsSession == null) {
                logger.trace("creating new OwlcmsSession {}", LoggerUtils.whereFrom());
                owlcmsSession = new OwlcmsSession();
                currentVaadinSession.setAttribute("owlcmsSession", owlcmsSession);
            }
            return owlcmsSession;
        } else {
            // Used for testing, return a singleton
            if (owlcmsSessionSingleton == null) {
                owlcmsSessionSingleton = new OwlcmsSession();
            }
            return owlcmsSessionSingleton;
        }
    }

    public static FieldOfPlay getFop() {
        return (FieldOfPlay) getAttribute(FOP);
    }

    /**
     * Copied from Vaadin {@link Component} to ensure consistent behavior.
     * {@link Translator} will enforce a language if the competition screens must
     * ignore browser settings
     *
     * @return
     */
    public static Locale getLocale() {
        UI currentUi = UI.getCurrent();
        Locale locale = currentUi == null ? null : currentUi.getLocale();
        if (locale == null) {
            List<Locale> locales = Translator.getAvailableLocales();
            if (locales != null && !locales.isEmpty()) {
                locale = locales.get(0);
            } else {
                locale = Locale.getDefault();
            }
        }
        return locale;
    }

    public static String getRequestedUrl() {
        return (String) getAttribute("requestedURL");
    }

    public static boolean isAuthenticated() {
        return Boolean.TRUE.equals(getAttribute("authenticated"));
    }

    /**
     * Sets the attribute.
     *
     * @param s the s
     * @param o the o
     */
    public static void setAttribute(String s, Object o) {
        getCurrent().attributes.put(s, o);
    }

    public static void setAuthenticated(boolean isAuthenticated) {
        setAttribute("authenticated", isAuthenticated);
    }

    public static void setFop(FieldOfPlay fop) {
        logger.trace("setFop {} from {}", (fop != null ? fop.getName() : null), LoggerUtils.whereFrom());
        setAttribute(FOP, fop);
    }

    public static void setRequestedUrl(String url) {
        setAttribute("requestedURL", url);
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

    private OwlcmsSession() {
    }
}
