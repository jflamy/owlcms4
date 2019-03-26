/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.init;

import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.data.competition.Competition;
import app.owlcms.state.FieldOfPlayState;
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
	
	private final static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsSession.class);
	static {
		logger.setLevel(Level.DEBUG);
	}
	
	private static final String FOP = "fop";

	private OwlcmsSession() {
	}
	
	private static OwlcmsSession owlcmsSessionSingleton = null;

	public static OwlcmsSession getCurrent() {
		VaadinSession currentVaadinSession = VaadinSession.getCurrent();
		if (currentVaadinSession != null) {
			OwlcmsSession owlcmsSession = (OwlcmsSession) currentVaadinSession.getAttribute("owlcmsSession");
			if (owlcmsSession == null) {
				owlcmsSession = new OwlcmsSession();
				currentVaadinSession.setAttribute("owlcmsSession", owlcmsSession);
			}
			return owlcmsSession;
		} else {
			// Used for testing, return a singleton
			if (owlcmsSessionSingleton == null) {
				owlcmsSessionSingleton =  new OwlcmsSession();
			}
			return owlcmsSessionSingleton;
		}
	}
	
	private Properties attributes = new Properties();
	
	/**
	 * Gets the attribute.
	 *
	 * @param s the s
	 * @return the attribute
	 */
	public static Object getAttribute(String s) {
		return getCurrent().attributes.get(s);
	}
	
	/**
	 * Sets the attribute.
	 *
	 * @param s the s
	 * @param o the o
	 */
	public static void setAttribute(String s, Object o) {
		getCurrent().attributes.put(s,o);
	}

	public static void withFop(Consumer<FieldOfPlayState> command) {
		FieldOfPlayState fop = getFop();
		if (fop == null) {
			fop = OwlcmsFactory.getDefaultFOP();
		}
		if (fop != null) {
			command.accept(fop);	
		}
	}

	public static FieldOfPlayState getFop() {
		return (FieldOfPlayState) getAttribute(FOP);
	}
	
	public static void setFop(FieldOfPlayState fop) {
		logger.debug("setFop {} from {}", (fop != null ? fop.getName() : null), LoggerUtils.whereFrom());
		setAttribute(FOP, fop);
	}
	
	public static Locale getLocale() {
		UI u = UI.getCurrent();
		if (u != null) {
			return u.getLocale();
		}
		VaadinSession v = VaadinSession.getCurrent();
		if (v != null) {
			return v.getLocale();
		} 
		Competition c = Competition.getCurrent();
		if (c != null) {
			return c.getDefaultLocale();
		}
		return Locale.ENGLISH;
	}
}
