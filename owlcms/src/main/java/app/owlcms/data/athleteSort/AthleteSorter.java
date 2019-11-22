/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
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
import app.owlcms.data.competition.Competition;
import ch.qos.logback.classic.Logger;

/**
 * The Class AthleteSorter.
 *
 * @author jflamy
 * @since
 */
@Entity
public class AthleteSorter implements Serializable {

    /**
     * The Enum Ranking.
     */
    public enum Ranking {
        SNATCH, CLEANJERK, TOTAL,
        /** combined (men + women). */
        COMBINED, SINCLAIR, // cat, bw or smm depending on competition parameters
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
        Integer prevAgeGroup = null;
        Integer curAgeGroup = null;

        int rank = 1;
        for (Athlete curLifter : sortedList) {
            Category curCategory = null;
            if (Competition.getCurrent().isUseRegistrationCategory() || rankingType == Ranking.CUSTOM) {
                curCategory = curLifter.getRegistrationCategory();
                if (curCategory == null && rankingType == Ranking.CUSTOM) {
                    curCategory = curLifter.getCategory();
                }
                logger.trace("Athlete {}, category {}, regcategory {}",
                        new Object[] { curLifter, curLifter.getCategory(), curLifter.getRegistrationCategory() });
            } else {
                curCategory = curLifter.getCategory();
            }
            if (Competition.getCurrent().isMasters()) {
                curAgeGroup = curLifter.getAgeGroup();
                if (!equals(curCategory, prevCategory) || !equals(curAgeGroup, prevAgeGroup)) {
                    // category boundary has been crossed
                    rank = 1;
                }
            } else {
                // not masters, only consider category boundary
                if (!equals(curCategory, prevCategory)) {
                    // category boundary has been crossed
                    logger.trace("category boundary crossed {}", curCategory);
                    rank = 1;
                }
            }

            if (curLifter.isInvited() || !curLifter.getTeamMember()) {
                logger.trace("not counted {}  {}Rank={} total={} {}",
                        new Object[] { curLifter, rankingType, -1, curLifter.getTotal(), curLifter.isInvited() });
                setRank(curLifter, -1, rankingType);
                setPoints(curLifter, 0, rankingType);
            } else {
                // if (curLifter.getTeamMember()) {
                // setTeamRank(curLifter, 0, rankingType);
                // }
                final double rankingTotal = getRankingTotal(curLifter, rankingType);
                if (rankingTotal > 0) {
                    setRank(curLifter, rank, rankingType);
                    logger.trace("Athlete {}  {}rank={} total={}",
                            new Object[] { curLifter, rankingType, getRank(curLifter, rankingType), rankingTotal });
                    rank++;
                } else {
                    logger.trace("Athlete {}  {}rank={} total={}",
                            new Object[] { curLifter, rankingType, 0, rankingTotal });
                    setRank(curLifter, 0, rankingType);
                    rank++;
                }
                final float points = computePoints(curLifter, rankingType);
                setPoints(curLifter, points, rankingType);

            }
            prevCategory = curCategory;
            prevAgeGroup = curAgeGroup;
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
     * Assign ranks, sequentially.
     *
     * @param sortedList  the sorted list
     * @param rankingType the ranking type
     */
    public static void assignSinclairRanksAndPoints(List<Athlete> sortedList, Ranking rankingType) {
        Gender prevGender = null;
        // String prevAgeGroup = null;
        int rank = 1;
        for (Athlete curLifter : sortedList) {
            final Gender curGender = curLifter.getGender();
            // final Integer curAgeGroup = curLifter.getAgeGroup();
            if (!equals(curGender, prevGender)
            // || !equals(curAgeGroup,prevAgeGroup)
            ) {
                // category boundary has been crossed
                rank = 1;
            }

            if (curLifter.isInvited() || !curLifter.getTeamMember()) {
                logger.trace("invited {}  {}rank={} total={} {}",
                        new Object[] { curLifter, rankingType, -1, curLifter.getTotal(), curLifter.isInvited() });
                setRank(curLifter, -1, rankingType);
                setPoints(curLifter, 0, rankingType);
            } else {
                setTeamRank(curLifter, 0, rankingType);
                final double rankingTotal = getRankingTotal(curLifter, rankingType);
                if (rankingTotal > 0) {
                    setRank(curLifter, rank, rankingType);
                    logger.trace("Athlete {}  {}rank={} {}={} total={}",
                            new Object[] { curLifter, rankingType, rank, rankingTotal });
                    rank++;
                } else {
                    logger.trace("Athlete {}  {}rank={} total={}",
                            new Object[] { curLifter, rankingType, 0, rankingTotal });
                    setRank(curLifter, 0, rankingType);
                    rank++;
                }
                final float points = computePoints(curLifter, rankingType);
                setPoints(curLifter, points, rankingType);
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
     * @param curLifter
     * @param rankingType
     * @return
     */
    private static float computePoints(Athlete curLifter, Ranking rankingType) {
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
        case SINCLAIR:
            return curLifter.getSinclairRank();
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
     * @param curLifter
     * @param rankingType
     * @return
     */
    private static double getRankingTotal(Athlete curLifter, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            return curLifter.getBestSnatch();
        case CLEANJERK:
            return curLifter.getBestCleanJerk();
        case TOTAL:
            return curLifter.getTotal();
        case SINCLAIR:
            return curLifter.getSinclair();
        case ROBI:
            return curLifter.getRobi();
        case CUSTOM:
            return curLifter.getCustomScore();
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
     * Check that Athlete is one of the howMany previous athletes. The list of
     * athletes is assumed to have been sorted with {@link #liftTimeOrderCopy}
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
     * <li>At first attempt of each lift, lowest lot number goes first if same
     * weight is requested</li>
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
     * Sort athletes according to official rules (in place) for the technical
     * meeting <tableToolbar>
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
     * @return athletes, ordered according to their standard order for the technical
     *         meeting
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
     * @param curLifter
     * @param points
     * @param rankingType
     */
    private static void setPoints(Athlete curLifter, float points, Ranking rankingType) {
        logger.trace(curLifter + " " + rankingType + " points=" + points);
        switch (rankingType) {
        case SNATCH:
            curLifter.setSnatchPoints(points);
            break;
        case CLEANJERK:
            curLifter.setCleanJerkPoints(points);
            break;
        case TOTAL:
            curLifter.setTotalPoints(points);
            break;
        case CUSTOM:
            curLifter.setCustomPoints(points);
            break;
        default:
            break;// computed
        }
    }

    /**
     * Sets the rank.
     *
     * @param curLifter   the cur lifter
     * @param i           the i
     * @param rankingType the ranking type
     */
    public static void setRank(Athlete curLifter, int i, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            curLifter.setSnatchRank(i);
            break;
        case CLEANJERK:
            curLifter.setCleanJerkRank(i);
            break;
        case TOTAL:
            curLifter.setTotalRank(i);
            break;
        case SINCLAIR:
            curLifter.setSinclairRank(i);
            break;
        case ROBI:
            curLifter.setRobiRank(i);
            break;
        case CUSTOM:
            curLifter.setCustomRank(i);
            break;
        default:
            break;
        }
    }

    /**
     * Sets the team rank.
     *
     * @param curLifter   the cur lifter
     * @param i           the i
     * @param rankingType the ranking type
     */
    public static void setTeamRank(Athlete curLifter, int i, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            curLifter.setTeamSnatchRank(i);
            break;
        case CLEANJERK:
            curLifter.setTeamCleanJerkRank(i);
            break;
        case TOTAL:
            curLifter.setTeamTotalRank(i);
            break;
        case SINCLAIR:
            curLifter.setTeamSinclairRank(i);
            break;
        case ROBI:
            curLifter.setTeamRobiRank(i);
            break;
        case COMBINED:
            return; // there is no combined rank
        default:
            break;
        }
    }

    /**
     * Sort athletes according to official rules (in place) for the start number
     * <tableToolbar>
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
    static public void teamRankingOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new TeamRankingComparator(rankingType));
    }

    /**
     * Sort athletes by team, gender and totalRank so team totals can be computed.
     *
     * @param toBeSorted  the to be sorted
     * @param rankingType what type of lift or total is being ranked
     * @return the list
     */
    public static List<Athlete> teamRankingOrderCopy(List<Athlete> toBeSorted, Ranking rankingType) {
        List<Athlete> sorted = new ArrayList<>(toBeSorted);
        teamRankingOrder(sorted, rankingType);
        return sorted;
    }

    /**
     * Assign ranks, sequentially.
     *
     * @param sortedList  the sorted list
     * @param rankingType the ranking type
     */
    public void assignRanksWithinTeam(List<Athlete> sortedList, Ranking rankingType) {
        String prevTeam = null;
        // String prevAgeGroup = null;
        int rank = 1;
        for (Athlete curLifter : sortedList) {
            final String curTeam = curLifter.getTeam() + "_" + curLifter.getGender();
            // final Integer curAgeGroup = curLifter.getAgeGroup();
            if (!equals(curTeam, prevTeam)
            // || !equals(curAgeGroup,prevAgeGroup)
            ) {
                // category boundary has been crossed
                rank = 1;
            }

            if (curLifter.isInvited() || !curLifter.getTeamMember()) {
                setTeamRank(curLifter, -1, rankingType);
            } else {
                if (getRankingTotal(curLifter, rankingType) > 0) {
                    setTeamRank(curLifter, rank, rankingType);
                    rank++;
                } else {
                    setTeamRank(curLifter, 0, rankingType);
                    rank++;
                }
            }
            prevTeam = curTeam;
        }
    }

    /**
     * @param athletes
     * @param rankingType
     */
    @SuppressWarnings("unused")
    private void combinedPointsOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new CombinedPointsOrderComparator(rankingType));
    }

    /**
     * @param athletes
     * @param rankingType
     */
    @SuppressWarnings("unused")
    private void teamPointsOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new TeamPointsOrderComparator(rankingType));
    }

    // public Collection<Team> fullResults(List<Athlete> athletes) {
    // resultsOrder(athletes, Ranking.SNATCH);
    // assignCategoryRanksAndPoints(athletes, Ranking.SNATCH);
    // teamPointsOrder(athletes, Ranking.SNATCH);
    // assignRanksWithinTeam(athletes, Ranking.SNATCH);
    //
    // resultsOrder(athletes, Ranking.CLEANJERK);
    // assignCategoryRanksAndPoints(athletes, Ranking.CLEANJERK);
    // teamPointsOrder(athletes, Ranking.CLEANJERK);
    // assignRanksWithinTeam(athletes, Ranking.CLEANJERK);
    //
    // resultsOrder(athletes, Ranking.TOTAL);
    // assignCategoryRanksAndPoints(athletes, Ranking.TOTAL);
    // teamPointsOrder(athletes, Ranking.TOTAL);
    // assignRanksWithinTeam(athletes, Ranking.TOTAL);
    //
    // combinedPointsOrder(athletes, Ranking.COMBINED);
    // assignCategoryRanksAndPoints(athletes, Ranking.COMBINED);
    // teamPointsOrder(athletes, Ranking.COMBINED);
    // assignRanksWithinTeam(athletes, Ranking.COMBINED);
    //
    // resultsOrder(athletes, Ranking.SINCLAIR);
    // assignCategoryRanksAndPoints(athletes, Ranking.SINCLAIR);
    // teamPointsOrder(athletes, Ranking.SINCLAIR);
    // assignRanksWithinTeam(athletes, Ranking.SINCLAIR);
    //
    // HashSet<Team> teams = new HashSet<Team>();
    // return new TreeSet<Team>(teams);
    // }

}
