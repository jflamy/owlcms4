package app.owlcms.audit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * One audit segment. Callers serialize operations separately from publication, so waiting for a signer
 * never holds the output lock needed by that signer.
 */
public final class SealedAuditStream implements AutoCloseable {
	private final Object operationLock = new Object();
	private final Object outputLock = new Object();
	private final OutputStream output;
	private final AuditSigningKey key;
	private final AuditSigningQueue signing;
	private final AuditSealLimits limits;
	private final Clock clock;
	private final LongSupplier nanos;
	private final AuditMarker.Identity identity;
	private final ByteArrayOutputStream active = new ByteArrayOutputStream();
	private final AtomicReference<IOException> failure = new AtomicReference<>();
	private CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);
	private long sequence;
	private long firstLine;
	private int records;
	private long oldestNanos;
	private long lastRecordNanos;
	private String lastSignedHash;
	private boolean closed;

	public SealedAuditStream(OutputStream output, AuditSigningKey key, AuditSigningQueue signing,
			AuditSealLimits limits, Clock clock, LongSupplier nanos, String stream, String runId,
			String artifactName, String applicationVersion, byte[] header,
			String previousFile, String previousBlockHash) throws IOException {
		this.output = output;
		this.key = key;
		this.signing = signing;
		this.limits = limits;
		this.clock = clock;
		this.nanos = nanos;
		this.identity = new AuditMarker.Identity(stream, runId, artifactName, clock.instant(),
				applicationVersion, key.fingerprint());
		byte[] capturedHeader = header.clone();
		requireRecord(capturedHeader);
		this.sequence = 1;
		AuditMarker.Block block = AuditMarker.Block.of(1, 1, capturedHeader);
		AuditMarker marker;
		if (previousFile == null && previousBlockHash == null) {
			marker = new AuditMarker.First(this.identity, block);
		} else if (previousFile != null && previousBlockHash != null) {
			marker = new AuditMarker.Continue(this.identity, block, previousFile, previousBlockHash);
		} else {
			throw new IOException("An audit continuation requires both the previous filename and block hash");
		}
		synchronized (this.outputLock) {
			writeBytes(capturedHeader);
			writeMarker(marker);
			this.lastSignedHash = block.sha256();
		}
	}

	public long nextSequence() {
		synchronized (this.operationLock) {
			return this.sequence + 1;
		}
	}

	public void append(byte[] record) throws IOException {
		synchronized (this.operationLock) {
			checkWritable();
			if (record.length > this.limits.blockBytes()) {
				throw new IOException("Audit record exceeds the maximum block size");
			}
			byte[] captured = record.clone();
			requireRecord(captured);
			if (this.active.size() > 0 && this.active.size() + captured.length > this.limits.blockBytes()) {
				freeze();
			}
			long now = this.nanos.getAsLong();
			if (this.active.size() == 0) {
				this.firstLine = this.sequence + 1;
				this.oldestNanos = now;
			}
			synchronized (this.outputLock) {
				writeBytes(captured);
			}
			this.active.writeBytes(captured);
			this.sequence++;
			this.records++;
			this.lastRecordNanos = now;
			if (this.active.size() >= this.limits.blockBytes() || this.records >= this.limits.blockRecords()) {
				freeze();
			}
		}
	}

	public void closeBlock() throws IOException {
		synchronized (this.operationLock) {
			checkWritable();
			freeze();
		}
	}

	public void tick() throws IOException {
		synchronized (this.operationLock) {
			checkWritable();
			if (this.active.size() == 0) {
				return;
			}
			long now = this.nanos.getAsLong();
			if (now - this.oldestNanos >= this.limits.blockAge().toNanos()
					|| now - this.lastRecordNanos >= this.limits.idleTime().toNanos()) {
				freeze();
			}
		}
	}

	public void awaitSeals() throws IOException {
		synchronized (this.operationLock) {
			checkFailure();
			awaitPending();
		}
	}

	public String lastBlockHash() throws IOException {
		synchronized (this.operationLock) {
			awaitPending();
			synchronized (this.outputLock) {
				return this.lastSignedHash;
			}
		}
	}

	/** Close a damaged/interrupted segment without claiming completion or sealing its active tail. */
	public void abort() throws IOException {
		synchronized (this.operationLock) {
			if (!this.closed) {
				try {
					awaitPending();
				} finally {
					this.closed = true;
					synchronized (this.outputLock) {
						this.output.close();
					}
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (this.operationLock) {
			if (this.closed) {
				return;
			}
			try {
				checkWritable();
				freeze();
				awaitPending();
				synchronized (this.outputLock) {
					writeMarker(new AuditMarker.Final(currentIdentity(), this.sequence, this.lastSignedHash));
				}
			} finally {
				this.closed = true;
				synchronized (this.outputLock) {
					this.output.close();
				}
			}
		}
	}

	private void freeze() throws IOException {
		if (this.active.size() == 0) {
			return;
		}
		byte[] bytes = this.active.toByteArray();
		long start = this.firstLine;
		long end = this.sequence;
		CompletableFuture<Void> submitted = this.signing.submit(bytes.length, () -> {
			try {
				AuditMarker.Block block = AuditMarker.Block.of(start, end, bytes);
				synchronized (this.outputLock) {
					checkFailure();
					writeMarker(new AuditMarker.Seal(currentIdentity(), block, this.lastSignedHash));
					this.lastSignedHash = block.sha256();
				}
			} catch (IOException | RuntimeException e) {
				IOException failed = e instanceof IOException io ? io : new IOException("Audit sealing failed", e);
				this.failure.compareAndSet(null, failed);
				throw failed;
			} catch (Error e) {
				this.failure.compareAndSet(null, new IOException("Audit signing worker failed", e));
				throw e;
			}
		});
		this.pending = submitted;
		this.active.reset();
		this.records = 0;
	}

	private AuditMarker.Identity currentIdentity() {
		return new AuditMarker.Identity(this.identity.stream(), this.identity.runId(), this.identity.artifactName(),
				this.clock.instant(), this.identity.applicationVersion(), this.identity.keyFingerprint());
	}

	private void writeMarker(AuditMarker marker) throws IOException {
		try {
			writeBytes((AuditMarkerCodec.render(AuditMarkerCodec.sign(marker, this.key)) + "\n")
					.getBytes(StandardCharsets.UTF_8));
			this.output.flush();
		} catch (GeneralSecurityException e) {
			IOException failed = new IOException("Unable to sign audit marker", e);
			this.failure.compareAndSet(null, failed);
			throw failed;
		}
	}

	private void writeBytes(byte[] bytes) throws IOException {
		try {
			this.output.write(bytes);
			this.output.flush();
		} catch (IOException e) {
			this.failure.compareAndSet(null, e);
			throw e;
		}
	}

	private void awaitPending() throws IOException {
		try {
			this.pending.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for audit seals", e);
		} catch (ExecutionException e) {
			throw new IOException("Audit block signing or publication failed", e.getCause());
		}
		checkFailure();
	}

	private void checkWritable() throws IOException {
		checkFailure();
		if (this.closed) {
			throw new IOException("Audit segment is closed");
		}
	}

	private void checkFailure() throws IOException {
		IOException failed = this.failure.get();
		if (failed != null) {
			throw failed;
		}
	}

	private static void requireRecord(byte[] bytes) throws IOException {
		int terminators = 0;
		for (byte value : bytes) {
			if (value == '\n') {
				terminators++;
			}
		}
		if (bytes.length == 0 || bytes[bytes.length - 1] != '\n' || terminators != 1) {
			throw new IOException("An audit record must be exactly one LF-terminated line");
		}
	}
}
