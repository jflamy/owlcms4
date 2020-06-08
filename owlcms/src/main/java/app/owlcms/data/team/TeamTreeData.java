/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.hierarchy.TreeData;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.ui.results.TeamResultsContent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TeamTreeData extends TreeData<TeamTreeItem> {

    private final Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeData.class);

    Map<Gender, List<TeamTreeItem>> teamsByGender = new EnumMap<>(Gender.class);

    private List<Group> doneGroups = null;

    private boolean debug = false;

    private Gender genderFilter;

    public TeamTreeData() {
        init();
    }

    public TeamTreeData(TeamResultsContent teamResultsContent) {
        genderFilter = teamResultsContent.getGenderFilter().getValue();
        init();
    }

    public Map<Gender, List<TeamTreeItem>> getTeamItemsByGender() {
        return teamsByGender;
    }

    private void buildTeamItemTree() {
        doneGroups = null; // force recompute.
        for (Gender gender : Gender.mfValues()) {
            if (genderFilter != null && gender != genderFilter) {
                continue;
            }

            logger.debug("**************************************** Gender {} {}", gender, LoggerUtils.whereFrom());

            List<TeamTreeItem> curGenderTeams = getTeamItemsByGender().get(gender);
            if (curGenderTeams == null) {
                curGenderTeams = new ArrayList<>();
                getTeamItemsByGender().put(gender, curGenderTeams);
                logger.debug("created list for gender {}: {}", gender, getTeamItemsByGender().get(gender));
            }

            TeamTreeItem curTeamItem = null;
            List<Athlete> athletes = (List<Athlete>) Competition.getCurrent().getGlobalTeamsRanking(gender);
            String prevTeamName = null;
            // count points for each team
            for (Athlete a : athletes) {
                // check if competition is a "best n results" team comp.
                // if the competition is "top n", we can have "top 4 men" + "top 2 women", so we want the athlete's
                // gender.
                Integer maxCount = getTopNTeamSize(a.getGender());
                String curTeamName = a.getTeam();
                curTeamItem = findCurTeamItem(getTeamItemsByGender(), gender, curGenderTeams, prevTeamName, curTeamItem,
                        curTeamName);
                boolean groupIsDone = groupIsDone(a);
                Integer curPoints = a.getTotalPoints();
                double curScore = a.getSinclairForDelta();

                int curTeamCount = 0;
                logger.debug("Athlete {} {} {} {} {} {}", curTeamName, a, a.getGender(), curPoints, curTeamCount,
                        groupIsDone);
                // results are ordered by total points

                boolean b = curTeamCount < maxCount;
                boolean c = curPoints != null && curPoints > 0;

                Team curTeam = curTeamItem.getTeam();

                if (groupIsDone && b && c) {
                    curTeam.setPoints(curTeam.getPoints() + Math.round(curPoints));
                    curTeam.setScore(curTeam.getScore() + curScore);
                    curTeam.setCounted(curTeam.getCounted() + 1);
                }
                curTeamItem.addTreeItemChild(a, groupIsDone);
                curTeamCount += 1;
                curTeam.setSize(curTeam.getSize() + 1);
                prevTeamName = curTeamName;
            }
        }

//        dumpTrees(teamsByGender);
    }

    private void dumpTeams() {
        for (Gender g : Gender.values()) {
            List<TeamTreeItem> teamItems = getTeamItemsByGender().get(g);
            if (teamItems == null) {
                continue;
            }
            for (TeamTreeItem item : teamItems) {
                logger.debug("team: {} {} {}", item.getName(), item.getGender(), item.getPoints(), item.getScore());
                List<TeamTreeItem> teamMembers = item.getTeamMembers();
                teamMembers.sort(TeamTreeItem.scoreComparator);
                for (TeamTreeItem t : teamMembers) {
                    logger.debug("    {} {}", t.getName(), t.getScore());
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
            throw new RuntimeException("Can't happen: there is no Top N mixed size");
        }
        return maxCount;
    }

    private boolean groupIsDone(Athlete a) {
        if (doneGroups == null) {
            doneGroups = GroupRepository.findAll().stream().filter(g -> g.isDone()).collect(Collectors.toList());
        }
        return doneGroups.contains(a.getGroup());
    }

    private void init() {
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        buildTeamItemTree();
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

}
