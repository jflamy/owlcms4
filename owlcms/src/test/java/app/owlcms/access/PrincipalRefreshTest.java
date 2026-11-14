package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import app.owlcms.access.Principal.AuthSource;
import app.owlcms.data.account.RoleGrant;

public class PrincipalRefreshTest {

	@Test
	public void initialAndUnchangedAuthorizationDoNotRefresh() {
		PrincipalRefresh navigation = new PrincipalRefresh();
		Queue<Runnable> deferred = new ArrayDeque<>();
		AtomicInteger refreshes = new AtomicInteger();

		navigation.authenticated(account(Role.RESULTS), deferred::add, refreshes::incrementAndGet);
		navigation.authenticated(account(Role.RESULTS), deferred::add, refreshes::incrementAndGet);

		assertTrue(deferred.isEmpty());
		assertEquals(0, refreshes.get());
	}

	@Test
	public void changedGrantsRefreshAfterNavigationWithoutLooping() {
		PrincipalRefresh navigation = new PrincipalRefresh();
		Queue<Runnable> deferred = new ArrayDeque<>();
		AtomicInteger refreshes = new AtomicInteger();
		Principal original = account(Role.PREPARATION);
		Principal reduced = account(Role.RESULTS);
		PageRule results = PageRule.accesses(Set.of(Access.RESULTS), false);
		assertTrue(AccessPolicy.canOpen(original, results, null));
		assertTrue(AccessPolicy.canOpen(reduced, results, null));
		assertTrue(AccessPolicy.hasPermission(original, Permission.ATHLETE_CARD_EDITING, "A"));
		assertFalse(AccessPolicy.hasPermission(reduced, Permission.ATHLETE_CARD_EDITING, "A"));

		navigation.authenticated(original, deferred::add, refreshes::incrementAndGet);
		navigation.authenticated(reduced, deferred::add, () -> {
			refreshes.incrementAndGet();
			navigation.authenticated(account(Role.RESULTS), deferred::add, refreshes::incrementAndGet);
		});

		assertEquals(0, refreshes.get());
		assertEquals(1, deferred.size());
		deferred.remove().run();
		assertEquals(1, refreshes.get());
		assertTrue(deferred.isEmpty());
	}

	@Test
	public void principalChangesBeforeTheResponseShareOneRefresh() {
		PrincipalRefresh navigation = new PrincipalRefresh();
		Queue<Runnable> deferred = new ArrayDeque<>();
		AtomicInteger refreshes = new AtomicInteger();

		navigation.authenticated(account(Role.RESULTS), deferred::add, refreshes::incrementAndGet);
		navigation.authenticated(account(Role.PREPARATION), deferred::add, refreshes::incrementAndGet);
		navigation.authenticated(account(Role.ADMIN), deferred::add, refreshes::incrementAndGet);

		assertEquals(1, deferred.size());
		deferred.remove().run();
		navigation.authenticated(account(Role.ADMIN), deferred::add, refreshes::incrementAndGet);
		assertEquals(1, refreshes.get());
		assertTrue(deferred.isEmpty());

		navigation.authenticated(account(Role.RESULTS), deferred::add, refreshes::incrementAndGet);
		assertEquals(1, deferred.size());
		deferred.remove().run();
		assertEquals(2, refreshes.get());
	}

	@Test
	public void changedActorScopeAndTokenAlsoRefresh() {
		PrincipalRefresh navigation = new PrincipalRefresh();
		Queue<Runnable> deferred = new ArrayDeque<>();
		AtomicInteger refreshes = new AtomicInteger();
		List<RoleGrant> grants = List.of(new RoleGrant(Role.DISPLAYS, null));
		Principal original = new Principal(AuthSource.ACCOUNT, 1L, "user", grants, "A");
		navigation.authenticated(original, deferred::add, refreshes::incrementAndGet);

		for (Principal changed : List.of(
		        original.withLoginPlatform("B"),
		        new Principal(AuthSource.ACCOUNT, 1L, "user", List.of(new RoleGrant(Role.DISPLAYS, "B")), "B"),
		        new Principal(AuthSource.ACCOUNT, 2L, "other", grants, "B"),
		        new Principal(AuthSource.TOKEN, 2L, "other", grants, null, "first"),
		        new Principal(AuthSource.TOKEN, 2L, "other", grants, null, "second"))) {
			navigation.authenticated(changed, deferred::add, refreshes::incrementAndGet);
			assertEquals(1, deferred.size());
			deferred.remove().run();
			navigation.authenticated(changed, deferred::add, refreshes::incrementAndGet);
			assertTrue(deferred.isEmpty());
		}
		assertEquals(5, refreshes.get());
	}

	private static Principal account(Role role) {
		return new Principal(AuthSource.ACCOUNT, 1L, "user", List.of(new RoleGrant(role, null)), null);
	}
}
