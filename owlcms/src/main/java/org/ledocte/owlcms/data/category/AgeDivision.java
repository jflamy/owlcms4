/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.category;

import java.util.Arrays;
import java.util.Collection;

/**
 * The Enum AgeDivision.
 */
public enum AgeDivision {
    
    /** The default. */
    DEFAULT, 
 /** The senior. */
 SENIOR, 
 /** The junior. */
 JUNIOR, 
 /** The youth. */
 YOUTH, 
 /** The kids. */
 KIDS, 
 /** The masters. */
 MASTERS, 
 /** The traditional. */
 TRADITIONAL, 
 /** The a. */
 A, 
 /** The b. */
 B, 
 /** The c. */
 C, 
 /** The d. */
 D;

//    @Override
//    public String toString() {
//        return (isDefault() ? "" : name().charAt(0) + name().substring(1).toLowerCase());
//    }

    /**
 * Gets the code.
 *
 * @return the code
 */
public String getCode() {
        return (isDefault() ? "" : name().substring(0,1).toLowerCase());
    }

    /**
     * Checks if is default.
     *
     * @return true, if is default
     */
    public boolean isDefault() {
        return this == DEFAULT;
    }

    /**
     * Gets the age division from code.
     *
     * @param code the code
     * @return the age division from code
     */
    static public AgeDivision getAgeDivisionFromCode(String code) {
        for (AgeDivision curAD : AgeDivision.values()) {
            if (code.equals(curAD.getCode())) {
                return curAD;
            }
        }
        return AgeDivision.DEFAULT;
    }

	/**
	 * Find all.
	 *
	 * @return the collection
	 */
	public static Collection<AgeDivision> findAll() {
		return Arrays.asList(AgeDivision.values());
	}
}
