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

    public static Comparator<Team> scoreComparator = ((a, b) -> -ObjectUtils.compare(a.score, b.score, true));

    public String name;

    public int score = 0;

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

    public int getScore() {
        return score;
    }

    public String getName() {
        return name;
    }
    public long getSize() {
        return size;
    }

    public void setCounted(int counted) {
        this.counted = counted;
    }
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    public void setScore(int score) {
        this.score = score;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
