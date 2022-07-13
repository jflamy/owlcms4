/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
import app.owlcms.spreadsheet.PAthlete;
import ch.qos.logback.classic.Logger;

/**
 * The Class AthleteSorter.
 *
 * @author jflamy
 * @since
 */

@SuppressWarnings("serial")
//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
public class AthleteSorter implements Serializable {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(AthleteSorter.class);

    /**
     * Assign ranks within each category, for all athletes in categories present in group. Returns the list of these
     * athletes (i.e. not only these in group g)
     *
     * @param g
     * @return
     */
    public static List<Athlete> assignCategoryRanks(Group g) {
        List<Athlete> impactedAthletes;
        if (g != null) {
            impactedAthletes = AthleteRepository.findAthletesForGlobalRanking(g);
//            logger.debug("all athletes in group's categories {}", impactedAthletes);
        } else {
            impactedAthletes = AthleteRepository.findAllByGroupAndWeighIn(null, true);
            // logger.debug("all athletes in all groups {}", impactedAthletes);
        }

        List<Athlete> sortedAthletes;
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.SNATCH, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.SNATCH);
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.CLEANJERK, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.CLEANJERK);
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.TOTAL, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.TOTAL);
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.CUSTOM, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.CUSTOM);

//        if (logger.isEnabledFor(Level.DEBUG)) {
//            for (Athlete a : impactedAthletes) {
//                Participation p = a.getMainRankings();
//                if (p != null) logger.debug("** {} {}", a, p.long_dump());
//            }
//        }
        return impactedAthletes;
    }
    
    public static List<Athlete> assignCategoryRanks(EntityManager em, Group g) {
        List<Athlete> impactedAthletes;
        if (g != null) {
            impactedAthletes = AthleteRepository.findAthletesForGlobalRanking(em, g);
//            logger.debug("all athletes in group's categories {}", impactedAthletes);
        } else {
            impactedAthletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, null, true, null);
            // logger.debug("all athletes in all groups {}", impactedAthletes);
        }

        List<Athlete> sortedAthletes;
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.SNATCH, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.SNATCH);
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.CLEANJERK, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.CLEANJERK);
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.TOTAL, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.TOTAL);
        sortedAthletes = AthleteSorter.resultsOrderCopy(impactedAthletes, Ranking.CUSTOM, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedAthletes, Ranking.CUSTOM);

//        if (logger.isEnabledFor(Level.DEBUG)) {
//            for (Athlete a : impactedAthletes) {
//                Participation p = a.getMainRankings();
//                if (p != null) logger.debug("** {} {}", a, p.long_dump());
//            }
//        }
        return impactedAthletes;
    }

    /**
     * Assign ranks, sequentially.
     *
     * @param sortedList  the sorted list
     * @param rankingType the ranking type
     */
    public static void assignCategoryRanks(List<Athlete> sortedList, Ranking rankingType) {
        AthleteSorter.resultsOrder(sortedList, rankingType, true);
        AthleteSorter.assignEligibleCategoryRanks(sortedList, rankingType);
        AthleteSorter.resultsOrder(sortedList, rankingType, false);
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
     * Assign overall (non category-dependent) ranks, sequentially for each gender
     *
     * @param sortedList  the sorted list
     * @param rankingType the ranking type
     */
    public static void assignOverallRanksAndPoints(List<Athlete> sortedList, Ranking rankingType) {
        Gender prevGender = null;

        OverallRankSetter rs = new OverallRankSetter();
        for (Athlete curLifter : sortedList) {
            final Gender curGender = curLifter.getGender();
            // final Integer curAgeGroup = curLifter.getAgeGroup();
            if (!equals(curGender, prevGender)) {
                // different gender
                rs = new OverallRankSetter();
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

    /**
     * Sort athletes according to official rules (in place) <tableToolbar>
     * <li>by category</li>
     * <li>by lot number</li> </tableToolbar>.
     *
     * @param athletes the to be sorted
     */
    static public void displayOrder(List<? extends Athlete> athletes) {
        Collections.sort(athletes, new DisplayOrderComparator());
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
            return curLifter.getMainRankings().getSnatchRank();
        case CLEANJERK:
            return curLifter.getMainRankings().getCleanJerkRank();
        case SMM:
            return curLifter.getSmmRank();
        case BW_SINCLAIR:
            return curLifter.getSinclairRank();
        case CAT_SINCLAIR:
            return curLifter.getCatSinclairRank();
        case ROBI:
            return curLifter.getRobiRank();
        case TOTAL:
            return curLifter.getMainRankings().getTotalRank();
        case CUSTOM:
            return curLifter.getMainRankings().getCustomRank();
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
     * @param rank
     * @param curLifter
     * @return
     */
    public static int pointsFormula(Integer rank) {
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
     * @param rank
     * @param curLifter
     * @return
     */
    public static float pointsFormula(Integer rank, Athlete curLifter) {
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
    static public void resultsOrder(List<Athlete> toBeSorted, Ranking rankingType, boolean absoluteOrder) {
        Collections.sort(toBeSorted, new WinningOrderComparator(rankingType, absoluteOrder));
//        int liftOrder = 1;
//        for (Athlete curLifter : toBeSorted) {
////        	setRank(curLifter,liftOrder++, rankingType);
//            curLifter.setResultOrderRank(liftOrder++, rankingType);
//        }
    }

    /**
     * Sort athletes according to winning order, creating a new list.
     *
     * @param athletes    the to be sorted
     * @param rankingType the ranking type
     * @return athletes, ordered according to their category and totalRank order
     * @see #liftingOrder(List)
     */
    static public List<Athlete> resultsOrderCopy(List<? extends Athlete> athletes, Ranking rankingType) {
        List<Athlete> sorted = new ArrayList<>(athletes);
        switch (rankingType) {
        case BW_SINCLAIR:
        case CAT_SINCLAIR:
        case SNATCH_CJ_TOTAL:
        case ROBI:
        case SMM:
            resultsOrder(sorted, rankingType, true);
            break;
        case SNATCH:
        case TOTAL:
        case CUSTOM:
        case CLEANJERK:
            resultsOrder(sorted, rankingType, false);
            break;
        }
        return sorted;
    }

    /**
     * Sort athletes according to winning order, creating a new list.
     *
     * @param toBeSorted  the to be sorted
     * @param rankingType the ranking type
     * @return athletes, ordered according to their category and totalRank order
     * @see #liftingOrder(List)
     */
    static public List<Athlete> resultsOrderCopy(List<? extends Athlete> toBeSorted, Ranking rankingType,
            boolean absoluteOrder) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        switch (rankingType) {
        case BW_SINCLAIR:
        case CAT_SINCLAIR:
        case SNATCH_CJ_TOTAL:
        case ROBI:
        case SMM:
            resultsOrder(sorted, rankingType, true);
            break;
        case SNATCH:
        case TOTAL:
        case CUSTOM:
        case CLEANJERK:
            resultsOrder(sorted, rankingType, absoluteOrder);
            break;
        }

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
     * @param athletes    the to be sorted
     * @param rankingType what type of lift or total is being ranked
     * @return the list
     */
    public static List<Athlete> teamPointsOrderCopy(List<? extends Athlete> athletes, Ranking rankingType) {
        List<Athlete> sorted = new ArrayList<>(athletes);
        teamPointsOrder(sorted, rankingType);
        return sorted;
    }

    public static List<PAthlete> teamPointsOrderedPAthletes(List<Participation> mwAgeGroupParticipations,
            Ranking rankingType) {
        mwAgeGroupParticipations.sort(new TeamPointsPComparator(rankingType));
        List<PAthlete> res = mwAgeGroupParticipations.stream().map(p -> new PAthlete(p)).collect(Collectors.toList());
        return res;
    }

    /**
     * Assign ranks, sequentially.
     *
     * @param absoluteOrderList list sorted without taking categories into accountoui
     *
     * @param rankingType       the ranking type
     */
    private static void assignEligibleCategoryRanks(List<Athlete> absoluteOrderList, Ranking rankingType) {
        MultiCategoryRankSetter rt = new MultiCategoryRankSetter();
        for (Athlete curLifter : absoluteOrderList) {
            if (curLifter.isEligibleForIndividualRanking()) {
                final double rankingValue = getRankingValue(curLifter, rankingType);
                rt.increment(curLifter, rankingType, rankingValue);
            }
        }

    }

    /**
     * @param curLifter
     * @param rankingType
     * @return
     */
    @SuppressWarnings("unused")
    private static float computePoints(Athlete curLifter, Ranking rankingType) {
        if (!curLifter.isEligibleForTeamRanking()) {
            return 0;
        }
        switch (rankingType) {
        case SNATCH:
            return pointsFormula(curLifter.getMainRankings().getSnatchRank(), curLifter);
        case CLEANJERK:
            return pointsFormula(curLifter.getMainRankings().getCleanJerkRank(), curLifter);
        case TOTAL:
            return pointsFormula(curLifter.getMainRankings().getTotalRank(), curLifter);
        case CUSTOM:
            return pointsFormula(curLifter.getMainRankings().getCustomRank(), curLifter);
        case SNATCH_CJ_TOTAL:
            return pointsFormula(curLifter.getMainRankings().getSnatchRank(), curLifter)
                    + pointsFormula(curLifter.getMainRankings().getCleanJerkRank(), curLifter)
                    + pointsFormula(curLifter.getMainRankings().getTotalRank(), curLifter);
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
        case SNATCH_CJ_TOTAL:
            return 0D; // no such thing
        case BW_SINCLAIR:
            return curLifter.getSinclairForDelta();
        case CAT_SINCLAIR:
            return curLifter.getCategorySinclair();
        case SMM:
            return curLifter.getSmfForDelta();
        default:
            break;
        }
        return 0D;
    }
}
