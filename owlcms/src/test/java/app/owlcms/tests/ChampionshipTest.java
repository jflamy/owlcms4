/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.category.ParticipationId;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.team.TeamResultsTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.spreadsheet.PAthlete;
import ch.qos.logback.classic.Logger;

public class ChampionshipTest {

    private static final String FIXTURE_RESOURCE = "/testDatabases/mixedTestsJRSR.mv.db";
    private static final String MEMORY_JDBC_URL = "jdbc:h2:mem:owlcms;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
    @SuppressWarnings("unused")
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ChampionshipTest.class);

    private static Path fixtureDirectory;
    private static Map<ParticipationId, Boolean> originalMixedTeamMemberships;

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
        originalMixedTeamMemberships = snapshotMixedTeamMemberships();
    }

    @After
    public void restoreMixedTeamMembershipsAfterEachTest() {
        restoreMixedTeamMemberships(originalMixedTeamMemberships);
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

    @Test
    public void testSeniorMixedTeamsWithNoAthletesAreAbsent() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertFalse("Senior mixed teams in fixture should be score-based", senior.computeMixedPointsBased());

        clearMixedTeamMemberships(senior);

        List<TeamTreeItem> teams = computeTeamResults(senior, Gender.MF);
        assertTrue("senior mixed teams should be absent when no athlete has mixed-team membership", teams.isEmpty());
    }

    @Test
    public void testSeniorMixedTeamScoresWithFirstFemaleInSmallestCategory() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertFalse("Senior mixed teams in fixture should be score-based", senior.computeMixedPointsBased());

        // Select candidates from the original fixture state, then apply (which clears all others)
        Map<String, List<Participation>> selectedByTeam = selectMixedTeamMembers(senior, 1, 0);
        printSelectedAthletes("Senior mixed first female", selectedByTeam);
        applyMixedTeamMemberships(senior, selectedByTeam);

        // Verify from the athlete side: each team has exactly one selected female
        for (Map.Entry<String, List<Participation>> entry : selectedByTeam.entrySet()) {
            assertEquals("selected count for " + entry.getKey(), 1, entry.getValue().size());
            Participation participation = entry.getValue().get(0);
            Athlete athlete = participation.getAthlete();
            Double expectedGamx = getDirectGamxScore(participation);
            assertNotNull("selected athlete GAMX for " + entry.getKey(), expectedGamx);
            logger.info("Senior first-female athlete {} ({}) GAMX={}",
                athlete.getFullName(), entry.getKey(), String.format(Locale.ROOT, "%.2f", expectedGamx));
        }

        // Verify from the team side: iteration contains only the selected athlete and
        // the team score matches that athlete's direct GAMX.
        List<TeamTreeItem> teams = computeTeamResults(senior, Gender.MF);
        for (TeamTreeItem team : teams) {
            logger.info("Senior mixed first-female team {} : size={}, counted={}, score={}",
                team.getName(), team.getSize(), team.getCounted(), team.getScore());
            List<Participation> selected = selectedByTeam.getOrDefault(team.getName(), List.of());
            assertEquals("counted for " + team.getName(), selected.size(), team.getCounted().intValue());
            assertEquals("iterated athletes for " + team.getName(), selected.size(), team.getTeamMembers().size());

            Participation selectedParticipation = selected.get(0);
            Athlete selectedAthlete = selectedParticipation.getAthlete();
            Double expectedScore = getDirectGamxScore(selectedParticipation);
            TeamTreeItem iteratedAthlete = team.getTeamMembers().get(0);

            assertEquals("iterated athlete id for " + team.getName(), selectedAthlete.getId(),
                    iteratedAthlete.getAthlete().getId());
            assertRoundedTo2("iterated athlete score for " + team.getName(), expectedScore, iteratedAthlete.getScore());
            assertRoundedTo2("score for " + team.getName(), expectedScore, team.getScore());
        }
    }

    @Test
    public void testSeniorMixedTeamScoresWithFirstTwoFemalesAndFirstTwoMales() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertFalse("Senior mixed teams in fixture should be score-based", senior.computeMixedPointsBased());

        Map<String, List<Participation>> selectedByTeam = selectMixedTeamMembers(senior, 2, 2);
        printSelectedAthletes("Senior mixed first two females and first two males", selectedByTeam);
        applyMixedTeamMemberships(senior, selectedByTeam);

        List<TeamTreeItem> teams = computeTeamResults(senior, Gender.MF);
        for (TeamTreeItem team : teams) {
            logger.info("Senior mixed two+two team {} : size={}, counted={}, score={}",
                team.getName(), team.getSize(), team.getCounted(), team.getScore());
        }
        assertMixedTeamScoresMatchSelectedGamx(teams, selectedByTeam,
                "senior mixed first two females and first two males");
    }

    @Test
    public void testSeniorExplicitMixedMemberWorksForPouraminFromIri() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        Championship junior = ChampionshipRepository.findByName("Junior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertNotNull("Junior championship should be loaded from fixture", junior);

        Participation seniorSeedParticipation = findChampionshipParticipationForAthlete(senior, "POURAMIN", "IRI");
        clearMixedTeamMemberships(senior);
        Athlete athlete = AthleteRepository.findById(seniorSeedParticipation.getAthlete().getId());
        assertNotNull("POURAMIN from IRI should exist for Senior championship", athlete);
        assertEquals("expected athlete last name", "POURAMIN", athlete.getLastName().toUpperCase(Locale.ROOT));
        assertEquals("expected athlete team", "IRI", athlete.getTeam());

        Participation selectedParticipation = findParticipationForChampionship(athlete, senior);
        Participation juniorParticipation = findParticipationForChampionship(athlete, junior);
        assertNotNull("POURAMIN from IRI should also exist for Junior championship", juniorParticipation);
        assertFalse("Senior and Junior participations should be distinct for " + athlete.getFullName(),
            selectedParticipation.getId().equals(juniorParticipation.getId()));

        selectedParticipation.setMixedTeamMember(true);
        AthleteRepository.save(athlete);

        Athlete reloadedAthlete = AthleteRepository.findById(athlete.getId());
        assertNotNull("reloaded athlete should exist for " + athlete.getFullName(), reloadedAthlete);
        Participation reloadedSelectedParticipation = findParticipationForChampionship(reloadedAthlete, senior);
        Participation reloadedJuniorParticipation = findParticipationForChampionship(reloadedAthlete, junior);

        assertTrue("selected Senior participation should be explicit mixed member for " + athlete.getFullName(),
            reloadedSelectedParticipation.getMixedTeamMember());
        assertFalse("Junior participation should not be the selected Senior participation for " + athlete.getFullName(),
            reloadedSelectedParticipation.getId().equals(reloadedJuniorParticipation.getId()));
        assertFalse("Junior participation should remain non-explicit for " + athlete.getFullName(),
            Boolean.TRUE.equals(reloadedJuniorParticipation.getMixedTeamMember()));

        List<TeamTreeItem> teams = computeTeamResults(senior, Gender.MF);
        TeamTreeItem team = teams.stream()
            .filter(item -> item.getName().equals(reloadedAthlete.getTeam()))
                .findFirst()
                .orElse(null);
        assertNotNull("team results should include " + reloadedAthlete.getTeam() + " for " + reloadedAthlete.getFullName(), team);
        assertEquals("counted athletes for " + reloadedAthlete.getTeam(), 1, team.getCounted().intValue());
        assertEquals("iterated athletes for " + reloadedAthlete.getTeam(), 1, team.getTeamMembers().size());

        TeamTreeItem iteratedAthlete = team.getTeamMembers().get(0);
        Double expectedScore = getDirectGamxScore(reloadedSelectedParticipation);
        assertEquals("iterated athlete id for " + reloadedAthlete.getFullName(), reloadedAthlete.getId(),
                iteratedAthlete.getAthlete().getId());
        assertRoundedTo2("iterated athlete score for " + reloadedAthlete.getFullName(), expectedScore,
                iteratedAthlete.getScore());
        assertRoundedTo2("team score for " + reloadedAthlete.getTeam(), expectedScore, team.getScore());
    }

    @Test
    public void testSeniorMensTeamScores() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertTrue("Senior gendered teams in fixture should be points-based", senior.computePointsBased());

        List<TeamTreeItem> teams = computeTeamResults(senior, Gender.M);
        assertEquals("senior men's team count", 4, teams.size());

        Map<String, int[]> expected = Map.of(
            "EGY", new int[] { 5, 5, 372 },
            "IRI", new int[] { 5, 5, 332 },
            "TUR", new int[] { 5, 5, 342 },
            "UZB", new int[] { 5, 5, 399 }
        );

        for (TeamTreeItem team : teams) {
            int[] exp = expected.get(team.getName());
            assertNotNull("unexpected team " + team.getName(), exp);
            assertEquals("size for " + team.getName(), exp[0], (int) team.getSize());
            assertEquals("counted for " + team.getName(), exp[1], team.getCounted().intValue());
            assertEquals("points for " + team.getName(), exp[2], team.getPoints().intValue());

            int summedMemberPoints = team.getTeamMembers().stream()
                    .map(TeamTreeItem::getPoints)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            assertEquals("team points should equal summed athlete points for " + team.getName(),
                    exp[2], summedMemberPoints);
            logger.info("Senior men's team {} : size={}, counted={}, points={}, summedMemberPoints={}",
                team.getName(), team.getSize(), team.getCounted(), team.getPoints(), summedMemberPoints);
        }
    }

    @Test
    public void testJuniorImplicitMixedTeamsUseConfiguredResultValue() {
        Championship junior = ChampionshipRepository.findByName("Junior");
        assertNotNull("Junior championship should be loaded from fixture", junior);
        assertTrue("Junior championship in fixture should be points-based", junior.computePointsBased());

        List<TeamTreeItem> juniorMixedTeams = computeTeamResults(junior, Gender.MF);
        assertEquals("junior implicit mixed team count", 4, juniorMixedTeams.size());

        Map<String, int[]> expected = Map.of(
            "EGY", new int[] { 4, 4, 324 },
            "IRI", new int[] { 6, 6, 433 },
            "TUR", new int[] { 1, 1, 84 },
            "UZB", new int[] { 3, 3, 243 }
        );

        for (TeamTreeItem team : juniorMixedTeams) {
            int[] exp = expected.get(team.getName());
            assertNotNull("unexpected team " + team.getName(), exp);
            assertEquals("size for " + team.getName(), exp[0], (int) team.getSize());
            assertEquals("counted for " + team.getName(), exp[1], team.getCounted().intValue());
            assertEquals("points for " + team.getName(), exp[2], team.getPoints().intValue());
        }
    }

    @Test
    public void testSeniorReportingBeansMatchTeamResults() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        // Set up explicit mixed team memberships: 2 women + 2 men per team
        Map<String, List<Participation>> selectedByTeam = selectMixedTeamMembers(senior, 2, 2);
        applyMixedTeamMemberships(senior, selectedByTeam);

        // Compute reporting beans (same path as CompetitionBook / PackageContent)
        HashMap<String, Object> beans = Competition.getCurrent().computeReportingInfo(null, senior);

        // --- Validate mTeamSenior bean (gendered men's team athletes) ---
        @SuppressWarnings("unchecked")
        List<Athlete> mTeam = (List<Athlete>) beans.get("mTeamSenior");
        assertNotNull("mTeamSenior bean should exist", mTeam);
        assertTrue("mTeamSenior should not be empty", !mTeam.isEmpty());

        // All athletes in the mTeam bean should be Male PAthletes marked as team members
        for (Athlete a : mTeam) {
            assertTrue("mTeamSenior athlete should be PAthlete: " + a.getFullName(), a instanceof PAthlete);
            assertEquals("mTeamSenior athlete gender: " + a.getFullName(), Gender.M, a.getGender());
            assertTrue("mTeamSenior athlete should be team member: " + a.getFullName(), a.isTeamMember());
        }

        // Sum points per team from mTeam bean and compare to tree data.
        // The tree uses combinedPoints (snatch+CJ+total) when snatchCJTotalMedals is set.
        boolean combinedTotal = senior.isSnatchCJTotalMedals();
        List<TeamTreeItem> menTeams = computeTeamResults(senior, Gender.M);
        Map<String, Integer> beanPointsByTeam = mTeam.stream()
                .filter(a -> a.getTeam() != null)
                .collect(Collectors.groupingBy(Athlete::getTeam,
                        Collectors.summingInt(a -> combinedTotal ? a.getCombinedPoints() : a.getTotalPoints())));
        for (TeamTreeItem team : menTeams) {
            Integer beanPoints = beanPointsByTeam.getOrDefault(team.getName(), 0);
            assertEquals("mTeamSenior points match tree for " + team.getName(),
                    team.getPoints().intValue(), beanPoints.intValue());
            logger.info("Senior mTeam bean vs tree for {} : beanPoints={}, treePoints={}",
                    team.getName(), beanPoints, team.getPoints());
        }

        // --- Validate mwTeamSenior bean (explicit mixed team athletes) ---
        @SuppressWarnings("unchecked")
        List<Athlete> mwTeam = (List<Athlete>) beans.get("mwTeamSenior");
        assertNotNull("mwTeamSenior bean should exist", mwTeam);

        // Only explicitly selected mixed-team members should be in this bean
        Set<Long> selectedAthleteIds = selectedByTeam.values().stream()
                .flatMap(List::stream)
                .map(p -> p.getAthlete().getId())
                .collect(Collectors.toSet());
        for (Athlete a : mwTeam) {
            assertTrue("mwTeamSenior athlete should be PAthlete: " + a.getFullName(), a instanceof PAthlete);
            assertTrue("mwTeamSenior athlete should be in explicit selection: " + a.getFullName(),
                    selectedAthleteIds.contains(a.getId()));
        }
        assertEquals("mwTeamSenior should contain all selected mixed members",
                selectedAthleteIds.size(), mwTeam.size());

        // Verify mwTeam GAMX scores match the tree results
        List<TeamTreeItem> mixedTeams = computeTeamResults(senior, Gender.MF);
        Map<String, Double> beanGamxByTeam = mwTeam.stream()
                .filter(a -> a.getTeam() != null)
                .collect(Collectors.groupingBy(Athlete::getTeam,
                        Collectors.summingDouble(Athlete::getGamx)));
        for (TeamTreeItem team : mixedTeams) {
            Double beanGamx = beanGamxByTeam.getOrDefault(team.getName(), 0.0);
            assertRoundedTo2("mwTeamSenior GAMX match tree for " + team.getName(),
                    team.getScore(), beanGamx);
            logger.info("Senior mwTeam bean vs tree for {} : beanGamx={}, treeScore={}",
                    team.getName(), String.format(Locale.ROOT, "%.2f", beanGamx),
                    String.format(Locale.ROOT, "%.2f", team.getScore()));
        }
    }

    @Test
    public void testJuniorReportingBeansMatchTeamResults() {
        Championship junior = ChampionshipRepository.findByName("Junior");
        assertNotNull("Junior championship should be loaded from fixture", junior);
        assertFalse("Junior should not have explicit mixed team members",
                junior.isExplicitMixedTeamMembers());

        // Compute reporting beans
        HashMap<String, Object> beans = Competition.getCurrent().computeReportingInfo(null, junior);

        // --- Validate mTeamJunior bean ---
        @SuppressWarnings("unchecked")
        List<Athlete> mTeam = (List<Athlete>) beans.get("mTeamJunior");
        assertNotNull("mTeamJunior bean should exist", mTeam);
        assertTrue("mTeamJunior should not be empty", !mTeam.isEmpty());

        for (Athlete a : mTeam) {
            assertTrue("mTeamJunior athlete should be PAthlete: " + a.getFullName(), a instanceof PAthlete);
            assertEquals("mTeamJunior athlete gender: " + a.getFullName(), Gender.M, a.getGender());
        }

        // Compare mTeam bean points to tree data.
        // The tree uses combinedPoints (snatch+CJ+total) when snatchCJTotalMedals is set.
        boolean combinedTotal = junior.isSnatchCJTotalMedals();
        List<TeamTreeItem> menTeams = computeTeamResults(junior, Gender.M);
        Map<String, Integer> beanPointsByTeam = mTeam.stream()
                .filter(a -> a.getTeam() != null)
                .collect(Collectors.groupingBy(Athlete::getTeam,
                        Collectors.summingInt(a -> combinedTotal ? a.getCombinedPoints() : a.getTotalPoints())));
        for (TeamTreeItem team : menTeams) {
            Integer beanPoints = beanPointsByTeam.getOrDefault(team.getName(), 0);
            assertEquals("mTeamJunior points match tree for " + team.getName(),
                    team.getPoints().intValue(), beanPoints.intValue());
            logger.info("Junior mTeam bean vs tree for {} : beanPoints={}, treePoints={}",
                    team.getName(), beanPoints, team.getPoints());
        }

        // --- Validate mwTeamJunior bean (implicit = all men + all women) ---
        @SuppressWarnings("unchecked")
        List<Athlete> mwTeam = (List<Athlete>) beans.get("mwTeamJunior");
        assertNotNull("mwTeamJunior bean should exist", mwTeam);

        @SuppressWarnings("unchecked")
        List<Athlete> wTeam = (List<Athlete>) beans.get("wTeamJunior");
        assertNotNull("wTeamJunior bean should exist", wTeam);

        // For implicit mixed, mwTeam should contain all men + all women
        Set<Long> mwIds = mwTeam.stream().map(Athlete::getId).collect(Collectors.toSet());
        Set<Long> mIds = mTeam.stream().map(Athlete::getId).collect(Collectors.toSet());
        Set<Long> wIds = wTeam.stream().map(Athlete::getId).collect(Collectors.toSet());
        Set<Long> mPlusW = new java.util.HashSet<>(mIds);
        mPlusW.addAll(wIds);
        assertEquals("mwTeamJunior should be union of mTeam + wTeam", mPlusW, mwIds);

        // Compare mwTeam bean points to tree (implicit mixed uses points)
        List<TeamTreeItem> mixedTeams = computeTeamResults(junior, Gender.MF);
        Map<String, Integer> mixedBeanPointsByTeam = mwTeam.stream()
                .filter(a -> a.getTeam() != null)
                .collect(Collectors.groupingBy(Athlete::getTeam,
                        Collectors.summingInt(a -> combinedTotal ? a.getCombinedPoints() : a.getTotalPoints())));
        for (TeamTreeItem team : mixedTeams) {
            Integer beanPoints = mixedBeanPointsByTeam.getOrDefault(team.getName(), 0);
            assertEquals("mwTeamJunior points match tree for " + team.getName(),
                    team.getPoints().intValue(), beanPoints.intValue());
            logger.info("Junior mwTeam bean vs tree for {} : beanPoints={}, treePoints={}",
                    team.getName(), beanPoints, team.getPoints());
        }
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

    private static void clearMixedTeamMemberships(Championship championship) {
        applyMixedTeamMemberships(championship, Map.of());
    }

    private static List<TeamTreeItem> computeTeamResults(Championship championship, Gender gender) {
        TeamResultsTreeData teamResults = new TeamResultsTreeData(null, championship, gender, Ranking.GAMX, true);
        List<TeamTreeItem> teams = teamResults.getTeamItemsByGender().get(gender);
        return teams != null ? teams : List.of();
    }

    private static void assertMixedTeamScoresMatchSelectedGamx(List<TeamTreeItem> teams,
            Map<String, List<Participation>> selectedByTeam, String label) {
        assertTrue(label + " should produce mixed teams", !teams.isEmpty());

        for (TeamTreeItem team : teams) {
            List<Participation> selectedParticipations = selectedByTeam.getOrDefault(team.getName(), List.of());
            assertEquals(label + " counted athletes for team " + team.getName(), selectedParticipations.size(),
                    team.getCounted().intValue());
            assertEquals(label + " iterated athletes for team " + team.getName(), selectedParticipations.size(),
                    team.getTeamMembers().size());

            Map<Long, TeamTreeItem> iteratedAthletesById = team.getTeamMembers().stream()
                .collect(Collectors.toMap(item -> item.getAthlete().getId(), item -> item));

            double expectedScore = 0.0D;
            for (Participation selectedParticipation : selectedParticipations) {
                Athlete selectedAthlete = selectedParticipation.getAthlete();
            TeamTreeItem iteratedAthlete = iteratedAthletesById.get(selectedAthlete.getId());
                Double expectedAthleteScore = getDirectGamxScore(selectedParticipation);

            assertNotNull(label + " missing iterated athlete for team " + team.getName() + ": "
                + selectedAthlete.getFullName(), iteratedAthlete);
            assertRoundedTo2(label + " iterated athlete score for team " + team.getName() + ": "
                + selectedAthlete.getFullName(),
                        expectedAthleteScore, iteratedAthlete.getScore());
                expectedScore += expectedAthleteScore;
            }

            assertRoundedTo2(label + " score for team " + team.getName(), expectedScore, team.getScore());
        }
    }

    private static Double getDirectGamxScore(Participation participation) {
        Athlete rankedAthlete = participation.getAthlete();
        assertNotNull("ranked athlete for participation " + participation.getId(), rankedAthlete);
        Double gamx = rankedAthlete.getGamx();
        assertNotNull("direct GAMX for " + rankedAthlete.getFullName(), gamx);
        return gamx;
    }

    private static void applyMixedTeamMemberships(Championship championship, Map<String, List<Participation>> selectedByTeam) {
        Set<ParticipationId> selectedParticipationIds = selectedByTeam.values().stream()
                .flatMap(List::stream)
                .filter(participation -> participation != null && participation.getId() != null)
                .map(Participation::getId)
                .collect(Collectors.toSet());
        Set<Long> categoryIds = getChampionshipCategoryIds(championship);

        JPAService.runInTransaction(em -> {
            List<Participation> participations = em.createQuery(
                    "select distinct p from Participation p join p.category c where c.id in :categoryIds",
                    Participation.class)
                    .setParameter("categoryIds", categoryIds)
                    .getResultList();
            for (Participation participation : participations) {
                participation.setMixedTeamMember(selectedParticipationIds.contains(participation.getId()));
            }
            return null;
        });
    }

    private static void printSelectedAthletes(String label, Map<String, List<Participation>> selectedByTeam) {
        selectedByTeam.forEach((teamName, athletes) -> {
            String athleteSummary = athletes.isEmpty()
                    ? "none"
                    : athletes.stream().map(ChampionshipTest::describeAthlete).collect(Collectors.joining(", "));
            logger.info("{} | {} | {}", label, teamName, athleteSummary);
        });
    }

    private static String describeAthlete(Participation participation) {
        Athlete athlete = participation.getAthlete();
        String categoryName = participation.getCategory() != null ? participation.getCategory().getNameWithAgeGroup() : "?";
        Double gamx = athlete.getGamx();
        String gamxText = gamx != null ? String.format(Locale.ROOT, "%.2f", gamx) : "null";
        return athlete.getFullName() + " [" + categoryName + ", GAMX=" + gamxText + "]";
    }

    private static Map<String, List<Participation>> selectMixedTeamMembers(Championship championship, int femaleCount,
            int maleCount) {
        LinkedHashMap<String, List<Participation>> groupedByTeam = getMixedCandidatesByTeam(championship);
        LinkedHashMap<String, List<Participation>> selectedByTeam = new LinkedHashMap<>();

        for (Map.Entry<String, List<Participation>> entry : groupedByTeam.entrySet()) {
            List<Participation> selected = new ArrayList<>();
            selected.addAll(selectAthletes(entry.getValue(), Gender.F, femaleCount));
            selected.addAll(selectAthletes(entry.getValue(), Gender.M, maleCount));
            selectedByTeam.put(entry.getKey(), selected);
        }

        long totalSelected = selectedByTeam.values().stream().mapToLong(List::size).sum();
        assertTrue("expected at least one mixed-team athlete selection for " + championship.getName(), totalSelected > 0);
        return selectedByTeam;
    }

    private static LinkedHashMap<String, List<Participation>> getMixedCandidatesByTeam(Championship championship) {
        List<Participation> participations = getChampionshipParticipations(championship)
                .stream()
                .filter(p -> p.getAthlete() != null && p.getAthlete().getTeam() != null
                        && !p.getAthlete().getTeam().isBlank())
                .sorted(participationThenNameComparator())
                .collect(Collectors.toList());
        assertNotNull("missing mixed-team candidates for " + championship.getName(), participations);

        LinkedHashMap<String, List<Participation>> groupedByTeam = new LinkedHashMap<>();
        for (Participation participation : participations) {
            groupedByTeam.computeIfAbsent(participation.getAthlete().getTeam(), ignored -> new ArrayList<>())
                    .add(participation);
        }
        return groupedByTeam;
    }

    private static List<Participation> selectAthletes(List<Participation> participations, Gender gender, int count) {
        return participations.stream()
                .filter(participation -> participation.getAthlete().getGender() == gender)
                .sorted(participationThenNameComparator())
                .limit(count)
                .collect(Collectors.toList());
    }

    private static Comparator<Participation> participationThenNameComparator() {
        return Comparator.comparing((Participation participation) -> participation.getAthlete().getTeam(),
                String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Participation::getCategory)
                .thenComparing(participation -> participation.getAthlete().getLastName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(participation -> participation.getAthlete().getFirstName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(participation -> participation.getId() != null ? participation.getId().athleteId : Long.MAX_VALUE)
                .thenComparing(participation -> participation.getId() != null ? participation.getId().categoryId : Long.MAX_VALUE);
    }

    private static List<Participation> getChampionshipParticipations(Championship championship) {
        Set<Long> categoryIds = getChampionshipCategoryIds(championship);

        return JPAService.runInTransaction(em -> em.createQuery(
                "select distinct p from Participation p join p.athlete a join p.category c",
                Participation.class)
                .getResultList())
                .stream()
                .filter(participation -> participation.getCategory() != null
                        && participation.getCategory().getId() != null
                        && categoryIds.contains(participation.getCategory().getId()))
                .collect(Collectors.toList());
    }

                private static Set<Long> getChampionshipCategoryIds(Championship championship) {
                List<AgeGroup> ageGroups = AgeGroupRepository.findFiltered(null, null, championship, null, true, -1, -1);
                return ageGroups.stream()
                    .map(AgeGroup::getCategories)
                    .flatMap(List::stream)
                    .map(Category::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
                }

                private static Participation findChampionshipParticipationForAthlete(Championship championship, String lastName,
                    String team) {
                Participation participation = getChampionshipParticipations(championship).stream()
                .filter(p -> p.getAthlete() != null)
                    .filter(p -> lastName.equalsIgnoreCase(p.getAthlete().getLastName()))
                    .filter(p -> team.equalsIgnoreCase(p.getAthlete().getTeam()))
                .findFirst()
                .orElse(null);
                assertNotNull("expected athlete " + lastName + " from " + team + " in " + championship.getName()
                    + " championship participations", participation);
                return participation;
                }

                private static Participation findParticipationForChampionship(Athlete athlete, Championship championship) {
                Set<Long> categoryIds = getChampionshipCategoryIds(championship);
                Participation participation = athlete.getParticipations().stream()
                    .filter(p -> p.getCategory() != null
                        && p.getCategory().getId() != null
                        && categoryIds.contains(p.getCategory().getId()))
                    .findFirst()
                    .orElse(null);
                assertNotNull("expected " + athlete.getFullName() + " to have a participation in " + championship.getName(),
                    participation);
                return participation;
                }

    private static Map<ParticipationId, Boolean> snapshotMixedTeamMemberships() {
        return JPAService.runInTransaction(em -> em.createQuery("select p from Participation p", Participation.class)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(Participation::getId, Participation::getMixedTeamMember)));
    }

    private static void restoreMixedTeamMemberships(Map<ParticipationId, Boolean> mixedFlagsByParticipationId) {
        JPAService.runInTransaction(em -> {
            List<Participation> participations = em.createQuery("select p from Participation p", Participation.class)
                    .getResultList();
            for (Participation participation : participations) {
                Boolean mixed = mixedFlagsByParticipationId.get(participation.getId());
                participation.setMixedTeamMember(Boolean.TRUE.equals(mixed));
            }
            return null;
        });
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