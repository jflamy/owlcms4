package app.owlcms.tests.migration;

import org.junit.Test;

public class V65JsonV1LegacyImportTest extends ChampionshipLegacyMigrationSupport {

    @Test
    public void importCompletes() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V65_JSON);
    }

    @Test
    public void importCreatesCompetitionTemplate() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V65_JSON);

        assertCompetitionTemplateExists();
    }

    @Test
    public void importMarksPersistedCompetitionMigrated() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V65_JSON);

        assertPersistedCompetitionMigrated();
    }

    @Test
    public void importMarksCurrentCompetitionMigrated() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V65_JSON);

        assertCurrentCompetitionMigrated();
    }

    @Test
    public void importMaterializesExpectedChampionships() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V65_JSON);

        assertMigratedChampionships();
    }

    @Test
    public void importRemovesIwfRows() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V65_JSON);

        assertNoIwfRowsRemain();
    }
}