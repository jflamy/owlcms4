package app.owlcms.tests.migration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChampionshipV65MigrationTest extends ChampionshipLegacyMigrationSupport {

    @Test
    public void jpaInitializes() throws Exception {
        loadFixtureIntoMemoryDatabase(V65_DATABASE, "v65-h2v2.mv.db");
    }

    @Test
    public void ageGroupsAreReadableBeforeReconcile() throws Exception {
        loadFixtureIntoMemoryDatabase(V65_DATABASE, "v65-h2v2.mv.db");

        assertReadableAgeGroups();
    }

    @Test
    public void schemaUpdateCreatesEmptyChampionshipTable() throws Exception {
        loadFixtureIntoMemoryDatabase(V65_DATABASE, "v65-h2v2.mv.db");

        assertEquals("v65 should start without stored championships", 0L, countChampionships(false));
    }

    @Test
    public void reconcileMigratesCompetitionTemplate() throws Exception {
        loadFixtureIntoMemoryDatabase(V65_DATABASE, "v65-h2v2.mv.db");

        reconcileTwice();

        assertMigratedCompetitionTemplate();
    }

    @Test
    public void reconcileMaterializesExpectedChampionships() throws Exception {
        loadFixtureIntoMemoryDatabase(V65_DATABASE, "v65-h2v2.mv.db");

        reconcileTwice();

        assertMigratedChampionships();
    }

    @Test
    public void reconcileRemovesIwfRows() throws Exception {
        loadFixtureIntoMemoryDatabase(V65_DATABASE, "v65-h2v2.mv.db");

        reconcileTwice();

        assertNoIwfRowsRemain();
    }
}