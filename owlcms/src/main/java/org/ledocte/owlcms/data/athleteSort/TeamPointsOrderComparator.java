/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.athleteSort;

import java.util.Comparator;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athleteSort.LifterSorter.Ranking;


/**
 * This comparator sorts athletes within their team
 * 
 * @author jflamy
 * 
 */
public class TeamPointsOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

    private Ranking rankingType;

    TeamPointsOrderComparator(Ranking rankingType) {
        this.rankingType = rankingType;
    }

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
