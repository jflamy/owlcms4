/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.persistence.Entity;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import ch.qos.logback.classic.Logger;

/**
 * The Class AthleteSorter.
 *
 * @author jflamy
 * @since
 */

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
public class AthleteSorter implements Serializable {

    /**
     * The Enum Ranking.
     */
    public enum Ranking {
        SNATCH, CLEANJERK, TOTAL,
        /** combined (men + women). */
        COMBINED,
//        SINCLAIR, // cat, bw or smm depending on competition parameters
        CAT_SINCLAIR, // legacy Quebec federation, Sinclair computed at category boundary
        BW_SINCLAIR, // normal sinclair
        SMM, // Sinclair Malone-Meltzer
        ROBI, CUSTOM // custom score (e.g. technical merit for kids competition)

    }

    private static final long serialVersionUID = -3507146241019771820L;

    private static final Logger logger = (Logger) LoggerFactory.getLogger(AthleteSorter.class);

    /**
     * Assign ranks within each category. Provided list is left untouched.
     *
     * @param athletes the list of athletes to sort
     */
    public static void assignCategoryRanks(List<Athlete> athletes) {
        List<Athlete> sortedAthletes;
        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SNATCH);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.SNATCH);
        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CLEANJERK);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.CLEANJERK);
        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.TOTAL);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.TOTAL);
    }

    /**
     * Assign ranks, sequentially.
     *
     * @param sortedList  the sorted list
     * @param rankingType the ranking type
     */
    public static void assignCategoryRanks(List<Athlete> sortedList, Ranking rankingType) {
        Category prevCategory = null;

        RankSetter rt = new RankSetter();
        for (Athlete curLifter : sortedList) {
            Category curCategory = curLifter.getCategory();
            if (!equals(curCategory, prevCategory)) {
                // category boundary has been crossed
                logger.trace("category boundary crossed {}", curCategory);
                rt = new RankSetter();
            }

            if (!curLifter.isEligibleForIndividualRanking()) {
                // not eligible
                rt.increment(curLifter, rankingType, false, false);
                logger.trace("not counted {}  {} Rank={} total={}", curLifter, rankingType,
                        getRank(curLifter, rankingType), curLifter.getTotal());
                // if not eligible, cannot be part of a team
                setPoints(curLifter, 0, rankingType);
            } else {
                final double rankingValue = getRankingValue(curLifter, rankingType);
                if (rankingValue > 0) {
                    // eligible, not zero
                    rt.increment(curLifter, rankingType, true, false);
                    logger.trace("Athlete {} {}rank={} total={}", curLifter, rankingType,
                            getRank(curLifter, rankingType), rankingValue);
                } else {
                    // eligible but zero
                    rt.increment(curLifter, rankingType, true, true);
                    logger.trace("Athlete {}  {}rank={} total={}", curLifter, rankingType,
                            getRank(curLifter, rankingType), rankingValue);
                }

                // some competitions allow substitutes/non-team members to be eligible individually and earn medals but
                // not score team points unless explicitly named as part of team
                if (curLifter.isEligibleForTeamRanking()) {
                    final float points = computePoints(curLifter, rankingType);
                    setPoints(curLifter, points, rankingType);
                } else {
                    setPoints(curLifter, 0, rankingType);
                }

            }
            prevCategory = curCategory;
        }
    }

    /**
     * Assign lot numbers, sequentially. Normally called by {@link #drawLots(List)}.
     *
     * @param shuffledList the shuffled list
     */
    static public void assignLotNumbers(List<Athlete> shuffledList) {
        int lotNumber = 1;
        for (Athlete curLifter : shuffledList) {
            curLifter.setLotNumber(lotNumber++);
            curLifter.setStartNumber(0);
        }
    }

    /**
     * Assign sinclair ranks, sequentially for each gender
     *
     * @param sortedList  the sorted list
     * @param rankingType the ranking type
     */
    public static void assignSinclairRanksAndPoints(List<Athlete> sortedList, Ranking rankingType) {
        Gender prevGender = null;
        // String prevAgeGroup = null;
        RankSetter rs = new RankSetter();
        for (Athlete curLifter : sortedList) {
            final Gender curGender = curLifter.getGender();
            // final Integer curAgeGroup = curLifter.getAgeGroup();
            if (!equals(curGender, prevGender)) {
                // different gender
                rs = new RankSetter();
            }

            if (!curLifter.isEligibleForIndividualRanking()) {
                rs.increment(curLifter, rankingType, false, false);
                logger.trace("not eligible {}  {} rank={} total={}", curLifter, rankingType,
                        getRank(curLifter, rankingType), curLifter.getTotal());
            } else {
                final double rankingTotal = getRankingValue(curLifter, rankingType);
                if (rankingTotal > 0) {
                    rs.increment(curLifter, rankingType, true, false);
                    logger.trace("ranked {}  {} rank={} {}={} total={}", curLifter, rankingType,
                            getRank(curLifter, rankingType), rankingTotal);
                } else {
                    rs.increment(curLifter, rankingType, true, true);
                    logger.trace("zero {}  {} rank={} total={}", curLifter, rankingType,
                            getRank(curLifter, rankingType), rankingTotal);
                }
            }
            prevGender = curGender;
        }
    }

    /**
     * Assign start numbers to athletes.
     *
     * @param sortedList the sorted list
     */
    public static void assignStartNumbers(List<Athlete> sortedList) {
        int rank = 1;
        for (Athlete curLifter : sortedList) {
            Double bodyWeight = curLifter.getBodyWeight();
            if (bodyWeight != null && bodyWeight > 0.0D) {
                curLifter.setStartNumber(rank);
                rank++;
            } else {
                curLifter.setStartNumber(0);
            }

        }
    }

    /**
     * Compute the number of lifts already done. During snatch, exclude cj
     *
     * @param lifters the athletes in the group
     * @return number of lifts
     */
    static public int countLiftsDone(List<Athlete> lifters) {
        if (lifters != null && !lifters.isEmpty()) {
            int totalSnatch = 0;
            int totalCJ = 0;
            boolean cJHasStarted = false;
            for (Athlete Athlete : lifters) {
                totalSnatch += Athlete.getSnatchAttemptsDone();
                totalCJ += Athlete.getCleanJerkAttemptsDone();
                if (Athlete.getCleanJerkTotal() > 0) {
                    cJHasStarted = true;
                }
            }
            if (cJHasStarted || totalSnatch >= lifters.size() * 3) {
                return totalCJ;
            } else {
                return totalSnatch;
            }
        } else {
            return 0;
        }
    }

    // /**
    // * Sort athletes according to official rules (in place) for the technical
    // * meeting <tableToolbar> <li>by registration category</li> <li>by lot
    // * number</li> </tableToolbar>
    // */
    // static public void weighInOrder(List<Athlete> toBeSorted) {
    // Collections.sort(toBeSorted, new WeighInOrderComparator());
    // }
    //
    // /**
    // * Sort athletes according to official rules, creating a new list.
    // *
    // * @see #liftingOrder(List)
    // * @return athletes, ordered according to their standard order for the
    // * technical meeting
    // */
    // static public List<Athlete> weighInOrderCopy(List<Athlete> toBeSorted) {
    // List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
    // weighInOrder(sorted);
    // return sorted;
    // }

    /**
     * Sort athletes according to official rules (in place) <tableToolbar>
     * <li>by category</li>
     * <li>by lot number</li> </tableToolbar>.
     *
     * @param toBeSorted the to be sorted
     */
    static public void displayOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new DisplayOrderComparator());
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @param toBeSorted the to be sorted
     * @return athletes, ordered according to their standard order
     * @see #liftingOrder(List)
     */
    static public List<Athlete> displayOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        displayOrder(sorted);
        return sorted;
    }

    /**
     * Assign lot numbers at random.
     *
     * @param toBeShuffled the to be shuffled
     * @return the list
     */
    static public List<Athlete> drawLots(List<Athlete> toBeShuffled) {
        List<Athlete> shuffled = new ArrayList<>(toBeShuffled);
        Collections.shuffle(shuffled, new Random());
        assignLotNumbers(shuffled);
        return shuffled;
    }

    /**
     * Gets the rank.
     *
     * @param curLifter   the cur lifter
     * @param rankingType the ranking type
     * @return the rank
     */
    public static Integer getRank(Athlete curLifter, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            return curLifter.getSnatchRank();
        case CLEANJERK:
            return curLifter.getCleanJerkRank();
        case SMM:
            return curLifter.getSmmRank();
        case BW_SINCLAIR:
            return curLifter.getSinclairRank();
        case CAT_SINCLAIR:
            return curLifter.getCatSinclairRank();
        case ROBI:
            return curLifter.getRobiRank();
        case TOTAL:
            return curLifter.getRank();
        case CUSTOM:
            return curLifter.getCustomRank();
        default:
            break;
        }
        return 0;
    }

    /**
     * Check that Athlete is one of the howMany previous athletes. The list of athletes is assumed to have been sorted
     * with {@link #liftTimeOrderCopy}
     *
     * @param Athlete       the athlete
     * @param sortedLifters the sorted lifters
     * @param howMany       the how many
     * @return true if Athlete is found and meets criterion.
     * @see #liftingOrder(List)
     */
    static public boolean isRecentLifter(Athlete Athlete, List<Athlete> sortedLifters, int howMany) {
        int rank = sortedLifters.indexOf(Athlete);
        if (rank >= 0 && rank <= howMany - 1) {
            return true;
        }
        return false;
    }

    /**
     * Sort athletes according to official rules.
     * <p>
     * <li>Lowest weight goes first</li>
     * <li>At same weight, lower attempt goes first</li>
     * <li>At same weight and same attempt, whoever lifted first goes first</li>
     * <li>At first attempt of each lift, lowest lot number goes first if same weight is requested</li>
     * </p>
     *
     * @param toBeSorted the to be sorted
     */
    static public void liftingOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new LiftOrderComparator());
        int liftOrder = 1;
        for (Athlete curLifter : toBeSorted) {
            curLifter.setLiftOrderRank(liftOrder++);
        }
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @param toBeSorted the to be sorted
     * @return athletes, ordered according to their lifting order
     * @see #liftingOrder(List)
     */
    static public List<Athlete> liftingOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);

        liftingOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes according to who lifted last.
     *
     * @param toBeSorted the to be sorted
     */
    static public void liftTimeOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new LiftTimeStampComparator());
    }

    /**
     * Sort athletes according to who lifted last, creating a new list.
     *
     * @param toBeSorted the to be sorted
     * @return athletes, ordered according to their lifting order
     * @see #liftTimeOrder(List)
     */
    static public List<Athlete> LiftTimeOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        liftTimeOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes according to official rules (in place) for the technical meeting <tableToolbar>
     * <li>by registration category</li>
     * <li>by lot number</li> </tableToolbar>.
     *
     * @param toBeSorted the to be sorted
     */
    static public void registrationOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new RegistrationOrderComparator());
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @param toBeSorted the to be sorted
     * @return athletes, ordered according to their standard order for the technical meeting
     * @see #liftingOrder(List)
     */
    static public List<Athlete> registrationOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        registrationOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes according to winning order.
     *
     * @param toBeSorted  the to be sorted
     * @param rankingType the ranking type
     */
    static public void resultsOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new WinningOrderComparator(rankingType));
//        int liftOrder = 1;
//        for (Athlete curLifter : toBeSorted) {
////        	setRank(curLifter,liftOrder++, rankingType);
//            curLifter.setResultOrderRank(liftOrder++, rankingType);
//        }
    }

    /**
     * Sort athletes according to winning order, creating a new list.
     *
     * @param toBeSorted  the to be sorted
     * @param rankingType the ranking type
     * @return athletes, ordered according to their category and totalRank order
     * @see #liftingOrder(List)
     */
    static public List<Athlete> resultsOrderCopy(List<Athlete> toBeSorted, Ranking rankingType) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        resultsOrder(sorted, rankingType);
        return sorted;
    }

    /**
     * Sort athletes according to official rules (in place) for the start number <tableToolbar>
     * <li>by registration category</li>
     * <li>by lot number</li> </tableToolbar>.
     *
     * @param toBeSorted the to be sorted
     */
    static public void startNumberOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new StartNumberOrderComparator());
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @param toBeSorted the to be sorted
     * @return athletes, ordered according to their start number
     * @see #liftingOrder(List)
     */
    static public List<Athlete> startNumberOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        startNumberOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes by team, gender and totalRank so team totals can be assigned.
     *
     * @param toBeSorted  the to be sorted
     * @param rankingType the ranking type
     */
    public static void teamPointsOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new TeamPointsComparator(rankingType));
    }

    /**
     * Sort athletes by team, gender and totalRank so team totals can be computed.
     *
     * @param toBeSorted  the to be sorted
     * @param rankingType what type of lift or total is being ranked
     * @return the list
     */
    public static List<Athlete> teamPointsOrderCopy(List<Athlete> toBeSorted, Ranking rankingType) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        teamPointsOrder(sorted, rankingType);
        return sorted;
    }

    /**
     * @param curLifter
     * @param rankingType
     * @return
     */
    private static float computePoints(Athlete curLifter, Ranking rankingType) {
        if (!curLifter.isEligibleForTeamRanking()) {
            return 0;
        }
        switch (rankingType) {
        case SNATCH:
            return pointsFormula(curLifter.getSnatchRank(), curLifter);
        case CLEANJERK:
            return pointsFormula(curLifter.getCleanJerkRank(), curLifter);
        case TOTAL:
            return pointsFormula(curLifter.getTotalRank(), curLifter);
        case CUSTOM:
            return pointsFormula(curLifter.getCustomRank(), curLifter);
        case COMBINED:
            return pointsFormula(curLifter.getSnatchRank(), curLifter)
                    + pointsFormula(curLifter.getCleanJerkRank(), curLifter)
                    + pointsFormula(curLifter.getTotalRank(), curLifter);
        default:
            break;
        }
        return 0;
    }

    static private boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        }
        return false; // o1 is null but not o2
    }

    /**
     * @param curLifter
     * @param rankingType
     * @return
     */
    private static double getRankingValue(Athlete curLifter, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            return curLifter.getBestSnatch();
        case CLEANJERK:
            return curLifter.getBestCleanJerk();
        case TOTAL:
            return curLifter.getTotal();
        case ROBI:
            return curLifter.getRobi();
        case CUSTOM:
            return curLifter.getCustomScoreComputed();
        case COMBINED:
            return 0D; // no such thing
        case BW_SINCLAIR:
            return curLifter.getSinclair();
        case CAT_SINCLAIR:
            return curLifter.getCategorySinclair();
        case SMM:
            return curLifter.getSmm();
        default:
            break;
        }
        return 0D;
    }

    /**
     * @param rank
     * @param curLifter
     * @return
     */
    private static float pointsFormula(Integer rank, Athlete curLifter) {
        if (rank == null || rank <= 0) {
            return 0;
        }
        if (rank == 1) {
            return 28;
        }
        if (rank == 2) {
            return 25;
        }
        return 26 - rank;
    }

    /**
     * @param curLifter
     * @param Math.round(points
     * @param rankingType
     */
    private static void setPoints(Athlete curLifter, float points, Ranking rankingType) {
        logger.trace(curLifter + " " + rankingType + " points=" + points);
        switch (rankingType) {
        case SNATCH:
            curLifter.setSnatchPoints(Math.round(points));
            break;
        case CLEANJERK:
            curLifter.setCleanJerkPoints(Math.round(points));
            break;
        case TOTAL:
            curLifter.setTotalPoints(Math.round(points));
            break;
        case CUSTOM:
            curLifter.setCustomPoints(Math.round(points));
            break;
        default:
            break;// computed
        }
    }
}
