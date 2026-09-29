package app.owlcms.access;

import org.slf4j.LoggerFactory;

import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/** Startup work for access control: the built-in admin account and configuration warnings. */
public final class AccessStartup {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccessStartup.class);

	private AccessStartup() {
	}

	public static void run() {
		UserAccountRepository.ensureBuiltInAdmin();
		if (!Config.getCurrent().isAccountsMode()) {
			return;
		}
		if (StartupUtils.getStringParam("pin") != null) {
			logger./**/warn("OWLCMS_PIN is ignored because accounts mode is active");
		}
		if (!UserAccountRepository.adminHasPassword()) {
			logger./**/error(
			        "accounts mode is active but the admin account has no password: only backdoor access works. Set OWLCMS_BACKDOOR to the addresses that may administer.");
		}
	}
}
