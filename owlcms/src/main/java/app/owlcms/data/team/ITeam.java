package app.owlcms.data.team;

import app.owlcms.data.athlete.Gender;

public interface ITeam {

    String getName();

    void setName(String name);

    int getScore();

    void setScore(int score);

    int getCounted();

    void setCounted(int counted);

    long getSize();

    void setSize(long size);

    Gender getGender();

    void setGender(Gender gender);

}