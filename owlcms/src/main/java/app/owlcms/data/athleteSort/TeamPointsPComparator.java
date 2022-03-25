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
import app.owlcms.data.category.Participation;
import ch.qos.logback.classic.Logger;

/**
 * This comparator sorts Participations within their team.
 *
 * @author jflamy
 */
public class TeamPointsPComparator extends AbstractLifterComparator implements Comparator<Participation> {
    final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamPointsPComparator.class);

    private Ranking rankingType;

    /**
     * Instantiates a new team ranking comparator.
     *
     * @param rankingType the ranking type
     */
    TeamPointsPComparator(Ranking rankingType) {
        this.rankingType = rankingType;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Participation p1, Participation p2) {
        int compare = 0;

        Athlete lifter1 = p1.getAthlete();
        Athlete lifter2 = p2.getAthlete();
        compare = compareClub(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = compareGender(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        compare = comparePointsOrder(p1, p2);
        if (compare != 0) {
            return -compare;
        }

        return compare;
    }

    /**
     * @param p1
     * @param p2
     * @return
     */
    private int comparePointsOrder(Participation p1, Participation p2) {
        switch (rankingType) {
        case SNATCH:
            return Integer.compare(p1.getSnatchPoints(), p2.getSnatchPoints());
        case CLEANJERK:
            return Integer.compare(p1.getCleanJerkPoints(), p2.getCleanJerkPoints());
        case TOTAL:
            final Integer totalPoints1 = p1.getTotalPoints();
            final Integer totalPoints2 = p2.getTotalPoints();
            final int compareTotal = totalPoints1.compareTo(totalPoints2);
            logger.trace(p1 + " " + totalPoints1 + " [" + compareTotal + "]" + p2 + " " + totalPoints2);
            return compareTotal;
        case CUSTOM:
            final Integer customPoints1 = p1.getCustomPoints();
            final Integer customPoints2 = p2.getCustomPoints();
            final int compareCustom = customPoints1.compareTo(customPoints2);
            logger.trace(p1 + " " + customPoints1 + " [" + compareCustom + "]" + p2 + " " + customPoints2);
            return compareCustom;
        case SNATCH_CJ_TOTAL:
            final Integer combinedPoints1 = p1.getCombinedPoints();
            final Integer combinedPoints2 = p2.getCombinedPoints();
            final int compareCombined = combinedPoints1.compareTo(combinedPoints2);
            logger.trace(
                    p1 + " " + combinedPoints1 + " [" + compareCombined + "]" + p2 + " " + combinedPoints2);
            return compareCombined;
        default:
            break;
        }

        return 0;
    }

}
