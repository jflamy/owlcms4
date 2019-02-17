/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
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
	
	/**
	 * Gets the attribute.
	 *
	 * @param s the s
	 * @return the attribute
	 */
	public Object getAttribute(String s) {
		return attributes.get(s);
	}
	
	/**
	 * Sets the attribute.
	 *
	 * @param s the s
	 * @param o the o
	 */
	public void setAttribute(String s, Object o) {
		attributes.put(s,o);
	}

}
