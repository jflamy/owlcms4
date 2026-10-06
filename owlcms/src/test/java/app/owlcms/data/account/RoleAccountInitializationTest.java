package app.owlcms.data.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		assertEquals(22, accounts.size());
		for (Role role : Role.values()) {
			if (!role.isGrantable()) {
				assertFalse(accounts.stream().anyMatch(a -> a.getGrants().stream()
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
		assertTrue(accounts.stream().anyMatch(a -> a.getUsername().equals("announcer-A")));
		assertTrue(accounts.stream().anyMatch(a -> a.getUsername().equals("platform-B")));
	}

	@Test
	public void repeatedInitializationPreservesExistingAccountsAndAddsNewPlatforms() {
		List<UserAccount> existing = UserAccountRepository.missingRoleAccounts(List.of(), List.of("A"));
		UserAccount announcer = existing.stream().filter(a -> a.getUsername().equals("announcer"))
		        .findFirst().orElseThrow();
		announcer.setEnabled(false);
		announcer.setPasswordHash(PasswordHasher.hash("existing password"));
		assertTrue(UserAccountRepository.missingRoleAccounts(existing, List.of("A")).isEmpty());
		List<UserAccount> added = UserAccountRepository.missingRoleAccounts(existing, List.of("A", "B"));
		assertEquals(16, added.size());
		assertTrue(added.stream().allMatch(a -> a.getUsername().endsWith("-A") || a.getUsername().endsWith("-B")));
		assertFalse(announcer.isEnabled());
		assertFalse(announcer.isPasswordChangeRequired());
		assertTrue(PasswordHasher.verify("existing password", announcer.getPasswordHash()));
		assertTrue(UserAccountRepository.missingRoleAccounts(
		        Stream.concat(existing.stream(), added.stream()).toList(), List.of("A", "B")).isEmpty());
	}

	@Test
	public void createsOnlyGlobalRolesWhenThereAreNoPlatforms() {
		List<UserAccount> accounts = UserAccountRepository.missingRoleAccounts(List.of(), List.of());
		assertEquals(6, accounts.size());
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
		assertEquals(14, accounts.size());
		assertTrue(accounts.stream().noneMatch(a -> a.getUsername().contains("-")));
		UserAccount announcer = accounts.stream().filter(a -> a.getUsername().equals("announcer"))
		        .findFirst().orElseThrow();
		assertEquals(List.of(new RoleGrant(Role.ANNOUNCER, "Main")), announcer.getGrants());
	}

	@Test
	public void resetRecognizesBothSingleAndMultiPlatformConventionalAccounts() {
		UserAccount single = account("ANNOUNCER", new RoleGrant(Role.ANNOUNCER, "Main"));
		UserAccount multiple = account("announcer-Main", new RoleGrant(Role.ANNOUNCER, "Main"));
		assertTrue(UserAccountRepository.isConventionalPlatformAccount(single));
		assertTrue(UserAccountRepository.isConventionalPlatformAccount(multiple));
		assertFalse(UserAccountRepository.isConventionalPlatformAccount(
		        account("speaker", new RoleGrant(Role.ANNOUNCER, "Main"))));
		assertFalse(UserAccountRepository.isConventionalPlatformAccount(
		        account("announcer-Main", new RoleGrant(Role.ANNOUNCER, null))));
		assertFalse(UserAccountRepository.isConventionalPlatformAccount(
		        account("results", new RoleGrant(Role.RESULTS, null))));
		assertFalse(UserAccountRepository.isConventionalPlatformAccount(
		        account("admin", new RoleGrant(Role.ADMIN, null))));
		UserAccount custom = account("announcer", new RoleGrant(Role.ANNOUNCER, "Main"));
		custom.getGrants().add(new RoleGrant(Role.RESULTS, null));
		assertFalse(UserAccountRepository.isConventionalPlatformAccount(custom));
	}

	@Test
	public void resetRegeneratesNamesInBothDirectionsAndPreservesCustomAndGlobalAccounts() {
		List<UserAccount> singlePlatform = UserAccountRepository.missingRoleAccounts(List.of(), List.of("A"));
		UserAccount custom = account("custom-speaker", new RoleGrant(Role.ANNOUNCER, "A"));
		List<UserAccount> retained = Stream.concat(singlePlatform.stream(), Stream.of(custom))
		        .filter(a -> !UserAccountRepository.isConventionalPlatformAccount(a)).toList();
		assertEquals(7, retained.size());
		List<UserAccount> multiplePlatforms = UserAccountRepository.missingRoleAccounts(retained, List.of("A", "B"));
		assertEquals(16, multiplePlatforms.size());
		assertTrue(multiplePlatforms.stream().noneMatch(a -> a.getUsername().equals("announcer")));
		assertTrue(multiplePlatforms.stream().anyMatch(a -> a.getUsername().equals("announcer-A")));
		assertTrue(multiplePlatforms.stream().anyMatch(a -> a.getUsername().equals("announcer-B")));
		assertTrue(retained.contains(custom));
		List<UserAccount> all = Stream.concat(retained.stream(), multiplePlatforms.stream()).toList();
		List<UserAccount> backToSingle = all.stream()
		        .filter(a -> !UserAccountRepository.isConventionalPlatformAccount(a)).toList();
		List<UserAccount> recreated = UserAccountRepository.missingRoleAccounts(backToSingle, List.of("A"));
		assertEquals(8, recreated.size());
		assertTrue(recreated.stream().allMatch(a -> !a.getUsername().contains("-")));
		assertTrue(recreated.stream().allMatch(UserAccount::isPasswordChangeRequired));
	}

	private static UserAccount account(String username, RoleGrant grant) {
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.getGrants().add(grant);
		return account;
	}
}
