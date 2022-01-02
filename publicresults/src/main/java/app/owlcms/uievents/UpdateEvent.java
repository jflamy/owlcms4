/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import java.util.Objects;

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
    private boolean hidden;
    private Integer startNumber;
    private String teamName;
    private Integer weight;
    private Integer timeAllowed;
    private String fopName;
    private String fopState;
    private String competitionName;
    private Boolean isBreak;
    private BreakType breakType;
    private Integer breakRemaining;
    private boolean indefinite;

    public UpdateEvent() {
        setLeaders(leaders);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        UpdateEvent other = (UpdateEvent) obj;
        return Objects.equals(athletes, other.athletes) && Objects.equals(attempt, other.attempt)
                && Objects.equals(breakRemaining, other.breakRemaining) && breakType == other.breakType
                && Objects.equals(categoryName, other.categoryName)
                && Objects.equals(competitionName, other.competitionName) && Objects.equals(fopName, other.fopName)
                && Objects.equals(fopState, other.fopState) && Objects.equals(fullName, other.fullName)
                && Objects.equals(groupName, other.groupName) && hidden == other.hidden
                && indefinite == other.indefinite && Objects.equals(isBreak, other.isBreak)
                && Objects.equals(leaders, other.leaders) && Objects.equals(liftsDone, other.liftsDone)
                && Objects.equals(startNumber, other.startNumber) && Objects.equals(teamName, other.teamName)
                && Objects.equals(timeAllowed, other.timeAllowed)
                && Objects.equals(translationMap, other.translationMap) && Objects.equals(weight, other.weight)
                && wideTeamNames == other.wideTeamNames;
    }

    public String getAthletes() {
        return this.athletes;
    }

    public String getAttempt() {
        return attempt;
    }

    public Integer getBreakRemaining() {
        return breakRemaining;
    }

    public BreakType getBreakType() {
        return breakType;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public String getCompetitionName() {
        return this.competitionName;
    }

    public String getFopName() {
        return fopName;
    }

    public String getFopState() {
        return fopState;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean getHidden() {
        return hidden;
    }

    public String getLeaders() {
        return leaders;
    }

    public String getLiftsDone() {
        return this.liftsDone;
    }

    public Integer getStartNumber() {
        return startNumber;
    }

    public String getTeamName() {
        return teamName;
    }

    public Integer getTimeAllowed() {
        return this.timeAllowed;
    }

    public String getTranslationMap() {
        return this.translationMap;
    }

    public Integer getWeight() {
        return weight;
    }

    public boolean getWideTeamNames() {
        return this.wideTeamNames;
    }

    @Override
    public int hashCode() {
        return Objects.hash(athletes, attempt, breakRemaining, breakType, categoryName, competitionName, fopName,
                fopState, fullName, groupName, hidden, indefinite, isBreak, leaders, liftsDone, startNumber, teamName,
                timeAllowed, translationMap, weight, wideTeamNames);
    }

    public Boolean isBreak() {
        return isBreak;
    }

    public boolean isIndefinite() {
        return this.indefinite;
    }

    public void setAthletes(String athletes) {
        this.athletes = athletes;
    }

    public void setAttempt(String parameter) {
        this.attempt = parameter;
    }

    public void setBreak(Boolean isBreak) {
        this.isBreak = isBreak;
    }

    public void setBreakRemaining(Integer milliseconds) {
        this.breakRemaining = milliseconds;
    }

    public void setBreakType(BreakType bt) {
        this.breakType = bt;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public void setFopName(String parameter) {
        this.fopName = parameter;
    }

    public void setFopState(String parameter) {
        this.fopState = parameter;
    }

    public void setFullName(String parameter) {
        this.fullName = parameter;
    }

    public void setGroupName(String parameter) {
        this.groupName = parameter;
    }

    public void setHidden(boolean parameter) {
        this.hidden = parameter;
    }

    public void setIndefinite(boolean indefinite) {
        this.indefinite = indefinite;
    }

    public void setLeaders(String leaders) {
        this.leaders = leaders;
    }

    public void setLiftsDone(String liftsDone) {
        this.liftsDone = liftsDone;
    }

    public void setStartNumber(Integer parameter) {
        this.startNumber = parameter;
    }

    public void setTeamName(String parameter) {
        this.teamName = parameter;
    }

    public void setTimeAllowed(Integer integer) {
        this.timeAllowed = integer;
    }

    public void setTranslationMap(String translationMap) {
        this.translationMap = translationMap;
    }

    public void setWeight(Integer integer) {
        this.weight = integer;
    }

    public void setWideTeamNames(boolean wideTeamNames) {
        this.wideTeamNames = wideTeamNames;
    }

    @Override
    public String toString() {
        return "UpdateEvent [groupName=" + groupName + ", timeAllowed=" + timeAllowed + ", fopName=" + fopName
                + ", fopState=" + fopState + ", isBreak=" + isBreak + ", breakType=" + breakType + ", breakRemaining="
                + breakRemaining + "]";
    }

}
