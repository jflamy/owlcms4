/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@Entity
@Cacheable
@Table(indexes = {
        @Index(name = "ix_category", columnList = "gender,ageGrpLower,ageGrpUpper,bwCatLower,bwCatUpper") })
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
@JsonInclude(Include.NON_NULL)
public class RecordEvent {

	public class MissingAgeGroup extends Exception {
	}

	public class MissingGender extends Exception {
	}

	public class UnknownIWFBodyWeightCategory extends Exception {
	}

	@Transient
	final private static Logger logger = (Logger) LoggerFactory.getLogger(RecordEvent.class);

	public static RecordEvent newRecord(Athlete a, RecordEvent rec, Double value, Group currentGroup) {
		RecordEvent newRecord = new RecordEvent();
		newRecord.setAgeGrp(rec.getAgeGrp());
		newRecord.setAgeGrpLower(rec.getAgeGrpLower());
		newRecord.setAgeGrpUpper(rec.getAgeGrpUpper());
		newRecord.setAthleteName(a.getFullName());
		newRecord.setBirthDate(a.getFullBirthDate());
		newRecord.setBirthYear(a.getYearOfBirth());
		newRecord.setBwCatLower(rec.getBwCatLower());
		newRecord.setBwCatUpper(rec.getBwCatUpper());
		newRecord.setBwCatString(rec.getBwCatString());
		newRecord.setEvent(Competition.getCurrent().getCompetitionName());
		newRecord.setEventLocation(Competition.getCurrent().getCompetitionCity());
		newRecord.setGender(a.getGender());
		newRecord.setNation(a.getTeam());
		newRecord.setRecordDate(LocalDate.now());
		newRecord.setRecordFederation(rec.getRecordFederation());
		newRecord.setRecordLift(rec.getRecordLift());
		newRecord.setRecordName(rec.getRecordName());
		newRecord.setRecordValue(value);
		newRecord.setRecordYear(LocalDate.now().getYear());
		newRecord.setFileName(rec.getFileName());
		newRecord.setGroupNameString(currentGroup != null ? currentGroup.getName() : null);
		newRecord.setAthleteAge(a.getAge());
		newRecord.setAthleteBW(a.getBodyWeight());
		Category cat = a.getCategory();
		newRecord.setCategoryString(cat != null ? cat.getName() : "");
		return newRecord;
	}

	private Double athleteBW;

	private Integer athleteAge;

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
	private String fileName;
	private String bwCatString;
	private String groupNameString;
	private String categoryString;

	@Transient
	@JsonIgnore
	private DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		RecordEvent other = (RecordEvent) obj;
		return Objects.equals(ageGrp, other.ageGrp) && ageGrpLower == other.ageGrpLower
		        && ageGrpUpper == other.ageGrpUpper && Objects.equals(athleteName, other.athleteName)
		        && Objects.equals(birthDate, other.birthDate) && Objects.equals(birthYear, other.birthYear)
		        && bwCatLower == other.bwCatLower && Objects.equals(bwCatString, other.bwCatString)
		        && Objects.equals(bwCatUpper, other.bwCatUpper) && Objects.equals(categoryString, other.categoryString)
		        && Objects.equals(event, other.event) && Objects.equals(eventLocation, other.eventLocation)
		        && gender == other.gender && Objects.equals(groupNameString, other.groupNameString)
		        && Objects.equals(id, other.id) && Objects.equals(nation, other.nation)
		        && Objects.equals(recordDate, other.recordDate)
		        && Objects.equals(recordFederation, other.recordFederation) && recordLift == other.recordLift
		        && Objects.equals(recordName, other.recordName) && Objects.equals(recordValue, other.recordValue)
		        && Objects.equals(fileName, other.fileName)
		        && recordYear == other.recordYear;
	}

	public void fillDefaults() throws MissingAgeGroup, MissingGender, UnknownIWFBodyWeightCategory {
		if (ageGrp == null) {
			throw new MissingAgeGroup();
		}
		ageGrp = ageGrp.trim();
		ageGrp = ageGrp.toUpperCase();

		boolean knownAgeGroup = true;
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
			knownAgeGroup = false;
		}

		if (knownAgeGroup) {
			fillIWFBodyWeights();
		}
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

	public Integer getAthleteAge() {
		if (athleteAge == null) {
			return computeAthleteAge();
		}
		return athleteAge;
	}

	private Integer computeAthleteAge() {
		if (recordDate == null) {
			logger.error("missing record date {} {}",athleteName,categoryString);
		}
		if (birthDate != null) {
			return Period.between(birthDate, recordDate).getYears();
		} else if (birthYear != null) {
			return Period.between(LocalDate.of(birthYear, 01, 01), recordDate).getYears();
		}
		return null;
	}

	public Double getAthleteBW() {
		if (athleteAge == null) {
			return computeAthleteBW();
		}
		return athleteBW;
	}

	private Double computeAthleteBW() {
		return (double)bwCatUpper - 0.001D;
	}

	public String getAthleteName() {
		return athleteName;
	}

	public String getBirth() {
		return (birthDate != null ? dateFormat.format(birthDate)
		        : (birthYear != null ? Integer.toString(birthYear) : null));
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

	public String getBwCatString() {
		return bwCatString;
	}

	public Integer getBwCatUpper() {
		return bwCatUpper;
	}

	/**
	 * @return the categoryString
	 */
	public String getCategoryString() {
		return categoryString;
	}

	public String getEvent() {
		return event;
	}

	public String getEventLocation() {
		return eventLocation;
	}

	public String getFileName() {
		return fileName;
	}

	public Gender getGender() {
		return gender;
	}

	/**
	 * Group Name. If not empty, record was set in the current competition.Autan
	 *
	 * @return the group name in the current competition
	 */
	public String getGroupNameString() {
		return groupNameString;
	}

	public Long getId() {
		return id;
	}

	public String getKey() {
		return getRecordName() + "_" + getRecordLift() + "_" + getBwCatLower() + "_" + getBwCatUpper() + "_"
		        + getAgeGrpLower() + "_" + getAgeGrpUpper();
	}

	public String getNation() {
		return nation;
	}

	public LocalDate getRecordDate() {
		return recordDate;
	}

	public String getRecordDateAsString() {
		if (recordDate == null) {
			return "";
		}
		return recordDate.format(dateFormat);
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

	@Transient
	@JsonIgnore
	public String getResAthleteName() {
		return (athleteName != null ? athleteName.replaceAll(",", "") : "");
	}

	@Transient
	@JsonIgnore
	public String getResRecordLift() {
		switch (recordLift) {
		case CLEANJERK:
			return Translator.translate("Results.Clean_and_Jerk");
		case SNATCH:
			return Translator.translate("Results.Snatch");
		case TOTAL:
			return Translator.translate("Results.Total");
		default:
			return recordLift.toString();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(ageGrp, ageGrpLower, ageGrpUpper, athleteName, birthDate, birthYear, bwCatLower,
		        bwCatString, bwCatUpper, categoryString, event, eventLocation, gender, groupNameString, id, nation,
		        recordDate, recordFederation, recordLift, recordName, recordValue, fileName, recordYear);
	}

	/**
	 * The two records are equivalent (ignores Id in database)
	 *
	 * @param obj
	 * @return
	 */
	public boolean sameAs(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		RecordEvent other = (RecordEvent) obj;
		return Objects.equals(ageGrp, other.ageGrp) && ageGrpLower == other.ageGrpLower
		        && ageGrpUpper == other.ageGrpUpper && Objects.equals(athleteName, other.athleteName)
		        && Objects.equals(birthDate, other.birthDate) && Objects.equals(birthYear, other.birthYear)
		        && bwCatLower == other.bwCatLower && Objects.equals(bwCatString, other.bwCatString)
		        && Objects.equals(bwCatUpper, other.bwCatUpper) && Objects.equals(categoryString, other.categoryString)
		        && Objects.equals(event, other.event) && Objects.equals(eventLocation, other.eventLocation)
		        && Objects.equals(fileName, other.fileName) && gender == other.gender
		        && Objects.equals(groupNameString, other.groupNameString) && Objects.equals(nation, other.nation)
		        && Objects.equals(recordDate, other.recordDate)
		        && Objects.equals(recordFederation, other.recordFederation) && recordLift == other.recordLift
		        && Objects.equals(recordName, other.recordName) && Objects.equals(recordValue, other.recordValue)
		        && recordYear == other.recordYear;
	}

	/**
	 * The two records are the same record, excluding the record value
	 *
	 * @param obj
	 * @return
	 */
	public boolean sameRecordAs(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		RecordEvent other = (RecordEvent) obj;
		return Objects.equals(ageGrp, other.ageGrp) && ageGrpLower == other.ageGrpLower
		        && ageGrpUpper == other.ageGrpUpper && Objects.equals(athleteName, other.athleteName)
		        && Objects.equals(birthDate, other.birthDate) && Objects.equals(birthYear, other.birthYear)
		        && bwCatLower == other.bwCatLower && Objects.equals(bwCatString, other.bwCatString)
		        && Objects.equals(bwCatUpper, other.bwCatUpper) && Objects.equals(categoryString, other.categoryString)
		        && Objects.equals(event, other.event) && Objects.equals(eventLocation, other.eventLocation)
		        && Objects.equals(fileName, other.fileName) && gender == other.gender
		        && Objects.equals(groupNameString, other.groupNameString) && Objects.equals(nation, other.nation)
		        && Objects.equals(recordDate, other.recordDate)
		        && Objects.equals(recordFederation, other.recordFederation) && recordLift == other.recordLift
		        && Objects.equals(recordName, other.recordName)
		        && recordYear == other.recordYear;
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

	public void setAthleteAge(Integer age) {
		this.athleteAge = age;
	}

	public void setAthleteBW(Double bodyWeight) {
		this.athleteBW = bodyWeight;
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

	public void setBwCatString(String cellValue) {
		this.bwCatString = cellValue;
	}

	public void setBwCatUpper(Integer bwCatUpper) {
		this.bwCatUpper = bwCatUpper;
	}

	/**
	 * @param categoryString the categoryString to set
	 */
	public void setCategoryString(String categoryString) {
		this.categoryString = categoryString;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public void setEventLocation(String eventLocation) {
		this.eventLocation = eventLocation;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public void setGroupNameString(String groupName) {
		this.groupNameString = groupName;
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
		return "RecordEvent [athleteBW=" + athleteBW + ", athleteAge=" + athleteAge + ", recordValue=" + recordValue
		        + ", ageGrp=" + ageGrp + ", ageGrpLower=" + ageGrpLower + ", ageGrpUpper=" + ageGrpUpper
		        + ", athleteName=" + athleteName + ", birthDate=" + birthDate + ", birthYear=" + birthYear
		        + ", bwCatLower=" + bwCatLower + ", bwCatUpper=" + bwCatUpper + ", event=" + event + ", eventLocation="
		        + eventLocation + ", gender=" + gender + ", nation=" + nation + ", recordDate=" + recordDate
		        + ", recordFederation=" + recordFederation + ", recordLift=" + recordLift + ", recordName=" + recordName
		        + ", recordYear=" + recordYear + ", fileName=" + fileName + ", bwCatString=" + bwCatString
		        + ", groupNameString=" + groupNameString + ", categoryString=" + categoryString + "]";
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
				// throw new UnknownIWFBodyWeightCategory();
				// leave alone
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
				// throw new UnknownIWFBodyWeightCategory();
				// leave alone
			}

		}
	}

}
