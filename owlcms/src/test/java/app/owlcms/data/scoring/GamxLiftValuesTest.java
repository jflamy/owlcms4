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
 * GAMX2 Lift Values Test (Test B)
 * 
 * Verifies that snatch and clean & jerk GAMX parameters are correctly loaded,
 * structurally valid, and produce expected scoring behavior.
 * 
 * Success criteria:
 * - Snatch and CJ parameters load without errors for all variants
 * - Scores are non-zero for valid inputs
 * - Snatch ≠ CJ ≠ TOTAL (different parameter sets produce different scores)
 * - Scores are monotonically increasing with weight (higher weight = higher score)
 * - Values are in reasonable ranges
 * - Both SEN and MAS variants work for snatch/CJ
 */
public class GamxLiftValuesTest {

	private static final double EPSILON = 0.01;

	@BeforeClass
	public static void setUp() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		TestData.insertInitialData(5, true);
		System.out.println("\n========== GAMX2 Lift Values Test (Test B) ==========");
	}

	@AfterClass
	public static void tearDown() {
		JPAService.close();
	}

	/** Test that snatch parameters load and produce valid scores (male) */
	@Test
	public void testSnatchSeniorMale() {
		Athlete a = createMockAthlete("Snatcher", Gender.M, 80.0, null);
		double snatchScore = GAMX2.getGamxSnatch(a, 150);
		assertNotEquals("Snatch GAMX should load successfully", 0.0, snatchScore, EPSILON);
		assertFalse("Snatch score should be a valid number", Double.isNaN(snatchScore) || Double.isInfinite(snatchScore));
		assertTrue("Snatch GAMX should be in reasonable range, got " + snatchScore, snatchScore > 800 && snatchScore < 1400);
		System.out.println("✓ Snatch SENIOR male: GAMX-S(150kg, 80kg) = " + fmt(snatchScore));
	}

	/** Test that snatch works for female athlete */
	@Test
	public void testSnatchSeniorFemale() {
		Athlete a = createMockAthlete("Snatcher-F", Gender.F, 65.0, null);
		double snatchScore = GAMX2.getGamxSnatch(a, 110);
		assertNotEquals("Snatch GAMX should be non-zero", 0.0, snatchScore, EPSILON);
		assertTrue("Snatch GAMX should be in reasonable range", snatchScore > 800 && snatchScore < 1400);
		System.out.println("✓ Snatch SENIOR female: GAMX-S(110kg, 65kg) = " + fmt(snatchScore));
	}

	/** Test that C&J parameters load and produce valid scores (male) */
	@Test
	public void testCJSeniorMale() {
		Athlete a = createMockAthlete("Jerker", Gender.M, 80.0, null);
		double cjScore = GAMX2.getGamxCJ(a, 190);
		assertNotEquals("C&J GAMX should load successfully", 0.0, cjScore, EPSILON);
		assertFalse("C&J score should be a valid number", Double.isNaN(cjScore) || Double.isInfinite(cjScore));
		assertTrue("C&J GAMX should be in reasonable range, got " + cjScore, cjScore > 800 && cjScore < 1400);
		System.out.println("✓ C&J SENIOR male: GAMX-CJ(190kg, 80kg) = " + fmt(cjScore));
	}

	/** Test that C&J works for female athlete */
	@Test
	public void testCJSeniorFemale() {
		Athlete a = createMockAthlete("Jerker-F", Gender.F, 65.0, null);
		double cjScore = GAMX2.getGamxCJ(a, 140);
		assertNotEquals("C&J GAMX should be non-zero", 0.0, cjScore, EPSILON);
		assertTrue("C&J GAMX should be in reasonable range", cjScore > 800 && cjScore < 1400);
		System.out.println("✓ C&J SENIOR female: GAMX-CJ(140kg, 65kg) = " + fmt(cjScore));
	}

	/** Test that Snatch ≠ C&J ≠ TOTAL (different parameter sets) */
	@Test
	public void testLiftsDiffer() {
		Athlete a = createMockAthlete("Multi", Gender.M, 75.0, null);
		double snatchScore = GAMX2.getGamxSnatch(a, 120);
		double cjScore = GAMX2.getGamxCJ(a, 150);
		double totalScore = GAMX2.getGamx(a, 200);
		
		assertNotEquals("Snatch and C&J should have different scores", snatchScore, cjScore, EPSILON);
		assertNotEquals("Snatch and Total should have different scores", snatchScore, totalScore, EPSILON);
		assertNotEquals("C&J and Total should have different scores", cjScore, totalScore, EPSILON);
		System.out.println("✓ Lifts differ: GAMX-S=" + fmt(snatchScore) + ", GAMX-CJ=" + fmt(cjScore) + ", GAMX=" + fmt(totalScore));
	}

	/** Test snatch monotonicity: higher weights produce higher scores */
	@Test
	public void testSnatchMonotonicity() {
		Athlete a = createMockAthlete("Test", Gender.M, 80.0, null);
		double s140 = GAMX2.getGamxSnatch(a, 140);
		double s150 = GAMX2.getGamxSnatch(a, 150);
		double s160 = GAMX2.getGamxSnatch(a, 160);
		
		assertTrue("Snatch(140) should be non-zero", s140 > 0);
		assertTrue("Snatch(150) should be > Snatch(140)", s150 > s140);
		assertTrue("Snatch(160) should be > Snatch(150)", s160 > s150);
		System.out.println("✓ Snatch monotonicity: " + fmt(s140) + " < " + fmt(s150) + " < " + fmt(s160));
	}

	/** Test C&J monotonicity: higher weights produce higher scores */
	@Test
	public void testCJMonotonicity() {
		Athlete a = createMockAthlete("Test", Gender.M, 80.0, null);
		double cj170 = GAMX2.getGamxCJ(a, 170);
		double cj180 = GAMX2.getGamxCJ(a, 180);
		double cj190 = GAMX2.getGamxCJ(a, 190);
		
		assertTrue("C&J(170) should be non-zero", cj170 > 0);
		assertTrue("C&J(180) should be > C&J(170)", cj180 > cj170);
		assertTrue("C&J(190) should be > C&J(180)", cj190 > cj180);
		System.out.println("✓ C&J monotonicity: " + fmt(cj170) + " < " + fmt(cj180) + " < " + fmt(cj190));
	}

	/** Test that MASTERS variant works for snatch (age-dependent) */
	@Test
	public void testSnatchMasters() {
		Athlete a = createMockAthlete("OldSnatcher", Gender.M, 80.0, 50);
		double snatchMasters = GAMX2.getGamxSnatch(a, 140);
		assertNotEquals("Snatch MASTERS should load (age params exist)", 0.0, snatchMasters, EPSILON);
		assertFalse("Snatch MASTERS score should be valid", Double.isNaN(snatchMasters) || Double.isInfinite(snatchMasters));
		assertTrue("Snatch MASTERS should be in reasonable range", snatchMasters > 800 && snatchMasters < 1400);
		System.out.println("✓ Snatch MASTERS (age 50): GAMX-S(140kg, 80kg) = " + fmt(snatchMasters));
	}

	/** Test that MASTERS variant works for C&J (age-dependent) */
	@Test
	public void testCJMasters() {
		Athlete a = createMockAthlete("OldJerker", Gender.M, 80.0, 50);
		double cjMasters = GAMX2.getGamxCJ(a, 180);
		assertNotEquals("C&J MASTERS should load (age params exist)", 0.0, cjMasters, EPSILON);
		assertFalse("C&J MASTERS score should be valid", Double.isNaN(cjMasters) || Double.isInfinite(cjMasters));
		assertTrue("C&J MASTERS should be in reasonable range", cjMasters > 800 && cjMasters < 1400);
		System.out.println("✓ C&J MASTERS (age 50): GAMX-CJ(180kg, 80kg) = " + fmt(cjMasters));
	}

	/** Test that all lift methods execute without throwing exceptions */
	@Test
	public void testAllLiftsNoErrors() {
		Athlete male = createMockAthlete("M", Gender.M, 80.0, null);
		Athlete female = createMockAthlete("F", Gender.F, 65.0, null);
		Athlete master = createMockAthlete("Master", Gender.M, 75.0, 50);
		
		assertTrue("Snatch male", GAMX2.getGamxSnatch(male, 140) > 0);
		assertTrue("Snatch female", GAMX2.getGamxSnatch(female, 100) > 0);
		assertTrue("Snatch master", GAMX2.getGamxSnatch(master, 130) > 0);
		assertTrue("C&J male", GAMX2.getGamxCJ(male, 180) > 0);
		assertTrue("C&J female", GAMX2.getGamxCJ(female, 130) > 0);
		assertTrue("C&J master", GAMX2.getGamxCJ(master, 170) > 0);
		System.out.println("✓ All lift methods execute without errors");
	}

	/** Test edge case: zero weight returns 0 */
	@Test
	public void testZeroWeight() {
		Athlete a = createMockAthlete("Test", Gender.M, 80.0, null);
		assertEquals("Snatch(0) should return 0", 0.0, GAMX2.getGamxSnatch(a, 0), EPSILON);
		assertEquals("C&J(0) should return 0", 0.0, GAMX2.getGamxCJ(a, 0), EPSILON);
		System.out.println("✓ Zero weight handling: returns 0.0");
	}

	/** Test edge case: null athlete returns 0 */
	@Test
	public void testNullAthlete() {
		assertEquals("Snatch(null) should return 0", 0.0, GAMX2.getGamxSnatch(null, 150), EPSILON);
		assertEquals("C&J(null) should return 0", 0.0, GAMX2.getGamxCJ(null, 190), EPSILON);
		System.out.println("✓ Null athlete handling: returns 0.0");
	}

	/** Test edge case: negative weight returns 0 */
	@Test
	public void testNegativeWeight() {
		Athlete a = createMockAthlete("Test", Gender.M, 80.0, null);
		assertEquals("Snatch(-100) should return 0", 0.0, GAMX2.getGamxSnatch(a, -100), EPSILON);
		assertEquals("C&J(-100) should return 0", 0.0, GAMX2.getGamxCJ(a, -100), EPSILON);
		System.out.println("✓ Negative weight handling: returns 0.0");
	}

	/** Test that female scores differ from male scores (different gender tables) */
	@Test
	public void testGenderDifference() {
		Athlete male = createMockAthlete("M", Gender.M, 75.0, null);
		Athlete female = createMockAthlete("F", Gender.F, 75.0, null);
		double maleSnatch = GAMX2.getGamxSnatch(male, 140);
		double femaleSnatch = GAMX2.getGamxSnatch(female, 140);
		assertNotEquals("Male and female should have different snatch scores", maleSnatch, femaleSnatch, EPSILON);
		System.out.println("✓ Gender differences: Male=" + fmt(maleSnatch) + ", Female=" + fmt(femaleSnatch));
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
