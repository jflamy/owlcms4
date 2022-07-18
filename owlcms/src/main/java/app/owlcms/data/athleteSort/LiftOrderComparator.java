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
 * The Class LiftOrderComparator.
 */
public class LiftOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare;

        // a Athlete that has the boolean flag "forceAsFirst" collates smallest
        // by definition
        compare = compareForcedAsCurrent(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        // athletes who are done lifting are shown at bottom, in reverse total
        // number
        compare = compareFinalResults(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = compareLiftType(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        if (Competition.getCurrent().isRoundRobinOrder()) {
            compare = compareAttemptsDone(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        }

        if (Competition.getCurrent().isGenderOrder()) {
            compare = compareGender(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        }
        
        if (Competition.getCurrent().isFixedOrder()) {
            compare = compareLotNumber(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        }

        compare = compareRequestedWeight(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        // if round-robin, attempts done has already been taken into account above so skip.
        if (!Competition.getCurrent().isRoundRobinOrder()) {
            compare = compareAttemptsDone(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        }

        compare = compareProgression(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = compareStartNumber(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        return compare;
    }

}
