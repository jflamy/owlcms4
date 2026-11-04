/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Qualifying-total checks at registration, kept Vaadin-free so the rule can be unit tested and
 * reused by both the athlete form and the spreadsheet import.
 */
public final class RegistrationQtHelper {

	private RegistrationQtHelper() {
	}

	/**
	 * Categories whose qualifying total exceeds the entry total, in encounter order.
	 * Every selected category is checked, not only the registration category (spec §2).
	 */
	public static Set<Category> categoriesFailingQt(Collection<Category> selected, Integer entryTotal) {
		Set<Category> failing = new LinkedHashSet<>();
		if (selected == null) {
			return failing;
		}
		for (Category c : selected) {
			if (c != null && c.requiresEntryTotalConfirmation(entryTotal)) {
				failing.add(c);
			}
		}
		return failing;
	}

	/** Category fails only the qualifying total: everything else about the athlete fits. */
	public static boolean failsOnlyQt(Category c, Integer entryTotal, Integer athleteAge) {
		if (c == null || !c.requiresEntryTotalConfirmation(entryTotal)) {
			return false;
		}
		return ageFits(c, athleteAge);
	}

	/** Category fails something other than the qualifying total. */
	public static boolean failsOtherThanQt(Category c, Integer entryTotal, Integer athleteAge) {
		if (c == null) {
			return false;
		}
		if (!ageFits(c, athleteAge)) {
			return true;
		}
		return false;
	}

	public static boolean ageFits(Category c, Integer athleteAge) {
		if (athleteAge == null || c.getAgeGroup() == null) {
			return true;
		}
		return athleteAge >= c.getAgeGroup().getMinAge() && athleteAge <= c.getAgeGroup().getMaxAge();
	}
}
