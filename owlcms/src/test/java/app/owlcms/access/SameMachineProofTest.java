package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SameMachineProofTest {

	private static final String TOKEN = "3f1c9a7be2d84c60a5f0e91b7d2c4e8a6b1d3f5a7c9e0b2d";

	@Test
	public void theTokenMustBePresentedAndMatch() {
		assertNull(SameMachineProof.refusal(TOKEN, TOKEN));
		assertNull("surrounding spaces are ignored", SameMachineProof.refusal(TOKEN, " " + TOKEN + " "));
		assertEquals("invalid", SameMachineProof.refusal(TOKEN, TOKEN.toUpperCase()));
		assertEquals("invalid", SameMachineProof.refusal(TOKEN, "wrong"));
		assertEquals("missing", SameMachineProof.refusal(TOKEN, null));
		assertEquals("missing", SameMachineProof.refusal(TOKEN, " "));
	}

	@Test
	public void withoutATokenEverythingIsRefused() {
		assertEquals(SameMachineProof.NO_TOKEN, SameMachineProof.refusal(null, null));
		assertEquals(SameMachineProof.NO_TOKEN, SameMachineProof.refusal(" ", " "));
		assertEquals(SameMachineProof.NO_TOKEN, SameMachineProof.refusal(null, TOKEN));
	}

	@Test
	public void comparisonNeedsBothValues() {
		assertTrue(SameMachineProof.matches(TOKEN, TOKEN));
		assertFalse(SameMachineProof.matches(null, TOKEN));
		assertFalse(SameMachineProof.matches(TOKEN, null));
		assertFalse(SameMachineProof.matches(TOKEN, " "));
	}
}
