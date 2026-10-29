package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.owlcms.access.AdminRecovery.Outcome;

public class AdminRecoveryTest {

	private static final String TOKEN = "Ab3-xY9_kLmN";
	private static final String PASSWORD = "correct horse";

	@Test
	public void onlyTheControlPanelStopPassesThrough() {
		assertTrue(AdminRecovery.passesThrough("/controlpanel/stop"));
		for (String path : new String[] { "", "/", "/admin", "/competition/h2", "/competition/export", "/simulation/",
		        "/mqtt", "/VAADIN/build/x.js", "/controlpanel/stop/", "/controlpanel" }) {
			assertFalse(path, AdminRecovery.passesThrough(path));
		}
	}

	@Test
	public void aCorrectTokenAndConfirmedPasswordIsAccepted() {
		assertEquals(Outcome.ACCEPTED, AdminRecovery.check(TOKEN, TOKEN, PASSWORD, PASSWORD, 0));
		assertEquals("surrounding spaces on the token are ignored", Outcome.ACCEPTED,
		        AdminRecovery.check(TOKEN, " " + TOKEN + " ", PASSWORD, PASSWORD, 0));
	}

	@Test
	public void withoutAConfiguredTokenNothingIsAccepted() {
		assertEquals(Outcome.NO_TOKEN_CONFIGURED, AdminRecovery.check(null, TOKEN, PASSWORD, PASSWORD, 0));
		assertEquals(Outcome.NO_TOKEN_CONFIGURED, AdminRecovery.check(" ", " ", PASSWORD, PASSWORD, 0));
	}

	@Test
	public void throttlingComesBeforeTheTokenCheck() {
		assertEquals(Outcome.THROTTLED, AdminRecovery.check(TOKEN, TOKEN, PASSWORD, PASSWORD, 5));
		assertEquals(Outcome.THROTTLED, AdminRecovery.check(TOKEN, "wrong", PASSWORD, PASSWORD, 5));
	}

	@Test
	public void theTokenIsCheckedBeforeThePassword() {
		assertEquals(Outcome.INVALID_TOKEN, AdminRecovery.check(TOKEN, "wrong", PASSWORD, PASSWORD, 0));
		assertEquals(Outcome.INVALID_TOKEN, AdminRecovery.check(TOKEN, null, "short", "other", 0));
		assertEquals(Outcome.INVALID_TOKEN, AdminRecovery.check(TOKEN, TOKEN.toLowerCase(), PASSWORD, PASSWORD, 0));
	}

	@Test
	public void thePasswordMustBeAcceptableAndConfirmed() {
		assertEquals(Outcome.UNACCEPTABLE, AdminRecovery.check(TOKEN, TOKEN, "short", "short", 0));
		assertEquals(Outcome.UNACCEPTABLE, AdminRecovery.check(TOKEN, TOKEN, null, null, 0));
		assertEquals(Outcome.MISMATCH, AdminRecovery.check(TOKEN, TOKEN, PASSWORD, PASSWORD + "x", 0));
		assertEquals(Outcome.MISMATCH, AdminRecovery.check(TOKEN, TOKEN, PASSWORD, null, 0));
	}
}
