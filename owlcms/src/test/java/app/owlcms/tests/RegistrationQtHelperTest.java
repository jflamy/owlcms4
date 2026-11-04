/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.RegistrationQtHelper;

public class RegistrationQtHelperTest {

	@Test
	public void failingQtListsEverySelectedCategoryNotJustTheFirst() {
		Category open = category("Open", 15, 999, 95, 0);
		Category mastersLowQt = category("M45", 45, 49, 95, 100);
		Category mastersHighQt = category("M45", 45, 49, 102, 300);

		assertEquals(Set.of(), RegistrationQtHelper.categoriesFailingQt(List.of(open, mastersLowQt), 245));
		assertEquals(Set.of(mastersHighQt),
		        RegistrationQtHelper.categoriesFailingQt(List.of(open, mastersHighQt), 245));
		assertEquals(Set.of(mastersLowQt, mastersHighQt),
		        RegistrationQtHelper.categoriesFailingQt(List.of(mastersLowQt, mastersHighQt), 50));
		assertEquals(Set.of(), RegistrationQtHelper.categoriesFailingQt(null, 50));
	}

	@Test
	public void failsOnlyQtRequiresTheAgeToFit() {
		Category masters = category("M45", 45, 49, 95, 300);
		assertTrue(RegistrationQtHelper.failsOnlyQt(masters, 245, 46));
		assertFalse("age failure is not a QT-only failure", RegistrationQtHelper.failsOnlyQt(masters, 245, 42));
		assertFalse("meeting the QT is not a failure", RegistrationQtHelper.failsOnlyQt(masters, 300, 46));
		assertFalse(RegistrationQtHelper.failsOnlyQt(null, 245, 46));
	}

	@Test
	public void missingOrZeroEntryTotalFailsAPositiveQt() {
		Category masters = category("M45", 45, 49, 95, 300);
		assertTrue(RegistrationQtHelper.failsOnlyQt(masters, null, 46));
		assertTrue(RegistrationQtHelper.failsOnlyQt(masters, 0, 46));
		Category noQt = category("Open", 15, 999, 95, 0);
		assertFalse(RegistrationQtHelper.failsOnlyQt(noQt, null, 46));
	}

	private Category category(String ageGroupCode, int minAge, int maxAge, int upperLimit, int qualifyingTotal) {
		AgeGroup ageGroup = new AgeGroup();
		ageGroup.setCode(ageGroupCode);
		ageGroup.setGender(Gender.M);
		ageGroup.setMinAge(minAge);
		ageGroup.setMaxAge(maxAge);
		Category category = new Category();
		category.setAgeGroup(ageGroup);
		category.setGender(Gender.M);
		category.setMaximumWeight((double) upperLimit);
		category.setQualifyingTotal(qualifyingTotal);
		return category;
	}
}
