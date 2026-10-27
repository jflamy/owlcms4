/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import app.owlcms.data.athleteSort.Ranking;

/** Effective competition rules while the TCRR compliance feature is enabled. */
public final class TcrrCompliance {

	public static final int JURY_SIZE = 4;
	public static final int TEAM_POINTS_FIRST = 28;
	public static final int TEAM_POINTS_SECOND = 25;
	public static final int TEAM_POINTS_THIRD = 23;

	private TcrrCompliance() {
	}

	public static boolean isEnabled() {
		return FeatureSwitch.TCRR_COMPLIANCE.isEffective();
	}

	public static Ranking bestAthleteScoring(Ranking configured) {
		return bestAthleteScoring(isEnabled(), configured);
	}

	public static Ranking bestAthleteScoring(boolean enabled, Ranking configured) {
		return enabled ? Ranking.GAMX : configured;
	}

	public static boolean enforce20kgRule(boolean configured) {
		return enforce20kgRule(isEnabled(), configured);
	}

	public static boolean enforce20kgRule(boolean enabled, boolean configured) {
		return enabled || configured;
	}

	public static boolean imwaRules(boolean configured) {
		return imwaRules(isEnabled(), configured);
	}

	public static boolean imwaRules(boolean enabled, boolean configured) {
		return !enabled && configured;
	}

	public static int jurySize(Integer configured) {
		return jurySize(isEnabled(), configured);
	}

	public static int jurySize(boolean enabled, Integer configured) {
		return enabled ? JURY_SIZE : configured != null ? configured : 0;
	}

	public static boolean alternativeLiftingOrder(boolean configured) {
		return alternativeLiftingOrder(isEnabled(), configured);
	}

	public static boolean alternativeLiftingOrder(boolean enabled, boolean configured) {
		return !enabled && configured;
	}

	public static int teamPointsFirst(Integer configured) {
		return teamPointsFirst(isEnabled(), configured);
	}

	public static int teamPointsFirst(boolean enabled, Integer configured) {
		return enabled ? TEAM_POINTS_FIRST : configured != null ? configured : 0;
	}

	public static int teamPointsSecond(Integer configured) {
		return teamPointsSecond(isEnabled(), configured);
	}

	public static int teamPointsSecond(boolean enabled, Integer configured) {
		return enabled ? TEAM_POINTS_SECOND : configured != null ? configured : 0;
	}

	public static int teamPointsThird(Integer configured) {
		return teamPointsThird(isEnabled(), configured);
	}

	public static int teamPointsThird(boolean enabled, Integer configured) {
		return enabled ? TEAM_POINTS_THIRD : configured != null ? configured : 0;
	}
}
