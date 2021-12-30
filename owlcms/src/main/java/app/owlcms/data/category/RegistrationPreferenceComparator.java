/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.agegroup.AgeGroup;

/**
 * When several categories are possible for an athlete, this class returns the preferred choice.
 *
 * Given a 36 year old athlete, they could be
 * <ul>
 * <li>M35 (35-39)
 * <li>O21 (21+) or
 * <li>SR (15+)
 * </ul>
 * in that order of preference if all 3 categories are active. The athlete would be placed as a M35 by default, and the
 * choice can be overriden.
 *
 * Given a 15 year old athlete they could be
 * <ul>
 * <li>U15 (13-15)
 * <li>JR (15-20) or
 * <li>SR (15+)
 * </ul>
 * in that order of preference. Normally youth age groups would not be used in addition to JR because of the ambiguity,
 * but JR and SR could be used at the same time. The lifter would be shown on the boards as JR.
 *
 */
public class RegistrationPreferenceComparator implements Comparator<Category> {

    @Override
    public int compare(Category c1, Category c2) {
        // null is larger -- will show at the end
        if (c1 == null && c2 == null) {
            return 0;
        }
        if (c1 == null && c2 != null) {
            return 1;
        }
        if (c1 != null && c2 == null) {
            return -1;
        }

        AgeGroup ag1 = c1.getAgeGroup();
        AgeDivision ad1 = (ag1 != null ? ag1.getAgeDivision() : null);
        AgeGroup ag2 = c2.getAgeGroup();
        AgeDivision ad2 = (ag2 != null ? ag2.getAgeDivision() : null);

        int compare = 0;
        compare = ObjectUtils.compare(c1.getGender(), c2.getGender());
        if (compare != 0) {
            return compare;
        }

        // age divisions are in registration preference order
        // U before M before OLY before IWF before DEFAULT
        compare = ad1.compareTo(ad2);
        if (compare != 0) {
            return compare;
        }

        // athlete will be placed in youngest age group by default
        compare = Integer.compare(ag1.getMinAge(), ag2.getMinAge());
        if (compare != 0) {
            return compare;
        }

        // same minimum age, listed in most specific age category
        compare = Integer.compare(ag1.getMaxAge(), ag2.getMaxAge());
        if (compare != 0) {
            return compare;
        }

        // compare age divisions -- grasping at straws to get a total order.
        compare = ad1.compareTo(ad2);
        if (compare != 0) {
            return compare;
        }

        // compare max body weights
        compare = Double.compare(c1.getMaximumWeight(), c2.getMaximumWeight());
        if (compare != 0) {
            return compare;
        }

        return 0;
    }

}
