/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class TeamTreeItem extends Team {

    private static List<Group> doneGroups = null;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeItem.class);

    static public void buildTeamItemTree(Competition competition, Map<Gender, List<TeamTreeItem>> teamsByGender) {
        doneGroups = null; // force recompute.
        //TODO bring back mixed using Gender.values()
        for (Gender gender : Gender.mfValues()) {

            logger.debug("**************************************** Gender {} {}", gender, LoggerUtils.whereFrom());

            List<TeamTreeItem> curGenderTeams = teamsByGender.get(gender);
            if (curGenderTeams == null) {
                curGenderTeams = new ArrayList<TeamTreeItem>();
                teamsByGender.put(gender, curGenderTeams);
            }

            TeamTreeItem curTeam = null;
            List<Athlete> athletes = (List<Athlete>) competition.getGlobalTeamsRanking(gender);
            String prevTeamName = null;
            // count points for each team
            for (Athlete a : athletes) {
                // check if competition is a "best n results" team comp.
                // if the competition is "top n", we can have "top 4 men" + "top 2 women", so we want the athlete's
                // gender.
                Integer maxCount = getTopNTeamSize(a.getGender());
                String curTeamName = a.getTeam();
                curTeam = findCurTeam(teamsByGender, gender, curGenderTeams, prevTeamName, curTeam, curTeamName);
                boolean groupIsDone = groupIsDone(a);
                Float curPoints = a.getTotalPoints();

                int curTeamCount = 0;
                logger.debug("Athlete {} {} {} {} {} {}", curTeamName, a, a.getGender(), curPoints, curTeamCount,
                        groupIsDone);
                // results are ordered by total points

                boolean b = curTeamCount < maxCount;
                boolean c = curPoints != null && curPoints > 0;

                if (groupIsDone && b && c) {
                    curTeam.score = curTeam.score + Math.round(curPoints);
                    curTeam.counted += 1;
                }
                curTeam.addTreeItemChild(a);
                curTeamCount += 1;
                curTeam.size += 1;
                prevTeamName = curTeamName;
            }
        }

//        dumpTrees(teamsByGender);
    }

    private static TeamTreeItem findCurTeam(Map<Gender, List<TeamTreeItem>> teamsByGender, Gender gender,
            List<TeamTreeItem> curGenderTeams, String prevTeamName, TeamTreeItem curTeam, String curTeamName) {
        if (curTeam == null || prevTeamName == null || !curTeamName.contentEquals(prevTeamName)) {
            // maybe we have seen the team already (if mixed)
            TeamTreeItem found = null;
            for (TeamTreeItem ct : curGenderTeams) {
                if (ct.getName() != null && ct.getName().contentEquals(curTeamName)) {
                    found = ct;
                    break;
                }
            }
            if (found != null) {
                curTeam = found;
            } else {
                curTeam = new TeamTreeItem(curTeamName, gender, null);
                curTeam.size = AthleteRepository.countTeamMembers(curTeamName, gender);
                teamsByGender.get(gender).add(curTeam);
            }
        }
        return curTeam;
    }

    @SuppressWarnings("unused")
    private static void dumpTrees(Map<Gender, List<TeamTreeItem>> teamsByGender) {
        for (Gender g : Gender.values()) {
            List<TeamTreeItem> teams = teamsByGender.get(g);
            for (TeamTreeItem team : teams) {
                logger.debug("team: {} {}", team.getName(), team.getGender(), team.getScore());
                List<TeamTreeItem> teamMembers = team.getTeamMembers();
                teamMembers.sort(Team.scoreComparator);
                for (TeamTreeItem t : teamMembers) {
                    logger.debug("    {} {}", t.getName(), t.getScore());
                }
            }
        }
    }

    private static Integer getTopNTeamSize(Gender gender) {
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

    private static boolean groupIsDone(Athlete a) {
        if (doneGroups == null) {
            doneGroups = GroupRepository.findAll().stream().filter(g -> g.isDone()).collect(Collectors.toList());
        }
        return doneGroups.contains(a.getGroup());
    }

    private Athlete athlete;

    private TeamTreeItem parent;

    private List<TeamTreeItem> teamMembers;

    public TeamTreeItem(String curTeamName, Gender gender, Athlete teamMember) {
        super(curTeamName, gender);
        this.athlete = teamMember;
        if (this.athlete == null) {
            // we are a team
            this.teamMembers = new ArrayList<>();
        }
    }

    public Integer getCleanJerkPoints() {
        return toInteger(athlete.getCleanJerkPoints());
    }

    public Integer getCombinedPoints() {
        return toInteger(athlete.getCombinedPoints());
    }

    public Integer getCustomPoints() {
        return toInteger(athlete.getCustomPoints());
    }

    @Override
    public String getName() {
        if (athlete == null) {
            return super.getName();
        } else {
            return athlete.getFullName();
        }
    }

    public TeamTreeItem getParent() {
        return parent;
    }

    public Integer getSnatchPoints() {
        return toInteger(athlete.getSnatchPoints());
    }

    public String getTeam() {
        return athlete.getTeam();
    }

    public List<TeamTreeItem> getTeamMembers() {
        return teamMembers;
    }

    public Integer getTotalPoints() {
        return toInteger(athlete.getTotalPoints());
    }

    public void setParent(TeamTreeItem parent) {
        this.parent = parent;
    }

    public void setTeamMembers(List<TeamTreeItem> teamMembers) {
        this.teamMembers = teamMembers;
    }

    private void addTreeItemChild(Athlete a) {
        TeamTreeItem child = new TeamTreeItem(null, a.getGender(), a);
        child.setParent(this);
        teamMembers.add(child);
    }

    private Integer toInteger(Float f) {
        return f == null ? null : Math.round(f);
    }

    public Athlete getAthlete() {
        return athlete;
    }

}
