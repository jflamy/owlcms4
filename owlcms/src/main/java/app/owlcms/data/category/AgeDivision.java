/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.data.category;

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
