package app.owlcms.audit;

import java.util.Objects;

import app.owlcms.data.athlete.Athlete;

public final class SettingsAudit {
	private SettingsAudit() {
	}

	public static void change(String platform, String action, String field, Object oldValue, Object newValue,
			String detail) {
		if (Objects.equals(oldValue, newValue)) {
			return;
		}
		AuditActor actor = currentActor();
		if (actor == null) {
			return;
		}
		AuditLog.write(AuditEntry.builder(platform, action).actor(actor).field(field).oldValue(oldValue)
				.newValue(newValue).cause(AuditContext.cause()).detail(detail).build());
	}

	public static void event(String platform, String action, AuditActor actor, Athlete athlete, String field,
			Object oldValue, Object newValue, String detail) {
		AuditActor effective = actor != null ? actor : currentActor();
		if (effective == null) {
			return;
		}
		AuditLog.write(AuditEntry.builder(platform, action).actor(effective).athlete(athlete).field(field)
				.oldValue(oldValue).newValue(newValue).cause(AuditContext.cause()).detail(detail).build());
	}

	private static AuditActor currentActor() {
		AuditActor actor = AuditContext.actor();
		return actor != null ? actor : AuditActor.fromCurrentUi();
	}
}
