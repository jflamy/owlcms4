package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.sql.Date;
import java.time.LocalDate;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.BirthDateTextMigration;
import app.owlcms.data.jpa.JPAService;

public class BirthDateTextMigrationTest {

	private static long nextAthleteId;

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

	@Before
	public void resetLegacyColumn() {
		JPAService.runInTransaction(em -> {
			em.createNativeQuery("DELETE FROM Athlete").executeUpdate();
			em.createNativeQuery("ALTER TABLE Athlete DROP COLUMN IF EXISTS fullBirthDate").executeUpdate();
			return null;
		});
	}

	@Test
	public void migratesLegacyDatesExactlyAndIsIdempotent() {
		addLegacyBirthDateColumn();
		Long ordinaryDateId = insertAthlete(LocalDate.of(2003, 12, 2), null);
		Long januaryFirstId = insertAthlete(LocalDate.of(2003, 1, 1), null);

		runMigration();
		assertIsoBirthDate(ordinaryDateId, "2003-12-02");
		assertIsoBirthDate(januaryFirstId, "2003-01-01");

		runMigration();
		assertIsoBirthDate(ordinaryDateId, "2003-12-02");
		assertIsoBirthDate(januaryFirstId, "2003-01-01");
	}

	@Test
	public void doesNotOverwriteExistingIsoBirthDate() {
		addLegacyBirthDateColumn();
		Long athleteId = insertAthlete(LocalDate.of(2003, 12, 2), "2003");

		runMigration();

		assertIsoBirthDate(athleteId, "2003");
	}

	@Test
	public void acceptsNewDatabaseWithoutLegacyColumn() {
		runMigration();
	}

	private static void addLegacyBirthDateColumn() {
		JPAService.runInTransaction(em -> {
			em.createNativeQuery("ALTER TABLE Athlete ADD fullBirthDate DATE").executeUpdate();
			return null;
		});
	}

	private static Long insertAthlete(LocalDate legacyBirthDate, String isoBirthDate) {
		return JPAService.runInTransaction(em -> {
			Athlete athlete = new Athlete();
			athlete.setId(++nextAthleteId);
			athlete.setIsoBirthDate(isoBirthDate);
			em.persist(athlete);
			em.flush();
			em.createNativeQuery("UPDATE Athlete SET fullBirthDate = ? WHERE id = ?")
					.setParameter(1, Date.valueOf(legacyBirthDate))
					.setParameter(2, athlete.getId())
					.executeUpdate();
			return athlete.getId();
		});
	}

	private static void runMigration() {
		JPAService.runInTransaction(em -> {
			BirthDateTextMigration.migrate(em);
			return null;
		});
	}

	private static void assertIsoBirthDate(Long athleteId, String expected) {
		JPAService.runInTransaction(em -> {
			em.clear();
			assertEquals(expected, em.find(Athlete.class, athleteId).getIsoBirthDate());
			return null;
		});
	}
}