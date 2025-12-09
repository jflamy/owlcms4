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
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
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
 * - U30: U30 parameters (uses params_iwf CSV files) - GAMX-Y
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
		final double bodyMass;
		final double mu;
		final double sigma;
		final double nu;

		ParamRow(double bodyMass, double mu, double sigma, double nu) {
			this.bodyMass = bodyMass;
			this.mu = mu;
			this.sigma = sigma;
			this.nu = nu;
		}
	}

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

	// Cache: variant -> gender -> list of ParamRow
	private static final Map<Variant, Map<Gender, List<ParamRow>>> parameterCache = new EnumMap<>(Variant.class);

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
	 * Compute GAMX-Y (U30) score for an athlete.
	 * 
	 * @param a            the athlete
	 * @param liftedWeight total lifted in kg
	 * @return GAMX-Y score, or 0.0 if inputs invalid
	 */
	public static double getGamxY(Athlete a, Integer liftedWeight) {
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
		if (gender == null || bodyMass == null || bodyMass <= 0 || total <= 0) {
			return 0.0;
		}

		// Ensure parameters are loaded
		loadParameters(variant);

		// Get parameter table for this gender
		Map<Gender, List<ParamRow>> genderMap = parameterCache.get(variant);
		if (genderMap == null) {
			logger.warn("No parameters loaded for variant {}", variant);
			return 0.0;
		}

		List<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			logger.warn("No parameters for gender {} variant {}", gender, variant);
			return 0.0;
		}

		// Interpolate mu, sigma, nu from body mass
		InterpolatedParams interp = interpolateParams(params, bodyMass);
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
	 * @param targetScore the target GAMX score (must strictly exceed this at 2 decimal precision)
	 * @param bodyMass    body mass in kg
	 * @param variant     which parameter set to use
	 * @return minimum total in kg that strictly exceeds targetScore, or 0 if impossible
	 */
	public static int kgTarget(Gender gender, double targetScore, double bodyMass, Variant variant) {
		if (gender == null || bodyMass <= 0) {
			return 0;
		}

		// Ensure parameters are loaded
		loadParameters(variant);

		Map<Gender, List<ParamRow>> genderMap = parameterCache.get(variant);
		if (genderMap == null) {
			return 0;
		}

		List<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			return 0;
		}

		InterpolatedParams interp = interpolateParams(params, bodyMass);
		if (!interp.success) {
			return 0;
		}

		// Convert target GAMX score to probability
		// GAMX = qnorm(p) * 100 + 1000, so p = pnorm((GAMX - 1000) / 100)
		double z = (targetScore - 1000.0) / 100.0;
		double p = NORMAL.cumulativeProbability(z);

		// Compute initial total using qBCCG (gives a hint for binary search)
		double formulaResult = qBCCG(p, interp.mu, interp.sigma, interp.nu);

		if (Double.isNaN(formulaResult) || Double.isInfinite(formulaResult) || formulaResult <= 0) {
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

		Map<Gender, List<ParamRow>> genderMap = parameterCache.get(variant);
		if (genderMap == null) {
			return 0;
		}

		List<ParamRow> params = genderMap.get(gender);
		if (params == null || params.isEmpty()) {
			return 0;
		}

		InterpolatedParams interp = interpolateParams(params, bodyMass);
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
		return kgTarget(gender, targetScore, bodyMass, Variant.SENIOR);
	}

	/**
	 * Find the total needed to achieve a target GAMX-Y (U30) score.
	 */
	public static int kgTargetY(Gender gender, double targetScore, double bodyMass) {
		return kgTarget(gender, targetScore, bodyMass, Variant.AGE_ADJUSTED);
	}

	/**
	 * Find the total needed to achieve a target GAMX-U (U17) score.
	 */
	public static int kgTargetU(Gender gender, double targetScore, double bodyMass) {
		return kgTarget(gender, targetScore, bodyMass, Variant.U17);
	}

	/**
	 * Find the total needed to achieve a target GAMX-M (Masters) score.
	 */
	public static int kgTargetM(Gender gender, double targetScore, double bodyMass) {
		return kgTarget(gender, targetScore, bodyMass, Variant.MASTERS);
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
			logger.warn("GAMX computation error: {}", e.getMessage());
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
	 * Interpolate mu, sigma, nu from the parameter table based on body mass.
	 */
	private static InterpolatedParams interpolateParams(List<ParamRow> params, double bodyMass) {
		if (params.isEmpty()) {
			return new InterpolatedParams();
		}

		double minBm = params.get(0).bodyMass;
		double maxBm = params.get(params.size() - 1).bodyMass;

		// Clamp body mass to valid range
		if (bodyMass < minBm) {
			bodyMass = minBm;
		} else if (bodyMass > maxBm) {
			bodyMass = maxBm;
		}

		// Find bracketing indices
		int lowIdx = -1;
		int highIdx = -1;

		for (int i = 0; i < params.size(); i++) {
			if (params.get(i).bodyMass <= bodyMass) {
				lowIdx = i;
			}
			if (params.get(i).bodyMass >= bodyMass && highIdx < 0) {
				highIdx = i;
			}
		}

		if (lowIdx < 0 || highIdx < 0) {
			logger.warn("Failed to find bracketing rows for bodyMass={}", bodyMass);
			return new InterpolatedParams();
		}

		ParamRow low = params.get(lowIdx);
		ParamRow high = params.get(highIdx);

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

		List<ParamRow> menParams = loadCsv(menFile);
		List<ParamRow> womenParams = loadCsv(womenFile);

		Map<Gender, List<ParamRow>> genderMap = new EnumMap<>(Gender.class);
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
	 * Expected format: bodyMass,mu,sigma,nu (with header row)
	 */
	private static List<ParamRow> loadCsv(String resourcePath) {
		List<ParamRow> rows = new ArrayList<>();

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

				while ((line = reader.readLine()) != null) {
					// Skip header
					if (firstLine) {
						firstLine = false;
						continue;
					}

					// Skip empty lines
					line = line.trim();
					if (line.isEmpty()) {
						continue;
					}

					String[] parts = line.split(",");
					if (parts.length < 4) {
						logger.warn("Invalid CSV line in {}: {}", resourcePath, line);
						continue;
					}

					try {
						double bodyMass = Double.parseDouble(parts[0].trim());
						double mu = Double.parseDouble(parts[1].trim());
						double sigma = Double.parseDouble(parts[2].trim());
						double nu = Double.parseDouble(parts[3].trim());

						rows.add(new ParamRow(bodyMass, mu, sigma, nu));
					} catch (NumberFormatException e) {
						logger.warn("Failed to parse CSV line in {}: {}", resourcePath, line);
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
