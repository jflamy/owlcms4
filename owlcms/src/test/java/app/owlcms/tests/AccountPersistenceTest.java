package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.access.AccessMode;
import app.owlcms.access.PasswordHasher;
import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

public class AccountPersistenceTest {

	@BeforeClass
	public static void setUpDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	private static UserAccount newAccount(String username, RoleGrant... grants) {
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.setGrants(new java.util.ArrayList<>(List.of(grants)));
		return UserAccountRepository.save(account);
	}

	@Test
	public void builtInAdminIsSeededOnceWithoutPassword() {
		UserAccountRepository.ensureBuiltInAdmin();
		UserAccountRepository.ensureBuiltInAdmin();

		UserAccount admin = UserAccountRepository.findByUsername("ADMIN");
		assertNotNull(admin);
		assertTrue(admin.isBuiltInAdmin());
		assertTrue(admin.isEnabled());
		assertEquals(List.of(new RoleGrant(Role.ADMIN, null)), admin.getGrants());
		assertEquals(1, UserAccountRepository.findAll().stream().filter(UserAccount::isBuiltInAdmin).count());

		UserAccount withPassword = admin.copy();
		if (!withPassword.hasPassword()) {
			assertFalse(UserAccountRepository.adminHasPassword());
			withPassword.setPasswordHash(PasswordHasher.hash("a long password"));
			UserAccountRepository.save(withPassword);
		}
		assertTrue(UserAccountRepository.adminHasPassword());
	}

	@Test
	public void repairsAnAdminThatWasDisabledOrDemoted() {
		UserAccountRepository.ensureBuiltInAdmin();
		UserAccount damaged = UserAccountRepository.findByUsername("admin").copy();
		damaged.setEnabled(false);
		damaged.getGrants().clear();
		UserAccountRepository.save(damaged);

		UserAccountRepository.ensureBuiltInAdmin();

		UserAccount admin = UserAccountRepository.findByUsername("admin");
		assertTrue(admin.isEnabled());
		assertEquals(List.of(new RoleGrant(Role.ADMIN, null)), admin.getGrants());
	}

	@Test
	public void grantsSurviveARoundTripAndLookupIgnoresCase() {
		newAccount("Carol", new RoleGrant(Role.PLATFORM, "A"), new RoleGrant(Role.RESULTS, null));
		UserAccountRepository.invalidate();

		UserAccount found = UserAccountRepository.findByUsername("  CAROL ");
		assertNotNull(found);
		assertEquals("carol", found.getUsername());
		assertEquals(List.of(new RoleGrant(Role.PLATFORM, "A"), new RoleGrant(Role.RESULTS, null)), found.getGrants());
		assertNull(UserAccountRepository.findByUsername("nobody"));
	}

	@Test
	public void editingACopyDoesNotChangeTheCachedAccount() {
		newAccount("dave", new RoleGrant(Role.JURY, "A"));

		UserAccount copy = UserAccountRepository.findAll().stream().filter(a -> a.getUsername().equals("dave"))
		        .findFirst().orElseThrow();
		copy.getGrants().clear();
		copy.setEnabled(false);

		UserAccount cached = UserAccountRepository.findByUsername("dave");
		assertTrue(cached.isEnabled());
		assertEquals(1, cached.getGrants().size());
	}

	@Test
	public void renamingAPlatformFollowsTheGrants() {
		newAccount("erin", new RoleGrant(Role.ANNOUNCER, "Old"), new RoleGrant(Role.RESULTS, null));

		UserAccountRepository.renamePlatform("Old", "New");

		assertEquals(List.of(new RoleGrant(Role.ANNOUNCER, "New"), new RoleGrant(Role.RESULTS, null)),
		        UserAccountRepository.findByUsername("erin").getGrants());
	}

	@Test
	public void deletingAPlatformRemovesOnlyGrantsNamingIt() {
		newAccount("frank", new RoleGrant(Role.TC, "Gone"), new RoleGrant(Role.TIMEKEEPER, "Kept"),
		        new RoleGrant(Role.RESULTS, null));

		UserAccountRepository.removePlatform("Gone");

		assertEquals(List.of(new RoleGrant(Role.TIMEKEEPER, "Kept"), new RoleGrant(Role.RESULTS, null)),
		        UserAccountRepository.findByUsername("frank").getGrants());
	}

	@Test
	public void deletedAccountsDisappear() {
		UserAccount account = newAccount("gina", new RoleGrant(Role.WEIGHIN, null));
		UserAccountRepository.delete(account);
		assertNull(UserAccountRepository.findByUsername("gina"));
	}

	@Test
	public void importedConfigKeepsTheRunningAccessMode() {
		Config current = Config.getCurrent();
		current.setAccessMode(AccessMode.ACCOUNTS);
		Config.setCurrent(current);
		try {
			Config imported = new Config();
			Config saved = Config.setCurrent(imported);

			assertEquals(AccessMode.ACCOUNTS, saved.getAccessMode());
			assertEquals(AccessMode.ACCOUNTS, Config.getCurrent().getAccessMode());
		} finally {
			Config restored = Config.getCurrent();
			restored.setAccessMode(AccessMode.PIN);
			Config.setCurrent(restored);
		}
		assertEquals(AccessMode.PIN, Config.getCurrent().getAccessMode());
	}
}
