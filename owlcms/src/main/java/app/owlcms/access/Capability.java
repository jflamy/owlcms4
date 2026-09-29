package app.owlcms.access;

import java.util.Set;

/** In-page functions that depend on roles rather than on navigation. */
public enum Capability {
	BREAK_MANAGEMENT(Set.of(Role.ANNOUNCER, Role.TIMEKEEPER, Role.MARSHAL), true);

	private final Set<Role> roles;
	private final boolean platformBound;

	Capability(Set<Role> roles, boolean platformBound) {
		this.roles = roles;
		this.platformBound = platformBound;
	}

	public PageRule asRule() {
		return PageRule.roles(this.roles, this.platformBound);
	}
}
