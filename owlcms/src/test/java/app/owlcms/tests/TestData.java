/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.data.category.AgeDivision.MASTERS;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class TestData.
 */
public class TestData {

    private static Logger logger = (Logger) LoggerFactory.getLogger(TestData.class);
    static {
        logger.setLevel(Level.INFO);
    }

    static int lotNumber = 1;
    private static Clock clock = Clock.fixed(Instant.parse("2021-10-01T08:30:00.00Z"), ZoneId.systemDefault());

    public static void deleteAllLifters(EntityManager em) {
        List<Athlete> athletes = AthleteRepository.doFindAll(em);
        for (Athlete a : athletes) {
            em.remove(a);
        }
    }

    /**
     * Insert initial data if the database is empty.
     *
     * @param nbAthletes how many athletes
     * @param testMode   true if creating dummy data
     */
    public static void insertInitialData(int nbAthletes, boolean testMode) {
        JPAService.runInTransaction(em -> {
            EnumSet<AgeDivision> divisions = EnumSet.of(AgeDivision.IWF);
            Competition competition = createDefaultCompetition(divisions);
            CompetitionRepository.save(competition);
            AgeGroupRepository.insertAgeGroups(em, divisions, "/agegroups/AgeGroups_Tests.xlsx");
            return null;
        });
        JPAService.runInTransaction(em -> {
            setupTestData(em, nbAthletes);
            return null;
        });
        AthleteRepository.resetParticipations();

    }

    public static void insertSampleLifters(EntityManager em, int liftersToLoad, Group groupA,
            Group groupB,
            Group groupC) {
        final String[] fnames = { "Peter", "Albert", "Joshua", "Mike", "Oliver",
                "Paul", "Alex", "Richard", "Dan", "Umberto", "Henrik", "Rene",
                "Fred", "Donald" };
        final String[] lnames = { "Smith", "Gordon", "Simpson", "Brown",
                "Clavel", "Simons", "Verne", "Scott", "Allison", "Gates",
                "Rowling", "Barks", "Ross", "Schneider", "Tate" };

        Random r = new Random(0);

        lotNumber = 1;
        createGroup(em, groupA, fnames, lnames, r, 81, 73, liftersToLoad);
        createGroup(em, groupB, fnames, lnames, r, 73, 67, liftersToLoad);

    }

    protected static void assignStartNumbers(EntityManager em, Group groupA) {
        List<Athlete> athletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, groupA, true, (Gender) null);
        AthleteSorter.registrationOrder(athletes);
        AthleteSorter.assignStartNumbers(athletes);
    }

    protected static void createAthlete(EntityManager em, Random r, Athlete p, double nextDouble, int catLimit) {
        p.setBodyWeight(81 - nextDouble);
        p.setGender(Gender.M);
//        Category categ = CategoryRepository.findByGenderAgeBW(Gender.M, 40, p.getBodyWeight()).get(0);
//        p.addEligibleCategory(em.contains(categ) ? categ : em.merge(categ));
//        p.setCategory(categ);
        logger.debug("athlete {} category {} participations {}", p, p.getCategory(), p.getParticipations());
    }

    protected static Competition createDefaultCompetition(EnumSet<AgeDivision> ageDivisions) {
        Competition competition = new Competition();

        competition.setCompetitionName("Spring Equinox Open");
        competition.setCompetitionCity("Sometown, Lower FOPState");
        competition.setCompetitionDate(LocalDate.of(2019, 03, 23));
        competition.setCompetitionOrganizer("Giant Weightlifting Club");
        competition.setCompetitionSite("West-End Gym");
        competition.setFederation("National Weightlifting Federation");
        competition.setFederationAddress("22 River Street, Othertown, Upper FOPState,  J0H 1J8");
        competition.setFederationEMail("results@national-weightlifting.org");
        competition.setFederationWebSite("http://national-weightlifting.org");

        competition.setEnforce20kgRule(true);
        competition.setMasters(ageDivisions != null && ageDivisions.contains(MASTERS));
        competition.setUseBirthYear(true);
        competition.setAnnouncerLiveDecisions(true);

        return competition;
    }

    protected static void createGroup(EntityManager em, Group group, final String[] fnames, final String[] lnames,
            Random r,
            int cat1, int cat2, int liftersToLoad) {
        for (int i = 0; i < liftersToLoad; i++) {
            Athlete p = new Athlete();
            Group mg = (em.contains(group) ? group : em.merge(group));
            p.setGroup(mg);
            p.setFirstName(fnames[r.nextInt(fnames.length)]);
            p.setLastName(lnames[r.nextInt(lnames.length)]);
            p.setFullBirthDate(LocalDate.of(testDateNow().getYear() - 40, 1, 1));
            p.setLotNumber(lotNumber);
            lotNumber++;
            createAthlete(em, r, p, 0.0D, cat1);
            em.persist(p);
        }
    }

    protected static void drawLots(EntityManager em) {
        List<Athlete> athletes = AthleteRepository.doFindAll(em);
        AthleteSorter.drawLots(athletes);
    }

    /**
     * Setup test data.
     *
     * @param em
     *
     * @param competition   the competition
     * @param liftersToLoad the lifters to load
     * @param w             the w
     * @param c             the c
     */
    protected static void setupTestData(EntityManager em, int liftersToLoad) {
        logger.info("inserting test data.");
        // needed because some classes such as Athlete refer to the current competition
        Competition.setCurrent(new Competition());

        LocalDateTime w = testDateTimeNow();
        LocalDateTime c = w.plusHours((long) 2.0);

        Platform platform1 = new Platform("Gym 1");
        Platform platform2 = new Platform("Gym 2");

        Group groupA = new Group("A", w, c);
        groupA.setPlatform(platform1);

        Group groupB = new Group("B", w, c);
        groupB.setPlatform(platform2);

        w = w.plusHours((long) 2.0);
        c = w.plusHours((long) 2.0);
        Group groupC = new Group("C", w, c);
        groupC.setPlatform(platform1);

        insertSampleLifters(em, liftersToLoad, groupA, groupB, groupC);

//        em.persist(groupA);
//        em.persist(groupB);
//        em.persist(groupC);
    }

    private static LocalDateTime testDateTimeNow() {
        return LocalDateTime.now(clock);
    }

    private static LocalDate testDateNow() {
        return LocalDate.now(clock);
    }

}
