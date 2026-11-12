package app.owlcms.access;

import java.util.Set;

import app.owlcms.data.config.FeatureSwitch;

/** In-page functions and URLs: any one of the listed access rights. */
public enum Permission {
	/** The Security and Audit tab of the Administration page: access mode, IWF compliance check, shared passwords. */
	SECURITY_SETTINGS(Set.of(Access.ADMINISTRATOR, Access.SUPERVISOR), false),
	/** Audit key initialization and audit integrity verification, on the Security and Audit tab: admin only. */
	AUDIT_KEYS(Set.of(Access.ADMINISTRATOR), false),
	/** Registration, SBDE and database imports; see {@link ImportGate}. */
	IMPORT_DATA(Set.of(Access.REGISTRATION, Access.SUPERVISOR), false),
	METADATA_EDITING(Set.of(Access.PREPARATION, Access.REGISTRATION, Access.WEIGHIN,
	        Access.SUPERVISOR), false),
	ATHLETE_CARD_EDITING(Set.of(Access.ANNOUNCER, Access.MARSHAL, Access.PREPARATION,
	        Access.SUPERVISOR), true),
	/**
	 * The "Allow results editing" box of the athlete card starts ticked. The other card editors find it unticked and
	 * may tick it.
	 */
	ATHLETE_RESULTS_EDITING(Set.of(Access.ANNOUNCER, Access.PREPARATION, Access.SUPERVISOR), true),
	ATHLETE_TIMER(Set.of(Access.ANNOUNCER, Access.TIMEKEEPER, Access.MARSHAL,
	        Access.SUPERVISOR), true),
	BREAK_TIMER(Set.of(Access.ANNOUNCER, Access.TIMEKEEPER, Access.MARSHAL,
	        Access.SUPERVISOR), true),
	DECISION_BUTTONS(Set.of(Access.ANNOUNCER, Access.SUPERVISOR), true),
	/**
	 * Database (JSON) and SBDE exports, and the export and H2 backup URLs: the Results, Competition Director,
	 * Competition Supervisor and External roles, admin.
	 */
	EXPORT_DATA(Set.of(Access.RESULTS), false),
	/**
	 * V1 database (JSON) exports, from the buttons and the V1 export URLs. V1 exports are never signed; the
	 * {@code noV1Export} feature switch withdraws them.
	 */
	EXPORT_V1(Set.of(Access.RESULTS), false) {
		@Override
		public boolean isAvailable() {
			return !FeatureSwitch.NO_V1_EXPORT.isEffective();
		}
	},
	/** The tracker's recovery checkpoint URL: the export plus the log files. */
	CHECKPOINT(Set.of(Access.SUPERVISOR), false),
	/** Keyboard input on a display page, on the display's platform. */
	DISPLAY_INPUT(Set.of(Access.DISPLAYS), true);

	private final Set<Access> accesses;
	private final boolean platformBound;

	Permission(Set<Access> accesses, boolean platformBound) {
		this.accesses = accesses;
		this.platformBound = platformBound;
	}

	/** @return false when the configuration withholds the permission from everyone, whatever their access rights */
	public boolean isAvailable() {
		return true;
	}

	public Set<Access> accesses() {
		return this.accesses;
	}

	public PageRule asRule() {
		return PageRule.accesses(this.accesses, this.platformBound);
	}
}
