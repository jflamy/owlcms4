/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import app.owlcms.data.athlete.Gender;

public class IWFCategoriesTest {

	@Test
	public void bodyWeightAtUpperLimitBelongsToThatCategory() {
		List<Category> men = categories(Gender.M, 60, 65, 70, 75, 85, 95, 110, 999);
		for (double limit : new double[] { 60, 65, 70, 75, 85, 95, 110 }) {
			assertEquals("body weight " + limit, limit, IWFCategories.findCategory(men, Gender.M, limit).getMaximumWeight(),
			        0.0);
		}
		List<Category> women = categories(Gender.F, 49, 53, 57, 61, 69, 77, 86, 999);
		for (double limit : new double[] { 49, 53, 57, 61, 69, 77, 86 }) {
			assertEquals("body weight " + limit, limit,
			        IWFCategories.findCategory(women, Gender.F, limit).getMaximumWeight(), 0.0);
		}
	}

	@Test
	public void bodyWeightAboveLowerLimitBelongsToNextCategory() {
		List<Category> men = categories(Gender.M, 60, 65, 70, 75, 85, 95, 110, 999);
		assertEquals(75.0, IWFCategories.findCategory(men, Gender.M, 70.01).getMaximumWeight(), 0.0);
		assertEquals(999.0, IWFCategories.findCategory(men, Gender.M, 110.01).getMaximumWeight(), 0.0);
	}

	private static List<Category> categories(Gender gender, double... maximumWeights) {
		List<Category> categories = new ArrayList<>();
		double minimumWeight = 0.0;
		for (double maximumWeight : maximumWeights) {
			categories.add(new Category(minimumWeight, maximumWeight, gender, true, 0, 0, 0, null, 0));
			minimumWeight = maximumWeight;
		}
		return categories;
	}
}
