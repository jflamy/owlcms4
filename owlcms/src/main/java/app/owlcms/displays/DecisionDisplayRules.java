/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays;

public final class DecisionDisplayRules {

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
