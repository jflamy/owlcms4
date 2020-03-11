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

    public String name;
    public int menScore = 0;
    public int womenScore = 0;
    public int counted;
    public long size;
    public Gender gender;
    
    public static Comparator<Team> menComparator = ((a, b) -> -ObjectUtils.compare(a.menScore, b.menScore, true));

    public static Comparator<Team> womenComparator = ((a, b) -> -ObjectUtils.compare(a.womenScore, b.womenScore, true));

    public Team(String curTeamName, Gender gender) {
        name = curTeamName;
        this.gender = gender;
    }

}
