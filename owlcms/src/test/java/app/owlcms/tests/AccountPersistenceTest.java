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

import app.owlcms.audit.AuditOrigin;
import app.owlcms.Main;
import app.owlcms.access.AccessMode;
import app.owlcms.access.AccessStartup;
import app.owlcms.access.PasswordHasher;
import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
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
		return UserAccountRepository.save(account, AuditOrigin.system("test"));
	}

	@Test
	public void builtInAdminIsSeededOnceWithoutPassword() {
		UserAccountRepository.ensureBuiltInAdmin(AuditOrigin.system("test"));
		UserAccountRepository.ensureBuiltInAdmin(AuditOrigin.system("test"));

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
			UserAccountRepository.save(withPassword, AuditOrigin.system("test"));
		}
		assertTrue(UserAccountRepository.adminHasPassword());
	}

	@Test
	public void repairsAnAdminThatWasDisabledOrDemoted() {
		UserAccountRepository.ensureBuiltInAdmin(AuditOrigin.system("test"));
		UserAccount damaged = UserAccountRepository.findByUsername("admin").copy();
		damaged.setEnabled(false);
		damaged.getGrants().clear();
		UserAccountRepository.save(damaged, AuditOrigin.system("test"));

		UserAccountRepository.ensureBuiltInAdmin(AuditOrigin.system("test"));

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

		UserAccountRepository.renamePlatform("Old", "New", AuditOrigin.system("test"));

		assertEquals(List.of(new RoleGrant(Role.ANNOUNCER, "New"), new RoleGrant(Role.RESULTS, null)),
		        UserAccountRepository.findByUsername("erin").getGrants());
	}

	@Test
	public void deletingAPlatformRemovesOnlyGrantsNamingIt() {
		newAccount("frank", new RoleGrant(Role.TC, "Gone"), new RoleGrant(Role.TIMEKEEPER, "Kept"),
		        new RoleGrant(Role.RESULTS, null));

		UserAccountRepository.removePlatform("Gone", AuditOrigin.system("test"));

		assertEquals(List.of(new RoleGrant(Role.TIMEKEEPER, "Kept"), new RoleGrant(Role.RESULTS, null)),
		        UserAccountRepository.findByUsername("frank").getGrants());
	}

	@Test
	public void deletedAccountsDisappear() {
		UserAccount account = newAccount("gina", new RoleGrant(Role.WEIGHIN, null));
		UserAccountRepository.delete(account, AuditOrigin.system("test"));
		assertNull(UserAccountRepository.findByUsername("gina"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void usernamesDifferingOnlyInCaseAreRejected() {
		newAccount("CaseSensitiveDisplay", new RoleGrant(Role.WEIGHIN, null));
		newAccount("casesensitivedisplay", new RoleGrant(Role.RESULTS, null));
	}

	@Test
	public void anImportedConfigIsTakenAsIsNotCompletedWithTheRunningValues() {
		Config current = Config.getCurrent();
		current.setAccessMode(AccessMode.ACCOUNTS);
		current.setIwfCompliance(true);
		Config.setCurrent(current);
		AccessStartup.roleAccountInitialization().join();
		try {
			Config olderExport = new Config();
			Config saved = Config.setCurrent(olderExport);

			assertEquals("a missing access mode means the default", AccessMode.PIN, saved.getAccessMode());
			assertNull(saved.getIwfCompliance());
			assertEquals(AccessMode.PIN, Config.getCurrent().getAccessMode());
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
	public void recomputeKeepsPasswordsAndRemovesAccountsOfDeletedPlatforms() {
		JPAService.runInTransaction(em -> {
			em.persist(new Platform("Recompute A"));
			return null;
		});
		AccessStartup.recomputeGenericAccountsInBackground(AuditOrigin.system("test")).join();
		UserAccount kept = UserAccountRepository.findAll().stream()
		        .filter(a -> a.isGeneric() && a.getGrants().equals(List.of(new RoleGrant(Role.ANNOUNCER, "Recompute A"))))
		        .findFirst().orElseThrow();
		kept.setPasswordHash(PasswordHasher.hash("kept password"));
		UserAccountRepository.save(kept, AuditOrigin.system("test"));
		UserAccount orphan = newAccount("announcer-Gone", new RoleGrant(Role.ANNOUNCER, "Gone"));
		orphan.setGeneric(true);
		UserAccountRepository.save(orphan, AuditOrigin.system("test"));
		UserAccount person = newAccount("speaker-gone", new RoleGrant(Role.ANNOUNCER, "Gone"));
		UserAccountRepository.ensureBuiltInAdmin(AuditOrigin.system("test"));
		UserAccount admin = UserAccountRepository.findByUsername("admin");

		List<UserAccount> recomputed = AccessStartup.recomputeGenericAccountsInBackground(AuditOrigin.system("test")).join();
		UserAccountRepository.invalidate();

		assertNull(UserAccountRepository.findById(orphan.getId()));
		assertNotNull(UserAccountRepository.findById(person.getId()));
		assertEquals(admin.getId(), UserAccountRepository.findByUsername("admin").getId());
		UserAccount afterRecompute = UserAccountRepository.findById(kept.getId());
		assertNotNull(afterRecompute);
		assertTrue(PasswordHasher.verify("kept password", afterRecompute.getPasswordHash()));
		assertEquals(recomputed.size(), UserAccountRepository.findAll().size());
	}
}
