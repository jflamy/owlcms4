package app.owlcms.audit;

import java.util.List;

import app.owlcms.audit.AthleteDiff.Change;
import app.owlcms.data.athlete.Athlete;

public final class AthleteAudit {
	private AthleteAudit() {
	}

	public static void write(Athlete athlete, List<Change> changes) {
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
		String platform = athlete.getGroup() != null && athlete.getGroup().getPlatform() != null
				? athlete.getGroup().getPlatform().getName() : "competition";
		for (Change change : changes) {
			AuditLog.write(AuditEntry.builder(platform, "athlete.change")
					.actor(actor)
					.athlete(athlete)
					.attempt(change.attempt())
					.field(change.field())
					.oldValue(change.oldValue())
					.newValue(annotate(athlete, change))
					.cause(AuditContext.cause())
					.build());
		}
	}

	/** Marks automatic progressions as (+1) after a good lift or (SAME) after a no lift. */
	static Object annotate(Athlete athlete, Change change) {
		Object value = change.newValue();
		String attempt = change.attempt();
		if (value == null || attempt == null || !change.field().endsWith(".automatic")) {
			return value;
		}
		int n = attempt.charAt(2) - '0';
		if (n <= 1) {
			return value;
		}
		int index = (attempt.startsWith("CJ") ? 3 : 0) + n;
		Integer previous = athlete.getRequestedWeightForAttempt(index - 1);
		int automatic;
		try {
			automatic = Integer.parseInt(value.toString().trim());
		} catch (NumberFormatException e) {
			return value;
		}
		if (previous == null || previous <= 0) {
			return value;
		}
		if (automatic == previous + 1) {
			return value + " (+1)";
		}
		return automatic == previous ? value + " (SAME)" : value;
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