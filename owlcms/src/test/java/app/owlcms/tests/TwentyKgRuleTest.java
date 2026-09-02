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

import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.athlete.Athlete;

public class TwentyKgRuleTest {

    private static final int ALLOWED_MARGIN = 20;

    private Athlete athlete;

    @Before
    public void setupTest() {
        athlete = new Athlete();
        athlete.setEntryTotal(185);
        athlete.setSnatch1Declaration("60");
        athlete.setCleanJerk1Declaration("80");
    }

    @Test
    public void declarationDeficitWithoutChangesIsNotDebt() {
        assertEquals("declarations are 25 kg short", 25,
                athlete.startingDeclarationTotalDelta(ALLOWED_MARGIN));
        assertEquals("current requests initially match declarations", 25,
                athlete.startingTotalDelta(ALLOWED_MARGIN));
        assertFalse("declaration-only violations remain blocking",
                athlete.hasStartingTotalDebt(ALLOWED_MARGIN));
    }

    @Test
    public void firstAttemptChangeCreatesDebtAndRequiredCleanJerkTarget() {
        athlete.setSnatch1Change1("65");

        assertTrue("a first-attempt change makes the shortfall non-blocking debt",
                athlete.hasStartingTotalDebt(ALLOWED_MARGIN));
        assertEquals("the required clean and jerk includes the remaining debt", 100,
                athlete.getRequiredCleanJerkForStartingTotal(ALLOWED_MARGIN));
    }

    @Test
    public void latestFirstAttemptRequestsDetermineDebt() {
        athlete.setSnatch1Change1("85");

        assertFalse("an upward change can satisfy the starting-total rule",
                athlete.hasStartingTotalDebt(ALLOWED_MARGIN));

        athlete.setSnatch1Change2("75");

        assertEquals("the latest snatch request creates 10 kg of debt", 10,
                athlete.startingTotalDelta(ALLOWED_MARGIN));
        assertTrue("a later reduction creates starting-total debt",
                athlete.hasStartingTotalDebt(ALLOWED_MARGIN));
        assertEquals("all remaining debt must move to clean and jerk", 90,
                athlete.getRequiredCleanJerkForStartingTotal(ALLOWED_MARGIN));

        athlete.setCleanJerk1Change1("90");

        assertFalse("raising the clean and jerk repays the debt",
                athlete.hasStartingTotalDebt(ALLOWED_MARGIN));
    }
}
