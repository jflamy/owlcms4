package app.owlcms.tests.migration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChampionshipV66MigrationTest extends ChampionshipLegacyMigrationSupport {

    @Test
    public void migratesPersistedChampionshipsWithoutDuplicates() throws Exception {
        loadFixtureIntoMemoryDatabase(V66_DATABASE, "v66-h2v2.mv.db");

        assertReadableAgeGroups();
        assertEquals("v66 should start with its 12 persisted championships", 12L, countChampionships(false));

        reconcileTwice();

        assertMigratedChampionshipState();
    }
}