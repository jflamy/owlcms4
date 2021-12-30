/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
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
    @SuppressWarnings("incomplete-switch")
    private int compareRanking(Athlete lifter1, Athlete lifter2) {
        switch (rankingType) {
        case SNATCH:
            int snatchRank = lifter1.getMainRankings().getSnatchRank();
            int snatchRank2 = lifter2.getMainRankings().getSnatchRank();
            return Integer.compare(snatchRank, snatchRank2);
        case CLEANJERK:
            int cleanJerkRank = lifter1.getMainRankings().getCleanJerkRank();
            int cleanJerkRank2 = lifter2.getMainRankings().getCleanJerkRank();
            return Integer.compare(cleanJerkRank, cleanJerkRank2);
        case TOTAL:
            int totalRank = lifter1.getMainRankings().getTotalRank();
            int totalRank2 = lifter2.getMainRankings().getTotalRank();
            return Integer.compare(totalRank, totalRank2);
        case CUSTOM:
            int customRank1 = lifter1.getMainRankings().getCustomRank();
            int customRank2 = lifter2.getMainRankings().getCustomRank();
            return Integer.compare(customRank1, customRank2);
        }
        return 0;
    }

}
