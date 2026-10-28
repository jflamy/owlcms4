package app.owlcms.audit;

import java.util.Collection;

/**
 * Logins, failed logins, logouts, passwords, account changes and access-mode changes: written to the competition
 * log, and to the platform log when one is involved.
 */
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

	/** Never pass the password itself. */
	public static void temporaryPasswordGenerated(String user) {
		write("auth.temporary-password", "ACCOUNTS", user, AuditFormat.kv("disclosed", "owlcms.log"), null);
	}

	/** @param how "self" when users change their own password */
	public static void passwordChanged(String user, String how) {
		write("auth.password-changed", "ACCOUNTS", user, AuditFormat.kv("how", how), null);
	}

	public static void passwordsCleared(String byUser, String accounts, int count) {
		write("auth.passwords-cleared", "ACCOUNTS", byUser, AuditFormat.kvs("accounts", accounts, "count", count),
		        null);
	}

	/**
	 * An account was created, changed or deleted, by whatever path. The detail never contains password values or
	 * hashes.
	 *
	 * @param action "added", "updated" or "deleted"
	 */
	public static void accountChanged(AuditOrigin origin, String action, String detail) {
		writeFor(origin, "auth.account-" + action, detail);
	}

	public static void accessModeChanged(AuditOrigin origin, String from, String to) {
		writeFor(origin, "auth.access-mode", AuditFormat.kvs("from", from, "to", to));
	}

	private static void writeFor(AuditOrigin origin, String action, String detail) {
		AuditLog.write(AuditEntry.builder(COMPETITION, action).actor(origin.actor()).cause(origin.cause())
		        .detail(detail).build());
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
