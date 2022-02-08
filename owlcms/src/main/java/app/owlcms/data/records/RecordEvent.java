/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.time.LocalDate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import ch.qos.logback.classic.Logger;

@Entity
@Cacheable
public class RecordEvent {

    @Transient
    final private static Logger logger = (Logger) LoggerFactory.getLogger(RecordEvent.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    String recordFederation;
    String ageGrp;
    Gender gender;
    Integer bwCatUpper;
    RecordKind recordKind;
    Integer recordValue;
    String athleteName;
    LocalDate birthDate;
    Integer birthYear;
    String nation;
    LocalDate recordDate;
    String eventLocation;
    String event;

    private int recordYear;

    public String getAgeGrp() {
        return ageGrp;
    }

    public String getAthleteName() {
        return athleteName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public Integer getBwCatUpper() {
        return bwCatUpper;
    }

    public String getEvent() {
        return event;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public Gender getGender() {
        return gender;
    }

    public Long getId() {
        return id;
    }

    public String getNation() {
        return nation;
    }

    public LocalDate getRecordeDate() {
        return recordDate;
    }

    public String getRecordFederation() {
        return recordFederation;
    }

    public RecordKind getRecordKind() {
        return recordKind;
    }

    public Integer getRecordValue() {
        return recordValue;
    }

    public int getRecordYear() {
        return recordYear;
    }

    public void setAgeGrp(String ageGrp) {
        this.ageGrp = ageGrp;
    }

    public void setAthleteName(String athleteName) {
        this.athleteName = athleteName;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public void setBwCatUpper(Integer bwCatUpper) {
        this.bwCatUpper = bwCatUpper;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public void setRecordDate(LocalDate recordeDate) {
        this.recordDate = recordeDate;
    }

    public void setRecordFederation(String recordFederation) {
        this.recordFederation = recordFederation;
    }

    public void setRecordKind(String recordKind) {
        this.recordKind = RecordKind.valueOf(recordKind);
    }

    public void setRecordValue(Integer recordValue) {
        this.recordValue = recordValue;
    }

    public void setRecordYear(int parseInt) {
        this.recordYear = parseInt;
    }

    @Override
    public String toString() {
        return "RecordEvent [recordFederation=" + recordFederation + ", ageGrp=" + ageGrp + ", gender=" + gender
                + ", bwCatUpper=" + bwCatUpper + ", recordKind=" + recordKind + ", recordValue=" + recordValue
                + ", athleteName=" + athleteName + ", birthDate=" + birthDate + ", birthYear=" + birthYear + ", nation="
                + nation + ", recordDate=" + recordDate + ", eventLocation=" + eventLocation + ", event=" + event
                + ", recordYear=" + recordYear + "]";
    }
}
