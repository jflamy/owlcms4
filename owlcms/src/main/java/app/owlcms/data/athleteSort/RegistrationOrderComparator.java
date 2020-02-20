/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;

/**
 * This comparator is used for the technical meeting sheet. It is based on the registration category
 *
 * @author jflamy
 *
 */
public class RegistrationOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = compareGroupWeighInTime(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        // in the unlikely event that two competition groups are weighed-in at the same
        // time. Don't mix the groups.
        compare = compareGroup(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        if (Competition.getCurrent().isMasters()) {
            compare = compareAgeGroup(lifter1, lifter2);
            if (compare != 0) {
                return -compare;
            }
        }

        compare = ObjectUtils.compare(lifter1.getCategory(), lifter2.getCategory(), true); // null weighed after
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
