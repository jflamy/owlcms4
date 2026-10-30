package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

public class ControlPanelChallengeTest {

	private static final String TOKEN = "panel-token-123";
	private static final String REMOTE = "10.0.0.5";
	private static final String LOOPBACK = "127.0.0.1";
	private final AtomicLong now = new AtomicLong(1_000_000L);
	private LoginThrottle throttle;

	@Before
	public void setUp() {
		this.throttle = new LoginThrottle(this.now::get);
	}

	private ControlPanelChallenge.Result check(String presented, String remoteAddr, String expected) {
		return ControlPanelChallenge.check(presented, remoteAddr, "test", expected, this.throttle);
	}

	@Test
	public void theTokenIsAcceptedFromAnyAddress() {
		assertTrue(check(TOKEN, REMOTE, TOKEN).accepted());
		assertTrue(check(" " + TOKEN + " ", REMOTE, TOKEN).accepted());
	}

	@Test
	public void aLocalConnectionIsNotEnough() {
		assertFalse(check("", LOOPBACK, TOKEN).accepted());
		assertFalse(check("wrong", LOOPBACK, TOKEN).accepted());
	}

	@Test
	public void withoutATokenOwlcmsRefusesWithoutThrottling() {
		for (int i = 0; i < 10; i++) {
			ControlPanelChallenge.Result result = check("", LOOPBACK, null);
			assertFalse(result.accepted());
			assertTrue(result.noToken());
			assertEquals(0, result.blockedSeconds());
		}
	}

	@Test
	public void repeatedFailuresAreThrottled() {
		for (int i = 0; i < 5; i++) {
			check("guess " + i, REMOTE, TOKEN);
		}
		ControlPanelChallenge.Result result = check(TOKEN, REMOTE, TOKEN);
		assertFalse(result.accepted());
		assertEquals(30, result.blockedSeconds());
	}
}
