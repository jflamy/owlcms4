package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import app.owlcms.tests.SlowCategory;

@Category(SlowCategory.class)
public class AuditLogPersistenceTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void productionBridgeAndShutdownHookProduceVerifiedFinalizedLogs() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "sealed");
		List<Path> files = AuditFileStoreTest.files(root.resolve("logs/audit"));
		assertEquals(2, files.size());
		for (Path file : files) {
			AuditFileStoreTest.Checked checked = AuditFileStoreTest.check(file).getFirst();
			assertTrue(checked.finalized());
			assertEquals(0, checked.unsealed());
			assertTrue(checked.publicKey().isBuiltin());
		}
		assertEquals(2, named(root, AuditFileStore.PLAIN_INFIX).size());
		assertFalse(Files.exists(root.resolve("logs/audit/A.log")));
	}

	@Test
	public void aRecordBeforeSealingStartsIsRefusedAndNothingIsWritten() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "unstarted");
		assertEquals("Audit sealing has not been started", Files.readString(root.resolve("refused.txt")));
		assertFalse(Files.exists(root.resolve("logs/audit")));
	}

	private static List<Path> named(Path root, String infix) throws IOException {
		try (Stream<Path> paths = Files.list(root.resolve("logs/audit"))) {
			return paths.filter(path -> path.getFileName().toString().contains(infix)).sorted().toList();
		}
	}

	@Test
	public void aPendingPemIsPromotedAndSeals() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "key");
		assertTrue(Files.exists(root.resolve("home/.owlcms/auditkey.pem")));
		assertFalse(Files.exists(root.resolve("home/.owlcms/auditkey.pem.new")));
		for (Path file : AuditFileStoreTest.files(root.resolve("logs/audit"))) {
			AuditFileStoreTest.Checked checked = AuditFileStoreTest.check(file).getFirst();
			assertTrue(checked.finalized());
			assertFalse(checked.publicKey().isBuiltin());
			assertTrue(Files.readString(file).contains("keySource=configured"));
		}
	}

	@Test
	public void forcedProcessStopLeavesTheCrashedFileUnchangedAcrossRestart() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "crash");
		List<Path> crashedFiles = AuditFileStoreTest.files(root.resolve("logs/audit"));
		Path file = crashedFiles.getFirst();
		byte[] before = Files.readAllBytes(file);
		AuditFileStoreTest.Checked crashed = AuditFileStoreTest.check(file).getFirst();
		assertFalse(crashed.finalized());
		assertEquals(1, crashed.unsealed());
		runFixture(root, "sealed");
		assertTrue(Arrays.equals(before, Files.readAllBytes(file)));
		List<AuditFileStoreTest.Checked> segments = AuditFileStoreTest.check(file);
		assertEquals(1, segments.size());
		assertFalse(segments.getFirst().finalized());
		assertEquals(1, segments.getFirst().unsealed());
		for (Path next : AuditFileStoreTest.files(root.resolve("logs/audit"))) {
			if (!crashedFiles.contains(next)) {
				assertTrue(AuditFileStoreTest.check(next).getFirst().finalized());
			}
		}
	}

	@Test
	public void savedLiftResultIsSealedImmediatelyAfterItsDecisionLine() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, AuditLiftFixture.class);
		Path decisionFile = null;
		for (Path file : AuditFileStoreTest.files(root.resolve("logs/audit"))) {
			for (AuditFileStoreTest.Checked checked : AuditFileStoreTest.check(file)) {
				assertTrue(checked.finalized());
				assertEquals(0, checked.unsealed());
			}
			if (Files.readString(file).contains("referee.decision")) {
				decisionFile = file;
			}
		}
		assertTrue("no referee.decision line written", decisionFile != null);
		List<String> lines = Files.readAllLines(decisionFile);
		int decisions = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains("referee.decision")) {
				decisions++;
				assertTrue(lines.get(i + 1), lines.get(i + 1).startsWith("SEAL "));
			}
		}
		assertTrue("expected several lifts, got " + decisions, decisions > 1);
	}

	@Test
	public void realV2ExportIsSignedAndAnEditedImportIsRecordedInvalid() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, ExportSigningFixture.class);
		String fingerprint = Files.readString(root.resolve("fingerprint.txt"));
		StringBuilder trail = new StringBuilder();
		for (Path file : AuditFileStoreTest.files(root.resolve("logs/audit"))) {
			trail.append(Files.readString(file));
		}
		List<String> lines = trail.toString().lines().toList();
		assertTrue("export.json records the key", lines.stream().anyMatch(line -> line.contains("export.json")
				&& line.contains("format=2,channel=download") && line.contains(",signature=" + fingerprint)));
		assertTrue("the edited import is recorded invalid", lines.stream().anyMatch(line -> line.contains(
				"application.import") && line.contains("source=edited.json")
				&& line.contains(",signature=invalid,reason=checksum,key=-,fingerprint=" + fingerprint)));
	}

	private static void runFixture(Path root, String mode) throws Exception {
		runFixture(root, AuditProcessFixture.class, mode);
	}

	private static void runFixture(Path root, Class<?> fixture, String... args) throws Exception {
		String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		Path java = Path.of(System.getProperty("java.home"), "bin", "java");
		List<String> command = new ArrayList<>(List.of(java.toString(), "-cp", classpath, fixture.getName()));
		command.addAll(List.of(args));
		ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile());
		builder.environment().remove(AuditKeyFiles.PEM_ENV);
		builder.environment().remove(AuditKeyFiles.PATH_ENV);
		builder.environment().remove("OWLCMS_FEATURESWITCHES");
		builder.redirectErrorStream(true);
		Path output = root.resolve("fixture-output.txt");
		builder.redirectOutput(output.toFile());
		Process process = builder.start();
		if (!process.waitFor(30, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IllegalStateException("Audit fixture JVM timed out: " + Files.readString(output));
		}
		assertEquals(Files.readString(output), 0, process.exitValue());
	}
}
