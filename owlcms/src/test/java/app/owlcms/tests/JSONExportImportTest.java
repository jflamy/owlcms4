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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.export.v2.ChampionshipDTO;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.JPAService;

public class JSONExportImportTest {
	
    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        TestData.insertInitialData(1, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

	@Test
	public void test() {
		/*
		 * Most bugs are due to an Exception when serializing causing a truncated output,
		 * or a bug in deserializing (e.g. duplicate objects)
		 */
        List<Championship> championships = ChampionshipRepository.findAll();
        assertFalse("championships should be loaded from age group data", championships.isEmpty());
        for (Championship championship : championships) {
            assertNotNull("championship name should be populated", championship.getName());
        }

        CompetitionData competitionData = new CompetitionData();
        try {
        	String s = competitionData.exportDataAsString();
            assertTrue("serialized JSON should include championship order", s.contains("\"order\""));
            CompetitionData imported = competitionData.importDataFromString(s);
            assertTrue("real championships should have an exported order",
                    imported.getChampionships().stream()
                            .filter(championship -> !championship.isCompetitionTemplate())
                            .allMatch(championship -> championship.getOrder() != null));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

    @Test
    public void legacyJsonWithoutChampionshipOrderPreservesArrayOrder() {
        String json = "{\"championships\":["
                + "{\"name\":\"Second\",\"type\":\"U\"},"
                + "{\"name\":\"First\",\"type\":\"U\"}]}";

        CompetitionData imported = new CompetitionData().importDataFromString(json);

        assertEquals(Integer.valueOf(0), imported.getChampionships().get(0).getOrder());
        assertEquals(Integer.valueOf(1), imported.getChampionships().get(1).getOrder());
    }

    @Test
    public void v2JsonWithoutChampionshipOrderPreservesArrayOrder() {
        String json = "{\"formatVersion\":\"2.0\",\"championships\":["
                + "{\"name\":\"Explicit\",\"type\":\"U\",\"order\":5},"
                + "{\"name\":\"Legacy First\",\"type\":\"U\"},"
                + "{\"name\":\"Legacy Second\",\"type\":\"U\"}]}";

        CompetitionDataV2 imported = new CompetitionDataV2().importData(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(imported);
        assertEquals(Integer.valueOf(5), imported.getChampionships().get(0).getOrder());
        assertEquals(Integer.valueOf(6), imported.getChampionships().get(1).getOrder());
        assertEquals(Integer.valueOf(7), imported.getChampionships().get(2).getOrder());
    }

    @Test
    public void championshipOrderRoundTripsThroughV2Dto() {
        Championship championship = new Championship("Senior", null);
        championship.setOrder(3);

        ChampionshipDTO dto = ChampionshipDTO.fromChampionship(championship);
        Championship restored = dto.toChampionship();

        assertEquals(Integer.valueOf(3), dto.getOrder());
        assertEquals(Integer.valueOf(3), restored.getOrder());
    }

    @Test
    public void useCompetitionDefaultsRoundTripsThroughCompetitionDataJson() {
        Championship source = Championship.findAll().stream()
                .filter(c -> !c.isCompetitionTemplate())
                .findFirst()
                .orElse(null);
        assertNotNull("expected at least one non-template championship", source);

        JPAService.runInTransaction(em -> {
            Championship managed = em.find(Championship.class, source.getId());
            managed.setUseCompetitionDefaults(true);
            return null;
        });
        Championship.reset();

        CompetitionData competitionData = new CompetitionData();
        try {
            String serialized = competitionData.exportDataAsString();
            assertFalse("serialized JSON should not include removed useCompetitionDefaults field", serialized.contains("\"useCompetitionDefaults\""));

            CompetitionData imported = competitionData.importDataFromString(serialized);
            Championship importedChampionship = imported.getChampionships().stream()
                    .filter(championship -> championship.getName().equals(source.getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull("championship should be present after import", importedChampionship);
            assertTrue("useCompetitionDefaults should round-trip through CompetitionData JSON",
                    importedChampionship.usesCompetitionDefaults());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
