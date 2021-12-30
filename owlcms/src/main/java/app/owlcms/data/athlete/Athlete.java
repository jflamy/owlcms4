/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.data.athlete;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.category.RobiCategories;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.LiftOrderInfo;
import app.owlcms.fieldofplay.LiftOrderReconstruction;
import app.owlcms.init.OwlcmsSession;
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

//must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
@JsonPropertyOrder({ "id", "participations", "category" })
public class Athlete {
    private static final int YEAR = LocalDateTime.now().getYear();

    @Transient
    protected final static Logger logger = (Logger) LoggerFactory.getLogger(Athlete.class);

    static private boolean skipValidationsDuringImport = false;

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
                dest.setSmmRank(src.getSmmRank());
                dest.setTeamSinclairRank(src.getTeamSinclairRank());
                dest.setCatSinclairRank(src.getCatSinclairRank());
            }
        } finally {
            dest.setValidation(validation);
            dest.setLoggerLevel(prevDestLevel);
            src.setLoggerLevel(prevSrcLevel);
        }
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

    private String coach;

    @Transient
    private Long copyId = null;

    @Transient
    private final Level NORMAL_LEVEL = Level.INFO;

    @Id
    // @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer lotNumber = null;

    private Integer startNumber = null;

    private String firstName = "";

    private String lastName = "";

    private String team = "";

    private Gender gender = null; // $NON-NLS-1$

    private LocalDate fullBirthDate = null;

    private Double bodyWeight = null;

    private String membership = "";

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH }, optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_group", nullable = true)
    private Group group;
    /*
     * eager does not hurt for us.
     * https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH }, optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_categ", nullable = true)
    @JsonProperty(index = 300)
    @JsonIdentityReference(alwaysAsId = true)
    private Category category = null;
    @OneToMany(mappedBy = "athlete", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonProperty(index = 200)
    private List<Participation> participations = new ArrayList<>();
    /**
     * Using separate fields is brute force, but having embedded classes does not bring much and we don't want joins or
     * other such logic for the Athlete card. Since the Athlete card is 6 x 4 items, we take the simple route.
     *
     * The use of Strings is historical. It was extremely cumbersome to handle conversions to/from Integer in Vaadin 6
     * circa 2009, and migration of databases would be annoying to users.
     */
    private String snatch1Declaration;
    private String snatch1Change1;
    private String snatch1Change2;

    private String snatch1ActualLift;
    private LocalDateTime snatch1LiftTime;
    private String snatch2Declaration;
    private String snatch2Change1;
    private String snatch2Change2;

    private String snatch2ActualLift;
    private LocalDateTime snatch2LiftTime;
    private String snatch3Declaration;
    private String snatch3Change1;
    private String snatch3Change2;

    private String snatch3ActualLift;
    private LocalDateTime snatch3LiftTime;
    private String cleanJerk1Declaration;
    private String cleanJerk1Change1;
    private String cleanJerk1Change2;

    private String cleanJerk1ActualLift;
    private LocalDateTime cleanJerk1LiftTime;
    private String cleanJerk2Declaration;
    private String cleanJerk2Change1;
    private String cleanJerk2Change2;

    private String cleanJerk2ActualLift;
    private LocalDateTime cleanJerk2LiftTime;
    private String cleanJerk3Declaration;
    private String cleanJerk3Change1;
    private String cleanJerk3Change2;

    private String cleanJerk3ActualLift;
    private LocalDateTime cleanJerk3LiftTime;
    private Integer sinclairRank;
    private Integer robiRank;
    private Integer teamSinclairRank;
    private Integer teamRobiRank;
    private Integer teamSnatchRank;
    private Integer teamCleanJerkRank;
    private Integer teamTotalRank;

    private Integer teamCustomRank;
    private Integer teamCombinedRank;
    @Column(columnDefinition = "integer default 0")
    private int catSinclairRank;
    @Column(columnDefinition = "integer default 0")
    private int combinedRank;
    @Column(columnDefinition = "integer default 0")
    private int smmRank;
    private Integer qualifyingTotal = 0;
    private Double customScore;
    @Column(columnDefinition = "boolean default true")
    private boolean eligibleForIndividualRanking = true;
    /** The forced as current. */
    @Column(columnDefinition = "boolean default false")
    private boolean forcedAsCurrent = false;

    private boolean eligibleForTeamRanking = true;
    /**
     * body weight inferred from category, used until real bodyweight is known.
     */
    private Double presumedBodyWeight;
    /*
     * Non-persistent properties. These properties will be lost as soon as the athlete is saved.
     */
    @Transient
    Integer liftOrderRank = 0;

    @Transient
    private boolean validation = true;

    @Transient
    DecimalFormat df = null;

    private String custom1;

    private String custom2;

    /**
     * Instantiates a new athlete.
     */
    public Athlete() {
        setId(IdUtils.getTimeBasedId());
        validation = true;
    }

    public void addEligibleCategory(Category category) {
        if (category == null) {
            return;
        }
        Participation participation = new Participation(this, category);

        if (participations == null) {
            participations = new ArrayList<>();
        }
        removeCurrentAthleteCategoryParticipation(category, participations);
        participations.add(participation);
        setParticipations(participations);

        List<Participation> categoryParticipations = category.getParticipations();
        if (categoryParticipations == null) {
            categoryParticipations = new ArrayList<>();
        }
        removeCurrentAthleteCategoryParticipation(category, categoryParticipations);
        categoryParticipations.add(participation);
        category.setParticipations(categoryParticipations);
    }

    public void checkParticipations() {
        for (Iterator<Participation> iterator = participations.iterator(); iterator.hasNext();) {
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
            this.setCleanJerk1ActualLift("");
            this.setCleanJerk1LiftTime(null);

            this.setCleanJerk2Declaration("");
            this.setCleanJerk2AutomaticProgression("");
            this.setCleanJerk2Change1("");
            this.setCleanJerk2Change2("");
            this.setCleanJerk2ActualLift("");
            this.setCleanJerk2LiftTime(null);

            this.setCleanJerk3Declaration("");
            this.setCleanJerk3AutomaticProgression("");
            this.setCleanJerk3Change1("");
            this.setCleanJerk3Change2("");
            this.setCleanJerk3ActualLift("");
            this.setCleanJerk3LiftTime(null);

            this.setSnatch1Declaration("");
            this.setSnatch1AutomaticProgression("");
            this.setSnatch1Change1("");
            this.setSnatch1Change2("");
            this.setSnatch1ActualLift("");
            this.setSnatch1LiftTime(null);

            this.setSnatch2Declaration("");
            this.setSnatch2AutomaticProgression("");
            this.setSnatch2Change1("");
            this.setSnatch2Change2("");
            this.setSnatch2ActualLift("");
            this.setSnatch2LiftTime(null);

            this.setSnatch3Declaration("");
            this.setSnatch3AutomaticProgression("");
            this.setSnatch3Change1("");
            this.setSnatch3Change2("");
            this.setSnatch3ActualLift("");
            this.setSnatch3LiftTime(null);

            this.setSnatch1Declaration(sn1Decl);
            this.setCleanJerk1Declaration(cj1Decl);
        } finally {
            this.setValidation(validate);
            this.setLoggerLevel(prevLevel);
        }
    }

    public void computeMainCategory() {
        Double weight = this.getBodyWeight();
        Integer age = this.getAge();
        if (weight == null || weight < 0.01) {
//            logger.debug("no weight {}", this.getShortName());
            Double presumedBodyWeight = this.getPresumedBodyWeight();
//            logger.debug("presumed weight {} {} {}",this.getShortName(), presumedBodyWeight, this.getCategory());
            if (presumedBodyWeight != null) {
                weight = presumedBodyWeight - 0.01D;
                if (age == null || age == 0) {

                    // try to set category to match sheet, with coherent eligibles
                    if (this.category != null) {
                        age = category.getAgeGroup().getMaxAge();
                    }
                }

                List<Category> categories = CategoryRepository.findByGenderAgeBW(
                        this.getGender(), age, weight);

                categories = categories.stream()
//                        .peek((c) -> {
//                            logger.debug("no weight a {} aq {} cq {}", this.getShortName(), this.getQualifyingTotal(),
//                                    c.getQualifyingTotal());
//                        })
                        .filter(c -> this.getQualifyingTotal() >= c.getQualifyingTotal()).collect(Collectors.toList());
//                logger.debug("{} presumed weight {} age {} {} {}",this.getShortName(), presumedBodyWeight, age, this.getCategory(), categories);
                setEligibles(this, categories);
                this.setCategory(bestMatch(categories));

//                logger.debug("{} {} gender {} age {} weight {} category *{}* categories {}", this.getId(), this.getShortName(), this.getGender(), this.getAge(), weight, this.getCategory(), categories);

            }
        } else {
//            logger.debug("weight {}", this.getShortName());
            List<Category> categories = CategoryRepository.findByGenderAgeBW(
                    this.getGender(), age, weight);
            categories = categories.stream()
//                    .peek((c) -> {
//                        logger.debug("a {} aq {} cq {}", this.getShortName(), this.getQualifyingTotal(),
//                                c.getQualifyingTotal());
//                    })
                    .filter(c -> this.getQualifyingTotal() >= c.getQualifyingTotal()).collect(Collectors.toList());
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
        if (category != null) {
            // we can't have category without eligibility relationship and one with same id that has it in the
            // eligibility list
            // so we find the one in the eligibility list and use it.
            Category matchingEligible = null;
            for (Category eligible : getEligibleCategories()) {
                if (ObjectUtils.compare(eligible.getName(), category.getName()) == 0) {
                    matchingEligible = eligible;
                    break;
                }
            }
            setCategory(matchingEligible);
            logger.trace("category {} {} matching eligible {} {}", category, System.identityHashCode(category),
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
        OwlcmsSession.withFop(fop -> {
            try {
                getLogger().info("{}no lift for {}", OwlcmsSession.getFopLoggingName(), this.getShortName());
                final String weight = Integer.toString(-getNextAttemptRequestedWeight());
                doLift(weight);
            } catch (Exception e) {
                getLogger().error(e.getLocalizedMessage());
            }
        });
    }

    @Transient
    @JsonIgnore
    public Integer getActualLift(int liftNo) {
        try {
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
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            LoggerUtils.logError(logger, e);
            return 0;
        }
    }

    /**
     * @return age as of current day
     */
    @Transient
    @JsonIgnore
    public Integer getAge() {
        // LocalDate date = Competition.getCurrent().getCompetitionDate();
        LocalDate date = null;
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

    /**
     * Gets the attempted lifts. 0 means no lift done.
     *
     * @return the attempted lifts
     */
    @Transient
    @JsonIgnore
    public int getAttemptedLifts() {
        int i = 0;
        if (zeroIfInvalid(snatch1ActualLift) != 0) {
            i++;
        }
        if (zeroIfInvalid(snatch2ActualLift) != 0) {
            i++;
        }
        if (zeroIfInvalid(snatch3ActualLift) != 0) {
            i++;
        }
        if (zeroIfInvalid(cleanJerk1ActualLift) != 0) {
            i++;
        }
        if (zeroIfInvalid(cleanJerk2ActualLift) != 0) {
            i++;
        }
        if (zeroIfInvalid(cleanJerk3ActualLift) != 0) {
            i++;
        }
        return i; // long ago
    }

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
        final int cj1 = zeroIfInvalid(cleanJerk1ActualLift);
        final int cj2 = zeroIfInvalid(cleanJerk2ActualLift);
        final int cj3 = zeroIfInvalid(cleanJerk3ActualLift);
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
            if (zeroIfInvalid(cleanJerk3ActualLift) == referenceValue) {
                return 6;
            }
            if (zeroIfInvalid(cleanJerk2ActualLift) == referenceValue) {
                return 5;
            }
            if (zeroIfInvalid(cleanJerk1ActualLift) == referenceValue) {
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
            if (zeroIfInvalid(cleanJerk3ActualLift) == referenceValue) {
                return 6;
            }
            if (zeroIfInvalid(cleanJerk2ActualLift) == referenceValue) {
                return 5;
            }
            if (zeroIfInvalid(cleanJerk1ActualLift) == referenceValue) {
                return 4;
            }
        } else {
            if (referenceValue > 0) {
                referenceValue = getBestSnatch();
                if (zeroIfInvalid(snatch3ActualLift) == referenceValue) {
                    return 3;
                }
                if (zeroIfInvalid(snatch2ActualLift) == referenceValue) {
                    return 2;
                }
                if (zeroIfInvalid(snatch1ActualLift) == referenceValue) {
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
        final int sn1 = zeroIfInvalid(snatch1ActualLift);
        final int sn2 = zeroIfInvalid(snatch2ActualLift);
        final int sn3 = zeroIfInvalid(snatch3ActualLift);
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
            if (zeroIfInvalid(snatch3ActualLift) == referenceValue) {
                return 3;
            }
            if (zeroIfInvalid(snatch2ActualLift) == referenceValue) {
                return 2;
            }
            if (zeroIfInvalid(snatch1ActualLift) == referenceValue) {
                return 1;
            }
        }
        return 0; // no match - bomb-out.

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
        return bodyWeight;
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
        return category;
    }

    @Transient
    @JsonIgnore
    public String getCategoryCode() {
        return category != null ? category.getCode() : "-";
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
            } else if (categoryWeight > SinclairCoefficients.menMaxWeight()) {
                categoryWeight = SinclairCoefficients.menMaxWeight();
            }
        } else {
            if (categoryWeight < 45.0) {
                categoryWeight = 45.0;
            } else if (categoryWeight > SinclairCoefficients.womenMaxWeight()) {
                categoryWeight = SinclairCoefficients.womenMaxWeight();
            }
        }
        return getSinclair(categoryWeight);
    }

    public int getCatSinclairRank() {
        return catSinclairRank;
    }

    /**
     * Gets the clean jerk 1 actual lift.
     *
     * @return the clean jerk 1 actual lift
     */
    public String getCleanJerk1ActualLift() {
        return emptyIfNull(cleanJerk1ActualLift);
    }

    /**
     * Gets the clean jerk 1 as integer.
     *
     * @return the clean jerk 1 as integer
     */
    @Transient
    @JsonIgnore
    public Integer getCleanJerk1AsInteger() {
        return asInteger(cleanJerk1ActualLift);
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
        return emptyIfNull(cleanJerk1Change1);
    }

    /**
     * Gets the clean jerk 1 change 2.
     *
     * @return the clean jerk 1 change 2
     */
    public String getCleanJerk1Change2() {
        return emptyIfNull(cleanJerk1Change2);
    }

    /**
     * Gets the clean jerk 1 declaration.
     *
     * @return the clean jerk 1 declaration
     */
    public String getCleanJerk1Declaration() {
        return emptyIfNull(cleanJerk1Declaration);
    }

    /**
     * Gets the clean jerk 1 lift time.
     *
     * @return the clean jerk 1 lift time
     */
    public LocalDateTime getCleanJerk1LiftTime() {
        return cleanJerk1LiftTime;
    }

    /**
     * Gets the clean jerk 2 actual lift.
     *
     * @return the clean jerk 2 actual lift
     */
    public String getCleanJerk2ActualLift() {
        return emptyIfNull(cleanJerk2ActualLift);
    }

    /**
     * Gets the clean jerk 2 as integer.
     *
     * @return the clean jerk 2 as integer
     */
    @Transient
    @JsonIgnore
    public Integer getCleanJerk2AsInteger() {
        return asInteger(cleanJerk2ActualLift);
    }

    /**
     * Gets the clean jerk 2 automatic progression.
     *
     * @return the clean jerk 2 automatic progression
     */
    public String getCleanJerk2AutomaticProgression() {
        final int prevVal = zeroIfInvalid(cleanJerk1ActualLift);
        return doAutomaticProgression(prevVal);
    }

    /**
     * Gets the clean jerk 2 change 1.
     *
     * @return the clean jerk 2 change 1
     */
    public String getCleanJerk2Change1() {
        return emptyIfNull(cleanJerk2Change1);
    }

    /**
     * Gets the clean jerk 2 change 2.
     *
     * @return the clean jerk 2 change 2
     */
    public String getCleanJerk2Change2() {
        return emptyIfNull(cleanJerk2Change2);
    }

    /**
     * Gets the clean jerk 2 declaration.
     *
     * @return the clean jerk 2 declaration
     */
    public String getCleanJerk2Declaration() {
        return emptyIfNull(cleanJerk2Declaration);
    }

    /**
     * Gets the clean jerk 2 lift time.
     *
     * @return the clean jerk 2 lift time
     */
    public LocalDateTime getCleanJerk2LiftTime() {
        return cleanJerk2LiftTime;
    }

    /**
     * Gets the clean jerk 3 actual lift.
     *
     * @return the clean jerk 3 actual lift
     */
    public String getCleanJerk3ActualLift() {
        return emptyIfNull(cleanJerk3ActualLift);
    }

    /**
     * Gets the clean jerk 3 as integer.
     *
     * @return the clean jerk 3 as integer
     */
    @Transient
    @JsonIgnore
    public Integer getCleanJerk3AsInteger() {
        return asInteger(cleanJerk3ActualLift);
    }

    /**
     * Gets the clean jerk 3 automatic progression.
     *
     * @return the clean jerk 3 automatic progression
     */
    public String getCleanJerk3AutomaticProgression() {
        final int prevVal = zeroIfInvalid(cleanJerk2ActualLift);
        return doAutomaticProgression(prevVal);
    }

    /**
     * Gets the clean jerk 3 change 1.
     *
     * @return the clean jerk 3 change 1
     */
    public String getCleanJerk3Change1() {
        return emptyIfNull(cleanJerk3Change1);
    }

    /**
     * Gets the clean jerk 3 change 2.
     *
     * @return the clean jerk 3 change 2
     */
    public String getCleanJerk3Change2() {
        return emptyIfNull(cleanJerk3Change2);
    }

    /**
     * Gets the clean jerk 3 declaration.
     *
     * @return the clean jerk 3 declaration
     */
    public String getCleanJerk3Declaration() {
        return emptyIfNull(cleanJerk3Declaration);
    }

    /**
     * Gets the clean jerk 3 lift time.
     *
     * @return the clean jerk 3 lift time
     */
    public LocalDateTime getCleanJerk3LiftTime() {
        return cleanJerk3LiftTime;
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
        if (!isEmpty(cleanJerk1ActualLift)) {
            attempts++;
        } else {
            return attempts;
        }
        if (!isEmpty(cleanJerk2ActualLift)) {
            attempts++;
        } else {
            return attempts;
        }
        if (!isEmpty(cleanJerk3ActualLift)) {
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
        final int cleanJerkTotal = max(0, zeroIfInvalid(cleanJerk1ActualLift), zeroIfInvalid(cleanJerk2ActualLift),
                zeroIfInvalid(cleanJerk3ActualLift));
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
        return getSnatchPoints() + getCleanJerkPoints() + getTotalPoints();
    }

    public int getCombinedRank() {
        return combinedRank;
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
        return custom1;
    }

    /**
     * @return the custom2
     */
    public String getCustom2() {
        return custom2;
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
        return customScore;
    }

    /**
     * Gets the custom score.
     *
     * @return the custom score
     */
    @Transient
    @JsonIgnore
    public Double getCustomScoreComputed() {
        if (this.customScore == null || this.customScore < 0.01) {
            return Double.valueOf(getTotal());
        }
        return customScore;
    }

    /**
     * Gets the display category.
     *
     * @return the display category
     */
    @Transient
    @JsonIgnore
    public String getDisplayCategory() {
        return getLongCategory();
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
        // logger.trace("{}{} getEligibleCategories {} from {}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
        // s.toString(),
        // LoggerUtils.whereFrom());
        return s;
    }

    public Integer getEntryTotal() {
        // intentional, this is the legacy name of the column in the database
        return getQualifyingTotal();
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
        if (zeroIfInvalid(snatch1ActualLift) != 0) {
            attemptTime = getSnatch1LiftTime();
        } else if (zeroIfInvalid(snatch2ActualLift) != 0) {
            attemptTime = getSnatch2LiftTime();
        } else if (zeroIfInvalid(snatch3ActualLift) != 0) {
            attemptTime = getSnatch3LiftTime();
        } else if (zeroIfInvalid(cleanJerk1ActualLift) != 0) {
            attemptTime = getCleanJerk1LiftTime();
        } else if (zeroIfInvalid(cleanJerk2ActualLift) != 0) {
            attemptTime = getCleanJerk2LiftTime();
        } else if (zeroIfInvalid(cleanJerk3ActualLift) != 0) {
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
        return firstName;
    }

    @Transient
    @JsonIgnore
    public String getFormattedBirth() {
        if (Competition.getCurrent().isUseBirthYear()) {
            Integer yearOfBirth = getYearOfBirth();
            return yearOfBirth != null ? yearOfBirth.toString() : "";
        } else {
            return getFullBirthDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        }
    }

    /**
     * Gets the full birth date.
     *
     * @return the fullBirthDate
     */
    public LocalDate getFullBirthDate() {
        return fullBirthDate;
    }

    @Transient
    @JsonIgnore
    public String getFullId() {
        String fullName = getFullName();
        Category category2 = getCategory();
        if (!fullName.isEmpty()) {
            return fullName + " " + (category2 != null ? category2 : "");
//              +(startNumber2 != null && startNumber2 >0 ? " ["+startNumber2+"]" : "");
        } else {
            return "";
        }
    }

    @Transient
    @JsonIgnore
    public String getFullName() {
        String upperCase = this.getLastName().toUpperCase();
        String firstName2 = this.getFirstName();
        if ((upperCase != null) && !upperCase.trim().isEmpty() && (firstName2 != null)
                && !firstName2.trim().isEmpty()) {
            return upperCase + ", " + firstName2;
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
        return gender;
    }

    /**
     * Gets the group.
     *
     * @return the group
     */
    @JsonIdentityReference(alwaysAsId = true)
    public Group getGroup() {
        return group;
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
     * Gets the last attempted lift time.
     *
     * @return the last attempted lift time
     */
    @Transient
    @JsonIgnore
    public LocalDateTime getLastAttemptedLiftTime() {
        if (zeroIfInvalid(cleanJerk3ActualLift) != 0) {
            return getCleanJerk3LiftTime();
        }
        if (zeroIfInvalid(cleanJerk2ActualLift) != 0) {
            return getCleanJerk2LiftTime();
        }
        if (zeroIfInvalid(cleanJerk1ActualLift) != 0) {
            return getCleanJerk1LiftTime();
        }
        if (zeroIfInvalid(snatch3ActualLift) != 0) {
            return getSnatch3LiftTime();
        }
        if (zeroIfInvalid(snatch2ActualLift) != 0) {
            return getSnatch2LiftTime();
        }
        if (zeroIfInvalid(snatch1ActualLift) != 0) {
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
        return lastName;
    }

    /**
     * Gets the last successful lift time.
     *
     * @return the last successful lift time
     */
    @Transient
    @JsonIgnore
    public LocalDateTime getLastSuccessfulLiftTime() {
        if (zeroIfInvalid(cleanJerk3ActualLift) > 0) {
            return getCleanJerk3LiftTime();
        }
        if (zeroIfInvalid(cleanJerk2ActualLift) > 0) {
            return getCleanJerk2LiftTime();
        }
        if (zeroIfInvalid(cleanJerk1ActualLift) > 0) {
            return getCleanJerk1LiftTime();
        }
        if (zeroIfInvalid(snatch3ActualLift) > 0) {
            return getSnatch3LiftTime();
        }
        if (zeroIfInvalid(snatch2ActualLift) > 0) {
            return getSnatch2LiftTime();
        }
        if (zeroIfInvalid(snatch1ActualLift) > 0) {
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
        return liftOrderRank;
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
        return (category != null ? category.getName() : "");
    }

    /**
     * Gets the lot number.
     *
     * @return the lotNumber
     */
    public Integer getLotNumber() {
        return (lotNumber == null ? 0 : lotNumber);
    }

    @Transient
    @JsonIgnore
    public Participation getMainRankings() {
        Participation curRankings = null;
        List<Participation> participations2 = getParticipations();
        // logger.debug("athlete {} category {} participations {}", this, category, participations2);
        for (Participation eligible : participations2) {
            Category eligibleCat = eligible.getCategory();
            if (category != null && eligibleCat != null) {
                String eligibleCode = eligibleCat.getComputedCode();
                String catCode = category.getComputedCode();
                if (StringUtils.equals(eligibleCode, catCode)) {
                    curRankings = eligible;
                    // logger.debug("yep eligibleCode '{}' catCode '{}'", eligibleCode, catCode);
                    break;
                } else {
                    // logger.debug("nope eligibleCode '{}' catCode '{}'", eligibleCode, catCode);
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
        return getCategory().getName();
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
        return membership;
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
        return participations;
    }

    public Double getPresumedBodyWeight() {
        Double bodyWeight2 = getBodyWeight();
        if (bodyWeight2 != null && bodyWeight2 >= 0) {
            return bodyWeight2;
        }
        if (category != null) {
            return category.getMaximumWeight();
        }
        return presumedBodyWeight;
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

    /**
     * Gets the qualifying total.
     *
     * @return the qualifying total
     */
    public Integer getQualifyingTotal() {
        if (qualifyingTotal == null) {
            return 0;
        }
        return qualifyingTotal;
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
        return category;
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
            return last(zeroIfInvalid(getSnatch1AutomaticProgression()), zeroIfInvalid(snatch1Declaration),
                    zeroIfInvalid(snatch1Change1), zeroIfInvalid(snatch1Change2));
        case 2:
            return last(zeroIfInvalid(getSnatch2AutomaticProgression()), zeroIfInvalid(snatch2Declaration),
                    zeroIfInvalid(snatch2Change1), zeroIfInvalid(snatch2Change2));
        case 3:
            return last(zeroIfInvalid(getSnatch3AutomaticProgression()), zeroIfInvalid(snatch3Declaration),
                    zeroIfInvalid(snatch3Change1), zeroIfInvalid(snatch3Change2));
        case 4:
            return last(zeroIfInvalid(getCleanJerk1AutomaticProgression()), zeroIfInvalid(cleanJerk1Declaration),
                    zeroIfInvalid(cleanJerk1Change1), zeroIfInvalid(cleanJerk1Change2));
        case 5:
            return last(zeroIfInvalid(getCleanJerk2AutomaticProgression()), zeroIfInvalid(cleanJerk2Declaration),
                    zeroIfInvalid(cleanJerk2Change1), zeroIfInvalid(cleanJerk2Change2));
        case 6:
            return last(zeroIfInvalid(getCleanJerk3AutomaticProgression()), zeroIfInvalid(cleanJerk3Declaration),
                    zeroIfInvalid(cleanJerk3Change1), zeroIfInvalid(cleanJerk3Change2));
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
        Participation mainRankings;
        Category c;
        if ((mainRankings = getMainRankings()) == null || (c = mainRankings.getCategory()) == null) {
            return 0.0;
        }
        Integer wr = c.getWr();
        if (wr == null || wr <= 0.000001) {
            // not an IWF category, find what the IWF Robi would be for age/body weight
            Category robiC = RobiCategories.findRobiCategory(this);
            if (robiC == null) {
                return 0.0;
            }
            Integer age = getAge();
            if (age != null) {
                wr = robiC.getWr(age);
            } else {
                return robiC.getWr(999) + 0.0D;
            }
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
        return robiRank;
    }

    @Transient
    @JsonIgnore
    public String getRoundedBodyWeight() {
        if (df == null) {
            df = new DecimalFormat("#.##");
        }
        return df.format(getBodyWeight());
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
        if (gender == Gender.M) {
            return sinclairFactor(this.bodyWeight, SinclairCoefficients.menCoefficient(),
                    SinclairCoefficients.menMaxWeight());
        } else {
            return sinclairFactor(this.bodyWeight, SinclairCoefficients.womenCoefficient(),
                    SinclairCoefficients.womenMaxWeight());
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
        return sinclairRank;
    }

    /**
     * Gets the smm.
     *
     * @return the smm
     */
    @Transient
    @JsonIgnore
    public Double getSmm() {
        final Integer birthDate1 = getYearOfBirth();
        if (birthDate1 == null) {
            return 0.0;
        }
        return getSinclair() * SinclairCoefficients.getSMMCoefficient(YEAR - birthDate1);
    }

    public int getSmmRank() {
        return smmRank;
    }

    /**
     * Gets the snatch 1 actual lift.
     *
     * @return the snatch 1 actual lift
     */
    public String getSnatch1ActualLift() {
        return emptyIfNull(snatch1ActualLift);
    }

    /**
     * Gets the snatch 1 as integer.
     *
     * @return the snatch 1 as integer
     */
    @Transient
    @JsonIgnore
    public Integer getSnatch1AsInteger() {
        return asInteger(snatch1ActualLift);
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
        return emptyIfNull(snatch1Change1);
    }

    /**
     * Gets the snatch 1 change 2.
     *
     * @return the snatch 1 change 2
     */
    public String getSnatch1Change2() {
        return emptyIfNull(snatch1Change2);
    }

    /**
     * Gets the snatch 1 declaration.
     *
     * @return the snatch 1 declaration
     */
    public String getSnatch1Declaration() {
        return emptyIfNull(snatch1Declaration);
    }

    /**
     * Gets the snatch 1 lift time.
     *
     * @return the snatch 1 lift time
     */
    public LocalDateTime getSnatch1LiftTime() {
        return snatch1LiftTime;
    }

    /**
     * Gets the snatch 2 actual lift.
     *
     * @return the snatch 2 actual lift
     */
    public String getSnatch2ActualLift() {
        return emptyIfNull(snatch2ActualLift);
    }

    /**
     * Gets the snatch 2 as integer.
     *
     * @return the snatch 2 as integer
     */
    @Transient
    @JsonIgnore
    public Integer getSnatch2AsInteger() {
        return asInteger(snatch2ActualLift);
    }

    /**
     * Gets the snatch 2 automatic progression.
     *
     * @return the snatch 2 automatic progression
     */
    public String getSnatch2AutomaticProgression() {
        final int prevVal = zeroIfInvalid(snatch1ActualLift);
        return doAutomaticProgression(prevVal);
    }

    /**
     * Gets the snatch 2 change 1.
     *
     * @return the snatch 2 change 1
     */
    public String getSnatch2Change1() {
        return emptyIfNull(snatch2Change1);
    }

    /**
     * Gets the snatch 2 change 2.
     *
     * @return the snatch 2 change 2
     */
    public String getSnatch2Change2() {
        return emptyIfNull(snatch2Change2);
    }

    /**
     * Gets the snatch 2 declaration.
     *
     * @return the snatch 2 declaration
     */
    public String getSnatch2Declaration() {
        return emptyIfNull(snatch2Declaration);
    }

    /**
     * Gets the snatch 2 lift time.
     *
     * @return the snatch 2 lift time
     */
    public LocalDateTime getSnatch2LiftTime() {
        return snatch2LiftTime;
    }

    /**
     * Gets the snatch 3 actual lift.
     *
     * @return the snatch 3 actual lift
     */
    public String getSnatch3ActualLift() {
        return emptyIfNull(snatch3ActualLift);
    }

    /**
     * Gets the snatch 3 as integer.
     *
     * @return the snatch 3 as integer
     */
    @Transient
    @JsonIgnore
    public Integer getSnatch3AsInteger() {
        return asInteger(snatch3ActualLift);
    }

    /**
     * Gets the snatch 3 automatic progression.
     *
     * @return the snatch 3 automatic progression
     */
    public String getSnatch3AutomaticProgression() {
        final int prevVal = zeroIfInvalid(snatch2ActualLift);
        return doAutomaticProgression(prevVal);
    }

    /**
     * Gets the snatch 3 change 1.
     *
     * @return the snatch 3 change 1
     */
    public String getSnatch3Change1() {
        return emptyIfNull(snatch3Change1);
    }

    /**
     * Gets the snatch 3 change 2.
     *
     * @return the snatch 3 change 2
     */
    public String getSnatch3Change2() {
        return emptyIfNull(snatch3Change2);
    }

    /**
     * Gets the snatch 3 declaration.
     *
     * @return the snatch 3 declaration
     */
    public String getSnatch3Declaration() {
        return emptyIfNull(snatch3Declaration);
    }

    /**
     * Gets the snatch 3 lift time.
     *
     * @return the snatch 3 lift time
     */
    public LocalDateTime getSnatch3LiftTime() {
        return snatch3LiftTime;
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
        if (!isEmpty(snatch1ActualLift)) {
            attempts++;
        } else {
            return attempts;
        }
        if (!isEmpty(snatch2ActualLift)) {
            attempts++;
        } else {
            return attempts;
        }
        if (!isEmpty(snatch3ActualLift)) {
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
        // logger.debug("{} snatchRank {}", this.getShortName(), snatchRank);
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
        final int snatchTotal = max(0, zeroIfInvalid(snatch1ActualLift), zeroIfInvalid(snatch2ActualLift),
                zeroIfInvalid(snatch3ActualLift));
        return snatchTotal;
    }

    /**
     * Gets the start number.
     *
     * @return the start number
     */
    public Integer getStartNumber() {
        return startNumber != null ? startNumber : 0;
    }

    /**
     * Gets the team.
     *
     * @return the team
     */
    public String getTeam() {
        return team;
    }

    /**
     * Gets the team clean jerk rank.
     *
     * @return the team clean jerk rank
     */
    public Integer getTeamCleanJerkRank() {
        return teamCleanJerkRank;
    }

    /**
     * Gets the team combined rank.
     *
     * @return the teamCombinedRank
     */
    public Integer getTeamCombinedRank() {
        return teamCombinedRank;
    }

    public Integer getTeamCustomRank() {
        return teamCustomRank;
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
        return teamRobiRank;
    }

    /**
     * Gets the team sinclair rank.
     *
     * @return the teamSinclairRank
     */
    public Integer getTeamSinclairRank() {
        return teamSinclairRank;
    }

    /**
     * Gets the team snatch rank.
     *
     * @return the team snatch rank
     */
    public Integer getTeamSnatchRank() {
        return teamSnatchRank;
    }

    /**
     * Gets the team total rank.
     *
     * @return the team total rank
     */
    public Integer getTeamTotalRank() {
        return teamTotalRank;
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
        if (this.fullBirthDate != null) {
            return fullBirthDate.getYear();
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
                    (zeroIfInvalid(snatch1Declaration) > 0) &&
                    (zeroIfInvalid(snatch1Change1) == 0) &&
                    (zeroIfInvalid(snatch1Change2) == 0) &&
                    (zeroIfInvalid(snatch1ActualLift) == 0);
            changing = false;
            break;
        case 2:
            {
                int ap = zeroIfInvalid(getSnatch2AutomaticProgression());
                int decl = zeroIfInvalid(snatch2Declaration);
                declaring =
                        (ap > 0) &&
                        (decl > 0) &&
                        (zeroIfInvalid(snatch2Change1) == 0) &&
                        (zeroIfInvalid(snatch2Change2) == 0) &&
                        (zeroIfInvalid(snatch2ActualLift) == 0);
                changing = ap != decl;
            }
            break;
        case 3:
            {
                int ap = zeroIfInvalid(getSnatch3AutomaticProgression());
                int decl = zeroIfInvalid(snatch3Declaration);
                declaring =
                        (ap > 0) &&
                        (decl > 0) &&
                        (zeroIfInvalid(snatch3Change1) == 0) &&
                        (zeroIfInvalid(snatch3Change2) == 0) &&
                        (zeroIfInvalid(snatch3ActualLift) == 0);
                changing = ap != decl;

            }
            break;
        case 4:
            declaring =
                    (zeroIfInvalid(cleanJerk1Declaration) > 0) &&
                    (zeroIfInvalid(cleanJerk1Change1) == 0) &&
                    (zeroIfInvalid(cleanJerk1Change2) == 0) &&
                    (zeroIfInvalid(cleanJerk1ActualLift) == 0);
            changing = false;
            break;
        case 5:
            {
                int ap = zeroIfInvalid(getCleanJerk2AutomaticProgression());
                int decl = zeroIfInvalid(cleanJerk2Declaration);
                declaring =
                        (ap > 0) &&
                        (decl > 0) &&
                        (zeroIfInvalid(cleanJerk2Change1) == 0) &&
                        (zeroIfInvalid(cleanJerk2Change2) == 0) &&
                        (zeroIfInvalid(cleanJerk2ActualLift) == 0);
                changing = ap != decl;
            }
            break;
        case 6:
            {
                int ap = zeroIfInvalid(getCleanJerk3AutomaticProgression());
                int decl = zeroIfInvalid(cleanJerk3Declaration);
                declaring =
                        (ap > 0) &&
                        (decl > 0) &&
                        (zeroIfInvalid(cleanJerk3Change1) == 0) &&
                        (zeroIfInvalid(cleanJerk3Change2) == 0) &&
                        (zeroIfInvalid(cleanJerk3ActualLift) == 0);
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
        return eligibleForIndividualRanking;
    }

    public boolean isEligibleForTeamRanking() {
        return eligibleForTeamRanking;
    }

    /**
     * Checks if is forced as current.
     *
     * @return true, if is forced as current
     */
    @JsonIgnore
    public boolean isForcedAsCurrent() {
        return forcedAsCurrent;
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
        return validation && !isSkipValidationsDuringImport();
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
                .append(" category=" + (category != null ? category.getName().toLowerCase() : null))
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
        for (Iterator<Participation> iterator = participations.iterator(); iterator.hasNext();) {
            Participation participation = iterator.next();

            boolean athleteEqual = participation.getAthlete().equals(this);

            Category category2 = participation.getCategory();
            boolean categoryEqual = category2 != null && category2.getName().contentEquals(category.getName());
            if (athleteEqual && categoryEqual) {
                logger.trace("removeCategory removing {} {}", category, participation);
                iterator.remove();
                category2.getParticipations().remove(participation);
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
            // explicitly provided information, to be used if actual bodyweight is not yet known
            setPresumedBodyWeight(category.getMaximumWeight());
        }
        // the category is already from the eligible set
        // this.addEligibleCategory(category);
        // logger.trace("{}{} category {} {}", OwlcmsSession.getFopLoggingName(), System.identityHashCode(this),
        // category != null ? category.getParticipations() : null, LoggerUtils./**/stackTrace());
        this.category = category;
    }

    public void setCatSinclairRank(int i) {
        catSinclairRank = i;
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
        if ("0".equals(cleanJerk1Declaration)) {
            this.cleanJerk1Declaration = cleanJerk1Declaration;
            getLogger().info("{}{} cleanJerk1Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
                    cleanJerk1Declaration);
            setCleanJerk1ActualLift("0");
            return;
        }

        if (isValidation()) {
            validateDeclaration(1, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
                    cleanJerk1Change2, cleanJerk1ActualLift);
        }
        this.cleanJerk1Declaration = cleanJerk1Declaration;
//      if (zeroIfInvalid(getSnatch1Declaration()) > 0)
//          // validateStartingTotalsRule();

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

//        if (zeroIfInvalid(cleanJerk2ActualLift) == 0) {
//            this.setCleanJerk2LiftTime((LocalDateTime) null);
//        } else {
//            this.setCleanJerk2LiftTime(LocalDateTime.now());
//        }
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
            validateDeclaration(2, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                    cleanJerk2Change2, cleanJerk2ActualLift);
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

//        if (zeroIfInvalid(cleanJerk3ActualLift) == 0) {
//            this.setCleanJerk3LiftTime((null));
//        } else {
//            this.setCleanJerk3LiftTime(LocalDateTime.now());
//        }
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
            validateDeclaration(3, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                    cleanJerk3Change2, cleanJerk3ActualLift);
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
        combinedRank = i;
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

//  /**
//   * Sets the result order rank.
//   *
//   * @param resultOrderRank the result order rank
//   * @param rankingType     the ranking type
//   */
//  public void setResultOrderRank(Integer resultOrderRank, Ranking rankingType) {
//      this.resultOrderRank = resultOrderRank;
//  }

    public void setEligibleCategories(Set<Category> newEligibles) {
        logger.trace("athlete participations {}", getParticipations());
        Set<Category> oldEligibles = getEligibleCategories();
        logger.trace("setting eligible before:{} target:{}", oldEligibles, newEligibles);
        if (oldEligibles != null) {
            for (Category cat : oldEligibles) {
                removeEligibleCategory(cat);
            }
        }
        if (newEligibles != null) {
            for (Category cat : newEligibles) {
                addEligibleCategory(cat); // creates new join table entry, links from category as well.
            }
        }
        // logger.debug("{}{} {} after set eligible {}", OwlcmsSession.getFopLoggingName(),
        // System.identityHashCode(this), getShortName(),getEligibleCategories());
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

    /**
     * Sets the first name.
     *
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Sets the forced as current.
     *
     * @param forcedAsCurrent the new forced as current
     */
    public void setForcedAsCurrent(boolean forcedAsCurrent) {
        // logger.trace("setForcedAsCurrent({}) from {}", forcedAsCurrent, LoggerUtils.whereFrom());
        this.forcedAsCurrent = forcedAsCurrent;
    }

    /*
     * General event framework: we implement the com.vaadin.event.MethodEventSource interface which defines how a
     * notifier can call a method on a listener to signal that an event has occurred, and how the listener can
     * register/unregister itself.
     */

    /**
     * Sets the full birth date.
     *
     * @param fullBirthDate the fullBirthDate to set
     */
    public void setFullBirthDate(LocalDate fullBirthDate) {
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
        // and do NOT call setCategory (which would change the presumed body weight, something
        // we do NOT want.
        this.category = category;
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

    /**
     * Sets the sinclair rank.
     *
     * @param sinclairRank the new sinclair rank
     */
    public void setSinclairRank(Integer sinclairRank) {
        this.sinclairRank = sinclairRank;
    }

    public void setSmmRank(int i) {
        smmRank = i;

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
//        if (zeroIfInvalid(snatch1ActualLift) == 0) {
//            this.setSnatch1LiftTime(null);
//        } else {
//            this.setSnatch1LiftTime(LocalDateTime.now());
//        }
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
            validateSnatch1Change2(snatch1Change2);
        }
        this.snatch1Change2 = snatch1Change2;
        // validateStartingTotalsRule();

    }

    /**
     * Sets the snatch 1 declaration.
     *
     * @param snatch1Declaration the new snatch 1 declaration
     */
    public void setSnatch1Declaration(String snatch1Declaration) {
        if ("0".equals(snatch1Declaration)) {
            this.snatch1Declaration = snatch1Declaration;
            getLogger().info("{}{} snatch1Declaration={}", OwlcmsSession.getFopLoggingName(), this.getShortName(),
                    snatch1Declaration);
            setSnatch1ActualLift("0");
            return;
        }
        if (isValidation()) {
            validateDeclaration(1, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
                    snatch1ActualLift);
        }
        this.snatch1Declaration = snatch1Declaration;
//      if (zeroIfInvalid(getCleanJerk1Declaration()) > 0)
//          validateStartingTotalsRule();

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
//        if (zeroIfInvalid(snatch2ActualLift) == 0) {
//            this.setSnatch2LiftTime((LocalDateTime)null);
//        } else {
//            this.setSnatch2LiftTime(LocalDateTime.now());
//        }
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
            validateDeclaration(2, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                    snatch2ActualLift);
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
//        if (zeroIfInvalid(snatch3ActualLift) == 0) {
//            this.setSnatch3LiftTime((LocalDateTime)null);
//        } else {
//            this.setSnatch3LiftTime(LocalDateTime.now());
//        }
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

//    /**
//     * Sets the snatch rank.
//     *
//     * @param snatchRank the new snatch rank
//     */
//    public void setSnatchRank(Integer snatchRank) {
//        this.snatchRank = snatchRank;
//    }
//
//    public void setSnatchRankJr(Integer snatchRankJr) {
//        this.snatchRankJr = snatchRankJr;
//    }
//
//    public void setSnatchRankSr(Integer snatchRankSr) {
//        this.snatchRankSr = snatchRankSr;
//    }
//
//    public void setSnatchRankYth(Integer snatchRankYth) {
//        this.snatchRankYth = snatchRankYth;
//    }

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
            validateDeclaration(3, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                    snatch3ActualLift);
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

    /**
     * Sets the start number.
     *
     * @param startNumber the new start number
     */
    public void setStartNumber(Integer startNumber) {
        this.startNumber = startNumber;
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

    /**
     * Successful lift.
     */
    public void successfulLift() {
        OwlcmsSession.withFop(fop -> {
            try {
                getLogger().info("{}good lift for {}", OwlcmsSession.getFopLoggingName(), this.getShortName());
                final String weight = Integer.toString(getNextAttemptRequestedWeight());
                doLift(weight);
            } catch (Exception e) {
                getLogger().error(e.getLocalizedMessage());
            }
        });
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
        if (actualLift == null || actualLift.trim().length() == 0) {
            return; // allow reset of field.
        }

        int lastChange = last(zeroIfInvalid(automaticProgression), zeroIfInvalid(declaration),
                zeroIfInvalid(change1), zeroIfInvalid(change2));
        final int iAutomaticProgression = zeroIfInvalid(automaticProgression);
        final int liftedWeight = zeroIfInvalid(actualLift);

        getLogger().trace(
                "declaredChanges={} automaticProgression={} declaration={} change1={} change2={} liftedWeight={}",
                lastChange, automaticProgression, declaration, change1, change2, liftedWeight);
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
            return;
        } else {
            if (!lastChangeTooLow) {
                throw new RuleViolationException.LastChangeTooLow(this, curLift, lastChange, iAutomaticProgression);
            }
            if (!liftedWeightOk) {
                throw new RuleViolationException.LiftValueNotWhatWasRequested(this, curLift, actualLift, lastChange,
                        liftedWeight);
            }
            return;
        }
    }

    public boolean validateCleanJerk1ActualLift(String cleanJerk1ActualLift) throws RuleViolationException {
        validateActualLift(3, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
                cleanJerk1Change2, cleanJerk1ActualLift);
        return true;
    }

    public boolean validateCleanJerk1Change1(String cleanJerk1Change1) throws RuleViolationException {
        validateChange1(3, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
                cleanJerk1Change2, cleanJerk1ActualLift, false);
        validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
        return true;
    }

    public boolean validateCleanJerk1Change2(String cleanJerk1Change2) throws RuleViolationException {
        validateChange2(3, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
                cleanJerk1Change2, cleanJerk1ActualLift, false);
        validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
        return true;
    }

    public boolean validateCleanJerk1Declaration(String cleanJerk1Declaration) throws RuleViolationException {
        // always true
        return validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
    }

    public boolean validateCleanJerk2ActualLift(String cleanJerk2ActualLift) throws RuleViolationException {
        validateActualLift(4, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift);
        return true;
    }

    public boolean validateCleanJerk2Change1(String cleanJerk2Change1) throws RuleViolationException {
        validateChange1(4, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk2Change2(String cleanJerk2Change2) throws RuleViolationException {
        validateChange2(4, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk2Declaration(String cleanJerk2Declaration) throws RuleViolationException {
        validateDeclaration(4, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift);
        return true;
    }

    public boolean validateCleanJerk3ActualLift(String cleanJerk3ActualLift) throws RuleViolationException {
        validateActualLift(5, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift);
        // throws exception if invalid
        return true;
    }

    public boolean validateCleanJerk3Change1(String cleanJerk3Change1) throws RuleViolationException {
        validateChange1(5, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk3Change2(String cleanJerk3Change2) throws RuleViolationException {
        validateChange2(5, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk3Declaration(String cleanJerk3Declaration) throws RuleViolationException {
        validateDeclaration(5, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift);
        return true;
    }

    public boolean validateSnatch1ActualLift(String snatch1ActualLift) throws RuleViolationException {
        validateActualLift(0, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
                snatch1ActualLift);
        return true;
    }

    public boolean validateSnatch1Change1(String snatch1Change1) throws RuleViolationException {
        validateChange1(0, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
                snatch1ActualLift, true);
        validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
        return true;
    }

    public boolean validateSnatch1Change2(String snatch1Change2) throws RuleViolationException {
        validateChange2(0, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
                snatch1ActualLift, true);
        validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
        return true;
    }

    public boolean validateSnatch1Declaration(String snatch1Declaration) throws RuleViolationException {
        // always true
        return validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
    }

    public boolean validateSnatch2ActualLift(String snatch2ActualLift) throws RuleViolationException {
        validateActualLift(1, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                snatch2ActualLift);
        return true;
    }

    public boolean validateSnatch2Change1(String snatch2Change1) throws RuleViolationException {
        Level prevLevel = getLogger().getLevel();
        try {
            validateChange1(1, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                    snatch2ActualLift, true);
        } finally {
            getLogger().setLevel(prevLevel);
        }
        return true;
    }

    public boolean validateSnatch2Change2(String snatch2Change2) throws RuleViolationException {
        validateChange2(1, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                snatch2ActualLift, true);
        return true;
    }

    public boolean validateSnatch2Declaration(String snatch2Declaration) throws RuleViolationException {
        validateDeclaration(1, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                snatch2ActualLift);
        return true;
    }

    public boolean validateSnatch3ActualLift(String snatch3ActualLift) throws RuleViolationException {
        validateActualLift(2, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift);
        return true;
    }

    public boolean validateSnatch3Change1(String snatch3Change1) throws RuleViolationException {
        validateChange1(2, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift, true);
        return true;
    }

    public boolean validateSnatch3Change2(String snatch3Change2) throws RuleViolationException {
        validateChange2(2, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift, true);
        return true;
    }

    public boolean validateSnatch3Declaration(String snatch3Declaration) throws RuleViolationException {
        validateDeclaration(2, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift);
        return true;
    }

    /**
     * @param snatchDeclaration
     * @param cleanJerkDeclaration
     * @param entryTotal
     * @return true if ok, exception if not
     * @throws RuleViolationException if rule violated, exception contails details.
     */
    public boolean validateStartingTotalsRule(Integer snatch1Request, Integer cleanJerk1Request,
            int qualTotal) {
        boolean enforce20kg = Competition.getCurrent().isEnforce20kgRule();
        if (!enforce20kg) {
            return true;
        }

        int curStartingTotal = 0;

        curStartingTotal = snatch1Request + cleanJerk1Request;
        int delta = qualTotal - curStartingTotal;
        String message = null;
        int _20kgRuleValue = getStartingTotalMargin(this.getCategory(), qualTotal);

        getLogger().debug("{} validate20kgRule {} {} {}, {}, {}, {}", this, snatch1Request, cleanJerk1Request,
                curStartingTotal,
                qualTotal, delta, LoggerUtils.whereFrom());

        if (snatch1Request == 0 && cleanJerk1Request == 0) {
            getLogger().debug("not checking starting total - no declarations");
            return true;
        }
        RuleViolationException rule15_20Violated = null;
        int missing = delta - _20kgRuleValue;
        if (missing > 0) {
            // logger.debug("FAIL missing {}",missing);
            Integer startNumber2 = this.getStartNumber();
            rule15_20Violated = new RuleViolationException.Rule15_20Violated(this, this.getLastName(),
                    this.getFirstName(),
                    (startNumber2 != null ? startNumber2.toString() : "-"),
                    snatch1Request, cleanJerk1Request, missing, qualTotal);
            message = rule15_20Violated.getLocalizedMessage(OwlcmsSession.getLocale());
            getLogger().warn("{}{} {}", OwlcmsSession.getFopLoggingName(), this.getShortName(), message);
            throw rule15_20Violated;
        } else {
            getLogger().debug("OK margin={}", -(missing));
            return true;
        }
    }

    /**
     * @param entryTotal
     * @return true if ok, exception if not
     * @throws RuleViolationException if rule violated, exception contails details.
     */
    public boolean validateStartingTotalsRule(String snatch1Declaration, String snatch1Change1, String snatch1Change2,
            String cleanJerk1Declaration, String cleanJerk1Change1, String cleanJerk1Change2) {
        boolean enforce20kg = Competition.getCurrent().isEnforce20kgRule();
        int entryTotal = getEntryTotal();
        getLogger().trace("enforcing 20kg rule {} {}", enforce20kg, entryTotal);
        if (!enforce20kg || (entryTotal == 0)) {
            return true;
        }
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
        if (snatch1ActualLift != null && snatch1ActualLift.trim().isEmpty()) {
            setSnatch1ActualLift("0");
            setSnatch1LiftTime(null);
        }
        if (snatch2ActualLift != null && snatch2ActualLift.trim().isEmpty()) {
            setSnatch2ActualLift("0");
            setSnatch2LiftTime(null);
        }
        if (snatch3ActualLift != null && snatch3ActualLift.trim().isEmpty()) {
            setSnatch3ActualLift("0");
            setSnatch3LiftTime(null);
        }
        if (cleanJerk1ActualLift != null && cleanJerk1ActualLift.trim().isEmpty()) {
            setCleanJerk1ActualLift("0");
            setCleanJerk1LiftTime(null);
        }
        if (cleanJerk2ActualLift != null && cleanJerk2ActualLift.trim().isEmpty()) {
            setCleanJerk2ActualLift("0");
            setCleanJerk2LiftTime(null);
        }
        if (cleanJerk3ActualLift != null && cleanJerk3ActualLift.trim().isEmpty()) {
            setCleanJerk3ActualLift("0");
            setCleanJerk3LiftTime(null);
        }
    }

    /**
     * As integer.
     *
     * @param stringValue the string value
     * @return the integer
     */
    protected Integer asInteger(String stringValue) {
        if (stringValue == null) {
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
        Integer requestedWeight = newVal;
        int referenceWeight = reference.getWeight();
        int referenceAttemptNo = reference.getAttemptNo();// this is the lift that was attempted by previous lifter
        int currentLiftNo = getAttemptedLifts() + 1;
        int checkedLift = curLift + 1;
        if (checkedLift < currentLiftNo) {
            // we are checking an earlier attempt of the athlete (e.g. when loading the athlete card)
            logger.trace("ignoring lift {} {}", checkedLift, currentLiftNo);
            return;
        } else {
            logger.trace("checking lift {} {}", checkedLift, currentLiftNo);
        }

        if (requestedWeight > referenceWeight) {
            getLogger().debug("{}{} attempt {}: requested {} > previous {}", OwlcmsSession.getFopLoggingName(), this,
                    currentLiftNo,
                    requestedWeight,
                    referenceWeight);
            // lifting order is respected
            return;
        }
        if (referenceAttemptNo == 3 && currentLiftNo == 4) {
            getLogger().debug("{}start of CJ", OwlcmsSession.getFopLoggingName());
            // first attempt for C&J, no check
            return;
        }

        if (requestedWeight < referenceWeight) {
            getLogger().debug("{}requestedWeight {} < referenceWeight {}", OwlcmsSession.getFopLoggingName(),
                    requestedWeight,
                    referenceWeight);
            // someone has already lifted heavier previously
            if (requestedWeight > 0) {
                throw new RuleViolationException.WeightBelowAlreadyLifted(this, requestedWeight,
                        reference.getAthlete(), referenceWeight, referenceAttemptNo);
            }
        } else {
            checkSameWeightAsReference(reference, requestedWeight, referenceWeight, referenceAttemptNo, currentLiftNo);
        }
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
        Level prevLoggerLevel = getLogger().getLevel();
        if (Competition.getCurrent().isGenderOrder()) {
            return;
        }
        try {
            getLogger().setLevel(Level.DEBUG);
            doCheckChangeVsLiftOrder(curLift, newVal);
        } finally {
            getLogger().setLevel(prevLoggerLevel);
        }
    }

    private void checkChangeVsTimer(int curLift, String declaration, String change1, String change2) {
        Level prevLoggerLevel = getLogger().getLevel();
        if (Competition.getCurrent().isGenderOrder()) {
            return;
        }
        Integer attemptsDone = this.getAttemptsDone(); // 0..5
        if (curLift != attemptsDone) {
            return;
        }
        try {
            getLogger().setLevel(Level.DEBUG);
            doCheckChangeVsTimer(declaration, change1, change2);
        } finally {
            getLogger().setLevel(prevLoggerLevel);
        }
    }

//    @SuppressWarnings("unused")
//    private Long getCopyId() {
//        return copyId;
//    }

//    @SuppressWarnings("unused")
//    private Integer getDeclaredAndActuallyAttempted(Integer... items) {
//        int lastIndex = items.length - 1;
//        if (items.length == 0) {
//            return 0;
//        }
//        while (lastIndex >= 0) {
//            if (items[lastIndex] > 0) {
//                // if went down from declared weight, then return lower weight
//                return (items[lastIndex] < items[0] ? items[lastIndex] : items[0]);
//            }
//            lastIndex--;
//        }
//        return 0;
//    }

    private void checkDeclarationWasMade(int curLift, String declaration) {
        if (curLift != this.getAttemptsDone()) {
            return;
        }
        OwlcmsSession.withFop(fop -> {
            int clock = fop.getAthleteTimer().liveTimeRemaining();
            if (declaration == null || declaration.isBlank()) {
                // there was no declaration made in time
                logger./**/warn("{}{} change without declaration (not owning clock)", OwlcmsSession.getFopLoggingName(),
                        this.getShortName());
                throw new RuleViolationException.MustDeclareFirst(this, clock);
            }
        });
    }

    private void checkSameProgression(LiftOrderInfo reference, Integer requestedWeight, int currentProgression,
            int referenceProgression) {
        String fopLoggingName = OwlcmsSession.getFopLoggingName();
        getLogger().debug("{}currentProgression {} == referenceProgression {}", fopLoggingName, currentProgression,
                referenceProgression);
        if (this.getStartNumber() > 0) {
            // same weight, same attempt, allowed if start number is greater than previous lifter
            if (reference.getStartNumber() > this.getStartNumber()) {
                getLogger().debug("{}lastLift.getStartNumber() {} > this.getStartNumber() {}",
                        fopLoggingName, reference.getStartNumber(), this.getStartNumber());
                throw new RuleViolationException.StartNumberTooHigh(this, requestedWeight,
                        reference.getAthlete(), this);
            } else {
                getLogger().debug("{}lastLift.getStartNumber() {} <= this.getStartNumber() {}",
                        fopLoggingName, reference.getStartNumber(), this.getStartNumber());
            }
        } else {
            // no start number was attributed, try with lot number
            if (reference.getLotNumber() > this.getLotNumber()) {
                getLogger().debug("{}lastLift.getLotNumber() {} > this.getLotNumber() {}",
                        fopLoggingName, reference.getLotNumber(), this.getLotNumber());
                throw new RuleViolationException.LotNumberTooHigh(this, requestedWeight,
                        reference.getLotNumber(), this.getLotNumber());
            } else {
                getLogger().debug("{}lastLift.getLotNumber() {} <= this.getLotNumber() {}",
                        fopLoggingName, reference.getLotNumber(), this.getLotNumber());
            }
        }
    }

    private void checkSameWeightAsReference(LiftOrderInfo reference, Integer requestedWeight, int referenceWeight,
            int referenceAttemptNo, int currentLiftNo) {
        String fopLoggingName = OwlcmsSession.getFopLoggingName();
        getLogger().debug("{}requestedWeight {} == referenceWeight {}",
                fopLoggingName, requestedWeight, referenceWeight);
        // asking for same weight as previous lifter, cannot be a lower attempt number
        // Example we are asking for weight X on (say) first attempt, but someone else already
        // lifted that weight on second attempt. Too late, we are out of order.
        if (currentLiftNo < referenceAttemptNo) {
            getLogger().debug("{}currentLiftNo {} < prevAttemptNo {}",
                    fopLoggingName, currentLiftNo, referenceAttemptNo);
            throw new RuleViolationException.AttemptNumberTooLow(this, requestedWeight,
                    reference.getAthlete(), referenceWeight, 1 + (referenceAttemptNo - 1) % 3);
        } else if (currentLiftNo == referenceAttemptNo) {
            getLogger().debug("{}currentLiftNo {} == referenceAttemptNo {}",
                    fopLoggingName, currentLiftNo, referenceAttemptNo);
            int currentProgression = this.getProgression(requestedWeight);

            // BEWARE: referenceProgression if for current reference athlete and their prior attempt.
            // int referenceProgression = reference.getAthlete().getProgression(requestedWeight);
            int referenceProgression = reference.getProgression();

            if (currentProgression == referenceProgression) {
                checkSameProgression(reference, requestedWeight, currentProgression, referenceProgression);
            } else if (currentProgression > referenceProgression) {
                getLogger().debug("{}currentProgression {} > referenceProgression {}",
                        fopLoggingName, currentProgression, referenceProgression);
                // larger progression means a smaller previous attempt, hence lifted earlier than the last lift.
                // so we should already have lifted.
                getLogger().debug("{}{} lifted previous attempt earlier than {}, should already have done attempt",
                        fopLoggingName, reference.getAthlete().getShortName(), this.getShortName());
                throw new RuleViolationException.LiftedEarlier(this, requestedWeight, reference.getAthlete(),
                        this);
            } else {
                getLogger().debug("{}currentProgression {} < referenceProgression {}", fopLoggingName,
                        currentProgression, referenceProgression);
            }
        } else {
            // ok because does not change lift order.
            getLogger().debug("{}currentLiftNo {} > referenceAttemptNo {}",
                    fopLoggingName, currentLiftNo, referenceAttemptNo);
        }
    }

//    /**
//     * Null-safe comparison for longs.
//     *
//     * @param o1
//     * @param o2
//     * @return
//     */
//    private boolean LongEquals(Long o1, Long o2) {
//        return o1 == o2 || o1 != null && o2 != null && o1.longValue() == (o2.longValue());
//    }

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
            logger.debug("{}{} declaration accepted (not owning clock)", OwlcmsSession.getFopLoggingName(),
                    this.getShortName());
            return;
        } else {
            logger.debug("{}{} change accepted (not owning clock)", OwlcmsSession.getFopLoggingName(),
                    this.getShortName());
            return;
        }
    }

    private void doCheckChangeOwningTimer(String declaration, String change1, String change2, FieldOfPlay fop,
            int clock, int initialTime) {
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
            return;
        } else {
            if (clock < 30000) {
                logger./**/warn("{}{} late change denied after final warning ({})", OwlcmsSession.getFopLoggingName(),
                        this.getShortName(), clock / 1000.0);
                throw new RuleViolationException.MustChangeBeforeFinalWarning(this, clock);
            }
            logger.debug("{}change before final warning", OwlcmsSession.getFopLoggingName(), clock);
            return;
        }
    }

    private void doCheckChangeVsLiftOrder(int curLift, int newVal) throws RuleViolationException {

        int currentLiftNo = getAttemptedLifts() + 1;
        int checkedLift = curLift + 1;
        if (checkedLift < currentLiftNo) {
            // we are checking an earlier attempt of the athlete (e.g. when loading the athlete card)
            logger.trace("doCheckChangeVsLiftOrder ignoring lift {} {}", checkedLift, currentLiftNo);
            return;
        } else {
            logger.trace("doCheckChangeVsLiftOrder checking lift {} {}", checkedLift, currentLiftNo);
        }

        Object wi = OwlcmsSession.getAttribute("weighIn");
        String fopLoggingName = OwlcmsSession.getFopLoggingName();
        if (wi == this) {
            // current athlete being weighed in
            getLogger().trace("{}weighin {}", fopLoggingName, wi);
            return;
        } else {
            getLogger().trace("{}lifting", fopLoggingName);
        }
        OwlcmsSession.withFop(fop -> {
            Integer weightAtLastStart = fop.getWeightAtLastStart();
            if (weightAtLastStart == null || weightAtLastStart == 0 || newVal == weightAtLastStart) {
                // getLogger().debug("{}weight at last start: {} request = {}", fopLoggingName, weightAtLastStart,
                // newVal);
                // program has just been started, or first athlete in group, or moving down to clock value
                // compare with what the lifting order rules say.
                LiftOrderReconstruction pastOrder = new LiftOrderReconstruction(fop);
                LiftOrderInfo reference = null;

                Athlete clockOwner = fop.getClockOwner();
                if (clockOwner != null) {
                    // if clock is running, reference becomes the clock owner instead of last good/bad lift.
                    reference = clockOwner.getRunningLiftOrderInfo();
                    pastOrder.shortDump("lastLift info clock running", getLogger());
                } else {
                    reference = pastOrder.getLastLift();
                    // *******************************************
                    pastOrder.shortDump("lastLift info no clock", getLogger());
                }

                if (reference != null) {
                    checkAttemptVsLiftOrderReference(curLift, newVal, reference);
                } else {
                    // no last lift, go ahead
                }
            } else if (newVal > 0 && newVal < weightAtLastStart) {
                // check that we are comparing the value for the same lift
                boolean cjClock = fop.getLiftsDoneAtLastStart() >= 3;
                boolean cjStarted = getAttemptsDone() >= 3;
                logger.trace("newval {} weightAtLastStart {}", newVal, weightAtLastStart);
                logger.trace("lifts done at last start {} current lifts done {}", fop.getLiftsDoneAtLastStart(),
                        getAttemptsDone());
                if ((!cjClock && !cjStarted) || (cjStarted && cjClock)) {
                    throw new RuleViolationException.ValueBelowStartedClock(this, newVal, weightAtLastStart);
                }
            } else {
                // ok, nothing to do.
            }
        });
    }

    private void doCheckChangeVsTimer(String declaration, String change1, String change2) {
        Object wi = OwlcmsSession.getAttribute("weighIn");
        String fopLoggingName = OwlcmsSession.getFopLoggingName();
        if (wi == this) {
            // current athlete being weighed in
            getLogger().trace("{}weighin {}", fopLoggingName, wi);
            return;
        } else {
            getLogger().trace("{}lifting", fopLoggingName);
        }
        OwlcmsSession.withFop(fop -> {
            int clock = fop.getAthleteTimer().liveTimeRemaining();
            Athlete owner = fop.getClockOwner();
            int initialTime = fop.getClockOwnerInitialTimeAllowed();
            logger.debug("{}athlete={} owner={}, clock={}, initialTimeAllowed={}, d={}, c1={}, c2={}",
                    OwlcmsSession.getFopLoggingName(), this, owner,
                    clock, initialTime, declaration, change1, change2);
            if (!this.isSameAthleteAs(owner)) {
                // clock is not running for us
                doCheckChangeNotOwningTimer(declaration, change1, change2, fop, clock, initialTime);
                return;
            } else {
                doCheckChangeOwningTimer(declaration, change1, change2, fop, clock, initialTime);
            }
        });
    }

    private String emptyIfNull(String value) {
        return (value == null ? "" : value);
    }

    @Transient
    @JsonIgnore
    private int getProgression(Integer requestedWeight) {
        int attempt = getAttemptsDone() + 1;
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
    private LiftOrderInfo getRunningLiftOrderInfo() {
        LiftOrderInfo loi = new LiftOrderInfo();
        loi.setAthlete(this);
        Integer nextAttemptRequestedWeight = this.getNextAttemptRequestedWeight();
        loi.setWeight(nextAttemptRequestedWeight);
        // the clock is running for our current attempt, so done+1
        int attemptsDone = this.getAttemptsDone() + 1;
        loi.setAttemptNo(attemptsDone);
        loi.setProgression(this.getProgression(nextAttemptRequestedWeight));
        loi.setStartNumber(this.getStartNumber());
        loi.setLotNumber(this.getLotNumber());
        getLogger().debug("{}clockOwner: {}", OwlcmsSession.getFopLoggingName(), loi);
        return loi;
    }

    @Transient
    @JsonIgnore
    private Double getSinclair(Double bodyWeight1, Integer total1) {
        if (total1 == null || total1 < 0.1 || (gender == null)) {
            return 0.0;
        }
        if (gender == Gender.M) { // $NON-NLS-1$
            return total1 * sinclairFactor(bodyWeight1, SinclairCoefficients.menCoefficient(),
                    SinclairCoefficients.menMaxWeight());
        } else {
            return total1 * sinclairFactor(bodyWeight1, SinclairCoefficients.womenCoefficient(),
                    SinclairCoefficients.womenMaxWeight());
        }
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
                AgeDivision ad = ag.getAgeDivision();
                if (ad != null) {
                    if (ad == AgeDivision.MASTERS) {
                        double margin = 0.2D * entryTotal;
                        // we would round up the required total, so we round down the allowed margin
                        double floor = Math.floor(margin);
                        int asInt = (int) Math.round(floor);
                        getLogger().debug("margin = {} floor = {} asInt = {} required = {}", margin, floor, asInt,
                                entryTotal - asInt);
                        return asInt;
                    }
                }
            }
        }
        return 20;
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
                    // logger.debug(" removing {}", part);
                    iterator.remove();
                } else {
                    // logger.trace(" ok {} {}-{} {}-{}", part, athId, partAthId, catId, partCatId);
                }
            }
        }
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
            this.fullBirthDate = LocalDate.of(yearOfBirth, 1, 1);
        } else {
            this.fullBirthDate = null;
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

    /**
     * @param curLift
     * @param actualLift
     */
    private void validateChange1(int curLift, String automaticProgression, String declaration, String change1,
            String change2, String actualLift, boolean isSnatch) throws RuleViolationException {
        if (change1 == null || change1.trim().length() == 0) {
            return; // allow reset of field.
        }
        int newVal = zeroIfInvalid(change1);
        int prevVal = zeroIfInvalid(automaticProgression);
        if (newVal < prevVal) {
            throw new RuleViolationException.LastChangeTooLow(this, curLift, newVal, prevVal);
        }
        try {
            checkChangeVsTimer(curLift, declaration, change1, change2);
            checkDeclarationWasMade(curLift, declaration);
            checkChangeVsLiftOrder(curLift, newVal);
        } catch (RuleViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param curLift
     * @param actualLift
     */
    private void validateChange2(int curLift, String automaticProgression, String declaration, String change1,
            String change2, String actualLift, boolean isSnatch) throws RuleViolationException {
        if (change2 == null || change2.trim().length() == 0) {
            return; // allow reset of field.
        }
        int newVal = zeroIfInvalid(change2);
        int prevVal = zeroIfInvalid(automaticProgression);
        if (newVal < prevVal) {
            throw new RuleViolationException.LastChangeTooLow(this, curLift, newVal, prevVal);
        }
        try {
            checkChangeVsTimer(curLift, declaration, change1, change2);
            checkDeclarationWasMade(curLift, declaration);
            checkChangeVsLiftOrder(curLift, newVal);
        } catch (RuleViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param curLift
     * @param actualLift
     */
    private void validateDeclaration(int curLift, String automaticProgression, String declaration, String change1,
            String change2, String actualLift) throws RuleViolationException {
        getLogger().trace("{}{} validateDeclaration", OwlcmsSession.getFopLoggingName(), this, declaration);
        int newVal = zeroIfInvalid(declaration);
        int iAutomaticProgression = zeroIfInvalid(automaticProgression);
        // allow null declaration for reloading old results.
        if (iAutomaticProgression > 0 && newVal > 0 && newVal < iAutomaticProgression) {
            throw new RuleViolationException.DeclarationValueTooSmall(this, curLift, newVal, iAutomaticProgression);
        }
        try {
            checkChangeVsTimer(curLift, declaration, change1, change2);
            checkChangeVsLiftOrder(curLift, newVal);
        } catch (RuleViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
