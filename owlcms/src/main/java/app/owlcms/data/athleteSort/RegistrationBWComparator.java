/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;

/**
 * This comparator is used for the technical meeting sheet. It is based on the
 * registration category
 *
 * @author jflamy
 *
 */
public class RegistrationBWComparator extends AbstractLifterComparator implements Comparator<Athlete> {

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Athlete lifter1, Athlete lifter2) {
		int compare = 0;

		compare = ObjectUtils.compare(lifter1.getPresumedOpenCategoryString(), lifter2.getPresumedOpenCategoryString(), true); // null weighed after
		if (compare != 0) {
			return compare;
		}
		
		compare = ObjectUtils.compare(lifter1.getCategory(), lifter2.getCategory(), true); // null weighed after
		if (compare != 0) {
			return compare;
		}
		
		compare = compareAgeGroup(lifter1, lifter2);
		if (compare != 0) {
			return Competition.getCurrent().isMasters() ? -compare : compare;
		}

		compare = compareEntryTotal(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		compare = compareLotNumber(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		compare = compareLastName(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		compare = compareFirstName(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		return compare;
	}

}
