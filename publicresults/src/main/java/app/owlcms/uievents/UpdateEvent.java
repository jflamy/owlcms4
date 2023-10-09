/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import java.util.Objects;

public class UpdateEvent {

    private String athletes;
    private String attempt;
    private Integer breakRemaining;
    private BreakType breakType;
    private String categoryName;
    private String competitionName;
    private String fopName;
    private String fopState;
    private String fullName;
    private String groupName;
    private boolean hidden;
    private boolean indefinite;
    private Boolean isBreak;
    private String leaders;
    private String liftingOrderAthletes;
    private String liftsDone;
    private String noLiftRanks;
    private String recordKind;
    private String recordMessage;
    private String records;
    private boolean sinclairMeet;
    private Integer startNumber;
    private String teamName;
    private Integer timeAllowed;
    private String translationMap;
    private Integer weight;
    private boolean wideTeamNames;
    private String stylesDir;
    private String groupDescription;
    private CeremonyType ceremonyType;
    private String mode;
    private boolean done = false;

    public UpdateEvent() {
        setLeaders(this.leaders);
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
        return Objects.equals(this.athletes, other.athletes) && Objects.equals(this.attempt, other.attempt)
                && Objects.equals(this.breakRemaining, other.breakRemaining) && this.breakType == other.breakType
                && Objects.equals(this.categoryName, other.categoryName) && this.ceremonyType == other.ceremonyType
                && Objects.equals(this.competitionName, other.competitionName) && Objects.equals(this.fopName, other.fopName)
                && Objects.equals(this.fopState, other.fopState) && Objects.equals(this.fullName, other.fullName)
                && Objects.equals(this.groupDescription, other.groupDescription)
                && Objects.equals(this.groupName, other.groupName) && this.hidden == other.hidden
                && this.indefinite == other.indefinite && Objects.equals(this.isBreak, other.isBreak)
                && Objects.equals(this.leaders, other.leaders)
                && Objects.equals(this.liftingOrderAthletes, other.liftingOrderAthletes)
                && Objects.equals(this.liftsDone, other.liftsDone) && Objects.equals(this.noLiftRanks, other.noLiftRanks)
                && Objects.equals(this.recordKind, other.recordKind) && Objects.equals(this.recordMessage, other.recordMessage)
                && Objects.equals(this.records, other.records) && this.sinclairMeet == other.sinclairMeet
                && Objects.equals(this.startNumber, other.startNumber) && Objects.equals(this.stylesDir, other.stylesDir)
                && Objects.equals(this.teamName, other.teamName) && Objects.equals(this.timeAllowed, other.timeAllowed)
                && Objects.equals(this.translationMap, other.translationMap) && Objects.equals(this.weight, other.weight)
                && this.wideTeamNames == other.wideTeamNames;
    }

    public String getAthletes() {
        return this.athletes;
    }

    public String getAttempt() {
        return this.attempt;
    }

    public Integer getBreakRemaining() {
        return this.breakRemaining;
    }

    public BreakType getBreakType() {
        return this.breakType;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public CeremonyType getCeremonyType() {
        return this.ceremonyType;
    }

    public String getCompetitionName() {
        return this.competitionName;
    }

    public String getFopName() {
        return this.fopName;
    }

    public String getFopState() {
        return this.fopState;
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getGroupDescription() {
        return this.groupDescription;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public boolean getHidden() {
        return this.hidden;
    }

    public final Boolean getIsBreak() {
        return this.isBreak;
    }

    public String getLeaders() {
        return this.leaders;
    }

    public String getLiftingOrderAthletes() {
        return this.liftingOrderAthletes;
    }

    public String getLiftsDone() {
        return this.liftsDone;
    }

    public String getMode() {
        return this.mode;
    }

    public String getNoLiftRanks() {
        return this.noLiftRanks;
    }

    public String getRecordKind() {
        return this.recordKind;
    }

    public String getRecordMessage() {
        return this.recordMessage;
    }

    public String getRecords() {
        return this.records;
    }

    public Integer getStartNumber() {
        return this.startNumber;
    }

    public String getStylesDir() {
        return this.stylesDir;
    }

    public String getTeamName() {
        return this.teamName;
    }

    public Integer getTimeAllowed() {
        return this.timeAllowed;
    }

    public String getTranslationMap() {
        return this.translationMap;
    }

    public Integer getWeight() {
        return this.weight;
    }

    public boolean getWideTeamNames() {
        return this.wideTeamNames;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.athletes, this.attempt, this.breakRemaining, this.breakType, this.categoryName, this.ceremonyType, this.competitionName,
                this.fopName, this.fopState, this.fullName, this.groupDescription, this.groupName, this.hidden, this.indefinite, this.isBreak, this.leaders,
                this.liftingOrderAthletes, this.liftsDone, this.noLiftRanks, this.recordKind, this.recordMessage, this.records, this.sinclairMeet,
                this.startNumber, this.stylesDir, this.teamName, this.timeAllowed, this.translationMap, this.weight, this.wideTeamNames);
    }

    public Boolean isBreak() {
        return this.isBreak;
    }

    public final boolean isDone() {
        return this.done;
    }

    public boolean isIndefinite() {
        return this.indefinite;
    }

    public boolean isSinclairMeet() {
        return this.sinclairMeet;
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

    public void setCeremonyType(CeremonyType ceremonyType) {
        this.ceremonyType = ceremonyType;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public void setDone(boolean b) {
        this.done  = b;
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

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
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

    public final void setIsBreak(Boolean isBreak) {
        this.isBreak = isBreak;
    }

    public void setLeaders(String leaders) {
        this.leaders = leaders;
    }

    public void setLiftingOrderAthletes(String athletes) {
        this.liftingOrderAthletes = athletes;
    }

    public void setLiftsDone(String liftsDone) {
        this.liftsDone = liftsDone;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setNoLiftRanks(String parameter) {
        this.noLiftRanks = parameter;
    }

    public void setRecordKind(String kind) {
        this.recordKind = kind;
    }

    public void setRecordMessage(String recordMessage) {
        this.recordMessage = recordMessage;
    }

    public void setRecords(String records) {
        this.records = records;
    }

    public void setSinclairMeet(boolean sinclairMeet) {
        this.sinclairMeet = sinclairMeet;
    }

    public void setStartNumber(Integer parameter) {
        this.startNumber = parameter;
    }

    public void setStylesDir(String stylesDir) {
        this.stylesDir = stylesDir;
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
        return "UpdateEvent [groupName=" + this.groupName + ", timeAllowed=" + this.timeAllowed + ", fopName=" + this.fopName
                + ", fopState=" + this.fopState + ", isBreak=" + this.isBreak + ", breakType=" + this.breakType + ", breakRemaining="
                + this.breakRemaining + "]";
    }

}
