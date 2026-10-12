package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UnsealedAuditFilesTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void plainAndFullFilesPairAndRollTogetherAtTheSizeLimit() throws Exception {
		Path root = temporary.getRoot().toPath();
		UnsealedAuditFiles files = new UnsealedAuditFiles(root, 1024, "version=69.0.0,runId=run-1");
		OffsetDateTime start = OffsetDateTime.now(AuditFileStoreTest.CLOCK);
		for (int i = 0; i < 10; i++) {
			files.write(AuditEntry.builder("A", "athlete.change").actor(AuditActor.system())
					.detail("change=" + i).build(), start.plusSeconds(i));
		}
		List<Path> full = named(root, UnsealedAuditFiles.FULL_INFIX);
		List<Path> plain = named(root, AuditFileStore.PLAIN_INFIX);
		assertTrue("expected a size rollover", full.size() >= 2);
		assertEquals(full.size(), plain.size());
		int events = 0;
		for (int i = 0; i < full.size(); i++) {
			assertEquals(full.get(i).getFileName().toString().replace(UnsealedAuditFiles.FULL_INFIX,
					AuditFileStore.PLAIN_INFIX), plain.get(i).getFileName().toString());
			List<String> fullLines = Files.readAllLines(full.get(i), StandardCharsets.UTF_8);
			List<String> plainLines = Files.readAllLines(plain.get(i), StandardCharsets.UTF_8);
			assertTrue(fullLines.getFirst().startsWith("    1 | ") && fullLines.getFirst().contains("audit.open"));
			assertEquals(fullLines.size() - 1, plainLines.size());
			for (int line = 0; line < plainLines.size(); line++) {
				String expected = fullLines.get(line + 1);
				assertEquals(expected.substring(0, expected.indexOf(" | cause=")), plainLines.get(line));
				assertFalse(plainLines.get(line).contains("audit.open"));
			}
			events += plainLines.size();
		}
		assertEquals(10, events);
		assertEquals(AuditIntegrityChecker.Integrity.UNSEALED, AuditIntegrityChecker.check(root).integrity());
	}

	private static List<Path> named(Path root, String infix) throws Exception {
		try (Stream<Path> paths = Files.list(root)) {
			return paths.filter(path -> path.getFileName().toString().contains(infix)).sorted().toList();
		}
	}
}
