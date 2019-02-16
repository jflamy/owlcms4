/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;
import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.Gender;
import org.ledocte.owlcms.data.category.AgeDivision;
import org.ledocte.owlcms.data.category.Category;

public class AthleteTest {

    private static Athlete athlete;

    @Before
    public void setupTest() {
        athlete = new Athlete();
        athlete.setLastName("Strong"); //$NON-NLS-1$
        athlete.setFirstName("Paul"); //$NON-NLS-1$
        athlete.setGender("M"); //$NON-NLS-1$
        athlete.setBodyWeight(68.5);
        athlete.setSnatch1Declaration("60"); //$NON-NLS-1$
        athlete.setCleanJerk1Declaration("80"); //$NON-NLS-1$
        athlete.setYearOfBirth(1900);
        Category registrationCategory = new Category(67.0,73.0,Gender.M,true,AgeDivision.DEFAULT,348);
        athlete.setRegistrationCategory(registrationCategory);
    }


    /**
     * Test method for
     * {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalNoData() {
        assertEquals("total without any results", 0, (long) athlete.getTotal()); //$NON-NLS-1$
    }

    /**
     * Test method for
     * {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalNoSnatchData() {
        athlete.setSnatch1ActualLift(null);
        athlete.setSnatch2ActualLift(null);
        athlete.setSnatch3ActualLift(null);
        athlete.setCleanJerk1ActualLift("80"); //$NON-NLS-1$
        athlete.setCleanJerk2ActualLift("81"); //$NON-NLS-1$
        athlete.setCleanJerk3ActualLift("82"); //$NON-NLS-1$
        assertEquals("total with no snatch results", 0L, (long) athlete.getTotal()); //$NON-NLS-1$
    }

    /**
     * Test method for
     * {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalNoCleanJerkData() {
        athlete.setSnatch1ActualLift("60"); //$NON-NLS-1$
        athlete.setSnatch2ActualLift("61"); //$NON-NLS-1$
        athlete.setSnatch3ActualLift("62"); //$NON-NLS-1$
        athlete.setCleanJerk1ActualLift(null);
        athlete.setCleanJerk2ActualLift(null);
        athlete.setCleanJerk3ActualLift(null);
        assertEquals("total with no clean and jerk results", 0, (long) athlete.getTotal()); //$NON-NLS-1$
    }

    /**
     * Test method for
     * {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalPartialData() {
        athlete.setSnatch1ActualLift("60"); //$NON-NLS-1$
        athlete.setSnatch2ActualLift(""); //$NON-NLS-1$
        athlete.setSnatch3ActualLift(null);
        athlete.setCleanJerk1ActualLift("-80"); //$NON-NLS-1$
        athlete.setCleanJerk2ActualLift("-"); //$NON-NLS-1$
        athlete.setCleanJerk3ActualLift(null);
        assertEquals("total with failed clean and jerk results", 0, (long) athlete.getTotal()); //$NON-NLS-1$
    }

    /**
     * Test method for
     * {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalHappyPath() {
        athlete.setSnatch1ActualLift("60"); //$NON-NLS-1$
        athlete.setSnatch2ActualLift("61"); //$NON-NLS-1$
        athlete.setSnatch3ActualLift("62"); //$NON-NLS-1$
        athlete.setCleanJerk1ActualLift("80"); //$NON-NLS-1$
        athlete.setCleanJerk2ActualLift("81"); //$NON-NLS-1$
        athlete.setCleanJerk3ActualLift("82"); //$NON-NLS-1$
        assertEquals("total with all values", 144, (long) athlete.getTotal()); //$NON-NLS-1$
        assertEquals("robi score", 53.33D, athlete.getRobi(),0.005);
    }

    /**
     * Test method for
     * {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalSnatchBombOut() {
        athlete.setSnatch1ActualLift("-60"); //$NON-NLS-1$
        athlete.setSnatch2ActualLift("-60"); //$NON-NLS-1$
        athlete.setSnatch3ActualLift("-60"); //$NON-NLS-1$
        athlete.setCleanJerk1ActualLift("80"); //$NON-NLS-1$
        athlete.setCleanJerk2ActualLift("81"); //$NON-NLS-1$
        athlete.setCleanJerk3ActualLift("-"); //$NON-NLS-1$
        assertEquals("total with snatch bomb out", 0, (long) athlete.getTotal()); //$NON-NLS-1$
    }

    /**
     * Test method for
     * {@link org.concordiainternational.competition.data.Athlete#getTotal()}.
     */
    @Test
    public void testGetTotalBombOut() {
        athlete.setSnatch1ActualLift("-60"); //$NON-NLS-1$
        athlete.setSnatch2ActualLift("-60"); //$NON-NLS-1$
        athlete.setSnatch3ActualLift("-60"); //$NON-NLS-1$
        athlete.setCleanJerk1ActualLift("-80"); //$NON-NLS-1$
        athlete.setCleanJerk2ActualLift("-80"); //$NON-NLS-1$
        athlete.setCleanJerk3ActualLift("-80"); //$NON-NLS-1$
        assertEquals("total with full bomb out", 0, (long) athlete.getTotal()); //$NON-NLS-1$
    }

    @Test
    public void ageGroup() {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        assertEquals(80, (long) athlete.getAgeGroup());
        athlete.setYearOfBirth(thisYear - 40);
        assertEquals(40, (long) athlete.getAgeGroup());
        athlete.setYearOfBirth(thisYear - 39);
        assertEquals(35, (long) athlete.getAgeGroup());
        athlete.setYearOfBirth(thisYear - 41);
        assertEquals(40, (long) athlete.getAgeGroup());
        athlete.setYearOfBirth(thisYear - 86);
        assertEquals(80, (long) athlete.getAgeGroup());
        athlete.setGender("F"); //$NON-NLS-1$
        assertEquals(70, (long) athlete.getAgeGroup());
        athlete.setYearOfBirth(null);
        assertEquals(70, (long) athlete.getAgeGroup());
        athlete.setGender(""); //$NON-NLS-1$
        athlete.setYearOfBirth(1900);
        assertEquals(70, (long) athlete.getAgeGroup());
    }

}
