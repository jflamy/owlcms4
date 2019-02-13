package org.ledocte.owlcms;

import java.util.Properties;

/**
 * Store the current user's settings and choices, across the multiple pages that may be opened.
 * 
 * This class is either stored in a http session, or used directly for testing.
 * * A Vaadin page would get it from 
 * 
 * @author owlcms
 */
public class OwlcmsSession {
	private Properties attributes = new Properties();
	
	public Object getAttribute(String s) {
		return attributes.get(s);
	}
	
	public void setAttribute(String s, Object o) {
		attributes.put(s,o);
	}

}
