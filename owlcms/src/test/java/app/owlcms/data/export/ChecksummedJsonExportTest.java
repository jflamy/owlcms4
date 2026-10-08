package app.owlcms.data.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import app.owlcms.data.export.ChecksummedJsonExport.Checksum;
import app.owlcms.data.export.ChecksummedJsonExport.ChecksummedInputStream;
import app.owlcms.data.export.ChecksummedJsonExport.Listener;

public class ChecksummedJsonExportTest {

	private static final class RecordingListener implements Listener {
		final AtomicReference<Checksum> success = new AtomicReference<>();
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final CountDownLatch done = new CountDownLatch(1);

		@Override
		public void succeeded(Checksum checksum) {
			this.success.set(checksum);
			this.done.countDown();
		}

		@Override
		public void failed(Throwable error) {
			this.failure.set(error);
			this.done.countDown();
		}

		void await() throws InterruptedException {
			assertTrue("writer did not complete", this.done.await(10, TimeUnit.SECONDS));
		}
	}

	@Test
	public void checksumAndLengthDescribeExactlyTheBytesRead() throws Exception {
		// larger than the pipe buffer so the writer blocks and streams in several chunks
		byte[] payload = ("{\"exportDate\":\"2026-10-07T20:00:00Z\",\"athletes\":[" + "{\"x\":1},".repeat(5000) + "{}]}")
				.getBytes(StandardCharsets.UTF_8);
		RecordingListener listener = new RecordingListener();

		byte[] read;
		Checksum checksum;
		try (ChecksummedInputStream in = ChecksummedJsonExport.stream(out -> out.write(payload), listener)) {
			read = in.readAllBytes();
			checksum = in.checksum();
		}
		listener.await();

		assertEquals(payload.length, read.length);
		assertEquals(payload.length, checksum.bytes());
		assertEquals(sha256(read), checksum.sha256());
		assertEquals(checksum, listener.success.get());
		assertNull(listener.failure.get());
	}

	@Test
	public void singleByteChangeChangesChecksum() throws Exception {
		byte[] a = "{\"total\":100}".getBytes(StandardCharsets.UTF_8);
		byte[] b = "{\"total\":101}".getBytes(StandardCharsets.UTF_8);
		assertNotEquals(exportAndChecksum(a).sha256(), exportAndChecksum(b).sha256());
	}

	@Test
	public void writerFailureReachesReaderAndIsNotReportedAsSuccess() throws Exception {
		RecordingListener listener = new RecordingListener();
		IOException boom = new IOException("serialization failed");

		try (ChecksummedInputStream in = ChecksummedJsonExport.stream(out -> {
			out.write("{\"partial\":".getBytes(StandardCharsets.UTF_8));
			throw boom;
		}, listener)) {
			try {
				in.readAllBytes();
				fail("truncated export must not read as a normal end of stream");
			} catch (IOException expected) {
				assertEquals("serialization failed", expected.getMessage());
			}
			try {
				in.checksum();
				fail("no checksum may be reported for a partial artifact");
			} catch (IOException expected) {
				assertEquals("serialization failed", expected.getMessage());
			}
		}
		listener.await();

		assertNull(listener.success.get());
		assertEquals(boom, listener.failure.get());
	}

	@Test
	public void readerDoesNotObserveEndOfStreamBeforeSuccessListenerCompletes() throws Exception {
		CountDownLatch listenerStarted = new CountDownLatch(1);
		CountDownLatch releaseListener = new CountDownLatch(1);
		Listener listener = new Listener() {
			@Override
			public void succeeded(Checksum checksum) {
				listenerStarted.countDown();
				try {
					assertTrue("listener was not released", releaseListener.await(10, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
			}

			@Override
			public void failed(Throwable error) {
				fail("unexpected writer failure: " + error);
			}
		};

		try (ChecksummedInputStream in = ChecksummedJsonExport.stream(
				out -> out.write("{}".getBytes(StandardCharsets.UTF_8)), listener)) {
			CompletableFuture<byte[]> reader = CompletableFuture.supplyAsync(() -> {
				try {
					return in.readAllBytes();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});

			assertTrue("success listener did not start", listenerStarted.await(10, TimeUnit.SECONDS));
			assertTrue("reader observed EOF before the success listener completed", !reader.isDone());
			releaseListener.countDown();
			assertEquals("{}", new String(reader.get(10, TimeUnit.SECONDS), StandardCharsets.UTF_8));
		}
	}

	private static Checksum exportAndChecksum(byte[] payload) throws Exception {
		RecordingListener listener = new RecordingListener();
		try (ChecksummedInputStream in = ChecksummedJsonExport.stream(out -> out.write(payload), listener)) {
			in.readAllBytes();
			return in.checksum();
		}
	}

	private static String sha256(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}
}
