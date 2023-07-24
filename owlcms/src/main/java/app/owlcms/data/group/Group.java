/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.group;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.utils.DateTimeUtils;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Logger;

/**
 * The Class Group.
 */

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity(name = "CompetitionGroup")
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Group.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Group implements Comparable<Group> {

	private final static NaturalOrderComparator<String> c = new NaturalOrderComparator<>();

	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

	private final static DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder().parseLenient()
	        .appendPattern(DATE_FORMAT).toFormatter();

	private static DisplayGroup displayGroup;

	/** The platform. */
	@ManyToOne(cascade = { CascadeType.MERGE }, optional = true, fetch = FetchType.EAGER)
	@JsonIdentityReference(alwaysAsId = true)
	Platform platform;

	private String announcer;

	/** The competition short date time. */
	private LocalDateTime competitionTime;
	private String description;

	@Column(columnDefinition = "boolean default false")
	private boolean done;

	@Id
	// @GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String jury1;
	private String jury2;
	private String jury3;
	private String jury4;
	private String jury5;

	@Transient
	final private Logger logger = (Logger) LoggerFactory.getLogger(Group.class);

	private String marshall;
	private String marshal2;
	private String name;

	private String referee1;
	private String referee2;
	private String referee3;

	private String reserve;

	private String technicalController;
	private String technicalController2;

	private String timeKeeper;

	private String weighIn1;
	private String weighIn2;

	private LocalDateTime weighInTime;

	/**
	 * Instantiates a new group.
	 */
	public Group() {
		setId(IdUtils.getTimeBasedId());
	}

	/**
	 * Instantiates a new group.
	 *
	 * @param groupName the group name
	 */
	public Group(String groupName) {
		setId(IdUtils.getTimeBasedId());
		this.name = groupName;
		final LocalDateTime now = LocalDateTime.now();
		this.setWeighInTime(now);
		this.setCompetitionTime(now);
	}

	/**
	 * Instantiates a new group.
	 *
	 * @param groupName   the group name
	 * @param weighin     the weighin
	 * @param competition the competition
	 */
	public Group(String groupName, LocalDateTime weighin, LocalDateTime competition) {
		setId(IdUtils.getTimeBasedId());
		this.name = groupName;
		this.setWeighInTime(weighin);
		this.setCompetitionTime(competition);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Group obj) {

		if (this == obj) {
			return 0;
		}
		if (obj == null) {
			return -1;
		}
		Group other = obj;
		if (name == null) {
			if (other.name != null) {
				return 1;
			} else {
				return 0;
			}
		} else {
			if (other.name != null) {
				return c.compare(name, other.name);
			} else {
				return -1;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareToWeighIn(Group obj) {
		if (this == obj) {
			return 0;
		}
		if (obj == null) {
			return -1;
		}
		int compare = ObjectUtils.compare(this.getWeighInTime(), obj.getWeighInTime(), true);
		if (compare != 0) {
			return compare;
		}
		compare = ObjectUtils.compare(this.getPlatform(), obj.getPlatform(), true);
		if (compare != 0) {
			return compare;
		}
		compare = ObjectUtils.compare(this, obj, true);
		return compare;
	}

	public void doDone(boolean b) {
		logger.debug("done? {} previous={} done={} {} [{}]", getName(), this.done, b, System.identityHashCode(this),
		        LoggerUtils.whereFrom());
		if (this.done != b) {
			this.setDone(b);
			GroupRepository.save(this);
		}
	}

	@Override
	public boolean equals(Object obj) {
		// https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		Group other = (Group) obj;
		return getId() != null && getId().equals(other.getId());

		// public boolean equals(Object obj) {
		// if (this == obj) {
		// return true;
		// }
		// if (obj == null) {
		// return false;
		// }
		// if (getClass() != obj.getClass()) {
		// return false;
		// }
		// Group other = (Group) obj;
		// return Objects.equals(name, other.name)
		// && Objects.equals(announcer, other.announcer) &&
		// Objects.equals(competitionTime, other.competitionTime)
		// && Objects.equals(id, other.id) && Objects.equals(jury1, other.jury1)
		// && Objects.equals(jury2, other.jury2) && Objects.equals(jury3, other.jury3)
		// && Objects.equals(jury4, other.jury4) && Objects.equals(jury5, other.jury5)
		// && Objects.equals(marshall, other.marshall) && Objects.equals(platform,
		// other.platform)
		// && Objects.equals(referee1, other.referee1)
		// && Objects.equals(referee2, other.referee2) && Objects.equals(referee3,
		// other.referee3)
		// && Objects.equals(technicalController, other.technicalController)
		// && Objects.equals(timeKeeper, other.timeKeeper) &&
		// Objects.equals(weighInTime, other.weighInTime);
		// }
	}

//    @Override

	/**
	 * Gets the announcer.
	 *
	 * @return the announcer
	 */
	public String getAnnouncer() {
		return announcer;
	}

	/**
	 * Gets the competition short date time.
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getCompetitionShortDateTime() {
		String formatted = "";
		try {
			LocalDateTime competitionTime2 = getCompetitionTime();
			formatted = competitionTime2 == null ? "" : DATE_TIME_FORMATTER.format(competitionTime2);
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the competition time.
	 *
	 * @return the competition time
	 */
	public LocalDateTime getCompetitionTime() {
		return competitionTime;
	}

	@Transient
	@JsonIgnore
	public Date getCompetitionTimeAsDate() {
		return DateTimeUtils.dateFromLocalDateTime(competitionTime);
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Gets the jury.
	 *
	 * @return the jury
	 */
	@JsonIgnore
	public String getJury() {
		List<String> jurors = Arrays.asList(jury1, jury2, jury3, jury4, jury5);
		Iterables.removeIf(jurors, Predicates.isNull());
		return String.join(", ", jurors);
	}

	/**
	 * @return the jury1
	 */
	public String getJury1() {
		return jury1;
	}

	/**
	 * @return the jury2
	 */
	public String getJury2() {
		return jury2;
	}

	/**
	 * @return the jury3
	 */
	public String getJury3() {
		return jury3;
	}

	/**
	 * @return the jury4
	 */
	public String getJury4() {
		return jury4;
	}

	/**
	 * @return the jury5
	 */
	public String getJury5() {
		return jury5;
	}

	public String getMarshal2() {
		return marshal2;
	}

	/**
	 * Gets the marshall.
	 *
	 * @return the marshall
	 */
	public String getMarshall() {
		return marshall;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the platform.
	 *
	 * @return the platformName on which group will be lifting
	 */
	public Platform getPlatform() {
		return platform;
	}

	/**
	 * Gets the referee 1.
	 *
	 * @return the referee 1
	 */
	public String getReferee1() {
		return referee1;
	}

	/**
	 * Gets the referee 2.
	 *
	 * @return the referee 2
	 */
	public String getReferee2() {
		return referee2;
	}

	/**
	 * Gets the referee 3.
	 *
	 * @return the referee 3
	 */
	public String getReferee3() {
		return referee3;
	}

	/**
	 * @return the reserve
	 */
	public String getReserve() {
		return reserve;
	}

	/**
	 * Gets the technical controller.
	 *
	 * @return the technical controller
	 */
	public String getTechnicalController() {
		return technicalController;
	}

	public String getTechnicalController2() {
		return technicalController2;
	}

	/**
	 * Gets the time keeper.
	 *
	 * @return the time keeper
	 */
	public String getTimeKeeper() {
		return timeKeeper;
	}

	public String getWeighIn1() {
		return weighIn1;
	}

	public String getWeighIn2() {
		return weighIn2;
	}

	/**
	 * Gets the weigh in short date time.
	 *
	 * @return the weigh-in time (two hours before competition, normally)
	 */
	@Transient
	@JsonIgnore
	public String getWeighInShortDateTime() {
		String formatted = "";
		try {
			LocalDateTime weighInTime2 = getWeighInTime();
			formatted = weighInTime2 == null ? "" : DATE_TIME_FORMATTER.format(weighInTime2);
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the weigh in time.
	 *
	 * @return the weigh-in time (two hours before competition, normally)
	 */
	public LocalDateTime getWeighInTime() {
		return weighInTime;
	}

	@Transient
	@JsonIgnore
	public Date getWeighInTimeAsDate() {
		return DateTimeUtils.dateFromLocalDateTime(weighInTime);
	}

	@Override
	public int hashCode() {
		// https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
		return 31;
	}

	public boolean isDone() {
		return done;
	}

	/**
	 * Sets the announcer.
	 *
	 * @param announcer the new announcer
	 */
	public void setAnnouncer(String announcer) {
		this.announcer = announcer;
	}

	/**
	 * Sets the competition time.
	 *
	 * @param c the competition time to set
	 */
	public void setCompetitionTime(LocalDateTime c) {
		this.competitionTime = c;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		// logger.debug("settingId {} {}\\n{}",id,name,LoggerUtils.stackTrace());
		this.id = id;
	}

	/**
	 * @param jury1 the jury1 to set
	 */
	public void setJury1(String jury1) {
		this.jury1 = jury1;
	}

	/**
	 * @param jury2 the jury2 to set
	 */
	public void setJury2(String jury2) {
		this.jury2 = jury2;
	}

	/**
	 * @param jury3 the jury3 to set
	 */
	public void setJury3(String jury3) {
		this.jury3 = jury3;
	}

	/**
	 * @param jury4 the jury4 to set
	 */
	public void setJury4(String jury4) {
		this.jury4 = jury4;
	}

	/**
	 * @param jury5 the jury5 to set
	 */
	public void setJury5(String jury5) {
		this.jury5 = jury5;
	}

	public void setMarshal2(String marshal2) {
		this.marshal2 = marshal2;
	}

	/**
	 * Sets the marshall.
	 *
	 * @param announcer the new marshall
	 */
	public void setMarshall(String announcer) {
		this.marshall = announcer;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the platform.
	 *
	 * @param platform the new platform
	 */
	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	/**
	 * Sets the referee 1.
	 *
	 * @param referee1 the new referee 1
	 */
	public void setReferee1(String referee1) {
		this.referee1 = referee1;
	}

	/**
	 * Sets the referee 2.
	 *
	 * @param referee2 the new referee 2
	 */
	public void setReferee2(String referee2) {
		this.referee2 = referee2;
	}

	/**
	 * Sets the referee 3.
	 *
	 * @param referee3 the new referee 3
	 */
	public void setReferee3(String referee3) {
		this.referee3 = referee3;
	}

	/**
	 * @param reserve the reserve to set
	 */
	public void setReserve(String reserve) {
		this.reserve = reserve;
	}

	/**
	 * Sets the technical controller.
	 *
	 * @param technicalController the new technical controller
	 */
	public void setTechnicalController(String technicalController) {
		this.technicalController = technicalController;
	}

	public void setTechnicalController2(String technicalController2) {
		this.technicalController2 = technicalController2;
	}

	/**
	 * Sets the time keeper.
	 *
	 * @param timeKeeper the new time keeper
	 */
	public void setTimeKeeper(String timeKeeper) {
		this.timeKeeper = timeKeeper;
	}

	public void setWeighIn1(String weighInTO1) {
		this.weighIn1 = weighInTO1;
	}

	public void setWeighIn2(String weighInTO2) {
		this.weighIn2 = weighInTO2;
	}

	/**
	 * Sets the weigh in time.
	 *
	 * @param w the weigh-in time to set
	 */
	public void setWeighInTime(LocalDateTime w) {
		this.weighInTime = w;
	}

	public int size() {
		return AthleteRepository.findAllByGroupAndWeighIn(this, null).size();
	}

	public String fullDump() {
		return "Group [name=" + name + ", platform=" + platform + ", description=" + description + ", weighInTime="
		        + weighInTime + ", competitionTime=" + competitionTime + ", done=" + done + ", announcer=" + announcer
		        + ", marshall=" + marshall + ", marshal2=" + marshal2 + ", referee1=" + referee1 + ", referee2="
		        + referee2 + ", referee3=" + referee3 + ", weighIn1=" + weighIn1 + ", weighIn2=" + weighIn2
		        + ", timeKeeper=" + timeKeeper + ", technicalController=" + technicalController
		        + ", technicalController2=" + technicalController2 + ", jury1=" + jury1 + ", jury2=" + jury2
		        + ", jury3=" + jury3 + ", jury4=" + jury4 + ", jury5=" + jury5 + ", logger=" + logger + ", reserve="
		        + reserve + ", id=" + id + "]";
	}

	@Override
	public String toString() {
		return getName();
	}

	private void setDone(boolean b) {
		this.done = b;
	}

	public static DisplayGroup getEmptyDisplayGroup() {
		return new DisplayGroup("?", "", null, "", "");
	}

}
