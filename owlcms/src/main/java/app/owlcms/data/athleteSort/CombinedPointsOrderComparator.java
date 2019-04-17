/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.competition.Competition;
import ch.qos.logback.classic.Logger;

/**
 * Medal ordering.
 *
 * @author jflamy
 *
 */
public class CombinedPointsOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    /** The Constant logger. */
    final static Logger logger = (Logger) LoggerFactory.getLogger(CombinedPointsOrderComparator.class);

    private Ranking rankingType;

    /**
     * Instantiates a new combined points order comparator.
     *
     * @param rankingType the ranking type
     */
    public CombinedPointsOrderComparator(Ranking rankingType) {
        this.rankingType = rankingType;
    }

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        switch (rankingType) {
        case SNATCH:
            return compareSnatchResultOrder(lifter1, lifter2);
        case CLEANJERK:
            return compareCleanJerkResultOrder(lifter1, lifter2);
        case TOTAL:
            return compareTotalResultOrder(lifter1, lifter2);
        case SINCLAIR:
            return compareSinclairResultOrder(lifter1, lifter2);
        case ROBI:
            return compareRobiResultOrder(lifter1, lifter2);
        case CUSTOM:
        	return compareCustomScore(lifter1, lifter2);
		default:
			break;
        }

        return compare;
    }

    /**
     * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareTotalResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        if (Competition.getCurrent().isUseRegistrationCategory()) {
            compare = compareRegistrationCategory(lifter1, lifter2);
        } else {
            compare = compareCategory(lifter1, lifter2);
        }
        if (compare != 0)
            return compare;

        compare = compareTotal(lifter1, lifter2);
        if (compare != 0)
            return -compare; // we want reverse order - smaller
                             // comes after

        compare = compareBodyWeight(lifter1, lifter2);
        if (compare != 0)
            return compare; // smaller Athlete wins

        compare = compareBestCleanJerk(lifter1, lifter2);
        if (compare != 0)
            return compare; // smallest clean and jerk wins (i.e.
                            // best snatch wins !)

        compare = compareBestCleanJerkAttemptNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // earlier best attempt wins

        // note that when comparing total, we do NOT consider snatch. At this
        // stage, both athletes have
        // done the same weight at the same attempt. We are trying to determine
        // who did the attempt first.
        // So if the best attempt was the first one, we must NOT consider snatch
        // results when doing this determination
        compare = comparePreviousAttempts(lifter1.getBestResultAttemptNumber(), true, lifter1, lifter2);
        if (compare != 0)
            return compare; // compare attempted weights (prior to
                            // best attempt), smaller first

        // The IWF referee examination example shows a case where the Athlete in
        // the earlier group is not
        // given the ranking; according to the answers, lot number alone is
        // used.
        // compare = compareGroup(lifter1, lifter2);
        // if (compare != 0) return compare; // if split groups, smallest group
        // wins -- lifted earlier

        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // if equality within a group,
                            // smallest lot number wins

        return compare;
    }

    /**
     * Compare snatch result order.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareSnatchResultOrder(Athlete lifter1, Athlete lifter2) {
        boolean trace =
                // (
                // (lifter1.getFirstName().equals("Yvon") &&
                // lifter2.getFirstName().equals("Anthony"))
                // ||
                // (lifter2.getFirstName().equals("Yvon") &&
                // lifter1.getFirstName().equals("Anthony"))
                // );
                false;
        int compare = 0;

        if (trace)
            logger.trace("lifter1 {};  lifter2 {}", lifter1.getFirstName(), lifter2.getFirstName());

        if (Competition.getCurrent().isUseRegistrationCategory()) {
            compare = compareRegistrationCategory(lifter1, lifter2);
        } else {
            compare = compareCategory(lifter1, lifter2);
        }
        if (trace)
            logger.trace("compareCategory {}", compare);
        if (compare != 0)
            return compare;

        compare = compareBestSnatch(lifter1, lifter2);
        if (trace)
            logger.trace("compareBestSnatch {}", compare);
        if (compare != 0)
            return -compare; // smaller snatch is less good

        compare = compareBodyWeight(lifter1, lifter2);
        if (trace)
            logger.trace("compareBodyWeight {}", compare);
        if (compare != 0)
            return compare; // smaller Athlete wins

        compare = compareBestSnatchAttemptNumber(lifter1, lifter2);
        if (trace)
            logger.trace("compareBestSnatchAttemptNumber {}", compare);
        if (compare != 0)
            return compare; // earlier best attempt wins

        compare = comparePreviousAttempts(lifter1.getBestSnatchAttemptNumber(), false, lifter1, lifter2);
        if (trace)
            logger.trace("comparePreviousAttempts {}", compare);
        if (compare != 0)
            return compare; // compare attempted weights (prior to
                            // best attempt), smaller first

        // The referee examination example shows a case where the Athlete in the
        // earlier group is not
        // given the ranking.
        // compare = compareGroup(lifter1, lifter2);
        // if (trace) logger.trace("compareGroup {}",compare);
        // if (compare != 0) return compare; // if split groups, smallest group
        // wins -- lifted earlier

        compare = compareLotNumber(lifter1, lifter2);
        if (trace)
            logger.trace("compareLotNumber {}", compare);
        if (compare != 0)
            return compare; // if equality within a group,
                            // smallest lot number wins

        return compare;
    }

    /**
     * Compare clean jerk result order.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareCleanJerkResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        if (Competition.getCurrent().isUseRegistrationCategory()) {
            compare = compareRegistrationCategory(lifter1, lifter2);
        } else {
            compare = compareCategory(lifter1, lifter2);
        }
        if (compare != 0)
            return compare;

        compare = compareBestCleanJerk(lifter1, lifter2);
        if (compare != 0)
            return -compare; // smaller is less good

        compare = compareBodyWeight(lifter1, lifter2);
        if (compare != 0)
            return compare; // smaller Athlete wins

        compare = compareBestCleanJerkAttemptNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // earlier best attempt wins

        compare = comparePreviousAttempts(lifter1.getBestCleanJerkAttemptNumber(), true, lifter1, lifter2);
        if (compare != 0)
            return compare; // compare attempted weights (prior to
                            // best attempt), smaller first

        // The referee examination example shows a case where the Athlete in the
        // earlier group is not
        // given the ranking.
        // compare = compareGroup(lifter1, lifter2);
        // if (compare != 0) return compare; // if split groups, smallest group
        // wins -- lifted earlier

        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // if equality within a group,
                            // smallest lot number wins

        return compare;
    }

    /**
     * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareSinclairResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = compareSinclair(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareBodyWeight(lifter1, lifter2);
        if (compare != 0)
            return compare; // smaller Athlete wins

        compare = compareBestCleanJerk(lifter1, lifter2);
        if (compare != 0)
            return compare; // smallest clean and jerk wins (i.e.
                            // best snatch wins !)

        compare = compareBestCleanJerkAttemptNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // earlier best attempt wins

        // note that when comparing total, we do NOT consider snatch. At this
        // stage, both athletes have
        // done the same weight at the same attempt. We are trying to determine
        // who did the attempt first.
        // So if the best attempt was the first one, we must NOT consider snatch
        // results when doing this determination
        compare = comparePreviousAttempts(lifter1.getBestResultAttemptNumber(), true, lifter1, lifter2);
        if (compare != 0)
            return compare; // compare attempted weights (prior to
                            // best attempt), smaller first

        // The IWF referee examination example shows a case where the Athlete in
        // the earlier group is not
        // given the ranking; according to the answers, lot number alone is
        // used.
        // compare = compareGroup(lifter1, lifter2);
        // if (compare != 0) return compare; // if split groups, smallest group
        // wins -- lifted earlier

        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // if equality within a group,
                            // smallest lot number wins

        return compare;
    }

    /**
     * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareRobiResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = compareRobi(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareBodyWeight(lifter1, lifter2);
        if (compare != 0)
            return compare; // smaller Athlete wins

        compare = compareBestCleanJerk(lifter1, lifter2);
        if (compare != 0)
            return compare; // smallest clean and jerk wins (i.e.
                            // best snatch wins !)

        compare = compareBestCleanJerkAttemptNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // earlier best attempt wins

        // note that when comparing total, we do NOT consider snatch. At this
        // stage, both athletes have
        // done the same weight at the same attempt. We are trying to determine
        // who did the attempt first.
        // So if the best attempt was the first one, we must NOT consider snatch
        // results when doing this determination
        compare = comparePreviousAttempts(lifter1.getBestResultAttemptNumber(), true, lifter1, lifter2);
        if (compare != 0)
            return compare; // compare attempted weights (prior to
                            // best attempt), smaller first

        // The IWF referee examination example shows a case where the Athlete in
        // the earlier group is not
        // given the ranking; according to the answers, lot number alone is
        // used.
        // compare = compareGroup(lifter1, lifter2);
        // if (compare != 0) return compare; // if split groups, smallest group
        // wins -- lifted earlier

        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0)
            return compare; // if equality within a group,
                            // smallest lot number wins

        return compare;
    }
}
