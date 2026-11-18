package app.owlcms.audit;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
		Map<String, Object> detail = AuditFormat.fields("how", how);
		AuditActor actor = AuditActor.login(mode, clean(user));
		AuditLog.write(AuditEntry.builder(COMPETITION, "auth.login").actor(actor).details(detail).build());
		for (String platform : platforms) {
			if (platform != null && !platform.isBlank() && !COMPETITION.equals(platform)) {
				AuditLog.write(AuditEntry.builder(platform, "auth.login").actor(actor).details(detail).build());
			}
		}
	}

	public static void loginFailed(String mode, String attemptedUser, String reason) {
		write("auth.login-failed", mode, attemptedUser, AuditFormat.fields("reason", reason), null);
	}

	/** @param loginPlatform the platform the session was logged in to, or null */
	public static void loggedOut(String mode, String user, String reason, String loginPlatform) {
		write("auth.logout", mode, user, AuditFormat.fields("reason", reason), loginPlatform);
	}

	/** @param how "self" when users change their own password */
	public static void passwordChanged(String user, String how) {
		write("auth.password-changed", "ACCOUNTS", user, AuditFormat.fields("how", how), null);
	}

	public static void passwordsCleared(String byUser, String accounts, int count) {
		write("auth.passwords-cleared", "ACCOUNTS", byUser, AuditFormat.fields("accounts", accounts, "count", count),
		        null);
	}

	/**
	 * An account was created, changed or deleted, by whatever path. The detail never contains password values or
	 * hashes.
	 *
	 * @param action "added", "updated" or "deleted"
	 */
	public static void accountChanged(AuditOrigin origin, String action, Map<String, Object> detail) {
		writeFor(origin, "auth.account-" + action, detail);
	}

	public static void accessModeChanged(AuditOrigin origin, String from, String to) {
		writeFor(origin, "auth.access-mode", AuditFormat.fields("from", from, "to", to));
	}

	/** The IWF compliance check was turned on or off; it only shows or hides the list of requirements not met. */
	public static void iwfComplianceCheckChanged(AuditOrigin origin, boolean on) {
		writeFor(origin, "auth.iwf-compliance", AuditFormat.fields("check", on ? "on" : "off"));
	}

	/** @param notMet the requirements not met at startup, empty when all are */
	public static void iwfComplianceChecked(AuditOrigin origin, List<String> notMet) {
		writeFor(origin, "auth.iwf-compliance-check", AuditFormat.fields("result", notMet.isEmpty() ? "met" : "not-met",
		        "missing", notMet.isEmpty() ? "-" : String.join(" ", notMet)));
	}

	/** A browser was given a "remember this browser" cookie after a correct PIN. */
	public static void browserRemembered(String clientIp, String kind) {
		AuditLog.write(AuditEntry.builder(COMPETITION, "auth.browser-remembered").actor(AuditActor.http(clientIp))
		        .details(AuditFormat.fields("pin", kind)).build());
	}

	/**
	 * A browser was given a cookie that keeps the account logged in until midnight, after a password login.
	 *
	 * @param until the expiry, as an ISO date-time
	 */
	public static void accountLoginRemembered(String clientIp, String user, String until) {
		AuditLog.write(AuditEntry.builder(COMPETITION, "auth.browser-remembered").actor(AuditActor.http(clientIp))
		        .details(AuditFormat.fields("account", clean(user), "until", until)).build());
	}

	/** Remembering account logins for the day was allowed or withdrawn. */
	public static void rememberAccountLoginsChanged(AuditOrigin origin, boolean allowed) {
		writeFor(origin, "auth.remember-account-logins", AuditFormat.fields("allowed", allowed ? "on" : "off"));
	}

	/**
	 * A token presented on a non-interactive request was refused. Never pass the token itself.
	 *
	 * @param reason   "no-token", "missing", "invalid", "throttled", "forbidden", "not-token-page", "malformed",
	 *                 "platform", "revoked" or "display-ip"
	 * @param username the token's owner when known, else null
	 * @param tokenId  the token identifier when known, else null
	 */
	public static void tokenRefused(String clientIp, String path, String reason, String username, String tokenId) {
		AuditActor actor = username == null ? AuditActor.http(clientIp)
		        : AuditActor.token(clean(username), tokenId, clientIp);
		AuditLog.write(AuditEntry.builder(COMPETITION, "auth.token-refused").actor(actor)
		        .details(AuditFormat.fields("reason", reason, "path", path)).build());
	}

	/** A display tab was opened with an account token. Never pass the token itself. */
	public static void tokenAccepted(String clientIp, String path, String username, String tokenId) {
		AuditLog.write(AuditEntry.builder(COMPETITION, "auth.token-accepted")
		        .actor(AuditActor.token(clean(username), tokenId, clientIp))
		        .details(AuditFormat.fields("path", path)).build());
	}

	private static void writeFor(AuditOrigin origin, String action, Map<String, Object> detail) {
		AuditLog.write(AuditEntry.builder(COMPETITION, action).actor(origin.actor()).cause(origin.cause())
		        .details(detail).build());
	}

	private static void write(String action, String mode, String user, Map<String, Object> detail, String platform) {
		AuditActor actor = AuditActor.login(mode, clean(user));
		AuditLog.write(AuditEntry.builder(COMPETITION, action).actor(actor).details(detail).build());
		if (platform != null && !platform.isBlank() && !COMPETITION.equals(platform)) {
			AuditLog.write(AuditEntry.builder(platform, action).actor(actor).details(detail).build());
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
