package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

public class ControlPanelChallengeTest {

	private static final String TOKEN = "panel-token-123";
	private static final String REMOTE = "10.0.0.5";
	private static final String LOOPBACK = "127.0.0.1";
	private static final Function<String, String> NO_HEADERS = name -> null;
	private final AtomicLong now = new AtomicLong(1_000_000L);
	private LoginThrottle throttle;

	@Before
	public void setUp() {
		this.throttle = new LoginThrottle(this.now::get);
	}

	private ControlPanelChallenge.Result check(String presented, String remoteAddr, Function<String, String> header,
	        String expected, boolean iwf) {
		return ControlPanelChallenge.check(presented, remoteAddr, header, "test", expected, iwf, this.throttle);
	}

	@Test
	public void theTokenIsAcceptedFromAnyAddress() {
		assertTrue(check(TOKEN, REMOTE, NO_HEADERS, TOKEN, true).accepted());
		assertTrue(check(" " + TOKEN + " ", REMOTE, NO_HEADERS, TOKEN, false).accepted());
	}

	@Test
	public void whenOwlcmsHasATokenALocalConnectionIsNotEnough() {
		assertFalse(check("", LOOPBACK, NO_HEADERS, TOKEN, false).accepted());
		assertFalse(check("wrong", LOOPBACK, NO_HEADERS, TOKEN, false).accepted());
	}

	@Test
	public void withoutATokenOnlyADirectLocalConnectionPasses() {
		assertTrue(check("", LOOPBACK, NO_HEADERS, null, false).accepted());
		assertFalse("a proxy adds a forwarding header",
		        check("", LOOPBACK, Map.of("X-Forwarded-For", "203.0.113.9")::get, null, false).accepted());
		assertFalse(check("", REMOTE, NO_HEADERS, null, false).accepted());
		assertFalse("IWF compliance has no local fallback", check("", LOOPBACK, NO_HEADERS, null, true).accepted());
	}

	@Test
	public void repeatedFailuresAreThrottled() {
		for (int i = 0; i < 5; i++) {
			check("guess " + i, REMOTE, NO_HEADERS, TOKEN, false);
		}
		ControlPanelChallenge.Result result = check(TOKEN, REMOTE, NO_HEADERS, TOKEN, false);
		assertFalse(result.accepted());
		assertEquals(30, result.blockedSeconds());
	}
}
