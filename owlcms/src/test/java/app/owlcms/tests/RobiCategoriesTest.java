/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.RobiCategories;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.init.OwlcmsSession;

public class RobiCategoriesTest {

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        TestData.insertInitialData(5, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    @Before
    public void setupTest() {
        OwlcmsSession.withFop(fop -> fop.testBefore());
    }

    @Test
    public void testInside() {
        Athlete a = new Athlete();
        a.setBodyWeight(57.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M60", cat.getComputedCode());
    }

    @Test
    public void testNotWeighed() {
        Athlete a = new Athlete();
        a.setBodyWeight(null);
        a.setGender(Gender.M);
        a.setYearOfBirth(LocalDate.now().getYear() - 17);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals(null, cat);
    }

    @Test
    public void testOutside() {
        Athlete a = new Athlete();
        a.setBodyWeight(50.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M60", cat.getComputedCode());
    }

    @Test
    public void testTooYoung() {
        Athlete a = new Athlete();
        a.setBodyWeight(null);
        a.setGender(Gender.M);
        a.setYearOfBirth(LocalDate.now().getYear() - 10);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals(null, cat);
    }

    @Test
    public void testYoung() {
        Athlete a = new Athlete();
        a.setBodyWeight(48.2D);
        a.setGender(Gender.M);
        a.setYearOfBirth(LocalDate.now().getYear() - 17);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M56", cat.getComputedCode());
    }

    @Test
    public void testFindCorrectRobiCategoryForLowerYouthAthletes() {
        Athlete youthFemaleAthlete = new Athlete();
        youthFemaleAthlete.setBodyWeight(40.0D);
        youthFemaleAthlete.setYearOfBirth(LocalDate.now().getYear() - 12);
        youthFemaleAthlete.setGender(Gender.F);
        assertEquals("F44", RobiCategories.findRobiCategory(youthFemaleAthlete).getComputedCode());

        Athlete youthMaleAthlete = new Athlete();
        youthMaleAthlete.setBodyWeight(40.0D);
        youthMaleAthlete.setYearOfBirth(LocalDate.now().getYear() - 12);
        youthMaleAthlete.setGender(Gender.M);
        assertEquals("M56", RobiCategories.findRobiCategory(youthMaleAthlete).getComputedCode());
    }

}
