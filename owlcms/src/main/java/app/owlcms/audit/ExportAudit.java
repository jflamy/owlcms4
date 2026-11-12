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
	public static final String CHANNEL_AUDIT = "audit";
	private static final String COMPETITION = "competition";

	private ExportAudit() {
	}

	/**
	 * @param sha256               checksum of the export body (the whole file when unsigned)
	 * @param signatureFingerprint fingerprint of the signing key, null when the export is unsigned
	 */
	public static void success(AuditActor actor, int format, String channel, long bytes, String sha256,
			String signatureFingerprint) {
		AuditLog.write(AuditEntry.builder(COMPETITION, ACTION).actor(effective(actor))
				.detail(successDetail(format, channel, bytes, sha256, signatureFingerprint)).build());
	}

	public static void failed(AuditActor actor, int format, String channel, Throwable error) {
		AuditLog.write(AuditEntry.builder(COMPETITION, ACTION).actor(effective(actor))
				.detail(failureDetail(format, channel, error)).build());
	}

	/** The session that clicked has no EXPORT_DATA permission (RESULTS access). */
	public static final String REASON_NO_PERMISSION = "no-results-permission";
	/** The format is not available in the current configuration (V1 with the noV1Export feature switch). */
	public static final String REASON_UNAVAILABLE = "unavailable";

	public static void refused(AuditActor actor, int format, String channel, String reason) {
		AuditLog.write(AuditEntry.builder(COMPETITION, ACTION).actor(effective(actor))
				.detail(AuditFormat.kvs("format", format, "channel", channel, "outcome", "refused", "reason",
						reason))
				.build());
	}

	static String successDetail(int format, String channel, long bytes, String sha256, String signatureFingerprint) {
		return AuditFormat.kvs("format", format, "channel", channel, "bytes", bytes, "sha256", sha256, "signature",
				signatureFingerprint != null ? signatureFingerprint : "none");
	}

	static String failureDetail(int format, String channel, Throwable error) {
		return AuditFormat.kvs("format", format, "channel", channel, "outcome", "failed", "reason",
				LoggerUtils.exceptionMessage(error));
	}

	private static AuditActor effective(AuditActor actor) {
		return actor != null ? actor : AuditActor.system();
	}
}
