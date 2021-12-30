/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
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