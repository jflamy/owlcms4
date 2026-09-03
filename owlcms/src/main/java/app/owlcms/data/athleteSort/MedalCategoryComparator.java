/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;

public class MedalCategoryComparator implements Comparator<Athlete> {

	private final boolean mastersAgeGroupOrder;

	public MedalCategoryComparator(Group group) {
		this.mastersAgeGroupOrder = RegistrationOrderComparator.useMastersAgeGroupOrder(group);
	}

	/** Global medal presentation order: gender (women first), configured championship order, then medaling sort code. */
	public static Comparator<Category> categoryMedalOrder() {
		return (a, b) -> {
			if (a == null || b == null) {
				return ObjectUtils.compare(a, b, true);
			}
			int compare = ObjectUtils.compare(a.getGender(), b.getGender(), true);
			if (compare != 0) {
				return compare;
			}
			compare = compareChampionships(a, b);
			return compare != 0 ? compare : Category.medalingComparator().compare(a, b);
		};
	}

	private static int compareChampionships(Category a, Category b) {
		Championship aChampionship = a.getAgeGroup() != null ? a.getAgeGroup().getChampionship() : null;
		Championship bChampionship = b.getAgeGroup() != null ? b.getAgeGroup().getChampionship() : null;
		return ObjectUtils.compare(aChampionship, bChampionship, true);
	}

	@Override
	public int compare(Athlete first, Athlete second) {
		Category firstCategory = first != null ? first.getCategory() : null;
		Category secondCategory = second != null ? second.getCategory() : null;
		if (firstCategory == null || secondCategory == null) {
			return ObjectUtils.compare(firstCategory, secondCategory, true);
		}

		int compare = ObjectUtils.compare(firstCategory.getGender(), secondCategory.getGender(), true);
		if (compare != 0) {
			return compare;
		}

		compare = compareChampionships(firstCategory, secondCategory);
		if (compare != 0) {
			return compare;
		}

		compare = RegistrationOrderComparator.compareMastersAgeGroupOrder(
		        first, second, this.mastersAgeGroupOrder);
		if (compare != 0) {
			return compare;
		}

		return Category.medalingComparator().compare(firstCategory, secondCategory);
	}
}
