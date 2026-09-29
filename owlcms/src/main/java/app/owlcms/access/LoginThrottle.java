package app.owlcms.access;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/** In-memory brute-force brake, keyed separately by user name and by client IP. */
public class LoginThrottle {

	static final int FREE_FAILURES = 5;
	static final long BASE_BLOCK_MS = 30_000L;
	static final long MAX_BLOCK_MS = 15 * 60_000L;
	private static final long FORGET_AFTER_MS = 60 * 60_000L;
	private static final int PRUNE_THRESHOLD = 1000;

	private static final LoginThrottle SHARED = new LoginThrottle(System::currentTimeMillis);

	private record Entry(int failures, long blockedUntil, long lastFailure) {
	}

	private final LongSupplier clock;
	private final Map<String, Entry> entries = new HashMap<>();

	public LoginThrottle(LongSupplier clock) {
		this.clock = clock;
	}

	public static LoginThrottle shared() {
		return SHARED;
	}

	/** @return seconds remaining before another attempt is accepted, 0 when allowed */
	public synchronized long blockedSeconds(String username, String clientIp) {
		long now = this.clock.getAsLong();
		long until = Math.max(blockedUntil(userKey(username)), blockedUntil(ipKey(clientIp)));
		return until > now ? (until - now + 999) / 1000 : 0;
	}

	public synchronized void fail(String username, String clientIp) {
		long now = this.clock.getAsLong();
		prune(now);
		recordFailure(userKey(username), now);
		recordFailure(ipKey(clientIp), now);
	}

	public synchronized void success(String username, String clientIp) {
		this.entries.remove(userKey(username));
		this.entries.remove(ipKey(clientIp));
	}

	private long blockedUntil(String key) {
		Entry entry = this.entries.get(key);
		return entry == null ? 0 : entry.blockedUntil();
	}

	private void recordFailure(String key, long now) {
		Entry previous = this.entries.get(key);
		int failures = previous == null ? 1 : previous.failures() + 1;
		long blockedUntil = 0;
		if (failures >= FREE_FAILURES) {
			long block = BASE_BLOCK_MS;
			for (int i = FREE_FAILURES; i < failures && block < MAX_BLOCK_MS; i++) {
				block *= 2;
			}
			blockedUntil = now + Math.min(block, MAX_BLOCK_MS);
		}
		this.entries.put(key, new Entry(failures, blockedUntil, now));
	}

	private void prune(long now) {
		if (this.entries.size() > PRUNE_THRESHOLD) {
			this.entries.values().removeIf(e -> now - e.lastFailure() > FORGET_AFTER_MS);
		}
	}

	private static String userKey(String username) {
		return "u:" + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
	}

	private static String ipKey(String clientIp) {
		return "i:" + (clientIp == null ? "" : clientIp);
	}
}
