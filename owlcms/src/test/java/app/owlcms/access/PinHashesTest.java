package app.owlcms.access;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

public class PinHashesTest {

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

	@Test
	public void pinsAreHashedLikeAccountPasswords() {
		String stored = PinHashes.hash("4321");
		assertTrue(PasswordHasher.isHash(stored));
		assertNotEquals("4321", stored);
		assertTrue(PinHashes.matches("4321", stored));
		assertTrue("a repeated check is answered from memory", PinHashes.matches("4321", stored));
		assertFalse(PinHashes.matches("1234", stored));
		assertFalse(PinHashes.matches(null, stored));
		assertFalse(PinHashes.matches("4321", null));
		assertFalse(PinHashes.isOutdated(stored));
	}

	@Test
	public void anOlderShaPinIsAcceptedAndMarkedForUpgrade() {
		String legacy = AccessUtils.encodePin("2468", null, false);
		assertFalse(PasswordHasher.isHash(legacy));
		assertTrue(PinHashes.matches("2468", legacy));
		assertFalse(PinHashes.matches("1357", legacy));
		assertTrue(PinHashes.isOutdated(legacy));
	}

	@Test
	public void anUnreadableValueNeverMatches() {
		assertFalse(PinHashes.matches("2468", "not a hash"));
	}
}
