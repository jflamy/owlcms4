/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import ch.qos.logback.classic.Logger;

public class TeamTreeItem extends Team {
    
    @SuppressWarnings("unused")
    private final Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeItem.class);

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
        if (teamMembers == null) {
            return Collections.emptyList();
        }
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

    public void addTreeItemChild(Athlete a) {
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
