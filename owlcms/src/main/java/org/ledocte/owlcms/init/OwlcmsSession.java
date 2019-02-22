/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.init;

import java.util.Properties;

import com.vaadin.flow.server.VaadinSession;

/**
 * Store the current user's settings and choices, across the multiple pages that may be opened.
 * 
 * This class is either stored in a the Vaadin session shared between pages, or used as a singleton for testing.
 * 
 * @author Jean-François Lamy
 */
public class OwlcmsSession {
	
	private OwlcmsSession() {}
	
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

}
