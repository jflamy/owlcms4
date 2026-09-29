package app.owlcms.audit;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.records.RecordEvent;

public final class RecordAudit {
	private RecordAudit() {
	}

	public static void challenge(String platform, Athlete athlete, int attempt, Integer requested,
			RecordEvent record) {
		write(platform, "record.challenge", athlete, attempt, record, record.getRecordValue(), requested);
	}

	public static void improved(String platform, Athlete athlete, int attempt, RecordEvent previous,
			RecordEvent improved) {
		write(platform, "record.new", athlete, attempt, previous, previous.getRecordValue(), improved.getRecordValue());
	}

	public static void cancelled(String platform, Athlete athlete, int attempt, RecordEvent record) {
		write(platform, "record.cancelled", athlete, attempt, record, record.getRecordValue(), null);
	}

	private static void write(String platform, String action, Athlete athlete, int attempt, RecordEvent record,
			Object oldValue, Object newValue) {
		AuditActor actor = AuditContext.actor() != null ? AuditContext.actor() : AuditActor.system();
		AuditLog.write(AuditEntry.builder(platform, action)
				.actor(actor)
				.athlete(athlete)
				.attempt(attempt(attempt))
				.field("record")
				.oldValue(oldValue)
				.newValue(newValue)
				.cause(AuditContext.cause())
				.detail(detail(record))
				.build());
	}

	private static String detail(RecordEvent record) {
		return AuditFormat.kvs("federation", record.getRecordFederation(), "name", record.getRecordName(),
				"ageGroup", record.getAgeGrp(), "gender", record.getGender(),
				"bodyWeightCategory", record.getBwCatString(), "lift", record.getRecordLift());
	}

	private static String attempt(int attempt) {
		return attempt <= 3 ? "SN" + attempt : "CJ" + (attempt - 3);
	}
}