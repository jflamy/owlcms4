/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.Collection;
import java.util.Set;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * The Enum Championship.
 *
 * Divisions are listed in registration preference order.
 */
public class Championship implements Comparable<Championship> {

//    /*
//     * the divisions are listed in preference order, from more specific to more generic
//     */
//	MASTERS,
//	/** 35+ (30+ in some federations) */
//	U,
//	/** for age groups */
//	OLY,
//	IWF,
//	DEFAULT, /* All ages */
//	SPECIAL;

	@SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(Championship.class);
	public static final Championship IWF = new Championship("IWF");
	public static final Championship DEFAULT = new Championship("DEFAULT");
	public static final Championship MASTERS = new Championship("MASTERS");
	public static final Championship U = new Championship("U");
	private String code;

	public Championship(String string) {
		this.code = string;
	}

	/**
	 * Find all.
	 *
	 * @return the collection
	 */
	public static Collection<Championship> findAll() {
		return Set.of(Championship.DEFAULT, Championship.IWF, Championship.U, Championship.MASTERS);
		// return Arrays.asList(Championship.values());
	}

	/**
	 * Gets the age division from code.
	 *
	 * @param code the code
	 * @return the age division from code
	 */
	static public Championship getAgeDivisionFromCode(String code) {
		if (code == null) {
			return null;
		}
		for (Championship curAD : Championship.values()) {
			if (code.equalsIgnoreCase(curAD.name())) {
				return curAD;
			}
		}
		return Championship.DEFAULT;
	}

	public String name() {
		return code;
	}

	public static Championship[] values() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public String getCode() {
		return (isDefault() ? "" : name().substring(0, 1).toLowerCase());
	}

	/**
	 * Checks if is default.
	 *
	 * @return true, if is default
	 */
	public boolean isDefault() {
		return this == DEFAULT;
	}

	@Override
	public int compareTo(Championship o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static Championship valueOf(String ageDivisionName) {
		// TODO Auto-generated method stub
		return null;
	}
}
