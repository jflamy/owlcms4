package app.owlcms.tests.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.BirthDateTextMigration;
import app.owlcms.data.jpa.JPAService;

public class BirthDateLegacySchemaMigrationTest extends ChampionshipLegacyMigrationSupport {

	private static final String V67_DATABASE = "/testDatabases/v67-birthdate.mv.db";

	@Test
	public void hibernateUpdateAndStartupMigrationPreserveLegacyBirthDates() throws Exception {
		loadFixtureIntoMemoryDatabase(V67_DATABASE, "v67-birthdate.mv.db");
		Config.initConfig();
		Map<Long, String> legacyBirthDates = readLegacyBirthDatesMissingIso();

		assertFalse("v67 fixture should contain legacy birth dates", legacyBirthDates.isEmpty());
		runMigration();
		assertMigratedValues(legacyBirthDates);

		runMigration();
		assertMigratedValues(legacyBirthDates);
	}

	private static void assertMigratedValues(Map<Long, String> legacyBirthDates) {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = JPAService.runInTransaction(em -> em.createNativeQuery(
				"SELECT id, isoBirthDate FROM Athlete WHERE fullBirthDate IS NOT NULL ORDER BY id")
				.getResultList());

		assertEquals(legacyBirthDates.size(), rows.size());
		for (Object[] row : rows) {
			assertEquals(legacyBirthDates.get(((Number) row[0]).longValue()), row[1]);
		}
	}

	private static Map<Long, String> readLegacyBirthDatesMissingIso() {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = JPAService.runInTransaction(em -> em.createNativeQuery(
				"SELECT id, fullBirthDate FROM Athlete "
						+ "WHERE fullBirthDate IS NOT NULL AND isoBirthDate IS NULL ORDER BY id")
				.getResultList());
		Map<Long, String> birthDates = new LinkedHashMap<>();
		for (Object[] row : rows) {
			birthDates.put(((Number) row[0]).longValue(), toIsoDate(row[1]));
		}
		return birthDates;
	}

	private static String toIsoDate(Object value) {
		if (value instanceof Timestamp timestamp) {
			return timestamp.toLocalDateTime().toLocalDate().toString();
		}
		if (value instanceof Date date) {
			return date.toLocalDate().toString();
		}
		if (value instanceof LocalDateTime dateTime) {
			return dateTime.toLocalDate().toString();
		}
		if (value instanceof LocalDate date) {
			return date.toString();
		}
		return value.toString();
	}

	private static void runMigration() {
		JPAService.runInTransaction(em -> {
			BirthDateTextMigration.migrate(em);
			return null;
		});
	}
}