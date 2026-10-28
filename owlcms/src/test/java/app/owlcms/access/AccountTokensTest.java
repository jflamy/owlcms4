package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccountTokensTest {

	@Test
	public void tokensHaveTwelveCharactersFromTheUrlSafeAlphabet() {
		for (int i = 0; i < 200; i++) {
			String value = AccountTokens.generate().value();
			assertEquals(AccountTokens.LENGTH, value.length());
			assertTrue(value, value.chars().allMatch(c -> AccountTokens.ALPHABET.indexOf(c) >= 0));
		}
		assertEquals(64, AccountTokens.ALPHABET.length());
	}

	@Test
	public void recordKeepsOnlyTheHashAndANonSecretIdentifier() {
		AccountTokens.Generated generated = AccountTokens.generate();
		assertEquals(AccountTokens.hash(generated.value()), generated.record().getTokenHash());
		assertEquals(64, generated.record().getTokenHash().length());
		assertFalse(generated.record().getTokenHash().contains(generated.value()));
		assertFalse(generated.record().getTokenId().contains(generated.value()));
		assertTrue(generated.record().getCreatedAt() != null);
	}

	@Test
	public void successiveTokensDiffer() {
		AccountTokens.Generated first = AccountTokens.generate();
		AccountTokens.Generated second = AccountTokens.generate();
		assertNotEquals(first.value(), second.value());
		assertNotEquals(first.record().getTokenId(), second.record().getTokenId());
	}
}
