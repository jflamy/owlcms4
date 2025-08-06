/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
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
	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
		boolean bestScore = Config.getCurrent().featureSwitch("displayBestScore");
		boolean bestScoreRank = Config.getCurrent().featureSwitch("displayBestScoreRank");
		
		String category;
		category = curCat != null ? curCat.getDisplayName() : "";
		String fullName;
		if (isAbbreviatedName()) {
			fullName = a.getAbbreviatedName() != null ? a.getAbbreviatedName() : "";
		} else {
			fullName = a.getFullName() != null ? a.getFullName() : "";
		}
		if (!a.isEligibleForIndividualRanking() && !fullName.isBlank()) {
			fullName = Translator.translate("Scoreboard.Extra/Invited", fullName);
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
		setCurrentAthleteRanks(a);
		ja.put("snatchRanks", getRanksJson(a, Ranking.SNATCH, this.getAgeGroupMap()));
		ja.put("cleanJerkRanks", getRanksJson(a, Ranking.CLEANJERK, this.getAgeGroupMap()));
		ja.put("totalRanks", getRanksJson(a, Ranking.TOTAL, this.getAgeGroupMap()));
		ja.put("group", a.getGroup().getName());
		ja.put("subCategory", a.getSubCategory());

		if (a.getComputedScoringSystem() != Ranking.TOTAL || bestScore || bestScoreRank) {
			ja.put("sinclair", computedScore(a));
			if (bestScoreRank) {
				ja.put("sinclairRank", computedScoreRank(a));
			}
		}

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

	private JsonValue getRanksJson(Athlete a, Ranking r, LinkedHashMap<String, Participation> ageGroupMap2) {
		logger.warn("************* a {} r {}", a.getShortName(), r);
		JsonArray ranks = Json.createArray();
		int i = 0;
		for (Entry<String, Participation> e : this.getAgeGroupMap().entrySet()) {
			Participation p = e.getValue();
			logger.warn("a {} k {} v {}", a.getShortName(), e.getKey(), p);
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
						ranks.set(i, formatRank(p.getCategoryScoreRank()));
						break;
					default:
						ranks.set(i, formatRank(p.getCategoryScoreRank()));
						break;
				}
			}
			i++;
		}
		return ranks;
	}
	
	private void setCurrentAthleteRanks(Athlete a) {
		if (isChampionshipRanks()) {
			setCurrentAthleteChampionshipRanks(a);
		} else {
			setCurrentAthleteAgeGroupRanks(a);
		}
	}

	private void setCurrentAthleteAgeGroupRanks(Athlete a) {
		OwlcmsSession.withFop(fop -> {
			// ensure that the columns are in the same order as the header
			this.setAgeGroupMap(new LinkedHashMap<>(fop.getAgeGroupMap()));
			for (Entry<String, Participation> cape : this.getAgeGroupMap().entrySet()) {
				cape.setValue(null);
			}
			if (a != null) {
				// logger,debug(">>>setCurrentAthleteParticipations begin");
				// logger,debug("setting {}", a.getShortName());
				for (Participation p : a.getParticipations()) {
					AgeGroup ag = p.getCategory() != null ? p.getCategory().getAgeGroup() : null;
					if (ag != null) {
						// logger,debug("athlete {} curRankings {} {}", a, ag.getCode(), p);
						this.getAgeGroupMap().put(ag.getCode(), p);
					}
				}
				// logger,debug("<<<setCurrentAthleteParticipations end");
			} else {
				// logger,debug("+++ cleared");
			}
		});
	}
	
	private void setCurrentAthleteChampionshipRanks(Athlete a) {
		OwlcmsSession.withFop(fop -> {
			// ensure that the columns are in the same order as the header
			for (Entry<String, Participation> cape : this.getAgeGroupMap().entrySet()) {
				cape.setValue(null);
			}
			if (a != null) {
				// logger,debug("setting {}", a.getShortName());
				for (Participation p : a.getParticipations()) {
					AgeGroup ag = p.getCategory() != null ? p.getCategory().getAgeGroup() : null;
					if (ag != null) {
						//logger.debug("athlete {} ag {} column {} p {}", a, ag.getCode(), getColumnName(ag), p);
						getAgeGroupMap().put(getColumnName(ag), p);
					}
				}
			} else {
				// logger,debug("+++ cleared");
			}
		});
	}
	
	@Override
	protected JsonArray getAgeGroupNamesJson(LinkedHashMap<String, Participation> currentAthleteParticipations) {
		if (isChampionshipRanks()) {
			return getChampionshipNamesJson(currentAthleteParticipations);
		}
		
		JsonArray ageGroups = Json.createArray();
		int i = 0;
		for (Entry<String, Participation> e : OwlcmsSession.getFop().getAgeGroupMap().entrySet()) {
			ageGroups.set(i, e.getKey());
			i++;
		}
		getElement().setProperty("nbRanks", "" + i);
		return ageGroups;
	}

	public boolean isChampionshipRanks() {
		var group2 = getGroup();
		return Config.getCurrent().featureSwitch("championshipGrouping") || (group2 != null && group2.isMasters());
	}

	
	private JsonArray getChampionshipNamesJson(LinkedHashMap<String, Participation> agMap) {
		// temporary
		var ag2 = new LinkedHashMap<String, Participation>();
		//setAgeGroupMap(new LinkedHashMap<String, Participation>(agMap));
		JsonArray ageGroups = Json.createArray();
		int i = 0;
		for (Entry<String, Participation> e : agMap.entrySet()) {
			AgeGroup ag = AgeGroupRepository.findByName(e.getKey());
			String championshipName = getColumnName(ag);
			if (!ag2.containsKey(championshipName)) {
				ageGroups.set(i, championshipName);
				ag2.put(championshipName,null);
				i++;
			}
		}
		setAgeGroupMap(ag2);
		getElement().setProperty("nbRanks", "" + i);
		return ageGroups;
	}
	
	private String getColumnName(AgeGroup ag) {
		var group2 = this.getFop() != null ? getFop().getGroup() : null;
		if (group2 == null) {
			// can't happen, and if somehow it does this will not be matched
			logger.error("getColumnName called with no active FOP {}",LoggerUtils.whereFrom());
			return "-";
		}
		var championshipName = (group2.isMasters()
				|| Config.getCurrent().featureSwitch("championshipGrouping")) ? ag.computeChampionshipName() : ag.getCode();
		return championshipName;
	}

	protected LinkedHashMap<String, Participation> getAgeGroupMap() {
		return ageGroupMap;
	}

	protected void setAgeGroupMap(LinkedHashMap<String, Participation> ageGroupMap) {
		//logger.debug("ageGroupMap {} {}", ageGroupMap, LoggerUtils.whereFrom());
		this.ageGroupMap = ageGroupMap;
	}
}