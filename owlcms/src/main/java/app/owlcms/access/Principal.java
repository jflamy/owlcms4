package app.owlcms.access;

import java.util.List;

import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;

/**
 * The authenticated subject of a session or of a token tab. {@code tokenId} identifies the account token of a token tab
 * (it is not secret); it is null otherwise.
 */
public record Principal(AuthSource source, Long accountId, String username, List<RoleGrant> grants,
        String loginPlatform, String tokenId) {

	public enum AuthSource {
		ACCOUNT, TOKEN, OFFICIALS_PIN, DISPLAY_PIN, NO_PIN
	}

	public Principal(AuthSource source, Long accountId, String username, List<RoleGrant> grants,
	        String loginPlatform) {
		this(source, accountId, username, grants, loginPlatform, null);
	}

	/** Officials PIN, no PIN expected, or backdoor: everything, all platforms, live platform selectors. */
	public static Principal admin(AuthSource source) {
		return new Principal(source, null, "-", List.of(new RoleGrant(Role.ADMIN, null)), null);
	}

	public static Principal displays() {
		return new Principal(AuthSource.DISPLAY_PIN, null, "-", List.of(new RoleGrant(Role.DISPLAYS, null)), null);
	}

	/** Rebuilt from the account at each navigation, so revoked grants take effect immediately. */
	public static Principal of(UserAccount account, String loginPlatform) {
		return new Principal(AuthSource.ACCOUNT, account.getId(), account.getUsername(),
		        List.copyOf(account.getGrants()), loginPlatform);
	}

	/**
	 * Authenticated by an account token, for one tab. Rebuilt from the account at each navigation, so revoked grants
	 * take effect at the next navigation. It is not an account session: no password change, no platform lock.
	 */
	public static Principal ofToken(UserAccount account, String tokenId) {
		return new Principal(AuthSource.TOKEN, account.getId(), account.getUsername(),
		        List.copyOf(account.getGrants()), null, tokenId);
	}

	public Principal withLoginPlatform(String platform) {
		return new Principal(this.source, this.accountId, this.username, this.grants, platform, this.tokenId);
	}
}
