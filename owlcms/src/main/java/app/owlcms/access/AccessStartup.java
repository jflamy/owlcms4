package app.owlcms.access;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
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
		AuditOrigin origin = AuditOrigin.system("startup");
		UserAccountRepository.ensureBuiltInAdmin(origin);
		Config config = Config.getCurrent();
		if (!config.isAccountsMode()) {
			AccountPasswords.ensureAdminPassword(false, origin);
			return;
		}
		if (StartupUtils.getStringParam("pin") != null) {
			logger./**/warn("OWLCMS_PIN is ignored because accounts mode is active");
		}
		if (FeatureSwitch.IWF_COMPLIANCE.isEffective(config)) {
			String override = StartupUtils.getStringParam("accessMode");
			if (override != null && "PIN".equalsIgnoreCase(override.trim())) {
				logger./**/warn("OWLCMS_ACCESSMODE=PIN is ignored because iwfCompliance requires accounts mode");
			} else if (Boolean.TRUE.equals(config.getPinModeChosen())) {
				logger.info("the PIN mode choice is ignored because iwfCompliance requires accounts mode");
			}
		}
		AccountPasswords.ensureAdminPassword(true, origin);
	}

	/** @param origin captured by the caller, on its own thread, and passed to the background work */
	public static synchronized CompletableFuture<Void> initializeRoleAccountsInBackground(AuditOrigin origin) {
		initialization = CompletableFuture.runAsync(() -> UserAccountRepository.ensureRoleAccounts(origin),
		        ACCOUNT_INITIALIZER)
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

	public static synchronized CompletableFuture<List<UserAccount>> recomputeGenericAccountsInBackground(
	        AuditOrigin origin) {
		CompletableFuture<List<UserAccount>> reset = CompletableFuture.supplyAsync(
		        () -> UserAccountRepository.recomputeGenericAccounts(origin), ACCOUNT_INITIALIZER)
		        .whenComplete((accounts, failure) -> {
			        if (failure != null) {
				        logger./**/warn("Unable to recompute generic accounts", failure);
			        }
		        });
		initialization = reset.thenApply(accounts -> null);
		return reset;
	}

	/** After a platform is added or renamed, generic accounts are created or renamed at once in accounts mode. */
	public static void platformsChanged(AuditOrigin origin) {
		if (Config.getCurrent().isAccountsMode()) {
			initializeRoleAccountsInBackground(origin);
		}
	}

	/** Clears the passwords of one group of accounts but the built-in admin and the acting account; completes with the count. */
	public static CompletableFuture<Integer> clearPasswordsInBackground(boolean generic, Long actingAccountId,
	        String actingUser, AuditOrigin origin) {
		return CompletableFuture.supplyAsync(
		        () -> AccountPasswords.clearPasswords(generic, actingAccountId, actingUser, origin), ACCOUNT_INITIALIZER)
		        .whenComplete((count, failure) -> {
			        if (failure != null) {
				        logger./**/warn("Unable to clear account passwords", failure);
			        }
		        });
	}

	public static void initializeRoleAccountsAfterDataLoad() {
		if (Config.getCurrent().isAccountsMode()) {
			UserAccountRepository.ensureRoleAccounts(AuditOrigin.system("startup"));
		}
	}
}
