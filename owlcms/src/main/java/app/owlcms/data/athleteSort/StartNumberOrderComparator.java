/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Athlete;

/**
 * This comparator is used for the technical meeting sheet. It is based on the registration category
 *
 * @author jflamy
 *
 */
public class StartNumberOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Athlete lifter1, Athlete lifter2) {
		int compare = 0;

		// nulls last
		if (lifter1 == null && lifter2 == null) {
			return 0;
		} else if (lifter1 == null) {
			return 1;
		} else if (lifter2 == null) {
			return -1;
		}

		compare = ObjectUtils.compare(lifter1.getStartNumber(), lifter2.getStartNumber(), true);

		return compare;
	}

}
