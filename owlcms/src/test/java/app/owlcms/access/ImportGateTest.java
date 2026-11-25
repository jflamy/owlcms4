/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.access.ImportsNotAllowedException.Reason;
import app.owlcms.access.Principal.AuthSource;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.group.Group;
import app.owlcms.access.ImportLatch.ActiveSession;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.TestFopRegistry;
import app.owlcms.nui.admin.AdminView;
import app.owlcms.nui.admin.AuditIntegrityCheckView;
import app.owlcms.nui.preparation.ConfigContent;
import app.owlcms.tests.TestStartup;

/**
 * "Imports are allowed" (compliance item 130). No UI objects: the Administration page is checked through its page
 * rule only.
 */
public class ImportGateTest {

	private static final AuditOrigin ORIGIN = AuditOrigin.system("test");
	private static final Principal ADMIN = Principal.admin(AuthSource.ACCOUNT);

	private static Principal account(Role role) {
		return new Principal(AuthSource.ACCOUNT, 3L, role.name().toLowerCase(), List.of(new RoleGrant(role, null)),
		        null);
	}

	@BeforeClass
	public static void setUpDatabase() {
		TestStartup.init();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Before
	public void allowed() {
		if (CompetitionRepository.findAll().isEmpty()) {
			CompetitionRepository.create(new Competition(), ORIGIN);
		}
		ConfigRepository.apply(c -> c.setImportsAllowed(null), ORIGIN);
	}

	private static byte[] export() throws Exception {
		try (InputStream export = new CompetitionDataV2().fromDatabase().exportData()) {
			return export.readAllBytes();
		}
	}

	@Test
	public void unsetMeansAllowed() {
		assertNull(Config.getCurrent().getImportsAllowed());
		assertTrue(ImportGate.isAllowed());
		for (ImportGate.Kind kind : ImportGate.Kind.values()) {
			ImportGate.requireAllowed(kind, ADMIN, ORIGIN);
		}
	}

	@Test
	public void turnedOffEveryImportIsRefused() {
		assertTrue(ImportGate.setAllowed(false, ORIGIN));
		assertFalse("unchanged", ImportGate.setAllowed(false, ORIGIN));
		assertEquals(Boolean.FALSE, ConfigRepository.loadForEdit().getImportsAllowed());
		for (ImportGate.Kind kind : ImportGate.Kind.values()) {
			ImportsNotAllowedException refused = assertThrows(ImportsNotAllowedException.class,
			        () -> ImportGate.requireAllowed(kind, ADMIN, ORIGIN));
			assertEquals(kind, refused.getKind());
			assertEquals(Reason.NOT_ALLOWED, refused.getReason());
		}
		assertTrue(ImportGate.setAllowed(true, ORIGIN));
		assertTrue(ImportGate.isAllowed());
	}

	@Test
	public void databaseImportIsRefusedWhenTurnedOff() throws Exception {
		byte[] json = export();
		ImportGate.setAllowed(false, ORIGIN);
		Long stamp = ConfigRepository.loadForEdit().getLastSavedAt();
		ImportsNotAllowedException refused = assertThrows(ImportsNotAllowedException.class,
		        () -> FormatDetector.importData(json, "test.json", ADMIN, ORIGIN));
		assertEquals(ImportGate.Kind.DATABASE, refused.getKind());
		assertEquals("nothing was replaced", stamp, ConfigRepository.loadForEdit().getLastSavedAt());
	}

	@Test
	public void activeSessionRefusesSpreadsheetImportsButAllowsDatabaseImport() {
		FieldOfPlay platform = new FieldOfPlay();
		platform.setName("Platform 1");
		platform.setGroup(new Group("A"));
		Map<String, FieldOfPlay> previous = TestFopRegistry.replace(Map.of("Platform 1", platform));
		try {
			for (ImportGate.Kind kind : List.of(ImportGate.Kind.REGISTRATION, ImportGate.Kind.SBDE,
			        ImportGate.Kind.AGE_GROUPS)) {
				ImportsNotAllowedException refused = assertThrows(ImportsNotAllowedException.class,
				        () -> ImportGate.requireAllowed(kind, ADMIN, ORIGIN));
				assertEquals(Reason.SESSION_IN_PROGRESS, refused.getReason());
				assertEquals("SESSION_IN_PROGRESS", refused.getReason().auditValue());
				assertEquals("A", refused.getActiveSessions().get(0).sessionName());
				assertEquals("Platform 1", refused.getActiveSessions().get(0).platformName());
			}

			ImportsNotAllowedException refused = assertThrows(ImportsNotAllowedException.class,
			        () -> AgeGroupRepository.reloadDefinitions(new ByteArrayInputStream(new byte[0]), ORIGIN));
			assertEquals(ImportGate.Kind.AGE_GROUPS, refused.getKind());
			assertEquals(Reason.SESSION_IN_PROGRESS, refused.getReason());
			ImportGate.requireAllowed(ImportGate.Kind.DATABASE, ADMIN, ORIGIN);
		} finally {
			TestFopRegistry.replace(previous);
		}
	}

	@Test
	public void activePlatformMessageUsesLocalizedSingularAndPluralLists() {
		ActiveSession a = new ActiveSession(1L, "A1", "B");
		ActiveSession b = new ActiveSession(2L, "A2", "A");
		assertTrue(ImportLatch.activePlatformMessage(List.of(a), Locale.ENGLISH)
		        .startsWith("A session is active on platform B."));
		assertTrue(ImportLatch.activePlatformMessage(List.of(a, b), Locale.ENGLISH)
		        .startsWith("Sessions are active on platforms: A and B."));
	}

	@Test
	public void anImportRestoresTheSetting() throws Exception {
		ImportGate.setAllowed(false, ORIGIN);
		byte[] json = export();
		ImportGate.setAllowed(true, ORIGIN);
		FormatDetector.importData(json, "test.json", ADMIN, ORIGIN);
		assertFalse(ImportGate.isAllowed());
	}

	@Test
	public void importPermissionComesWithPreparationRegistrationSupervisorAndAdmin() {
		for (Role role : List.of(Role.PREPARATION, Role.REGISTRATION, Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasPermission(account(role), Permission.IMPORT_DATA, null));
		}
		for (Role role : List.of(Role.COMPETITION_DIRECTOR, Role.RESULTS, Role.WEIGHIN, Role.PLATFORM, Role.EXTERNAL)) {
			assertFalse(role.name(), AccessPolicy.hasPermission(account(role), Permission.IMPORT_DATA, null));
		}
		assertTrue("Simple Access",
		        AccessPolicy.hasPermission(Principal.admin(AuthSource.OFFICIALS_PIN), Permission.IMPORT_DATA, null));
	}

	@Test
	public void competitionDirectorImportIsRefusedEvenWhenAllowed() throws Exception {
		Principal director = account(Role.COMPETITION_DIRECTOR);
		assertTrue(ImportGate.isAllowed());
		for (ImportGate.Kind kind : ImportGate.Kind.values()) {
			ImportsNotAllowedException refused = assertThrows(ImportsNotAllowedException.class,
			        () -> ImportGate.requireAllowed(kind, director, ORIGIN));
			assertEquals(Reason.NO_PERMISSION, refused.getReason());
		}
		byte[] json = export();
		Long stamp = ConfigRepository.loadForEdit().getLastSavedAt();
		assertThrows(ImportsNotAllowedException.class, () -> FormatDetector.importData(json, "test.json", director,
		        ORIGIN));
		assertEquals("nothing was replaced", stamp, ConfigRepository.loadForEdit().getLastSavedAt());
	}

	@Test
	public void supervisorOpensTheSecurityTabButNotTheAuditKeys() {
		Principal supervisor = new Principal(AuthSource.ACCOUNT, 1L, "supervisor",
		        List.of(new RoleGrant(Role.COMPETITION_SUPERVISOR, null)), null);
		Principal admin = new Principal(AuthSource.ACCOUNT, 2L, "admin", List.of(new RoleGrant(Role.ADMIN, null)), null);
		PageRule adminPage = PageRule.of(AdminView.class);
		assertTrue(AccessPolicy.canOpen(supervisor, adminPage, null));
		assertTrue(AccessPolicy.hasPermission(supervisor, Permission.SECURITY_SETTINGS, null));
		assertFalse(AccessPolicy.hasPermission(supervisor, Permission.AUDIT_KEYS, null));
		assertFalse(AccessPolicy.canOpen(supervisor, PageRule.of(AuditIntegrityCheckView.class), null));
		assertTrue(AccessPolicy.canOpen(admin, adminPage, null));
		assertTrue(AccessPolicy.hasPermission(admin, Permission.SECURITY_SETTINGS, null));
		assertTrue(AccessPolicy.hasPermission(admin, Permission.AUDIT_KEYS, null));
		assertTrue(AccessPolicy.canOpen(admin, PageRule.of(AuditIntegrityCheckView.class), null));
		assertTrue(AccessPolicy.canOpen(supervisor, PageRule.of(ConfigContent.class), null));
	}
}
