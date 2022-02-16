/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Logger;

/**
 * This comparator sorts athletes within their team.
 *
 * @author jflamy
 */
public class TeamPointsComparator extends AbstractLifterComparator implements Comparator<Athlete> {
    final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamPointsComparator.class);

    private Ranking rankingType;

    /**
     * Instantiates a new team ranking comparator.
     *
     * @param rankingType the ranking type
     */
    TeamPointsComparator(Ranking rankingType) {
        this.rankingType = rankingType;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = compareClub(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = compareGender(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = comparePointsOrder(lifter1, lifter2);
        if (compare != 0) {
            return -compare;
        }

        return compare;
    }

    /**
     * @param lifter1
     * @param lifter2
     * @return
     */
    private int comparePointsOrder(Athlete lifter1, Athlete lifter2) {
        switch (rankingType) {
        case SNATCH:
            return Integer.compare(lifter1.getSnatchPoints(), lifter2.getSnatchPoints());
        case CLEANJERK:
            return Integer.compare(lifter1.getCleanJerkPoints(), lifter2.getCleanJerkPoints());
        case TOTAL:
            final Integer totalPoints1 = lifter1.getTotalPoints();
            final Integer totalPoints2 = lifter2.getTotalPoints();
            final int compareTotal = totalPoints1.compareTo(totalPoints2);
            logger.trace(lifter1 + " " + totalPoints1 + " [" + compareTotal + "]" + lifter2 + " " + totalPoints2);
            return compareTotal;
        case CUSTOM:
            final Integer customPoints1 = lifter1.getCustomPoints();
            final Integer customPoints2 = lifter2.getCustomPoints();
            final int compareCustom = customPoints1.compareTo(customPoints2);
            logger.trace(lifter1 + " " + customPoints1 + " [" + compareCustom + "]" + lifter2 + " " + customPoints2);
            return compareCustom;
        case SNATCH_CJ_TOTAL:
            final Integer combinedPoints1 = lifter1.getCombinedPoints();
            final Integer combinedPoints2 = lifter2.getCombinedPoints();
            final int compareCombined = combinedPoints1.compareTo(combinedPoints2);
            logger.trace(
                    lifter1 + " " + combinedPoints1 + " [" + compareCombined + "]" + lifter2 + " " + combinedPoints2);
            return compareCombined;
        default:
            break;
        }

        return 0;
    }

}
