package app.owlcms.access;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinModeAuthenticatorTest {

	@Test
	public void environmentPinTakesPrecedenceOverStoredPin() {
		assertTrue(PinModeAuthenticator.pinExpected("1234", null));
		assertTrue(PinModeAuthenticator.pinExpected("1234", "stored"));
		assertFalse(PinModeAuthenticator.pinExpected("", "stored"));
	}

	@Test
	public void storedPinAppliesWithoutEnvironmentOverride() {
		assertTrue(PinModeAuthenticator.pinExpected(null, "stored"));
		assertFalse(PinModeAuthenticator.pinExpected(null, null));
		assertFalse(PinModeAuthenticator.pinExpected(null, ""));
	}
}