/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.time.LocalDateTime;
import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Logger;

/**
 * Medal ordering.
 *
 * @author jflamy
 *
 */
public class WinningOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    /** The Constant logger. */
    final static Logger logger = (Logger) LoggerFactory.getLogger(WinningOrderComparator.class);

    private Ranking rankingType;

    private boolean absoluteOrder;

    /**
     * Instantiates a new winning order comparator.
     *
     * @param rankingType the ranking type
     */
    public WinningOrderComparator(Ranking rankingType, boolean absoluteOrder) {
        this.rankingType = rankingType;
        this.absoluteOrder = absoluteOrder;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        switch (rankingType) {
        case SNATCH:
            return compareSnatchResultOrder(lifter1, lifter2, absoluteOrder);
        case CLEANJERK:
            return compareCleanJerkResultOrder(lifter1, lifter2, absoluteOrder);
        case TOTAL:
            return compareTotalResultOrder(lifter1, lifter2, absoluteOrder);
        case CUSTOM:
            return compareCustomResultOrder(lifter1, lifter2, absoluteOrder);
        case ROBI:
            return compareRobiResultOrder(lifter1, lifter2);
        case CAT_SINCLAIR:
            return compareCatSinclairResultOrder(lifter1, lifter2);
        case BW_SINCLAIR:
            return compareSinclairResultOrder(lifter1, lifter2);
        case SMM:
            return compareSmmResultOrder(lifter1, lifter2);
//        case SINCLAIR:
//            if (Competition.getCurrent().isMasters()) {
//                return compareSmmResultOrder(lifter1, lifter2);
//            } else {
//                if (Competition.getCurrent().isUseCategorySinclair()) {
//                    return compareCategorySinclairResultOrder(lifter1, lifter2);
//                } else {
//                    return compareSinclairResultOrder(lifter1, lifter2);
//                }
//            }
        default:
            break;
        }
        return compare;
    }

    /**
     * Determine who ranks first. the Athlete who reached total first is ranked first.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareCatSinclairResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = compareCategorySinclair(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        return tieBreak(lifter1, lifter2, true);
    }

    /**
     * Compare clean jerk result order.
     *
     * @param lifter1       the lifter 1
     * @param lifter2       the lifter 2
     * @param absoluteOrder do not take categories into account
     * @return the int
     */
    public int compareCleanJerkResultOrder(Athlete lifter1, Athlete lifter2, boolean absoluteOrder) {
        int compare = 0;

        if (!absoluteOrder) {
            compare = compareCategory(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        }

        compare = compareBestCleanJerk(lifter1, lifter2);
        if (compare != 0) {
            return -compare; // smaller is less good
        }

        return tieBreak(lifter1, lifter2, Competition.getCurrent().isUseOldBodyWeightTieBreak());
    }

    /**
     * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
     *
     * This variant allows judges to award a score based on a formula, with bonuses or penalties, manually. Used for the
     * U12 championship in Quebec.
     *
     * @param lifter1       the lifter 1
     * @param lifter2       the lifter 2
     * @param absoluteOrder do not take category into account
     * @return the int
     */
    public int compareCustomResultOrder(Athlete lifter1, Athlete lifter2, boolean absoluteOrder) {
        int compare = 0;

        if (!absoluteOrder) {
            compare = ObjectUtils.compare(lifter1.getCategory(), lifter2.getCategory(), true);
            if (compare != 0) {
                return compare;
            }
        }

        compare = compareCustomScore(lifter1, lifter2);
        if (compare != 0) {
            return -compare; // we want reverse order - smaller comes after
        }

        compare = compareTotal(lifter1, lifter2);
        if (compare != 0) {
            return -compare; // we want reverse order - smaller comes after
        }

        return tieBreak(lifter1, lifter2, Competition.getCurrent().isUseOldBodyWeightTieBreak());
    }

    /**
     * Determine who ranks first on Robi points.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareRobiResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
        if (compare != 0) {
            return compare;
        }

        compare = compareRobi(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        // for robi, lighter Athlete that achieves same robi is better
        return tieBreak(lifter1, lifter2, true);
    }

    /**
     * Determine who ranks first on Sinclair points.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareSinclairResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;
        compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
        if (compare != 0) {
            return compare;
        }
        if ((lifter1 != null && lifter1.getAttemptsDone() <= 3)
                && (lifter2 != null && lifter2.getAttemptsDone() <= 3)) {
            // compare tentative sinclair
            compare = compareSinclairForDelta(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        } else {
            compare = compareSinclair(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        }
        // for sinclair, lighter Athlete that achieves same sinclair is better
        return tieBreak(lifter1, lifter2, true);
    }

    /**
     * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    public int compareSmmResultOrder(Athlete lifter1, Athlete lifter2) {
        int compare = 0;
        compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
        if (compare != 0) {
            return compare;
        }

        compare = compareSmm(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        return tieBreak(lifter1, lifter2, true);
    }

    /**
     * Compare snatch result order.
     *
     * @param lifter1       the lifter 1
     * @param lifter2       the lifter 2
     * @param absoluteOrder do not take categories into account
     * @return the int
     */
    public int compareSnatchResultOrder(Athlete lifter1, Athlete lifter2, boolean absoluteOrder) {
        boolean trace = false;
        int compare = 0;

        if (trace) {
            logger.trace("lifter1 {};  lifter2 {}", lifter1.getFirstName(), lifter2.getFirstName());
        }

        if (!absoluteOrder) {
            compare = compareCategory(lifter1, lifter2);
            if (trace) {
                logger.trace("compareCategory {}", compare);
            }
            if (compare != 0) {
                return compare;
            }
        }

        compare = compareBestSnatch(lifter1, lifter2);
        if (trace) {
            logger.trace("compareBestSnatch {}", compare);
        }
        if (compare != 0) {
            return -compare; // smaller snatch is less good
        }

        compare = compareCompetitionSessionTime(lifter1, lifter2);
        traceComparison("compareCompetitionSessionTime", lifter1, lifter2, compare);
        if (compare != 0) {
            return compare; // earlier group time wins
        }

        if (Competition.getCurrent().isUseOldBodyWeightTieBreak()) {
            compare = compareBodyWeight(lifter1, lifter2);
            if (trace) {
                logger.trace("compareBodyWeight {}", compare);
            }
            if (compare != 0) {
                return compare; // smaller Athlete wins
            }
        }

//        if (Competition.getCurrent().isMasters()) {
//            compare = compareBirthDate(lifter1, lifter2);
//            if (compare != 0) return -compare; // oldest wins
//        }

        compare = compareBestSnatchAttemptNumber(lifter1, lifter2);
        if (trace) {
            logger.trace("compareBestSnatchAttemptNumber {}", compare);
        }
        if (compare != 0) {
            return compare; // earlier best attempt wins
        }

        compare = comparePreviousAttempts(lifter1.getBestSnatchAttemptNumber(), false, lifter1, lifter2);
        if (trace) {
            logger.trace("comparePreviousAttempts {}", compare);
        }
        if (compare != 0) {
            return compare; // compare attempted weights (prior to
                            // best attempt), smaller first
        }

        compare = compareLotNumber(lifter1, lifter2);
        if (trace) {
            logger.trace("compareLotNumber {}", compare);
        }
        if (compare != 0) {
            return compare; // if equality within a group,
                            // smallest lot number wins
        }

        return compare;
    }

    /**
     * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
     *
     * @param lifter1       the lifter 1
     * @param lifter2       the lifter 2
     * @param absoluteOrder do not take categories into account
     * @return the int
     */
    public int compareTotalResultOrder(Athlete lifter1, Athlete lifter2, boolean absoluteOrder) {
        int compare = 0;

        if (!absoluteOrder) {
            compare = compareCategory(lifter1, lifter2);
            if (compare != 0) {
                return compare;
            }
        }

        compare = compareTotal(lifter1, lifter2);
        traceComparison("compareTotal", lifter1, lifter2, compare);
        if (compare != 0) {
            return -compare; // we want reverse order - smaller comes after
        }

        return tieBreak(lifter1, lifter2, Competition.getCurrent().isUseOldBodyWeightTieBreak());
    }

    /**
     * Compare competition session start times for two athletes. A null session time is considered to be at the
     * beginning of time, earlier than any non-null time.
     *
     * @param lifter1
     * @param lifter2
     * @return -1 if lifter1 was part of earlier group, 0 if same group, 1 if lifter1 lifted in later group
     */
    private int compareCompetitionSessionTime(Athlete lifter1, Athlete lifter2) {
        Group group1 = lifter1.getGroup();
        Group group2 = lifter2.getGroup();
        if (group1 == null && group2 == null) {
            return 0;
        }
        if (group1 == null) {
            return -1;
        }
        if (group2 == null) {
            return 1;
        }
        LocalDateTime competitionTime1 = group1.getCompetitionTime();
        LocalDateTime competitionTime2 = group2.getCompetitionTime();
        if (competitionTime1 == null && competitionTime2 == null) {
            return 0;
        }
        if (competitionTime1 == null) {
            return -1;
        }
        if (competitionTime2 == null) {
            return 1;
        }
        return competitionTime1.compareTo(competitionTime2);
    }

    /**
     * Processing shared between all coefficient-based rankings
     *
     * @param lifter1
     * @param lifter2
     * @return
     */
    private int tieBreak(Athlete lifter1, Athlete lifter2, boolean bodyWeightTieBreak) {
        int compare;

        compare = compareCompetitionSessionTime(lifter1, lifter2);
        traceComparison("compareCompetitionSessionTime", lifter1, lifter2, compare);
        if (compare != 0) {
            return compare; // earlier group time wins
        }

        if (bodyWeightTieBreak) {
            compare = compareBodyWeight(lifter1, lifter2);
            traceComparison("compareBodyWeight", lifter1, lifter2, compare);
            if (compare != 0) {
                return compare; // smaller Athlete wins
            }
        }

        // for total, must compare best clean and jerk value and smaller is better
        // because the total was reached earlier.
        // if this routine called to tiebreak cj ranking, the result will be 0 so this
        // test is harmless
        compare = compareBestCleanJerk(lifter1, lifter2);
        traceComparison("compareBestCleanJerk", lifter1, lifter2, compare);
        if (compare != 0) {
            return compare; // smaller cj, when total is the same, means total was reached earlier.
        }

        // same clean and jerk, earlier attempt wins
        compare = compareBestCleanJerkAttemptNumber(lifter1, lifter2);
        traceComparison("compareBestCleanJerkAttemptNumber", lifter1, lifter2, compare);
        if (compare != 0) {
            return compare; // earlier best attempt wins
        }

        // determine who lifted best clean and jerk first
        compare = comparePreviousAttempts(lifter1.getBestCleanJerkAttemptNumber(), true, lifter1, lifter2);
        traceComparison("comparePreviousAttempts", lifter1, lifter2, compare);
        if (compare != 0) {
            return compare; // compare attempted weights (prior to best attempt), smaller first
        }

        // if equality within a group, smallest lot number wins (same session, same
        // category, same weight, same attempt) -- smaller lot lifted first.
        compare = compareLotNumber(lifter1, lifter2);
        if (compare != 0) {
            return compare; // compare attempted weights (prior to best attempt), smaller first
        }

        // if no lot number, we get weird results. we need a stable comparison
        compare = ObjectUtils.compare(lifter1.getId(), lifter2.getId());
        return compare;

    }

    private void traceComparison(String where, Athlete lifter1, Athlete lifter2, int compare) {
        if (logger.isTraceEnabled()) {
            logger./**/warn("{} {} {} {}", where, lifter1, (compare < 0 ? "<" : (compare == 0 ? "=" : ">")), lifter2);
        }
    }

}
