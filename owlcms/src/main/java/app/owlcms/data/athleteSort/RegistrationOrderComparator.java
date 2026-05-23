/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Logger;

/**
 * This comparator is used for the technical meeting sheet. It is based on the registration category
 *
 * @author jflamy
 *
 */
public class RegistrationOrderComparator extends AbstractLifterComparator implements Comparator<Athlete> {

	static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationOrderComparator.class);
	public static Comparator<AgeGroup> ageGroupRegistrationComparator = AgeGroup.registrationComparator;
	public static Comparator<Category> categoryRegistrationComparator = (category1, category2) -> {
		if (category1 == null && category2 == null) {
			return 0;
		} else if (category2 == null) {
			return -1; // category1 is smaller than null -- category2 goes to the end;
		} else if (category1 == null) {
			return 1; // category1 is null, goes to the end
		}

		int compare;

		compare = ObjectUtils.compare(category1.getCode(), category2.getCode());
		if (compare == 0) {
			// shortcut. identical codes are identical
			return compare;
		}

		compare = ObjectUtils.compare(category1.getGender(), category2.getGender());
		if (compare != 0) {
			traceComparison("categoryRegistrationComparator gender", category1, category1.getGender(), category2, category2.getGender(), compare);
			return compare;
		}

		// same division, same gender, rank according to maximumWeight.
		Double value1 = category1.getMaximumWeight();
		Double value2 = category2.getMaximumWeight();
		compare = ObjectUtils.compare(value1, value2);
		if (compare != 0) {
			traceComparison("categoryRegistrationComparator maximum weight", category1, category1.getMaximumWeight(), category2, category2.getMaximumWeight(),
			        compare);
			return compare;
		}

		if (!Competition.getCurrent().isDisplayByAgeGroup() && Config.getCurrent().featureSwitch("bwClassThenAgeGroup")) {
			// for scoreboard readability, we group by age group within the bodyweight category.
			// (used in South America)
			compare = AgeGroup.registrationComparator.compare(category1.getAgeGroup(), category2.getAgeGroup());
			if (compare != 0) {
				traceComparison("categoryRegistrationComparator agegroup", category1, category1.getAgeGroup(), category2, category2.getAgeGroup(), compare);
				return compare;
			}
		}

		if (Competition.getCurrent().isDisplayByAgeGroup()) {
			// multiple age groups with same boundaries
			return ObjectUtils.compare(category1.getAgeGroup().getCode(), category2.getAgeGroup().getCode());
		}
		return compare;
	};
	
	public static Comparator<Category> categoryReportOrderComparator = (category1, category2) -> {
		if (category1 == null && category2 == null) {
			return 0;
		} else if (category2 == null) {
			return -1; // category1 is smaller than null -- category2 goes to the end;
		} else if (category1 == null) {
			return 1; // category1 is null, goes to the end
		}

		int compare;

		compare = ObjectUtils.compare(category1.getCode(), category2.getCode());
		if (compare == 0) {
			// shortcut. identical codes are identical
			return compare;
		}

		compare = ObjectUtils.compare(category1.getGender(), category2.getGender());
		if (compare != 0) {
			traceComparison("categoryDisplayComparator gender", category1, category1.getGender(), category2, category2.getGender(), compare);
			return compare;
		}

		compare = AgeGroup.registrationComparator.compare(category1.getAgeGroup(), category2.getAgeGroup());
		if (compare != 0) {
			traceComparison("categoryDisplayComparator agegroup", category1, category1.getAgeGroup(), category2, category2.getAgeGroup(), compare);
			return compare;
		}

		// same division, same gender, rank according to maximumWeight.
		Double value1 = category1.getMaximumWeight();
		Double value2 = category2.getMaximumWeight();
		compare = ObjectUtils.compare(value1, value2);
		if (compare != 0) {
			traceComparison("categoryDisplayComparator maximum weight", category1, category1.getMaximumWeight(), category2, category2.getMaximumWeight(),
			        compare);
			return compare;
		}

		// multiple age groups with same boundaries
		return ObjectUtils.compare(category1.getAgeGroup().getCode(), category2.getAgeGroup().getCode());
	};
	
	public static Comparator<Athlete> athleteReportOrderComparator = (lifter1, lifter2) -> {
		int compare;

		Category a = lifter1.getCategory();
		Category b = lifter2.getCategory();
		compare = categoryReportOrderComparator.compare(a, b);
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
	};

	public static boolean useMastersAgeGroupOrder(Group group) {
		if (isMastersCompetition()) {
			return true;
		}
		return group != null && group.isMasters();
	}

	public static boolean useMastersAgeGroupOrder(Athlete lifter1, Athlete lifter2) {
		if (isMastersCompetition()) {
			return true;
		}
		return isMastersSession(lifter1) && isMastersSession(lifter2);
	}

	public static int compareMastersAgeGroupOrder(Athlete lifter1, Athlete lifter2) {
		return compareMastersAgeGroupOrder(lifter1, lifter2, useMastersAgeGroupOrder(lifter1, lifter2));
	}

	public static int compareMastersAgeGroupOrder(Athlete lifter1, Athlete lifter2, boolean mastersAgeGroupOrder) {
		if (!mastersAgeGroupOrder) {
			return 0;
		}
		AgeGroup ageGroup1 = lifter1 != null ? lifter1.getAgeGroup() : null;
		AgeGroup ageGroup2 = lifter2 != null ? lifter2.getAgeGroup() : null;
		return -ageGroupRegistrationComparator.compare(ageGroup1, ageGroup2);
	}

	private static boolean isMastersSession(Athlete lifter) {
		Group group = lifter != null ? lifter.getGroup() : null;
		return group != null && group.isMasters();
	}

	private static boolean isMastersCompetition() {
		Competition competition = Competition.getCurrent();
		return competition != null && competition.isMasters();
	}
	
	public static Comparator<Athlete> athleteRegistrationOrderComparator = (lifter1, lifter2) -> {
		int compare;
		// normally part of the same group when this is called, but never too careful.
		boolean mastersAgeGroupOrder = useMastersAgeGroupOrder(lifter1, lifter2);
		
		if (Competition.getCurrent().isDisplayByAgeGroup() || mastersAgeGroupOrder) {
			compare = ageGroupRegistrationComparator.compare(lifter1.getAgeGroup(), lifter2.getAgeGroup());
			if (compare != 0) {
				int ageGroupCompare = mastersAgeGroupOrder ? -compare : compare;
				traceComparison("ageGroupRegistrationComparator ageGroup", lifter1, lifter1.getAgeGroup(), lifter2,
				        lifter2.getAgeGroup(), ageGroupCompare);
				return ageGroupCompare;
			}
		} else {
			compare = ObjectUtils.compare(lifter1.getGender(), lifter2.getGender());
			if (compare != 0) {
				traceComparison("athleteRegistrationOrderComparator gender", lifter1, lifter1.getAgeGroup(), lifter2,
				        lifter2.getAgeGroup(), compare);
				return compare;
			}
		}

		Category a = lifter1.getCategory();
		Category b = lifter2.getCategory();
		compare = categoryRegistrationComparator.compare(a, b);
		if (compare != 0) {
			traceComparison("categoryRegistrationComparator category", lifter1, a, lifter2, b, compare);
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
	};
	public static Comparator<Athlete> athleteSessionRegistrationOrderComparator = (lifter1, lifter2) -> {
		return AbstractLifterComparator.athleteSessionComparator
			.thenComparing(athleteRegistrationOrderComparator)
			.compare(lifter1, lifter2);
	};

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Athlete lifter1, Athlete lifter2) {
		return athleteRegistrationOrderComparator.compare(lifter1, lifter2);
	}
}
