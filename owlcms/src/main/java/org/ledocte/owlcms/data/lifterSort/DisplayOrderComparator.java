/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.lifterSort;

import java.util.Comparator;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.competition.Competition;


/**
 * This comparator is used for the standard display board. It returns the same order throughout the competition.
 *
 * @author jflamy
 *
 */
public class DisplayOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        if (Competition.getCurrent().isMasters()) {
            compare = compareAgeGroup(lifter1, lifter2);
            if (compare != 0)
                return -compare;
        }

        if (Competition.getCurrent().isUseRegistrationCategory()) {
            compare = compareRegistrationCategory(lifter1, lifter2);
        } else {
            compare = compareCategory(lifter1, lifter2);
        }
        if (compare != 0)
            return compare;

        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareLastName(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareFirstName(lifter1, lifter2);
        if (compare != 0)
            return compare;

        return compare;
    }

}
