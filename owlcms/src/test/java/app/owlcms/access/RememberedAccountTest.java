package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;

public class RememberedAccountTest {

	private static final String SECRET = RememberedBrowser.newSecret();
	private static final String HASH = "pbkdf2-sha256$600000$abc$def";
	private static final long NOW = 1_800_000_000L;
	private static final long MIDNIGHT = NOW + 3600;
	private static final List<String> PLATFORMS = List.of("A", "B");

	private static UserAccount account(String username, RoleGrant... grants) {
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.setPasswordHash(HASH);
		account.setGrants(List.of(grants));
		return account;
	}

	private static Principal restore(String cookie, UserAccount account, boolean allowed, long now) {
		return RememberedAccount.restore(cookie, allowed, SECRET, Map.of(account.getId(), account)::get, PLATFORMS,
		        now);
	}

	@Test
	public void aCookieLogsTheAccountInAgainUntilMidnight() {
		UserAccount results = account("results", new RoleGrant(Role.RESULTS, null));
		String cookie = RememberedAccount.cookieValue(results.getId(), null, HASH, SECRET, MIDNIGHT);

		Principal principal = restore(cookie, results, true, NOW);
		assertNotNull(principal);
		assertEquals("results", principal.username());
		assertEquals(Principal.AuthSource.ACCOUNT, principal.source());
		assertNull(principal.loginPlatform());

		assertNull("midnight has passed", restore(cookie, results, true, MIDNIGHT));
		assertNull("the setting was withdrawn", restore(cookie, results, false, NOW));
	}

	@Test
	public void theCookieStopsWorkingWhenTheAccountChanges() {
		UserAccount results = account("results", new RoleGrant(Role.RESULTS, null));
		String cookie = RememberedAccount.cookieValue(results.getId(), null, HASH, SECRET, MIDNIGHT);

		results.setPasswordHash("pbkdf2-sha256$600000$xyz$new");
		assertNull("password changed", restore(cookie, results, true, NOW));

		results.setPasswordHash(HASH);
		results.setEnabled(false);
		assertNull("account disabled", restore(cookie, results, true, NOW));

		results.setEnabled(true);
		results.setGrants(List.of(new RoleGrant(Role.RESULTS, null), new RoleGrant(Role.ADMIN, null)));
		assertNull("administrators always enter their password", restore(cookie, results, true, NOW));

		assertNull("all browsers forgotten", RememberedAccount.restore(cookie, true, RememberedBrowser.newSecret(),
		        Map.of(results.getId(), results)::get, PLATFORMS, NOW));
	}

	@Test
	public void tamperedOrMalformedCookiesAreRefused() {
		UserAccount results = account("results", new RoleGrant(Role.RESULTS, null));
		UserAccount other = account("other", new RoleGrant(Role.RESULTS, null));
		String cookie = RememberedAccount.cookieValue(results.getId(), null, HASH, SECRET, MIDNIGHT);

		String extended = cookie.replace("." + MIDNIGHT + ".", "." + (MIDNIGHT + 86400) + ".");
		assertNull("the expiry is covered by the MAC", restore(extended, results, true, NOW));
		String moved = cookie.replace("." + results.getId() + ".", "." + other.getId() + ".");
		assertNull("the account is covered by the MAC", restore(moved, other, true, NOW));
		assertNull(restore("garbage", results, true, NOW));
		assertNull(restore("a1.x.-.1.mac", results, true, NOW));
		assertNull(restore(null, results, true, NOW));
	}

	@Test
	public void anAccountLockedToAPlatformGetsTheOneChosenAtLogin() {
		UserAccount marshal = account("marshal", new RoleGrant(Role.MARSHAL, null));
		String cookie = RememberedAccount.cookieValue(marshal.getId(), "B", HASH, SECRET, MIDNIGHT);
		assertEquals("B", restore(cookie, marshal, true, NOW).loginPlatform());

		String other = cookie.replaceFirst("\\.Qg\\.", ".QQ.");
		assertNull("the platform is covered by the MAC", restore(other, marshal, true, NOW));

		marshal.setGrants(List.of(new RoleGrant(Role.MARSHAL, "A")));
		assertNull("the platform can no longer be chosen", restore(cookie, marshal, true, NOW));

		String noPlatform = RememberedAccount.cookieValue(marshal.getId(), null, HASH, SECRET, MIDNIGHT);
		assertNull("a platform is required", restore(noPlatform, marshal, true, NOW));
	}

	@Test
	public void onlyEnabledNonAdministratorAccountsWithAPasswordAreRememberable() {
		assertTrue(RememberedAccount.isRememberable(account("marshal", new RoleGrant(Role.MARSHAL, "A"))));
		assertFalse(RememberedAccount.isRememberable(account("boss", new RoleGrant(Role.ADMIN, null))));
		assertFalse("the built-in admin is excluded even if its grants have not been repaired yet",
		        RememberedAccount.isRememberable(account(UserAccount.BUILT_IN_ADMIN)));
		UserAccount mustChange = account("results", new RoleGrant(Role.RESULTS, null));
		mustChange.setMustChangePassword(true);
		assertFalse(RememberedAccount.isRememberable(mustChange));
		UserAccount noPassword = account("results", new RoleGrant(Role.RESULTS, null));
		noPassword.setPasswordHash(null);
		assertFalse(RememberedAccount.isRememberable(noPassword));
		assertFalse(RememberedAccount.isRememberable(null));
	}

	@Test
	public void theLoginIsKeptUntilTheNextMidnightInTheZone() {
		ZoneId montreal = ZoneId.of("America/Montreal");
		Instant afternoon = ZonedDateTime.of(2026, 11, 18, 13, 26, 0, 0, montreal).toInstant();
		long expected = ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, montreal).toEpochSecond();
		assertEquals(expected, RememberedAccount.nextMidnight(afternoon, montreal));

		Instant justBefore = ZonedDateTime.of(2026, 11, 18, 23, 59, 59, 0, montreal).toInstant();
		assertEquals(expected, RememberedAccount.nextMidnight(justBefore, montreal));
	}
}
