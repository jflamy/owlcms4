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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.agegroup.DefaultChampionship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.category.ParticipationId;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.team.TeamResultsDisplayRules;
import app.owlcms.data.team.TeamSelectionDisplayRules;
import app.owlcms.data.team.TeamSelectionTreeData;
import app.owlcms.data.team.TeamResultsTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.spreadsheet.JXLSTeamResultsSheet;
import ch.qos.logback.classic.Logger;

public class ChampionshipTest {

    private static class ChampionshipConfigSnapshot {

        private final boolean explicitMixedTeamMembers;
        private final boolean mixedTeamEnabled;
        private final Integer explicitTeamSize;
        private final Ranking teamScoringSystem;
        private final Ranking mixedTeamScoringSystem;
        private final Integer mensBestN;
        private final Integer womensBestN;
        private final Integer mixedBestN;
        private final Integer mixedMensBestN;
        private final Integer mixedWomensBestN;

        private ChampionshipConfigSnapshot(Championship championship) {
            this.explicitMixedTeamMembers = championship.isExplicitMixedTeamMembers();
            this.mixedTeamEnabled = championship.isMixedTeamEnabled();
            this.explicitTeamSize = championship.getExplicitTeamSize();
            this.teamScoringSystem = championship.getTeamScoringSystem();
            this.mixedTeamScoringSystem = championship.getMixedTeamScoringSystem();
            this.mensBestN = championship.getMensBestN();
            this.womensBestN = championship.getWomensBestN();
            this.mixedBestN = championship.getMixedBestN();
            this.mixedMensBestN = championship.getMixedMensBestN();
            this.mixedWomensBestN = championship.getMixedWomensBestN();
        }
    }

    private static final String FIXTURE_RESOURCE = "/testDatabases/mixedTestsJRSR.mv.db";
    private static String memoryJdbcUrl;
    @SuppressWarnings("unused")
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ChampionshipTest.class);

    private static Path fixtureDirectory;
    private static Map<ParticipationId, Boolean> originalMixedTeamMemberships;
    private static Map<String, ChampionshipConfigSnapshot> originalChampionshipConfigs;

    @BeforeClass
    public static void setupTests() throws Exception {
        Main.injectSuppliers();
        memoryJdbcUrl = createMemoryJdbcUrl();
        System.setProperty("JDBC_DATABASE_URL", memoryJdbcUrl);
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

        // The fixture database predates the mixedTeamEnabled column.
        // Set the correct values for each championship.
        initFixtureMixedTeamEnabled();

        Competition.recomputeAllAthleteRanks();
        originalMixedTeamMemberships = snapshotMixedTeamMemberships();
        originalChampionshipConfigs = snapshotChampionshipConfigs();
    }

    @After
    public void restoreMixedTeamMembershipsAfterEachTest() {
        restoreMixedTeamMemberships(originalMixedTeamMemberships);
        restoreChampionshipConfigs(originalChampionshipConfigs);
    }

    @AfterClass
    public static void tearDownTests() throws Exception {
        try {
            Championship.reset();
        } catch (Exception e) {
            // DB may already be closed if setup failed
        }
        Competition.setCurrent(null);
        JPAService.close();
        memoryJdbcUrl = null;
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
    public void testMissingChampionshipCanBeDetectedAndCreatedWithoutLosingDefaultFallback() {
        String championshipName = "Temporary Championship " + UUID.randomUUID();

        assertNull("missing championship should not have a stored row yet",
                Championship.findStored(championshipName));
        assertSame("missing championship should still resolve to the default fallback",
                DefaultChampionship.getInstance(), Championship.of(championshipName));

        Championship created = Championship.ensureStored(championshipName, ChampionshipType.U);
        try {
            assertNotNull("ensureStored should create a championship row", created);
            assertNotNull("created championship should be persisted", created.getId());
            assertEquals("created championship name", championshipName, created.getName());

            Championship stored = Championship.findStored(championshipName);
            assertNotNull("stored championship should now be discoverable", stored);
            assertEquals("stored championship id should match created row", created.getId(), stored.getId());
        } finally {
            ChampionshipRepository.delete(created);
            Championship.reset();
        }
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
    public void testSeniorTeamSelectionWithoutGenderFilterAlsoShowsMixedTeams() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        Set<String> mixedRootNames = computeTeamSelectionRootNames(senior, Gender.MF).stream()
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
        assertFalse("senior team selection should expose mixed roots when MF is selected", mixedRootNames.isEmpty());

        Map<Gender, Set<String>> unfilteredRootNames = computeTeamSelectionRootNamesByGender(senior, null);
        assertFalse("senior team selection without gender filter should still expose men's roots",
                unfilteredRootNames.getOrDefault(Gender.M, Set.of()).isEmpty());
        assertFalse("senior team selection without gender filter should still expose women's roots",
                unfilteredRootNames.getOrDefault(Gender.F, Set.of()).isEmpty());
        assertTrue("senior team selection without gender filter should also expose mixed roots",
                unfilteredRootNames.getOrDefault(Gender.MF, Set.of()).containsAll(mixedRootNames));
    }

    @Test
    public void testMastersTeamSummaryShowsOnlyChampionshipSelectedScore() throws Exception {
        Championship masters = ChampionshipRepository.findByName("Masters");
        assertNotNull("Masters championship should be loaded from fixture", masters);

        Competition competition = Competition.getCurrent();
        Ranking originalCompetitionScoring = competition.getScoringSystem();

        try {
            competition.setScoringSystem(Ranking.BW_SINCLAIR);
            masters.setTeamScoringSystem(Ranking.QPOINTS);
            masters.setMixedTeamScoringSystem(Ranking.GAMX);

            assertEquals("masters score columns should include selected and informational rankings",
                List.of(Ranking.QPOINTS, Ranking.GAMX, Ranking.BW_SINCLAIR, Ranking.QAGE, Ranking.SMM),
                TeamResultsDisplayRules.getRequiredScoreRankings(masters, null, competition.getScoringSystem()));

            TeamTreeItem mensTeam = new TeamTreeItem("Test Men", Gender.M, null, false);
            TeamTreeItem mixedTeam = new TeamTreeItem("Test Mixed", Gender.MF, null, false);
            Athlete athlete = AthleteRepository.findAll().stream().findFirst().orElse(null);
            assertNotNull("fixture should provide at least one athlete", athlete);
            TeamTreeItem athleteItem = new TeamTreeItem(null, athlete.getGender(), athlete, true);

            assertTrue("men's team should show selected QPoints total",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, mensTeam, Ranking.QPOINTS));
            assertFalse("men's team should hide points total for score-based championships",
                TeamResultsDisplayRules.shouldShowTeamSummaryPoints(masters, mensTeam));
            assertFalse("men's team should hide mixed GAMX total",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, mensTeam, Ranking.GAMX));
            assertFalse("men's team should hide competition Sinclair total",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, mensTeam, Ranking.BW_SINCLAIR));
            assertFalse("men's team should hide informational Q-Masters total",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, mensTeam, Ranking.QAGE));
            assertFalse("men's team should hide informational SMHF total",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, mensTeam, Ranking.SMM));

            assertTrue("mixed team should show selected GAMX total",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, mixedTeam, Ranking.GAMX));
            assertFalse("mixed team should hide points total for score-based championships",
                TeamResultsDisplayRules.shouldShowTeamSummaryPoints(masters, mixedTeam));
            assertFalse("mixed team should hide gendered QPoints total",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, mixedTeam, Ranking.QPOINTS));

            masters.setTeamScoringSystem(null);
            masters.setMixedTeamScoringSystem(null);
            assertTrue("points-based men's championship should show points total",
                TeamResultsDisplayRules.shouldShowTeamSummaryPoints(masters, mensTeam));
            assertTrue("points-based mixed championship should show points total",
                TeamResultsDisplayRules.shouldShowTeamSummaryPoints(masters, mixedTeam));

            assertTrue("athlete rows should keep informational columns visible",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, athleteItem, Ranking.QAGE));
            assertTrue("athlete rows should keep best-athlete informational columns visible",
                TeamResultsDisplayRules.shouldShowTeamSummaryValue(masters, athleteItem, Ranking.BW_SINCLAIR));
            assertTrue("athlete rows should keep points visible",
                TeamResultsDisplayRules.shouldShowTeamSummaryPoints(masters, athleteItem));
        } finally {
            competition.setScoringSystem(originalCompetitionScoring);
        }
    }

        @Test
        public void testTeamSelectionDisplayRulesUseMixedMembershipColumnForMixedRowsWhenUnfiltered() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertTrue("Senior championship should enable mixed teams for the preparation view", senior.isMixedTeamEnabled());

        TeamTreeItem mensTeam = new TeamTreeItem("EGY", Gender.M, null, false);
        TeamTreeItem mixedTeam = new TeamTreeItem("EGY", Gender.MF, null, false);

        Athlete maleAthlete = getChampionshipParticipations(senior).stream()
            .map(Participation::getAthlete)
            .filter(Objects::nonNull)
            .filter(a -> a.getGender() == Gender.M)
            .findFirst()
            .orElse(null);
        Athlete femaleAthlete = getChampionshipParticipations(senior).stream()
            .map(Participation::getAthlete)
            .filter(Objects::nonNull)
            .filter(a -> a.getGender() == Gender.F)
            .findFirst()
            .orElse(null);
        assertNotNull("fixture should provide at least one senior male athlete", maleAthlete);
        assertNotNull("fixture should provide at least one senior female athlete", femaleAthlete);

        TeamTreeItem mensAthleteItem = new TeamTreeItem(null, maleAthlete.getGender(), maleAthlete, true);
        mensAthleteItem.setParent(mensTeam);
        TeamTreeItem mixedAthleteItem = new TeamTreeItem(null, femaleAthlete.getGender(), femaleAthlete, true);
        mixedAthleteItem.setParent(mixedTeam);

        assertTrue("unfiltered team selection should keep the standard membership column visible",
            TeamSelectionDisplayRules.shouldShowMembershipColumn((Gender) null));
        assertTrue("unfiltered team selection should also show the mixed membership column",
            TeamSelectionDisplayRules.shouldShowMixedMembershipColumn(senior, null));

        assertTrue("gendered team roots should use the standard membership column",
            TeamSelectionDisplayRules.shouldShowMembershipColumn(senior, null, mensTeam));
        assertFalse("gendered team roots should not use the mixed membership column",
            TeamSelectionDisplayRules.shouldShowMixedMembershipColumn(senior, null, mensTeam));
        assertTrue("gendered team athletes should use the standard membership column",
            TeamSelectionDisplayRules.shouldShowMembershipColumn(senior, null, mensAthleteItem));
        assertFalse("gendered team athletes should not use the mixed membership column",
            TeamSelectionDisplayRules.shouldShowMixedMembershipColumn(senior, null, mensAthleteItem));

        assertFalse("mixed team roots should not use the standard membership column when unfiltered",
            TeamSelectionDisplayRules.shouldShowMembershipColumn(senior, null, mixedTeam));
        assertTrue("mixed team roots should use the mixed membership column when unfiltered",
            TeamSelectionDisplayRules.shouldShowMixedMembershipColumn(senior, null, mixedTeam));
        assertFalse("mixed team athletes should not use the standard membership column when unfiltered",
            TeamSelectionDisplayRules.shouldShowMembershipColumn(senior, null, mixedAthleteItem));
        assertTrue("mixed team athletes should use the mixed membership column when unfiltered",
            TeamSelectionDisplayRules.shouldShowMixedMembershipColumn(senior, null, mixedAthleteItem));
        }

    @Test
    public void testChampionshipConfiguredTeamSizeFollowsStatusRules() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        senior.setMaxTeamSize(8);
        senior.setExplicitTeamSize(6);
        senior.setMensBestN(4);
        senior.setWomensBestN(3);
        senior.setMixedBestN(5);
        senior.setMixedMensBestN(2);
        senior.setMixedWomensBestN(2);
        senior.setExplicitMixedTeamMembers(true);

        assertEquals("gendered men should use mensBestN when configured", 4,
            senior.getConfiguredTeamSize(null, Gender.M));
        assertEquals("gendered women should use womensBestN when configured", 3,
            senior.getConfiguredTeamSize(null, Gender.F));
        assertEquals("mixed should prefer overall mixedBestN when configured", 5,
            senior.getConfiguredTeamSize(null, Gender.MF));

        senior.setMixedBestN(null);
        assertEquals("mixed should use mixed men + women topN when overall mixedBestN is absent", 4,
            senior.getConfiguredTeamSize(null, Gender.MF));

        senior.setMixedMensBestN(null);
        senior.setMixedWomensBestN(null);
        assertEquals("explicit mixed should fall back to explicit roster size", 6,
            senior.getConfiguredTeamSize(null, Gender.MF));

        senior.setMensBestN(null);
        assertEquals("gendered men should fall back to roster size when mensBestN is absent", 8,
            senior.getConfiguredTeamSize(null, Gender.M));

        senior.setExplicitMixedTeamMembers(false);
        assertEquals("implicit mixed should fall back to max team size when no mixed topN is configured", 8,
            senior.getConfiguredTeamSize(null, Gender.MF));
    }

    @Test
    public void testMixedExplicitTeamSizeDefaultsToSmallestChampionshipCategoryCount() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        int expectedDefault = AgeGroupRepository.findFiltered(null, null, senior, null, true, -1, -1).stream()
            .map(AgeGroup::getCategories)
            .mapToInt(List::size)
            .filter(categoryCount -> categoryCount > 0)
            .min()
            .orElse(senior.getMaxTeamSize());

        senior.setMixedBestN(null);
        senior.setMixedMensBestN(null);
        senior.setMixedWomensBestN(null);
        senior.setExplicitMixedTeamMembers(true);
        senior.setExplicitTeamSize(null);

        assertTrue("fixture should provide at least one positive championship category count", expectedDefault > 0);
        assertEquals("explicit mixed default should use the smallest category count across championship age groups",
            expectedDefault, senior.getExplicitTeamSize().intValue());
        assertEquals("configured mixed team size should use the computed explicit default when no cap is configured",
            expectedDefault, senior.getConfiguredTeamSize(null, Gender.MF));

        senior.setExplicitTeamSize(expectedDefault + 1);
        assertEquals("configured explicit size should override the computed mixed default",
            expectedDefault + 1, senior.getConfiguredTeamSize(null, Gender.MF));
    }

    @Test
    public void testChampionshipRenameUpdatesCacheAndAgeGroupReferences() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        List<AgeGroup> originalAgeGroups = AgeGroupRepository.findFiltered(null, null, senior, null, true, -1, -1);
        assertFalse("fixture should provide age groups for Senior championship", originalAgeGroups.isEmpty());

        String originalName = senior.getName();
        String renamedName = originalName + " Renamed";

        try {
            senior.rename(renamedName);

            Championship renamed = Championship.findStored(renamedName);
            assertNotNull("renamed championship should be available from cache/db by its new name", renamed);
            assertNull("old championship name should no longer resolve after rename", Championship.findStored(originalName));

            List<AgeGroup> renamedAgeGroups = AgeGroupRepository.findFiltered(null, null, renamed, null, true, -1, -1);
            assertEquals("renamed championship should retain the same age-group membership",
                originalAgeGroups.size(), renamedAgeGroups.size());
            assertTrue("age groups should carry the renamed championship name",
                renamedAgeGroups.stream().allMatch(ageGroup -> renamedName.equals(ageGroup.getChampionshipName())));
        } finally {
            Championship renamed = Championship.findStored(renamedName);
            if (renamed != null) {
                renamed.rename(originalName);
            }
        }
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
        public void testPointBasedTeamResultsIgnoreUnfinishedGroups() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertTrue("Senior championship in fixture should be points-based", senior.computePointsBased());
        assertAllChampionshipGroupsDone(senior);

        List<TeamTreeItem> initialTeams = computeTeamResults(senior, Gender.M);
        TeamTreeItem initialTeam = initialTeams.stream()
            .filter(team -> !team.getCountedTeamMembers().isEmpty())
            .findFirst()
            .orElseThrow(() -> new AssertionError("expected at least one counted men's team"));
        TeamTreeItem initialMember = initialTeam.getCountedTeamMembers().get(0);
        Athlete initialAthlete = initialMember.getAthlete();
        assertNotNull("counted athlete should be present", initialAthlete);
        assertNotNull("counted athlete should belong to a group", initialAthlete.getGroup());
        assertNotNull("counted athlete should have point score", initialMember.getPoints());

        Long groupId = initialAthlete.getGroup().getId();
        Long athleteId = initialAthlete.getId();
        String teamName = initialTeam.getName();
        int originalTeamPoints = initialTeam.getPoints();
        int originalCounted = initialTeam.getCounted();
        int removedMemberPoints = initialMember.getPoints();
        boolean originalDone = setGroupDone(groupId, false);
        assertTrue("selected group should start done in fixture", originalDone);

        try {
            List<TeamTreeItem> updatedTeams = computeTeamResults(senior, Gender.M);
            TeamTreeItem updatedTeam = updatedTeams.stream()
                .filter(team -> teamName.equals(team.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing team after marking group unfinished: " + teamName));

            assertEquals("unfinished group athlete should not add team points",
                originalTeamPoints - removedMemberPoints, updatedTeam.getPoints().intValue());
            assertEquals("unfinished group athlete should not consume a counted slot",
                originalCounted - 1, updatedTeam.getCounted().intValue());

            TeamTreeItem unfinishedMember = updatedTeam.getTeamMembers().stream()
                .filter(member -> member.getAthlete() != null && athleteId.equals(member.getAthlete().getId()))
                .findFirst()
                .orElse(null);
            assertNotNull("unfinished athlete should still be present in team member pool", unfinishedMember);
            assertNull("unfinished athlete should not show awarded points", unfinishedMember.getPoints());
            assertFalse("unfinished athlete should not be counted for the team", unfinishedMember.isCountedForTeam());
            assertFalse("unfinished athlete should not appear in counted team members",
                updatedTeam.getCountedTeamMembers().stream()
                    .anyMatch(member -> member.getAthlete() != null && athleteId.equals(member.getAthlete().getId())));
        } finally {
            setGroupDone(groupId, originalDone);
        }
        }

    @Ignore("Score-based mixed-team totals may include unfinished groups because scores are monotonic.")
    @Test
    public void testScoreBasedTeamResultsIgnoreUnfinishedGroups() {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);
        assertFalse("Senior mixed teams in fixture should be score-based", senior.computeMixedPointsBased());
        assertAllChampionshipGroupsDone(senior);

        List<TeamTreeItem> initialTeams = computeTeamResults(senior, Gender.MF);
        TeamTreeItem initialTeam = initialTeams.stream()
            .filter(team -> !team.getCountedTeamMembers().isEmpty())
            .findFirst()
            .orElseThrow(() -> new AssertionError("expected at least one counted mixed team"));
        TeamTreeItem initialMember = initialTeam.getCountedTeamMembers().get(0);
        Athlete initialAthlete = initialMember.getAthlete();
        assertNotNull("counted mixed athlete should be present", initialAthlete);
        assertNotNull("counted mixed athlete should belong to a group", initialAthlete.getGroup());
        assertNotNull("counted mixed athlete should have score", initialMember.getScore());

        Long groupId = initialAthlete.getGroup().getId();
        Long athleteId = initialAthlete.getId();
        String teamName = initialTeam.getName();
        double originalTeamScore = initialTeam.getScore();
        int originalCounted = initialTeam.getCounted();
        double removedMemberScore = initialMember.getScore();
        boolean originalDone = setGroupDone(groupId, false);
        assertTrue("selected mixed group should start done in fixture", originalDone);

        try {
            List<TeamTreeItem> updatedTeams = computeTeamResults(senior, Gender.MF);
            TeamTreeItem updatedTeam = updatedTeams.stream()
                .filter(team -> teamName.equals(team.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing mixed team after marking group unfinished: " + teamName));

            assertRoundedTo2("unfinished group athlete should not add mixed team score",
                originalTeamScore - removedMemberScore, updatedTeam.getScore());
            assertEquals("unfinished group athlete should not consume a counted mixed slot",
                originalCounted - 1, updatedTeam.getCounted().intValue());

            TeamTreeItem unfinishedMember = updatedTeam.getTeamMembers().stream()
                .filter(member -> member.getAthlete() != null && athleteId.equals(member.getAthlete().getId()))
                .findFirst()
                .orElse(null);
            assertNotNull("unfinished mixed athlete should still be present in team member pool", unfinishedMember);
            assertFalse("unfinished mixed athlete should not be counted for the team", unfinishedMember.isCountedForTeam());
            assertFalse("unfinished mixed athlete should not appear in counted team members",
                updatedTeam.getCountedTeamMembers().stream()
                    .anyMatch(member -> member.getAthlete() != null && athleteId.equals(member.getAthlete().getId())));
        } finally {
            setGroupDone(groupId, originalDone);
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
            public void testJuniorImplicitMixedTeamsUseTop2MenTop2WomenWithMixedScoring() {
            Championship junior = ChampionshipRepository.findByName("Junior");
            assertNotNull("Junior championship should be loaded from fixture", junior);

            configureMixedTeamRules(junior, false, null, Ranking.GAMX, 0, 2, 2);
            assertPersistedMixedTeamRules("junior implicit mixed top 2 men top 2 women",
                junior, false, null, Ranking.GAMX, 0, 2, 2);

            Map<String, List<Participation>> candidatePoolByTeam = getMixedCandidatesByTeam(junior);
            Map<String, List<Participation>> expectedCountedByTeam = computeExpectedMixedCountedByTeam(candidatePoolByTeam,
                Ranking.GAMX, 0, 2, 2);
            printSelectedAthletes("junior implicit mixed top 2 men top 2 women | expected counted",
                expectedCountedByTeam);

            List<TeamTreeItem> teams = computeTeamResults(junior, Gender.MF);
            assertMixedTeamScoresMatchExpectedSelection(teams, candidatePoolByTeam, expectedCountedByTeam,
                "junior implicit mixed top 2 men top 2 women");
            assertMixedReportingBeanMatchesTree("junior implicit mixed top 2 men top 2 women",
                junior, teams, null, null);
            }

            @Test
            public void testJuniorImplicitMixedTeamsUseTop3GenderNeutralWithMixedScoring() {
            Championship junior = ChampionshipRepository.findByName("Junior");
            assertNotNull("Junior championship should be loaded from fixture", junior);

            configureMixedTeamRules(junior, false, null, Ranking.GAMX, 3, 2, 2);
            assertPersistedMixedTeamRules("junior implicit mixed top 3 gender neutral",
                junior, false, null, Ranking.GAMX, 3, 2, 2);

            Map<String, List<Participation>> candidatePoolByTeam = getMixedCandidatesByTeam(junior);
            Map<String, List<Participation>> expectedCountedByTeam = computeExpectedMixedCountedByTeam(candidatePoolByTeam,
                Ranking.GAMX, 3, 2, 2);
            printSelectedAthletes("junior implicit mixed top 3 gender neutral | expected counted",
                expectedCountedByTeam);

            List<TeamTreeItem> teams = computeTeamResults(junior, Gender.MF);
            assertMixedTeamScoresMatchExpectedSelection(teams, candidatePoolByTeam, expectedCountedByTeam,
                "junior implicit mixed top 3 gender neutral");
            assertMixedReportingBeanMatchesTree("junior implicit mixed top 3 gender neutral",
                junior, teams, null, null);
            }

            @Test
            public void testSeniorExplicitMixedSubsetUsesTop2MenTop2Women() {
            Championship senior = ChampionshipRepository.findByName("Senior");
            assertNotNull("Senior championship should be loaded from fixture", senior);

            Map<String, List<Participation>> explicitSubsetByTeam = selectMixedTeamMembers(senior, 3, 3);
            printSelectedAthletes("senior explicit mixed subset top 2 men top 2 women | explicit roster",
                explicitSubsetByTeam);
            applyMixedTeamMemberships(senior, explicitSubsetByTeam);
            assertPersistedExplicitMixedRoster("senior explicit mixed subset top 2 men top 2 women",
                senior, explicitSubsetByTeam, 3, 3);
            configureMixedTeamRules(senior, true, null, Ranking.GAMX, 0, 2, 2);
            assertPersistedMixedTeamRules("senior explicit mixed subset top 2 men top 2 women",
                senior, true, null, Ranking.GAMX, 0, 2, 2);

            Map<String, List<Participation>> expectedCountedByTeam = computeExpectedMixedCountedByTeam(explicitSubsetByTeam,
                Ranking.GAMX, 0, 2, 2);
            printSelectedAthletes("senior explicit mixed subset top 2 men top 2 women | expected counted",
                expectedCountedByTeam);

            List<TeamTreeItem> teams = computeTeamResults(senior, Gender.MF);
            assertMixedTeamScoresMatchExpectedSelection(teams, explicitSubsetByTeam, expectedCountedByTeam,
                "senior explicit mixed subset top 2 men top 2 women");
            assertMixedReportingBeanMatchesTree("senior explicit mixed subset top 2 men top 2 women",
                senior, teams, explicitSubsetByTeam, expectedCountedByTeam);
            }

            @Test
            public void testSeniorTeamResultsExportUsesCountedMixedMembers() throws Exception {
            Championship senior = ChampionshipRepository.findByName("Senior");
            assertNotNull("Senior championship should be loaded from fixture", senior);

            Map<String, List<Participation>> explicitSubsetByTeam = selectMixedTeamMembers(senior, 3, 3);
            applyMixedTeamMemberships(senior, explicitSubsetByTeam);
            configureMixedTeamRules(senior, true, null, Ranking.GAMX, 0, 2, 2);

            Map<String, List<Participation>> expectedCountedByTeam = computeExpectedMixedCountedByTeam(explicitSubsetByTeam,
                Ranking.GAMX, 0, 2, 2);
            Set<Long> expectedCountedIds = expectedCountedByTeam.values().stream()
                .flatMap(List::stream)
                .map(participation -> participation.getAthlete().getId())
                .collect(Collectors.toSet());

            JXLSTeamResultsSheet sheet = new JXLSTeamResultsSheet(null);
            sheet.setChampionship(senior);
            sheet.setGender(Gender.MF);
            invokeSetReportingInfo(sheet);

            @SuppressWarnings("unchecked")
            List<Athlete> mwTeam = (List<Athlete>) sheet.getReportingBeans().get("mwTeam");
            assertNotNull("Team Results export should publish mwTeam bean", mwTeam);
            assertEquals("Team Results export should only expose counted mixed members",
                expectedCountedIds, mwTeam.stream().map(Athlete::getId).collect(Collectors.toSet()));

            @SuppressWarnings("unchecked")
            List<TeamTreeItem> mwTeamItems = (List<TeamTreeItem>) sheet.getReportingBeans().get("mwTeamItems");
            assertNotNull("Team Results export should publish mwTeamItems", mwTeamItems);
            assertEquals("Team Results export should publish configured mixed team size",
                senior.getConfiguredTeamSize(null, Gender.MF),
                ((Integer) sheet.getReportingBeans().get("mwTeamSize")).intValue());

            Set<Long> exportedItemIds = mwTeamItems.stream()
                .flatMap(team -> team.getCountedTeamMembers().stream())
                .map(member -> member.getAthlete().getId())
                .collect(Collectors.toSet());
            assertEquals("Team Results export team items should match counted mixed members",
                expectedCountedIds, exportedItemIds);
            }

    @Test
    public void testSeniorTeamResultsExportUsesMixedChampionshipScoring() throws Exception {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        Map<String, List<Participation>> explicitSubsetByTeam = selectMixedTeamMembers(senior, 3, 3);
        applyMixedTeamMemberships(senior, explicitSubsetByTeam);
        // Mixed championship explicitly uses GAMX so the score column is meaningful.
        configureMixedTeamRules(senior, true, null, Ranking.GAMX, 0, 2, 2);

        JXLSTeamResultsSheet sheet = new JXLSTeamResultsSheet(null);
        sheet.setChampionship(senior);
        sheet.setGender(Gender.MF);
        invokeSetReportingInfo(sheet);

        assertEquals("Mixed export should use the mixed-team scoring title",
                Ranking.getScoringTitle(Ranking.GAMX), sheet.getReportingBeans().get("mwScoringTitle"));

        @SuppressWarnings("unchecked")
        List<TeamTreeItem> mwTeamItems = (List<TeamTreeItem>) sheet.getReportingBeans().get("mwTeamItems");
        assertNotNull("Team Results export should publish mwTeamItems", mwTeamItems);
        assertFalse("Team Results export should publish mixed teams", mwTeamItems.isEmpty());

        for (TeamTreeItem team : mwTeamItems) {
            double expectedTeamScore = 0.0D;
            for (TeamTreeItem member : team.getCountedTeamMembers()) {
                double expectedMemberScore = Ranking.getRankingValue(member.getAthlete(), Ranking.GAMX);
                assertRoundedTo2("Mixed export member score should follow mixed championship scoring for "
                        + member.getAthlete().getFullName(), expectedMemberScore, member.getScore());
                expectedTeamScore += expectedMemberScore;
            }
            assertRoundedTo2("Mixed export team score should follow mixed championship scoring for "
                    + team.getName(), expectedTeamScore, team.getScore());
        }
    }

    /**
     * Verify that men's, women's, and mixed sheets each use their own championship
     * scoring system and that no value leaks across tabs. The mixed scoring system
     * (GAMX) must not appear on the gendered tabs, and the gendered scoring system
     * (BW_SINCLAIR) must not appear on the mixed tab.
     */
    @Test
    public void testTeamResultsExportSegregatesScoringSystemsPerSheet() throws Exception {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        Map<String, List<Participation>> explicitSubsetByTeam = selectMixedTeamMembers(senior, 3, 3);
        applyMixedTeamMemberships(senior, explicitSubsetByTeam);
        // Distinct scoring systems for gendered vs mixed tabs.
        configureMixedTeamRules(senior, true, Ranking.BW_SINCLAIR, Ranking.GAMX, 0, 2, 2);

        JXLSTeamResultsSheet sheet = new JXLSTeamResultsSheet(null);
        sheet.setChampionship(senior);
        sheet.setGender(Gender.MF);
        invokeSetReportingInfo(sheet);

        Map<String, Object> beans = sheet.getReportingBeans();

        // Titles must be segregated per tab.
        assertEquals("Men's tab should use gendered (BW_SINCLAIR) scoring title",
                Ranking.getScoringTitle(Ranking.BW_SINCLAIR), beans.get("mScoringTitle"));
        assertEquals("Women's tab should use gendered (BW_SINCLAIR) scoring title",
                Ranking.getScoringTitle(Ranking.BW_SINCLAIR), beans.get("wScoringTitle"));
        assertEquals("Mixed tab should use mixed (GAMX) scoring title",
                Ranking.getScoringTitle(Ranking.GAMX), beans.get("mwScoringTitle"));

        // showPoints flags must reflect per-tab configuration (both non-points-based here).
        assertEquals("Men's tab should not be points-based when team scoring is set",
                Boolean.FALSE, beans.get("mShowPoints"));
        assertEquals("Women's tab should not be points-based when team scoring is set",
                Boolean.FALSE, beans.get("wShowPoints"));
        assertEquals("Mixed tab should not be points-based when mixed scoring is set",
                Boolean.FALSE, beans.get("mwShowPoints"));

        @SuppressWarnings("unchecked")
        List<TeamTreeItem> mTeamItems = (List<TeamTreeItem>) beans.get("mTeamItems");
        @SuppressWarnings("unchecked")
        List<TeamTreeItem> wTeamItems = (List<TeamTreeItem>) beans.get("wTeamItems");
        @SuppressWarnings("unchecked")
        List<TeamTreeItem> mwTeamItems = (List<TeamTreeItem>) beans.get("mwTeamItems");

        // Mixed must always be present (the export targets the Mixed sheet).
        assertNotNull("Mixed items must be present", mwTeamItems);
        assertFalse("Mixed items must not be empty", mwTeamItems.isEmpty());

        // Item objects must not be shared across tabs (per-gender lists are disjoint).
        Set<TeamTreeItem> all = new java.util.HashSet<>();
        if (mTeamItems != null) {
            for (TeamTreeItem t : mTeamItems) {
                assertTrue("Men's TeamTreeItem must be unique to its tab", all.add(t));
                for (TeamTreeItem m : t.getCountedTeamMembers()) {
                    assertTrue("Men's member TeamTreeItem must be unique to its tab", all.add(m));
                }
            }
        }
        if (wTeamItems != null) {
            for (TeamTreeItem t : wTeamItems) {
                assertTrue("Women's TeamTreeItem must be unique to its tab", all.add(t));
                for (TeamTreeItem m : t.getCountedTeamMembers()) {
                    assertTrue("Women's member TeamTreeItem must be unique to its tab", all.add(m));
                }
            }
        }
        for (TeamTreeItem t : mwTeamItems) {
            assertTrue("Mixed TeamTreeItem must be unique to its tab", all.add(t));
            for (TeamTreeItem m : t.getCountedTeamMembers()) {
                assertTrue("Mixed member TeamTreeItem must be unique to its tab", all.add(m));
            }
        }

        // Each item must compute its score using its tab's scoring system —
        // no cross-tab leakage. We assert this through the public getScore()
        // surface, which is what the Excel template reads.
        if (mTeamItems != null) {
            for (TeamTreeItem t : mTeamItems) {
                for (TeamTreeItem m : t.getCountedTeamMembers()) {
                    double expected = Ranking.getRankingValue(m.getAthlete(), Ranking.BW_SINCLAIR);
                    assertRoundedTo2("Men's member must use gendered (BW_SINCLAIR) scoring for "
                            + m.getAthlete().getFullName(), expected, m.getScore());
                }
            }
        }
        if (wTeamItems != null) {
            for (TeamTreeItem t : wTeamItems) {
                for (TeamTreeItem m : t.getCountedTeamMembers()) {
                    double expected = Ranking.getRankingValue(m.getAthlete(), Ranking.BW_SINCLAIR);
                    assertRoundedTo2("Women's member must use gendered (BW_SINCLAIR) scoring for "
                            + m.getAthlete().getFullName(), expected, m.getScore());
                }
            }
        }
        for (TeamTreeItem t : mwTeamItems) {
            double expectedTeam = 0.0D;
            for (TeamTreeItem m : t.getCountedTeamMembers()) {
                double expected = Ranking.getRankingValue(m.getAthlete(), Ranking.GAMX);
                assertRoundedTo2("Mixed member must use mixed (GAMX) scoring for "
                        + m.getAthlete().getFullName(), expected, m.getScore());
                expectedTeam += expected;
            }
            assertRoundedTo2("Mixed team score must accumulate mixed (GAMX) scoring for "
                    + t.getName(), expectedTeam, t.getScore());
        }
    }

    @Test
    public void testTeamResultsTemplatesIterateCountedTeamMembers() throws Exception {
        assertTeamResultsTemplateUsesCountedMembers("/templates/teamResults/TeamResults-A4.xlsx");
        assertTeamResultsTemplateUsesCountedMembers("/templates/teamResults/TeamResults-Letter.xlsx");
    }

    @Test
    public void testTeamResultsPostProcessHidesUnusedMeasureColumnPerTab() throws Exception {
        Championship senior = ChampionshipRepository.findByName("Senior");
        assertNotNull("Senior championship should be loaded from fixture", senior);

        try (Workbook workbook = createGeneratedTeamResultsWorkbook(senior, null)) {
            assertTrue("mixed score-based tab should hide points column E",
                workbook.getSheet("Mixed").isColumnHidden(4));
            assertFalse("mixed score-based tab should keep score column G visible",
                workbook.getSheet("Mixed").isColumnHidden(6));

            assertFalse("men's points-based tab should keep points column E visible",
                workbook.getSheet("Men").isColumnHidden(4));
            assertTrue("men's points-based tab should hide score column G",
                workbook.getSheet("Men").isColumnHidden(6));
        }
    }

            @Test
            public void testSeniorExplicitMixedSubsetUsesTop3GenderNeutral() {
            Championship senior = ChampionshipRepository.findByName("Senior");
            assertNotNull("Senior championship should be loaded from fixture", senior);

            Map<String, List<Participation>> explicitSubsetByTeam = selectMixedTeamMembers(senior, 3, 3);
            printSelectedAthletes("senior explicit mixed subset top 3 gender neutral | explicit roster",
                explicitSubsetByTeam);
            applyMixedTeamMemberships(senior, explicitSubsetByTeam);
            assertPersistedExplicitMixedRoster("senior explicit mixed subset top 3 gender neutral",
                senior, explicitSubsetByTeam, 3, 3);
            configureMixedTeamRules(senior, true, null, Ranking.GAMX, 3, 2, 2);
            assertPersistedMixedTeamRules("senior explicit mixed subset top 3 gender neutral",
                senior, true, null, Ranking.GAMX, 3, 2, 2);

            Map<String, List<Participation>> expectedCountedByTeam = computeExpectedMixedCountedByTeam(explicitSubsetByTeam,
                Ranking.GAMX, 3, 2, 2);
            printSelectedAthletes("senior explicit mixed subset top 3 gender neutral | expected counted",
                expectedCountedByTeam);

            List<TeamTreeItem> teams = computeTeamResults(senior, Gender.MF);
            assertMixedTeamScoresMatchExpectedSelection(teams, explicitSubsetByTeam, expectedCountedByTeam,
                "senior explicit mixed subset top 3 gender neutral");
            assertMixedReportingBeanMatchesTree("senior explicit mixed subset top 3 gender neutral",
                senior, teams, explicitSubsetByTeam, expectedCountedByTeam);
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

        try (Connection target = DriverManager.getConnection(memoryJdbcUrl, "sa", "");
                Statement targetStatement = target.createStatement()) {
            targetStatement.execute("RUNSCRIPT FROM '" + escapedScriptFile + "'");
        }
    }

    private static String createMemoryJdbcUrl() {
        return "jdbc:h2:mem:championshipTest-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
    }

    private static void initFixtureMixedTeamEnabled() {
        JPAService.runInTransaction(em -> {
            for (Championship c : em.createQuery("select c from Championship c", Championship.class).getResultList()) {
                // Senior and Open have mixed teams; others do not
                boolean mixed = "Senior".equals(c.getName()) || "Open".equals(c.getName());
                c.setMixedTeamEnabled(mixed);
            }
            return null;
        });
        Championship.reset();
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

    private static Map<Gender, Set<String>> computeTeamSelectionRootNamesByGender(Championship championship, Gender gender) {
        return computeTeamSelectionRoots(championship, gender).stream()
                .filter(root -> root.getGender() != null)
                .collect(Collectors.groupingBy(TeamTreeItem::getGender,
                        () -> new EnumMap<>(Gender.class),
                        Collectors.mapping(TeamTreeItem::getName, Collectors.toSet())));
    }

    private static Set<String> computeTeamSelectionRootNames(Championship championship, Gender gender) {
        return computeTeamSelectionRoots(championship, gender).stream()
                .map(TeamTreeItem::getName)
                .collect(Collectors.toSet());
    }

    private static List<TeamTreeItem> computeTeamSelectionRoots(Championship championship, Gender gender) {
        TeamSelectionTreeData teamSelection = new TeamSelectionTreeData(null, championship, gender,
                Ranking.SNATCH_CJ_TOTAL, false);
        return new ArrayList<>(teamSelection.getRootItems());
    }

    private static void invokeSetReportingInfo(JXLSTeamResultsSheet sheet) throws Exception {
        Method method = JXLSTeamResultsSheet.class.getDeclaredMethod("setReportingInfo");
        method.setAccessible(true);
        method.invoke(sheet);
    }

    private static void assertTeamResultsTemplateUsesCountedMembers(String templateResource) throws Exception {
        try (InputStream templateStream = ChampionshipTest.class.getResourceAsStream(templateResource)) {
            assertNotNull("missing Team Results template: " + templateResource, templateStream);
            try (Workbook workbook = WorkbookFactory.create(templateStream)) {
                String statusValue = workbook.getSheet("Mixed")
                    .getRow(2)
                    .getCell(5)
                    .getStringCellValue();
                assertEquals("mixed status denominator should use configured team size for " + templateResource,
                    "${team.counted}/${mwTeamSize != 0 ? mwTeamSize : team.size}",
                    statusValue);

                String mixedMemberLoop = workbook.getSheet("Mixed")
                    .getRow(4)
                    .getCell(0)
                    .getCellComment()
                    .getString()
                    .getString();
                assertEquals("mixed member loop should use counted team members for " + templateResource,
                    "jx:each(items=\"team.countedTeamMembers\" var=\"member\" lastCell=\"G5\")",
                    mixedMemberLoop);
            }
        }
    }

    private static Workbook createGeneratedTeamResultsWorkbook(Championship championship, Gender gender) throws Exception {
        JXLSTeamResultsSheet sheet = new JXLSTeamResultsSheet(null);
        sheet.setTemplateFileName("/templates/teamResults/TeamResults-A4.xlsx");
        sheet.setChampionship(championship);
        sheet.setGender(gender);
        try (InputStream generatedStream = sheet.createInputStream()) {
            return WorkbookFactory.create(generatedStream);
        }
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

            private static void assertMixedTeamScoresMatchExpectedSelection(List<TeamTreeItem> teams,
                Map<String, List<Participation>> contributingPoolByTeam,
                Map<String, List<Participation>> expectedCountedByTeam,
                String label) {
            assertTrue(label + " should produce mixed teams", !teams.isEmpty());

            for (TeamTreeItem team : teams) {
                List<Participation> contributingPool = contributingPoolByTeam.getOrDefault(team.getName(), List.of());
                List<Participation> expectedCounted = expectedCountedByTeam.getOrDefault(team.getName(), List.of());

                assertEquals(label + " iterated athletes for team " + team.getName(), contributingPool.size(),
                    team.getTeamMembers().size());
                assertEquals(label + " counted athletes for team " + team.getName(), expectedCounted.size(),
                    team.getCounted().intValue());

                List<Long> expectedPoolIds = contributingPool.stream()
                    .map(participation -> participation.getAthlete().getId())
                    .collect(Collectors.toList());
                List<Long> actualPoolIds = team.getTeamMembers().stream()
                    .map(item -> item.getAthlete().getId())
                    .collect(Collectors.toList());
                assertEquals(label + " iterated athlete ids for team " + team.getName(),
                    expectedPoolIds.stream().collect(Collectors.toSet()),
                    actualPoolIds.stream().collect(Collectors.toSet()));

                double expectedScore = expectedCounted.stream()
                    .mapToDouble(participation -> getDirectGamxScore(participation))
                    .sum();
                logger.info("{} | team={} | expectedPoolIds={} | actualPoolIds={} | pool={} | counted={} | expectedSum={} | actualSum={}",
                    label,
                    team.getName(),
                    expectedPoolIds,
                    actualPoolIds,
                    describeParticipations(contributingPool),
                    describeParticipations(expectedCounted),
                    String.format(Locale.ROOT, "%.2f", expectedScore),
                    String.format(Locale.ROOT, "%.2f", team.getScore()));
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

    private static String describeParticipations(List<Participation> participations) {
        return participations.isEmpty()
                ? "none"
                : participations.stream().map(ChampionshipTest::describeAthlete).collect(Collectors.joining(", "));
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

    private static Map<String, ChampionshipConfigSnapshot> snapshotChampionshipConfigs() {
        return ChampionshipRepository.findAll().stream()
                .collect(Collectors.toMap(Championship::getName, ChampionshipConfigSnapshot::new));
    }

    private static void restoreChampionshipConfigs(Map<String, ChampionshipConfigSnapshot> configs) {
        if (configs == null) {
            return;
        }
        JPAService.runInTransaction(em -> {
            List<Championship> championships = em.createQuery("select c from Championship c", Championship.class)
                    .getResultList();
            for (Championship championship : championships) {
                ChampionshipConfigSnapshot snapshot = configs.get(championship.getName());
                if (snapshot == null) {
                    continue;
                }
                championship.setExplicitMixedTeamMembers(snapshot.explicitMixedTeamMembers);
                championship.setMixedTeamEnabled(snapshot.mixedTeamEnabled);
                championship.setExplicitTeamSize(snapshot.explicitTeamSize);
                championship.setTeamScoringSystem(snapshot.teamScoringSystem);
                championship.setMixedTeamScoringSystem(snapshot.mixedTeamScoringSystem);
                championship.setMensBestN(snapshot.mensBestN);
                championship.setWomensBestN(snapshot.womensBestN);
                championship.setMixedBestN(snapshot.mixedBestN);
                championship.setMixedMensBestN(snapshot.mixedMensBestN);
                championship.setMixedWomensBestN(snapshot.mixedWomensBestN);
            }
            return null;
        });
    }

    private static boolean setGroupDone(Long groupId, boolean done) {
        return JPAService.runInTransaction(em -> {
            Group group = em.find(Group.class, groupId);
            assertNotNull("group should exist for id " + groupId, group);
            boolean previousDone = group.isDone();
            group.setDone(done);
            return previousDone;
        });
    }

    private static void configureMixedTeamRules(Championship championship, boolean explicitMixedTeamMembers,
            Ranking teamScoringSystem, Ranking mixedTeamScoringSystem, Integer mixedBestN,
            Integer mixedMensBestN, Integer mixedWomensBestN) {
        championship.setExplicitMixedTeamMembers(explicitMixedTeamMembers);
        championship.setTeamScoringSystem(teamScoringSystem);
        championship.setMixedTeamScoringSystem(mixedTeamScoringSystem);
        championship.setMixedBestN(mixedBestN);
        championship.setMixedMensBestN(mixedMensBestN);
        championship.setMixedWomensBestN(mixedWomensBestN);

        JPAService.runInTransaction(em -> {
            Championship managed = em.find(Championship.class, championship.getId());
            managed.setExplicitMixedTeamMembers(explicitMixedTeamMembers);
            managed.setTeamScoringSystem(teamScoringSystem);
            managed.setMixedTeamScoringSystem(mixedTeamScoringSystem);
            managed.setMixedBestN(mixedBestN);
            managed.setMixedMensBestN(mixedMensBestN);
            managed.setMixedWomensBestN(mixedWomensBestN);
            return null;
        });
    }

            private static void assertPersistedMixedTeamRules(String label, Championship championship,
                boolean explicitMixedTeamMembers, Ranking teamScoringSystem, Ranking mixedTeamScoringSystem,
                Integer mixedBestN, Integer mixedMensBestN, Integer mixedWomensBestN) {
            Championship persisted = ChampionshipRepository.findByName(championship.getName());
            assertNotNull(label + " persisted championship", persisted);
            assertEquals(label + " explicitMixedTeamMembers", explicitMixedTeamMembers,
                persisted.isExplicitMixedTeamMembers());
            assertEquals(label + " teamScoringSystem", teamScoringSystem, persisted.getTeamScoringSystem());
            assertEquals(label + " mixedTeamScoringSystem", mixedTeamScoringSystem, persisted.getMixedTeamScoringSystem());
            assertEquals(label + " mixedBestN", mixedBestN, persisted.getMixedBestN());
            assertEquals(label + " mixedMensBestN", mixedMensBestN, persisted.getMixedMensBestN());
            assertEquals(label + " mixedWomensBestN", mixedWomensBestN, persisted.getMixedWomensBestN());
            logger.info("{} | persisted settings explicit={} teamScoring={} mixedScoring={} mixedBestN={} mixedMensBestN={} mixedWomensBestN={}",
                label,
                persisted.isExplicitMixedTeamMembers(),
                persisted.getTeamScoringSystem(),
                persisted.getMixedTeamScoringSystem(),
                persisted.getMixedBestN(),
                persisted.getMixedMensBestN(),
                persisted.getMixedWomensBestN());
            }

            private static void assertPersistedExplicitMixedRoster(String label, Championship championship,
                Map<String, List<Participation>> selectedByTeam, int expectedFemaleCount, int expectedMaleCount) {
            Map<String, List<Participation>> actualByTeam = getChampionshipParticipations(championship).stream()
                .filter(Participation::getMixedTeamMember)
                .collect(Collectors.groupingBy(participation -> participation.getAthlete().getTeam(),
                    LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<Participation>> entry : selectedByTeam.entrySet()) {
                String teamName = entry.getKey();
                List<Participation> expected = entry.getValue().stream()
                    .sorted(participationThenNameComparator())
                    .collect(Collectors.toList());
                List<Participation> actual = actualByTeam.getOrDefault(teamName, List.of()).stream()
                    .sorted(participationThenNameComparator())
                    .collect(Collectors.toList());

                assertEquals(label + " persisted explicit roster size for " + teamName,
                    expectedFemaleCount + expectedMaleCount, actual.size());
                assertEquals(label + " persisted female count for " + teamName,
                    expectedFemaleCount, actual.stream().filter(p -> p.getAthlete().getGender() == Gender.F).count());
                assertEquals(label + " persisted male count for " + teamName,
                    expectedMaleCount, actual.stream().filter(p -> p.getAthlete().getGender() == Gender.M).count());
                assertEquals(label + " persisted roster ids for " + teamName,
                    expected.stream().map(Participation::getId).collect(Collectors.toList()),
                    actual.stream().map(Participation::getId).collect(Collectors.toList()));
                logger.info("{} | persisted explicit roster | team={} | athletes={}",
                    label, teamName, describeParticipations(actual));
            }
            }

            private static void assertMixedReportingBeanMatchesTree(String label, Championship championship,
                List<TeamTreeItem> teams, Map<String, List<Participation>> explicitRosterByTeam,
                Map<String, List<Participation>> expectedCountedByTeam) {
            HashMap<String, Object> beans = Competition.getCurrent().computeReportingInfo(null, championship);
            String beanKey = "mwTeam" + championship.getName();

            @SuppressWarnings("unchecked")
            List<Athlete> mwTeam = (List<Athlete>) beans.get(beanKey);
            assertNotNull(label + " | " + beanKey + " bean should exist", mwTeam);

            for (Athlete a : mwTeam) {
                assertTrue(label + " | " + beanKey + " athlete should be PAthlete: " + a.getFullName(),
                    a instanceof PAthlete);
            }

            if (championship.isExplicitMixedTeamMembers() && explicitRosterByTeam != null) {
                // Explicit: every bean athlete must come from the explicit roster
                Set<Long> rosterIds = explicitRosterByTeam.values().stream()
                    .flatMap(List::stream)
                    .map(p -> p.getAthlete().getId())
                    .collect(Collectors.toSet());
                for (Athlete a : mwTeam) {
                    assertTrue(label + " | " + beanKey + " athlete should be in explicit roster: " + a.getFullName(),
                        rosterIds.contains(a.getId()));
                }
                // Bean contains full roster; verify counted members in tree match expected topN
                if (expectedCountedByTeam != null) {
                    Set<Long> expectedCountedIds = expectedCountedByTeam.values().stream()
                        .flatMap(List::stream)
                        .map(p -> p.getAthlete().getId())
                        .collect(Collectors.toSet());
                    Set<Long> treeCountedIds = teams.stream()
                        .flatMap(t -> t.getTeamMembers().stream())
                        .filter(TeamTreeItem::isCountedForTeam)
                        .map(m -> m.getAthlete().getId())
                        .collect(Collectors.toSet());
                    assertEquals(label + " | tree counted members should match expected topN subset",
                        expectedCountedIds, treeCountedIds);
                }
            } else {
                // Implicit: bean should be union of mTeam + wTeam
                @SuppressWarnings("unchecked")
                List<Athlete> mTeam = (List<Athlete>) beans.get("mTeam" + championship.getName());
                @SuppressWarnings("unchecked")
                List<Athlete> wTeam = (List<Athlete>) beans.get("wTeam" + championship.getName());
                if (mTeam != null && wTeam != null) {
                    Set<Long> mwIds = mwTeam.stream().map(Athlete::getId).collect(Collectors.toSet());
                    Set<Long> mPlusW = new java.util.HashSet<>();
                    mPlusW.addAll(mTeam.stream().map(Athlete::getId).collect(Collectors.toSet()));
                    mPlusW.addAll(wTeam.stream().map(Athlete::getId).collect(Collectors.toSet()));
                    assertEquals(label + " | " + beanKey + " should be union of mTeam + wTeam", mPlusW, mwIds);
                }
                // Implicit: verify counted members in tree match expected topN
                if (expectedCountedByTeam != null) {
                    Set<Long> expectedCountedIds = expectedCountedByTeam.values().stream()
                        .flatMap(List::stream)
                        .map(p -> p.getAthlete().getId())
                        .collect(Collectors.toSet());
                    Set<Long> treeCountedIds = teams.stream()
                        .flatMap(t -> t.getTeamMembers().stream())
                        .filter(TeamTreeItem::isCountedForTeam)
                        .map(m -> m.getAthlete().getId())
                        .collect(Collectors.toSet());
                    assertEquals(label + " | tree counted members should match expected topN subset",
                        expectedCountedIds, treeCountedIds);
                }
            }

            // Per-team: verify bean athletes include tree's iterated athletes
            Map<String, Set<Long>> beanIdsByTeam = mwTeam.stream()
                .filter(a -> a.getTeam() != null)
                .collect(Collectors.groupingBy(Athlete::getTeam,
                    Collectors.mapping(Athlete::getId, Collectors.toSet())));
            for (TeamTreeItem team : teams) {
                Set<Long> beanIds = beanIdsByTeam.getOrDefault(team.getName(), Set.of());
                for (TeamTreeItem member : team.getTeamMembers()) {
                    assertTrue(label + " | " + beanKey + " should contain iterated athlete "
                        + member.getAthlete().getFullName() + " for team " + team.getName(),
                        beanIds.contains(member.getAthlete().getId()));
                }
                logger.info("{} | {} bean for {} : beanCount={}, treeSize={}, treeCounted={}",
                    label, beanKey, team.getName(), beanIds.size(), team.getSize(), team.getCounted());
            }
            }

    private static Map<String, List<Participation>> computeExpectedMixedCountedByTeam(
            Map<String, List<Participation>> contributingPoolByTeam,
            Ranking ranking,
            Integer mixedBestN,
            Integer mixedMensBestN,
            Integer mixedWomensBestN) {
        LinkedHashMap<String, List<Participation>> expectedByTeam = new LinkedHashMap<>();

        for (Map.Entry<String, List<Participation>> entry : contributingPoolByTeam.entrySet()) {
            List<Participation> sortedPool = sortParticipationsByMixedRanking(entry.getValue(), ranking);
            List<Participation> counted = new ArrayList<>();

            Integer overallCap = positiveCap(mixedBestN);
            if (overallCap != null) {
                counted.addAll(sortedPool.stream().limit(overallCap).collect(Collectors.toList()));
            } else {
                int menCount = 0;
                int womenCount = 0;
                Integer menCap = positiveCapOrUnlimited(mixedMensBestN);
                Integer womenCap = positiveCapOrUnlimited(mixedWomensBestN);
                for (Participation participation : sortedPool) {
                    Gender gender = participation.getAthlete().getGender();
                    if (gender == Gender.M && menCount < menCap) {
                        counted.add(participation);
                        menCount++;
                    } else if (gender == Gender.F && womenCount < womenCap) {
                        counted.add(participation);
                        womenCount++;
                    }
                }
            }

            expectedByTeam.put(entry.getKey(), counted);
            entry.setValue(sortedPool);
        }

        return expectedByTeam;
    }

    private static List<Participation> sortParticipationsByMixedRanking(List<Participation> participations, Ranking ranking) {
        Map<Long, Participation> participationByAthleteId = participations.stream()
                .filter(participation -> participation.getAthlete() != null && participation.getAthlete().getId() != null)
                .collect(Collectors.toMap(participation -> participation.getAthlete().getId(), participation -> participation,
                        (left, right) -> left, LinkedHashMap::new));

        List<Athlete> sortedAthletes = app.owlcms.data.athleteSort.AthleteSorter.teamPointsOrderCopyMixed(
                participations.stream().map(Participation::getAthlete).collect(Collectors.toList()), ranking);

        return sortedAthletes.stream()
                .map(athlete -> participationByAthleteId.get(athlete.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Integer positiveCap(Integer cap) {
        return cap != null && cap > 0 ? cap : null;
    }

    private static void assertAllChampionshipGroupsDone(Championship championship) {
        List<String> unfinishedGroups = getChampionshipParticipations(championship).stream()
                .map(Participation::getAthlete)
                .filter(Objects::nonNull)
                .map(Athlete::getGroup)
                .filter(Objects::nonNull)
                .distinct()
                .filter(group -> !group.isDone())
                .map(Group::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        assertTrue("fixture groups for " + championship.getName() + " should all start done: " + unfinishedGroups,
                unfinishedGroups.isEmpty());
    }

    private static Integer positiveCapOrUnlimited(Integer cap) {
        Integer positiveCap = positiveCap(cap);
        return positiveCap != null ? positiveCap : Integer.MAX_VALUE;
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