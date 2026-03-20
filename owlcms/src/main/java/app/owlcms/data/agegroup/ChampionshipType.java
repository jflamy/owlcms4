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
	DEFAULT;

	/** True for MASTERS championships. */
	public boolean isMasters() {
		return this == MASTERS;
	}

	/** True for youth-style championships. */
	public boolean isU() {
		return this == U;
	}

	/** True for IWF championships. */
	public boolean isIWF() {
		return this == IWF;
	}

}
