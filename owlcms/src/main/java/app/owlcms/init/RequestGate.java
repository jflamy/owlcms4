/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.util.concurrent.CountDownLatch;

/**
 * Holds request processing until the data can be used. There are two independent conditions: startup has completed,
 * and no database import is running. They are kept apart because an import can run as part of startup
 * (OWLCMS_LOADJSON): the end of that import must not let requests through while the rest of startup is still running.
 */
class RequestGate {

	private final CountDownLatch startup = new CountDownLatch(1);
	private volatile CountDownLatch noImport = new CountDownLatch(0);

	/** Blocks until startup has completed and no import is running. */
	void await() throws InterruptedException {
		this.startup.await();
		this.noImport.await();
	}

	/** Startup has completed. The startup condition is never replaced, so that no waiting request is left behind. */
	void startupDone() {
		this.startup.countDown();
	}

	synchronized void importStarted() {
		if (this.noImport.getCount() == 0) {
			this.noImport = new CountDownLatch(1);
		}
	}

	synchronized void importEnded() {
		this.noImport.countDown();
	}
}
