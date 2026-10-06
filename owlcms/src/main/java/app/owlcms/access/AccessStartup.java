package app.owlcms.access;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/** Startup work for access control: the built-in admin account and configuration warnings. */
public final class AccessStartup {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccessStartup.class);
	private static final ExecutorService ACCOUNT_INITIALIZER = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "role-account-initializer");
		thread.setDaemon(true);
		return thread;
	});
	private static CompletableFuture<Void> initialization = CompletableFuture.completedFuture(null);

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

	public static synchronized CompletableFuture<Void> initializeRoleAccountsInBackground() {
		initialization = CompletableFuture.runAsync(UserAccountRepository::ensureRoleAccounts, ACCOUNT_INITIALIZER)
		        .whenComplete((ignored, failure) -> {
			        if (failure != null) {
				        logger./**/warn("Unable to initialize role accounts; check the User Accounts page", failure);
			        }
		        });
		return initialization;
	}

	public static synchronized CompletableFuture<Void> roleAccountInitialization() {
		return initialization;
	}

	public static synchronized CompletableFuture<List<UserAccount>> resetPlatformAccountsInBackground() {
		CompletableFuture<List<UserAccount>> reset = CompletableFuture.supplyAsync(
		        UserAccountRepository::resetPlatformAccounts, ACCOUNT_INITIALIZER)
		        .whenComplete((accounts, failure) -> {
			        if (failure != null) {
				        logger./**/warn("Unable to reset platform accounts", failure);
			        }
		        });
		initialization = reset.thenApply(accounts -> null);
		return reset;
	}

	public static void initializeRoleAccountsAfterDataLoad() {
		if (Config.getCurrent().isAccountsMode()) {
			UserAccountRepository.ensureRoleAccounts();
		}
	}
}
