package app.owlcms.access;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;

import app.owlcms.access.Principal.AuthSource;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/** Resolves the principal when named accounts are in use. */
public final class AccountModeAuthenticator {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountModeAuthenticator.class);

	private AccountModeAuthenticator() {
	}

	/** @return the principal, or null when the login page must be shown */
	public static Principal resolve(String path, QueryParameters queryParameters) {
		Principal existing = OwlcmsSession.getPrincipal();
		if (existing != null && existing.source() == AuthSource.ACCOUNT) {
			UserAccount account = UserAccountRepository.findById(existing.accountId());
			if (account != null && account.isEnabled()) {
				return Principal.of(account, existing.loginPlatform());
			}
			logout(existing, "account disabled or deleted");
		}

		if (AccessUtils.isBackdoorAccess()) {
			logger.info("Backdoor access from {}", AccessUtils.getClientIp());
			return Principal.admin(AuthSource.BACKDOOR);
		}

		OwlcmsSession.setRequestedUrl(path);
		OwlcmsSession.setRequestedQueryParameters(queryParameters);
		return null;
	}

	/** Forgets the identity of this session; the caller decides where to send the browser. */
	public static void logout(Principal principal, String reason) {
		logger.info("logout user={} reason={}", principal.username(), reason);
		AuthAudit.loggedOut("ACCOUNTS", principal.username(), reason, principal.loginPlatform());
		OwlcmsSession.setPrincipal(null);
	}
}
