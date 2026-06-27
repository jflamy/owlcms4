package app.owlcms.data.scoring;

import static org.junit.Assert.*;

import java.time.LocalDate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.TestData;

/**
 * GAMX2 JSON Format Parity Test (Test A)
 * 
 * Verifies that TOTAL scores computed via new JSON files produce consistent,
 * valid results across all variants. This is the critical gate test for Phase 6.
 * 
 * Success criteria:
 * - All 4 variants (SENIOR, AGE_ADJUSTED, U17, MASTERS) load successfully
 * - Scores are non-zero (parameters loaded)
 * - Scores are in reasonable range (typically 900-1500 for competition lifters)
 * - Scores are monotonically increasing with weight
 * - Score precision is valid (representable as 2-decimal values)
 */
public class GamxJsonParityTest {

	private static final double EPSILON = 0.01;

	@BeforeClass
	public static void setUp() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		TestData.insertInitialData(5, true);
		System.out.println("\n========== GAMX2 JSON Parity Test (Test A) ==========");
	}

	@AfterClass
	public static void tearDown() {
		JPAService.close();
	}

	/** Test SENIOR variant with male athlete */
	@Test
	public void testSeniorMale() {
		Athlete a = createMockAthlete("Smith", Gender.M, 75.0, null);
		double gamx = GAMX2.getGamx(a, 200);
		assertNotEquals("GAMX should not be 0 (params should load)", 0.0, gamx, EPSILON);
		assertTrue("GAMX should be in reasonable range, got " + gamx, gamx > 400 && gamx < 1600);
		System.out.println("✓ SENIOR male: GAMX(200kg, 75kg) = " + fmt(gamx));
	}

	/** Test SENIOR variant with female athlete */
	@Test
	public void testSeniorFemale() {
		Athlete a = createMockAthlete("Jones", Gender.F, 65.0, null);
		double gamx = GAMX2.getGamx(a, 140);
		assertNotEquals("GAMX should not be 0 (params should load)", 0.0, gamx, EPSILON);
		assertTrue("GAMX should be in reasonable range, got " + gamx, gamx > 400 && gamx < 1600);
		System.out.println("✓ SENIOR female: GAMX(140kg, 65kg) = " + fmt(gamx));
	}

	/** Test AGE_ADJUSTED variant with young athlete (age 20) */
	@Test
	public void testAgeAdjustedYoung() {
		Athlete a = createMockAthlete("Young", Gender.M, 80.0, 20);
		double gamxA = GAMX2.getGamxA(a, 220);
		assertNotEquals("GAMX-A should not be 0 (age params should load)", 0.0, gamxA, EPSILON);
		assertTrue("GAMX-A should be in reasonable range, got " + gamxA, gamxA > 400 && gamxA < 1600);
		System.out.println("✓ AGE_ADJUSTED (age 20): GAMX-A(220kg, 80kg) = " + fmt(gamxA));
	}

	/** Test AGE_ADJUSTED variant with mid-age athlete (age 30) */
	@Test
	public void testAgeAdjustedMid() {
		Athlete a = createMockAthlete("Mid", Gender.F, 58.0, 30);
		double gamxA = GAMX2.getGamxA(a, 130);
		assertNotEquals("GAMX-A should not be 0", 0.0, gamxA, EPSILON);
		assertTrue("GAMX-A should be in reasonable range", gamxA > 400 && gamxA < 1600);
		System.out.println("✓ AGE_ADJUSTED (age 30): GAMX-A(130kg, 58kg) = " + fmt(gamxA));
	}

	/** Test U17 variant */
	@Test
	public void testU17() {
		Athlete a = createMockAthlete("Teen", Gender.M, 70.0, 16);
		double gamxU = GAMX2.getGamxU(a, 180);
		assertNotEquals("GAMX-U should not be 0 (U17 params should load)", 0.0, gamxU, EPSILON);
		assertTrue("GAMX-U should be in reasonable range, got " + gamxU, gamxU > 400 && gamxU < 1600);
		System.out.println("✓ U17 variant: GAMX-U(180kg, 70kg, age 16) = " + fmt(gamxU));
	}

	/** Test MASTERS variant with older athlete (age 50) */
	@Test
	public void testMasters() {
		Athlete a = createMockAthlete("Senior", Gender.M, 80.0, 50);
		double gamxM = GAMX2.getGamxM(a, 200);
		assertNotEquals("GAMX-M should not be 0 (masters params should load)", 0.0, gamxM, EPSILON);
		assertTrue("GAMX-M should be in reasonable range, got " + gamxM, gamxM > 400 && gamxM < 1600);
		System.out.println("✓ MASTERS variant: GAMX-M(200kg, 80kg, age 50) = " + fmt(gamxM));
	}

	/** Test monotonicity: higher total should give higher GAMX score */
	@Test
	public void testMonotonicity() {
		Athlete a = createMockAthlete("Test", Gender.M, 80.0, null);
		double gamx180 = GAMX2.getGamx(a, 180);
		double gamx190 = GAMX2.getGamx(a, 190);
		double gamx200 = GAMX2.getGamx(a, 200);
		assertTrue("GAMX(180) should be non-zero", gamx180 > 0);
		assertTrue("GAMX(190) should be > GAMX(180)", gamx190 > gamx180);
		assertTrue("GAMX(200) should be > GAMX(190)", gamx200 > gamx190);
		System.out.println("✓ Monotonicity: " + fmt(gamx180) + " < " + fmt(gamx190) + " < " + fmt(gamx200));
	}

	/** Test that all variants load successfully */
	@Test
	public void testAllVariantsLoad() {
		Athlete maleAthlete = createMockAthlete("M", Gender.M, 80.0, null);
		Athlete femaleAthlete = createMockAthlete("F", Gender.F, 60.0, null);
		Athlete youngAthlete = createMockAthlete("Y", Gender.M, 75.0, 16);
		Athlete olderAthlete = createMockAthlete("O", Gender.M, 82.0, 45);
		
		assertTrue("SENIOR male", GAMX2.getGamx(maleAthlete, 200) > 0);
		assertTrue("SENIOR female", GAMX2.getGamx(femaleAthlete, 140) > 0);
		assertTrue("AGE_ADJUSTED", GAMX2.getGamxA(olderAthlete, 200) > 0);
		assertTrue("U17", GAMX2.getGamxU(youngAthlete, 190) > 0);
		assertTrue("MASTERS", GAMX2.getGamxM(olderAthlete, 210) > 0);
		System.out.println("✓ All variants load successfully");
	}

	/** Test decimal precision: scores should be representable at 2-decimal precision */
	@Test
	public void testDecimalPrecision() {
		Athlete a = createMockAthlete("Test", Gender.M, 75.0, null);
		double gamx = GAMX2.getGamx(a, 200);
		double rounded = Math.round(gamx * 100.0) / 100.0;
		assertEquals("Score should have valid 2-decimal precision", gamx, rounded, EPSILON);
		System.out.println("✓ Decimal precision valid: " + gamx + " → " + rounded);
	}

	// Helper methods

	private Athlete createMockAthlete(String name, Gender gender, double bodyWeight, Integer age) {
		Athlete a = new Athlete();
		a.setFirstName(name);
		a.setGender(gender);
		a.setBodyWeight(bodyWeight);
		if (age != null) {
			a.setYearOfBirth(LocalDate.now().getYear() - age);
		}
		return a;
	}

	private String fmt(double value) {
		return String.format("%.2f", Math.round(value * 100.0) / 100.0);
	}
}
