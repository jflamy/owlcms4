package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class SealedAuditStreamTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T11:30:00Z"), ZoneOffset.UTC);

	@Test
	public void explicitBoundarySealsAllFieldsOfOneSaveTogether() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		AuditSealLimits limits = AuditSealLimits.DEFAULT;
		try (AuditSigningQueue queue = new AuditSigningQueue(limits.queueBytes());
				SealedAuditStream stream = stream(out, queue, limits, new AtomicLong(), "A")) {
			stream.append(bytes("2 | WEIGHIN bodyWeight - -> 81.35\n"));
			stream.append(bytes("3 | WEIGHIN declaration - -> 120\n"));
			stream.closeBlock();
			stream.awaitSeals();
			List<AuditMarker> markers = markers(out);
			assertEquals(2, markers.size());
			AuditMarker.Seal seal = (AuditMarker.Seal) markers.get(1);
			assertEquals(2, seal.block().firstLine());
			assertEquals(3, seal.block().lastLine());
			stream.closeBlock();
			stream.awaitSeals();
			assertEquals(2, markers(out).size());
		}
		assertTrue(markers(out).getLast() instanceof AuditMarker.Final);
		verify(out);
	}

	@Test
	public void idleAndAgeUseIndependentTimersAndNeverSealAnEmptyBlock() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		AtomicLong nanos = new AtomicLong();
		AuditSealLimits limits = new AuditSealLimits(1024, 100, Duration.ofMinutes(5),
				Duration.ofMinutes(5), 4096, 10000);
		try (AuditSigningQueue queue = new AuditSigningQueue(limits.queueBytes());
				SealedAuditStream stream = stream(out, queue, limits, nanos, "A")) {
			nanos.set(Duration.ofHours(1).toNanos());
			stream.tick();
			assertEquals(1, markers(out).size());
			stream.append(bytes("2 | clock.start\n"));
			nanos.addAndGet(Duration.ofMinutes(4).toNanos());
			stream.append(bytes("3 | clock.stop\n"));
			stream.tick();
			stream.awaitSeals();
			assertEquals(1, markers(out).size());
			nanos.addAndGet(Duration.ofMinutes(1).toNanos());
			stream.tick();
			stream.awaitSeals();
			assertEquals(2, markers(out).size());
			stream.tick();
			stream.awaitSeals();
			assertEquals(2, markers(out).size());
		}
		verify(out);
	}

	@Test
	public void recordAndByteLimitsCloseBeforeExceedingABlock() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		AuditSealLimits limits = new AuditSealLimits(64, 2, Duration.ofMinutes(5),
				Duration.ofMinutes(5), 256, 10000);
		try (AuditSigningQueue queue = new AuditSigningQueue(limits.queueBytes());
				SealedAuditStream stream = stream(out, queue, limits, new AtomicLong(), "A")) {
			stream.append(bytes("2 | a\n"));
			stream.append(bytes("3 | b\n"));
			stream.awaitSeals();
			assertEquals(2, markers(out).size());
			stream.append(bytes("4 | " + "x".repeat(40) + "\n"));
			stream.append(bytes("5 | " + "y".repeat(40) + "\n"));
			stream.awaitSeals();
			assertEquals(3, markers(out).size());
			assertThrows(IOException.class, () -> stream.append(bytes("6 | " + "z".repeat(64) + "\n")));
			stream.closeBlock();
			stream.awaitSeals();
			AuditMarker.Seal latest = (AuditMarker.Seal) markers(out).getLast();
			assertEquals(5, latest.block().lastLine());
			assertTrue(latest.block().byteCount() <= limits.blockBytes());
		}

		verify(out);
	}

	@Test
	public void idleTriggerCanSealBeforeTheMaximumAge() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		AtomicLong nanos = new AtomicLong();
		AuditSealLimits limits = new AuditSealLimits(1024, 100, Duration.ofMinutes(15),
				Duration.ofMinutes(5), 4096, 10000);
		try (AuditSigningQueue queue = new AuditSigningQueue(limits.queueBytes());
				SealedAuditStream stream = stream(out, queue, limits, nanos, "A")) {
			stream.append(bytes("2 | auth.login\n"));
			nanos.set(Duration.ofMinutes(5).toNanos() - 1);
			stream.tick();
			stream.awaitSeals();
			assertEquals(1, markers(out).size());
			nanos.incrementAndGet();
			stream.tick();
			stream.awaitSeals();
			assertEquals(2, markers(out).size());
		}
		verify(out);
	}

	@Test
	public void platformStreamsShareTheQueueButNotTheirBlockChains() throws Exception {
		ByteArrayOutputStream a = new ByteArrayOutputStream();
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		AuditSealLimits limits = AuditSealLimits.DEFAULT;
		try (AuditSigningQueue queue = new AuditSigningQueue(limits.queueBytes());
				SealedAuditStream streamA = stream(a, queue, limits, new AtomicLong(), "A");
				SealedAuditStream streamB = stream(b, queue, limits, new AtomicLong(), "B")) {
			streamA.append(bytes("2 | A clock.start\n"));
			streamB.append(bytes("2 | B clock.start\n"));
			streamB.closeBlock();
			streamA.closeBlock();
			streamA.awaitSeals();
			streamB.awaitSeals();
			assertEquals("A", markers(a).getLast().identity().stream());
			assertEquals("B", markers(b).getLast().identity().stream());
		}
		verify(a);
		verify(b);
	}

	@Test
	public void signingQueueBackpressureIsByteBounded() throws Exception {
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch secondStarted = new CountDownLatch(1);
		try (AuditSigningQueue queue = new AuditSigningQueue(4)) {
			CompletableFuture<Void> first = queue.submit(4, () -> {
				entered.countDown();
				assertTrue(release.await(5, TimeUnit.SECONDS));
			});
			try {
				assertTrue(entered.await(5, TimeUnit.SECONDS));
				assertEquals(4, queue.pendingBytes());
				CompletableFuture<CompletableFuture<Void>> second = CompletableFuture.supplyAsync(() -> {
					secondStarted.countDown();
					try {
						return queue.submit(1, () -> {});
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				});
				assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
				assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
				assertEquals(4, queue.pendingBytes());
				release.countDown();
				first.get(5, TimeUnit.SECONDS);
				second.get(5, TimeUnit.SECONDS).get(5, TimeUnit.SECONDS);
			} finally {
				release.countDown();
			}
		}
	}

	@Test
	public void publicationFailureDoesNotProduceAFinalOrSilentlyContinue() throws Exception {
		ByteArrayOutputStream stored = new ByteArrayOutputStream();
		OutputStream failing = new OutputStream() {
			@Override
			public void write(int value) throws IOException {
				stored.write(value);
			}

			@Override
			public void write(byte[] value) throws IOException {
				if (new String(value, StandardCharsets.UTF_8).startsWith("SEAL ")) {
					throw new IOException("simulated disk full");
				}
				stored.write(value);
			}
		};
		try (AuditSigningQueue queue = new AuditSigningQueue(4096)) {
			SealedAuditStream stream = stream(failing, queue, AuditSealLimits.DEFAULT, new AtomicLong(), "A");
			stream.append(bytes("2 | clock.start\n"));
			stream.closeBlock();
			assertThrows(IOException.class, stream::awaitSeals);
			assertThrows(IOException.class, () -> stream.append(bytes("3 | clock.stop\n")));
			assertThrows(IOException.class, stream::close);
		}
		assertEquals(1, markers(stored).size());
		assertFalse(stored.toString(StandardCharsets.UTF_8).contains("FINAL "));
	}

	private static SealedAuditStream stream(OutputStream out, AuditSigningQueue queue, AuditSealLimits limits,
			AtomicLong nanos, String platform) throws Exception {
		return new SealedAuditStream(out, AuditSigningKey.builtin(), queue, limits, CLOCK, nanos::get,
				platform, "run-123", platform + "_full_fixture.log", "69.0.0", bytes("1 | audit.open\n"), null, null);
	}

	private static List<AuditMarker> markers(ByteArrayOutputStream output) throws IOException {
		List<AuditMarker> result = new ArrayList<>();
		for (String line : output.toString(StandardCharsets.UTF_8).split("\n")) {
			if (!line.contains(" | ")) {
				result.add(AuditMarkerCodec.parse(line).marker());
			}
		}
		return result;
	}

	private static void verify(ByteArrayOutputStream output) throws Exception {
		AuditPublicKey key = AuditPublicKey.fromBase64(AuditSigningKey.builtin().publicKeyBase64());
		AuditBlockChain chain = new AuditBlockChain();
		Map<Long, byte[]> records = new TreeMap<>();
		for (String line : output.toString(StandardCharsets.UTF_8).split("\n")) {
			if (line.contains(" | ")) {
				records.put(Long.parseLong(line.substring(0, line.indexOf(" | "))), bytes(line + "\n"));
			} else {
				AuditMarkerCodec.Signed signed = AuditMarkerCodec.parse(line);
				AuditMarker.Block block = switch (signed.marker()) {
					case AuditMarker.First first -> first.block();
					case AuditMarker.Continue continuation -> continuation.block();
					case AuditMarker.Seal seal -> seal.block();
					case AuditMarker.Final end -> {
						assertEquals(records.size(), end.recordCount());
						yield null;
					}
				};
				ByteArrayOutputStream covered = new ByteArrayOutputStream();
				if (block != null) {
					for (long sequence = block.firstLine(); sequence <= block.lastLine(); sequence++) {
						covered.writeBytes(records.get(sequence));
					}
				}
				chain.accept(signed, covered.toByteArray(), key);
			}
		}
		assertTrue(chain.isFinalized());
	}

	private static byte[] bytes(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}
}
