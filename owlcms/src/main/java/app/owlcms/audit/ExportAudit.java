package app.owlcms.audit;

import app.owlcms.utils.LoggerUtils;

/**
 * Competition-wide {@code export.json} events recording the SHA-256 and byte length of every JSON export (V1 and V2).
 * A digest is only ever reported for a complete artifact; failures are recorded without one.
 */
public final class ExportAudit {
	public static final String ACTION = "export.json";
	public static final String CHANNEL_DOWNLOAD = "download";
	public static final String CHANNEL_HTTP = "http";
	public static final String CHANNEL_WEBSOCKET = "websocket";
	public static final String CHANNEL_INTERNAL = "internal";
	private static final String COMPETITION = "competition";

	private ExportAudit() {
	}

	public static void success(AuditActor actor, int format, String channel, long bytes, String sha256) {
		AuditLog.write(AuditEntry.builder(COMPETITION, ACTION).actor(effective(actor))
				.detail(successDetail(format, channel, bytes, sha256)).build());
	}

	public static void failed(AuditActor actor, int format, String channel, Throwable error) {
		AuditLog.write(AuditEntry.builder(COMPETITION, ACTION).actor(effective(actor))
				.detail(failureDetail(format, channel, error)).build());
	}

	static String successDetail(int format, String channel, long bytes, String sha256) {
		return AuditFormat.kvs("format", format, "channel", channel, "bytes", bytes, "sha256", sha256);
	}

	static String failureDetail(int format, String channel, Throwable error) {
		return AuditFormat.kvs("format", format, "channel", channel, "outcome", "failed", "reason",
				LoggerUtils.exceptionMessage(error));
	}

	private static AuditActor effective(AuditActor actor) {
		return actor != null ? actor : AuditActor.system();
	}
}
