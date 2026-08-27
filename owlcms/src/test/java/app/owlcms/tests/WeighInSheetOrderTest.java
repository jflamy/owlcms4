/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.spreadsheet.JXLSWeighInSheet;

/**
 * Verifies the exact athlete list handed to the weigh-in form template.
 *
 * The WeighInForm templates iterate the "athletes" bean in list order (plain jx:forEach, no
 * sorting in Excel), so this is the printed order of the weigh-in form.
 */
public class WeighInSheetOrderTest {

    private static Group sessionA;

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
        JPAService.runInTransaction(em -> {
            Platform platform = new Platform("A");
            em.persist(platform);
            Group group = new Group("A");
            group.setPlatform(platform);
            em.persist(group);
            // lot numbers chosen so that toggle on/off yield different orders within M70
            persistAthlete(em, group, "junior70", "JR_M70", 17, 20);
            persistAthlete(em, group, "senior70", "SR_M70", 25, 10);
            persistAthlete(em, group, "senior85", "SR_M85", 25, 5);
            sessionA = group;
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
    public void weighInFormUsesBodyweightThenLotWhenToggleIsOff() {
        Config.getCurrent().setFeatureSwitchValue(FeatureSwitch.BW_CLASS_THEN_AGE_GROUP, false);

        JXLSWeighInSheet sheet = new JXLSWeighInSheet();
        sheet.setGroup(sessionA);

        assertEquals(List.of("senior70", "junior70", "senior85"), lastNames(sheet.computeSortedAthletes()));
    }

    @Test
    public void weighInFormGroupsByAgeGroupWithinBodyweightWhenToggleIsOn() {
        Config.getCurrent().setFeatureSwitchValue(FeatureSwitch.BW_CLASS_THEN_AGE_GROUP, true);

        JXLSWeighInSheet sheet = new JXLSWeighInSheet();
        sheet.setGroup(sessionA);

        assertEquals(List.of("junior70", "senior70", "senior85"), lastNames(sheet.computeSortedAthletes()));
    }

    private static void persistAthlete(EntityManager em, Group group, String lastName,
            String categoryCode, int age, int lotNumber) {
        Athlete athlete = new Athlete();
        athlete.setGroup(em.contains(group) ? group : em.merge(group));
        athlete.setFirstName("Test");
        athlete.setLastName(lastName);
        athlete.setGender(Gender.M);
        athlete.setFullBirthDate(LocalDate.now().minusYears(age));
        athlete.setLotNumber(lotNumber);
        athlete.computeCategory(CategoryRepository.findByCode(categoryCode));
        em.persist(athlete);
    }

    private List<String> lastNames(Collection<Athlete> athletes) {
        return athletes.stream().map(Athlete::getLastName).collect(Collectors.toList());
    }

}
