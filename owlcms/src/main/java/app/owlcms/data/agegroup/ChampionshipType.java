/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

public enum ChampionshipType {

    /* 35+ (30+ in some federations) */
	MASTERS,

    /* age groups, used for anything other than MASTERS, IWF and DEFAULT */
	U,

    /* Standard IWF -- there is a ROBI for these age groups */
	IWF,

    /* All Ages: All bodyweight categories are present, no restriction on age */
	DEFAULT,

    /* Like U, but with explicit mixed team participations */
	U_MIXED,

    /* Like MASTERS, but with explicit mixed team participations */
	MASTERS_MIXED,

    /* Like IWF, but with explicit mixed team participations */
	IWF_MIXED;

	/** True for MASTERS and MASTERS_MIXED. */
	public boolean isMasters() {
		return this == MASTERS || this == MASTERS_MIXED;
	}

	/** True for U and U_MIXED. */
	public boolean isU() {
		return this == U || this == U_MIXED;
	}

	/** True for IWF and IWF_MIXED. */
	public boolean isIWF() {
		return this == IWF || this == IWF_MIXED;
	}

	/** True for U_MIXED, MASTERS_MIXED and IWF_MIXED (explicit mixed team membership). */
	public boolean isMixed() {
		return this == U_MIXED || this == MASTERS_MIXED || this == IWF_MIXED;
	}

}
