package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import app.owlcms.audit.AuditIntegrityChecker.Coverage;
import app.owlcms.audit.AuditIntegrityChecker.Integrity;
import app.owlcms.audit.AuditIntegrityChecker.Inventory;
import app.owlcms.audit.AuditIntegrityChecker.KeyProtection;
import app.owlcms.audit.AuditIntegrityChecker.Report;

public class AuditIntegrityCheckerTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void intactFinalizedBuiltinEvidenceIsComplete() throws Exception {
		Path root = temporary.newFolder().toPath();
		Path file = sealed(root, AuditSigningKey.builtin(), "run-1");

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.COMPLETE, report.coverage());
		assertEquals(KeyProtection.BUILTIN, report.keyProtection());
		assertTrue(report.failures().isEmpty());
		assertEquals(1, report.segments().size());
		assertTrue(report.segments().getFirst().finalized());
		assertEquals(file.getFileName().toString(), report.segments().getFirst().fileName());
		assertEquals(AuditSigningKey.BUILTIN_FINGERPRINT,
				report.fingerprints().getFirst().fingerprint());
		assertEquals(List.of(new AuditIntegrityChecker.RecordRange(1, 3)),
				report.fingerprints().getFirst().coveredRanges().getFirst().ranges());
		assertTrue(report.fingerprints().getFirst().coveredRanges().getFirst().firstSealedAt() != null);
		assertTrue(report.fingerprints().getFirst().coveredRanges().getFirst().lastSealedAt() != null);
	}

	@Test
	public void crashTailIsIntactButPartialAndReportsExactUnvalidatedRange() throws Exception {
		Path root = temporary.newFolder().toPath();
		AuditFileStore store = store(root, AuditSigningKey.builtin(), "crashed", AuditSealLimits.DEFAULT);
		store.append("A", List.of(record("athlete.change", "change=1")), true);
		store.append("A", List.of(record("clock.start", "clock=1:00")), false);
		store.abort();

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.PARTIAL, report.coverage());
		assertEquals(List.of(new AuditIntegrityChecker.RecordRange(3, 3)),
				report.segments().getFirst().unvalidatedRanges());
		assertFalse(report.segments().getFirst().finalized());
	}

	@Test
	public void ordinaryAuditLogIsUnsealedAndNotAssessed() throws Exception {
		Path root = temporary.newFolder().toPath();
		Files.writeString(root.resolve("A_full.log"), "    1 | ordinary audit record\n");

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(Integrity.UNSEALED, report.integrity());
		assertEquals(Coverage.NOT_ASSESSED, report.coverage());
		assertEquals(KeyProtection.NONE, report.keyProtection());
	}

	@Test
	public void editsInsertionsDeletionsReorderingAndMarkerReplacementFail() throws Exception {
		assertMutationFails(lines -> replaceFirst(lines, "change=1", line -> line.replace("change=1", "change=9")));
		assertMutationFails(lines -> insertBefore(lines, "SEAL ", find(lines, "change=1")));
		assertMutationFails(lines -> removeFirst(lines, "change=1"));
		assertMutationFails(lines -> swapRecords(lines, "change=1", "change=2"));
		assertMutationFails(lines -> replaceFirst(lines, "\"signature\":\"",
				line -> replaceSignatureCharacter(line)));
	}

	@Test
	public void failedBlockIsReportedAsUnvalidated() throws Exception {
		Path root = temporary.newFolder().toPath();
		Path file = sealed(root, AuditSigningKey.builtin(), "run-1");
		List<String> lines = Files.readAllLines(file);
		int changed = find(lines, "change=1");
		lines.set(changed, lines.get(changed).replace("change=1", "change=9"));
		Files.write(file, lines, StandardCharsets.UTF_8);

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(Integrity.FAILED, report.integrity());
		assertEquals(List.of(new AuditIntegrityChecker.RecordRange(2, 3)),
				report.segments().getFirst().unvalidatedRanges());
	}

	@Test
	public void publicKeyReplacementAndCompleteBlockDeletionFail() throws Exception {
		AuditSigningKey replacement = AuditSigningKey.generate();
		assertMutationFails(lines -> replaceFirst(lines, "publicKey=",
				line -> line.replace(publicKeyValue(line), replacement.publicKeyBase64())));
		assertMutationFails(lines -> {
			List<String> changed = new ArrayList<>(lines);
			changed.removeIf(line -> line.contains("change=1") || line.contains("change=2")
					|| line.startsWith("SEAL "));
			return changed;
		});
	}

	@Test
	public void sealedBlockReorderingFails() throws Exception {
		Path root = temporary.newFolder().toPath();
		Path file;
		try (AuditFileStore store = store(root, AuditSigningKey.builtin(), "run-1",
				AuditSealLimits.DEFAULT)) {
			store.append("A", List.of(record("athlete.change", "change=1")), true);
			store.append("A", List.of(record("athlete.change", "change=2")), true);
		}
		file = AuditFileStoreTest.files(root).getFirst();
		List<String> lines = Files.readAllLines(file);
		int first = find(lines, "change=1");
		int second = find(lines, "change=2");
		int end = find(lines, "FINAL ");
		List<String> reordered = new ArrayList<>(lines.subList(0, first));
		reordered.addAll(lines.subList(second, end));
		reordered.addAll(lines.subList(first, second));
		reordered.addAll(lines.subList(end, lines.size()));
		Files.write(file, reordered, StandardCharsets.UTF_8);

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(Integrity.FAILED, report.integrity());
		assertEquals(Coverage.PARTIAL, report.coverage());
	}

	@Test
	public void incompleteMarkerPublicationIsIntactButPartial() throws Exception {
		Path root = temporary.newFolder().toPath();
		Path file = sealed(root, AuditSigningKey.builtin(), "run-1");
		List<String> lines = Files.readAllLines(file);
		String end = lines.getLast();
		Files.writeString(file, String.join("\n", lines.subList(0, lines.size() - 1))
				+ "\n" + end.substring(0, end.length() / 2));

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.PARTIAL, report.coverage());
		assertTrue(report.warnings().stream().anyMatch(message -> message.contains("incomplete trailing audit marker")));
	}

	@Test
	public void unparsableMarkerFollowedByMoreContentFails() throws Exception {
		assertMutationFails(lines -> replaceFirst(lines, "SEAL ", line -> "SEAL {\"byteCount\":"));
	}

	@Test
	public void restartAfterPartialMarkerPublicationKeepsOldTailUnvalidated() throws Exception {
		Path root = temporary.newFolder().toPath();
		AuditFileStore crashed = store(root, AuditSigningKey.builtin(), "crashed", AuditSealLimits.DEFAULT);
		crashed.append("A", List.of(record("athlete.change", "change=1")), true);
		crashed.abort();
		Path file = AuditFileStoreTest.files(root).getFirst();
		Files.writeString(file, "SEAL {\"byteCount\":", StandardOpenOption.APPEND);
		try (AuditFileStore restarted = store(root, AuditSigningKey.builtin(), "restarted",
				AuditSealLimits.DEFAULT)) {
			restarted.append("A", List.of(record("clock.start", "clock=1:00")), true);
		}

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.PARTIAL, report.coverage());
		assertEquals(2, report.segments().size());
		assertFalse(report.segments().getFirst().finalized());
		assertTrue(report.segments().getLast().finalized());
	}

	@Test
	public void rolloverAndRestartAfterRolloverFormCompleteEvidence() throws Exception {
		Path root = temporary.newFolder().toPath();
		AuditSealLimits limits = rolloverLimits();
		try (AuditFileStore store = store(root, AuditSigningKey.builtin(), "run-1", limits)) {
			for (int i = 0; i < 10; i++) {
				store.append("A", List.of(record("athlete.change", "change=" + i)), true);
			}
		}
		try (AuditFileStore store = store(root, AuditSigningKey.builtin(), "run-2", limits)) {
			store.append("A", List.of(record("clock.start", "clock=1:00")), true);
		}

		Report report = AuditIntegrityChecker.check(root);

		assertTrue(AuditFileStoreTest.files(root).size() > 1);
		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.COMPLETE, report.coverage());
		assertTrue(report.segments().stream().anyMatch(segment -> "run-1".equals(segment.runId())));
		assertTrue(report.segments().stream().anyMatch(segment -> "run-2".equals(segment.runId())));
		assertEquals(1, report.fingerprints().size());
		assertEquals(report.segments().size(), report.fingerprints().getFirst().coveredRanges().size());
	}

	@Test
	public void independentRolledFileWarnsThatPredecessorWasNotChecked() throws Exception {
		Path root = temporary.newFolder().toPath();
		try (AuditFileStore store = store(root, AuditSigningKey.builtin(), "run-1", rolloverLimits())) {
			for (int i = 0; i < 10; i++) {
				store.append("A", List.of(record("athlete.change", "change=" + i)), true);
			}
		}
		Path last = AuditFileStoreTest.files(root).getLast();

		Report report = AuditIntegrityChecker.check(last);

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.PARTIAL, report.coverage());
		assertTrue(report.warnings().stream().anyMatch(message -> message.contains("predecessor not checked")));
	}

	@Test
	public void inventoryDetectsMissingStreamAndFile() throws Exception {
		Path root = temporary.newFolder().toPath();
		sealed(root, AuditSigningKey.builtin(), "run-1");

		Report report = AuditIntegrityChecker.check(root,
				new Inventory(java.util.Set.of("A", "B"), java.util.Set.of("missing_full.log")));

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.PARTIAL, report.coverage());
		assertTrue(report.warnings().stream().anyMatch(message -> message.contains("stream is missing: B")));
		assertTrue(report.warnings().stream().anyMatch(message -> message.contains("file is missing")));
	}

	@Test
	public void configuredKeyEvidenceReportsConfiguredProtection() throws Exception {
		Path root = temporary.newFolder().toPath();
		sealed(root, AuditSigningKey.generate(), "configured-run");

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.COMPLETE, report.coverage());
		assertEquals(KeyProtection.CONFIGURED, report.keyProtection());
	}

	@Test
	public void configuredAndBuiltinSegmentsReportMixedKeyProtection() throws Exception {
		Path root = temporary.newFolder().toPath();
		sealed(root, AuditSigningKey.builtin(), "builtin-run");
		try (AuditFileStore store = store(root, AuditSigningKey.generate(), "configured-run",
				AuditSealLimits.DEFAULT)) {
			store.append("A", List.of(record("clock.start", "clock=1:00")), true);
		}

		Report report = AuditIntegrityChecker.check(root);

		assertEquals(report.failures().toString(), Integrity.INTACT, report.integrity());
		assertEquals(Coverage.COMPLETE, report.coverage());
		assertEquals(KeyProtection.MIXED, report.keyProtection());
		assertEquals(2, report.segments().size());
		assertEquals(2, report.fingerprints().size());
	}

	private void assertMutationFails(UnaryOperator<List<String>> mutation) throws Exception {
		Path root = temporary.newFolder().toPath();
		Path file = sealed(root, AuditSigningKey.builtin(), "run-1");
		List<String> changed = mutation.apply(new ArrayList<>(Files.readAllLines(file)));
		Files.write(file, changed, StandardCharsets.UTF_8);
		Report report = AuditIntegrityChecker.check(root);
		assertEquals(report.failures().toString(), Integrity.FAILED, report.integrity());
		assertEquals(Coverage.PARTIAL, report.coverage());
	}

	private static Path sealed(Path root, AuditSigningKey key, String run) throws Exception {
		try (AuditFileStore store = store(root, key, run, AuditSealLimits.DEFAULT)) {
			store.append("A", List.of(record("athlete.change", "change=1"),
					record("athlete.change", "change=2")), true);
		}
		return AuditFileStoreTest.files(root).getLast();
	}

	private static AuditFileStore store(Path root, AuditSigningKey key, String run, AuditSealLimits limits)
			throws Exception {
		return new AuditFileStore(root, key, limits, AuditFileStoreTest.CLOCK, new AtomicLong()::get,
				run, "69.0.0", (platform, line) -> {
				});
	}

	private static AuditRecordSnapshot record(String action, String detail) {
		return AuditFileStoreTest.record(action, detail);
	}

	private static AuditSealLimits rolloverLimits() {
		return new AuditSealLimits(4096, 500, Duration.ofMinutes(5),
				Duration.ofMinutes(5), 8192, 4000);
	}

	private static List<String> replaceFirst(List<String> lines, String token, UnaryOperator<String> replacement) {
		List<String> changed = new ArrayList<>(lines);
		int index = find(changed, token);
		changed.set(index, replacement.apply(changed.get(index)));
		return changed;
	}

	private static List<String> insertBefore(List<String> lines, String token, int source) {
		List<String> changed = new ArrayList<>(lines);
		changed.add(find(changed, token), changed.get(source));
		return changed;
	}

	private static List<String> removeFirst(List<String> lines, String token) {
		List<String> changed = new ArrayList<>(lines);
		changed.remove(find(changed, token));
		return changed;
	}

	private static List<String> swapRecords(List<String> lines, String first, String second) {
		List<String> changed = new ArrayList<>(lines);
		int firstIndex = find(changed, first);
		int secondIndex = find(changed, second);
		String saved = changed.get(firstIndex);
		changed.set(firstIndex, changed.get(secondIndex));
		changed.set(secondIndex, saved);
		return changed;
	}

	private static int find(List<String> lines, String token) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains(token)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Token not found: " + token);
	}

	private static String replaceSignatureCharacter(String line) {
		int start = line.indexOf("\"signature\":\"") + "\"signature\":\"".length();
		char replacement = line.charAt(start) == 'A' ? 'B' : 'A';
		return line.substring(0, start) + replacement + line.substring(start + 1);
	}

	private static String publicKeyValue(String line) {
		int start = line.indexOf("publicKey=") + "publicKey=".length();
		int end = line.indexOf(',', start);
		return line.substring(start, end);
	}
}
