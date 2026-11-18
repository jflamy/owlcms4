package app.owlcms.audit;

import java.util.LinkedHashMap;
import java.util.Map;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;

public record AuditEntry(String platform, AuditActor actor, String action, Athlete athlete, String session,
		String attempt, String field, Object oldValue, Object newValue, Integer priorRequested,
		Integer previousAttemptRequested, String cause, Map<String, Object> details) {
	public static Builder builder(String platform, String action) {
		return new Builder(platform, action);
	}

	public static String sessionOf(Group group) {
		return group != null ? group.getName() : null;
	}

	public static class Builder {
		private final String platform;
		private final String action;
		private AuditActor actor;
		private Athlete athlete;
		private String session;
		private boolean sessionSet;
		private String attempt;
		private String field;
		private Object oldValue;
		private Object newValue;
		private Integer priorRequested;
		private Integer previousAttemptRequested;
		private String cause;
		private Map<String, Object> details = Map.of();

		Builder(String platform, String action) {
			this.platform = platform;
			this.action = action;
		}

		public Builder actor(AuditActor actor) { this.actor = actor; return this; }
		public Builder athlete(Athlete athlete) { this.athlete = athlete; return this; }
		/** Overrides the default session, which is the athlete's. */
		public Builder session(String session) { this.session = session; this.sessionSet = true; return this; }
		public Builder attempt(String attempt) { this.attempt = attempt; return this; }
		public Builder field(String field) { this.field = field; return this; }
		public Builder oldValue(Object oldValue) { this.oldValue = oldValue; return this; }
		public Builder newValue(Object newValue) { this.newValue = newValue; return this; }
		public Builder priorRequested(Integer priorRequested) { this.priorRequested = priorRequested; return this; }
		public Builder previousAttemptRequested(Integer previous) { this.previousAttemptRequested = previous; return this; }
		public Builder cause(String cause) { this.cause = cause; return this; }
		public Builder details(Map<String, ?> details) {
			this.details = details == null ? Map.of() : new LinkedHashMap<>(details);
			return this;
		}

		public AuditEntry build() {
			String recordSession = this.sessionSet ? this.session
					: this.athlete != null ? sessionOf(this.athlete.getGroup()) : null;
			return new AuditEntry(this.platform, this.actor, this.action, this.athlete, recordSession, this.attempt,
					this.field, this.oldValue, this.newValue, this.priorRequested, this.previousAttemptRequested,
					this.cause, this.details);
		}
	}
}