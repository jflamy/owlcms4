/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.athleteSort;

import java.util.Comparator;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athleteSort.AthleteSorter.Ranking;


/**
 * This comparator sorts athletes within their team.
 *
 * @author jflamy
 */
public class TeamPointsOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    private Ranking rankingType;

    /**
     * Instantiates a new team points order comparator.
     *
     * @param rankingType the ranking type
     */
    TeamPointsOrderComparator(Ranking rankingType) {
        this.rankingType = rankingType;
    }

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        compare = compareClub(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareGender(lifter1, lifter2);
        if (compare != 0)
            return compare;

        compare = compareRanking(lifter1, lifter2);
        if (compare != 0)
            return compare;

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
		default:
			break;
        }
        return 0;
    }

}
