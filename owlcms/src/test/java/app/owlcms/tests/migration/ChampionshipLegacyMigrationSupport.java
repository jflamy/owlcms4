package app.owlcms.tests.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.After;

import app.owlcms.Main;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.TestData;

abstract class ChampionshipLegacyMigrationSupport {

    protected static final String V65_DATABASE = "/testDatabases/v65-h2v2.mv.db";
    protected static final String V66_DATABASE = "/testDatabases/v66-h2v2.mv.db";
    protected static final String V65_JSON = "/testDatabases/65_1_Database_2026-06-24_20h09.json";
    protected static final String V66_JSON = "/testDatabases/66_5_Database_2026-06-24_20h12.json";

    private static final Set<String> EXPECTED_CHAMPIONSHIPS = Set.of(
            "Junior", "Masters", "O21", "Open", "Score", "Senior",
            "U11", "U13", "U15", "U17", "U20", "Youth");

    private Path fixtureDirectory;
    private String memoryJdbcUrl;

    @After
    public void tearDown() throws Exception {
        clearChampionshipCacheOnly();
        Competition.setCurrent(null);
        JPAService.close();
        memoryJdbcUrl = null;
        System.clearProperty("JDBC_DATABASE_URL");
        deleteFixtureDirectory();
    }

    protected void loadFixtureIntoMemoryDatabase(String fixtureResource, String copiedFileName)
            throws IOException, ReflectiveOperationException, SQLException {
        Main.injectSuppliers();
        JPAService.close();
        Competition.setCurrent(null);
        memoryJdbcUrl = createMemoryJdbcUrl();
        System.setProperty("JDBC_DATABASE_URL", memoryJdbcUrl);
        Path testTempDirectory = Path.of("target", "test-temp");
        Files.createDirectories(testTempDirectory);
        fixtureDirectory = Files.createTempDirectory(testTempDirectory, "championship-migration-test-db-");
        Path copiedDatabase = fixtureDirectory.resolve(copiedFileName);
        Path scriptFile = fixtureDirectory.resolve(copiedFileName.replaceAll("\\.mv\\.db$", ".sql"));

        try (InputStream fixtureStream = ChampionshipLegacyMigrationSupport.class.getResourceAsStream(fixtureResource)) {
            assertNotNull("Fixture database not found: " + fixtureResource, fixtureStream);
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

        JPAService.init(true, false);
        clearChampionshipCacheOnly();
        Competition.setCurrent(null);
    }

    protected void initializeImportDatabase() throws ReflectiveOperationException {
        Main.injectSuppliers();
        JPAService.close();
        Competition.setCurrent(null);
        memoryJdbcUrl = createMemoryJdbcUrl();
        System.setProperty("JDBC_DATABASE_URL", memoryJdbcUrl);

        JPAService.init(true, true);
        Config.initConfig();
        TestData.insertInitialData(1, true);
        clearChampionshipCacheOnly();
        Competition.setCurrent(null);
    }

    protected void importJsonFixture(String fixtureResource) throws Exception {
        try (InputStream fixtureStream = ChampionshipLegacyMigrationSupport.class.getResourceAsStream(fixtureResource)) {
            assertNotNull("JSON fixture not found: " + fixtureResource, fixtureStream);
            FormatDetector.importData(fixtureStream);
        }
        clearChampionshipCacheOnly();
    }

    protected void assertReadableAgeGroups() {
        @SuppressWarnings("unchecked")
        List<Object[]> ageGroups = JPAService.runInTransaction(em -> em.createQuery(
                "select ag.championshipName, ag.championshipType from AgeGroup ag")
                .getResultList());
        assertFalse("legacy fixture should contain age groups", ageGroups.isEmpty());

        assertTrue("Masters age groups should be readable as MASTERS",
                ageGroups.stream().anyMatch(row -> "Masters".equals(row[0])
                        && row[1] == ChampionshipType.MASTERS));
        assertTrue("Youth age groups should still be readable before IWF cleanup",
                ageGroups.stream().anyMatch(row -> "Youth".equals(row[0])
                        && row[1] == ChampionshipType.IWF));
        assertTrue("Open age group should be readable as DEFAULT",
                ageGroups.stream().anyMatch(row -> "Open".equals(row[0])
                        && row[1] == ChampionshipType.DEFAULT));
    }

    protected void reconcileTwice() {
        Config.initConfig();
        Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
        ChampionshipRepository.reconcileFromAgeGroups();
        Championship.reset();
        ChampionshipRepository.reconcileFromAgeGroups();
        Championship.reset();
    }

    protected void assertMigratedChampionshipState() {
        assertMigratedCompetitionTemplate();
        assertMigratedChampionships();
        assertNoIwfRowsRemain();
    }

    protected void assertMigratedCompetitionTemplate() {
        assertCompetitionTemplateExists();
        assertCompetitionMigrated();
    }

    protected void assertCompetitionTemplateExists() {
        Championship template = ChampionshipRepository.ensureCompetitionTemplate();

        assertNotNull("competition template should exist", template);
        assertTrue("competition template should be marked as template", template.isCompetitionTemplate());
    }

    protected void assertCompetitionMigrated() {
        assertPersistedCompetitionMigrated();
        assertCurrentCompetitionMigrated();
    }

    protected void assertPersistedCompetitionMigrated() {
        Competition competition = JPAService.runInTransaction(em -> em
                .createQuery("select c from Competition c", Competition.class)
                .getResultList().stream().findFirst().orElse(null));

        assertNotNull("persisted competition should be loaded", competition);
        assertTrue("persisted competition should be marked migrated", competition.isMigrated());
    }

    protected void assertCurrentCompetitionMigrated() {
        Competition competition = Competition.getCurrent();

        assertNotNull("competition should be loaded", competition);
        assertTrue("current competition should be marked migrated", competition.isMigrated());
    }

    protected void assertMigratedChampionships() {
        List<Championship> championships = ChampionshipRepository.findAll();
        Map<String, Championship> byName = championships.stream()
                .collect(Collectors.toMap(Championship::getName, Function.identity()));

        assertEquals("stored non-template championship names", EXPECTED_CHAMPIONSHIPS, byName.keySet());
        assertEquals("stored non-template championship count", EXPECTED_CHAMPIONSHIPS.size(), championships.size());

        assertChampionshipType(byName, "Masters", ChampionshipType.MASTERS);
        assertChampionshipType(byName, "Youth", ChampionshipType.U);
        assertChampionshipType(byName, "Junior", ChampionshipType.U);
        assertChampionshipType(byName, "Senior", ChampionshipType.U);
        assertChampionshipType(byName, "Open", ChampionshipType.DEFAULT);

        for (String name : List.of("U11", "U13", "U15", "U17", "U20", "O21", "Score")) {
            assertChampionshipType(byName, name, ChampionshipType.U);
        }
    }

    protected void assertNoIwfRowsRemain() {
        assertEquals("IWF championship rows should be migrated", 0L, countIwfChampionships());
        assertEquals("IWF age-group championship types should be migrated", 0L, countIwfAgeGroupTypes());
        assertEquals("IWF age divisions should be migrated", 0L, countIwfAgeDivisions());
    }

    protected long countChampionships(boolean includeTemplate) {
        return JPAService.runInTransaction(em -> em.createQuery(
                includeTemplate
                        ? "select count(c) from Championship c"
                        : "select count(c) from Championship c where c.competitionTemplate = false",
                Long.class).getSingleResult());
    }

    private void assertChampionshipType(Map<String, Championship> byName, String name, ChampionshipType expected) {
        Championship championship = byName.get(name);
        assertNotNull(name + " championship should exist", championship);
        assertEquals(name + " championship type", expected, championship.getType());
    }

    private long countIwfChampionships() {
        return JPAService.runInTransaction(em -> em.createQuery(
                "select count(c) from Championship c where c.type = :iwf", Long.class)
                .setParameter("iwf", ChampionshipType.IWF)
                .getSingleResult());
    }

    private long countIwfAgeGroupTypes() {
        return JPAService.runInTransaction(em -> em.createQuery(
                "select count(ag) from AgeGroup ag where ag.championshipType = :iwf", Long.class)
                .setParameter("iwf", ChampionshipType.IWF)
                .getSingleResult());
    }

    private long countIwfAgeDivisions() {
        return JPAService.runInTransaction(em -> em.createQuery(
                "select count(ag) from AgeGroup ag where lower(ag.ageDivision) = :iwf", Long.class)
                .setParameter("iwf", ChampionshipType.IWF.name().toLowerCase())
                .getSingleResult());
    }

    private String createMemoryJdbcUrl() {
        return "jdbc:h2:mem:championshipLegacyMigrationTest-" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
    }

    private String escapePath(Path path) {
        return path.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "''");
    }

    private void clearChampionshipCacheOnly() throws ReflectiveOperationException {
        Field cache = Championship.class.getDeclaredField("allChampionshipsMap");
        cache.setAccessible(true);
        cache.set(null, null);
    }

    private void deleteFixtureDirectory() throws IOException {
        if (fixtureDirectory == null || !Files.exists(fixtureDirectory)) {
            return;
        }

        try (var paths = Files.walk(fixtureDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
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