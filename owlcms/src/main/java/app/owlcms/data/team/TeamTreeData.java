/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.hierarchy.TreeData;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TeamTreeData extends TreeData<TeamTreeItem> {

    Map<Gender, List<TeamTreeItem>> teamsByGender = new EnumMap<>(Gender.class);

    private boolean debug = false;

    private List<Group> doneGroups = null;

    private Gender genderFilterValue;

    private final Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeData.class);

    private HashMap<String, Object> reportingBeans;

    private Ranking ranking;

    public TeamTreeData(String ageGroupPrefix, AgeDivision ageDivision, Gender gender, Ranking ranking) {
        genderFilterValue = gender;
        this.setRanking(ranking);
        init(ageGroupPrefix, ageDivision);
    }

    public Map<Gender, List<TeamTreeItem>> getTeamItemsByGender() {
        return teamsByGender;
    }

    private void buildTeamItemTree(HashMap<String, Object> reportingBeans2, String ageGroupPrefix,
            AgeDivision ageDivision) {
        doneGroups = null; // force recompute.
        if (ageDivision == null) {
            return;
        }
        for (Gender gender : Gender.mfValues()) {
            if (genderFilterValue != null && gender != genderFilterValue) {
                continue;
            }

            List<TeamTreeItem> curGenderTeams = getTeamItemsByGender().get(gender);
            if (curGenderTeams == null) {
                curGenderTeams = new ArrayList<>();
                getTeamItemsByGender().put(gender, curGenderTeams);
                logger.debug("created list for gender {}: {}", gender, getTeamItemsByGender().get(gender));
            }

            TeamTreeItem curTeamItem = null;
            String key = computeGenderKey(gender) + "Team"
                    + (ageGroupPrefix != null ? ageGroupPrefix : ageDivision.name());
            logger.debug("looking for {} in {}", key, reportingBeans.keySet());

            @SuppressWarnings("unchecked")
            List<Athlete> athletes = (List<Athlete>) reportingBeans.get(key);
            if (athletes == null) {
                return;
            }
            athletes = athletes.stream()
//                    .peek(a -> {
//                        logger.debug("{} {} {} {}",a.getShortName(), ((PAthlete) a)._getOriginalParticipation().getTeamMember(), a.getClass().getSimpleName(), ((PAthlete) a).getCategory());
//                    })
                    .filter(a -> a.isTeamMember())
                    .collect(Collectors.toList());
            AthleteSorter.teamPointsOrder(athletes, ranking);

            String prevTeamName = null;
            if (athletes != null) {
                // count points for each team
                for (Athlete a : athletes) {
                    // check if competition is a "best n results" team comp.
                    // if the competition is "top n", we can have "top 4 men" + "top 2 women", so we want the athlete's
                    // gender.
                    Integer maxCount = getTopNTeamSize(a.getGender());
                    String curTeamName = a.getTeam();
                    // logger.debug("a={} curTeam = {}",a, a.getTeam());
                    curTeamItem = findCurTeamItem(getTeamItemsByGender(), gender, curGenderTeams, prevTeamName,
                            curTeamItem,
                            curTeamName != null ? curTeamName : "-");
                    boolean groupIsDone = groupIsDone(a);
                    Integer curPoints = a.getTotalPoints();
                    double curSinclair = a.getSinclairForDelta();
                    double curSmf = a.getSmfForDelta();
                    double curRobi = a.getRobi();

                    Team curTeam = curTeamItem.getTeam();

                    boolean b = curTeam.getCounted() < maxCount;
                    boolean c = curPoints != null && curPoints > 0;

//                    if (debug) {
//                        logger.debug("---- Athlete {} {} {} {} {} {} {} {}", curTeamName, a, a.getGender(), curPoints,
//                                curTeam.getCounted(), groupIsDone, b, c);
//                    }

                    if (groupIsDone && b && c) {
                        curTeam.setPoints(curTeam.getPoints() + Math.round(curPoints));
                    }
                    if (b) {
                        curTeam.setSinclairScore(curTeam.getSinclairScore() + curSinclair);
                        curTeam.setSmfScore(curTeam.getSmfScore() + curSmf);
                        curTeam.setCounted(curTeam.getCounted() + 1);
                        curTeam.setRobi(curTeam.getRobi() + curRobi);
                    }
                    curTeamItem.addTreeItemChild(a, groupIsDone);
                    curTeam.setSize(curTeam.getSize() + 1);
                    prevTeamName = curTeamName;
                }
            }
        }

        dumpTeams();
    }

    private String computeGenderKey(Gender gender) {
        String genderKey;
        switch (gender) {
        case F:
            genderKey = "w";
            break;
        case M:
            genderKey = "m";
            break;
        default:
            genderKey = "mw";
            break;
        }
        return genderKey;
    }

    private void dumpTeams() {
        for (Gender g : Gender.values()) {
            List<TeamTreeItem> teamItems = getTeamItemsByGender().get(g);
            if (teamItems == null) {
                continue;
            }
            for (TeamTreeItem item : teamItems) {
                logger.debug("team: {} {} {} {} {}", item.getName(), item.getGender(), item.getPoints(),
                        item.getSinclairScore(), item.getCounted());
                List<TeamTreeItem> teamMembers = item.getTeamMembers();
                for (TeamTreeItem t : teamMembers) {
                    logger.debug("    {} {} {}", t.getName(), t.getPoints(), t.getSinclairScore());
                }
            }
        }
    }

    private TeamTreeItem findCurTeamItem(Map<Gender, List<TeamTreeItem>> teamItemsByGender, Gender gender,
            List<TeamTreeItem> curGenderTeams, String prevTeamName, TeamTreeItem curTeamItem, String curTeamName) {
        if (curTeamItem == null || prevTeamName == null || !curTeamName.contentEquals(prevTeamName)) {
            // maybe we have seen the team already (if mixed)
            TeamTreeItem found = null;
            for (TeamTreeItem ct : curGenderTeams) {
                if (ct.getName() != null && ct.getName().contentEquals(curTeamName)) {
                    found = ct;
                    break;
                }
            }
            if (found != null) {
                curTeamItem = found;
            } else {
                curTeamItem = new TeamTreeItem(curTeamName, gender, null, false);
                curTeamItem.getTeam().setSize(AthleteRepository.countTeamMembers(curTeamName, gender));
                teamItemsByGender.get(gender).add(curTeamItem);
            }
        }
        return curTeamItem;
    }

    private Integer getTopNTeamSize(Gender gender) {
        Integer maxCount = null;
        Competition comp = Competition.getCurrent();

        switch (gender) {
        case M:
            maxCount = comp.getMensTeamSize() != null ? comp.getMensTeamSize() : Integer.MAX_VALUE;
            break;
        case F:
            maxCount = comp.getWomensTeamSize() != null ? comp.getWomensTeamSize() : Integer.MAX_VALUE;
            break;
        case MIXED:
            throw new RuntimeException("Can't happen: there is no Top mixed size");
        }
        return maxCount;
    }

    private boolean groupIsDone(Athlete a) {
        if (doneGroups == null) {
            doneGroups = GroupRepository.findAll().stream().filter(g -> g.isDone()).collect(Collectors.toList());
        }
        return doneGroups.contains(a.getGroup());
    }

    private void init(String ageGroupPrefix, AgeDivision ageDivision) {
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        // logger.debug("init tree {} {}", ageGroupPrefix, ageDivision);
        reportingBeans = Competition.getCurrent().computeReportingInfo(ageGroupPrefix, ageDivision);
        buildTeamItemTree(reportingBeans, ageGroupPrefix, ageDivision);
        if (debug) {
            dumpTeams();
        }

        for (Gender g : Gender.values()) {
            List<TeamTreeItem> teams = getTeamItemsByGender().get(g);
            if (teams != null) {
                addItems(teams, TeamTreeItem::getSortedTeamMembers);
            }
        }
    }

    public Ranking getRanking() {
        return ranking;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }

}
