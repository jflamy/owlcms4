package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AuditFileStoreTest {
	static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T14:40:00Z"), ZoneOffset.UTC);
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void rolloverResetsSequencesAndLinksExactPriorFileAndBlock() throws Exception {
		Path root = temporary.getRoot().toPath();
		AuditSealLimits limits = new AuditSealLimits(4096, 500, Duration.ofMinutes(5),
				Duration.ofMinutes(5), 8192, 4000);
		List<String> readable = new ArrayList<>();
		try (AuditFileStore store = store(root, "run-1", limits, readable)) {
			for (int i = 0; i < 12; i++) {
				store.append("A", List.of(record("athlete.change", "change=" + i)), true);
			}
		}
		List<Path> files = files(root);
		assertTrue(files.size() >= 2);
		Checked previous = null;
		int seen = 0;
		for (Path file : files) {
			List<Checked> segments = check(file);
			assertEquals(1, segments.size());
			Checked segment = segments.getFirst();
			assertTrue(segment.finalized());
			assertEquals(0, segment.unsealed());
			assertEquals(1L, AuditFileStore.blockOf(segment.markers().getFirst().marker()).firstLine());
			if (previous == null) {
				assertTrue(segment.markers().getFirst().marker() instanceof AuditMarker.First);
			} else {
				AuditMarker.Continue next = (AuditMarker.Continue) segment.markers().getFirst().marker();
				AuditBlockChain.checkContinuation(next, previous.markers().getLast(), segment.publicKey());
			}
			seen += (int) segment.recordCount() - 1;
			previous = segment;
		}
		assertEquals(12, seen);
		assertEquals(12 + files.size(), readable.size());
	}

	@Test
	public void restartAppendsFirstAndNewRunWithoutChangingPreviousBytes() throws Exception {
		Path root = temporary.getRoot().toPath();
		try (AuditFileStore store = store(root, "run-1", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			store.append("A", List.of(record("referee.decision", "result=GOOD")), true);
		}
		Path file = files(root).getFirst();
		byte[] before = Files.readAllBytes(file);
		try (AuditFileStore store = store(root, "run-2", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			store.append("A", List.of(record("referee.decision", "result=NO_LIFT")), true);
		}
		assertEquals(1, files(root).size());
		byte[] after = Files.readAllBytes(file);
		assertTrue(after.length > before.length);
		for (int i = 0; i < before.length; i++) {
			assertEquals(before[i], after[i]);
		}
		List<Checked> segments = check(file);
		assertEquals(2, segments.size());
		assertEquals("run-1", segments.get(0).markers().getFirst().marker().identity().runId());
		assertEquals("run-2", segments.get(1).markers().getFirst().marker().identity().runId());
		assertTrue(segments.stream().allMatch(Checked::finalized));
		assertTrue(segments.get(1).markers().getFirst().marker() instanceof AuditMarker.First);
		assertEquals(2, segments.get(1).recordCount());
	}

	@Test
	public void crashTailIsNotResignedWhenTheNextRunAppends() throws Exception {
		Path root = temporary.getRoot().toPath();
		AuditFileStore crashed = store(root, "crashed", AuditSealLimits.DEFAULT, new ArrayList<>());
		crashed.append("A", List.of(record("athlete.change", "bodyWeight=81")), true);
		crashed.append("A", List.of(record("clock.start", "clock=1:00")), false);
		crashed.abort();
		Path file = files(root).getFirst();
		List<Checked> before = check(file);
		assertFalse(before.getFirst().finalized());
		assertEquals(1, before.getFirst().unsealed());
		String old = Files.readString(file);
		try (AuditFileStore restarted = store(root, "restarted", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			restarted.append("A", List.of(record("clock.stop", "clock=0:52")), false);
		}
		assertTrue(Files.readString(file).startsWith(old));
		List<Checked> segments = check(file);
		assertEquals(2, segments.size());
		assertFalse(segments.getFirst().finalized());
		assertEquals(1, segments.getFirst().unsealed());
		assertTrue(segments.getLast().finalized());
		assertEquals(0, segments.getLast().unsealed());
	}

	@Test
	public void incompleteLastLineIsPreservedAndNewHeaderStartsOnANewLine() throws Exception {
		Path root = temporary.getRoot().toPath();
		try (AuditFileStore store = store(root, "old", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			store.append("A", List.of(record("clock.start", "clock=1:00")), true);
		}
		Path file = files(root).getFirst();
		Files.writeString(file, "  999 | incomplete", StandardOpenOption.APPEND);
		String before = Files.readString(file);
		try (AuditFileStore store = store(root, "new", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			store.append("A", List.of(record("clock.stop", "clock=0:59")), true);
		}
		String after = Files.readString(file);
		assertTrue(after.startsWith(before + "\n    1 | "));
		assertTrue(after.contains("runId=new"));
	}

	@Test
	public void publicationPreventsMutableValuesFromChangingAfterCapture() throws Exception {
		StringBuilder value = new StringBuilder("original");
		AuditEntry entry = AuditEntry.builder("A", "athlete.change").actor(AuditActor.system())
				.field("name").newValue(value).build();
		AuditRecordSnapshot snapshot = AuditRecordSnapshot.capture(entry, OffsetDateTime.now(CLOCK));
		value.replace(0, value.length(), "changed");
		Path root = temporary.getRoot().toPath();
		try (AuditFileStore store = store(root, "run-1", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			store.append("A", List.of(snapshot), true);
		}
		String log = Files.readString(files(root).getFirst());
		assertTrue(log.contains("name - -> original"));
		assertFalse(log.contains("name - -> changed"));
		check(files(root).getFirst());
	}

	@Test
	public void distinctStreamsRemainIndependentAndNameCollisionsAreRejected() throws Exception {
		Path root = temporary.getRoot().toPath();
		try (AuditFileStore store = store(root, "run-1", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			store.append("A", List.of(record("clock.start", "platform=A")), true);
			store.append("B", List.of(record("clock.start", "platform=B")), true);
			store.append("A/B", List.of(record("clock.start", "platform=other")), true);
			assertThrows(IOException.class,
					() -> store.append("A_B", List.of(record("clock.start", "collision")), true));
		}
		assertEquals(3, files(root).size());
		for (Path file : files(root)) {
			assertTrue(check(file).getFirst().finalized());
		}
	}

	@Test
	public void forcedRolloverFinalizesCurrentFilesAndZipExcludesTheOpenOnes() throws Exception {
		Path logs = temporary.newFolder("logs").toPath();
		Path root = Files.createDirectories(logs.resolve("audit"));
		Files.writeString(logs.resolve("owlcms.log"), "application log\n");
		Files.writeString(root.resolve("A.log"), "readable log\n");
		try (AuditFileStore store = store(root, "run-1", AuditSealLimits.DEFAULT, new ArrayList<>())) {
			store.append("A", List.of(record("referee.decision", "result=GOOD")), true);
			store.append("B", List.of(record("referee.decision", "result=NO_LIFT")), false);
			java.util.Set<Path> open = store.rollAll();
			assertEquals(2, open.size());
			List<Path> all = files(root);
			assertEquals(4, all.size());
			for (Path file : all) {
				Checked checked = check(file).getFirst();
				boolean isOpen = open.stream().anyMatch(path -> path.getFileName().equals(file.getFileName()));
				assertEquals(!isOpen, checked.finalized());
				if (isOpen) {
					assertTrue(checked.markers().getFirst().marker() instanceof AuditMarker.Continue);
				}
			}
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes)) {
				AuditSnapshot.zipLogs(logs, open, zip);
			}
			Path extracted = temporary.newFolder("snapshot").toPath();
			List<String> entries = new ArrayList<>();
			try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(
					new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
				for (java.util.zip.ZipEntry entry; (entry = in.getNextEntry()) != null;) {
					entries.add(entry.getName());
					Path target = extracted.resolve(entry.getName());
					Files.createDirectories(target.getParent());
					Files.copy(in, target);
				}
			}
			assertTrue(entries.contains("logs/owlcms.log"));
			assertTrue(entries.contains("logs/audit/A.log"));
			assertEquals(4, entries.size());
			for (Path path : open) {
				assertFalse(entries.contains("logs/audit/" + path.getFileName()));
			}
			AuditIntegrityChecker.Report report = AuditIntegrityChecker.check(extracted.resolve("logs/audit"));
			assertEquals(AuditIntegrityChecker.Integrity.INTACT, report.integrity());
			assertEquals(AuditIntegrityChecker.Coverage.COMPLETE, report.coverage());
		}
	}

	@Test
	public void weighInAndRegistrationOnlyAreSaveBoundaries() {
		assertTrue(AthleteAudit.sealsSave(AuditActor.device("WEIGHIN", null, "-")));
		assertTrue(AthleteAudit.sealsSave(AuditActor.device("REGISTRATION", null, "-")));
		assertFalse(AthleteAudit.sealsSave(AuditActor.device("MARSHAL", null, "-")));
		assertFalse(AthleteAudit.sealsSave(AuditActor.system()));
	}

	static AuditRecordSnapshot record(String action, String detail) {
		return AuditRecordSnapshot.capture(AuditEntry.builder("A", action).actor(AuditActor.system())
				.detail(detail).build(), OffsetDateTime.now(CLOCK));
	}

	private static AuditFileStore store(Path root, String run, AuditSealLimits limits, List<String> readable)
			throws Exception {
		return new AuditFileStore(root, AuditSigningKey.builtin(), limits, CLOCK, new AtomicLong()::get,
				run, "69.0.0", (platform, line) -> readable.add(line));
	}

	static List<Path> files(Path root) throws IOException {
		try (Stream<Path> paths = Files.list(root)) {
			return paths.filter(path -> path.getFileName().toString().contains("_full_"))
					.sorted().toList();
		}
	}

	static List<Checked> check(Path file) throws Exception {
		List<Checked> result = new ArrayList<>();
		AuditBlockChain chain = null;
		AuditPublicKey key = null;
		List<AuditMarkerCodec.Signed> markers = new ArrayList<>();
		Map<Long, byte[]> records = new HashMap<>();
		long recordCount = 0;
		for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
			boolean markerLine = line.startsWith("FIRST ") || line.startsWith("CONTINUE ")
					|| line.startsWith("SEAL ") || line.startsWith("FINAL ");
			if (!markerLine) {
				long sequence = Long.parseLong(line.substring(0, line.indexOf(" | ")).strip());
				if (sequence == 1) {
					if (chain != null) {
						result.add(new Checked(List.copyOf(markers), key, chain.isFinalized(), records.size(), recordCount));
					}
					Matcher publicKey = Pattern.compile("publicKey=\"([A-Za-z0-9+/=]+)\"").matcher(line);
					assertTrue(publicKey.find());
					key = AuditPublicKey.fromBase64(publicKey.group(1));
					chain = new AuditBlockChain();
					markers.clear();
					records.clear();
					recordCount = 0;
				}
				records.put(sequence, (line + "\n").getBytes(StandardCharsets.UTF_8));
				recordCount++;
			} else {
				AuditMarkerCodec.Signed signed = AuditMarkerCodec.parse(line);
				assertEquals(file.getFileName().toString(), signed.marker().identity().artifactName());
				AuditMarker.Block block = AuditFileStore.blockOf(signed.marker());
				ByteArrayOutputStream covered = new ByteArrayOutputStream();
				if (block != null) {
					for (long n = block.firstLine(); n <= block.lastLine(); n++) {
						byte[] bytes = records.remove(n);
						assertTrue("missing record " + n, bytes != null);
						covered.writeBytes(bytes);
					}
				}
				chain.accept(signed, covered.toByteArray(), key);
				markers.add(signed);
			}
		}
		if (chain != null) {
			result.add(new Checked(List.copyOf(markers), key, chain.isFinalized(), records.size(), recordCount));
		}
		return result;
	}

	record Checked(List<AuditMarkerCodec.Signed> markers, AuditPublicKey publicKey,
			boolean finalized, int unsealed, long recordCount) {
	}
}
