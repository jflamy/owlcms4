package app.owlcms.access;

import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import app.owlcms.data.account.UserAccount;
import ch.qos.logback.classic.Logger;

/** Checks a user name and password; no Vaadin types and no session, so it can be unit-tested. */
public final class AccountAuthenticator {

	public sealed interface Result {
		record Success(UserAccount account) implements Result {
		}

		/** The built-in admin, with the control panel token as the password: the admin must set a new password. */
		record ControlPanelToken(UserAccount account) implements Result {
		}

		/** Unknown user, wrong password or disabled account; deliberately indistinguishable. */
		record Failure() implements Result {
		}

		record Throttled(long seconds) implements Result {
		}
	}

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountAuthenticator.class);

	// verified against when the user is unknown, so that timing does not reveal which names exist
	private static volatile String decoyHash;

	private final Function<String, UserAccount> lookup;
	private final LoginThrottle throttle;
	private final Supplier<String> controlPanelToken;

	public AccountAuthenticator(Function<String, UserAccount> lookup, LoginThrottle throttle,
	        Supplier<String> controlPanelToken) {
		this.lookup = lookup;
		this.throttle = throttle;
		this.controlPanelToken = controlPanelToken;
	}

	public Result authenticate(String username, String password, String clientIp) {
		long blocked = this.throttle.blockedSeconds(username, clientIp);
		if (blocked > 0) {
			logger./**/warn("login throttled user={} ip={} seconds={}", username, clientIp, blocked);
			return new Result.Throttled(blocked);
		}

		UserAccount account = username == null ? null : this.lookup.apply(username);
		String candidate = password == null ? "" : password;
		if (account != null && account.isBuiltInAdmin()
		        && SameMachineProof.matches(candidate, this.controlPanelToken.get())) {
			this.throttle.success(username, clientIp);
			logger./**/warn("admin login with the control panel token ip={}", clientIp);
			return new Result.ControlPanelToken(account);
		}
		boolean valid;
		if (account != null && account.hasPassword()) {
			valid = PasswordHasher.verify(candidate, account.getPasswordHash());
		} else {
			PasswordHasher.verify(candidate, decoy());
			valid = false;
		}

		if (valid && account.isEnabled()) {
			this.throttle.success(username, clientIp);
			return new Result.Success(account);
		}
		this.throttle.fail(username, clientIp);
		logger./**/warn("login failed user={} ip={}", username, clientIp);
		return new Result.Failure();
	}

	private static String decoy() {
		String hash = decoyHash;
		if (hash == null) {
			hash = PasswordHasher.hash("decoy-not-a-real-password");
			decoyHash = hash;
		}
		return hash;
	}
}
