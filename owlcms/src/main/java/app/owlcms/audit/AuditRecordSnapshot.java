package app.owlcms.audit;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Locale;

/** Captures mutable athlete/actor values before handing a record to the I/O worker. */
public record AuditRecordSnapshot(String readableBody, String fullBody) {
	public static AuditRecordSnapshot capture(AuditEntry entry, OffsetDateTime now) {
		String readable = AuditFormat.format(0, entry, now, false);
		String full = AuditFormat.format(0, entry, now, true);
		return new AuditRecordSnapshot(readable.substring(readable.indexOf(" | ")),
				full.substring(full.indexOf(" | ")));
	}

	public String readable(long sequence) {
		return String.format(Locale.ROOT, "%5d", sequence) + this.readableBody;
	}

	public byte[] full(long sequence) {
		return (String.format(Locale.ROOT, "%5d", sequence) + this.fullBody + "\n")
				.getBytes(StandardCharsets.UTF_8);
	}

	public int retainedBytes() {
		return Math.addExact(this.readableBody.getBytes(StandardCharsets.UTF_8).length,
				this.fullBody.getBytes(StandardCharsets.UTF_8).length);
	}
}
