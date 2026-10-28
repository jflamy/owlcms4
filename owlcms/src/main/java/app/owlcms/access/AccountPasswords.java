package app.owlcms.access;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

/** Password lifecycle outside the login itself: temporary admin password, self-service change, bulk clearing. */
public final class AccountPasswords {

	/** Route of the self-service account page, which is also the forced password-change page. */
	public static final String ACCOUNT_ROUTE = "account";

	public enum ChangeResult {
		CHANGED, NO_ACCOUNT, WRONG_CURRENT_PASSWORD, UNACCEPTABLE, SAME_AS_CURRENT
	}

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountPasswords.class);
	// routed only to owlcms.log by logback.xml, so the secret stays out of the console and startup.log
	private static final Logger secretLogger = (Logger) LoggerFactory
	        .getLogger("app.owlcms.access.TemporaryAdminPassword");
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
	private static final int TEMPORARY_LENGTH = 16;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static volatile boolean generatedAtStartup;

	private AccountPasswords() {
	}

	/**
	 * Called at each startup. When accounts mode is required and the built-in admin has no password, sets a
	 * single-use password that must be changed at the first login, and writes it to owlcms.log only.
	 *
	 * @return true if a password was generated
	 */
	public static boolean ensureAdminPassword(boolean accountsModeRequired, AuditOrigin origin) {
		return generateIfRequired(accountsModeRequired, origin) != null;
	}

	/** @return the generated password, for tests; null if none was needed */
	static synchronized String generateIfRequired(boolean accountsModeRequired, AuditOrigin origin) {
		generatedAtStartup = false;
		if (!accountsModeRequired || UserAccountRepository.adminHasPassword()) {
			return null;
		}
		UserAccount cached = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN);
		if (cached == null) {
			return null;
		}
		String password = randomPassword();
		UserAccount admin = cached.copy();
		admin.setPasswordHash(PasswordHasher.hash(password));
		admin.setMustChangePassword(true);
		UserAccountRepository.save(admin, new AuditOrigin(origin.actor(), "temporary admin password"));
		secretLogger.warn("Single-use admin password: {}  (log in as {} and set a new password)", password,
		        admin.getUsername());
		logger./**/warn("accounts mode is active and the admin account had no password: a single-use admin password was written to logs/owlcms.log");
		AuthAudit.temporaryPasswordGenerated(admin.getUsername());
		generatedAtStartup = true;
		return password;
	}

	/** @return true while the password generated at this startup has not been replaced; drives the login notice */
	public static boolean isTemporaryAdminPasswordPending() {
		if (!generatedAtStartup) {
			return false;
		}
		UserAccount admin = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN);
		return admin != null && admin.isMustChangePassword();
	}

	/** @return true if the account must change its password before opening any other page */
	public static boolean mustChangePassword(Principal principal) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT) {
			return false;
		}
		UserAccount account = UserAccountRepository.findById(principal.accountId());
		return account != null && account.isMustChangePassword();
	}

	/**
	 * A user changing their own password; the account comes from the session, never from the request.
	 *
	 * @param origin captured by the page; its actor is the same person as the principal
	 */
	public static synchronized ChangeResult changeOwnPassword(Principal principal, String currentPassword,
	        String newPassword, AuditOrigin origin) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT) {
			return ChangeResult.NO_ACCOUNT;
		}
		UserAccount cached = UserAccountRepository.findById(principal.accountId());
		if (cached == null || !cached.isEnabled() || !cached.hasPassword()) {
			return ChangeResult.NO_ACCOUNT;
		}
		if (!PasswordHasher.verify(currentPassword == null ? "" : currentPassword, cached.getPasswordHash())) {
			logger./**/warn("password change refused user={} reason=wrong current password", cached.getUsername());
			return ChangeResult.WRONG_CURRENT_PASSWORD;
		}
		if (!PasswordHasher.isAcceptable(newPassword)) {
			return ChangeResult.UNACCEPTABLE;
		}
		if (PasswordHasher.verify(newPassword, cached.getPasswordHash())) {
			return ChangeResult.SAME_AS_CURRENT;
		}
		UserAccount account = cached.copy();
		account.setPasswordHash(PasswordHasher.hash(newPassword));
		account.setMustChangePassword(false);
		UserAccountRepository.save(account, origin);
		logger.info("password changed user={} how=self", account.getUsername());
		AuthAudit.passwordChanged(account.getUsername(), "self");
		return ChangeResult.CHANGED;
	}

	/**
	 * Removes the password of every account in one group, except the built-in admin and the acting account.
	 * Cleared accounts cannot log in until an administrator sets a password; their sessions end at their next
	 * navigation.
	 *
	 * @param generic         true for the generic (role) accounts, false for the accounts of persons
	 * @param actingAccountId the administrator's account, or null for a PIN/backdoor session
	 * @return the number of accounts whose password was removed
	 */
	public static synchronized int clearPasswords(boolean generic, Long actingAccountId, String actingUser,
	        AuditOrigin origin) {
		List<String> details = JPAService.runInTransaction(em -> {
			List<String> result = new ArrayList<>();
			List<UserAccount> accounts = em.createQuery("select a from UserAccount a", UserAccount.class)
			        .getResultList();
			for (UserAccount account : accounts) {
				if (account.isGeneric() != generic || account.isBuiltInAdmin()
				        || account.getId().equals(actingAccountId)) {
					continue;
				}
				if (account.hasPassword() || account.isMustChangePassword()) {
					UserAccount before = account.copy();
					account.setPasswordHash(null);
					account.setMustChangePassword(false);
					result.add(UserAccount.auditDetail(before, account.copy()));
				}
			}
			return result;
		});
		int cleared = details.size();
		UserAccountRepository.invalidate();
		for (String detail : details) {
			AuthAudit.accountChanged(origin, "updated", detail);
		}
		String group = generic ? "generic" : "users";
		logger.info("passwords cleared by={} accounts={} count={}", actingUser, group, cleared);
		AuthAudit.passwordsCleared(actingUser, group, cleared);
		return cleared;
	}

	static String randomPassword() {
		StringBuilder password = new StringBuilder(TEMPORARY_LENGTH);
		for (int i = 0; i < TEMPORARY_LENGTH; i++) {
			password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return password.toString();
	}
}
