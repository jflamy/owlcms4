package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;

public class IndividualEligibilityStatusTest {

    private static final Level LOGGER_LEVEL = Level.OFF;
    private Athlete athlete;

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

    @Before
    public void setupTest() {
        athlete = new Athlete();
        athlete.setLastName("Test");
        athlete.setFirstName("User");
        // quiet logs
        athlete.getLogger().setLevel(LOGGER_LEVEL);
    }

    /**
     * Test all combinations where boolean setter is used and enum is null / non-null.
     */
    @Test
    public void testBooleanSetterWhenEnumNull() {
        // initial: enum null, boolean default true
        assertTrue(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.ELIGIBLE, athlete.getEffectiveIndividualEligibilityStatus());

        // set boolean false -> when enum is null, enum becomes OOC_INVITED and boolean false
        athlete.setEligibleForIndividualRanking(false);
        assertFalse(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.OOC_INVITED, athlete.getIndividualEligibilityStatus());
    }

    @Test
    public void testBooleanSetterWhenEnumNotNull() {
        // set the enum to something else first and not OOC_INVITED, so we can see if boolean setter overrides it
        athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.DSQ_DOPING);
        assertEquals(EligibleForIndividualRankingStatus.DSQ_DOPING, athlete.getIndividualEligibilityStatus());
        assertFalse(athlete.isEligibleForIndividualRanking());

        athlete.setEligibleForIndividualRanking(true);
        // enum should have switched to ELIGIBLE, so effective eligibility is true
        assertTrue(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.ELIGIBLE, athlete.getIndividualEligibilityStatus());

        // set boolean false -> when enum is non-null, enum becomes OOC_INVITED and boolean false
        athlete.setEligibleForIndividualRanking(false);
        assertFalse(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.OOC_INVITED, athlete.getIndividualEligibilityStatus());
    }

    @Test
    public void testBooleanSetterFalseDoesNotOverrideExistingNonEligibleEnum() {
        athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.DSQ_DISCIPLINARY);
        assertEquals(EligibleForIndividualRankingStatus.DSQ_DISCIPLINARY, athlete.getIndividualEligibilityStatus());
        assertFalse(athlete.isEligibleForIndividualRanking());

        athlete.setEligibleForIndividualRanking(false);
        assertFalse(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.DSQ_DISCIPLINARY, athlete.getIndividualEligibilityStatus());
    }

    @Test
    public void testBooleanSetterTrueLeavesExistingEligibleEnum() {
        athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.ELIGIBLE);
        assertTrue(athlete.isEligibleForIndividualRanking());
        athlete.setEligibleForIndividualRanking(true);
        assertTrue(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.ELIGIBLE, athlete.getIndividualEligibilityStatus());
    }


    /**
     * Test setting explicit enum status updates boolean accordingly.
     */
    @Test
    public void testEnumSetterUpdatesBoolean() {
        // set explicit non-eligibility: DSQ_DOPING
        athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.DSQ_DOPING);
        assertFalse(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.DSQ_DOPING, athlete.getIndividualEligibilityStatus());

        // set explicit to null should leave boolean unchanged (false)
        athlete.setIndividualEligibilityStatus(null);
        assertFalse(athlete.isEligibleForIndividualRanking());

        // set explicit eligible
        athlete.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.ELIGIBLE);
        assertTrue(athlete.isEligibleForIndividualRanking());
        assertEquals(EligibleForIndividualRankingStatus.ELIGIBLE, athlete.getIndividualEligibilityStatus());

        // set explicit to null should leave boolean unchanged (true)
        athlete.setIndividualEligibilityStatus(null);
        assertTrue(athlete.isEligibleForIndividualRanking());
    }

}
