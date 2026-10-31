/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditSnapshot.Scope;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

/** What each backup choice puts in the zip. */
public class AuditSnapshotScopeTest {

	@BeforeClass
	public static void setUpDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		if (CompetitionRepository.findAll().isEmpty()) {
			CompetitionRepository.create(new Competition(), AuditOrigin.system("test"));
		}
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	private static List<String> entries(Scope scope) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		AuditSnapshot.writeTo(bytes, scope, AuditActor.system());
		List<String> names = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
				names.add(entry.getName());
			}
		}
		return names;
	}

	private static boolean isExport(String name) {
		return name.startsWith("database_v2_") && name.endsWith(".json");
	}

	@Test
	public void databaseExportOnly() throws Exception {
		List<String> names = entries(Scope.EXPORT);
		assertEquals(names.toString(), 1, names.size());
		assertTrue(names.toString(), isExport(names.get(0)));
	}

	@Test
	public void logsOnly() throws Exception {
		List<String> names = entries(Scope.LOGS);
		assertFalse(names.toString(), names.stream().anyMatch(AuditSnapshotScopeTest::isExport));
		assertTrue(names.toString(), names.stream().allMatch(name -> name.startsWith("logs/")));
		if (Files.isDirectory(AuditSnapshot.LOGS_DIRECTORY)) {
			assertFalse("the logs directory exists, so some log is zipped", names.isEmpty());
		}
	}

	@Test
	public void exportAndLogs() throws Exception {
		List<String> names = entries(Scope.LOGS_AND_EXPORT);
		assertEquals(1, names.stream().filter(AuditSnapshotScopeTest::isExport).count());
		assertTrue(names.stream().allMatch(name -> isExport(name) || name.startsWith("logs/")));
	}

	@Test
	public void anomalyIsRecordedAndTheZipIsTheUsualOne() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		AuditSnapshot.writeTo(bytes, Scope.LOGS_AND_EXPORT, AuditActor.system(), "clock\nstopped");
		List<String> names = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
				names.add(entry.getName());
			}
		}
		assertEquals(1, names.stream().filter(AuditSnapshotScopeTest::isExport).count());
	}

	@Test
	public void anomalyDescriptionIsOneBoundedLine() {
		assertEquals("clock stopped", AuditSnapshot.describe("clock\nstopped"));
		assertEquals("-", AuditSnapshot.describe("  \n "));
		assertEquals(500, AuditSnapshot.describe("x".repeat(800)).length());
	}
}
