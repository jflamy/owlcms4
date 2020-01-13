/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package publish;

public class UpdateEvent {

    private String leaders;
    private String categoryName;
    private boolean wideTeamNames;
    private String athletes;

    public UpdateEvent(String leaders) {
        setLeaders(leaders);
    }

    public String getAthletes() {
        return this.athletes;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public String getLeaders() {
        return leaders;
    }

    public boolean getWideTeamNames() {
        return this.wideTeamNames;
    }

    public void setAthletes(String athletes) {
        this.athletes = athletes;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setLeaders(String leaders) {
        this.leaders = leaders;
    }

    public void setWideTeamNames(boolean wideTeamNames) {
        this.wideTeamNames = wideTeamNames;
    }

    public String getLiftsDone() {
        // TODO Auto-generated method stub
        return null;
    }

}
