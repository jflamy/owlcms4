package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.owlcms.access.RememberedBrowser.Kind;

public class RememberedBrowserTest {

	private static final String SECRET = RememberedBrowser.newSecret();
	private static final String PIN = "stored:pbkdf2-sha256$600000$abc$def";
	private static final long NOW = 1_800_000_000L;
	private static final long LATER = NOW + 3600;

	@Test
	public void aCookieIsValidForItsPinSecretAndKindUntilItExpires() {
		String cookie = RememberedBrowser.cookieValue(Kind.OFFICIALS, PIN, SECRET, LATER);
		assertTrue(RememberedBrowser.isValid(cookie, Kind.OFFICIALS, PIN, SECRET, NOW));
		assertFalse("expired", RememberedBrowser.isValid(cookie, Kind.OFFICIALS, PIN, SECRET, LATER));
		assertFalse("another PIN", RememberedBrowser.isValid(cookie, Kind.OFFICIALS, "stored:other", SECRET, NOW));
		assertFalse("browsers forgotten", RememberedBrowser.isValid(cookie, Kind.OFFICIALS, PIN,
		        RememberedBrowser.newSecret(), NOW));
		assertFalse("an officials cookie is not a display cookie",
		        RememberedBrowser.isValid(cookie, Kind.DISPLAY, PIN, SECRET, NOW));
	}

	@Test
	public void tamperedOrMalformedCookiesAreRefused() {
		String cookie = RememberedBrowser.cookieValue(Kind.DISPLAY, PIN, SECRET, LATER);
		String extended = cookie.replace("." + LATER + ".", "." + (LATER + 86400) + ".");
		assertFalse("the expiry is covered by the MAC", RememberedBrowser.isValid(extended, Kind.DISPLAY, PIN,
		        SECRET, NOW));
		assertFalse(RememberedBrowser.isValid("v1.abc.def", Kind.DISPLAY, PIN, SECRET, NOW));
		assertFalse(RememberedBrowser.isValid("garbage", Kind.DISPLAY, PIN, SECRET, NOW));
		assertFalse(RememberedBrowser.isValid(null, Kind.DISPLAY, PIN, SECRET, NOW));
		assertFalse("no PIN, nothing to remember", RememberedBrowser.isValid(cookie, Kind.DISPLAY, null, SECRET, NOW));
		assertFalse("no secret yet", RememberedBrowser.isValid(cookie, Kind.DISPLAY, PIN, null, NOW));
	}

	@Test
	public void aTicketIsRedeemedOnceWithinItsLifetime() {
		long now = System.currentTimeMillis();
		String ticket = RememberedBrowser.issueTicket(Kind.OFFICIALS, now);
		assertEquals(Kind.OFFICIALS, RememberedBrowser.redeemTicket(ticket, now));
		assertNull("used", RememberedBrowser.redeemTicket(ticket, now));

		String late = RememberedBrowser.issueTicket(Kind.DISPLAY, now);
		assertNull("expired", RememberedBrowser.redeemTicket(late,
		        now + RememberedBrowser.TICKET_LIFETIME.toMillis() + 1));
		assertNull(RememberedBrowser.redeemTicket("unknown", now));
		assertNull(RememberedBrowser.redeemTicket(null, now));
	}
}
