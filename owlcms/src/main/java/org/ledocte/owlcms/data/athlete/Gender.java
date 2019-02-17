/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.athlete;

/**
 * The Enum Gender.
 */
public enum Gender {
    
    /** The f. */
    F, 
 /** The m. */
 M, 
 /** The unkown. */
 UNKOWN;

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
    	if (this == UNKOWN) {
    		return ("?");
    	}
        return name().toUpperCase();
    }
}
