/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.time.LocalDateTime;
import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Medal ordering.
 *
 * @author jflamy
 *
 */
public class WinningOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

	/** The Constant logger. */
	final static Logger logger = (Logger) LoggerFactory.getLogger(WinningOrderComparator.class);
	private boolean ignoreCategories;
	private Ranking rankingType;

	/**
	 * Instantiates a new winning order comparator.
	 *
	 * @param rankingType the ranking type
	 */
	public WinningOrderComparator(Ranking rankingType, boolean ignoreCategories) {
		this.rankingType = rankingType;
		this.ignoreCategories = ignoreCategories;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Athlete lifter1, Athlete lifter2) {
		switch (this.rankingType) {
			case SNATCH:
				return compareSnatchResultOrder(lifter1, lifter2, this.ignoreCategories);
			case CLEANJERK:
				return compareCleanJerkResultOrder(lifter1, lifter2, this.ignoreCategories);
			case TOTAL:
				return compareTotalResultOrder(lifter1, lifter2, this.ignoreCategories);
			case CATEGORY_SCORE:
				return compareScoreResultOrder(lifter1, lifter2, this.ignoreCategories);
			case CUSTOM:
				return compareCustomResultOrder(lifter1, lifter2, this.ignoreCategories);
			case ROBI:
				return compareRobiResultOrder(lifter1, lifter2);
			case CAT_SINCLAIR:
				return compareCatSinclairResultOrder(lifter1, lifter2);
			case BW_SINCLAIR:
				return compareSinclairResultOrder(lifter1, lifter2);
			case SMM:
				return compareSmhfResultOrder(lifter1, lifter2);
			case QPOINTS:
				return compareQPointsResultOrder(lifter1, lifter2);
			case GAMX:
				return compareGamxResultOrder(lifter1, lifter2);
			case AGEFACTORS:
				return compareAgeAdjustedTotalOrder(lifter1, lifter2);
			case QAGE:
				return compareQAgeResultOrder(lifter1, lifter2);
			case SNATCH_CJ_TOTAL:
				throw new UnsupportedOperationException("Unsupported ranking type " + this.rankingType);
		}
		return 0;
	}

	public int compareAgeAdjustedTotal(Athlete lifter1, Athlete lifter2) {
		Double lifter1Value = lifter1.getAgeAdjustedTotal();
		Double lifter2Value = lifter2.getAgeAdjustedTotal();
		final Double notWeighed = 0D;
		if (lifter1Value == null) {
			lifter1Value = notWeighed;
		}
		if (lifter2Value == null) {
			lifter2Value = notWeighed;
		}
		if (lifter1Value <= 0.0001 && lifter2Value < 0.0001) {
			// avoid going to full tie break; need something stable.
			return ObjectUtils.compare(lifter1.getId(), lifter2.getId());
		}
		// bigger adjusted total comes first
		return -lifter1Value.compareTo(lifter2Value);
	}

	public int compareAgeAdjustedTotalForDelta(Athlete lifter1, Athlete lifter2) {
		Double lifter1Value = lifter1.getAgeAdjustedTotalForDelta();
		Double lifter2Value = lifter2.getAgeAdjustedTotalForDelta();
		final Double notWeighed = 0D;
		if (lifter1Value == null) {
			lifter1Value = notWeighed;
		}
		if (lifter2Value == null) {
			lifter2Value = notWeighed;
		}
		if (lifter1Value <= 0.0001 && lifter2Value < 0.0001) {
			// avoid going to full tie break; need something stable.
			return ObjectUtils.compare(lifter1.getId(), lifter2.getId());
		}
		// bigger adjusted total comes first
		return -lifter1Value.compareTo(lifter2Value);
	}

	/**
	 * Determine who ranks first on AgeFactor-adjusted total.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareAgeAdjustedTotalOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;
		compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
		if (compare != 0) {
			return compare;
		}
		if (JXLSWorkbookStreamSource.isNoInterimScoresInResults()) {
			compare = compareAgeAdjustedTotal(lifter1, lifter2);
		} else {
			compare = compareAgeAdjustedTotalForDelta(lifter1, lifter2);
		}
		traceComparison("ageAdjustedTotal", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare;
		}

		return tieBreak(lifter1, lifter2);
	}

	/**
	 * Determine who ranks first. the Athlete who reached total first is ranked first.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareCatSinclairResultOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;

		compare = compareCategorySinclair(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		return tieBreak(lifter1, lifter2);
	}

	/**
	 * Compare clean jerk result order.
	 *
	 * @param lifter1          the lifter 1
	 * @param lifter2          the lifter 2
	 * @param ignoreCategories do not take categories into account
	 * @return the int
	 */
	public int compareCleanJerkResultOrder(Athlete lifter1, Athlete lifter2, boolean ignoreCategories) {
		int compare = 0;

		if (!ignoreCategories) {
			compare = compareCategory(lifter1, lifter2);
			traceComparison("compareCategory", lifter1.getAbbreviatedName(), lifter1.getCategoryCode(), lifter1.getAbbreviatedName(), lifter2.getCategoryCode(), compare);
			if (compare != 0) {
				return compare;
			}
		}

		compare = compareBestCleanJerk(lifter1, lifter2);
		if (compare != 0) {
			traceComparison("compareBestCleanJerk", lifter1.getAbbreviatedName(), lifter1.getBestCleanJerk(), lifter2.getAbbreviatedName(), lifter2.getBestCleanJerk(),
			        compare);
			return -compare; // smaller is less good
		}

		return tieBreak(lifter1, lifter2);
	}

	/**
	 * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
	 *
	 * This variant allows judges to award a score based on a formula, with bonuses or penalties, manually. Used for the U12 championship in Quebec.
	 *
	 * @param lifter1          the lifter 1
	 * @param lifter2          the lifter 2
	 * @param ignoreCategories do not take category into account
	 * @return the int
	 */
	public int compareCustomResultOrder(Athlete lifter1, Athlete lifter2, boolean ignoreCategories) {
		int compare = 0;

		if (!ignoreCategories) {
			compare = ObjectUtils.compare(lifter1.getCategory(), lifter2.getCategory(), true);
			traceComparison("!ignoreCategories", lifter1, lifter2, compare);
			if (compare != 0) {
				return compare;
			}
		}

		compare = compareCustomScore(lifter1, lifter2);
		traceComparison("customScore", lifter1, lifter2, compare);
		if (compare != 0) {
			return -compare; // we want reverse order - smaller comes after
		}

		compare = compareTotal(lifter1, lifter2);
		traceComparison("total", lifter1, lifter2, compare);
		if (compare != 0) {
			return -compare; // we want reverse order - smaller comes after
		}

		return tieBreak(lifter1, lifter2);
	}

	/**
	 * Determine who ranks first on GAMX points.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareGamxResultOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;
		compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
		if (compare != 0) {
			return compare;
		}
		compare = compareGamx(lifter1, lifter2);
		traceComparison("gamx", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare;
		}

		compare = compareBodyWeight(lifter1, lifter2);
		traceComparison("gamx compareBodyWeight", lifter1, lifter2, compare);
		return compare; // smaller Athlete wins
	}

	/**
	 * Determine who ranks first on QPoints points.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareQAgeResultOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;
		compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
		if (compare != 0) {
			return compare;
		}
		if (JXLSWorkbookStreamSource.isNoInterimScoresInResults()) {
			compare = compareQAge(lifter1, lifter2);
		} else {
			compare = compareQAgeForDelta(lifter1, lifter2);
		}
		traceComparison("qPoints", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare;
		}
		compare = compareBodyWeight(lifter1, lifter2);
		traceComparison("qage compareBodyWeight", lifter1, lifter2, compare);
		return compare; // smaller Athlete wins
	}

	/**
	 * Determine who ranks first on QPoints points.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareQPointsResultOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;
		compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
		if (compare != 0) {
			return compare;
		}
		if (JXLSWorkbookStreamSource.isNoInterimScoresInResults()) {
			compare = compareQPoints(lifter1, lifter2);
		} else {
			compare = compareQPointsForDelta(lifter1, lifter2);
		}
		traceComparison("qPoints", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare;
		}
		compare = compareBodyWeight(lifter1, lifter2);
		traceComparison("qpoints compareBodyWeight", lifter1, lifter2, compare);
		return compare; // smaller Athlete wins
	}

	/**
	 * Determine who ranks first on Robi points.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareRobiResultOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;

		compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
		if (compare != 0) {
			return compare;
		}

		compare = compareRobi(lifter1, lifter2);
		traceComparison("robi", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare;
		}
		compare = compareBodyWeight(lifter1, lifter2);
		traceComparison("robi compareBodyWeight", lifter1, lifter2, compare);
		return compare; // smaller Athlete wins
	}

	public int compareScoreResultOrder(Athlete lifter1, Athlete lifter2, boolean ignoreCategories) {
		int compare = 0;

		if (!ignoreCategories) {
			compare = ObjectUtils.compare(lifter1.getCategory(), lifter2.getCategory(), true);
			traceComparison("!ignoreCategories", lifter1, lifter2, compare);
			if (compare != 0) {
				return compare;
			}
		}

		compare = compareScore(lifter1, lifter2);
		if (compare != 0) {
			return -compare; // we want reverse order - smaller comes after
		}
		traceComparison("score", lifter1, lifter1.computedCategoryScore(), lifter2, lifter2.computedCategoryScore(), compare);

		return tieBreak(lifter1, lifter2);
	}

	/**
	 * Determine who ranks first on Sinclair points.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareSinclairResultOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;
		compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
		if (compare != 0) {
			return compare;
		}
		if (JXLSWorkbookStreamSource.isNoInterimScoresInResults()) {
			compare = compareSinclair(lifter1, lifter2);
		} else {
			compare = compareSinclairForDelta(lifter1, lifter2);
		}
		traceComparison("sinclair", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare;
		}
		compare = compareBodyWeight(lifter1, lifter2);
		traceComparison("sinclair compareBodyWeight", lifter1, lifter2, compare);
		return compare; // smaller Athlete wins
	}

	/**
	 * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
	 *
	 * @param lifter1 the lifter 1
	 * @param lifter2 the lifter 2
	 * @return the int
	 */
	public int compareSmhfResultOrder(Athlete lifter1, Athlete lifter2) {
		int compare = 0;
		compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
		if (compare != 0) {
			return compare;
		}

		if (JXLSWorkbookStreamSource.isNoInterimScoresInResults()) {
			compare = compareSmhf(lifter1, lifter2);
		} else {
			compare = compareSmhfForDelta(lifter1, lifter2);
		}

		traceComparison("smhf", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare;
		}
		compare = compareBodyWeight(lifter1, lifter2);
		traceComparison("smm compareBodyWeight", lifter1, lifter2, compare);
		return compare; // smaller Athlete wins
	}

	/**
	 * Compare snatch result order.
	 *
	 * @param lifter1          the lifter 1
	 * @param lifter2          the lifter 2
	 * @param ignoreCategories do not take categories into account
	 * @return the int
	 */
	public int compareSnatchResultOrder(Athlete lifter1, Athlete lifter2, boolean ignoreCategories) {
		boolean trace = false;
		int compare = 0;

		if (trace) {
			logger.trace("lifter1 {};  lifter2 {}", lifter1.getFirstName(), lifter2.getFirstName());
		}

		if (!ignoreCategories) {
			compare = compareCategory(lifter1, lifter2);
			traceComparison("snatch category", lifter1.getLastName(), lifter1.getCategoryCode(), lifter2.getLastName(), lifter1.getCategoryCode(), compare);
			if (compare != 0) {
				return compare;
			}
		}

		compare = compareBestSnatch(lifter1, lifter2);
		traceComparison("snatch best snatch", lifter1, lifter2, compare);
		if (compare != 0) {
			return -compare; // smaller snatch is less good
		}

		if (lifter1 != null && lifter2 != null && lifter1.getGroup() != lifter2.getGroup()) {
			compare = compareBestSnatchTime(lifter1, lifter2);
			traceComparison("snatch best snatch time", lifter1, lifter1.getBestSnatchAttemptTime(), lifter2, lifter2.getBestSnatchAttemptTime(), compare);
			if (compare != 0) {
				//logger.debug("compare {}",compare);
				// <0 means lifter1 earlier than lifter2
				return compare; // earlier is better, rank 1 is better than rank 2
			}
		}

		compare = compareCompetitionSessionTime(lifter1, lifter2);
		traceComparison("compareCompetitionSessionTime", lifter1, lifter2, compare);
		if (compare != 0) {
			// <0 means lifter1 earlier than lifter2
			return compare; // earlier is better, rank 1 is better than rank 2
		}

		compare = compareBestSnatchAttemptNumber(lifter1, lifter2);
		traceComparison("best snatch attempt number", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare; // earlier best attempt wins
		}

		compare = comparePreviousAttempts(lifter1.getBestSnatchAttemptNumber(), false, lifter1, lifter2);
		traceComparison("snatch previous attempt", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare; // compare attempted weights (prior to
			                // best attempt), smaller first
		}

		compare = compareStartNumber(lifter1, lifter2);
		traceComparison("start number", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare; // if equality within a group, smallest lot number wins
		}

		return compare;
	}

	/**
	 * Determine who ranks first. If the body weights are the same, the Athlete who reached total first is ranked first.
	 *
	 * @param lifter1          the lifter 1
	 * @param lifter2          the lifter 2
	 * @param ignoreCategories do not take categories into account
	 * @return the int
	 */
	public int compareTotalResultOrder(Athlete lifter1, Athlete lifter2, boolean ignoreCategories) {
		int compare = 0;

		if (!ignoreCategories) {
			compare = compareAgeGroup(lifter1, lifter2);
			traceComparison("compareAgeGroup", lifter1.getShortName(), lifter1.getAgeGroupDisplayName(), lifter1.getShortName(),
			        lifter2.getAgeGroupDisplayName(), compare);
			if (compare != 0) {
				return compare;
			}
			compare = compareCategory(lifter1, lifter2);
			traceComparison("categories", lifter1, lifter2, compare);
			if (compare != 0) {
				return compare;
			}
		}

		compare = compareTotal(lifter1, lifter2);
		traceComparison("compareTotal", lifter1, lifter2, compare);
		if (compare != 0) {
			return -compare; // we want reverse order - smaller comes after
		}

		return tieBreak(lifter1, lifter2);
	}

	/**
	 * Compare Q-Points.
	 */
	int compareQPoints(Athlete lifter1, Athlete lifter2) {
		Double lifter1Value = lifter1.getQPoints();
		Double lifter2Value = lifter2.getQPoints();
		final Double notWeighed = 0D;
		if (lifter1Value == null) {
			lifter1Value = notWeighed;
		}
		if (lifter2Value == null) {
			lifter2Value = notWeighed;
		}
		// bigger QPoints comes first
		int compare;
		compare = -lifter1Value.compareTo(lifter2Value);
		// traceComparison("qpoints", lifter1, lifter2, compare);
		return compare;
	}

	/**
	 * Compare Q-Points.
	 */
	int compareQPointsForDelta(Athlete lifter1, Athlete lifter2) {
		Double lifter1Value = lifter1.getQPointsForDelta();
		Double lifter2Value = lifter2.getQPointsForDelta();
		final Double notWeighed = 0D;
		if (lifter1Value == null) {
			lifter1Value = notWeighed;
		}
		if (lifter2Value == null) {
			lifter2Value = notWeighed;
		}
		// bigger QPoints comes first
		int compare;
		compare = -lifter1Value.compareTo(lifter2Value);
		// traceComparison("qpoints", lifter1, lifter2, compare);
		return compare;
	}

	private int compareBestCleanJerkTime(Athlete lifter1, Athlete lifter2) {
		LocalDateTime bestCleanJerkAttemptTime1 = lifter1.getBestCleanJerkAttemptTime();
		LocalDateTime bestCleanJerkAttemptTime2 = lifter2.getBestCleanJerkAttemptTime();
		if (bestCleanJerkAttemptTime1 == null || bestCleanJerkAttemptTime2 == null) {
			// we will rely on session time.
			return 0;
		}
		// logger.trace("tieBreak {} {}={} {} {}={} {}", LoggerUtils.stackTrace(),
		// lifter1.getShortName(), lifter1.getBestCleanJerk(), bestCleanJerkAttemptTime1,
		// lifter2.getShortName(), lifter2.getBestCleanJerk(), bestCleanJerkAttemptTime2);
		int compare = ObjectUtils.compare(bestCleanJerkAttemptTime1, bestCleanJerkAttemptTime2);
		// traceComparison("best clean jerk ", lifter1, lifter2, compare);
		return compare;
	}

	private int compareBestSnatchTime(Athlete lifter1, Athlete lifter2) {
		LocalDateTime bestSnatchAttemptTime1 = lifter1.getBestSnatchAttemptTime();
		LocalDateTime bestSnatchAttemptTime2 = lifter2.getBestSnatchAttemptTime();
		if (bestSnatchAttemptTime1 == null || bestSnatchAttemptTime2 == null) {
			// we will rely on session time.
			logger.error("bestSnatchTime missing {}={} {}={}", lifter1.getAbbreviatedName(), bestSnatchAttemptTime1, lifter2.getAbbreviatedName(), bestSnatchAttemptTime2);
			return 0;
		}
		return ObjectUtils.compare(bestSnatchAttemptTime1, bestSnatchAttemptTime2);
	}

	/**
	 * Compare competition session start times for two athletes. A null session time is considered to be at the beginning of time, earlier than any non-null
	 * time.
	 *
	 * @param lifter1
	 * @param lifter2
	 * @return -1 if lifter1 was part of earlier group, 0 if same group, 1 if lifter1 lifted in later group
	 */
	private int compareCompetitionSessionTime(Athlete lifter1, Athlete lifter2) {
		Group group1 = lifter1.getGroup();
		Group group2 = lifter2.getGroup();
		if (group1 == null && group2 == null) {
			return 0;
		}
		if (group1 == null) {
			return -1;
		}
		if (group2 == null) {
			return 1;
		}
		LocalDateTime competitionTime1 = group1.getCompetitionTime();
		LocalDateTime competitionTime2 = group2.getCompetitionTime();
		if (competitionTime1 == null && competitionTime2 == null) {
			return 0;
		}
		if (competitionTime1 == null) {
			return -1;
		}
		if (competitionTime2 == null) {
			return 1;
		}
		return competitionTime1.compareTo(competitionTime2);
	}

	/**
	 * Processing shared between all coefficient-based rankings
	 *
	 * @param lifter1
	 * @param lifter2
	 * @return
	 */
	private int tieBreak(Athlete lifter1, Athlete lifter2) {
		int compare;

		// if the athletes were not in the same session
		if (lifter1 != null && lifter2 != null && !sameGroup(lifter1, lifter2)) {
			compare = compareBestCleanJerkTime(lifter1, lifter2);
			Group group1 = lifter1.getGroup();
			Group group13 = lifter2.getGroup();
			// if (lifter1.getCategory().getCode().equals("Open_F64") && lifter1.getTotal() > 210) {
			traceComparison("tiebreak compareBestCleanJerkTime", lifter1, group1, lifter2, group13, compare);
			// }
			if (compare != 0) {
				// <0 means lifter1 earlier than lifter2
				return compare; // earlier is better, rank 1 is better than rank 2
			}
		}

		// earlier session wins (redundant given previous test)
		if (lifter1 != null && lifter2 != null && !sameGroup(lifter1, lifter2)) {
			compare = compareCompetitionSessionTime(lifter1, lifter2);
			Group group12 = lifter1.getGroup();
			Group group13 = lifter2.getGroup();
			if (group12 != null && group13 != null)
				// if (lifter1.getCategory().getCode().equals("Open_F64") && lifter1.getTotal() > 210) {
				traceComparison("tiebreak compareCompetitionSessionTime", lifter1, group12.getCompetitionTime(), lifter2, group13.getCompetitionTime(),
				        compare);
			// }
			if (compare != 0) {
				return compare; // <0 = earlier group that should win, so tiebreak needs to return negative (rank 1 is better than rank 2)
			}
		}

		// for total, must compare best clean and jerk value and smaller is better
		// because the total was reached earlier.
		// if this routine called to tiebreak cj ranking, the result will be 0 so this
		// test is harmless
		compare = compareBestCleanJerk(lifter1, lifter2);
		traceComparison("tiebreak compareBestCleanJerk", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare; // smaller cj, when total is the same, means total was reached earlier.
		}

		// same clean and jerk, earlier attempt wins
		compare = compareBestCleanJerkAttemptNumber(lifter1, lifter2);
		traceComparison("tiebreak compareBestCleanJerkAttemptNumber", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare; // earlier best attempt wins
		}

		// determine who lifted best clean and jerk first
		compare = comparePreviousAttempts(lifter1.getBestCleanJerkAttemptNumber(), true, lifter1, lifter2);
		traceComparison("tiebreak comparePreviousAttempts", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare; // compare attempted weights (prior to best attempt), smaller first
		}

		// if equality within a group, smallest lot number wins (same session, same
		// category, same weight, same attempt) -- smaller lot lifted first.
		compare = compareStartNumber(lifter1, lifter2);
		traceComparison("tiebreak compareStartNumber", lifter1, lifter2, compare);
		if (compare != 0) {
			return compare; // compare attempted weights (prior to best attempt), smaller first
		}

		// if no lot number, we get weird results. we need a stable comparison
		compare = ObjectUtils.compare(lifter1.getId(), lifter2.getId());
		return compare;

	}

	public boolean sameGroup(Athlete lifter1, Athlete lifter2) {
		var g1 = lifter1.getGroup();
		var g2 = lifter2.getGroup();
		if (g1 == null || g2 == null) {
			return false;
		}
		return g1.equals(g2);
	}

	private void traceComparison(String where, Athlete lifter1, Athlete lifter2, int compare) {
		if (logger.isTraceEnabled()) {
			logger./**/warn("{} {} {} {} {}", where, lifter1, (compare < 0 ? "<" : (compare == 0 ? "=" : ">")), lifter2,
			        LoggerUtils.whereFrom(1));
		}
	}

	@SuppressWarnings("unused")
	private void doTraceComparison(String where, Athlete lifter1, Athlete lifter2, int compare) {
		logger./**/warn("{} {} {} {} {}", where, lifter1, (compare < 0 ? "<" : (compare == 0 ? "=" : ">")), lifter2,
		        LoggerUtils.whereFrom(1));
	}

}
