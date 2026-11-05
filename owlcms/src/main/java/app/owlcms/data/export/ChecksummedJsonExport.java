/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditSigningKey;
import app.owlcms.audit.ExportAudit;
import app.owlcms.audit.ExportSignature;
import app.owlcms.spreadsheet.InputStreamWrapper;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Streams a JSON export through a pipe while computing the SHA-256 and byte length of the exact bytes written.
 * <p>
 * The body is serialized on a writer thread into a {@link DigestOutputStream} that feeds the pipe, so the digest is
 * computed on the fly without buffering or re-serializing. A writer failure is surfaced to the reader through
 * {@link InputStreamWrapper} instead of looking like a normal end of stream.
 * <p>
 * With a signing key, the closing {@code }} of the body is held back (but digested) and replaced by the signature
 * trailer, so the export is still produced in a single pass.
 */
public final class ChecksummedJsonExport {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ChecksummedJsonExport.class);

	/**
	 * @param sha256               checksum of the body as serialized (the whole file when unsigned)
	 * @param signatureFingerprint fingerprint of the signing key, null when unsigned
	 */
	public record Checksum(long bytes, String sha256, String signatureFingerprint) {
		public Checksum(long bytes, String sha256) {
			this(bytes, sha256, null);
		}
	}

	@FunctionalInterface
	public interface Body {
		void writeTo(OutputStream out) throws Exception;
	}

	public interface Listener {
		void succeeded(Checksum checksum);

		void failed(Throwable error);
	}

	@FunctionalInterface
	interface TrailerFactory {
		byte[] trailer(String bodySha256) throws GeneralSecurityException;
	}

	/** Pipe read side; {@link #checksum()} blocks until the writer has finished. */
	public static final class ChecksummedInputStream extends InputStreamWrapper {
		private final CompletableFuture<Checksum> checksum;

		private ChecksummedInputStream(InputStream delegate, AtomicReference<IOException> writerException,
				CompletableFuture<Checksum> checksum) {
			super(delegate, writerException);
			this.checksum = checksum;
		}

		public Checksum checksum() throws IOException {
			try {
				return this.checksum.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				throw cause instanceof IOException io ? io : new IOException(cause);
			}
		}
	}

	private ChecksummedJsonExport() {
	}

	/** Streams the body and writes an {@code export.json} audit event when the writer completes. */
	public static ChecksummedInputStream audited(int format, String channel, AuditActor actor, Body body)
			throws IOException {
		return audited(format, channel, actor, null, body);
	}

	/** As above, signing the export with the key when it is not null. */
	public static ChecksummedInputStream audited(int format, String channel, AuditActor actor,
			AuditSigningKey signingKey, Body body) throws IOException {
		return stream(body, new Listener() {
			@Override
			public void succeeded(Checksum checksum) {
				ExportAudit.success(actor, format, channel, checksum.bytes(), checksum.sha256(),
						checksum.signatureFingerprint());
			}

			@Override
			public void failed(Throwable error) {
				ExportAudit.failed(actor, format, channel, error);
			}
		}, signingKey);
	}

	public static ChecksummedInputStream stream(Body body, Listener listener) throws IOException {
		return stream(body, listener, (AuditSigningKey) null);
	}

	/** Signs the export with the key when it is not null. */
	public static ChecksummedInputStream stream(Body body, Listener listener, AuditSigningKey signingKey)
			throws IOException {
		TrailerFactory trailers = signingKey == null ? null : sha -> ExportSignature.trailer(sha, signingKey);
		String fingerprint = signingKey == null ? null : signingKey.fingerprint();
		return stream(body, listener, trailers, fingerprint);
	}

	static ChecksummedInputStream stream(Body body, Listener listener, TrailerFactory trailers, String fingerprint)
			throws IOException {
		PipedOutputStream pipeOut = new PipedOutputStream();
		PipedInputStream pipeIn = new PipedInputStream(pipeOut);
		AtomicReference<IOException> writerException = new AtomicReference<>();
		CompletableFuture<Checksum> future = new CompletableFuture<>();
		MessageDigest digest = sha256();
		CountingOutputStream counter = new CountingOutputStream(pipeOut);
		HoldLastByteOutputStream hold = trailers == null ? null : new HoldLastByteOutputStream(counter);
		DigestOutputStream digestOut = new DigestOutputStream(hold != null ? hold : counter, digest);

		Thread writer = new Thread(() -> {
			try {
				OutputStream bodyOut = new NonClosingOutputStream(digestOut);
				body.writeTo(bodyOut);
				bodyOut.flush();
				String bodySha256 = HexFormat.of().formatHex(digest.digest());
				if (hold != null) {
					if (!hold.holdsClosingBrace()) {
						throw new IOException("export body does not end with }");
					}
					// the trailer replaces the held }, and is not part of the digest
					counter.write(trailers.trailer(bodySha256));
				}
				Checksum checksum = new Checksum(counter.count, bodySha256, fingerprint);
				listener.succeeded(checksum);
				future.complete(checksum);
				digestOut.close();
			} catch (Throwable e) {
				// record before closing the pipe so the reader sees the failure rather than EOF
				IOException failure = e instanceof IOException io ? io : new IOException(e);
				writerException.set(failure);
				future.completeExceptionally(failure);
				try {
					digestOut.close();
				} catch (IOException ignored) {
				}
				LoggerUtils.logError(logger, e);
				listener.failed(e);
			}
		}, "json-export");
		writer.setDaemon(true);
		writer.start();
		return new ChecksummedInputStream(pipeIn, writerException, future);
	}

	private static final class NonClosingOutputStream extends FilterOutputStream {

		NonClosingOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void close() throws IOException {
			flush();
		}
	}

	/** Forwards every byte except the most recent one, which is held and discarded on close. */
	private static final class HoldLastByteOutputStream extends FilterOutputStream {
		private int held = -1;

		HoldLastByteOutputStream(OutputStream out) {
			super(out);
		}

		boolean holdsClosingBrace() {
			return this.held == '}';
		}

		@Override
		public void write(int b) throws IOException {
			if (this.held >= 0) {
				this.out.write(this.held);
			}
			this.held = b & 0xFF;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (len <= 0) {
				return;
			}
			if (this.held >= 0) {
				this.out.write(this.held);
			}
			this.out.write(b, off, len - 1);
			this.held = b[off + len - 1] & 0xFF;
		}
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final class CountingOutputStream extends FilterOutputStream {
		private long count;

		CountingOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			this.out.write(b);
			this.count++;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.out.write(b, off, len);
			this.count += len;
		}
	}
}
