package app.owlcms.access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.account.AccountToken;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;

/**
 * Account tokens: generation and the self-service change. A token gives exactly the rights its owner has when it
 * is used; only its SHA-256 is stored and exported.
 */
public final class AccountTokens {

	/** 64 symbols, so each character carries 6 bits: 12 characters give 72 bits. */
	static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
	static final int LENGTH = 12;
	private static final String ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int ID_LENGTH = 6;
	private static final SecureRandom RANDOM = new SecureRandom();

	/** A freshly generated token: the value to show once, and the record to store. */
	public record Generated(String value, AccountToken record) {
	}

	private AccountTokens() {
	}

	public static Generated generate() {
		String value = random(ALPHABET, LENGTH);
		return new Generated(value, new AccountToken(random(ID_ALPHABET, ID_LENGTH), hash(value), LocalDateTime.now()));
	}

	public static String hash(String token) {
		try {
			return HexFormat.of().formatHex(
			        MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * A user replacing or revoking their own token; the account comes from the session, never from the request.
	 *
	 * @param token the new token record, or null to revoke
	 * @return false when the session has no account of its own
	 */
	public static synchronized boolean setOwnToken(Principal principal, AccountToken token, AuditOrigin origin) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT) {
			return false;
		}
		UserAccount cached = UserAccountRepository.findById(principal.accountId());
		if (cached == null) {
			return false;
		}
		UserAccount account = cached.copy();
		account.replaceToken(token);
		UserAccountRepository.save(account, origin);
		return true;
	}

	private static String random(String alphabet, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
		}
		return sb.toString();
	}
}
