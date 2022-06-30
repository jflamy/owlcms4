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
import app.owlcms.data.athleteSort.Ranking;
import ch.qos.logback.classic.Logger;

@Entity
@Cacheable
@Table(indexes = {
        @Index(name = "ix_category", columnList = "gender,ageGrpLower,ageGrpUpper,bwCatLower,bwCatUpper") })
@SuppressWarnings("serial")
public class RecordEvent {

    public class MissingAgeGroup extends Exception {
    }

    public class MissingGender extends Exception {
    }

    public class UnknownIWFBodyWeightCategory extends Exception {
    }

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
    private Ranking recordLift;
    private String recordName;
    private int recordYear;

    private String bwCatString;

    public void fillDefaults() throws MissingAgeGroup, MissingGender, UnknownIWFBodyWeightCategory {
        if (ageGrp == null) {
            throw new MissingAgeGroup();
        }
        ageGrp = ageGrp.trim();
        ageGrp = ageGrp.toUpperCase();
        if (ageGrp.equals("YTH")) {
            ageGrpLower = ageGrpLower > 0 ? ageGrpLower : 13;
            ageGrpUpper = ageGrpUpper > 0 ? ageGrpUpper : 17;
        } else if (ageGrp.equals("JR")) {
            ageGrpLower = ageGrpLower > 0 ? ageGrpLower : 15;
            ageGrpUpper = ageGrpUpper > 0 ? ageGrpUpper : 20;
        } else if (ageGrp.equals("SR")) {
            ageGrpLower = ageGrpLower > 0 ? ageGrpLower : 15;
            ageGrpUpper = ageGrpUpper > 0 ? ageGrpUpper : 999;
        } else {
            ageGrpLower = ageGrpLower > 0 ? ageGrpLower : 0;
            ageGrpUpper = ageGrpUpper > 0 ? ageGrpUpper : 999;
        }
        fillIWFBodyWeights();
    }

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

    public Ranking getRecordLift() {
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

    public void setRecordLift(Ranking recordLift) {
        this.recordLift = recordLift;
    }

    public void setRecordLift(String liftAbbreviation) {
        if (liftAbbreviation.toLowerCase().startsWith("s")) {
            this.recordLift = Ranking.SNATCH;
        } else if (liftAbbreviation.toLowerCase().startsWith("c")) {
            this.recordLift = Ranking.CLEANJERK;
        } else if (liftAbbreviation.toLowerCase().startsWith("t")) {
            this.recordLift = Ranking.TOTAL;
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
        return "RecordEvent [id=" + id + ", recordValue=" + recordValue + ", ageGrp=" + ageGrp + ", ageGrpLower="
                + ageGrpLower + ", ageGrpUpper=" + ageGrpUpper + ", athleteName=" + athleteName + ", bwCatLower="
                + bwCatLower + ", bwCatUpper=" + bwCatUpper + ", birthDate=" + birthDate + ", birthYear=" + birthYear
                + ", event=" + event + ", eventLocation=" + eventLocation + ", gender=" + gender + ", nation=" + nation
                + ", recordDate=" + recordDate + ", recordFederation=" + recordFederation + ", recordLift=" + recordLift
                + ", recordName=" + recordName + ", recordYear=" + recordYear + "]";
    }

    private void fillIWFBodyWeights() throws MissingGender, UnknownIWFBodyWeightCategory {
        if (gender == null) {
            throw new MissingGender();
        }
        if (gender == Gender.F) {
            switch (bwCatUpper) {
            case 40:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 0;
                break;
            case 45:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 40;
                break;
            case 49:
                if (ageGrp.equals("YTH")) {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 45;
                } else {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 0;
                }
                break;
            case 55:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 49;
                break;
            case 59:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 55;
                break;
            case 64:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 59;
                break;
            case 71:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 64;
                break;
            case 76:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 71;
                break;
            case 81:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 76;
                break;
            case 87:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 81;
                break;
            case 999:
                if (ageGrp.equals("YTH")) {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 81;
                } else {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 87;
                }
                break;
            default:
                throw new UnknownIWFBodyWeightCategory();
            }
        } else {
            switch (bwCatUpper) {
            case 49:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 0;
                break;
            case 55:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 49;
                break;
            case 61:
                if (ageGrp.equals("YTH")) {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 55;
                } else {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 0;
                }
                break;
            case 67:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 61;
                break;
            case 73:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 67;
                break;
            case 81:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 73;
                break;
            case 89:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 81;
                break;
            case 96:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 89;
                break;
            case 102:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 96;
                break;
            case 109:
                bwCatLower = bwCatLower > 0 ? bwCatLower : 96;
                break;
            case 999:
                if (ageGrp.equals("YTH")) {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 102;
                } else {
                    bwCatLower = bwCatLower > 0 ? bwCatLower : 109;
                }
                break;
            default:
                throw new UnknownIWFBodyWeightCategory();
            }

        }
    }

    public void setBwCatString(String cellValue) {
        this.bwCatString = cellValue;
    }

    public String getBwCatString() {
        return bwCatString;
    }
}
