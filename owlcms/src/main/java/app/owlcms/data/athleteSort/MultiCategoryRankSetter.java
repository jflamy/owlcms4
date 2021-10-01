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
import app.owlcms.data.category.CategoryRankingHolder;
import app.owlcms.data.category.Participation;
import ch.qos.logback.classic.Logger;

public class MultiCategoryRankSetter {

    Logger logger = (Logger) LoggerFactory.getLogger(MultiCategoryRankSetter.class);

    private int rank = 0;

    // we use a participation objet because, by definition, it contains all the category-based rankings
    Map<String, CategoryRankingHolder> rankings = new HashMap<>();

    public void increment(Athlete a, Ranking r, double rankingValue) {
        if (a == null) {
            return;
        }
        Category category = a.getCategory();
        boolean eligible = a.isEligibleForIndividualRanking();
        boolean zero = rankingValue <= 0;
        //logger.debug("a {} v {} z {} e {}", a.getShortName(), rankingValue, zero, eligible);

        int value = eligible ? (zero ? 0 : ++rank) : -1;
        switch (r) {
        case SNATCH:
        case CLEANJERK:
        case TOTAL:
            doCategoryBasedRankings(a, r, category, zero);
            break;
        case BW_SINCLAIR:
            a.setSinclairRank(value);
            break;
        case CAT_SINCLAIR:
            a.setCatSinclairRank(value);
            break;
        case SNATCH_CJ_TOTAL:
            a.setCombinedRank(value);
            break;
        case CUSTOM:
            a.setCustomRank(value);
            break;
        case ROBI:
            a.setRobiRank(value);
            break;
        case SMM:
            a.setSmmRank(value);
            break;
        }
    }

    private void doCategoryBasedRankings(Athlete a, Ranking r, Category category, boolean zero) {
        for (Participation p : a.getParticipations()) {
            Category curCat = p.getCategory();
            switch (r) {
            case SNATCH: {
                if (!zero) {
                    CategoryRankingHolder curRankings = getCategoryRankings(curCat);
                    rank = curRankings.getSnatchRank();
                    rank = rank + 1;
                    p.setSnatchRank(rank);
                    curRankings.setSnatchRank(rank);
                    logger.debug("setting snatch rank {} {} {} {} {}", a, curCat, rank, System.identityHashCode(p), System.identityHashCode(curRankings));
                } else {
                    p.setSnatchRank(0);
                    logger.debug("skipping snatch rank {} {} {}", a, curCat, 0);
                }

            }
                break;
            case CLEANJERK: {
                if (!zero) {
                    CategoryRankingHolder curRankings = getCategoryRankings(curCat);
                    rank = curRankings.getCleanJerkRank();
                    rank = rank + 1;
                    p.setCleanJerkRank(rank);
                    curRankings.setCleanJerkRank(rank);
                    logger.debug("setting clean&jerk rank {} {} {} {} {}", a, curCat, rank, System.identityHashCode(p), System.identityHashCode(curRankings));
                } else {
                    p.setCleanJerkRank(0);
                    logger.debug("skipping clean&jerk rank {} {} {}", a, curCat, 0);
                }

            }
                break;
            case TOTAL: {
                if (!zero) {
                    CategoryRankingHolder curRankings = getCategoryRankings(curCat);
                    rank = curRankings.getTotalRank();
                    rank = rank + 1;
                    p.setTotalRank(rank);
                    curRankings.setTotalRank(rank);
                    logger.debug("setting total rank {} {} {} {} {}", a, curCat, rank, System.identityHashCode(p), System.identityHashCode(curRankings));
                } else {
                    p.setTotalRank(0);
                    logger.debug("skipping total rank {} {} {}", a, curCat, 0);
                }

            }
                break;
            default:
                break;
            }
        }
    }

    CategoryRankingHolder getCategoryRankings(Category category) {
        //logger.debug("Category {} {}",category, System.identityHashCode(category));
        CategoryRankingHolder bestCategoryRanks = rankings.get(category.getCode());
        if (bestCategoryRanks == null) {
            bestCategoryRanks = new CategoryRankingHolder();
            rankings.put(category.getCode(), bestCategoryRanks);
        }
        return bestCategoryRanks;
    }

}
