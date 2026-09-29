package app.owlcms.access;

import java.util.EnumSet;
import java.util.Set;

/**
 * Roles known to the access control. Pages only reference base roles; grantable roles may be composite and expand to
 * base roles in {@link #expand()}.
 */
public enum Role {
	ADMIN(true, false, false, false),
	ADMIN_PAGES(false, false, false, true),
	PREPARATION(true, false, false, true),
	REGISTRATION(true, false, false, true),
	WEIGHIN(true, false, false, true),
	RESULTS(true, false, false, true),
	PLATFORM(true, true, false, false),
	ANNOUNCER(true, true, true, true),
	MARSHAL(true, true, true, true),
	TIMEKEEPER(true, true, true, true),
	TC(true, true, true, true),
	JURY(true, true, true, true),
	REFEREE(true, true, true, true),
	DISPLAYS(true, true, false, true);

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

	public Set<Role> expand() {
		return switch (this) {
			case ADMIN -> baseRoles();
			case RESULTS -> EnumSet.of(RESULTS, DISPLAYS);
			case PLATFORM -> EnumSet.of(ANNOUNCER, MARSHAL, TIMEKEEPER, TC, DISPLAYS);
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
