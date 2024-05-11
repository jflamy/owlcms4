/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * This comparator is used for the technical meeting sheet. It is based on the registration category
 *
 * @author jflamy
 *
 */
public class RegistrationOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

	Logger logger = (Logger) LoggerFactory.getLogger(RegistrationOrderComparator.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Athlete lifter1, Athlete lifter2) {
		// logger.debug("comparing RegistrationOrderComparator");
		int compare = 0;

		// takes into account platform and group name so that groups are not mixed
		// together
		compare = compareGroupWeighInTime(lifter1, lifter2);
		if (compare != 0) {
			return compare;
		}

		if (!Competition.getCurrent().isDisplayByAgeGroup()) {
			compare = ageGroupRegistrationComparator.compare(lifter1.getAgeGroup(), lifter2.getAgeGroup());
			if (compare != 0) {
				traceComparison("RegistrationOrderComparator ageGroup", lifter1, lifter1.getAgeGroup(), lifter2,
				        lifter2.getAgeGroup(), compare);
				return Competition.getCurrent().isMasters() ? -compare : compare;
			}
		}

		Category a = lifter1.getCategory();
		Category b = lifter2.getCategory();
		compare = categoryRegistrationComparator.compare(a, b);
		if (compare != 0) {
			traceComparison("RegistrationOrderComparator category", lifter1, a, lifter2, b, compare);
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

	public static Comparator<AgeGroup> ageGroupRegistrationComparator = AgeGroup.registrationComparator;
	
	public static Comparator<Category> categoryRegistrationComparator = (category1, category2) -> {
		if (category2 == null) {
			return -1; // category1 is smaller than null -- null goes to the end;
		}

		int compare;

		compare = ObjectUtils.compare(category1.getCode(), category2.getCode());
		if (compare == 0) {
			// shortcut. identical codes are identical
			return compare;
		}

		compare = ObjectUtils.compare(category1.getGender(), category2.getGender());
		if (compare != 0) {
			// logger.debug("gender {} {} {} ", category1.getGender(), compare > 0 ? ">" : "<", category2.getGender());
			return compare;
		}

		compare = AgeGroup.registrationComparator.compare(category1.getAgeGroup(), category2.getAgeGroup());
		if (compare != 0) {
			// logger.debug("agegroup {} {} {} ", category1.getAgeGroup(), compare > 0 ? ">" : "<",
			// category2.getAgeGroup());
			return compare;
		}

		// same division, same gender, rank according to maximumWeight.
		Double value1 = category1.getMaximumWeight();
		Double value2 = category2.getMaximumWeight();
		compare = ObjectUtils.compare(value1, value2);
		return compare;
	};

	private void traceComparison(String where, Athlete lifter1, Object v1, Athlete lifter2, Object v2, int compare) {
		if (logger.isTraceEnabled()) {
			logger./**/warn("{} {}={} {} {}={} {}", where, lifter1.getLastName(), v1, (compare < 0 ? " < " : (compare == 0 ? "=" : " > ")),
			        lifter2.getLastName(), v2,
			        LoggerUtils.whereFrom());
		}
	}

}
