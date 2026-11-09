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

	/** Clear referee lights on Jury Control while a jury-only review vote is in progress (the jury box keeps them). */
	public static final boolean HIDE_REFEREES_PANEL = Boolean.TRUE;

	/** Clear referee lights on public boards during and after a jury-only review vote: only the jurors voted. */
	public static final boolean HIDE_REFEREES_DISPLAYS = Boolean.TRUE;

	/**
	 * When a review vote clears the middle referee indicators, Jury Control shows the original referee decision at the
	 * top right and keeps it there until the next clock starts. The jury box, which has no separate top line, gets the
	 * original referee lights back for a jury-only vote.
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
