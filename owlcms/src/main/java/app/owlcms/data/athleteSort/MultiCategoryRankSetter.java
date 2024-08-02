/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRankingHolder;
import app.owlcms.data.category.Participation;
import ch.qos.logback.classic.Logger;

public class MultiCategoryRankSetter {

	Logger logger = (Logger) LoggerFactory.getLogger(MultiCategoryRankSetter.class);

	// we use a participation objet because, by definition, it contains all the
	// category-based rankings
	Map<String, CategoryRankingHolder> rankings = new HashMap<>();
	private int rank = 0;
	private int snatchRank = 0;
	private int cjRank = 0;
	private int totalRank = 0;
	private int customRank = 0;

	public void increment(Athlete a, Ranking r, double rankingValue) {
		if (a == null) {
			return;
		}
		Category category = a.getCategory();
		boolean eligible = a.isEligibleForIndividualRanking();
		boolean zero = rankingValue <= 0;
		// logger.debug("a {} v {} z {} e {}", a.getShortName(), rankingValue, zero,
		// eligible);

		int value = eligible ? (zero ? 0 : ++this.rank) : -1;
		switch (r) {
			case SNATCH:
			case CLEANJERK:
			case TOTAL:
			case CUSTOM:
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
			case ROBI:
				a.setRobiRank(value);
				break;
			case SMHF:
				a.setSmhfRank(value);
				break;
			case QPOINTS:
				a.setqPointsRank(value);
				break;
			case QAGE:
				a.setqAgeRank(value);
				break;
			case GAMX:
				a.setGamxRank(value);
				break;
			case AGEFACTORS:
				a.setAgeAdjustedTotalRank(value);
		}
	}

	CategoryRankingHolder getCategoryRankings(Category category) {
		// logger.debug("Category {} {}",category, System.identityHashCode(category));
		CategoryRankingHolder bestCategoryRanks = this.rankings.get(category.getComputedCode());
		if (bestCategoryRanks == null) {
			bestCategoryRanks = new CategoryRankingHolder();
			this.rankings.put(category.getComputedCode(), bestCategoryRanks);
		}
		return bestCategoryRanks;
	}

	private void doCategoryBasedRankings(Athlete a, Ranking r, Category category, boolean zero) {
		for (Participation p : a.getParticipations()) {
			Category curCat = p.getCategory();
			switch (r) {
				case SNATCH: {
					if (!zero) {
						CategoryRankingHolder curRankings = getCategoryRankings(curCat);
						this.snatchRank = curRankings.getSnatchRank();
						this.snatchRank = this.snatchRank + 1;
						p.setSnatchRank(this.snatchRank);
						curRankings.setSnatchRank(this.snatchRank);
						// logger.debug("setting snatch rank {} {} {} {} {}", a, curCat, snatchRank,
						// System.identityHashCode(p),
						// System.identityHashCode(curRankings));
					} else {
						p.setSnatchRank(0);
						// logger.debug("skipping snatch rank {} {} {}", a, curCat, 0);
					}

				}
					break;
				case CLEANJERK: {
					if (!zero) {
						CategoryRankingHolder curRankings = getCategoryRankings(curCat);
						this.cjRank = curRankings.getCleanJerkRank();
						this.cjRank = this.cjRank + 1;
						p.setCleanJerkRank(this.cjRank);
						curRankings.setCleanJerkRank(this.cjRank);
						// logger.debug("setting clean&jerk rank {} {} {} {} {}", a, curCat, cjRank,
						// System.identityHashCode(p), System.identityHashCode(curRankings));
					} else {
						p.setCleanJerkRank(0);
						// logger.debug("skipping clean&jerk rank {} {} {}", a, curCat, 0);
					}

				}
					break;
				case TOTAL: {
					if (!zero) {
						CategoryRankingHolder curRankings = getCategoryRankings(curCat);
						this.totalRank = curRankings.getTotalRank();
						this.totalRank = this.totalRank + 1;
						p.setTotalRank(this.totalRank);
						curRankings.setTotalRank(this.totalRank);
						// logger.debug("setting total rank {} {} {} {} {}", a, curCat, totalRank,
						// System.identityHashCode(p),
						// System.identityHashCode(curRankings));
					} else {
						p.setTotalRank(0);
						// logger.debug("skipping total rank {} {} {}", a, curCat, 0);
					}
				}
					break;
				case CUSTOM: {
					if (!zero) {
						CategoryRankingHolder curRankings = getCategoryRankings(curCat);
						this.customRank = curRankings.getCustomRank();
						this.customRank = this.customRank + 1;
						p.setCustomRank(this.customRank);
						curRankings.setCustomRank(this.customRank);
						// logger.debug("setting custom rank {} {} {} {} {}", a, curCat, customRank,
						// System.identityHashCode(p),
						// System.identityHashCode(curRankings));
					} else {
						p.setCustomRank(0);
						// logger.debug("skipping custom rank {} {} {}", a, curCat, 0);
					}
					break;
				}
				default:
					this.logger.error("CAN'T HAPPEN setting unknown rank '{}'  {} {} {}", r, a, curCat,
					        System.identityHashCode(p));
					break;
			}
		}
	}

}
