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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;
import app.owlcms.spreadsheet.RCompetition;

public class NRegistrationFileProcessorCategoryAssignmentTest {

	private static final String REGISTRATION_FILE = "/testData/missingCellsRegistration.xlsx";

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		ProdData.insertInitialData(0);
		System.setProperty("enableEmbeddedMqtt", "false");
		app.owlcms.init.OwlcmsFactory.initDefaultFOP();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Before
	public void resetDatabase() {
		JPAService.close();
		JPAService.init(true, true);
		Config.initConfig();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		ProdData.insertInitialData(0);
		app.owlcms.init.OwlcmsFactory.initDefaultFOP();
	}

	@Test
	public void testRegistrationReaderErrorAndSetterMatrixWithFixture() throws Exception {
		configureMaleAgeGroups("Open", "U13", "U15", "M45");
		configureSessionGroups("2", "3", "4");

		assertTrue("Open male age group should be active for automatic assignment", isAgeGroupActive("Open", Gender.M));
		assertTrue("U13 male age group should be active for explicit assignment", isAgeGroupActive("U13", Gender.M));
		assertTrue("U15 male age group should be active for explicit assignment", isAgeGroupActive("U15", Gender.M));
		assertTrue("M45 male age group should be active for explicit assignment", isAgeGroupActive("M45", Gender.M));
		assertFalse("M40 male age group should remain inactive in this controlled basis", isAgeGroupActive("M40", Gender.M));
		assertActiveCategoryAvailable("U13 M 42");
		assertActiveCategoryAvailable("M45 94");
		assertActiveCategoryAvailable("M 94");
		assertActiveCategoryUnavailable("U15 M 79+");

		try (InputStream xlsInputStream = this.getClass().getResourceAsStream(REGISTRATION_FILE)) {
			assertNotNull("Test file not found: " + REGISTRATION_FILE, xlsInputStream);

			byte[] fileBytes = xlsInputStream.readAllBytes();
			ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);

			NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, java.util.Locale.ENGLISH);
			processor.setSessionOptions(NRegistrationFileProcessor.SessionOptions.IGNORE_SESSIONS);

			StringBuilder importMessages = new StringBuilder();
			int athletesProcessed = processor.doProcessAthletes(
				bais,
				false,
				msg -> importMessages.append(msg).append('\n'),
				() -> {
				});

			assertTrue("The registration fixture should import the targeted athletes", athletesProcessed >= 7);

			Athlete caplan = findAthlete("Caplan", "Joey");
			assertNotNull("Joey Caplan should be imported", caplan);
			assertNotNull("Joey Caplan should receive a category", caplan.getCategory());
			assertEquals("Joey Caplan should keep his explicit U13 category", Category.codeFromName("U13 M 42"), caplan.getCategory().getCode());
			assertEquals("Joey Caplan should resolve to the U13 age group", "U13", caplan.getAgeGroup().getCode());

			Athlete hall = findAthlete("Hall", "Kevin");
			assertNotNull("Kevin Hall should be imported", hall);
			assertNull("Kevin Hall should remain uncategorized when his explicit category does not match the active basis", hall.getCategory());

			Athlete bromley = findAthlete("Bromley", "Chip");
			assertNotNull("Chip Bromley should be imported", bromley);
			assertNotNull("Chip Bromley should keep his explicit M45 category", bromley.getCategory());
			assertEquals("Chip Bromley should keep his explicit M45 category", Category.codeFromName("M45 94"), bromley.getCategory().getCode());
			assertEquals("Chip Bromley should resolve to the M45 age group", "M45", bromley.getAgeGroup().getCode());

			Athlete schreiber = findAthlete("Schreiber", "Erik");
			assertNotNull("Erik Schreiber should be imported", schreiber);
			assertNotNull("Erik Schreiber should receive an automatically inferred category", schreiber.getCategory());
			assertEquals("Erik Schreiber should be inferred into the active Open category", Category.codeFromName("M 94"), schreiber.getCategory().getCode());
			assertEquals("Erik Schreiber should resolve to the Open age group", "Open", schreiber.getAgeGroup().getCode());

			Athlete buser = findAthlete("Buser", "Nicole");
			assertNotNull("Nicole Buser should still be imported", buser);
			assertNull("Nicole Buser should remain uncategorized without a birth date", buser.getCategory());
			assertEquals("Nicole Buser gender should still be imported", Gender.F, buser.getGender());

			Athlete noGender = findAthlete("NoGender", "Case");
			assertNotNull("The missing-gender row should still be imported", noGender);
			assertNull("The missing-gender row should not have a gender", noGender.getGender());
			assertNull("The missing-gender row should not receive a category", noGender.getCategory());

			Athlete noBodyWeight = findAthlete("NoBodyWeight", "Case");
			assertNotNull("The missing-body-weight row should still be imported", noBodyWeight);
			assertEquals("The missing-body-weight row should keep its gender", Gender.M, noBodyWeight.getGender());
			assertNull("The missing-body-weight row should not receive a category", noBodyWeight.getCategory());

			Athlete numericPass = findAthlete("NumericPass", "Case");
			assertNotNull("The numeric-only success row should be imported", numericPass);
			assertNotNull("The numeric-only success row should receive a category", numericPass.getCategory());
			assertEquals("The numeric-only success row should resolve to the active Open category", Category.codeFromName("M 94"), numericPass.getCategory().getCode());
			assertEquals("The numeric-only success row should resolve to the Open age group", "Open", numericPass.getAgeGroup().getCode());

			String errors = importMessages.toString();
			String blankCategoryMessage = Translator.translate("Upload.CannotDetermineRegistrationCategory");
			String missingGenderMessage = Translator.translate("Upload.MissingGender");
			String missingBirthDateMessage = Translator.translate("Upload.MissingBirthDate");
			String missingBodyWeightMessage = Translator.translate("Upload.MissingBodyWeight");
			assertTrue("The blank category row with complete data should still report the category-cell error", errors.contains("G5 " + blankCategoryMessage));
			assertTrue("The blank category row with missing gender should report the category-cell error", errors.contains("G12 " + blankCategoryMessage));
			assertTrue("The blank category row with missing body weight should report the category-cell error", errors.contains("G13 " + blankCategoryMessage));
			assertTrue("The missing-gender row should report the gender-cell error", errors.contains("F12 " + missingGenderMessage));
			assertTrue("The numeric-only missing-birth-date row should report the birth-date-cell error", errors.contains("E3 " + missingBirthDateMessage));
			assertTrue("The blank-category missing-body-weight row should report the body-weight-cell error", errors.contains("K13 " + missingBodyWeightMessage));
			assertTrue("The invalid explicit U15 category should still be reported", errors.contains("U15 M 79+"));
			assertEquals("Every blank category row should report the category-cell error", 3,
				countOccurrences(errors, blankCategoryMessage));
			assertEquals("The missing-gender rule should fire once", 1, countOccurrences(errors, missingGenderMessage));
			assertEquals("Only the numeric-category row should report a missing birth date", 1, countOccurrences(errors, missingBirthDateMessage));
			assertEquals("Only the blank-category row missing body weight should report missing body weight", 1,
				countOccurrences(errors, missingBodyWeightMessage));
		}
	}

	private void configureMaleAgeGroups(String... activeCodes) {
		Set<String> activeCodeSet = Set.of(activeCodes);
		JPAService.runInTransaction(em -> {
			em.createQuery("select ag from AgeGroup ag where ag.gender = :gender", AgeGroup.class)
				.setParameter("gender", Gender.M)
				.getResultList()
				.forEach(ageGroup -> ageGroup.setActive(activeCodeSet.contains(ageGroup.getCode())));
			return null;
		});
		CategoryRepository.resetCodeMap();
		RCompetition.resetActiveCategories();
	}

	private void configureSessionGroups(String... groupNames) {
		Set<String> requestedGroupNames = Set.of(groupNames);
		JPAService.runInTransaction(em -> {
			Set<String> existingGroupNames = em.createQuery("select g.name from CompetitionGroup g", String.class)
				.getResultList()
				.stream()
				.collect(Collectors.toSet());
			for (String groupName : requestedGroupNames) {
				if (!existingGroupNames.contains(groupName)) {
					em.persist(new Group(groupName));
				}
			}
			return null;
		});
		RCompetition.resetActiveGroups();
	}

	private boolean isAgeGroupActive(String code, Gender gender) {
		return JPAService.runInTransaction(em -> em.createQuery(
			"select ag from AgeGroup ag where ag.code = :code and ag.gender = :gender", AgeGroup.class)
			.setParameter("code", code)
			.setParameter("gender", gender)
			.getResultList()
			.stream()
			.findFirst()
			.map(AgeGroup::isActive)
			.orElse(false));
	}

	private void assertActiveCategoryAvailable(String categoryName) {
		Category databaseCategory = findActiveCategoryInDatabase(categoryName);
		assertNotNull(buildActiveCategoryFailureMessage(categoryName, databaseCategory), databaseCategory);
		String categoryCode = Category.codeFromName(categoryName);
		assertNotNull("Category should be present in the active-category cache: " + categoryName, categoryCode);
		assertTrue("Category should be present in RCompetition active categories: " + categoryName,
			RCompetition.getActiveCategories().containsKey(categoryCode));
		assertEquals("Category code should match between database and cache for: " + categoryName,
			databaseCategory.getCode(), categoryCode);
		assertNotNull("Category should be present in the database by code: " + categoryName,
			CategoryRepository.findByCode(categoryCode));
	}

	private void assertActiveCategoryUnavailable(String categoryName) {
		assertNull("Category should not be present in the active-category database query: " + categoryName,
			findActiveCategoryInDatabase(categoryName));
		assertNull("Category should not be present in the active-category cache: " + categoryName,
			Category.codeFromName(categoryName));
	}

	private Category findActiveCategoryInDatabase(String categoryName) {
		return CategoryRepository.findActive().stream()
			.filter(category -> categoryName.equals(category.getDisplayName())
				|| categoryName.equals(category.getNameWithAgeGroup())
				|| categoryName.equals(Category.canonicalName(category.getDisplayName()))
				|| categoryName.equals(Category.canonicalName(category.getNameWithAgeGroup())))
			.findFirst()
			.orElse(null);
	}

	private String buildActiveCategoryFailureMessage(String categoryName, Category databaseCategory) {
		if (databaseCategory != null) {
			return null;
		}
		String activeAgeGroups = AgeGroupRepository.findFiltered(null, Gender.M, null, null, true, -1, -1).stream()
			.map(ageGroup -> ageGroup.getCode() + "=" + ageGroup.isActive())
			.collect(Collectors.joining(", "));
		String activeCategories = CategoryRepository.findActive().stream()
			.limit(40)
			.map(category -> category.getDisplayName() + " [" + category.getCode() + "]")
			.collect(Collectors.joining(", "));
		return "Category should be present in the active-category database query: " + categoryName
			+ " active male age groups=" + activeAgeGroups
			+ " active categories sample=" + activeCategories;
	}

	private Athlete findAthlete(String lastName, String firstName) {
		var athletes = AthleteRepository.findFiltered(lastName, null, null, null, null, null, null, null, 0, 20)
			.stream()
			.filter(a -> firstName.equals(a.getFirstName()))
			.collect(Collectors.toList());
		assertEquals("Expected one matching athlete for " + firstName + " " + lastName, 1, athletes.size());
		return athletes.get(0);
	}

	private int countOccurrences(String value, String token) {
		if (value == null || token == null || token.isEmpty()) {
			return 0;
		}
		int count = 0;
		int start = 0;
		while ((start = value.indexOf(token, start)) >= 0) {
			count++;
			start += token.length();
		}
		return count;
	}
}