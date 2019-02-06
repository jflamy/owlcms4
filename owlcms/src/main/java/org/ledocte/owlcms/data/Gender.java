/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data;

public enum Gender {
    F, M, UNKOWN;

    @Override
    public String toString() {
    	if (this == UNKOWN) {
    		return ("?");
    	}
        return name().toUpperCase();
    }
}
