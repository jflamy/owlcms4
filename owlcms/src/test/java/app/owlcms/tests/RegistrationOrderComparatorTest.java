/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

public class RegistrationOrderComparatorTest {

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.close();
        JPAService.init(true, true);
        Config.initConfig();
        JPAService.runInTransaction(em -> {
            Competition.setCurrent(new Competition());
            AgeGroupRepository.insertAgeGroups(em, EnumSet.of(
            		ChampionshipType.IWF,
            		ChampionshipType.MASTERS,
            		ChampionshipType.U),
            			"/agegroups/AgeGroups_2026-08.xlsx");
            return null;
        });
    }

    @AfterClass
    public static void tearDownTests() {
        Competition.setCurrent(null);
        JPAService.close();
    }

    @Test
    public void juniorAndSeniorCategoriesAreEligibleAtAgeTwenty() {
        Collection<Category> cats = CategoryRepository.findByGenderAgeBW(Gender.M, 20, 66.0D);
        assertEquals(List.of("JR_M70", "SR_M70"), categoryCodes(cats));
    }

    @Test
    public void mastersCategoryIsPreferredAtAgeThirtySix() {
        Collection<Category> cats = CategoryRepository.findByGenderAgeBW(Gender.M, 36, 66.0D);
        assertEquals(List.of("M35_M70", "SR_M70"), categoryCodes(cats));
    }

    @Test
    public void youthCategoryIsPreferredAtAgeFifteen() {
        Collection<Category> cats = CategoryRepository.findByGenderAgeBW(Gender.M, 15, 66.0D);
        assertEquals(List.of("JR_M70", "SR_M70", "U15_M70"), categoryCodes(cats));
    }

    private List<String> categoryCodes(Collection<Category> categories) {
        return categories.stream().map(Category::getCode).collect(Collectors.toList());
    }

}
