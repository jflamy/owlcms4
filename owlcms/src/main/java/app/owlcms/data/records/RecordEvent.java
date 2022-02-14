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
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import ch.qos.logback.classic.Logger;

@Entity
@Cacheable
@Table(indexes = {
        @Index(name = "ix_category", columnList = "gender,ageGrpLower,ageGrpUpper,bwCatLower,bwCatUpper") })
public class RecordEvent {

    @Transient
    final private static Logger logger = (Logger) LoggerFactory.getLogger(RecordEvent.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    Double recordValue;
    private String ageGrp;
    private int ageGrpLower;
    private int ageGrpUpper;
    private String athleteName;
    private LocalDate birthDate;
    private Integer birthYear;
    private int bwCatLower;
    private Integer bwCatUpper;
    private String event;
    private String eventLocation;
    private Gender gender;
    private String nation;
    private LocalDate recordDate;
    private String recordFederation;
    private RecordKind recordLift;
    private String recordName;
    private int recordYear;

    public String getAgeGrp() {
        return ageGrp;
    }

    public int getAgeGrpLower() {
        return ageGrpLower;
    }

    public int getAgeGrpUpper() {
        return ageGrpUpper;
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

    public int getBwCatLower() {
        return bwCatLower;
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

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public LocalDate getRecordeDate() {
        return recordDate;
    }

    public String getRecordFederation() {
        return recordFederation;
    }

    public RecordKind getRecordLift() {
        return recordLift;
    }

    public String getRecordName() {
        return recordName;
    }

    public Double getRecordValue() {
        return recordValue;
    }

    public int getRecordYear() {
        return recordYear;
    }

    public void setAgeGrp(String ageGrp) {
        this.ageGrp = ageGrp;
    }

    public void setAgeGrpLower(int intExact) {
        this.ageGrpLower = intExact;
    }

    public void setAgeGrpUpper(int intExact) {
        this.ageGrpUpper = intExact;
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

    public void setBwCatLower(int intExact) {
        this.bwCatLower = intExact;
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

    public void setRecordLift(RecordKind recordLift) {
        this.recordLift = recordLift;
    }

    public void setRecordLift(String liftAbbreviation) {
        if (liftAbbreviation.toLowerCase().startsWith("s")) {
            this.recordLift = RecordKind.SNATCH;
        } else if (liftAbbreviation.toLowerCase().startsWith("c")) {
            this.recordLift = RecordKind.CJ;
        } else if (liftAbbreviation.toLowerCase().startsWith("t")) {
            this.recordLift = RecordKind.TOTAL;
        } else {
            throw new IllegalArgumentException("recordLift");
        }
    }

    public void setRecordName(String cellValue) {
        this.recordName = cellValue;
    }

    public void setRecordValue(double d) {
        this.recordValue = d;
    }

    public void setRecordValue(Double recordValue) {
        this.recordValue = recordValue;
    }

    public void setRecordYear(int parseInt) {
        this.recordYear = parseInt;
    }

    @Override
    public String toString() {
        return "RecordEvent [recordFederation=" + recordFederation + ", ageGrp=" + ageGrp + ", gender=" + gender
                + ", bwCatUpper=" + bwCatUpper + ", recordLift=" + recordLift + ", recordValue=" + recordValue
                + ", athleteName=" + athleteName + ", birthDate=" + birthDate + ", birthYear=" + birthYear + ", nation="
                + nation + ", recordDate=" + recordDate + ", eventLocation=" + eventLocation + ", event=" + event
                + ", recordYear=" + recordYear + ", bwCatLower=" + bwCatLower + ", ageGrpLower=" + ageGrpLower
                + ", ageGrpUpper=" + ageGrpUpper + ", recordName=" + recordName + "]";
    }
}
