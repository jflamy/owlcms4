package app.owlcms.access;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class LoginThrottleTest {

	private final AtomicLong now = new AtomicLong(1_000_000L);
	private final LoginThrottle throttle = new LoginThrottle(this.now::get);

	private void failTimes(String user, String ip, int n) {
		for (int i = 0; i < n; i++) {
			this.throttle.fail(user, ip);
		}
	}

	@Test
	public void fourFailuresDoNotBlock() {
		failTimes("bob", "1.1.1.1", 4);
		assertEquals(0, this.throttle.blockedSeconds("bob", "1.1.1.1"));
	}

	@Test
	public void fifthFailureBlocksThirtySecondsThenExpires() {
		failTimes("bob", "1.1.1.1", 5);
		assertEquals(30, this.throttle.blockedSeconds("bob", "1.1.1.1"));
		this.now.addAndGet(29_000);
		assertEquals(1, this.throttle.blockedSeconds("bob", "1.1.1.1"));
		this.now.addAndGet(1_000);
		assertEquals(0, this.throttle.blockedSeconds("bob", "1.1.1.1"));
	}

	@Test
	public void blockDoublesOnEachFurtherFailureUpToTheCap() {
		failTimes("bob", "1.1.1.1", 6);
		assertEquals(60, this.throttle.blockedSeconds("bob", "1.1.1.1"));
		failTimes("bob", "1.1.1.1", 1);
		assertEquals(120, this.throttle.blockedSeconds("bob", "1.1.1.1"));
		failTimes("bob", "1.1.1.1", 20);
		assertEquals(900, this.throttle.blockedSeconds("bob", "1.1.1.1"));
	}

	@Test
	public void successResetsBothKeys() {
		failTimes("bob", "1.1.1.1", 5);
		this.throttle.success("bob", "1.1.1.1");
		assertEquals(0, this.throttle.blockedSeconds("bob", "1.1.1.1"));
		failTimes("bob", "1.1.1.1", 4);
		assertEquals(0, this.throttle.blockedSeconds("bob", "1.1.1.1"));
	}

	@Test
	public void userAndIpAreTrackedSeparately() {
		failTimes("bob", "1.1.1.1", 5);
		assertEquals(30, this.throttle.blockedSeconds("bob", "2.2.2.2"));
		assertEquals(30, this.throttle.blockedSeconds("alice", "1.1.1.1"));
		assertEquals(0, this.throttle.blockedSeconds("alice", "2.2.2.2"));
	}

	@Test
	public void usernameIsCaseInsensitive() {
		failTimes("Bob", "1.1.1.1", 5);
		assertEquals(30, this.throttle.blockedSeconds("bOb", "9.9.9.9"));
	}
}
