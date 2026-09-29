package app.owlcms.audit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public final class AuditContext {
	private static final ThreadLocal<Deque<Entry>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

	private AuditContext() {
	}

	public static void run(AuditActor actor, String cause, Runnable action) {
		push(new Entry(actor, cause, false), action);
	}

	public static <T> T call(AuditActor actor, String cause, Supplier<T> action) {
		Deque<Entry> entries = CURRENT.get();
		entries.push(new Entry(actor, cause, false));
		try {
			return action.get();
		} finally {
			entries.pop();
			if (entries.isEmpty()) {
				CURRENT.remove();
			}
		}
	}

	public static void suppressed(Runnable action) {
		push(new Entry(null, null, true), action);
	}

	public static <T> T suppressed(Supplier<T> action) {
		return push(new Entry(null, null, true), action);
	}

	public static <E extends Exception> void suppressedChecked(ThrowingRunnable<E> action) throws E {
		Deque<Entry> entries = CURRENT.get();
		entries.push(new Entry(null, null, true));
		try {
			action.run();
		} finally {
			entries.pop();
			if (entries.isEmpty()) {
				CURRENT.remove();
			}
		}
	}

	public static AuditActor actor() {
		Entry entry = CURRENT.get().peek();
		return entry == null ? null : entry.actor();
	}

	public static String cause() {
		Entry entry = CURRENT.get().peek();
		return entry == null ? null : entry.cause();
	}

	public static boolean isSuppressed() {
		return CURRENT.get().stream().anyMatch(Entry::suppressed);
	}

	private static void push(Entry entry, Runnable action) {
		Deque<Entry> entries = CURRENT.get();
		entries.push(entry);
		try {
			action.run();
		} finally {
			entries.pop();
			if (entries.isEmpty()) {
				CURRENT.remove();
			}
		}
	}

	private static <T> T push(Entry entry, Supplier<T> action) {
		Deque<Entry> entries = CURRENT.get();
		entries.push(entry);
		try {
			return action.get();
		} finally {
			entries.pop();
			if (entries.isEmpty()) {
				CURRENT.remove();
			}
		}
	}

	@FunctionalInterface
	public interface ThrowingRunnable<E extends Exception> {
		void run() throws E;
	}

	private record Entry(AuditActor actor, String cause, boolean suppressed) {
	}
}