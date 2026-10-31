package app.owlcms.access;

import java.util.Set;

/** In-page functions that depend on roles rather than on navigation. */
public enum Capability {
	BREAK_MANAGEMENT(Set.of(Role.ANNOUNCER, Role.TIMEKEEPER, Role.MARSHAL), true),
	/** The Security and Audit tab of the Administration page; the Competition Supervisor sees only the System tab. */
	SECURITY_SETTINGS(Set.of(Role.ADMIN_PAGES), false);

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
