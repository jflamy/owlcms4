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