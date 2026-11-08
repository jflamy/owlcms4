package app.owlcms.access;

/**
 * Permissions are what pages ({@link RequiresPermission}) and capabilities check. A permission and a role that share
 * a name are unrelated except through {@link Role#permissions()}; matching names are for readability only and must not
 * be relied on.
 * <p>
 * DISPLAYS is for the competition venue's own screens. STREAMING is for what a streaming client may show. Every page
 * naming STREAMING also names DISPLAYS. RESULTS gives results access only; EXTERNAL is a role that expands to RESULTS
 * and STREAMING.
 */
public enum Permission {
	ADMIN_PAGES(false, false),
	COMPETITION_SUPERVISOR(false, false),
	PREPARATION(false, false),
	REGISTRATION(false, false),
	/**
	 * Registration, SBDE and database imports. Not granted alone: it comes with the Preparation and Registration
	 * roles, Competition Supervisor and admin, not with the Competition Director.
	 */
	IMPORT(false, false),
	WEIGHIN(false, false),
	RESULTS(false, false),
	/** In-page actions, not navigation permissions. */
	METADATA_EDITING(false, false),
	ATHLETE_CARD_EDITING(true, true),
	ANNOUNCER(true, true),
	MARSHAL(true, true),
	TIMEKEEPER(true, true),
	TC(true, true),
	JURY(true, true),
	REFEREE(true, true),
	DISPLAYS(true, false),
	STREAMING(false, false);

	private final boolean platformScopable;
	private final boolean lockable;

	Permission(boolean platformScopable, boolean lockable) {
		this.platformScopable = platformScopable;
		this.lockable = lockable;
	}

	public boolean isPlatformScopable() {
		return this.platformScopable;
	}

	/** Effective only on the platform chosen at login (accounts mode). */
	public boolean isLockable() {
		return this.lockable;
	}
}
