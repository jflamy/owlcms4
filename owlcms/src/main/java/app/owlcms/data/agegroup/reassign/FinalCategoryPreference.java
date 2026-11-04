/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup.reassign;

import java.util.Comparator;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.Category;

/**
 * Registration preference (specificity, then medal order, as {@code doFindEligibleCategories} and
 * {@code Athlete.bestMatch} apply it) computed against the final category counts. While a save is in flight,
 * {@code AgeGroup.getCategories()} still lists categories marked for deletion, and both
 * {@code Category.specificityComparator} and the medaling sort code read that size. The overrides map carries the
 * kept-and-added count for such an age group; every other age group uses its current count.
 */
public final class FinalCategoryPreference implements Comparator<Category> {

	private final Map<Long, Integer> categoryCountByAgeGroupId;

	public FinalCategoryPreference(Map<Long, Integer> categoryCountByAgeGroupId) {
		this.categoryCountByAgeGroupId = categoryCountByAgeGroupId != null ? categoryCountByAgeGroupId : Map.of();
	}

	@Override
	public int compare(Category a, Category b) {
		if (a == null || b == null) {
			return ObjectUtils.compare(a, b, true);
		}
		AgeGroup aAgeGroup = a.getAgeGroup();
		AgeGroup bAgeGroup = b.getAgeGroup();
		if (aAgeGroup == null || bAgeGroup == null) {
			return ObjectUtils.compare(aAgeGroup, bAgeGroup, true);
		}
		int compare = ObjectUtils.compare(aAgeGroup.getGender(), bAgeGroup.getGender());
		if (compare != 0) {
			return compare;
		}

		int aDelta = aAgeGroup.getMaxAge() - aAgeGroup.getMinAge();
		int bDelta = bAgeGroup.getMaxAge() - bAgeGroup.getMinAge();
		compare = Integer.compare(aDelta, bDelta);
		if (compare != 0) {
			return compare;
		}

		// more categories comes first (an age group with no body-weight categories is less specific)
		compare = -Integer.compare(countOf(aAgeGroup), countOf(bAgeGroup));
		if (compare != 0) {
			return compare;
		}

		return ObjectUtils.compare(medalingSortCode(a), medalingSortCode(b));
	}

	private int countOf(AgeGroup ageGroup) {
		Integer override = ageGroup.getId() != null ? this.categoryCountByAgeGroupId.get(ageGroup.getId()) : null;
		return override != null ? override : ageGroup.getCategories().size();
	}

	private String medalingSortCode(Category category) {
		AgeGroup ageGroup = category.getAgeGroup();
		String agName = ageGroup != null ? ageGroup.getName() : null;
		if (agName == null || agName.isEmpty()) {
			return "zzzz" + "_" + category.getGender() + category.getSortCodeLimitString();
		}
		return agName + "_" + ageFirstSortCode(ageGroup) + "_" + category.getSortCodeLimitString();
	}

	/** {@code AgeGroup.getAgeFirstSortCode} with the final category count instead of the live collection size. */
	private String ageFirstSortCode(AgeGroup ageGroup) {
		Integer maxAge = ageGroup.getMaxAge();
		Integer minAge = ageGroup.getMinAge();
		String core = String.format("%03d", maxAge)
		        + "_" + String.format("%03d", maxAge >= 900 ? maxAge : minAge)
		        + "_" + String.format("%03d", 999 - countOf(ageGroup));
		return core + "_" + ageGroup.getGender().ordinal();
	}
}
