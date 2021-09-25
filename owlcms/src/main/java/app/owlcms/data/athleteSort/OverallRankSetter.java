/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;

public class OverallRankSetter {

    private int rank = 0;
    

    public void increment(Athlete a, Ranking r, boolean eligible, boolean zero) {

        switch (r) {
        case SNATCH:
        case CLEANJERK:
        case TOTAL:
            throw new RuntimeException("using OverallRankSetter on a category-specific ranking");
        case BW_SINCLAIR:
            a.setSinclairRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case CAT_SINCLAIR:
            a.setCatSinclairRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;

        case COMBINED:
            a.setCombinedRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case CUSTOM:
            a.setCustomRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case ROBI:
            a.setRobiRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case SMM:
            a.setSmmRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        }
    }

}
