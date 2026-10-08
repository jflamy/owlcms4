package app.owlcms.audit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Read-only verification of sealed audit files. */
public final class AuditIntegrityChecker {
	private static final Pattern PUBLIC_KEY =
			Pattern.compile("(?:^|,)publicKey=\"?([A-Za-z0-9+/=]+)\"?(?:,|$)");

	public enum Integrity {
		INTACT, FAILED, UNSEALED
	}

	public enum Coverage {
		COMPLETE, PARTIAL, NOT_ASSESSED
	}

	public enum KeyProtection {
		CONFIGURED, BUILTIN, MIXED, NONE
	}

	public record RecordRange(long firstRecord, long lastRecord) {
		public RecordRange {
			if (firstRecord < 1 || lastRecord < firstRecord) {
				throw new IllegalArgumentException("Invalid audit record range");
			}
		}
	}

	public record SegmentResult(String fileName, String stream, String runId, String fingerprint,
			boolean builtinKey, boolean finalized, List<RecordRange> validatedRanges,
			List<RecordRange> unvalidatedRanges, Instant firstSealedAt, Instant lastSealedAt) {
		public SegmentResult {
			validatedRanges = List.copyOf(validatedRanges);
			unvalidatedRanges = List.copyOf(unvalidatedRanges);
		}
	}

	public record CoveredRange(String fileName, String stream, String runId, List<RecordRange> ranges,
			Instant firstSealedAt, Instant lastSealedAt) {
		public CoveredRange {
			ranges = List.copyOf(ranges);
		}
	}

	public record FingerprintCoverage(String fingerprint, boolean builtinKey,
			List<CoveredRange> coveredRanges) {
		public FingerprintCoverage {
			coveredRanges = List.copyOf(coveredRanges);
		}
	}

	public record Inventory(Set<String> streams, Set<String> files) {
		public static final Inventory NONE = new Inventory(Set.of(), Set.of());

		public Inventory {
			streams = Set.copyOf(streams);
			files = Set.copyOf(files);
		}
	}

	public record Report(Integrity integrity, Coverage coverage, KeyProtection keyProtection,
			List<SegmentResult> segments, List<FingerprintCoverage> fingerprints,
			List<String> failures, List<String> warnings) {
		public Report {
			segments = List.copyOf(segments);
			fingerprints = List.copyOf(fingerprints);
			failures = List.copyOf(failures);
			warnings = List.copyOf(warnings);
		}
	}

	private AuditIntegrityChecker() {
	}

	public static Report check(Path evidence) throws IOException {
		return check(evidence, Inventory.NONE);
	}

	public static Report check(Path evidence, Inventory inventory) throws IOException {
		List<Path> files = evidenceFiles(evidence);
		List<SegmentState> segments = new ArrayList<>();
		List<String> failures = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		Set<String> unsealedFiles = new LinkedHashSet<>();
		for (Path file : files) {
			checkFile(file, segments, failures, warnings, unsealedFiles);
		}
		if (segments.isEmpty()) {
			if (!inventory.files().isEmpty() || !inventory.streams().isEmpty()) {
				warnings.add("Expected sealed audit evidence is missing");
			}
			return new Report(Integrity.UNSEALED, Coverage.NOT_ASSESSED, KeyProtection.NONE,
					List.of(), List.of(), List.copyOf(failures), List.copyOf(warnings));
		}

		boolean partial = !unsealedFiles.isEmpty();
		for (String file : unsealedFiles) {
			warnings.add("Unsealed audit file was not assessed: " + file);
		}
		partial |= checkContinuations(segments, files, failures, warnings);
		partial |= checkRunRoots(segments, failures, warnings);
		partial |= checkInventory(segments, files, inventory, warnings);

		List<SegmentResult> results = new ArrayList<>();
		Map<String, List<CoveredRange>> fingerprintRanges = new LinkedHashMap<>();
		Map<String, Boolean> fingerprintKinds = new LinkedHashMap<>();
		Set<Boolean> keyKinds = new HashSet<>();
		for (SegmentState segment : segments) {
			SegmentResult result = segment.result();
			results.add(result);
			if (segment.key != null) {
				keyKinds.add(segment.key.isBuiltin());
			}
			if (!result.unvalidatedRanges().isEmpty() || !result.finalized()) {
				partial = true;
			}
			if (!result.validatedRanges().isEmpty() && result.fingerprint() != null) {
				fingerprintKinds.put(result.fingerprint(), result.builtinKey());
				fingerprintRanges.computeIfAbsent(result.fingerprint(), ignored -> new ArrayList<>())
						.add(new CoveredRange(result.fileName(), result.stream(), result.runId(),
								result.validatedRanges(), result.firstSealedAt(), result.lastSealedAt()));
			}
		}
		if (!failures.isEmpty()) {
			partial = true;
		}
		KeyProtection keyProtection = keyProtection(keyKinds);
		List<FingerprintCoverage> fingerprints = fingerprintRanges.entrySet().stream()
				.map(entry -> new FingerprintCoverage(entry.getKey(), fingerprintKinds.get(entry.getKey()),
						entry.getValue()))
				.toList();
		return new Report(failures.isEmpty() ? Integrity.INTACT : Integrity.FAILED,
				partial ? Coverage.PARTIAL : Coverage.COMPLETE, keyProtection,
				results, fingerprints, failures, warnings);
	}

	private static void checkFile(Path file, List<SegmentState> segments, List<String> failures,
			List<String> warnings, Set<String> unsealedFiles) throws IOException {
		List<RawLine> lines = lines(Files.readAllBytes(file));
		boolean claimsSealing = lines.stream().anyMatch(line -> isMarker(line.text())
				|| line.text().contains("publicKey=") && isAuditOpen(line.text()));
		if (!claimsSealing) {
			unsealedFiles.add(file.getFileName().toString());
			return;
		}

		SegmentState current = null;
		for (RawLine line : lines) {
			if (isMarker(line.text())) {
				if (current == null) {
					if (!line.terminated()) {
						warnings.add(file.getFileName() + ": incomplete trailing audit marker");
					} else {
						failures.add(file.getFileName() + ": audit marker precedes audit.open");
					}
					continue;
				}
				current.marker(line, failures, warnings);
				continue;
			}

			Long sequence = sequence(line.text());
			if (sequence != null && sequence == 1 && isAuditOpen(line.text())) {
				if (current != null) {
					current.finish(warnings);
					segments.add(current);
				}
				current = new SegmentState(file.getFileName().toString(), publicKey(line.text(), failures, file));
			}
			if (current == null) {
				if (line.terminated()) {
					failures.add(file.getFileName() + ": content precedes sealed audit.open");
				} else {
					warnings.add(file.getFileName() + ": incomplete content precedes audit.open");
				}
			} else {
				current.record(line, sequence, failures, warnings);
			}
		}
		if (current != null) {
			current.finish(warnings);
			segments.add(current);
		}
	}

	private static AuditPublicKey publicKey(String line, List<String> failures, Path file) {
		Matcher matcher = PUBLIC_KEY.matcher(detail(line));
		if (!matcher.find()) {
			failures.add(file.getFileName() + ": audit.open has no public key");
			return null;
		}
		try {
			return AuditPublicKey.fromBase64(matcher.group(1));
		} catch (IOException e) {
			failures.add(file.getFileName() + ": audit.open public key is invalid");
			return null;
		}
	}

	private static boolean checkContinuations(List<SegmentState> segments, List<Path> files,
			List<String> failures, List<String> warnings) {
		Set<String> suppliedFiles = new HashSet<>();
		Map<String, Integer> successors = new HashMap<>();
		files.forEach(path -> suppliedFiles.add(path.getFileName().toString()));
		boolean partial = false;
		for (SegmentState next : segments) {
			if (!(next.firstMarker != null
					&& next.firstMarker.marker() instanceof AuditMarker.Continue continuation)) {
				continue;
			}
			String predecessor = continuation.identity().stream() + "\n" + continuation.identity().runId()
					+ "\n" + continuation.previousFile() + "\n" + continuation.previousBlockSha256();
			if (successors.merge(predecessor, 1, Integer::sum) > 1) {
				failures.add(next.fileName + ": audit rollover predecessor has multiple successors");
			}
			SegmentState previous = segments.stream()
					.filter(candidate -> candidate.fileName.equals(continuation.previousFile())
							&& candidate.lastMarker != null
							&& candidate.lastMarker.marker() instanceof AuditMarker.Final end
							&& end.identity().runId().equals(continuation.identity().runId())
							&& end.identity().stream().equals(continuation.identity().stream()))
					.findFirst().orElse(null);
			if (previous == null) {
				partial = true;
				if (suppliedFiles.contains(continuation.previousFile())) {
					failures.add(next.fileName + ": CONTINUE has no matching valid predecessor FINAL");
				} else {
					warnings.add(next.fileName + ": predecessor not checked: " + continuation.previousFile());
				}
				continue;
			}
			try {
				AuditBlockChain.checkContinuation(continuation, previous.lastMarker, next.key);
			} catch (IOException | RuntimeException e) {
				failures.add(next.fileName + ": " + e.getMessage());
			}
		}
		return partial;
	}

	private static boolean checkRunRoots(List<SegmentState> segments, List<String> failures,
			List<String> warnings) {
		Map<String, Integer> roots = new HashMap<>();
		Map<String, Integer> continuations = new HashMap<>();
		for (SegmentState segment : segments) {
			if (segment.firstMarker == null) {
				continue;
			}
			AuditMarker marker = segment.firstMarker.marker();
			String run = marker.identity().stream() + "\n" + marker.identity().runId();
			if (marker instanceof AuditMarker.First) {
				roots.merge(run, 1, Integer::sum);
			} else if (marker instanceof AuditMarker.Continue) {
				continuations.merge(run, 1, Integer::sum);
			}
		}
		for (Map.Entry<String, Integer> entry : roots.entrySet()) {
			if (entry.getValue() > 1) {
				failures.add("Audit run has more than one FIRST segment: " + entry.getKey().replace('\n', '/'));
			}
		}
		boolean partial = false;
		for (String run : continuations.keySet()) {
			if (!roots.containsKey(run)) {
				partial = true;
				warnings.add("Audit run root was not supplied: " + run.replace('\n', '/'));
			}
		}
		return partial;
	}

	private static boolean checkInventory(List<SegmentState> segments, List<Path> files,
			Inventory inventory, List<String> warnings) {
		Set<String> presentFiles = new HashSet<>();
		files.forEach(path -> presentFiles.add(path.getFileName().toString()));
		Set<String> presentStreams = new HashSet<>();
		segments.stream().filter(segment -> segment.stream != null)
				.forEach(segment -> presentStreams.add(segment.stream));
		boolean partial = false;
		for (String expected : inventory.files()) {
			if (!presentFiles.contains(expected)) {
				partial = true;
				warnings.add("Expected audit file is missing: " + expected);
			}
		}
		for (String expected : inventory.streams()) {
			if (!presentStreams.contains(expected)) {
				partial = true;
				warnings.add("Expected audit stream is missing: " + expected);
			}
		}
		return partial;
	}

	private static KeyProtection keyProtection(Set<Boolean> keyKinds) {
		if (keyKinds.isEmpty()) {
			return KeyProtection.NONE;
		}
		if (keyKinds.size() > 1) {
			return KeyProtection.MIXED;
		}
		return keyKinds.contains(Boolean.TRUE) ? KeyProtection.BUILTIN : KeyProtection.CONFIGURED;
	}

	private static List<Path> evidenceFiles(Path evidence) throws IOException {
		if (Files.isRegularFile(evidence)) {
			return List.of(evidence);
		}
		if (!Files.isDirectory(evidence)) {
			throw new IOException("Audit evidence does not exist: " + evidence);
		}
		try (Stream<Path> stream = Files.list(evidence)) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().contains("_full")
							&& path.getFileName().toString().endsWith(".log"))
					.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
		}
	}

	private static List<RawLine> lines(byte[] bytes) {
		List<RawLine> result = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == '\n') {
				result.add(line(bytes, start, i + 1, true));
				start = i + 1;
			}
		}
		if (start < bytes.length) {
			result.add(line(bytes, start, bytes.length, false));
		}
		return result;
	}

	private static RawLine line(byte[] bytes, int start, int end, boolean terminated) {
		byte[] raw = java.util.Arrays.copyOfRange(bytes, start, end);
		int textEnd = raw.length - (terminated ? 1 : 0);
		return new RawLine(raw, new String(raw, 0, textEnd, StandardCharsets.UTF_8), terminated);
	}

	private static boolean isMarker(String line) {
		return line.startsWith("FIRST ") || line.startsWith("CONTINUE ")
				|| line.startsWith("SEAL ") || line.startsWith("FINAL ");
	}

	private static boolean isAuditOpen(String line) {
		String[] fields = line.split(" \\| ", 8);
		return fields.length > 3 && "audit.open".equals(fields[3].strip());
	}

	private static Long sequence(String line) {
		int separator = line.indexOf(" | ");
		if (separator < 0) {
			return null;
		}
		try {
			return Long.valueOf(line.substring(0, separator).strip());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String detail(String line) {
		String[] fields = line.split(" \\| ", 8);
		return fields.length > 6 ? fields[6] : "";
	}

	private static List<RecordRange> ranges(List<Long> values) {
		if (values.isEmpty()) {
			return List.of();
		}
		List<Long> sorted = values.stream().distinct().sorted().toList();
		List<RecordRange> result = new ArrayList<>();
		long first = sorted.getFirst();
		long last = first;
		for (int i = 1; i < sorted.size(); i++) {
			long value = sorted.get(i);
			if (value == last + 1) {
				last = value;
			} else {
				result.add(new RecordRange(first, last));
				first = value;
				last = value;
			}
		}
		result.add(new RecordRange(first, last));
		return result;
	}

	private record RawLine(byte[] bytes, String text, boolean terminated) {
	}

	private record Pending(long sequence, byte[] bytes) {
	}

	private static final class SegmentState {
		private final String fileName;
		private final AuditPublicKey key;
		private final AuditBlockChain chain = new AuditBlockChain();
		private final List<Pending> pending = new ArrayList<>();
		private final List<RawLine> unnumbered = new ArrayList<>();
		private final List<Long> validated = new ArrayList<>();
		private AuditMarkerCodec.Signed firstMarker;
		private AuditMarkerCodec.Signed lastMarker;
		private String stream;
		private String runId;
		private String fingerprint;
		private Instant firstSealedAt;
		private Instant lastSealedAt;
		private boolean invalid;
		private boolean incomplete;
		// An unparsable marker is a crash remnant only if nothing else follows it in the segment.
		private String malformedMarker;

		private SegmentState(String fileName, AuditPublicKey key) {
			this.fileName = fileName;
			this.key = key;
		}

		private boolean malformedMarkerNotLast(List<String> failures) {
			if (this.malformedMarker == null) {
				return false;
			}
			this.invalid = true;
			failures.add(this.fileName + ": " + this.malformedMarker);
			this.malformedMarker = null;
			return true;
		}

		private void record(RawLine line, Long sequence, List<String> failures, List<String> warnings) {
			malformedMarkerNotLast(failures);
			if (sequence == null) {
				this.incomplete = true;
				this.unnumbered.add(line);
				return;
			}
			this.pending.add(new Pending(sequence, line.bytes()));
		}

		private void marker(RawLine line, List<String> failures, List<String> warnings) {
			if (malformedMarkerNotLast(failures) || this.invalid) {
				return;
			}
			if (!line.terminated()) {
				this.incomplete = true;
				warnings.add(this.fileName + ": incomplete trailing audit marker");
				return;
			}
			AuditMarkerCodec.Signed signed;
			try {
				signed = AuditMarkerCodec.parse(line.text());
			} catch (IOException e) {
				this.incomplete = true;
				this.malformedMarker = e.getMessage();
				return;
			}
			AuditMarker marker = signed.marker();
			if (!this.unnumbered.isEmpty()) {
				this.invalid = true;
				failures.add(this.fileName + ": audit marker follows malformed or incomplete content");
				return;
			}
			if (!this.fileName.equals(marker.identity().artifactName())) {
				this.invalid = true;
				failures.add(this.fileName + ": marker names a different artifact");
				return;
			}
			if (this.firstMarker == null) {
				this.firstMarker = signed;
				this.stream = marker.identity().stream();
				this.runId = marker.identity().runId();
				this.fingerprint = marker.identity().keyFingerprint();
			}
			AuditMarker.Block block = AuditFileStore.blockOf(marker);
			byte[] covered = new byte[0];
			if (block != null) {
				covered = covered(block, failures);
				if (covered == null) {
					return;
				}
			} else if (!this.pending.isEmpty()) {
				this.invalid = true;
				failures.add(this.fileName + ": FINAL follows unsealed or inserted records");
				return;
			}
			if (this.key == null) {
				this.invalid = true;
				return;
			}
			try {
				this.chain.accept(signed, covered, this.key);
				if (block != null) {
					this.pending.clear();
				}
				this.lastMarker = signed;
				if (block != null) {
					for (long sequence = block.firstLine(); sequence <= block.lastLine(); sequence++) {
						this.validated.add(sequence);
					}
					if (this.firstSealedAt == null) {
						this.firstSealedAt = marker.identity().sealedAt();
					}
					this.lastSealedAt = marker.identity().sealedAt();
				}
			} catch (IOException | RuntimeException e) {
				this.invalid = true;
				failures.add(this.fileName + ": " + e.getMessage());
			}
		}

		private byte[] covered(AuditMarker.Block block, List<String> failures) {
			long expected = block.lastLine() - block.firstLine() + 1;
			if (this.pending.size() != expected) {
				this.invalid = true;
				failures.add(this.fileName + ": sealed block has missing, duplicated, inserted or reordered records");
				return null;
			}
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			for (int i = 0; i < this.pending.size(); i++) {
				Pending record = this.pending.get(i);
				if (record.sequence() != block.firstLine() + i) {
					this.invalid = true;
					failures.add(this.fileName + ": sealed block record order or range is invalid");
					return null;
				}
				bytes.writeBytes(record.bytes());
			}
			return bytes.toByteArray();
		}

		private void finish(List<String> warnings) {
			if (this.malformedMarker != null) {
				warnings.add(this.fileName + ": incomplete trailing audit marker was not validated");
			}
			if (!this.unnumbered.isEmpty()) {
				warnings.add(this.fileName + ": incomplete trailing audit marker or record was not validated");
			}
			if (!this.pending.isEmpty() || this.incomplete || !this.chain.isFinalized()) {
				warnings.add(this.fileName + ": audit segment is not fully finalized");
			}
		}

		private SegmentResult result() {
			List<Long> unvalidated = this.pending.stream().map(Pending::sequence).toList();
			return new SegmentResult(this.fileName, this.stream, this.runId, this.fingerprint,
					this.key != null && this.key.isBuiltin(),
					!this.invalid && !this.incomplete && this.pending.isEmpty() && this.chain.isFinalized(),
					ranges(this.validated), ranges(unvalidated), this.firstSealedAt, this.lastSealedAt);
		}
	}
}
