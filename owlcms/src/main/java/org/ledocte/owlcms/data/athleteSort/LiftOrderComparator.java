/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.athleteSort;

import java.util.Comparator;

import org.ledocte.owlcms.data.athlete.Athlete;

public class LiftOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare;

        // a Athlete that has the boolean flag "forceAsFirst" collates smallest
        // by definition
        compare = compareForcedAsFirst(lifter1, lifter2);
        if (compare != 0)
            return compare;

        // athletes who are done lifting are shown at bottom, in reverse total
        // number
        compare = compareFinalResults(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareLiftType(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareRequestedWeight(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareAttemptsDone(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareProgression(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareStartNumber(lifter1, lifter2);
        if (compare != 0)
            return compare;

        return compare;
    }

}
