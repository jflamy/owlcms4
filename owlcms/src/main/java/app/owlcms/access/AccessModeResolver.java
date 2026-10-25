package app.owlcms.access;

import java.util.Locale;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/** Decides which access mode is in force; free of database and UI dependencies. */
public final class AccessModeResolver {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AccessModeResolver.class);

	private AccessModeResolver() {
	}

	/**
	 * @param override        the OWLCMS_ACCESSMODE environment or parameter value, may be null
	 * @param stored          the mode stored in the database, null if never set
	 * @param pinModeChosen   true if the administrator deliberately chose PIN mode
	 * @param pemForced       true if an audit signing key is configured
	 * @param adminHasPassword true if the built-in admin account has a password
	 * @return the mode in force
	 */
	public static AccessMode resolve(String override, AccessMode stored, Boolean pinModeChosen, boolean pemForced,
	        boolean adminHasPassword) {
		if (override != null && !override.isBlank()) {
			try {
				return AccessMode.valueOf(override.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				logger.warn("OWLCMS_ACCESSMODE '{}' ignored, expected PIN or ACCOUNTS", override);
			}
		}
		if (Boolean.TRUE.equals(pinModeChosen)) {
			return AccessMode.PIN;
		}
		if (pemForced && adminHasPassword) {
			return AccessMode.ACCOUNTS;
		}
		return stored != null ? stored : AccessMode.PIN;
	}
}
