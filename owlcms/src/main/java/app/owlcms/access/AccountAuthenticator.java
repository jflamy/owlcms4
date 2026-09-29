package app.owlcms.access;

import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.LoggerFactory;

import app.owlcms.data.account.UserAccount;
import ch.qos.logback.classic.Logger;

/** Checks a user name and password; no Vaadin types and no session, so it can be unit-tested. */
public final class AccountAuthenticator {

	public sealed interface Result {
		record Success(UserAccount account) implements Result {
		}

		/** Unknown user, wrong password or disabled account; deliberately indistinguishable. */
		record Failure() implements Result {
		}

		record Throttled(long seconds) implements Result {
		}

		/** Client address not on the officials list. */
		record IpRefused() implements Result {
		}
	}

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountAuthenticator.class);

	// verified against when the user is unknown, so that timing does not reveal which names exist
	private static volatile String decoyHash;

	private final Function<String, UserAccount> lookup;
	private final LoginThrottle throttle;
	private final Predicate<String> officialsIpAllowed;

	public AccountAuthenticator(Function<String, UserAccount> lookup, LoginThrottle throttle,
	        Predicate<String> officialsIpAllowed) {
		this.lookup = lookup;
		this.throttle = throttle;
		this.officialsIpAllowed = officialsIpAllowed;
	}

	public Result authenticate(String username, String password, String clientIp) {
		if (!this.officialsIpAllowed.test(clientIp)) {
			return new Result.IpRefused();
		}
		long blocked = this.throttle.blockedSeconds(username, clientIp);
		if (blocked > 0) {
			logger./**/warn("login throttled user={} ip={} seconds={}", username, clientIp, blocked);
			return new Result.Throttled(blocked);
		}

		UserAccount account = username == null ? null : this.lookup.apply(username);
		String candidate = password == null ? "" : password;
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
