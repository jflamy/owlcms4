package app.owlcms.audit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SealedAuditBackendTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void asynchronousSubmissionDoesNotWaitForDiskButDrainAndCloseDo() throws Exception {
		Path root = temporary.getRoot().toPath();
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean first = new AtomicBoolean(true);
		SealedAuditBackend backend = new SealedAuditBackend(root, AuditSigningKey.builtin(),
				AuditSealLimits.DEFAULT, AuditFileStoreTest.CLOCK, new AtomicLong()::get, "run-1", "69.0.0",
				(platform, line) -> {
					if (first.getAndSet(false)) {
						entered.countDown();
						try {
							if (!release.await(5, TimeUnit.SECONDS)) {
								throw new IllegalStateException("Test did not release readable publication");
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new IllegalStateException(e);
						}
					}
				}, false);
		try {
			backend.write("A", List.of(AuditFileStoreTest.record("athlete.change", "bodyWeight=81")), true, false);
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			assertTrue(backend.healthy());
		} finally {
			release.countDown();
			backend.close();
		}
		assertTrue(AuditFileStoreTest.check(AuditFileStoreTest.files(root).getFirst()).getFirst().finalized());
	}

	@Test
	public void failedPublicationIsVisibleAndPreventsFurtherRecordsAndFinal() throws Exception {
		Path root = temporary.getRoot().toPath();
		SealedAuditBackend backend = new SealedAuditBackend(root, AuditSigningKey.builtin(),
				AuditSealLimits.DEFAULT, AuditFileStoreTest.CLOCK, new AtomicLong()::get, "run-1", "69.0.0",
				(platform, line) -> { throw new IllegalStateException("simulated disk failure"); }, false);
		assertThrows(IOException.class, () -> backend.write("A",
				List.of(AuditFileStoreTest.record("clock.start", "clock=1:00")), false, true));
		assertFalse(backend.healthy());
		assertThrows(IOException.class, () -> backend.write("A",
				List.of(AuditFileStoreTest.record("clock.stop", "clock=0:59")), false, true));
		assertThrows(IOException.class, backend::close);
		String log = Files.readString(AuditFileStoreTest.files(root).getFirst());
		assertFalse(log.contains("FINAL "));
	}
}
