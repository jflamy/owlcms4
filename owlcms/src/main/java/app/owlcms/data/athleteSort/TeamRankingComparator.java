/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;

/**
 * This comparator sorts athletes within their team.
 *
 * @author jflamy
 */
public class TeamRankingComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    private Ranking rankingType;

    /**
     * Instantiates a new team points order comparator.
     *
     * @param rankingType the ranking type
     */
    TeamRankingComparator(Ranking rankingType) {
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

        compare = compareRanking(lifter1, lifter2);
        if (compare != 0) {
            return compare;
        }

        return compare;
    }

    /**
     * @param lifter1
     * @param lifter2
     * @return
     */
    private int compareRanking(Athlete lifter1, Athlete lifter2) {
        switch (rankingType) {
        case SNATCH:
            return lifter1.getSnatchRank().compareTo(lifter2.getSnatchRank());
        case CLEANJERK:
            return lifter1.getCleanJerkRank().compareTo(lifter2.getCleanJerkRank());
        case TOTAL:
            return lifter1.getRank().compareTo(lifter2.getRank());
        case CUSTOM:
            return lifter1.getRank().compareTo(lifter2.getCustomRank());
        default:
            break;
        }
        return 0;
    }

}
