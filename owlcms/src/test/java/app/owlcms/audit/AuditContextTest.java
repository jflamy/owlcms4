package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class AuditContextTest {
	@Test
	public void nestedContextRestoresOuterEntry() {
		AuditActor outer = AuditActor.device("TIMEKEEPER", null, "clock");
		AuditActor inner = AuditActor.device("REFEREE", 2, "refbox");
		AuditContext.run(outer, "outer", () -> {
			assertEquals(outer, AuditContext.actor());
			AuditContext.run(inner, "inner", () -> assertEquals(inner, AuditContext.actor()));
			assertEquals(outer, AuditContext.actor());
		});
		assertNull(AuditContext.actor());
	}

	@Test
	public void suppressionAndExceptionsAreCleanedUp() {
		assertThrows(IllegalStateException.class,
				() -> AuditContext.suppressed(() -> {
					assertTrue(AuditContext.isSuppressed());
					throw new IllegalStateException();
				}));
		assertFalse(AuditContext.isSuppressed());
	}

	@Test
	public void valueReturningSuppressionReturnsValueAndCleansUp() {
		String result = AuditContext.suppressed(() -> {
			assertTrue(AuditContext.isSuppressed());
			return "result";
		});

		assertEquals("result", result);
		assertFalse(AuditContext.isSuppressed());
	}

	@Test
	public void checkedSuppressionPropagatesExceptionAndCleansUp() {
		assertThrows(IOException.class, () -> AuditContext.suppressedChecked(() -> {
			assertTrue(AuditContext.isSuppressed());
			throw new IOException();
		}));
		assertFalse(AuditContext.isSuppressed());
	}
}