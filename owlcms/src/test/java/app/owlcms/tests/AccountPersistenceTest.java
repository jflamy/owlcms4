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
import app.owlcms.access.AccessStartup;
import app.owlcms.access.PasswordHasher;
import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;

public class AccountPersistenceTest {

	@BeforeClass
	public static void setUpDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		AccessStartup.roleAccountInitialization().join();
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
		assertEquals("Carol", found.getUsername());
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

	@Test(expected = IllegalArgumentException.class)
	public void usernamesDifferingOnlyInCaseAreRejected() {
		newAccount("CaseSensitiveDisplay", new RoleGrant(Role.WEIGHIN, null));
		newAccount("casesensitivedisplay", new RoleGrant(Role.RESULTS, null));
	}

	@Test
	public void importedConfigKeepsTheRunningAccessMode() {
		Config current = Config.getCurrent();
		current.setAccessMode(AccessMode.ACCOUNTS);
		Config.setCurrent(current);
		AccessStartup.roleAccountInitialization().join();
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

	@Test
	public void enablingAccountsPersistsMissingRolesWithPasswordsRequired() {
		JPAService.runInTransaction(em -> {
			em.persist(new Platform("Seed A"));
			em.persist(new Platform("Seed B"));
			return null;
		});
		Config config = Config.getCurrent();
		config.setAccessMode(AccessMode.PIN);
		Config.setCurrent(config);
		try {
			config = Config.getCurrent();
			config.setAccessMode(AccessMode.ACCOUNTS);
			Config.setCurrent(config);
			AccessStartup.roleAccountInitialization().join();
			UserAccountRepository.invalidate();
			UserAccount results = UserAccountRepository.findByUsername("results");
			assertNotNull(results);
			assertEquals(List.of(new RoleGrant(Role.RESULTS, null)), results.getGrants());
			UserAccount announcer = UserAccountRepository.findByUsername("announcer-seed a");
			assertNotNull(announcer);
			assertEquals("announcer-Seed A", announcer.getUsername());
			assertTrue(announcer.isPasswordChangeRequired());
			assertEquals(List.of(new RoleGrant(Role.ANNOUNCER, "Seed A")), announcer.getGrants());
			UserAccount referee = UserAccountRepository.findByUsername("referee-seed b");
			assertNotNull(referee);
			assertTrue(referee.isPasswordChangeRequired());
			assertEquals(List.of(new RoleGrant(Role.REFEREE, "Seed B")), referee.getGrants());
		} finally {
			Config restored = Config.getCurrent();
			restored.setAccessMode(AccessMode.PIN);
			Config.setCurrent(restored);
		}
	}

	@Test
	public void resetPlatformAccountsReplacesConventionalAccountsAndPreservesOthers() {
		JPAService.runInTransaction(em -> {
			em.persist(new Platform("Reset A"));
			em.persist(new Platform("Reset B"));
			return null;
		});
		UserAccount single = newAccount("announcer", new RoleGrant(Role.ANNOUNCER, "Reset A"));
		UserAccount suffixed = newAccount("announcer-Reset A", new RoleGrant(Role.ANNOUNCER, "Reset A"));
		suffixed.setPasswordHash(PasswordHasher.hash("old platform password"));
		UserAccountRepository.save(suffixed);
		UserAccount custom = newAccount("custom-reset-speaker", new RoleGrant(Role.ANNOUNCER, "Reset A"));
		custom.setPasswordHash(PasswordHasher.hash("custom password"));
		UserAccountRepository.save(custom);
		UserAccount global = newAccount("global-reset-results", new RoleGrant(Role.RESULTS, null));
		global.setPasswordHash(PasswordHasher.hash("global password"));
		UserAccountRepository.save(global);
		UserAccountRepository.ensureBuiltInAdmin();
		UserAccount admin = UserAccountRepository.findByUsername("admin");

		List<UserAccount> reset = AccessStartup.resetPlatformAccountsInBackground().join();
		UserAccountRepository.invalidate();

		assertNull(UserAccountRepository.findById(single.getId()));
		assertNull(UserAccountRepository.findById(suffixed.getId()));
		assertNull(UserAccountRepository.findByUsername("announcer"));
		UserAccount recreated = UserAccountRepository.findByUsername("announcer-reset a");
		assertNotNull(recreated);
		assertTrue(recreated.isPasswordChangeRequired());
		assertEquals(List.of(new RoleGrant(Role.ANNOUNCER, "Reset A")), recreated.getGrants());
		assertNotNull(UserAccountRepository.findByUsername("announcer-reset b"));
		assertEquals(custom.getId(), UserAccountRepository.findByUsername("custom-reset-speaker").getId());
		assertTrue(PasswordHasher.verify("custom password",
		        UserAccountRepository.findByUsername("custom-reset-speaker").getPasswordHash()));
		assertEquals(global.getId(), UserAccountRepository.findByUsername("global-reset-results").getId());
		assertTrue(PasswordHasher.verify("global password",
		        UserAccountRepository.findByUsername("global-reset-results").getPasswordHash()));
		assertEquals(admin.getId(), UserAccountRepository.findByUsername("admin").getId());
		assertEquals(admin.getPasswordHash(), UserAccountRepository.findByUsername("admin").getPasswordHash());
		assertEquals(reset.size(), UserAccountRepository.findAll().size());
	}
}
