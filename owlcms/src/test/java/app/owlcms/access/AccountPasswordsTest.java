package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.Main;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

public class AccountPasswordsTest {

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

	@Before
	public void adminWithoutPassword() {
		UserAccountRepository.ensureBuiltInAdmin(AuditOrigin.system("test"));
		UserAccount admin = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN).copy();
		admin.setPasswordHash(null);
		admin.setMustChangePassword(false);
		UserAccountRepository.save(admin, AuditOrigin.system("test"));
	}

	@Test
	public void temporaryAdminPasswordOnlyWhenAccountsModeIsRequired() {
		assertNull(AccountPasswords.generateIfRequired(false, AuditOrigin.system("test")));
		assertFalse(UserAccountRepository.adminHasPassword());
		assertFalse(AccountPasswords.isTemporaryAdminPasswordPending());

		String password = AccountPasswords.generateIfRequired(true, AuditOrigin.system("test"));
		assertNotNull(password);
		assertEquals(16, password.length());
		UserAccount admin = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN);
		assertTrue(PasswordHasher.verify(password, admin.getPasswordHash()));
		assertTrue(admin.isMustChangePassword());
		assertTrue(AccountPasswords.isTemporaryAdminPasswordPending());

		// a restart before the change does not replace the password the operator may already have read
		assertNull(AccountPasswords.generateIfRequired(true, AuditOrigin.system("test")));
		assertTrue(PasswordHasher.verify(password,
		        UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN).getPasswordHash()));
		assertFalse("only the start that generated it shows the notice",
		        AccountPasswords.isTemporaryAdminPasswordPending());
	}

	@Test
	public void forcedChangeIsClearedBySettingANewPassword() {
		String temporary = AccountPasswords.generateIfRequired(true, AuditOrigin.system("test"));
		Principal admin = Principal.of(UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN), null);
		assertTrue(AccountPasswords.mustChangePassword(admin));

		assertEquals(AccountPasswords.ChangeResult.WRONG_CURRENT_PASSWORD,
		        AccountPasswords.changeOwnPassword(admin, "wrong password", "a new long password", AuditOrigin.system("test")));
		assertEquals(AccountPasswords.ChangeResult.UNACCEPTABLE,
		        AccountPasswords.changeOwnPassword(admin, temporary, "short", AuditOrigin.system("test")));
		assertEquals(AccountPasswords.ChangeResult.SAME_AS_CURRENT,
		        AccountPasswords.changeOwnPassword(admin, temporary, temporary, AuditOrigin.system("test")));
		assertTrue(AccountPasswords.mustChangePassword(admin));

		assertEquals(AccountPasswords.ChangeResult.CHANGED,
		        AccountPasswords.changeOwnPassword(admin, temporary, "a new long password", AuditOrigin.system("test")));
		UserAccount changed = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN);
		assertTrue(PasswordHasher.verify("a new long password", changed.getPasswordHash()));
		assertFalse(changed.isMustChangePassword());
		assertFalse(AccountPasswords.mustChangePassword(admin));
		assertFalse(AccountPasswords.isTemporaryAdminPasswordPending());
	}

	@Test
	public void pinSessionsHaveNoOwnPassword() {
		assertEquals(AccountPasswords.ChangeResult.NO_ACCOUNT, AccountPasswords.changeOwnPassword(
		        Principal.admin(Principal.AuthSource.OFFICIALS_PIN), "x", "a new long password", AuditOrigin.system("test")));
		assertFalse(AccountPasswords.mustChangePassword(Principal.admin(Principal.AuthSource.BACKDOOR)));
	}

	@Test
	public void clearingSparesTheBuiltInAdminTheActingAccountAndTheOtherGroup() {
		UserAccount admin = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN).copy();
		admin.setPasswordHash(PasswordHasher.hash("admin password"));
		UserAccountRepository.save(admin, AuditOrigin.system("test"));
		UserAccount acting = account("clear-acting", "acting password", false, new RoleGrant(Role.ADMIN, null));
		account("clear-other", "other password", true, new RoleGrant(Role.RESULTS, null));
		account("clear-none", null, false, new RoleGrant(Role.WEIGHIN, null));
		UserAccount generic = account("clear-generic", "generic password", false, new RoleGrant(Role.TC, "A"));
		generic.setGeneric(true);
		UserAccountRepository.save(generic, AuditOrigin.system("test"));

		int cleared = AccountPasswords.clearPasswords(false, acting.getId(), acting.getUsername(), AuditOrigin.system("test"));

		assertTrue(cleared >= 1);
		assertTrue(UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN).hasPassword());
		assertTrue(UserAccountRepository.findByUsername("clear-acting").hasPassword());
		UserAccount clearedOther = UserAccountRepository.findByUsername("clear-other");
		assertFalse(clearedOther.hasPassword());
		assertFalse(clearedOther.isMustChangePassword());
		assertFalse(UserAccountRepository.findByUsername("clear-none").hasPassword());
		assertTrue("generic accounts are cleared separately",
		        UserAccountRepository.findByUsername("clear-generic").hasPassword());

		AccountPasswords.clearPasswords(true, acting.getId(), acting.getUsername(), AuditOrigin.system("test"));
		assertFalse(UserAccountRepository.findByUsername("clear-generic").hasPassword());
		assertTrue("the built-in admin is generic but never cleared",
		        UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN).hasPassword());
	}

	@Test
	public void mustChangeFlagSurvivesPersistenceAndCopy() {
		UserAccount flagged = account("flagged-user", "flagged password", true, new RoleGrant(Role.JURY, "A"));
		UserAccountRepository.invalidate();
		assertTrue(UserAccountRepository.findByUsername("flagged-user").isMustChangePassword());
		assertTrue(UserAccountRepository.findByUsername("flagged-user").copy().isMustChangePassword());
		assertNotNull(flagged);
	}

	private static UserAccount account(String username, String password, boolean mustChange, RoleGrant... grants) {
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.setGrants(new ArrayList<>(List.of(grants)));
		if (password != null) {
			account.setPasswordHash(PasswordHasher.hash(password));
		}
		account.setMustChangePassword(mustChange);
		return UserAccountRepository.save(account, AuditOrigin.system("test"));
	}
}
