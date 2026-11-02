/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.access.AccessStartup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.technicalofficial.OfficialRole;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Sessions refer to their platform by name, and timetable entries to their session by name. Both are resolved while
 * the import is writing the platforms and sessions, before they are committed.
 */
public class V2SessionImportTest {

	private static final String FIXTURE = "/startup/competition-director-acceptance.json";
	private static final ObjectMapper MAPPER = JsonMapper.builder().build();
	private static final LocalDateTime FIRST_SNATCH = LocalDateTime.of(2026, 11, 2, 10, 0, 5);
	private static final LocalDateTime FIRST_CJ = LocalDateTime.of(2026, 11, 2, 10, 40, 6);
	private static final LocalDateTime LAST_SNATCH_DECISION = LocalDateTime.of(2026, 11, 2, 10, 20, 7);
	private static final LocalDateTime LAST_CJ_DECISION = LocalDateTime.of(2026, 11, 2, 11, 0, 8);

	/** The session properties in the V2 format before sessions were written by introspection. */
	private static final Set<String> V2_SESSION_PROPERTIES = Set.of("announcer", "cleanJerkBreakDuration",
	        "competitionDirector", "competitionSecretary", "competitionSecretary2", "competitionTime", "description",
	        "doctor", "doctor2", "doctor3", "done", "firstCJTime", "firstSnatchTime", "jury1", "jury2", "jury3", "jury4",
	        "jury5", "lastCJDecisionTime", "lastSnatchDecisionTime", "marshal2", "marshall", "masters", "name",
	        "platformName", "referee1", "referee2", "referee3", "reserve", "reserveJury", "technicalController",
	        "technicalController2", "technicalController3", "timeKeeper", "tis1", "tis2", "weighIn1", "weighIn2",
	        "weighInTime");

	@BeforeClass
	public static void loadFixtureWithPlatformsAndTimetable() throws Exception {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		ObjectNode root;
		try (InputStream in = V2SessionImportTest.class.getResourceAsStream(FIXTURE)) {
			assertNotNull("fixture " + FIXTURE, in);
			root = (ObjectNode) MAPPER.readTree(in);
		}
		for (JsonNode session : root.get("sessions")) {
			((ObjectNode) session).put("platformName", "A");
			if ("M1".equals(session.get("name").asString())) {
				((ObjectNode) session)
				        .put("firstSnatchTime", FIRST_SNATCH.toString())
				        .put("firstCJTime", FIRST_CJ.toString())
				        .put("lastSnatchDecisionTime", LAST_SNATCH_DECISION.toString())
				        .put("lastCJDecisionTime", LAST_CJ_DECISION.toString());
			}
		}
		((ArrayNode) root.get("technicalOfficialsTimetable")).addObject()
		        .put("sessionName", "M1").put("roleCategory", OfficialRole.TIMEKEEPER.name()).put("teamNumber", 1);
		FormatDetector.importData(new ByteArrayInputStream(MAPPER.writeValueAsBytes(root)));
		AccessStartup.roleAccountInitialization().join();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Test
	public void sessionsKeepTheirPlatform() {
		List<Group> sessions = GroupRepository.findAll();
		assertEquals(8, sessions.size());
		for (Group session : sessions) {
			assertNotNull(session.getName() + " should keep its platform", session.getPlatform());
			assertEquals(session.getName(), "A", session.getPlatform().getName());
		}
	}

	@Test
	public void sessionTimesAreRestored() {
		Group m1 = GroupRepository.findByName("M1");
		assertEquals(FIRST_SNATCH, m1.getFirstSnatchTime());
		assertEquals(FIRST_CJ, m1.getFirstCJTime());
		assertEquals(LAST_SNATCH_DECISION, m1.getLastSnatchDecisionTime());
		assertEquals(LAST_CJ_DECISION, m1.getLastCJDecisionTime());
	}

	@Test
	public void timetableEntriesAreRestored() {
		List<TechnicalOfficialsTimetable> entries = JPAService
		        .runInTransaction(em -> TechnicalOfficialsTimetableRepository.findAll(em));
		assertEquals(1, entries.size());
		assertEquals("M1", entries.get(0).getGroup().getName());
		assertEquals(OfficialRole.TIMEKEEPER, entries.get(0).getRoleCategory());
	}

	@Test
	public void exportedTimetableEntriesReferToTheirSessionByName() throws Exception {
		JsonNode exported;
		try (InputStream in = new CompetitionDataV2().fromDatabase().exportData()) {
			exported = MAPPER.readTree(in);
		}
		JsonNode entries = exported.get("technicalOfficialsTimetable");
		assertEquals(1, entries.size());
		JsonNode entry = entries.get(0);
		assertEquals("M1", entry.get("sessionName").asString());
		assertEquals(OfficialRole.TIMEKEEPER.name(), entry.get("roleCategory").asString());
		assertEquals(1, entry.get("teamNumber").asInt());
		assertFalse("database id must not be exported", entry.has("id"));
		assertFalse("session is exported by name only", entry.has("group"));
	}

	@Test
	public void exportedSessionsKeepTheV2Format() throws Exception {
		JsonNode exported;
		try (InputStream in = new CompetitionDataV2().fromDatabase().exportData()) {
			exported = MAPPER.readTree(in);
		}
		for (JsonNode session : exported.get("sessions")) {
			Set<String> properties = new TreeSet<>(session.propertyNames());
			assertTrue(session.get("name") + " lacks " + missing(properties),
			        properties.containsAll(V2_SESSION_PROPERTIES));
			assertFalse("database id must not be exported", properties.contains("id"));
			assertFalse("platform is exported by name only", properties.contains("platform"));
			assertEquals("A", session.get("platformName").asString());
			if ("M1".equals(session.get("name").asString())) {
				assertEquals(FIRST_SNATCH, LocalDateTime.parse(session.get("firstSnatchTime").asString()));
				assertEquals(LAST_CJ_DECISION, LocalDateTime.parse(session.get("lastCJDecisionTime").asString()));
			}
		}
	}

	private static Set<String> missing(Set<String> properties) {
		Set<String> missing = new TreeSet<>(V2_SESSION_PROPERTIES);
		missing.removeAll(properties);
		return missing;
	}
}
