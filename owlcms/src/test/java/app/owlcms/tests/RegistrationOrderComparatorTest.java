/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.init.OwlcmsSession;

public class RegistrationOrderComparatorTest {

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        JPAService.runInTransaction(em -> {
            Competition.setCurrent(new Competition());
            AgeGroupRepository.insertAgeGroups(em, EnumSet.of(AgeDivision.IWF, AgeDivision.MASTERS, AgeDivision.U));
            return null;
        });
    }
    
    @Before
    public void setupTest() {
        OwlcmsSession.withFop(fop -> fop.testBefore());
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    List<Athlete> athletes = null;

    @Test
    public void checkJr() {
        Collection<Category> cats = CategoryRepository.findByGenderAgeBW(Gender.M, 20, 66.0D);
        assertEquals("[U20 M 67, JR M 67, SR M 67]", cats.toString());
    }

    @Test
    public void checkProudMasters() {
        Collection<Category> cats = CategoryRepository.findByGenderAgeBW(Gender.M, 36, 66.0D);
        assertEquals("[M35 67, O21 M 67, SR M 67]", cats.toString());
    }

    @Test
    public void checkYth() {
        Collection<Category> cats = CategoryRepository.findByGenderAgeBW(Gender.M, 15, 66.0D);
        assertEquals("[U15 M 67, YTH M 67, JR M 67, SR M 67]", cats.toString());
    }

}
