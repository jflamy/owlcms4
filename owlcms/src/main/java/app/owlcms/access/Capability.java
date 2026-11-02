package app.owlcms.access;

import java.util.Set;

/** In-page functions that depend on roles rather than on navigation. */
public enum Capability {
	/** The Security and Audit tab of the Administration page: access mode, IWF compliance check, shared passwords. */
	SECURITY_SETTINGS(Set.of(Role.ADMIN_PAGES, Role.COMPETITION_SUPERVISOR), false),
	/** Audit key initialization and audit integrity verification, on the Security and Audit tab: admin only. */
	AUDIT_KEYS(Set.of(Role.ADMIN_PAGES), false),
	/** Registration, SBDE and database imports; see {@link ImportGate}. */
	IMPORT_DATA(Set.of(Role.IMPORT), false),
	/** Database (JSON) and SBDE exports, and the export and H2 backup URLs: the Results, Competition Director, Competition Supervisor, External roles, admin. */
	EXPORT_DATA(Set.of(Role.RESULTS), false),
	/** The tracker's recovery checkpoint URL: the export plus the log files. */
	CHECKPOINT(Set.of(Role.COMPETITION_SUPERVISOR), false),
	/** Keyboard input on a display page, on the display's platform. */
	DISPLAY_INPUT(Set.of(Role.DISPLAYS), true);

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
