/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.scoring;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import ch.qos.logback.classic.Logger;

/**
 * Comparison tests for GAMX2 Java implementation vs Docker API reference implementation.
 * 
 * This test suite validates that GAMX2.java produces results consistent with the
 * official R/Plumber Docker API. Expected values are pre-computed from the Docker API
 * and embedded as constants for reproducible testing.
 * 
 * Expected values computed from: http://localhost:8000/gamx_full
 */
public class GAMX2ComparisonTest {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(GAMX2ComparisonTest.class);
	private static final double TOLERANCE = 0.0001; // Match to 4 decimals

	/**
	 * Expected GAMX values for men (body_mass, total) -> expected_gamx
	 * Pre-computed from Docker API on 2025-12-08
	 */
	private static final Object[][] MEN_TEST_CASES = {
			{ 40.0, 80, 639.1203 },
			{ 55.0, 110, 661.8862 },
			{ 70.0, 140, 676.6842 },
			{ 85.0, 170, 699.6138 },
			{ 100.0, 200, 713.9157 },
			{ 115.0, 230, 724.1419 },
			{ 130.0, 260, 738.2908 },
			{ 145.0, 290, 749.7999 },
			{ 160.0, 320, 772.9558 },
			{ 190.0, 380, 854.3847 },
	};

	/**
	 * Expected GAMX values for women (body_mass, total) -> expected_gamx
	 * Pre-computed from Docker API on 2025-12-08
	 */
	private static final Object[][] WOMEN_TEST_CASES = {
			{ 40.0, 140, 960.0456 },
			{ 50.0, 150, 874.3576 },
			{ 60.0, 160, 835.9225 },
			{ 70.0, 170, 825.9754 },
			{ 80.0, 180, 825.8145 },
			{ 90.0, 190, 849.4980 },
			{ 100.0, 200, 861.0436 },
			{ 110.0, 210, 863.7192 },
			{ 120.0, 220, 867.3155 },
			{ 160.0, 260, 864.1412 },
	};

	@Test
	public void testGAMX2MenMatchesDockerAPI() {
		logger.info("Testing GAMX2 SENIOR variant for men against Docker API reference");

		int passed = 0;
		int failed = 0;

		for (Object[] testCase : MEN_TEST_CASES) {
			double bodyMass = (Double) testCase[0];
			int total = (Integer) testCase[1];
			double expected = (Double) testCase[2];

			// Compute using GAMX2.java
			double actual = GAMX2.computeGamx(Gender.M, bodyMass, total, GAMX2.Variant.SENIOR);

			double diff = Math.abs(actual - expected);
			boolean matches = diff <= TOLERANCE;

			if (matches) {
				passed++;
				logger.info(String.format("✓ M/%.0fkg/%dkg: Java=%.4f, API=%.4f, diff=%.6f",
						bodyMass, total, actual, expected, diff));
			} else {
				failed++;
				logger.error(String.format("✗ M/%.0fkg/%dkg: Java=%.4f, API=%.4f, diff=%.6f",
						bodyMass, total, actual, expected, diff));
			}
		}

		logger.info("Men test results: {} passed, {} failed", passed, failed);
		assertTrue("Men test cases failed: " + failed + " out of " + MEN_TEST_CASES.length,
				failed == 0);
	}

	@Test
	public void testGAMX2WomenMatchesDockerAPI() {
		logger.info("Testing GAMX2 SENIOR variant for women against Docker API reference");

		int passed = 0;
		int failed = 0;

		for (Object[] testCase : WOMEN_TEST_CASES) {
			double bodyMass = (Double) testCase[0];
			int total = (Integer) testCase[1];
			double expected = (Double) testCase[2];

			// Compute using GAMX2.java
			double actual = GAMX2.computeGamx(Gender.F, bodyMass, total, GAMX2.Variant.SENIOR);

			double diff = Math.abs(actual - expected);
			boolean matches = diff <= TOLERANCE;

			if (matches) {
				passed++;
				logger.info(String.format("✓ W/%.0fkg/%dkg: Java=%.4f, API=%.4f, diff=%.6f",
						bodyMass, total, actual, expected, diff));
			} else {
				failed++;
				logger.error(String.format("✗ W/%.0fkg/%dkg: Java=%.4f, API=%.4f, diff=%.6f",
						bodyMass, total, actual, expected, diff));
			}
		}

		logger.info("Women test results: {} passed, {} failed", passed, failed);
		assertTrue("Women test cases failed: " + failed + " out of " + WOMEN_TEST_CASES.length,
				failed == 0);
	}

	@Test
	public void testSENIORVariantLoads() {
		logger.info("Testing GAMX2 SENIOR variant parameter loading");

		double gamxMen = GAMX2.computeGamx(Gender.M, 100.0, 200, GAMX2.Variant.SENIOR);
		double gamxWomen = GAMX2.computeGamx(Gender.F, 80.0, 150, GAMX2.Variant.SENIOR);

		logger.info(String.format("SENIOR variant test - Men (100kg, 200kg total): %.4f", gamxMen));
		logger.info(String.format("SENIOR variant test - Women (80kg, 150kg total): %.4f", gamxWomen));

		assertTrue("Failed to compute GAMX for men - parameters may not have loaded", gamxMen > 0);
		assertTrue("Failed to compute GAMX for women - parameters may not have loaded", gamxWomen > 0);
	}

	@Test
	public void testAllVariantsAccessible() {
		logger.info("Testing all GAMX2 variant accessibility");

		double gamxSenior = GAMX2.computeGamx(Gender.M, 100.0, 200, GAMX2.Variant.SENIOR);
		double gamxU30 = GAMX2.computeGamx(Gender.M, 100.0, 200, GAMX2.Variant.AGE_ADJUSTED);
		double gamxU17 = GAMX2.computeGamx(Gender.M, 100.0, 200, GAMX2.Variant.U17);
		double gamxMasters = GAMX2.computeGamx(Gender.M, 100.0, 200, GAMX2.Variant.MASTERS);

		logger.info("Variant test results (M, 100kg, 200kg total):");
		logger.info(String.format("  SENIOR:  %.4f", gamxSenior));
		logger.info(String.format("  U30:     %.4f", gamxU30));
		logger.info(String.format("  U17:     %.4f", gamxU17));
		logger.info(String.format("  MASTERS: %.4f", gamxMasters));

		assertTrue("SENIOR variant failed", gamxSenior > 0);
		assertTrue("U30 variant failed", gamxU30 > 0);
		assertTrue("U17 variant failed", gamxU17 > 0);
		assertTrue("MASTERS variant failed", gamxMasters > 0);
	}

	/**
	 * Test cases for reverse GAMX calculation (kgTarget)
	 * Format: (bodyMass, targetGAMX)
	 * Validation: kgTarget returns a total T such that computeGamx(bodyMass, T) ≈ targetGAMX
	 */
	private static final Object[][] MEN_KGTARGET_CASES = {
			{ 100.0, 800.0 },
			{ 85.0, 750.0 },
			{ 73.0, 900.0 },
			{ 120.0, 950.0 },
			{ 55.0, 700.0 },
	};

	private static final Object[][] WOMEN_KGTARGET_CASES = {
			{ 70.0, 850.0 },
			{ 60.0, 800.0 },
			{ 80.0, 900.0 },
			{ 50.0, 750.0 },
			{ 90.0, 880.0 },
	};

	@Test
	public void testKgTargetMenSENIORVariant() {
		logger.info("Testing kgTarget for men SENIOR variant");

		int passed = 0;
		int failed = 0;

		for (Object[] testCase : MEN_KGTARGET_CASES) {
			double bodyMass = (Double) testCase[0];
			double targetGAMX = (Double) testCase[1];

			// Compute required total using kgTarget
			int computedTotal = GAMX2.kgTarget(Gender.M, targetGAMX, bodyMass, GAMX2.Variant.SENIOR);

			if (computedTotal == 0) {
				failed++;
				logger.error(String.format("✗ M/%.0fkg/target=%.0f: kgTarget returned 0", bodyMass, targetGAMX));
				continue;
			}

			// Verify: GAMX at returned total must meet or exceed target
			double actualGAMX = GAMX2.computeGamx(Gender.M, bodyMass, computedTotal, GAMX2.Variant.SENIOR);
			// Round to 2 decimal places for comparison to avoid floating-point precision issues
			double actualRounded = Math.round(actualGAMX * 100.0) / 100.0;
			double targetRounded = Math.round(targetGAMX * 100.0) / 100.0;

			if (actualRounded >= targetRounded) {
				passed++;
				logger.info(String.format("✓ M/%.0fkg/target=%.0f: total=%dkg, actualGAMX=%.4f (>= target)",
						bodyMass, targetGAMX, computedTotal, actualGAMX));
			} else {
				failed++;
				logger.error(String.format("✗ M/%.0fkg/target=%.0f: total=%dkg, actualGAMX=%.4f (< target!)",
						bodyMass, targetGAMX, computedTotal, actualGAMX));
			}
		}

		logger.info("Men kgTarget test results: {} passed, {} failed", passed, failed);
		assertTrue("Men kgTarget test cases failed: " + failed + " out of " + MEN_KGTARGET_CASES.length,
				failed == 0);
	}

	@Test
	public void testKgTargetWomenSENIORVariant() {
		logger.info("Testing kgTarget for women SENIOR variant");

		int passed = 0;
		int failed = 0;

		for (Object[] testCase : WOMEN_KGTARGET_CASES) {
			double bodyMass = (Double) testCase[0];
			double targetGAMX = (Double) testCase[1];

			// Compute required total using kgTarget
			int computedTotal = GAMX2.kgTarget(Gender.F, targetGAMX, bodyMass, GAMX2.Variant.SENIOR);

			if (computedTotal == 0) {
				failed++;
				logger.error(String.format("✗ W/%.0fkg/target=%.0f: kgTarget returned 0", bodyMass, targetGAMX));
				continue;
			}

			// Verify: GAMX at returned total must meet or exceed target
			double actualGAMX = GAMX2.computeGamx(Gender.F, bodyMass, computedTotal, GAMX2.Variant.SENIOR);
			// Round to 2 decimal places for comparison to avoid floating-point precision issues
			double actualRounded = Math.round(actualGAMX * 100.0) / 100.0;
			double targetRounded = Math.round(targetGAMX * 100.0) / 100.0;

			if (actualRounded >= targetRounded) {
				passed++;
				logger.info(String.format("✓ W/%.0fkg/target=%.0f: total=%dkg, actualGAMX=%.4f (>= target)",
						bodyMass, targetGAMX, computedTotal, actualGAMX));
			} else {
				failed++;
				logger.error(String.format("✗ W/%.0fkg/target=%.0f: total=%dkg, actualGAMX=%.4f (< target!)",
						bodyMass, targetGAMX, computedTotal, actualGAMX));
			}
		}

		logger.info("Women kgTarget test results: {} passed, {} failed", passed, failed);
		assertTrue("Women kgTarget test cases failed: " + failed + " out of " + WOMEN_KGTARGET_CASES.length,
				failed == 0);
	}

	@Test
	public void testKgTargetComparedToIterative() {
		logger.info("Testing that kgTarget (formula) and kgTargetIterative match");

		int matches = 0;
		int mismatches = 0;

		// Test a few points
		Object[][] testPoints = {
				{ Gender.M, 100.0, 800.0 },
				{ Gender.M, 85.0, 750.0 },
				{ Gender.F, 70.0, 850.0 },
				{ Gender.F, 60.0, 800.0 },
		};

		for (Object[] point : testPoints) {
			Gender gender = (Gender) point[0];
			double bodyMass = (Double) point[1];
			double targetGAMX = (Double) point[2];

			int formulaResult = GAMX2.kgTarget(gender, targetGAMX, bodyMass, GAMX2.Variant.SENIOR);
			int iterativeResult = GAMX2.kgTargetIterative(gender, targetGAMX, bodyMass, GAMX2.Variant.SENIOR);

			if (formulaResult == iterativeResult) {
				matches++;
				logger.info(String.format("✓ %s/%.0fkg/%.0f GAMX: formula=%dkg, iterative=%dkg (match)",
						gender, bodyMass, targetGAMX, formulaResult, iterativeResult));
			} else {
				mismatches++;
				logger.error(String.format("✗ %s/%.0fkg/%.0f GAMX: formula=%dkg, iterative=%dkg (diff=%d)",
						gender, bodyMass, targetGAMX, formulaResult, iterativeResult,
						Math.abs(formulaResult - iterativeResult)));
			}
		}

		logger.info("Comparison results: {} matches, {} mismatches", matches, mismatches);
		assertTrue("Formula and iterative methods don't match: " + mismatches + " mismatches",
				mismatches == 0);
	}

	/**
	 * Test that kgTarget returns a total that strictly beats the target GAMX at 2 decimal precision.
	 * 
	 * Scenario: Two athletes with identical age, gender, bodyweight compete.
	 * Athlete A lifts a total that produces GAMX score X.
	 * Athlete B wants to beat A - kgTarget(X) must return a total that produces GAMX > X at 2 decimals.
	 * No ties allowed - we want a clear win.
	 */
	@Test
	public void testKgTargetStrictlyExceedsTarget() {
		logger.info("Testing that kgTarget returns total that strictly exceeds target (no ties at 2 decimals)");

		int passed = 0;
		int failed = 0;

		// Test cases: (gender, bodyMass, opponentTotal) - we compute opponent's GAMX, then find total to beat it
		Object[][] testCases = {
				{ Gender.M, 85.0, 300 },   // Men 85kg, opponent lifted 300kg
				{ Gender.M, 102.0, 350 },  // Men 102kg, opponent lifted 350kg
				{ Gender.F, 64.0, 200 },   // Women 64kg, opponent lifted 200kg
				{ Gender.F, 76.0, 230 },   // Women 76kg, opponent lifted 230kg
		};

		for (Object[] testCase : testCases) {
			Gender gender = (Gender) testCase[0];
			double bodyMass = (Double) testCase[1];
			int opponentTotal = (Integer) testCase[2];

			// Compute opponent's GAMX score
			double opponentGAMX = GAMX2.computeGamx(gender, bodyMass, opponentTotal, GAMX2.Variant.SENIOR);
			double opponentRounded = Math.round(opponentGAMX * 100.0) / 100.0;

			// Find the total needed to beat opponent
			int totalToBeat = GAMX2.kgTarget(gender, opponentGAMX, bodyMass, GAMX2.Variant.SENIOR);

			// Compute our GAMX at that total
			double ourGAMX = GAMX2.computeGamx(gender, bodyMass, totalToBeat, GAMX2.Variant.SENIOR);
			double ourRounded = Math.round(ourGAMX * 100.0) / 100.0;

			// Must strictly exceed at 2 decimal precision (no ties)
			if (ourRounded > opponentRounded) {
				passed++;
				logger.info(String.format("✓ %s/%.0fkg: opponent=%dkg(%.2f), toBeat=%dkg(%.2f) - WIN by %.2f",
						gender, bodyMass, opponentTotal, opponentRounded, totalToBeat, ourRounded,
						ourRounded - opponentRounded));
			} else {
				failed++;
				logger.error(String.format("✗ %s/%.0fkg: opponent=%dkg(%.2f), toBeat=%dkg(%.2f) - TIE or LOSS!",
						gender, bodyMass, opponentTotal, opponentRounded, totalToBeat, ourRounded));
			}
		}

		logger.info("Strict exceed test results: {} passed, {} failed", passed, failed);
		assertTrue("kgTarget must strictly exceed target (no ties): " + failed + " failures",
				failed == 0);
	}
}
