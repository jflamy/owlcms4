/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.export.v2.AthleteDTO;
import app.owlcms.data.export.v2.TeamDTO;
import app.owlcms.i18n.Translator;

/**
 * Exports athlete data in V2 DTO format for WebSocket transmission.
 * Handles conversion of athlete objects to serializable maps with displayInfo.
 */
public class AthleteExporter {

	/**
	 * Export session athletes in V2 DTO format with team mapping and complete displayInfo.
	 * Returns a list of maps that will be serialized by Jackson when the forwarder writes JSON.
	 * 
	 * @param athletes The session athletes in display/start order
	 * @param liftingOrder The current lifting order (used to determine classname: current/next)
	 * @return List of athlete data maps with athlete DTO and displayInfo
	 */
	public static List<Map<String, Object>> exportSessionAthletes(List<Athlete> athletes, List<Athlete> liftingOrder) {
		if (athletes == null || athletes.isEmpty()) {
			return Collections.emptyList();
		}

		// Build team map for consistent team ID references
		Map<String, TeamDTO> teamMap = buildTeamMap(athletes);
		
		// Determine current and next athlete IDs from lifting order for classname
		long currentId = (liftingOrder != null && liftingOrder.size() > 0) ? liftingOrder.get(0).getId() : -1L;
		long nextId = (liftingOrder != null && liftingOrder.size() > 1) ? liftingOrder.get(1).getId() : -1L;

		List<Map<String, Object>> result = new ArrayList<>();
		for (Athlete athlete : athletes) {
			AthleteDTO dto = AthleteDTO.fromAthlete(athlete, teamMap);
			
			// Determine this athlete's position in lifting order for attempt status
			int liftOrderRank = (athlete.getId() == currentId) ? 1 : ((athlete.getId() == nextId) ? 2 : 0);
			int attemptsDone = athlete.getAttemptsDone();

			// Build displayInfo with all precomputed display values
			Map<String, Object> displayInfo = buildDisplayInfo(athlete, dto, liftOrderRank, attemptsDone);

			Map<String, Object> sessionAthlete = new LinkedHashMap<>();
			// Add top-level athleteKey for readability (redundant with athlete.key inside)
			String athleteKeyTop = athlete.getKey() != null ? String.valueOf(athlete.getKey())
				: (athlete.getId() != null ? String.valueOf(athlete.getId()) : null);
			sessionAthlete.put("athleteKey", athleteKeyTop);
			sessionAthlete.put("athlete", dto);
			sessionAthlete.put("displayInfo", displayInfo);

			result.add(sessionAthlete);
		}

		return result;
	}

	/**
	 * Export leader entries with category spacers.
	 * 
	 * @param leaders List of athletes who are leaders
	 * @return List of maps including spacer entries between categories
	 */
	public static List<Map<String, Object>> exportLeaderEntries(List<Athlete> leaders) {
		if (leaders == null || leaders.isEmpty()) {
			return Collections.emptyList();
		}

		List<Map<String, Object>> baseEntries = exportSessionAthletes(leaders, null);
		if (baseEntries.isEmpty()) {
			return Collections.emptyList();
		}

		List<Map<String, Object>> result = new ArrayList<>();
		Category previousCategory = null;
		for (int i = 0; i < leaders.size(); i++) {
			Athlete athlete = leaders.get(i);
			Category currentCategory = athlete != null ? athlete.getCategory() : null;
			boolean categoryChanged = false;
			if (currentCategory != null) {
				categoryChanged = previousCategory == null || !currentCategory.sameAs(previousCategory);
			} else if (previousCategory != null) {
				categoryChanged = true;
			}

			if (categoryChanged) {
				result.add(createSpacerEntry());
				previousCategory = currentCategory;
			}

			Map<String, Object> entry = baseEntries.get(i);
			if (entry != null) {
				result.add(entry);
			}
		}

		return result;
	}

	/**
	 * Build an order list consisting of athlete keys with spacer entries inserted
	 * whenever category boundaries (start order) or lift-phase transitions
	 * (lifting order) occur.
	 * 
	 * @param athletes List of athletes
	 * @param startOrder true for start order (category spacers), false for lifting order (phase spacers)
	 * @return List of athlete keys (Integer) and spacer entries (Map)
	 */
	public static List<Object> getAthleteKeyEntries(List<Athlete> athletes, boolean startOrder) {
		if (athletes == null || athletes.isEmpty()) {
			return Collections.emptyList();
		}

		List<Object> entries = new ArrayList<>();
		Category prevCat = null;
		Athlete prevAth = null;

		for (Athlete athlete : athletes) {
			if (startOrder) {
				Category curCat = athlete.getCategory();
				if (curCat != null && !curCat.sameAs(prevCat)) {
					entries.add(createSpacerEntry());
					prevCat = curCat;
				}
			} else {
				if (prevAth == null ||
				        (athlete.getActuallyAttemptedLifts() >= 3 && prevAth.getActuallyAttemptedLifts() < 3)) {
					entries.add(createSpacerEntry());
				}
				prevAth = athlete;
			}

			Integer key = athlete.getKey();
			if (key != null) {
				entries.add(key);
			}
		}

		return entries;
	}

	/**
	 * Pick the most relevant attempt value in display order: actual -> change2 -> change1 -> declaration -> automaticProgression
	 * Returns a Map with "value" (Integer or nbsp) and "status" (AttemptStatus string value).
	 * 
	 * Phase logic (snatch vs CJ) is handled by the caller which adjusts liftOrderRank accordingly:
	 * - During snatch phase (attemptsDone 0-2): snatch attempts get liftOrderRank, CJ attempts get 0
	 * - During CJ phase (attemptsDone 3-5): CJ attempts get liftOrderRank, snatch attempts get 0
	 * 
	 * @param actual The actual lift result (positive=good, negative=fail, null=not attempted)
	 * @param change2 Second weight change
	 * @param change1 First weight change  
	 * @param declaration Original declaration
	 * @param automaticProgression Computed automatic progression (previous lift weight or +1kg)
	 * @param liftOrderRank 1=current athlete, 2=next athlete, 0=other (already phase-adjusted by caller)
	 * @return Map with "value" and "status" keys
	 */
	public static Map<String, Object> buildAttemptInfo(Integer actual, Integer change2, Integer change1, Integer declaration,
			Integer automaticProgression, int liftOrderRank) {
		Map<String, Object> result = new LinkedHashMap<>();
		
		if (actual != null) {
			// Attempt was done
			result.put("value", Math.abs(actual));
			result.put("status", (actual > 0 ? AttemptStatus.GOOD : AttemptStatus.BAD).getValue());
		} else {
			// Attempt not done yet - find the requested weight 
			// Priority: change2 > change1 > declaration > automaticProgression
			Integer requested = null;
			if (change2 != null) requested = change2;
			else if (change1 != null) requested = change1;
			else if (declaration != null) requested = declaration;
			else if (automaticProgression != null && automaticProgression > 0) requested = automaticProgression;
			
			if (requested != null) {
				result.put("value", requested);
				result.put("status", AttemptStatus.fromLiftOrderRank(liftOrderRank).getValue());
			} else {
				// No data at all -> mark explicitly as empty for display-ready output
				// Use a Unicode non-breaking space so the frontend has a printable cell value
				result.put("value", "\u00A0");
				result.put("status", AttemptStatus.EMPTY.getValue());
			}
		}
		
		return result;
	}

	/**
	 * Ensure only the first pending attempt (without actual lift) retains a status.
	 * Subsequent pending attempts are marked as empty to avoid showing multiple
	 * "current" or "request" markers for future attempts.
	 * 
	 * @param attempts List of attempt info maps with "status" keys
	 */
	private static void enforceFirstPendingOnly(List<Map<String, Object>> attempts) {
		boolean foundFirstPending = false;
		for (Map<String, Object> attempt : attempts) {
			String status = (String) attempt.get("status");
			// "good" and "bad" are completed lifts - skip them
			if (AttemptStatus.GOOD.getValue().equals(status) || AttemptStatus.BAD.getValue().equals(status)) {
				continue;
			}
			// This is a pending attempt (current, next, request, or empty)
			if (!foundFirstPending && !AttemptStatus.EMPTY.getValue().equals(status)) {
				// First pending attempt with a status - keep it
				foundFirstPending = true;
			} else if (foundFirstPending && !AttemptStatus.EMPTY.getValue().equals(status)) {
				// Subsequent pending attempt with status - mark as empty and clear value
				attempt.put("status", AttemptStatus.EMPTY.getValue());
				attempt.put("value", null);
			}
		}
	}

	/**
	 * Create a spacer entry for order lists.
	 */
	public static Map<String, Object> createSpacerEntry() {
		Map<String, Object> spacer = new LinkedHashMap<>();
		spacer.put("isSpacer", true);
		return spacer;
	}

	/**
	 * Build team map from athlete list for consistent team ID references.
	 */
	public static Map<String, TeamDTO> buildTeamMap(List<Athlete> athletes) {
		Map<String, TeamDTO> teamMap = new HashMap<>();
		if (athletes == null) {
			return teamMap;
		}
		for (Athlete athlete : athletes) {
			if (athlete == null) {
				continue;
			}
			String teamName = athlete.getTeam();
			if (teamName == null || teamName.trim().isEmpty() || teamMap.containsKey(teamName)) {
				continue;
			}
			TeamDTO teamDto = new TeamDTO();
			teamDto.setId(teamName.hashCode());
			teamDto.setName(teamName);
			teamMap.put(teamName, teamDto);
		}
		return teamMap;
	}

	/**
	 * Build displayInfo map with all precomputed display values for an athlete.
	 */
	private static Map<String, Object> buildDisplayInfo(Athlete athlete, AthleteDTO dto, int liftOrderRank, int attemptsDone) {
		Map<String, Object> displayInfo = new HashMap<>();

		// Only the active lift phase (snatch vs clean&jerk) may have the "current" or "next" markers.
		boolean inCjPhase = attemptsDone >= 3;

		// Attempt arrays with status info
		// Priority for requested weight: change2 > change1 > declaration > automaticProgression
		// Snatch1 has no automatic progression (first attempt)
		List<Map<String, Object>> sattemptsList = new ArrayList<>();
		int snatchLiftOrderRank = inCjPhase ? 0 : liftOrderRank;
		sattemptsList.add(buildAttemptInfo(dto.getSnatch1ActualLift(), dto.getSnatch1Change2(), dto.getSnatch1Change1(), dto.getSnatch1Declaration(), null, snatchLiftOrderRank));
		sattemptsList.add(buildAttemptInfo(dto.getSnatch2ActualLift(), dto.getSnatch2Change2(), dto.getSnatch2Change1(), dto.getSnatch2Declaration(), parseAutoProgression(dto.getSnatch2AutomaticProgression()), snatchLiftOrderRank));
		sattemptsList.add(buildAttemptInfo(dto.getSnatch3ActualLift(), dto.getSnatch3Change2(), dto.getSnatch3Change1(), dto.getSnatch3Declaration(), parseAutoProgression(dto.getSnatch3AutomaticProgression()), snatchLiftOrderRank));
		// Only the first pending attempt should have a status; subsequent pending attempts are empty
		enforceFirstPendingOnly(sattemptsList);
		displayInfo.put("sattempts", sattemptsList);

		// CleanJerk1 has no automatic progression (first C&J attempt)
		List<Map<String, Object>> cattemptsList = new ArrayList<>();
		int cjLiftOrderRank = inCjPhase ? liftOrderRank : 0;
		cattemptsList.add(buildAttemptInfo(dto.getCleanJerk1ActualLift(), dto.getCleanJerk1Change2(), dto.getCleanJerk1Change1(), dto.getCleanJerk1Declaration(), null, cjLiftOrderRank));
		cattemptsList.add(buildAttemptInfo(dto.getCleanJerk2ActualLift(), dto.getCleanJerk2Change2(), dto.getCleanJerk2Change1(), dto.getCleanJerk2Declaration(), parseAutoProgression(dto.getCleanJerk2AutomaticProgression()), cjLiftOrderRank));
		cattemptsList.add(buildAttemptInfo(dto.getCleanJerk3ActualLift(), dto.getCleanJerk3Change2(), dto.getCleanJerk3Change1(), dto.getCleanJerk3Declaration(), parseAutoProgression(dto.getCleanJerk3AutomaticProgression()), cjLiftOrderRank));
		// Only the first pending attempt should have a status; subsequent pending attempts are empty
		enforceFirstPendingOnly(cattemptsList);
		displayInfo.put("cattempts", cattemptsList);

		// Basic display fields
		displayInfo.put("fullName", athlete.getFullName() != null ? athlete.getFullName() : "");
		displayInfo.put("teamName", athlete.getTeam() != null ? athlete.getTeam() : "");
		displayInfo.put("yearOfBirth", athlete.getYearOfBirth() != null ? athlete.getYearOfBirth().toString() : "");
		displayInfo.put("gender", athlete.getGender() != null ? athlete.getGender().toString() : "");
		Integer startNumber = athlete.getStartNumber();
		displayInfo.put("startNumber", startNumber != null ? startNumber.toString() : "");
		Integer lotNumber = athlete.getLotNumber();
		displayInfo.put("lotNumber", lotNumber != null ? lotNumber.toString() : "");
		
		// Category with age group
		Category curCat = athlete.getCategory();
		displayInfo.put("category", curCat != null ? curCat.getNameWithAgeGroup() : "");
		
		// Best lifts and total
		displayInfo.put("bestSnatch", formatInt(athlete.getBestSnatch()));
		displayInfo.put("bestCleanJerk", formatInt(athlete.getBestCleanJerk()));
		displayInfo.put("total", formatInt(athlete.getTotal()));
		
		// Session ranks
		Participation mainRankings = athlete.getMainRankings();
		if (mainRankings != null) {
			displayInfo.put("snatchRank", formatInt(mainRankings.getSnatchRank()));
			displayInfo.put("cleanJerkRank", formatInt(mainRankings.getCleanJerkRank()));
			displayInfo.put("totalRank", formatInt(mainRankings.getTotalRank()));
		} else {
			displayInfo.put("snatchRank", "-");
			displayInfo.put("cleanJerkRank", "-");
			displayInfo.put("totalRank", "-");
		}
		
		// Sinclair/computed score
		displayInfo.put("sinclair", computedScore(athlete));
		displayInfo.put("sinclairRank", computedScoreRank(athlete));
		
		// Group and subcategory
		if (athlete.getGroup() != null) {
			displayInfo.put("group", athlete.getGroup().getName());
		}
		displayInfo.put("subCategory", athlete.getSubCategory());
		
		// Classname for highlighting current/next athlete
		boolean notDone = athlete.getAttemptsDone() < 6;
		String blink = (notDone ? " blink" : "");
		if (notDone) {
			displayInfo.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
		} else {
			displayInfo.put("classname", "");
		}
		
		// Custom fields
		displayInfo.put("custom1", athlete.getCustom1() != null ? athlete.getCustom1() : "");
		displayInfo.put("custom2", athlete.getCustom2() != null ? athlete.getCustom2() : "");
		displayInfo.put("membership", athlete.getMembership() != null ? athlete.getMembership() : "");
		
		// Team flag info
		String team = athlete.getTeam();
		if (team != null) {
			int teamLength = team.length();
			displayInfo.put("teamLength", teamLength);
			String flagPath = "/local/flags/" + team + ".svg";
			displayInfo.put("flagURL", flagPath);
			displayInfo.put("flagClass", teamLength <= Competition.SHORT_TEAM_LENGTH ? "shortTeam" : "longTeam");
		} else {
			displayInfo.put("teamLength", 0);
			displayInfo.put("flagURL", "");
			displayInfo.put("flagClass", "");
		}

		return displayInfo;
	}

	/**
	 * Parse automatic progression string to Integer.
	 * Returns null if the string is null, empty, or "0" (no automatic progression).
	 */
	private static Integer parseAutoProgression(String autoProgression) {
		if (autoProgression == null || autoProgression.isEmpty()) {
			return null;
		}
		try {
			int value = Integer.parseInt(autoProgression);
			return value > 0 ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String formatInt(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			return Translator.translate("Results.Extra/Invited");
		} else if (total < 0) {
			return "(" + Math.abs(total) + ")";
		} else {
			return total.toString();
		}
	}

	private static String computedScore(Athlete a) {
		Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
		double value = Ranking.getRankingValue(a, scoringSystem);
		return value > 0.001 ? String.format("%.3f", value) : "-";
	}

	private static String computedScoreRank(Athlete a) {
		Integer value = Ranking.getRanking(a, Competition.getCurrent().getScoringSystem());
		return value != null && value > 0 ? "" + value : "-";
	}
}
