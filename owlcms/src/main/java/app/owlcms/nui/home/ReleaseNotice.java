package app.owlcms.nui.home;

import java.util.Locale;

import app.owlcms.i18n.Translator;

/**
 * Markdown notice shown on the home page according to the kind of version running (development snapshot, alpha,
 * beta, release candidate or release).
 *
 * The notice is the translation of {@code ReleaseNotice.<kind>}; there is no key for releases.
 */
public final class ReleaseNotice {

	private ReleaseNotice() {
	}

	public static String kind(String version) {
		String v = version == null ? "" : version.toLowerCase(Locale.ROOT);
		if (v.contains("-snapshot")) {
			return "snapshot";
		}
		if (v.contains("-alpha")) {
			return "alpha";
		}
		if (v.contains("-beta")) {
			return "beta";
		}
		if (v.contains("-rc")) {
			return "rc";
		}
		return "release";
	}

	public static String translationKey(String version) {
		return "ReleaseNotice." + kind(version);
	}

	/**
	 * The locale is explicit because this is called from a background thread, where the session locale is not
	 * available.
	 *
	 * @return the markdown notice for the running version, or null if there is none.
	 */
	public static String read(String version, Locale locale) {
		String text = Translator.translateNoOverrideOrElseNull(translationKey(version),
		        locale != null ? locale : Locale.ENGLISH);
		return text == null || text.isBlank() ? null : text;
	}
}
