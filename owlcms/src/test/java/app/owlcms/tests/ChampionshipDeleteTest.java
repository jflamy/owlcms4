/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

/**
 * Deletes championships for real, so it uses its own copy of the fixture rather than sharing ChampionshipTest's.
 */
public class ChampionshipDeleteTest {

	private static final String FIXTURE_RESOURCE = "/testDatabases/mixedTestsJRSR.mv.db";
	private static Path fixtureDirectory;

	@BeforeClass
	public static void setupTests() throws Exception {
		Main.injectSuppliers();
		String memoryJdbcUrl = "jdbc:h2:mem:championshipDeleteTest-" + UUID.randomUUID()
		        + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
		System.setProperty("JDBC_DATABASE_URL", memoryJdbcUrl);
		loadFixtureIntoMemoryDatabase(memoryJdbcUrl);
		JPAService.init(true, false);
		Config.initConfig();
		Competition.setCurrent(null);
		Championship.reset();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
	}

	@AfterClass
	public static void tearDownTests() throws Exception {
		try {
			Championship.reset();
		} catch (Exception e) {
			// DB may already be closed if setup failed
		}
		Competition.setCurrent(null);
		JPAService.close();
		System.clearProperty("JDBC_DATABASE_URL");
		deleteFixtureDirectory();
	}

	@Test
	public void testDeletingChampionshipsPromotesRemainingRegistrationCategory() {
		Championship junior = ChampionshipRepository.findByName("Junior");
		Championship senior = ChampionshipRepository.findByName("Senior");
		assertNotNull("Junior championship should be loaded from fixture", junior);
		assertNotNull("Senior championship should be loaded from fixture", senior);

		Athlete before = findAthlete("POURAMIN", "IRI");
		Category juniorCategory = categoryInChampionship(before, "Junior");
		Category seniorCategory = categoryInChampionship(before, "Senior");
		assertEquals("fixture should register POURAMIN in the more specific Junior category",
		        juniorCategory.getId(), before.getCategory().getId());

		Championship.removeWithAssociatedAgeGroups(junior, ageGroupsOf("Junior"));

		assertTrue("Junior age groups should be deleted", ageGroupsOf("Junior").isEmpty());
		Athlete afterJunior = AthleteRepository.findById(before.getId());
		assertNotNull("deleting a championship keeps its athletes", afterJunior);
		assertNotNull("POURAMIN should still have a registration category", afterJunior.getCategory());
		assertEquals("the Senior category should become the registration category",
		        seniorCategory.getId(), afterJunior.getCategory().getId());
		assertFalse("participations in deleted categories should be gone",
		        afterJunior.getParticipations().stream()
		                .anyMatch(p -> juniorCategory.getId().equals(p.getCategory().getId())));

		Championship.removeWithAssociatedAgeGroups(ChampionshipRepository.findByName("Senior"),
		        ageGroupsOf("Senior"));

		Athlete afterSenior = AthleteRepository.findById(before.getId());
		assertNotNull("deleting a championship keeps its athletes", afterSenior);
		assertNull("no registration category should remain once every eligible category is deleted",
		        afterSenior.getCategory());
		assertTrue("no participations should remain", afterSenior.getParticipations().isEmpty());
	}

	private static Athlete findAthlete(String lastName, String team) {
		Athlete athlete = AthleteRepository.findAll().stream()
		        .filter(a -> lastName.equalsIgnoreCase(a.getLastName()) && team.equalsIgnoreCase(a.getTeam()))
		        .findFirst()
		        .orElse(null);
		assertNotNull("expected athlete " + lastName + " from " + team + " in fixture", athlete);
		return athlete;
	}

	private static List<AgeGroup> ageGroupsOf(String championshipName) {
		return AgeGroupRepository.findAll().stream()
		        .filter(ag -> championshipName.equalsIgnoreCase(ag.getChampionshipName()))
		        .toList();
	}

	private static Category categoryInChampionship(Athlete athlete, String championshipName) {
		Set<Long> categoryIds = ageGroupsOf(championshipName).stream()
		        .flatMap(ag -> ag.getAllCategories().stream())
		        .map(Category::getId)
		        .collect(Collectors.toSet());
		Category category = athlete.getParticipations().stream()
		        .map(Participation::getCategory)
		        .filter(c -> c != null && categoryIds.contains(c.getId()))
		        .findFirst()
		        .orElse(null);
		assertNotNull(athlete.getFullName() + " should be eligible in " + championshipName, category);
		return category;
	}

	private static void loadFixtureIntoMemoryDatabase(String memoryJdbcUrl) throws IOException, SQLException {
		fixtureDirectory = Files.createTempDirectory("championship-delete-test-db-");
		Path copiedDatabase = fixtureDirectory.resolve("mixedTestsJRSR.mv.db");
		Path scriptFile = fixtureDirectory.resolve("mixedTestsJRSR.sql");

		try (InputStream fixtureStream = ChampionshipDeleteTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
			assertNotNull("Fixture database not found: " + FIXTURE_RESOURCE, fixtureStream);
			Files.copy(fixtureStream, copiedDatabase, StandardCopyOption.REPLACE_EXISTING);
		}

		String sourceBase = copiedDatabase.toAbsolutePath().toString().replaceAll("\\.mv\\.db$", "");
		String sourceUrl = "jdbc:h2:file:" + sourceBase + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0";
		String escapedScriptFile = scriptFile.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "''");

		try (Connection source = DriverManager.getConnection(sourceUrl, "sa", "");
		        Statement sourceStatement = source.createStatement()) {
			sourceStatement.execute("SCRIPT TO '" + escapedScriptFile + "'");
		}
		try (Connection target = DriverManager.getConnection(memoryJdbcUrl, "sa", "");
		        Statement targetStatement = target.createStatement()) {
			targetStatement.execute("RUNSCRIPT FROM '" + escapedScriptFile + "'");
		}
	}

	private static void deleteFixtureDirectory() throws IOException {
		if (fixtureDirectory == null || !Files.exists(fixtureDirectory)) {
			return;
		}
		try (var paths = Files.walk(fixtureDirectory)) {
			for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}
}
