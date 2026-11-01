package app.owlcms.audit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.LoggerFactory;

import app.owlcms.data.export.ChecksummedJsonExport.ChecksummedInputStream;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.spreadsheet.InputStreamWrapper;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.ZipUtils;
import ch.qos.logback.classic.Logger;

/**
 * A backup of every log, taken at a quiet point, for the competition director to copy off the machine.
 * <p>
 * The database export is written first so its {@code export.json} checksum line is sealed, then every
 * sealed stream is forced into a new file so the previous files are finalized. The whole {@code logs}
 * tree goes into the zip, application logs included; only the sealed files just opened by the roll are
 * left out, since they hold nothing yet. The checker reports any tail left by an earlier crash as partial.
 */
public final class AuditSnapshot {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditSnapshot.class);
	public static final String ACTION = "audit.snapshot";
	public static final String ANOMALY_ACTION = "audit.anomaly";
	public static final String CHECKPOINT_SCOPE = "CHECKPOINT";
	static final Path LOGS_DIRECTORY = Path.of("logs");
	private static final int MAX_DESCRIPTION = 500;

	public enum Scope {
		EXPORT, LOGS, LOGS_AND_EXPORT, LOGS_EXPORT_AND_LOCAL;

		boolean export() {
			return this != LOGS;
		}

		boolean logs() {
			return this != EXPORT;
		}

		boolean local() {
			return this == LOGS_EXPORT_AND_LOCAL;
		}
	}

	private AuditSnapshot() {
	}

	/** Produced on a background thread; the reader sees a writer failure instead of a truncated zip. */
	public static InputStream stream(Scope scope, AuditActor actor) {
		return stream(scope, actor, null);
	}

	/**
	 * @param anomaly what the operator saw (Capture Anomaly), recorded before the logs are sealed so the zip holds it;
	 *                null for an ordinary backup
	 */
	public static InputStream stream(Scope scope, AuditActor actor, String anomaly) {
		PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in;
		try {
			in = new PipedInputStream(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		AtomicReference<IOException> failure = new AtomicReference<>();
		Thread.ofVirtual().name("AuditSnapshot").start(() -> {
			try (out) {
				writeTo(out, scope, actor, anomaly);
			} catch (Throwable e) {
				failure.set(e instanceof IOException io ? io : new IOException(e));
				LoggerUtils.logError(logger, e);
			}
		});
		return new InputStreamWrapper(in, failure);
	}

	public static void writeTo(OutputStream out, Scope scope, AuditActor actor) throws IOException {
		writeTo(out, scope, actor, null);
	}

	public static void writeTo(OutputStream out, Scope scope, AuditActor actor, String anomaly) throws IOException {
		AuditActor effective = actor != null ? actor : AuditActor.system();
		if (anomaly != null) {
			String description = describe(anomaly);
			logger.warn("anomaly captured by {}: {}", effective.user(), description);
			AuditLog.write(AuditEntry.builder("competition", ANOMALY_ACTION).actor(effective)
					.detail(AuditFormat.kv("description", description)).build());
		}
		String stamp = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"));
		ZipOutputStream zip = new ZipOutputStream(out);
		if (scope.export()) {
			zip.putNextEntry(new ZipEntry("database_v2_" + stamp + ".json"));
			try (ChecksummedInputStream export = new CompetitionDataV2().exportData(effective,
					ExportAudit.CHANNEL_AUDIT)) {
				export.transferTo(zip);
				export.checksum();
			}
			zip.closeEntry();
		}
		AuditLog.write(AuditEntry.builder("competition", ACTION).actor(effective)
				.detail(AuditFormat.kv("scope", scope.name())).build());
		if (scope.logs()) {
			Set<Path> open = AuditLog.rollover();
			zipLogs(LOGS_DIRECTORY, open, zip);
		}
		if (scope.local()) {
			Path local = ResourceWalker.getLocalDirPath();
			if (local != null && Files.isDirectory(local)) {
				ZipUtils.zipFile(local.toFile(), "local", zip);
			}
		}
		zip.finish();
	}

	/**
	 * Checkpoint for the tracker: the export, then every log file modified at or after {@code since}, open or closed,
	 * copied as it is. Nothing is rolled and no seal is awaited, so an open file may end in the middle of a record.
	 * The caller names the zip with {@link #checkpointName(Instant)} for a time taken before calling, so a file
	 * modified while the zip is built is sent again next time.
	 *
	 * @param since null for every log file
	 */
	public static void writeCheckpoint(OutputStream out, AuditActor actor, Instant since) throws IOException {
		AuditActor effective = actor != null ? actor : AuditActor.system();
		String stamp = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"));
		ZipOutputStream zip = new ZipOutputStream(out);
		zip.putNextEntry(new ZipEntry("database_v2_" + stamp + ".json"));
		try (ChecksummedInputStream export = new CompetitionDataV2().exportData(effective,
		        ExportAudit.CHANNEL_HTTP)) {
			export.transferTo(zip);
			export.checksum();
		}
		zip.closeEntry();
		AuditLog.write(AuditEntry.builder("competition", ACTION).actor(effective)
		        .detail(AuditFormat.kvs("scope", CHECKPOINT_SCOPE, "channel", ExportAudit.CHANNEL_HTTP,
		                "since", since != null ? checkpointStamp(since) : "-"))
		        .build());
		zipLogs(LOGS_DIRECTORY, file -> checkpointSelects(file, since), zip);
		zip.finish();
	}

	/** Log files modified at or after {@code since} (all when null); hidden files such as .DS_Store are not logs. */
	static boolean checkpointSelects(Path file, Instant since) {
		if (file.getFileName().toString().startsWith(".")) {
			return false;
		}
		return since == null || modifiedSince(file, since);
	}

	/** @return the zip name for a checkpoint selected at that time, e.g. {@code 2026-10-31T20-37-42.123Z.zip} */
	public static String checkpointName(Instant selectedAt) {
		return checkpointStamp(selectedAt) + ".zip";
	}

	/**
	 * @param value a checkpoint name or its timestamp, with or without {@code .zip}, or an ISO 8601 instant
	 * @return null for a missing value
	 * @throws DateTimeParseException if the value is not a time
	 */
	public static Instant parseCheckpointTime(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String text = value.strip();
		if (text.endsWith(".zip")) {
			text = text.substring(0, text.length() - 4);
		}
		if (text.contains(":")) {
			return Instant.parse(text);
		}
		return Instant.from(AuditFileStore.NAME_TIME.parse(text));
	}

	static String checkpointStamp(Instant time) {
		return AuditFileStore.NAME_TIME.format(time.truncatedTo(ChronoUnit.MILLIS));
	}

	private static boolean modifiedSince(Path file, Instant since) {
		try {
			return !Files.getLastModifiedTime(file).toInstant().isBefore(since);
		} catch (IOException e) {
			return true;
		}
	}

	/** Typed text that ends up in a log line: one line, bounded. */
	static String describe(String anomaly) {
		String cleaned = anomaly.replaceAll("\\p{Cntrl}", " ").trim();
		if (cleaned.isEmpty()) {
			return "-";
		}
		return cleaned.length() > MAX_DESCRIPTION ? cleaned.substring(0, MAX_DESCRIPTION) : cleaned;
	}

	static void zipLogs(Path root, Set<Path> excluded, ZipOutputStream zip) throws IOException {
		zipLogs(root, file -> excluded.stream().noneMatch(path -> sameFile(path, file)), zip);
	}

	static void zipLogs(Path root, Predicate<Path> included, ZipOutputStream zip) throws IOException {
		if (!Files.isDirectory(root)) {
			return;
		}
		try (Stream<Path> files = Files.walk(root)) {
			for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
				if (!included.test(file)) {
					continue;
				}
				String name = root.getFileName().resolve(root.relativize(file)).toString().replace('\\', '/');
				zip.putNextEntry(new ZipEntry(name));
				Files.copy(file, zip);
				zip.closeEntry();
			}
		}
	}

	private static boolean sameFile(Path a, Path b) {
		try {
			return Files.isSameFile(a, b);
		} catch (IOException e) {
			return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
		}
	}
}
