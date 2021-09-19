/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.HashMap;
import java.util.Map;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;

public class MultiCategoryRankSetter {

    private int rank = 0;

    // we use a participation objet because, by definition, it contains all the category-based rankings
    Map<Category, Participation> rankings = new HashMap<Category, Participation>();

    public void increment(Athlete a, Ranking r, boolean zero) {
        if (a == null) {
            return;
        }
        Category category = a.getCategory();
        getCategoryRankings(a, category);
        boolean eligible = a.isEligibleForIndividualRanking();

        switch (r) {
        case SNATCH:
        case CLEANJERK:
        case TOTAL:
            doCategoryBasedRankings(a, r, category);
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

    private void doCategoryBasedRankings(Athlete a, Ranking r, Category category) {
        for (Participation p : a.getParticipations()) {
            switch (r) {
            case SNATCH: {
                Participation curRankings = getCategoryRankings(a, category);
                rank = curRankings.getSnatchRank();
                rank++;
                p.setSnatchRank(rank);
                curRankings.setSnatchRank(rank);
            }
                break;
            case CLEANJERK: {
                Participation curRankings = getCategoryRankings(a, category);
                rank = curRankings.getCleanJerkRank();
                rank++;
                p.setCleanJerkRank(rank);
                curRankings.setCleanJerkRank(rank);
            }
                break;
            case TOTAL: {
                Participation curRankings = getCategoryRankings(a, category);
                rank = curRankings.getTotalRank();
                rank++;
                p.setTotalRank(rank);
                curRankings.setTotalRank(rank);
            }
                break;
            default:
                break;
            }
        }
    }

    private Participation getCategoryRankings(Athlete a, Category category) {
        Participation bestCategoryRanks = rankings.get(category);
        if (bestCategoryRanks == null) {
            bestCategoryRanks = new Participation(a, category);
            rankings.put(category, bestCategoryRanks);
        }
        return bestCategoryRanks;
    }

}
