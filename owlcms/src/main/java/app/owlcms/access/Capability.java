package app.owlcms.access;

import java.util.Set;

/** In-page functions that depend on roles rather than on navigation. */
public enum Capability {
	/** The Security and Audit tab of the Administration page; the Competition Supervisor sees only the System tab. */
	SECURITY_SETTINGS(Set.of(Role.ADMIN_PAGES), false),
	/** Registration, SBDE and database imports; see {@link ImportGate}. */
	IMPORT_DATA(Set.of(Role.IMPORT), false),
	/** Database (JSON) and SBDE exports: the Results, Competition Director, Competition Supervisor, External roles, admin. */
	EXPORT_DATA(Set.of(Role.RESULTS), false);

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
