package app.owlcms.tests;

import java.io.IOException;
import java.io.UncheckedIOException;

import app.owlcms.Main;
import app.owlcms.audit.AuditLog;
import app.owlcms.audit.AuditSigningKey;

/**
 * Test stand-in for {@code Main.init()}: starts audit sealing first, as the application does, then injects the
 * suppliers. The production audit writer runs unchanged; only the key is replaced by the built-in key, so tests never
 * sign with a key from the developer's home directory. Sealing starts once per test JVM.
 */
public final class TestStartup {
	private static boolean auditStarted;

	private TestStartup() {
	}

	public static synchronized void init() {
		if (!auditStarted) {
			try {
				AuditLog.initializeIntegrity(AuditSigningKey.builtin());
			} catch (Exception e) {
				throw new IllegalStateException("Test audit sealing could not start", e);
			}
			finishAuditAtExit();
			auditStarted = true;
		}
		Main.injectSuppliers();
	}

	/**
	 * Finalizes the audit files when the JVM exits, as the last step of Main's shutdown does. A failure is thrown, so
	 * that it is printed with the test output.
	 */
	public static void finishAuditAtExit() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				AuditLog.finish();
			} catch (IOException e) {
				throw new UncheckedIOException("Test audit files were not finalized", e);
			}
		}, "TestAuditShutdown"));
	}
}
