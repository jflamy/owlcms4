/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.List;

public enum ChampionshipType {

    /* 35+ (30+ in some federations) */
	MASTERS,

	/* age groups, used for anything other than MASTERS and DEFAULT */
	U,

	/* Legacy value retained for older databases; behaves like U. */
	IWF,

    /* All Ages: All bodyweight categories are present, no restriction on age */
	DEFAULT;

	private static final List<ChampionshipType> SELECTABLE_VALUES = List.of(MASTERS, U, DEFAULT);

	public static List<ChampionshipType> selectableValues() {
		return SELECTABLE_VALUES;
	}

	public static ChampionshipType normalizeLegacy(ChampionshipType type) {
		return type == IWF ? U : type;
	}

	public static ChampionshipType normalizeOrDefault(ChampionshipType type) {
		ChampionshipType normalized = normalizeLegacy(type);
		return normalized != null ? normalized : U;
	}

	public String labelKey() {
		return "Championship.Type." + normalizeOrDefault(this).name();
	}

	/** True for MASTERS championships. */
	public boolean isMasters() {
		return this == MASTERS;
	}

	/** True for youth-style championships. */
	public boolean isU() {
		return normalizeOrDefault(this) == U;
	}

}
