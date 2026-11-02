package app.owlcms.access;

import java.util.EnumSet;
import java.util.Set;

/**
 * Roles known to the access control. Pages only reference base roles; grantable roles may be composite and expand to
 * base roles in {@link #expand()}.
 * <p>
 * DISPLAYS is for the competition venue's own screens. STREAMING is for what a streaming client may show. Every page
 * naming STREAMING also names DISPLAYS. RESULTS gives results access only; EXTERNAL expands to RESULTS and STREAMING.
 */
public enum Role {
	ADMIN(true, false, false, false),
	ADMIN_PAGES(false, false, false, true),
	/** The TIS operator who supervises the competition from the Competition Supervisor page. */
	COMPETITION_SUPERVISOR(true, false, false, true),
	/** The rules-defined official, who often does not use the system: preparation and results only. */
	COMPETITION_DIRECTOR(true, false, false, false),
	PREPARATION(true, false, false, true),
	REGISTRATION(true, false, false, true),
	/**
	 * Registration, SBDE and database imports. Not granted alone: it comes with the Preparation and Registration
	 * roles, Competition Supervisor and admin, not with the Competition Director.
	 */
	IMPORT(false, false, false, true),
	WEIGHIN(true, false, false, true),
	RESULTS(true, false, false, true),
	PLATFORM(true, true, false, false),
	ANNOUNCER(true, true, true, true),
	MARSHAL(true, true, true, true),
	TIMEKEEPER(true, true, true, true),
	TC(true, true, true, true),
	JURY(true, true, true, true),
	REFEREE(true, true, true, true),
	DISPLAYS(true, true, false, true),
	STREAMING(true, false, false, true),
	EXTERNAL(true, false, false, false);

	private final boolean grantable;
	private final boolean platformScopable;
	private final boolean lockable;
	private final boolean base;

	Role(boolean grantable, boolean platformScopable, boolean lockable, boolean base) {
		this.grantable = grantable;
		this.platformScopable = platformScopable;
		this.lockable = lockable;
		this.base = base;
	}

	public boolean isGrantable() {
		return this.grantable;
	}

	public boolean isPlatformScopable() {
		return this.platformScopable;
	}

	/** Effective only on the platform chosen at login (accounts mode). */
	public boolean isLockable() {
		return this.lockable;
	}

	/** Base roles are the only ones pages refer to. */
	public boolean isBase() {
		return this.base;
	}

	/**
	 * Operates on every platform: never locked to the platform chosen at login and not asked to choose one. A property
	 * of the granted role, not of its permissions.
	 */
	public boolean coversAllPlatforms() {
		return this == ADMIN || this == COMPETITION_SUPERVISOR;
	}

	public Set<Role> expand() {
		return switch (this) {
			case ADMIN -> baseRoles();
			case COMPETITION_SUPERVISOR -> EnumSet.of(COMPETITION_SUPERVISOR, PREPARATION, REGISTRATION, IMPORT,
			        WEIGHIN, RESULTS, ANNOUNCER, MARSHAL, TIMEKEEPER, TC, JURY, REFEREE, DISPLAYS, STREAMING);
			case COMPETITION_DIRECTOR -> EnumSet.of(PREPARATION, RESULTS, DISPLAYS);
			case PLATFORM -> EnumSet.of(ANNOUNCER, MARSHAL, TIMEKEEPER, TC, DISPLAYS);
			case EXTERNAL -> EnumSet.of(RESULTS, STREAMING);
			case PREPARATION -> EnumSet.of(PREPARATION, IMPORT);
			case REGISTRATION -> EnumSet.of(REGISTRATION, IMPORT);
			default -> EnumSet.of(this);
		};
	}

	public static Set<Role> baseRoles() {
		Set<Role> result = EnumSet.noneOf(Role.class);
		for (Role role : values()) {
			if (role.base) {
				result.add(role);
			}
		}
		return result;
	}
}
