/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.competition.Competition;

/**
 * Configuration for which rankings should be computed.
 * Controls which scoring systems are calculated for athletes.
 * 
 * Rankings are computed if they are either:
 * - Required by age groups or global scoring system (mustCompute - read-only)
 * - Enabled by user preference (userEnabled - editable)
 */
public class RankingConfig {

	/** User-selected rankings (stored in database) */
	private static final EnumMap<Ranking, Boolean> userEnabled = new EnumMap<>(Ranking.class);
	
	/** Rankings required by age groups and global scoring system (computed at runtime) */
	private static final EnumSet<Ranking> mustCompute = EnumSet.noneOf(Ranking.class);

	static {
		// Initialize user preferences with defaults
		resetUserDefaults();
	}

	/**
	 * Check if a ranking should be computed.
	 * Returns true if the ranking is either required (mustCompute) or user-enabled.
	 *
	 * @param ranking the ranking to check
	 * @return true if the ranking should be computed, false otherwise
	 */
	public static boolean shouldCompute(Ranking ranking) {
		// Category rankings are always computed
		if (getCategoryRankings().contains(ranking)) {
			return true;
		}
		return mustCompute.contains(ranking) || userEnabled.getOrDefault(ranking, false);
	}

	/**
	 * Check if a ranking is required (cannot be disabled by user).
	 *
	 * @param ranking the ranking to check
	 * @return true if the ranking is required by age groups or global scoring
	 */
	public static boolean isMustCompute(Ranking ranking) {
		return mustCompute.contains(ranking);
	}

	/**
	 * Check if a ranking is user-enabled (editable preference).
	 *
	 * @param ranking the ranking to check
	 * @return true if the user has enabled this ranking
	 */
	public static boolean isUserEnabled(Ranking ranking) {
		return userEnabled.getOrDefault(ranking, false);
	}

	/**
	 * Set whether a ranking is user-enabled.
	 * This only affects user preferences, not mustCompute rankings.
	 *
	 * @param ranking the ranking to configure
	 * @param enabled true to enable, false to disable
	 */
	public static void setUserEnabled(Ranking ranking, boolean enabled) {
		userEnabled.put(ranking, enabled);
	}

	/**
	 * Legacy method for compatibility - delegates to setUserEnabled.
	 */
	public static void setCompute(Ranking ranking, boolean compute) {
		setUserEnabled(ranking, compute);
	}

	/**
	 * Update the mustCompute set based on age groups and global scoring system.
	 * Should be called after age groups are loaded/changed.
	 */
	public static void updateMustCompute() {
		mustCompute.clear();
		
		// Add global scoring system from Competition
		Competition comp = Competition.getCurrent();
		if (comp != null) {
			Ranking globalScoring = comp.getScoringSystem();
			if (globalScoring != null && getAllScoringRankings().contains(globalScoring)) {
				mustCompute.add(globalScoring);
			}
		}
		
		// Add scoring systems from all age groups
		List<AgeGroup> ageGroups = AgeGroupRepository.findAll();
		for (AgeGroup ag : ageGroups) {
			if (!ag.isActive()) {
				continue;
			}
			// Medal scoring system
			Ranking scoringSystem = ag.getComputedScoringSystem();
			if (scoringSystem != null && getAllScoringRankings().contains(scoringSystem)) {
				mustCompute.add(scoringSystem);
			}
			// Best athlete scoring system
			Ranking bestAthleteScoring = ag.getBestAthleteScoringSystem();
			if (bestAthleteScoring != null && getAllScoringRankings().contains(bestAthleteScoring)) {
				mustCompute.add(bestAthleteScoring);
			}
		}
	}

	/**
	 * Update mustCompute using a specific global scoring system.
	 * Age groups are read from the database.
	 * Used when the global scoring dropdown changes in the UI (before save).
	 */
	public static void updateMustCompute(Ranking globalScoring) {
		List<AgeGroup> ageGroups = AgeGroupRepository.findAll();
		updateMustCompute(ageGroups, globalScoring);
	}

	/**
	 * Update mustCompute from a provided list of age groups (used during import
	 * before age groups are persisted to database).
	 */
	public static void updateMustCompute(List<AgeGroup> ageGroups, Ranking globalScoring) {
		mustCompute.clear();
		
		// Add global scoring system
		if (globalScoring != null && getAllScoringRankings().contains(globalScoring)) {
			mustCompute.add(globalScoring);
		}
		
		// Add scoring systems from provided age groups
		if (ageGroups != null) {
			for (AgeGroup ag : ageGroups) {
				if (!ag.isActive()) {
					continue;
				}
				Ranking scoringSystem = ag.getComputedScoringSystem();
				if (scoringSystem != null && getAllScoringRankings().contains(scoringSystem)) {
					mustCompute.add(scoringSystem);
				}
				Ranking bestAthleteScoring = ag.getBestAthleteScoringSystem();
				if (bestAthleteScoring != null && getAllScoringRankings().contains(bestAthleteScoring)) {
					mustCompute.add(bestAthleteScoring);
				}
			}
		}
	}

	/**
	 * Get the current mustCompute set (read-only view).
	 */
	public static Set<Ranking> getMustCompute() {
		return EnumSet.copyOf(mustCompute);
	}

	/**
	 * Get the current user configuration as a copy.
	 *
	 * @return a copy of the current user preferences EnumMap
	 */
	public static EnumMap<Ranking, Boolean> getConfig() {
		return new EnumMap<>(userEnabled);
	}

	/**
	 * Reset user preferences to defaults.
	 */
	public static void resetUserDefaults() {
		userEnabled.clear();
		// Global scoring systems - reasonable defaults
		userEnabled.put(Ranking.BW_SINCLAIR, true);
		userEnabled.put(Ranking.CAT_SINCLAIR, false);
		userEnabled.put(Ranking.SMM, true);
		userEnabled.put(Ranking.CAT_QPOINTS, false);
		userEnabled.put(Ranking.ROBI, false);
		userEnabled.put(Ranking.QPOINTS, false);
		userEnabled.put(Ranking.AGEFACTORS, false);
		userEnabled.put(Ranking.QAGE, false);
		userEnabled.put(Ranking.GAMX, false);
		userEnabled.put(Ranking.GAMX_M, false);
		userEnabled.put(Ranking.GAMX_MS, false);
		userEnabled.put(Ranking.GAMX_MC, false);
		userEnabled.put(Ranking.GAMX_U, false);
		userEnabled.put(Ranking.GAMX_A, false);
		userEnabled.put(Ranking.GAMX_S, false);
		userEnabled.put(Ranking.GAMX_C, false);
		userEnabled.put(Ranking.CAT_GAMX, false);
	}

	/**
	 * Legacy method - resets user defaults only (mustCompute is derived).
	 */
	public static void resetDefaults() {
		resetUserDefaults();
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
	 * @return set containing GAMX, GAMX_M, GAMX_U, GAMX_A, and CAT_GAMX
	 */
	public static Set<Ranking> getGamxRankings() {
		return EnumSet.of(Ranking.GAMX, Ranking.GAMX_M, Ranking.GAMX_MS, Ranking.GAMX_MC, Ranking.GAMX_U, Ranking.GAMX_A, Ranking.GAMX_S, Ranking.GAMX_C, Ranking.CAT_GAMX);
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
			Ranking.BW_SINCLAIR, Ranking.SMM, Ranking.CAT_SINCLAIR,
			Ranking.QPOINTS, Ranking.AGEFACTORS, Ranking.QAGE, Ranking.CAT_QPOINTS,
			Ranking.GAMX, Ranking.GAMX_M, Ranking.GAMX_MS, Ranking.GAMX_MC, Ranking.GAMX_U, Ranking.GAMX_A, Ranking.GAMX_S, Ranking.GAMX_C, Ranking.CAT_GAMX, 
			Ranking.ROBI);
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
