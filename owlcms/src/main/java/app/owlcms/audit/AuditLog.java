package app.owlcms.audit;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

public final class AuditLog {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditLog.class);
	private static final String RUN_ID = UUID.randomUUID().toString();
	private static volatile SealedAuditBackend sealed;

	private AuditLog() {
	}

	public static void bulk(String platform, String action, int count) {
		AuditActor actor = AuditContext.actor() != null ? AuditContext.actor() : AuditActor.fromCurrentUi();
		if (actor != null) {
			write(AuditEntry.builder(platform, "bulk." + action).actor(actor)
					.details(AuditFormat.fields("count", count)).build());
		}
	}

	public static void write(AuditEntry entry) {
		writeBatch(List.of(entry), false);
	}

	public static synchronized void writeBatch(List<AuditEntry> entries, boolean closeBlock) {
		if (AuditContext.isSuppressed()) {
			return;
		}
		if (entries.isEmpty()) {
			return;
		}
		SealedAuditBackend backend = backend();
		String platform = entries.getFirst().platform();
		if (entries.stream().anyMatch(entry -> !Objects.equals(platform, entry.platform()))) {
			throw new IllegalArgumentException("An audit batch must belong to one platform stream");
		}
		OffsetDateTime now = OffsetDateTime.now();
		List<AuditRecordSnapshot> captured = entries.stream()
				.map(entry -> AuditRecordSnapshot.capture(entry, now)).toList();
		try {
			backend.write(platform, captured, closeBlock, UI.getCurrent() == null);
		} catch (IOException e) {
			logger.error("Unable to persist sealed audit records platform={}", platform, e);
			throw new IllegalStateException("Audit persistence failed", e);
		}
	}

	public static void closeBlock(String platform) {
		if (!AuditContext.isSuppressed()) {
			try {
				backend().closeBlock(platform, UI.getCurrent() == null);
			} catch (IOException e) {
				logger.error("Unable to seal audit block platform={}", platform, e);
				throw new IllegalStateException("Audit sealing failed", e);
			}
		}
	}

	public static synchronized void initializeIntegrity(AuditSigningKey key) throws IOException {
		if (sealed != null) {
			throw new IllegalStateException("Sealed audit logging is already initialized");
		}
		String version = StartupUtils.getVersion();
		sealed = new SealedAuditBackend(DIRECTORY, key, AuditSealLimits.DEFAULT,
				Clock.systemDefaultZone(), System::nanoTime, RUN_ID,
				version == null || version.isBlank() ? "unknown" : version,
				(stream, line) -> {
				}, true);
	}

	/**
	 * Seals and finalizes every stream. It is the last step of the application shutdown, after the inputs have closed;
	 * it is safe to call more than once.
	 *
	 * @throws IOException if a stream could not be finalized, or the stop could not be recorded; the records after the
	 *                     last seal are then not validated
	 */
	public static void finish() throws IOException {
		SealedAuditBackend backend = sealed;
		if (backend == null) {
			return;
		}
		RuntimeException notRecorded = null;
		try {
			ApplicationAudit.stopping("audit shutdown");
		} catch (RuntimeException e) {
			notRecorded = e;
		}
		try {
			backend.close();
		} catch (IOException e) {
			if (notRecorded != null) {
				e.addSuppressed(notRecorded);
			}
			throw e;
		}
		if (notRecorded != null) {
			throw new IOException("The stop could not be recorded in the audit log", notRecorded);
		}
	}

	/**
	 * Sealing is started first thing at startup ({@link AuditIntegrity#start()}), before anything can be audited: an
	 * audit record without it is a programming error, never written unsealed.
	 */
	private static SealedAuditBackend backend() {
		SealedAuditBackend backend = sealed;
		if (backend == null) {
			throw new IllegalStateException("Audit sealing has not been started");
		}
		return backend;
	}

	public static boolean integrityHealthy() {
		SealedAuditBackend backend = sealed;
		return backend != null && backend.healthy();
	}

	/** System property overriding {@link #DIRECTORY}; set by the Maven test run so tests do not write to logs/. */
	public static final String DIRECTORY_PROPERTY = "owlcms.auditDirectory";

	public static final Path DIRECTORY = Path.of(System.getProperty(DIRECTORY_PROPERTY, "logs/audit"));

	/**
	 * Forces every sealed stream into a new file so that all previous files are finalized. Returns the files
	 * now being written.
	 */
	public static synchronized Set<Path> rollover() throws IOException {
		// captured under the writeBatch lock so the new headers are ordered with record timestamps
		return backend().rollover(OffsetDateTime.now());
	}

	/** The audit files this process is currently writing; empty when they cannot be determined. */
	public static synchronized Set<Path> openFiles() {
		try {
			return backend().openFiles();
		} catch (IOException e) {
			logger.warn("Unable to list the open sealed audit files: {}", e.getMessage());
			return Set.of();
		}
	}
}
