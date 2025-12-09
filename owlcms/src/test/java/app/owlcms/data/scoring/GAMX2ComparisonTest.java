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

		// SENIOR variant - age parameter ignored (always uses 25.0)
		double gamxSenior = GAMX2.computeGamx(Gender.M, null, 100.0, 200, GAMX2.Variant.SENIOR);
		
		// Age-dependent variants - require age parameter
		double gamxAgeAdjusted = GAMX2.computeGamx(Gender.M, 20.0, 100.0, 200, GAMX2.Variant.AGE_ADJUSTED);
		double gamxU17 = GAMX2.computeGamx(Gender.M, 15.0, 100.0, 200, GAMX2.Variant.U17);
		double gamxMasters = GAMX2.computeGamx(Gender.M, 50.0, 100.0, 200, GAMX2.Variant.MASTERS);

		logger.info("Variant test results (M, 100kg, 200kg total):");
		logger.info(String.format("  SENIOR (age ignored):     %.4f", gamxSenior));
		logger.info(String.format("  AGE_ADJUSTED (age 20):    %.4f", gamxAgeAdjusted));
		logger.info(String.format("  U17 (age 15):             %.4f", gamxU17));
		logger.info(String.format("  MASTERS (age 50):         %.4f", gamxMasters));

		assertTrue("SENIOR variant failed", gamxSenior > 0);
		assertTrue("AGE_ADJUSTED variant failed", gamxAgeAdjusted > 0);
		assertTrue("U17 variant failed", gamxU17 > 0);
		assertTrue("MASTERS variant failed", gamxMasters > 0);
	}

	@Test
	public void testSENIORVariantIgnoresAge() {
		logger.info("Testing that SENIOR variant always uses age=25.0 regardless of input");

		// SENIOR variant should produce same result regardless of age parameter
		double gamx1 = GAMX2.computeGamx(Gender.M, null, 100.0, 200, GAMX2.Variant.SENIOR);
		double gamx2 = GAMX2.computeGamx(Gender.M, 20.0, 100.0, 200, GAMX2.Variant.SENIOR);
		double gamx3 = GAMX2.computeGamx(Gender.M, 30.0, 100.0, 200, GAMX2.Variant.SENIOR);
		double gamx4 = GAMX2.computeGamx(Gender.M, 50.0, 100.0, 200, GAMX2.Variant.SENIOR);

		logger.info(String.format("SENIOR with age=null: %.4f", gamx1));
		logger.info(String.format("SENIOR with age=20:   %.4f", gamx2));
		logger.info(String.format("SENIOR with age=30:   %.4f", gamx3));
		logger.info(String.format("SENIOR with age=50:   %.4f", gamx4));

		assertTrue("SENIOR should produce same result regardless of age",
				Math.abs(gamx1 - gamx2) < 0.0001 &&
				Math.abs(gamx1 - gamx3) < 0.0001 &&
				Math.abs(gamx1 - gamx4) < 0.0001);
	}

	@Test
	public void testAgeParameterRequiredForNonSenior() {
		logger.info("Testing that non-SENIOR variants require age parameter");

		// These should return 0.0 because age is null
		double gamxAgeAdjusted = GAMX2.computeGamx(Gender.M, null, 100.0, 200, GAMX2.Variant.AGE_ADJUSTED);
		double gamxU17 = GAMX2.computeGamx(Gender.M, null, 100.0, 200, GAMX2.Variant.U17);
		double gamxMasters = GAMX2.computeGamx(Gender.M, null, 100.0, 200, GAMX2.Variant.MASTERS);

		logger.info(String.format("AGE_ADJUSTED with age=null: %.4f (should be 0.0)", gamxAgeAdjusted));
		logger.info(String.format("U17 with age=null:          %.4f (should be 0.0)", gamxU17));
		logger.info(String.format("MASTERS with age=null:      %.4f (should be 0.0)", gamxMasters));

		assertTrue("AGE_ADJUSTED should return 0 when age is null", gamxAgeAdjusted == 0.0);
		assertTrue("U17 should return 0 when age is null", gamxU17 == 0.0);
		assertTrue("MASTERS should return 0 when age is null", gamxMasters == 0.0);
	}

	@Test
	public void testKgTargetCrossingAgeVariants() {
		logger.info("Testing kgTarget: SENIOR athlete matching their MASTERS score");

		// Scenario: 69kg male athlete at age 58 (MASTERS) lifts 140kg
		// Question: How much does the same athlete need to lift as SENIOR (age normalized to 25) to match that GAMX?
		double bodyMass = 69.0;
		int mastersTotal = 140;
		double mastersAge58 = 58.0;

		// Compute GAMX for 58-year-old lifting 140kg (MASTERS variant)
		double masters58GAMX = GAMX2.computeGamx(Gender.M, mastersAge58, bodyMass, mastersTotal, GAMX2.Variant.MASTERS);

		logger.info(String.format("MASTERS athlete: age=%.0f, bodyMass=%.0fkg, total=%dkg → GAMX=%.4f",
				mastersAge58, bodyMass, mastersTotal, masters58GAMX));

		// Find total needed as SENIOR to match that GAMX (age normalized to 25)
		int seniorTotal = GAMX2.kgTarget(Gender.M, null, masters58GAMX, bodyMass, GAMX2.Variant.SENIOR);

		// Verify the SENIOR total produces equivalent GAMX
		double seniorGAMX = GAMX2.computeGamx(Gender.M, null, bodyMass, seniorTotal, GAMX2.Variant.SENIOR);
		double seniorRounded = Math.round(seniorGAMX * 100.0) / 100.0;
		double masters58Rounded = Math.round(masters58GAMX * 100.0) / 100.0;

		logger.info(String.format("SENIOR athlete: age=25 (normalized), bodyMass=%.0fkg, total=%dkg → GAMX=%.4f",
				bodyMass, seniorTotal, seniorGAMX));
		logger.info(String.format("Difference: SENIOR needs %d kg more to exceed MASTERS age 58 GAMX (%.4f vs %.4f)",
				seniorTotal - mastersTotal, seniorGAMX, masters58GAMX));

		// Verify that subtracting 1kg from SENIOR total yields lower GAMX than MASTERS
		double seniorMinus1GAMX = GAMX2.computeGamx(Gender.M, null, bodyMass, seniorTotal - 1, GAMX2.Variant.SENIOR);
		double seniorMinus1Rounded = Math.round(seniorMinus1GAMX * 100.0) / 100.0;

		logger.info(String.format("SENIOR athlete with -1kg: total=%dkg → GAMX=%.4f (should be < %.4f)",
				seniorTotal - 1, seniorMinus1GAMX, masters58GAMX));

		assertTrue("Failed to compute MASTERS GAMX", masters58GAMX > 0);
		assertTrue("Failed to compute SENIOR target total", seniorTotal > 0);
		
		boolean seniorMatches = seniorRounded >= masters58Rounded;
		assertTrue("SENIOR total should exceed MASTERS GAMX at 2 decimal precision", seniorMatches);
		logger.info(String.format("%s SENIOR total exceeds MASTERS age 58 GAMX at 2 decimals", seniorMatches ? "✓" : "✗"));
		
		boolean seniorMinus1Lower = seniorMinus1Rounded < masters58Rounded;
		assertTrue("SENIOR total minus 1kg should yield lower GAMX than MASTERS age 58", seniorMinus1Lower);
		logger.info(String.format("%s SENIOR total minus 1kg yields lower GAMX than MASTERS age 58", seniorMinus1Lower ? "✓" : "✗"));

		// Now test 25-year-old MASTERS athlete needing to match the 58-year-old's GAMX
		double mastersAge25 = 25.0;
		
		// Find total needed for 25-year-old MASTERS to match 58-year-old's GAMX
		int masters25Total = GAMX2.kgTarget(Gender.M, mastersAge25, masters58GAMX, bodyMass, GAMX2.Variant.MASTERS);

		// Verify the 25-year-old MASTERS total produces equivalent GAMX
		double masters25GAMX = GAMX2.computeGamx(Gender.M, mastersAge25, bodyMass, masters25Total, GAMX2.Variant.MASTERS);
		double masters25Rounded = Math.round(masters25GAMX * 100.0) / 100.0;

		logger.info(String.format("MASTERS athlete: age=%.0f, bodyMass=%.0fkg, total=%dkg → GAMX=%.4f",
				mastersAge25, bodyMass, masters25Total, masters25GAMX));
		logger.info(String.format("Difference: 25-year-old needs %d kg more than 58-year-old to match GAMX (%.4f vs %.4f)",
				masters25Total - mastersTotal, masters25GAMX, masters58GAMX));

		// Verify that subtracting 1kg from 25-year-old total yields lower GAMX than 58-year-old
		double masters25Minus1GAMX = GAMX2.computeGamx(Gender.M, mastersAge25, bodyMass, masters25Total - 1, GAMX2.Variant.MASTERS);
		double masters25Minus1Rounded = Math.round(masters25Minus1GAMX * 100.0) / 100.0;

		logger.info(String.format("MASTERS age 25 with -1kg: total=%dkg → GAMX=%.4f (should be < %.4f)",
				masters25Total - 1, masters25Minus1GAMX, masters58GAMX));

		assertTrue("Failed to compute MASTERS age 25 target total", masters25Total > 0);
		
		// CRITICAL: 25-year-old (normalized to 30) is YOUNGER/STRONGER than 58-year-old
		// Therefore they need MORE kg to achieve the same GAMX score
		assertTrue("25-year-old MASTERS should need MORE kg than 58-year-old (younger = stronger = needs more weight)",
				masters25Total > mastersTotal);
		logger.info(String.format("%s 25-year-old needs more kg than 58-year-old (%d > %d)", 
				masters25Total > mastersTotal ? "✓" : "✗", masters25Total, mastersTotal));
		
		boolean masters25Matches = masters25Rounded >= masters58Rounded;
		assertTrue("25-year-old MASTERS should exceed 58-year-old GAMX at 2 decimal precision", masters25Matches);
		logger.info(String.format("%s 25-year-old MASTERS total exceeds 58-year-old GAMX at 2 decimals", masters25Matches ? "✓" : "✗"));
		
		boolean masters25Minus1Lower = masters25Minus1Rounded < masters58Rounded;
		assertTrue("25-year-old MASTERS minus 1kg should yield lower GAMX than 58-year-old", masters25Minus1Lower);
		logger.info(String.format("%s 25-year-old MASTERS minus 1kg yields lower GAMX than 58-year-old", masters25Minus1Lower ? "✓" : "✗"));
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
			int computedTotal = GAMX2.kgTarget(Gender.M, null, targetGAMX, bodyMass, GAMX2.Variant.SENIOR);

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
			int computedTotal = GAMX2.kgTarget(Gender.F, null, targetGAMX, bodyMass, GAMX2.Variant.SENIOR);

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

			int formulaResult = GAMX2.kgTarget(gender, null, targetGAMX, bodyMass, GAMX2.Variant.SENIOR);
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
			int totalToBeat = GAMX2.kgTarget(gender, null, opponentGAMX, bodyMass, GAMX2.Variant.SENIOR);

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
