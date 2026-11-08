package app.owlcms.access;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Roles are what is granted to accounts and stored in account data. Each role declares the permissions it grants; that
 * declaration is the only link between roles and permissions. A role and a permission that share a name are otherwise
 * unrelated: matching names are for readability only and must not be relied on.
 */
public enum Role {
	ADMIN(false, Permission.values()),
	/** The TIS operator who supervises the competition from the Competition Supervisor page. */
	COMPETITION_SUPERVISOR(false, Permission.COMPETITION_SUPERVISOR, Permission.PREPARATION, Permission.REGISTRATION,
	        Permission.IMPORT, Permission.WEIGHIN, Permission.RESULTS, Permission.ANNOUNCER, Permission.MARSHAL,
	        Permission.TIMEKEEPER, Permission.TC, Permission.JURY, Permission.REFEREE, Permission.DISPLAYS,
	        Permission.STREAMING, Permission.METADATA_EDITING, Permission.ATHLETE_CARD_EDITING),
	/** The rules-defined official, who often does not use the system: preparation and results only. */
	COMPETITION_DIRECTOR(false, Permission.PREPARATION, Permission.RESULTS, Permission.DISPLAYS,
	        Permission.METADATA_EDITING),
	PREPARATION(false, Permission.PREPARATION, Permission.IMPORT, Permission.METADATA_EDITING,
	        Permission.ATHLETE_CARD_EDITING),
	REGISTRATION(false, Permission.REGISTRATION, Permission.IMPORT, Permission.METADATA_EDITING),
	WEIGHIN(false, Permission.WEIGHIN, Permission.METADATA_EDITING),
	RESULTS(false, Permission.RESULTS),
	PLATFORM(true, Permission.ANNOUNCER, Permission.MARSHAL, Permission.TIMEKEEPER, Permission.TC,
	        Permission.DISPLAYS, Permission.ATHLETE_CARD_EDITING),
	ANNOUNCER(true, Permission.ANNOUNCER, Permission.ATHLETE_CARD_EDITING),
	MARSHAL(true, Permission.MARSHAL, Permission.ATHLETE_CARD_EDITING),
	TIMEKEEPER(true, Permission.TIMEKEEPER),
	TC(true, Permission.TC),
	JURY(true, Permission.JURY),
	REFEREE(true, Permission.REFEREE),
	DISPLAYS(true, Permission.DISPLAYS),
	STREAMING(false, Permission.STREAMING),
	EXTERNAL(false, Permission.RESULTS, Permission.STREAMING);

	private final boolean platformScopable;
	private final Set<Permission> permissions;

	/**
	 * @param platformScopable whether a grant of this role may be limited to one platform
	 * @param permissions      the permissions the role grants
	 */
	Role(boolean platformScopable, Permission... permissions) {
		this.platformScopable = platformScopable;
		this.permissions = Collections.unmodifiableSet(EnumSet.copyOf(List.of(permissions)));
	}

	public boolean isPlatformScopable() {
		return this.platformScopable;
	}

	/**
	 * Operates on every platform: never locked to the platform chosen at login and not asked to choose one. A property
	 * of the granted role, not of its permissions.
	 */
	public boolean coversAllPlatforms() {
		return this == Role.ADMIN || this == Role.COMPETITION_SUPERVISOR;
	}

	/**
	 * The permissions declared for this role. A role does not imply the permission of the same name unless it is
	 * declared here.
	 */
	public Set<Permission> permissions() {
		return this.permissions;
	}
}
