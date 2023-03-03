/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import app.owlcms.data.competition.Competition;

/**
 * The Enum Gender.
 */
public enum Gender {
	F, M, I;

	static Gender[] mfValueArray = new Gender[] { F, M };
	static Gender[] mfiValueArray = new Gender[] { F, M, I };

	public static Gender[] mfValues() {
		if (Competition.getCurrent().isGenderInclusive()) {
			return mfiValueArray;
		}
		return mfValueArray;
	}
}
