/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.utils.DateTimeUtils;
import ch.qos.logback.classic.Logger;

/**
 * Used to read groups from spreadsheet and perform Excel-to-Java conversions
 *
 * @author Jean-François Lamy
 *
 */
public class RGroup {
    final Logger logger = (Logger) LoggerFactory.getLogger(RGroup.class);

    Group group = new Group();

    String groupName;
    String platform;
    String notes;
    String weighinTime;
    String competitionTime;
    String weighInTO1;
    String weighInTO2;
    String announcer;
    String marshall;
    String timekeeper;
    String techController;
    String ref1;
    String ref2;
    String ref3;
    String jury1;
    String jury2;
    String jury3;
    String jury4;
    String jury5;
    String reserve;

    public String getAnnouncer() {
        return announcer;
    }

    public String getCompetitionTime() {
        return competitionTime;
    }

    public Group getGroup() {
        return group;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getJury1() {
        return jury1;
    }

    public String getJury2() {
        return jury2;
    }

    public String getJury3() {
        return jury3;
    }

    public String getJury4() {
        return jury4;
    }

    public String getJury5() {
        return jury5;
    }

    public String getMarshall() {
        return marshall;
    }

    public String getNotes() {
        return notes;
    }

    public String getPlatform() {
        return platform;
    }

    public String getRef1() {
        return ref1;
    }

    public String getRef2() {
        return ref2;
    }

    public String getRef3() {
        return ref3;
    }

    public String getReserve() {
        return reserve;
    }

    public String getTechController() {
        return techController;
    }

    public String getTimekeeper() {
        return timekeeper;
    }

    public String getWeighinTime() {
        return weighinTime;
    }

    public String getWeighInTO1() {
        return weighInTO1;
    }

    public String getWeighInTO2() {
        return weighInTO2;
    }

    public void setAnnouncer(String announcer) {
        group.setAnnouncer(announcer);
        this.announcer = announcer;
    }

    public void setCompetitionTime(String competitionTime) {
        if (competitionTime == null || competitionTime.isBlank()) {
            this.competitionTime = "";
            return;
        }
        LocalDateTime parseExcelDateTime = DateTimeUtils.parseExcelFractionalDate(competitionTime);
        parseExcelDateTime = parseExcelDateTime.withSecond(0).withNano(0);
        group.setCompetitionTime(parseExcelDateTime);
        this.competitionTime = parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " "
                + parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setGroupName(String groupName) {
        group.setName(groupName);
        this.groupName = groupName;
    }

    public void setJury1(String jury1) {
        group.setJury1(jury1);
        this.jury1 = jury1;
    }

    public void setJury2(String jury2) {
        group.setJury2(jury2);
        this.jury2 = jury2;
    }

    public void setJury3(String jury3) {
        group.setJury3(jury3);
        this.jury3 = jury3;
    }

    public void setJury4(String jury4) {
        group.setJury4(jury4);
        this.jury4 = jury4;
    }

    public void setJury5(String jury5) {
        group.setJury5(jury5);
        this.jury5 = jury5;
    }

    public void setMarshall(String marshall) {
        group.setMarshall(marshall);
        this.marshall = marshall;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPlatform(String pName) {
        // the existing platforms are reused, processed as part of the upload loop
        this.platform = pName;
    }

    public void setRef1(String ref1) {
        group.setReferee1(ref1);
        this.ref1 = ref1;
    }

    public void setRef2(String ref2) {
        group.setReferee2(ref2);
        this.ref2 = ref2;
    }

    public void setRef3(String ref3) {
        group.setReferee3(ref3);
        this.ref3 = ref3;
    }

    public void setReserve(String reserve) {
        group.setReserve(reserve);
        this.reserve = reserve;
    }

    public void setTechController(String techController) {
        group.setTechnicalController(techController);
        this.techController = techController;
    }

    public void setTimekeeper(String timekeeper) {
        group.setTimeKeeper(timekeeper);
        this.timekeeper = timekeeper;
    }

    public void setWeighinTime(String weighinTime) {
        if (weighinTime == null || weighinTime.isBlank()) {
            weighinTime = "";
            return;
        }
        LocalDateTime parseExcelDateTime = DateTimeUtils.parseExcelFractionalDate(weighinTime);
        parseExcelDateTime = parseExcelDateTime.withSecond(0).withNano(0);
        group.setWeighInTime(parseExcelDateTime);
        this.weighinTime = parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " "
                + parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public void setWeighInTO1(String weighInTO1) {
        group.setWeighIn1(weighInTO1);
        this.weighInTO1 = weighInTO1;
    }

    public void setWeighInTO2(String weighInTO2) {
        group.setWeighIn2(weighInTO2);
        this.weighInTO2 = weighInTO2;
    }

}
