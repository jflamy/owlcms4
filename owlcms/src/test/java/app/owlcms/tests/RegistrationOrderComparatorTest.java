/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
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

    @After
    public void resetFeatureSwitches() {
        Config.getCurrent().setFeatureSwitchValue(FeatureSwitch.BW_CLASS_THEN_AGE_GROUP, false);
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

    @Test
    public void lotNumberOrdersAthletesWithinBodyweightWhenAgeGroupToggleIsOff() {
        Config.getCurrent().setFeatureSwitchValue(FeatureSwitch.BW_CLASS_THEN_AGE_GROUP, false);
        List<Athlete> athletes = athletesAcrossAgeGroupsAndBodyweights();

        AthleteSorter.registrationOrder(athletes);

        assertEquals(List.of("senior70", "junior70", "senior85", "junior85"), athleteNames(athletes));
    }

    @Test
    public void ageGroupOrdersAthletesWithinBodyweightBeforeAndAfterWeighIn() {
        Config.getCurrent().setFeatureSwitchValue(FeatureSwitch.BW_CLASS_THEN_AGE_GROUP, true);
        List<Athlete> athletes = athletesAcrossAgeGroupsAndBodyweights();
        athletes.stream()
                .filter(a -> !a.getLastName().equals("junior70"))
                .forEach(a -> a.setBodyWeight(a.getCategory().getMaximumWeight() - 0.1));

        AthleteSorter.registrationOrder(athletes);
        List<Athlete> weighedAthletes = athletes.stream()
                .filter(a -> a.getBodyWeight() != null)
                .collect(Collectors.toCollection(ArrayList::new));
        AthleteSorter.registrationOrder(weighedAthletes);
        AthleteSorter.doAssignStartNumbers(weighedAthletes);

        assertEquals(List.of("junior70", "senior70", "junior85", "senior85"), athleteNames(athletes));
        assertEquals(List.of("senior70", "junior85", "senior85"), athleteNames(weighedAthletes));
        assertEquals(List.of(1, 2, 3), weighedAthletes.stream().map(Athlete::getStartNumber).collect(Collectors.toList()));
    }

    private List<Athlete> athletesAcrossAgeGroupsAndBodyweights() {
        return new ArrayList<>(List.of(
                athlete("junior85", "JR_M85", 40),
                athlete("senior70", "SR_M70", 10),
                athlete("senior85", "SR_M85", 5),
                athlete("junior70", "JR_M70", 30)));
    }

    private Athlete athlete(String name, String categoryCode, int lotNumber) {
        Athlete athlete = new Athlete();
        athlete.setLastName(name);
        athlete.setGender(Gender.M);
        athlete.setLotNumber(lotNumber);
        athlete.computeCategory(CategoryRepository.findByCode(categoryCode));
        return athlete;
    }

    private List<String> athleteNames(Collection<Athlete> athletes) {
        return athletes.stream().map(Athlete::getLastName).collect(Collectors.toList());
    }

    private List<String> categoryCodes(Collection<Category> categories) {
        return categories.stream().map(Category::getCode).collect(Collectors.toList());
    }

}
