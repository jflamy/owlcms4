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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.jpa.JPAService;

public class RAthleteTest {

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@After
	public void resetExplicitTeams() {
		setExplicitTeams(false);
	}

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
	public void testParseParticipationSpecDefaultsToTeamMemberWhenImplicit() throws Exception {
		assertTrue(RAthlete.parseParticipationSpec("M 73").teamMember);
		assertTrue(RAthlete.parseParticipationSpec("M 73/+MT").teamMember);
	}

	@Test
	public void testParseParticipationSpecDefaultsToNoTeamWhenExplicitTeams() throws Exception {
		setExplicitTeams(true);

		assertFalse(RAthlete.parseParticipationSpec("M 73").teamMember);
		assertFalse(RAthlete.parseParticipationSpec("M 73/+MT").teamMember);
		assertTrue(RAthlete.parseParticipationSpec("M 73/+T").teamMember);
		assertTrue(RAthlete.parseParticipationSpec("M 73/YesTeam,+MT").teamMember);
		assertFalse(RAthlete.parseParticipationSpec("M 73/-T").teamMember);
	}

	@Test
	public void testMembershipMarkersRoundTripWithEitherSwitchSetting() throws Exception {
		for (boolean explicitTeams : new boolean[] { false, true }) {
			setExplicitTeams(explicitTeams);
			for (boolean teamMember : new boolean[] { false, true }) {
				for (boolean mixedTeamMember : new boolean[] { false, true }) {
					String exported = RAthlete.appendMembershipMarkers("M 73", teamMember, mixedTeamMember);
					RAthlete.ParticipationSpec imported = RAthlete.parseParticipationSpec(exported);
					assertEquals(exported, "M 73", imported.categoryName);
					assertEquals(exported, teamMember, imported.teamMember);
					assertEquals(exported, mixedTeamMember, imported.mixedTeamMember);
				}
			}
		}
	}

	@Test
	public void testExplicitExportIncludesTeamOptIn() {
		setExplicitTeams(true);
		assertEquals("M 73/+T", RAthlete.appendMembershipMarkers("M 73", true, false));
		assertEquals("M 73/+T,+MT", RAthlete.appendMembershipMarkers("M 73", true, true));
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

	private static void setExplicitTeams(boolean enabled) {
		Config config = Config.getCurrent();
		config.setFeatureSwitchValue(FeatureSwitch.EXPLICIT_TEAMS, enabled);
		Config.setCurrent(config);
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