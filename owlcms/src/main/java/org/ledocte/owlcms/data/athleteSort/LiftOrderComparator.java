/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.athleteSort;

import java.util.Comparator;

import org.ledocte.owlcms.data.athlete.Athlete;

/**
 * The Class LiftOrderComparator.
 */
public class LiftOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
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
