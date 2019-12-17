/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.jpa;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * TestData.
 */
public class DemoData {

    private static Logger logger = (Logger) LoggerFactory.getLogger(DemoData.class);

    private static Logger startLogger = (Logger) LoggerFactory.getLogger(Main.class);
    static {
        logger.setLevel(Level.INFO);
    }

    protected static void assignStartNumbers(EntityManager em, Group groupA) {
        List<Athlete> athletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, groupA, true);
        AthleteSorter.registrationOrder(athletes);
        AthleteSorter.assignStartNumbers(athletes);
    }

    protected static void createAthlete(EntityManager em, Random r, Athlete p, double nextDouble, int catMax,
            boolean masters, int minAge, int maxAge, Gender gender) {
        int referenceYear = LocalDate.now().getYear();
        LocalDate baseDate = LocalDate.of(referenceYear, 12, 31);
        
        Double catLimit = (double) catMax;
        double bodyWeight = catLimit - (nextDouble * 2.0);
        p.setBodyWeight(bodyWeight);
        double sd = catLimit * (1 + (r.nextGaussian() / 10));
        long isd = Math.round(sd);
        p.setSnatch1Declaration(Long.toString(isd));
        long icjd = Math.round(sd * 1.20D);
        p.setCleanJerk1Declaration(Long.toString(icjd));
        nextDouble = r.nextDouble();
        String team;
        if (nextDouble < 0.333) {
            team = "EAST";
        } else if (nextDouble < 0.666) {
            team = "WEST";
        } else {
            team = "NORTH";
        }
        p.setTeam(team);
        // compute a random number of weeks inside the age bracket
        long weeksToSubtract = (long) ((minAge * 52) + Math.floor(r.nextDouble() * (maxAge - minAge) * 52));
        LocalDate fullBirthDate = baseDate.minusWeeks(weeksToSubtract);
        p.setFullBirthDate(fullBirthDate);
        int age = LocalDate.now().getYear() - fullBirthDate.getYear();
        
        if (masters) {
            p.setAgeDivision(AgeDivision.MASTERS);
        } else {
            p.setAgeDivision(AgeDivision.DEFAULT);
        }
        
        List<Category> cat = CategoryRepository.findByGenderDivisionAgeBW(gender,p.getAgeDivision(),age,bodyWeight);
        p.setCategory(cat.stream().findFirst().orElse(null));
        
        // respect 20kg rule
        p.setQualifyingTotal((int) (isd + icjd - 15));

    }

    protected static Competition createDefaultCompetition(boolean masters) {
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
        competition.setMasters(masters);
        competition.setUseBirthYear(true);

        // needed because some classes such as Athlete refer to the current competition
        Competition.setCurrent(competition);

        return competition;
    }

    protected static void createGroup(EntityManager em, Group group, final String[] fnames, final String[] lnames,
            Random r, int cat1, int cat2, int liftersToLoad, boolean masters, int min, int max,
            Gender gender) {
        if (liftersToLoad < 1)
            liftersToLoad = 1;

        for (int i = 0; i < liftersToLoad; i++) {
            Athlete p = new Athlete();
            try {
                p.setLoggerLevel(Level.WARN);
                p.setGroup(group);
                p.setFirstName(fnames[r.nextInt(fnames.length)]);
                p.setLastName(lnames[r.nextInt(lnames.length)]);
                p.setGender(gender);
                double nextDouble = r.nextDouble();
                if (nextDouble > 0.5F) {
                    createAthlete(em, r, p, nextDouble, cat1, masters, min, max, gender);
                } else {
                    createAthlete(em, r, p, nextDouble, cat2, masters, min, max, gender);
                }
                em.persist(p);
            } catch (Exception e) {
                logger.error(LoggerUtils.stackTrace(e));
            } finally {
                p.resetLoggerLevel();
            }
        }
    }

    protected static void defaultPlates(Platform platform1) {
        // setDefaultMixerName(platform1);
        platform1.setShowDecisionLights(true);
        platform1.setShowTimer(true);
        // collar
        platform1.setNbC_2_5(1);
        // small plates
        platform1.setNbS_0_5(1);
        platform1.setNbS_1(1);
        platform1.setNbS_1_5(1);
        platform1.setNbS_2(1);
        platform1.setNbS_2_5(1);
        platform1.setNbS_5(1);
        // large plates, regulation set-up
        platform1.setNbL_2_5(0);
        platform1.setNbL_5(0);
        platform1.setNbL_10(1);
        platform1.setNbL_15(1);
        platform1.setNbL_20(1);
        platform1.setNbL_25(3);
    }

    protected static void drawLots(EntityManager em) {
        List<Athlete> athletes = AthleteRepository.doFindAll(em);
        AthleteSorter.drawLots(athletes);
    }

    private static String getDefaultLanguage() {
        // default language as defined in properties file (not the JVM).
        // this will typically be en.
        return getLocale().getLanguage();
    }

    private static Locale getLocale() {
        return Locale.ENGLISH;
    }

    /**
     * Insert initial data if the database is empty.
     *
     * @param nbAthletes how many athletes
     * @param masters
     */
    public static void insertInitialData(int nbAthletes, boolean masters) {
        startLogger.info("inserting demo data.{}", masters ? " (masters=true)" : "");
        JPAService.runInTransaction(em -> {
            CategoryRepository.insertStandardCategories(em);
            return null;
        });
        
        JPAService.runInTransaction(em -> {
            setupDemoData(em, nbAthletes, masters);
            return null;
        });
    }

    private static void insertSampleLifters(EntityManager em, int liftersToLoad, Group groupM1, Group groupM2,
            Group groupF1, Group groupY1, boolean masters) {
        final String[] lnames = { "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson",
                "Moore", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia",
                "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen", "Young",
                "Hernandez", "King", "Wright", "Lopez", "Hill", "Scott", "Green", "Adams", "Baker", "Gonzalez",
                "Nelson", "Carter", "Mitchell", "Perez", "Roberts", "Turner", "Phillips", "Campbell", "Parker", "Evans",
                "Edwards", "Collins", };
        final String[] mNames = { "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
                "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Donald", "Mark", "Paul", "Steven",
                "Andrew", "Kenneth", "George", "Joshua", "Kevin", "Brian", "Edward", "Ronald", "Timothy", "Jason",
                "Jeffrey", "Ryan", "Jacob", "Gary", "Nicholas", "Eric", "Stephen", "Jonathan", "Larry", "Justin",
                "Scott", "Brandon", "Frank", "Benjamin", "Gregory", "Raymond", "Samuel", "Patrick", "Alexander", "Jack",
                "Dennis", "Jerry", };
        final String[] fNames = { "Emily", "Abigail", "Alexis", "Alyssa", "Angela", "Ashley", "Brianna", "Cynthia",
                "Deborah", "Donna", "Elizabeth", "Elizabeth", "Emma", "Grace", "Hannah", "Jennifer", "Jessica", "Julie",
                "Karen", "Kayla", "Kimberly", "Laura", "Lauren", "Linda", "Lisa", "Lori", "Madison", "Mary", "Megan",
                "Michelle", "Olivia", "Pamela", "Patricia", "Samantha", "Sandra", "Sarah", "Susan", "Tammy", "Taylor",
                "Victoria", };

        Random r = new Random(0);
        Random r2 = new Random(0);

        if (masters) {
            createGroup(em, groupM1, mNames, lnames, r, 81, 73, liftersToLoad, true, 35, 45, Gender.M);
            createGroup(em, groupM2, mNames, lnames, r, 73, 67, liftersToLoad, true, 35, 50, Gender.M);
            createGroup(em, groupF1, fNames, lnames, r2, 59, 59, liftersToLoad / 2, true, 35, 45, Gender.F);
            createGroup(em, groupY1, mNames, lnames, r2, 55, 61, liftersToLoad / 4, true, 13, 17, Gender.M);
            createGroup(em, groupY1, fNames, lnames, r2, 45, 49, liftersToLoad / 4, true, 13, 17, Gender.F);
        } else {
            createGroup(em, groupM1, mNames, lnames, r, 81, 73, liftersToLoad, false, 18, 32, Gender.M);
            createGroup(em, groupM2, mNames, lnames, r, 73, 67, liftersToLoad, false, 18, 32, Gender.M);
            createGroup(em, groupF1, fNames, lnames, r2, 59, 59, liftersToLoad / 2, false, 18, 32, Gender.F);
            createGroup(em, groupY1, mNames, lnames, r2, 55, 61, liftersToLoad / 4, false, 13, 17, Gender.M);
            createGroup(em, groupY1, fNames, lnames, r2, 45, 49, liftersToLoad / 4, false, 13, 17, Gender.F);
        }

        drawLots(em);

        assignStartNumbers(em, groupM1);
        assignStartNumbers(em, groupM2);
        assignStartNumbers(em, groupF1);
        assignStartNumbers(em, groupY1);
    }

    protected static void setupCompetitionDocuments(Competition competition, Platform platform1) {
        // competition template
        File templateFile;
        String defaultLanguage = getDefaultLanguage();
        String templateName;
        if (!defaultLanguage.equals("fr")) {
            templateName = "/templates/protocolSheet/ProtocolSheetTemplate_" + defaultLanguage + ".xls";
        } else {
            // historical kludge for Québec
            templateName = "/templates/protocolSheet/Quebec_" + defaultLanguage + ".xls";
        }
        URL templateUrl = platform1.getClass().getResource(templateName);
        try {
            templateFile = new File(templateUrl.toURI());
            competition.setProtocolFileName(templateFile.getCanonicalPath());
        } catch (URISyntaxException e) {
            templateFile = new File(templateUrl.getPath());
        } catch (IOException e) {
        } catch (Exception e) {
            logger.debug("templateName = {}", templateName);
        }

        // competition book template
        templateUrl = platform1.getClass()
                .getResource("/templates/competitionBook/CompetitionBook_Total_" + defaultLanguage + ".xls");
        try {
            templateFile = new File(templateUrl.toURI());
            competition.setFinalPackageTemplateFileName(templateFile.getCanonicalPath());
        } catch (URISyntaxException e) {
            templateFile = new File(templateUrl.getPath());
        } catch (IOException e) {
        } catch (Exception e) {
            logger.debug("templateUrl = {}", templateUrl);
        }
    }

    /**
     * Setup demo data.
     * 
     * @param em
     *
     * @param competition   the competition
     * @param liftersToLoad the lifters to load
     * @param masters
     * @param w             the w
     * @param c             the c
     */
    protected static void setupDemoData(EntityManager em, int liftersToLoad, boolean masters) {
        Competition competition = createDefaultCompetition(masters);

        LocalDateTime w = LocalDateTime.now();
        LocalDateTime c = w.plusHours((long) 2.0);

        Platform platform1 = new Platform("A");
        defaultPlates(platform1);
        Platform platform2 = new Platform("B");
        defaultPlates(platform2);

        Group groupM1 = new Group("M1", w, c);
        groupM1.setPlatform(platform1);

        Group groupM2 = new Group("M2", w, c);
        groupM2.setPlatform(platform2);

        Group groupF1 = new Group("F1", w, c);
        groupF1.setPlatform(platform1);

        Group groupY1 = new Group("Y1", w, c);
        groupY1.setPlatform(platform2);

        em.persist(groupM1);
        em.persist(groupM2);
        em.persist(new Group("M3", null, null));
        em.persist(new Group("M4", null, null));
        em.persist(groupF1);
        em.persist(groupY1);
        em.persist(new Group("F3", null, null));

        em.persist(platform1);
        em.persist(platform2);
        em.persist(competition);

        insertSampleLifters(em, liftersToLoad, groupM1, groupM2, groupF1, groupY1, masters);
        em.flush();
    }

}
