/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.scoring;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.LoggerFactory;

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
 * - SENIOR: Senior parameters (uses params_sen CSV files) - standard GAMX
 * - AGE_ADJUSTED: Age-adjusted parameters (uses params_iwf CSV files) - GAMX-A
 * - U17: U17 parameters (uses params_usa CSV files) - GAMX-U
 * - MASTERS: Masters athlete parameters (uses params_mas CSV files) - GAMX-M
 * 
 * Parameters (mu, sigma, nu) are interpolated from CSV tables based on body mass.
 */
public class GAMX2 {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(GAMX2.class);
	private static final NormalDistribution NORMAL = new NormalDistribution(0, 1);

	/**
	 * Parameter set variants
	 */
	public enum Variant {
		SENIOR,   // Senior parameters (params_sen) - standard GAMX
		AGE_ADJUSTED,      // Age-Adjusted IWF Data 13-40 (params_iwf) - GAMX-A
		U17,      // U17 parameters (params_usa) - GAMX-U
		MASTERS   // Masters parameters (params_mas) - GAMX-M
	}

	/**
	 * A single row from the parameter CSV file
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

	// Cache: variant -> gender -> ArrayList of ParamRow (pre-allocated for performance)
	private static final Map<Variant, Map<Gender, ArrayList<ParamRow>>> parameterCache = new EnumMap<>(Variant.class);

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
		return computeGamx(gender, bodyMass, liftedWeight, variant);
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
		return computeGamx(gender, bodyMass, liftedWeight, variant);
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
		return computeGamx(gender, bodyMass, total, Variant.SENIOR);
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
	 * Compute GAMX score with age parameter.
	 * 
	 * @param gender   Gender.MALE or Gender.FEMALE
	 * @param age      athlete's age (required for AGE_ADJUSTED, U17, MASTERS; ignored for SENIOR)
	 * @param bodyMass body mass in kg
	 * @param total    lifted total in kg
	 * @param variant  which parameter set to use
	 * @return GAMX score, or 0.0 if inputs invalid
	 */
	public static double computeGamx(Gender gender, Double age, Double bodyMass, int total, Variant variant) {
		if (gender == null || bodyMass == null || bodyMass <= 0 || total <= 0) {
			return 0.0;
		}

		// Ensure parameters are loaded
		loadParameters(variant);

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

		// Get parameter table for this gender
		Map<Gender, ArrayList<ParamRow>> genderMap = parameterCache.get(variant);
		if (genderMap == null) {
			logger.error("No parameters loaded for variant {}", variant);
			return 0.0;
		}

		ArrayList<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			logger.error("No parameters for gender {} variant {}", gender, variant);
			return 0.0;
		}

		// Interpolate mu, sigma, nu from age and body mass
		InterpolatedParams interp = interpolateParams(params, normalizedAge, bodyMass);
		if (!interp.success) {
			return 0.0;
		}

		// Compute GAMX score
		return computeGamxCore(total, interp.mu, interp.sigma, interp.nu);
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
	 * @return minimum total in kg that strictly exceeds targetScore, or 0 if impossible
	 */
	public static int kgTarget(Gender gender, Double age, double targetScore, double bodyMass, Variant variant) {
		if (gender == null || bodyMass <= 0) {
			return 0;
		}

		// Ensure parameters are loaded
		loadParameters(variant);

		Map<Gender, ArrayList<ParamRow>> genderMap = parameterCache.get(variant);
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
	 * @param targetScore the target GAMX score
	 * @param bodyMass    body mass in kg
	 * @param variant     which parameter set to use
	 * @return total in kg needed, or 0 if impossible
	 */
	public static int kgTargetIterative(Gender gender, double targetScore, double bodyMass, Variant variant) {
		if (gender == null || bodyMass <= 0) {
			return 0;
		}

		// Ensure parameters are loaded
		loadParameters(variant);

		Map<Gender, ArrayList<ParamRow>> genderMap = parameterCache.get(variant);
		if (genderMap == null) {
			return 0;
		}

		ArrayList<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			return 0;
		}

		// Normalize age: use 25.0 for SENIOR variant
		double normalizedAge = SENIOR_AGE;

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
	 * Find the total needed to achieve a target GAMX score using SENIOR variant.
	 */
	public static int kgTarget(Gender gender, double targetScore, double bodyMass) {
		return kgTarget(gender, null, targetScore, bodyMass, Variant.SENIOR);
	}

	/**
	 * Find the total needed to achieve a target GAMX-A (AGE_ADJUSTED) score.
	 */
	public static int kgTargetA(Gender gender, double targetScore, double bodyMass) {
		return kgTarget(gender, null, targetScore, bodyMass, Variant.AGE_ADJUSTED);
	}

	/**
	 * Find the total needed to achieve a target GAMX-U (U17) score.
	 */
	public static int kgTargetU(Gender gender, double targetScore, double bodyMass) {
		return kgTarget(gender, null, targetScore, bodyMass, Variant.U17);
	}

	/**
	 * Find the total needed to achieve a target GAMX-M (Masters) score.
	 */
	public static int kgTargetM(Gender gender, double targetScore, double bodyMass) {
		return kgTarget(gender, null, targetScore, bodyMass, Variant.MASTERS);
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
		
		// Map Ranking to Variant
		Variant variant = switch (ranking) {
			case GAMX, CAT_GAMX -> Variant.SENIOR;
			case GAMX_M -> Variant.MASTERS;
			case GAMX_U -> Variant.U17;
			case GAMX_A -> Variant.AGE_ADJUSTED;
			default -> Variant.SENIOR;
		};
		
		return kgTarget(a.getGender(), null, targetScore, bodyMass, variant);
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
	 * Load parameters for a variant if not already cached.
	 */
	private static synchronized void loadParameters(Variant variant) {
		if (parameterCache.containsKey(variant)) {
			return;
		}

		String menFile = getResourcePath(variant, Gender.M);
		String womenFile = getResourcePath(variant, Gender.F);

		ArrayList<ParamRow> menParams = loadCsv(menFile);
		ArrayList<ParamRow> womenParams = loadCsv(womenFile);

		Map<Gender, ArrayList<ParamRow>> genderMap = new EnumMap<>(Gender.class);
		genderMap.put(Gender.M, menParams);
		genderMap.put(Gender.F, womenParams);
		parameterCache.put(variant, genderMap);

		logger.info("Loaded GAMX parameters for {}: {} men rows, {} women rows",
		        variant, menParams.size(), womenParams.size());
	}

	/**
	 * Get resource path for a parameter file.
	 */
	private static String getResourcePath(Variant variant, Gender gender) {
		String prefix = switch (variant) {
			case SENIOR -> "params_sen";
			case AGE_ADJUSTED -> "params_iwf";
			case U17 -> "params_usa";
			case MASTERS -> "params_mas";
		};
		String suffix = (gender == Gender.M) ? "_men.csv" : "_wom.csv";
		return "/gamx/" + prefix + suffix;
	}

	/**
	 * Load a CSV parameter file.
	 * Format: bodyMass,mu,sigma,nu (SENIOR) or age,bodyMass,mu,sigma,nu (age-dependent)
	 * Pre-allocates ArrayList based on variant type for optimal performance.
	 */
	private static ArrayList<ParamRow> loadCsv(String resourcePath) {
		// Estimate capacity based on file type to avoid reallocations
		int estimatedCapacity = resourcePath.contains("params_sen") ? 1600 :
		                       resourcePath.contains("params_iwf") ? 45000 :
		                       resourcePath.contains("params_usa") ? 30000 :
		                       resourcePath.contains("params_mas") ? 100000 : 10000;
		ArrayList<ParamRow> rows = new ArrayList<>(estimatedCapacity);

		try {
			InputStream stream = ResourceWalker.getResourceAsStream(resourcePath);
			if (stream == null) {
				logger.error("Resource not found: {}", resourcePath);
				return rows;
			}

			try (BufferedReader reader = new BufferedReader(
			        new InputStreamReader(stream, StandardCharsets.UTF_8))) {

				String line;
				boolean firstLine = true;
				boolean hasAgeColumn = false;

				while ((line = reader.readLine()) != null) {
					// Check header to determine format
					if (firstLine) {
						firstLine = false;
						// Check if header contains "age" column
						String headerLower = line.toLowerCase();
						hasAgeColumn = headerLower.contains("age") && headerLower.contains("bmass");
						continue;
					}

					// Skip empty lines
					line = line.trim();
					if (line.isEmpty()) {
						continue;
					}

					String[] parts = line.split(",");
					int expectedCols = hasAgeColumn ? 5 : 4;
					if (parts.length < expectedCols) {
						logger.error("Invalid CSV line in {}: expected {} columns, got {}", 
						    resourcePath, expectedCols, parts.length);
						continue;
					}

					try {
						if (hasAgeColumn) {
							// Format: age, bodyMass, mu, sigma, nu
							double age = Double.parseDouble(parts[0].trim());
							double bodyMass = Double.parseDouble(parts[1].trim());
							double mu = Double.parseDouble(parts[2].trim());
							double sigma = Double.parseDouble(parts[3].trim());
							double nu = Double.parseDouble(parts[4].trim());
							rows.add(new ParamRow(age, bodyMass, mu, sigma, nu));
						} else {
							// Format: bodyMass, mu, sigma, nu (SENIOR - normalize to age 25.0)
							double bodyMass = Double.parseDouble(parts[0].trim());
							double mu = Double.parseDouble(parts[1].trim());
							double sigma = Double.parseDouble(parts[2].trim());
							double nu = Double.parseDouble(parts[3].trim());
							rows.add(new ParamRow(SENIOR_AGE, bodyMass, mu, sigma, nu));
						}
					} catch (NumberFormatException e) {
						logger.error("Failed to parse CSV line in {}: {}", resourcePath, line);
					}
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
		return (float) computeGamx(gender, bodyMass, liftedWeight != null ? liftedWeight : 0, Variant.SENIOR);
	}
}
