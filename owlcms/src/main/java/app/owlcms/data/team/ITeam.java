/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.team;

import app.owlcms.data.athlete.Gender;

public interface ITeam {

    int getCounted();

    Gender getGender();

    String getName();

    int getScore();

    long getSize();

    void setCounted(int counted);

    void setGender(Gender gender);

    void setName(String name);

    void setScore(int score);

    void setSize(long size);

}