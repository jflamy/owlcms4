package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.access.SecuritySettings.Outcome;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.access.RememberedLoginRepository;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.TestStartup;

public class SecuritySettingsTest {

	private static final AuditOrigin ORIGIN = AuditOrigin.system("test");

	@BeforeClass
	public static void setUpDatabase() {
		TestStartup.init();
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
		RememberedLoginRepository.deleteAll();
		UserAccountRepository.ensureBuiltInAdmin(ORIGIN);
		setAdminPassword(null);
		Config config = Config.getCurrent();
		config.setIwfCompliance(false);
		config.setFeatureSwitchValue(FeatureSwitch.NO_V1_EXPORT, false);
		config.setFeatureSwitchValue(FeatureSwitch.V2_EXPORT, false);
		config.setAccessMode(AccessMode.PIN);
		config.setRememberAccountLogins(true);
		Config.setCurrentTesting(config, ORIGIN);
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
		RememberedLogins.Issued remembered = RememberedLogins.issue(RememberedBrowser.Kind.OFFICIALS, null, null,
		        "stored:pin-hash", Instant.now(), RememberedAccount.zone(Config.getCurrent()), "127.0.0.1", "test");
		assertEquals(Outcome.CHANGED, SecuritySettings.switchAccessMode(AccessMode.ACCOUNTS, ORIGIN));
		assertNull(RememberedLoginRepository.find(remembered.login().getSelector()));
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
	public void rememberedAccountLoginsAreOnByDefaultAndCanBeWithdrawn() {
		Config config = new Config();
		assertTrue(config.allowsRememberedLogins());
		config.setRememberAccountLogins(null);
		assertTrue("older exports and databases behave as enabled", config.allowsRememberedLogins());

		assertEquals(Outcome.UNCHANGED,
		        SecuritySettings.setRememberAccountLogins(true, ORIGIN));
		assertEquals(Outcome.CHANGED,
		        SecuritySettings.setRememberAccountLogins(false, ORIGIN));
		assertFalse(Config.getCurrent().allowsRememberedLogins());
		assertEquals(Outcome.UNCHANGED,
		        SecuritySettings.setRememberAccountLogins(false, ORIGIN));
		assertEquals(Outcome.CHANGED,
		        SecuritySettings.setRememberAccountLogins(true, ORIGIN));
		assertTrue(Config.getCurrent().allowsRememberedLogins());
	}

	@Test
	public void theNoV1ExportSwitchWithdrawsUnsignedV1Exports() {
		Principal admin = Principal.of(UserAccountRepository.findByUsername(UserAccount.BUILT_IN_ADMIN), null);
		assertTrue(AccessPolicy.hasPermission(admin, Permission.EXPORT_V1, null));

		assertEquals(Outcome.CHANGED, SecuritySettings.setIwfComplianceCheck(true, ORIGIN));
		assertTrue("the IWF check only reports", AccessPolicy.hasPermission(admin, Permission.EXPORT_V1, null));

		setFeatureSwitch(FeatureSwitch.NO_V1_EXPORT, true);
		assertFalse("V1 exports are never signed", AccessPolicy.hasPermission(admin, Permission.EXPORT_V1, null));
		assertTrue(AccessPolicy.hasPermission(admin, Permission.EXPORT_DATA, null));
		assertTrue("V2 replaces V1", FeatureSwitch.V2_EXPORT.isEffective());

		setFeatureSwitch(FeatureSwitch.NO_V1_EXPORT, false);
		assertTrue(AccessPolicy.hasPermission(admin, Permission.EXPORT_V1, null));
		assertFalse(FeatureSwitch.V2_EXPORT.isEffective());
	}

	private static void setFeatureSwitch(FeatureSwitch featureSwitch, boolean enabled) {
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(featureSwitch, enabled);
		Config.setCurrentTesting(config, ORIGIN);
	}
}
