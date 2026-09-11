package app.owlcms.tests.migration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.BirthDateTextMigration;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.UtcNormalizationMigration;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.tests.TestData;

/**
 * One-shot conversion tool: load a V1 JSON export into a scratch file-based H2 database,
 * simulate an application restart, and re-export in V2 format.
 *
 * <pre>
 * mvn -pl owlcms -Dtest=JsonV1ToV2ConversionTest -Dowlcms.v1Json=/path/2026-06 meet/databaseExports/in.json \
 *     -Dowlcms.conversionDir=/Users/jflamy/git/meets/conversion test
 * </pre>
 *
 * A folder named after the source competition directory is created under the conversion
 * directory; it receives a copy of the source, the V2 output, and the scratch database.
 * Generic parent folder names (databaseExports, exports, json) are skipped when inferring the name.
 * Without system properties, the V66 classpath fixture is converted into target/test-temp.
 */
public class JsonV1ToV2ConversionTest {

    private static final String DEFAULT_FIXTURE = "/testDatabases/66_5_Database_2026-06-24_20h12.json";
    private static final String DEFAULT_CONVERSION_DIR = "/Users/jflamy/git/meets/conversion";
    private static final Set<String> GENERIC_FOLDER_NAMES = Set.of("databaseexports", "exports", "json", "database", "databases");

    private Path workDirectory;
    private String jdbcUrl;

    @After
    public void tearDown() throws Exception {
        clearChampionshipCache();
        Competition.setCurrent(null);
        JPAService.close();
        System.clearProperty("JDBC_DATABASE_URL");
    }

    @Test
    public void convertV1ToV2() throws Exception {
        String v1Path = System.getProperty("owlcms.v1Json");
        String v2Path = System.getProperty("owlcms.v2Json");

        String baseName;
        if (v1Path != null && !v1Path.isBlank()) {
            Path source = Path.of(v1Path).toAbsolutePath();
            baseName = source.getFileName().toString();
            String conversionDir = System.getProperty("owlcms.conversionDir", DEFAULT_CONVERSION_DIR);
            workDirectory = Path.of(conversionDir).resolve(inferCompetitionName(source));
            Files.createDirectories(workDirectory);
            Files.copy(source, workDirectory.resolve(baseName), StandardCopyOption.REPLACE_EXISTING);
        } else {
            baseName = DEFAULT_FIXTURE.substring(DEFAULT_FIXTURE.lastIndexOf('/') + 1);
            Path testTempDirectory = Path.of("target", "test-temp");
            Files.createDirectories(testTempDirectory);
            workDirectory = Files.createTempDirectory(testTempDirectory, "v1-to-v2-");
        }

        Path output = (v2Path != null && !v2Path.isBlank())
                ? Path.of(v2Path)
                : workDirectory.resolve(baseName.replaceAll("\\.json$", "") + "_v2.json");

        // fresh empty file-based database
        Main.injectSuppliers();
        JPAService.close();
        Competition.setCurrent(null);
        Files.deleteIfExists(workDirectory.resolve("owlcms.mv.db"));
        Files.deleteIfExists(workDirectory.resolve("owlcms.trace.db"));
        jdbcUrl = "jdbc:h2:file:" + workDirectory.resolve("owlcms").toAbsolutePath();
        System.setProperty("JDBC_DATABASE_URL", jdbcUrl);
        JPAService.init(false, true);
        Config.initConfig();
        TestData.insertInitialData(1, true);
        clearChampionshipCache();
        Competition.setCurrent(null);

        // import V1
        try (InputStream in = openInput(v1Path)) {
            FormatDetector.importData(in);
        }
        clearChampionshipCache();
        Competition.setCurrent(null);

        // simulate restart: close and reopen the same database, run startup migrations
        JPAService.close();
        JPAService.init(false, false);
        Config.initConfig();
        JPAService.runInTransaction(em -> {
            BirthDateTextMigration.migrate(em);
            UtcNormalizationMigration.normalizeAllToUtc(em);
            return null;
        });
        Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
        CategoryRepository.fixCategories();
        AthleteRepository.removeBrokenParticipationsAndCategories();
        ChampionshipRepository.reconcileFromAgeGroups();
        clearChampionshipCache();
        AgeGroupRepository.validateCategoriesConsistency();
        PlatformRepository.checkPlatforms();
        Competition.recomputeAllAthleteRanks();

        // export V2
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (InputStream exported = new CompetitionDataV2().fromDatabase().exportData()) {
            Files.copy(exported, output, StandardCopyOption.REPLACE_EXISTING);
        }

        assertTrue("V2 export should not be empty", Files.size(output) > 0);
        System.out.println("V2 export written to " + output.toAbsolutePath());
    }

    private String inferCompetitionName(Path source) {
        Path dir = source.getParent();
        while (dir != null && dir.getFileName() != null
                && GENERIC_FOLDER_NAMES.contains(dir.getFileName().toString().toLowerCase(Locale.ROOT))) {
            dir = dir.getParent();
        }
        if (dir == null || dir.getFileName() == null) {
            return source.getFileName().toString().replaceAll("\\.json$", "");
        }
        return dir.getFileName().toString();
    }

    private InputStream openInput(String v1Path) throws Exception {
        if (v1Path != null && !v1Path.isBlank()) {
            return new ByteArrayInputStream(Files.readAllBytes(Path.of(v1Path)));
        }
        try (InputStream in = JsonV1ToV2ConversionTest.class.getResourceAsStream(DEFAULT_FIXTURE)) {
            assertNotNull("JSON fixture not found: " + DEFAULT_FIXTURE, in);
            return new ByteArrayInputStream(in.readAllBytes());
        }
    }

    private void clearChampionshipCache() throws ReflectiveOperationException {
        Field cache = Championship.class.getDeclaredField("allChampionshipsMap");
        cache.setAccessible(true);
        cache.set(null, null);
    }
}
