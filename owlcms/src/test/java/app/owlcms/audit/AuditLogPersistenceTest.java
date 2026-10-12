package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
		assertTrue(named(root, UnsealedAuditFiles.FULL_INFIX).isEmpty());
		assertFalse(Files.exists(root.resolve("logs/audit/A.log")));
	}

	@Test
	public void integrityOffPreservesPlainLogNamesAndHasNoMarkersOrKeys() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "plain");
		assertTrue(named(root, AuditFileStore.SEALED_INFIX).isEmpty());
		List<Path> fullFiles = named(root, UnsealedAuditFiles.FULL_INFIX);
		assertEquals(1, fullFiles.size());
		Path full = fullFiles.getFirst();
		assertTrue(full.getFileName().toString().startsWith("A_full_"));
		String log = Files.readString(full);
		assertFalse(log.contains("FIRST "));
		assertFalse(log.contains("SEAL "));
		assertFalse(log.contains("FINAL "));
		assertFalse(log.contains("publicKey="));
		assertTrue(log.contains("audit.open"));
		assertTrue(log.contains("bodyWeight - -> 81.35"));

		Path plain = full.resolveSibling(full.getFileName().toString()
				.replace(UnsealedAuditFiles.FULL_INFIX, AuditFileStore.PLAIN_INFIX));
		List<String> plainLines = Files.readAllLines(plain);
		assertEquals(3, plainLines.size());
		assertFalse(String.join("\n", plainLines).contains("audit.open"));
		assertTrue(plainLines.getFirst().startsWith("    2 | "));
		assertFalse(plainLines.getFirst().contains(" | cause="));

		runFixture(root, "plain");
		assertEquals("each run starts a new unsealed pair", 2, named(root, UnsealedAuditFiles.FULL_INFIX).size());
	}

	private static List<Path> named(Path root, String infix) throws IOException {
		try (Stream<Path> paths = Files.list(root.resolve("logs/audit"))) {
			return paths.filter(path -> path.getFileName().toString().contains(infix)).sorted().toList();
		}
	}

	@Test
	public void forcedKeyTurnsOnRealProductionSealingAndPromotesPendingPem() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "forced");
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
	public void forcedProcessStopPreservesUnsealedTailAcrossRestart() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "crash");
		Path file = AuditFileStoreTest.files(root.resolve("logs/audit")).getFirst();
		String before = Files.readString(file);
		AuditFileStoreTest.Checked crashed = AuditFileStoreTest.check(file).getFirst();
		assertFalse(crashed.finalized());
		assertEquals(1, crashed.unsealed());
		runFixture(root, "sealed");
		assertTrue(Files.readString(file).startsWith(before));
		List<AuditFileStoreTest.Checked> segments = AuditFileStoreTest.check(file);
		assertEquals(2, segments.size());
		assertFalse(segments.getFirst().finalized());
		assertEquals(1, segments.getFirst().unsealed());
		assertTrue(segments.getLast().finalized());
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
