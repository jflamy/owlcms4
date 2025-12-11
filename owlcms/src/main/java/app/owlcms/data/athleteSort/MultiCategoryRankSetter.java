/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
	private int categoryScoreRank = 0;

	public Participation increment(Athlete a, Ranking r, double rankingValue, Category participationCategory) {
		if (a == null) {
			return null;
		}
		boolean eligible = a.isEligibleForIndividualRanking();
		boolean zero = rankingValue <= 0;
		if (participationCategory == null) {
			participationCategory = a.getCategory();
		}

		Participation participation = a.getMainRankings();
		int rank = eligible ? (rankingValue == 0 ? 0 : ++this.rank) : -1;
		// logger.debug("c {} r {} -- a {}/{} v {} z {} e {} rank={} {}", participationCategory, r, a.getAbbreviatedName(), System.identityHashCode(a),
		// rankingValue, zero, eligible, rank, ""); // LoggerUtils.stackTrace());
		switch (r) {
			case SNATCH:
			case CLEANJERK:
			case TOTAL:
			case CUSTOM:
			case CATEGORY_SCORE:
				participation = doCategoryBasedRankings(a, r, participationCategory, zero);
				break;
			case BW_SINCLAIR:
				a.setSinclairRank(rank);
				break;
			case CAT_SINCLAIR:
				a.setCatSinclairRank(rank);
				break;
			case CAT_QPOINTS:
				a.setCatQPointsRank(rank);
				break;
			case CAT_GAMX:
				a.setCatGAMXRank(rank);
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
				a.setQMastersRank(rank);
				break;
			case GAMX:
				a.setGamxRank(rank);
				break;
			case GAMX_M:
				a.setGamxMRank(rank);
				break;
			case GAMX_U:
				a.setGamxURank(rank);
				break;
			case GAMX_A:
				a.setGamxARank(rank);
				break;
			case AGEFACTORS:
				a.setQYouthRank(rank);
				break;
		}
		return participation;
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

	private Participation doCategoryBasedRankings(Athlete a, Ranking r, Category category, boolean zero) {
		// logger.debug("a {} participations {}", a.getAbbreviatedName(), a.getParticipations());
		for (Participation p : a.getParticipations()) {
			Category curCat = p.getCategory();
			if (curCat.sameAs(category)) {
				switch (r) {
					case SNATCH: {
						CategoryRankingHolder curRankings = getCategoryRankings(curCat);
						if (!zero && a.isEligibleForIndividualRanking()) {
							this.snatchRank = curRankings.getSnatchRank();
							this.snatchRank = this.snatchRank + 1;
							p.setSnatchRank(this.snatchRank);
							curRankings.setSnatchRank(this.snatchRank);
							// logger.debug("setting snatch rank {} {} {} p={} a={}", a, curCat, snatchRank, System.identityHashCode(p),
							// System.identityHashCode(p.getAthlete()));
						} else {
							p.setSnatchRank(a.isEligibleForIndividualRanking() ? 0 : -1);
							// logger.debug("skipping snatch rank {} {} {}", a, curCat, this.snatchRank);
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
							// logger.debug("setting clean&jerk rank {} {} {} p {} a {}", a, curCat, cjRank, System.identityHashCode(p), //
							// System.identityHashCode(p.getAthlete()));
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
							// logger.debug("setting total rank {} {} {} p {} a {}", a, curCat, totalRank, System.identityHashCode(p), //
							// System.identityHashCode(p.getAthlete()));

						} else {
							p.setTotalRank(a.isEligibleForIndividualRanking() ? 0 : -1);
							// logger.debug("skipping total rank {} {} {}", a, curCat, totalRank);
						}
					}
						break;
					   case CATEGORY_SCORE: {
						   CategoryRankingHolder curRankings = getCategoryRankings(curCat);
						   double rankingValue = Ranking.getRankingValue(a, Ranking.CATEGORY_SCORE);
						   boolean eligible = a.isEligibleForIndividualRanking();
						   boolean zeroValue = rankingValue <= 0;
						   //logger.debug("[CATEGORY_SCORE] Athlete: {} | Category: {} | rankingValue: {} | eligible: {} | Participation ID: {}", a.getAbbreviatedName(), curCat.getCode(), rankingValue, eligible, System.identityHashCode(p));
						   if (!zeroValue && eligible) {
							   this.categoryScoreRank = curRankings.getCategoryScoreRank();
							   this.categoryScoreRank = this.categoryScoreRank + 1;
							   p.setCategoryScoreRank(this.categoryScoreRank);
							   curRankings.setCategoryScoreRank(this.categoryScoreRank);
							   //logger.debug("[CATEGORY_SCORE] Assigned rank {} to athlete {} in category {} | Participation ID: {}", this.categoryScoreRank, a.getAbbreviatedName(), curCat.getCode(), System.identityHashCode(p));
						   } else {
							   p.setCategoryScoreRank(eligible ? 0 : -1);
							   //logger.debug("[CATEGORY_SCORE] Set rank {} for athlete {} in category {} (reason: {}{} ) | Participation ID: {}", (eligible ? 0 : -1), a.getAbbreviatedName(), curCat.getCode(), (!eligible ? "not eligible" : "zero value"), "", System.identityHashCode(p));
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
				return p;
			} else {
				// logger.debug("? curCat {} not same as category {}", curCat, category);
			}
		}
		return null;
	}

}
