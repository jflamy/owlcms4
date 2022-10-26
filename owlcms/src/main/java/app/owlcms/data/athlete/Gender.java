/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

/**
 * The Enum Gender.
 */
public enum Gender {
    F, M;

    static Gender[] mfValueArray = new Gender[] { F, M };

    public static Gender[] mfValues() {
        return mfValueArray;
    }
}
