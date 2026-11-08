/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupChange;
import app.owlcms.data.agegroup.AgeGroupDefinitionService;
import app.owlcms.data.agegroup.AgeGroupDefinitionService.Decision;
import app.owlcms.data.agegroup.AgeGroupDefinitionService.SaveOutcome;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.SessionsInProgressException;
import app.owlcms.data.agegroup.reassign.LeaveReason;
import app.owlcms.data.agegroup.reassign.ReassignmentPreview;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.JpaTransactions;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.TestFopRegistry;

/**
 * Age-group definition saves (compliance item 200, phase 3). The scratchpads are built the way the editor builds
 * them: detached copies whose categories have new ids and empty participation lists. Domain code only, no Vaadin
 * objects in the assertions path (the session-guard test drives a mock field of play, not a UI).
 */
public class AgeGroupDefinitionSaveTest {

	private static final String FIXTURE_RESOURCE = "/testDatabases/mixedTestsJRSR.mv.db";
	private static Path fixtureDirectory;
	private final AgeGroupDefinitionService service = new AgeGroupDefinitionService();

	@BeforeClass
	public static void setupTests() throws Exception {
		Main.injectSuppliers();
		String memoryJdbcUrl = "jdbc:h2:mem:ageGroupDefinitionSaveTest-" + UUID.randomUUID()
		        + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
		System.setProperty("JDBC_DATABASE_URL", memoryJdbcUrl);
		loadFixtureIntoMemoryDatabase(memoryJdbcUrl);
		JPAService.init(true, false);
		Config.initConfig();
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

	@Before
	public void competitionExists() {
		if (CompetitionRepository.findAll().isEmpty()) {
			CompetitionRepository.create(new Competition(), AuditOrigin.system("test"));
		}
		Competition current = Competition.getCurrent();
		if (current != null && !LocalDate.now().equals(current.getCompetitionDate())) {
			current.setCompetitionDate(LocalDate.now());
		}
	}

	@After
	public void clearHooks() {
		AgeGroupDefinitionService.afterStepHookForTesting = null;
		AgeGroupDefinitionService.beforeRefreshStepHookForTesting = null;
	}

	// ---------------------------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------------------------

	private static Long newAgeGroup(String code, String championship, Gender gender, int minAge, int maxAge,
	        boolean active, int qualifyingTotal, double... upperLimits) {
		return JpaTransactions.inTransaction(em -> {
			AgeGroup ageGroup = new AgeGroup(code, active, minAge, maxAge, gender, "U", 0);
			ageGroup.setChampionshipName(championship);
			double lower = 0.0;
			for (double upper : upperLimits) {
				ageGroup.addCategory(new Category(lower, upper, gender, active, 0, 0, 0, ageGroup, qualifyingTotal));
				lower = upper;
			}
			ageGroup.stamp(AuditOrigin.system("test"), null);
			em.persist(ageGroup);
			em.flush();
			return ageGroup.getId();
		});
	}

	private static Long newAthlete(String lastName, Gender gender, Integer age, Double bodyWeight, Integer entryTotal) {
		return JpaTransactions.inTransaction(em -> {
			Athlete athlete = new Athlete();
			athlete.setLastName(lastName);
			athlete.setFirstName("T");
			athlete.setGender(gender);
			if (age != null) {
				athlete.setYearOfBirth(LocalDate.now().getYear() - age);
			}
			if (bodyWeight != null) {
				athlete.setBodyWeight(bodyWeight);
			}
			if (entryTotal != null) {
				athlete.setQualifyingTotal(entryTotal);
			}
			em.persist(athlete);
			em.flush();
			return athlete.getId();
		});
	}

	private static void participate(Long athleteId, Long categoryId, boolean team, boolean mixed) {
		JpaTransactions.inTransaction(em -> {
			Athlete athlete = em.find(Athlete.class, athleteId);
			Category category = em.find(Category.class, categoryId);
			athlete.addEligibleCategory(category, team, mixed);
			em.flush();
			return null;
		});
	}

	private static void setRegistration(Long athleteId, Long categoryId) {
		JpaTransactions.inTransaction(em -> {
			Athlete athlete = em.find(Athlete.class, athleteId);
			athlete.setCategory(categoryId != null ? em.find(Category.class, categoryId) : null);
			em.flush();
			return null;
		});
	}

	private static void setSnatchRank(Long athleteId, Long categoryId, int rank) {
		JpaTransactions.inTransaction(em -> {
			for (Participation p : em.find(Athlete.class, athleteId).getParticipations()) {
				if (p.getCategory() != null && p.getCategory().getId().equals(categoryId)) {
					p.setSnatchRank(rank);
				}
			}
			em.flush();
			return null;
		});
	}

	/** The editor shape: the scratchpad copy constructor carries the id and stamps; its categories have new ids. */
	private static AgeGroup scratchOf(Long ageGroupId) {
		return new AgeGroup(AgeGroupRepository.loadForEdit(ageGroupId));
	}

	private static Long stampOf(Long ageGroupId) {
		return AgeGroupRepository.loadForEdit(ageGroupId).getLastSavedAt();
	}

	private static Map<Double, Long> categoryIdsByMax(Long ageGroupId) {
		return JpaTransactions.inTransaction(em -> {
			Map<Double, Long> result = new LinkedHashMap<>();
			for (Category c : em.find(AgeGroup.class, ageGroupId).getAllCategories()) {
				result.put(c.getMaximumWeight(), c.getId());
			}
			return result;
		});
	}

	private static Double minimumWeightOf(Long ageGroupId, double maxWeight) {
		return JpaTransactions.inTransaction(em -> {
			for (Category c : em.find(AgeGroup.class, ageGroupId).getAllCategories()) {
				if (c.getMaximumWeight() == maxWeight) {
					return c.getMinimumWeight();
				}
			}
			return null;
		});
	}

	private static Integer storedQt(Long ageGroupId, double maxWeight) {
		return JpaTransactions.inTransaction(em -> {
			for (Category c : em.find(AgeGroup.class, ageGroupId).getAllCategories()) {
				if (c.getMaximumWeight() == maxWeight) {
					return c.getQualifyingTotal();
				}
			}
			return null;
		});
	}

	private static String storedCode(Long categoryId) {
		return JpaTransactions.inTransaction(em -> em.find(Category.class, categoryId).getCode());
	}

	private static String storedChampionshipName(Long ageGroupId) {
		return JpaTransactions.inTransaction(em -> em.find(AgeGroup.class, ageGroupId).getChampionshipName());
	}

	private static Map<Long, boolean[]> flagsByCategoryId(Long athleteId) {
		return JpaTransactions.inTransaction(em -> {
			Map<Long, boolean[]> result = new LinkedHashMap<>();
			for (Participation p : em.find(Athlete.class, athleteId).getParticipations()) {
				result.put(p.getCategory().getId(), new boolean[] { p.getTeamMember(), p.getMixedTeamMember() });
			}
			return result;
		});
	}

	private static Integer snatchRank(Long athleteId, Long categoryId) {
		return JpaTransactions.inTransaction(em -> {
			for (Participation p : em.find(Athlete.class, athleteId).getParticipations()) {
				if (p.getCategory() != null && p.getCategory().getId().equals(categoryId)) {
					return p.getSnatchRank();
				}
			}
			return null;
		});
	}

	private static Long registrationId(Long athleteId) {
		return JpaTransactions.inTransaction(em -> {
			Category registration = em.find(Athlete.class, athleteId).getCategory();
			return registration != null ? registration.getId() : null;
		});
	}

	private static int categoryParticipationCount(Long categoryId) {
		return JpaTransactions.inTransaction(em -> {
			Category category = em.find(Category.class, categoryId);
			return category != null ? category.getParticipations().size() : 0;
		});
	}

	private static boolean championshipExists(String name) {
		return JpaTransactions.inTransaction(em -> !em.createQuery(
		        "select c from Championship c where lower(trim(c.name)) = :name", Championship.class)
		        .setParameter("name", name.trim().toLowerCase()).getResultList().isEmpty());
	}

	private static long championshipRowCount() {
		return JpaTransactions.inTransaction(em -> em
		        .createQuery("select count(c) from Championship c", Long.class).getSingleResult());
	}

	private static boolean ageGroupExists(Long id) {
		return JpaTransactions.inTransaction(em -> em.find(AgeGroup.class, id) != null);
	}

	private static void setCategoryQt(AgeGroup scratch, double maxWeight, int qt) {
		for (Category c : scratch.getCategories()) {
			if (c.getMaximumWeight() == maxWeight) {
				c.setQualifyingTotal(qt);
				return;
			}
		}
		throw new IllegalArgumentException("no category with upper limit " + maxWeight);
	}

	private static void removeFromScratch(AgeGroup scratch, double maxWeight) {
		Category found = null;
		for (Category c : scratch.getCategories()) {
			if (c.getMaximumWeight() == maxWeight) {
				found = c;
				break;
			}
		}
		if (found == null) {
			throw new IllegalArgumentException("no category with upper limit " + maxWeight);
		}
		scratch.removeCategory(found);
	}

	private static void addToScratch(AgeGroup scratch, double maxWeight, int qt) {
		scratch.addCategory(new Category(0.0, maxWeight, scratch.getGender(), true, 0, 0, 0, null, qt));
	}

	/** A hand-built age group for pure classification checks; never persisted. */
	private static AgeGroup handBuilt(String code, String championship, Gender gender, int minAge, int maxAge,
	        boolean active, boolean medals, int qt, double... limits) {
		AgeGroup ageGroup = new AgeGroup(code, active, minAge, maxAge, gender, "U", 0);
		ageGroup.setChampionshipName(championship);
		ageGroup.setMedals(medals);
		double lower = 0.0;
		for (double limit : limits) {
			ageGroup.addCategory(new Category(lower, limit, gender, active, 0, 0, 0, ageGroup, qt));
			lower = limit;
		}
		return ageGroup;
	}

	private static void loadFixtureIntoMemoryDatabase(String memoryJdbcUrl) throws IOException, SQLException {
		fixtureDirectory = Files.createTempDirectory("agegroup-definition-save-test-db-");
		Path copiedDatabase = fixtureDirectory.resolve("mixedTestsJRSR.mv.db");
		Path scriptFile = fixtureDirectory.resolve("mixedTestsJRSR.sql");

		try (InputStream fixtureStream = AgeGroupDefinitionSaveTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
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

	// ---------------------------------------------------------------------------------------------
	// classification (spec 3.3): pure object checks, no database
	// ---------------------------------------------------------------------------------------------

	@Test
	public void classificationRules() {
		AgeGroup stored = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, true, 0, 73, 81, 999);

		AgeGroup same = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, true, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.NONE, AgeGroupChange.classify(stored, same));

		AgeGroup renamed = handBuilt("CL36", "ClassM", Gender.M, 35, 39, true, true, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.DIRECT, AgeGroupChange.classify(stored, renamed));
		AgeGroup medals = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, false, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.DIRECT, AgeGroupChange.classify(stored, medals));
		AgeGroup gendered = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, true, 0, 73, 81, 999);
		gendered.setAlreadyGendered(true);
		assertEquals(AgeGroupChange.Kind.DIRECT, AgeGroupChange.classify(stored, gendered));

		AgeGroup ageRange = handBuilt("CL35", "ClassM", Gender.M, 35, 38, true, true, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.REASSIGN, AgeGroupChange.classify(stored, ageRange));
		AgeGroup gender = handBuilt("CL35", "ClassM", Gender.F, 35, 39, true, true, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.REASSIGN, AgeGroupChange.classify(stored, gender));
		AgeGroup inactive = handBuilt("CL35", "ClassM", Gender.M, 35, 39, false, true, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.REASSIGN, AgeGroupChange.classify(stored, inactive));
		AgeGroup qt = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, true, 100, 73, 81, 999);
		assertEquals("a QT change reassigns", AgeGroupChange.Kind.REASSIGN, AgeGroupChange.classify(stored, qt));
		AgeGroup added = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, true, 0, 73, 81, 85, 999);
		assertEquals("an added upper limit reassigns", AgeGroupChange.Kind.REASSIGN,
		        AgeGroupChange.classify(stored, added));
		AgeGroup removed = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, true, 0, 73, 999);
		assertEquals("a removed category reassigns", AgeGroupChange.Kind.REASSIGN,
		        AgeGroupChange.classify(stored, removed));

		AgeGroup moved = handBuilt("CL35", "OtherChamp", Gender.M, 35, 39, true, true, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.CHAMPIONSHIP_ONLY, AgeGroupChange.classify(stored, moved));
		AgeGroup mixed = handBuilt("CL35", "OtherChamp", Gender.M, 35, 39, true, false, 0, 73, 81, 999);
		assertEquals(AgeGroupChange.Kind.CHAMPIONSHIP_MIXED, AgeGroupChange.classify(stored, mixed));

		AgeGroup duplicate = handBuilt("CL35", "ClassM", Gender.M, 35, 39, true, true, 0, 73, 81, 999);
		duplicate.addCategory(new Category(73.0, 81.0, Gender.M, true, 0, 0, 0, duplicate, 50));
		assertThrows("a duplicate upper limit is refused", IllegalArgumentException.class,
		        () -> AgeGroupChange.classify(stored, duplicate));
	}

	// ---------------------------------------------------------------------------------------------
	// data preservation (the 40 -> 0 regression shape) and category identity (spec 3.2 / 10)
	// ---------------------------------------------------------------------------------------------

	/** A QT edit: same category ids, kept participations keep flags and ranks, only the QT-failing athlete moves. */
	@Test
	public void qtEditPreservesIdsParticipationsAndFlags() {
		newChampionshipRow("PresM", true);
		Long ag = newAgeGroup("PM35", "PresM", Gender.M, 35, 39, true, 0, 73, 81, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long a1 = newAthlete("PresOne", Gender.M, 36, 70.0, 150);
		participate(a1, cats.get(73.0), true, false);
		setRegistration(a1, cats.get(73.0));
		setSnatchRank(a1, cats.get(73.0), 2);
		Long a2 = newAthlete("PresTwo", Gender.M, 36, 80.0, 150);
		participate(a2, cats.get(81.0), false, true);
		setRegistration(a2, cats.get(81.0));
		Long a3 = newAthlete("PresThree", Gender.M, 36, 90.0, 150);
		participate(a3, cats.get(999.0), true, false);
		Long a4 = newAthlete("PresFour", Gender.M, 36, 78.0, 90);
		participate(a4, cats.get(81.0), true, false);
		setRegistration(a4, cats.get(81.0));

		AgeGroup scratch = scratchOf(ag);
		setCategoryQt(scratch, 81.0, 100);
		SaveOutcome outcome = service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.APPLY);

		assertEquals(AgeGroupChange.Kind.REASSIGN, outcome.change());
		assertTrue(outcome.refreshFailures().toString(), outcome.refreshFailures().isEmpty());
		assertEquals("every category keeps its id", cats, categoryIdsByMax(ag));
		assertEquals(Integer.valueOf(100), storedQt(ag, 81.0));

		boolean[] f1 = flagsByCategoryId(a1).get(cats.get(73.0));
		assertNotNull(f1);
		assertTrue(f1[0]);
		assertFalse(f1[1]);
		assertEquals("same row: ranks survive", 2, snatchRank(a1, cats.get(73.0)).intValue());
		boolean[] f2 = flagsByCategoryId(a2).get(cats.get(81.0));
		assertNotNull(f2);
		assertFalse(f2[0]);
		assertTrue("mixed kept: the championship allows it", f2[1]);
		assertTrue(flagsByCategoryId(a3).containsKey(cats.get(999.0)));

		assertTrue("the QT-failing athlete leaves the category", flagsByCategoryId(a4).isEmpty());
		assertNull(registrationId(a4));
		assertEquals(3, Championship.getParticipantCount("PresM"));
	}

	/** Adding 85 creates it; 89 keeps its id; athletes above 81 up to 85 move from 89 to 85. */
	@Test
	public void addedLimitKeepsUpperCategoryId() {
		newChampionshipRow("AddM", false);
		Long ag = newAgeGroup("AM35", "AddM", Gender.M, 35, 39, true, 0, 73, 81, 999);
		Map<Double, Long> before = categoryIdsByMax(ag);
		Long b1 = newAthlete("AddOne", Gender.M, 36, 78.0, 100);
		participate(b1, before.get(81.0), true, false);
		Long b2 = newAthlete("AddTwo", Gender.M, 36, 83.0, 100);
		participate(b2, before.get(999.0), true, false);
		setRegistration(b2, before.get(999.0));
		Long b3 = newAthlete("AddThree", Gender.M, 36, 90.0, 100);
		participate(b3, before.get(999.0), true, false);

		AgeGroup scratch = scratchOf(ag);
		addToScratch(scratch, 85.0, 0);
		service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.APPLY);

		Map<Double, Long> after = categoryIdsByMax(ag);
		assertEquals(before.get(73.0), after.get(73.0));
		assertEquals("the category just above keeps its id", before.get(999.0), after.get(999.0));
		assertNotNull(after.get(85.0));
		assertFalse("the new limit is a new row", before.containsValue(after.get(85.0)));
		assertEquals("lower limit follows the new limit", 85.0, minimumWeightOf(ag, 999.0), 0.001);
		assertEquals(81.0, minimumWeightOf(ag, 85.0), 0.001);

		assertTrue(flagsByCategoryId(b1).containsKey(before.get(81.0)));
		assertTrue("above 81 up to 85 moves to the new category", flagsByCategoryId(b2).containsKey(after.get(85.0)));
		assertEquals(after.get(85.0), registrationId(b2));
		assertTrue("the others keep the unlimited participation", flagsByCategoryId(b3).containsKey(before.get(999.0)));
	}

	/** Deleting 81 removes its row after its athletes move to 89; 89 keeps its id and now starts at 73. */
	@Test
	public void deletedLimitRemovesAndRemaps() {
		newChampionshipRow("DelLimM", false);
		Long ag = newAgeGroup("DLM35", "DelLimM", Gender.M, 35, 39, true, 0, 73, 81, 999);
		Map<Double, Long> before = categoryIdsByMax(ag);
		Long c1 = newAthlete("DelOne", Gender.M, 36, 78.0, 100);
		participate(c1, before.get(81.0), true, false);
		setRegistration(c1, before.get(81.0));
		Long c2 = newAthlete("DelTwo", Gender.M, 36, 70.0, 100);
		participate(c2, before.get(73.0), true, false);
		Long c3 = newAthlete("DelThree", Gender.M, 36, 90.0, 100);
		participate(c3, before.get(999.0), true, false);

		AgeGroup scratch = scratchOf(ag);
		removeFromScratch(scratch, 81.0);
		service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.APPLY);

		Map<Double, Long> after = categoryIdsByMax(ag);
		assertNull("the deleted category is gone", after.get(81.0));
		assertEquals(before.get(73.0), after.get(73.0));
		assertEquals("the category above keeps its id", before.get(999.0), after.get(999.0));
		assertEquals("its lower limit becomes the deleted category's lower limit", 73.0,
		        minimumWeightOf(ag, 999.0), 0.001);

		assertTrue("the deleted category's athletes move up", flagsByCategoryId(c1).containsKey(before.get(999.0)));
		assertEquals(before.get(999.0), registrationId(c1));
		assertTrue(flagsByCategoryId(c2).containsKey(before.get(73.0)));
		assertTrue(flagsByCategoryId(c3).containsKey(before.get(999.0)));
		assertEquals(0, categoryParticipationCount(before.get(81.0)));
	}

	/** Moving a limit (81 to 83) is delete plus add: 81 gone, 83 created, 89 keeps its id with lower limit 83. */
	@Test
	public void movedLimitIsDeletePlusAdd() {
		newChampionshipRow("MoveLimM", false);
		Long ag = newAgeGroup("MLM35", "MoveLimM", Gender.M, 35, 39, true, 0, 73, 81, 89, 999);
		Map<Double, Long> before = categoryIdsByMax(ag);
		Long d1 = newAthlete("MoveOne", Gender.M, 36, 78.0, 100);
		participate(d1, before.get(81.0), true, false);
		Long d2 = newAthlete("MoveTwo", Gender.M, 36, 85.0, 100);
		participate(d2, before.get(89.0), true, false);

		AgeGroup scratch = scratchOf(ag);
		removeFromScratch(scratch, 81.0);
		addToScratch(scratch, 83.0, 0);
		service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.APPLY);

		Map<Double, Long> after = categoryIdsByMax(ag);
		assertNull(after.get(81.0));
		assertNotNull(after.get(83.0));
		assertFalse(before.containsValue(after.get(83.0)));
		assertEquals("89 keeps its id", before.get(89.0), after.get(89.0));
		assertEquals(83.0, minimumWeightOf(ag, 89.0), 0.001);

		assertTrue("up to 83 goes to the new category", flagsByCategoryId(d1).containsKey(after.get(83.0)));
		assertTrue("above 81 keeps the 89 participation", flagsByCategoryId(d2).containsKey(before.get(89.0)));
	}

	/** The unlimited category keeps its id; deleting every finite limit leaves it covering 0 to 999. */
	@Test
	public void unlimitedCategorySurvivesDeletingAllFinite() {
		newChampionshipRow("UnlM", false);
		Long ag = newAgeGroup("UM35", "UnlM", Gender.M, 35, 39, true, 0, 73, 81, 999);
		Map<Double, Long> before = categoryIdsByMax(ag);
		Long e1 = newAthlete("UnlOne", Gender.M, 36, 70.0, 100);
		participate(e1, before.get(73.0), true, false);
		Long e2 = newAthlete("UnlTwo", Gender.M, 36, 85.0, 100);
		participate(e2, before.get(999.0), true, false);

		AgeGroup scratch = scratchOf(ag);
		removeFromScratch(scratch, 73.0);
		removeFromScratch(scratch, 81.0);
		service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.APPLY);

		Map<Double, Long> after = categoryIdsByMax(ag);
		assertEquals(1, after.size());
		assertEquals(before.get(999.0), after.get(999.0));
		assertEquals(0.0, minimumWeightOf(ag, 999.0), 0.001);
		assertTrue(flagsByCategoryId(e1).containsKey(before.get(999.0)));
		assertTrue(flagsByCategoryId(e2).containsKey(before.get(999.0)));
	}

	/** A duplicate upper limit in the scratchpad is refused; nothing is written. */
	@Test
	public void duplicateUpperLimitRefused() {
		newChampionshipRow("DupM", false);
		Long ag = newAgeGroup("DM35", "DupM", Gender.M, 35, 39, true, 0, 73, 999);
		Map<Double, Long> before = categoryIdsByMax(ag);
		Long stamp = stampOf(ag);

		AgeGroup scratch = scratchOf(ag);
		scratch.addCategory(new Category(73.0, 81.0, Gender.M, true, 0, 0, 0, scratch, 0));
		scratch.addCategory(new Category(0.0, 73.0, Gender.M, true, 0, 0, 0, scratch, 0));

		assertThrows(IllegalArgumentException.class,
		        () -> service.save(stamp, AuditOrigin.system("test"), scratch, Decision.APPLY));
		assertEquals(before, categoryIdsByMax(ag));
		assertEquals("a refused save does not advance the stamp", stamp, stampOf(ag));
	}

	// ---------------------------------------------------------------------------------------------
	// scratchpad and copy-back (spec 11): cancel and preview write nothing
	// ---------------------------------------------------------------------------------------------

	/** Editing the copy without confirming leaves every stored row unchanged; the preview also writes nothing. */
	@Test
	public void cancelAndPreviewWriteNothing() {
		Long ag = newAgeGroup("CNM35", "CancelChamp", Gender.M, 35, 39, true, 0, 73, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long x1 = newAthlete("CancelOne", Gender.M, 36, 70.0, 100);
		participate(x1, cats.get(73.0), true, false);
		long championshipsBefore = championshipRowCount();

		AgeGroup scratch = scratchOf(ag);
		setCategoryQt(scratch, 73.0, 250);
		scratch.setMaxAge(38);
		removeFromScratch(scratch, 999.0);
		// never confirmed: stored rows unchanged
		assertEquals(Integer.valueOf(0), storedQt(ag, 73.0));
		assertEquals(cats, categoryIdsByMax(ag));

		// the dry run computes against the proposed definitions but commits nothing
		var preview = service.preview(scratch, stampOf(ag));
		assertNotNull(preview);
		assertEquals(Integer.valueOf(0), storedQt(ag, 73.0));
		assertEquals(cats, categoryIdsByMax(ag));
		assertTrue(flagsByCategoryId(x1).containsKey(cats.get(73.0)));
		assertEquals("no championship row materialized by the preview", championshipsBefore, championshipRowCount());
		assertFalse(championshipExists("CancelChamp"));
	}

	// ---------------------------------------------------------------------------------------------
	// Later (spec 3.4) and the global actions
	// ---------------------------------------------------------------------------------------------

	/** Later stores the definitions and keeps participations; "Reassign within championships" applies them. */
	@Test
	public void laterKeepsParticipationsUntilReassignWithin() {
		newChampionshipRow("LaterM", false);
		Long ag = newAgeGroup("LM35", "LaterM", Gender.M, 35, 39, true, 0, 73, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long l1 = newAthlete("LaterOne", Gender.M, 36, 70.0, 100);
		participate(l1, cats.get(73.0), true, false);
		setRegistration(l1, cats.get(73.0));

		AgeGroup scratch = scratchOf(ag);
		setCategoryQt(scratch, 73.0, 200);
		service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.LATER);

		assertEquals("definitions are stored", Integer.valueOf(200), storedQt(ag, 73.0));
		assertTrue("participations are identical until the reassignment",
		        flagsByCategoryId(l1).containsKey(cats.get(73.0)));

		List<ReassignmentPreview.Entry> leavers = service.previewReassignWithin().leavers("LaterM");
		assertTrue("the dry run lists the athlete leaving with the QT reason",
		        leavers.stream().anyMatch(e -> e.athleteId().equals(l1) && e.reason() == LeaveReason.QT));

		service.reassignWithin(AuditOrigin.system("test"));
		assertTrue(flagsByCategoryId(l1).isEmpty());
		assertNull(registrationId(l1));
	}

	/** Later is refused when categories are removed; nothing is written. */
	@Test
	public void laterRefusedWhenCategoriesRemoved() {
		newChampionshipRow("LaterDelM", false);
		Long ag = newAgeGroup("LDM35", "LaterDelM", Gender.M, 35, 39, true, 0, 73, 999);
		Map<Double, Long> before = categoryIdsByMax(ag);
		Long stamp = stampOf(ag);

		AgeGroup scratch = scratchOf(ag);
		removeFromScratch(scratch, 73.0);
		assertThrows(IllegalArgumentException.class,
		        () -> service.save(stamp, AuditOrigin.system("test"), scratch, Decision.LATER));
		assertEquals(before, categoryIdsByMax(ag));
		assertEquals(stamp, stampOf(ag));
	}

	/** "Recompute all eligibilities" adds an athlete who fits; "Reassign within championships" does not. */
	@Test
	public void recomputeAllAddsAndPreviewListsTheJoin() {
		newChampionshipRow("GlobM", false);
		Long ag = newAgeGroup("GM35", "GlobM", Gender.M, 35, 39, true, 0, 999);
		Long cat = categoryIdsByMax(ag).get(999.0);
		Long g1 = newAthlete("GlobOne", Gender.M, 36, 80.0, 100);

		assertTrue("not in the championship: nothing to leave, and never added",
		        service.previewReassignWithin().joiners("GlobM").stream()
		                .noneMatch(e -> e.athleteId().equals(g1)));
		assertTrue(service.previewRecomputeAll().joiners("GlobM").stream()
		        .anyMatch(e -> e.athleteId().equals(g1)));

		service.recomputeAllEligibilities(AuditOrigin.system("test"));
		assertTrue(flagsByCategoryId(g1).containsKey(cat));
	}

	// ---------------------------------------------------------------------------------------------
	// atomicity (spec 10): injected failure after each of steps 3 to 7 rolls everything back
	// ---------------------------------------------------------------------------------------------

	@Test
	public void atomicityRollsBackEveryStep() {
		for (int step = 3; step <= 7; step++) {
			final int failingStep = step;
			String championship = "AtomM" + step;
			String code = "AT35_" + step;
			Long ag = newAgeGroup(code, championship, Gender.M, 35, 39, true, 0, 73, 81, 999);
			Map<Double, Long> cats = categoryIdsByMax(ag);
			Long t1 = newAthlete("AtomOne" + step, Gender.M, 36, 70.0, 100);
			participate(t1, cats.get(73.0), true, false);
			Long t2 = newAthlete("AtomTwo" + step, Gender.M, 36, 80.0, 150);
			participate(t2, cats.get(81.0), true, true);
			Long stamp = stampOf(ag);

			AgeGroup scratch = scratchOf(ag);
			setCategoryQt(scratch, 81.0, 100);
			removeFromScratch(scratch, 73.0);

			AgeGroupDefinitionService.afterStepHookForTesting = s -> {
				if (s == failingStep) {
					throw new IllegalStateException("boom at " + failingStep);
				}
			};
			try {
				service.save(stamp, AuditOrigin.system("test"), scratch, Decision.APPLY);
				throw new AssertionError("the injected failure must propagate");
			} catch (IllegalStateException expected) {
				assertEquals("boom at " + failingStep, expected.getMessage());
			} finally {
				AgeGroupDefinitionService.afterStepHookForTesting = null;
			}

			assertEquals("step " + step + ": fields rolled back", Integer.valueOf(0), storedQt(ag, 81.0));
			assertEquals("step " + step + ": category ids rolled back", cats, categoryIdsByMax(ag));
			assertTrue("step " + step + ": participations rolled back",
			        flagsByCategoryId(t1).containsKey(cats.get(73.0))
			                && flagsByCategoryId(t2).containsKey(cats.get(81.0)));
			assertFalse("step " + step + ": no championship row created", championshipExists(championship));
			assertEquals("step " + step + ": stamp not advanced", stamp, stampOf(ag));
		}
	}

	/** A post-commit refresh failure reports a committed save; the definitions are written, the stamp advanced. */
	@Test
	public void refreshFailureReportsCommittedSave() {
		newChampionshipRow("RefM", false);
		Long ag = newAgeGroup("RFM35", "RefM", Gender.M, 35, 39, true, 0, 73, 999);
		AgeGroup scratch = scratchOf(ag);
		setCategoryQt(scratch, 73.0, 50);
		Long stamp = stampOf(ag);

		AgeGroupDefinitionService.beforeRefreshStepHookForTesting = step -> {
			if ("participantCounts".equals(step)) {
				throw new IllegalStateException("counts boom");
			}
		};
		SaveOutcome outcome = service.save(stamp, AuditOrigin.system("test"), scratch, Decision.APPLY);

		assertEquals(1, outcome.refreshFailures().size());
		assertEquals("counts boom", outcome.refreshFailures().get(0).getMessage());
		assertEquals("the save committed", Integer.valueOf(50), storedQt(ag, 73.0));
		assertNotEquals("the stamp advanced once", stamp, stampOf(ag));
	}

	// ---------------------------------------------------------------------------------------------
	// during the competition (spec 3.7, decision 9)
	// ---------------------------------------------------------------------------------------------

	/** Children lifting in the morning: the Masters fix proceeds; touching the children's championship is refused. */
	@Test
	public void sessionConflictRefusesAffectedChampionshipOnly() {
		Map<String, FieldOfPlay> previous = TestFopRegistry.replace(new HashMap<>());
		try {
			Long childAg = newAgeGroup("SEYTH", "SessChild", Gender.M, 13, 17, true, 0, 999);
			Long childCat = categoryIdsByMax(childAg).get(999.0);
			Long mastersAg = newAgeGroup("SEMAS", "SessMasters", Gender.M, 35, 39, true, 0, 999);
			Long mastersCat = categoryIdsByMax(mastersAg).get(999.0);

			Long[] ids = JpaTransactions.inTransaction(em -> {
				// MockFieldOfPlay.create expects the test group "A" as its initial session
				em.persist(new Group("A"));
				Group morning = new Group("SessMorning");
				em.persist(morning);
				Athlete child = new Athlete();
				child.setLastName("SessChild");
				child.setFirstName("T");
				child.setGender(Gender.M);
				child.setYearOfBirth(LocalDate.now().getYear() - 15);
				child.setBodyWeight(60.0);
				child.setQualifyingTotal(50);
				child.setSnatch1Declaration("60");
				child.setCleanJerk1Declaration("80");
				child.setGroup(morning);
				em.persist(child);
				Athlete master = new Athlete();
				master.setLastName("SessMaster");
				master.setFirstName("T");
				master.setGender(Gender.M);
				master.setYearOfBirth(LocalDate.now().getYear() - 37);
				master.setBodyWeight(80.0);
				master.setQualifyingTotal(100);
				em.persist(master);
				em.flush();
				return new Long[] { child.getId(), master.getId(), morning.getId() };
			});
			participate(ids[0], childCat, true, false);
			participate(ids[1], mastersCat, true, false);
			setRegistration(ids[0], childCat);
			setRegistration(ids[1], mastersCat);

			Group morning = JpaTransactions.inTransaction(em -> em.find(Group.class, ids[2]));
			FieldOfPlay fop = MockFieldOfPlay.create(AthleteRepository.findAll(), new MockCountdownTimer(),
			        new MockCountdownTimer());
			OwlcmsFactory.getFopByName().put("test", fop);
			fop.fopEventPost(new FOPEvent.SwitchGroup(morning, this));
			fop.fopEventPost(new FOPEvent.StartLifting(this));
			fop.fopEventPost(new FOPEvent.TimeStarted(this));
			assertTrue(fop.isSessionInProgress());

			// the Masters fix for the afternoon proceeds while the children are lifting
			AgeGroup mastersScratch = scratchOf(mastersAg);
			setCategoryQt(mastersScratch, 999.0, 50);
			service.save(stampOf(mastersAg), AuditOrigin.system("test"), mastersScratch, Decision.APPLY);
			assertEquals(Integer.valueOf(50), storedQt(mastersAg, 999.0));

			// the children's championship is refused, and nothing is written
			AgeGroup childScratch = scratchOf(childAg);
			setCategoryQt(childScratch, 999.0, 50);
			SessionsInProgressException refused = assertThrows(SessionsInProgressException.class,
			        () -> service.save(stampOf(childAg), AuditOrigin.system("test"), childScratch,
			                Decision.APPLY));
			assertTrue(refused.getConflicts().toString().contains("SessMorning"));
			assertEquals(Integer.valueOf(0), storedQt(childAg, 999.0));

			// the global actions are refused too
			assertThrows(SessionsInProgressException.class,
			        () -> service.reassignWithin(AuditOrigin.system("test")));
			assertThrows(SessionsInProgressException.class,
			        () -> service.recomputeAllEligibilities(AuditOrigin.system("test")));

			// unselecting the session lifts the refusal
			fop.fopEventPost(new FOPEvent.SwitchGroup(null, this));
			assertFalse(fop.isSessionInProgress());
			AgeGroup childScratch2 = scratchOf(childAg);
			setCategoryQt(childScratch2, 999.0, 60);
			service.save(stampOf(childAg), AuditOrigin.system("test"), childScratch2, Decision.APPLY);
			assertEquals(Integer.valueOf(60), storedQt(childAg, 999.0));
		} finally {
			TestFopRegistry.replace(previous);
		}
	}

	// ---------------------------------------------------------------------------------------------
	// stale-save per age group (spec 3.8), NONE/DIRECT behavior (decision 7)
	// ---------------------------------------------------------------------------------------------

	/** Two tabs edit copies of one age group; the second save is refused and writes nothing. */
	@Test
	public void staleDraftRefused() {
		newChampionshipRow("StaleM", false);
		Long ag = newAgeGroup("STM35", "StaleM", Gender.M, 35, 39, true, 0, 73, 999);
		AgeGroup first = scratchOf(ag);
		AgeGroup second = scratchOf(ag);

		setCategoryQt(first, 73.0, 100);
		service.save(first.getLastSavedAt(), AuditOrigin.system("test"), first, Decision.APPLY);

		setCategoryQt(second, 73.0, 200);
		StaleSingletonException stale = assertThrows(StaleSingletonException.class,
		        () -> service.save(second.getLastSavedAt(), AuditOrigin.system("test"), second, Decision.APPLY));
		assertEquals(StaleSingletonException.Kind.AGE_GROUP, stale.getKind());
		assertEquals(Integer.valueOf(100), storedQt(ag, 73.0));
	}

	/** An accepted save with no changes advances the stamp and changes nothing else. */
	@Test
	public void noneSaveAdvancesStampWithoutChanges() {
		newChampionshipRow("NoneM", false);
		Long ag = newAgeGroup("NOM35", "NoneM", Gender.M, 35, 39, true, 0, 73, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long n1 = newAthlete("NoneOne", Gender.M, 36, 70.0, 100);
		participate(n1, cats.get(73.0), true, false);
		Long stamp = stampOf(ag);

		AgeGroup scratch = scratchOf(ag);
		SaveOutcome outcome = service.save(stamp, AuditOrigin.system("test"), scratch, Decision.APPLY);

		assertEquals(AgeGroupChange.Kind.NONE, outcome.change());
		assertNotEquals(stamp, stampOf(ag));
		assertEquals(cats, categoryIdsByMax(ag));
		assertTrue(flagsByCategoryId(n1).containsKey(cats.get(73.0)));
	}

	/** A rename is DIRECT: codes recomputed, and no reassignment even for a non-fitting athlete. */
	@Test
	public void directRenameNeverReassigns() {
		newChampionshipRow("RenameM", false);
		Long ag = newAgeGroup("RNM35", "RenameM", Gender.M, 35, 39, true, 500, 73, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long r1 = newAthlete("RenameOne", Gender.M, 36, 70.0, 50);
		// does not fit the QT: a reassignment would remove him; a DIRECT save must not
		participate(r1, cats.get(73.0), true, false);
		setRegistration(r1, cats.get(73.0));

		AgeGroup scratch = scratchOf(ag);
		scratch.setCode("RNM35X");
		SaveOutcome outcome = service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.APPLY);

		assertEquals(AgeGroupChange.Kind.DIRECT, outcome.change());
		assertTrue("category codes recomputed", storedCode(cats.get(73.0)).contains("RNM35X"));
		assertTrue("no reassignment on a direct change", flagsByCategoryId(r1).containsKey(cats.get(73.0)));
		assertEquals(cats.get(73.0), registrationId(r1));
	}

	// ---------------------------------------------------------------------------------------------
	// championship change (spec 3.4)
	// ---------------------------------------------------------------------------------------------

	/** A clean championship change moves the age group and its participants as they are, no reassignment. */
	@Test
	public void championshipOnlyMovePreservesParticipations() {
		newChampionshipRow("OldChampM", false);
		Long ag = newAgeGroup("CM35", "OldChampM", Gender.M, 35, 39, true, 0, 73, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long m1 = newAthlete("MoveChampOne", Gender.M, 36, 70.0, 50);
		participate(m1, cats.get(73.0), true, true);
		setRegistration(m1, cats.get(73.0));

		AgeGroup scratch = scratchOf(ag);
		scratch.setChampionshipName("NewChampM");
		SaveOutcome outcome = service.save(stampOf(ag), AuditOrigin.system("test"), scratch, Decision.APPLY);

		assertEquals(AgeGroupChange.Kind.CHAMPIONSHIP_ONLY, outcome.change());
		assertTrue(outcome.championshipCreated());
		assertTrue(championshipExists("NewChampM"));
		assertEquals("NewChampM", storedChampionshipName(ag));
		boolean[] flags = flagsByCategoryId(m1).get(cats.get(73.0));
		assertNotNull("participations move as they are", flags);
		assertTrue(flags[0] && flags[1]);
		assertEquals(cats.get(73.0), registrationId(m1));
	}

	/** A championship change combined with any other pending change is refused and writes nothing. */
	@Test
	public void championshipMixedRefused() {
		newChampionshipRow("MixOldM", false);
		Long ag = newAgeGroup("MXM35", "MixOldM", Gender.M, 35, 39, true, 0, 73, 999);
		Long stamp = stampOf(ag);

		AgeGroup scratch = scratchOf(ag);
		scratch.setChampionshipName("MixNewM");
		setCategoryQt(scratch, 73.0, 100);
		assertThrows(IllegalArgumentException.class,
		        () -> service.save(stamp, AuditOrigin.system("test"), scratch, Decision.APPLY));

		assertEquals("MixOldM", storedChampionshipName(ag));
		assertEquals(Integer.valueOf(0), storedQt(ag, 73.0));
		assertFalse(championshipExists("MixNewM"));
		assertEquals(stamp, stampOf(ag));
	}

	// ---------------------------------------------------------------------------------------------
	// create, delete, championship-page move
	// ---------------------------------------------------------------------------------------------

	/** ADD creates the age group, its categories with recomputed bounds, and the championship row; duplicate refused. */
	@Test
	public void createAddsAllRowsAtomically() {
		AgeGroup scratch = handBuilt("CRE35", "CreateChamp", Gender.M, 35, 39, true, true, 0, 73, 999);
		setCategoryQt(scratch, 73.0, 5);

		SaveOutcome outcome = service.create(scratch, AuditOrigin.system("test"));

		assertTrue(outcome.refreshFailures().toString(), outcome.refreshFailures().isEmpty());
		Long created = findIdByCodeGender("CRE35", Gender.M);
		assertNotNull(created);
		assertEquals(0.0, minimumWeightOf(created, 73.0), 0.001);
		assertEquals("lower limit follows the upper limit below", 73.0, minimumWeightOf(created, 999.0), 0.001);
		assertEquals(Integer.valueOf(5), storedQt(created, 73.0));
		assertTrue(championshipExists("CreateChamp"));
		assertNotNull("created rows are stamped", stampOf(created));

		assertThrows("duplicate code+gender", IllegalArgumentException.class,
		        () -> service.create(scratch, AuditOrigin.system("test")));
	}

	/** Deleting an age group reassigns its athletes by eligibility; the registration category is repaired or cleared. */
	@Test
	public void deleteRemovesAndRepairsRegistration() {
		newChampionshipRow("DelChampM", false);
		Long young = newAgeGroup("DA35", "DelChampM", Gender.M, 35, 39, true, 0, 73, 999);
		Long older = newAgeGroup("DA38", "DelChampM", Gender.M, 38, 44, true, 0, 999);
		Map<Double, Long> youngCats = categoryIdsByMax(young);
		Map<Double, Long> olderCats = categoryIdsByMax(older);
		Long x1 = newAthlete("DelX", Gender.M, 38, 70.0, 100);
		participate(x1, youngCats.get(73.0), true, false);
		participate(x1, olderCats.get(999.0), true, false);
		setRegistration(x1, youngCats.get(73.0));
		Long y1 = newAthlete("DelY", Gender.M, 36, 70.0, 100);
		participate(y1, youngCats.get(73.0), true, false);
		setRegistration(y1, youngCats.get(73.0));

		service.delete(stampOf(young), AuditOrigin.system("test"), young);

		assertFalse(ageGroupExists(young));
		assertTrue(ageGroupExists(older));
		Map<Long, boolean[]> xFlags = flagsByCategoryId(x1);
		assertFalse(xFlags.containsKey(youngCats.get(73.0)));
		assertTrue(xFlags.containsKey(olderCats.get(999.0)));
		assertEquals("the remaining participation becomes the registration category",
		        olderCats.get(999.0), registrationId(x1));
		assertTrue(flagsByCategoryId(y1).isEmpty());
		assertNull("nothing left: registration cleared", registrationId(y1));
	}

	/** The delete dry run lists who would lose the championship and writes nothing. */
	@Test
	public void previewDeleteListsAthletesWhoLeaveAndWritesNothing() {
		newChampionshipRow("DelPrevChampM", false);
		Long young = newAgeGroup("DP35", "DelPrevChampM", Gender.M, 35, 39, true, 0, 73, 999);
		newAgeGroup("DP38", "DelPrevChampM", Gender.M, 38, 44, true, 0, 999);
		Map<Double, Long> youngCats = categoryIdsByMax(young);
		Long y1 = newAthlete("PrevY", Gender.M, 36, 70.0, 100);
		participate(y1, youngCats.get(73.0), true, false);
		setRegistration(y1, youngCats.get(73.0));

		var preview = service.previewDelete(young, stampOf(young));

		assertEquals(1, preview.entries().size());
		assertTrue(ageGroupExists(young));
		assertEquals(youngCats.get(73.0), registrationId(y1));
		assertFalse(flagsByCategoryId(y1).isEmpty());
	}

	/** The championship-page move checks and stamps every row; a stale row refuses the whole move. */
	@Test
	public void moveToChampionshipStampsAllAndRefusesStale() {
		Long mvA = newAgeGroup("MVA", "MoveSrc", Gender.M, 35, 39, true, 0, 999);
		Long mvB = newAgeGroup("MVB", "MoveSrc", Gender.M, 40, 44, true, 0, 999);
		Map<Long, Long> stamps = Map.of(mvA, stampOf(mvA), mvB, stampOf(mvB));

		service.moveToChampionship(AuditOrigin.system("test"), stamps, "MoveDst");

		assertEquals("MoveDst", storedChampionshipName(mvA));
		assertEquals("MoveDst", storedChampionshipName(mvB));
		assertNotEquals(stamps.get(mvA), stampOf(mvA));
		assertNotEquals(stamps.get(mvB), stampOf(mvB));
		assertTrue(championshipExists("MoveDst"));

		// a stale draft of either row refuses the whole move and nothing moves
		Map<Long, Long> mixedStamps = Map.of(mvA, stampOf(mvA), mvB, stamps.get(mvB));
		assertThrows(StaleSingletonException.class,
		        () -> service.moveToChampionship(AuditOrigin.system("test"), mixedStamps, "MoveDst2"));
		assertEquals("MoveDst", storedChampionshipName(mvA));
		assertEquals("MoveDst", storedChampionshipName(mvB));
	}

	/** A row written before the stamp existed (null) saves once; the next save with the old draft is refused. */
	@Test
	public void legacyNullStampSavesOnce() {
		newChampionshipRow("LegacyM", false);
		Long ag = newAgeGroup("LGM35", "LegacyM", Gender.M, 35, 39, true, 0, 73, 999);
		JpaTransactions.inTransaction(em -> {
			em.createQuery("update AgeGroup ag set ag.lastSavedAt = null, ag.lastSavedBy = null where ag.id = :id")
			        .setParameter("id", ag).executeUpdate();
			return null;
		});

		AgeGroup scratch = scratchOf(ag);
		assertNull(scratch.getLastSavedAt());
		setCategoryQt(scratch, 73.0, 75);
		service.save(null, AuditOrigin.system("test"), scratch, Decision.APPLY);
		assertEquals(Integer.valueOf(75), storedQt(ag, 73.0));
		assertNotNull(stampOf(ag));

		AgeGroup staleScratch = scratchOf(ag);
		setCategoryQt(staleScratch, 73.0, 90);
		assertThrows(StaleSingletonException.class,
		        () -> service.save(null, AuditOrigin.system("test"), staleScratch, Decision.APPLY));
		assertEquals(Integer.valueOf(75), storedQt(ag, 73.0));
	}

	private static Long findIdByCodeGender(String code, Gender gender) {
		return JpaTransactions.inTransaction(em -> em
		        .createQuery("select ag.id from AgeGroup ag where ag.code = :code and ag.gender = :gender", Long.class)
		        .setParameter("code", code).setParameter("gender", gender)
		        .getResultList().stream().findFirst().orElse(null));
	}

	private void newChampionshipRow(String name, boolean explicitMixed) {
		JpaTransactions.inTransaction(em -> {
			Championship championship = new Championship();
			championship.setName(name);
			championship.setExplicitMixedTeamMembers(explicitMixed);
			em.persist(championship);
			em.flush();
			return null;
		});
	}
}
