/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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

    @Test
    public void testInside() {
        Athlete a = new Athlete();
        a.setBodyWeight(57.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M61", cat.getComputedCode());
    }

    @Test
    public void testOutside() {
        Athlete a = new Athlete();
        a.setBodyWeight(50.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M55", cat.getComputedCode());
    }

    @Test
    public void testYoung() {
        Athlete a = new Athlete();
        a.setBodyWeight(48.2D);
        a.setGender(Gender.M);
        a.setYearOfBirth(LocalDate.now().getYear() - 17);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M49", cat.getComputedCode());
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
    public void testTooYoung() {
        Athlete a = new Athlete();
        a.setBodyWeight(null);
        a.setGender(Gender.M);
        a.setYearOfBirth(LocalDate.now().getYear() - 10);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals(null, cat);
    }
    
    @Before
    public void setupTest() {
        OwlcmsSession.withFop(fop -> fop.testBefore());
    }
}
