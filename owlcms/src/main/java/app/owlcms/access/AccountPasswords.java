package app.owlcms.access;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

/**
 * Password lifecycle outside the login itself: control panel token login, self-service change, bulk clearing.
 * <p>
 * The control panel token, entered as the built-in admin's password, is the emergency way in: for the first password
 * of a new installation and for a forgotten one. Whoever has the token controls the OWLCMS process (the control panel,
 * or the secrets of a cloud app). The admin must then set a new password, after which OWLCMS restarts, ending every
 * session.
 */
public final class AccountPasswords {

	/** Route of the self-service account page, which is also the forced password-change page. */
	public static final String ACCOUNT_ROUTE = "account";

	public enum ChangeResult {
		CHANGED, NO_ACCOUNT, WRONG_CURRENT_PASSWORD, UNACCEPTABLE, SAME_AS_CURRENT
	}

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountPasswords.class);

	private AccountPasswords() {
	}

	/**
	 * After a login with the control panel token: the token becomes the admin's current password, so the account
	 * session stays valid, and the admin must change it before opening any other page.
	 *
	 * @return the updated account
	 */
	public static synchronized UserAccount controlPanelTokenLogin(UserAccount admin, String token, AuditOrigin origin) {
		UserAccount account = admin.copy();
		account.setPasswordHash(PasswordHasher.hash(token));
		account.setMustChangePassword(true);
		return UserAccountRepository.save(account, origin);
	}

	/**
	 * @return true while the admin logged in with the control panel token has not set a new password; the
	 *         change-password page then does not ask for the current password
	 */
	public static boolean isControlPanelTokenLogin(Principal principal) {
		UserAccount account = ownAccount(principal);
		String token = SameMachineProof.configuredToken();
		return account != null && account.isBuiltInAdmin() && account.isMustChangePassword() && token != null
		        && account.hasPassword() && PasswordHasher.verify(token, account.getPasswordHash());
	}

	/** @return true if the account must change its password before opening any other page */
	public static boolean mustChangePassword(Principal principal) {
		UserAccount account = ownAccount(principal);
		return account != null && account.isMustChangePassword();
	}

	private static UserAccount ownAccount(Principal principal) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT) {
			return null;
		}
		return UserAccountRepository.findById(principal.accountId());
	}

	/**
	 * The admin logged in with the control panel token sets a new password, without the current one (the token).
	 * The caller then restarts OWLCMS.
	 */
	public static synchronized ChangeResult changeAfterControlPanelToken(Principal principal, String newPassword,
	        AuditOrigin origin) {
		if (!isControlPanelTokenLogin(principal)) {
			return ChangeResult.NO_ACCOUNT;
		}
		UserAccount cached = ownAccount(principal);
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
		logger.info("password changed user={} how=control-panel-token", account.getUsername());
		AuthAudit.passwordChanged(account.getUsername(), "control-panel-token");
		return ChangeResult.CHANGED;
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
}
