package app.owlcms.access;

import app.owlcms.data.access.RememberedLogin;
import app.owlcms.data.config.Config;

/**
 * Shared names and PIN-basis logic for remembered browsers. Authentication itself is checked against the server-side
 * {@link RememberedLogin} rows by {@link RememberedLogins}.
 */
public final class RememberedBrowser {

	public enum Kind {
		OFFICIALS("owlcms-officials"),
		DISPLAY("owlcms-display"),
		/** Accounts mode: keeps one account logged in until midnight; see {@link RememberedAccount}. */
		ACCOUNT("owlcms-account");

		private final String cookieName;

		Kind(String cookieName) {
			this.cookieName = cookieName;
		}

		public String getCookieName() {
			return this.cookieName;
		}
	}

	private RememberedBrowser() {
	}

	/** @return the value the cookie is tied to, or null when no PIN is required (nothing to remember) */
	public static String pinBasis(Kind kind, Config config) {
		if (kind == Kind.ACCOUNT) {
			return null;
		}
		String override = kind == Kind.OFFICIALS ? config.getParamPin() : config.getParamDisplayPin();
		if (override != null) {
			return override.isBlank() ? null : "override:" + override;
		}
		String stored = kind == Kind.OFFICIALS ? config.getPin() : config.getDisplayPin();
		return stored == null || stored.isBlank() ? null : "stored:" + stored;
	}
}
