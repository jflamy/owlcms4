package app.owlcms.audit;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import app.owlcms.audit.ExportSignature.KeyMatch;
import app.owlcms.audit.ExportSignature.Reason;
import app.owlcms.audit.ExportSignature.Result;

public final class ApplicationAudit {
	private static final AtomicBoolean STOPPING = new AtomicBoolean();

	private ApplicationAudit() {
	}

	public static void started() {
		AuditLog.write(AuditEntry.builder("competition", "application.started")
				.actor(AuditActor.system()).build());
	}

	/** Records the export that replaced the competition at startup, with the outcome of its signature check. */
	public static void startupImport(String source, Result signature, KeyMatch match) {
		AuditLog.write(AuditEntry.builder("competition", "application.startup-import")
				.actor(AuditActor.system()).detail(importDetail(source, signature, match)).build());
	}

	/**
	 * Records an export that replaced the competition while running (Import Database), with the outcome of its
	 * signature check. The import is never refused because of the signature.
	 */
	public static void imported(AuditOrigin origin, String source, Result signature, KeyMatch match) {
		AuditLog.write(AuditEntry.builder("competition", "application.import").actor(origin.actor())
				.cause(origin.cause()).detail(importDetail(AuthAudit.clean(source), signature, match)).build());
	}

	/**
	 * The SHA-256 is the locally computed body checksum (the whole file when unsigned), never the value claimed by
	 * the file, so the file can be identified whatever version produced it.
	 */
	static String importDetail(String source, Result signature, KeyMatch match) {
		return AuditFormat.kvs("source", source, "bytes", signature.bytes(), "sha256", signature.sha256(),
				"signature", lower(signature.status()),
				"reason", signature.reason() == Reason.NONE ? "-" : lower(signature.reason()),
				"key", match == KeyMatch.NONE ? "-" : lower(match),
				"fingerprint", signature.fingerprint() == null ? "-" : signature.fingerprint());
	}

	private static String lower(Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	/** "Imports are allowed" was turned on or off (Administration page). */
	public static void importsAllowedChanged(AuditOrigin origin, boolean allowed) {
		AuditLog.write(AuditEntry.builder("competition", "application.imports-allowed").actor(origin.actor())
				.cause(origin.cause()).detail(AuditFormat.kv("imports", allowed ? "allowed" : "not-allowed"))
				.build());
	}

	/** An import was refused: no IMPORT permission, or imports are not allowed. */
	public static void importRefused(AuditOrigin origin, String kind, String reason) {
		AuditLog.write(AuditEntry.builder("competition", "application.import-refused").actor(origin.actor())
				.cause(origin.cause()).detail(AuditFormat.kvs("import", kind, "reason", reason))
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