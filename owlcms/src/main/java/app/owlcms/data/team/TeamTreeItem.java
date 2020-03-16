/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class TeamTreeItem {
    
    public static Comparator<TeamTreeItem> scoreComparator = ((a, b) -> -ObjectUtils.compare(a.getScore(), b.getScore(), true));
    
    @SuppressWarnings("unused")
    private final Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeItem.class);
    
    private Athlete athlete;
    private boolean done;
    private TeamTreeItem parent;
    private Team team;

    private List<TeamTreeItem> teamMembers;

    public TeamTreeItem(String curTeamName, Gender gender, Athlete teamMember, boolean done) {
        this.athlete = teamMember;
        this.setDone(done);
        if (this.athlete == null) {
            // we are a team
            this.setTeam(new Team(curTeamName, gender));
            this.teamMembers = new ArrayList<>();
        }
    }

    public void addTreeItemChild(Athlete a, boolean done) {
        TeamTreeItem child = new TeamTreeItem(null, a.getGender(), a, done);
        child.setParent(this);
        teamMembers.add(child);
    }

    public String formatName() {
        if (athlete == null) {
            return Translator.translate("TeamResults.TeamNameFormat",getTeam().getName(),getTeam().getGender());
        } else {
            return athlete.getFullName();
        }
    }
    
    public String formatPoints() {
        Float pts = getPoints();
        return (pts == null ? "" : Integer.toString(Math.round(pts)));    
    }

    public String formatProgress() {
        if (athlete != null) {
            return isDone() ? Translator.translate("Done") : "";
        } else {
            return getTeam().getCounted() + "/" + getTeam().getSize();
        }
    }
    
    public Athlete getAthlete() {
        return athlete;
    }

    public Integer getCleanJerkPoints() {
        return toInteger(athlete.getCleanJerkPoints());
    }

    public Integer getCombinedPoints() {
        return toInteger(athlete.getCombinedPoints());
    }

    public Integer getCounted() {
        return team != null ? team.getCounted() : null;
    }

    public Integer getCustomPoints() {
        return toInteger(athlete.getCustomPoints());
    }
    
    public Gender getGender() {
        return team != null ? team.getGender() : athlete.getGender();
    }

    public String getName() {
        if (athlete == null) {
            return getTeam().getName();
        } else {
            return athlete.getFullName();
        }
    }

    public TeamTreeItem getParent() {
        return parent;
    }

    public Float getScore() {
        return (team != null ? team.getScore() : null);
    }
    
    public long getSize() {
        return team != null ? team.getSize() : null;
    }

    public Integer getSnatchPoints() {
        return toInteger(athlete.getSnatchPoints());
    }

    public List<TeamTreeItem> getSortedTeamMembers() {
        if (teamMembers == null) {
            return Collections.emptyList();
        }
        teamMembers.sort(Comparator.comparing(TeamTreeItem::getPoints, (a,b) -> ObjectUtils.compare(a, b, true)));
        return teamMembers;
    }

    public Team getTeam( ) {
        return team;
    }
    
    public List<TeamTreeItem> getTeamMembers() {
        if (teamMembers == null) {
            return Collections.emptyList();
        }
        return teamMembers;
    }

    public String getTeamName() {
        return athlete.getTeam();
    }
    
    public Integer getTotalPoints() {
        return toInteger(athlete != null ? athlete.getTotalPoints() : null);
    }

    public void setParent(TeamTreeItem parent) {
        this.parent = parent;
    }

    public void setTeamMembers(List<TeamTreeItem> teamMembers) {
        this.teamMembers = teamMembers;
    }

    private Float getPoints() {
        Float pts;
        if (athlete == null) {
            pts = getTeam().getScore();
        } else {
            pts = isDone() ? (float)getTotalPoints() : null;
        }
        return pts;
    }

    private boolean isDone() {
        return done;
    }

    private void setDone(boolean done) {
        this.done = done;
    }
    
    private void setTeam(Team team) {
        this.team = team;
    }

    private Integer toInteger(Float f) {
        return f == null ? null : Math.round(f);
    }

}
