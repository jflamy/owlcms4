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
    private String liftsDone;
    private String translationMap;
    private String attempt;
    private String fullName;
    private String groupName;
    private String hidden;
    private String startNumber;
    private String teamName;
    private String weight;

    public UpdateEvent() {
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

    public String getLiftsDone() {
        return this.liftsDone;
    }

    public String getTranslationMap() {
        return this.translationMap;
    }

    public boolean getWideTeamNames() {
        return this.wideTeamNames;
    }

    public void setAthletes(String athletes) {
        this.athletes = athletes;
    }

    public void setAttempt(String parameter) {
        this.attempt = parameter;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setFullName(String parameter) {
        this.fullName = parameter;
    }

    public void setGroupName(String parameter) {
        this.groupName = parameter;
    }

    public String getAttempt() {
        return attempt;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getHidden() {
        return hidden;
    }

    public String getStartNumber() {
        return startNumber;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getWeight() {
        return weight;
    }

    public void setHidden(String parameter) {
        this.hidden = parameter;
    }

    public void setLeaders(String leaders) {
        this.leaders = leaders;
    }

    public void setLiftsDone(String liftsDone) {
        this.liftsDone = liftsDone;
    }

    public void setStartNumber(String parameter) {
        this.startNumber = parameter;
    }

    public void setTeamName(String parameter) {
        this.teamName = parameter;
    }

    public void setTranslationMap(String translationMap) {
        this.translationMap = translationMap;
    }

    public void setWeight(String parameter) {
        this.weight = parameter;
    }

    public void setWideTeamNames(boolean wideTeamNames) {
        this.wideTeamNames = wideTeamNames;
    }

}
