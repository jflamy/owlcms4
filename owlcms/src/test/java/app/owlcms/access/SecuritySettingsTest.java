package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.access.SecuritySettings.Outcome;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

public class SecuritySettingsTest {

	private static final AuditOrigin ORIGIN = AuditOrigin.system("test");

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
	public void pinModeWithoutAdminPassword() {
		clearProperties();
		UserAccountRepository.ensureBuiltInAdmin(ORIGIN);
		setAdminPassword(null);
		Config config = Config.getCurrent();
		config.setIwfCompliance(false);
		config.chooseAccessMode(AccessMode.PIN);
		Config.setCurrent(config, ORIGIN);
	}

	@After
	public void clearProperties() {
		System.clearProperty("accessMode");
		System.clearProperty("iwfCompliance");
	}

	private static void setAdminPassword(String password) {
		UserAccount admin = UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN).copy();
		admin.setPasswordHash(password == null ? null : PasswordHasher.hash(password));
		admin.setMustChangePassword(false);
		UserAccountRepository.save(admin, ORIGIN);
	}

	@Test
	public void accountsModeNeedsAnAdminPassword() {
		assertEquals(Outcome.ADMIN_PASSWORD_REQUIRED, SecuritySettings.switchAccessMode(AccessMode.ACCOUNTS, ORIGIN));
		setAdminPassword("an admin password");
		assertEquals(Outcome.CHANGED, SecuritySettings.switchAccessMode(AccessMode.ACCOUNTS, ORIGIN));
		assertTrue(Config.getCurrent().isAccountsMode());
		assertEquals(Outcome.UNCHANGED, SecuritySettings.switchAccessMode(AccessMode.ACCOUNTS, ORIGIN));
		assertEquals(Outcome.CHANGED, SecuritySettings.switchAccessMode(AccessMode.PIN, ORIGIN));
		assertFalse(Config.getCurrent().isAccountsMode());
	}

	@Test
	public void theIwfCheckEnforcesNothing() {
		assertEquals(Outcome.CHANGED, SecuritySettings.setIwfComplianceCheck(true, ORIGIN));
		assertTrue(Config.getCurrent().isIwfComplianceChecked());
		assertFalse("the access mode is unchanged", Config.getCurrent().isAccountsMode());
		assertEquals(Outcome.UNCHANGED, SecuritySettings.setIwfComplianceCheck(true, ORIGIN));

		setAdminPassword("an admin password");
		assertEquals(Outcome.CHANGED, SecuritySettings.switchAccessMode(AccessMode.ACCOUNTS, ORIGIN));
		assertEquals("PIN mode stays available", Outcome.CHANGED,
		        SecuritySettings.switchAccessMode(AccessMode.PIN, ORIGIN));

		assertEquals(Outcome.CHANGED, SecuritySettings.setIwfComplianceCheck(false, ORIGIN));
		assertFalse(Config.getCurrent().isIwfComplianceChecked());
	}

	@Test
	public void startupParametersCannotBeOverriddenFromThePage() {
		System.setProperty("accessMode", "PIN");
		assertEquals(Outcome.SET_AT_STARTUP, SecuritySettings.switchAccessMode(AccessMode.ACCOUNTS, ORIGIN));
		System.setProperty("iwfCompliance", "false");
		assertEquals(Outcome.SET_AT_STARTUP, SecuritySettings.setIwfComplianceCheck(true, ORIGIN));
	}
}
