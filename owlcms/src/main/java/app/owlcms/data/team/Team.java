/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.team;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Gender;

/**
 * A non-persistent class to assist in creating reports
 *
 * @author Jean-François Lamy
 */
public class Team {

    public static Comparator<Team> menComparator = ((a, b) -> -ObjectUtils.compare(a.menScore, b.menScore, true));

    public static Comparator<Team> womenComparator = ((a, b) -> -ObjectUtils.compare(a.womenScore, b.womenScore, true));

    public String name;

    public int menScore = 0;

    public int womenScore = 0;
    
    public int mixedScore = 0;

    public int counted;

    public long size;

    public Gender gender;

    public Team(String curTeamName, Gender gender) {
        name = curTeamName;
        this.gender = gender;
    }

    public int getCounted() {
        return counted;
    }

    public Gender getGender() {
        return gender;
    }

    public int getMenScore() {
        return menScore;
    }

    public String getName() {
        return name;
    }
    public long getSize() {
        return size;
    }
    public int getWomenScore() {
        return womenScore;
    }
    public void setCounted(int counted) {
        this.counted = counted;
    }
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    public void setMenScore(int menScore) {
        this.menScore = menScore;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setWomenScore(int womenScore) {
        this.womenScore = womenScore;
    }

}
