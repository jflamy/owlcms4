package app.owlcms.audit;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/** Serializes file I/O off the UI thread; background export callers wait for actual audit publication. */
public final class SealedAuditBackend implements AutoCloseable {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(SealedAuditBackend.class);
	private final Object lifecycle = new Object();
	private final AuditFileStore store;
	private final int capacity;
	private final Semaphore retainedBytes;
	private final AtomicReference<IOException> failure = new AtomicReference<>();
	private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "AuditFileIO");
		thread.setDaemon(true);
		return thread;
	});
	private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
		Thread thread = new Thread(task, "AuditIdleTimer");
		thread.setDaemon(true);
		return thread;
	});
	private CompletableFuture<Void> closing;

	@FunctionalInterface
	private interface Operation {
		void run() throws IOException;
	}

	public SealedAuditBackend(Path directory, AuditSigningKey key, AuditSealLimits limits,
			Clock clock, LongSupplier nanos, String runId, String version,
			BiConsumer<String, String> readable, boolean scheduleTimer) throws IOException {
		this.store = new AuditFileStore(directory, key, limits, clock, nanos, runId, version, readable);
		this.capacity = limits.queueBytes();
		this.retainedBytes = new Semaphore(this.capacity, true);
		if (scheduleTimer) {
			this.timer.scheduleWithFixedDelay(() -> {
				try {
					tick();
				} catch (IOException e) {
					synchronized (this.lifecycle) {
						if (this.closing == null) {
							fail(e);
						}
					}
				}
			}, 1, 1, TimeUnit.SECONDS);
		}
	}

	public void write(String platform, List<AuditRecordSnapshot> records, boolean boundary, boolean wait)
			throws IOException {
		int weight = 256;
		try {
			for (AuditRecordSnapshot record : records) {
				weight = Math.addExact(weight, record.retainedBytes());
			}
		} catch (ArithmeticException e) {
			throw fail(new IOException("Audit record batch is too large", e));
		}
		List<AuditRecordSnapshot> captured = List.copyOf(records);
		CompletableFuture<Void> published = submit(weight, wait,
				() -> this.store.append(platform, captured, boundary));
		if (wait) {
			await(published);
		}
	}

	public void closeBlock(String platform, boolean wait) throws IOException {
		CompletableFuture<Void> sealed = submit(256, wait, () -> this.store.closeBlock(platform));
		if (wait) {
			await(sealed);
		}
	}

	public void tick() throws IOException {
		await(submit(256, true, this.store::tick));
	}

	/** Forces every stream into a new file so the previous files are finalized; returns the files now open. */
	public Set<Path> rollover() throws IOException {
		AtomicReference<Set<Path>> open = new AtomicReference<>();
		await(submit(256, true, () -> open.set(this.store.rollAll())));
		return open.get();
	}

	public void drain() throws IOException {
		await(submit(256, true, this.store::awaitSeals));
	}

	public boolean healthy() {
		return this.failure.get() == null;
	}

	public void abort() throws IOException {
		finish(false);
	}

	@Override
	public void close() throws IOException {
		finish(true);
	}

	private CompletableFuture<Void> submit(int weight, boolean wait, Operation operation) throws IOException {
		checkFailure();
		if (weight > this.capacity) {
			throw fail(new IOException("Audit record batch exceeds the I/O queue bound"));
		}
		if (wait) {
			try {
				this.retainedBytes.acquire(weight);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw fail(new IOException("Interrupted while awaiting audit I/O capacity", e));
			}
		} else if (!this.retainedBytes.tryAcquire(weight)) {
			throw fail(new IOException("Audit I/O queue is full; integrity is degraded"));
		}
		synchronized (this.lifecycle) {
			if (this.closing != null) {
				this.retainedBytes.release(weight);
				throw new IOException("Audit backend is closing");
			}
			CompletableFuture<Void> result = new CompletableFuture<>();
			try {
				this.io.execute(() -> {
					try {
						checkFailure();
						operation.run();
						result.complete(null);
					} catch (IOException | RuntimeException e) {
						result.completeExceptionally(fail(e instanceof IOException error
								? error : new IOException("Audit persistence failed", e)));
					} catch (Error e) {
						result.completeExceptionally(fail(new IOException("Audit I/O worker failed", e)));
						throw e;
					} finally {
						this.retainedBytes.release(weight);
					}
				});
			} catch (RuntimeException e) {
				this.retainedBytes.release(weight);
				throw fail(new IOException("Audit I/O worker cannot accept records", e));
			}
			return result;
		}
	}

	private void finish(boolean finalize) throws IOException {
		CompletableFuture<Void> finished;
		synchronized (this.lifecycle) {
			if (this.closing == null) {
				this.timer.shutdown();
				this.closing = new CompletableFuture<>();
				this.io.execute(() -> {
					try {
						if (finalize && healthy()) {
							this.store.close();
						} else {
							this.store.abort();
						}
						checkFailure();
						this.closing.complete(null);
					} catch (IOException | RuntimeException e) {
						this.closing.completeExceptionally(fail(e instanceof IOException error
								? error : new IOException("Audit shutdown failed", e)));
					} catch (Error e) {
						this.closing.completeExceptionally(fail(new IOException("Audit shutdown worker failed", e)));
						throw e;
					}
				});
				this.io.shutdown();
			}
			finished = this.closing;
		}
		await(finished);
	}

	private IOException fail(IOException error) {
		if (this.failure.compareAndSet(null, error)) {
			logger.error("Audit integrity is degraded; records must not be treated as completely sealed", error);
			this.timer.shutdown();
		}
		return error;
	}

	private void checkFailure() throws IOException {
		IOException error = this.failure.get();
		if (error != null) {
			throw error;
		}
	}

	private static void await(CompletableFuture<Void> future) throws IOException {
		try {
			future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while awaiting audit publication", e);
		} catch (ExecutionException e) {
			throw new IOException("Audit persistence did not complete", e.getCause());
		}
	}
}
