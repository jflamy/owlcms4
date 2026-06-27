/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.scoring;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * Compute GAMX scores using the Box-Cox Cole and Green (BCCG) distribution.
 * 
 * Formula: GAMX = qnorm(pBCCG(total, mu, sigma, nu)) * 100 + 1000
 * 
 * This implementation supports four parameter variants:
 * - SENIOR: Senior parameters (params-*-sen-*.json) - standard GAMX
 * - AGE_ADJUSTED: Age-adjusted parameters (params-total-age-*.json) - GAMX-A
 * - U17: U17 parameters (params-total-u17-*.json) - GAMX-U
 * - MASTERS: Masters athlete parameters (params-*-mas-*.json) - GAMX-M
 * 
 * Parameters (mu, sigma, nu) are interpolated from JSON tables based on body mass.
 */
public class GAMX2 {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(GAMX2.class);
	private static final NormalDistribution NORMAL = new NormalDistribution(0, 1);

	/**
	 * Lift types (TOTAL, SNATCH, CJ)
	 */
	public enum Lift {
		TOTAL,    // Total (3 lifts combined) - params-total-*.json
		SNATCH,   // Snatch only - params-snatch-*.json
		CJ        // Clean & Jerk only - params-cj-*.json
	}

	/**
	 * Parameter set variants
	 */
	public enum Variant {
		SENIOR,   // Senior parameters (params-*-sen) - standard GAMX
		AGE_ADJUSTED,      // Age-Adjusted IWF Data 13-40 (params-total-age) - GAMX-A
		U17,      // U17 parameters (params-total-u17) - GAMX-U
		MASTERS   // Masters parameters (params-*-mas) - GAMX-M
	}

	/**
	 * A single row from the parameter JSON file
	 */
	private static class ParamRow {
		final double age;        // normalized to 25.0 for SENIOR variant
		final double bodyMass;
		final double mu;
		final double sigma;
		final double nu;

		ParamRow(double age, double bodyMass, double mu, double sigma, double nu) {
			this.age = age;
			this.bodyMass = bodyMass;
			this.mu = mu;
			this.sigma = sigma;
			this.nu = nu;
		}
	}

	/**
	 * Normalized age for SENIOR variant (used for efficient binary search)
	 */
	private static final double SENIOR_AGE = 25.0;

	/**
	 * Result of parameter interpolation
	 */
	private static class InterpolatedParams {
		final double mu;
		final double sigma;
		final double nu;
		final boolean success;

		InterpolatedParams(double mu, double sigma, double nu) {
			this.mu = mu;
			this.sigma = sigma;
			this.nu = nu;
			this.success = true;
		}

		InterpolatedParams() {
			this.mu = 0;
			this.sigma = 0;
			this.nu = 0;
			this.success = false;
		}
	}

	// Cache: lift -> variant -> gender -> ArrayList of ParamRow (3-level hierarchy)
	private static final Map<Lift, Map<Variant, Map<Gender, ArrayList<ParamRow>>>> parameterCache = 
		new EnumMap<>(Lift.class);

	/**
	 * Compute GAMX score for an athlete using the default SENIOR variant.
	 * 
	 * @param a            the athlete
	 * @param liftedWeight total lifted in kg
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double getGamx(Athlete a, Integer liftedWeight) {
		return getGamx(a, liftedWeight, Variant.SENIOR);
	}

	/**
	 * Compute GAMX-A (Age-Adjusted) score for an athlete.
	 * 
	 * @param a            the athlete
	 * @param liftedWeight total lifted in kg
	 * @return GAMX-A score, or 0.0 if inputs invalid
	 */
	public static double getGamxA(Athlete a, Integer liftedWeight) {
		return getGamx(a, liftedWeight, Variant.AGE_ADJUSTED);
	}

	/**
	 * Compute GAMX-U (U17) score for an athlete.
	 * 
	 * @param a            the athlete
	 * @param liftedWeight total lifted in kg
	 * @return GAMX-U score, or 0.0 if inputs invalid
	 */
	public static double getGamxU(Athlete a, Integer liftedWeight) {
		return getGamx(a, liftedWeight, Variant.U17);
	}

	/**
	 * Compute GAMX-M (Masters) score for an athlete.
	 * 
	 * @param a            the athlete
	 * @param liftedWeight total lifted in kg
	 * @return GAMX-M score, or 0.0 if inputs invalid
	 */
	public static double getGamxM(Athlete a, Integer liftedWeight) {
		return getGamx(a, liftedWeight, Variant.MASTERS);
	}

	/**
	 * Compute GAMX-Snatch score for an athlete using SNATCH parameters.
	 * 
	 * @param a            the athlete
	 * @param snatchWeight snatch weight lifted in kg
	 * @return GAMX-Snatch score, or 0.0 if inputs invalid
	 */
	public static double getGamxSnatch(Athlete a, Integer snatchWeight) {
		if (a == null || snatchWeight == null || snatchWeight <= 0) {
			return 0.0;
		}
		Gender gender = a.getGender();
		Double bodyMass = a.getBodyWeight();
		Double age = a.getAge() != null ? a.getAge().doubleValue() : null;
		return computeGamxForLift(gender, age, bodyMass, snatchWeight, Variant.SENIOR, Lift.SNATCH);
	}

	/**
	 * Compute GAMX-CJ (Clean & Jerk) score for an athlete using CJ parameters.
	 * 
	 * @param a            the athlete
	 * @param cjWeight     clean & jerk weight lifted in kg
	 * @return GAMX-CJ score, or 0.0 if inputs invalid
	 */
	public static double getGamxCJ(Athlete a, Integer cjWeight) {
		if (a == null || cjWeight == null || cjWeight <= 0) {
			return 0.0;
		}
		Gender gender = a.getGender();
		Double bodyMass = a.getBodyWeight();
		Double age = a.getAge() != null ? a.getAge().doubleValue() : null;
		return computeGamxForLift(gender, age, bodyMass, cjWeight, Variant.SENIOR, Lift.CJ);
	}

	/**
	 * Compute GAMX score for an athlete.
	 * 
	 * @param a            the athlete
	 * @param liftedWeight total lifted in kg
	 * @param variant      which parameter set to use
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double getGamx(Athlete a, Integer liftedWeight, Variant variant) {
		if (a == null || liftedWeight == null || liftedWeight <= 0) {
			return 0.0;
		}
		Gender gender = a.getGender();
		Double bodyMass = a.getBodyWeight();
		Double age = a.getAge() != null ? a.getAge().doubleValue() : null;
		return computeGamx(gender, age, bodyMass, liftedWeight, variant);
	}

	/**
	 * Compute GAMX score for an athlete using a custom body mass (e.g., category boundary weight).
	 * This is used for category-normalized GAMX scores (CAT_GAMX).
	 * 
	 * @param a            the athlete (for gender and age)
	 * @param liftedWeight total lifted in kg
	 * @param bodyMass     custom body mass in kg (e.g., category maximum weight)
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double getGamx(Athlete a, Integer liftedWeight, Double bodyMass) {
		return getGamx(a, liftedWeight, bodyMass, Variant.SENIOR);
	}

	/**
	 * Compute GAMX score for an athlete using a custom body mass and variant.
	 * This is used for category-normalized GAMX scores (CAT_GAMX).
	 * 
	 * @param a            the athlete (for gender and age)
	 * @param liftedWeight total lifted in kg
	 * @param bodyMass     custom body mass in kg (e.g., category maximum weight)
	 * @param variant      which parameter set to use
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double getGamx(Athlete a, Integer liftedWeight, Double bodyMass, Variant variant) {
		if (a == null || liftedWeight == null || liftedWeight <= 0 || bodyMass == null || bodyMass <= 0) {
			return 0.0;
		}
		Gender gender = a.getGender();
		Double age = a.getAge() != null ? a.getAge().doubleValue() : null;
		return computeGamx(gender, age, bodyMass, liftedWeight, variant);
	}

	/**
	 * Compute GAMX score using default SENIOR variant.
	 * 
	 * @param gender   Gender.MALE or Gender.FEMALE
	 * @param bodyMass body mass in kg
	 * @param total    lifted total in kg
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double computeGamx(Gender gender, Double bodyMass, int total) {
		return computeGamx(gender, SENIOR_AGE, bodyMass, total, Variant.SENIOR);
	}

	/**
	 * Compute GAMX score.
	 * 
	 * @param gender   Gender.MALE or Gender.FEMALE
	 * @param bodyMass body mass in kg
	 * @param total    lifted total in kg
	 * @param variant  which parameter set to use
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double computeGamx(Gender gender, Double bodyMass, int total, Variant variant) {
		return computeGamx(gender, null, bodyMass, total, variant);
	}

	/**
	 * Compute GAMX score with age parameter (internal, for TOTAL lift only).
	 * 
	 * @param gender   Gender.MALE or Gender.FEMALE
	 * @param age      athlete's age (required for AGE_ADJUSTED, U17, MASTERS; ignored for SENIOR)
	 * @param bodyMass body mass in kg
	 * @param total    lifted total in kg
	 * @param variant  which parameter set to use
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double computeGamx(Gender gender, Double age, Double bodyMass, int total, Variant variant) {
		return computeGamxForLift(gender, age, bodyMass, total, variant, Lift.TOTAL);
	}

	/**
	 * Compute GAMX score for a specific lift (TOTAL, SNATCH, or CJ).
	 * Internal method supporting all lift types and variants.
	 * 
	 * @param gender   Gender.MALE or Gender.FEMALE
	 * @param age      athlete's age (required for AGE_ADJUSTED, U17, MASTERS; ignored for SENIOR)
	 * @param bodyMass body mass in kg
	 * @param weight   lifted weight in kg
	 * @param variant  which parameter set to use
	 * @param lift     which lift type to use (TOTAL, SNATCH, or CJ)
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	private static double computeGamxForLift(Gender gender, Double age, Double bodyMass, int weight, Variant variant, Lift lift) {
		if (gender == null || bodyMass == null || bodyMass <= 0 || weight <= 0) {
			return 0.0;
		}

		// Ensure parameters are loaded
		loadParametersForLift(variant, lift);

		// Age handling: SENIOR always uses 25.0, other variants require actual age
		double normalizedAge;
		if (variant == Variant.SENIOR) {
			normalizedAge = SENIOR_AGE; // Always 25.0 for SENIOR, regardless of age parameter
		} else {
			if (age == null || age <= 0) {
				logger.error("Age required for variant {} but not provided", variant);
				return 0.0;
			}
			normalizedAge = age;
		}

		// Get parameter table for this lift and gender
		Map<Variant, Map<Gender, ArrayList<ParamRow>>> variantMap = parameterCache.get(lift);
		if (variantMap == null) {
			logger.error("No parameters loaded for lift {}", lift);
			return 0.0;
		}

		Map<Gender, ArrayList<ParamRow>> genderMap = variantMap.get(variant);
		if (genderMap == null) {
			logger.error("No parameters loaded for lift {} variant {}", lift, variant);
			return 0.0;
		}

		ArrayList<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			logger.error("No parameters for gender {} lift {} variant {}", gender, lift, variant);
			return 0.0;
		}

		// Interpolate mu, sigma, nu from age and body mass
		InterpolatedParams interp = interpolateParams(params, normalizedAge, bodyMass);
		if (!interp.success) {
			return 0.0;
		}

		// Compute GAMX score
		return computeGamxCore(weight, interp.mu, interp.sigma, interp.nu);
	}

	/**
	 * Find the minimum total needed to strictly exceed a target GAMX score at 2 decimal precision.
	 * 
	 * This ensures no ties: if two athletes have identical age, gender, and bodyweight, and one
	 * achieves GAMX score X, the other needs kgTarget(X) to guarantee a win (not just a tie).
	 * 
	 * @param gender      Gender.MALE or Gender.FEMALE
	 * @param age         athlete's age (required for AGE_ADJUSTED, U17, MASTERS; ignored for SENIOR)
	 * @param targetScore the target GAMX score (must strictly exceed this at 2 decimal precision)
	 * @param bodyMass    body mass in kg
	 * @param variant     which parameter set to use
	 * @param lift        which lift dimension (TOTAL, SNATCH, CJ)
	 * @return minimum total in kg that strictly exceeds targetScore, or 0 if impossible
	 */
	public static int kgTarget(Gender gender, Double age, double targetScore, double bodyMass, Variant variant, Lift lift) {
		if (gender == null || bodyMass <= 0) {
			return 0;
		}

		// Ensure parameters are loaded for the requested lift
		loadParametersForLift(variant, lift);

		Map<Variant, Map<Gender, ArrayList<ParamRow>>> variantMap = parameterCache.get(lift);
		if (variantMap == null) {
			return 0;
		}

		Map<Gender, ArrayList<ParamRow>> genderMap = variantMap.get(variant);
		if (genderMap == null) {
			return 0;
		}

		ArrayList<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			return 0;
		}

		// Age handling: SENIOR always uses 25.0, other variants require actual age
		double normalizedAge;
		if (variant == Variant.SENIOR) {
			normalizedAge = SENIOR_AGE; // Always 25.0 for SENIOR, regardless of age parameter
		} else {
			if (age == null || age <= 0) {
				logger.error("Age required for variant {} but not provided", variant);
				return 0;
			}
			normalizedAge = age;
		}

		InterpolatedParams interp = interpolateParams(params, normalizedAge, bodyMass);
		if (!interp.success) {
			logger.error("kgTarget: interpolateParams failed for variant={}, gender={}, normalizedAge={}, bodyMass={}", 
					variant, gender, normalizedAge, bodyMass);
			return 0;
		}

		// Convert target GAMX score to probability
		// GAMX = qnorm(p) * 100 + 1000, so p = pnorm((GAMX - 1000) / 100)
		double z = (targetScore - 1000.0) / 100.0;
		double p = NORMAL.cumulativeProbability(z);

		// Compute initial total using qBCCG (gives a hint for binary search)
		double formulaResult = qBCCG(p, interp.mu, interp.sigma, interp.nu);

		logger.debug("kgTarget: variant={}, gender={}, age={}, normalizedAge={}, bodyMass={}, targetScore={}, z={}, p={}, mu={}, sigma={}, nu={}, formulaResult={}",
				variant, gender, age, normalizedAge, bodyMass, targetScore, z, p, interp.mu, interp.sigma, interp.nu, formulaResult);

		if (Double.isNaN(formulaResult) || Double.isInfinite(formulaResult) || formulaResult <= 0) {
			logger.error("kgTarget: qBCCG returned invalid result: formulaResult={}, p={}, mu={}, sigma={}, nu={}, variant={}, gender={}, age={}, bodyMass={}",
					formulaResult, p, interp.mu, interp.sigma, interp.nu, variant, gender, age, bodyMass);
			return 0;
		}

		// Start with ceiling of formula result (likely exceeds target)
		int candidate = (int) Math.ceil(formulaResult);

		// Round target to 2 decimal places for comparison
		double targetRounded = Math.round(targetScore * 100.0) / 100.0;

		// If candidate doesn't exceed, increment until it does
		while (candidate < 600) { // reasonable upper bound
			double gamxAtCandidate = computeGamxCore(candidate, interp.mu, interp.sigma, interp.nu);
			double gamxRounded = Math.round(gamxAtCandidate * 100.0) / 100.0;
			if (gamxRounded > targetRounded) {
				break; // Found a value that exceeds
			}
			candidate++;
		}

		// Now decrement to find the minimum that still exceeds
		while (candidate > 1) {
			int test = candidate - 1;
			double gamxAtTest = computeGamxCore(test, interp.mu, interp.sigma, interp.nu);
			double gamxRounded = Math.round(gamxAtTest * 100.0) / 100.0;
			if (gamxRounded > targetRounded) {
				candidate = test; // Still exceeds, keep going lower
			} else {
				break; // test doesn't exceed, candidate is the minimum
			}
		}

		return candidate;
	}

	/**
	 * Find the total needed to achieve a target GAMX score using iterative binary search.
	 * 
	 * @param gender      Gender.MALE or Gender.FEMALE
	 * @param age         athlete's age (required for AGE_ADJUSTED, U17, MASTERS; ignored for SENIOR)
	 * @param targetScore the target GAMX score
	 * @param bodyMass    body mass in kg
	 * @param variant     which parameter set to use
	 * @param lift        which lift dimension (TOTAL, SNATCH, CJ)
	 * @return total in kg needed, or 0 if impossible
	 */
	public static int kgTargetIterative(Gender gender, Double age, double targetScore, double bodyMass, Variant variant, Lift lift) {
		if (gender == null || bodyMass <= 0) {
			return 0;
		}

		// Ensure parameters are loaded for the requested lift
		loadParametersForLift(variant, lift);

		Map<Variant, Map<Gender, ArrayList<ParamRow>>> variantMap = parameterCache.get(lift);
		if (variantMap == null) {
			return 0;
		}

		Map<Gender, ArrayList<ParamRow>> genderMap = variantMap.get(variant);
		if (genderMap == null) {
			return 0;
		}

		ArrayList<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			return 0;
		}

		// Age handling: SENIOR always uses 25.0, other variants require actual age
		double normalizedAge;
		if (variant == Variant.SENIOR) {
			normalizedAge = SENIOR_AGE;
		} else {
			if (age == null || age <= 0) {
				logger.error("Age required for variant {} but not provided", variant);
				return 0;
			}
			normalizedAge = age;
		}

		InterpolatedParams interp = interpolateParams(params, normalizedAge, bodyMass);
		if (!interp.success) {
			return 0;
		}

		// Binary search for the total that achieves the target score
		// GAMX is monotonically increasing with total
		int low = 1;
		int high = 600; // reasonable upper bound for weightlifting totals

		while (low < high) {
			int mid = (low + high) / 2;
			double score = computeGamxCore(mid, interp.mu, interp.sigma, interp.nu);
			if (score < targetScore) {
				low = mid + 1;
			} else {
				high = mid;
			}
		}

		// Verify the result
		double finalScore = computeGamxCore(low, interp.mu, interp.sigma, interp.nu);
		if (finalScore >= targetScore) {
			return low;
		}

		return 0;
	}

	/**
	 * Find the total needed to achieve a target GAMX score for an athlete.
	 * Automatically determines body weight (actual vs category) and variant based on Ranking.
	 * 
	 * @param a           the athlete
	 * @param targetScore the target score
	 * @param ranking     the ranking type (GAMX, GAMX_M, GAMX_U, GAMX_A, CAT_GAMX)
	 * @return total in kg needed, or 0 if impossible
	 */
	public static int kgTarget(Athlete a, double targetScore, Ranking ranking) {
		if (a == null || ranking == null) {
			return 0;
		}
		
		// Determine body weight: category boundary for CAT_GAMX, actual for others
		Double bodyMass;
		if (ranking == Ranking.CAT_GAMX) {
			bodyMass = a.computeIwfCategoryBodyWeight();
			if (bodyMass == null || bodyMass <= 0) {
				return 0;
			}
		} else {
			bodyMass = a.getBodyWeight();
		}
		
		// Map Ranking to Variant (age-sensitive variants require the athlete's age)
		Variant variant = switch (ranking) {
			case GAMX_M, GAMX_MS, GAMX_MC -> Variant.MASTERS;
			case GAMX_U -> Variant.U17;
			case GAMX_A -> Variant.AGE_ADJUSTED;
			// GAMX, CAT_GAMX, and the senior snatch/CJ placeholders (GAMX_S, GAMX_C) ignore age
			case GAMX, CAT_GAMX, GAMX_S, GAMX_C -> Variant.SENIOR;
			default -> Variant.SENIOR;
		};
		
		Double age = a.getAge() != null ? a.getAge().doubleValue() : null;
		return kgTarget(a.getGender(), age, targetScore, bodyMass, variant, Lift.TOTAL);
	}

	/**
	 * Core GAMX computation from total and distribution parameters.
	 * 
	 * Formula: GAMX = qnorm(pBCCG(total, mu, sigma, nu)) * 100 + 1000
	 */
	private static double computeGamxCore(double total, double mu, double sigma, double nu) {
		// Validate parameters
		if (total <= 0 || mu <= 0 || sigma <= 0) {
			return 0.0;
		}

		try {
			// Compute p using BCCG CDF
			double p = pBCCG(total, mu, sigma, nu);

			if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0 || p >= 1) {
				logger.trace("pBCCG returned invalid p={} for total={}, mu={}, sigma={}, nu={}",
				        p, total, mu, sigma, nu);
				return 0.0;
			}

			// Transform p to z-score using inverse normal CDF
			double z = NORMAL.inverseCumulativeProbability(p);

			if (Double.isNaN(z) || Double.isInfinite(z)) {
				logger.trace("qnorm returned invalid z={} for p={}", z, p);
				return 0.0;
			}

			// Scale to GAMX score
			double gamx = z * 100 + 1000;
			return gamx;

		} catch (Exception e) {
			logger.error("GAMX computation error: {}", e.getMessage());
			return 0.0;
		}
	}

	/**
	 * BCCG (Box-Cox Cole and Green) cumulative distribution function.
	 * 
	 * Implements R's gamlss.dist::pBCCG function exactly.
	 * 
	 * The BCCG CDF is a truncated/normalized distribution:
	 * p = (Phi(z) - FYy2) / FYy3
	 * 
	 * where:
	 * - z = ((y/mu)^nu - 1) / (nu * sigma) for nu != 0
	 * - z = log(y/mu) / sigma for nu == 0
	 * - FYy2 = Phi(-1/(sigma*|nu|)) if nu > 0, else 0
	 * - FYy3 = Phi(1/(sigma*|nu|))
	 */
	private static double pBCCG(double y, double mu, double sigma, double nu) {
		// Validate inputs
		if (y <= 0 || mu <= 0 || sigma <= 0) {
			return 0.5; // Return neutral probability
		}

		double z;

		// Compute z using Box-Cox transformation
		if (Math.abs(nu) < 1e-10) {
			// Limiting case: log-normal when nu -> 0
			z = Math.log(y / mu) / sigma;
		} else {
			// General case: Box-Cox power transformation
			z = (Math.pow(y / mu, nu) - 1.0) / (nu * sigma);
		}

		// Compute the three components of the normalized CDF
		double FYy1 = NORMAL.cumulativeProbability(z);
		
		double FYy2;
		if (nu > 0) {
			FYy2 = NORMAL.cumulativeProbability(-1.0 / (sigma * Math.abs(nu)));
		} else {
			FYy2 = 0.0;
		}
		
		double FYy3 = NORMAL.cumulativeProbability(1.0 / (sigma * Math.abs(nu)));
		
		// Normalized CDF
		double p = (FYy1 - FYy2) / FYy3;
		
		return p;
	}

	/**
	 * BCCG (Box-Cox Cole and Green) quantile function (inverse CDF).
	 * 
	 * Implements R's gamlss.dist::qBCCG function exactly.
	 * 
	 * Given a probability p, returns the value y such that pBCCG(y, mu, sigma, nu) = p
	 * 
	 * The formula accounts for truncation:
	 * - If nu <= 0: z = qnorm(p * pnorm(1/(sigma*|nu|)))
	 * - If nu > 0:  z = qnorm(1 - (1-p) * pnorm(1/(sigma*|nu|)))
	 * 
	 * Then inverse Box-Cox:
	 * - If nu != 0: y = mu * ((nu * sigma * z + 1)^(1/nu))
	 * - If nu == 0: y = mu * exp(sigma * z)
	 * 
	 * @param p     probability (0 < p < 1)
	 * @param mu    location parameter (mu > 0)
	 * @param sigma scale parameter (sigma > 0)
	 * @param nu    shape parameter (Box-Cox power)
	 * @return the quantile value y
	 */
	private static double qBCCG(double p, double mu, double sigma, double nu) {
		// Validate inputs
		if (p <= 0 || p >= 1 || mu <= 0 || sigma <= 0) {
			return Double.NaN;
		}

		double z;
		double pnormFactor = NORMAL.cumulativeProbability(1.0 / (sigma * Math.abs(nu)));

		// Transform p to z, accounting for truncation
		if (nu <= 0) {
			z = NORMAL.inverseCumulativeProbability(p * pnormFactor);
		} else {
			z = NORMAL.inverseCumulativeProbability(1.0 - (1.0 - p) * pnormFactor);
		}

		// Inverse Box-Cox transformation
		double y;
		if (Math.abs(nu) < 1e-10) {
			// Limiting case: log-normal when nu -> 0
			y = mu * Math.exp(sigma * z);
		} else {
			// General case: inverse Box-Cox power transformation
			double base = nu * sigma * z + 1.0;
			if (base <= 0) {
				return Double.NaN; // Invalid result
			}
			y = mu * Math.pow(base, 1.0 / nu);
		}

		return y;
	}

	/**
	 * Binary search to find first row with matching age.
	 * If age is below table minimum, returns index for minimum age (lower bound normalization).
	 * If age is above table maximum, returns index for maximum age (upper bound normalization).
	 * Returns -1 only if params is empty.
	 */
	private static int binarySearchAge(ArrayList<ParamRow> params, double targetAge) {
		if (params.isEmpty()) {
			return -1;
		}

		// Check if target age is below minimum - use lower bound
		double minAge = params.get(0).age;
		if (targetAge < minAge) {
			logger.debug("Age {} below table minimum {}, normalizing to lower bound", targetAge, minAge);
			return 0;
		}

		// Check if target age is above maximum - use upper bound
		double maxAge = params.get(params.size() - 1).age;
		// Find first row with max age (walk backwards to find first occurrence)
		for (int i = params.size() - 1; i >= 0; i--) {
			if (Math.abs(params.get(i).age - maxAge) < 0.01) {
				if (i == 0 || Math.abs(params.get(i - 1).age - maxAge) >= 0.01) {
					if (targetAge > maxAge) {
						logger.debug("Age {} above table maximum {}, normalizing to upper bound", targetAge, maxAge);
						return i;
					}
					break;
				}
			}
		}

		// Binary search for exact match
		int left = 0;
		int right = params.size() - 1;
		int result = -1;

		while (left <= right) {
			int mid = left + (right - left) / 2;
			double midAge = params.get(mid).age;

			if (Math.abs(midAge - targetAge) < 0.01) {
				// Found matching age, keep searching left for first occurrence
				result = mid;
				right = mid - 1;
			} else if (midAge < targetAge) {
				left = mid + 1;
			} else {
				right = mid - 1;
			}
		}

		return result;
	}

	/**
	 * Interpolate parameters at specific age and body mass using linear interpolation.
	 * Uses binary search to efficiently find age range in large parameter arrays.
	 * 
	 * For age out-of-range:
	 * - If age < min: normalizes to min age (lower bound)
	 * - If age > max: normalizes to max age (upper bound)
	 */
	private static InterpolatedParams interpolateParams(ArrayList<ParamRow> params, double age, double bodyMass) {
		if (params == null || params.isEmpty()) {
			return new InterpolatedParams();
		}

		// Normalize age to table bounds (e.g., age 25 → 30 for MASTERS which starts at 30)
		double minAge = params.get(0).age;
		double maxAge = params.get(params.size() - 1).age;
		double normalizedAge = age;
		if (age < minAge) {
			logger.debug("Age {} below minimum {}, normalizing to lower bound", age, minAge);
			normalizedAge = minAge;
		} else if (age > maxAge) {
			logger.debug("Age {} above maximum {}, normalizing to upper bound", age, maxAge);
			normalizedAge = maxAge;
		}

		// Binary search to find first row with matching age
		int firstAgeIdx = binarySearchAge(params, normalizedAge);
		if (firstAgeIdx < 0) {
			logger.error("No parameters found for age={}", normalizedAge);
			return new InterpolatedParams();
		}

		// Find last row with same age (walk forward) - use normalizedAge, not original age
		int lastAgeIdx = firstAgeIdx;
		while (lastAgeIdx + 1 < params.size() && Math.abs(params.get(lastAgeIdx + 1).age - normalizedAge) < 0.01) {
			lastAgeIdx++;
		}

		// Extract age-specific rows for body mass interpolation
		ArrayList<ParamRow> ageParams = new ArrayList<>(params.subList(firstAgeIdx, lastAgeIdx + 1));

		double minBm = ageParams.get(0).bodyMass;
		double maxBm = ageParams.get(ageParams.size() - 1).bodyMass;

		// Clamp body mass to valid range
		if (bodyMass < minBm) {
			bodyMass = minBm;
		} else if (bodyMass > maxBm) {
			bodyMass = maxBm;
		}

		// Find bracketing indices within age-specific rows
		int lowIdx = -1;
		int highIdx = -1;

		for (int i = 0; i < ageParams.size(); i++) {
			if (ageParams.get(i).bodyMass <= bodyMass) {
				lowIdx = i;
			}
			if (ageParams.get(i).bodyMass >= bodyMass && highIdx < 0) {
				highIdx = i;
			}
		}

		if (lowIdx < 0 || highIdx < 0) {
			logger.error("Failed to find bracketing rows for bodyMass={}", bodyMass);
			return new InterpolatedParams();
		}

		ParamRow low = ageParams.get(lowIdx);
		ParamRow high = ageParams.get(highIdx);

		// Exact match or same row
		if (lowIdx == highIdx || Math.abs(high.bodyMass - low.bodyMass) < 1e-10) {
			return new InterpolatedParams(low.mu, low.sigma, low.nu);
		}

		// Linear interpolation
		double lowRatio = bodyMass - low.bodyMass;
		double highRatio = high.bodyMass - bodyMass;
		double denom = lowRatio + highRatio;

		double mu = (highRatio * low.mu + lowRatio * high.mu) / denom;
		double sigma = (highRatio * low.sigma + lowRatio * high.sigma) / denom;
		double nu = (highRatio * low.nu + lowRatio * high.nu) / denom;

		return new InterpolatedParams(mu, sigma, nu);
	}

	/**
	 * Load parameters for a variant and lift type if not already cached.
	 */
	private static synchronized void loadParametersForLift(Variant variant, Lift lift) {
		Map<Variant, Map<Gender, ArrayList<ParamRow>>> variantMap = parameterCache.get(lift);
		if (variantMap != null && variantMap.containsKey(variant)) {
			return; // Already loaded
		}

		// Initialize maps if needed
		if (variantMap == null) {
			variantMap = new EnumMap<>(Variant.class);
			parameterCache.put(lift, variantMap);
		}

		String menFile = getResourcePath(variant, Gender.M, lift);
		String womenFile = getResourcePath(variant, Gender.F, lift);

		ArrayList<ParamRow> menParams = loadJson(menFile);
		ArrayList<ParamRow> womenParams = loadJson(womenFile);

		Map<Gender, ArrayList<ParamRow>> genderMap = new EnumMap<>(Gender.class);
		genderMap.put(Gender.M, menParams);
		genderMap.put(Gender.F, womenParams);
		variantMap.put(variant, genderMap);

		logger.info("Loaded GAMX parameters for lift {} variant {}: {} men rows, {} women rows",
		        lift, variant, menParams.size(), womenParams.size());
	}

	/**
	 * Get resource path for a parameter JSON file.
	 * Format: params-{lift}-{variant}-{gender}.json
	 */
	private static String getResourcePath(Variant variant, Gender gender, Lift lift) {
		String liftPrefix = switch (lift) {
			case TOTAL -> "total";
			case SNATCH -> "snatch";
			case CJ -> "cj";
		};
		String variantPart = switch (variant) {
			case SENIOR -> "sen";
			case AGE_ADJUSTED -> "age";
			case U17 -> "u17";
			case MASTERS -> "mas";
		};
		String genderSuffix = (gender == Gender.M) ? "men" : "wom";
		return "/gamx/params-" + liftPrefix + "-" + variantPart + "-" + genderSuffix + ".json";
	}

	/**
	 * Load a JSON parameter file as array-of-arrays.
	 * Format: [[bodyMass, mu, sigma, nu], ...] (SENIOR) 
	 *      or [[age, bodyMass, mu, sigma, nu], ...] (age-dependent)
	 */
	private static ArrayList<ParamRow> loadJson(String resourcePath) {
		// Estimate capacity based on file type
		int estimatedCapacity = resourcePath.contains("params-snatch-sen") || resourcePath.contains("params-cj-sen") ? 1600 :
		                       resourcePath.contains("params-total-sen") ? 1600 :
		                       resourcePath.contains("params-total-age") ? 45000 :
		                       resourcePath.contains("params-total-u17") ? 30000 :
		                       resourcePath.contains("params-snatch-mas") || resourcePath.contains("params-cj-mas") ? 100000 :
		                       resourcePath.contains("params-total-mas") ? 100000 : 10000;
		ArrayList<ParamRow> rows = new ArrayList<>(estimatedCapacity);

		try {
			InputStream stream = ResourceWalker.getResourceAsStream(resourcePath);
			if (stream == null) {
				logger.error("Resource not found: {}", resourcePath);
				return rows;
			}

			// Read JSON using Jackson ObjectMapper
			ObjectMapper mapper = new ObjectMapper();
			double[][] data = mapper.readValue(stream, double[][].class);

			// Determine if age-dependent by inspecting row structure (5 columns = age-dependent)

			for (double[] row : data) {
				if (row.length == 4) {
					// Format: [bodyMass, mu, sigma, nu] (SENIOR)
					rows.add(new ParamRow(SENIOR_AGE, row[0], row[1], row[2], row[3]));
				} else if (row.length == 5) {
					// Format: [age, bodyMass, mu, sigma, nu] (age-dependent)
					rows.add(new ParamRow(row[0], row[1], row[2], row[3], row[4]));
				} else {
					logger.error("Invalid JSON row length {} in {}: expected 4 or 5 columns", row.length, resourcePath);
				}
			}

		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}

		return rows;
	}



	/**
	 * For backwards compatibility with GAMX.java API
	 */
	public static float doGetGamx(Gender gender, Double bodyMass, Integer liftedWeight) {
		return (float) computeGamx(gender, SENIOR_AGE, bodyMass, liftedWeight != null ? liftedWeight : 0, Variant.SENIOR);
	}
}
