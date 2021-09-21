/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import ch.qos.logback.classic.Logger;

public class MultiCategoryRankSetter {

    Logger logger = (Logger) LoggerFactory.getLogger(MultiCategoryRankSetter.class);

    private int rank = 0;

    // we use a participation objet because, by definition, it contains all the category-based rankings
    Map<String, Participation> rankings = new HashMap<>();

    public void increment(Athlete a, Ranking r, double rankingValue) {
        if (a == null) {
            return;
        }
        Category category = a.getCategory();
        boolean eligible = a.isEligibleForIndividualRanking();
        boolean zero = rankingValue <= 0;
        //logger.warn("a {} v {} z {} e {}", a.getShortName(), rankingValue, zero, eligible);

        switch (r) {
        case SNATCH:
        case CLEANJERK:
        case TOTAL:
            doCategoryBasedRankings(a, r, category, zero);
            break;
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

    private void doCategoryBasedRankings(Athlete a, Ranking r, Category category, boolean zero) {
        for (Participation p : a.getParticipations()) {
            Category curCat = p.getCategory();
            switch (r) {
            case SNATCH: {
                if (!zero) {
                    Participation curRankings = getCategoryRankingsForAthlete(a, curCat);
                    rank = curRankings.getSnatchRank();
                    rank = rank + 1;
                    p.setSnatchRank(rank);
                    curRankings.setSnatchRank(rank);
                    logger.warn("setting snatch rank {} {} {}", a, curCat, rank);
                } else {
                    p.setSnatchRank(0);
                    logger.warn("skipping snatch rank {} {} {}", a, curCat, 0);
                }

            }
                break;
            case CLEANJERK: {
                if (!zero) {
                    Participation curRankings = getCategoryRankingsForAthlete(a, curCat);
                    rank = curRankings.getCleanJerkRank();
                    rank = rank + 1;
                    p.setCleanJerkRank(zero ? rank : 0);
                    curRankings.setCleanJerkRank(rank);
                    logger.warn("setting clean&jerk rank {} {} {}", a, curCat, rank);
                } else {
                    p.setCleanJerkRank(0);
                    logger.warn("skipping clean&jerk rank {} {} {}", a, curCat, 0);
                }

            }
                break;
            case TOTAL: {
                if (!zero) {
                    Participation curRankings = getCategoryRankingsForAthlete(a, curCat);
                    rank = curRankings.getTotalRank();
                    rank = rank + 1;
                    p.setTotalRank(zero ? rank : 0);
                    curRankings.setTotalRank(rank);
                    logger.warn("setting total rank {} {} {}", a, curCat, rank);
                } else {
                    p.setTotalRank(0);
                    logger.warn("skipping total rank {} {} {}", a, curCat, 0);
                }

            }
                break;
            default:
                break;
            }
        }
    }

    Participation getCategoryRankingsForAthlete(Athlete a, Category category) {
        logger.warn("Category {} {}",category, System.identityHashCode(category));
        Participation bestCategoryRanks = rankings.get(category.getCode());
        if (bestCategoryRanks == null) {
            bestCategoryRanks = new Participation(a, category);
            rankings.put(category.getCode(), bestCategoryRanks);
        }
        return bestCategoryRanks;
    }

}
