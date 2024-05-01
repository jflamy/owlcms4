/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;

/**
 * This comparator is used for the technical meeting sheet. It is based on the registration category
 *
 * @author jflamy
 *
 */
public class RegistrationOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Athlete lifter1, Athlete lifter2) {
		//logger.debug("comparing RegistrationOrderComparator");
		int compare = 0;

		// takes into account platform and group name so that groups are not mixed
		// together
		compare = compareGroupWeighInTime(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		if (Competition.getCurrent().isMasters()) {
			compare = AgeGroup.registrationComparator.compare(lifter1.getAgeGroup(), lifter2.getAgeGroup());
			if (compare != 0) {
				return -compare;
			}
		}

		Category a = lifter1.getCategory();
		Category b = lifter2.getCategory();
		compare = registrationComparator.compare(a, b);
		if (compare != 0) {
			//logger.debug("category {} {} {} ", a, compare > 0 ? ">" : "<", b);
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

	public static Comparator<Category> registrationComparator = (category1, category2) -> {
		if (category2 == null) {
			return -1; // category1 is smaller than null -- null goes to the end;
		}
		
		int compare;
		
		compare = ObjectUtils.compare(category1.getCode(), category2.getCode());
		if (compare == 0) {
			// shortcut.  identical codes are identical
			return compare;
		}
		
		compare = ObjectUtils.compare(category1.getGender(), category2.getGender());
		if (compare != 0) {
			//logger.debug("gender {} {} {} ", category1.getGender(), compare > 0 ? ">" : "<", category2.getGender());
			return compare;
		}

		compare = AgeGroup.registrationComparator.compare(category1.getAgeGroup(), category2.getAgeGroup());
		if (compare != 0) {
			//logger.debug("agegroup {} {} {} ", category1.getAgeGroup(), compare > 0 ? ">" : "<", category2.getAgeGroup());
			return compare;
		}

		// same division, same gender, rank according to maximumWeight.
		Double value1 = category1.getMaximumWeight();
		Double value2 = category2.getMaximumWeight();
		compare = ObjectUtils.compare(value1, value2);
		return compare;
	};

}
