package app.owlcms.audit;

import java.util.Map;

/**
 * Immutable, self-contained evidence for one ordinary audit line. {@code session} is the session the record belongs
 * to: the athlete's session, or the platform's session for a record without an athlete; null when not tied to a
 * session. It is part of schema version 2.
 */
public record AuditRecord(int schemaVersion, long sequence, String timestamp, String platform, AuditActor actor,
		String action, Athlete athlete, String session, String attempt, Change change, String cause,
		Map<String, Object> details) {
	public record Athlete(String id, String name) {
	}

	public record Change(String field, Object before, Object after, Integer priorRequested,
			Integer previousAttemptRequested) {
	}

	public AuditRecord withSequence(long sequence) {
		return new AuditRecord(this.schemaVersion, sequence, this.timestamp, this.platform, this.actor, this.action,
				this.athlete, this.session, this.attempt, this.change, this.cause, this.details);
	}
}
