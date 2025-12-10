/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * Configuration for which rankings should be computed.
 * Controls which scoring systems are calculated for athletes.
 */
public class RankingConfig {

	private static final EnumMap<Ranking, Boolean> computeRanking = new EnumMap<>(Ranking.class);

	static {
		// Category values - always computed by default
		computeRanking.put(Ranking.SNATCH, true);
		computeRanking.put(Ranking.CLEANJERK, true);
		computeRanking.put(Ranking.TOTAL, true);
		computeRanking.put(Ranking.CUSTOM, true);
		computeRanking.put(Ranking.SNATCH_CJ_TOTAL, true);
		computeRanking.put(Ranking.CATEGORY_SCORE, true);

		// Global scoring systems - Sinclair and QPoints enabled by default
		computeRanking.put(Ranking.BW_SINCLAIR, true);
		computeRanking.put(Ranking.CAT_SINCLAIR, true);
		computeRanking.put(Ranking.SMM, true);
		computeRanking.put(Ranking.CAT_QPOINTS, false);
		computeRanking.put(Ranking.ROBI, false);
		computeRanking.put(Ranking.QPOINTS, true);
		computeRanking.put(Ranking.AGEFACTORS, false);
		computeRanking.put(Ranking.QAGE, true);
		computeRanking.put(Ranking.GAMX, false);
		computeRanking.put(Ranking.GAMX_M, false);
		computeRanking.put(Ranking.GAMX_U, false);
		computeRanking.put(Ranking.GAMX_A, false);
	}

	/**
	 * Check if a ranking should be computed.
	 *
	 * @param ranking the ranking to check
	 * @return true if the ranking should be computed, false otherwise
	 */
	public static boolean shouldCompute(Ranking ranking) {
		return computeRanking.getOrDefault(ranking, false);
	}

	/**
	 * Set whether a ranking should be computed.
	 *
	 * @param ranking the ranking to configure
	 * @param compute true to enable computation, false to disable
	 */
	public static void setCompute(Ranking ranking, boolean compute) {
		computeRanking.put(ranking, compute);
	}

	/**
	 * Get the current configuration as a copy.
	 *
	 * @return a copy of the current EnumMap configuration
	 */
	public static EnumMap<Ranking, Boolean> getConfig() {
		return new EnumMap<>(computeRanking);
	}

	/**
	 * Reset all rankings to their default configuration.
	 */
	public static void resetDefaults() {
		computeRanking.clear();
		// Category values
		computeRanking.put(Ranking.SNATCH, true);
		computeRanking.put(Ranking.CLEANJERK, true);
		computeRanking.put(Ranking.TOTAL, true);
		computeRanking.put(Ranking.CUSTOM, true);
		computeRanking.put(Ranking.SNATCH_CJ_TOTAL, true);
		computeRanking.put(Ranking.CATEGORY_SCORE, true);

		// Global scoring systems - Sinclair and QPoints enabled by default
		computeRanking.put(Ranking.BW_SINCLAIR, true);
		computeRanking.put(Ranking.CAT_SINCLAIR, true);
		computeRanking.put(Ranking.SMM, true);
		computeRanking.put(Ranking.CAT_QPOINTS, false);
		computeRanking.put(Ranking.ROBI, false);
		computeRanking.put(Ranking.QPOINTS, true);
		computeRanking.put(Ranking.AGEFACTORS, false);
		computeRanking.put(Ranking.QAGE, true);
		computeRanking.put(Ranking.GAMX, false);
		computeRanking.put(Ranking.GAMX_M, false);
		computeRanking.put(Ranking.GAMX_U, false);
		computeRanking.put(Ranking.GAMX_A, false);
	}

	private RankingConfig() {
		// Utility class - no instantiation
	}

	/**
	 * Get all Sinclair-based rankings.
	 *
	 * @return set containing BW_SINCLAIR, CAT_SINCLAIR, and SMM
	 */
	public static Set<Ranking> getSinclairRankings() {
		return EnumSet.of(Ranking.BW_SINCLAIR, Ranking.CAT_SINCLAIR, Ranking.SMM);
	}

	/**
	 * Get all QPoints-based rankings.
	 *
	 * @return set containing QPOINTS, QAGE, AGEFACTORS, and CAT_QPOINTS
	 */
	public static Set<Ranking> getQPointsRankings() {
		return EnumSet.of(Ranking.QPOINTS, Ranking.QAGE, Ranking.AGEFACTORS, Ranking.CAT_QPOINTS);
	}

	/**
	 * Get all GAMX-based rankings.
	 *
	 * @return set containing GAMX, GAMX_M, GAMX_U, and GAMX_A
	 */
	public static Set<Ranking> getGamxRankings() {
		return EnumSet.of(Ranking.GAMX, Ranking.GAMX_M, Ranking.GAMX_U, Ranking.GAMX_A);
	}

	/**
	 * Get all other global scoring system rankings.
	 *
	 * @return set containing SMM and ROBI
	 */
	public static Set<Ranking> getOtherScoringRankings() {
		return EnumSet.of(Ranking.SMM, Ranking.ROBI);
	}

	/**
	 * Get all ROBI-based rankings.
	 *
	 * @return set containing ROBI
	 */
	public static Set<Ranking> getRobiRankings() {
		return EnumSet.of(Ranking.ROBI);
	}

	/**
	 * Get all global scoring system rankings (all non-category rankings).
	 *
	 * @return set containing all global scoring systems
	 */
	public static Set<Ranking> getAllScoringRankings() {
		return EnumSet.of(
			Ranking.BW_SINCLAIR, Ranking.CAT_SINCLAIR, Ranking.CAT_QPOINTS,
			Ranking.SMM, Ranking.ROBI, Ranking.QPOINTS, Ranking.AGEFACTORS,
			Ranking.QAGE, Ranking.GAMX, Ranking.GAMX_M, Ranking.GAMX_U, Ranking.GAMX_A
		);
	}

	/**
	 * Get all category value rankings.
	 *
	 * @return set containing SNATCH, CLEANJERK, TOTAL, CUSTOM, SNATCH_CJ_TOTAL, CATEGORY_SCORE
	 */
	public static Set<Ranking> getCategoryRankings() {
		return EnumSet.of(
			Ranking.SNATCH, Ranking.CLEANJERK, Ranking.TOTAL, Ranking.CUSTOM,
			Ranking.SNATCH_CJ_TOTAL, Ranking.CATEGORY_SCORE
		);
	}
}
