/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings("serial")
@Tag("resultsfull-template")
@JsModule("./components/ResultsLeadersRanks.js")
@JsModule("./components/AudioContext.js")
@Route("displays/resultsLeadersRanks")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class ResultsLeadersRanks extends Results {

    protected Logger logger = (Logger) LoggerFactory.getLogger(ResultsLeadersRanks.class);
    private LinkedHashMap<String, Participation> ageGroupMap;

    /**
     * Instantiates a new results board.
     */
    public ResultsLeadersRanks() {
        OwlcmsFactory.waitDBInitialized();
        timer.setOrigin(this);
        setDarkMode(true);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("ScoreboardMultiRanksTitle") + OwlcmsSession.getFopNameIfMultiple();
    }

    @Override
    protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
        String category;
        category = curCat != null ? curCat.getName() : "";
        ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
        ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
        ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
        Integer startNumber = a.getStartNumber();
        ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
        ja.put("category", category != null ? category : "");
        getAttemptsJson(a, liftOrderRank, fop);
        ja.put("sattempts", sattempts);
        ja.put("cattempts", cattempts);
        ja.put("total", formatInt(a.getTotal()));
        setCurrentAthleteParticipations(a);
        ja.put("snatchRanks", getRanksJson(a, Ranking.SNATCH, ageGroupMap));
        ja.put("cleanJerkRanks", getRanksJson(a, Ranking.CLEANJERK, ageGroupMap));
        ja.put("totalRanks", getRanksJson(a, Ranking.TOTAL, ageGroupMap));
        ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");

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
    }


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

    private JsonValue getRanksJson(Athlete a, Ranking r, LinkedHashMap<String, Participation> ageGroupMap2) {
        JsonArray ranks = Json.createArray();
        int i = 0;
        for (Entry<String, Participation> e : ageGroupMap.entrySet()) {
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
            ageGroupMap = new LinkedHashMap<>(fop.getAgeGroupMap());
            for (Entry<String, Participation> cape : ageGroupMap.entrySet()) {
                cape.setValue(null);
            }
            if (a != null) {
                // logger,debug(">>>setCurrentAthleteParticipations begin");
                // logger,debug("setting {}", a.getShortName());
                for (Participation p : a.getParticipations()) {
                    AgeGroup ag = p.getCategory() != null ? p.getCategory().getAgeGroup() : null;
                    if (ag != null) {
                        // logger,debug("athlete {} curRankings {} {}", a, ag.getCode(), p);
                        ageGroupMap.put(ag.getCode(), p);
                    }
                }
                // logger,debug("<<<setCurrentAthleteParticipations end");
            } else {
                // logger,debug("+++ cleared");
            }
        });
    }
}