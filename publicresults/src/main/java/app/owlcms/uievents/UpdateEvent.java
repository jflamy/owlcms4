/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import java.util.Objects;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

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
    private String groupInfo;
    private int hashCode;
    private Logger logger = (Logger) LoggerFactory.getLogger(UpdateEvent.class);
    private boolean showLiftRanks;
    private boolean showTotalRank;
    private boolean showSinclairRank;
    private boolean showSinclair;

    public UpdateEvent() {
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
                && Objects.equals(this.competitionName, other.competitionName) && this.done == other.done
                && Objects.equals(this.fopName, other.fopName) && Objects.equals(this.fopState, other.fopState)
                && Objects.equals(this.fullName, other.fullName)
                && Objects.equals(this.groupDescription, other.groupDescription)
                && Objects.equals(this.groupInfo, other.groupInfo) && Objects.equals(this.groupName, other.groupName)
                && this.hashCode == other.hashCode && this.hidden == other.hidden && this.indefinite == other.indefinite
                && Objects.equals(this.isBreak, other.isBreak) && Objects.equals(this.leaders, other.leaders)
                && Objects.equals(this.liftingOrderAthletes, other.liftingOrderAthletes)
                && Objects.equals(this.liftsDone, other.liftsDone) && Objects.equals(this.mode, other.mode)
                && Objects.equals(this.recordKind, other.recordKind)
                && Objects.equals(this.recordMessage, other.recordMessage)
                && Objects.equals(this.records, other.records)
                && Objects.equals(this.showLiftRanks, other.showLiftRanks)
                && Objects.equals(this.showSinclair, other.showSinclair)
                && Objects.equals(this.showSinclairRank, other.showSinclairRank)
                && Objects.equals(this.showTotalRank, other.showTotalRank) && this.sinclairMeet == other.sinclairMeet
                && Objects.equals(this.startNumber, other.startNumber)
                && Objects.equals(this.stylesDir, other.stylesDir)
                && Objects.equals(this.teamName, other.teamName) && Objects.equals(this.timeAllowed, other.timeAllowed)
                && Objects.equals(this.translationMap, other.translationMap)
                && Objects.equals(this.weight, other.weight)
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

    public String getGroupInfo() {
        return this.groupInfo;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public int getHashCode() {
        return this.hashCode;
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

    public Logger getLogger() {
        return this.logger;
    }

    public String getMode() {
        return this.mode;
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
        return Objects.hash(this.athletes, this.attempt, this.breakRemaining, this.breakType, this.categoryName,
                this.ceremonyType, this.competitionName,
                this.done, this.fopName, this.fopState, this.fullName, this.groupDescription, this.groupInfo,
                this.groupName, this.hashCode, this.hidden, this.indefinite,
                this.isBreak, this.leaders, this.liftingOrderAthletes, this.liftsDone, this.mode, this.recordKind,
                this.recordMessage, this.records,
                this.showLiftRanks, this.showSinclair, this.showSinclairRank, this.showTotalRank, this.sinclairMeet,
                this.startNumber, this.stylesDir,
                this.teamName, this.timeAllowed, this.translationMap, this.weight, this.wideTeamNames);
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

    public boolean isShowLiftRanks() {
        return this.showLiftRanks;
    }

    public boolean isShowSinclair() {
        return this.showSinclair;
    }

    public boolean isShowSinclairRank() {
        return this.showSinclairRank;
    }

    public boolean isShowTotalRank() {
        return this.showTotalRank;
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

    public void setBreakType(String string) {
        this.breakType = string != null ? BreakType.valueOf(string) : null;;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setCeremonyType(String string) {
        this.ceremonyType = string != null ? CeremonyType.valueOf(string) : null;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public void setDone(boolean b) {
        this.done = b;
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

    public void setGroupInfo(String groupInfo) {
        this.groupInfo = groupInfo;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
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

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public void setShowLiftRanks(boolean b) {
        this.showLiftRanks = b;
    }

    public void setShowSinclair(boolean showSinclair) {
        this.showSinclair = showSinclair;
    }

    public void setShowSinclairRank(boolean showSinclairRank) {
        this.showSinclairRank = showSinclairRank;
    }

    public void setShowTotalRank(boolean showTotalRanks) {
        this.showTotalRank = showTotalRanks;
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
        return "UpdateEvent [groupName=" + this.groupName + ", timeAllowed=" + this.timeAllowed + ", fopName="
                + this.fopName
                + ", fopState=" + this.fopState + ", isBreak=" + this.isBreak + ", breakType=" + this.breakType
                + ", breakRemaining="
                + this.breakRemaining + "]";
    }

    public void setBreakType(BreakType breakType) {
        this.breakType = breakType;
    }

    public void setCeremonyType(CeremonyType ceremonyType) {
        this.ceremonyType = ceremonyType;
    }

}
