package app.owlcms.data.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.Test;

import app.owlcms.access.PasswordHasher;
import app.owlcms.access.Role;

public class RoleAccountInitializationTest {

	@Test
	public void createsAllGrantableRolesWithTheCorrectPlatformScope() {
		List<UserAccount> accounts = UserAccountRepository.missingRoleAccounts(List.of(), List.of("A", "B"));
		assertEquals(23, accounts.size());
		for (Role role : Role.values()) {
			if (!role.isGrantable() || role == Role.COMPETITION_DIRECTOR) {
				assertFalse(role.name(), accounts.stream().anyMatch(a -> a.getGrants().stream()
				        .anyMatch(g -> g.getRole() == role)));
				continue;
			}
			List<String> scopes = role.isPlatformScopable() ? List.of("A", "B")
			        : Collections.singletonList(null);
			for (String scope : scopes) {
				assertEquals(1, accounts.stream()
				        .filter(a -> a.getGrants().equals(List.of(new RoleGrant(role, scope)))).count());
			}
		}
		assertTrue(accounts.stream().allMatch(UserAccount::isEnabled));
		assertTrue(accounts.stream().allMatch(UserAccount::isPasswordChangeRequired));
		assertTrue(accounts.stream().allMatch(UserAccount::isGeneric));
		assertTrue(accounts.stream().anyMatch(a -> a.getUsername().equals("announcer-A")));
		assertTrue(accounts.stream().anyMatch(a -> a.getUsername().equals("platform-B")));
		assertTrue(accounts.stream().anyMatch(a -> a.getUsername().equals("competition_supervisor")));
	}

	@Test
	public void builtInAdminIsAlwaysGenericAndOtherAccountsOnlyWhenMarked() {
		assertTrue(account("ADMIN", new RoleGrant(Role.ADMIN, null)).isGeneric());
		assertFalse(account("speaker-rm", new RoleGrant(Role.ANNOUNCER, "A")).isGeneric());
	}

	@Test
	public void addingASecondPlatformRenamesTheExistingAccountsAndKeepsTheirPasswords() {
		List<UserAccount> existing = UserAccountRepository.missingRoleAccounts(List.of(), List.of("A"));
		UserAccount announcer = existing.stream().filter(a -> a.getUsername().equals("announcer"))
		        .findFirst().orElseThrow();
		announcer.setEnabled(false);
		announcer.setPasswordHash(PasswordHasher.hash("existing password"));
		assertTrue(UserAccountRepository.missingRoleAccounts(existing, List.of("A")).isEmpty());
		List<UserAccount> added = UserAccountRepository.missingRoleAccounts(existing, List.of("A", "B"));
		assertEquals(8, added.size());
		assertTrue(added.stream().allMatch(a -> a.getUsername().endsWith("-B")));
		assertEquals("announcer-A", announcer.getUsername());
		assertFalse(announcer.isEnabled());
		assertTrue(PasswordHasher.verify("existing password", announcer.getPasswordHash()));
		assertTrue(UserAccountRepository.missingRoleAccounts(
		        Stream.concat(existing.stream(), added.stream()).toList(), List.of("A", "B")).isEmpty());
	}

	@Test
	public void recomputeFollowsPlatformsBeingAddedAndRemoved() {
		List<UserAccount> accounts = new ArrayList<>();
		UserAccount admin = account("admin", new RoleGrant(Role.ADMIN, null));
		accounts.add(admin);
		UserAccount person = account("speaker-rm", new RoleGrant(Role.ANNOUNCER, "RED"));
		accounts.add(person);
		accounts.addAll(UserAccountRepository.missingRoleAccounts(accounts, List.of("RED", "BLUE")));
		assertEquals(2, platformSets(accounts));

		accounts.addAll(UserAccountRepository.missingRoleAccounts(accounts, List.of("RED", "BLUE", "WHITE")));
		assertEquals(3, platformSets(accounts));

		// deleting a platform strips its grants, as UserAccountRepository.removePlatform does
		accounts.forEach(a -> a.getGrants().removeIf(g -> "RED".equals(g.getPlatformName())));
		List<String> platforms = List.of("BLUE", "WHITE");
		List<UserAccount> unmatched = UserAccountRepository.unmatchedGenericAccounts(accounts, platforms);
		assertEquals(8, unmatched.size());
		assertTrue(unmatched.stream().allMatch(a -> a.getUsername().endsWith("-RED")));
		accounts.removeAll(unmatched);
		assertTrue(UserAccountRepository.missingRoleAccounts(accounts, platforms).isEmpty());
		assertEquals(2, platformSets(accounts));
		assertTrue("user accounts are never recomputed", accounts.contains(person));
		assertTrue(accounts.contains(admin));

		accounts.forEach(a -> a.getGrants().removeIf(g -> "BLUE".equals(g.getPlatformName())));
		List<String> single = List.of("WHITE");
		accounts.removeAll(UserAccountRepository.unmatchedGenericAccounts(accounts, single));
		UserAccount whiteAnnouncer = accounts.stream()
		        .filter(a -> a.getUsername().equals("announcer-WHITE")).findFirst().orElseThrow();
		whiteAnnouncer.setPasswordHash(PasswordHasher.hash("white password"));
		assertTrue(UserAccountRepository.missingRoleAccounts(accounts, single).isEmpty());
		assertEquals("back to one platform, the suffix is dropped", "announcer", whiteAnnouncer.getUsername());
		assertTrue(PasswordHasher.verify("white password", whiteAnnouncer.getPasswordHash()));
		assertTrue(accounts.stream().filter(UserAccount::isGeneric).noneMatch(a -> a.getUsername().contains("-")));
	}

	private static long platformSets(List<UserAccount> accounts) {
		return accounts.stream().filter(UserAccount::isGeneric)
		        .filter(a -> a.getGrants().size() == 1 && a.getGrants().get(0).getRole() == Role.ANNOUNCER)
		        .count();
	}

	@Test
	public void createsOnlyGlobalRolesWhenThereAreNoPlatforms() {
		List<UserAccount> accounts = UserAccountRepository.missingRoleAccounts(List.of(), List.of());
		assertEquals(7, accounts.size());
		assertTrue(accounts.stream().allMatch(a -> a.getGrants().get(0).getPlatformName() == null));
	}

	@Test
	public void doesNotAlterAnExistingUsernameWithDifferentGrants() {
		UserAccount custom = new UserAccount();
		custom.setUsername("results");
		custom.getGrants().add(new RoleGrant(Role.WEIGHIN, null));
		List<UserAccount> added = UserAccountRepository.missingRoleAccounts(List.of(custom), List.of());
		assertFalse(added.stream().anyMatch(a -> a.getUsername().equals("results")));
		assertEquals(List.of(new RoleGrant(Role.WEIGHIN, null)), custom.getGrants());
	}

	@Test
	public void passwordRequiredFlagClearsWhenAPasswordIsAssigned() {
		UserAccount account = new UserAccount();
		assertTrue(account.isPasswordChangeRequired());
		account.setPasswordHash(PasswordHasher.hash("a long password"));
		assertFalse(account.isPasswordChangeRequired());
		account.setPasswordHash(null);
		assertTrue(account.isPasswordChangeRequired());
	}

	@Test
	public void matchesExistingAccountsIgnoringCaseWithoutRenamingThem() {
		List<UserAccount> existing = UserAccountRepository.missingRoleAccounts(List.of(), List.of("MixedCase"));
		existing.forEach(a -> a.setUsername(a.getUsername().toUpperCase(Locale.ROOT)));
		assertTrue(UserAccountRepository.missingRoleAccounts(existing, List.of("MixedCase")).isEmpty());
		assertTrue(existing.stream().anyMatch(a -> a.getUsername().equals("ANNOUNCER")));
	}

	@Test
	public void singlePlatformAccountsHaveNoSuffixButKeepTheirPlatformScope() {
		List<UserAccount> accounts = UserAccountRepository.missingRoleAccounts(List.of(), List.of("Main"));
		assertEquals(15, accounts.size());
		assertTrue(accounts.stream().noneMatch(a -> a.getUsername().contains("-")));
		UserAccount announcer = accounts.stream().filter(a -> a.getUsername().equals("announcer"))
		        .findFirst().orElseThrow();
		assertEquals(List.of(new RoleGrant(Role.ANNOUNCER, "Main")), announcer.getGrants());
	}

	private static UserAccount account(String username, RoleGrant grant) {
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.getGrants().add(grant);
		return account;
	}
}
