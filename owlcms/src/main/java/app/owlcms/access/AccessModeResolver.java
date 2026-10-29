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
	 * @param iwfCompliance   the switch as read at startup; it requires accounts mode
	 * @return the mode in force; PIN unless accounts mode was chosen or required
	 */
	public static AccessMode resolve(String override, AccessMode stored, Boolean pinModeChosen,
	        boolean iwfCompliance) {
		if (iwfCompliance) {
			return AccessMode.ACCOUNTS;
		}
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
		return stored != null ? stored : AccessMode.PIN;
	}
}
