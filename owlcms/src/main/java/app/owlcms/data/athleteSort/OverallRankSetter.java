/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Logger;

public class OverallRankSetter {

    Logger logger = (Logger) LoggerFactory.getLogger(OverallRankSetter.class);

    private int rank = 0;

    public void increment(Athlete a, Ranking r, boolean eligible, boolean zero) {
        // logger.trace("increment {} {}",a.getShortName(), rank, r);
        switch (r) {
        case SNATCH:
        case CLEANJERK:
        case TOTAL:
        case SNATCH_CJ_TOTAL:
        case CUSTOM:
            throw new RuntimeException("using OverallRankSetter on a category-specific ranking");
        case BW_SINCLAIR:
            a.setSinclairRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case CAT_SINCLAIR:
            a.setCatSinclairRank(eligible ? (zero ? 0 : ++rank) : -1);
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
