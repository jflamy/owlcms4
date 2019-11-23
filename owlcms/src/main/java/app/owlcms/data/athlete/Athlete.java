/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.athlete;

import java.sql.Date;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.slf4j.LoggerFactory;

import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class stores all the information related to a particular participant.
 * <p>
 * This class is an example of what not to do. This was designed prior reaching
 * a proper understanding of Hibernate/JPA and of proper separation between
 * Vaadin Containers and persistence frameworks. Live and Learn.
 * <p>
 * All persistent properties are managed by Java Persistance annotations.
 * "Field" access mode is used, meaning that it is the values of the fields that
 * are stored, and not the values returned by the getters. Note that it is often
 * necessary to know when a value has been captured or not -- this is why values
 * are stored as Integers or Doubles, so that we can use null to indicate that a
 * value has not been captured.
 * </p>
 * <p>
 * This allows us to use the getters to return the values as they will be
 * displayed by the application
 * </p>
 * <p>
 * Computed fields are defined as final transient properties and marked
 * as @Transient; the only reason for this is so the JavaBeans introspection
 * mechanisms find them.
 * </p>
 * <p>
 * This class uses events to notify interested user interface components that
 * fields or computed values have changed. In this way the user interface does
 * not have to know that the category field on the screen is dependent on the
 * bodyweight and the gender -- all the dependency logic is kept at the business
 * object level.
 * </p>
 */
@Entity
@Cacheable
public class Athlete {
    private final static Logger logger = (Logger) LoggerFactory.getLogger(Athlete.class);
    private static final int YEAR = LocalDateTime.now().getYear();

    /**
     * Copy lift values to/from another athlete object used as editing scratchpad.
     *
     * The methods must be called in the proper order otherwise validation errors
     * will occur (e.g. actual lift must match last requested change)
     *
     * @param dest
     * @param src
     */
    public static void copy(Athlete dest, Athlete src) {
        boolean validation = dest.isValidation();
        try {
            dest.setValidation(false);
            dest.setLoggerLevel(Level.OFF);

            dest.setLastName(src.getLastName());
            dest.setFirstName(src.getFirstName());
            dest.setGroup(src.getGroup());
            dest.setStartNumber(src.getStartNumber());
            dest.setQualifyingTotal(src.getQualifyingTotal());

            dest.setSnatch1Declaration(src.getSnatch1Declaration());
            dest.setSnatch1Change1(src.getSnatch1Change1());
            dest.setSnatch1Change2(src.getSnatch1Change2());
            dest.setSnatch1ActualLift(src.getSnatch1ActualLift());

            dest.setSnatch2AutomaticProgression(src.getSnatch2AutomaticProgression());
            dest.setSnatch2Declaration(src.getSnatch2Declaration());
            dest.setSnatch2Change1(src.getSnatch2Change1());
            dest.setSnatch2Change2(src.getSnatch2Change2());
            dest.setSnatch2ActualLift(src.getSnatch2ActualLift());

            dest.setSnatch3AutomaticProgression(src.getSnatch3AutomaticProgression());
            dest.setSnatch3Declaration(src.getSnatch3Declaration());
            dest.setSnatch3Change1(src.getSnatch3Change1());
            dest.setSnatch3Change2(src.getSnatch3Change2());
            dest.setSnatch3ActualLift(src.getSnatch3ActualLift());

            dest.setCleanJerk1Declaration(src.getCleanJerk1Declaration());
            dest.setCleanJerk1Change1(src.getCleanJerk1Change1());
            dest.setCleanJerk1Change2(src.getCleanJerk1Change2());
            dest.setCleanJerk1ActualLift(src.getCleanJerk1ActualLift());

            dest.setCleanJerk2AutomaticProgression(src.getCleanJerk2AutomaticProgression());
            dest.setCleanJerk2Declaration(src.getCleanJerk2Declaration());
            dest.setCleanJerk2Change1(src.getCleanJerk2Change1());
            dest.setCleanJerk2Change2(src.getCleanJerk2Change2());
            dest.setCleanJerk2ActualLift(src.getCleanJerk2ActualLift());

            dest.setCleanJerk3AutomaticProgression(src.getCleanJerk3AutomaticProgression());
            dest.setCleanJerk3Declaration(src.getCleanJerk3Declaration());
            dest.setCleanJerk3Change1(src.getCleanJerk3Change1());
            dest.setCleanJerk3Change2(src.getCleanJerk3Change2());
            dest.setCleanJerk3ActualLift(src.getCleanJerk3ActualLift());

            dest.setForcedAsCurrent(src.getForcedAsCurrent());
        } finally {
            dest.setValidation(validation);
            dest.resetLoggerLevel();
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
     * @param a                    Athlete being validated
     * @param snatchDeclaration
     * @param cleanJerkDeclaration
     * @param entryTotal
     * @return true if ok, exception if not
     * @throws RuleViolationException if rule violated, exception contails details.
     */
    public static boolean validateStartingTotalsRule(Athlete a, Integer snatch1Request, Integer cleanJerk1Request,
            int qualTotal) {
        boolean enforce20kg = Competition.getCurrent().isEnforce20kgRule();
        if (!enforce20kg) {
            return true;
        }

        int curStartingTotal = 0;

        curStartingTotal = snatch1Request + cleanJerk1Request;
        int delta = qualTotal - curStartingTotal;
        String message = null;
        int _20kgRuleValue = a.get20kgRuleValue();

        logger.debug("{} validate20kgRule {} {} {}, {}, {}, {}", a, snatch1Request, cleanJerk1Request, curStartingTotal,
                qualTotal, delta, LoggerUtils.whereFrom());

        if (snatch1Request == 0 && cleanJerk1Request == 0) {
            logger.debug("not checking starting total - no declarations");
            return true;
        }
        RuleViolationException rule15_20Violated = null;
        int missing = delta - _20kgRuleValue;
        if (missing > 0) {
            // logger.debug("FAIL missing {}",missing);
            Integer startNumber2 = a.getStartNumber();
            rule15_20Violated = RuleViolation.rule15_20Violated(a.getLastName(), a.getFirstName(),
                    (startNumber2 != null ? startNumber2 : "-"), snatch1Request, cleanJerk1Request, missing, qualTotal);
            message = rule15_20Violated.getLocalizedMessage(OwlcmsSession.getLocale());
            logger.info("{} {}", a, message);
            throw rule15_20Violated;
        } else {
            logger.debug("OK margin={}", -(missing));
            return true;
        }
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

    private final Level NORMAL_LEVEL = Level.INFO;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long version;

    private Integer lotNumber = null;

    private Integer startNumber = null;

    private String firstName = "";

    // This is brute force, but having embedded classes does not bring much
    // and we don't want joins or other such logic for the Athlete card.
    // Since the Athlete card is 6 x 4 items, we take the simple route.

    // Note: we use Strings because we need to distinguish actually entered
    // values (such as 0)
    // from empty cells. Using Integers or Doubles would work as well, but many
    // people want to type
    // "-" or other things in the cells, so Strings are actually easier.

    private String lastName = "";
    private String team = "";
    private Gender gender = null; // $NON-NLS-1$
    private LocalDate fullBirthDate = null;

    private AgeDivision ageDivision = null;
    private Double bodyWeight = null;
    private String membership = "";

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH }, optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_group", nullable = true)
    private Group group;
    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_categ", nullable = true)
    private Category category = null;
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
    private Integer snatchRank;
    private Integer cleanJerkRank;
    private Integer totalRank;
    private Integer sinclairRank;

    private Integer robiRank;
    private Integer customRank;
    private Float snatchPoints;
    private Float cleanJerkPoints;

    private Float totalPoints; // points based on totalRank
    private Float sinclairPoints;

    private Float customPoints;
    private Integer teamSinclairRank;
    private Integer teamRobiRank;
    private Integer teamSnatchRank;
    private Integer teamCleanJerkRank;
    private Integer teamTotalRank;

    private Integer teamCombinedRank;
    private Integer qualifyingTotal = 0;
    private Double customScore;
    private boolean eligibleForIndividualRanking = true;

    private boolean eligibleForTeamRanking = true;

    /*
     * Non-persistent properties. These properties are used during computations, but
     * need not be stored in the database
     */
    @Transient
    Integer liftOrderRank = 0;

    /** The forced as current. */
    @Transient
    boolean forcedAsCurrent = false;

    @Transient
    private boolean validation = true;

    DecimalFormat df = null;

    /**
     * Instantiates a new athlete.
     */
    public Athlete() {
        super();
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

    public void clearLifts() {
        String cj1Decl = this.getCleanJerk1Declaration();
        String sn1Decl = this.getSnatch1Declaration();
        boolean validate = this.isValidation();
        try {
            this.setValidation(false);
            this.setLoggerLevel(Level.OFF);

            this.setCleanJerk1Declaration("");
            this.setCleanJerk1AutomaticProgression("");
            this.setCleanJerk1Change1("");
            this.setCleanJerk1Change2("");
            this.setCleanJerk1ActualLift("");

            this.setCleanJerk2Declaration("");
            this.setCleanJerk2AutomaticProgression("");
            this.setCleanJerk2Change1("");
            this.setCleanJerk2Change2("");
            this.setCleanJerk2ActualLift("");

            this.setCleanJerk3Declaration("");
            this.setCleanJerk3AutomaticProgression("");
            this.setCleanJerk3Change1("");
            this.setCleanJerk3Change2("");
            this.setCleanJerk3ActualLift("");

            this.setSnatch1Declaration("");
            this.setSnatch1AutomaticProgression("");
            this.setSnatch1Change1("");
            this.setSnatch1Change2("");
            this.setSnatch1ActualLift("");

            this.setSnatch2Declaration("");
            this.setSnatch2AutomaticProgression("");
            this.setSnatch2Change1("");
            this.setSnatch2Change2("");
            this.setSnatch2ActualLift("");

            this.setSnatch3Declaration("");
            this.setSnatch3AutomaticProgression("");
            this.setSnatch3Change1("");
            this.setSnatch3Change2("");
            this.setSnatch3ActualLift("");

            this.setSnatch1Declaration(sn1Decl);
            this.setCleanJerk1Declaration(cj1Decl);
        } finally {
            this.setValidation(validate);
            this.resetLoggerLevel();
        }
    }

//	/**
//	 * @param entryTotal
//	 * @return true if ok, exception if not
//	 * @throws RuleViolationException if rule violated, exception contails details.
//	 */
//	private boolean validateMasters15_20Rule(int entryTotal) throws RuleViolationException {
//		int curStartingTotal = 0;
//		int snatch1request = 0;
//		int cleanJerkRequest = 0;
//
//		snatch1request = last(
//			zeroIfInvalid(snatch1Declaration),
//			zeroIfInvalid(snatch1Change1),
//			zeroIfInvalid(snatch1Change2));
//		cleanJerkRequest = last(
//			zeroIfInvalid(cleanJerk1Declaration),
//			zeroIfInvalid(cleanJerk1Change1),
//			zeroIfInvalid(cleanJerk1Change2));
//
//		int _20kgRuleValue = this.get20kgRuleValue();
////		int bestSnatch1 = getBestSnatch();
//		// example: male 55/65 declarations given 135 qual total (120 within 15kg of 135, ok)
//		// athlete does 70 snatch, which is bigger than 15kg gap.
//		// can now declare 50 opening CJ according to 2.4.3
//		curStartingTotal = snatch1request + cleanJerkRequest; // 120
//		int delta = entryTotal - curStartingTotal; // 15 -- no margin of error
//
////		int curForecast = bestSnatch1 + zeroIfInvalid(cleanJerk1Declaration); // 70 + 65 = 135
////		if (curForecast >= qualTotal) {
////			// already predicted to clear the QT, may change the CJ request down.
////			logger.info("forecast = {}", curForecast);
////			delta = qualTotal - (bestSnatch1 + cleanJerkRequest); // delta = 135 - 135 = 0
////			snatch1request = bestSnatch1;
////			// possible CJ initial request reduction = _20kgRuleValue - delta
////			// can bring CJ down to 50 (15 - 0)
////		}
//
//		return doCheckRule(entryTotal, snatch1request, cleanJerkRequest, _20kgRuleValue, delta);
//	}

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

    private Integer doGetAgeGroup(Integer yob) {
        if (yob == null) {
            yob = 1900;
        }
        Gender gender2 = this.getGender();
        if (gender2 == null) {
            gender2 = Gender.F;
        }
        int year1 = Calendar.getInstance().get(Calendar.YEAR);
        final int age = year1 - yob;
        if (age <= 17) {
            return 17;
        } else if (age <= 20) {
            return 20;
        } else if (age < 35) {
            return 34;
        }
        int ageGroup1 = (int) (Math.ceil(age / 5) * 5);

        if (Gender.F == gender2 && ageGroup1 >= 70) { // $NON-NLS-1$
            return 70;
        }
        if (Gender.M == gender2 && ageGroup1 >= 80) { // $NON-NLS-1$
            return 80;
        }
        // normal case
        return ageGroup1;
    }

    /**
     * @param Athlete
     * @param athletes
     * @param weight
     */
    private void doLift(final String weight) {
        switch (this.getAttemptsDone() + 1) {
        case 1:
            this.setSnatch1ActualLift(weight);
            break;
        case 2:
            this.setSnatch2ActualLift(weight);
            break;
        case 3:
            this.setSnatch3ActualLift(weight);
            break;
        case 4:
            this.setCleanJerk1ActualLift(weight);
            break;
        case 5:
            this.setCleanJerk2ActualLift(weight);
            break;
        case 6:
            this.setCleanJerk3ActualLift(weight);
            break;
        }
    }

    private String emptyIfNull(String value) {
        return (value == null ? "" : value);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Athlete other = (Athlete) obj;

        if (fullBirthDate == null) {
            if (other.fullBirthDate != null) {
                return false;
            }
        } else if (!fullBirthDate.equals(other.fullBirthDate)) {
            return false;
        }

        if (firstName == null) {
            if (other.firstName != null) {
                return false;
            }
        } else if (!firstName.equals(other.firstName)) {
            return false;
        }

        if (lastName == null) {
            if (other.lastName != null) {
                return false;
            }
        } else if (!lastName.equals(other.lastName)) {
            return false;
        }

        if (team == null) {
            if (other.team != null) {
                return false;
            }
        } else if (!team.equals(other.team)) {
            return false;
        }

        if (gender == null) {
            if (other.gender != null) {
                return false;
            }
        } else if (!gender.equals(other.gender)) {
            return false;
        }

        if (membership == null) {
            if (other.membership != null) {
                return false;
            }
        } else if (!membership.equals(other.membership)) {
            return false;
        }

        return true;
    }

    /**
     * Failed lift.
     */
    public void failedLift() {
        try {
            logger.info("no lift for {}", this);
            final String weight = Integer.toString(-getNextAttemptRequestedWeight());
            doLift(weight);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    /**
     * Gets the 20 kg rule value.
     *
     * @return the 20 kg rule value
     */
    public int get20kgRuleValue() {
        if (Competition.getCurrent().isMasters()) {
            if (Gender.M.equals(this.getGender())) {
                return 15;
            } else {
                return 10;
            }
        } else {
            return 20;
        }

    }

    public AgeDivision getAgeDivision() {
        return ageDivision;
    }

    /**
     * Gets the age group.
     *
     * @return the ageGroup. M80 if male missing birth date, F70 if female missing
     *         birth date or missing both gender and birth.
     */
    public Integer getAgeGroup() {
        Integer yob = this.getYearOfBirth();
        return doGetAgeGroup(yob);
    }

    /**
     * Gets the attempted lifts.
     *
     * @return the attempted lifts
     */
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
    public Integer getAttemptNumber() {
        return getAttemptsDone() % 3 + 1;
    }

    /**
     * Gets the attempts done.
     *
     * @return the attemptsDone
     */
    public Integer getAttemptsDone() {
        return getSnatchAttemptsDone() + getCleanJerkAttemptsDone();
    }

    /**
     * Gets the best clean jerk.
     *
     * @return the bestCleanJerk
     */
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
     * @deprecated use
     */
    @Deprecated
    @Transient
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
     * Gets the category.
     *
     * @return the category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Compute the body weight at the maximum weight of the Athlete's category.
     * Note: for the purpose of this computation, only "official" categories are
     * used as the purpose is to totalRank athletes according to their competition
     * potential.
     *
     * @return the category sinclair
     */
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
    public Float getCleanJerkPoints() {
        if (cleanJerkPoints == null) {
            return 0.0F;
        }
        return cleanJerkPoints;
    }

    /**
     * Gets the clean jerk rank.
     *
     * @return the clean jerk rank
     */
    public Integer getCleanJerkRank() {
        return cleanJerkRank;
    }

    /**
     * Gets the clean jerk total.
     *
     * @return total for clean and jerk
     */
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

    /**
     * Gets the combined points.
     *
     * @return the combined points
     */
    public Float getCombinedPoints() {
        return getSnatchPoints() + getCleanJerkPoints() + getTotalPoints();
    }

    /**
     * Gets the current automatic.
     *
     * @return the current automatic
     */
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
     * Gets the custom points.
     *
     * @return the customPoints
     */
    public Float getCustomPoints() {
        return customPoints;
    }

    /**
     * Gets the custom rank.
     *
     * @return the custom rank
     */
    public Integer getCustomRank() {
        return this.customRank;
    }

    /**
     * Gets the custom score.
     *
     * @return the custom score
     */
    public Double getCustomScore() {
        if (customScore == null || customScore < 0.01) {
            return Double.valueOf(getTotal());
        }
        return customScore;
    }

    @SuppressWarnings("unused")
    private Integer getDeclaredAndActuallyAttempted(Integer... items) {
        int lastIndex = items.length - 1;
        if (items.length == 0) {
            return 0;
        }
        while (lastIndex >= 0) {
            if (items[lastIndex] > 0) {
                // if went down from declared weight, then return lower weight
                return (items[lastIndex] < items[0] ? items[lastIndex] : items[0]);
            }
            lastIndex--;
        }
        return 0;
    }

    /**
     * Gets the display category.
     *
     * @return the display category
     */
    public String getDisplayCategory() {
        if (Competition.getCurrent().isMasters()) {
            return getShortCategory();
        } else {
            return getLongCategory();
        }
    }

    /**
     * Gets the first attempted lift time.
     *
     * @return the first attempted lift time
     */
    public LocalDateTime getFirstAttemptedLiftTime() {
        if (zeroIfInvalid(snatch1ActualLift) != 0) {
            return getSnatch1LiftTime();
        }
        if (zeroIfInvalid(snatch2ActualLift) != 0) {
            return getSnatch2LiftTime();
        }
        if (zeroIfInvalid(snatch3ActualLift) != 0) {
            return getSnatch3LiftTime();
        }
        if (zeroIfInvalid(cleanJerk1ActualLift) != 0) {
            return getCleanJerk1LiftTime();
        }
        if (zeroIfInvalid(cleanJerk2ActualLift) != 0) {
            return getCleanJerk2LiftTime();
        }
        if (zeroIfInvalid(cleanJerk3ActualLift) != 0) {
            return getCleanJerk3LiftTime();
        }
        return LocalDateTime.MAX; // forever in the future
    }

    /**
     * Gets the first name.
     *
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Gets the forced as current.
     *
     * @return the forced as current
     */
    public boolean getForcedAsCurrent() {
        return forcedAsCurrent;
    }

    public String getFormattedBirth() {
        if (Competition.getCurrent().isUseBirthYear()) {
            return getYearOfBirth().toString();
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

    public String getFullId() {
        String fullName = getFullName();
        Category category2 = getCategory();
        if (!fullName.isEmpty()) {
            return fullName + " " + (category2 != null ? category2 : "");
//				+(startNumber2 != null && startNumber2 >0 ? " ["+startNumber2+"]" : "");
        } else {
            return "";
        }
    }

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
    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets the long category.
     *
     * @return the long category
     */
    public String getLongCategory() {
        if (Competition.getCurrent().isUseRegistrationCategory()) {
            Category registrationCategory2 = getRegistrationCategory();
            if (registrationCategory2 == null) {
                return "?";
            }
            return registrationCategory2.getName();
        } else if (Competition.getCurrent().isMasters()) {
            return getMastersLongCategory();
        } else {
            Category category = getCategory();
            return (category != null ? category.getName() : "");
        }
    }

    /**
     * Gets the lot number.
     *
     * @return the lotNumber
     */
    public Integer getLotNumber() {
        return (lotNumber == null ? 0 : lotNumber);
    }

    /**
     * Gets the masters age group.
     *
     * @return the masters age group
     */
    public String getMastersAgeGroup() {
        if (this.getGender() == null) {
            return "";
        }
        String gender1 = getGender().name();
        Integer yob = this.getYearOfBirth();
        return getMastersAgeGroup(gender1, yob);
    }

    /**
     * @param gender1
     * @param yob
     * @return
     */
    public String getMastersAgeGroup(String gender1, Integer yob) {
        Integer ageGroup1;
        ageGroup1 = doGetAgeGroup(yob);

        String agePlus = "";
        if ("M".equals(gender1) && ageGroup1 == 80) {
            agePlus = "+";
        } else if ("F".equals(gender1) && ageGroup1 == 70) {
            agePlus = "+";
        } else if (ageGroup1 == 17) {
            agePlus = "-";
        } else if (ageGroup1 == 20) {
            agePlus = "-";
        } else if (ageGroup1 == 34) {
            ageGroup1 = 21;
        }

        final String mastersAgeCategory = ("F".equals(gender1) ? "W" : "M") + ageGroup1 + agePlus;
        return mastersAgeCategory;
    }

    /**
     * Gets the masters age group interval.
     *
     * @return the ageGroup
     */
    public String getMastersAgeGroupInterval() {
        Integer ageGroup1 = getAgeGroup();
        if (this.getGender() == Gender.F && ageGroup1 >= 70) { // $NON-NLS-1$
            return "70+";
        }
        if (this.getGender() == Gender.M && ageGroup1 >= 80) { // $NON-NLS-1$
            return "80+";
        }
        if (ageGroup1 == 17) {
            return "0-17";
        } else if (ageGroup1 == 20) {
            return "18-20";
        } else if (ageGroup1 == 34) {
            return "21+";
        }
        return ageGroup1 + "-" + (ageGroup1 + 4);
    }

    /**
     * Gets the masters gender age group interval.
     *
     * @return the masters gender age group interval
     */
    public String getMastersGenderAgeGroupInterval() {
        String gender2 = getGender().name();
        if (gender2 == "F") {
            gender2 = "W";
        }
        return gender2.toUpperCase() + getMastersAgeGroupInterval();
    }

    /**
     * Gets the masters long category.
     *
     * @return the masters long category
     */
    public String getMastersLongCategory() {
        String catString;
        String gender1 = getGender().name();
        final String mastersAgeCategory = getMastersAgeGroup(gender1, this.getYearOfBirth());
        final String shortCategory = getShortCategory(gender1);
        catString = mastersAgeCategory + " " + shortCategory;
        return catString;
    }

    /**
     * Gets the masters long registration category name.
     *
     * @return the masters long registration category name
     */
    public String getMastersLongRegistrationCategoryName() {
        String catString;
        String gender1 = getGender().name();
        final String mastersAgeCategory = getMastersAgeGroup(gender1, this.getYearOfBirth());
        final String shortCategory = getShortRegistrationCategory(gender1);
        catString = mastersAgeCategory + " " + shortCategory;
        return catString;
    }

    /**
     * Gets the medal rank.
     *
     * @return the medal rank
     */
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
    public Integer getNextAttemptRequestedWeight() {
        int attempt = getAttemptsDone() + 1;
        return getRequestedWeightForAttempt(attempt);
    }

    /**
     * Compute the time of last lift for Athlete. Times are only compared within the
     * same lift type (if a Athlete is at the first attempt of clean and jerk, then
     * the last lift occurred forever ago.)
     *
     * @return null if Athlete has not lifted
     */
    public LocalDateTime getPreviousLiftTime() {
        LocalDateTime max = null; // long ago

        if (getAttemptsDone() <= 3) {
            final LocalDateTime sn1 = snatch1LiftTime;
            if (sn1 != null) {
                max = sn1;
            } else {
                return max;
            }
            final LocalDateTime sn2 = snatch2LiftTime;
            if (sn2 != null) {
                max = (max.isAfter(sn2) ? max : sn2);
            } else {
                return max;
            }
            final LocalDateTime sn3 = snatch3LiftTime;
            if (sn3 != null) {
                max = (max.isAfter(sn3) ? max : sn3);
            } else {
                return max;
            }
        } else {
            final LocalDateTime cj1 = cleanJerk1LiftTime;
            if (cj1 != null) {
                max = cj1;
            } else {
                return max;
            }
            final LocalDateTime cj2 = cleanJerk2LiftTime;
            if (cj2 != null) {
                max = (max.isAfter(cj2) ? max : cj2);
            } else {
                return max;
            }
            final LocalDateTime cj3 = cleanJerk3LiftTime;
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
    public Integer getRank() {
        return totalRank;
    }

    /**
     * Gets the registration category.
     *
     * @return the registration category
     */
    public Category getRegistrationCategory() {
        return category;
    }

    /**
     * Gets the requested weight for attempt.
     *
     * @param attempt the attempt
     * @return the requested weight for attempt
     */
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
     * Gets the robi.
     *
     * @return the robi
     */
    public Double getRobi() {
        Category c;
        if (Competition.getCurrent().isUseRegistrationCategory()) {
            c = getRegistrationCategory();
        } else {
            c = getCategory();
        }

        if (c == null) {
            return 0.0;
        }
        if (c.getWr() == null || c.getWr() == 0) {
            return 0.0;
        }
        if (c.getRobiA() == null || c.getWr() <= 0.000001) {
            return 0.0;
        }
        double robi = c.getRobiA() * Math.pow(getTotal(), c.getRobiB());
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

    public String getRoundedBodyWeight() {
        if (df == null) {
            df = new DecimalFormat("#.##");
        }
        return df.format(getBodyWeight());
    }

    /**
     * Create a category acronym without gender.
     *
     * @return the short category
     */
    public String getShortCategory() {
        String gender1 = getGender().name();
        return getShortCategory(gender1);
    }

    /**
     * Create a category acronym without gender.
     *
     * @param gender1 the gender 1
     * @return the short category
     */
    public String getShortCategory(String gender1) {
        final Category category = getCategory();
        if (category == null) {
            return "";
        }

        if (Competition.getCurrent().isUseRegistrationCategory()) {
            return getShortRegistrationCategory(gender1);
        }

        String shortCategory = category.getName();
        int gtPos = shortCategory.indexOf(">");
        if (gtPos > 0) {
            return shortCategory.substring(gtPos);
        } else {
            return shortCategory.substring(1);
        }
    }

    /**
     * Gets the short registration category.
     *
     * @param gender1 the gender 1
     * @return registration category stripped of gender prefix.
     */
    public String getShortRegistrationCategory(String gender1) {
        final Category category = getRegistrationCategory();
        if (category == null) {
            return "?";
        }

        String shortCategory = category.getName();
        int gtPos = shortCategory.indexOf(">");
        if (gtPos > 0) {
            return shortCategory.substring(gtPos);
        } else {
            return shortCategory.substring(1);
        }
    }

    /**
     * Compute the Sinclair total for the Athlete, that is, the total multiplied by
     * a value that depends on the Athlete's body weight. This value extrapolates
     * what the Athlete would have lifted if he/she had the bodymass of a
     * maximum-weight Athlete.
     *
     * @return the sinclair-adjusted value for the Athlete
     */
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
    public Double getSinclair(Double bodyWeight1) {
        Integer total1 = getTotal();
        return getSinclair(bodyWeight1, total1);
    }

    private Double getSinclair(Double bodyWeight1, Integer total1) {
        if (total1 == null || total1 < 0.1) {
            return 0.0;
        }
        if (gender == null) {
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
     * Gets the sinclair factor.
     *
     * @return the sinclair factor
     */
    public Double getSinclairFactor() {
        if (gender == Gender.M) { // $NON-NLS-1$
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
    public Double getSinclairForDelta() {
        final Double bodyWeight1 = getBodyWeight();
        if (bodyWeight1 == null) {
            return 0.0;
        }
        Integer total1 = getBestCleanJerk() + getBestSnatch();
        return getSinclair(bodyWeight1, total1);
    }

    /**
     * Gets the sinclair points.
     *
     * @return the sinclairPoints
     */
    public Float getSinclairPoints() {
        return sinclairPoints;
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
    public Double getSmm() {
        final Integer birthDate1 = getYearOfBirth();
        if (birthDate1 == null) {
            return 0.0;
        }
        return getSinclair() * SinclairCoefficients.getSMMCoefficient(YEAR - birthDate1);
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
    public Float getSnatchPoints() {
        if (snatchPoints == null) {
            return 0.0F;
        }
        return snatchPoints;
    }

    /**
     * Gets the snatch rank.
     *
     * @return the snatch rank
     */
    public Integer getSnatchRank() {
        return snatchRank;
    }

    /**
     * Gets the snatch total.
     *
     * @return total for snatch.
     */
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
        return startNumber;
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

    /**
     * Gets the team member.
     *
     * @return the team member
     */
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
     * Total is zero if all three snatches or all three clean&jerks are failed.
     * Failed lifts are indicated as negative amounts. Total is the sum of all good
     * lifts otherwise. Null entries indicate that no data has been captured, and
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
    public Float getTotalPoints() {
        if (totalPoints == null) {
            return 0.0F;
        }
        return totalPoints;
    }

    /**
     * Gets the total rank.
     *
     * @return the total rank
     */
    public Integer getTotalRank() {
        return totalRank;
    }

    /**
     * Gets the version.
     *
     * @return the version
     */
    @Version
    public Long getVersion() {
        return version;
    }

    /**
     * Gets the year of birth.
     *
     * @return the year of birth (1900 if both birthDate and fullBirthDate are null)
     */
    public Integer getYearOfBirth() {
        if (this.fullBirthDate != null) {
            return fullBirthDate.getYear();
        } else {
            return 1900;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((team == null) ? 0 : team.hashCode());
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        return result;
    }

    /**
     * Checks if is a team member.
     *
     * @return true, if is a team member
     */
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
    public boolean isForcedAsCurrent() {
        return forcedAsCurrent;
    }

    /**
     * Checks if is invited.
     *
     * @return true, if is invited
     */
    public boolean isInvited() {
        return !isEligibleForIndividualRanking();
    }

    public boolean isValidation() {
        return validation;
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
     * Reset forced as current.
     */
    public void resetForcedAsCurrent() {
        this.forcedAsCurrent = false;
    }

    public void resetLoggerLevel() {
        logger.setLevel(NORMAL_LEVEL);
    }

    public void setAgeDivision(AgeDivision ageDivision) {
        this.ageDivision = ageDivision;
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
        this.category = category;
    }

    /**
     * Sets the clean jerk 1 actual lift.
     *
     * @param cleanJerk1ActualLift the new clean jerk 1 actual lift
     */
    public void setCleanJerk1ActualLift(String cleanJerk1ActualLift) {
        if (validation) {
            validateCleanJerk1ActualLift(cleanJerk1ActualLift);
        }
        this.cleanJerk1ActualLift = cleanJerk1ActualLift;
        logger.info("{} cleanJerk1ActualLift={}", this, cleanJerk1ActualLift);
        if (zeroIfInvalid(cleanJerk1ActualLift) == 0) {
            this.cleanJerk1LiftTime = (null);
        } else {
            this.cleanJerk1LiftTime = sqlNow();
        }

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
            logger.info("{} cleanJerk1Change1={}", this, cleanJerk1Change1);
            setCleanJerk1ActualLift("0");
            return;
        }
        if (validation) {
            validateCleanJerk1Change1(cleanJerk1Change1);
        }
        this.cleanJerk1Change1 = cleanJerk1Change1;
        // validateStartingTotalsRule();

        logger.info("{} cleanJerk1Change1={}", this, cleanJerk1Change1);
    }

    /**
     * Sets the clean jerk 1 change 2.
     *
     * @param cleanJerk1Change2 the new clean jerk 1 change 2
     */
    public void setCleanJerk1Change2(String cleanJerk1Change2) {
        if ("0".equals(cleanJerk1Change2)) {
            this.cleanJerk1Change2 = cleanJerk1Change2;
            logger.info("{} cleanJerk1Change2={}", this, cleanJerk1Change2);
            setCleanJerk1ActualLift("0");
            return;
        }
        if (validation) {
            validateCleanJerk1Change2(cleanJerk1Change2);
        }
        this.cleanJerk1Change2 = cleanJerk1Change2;
        // validateStartingTotalsRule();

        logger.info("{} cleanJerk1Change2={}", this, cleanJerk1Change2);
    }

    /**
     * Sets the clean jerk 1 declaration.
     *
     * @param cleanJerk1Declaration the new clean jerk 1 declaration
     */
    public void setCleanJerk1Declaration(String cleanJerk1Declaration) {
        if ("0".equals(cleanJerk1Declaration)) {
            this.cleanJerk1Declaration = cleanJerk1Declaration;
            logger.info("{} cleanJerk1Declaration={}", this, cleanJerk1Declaration);
            setCleanJerk1ActualLift("0");
            return;
        }

        if (validation) {
            validateDeclaration(1, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
                    cleanJerk1Change2, cleanJerk1ActualLift);
        }
        this.cleanJerk1Declaration = cleanJerk1Declaration;
//		if (zeroIfInvalid(getSnatch1Declaration()) > 0)
//			// validateStartingTotalsRule();

        logger.info("{} cleanJerk1Declaration={}", this, cleanJerk1Declaration);
    }

    /**
     * Sets the clean jerk 1 lift time.
     *
     * @param date the new clean jerk 1 lift time
     */
    public void setCleanJerk1LiftTime(java.util.Date date) {
    }

    /**
     * Sets the clean jerk 2 actual lift.
     *
     * @param cleanJerk2ActualLift the new clean jerk 2 actual lift
     */
    public void setCleanJerk2ActualLift(String cleanJerk2ActualLift) {
        if (validation) {
            validateCleanJerk2ActualLift(cleanJerk2ActualLift);
        }
        this.cleanJerk2ActualLift = cleanJerk2ActualLift;
        logger.info("{} cleanJerk2ActualLift={}", this, cleanJerk2ActualLift);

        if (zeroIfInvalid(cleanJerk2ActualLift) == 0) {
            this.cleanJerk2LiftTime = (null);
        } else {
            this.cleanJerk2LiftTime = sqlNow();
        }
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
            logger.info("{} cleanJerk2Change1={}", this, cleanJerk2Change1);
            setCleanJerk2ActualLift("0");
            return;
        }
        if (validation) {
            validateCleanJerk2Change1(cleanJerk2Change1);
        }
        this.cleanJerk2Change1 = cleanJerk2Change1;
        logger.info("{} cleanJerk2Change1={}", this, cleanJerk2Change1);
    }

    /**
     * Sets the clean jerk 2 change 2.
     *
     * @param cleanJerk2Change2 the new clean jerk 2 change 2
     */
    public void setCleanJerk2Change2(String cleanJerk2Change2) {
        if ("0".equals(cleanJerk2Change2)) {
            this.cleanJerk2Change2 = cleanJerk2Change2;
            logger.info("{} cleanJerk2Change2={}", this, cleanJerk2Change2);
            setCleanJerk2ActualLift("0");
            return;
        }
        if (validation) {
            validateCleanJerk2Change2(cleanJerk2Change2);
        }
        this.cleanJerk2Change2 = cleanJerk2Change2;
        logger.info("{} cleanJerk2Change2={}", this, cleanJerk2Change2);
    }

    /**
     * Sets the clean jerk 2 declaration.
     *
     * @param cleanJerk2Declaration the new clean jerk 2 declaration
     */
    public void setCleanJerk2Declaration(String cleanJerk2Declaration) {
        if ("0".equals(cleanJerk2Declaration)) {
            this.cleanJerk2Declaration = cleanJerk2Declaration;
            logger.info("{} cleanJerk2Declaration={}", this, cleanJerk2Declaration);
            setCleanJerk2ActualLift("0");
            return;
        }
        if (validation) {
            validateDeclaration(2, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                    cleanJerk2Change2, cleanJerk2ActualLift);
        }
        this.cleanJerk2Declaration = cleanJerk2Declaration;
        logger.info("{} cleanJerk2Declaration={}", this, cleanJerk2Declaration);
    }

    /**
     * Sets the clean jerk 2 lift time.
     *
     * @param cleanJerk2LiftTime the new clean jerk 2 lift time
     */
    public void setCleanJerk2LiftTime(Date cleanJerk2LiftTime) {
    }

    /**
     * Sets the clean jerk 3 actual lift.
     *
     * @param cleanJerk3ActualLift the new clean jerk 3 actual lift
     */
    public void setCleanJerk3ActualLift(String cleanJerk3ActualLift) {
        if (validation) {
            validateCleanJerk3ActualLift(cleanJerk3ActualLift);
        }
        this.cleanJerk3ActualLift = cleanJerk3ActualLift;
        logger.info("{} cleanJerk3ActualLift={}", this, cleanJerk3ActualLift);

        if (zeroIfInvalid(cleanJerk3ActualLift) == 0) {
            this.cleanJerk3LiftTime = (null);
        } else {
            this.cleanJerk3LiftTime = sqlNow();
        }
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
            logger.info("{} cleanJerk3Change1={}", this, cleanJerk3Change1);
            setCleanJerk3ActualLift("0");
            return;
        }
        if (validation) {
            validateCleanJerk3Change1(cleanJerk3Change1);
        }
        this.cleanJerk3Change1 = cleanJerk3Change1;
        logger.info("{} cleanJerk3Change1={}", this, cleanJerk3Change1);
    }

    /**
     * Sets the clean jerk 3 change 2.
     *
     * @param cleanJerk3Change2 the new clean jerk 3 change 2
     */
    public void setCleanJerk3Change2(String cleanJerk3Change2) {
        if ("0".equals(cleanJerk3Change2)) {
            this.cleanJerk3Change2 = cleanJerk3Change2;
            logger.info("{} cleanJerk3Change2={}", this, cleanJerk3Change2);
            setCleanJerk3ActualLift("0");
            return;
        }

        if (validation) {
            validateCleanJerk3Change2(cleanJerk3Change2);
        }
        this.cleanJerk3Change2 = cleanJerk3Change2;
        logger.info("{} cleanJerk3Change2={}", this, cleanJerk3Change2);
    }

    /**
     * Sets the clean jerk 3 declaration.
     *
     * @param cleanJerk3Declaration the new clean jerk 3 declaration
     */
    public void setCleanJerk3Declaration(String cleanJerk3Declaration) {
        if ("0".equals(cleanJerk3Declaration)) {
            this.cleanJerk3Declaration = cleanJerk3Declaration;
            logger.info("{} cleanJerk3Declaration={}", this, cleanJerk3Declaration);
            setCleanJerk3ActualLift("0");
            return;
        }
        if (validation) {
            validateDeclaration(3, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                    cleanJerk3Change2, cleanJerk3ActualLift);
        }
        this.cleanJerk3Declaration = cleanJerk3Declaration;
        logger.info("{} cleanJerk3Declaration={}", this, cleanJerk3Declaration);
    }

    /**
     * Sets the clean jerk 3 lift time.
     *
     * @param cleanJerk3LiftTime the new clean jerk 3 lift time
     */
    public void setCleanJerk3LiftTime(Date cleanJerk3LiftTime) {
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
     * @param cleanJerkPoints the new clean jerk points
     */
    public void setCleanJerkPoints(Float cleanJerkPoints) {
        this.cleanJerkPoints = cleanJerkPoints;
    }

    /**
     * Sets the clean jerk rank.
     *
     * @param cleanJerkRank the new clean jerk rank
     */
    public void setCleanJerkRank(Integer cleanJerkRank) {
        this.cleanJerkRank = cleanJerkRank;
    }

    /**
     * Sets the club.
     *
     * @param club the club to set
     */
    public void setClub(String club) {
        setTeam(club);
    }

//	/**
//	 * Sets the result order rank.
//	 *
//	 * @param resultOrderRank the result order rank
//	 * @param rankingType     the ranking type
//	 */
//	public void setResultOrderRank(Integer resultOrderRank, Ranking rankingType) {
//		this.resultOrderRank = resultOrderRank;
//	}

    /**
     * Sets the custom points.
     *
     * @param customPoints the new custom points
     */
    public void setCustomPoints(float customPoints) {
        this.customPoints = customPoints;
    }

    /**
     * Sets the custom rank.
     *
     * @param customRank the new custom rank
     */
    public void setCustomRank(Integer customRank) {
        this.customRank = customRank;
    }

    /**
     * Sets the custom score.
     *
     * @param customScore the new custom score
     */
    public void setCustomScore(Double customScore) {
        this.customScore = customScore;
    }

    public void setEligibleForIndividualRanking(boolean eligibleForIndividualRanking) {
        this.eligibleForIndividualRanking = eligibleForIndividualRanking;
    }

    public void setEligibleForTeamRanking(boolean eligibleForTeamRanking) {
        this.eligibleForTeamRanking = eligibleForTeamRanking;
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
        logger.trace("setForcedAsCurrent({})", forcedAsCurrent);
        this.forcedAsCurrent = forcedAsCurrent;
    }

    /**
     * Set all date fields consistently.
     *
     * @param newBirthDateAsDate
     */

    private void setFullBirthDate(Integer yearOfBirth) {
        if (yearOfBirth != null) {
            this.fullBirthDate = LocalDate.of(yearOfBirth, 1, 1);
        } else {
            this.fullBirthDate = null;
        }
    }

    /**
     * Sets the full birth date.
     *
     * @param fullBirthDate the fullBirthDate to set
     */
    public void setFullBirthDate(LocalDate fullBirthDate) {
        this.fullBirthDate = fullBirthDate;
    }

    /*
     * General event framework: we implement the com.vaadin.event.MethodEventSource
     * interface which defines how a notifier can call a method on a listener to
     * signal that an event has occurred, and how the listener can
     * register/unregister itself.
     */

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
     * Sets the last lift time.
     *
     * @param d the new last lift time
     */
    public void setLastLiftTime(Date d) {
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
        logger.setLevel(newLevel);
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

    /**
     * Sets the qualifying total.
     *
     * @param qualifyingTotal the new qualifying total
     */
    public void setQualifyingTotal(Integer qualifyingTotal) {
        this.qualifyingTotal = qualifyingTotal;
    }

    /**
     * Sets the rank.
     *
     * @param i the new rank
     */
    public void setRank(Integer i) {
        this.totalRank = i;
    }

    /**
     * Sets the registration category.
     *
     * @param registrationCategory the new registration category
     */
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

    /**
     * Sets the snatch 1 actual lift.
     *
     * @param snatch1ActualLift the new snatch 1 actual lift
     */
    public void setSnatch1ActualLift(String snatch1ActualLift) {
        if (validation) {
            validateSnatch1ActualLift(snatch1ActualLift);
        }
        this.snatch1ActualLift = snatch1ActualLift;
        logger.info("{} snatch1ActualLift={} - {}", this, snatch1ActualLift);
        if (zeroIfInvalid(snatch1ActualLift) == 0) {
            this.snatch1LiftTime = null;
        } else {
            this.snatch1LiftTime = sqlNow();
        }
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
            logger.info("{} snatch1Change1={}", this, snatch1Change1);
            setSnatch1ActualLift("0");
            return;
        }
        if (validation) {
            validateSnatch1Change1(snatch1Change1);
        }
        this.snatch1Change1 = snatch1Change1;
        // validateStartingTotalsRule();

        logger.info("{} snatch1Change1={}", this, snatch1Change1);
    }

    /**
     * Sets the snatch 1 change 2.
     *
     * @param snatch1Change2 the new snatch 1 change 2
     */
    public void setSnatch1Change2(String snatch1Change2) {
        if ("0".equals(snatch1Change2)) {
            this.snatch1Change2 = snatch1Change2;
            logger.info("{} snatch1Change2={}", this, snatch1Change2);
            setSnatch1ActualLift("0");
            return;
        }
        if (validation) {
            validateSnatch1Change2(snatch1Change2);
        }
        this.snatch1Change2 = snatch1Change2;
        // validateStartingTotalsRule();

        logger.info("{} snatch1Change2={}", this, snatch1Change2);
    }

    /**
     * Sets the snatch 1 declaration.
     *
     * @param snatch1Declaration the new snatch 1 declaration
     */
    public void setSnatch1Declaration(String snatch1Declaration) {
        if ("0".equals(snatch1Declaration)) {
            this.snatch1Declaration = snatch1Declaration;
            logger.info("{} snatch1Declaration={}", this, snatch1Declaration);
            setSnatch1ActualLift("0");
            return;
        }
        if (validation) {
            validateDeclaration(1, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
                    snatch1ActualLift);
        }
        this.snatch1Declaration = snatch1Declaration;
//		if (zeroIfInvalid(getCleanJerk1Declaration()) > 0)
//			validateStartingTotalsRule();

        logger.info("{} snatch1Declaration={}", this, snatch1Declaration);
    }

    /**
     * Sets the snatch 1 lift time.
     *
     * @param snatch1LiftTime the new snatch 1 lift time
     */
    public void setSnatch1LiftTime(Date snatch1LiftTime) {
    }

    /**
     * Sets the snatch 2 actual lift.
     *
     * @param snatch2ActualLift the new snatch 2 actual lift
     */
    public void setSnatch2ActualLift(String snatch2ActualLift) {
        if (validation) {
            validateSnatch2ActualLift(snatch2ActualLift);
        }
        this.snatch2ActualLift = snatch2ActualLift;
        logger.info("{} snatch2ActualLift={}", this, snatch2ActualLift);
        if (zeroIfInvalid(snatch2ActualLift) == 0) {
            this.snatch2LiftTime = (null);
        } else {
            this.snatch2LiftTime = sqlNow();
        }
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
            logger.info("{} snatch2Change1={}", this, snatch2Change1);
            setSnatch2ActualLift("0");
            return;
        }
        if (validation) {
            validateSnatch2Change1(snatch2Change1);
        }
        this.snatch2Change1 = snatch2Change1;
        logger.info("{} snatch2Change1={}", this, snatch2Change1);
    }

    /**
     * Sets the snatch 2 change 2.
     *
     * @param snatch2Change2 the new snatch 2 change 2
     */
    public void setSnatch2Change2(String snatch2Change2) {
        if ("0".equals(snatch2Change2)) {
            this.snatch2Change2 = snatch2Change2;
            logger.info("{} snatch2Change2={}", this, snatch2Change2);
            setSnatch2ActualLift("0");
            return;
        }
        if (validation) {
            validateSnatch2Change2(snatch2Change2);
        }
        this.snatch2Change2 = snatch2Change2;
        logger.info("{} snatch2Change2={}", this, snatch2Change2);
    }

    /**
     * Sets the snatch 2 declaration.
     *
     * @param snatch2Declaration the new snatch 2 declaration
     */
    public void setSnatch2Declaration(String snatch2Declaration) {
        if ("0".equals(snatch2Declaration)) {
            this.snatch2Declaration = snatch2Declaration;
            logger.info("{} snatch2Declaration={}", this, snatch2Declaration);
            setSnatch2ActualLift("0");
            return;
        }
        if (validation) {
            validateDeclaration(2, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                    snatch2ActualLift);
        }
        this.snatch2Declaration = snatch2Declaration;
        logger.info("{} snatch2Declaration={}", this, snatch2Declaration);
    }

    /**
     * Sets the snatch 2 lift time.
     *
     * @param snatch2LiftTime the new snatch 2 lift time
     */
    public void setSnatch2LiftTime(Date snatch2LiftTime) {
    }

    /**
     * Sets the snatch 3 actual lift.
     *
     * @param snatch3ActualLift the new snatch 3 actual lift
     */
    public void setSnatch3ActualLift(String snatch3ActualLift) {
        if (validation) {
            validateSnatch3ActualLift(snatch3ActualLift);
        }
        this.snatch3ActualLift = snatch3ActualLift;
        logger.info("{} snatch3ActualLift={}", this, snatch3ActualLift);
        if (zeroIfInvalid(snatch3ActualLift) == 0) {
            this.snatch3LiftTime = (null);
        } else {
            this.snatch3LiftTime = sqlNow();
        }

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
            logger.info("{} snatch3Change1={}", this, snatch3Change1);
            setSnatch3ActualLift("0");
            return;
        }
        if (validation) {
            validateSnatch3Change1(snatch3Change1);
        }
        this.snatch3Change1 = snatch3Change1;
        logger.info("{} snatch3Change1={}", this, snatch3Change1);
    }

    /**
     * Sets the snatch 3 change 2.
     *
     * @param snatch3Change2 the new snatch 3 change 2
     */
    public void setSnatch3Change2(String snatch3Change2) {
        if ("0".equals(snatch3Change2)) {
            this.snatch3Change2 = snatch3Change2;
            logger.info("{} snatch3Change2={}", this, snatch3Change2);
            setSnatch3ActualLift("0");
            return;
        }
        if (validation) {
            validateSnatch3Change2(snatch3Change2);
        }
        this.snatch3Change2 = snatch3Change2;
        logger.info("{} snatch3Change2={}", this, snatch3Change2);
    }

    /**
     * Sets the snatch 3 declaration.
     *
     * @param snatch3Declaration the new snatch 3 declaration
     */
    public void setSnatch3Declaration(String snatch3Declaration) {
        if ("0".equals(snatch3Declaration)) {
            this.snatch3Declaration = snatch3Declaration;
            logger.info("{} snatch3Declaration={}", this, snatch3Declaration);
            setSnatch3ActualLift("0");
            return;
        }
        if (validation) {
            validateDeclaration(3, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                    snatch3ActualLift);
        }
        this.snatch3Declaration = snatch3Declaration;
        logger.info("{} snatch3Declaration={}", this, snatch3Declaration);
    }

    /**
     * Sets the snatch 3 lift time.
     *
     * @param snatch3LiftTime the new snatch 3 lift time
     */
    public void setSnatch3LiftTime(Date snatch3LiftTime) {
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
    public void setSnatchPoints(float snatchPoints) {
        this.snatchPoints = snatchPoints;
    }

    /**
     * Sets the snatch rank.
     *
     * @param snatchRank the new snatch rank
     */
    public void setSnatchRank(Integer snatchRank) {
        this.snatchRank = snatchRank;
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
    public void setTotalPoints(float totalPoints) {
        this.totalPoints = totalPoints;
    }

    /**
     * Sets the total rank.
     *
     * @param totalRank the new total rank
     */
    public void setTotalRank(Integer totalRank) {
        this.totalRank = totalRank;
    }

    public void setValidation(boolean b) {
        logger.trace("Validation {}", b);
        validation = b;
    }

    /**
     * Sets the year of birth.
     *
     * @param birthYear the new year of birth
     */
    public void setYearOfBirth(Integer birthYear) {
        setFullBirthDate(birthYear);
    }

    /**
     * Compute the Sinclair formula given its parameters.
     *
     * @param coefficient
     * @param maxWeight
     */
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

    private LocalDateTime sqlNow() {
        return LocalDateTime.now();
    }

    /**
     * Successful lift.
     */
    public void successfulLift() {
        try {
            logger.info("good lift for {}", this);
            final String weight = Integer.toString(getNextAttemptRequestedWeight());
            doLift(weight);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
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
        String suffix = "_" + System.identityHashCode(this);
        if (getLastName() != null) {
            return prefix + "_" + getLastName() + "_" + getFirstName() + suffix;
        } else {
            return prefix + suffix;
        }

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

        int declaredChanges = last(zeroIfInvalid(automaticProgression), zeroIfInvalid(declaration),
                zeroIfInvalid(change1), zeroIfInvalid(change2));
        final int iAutomaticProgression = zeroIfInvalid(automaticProgression);
        final int liftedWeight = zeroIfInvalid(actualLift);

        logger.trace("declaredChanges={} automaticProgression={} declaration={} change1={} change2={} liftedWeight={}",
                declaredChanges, automaticProgression, declaration, change1, change2, liftedWeight);
        if (liftedWeight == 0) {
            // Athlete is not taking try; always ok no matter what was declared.
            return;
        }
//		if (declaredChanges == 0 && iAutomaticProgression > 0) {
//			// assume data entry is being done without reference to
//			// declarations, check if > progression
//			if (Math.abs(liftedWeight) >= iAutomaticProgression) {
//				return;
//			} else {
//				throw RuleViolation.liftValueBelowProgression(curLift, actualLift, iAutomaticProgression);
//			}
//		} else {

        // allow empty declaration (declaration == automatic progression).
        // if (validation) validateDeclaration(curLift, automaticProgression,
        // declaration, change1, change2, actualLift);
        final boolean declaredChangesOk = declaredChanges >= iAutomaticProgression;
        final boolean liftedWeightOk = Math.abs(liftedWeight) == declaredChanges;
        if (liftedWeightOk && declaredChangesOk) {
            return;
        } else {
            if (!declaredChangesOk) {
                throw RuleViolation.declaredChangesNotOk(curLift, declaredChanges, iAutomaticProgression,
                        iAutomaticProgression + 1);
            }
            if (!liftedWeightOk) {
                throw RuleViolation.liftValueNotWhatWasRequested(curLift, actualLift, declaredChanges, liftedWeight);
            }
            return;
        }
//		}
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
            throw RuleViolation.declaredChangesNotOk(curLift, newVal, prevVal);
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
            throw RuleViolation.declaredChangesNotOk(curLift, newVal, prevVal);
        }
    }

    public boolean validateCleanJerk1ActualLift(String cleanJerk1ActualLift) throws RuleViolationException {
        validateActualLift(1, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
                cleanJerk1Change2, cleanJerk1ActualLift);
        return true;
    }

    public boolean validateCleanJerk1Change1(String cleanJerk1Change1) throws RuleViolationException {
        validateChange1(1, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
                cleanJerk1Change2, cleanJerk1ActualLift, false);
        validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
        return true;
    }

    public boolean validateCleanJerk1Change2(String cleanJerk1Change2) throws RuleViolationException {
        validateChange2(1, getCleanJerk1AutomaticProgression(), cleanJerk1Declaration, cleanJerk1Change1,
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
        validateActualLift(2, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift);
        return true;
    }

    public boolean validateCleanJerk2Change1(String cleanJerk2Change1) throws RuleViolationException {
        validateChange1(2, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk2Change2(String cleanJerk2Change2) throws RuleViolationException {
        validateChange2(2, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk2Declaration(String cleanJerk2Declaration) throws RuleViolationException {
        validateDeclaration(2, getCleanJerk2AutomaticProgression(), cleanJerk2Declaration, cleanJerk2Change1,
                cleanJerk2Change2, cleanJerk2ActualLift);
        return true;
    }

    public boolean validateCleanJerk3ActualLift(String cleanJerk3ActualLift) throws RuleViolationException {
        validateActualLift(3, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift);
        // throws exception if invalid
        return true;
    }

    public boolean validateCleanJerk3Change1(String cleanJerk3Change1) throws RuleViolationException {
        validateChange1(3, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk3Change2(String cleanJerk3Change2) throws RuleViolationException {
        validateChange2(3, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift, false);
        return true;
    }

    public boolean validateCleanJerk3Declaration(String cleanJerk3Declaration) throws RuleViolationException {
        validateDeclaration(3, getCleanJerk3AutomaticProgression(), cleanJerk3Declaration, cleanJerk3Change1,
                cleanJerk3Change2, cleanJerk3ActualLift);
        return true;
    }

    /**
     * @param curLift
     * @param actualLift
     */
    private void validateDeclaration(int curLift, String automaticProgression, String declaration, String change1,
            String change2, String actualLift) throws RuleViolationException {
//		boolean actualLiftEmpty = actualLift == null || actualLift.trim().isEmpty();
//		boolean declarationEmpty = declaration == null || declaration.trim().isEmpty();
//		if (declarationEmpty) {
//			if (actualLiftEmpty)
//			else
//				throw RuleViolation.declarationValueRequired(curLift);
//		}
        logger.trace("{} validateDeclaration", this, declaration);
        int newVal = zeroIfInvalid(declaration);
        int iAutomaticProgression = zeroIfInvalid(automaticProgression);
        // allow null declaration for reloading old results.
        if (iAutomaticProgression > 0 && newVal > 0 && newVal < iAutomaticProgression) {
            throw RuleViolation.declarationValueTooSmall(curLift, newVal, iAutomaticProgression);
        }
    }

    public boolean validateSnatch1ActualLift(String snatch1ActualLift) throws RuleViolationException {
        validateActualLift(1, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
                snatch1ActualLift);
        return true;
    }

    public boolean validateSnatch1Change1(String snatch1Change1) throws RuleViolationException {
        validateChange1(1, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
                snatch1ActualLift, true);
        validateStartingTotalsRule(snatch1Declaration, snatch1Change1, snatch1Change2, cleanJerk1Declaration,
                cleanJerk1Change1, cleanJerk1Change2);
        return true;
    }

    public boolean validateSnatch1Change2(String snatch1Change2) throws RuleViolationException {
        validateChange2(1, getSnatch1AutomaticProgression(), snatch1Declaration, snatch1Change1, snatch1Change2,
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
        validateActualLift(2, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                snatch2ActualLift);
        return true;
    }

    public boolean validateSnatch2Change1(String snatch2Change1) throws RuleViolationException {
        validateChange1(2, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                snatch2ActualLift, true);
        return true;
    }

    public boolean validateSnatch2Change2(String snatch2Change2) throws RuleViolationException {
        validateChange2(2, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                snatch2ActualLift, true);
        return true;
    }

    public boolean validateSnatch2Declaration(String snatch2Declaration) throws RuleViolationException {
        validateDeclaration(2, getSnatch2AutomaticProgression(), snatch2Declaration, snatch2Change1, snatch2Change2,
                snatch2ActualLift);
        return true;
    }

    public boolean validateSnatch3ActualLift(String snatch3ActualLift) throws RuleViolationException {
        validateActualLift(3, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift);
        return true;
    }

    public boolean validateSnatch3Change1(String snatch3Change1) throws RuleViolationException {
        validateChange1(3, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift, true);
        return true;
    }

    public boolean validateSnatch3Change2(String snatch3Change2) throws RuleViolationException {
        validateChange2(3, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift, true);
        return true;
    }

    public boolean validateSnatch3Declaration(String snatch3Declaration) throws RuleViolationException {
        validateDeclaration(3, getSnatch3AutomaticProgression(), snatch3Declaration, snatch3Change1, snatch3Change2,
                snatch3ActualLift);
        return true;
    }

    /**
     * @param entryTotal
     * @return true if ok, exception if not
     * @throws RuleViolationException if rule violated, exception contails details.
     */
    public boolean validateStartingTotalsRule(String snatch1Declaration, String snatch1Change1, String snatch1Change2,
            String cleanJerk1Declaration, String cleanJerk1Change1, String cleanJerk1Change2) {
        boolean enforce20kg = Competition.getCurrent().isEnforce20kgRule();
        int qualTotal = getQualifyingTotal();
        logger.debug("enforcing 20kg rule {} {}", enforce20kg, qualTotal);
        if (!enforce20kg) {
            return true;
        }
        if (qualTotal == 0) {
            return true;
        }
        int sn1Decl = zeroIfInvalid(snatch1Declaration);
        int cj1Decl = zeroIfInvalid(cleanJerk1Declaration);
        logger.trace("prior to checking {} {}", sn1Decl, cj1Decl);
        if (sn1Decl == 0 && cj1Decl == 0) {
            return true; // do not complain on registration form or empty weigh-in form.
        }

        Integer snatch1Request = last(sn1Decl, zeroIfInvalid(snatch1Change1), zeroIfInvalid(snatch1Change2));

        Integer cleanJerk1Request = last(cj1Decl, zeroIfInvalid(cleanJerk1Change1), zeroIfInvalid(cleanJerk1Change2));
        return validateStartingTotalsRule(this, snatch1Request, cleanJerk1Request, qualTotal);
    }

    /**
     * Withdraw.
     */
    public void withdraw() {
        if (snatch1ActualLift != null && snatch1ActualLift.trim().isEmpty()) {
            setSnatch1ActualLift("0");
        }
        if (snatch2ActualLift != null && snatch2ActualLift.trim().isEmpty()) {
            setSnatch2ActualLift("0");
        }
        if (snatch3ActualLift != null && snatch3ActualLift.trim().isEmpty()) {
            setSnatch3ActualLift("0");
        }
        if (cleanJerk1ActualLift != null && cleanJerk1ActualLift.trim().isEmpty()) {
            setCleanJerk1ActualLift("0");
        }
        if (cleanJerk2ActualLift != null && cleanJerk2ActualLift.trim().isEmpty()) {
            setCleanJerk2ActualLift("0");
        }
        if (cleanJerk3ActualLift != null && cleanJerk3ActualLift.trim().isEmpty()) {
            setCleanJerk3ActualLift("0");
        }
    }

}
