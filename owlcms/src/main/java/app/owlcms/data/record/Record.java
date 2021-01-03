package app.owlcms.data.record;

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
public class Record {
    
    @Transient
    final private static Logger logger = (Logger) LoggerFactory.getLogger(Record.class);

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getRecordFederation() {
        return recordFederation;
    }
    public void setRecordFederation(String recordFederation) {
        this.recordFederation = recordFederation;
    }
    public String getAgeGrp() {
        return ageGrp;
    }
    public void setAgeGrp(String ageGrp) {
        this.ageGrp = ageGrp;
    }
    public Gender getGender() {
        return gender;
    }
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    public Integer getBwCatUpper() {
        return bwCatUpper;
    }
    public void setBwCatUpper(Integer bwCatUpper) {
        this.bwCatUpper = bwCatUpper;
    }
    public String getRecordKind() {
        return recordKind;
    }
    public void setRecordKind(String recordKind) {
        this.recordKind = recordKind;
    }
    public Integer getRecordValue() {
        return recordValue;
    }
    public void setRecordValue(Integer recordValue) {
        this.recordValue = recordValue;
    }
    public String getAthleteName() {
        return athleteName;
    }
    public void setAthleteName(String athleteName) {
        this.athleteName = athleteName;
    }
    public LocalDate getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    public Integer getBirthYear() {
        return birthYear;
    }
    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }
    public String getNation() {
        return nation;
    }
    public void setNation(String nation) {
        this.nation = nation;
    }
    public LocalDate getRecordeDate() {
        return recordeDate;
    }
    public void setRecordeDate(LocalDate recordeDate) {
        this.recordeDate = recordeDate;
    }
    public String getEventLocation() {
        return eventLocation;
    }
    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }
    public String getEvent() {
        return event;
    }
    public void setEvent(String event) {
        this.event = event;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    
    String recordFederation;
    String ageGrp;
    Gender gender;
    Integer bwCatUpper;
    String recordKind;
    Integer recordValue;
    String athleteName;
    LocalDate birthDate;
    Integer birthYear;
    String nation;
    LocalDate recordeDate;
    String eventLocation;
    String event;
}
