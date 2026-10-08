package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
		assertTrue(Files.exists(root.resolve("logs/audit/A.log")));
		assertFalse(Files.exists(root.resolve("logs/audit/A_full.log")));
	}

	@Test
	public void integrityOffPreservesPlainLogNamesAndHasNoMarkersOrKeys() throws Exception {
		Path root = temporary.newFolder().toPath();
		runFixture(root, "plain");
		Path full = root.resolve("logs/audit/A_full.log");
		assertTrue(Files.exists(full));
		String log = Files.readString(full);
		assertFalse(log.contains("FIRST "));
		assertFalse(log.contains("SEAL "));
		assertFalse(log.contains("FINAL "));
		assertFalse(log.contains("publicKey="));
		assertTrue(log.contains("bodyWeight - -> 81.35"));
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
