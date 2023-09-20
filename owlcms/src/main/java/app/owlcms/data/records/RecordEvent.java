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
	private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		RecordEvent other = (RecordEvent) obj;
		return Objects.equals(this.ageGrp, other.ageGrp) && this.ageGrpLower == other.ageGrpLower
				&& this.ageGrpUpper == other.ageGrpUpper && Objects.equals(this.athleteName, other.athleteName)
				&& Objects.equals(this.birthDate, other.birthDate) && Objects.equals(this.birthYear, other.birthYear)
				&& this.bwCatLower == other.bwCatLower && Objects.equals(this.bwCatString, other.bwCatString)
				&& Objects.equals(this.bwCatUpper, other.bwCatUpper)
				&& Objects.equals(this.categoryString, other.categoryString)
				&& Objects.equals(this.event, other.event) && Objects.equals(this.eventLocation, other.eventLocation)
				&& this.gender == other.gender && Objects.equals(this.groupNameString, other.groupNameString)
				&& Objects.equals(this.id, other.id) && Objects.equals(this.nation, other.nation)
				&& Objects.equals(this.recordDate, other.recordDate)
				&& Objects.equals(this.recordFederation, other.recordFederation) && this.recordLift == other.recordLift
				&& Objects.equals(this.recordName, other.recordName)
				&& Objects.equals(this.recordValue, other.recordValue)
				&& Objects.equals(this.fileName, other.fileName)
				&& this.recordYear == other.recordYear;
	}

	public void fillDefaults() throws MissingAgeGroup, MissingGender, UnknownIWFBodyWeightCategory {
		if (this.ageGrp == null) {
			throw new MissingAgeGroup();
		}
		this.ageGrp = this.ageGrp.trim();
		this.ageGrp = this.ageGrp.toUpperCase();

		boolean knownAgeGroup = true;
		if (this.ageGrp.equals("YTH")) {
			this.ageGrpLower = this.ageGrpLower > 0 ? this.ageGrpLower : 13;
			this.ageGrpUpper = this.ageGrpUpper > 0 ? this.ageGrpUpper : 17;
		} else if (this.ageGrp.equals("JR")) {
			this.ageGrpLower = this.ageGrpLower > 0 ? this.ageGrpLower : 15;
			this.ageGrpUpper = this.ageGrpUpper > 0 ? this.ageGrpUpper : 20;
		} else if (this.ageGrp.equals("SR")) {
			this.ageGrpLower = this.ageGrpLower > 0 ? this.ageGrpLower : 15;
			this.ageGrpUpper = this.ageGrpUpper > 0 ? this.ageGrpUpper : 999;
		} else {
			knownAgeGroup = false;
		}

		if (knownAgeGroup) {
			fillIWFBodyWeights();
		}
	}

	public String getAgeGrp() {
		return this.ageGrp;
	}

	public int getAgeGrpLower() {
		return this.ageGrpLower;
	}

	public int getAgeGrpUpper() {
		return this.ageGrpUpper;
	}

	@Transient
	@JsonIgnore
	public Integer getAthleteAge() {
		if (this.athleteAge == null) {
			return computeAthleteAgeAtTimeOfRecord();
		}
		return this.athleteAge;
	}

	public Double getAthleteBW() {
		if (this.athleteAge == null) {
			return computeAthleteBW();
		}
		return this.athleteBW;
	}

	public String getAthleteName() {
		return this.athleteName;
	}

	public String getBirth() {
		return (this.birthDate != null ? this.dateFormat.format(this.birthDate)
				: (this.birthYear != null ? Integer.toString(this.birthYear) : null));
	}

	public LocalDate getBirthDate() {
		return this.birthDate;
	}

	public Integer getBirthYear() {
		return this.birthYear;
	}

	public int getBwCatLower() {
		return this.bwCatLower;
	}

	public String getBwCatString() {
		return this.bwCatString;
	}

	public Integer getBwCatUpper() {
		return this.bwCatUpper;
	}

	/**
	 * @return the categoryString
	 */
	public String getCategoryString() {
		return this.categoryString;
	}

	public String getEvent() {
		return this.event;
	}

	public String getEventLocation() {
		return this.eventLocation;
	}

	public String getFileName() {
		return this.fileName;
	}

	public Gender getGender() {
		return this.gender;
	}

	/**
	 * Group Name. If not empty, record was set in the current competition.Autan
	 *
	 * @return the group name in the current competition
	 */
	public String getGroupNameString() {
		return this.groupNameString;
	}

	public Long getId() {
		return this.id;
	}

	public String getKey() {
		return getRecordName() + "_" + getRecordLift() + "_" + getBwCatLower() + "_" + getBwCatUpper() + "_"
				+ getAgeGrpLower() + "_" + getAgeGrpUpper();
	}

	public String getNation() {
		return this.nation;
	}

	public LocalDate getRecordDate() {
		return this.recordDate;
	}

	public String getRecordDateAsString() {
		if (this.recordDate == null) {
			return "";
		}
		return this.recordDate.format(this.dateFormat);
	}

	public String getRecordFederation() {
		return this.recordFederation;
	}

	public Ranking getRecordLift() {
		return this.recordLift;
	}

	public String getRecordName() {
		return this.recordName;
	}

	public Double getRecordValue() {
		return this.recordValue;
	}

	public int getRecordYear() {
		return this.recordYear;
	}

	@Transient
	@JsonIgnore
	public String getResAthleteName() {
		return (this.athleteName != null ? this.athleteName.replaceAll(",", "") : "");
	}

	@Transient
	@JsonIgnore
	public String getResRecordLift() {
		switch (this.recordLift) {
		case CLEANJERK:
			return Translator.translate("Results.Clean_and_Jerk");
		case SNATCH:
			return Translator.translate("Results.Snatch");
		case TOTAL:
			return Translator.translate("Results.Total");
		default:
			return this.recordLift.toString();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.ageGrp, this.ageGrpLower, this.ageGrpUpper, this.athleteName, this.birthDate,
				this.birthYear, this.bwCatLower,
				this.bwCatString, this.bwCatUpper, this.categoryString, this.event, this.eventLocation, this.gender,
				this.groupNameString, this.id, this.nation,
				this.recordDate, this.recordFederation, this.recordLift, this.recordName, this.recordValue,
				this.fileName, this.recordYear);
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
		return Objects.equals(this.ageGrp, other.ageGrp) && this.ageGrpLower == other.ageGrpLower
				&& this.ageGrpUpper == other.ageGrpUpper && Objects.equals(this.athleteName, other.athleteName)
				&& Objects.equals(this.birthDate, other.birthDate) && Objects.equals(this.birthYear, other.birthYear)
				&& this.bwCatLower == other.bwCatLower && Objects.equals(this.bwCatString, other.bwCatString)
				&& Objects.equals(this.bwCatUpper, other.bwCatUpper)
				&& Objects.equals(this.categoryString, other.categoryString)
				&& Objects.equals(this.event, other.event) && Objects.equals(this.eventLocation, other.eventLocation)
				&& Objects.equals(this.fileName, other.fileName) && this.gender == other.gender
				&& Objects.equals(this.groupNameString, other.groupNameString)
				&& Objects.equals(this.nation, other.nation)
				&& Objects.equals(this.recordDate, other.recordDate)
				&& Objects.equals(this.recordFederation, other.recordFederation) && this.recordLift == other.recordLift
				&& Objects.equals(this.recordName, other.recordName)
				&& Objects.equals(this.recordValue, other.recordValue)
				&& this.recordYear == other.recordYear;
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
		return Objects.equals(this.ageGrp, other.ageGrp) && this.ageGrpLower == other.ageGrpLower
				&& this.ageGrpUpper == other.ageGrpUpper && Objects.equals(this.athleteName, other.athleteName)
				&& Objects.equals(this.birthDate, other.birthDate) && Objects.equals(this.birthYear, other.birthYear)
				&& this.bwCatLower == other.bwCatLower && Objects.equals(this.bwCatString, other.bwCatString)
				&& Objects.equals(this.bwCatUpper, other.bwCatUpper)
				&& Objects.equals(this.categoryString, other.categoryString)
				&& Objects.equals(this.event, other.event) && Objects.equals(this.eventLocation, other.eventLocation)
				&& Objects.equals(this.fileName, other.fileName) && this.gender == other.gender
				&& Objects.equals(this.groupNameString, other.groupNameString)
				&& Objects.equals(this.nation, other.nation)
				&& Objects.equals(this.recordDate, other.recordDate)
				&& Objects.equals(this.recordFederation, other.recordFederation) && this.recordLift == other.recordLift
				&& Objects.equals(this.recordName, other.recordName)
				&& this.recordYear == other.recordYear;
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
		return "RecordEvent [athleteBW=" + this.athleteBW + ", athleteAge=" + this.athleteAge + ", recordValue="
				+ this.recordValue
				+ ", ageGrp=" + this.ageGrp + ", ageGrpLower=" + this.ageGrpLower + ", ageGrpUpper=" + this.ageGrpUpper
				+ ", athleteName=" + this.athleteName + ", birthDate=" + this.birthDate + ", birthYear="
				+ this.birthYear
				+ ", bwCatLower=" + this.bwCatLower + ", bwCatUpper=" + this.bwCatUpper + ", event=" + this.event
				+ ", eventLocation="
				+ this.eventLocation + ", gender=" + this.gender + ", nation=" + this.nation + ", recordDate="
				+ this.recordDate
				+ ", recordFederation=" + this.recordFederation + ", recordLift=" + this.recordLift + ", recordName="
				+ this.recordName
				+ ", recordYear=" + this.recordYear + ", fileName=" + this.fileName + ", bwCatString="
				+ this.bwCatString
				+ ", groupNameString=" + this.groupNameString + ", categoryString=" + this.categoryString + "]";
	}

	private Integer computeAthleteAgeAtTimeOfRecord() {
		if (this.recordDate == null) {
			logger.error("missing record date {} {}", this.athleteName, this.categoryString);
			return null;
		} else if (this.birthDate != null) {
			return Period.between(this.birthDate, this.recordDate).getYears();
		} else if (this.birthYear != null) {
			return Period.between(LocalDate.of(this.birthYear, 01, 01), this.recordDate).getYears();
		}
		return null;
	}

	private Double computeAthleteBW() {
		if (this.bwCatUpper != null) {
			return (double) this.bwCatUpper - 0.001D;
		} else {
			return null;
		}
	}

	private void fillIWFBodyWeights() throws MissingGender, UnknownIWFBodyWeightCategory {
		if (this.gender == null) {
			throw new MissingGender();
		}
		if (this.gender == Gender.F) {
			switch (this.bwCatUpper) {
			case 40:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 0;
				break;
			case 45:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 40;
				break;
			case 49:
				if (this.ageGrp.equals("YTH")) {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 45;
				} else {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 0;
				}
				break;
			case 55:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 49;
				break;
			case 59:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 55;
				break;
			case 64:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 59;
				break;
			case 71:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 64;
				break;
			case 76:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 71;
				break;
			case 81:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 76;
				break;
			case 87:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 81;
				break;
			case 999:
				if (this.ageGrp.equals("YTH")) {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 81;
				} else {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 87;
				}
				break;
			default:
				// throw new UnknownIWFBodyWeightCategory();
				// leave alone
			}
		} else {
			switch (this.bwCatUpper) {
			case 49:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 0;
				break;
			case 55:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 49;
				break;
			case 61:
				if (this.ageGrp.equals("YTH")) {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 55;
				} else {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 0;
				}
				break;
			case 67:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 61;
				break;
			case 73:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 67;
				break;
			case 81:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 73;
				break;
			case 89:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 81;
				break;
			case 96:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 89;
				break;
			case 102:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 96;
				break;
			case 109:
				this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 96;
				break;
			case 999:
				if (this.ageGrp.equals("YTH")) {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 102;
				} else {
					this.bwCatLower = this.bwCatLower > 0 ? this.bwCatLower : 109;
				}
				break;
			default:
				// throw new UnknownIWFBodyWeightCategory();
				// leave alone
			}

		}
	}

}
