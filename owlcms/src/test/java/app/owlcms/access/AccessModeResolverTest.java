package app.owlcms.access;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AccessModeResolverTest {

	private static AccessMode resolve(String override, AccessMode stored, Boolean pinChosen) {
		return AccessModeResolver.resolve(override, stored, pinChosen);
	}

	@Test
	public void validOverrideWinsOverEverything() {
		assertEquals(AccessMode.PIN, resolve("pin", AccessMode.ACCOUNTS, false));
		assertEquals(AccessMode.PIN, resolve(" PIN ", AccessMode.ACCOUNTS, null));
		assertEquals(AccessMode.ACCOUNTS, resolve("accounts", AccessMode.PIN, true));
		assertEquals(AccessMode.ACCOUNTS, resolve("Accounts", null, true));
	}

	@Test
	public void invalidOrBlankOverrideIsIgnored() {
		assertEquals(AccessMode.PIN, resolve("bogus", null, null));
		assertEquals(AccessMode.ACCOUNTS, resolve("bogus", AccessMode.ACCOUNTS, null));
		assertEquals(AccessMode.PIN, resolve("", AccessMode.PIN, null));
		assertEquals(AccessMode.PIN, resolve("  ", null, null));
	}

	@Test
	public void explicitPinChoiceWins() {
		for (AccessMode stored : new AccessMode[] { null, AccessMode.PIN, AccessMode.ACCOUNTS }) {
			assertEquals(AccessMode.PIN, resolve(null, stored, true));
		}
	}

	@Test
	public void otherwiseTheStoredModeAppliesAndDefaultsToPin() {
		assertEquals(AccessMode.PIN, resolve(null, null, null));
		assertEquals(AccessMode.PIN, resolve(null, null, false));
		assertEquals(AccessMode.PIN, resolve(null, AccessMode.PIN, false));
		assertEquals(AccessMode.ACCOUNTS, resolve(null, AccessMode.ACCOUNTS, null));
		assertEquals(AccessMode.ACCOUNTS, resolve(null, AccessMode.ACCOUNTS, false));
	}
}
