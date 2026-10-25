package app.owlcms.access;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuditIntegrity;
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
		Config config = Config.getCurrent();
		if (!config.isAccountsMode()) {
			AccountPasswords.ensureAdminPassword(false);
			return;
		}
		if (selectedByAuditKey(config)) {
			logger.info("accounts mode selected because an audit signing key is configured and the admin account has a password; the administrator can choose PIN mode instead");
		}
		if (StartupUtils.getStringParam("pin") != null) {
			logger./**/warn("OWLCMS_PIN is ignored because accounts mode is active");
		}
		AccountPasswords.ensureAdminPassword(true);
	}

	private static boolean selectedByAuditKey(Config config) {
		String override = StartupUtils.getStringParam("accessMode");
		return AuditIntegrity.isInitialized() && AuditIntegrity.current().forcedByKey()
		        && config.getAccessMode() != AccessMode.ACCOUNTS
		        && !Boolean.TRUE.equals(config.getPinModeChosen())
		        && (override == null || !"ACCOUNTS".equalsIgnoreCase(override.trim()));
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

	public static synchronized CompletableFuture<List<UserAccount>> recomputeGenericAccountsInBackground() {
		CompletableFuture<List<UserAccount>> reset = CompletableFuture.supplyAsync(
		        UserAccountRepository::recomputeGenericAccounts, ACCOUNT_INITIALIZER)
		        .whenComplete((accounts, failure) -> {
			        if (failure != null) {
				        logger./**/warn("Unable to recompute generic accounts", failure);
			        }
		        });
		initialization = reset.thenApply(accounts -> null);
		return reset;
	}

	/** After a platform is added or renamed, generic accounts are created or renamed at once in accounts mode. */
	public static void platformsChanged() {
		if (Config.getCurrent().isAccountsMode()) {
			initializeRoleAccountsInBackground();
		}
	}

	/** Clears the passwords of one group of accounts but the built-in admin and the acting account; completes with the count. */
	public static CompletableFuture<Integer> clearPasswordsInBackground(boolean generic, Long actingAccountId,
	        String actingUser) {
		return CompletableFuture.supplyAsync(
		        () -> AccountPasswords.clearPasswords(generic, actingAccountId, actingUser), ACCOUNT_INITIALIZER)
		        .whenComplete((count, failure) -> {
			        if (failure != null) {
				        logger./**/warn("Unable to clear account passwords", failure);
			        }
		        });
	}

	public static void initializeRoleAccountsAfterDataLoad() {
		if (Config.getCurrent().isAccountsMode()) {
			UserAccountRepository.ensureRoleAccounts();
		}
	}
}
