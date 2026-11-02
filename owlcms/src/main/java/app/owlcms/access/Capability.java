package app.owlcms.access;

import java.util.Set;

/** In-page functions and URLs: any one of the listed permissions. */
public enum Capability {
	/** The Security and Audit tab of the Administration page: access mode, IWF compliance check, shared passwords. */
	SECURITY_SETTINGS(Set.of(Permission.ADMIN_PAGES, Permission.COMPETITION_SUPERVISOR), false),
	/** Audit key initialization and audit integrity verification, on the Security and Audit tab: admin only. */
	AUDIT_KEYS(Set.of(Permission.ADMIN_PAGES), false),
	/** Registration, SBDE and database imports; see {@link ImportGate}. */
	IMPORT_DATA(Set.of(Permission.IMPORT), false),
	/**
	 * Database (JSON) and SBDE exports, and the export and H2 backup URLs: the Results, Competition Director,
	 * Competition Supervisor and External roles, admin.
	 */
	EXPORT_DATA(Set.of(Permission.RESULTS), false),
	/** The tracker's recovery checkpoint URL: the export plus the log files. */
	CHECKPOINT(Set.of(Permission.COMPETITION_SUPERVISOR), false),
	/** Keyboard input on a display page, on the display's platform. */
	DISPLAY_INPUT(Set.of(Permission.DISPLAYS), true);

	private final Set<Permission> permissions;
	private final boolean platformBound;

	Capability(Set<Permission> permissions, boolean platformBound) {
		this.permissions = permissions;
		this.platformBound = platformBound;
	}

	public PageRule asRule() {
		return PageRule.permissions(this.permissions, this.platformBound);
	}
}
