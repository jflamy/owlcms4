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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.data.technicalofficial.OfficialRole;
import app.owlcms.data.technicalofficial.SessionAssignmentGenerator;
import app.owlcms.data.technicalofficial.TeamRole;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
import app.owlcms.spreadsheet.TimetableIO;

public class SessionAssignmentGeneratorTest {

	private static final String FIRST_SESSION_NAME = "M1";
	private static final String SECOND_SESSION_NAME = "M2";

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		System.setProperty("enableEmbeddedMqtt", "false");
		JPAService.init(true, true);
		Config.initConfig();
		ProdData.insertInitialData(0);
		app.owlcms.init.OwlcmsFactory.initDefaultFOP();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Before
	public void resetDatabase() {
		JPAService.close();
		JPAService.init(true, true);
		Config.initConfig();
		ProdData.insertInitialData(0);
		app.owlcms.init.OwlcmsFactory.initDefaultFOP();
		JPAService.runInTransaction(em -> {
			TechnicalOfficialRepository.deleteAll(em);
			TechnicalOfficialsTimetableRepository.deleteAll(em);
			return null;
		});
	}

	@Test
	public void generateSessionAssignmentsClearsStaleAssignmentsBeforeRebuilding() {
		Group session = GroupRepository.findByName(FIRST_SESSION_NAME);
		assertNotNull("Expected initial session " + FIRST_SESSION_NAME, session);

		session.setJury4("Montero, David");
		session.setJury5("Alarcon, Jose");
		session.setReserveJury("Legacy, Reserve Jury");
		session.setReferee1("Legacy, Left Referee");
		session.setReferee2("Legacy, Center Referee");
		session.setReferee3("Legacy, Right Referee");
		session.setReserve("Legacy, Reserve Referee");
		GroupRepository.save(session);

		createOfficial("Ortiz", "Maritza", TeamRole.JURY_PRESIDENT, 1);
		createOfficial("Zambrano", "Luis", TeamRole.JURY, 1);
		createOfficial("Montero", "David", TeamRole.JURY, 1);

		createOfficial("Alarcon", "Jose", TeamRole.REFEREE, 2);
		createOfficial("Carpio", "Victor", TeamRole.REFEREE, 2);
		createOfficial("Nunez", "Maria", TeamRole.REFEREE, 2);

		createTimetableEntry(session, OfficialRole.JURY_PRESIDENT, 1);
		createTimetableEntry(session, OfficialRole.JURY, 1);
		createTimetableEntry(session, OfficialRole.REFEREE, 2);

		int assignmentCount = SessionAssignmentGenerator.generateSessionAssignments();
		assertEquals("Expected president + 2 jury + 3 referees", 6, assignmentCount);

		Group refreshed = GroupRepository.findByName(FIRST_SESSION_NAME);
		assertNotNull(refreshed);

		assertEquals("Ortiz, Maritza", refreshed.getJury1());
		assertEquals(setOf("Zambrano, Luis", "Montero, David"), setOf(refreshed.getJury2(), refreshed.getJury3()));
		assertNull("Stale jury slot must be cleared", refreshed.getJury4());
		assertNull("Stale non-jury assignment must be cleared", refreshed.getJury5());
		assertNull("Stale jury reserve must be cleared", refreshed.getReserveJury());
		assertFalse(
				"Referee-only official must not remain in jury slots after regeneration",
				Arrays.asList(
						refreshed.getJury1(),
						refreshed.getJury2(),
						refreshed.getJury3(),
						refreshed.getJury4(),
						refreshed.getJury5(),
						refreshed.getReserveJury()).contains("Alarcon, Jose"));

		assertEquals(
				setOf("Alarcon, Jose", "Carpio, Victor", "Nunez, Maria"),
				setOf(refreshed.getReferee1(), refreshed.getReferee2(), refreshed.getReferee3()));
		assertNull("Referee reserve must be cleared when the team only has three members", refreshed.getReserve());
	}

	@Test
	public void timetableExportImportRoundTripsAnnouncerAssignments() throws Exception {
		Group session = GroupRepository.findByName(FIRST_SESSION_NAME);
		assertNotNull("Expected initial session " + FIRST_SESSION_NAME, session);

		TechnicalOfficialsTimetable announcerEntry = new TechnicalOfficialsTimetable(session, OfficialRole.ANNOUNCER, 3);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TimetableIO.exportTimetable(out, List.of(announcerEntry));

		List<TechnicalOfficialsTimetable> importedEntries = TimetableIO.importTimetable(
				new ByteArrayInputStream(out.toByteArray()));

		assertEquals(1, importedEntries.size());
		TechnicalOfficialsTimetable importedEntry = importedEntries.get(0);
		assertEquals(FIRST_SESSION_NAME, importedEntry.getGroup().getName());
		assertEquals(OfficialRole.ANNOUNCER, importedEntry.getRoleCategory());
		assertEquals(Integer.valueOf(3), importedEntry.getTeamNumber());
	}

	@Test
	public void timetableExportImportRoundTripsWeighInTeamAssignments() throws Exception {
		Group session = GroupRepository.findByName(FIRST_SESSION_NAME);
		assertNotNull("Expected initial session " + FIRST_SESSION_NAME, session);

		TechnicalOfficialsTimetable weighInEntry = new TechnicalOfficialsTimetable(session, OfficialRole.WEIGHIN, 4);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TimetableIO.exportTimetable(out, List.of(weighInEntry));

		List<TechnicalOfficialsTimetable> importedEntries = TimetableIO.importTimetable(
				new ByteArrayInputStream(out.toByteArray()));

		assertEquals(1, importedEntries.size());
		TechnicalOfficialsTimetable importedEntry = importedEntries.get(0);
		assertEquals(FIRST_SESSION_NAME, importedEntry.getGroup().getName());
		assertEquals(OfficialRole.WEIGHIN, importedEntry.getRoleCategory());
		assertEquals(Integer.valueOf(4), importedEntry.getTeamNumber());
	}

	@Test
	public void generateSessionAssignmentsUsesThreePersonJuryRotation() {
		Group sessionA = GroupRepository.findByName(FIRST_SESSION_NAME);
		Group sessionB = GroupRepository.findByName(SECOND_SESSION_NAME);
		assertNotNull(sessionA);
		assertNotNull(sessionB);

		createOfficial("Ortiz", "Maritza", TeamRole.JURY_PRESIDENT, 1);
		createOfficial("Zambrano", "Luis", TeamRole.JURY, 1);
		createOfficial("Montero", "David", TeamRole.JURY, 1);
		createOfficial("Perez", "Lucia", TeamRole.JURY, 1);

		createTimetableEntry(sessionA, OfficialRole.JURY_PRESIDENT, 1);
		createTimetableEntry(sessionA, OfficialRole.JURY, 1);
		createTimetableEntry(sessionB, OfficialRole.JURY_PRESIDENT, 1);
		createTimetableEntry(sessionB, OfficialRole.JURY, 1);

		int assignmentCount = SessionAssignmentGenerator.generateSessionAssignments();
		assertEquals("Expected 2 president assignments and 3 rotating jury-member assignments per session", 8, assignmentCount);

		Group refreshedA = GroupRepository.findByName(FIRST_SESSION_NAME);
		Group refreshedB = GroupRepository.findByName(SECOND_SESSION_NAME);
		assertEquals("Ortiz, Maritza", refreshedA.getJury1());
		assertEquals("Ortiz, Maritza", refreshedB.getJury1());

		assertEquals("Zambrano, Luis", refreshedA.getJury2());
		assertEquals("Montero, David", refreshedA.getJury3());
		assertEquals("Perez, Lucia", refreshedA.getReserveJury());
		assertNull(refreshedA.getJury4());
		assertNull(refreshedA.getJury5());

		assertEquals("Perez, Lucia", refreshedB.getJury2());
		assertEquals("Zambrano, Luis", refreshedB.getJury3());
		assertEquals("Montero, David", refreshedB.getReserveJury());
		assertNull(refreshedB.getJury4());
		assertNull(refreshedB.getJury5());
	}

	@Test
	public void generateSessionAssignmentsUsesFivePersonJuryRotationWithoutReserveWhenOnlyFourMembers() {
		Group sessionA = GroupRepository.findByName(FIRST_SESSION_NAME);
		Group sessionB = GroupRepository.findByName(SECOND_SESSION_NAME);
		assertNotNull(sessionA);
		assertNotNull(sessionB);

		createOfficial("Ortiz", "Maritza", TeamRole.JURY_PRESIDENT, 1);
		createOfficial("Zambrano", "Luis", TeamRole.JURY, 1);
		createOfficial("Montero", "David", TeamRole.JURY, 1);
		createOfficial("Perez", "Lucia", TeamRole.JURY, 1);
		createOfficial("Ruiz", "Ana", TeamRole.JURY, 1);

		createTimetableEntry(sessionA, OfficialRole.JURY_PRESIDENT, 1);
		createTimetableEntry(sessionA, OfficialRole.JURY, 1);
		createTimetableEntry(sessionB, OfficialRole.JURY_PRESIDENT, 1);
		createTimetableEntry(sessionB, OfficialRole.JURY, 1);

		int assignmentCount = SessionAssignmentGenerator.generateSessionAssignments();
		assertEquals("Expected 2 president assignments and 4 rotating jury-member assignments per session", 10, assignmentCount);

		Group refreshedA = GroupRepository.findByName(FIRST_SESSION_NAME);
		Group refreshedB = GroupRepository.findByName(SECOND_SESSION_NAME);
		assertEquals("Ortiz, Maritza", refreshedA.getJury1());
		assertEquals("Ortiz, Maritza", refreshedB.getJury1());

		assertEquals("Zambrano, Luis", refreshedA.getJury2());
		assertEquals("Montero, David", refreshedA.getJury3());
		assertEquals("Perez, Lucia", refreshedA.getJury4());
		assertEquals("Ruiz, Ana", refreshedA.getJury5());
		assertNull(refreshedA.getReserveJury());

		assertEquals("Ruiz, Ana", refreshedB.getJury2());
		assertEquals("Zambrano, Luis", refreshedB.getJury3());
		assertEquals("Montero, David", refreshedB.getJury4());
		assertEquals("Perez, Lucia", refreshedB.getJury5());
		assertNull(refreshedB.getReserveJury());
	}

	private static TechnicalOfficial createOfficial(String lastName, String firstName, TeamRole teamRole, int teamNumber) {
		TechnicalOfficial official = new TechnicalOfficial();
		official.setLastName(lastName);
		official.setFirstName(firstName);
		official.setTeamRole(teamRole);
		official.setTechnicalOfficialTeam(teamNumber);
		return TechnicalOfficialRepository.save(official);
	}

	private static void createTimetableEntry(Group group, OfficialRole roleCategory, int teamNumber) {
		JPAService.runInTransaction(em -> {
			Group managedGroup = GroupRepository.getById(group.getId(), em);
			TechnicalOfficialsTimetable entry = new TechnicalOfficialsTimetable(managedGroup, roleCategory, teamNumber);
			TechnicalOfficialsTimetableRepository.save(em, entry);
			return null;
		});
	}

	private static Set<String> setOf(String... values) {
		return new HashSet<>(Arrays.asList(values));
	}
}