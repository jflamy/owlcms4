/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

public class ChampionshipTest {

    private static final String FIXTURE_RESOURCE = "/testDatabases/mixedTestsJRSR.mv.db";
    private static final String MEMORY_JDBC_URL = "jdbc:h2:mem:owlcms;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
    @SuppressWarnings("unused")
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ChampionshipTest.class);

    private static Path fixtureDirectory;

    @BeforeClass
    public static void setupTests() throws Exception {
        Main.injectSuppliers();
        System.setProperty("JDBC_DATABASE_URL", MEMORY_JDBC_URL);
        loadFixtureIntoMemoryDatabase();
        JPAService.init(true, false);
        Config.initConfig();
        Competition.setCurrent(null);
        Championship.reset();

        // Replicate the essential parts of Main.initData()
        Gender.initPublicGenderCodeMapString(Locale.ENGLISH);

        // Override the fixture's persisted ranking settings so this test controls
        // exactly which global/category scoring systems are computed.
        overrideFixtureEnabledRankings();

        Competition.recomputeAllAthleteRanks();
    }

    @AfterClass
    public static void tearDownTests() throws Exception {
        Championship.reset();
        Competition.setCurrent(null);
        JPAService.close();
        System.clearProperty("JDBC_DATABASE_URL");
        deleteFixtureDirectory();
    }

    @Test
    public void testCompetitionLoadedFromFixture() {
        Competition competition = Competition.getCurrent();

        assertEquals("competition name", "6th Islamic Solidarity Games", competition.getCompetitionName());
        assertEquals("competition scoring system", Ranking.BW_SINCLAIR, competition.getScoringSystem());
        assertEquals("mixed team size", Integer.valueOf(8), competition.getMixedBestN());
    }

    @Test
    public void testFixtureChampionshipsAreAvailable() {
        assertEquals("stored championship count", 5, ChampionshipRepository.findAll().size());

        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertTrue("Senior should require explicit mixed team members", senior.isExplicitMixedTeamMembers());
        assertEquals("Senior best athlete scoring", Ranking.GAMX, senior.getBestAthleteScoringSystem());
        assertEquals("Senior best snatch scoring", Ranking.GAMX_S, senior.getBestSnatchScoringSystem());
        assertEquals("Senior best clean and jerk scoring", Ranking.GAMX_C, senior.getBestCJScoringSystem());
        assertEquals("Senior mixed team scoring", Ranking.GAMX, senior.getMixedTeamScoringSystem());
    }

    @Test
    public void testChampionshipApiListsLoadedChampionships() {
        List<String> championshipNames = Championship.findAll().stream().map(Championship::getName)
                .collect(Collectors.toList());

        assertEquals("championship names from OWLCMS API",
            List.of("Masters", "Youth", "Junior", "Senior", "Open"), championshipNames);
    }

    @Test
    public void testMedalComputationProducesRanks() {
        // recomputeAllAthleteRanks() was called in setupTests().
        // Ranks are per-category on Participation objects, accessed through PAthletes.
        // We use computeMedalsByCategory() which returns PAthletes with ranks set,
        // just like ResultsMedals does.
        List<Athlete> weighedIn = AthleteRepository.findAllByGroupAndWeighIn(null, true);
        assertTrue("fixture should have weighed-in athletes", weighedIn.size() > 0);

        Competition competition = Competition.getCurrent();
        TreeMap<String, List<Athlete>> medals = competition.computeMedalsByCategory(weighedIn);
        assertNotNull("medal map should not be null", medals);
        assertTrue("medal map should have category entries", medals.size() > 0);

        // Spot-check JR_F48: gold should be POURAMIN
        List<Athlete> jrF48 = medals.get("JR_F48");
        assertNotNull("JR_F48 should be in medal map", jrF48);
        Athlete jrF48Gold = jrF48.stream()
                .filter(a -> a.getTotalRank() == 1)
                .findFirst().orElse(null);
        assertNotNull("JR_F48 should have a gold medalist", jrF48Gold);
        assertTrue("JR_F48 gold medalist should be Pouramin, got " + jrF48Gold.getLastName(),
            "POURAMIN".equalsIgnoreCase(jrF48Gold.getLastName()));
        assertRoundedTo2("JR_F48 gold GAMX", 871.21D, jrF48Gold.getGamx());

        // Spot-check SR_F48: gold should be ALTUN
        List<Athlete> srF48 = medals.get("SR_F48");
        assertNotNull("SR_F48 should be in medal map", srF48);
        Athlete srF48Gold = srF48.stream()
                .filter(a -> a.getTotalRank() == 1)
                .findFirst().orElse(null);
        assertNotNull("SR_F48 should have a gold medalist", srF48Gold);
        assertTrue("SR_F48 gold medalist should be Altun, got " + srF48Gold.getLastName(),
            "ALTUN".equalsIgnoreCase(srF48Gold.getLastName()));
            assertRoundedTo2("SR_F48 gold GAMX", 1026.14D, srF48Gold.getGamx());

        // Spot-check: verify ranks are stored on participations in the database
        Athlete anyAthlete = weighedIn.stream()
                .filter(a -> a.getTotal() > 0)
                .findFirst()
                .orElse(null);
        assertNotNull("should have at least one athlete with a total", anyAthlete);
        boolean hasRankedParticipation = anyAthlete.getParticipations().stream()
                .anyMatch(p -> p.getTotalRank() > 0);
        assertTrue("athlete " + anyAthlete.getLastName()
                + " should have at least one participation with totalRank > 0",
                hasRankedParticipation);
    }

    private static void loadFixtureIntoMemoryDatabase() throws IOException, SQLException {
        fixtureDirectory = Files.createTempDirectory("championship-test-db-");
        Path copiedDatabase = fixtureDirectory.resolve("mixedTestsJRSR.mv.db");
        Path scriptFile = fixtureDirectory.resolve("mixedTestsJRSR.sql");

        try (InputStream fixtureStream = ChampionshipTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull("Fixture database not found: " + FIXTURE_RESOURCE, fixtureStream);
            Files.copy(fixtureStream, copiedDatabase, StandardCopyOption.REPLACE_EXISTING);
        }

        String sourceBase = copiedDatabase.toAbsolutePath().toString().replaceAll("\\.mv\\.db$", "");
        String sourceUrl = "jdbc:h2:file:" + sourceBase + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0";
        String escapedScriptFile = escapePath(scriptFile);

        try (Connection source = DriverManager.getConnection(sourceUrl, "sa", "");
                Statement sourceStatement = source.createStatement()) {
            sourceStatement.execute("SCRIPT TO '" + escapedScriptFile + "'");
        }

        try (Connection target = DriverManager.getConnection(MEMORY_JDBC_URL, "sa", "");
                Statement targetStatement = target.createStatement()) {
            targetStatement.execute("RUNSCRIPT FROM '" + escapedScriptFile + "'");
        }
    }

    private static void overrideFixtureEnabledRankings() {
        Competition competition = Competition.getCurrent();
        competition.setEnabledRankings(List.of(
            Ranking.BW_SINCLAIR.name(),
            Ranking.GAMX.name(),
            Ranking.CAT_GAMX.name()
        ));
        JPAService.runInTransaction(em -> {
            em.merge(competition);
            return null;
        });
        competition.initializeRankingConfig();
    }

    private static void assertRoundedTo2(String message, double expected, Double actual) {
        assertNotNull(message + " should not be null", actual);
        double actualRounded = Math.round(actual * 100.0D) / 100.0D;
        double expectedRounded = Math.round(expected * 100.0D) / 100.0D;
        assertEquals(message, expectedRounded, actualRounded, 0.0D);
    }

    private static String escapePath(Path path) {
        return path.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "''");
    }

    private static void deleteFixtureDirectory() throws IOException {
        if (fixtureDirectory == null || !Files.exists(fixtureDirectory)) {
            return;
        }

        try (var paths = Files.walk(fixtureDirectory)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }
}