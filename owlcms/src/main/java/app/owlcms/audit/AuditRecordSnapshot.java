package app.owlcms.audit;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/**
 * Captures mutable athlete/actor values before handing a record to the I/O worker. The record is not numbered yet: the
 * writer assigns its sequence number and calls {@link #line(long)}.
 */
public record AuditRecordSnapshot(AuditRecord record, OffsetDateTime timestamp, int retainedBytes) {
	public static AuditRecordSnapshot capture(AuditEntry entry, OffsetDateTime now) {
		AuditRecord record = AuditFormat.capture(0, entry, now);
		int readableBytes = AuditFormat.readable(record).getBytes(StandardCharsets.UTF_8).length;
		int jsonBytes = AuditFormat.json(record).getBytes(StandardCharsets.UTF_8).length;
		return new AuditRecordSnapshot(record, now, Math.addExact(readableBytes, jsonBytes));
	}

	/**
	 * One audit line as it is sealed: the readable part followed by the JSON. The plain line is not built separately:
	 * it is the leading part of this line.
	 */
	public record Line(String full, int readableLength) {
		public String readable() {
			return this.full.substring(0, this.readableLength);
		}

		public byte[] fullBytes() {
			return (this.full + "\n").getBytes(StandardCharsets.UTF_8);
		}
	}

	/** The record is numbered once; the readable part and the JSON are both built from that numbered record. */
	public Line line(long sequence) {
		AuditRecord numbered = this.record.withSequence(sequence);
		String readable = AuditFormat.readable(numbered);
		return new Line(AuditFormat.full(readable, numbered), readable.length());
	}
}
