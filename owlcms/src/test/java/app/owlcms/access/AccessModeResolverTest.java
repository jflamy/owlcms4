package app.owlcms.access;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AccessModeResolverTest {

	private static AccessMode resolve(String override, AccessMode stored, Boolean pinChosen, boolean pem,
	        boolean adminPassword) {
		return AccessModeResolver.resolve(override, stored, pinChosen, pem, adminPassword);
	}

	@Test
	public void validOverrideWinsOverEverything() {
		assertEquals(AccessMode.PIN, resolve("pin", AccessMode.ACCOUNTS, false, true, true));
		assertEquals(AccessMode.PIN, resolve(" PIN ", AccessMode.ACCOUNTS, null, true, true));
		assertEquals(AccessMode.ACCOUNTS, resolve("accounts", AccessMode.PIN, true, false, false));
		assertEquals(AccessMode.ACCOUNTS, resolve("Accounts", null, true, true, true));
	}

	@Test
	public void invalidOrBlankOverrideIsIgnored() {
		assertEquals(AccessMode.PIN, resolve("bogus", null, null, false, false));
		assertEquals(AccessMode.ACCOUNTS, resolve("bogus", AccessMode.ACCOUNTS, null, false, false));
		assertEquals(AccessMode.ACCOUNTS, resolve("bogus", AccessMode.PIN, null, true, true));
		assertEquals(AccessMode.PIN, resolve("", AccessMode.PIN, null, true, false));
		assertEquals(AccessMode.PIN, resolve("  ", null, null, false, false));
	}

	@Test
	public void explicitPinChoiceWinsOverTheKey() {
		for (AccessMode stored : new AccessMode[] { null, AccessMode.PIN, AccessMode.ACCOUNTS }) {
			assertEquals(AccessMode.PIN, resolve(null, stored, true, true, true));
			assertEquals(AccessMode.PIN, resolve(null, stored, true, true, false));
			assertEquals(AccessMode.PIN, resolve(null, stored, true, false, false));
		}
	}

	@Test
	public void keyWithAdminPasswordSelectsAccounts() {
		for (AccessMode stored : new AccessMode[] { null, AccessMode.PIN, AccessMode.ACCOUNTS }) {
			assertEquals(AccessMode.ACCOUNTS, resolve(null, stored, null, true, true));
			assertEquals(AccessMode.ACCOUNTS, resolve(null, stored, false, true, true));
		}
	}

	@Test
	public void keyWithoutAdminPasswordKeepsTheStoredMode() {
		assertEquals(AccessMode.PIN, resolve(null, null, null, true, false));
		assertEquals(AccessMode.PIN, resolve(null, AccessMode.PIN, false, true, false));
		assertEquals(AccessMode.ACCOUNTS, resolve(null, AccessMode.ACCOUNTS, false, true, false));
	}

	@Test
	public void withoutKeyTheStoredModeApplies() {
		assertEquals(AccessMode.PIN, resolve(null, null, null, false, false));
		assertEquals(AccessMode.PIN, resolve(null, AccessMode.PIN, false, false, true));
		assertEquals(AccessMode.ACCOUNTS, resolve(null, AccessMode.ACCOUNTS, null, false, false));
		assertEquals(AccessMode.ACCOUNTS, resolve(null, AccessMode.ACCOUNTS, false, false, true));
		assertEquals(AccessMode.PIN, resolve(null, AccessMode.PIN, null, false, true));
	}
}
