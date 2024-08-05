/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.athlete;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.category.RegistrationPreferenceComparator;
import app.owlcms.data.category.RobiCategories;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.DisplayGroup;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.LocalDateAttributeConverter;
import app.owlcms.data.scoring.AgeFactors;
import app.owlcms.data.scoring.GAMX;
import app.owlcms.data.scoring.QPoints;
import app.owlcms.data.scoring.SinclairCoefficients;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.LiftOrderInfo;
import app.owlcms.fieldofplay.LiftOrderReconstruction;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.RAthlete;
import app.owlcms.utils.DateTimeUtils;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class stores all the information related to a particular athlete.
 * <p>
 * This class is an example of what not to do. This was designed prior reaching a proper understanding of Hibernate/JPA
 * and of proper separation between Vaadin Containers and persistence frameworks. Live and Learn.
 * <p>
 * All persistent properties are managed by Java Persistance annotations. "Field" access mode is used, meaning that it
 * is the values of the fields that are stored, and not the values returned by the getters. Note that it is often
 * necessary to know when a value has been captured or not -- this is why values are stored as Integers or Doubles, so
 * that we can use null to indicate that a value has not been captured.
 * </p>
 * <p>
 * This allows us to use the getters to return the values as they will be displayed by the application
 * </p>
 * <p>
 * Computed fields are defined as final transient properties and marked as @Transient; the only reason for this is so
 * the JavaBeans introspection mechanisms find them.
 * </p>
 * <p>
 * This class uses events to notify interested user interface components that fields or computed values have changed. In
 * this way the user interface does not have to know that the category field on the screen is dependent on the
 * bodyweight and the gender -- all the dependency logic is kept at the business object level.
 * </p>
 */

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
@JsonPropertyOrder({ "id", "participations", "category" })
public class Athlete {
	@Transient
	protected final static Logger logger = (Logger) LoggerFactory.getLogger(Athlete.class);
	static private boolean skipValidationsDuringImport = false;
	private static final int YEAR = LocalDateTime.now().getYear();
	@Transient
	@JsonIgnore
	private static SinclairCoefficients sinclairProperties2020 = new SinclairCoefficients(2020);
	@Transient
	@JsonIgnore
	private static SinclairCoefficients sinclairProperties2024 = new SinclairCoefficients(2024);
	@Transient
	@JsonIgnore
	private static QPoints qPoints = new QPoints(2023);

	public static void conditionalCopy(Athlete dest, Athlete src, boolean copyResults) {
		boolean validation = dest.isValidation();
		Level prevSrcLevel = src.getLogger().getLevel();
		Level prevDestLevel = dest.getLogger().getLevel();
		try {
			dest.setId(src.getId());
			dest.setValidation(false);
			dest.setLoggerLevel(Level.OFF);
			dest.setCopyId(src.getId());

			dest.setLastName(src.getLastName());
			dest.setFirstName(src.getFirstName());
			dest.setFullBirthDate(src.getFullBirthDate());
			dest.setGroup(src.getGroup());
			dest.setStartNumber(src.getStartNumber());
			dest.setLotNumber(src.getLotNumber());
			dest.setEntryTotal(src.getEntryTotal());
			dest.setCategory(src.getCategory());

			dest.setSnatch1Declaration(src.getSnatch1Declaration());
			dest.setSnatch1Change1(src.getSnatch1Change1());
			dest.setSnatch1Change2(src.getSnatch1Change2());
			if (copyResults) {
				dest.setSnatch1ActualLift(src.getSnatch1ActualLift());
				dest.setSnatch1LiftTime(src.getSnatch1LiftTime());
			}

			dest.setSnatch2AutomaticProgression(src.getSnatch2AutomaticProgression());
			dest.setSnatch2Declaration(src.getSnatch2Declaration());
			dest.setSnatch2Change1(src.getSnatch2Change1());
			dest.setSnatch2Change2(src.getSnatch2Change2());
			if (copyResults) {
				dest.setSnatch2ActualLift(src.getSnatch2ActualLift());
				dest.setSnatch2LiftTime(src.getSnatch2LiftTime());
			}

			dest.setSnatch3AutomaticProgression(src.getSnatch3AutomaticProgression());
			dest.setSnatch3Declaration(src.getSnatch3Declaration());
			dest.setSnatch3Change1(src.getSnatch3Change1());
			dest.setSnatch3Change2(src.getSnatch3Change2());
			if (copyResults) {
				dest.setSnatch3ActualLift(src.getSnatch3ActualLift());
				dest.setSnatch3LiftTime(src.getSnatch3LiftTime());
			}

			dest.setCleanJerk1Declaration(src.getCleanJerk1Declaration());
			dest.setCleanJerk1Change1(src.getCleanJerk1Change1());
			dest.setCleanJerk1Change2(src.getCleanJerk1Change2());
			if (copyResults) {
				dest.setCleanJerk1ActualLift(src.getCleanJerk1ActualLift());
				dest.setCleanJerk1LiftTime(src.getCleanJerk1LiftTime());
			}

			dest.setCleanJerk2AutomaticProgression(src.getCleanJerk2AutomaticProgression());
			dest.setCleanJerk2Declaration(src.getCleanJerk2Declaration());
			dest.setCleanJerk2Change1(src.getCleanJerk2Change1());
			dest.setCleanJerk2Change2(src.getCleanJerk2Change2());
			if (copyResults) {
				dest.setCleanJerk2ActualLift(src.getCleanJerk2ActualLift());
				dest.setCleanJerk2LiftTime(src.getCleanJerk2LiftTime());
			}

			dest.setCleanJerk3AutomaticProgression(src.getCleanJerk3AutomaticProgression());
			dest.setCleanJerk3Declaration(src.getCleanJerk3Declaration());
			dest.setCleanJerk3Change1(src.getCleanJerk3Change1());
			dest.setCleanJerk3Change2(src.getCleanJerk3Change2());
			if (copyResults) {
				dest.setCleanJerk3ActualLift(src.getCleanJerk3ActualLift());
				dest.setCleanJerk3LiftTime(src.getCleanJerk3LiftTime());
			}
			
			if (copyResults) {
				dest.setCustomScore(src.getCustomScoreComputed());
			}

			dest.setForcedAsCurrent(src.isForcedAsCurrent());
			dest.setCoach(src.getCoach());
			dest.setCustom1(src.getCustom1());
			dest.setCustom2(src.getCustom2());

			if (copyResults) {
				// Category-specific results are in the participation objects
				// Category-independent scores are here
				dest.setSinclairRank(src.getSinclairRank());
				dest.setqPointsRank(src.getqPointsRank());
				dest.setqAgeRank(src.getqAgeRank());
				dest.setSmhfRank(src.getSmhfRank());
				dest.setTeamSinclairRank(src.getTeamSinclairRank());
				dest.setCatSinclairRank(src.getCatSinclairRank());
				dest.setGamxRank(src.getGamxRank());
				dest.setRobiRank(src.getRobiRank());
				dest.setAgeAdjustedTotalRank(src.getAgeAdjustedTotalRank());
			}
		} finally {
			dest.setValidation(validation);
			dest.setLoggerLevel(prevDestLevel);
			src.setLoggerLevel(prevSrcLevel);
		}
	}

	public void setqAgeRank(int qAgeRank2) {
		this.qAgeRank = qAgeRank2;
	}

	public int getqAgeRank() {
		return this.qAgeRank;
	}

	/**
	 * Checks if is empty.
	 *
	 * @param value the value
	 * @return true, if is empty
	 */
	public static boolean isEmpty(String value) {
		return (value == null) || value.trim().isEmpty();
	}

	/**
	 * @return the skipValidationsDuringImport
	 */
	public static boolean isSkipValidationsDuringImport() {
		return skipValidationsDuringImport;
	}

	/**
	 * Return the last non-zero item
	 *
	 * @param items the items
	 * @return the integer
	 */
	public static Integer last(Integer... items) {
		int lastIndex = items.length - 1;
		while (lastIndex >= 0) {
			if (items[lastIndex] > 0) {
				return items[lastIndex];
			}
			lastIndex--;
		}
		return 0;
	}

	/**
	 * @param skipValidationsDuringImport the skipValidationsDuringImport to set
	 */
	public static void setSkipValidationsDuringImport(boolean importAsIs) {
		Athlete.skipValidationsDuringImport = importAsIs;
	}

	/**
	 * Zero if invalid.
	 *
	 * @param value the value
	 * @return the int
	 */
	public static int zeroIfInvalid(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	/**
	 * We cannot always rely on the session to be present and give us a valid Fop. LoadGroup should set the Fop.
	 */
	@Transient
	@JsonIgnore
	public FieldOfPlay fop;
	@Transient
	@JsonIgnore
	protected final Logger timingLogger = (Logger) LoggerFactory.getLogger("TimingLogger");
	@Transient
	@JsonIgnore
	DecimalFormat df = null;

	@Transient
	@JsonIgnore
	Integer liftOrderRank = 0;
	private Double bodyWeight = null;

	/*
	 * eager does not hurt for us. https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa
	 * -and-hibernate/
	 */
	@ManyToOne(cascade = { CascadeType.MERGE,
	        CascadeType.REFRESH }, optional = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_categ", nullable = true)
	@JsonProperty(index = 300)
	@JsonIdentityReference(alwaysAsId = true)
	private Category category = null;
	
	@Transient
	@JsonIgnore
	private boolean categoryDone;
	
	@Column(columnDefinition = "integer default 0")
	private int catSinclairRank;
	private String cleanJerk1ActualLift;
	private String cleanJerk1Change1;
	private String cleanJerk1Change2;
	private String cleanJerk1Declaration;
	private LocalDateTime cleanJerk1LiftTime;
	private String cleanJerk2ActualLift;
	private String cleanJerk2Change1;
	private String cleanJerk2Change2;
	private String cleanJerk2Declaration;
	private LocalDateTime cleanJerk2LiftTime;
	private String cleanJerk3ActualLift;
	private String cleanJerk3Change1;
	private String cleanJerk3Change2;
	private String cleanJerk3Declaration;
	private LocalDateTime cleanJerk3LiftTime;
	private String coach;
	@Column(columnDefinition = "integer default 0")
	private int combinedRank;
	@Transient
	@JsonIgnore
	private Long copyId = null;
	private String custom1;
	private String custom2;
	private Double customScore;
	@Column(columnDefinition = "boolean default true")
	private boolean eligibleForIndividualRanking = true;
	private boolean eligibleForTeamRanking = true;
	private String firstName = "";
	/** The forced as current. */
	@Column(columnDefinition = "boolean default false")
	private boolean forcedAsCurrent = false;
	@Convert(converter = LocalDateAttributeConverter.class)
	private LocalDate fullBirthDate = null;
	private Gender gender = null; // $NON-NLS-1$
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE,
	        CascadeType.REFRESH }, optional = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_group", nullable = true)
	private Group group;
	@Id
	// @GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String lastName = "";
	private Integer lotNumber = null;
	private String membership = "";
	@Transient
	@JsonIgnore
	private final Level NORMAL_LEVEL = Level.INFO;
	@OneToMany(mappedBy = "athlete", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JsonProperty(index = 200)
	private List<Participation> participations = new ArrayList<>();
	/**
	 * body weight inferred from category, used until real bodyweight is known.
	 */
	private Double presumedBodyWeight;
	private Integer qualifyingTotal = 0;
	
	@JsonIgnore
	private Integer robiRank;
	
	@JsonIgnore
	private Integer sinclairRank;
	
	@JsonIgnore
	@Column(columnDefinition = "integer default 0")
	private Integer qPointsRank;
	
	@JsonIgnore
	@Column(name="smmRank", columnDefinition = "integer default 0")
	private int smhfRank;
	
	@JsonIgnore
	@Column(columnDefinition = "integer default 0")
	private int qAgeRank;
	
	private String snatch1ActualLift;
	private String snatch1Change1;
	private String snatch1Change2;
	/**
	 * Using separate fields is brute force, but having embedded classes does not bring much and we don't want joins or
	 * other such logic for the Athlete card. Since the Athlete card is 6 x 4 items, we take the simple route.
	 *
	 * The use of Strings is historical. It was extremely cumbersome to handle conversions to/from Integer in Vaadin 6
	 * circa 2009, and migration of databases would be annoying to users.
	 */
	private String snatch1Declaration;
	private LocalDateTime snatch1LiftTime;
	private String snatch2ActualLift;
	private String snatch2Change1;
	private String snatch2Change2;
	private String snatch2Declaration;
	private LocalDateTime snatch2LiftTime;
	private String snatch3ActualLift;
	private String snatch3Change1;
	private String snatch3Change2;
	private String snatch3Declaration;
	private LocalDateTime snatch3LiftTime;
	private Integer startNumber = null;
	private String team = "";
	private Integer teamCleanJerkRank;
	private Integer teamCombinedRank;
	private Integer teamCustomRank;
	private Integer teamRobiRank;
	private Integer teamSinclairRank;
	private Integer teamSnatchRank;
	private Integer teamTotalRank;
	@Transient
	@JsonIgnore
	private boolean validation = true;
	@Transient
	@JsonIgnore
	private SinclairCoefficients sinclairProperties;
	private String federationCodes;
	private Integer personalBestSnatch;
	private Integer personalBestCleanJerk;
	private Integer personalBestTotal;
	@Transient
	@JsonIgnore
	private boolean startingTotalViolation = false;
	@Transient
	@JsonIgnore
	private boolean checkTiming;
	private String subCategory;
	@Column(columnDefinition = "integer default 0", name="gmaxRank")
	private Integer gamxRank;
	private Integer ageAdjustedTotalRank;

	/**
	 * Instantiates a new athlete.
	 */
	public Athlete() {
		setId(IdUtils.getTimeBasedId());
		this.validation = true;
		this.timingLogger.setLevel(Level.WARN);
	}

	public void addEligibleCategory(Category category) {
		addEligibleCategory(category, true);
	}

	public void addEligibleCategory(Category category, boolean teamMember) {
		if (category == null) {
			return;
		}
		Participation participation = new Participation(this, category);
		participation.setTeamMember(teamMember);

		if (this.participations == null) {
			this.participations = new ArrayList<>();
		}
		removeCurrentAthleteCategoryParticipation(category, this.participations);
		this.participations.add(participation);
		setParticipations(this.participations);

		List<Participation> categoryParticipations = category.getParticipations();
		if (categoryParticipations == null) {
			categoryParticipations = new ArrayList<>();
		}
		removeCurrentAthleteCategoryParticipation(category, categoryParticipations);
		categoryParticipations.add(participation);
		category.setParticipations(categoryParticipations);
	}

	public void checkParticipations() {
		for (Iterator<Participation> iterator = this.participations.iterator(); iterator.hasNext();) {
			Participation p = iterator.next();
			if (p.getAthlete() == null || p.getAthlete().getId() == null || p.getCategory() == null
			        || p.getCategory().getId() == null) {
				iterator.remove();
				continue;
			}
		}
	}

	public void clearLifts() {
		String cj1Decl = this.getCleanJerk1Declaration();
		String sn1Decl = this.getSnatch1Declaration();
		boolean validate = this.isValidation();
		Level prevLevel = this.getLogger().getLevel();
		try {
			this.setValidation(false);
			this.setLoggerLevel(Level.OFF);

			this.setCleanJerk1Declaration("");
			this.setCleanJerk1AutomaticProgression("");
			this.setCleanJerk1Change1("");
			this.setCleanJerk1Change2("");
			this.setCleanJerk1ActualLift(null);
			this.setCleanJerk1LiftTime(null);

			this.setCleanJerk2Declaration("");
			this.setCleanJerk2AutomaticProgression("");
			this.setCleanJerk2Change1("");
			this.setCleanJerk2Change2("");
			this.setCleanJerk2ActualLift(null);
			this.setCleanJerk2LiftTime(null);

			this.setCleanJerk3Declaration("");
			this.setCleanJerk3AutomaticProgression("");
			this.setCleanJerk3Change1("");
			this.setCleanJerk3Change2("");
			this.setCleanJerk3ActualLift(null);
			this.setCleanJerk3LiftTime(null);

			this.setSnatch1Declaration("");
			this.setSnatch1AutomaticProgression("");
			this.setSnatch1Change1("");
			this.setSnatch1Change2("");
			this.setSnatch1ActualLift(null);
			this.setSnatch1LiftTime(null);

			this.setSnatch2Declaration("");
			this.setSnatch2AutomaticProgression("");
			this.setSnatch2Change1("");
			this.setSnatch2Change2("");
			this.setSnatch2ActualLift(null);
			this.setSnatch2LiftTime(null);

			this.setSnatch3Declaration("");
			this.setSnatch3AutomaticProgression("");
			this.setSnatch3Change1("");
			this.setSnatch3Change2("");
			this.setSnatch3ActualLift(null);
			this.setSnatch3LiftTime(null);

			this.setSnatch1Declaration(sn1Decl);
			this.setCleanJerk1Declaration(cj1Decl);
		} finally {
			this.setValidation(validate);
			this.setLoggerLevel(prevLevel);
		}
	}

	public void computeMainAndEligibleCategories() {
		Double weight = this.getBodyWeight();
		Integer age = this.getAge();
		if (weight == null || weight < 0.01) {
			Double presumedBodyWeight = this.getPresumedBodyWeight();
			if (presumedBodyWeight != null) {
				weight = presumedBodyWeight - 0.01D;
				if (age == null || age == 0) {
					// try to set category to match sheet, with coherent eligibles
					if (this.category != null) {
						age = this.category.getAgeGroup().getMaxAge();
					}
				}
				List<Category> categories = CategoryRepository.doFindEligibleCategories(this, this.gender, age, weight,
				        this.qualifyingTotal);
				setEligibles(this, categories);
				this.setCategory(bestMatch(categories));
			}
		} else {
			List<Category> categories = CategoryRepository.doFindEligibleCategories(this, this.gender, age, weight,
			        this.qualifyingTotal);
			setEligibles(this, categories);
			this.setCategory(bestMatch(categories));
		}
	}

	/**
	 * used for jury overrides and for testing
	 *
	 * @param liftNo
	 * @param weight
	 */
	public void doLift(int liftNo, final String weight) {
		switch (liftNo) {
			case 1:
				this.setSnatch1ActualLift(weight);
				this.setSnatch1LiftTime(LocalDateTime.now());
				break;
			case 2:
				this.setSnatch2ActualLift(weight);
				this.setSnatch2LiftTime(LocalDateTime.now());
				break;
			case 3:
				this.setSnatch3ActualLift(weight);
				this.setSnatch3LiftTime(LocalDateTime.now());
				break;
			case 4:
				this.setCleanJerk1ActualLift(weight);
				this.setCleanJerk1LiftTime(LocalDateTime.now());
				break;
			case 5:
				this.setCleanJerk2ActualLift(weight);
				this.setCleanJerk2LiftTime(LocalDateTime.now());
				break;
			case 6:
				this.setCleanJerk3ActualLift(weight);
				this.setCleanJerk3LiftTime(LocalDateTime.now());
				break;
		}
	}

	/**
	 * Public for testing purposes only.
	 *
	 * @param Athlete
	 * @param athletes
	 * @param weight
	 */
	public void doLift(final String weight) {
		int liftNo = this.getAttemptsDone() + 1;
		doLift(liftNo, weight);
	}

	public void enforceCategoryIsEligible() {
		if (this.category != null) {
			// we can't have category without eligibility relationship and one with same id
			// that has it in the
			// eligibility list
			// so we find the one in the eligibility list and use it.
			Category matchingEligible = null;
			for (Category eligible : getEligibleCategories()) {
				if (sameCategory(eligible, this.category)) {
					matchingEligible = eligible;
					break;
				}
			}
			setCategory(matchingEligible);
			logger.trace("category {} {} matching eligible {} {}", this.category,
			        System.identityHashCode(this.category),
			        matchingEligible, System.identityHashCode(matchingEligible));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		Athlete other = (Athlete) obj;
		return getId() != null && getId().equals(other.getId());
	}

	/**
	 * Failed lift.
	 */
	public void failedLift() {
		try {
			getLogger().info("{}no lift for {}", OwlcmsSession.getFopLoggingName(), this.getShortName());
			final String weight = Integer.toString(-getNextAttemptRequestedWeight());
			doLift(weight);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage());
		}
	}

	@Transient
	@JsonIgnore
	public String getAbbreviatedName() {
		String upperCase = this.getLastName() != null ? this.getLastName().toUpperCase() : "";
		String firstName2 = this.getFirstName() != null ? this.getFirstName() : "";
		String[] hyphenatedParts = firstName2.split("-");
		String abbreviated = Arrays.stream(hyphenatedParts).map(hpart -> {
			return Arrays.stream(hpart.split("[ .]+")).map(word -> (word.substring(0, 1) + "."))
			        .collect(Collectors.joining(" "));
		}).collect(Collectors.joining("-"));

		if (!upperCase.isBlank() && !abbreviated.isBlank()) {
			return Translator.translate("AbbreviatedNameFormat", upperCase, abbreviated);
		} else if (!upperCase.isBlank()) {
			return upperCase;
		} else if (!firstName2.isBlank()) {
			return firstName2;
		} else {
			return "?";
		}
	}

	@Transient
	@JsonIgnore
	public Integer getActualLift(int liftNo) {
		try {
			String value = getActualLiftStringOrElseNull(liftNo);
			return value == null ? 0 : Integer.valueOf(value);
		} catch (NumberFormatException e) {
			LoggerUtils.logError(logger, e);
			return 0;
		}
	}

	@Transient
	@JsonIgnore
	public Integer getActualLiftOrNull(int liftNo) {
		try {
			String value = getActualLiftStringOrElseNull(liftNo);
			return value == null ? null : Integer.valueOf(value);
		} catch (NumberFormatException e) {
			LoggerUtils.logError(logger, e);
			return null;
		}
	}

	/**
	 * Gets the attempted lifts. 0 means no lift done.
	 *
	 * @return the attempted lifts
	 */
	@Transient
	@JsonIgnore
	public int getActuallyAttemptedLifts() {
		int i = 0;
		if (zeroIfInvalid(this.snatch1ActualLift) != 0) {
			i++;
		}
		if (zeroIfInvalid(this.snatch2ActualLift) != 0) {
			i++;
		}
		if (zeroIfInvalid(this.snatch3ActualLift) != 0) {
			i++;
		}
		if (zeroIfInvalid(this.cleanJerk1ActualLift) != 0) {
			i++;
		}
		if (zeroIfInvalid(this.cleanJerk2ActualLift) != 0) {
			i++;
		}
		if (zeroIfInvalid(this.cleanJerk3ActualLift) != 0) {
			i++;
		}
		return i; // long ago
	}

	/**
	 * @return age as of current day
	 */
	@Transient
	@JsonIgnore
	public Integer getAge() {
		LocalDate date = null;
		if (Config.getCurrent().isUseCompetitionDate()) {
			date = Competition.getCurrent().getCompetitionDate();
			// logger.debug("competition date {}", date);
		}
		if (date == null) {
			date = LocalDate.now();
		}
		LocalDate fullBirthDate2 = getFullBirthDate();
		if (fullBirthDate2 == null) {
			return null;
		}
		return date.getYear() - fullBirthDate2.getYear();
	}

	/**
	 * Gets the age group.
	 *
	 * @return the ageGroup. M80 if male missing birth date, F70 if female missing birth date or missing both gender and
	 *         birth.
	 */
	@Transient
	@JsonIgnore
	public AgeGroup getAgeGroup() {
		Category cat = getCategory();
		return (cat != null ? cat.getAgeGroup() : null);
	}

	public String getAgeGroupDisplayName() {
		AgeGroup ag = getAgeGroup();
		return ag != null ? ag.getDisplayName() : "";
	}

	@Transient
	@JsonIgnore
	public String getAgeGroupCodesAsString() {
		return this.getEligibleCategories().stream()
		        .map(category -> {
			        return category.getAgeGroup().getCode();
		        })
		        .collect(Collectors.joining(", "));
	}

	@Transient
	@JsonIgnore
	public Set<String> getAgeGroupTeams() {
		// we use strings because I can't figure out why AgeGroups don't behave properly
		// in a checkbox group
		Set<String> s = new LinkedHashSet<>();
		List<Participation> participations2 = getParticipations();
		for (Participation p : participations2) {
			if (p.getTeamMember()) {
				s.add(p.getCategory().getAgeGroup().getDisplayName());
			}
		}
		return s;
	}

	@Transient
	@JsonIgnore
	public String getAllCategoriesAsString() {
		Category mrCat = getMainRankings() != null ? this.getMainRankings().getCategory() : null;
		// use getName because we don't want the translated gender.
		String mainCategory = mrCat != null ? mrCat.getDisplayName() : "";

		String mainCategoryString = mainCategory;
		if (mrCat != null && !getMainRankings().getTeamMember()) {
			mainCategoryString = mainCategory + RAthlete.NoTeamMarker;
		}

		String eligiblesAsString = this.getParticipations().stream()
		        .filter(p -> (p.getCategory() != mrCat))
		        .sorted((a, b) -> a.getCategory().getAgeGroup().compareTo(b.getCategory().getAgeGroup()))
		        .map(p -> {
			        String catName = p.getCategory().getDisplayName();
			        return catName + (!p.getTeamMember() ? RAthlete.NoTeamMarker : "");
		        })
		        .collect(Collectors.joining(";"));
		if (eligiblesAsString.isBlank()) {
			return mainCategoryString;
		} else {
			return mainCategory + "|" + eligiblesAsString;
		}
	}

	// @Transient
	// @JsonIgnore
	// private String getAllTranslatedCategoriesAsString() {
	// Category mrCat = getMainRankings() != null ? this.getMainRankings().getCategory() : null;
	// String mainCategory = mrCat != null ? mrCat.getNameWithAgeGroup() : "";
	//
	// String mainCategoryString = mainCategory;
	// if (mrCat != null && !getMainRankings().getTeamMember()) {
	// mainCategoryString = mainCategory + RAthlete.NoTeamMarker;
	// }
	//
	// String eligiblesAsString = this.getParticipations().stream()
	// .filter(p -> (p.getCategory() != mrCat))
	// .sorted((a, b) -> a.getCategory().getAgeGroup().compareTo(b.getCategory().getAgeGroup()))
	// .map(p -> {
	// String catName = p.getCategory().getNameWithAgeGroup();
	// return catName + (!p.getTeamMember() ? RAthlete.NoTeamMarker : "");
	// })
	// .collect(Collectors.joining(";"));
	// if (eligiblesAsString.isBlank()) {
	// return mainCategoryString;
	// } else {
	// return mainCategory + "|" + eligiblesAsString;
	// }
	// }

	/**
	 * Number of attempt 1..3, relative to current lift
	 *
	 * @return
	 */
	@Transient
	@JsonIgnore
	public Integer getAttemptNumber() {
		return getAttemptsDone() % 3 + 1;
	}

	/**
	 * Gets the attempts done.
	 *
	 * @return the attemptsDone
	 */
	@Transient
	@JsonIgnore
	public Integer getAttemptsDone() {
		return getSnatchAttemptsDone() + getCleanJerkAttemptsDone();
	}

	/**
	 * Gets the best clean jerk.
	 *
	 * @return the bestCleanJerk
	 */
	@Transient
	@JsonIgnore
	public Integer getBestCleanJerk() {
		final int cj1 = zeroIfInvalid(this.cleanJerk1ActualLift);
		final int cj2 = zeroIfInvalid(this.cleanJerk2ActualLift);
		final int cj3 = zeroIfInvalid(this.cleanJerk3ActualLift);
		return max(0, cj1, cj2, cj3);
	}

	/**
	 * Gets the best clean jerk attempt number.
	 *
	 * @return the best clean jerk attempt number
	 */
	@Transient
	@JsonIgnore
	public int getBestCleanJerkAttemptNumber() {
		int referenceValue = getBestCleanJerk();
		if (referenceValue > 0) {
			if (zeroIfInvalid(this.cleanJerk3ActualLift) == referenceValue) {
				return 6;
			}
			if (zeroIfInvalid(this.cleanJerk2ActualLift) == referenceValue) {
				return 5;
			}
			if (zeroIfInvalid(this.cleanJerk1ActualLift) == referenceValue) {
				return 4;
			}
		}
		return 0; // no match - bomb-out.
	}

	/**
	 * Gets the best result attempt number.
	 *
	 * @return the best result attempt number
	 */
	@Transient
	@JsonIgnore
	public int getBestResultAttemptNumber() {
		int referenceValue = getBestCleanJerk();
		if (referenceValue > 0) {
			if (zeroIfInvalid(this.cleanJerk3ActualLift) == referenceValue) {
				return 6;
			}
			if (zeroIfInvalid(this.cleanJerk2ActualLift) == referenceValue) {
				return 5;
			}
			if (zeroIfInvalid(this.cleanJerk1ActualLift) == referenceValue) {
				return 4;
			}
		} else {
			if (referenceValue > 0) {
				referenceValue = getBestSnatch();
				if (zeroIfInvalid(this.snatch3ActualLift) == referenceValue) {
					return 3;
				}
				if (zeroIfInvalid(this.snatch2ActualLift) == referenceValue) {
					return 2;
				}
				if (zeroIfInvalid(this.snatch1ActualLift) == referenceValue) {
					return 1;
				}
			}
		}
		return 0; // no match - bomb-out.
	}

	/**
	 * Gets the best snatch.
	 *
	 * @return the bestSnatch
	 */
	@Transient
	@JsonIgnore
	public Integer getBestSnatch() {
		final int sn1 = zeroIfInvalid(this.snatch1ActualLift);
		final int sn2 = zeroIfInvalid(this.snatch2ActualLift);
		final int sn3 = zeroIfInvalid(this.snatch3ActualLift);
		return max(0, sn1, sn2, sn3);
	}

	/**
	 * Gets the best snatch attempt number.
	 *
	 * @return the best snatch attempt number
	 */
	@Transient
	@JsonIgnore
	public int getBestSnatchAttemptNumber() {
		int referenceValue = getBestSnatch();
		if (referenceValue > 0) {
			if (zeroIfInvalid(this.snatch3ActualLift) == referenceValue) {
				return 3;
			}
			if (zeroIfInvalid(this.snatch2ActualLift) == referenceValue) {
				return 2;
			}
			if (zeroIfInvalid(this.snatch1ActualLift) == referenceValue) {
				return 1;
			}
		}
		return 0; // no match - bomb-out.
	}

	/**
	 * Gets the best snatch attempt number.
	 *
	 * @return the best snatch attempt number
	 */
	@Transient
	@JsonIgnore
	public LocalDateTime getBestSnatchAttemptTime() {
		int referenceValue = getBestSnatch();
		if (referenceValue > 0) {
			if (zeroIfInvalid(this.snatch3ActualLift) == referenceValue) {
				return this.getSnatch3LiftTime();
			}
			if (zeroIfInvalid(this.snatch2ActualLift) == referenceValue) {
				return this.getSnatch2LiftTime();
			}
			if (zeroIfInvalid(this.snatch1ActualLift) == referenceValue) {
				return this.getSnatch1LiftTime();
			}
		}
		// should not be required - bomb-out.
		return LocalDateTime.MIN;
	}

	/**
	 * Gets the best snatch attempt number.
	 *
	 * @return the best snatch attempt number
	 */
	@Transient
	@JsonIgnore
	public LocalDateTime getBestCleanJerkAttemptTime() {
		int referenceValue = getBestCleanJerk();
		if (referenceValue > 0) {
			if (zeroIfInvalid(this.cleanJerk3ActualLift) == referenceValue) {
				return this.getCleanJerk3LiftTime();
			}
			if (zeroIfInvalid(this.cleanJerk2ActualLift) == referenceValue) {
				return this.getSnatch2LiftTime();
			}
			if (zeroIfInvalid(this.cleanJerk1ActualLift) == referenceValue) {
				return this.getCleanJerk1LiftTime();
			}
		}
		// should not be required - bomb-out.
		return LocalDateTime.MIN;
	}

	/**
	 * Gets the birth date.
	 *
	 * @return the birthDate
	 * @deprecated use getYearOfBirth
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public Integer getBirthDate() {
		return this.getYearOfBirth();
	}

	/**
	 * Gets the body weight.
	 *
	 * @return the bodyWeight
	 */
	public Double getBodyWeight() {
		return this.bodyWeight;
	}

	/**
	 * Athlete's bodyweight category, without gender (examples: 67, >109)
	 *
	 * @return the short category
	 */
	@Transient
	@JsonIgnore
	public String getBWCategory() {
		// logger./**/warn("getBWCategory {}", this.getFullName());
		final Category category = getCategory();
		if (category == null) {
			// logger./**/warn("category null");
			return "";
		}
		return category.getLimitString();
	}

	/**
	 * Gets the category.
	 *
	 * @return the category
	 */
	@JsonIdentityReference(alwaysAsId = true)
	public Category getCategory() {
		return this.category;
	}

	@Transient
	@JsonIgnore
	public String getCategoryCode() {
		return this.category != null ? this.category.getCode() : "-";
	}

	/**
	 * Compute the body weight at the maximum weight of the Athlete's category. Note: for the purpose of this
	 * computation, only "official" categories are used as the purpose is to totalRank athletes according to their
	 * competition potential.
	 *
	 * @return the category sinclair
	 */
	@Transient
	@JsonIgnore
	public Double getCategorySinclair() {
		Category category = getCategory();
		if (category == null) {
			return 0.0;
		}
		Double categoryWeight = category.getMaximumWeight();
		final Integer total1 = getTotal();
		if (total1 == null || total1 < 0.1) {
			return 0.0;
		}
		if (getGender() == Gender.M) { // $NON-NLS-1$
			if (categoryWeight < 55.0) {
				categoryWeight = 55.0;
			} else if (categoryWeight > getSinclairProperties().menMaxWeight()) {
				categoryWeight = getSinclairProperties().menMaxWeight();
			}
		} else if (getGender() == Gender.F) {
			if (categoryWeight < 45.0) {
				categoryWeight = 45.0;
			} else if (categoryWeight > getSinclairProperties().womenMaxWeight()) {
				categoryWeight = getSinclairProperties().womenMaxWeight();
			}
		} else {
			return 0.0D;
		}
		return getSinclair(categoryWeight);
	}

	/**
	 * Gets the sinclair factor.
	 *
	 * @return the sinclair factor
	 */
	@Transient
	@JsonIgnore
	public Double getCatSinclairFactor() {
		if (this.gender == Gender.M) {
			return sinclairFactor(this.getCategory().getMaximumWeight(), getSinclairProperties().menCoefficient(),
			        getSinclairProperties().menMaxWeight());
		} else if (getGender() == Gender.F) {
			return sinclairFactor(this.getCategory().getMaximumWeight(), getSinclairProperties().womenCoefficient(),
			        getSinclairProperties().womenMaxWeight());
		} else {
			return 0.0D;
		}
	}

	public int getCatSinclairRank() {
		return this.catSinclairRank;
	}

	/**
	 * Gets the clean jerk 1 actual lift.
	 *
	 * @return the clean jerk 1 actual lift
	 */
	public String getCleanJerk1ActualLift() {
		return emptyIfNull(this.cleanJerk1ActualLift);
	}

	/**
	 * Gets the clean jerk 1 as integer.
	 *
	 * @return the clean jerk 1 as integer
	 */
	@Transient
	@JsonIgnore
	public Integer getCleanJerk1AsInteger() {
		return asInteger(this.cleanJerk1ActualLift);
	}

	/**
	 * Gets the clean jerk 1 automatic progression.
	 *
	 * @return the clean jerk 1 automatic progression
	 */
	public String getCleanJerk1AutomaticProgression() {
		return "-"; // there is no such thing.
	}

	/**
	 * Gets the clean jerk 1 change 1.
	 *
	 * @return the clean jerk 1 change 1
	 */
	public String getCleanJerk1Change1() {
		return emptyIfNull(this.cleanJerk1Change1);
	}

	/**
	 * Gets the clean jerk 1 change 2.
	 *
	 * @return the clean jerk 1 change 2
	 */
	public String getCleanJerk1Change2() {
		return emptyIfNull(this.cleanJerk1Change2);
	}

	/**
	 * Gets the clean jerk 1 declaration.
	 *
	 * @return the clean jerk 1 declaration
	 */
	public String getCleanJerk1Declaration() {
		return emptyIfNull(this.cleanJerk1Declaration);
	}

	/**
	 * Gets the clean jerk 1 lift time.
	 *
	 * @return the clean jerk 1 lift time
	 */
	public LocalDateTime getCleanJerk1LiftTime() {
		return this.cleanJerk1LiftTime;
	}

	/**
	 * Gets the clean jerk 2 actual lift.
	 *
	 * @return the clean jerk 2 actual lift
	 */
	public String getCleanJerk2ActualLift() {
		return emptyIfNull(this.cleanJerk2ActualLift);
	}

	/**
	 * Gets the clean jerk 2 as integer.
	 *
	 * @return the clean jerk 2 as integer
	 */
	@Transient
	@JsonIgnore
	public Integer getCleanJerk2AsInteger() {
		return asInteger(this.cleanJerk2ActualLift);
	}

	/**
	 * Gets the clean jerk 2 automatic progression.
	 *
	 * @return the clean jerk 2 automatic progression
	 */
	public String getCleanJerk2AutomaticProgression() {
		final int prevVal = zeroIfInvalid(this.cleanJerk1ActualLift);
		return doAutomaticProgression(prevVal);
	}

	/**
	 * Gets the clean jerk 2 change 1.
	 *
	 * @return the clean jerk 2 change 1
	 */
	public String getCleanJerk2Change1() {
		return emptyIfNull(this.cleanJerk2Change1);
	}

	/**
	 * Gets the clean jerk 2 change 2.
	 *
	 * @return the clean jerk 2 change 2
	 */
	public String getCleanJerk2Change2() {
		return emptyIfNull(this.cleanJerk2Change2);
	}

	/**
	 * Gets the clean jerk 2 declaration.
	 *
	 * @return the clean jerk 2 declaration
	 */
	public String getCleanJerk2Declaration() {
		return emptyIfNull(this.cleanJerk2Declaration);
	}

	/**
	 * Gets the clean jerk 2 lift time.
	 *
	 * @return the clean jerk 2 lift time
	 */
	public LocalDateTime getCleanJerk2LiftTime() {
		return this.cleanJerk2LiftTime;
	}

	/**
	 * Gets the clean jerk 3 actual lift.
	 *
	 * @return the clean jerk 3 actual lift
	 */
	public String getCleanJerk3ActualLift() {
		return emptyIfNull(this.cleanJerk3ActualLift);
	}

	/**
	 * Gets the clean jerk 3 as integer.
	 *
	 * @return the clean jerk 3 as integer
	 */
	@Transient
	@JsonIgnore
	public Integer getCleanJerk3AsInteger() {
		return asInteger(this.cleanJerk3ActualLift);
	}

	/**
	 * Gets the clean jerk 3 automatic progression.
	 *
	 * @return the clean jerk 3 automatic progression
	 */
	public String getCleanJerk3AutomaticProgression() {
		final int prevVal = zeroIfInvalid(this.cleanJerk2ActualLift);
		return doAutomaticProgression(prevVal);
	}

	/**
	 * Gets the clean jerk 3 change 1.
	 *
	 * @return the clean jerk 3 change 1
	 */
	public String getCleanJerk3Change1() {
		return emptyIfNull(this.cleanJerk3Change1);
	}

	/**
	 * Gets the clean jerk 3 change 2.
	 *
	 * @return the clean jerk 3 change 2
	 */
	public String getCleanJerk3Change2() {
		return emptyIfNull(this.cleanJerk3Change2);
	}

	/**
	 * Gets the clean jerk 3 declaration.
	 *
	 * @return the clean jerk 3 declaration
	 */
	public String getCleanJerk3Declaration() {
		return emptyIfNull(this.cleanJerk3Declaration);
	}

	/**
	 * Gets the clean jerk 3 lift time.
	 *
	 * @return the clean jerk 3 lift time
	 */
	public LocalDateTime getCleanJerk3LiftTime() {
		return this.cleanJerk3LiftTime;
	}

	/**
	 * Gets the clean jerk attempts done.
	 *
	 * @return the cleanJerkAttemptsDone
	 */
	@Transient
	@JsonIgnore
	public Integer getCleanJerkAttemptsDone() {
		// if Athlete signals he wont take his remaining tries, a zero is entered
		// further lifts are not counted.
		int attempts = 0;
		if (!isEmpty(this.cleanJerk1ActualLift)) {
			attempts++;
		} else {
			return attempts;
		}
		if (!isEmpty(this.cleanJerk2ActualLift)) {
			attempts++;
		} else {
			return attempts;
		}
		if (!isEmpty(this.cleanJerk3ActualLift)) {
			attempts++;
		} else {
			return attempts;
		}
		return attempts;
	}

	/**
	 * Gets the clean jerk points.
	 *
	 * @return the clean jerk points
	 */
	@Transient
	@JsonIgnore
	public int getCleanJerkPoints() {
		Participation mr = getMainRankings();
		int points = (mr != null ? mr.getSnatchPoints() : 0);
		return points;
	}

	@Transient
	@JsonIgnore
	public int getCleanJerkRank() {
		return (getMainRankings() != null ? getMainRankings().getCleanJerkRank() : -1);
	}

	/**
	 * Gets the clean jerk total.
	 *
	 * @return total for clean and jerk
	 */
	@Transient
	@JsonIgnore
	public int getCleanJerkTotal() {
		final int cleanJerkTotal = max(0, zeroIfInvalid(this.cleanJerk1ActualLift),
		        zeroIfInvalid(this.cleanJerk2ActualLift),
		        zeroIfInvalid(this.cleanJerk3ActualLift));
		return cleanJerkTotal;
	}

	/**
	 * Gets the club.
	 *
	 * @return the club
	 */
	public String getClub() {
		return getTeam();
	}

	public String getCoach() {
		return this.coach;
	}

	/**
	 * Gets the combined points.
	 *
	 * @return the combined points
	 */
	@Transient
	@JsonIgnore
	public Integer getCombinedPoints() {
		if (Competition.getCurrent().isSnatchCJTotalMedals()) {
			return getSnatchPoints() + getCleanJerkPoints() + getTotalPoints();
		} else {
			return getTotalPoints();
		}
	}

	public int getCombinedRank() {
		return this.combinedRank;
	}

	/**
	 * Gets the current automatic.
	 *
	 * @return the current automatic
	 */
	@Transient
	@JsonIgnore
	public String getCurrentAutomatic() {
		switch (this.getAttemptsDone() + 1) {
			case 1:
				return this.getSnatch1Declaration();
			case 2:
				return this.getSnatch2AutomaticProgression();
			case 3:
				return this.getSnatch3AutomaticProgression();
			case 4:
				return this.getCleanJerk1Declaration();
			case 5:
				return this.getCleanJerk2AutomaticProgression();
			case 6:
				return this.getCleanJerk3AutomaticProgression();
		}
		return null;
	}

	/**
	 * Gets the current change 1.
	 *
	 * @return the current change 1
	 */
	@Transient
	@JsonIgnore
	public String getCurrentChange1() {
		switch (this.getAttemptsDone() + 1) {
			case 1:
				return this.getSnatch1Change1();
			case 2:
				return this.getSnatch2Change1();
			case 3:
				return this.getSnatch3Change1();
			case 4:
				return this.getCleanJerk1Change1();
			case 5:
				return this.getCleanJerk2Change1();
			case 6:
				return this.getCleanJerk3Change1();
		}
		return null;
	}

	/**
	 * Gets the current declaration.
	 *
	 * @return the current declaration
	 */
	@Transient
	@JsonIgnore
	public String getCurrentDeclaration() {
		switch (this.getAttemptsDone() + 1) {
			case 1:
				return this.getSnatch1Declaration();
			case 2:
				return this.getSnatch2Declaration();
			case 3:
				return this.getSnatch3Declaration();
			case 4:
				return this.getCleanJerk1Declaration();
			case 5:
				return this.getCleanJerk2Declaration();
			case 6:
				return this.getCleanJerk3Declaration();
		}
		return null;
	}

	/**
	 * @return the custom1
	 */
	public String getCustom1() {
		return this.custom1;
	}

	/**
	 * @return the custom2
	 */
	public String getCustom2() {
		return this.custom2;
	}

	/**
	 * Gets the custom points.
	 *
	 * @return the customPoints
	 */
	@Transient
	@JsonIgnore
	public int getCustomPoints() {
		Participation mr = getMainRankings();
		int points = (mr != null ? mr.getCustomPoints() : 0);
		return points;
	}

	/**
	 * Gets the custom rank.
	 *
	 * @return the custom rank
	 */
	@Transient
	@JsonIgnore
	public int getCustomRank() {
		return (getMainRankings() != null ? getMainRankings().getCustomRank() : -1);
	}

	public Double getCustomScore() {
		return this.customScore;
	}

	/**
	 * Gets the custom score.
	 *
	 * @return the custom score
	 */
	@Transient
	@JsonIgnore
	public Double getCustomScoreComputed() {
		AgeGroup ageGroup = getAgeGroup();
		if (ageGroup == null) {
			return 0.0;
		}
		Ranking scoringSystem = ageGroup.getScoringSystem();
		// avoid infinite recursion
		if (scoringSystem != null && scoringSystem != Ranking.CUSTOM) {
			return Ranking.getRankingValue(this, scoringSystem);
		} else if (this.customScore == null || this.customScore < 0.01) {
			return Ranking.getRankingValue(this, Ranking.TOTAL);
		} else {
			return this.customScore;
		}
	}

	/**
	 * Gets the display category.
	 *
	 * @return the display category
	 */
	@Transient
	@JsonIgnore
	public String getDisplayCategory() {
		Category category = getCategory();
		return (category != null ? category.getDisplayName() : "");
	}

	@JsonIgnore
	@Transient
	public DisplayGroup getDisplayGroup() {
		return this.group != null ? new DisplayGroup(
		        this.group.getName(),
		        this.group.getDescription(),
		        this.group.getPlatform(),
		        this.group.getWeighInShortDateTime(),
		        this.group.getCompetitionShortDateTime())
		        : Group.getEmptyDisplayGroup();
	}

	@Transient
	@JsonIgnore
	public Set<Category> getEligibleCategories() {
		// brain dead version, cannot get query version to work.
		Set<Category> s = new LinkedHashSet<>();
		List<Participation> participations2 = getParticipations();
		for (Participation p : participations2) {
			Category category2 = p.getCategory();
			s.add(category2);
		}
		return s;
	}

	@Transient
	@JsonIgnore
	public String getEligibleCategoriesAsString() {
		Category mrCat = getMainRankings() != null ? this.getMainRankings().getCategory() : null;
		String mainCategory = mrCat != null ? mrCat.getDisplayName() : "";

		String mainCategoryString = mainCategory;
		if (mrCat != null && !getMainRankings().getTeamMember()) {
			mainCategoryString = mainCategory + RAthlete.NoTeamMarker;
		}

		String eligiblesAsString = this.getParticipations().stream()
		        .filter(p -> (p.getCategory() != mrCat))
		        .sorted((a, b) -> a.getCategory().getAgeGroup().compareTo(b.getCategory().getAgeGroup()))
		        .map(p -> {
			        String catName = p.getCategory().getNameWithAgeGroup();
			        return catName;
		        })
		        .collect(Collectors.joining(";"));
		if (eligiblesAsString.isBlank()) {
			return mainCategoryString;
		} else {
			return mainCategory + ";" + eligiblesAsString;
		}
	}

	@Transient
	@JsonIgnore
	public String getAgeGroupCodesMainFirstAsString() {
		Category mrCat = getMainRankings() != null ? this.getMainRankings().getCategory() : null;
		String mainCategory = mrCat != null ? mrCat.getAgeGroup().getCode() : "";

		String delimiter = ", ";
		String eligiblesAsString = this.getParticipations().stream()
		        .filter(p -> (p.getCategory() != mrCat))
		        .sorted((a, b) -> a.getCategory().getAgeGroup().compareTo(b.getCategory().getAgeGroup()))
		        .map(p -> {
			        String catName = p.getCategory().getAgeGroup().getCode();
			        return catName;
		        })
		        .collect(Collectors.joining(delimiter));
		if (eligiblesAsString.isBlank()) {
			return mainCategory;
		} else {
			return (!mainCategory.isBlank() ? mainCategory + delimiter : "") + eligiblesAsString;
		}
	}

	public Integer getEntryTotal() {
		// intentional, this is the legacy name of the column in the database
		return getQualifyingTotal();
	}

	public String getFederationCodes() {
		return this.federationCodes;
	}

	/**
	 * Gets the first attempted lift time.
	 *
	 * @return the first attempted lift time
	 */
	@Transient
	@JsonIgnore
	public LocalDateTime getFirstAttemptedLiftTime() {
		LocalDateTime attemptTime = LocalDateTime.MAX;// forever in the future
		if (zeroIfInvalid(this.snatch1ActualLift) != 0) {
			attemptTime = getSnatch1LiftTime();
		} else if (zeroIfInvalid(this.snatch2ActualLift) != 0) {
			attemptTime = getSnatch2LiftTime();
		} else if (zeroIfInvalid(this.snatch3ActualLift) != 0) {
			attemptTime = getSnatch3LiftTime();
		} else if (zeroIfInvalid(this.cleanJerk1ActualLift) != 0) {
			attemptTime = getCleanJerk1LiftTime();
		} else if (zeroIfInvalid(this.cleanJerk2ActualLift) != 0) {
			attemptTime = getCleanJerk2LiftTime();
		} else if (zeroIfInvalid(this.cleanJerk3ActualLift) != 0) {
			attemptTime = getCleanJerk3LiftTime();
		}
		return attemptTime;
	}

	/**
	 * Gets the first name.
	 *
	 * @return the firstName
	 */
	public String getFirstName() {
		return this.firstName;
	}

	public FieldOfPlay getFop() {
		if (this.fop == null) {
			return OwlcmsSession.getFop();
		}
		return this.fop;
	}

	@Transient
	@JsonIgnore
	public String getFormattedBirth() {
		if (Competition.getCurrent().isUseBirthYear()) {
			Integer yearOfBirth = getYearOfBirth();
			return yearOfBirth != null ? yearOfBirth.toString() : "";
		} else {
			Locale locale = OwlcmsSession.getLocale();
			String shortPattern = DateTimeUtils.localizedShortDatePattern(locale);
			DateTimeFormatter shortStyleFormatter = DateTimeFormatter.ofPattern(shortPattern, locale);
			return getFullBirthDate().format(shortStyleFormatter);
		}
	}

	@Transient
	@JsonIgnore
	public String getIsoBirth() {
		if (Competition.getCurrent().isUseBirthYear()) {
			Integer yearOfBirth = getYearOfBirth();
			return yearOfBirth != null ? yearOfBirth.toString() : "";
		} else {
			DateTimeFormatter shortStyleFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			return getFullBirthDate().format(shortStyleFormatter);
		}
	}

	/**
	 * Gets the full birth date.
	 *
	 * @return the fullBirthDate
	 */
	public LocalDate getFullBirthDate() {
		return this.fullBirthDate;
	}

	@Transient
	@JsonIgnore
	public String getFullId() {
		String fullName = getFullName();
		Category category2 = getCategory();
		if (!fullName.isEmpty()) {
			return fullName + " " + (category2 != null ? category2 : "");
			// +(startNumber2 != null && startNumber2 >0 ? " ["+startNumber2+"]" : "");
		} else {
			return "";
		}
	}

	@Transient
	@JsonIgnore
	public String getFullName() {
		String upperCase = this.getLastName() != null ? this.getLastName().toUpperCase() : "";
		String firstName2 = this.getFirstName() != null ? this.getFirstName() : "";
		if ((upperCase != null) && !upperCase.trim().isEmpty() && (firstName2 != null)
		        && !firstName2.trim().isEmpty()) {
			String fullName = Translator.translate("FullNameFormat", upperCase, firstName2);
			return fullName;
		} else {
			return "";
		}
	}

	/**
	 * Gets the gender.
	 *
	 * @return the gender
	 */
	public Gender getGender() {
		return this.gender;
	}

	/**
	 * Gets the group.
	 *
	 * @return the group
	 */
	@JsonIdentityReference(alwaysAsId = true)
	public Group getGroup() {
		return this.group;
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
	 * Gets the last attempted lift time.
	 *
	 * @return the last attempted lift time
	 */
	@Transient
	@JsonIgnore
	public LocalDateTime getLastAttemptedLiftTime() {
		if (zeroIfInvalid(this.cleanJerk3ActualLift) != 0) {
			return getCleanJerk3LiftTime();
		}
		if (zeroIfInvalid(this.cleanJerk2ActualLift) != 0) {
			return getCleanJerk2LiftTime();
		}
		if (zeroIfInvalid(this.cleanJerk1ActualLift) != 0) {
			return getCleanJerk1LiftTime();
		}
		if (zeroIfInvalid(this.snatch3ActualLift) != 0) {
			return getSnatch3LiftTime();
		}
		if (zeroIfInvalid(this.snatch2ActualLift) != 0) {
			return getSnatch2LiftTime();
		}
		if (zeroIfInvalid(this.snatch1ActualLift) != 0) {
			return getSnatch1LiftTime();
		}
		return LocalDateTime.MIN; // long ago
	}

	/**
	 * Gets the last name.
	 *
	 * @return the lastName
	 */
	public String getLastName() {
		return this.lastName;
	}

	/**
	 * Gets the last successful lift time.
	 *
	 * @return the last successful lift time
	 */
	@Transient
	@JsonIgnore
	public LocalDateTime getLastSuccessfulLiftTime() {
		if (zeroIfInvalid(this.cleanJerk3ActualLift) > 0) {
			return getCleanJerk3LiftTime();
		}
		if (zeroIfInvalid(this.cleanJerk2ActualLift) > 0) {
			return getCleanJerk2LiftTime();
		}
		if (zeroIfInvalid(this.cleanJerk1ActualLift) > 0) {
			return getCleanJerk1LiftTime();
		}
		if (zeroIfInvalid(this.snatch3ActualLift) > 0) {
			return getSnatch3LiftTime();
		}
		if (zeroIfInvalid(this.snatch2ActualLift) > 0) {
			return getSnatch2LiftTime();
		}
		if (zeroIfInvalid(this.snatch1ActualLift) > 0) {
			return getSnatch1LiftTime();
		}
		return LocalDateTime.MIN; // long ago
	}

	/**
	 * Gets the lift order rank.
	 *
	 * @return the lift order rank
	 */
	public Integer getLiftOrderRank() {
		return this.liftOrderRank;
	}

	/**
	 * Gets the logger.
	 *
	 * @return the logger
	 */
	@Transient
	@JsonIgnore
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Gets the long category.
	 *
	 * @return the long category
	 */
	@Transient
	@JsonIgnore
	public String getLongCategory() {
		Category category = getCategory();
		return (category != null ? category.getNameWithAgeGroup() : "");
	}

	/**
	 * Gets the lot number.
	 *
	 * @return the lotNumber
	 */
	public Integer getLotNumber() {
		return (this.lotNumber == null ? 0 : this.lotNumber);
	}

	@Transient
	@JsonIgnore
	public Participation getMainRankings() {
		Participation curRankings = null;
		List<Participation> participations2 = getParticipations();
		// logger.trace("athlete {} category {} participations {}", this, category,
		// participations2);
		for (Participation eligible : participations2) {
			Category eligibleCat = eligible.getCategory();
			if (this.category != null && eligibleCat != null) {
				String eligibleCode = eligibleCat.getComputedCode();
				String catCode = this.category.getComputedCode();
				if (StringUtils.equals(eligibleCode, catCode)) {
					curRankings = eligible;
					// logger.trace("yep eligibleCode '{}' catCode '{}'", eligibleCode, catCode);
					break;
				} else {
					// logger.trace("nope eligibleCode '{}' catCode '{}'", eligibleCode, catCode);
				}
			}
		}
		return curRankings;
	}

	/**
	 * Get the age group. mastersAgeGroup is a misnomer.
	 *
	 * @return the masters age group
	 */
	@Transient
	@JsonIgnore
	public String getMastersAgeGroup() {
		if (this.getGender() == null || this.getAgeGroup() == null) {
			return "";
		}
		return getAgeGroup().getName();
	}

	/**
	 * Gets the masters age group interval.
	 *
	 * @return the ageGroup
	 */
	@Transient
	@JsonIgnore
	public String getMastersAgeGroupInterval() {
		AgeGroup ag = getAgeGroup();
		if (ag == null) {
			return "";
		}

		if (ag.getMinAge() == 0) {
			return "<" + ag.getMaxAge();
		} else if (ag.getMaxAge() == 999) {
			return ">" + ag.getMinAge();
		} else {
			return ag.getMinAge() + "-" + ag.getMaxAge();
		}
	}

	/**
	 * Gets the masters gender age group interval.
	 *
	 * @return the masters gender age group interval
	 */
	@Transient
	@JsonIgnore
	public String getMastersGenderAgeGroupInterval() {
		String gender2 = getGender().name();
		if (gender2 == "F") {
			gender2 = "W";
		}
		return gender2.toUpperCase() + getMastersAgeGroupInterval();
	}

	/**
	 * Gets the masters category with age group.
	 *
	 * @return the masters long category
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public String getMastersLongCategory() {
		return getCategory().getDisplayName();
	}

	/**
	 * Gets the medal rank.
	 *
	 * @return the medal rank
	 */
	@Transient
	@JsonIgnore
	public Integer getMedalRank() {
		Integer i = getRank();
		if (i == null) {
			return 0;
		}
		return (i <= 3 ? i : 0);
	}

	/**
	 * Gets the membership.
	 *
	 * @return the membership
	 */
	public String getMembership() {
		return this.membership;
	}

	/**
	 * Gets the next attempt requested weight.
	 *
	 * @return the nextAttemptRequestedWeight
	 */
	@Transient
	@JsonIgnore
	public Integer getNextAttemptRequestedWeight() {
		int attempt = getAttemptsDone() + 1;
		return getRequestedWeightForAttempt(attempt);
	}

	public List<Participation> getParticipations() {
		return this.participations;
	}

	public Integer getPersonalBestCleanJerk() {
		return this.personalBestCleanJerk;
	}

	public Integer getPersonalBestSnatch() {
		return this.personalBestSnatch;
	}

	public Integer getPersonalBestTotal() {
		return this.personalBestTotal;
	}

	@Transient
	@JsonIgnore
	public Set<String> getPossibleAgeGroupTeams() {
		// we use strings because I can't figure out why AgeGroups don't behave properly
		// in a checkbox group
		Set<String> s = new LinkedHashSet<>();
		List<Participation> participations2 = getParticipations();
		List<Category> pcats = participations2.stream().map(p -> p.getCategory()).collect(Collectors.toList());
		pcats.sort(new RegistrationPreferenceComparator());
		for (Category p : pcats) {
			s.add(p.getAgeGroup().getDisplayName());
		}
		return s;
	}

	@Transient
	@JsonIgnore
	public Double getPresumedBodyWeight() {
		Double bodyWeight2 = getBodyWeight();
		if (bodyWeight2 != null && bodyWeight2 >= 0) {
			return bodyWeight2;
		}
		if (this.category != null) {
			return this.category.getMaximumWeight();
		}
		return this.presumedBodyWeight;
	}

	@Transient
	@JsonIgnore
	public String getPresumedBodyWeightString() {
		Double bodyWeight2 = getBodyWeight();
		if (this.category != null) {
			if (this.category.getMaximumWeight() > 998) {
				return this.gender + String.format("%04d", Math.round(this.category.getMinimumWeight() + 1));
			}
			return this.gender + String.format("%04d", Math.round(this.category.getMaximumWeight()));
		} else if (bodyWeight2 != null && bodyWeight2 >= 0) {
			return this.gender + String.format("%04d", Math.round(bodyWeight2));
		} else {
			return this.gender + "9999";
		}
	}

	@Transient
	@JsonIgnore
	public String getPresumedOpenCategoryString() {
		if (this.category != null) {
			return this.gender.asPublicGenderCode() + " " + this.category.getUpperBound();
		}
		Double bw = getPresumedBodyWeight();
		if (bw != null && this.gender != null) {
			return this.gender.asPublicGenderCode() + " " + Math.round(bw);
		}
		return "";
	}

	/**
	 * Compute the time of last lift for Athlete. Times are only compared within the same lift type (if a Athlete is at
	 * the first attempt of clean and jerk, then the last lift occurred forever ago.)
	 *
	 * @return null if Athlete has not lifted
	 */
	@Transient
	@JsonIgnore
	public LocalDateTime getPreviousLiftTime() {
		LocalDateTime max = null; // long ago

		if (getAttemptsDone() <= 3) {
			final LocalDateTime sn1 = getSnatch1LiftTime();
			if (sn1 != null) {
				max = sn1;
			} else {
				return max;
			}
			final LocalDateTime sn2 = getSnatch2LiftTime();
			if (sn2 != null) {
				max = (max.isAfter(sn2) ? max : sn2);
			} else {
				return max;
			}
			final LocalDateTime sn3 = getSnatch3LiftTime();
			if (sn3 != null) {
				max = (max.isAfter(sn3) ? max : sn3);
			} else {
				return max;
			}
		} else {
			final LocalDateTime cj1 = getCleanJerk1LiftTime();
			if (cj1 != null) {
				max = cj1;
			} else {
				return max;
			}
			final LocalDateTime cj2 = getCleanJerk2LiftTime();
			if (cj2 != null) {
				max = (max.isAfter(cj2) ? max : cj2);
			} else {
				return max;
			}
			final LocalDateTime cj3 = getCleanJerk3LiftTime();
			if (cj3 != null) {
				max = (max.isAfter(cj3) ? max : cj3);
			} else {
				return max;
			}
		}

		return max;
	}

	@Transient
	@JsonIgnore
	public Double getqPoints() {
		Integer total = getBestCleanJerk() + getBestSnatch();
		return qPoints.getQPoints(this, total);
	}

	/**
	 * Needed because the Excel getters do not follow the weird Bean getter convention.
	 *
	 * @param bodyWeight1
	 * @param total1
	 * @return
	 */
	@Transient
	@JsonIgnore
	public Double getQPoints() {
		Integer total = getBestCleanJerk() + getBestSnatch();
		return qPoints.getQPoints(this, total);
	}

	public Integer getqPointsRank() {
		return this.qPointsRank;
	}

	/**
	 * Gets the qualifying total.
	 *
	 * @return the qualifying total
	 */
	public Integer getQualifyingTotal() {
		if (this.qualifyingTotal == null) {
			return 0;
		}
		return this.qualifyingTotal;
	}

	/**
	 * Gets the rank.
	 *
	 * @return the rank
	 */
	@Transient
	@JsonIgnore
	public Integer getRank() {
		Participation mainRankings = getMainRankings();
		return mainRankings != null ? mainRankings.getTotalRank() : null;
	}

	/**
	 * Gets the registration category. Only used in reports.
	 *
	 * @return the registration category
	 */
	@Transient
	@JsonIgnore
	public Category getRegistrationCategory() {
		return this.category;
	}

	/**
	 * Gets the requested weight for attempt.
	 *
	 * @param attempt the attempt
	 * @return the requested weight for attempt
	 */
	@Transient
	@JsonIgnore
	public Integer getRequestedWeightForAttempt(int attempt) {
		switch (attempt) {
			case 1:
				return last(zeroIfInvalid(getSnatch1AutomaticProgression()), zeroIfInvalid(this.snatch1Declaration),
				        zeroIfInvalid(this.snatch1Change1), zeroIfInvalid(this.snatch1Change2));
			case 2:
				return last(zeroIfInvalid(getSnatch2AutomaticProgression()), zeroIfInvalid(this.snatch2Declaration),
				        zeroIfInvalid(this.snatch2Change1), zeroIfInvalid(this.snatch2Change2));
			case 3:
				return last(zeroIfInvalid(getSnatch3AutomaticProgression()), zeroIfInvalid(this.snatch3Declaration),
				        zeroIfInvalid(this.snatch3Change1), zeroIfInvalid(this.snatch3Change2));
			case 4:
				return last(zeroIfInvalid(getCleanJerk1AutomaticProgression()),
				        zeroIfInvalid(this.cleanJerk1Declaration),
				        zeroIfInvalid(this.cleanJerk1Change1), zeroIfInvalid(this.cleanJerk1Change2));
			case 5:
				return last(zeroIfInvalid(getCleanJerk2AutomaticProgression()),
				        zeroIfInvalid(this.cleanJerk2Declaration),
				        zeroIfInvalid(this.cleanJerk2Change1), zeroIfInvalid(this.cleanJerk2Change2));
			case 6:
				return last(zeroIfInvalid(getCleanJerk3AutomaticProgression()),
				        zeroIfInvalid(this.cleanJerk3Declaration),
				        zeroIfInvalid(this.cleanJerk3Change1), zeroIfInvalid(this.cleanJerk3Change2));
		}
		return 0;
	}

	/**
	 * Gets the robi. In a multiple age group competition, the Robi shown in the final package will depend on the age
	 * group
	 *
	 * @return the robi
	 */
	@Transient
	@JsonIgnore
	public Double getRobi() {
		Integer wr = getRobiWr();
		if (wr == null) {
			return 0.0D;
		}

		// assuming that ROBI_B does not change per age group -- should not
		// since is same for women and men
		double robiA = 1000.0D / Math.pow(wr, Category.ROBI_B);
		double robi = robiA * Math.pow(getTotal(), Category.ROBI_B);

		return robi;
	}

	/**
	 * Gets the robi rank.
	 *
	 * @return the robi rank
	 */
	public Integer getRobiRank() {
		return this.robiRank;
	}

	@Transient
	@JsonIgnore
	public Integer getRobiWr() {
		Participation mainRankings;
		Category c;
		if ((mainRankings = getMainRankings()) == null || (c = mainRankings.getCategory()) == null) {
			return null;
		}
		Integer wr = c.getWr();
		if (wr == null || wr <= 0.000001) {
			// not an IWF category, find what the IWF Robi would be for age/body weight
			Category robiC = RobiCategories.findRobiCategory(this);
			if (robiC == null) {
				return null;
			}
			Integer age = getAge();
			if (age != null) {
				wr = robiC.getWr(age);
			} else {
				return robiC.getWr(999);
			}
		}
		return wr;
	}

	@Transient
	@JsonIgnore
	public String getRoundedBodyWeight() {
		if (this.df == null) {
			this.df = new DecimalFormat("#.##");
		}
		return this.df.format(getBodyWeight());
	}

	@Transient
	@JsonIgnore
	public Double getBestLifterScore() {
		return Ranking.getRankingValue(this, Competition.getCurrent().getScoringSystem());
	}
	
	@Transient
	@JsonIgnore
	public int getBestLifterRank() {
		return Ranking.getRanking(this, Competition.getCurrent().getScoringSystem());
	}

	public String getSessionPattern() {
		Group g = getGroup();
		return (g != null ? Translator.translate("ChallengeCard.SessionPattern", g.getName()) : "");
	}

	/**
	 * @see #getBWCategory()
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public String getShortCategory() {
		return getBWCategory();
	}

	@Transient
	@JsonIgnore
	public String getShortName() {
		String firstName2 = getFirstName();
		Integer startNumber2 = getStartNumber();
		return "#" + (startNumber2 != null ? startNumber2 : "?") + " " + getLastName() + " "
		        + (firstName2 == null || firstName2.isBlank() ? "" : firstName2.substring(0, 1) + ".");
	}

	/**
	 * Compute the Sinclair total for the Athlete, that is, the total multiplied by a value that depends on the
	 * Athlete's body weight. This value extrapolates what the Athlete would have lifted if he/she had the bodymass of a
	 * maximum-weight Athlete.
	 *
	 * @return the sinclair-adjusted value for the Athlete
	 */
	@Transient
	@JsonIgnore
	public Double getSinclair() {
		final Double bodyWeight1 = getBodyWeight();
		if (bodyWeight1 == null) {
			return 0.0;
		}
		return getSinclair(bodyWeight1);
	}

	/**
	 * Gets the sinclair.
	 *
	 * @param bodyWeight1 the body weight 1
	 * @return the sinclair
	 */
	@Transient
	@JsonIgnore
	public Double getSinclair(Double bodyWeight1) {
		Integer total1 = getTotal();
		return getSinclair(bodyWeight1, total1);
	}

	/**
	 * Gets the sinclair factor.
	 *
	 * @return the sinclair factor
	 */
	@Transient
	@JsonIgnore
	public Double getSinclairFactor() {
		if (this.gender == Gender.M) {
			return sinclairFactor(this.bodyWeight, getSinclairProperties().menCoefficient(),
			        getSinclairProperties().menMaxWeight());
		} else if (getGender() == Gender.F) {
			return sinclairFactor(this.bodyWeight, getSinclairProperties().womenCoefficient(),
			        getSinclairProperties().womenMaxWeight());
		} else {
			return 0.0D;
		}
	}

	/**
	 * Gets the sinclair for delta.
	 *
	 * @return a Sinclair value even if c&j has not started
	 */
	@Transient
	@JsonIgnore
	public Double getSinclairForDelta() {
		final Double bodyWeight1 = getBodyWeight();
		if (bodyWeight1 == null) {
			return 0.0;
		}
		Integer total1 = getBestCleanJerk() + getBestSnatch();
		return getSinclair(bodyWeight1, total1);
	}

	/**
	 * Gets the sinclair rank.
	 *
	 * @return the sinclair rank
	 */
	public Integer getSinclairRank() {
		return this.sinclairRank;
	}

	@Transient
	@JsonIgnore
	public Float getSmhfFactor() {
		final Integer birthDate1 = getYearOfBirth();
		if (birthDate1 == null) {
			return 0.0F;
		}
		return sinclairProperties2020.getAgeGenderCoefficient(YEAR - birthDate1, getGender());
	}

	/**
	 * Gets the smm.
	 *
	 * @return the smm
	 */
	@Transient
	@JsonIgnore
	public Double getSmhfForDelta() {
		double d = getMastersSinclairForDelta()
		        * getSmhfFactor();
		return d;
	}

	/**
	 * Gets the smm.
	 *
	 * @return the smm
	 */
	@Transient
	@JsonIgnore
	public Double getSmhf() {
		double d = getMastersSinclair() * getSmhfFactor();
		return d;
	}
	
	@Transient
	@JsonIgnore
	public Double getSmm() {
		return getSmhf();
	}

	@Transient
	@JsonIgnore
	public int getSmhfRank() {
		return this.smhfRank;
	}
	
	@Transient
	@JsonIgnore
	public int getSmmRank() {
		return getSmhfRank();
	}

	/**
	 * Gets the snatch 1 actual lift.
	 *
	 * @return the snatch 1 actual lift
	 */
	public String getSnatch1ActualLift() {
		return emptyIfNull(this.snatch1ActualLift);
	}

	/**
	 * Gets the snatch 1 as integer.
	 *
	 * @return the snatch 1 as integer
	 */
	@Transient
	@JsonIgnore
	public Integer getSnatch1AsInteger() {
		return asInteger(this.snatch1ActualLift);
	}

	/**
	 * Gets the snatch 1 automatic progression.
	 *
	 * @return the snatch 1 automatic progression
	 */
	public String getSnatch1AutomaticProgression() {
		return "-"; // no such thing.
	}

	/**
	 * Gets the snatch 1 change 1.
	 *
	 * @return the snatch 1 change 1
	 */
	public String getSnatch1Change1() {
		return emptyIfNull(this.snatch1Change1);
	}

	/**
	 * Gets the snatch 1 change 2.
	 *
	 * @return the snatch 1 change 2
	 */
	public String getSnatch1Change2() {
		return emptyIfNull(this.snatch1Change2);
	}

	/**
	 * Gets the snatch 1 declaration.
	 *
	 * @return the snatch 1 declaration
	 */
	public String getSnatch1Declaration() {
		return emptyIfNull(this.snatch1Declaration);
	}

	/**
	 * Gets the snatch 1 lift time.
	 *
	 * @return the snatch 1 lift time
	 */
	public LocalDateTime getSnatch1LiftTime() {
		return this.snatch1LiftTime;
	}

	/**
	 * Gets the snatch 2 actual lift.
	 *
	 * @return the snatch 2 actual lift
	 */
	public String getSnatch2ActualLift() {
		return emptyIfNull(this.snatch2ActualLift);
	}

	/**
	 * Gets the snatch 2 as integer.
	 *
	 * @return the snatch 2 as integer
	 */
	@Transient
	@JsonIgnore
	public Integer getSnatch2AsInteger() {
		return asInteger(this.snatch2ActualLift);
	}

	/**
	 * Gets the snatch 2 automatic progression.
	 *
	 * @return the snatch 2 automatic progression
	 */
	public String getSnatch2AutomaticProgression() {
		final int prevVal = zeroIfInvalid(this.snatch1ActualLift);
		return doAutomaticProgression(prevVal);
	}

	/**
	 * Gets the snatch 2 change 1.
	 *
	 * @return the snatch 2 change 1
	 */
	public String getSnatch2Change1() {
		return emptyIfNull(this.snatch2Change1);
	}

	/**
	 * Gets the snatch 2 change 2.
	 *
	 * @return the snatch 2 change 2
	 */
	public String getSnatch2Change2() {
		return emptyIfNull(this.snatch2Change2);
	}

	/**
	 * Gets the snatch 2 declaration.
	 *
	 * @return the snatch 2 declaration
	 */
	public String getSnatch2Declaration() {
		return emptyIfNull(this.snatch2Declaration);
	}

	/**
	 * Gets the snatch 2 lift time.
	 *
	 * @return the snatch 2 lift time
	 */
	public LocalDateTime getSnatch2LiftTime() {
		return this.snatch2LiftTime;
	}

	/**
	 * Gets the snatch 3 actual lift.
	 *
	 * @return the snatch 3 actual lift
	 */
	public String getSnatch3ActualLift() {
		return emptyIfNull(this.snatch3ActualLift);
	}

	/**
	 * Gets the snatch 3 as integer.
	 *
	 * @return the snatch 3 as integer
	 */
	@Transient
	@JsonIgnore
	public Integer getSnatch3AsInteger() {
		return asInteger(this.snatch3ActualLift);
	}

	/**
	 * Gets the snatch 3 automatic progression.
	 *
	 * @return the snatch 3 automatic progression
	 */
	public String getSnatch3AutomaticProgression() {
		final int prevVal = zeroIfInvalid(this.snatch2ActualLift);
		return doAutomaticProgression(prevVal);
	}

	/**
	 * Gets the snatch 3 change 1.
	 *
	 * @return the snatch 3 change 1
	 */
	public String getSnatch3Change1() {
		return emptyIfNull(this.snatch3Change1);
	}

	/**
	 * Gets the snatch 3 change 2.
	 *
	 * @return the snatch 3 change 2
	 */
	public String getSnatch3Change2() {
		return emptyIfNull(this.snatch3Change2);
	}

	/**
	 * Gets the snatch 3 declaration.
	 *
	 * @return the snatch 3 declaration
	 */
	public String getSnatch3Declaration() {
		return emptyIfNull(this.snatch3Declaration);
	}

	/**
	 * Gets the snatch 3 lift time.
	 *
	 * @return the snatch 3 lift time
	 */
	public LocalDateTime getSnatch3LiftTime() {
		return this.snatch3LiftTime;
	}

	/**
	 * Gets the snatch attempts done.
	 *
	 * @return how many snatch attempts have been performed
	 */
	@Transient
	@JsonIgnore
	public Integer getSnatchAttemptsDone() {
		// Athlete signals he wont take his remaining tries, a zero is entered
		// further lifts are not counted.
		int attempts = 0;
		if (!isEmpty(this.snatch1ActualLift)) {
			attempts++;
		} else {
			return attempts;
		}
		if (!isEmpty(this.snatch2ActualLift)) {
			attempts++;
		} else {
			return attempts;
		}
		if (!isEmpty(this.snatch3ActualLift)) {
			attempts++;
		} else {
			return attempts;
		}
		return attempts;
	}

	/**
	 * Gets the snatch points.
	 *
	 * @return the snatch points
	 */
	public int getSnatchPoints() {
		Participation mr = getMainRankings();
		int points = (mr != null ? mr.getSnatchPoints() : 0);
		return points;
	}

	public int getSnatchRank() {
		int snatchRank;
		if (getMainRankings() != null) {
			snatchRank = getMainRankings().getSnatchRank();
		} else {
			snatchRank = -1;
		}
		// logger.trace("{} snatchRank {}", this.getShortName(), snatchRank);
		return snatchRank;
	}

	/**
	 * Gets the snatch total.
	 *
	 * @return total for snatch.
	 */
	@Transient
	@JsonIgnore
	public int getSnatchTotal() {
		final int snatchTotal = max(0, zeroIfInvalid(this.snatch1ActualLift), zeroIfInvalid(this.snatch2ActualLift),
		        zeroIfInvalid(this.snatch3ActualLift));
		return snatchTotal;
	}

	/**
	 * Gets the start number.
	 *
	 * @return the start number
	 */
	public Integer getStartNumber() {
		return this.startNumber != null ? this.startNumber : 0;
	}

	public String getSubCategory() {
		if (Config.getCurrent().featureSwitch("UseCustom2AsSubCategory")) {
			return (this.getCustom2() != null && !this.getCustom2().isBlank()) ? this.getCustom2() : "";
		} else {
			return this.subCategory != null ? this.subCategory : "";
		}
	}

	/**
	 * Gets the team.
	 *
	 * @return the team
	 */
	public String getTeam() {
		return this.team;
	}

	@Transient
	@JsonIgnore
	public String getTeamAgeGroupsAsString() {
		Set<String> s = new LinkedHashSet<>();
		List<Participation> participations2 = getParticipations();
		for (Participation p : participations2) {
			if (p.getTeamMember()) {
				s.add(p.getCategory().getNameWithAgeGroup());
			}
		}

		if (s == null || s.isEmpty()) {
			return "";
		} else {
			String collect = s.stream().collect(Collectors.joining(";"));
			return collect;
		}
	}

	/**
	 * Gets the team clean jerk rank.
	 *
	 * @return the team clean jerk rank
	 */
	public Integer getTeamCleanJerkRank() {
		return this.teamCleanJerkRank;
	}

	/**
	 * Gets the team combined rank.
	 *
	 * @return the teamCombinedRank
	 */
	public Integer getTeamCombinedRank() {
		return this.teamCombinedRank;
	}

	public Integer getTeamCustomRank() {
		return this.teamCustomRank;
	}

	/**
	 * Gets the team member.
	 *
	 * @return the team member
	 */
	@Deprecated
	public Boolean getTeamMember() {
		return isEligibleForTeamRanking();
	}

	/**
	 * Gets the team robi rank.
	 *
	 * @return the teamRobiRank
	 */
	public Integer getTeamRobiRank() {
		return this.teamRobiRank;
	}

	/**
	 * Gets the team sinclair rank.
	 *
	 * @return the teamSinclairRank
	 */
	public Integer getTeamSinclairRank() {
		return this.teamSinclairRank;
	}

	/**
	 * Gets the team snatch rank.
	 *
	 * @return the team snatch rank
	 */
	public Integer getTeamSnatchRank() {
		return this.teamSnatchRank;
	}

	/**
	 * Gets the team total rank.
	 *
	 * @return the team total rank
	 */
	public Integer getTeamTotalRank() {
		return this.teamTotalRank;
	}

	/**
	 * Total is zero if all three snatches or all three clean&jerks are failed. Failed lifts are indicated as negative
	 * amounts. Total is the sum of all good lifts otherwise. Null entries indicate that no data has been captured, and
	 * are counted as zero.
	 *
	 * @return the total
	 */
	public Integer getTotal() {
		final int snatchTotal = getSnatchTotal();
		if (snatchTotal == 0) {
			return 0;
		}
		final int cleanJerkTotal = getCleanJerkTotal();
		if (cleanJerkTotal == 0) {
			return 0;
		}
		return snatchTotal + cleanJerkTotal;
	}

	/**
	 * Gets the total points.
	 *
	 * @return the total points
	 */
	public int getTotalPoints() {
		Participation mr = getMainRankings();
		int totalPoints = (mr != null ? mr.getTotalPoints() : 0);
		return totalPoints;
	}

	public int getTotalRank() {
		return (getMainRankings() != null ? getMainRankings().getTotalRank() : -1);
	}

	/**
	 * Gets the year of birth.
	 *
	 * @return the year of birth
	 */
	public Integer getYearOfBirth() {
		if (this.getFullBirthDate() != null) {
			// logger.trace(" getYearOfBirth {} {} {}", getFullBirthDate(),
			// getFullBirthDate().getYear(),
			// LoggerUtils.whereFrom());
			return getFullBirthDate().getYear();
		} else {
			return null;
		}
	}

	@Override
	public int hashCode() {
		return 31;
	}

	/**
	 * Checks if is a team member.
	 *
	 * @return true, if is a team member
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public boolean isATeamMember() {
		return isEligibleForTeamRanking();
	}

	public boolean isCheckTiming() {
		return this.checkTiming;
	}

	/**
	 * @return true if the last apparent change is a declaration
	 */
	public int isDeclaring() {
		// @formatter:off
		int attempt = getAttemptsDone() + 1;
		boolean declaring = false;
		boolean changing = false;
		switch (attempt) {
		case 1:
			declaring =
			(zeroIfInvalid(this.snatch1Declaration) > 0) &&
			(zeroIfInvalid(this.snatch1Change1) == 0) &&
			(zeroIfInvalid(this.snatch1Change2) == 0) &&
			(zeroIfInvalid(this.snatch1ActualLift) == 0);
			changing = false;
			break;
		case 2:
		{
			int ap = zeroIfInvalid(getSnatch2AutomaticProgression());
			int decl = zeroIfInvalid(this.snatch2Declaration);
			declaring =
					(ap > 0) &&
					(decl > 0) &&
					(zeroIfInvalid(this.snatch2Change1) == 0) &&
					(zeroIfInvalid(this.snatch2Change2) == 0) &&
					(zeroIfInvalid(this.snatch2ActualLift) == 0);
			changing = ap != decl;
		}
		break;
		case 3:
		{
			int ap = zeroIfInvalid(getSnatch3AutomaticProgression());
			int decl = zeroIfInvalid(this.snatch3Declaration);
			declaring =
					(ap > 0) &&
					(decl > 0) &&
					(zeroIfInvalid(this.snatch3Change1) == 0) &&
					(zeroIfInvalid(this.snatch3Change2) == 0) &&
					(zeroIfInvalid(this.snatch3ActualLift) == 0);
			changing = ap != decl;

		}
		break;
		case 4:
			declaring =
			(zeroIfInvalid(this.cleanJerk1Declaration) > 0) &&
			(zeroIfInvalid(this.cleanJerk1Change1) == 0) &&
			(zeroIfInvalid(this.cleanJerk1Change2) == 0) &&
			(zeroIfInvalid(this.cleanJerk1ActualLift) == 0);
			changing = false;
			break;
		case 5:
		{
			int ap = zeroIfInvalid(getCleanJerk2AutomaticProgression());
			int decl = zeroIfInvalid(this.cleanJerk2Declaration);
			declaring =
					(ap > 0) &&
					(decl > 0) &&
					(zeroIfInvalid(this.cleanJerk2Change1) == 0) &&
					(zeroIfInvalid(this.cleanJerk2Change2) == 0) &&
					(zeroIfInvalid(this.cleanJerk2ActualLift) == 0);
			changing = ap != decl;
		}
		break;
		case 6:
		{
			int ap = zeroIfInvalid(getCleanJerk3AutomaticProgression());
			int decl = zeroIfInvalid(this.cleanJerk3Declaration);
			declaring =
					(ap > 0) &&
					(decl > 0) &&
					(zeroIfInvalid(this.cleanJerk3Change1) == 0) &&
					(zeroIfInvalid(this.cleanJerk3Change2) == 0) &&
					(zeroIfInvalid(this.cleanJerk3ActualLift) == 0);
			changing = ap != decl;
		}
		break;
		}
		// @formatter:on
		if (declaring && changing) {
			return 1;
		} else if (declaring && !changing) {
			return 0;
		} else {
			return -1;
		}
	}

	public boolean isEligibleForIndividualRanking() {
		return this.eligibleForIndividualRanking;
	}

	public boolean isEligibleForTeamRanking() {
		return this.eligibleForTeamRanking;
	}

	/**
	 * Checks if is forced as current.
	 *
	 * @return true, if is forced as current
	 */
	@JsonIgnore
	public boolean isForcedAsCurrent() {
		return this.forcedAsCurrent;
	}

	/**
	 * Checks if is invited.
	 *
	 * @see #isEligibleForIndividualRanking()
	 * @return true, if is invited
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public boolean isInvited() {
		return !isEligibleForIndividualRanking();
	}

	@Transient
	@JsonIgnore
	public boolean isTeamMember() {
		return (getMainRankings() != null ? getMainRankings().getTeamMember() : false);
	}

	@Transient
	@JsonIgnore
	public boolean isValidation() {
		return this.validation && !isSkipValidationsDuringImport();
	}

	/**
	 * Long dump.
	 *
	 * @return the string
	 */
	public String longDump() {
		final Category category = this.getCategory();
		final Group group = this.getGroup();
		return (new StringBuilder()).append(" lastName=" + this.getLastName())
		        .append(" firstName=" + this.getFirstName()).append(" membership=" + this.getMembership())
		        .append(" lotNumber=" + this.getLotNumber())
		        .append(" group=" + (group != null ? group.getName() : null)).append(" team=" + this.getTeam())
		        .append(" gender=" + this.getGender()).append(" bodyWeight=" + this.getBodyWeight())
		        .append(" birthDate=" + this.getYearOfBirth())
		        .append(" category=" + (category != null ? category.getDisplayName().toLowerCase() : null))
		        .append(" actualCategory=" + this.getLongCategory().toString().toLowerCase())
		        .append(" snatch1ActualLift=" + this.getSnatch1ActualLift())
		        .append(" snatch2=" + this.getSnatch2ActualLift()).append(" snatch3=" + this.getSnatch3ActualLift())
		        .append(" bestSnatch=" + this.getBestSnatch())
		        .append(" cleanJerk1ActualLift=" + this.getCleanJerk1ActualLift())
		        .append(" cleanJerk2=" + this.getCleanJerk2ActualLift())
		        .append(" cleanJerk3=" + this.getCleanJerk3ActualLift()).append(" total=" + this.getTotal())
		        .append(" totalRank=" + this.getRank()).append(" teamMember=" + this.getTeamMember()).toString();
	}

	public void removeEligibleCategory(Category category) {
		for (Iterator<Participation> iterator = this.participations.iterator(); iterator.hasNext();) {
			Participation participation = iterator.next();

			boolean athleteEqual = participation.getAthlete().equals(this);

			Category category2 = participation.getCategory();
			boolean categoryEqual = sameCategory(category, category2);
			if (athleteEqual && categoryEqual) {
				logger.trace("removeCategory removing {} {}", category, participation);
				iterator.remove();
				if (category2 != null) {
					category2.getParticipations().remove(participation);
				}
				participation.setAthlete(null);
				participation.setCategory(null);
			} else {
				logger.trace("removeCategory skipping {} {} {} {} {}", this, athleteEqual, participation, category,
				        categoryEqual);
			}
		}
	}

	/**
	 * Reset forced as current.
	 */
	public void resetForcedAsCurrent() {
		setForcedAsCurrent(false);
	}

	@Transient
	@JsonIgnore
	public void setAgeGroupTeams(Set<String> s) {
		// we use strings because I can't figure out why AgeGroups don't behave properly
		// in a checkbox group
		List<Participation> participations2 = getParticipations();
		for (Participation p : participations2) {
			p.setTeamMember(s.contains(p.getCategory().getAgeGroup().getDisplayName()));
		}
	}

	/**
	 * Sets the attempts done.
	 *
	 * @param i the new attempts done
	 */
	public void setAttemptsDone(Integer i) {
	}

	/**
	 * Sets the best clean jerk.
	 *
	 * @param i the new best clean jerk
	 */
	public void setBestCleanJerk(Integer i) {
	}

	/**
	 * Sets the best snatch.
	 *
	 * @param i the new best snatch
	 */
	public void setBestSnatch(Integer i) {
	}

	/**
	 * Sets the birth date.
	 *
	 * @param birthYear the new birth date
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public void setBirthDate(Integer birthYear) {
		setYearOfBirth(birthYear);
	}

	/**
	 * Sets the body weight.
	 *
	 * @param bodyWeight the bodyWeight to set
	 */
	public void setBodyWeight(Double bodyWeight) {
		if (bodyWeight != null && bodyWeight <= 0.01) {
			this.bodyWeight = null;
		} else {
			this.bodyWeight = bodyWeight;
		}
	}

	/**
	 * Sets the category.
	 *
	 * @param category the category to set
	 */
	public void setCategory(Category category) {
		if (category != null) {
			// explicitly provided information, to be used if actual bodyweight is not yet
			// known
			setPresumedBodyWeight(category.getMaximumWeight());
		}
		this.category = category;
	}

	public void setCategoryDone(boolean done) {
		this.categoryDone = done;
	}

	public void setCatSinclairRank(int i) {
		this.catSinclairRank = i;
	}

	public void setCheckTiming(boolean checkTiming) {
		// logger.debug("===== setting timing check {}, {}", checkTiming, LoggerUtils.stackTrace());
		this.checkTiming = checkTiming;
	}

	/**
	 * Sets the clean jerk 1 actual lift.
	 *
	 * @param cleanJerk1ActualLift the new clean jerk 1 actual lift
	 */
	public void setCleanJerk1ActualLift(String cleanJerk1ActualLift) {
		if (isValidation()) {
			validateCleanJerk1ActualLift(cleanJerk1ActualLift);
		}
		this.cleanJerk1ActualLift = cleanJerk1ActualLift;
		getLogger().info("{}{} cleanJerk1ActualLift={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk1ActualLift);
	}

	/**
	 * Sets the clean jerk 1 automatic progression.
	 *
	 * @param s the new clean jerk 1 automatic progression
	 */
	public void setCleanJerk1AutomaticProgression(String s) {
	}

	/**
	 * Sets the clean jerk 1 change 1.
	 *
	 * @param cleanJerk1Change1 the new clean jerk 1 change 1
	 */
	public void setCleanJerk1Change1(String cleanJerk1Change1) {
		if ("0".equals(cleanJerk1Change1)) {
			this.cleanJerk1Change1 = cleanJerk1Change1;
			getLogger().info("{}{} cleanJerk1Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk1Change1);
			setCleanJerk1ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateCleanJerk1Change1(cleanJerk1Change1);
		}
		this.cleanJerk1Change1 = cleanJerk1Change1;
		// validateStartingTotalsRule();

		getLogger().info("{}{} cleanJerk1Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk1Change1);
	}

	/**
	 * Sets the clean jerk 1 change 2.
	 *
	 * @param cleanJerk1Change2 the new clean jerk 1 change 2
	 */
	public void setCleanJerk1Change2(String cleanJerk1Change2) {
		if ("0".equals(cleanJerk1Change2)) {
			this.cleanJerk1Change2 = cleanJerk1Change2;
			getLogger().info("{}{} cleanJerk1Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk1Change2);
			setCleanJerk1ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateCleanJerk1Change2(cleanJerk1Change2);
		}
		this.cleanJerk1Change2 = cleanJerk1Change2;
		// validateStartingTotalsRule();

		getLogger().info("{}{} cleanJerk1Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk1Change2);
	}

	/**
	 * Sets the clean jerk 1 declaration.
	 *
	 * @param cleanJerk1Declaration the new clean jerk 1 declaration
	 */
	public void setCleanJerk1Declaration(String cleanJerk1Declaration) {
		// if ("0".equals(cleanJerk1Declaration)) {
		// this.cleanJerk1Declaration = cleanJerk1Declaration;
		// getLogger().info("{}{} cleanJerk1Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		// cleanJerk1Declaration);
		// setCleanJerk1ActualLift("0");
		// return;
		// }

		if (isValidation()) {
			validateCleanJerk1Declaration(cleanJerk1Declaration);
		}
		this.cleanJerk1Declaration = cleanJerk1Declaration;
		// if (zeroIfInvalid(getSnatch1Declaration()) > 0)
		// // validateStartingTotalsRule();

		getLogger().info("{}{} cleanJerk1Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk1Declaration);
	}

	public void setCleanJerk1LiftTime(LocalDateTime cleanJerk1LiftTime) {
		this.cleanJerk1LiftTime = cleanJerk1LiftTime;
	}

	/**
	 * Sets the clean jerk 2 actual lift.
	 *
	 * @param cleanJerk2ActualLift the new clean jerk 2 actual lift
	 */
	public void setCleanJerk2ActualLift(String cleanJerk2ActualLift) {
		if (isValidation()) {
			validateCleanJerk2ActualLift(cleanJerk2ActualLift);
		}
		this.cleanJerk2ActualLift = cleanJerk2ActualLift;
		getLogger().info("{}{} cleanJerk2ActualLift={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk2ActualLift);

		// if (zeroIfInvalid(cleanJerk2ActualLift) == 0) {
		// this.setCleanJerk2LiftTime((LocalDateTime) null);
		// } else {
		// this.setCleanJerk2LiftTime(LocalDateTime.now());
		// }
	}

	/**
	 * Sets the clean jerk 2 automatic progression.
	 *
	 * @param s the new clean jerk 2 automatic progression
	 */
	public void setCleanJerk2AutomaticProgression(String s) {
	}

	/**
	 * Sets the clean jerk 2 change 1.
	 *
	 * @param cleanJerk2Change1 the new clean jerk 2 change 1
	 */
	public void setCleanJerk2Change1(String cleanJerk2Change1) {
		if ("0".equals(cleanJerk2Change1)) {
			this.cleanJerk2Change1 = cleanJerk2Change1;
			getLogger().info("{}{} cleanJerk2Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk2Change1);
			setCleanJerk2ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateCleanJerk2Change1(cleanJerk2Change1);
		}
		this.cleanJerk2Change1 = cleanJerk2Change1;
		getLogger().info("{}{} cleanJerk2Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk2Change1);
	}

	/**
	 * Sets the clean jerk 2 change 2.
	 *
	 * @param cleanJerk2Change2 the new clean jerk 2 change 2
	 */
	public void setCleanJerk2Change2(String cleanJerk2Change2) {
		if ("0".equals(cleanJerk2Change2)) {
			this.cleanJerk2Change2 = cleanJerk2Change2;
			getLogger().info("{}{} cleanJerk2Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk2Change2);
			setCleanJerk2ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateCleanJerk2Change2(cleanJerk2Change2);
		}
		this.cleanJerk2Change2 = cleanJerk2Change2;
		getLogger().info("{}{} cleanJerk2Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk2Change2);
	}

	/**
	 * Sets the clean jerk 2 declaration.
	 *
	 * @param cleanJerk2Declaration the new clean jerk 2 declaration
	 */
	public void setCleanJerk2Declaration(String cleanJerk2Declaration) {
		if ("0".equals(cleanJerk2Declaration)) {
			this.cleanJerk2Declaration = cleanJerk2Declaration;
			getLogger().info("{}{} cleanJerk2Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk2Declaration);
			setCleanJerk2ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateCleanJerk2Declaration(cleanJerk2Declaration);
		}
		this.cleanJerk2Declaration = cleanJerk2Declaration;
		getLogger().info("{}{} cleanJerk2Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk2Declaration);
	}

	public void setCleanJerk2LiftTime(LocalDateTime cleanJerk2LiftTime) {
		this.cleanJerk2LiftTime = cleanJerk2LiftTime;
	}

	/**
	 * Sets the clean jerk 3 actual lift.
	 *
	 * @param cleanJerk3ActualLift the new clean jerk 3 actual lift
	 */
	public void setCleanJerk3ActualLift(String cleanJerk3ActualLift) {
		if (isValidation()) {
			validateCleanJerk3ActualLift(cleanJerk3ActualLift);
		}
		this.cleanJerk3ActualLift = cleanJerk3ActualLift;
		getLogger().info("{}{} cleanJerk3ActualLift={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk3ActualLift);
	}

	/**
	 * Sets the clean jerk 3 automatic progression.
	 *
	 * @param s the new clean jerk 3 automatic progression
	 */
	public void setCleanJerk3AutomaticProgression(String s) {
	}

	/**
	 * Sets the clean jerk 3 change 1.
	 *
	 * @param cleanJerk3Change1 the new clean jerk 3 change 1
	 */
	public void setCleanJerk3Change1(String cleanJerk3Change1) {
		if ("0".equals(cleanJerk3Change1)) {
			this.cleanJerk3Change1 = cleanJerk3Change1;
			getLogger().info("{}{} cleanJerk3Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk3Change1);
			setCleanJerk3ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateCleanJerk3Change1(cleanJerk3Change1);
		}
		this.cleanJerk3Change1 = cleanJerk3Change1;
		getLogger().info("{}{} cleanJerk3Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk3Change1);
	}

	/**
	 * Sets the clean jerk 3 change 2.
	 *
	 * @param cleanJerk3Change2 the new clean jerk 3 change 2
	 */
	public void setCleanJerk3Change2(String cleanJerk3Change2) {
		if ("0".equals(cleanJerk3Change2)) {
			this.cleanJerk3Change2 = cleanJerk3Change2;
			getLogger().info("{}{} cleanJerk3Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk3Change2);
			setCleanJerk3ActualLift("0");
			return;
		}

		if (isValidation()) {
			validateCleanJerk3Change2(cleanJerk3Change2);
		}
		this.cleanJerk3Change2 = cleanJerk3Change2;
		getLogger().info("{}{} cleanJerk3Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk3Change2);
	}

	/**
	 * Sets the clean jerk 3 declaration.
	 *
	 * @param cleanJerk3Declaration the new clean jerk 3 declaration
	 */
	public void setCleanJerk3Declaration(String cleanJerk3Declaration) {
		if ("0".equals(cleanJerk3Declaration)) {
			this.cleanJerk3Declaration = cleanJerk3Declaration;
			getLogger().info("{}{} cleanJerk3Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        cleanJerk3Declaration);
			setCleanJerk3ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateCleanJerk3Declaration(cleanJerk3Declaration);
		}
		this.cleanJerk3Declaration = cleanJerk3Declaration;
		getLogger().info("{}{} cleanJerk3Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        cleanJerk3Declaration);
	}

	public void setCleanJerk3LiftTime(LocalDateTime cleanJerk3LiftTime) {
		this.cleanJerk3LiftTime = cleanJerk3LiftTime;
	}

	/**
	 * Sets the clean jerk attempts done.
	 *
	 * @param i the new clean jerk attempts done
	 */
	public void setCleanJerkAttemptsDone(Integer i) {
	}

	/**
	 * Sets the clean jerk points.
	 *
	 * @param points the new clean jerk points
	 */
	@Transient
	@JsonIgnore
	public void setCleanJerkPoints(Integer points) {
		// ignored. computed property. setter needed for beans introspection.
	}

	@Transient
	@JsonIgnore
	public void setCleanJerkRank(int ignored) {
		// ignored. computed property. setter needed for beans introspection.
	}

	/**
	 * Sets the club.
	 *
	 * @param club the club to set
	 */
	public void setClub(String club) {
		setTeam(club);
	}

	/**
	 * @param coach the coach to set
	 */
	public void setCoach(String coach) {
		this.coach = coach;
	}

	public void setCombinedRank(int i) {
		this.combinedRank = i;
	}

	public void setCustom1(String v) {
		this.custom1 = v;
	}

	public void setCustom2(String v) {
		this.custom2 = v;
	}

	/**
	 * Sets the custom points.
	 *
	 * @param customPoints the new custom points
	 */
	@Transient
	@JsonIgnore
	public void setCustomPoints(Integer customPoints) {
		// ignored. computed property. setter needed for beans introspection.
	}

	/*
	 * General event framework: we implement the com.vaadin.event.MethodEventSource interface which defines how a
	 * notifier can call a method on a listener to signal that an event has occurred, and how the listener can
	 * register/unregister itself.
	 */

	/**
	 * Sets the custom rank.
	 *
	 * @param customRank the new custom rank
	 */
	@Transient
	@JsonIgnore
	public void setCustomRank(Integer customRank) {
		// ignored. computed property. setter needed for beans introspection
	}

	/**
	 * Sets the custom score.
	 *
	 * @param customScore the new custom score
	 */
	public void setCustomScore(Double customScore) {
		this.customScore = customScore;
	}

	public void setEligibleCategories(Set<Category> newEligibles) {

		List<Participation> participations2 = getParticipations();

		Set<String> membershipCategories = participations2.stream().filter(p -> p.getTeamMember())
		        .map(p -> p.getCategory().getCode()).collect(Collectors.toSet());

		Set<Category> oldEligibles = getEligibleCategories();
		if (oldEligibles != null) {
			for (Category cat : oldEligibles) {
				removeEligibleCategory(cat);
			}
		}
		if (newEligibles != null) {
			for (Category cat : newEligibles) {
				boolean membership = membershipCategories.contains(cat.getCode());
				addEligibleCategory(cat, membership); // creates new join table entry, links from category as well.
			}
		}
	}

	public void setEligibleForIndividualRanking(boolean eligibleForIndividualRanking) {
		this.eligibleForIndividualRanking = eligibleForIndividualRanking;
	}

	public void setEligibleForTeamRanking(boolean eligibleForTeamRanking) {
		this.eligibleForTeamRanking = eligibleForTeamRanking;
	}

	public void setEntryTotal(Integer entryTotal) {
		// intentional, legacy name in database
		setQualifyingTotal(entryTotal);
	}

	public void setFederationCodes(String federationCodes) {
		this.federationCodes = federationCodes;
	}

	/**
	 * Sets the first name.
	 *
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setFop(FieldOfPlay fop) {
		// logger.debug("++++++ setting fop {} for {}", fop, this.getShortName());
		this.fop = fop;
	}

	/**
	 * Sets the forced as current.
	 *
	 * @param forcedAsCurrent the new forced as current
	 */
	public void setForcedAsCurrent(boolean forcedAsCurrent) {
		// logger.trace("setForcedAsCurrent({}) from {}", forcedAsCurrent,
		// LoggerUtils.whereFrom());
		this.forcedAsCurrent = forcedAsCurrent;
	}

	/**
	 * Sets the full birth date.
	 *
	 * @param fullBirthDate the fullBirthDate to set
	 */
	public void setFullBirthDate(LocalDate fullBirthDate) {
		// logger.trace("setting {} {} {}",getShortName(), fullBirthDate,
		// LoggerUtils.whereFrom());
		this.fullBirthDate = fullBirthDate;
	}

	/**
	 * Sets the gender.
	 *
	 * @param string the gender to set
	 */
	public void setGender(Gender gender) {
		this.gender = gender;
	}

	/**
	 * Sets the competition session.
	 *
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * Sets the last name.
	 *
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * Sets the lift order rank.
	 *
	 * @param liftOrder the new lift order rank
	 */
	public void setLiftOrderRank(Integer liftOrder) {
		this.liftOrderRank = liftOrder;
	}

	public void setLoggerLevel(Level newLevel) {
		getLogger().setLevel(newLevel);
	}

	/**
	 * Sets the lot number.
	 *
	 * @param lotNumber the lotNumber to set
	 */
	public void setLotNumber(Integer lotNumber) {
		this.lotNumber = lotNumber;
	}

	/**
	 * Sets the membership.
	 *
	 * @param membership the new membership
	 */
	public void setMembership(String membership) {
		this.membership = membership;
	}

	/**
	 * Sets the next attempt requested weight.
	 *
	 * @param i the new next attempt requested weight
	 */
	public void setNextAttemptRequestedWeight(Integer i) {
	}

	public void setParticipations(List<Participation> participations) {
		this.participations = participations;
	}

	public void setPersonalBestCleanJerk(Integer personalBestCleanJerk) {
		this.personalBestCleanJerk = personalBestCleanJerk;
	}

	public void setPersonalBestSnatch(Integer personalBestSnatch) {
		this.personalBestSnatch = personalBestSnatch;
	}

	public void setPersonalBestTotal(Integer personalBestTotal) {
		this.personalBestTotal = personalBestTotal;
	}

	public void setPresumedBodyWeight(Double presumedBodyWeight) {
		this.presumedBodyWeight = presumedBodyWeight;
	}

	/**
	 * When adding/deleting categories without knowing the actual bodyweight, we need to keep the last bodyweight we
	 * were factually told about by a human (either by explicitly setting the category, or through a registration file)
	 *
	 * if cat 59kg is deleted, the presumed category will become 64kg, but the presumed bodyweight remains 59 -- the
	 * switch to 64 is not from factual information about the lifter, it is something we made up. If we reinstate 59,
	 * the lifter will be again assumed to be 59.
	 *
	 * @param category
	 */

	public void setPresumedCategory(Category category) {
		// this relies on the fact that Hibernate/JPA field accesses use reflection
		// and do NOT call setCategory (which would change the presumed body weight,
		// something
		// we do NOT want.
		this.category = category;
	}

	public void setqPointsRank(Integer qPointsRank) {
		this.qPointsRank = qPointsRank;
	}

	/**
	 * Sets the qualifying total.
	 *
	 * @param qualifyingTotal the new qualifying total
	 */
	public void setQualifyingTotal(Integer qualifyingTotal) {
		this.qualifyingTotal = qualifyingTotal;
	}

	/**
	 * Sets the category. There is no longer a registration category.
	 *
	 * @param registrationCategory the new registration category
	 */
	@Deprecated
	public void setRegistrationCategory(Category registrationCategory) {
		this.category = registrationCategory;
	}

	/**
	 * Sets the robi rank.
	 *
	 * @param robiRank the new robi rank
	 */
	public void setRobiRank(Integer robiRank) {
		this.robiRank = robiRank;
	}

	public void setSessionPattern(String ignored) {

	}

	/**
	 * Sets the sinclair rank.
	 *
	 * @param sinclairRank the new sinclair rank
	 */
	public void setSinclairRank(Integer sinclairRank) {
		this.sinclairRank = sinclairRank;
	}

	public void setSmhfRank(int i) {
		this.smhfRank = i;
	}

	/**
	 * Sets the snatch 1 actual lift.
	 *
	 * @param snatch1ActualLift the new snatch 1 actual lift
	 */
	public void setSnatch1ActualLift(String snatch1ActualLift) {
		if (isValidation()) {
			validateSnatch1ActualLift(snatch1ActualLift);
		}
		this.snatch1ActualLift = snatch1ActualLift;
		getLogger().info("{}{} snatch1ActualLift={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch1ActualLift);
		// if (zeroIfInvalid(snatch1ActualLift) == 0) {
		// this.setSnatch1LiftTime(null);
		// } else {
		// this.setSnatch1LiftTime(LocalDateTime.now());
		// }
	}

	/**
	 * Sets the snatch 1 automatic progression.
	 *
	 * @param s the new snatch 1 automatic progression
	 */
	public void setSnatch1AutomaticProgression(String s) {
	}

	/**
	 * Sets the snatch 1 change 1.
	 *
	 * @param snatch1Change1 the new snatch 1 change 1
	 */
	public void setSnatch1Change1(String snatch1Change1) {
		if ("0".equals(snatch1Change1)) {
			this.snatch1Change1 = snatch1Change1;
			getLogger().info("{}{} snatch1Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch1Change1);
			setSnatch1ActualLift("0");
			return;
		}
		getLogger().info("{}{} snatch1Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch1Change1);
		if (isValidation()) {
			validateSnatch1Change1(snatch1Change1);
		}
		this.snatch1Change1 = snatch1Change1;
		// validateStartingTotalsRule();
	}

	/**
	 * Sets the snatch 1 change 2.
	 *
	 * @param snatch1Change2 the new snatch 1 change 2
	 */
	public void setSnatch1Change2(String snatch1Change2) {
		if ("0".equals(snatch1Change2)) {
			this.snatch1Change2 = snatch1Change2;
			getLogger().info("{}{} snatch1Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch1Change2);
			setSnatch1ActualLift("0");
			return;
		}
		getLogger().info("{}{} snatch1Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch1Change2);
		if (isValidation()) {
			try {
				validateSnatch1Change2(snatch1Change2);
			} catch (Exception e) {
				throw e;
			}
		}
		this.snatch1Change2 = snatch1Change2;

	}

	/**
	 * Sets the snatch 1 declaration.
	 *
	 * @param snatch1Declaration the new snatch 1 declaration
	 */
	public void setSnatch1Declaration(String snatch1Declaration) {
		if (isValidation()) {
			validateSnatch1Declaration(snatch1Declaration);
		}
		this.snatch1Declaration = snatch1Declaration;
		getLogger().info("{}{} snatch1Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch1Declaration);
	}

	public void setSnatch1LiftTime(LocalDateTime snatch1LiftTime) {
		this.snatch1LiftTime = snatch1LiftTime;
	}

	/**
	 * Sets the snatch 2 actual lift.
	 *
	 * @param snatch2ActualLift the new snatch 2 actual lift
	 */
	public void setSnatch2ActualLift(String snatch2ActualLift) {
		if (isValidation()) {
			validateSnatch2ActualLift(snatch2ActualLift);
		}
		this.snatch2ActualLift = snatch2ActualLift;
		getLogger().info("{}{} snatch2ActualLift={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch2ActualLift);
	}

	/**
	 * Sets the snatch 2 automatic progression.
	 *
	 * @param s the new snatch 2 automatic progression
	 */
	public void setSnatch2AutomaticProgression(String s) {
	}

	/**
	 * Sets the snatch 2 change 1.
	 *
	 * @param snatch2Change1 the new snatch 2 change 1
	 */
	public void setSnatch2Change1(String snatch2Change1) {
		if ("0".equals(snatch2Change1)) {
			this.snatch2Change1 = snatch2Change1;
			getLogger().info("{}{} snatch2Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch2Change1);
			setSnatch2ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateSnatch2Change1(snatch2Change1);
		}
		this.snatch2Change1 = snatch2Change1;
		getLogger().info("{}{} snatch2Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch2Change1);
	}

	/**
	 * Sets the snatch 2 change 2.
	 *
	 * @param snatch2Change2 the new snatch 2 change 2
	 */
	public void setSnatch2Change2(String snatch2Change2) {
		if ("0".equals(snatch2Change2)) {
			this.snatch2Change2 = snatch2Change2;
			getLogger().info("{}{} snatch2Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch2Change2);
			setSnatch2ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateSnatch2Change2(snatch2Change2);
		}
		this.snatch2Change2 = snatch2Change2;
		getLogger().info("{}{} snatch2Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch2Change2);
	}

	/**
	 * Sets the snatch 2 declaration.
	 *
	 * @param snatch2Declaration the new snatch 2 declaration
	 */
	public void setSnatch2Declaration(String snatch2Declaration) {
		if ("0".equals(snatch2Declaration)) {
			this.snatch2Declaration = snatch2Declaration;
			getLogger().info("{}{} snatch2Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch2Declaration);
			setSnatch2ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateSnatch2Declaration(snatch2Declaration);
		}
		this.snatch2Declaration = snatch2Declaration;
		getLogger().info("{}{} snatch2Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch2Declaration);
	}

	public void setSnatch2LiftTime(LocalDateTime snatch2LiftTime) {
		this.snatch2LiftTime = snatch2LiftTime;
	}

	/**
	 * Sets the snatch 3 actual lift.
	 *
	 * @param snatch3ActualLift the new snatch 3 actual lift
	 */
	public void setSnatch3ActualLift(String snatch3ActualLift) {
		if (isValidation()) {
			validateSnatch3ActualLift(snatch3ActualLift);
		}
		this.snatch3ActualLift = snatch3ActualLift;
		getLogger().info("{}{} snatch3ActualLift={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch3ActualLift);
	}

	/**
	 * Sets the snatch 3 automatic progression.
	 *
	 * @param s the new snatch 3 automatic progression
	 */
	public void setSnatch3AutomaticProgression(String s) {
	}

	/**
	 * Sets the snatch 3 change 1.
	 *
	 * @param snatch3Change1 the new snatch 3 change 1
	 */
	public void setSnatch3Change1(String snatch3Change1) {
		if ("0".equals(snatch3Change1)) {
			this.snatch3Change1 = snatch3Change1;
			getLogger().info("{}{} snatch3Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch3Change1);
			setSnatch3ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateSnatch3Change1(snatch3Change1);
		}
		this.snatch3Change1 = snatch3Change1;
		getLogger().info("{}{} snatch3Change1={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch3Change1);
	}

	/**
	 * Sets the snatch 3 change 2.
	 *
	 * @param snatch3Change2 the new snatch 3 change 2
	 */
	public void setSnatch3Change2(String snatch3Change2) {
		if ("0".equals(snatch3Change2)) {
			this.snatch3Change2 = snatch3Change2;
			getLogger().info("{}{} snatch3Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch3Change2);
			setSnatch3ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateSnatch3Change2(snatch3Change2);
		}
		this.snatch3Change2 = snatch3Change2;
		getLogger().info("{}{} snatch3Change2={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch3Change2);
	}

	/**
	 * Sets the snatch 3 declaration.
	 *
	 * @param snatch3Declaration the new snatch 3 declaration
	 */
	public void setSnatch3Declaration(String snatch3Declaration) {
		if ("0".equals(snatch3Declaration)) {
			this.snatch3Declaration = snatch3Declaration;
			getLogger().info("{}{} snatch3Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        snatch3Declaration);
			setSnatch3ActualLift("0");
			return;
		}
		if (isValidation()) {
			validateSnatch3Declaration(snatch3Declaration);
		}
		this.snatch3Declaration = snatch3Declaration;
		getLogger().info("{}{} snatch3Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
		        snatch3Declaration);
	}

	public void setSnatch3LiftTime(LocalDateTime snatch3LiftTime) {
		this.snatch3LiftTime = snatch3LiftTime;
	}

	/**
	 * Sets the snatch attempts done.
	 *
	 * @param i the new snatch attempts done
	 */
	public void setSnatchAttemptsDone(Integer i) {
	}

	/**
	 * Sets the snatch points.
	 *
	 * @param snatchPoints the new snatch points
	 */
	@Transient
	@JsonIgnore
	public void setSnatchPoints(Integer snatchPoints) {
		// ignored. computed property. setter needed for beans introspection.
	}

	@Transient
	@JsonIgnore
	public void setSnatchRank(int ignored) {
		// ignored. computed property. setter needed for beans introspection.
	}

	public void setStartingTotalViolation(boolean startingTotalViolation) {
		this.startingTotalViolation = startingTotalViolation;
	}

	/**
	 * Sets the start number.
	 *
	 * @param startNumber the new start number
	 */
	public void setStartNumber(Integer startNumber) {
		this.startNumber = startNumber;
	}

	/**
	 * @param s = A/B/C/D group -- we don't use the word group because of confusion with session.
	 */
	public void setSubCategory(String s) {
		this.subCategory = s;
	}

	/**
	 * Sets the team.
	 *
	 * @param club the new team
	 */
	public void setTeam(String club) {
		this.team = club;
	}

	/**
	 * Sets the team clean jerk rank.
	 *
	 * @param teamCJRank the new team clean jerk rank
	 */
	public void setTeamCleanJerkRank(Integer teamCJRank) {
		this.teamCleanJerkRank = teamCJRank;
	}

	/**
	 * Sets the team combined rank.
	 *
	 * @param teamCombinedRank the new team combined rank
	 */
	public void setTeamCombinedRank(Integer teamCombinedRank) {
		this.teamCombinedRank = teamCombinedRank;
	}

	/**
	 * Sets the team total rank.
	 *
	 * @param teamTotalRank the new team total rank
	 */
	public void setTeamCustomRank(Integer teamCustomRank) {
		this.teamCustomRank = teamCustomRank;
	}

	public void setTeamMember(boolean member) {
		throw new UnsupportedOperationException("Team Membership should be updated via PAthlete");
	}

	/**
	 * Sets the team robi rank.
	 *
	 * @param teamRobiRank the new team robi rank
	 */
	public void setTeamRobiRank(Integer teamRobiRank) {
		this.teamRobiRank = teamRobiRank;
	}

	/**
	 * Sets the team sinclair rank.
	 *
	 * @param teamSinclairRank the new team sinclair rank
	 */
	public void setTeamSinclairRank(Integer teamSinclairRank) {
		this.teamSinclairRank = teamSinclairRank;
	}

	/**
	 * Sets the team snatch rank.
	 *
	 * @param teamSnatchRank the new team snatch rank
	 */
	public void setTeamSnatchRank(Integer teamSnatchRank) {
		this.teamSnatchRank = teamSnatchRank;
	}

	/**
	 * Sets the team total rank.
	 *
	 * @param teamTotalRank the new team total rank
	 */
	public void setTeamTotalRank(Integer teamTotalRank) {
		this.teamTotalRank = teamTotalRank;
	}

	/**
	 * Sets the total.
	 *
	 * @param i the new total
	 */
	public void setTotal(Integer i) {
	}

	/**
	 * Sets the total points.
	 *
	 * @param totalPoints the new total points
	 */
	@Transient
	@JsonIgnore
	public void setTotalPoints(Integer totalPoints) {
		// ignored. computed property. setter needed for beans introspection.
	}

	@Transient
	@JsonIgnore
	public void setTotalRank(int ignored) {
		// ignored. computed property. setter needed for beans introspection.
	}

	public void setValidation(boolean validation) {
		this.validation = validation;
	}

	/**
	 * Sets the year of birth.
	 *
	 * @param birthYear the new year of birth
	 */
	@Transient
	@JsonIgnore
	public void setYearOfBirth(Integer birthYear) {
		setFullBirthDateFromYear(birthYear);
	}

	public int startingTotalDelta() {
		int sn1Decl = zeroIfInvalid(this.snatch1Declaration);
		int cj1Decl = zeroIfInvalid(this.cleanJerk1Declaration);
		getLogger().trace("prior to checking {} {}", sn1Decl, cj1Decl);
		if (sn1Decl == 0 && cj1Decl == 0) {
			return 0; // do not complain on registration form or empty weigh-in form.
		}
		Integer snatch1Request = last(sn1Decl, zeroIfInvalid(this.snatch1Change1), zeroIfInvalid(this.snatch1Change2));
		Integer cleanJerk1Request = last(cj1Decl, zeroIfInvalid(this.cleanJerk1Change1),
		        zeroIfInvalid(this.cleanJerk1Change2));
		return startingTotalDelta(snatch1Request, cleanJerk1Request, getEntryTotal());
	}

	/**
	 * Successful lift.
	 */
	public void successfulLift() {
		try {
			getLogger().info("{}good lift for {}", OwlcmsSession.getFopLoggingName(), this.getShortName());
			final String weight = Integer.toString(getNextAttemptRequestedWeight());
			doLift(weight);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage());
		}
	}

	public String toShortString() {
		Integer startNumber2 = getStartNumber();
		String prefix = getGroup() + "." + (startNumber2 != null ? "[" + startNumber2.toString() + "]" : "");
		if (getLastName() != null) {
			return prefix + "_" + getLastName() + "_" + getFirstName();
		} else {
			return prefix;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Integer startNumber2 = getStartNumber();
		String prefix = getGroup() + "." + (startNumber2 != null ? startNumber2.toString() : "");
		String suffix = "_id" + getId() + "_" + System.identityHashCode(this);
		if (getLastName() != null) {
			return prefix + "_" + getLastName() + "_" + getFirstName() + suffix;
		} else {
			return prefix + suffix;
		}
	}

	public String toStringRanks() {
		return getSnatchRank() + " " + getCleanJerkRank() + " " + getTotalRank();
	}

	/**
	 * @param curLift
	 * @param actualLift
	 */
	public void validateActualLift(int curLift, String automaticProgression, String declaration, String change1,
	        String change2, String actualLift) {
		if (actualLift == null || actualLift.trim().length() == 0 || !isValidation()) {
			return;
		}
		int lastChange = last(zeroIfInvalid(automaticProgression), zeroIfInvalid(declaration),
		        zeroIfInvalid(change1), zeroIfInvalid(change2));
		final int iAutomaticProgression = zeroIfInvalid(automaticProgression);
		final int liftedWeight = zeroIfInvalid(actualLift);

		// getLogger().trace(
		// "declaredChanges={} automaticProgression={} declaration={} change1={} change2={} liftedWeight={}",
		// lastChange, automaticProgression, declaration, change1, change2, liftedWeight);
		if (liftedWeight == 0) {
			// Athlete is not taking try; always ok no matter what was declared.
			return;
		}
		// allow empty declaration (declaration == automatic progression).
		// if (validation) validateDeclaration(curLift, automaticProgression,
		// declaration, change1, change2, actualLift);
		final boolean lastChangeTooLow = lastChange >= iAutomaticProgression;
		final boolean liftedWeightOk = Math.abs(liftedWeight) == lastChange;
		if (liftedWeightOk && lastChangeTooLow) {
		} else {
			if (!lastChangeTooLow) {
				throw new RuleViolationException.LastChangeTooLow(this, curLift, lastChange, iAutomaticProgression);
			}
			if (!liftedWeightOk) {
				throw new RuleViolationException.LiftValueNotWhatWasRequested(this, curLift, actualLift, lastChange,
				        liftedWeight);
			}
		}
	}

	public boolean validateCleanJerk1ActualLift(String cleanJerk1ActualLift) throws RuleViolationException {
		validateActualLift(3, getCleanJerk1AutomaticProgression(), this.cleanJerk1Declaration, this.cleanJerk1Change1,
		        this.cleanJerk1Change2, cleanJerk1ActualLift);
		return true;
	}

	public boolean validateCleanJerk1Change1(String cleanJerk1Change1) throws RuleViolationException {
		validateChange1(3, getCleanJerk1AutomaticProgression(), this.cleanJerk1Declaration, cleanJerk1Change1,
		        this.cleanJerk1Change2, this.cleanJerk1ActualLift, false);
		// validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
		// cleanJerk1Change1, cleanJerk1Change2);
		return true;
	}

	/**
	 * @param cleanJerk1Change2
	 * @return
	 * @throws RuleViolationException
	 */
	public boolean validateCleanJerk1Change2(String cleanJerk1Change2) throws RuleViolationException {
		validateChange2(3, getCleanJerk1AutomaticProgression(), this.cleanJerk1Declaration, this.cleanJerk1Change1,
		        cleanJerk1Change2, this.cleanJerk1ActualLift, false);
		// validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
		// cleanJerk1Change1, cleanJerk1Change2);
		return true;
	}

	public boolean validateCleanJerk1Declaration(String cleanJerk1Declaration) throws RuleViolationException {
		// not always true. Can violate moving down rules
		validateDeclaration(3, "0", cleanJerk1Declaration, this.cleanJerk1Change1,
		        this.cleanJerk1Change1, this.cleanJerk2ActualLift);
		return true;
	}

	public boolean validateCleanJerk2ActualLift(String cleanJerk2ActualLift) throws RuleViolationException {
		validateActualLift(4, getCleanJerk2AutomaticProgression(), this.cleanJerk2Declaration, this.cleanJerk2Change1,
		        this.cleanJerk2Change2, cleanJerk2ActualLift);
		return true;
	}

	public boolean validateCleanJerk2Change1(String cleanJerk2Change1) throws RuleViolationException {
		validateChange1(4, getCleanJerk2AutomaticProgression(), this.cleanJerk2Declaration, cleanJerk2Change1,
		        this.cleanJerk2Change2, this.cleanJerk2ActualLift, false);
		return true;
	}

	public boolean validateCleanJerk2Change2(String cleanJerk2Change2) throws RuleViolationException {
		validateChange2(4, getCleanJerk2AutomaticProgression(), this.cleanJerk2Declaration, this.cleanJerk2Change1,
		        cleanJerk2Change2, this.cleanJerk2ActualLift, false);
		return true;
	}

	public boolean validateCleanJerk2Declaration(String cleanJerk2Declaration) throws RuleViolationException {
		validateDeclaration(4, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, this.cleanJerk2Change1,
		        this.cleanJerk2Change2, this.cleanJerk2ActualLift);
		return true;
	}

	public boolean validateCleanJerk3ActualLift(String cleanJerk3ActualLift) throws RuleViolationException {
		validateActualLift(5, getCleanJerk3AutomaticProgression(), this.cleanJerk3Declaration, this.cleanJerk3Change1,
		        this.cleanJerk3Change2, cleanJerk3ActualLift);
		// throws exception if invalid
		return true;
	}

	public boolean validateCleanJerk3Change1(String cleanJerk3Change1) throws RuleViolationException {
		validateChange1(5, getCleanJerk3AutomaticProgression(), this.cleanJerk3Declaration, cleanJerk3Change1,
		        this.cleanJerk3Change2, this.cleanJerk3ActualLift, false);
		return true;
	}

	public boolean validateCleanJerk3Change2(String cleanJerk3Change2) throws RuleViolationException {
		validateChange2(5, getCleanJerk3AutomaticProgression(), this.cleanJerk3Declaration, this.cleanJerk3Change1,
		        cleanJerk3Change2, this.cleanJerk3ActualLift, false);
		return true;
	}

	public boolean validateCleanJerk3Declaration(String cleanJerk3Declaration) throws RuleViolationException {
		validateDeclaration(5, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, this.cleanJerk3Change1,
		        this.cleanJerk3Change2, this.cleanJerk3ActualLift);
		return true;
	}

	public boolean validateSnatch1ActualLift(String snatch1ActualLift) throws RuleViolationException {
		validateActualLift(0, getSnatch1AutomaticProgression(), this.snatch1Declaration, this.snatch1Change1,
		        this.snatch1Change2,
		        snatch1ActualLift);
		return true;
	}

	public boolean validateSnatch1Change1(String snatch1Change1) throws RuleViolationException {
		validateChange1(0, getSnatch1AutomaticProgression(), this.snatch1Declaration, snatch1Change1,
		        this.snatch1Change2,
		        this.snatch1ActualLift, true);
		return true;
	}

	public boolean validateSnatch1Change2(String snatch1Change2) throws RuleViolationException {
		validateChange2(0, getSnatch1AutomaticProgression(), this.snatch1Declaration, this.snatch1Change1,
		        snatch1Change2,
		        this.snatch1ActualLift, true);
		return true;
	}

	public boolean validateSnatch1Declaration(String snatch1Declaration) throws RuleViolationException {
		// can violate movingdown rules if changed inadequately by marshal.
		validateDeclaration(0, "0", snatch1Declaration, this.snatch1Change1, this.snatch1Change2,
		        this.snatch1ActualLift);
		return true;
	}

	public boolean validateSnatch2ActualLift(String snatch2ActualLift) throws RuleViolationException {
		validateActualLift(1, getSnatch2AutomaticProgression(), this.snatch2Declaration, this.snatch2Change1,
		        this.snatch2Change2,
		        snatch2ActualLift);
		return true;
	}

	public boolean validateSnatch2Change1(String snatch2Change1) throws RuleViolationException {
		Level prevLevel = getLogger().getLevel();
		try {
			validateChange1(1, getSnatch2AutomaticProgression(), this.snatch2Declaration, snatch2Change1,
			        this.snatch2Change2,
			        this.snatch2ActualLift, true);
		} finally {
			getLogger().setLevel(prevLevel);
		}
		return true;
	}

	// @SuppressWarnings("unused")
	// private Long getCopyId() {
	// return copyId;
	// }

	// @SuppressWarnings("unused")
	// private Integer getDeclaredAndActuallyAttempted(Integer... items) {
	// int lastIndex = items.length - 1;
	// if (items.length == 0) {
	// return 0;
	// }
	// while (lastIndex >= 0) {
	// if (items[lastIndex] > 0) {
	// // if went down from declared weight, then return lower weight
	// return (items[lastIndex] < items[0] ? items[lastIndex] : items[0]);
	// }
	// lastIndex--;
	// }
	// return 0;
	// }

	public boolean validateSnatch2Change2(String snatch2Change2) throws RuleViolationException {
		validateChange2(1, getSnatch2AutomaticProgression(), this.snatch2Declaration, this.snatch2Change1,
		        snatch2Change2,
		        this.snatch2ActualLift, true);
		return true;
	}

	public boolean validateSnatch2Declaration(String snatch2Declaration) throws RuleViolationException {
		validateDeclaration(1, getSnatch2AutomaticProgression(), snatch2Declaration, this.snatch2Change1,
		        this.snatch2Change2,
		        this.snatch2ActualLift);
		return true;
	}

	public boolean validateSnatch3ActualLift(String snatch3ActualLift) throws RuleViolationException {
		validateActualLift(2, getSnatch3AutomaticProgression(), this.snatch3Declaration, this.snatch3Change1,
		        this.snatch3Change2,
		        snatch3ActualLift);
		return true;
	}

	// /**
	// * Null-safe comparison for longs.
	// *
	// * @param o1
	// * @param o2
	// * @return
	// */
	// private boolean LongEquals(Long o1, Long o2) {
	// return o1 == o2 || o1 != null && o2 != null && o1.longValue() == (o2.longValue());
	// }

	public boolean validateSnatch3Change1(String snatch3Change1) throws RuleViolationException {
		validateChange1(2, getSnatch3AutomaticProgression(), this.snatch3Declaration, snatch3Change1,
		        this.snatch3Change2,
		        this.snatch3ActualLift, true);
		return true;
	}

	public boolean validateSnatch3Change2(String snatch3Change2) throws RuleViolationException {
		validateChange2(2, getSnatch3AutomaticProgression(), this.snatch3Declaration, this.snatch3Change1,
		        snatch3Change2,
		        this.snatch3ActualLift, true);
		return true;
	}

	public boolean validateSnatch3Declaration(String snatch3Declaration) throws RuleViolationException {
		validateDeclaration(2, getSnatch3AutomaticProgression(), snatch3Declaration, this.snatch3Change1,
		        this.snatch3Change2,
		        this.snatch3ActualLift);
		return true;
	}

	public boolean validateStartingTotalsRule() throws RuleViolationException {
		try {
			if (!isValidation()) {
				return true;
			}
			boolean b = validateStartingTotalsRule(
			        this.snatch1Declaration,
			        this.snatch1Change1,
			        this.snatch1Change2,
			        this.cleanJerk1Declaration,
			        this.cleanJerk1Change1,
			        this.cleanJerk1Change2);
			return b;
		} catch (RuleViolationException e) {
			throw e;
		}
	}

	/**
	 * @param snatchDeclaration
	 * @param cleanJerkDeclaration
	 * @param entryTotal
	 * @return true if ok, exception if not
	 * @throws RuleViolationException if rule violated, exception contains details.
	 */
	public boolean validateStartingTotalsRule(Integer snatch1Request, Integer cleanJerk1Request,
	        int qualTotal) throws RuleViolationException.Rule15_20Violated {
		boolean enforce20kg = Competition.getCurrent().isEnforce20kgRule();
		// logger.debug("validateStartingTotalsRule {} {} {} {}", LoggerUtils.whereFrom(),
		// System.identityHashCode(this), isValidation(), this.isStartingTotalViolation());
		if (!enforce20kg || !isValidation()) {
			// don't complain again if exception already raised.
			return true;
		}
		int missing = startingTotalDelta(snatch1Request, cleanJerk1Request, qualTotal);
		if (missing > 0) {
			this.setStartingTotalViolation(true);
			// logger.debug("FAIL missing {}", missing);
			Integer startNumber2 = this.getStartNumber();
			throw new RuleViolationException.Rule15_20Violated(this, this.getLastName(),
			        this.getFirstName(),
			        (startNumber2 != null ? startNumber2.toString() : "-"),
			        snatch1Request, cleanJerk1Request, missing, qualTotal);
		} else {
			this.setStartingTotalViolation(true);
			// logger.debug("OK margin={}", -(missing));
			return true;
		}
	}

	/**
	 * @param entryTotal
	 * @return true if ok, exception if not
	 * @throws RuleViolationException if rule violated, exception contains details.
	 */
	public boolean validateStartingTotalsRule(String snatch1Declaration, String snatch1Change1, String snatch1Change2,
	        String cleanJerk1Declaration, String cleanJerk1Change1, String cleanJerk1Change2) {
		boolean enforce20kg = Competition.getCurrent().isEnforce20kgRule();
		int entryTotal = getEntryTotal();
		if (!isValidation() || !enforce20kg || (entryTotal == 0)) {
			return true;
		}
		// getLogger().debug("enforcing 20kg rule {} {} {}", LoggerUtils.whereFrom(), enforce20kg, entryTotal);
		int sn1Decl = zeroIfInvalid(snatch1Declaration);
		int cj1Decl = zeroIfInvalid(cleanJerk1Declaration);
		getLogger().trace("prior to checking {} {}", sn1Decl, cj1Decl);
		if (sn1Decl == 0 && cj1Decl == 0) {
			return true; // do not complain on registration form or empty weigh-in form.
		}

		Integer snatch1Request = last(sn1Decl, zeroIfInvalid(snatch1Change1), zeroIfInvalid(snatch1Change2));

		Integer cleanJerk1Request = last(cj1Decl, zeroIfInvalid(cleanJerk1Change1), zeroIfInvalid(cleanJerk1Change2));
		return validateStartingTotalsRule(snatch1Request, cleanJerk1Request, entryTotal);
	}

	/**
	 * Withdraw.
	 */
	public void withdraw() {
		if (this.snatch1ActualLift != null && this.snatch1ActualLift.trim().isEmpty()) {
			setSnatch1ActualLift("0");
			setSnatch1LiftTime(null);
		}
		if (this.snatch2ActualLift != null && this.snatch2ActualLift.trim().isEmpty()) {
			setSnatch2ActualLift("0");
			setSnatch2LiftTime(null);
		}
		if (this.snatch3ActualLift != null && this.snatch3ActualLift.trim().isEmpty()) {
			setSnatch3ActualLift("0");
			setSnatch3LiftTime(null);
		}
		if (this.cleanJerk1ActualLift != null && this.cleanJerk1ActualLift.trim().isEmpty()) {
			setCleanJerk1ActualLift("0");
			setCleanJerk1LiftTime(null);
		}
		if (this.cleanJerk2ActualLift != null && this.cleanJerk2ActualLift.trim().isEmpty()) {
			setCleanJerk2ActualLift("0");
			setCleanJerk2LiftTime(null);
		}
		if (this.cleanJerk3ActualLift != null && this.cleanJerk3ActualLift.trim().isEmpty()) {
			setCleanJerk3ActualLift("0");
			setCleanJerk3LiftTime(null);
		}
	}

	/**
	 * Withdraw.
	 */
	public void withdrawFromSnatch() {
		if (this.snatch1ActualLift != null && this.snatch1ActualLift.trim().isEmpty()) {
			setSnatch1ActualLift("0");
			setSnatch1LiftTime(null);
		}
		if (this.snatch2ActualLift != null && this.snatch2ActualLift.trim().isEmpty()) {
			setSnatch2ActualLift("0");
			setSnatch2LiftTime(null);
		}
		if (this.snatch3ActualLift != null && this.snatch3ActualLift.trim().isEmpty()) {
			setSnatch3ActualLift("0");
			setSnatch3LiftTime(null);
		}
	}

	/**
	 * As integer.
	 *
	 * @param stringValue the string value
	 * @return the integer
	 */
	protected Integer asInteger(String stringValue) {
		if (stringValue == null || stringValue.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(stringValue);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	private Category bestMatch(List<Category> allEligible2) {
		return allEligible2 != null ? (allEligible2.size() > 0 ? allEligible2.get(0) : null) : null;
	}

	private void checkAttemptVsLiftOrderReference(int curLift, int newVal, LiftOrderInfo reference) {
		long start = System.currentTimeMillis();
		Integer requestedWeight = newVal;
		int referenceWeight = reference.getWeight();
		int referenceAttemptNo_1 = reference.getAttemptNo();// this is the lift that was attempted by previous lifter

		int currentLiftNo_1 = getAttemptsDone() + 1;
		int checkedLift_1 = curLift + 1;
		if (checkedLift_1 < currentLiftNo_1) {
			// we are checking an earlier attempt of the athlete (e.g. when loading the
			// athlete card)
			logger.debug("ignoring lift {} {}", checkedLift_1, currentLiftNo_1);
			return;
		} else {
			logger.debug("checking lift {} {}", checkedLift_1, currentLiftNo_1);
		}

		logger.debug("referenceAttempt {} reference weight {} curLift {} currentLiftNo {}", referenceAttemptNo_1,
		        referenceWeight, curLift, currentLiftNo_1);
		// Careful: do not mix one-based numbers with zero-based numbers.
		if (referenceAttemptNo_1 <= 3 && currentLiftNo_1 == 4) {
			getLogger().info("{}start of CJ {}", OwlcmsSession.getFopLoggingName(), curLift);
			// first attempt for C&J, no check
			return;
		}

		if (requestedWeight > referenceWeight) {
			getLogger().trace("{}{} attempt {}: requested {} > previous {}", OwlcmsSession.getFopLoggingName(), this,
			        currentLiftNo_1,
			        requestedWeight,
			        referenceWeight);
			// lifting order is respected
			return;
		}

		if (requestedWeight < referenceWeight) {
			getLogger().trace("{}requestedWeight {} < referenceWeight {}", OwlcmsSession.getFopLoggingName(),
			        requestedWeight,
			        referenceWeight);
			// someone has already lifted heavier previously
			if (requestedWeight > 0) {
				throw new RuleViolationException.WeightBelowAlreadyLifted(this, requestedWeight,
				        reference.getAthlete(), referenceWeight, referenceAttemptNo_1);
			}
		} else {
			checkSameWeightAsReference(reference, requestedWeight, referenceWeight, referenceAttemptNo_1,
			        currentLiftNo_1);
		}
		this.timingLogger.info("    checkAttemptVsLiftOrderReference {}", System.currentTimeMillis() - start);
	}

	private boolean checkBlank(String s) {
		return s == null || s.isBlank();
	}

	/**
	 * Check that the change does not allow lifter to lift out of order
	 *
	 * Changing requested weight and moving back
	 *
	 * @param curLift
	 * @param newVal
	 */
	private void checkChangeVsLiftOrder(int curLift, int newVal) {
		long start = System.currentTimeMillis();
		Level prevLoggerLevel = getLogger().getLevel();
		if (Competition.getCurrent().isGenderOrder()) {
			return;
		}
		try {
			// getLogger().setLevel(Level.DEBUG);
			doCheckChangeVsLiftOrder(curLift, newVal);
		} finally {
			getLogger().setLevel(prevLoggerLevel);
		}
		this.timingLogger.info("    checkChangeVsLiftOrder {}ms {} {}", System.currentTimeMillis() - start, curLift,
		        LoggerUtils.whereFrom());
	}

	private void checkChangeVsTimer(int curLift, String declaration, String change1, String change2) {
		if (Competition.getCurrent().isGenderOrder()) {
			return;
		}
		Integer attemptsDone = this.getAttemptsDone(); // 0..5
		if (curLift != attemptsDone) {
			return;
		}

		// logger.debug(" checkChangeVsTimer {} {}", curLift, LoggerUtils.whereFrom());

		Object wi = OwlcmsSession.getAttribute("weighIn");
		String fopLoggingName = OwlcmsSession.getFopLoggingName();
		if (wi == this) {
			// current athlete being weighed in
			getLogger().debug("{}weighin {}", fopLoggingName, wi);
			return;
		} else {
			getLogger().debug("{}lifting", fopLoggingName);
		}

		Athlete owner = getFop().getClockOwner();
		int initialTime = getFop().getClockOwnerInitialTimeAllowed();
		int clock = getFop().getAthleteTimer().liveTimeRemaining();
		if (!this.isSameAthleteAs(owner)) {
			// clock is not running for us
			logger.debug("NOT owning clock");
			doCheckChangeNotOwningTimer(declaration, change1, change2, getFop(), clock, initialTime);
		} else {
			logger.debug("OWNING clock");
			doCheckChangeOwningTimer(declaration, change1, change2, getFop(), clock, initialTime);
		}

	}

	private void checkDeclarationWasMade(int curLift, String declaration) {
		long start = System.currentTimeMillis();
		if (curLift != this.getAttemptsDone()) {
			return;
		}
		int clock = getFop().getAthleteTimer().liveTimeRemaining();
		if (declaration == null || declaration.isBlank()) {
			// there was no declaration made in time
			logger./**/warn("{}{} change without declaration (not owning clock)", OwlcmsSession.getFopLoggingName(),
			        this.getShortName());
			throw new RuleViolationException.MustDeclareFirst(this, clock);
		}
		this.timingLogger.info("    checkDeclarationWasMade {}ms {} {}", System.currentTimeMillis() - start, curLift,
		        LoggerUtils.whereFrom());
	}

	private void checkSameProgression(LiftOrderInfo reference, Integer requestedWeight, int currentProgression,
	        int referenceProgression) {
		long start = System.currentTimeMillis();
		String fopLoggingName = OwlcmsSession.getFopLoggingName();
		getLogger().trace("{}currentProgression {} == referenceProgression {}", fopLoggingName, currentProgression,
		        referenceProgression);
		if (this.getStartNumber() > 0) {
			// same weight, same attempt, allowed if start number is greater than previous
			// lifter
			if (reference.getStartNumber() > this.getStartNumber()) {
				getLogger().trace("{}lastLift.getStartNumber() {} > this.getStartNumber() {}",
				        fopLoggingName, reference.getStartNumber(), this.getStartNumber());
				throw new RuleViolationException.StartNumberTooHigh(this, requestedWeight,
				        reference.getAthlete(), this);
			} else {
				getLogger().trace("{}lastLift.getStartNumber() {} <= this.getStartNumber() {}",
				        fopLoggingName, reference.getStartNumber(), this.getStartNumber());
			}
		} else {
			// no start number was attributed, try with lot number
			if (reference.getLotNumber() > this.getLotNumber()) {
				getLogger().trace("{}lastLift.getLotNumber() {} > this.getLotNumber() {}",
				        fopLoggingName, reference.getLotNumber(), this.getLotNumber());
				throw new RuleViolationException.LotNumberTooHigh(this, requestedWeight,
				        reference.getLotNumber(), this.getLotNumber());
			} else {
				getLogger().trace("{}lastLift.getLotNumber() {} <= this.getLotNumber() {}",
				        fopLoggingName, reference.getLotNumber(), this.getLotNumber());
			}
		}
		this.timingLogger.info("    checkSameProgression {}ms {}", System.currentTimeMillis() - start,
		        LoggerUtils.whereFrom());
	}

	private void checkSameWeightAsReference(LiftOrderInfo reference, Integer requestedWeight, int referenceWeight,
	        int referenceAttemptNo, int currentLiftNo) {
		long start = System.currentTimeMillis();
		String fopLoggingName = OwlcmsSession.getFopLoggingName();
		getLogger().trace("{}requestedWeight {} == referenceWeight {}",
		        fopLoggingName, requestedWeight, referenceWeight);
		// asking for same weight as previous lifter, cannot be a lower attempt number
		// Example we are asking for weight X on (say) first attempt, but someone else
		// already
		// lifted that weight on second attempt. Too late, we are out of order.
		if (currentLiftNo < referenceAttemptNo) {
			getLogger().trace("{}currentLiftNo {} < prevAttemptNo {}",
			        fopLoggingName, currentLiftNo, referenceAttemptNo);
			throw new RuleViolationException.AttemptNumberTooLow(this, requestedWeight,
			        reference.getAthlete(), referenceWeight, 1 + (referenceAttemptNo - 1) % 3);
		} else if (currentLiftNo == referenceAttemptNo) {
			getLogger().trace("{}currentLiftNo {} == referenceAttemptNo {}",
			        fopLoggingName, currentLiftNo, referenceAttemptNo);
			int currentProgression = this.getProgression(requestedWeight);

			// BEWARE: referenceProgression is for current reference athlete and their prior attempt.
			int referenceProgression = reference.getProgression();

			if (currentProgression == referenceProgression) {
				getLogger().warn("{}progression({}) {} == referenceProgression({}) {}",
				        fopLoggingName, this.getLastName(), currentProgression, reference.getAthlete().getLastName(),
				        referenceProgression);
				// look back to previous attempt if any to determine who actually lifted first.
				currentProgression = this.getCumulativeProgression(requestedWeight);
				referenceProgression = reference.getCumulativeProgression();
			}

			// check again.
			if (currentProgression == referenceProgression) {
				getLogger().warn("{}cumulativeProgression({}) {} == referenceCumulativeProgression({}) {}",
				        fopLoggingName, this.getLastName(), currentProgression, reference.getAthlete().getLastName(),
				        referenceProgression);
				checkSameProgression(reference, requestedWeight, currentProgression, referenceProgression);
			} else

			if (currentProgression > referenceProgression) {
				getLogger().warn("{}currentProgression({}) {} > referenceProgression({}) {}",
				        fopLoggingName, this.getLastName(), currentProgression, reference.getAthlete().getLastName(),
				        referenceProgression);
				// larger progression means a smaller previous attempt, hence lifted earlier
				// than the last lift.
				// so we should already have lifted.
				getLogger().trace("{}{} lifted previous attempt earlier than {}, should already have done attempt",
				        fopLoggingName, reference.getAthlete().getShortName(), this.getShortName());
				throw new RuleViolationException.LiftedEarlier(this, requestedWeight, reference.getAthlete(),
				        this);
			} else {
				getLogger().trace("{}currentProgression {} < referenceProgression {}", fopLoggingName,
				        currentProgression, referenceProgression);
			}
		} else {
			// ok because does not change lift order.
			getLogger().trace("{}currentLiftNo {} > referenceAttemptNo {}",
			        fopLoggingName, currentLiftNo, referenceAttemptNo);
		}
		this.timingLogger.info("    checkSameWeightAsReference {}ms", System.currentTimeMillis() - start);
	}

	/**
	 * @param prevVal
	 * @return
	 */
	private String doAutomaticProgression(final int prevVal) {
		if (prevVal > 0) {
			return Integer.toString(prevVal + 1);
		} else {
			return Integer.toString(Math.abs(prevVal));
		}
	}

	private void doCheckChangeNotOwningTimer(String declaration, String change1, String change2, FieldOfPlay fop,
	        int clock, int initialTime) {
		if ((declaration != null && !declaration.isBlank()) && (change1 == null || change1.isBlank())
		        && (change2 == null || change2.isBlank())) {
			logger.trace("{}{} declaration accepted (not owning clock)", OwlcmsSession.getFopLoggingName(),
			        this.getShortName());
		} else {
			logger.trace("{}{} change accepted (not owning clock)", OwlcmsSession.getFopLoggingName(),
			        this.getShortName());
		}
	}

	private void doCheckChangeOwningTimer(String declaration, String change1, String change2, FieldOfPlay fop,
	        int clock, int initialTime) {
		// logger.debug("{}doCheckChangeOwningTimer ===== initialTime={} clock={} {} {} {}\n{}",
		// FieldOfPlay.getLoggingName(fop), initialTime, clock, declaration, change1, change2,
		// LoggerUtils.stackTrace());
		if ((change1 == null || change1.isBlank()) && (change2 == null || change2.isBlank())) {
			// validate declaration
			if (clock < initialTime - 30000) {
				logger./**/warn("{}{} late declaration denied ({})", OwlcmsSession.getFopLoggingName(),
				        this.getShortName(),
				        clock / 1000.0);
				throw new RuleViolationException.LateDeclaration(this, clock);
			}
			logger.debug("{}{}valid declaration", OwlcmsSession.getFopLoggingName(), this.getShortName(),
			        clock / 1000.0);
		} else {
			if (clock < 30000) {
				logger./**/warn("{}{} late change denied after final warning ({})", OwlcmsSession.getFopLoggingName(),
				        this.getShortName(), clock / 1000.0);
				throw new RuleViolationException.MustChangeBeforeFinalWarning(this, clock);
			}
			logger.debug("{}change before final warning", OwlcmsSession.getFopLoggingName(), clock);
		}
	}

	private void doCheckChangeVsLiftOrder(int curLift, int newVal) throws RuleViolationException {
		if (getFop() == null) {
			return;
		}
		int currentLiftNo = getAttemptsDone() + 1; // check
		int checkedLift = curLift + 1;
		if (checkedLift < currentLiftNo) {
			// we are checking an earlier attempt of the athlete (e.g. when loading the
			// athlete card)
			// getLogger().debug("doCheckChangeVsLiftOrder ignoring lift {} {}", checkedLift, currentLiftNo);
			return;
		} else {
			// getLogger().debug("doCheckChangeVsLiftOrder checking lift {} {} from {}", checkedLift, currentLiftNo,
			// LoggerUtils.whereFrom(1));
		}

		if (currentLiftNo <= 3 && checkedLift > 3) {
			// ignore CJ while doing snatch
			// getLogger().debug("doCheckChangeVsLiftOrder ignoring cj lift {} {}", checkedLift, currentLiftNo);
			return;
		}

		Object wi = OwlcmsSession.getAttribute("weighIn");
		String fopLoggingName = OwlcmsSession.getFopLoggingName();
		if (wi == this) {
			// athlete being weighed in
			getLogger().trace("{}weighin {}", fopLoggingName, wi);
			return;
		} else {
			getLogger().trace("{}lifting", fopLoggingName);
		}
		Integer weightAtLastStart = getFop().getWeightAtLastStart();
		if (getFop().getState() == FOPState.INACTIVE) {
			weightAtLastStart = null;
		}
		if (weightAtLastStart == null || weightAtLastStart == 0 || newVal == weightAtLastStart) {
			getLogger().trace("{}weight at last start: {} request = {}", fopLoggingName, weightAtLastStart, newVal);
			// program has just been started, or first athlete in group, or moving down to
			// clock value
			// compare with what the lifting order rules say.
			LiftOrderReconstruction pastOrder = new LiftOrderReconstruction(getFop());
			LiftOrderInfo reference = null;

			Athlete clockOwner = getFop().getClockOwner();
			if (clockOwner != null) {
				// if clock is running, reference becomes the clock owner instead of last
				// good/bad lift.
				reference = clockOwner.getRunningLiftOrderInfo();
				pastOrder.shortDump("lastLift info clock running", getLogger());
			} else {
				reference = pastOrder.getLastLift();
				pastOrder.shortDump("lastLift info no clock", getLogger());
			}

			if (reference != null) {
				checkAttemptVsLiftOrderReference(curLift, newVal, reference);
			} else {
				// no last lift, go ahead
			}
		} else if (newVal > 0 && newVal < weightAtLastStart) {
			// check that we are comparing the value for the same lift
			boolean cjClock = getFop().getLiftsDoneAtLastStart() >= 3;
			boolean cjStarted = getAttemptsDone() >= 3;
			// logger.trace("newval {} weightAtLastStart {}", newVal, weightAtLastStart);
			// logger.trace("lifts done at last start {} current lifts done {}",
			// fop.getLiftsDoneAtLastStart(),
			// getAttemptsDone());
			if (!Competition.getCurrent().isRoundRobinOrder()
			        && ((!cjClock && !cjStarted) || (cjStarted && cjClock))) {
				throw new RuleViolationException.ValueBelowStartedClock(this, newVal, weightAtLastStart);
			}
		} else {
			// ok, nothing to do.
		}
	}

	private String emptyIfNull(String value) {
		return (value == null ? "" : value);
	}

	@Transient
	@JsonIgnore
	private String getActualLiftStringOrElseNull(int liftNo) {
		String value = null;
		switch (liftNo) {
			case 1:
				value = this.getSnatch1ActualLift();
				break;
			case 2:
				value = this.getSnatch2ActualLift();
				break;
			case 3:
				value = this.getSnatch3ActualLift();
				break;
			case 4:
				value = this.getCleanJerk1ActualLift();
				break;
			case 5:
				value = this.getCleanJerk2ActualLift();
				break;
			case 6:
				value = this.getCleanJerk3ActualLift();
				break;
			default:
				value = null;
				break;
		}
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	/**
	 * Gets the 2020 Sinclair for use in SMF for Delta
	 *
	 * @return a Sinclair value even if c&j has not started
	 */
	@Transient
	@JsonIgnore
	private Double getMastersSinclair() {
		final Double bodyWeight1 = getBodyWeight();
		if (bodyWeight1 == null) {
			return 0.0;
		}
		Integer bestCleanJerk = getBestCleanJerk();
		Integer bestSnatch = getBestSnatch();
		Integer total1 = bestCleanJerk + bestSnatch;
		if (bestCleanJerk == null || bestSnatch == null || total1 == null || total1 < 0.1 || (this.gender == null)) {
			return 0.0;
		}
		if (this.gender == Gender.M) { // $NON-NLS-1$
			return total1 * sinclairFactor(bodyWeight1, sinclairProperties2020.menCoefficient(),
			        sinclairProperties2020.menMaxWeight());
		} else if (this.gender == Gender.F) {
			return total1 * sinclairFactor(bodyWeight1, sinclairProperties2020.womenCoefficient(),
			        sinclairProperties2020.womenMaxWeight());
		} else {
			return 1.0;
		}
	}

	/**
	 * Gets the 2020 Sinclair for use in SMF for Delta
	 *
	 * @return a Sinclair value even if c&j has not started
	 */
	@Transient
	@JsonIgnore
	private Double getMastersSinclairForDelta() {
		final Double bodyWeight1 = getBodyWeight();
		if (bodyWeight1 == null) {
			return 0.0;
		}
		Integer total1 = getBestCleanJerk() + getBestSnatch();
		if (total1 == null || total1 < 0.1 || (this.gender == null)) {
			return 0.0;
		}
		if (this.gender == Gender.M) { // $NON-NLS-1$
			return total1 * sinclairFactor(bodyWeight1, sinclairProperties2020.menCoefficient(),
			        sinclairProperties2020.menMaxWeight());
		} else if (this.gender == Gender.F) {
			return total1 * sinclairFactor(bodyWeight1, sinclairProperties2020.womenCoefficient(),
			        sinclairProperties2020.womenMaxWeight());
		} else {
			return 1.0;
		}
	}

	@Transient
	@JsonIgnore
	public int getProgression(Integer requestedWeight) {
		int attempt = getAttemptsDone() + 1;
		return doGetProgression(requestedWeight, attempt);
	}

	@Transient
	@JsonIgnore
	public int getAttemptProgression(int attempt) {
		return doGetProgression(this.getRequestedWeightForAttempt(attempt), attempt);
	}

	private int doGetProgression(Integer requestedWeight, int attempt) {
		switch (attempt) {
			case 1:
				return 0;
			case 2:
				return Math.abs(requestedWeight) - Math.abs(zeroIfInvalid(getSnatch1ActualLift()));
			case 3:
				return Math.abs(requestedWeight) - Math.abs(zeroIfInvalid(getSnatch2ActualLift()));
			case 4:
				return 0;
			case 5:
				return Math.abs(requestedWeight) - Math.abs(zeroIfInvalid(getCleanJerk1ActualLift()));
			case 6:
				return Math.abs(requestedWeight) - Math.abs(zeroIfInvalid(getCleanJerk2ActualLift()));
		}
		return 0;
	}

	@Transient
	@JsonIgnore
	public int getCumulativeAttemptProgression(int attempt) {
		return doGetCumulativeProgression(this.getRequestedWeightForAttempt(attempt), attempt);
	}

	@Transient
	@JsonIgnore
	public int getCumulativeProgression(Integer requestedWeight) {
		int attempt = getAttemptsDone() + 1;
		return doGetCumulativeProgression(requestedWeight, attempt);
	}

	private int doGetCumulativeProgression(Integer requestedWeight, int attempt) {
		int progression = 0;
		switch (attempt) {
			case 1:
				progression = 0;
				break;
			case 2:
				progression = Math.abs(requestedWeight)
				        - Math.abs(zeroIfInvalid(getSnatch1ActualLift()));
				break;
			case 3:
				progression = Math.abs(requestedWeight)
				        - Math.abs(zeroIfInvalid(getSnatch2ActualLift()))
				        + Math.abs(zeroIfInvalid(getSnatch2ActualLift()))
				        - Math.abs(zeroIfInvalid(getSnatch1ActualLift()));
				break;
			case 4:
				progression = 0;
				break;
			case 5:
				progression = Math.abs(requestedWeight)
				        - Math.abs(zeroIfInvalid(getCleanJerk1ActualLift()));
				break;
			case 6:
				progression = Math.abs(requestedWeight)
				        - Math.abs(zeroIfInvalid(getCleanJerk2ActualLift()))
				        + (Math.abs(zeroIfInvalid(getCleanJerk2ActualLift())))
				        - Math.abs(zeroIfInvalid(getCleanJerk1ActualLift()));
				break;
		}
		// logger.debug("++++ athlete {}, requestedWeight {}, attempt {}, cumulativeProgression {} {}",
		// this.getLastName(), requestedWeight, attempt, progression, LoggerUtils.whereFrom());
		return progression;
	}

	@Transient
	@JsonIgnore
	private LiftOrderInfo getRunningLiftOrderInfo() {
		LiftOrderInfo loi = new LiftOrderInfo();
		loi.setAthlete(this);
		Integer nextAttemptRequestedWeight = this.getNextAttemptRequestedWeight();
		loi.setWeight(nextAttemptRequestedWeight);
		// the clock is running for our current attempt, so done+1
		int attemptsDone = this.getAttemptsDone() + 1;
		loi.setAttemptNo(attemptsDone);
		loi.setProgression(this.getProgression(nextAttemptRequestedWeight));
		loi.setCumulativeProgression(this.getCumulativeProgression(nextAttemptRequestedWeight));
		loi.setStartNumber(this.getStartNumber());
		loi.setLotNumber(this.getLotNumber());
		// getLogger().trace("{}clockOwner: {}", OwlcmsSession.getFopLoggingName(),
		// loi);
		return loi;
	}

	@Transient
	@JsonIgnore
	private Double getSinclair(Double bodyWeight1, Integer total1) {
		if (total1 == null || total1 < 0.1 || (this.gender == null)) {
			return 0.0;
		}
		if (this.gender == Gender.M) { // $NON-NLS-1$
			return total1 * sinclairFactor(bodyWeight1, getSinclairProperties().menCoefficient(),
			        getSinclairProperties().menMaxWeight());
		} else if (this.gender == Gender.F) {
			return total1 * sinclairFactor(bodyWeight1, getSinclairProperties().womenCoefficient(),
			        getSinclairProperties().womenMaxWeight());
		} else {
			return 1.0;
		}
	}

	private SinclairCoefficients getSinclairProperties() {
		if (this.sinclairProperties == null) {
			this.sinclairProperties = (Competition.getCurrent().getSinclairYear() == 2024 ? sinclairProperties2024
			        : sinclairProperties2020);
		}
		return this.sinclairProperties;
	}

	/**
	 * 20kg rule or 80% rule for Masters
	 *
	 * @param cat
	 * @param entryTotal
	 * @return the allowed gap (inclusive) between sum of initial declarations and entry total.
	 */
	@Transient
	@JsonIgnore
	private int getStartingTotalMargin(Category cat, Integer entryTotal) {
		if (cat != null) {
			AgeGroup ag = cat.getAgeGroup();
			if (ag != null) {
				Championship ad = ag.getChampionship();
				if (ad != null) {
					if (ad.getType() == ChampionshipType.MASTERS) {
						double margin = 0.2D * entryTotal;
						// we would round up the required total, so we round down the allowed margin
						double floor = Math.floor(margin);
						int asInt = (int) Math.round(floor);
						// getLogger().trace("margin = {} floor = {} asInt = {} required = {}", margin,
						// floor, asInt,
						// entryTotal - asInt);
						return asInt;
					}
				}
			}
		} else {
			// getLogger().trace("cat {}", cat);
		}
		return 20;
	}

	@Transient
	@JsonIgnore
	public boolean isCategoryDone() {
		return this.categoryDone;
	}

	@Transient
	@JsonIgnore
	private boolean isSameAthleteAs(Athlete other) {
		if (other == null) {
			return false;
		}
		boolean weak = Integer.compare(this.getStartNumber(), other.getStartNumber()) == 0
		        && Integer.compare(this.getLotNumber(), other.getLotNumber()) == 0;
		boolean strong = Long.compare(this.getId(), other.getId()) == 0;
		if (weak && !strong) {
			logger.error("same athlete, different ids {} ", this);
		}
		return weak;

	}

	@SuppressWarnings("unused")
	private boolean isStartingTotalViolation() {
		return this.startingTotalViolation;
	}

	private Integer max(Integer... items) {
		List<Integer> itemList = Arrays.asList(items);
		final Integer max = Collections.max(itemList);
		return max;
	}

	@SuppressWarnings("unused")
	private Integer max(String... items) {
		List<String> itemList = Arrays.asList(items);
		List<Integer> intItemList = new ArrayList<>(itemList.size());
		for (String curString : itemList) {
			intItemList.add(zeroIfInvalid(curString));
		}
		final Integer max = Collections.max(intItemList);
		return max;
	}

	/**
	 * Prevent JPA conflict between two versions of the same object. Likely not needed anymore now that we allocate an
	 * Id to the Athlete in the constructor.
	 *
	 * @param category
	 * @param participations
	 */
	private void removeCurrentAthleteCategoryParticipation(Category category, List<Participation> participations) {
		for (Iterator<Participation> iterator = participations.iterator(); iterator.hasNext();) {
			Participation part = iterator.next();
			long athId = getId();
			long catId = category.getId();
			// defensive null checks due to broken import files (not needed otherwise)
			Athlete athlete = part.getAthlete();
			Category category2 = part.getCategory();
			if (category2 == null || athlete == null) {
				iterator.remove();
				continue;
			} else {
				Long partAthId = athlete.getId();
				Long partCatId = category2.getId();
				if (partAthId == null || partCatId == null) {
					iterator.remove();
					continue;
				}
				if (partAthId == athId && partCatId == catId) {
					// logger.trace(" removing {}", part);
					iterator.remove();
				} else {
					// logger.trace(" ok {} {}-{} {}-{}", part, athId, partAthId, catId, partCatId);
				}
			}
		}
	}

	private void rethrow(RuleViolationException e) throws RuleViolationException {
		// logger.debug("rethrowing {} at {}", e, LoggerUtils.whereFrom());
		throw e;
	}

	private boolean sameCategory(Category category1, Category category2) {
		boolean categoryEqual = category2 != null && category2.getCode().contentEquals(category1.getCode());
		return categoryEqual;
	}

	private void setCopyId(Long id2) {
		this.copyId = id2;
	}

	private void setEligibles(Athlete a, List<Category> categories) {
		TreeSet<Category> eligibles = new TreeSet<>(
		        (x, y) -> ObjectUtils.compare(x.getAgeGroup(), y.getAgeGroup()));
		eligibles.addAll(categories);
		a.setEligibleCategories(eligibles);
	}

	/**
	 * Set all date fields consistently.
	 *
	 * @param newBirthDateAsDate
	 */

	private void setFullBirthDateFromYear(Integer yearOfBirth) {
		if (yearOfBirth != null) {
			this.setFullBirthDate(LocalDate.of(yearOfBirth, 1, 1));
			// logger.trace("{} {}",yearOfBirth,getFullBirthDate());
		} else {
			this.setFullBirthDate(null);
		}
	}

	/**
	 * @param id the id to set
	 */
	private void setId(Long id) {
		this.id = id;
	}

	/**
	 * Compute the Sinclair formula given its parameters.
	 *
	 * @param coefficient
	 * @param maxWeight
	 */
	@Transient
	@JsonIgnore
	private Double sinclairFactor(Double bodyWeight1, Double coefficient, Double maxWeight) {
		if (bodyWeight1 == null) {
			return 0.0;
		}
		if (bodyWeight1 >= maxWeight) {
			return 1.0;
		} else {
			return Math.pow(10.0, coefficient * (Math.pow(Math.log10(bodyWeight1 / maxWeight), 2)));
		}
	}

	private int startingTotalDelta(Integer snatch1Request, Integer cleanJerk1Request, int qualTotal) {
		boolean enforce20kg = Competition.getCurrent().isEnforce20kgRule();
		if (!enforce20kg) {
			return 0;
		}
		int curStartingTotal = 0;
		curStartingTotal = snatch1Request + cleanJerk1Request;
		int delta = qualTotal - curStartingTotal;
		int _20kgRuleValue = getStartingTotalMargin(this.getCategory(), qualTotal);

		if (snatch1Request == 0 && cleanJerk1Request == 0) {
			// not checking starting total - no declarations
			return 0;
		}
		int missing = delta - _20kgRuleValue;
		return missing;
	}

	/**
	 * @param curLift
	 * @param actualLift
	 */
	private void validateChange1(int curLift, String automaticProgression, String declaration, String change1,
	        String change2, String actualLift, boolean isSnatch) throws RuleViolationException {
		if ((actualLift != null && !actualLift.isBlank()) || (curLift > 3 && getAttemptsDone() <= 2)) {
			// ignore cj value when still in snatch
			return;
		}
		long start = System.currentTimeMillis();
		if (change1 == null || change1.trim().length() == 0) {
			return; // allow reset of field.
		}
		int newVal = zeroIfInvalid(change1);
		int prevVal = zeroIfInvalid(automaticProgression);
		if (newVal < prevVal) {
			throw new RuleViolationException.LastChangeTooLow(this, curLift, newVal, prevVal);
		}
		try {
			// logger.debug("validateChange1 {} {} {}",isCheckTiming(),checkBlank(change2),checkBlank(actualLift));
			if (isCheckTiming() && checkBlank(change2) && checkBlank(actualLift)) {
				checkChangeVsTimer(curLift, declaration, change1, change2);
				checkDeclarationWasMade(curLift, declaration);
				checkChangeVsLiftOrder(curLift, newVal);
			}
		} catch (RuleViolationException e) {
			rethrow(e);
		}
		// catch (Exception e) {
		// throw new RuntimeException(e);
		// }
		this.timingLogger.info("validateChange1 {}ms {} {}", System.currentTimeMillis() - start, curLift,
		        LoggerUtils.whereFrom());
	}

	/**
	 * @param curLift
	 * @param actualLift
	 */
	private void validateChange2(int curLift, String automaticProgression, String declaration, String change1,
	        String change2, String actualLift, boolean isSnatch) throws RuleViolationException {
		if (actualLift != null && !actualLift.isBlank()) {
			return;
		}
		long start = System.currentTimeMillis();
		if (change2 == null || change2.trim().length() == 0) {
			return; // allow reset of field.
		}
		int newVal = zeroIfInvalid(change2);
		int prevVal = zeroIfInvalid(automaticProgression);
		if (newVal < prevVal) {
			throw new RuleViolationException.LastChangeTooLow(this, curLift, newVal, prevVal);
		}
		try {
			// logger.debug("validateChange2 {} {} {}",isCheckTiming(),checkBlank(actualLift));
			if (isCheckTiming() && checkBlank(actualLift)) {
				checkChangeVsTimer(curLift, declaration, change1, change2);
				checkDeclarationWasMade(curLift, declaration);
				checkChangeVsLiftOrder(curLift, newVal);
			}
		} catch (RuleViolationException e) {
			rethrow(e);
		} catch (Exception e) {
			logger.error("{}", LoggerUtils.exceptionMessage(e));
			throw new RuntimeException(e);
		}
		this.timingLogger.info("validateChange2 {}ms {} {}", System.currentTimeMillis() - start, curLift,
		        LoggerUtils.whereFrom());
	}

	/**
	 * @param curLift
	 * @param actualLift
	 */
	private void validateDeclaration(int curLift, String automaticProgression, String declaration, String change1,
	        String change2, String actualLift) throws RuleViolationException {
		if (declaration == null || declaration.isBlank() || (actualLift != null && !actualLift.isBlank())) {
			return;
		}
		long start = System.currentTimeMillis();
		// getLogger().debug("{}{} validateDeclaration {} {} {} from {}", OwlcmsSession.getFopLoggingName(), this,
		// declaration, change1, change2, LoggerUtils.stackTrace());
		int newVal = zeroIfInvalid(declaration);
		int iAutomaticProgression = zeroIfInvalid(automaticProgression);
		// allow null declaration for reloading old results.
		if (iAutomaticProgression > 0 && newVal > 0 && newVal < iAutomaticProgression) {
			throw new RuleViolationException.DeclarationValueTooSmall(this, curLift, newVal, iAutomaticProgression);
		}
		try {
			// logger.debug("validateDeclaration {} {} {}",isCheckTiming(),checkBlank(change1),checkBlank(actualLift));
			if (isCheckTiming() && (checkBlank(change1) && checkBlank(actualLift))) {
				checkChangeVsTimer(curLift, declaration, change1, change2);
				checkChangeVsLiftOrder(curLift, newVal);
			}
		} catch (RuleViolationException e) {
			rethrow(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.timingLogger.info("validateDeclaration {}ms {} {}", System.currentTimeMillis() - start, curLift,
		        LoggerUtils.whereFrom());
	}

	@Transient
	@JsonIgnore
	public boolean isDone() {
		boolean notFinishedLifting = this.getCleanJerk3ActualLift() == null || this.getCleanJerk3ActualLift().isBlank()
		        || this.getCleanJerk3AsInteger() == null;
		return !notFinishedLifting;
	}
	
	@Transient
	@JsonIgnore
	public boolean isDone(Group medalingSession) {
		// At the end of session "medalingSession", If a category still has athletes that are not done, medals cannot be given out for that category
		Group athleteGroup = getGroup();
		if (athleteGroup == null) {
			// athletes that are registered in the medaling categories but were withdrawn (have no session) are considered done
			return true;
		}
		if (medalingSession != null && athleteGroup.equals(medalingSession) && (getBodyWeight() == null || getBodyWeight() < 0.01)) {
			// athletes in the current group that have not weighed in are considered done.
			return true;
		}
		boolean notFinishedLifting = this.getCleanJerk3ActualLift() == null || this.getCleanJerk3ActualLift().isBlank()
		        || this.getCleanJerk3AsInteger() == null;
		return !notFinishedLifting;
	}

	@Transient
	@JsonIgnore
	public Double getAgeAdjustedTotal() {
		Integer total = getBestCleanJerk() + getBestSnatch();
		return (double) AgeFactors.getAgeAdjustedTotal(this, total);
	}
	
	@Transient
	@JsonIgnore
	public Double getGamx() {
		Integer total = getBestCleanJerk() + getBestSnatch();
		return (double) GAMX.getGamx(this, total);
	}

	@Transient
	@JsonIgnore
	public Integer getAgeAdjustedTotalRank() {
		return this.ageAdjustedTotalRank;
	}
	

	@Transient
	@JsonIgnore
	public Integer getGamxRank() {
		return this.gamxRank;
	}
	
	public void setAgeAdjustedTotalRank(Integer ageAdjustedTotalRank) {
		this.ageAdjustedTotalRank = ageAdjustedTotalRank;
	}
	
	public void setGamxRank(Integer rank) {
		this.gamxRank = rank;
	}

	public Double getQAge() {
		double d = getQPoints() * getSmhfFactor();
		return d;
	}

}
