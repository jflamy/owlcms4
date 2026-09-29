package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import app.owlcms.access.AccountAuthenticator.Result;
import app.owlcms.data.account.UserAccount;

public class AccountAuthenticatorTest {

	private static final String IP = "10.0.0.1";
	private final AtomicLong now = new AtomicLong(1_000_000L);
	private LoginThrottle throttle;
	private UserAccount bob;
	private UserAccount noPassword;
	private UserAccount disabled;
	private boolean ipAllowed;
	private AccountAuthenticator authenticator;

	@Before
	public void setUp() {
		this.throttle = new LoginThrottle(this.now::get);
		this.ipAllowed = true;
		this.bob = account("bob", "correct horse", true);
		this.noPassword = account("nopass", null, true);
		this.disabled = account("gone", "correct horse", false);
		Map<String, UserAccount> accounts = Map.of("bob", this.bob, "nopass", this.noPassword, "gone", this.disabled);
		this.authenticator = new AccountAuthenticator(
		        name -> accounts.get(UserAccount.normalizeUsername(name)), this.throttle, ip -> this.ipAllowed);
	}

	private static UserAccount account(String name, String password, boolean enabled) {
		UserAccount account = new UserAccount();
		account.setUsername(name);
		account.setEnabled(enabled);
		if (password != null) {
			account.setPasswordHash(PasswordHasher.hash(password));
		}
		return account;
	}

	@Test
	public void correctPasswordSucceedsAndUserNameIsCaseInsensitive() {
		Result result = this.authenticator.authenticate("  Bob ", "correct horse", IP);
		assertTrue(result instanceof Result.Success);
		assertEquals(this.bob, ((Result.Success) result).account());
	}

	@Test
	public void wrongPasswordUnknownUserNoPasswordAndDisabledAreAllTheSameFailure() {
		assertTrue(this.authenticator.authenticate("bob", "wrong password", IP) instanceof Result.Failure);
		assertTrue(this.authenticator.authenticate("nobody", "correct horse", IP) instanceof Result.Failure);
		assertTrue(this.authenticator.authenticate("nopass", "", IP) instanceof Result.Failure);
		assertTrue(this.authenticator.authenticate("gone", "correct horse", IP) instanceof Result.Failure);
		assertTrue(this.authenticator.authenticate(null, null, IP) instanceof Result.Failure);
	}

	@Test
	public void addressOutsideTheOfficialsListIsRefusedBeforeAnythingElse() {
		this.ipAllowed = false;
		assertTrue(this.authenticator.authenticate("bob", "correct horse", IP) instanceof Result.IpRefused);
		assertEquals(0, this.throttle.blockedSeconds("bob", IP));
	}

	@Test
	public void repeatedFailuresThrottleEvenTheCorrectPassword() {
		for (int i = 0; i < 5; i++) {
			this.authenticator.authenticate("bob", "wrong password", IP);
		}
		Result result = this.authenticator.authenticate("bob", "correct horse", IP);
		assertTrue(result instanceof Result.Throttled);
		assertEquals(30, ((Result.Throttled) result).seconds());

		this.now.addAndGet(30_000);
		assertTrue(this.authenticator.authenticate("bob", "correct horse", IP) instanceof Result.Success);
	}

	@Test
	public void successClearsEarlierFailures() {
		for (int i = 0; i < 4; i++) {
			this.authenticator.authenticate("bob", "wrong password", IP);
		}
		assertTrue(this.authenticator.authenticate("bob", "correct horse", IP) instanceof Result.Success);
		for (int i = 0; i < 4; i++) {
			this.authenticator.authenticate("bob", "wrong password", IP);
		}
		assertTrue(this.authenticator.authenticate("bob", "correct horse", IP) instanceof Result.Success);
	}
}
