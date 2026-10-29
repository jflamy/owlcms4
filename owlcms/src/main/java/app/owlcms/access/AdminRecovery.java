package app.owlcms.access;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.utils.StartupUtils;

/**
 * Recovery from a forgotten admin password. Started with {@code OWLCMS_ADMINRECOVERY=true}, OWLCMS runs only its
 * configuration phase and every request except the control panel stop gets the recovery form: the control panel
 * token, then a new password for the built-in admin. Once it is set, OWLCMS stays locked until restarted without the
 * setting.
 */
public final class AdminRecovery {

	public static final String SETTING = "adminRecovery";
	public static final String STOP_PATH = "/controlpanel/stop";
	/** Throttling key: recovery attempts are counted apart from logins. */
	public static final String THROTTLE_USER = "admin-recovery";

	public enum Outcome {
		NO_TOKEN_CONFIGURED, THROTTLED, INVALID_TOKEN, UNACCEPTABLE, MISMATCH, ACCEPTED
	}

	private static volatile boolean ready;
	private static volatile boolean done;

	private AdminRecovery() {
	}

	public static boolean isOn() {
		return StartupUtils.getBooleanParam(SETTING);
	}

	/** @return the control panel token OWLCMS was started with, or null */
	public static String expectedToken() {
		String token = StartupUtils.getStringParam(SameMachineProof.CONTROL_PANEL_TOKEN);
		return token == null || token.isBlank() ? null : token;
	}

	/** Called once the configuration phase is complete: the database can be used. */
	public static void markReady() {
		ready = true;
	}

	public static boolean isReady() {
		return ready;
	}

	public static boolean isDone() {
		return done;
	}

	public static boolean passesThrough(String path) {
		return STOP_PATH.equals(path);
	}

	/** Checks a submission, in this order: token configured, throttling, token, password. */
	public static Outcome check(String expectedToken, String presentedToken, String password, String confirm,
	        long blockedSeconds) {
		if (expectedToken == null || expectedToken.isBlank()) {
			return Outcome.NO_TOKEN_CONFIGURED;
		}
		if (blockedSeconds > 0) {
			return Outcome.THROTTLED;
		}
		if (!SameMachineProof.matches(presentedToken, expectedToken)) {
			return Outcome.INVALID_TOKEN;
		}
		if (!PasswordHasher.isAcceptable(password)) {
			return Outcome.UNACCEPTABLE;
		}
		if (!password.equals(confirm)) {
			return Outcome.MISMATCH;
		}
		return Outcome.ACCEPTED;
	}

	/** @return false if the password was already set during this run, or the admin account is missing */
	public static synchronized boolean setAdminPassword(String password) {
		if (done) {
			return false;
		}
		UserAccount cached = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN);
		if (cached == null) {
			return false;
		}
		UserAccount account = cached.copy();
		account.setPasswordHash(PasswordHasher.hash(password));
		account.setMustChangePassword(false);
		UserAccountRepository.save(account, AuditOrigin.system("admin recovery"));
		AuthAudit.passwordChanged(account.getUsername(), "recovery");
		done = true;
		return true;
	}
}
