/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
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
import app.owlcms.utils.IdUtils;
import ch.qos.logback.classic.Logger;

@Entity
@Cacheable
@Table(indexes = {
        @Index(name = "ix_category", columnList = "gender,ageGrpLower,ageGrpUpper,bwCatLower,bwCatUpper") })
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
@JsonInclude(Include.NON_NULL)
public class RecordEvent implements Comparable<RecordEvent> {

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
		newRecord.setGender(a.getGender());
		newRecord.setAthleteAge(a.getAge());
		newRecord.setAthleteBW(a.getBodyWeight());
		newRecord.setNation(a.getTeam());
		
		newRecord.setBwCatLower(rec.getBwCatLower());
		newRecord.setBwCatUpper(rec.getBwCatUpper());
		newRecord.setBwCatString(rec.getBwCatString());
		
		newRecord.setEvent(Competition.getCurrent().getCompetitionName());
		newRecord.setEventLocation(Competition.getCurrent().getCompetitionCity());
		newRecord.setRecordDate(LocalDate.now());
		newRecord.setRecordFederation(rec.getRecordFederation());
		newRecord.setRecordLift(rec.getRecordLift());
		newRecord.setRecordName(rec.getRecordName());
		newRecord.setRecordValue(value);
		newRecord.setRecordYear(LocalDate.now().getYear());
		newRecord.setFileName(rec.getFileName());
		
		newRecord.setGroupNameString(currentGroup != null ? currentGroup.getName() : null);
		logger.info("!!! new record {} {} {} {}",newRecord.getAthleteName(), newRecord.getAgeGrp(), newRecord.getRecordLift(), newRecord.getRecordValue());

		Category cat = a.getCategory();
		newRecord.setCategoryString(cat != null ? cat.getSafeName() : "");
		return newRecord;
	}

	private Double athleteBW;
	private Integer athleteAge;
	@Id
	// @GeneratedValue(strategy = GenerationType.AUTO)
	public Long id;
	public Double recordValue;
	public String ageGrp;
	public int ageGrpLower;
	public int ageGrpUpper;
	public String athleteName;
	public LocalDate birthDate;
	public Integer birthYear;
	public int bwCatLower;
	public Integer bwCatUpper;
	public String event;
	public String eventLocation;
	public Gender gender;
	public String nation;
	public LocalDate recordDate;
	public String recordFederation;
	public Ranking recordLift;
	public String recordName;
	public int recordYear;
	public String fileName;
	public String bwCatString;
	public String groupNameString;
	public String categoryString;
	@Transient
	@JsonIgnore
	private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public RecordEvent() {
		setId(IdUtils.getTimeBasedId());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof RecordEvent)) {
			return false;
		}

		RecordEvent other = (RecordEvent) o;
		return this.id != null && this.id.equals(other.getId());
	}

	public void fillDefaults() throws MissingAgeGroup, MissingGender, UnknownIWFBodyWeightCategory {
		if (this.ageGrp == null) {
			throw new MissingAgeGroup();
		}
		this.ageGrp = this.ageGrp.trim();
		this.ageGrp = this.ageGrp.toUpperCase();

		if (this.ageGrp.equals("YTH")) {
			this.ageGrpLower = this.ageGrpLower > 0 ? this.ageGrpLower : 13;
			this.ageGrpUpper = this.ageGrpUpper > 0 ? this.ageGrpUpper : 17;
		} else if (this.ageGrp.equals("JR")) {
			this.ageGrpLower = this.ageGrpLower > 0 ? this.ageGrpLower : 15;
			this.ageGrpUpper = this.ageGrpUpper > 0 ? this.ageGrpUpper : 20;
		} else if (this.ageGrp.equals("SR")) {
			this.ageGrpLower = this.ageGrpLower > 0 ? this.ageGrpLower : 15;
			this.ageGrpUpper = this.ageGrpUpper > 0 ? this.ageGrpUpper : 999;
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

	@Transient
	@JsonIgnore
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

	@Transient
	@JsonIgnore
	public String getKey() {
		return // getRecordFederation() + "_" +
		getRecordName() + "_" + getGender() + "_" + getRecordLift() + "_" + getBwCatLower() + "_" + getBwCatUpper() + "_"
		        + getAgeGrpLower() + "_" + getAgeGrpUpper();
	}

	public String getNation() {
		return this.nation;
	}

	public LocalDate getRecordDate() {
		return this.recordDate;
	}

	@Transient
	@JsonIgnore
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
		if (this.recordLift == null) {
			return "";
		}
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

	@Transient
	@JsonIgnore
	public String getTranslatedGender() {
		if (this.gender == null) {
			return "";
		}
		return this.gender.asGenderName();
	}

	@Transient
	@JsonIgnore
	public String getTranslatedLift() {
		if (this.recordLift == null) {
			return "";
		}
		return Translator.translate("Record." + this.recordLift);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
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
		return Objects.equals(this.gender, other.gender) && this.ageGrpLower == other.ageGrpLower
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

	// @Override
	// public int hashCode() {
	// return Objects.hash(this.ageGrp, this.ageGrpLower, this.ageGrpUpper, this.athleteName, this.birthDate,
	// this.birthYear, this.bwCatLower,
	// this.bwCatString, this.bwCatUpper, this.categoryString, this.event, this.eventLocation, this.gender,
	// this.groupNameString, this.id, this.nation,
	// this.recordDate, this.recordFederation, this.recordLift, this.recordName, this.recordValue,
	// this.fileName, this.recordYear);
	// }

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

	public void setTranslatedGender(String ignored) {
	}

	public void setTranslatedLift(String ignored) {
	}

	public void setBirth(String ignored) {
	}

	public void setKey(String ignored) {
	}

	public void setName(String ignored) {
	}

	public void setRecordDateAsString(String ignored) {
	}

	public void setResAthleteName(String ignored) {
	}

	public void setResRecordLift(String ignored) {
	}

	@Override
	public String toString() {
		return getKey();
		// return "RecordEvent [athleteBW=" + this.athleteBW + ", athleteAge=" + this.athleteAge + ", recordValue="
		// + this.recordValue
		// + ", ageGrp=" + this.ageGrp + ", ageGrpLower=" + this.ageGrpLower + ", ageGrpUpper=" + this.ageGrpUpper
		// + ", athleteName=" + this.athleteName + ", birthDate=" + this.birthDate + ", birthYear="
		// + this.birthYear
		// + ", bwCatLower=" + this.bwCatLower + ", bwCatUpper=" + this.bwCatUpper + ", event=" + this.event
		// + ", eventLocation="
		// + this.eventLocation + ", gender=" + this.gender + ", nation=" + this.nation + ", recordDate="
		// + this.recordDate
		// + ", recordFederation=" + this.recordFederation + ", recordLift=" + this.recordLift + ", recordName="
		// + this.recordName
		// + ", recordYear=" + this.recordYear + ", fileName=" + this.fileName + ", bwCatString="
		// + this.bwCatString
		// + ", groupNameString=" + this.groupNameString + ", categoryString=" + this.categoryString + "]";
	}

	private Integer computeAthleteAgeAtTimeOfRecord() {
		if (this.recordDate == null) {
			// logger.error("missing record date {} {}", this.athleteName, this.categoryString);
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

	public static Comparator<RecordEvent> sequentialOrderComparator() {
		return (a, b) -> {
			int compare;
			compare = ObjectUtils.compare(getIndex(a.getRecordName()), getIndex(b.getRecordName()));
			if (compare != 0) {
				// normally we have local - state - nation - continent - world; display "biggest" first
				return -compare;
			}
			compare = ObjectUtils.compare(a.getAgeGrpUpper(), b.getAgeGrpUpper());
			if (compare != 0) {
				// older age group first
				return -compare;
			}
			compare = ObjectUtils.compare(a.getRecordLift(), b.getRecordLift());
			if (compare != 0) {
				return compare;
			}
			return 0;
		};
	}

	public static Integer getIndex(String target) {
		ArrayList<String> recordOrder = RecordConfig.getCurrent().getRecordOrder();
		var index = IntStream.range(0, recordOrder.size())
		        .filter(i -> target.equals(recordOrder.get(i)))
		        .findFirst();
		return index.isEmpty() ? null : index.getAsInt();
	}

	public String prettyPrint() {
		return getRecordName() + " " + Translator.translate("Record."+getRecordLift()) + " " + getAgeGrp() + " " + getBwCatString();
	}

	@Transient
	@JsonIgnore
	public String getName() {
		return prettyPrint();
	}

	@Override
	public int compareTo(RecordEvent o) {
		return 0;
	}

}
