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
import app.owlcms.data.agegroup.MedalPolicy;
import app.owlcms.data.agegroup.TeamPointsPolicy;
import app.owlcms.data.config.Config;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.export.v2.ChampionshipDTO;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;

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
    public void collarThresholdRoundTripsThroughV2PlatformJson() throws Exception {
        Platform platform = new Platform("Competition");
        platform.setCollarThreshold(37);
        CompetitionDataV2 exported = new CompetitionDataV2();
        exported.setPlatforms(List.of(platform));

        String json = new String(exported.exportData().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue("V2 export should include the platform collar threshold",
                json.contains("\"collarThreshold\" : 37"));

        CompetitionDataV2 imported = new CompetitionDataV2().importData(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(imported);
        assertEquals(Integer.valueOf(37), imported.getPlatforms().get(0).getCollarThreshold());
    }

    @Test
    public void legacyJsonWithoutTeamPointsPolicyUsesMedalConfiguration() {
        String json = "{\"championships\":["
                + "{\"name\":\"Three medals\",\"type\":\"U\",\"snatchCJTotalMedals\":true},"
                + "{\"name\":\"Total\",\"type\":\"U\",\"snatchCJTotalMedals\":false}]}";

        CompetitionData imported = new CompetitionData().importDataFromString(json);

        assertEquals(TeamPointsPolicy.ALL_THREE, imported.getChampionships().get(0).getTeamPointsPolicy());
        assertEquals(TeamPointsPolicy.TOTAL_ONLY, imported.getChampionships().get(1).getTeamPointsPolicy());
        assertEquals(MedalPolicy.ALL_THREE, imported.getChampionships().get(0).getMedalPolicy());
        assertEquals(MedalPolicy.TOTAL_ONLY, imported.getChampionships().get(1).getMedalPolicy());
    }

    @Test
    public void v2JsonWithoutTeamPointsPolicyUsesMedalConfiguration() {
        String json = "{\"formatVersion\":\"2.0\",\"championships\":["
                + "{\"name\":\"Three medals\",\"type\":\"U\",\"snatchCJTotalMedals\":true},"
                + "{\"name\":\"Total\",\"type\":\"U\",\"snatchCJTotalMedals\":false}]}";

        CompetitionDataV2 imported = new CompetitionDataV2().importData(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(imported);
        assertEquals(TeamPointsPolicy.ALL_THREE, imported.getChampionships().get(0).getTeamPointsPolicy());
        assertEquals(TeamPointsPolicy.TOTAL_ONLY, imported.getChampionships().get(1).getTeamPointsPolicy());
        assertEquals(MedalPolicy.ALL_THREE, imported.getChampionships().get(0).getMedalPolicy());
        assertEquals(MedalPolicy.TOTAL_ONLY, imported.getChampionships().get(1).getMedalPolicy());
    }

    @Test
    public void priorJsonMapsMixedTeamSizeToExplicitRosterSizeAndDefaultsToCombinedTeams() {
        CompetitionData v1Explicit = new CompetitionData().importDataFromString(
                "{\"competition\":{\"mixedTeamSize\":6}}");
        CompetitionData v1Missing = new CompetitionData().importDataFromString(
                "{\"competition\":{}}");
        CompetitionDataV2 v2Explicit = new CompetitionDataV2().importData(
                new ByteArrayInputStream(
                        "{\"formatVersion\":\"2.0\",\"competition\":{\"mixedTeamSize\":6}}"
                                .getBytes(StandardCharsets.UTF_8)));
        CompetitionDataV2 v2Missing = new CompetitionDataV2().importData(
                new ByteArrayInputStream(
                        "{\"formatVersion\":\"2.0\",\"competition\":{}}"
                                .getBytes(StandardCharsets.UTF_8)));

        for (Competition competition : List.of(v1Explicit.getCompetition(), v2Explicit.getCompetition())) {
            Championship template = new Championship(Championship.COMPETITION_TEMPLATE_NAME, null);
            template.populateCompetitionTemplateDefaults(competition);
            assertEquals("old mixedTeamSize should become the explicit mixed roster size",
                    Integer.valueOf(6), template.getExplicitTeamSize());
            assertEquals("old mixedTeamSize should not become # Best Mixed", null, template.getMixedBestN());
            assertTrue("mixed scoring should default to combined teams", template.isCombinedMenWomenTeams());
        }

        for (Competition competition : List.of(v1Missing.getCompetition(), v2Missing.getCompetition())) {
            Championship template = new Championship(Championship.COMPETITION_TEMPLATE_NAME, null);
            template.populateCompetitionTemplateDefaults(competition);
            assertEquals("missing old mixedTeamSize should retain the explicit roster default",
                    Integer.valueOf(8), template.getExplicitTeamSize());
            assertEquals("combined teams should have no overall mixed cap", null, template.getMixedBestN());
            assertEquals(Integer.valueOf(Championship.COMBINED_GENDER_TEAM_LIMIT), template.getMixedMensBestN());
            assertEquals(Integer.valueOf(Championship.COMBINED_GENDER_TEAM_LIMIT), template.getMixedWomensBestN());
            assertTrue("missing old mixedTeamSize should use combined teams", template.isCombinedMenWomenTeams());
        }
    }

    @Test
    public void explicitMedalPolicyWinsRegardlessOfJsonPropertyOrder() {
        for (String fields : List.of(
                "\"medalPolicy\":\"LIFTS_ONLY\",\"snatchCJTotalMedals\":false,\"teamPointsPolicy\":\"TOTAL_ONLY\"",
                "\"teamPointsPolicy\":\"TOTAL_ONLY\",\"snatchCJTotalMedals\":false,\"medalPolicy\":\"LIFTS_ONLY\"")) {
            String json = "{\"formatVersion\":\"2.0\",\"championships\":[{\"name\":\"Lifts\",\"type\":\"U\"," + fields + "}]}";
            Championship legacy = new CompetitionData().importDataFromString(json).getChampionships().get(0);
            CompetitionDataV2 imported = new CompetitionDataV2().importData(
                    new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
            assertNotNull(imported);
            Championship restored = imported.getChampionships().get(0).toChampionship();
            for (Championship championship : List.of(legacy, restored)) {
                assertEquals(MedalPolicy.LIFTS_ONLY, championship.getMedalPolicy());
                assertEquals(TeamPointsPolicy.TOTAL_ONLY, championship.getTeamPointsPolicy());
            }
        }
    }

    @Test
    public void medalPolicyRoundTripsThroughDtoAndCopiesDefaults() {
        Championship template = new Championship(Championship.COMPETITION_TEMPLATE_NAME, null);
        template.setCompetitionTemplate(true);
        template.setMedalPolicy(MedalPolicy.LIFTS_ONLY);
        template.setTeamPointsPolicy(TeamPointsPolicy.ALL_THREE);
        template.normalizeTeamPointsPolicy();

        Championship copy = new Championship("Lifts", null);
        copy.copyCompetitionSettingsFrom(template);
        Championship restored = ChampionshipDTO.fromChampionship(copy).toChampionship();

        assertEquals(MedalPolicy.LIFTS_ONLY, restored.getMedalPolicy());
        assertEquals(TeamPointsPolicy.ALL_THREE, restored.getTeamPointsPolicy());
        assertTrue(copy.computeCompetitionDefaultDifferences(template, false).isEmpty());
        copy.setMedalPolicy(MedalPolicy.ALL_THREE);
        assertTrue(copy.computeCompetitionDefaultDifferences(template, false).stream()
                .anyMatch(difference -> difference.startsWith("medalPolicy=")));
    }

    @Test
    public void v2TeamPointsPolicyRoundTripsThroughDto() {
        Championship championship = new Championship("Senior", null);
        championship.setSnatchCJTotalMedals(true);
        championship.setTeamPointsPolicy(TeamPointsPolicy.LIFTS_ONLY);

        Championship restored = ChampionshipDTO.fromChampionship(championship).toChampionship();

        assertEquals(TeamPointsPolicy.LIFTS_ONLY, restored.getTeamPointsPolicy());
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
