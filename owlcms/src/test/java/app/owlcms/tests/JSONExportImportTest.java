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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.access.AccessMode;
import app.owlcms.access.AccessStartup;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.Main;
import app.owlcms.access.PasswordHasher;
import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.access.AccountTokens;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.agegroup.MedalPolicy;
import app.owlcms.data.agegroup.TeamPointsPolicy;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.data.coach.Coach;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ForwardingConnection;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.export.v2.AthleteDTO;
import app.owlcms.data.export.v2.ChampionshipDTO;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

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
    public void accountsRoundTripThroughBothJsonFormats() throws Exception {
        UserAccount account = new UserAccount();
        account.setUsername("fixtureuser");
        account.setPasswordHash(PasswordHasher.hash("fixtureuser"));
        account.setGrants(new ArrayList<>(List.of(new RoleGrant(Role.ANNOUNCER, "A"))));
        UserAccountRepository.save(account, AuditOrigin.system("test"));

        String legacyJson = new CompetitionData().exportDataAsString();
        CompetitionData legacyImported = new CompetitionData().importDataFromString(legacyJson);
        assertTrue(legacyJson.contains("\"passwordHash\""));
        assertTrue(legacyImported.getAccounts().stream()
                .anyMatch(imported -> imported.getUsername().equals("fixtureuser")
                        && PasswordHasher.verify("fixtureuser", imported.getPasswordHash())));

        CompetitionDataV2 exportedV2 = new CompetitionDataV2().fromDatabase();
        String v2Json = new String(exportedV2.exportData().readAllBytes(), StandardCharsets.UTF_8);
        CompetitionDataV2 importedV2 = new CompetitionDataV2().importData(
                new ByteArrayInputStream(v2Json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(importedV2.getAccounts().stream()
                .anyMatch(imported -> imported.getUsername().equals("fixtureuser")
                        && PasswordHasher.verify("fixtureuser", imported.getPasswordHash())));
    }

    @Test
    public void mustChangePasswordRoundTripsThroughBothJsonFormats() throws Exception {
        String username = "mustchangeuser";
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(PasswordHasher.hash("mustchangeuser"));
        account.setMustChangePassword(true);
        UserAccountRepository.save(account, AuditOrigin.system("test"));

        String legacyJson = new CompetitionData().exportDataAsString();
        String v2Json = new String(new CompetitionDataV2().fromDatabase().exportData().readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(legacyJson.contains("\"mustChangePassword\""));
        assertTrue(v2Json.contains("\"mustChangePassword\""));

        CompetitionData legacyImported = new CompetitionData().importDataFromString(legacyJson);
        assertTrue(findAccount(legacyImported.getAccounts(), username).isMustChangePassword());
        CompetitionDataV2 v2Imported = new CompetitionDataV2().importData(
                new ByteArrayInputStream(v2Json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(findAccount(v2Imported.getAccounts(), username).isMustChangePassword());
    }

    @Test
    public void genericFlagRoundTripsThroughBothJsonFormats() throws Exception {
        UserAccount generic = new UserAccount();
        generic.setUsername("genericroundtrip");
        generic.setGeneric(true);
        UserAccountRepository.save(generic, AuditOrigin.system("test"));
        UserAccount person = new UserAccount();
        person.setUsername("personroundtrip");
        UserAccountRepository.save(person, AuditOrigin.system("test"));

        String legacyJson = new CompetitionData().exportDataAsString();
        String v2Json = new String(new CompetitionDataV2().fromDatabase().exportData().readAllBytes(),
                StandardCharsets.UTF_8);

        CompetitionData legacyImported = new CompetitionData().importDataFromString(legacyJson);
        assertTrue(findAccount(legacyImported.getAccounts(), "genericroundtrip").isGeneric());
        assertFalse(findAccount(legacyImported.getAccounts(), "personroundtrip").isGeneric());
        CompetitionDataV2 v2Imported = new CompetitionDataV2().importData(
                new ByteArrayInputStream(v2Json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(findAccount(v2Imported.getAccounts(), "genericroundtrip").isGeneric());
        assertFalse(findAccount(v2Imported.getAccounts(), "personroundtrip").isGeneric());
    }

    @Test
    public void jsonWithoutMustChangePasswordImportsAsFalse() throws Exception {
        String username = "legacyjsonuser";
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(PasswordHasher.hash("legacyjsonuser"));
        account.setMustChangePassword(true);
        UserAccountRepository.save(account, AuditOrigin.system("test"));

        ObjectMapper mapper = JsonMapper.builder().build();
        JsonNode legacyTree = mapper.readTree(new CompetitionData().exportDataAsString());
        removeMustChangePassword(legacyTree.get("accounts"));
        JsonNode v2Tree = mapper.readTree(new String(
                new CompetitionDataV2().fromDatabase().exportData().readAllBytes(), StandardCharsets.UTF_8));
        removeMustChangePassword(v2Tree.get("accounts"));

        CompetitionData legacyImported = new CompetitionData().importDataFromString(mapper.writeValueAsString(legacyTree));
        assertFalse(findAccount(legacyImported.getAccounts(), username).isMustChangePassword());
        CompetitionDataV2 v2Imported = new CompetitionDataV2().importData(new ByteArrayInputStream(
                mapper.writeValueAsString(v2Tree).getBytes(StandardCharsets.UTF_8)));
        assertFalse(findAccount(v2Imported.getAccounts(), username).isMustChangePassword());
    }

    private static UserAccount findAccount(List<UserAccount> accounts, String username) {
        return accounts.stream().filter(a -> username.equals(a.getUsername())).findFirst().orElseThrow();
    }

    @Test
    public void tokenHashRoundTripsThroughBothJsonFormatsWithoutTheToken() throws Exception {
        String username = "tokenuser";
        AccountTokens.Generated generated = AccountTokens.generate();
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.replaceToken(generated.record());
        UserAccountRepository.save(account, AuditOrigin.system("test"));

        String legacyJson = new CompetitionData().exportDataAsString();
        String v2Json = new String(new CompetitionDataV2().fromDatabase().exportData().readAllBytes(),
                StandardCharsets.UTF_8);
        for (String json : List.of(legacyJson, v2Json)) {
            assertTrue(json.contains(generated.record().getTokenHash()));
            assertFalse("the token itself is never exported", json.contains(generated.value()));
        }

        UserAccount legacy = findAccount(new CompetitionData().importDataFromString(legacyJson).getAccounts(),
                username);
        UserAccount v2 = findAccount(new CompetitionDataV2().importData(
                new ByteArrayInputStream(v2Json.getBytes(StandardCharsets.UTF_8))).getAccounts(), username);
        for (UserAccount imported : List.of(legacy, v2)) {
            assertEquals(generated.record().getTokenId(), imported.getToken().getTokenId());
            assertEquals(AccountTokens.hash(generated.value()), imported.getToken().getTokenHash());
        }
    }

    private static void removeMustChangePassword(JsonNode accounts) {
        assertNotNull("exported JSON should contain accounts", accounts);
        for (JsonNode node : accounts) {
            ((ObjectNode) node).remove("mustChangePassword");
        }
    }

    @Test
    public void securitySettingsRoundTripThroughBothJsonFormats() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();
        try {
            Config config = Config.getCurrent();
            config.setAccessMode(AccessMode.ACCOUNTS);
            config.setIwfCompliance(true);
            Config.setCurrent(config);
            AccessStartup.roleAccountInitialization().join();
            String legacyJson = new CompetitionData().exportDataAsString();
            String v2Json = new String(new CompetitionDataV2().fromDatabase().exportData().readAllBytes(),
                    StandardCharsets.UTF_8);
            assertEquals("ACCOUNTS", mapper.readTree(legacyJson).get("config").get("accessMode").asText());
            assertEquals("ACCOUNTS", mapper.readTree(v2Json).get("config").get("accessMode").asText());

            for (boolean v2 : new boolean[] { false, true }) {
                config = Config.getCurrent();
                config.setAccessMode(AccessMode.PIN);
                config.setIwfCompliance(false);
                Config.setCurrent(config);
                if (v2) {
                    new CompetitionDataV2().importData(
                            new ByteArrayInputStream(v2Json.getBytes(StandardCharsets.UTF_8)));
                } else {
                    new CompetitionData().importDataFromString(legacyJson);
                }
                AccessStartup.roleAccountInitialization().join();
                assertEquals("the import is faithful", AccessMode.ACCOUNTS, Config.getCurrent().getAccessMode());
                assertTrue(Config.getCurrent().isIwfComplianceChecked());
            }
        } finally {
            Config restored = Config.getCurrent();
            restored.setAccessMode(AccessMode.PIN);
            restored.setIwfCompliance(null);
            Config.setCurrent(restored);
        }
    }

    @Test
    public void forwardingUpdateKeysRoundTripThroughBothJsonFormatsInPinMode() throws Exception {
        Config config = Config.getCurrent();
        List<ForwardingConnection> originalConnections = config.getForwardingDestinations();
        ObjectMapper mapper = JsonMapper.builder().build();
        ForwardingConnection connection = new ForwardingConnection("wss://tracker.example/ws", "fixture-update-key");
        try {
            config.setForwardingDestinations(List.of(connection));

            String legacyJson = new CompetitionData().exportDataAsString();
            String legacyExportedKey = mapper.readTree(legacyJson)
                    .get("config").get("forwardingDestinations").get(0).get("updateKey").asString();
            CompetitionData legacyImported = new CompetitionData().importDataFromString(legacyJson);

            Config.setCurrent(config);
            CompetitionDataV2 v2 = new CompetitionDataV2().fromDatabase();
            String v2Json = new String(v2.exportData().readAllBytes(), StandardCharsets.UTF_8);
            String v2ExportedKey = mapper.readTree(v2Json)
                    .get("config").get("forwardingDestinations").get(0).get("updateKey").asString();
            CompetitionDataV2 v2Imported = new CompetitionDataV2().importData(
                    new ByteArrayInputStream(v2Json.getBytes(StandardCharsets.UTF_8)));

            assertEquals("fixture-update-key", legacyExportedKey);
            assertEquals("fixture-update-key", v2ExportedKey);
            assertEquals("fixture-update-key",
                    legacyImported.getConfig().getForwardingDestinations().get(0).getUpdateKey());
            assertEquals("fixture-update-key",
                    v2Imported.getConfig().getForwardingDestinations().get(0).getUpdateKey());
            assertEquals("fixture-update-key", config.getForwardingDestinations().get(0).getUpdateKey());
        } finally {
            config.setForwardingDestinations(originalConnections);
            Config.setCurrent(config);
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
        exported.setConfig(Config.getCurrent());
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
    public void athleteScaleWeightAndEligibilityStatusRoundTripThroughV2Dto() {
        Athlete source = new Athlete();
        source.setScaleWeight(81.37);
        source.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.OOC_DID_NOT_MAKE_WEIGHT);

        Athlete restored = AthleteDTO.fromAthlete(source).toAthlete(null);

        assertEquals(Double.valueOf(81.37), restored.getScaleWeight());
        assertEquals(EligibleForIndividualRankingStatus.OOC_DID_NOT_MAKE_WEIGHT,
                restored.getIndividualEligibilityStatus());
        assertFalse(restored.isEligibleForIndividualRanking());
    }

    @Test
    public void athleteAttemptDecisionsRoundTripThroughV2Json() throws Exception {
        Athlete source = new Athlete();
        source.setSnatch1Decisions(List.of(false, true, false));
        source.setSnatch1JuryDeliberation(false);
        source.setSnatch2Decisions(List.of(true, true, false));
        source.setSnatch2Challenge(false);
        source.setSnatch3Decisions(List.of(true, false, true));
        source.setCleanJerk1Decisions(List.of(false, false, true));
        source.setCleanJerk1JuryDeliberation(true);
        source.setCleanJerk2Decisions(List.of(false, false, false));
        source.setCleanJerk3Decisions(List.of(true, true, true));
        source.setCleanJerk3Challenge(true);

        CompetitionDataV2 exported = new CompetitionDataV2();
        exported.setConfig(Config.getCurrent());
        exported.setAthletes(List.of(AthleteDTO.fromAthlete(source)));
        exported.setPlatforms(List.of());
        String json = new String(exported.exportData().readAllBytes(), StandardCharsets.UTF_8);
        JsonNode athlete = JsonMapper.builder().build().readTree(json).get("athletes").get(0);

        assertEquals(List.of(false, true, false), booleans(athlete.get("snatch1Decisions")));
        assertTrue(athlete.get("snatch1JuryDeliberation").isBoolean());
        assertFalse(athlete.get("snatch1JuryDeliberation").asBoolean());
        assertTrue(athlete.get("snatch1Challenge").isNull());
        assertFalse(athlete.get("snatch2Challenge").asBoolean());
        assertTrue(athlete.get("cleanJerk1JuryDeliberation").asBoolean());
        assertEquals(List.of(true, true, true), booleans(athlete.get("cleanJerk3Decisions")));
        assertTrue(athlete.get("cleanJerk3JuryDeliberation").isNull());
        assertTrue(athlete.get("cleanJerk3Challenge").isBoolean());
        assertTrue(athlete.get("cleanJerk3Challenge").asBoolean());

        CompetitionDataV2 imported = new CompetitionDataV2().importData(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        Athlete restored = imported.getAthletes().get(0).toAthlete(null);

        assertEquals(List.of(false, true, false), restored.getSnatch1Decisions());
        assertEquals(Boolean.FALSE, restored.getSnatch1JuryDeliberation());
        assertNull(restored.getSnatch1Challenge());

        assertEquals(List.of(true, true, false), restored.getSnatch2Decisions());
        assertNull(restored.getSnatch2JuryDeliberation());
        assertEquals(Boolean.FALSE, restored.getSnatch2Challenge());

        assertEquals(List.of(true, false, true), restored.getSnatch3Decisions());
        assertNull(restored.getSnatch3JuryDeliberation());
        assertNull(restored.getSnatch3Challenge());

        assertEquals(List.of(false, false, true), restored.getCleanJerk1Decisions());
        assertEquals(Boolean.TRUE, restored.getCleanJerk1JuryDeliberation());
        assertNull(restored.getCleanJerk1Challenge());

        assertEquals(List.of(false, false, false), restored.getCleanJerk2Decisions());
        assertNull(restored.getCleanJerk2JuryDeliberation());
        assertNull(restored.getCleanJerk2Challenge());

        assertEquals(List.of(true, true, true), restored.getCleanJerk3Decisions());
        assertNull(restored.getCleanJerk3JuryDeliberation());
        assertEquals(Boolean.TRUE, restored.getCleanJerk3Challenge());
    }

    private static List<Boolean> booleans(JsonNode array) {
        assertTrue(array.isArray());
        List<Boolean> values = new ArrayList<>();
        for (JsonNode node : array) {
            assertTrue(node.isBoolean());
            values.add(node.asBoolean());
        }
        return values;
    }

    @Test
    public void coachesDeserializeInBothJsonFormats() {
        String coachJson = "{\"coaches\":[{\"id\":123,\"lastName\":\"Doe\","
                + "\"firstName\":\"Jane\",\"membershipId\":\"C-42\",\"team\":\"OWL\"}]}";

        CompetitionData v1 = new CompetitionData().importDataFromString(coachJson);
        CompetitionDataV2 v2 = new CompetitionDataV2().importData(
                new ByteArrayInputStream(coachJson.getBytes(StandardCharsets.UTF_8)));

        for (Coach coach : List.of(v1.getCoaches().get(0), v2.getCoaches().get(0))) {
            assertEquals(Long.valueOf(123), coach.getId());
            assertEquals("Doe", coach.getLastName());
            assertEquals("Jane", coach.getFirstName());
            assertEquals("C-42", coach.getMembershipId());
            assertEquals("OWL", coach.getTeam());
        }
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
    public void legacyCompetitionTemplateWithoutMarkerRestores() {
        CompetitionData competitionData = new CompetitionData();
        try {
            ObjectMapper mapper = JsonMapper.builder().build();
            JsonNode export = mapper.readTree(competitionData.exportDataAsString());
            for (JsonNode championship : export.path("championships")) {
                if (Championship.COMPETITION_TEMPLATE_NAME.equals(championship.path("name").asString())) {
                    ((ObjectNode) championship).remove("competitionTemplate");
                }
            }

            competitionData.restore(new ByteArrayInputStream(
                    mapper.writeValueAsString(export).getBytes(StandardCharsets.UTF_8)));

            List<Championship> templates = Championship.findAllIncludingTemplate().stream()
                    .filter(Championship::isCompetitionTemplate)
                    .toList();
            assertEquals("legacy import should restore exactly one competition template", 1, templates.size());
            assertEquals(Championship.COMPETITION_TEMPLATE_NAME, templates.get(0).getName());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void legacyRestoreReplacesChampionshipsAndTechnicalOfficials() throws Exception {
        TechnicalOfficial official = new TechnicalOfficial();
        official.setLastName("Restore");
        official.setFirstName("Fixture");
        TechnicalOfficialRepository.save(official);
        String export = new CompetitionData().exportDataAsString();
        int championshipCount = Championship.findAllIncludingTemplate().size();
        int officialCount = TechnicalOfficialRepository.findAll().size();

        new CompetitionData().restore(new ByteArrayInputStream(export.getBytes(StandardCharsets.UTF_8)));

        assertEquals(championshipCount, ChampionshipRepository.findAllIncludingTemplate().size());
        assertEquals(officialCount, TechnicalOfficialRepository.findAll().size());
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
