/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.group;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AbstractLifterComparator;
import app.owlcms.data.config.Config;
import app.owlcms.data.platform.Platform;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.DateTimeUtils;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Logger;

/**
 * The Class Group.
 */

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity(name = "CompetitionGroup")
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Group.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger", "athletes" })
public class Group implements Comparable<Group> {

	private final static Logger logger = (Logger) LoggerFactory.getLogger(Group.class);
	private final static NaturalOrderComparator<String> c = new NaturalOrderComparator<>();
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
	private final static DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder().parseLenient()
	        .appendPattern(DATE_FORMAT).toFormatter();
	
	public static Comparator<Athlete> weighinTimeComparator = (lifter1, lifter2) -> {
		Group lifter1Group = lifter1.getGroup();
		Group lifter2Group = lifter2.getGroup();

		int compare;
		// null groups go to bottom
		if (lifter1Group == null || lifter2Group == null) {
			compare = ObjectUtils.compare(lifter1Group, lifter2Group, true);
			return compare;
		}

		LocalDateTime lifter1Date = lifter1Group.getWeighInTime();
		LocalDateTime lifter2Date = lifter2Group.getWeighInTime();
		compare = ObjectUtils.compare(lifter1Date, lifter2Date, true);
		AbstractLifterComparator.traceComparison("compareGroupWeighInTime", lifter1,
		        lifter1.getGroup().getWeighInTime(),
		        lifter2, lifter2.getGroup().getWeighInTime(), compare);
		if (compare != 0) {
			return compare;
		}

		// Platform p1 = lifter1Group.getPlatform();
		// Platform p2 = lifter2Group.getPlatform();
		// String name1 = p1 != null ? p1.getName() : null;
		// String name2 = p2 != null ? p2.getName() : null;
		// compare = ObjectUtils.compare(name1, name2, false);
		// if (compare != 0) {
		// // logger.trace("different platform {} {} {}", name1, name2,
		// // LoggerUtils.whereFrom(10));
		// return compare;
		// }

		String lifter1String = lifter1Group.getName();
		String lifter2String = lifter2Group.getName();

		if (lifter1String == null || lifter2String == null) {
			compare = ObjectUtils.compare(lifter1String, lifter2String, true);
		} else {
			compare = AbstractLifterComparator.noc.compare(lifter1String, lifter2String);
		}
		compare = AbstractLifterComparator.noc.compare(lifter1String, lifter2String);
		if (compare != 0) {
			// logger.trace("different group {} {} {}", lifter1String, lifter2String,
			// LoggerUtils.whereFrom(10));
			return compare;
		}

		return 0;
	};

	private enum USAFlagOrder {
		RED, WHITE, BLUE, STARS, STRIPES, GOLD, ROGUE
	}

	public static Comparator<Group> groupWeighinTimeComparator = (lifter1Group, lifter2Group) -> {

		int compare;
		if (lifter1Group == null || lifter2Group == null) {
			compare = ObjectUtils.compare(lifter1Group, lifter2Group, true);
			AbstractLifterComparator.traceComparison("compare group null", lifter1Group,
			        lifter1Group,
			        lifter2Group, lifter2Group, compare);
			return compare;
		}

		if (Config.getCurrent().featureSwitch("usaw")) {
			var lifter1SessionBlock = lifter1Group.getSessionBlock();
			var lifter2SessionBlock = lifter2Group.getSessionBlock();
			// null sessionBlocks go last.
			compare = ObjectUtils.compare(lifter1SessionBlock, lifter2SessionBlock, true);
			if (compare != 0) {
				AbstractLifterComparator.traceComparison("compare sessionBlock", lifter1Group,
				        lifter1SessionBlock, lifter2Group, lifter2SessionBlock, compare);
				return compare;
			}

			var lifter1Platform = lifter1Group.getPlatform();
			var lifter1PlatformName = lifter1Platform != null ? lifter1Platform.getName() : null;
			var lifter2Platform = lifter2Group.getPlatform();
			var lifter2PlatformName = lifter2Platform != null ? lifter2Platform.getName() : null;

			// null platform names go last.
			if (lifter1PlatformName == null || lifter2PlatformName == null) {
				compare = ObjectUtils.compare(lifter1PlatformName, lifter2PlatformName, true);
				AbstractLifterComparator.traceComparison("compare platform null", lifter1Group,
				        lifter1PlatformName, lifter2Group, lifter2PlatformName, compare);
				return compare;
			}

			try {
				var order1 = USAFlagOrder.valueOf(lifter1PlatformName.toUpperCase());
				var order2 = USAFlagOrder.valueOf(lifter2PlatformName.toUpperCase());
				compare = order1.compareTo(order2);
				AbstractLifterComparator.traceComparison("compare flagOrder", lifter1Group,
				        order1, lifter2Group, order2, compare);
			} catch (Exception e) {
				compare = ObjectUtils.compare(lifter1PlatformName, lifter2PlatformName);
				AbstractLifterComparator.traceComparison("compare platformName", lifter1Group,
				        lifter1PlatformName, lifter2Group, lifter2PlatformName, compare);
			}
			return compare;

		}
		LocalDateTime lifter1Date = lifter1Group.getWeighInTime();
		LocalDateTime lifter2Date = lifter2Group.getWeighInTime();
		compare = ObjectUtils.compare(lifter1Date, lifter2Date, true);
		if (compare != 0) {
			AbstractLifterComparator.traceComparison("compareGroupWeighInTime", lifter1Group,
			        lifter1Group.getWeighInTime(), lifter2Group, lifter2Group.getWeighInTime(), compare);
			return compare;
		}

		String lifter1String = lifter1Group.getName();
		String lifter2String = lifter2Group.getName();

		if (lifter1String == null || lifter2String == null) {
			compare = ObjectUtils.compare(lifter1String, lifter2String, true);
		} else {
			compare = AbstractLifterComparator.noc.compare(lifter1String, lifter2String);
		}
		compare = AbstractLifterComparator.noc.compare(lifter1String, lifter2String);
		if (compare != 0) {
			// logger.trace("different group {} {} {}", lifter1String, lifter2String,
			// LoggerUtils.whereFrom(10));
			return compare;
		}

		return 0;
	};
	public static Comparator<Group> groupSelectionComparator = (lifter1Group, lifter2Group) -> {

		int compare;
		if (lifter1Group == null || lifter2Group == null) {
			compare = ObjectUtils.compare(lifter1Group, lifter2Group, true);
			return compare;
		}

		Boolean lifter1Done = lifter1Group.isDone();
		Boolean lifter2Done = lifter2Group.isDone();
		compare = ObjectUtils.compare(lifter1Done, lifter2Done, true);
		if (compare != 0) {
			AbstractLifterComparator.traceComparison("compareGroup isDone", lifter1Group,
			        lifter1Group.isDone(),
			        lifter2Group, lifter2Group.isDone(), compare);
			return compare;
		}

		LocalDateTime lifter1Date = lifter1Group.getWeighInTime();
		LocalDateTime lifter2Date = lifter2Group.getWeighInTime();
		compare = ObjectUtils.compare(lifter1Date, lifter2Date, true);
		if (compare != 0) {
			AbstractLifterComparator.traceComparison("compareGroupWeighInTime", lifter1Group,
			        lifter1Group.getWeighInTime(),
			        lifter2Group, lifter2Group.getWeighInTime(), compare);
			return compare;
		}

		String lifter1String = lifter1Group.getName();
		String lifter2String = lifter2Group.getName();

		if (lifter1String == null || lifter2String == null) {
			compare = ObjectUtils.compare(lifter1String, lifter2String, true);
		} else {
			compare = AbstractLifterComparator.noc.compare(lifter1String, lifter2String);
		}
		compare = AbstractLifterComparator.noc.compare(lifter1String, lifter2String);
		if (compare != 0) {
			// logger.trace("different group {} {} {}", lifter1String, lifter2String,
			// LoggerUtils.whereFrom(10));
			return compare;
		}

		return 0;
	};

	public static DisplayGroup getEmptyDisplayGroup() {
		return new DisplayGroup("?", "", null, "", "");
	}

	@Transient
	@JsonIgnore
	private DateTimeFormatter hourFormatter;
	@Transient
	@JsonIgnore
	private DateTimeFormatter dayFormatter;
	@Transient
	@JsonIgnore
	final DateTimeFormatter isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
	@Transient
	@JsonIgnore
	final DateTimeFormatter isoHourFormatter = DateTimeFormatter.ofPattern("HH:mm");
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
		setHourFormatter(Locale.getDefault());
		setDayFormatter(Locale.getDefault());
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
		setHourFormatter(Locale.getDefault());
		setDayFormatter(Locale.getDefault());
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

	@Transient
	@JsonIgnore
	Pattern pattern = Pattern.compile("(\\d+)\\s+(\\w+)");

	@Transient
	@JsonIgnore
	public Integer getSessionBlock() {
		if (Config.getCurrent().featureSwitch("usaw")) {
			Matcher matcher = pattern.matcher(this.getName());
			if (matcher.find()) {
				String number = matcher.group(1);
				// String word = matcher.group(2);
				try {
					return Integer.parseInt(number);
				} catch (NumberFormatException e) {
					return 999;
				}
			}
			return 999;
		}
		return 1;
	}

	@Transient
	@JsonIgnore
	public List<AgeGroupInfo> getAgeGroupInfo() {
		return new AgeGroupInfoFactory().getAgeGroupInfos(this);
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
		if (this.name == null) {
			if (other.name != null) {
				return 1;
			} else {
				return 0;
			}
		} else {
			if (other.name != null) {
				return c.compare(this.name, other.name);
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

	public void copy(Group source) throws IllegalAccessException, InvocationTargetException {
		Long myId = getId();
		BeanUtils.copyProperties(source, this);
		this.setId(myId);
	}

	// @Override

	public void doDone(boolean b) {
		Group.logger.debug("done? {} previous={} done={} {} [{}]", getName(), this.done, b,
		        System.identityHashCode(this),
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
	}

	public String fullDump() {
		return "Group [name=" + this.name + ", platform=" + this.platform + ", description=" + this.description
		        + ", weighInTime="
		        + this.weighInTime + ", competitionTime=" + this.competitionTime + ", done=" + this.done
		        + ", announcer=" + this.announcer
		        + ", marshall=" + this.marshall + ", marshal2=" + this.marshal2 + ", referee1=" + this.referee1
		        + ", referee2="
		        + this.referee2 + ", referee3=" + this.referee3 + ", weighIn1=" + this.weighIn1 + ", weighIn2="
		        + this.weighIn2
		        + ", timeKeeper=" + this.timeKeeper + ", technicalController=" + this.technicalController
		        + ", technicalController2=" + this.technicalController2 + ", jury1=" + this.jury1 + ", jury2="
		        + this.jury2
		        + ", jury3=" + this.jury3 + ", jury4=" + this.jury4 + ", jury5=" + this.jury5 + ", logger="
		        + Group.logger + ", reserve="
		        + this.reserve + ", id=" + this.id + "]";
	}

	@Transient
	@JsonIgnore
	public List<Athlete> getAlphaAthletes() {
		List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(this, null);
		athletes.sort((a, b) -> ObjectUtils.compare(a.getFullName(), b.getFullName()));
		return athletes;
	}

	/**
	 * Gets the announcer.
	 *
	 * @return the announcer
	 */
	public String getAnnouncer() {
		return this.announcer;
	}

	@Transient
	@JsonIgnore
	public List<Athlete> getAthletes() {
		return AthleteRepository.findAllByGroupAndWeighIn(this, null);
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
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the competition time.
	 *
	 * @return the competition time
	 */
	public LocalDateTime getCompetitionTime() {
		return this.competitionTime;
	}

	@Transient
	@JsonIgnore
	public Date getCompetitionTimeAsDate() {
		return DateTimeUtils.dateFromLocalDateTime(this.competitionTime);
	}

	@Transient
	@JsonIgnore
	public Double getCompetitionTimeAsExcelDate() {
		var value = DateTimeUtils.localDateTimeToExcelDate(this.competitionTime);
		return value;
	}

	public String getDescription() {
		return this.description;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Gets the session short date in ISO format
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getIntlStartDay() {
		String formatted = "";
		try {
			LocalDateTime competitionTime2 = getCompetitionTime();
			formatted = competitionTime2 == null ? "" : this.isoDateFormatter.format(competitionTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the competition start hour in ISO format
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getIntlStartHour() {
		String formatted = "";
		try {
			LocalDateTime competitionTime2 = getCompetitionTime();
			formatted = competitionTime2 == null ? "" : this.isoHourFormatter.format(competitionTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the session short date in ISO format
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getIntlWeighInDay() {
		String formatted = "";
		try {
			LocalDateTime weighinTime2 = getWeighInTime();
			formatted = weighinTime2 == null ? "" : this.isoDateFormatter.format(weighinTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the competition WeighIn hour in ISO format
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getIntlWeighInHour() {
		String formatted = "";
		try {
			LocalDateTime weighinTime2 = getWeighInTime();
			formatted = weighinTime2 == null ? "" : this.isoHourFormatter.format(weighinTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the jury.
	 *
	 * @return the jury
	 */
	@JsonIgnore
	public String getJury() {
		List<String> jurors = Arrays.asList(this.jury1, this.jury2, this.jury3, this.jury4, this.jury5);
		Iterables.removeIf(jurors, Predicates.isNull());
		return String.join(", ", jurors);
	}

	/**
	 * @return the jury1
	 */
	public String getJury1() {
		return this.jury1;
	}

	/**
	 * @return the jury2
	 */
	public String getJury2() {
		return this.jury2;
	}

	/**
	 * @return the jury3
	 */
	public String getJury3() {
		return this.jury3;
	}

	/**
	 * @return the jury4
	 */
	public String getJury4() {
		return this.jury4;
	}

	/**
	 * @return the jury5
	 */
	public String getJury5() {
		return this.jury5;
	}

	/**
	 * Gets the session short date .
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getLocalizedStartDay() {
		String formatted = "";
		try {
			LocalDateTime competitionTime2 = getCompetitionTime();
			Locale locale = OwlcmsSession.getLocale();
			if (!locale.equals(getDayFormatter().getLocale())) {
				setDayFormatter(locale);
			}
			formatted = competitionTime2 == null ? "" : getDayFormatter().format(competitionTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the session short start time.
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getLocalizedStartHour() {
		String formatted = "";
		try {
			LocalDateTime competitionTime2 = getCompetitionTime();
			Locale locale = OwlcmsSession.getLocale();
			if (!locale.equals(getHourFormatter().getLocale())) {
				setHourFormatter(locale);
			}
			formatted = competitionTime2 == null ? "" : getHourFormatter().format(competitionTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the session short date .
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getLocalWeighInDay() {
		String formatted = "";
		try {
			LocalDateTime weighinTime2 = getWeighInTime();
			Locale locale = OwlcmsSession.getLocale();
			if (!locale.equals(getDayFormatter().getLocale())) {
				setDayFormatter(locale);
			}

			formatted = weighinTime2 == null ? "" : getDayFormatter().format(weighinTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the session short WeighIn time.
	 *
	 * @return the competition time
	 */
	@Transient
	@JsonIgnore
	public String getLocalWeighInHour() {
		String formatted = "";
		try {
			LocalDateTime weighinTime2 = getWeighInTime();
			Locale locale = OwlcmsSession.getLocale();
			if (!locale.equals(getHourFormatter().getLocale())) {
				setHourFormatter(locale);
			}
			formatted = weighinTime2 == null ? "" : getHourFormatter().format(weighinTime2);
		} catch (Exception e) {
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	public String getMarshal2() {
		return this.marshal2;
	}

	/**
	 * Gets the marshall.
	 *
	 * @return the marshall
	 */
	public String getMarshall() {
		return this.marshall;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the platform.
	 *
	 * @return the platformName on which group will be lifting
	 */
	public Platform getPlatform() {
		return this.platform;
	}

	/**
	 * Gets the referee 1.
	 *
	 * @return the referee 1
	 */
	public String getReferee1() {
		return this.referee1;
	}

	/**
	 * Gets the referee 2.
	 *
	 * @return the referee 2
	 */
	public String getReferee2() {
		return this.referee2;
	}

	/**
	 * Gets the referee 3.
	 *
	 * @return the referee 3
	 */
	public String getReferee3() {
		return this.referee3;
	}

	/**
	 * @return the reserve
	 */
	public String getReserve() {
		return this.reserve;
	}

	/**
	 * Gets the technical controller.
	 *
	 * @return the technical controller
	 */
	public String getTechnicalController() {
		return this.technicalController;
	}

	public String getTechnicalController2() {
		return this.technicalController2;
	}

	/**
	 * Gets the time keeper.
	 *
	 * @return the time keeper
	 */
	public String getTimeKeeper() {
		return this.timeKeeper;
	}

	public String getWeighIn1() {
		return this.weighIn1;
	}

	public String getWeighIn2() {
		return this.weighIn2;
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
			LoggerUtils.logError(Group.logger, e);
		}
		return formatted;
	}

	/**
	 * Gets the weigh in time.
	 *
	 * @return the weigh-in time (two hours before competition, normally)
	 */
	public LocalDateTime getWeighInTime() {
		return this.weighInTime;
	}

	@Transient
	@JsonIgnore
	public Date getWeighInTimeAsDate() {
		return DateTimeUtils.dateFromLocalDateTime(this.weighInTime);
	}

	@Override
	public int hashCode() {
		// https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
		return 31;
	}

	public boolean isDone() {
		return this.done;
	}

	public void setAlphaAthletes(List<Athlete> a) {
	}

	/**
	 * Sets the announcer.
	 *
	 * @param announcer the new announcer
	 */
	public void setAnnouncer(String announcer) {
		this.announcer = announcer;
	}

	public void setAthletes(List<Athlete> a) {
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

	@Override
	public String toString() {
		return getName();
	}

	DateTimeFormatter getDayFormatter() {
		return this.dayFormatter;
	}

	DateTimeFormatter getHourFormatter() {
		return this.hourFormatter;
	}

	void setDayFormatter(DateTimeFormatter dayFormatter) {
		this.dayFormatter = dayFormatter;
	}

	void setHourFormatter(DateTimeFormatter hourFormatter) {
		this.hourFormatter = hourFormatter;
	}

	private void setDayFormatter(Locale locale) {
		setDayFormatter(DateTimeFormatter
		        .ofLocalizedDate(FormatStyle.SHORT)
		        .withLocale(locale));
	}

	private void setDone(boolean b) {
		this.done = b;
	}

	private void setHourFormatter(Locale locale) {
		setHourFormatter(DateTimeFormatter
		        .ofLocalizedTime(FormatStyle.SHORT)
		        .withLocale(locale));
	}
}
