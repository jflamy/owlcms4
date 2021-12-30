/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;

/**
 * This comparator is used for the standard display board. It returns the same order throughout the competition.
 *
 * @author jflamy
 *
 */
public class DisplayOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        if (Competition.getCurrent().isMasters()) {
            compare = compareAgeGroup(lifter1, lifter2);
            if (compare != 0) {
                return -compare;
            }
        }

        compare = compareCategory(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = compareLastName(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = compareFirstName(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        return compare;
    }

}
