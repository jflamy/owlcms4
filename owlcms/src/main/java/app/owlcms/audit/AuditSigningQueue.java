package app.owlcms.audit;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/** One ordered signing worker with a byte-weighted bound shared by all platform streams. */
public final class AuditSigningQueue implements AutoCloseable {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditSigningQueue.class);
	private final int capacity;
	private final Semaphore bytes;
	private final AtomicInteger pendingBytes = new AtomicInteger();
	private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "AuditSigning");
		thread.setDaemon(true);
		return thread;
	});

	@FunctionalInterface
	public interface Task {
		void run() throws Exception;
	}

	public AuditSigningQueue(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("Audit signing queue capacity must be positive");
		}
		this.capacity = capacity;
		this.bytes = new Semaphore(capacity, true);
	}

	public CompletableFuture<Void> submit(int weight, Task task) throws IOException {
		if (weight < 1 || weight > this.capacity) {
			throw new IOException("Audit block exceeds the signing queue capacity");
		}
		try {
			this.bytes.acquire(weight);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while awaiting audit signing capacity", e);
		}
		this.pendingBytes.addAndGet(weight);
		CompletableFuture<Void> completed = new CompletableFuture<>();
		try {
			this.worker.execute(() -> {
				try {
					task.run();
					completed.complete(null);
				} catch (Exception e) {
					logger.error("Audit block signing or publication failed", e);
					completed.completeExceptionally(e);
				} catch (Error e) {
					completed.completeExceptionally(e);
					logger.error("Audit signing worker failed", e);
					throw e;
				} finally {
					this.pendingBytes.addAndGet(-weight);
					this.bytes.release(weight);
				}
			});
		} catch (RuntimeException e) {
			this.pendingBytes.addAndGet(-weight);
			this.bytes.release(weight);
			throw new IOException("Audit signing worker cannot accept the block", e);
		}
		return completed;
	}

	public int pendingBytes() {
		return this.pendingBytes.get();
	}

	@Override
	public void close() throws IOException {
		this.worker.shutdown();
		try {
			if (!this.worker.awaitTermination(30, TimeUnit.SECONDS)) {
				throw new IOException("Audit signing worker did not drain before shutdown");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while draining audit signing worker", e);
		}
	}
}
