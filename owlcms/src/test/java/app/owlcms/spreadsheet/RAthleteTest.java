/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.Category;

public class RAthleteTest {

	@Test
	public void testAppendMembershipMarkersUsesShortTokens() {
		assertEquals("M 73", RAthlete.appendMembershipMarkers("M 73", true, false));
		assertEquals("M 73/-T", RAthlete.appendMembershipMarkers("M 73", false, false));
		assertEquals("M 73/+MT", RAthlete.appendMembershipMarkers("M 73", true, true));
		assertEquals("M 73/-T,+MT", RAthlete.appendMembershipMarkers("M 73", false, true));
	}

	@Test
	public void testParseParticipationSpecAcceptsShortTokens() throws Exception {
		RAthlete.ParticipationSpec spec = RAthlete.parseParticipationSpec("M 73/-T,+MT");

		assertEquals("M 73", spec.categoryName);
		assertFalse(spec.teamMember);
		assertTrue(spec.mixedTeamMember);
	}

	@Test
	public void testParseParticipationSpecAcceptsLegacyTokens() throws Exception {
		RAthlete.ParticipationSpec spec = RAthlete.parseParticipationSpec("M 73/yEsTeAm,nOmIxEd");

		assertEquals("M 73", spec.categoryName);
		assertTrue(spec.teamMember);
		assertFalse(spec.mixedTeamMember);
	}

	@Test
	public void testWeightCategoriesSharingOnlyExclusiveLowerBoundaryAreDisjoint() {
		assertTrue(RAthlete.hasDisjointWeightCategories(categories(
				category(70D, 75D, 13, 17),
				category(65D, 70D, 13, 17))));
	}

	@Test
	public void testOverlappingWeightCategoriesAreNotDisjoint() {
		assertFalse(RAthlete.hasDisjointWeightCategories(categories(
				category(70D, 75D, 13, 17),
				category(65D, 71D, 13, 17))));
	}

	@Test
	public void testAgeCategoriesSharingInclusiveBoundaryAreNotDisjoint() {
		assertFalse(RAthlete.hasDisjointAgeCategories(categories(
				category(70D, 75D, 13, 17),
				category(70D, 75D, 17, 20))));
	}

	@Test
	public void testDisjointAgeCategoriesAreDetected() {
		assertTrue(RAthlete.hasDisjointAgeCategories(categories(
				category(70D, 75D, 13, 17),
				category(70D, 75D, 18, 20))));
	}

	@Test
	public void testThreeCategoriesRequireOneCommonIntersection() {
		assertTrue(RAthlete.hasDisjointWeightCategories(categories(
				category(60D, 75D, 13, 25),
				category(70D, 80D, 13, 25),
				category(75D, 90D, 13, 25))));
	}

	@Test
	public void testCellDisjointInBothDimensionsReportsBothConflicts() {
		Set<Category> cats = categories(
				category(70D, 75D, 13, 17),
				category(65D, 70D, 18, 20));
		assertTrue(RAthlete.hasDisjointWeightCategories(cats));
		assertTrue(RAthlete.hasDisjointAgeCategories(cats));
	}

	@Test
	public void testCompatibleCellIsDisjointInNeitherDimension() {
		Set<Category> cats = categories(
				category(70D, 75D, 13, 17),
				category(65D, 75D, 15, 20));
		assertFalse(RAthlete.hasDisjointWeightCategories(cats));
		assertFalse(RAthlete.hasDisjointAgeCategories(cats));
	}

	private Set<Category> categories(Category... categories) {
		return new LinkedHashSet<>(List.of(categories));
	}

	private Category category(double minimumWeight, double maximumWeight, int minAge, int maxAge) {
		AgeGroup ageGroup = new AgeGroup();
		ageGroup.setMinAge(minAge);
		ageGroup.setMaxAge(maxAge);
		Category category = new Category();
		category.setMinimumWeight(minimumWeight);
		category.setMaximumWeight(maximumWeight);
		category.setAgeGroup(ageGroup);
		return category;
	}
}