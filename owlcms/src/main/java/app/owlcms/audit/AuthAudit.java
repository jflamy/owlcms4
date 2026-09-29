package app.owlcms.audit;

import java.util.Collection;

/** Logins, failed logins and logouts: written to the competition log, and to the platform log when one is involved. */
public final class AuthAudit {

	private static final String COMPETITION = "competition";
	private static final int MAX_NAME = 64;

	private AuthAudit() {
	}

	/** @param platforms real platforms on which authentication granted access */
	public static void loginSucceeded(String mode, String user, String how, Collection<String> platforms) {
		String detail = AuditFormat.kv("how", how);
		AuditActor actor = AuditActor.login(mode, clean(user));
		AuditLog.write(AuditEntry.builder(COMPETITION, "auth.login").actor(actor).detail(detail).build());
		for (String platform : platforms) {
			if (platform != null && !platform.isBlank() && !COMPETITION.equals(platform)) {
				AuditLog.write(AuditEntry.builder(platform, "auth.login").actor(actor).detail(detail).build());
			}
		}
	}

	public static void loginFailed(String mode, String attemptedUser, String reason) {
		write("auth.login-failed", mode, attemptedUser, AuditFormat.kv("reason", reason), null);
	}

	/** @param loginPlatform the platform the session was logged in to, or null */
	public static void loggedOut(String mode, String user, String reason, String loginPlatform) {
		write("auth.logout", mode, user, AuditFormat.kv("reason", reason), loginPlatform);
	}

	private static void write(String action, String mode, String user, String detail, String platform) {
		AuditActor actor = AuditActor.login(mode, clean(user));
		AuditLog.write(AuditEntry.builder(COMPETITION, action).actor(actor).detail(detail).build());
		if (platform != null && !platform.isBlank() && !COMPETITION.equals(platform)) {
			AuditLog.write(AuditEntry.builder(platform, action).actor(actor).detail(detail).build());
		}
	}

	/** Typed user names are untrusted text that ends up in a log line. */
	static String clean(String name) {
		if (name == null || name.isBlank()) {
			return "-";
		}
		String cleaned = name.replaceAll("\\p{Cntrl}", " ").trim();
		return cleaned.length() > MAX_NAME ? cleaned.substring(0, MAX_NAME) : cleaned;
	}
}
