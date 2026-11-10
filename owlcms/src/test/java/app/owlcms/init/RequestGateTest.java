/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class RequestGateTest {

	private static final long BLOCKED_MS = 200;
	private static final long RELEASED_MS = 5000;

	/** OWLCMS_LOADJSON: the import runs inside startup, and its end must not let requests through. */
	@Test
	public void importDuringStartupDoesNotReleaseRequests() throws InterruptedException {
		RequestGate gate = new RequestGate();
		CountDownLatch beforeImport = waiter(gate);

		gate.importStarted();
		CountDownLatch duringImport = waiter(gate);
		gate.importEnded();
		CountDownLatch afterImport = waiter(gate);

		assertFalse("request made before the import", beforeImport.await(BLOCKED_MS, TimeUnit.MILLISECONDS));
		assertFalse("request made during the import", duringImport.await(BLOCKED_MS, TimeUnit.MILLISECONDS));
		assertFalse("request made after the import", afterImport.await(BLOCKED_MS, TimeUnit.MILLISECONDS));

		gate.startupDone();

		// none is left waiting on a condition that is never signalled
		assertTrue("request made before the import", beforeImport.await(RELEASED_MS, TimeUnit.MILLISECONDS));
		assertTrue("request made during the import", duringImport.await(RELEASED_MS, TimeUnit.MILLISECONDS));
		assertTrue("request made after the import", afterImport.await(RELEASED_MS, TimeUnit.MILLISECONDS));
	}

	@Test
	public void importOnRunningServerHoldsRequests() throws InterruptedException {
		RequestGate gate = new RequestGate();
		gate.startupDone();
		assertTrue(waiter(gate).await(RELEASED_MS, TimeUnit.MILLISECONDS));

		gate.importStarted();
		CountDownLatch duringImport = waiter(gate);
		assertFalse(duringImport.await(BLOCKED_MS, TimeUnit.MILLISECONDS));

		gate.importEnded();
		assertTrue(duringImport.await(RELEASED_MS, TimeUnit.MILLISECONDS));
		assertTrue(waiter(gate).await(RELEASED_MS, TimeUnit.MILLISECONDS));
	}

	@Test
	public void startupCompletingDuringImportHoldsRequests() throws InterruptedException {
		RequestGate gate = new RequestGate();
		gate.importStarted();
		CountDownLatch request = waiter(gate);
		gate.startupDone();
		assertFalse(request.await(BLOCKED_MS, TimeUnit.MILLISECONDS));

		gate.importEnded();
		assertTrue(request.await(RELEASED_MS, TimeUnit.MILLISECONDS));
	}

	/** @return counted down once a request thread has gone through the gate */
	private CountDownLatch waiter(RequestGate gate) throws InterruptedException {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch passed = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			started.countDown();
			try {
				gate.await();
				passed.countDown();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		thread.setDaemon(true);
		thread.start();
		assertTrue(started.await(RELEASED_MS, TimeUnit.MILLISECONDS));
		return passed;
	}
}
