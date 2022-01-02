/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import app.owlcms.data.athlete.Athlete;

/**
 * This comparator is used to highlight the athletes that have lifted recently, and are likely to request changes to the
 * automatic progression. It simply sorts according to time stamp, if available. Else lot number is used.
 *
 * @author jflamy
 *
 */
public class LiftTimeStampComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    /**
     * Instantiates a new lift time stamp comparator.
     */
    public LiftTimeStampComparator() {
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = comparePreviousLiftOrder(lifter1, lifter2);
        if (compare != 0) {
            return -compare;
        }

        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        return compare;
    }

}
