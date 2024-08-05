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

		int rank = eligible ? (rankingValue == 0 ? 0 : ++this.rank) : -1;
		logger.warn("a {} v {} z {} e {} rank={}", a.getShortName(), rankingValue, zero, eligible, rank);
		switch (r) {
			case SNATCH:
			case CLEANJERK:
			case TOTAL:
			case CUSTOM:
				doCategoryBasedRankings(a, r, category, zero);
				break;
			case BW_SINCLAIR:
				a.setSinclairRank(rank);
				break;
			case CAT_SINCLAIR:
				a.setCatSinclairRank(rank);
				break;
			case SNATCH_CJ_TOTAL:
				a.setCombinedRank(rank);
				break;
			case ROBI:
				a.setRobiRank(rank);
				break;
			case SMM:
				a.setSmhfRank(rank);
				break;
			case QPOINTS:
				a.setqPointsRank(rank);
				break;
			case QAGE:
				a.setqAgeRank(rank);
				break;
			case GAMX:
				a.setGamxRank(rank);
				break;
			case AGEFACTORS:
				a.setAgeAdjustedTotalRank(rank);
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
					CategoryRankingHolder curRankings = getCategoryRankings(curCat);
					if (!zero && a.isEligibleForIndividualRanking()) {
						this.snatchRank = curRankings.getSnatchRank();
						this.snatchRank = this.snatchRank + 1;
						p.setSnatchRank(this.snatchRank);
						curRankings.setSnatchRank(this.snatchRank);
						logger.warn("setting snatch rank {} {} {} {} {}", a, curCat, snatchRank, System.identityHashCode(p), System.identityHashCode(curRankings));
					} else {
						p.setSnatchRank(a.isEligibleForIndividualRanking() ? 0 : -1);
						logger.warn("skipping snatch rank {} {} {}", a, curCat, this.snatchRank);
					}
				}
					break;
				case CLEANJERK: {
					CategoryRankingHolder curRankings = getCategoryRankings(curCat);
					if (!zero && a.isEligibleForIndividualRanking()) {
						this.cjRank = curRankings.getCleanJerkRank();
						this.cjRank = this.cjRank + 1;
						p.setCleanJerkRank(this.cjRank);
						curRankings.setCleanJerkRank(this.cjRank);
						// logger.debug("setting clean&jerk rank {} {} {} {} {}", a, curCat, cjRank,
						// System.identityHashCode(p), System.identityHashCode(curRankings));
					} else {
						p.setCleanJerkRank(a.isEligibleForIndividualRanking() ? 0 : -1);
						// logger.debug("skipping clean&jerk rank {} {} {}", a, curCat, 0);
					}

				}
					break;
				case TOTAL: {
					CategoryRankingHolder curRankings = getCategoryRankings(curCat);
					if (!zero && a.isEligibleForIndividualRanking()) {
						this.totalRank = curRankings.getTotalRank();
						this.totalRank = this.totalRank + 1;
						p.setTotalRank(this.totalRank);
						curRankings.setTotalRank(this.totalRank);
						// logger.debug("setting total rank {} {} {} {} {}", a, curCat, totalRank,
						// System.identityHashCode(p),
						// System.identityHashCode(curRankings));
					} else {
						p.setTotalRank(a.isEligibleForIndividualRanking() ? 0 : -1);
						// logger.debug("skipping total rank {} {} {}", a, curCat, 0);
					}
				}
					break;
				case CUSTOM: {
					CategoryRankingHolder curRankings = getCategoryRankings(curCat);
					if (!zero && a.isEligibleForIndividualRanking()) {
						this.customRank = curRankings.getCustomRank();
						this.customRank = this.customRank + 1;
						p.setCustomRank(this.customRank);
						curRankings.setCustomRank(this.customRank);
						// logger.debug("setting custom rank {} {} {} {} {}", a, curCat, customRank,
						// System.identityHashCode(p),
						// System.identityHashCode(curRankings));
					} else {
						p.setCustomRank(a.isEligibleForIndividualRanking() ? 0 : -1);
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
