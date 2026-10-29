package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;

public class TokenAuthenticatorTest {

	private static final String IP = "203.0.113.7";
	private final Map<String, UserAccount> byHash = new HashMap<>();
	private TokenAuthenticator authenticator;

	@Before
	public void setUp() {
		this.byHash.clear();
		this.authenticator = new TokenAuthenticator(this.byHash::get, new LoginThrottle(() -> 1_000_000L));
	}

	@Test
	public void tokenComesFromTheParameterOrABearerHeader() {
		assertEquals("abc", TokenAuthenticator.presentedToken("abc", "Bearer other"));
		assertEquals("xyz", TokenAuthenticator.presentedToken(null, "Bearer xyz"));
		assertEquals("xyz", TokenAuthenticator.presentedToken(" ", "bearer  xyz "));
		assertNull(TokenAuthenticator.presentedToken(null, "Basic dXNlcjpwYXNz"));
		assertNull(TokenAuthenticator.presentedToken(null, null));
	}

	@Test
	public void noTokenIsMissing() {
		assertTrue(this.authenticator.authenticate(null, IP) instanceof TokenAuthenticator.Result.Missing);
	}

	@Test
	public void validTokenIdentifiesItsOwnerAndToken() {
		String token = issue("external", Role.EXTERNAL);
		TokenAuthenticator.Result result = this.authenticator.authenticate(token, IP);
		assertTrue(result instanceof TokenAuthenticator.Result.Authenticated);
		TokenAuthenticator.Result.Authenticated authenticated = (TokenAuthenticator.Result.Authenticated) result;
		assertEquals("external", authenticated.account().getUsername());
		assertEquals(AccountTokens.hash(token), authenticated.token().getTokenHash());
	}

	@Test
	public void resultsRoleIsHeldDirectlyOrThroughACompositeRole() {
		for (Role role : List.of(Role.RESULTS, Role.ADMIN, Role.COMPETITION_DIRECTOR, Role.COMPETITION_SUPERVISOR)) {
			assertTrue(role.name(), TokenAuthenticator.holds(account(role), Role.RESULTS));
		}
		assertFalse(TokenAuthenticator.holds(account(Role.EXTERNAL), Role.RESULTS));
		assertFalse(TokenAuthenticator.holds(account(Role.DISPLAYS), Role.RESULTS));
	}

	@Test
	public void unknownTokenAndDisabledAccountAreInvalid() {
		assertTrue(this.authenticator.authenticate("not-a-token1", IP) instanceof TokenAuthenticator.Result.Invalid);
		String token = issue("disabled", Role.RESULTS);
		this.byHash.get(AccountTokens.hash(token)).setEnabled(false);
		assertTrue(this.authenticator.authenticate(token, IP) instanceof TokenAuthenticator.Result.Invalid);
	}

	@Test
	public void revokedTokenIsInvalid() {
		String token = issue("revoked", Role.RESULTS);
		this.byHash.get(AccountTokens.hash(token)).replaceToken(null);
		assertTrue(this.authenticator.authenticate(token, IP) instanceof TokenAuthenticator.Result.Invalid);
	}

	@Test
	public void repeatedFailuresThrottleTheAddressOnly() {
		String token = issue("results", Role.RESULTS);
		for (int i = 0; i < LoginThrottle.FREE_FAILURES; i++) {
			this.authenticator.authenticate("bad-token-00" + i, IP);
		}
		assertTrue(this.authenticator.authenticate(token, IP) instanceof TokenAuthenticator.Result.Throttled);
		assertTrue("another address is not blocked", this.authenticator.authenticate(token, "198.51.100.9")
		        instanceof TokenAuthenticator.Result.Authenticated);
	}

	private UserAccount account(Role role) {
		UserAccount account = new UserAccount();
		account.setUsername(role.name().toLowerCase());
		account.getGrants().add(new RoleGrant(role, null));
		return account;
	}

	private String issue(String username, Role role) {
		AccountTokens.Generated generated = AccountTokens.generate();
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.getGrants().add(new RoleGrant(role, null));
		account.replaceToken(generated.record());
		this.byHash.put(generated.record().getTokenHash(), account);
		return generated.value();
	}
}
