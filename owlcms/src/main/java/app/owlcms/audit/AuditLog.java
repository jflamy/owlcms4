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
	private static UnsealedAuditFiles unsealed;

	private AuditLog() {
	}

	public static void bulk(String platform, String action, int count) {
		AuditActor actor = AuditContext.actor() != null ? AuditContext.actor() : AuditActor.fromCurrentUi();
		if (actor != null) {
			write(AuditEntry.builder(platform, "bulk." + action).actor(actor).detail(AuditFormat.kv("count", count)).build());
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
		SealedAuditBackend backend = sealed;
		if (backend != null) {
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
			return;
		}
		for (AuditEntry entry : entries) {
			writePlain(entry);
		}
	}

	public static void closeBlock(String platform) {
		SealedAuditBackend backend = sealed;
		if (backend != null && !AuditContext.isSuppressed()) {
			try {
				backend.closeBlock(platform, UI.getCurrent() == null);
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
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				ApplicationAudit.stopping("audit shutdown");
			} finally {
				try {
					sealed.close();
				} catch (IOException e) {
					logger.error("Audit shutdown could not finalize all streams", e);
				}
			}
		}, "AuditShutdown"));
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
	 * now being written; empty when sealing is off, since plain logs have nothing to finalize.
	 */
	public static synchronized Set<Path> rollover() throws IOException {
		SealedAuditBackend backend = sealed;
		// captured under the writeBatch lock so the new headers are ordered with record timestamps
		return backend == null ? Set.of() : backend.rollover(OffsetDateTime.now());
	}

	/** The audit files this process is currently writing, sealed or not; empty when none can be determined. */
	public static synchronized Set<Path> openFiles() {
		SealedAuditBackend backend = sealed;
		if (backend != null) {
			try {
				return backend.openFiles();
			} catch (IOException e) {
				logger.warn("Unable to list the open sealed audit files: {}", e.getMessage());
				return Set.of();
			}
		}
		return unsealed == null ? Set.of() : unsealed.openFiles();
	}

	private static void writePlain(AuditEntry entry) {
		try {
			unsealed().write(entry, OffsetDateTime.now());
		} catch (IOException | RuntimeException e) {
			logger./**/error("Unable to write audit entry action={} platform={}", entry.action(), entry.platform(), e);
		}
	}

	private static UnsealedAuditFiles unsealed() {
		if (unsealed == null) {
			String version = StartupUtils.getVersion();
			unsealed = new UnsealedAuditFiles(DIRECTORY, AuditSealLimits.DEFAULT.fileBytes(),
					AuditFormat.kvs("version", version == null || version.isBlank() ? "unknown" : version,
							"runId", RUN_ID));
		}
		return unsealed;
	}
}
