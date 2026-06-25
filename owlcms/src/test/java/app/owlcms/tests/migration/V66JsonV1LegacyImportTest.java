package app.owlcms.tests.migration;

import org.junit.Test;

public class V66JsonV1LegacyImportTest extends ChampionshipLegacyMigrationSupport {

    @Test
    public void importCompletes() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V66_JSON);
    }

    @Test
    public void importCreatesCompetitionTemplate() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V66_JSON);

        assertCompetitionTemplateExists();
    }

    @Test
    public void importMarksPersistedCompetitionMigrated() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V66_JSON);

        assertPersistedCompetitionMigrated();
    }

    @Test
    public void importMarksCurrentCompetitionMigrated() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V66_JSON);

        assertCurrentCompetitionMigrated();
    }

    @Test
    public void importReconcilesExpectedChampionships() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V66_JSON);

        assertMigratedChampionships();
    }

    @Test
    public void importRemovesIwfRows() throws Exception {
        initializeImportDatabase();

        importJsonFixture(V66_JSON);

        assertNoIwfRowsRemain();
    }
}