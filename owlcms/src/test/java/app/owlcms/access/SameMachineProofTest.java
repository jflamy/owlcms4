package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

public class SameMachineProofTest {

	private static final String TOKEN = "3f1c9a7be2d84c60a5f0e91b7d2c4e8a6b1d3f5a7c9e0b2d";
	private static final Function<String, String> NO_HEADERS = Map.<String, String>of()::get;

	@Test
	public void aPresentedTokenMustMatchWhateverTheAddress() {
		for (boolean mandatory : new boolean[] { false, true }) {
			assertNull(SameMachineProof.refusal(TOKEN, TOKEN, "203.0.113.7", NO_HEADERS, mandatory));
			assertNull("surrounding spaces are ignored",
			        SameMachineProof.refusal(TOKEN, " " + TOKEN + " ", "127.0.0.1", NO_HEADERS, mandatory));
			assertEquals("invalid",
			        SameMachineProof.refusal(TOKEN, TOKEN.toUpperCase(), "127.0.0.1", NO_HEADERS, mandatory));
			assertEquals("a wrong token is refused even from loopback", "invalid",
			        SameMachineProof.refusal(TOKEN, "wrong", "127.0.0.1", NO_HEADERS, mandatory));
			assertEquals("no token was given at launch", "invalid",
			        SameMachineProof.refusal(null, TOKEN, "127.0.0.1", NO_HEADERS, mandatory));
		}
	}

	@Test
	public void withoutATokenADirectLocalConnectionIsAccepted() {
		assertNull(SameMachineProof.refusal(TOKEN, null, "127.0.0.1", NO_HEADERS, false));
		assertNull(SameMachineProof.refusal(null, " ", "0:0:0:0:0:0:0:1", NO_HEADERS, false));
		assertEquals("not-local", SameMachineProof.refusal(TOKEN, null, "192.168.1.10", NO_HEADERS, false));
		assertEquals("not-local", SameMachineProof.refusal(null, null, null, NO_HEADERS, false));
		for (String header : SameMachineProof.FORWARDING_HEADERS) {
			assertEquals("forwarded by a proxy: " + header, "not-local",
			        SameMachineProof.refusal(null, null, "127.0.0.1", Map.of(header, "203.0.113.7")::get, false));
		}
		assertEquals("a forwarded loopback claim is not trusted", "not-local", SameMachineProof.refusal(null, null,
		        "127.0.0.1", Map.of("X-Forwarded-For", "127.0.0.1")::get, false));
		assertNull(SameMachineProof.refusal(null, null, "127.0.0.1", Map.of("X-Forwarded-For", " ")::get, false));
	}

	@Test
	public void inIwfComplianceTheTokenIsMandatory() {
		assertEquals("missing", SameMachineProof.refusal(TOKEN, null, "127.0.0.1", NO_HEADERS, true));
		assertEquals("missing", SameMachineProof.refusal(null, null, "127.0.0.1", NO_HEADERS, true));
		assertNull(SameMachineProof.refusal(TOKEN, TOKEN, "127.0.0.1", NO_HEADERS, true));
	}

	@Test
	public void comparisonNeedsBothValues() {
		assertTrue(SameMachineProof.matches(TOKEN, TOKEN));
		assertFalse(SameMachineProof.matches(null, TOKEN));
		assertFalse(SameMachineProof.matches(TOKEN, null));
		assertFalse(SameMachineProof.matches(TOKEN, " "));
	}
}
