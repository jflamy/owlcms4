/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;

/**
 * The Class AbstractLifterComparator.
 */
public class AbstractLifterComparator {
    final private static Logger logger = LoggerFactory.getLogger(AbstractLifterComparator.class);

    /**
     * Compare age group.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareAgeGroup(Athlete lifter1, Athlete lifter2) {
        AgeGroup lifter1Value = lifter1.getAgeGroup();
        AgeGroup lifter2Value = lifter2.getAgeGroup();
        return ObjectUtils.compare(lifter1Value, lifter2Value, true);
    }

    /**
     * Compare attempts done.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareAttemptsDone(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getAttemptsDone();
        Integer lifter2Value = lifter2.getAttemptsDone();
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare best clean jerk.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareBestCleanJerk(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getBestCleanJerk();
        Integer lifter2Value = lifter2.getBestCleanJerk();
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare best clean jerk attempt number.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareBestCleanJerkAttemptNumber(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getBestCleanJerkAttemptNumber();
        Integer lifter2Value = lifter2.getBestCleanJerkAttemptNumber();
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare best lift attempt number.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareBestLiftAttemptNumber(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getBestResultAttemptNumber();
        Integer lifter2Value = lifter2.getBestResultAttemptNumber();
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare best snatch.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareBestSnatch(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getBestSnatch();
        Integer lifter2Value = lifter2.getBestSnatch();
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare best snatch attempt number.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareBestSnatchAttemptNumber(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getBestSnatchAttemptNumber();
        Integer lifter2Value = lifter2.getBestSnatchAttemptNumber();
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare birth dates. No birth date implies newborn.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return 1 if lifter1 is younger; 0 if equal, -1 if older.
     */
    int compareBirthDate(Athlete lifter1, Athlete lifter2) {
        LocalDate lifter1Value = lifter1.getFullBirthDate();
        LocalDate lifter2Value = lifter2.getFullBirthDate();
        final LocalDate now = LocalDate.now();
        if (lifter1Value == null) {
            lifter1Value = now;
        }
        if (lifter2Value == null) {
            lifter2Value = now;
        }
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare body weight.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareBodyWeight(Athlete lifter1, Athlete lifter2) {
        Double lifter1Value = lifter1.getBodyWeight();
        Double lifter2Value = lifter2.getBodyWeight();
        final Double notWeighed = 0D;
        if (lifter1Value == null) {
            lifter1Value = notWeighed;
        }
        if (lifter2Value == null) {
            lifter2Value = notWeighed;
        }
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare category.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareCategory(Athlete lifter1, Athlete lifter2) {
        Category lifter1Value = lifter1.getCategory();
        Category lifter2Value = lifter2.getCategory();
        return ObjectUtils.compare(lifter1Value, lifter2Value, true);
    }

    /**
     * Compare category sinclair.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareCategorySinclair(Athlete lifter1, Athlete lifter2) {
        Gender gender1 = lifter1.getGender();
        Gender gender2 = lifter2.getGender();
        int compare = ObjectUtils.compare(gender1, gender2, true);
        if (compare != 0) {
            return compare;
        }

        Double lifter1Value = lifter1.getCategorySinclair();
        Double lifter2Value = lifter2.getCategorySinclair();
        final Double notWeighed = 0D;
        if (lifter1Value == null) {
            lifter1Value = notWeighed;
        }
        if (lifter2Value == null) {
            lifter2Value = notWeighed;
        }
        // bigger sinclair comes first
        return -lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare club.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareClub(Athlete lifter1, Athlete lifter2) {
        String club1 = lifter1.getTeam();
        String club2 = lifter2.getTeam();
        return ObjectUtils.compare(club1, club2, true);
    }

    /**
     * Compare custom score.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareCustomScore(Athlete lifter1, Athlete lifter2) {
        Double lifter1Value = lifter1.getCustomScoreComputed();
        Double lifter2Value = lifter2.getCustomScoreComputed();
        final Double notScored = 0D;
        if (lifter1Value == null) {
            lifter1Value = notScored;
        }
        if (lifter2Value == null) {
            lifter2Value = notScored;
        }
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Comparer les totaux des leveurs, si ils ont terminé tous leurs essais. Le leveur ayant terminé va après, de
     * manière à ce le premier à lever soit toujours toujours le premier dans la liste.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return -1,0,1 selon comparaison
     */
    int compareFinalResults(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Done = (lifter1.getAttemptsDone() >= 6 ? 1 : 0);
        Integer lifter2Done = (lifter2.getAttemptsDone() >= 6 ? 1 : 0);

        int compare = lifter1Done.compareTo(lifter2Done);
        if (compare != 0) {
            return compare;
        }

        // at this point both athletes are done, or both are not done.
        if (lifter1Done == 0) {
            // both are not done
            return 0;
        } else {
            // both are done, use descending order on total
            return -compareTotal(lifter1, lifter2);
        }

    }

    /**
     * Compare first name.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareFirstName(Athlete lifter1, Athlete lifter2) {
        String lifter1Value = lifter1.getFirstName();
        String lifter2Value = lifter2.getFirstName();
        return ObjectUtils.compare(lifter1Value, lifter2Value, true);
    }

    /**
     * Compare forced as first.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareForcedAsCurrent(Athlete lifter1, Athlete lifter2) {
        // can't be nulls, method returns primitive boolean
        Boolean lifter1Value = lifter1.isForcedAsCurrent();
        Boolean lifter2Value = lifter2.isForcedAsCurrent();

        // true.compareTo(false) returns positive (i.e. greater). We want the
        // opposite.
        final int compare = -lifter1Value.compareTo(lifter2Value);
        return compare;
    }

    /**
     * Compare gender.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareGender(Athlete lifter1, Athlete lifter2) {
        Gender gender1 = lifter1.getGender();
        Gender gender2 = lifter2.getGender();
        return ObjectUtils.compare(gender1, gender2, true);
    }

    /**
     * Compare group.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareGroup(Athlete lifter1, Athlete lifter2) {
        Group lifter1Group = lifter1.getGroup();
        Group lifter2Group = lifter2.getGroup();
        if (lifter1Group == null && lifter2Group == null) {
            return 0;
        }
        if (lifter1Group == null) {
            return -1;
        }
        if (lifter2Group == null) {
            return 1;
        }

        String lifter1Value = lifter1Group.getName();
        String lifter2Value = lifter2Group.getName();
        if (lifter1Value == null && lifter2Value == null) {
            return 0;
        }
        if (lifter1Value == null) {
            return -1;
        }
        if (lifter2Value == null) {
            return 1;
        }
        return lifter1Value.compareTo(lifter2Value);
    }

    int compareGroupPlatform(Athlete lifter1, Athlete lifter2) {

        Group lifter1Group = lifter1.getGroup();
        Group lifter2Group = lifter2.getGroup();

        int compare = ObjectUtils.compare(lifter1Group, lifter2Group, true);
        if ((compare == 0) || lifter1Group == null || lifter2Group == null) {
            // a non-null group will sort before null
            return compare;
        }

        Platform p1 = lifter1Group.getPlatform();
        Platform p2 = lifter2Group.getPlatform();
        String name1 = p1 != null ? p1.getName() : null;
        String name2 = p2 != null ? p2.getName() : null;
        compare = ObjectUtils.compare(name1, name2, true);
        if (compare != 0) {
            return compare;
        }

        return 0;
    }

    /**
     * Compare group weigh in time.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareGroupWeighInTime(Athlete lifter1, Athlete lifter2) {

        Group lifter1Group = lifter1.getGroup();
        Group lifter2Group = lifter2.getGroup();

        int compare = ObjectUtils.compare(lifter1Group, lifter2Group, true);
        if ((compare == 0) || lifter1Group == null || lifter2Group == null) {
            // a non-null group will sort before null
            return compare;
        }

        LocalDateTime lifter1Date = lifter1Group.getWeighInTime();
        LocalDateTime lifter2Date = lifter2Group.getWeighInTime();
        compare = ObjectUtils.compare(lifter1Date, lifter2Date, true);
        if (compare != 0) {
            // logger.trace("different date {} {}", lifter1Date, lifter1Date);
            return compare;
        }

        Platform p1 = lifter1Group.getPlatform();
        Platform p2 = lifter2Group.getPlatform();
        String name1 = p1 != null ? p1.getName() : null;
        String name2 = p2 != null ? p2.getName() : null;
        compare = ObjectUtils.compare(name1, name2, false);
        if (compare != 0) {
            // logger.trace("different platform {} {} {}", name1, name2, LoggerUtils.whereFrom(10));
            return compare;
        }

        String lifter1String = lifter1Group.getName();
        String lifter2String = lifter2Group.getName();
        compare = ObjectUtils.compare(lifter1String, lifter2String, true);
        if (compare != 0) {
            // logger.trace("different group {} {} {}", lifter1String, lifter2String, LoggerUtils.whereFrom(10));
            return compare;
        }

        return 0;
    }

    /**
     * Compare last name.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareLastName(Athlete lifter1, Athlete lifter2) {
        String lifter1Value = lifter1.getLastName();
        String lifter2Value = lifter2.getLastName();
        return ObjectUtils.compare(lifter1Value, lifter2Value, true);
    }

    /**
     * Compare last successful lift time.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareLastSuccessfulLiftTime(Athlete lifter1, Athlete lifter2) {
        LocalDateTime lifter1Value = lifter1.getLastSuccessfulLiftTime();
        LocalDateTime lifter2Value = lifter2.getLastSuccessfulLiftTime();
        // safe to compare, no nulls.
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare lift type.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareLiftType(Athlete lifter1, Athlete lifter2) {
        // snatch comes before clean and jerk
        Integer lifter1Value = lifter1.getAttemptsDone();
        Integer lifter2Value = lifter2.getAttemptsDone();
        if (lifter1Value < 3) {
            lifter1Value = 0;
        } else {
            lifter1Value = 1;
        }
        if (lifter2Value < 3) {
            lifter2Value = 0;
        } else {
            lifter2Value = 1;
        }
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare lot number.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareLotNumber(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getLotNumber();
        Integer lifter2Value = lifter2.getLotNumber();
        if (lifter1Value == null && lifter2Value == null) {
            return 0;
        }
        if (lifter1Value == null) {
            return -1;
        }
        if (lifter2Value == null) {
            return 1;
        }
        return lifter1Value.compareTo(lifter2Value);
    }
    
    /**
     * Compare lot number.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareEntryTotal(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getEntryTotal();
        Integer lifter2Value = lifter2.getEntryTotal();
        return ObjectUtils.compare(lifter1Value, lifter2Value, false);
    }


    /**
     * Compare absolute value of attempts prior to attempt "startingFrom" Start comparing attempted weights at
     * "startingFrom". If attempted weight differ, smallest attempted weight comes first. If attempted weights are same,
     * go back one attempt and keep comparing.
     *
     * startingFrom is exclusive endingWith is inclusive, and is used to the previous attempts.
     *
     * @param startingFrom  the starting from
     * @param excludeSnatch to consider only cleanAndJerk
     * @param lifter1       the lifter 1
     * @param lifter2       the lifter 2
     * @return the int
     */
    int comparePreviousAttempts(int startingFrom, boolean excludeSnatch, Athlete lifter1, Athlete lifter2) {
        int compare = 0;
        boolean trace = false;
        if (trace) {
            logger.trace("starting from {}, lifter1 {}, lifter2 {}", startingFrom, lifter1, lifter2);
        }
        if (startingFrom >= 6) {
            compare = ((Integer) Math.abs(Athlete.zeroIfInvalid(lifter1.getCleanJerk3ActualLift())))
                    .compareTo(Math.abs(Athlete.zeroIfInvalid(lifter2.getCleanJerk3ActualLift())));
            if (trace) {
                logger.trace("essai 6: {}", compare);
            }
            if (compare != 0) {
                return compare;
            }
        }
        if (startingFrom >= 5) {
            compare = ((Integer) Math.abs(Athlete.zeroIfInvalid(lifter1.getCleanJerk2ActualLift())))
                    .compareTo(Math.abs(Athlete.zeroIfInvalid(lifter2.getCleanJerk2ActualLift())));
            if (trace) {
                logger.trace("essai 5: {}", compare);
            }
            if (compare != 0) {
                return compare;
            }
        }
        if (startingFrom >= 4) {
            compare = ((Integer) Math.abs(Athlete.zeroIfInvalid(lifter1.getCleanJerk1ActualLift())))
                    .compareTo(Math.abs(Athlete.zeroIfInvalid(lifter2.getCleanJerk1ActualLift())));
            if (trace) {
                logger.trace("essai 4: {}", compare);
            }
            if (compare != 0) {
                return compare;
            }
        }
        if (excludeSnatch) {
            return 0;
        }
        if (startingFrom >= 3) {
            compare = ((Integer) Math.abs(Athlete.zeroIfInvalid(lifter1.getSnatch3ActualLift())))
                    .compareTo(Math.abs(Athlete.zeroIfInvalid(lifter2.getSnatch3ActualLift())));
            if (trace) {
                logger.trace("essai 3: {}", compare);
            }
            if (compare != 0) {
                return compare;
            }
        }
        if (startingFrom >= 2) {
            compare = ((Integer) Math.abs(Athlete.zeroIfInvalid(lifter1.getSnatch2ActualLift())))
                    .compareTo(Math.abs(Athlete.zeroIfInvalid(lifter2.getSnatch2ActualLift())));
            if (trace) {
                logger.trace("essai 2: {}", compare);
            }
            if (compare != 0) {
                return compare;
            }
        }
        if (startingFrom >= 1) {
            compare = ((Integer) Math.abs(Athlete.zeroIfInvalid(lifter1.getSnatch1ActualLift())))
                    .compareTo(Math.abs(Athlete.zeroIfInvalid(lifter2.getSnatch1ActualLift())));
            if (trace) {
                logger.trace("essai 1: {}", compare);
            }
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

    /**
     * Return who lifted last, for real.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int comparePreviousLiftOrder(Athlete lifter1, Athlete lifter2) {
        LocalDateTime lifter1Value = lifter1.getPreviousLiftTime();
        LocalDateTime lifter2Value = lifter2.getPreviousLiftTime();

        final LocalDateTime longAgo = LocalDateTime.MIN;
        if (lifter1Value == null) {
            lifter1Value = longAgo;
        }
        if (lifter2Value == null) {
            lifter2Value = longAgo;
        }

        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Return who lifted last, ignoring athletes who are done lifting for this part of the meet.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int comparePreviousLiftOrderExceptAtEnd(Athlete lifter1, Athlete lifter2) {
        LocalDateTime lifter1Value = lifter1.getPreviousLiftTime();
        LocalDateTime lifter2Value = lifter2.getPreviousLiftTime();

        final LocalDateTime longAgo = LocalDateTime.MIN;
        if (lifter1Value == null) {
            lifter1Value = longAgo;
        }
        if (lifter2Value == null) {
            lifter2Value = longAgo;
        }

        // at start of snatch and start of clean and jerk, previous lift is
        // irrelevant.
        if (lifter1.getAttemptsDone() == 3) {
            lifter1Value = longAgo;
        }
        if (lifter2.getAttemptsDone() == 3) {
            lifter2Value = longAgo;
        }

        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Determine who lifted first if both athletes are at same attempt and requesting same weight. Smaller previous
     * attempt means lifted first.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareProgression(Athlete lifter1, Athlete lifter2) {
        int compare = 0;

        int requested1 = lifter1.getNextAttemptRequestedWeight();
        int requested2 = lifter2.getNextAttemptRequestedWeight();
        assert requested1 == requested2;

        int startingFrom1 = lifter1.getAttemptsDone();
        int startingFrom2 = lifter2.getAttemptsDone();
        assert startingFrom1 == startingFrom2;

        int currentTry = startingFrom1 + 1;
        if (currentTry > 3) {
            // clean and jerk
            if (currentTry == 6) {
                // smaller 2nd attempt lifted first
                Integer attemptedCJ2_1 = Math.abs(Athlete.zeroIfInvalid(lifter1.getCleanJerk2ActualLift()));
                Integer attemptedCJ2_2 = Math.abs(Athlete.zeroIfInvalid(lifter2.getCleanJerk2ActualLift()));
                compare = (attemptedCJ2_1.compareTo(attemptedCJ2_2));
                if (compare != 0) {
                    return compare;
                    // if 2nd attempts are same, go on and compare first attempts
                }
            }
            if (currentTry >= 5) {
                // smaller 1st attempt lifted first
                Integer attemptedCJ1_1 = Math.abs(Athlete.zeroIfInvalid(lifter1.getCleanJerk1ActualLift()));
                Integer attemptedCJ1_2 = Math.abs(Athlete.zeroIfInvalid(lifter2.getCleanJerk1ActualLift()));
                compare = attemptedCJ1_1.compareTo(attemptedCJ1_2);
                if (compare != 0) {
                    return compare;
                    // if 1st attempts are same, can't determine who lifted first based on weights
                }
            }
            return 0;
        } else {
            // snatch
            if (currentTry == 3) {
                // smaller 2nd attempt lifted first
                Integer attemptedSn2_1 = Math.abs(Athlete.zeroIfInvalid(lifter1.getSnatch2ActualLift()));
                Integer attemptedSn2_2 = Math.abs(Athlete.zeroIfInvalid(lifter2.getSnatch2ActualLift()));
                compare = (attemptedSn2_1.compareTo(attemptedSn2_2));
                if (compare != 0) {
                    return compare;
                    // if 2nd attempts are same, go on and compare first attempts
                }
            }
            if (currentTry >= 2) {
                // smaller 1st attempt lifted first
                Integer attemptedSn1_1 = Math.abs(Athlete.zeroIfInvalid(lifter1.getSnatch1ActualLift()));
                Integer attemptedSn1_2 = Math.abs(Athlete.zeroIfInvalid(lifter2.getSnatch1ActualLift()));
                compare = attemptedSn1_1.compareTo(attemptedSn1_2);
                if (compare != 0) {
                    return compare;
                    // if 1st attempts are same, can't determine who lifted first based on weights
                }
            }
            return 0;
        }
    }

    /**
     * Compare registration category.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    @Deprecated
    int compareRegistrationCategory(Athlete lifter1, Athlete lifter2) {
        Category lifter1Value = lifter1.getRegistrationCategory();
        Category lifter2Value = lifter2.getRegistrationCategory();
        if (lifter1Value == null && lifter2Value == null) {
            return 0;
        }
        if (lifter1Value == null) {
            return -1;
        }
        if (lifter2Value == null) {
            return 1;
        }

        int compare = lifter1Value.compareTo(lifter2Value);
        return compare;
    }

    /**
     * Compare requested weight.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareRequestedWeight(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getNextAttemptRequestedWeight();
        Integer lifter2Value = lifter2.getNextAttemptRequestedWeight();
        if (lifter1Value == null || lifter1Value == 0) {
            lifter1Value = 999; // place people with no
        }
        // declared weight at the end
        if (lifter2Value == null || lifter2Value == 0) {
            lifter2Value = 999; // place people with no
        }
        // declared weight at the end
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare robi.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareRobi(Athlete lifter1, Athlete lifter2) {
        Gender gender = lifter1.getGender();
        if (gender == null) {
            return -1;
        }
        int compare = gender.compareTo(lifter2.getGender());
        if (compare != 0) {
            return compare;
        }

        Double lifter1Value = lifter1.getRobi();
        Double lifter2Value = lifter2.getRobi();
        final Double notWeighed = 0D;
        if (lifter1Value == null) {
            lifter1Value = notWeighed;
        }
        if (lifter2Value == null) {
            lifter2Value = notWeighed;
        }
        // bigger Robi comes first
        return -lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare sinclair.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareSinclair(Athlete lifter1, Athlete lifter2) {
        Gender gender = lifter1.getGender();
        if (gender == null) {
            return -1;
        }
        int compare = gender.compareTo(lifter2.getGender());
        if (compare != 0) {
            return compare;
        }

        Double lifter1Value = lifter1.getSinclair();
        Double lifter2Value = lifter2.getSinclair();
        final Double notWeighed = 0D;
        if (lifter1Value == null) {
            lifter1Value = notWeighed;
        }
        if (lifter2Value == null) {
            lifter2Value = notWeighed;
        }
        // bigger sinclair comes first
        return -lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare sinclair.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareSinclairForDelta(Athlete lifter1, Athlete lifter2) {
        Gender gender = lifter1.getGender();
        if (gender == null) {
            return -1;
        }
        int compare = gender.compareTo(lifter2.getGender());
        if (compare != 0) {
            return compare;
        }

        Double lifter1Value = lifter1.getSinclairForDelta();
        Double lifter2Value = lifter2.getSinclairForDelta();
        final Double notWeighed = 0D;
        if (lifter1Value == null) {
            lifter1Value = notWeighed;
        }
        if (lifter2Value == null) {
            lifter2Value = notWeighed;
        }
        // bigger sinclair comes first
        return -lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare smm.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareSmm(Athlete lifter1, Athlete lifter2) {
        Gender gender = lifter1.getGender();
        if (gender == null) {
            return -1;
        }
        int compare = gender.compareTo(lifter2.getGender());
        if (compare != 0) {
            return compare;
        }

        Double lifter1Value = lifter1.getSmm();
        Double lifter2Value = lifter2.getSmm();
        final Double notWeighed = 0D;
        if (lifter1Value == null) {
            lifter1Value = notWeighed;
        }
        if (lifter2Value == null) {
            lifter2Value = notWeighed;
        }
        // bigger sinclair comes first
        return -lifter1Value.compareTo(lifter2Value);
    }
    
    /**
     * Compare smm.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareSmfForDelta(Athlete lifter1, Athlete lifter2) {
        Gender gender = lifter1.getGender();
        if (gender == null) {
            return -1;
        }
        int compare = gender.compareTo(lifter2.getGender());
        if (compare != 0) {
            return compare;
        }

        Double lifter1Value = lifter1.getSmfForDelta();
        Double lifter2Value = lifter2.getSmfForDelta();
        final Double notWeighed = 0D;
        if (lifter1Value == null) {
            lifter1Value = notWeighed;
        }
        if (lifter2Value == null) {
            lifter2Value = notWeighed;
        }
        // bigger sinclair comes first
        return -lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare start number.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareStartNumber(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getStartNumber();
        Integer lifter2Value = lifter2.getStartNumber();
        if (lifter1Value == null && lifter2Value == null) {
            return 0;
        }
        if (lifter1Value == null) {
            return -1;
        }
        if (lifter2Value == null) {
            return 1;
        }
        return lifter1Value.compareTo(lifter2Value);
    }

    /**
     * Compare total.
     *
     * @param lifter1 the lifter 1
     * @param lifter2 the lifter 2
     * @return the int
     */
    int compareTotal(Athlete lifter1, Athlete lifter2) {
        Integer lifter1Value = lifter1.getTotal();
        Integer lifter2Value = lifter2.getTotal();
        return lifter1Value.compareTo(lifter2Value);
    }

}
