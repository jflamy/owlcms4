/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.category;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * The Enum AgeDivision.
 */
public enum AgeDivision {

    DEFAULT, /* All ages */
    U, /** for age groups */
    MASTERS, /** 35+ (30+ in some federations) */
    IWF, 
    A, // custom divisions
    B,
    C,
    D;

    @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(AgeDivision.class);
    
    /**
     * Find all.
     *
     * @return the collection
     */
    public static Collection<AgeDivision> findAll() {
        return Arrays.asList(AgeDivision.values());
    }

    /**
     * Gets the age division from code.
     *
     * @param code the code
     * @return the age division from code
     */
    static public AgeDivision getAgeDivisionFromCode(String code) {
        if (code == null) return null;
        for (AgeDivision curAD : AgeDivision.values()) {
            if (code.equalsIgnoreCase(curAD.name())) {
                return curAD;
            }
        }
        return AgeDivision.DEFAULT;
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
}
