package app.owlcms.audit;

import java.util.List;
import java.util.ArrayList;

import app.owlcms.audit.AthleteDiff.Change;
import app.owlcms.data.athlete.Athlete;

public final class AthleteAudit {
	/** An athlete changed by another committed action, such as an age group change; that action writes the entries. */
	public record Changed(Athlete athlete, String platform, List<Change> changes) {
	}

	private AthleteAudit() {
	}

	public static String platformOf(Athlete athlete) {
		return athlete.getGroup() != null && athlete.getGroup().getPlatform() != null
				? athlete.getGroup().getPlatform().getName() : "competition";
	}

	public static void write(Athlete athlete, List<Change> changes) {
		writeWithClock(athlete, changes, null);
	}

	public static void writeWithClock(Athlete athlete, List<Change> changes, Integer clockMillis) {
		if (changes.isEmpty() || AuditContext.isSuppressed()) {
			return;
		}
		AuditActor actor = AuditContext.actor();
		if (actor == null) {
			actor = AuditActor.fromCurrentUi();
		}
		if (actor == null) {
			return;
		}
		write(athlete, changes, new AuditOrigin(actor, AuditContext.cause()), clockMillis);
	}

	public static void write(Athlete athlete, List<Change> changes, AuditOrigin origin) {
		write(athlete, changes, origin, null);
	}

	private static void write(Athlete athlete, List<Change> changes, AuditOrigin origin, Integer clockMillis) {
		if (changes.isEmpty() || AuditContext.isSuppressed()) {
			return;
		}
		AuditActor actor = origin.actor();
		String platform = athlete.getGroup() != null && athlete.getGroup().getPlatform() != null
				? athlete.getGroup().getPlatform().getName() : "competition";
		List<AuditEntry> entries = new ArrayList<>();
		for (Change change : changes) {
			entries.add(AuditEntry.builder(platform, action(change))
					.actor(actor)
					.athlete(athlete)
					.attempt(change.attempt())
					.field(change.field())
					.oldValue(change.oldValue())
					.newValue(change.newValue())
					.priorRequested(change.priorRequested())
					.previousAttemptRequested(previousAttemptRequested(athlete, change))
					.cause(origin.cause())
					.details(clockMillis != null && change.attempt() != null
							? AuditFormat.fields("clockMillis", clockMillis) : null)
					.build());
		}
		AuditLog.writeBatch(entries, sealsSave(actor));
	}

	/** A lift result and the automatic progression it causes get their own actions, so they stand out in the log. */
	static String action(Change change) {
		if (change.field().endsWith(".actualLift")) {
			return "athlete.lift";
		}
		if (change.field().endsWith(".automatic")) {
			return "athlete.progression";
		}
		return "athlete.change";
	}

	static boolean sealsSave(AuditActor actor) {
		return "WEIGHIN".equals(actor.station()) || "REGISTRATION".equals(actor.station());
	}

	/**
	 * Marks automatic progressions as (+1) after a good lift or (SAME) after a no lift, and declarations and changes
	 * with their difference from the weight requested before them: (+X), (-X) or (SAME).
	 */
	static Object annotate(Athlete athlete, Change change) {
		return change.attempt() == null ? change.newValue()
				: AuditFormat.annotate(change.field(), change.newValue(), change.priorRequested(),
						previousAttemptRequested(athlete, change));
	}

	private static Integer previousAttemptRequested(Athlete athlete, Change change) {
		String attempt = change.attempt();
		if (attempt == null || !change.field().endsWith(".automatic")) {
			return null;
		}
		int n = attempt.charAt(2) - '0';
		if (n <= 1) {
			return null;
		}
		int index = (attempt.startsWith("CJ") ? 3 : 0) + n;
		return athlete.getRequestedWeightForAttempt(index - 1);
	}

	public static void deleted(Athlete athlete) {
		AuditActor actor = AuditContext.actor() != null ? AuditContext.actor() : AuditActor.fromCurrentUi();
		if (actor == null || AuditContext.isSuppressed()) {
			return;
		}
		String platform = athlete.getGroup() != null && athlete.getGroup().getPlatform() != null
				? athlete.getGroup().getPlatform().getName() : "competition";
		AuditLog.write(AuditEntry.builder(platform, "athlete.delete").actor(actor).athlete(athlete)
				.cause(AuditContext.cause()).build());
	}
}