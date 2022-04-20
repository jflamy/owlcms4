/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import java.util.Collection;
import java.util.EnumSet;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * The Enum AgeDivision.
 *
 * Divisions are listed in registration preference order.
 */
public enum AgeDivision {

    /* the divisions are listed in preference order, from more specific to more generic */
    MASTERS,    /** 35+ (30+ in some federations) */
    U,    /** for age groups */
    OLY,
    IWF,
    DEFAULT, /* All ages */
    SPECIAL;

    @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(AgeDivision.class);

    /**
     * Find all.
     *
     * @return the collection
     */
    public static Collection<AgeDivision> findAll() {
        return EnumSet.of(AgeDivision.DEFAULT, AgeDivision.IWF, AgeDivision.U, AgeDivision.MASTERS);
        // return Arrays.asList(AgeDivision.values());
    }

    /**
     * Gets the age division from code.
     *
     * @param code the code
     * @return the age division from code
     */
    static public AgeDivision getAgeDivisionFromCode(String code) {
        if (code == null) {
            return null;
        }
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
