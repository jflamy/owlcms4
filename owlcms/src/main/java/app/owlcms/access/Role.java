package app.owlcms.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Roles are what is granted to accounts and stored in account data. Each role declares the access rights it grants;
 * that declaration is the only link between roles and access rights. A role and an access right that share a name are
 * otherwise unrelated: matching names are for readability only and must not be relied on.
 */
public enum Role {
	ADMIN(false, false, Access.values()),
	/** The TIS operator who supervises the competition from the Competition Supervisor page. */
	COMPETITION_SUPERVISOR(false, false, Access.SUPERVISOR, Access.PREPARATION,
	        Access.REGISTRATION, Access.WEIGHIN, Access.RESULTS, Access.ANNOUNCER,
	        Access.SPEAKER, Access.MARSHAL, Access.TIMEKEEPER, Access.TC, Access.JURY,
	        Access.REFEREE, Access.DISPLAYS, Access.STREAMING),
	/** The rules-defined official, who often does not use the system: preparation and results only. */
	COMPETITION_DIRECTOR(false, false, Access.PREPARATION, Access.RESULTS, Access.DISPLAYS),
	PREPARATION(false, false, Access.PREPARATION, Access.REGISTRATION, Access.RESULTS),
	REGISTRATION(false, false, Access.REGISTRATION),
	WEIGHIN(false, false, Access.WEIGHIN),
	RESULTS(false, false, Access.RESULTS),
	/**
	 * Retired: replaced by {@link #ANNOUNCER}. It is not grantable, not generated, and gives no access right; accounts
	 * that hold it are removed.
	 */
	PLATFORM(false, false),
	/** Active announcer with marshal, timekeeper, technical controller, display and athlete card controls. */
	ANNOUNCER(true, true, Access.ANNOUNCER, Access.SPEAKER, Access.MARSHAL, Access.TIMEKEEPER,
	        Access.TC, Access.DISPLAYS),
	/** Passive announcer (presenter only, no editing, no clock or decision controls). */
	SPEAKER(true, true, Access.SPEAKER),
	MARSHAL(true, true, Access.MARSHAL),
	TIMEKEEPER(true, true, Access.TIMEKEEPER),
	TC(true, true, Access.TC),
	JURY(true, true, Access.JURY),
	REFEREE(true, true, Access.REFEREE),
	DISPLAYS(true, false, Access.DISPLAYS),
	STREAMING(false, false, Access.STREAMING),
	EXTERNAL(false, false, Access.RESULTS, Access.STREAMING);

	private final boolean platformScopable;
	private final boolean lockable;
	private final Set<Access> accesses;

	/**
	 * @param platformScopable whether a grant of this role may be limited to one platform
	 * @param lockable         whether a grant for all platforms makes the account choose a platform at login (accounts
	 *                         mode) and be limited to it
	 * @param accesses         the access rights the role grants
	 */
	Role(boolean platformScopable, boolean lockable, Access... accesses) {
		this.platformScopable = platformScopable;
		this.lockable = lockable;
		Set<Access> granted = EnumSet.noneOf(Access.class);
		granted.addAll(Arrays.asList(accesses));
		this.accesses = Collections.unmodifiableSet(granted);
	}

	/** The roles that the account editor offers and that generic accounts are generated for. */
	public static List<Role> grantableRoles() {
		return Arrays.stream(values()).filter(role -> !role.isObsolete()).toList();
	}

	public boolean isPlatformScopable() {
		return this.platformScopable;
	}

	/**
	 * Whether the role is limited to the platform chosen at login, unless the account also covers all platforms. A
	 * property of the granted role: all its access rights are locked together.
	 */
	public boolean isLockable() {
		return this.lockable;
	}

	/** A retired role that may still be found in old databases and exports. */
	public boolean isObsolete() {
		return this == Role.PLATFORM;
	}

	/**
	 * The username of the generic account generated for this role, before any platform suffix. It is not the enum
	 * constant name, which is stored in databases and exports and must not change.
	 */
	public String genericAccountName() {
		return this == Role.COMPETITION_SUPERVISOR ? "supervisor" : name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Operates on every platform: never locked to the platform chosen at login and not asked to choose one. A property
	 * of the granted role, not of its access rights.
	 */
	public boolean coversAllPlatforms() {
		return this == Role.ADMIN || this == Role.COMPETITION_SUPERVISOR;
	}

	/**
	 * The access rights declared for this role. A role does not imply the access right of the same name unless it is
	 * declared here.
	 */
	public Set<Access> accesses() {
		return this.accesses;
	}
}
