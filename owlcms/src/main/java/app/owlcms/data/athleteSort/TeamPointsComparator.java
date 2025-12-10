/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Logger;

/**
 * This comparator sorts athletes within their team.
 *
 * @author jflamy
 */
public class TeamPointsComparator extends AbstractLifterComparator implements Comparator<Athlete> {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamPointsComparator.class);
	private Ranking rankingType;

	/**
	 * Instantiates a new team ranking comparator.
	 *
	 * @param rankingType the ranking type
	 */
	TeamPointsComparator(Ranking rankingType) {
		this.rankingType = rankingType;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Athlete lifter1, Athlete lifter2) {
		int compare = 0;

		compare = compareClub(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		compare = compareGender(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		compare = comparePointsOrder(lifter1, lifter2);
		if (compare != 0) {
			// bigger is better, so we reverse the order
			return -compare;
		}

		return compare;
	}

	/**
	 * Return ascending order of points. Will normally be reversed for scoring.
	 *
	 * @param lifter1
	 * @param lifter2
	 * @return
	 */
	private int comparePointsOrder(Athlete lifter1, Athlete lifter2) {
		switch (this.rankingType) {
			case SNATCH:
				return Integer.compare(lifter1.getSnatchPoints(), lifter2.getSnatchPoints());
			case CLEANJERK:
				return Integer.compare(lifter1.getCleanJerkPoints(), lifter2.getCleanJerkPoints());
			case TOTAL:
				final Integer totalPoints1 = lifter1.getTotalPoints();
				final Integer totalPoints2 = lifter2.getTotalPoints();
				final int compareTotal = totalPoints1.compareTo(totalPoints2);
				logger.trace(lifter1 + " " + totalPoints1 + " [" + compareTotal + "]" + lifter2 + " " + totalPoints2);
				return compareTotal;
			case CUSTOM:
				final Integer customPoints1 = lifter1.getCustomPoints();
				final Integer customPoints2 = lifter2.getCustomPoints();
				final int compareCustom = customPoints1.compareTo(customPoints2);
				logger.trace(
				        lifter1 + " " + customPoints1 + " [" + compareCustom + "]" + lifter2 + " " + customPoints2);
				return compareCustom;
			case SNATCH_CJ_TOTAL:
				final Integer combinedPoints1 = lifter1.getCombinedPoints();
				final Integer combinedPoints2 = lifter2.getCombinedPoints();
				final int compareCombined = combinedPoints1.compareTo(combinedPoints2);
				logger.trace(
				        lifter1 + " " + combinedPoints1 + " [" + compareCombined + "]" + lifter2 + " "
				                + combinedPoints2);
				return compareCombined;
			case BW_SINCLAIR:
				final Double sinclair1 = lifter1.getSinclairForDelta();
				final Double sinclair2 = lifter2.getSinclairForDelta();
				final int compareSinclair = sinclair1.compareTo(sinclair2);
//					logger.debug(
//					        lifter1.getAbbreviatedName() + " " + sinclair1 + " [" + compareSinclair + "] " + lifter2.getAbbreviatedName() + " " + sinclair2);
				return compareSinclair;
			case SMM:
				final Double smf1 = lifter1.getSmhfForDelta();
				final Double smf2 = lifter2.getSmhfForDelta();
				final int compareSmf = smf1.compareTo(smf2);
				logger.trace(
				        lifter1 + " " + smf1 + " [" + compareSmf + "]" + lifter2 + " " + smf2);
				return compareSmf;
			case ROBI:
				final Double robi1 = lifter1.getRobi();
				final Double robi2 = lifter2.getRobi();
				final int compareRobi = robi1.compareTo(robi2);
				logger.trace(
				        lifter1 + " " + robi1 + " [" + compareRobi + "]" + lifter2 + " " + robi2);
				return compareRobi;
		case GAMX:
			final Double gamx1 = lifter1.getGamx();
			final Double gamx2 = lifter2.getGamx();
			final int compareGamx = gamx1.compareTo(gamx2);
			logger.trace(
			        lifter1 + " " + gamx1 + " [" + compareGamx + "]" + lifter2 + " " + gamx2);
			return compareGamx;
		case GAMX_M:
			final Double gamxM1 = lifter1.getGamxM();
			final Double gamxM2 = lifter2.getGamxM();
			final int compareGamxM = gamxM1.compareTo(gamxM2);
			logger.trace(
			        lifter1 + " " + gamxM1 + " [" + compareGamxM + "]" + lifter2 + " " + gamxM2);
			return compareGamxM;
		case GAMX_U:
			final Double gamxU1 = lifter1.getGamxU();
			final Double gamxU2 = lifter2.getGamxU();
			final int compareGamxU = gamxU1.compareTo(gamxU2);
			logger.trace(
			        lifter1 + " " + gamxU1 + " [" + compareGamxU + "]" + lifter2 + " " + gamxU2);
			return compareGamxU;
		case GAMX_A:
			final Double gamxA1 = lifter1.getGamxA();
			final Double gamxA2 = lifter2.getGamxA();
			final int compareGamxA = gamxA1.compareTo(gamxA2);
			logger.trace(
			        lifter1 + " " + gamxA1 + " [" + compareGamxA + "]" + lifter2 + " " + gamxA2);
			return compareGamxA;
		default:
				break;
		}

		return 0;
	}

}
