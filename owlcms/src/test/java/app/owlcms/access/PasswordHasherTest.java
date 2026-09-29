package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {

	@Test
	public void roundTripAndWrongPassword() {
		String hash = PasswordHasher.hash("correct horse");
		assertTrue(PasswordHasher.verify("correct horse", hash));
		assertFalse(PasswordHasher.verify("correct horsf", hash));
		assertFalse(PasswordHasher.verify("", hash));
	}

	@Test
	public void saltMakesHashesDiffer() {
		assertNotEquals(PasswordHasher.hash("same password"), PasswordHasher.hash("same password"));
	}

	@Test
	public void iterationCountIsStoredInTheHash() {
		String hash = PasswordHasher.hash("some password");
		assertTrue(hash.startsWith("pbkdf2-sha256$"));
		assertEquals(310_000, PasswordHasher.iterationsOf(hash));
	}

	@Test
	public void tamperedOrMalformedHashesAreRejected() {
		String hash = PasswordHasher.hash("some password");
		String[] parts = hash.split("\\$");
		String tampered = parts[0] + "$" + parts[1] + "$" + parts[2] + "$" + parts[3].replace('A', 'B') + "x";
		assertFalse(PasswordHasher.verify("some password", tampered));
		assertFalse(PasswordHasher.verify("some password", null));
		assertFalse(PasswordHasher.verify("some password", ""));
		assertFalse(PasswordHasher.verify("some password", "pbkdf2-sha256$abc$AAAA$AAAA"));
		assertFalse(PasswordHasher.verify("some password", "md5$1$AAAA$AAAA"));
		assertFalse(PasswordHasher.verify(null, hash));
	}

	@Test
	public void nonAsciiPasswordsAreNormalized() {
		String composed = "p\u00e4ssw\u00f6rd\u00e9";
		String decomposed = "pa\u0308sswo\u0308rde\u0301";
		String hash = PasswordHasher.hash(composed);
		assertTrue(PasswordHasher.verify(decomposed, hash));
		assertTrue(PasswordHasher.verify("\u5bc6\u7801\u5bc6\u7801\u5bc6\u7801", PasswordHasher.hash("\u5bc6\u7801\u5bc6\u7801\u5bc6\u7801")));
	}

	@Test
	public void minimumLength() {
		assertFalse(PasswordHasher.isAcceptable(null));
		assertFalse(PasswordHasher.isAcceptable("1234567"));
		assertTrue(PasswordHasher.isAcceptable("12345678"));
	}
}
