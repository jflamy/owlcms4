package app.owlcms.audit;

import app.owlcms.data.athlete.Athlete;

public record AuditEntry(String platform, AuditActor actor, String action, Athlete athlete, String attempt,
		String field, Object oldValue, Object newValue, String cause, String detail) {
	public static Builder builder(String platform, String action) {
		return new Builder(platform, action);
	}

	public static class Builder {
		private final String platform;
		private final String action;
		private AuditActor actor;
		private Athlete athlete;
		private String attempt;
		private String field;
		private Object oldValue;
		private Object newValue;
		private String cause;
		private String detail;

		Builder(String platform, String action) {
			this.platform = platform;
			this.action = action;
		}

		public Builder actor(AuditActor actor) { this.actor = actor; return this; }
		public Builder athlete(Athlete athlete) { this.athlete = athlete; return this; }
		public Builder attempt(String attempt) { this.attempt = attempt; return this; }
		public Builder field(String field) { this.field = field; return this; }
		public Builder oldValue(Object oldValue) { this.oldValue = oldValue; return this; }
		public Builder newValue(Object newValue) { this.newValue = newValue; return this; }
		public Builder cause(String cause) { this.cause = cause; return this; }
		public Builder detail(String detail) { this.detail = detail; return this; }

		public AuditEntry build() {
			return new AuditEntry(this.platform, this.actor, this.action, this.athlete, this.attempt, this.field,
					this.oldValue, this.newValue, this.cause, this.detail);
		}
	}
}