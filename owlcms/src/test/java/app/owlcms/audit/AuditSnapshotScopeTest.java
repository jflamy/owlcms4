/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import app.owlcms.Main;
import app.owlcms.audit.AuditSnapshot.Scope;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

/** What each backup choice puts in the zip. */
public class AuditSnapshotScopeTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

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

	private static List<String> checkpointEntries(Instant since) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		AuditSnapshot.writeCheckpoint(bytes, AuditActor.system(), since);
		List<String> names = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
				names.add(entry.getName());
			}
		}
		return names;
	}

	@Test
	public void checkpointPutsTheExportFirstThenTheLogs() throws Exception {
		List<String> names = checkpointEntries(null);
		assertTrue(names.toString(), isExport(names.getFirst()));
		assertEquals(1, names.stream().filter(AuditSnapshotScopeTest::isExport).count());
		assertTrue(names.toString(), names.stream().skip(1).allMatch(name -> name.startsWith("logs/")));
		if (Files.isDirectory(AuditSnapshot.LOGS_DIRECTORY)) {
			assertTrue("the logs directory exists, so some log is zipped", names.size() > 1);
		}
	}

	@Test
	public void checkpointSinceAFutureTimeHasOnlyTheExport() throws Exception {
		List<String> names = checkpointEntries(Instant.now().plus(1, ChronoUnit.HOURS));
		assertEquals(names.toString(), 1, names.size());
		assertTrue(isExport(names.getFirst()));
	}

	@Test
	public void checkpointSelectsFilesModifiedAtOrAfterSince() throws Exception {
		Path root = temporary.newFolder("logs").toPath();
		Instant since = Instant.parse("2026-10-31T20:00:00Z");
		Path before = Files.writeString(root.resolve("before.log"), "old");
		Path at = Files.writeString(Files.createDirectories(root.resolve("audit")).resolve("at.log"), "edge");
		Path after = Files.writeString(root.resolve("owlcms.log"), "new");
		Path hidden = Files.writeString(root.resolve(".DS_Store"), "finder");
		Files.setLastModifiedTime(before, FileTime.from(since.minusMillis(1)));
		Files.setLastModifiedTime(at, FileTime.from(since));
		Files.setLastModifiedTime(after, FileTime.from(since.plusSeconds(60)));
		Files.setLastModifiedTime(hidden, FileTime.from(since.plusSeconds(60)));

		assertEquals(List.of("logs/audit/at.log", "logs/owlcms.log"), selected(root, since));
		assertEquals(List.of("logs/audit/at.log", "logs/before.log", "logs/owlcms.log"), selected(root, null));
	}

	private static List<String> selected(Path root, Instant since) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			AuditSnapshot.zipLogs(root, file -> AuditSnapshot.checkpointSelects(file, since), zip);
		}
		List<String> names = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
				names.add(entry.getName());
			}
		}
		return names;
	}

	@Test
	public void checkpointNameIsColonFreeAndParsesBack() {
		Instant time = Instant.parse("2026-10-31T20:37:42.123456Z");
		String name = AuditSnapshot.checkpointName(time);
		assertEquals("2026-10-31T20-37-42.123Z.zip", name);
		Instant parsed = AuditSnapshot.parseCheckpointTime(name);
		assertEquals(time.truncatedTo(ChronoUnit.MILLIS), parsed);
		assertFalse("never later than the selection time", parsed.isAfter(time));
		assertEquals(parsed, AuditSnapshot.parseCheckpointTime("2026-10-31T20-37-42.123Z"));
		assertEquals(parsed, AuditSnapshot.parseCheckpointTime("2026-10-31T20:37:42.123Z"));
		assertNull(AuditSnapshot.parseCheckpointTime(" "));
		assertThrows(DateTimeParseException.class, () -> AuditSnapshot.parseCheckpointTime("yesterday"));
	}
}
