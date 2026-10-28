package app.owlcms.audit;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ApplicationAudit {
	private static final AtomicBoolean STOPPING = new AtomicBoolean();

	private ApplicationAudit() {
	}

	public static void started() {
		AuditLog.write(AuditEntry.builder("competition", "application.started")
				.actor(AuditActor.system()).build());
	}

	/** Records the export that replaced the competition at startup; the SHA-256 is of the exact JSON bytes imported. */
	public static void startupImport(String source, String sha256) {
		AuditLog.write(AuditEntry.builder("competition", "application.startup-import")
				.actor(AuditActor.system()).detail(AuditFormat.kvs("source", source, "sha256", sha256)).build());
	}

	/**
	 * Records an export that replaced the competition while running (Import Database). The SHA-256 is of the exact
	 * JSON bytes imported, so the file can be identified whatever version produced it.
	 */
	public static void imported(AuditOrigin origin, String source, int bytes, String sha256) {
		AuditLog.write(AuditEntry.builder("competition", "application.import").actor(origin.actor())
				.cause(origin.cause())
				.detail(AuditFormat.kvs("source", AuthAudit.clean(source), "bytes", bytes, "sha256", sha256))
				.build());
	}

	public static void restartRequested(String reason) {
		AuditLog.write(AuditEntry.builder("competition", "application.restart-requested")
				.actor(currentActor()).detail(AuditFormat.kv("reason", reason)).build());
	}

	public static void stopping(String reason) {
		if (STOPPING.compareAndSet(false, true)) {
			AuditLog.write(AuditEntry.builder("competition", "application.stopping")
					.actor(currentActor()).detail(AuditFormat.kv("reason", reason)).build());
		}
	}

	private static AuditActor currentActor() {
		AuditActor actor = AuditContext.actor();
		if (actor == null) {
			actor = AuditActor.fromCurrentUi();
		}
		return actor != null ? actor : AuditActor.system();
	}
}