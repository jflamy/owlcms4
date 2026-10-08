package app.owlcms.audit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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
	static final Path LOGS_DIRECTORY = Path.of("logs");

	public enum Scope {
		LOGS, LOGS_AND_EXPORT, LOGS_EXPORT_AND_LOCAL;

		boolean export() {
			return this != LOGS;
		}

		boolean local() {
			return this == LOGS_EXPORT_AND_LOCAL;
		}
	}

	private AuditSnapshot() {
	}

	/** Produced on a background thread; the reader sees a writer failure instead of a truncated zip. */
	public static InputStream stream(Scope scope, AuditActor actor) {
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
				writeTo(out, scope, actor);
			} catch (Throwable e) {
				failure.set(e instanceof IOException io ? io : new IOException(e));
				LoggerUtils.logError(logger, e);
			}
		});
		return new InputStreamWrapper(in, failure);
	}

	public static void writeTo(OutputStream out, Scope scope, AuditActor actor) throws IOException {
		AuditActor effective = actor != null ? actor : AuditActor.system();
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
		Set<Path> open = AuditLog.rollover();
		zipLogs(LOGS_DIRECTORY, open, zip);
		if (scope.local()) {
			Path local = ResourceWalker.getLocalDirPath();
			if (local != null && Files.isDirectory(local)) {
				ZipUtils.zipFile(local.toFile(), "local", zip);
			}
		}
		zip.finish();
	}

	static void zipLogs(Path root, Set<Path> excluded, ZipOutputStream zip) throws IOException {
		if (!Files.isDirectory(root)) {
			return;
		}
		try (Stream<Path> files = Files.walk(root)) {
			for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
				if (excluded.stream().anyMatch(path -> sameFile(path, file))) {
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
