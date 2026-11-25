/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.scoring;

import java.util.function.IntToDoubleFunction;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;

public final class ScoreTarget {

	private static final QPoints QPOINTS = new QPoints(2025);

	private ScoreTarget() {
	}

	public static int kgDifference(Athlete athlete, double targetScore, Ranking ranking) {
		if (athlete == null || ranking == null || !Double.isFinite(targetScore)) {
			throw new IllegalArgumentException("Score target calculation requires an athlete, ranking and finite target");
		}
		if (targetScore <= 0.0D) {
			return 0;
		}

		int currentTotal = currentWeight(athlete, ranking);
		return switch (ranking) {
			case BW_SINCLAIR -> linearTotalKgDifference(targetScore, currentTotal,
			        athlete.getSinclairFactor(), w -> w * athlete.getSinclairFactor());
			case CAT_SINCLAIR -> linearTotalKgDifference(targetScore, currentTotal,
			        athlete.getCatSinclairFactor(), w -> w * athlete.getCatSinclairFactor());
			case QPOINTS -> {
				double factor = qPointsFactor(athlete);
				yield linearTotalKgDifference(targetScore, currentTotal, factor, w -> w * factor);
			}
			case CAT_QPOINTS -> linearTotalKgDifference(targetScore, currentTotal,
			        athlete.getCatQPointsFactor(), w -> w * athlete.getCatQPointsFactor());
			case QAGE -> {
				double qFactor = qPointsFactor(athlete);
				Float ageFactor = athlete.getQMastersFactor();
				double combined = ageFactor != null ? ageFactor * qFactor : 0.0D;
				yield linearTotalKgDifference(targetScore, currentTotal, combined,
				        w -> (w * qFactor) * ageFactor);
			}
			case SMM -> {
				double sinclairFactor = mastersSinclairFactor(athlete);
				Float ageFactor = athlete.getSmhfFactor();
				double combined = ageFactor != null ? ageFactor * sinclairFactor : 0.0D;
				yield linearTotalKgDifference(targetScore, currentTotal, combined,
				        w -> (w * sinclairFactor) * ageFactor);
			}
			case AGEFACTORS -> linearTotalKgDifference(targetScore, currentTotal,
			        (double) AgeFactors.getAgeAdjustedTotal(athlete, 1),
			        w -> (double) AgeFactors.getAgeAdjustedTotal(athlete, w));
			case GAMX, GAMX_M, GAMX_MS, GAMX_MC, GAMX_U, GAMX_A, GAMX_S, GAMX_C, CAT_GAMX ->
				gamxKgDifference(athlete, targetScore, ranking);
			case ROBI -> robiKgDifference(athlete, targetScore);
			default -> 0;
		};
	}

	private static int linearTotalKgDifference(double targetScore, int currentTotal, Double scorePerKg,
	        IntToDoubleFunction scoreAtWeight) {
		if (scorePerKg == null || !Double.isFinite(scorePerKg) || scorePerKg <= 0.0D) {
			return 0;
		}
		int targetWeight = Math.max(currentTotal, (int) Math.ceil(targetScore / scorePerKg));
		while (scoreAtWeight.applyAsDouble(targetWeight) <= targetScore) {
			targetWeight++;
		}
		while (targetWeight > currentTotal && scoreAtWeight.applyAsDouble(targetWeight - 1) > targetScore) {
			targetWeight--;
		}
		return Math.max(0, targetWeight - currentTotal);
	}

	private static double qPointsFactor(Athlete athlete) {
		Gender gender = athlete.getGender();
		Double bodyWeight = athlete.getBodyWeight();
		if (gender == null || bodyWeight == null || bodyWeight <= 0.0D) {
			return 0.0D;
		}
		Double factor = QPOINTS.qPointsFactor(gender, bodyWeight);
		return factor != null ? factor : 0.0D;
	}

	private static int gamxKgDifference(Athlete athlete, double targetScore, Ranking ranking) {
		int targetWeight = GAMX2.kgTarget(athlete, targetScore, ranking);
		return targetWeight > 0 ? Math.max(0, targetWeight - currentWeight(athlete, ranking)) : 0;
	}

	public static int linearKgDifference(double targetScore, double currentScore, double scorePerKg) {
		if (!Double.isFinite(targetScore) || !Double.isFinite(currentScore)
		        || !Double.isFinite(scorePerKg) || scorePerKg <= 0.0D) {
			throw new IllegalArgumentException(
			        "Linear score target calculation requires finite scores and a positive score per kg");
		}
		int difference = (int) Math.max(0, Math.ceil((targetScore - currentScore) / scorePerKg));
		while (currentScore + difference * scorePerKg <= targetScore) {
			difference++;
		}
		while (difference > 0 && currentScore + (difference - 1) * scorePerKg > targetScore) {
			difference--;
		}
		return difference;
	}

	private static int currentWeight(Athlete athlete, Ranking ranking) {
		return switch (ranking) {
			case GAMX_S, GAMX_MS -> athlete.getBestSnatch();
			case GAMX_C, GAMX_MC -> athlete.getBestCleanJerk();
			default -> athlete.getBestSnatch() + athlete.getBestCleanJerk();
		};
	}

	private static double mastersSinclairFactor(Athlete athlete) {
		Gender gender = athlete.getGender();
		Double bodyWeight = athlete.getBodyWeight();
		if (gender == null || bodyWeight == null || bodyWeight <= 0.0D) {
			return 0.0D;
		}
		athlete.getSmhfForDelta();
		var coefficients = Athlete.sinclairProperties2020;
		return switch (gender) {
			case M -> Athlete.sinclairFactor(bodyWeight, coefficients.menCoefficient(),
			        coefficients.menMaxWeight());
			case F -> Athlete.sinclairFactor(bodyWeight, coefficients.womenCoefficient(),
			        coefficients.womenMaxWeight());
			case I, MF -> 0.0D;
		};
	}

	private static int robiKgDifference(Athlete athlete, double targetScore) {
		Integer robiWr = athlete.getRobiWr();
		if (robiWr == null || robiWr <= 0 || targetScore <= 0.0D) {
			return 0;
		}
		double A = 1000.0D / Math.pow(robiWr, Category.ROBI_B);
		int current = currentWeight(athlete, Ranking.ROBI);
		int targetWeight = Math.max(1, (int) Math.ceil(Math.pow(targetScore / A, 1.0D / Category.ROBI_B)));
		while (A * Math.pow(targetWeight, Category.ROBI_B) <= targetScore) {
			targetWeight++;
		}
		while (targetWeight > 1 && A * Math.pow(targetWeight - 1, Category.ROBI_B) > targetScore) {
			targetWeight--;
		}
		return Math.max(0, targetWeight - current);
	}
}
