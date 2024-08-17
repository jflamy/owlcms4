/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Show ranks for multiple age group competitions
 */
@SuppressWarnings("serial")
@Tag("resultsfull-template")
@JsModule("./components/ResultsMulti.js")
@JsModule("./components/AudioContext.js")

public class ResultsMultiRanks extends Results {

	protected Logger logger = (Logger) LoggerFactory.getLogger(ResultsMultiRanks.class);
	private LinkedHashMap<String, Participation> ageGroupMap;

	public ResultsMultiRanks() {
		OwlcmsFactory.waitDBInitialized();
		getTimer().setOrigin(this);
	}

	@Override
	protected String formatRank(Integer total) {
		if (total == null) {
			return "&nbsp;";
		} else if (total == 0) {
			return "&ndash;";
		} else if (total == -1) {
			// invited lifter, not eligible.
			return Translator.translate("Results.Extra/Invited"); 
		} else {
			return total.toString();
		}
	}

	@Override
	protected JsonArray getAgeGroupNamesJson(LinkedHashMap<String, Participation> currentAthleteParticipations) {
		JsonArray ageGroups = Json.createArray();
		int i = 0;
		for (Entry<String, Participation> e : OwlcmsSession.getFop().getAgeGroupMap().entrySet()) {
			ageGroups.set(i, e.getKey());
			i++;
		}
		getElement().setProperty("nbRanks", "" + i);
		return ageGroups;
	}

	@Override
	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		String category;
		category = curCat != null ? curCat.getDisplayName() : "";
		String fullName;
		if (isAbbreviatedName()) {
			fullName = a.getAbbreviatedName() != null ? a.getAbbreviatedName() : "";
		} else {
			fullName = a.getFullName() != null ? a.getFullName() : "";
		}
		if (!a.isEligibleForIndividualRanking() && !fullName.isBlank() ) {
			fullName = Translator.translate("Scoreboard.Extra/Invited",fullName);
		}
		ja.put("fullName", fullName);
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank, fop);
		ja.put("sattempts", getSattempts());
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", getCattempts());
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));
		setCurrentAthleteParticipations(a);
		ja.put("snatchRanks", getRanksJson(a, Ranking.SNATCH, this.ageGroupMap));
		ja.put("cleanJerkRanks", getRanksJson(a, Ranking.CLEANJERK, this.ageGroupMap));
		ja.put("totalRanks", getRanksJson(a, Ranking.TOTAL, this.ageGroupMap));
		ja.put("group", a.getGroup().getName());
		ja.put("subCategory", a.getSubCategory());

		ja.put("sinclair", computedScore(a));
		ja.put("sinclairRank", computedScoreRank(a));

		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");

		boolean notDone = a.getAttemptsDone() < 6;
		String blink = (notDone ? " blink" : "");
		String highlight = "";
		if (fop.getState() != FOPState.DECISION_VISIBLE && notDone) {
			switch (liftOrderRank) {
				case 1:
					highlight = (" current" + blink);
					break;
				case 2:
					highlight = " next";
					break;
				default:
					highlight = "";
			}
		}
		// logger.debug("{} {} {}", a.getShortName(), fop.getState(), highlight);
		ja.put("classname", highlight);

		setTeamFlag(a, ja);
	}

	private String computedScore(Athlete a) {
		Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
		double value = Ranking.getRankingValue(a, scoringSystem);
		String score = value > 0.001 ? String.format("%.3f", value) : "-";
		return score;
	}

	private String computedScoreRank(Athlete a) {
		if (!a.isEligibleForIndividualRanking()) {
			return Translator.translate("Results.Extra/Invited");
		}
		Integer value = Ranking.getRanking(a, Competition.getCurrent().getScoringSystem());
		return value != null && value > 0 ? "" + value : "-";
	}

	private JsonValue getRanksJson(Athlete a, Ranking r, LinkedHashMap<String, Participation> ageGroupMap2) {
		JsonArray ranks = Json.createArray();
		int i = 0;
		for (Entry<String, Participation> e : this.ageGroupMap.entrySet()) {
			Participation p = e.getValue();
			// logger,debug("a {} k {} v {}", a.getShortName(), e.getKey(), p);
			if (p == null) {
				ranks.set(i, formatRank(null));
			} else {
				switch (r) {
					case CLEANJERK:
						ranks.set(i, formatRank(p.getCleanJerkRank()));
						break;
					case SNATCH:
						ranks.set(i, formatRank(p.getSnatchRank()));
						break;
					case TOTAL:
						ranks.set(i, formatRank(p.getTotalRank()));
						break;
					default:
						break;
				}
			}
			i++;
		}
		return ranks;
	}

	private void setCurrentAthleteParticipations(Athlete a) {
		OwlcmsSession.withFop(fop -> {
			this.ageGroupMap = new LinkedHashMap<>(fop.getAgeGroupMap());
			for (Entry<String, Participation> cape : this.ageGroupMap.entrySet()) {
				cape.setValue(null);
			}
			if (a != null) {
				// logger,debug(">>>setCurrentAthleteParticipations begin");
				// logger,debug("setting {}", a.getShortName());
				for (Participation p : a.getParticipations()) {
					AgeGroup ag = p.getCategory() != null ? p.getCategory().getAgeGroup() : null;
					if (ag != null) {
						// logger,debug("athlete {} curRankings {} {}", a, ag.getCode(), p);
						this.ageGroupMap.put(ag.getCode(), p);
					}
				}
				// logger,debug("<<<setCurrentAthleteParticipations end");
			} else {
				// logger,debug("+++ cleared");
			}
		});
	}
}