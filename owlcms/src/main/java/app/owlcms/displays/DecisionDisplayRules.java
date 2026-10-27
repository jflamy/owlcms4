/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays;

public final class DecisionDisplayRules {

	// The switches are initialized from Boolean.TRUE so that they are not compile-time constants; otherwise the
	// compiler flags the branches for the other setting as dead code.

	/** Clear referee lights on Jury Control and the jury box while a jury-only review vote is in progress. */
	public static final boolean HIDE_REFEREES_PANEL = Boolean.TRUE;

	/** Clear referee lights on attempt boards and scoreboards while a jury-only review vote is in progress. */
	public static final boolean HIDE_REFEREES_DISPLAYS = Boolean.TRUE;

	/**
	 * When a jury-only review vote is revealed, Jury Control shows the original referee decision at the top right
	 * instead of in the referee decisions area, as with the seven-person panel. The jury box, which has no such area,
	 * gets the original referee lights back.
	 */
	public static final boolean SHOW_ORIGINAL_DECISIONS = Boolean.TRUE;

	private DecisionDisplayRules() {
	}

	public static boolean showDecisionSection(boolean publicDisplay, boolean videoDisplay,
	        boolean showOnWarmupDisplays) {
		return publicDisplay || videoDisplay || showOnWarmupDisplays;
	}

	public static boolean showJuryDecisionsOnScoreboard(int jurySize) {
		return jurySize > 0;
	}

	public static boolean showJuryDecisionsOnAttemptBoard(int jurySize, boolean enabled) {
		return jurySize > 0 && enabled;
	}
}
