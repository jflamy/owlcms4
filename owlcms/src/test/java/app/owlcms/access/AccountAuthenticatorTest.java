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
	private static final String TOKEN = "panel-token-123";
	private final AtomicLong now = new AtomicLong(1_000_000L);
	private LoginThrottle throttle;
	private UserAccount bob;
	private UserAccount noPassword;
	private UserAccount disabled;
	private UserAccount admin;
	private String configuredToken;
	private AccountAuthenticator authenticator;

	@Before
	public void setUp() {
		this.throttle = new LoginThrottle(this.now::get);
		this.configuredToken = TOKEN;
		this.bob = account("bob", "correct horse", true);
		this.noPassword = account("nopass", null, true);
		this.disabled = account("gone", "correct horse", false);
		this.admin = account(UserAccount.BUILT_IN_ADMIN, "admin password", true);
		Map<String, UserAccount> accounts = Map.of("bob", this.bob, "nopass", this.noPassword, "gone", this.disabled,
		        UserAccount.BUILT_IN_ADMIN, this.admin);
		this.authenticator = new AccountAuthenticator(
		        name -> accounts.get(UserAccount.normalizeUsername(name)), this.throttle,
		        () -> this.configuredToken);
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
	public void controlPanelTokenLogsInOnlyTheBuiltInAdmin() {
		Result result = this.authenticator.authenticate("Admin", TOKEN, IP);
		assertTrue(result instanceof Result.ControlPanelToken);
		assertEquals(this.admin, ((Result.ControlPanelToken) result).account());
		assertTrue("the admin's own password still works",
		        this.authenticator.authenticate("admin", "admin password", IP) instanceof Result.Success);
		assertTrue(this.authenticator.authenticate("bob", TOKEN, IP) instanceof Result.Failure);
		assertTrue(this.authenticator.authenticate("admin", TOKEN + "x", IP) instanceof Result.Failure);
	}

	@Test
	public void withoutAConfiguredTokenNothingMatches() {
		this.configuredToken = null;
		assertTrue(this.authenticator.authenticate("admin", TOKEN, IP) instanceof Result.Failure);
		assertTrue(this.authenticator.authenticate("admin", "", IP) instanceof Result.Failure);
	}

	@Test
	public void tokenGuessesAreThrottled() {
		for (int i = 0; i < 5; i++) {
			this.authenticator.authenticate("admin", "guess " + i, IP);
		}
		assertTrue(this.authenticator.authenticate("admin", TOKEN, IP) instanceof Result.Throttled);
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
