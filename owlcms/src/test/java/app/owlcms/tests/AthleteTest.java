/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.RuleViolationException;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;

public class AthleteTest {

    private static Athlete athlete;
    private static final Level LOGGER_LEVEL = Level.OFF;

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
        TestData.insertInitialData(5, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    @Before
    public void setupTest() {
        FieldOfPlay fopState = MockFieldOfPlay.create(new ArrayList<Athlete>(), new MockCountdownTimer(),
                new MockCountdownTimer());
        OwlcmsSession.setFop(fopState);
        fopState.getLogger().setLevel(LOGGER_LEVEL);
        // EventBus fopBus = fopState.getFopEventBus();

        athlete = new Athlete();
        athlete.setLastName("Strong");
        athlete.setFirstName("Paul");
        athlete.setGender(Gender.M);
        athlete.setBodyWeight(68.5);
        athlete.setSnatch1Declaration("60");
        athlete.setCleanJerk1Declaration("80");
        athlete.setYearOfBirth(1900);
        Category registrationCategory = CategoryRepository.findByCode("Open_M73");
        athlete.setEligibleCategories(new LinkedHashSet<>(Arrays.asList(registrationCategory)));
        athlete.computeCategory(registrationCategory);
    }

    @Test
    public void testCategoryComputation() {
        // Test the category computation logic
        assertEquals("Category", "M 73", athlete.getCategory().toString());
    }

    @Test
    public void testEntryTotalDoesNotFilterEligibleCategories() {
        Category registrationCategory = CategoryRepository.findByCode("Open_M73");
        int originalQualifyingTotal = registrationCategory.getQualifyingTotal();

        try {
            registrationCategory.setQualifyingTotal(999);
            CategoryRepository.save(registrationCategory);

            List<Category> eligibleCategories = CategoryRepository.doFindEligibleCategories(athlete, athlete.getGender(),
                    athlete.getAge(), athlete.getBodyWeight(), 1);

            assertTrue("entry total below category qualifying total should not remove category eligibility",
                    eligibleCategories.stream().anyMatch(c -> c.sameAs(registrationCategory)));
        } finally {
            registrationCategory.setQualifyingTotal(originalQualifyingTotal);
            CategoryRepository.save(registrationCategory);
        }
    }

    @Test
    public void testEntryTotalBelowCategoryQualifyingTotalRequiresConfirmation() {
        Category category = new Category();
        category.setQualifyingTotal(100);

        assertTrue("entry total below category qualifying total should require confirmation",
                category.requiresEntryTotalConfirmation(99));
        assertFalse("entry total equal to category qualifying total should not require confirmation",
                category.requiresEntryTotalConfirmation(100));
        assertFalse("entry total above category qualifying total should not require confirmation",
                category.requiresEntryTotalConfirmation(101));
        assertTrue("blank entry total should require confirmation when category has a qualifying total",
                category.requiresEntryTotalConfirmation(null));
        assertTrue("zero entry total should require confirmation when category has a qualifying total",
                category.requiresEntryTotalConfirmation(0));

        category.setQualifyingTotal(0);
        assertFalse("category without qualifying total should not require confirmation",
                category.requiresEntryTotalConfirmation(0));
    }

    /**
     * Test method for {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalBombOut() {
        athlete.setSnatch1ActualLift("-60");
        athlete.setSnatch2ActualLift("-60");
        athlete.setSnatch3ActualLift("-60");
        athlete.setCleanJerk1ActualLift("-80");
        athlete.setCleanJerk2ActualLift("-80");
        athlete.setCleanJerk3ActualLift("-80");
        assertEquals("total with full bomb out", 0, (long) athlete.getTotal());
    }

    /**
     * Test method for {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalHappyPath() {
        athlete.setSnatch1ActualLift("60");
        athlete.setSnatch2ActualLift("61");
        athlete.setSnatch3ActualLift("62");
        athlete.setCleanJerk1ActualLift("80");
        athlete.setCleanJerk2ActualLift("81");
        athlete.setCleanJerk3ActualLift("82");
        assertEquals("total with all values", 144, (long) athlete.getTotal());
        //assertEquals("robi score", 53.33D, athlete.getRobi(), 0.005);
    }

    /**
     * Test method for {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalNoCleanJerkData() {
        athlete.setSnatch1ActualLift("60");
        athlete.setSnatch2ActualLift("61");
        athlete.setSnatch3ActualLift("62");
        athlete.setCleanJerk1ActualLift(null);
        athlete.setCleanJerk2ActualLift(null);
        athlete.setCleanJerk3ActualLift(null);
        assertEquals("total with no clean and jerk results", 0, (long) athlete.getTotal());
    }

    /**
     * Test method for {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalNoData() {
        assertEquals("total without any results", 0, (long) athlete.getTotal());
    }

    /**
     * Test method for {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalNoSnatchData() {
        athlete.setSnatch1ActualLift(null);
        athlete.setSnatch2ActualLift(null);
        athlete.setSnatch3ActualLift(null);
        athlete.setCleanJerk1ActualLift("80");
        athlete.setCleanJerk2ActualLift("81");
        athlete.setCleanJerk3ActualLift("82");
        assertEquals("total with no snatch results", 0L, (long) athlete.getTotal());
    }

    /**
     * Test method for {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalPartialData() {
        athlete.setSnatch1ActualLift("60");
        athlete.setSnatch2ActualLift("");
        athlete.setSnatch3ActualLift(null);
        athlete.setCleanJerk1ActualLift("-80");
        athlete.setCleanJerk2ActualLift("-");
        athlete.setCleanJerk3ActualLift(null);
        assertEquals("total with failed clean and jerk results", 0, (long) athlete.getTotal());
    }

    /**
     * Test method for {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalSnatchBombOut() {
        athlete.setSnatch1ActualLift("-60");
        athlete.setSnatch2ActualLift("-60");
        athlete.setSnatch3ActualLift("-60");
        athlete.setCleanJerk1ActualLift("80");
        athlete.setCleanJerk2ActualLift("81");
        athlete.setCleanJerk3ActualLift("-");
        assertEquals("total with snatch bomb out", 0, (long) athlete.getTotal());
    }
    
    @Test
    public void testMaleSMF() {
        athlete.setSnatch1ActualLift("60");
        athlete.setSnatch2ActualLift("61");
        athlete.setSnatch3ActualLift("62");
        athlete.setCleanJerk1ActualLift("80");
        athlete.setCleanJerk2ActualLift("81");
        athlete.setCleanJerk3ActualLift("82");
        athlete.setFullBirthDate(LocalDate.now().minusYears(60));
        assertEquals("SMF 144kg for 68.5kg 60 year old male athlete ", 291.093D ,athlete.getSmhf(), 0.0005D);
    }
    
    @Test
    public void testFemaleSMHF() {
        athlete.setSnatch1ActualLift("60");
        athlete.setSnatch2ActualLift("61");
        athlete.setSnatch3ActualLift("62");
        athlete.setCleanJerk1ActualLift("80");
        athlete.setCleanJerk2ActualLift("81");
        athlete.setCleanJerk3ActualLift("82");
        athlete.setFullBirthDate(LocalDate.now().minusYears(60));
        athlete.setGender(Gender.F);
        assertEquals("SMHF 144kg for 68.5kg 60 year old female athlete ", 306.574D ,athlete.getSmhf(), 0.0005D);
    }
    
    @Test
    public void testFemaleSinclair2024() {
        athlete.setSnatch1ActualLift("60");
        athlete.setSnatch2ActualLift("61");
        athlete.setSnatch3ActualLift("62");
        athlete.setCleanJerk1ActualLift("80");
        athlete.setCleanJerk2ActualLift("81");
        athlete.setCleanJerk3ActualLift("82");
        athlete.setFullBirthDate(LocalDate.now().minusYears(60));
        athlete.setGender(Gender.F);
        assertEquals("Sinclair2024 144kg for 68.5kg female athlete ", 180.0536D ,athlete.getSinclair(), 0.0005D);
    }
    
    @Test
    public void testFemaleSinclair2020() {
        athlete.setSnatch1ActualLift("60");
        athlete.setSnatch2ActualLift("61");
        athlete.setSnatch3ActualLift("62");
        athlete.setCleanJerk1ActualLift("80");
        athlete.setCleanJerk2ActualLift("81");
        athlete.setCleanJerk3ActualLift("82");
        athlete.setFullBirthDate(LocalDate.now().minusYears(30));
        athlete.setGender(Gender.F);
        assertEquals("Sinclair2020 144kg for 68.5kg female athlete ", 179.8088D ,athlete.getSmhf(), 0.0005D);
    }

    @Test
    public void testStartingTotalViolationClearsWhenCorrected() throws Exception {
        Competition.getCurrent().setEnforce20kgRule(true);
        athlete.setValidation(true);
        athlete.setQualifyingTotal(185);

        try {
            athlete.validateStartingTotalsRule(60, 80, 185);
            fail("Expected 20kg rule violation for insufficient starting total");
        } catch (RuleViolationException.Rule15_20Violated expected) {
            // expected
        }

        assertTrue("violation flag should be set after a failed validation", getStartingTotalViolation(athlete));

        athlete.validateStartingTotalsRule(60, 125, 185);

        assertFalse("violation flag should clear after the starting total is corrected", getStartingTotalViolation(athlete));
    }

    @Test
    public void testRequiredInitialAttemptsUsesReferencedChampionshipTypeAfterMigration() {
        Competition.getCurrent().setEnforce20kgRule(true);
        Competition.getCurrent().setImwa(true);

        AgeGroup openAgeGroup = new AgeGroup();
        openAgeGroup.setCode("M");
        openAgeGroup.setGender(Gender.M);
        openAgeGroup.setMinAge(15);
        openAgeGroup.setMaxAge(40);
        openAgeGroup.setChampionshipName("Masters");
        openAgeGroup.setChampionshipType(ChampionshipType.U);

        Category openCategory = new Category(60.0, 65.0, Gender.M, true, 0, 0, 0, openAgeGroup, 0);
        athlete.setParticipations(new ArrayList<>());
        athlete.getParticipations().add(new Participation(athlete, openCategory));
        athlete.computeCategory(openCategory);
        athlete.setEntryTotal(250);

        assertEquals("Stored championship data should win over stale age-group championshipType values after migration",
                "200", athlete.getRequiredInitialAttempts());
    }

    private boolean getStartingTotalViolation(Athlete athlete) throws Exception {
        Field field = Athlete.class.getDeclaredField("startingTotalViolation");
        field.setAccessible(true);
        return field.getBoolean(athlete);
    }

}
