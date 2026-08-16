package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.v2.AthleteDTO;
import app.owlcms.data.jpa.JPAService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class AthleteBirthDateTest {

	@BeforeClass
	public static void setUpDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Test
	public void storesYearOnlyBirthDateWithoutInventingJanuaryFirst() {
		Athlete athlete = new Athlete();

		athlete.setIsoBirthDate("2003");

		assertEquals("2003", athlete.getIsoBirthDate());
		assertEquals(LocalDate.of(2003, 1, 1), athlete.getFullBirthDate());
		assertEquals(Integer.valueOf(2003), athlete.getYearOfBirth());
	}

	@Test
	public void fullDateCompatibilitySetterPreservesJanuaryFirstPrecision() {
		Athlete athlete = new Athlete();

		athlete.setFullBirthDate(LocalDate.of(2003, 1, 1));

		assertEquals("2003-01-01", athlete.getIsoBirthDate());
		assertEquals(LocalDate.of(2003, 1, 1), athlete.getFullBirthDate());

		athlete.setFullBirthDate(null);
		assertNull(athlete.getIsoBirthDate());
		assertNull(athlete.getFullBirthDate());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void yearCompatibilitySetterStoresYearOnlyPrecision() {
		Athlete athlete = new Athlete();

		athlete.setYearOfBirth(2003);
		assertEquals("2003", athlete.getIsoBirthDate());

		athlete.setBirthDate(2004);
		assertEquals("2004", athlete.getIsoBirthDate());
	}

	@Test
	public void rejectsMalformedAndImpossibleIsoBirthDates() {
		Athlete athlete = new Athlete();

		assertInvalidBirthDate(athlete, "2003-2-01");
		assertInvalidBirthDate(athlete, "2003-02-30");
		assertInvalidBirthDate(athlete, "2003/02/01");
	}

	@Test
	public void v1ImportUsesIsoBirthDateRegardlessOfPropertyOrder() throws Exception {
		ObjectMapper mapper = JsonMapper.builder().build();

		Athlete legacyThenIso = mapper.readValue(
				"{\"fullBirthDate\":[2003,12,2],\"isoBirthDate\":\"2003\"}", Athlete.class);
		Athlete isoThenLegacy = mapper.readValue(
				"{\"isoBirthDate\":\"2003\",\"fullBirthDate\":[2003,12,2]}", Athlete.class);
		Athlete legacyOnly = mapper.readValue("{\"fullBirthDate\":[2003,12,2]}", Athlete.class);
		Athlete isoOnly = mapper.readValue("{\"isoBirthDate\":\"2003\"}", Athlete.class);

		assertEquals("2003", legacyThenIso.getIsoBirthDate());
		assertEquals("2003", isoThenLegacy.getIsoBirthDate());
		assertEquals("2003-12-02", legacyOnly.getIsoBirthDate());
		assertEquals("2003", isoOnly.getIsoBirthDate());

		String serialized = mapper.writeValueAsString(isoOnly);
		assertTrue(serialized.contains("\"fullBirthDate\""));
		assertTrue(serialized.contains("\"isoBirthDate\":\"2003\""));
	}

	@Test
	public void v2DtoRoundTripPreservesYearOnlyPrecision() {
		Athlete athlete = new Athlete();
		athlete.setIsoBirthDate("2003");

		AthleteDTO dto = AthleteDTO.fromAthlete(athlete);
		Athlete roundTripped = dto.toAthlete(null);

		assertEquals("2003", dto.getIsoBirthDate());
		assertEquals(LocalDate.of(2003, 1, 1), dto.getFullBirthDate());
		assertEquals("2003", roundTripped.getIsoBirthDate());
	}

	@Test
	public void v2DtoIsoBirthDateWinsOverConflictingLegacyDate() {
		Athlete source = new Athlete();
		source.setFullBirthDate(LocalDate.of(2003, 12, 2));
		AthleteDTO dto = AthleteDTO.fromAthlete(source);
		dto.setFullBirthDate(LocalDate.of(2003, 12, 2));
		dto.setIsoBirthDate("2004");

		Athlete athlete = dto.toAthlete(null);

		assertEquals("2004", athlete.getIsoBirthDate());
		assertEquals(LocalDate.of(2004, 1, 1), athlete.getFullBirthDate());
	}

	@Test
	public void conditionalCopyPreservesYearOnlyPrecision() {
		Athlete source = new Athlete();
		source.setIsoBirthDate("2003");
		Athlete destination = new Athlete();

		Athlete.conditionalCopy(destination, source, false, false, false);

		assertEquals("2003", destination.getIsoBirthDate());
	}

	private static void assertInvalidBirthDate(Athlete athlete, String value) {
		try {
			athlete.setIsoBirthDate(value);
			fail("Expected invalid birth date " + value);
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}