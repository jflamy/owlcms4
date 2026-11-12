package app.owlcms.access;

/**
 * Page-level access rights: what pages ({@link RequiresAccess}) and {@link Permission}s check. An access right and a
 * role that share a name are unrelated except through {@link Role#accesses()}; matching names are for readability
 * only and must not be relied on.
 * <p>
 * DISPLAYS is for the competition venue's own screens. STREAMING is for what a streaming client may show. Every page
 * naming STREAMING also names DISPLAYS. RESULTS gives results access only; EXTERNAL is a role that expands to RESULTS
 * and STREAMING.
 */
public enum Access {
	ADMINISTRATOR(false),
	SUPERVISOR(false),
	PREPARATION(false),
	REGISTRATION(false),
	WEIGHIN(false),
	RESULTS(false),
	ANNOUNCER(true),
	SPEAKER(true),
	MARSHAL(true),
	TIMEKEEPER(true),
	TC(true),
	JURY(true),
	REFEREE(true),
	DISPLAYS(true),
	STREAMING(false);

	private final boolean platformScopable;

	Access(boolean platformScopable) {
		this.platformScopable = platformScopable;
	}

	public boolean isPlatformScopable() {
		return this.platformScopable;
	}
}
